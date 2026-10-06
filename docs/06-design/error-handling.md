# Xử lý lỗi

> Trạng thái: **Review** · Cập nhật: 2026-10-06 · DOC-35
> Phụ thuộc: SDD gốc §10.5, §12.3, [DOC-06](../02-glossary.md), [DOC-12](../03-architecture/code-architecture.md) §4, [DOC-36](../07-api/api-guidelines.md), [Sổ quyết định](../00-decision-register.md) (DR-09, 10, 12, 21, 25, 35, 41, 43, 44, 45, 47, 48, 55, 56, 58, 61, 63, 64, 66)
> Người dùng chính: P1-01 (khung `common.error`), P1-04 (Problem Details và i18n); mọi task backend khi thêm exception; [DOC-37](../07-api/api-endpoints.md) (cột mã lỗi), [DOC-40](../08-ux-ui/ui-states-and-copy.md) (ánh xạ mã lỗi → thông báo)

Tài liệu cho biết một lỗi được phân loại, đặt mã, đổi sang HTTP, ghi log và được client xử lý thế nào. Nó là nguồn duy nhất của **bảng mã lỗi** (40 mã). Định dạng Problem Details, header và phân trang nằm ở [DOC-36](../07-api/api-guidelines.md); thông báo hiển thị cho người dùng nằm ở DOC-40; log và metric nằm ở DOC-33. Mọi quyết định mới khi viết tài liệu này là DR-82…85 và tóm tắt ở mục 9.

## 1. Ba lớp lỗi

| Lớp | Nguyên nhân | HTTP | Log | Retry của client | Ví dụ |
| --- | --- | --- | --- | --- | --- |
| `BUSINESS` (nghiệp vụ) | Request hợp lệ về hình thức nhưng bị luật nghiệp vụ hoặc trạng thái từ chối; kết quả mong đợi | 4xx | INFO (`WARN` cho 401/403 lặp lại và `QUEUE_REQUIRED` bất thường) | Không tự retry, trừ `RATE_LIMITED`/`QUEUE_REQUIRED` theo bảng mục 6 | `SEATS_UNAVAILABLE`, `EVENT_NOT_ON_SALE` |
| `TRANSIENT` (hạ tầng tạm thời) | Hạ tầng hoặc bên thứ ba quá tải hoặc không trả lời; thử lại sau có thể thành công | 503 (429 cho rate limit) | WARN, kèm tên phụ thuộc, không stack trace dài | Có: backoff, cùng `Idempotency-Key` | `OVERLOADED`, `PAYMENT_PROVIDER_UNAVAILABLE` |
| `DEFECT` (lập trình) | Bug, bất biến bị vi phạm, cấu hình sai, exception không lường trước | 500 `INTERNAL_ERROR` | ERROR, đủ stack trace, `trace_id` | Không | `NullPointerException`, `DataIntegrityViolationException` không mong đợi |

Quy tắc phân loại:

1. Lỗi nghiệp vụ là kết quả bình thường của hệ thống đang chạy đúng (người thứ hai thua một ghế). Không bao giờ ở mức ERROR và không làm tăng alert.
2. Lỗi ràng buộc database **không** tự động là lỗi nghiệp vụ. Service bắt `DuplicateKeyException` ở đúng chỗ đã dự kiến (ví dụ `reservation` trùng người-sự kiện, DR-41) và ném exception nghiệp vụ; mọi chỗ khác để rơi xuống `DEFECT`.
3. Timeout và hết permit của bulkhead (DR-61) là `TRANSIENT`; mất kết nối database hẳn cũng là `TRANSIENT` (503 `OVERLOADED`) chứ không phải `DEFECT`, vì client thử lại được.
4. Webhook là ngoại lệ: lỗi xử lý bất kỳ trả 500 để Stripe gửi lại (DR-48), kể cả lỗi nghiệp vụ không lường trước; chỉ sai chữ ký trả 400.

## 2. Cây exception

Mọi exception nghiệp vụ kế thừa `common.error.DomainException` (DOC-12 §4). Service không trả `ResponseEntity` và không tự dựng Problem Details; `@RestControllerAdvice` `ApiExceptionHandler` trong `common.error` làm việc đó.

