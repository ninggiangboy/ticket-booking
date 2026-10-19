# Idempotency

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-25
> Phụ thuộc: SDD gốc §8.6, §10.5, [DOC-03](../01-product/requirements.md) (FR-08, NFR-04), [DOC-06](../02-glossary.md), [DOC-12](../03-architecture/code-architecture.md) §2, §3.3, [DOC-15](../05-data/ops-model.md) §5, §10, [DOC-35](error-handling.md) §3, §5, §6, [DOC-36](../07-api/api-guidelines.md) §6.1–6.2, [DOC-37](../07-api/api-endpoints.md) (E-14, E-16, E-17, E-18), [DOC-33](observability.md), [DOC-34](configuration-reference.md), [Sổ quyết định](../00-decision-register.md) (DR-19, 41, 43, 45, 47, 61, 64, 74, 153)
> Người dùng chính: P2-07 (hạ tầng idempotency), P2-08 (giữ vé, `confirm-free`), P2-09 (hủy giữ đường nhanh), P3-02 (`payment-intent`), frontend `src/api/` và `src/lib/`; [DOC-24](inventory-and-reservation.md), DOC-26 (`checkout-and-payment.md`, P3), EXP-03

Tài liệu mô tả cách bốn lệnh ghi của người mua trở nên an toàn khi gửi lại: header `Idempotency-Key`, băm request, giao thức chèn key ở đầu transaction nghiệp vụ, phát lại response và hành vi khi hai request cùng key chạy song song. DDL của bảng `idempotency_key` ở DOC-15 §5; schema request/response của từng endpoint ở DOC-37; quy tắc retry phía client ở DOC-35 §6; thứ tự câu của transaction giữ vé ở DOC-24. Tài liệu không lặp các phần đó.

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi thành DR-174…177 và tóm tắt ở §10.

## 1. Mục đích và phạm vi

| Endpoint | `operation` | Header bắt buộc | Ghi vào `idempotency_key` | Transaction chứa dòng key | Điều phối ở |
| --- | --- | --- | --- | --- | --- |
| E-14 `POST /events/{eventId}/reservations` | `HOLD` | có | có | Transaction giữ vé (DR-41) | `reservation.service.HoldService` |
| E-16 `DELETE /reservations/{reservationId}` | `CANCEL_HOLD` | có | có | Transaction thứ nhất của đường nhanh (DR-43) | `reservation.service.ReleaseService` (đường nhanh, tên và tách bean theo DOC-24) |
| E-18 `POST /orders/{orderId}/confirm-free` | `CONFIRM_FREE` | có | có | Transaction xác nhận (DOC-26) | `payment.service.ConfirmationService` qua `IdempotencyApi` |
| E-17 `POST /orders/{orderId}/payment-intent` | — | có (nhận, kiểm định dạng) | **không** | — | `payment.service.PaymentIntentService`; idempotent tự nhiên theo `order_id` (DR-47) |

Endpoint khác không đọc header `Idempotency-Key` (có gửi cũng bị bỏ qua). Webhook loại trùng bằng `stripe_event` (DR-48), magic link không có idempotency (DOC-19).

| Thành phần | Chạy ở | Kích hoạt | Đọc | Ghi | Việc |
| --- | --- | --- | --- | --- | --- |
| `IdempotencyRequestFactory` | `reservation` (gốc, dùng chung) | Controller của bốn endpoint | header, path, body thô | — | Kiểm header, băm request |
| `IdempotencyService` (cài `IdempotencyApi`) | `reservation.service` | Service điều phối, trong transaction của nó | `idempotency_key` | `idempotency_key` | `begin`, `complete` |
| `IdempotencyKeyRepository` | `reservation.repository` | `IdempotencyService` | `idempotency_key` | `idempotency_key` | SQL `JdbcClient` (bảng không có aggregate, DR-153) |
| `IdempotencyRetentionContributor` | `reservation.service` | `RetentionJob` 03:00 | `idempotency_key` | xóa dòng | Dọn sau `retention.idempotency-key` = 24 giờ (DR-74) |
| `idempotentRequest()` | frontend `src/lib/idempotency.ts` | Mọi lời gọi E-14, E-16, E-17, E-18 | — | bộ nhớ của trang | Giữ cặp (key, chuỗi body) qua các lần thử lại |

## 2. Thành phần và interface

### 2.1 Gốc module `reservation` (API công khai)

