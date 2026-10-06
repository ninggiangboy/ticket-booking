# Hướng dẫn API

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-36
> Phụ thuộc: SDD gốc §12, §10.5, [DOC-06](../02-glossary.md), [DOC-35](../06-design/error-handling.md), [DOC-11](../03-architecture/tech-stack-and-versions.md), [DOC-12](../03-architecture/code-architecture.md) §4, [Sổ quyết định](../00-decision-register.md) (DR-10, 12, 13, 22, 45, 55, 62, 63, 64, 66, 67, 77)
> Người dùng chính: P1-01 (khung `common`), P1-04, P1-06 (sinh `schema.d.ts`); [DOC-37](api-endpoints.md) khi viết từng `E-xx`; mọi task backend và frontend gọi API

Tài liệu chốt các quy ước chung của REST API: đường dẫn, JSON, thời gian, tiền, lỗi, phân trang, header, cache, OpenAPI và sinh client. Từng endpoint nằm ở [DOC-37](api-endpoints.md); bảng mã lỗi và exception nằm ở [DOC-35](../06-design/error-handling.md); ma trận quyền ở DOC-32. Quyết định mới khi viết tài liệu này là DR-82…89, tóm tắt ở mục 11.

## 1. Đường dẫn và phương thức

| Quy ước | Quy tắc |
| --- | --- |
| Tiền tố | `/api/v1`. Mọi đường dẫn trong SDD gốc hiểu là sau tiền tố; nginx proxy `/api/` tới `api:8080` (DR-63) |
| Ngoại lệ không tiền tố | `GET /media/{id}` (ảnh, cache dài ở nginx, DR-65); `/v3/api-docs` (OpenAPI, chỉ mạng nội bộ compose và profile `dev`); cổng quản trị 9090 không phải API |
| Đường dẫn sơ đồ | `/organizer/maps/…` (không phải `/maps/…`, DR-63) |
| Đặt tên | Danh từ số nhiều, chữ thường, gạch nối: `/events`, `/ticket-types`, `/publish-checks`. Tham số đường dẫn camelCase: `{eventId}` trong ví dụ, `{id}` khi không mơ hồ |
| Hành động không phải CRUD | `POST /<resource>/{id}/<verb>` với động từ kebab-case: `publish`, `pause`, `resume`, `close-sale`, `cancel`, `confirm-free`, `payment-intent`, `clone`, `validate` |
| Phạm vi người dùng | `/me`, `/me/orders`, `/me/tickets` luôn là của session hiện tại; `/organizer/**` lấy `organizer_id` từ session, không nhận từ client (DR-23) |
| Phiên bản | `v1` cố định trong giai đoạn này; thay đổi phá vỡ phải qua `oasdiff` (mục 10) và chỉ được vào khi cả client sinh ra cùng PR |

Phương thức và mã thành công:

| Phương thức | Dùng cho | Thành công |
| --- | --- | --- |
| `GET` | Đọc, không tác dụng phụ | 200; `GET /events/{id}/map` không `version` → 302 (DR-65) |
| `POST` | Tạo tài nguyên hoặc hành động | 201 kèm `Location` khi tạo tài nguyên (`/api/v1/reservations/{id}`); 200 với hành động trả kết quả; 202 khi việc hoàn tất sau (hủy giữ vé khi Stripe lỗi, DR-43; gửi magic link) |
| `PUT` | Thay toàn bộ (`…/draft`, có `revision`) | 200 kèm trạng thái mới |
| `PATCH` | Sửa một phần (`/organizer/events/{id}` kèm `rowVersion`, `/me`) | 200 kèm đối tượng đầy đủ |
| `DELETE` | Xóa hoặc hủy | 204; 200/202 khi có thân (`DELETE /reservations/{id}` trả trạng thái hiện tại, DR-43) |

`HEAD` và `OPTIONS` không dùng; không có CORS vì cùng origin (DOC-32).