```java
package io.ticket.common.error;

public enum ErrorClass { BUSINESS, TRANSIENT, DEFECT }

/** Exception nghiệp vụ: code chữ hoa SNAKE_CASE, status HTTP cố định theo code. */
public abstract class DomainException extends RuntimeException {
    private final ErrorCode code;                  // enum ErrorCode, mục 3
    private final Map<String, Object> extensions;  // thành viên mở rộng của Problem Details (mục 4)
    protected DomainException(ErrorCode code, String message, Map<String, Object> extensions) { … }
    public ErrorCode code() { return code; }
    public HttpStatus status() { return code.status(); }
    public ErrorClass errorClass() { return code.errorClass(); }
}

/** Hạ tầng tạm thời: luôn 503 hoặc 429, mang retryAfterSeconds. */
public class TransientException extends DomainException {
    private final Integer retryAfterSeconds;       // null → không đặt header Retry-After
}
```

`ErrorCode` là enum duy nhất chứa 40 mã của mục 3 (`status`, `errorClass`, key i18n `errors.<code viết thường>` (giao diện) và `problem.<code viết thường>.title|detail` (backend)). Thêm mã = thêm một giá trị enum, một dòng ở bảng mục 3, một dòng ở DOC-40 và một test (PR checklist DOC-12 mục 9).

Các exception cụ thể (tên là class trong package gốc của module nếu thuộc hợp đồng `…Api`, ngược lại ở `service`):

| Exception | Module | Mã (`ErrorCode`) | Thành viên mở rộng |
| --- | --- | --- | --- |
| `SeatsUnavailableException` | `inventory` | `SEATS_UNAVAILABLE` | `unavailableSeatIds` |
| `InsufficientCapacityException` | `inventory` | `INSUFFICIENT_CAPACITY` | `poolId`, `requested`, `available` |
| `ActiveReservationExistsException` | `reservation` | `ACTIVE_RESERVATION_EXISTS` | `reservationId` |
| `ReservationNotActiveException` | `reservation` | `RESERVATION_NOT_ACTIVE` | `status` |
| `PaymentWindowTooShortException` | `payment` | `PAYMENT_WINDOW_TOO_SHORT` | `remainingSeconds` |
| `PaymentAlreadySucceededException` | `payment` | `PAYMENT_ALREADY_SUCCEEDED` | `orderId` |
| `EventNotOnSaleException` | `catalog` | `EVENT_NOT_ON_SALE` | `displayStatus` |
| `EventStateConflictException` | `catalog` | `EVENT_STATE_CONFLICT` | `status` |
| `StaleEventVersionException` | `catalog` | `STALE_EVENT_VERSION` | `currentRowVersion` |
| `PublishPreconditionsFailedException` | `catalog` | `PUBLISH_PRECONDITIONS_FAILED` | `checks` |
| `MapLockedAfterSaleException` | `seatmap` | `MAP_LOCKED_AFTER_SALE` | `saleStartsAt` |
| `MapValidationFailedException` | `seatmap` | `MAP_VALIDATION_FAILED` | `issues` |
| `RevisionConflictException` | `seatmap` | `REVISION_CONFLICT` | `currentRevision` |
| `ValidationFailedException` | mọi module | `VALIDATION_FAILED` | `errors[]` |
| `NotFoundException` | mọi module | `NOT_FOUND` | — |
| `ForbiddenException` | `identity` | `FORBIDDEN`, `ORGANIZER_PROFILE_REQUIRED` | — |
| `UnauthenticatedException` | `identity` | `UNAUTHENTICATED`, `LOGIN_LINK_INVALID` | — |
| `OverloadedException` (con của `TransientException`) | `common` | `OVERLOADED` | `retryAfterSeconds` |
| `ProviderUnavailableException` (con của `TransientException`) | `payment`, `identity` | `PAYMENT_PROVIDER_UNAVAILABLE`, `EMAIL_PROVIDER_UNAVAILABLE` | `retryAfterSeconds` |

Tên module theo [DOC-07](../03-architecture/system-context-and-containers.md) §4; các mã còn lại (mục 3) dùng một exception một mã cùng quy ước đặt tên `<CodeInPascalCase>Exception`.

## 3. Bảng mã lỗi

Cột "Nguồn": **SDD** = SDD gốc 12.3, **DR-64** = bổ sung của DR-64, **mới** = bổ sung của tài liệu này (mục 9). Cột `type` luôn là `https://errors.ticket.dev/<code>` (DOC-36 §5). Cột lớp: B = `BUSINESS`, T = `TRANSIENT`, D = `DEFECT`.