```java
package io.ticket.reservation;

/** Một lệnh ghi có Idempotency-Key, đã kiểm và băm ở controller. */
public record IdempotencyRequest(
    UUID userId,
    UUID key,                     // header Idempotency-Key
    IdempotentOperation operation,
    byte[] requestHash) {         // SHA-256, 32 byte
}

public enum IdempotentOperation { HOLD, CANCEL_HOLD, CONFIRM_FREE }   // khớp CHECK của DOC-15 §5

/** Kết quả của begin(): chạy thật, hoặc phát lại response đã lưu. */
public sealed interface IdempotencyStart {
  record Fresh() implements IdempotencyStart {}
  record Replay(int status, String responseJson) implements IdempotencyStart {}
}

public interface IdempotencyApi {
  /** Câu đầu tiên của transaction nghiệp vụ. Ném IdempotencyKeyReusedException khi hash khác. */
  @Transactional(propagation = Propagation.MANDATORY)
  IdempotencyStart begin(IdempotencyRequest request);

  /** Câu cuối trước COMMIT; chỉ gọi với status 2xx. */
  @Transactional(propagation = Propagation.MANDATORY)
  void complete(IdempotencyRequest request, int status, Object responseBody);
}

public class IdempotencyKeyReusedException extends DomainException { … }   // 422 IDEMPOTENCY_KEY_REUSED
```

`IdempotencyApi` là hợp đồng mà `payment` dùng cho `confirm-free` (DOC-12 §2.3, DOC-07 §4.1). `MANDATORY` làm `begin` nổ lỗi ngay nếu bị gọi ngoài transaction: dòng key phải commit cùng tác dụng nghiệp vụ, không bao giờ riêng.

### 2.2 Controller: đọc body thô (`DR-174`)

Hash phải tính trên đúng các byte nhận được (DR-45), nên controller của ba endpoint có body nhận `@RequestBody(required = false) byte[]` rồi tự đọc JSON bằng `JsonBodyReader` (cùng `ObjectMapper` chặt của DR-88, cùng Bean Validation như `@Valid`, cùng mã lỗi 400 `BAD_REQUEST` và 422 `VALIDATION_FAILED`). Không dùng `ContentCachingRequestWrapper`: byte chỉ có sau khi Spring đọc xong body, và một filter ẩn khó đối chiếu với tài liệu (DR-153).

```java
// io.ticket.reservation.controller.ReservationController
@PostMapping("/events/{eventId}/reservations")
ResponseEntity<String> create(@PathVariable UUID eventId,
                              @RequestHeader(name = "Idempotency-Key", required = false) String idemKey,
                              @RequestBody(required = false) byte[] rawBody,
                              HttpServletRequest http,
                              @AuthenticationPrincipal SessionUser user) {
  IdempotencyRequest idem = idempotencyRequests.of(user.userId(), idemKey, IdempotentOperation.HOLD,
                                                   http.getRequestURI(), rawBody);   // 400/422 về key ở đây
  CreateReservationRequest body = jsonBodyReader.read(rawBody, CreateReservationRequest.class); // 400/422 về body
  HoldResult result = holdService.hold(user.userId(), eventId, idem, body.toCommand());
  return IdempotentResponses.created(result, "/api/v1/reservations/");                // §3.4
}
```

```java
// io.ticket.reservation.IdempotencyRequestFactory  (gốc: payment dùng cho confirm-free)
public IdempotencyRequest of(UUID userId, String header, IdempotentOperation op, String path, byte[] rawBody) {
  if (header == null || header.isBlank()) throw new IdempotencyKeyRequiredException();     // 400
  UUID key = parseUuidOrThrow(header);                                                    // 422 rule = uuid
  return new IdempotencyRequest(userId, key, op, hash(op, path, rawBody == null ? new byte[0] : rawBody));
}
```

`DELETE /reservations/{id}` không có body: `rawBody` rỗng. `confirm-free` gửi `{}` (DOC-37 E-18): hash tính trên 2 byte `{}`. Client gửi lại với body rỗng thay vì `{}` là body khác (§3.1).

Thứ tự kiểm tra trong request: (1) session và CSRF (filter); (2) header `Idempotency-Key`; (3) body (JSON, Bean Validation, giới hạn 50 unit của DR-41); (4) kiểm tra rẻ trước khi lấy connection (rate limit, cờ hết vé, bulkhead, DR-61); (5) transaction. Lệnh thiếu key bị chặn trước service nên không thuộc `ticket_hold_result_total` (OBS-06).

### 2.3 Repository

```java
// io.ticket.reservation.repository.IdempotencyKeyRepository  (@Repository, JdbcClient)
int insertIfAbsent(UUID userId, UUID key, String operation, byte[] requestHash);   // 1 hoặc 0 dòng
Optional<StoredKey> find(UUID userId, UUID key);   // record StoredKey(String operation, byte[] requestHash, Integer responseStatus, String responseBody)
int fillResponse(UUID userId, UUID key, int status, String responseJson);          // phải 1 dòng
int deleteOlderThan(Duration age, int batchSize);                                  // RetentionContributor
```