## 2. JSON

- `Content-Type: application/json; charset=utf-8` cho request và response; upload ảnh `multipart/form-data` (một phần `file`). Khác → 415 `UNSUPPORTED_MEDIA_TYPE`.
- **camelCase** cho mọi trường; enum là chuỗi `UPPER_SNAKE_CASE` (`PENDING_PAYMENT`, `SEAT`); trường tập hợp số nhiều.
- Trường không có giá trị trả `null`, **không bỏ trường** (DR-63); client không phân biệt "thiếu" và `null`. Mảng rỗng là `[]`, không bao giờ `null`.
- Body request **chặt**: trường lạ → 400 `BAD_REQUEST` (Jackson `FAIL_ON_UNKNOWN_PROPERTIES = true`), trường thiếu hoặc sai kiểu → 422 `VALIDATION_FAILED`, JSON hỏng → 400 `BAD_REQUEST` (DR-82). Ngoại lệ: tài liệu sơ đồ (`document`) được validate bằng JSON Schema (DR-32), không bằng Jackson.
- ID là chuỗi UUID (UUIDv7, chữ thường, có gạch) trừ khóa nội dung do client sinh trong sơ đồ (`zone-ab12cd34`, DR-32) và ID Stripe. Không bao giờ dùng số tự tăng.
- Số nguyên 64 bit (tiền, `sold`) là số JSON; chỉ tiền lớn nhất 100.000.000 và sức chứa ≤ 100.000 nên nằm gọn trong 2^53 của JavaScript.
- Chuỗi: UTF-8, không cắt khoảng trắng ngầm ở server (client `trim` ô nhập; server từ chối chuỗi chỉ gồm khoảng trắng ở trường bắt buộc).
- Tên enum, tên trường và `operationId` là hợp đồng; đổi là thay đổi phá vỡ.

## 3. Thời gian

| Hướng | Định dạng | Ví dụ |
| --- | --- | --- |
| Response | ISO 8601 UTC kết thúc bằng `Z`, giây; phần mili giây chỉ ở trường mà sai số quan trọng (`generatedAt`, `expiresAt`) | `2026-11-14T13:00:00Z`, `2026-11-10T03:00:01.200Z` |
| Request thông thường | ISO 8601 UTC kết thúc bằng `Z` | `"expiresAt"` không bao giờ do client gửi |
| Request của studio (giờ người tổ chức nhập) | Giờ địa phương không offset `yyyy-MM-dd'T'HH:mm` kèm `timezone` của event; server đổi sang UTC (DR-12) | `"startsAtLocal": "2026-11-14T20:00", "timezone": "Asia/Ho_Chi_Minh"` |
| Múi giờ | Tên IANA trong `ZoneId.getAvailableZoneIds()`; offset dạng `+07:00` bị từ chối (DR-12) | `"timezone": "Asia/Tokyo"` |
| Hạn | Server quyết định bằng `now()` của database; client chỉ hiển thị (DR-12, DR-66) | — |

Response của event luôn có cả mốc UTC (`startsAt`) và `timezone`, để client hiển thị bằng `Intl.DateTimeFormat(locale, { timeZone })` (DR-10). Giờ trong **ví dụ của tài liệu API** là UTC.

Đồng hồ đếm ngược dùng header `X-Server-Time` (mục 6.3).

## 4. Tiền

- Số nguyên đồng VND, **không có phần lẻ**; trường tiền đặt tên `amount`, `unitPrice`, `price`, và luôn đi kèm `currency` ở cấp đối tượng gốc của một khoản thanh toán (`"currency": "VND"`). Chỉ có VND, gán cứng (DR-13); `orders.currency` giữ `CHECK` để đối chiếu.
- Không bao giờ dùng số thực. Tổng đơn do server tính; client chỉ hiển thị (`Intl.NumberFormat(locale, { style: "currency", currency: "VND" })`).
- Giá loại vé: `0` hoặc trong `[payment.min-amount, payment.max-amount]` (DR-13); vi phạm → 422 `VALIDATION_FAILED` `rule = out_of_range`.
- Đơn 0 đồng xác nhận bằng `POST /orders/{id}/confirm-free` (DR-45), không tạo PaymentIntent.