| HTTP | `code` | Lớp | Khi nào | Nguồn |
| --- | --- | --- | --- | --- |
| 400 | `IDEMPOTENCY_KEY_REQUIRED` | B | Thiếu header `Idempotency-Key` ở endpoint bắt buộc (DR-45) | DR-64 |
| 400 | `BAD_REQUEST` | B | JSON hỏng, thiếu body, tham số query sai kiểu, `cursor` không giải mã được (`rule = invalid_cursor`) | mới |
| 400 | `INVALID_SIGNATURE` | B | Webhook Stripe sai `Stripe-Signature` hoặc quá dung sai 300 giây (DR-48) | mới |
| 401 | `UNAUTHENTICATED` | B | Chưa đăng nhập hoặc session hết hạn/bị thu hồi | SDD |
| 401 | `LOGIN_LINK_INVALID` | B | Token magic link hết hạn, đã dùng hoặc bị thay (DR-21) | DR-64 |
| 403 | `FORBIDDEN` | B | Không đủ quyền | SDD |
| 403 | `ORGANIZER_PROFILE_REQUIRED` | B | Vào `/organizer/**` khi chưa có hồ sơ tổ chức (DR-23) | DR-64 |
| 403 | `CSRF_TOKEN_INVALID` | B | Thiếu hoặc sai `X-CSRF-Token` ở `POST/PUT/PATCH/DELETE` có session (DR-22) | mới |
| 404 | `NOT_FOUND` | B | Không có tài nguyên, hoặc tài nguyên của tổ chức khác (không lộ tồn tại, DR-23) | DR-64 |
| 405 | `METHOD_NOT_ALLOWED` | B | Sai phương thức HTTP cho đường dẫn | mới |
| 409 | `SEATS_UNAVAILABLE` | B | Ghế đã bị lấy; kèm `unavailableSeatIds` | SDD |
| 409 | `INSUFFICIENT_CAPACITY` | B | Pool không đủ vé; kèm `poolId`, `requested`, `available` | SDD |
| 409 | `RESERVATION_NOT_ACTIVE` | B | Reservation đã `EXPIRING`/đóng khi tạo PaymentIntent, xác nhận đơn 0 đồng | SDD |
| 409 | `REVISION_CONFLICT` | B | `PUT …/draft` với `revision` cũ: tab khác đã ghi (DR-36) | SDD |
| 409 | `EVENT_NOT_ON_SALE` | B | Giữ vé ngoài khung bán, `PAUSED`, `DRAFT`, `ENDED`, `CANCELLED`, `SALE_CLOSED`; kèm `displayStatus` | DR-64 |
| 409 | `ACTIVE_RESERVATION_EXISTS` | B | Đã có reservation `ACTIVE`/`EXPIRING` cho sự kiện; kèm `reservationId` | DR-64 |
| 409 | `PAYMENT_WINDOW_TOO_SHORT` | B | Tạo PaymentIntent khi còn < 30 giây (DR-47) | DR-64 |
| 409 | `PAYMENT_ALREADY_SUCCEEDED` | B | Hủy giữ khi PaymentIntent đã `succeeded` (DR-43) | DR-64 |
| 409 | `EVENT_STATE_CONFLICT` | B | Chuyển trạng thái sự kiện không hợp lệ (publish, pause, resume, close-sale, cancel) | DR-64 |
| 409 | `STALE_EVENT_VERSION` | B | `PATCH` với `rowVersion` cũ (DR-70) | DR-64 |
| 409 | `ORGANIZER_EXISTS` | B | Lập hồ sơ tổ chức lần hai | DR-64 |
| 409 | `TICKET_TYPE_IN_USE` | B | Xóa loại vé đã có unit không `AVAILABLE`/đã bán (DR-26) | DR-64 |
| 409 | `CAPACITY_BELOW_USED` | B | Giảm sức chứa thấp hơn số `HELD`+`SOLD`; kèm `used` (DR-30) | DR-64 |
| 409 | `MAP_LOCKED_AFTER_SALE` | B | Sửa hoặc xuất bản sơ đồ từ giờ mở bán (DR-37) | DR-64 |
| 409 | `MAP_PUBLISH_BUSY` | B | Xuất bản sơ đồ hết `lock_timeout` (DR-37) | DR-64 |
| 409 | `MAP_ALREADY_EXISTS` | B | Nhân bản sơ đồ vào event đã có sơ đồ (DR-31) | DR-64 |
| 413 | `PAYLOAD_TOO_LARGE` | B | Tài liệu sơ đồ > 5 MB, ảnh vượt giới hạn, body > `client_max_body_size` | DR-64 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | B | `Content-Type` không phải `application/json` (hoặc `multipart/form-data` ở upload ảnh) | mới |
| 422 | `VALIDATION_FAILED` | B | Dữ liệu sai; kèm `errors: [{field, rule, params?}]` (DR-25, DR-41) | SDD |
| 422 | `IDEMPOTENCY_KEY_REUSED` | B | Cùng `Idempotency-Key` với `request_hash` khác (DR-45) | SDD |
| 422 | `PUBLISH_PRECONDITIONS_FAILED` | B | Xuất bản khi điều kiện chưa đạt; kèm `checks` | DR-64 |
| 422 | `MAP_VALIDATION_FAILED` | B | Sơ đồ có vấn đề mức `error`; kèm `issues` (DR-35) | DR-64 |
| 422 | `TICKET_TYPE_LIMIT_REACHED` | B | Đã đủ 5 loại vé (DR-26) | DR-64 |
| 422 | `MEDIA_INVALID` | B | Ảnh sai loại, kích thước, nội dung (DR-38) | DR-64 |
| 429 | `RATE_LIMITED` | T | Vượt rate limit nginx hoặc token bucket theo người dùng (DR-55, DR-56); kèm `Retry-After` | SDD |
| 429 | `QUEUE_REQUIRED` | B | Lệnh giữ vé của sự kiện có kiểm soát tiếp nhận khi chưa có lượt vào hợp lệ (DR-58) | SDD |
| 500 | `INTERNAL_ERROR` | D | Lỗi không lường trước; kèm `requestId` (màn E5) | DR-64 |
| 503 | `OVERLOADED` | T | Hết permit bulkhead, hết connection, hàng đợi đầy, mất Redis ở sự kiện `high_demand` (DR-56, DR-61); kèm `Retry-After` | SDD |
| 503 | `PAYMENT_PROVIDER_UNAVAILABLE` | T | Stripe lỗi hoặc timeout khi tạo PaymentIntent (DR-47) | DR-64 |
| 503 | `EMAIL_PROVIDER_UNAVAILABLE` | T | SMTP lỗi khi gửi magic link (DR-21); không có dòng outbox nào | DR-64 |