## 3. Thuật toán

### 3.1 Băm request (DR-45)

```
request_hash = SHA-256( utf8(operation) || 0x0A || utf8(path) || 0x0A || rawBody )
```

- `operation`: `HOLD`, `CANCEL_HOLD`, `CONFIRM_FREE`.
- `path`: `HttpServletRequest.getRequestURI()` đúng như nhận, gồm `/api/v1`, không gồm query string. Ví dụ `/api/v1/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34/reservations`.
- `rawBody`: các byte của body sau khi giải nén transfer (Tomcat đã xử lý `chunked`), không chuẩn hóa JSON, không trim.

Ví dụ: body `{"items":[{"type":"GA","ticketTypeId":"0199b1c2-7b01-7f10-8a22-3c4d5e6f7081","quantity":2}]}` và body cùng nội dung nhưng có một dấu cách sau `:` là **hai** hash khác nhau; gửi lại cùng key với bản có dấu cách → 422 `IDEMPOTENCY_KEY_REUSED` (IDEM-14). Đây là chủ ý của DR-45: client phải giữ chuỗi đã serialize (§3.6).

Đưa `operation` và `path` vào hash làm cho cùng một key dùng nhầm ở hai endpoint, hoặc ở hai sự kiện khác nhau, bị bắt là `IDEMPOTENCY_KEY_REUSED` thay vì phát lại response của lệnh khác (IDEM-07).

### 3.2 `begin` (câu đầu tiên của transaction nghiệp vụ)

```
begin(req):
 1. n = INSERT INTO idempotency_key (user_id, idem_key, operation, request_hash)
        VALUES (:u, :k, :op, :hash) ON CONFLICT (user_id, idem_key) DO NOTHING
 2. nếu n = 1 → return Fresh                                    -- lần đầu: chạy thật
 3. row = SELECT operation, request_hash, response_status, response_body
          FROM idempotency_key WHERE user_id = :u AND idem_key = :k
 4. nếu row.request_hash ≠ :hash → throw IdempotencyKeyReusedException   -- 422, rollback
 5. nếu row.response_status IS NULL → throw IllegalStateException          -- §7, lỗi lập trình, 500
 6. return Replay(row.response_status, row.response_body)
```

Service điều phối dừng ngay khi nhận `Replay`: không đọc event, không claim, không gọi ra ngoài; transaction kết thúc (rollback hay commit đều không đổi gì vì chưa ghi). `HoldService` tăng `ticket_hold_result_total{result="idempotent_replay"}`.

Bước 1 luôn là câu SQL ghi đầu tiên, ngay sau `SET LOCAL statement_timeout` và `SET LOCAL lock_timeout` (DOC-14 §8 bước 1, DR-41). Lý do: request trùng dừng trước khi chạm `reservation` hay `inventory_unit`, và khóa của dòng key là thứ tuần tự hóa hai request cùng key (§4.1).

### 3.3 `complete` (câu cuối trước COMMIT)

```
complete(req, status, body):           -- status ∈ 200…299
 1. json = objectMapper.writeValueAsString(body)
 2. n = UPDATE idempotency_key SET response_status = :status, response_body = :json::jsonb
        WHERE user_id = :u AND idem_key = :k AND response_status IS NULL
 3. nếu n ≠ 1 → throw IllegalStateException   -- dòng phải do chính transaction này chèn
```

Kết quả lỗi (4xx, 5xx) không bao giờ gọi `complete`: exception làm rollback cả transaction, gồm dòng key đã chèn ở `begin` (DR-45). Gửi lại sau một 409 chạy lại thật; đúng nghĩa vì 409 không để lại tác dụng phụ (IDEM-05).

### 3.4 Phát lại response (`DR-176`)

- Bảng chỉ giữ `response_status` và `response_body`; **không** giữ header. Controller dựng lại header từ body:
  - `Idempotent-Replayed: true` (chỉ khi phát lại, DOC-36 §6.1).
  - E-14 với status 201: `Location: /api/v1/reservations/{reservationId}`, `reservationId` đọc từ `response_body`.
  - `X-Request-Id`, `X-Server-Time` là của request hiện tại, không phải của lần đầu.
- `response_body` là `jsonb`, nên thứ tự khóa và khoảng trắng của bản phát lại có thể khác byte của lần đầu. Hợp đồng là **bằng nhau về JSON** (cùng khóa, cùng giá trị), không bằng nhau từng byte. Test so sánh bằng `JsonNode.equals` (IDEM-02).
- Response phát lại là ảnh chụp lúc commit lần đầu, không phải trạng thái hiện tại. Ví dụ: phát lại E-14 sau khi reservation đã hết hạn vẫn trả 201 `"status": "ACTIVE"`; màn Thanh toán gọi E-15 ngay sau đó và thấy trạng thái thật (DR-66: đồng hồ về 0 thì gọi lại API). Ngoại lệ duy nhất là `CANCEL_HOLD` (§3.5).