Ví dụ (rút từ SDD gốc 12.2):

```json
{ "reservationId": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091", "orderId": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162",
  "status": "ACTIVE", "expiresAt": "2026-11-10T03:10:00Z", "amount": 2500000, "currency": "VND" }
```

## 5. Lỗi: Problem Details

Mọi lỗi là RFC 9457 `application/problem+json`; bảng mã, exception và thành viên mở rộng ở [DOC-35](../06-design/error-handling.md) §3–§4.

| Trường | Kiểu | Luôn có | Ý nghĩa |
| --- | --- | --- | --- |
| `type` | URI | có | `https://errors.ticket.dev/<code>` (định danh, không cần phân giải được) |
| `title` | chuỗi | có | Tóm tắt theo `Accept-Language` (DR-10) |
| `status` | số | có | Mã HTTP |
| `detail` | chuỗi | có | Giải thích và hướng xử lý, theo `Accept-Language` |
| `code` | chuỗi | có | Mã chữ hoa tiếng Anh; client xử lý theo trường này |
| `requestId` | chuỗi | có | Trùng `X-Request-Id` và `trace_id` trong log |
| (mở rộng) | tùy mã | tùy | Ví dụ `unavailableSeatIds`, `errors`, `retryAfterSeconds` |

Ví dụ 422 kiểm tra dữ liệu (DR-25):

```http
HTTP/1.1 422 Unprocessable Entity
Content-Type: application/problem+json
Content-Language: vi

{
  "type": "https://errors.ticket.dev/VALIDATION_FAILED",
  "title": "Dữ liệu chưa hợp lệ",
  "status": 422,
  "detail": "Một số trường chưa đúng. Kiểm tra các ô được đánh dấu.",
  "code": "VALIDATION_FAILED",
  "requestId": "9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b",
  "errors": [
    { "field": "saleEndsAt", "rule": "after_event_start", "params": null },
    { "field": "items[1].quantity", "rule": "min", "params": { "min": 1 } }
  ]
}
```

Ví dụ 503 quá tải (DR-61):

```http
HTTP/1.1 503 Service Unavailable
Content-Type: application/problem+json
Retry-After: 2

{ "type": "https://errors.ticket.dev/OVERLOADED", "title": "Hệ thống đang quá tải",
  "status": 503, "detail": "Hệ thống đang đông. Chúng tôi sẽ tự thử lại sau ít giây.",
  "code": "OVERLOADED", "requestId": "c41b…", "retryAfterSeconds": 2 }
```

Quy tắc:

1. `field` là đường dẫn JSON trong body (`items[1].quantity`) hoặc tên tham số query (`limit`); `rule` là `snake_case` chữ thường, client tra key `validation.<rule>` (DOC-40).
2. Không bao giờ trả HTML hoặc văn bản trơn cho `/api/**`: nginx cũng phát Problem Details cho 413, 429 và 502/503/504 (DOC-62).
3. 404 dùng cho tài nguyên không tồn tại **và** tài nguyên của tổ chức khác (DR-23); 403 chỉ khi không đủ vai trò.
4. Tên và mã chính xác ở DOC-35; tài liệu này không lặp bảng.

## 6. Header

### 6.1 Bảng header