Tổng: 11 mã của SDD gốc + 24 của DR-64 + 5 mới = 40.

Ghi chú về `rule` của `VALIDATION_FAILED`: giá trị `rule` là `snake_case` chữ thường (`after_event_start`, `too_many_units`, `invalid_timezone`, `locked_after_publish`) và là phần của `errors[]`, không phải `code`. Các chỗ DR-12 và DR-25 viết "422 `invalid_timezone`" nghĩa là `422 VALIDATION_FAILED` với `errors: [{ "field": "timezone", "rule": "invalid_timezone" }]`. Client tra key `validation.<rule>` (DOC-40).

## 4. Thành viên mở rộng của Problem Details

Thành viên chuẩn (`type`, `title`, `status`, `detail`, `code`, `requestId`) ở DOC-36 §5. Thành viên mở rộng theo từng mã; tên camelCase, không bao giờ chứa dữ liệu cá nhân của người khác:

| `code` | Thành viên mở rộng | Ví dụ |
| --- | --- | --- |
| `SEATS_UNAVAILABLE` | `unavailableSeatIds: uuid[]` | `["0199a3f2-5b7c-7a10-9c2e-6d1f0a4b8c11"]` |
| `INSUFFICIENT_CAPACITY` | `poolId`, `requested`, `available` (số nguyên) | `"requested": 4, "available": 2` |
| `ACTIVE_RESERVATION_EXISTS` | `reservationId` | để giao diện chuyển sang `/checkout/:id` |
| `EVENT_NOT_ON_SALE` | `displayStatus` (DR-24), `saleStartsAt` khi `UPCOMING` | `"displayStatus": "UPCOMING"` |
| `EVENT_STATE_CONFLICT` | `status` (trạng thái lưu hiện tại) | `"status": "CANCELLED"` |
| `STALE_EVENT_VERSION` | `currentRowVersion` | `7` |
| `REVISION_CONFLICT` | `currentRevision` | `42` |
| `CAPACITY_BELOW_USED` | `used` = `HELD` + `SOLD` | `"used": 118` |
| `PUBLISH_PRECONDITIONS_FAILED` | `checks: [{ "key", "ok", "params"? }]` (chỉ liệt kê mục chưa đạt cũng được; thứ tự theo bước studio) | `{ "key": "has_ticket_type", "ok": false }` |
| `MAP_VALIDATION_FAILED` | `issues: [{ "code", "level", "objectIds", "params" }]` (DR-35) | `SEAT_OVERLAP` |
| `VALIDATION_FAILED` | `errors: [{ "field", "rule", "params"? }]` | `{ "field": "saleEndsAt", "rule": "after_event_start" }` |
| `PAYMENT_WINDOW_TOO_SHORT` | `remainingSeconds` | `12` |
| `RATE_LIMITED`, `OVERLOADED`, `PAYMENT_PROVIDER_UNAVAILABLE`, `EMAIL_PROVIDER_UNAVAILABLE` | `retryAfterSeconds` (số nguyên ≥ 1) — trùng giá trị header `Retry-After` | `3` |
| `QUEUE_REQUIRED` | `queueUrl` = `/events/{eventId}/queue` | giao diện về Phòng chờ |