Ví dụ phát lại E-14 (cùng key, cùng byte body với DOC-36 §6.2):

```http
HTTP/1.1 201 Created
Idempotent-Replayed: true
Location: /api/v1/reservations/0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091
X-Request-Id: 0199b1c3-0a11-7c00-8d00-9f1e2d3c4b5a
Content-Type: application/json

{"reservationId":"0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091","orderId":"0199b1c2-8d15-7a22-9b0c-1d2e3f405162","status":"ACTIVE","expiresAt":"2026-11-10T03:10:00.000Z","amount":0,"currency":"VND","items":[{"type":"GA","ticketTypeId":"0199b1c2-7b01-7f10-8a22-3c4d5e6f7081","ticketTypeName":"Vé tiêu chuẩn","quantity":2,"unitPrice":0,"seats":[]}]}
```

### 3.5 Hủy giữ vé (E-16): key ở transaction thứ nhất (`DR-175`)

Đường nhanh của DR-43 gồm hai transaction và một lời gọi ra ngoài ở giữa. Dòng key nằm ở **transaction thứ nhất**:

```
cancel(userId, reservationId, req):
 T1 (@Transactional; method của service trong DOC-24):
   1. idempotency.begin(req)
      - Replay → không dùng response đã lưu; nhảy tới bước 6 (đọc trạng thái hiện tại)
   2. n = UPDATE reservation SET status='EXPIRING', close_reason='BUYER_CANCELLED', expiring_since=now()
          WHERE reservation_id=:r AND user_id=:u AND status='ACTIVE'
   3. state = trạng thái hiện tại của reservation (sau bước 2)
   4. idempotency.complete(req, 202, {"reservationId": r, "status": "EXPIRING"})   nếu n = 1
      idempotency.complete(req, 200, {"reservationId": r, "status": <state>})      nếu n = 0
   COMMIT
 ngoài transaction: nếu n = 1 → chạy thủ tục "trả vé" của DOC-24 (hủy PaymentIntent nếu có, T2 trả vé)
 5. trả 200 CANCELLED, 202 EXPIRING, hoặc 409 PAYMENT_ALREADY_SUCCEEDED theo DR-43
 6. (phát lại) đọc reservation + order hiện tại, trả theo bảng dưới, kèm Idempotent-Replayed: true
```

T2 (trả vé) **không** cập nhật dòng key. Response của `CANCEL_HOLD` khi phát lại được tính từ trạng thái hiện tại, không lấy từ `response_body`:

| Trạng thái hiện tại khi phát lại | Response |
| --- | --- |
| reservation `CANCELLED` hoặc `EXPIRED` | 200 `{ "reservationId", "status": "CANCELLED" \| "EXPIRED" }` |
| reservation `EXPIRING` (job chưa xong) | 202 `{ "reservationId", "status": "EXPIRING" }` |
| reservation `CONFIRMED` (PaymentIntent đã `succeeded`, P3) | 409 `PAYMENT_ALREADY_SUCCEEDED` kèm `orderId` |

Lý do: lần đầu có thể trả 202 (Stripe lỗi) rồi `ReleaseExpiredHoldsJob` hoàn tất vài giây sau; hoặc tiến trình chết sau T1 trước khi trả response. Phát lại response đã lưu sẽ báo `EXPIRING` mãi, trong khi trạng thái hủy vốn idempotent theo nghĩa nghiệp vụ (DR-43 bước 4: "đã `EXPIRING`/đóng → 200 với trạng thái hiện tại"). Dòng key vẫn cần để bắt `IDEMPOTENCY_KEY_REUSED` và để `begin` tuần tự hóa hai lần bấm "Hủy" đồng thời. `response_status` ở dòng `CANCEL_HOLD` chỉ để ghi vết (luôn 2xx, thỏa §3.3).

### 3.6 Phía client (DR-45)

`src/lib/idempotency.ts`:

```ts
export interface PendingWrite { key: string; body: string }    // body là chuỗi đã JSON.stringify, gửi nguyên văn

/** Một thao tác của người dùng = một PendingWrite; mọi lần thử lại dùng lại đúng object này. */
export function newPendingWrite(payload: unknown | undefined): PendingWrite {
  return { key: crypto.randomUUID(), body: payload === undefined ? '' : JSON.stringify(payload) };
}
```