| Header | Hướng | Bắt buộc | Ý nghĩa |
| --- | --- | --- | --- |
| `Idempotency-Key` | request | Ở 4 endpoint của DR-45 | UUID (mọi phiên bản, chữ thường hoặc hoa); thiếu → 400 `IDEMPOTENCY_KEY_REQUIRED`; sai định dạng → 422 `VALIDATION_FAILED` `rule = uuid`. Gửi lại với cùng key phải gửi **đúng chuỗi body đã gửi lần đầu** (DR-45) |
| `Idempotent-Replayed` | response | Chỉ khi phát lại | `true` khi server trả lại response đã lưu; không có header ở lần chạy thật |
| `X-CSRF-Token` | request | `POST/PUT/PATCH/DELETE` có session | Giá trị `csrfToken` lấy từ `POST /auth/verify` hoặc `GET /me`; thiếu/sai → 403 `CSRF_TOKEN_INVALID`. Miễn: `/webhooks/stripe`, `/auth/magic-link`, `/auth/verify` (DR-22) |
| `X-Request-Id` | request và response | Không | nginx sinh `$request_id` nếu client không gửi; API đặt vào MDC `trace_id` và trả lại ở **mọi** response (kể cả lỗi). Client chỉ gửi giá trị `[A-Za-z0-9-]{8,64}`; khác thì nginx bỏ và sinh mới |
| `X-Server-Time` | response | Có ở mọi response | Epoch ms của server (DR-66) |
| `Retry-After` | response | 429 và 503 | Số nguyên giây; trùng `retryAfterSeconds` trong body (DR-84) |
| `X-Magic-Link-Throttled` | response | Chỉ `POST /auth/magic-link` bị giới hạn | `1`; status vẫn 202 (DR-21) |
| `Accept-Language` / `Content-Language` | request / response | Không | Chọn locale của `title`/`detail`; thứ tự chọn ở DOC-31; không có → `vi` |
| `Location` | response | 201 | Đường dẫn tuyệt đối của tài nguyên mới |
| `Cache-Control`, `ETag`, `Vary` | response | Theo mục 8 | — |
| `Stripe-Signature` | request | Chỉ webhook | Xác minh với dung sai 300 giây (DR-48) |
| `Allow` | response | 405 | Danh sách phương thức hợp lệ |

### 6.2 Phát lại idempotent

```http
POST /api/v1/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34/reservations HTTP/1.1
Idempotency-Key: 7b0e6c1a-3f52-4d9e-9a41-0c8d2e6f1b73
X-CSRF-Token: Qm9ZpL1…
Content-Type: application/json

{"items":[{"type":"GA","ticketTypeId":"0199b1c2-7b01-7f10-8a22-3c4d5e6f7081","quantity":2}]}
```

Lần gửi lại cùng key, cùng body byte-for-byte:

```http
HTTP/1.1 201 Created
Idempotent-Replayed: true
Location: /api/v1/reservations/0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091
```

Cùng key, body khác → 422 `IDEMPOTENCY_KEY_REUSED`. Chỉ response 2xx được lưu (DR-45), nên gửi lại sau 409 chạy thật.

### 6.3 Đồng hồ server

Interceptor của client (`frontend/src/api`) ghi `t0` trước khi gửi, `t1` khi nhận, tính `offset = serverTime − (t0 + t1)/2`, lấy trung vị 5 mẫu gần nhất; đồng hồ = `expiresAt − (Date.now() + offset)`, cập nhật 250 ms, hiển thị `mm:ss`, về 0 thì gọi lại API (DR-66). Header có ở **cả** response lỗi và 304.

## 7. Phân trang cursor

Mọi endpoint trả danh sách dùng cursor (SDD gốc 12.3); danh sách chắc chắn nhỏ và có giới hạn cứng (loại vé ≤ 5, phiên bản sơ đồ của một sơ đồ) trả mảng trực tiếp trong đối tượng cha, không phân trang.