Ví dụ đầy đủ của một lỗi nghiệp vụ:

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json
Content-Language: vi
X-Request-Id: 9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b
X-Server-Time: 1795050601234

{
  "type": "https://errors.ticket.dev/SEATS_UNAVAILABLE",
  "title": "Ghế không còn trống",
  "status": 409,
  "detail": "1 trong 3 ghế bạn chọn vừa được người khác giữ.",
  "code": "SEATS_UNAVAILABLE",
  "requestId": "9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b",
  "unavailableSeatIds": ["0199a3f2-5b7c-7a10-9c2e-6d1f0a4b8c11"]
}
```

## 5. Quy tắc ánh xạ và ghi log

### 5.1 Thứ tự xử lý trong `ApiExceptionHandler`

| Thứ tự | Exception bắt được | Kết quả |
| --- | --- | --- |
| 1 | `DomainException` (kể cả `TransientException`) | `code`, status, extensions từ exception; `Retry-After` khi có `retryAfterSeconds` |
| 2 | `MethodArgumentNotValidException`, `ConstraintViolationException`, `HandlerMethodValidationException` | 422 `VALIDATION_FAILED`; mỗi vi phạm thành `{field, rule}` với `rule` lấy từ tên annotation (`NotBlank` → `required`, `Size` → `size`, `Pattern` → `pattern`) hoặc `message` của constraint tùy biến |
| 3 | `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException` | 400 `BAD_REQUEST` |
| 4 | `NoResourceFoundException`, `NoHandlerFoundException` | 404 `NOT_FOUND` |
| 5 | `HttpRequestMethodNotSupportedException` | 405 `METHOD_NOT_ALLOWED`, header `Allow` |
| 6 | `HttpMediaTypeNotSupportedException` | 415 `UNSUPPORTED_MEDIA_TYPE` |
| 7 | `MaxUploadSizeExceededException` | 413 `PAYLOAD_TOO_LARGE` |
| 8 | `AuthenticationException` (từ filter session) | 401 `UNAUTHENTICATED` |
| 9 | `AccessDeniedException` | 403 `FORBIDDEN`; CSRF sai → 403 `CSRF_TOKEN_INVALID` |
| 10 | `CannotGetJdbcConnectionException`, `QueryTimeoutException`, `TransientDataAccessException`, `RejectedExecutionException`, permit bulkhead hết | 503 `OVERLOADED`, `Retry-After` 1–3 giây ngẫu nhiên (DR-61) |
| 11 | `com.stripe.exception.ApiConnectionException`, `RateLimitException`, `APIException` 5xx, timeout | 503 `PAYMENT_PROVIDER_UNAVAILABLE`, `Retry-After: 3` (chỉ ở endpoint tạo PaymentIntent; nơi khác xem mục 5.3) |
| 12 | `jakarta.mail.MessagingException`, `MailException` ở luồng magic link | 503 `EMAIL_PROVIDER_UNAVAILABLE`, `Retry-After: 5` |
| 13 | Mọi `Throwable` còn lại (`DataIntegrityViolationException`, `NullPointerException`…) | 500 `INTERNAL_ERROR`, response không lộ chi tiết |

Webhook `POST /webhooks/stripe` có `@ExceptionHandler` riêng ưu tiên cao hơn: `SignatureVerificationException` → 400 `INVALID_SIGNATURE`; mọi exception khác sau khi qua chữ ký → rollback và 500 (không phụ thuộc lớp), để Stripe gửi lại (DR-48).

### 5.2 Log theo lớp

| Lớp | Mức | Nội dung log | Metric |
| --- | --- | --- | --- |
| `BUSINESS` | INFO; WARN cho 401, 403, 429 `QUEUE_REQUIRED`, 413, `IDEMPOTENCY_KEY_REUSED` | `message` = `code`, trường `error.code`, `http.status`; **không** stack trace | `ticket_http_errors_total{code}` |
| `TRANSIENT` | WARN | `code`, `dependency` (`postgres`, `redis`, `stripe`, `smtp`, `s3`), `duration_ms`, `retry_after_s`; stack trace chỉ ở DEBUG | `ticket_http_errors_total{code}`, `ticket_dependency_failures_total{dependency}` |
| `DEFECT` | ERROR | Stack trace đầy đủ, `trace_id`, `user_id`, `event_id`, `reservation_id`, `order_id` nếu có | `ticket_http_errors_total{code="INTERNAL_ERROR"}` |

Luật chung (DR-09, DR-22): email không bao giờ vào log và vào `detail`; không log body request (có thể chứa token đăng nhập hoặc secret); `requestId` trong response trùng `trace_id` trong log nên người dùng báo mã là tìm được. Metric `ticket_http_errors_total` do tài liệu này đề xuất, nguồn chuẩn ở DOC-33.

### 5.3 Lỗi ngoài đường request

| Chỗ xảy ra | Hành vi |
| --- | --- |
| Job (`@Scheduled`) ném exception | Bắt ở mức job: log ERROR (`DEFECT`) hoặc WARN (`TRANSIENT`), không dừng lịch; lượt sau chạy lại. Lô đang xử lý rollback nguyên lô (DR-42) |
| `OutboxRelay` gửi SMTP lỗi | Tăng `attempts`, backoff, sau 8 lần `FAILED` (DR-53); log WARN mỗi lần, ERROR khi `FAILED` |
| Hủy PaymentIntent lỗi trong job trả vé | Reservation giữ `EXPIRING`, lượt sau thử lại; WARN (DR-42, DR-43) |
| Hủy PaymentIntent lỗi ở đường nhanh | Trả 202 và để job hoàn tất, không phải 503 (DR-43) |
| `PaymentReconcileJob` hỏi Stripe lỗi | WARN, bỏ qua đơn đó đến lượt sau (DR-49) |
| Redis lỗi ở bucket theo người dùng | Bỏ qua bucket, WARN có giới hạn tần suất; không có limiter dự phòng (DR-56) |

### 5.4 Quy tắc viết thông báo

- `title` và `detail` lấy từ `MessageSource` key `problem.<code viết thường>.title` / `problem.<code viết thường>.detail` theo `Accept-Language` đã giải quyết (DOC-31); thiếu key `vi` là lỗi build (test `ERR-12`). `code` luôn tiếng Anh, không dịch.
- `detail` nói người dùng làm gì tiếp; không chứa SQL, tên bảng, tên class, giá trị secret, email.
- `INTERNAL_ERROR` luôn trả cùng một `detail` chung; chi tiết chỉ ở log.

## 6. Retry phía client

Mở rộng bảng SDD gốc 10.5. Backoff: `delay = min(30 s, base × 2^n) × random(0,5…1,5)` (jitter), `base` = 1 giây, tối đa 5 lần với lệnh giữ vé và PaymentIntent; chờ ít nhất `Retry-After`; **cùng `Idempotency-Key` và cùng chuỗi body** ở mọi lần thử lại (DR-45). Server không bao giờ tự retry lệnh giữ vé thay client.

| Phản hồi | Client làm gì | Giao diện |
| --- | --- | --- |
| 409 `SEATS_UNAVAILABLE`, `INSUFFICIENT_CAPACITY` | Không retry; tải lại tình trạng chỗ và xóa ghế bị lấy khỏi lựa chọn | Thông báo trong màn Chọn chỗ, giữ phần lựa chọn còn lại |
| 409 `ACTIVE_RESERVATION_EXISTS` | Không retry; chuyển sang `/checkout/{reservationId}` | Thông báo "đang có lượt giữ vé" |
| 409 `EVENT_NOT_ON_SALE` | Không retry; cập nhật `displayStatus` | Về trang sự kiện |
| 409 `RESERVATION_NOT_ACTIVE`, `PAYMENT_WINDOW_TOO_SHORT` | Không retry; gọi `GET /reservations/{id}` rồi quay lại Chọn chỗ hoặc màn Kết quả | Màn hết hạn giữ vé |
| 409 `PAYMENT_ALREADY_SUCCEEDED` | Không retry; chuyển sang `/orders/{orderId}` | Màn Kết quả |
| 409 `STALE_EVENT_VERSION`, `REVISION_CONFLICT` | Không retry; hộp thoại tải lại (DR-70, DR-36) | Hộp thoại studio |
| 401 `UNAUTHENTICATED` | Không retry; lưu lựa chọn, chuyển `/login?returnTo=` (DR-67) | Màn hết phiên |
| 401 `LOGIN_LINK_INVALID` | Không retry | Màn "link hết hạn", nút gửi lại |
| 403, 404, 413, 415, 422 | Không retry | Thông báo theo mã |
| 429 `QUEUE_REQUIRED` | Không retry; về Phòng chờ (`queueUrl`) | Phòng chờ |
| 429 `RATE_LIMITED`, 503 `OVERLOADED` | Retry có backoff, tôn trọng `Retry-After`; sau 5 lần hiện lỗi 503 tự thử lại (E4) | Thanh tiến trình tự thử lại |
| 503 `PAYMENT_PROVIDER_UNAVAILABLE` | Retry như trên; hết lượt thì nút "Thử lại" thủ công | Thông báo ở màn Thanh toán |
| 503 `EMAIL_PROVIDER_UNAVAILABLE` | Không tự retry; nút "Gửi lại" | Màn Đăng nhập |
| 500 `INTERNAL_ERROR` | Không retry tự động; hiện `requestId` | Trang E5 |
| Timeout mạng, mất kết nối | Retry cùng key; server trả kết quả cũ nếu lần trước đã thành công (`Idempotent-Replayed: true`) | Giữ nguyên màn hình |
| Hết hạn đồng hồ giữ vé về 0 | Gọi lại API thay vì tự kết luận (DR-66) | — |

## 7. Ánh xạ mã lỗi → thông báo

Mỗi mã có đúng một key i18n `errors.<code viết thường>` (giao diện) và `problem.<code viết thường>.title|detail` (backend) (cho giao diện: `title` và `body`), bảng chuỗi `vi`/`en` ở [DOC-40](../08-ux-ui/ui-states-and-copy.md). Tài liệu này giữ quy tắc chọn **kiểu hiển thị**:

| Kiểu | Mã | Cách hiển thị |
| --- | --- | --- |
| Trang toàn màn | `NOT_FOUND` (E1), `UNAUTHENTICATED` (E2), `FORBIDDEN`/`ORGANIZER_PROFILE_REQUIRED` (E3), `OVERLOADED`/`RATE_LIMITED` kéo dài (E4), `INTERNAL_ERROR` (E5) | Trang lỗi (DOC-52) |
| Thông báo trong màn | `SEATS_UNAVAILABLE`, `INSUFFICIENT_CAPACITY`, `ACTIVE_RESERVATION_EXISTS`, `EVENT_NOT_ON_SALE`, `PAYMENT_*`, `RESERVATION_NOT_ACTIVE`, `QUEUE_REQUIRED` | Banner hoặc toast trong màn đang dùng |
| Lỗi theo ô nhập | `VALIDATION_FAILED` | Dưới từng ô, key `validation.<rule>` |
| Hộp thoại | `STALE_EVENT_VERSION`, `REVISION_CONFLICT`, `MAP_LOCKED_AFTER_SALE`, `CAPACITY_BELOW_USED`, `TICKET_TYPE_IN_USE` | Hộp thoại xác nhận (DOC-39) |
| Danh sách vấn đề | `PUBLISH_PRECONDITIONS_FAILED`, `MAP_VALIDATION_FAILED` | Danh sách điều kiện / vấn đề (studio) |
| Không hiển thị | `INVALID_SIGNATURE`, `BAD_REQUEST`, `METHOD_NOT_ALLOWED`, `UNSUPPORTED_MEDIA_TYPE`, `CSRF_TOKEN_INVALID` | Lỗi lập trình ở client: coi như `INTERNAL_ERROR` kèm `requestId`; `CSRF_TOKEN_INVALID` thì client gọi `GET /me` lấy token mới rồi thử lại **một** lần |

Mã lạ (server mới hơn client): client dùng `error.UNKNOWN` kèm `requestId`.

## 8. Test bắt buộc

Tiền tố `ERR-` (đã kiểm tra chưa dùng; đăng ký ở DOC-69).

| ID | Kịch bản | Kết quả mong đợi |
| --- | --- | --- |
| ERR-01 | Duyệt enum `ErrorCode` | Đúng 40 giá trị; mỗi giá trị có dòng ở bảng mục 3 (so với file này bằng test đọc markdown) và key `problem.<code viết thường>.title` ở cả `messages_vi` và `messages_en` |
| ERR-02 | Ném từng `DomainException` qua một controller thử | Status, `code`, `type = https://errors.ticket.dev/<code>`, `requestId` khớp bảng; `Content-Type: application/problem+json` |
| ERR-03 | `POST` JSON thiếu trường bắt buộc | 422 `VALIDATION_FAILED`, `errors[0] = {field, rule:"required"}`; không có stack trace trong body |
| ERR-04 | `POST` body `{"items":` (JSON hỏng) | 400 `BAD_REQUEST` |
| ERR-05 | `GET /api/v1/events?cursor=!!!` | 400 `BAD_REQUEST`, `errors[0].rule = invalid_cursor` |
| ERR-06 | Service ném `NullPointerException` | 500 `INTERNAL_ERROR`; body không chứa tên class; log ERROR có stack trace và `trace_id` bằng `requestId` |
| ERR-07 | Hết permit bulkhead giữ vé (giả lập 0 permit) | 503 `OVERLOADED`, header `Retry-After` ∈ {1,2,3}, `retryAfterSeconds` trùng header; log WARN, không ERROR |
| ERR-08 | Stripe giả trả lỗi kết nối khi tạo PaymentIntent | 503 `PAYMENT_PROVIDER_UNAVAILABLE`; không có dòng `orders` đổi trạng thái |
| ERR-09 | Webhook sai chữ ký | 400 `INVALID_SIGNATURE`; không có dòng `stripe_event` |
| ERR-10 | Webhook đúng chữ ký, handler ném lỗi | 500, rollback, `stripe_event` không có dòng |
| ERR-11 | Cùng một request với `Accept-Language: en` và `vi` | `title`/`detail` đổi theo locale, `code` giống nhau; không có `Accept-Language` → `vi` |
| ERR-12 | Thiếu một key `problem.<code viết thường>.*` trong `messages_vi` | Test build đỏ |
| ERR-13 | Mọi mã `BUSINESS` | Không có log mức ERROR; không có stack trace trong log |
| ERR-14 | `POST` với CSRF sai khi đã có session | 403 `CSRF_TOKEN_INVALID`, không có tác dụng phụ |
| ERR-15 | `PUT` vào đường dẫn chỉ cho `GET` | 405 `METHOD_NOT_ALLOWED`, header `Allow: GET` |
| ERR-16 | Body 6 MB tới `PUT …/draft` | 413 `PAYLOAD_TOO_LARGE` (nginx trả đúng Problem Details, DOC-62) |