1. Bấm "Giữ vé", "Hủy giữ vé", "Nhận vé": tạo **một** `PendingWrite`, lưu trong state của màn (không lưu `localStorage`).
2. Gửi `fetch(url, { body: pending.body, headers: { 'Idempotency-Key': pending.key, … } })`; `apiClient` không serialize lại body.
3. Thử lại tự động (503 `OVERLOADED`, 429, lỗi mạng, theo DOC-35 §6 và DR-132) dùng **cùng** `PendingWrite`.
4. Nhận 2xx, hoặc 4xx không thử lại được (409, 422): bỏ `PendingWrite`. Thao tác kế tiếp tạo key mới.
5. Nhận 422 `IDEMPOTENCY_KEY_REUSED`: lỗi lập trình (DOC-40 §4.1): ghi `console.error` kèm `requestId`, bỏ `PendingWrite`, hiện thông báo chung.
6. Người dùng đổi lựa chọn (thêm vé) sau một lần lỗi: là thao tác mới, key mới.

Tải lại trang làm mất `PendingWrite`: lần bấm sau là lần giữ vé mới; nếu lần trước đã commit, server trả 409 `ACTIVE_RESERVATION_EXISTS` kèm `reservationId` (DR-41) và giao diện chuyển tới reservation đó. Không cần giữ key qua tải lại trang.

## 4. Transaction và đồng thời

### 4.1 Hai request cùng key chạy song song

Request A và B cùng `(user_id, idem_key)`, cùng hash. Cả hai chạy `SET LOCAL lock_timeout = '1s'` rồi `INSERT … ON CONFLICT DO NOTHING`:

1. A chèn trước; dòng chưa commit. Unique index `idempotency_key_pkey` có một tuple của A.
2. B chèn: PostgreSQL thấy tuple trùng khóa thuộc transaction **chưa kết thúc** của A, nên B **chờ** trên khóa transaction ID của A (speculative insertion chờ `XactLockTableWait`), không trả 0 dòng ngay. Chờ này là một lần chờ khóa nên bị `lock_timeout` giới hạn.
3. A commit (dòng có response 2xx): B thức dậy, phát hiện xung đột, `DO NOTHING` trả 0 dòng; câu `SELECT` ở bước 3 của §3.2 chạy với snapshot mới (READ COMMITTED) và thấy dòng đã commit kèm response → B trả phát lại.
4. A rollback (409, 422, lỗi): tuple của A biến mất; B chèn được (1 dòng) và chạy thật như lần đầu. Kết quả cuối vẫn đúng một tác dụng.
5. A giữ transaction lâu hơn 1 giây (A có `statement_timeout` 2 giây cho mỗi câu, DR-41): B nhận lỗi `55P03 lock_not_available` → Spring `CannotAcquireLockException` (`TransientDataAccessException`) → 503 `OVERLOADED` kèm `Retry-After` 1–3 giây (DOC-35 §5.1 dòng 10). Client thử lại cùng key và lần sau nhận phát lại (IDEM-08).

Vì vậy N request cùng key không bao giờ cùng claim kho vé: chỉ transaction giữ dòng key (A) đi qua bước 1; các request khác hoặc chờ rồi phát lại, hoặc 503 rồi phát lại khi thử lại. Không có lúc nào hai transaction cùng thấy "chưa có key".

Thời gian: transaction giữ vé GA p95 kế hoạch < 120 ms (DOC-37 E-14), nên trong EXP-03 gần như mọi request trùng chờ < 1 giây và nhận phát lại ngay; 503 chỉ xảy ra khi A bị chậm bất thường.

### 4.2 Commit cùng nhau

| Endpoint | Commit cùng dòng key |
| --- | --- |
| E-14 | `reservation`, `reservation_item`, unit `HELD`, `orders` `PENDING_PAYMENT`, response 201 (DR-41) |
| E-16 | reservation `ACTIVE → EXPIRING` (T1), response 202/200 ghi vết |
| E-18 | reservation `CONFIRMED`, unit `SOLD`, `orders` `PAID`, `ticket`, outbox `EMAIL_TICKETS`, response 200 `OrderView` |

### 4.3 Tiến trình chết

| Điểm chết | Trạng thái DB | Người dùng thấy | Thử lại cùng key |
| --- | --- | --- | --- |
| Trước COMMIT của transaction có key | Không có gì (rollback) | Lỗi mạng | Chạy thật như lần đầu |
| Sau COMMIT, trước khi gửi response (E-14, E-18) | Reservation/vé đã có, key có response | Lỗi mạng | Phát lại 201/200, `Idempotent-Replayed: true` (FR-08.4, IDEM-04) |
| E-16 sau T1, trước trả vé | Reservation `EXPIRING`, key có 202 | Lỗi mạng | 202 `EXPIRING`; `ReleaseExpiredHoldsJob` lấy lại sau lease 30 giây (DR-42); lần thử sau đó → 200 `CANCELLED` (§3.5) |