| Thành phần | Quy tắc |
| --- | --- |
| Tham số | `?limit=` (mặc định 20, 1–100) và `?cursor=`; `limit` ngoài khoảng → 422 `VALIDATION_FAILED` `rule = out_of_range` |
| Response | `{ "items": [...], "nextCursor": "…" \| null }`; `nextCursor = null` khi hết |
| Cursor | base64url (không padding) của JSON `{"k": <khóa sắp xếp>, "id": "<uuid>"}`; client coi là chuỗi **mờ**, không tự dựng |
| Sắp xếp | Mỗi endpoint khai báo một khóa `k` và hướng; luôn kèm `id` làm khóa phụ để ổn định. Truy vấn là keyset (`WHERE (k, id) > (:k, :id) ORDER BY k, id LIMIT :limit + 1`), không `OFFSET` |
| Cursor hỏng | 400 `BAD_REQUEST` `rule = invalid_cursor` |
| Ổn định | Bản ghi chèn mới sau khi lấy trang đầu có thể không xuất hiện ở các trang sau; không có tổng số (`total`) |

Ví dụ (`GET /events?limit=2`, sắp xếp `startsAt` tăng dần):

```http
GET /api/v1/events?limit=2 HTTP/1.1

200 OK
{
  "items": [
    { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c33", "name": "Đêm nhạc mùa đông", "startsAt": "2026-11-12T12:00:00Z", "timezone": "Asia/Ho_Chi_Minh", "displayStatus": "ON_SALE" },
    { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34", "name": "Hội thảo Kiến trúc Hệ thống", "startsAt": "2026-11-14T13:00:00Z", "timezone": "Asia/Ho_Chi_Minh", "displayStatus": "UPCOMING" }
  ],
  "nextCursor": "eyJrIjoiMjAyNi0xMS0xNFQxMzowMDowMFoiLCJpZCI6IjAxOTliMWMyLTdhM2UtN2M0MC04ZjExLTJkNWU5YTZiMGMzNCJ9"
}
```

Trang kế: `GET /api/v1/events?limit=2&cursor=eyJrIjoi…`. Ví dụ trên chỉ minh họa cấu trúc; trường đầy đủ của từng item ở E-xx.

## 8. Cache

Mặc định mọi response API: `Cache-Control: no-store`. Chỉ các endpoint dưới đây mở cache; mọi endpoint khác (kể cả `GET /me`, `/organizer/**`, `/reservations/{id}`, `/orders/{id}`) **không bao giờ** cache.

| Endpoint | Cache-Control | Cache nginx (`proxy_cache`, DR-55) | Ghi chú |
| --- | --- | --- | --- |
| `GET /events/{id}/map?version=n` | `public, max-age=86400, immutable` | 1 ngày | Phiên bản bất biến (DR-16); `ETag` = `checksum` của phiên bản |
| `GET /events/{id}/map` (không `version`) | `no-cache`, trả 302 tới `?version=n` | Không | Event đổi phiên bản (xuất bản lại trước giờ mở bán) thì `n` đổi |
| `GET /events/{id}` | `public, max-age=5` | 5 giây | `Vary: Accept-Language` |
| `GET /events` | `public, max-age=5` (DR-86) | 5 giây | `Vary: Accept-Language`; khóa cache gồm cả query |
| `GET /events/{id}/availability` | `no-store` | Không | Cache ở tiến trình `api` 2 giây (DR-62), không ở nginx |
| `GET /media/{id}` | `public, max-age=86400` | 1 ngày | Tên file theo `media_id` bất biến |

Quy tắc:

1. Request có cookie `tb_session` tới endpoint công khai vẫn dùng chung bản cache nginx (nội dung không phụ thuộc người dùng); nginx không chuyển `Set-Cookie` vào cache.
2. Mọi endpoint ghi (`POST/PUT/PATCH/DELETE`) trả `Cache-Control: no-store`.
3. `ETag` + `If-None-Match` chỉ dùng cho `map?version=` (304 không thân); các endpoint khác không hỗ trợ điều kiện.

## 9. Webhook, giới hạn và biên