## 9. Quyết định mới khi viết tài liệu này

Mọi quyết định dưới đây do Claude chốt theo ủy quyền của Owner (master plan §0.1) và là đề xuất DR để bên gọi gán số.

- **DR-82 · Mã lỗi bổ sung cho lỗi kỹ thuật chung.** *Vấn đề:* SDD gốc 12.3 và DR-64 không có mã cho JSON hỏng, sai phương thức, sai `Content-Type`, sai chữ ký webhook và sai CSRF; không có mã thì bộ xử lý phải bịa hoặc rơi vào `INTERNAL_ERROR`, sai về lớp lỗi. *Quyết định:* thêm 5 mã: 400 `BAD_REQUEST`, 400 `INVALID_SIGNATURE`, 403 `CSRF_TOKEN_INVALID`, 405 `METHOD_NOT_ALLOWED`, 415 `UNSUPPORTED_MEDIA_TYPE` (mục 3). *Hệ quả:* bảng có 40 mã; mỗi mã có key `errors.<code viết thường>` (giao diện) và `problem.<code viết thường>.*` (backend) trong DOC-40. *Ghi vào:* DOC-35, DOC-36, DOC-40.
- **DR-83 · `rule` của `VALIDATION_FAILED` là `snake_case` và không phải `code`.** *Vấn đề:* DR-12 và DR-25 viết "422 `invalid_timezone`", lẫn `rule` với `code`. *Quyết định:* mọi giá trị chữ thường như vậy là `rule` trong `errors[]`; `code` luôn chữ hoa (mục 3). *Hệ quả:* không thêm mã lỗi, test `ERR-03`. *Ghi vào:* DOC-35, DOC-20, DOC-40.
- **DR-84 · `retryAfterSeconds` trong body trùng `Retry-After`.** *Vấn đề:* SPA đọc header `Retry-After` qua CORS-less fetch được nhưng body tiện hơn cho interceptor và test. *Quyết định:* mọi 429/503 có cả header và thành viên `retryAfterSeconds` cùng giá trị nguyên giây ≥ 1. *Hệ quả:* nginx `RATE_LIMITED` (DR-55) cũng phải đặt hai nơi (DOC-62). *Ghi vào:* DOC-35, DOC-36, DOC-62.
- **DR-85 · Một lần retry cho `CSRF_TOKEN_INVALID`.** *Quyết định:* client gọi `GET /me` lấy `csrfToken` mới rồi gửi lại đúng một lần; lần hai vẫn lỗi coi như lỗi lập trình (mục 7). *Ghi vào:* DOC-35, DOC-19.

## 10. Câu hỏi còn mở

Không có. Bốn quyết định ở mục 9 do Claude chốt theo ủy quyền; chờ gán số DR.