### 4.4 Phạm vi key

Khóa chính là `(user_id, idem_key)`: cùng một UUID do hai người dùng gửi là hai dòng độc lập (IDEM-09). Key không gắn session: đăng xuất rồi đăng nhập lại vẫn phát lại được trong 24 giờ.

### 4.5 `payment-intent` (E-17)

Header bắt buộc và kiểm định dạng như ba endpoint còn lại (400/422), để `apiClient` xử lý mọi lệnh ghi của luồng mua giống nhau. Server không đọc bảng `idempotency_key`; tạo lặp an toàn nhờ `orders.payment_intent_id` và khóa idempotency của Stripe `pi-create:<orderId>` (DR-47). Không lưu `client_secret` vào database (DR-45). Chi tiết ở DOC-26.

## 5. Cấu hình

| Khóa | Kiểu | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `retention.idempotency-key` | duration | `PT24H` | Dòng cũ hơn bị `RetentionJob` xóa (DR-74) |
| `retention.batch-size` | int | `5000` | Lô xóa |
| `hold.lock-timeout` | duration | `PT1S` | `lock_timeout` của transaction giữ vé, cũng là thời gian chờ tối đa trên key trùng (§4.1) |

Không có khóa riêng của idempotency. Giới hạn kích thước `response_body`: không cấu hình; response lớn nhất là `OrderView` với 50 vé (~12 KB).

**Dọn dữ liệu:**

```sql
DELETE FROM idempotency_key
WHERE ctid IN (SELECT ctid FROM idempotency_key
               WHERE created_at < now() - interval '24 hours'
               ORDER BY created_at LIMIT 5000);   -- idempotency_key_created_idx; lặp tới khi 0 dòng
```

Sau khi dòng bị xóa, cùng key gửi lại là lần chạy mới. Client không bao giờ giữ key quá thời gian một thao tác (§3.6), nên không có rủi ro thực tế.

## 6. Metrics và log

| Metric | Kiểu | Label | Nơi phát | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `ticket_idempotency_requests_total` (mới, `DR-177`) | C | `operation` ∈ {`HOLD`, `CANCEL_HOLD`, `CONFIRM_FREE`}, `result` ∈ {`executed`, `replayed`, `reused`} | `IdempotencyService` | Lần chạy thật, lần phát lại, lần bị từ chối vì hash khác. EXP-03 đọc trực tiếp |
| `ticket_hold_result_total{result="idempotent_replay"}` | C | (DOC-33) | `HoldService` | Đã có |
| `ticket_http_errors_total{code="IDEMPOTENCY_KEY_REQUIRED" \| "IDEMPOTENCY_KEY_REUSED"}` | C | (DOC-33) | `ApiExceptionHandler` | Đã có |

`executed` tăng sau khi `complete` thành công (tức chỉ lần chạy thật có 2xx); request chạy thật nhưng lỗi không tăng metric này.

Log (DOC-33 §2):

| Sự kiện | Mức | Trường |
| --- | --- | --- |
| `idempotency.replayed` | INFO | `operation`, `idem_key`, `response_status`, `user_id` |
| `idempotency.key_reused` | WARN | `operation`, `idem_key`, `stored_operation` (DOC-35 §5.2) |
| `idempotency.missing_response` | ERROR | `operation`, `idem_key` (§7, không được xảy ra) |

`idem_key` là UUID ngẫu nhiên của client, không phải dữ liệu cá nhân; được ghi log. Body request không bao giờ vào log.

## 7. Lỗi và cách xử lý

| Tình huống | Hành vi |
| --- | --- |
| Thiếu header ở E-14, E-16, E-17, E-18 | 400 `IDEMPOTENCY_KEY_REQUIRED`, trước khi đọc body, không lấy connection |
| Header không phải UUID | 422 `VALIDATION_FAILED`, `errors[{field:"Idempotency-Key", rule:"uuid"}]` |
| Cùng key, hash khác (body, endpoint hoặc đường dẫn khác) | 422 `IDEMPOTENCY_KEY_REUSED`; rollback; không đổi dòng đã có |
| Cùng key đang được transaction khác giữ quá 1 giây | 503 `OVERLOADED` + `Retry-After` (§4.1 bước 5) |
| Dòng đã commit mà `response_status IS NULL` | Lỗi lập trình (một đường code commit mà không gọi `complete`): 500 `INTERNAL_ERROR`, log ERROR `idempotency.missing_response`. Không xảy ra theo thiết kế; IDEM-17 khóa điều này |
| Body JSON hỏng hoặc sai validate | 400/422 như mọi endpoint; transaction chưa mở, không có dòng key |
| `complete` cập nhật 0 dòng | `IllegalStateException` → rollback, 500 |
| Gọi `begin` ngoài transaction | `IllegalTransactionStateException` (do `MANDATORY`), lỗi lập trình |