- `POST /webhooks/stripe`: đọc body thô, xác minh chữ ký, không `Idempotency-Key` (loại trùng bằng `stripe_event`, DR-48), không CSRF, không session; trả 200 cả khi bỏ qua event đã gặp.
- Kích thước: body ≤ 6 MB ở nginx (`client_max_body_size 6m`); tài liệu sơ đồ ≤ 5 MB và ảnh ≤ 5 MB ở ứng dụng (DR-32, DR-38) → 413 `PAYLOAD_TOO_LARGE`.
- Rate limit nginx: `api_ip` 20 r/s (burst 40), `auth_ip` 10 r/phút (burst 5) cho `/auth/magic-link`, `hold_ip` 5 r/s (burst 10) cho `POST /events/*/reservations` (DR-55); token bucket theo người dùng ở DR-56. Vượt → 429 `RATE_LIMITED` kèm `Retry-After`.
- Số dòng giữ vé: 1–20 `items`, tổng ≤ 50 unit (`reservation.max-units-per-hold`, DR-41).

## 10. OpenAPI và sinh client

### 10.1 Nguồn và truy cập

- springdoc-openapi sinh **OpenAPI 3.1** tại `/v3/api-docs` (DR-63, DOC-11). Chỉ truy cập được trong mạng nội bộ compose và profile `dev`; nginx không proxy `/v3/` và không bật Swagger UI ở compose.
- Mỗi module một **tag** (`auth`, `catalog`, `inventory`, `reservation`, `payment`, `seatmap`, `studio`, `media`, `queue`, `webhooks`), mỗi nhóm một `GroupedOpenApi`.
- Hợp đồng chỉ gồm class ở `controller` và `dto` của module; entity không bao giờ xuất hiện trong schema (DOC-12 §2.1).

### 10.2 Quy ước mô tả

| Mục | Quy tắc |
| --- | --- |
| `operationId` | camelCase, động từ + danh từ, **duy nhất**, trùng tên đã nêu ở mục `E-xx` của DOC-37: `createReservation`, `getEvent`, `requestMagicLink` |
| Schema | Tên class DTO (`ReservationResponse`); `record` ánh xạ sang schema; trường nullable khai `nullable` (kiểu `["string","null"]` ở 3.1); enum liệt kê đủ giá trị |
| Lỗi | Component dùng chung `Problem` và `ValidationProblem` (`errors[]`); `OpenApiCustomizer` gắn 401, 403, 429, 500, 503 `Problem` vào mọi operation; 409/422 khai ở từng operation với các `code` có thể xảy ra trong `description` |
| Bảo mật | Scheme `cookieAuth` (cookie `tb_session`) và `csrf` (header `X-CSRF-Token`); endpoint công khai khai `security: []` |
| Header | `Idempotency-Key` khai là tham số bắt buộc ở 4 endpoint của DR-45; response khai `X-Request-Id`, `X-Server-Time`, `Retry-After` |
| Ví dụ | Mỗi request/response chính có `example` lấy từ DOC-37 (test `APIG-07` kiểm tra ví dụ parse được theo schema) |
| Mô tả | Tiếng Anh ngắn (tài liệu API là hợp đồng kỹ thuật, `info.description` trỏ về DOC-37) |

### 10.3 Sinh client frontend

1. `make contract` (và CI) chạy test tích hợp `OpenApiExportTest` dựng ứng dụng, tải `/v3/api-docs` và ghi `backend/build/openapi.json` (DR-87); không cần chạy server riêng.
2. `pnpm gen:api` chạy `openapi-typescript` đọc `backend/build/openapi.json` và ghi `frontend/src/api/schema.d.ts`; file được commit.
3. Client là `openapi-fetch` gõ kiểu bằng `schema.d.ts`; **mọi** lời gọi đi qua `frontend/src/api/` (DOC-12 §6), interceptor ở đó: thêm `X-CSRF-Token`, sinh `Idempotency-Key`, đo `X-Server-Time`, tra `error.<code>`, backoff theo DOC-35 §6.
4. CI bước (4) của DR-08: chạy lại bước 1–2 rồi `git diff --exit-code frontend/src/api/schema.d.ts`; lệch thì chặn merge.
5. `oasdiff breaking backend/build/openapi.json <openapi của dev>` chạy ở CI (DR-77); thay đổi phá vỡ chỉ được vào khi PR có nhãn `api-breaking` và giải thích.