## 8. Test bắt buộc

Tiền tố `IDEM-`. IDEM-01…06 là số đã cam kết ở DOC-03 (FR-08.1…08.6). Mức theo DOC-69 §2 (I: tích hợp Testcontainers, C: đồng thời, U: đơn vị).

| ID | Mức | Kịch bản | Kỳ vọng |
| --- | --- | --- | --- |
| IDEM-01 | I | Gọi E-14, E-16, E-17, E-18 không có `Idempotency-Key` | Cả bốn: 400 `IDEMPOTENCY_KEY_REQUIRED`; không dòng `idempotency_key`, không dòng `reservation` |
| IDEM-02 | C | 20 request song song cùng key, cùng byte body giữ 2 vé GA (`CountDownLatch`), lặp 20 lần (DR-77) | Mỗi lần: đúng 1 dòng `reservation`, 2 unit `HELD`; 20 response 201 bằng nhau về JSON (cùng `reservationId`); đúng 19 có `Idempotent-Replayed: true` (hoặc ít hơn nếu có 503, khi đó thử lại cùng key cho tới 2xx); `ticket_idempotency_requests_total{operation="HOLD",result="executed"}` tăng 1 |
| IDEM-03 | I | Key K với body B1 (2 vé) đã 201; gửi K với body B2 (3 vé) | 422 `IDEMPOTENCY_KEY_REUSED`; vẫn 1 reservation 2 vé |
| IDEM-04 | I | Gọi `HoldService.hold` trực tiếp, commit, bỏ kết quả (giả lập chết trước response); gọi lại E-14 cùng key, cùng body | 201, `Idempotent-Replayed: true`, cùng `reservationId`, `Location` đúng `/api/v1/reservations/{id}` |
| IDEM-05 | I | Pool còn 1 vé; xin 2 với key K → 409 `INSUFFICIENT_CAPACITY`; tăng sức chứa thêm 5; gửi lại K cùng body | Lần hai 201 chạy thật (không có `Idempotent-Replayed`); không dòng key nào sau lần một |
| IDEM-06 | I | Dòng key `created_at` = now − 25 giờ và now − 23 giờ; chạy `RetentionJob` | Dòng 25 giờ bị xóa; dòng 23 giờ còn |
| IDEM-07 | I | Key K đã dùng cho E-14 của sự kiện X; gửi K cho E-14 của sự kiện Y (body y hệt), và cho E-18 | Cả hai 422 `IDEMPOTENCY_KEY_REUSED` (hash gồm `path` và `operation`) |
| IDEM-08 | C | Transaction A cùng key bị giữ 1,5 giây (test chèn `pg_sleep` qua hook); request B cùng key | B nhận 503 `OVERLOADED` với `Retry-After` ∈ {1,2,3} trong ≤ 1,2 giây; B thử lại sau khi A commit → 201 phát lại |
| IDEM-09 | I | Người dùng U1 và U2 gửi cùng UUID key cho E-14 | Hai reservation khác nhau, hai dòng key |
| IDEM-10 | I | E-16 khi `PaymentGateway.cancelIntent` (cổng giả, `cancel_failure`) lỗi: lần đầu 202 `EXPIRING`; job trả vé chạy xong; gửi lại cùng key | Lần hai 200 `{"status":"CANCELLED"}` + `Idempotent-Replayed: true` (§3.5). Ở P2 (chưa có PaymentIntent) giả lập bằng cách chặn bước trả vé sau T1 |
| IDEM-11 | I | E-18 đơn 0 đồng 3 vé; gửi lại cùng key 3 lần | 3 response 200 bằng nhau về JSON; đúng 3 vé `ISSUED`; đúng 1 dòng outbox `EMAIL_TICKETS` |
| IDEM-12 | I | E-17 có key hợp lệ | Không dòng nào trong `idempotency_key` |
| IDEM-13 | I | `Idempotency-Key: not-a-uuid` | 422 `VALIDATION_FAILED`, `rule = uuid` |
| IDEM-14 | I | Cùng key, body cùng nghĩa JSON nhưng thêm một dấu cách | 422 `IDEMPOTENCY_KEY_REUSED` (băm byte thô, DR-45) |
| IDEM-15 | C | A cùng key chèn dòng rồi rollback (claim thiếu) trong khi B đang chờ trên key | B chạy thật; tổng cộng 1 reservation (của B) hoặc 0 nếu B cũng thiếu; không 500 |
| IDEM-16 | U | Hash của `HOLD` + `/api/v1/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34/reservations` + body ví dụ §3.4 | Bằng hằng số hex ghi trong test (khóa thuật toán §3.1) |
| IDEM-17 | I | Chèn tay dòng key đã commit với `response_status = NULL`; gửi cùng key | 500 `INTERNAL_ERROR`; log ERROR `idempotency.missing_response` |
| IDEM-18 | E | Frontend: mock E-14 trả 503 `Retry-After: 1` hai lần rồi 201 | Ba request có cùng `Idempotency-Key` và body giống nhau từng byte |

## 9. Kiểm chứng ở task sau

- IDEM-01…09, 12…17: P2-07 và P2-08. IDEM-10: P2-09 (phần không PaymentIntent), bổ sung nhánh Stripe ở P3. IDEM-11: P2-08. IDEM-18: P2-10.
- EXP-03 (DOC-73) đo `ticket_idempotency_requests_total` và số `reservation` theo key dưới tải.

## 10. Quyết định phát sinh khi viết tài liệu này

**DR-174 · Controller đọc body thô bằng `@RequestBody byte[]`**
- **Vấn đề:** DR-45 băm byte thô; `@RequestBody` thông thường đã parse JSON nên mất byte gốc. DOC-12 §3.3 ví dụ truyền `String rawBody` vào service mà không nói lấy ở đâu.
- **Quyết định:** Ba endpoint có lưu key (E-14, E-16, E-18) nhận `@RequestBody(required = false) byte[]`, băm trong `IdempotencyRequestFactory` (package gốc `reservation`) và parse bằng `JsonBodyReader` (cùng `ObjectMapper` chặt và Bean Validation, cùng mã lỗi). Service nhận `IdempotencyRequest` (đã có hash) thay cho `String rawBody`. Không dùng `ContentCachingRequestWrapper` hay filter.
- **Hệ quả:** Ba controller không dùng `@Valid` trực tiếp; `JsonBodyReader` phải cho đúng lỗi 400/422 như `@Valid` (test ERR- của DOC-35 chạy lại trên các endpoint này). OpenAPI vẫn khai báo schema body bằng `@io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = …))`.
- **Ghi vào:** DOC-25, DOC-12 §3.3 (chữ ký `hold(…, IdempotencyRequest idem, HoldCommand cmd)`).

**DR-175 · Hủy giữ vé: dòng key ở transaction thứ nhất, phát lại tính từ trạng thái hiện tại**
- **Vấn đề:** Đường nhanh của DR-43 có hai transaction và một lời gọi Stripe; lần đầu có thể trả 202 rồi job hoàn tất sau, hoặc chết giữa chừng. Phát lại response đã lưu sẽ báo `EXPIRING` mãi.
- **Quyết định:** `begin`/`complete` chạy trong T1 (cùng câu `ACTIVE → EXPIRING`); T2 không chạm dòng key. Phát lại `CANCEL_HOLD` trả theo trạng thái hiện tại: đóng → 200, `EXPIRING` → 202, `CONFIRMED` → 409 `PAYMENT_ALREADY_SUCCEEDED`, kèm `Idempotent-Replayed: true`.
- **Hệ quả:** `CANCEL_HOLD` là ngoại lệ duy nhất của quy tắc "phát lại ảnh chụp"; khớp DR-43 bước 4.
- **Ghi vào:** DOC-25, DOC-24, DOC-37 E-16.

**DR-176 · Không lưu header; phát lại bằng nhau về JSON**
- **Vấn đề:** DOC-15 §5 chỉ có `response_status`, `response_body jsonb`; `Location` của 201 cần dựng lại; `jsonb` không giữ thứ tự khóa.
- **Quyết định:** Header dựng lại từ body (`Location` từ `reservationId`); hợp đồng phát lại là bằng nhau về JSON, không bằng nhau từng byte; response phát lại là ảnh chụp lúc commit.
- **Hệ quả:** Test so sánh `JsonNode`; client không được so chuỗi response.
- **Ghi vào:** DOC-25, DOC-36 §6.2 (một câu về "bằng nhau về JSON").

**DR-177 · Metric `ticket_idempotency_requests_total`**
- **Vấn đề:** `ticket_hold_result_total{result="idempotent_replay"}` chỉ phủ giữ vé; EXP-03 cần đếm cả hủy giữ và `confirm-free`.
- **Quyết định:** Counter `ticket_idempotency_requests_total{operation, result}`, `operation` ∈ {`HOLD`, `CANCEL_HOLD`, `CONFIRM_FREE`}, `result` ∈ {`executed`, `replayed`, `reused`}, phát ở `IdempotencyService`.
- **Hệ quả:** DOC-33 §4.2 thêm một dòng; tập label đóng (DR-121).
- **Ghi vào:** DOC-33, DOC-25, DOC-73.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-174…177 (§10).