Thay đổi **phá vỡ** gồm: xóa/đổi tên đường dẫn, trường, enum value hoặc `operationId`; thêm trường request bắt buộc; đổi kiểu; thu hẹp giá trị; đổi mã trạng thái thành công. **Không** phá vỡ: thêm endpoint, thêm trường response, thêm giá trị enum ở **response** (client phải chịu được giá trị lạ, DOC-35 §7: mã lạ → `error.UNKNOWN`), thêm mã lỗi.

### 10.4 Cấu hình

| Key | Kiểu | Mặc định | Ghi chú |
| --- | --- | --- | --- |
| `api.pagination.default-limit` | int | 20 | Mặc định `limit` (DR-63); khóa mới, nguồn chuẩn DOC-34 |
| `api.pagination.max-limit` | int | 100 | Trần `limit` (DR-63); khóa mới |
| `springdoc.api-docs.enabled` | boolean | `true` ở `dev`, `true` trong compose nhưng nginx không proxy | Khóa chuẩn của springdoc |
| `springdoc.swagger-ui.enabled` | boolean | `true` chỉ ở `dev` | Khóa chuẩn của springdoc |

## 11. Quyết định mới khi viết tài liệu này

Do Claude chốt theo ủy quyền của Owner (master plan §0.1); bên gọi gán số DR. Quyết định của [DOC-35](../06-design/error-handling.md) §9 (`DR-82`…`4`) áp dụng cho tài liệu này; thêm:

- **DR-86 · Cache công khai của `GET /events`.** *Vấn đề:* DR-55 chỉ nêu cache `GET /events/{id}` (5 giây), không nêu danh sách; trang chủ là endpoint được đọc nhiều nhất lúc mở bán. *Quyết định:* `GET /events` cache `public, max-age=5` cùng `proxy_cache` 5 giây, khóa gồm query string và `Accept-Language`. *Hệ quả:* sự kiện mới xuất bản hiện chậm tối đa 5 giây; thêm `proxy_cache_path` ở DOC-62. *Ghi vào:* DOC-36, DOC-62, DOC-37.
- **DR-87 · Xuất OpenAPI cho CI bằng test tích hợp.** *Vấn đề:* sinh `schema.d.ts` và `oasdiff` cần file OpenAPI mà không muốn thêm plugin Gradle chưa chắc tương thích Boot 4 (S-01). *Quyết định:* `OpenApiExportTest` (Testcontainers) ghi `backend/build/openapi.json`; `make contract` gọi nó. *Hệ quả:* thêm một test chậm ~vài giây; đổi sang plugin sau không đổi hợp đồng. *Ghi vào:* DOC-36, DOC-63, DOC-61.
- **DR-88 · Body request chặt.** *Vấn đề:* mặc định Jackson bỏ qua trường lạ, che lỗi gõ sai (`quantiy`). *Quyết định:* `spring.jackson.deserialization.fail-on-unknown-properties=true` → 400 `BAD_REQUEST`; client sinh từ OpenAPI không gửi trường lạ. *Hệ quả:* thêm trường request ở server là thay đổi phải đi cùng client mới (không phá vỡ nếu trường tùy chọn và client cũ không gửi). *Ghi vào:* DOC-36, DOC-12.
- **DR-89 · Trường giờ của studio có hậu tố `Local`.** *Vấn đề:* DR-12 nói form studio gửi giờ địa phương kèm `timezone` nhưng chưa nêu tên trường. *Quyết định:* request ghi dùng `startsAtLocal`, `endsAtLocal`, `saleStartsAtLocal`, `saleEndsAtLocal`, `prequeueOpensLocal` (`yyyy-MM-dd'T'HH:mm`) kèm `timezone`; response trả `startsAt`… UTC và `timezone`, cộng thêm các trường `…Local` để form điền lại không phải tính. *Hệ quả:* DOC-37 mục E của event dùng đúng tên này; DOC-20 chỉnh nếu khác. *Ghi vào:* DOC-36, DOC-37, DOC-20, DOC-55.

## 12. Test bắt buộc

Tiền tố `APIG-` (đã kiểm tra chưa dùng; đăng ký ở DOC-69). Test lỗi và retry ở DOC-35 (`ERR-`).

| ID | Kịch bản | Kết quả mong đợi |
| --- | --- | --- |
| APIG-01 | `GET /api/v1/events/{id}` | Header có `X-Request-Id` (≥ 8 ký tự), `X-Server-Time` (epoch ms trong ±2 giây so với đồng hồ test), không có trường nào bị bỏ (mọi trường null vẫn có mặt) |
| APIG-02 | Gửi `X-Request-Id: abc` (3 ký tự) qua nginx | Response mang giá trị mới do nginx sinh, khác `abc`; gửi `7f3c9a1e-5b2d-4e8a` thì giữ nguyên |
| APIG-03 | Seed 45 event, `GET /events?limit=20`, theo `nextCursor` đến hết | 3 trang: 20, 20, 5; không trùng, không thiếu; trang cuối `nextCursor = null`; thứ tự `(startsAt, eventId)` tăng |
| APIG-04 | `limit=0`, `limit=101`, `cursor=%%%` | 422 `out_of_range` (hai lần đầu), 400 `invalid_cursor` |
| APIG-05 | `POST /events/{id}/reservations` thiếu `Idempotency-Key`; với `Idempotency-Key: not-a-uuid` | 400 `IDEMPOTENCY_KEY_REQUIRED`; 422 `VALIDATION_FAILED` `rule = uuid` |
| APIG-06 | Gửi hai lần cùng key + cùng byte body; lần ba cùng key + body đổi thứ tự trường | Lần hai: 201 + `Idempotent-Replayed: true`, body y hệt lần một; lần ba: 422 `IDEMPOTENCY_KEY_REUSED` |
| APIG-07 | Mọi `example` trong OpenAPI | Parse được và hợp lệ theo schema của chính nó |
| APIG-08 | Body có trường lạ `{"quantiy":2}` | 400 `BAD_REQUEST` |
| APIG-09 | Request ghi có cookie `tb_session` hợp lệ, thiếu `X-CSRF-Token` | 403 `CSRF_TOKEN_INVALID` |
| APIG-10 | Event có `timezone = "Asia/Tokyo"`, form gửi `startsAtLocal = "2026-11-14T20:00"` | Response `startsAt = "2026-11-14T11:00:00Z"`, `startsAtLocal` trả lại `2026-11-14T20:00` |
| APIG-11 | `GET /events/{id}/map?version=2` hai lần | Lần hai gửi `If-None-Match` = `checksum` → 304; `Cache-Control: public, max-age=86400, immutable` |
| APIG-12 | `GET /me`, `GET /reservations/{id}` | `Cache-Control: no-store` |
| APIG-13 | Dựng lại OpenAPI rồi sinh `schema.d.ts` | Không có diff với file đã commit; `operationId` duy nhất; mọi operation có 500 và 503 `Problem` |
| APIG-14 | `oasdiff breaking` trên PR xóa một trường response | Báo thay đổi phá vỡ, CI đỏ nếu thiếu nhãn `api-breaking` |
| APIG-15 | Số tiền `1800000` ở `vi`, `en` | Giao diện hiện `1.800.000 ₫` và `₫1,800,000`; JSON vẫn là số nguyên `1800000` |

## 13. Câu hỏi còn mở

Không có. Quyết định ở mục 11 do Claude chốt theo ủy quyền, chờ gán số DR.
