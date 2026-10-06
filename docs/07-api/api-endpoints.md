# Danh mục endpoint

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-37
> Phụ thuộc: SDD gốc §12.1–12.2, [DOC-06](../02-glossary.md), [DOC-14](../05-data/domain-model.md), [DOC-15](../05-data/ops-model.md), [DOC-31](../06-design/i18n.md), [DOC-35](../06-design/error-handling.md) §3–§4, [DOC-36](api-guidelines.md), [DOC-82](../06-design/flows/README.md), [Sổ quyết định](../00-decision-register.md) (DR-10, 12, 13, 21, 22, 23, 24, 25, 26, 28, 29, 30, 31, 32, 35, 36, 37, 38, 41, 43, 44, 45, 46, 47, 48, 57, 58, 59, 62, 63, 64, 65, 66, 70, 71)
> Người dùng chính: P1-04 (nhóm xác thực), P2-xx, P3-xx, P4-xx, P5-xx, P6-xx (mỗi phase cài nhóm endpoint của mình); tác giả DOC-42…60 (cột "Endpoint" của màn hình); DOC-32 (ma trận quyền); `OpenApiExportTest` (so `operationId`)

Tài liệu liệt kê **48 endpoint** `E-01…48`: toàn bộ SDD gốc 12.1 và DR-65. Mỗi mục có tham số, JSON request và response đầy đủ, lỗi, nguồn dữ liệu, cache và chỉ tiêu hiệu năng. Quy ước chung (tiền tố, JSON, thời gian, tiền, header, phân trang, cache) ở [DOC-36](api-guidelines.md); bảng mã lỗi ở [DOC-35](../06-design/error-handling.md); ma trận quyền chi tiết ở DOC-32; luồng từng bước và sơ đồ tuần tự ở `FL-xx` (DOC-83…90). Mọi mục `E-xx` ở trạng thái **Draft** tới khi task cài đặt tương ứng xong và test `ENDP-` xanh; hợp đồng chạy thật là OpenAPI sinh từ code (DOC-36 §10) và phải khớp `operationId` ở đây.

Mọi đường dẫn bỏ tiền tố `/api/v1` (DR-63), trừ `GET /media/{mediaId}`. Giờ trong ví dụ là UTC; ví dụ chọn cố định một bộ ID để đọc xuyên suốt (mục 3.4).

## 1. Cách đọc một mục

| Trường | Ý nghĩa |
| --- | --- |
| **Mục đích / UC** | Việc endpoint làm, `UC-xx` (DOC-04), `FL-xx` (DOC-82) |
| **Quyền** | `Công khai`, `Đăng nhập`, `Tổ chức` (có hồ sơ tổ chức, sở hữu tài nguyên → 404 nếu không, DR-23), `Webhook`; ma trận chi tiết ở DOC-32 |
| **Tham số** | Bảng `Tên · Ở đâu · Kiểu · Bắt buộc · Mặc định · Ràng buộc`; "Ở đâu" là `path`, `query`, `header`, `body` |
| **Response** | Mã thành công, schema và ví dụ JSON đầy đủ (không bỏ trường, DOC-36 §2) |
| **Lỗi** | Chỉ các mã đặc thù của endpoint. Lỗi chung của mọi endpoint (§3.1) không lặp lại |
| **Nguồn dữ liệu** | Bảng, câu chính, index (tên index theo DOC-14, DOC-15) |
| **Cache** | Theo DOC-36 §8; mặc định `no-store` |
| **Hiệu năng** | Chỉ tiêu **kế hoạch** (planned), p95 tính tại `api`, không gồm mạng; đo thật ở EXP và NFR-02 (DR-75). Chưa có số đo |

CSRF: mọi `POST/PUT/PATCH/DELETE` có session cần `X-CSRF-Token` (DR-22); mục chỉ ghi ngoại lệ. Idempotency: chỉ 4 endpoint của DR-45 (E-14, E-16, E-17, E-18) cần `Idempotency-Key`.

## 2. Bảng tổng hợp

| E | Phương thức và đường dẫn | `operationId` | Quyền | UC · FL | Phase | Tag |
| --- | --- | --- | --- | --- | --- | --- |
| E-01 | `POST /auth/magic-link` | `requestMagicLink` | Công khai | UC-01 · FL-01 | P1 | `auth` |
| E-02 | `POST /auth/verify` | `verifyMagicLink` | Công khai | UC-01 · FL-02 | P1 | `auth` |
| E-03 | `POST /auth/logout` | `logout` | Đăng nhập | UC-01 · FL-03 | P1 | `auth` |
| E-04 | `GET /me` | `getMe` | Đăng nhập | UC-01, UC-20 · FL-02, FL-04 | P1 | `auth` |
| E-05 | `PATCH /me` | `updateMe` | Đăng nhập | UC-20 · FL-04 | P1 | `auth` |
| E-06 | `GET /events` | `listEvents` | Công khai | UC-02 · FL-12 | P2 | `catalog` |
| E-07 | `GET /events/{eventId}` | `getEvent` | Công khai | UC-02 · FL-12 | P2 | `catalog` |
| E-08 | `GET /events/{eventId}/map` | `getEventMap` | Công khai | UC-02 · FL-29 | P5 | `seatmap` |
| E-09 | `GET /events/{eventId}/availability` | `getAvailability` | Công khai | UC-02 · FL-29 | P5 | `inventory` |
| E-10 | `POST /events/{eventId}/queue` | `joinQueue` | Đăng nhập | UC-12 · FL-33 | P6 | `queue` |
| E-11 | `GET /events/{eventId}/queue` | `getQueueStatus` | Đăng nhập | UC-12 · FL-33 | P6 | `queue` |
| E-12 | `DELETE /events/{eventId}/queue` | `leaveQueue` | Đăng nhập | UC-12 · FL-35 | P6 | `queue` |
| E-13 | `GET /media/{mediaId}` | `getMedia` | Công khai | UC-02 · FL-12 | P2 | `media` |
| E-14 | `POST /events/{eventId}/reservations` | `createReservation` | Đăng nhập (+ lượt vào, DR-58) | UC-03 · FL-13, FL-30, FL-34 | P2 | `reservation` |
| E-15 | `GET /reservations/{reservationId}` | `getReservation` | Đăng nhập (chủ) | UC-04 · FL-20 | P2 | `reservation` |
| E-16 | `DELETE /reservations/{reservationId}` | `cancelReservation` | Đăng nhập (chủ) | UC-06 · FL-15 | P2 | `reservation` |
| E-17 | `POST /orders/{orderId}/payment-intent` | `createPaymentIntent` | Đăng nhập (chủ) | UC-04 · FL-20 | P3 | `payment` |
| E-18 | `POST /orders/{orderId}/confirm-free` | `confirmFreeOrder` | Đăng nhập (chủ) | UC-04 · FL-14 | P2 | `payment` |
| E-19 | `GET /orders/{orderId}` | `getOrder` | Đăng nhập (chủ) | UC-05 · FL-17, FL-20 | P2 | `payment` |
| E-20 | `GET /me/orders` | `listMyOrders` | Đăng nhập | UC-05 · FL-17 | P2 | `payment` |
| E-21 | `GET /me/tickets` | `listMyTickets` | Đăng nhập | UC-05 · FL-17 | P2 | `payment` |
| E-22 | `POST /webhooks/stripe` | `receiveStripeWebhook` | Webhook | UC-04, UC-15 · FL-21, FL-23 | P3 | `webhooks` |
| E-23 | `POST /organizer` | `createOrganizer` | Đăng nhập | UC-07 · FL-05 | P2 | `studio` |
| E-24 | `GET /organizer/events` | `listOrganizerEvents` | Tổ chức | UC-07, UC-10 · FL-32 | P2 | `studio` |
| E-25 | `POST /organizer/events` | `createEvent` | Tổ chức | UC-07 · FL-06 | P2 | `studio` |
| E-26 | `GET /organizer/events/{eventId}` | `getOrganizerEvent` | Tổ chức | UC-07 · FL-06, FL-07 | P2 | `studio` |
| E-27 | `PATCH /organizer/events/{eventId}` | `updateEvent` | Tổ chức | UC-07, UC-17 · FL-06, FL-11 | P2 | `studio` |
| E-28 | `GET /organizer/events/{eventId}/publish-checks` | `getPublishChecks` | Tổ chức | UC-09 · FL-08 | P2 | `studio` |
| E-29 | `POST /organizer/events/{eventId}/publish` | `publishEvent` | Tổ chức | UC-09 · FL-08 | P2 | `studio` |
| E-30 | `POST /organizer/events/{eventId}/pause` | `pauseEvent` | Tổ chức | UC-09 · FL-09 | P2 | `studio` |
| E-31 | `POST /organizer/events/{eventId}/resume` | `resumeEvent` | Tổ chức | UC-09 · FL-09 | P2 | `studio` |
| E-32 | `POST /organizer/events/{eventId}/close-sale` | `closeEventSale` | Tổ chức | UC-09 · FL-09 | P2 | `studio` |
| E-33 | `POST /organizer/events/{eventId}/cancel` | `cancelEvent` | Tổ chức | UC-13 · FL-10 | P3 | `studio` |
| E-34 | `POST /organizer/events/{eventId}/ticket-types` | `createTicketType` | Tổ chức | UC-07 · FL-07 | P2 | `studio` |
| E-35 | `PATCH /organizer/events/{eventId}/ticket-types/{ticketTypeId}` | `updateTicketType` | Tổ chức | UC-07 · FL-07 | P2 | `studio` |
| E-36 | `DELETE /organizer/events/{eventId}/ticket-types/{ticketTypeId}` | `deleteTicketType` | Tổ chức | UC-07 · FL-07 | P2 | `studio` |
| E-37 | `GET /organizer/events/{eventId}/sales` | `getEventSales` | Tổ chức | UC-10 · FL-32 | P5 | `studio` |
| E-38 | `GET /organizer/events/{eventId}/seat-status` | `getEventSeatStatus` | Tổ chức | UC-10 · FL-32 | P5 | `studio` |
| E-39 | `POST /organizer/media` | `uploadMedia` | Tổ chức | UC-07, UC-08 · FL-06, FL-26 | P2 | `media` |
| E-40 | `GET /organizer/events/{eventId}/map` | `getOrganizerEventMap` | Tổ chức | UC-08 · FL-26 | P4 | `seatmap` |
| E-41 | `GET /organizer/maps` | `listMaps` | Tổ chức | UC-22 · FL-28 | P4 | `seatmap` |
| E-42 | `POST /organizer/maps` | `createMap` | Tổ chức | UC-08 · FL-26 | P4 | `seatmap` |
| E-43 | `GET /organizer/maps/{seatMapId}` | `getMap` | Tổ chức | UC-08 · FL-26 | P4 | `seatmap` |
| E-44 | `PUT /organizer/maps/{seatMapId}/draft` | `saveMapDraft` | Tổ chức | UC-08 · FL-26 | P4 | `seatmap` |
| E-45 | `POST /organizer/maps/{seatMapId}/validate` | `validateMap` | Tổ chức | UC-08 · FL-27 | P4 | `seatmap` |
| E-46 | `POST /organizer/maps/{seatMapId}/publish` | `publishMap` | Tổ chức | UC-08, UC-14 · FL-27, FL-31 | P4 | `seatmap` |
| E-47 | `GET /organizer/maps/{seatMapId}/versions` | `listMapVersions` | Tổ chức | UC-08 · FL-27 | P4 | `seatmap` |
| E-48 | `POST /organizer/maps/{seatMapId}/clone` | `cloneMap` | Tổ chức | UC-22 · FL-28 | P4 | `seatmap` |

Cột "Phase" là phase cài đặt đầu tiên (master plan §5); khung P1 chỉ có nhóm xác thực (E-01…05). Ghi chú về tên: SDD gốc viết `PUT /maps/{id}/draft`; DR-63 chốt `/organizer/maps/…`. SDD gốc viết `/ticket-types` ở `POST, PATCH, DELETE`; ở đây `PATCH` và `DELETE` thêm `{ticketTypeId}` vào đường dẫn.

## 3. Thành phần dùng chung

### 3.1 Lỗi chung

Mọi endpoint (trừ ghi chú) có thể trả: 401 `UNAUTHENTICATED` (endpoint không phải `Công khai`), 403 `CSRF_TOKEN_INVALID` (ghi có session), 403 `ORGANIZER_PROFILE_REQUIRED` (`/organizer/**` khi chưa có hồ sơ), 404 `NOT_FOUND` (id không tồn tại hoặc không thuộc người gọi, DR-23), 400 `BAD_REQUEST` (JSON hỏng, trường lạ), 413, 415, 422 `VALIDATION_FAILED` (sai dữ liệu), 429 `RATE_LIMITED`, 500 `INTERNAL_ERROR`, 503 `OVERLOADED`. Bảng đầy đủ và thành viên mở rộng ở DOC-35 §3–§4.

### 3.2 Schema dùng chung

| Schema | Trường |
| --- | --- |
| `Money` (phẳng, không đối tượng riêng) | `amount` hoặc `price` hoặc `unitPrice`: số nguyên VND; `currency`: `"VND"` ở đối tượng gốc của khoản thanh toán (DOC-36 §4) |
| `Label` | Một trong `{ "section": "Khán đài A", "row": "C", "seat": "9" }` (SEAT) hoặc `{ "zone": "Fanzone" }` (ZONE) hoặc `{ "ticketType": "Vé tiêu chuẩn" }` (GA); đúng như `reservation_item.label` (DOC-14) |
| `EventTimes` | `startsAt`, `endsAt`, `saleStartsAt`, `saleEndsAt` (UTC, `…Z`) và `timezone` (IANA). Response của studio thêm `startsAtLocal`, `endsAtLocal`, `saleStartsAtLocal`, `saleEndsAtLocal` (`yyyy-MM-dd'T'HH:mm` theo `timezone`) (DR-12, DR-89) |
| `PublicTicketType` | `ticketTypeId`, `name`, `model` (`SEAT`/`ZONE`/`GA`), `price`, `currency`, `colorIndex` (1–5) |
| `OrganizerTicketType` | `PublicTicketType` + `gaCapacity` (`null` khi không phải `GA`), `sortOrder`, `used` (số unit `HELD`+`SOLD`, DR-30) |
| `TicketView` | `ticketId`, `code` (`GM-4K7P-92XD`), `status` (`ISSUED`/`VOID`), `ticketTypeName`, `unitPrice`, `label`: `Label`, `issuedAt` |
| `EventBrief` | `eventId`, `name`, `venue`, `startsAt`, `endsAt`, `timezone`, `imageUrl` (`/media/{id}` hoặc `null`) |
| `QueueState` | Xem E-10 |
| `EventStateResponse` | `eventId`, `status` (lưu: `DRAFT`/`PUBLISHED`/`PAUSED`/`ENDED`/`CANCELLED`), `displayStatus` (DR-24), `rowVersion` — trả bởi E-30…32 |

`displayStatus` ∈ `DRAFT`, `UPCOMING`, `ON_SALE`, `SOLD_OUT`, `SALE_CLOSED`, `PAUSED`, `ENDED`, `CANCELLED` (DR-24). `imageUrl` là đường dẫn tương đối `/media/{mediaId}`; client không dựng URL ảnh từ `mediaId`.

### 3.3 Ai thấy sự kiện nào

`GET /events` trả sự kiện `PUBLISHED`/`PAUSED` có `ends_at > now()`; `GET /events/{eventId}` và các endpoint công khai theo `eventId` trả cả `ENDED`, `CANCELLED`; `DRAFT` luôn 404 với người không sở hữu (DR-24). Sự kiện của tổ chức khác chỉ thấy qua endpoint công khai.

### 3.4 Bộ ID dùng trong ví dụ

| Đối tượng | ID |
| --- | --- |
| Người dùng | `0199a000-1111-7222-8333-444455556666` (`an@example.com`) |
| Tổ chức | `0199a000-2222-7333-8444-555566667777` ("Giao Mùa Studio") |
| Sự kiện GA "Hội thảo Kiến trúc Hệ thống" | `0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34` |
| Loại vé GA "Vé tiêu chuẩn" | `0199b1c2-7b01-7f10-8a22-3c4d5e6f7081` |
| Sự kiện có sơ đồ "Hòa nhạc Giao Mùa" | `0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35` |
| Loại vé ghế "Hạng A" / vé khu "Fanzone" | `0199b1c2-7b02-7f10-8a22-3c4d5e6f7082` / `0199b1c2-7b03-7f10-8a22-3c4d5e6f7083` |
| Sơ đồ | `0199b1c2-3333-7bbb-8ccc-ddddeeeeffff` |
| Ghế | `0199b1c2-9a01-7a10-9c2e-6d1f0a4b8c11`, `0199b1c2-9a02-7a10-9c2e-6d1f0a4b8c12` |
| Reservation / Đơn | `0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091` / `0199b1c2-8d15-7a22-9b0c-1d2e3f405162` |
| Ảnh | `0199b1c2-1111-7aaa-8bbb-ccccddddeeee` |
| Vé | `0199b1c2-9f01-7000-8000-000000000001` |

## 4. Xác thực và tài khoản (E-01…05)

### E-01 `POST /auth/magic-link` · `requestMagicLink`

- **Mục đích / UC:** Xin link đăng nhập gửi tới email (UC-01, FL-01). Thay mọi token chưa dùng của email này (DR-21).
- **Quyền:** Công khai. Miễn CSRF (chưa có session, DR-22).
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `email` | body | string | có | — | `trim`, chữ thường; regex `^[^@\s]+@[^@\s]+\.[^@\s]+$`; ≤ 254 (DR-21). Sai → 422 `rule = invalid_email` |
  | `returnTo` | body | string | không | `/` | Bắt đầu bằng `/`, không bắt đầu `//` hoặc `/\`, ≤ 512; sai thì dùng `/` (không lỗi) |
  | `locale` | body | string | không | locale của request (DOC-31 §2) | `vi` hoặc `en`; khác → 422 `rule = invalid_locale` |

- **Request:**

  ```json
  { "email": "An@Example.com ", "returnTo": "/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34", "locale": "vi" }
  ```

- **Response 202** (gửi được):

  ```json
  { "status": "SENT", "email": "an@example.com", "expiresInSeconds": 900 }
  ```

  Vượt giới hạn (3 link/email/15 phút hoặc 10 link/IP/giờ): vẫn **202**, thêm header `X-Magic-Link-Throttled: 1`, body `{ "status": "THROTTLED", "email": "an@example.com", "expiresInSeconds": 0 }`; không chèn token, không gửi email (DR-21). `email` trong body là bản đã chuẩn hóa để giao diện hiển thị đúng địa chỉ.
- **Lỗi:** 422 `VALIDATION_FAILED` (`invalid_email`, `invalid_locale`); 503 `EMAIL_PROVIDER_UNAVAILABLE` (SMTP lỗi sau commit; token đã chèn và tính vào giới hạn; không có dòng outbox; DR-21).
- **Nguồn dữ liệu:** `login_token`. Một transaction: đếm `SELECT count(*) FROM login_token WHERE email = :email AND created_at > now() - interval '15 minutes'` (`login_token_email_idx`) và `… WHERE requested_ip = :ip AND created_at > now() - interval '1 hour'` (`login_token_ip_idx`); `UPDATE login_token SET superseded_at = now() WHERE email = :email AND used_at IS NULL AND superseded_at IS NULL`; `INSERT` token mới (`token_hash = SHA-256`, `expires_at = now() + 15 phút`). Sau commit gửi SMTP trực tiếp (timeout 5 giây, không thử lại). IP lấy từ `X-Real-IP` do nginx đặt.
- **Cache:** không.
- **Hiệu năng:** p95 < 150 ms không gồm SMTP; SMTP timeout 5 giây. Edge: `auth_ip` 10 r/phút (DR-55).
- **Real-time:** không có.

### E-02 `POST /auth/verify` · `verifyMagicLink`

- **Mục đích / UC:** Đổi token trong link lấy session (UC-01, FL-02). Trang `/auth/callback` của SPA gọi endpoint này; trình duyệt mở link là trình duyệt nhận session.
- **Quyền:** Công khai. Miễn CSRF (DR-22).
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `token` | body | string | có | — | 43 ký tự base64url; sai định dạng → 401 `LOGIN_LINK_INVALID` (không lộ lý do) |

- **Request:** `{ "token": "q3J2k0wYb7nQ1u8Z5cXfHh3aT9mEoVd4LpS6iNgR2yA" }`
- **Response 200** + `Set-Cookie: tb_session=<32 byte base64url>; Path=/; HttpOnly; SameSite=Lax; Secure` (`Secure` tắt ở profile `dev`, DR-22):

  ```json
  {
    "userId": "0199a000-1111-7222-8333-444455556666",
    "email": "an@example.com",
    "locale": "vi",
    "roles": ["BUYER"],
    "organizer": null,
    "csrfToken": "Qm9ZpL1xVw3Rk8TnYc2HfJ6uSd0aEiGb4PzOq7MtXlA",
    "serverTime": 1795050601234,
    "returnTo": "/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34"
  }
  ```

  Người dùng chưa có tài khoản được tạo ngay lúc này (`app_user.locale` = `locale` của token).
- **Lỗi:** 401 `LOGIN_LINK_INVALID` (hết hạn, đã dùng, bị thay, không tồn tại; cùng một thông báo).
- **Nguồn dữ liệu:** `UPDATE login_token SET used_at = now() WHERE token_hash = :h AND used_at IS NULL AND superseded_at IS NULL AND expires_at > now() RETURNING email, return_to, locale` (một câu ghi có điều kiện, DR-21); `INSERT … ON CONFLICT (email)` vào `app_user` (`app_user_email_uq`); `INSERT` vào `session` (`token_hash`, `csrf_token`). Song song 50 request cùng token → đúng 1 thành công (test `AU-` của DOC-19).
- **Cache:** không (`no-store`).
- **Hiệu năng:** p95 < 100 ms.

### E-03 `POST /auth/logout` · `logout`

- **Mục đích / UC:** Đăng xuất (UC-01, FL-03).
- **Quyền:** Đăng nhập; cần `X-CSRF-Token`.
- **Tham số:** không.
- **Response 204**, `Set-Cookie: tb_session=; Max-Age=0; Path=/`. Gọi khi session đã hết hạn → 401 `UNAUTHENTICATED` (client coi như đã đăng xuất).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `UPDATE session SET revoked_at = now() WHERE token_hash = :h`; xóa mục Caffeine của session (DR-22).
- **Cache:** không. **Hiệu năng:** p95 < 50 ms.

### E-04 `GET /me` · `getMe`

- **Mục đích / UC:** Tài khoản, vai trò, token CSRF hiện tại (UC-01, UC-20; DR-23, DR-22). SPA gọi khi tải trang để biết đã đăng nhập chưa và lấy lại `csrfToken` sau khi tải lại (DOC-35 §6, DR-85).
- **Quyền:** Đăng nhập. Chưa đăng nhập → 401 `UNAUTHENTICATED` (SPA hiểu là khách, không hiện lỗi).
- **Response 200:**

  ```json
  {
    "userId": "0199a000-1111-7222-8333-444455556666",
    "email": "an@example.com",
    "locale": "vi",
    "roles": ["BUYER", "ORGANIZER"],
    "organizer": { "organizerId": "0199a000-2222-7333-8444-555566667777", "name": "Giao Mùa Studio" },
    "csrfToken": "Qm9ZpL1xVw3Rk8TnYc2HfJ6uSd0aEiGb4PzOq7MtXlA",
    "serverTime": 1795050601234
  }
  ```

  `roles` luôn có `BUYER`; có `ORGANIZER` khi `organizer` khác `null`. `serverTime` trùng `X-Server-Time` (tiện cho lần đo offset đầu, DR-66).
- **Lỗi:** chỉ 401.
- **Nguồn dữ liệu:** Caffeine (khóa `token_hash`, TTL 60 giây) → `session` JOIN `app_user` LEFT JOIN `organizer` (`session_user_idx`, `organizer.owner_user_id UNIQUE`); `last_seen_at` chỉ ghi khi cũ hơn 1 giờ (DR-22).
- **Cache:** không. **Hiệu năng:** p95 < 10 ms khi trúng Caffeine; < 30 ms khi trượt.

### E-05 `PATCH /me` · `updateMe`

- **Mục đích / UC:** Đổi ngôn ngữ giao diện (UC-20, FL-04). Email và Problem Details sau đó theo locale mới; email đã nằm trong outbox giữ locale cũ (DOC-31 §2).
- **Quyền:** Đăng nhập; CSRF.
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `locale` | body | string | có | — | `vi` hoặc `en`; khác → 422 `rule = invalid_locale` |

- **Request:** `{ "locale": "en" }`
- **Response 200:** đối tượng của E-04 với `locale` mới.
- **Lỗi:** 422 `VALIDATION_FAILED` (`invalid_locale`).
- **Nguồn dữ liệu:** `UPDATE app_user SET locale = :l WHERE user_id = :u`; xóa mục Caffeine liên quan (DR-22).
- **Cache:** không. **Hiệu năng:** p95 < 50 ms.

## 5. Sự kiện công khai, phòng chờ và ảnh (E-06…13)

### E-06 `GET /events` · `listEvents`

- **Mục đích / UC:** Danh sách sự kiện đang hiển thị công khai (UC-02, FL-12).
- **Quyền:** Công khai.
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `limit` | query | int | không | 20 | 1–100 (DOC-36 §7) |
  | `cursor` | query | string | không | — | Cursor mờ; sai → 400 `BAD_REQUEST` `rule = invalid_cursor` |

- **Response 200:** sắp xếp `(startsAt, eventId)` tăng dần; chỉ `PUBLISHED`/`PAUSED` có `endsAt > now()` (DR-24).

  ```json
  {
    "items": [
      {
        "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
        "name": "Hòa nhạc Giao Mùa",
        "venue": "Nhà hát Hòa Bình, TP.HCM",
        "startsAt": "2026-11-12T12:00:00Z",
        "endsAt": "2026-11-12T15:00:00Z",
        "timezone": "Asia/Ho_Chi_Minh",
        "saleStartsAt": "2026-11-05T03:00:00Z",
        "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
        "displayStatus": "ON_SALE",
        "priceFrom": 450000,
        "currency": "VND"
      },
      {
        "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34",
        "name": "Hội thảo Kiến trúc Hệ thống",
        "venue": "Dreamplex, Quận 1",
        "startsAt": "2026-11-14T13:00:00Z",
        "endsAt": "2026-11-14T17:00:00Z",
        "timezone": "Asia/Ho_Chi_Minh",
        "saleStartsAt": "2026-11-10T03:00:00Z",
        "imageUrl": null,
        "displayStatus": "UPCOMING",
        "priceFrom": 0,
        "currency": "VND"
      }
    ],
    "nextCursor": null
  }
  ```

  `priceFrom` là giá thấp nhất trong các loại vé chưa xóa, `null` khi chưa có loại vé (không xảy ra với sự kiện đã xuất bản).
- **Lỗi:** 422 `VALIDATION_FAILED` (`limit` ngoài khoảng, `rule = out_of_range`); 400 `BAD_REQUEST` (`invalid_cursor`).
- **Nguồn dữ liệu:** `event` (`event_public_idx` theo `status`, `starts_at`), JOIN `ticket_type` lấy `min(price)`; `displayStatus = SOLD_OUT` lấy từ snapshot tình trạng chỗ trong tiến trình (E-09, DR-62) nên không đếm `inventory_unit` cho từng dòng. `keyset: WHERE (starts_at, event_id) > (:k, :id) ORDER BY starts_at, event_id LIMIT :limit + 1`.
- **Cache:** `public, max-age=5`, `Vary: Accept-Language`; `proxy_cache` 5 giây, khóa gồm query (DR-86).
- **Hiệu năng:** p95 < 30 ms (trúng cache nginx: không chạm `api`).

### E-07 `GET /events/{eventId}` · `getEvent`

- **Mục đích / UC:** Chi tiết sự kiện và loại vé (UC-02, FL-12).
- **Quyền:** Công khai. `DRAFT` hoặc không tồn tại → 404.
- **Tham số:** `eventId` (path, uuid).
- **Response 200:**

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "name": "Hòa nhạc Giao Mùa",
    "description": "Đêm nhạc giao mùa với dàn nhạc giao hưởng.\nCửa mở lúc 18:30.",
    "venue": "Nhà hát Hòa Bình, TP.HCM",
    "timezone": "Asia/Ho_Chi_Minh",
    "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
    "startsAt": "2026-11-12T12:00:00Z",
    "endsAt": "2026-11-12T15:00:00Z",
    "saleStartsAt": "2026-11-05T03:00:00Z",
    "saleEndsAt": "2026-11-12T10:00:00Z",
    "displayStatus": "ON_SALE",
    "highDemand": true,
    "holdMinutes": 10,
    "hasSeatMap": true,
    "mapVersion": 2,
    "organizer": { "name": "Giao Mùa Studio", "contactEmail": "lienhe@giaomua.example" },
    "ticketTypes": [
      { "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082", "name": "Hạng A", "model": "SEAT", "price": 1200000, "currency": "VND", "colorIndex": 1, "soldOut": false },
      { "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "name": "Fanzone", "model": "ZONE", "price": 450000, "currency": "VND", "colorIndex": 2, "soldOut": false }
    ]
  }
  ```

  `holdMinutes` = `reservation.hold-duration` (phút, DR-42) để màn hình ghi "10 phút"; `mapVersion` là `version_no` của phiên bản đang dùng (`null` khi chỉ GA); `organizer.contactEmail` là `organizer.contact_email` hoặc email đăng nhập của chủ (DR-23); `soldOut` theo từng loại vé từ snapshot tình trạng chỗ. Sự kiện `CANCELLED`/`ENDED` vẫn trả 200 với `displayStatus` tương ứng (DR-24).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `event` (PK), `ticket_type` (`ticket_type_idx`, `deleted_at IS NULL`), `seat_map_version` (`version_no` qua `event.seat_map_version_id`), `organizer`; `displayStatus` tính ở server theo DR-24.
- **Cache:** `public, max-age=5`, `Vary: Accept-Language`; `proxy_cache` 5 giây.
- **Hiệu năng:** p95 < 20 ms.

### E-08 `GET /events/{eventId}/map` · `getEventMap`

- **Mục đích / UC:** Tài liệu sơ đồ của phiên bản đang dùng, cho màn Chọn chỗ (UC-02, FL-29; DR-65).
- **Quyền:** Công khai. Sự kiện không có sơ đồ (chỉ GA) hoặc `DRAFT` → 404.
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `eventId` | path | uuid | có | — | — |
  | `version` | query | int | không | — | `version_no` ≥ 1; không thuộc sơ đồ của sự kiện → 404 |

- **Response:**
  - Không có `version` → **302** `Location: /api/v1/events/{eventId}/map?version=2` (phiên bản `event.seat_map_version_id`), `Cache-Control: no-cache`.
  - Có `version` → **200**:

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "versionNo": 2,
    "checksum": "9b74c9897bac770ffc029102a200c5de3f8b1f1c2e0d4a5b6c7d8e9f0a1b2c3d",
    "seatCount": 180,
    "document": { "schemaVersion": 1, "canvas": { "width": 4000, "height": 3000, "seatDiameter": 20, "minSpacing": 24, "grid": 10, "background": null }, "sections": [], "zones": [], "decorations": [] }
  }
  ```

  `document` theo JSON Schema v1 (DR-32; cấu trúc đầy đủ ở DOC-16); ví dụ rút gọn. `checksum` là SHA-256 dạng hex của JSON chuẩn hóa RFC 8785 (DR-32), cũng là `ETag`.
- **Lỗi:** chỉ lỗi chung. `If-None-Match` trùng `ETag` → 304 không thân.
- **Nguồn dữ liệu:** `seat_map_version` theo `(seat_map_id, version_no)` (unique), qua `seat_map.event_id`.
- **Cache:** `version` có mặt: `public, max-age=86400, immutable`, `ETag`; `proxy_cache` 1 ngày. Không `version`: không cache (DOC-36 §8).
- **Hiệu năng:** p95 < 40 ms (20.000 ghế, ~3 MB JSON, nén gzip ở nginx); trúng cache nginx < 5 ms.

### E-09 `GET /events/{eventId}/availability` · `getAvailability`

- **Mục đích / UC:** Tình trạng chỗ để tô sơ đồ và hiện số vé còn lại; client hỏi mỗi 4 giây ± 0,5 giây, dừng khi tab ẩn (UC-02, FL-29; DR-62).
- **Quyền:** Công khai.
- **Tham số:** `eventId` (path).
- **Response 200:**

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "mapVersion": 2,
    "generatedAt": "2026-11-10T03:00:01.200Z",
    "seats": { "count": 180, "held": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "sold": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" },
    "pools": [
      { "poolId": "0199b1c2-5001-7aaa-8bbb-111122223333", "zoneKey": "zone-ab12cd34", "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "available": 86 }
    ],
    "ticketTypes": [
      { "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082", "available": 42, "held": 3 },
      { "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "available": 86, "held": 4 }
    ]
  }
  ```

  `held` và `sold` là bitset base64: bit `i` (LSB trước trong mỗi byte) ứng với `seat_index = i`; 180 ghế = 23 byte. Sự kiện GA: `seats` là `{ "count": 0, "held": "", "sold": "" }`, `mapVersion` là `null`. `mapVersion` khác bản client đang có → client tải lại E-08.
- **Lỗi:** chỉ lỗi chung (404 khi sự kiện `DRAFT`).
- **Nguồn dữ liệu:** hai truy vấn: ghế `HELD`/`SOLD` theo `unit_seat_taken_idx`; `count(*)` `AVAILABLE` theo pool và loại vé (`unit_pool_available_idx`, `unit_ticket_type_idx`). Cache Caffeine theo `eventId`, hết hạn 2 giây sau khi ghi, `cache.get(eventId, loader)` gộp request đồng thời (DR-62).
- **Cache:** `Cache-Control: no-store` ra ngoài; cache trong tiến trình 2 giây.
- **Hiệu năng:** p95 < 10 ms khi trúng cache; < 120 ms khi dựng (20.000 ghế, kế hoạch; đo ở EXP-10, S-03).

### E-10 `POST /events/{eventId}/queue` · `joinQueue`

- **Mục đích / UC:** Xin lượt vào; kiểm soát tiếp nhận luôn chạy trong khung mở bán, hàng đợi "trong suốt" khi còn chỗ (UC-12, FL-33; DR-57).
- **Quyền:** Đăng nhập; CSRF. Gọi lại khi đã ở trong hàng hoặc đã có lượt: trả trạng thái hiện tại (không xếp lại, giữ vị trí).
- **Tham số:** `eventId` (path). Body rỗng `{}`.
- **Response 200:** `QueueState`:

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "status": "WAITING",
    "position": 1284,
    "queueSize": 8051,
    "estimatedWaitSeconds": 26,
    "retryAfterSeconds": 10,
    "admittedUntil": null,
    "opensAt": null
  }
  ```

  | `status` | Ý nghĩa | Trường kèm theo |
  | --- | --- | --- |
  | `PRE_QUEUE` | Vào phòng chờ trước giờ mở bán (`high_demand`, từ `prequeue_opens`) | `opensAt` = `saleStartsAt`; `position = null`; `retryAfterSeconds = 30` |
  | `WAITING` | Đang xếp hàng | `position`, `queueSize`, `estimatedWaitSeconds` (`null` khi chưa có dữ liệu), `retryAfterSeconds` (3 nếu vị trí ≤ 200; 10 nếu ≤ 2.000; còn lại 30) |
  | `ADMITTED` | Có lượt vào | `admittedUntil` (ISO UTC, mili giây); `position = null`; `retryAfterSeconds = null` |
  | `PAUSED` | "Tạm hết vé": vé còn lại đang được giữ | như `WAITING` |
  | `SOLD_OUT` | Hết vé | `position = null` |
  | `NOT_IN_QUEUE` | Không còn trong hàng (chỉ ở E-11) | — |

  Mất Redis ở sự kiện không `high_demand`: trả `ADMITTED` với `admittedUntil = null` (không cần lượt vào để giữ vé, DR-57).
- **Lỗi:** 409 `EVENT_NOT_ON_SALE` (ngoài khung: trước `prequeue_opens` hoặc trước `saleStartsAt` khi không `high_demand`, `PAUSED`, `SALE_CLOSED`, `CANCELLED`; kèm `displayStatus`); 503 `OVERLOADED` (mất Redis ở sự kiện `high_demand`, DR-56).
- **Nguồn dữ liệu:** Redis, không đọc `inventory_unit` trực tiếp: `ZADD queue:{e} NX`, `ZSCORE admitted:{e}`, `admit-hist:{e}`, `queue-state:{e}` (script Lua, DR-59; tên key chuẩn ở DOC-17). Trạng thái sự kiện đọc từ `event` (cache 5 giây).
- **Cache:** không.
- **Hiệu năng:** p95 < 15 ms (một lần gọi Lua).
- **Real-time:** không có; client hỏi lại E-11 theo `retryAfterSeconds` (DR-59).

### E-11 `GET /events/{eventId}/queue` · `getQueueStatus`

- **Mục đích / UC:** Xem vị trí và nhận lượt vào (UC-12, FL-33).
- **Quyền:** Đăng nhập.
- **Tham số:** `eventId` (path).
- **Response 200:** `QueueState` như E-10; `NOT_IN_QUEUE` khi người dùng chưa vào hoặc đã bị loại do không hỏi quá `admission.idle-timeout` (2 phút): `{ "eventId": "…", "status": "NOT_IN_QUEUE", "position": null, "queueSize": null, "estimatedWaitSeconds": null, "retryAfterSeconds": null, "admittedUntil": null, "opensAt": null }`.
  Mỗi lần gọi làm mới `seen:{e}` của người dùng (chống rớt hàng khi còn kết nối).
- **Lỗi:** 409 `EVENT_NOT_ON_SALE` như E-10 (client dừng hỏi và quay về trang sự kiện); 503 `OVERLOADED`.
- **Nguồn dữ liệu:** Redis như E-10 (`ZRANK`, `ZSCORE`, `ZADD seen`).
- **Cache:** không. **Hiệu năng:** p95 < 10 ms.

### E-12 `DELETE /events/{eventId}/queue` · `leaveQueue`

- **Mục đích / UC:** Rời hàng, nhường chỗ cho người kế tiếp (UC-12, FL-35; DR-57, DR-65).
- **Quyền:** Đăng nhập; CSRF.
- **Response 204** (kể cả khi người dùng không ở trong hàng).
- **Lỗi:** chỉ lỗi chung (503 `OVERLOADED` khi mất Redis ở sự kiện `high_demand`).
- **Nguồn dữ liệu:** Redis: `ZREM` khỏi `prequeue`, `queue`, `admitted`, `seen`. Chỗ được cấp cho người kế tiếp ở vòng `AdmissionTicker` sau.
- **Cache:** không. **Hiệu năng:** p95 < 10 ms.

### E-13 `GET /media/{mediaId}` · `getMedia`

- **Mục đích / UC:** Phục vụ ảnh sự kiện và ảnh nền mặt bằng (UC-02, FL-12; DR-38, DR-65). **Không có tiền tố `/api/v1`** (nginx route riêng).
- **Quyền:** Công khai (ảnh là công khai; không có presigned URL).
- **Tham số:** `mediaId` (path, uuid).
- **Response 200:** byte ảnh; `Content-Type` = `media.content_type` (`image/jpeg`, `image/png`, `image/webp`); `Cache-Control: public, max-age=31536000, immutable`; `ETag: "<sha256 hex>"`. `If-None-Match` trùng → 304.
- **Lỗi:** 404 `NOT_FOUND` (Problem Details, không HTML). Không có `X-CSRF-Token`, không cần session.
- **Nguồn dữ liệu:** `media` (PK) rồi `GetObject` từ `s3` theo `object_key`, stream không đệm toàn bộ vào bộ nhớ.
- **Cache:** nginx `proxy_cache` 1 ngày (khóa theo URL); storage chỉ bị gọi khi cache trượt. ⚠ DOC-36 §8 ghi `max-age=86400`; DR-38 chốt `31536000, immutable` vì `mediaId` bất biến; theo DR-38 (DOC-36 cần chỉnh).
- **Hiệu năng:** p95 < 20 ms khi trúng nginx; < 80 ms khi trượt (ảnh ≤ 2 MB).

## 6. Giữ vé, đơn, vé và webhook (E-14…22)

### E-14 `POST /events/{eventId}/reservations` · `createReservation`

- **Mục đích / UC:** Giữ ghế, vé khu vực hoặc vé GA trong một lệnh; tạo `reservation`, `reservation_item` và `orders` (UC-03; FL-13 GA, FL-30 ghế và khu vực, FL-34 qua phòng chờ; DR-41).
- **Quyền:** Đăng nhập; CSRF. Khi sự kiện đang trong khung kiểm soát tiếp nhận (DR-57): phải có lượt vào hợp lệ (`ZSCORE admitted:{e} <userId> > now_ms`, DR-58), không thì 429 `QUEUE_REQUIRED`.
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `eventId` | path | uuid | có | — | — |
  | `Idempotency-Key` | header | uuid | có | — | Thiếu → 400 `IDEMPOTENCY_KEY_REQUIRED`; sai định dạng → 422 `rule = uuid` |
  | `items` | body | array | có | — | 1–20 dòng; tổng (số ghế + tổng `quantity`) ≤ `reservation.max-units-per-hold` = 50 (BR-01) |
  | `items[].type` | body | enum | có | — | `SEAT`, `ZONE`, `GA` |
  | `items[].seatIds` | body | uuid[] | khi `SEAT` | — | ≥ 1, không trùng; ghế thuộc loại vé `SEAT` của sự kiện |
  | `items[].zoneId` | body | string | khi `ZONE` | — | `zoneKey` (vd `zone-ab12cd34`, DR-32); phải là pool `ZONE` của sự kiện |
  | `items[].ticketTypeId` | body | uuid | khi `GA` | — | Loại vé `GA` của sự kiện |
  | `items[].quantity` | body | int | khi `ZONE`, `GA` | — | ≥ 1; cùng `zoneId` hoặc `ticketTypeId` không xuất hiện hai lần |

- **Request** (SDD gốc 12.2):

  ```json
  POST /api/v1/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35/reservations
  Idempotency-Key: 7b0e6c1a-3f52-4d9e-9a41-0c8d2e6f1b73

  {
    "items": [
      { "type": "SEAT", "seatIds": ["0199b1c2-9a01-7a10-9c2e-6d1f0a4b8c11", "0199b1c2-9a02-7a10-9c2e-6d1f0a4b8c12"] },
      { "type": "ZONE", "zoneId": "zone-ab12cd34", "quantity": 3 }
    ]
  }
  ```

- **Response 201** (`Location: /api/v1/reservations/0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091`):

  ```json
  {
    "reservationId": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091",
    "orderId": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162",
    "status": "ACTIVE",
    "expiresAt": "2026-11-10T03:10:00.000Z",
    "amount": 3750000,
    "currency": "VND",
    "items": [
      { "type": "SEAT", "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082", "ticketTypeName": "Hạng A", "quantity": 2, "unitPrice": 1200000,
        "seats": [
          { "seatId": "0199b1c2-9a01-7a10-9c2e-6d1f0a4b8c11", "label": { "section": "Khán đài A", "row": "C", "seat": "9" } },
          { "seatId": "0199b1c2-9a02-7a10-9c2e-6d1f0a4b8c12", "label": { "section": "Khán đài A", "row": "C", "seat": "10" } } ] },
      { "type": "ZONE", "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "ticketTypeName": "Fanzone", "quantity": 3, "unitPrice": 450000,
        "zoneId": "zone-ab12cd34", "seats": [] }
    ]
  }
  ```

  `amount` = tổng `quantity × unitPrice`, tính ở server (2 × 1.200.000 + 3 × 450.000 = 3.750.000). Tổng 0 đồng vẫn tạo order `PENDING_PAYMENT` (client gọi E-18). Phát lại cùng key và cùng body: 201 y hệt kèm `Idempotent-Replayed: true`.
- **Lỗi:**

  | HTTP | `code` | Khi nào · thành viên mở rộng |
  | --- | --- | --- |
  | 409 | `SEATS_UNAVAILABLE` | `unavailableSeatIds: ["0199b1c2-9a02-…"]`; không có tác dụng phụ (rollback) |
  | 409 | `INSUFFICIENT_CAPACITY` | `poolId`, `requested`, `available` |
  | 409 | `ACTIVE_RESERVATION_EXISTS` | `reservationId` của reservation đang mở (`reservation_open_uq`) |
  | 409 | `EVENT_NOT_ON_SALE` | `displayStatus`, `saleStartsAt` khi `UPCOMING` |
  | 429 | `QUEUE_REQUIRED` | `queueUrl`; chưa có hoặc hết hạn lượt vào |
  | 422 | `VALIDATION_FAILED` | `errors[].rule`: `too_many_units` (kèm `params.max = 50`), `too_many_items`, `duplicate_item`, `min`, `required`, `wrong_model` (ghế không thuộc loại vé `SEAT`, `zoneId` không phải pool `ZONE`, `ticketTypeId` không phải `GA`), `unknown_reference` (id không thuộc sự kiện) |
  | 400 | `IDEMPOTENCY_KEY_REQUIRED` | Thiếu header |
  | 422 | `IDEMPOTENCY_KEY_REUSED` | Cùng key, body khác |

- **Nguồn dữ liệu:** transaction READ COMMITTED, `statement_timeout 2s`, `lock_timeout 1s` (DR-41): (1) `INSERT idempotency_key … ON CONFLICT DO NOTHING`; (2) đọc `event` (`PUBLISHED`, trong khung bán); (3) `INSERT reservation … ON CONFLICT … DO NOTHING` (`reservation_open_uq`) **trước** unit; (4) claim ghế `UPDATE inventory_unit SET status='HELD' … WHERE event_id=:e AND seat_id = ANY(:ids) AND status='AVAILABLE'` (`unit_seat_uq`) và claim pool `… WHERE unit_id IN (SELECT unit_id FROM inventory_unit WHERE pool_id=:p AND status='AVAILABLE' LIMIT :n FOR UPDATE SKIP LOCKED)` (`unit_pool_available_idx`); (5) `INSERT reservation_item`, `INSERT orders`; (6) ghi response vào `idempotency_key`. Trước transaction: `soldout:pool:{poolId}` và bitmap tình trạng chặn sớm (DR-46, DR-62); token bucket theo người dùng (DR-56).
- **Cache:** không. **Hiệu năng:** p95 < 120 ms thời gian xử lý với 50 unit (kế hoạch; NFR-02 yêu cầu p95 < 500 ms đo từ k6); đo ở EXP-01, 02, 03.
- **Real-time:** không có; tình trạng chỗ lấy bằng E-09 (DR-62).

### E-15 `GET /reservations/{reservationId}` · `getReservation`

- **Mục đích / UC:** Trạng thái và thời hạn còn lại; dựng lại màn Thanh toán sau khi tải lại trang (UC-04, FL-20; SDD gốc 13.2).
- **Quyền:** Đăng nhập, chủ reservation (`user_id`), khác → 404.
- **Tham số:** `reservationId` (path).
- **Response 200:**

  ```json
  {
    "reservationId": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091",
    "orderId": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162",
    "status": "ACTIVE",
    "closeReason": null,
    "expiresAt": "2026-11-10T03:10:00.000Z",
    "amount": 3750000,
    "currency": "VND",
    "orderStatus": "PENDING_PAYMENT",
    "event": {
      "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Hòa nhạc Giao Mùa", "venue": "Nhà hát Hòa Bình, TP.HCM",
      "startsAt": "2026-11-12T12:00:00Z", "endsAt": "2026-11-12T15:00:00Z", "timezone": "Asia/Ho_Chi_Minh",
      "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee"
    },
    "items": [ { "type": "GA", "ticketTypeId": "0199b1c2-7b01-7f10-8a22-3c4d5e6f7081", "ticketTypeName": "Vé tiêu chuẩn", "quantity": 2, "unitPrice": 0, "seats": [] } ]
  }
  ```

  `status` ∈ `ACTIVE`, `EXPIRING`, `CONFIRMED`, `EXPIRED`, `CANCELLED`; `closeReason` ∈ `TIMEOUT`, `BUYER_CANCELLED`, `EVENT_CANCELLED` hoặc `null`. Đồng hồ đếm ngược dùng `expiresAt` và `X-Server-Time` (DR-66); về 0 thì gọi lại endpoint này thay vì tự kết luận.
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `reservation` (PK), `orders` (`reservation_id UNIQUE`), `reservation_item` (`reservation_item_res_idx`), `event`.
- **Cache:** không. **Hiệu năng:** p95 < 15 ms.

### E-16 `DELETE /reservations/{reservationId}` · `cancelReservation`

- **Mục đích / UC:** Hủy giữ vé và trả vé ngay trong request (UC-06, FL-15; DR-43).
- **Quyền:** Đăng nhập, chủ; CSRF.
- **Tham số:** `reservationId` (path); `Idempotency-Key` (header, bắt buộc, uuid).
- **Response:**
  - **200** `{ "reservationId": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091", "status": "CANCELLED" }`: đã trả vé (hoặc đã đóng từ trước: trả trạng thái hiện tại).
  - **202** `{ "reservationId": "…", "status": "EXPIRING" }`: Stripe lỗi hoặc timeout (3 giây) khi hủy PaymentIntent; `ReservationExpiryJob` hoàn tất sau.
- **Lỗi:** 409 `PAYMENT_ALREADY_SUCCEEDED` (kèm `orderId`; giao diện chuyển sang màn Kết quả); 400 `IDEMPOTENCY_KEY_REQUIRED`.
- **Nguồn dữ liệu:** (1) `UPDATE reservation SET status='EXPIRING', close_reason='BUYER_CANCELLED', expiring_since=now() WHERE reservation_id=:id AND user_id=:uid AND status='ACTIVE'`; (2) sau commit đọc `orders.payment_intent_id`, hủy PaymentIntent (nếu có); (3) transaction trả vé: reservation `EXPIRING → CANCELLED` (`closed_at`), `UPDATE inventory_unit SET status='AVAILABLE', reservation_id=NULL WHERE reservation_id=:id AND status='HELD'` (`unit_reservation_idx`), order `PENDING_PAYMENT → CANCELLED`; xóa `soldout:pool:{poolId}`.
- **Cache:** không. **Hiệu năng:** p95 < 150 ms khi không có PaymentIntent; < 3,2 giây khi gọi hủy Stripe (timeout 3 giây).

### E-17 `POST /orders/{orderId}/payment-intent` · `createPaymentIntent`

- **Mục đích / UC:** Tạo (hoặc lấy lại) PaymentIntent và trả `clientSecret` cho Payment Element (UC-04, FL-20; DR-47, ADR-0005).
- **Quyền:** Đăng nhập, chủ đơn; CSRF.
- **Tham số:** `orderId` (path); `Idempotency-Key` (header, bắt buộc): nhận để client thống nhất nhưng **không** ghi vào bảng; idempotent tự nhiên theo `order_id` (DR-45, DR-47). Body rỗng `{}`.
- **Response 200:**

  ```json
  { "clientSecret": "pi_3PqRsT2eZvKYlo2C1a2b3c4d_secret_9xYzWvUtSrQp", "amount": 3750000, "currency": "VND", "expiresAt": "2026-11-10T03:10:00.000Z" }
  ```

  Ở profile `fake-payments` `clientSecret` có dạng `fake_pi_<id>_secret_<random>` (DR-51). Gọi lần hai khi order đã có `payment_intent_id`: lấy PaymentIntent từ Stripe và trả cùng `clientSecret`.
- **Lỗi:** 409 `RESERVATION_NOT_ACTIVE` (kèm `status`); 409 `PAYMENT_WINDOW_TOO_SHORT` (còn < 30 giây, kèm `remainingSeconds`); 409 `PAYMENT_ALREADY_SUCCEEDED`; 422 `VALIDATION_FAILED` `rule = amount_zero` (đơn 0 đồng dùng E-18); 503 `PAYMENT_PROVIDER_UNAVAILABLE` (kèm `retryAfterSeconds`).
- **Nguồn dữ liệu:** `orders`, `reservation`; gọi Stripe ngoài transaction (khóa idempotency Stripe `pi-create:<orderId>`); transaction ngắn: `SELECT 1 FROM reservation WHERE reservation_id=:r AND status='ACTIVE' FOR SHARE`, rồi `UPDATE orders SET payment_intent_id=:pi WHERE order_id=:o AND payment_intent_id IS NULL`; không có dòng → hủy PaymentIntent vừa tạo, 409 `RESERVATION_NOT_ACTIVE` (DR-47).
- **Cache:** không. **Hiệu năng:** p95 < 1,2 giây (một lần gọi Stripe, timeout 2 + 10 giây, `maxNetworkRetries = 2`); cổng giả < 30 ms.

### E-18 `POST /orders/{orderId}/confirm-free` · `confirmFreeOrder`

- **Mục đích / UC:** Xác nhận đơn 0 đồng, phát hành vé không qua thanh toán (UC-04, UC-05, FL-14; DR-45).
- **Quyền:** Đăng nhập, chủ đơn; CSRF.
- **Tham số:** `orderId` (path); `Idempotency-Key` (header, bắt buộc). Body rỗng `{}`.
- **Response 200:** `OrderView` như E-19 với `status = "PAID"` và `tickets` đã phát hành. Phát lại cùng key: cùng response kèm `Idempotent-Replayed: true`.
- **Lỗi:** 409 `RESERVATION_NOT_ACTIVE` (hết hạn trước khi xác nhận); 422 `VALIDATION_FAILED` `rule = amount_not_zero` (đơn có tiền phải thanh toán qua E-17); 409 `EVENT_NOT_ON_SALE` không áp dụng (reservation còn hạn vẫn xác nhận được, DR-24).
- **Nguồn dữ liệu:** một transaction xác nhận như thanh toán (DOC-26): `SELECT status FROM event … FOR SHARE` (DR-28); `UPDATE reservation SET status='CONFIRMED' WHERE reservation_id=:r AND status='ACTIVE'`; `UPDATE inventory_unit SET status='SOLD' …`; `UPDATE orders SET status='PAID', paid_at=now() WHERE … status='PENDING_PAYMENT'`; `INSERT ticket` (một vé một unit, mã DR-52, `ticket_unit_issued_uq`); `INSERT outbox` `EMAIL_TICKETS`.
- **Cache:** không. **Hiệu năng:** p95 < 150 ms với 50 vé.

### E-19 `GET /orders/{orderId}` · `getOrder`

- **Mục đích / UC:** Trạng thái đơn; màn Kết quả hỏi tới khi `PAID` (UC-04, UC-05, FL-17, FL-20; DR-50, DR-65).
- **Quyền:** Đăng nhập, chủ đơn.
- **Tham số:** `orderId` (path).
- **Response 200 (`OrderView`):**

  ```json
  {
    "orderId": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162",
    "reservationId": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091",
    "status": "PAID",
    "amount": 3750000,
    "currency": "VND",
    "paidAt": "2026-11-10T03:04:12Z",
    "refundReason": null,
    "lastPaymentError": null,
    "createdAt": "2026-11-10T03:00:01Z",
    "event": { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Hòa nhạc Giao Mùa", "venue": "Nhà hát Hòa Bình, TP.HCM",
               "startsAt": "2026-11-12T12:00:00Z", "endsAt": "2026-11-12T15:00:00Z", "timezone": "Asia/Ho_Chi_Minh", "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee" },
    "tickets": [
      { "ticketId": "0199b1c2-9f01-7000-8000-000000000001", "code": "GM-4K7P-92XD", "status": "ISSUED", "ticketTypeName": "Hạng A",
        "unitPrice": 1200000, "label": { "section": "Khán đài A", "row": "C", "seat": "9" }, "issuedAt": "2026-11-10T03:04:12Z" }
    ]
  }
  ```

  `status` ∈ `PENDING_PAYMENT`, `PAID`, `EXPIRED`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED` (DR-44). `tickets` có khi `PAID` (SDD gốc 9.2 bước 6); rỗng `[]` ở trạng thái khác; sau khi sự kiện bị hủy vé chuyển `VOID` và đơn `REFUND_PENDING` với `refundReason = "EVENT_CANCELLED"` (`tickets` giữ các vé `VOID` để màn hiển thị). `refundReason` ∈ `LATE_PAYMENT`, `AMOUNT_MISMATCH`, `EVENT_CANCELLED` hoặc `null`. `lastPaymentError` là `decline_code` gần nhất (vd `insufficient_funds`) để màn Kết quả hiện "thẻ bị từ chối".
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `orders` (PK), `event`, `ticket` (`ticket_order_idx`).
- **Cache:** không. **Hiệu năng:** p95 < 15 ms. Client hỏi mỗi 2 giây trong 60 giây đầu, sau đó mỗi 10 giây tới 10 phút (DR-50): ~30 request/đơn đang chờ, nằm trong ngân sách `api_ip` (DR-55).

### E-20 `GET /me/orders` · `listMyOrders`

- **Mục đích / UC:** Đơn hàng của tôi (UC-05, FL-17).
- **Quyền:** Đăng nhập.
- **Tham số:** `limit` (query, mặc định 20, 1–100), `cursor` (query).
- **Response 200:** sắp xếp `(createdAt, orderId)` giảm dần.

  ```json
  {
    "items": [
      { "orderId": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162", "status": "PAID", "amount": 3750000, "currency": "VND", "ticketCount": 5,
        "createdAt": "2026-11-10T03:00:01Z", "event": { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Hòa nhạc Giao Mùa", "startsAt": "2026-11-12T12:00:00Z", "timezone": "Asia/Ho_Chi_Minh", "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee" } }
    ],
    "nextCursor": null
  }
  ```

  Bao gồm đơn ở mọi trạng thái (kể cả `EXPIRED`, `CANCELLED`) để người mua thấy lịch sử; màn "Vé của tôi" lọc theo `status` ở client.
- **Lỗi:** 400 `BAD_REQUEST` (`invalid_cursor`), 422 (`limit`).
- **Nguồn dữ liệu:** `orders` (`orders_user_idx` trên `(user_id, created_at DESC)`), JOIN `event`; `ticketCount` bằng `count(*)` `ticket_order_idx`.
- **Cache:** không. **Hiệu năng:** p95 < 30 ms.

### E-21 `GET /me/tickets` · `listMyTickets`

- **Mục đích / UC:** Vé của tôi, kèm mã vé (UC-05, FL-17).
- **Quyền:** Đăng nhập.
- **Tham số:** `limit`, `cursor` như E-20.
- **Response 200:** sắp xếp `(issuedAt, ticketId)` giảm dần; mỗi phần tử là `TicketView` cộng `orderId` và `event`:

  ```json
  {
    "items": [
      { "ticketId": "0199b1c2-9f01-7000-8000-000000000001", "orderId": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162", "code": "GM-4K7P-92XD",
        "status": "ISSUED", "ticketTypeName": "Hạng A", "unitPrice": 1200000,
        "label": { "section": "Khán đài A", "row": "C", "seat": "9" }, "issuedAt": "2026-11-10T03:04:12Z",
        "event": { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Hòa nhạc Giao Mùa", "venue": "Nhà hát Hòa Bình, TP.HCM",
                   "startsAt": "2026-11-12T12:00:00Z", "endsAt": "2026-11-12T15:00:00Z", "timezone": "Asia/Ho_Chi_Minh", "imageUrl": null },
        "displayStatus": "ON_SALE" }
    ],
    "nextCursor": null
  }
  ```

  Vé `VOID` (sự kiện bị hủy) vẫn có mặt với `status = "VOID"` và `displayStatus = "CANCELLED"` để màn hiện "Chờ hoàn tiền".
- **Lỗi:** như E-20.
- **Nguồn dữ liệu:** `ticket` JOIN `orders` (`user_id = :uid`) JOIN `event` (`ticket_order_idx`, `orders_user_idx`). Vé không có cột `user_id`, quyền sở hữu suy ra từ đơn.
- **Cache:** không. **Hiệu năng:** p95 < 40 ms.

### E-22 `POST /webhooks/stripe` · `receiveStripeWebhook`

- **Mục đích / UC:** Nhận event từ Stripe và đổi trạng thái đơn (UC-04, UC-15; FL-21, FL-23; DR-48).
- **Quyền:** Webhook: không session, không CSRF, không `Idempotency-Key`; xác thực bằng chữ ký.
- **Tham số:**

  | Tên | Ở đâu | Kiểu | Bắt buộc | Mặc định | Ràng buộc |
  | --- | --- | --- | --- | --- | --- |
  | `Stripe-Signature` | header | string | có | — | `Webhook.constructEvent(payload, header, secret, 300)`; sai hoặc quá 300 giây → 400 `INVALID_SIGNATURE` |
  | body | body | raw bytes | có | — | Đọc thô, không qua Jackson |

- **Request:** event Stripe nguyên bản (rút gọn):

  ```json
  { "id": "evt_1PqRsT2eZvKYlo2CAbCdEfGh", "type": "payment_intent.succeeded", "created": 1795050252,
    "data": { "object": { "id": "pi_3PqRsT2eZvKYlo2C1a2b3c4d", "amount": 3750000, "amount_received": 3750000, "currency": "vnd",
      "metadata": { "order_id": "0199b1c2-8d15-7a22-9b0c-1d2e3f405162", "reservation_id": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091", "event_id": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35" } } } }
  ```

- **Response 200:** `{ "received": true }`, cả khi event đã gặp (loại trùng bằng `stripe_event`) hoặc không tìm thấy đơn (`outcome = IGNORED`). Loại event đăng ký: `payment_intent.succeeded`, `payment_intent.payment_failed`, `payment_intent.canceled`, `charge.refunded`, `charge.dispute.created` (hai loại cuối chỉ ghi log WARN).
- **Lỗi:** 400 `INVALID_SIGNATURE`; **500** `INTERNAL_ERROR` với mọi lỗi xử lý khác (rollback; Stripe gửi lại; ngoại lệ của quy tắc "lỗi nghiệp vụ không bao giờ 500", DOC-35 §1).
- **Nguồn dữ liệu:** một transaction: `INSERT stripe_event … ON CONFLICT DO NOTHING`; tìm `orders` theo `payment_intent_id` (unique) rồi `metadata.order_id`; `SELECT status FROM event … FOR SHARE`; `succeeded`: kiểm tra `amount_received` và `currency` khớp đơn (khác → `REFUND_PENDING` `AMOUNT_MISMATCH`), rồi xác nhận như E-18 hoặc luồng trễ (`EXPIRED`/`CANCELLED → REFUND_PENDING` `LATE_PAYMENT`, outbox `EMAIL_REFUND_PENDING`); `payment_failed`: `UPDATE orders SET last_payment_error=:decline_code WHERE status='PENDING_PAYMENT'`.
- **Cache:** không. Giới hạn nginx: body ≤ 6 MB; không áp `hold_ip`.
- **Hiệu năng:** p95 < 150 ms (có phát hành tới 50 vé); Stripe coi > 10 giây là lỗi. Cổng giả gọi cùng endpoint với chữ ký tạo bằng secret giả (DR-51).


## 7. Studio: tổ chức, sự kiện, loại vé, số liệu và ảnh (E-23…39)

Mọi endpoint nhóm này (trừ E-23) nằm dưới `/organizer/**`: lấy `organizer_id` từ session, thêm `AND organizer_id = :sessionOrg` vào mọi truy vấn; tài nguyên của tổ chức khác trả 404 `NOT_FOUND`; chưa có hồ sơ trả 403 `ORGANIZER_PROFILE_REQUIRED` (DR-23). Mọi thời gian ghi dùng cặp `…Local` + `timezone` (DR-89); response trả UTC và `…Local` (§3.2 `EventTimes`).

`OrganizerEventView` (dùng chung bởi E-25, E-26, E-27):

```json
{
  "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
  "status": "DRAFT",
  "displayStatus": "DRAFT",
  "name": "Hòa nhạc Giao Mùa",
  "description": "Đêm nhạc giao mùa với dàn nhạc giao hưởng.\nCửa mở lúc 18:30.",
  "venue": "Nhà hát Hòa Bình, TP.HCM",
  "timezone": "Asia/Ho_Chi_Minh",
  "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
  "imageMediaId": "0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
  "startsAt": "2026-11-12T12:00:00Z", "startsAtLocal": "2026-11-12T19:00",
  "endsAt": "2026-11-12T15:00:00Z", "endsAtLocal": "2026-11-12T22:00",
  "saleStartsAt": "2026-11-05T03:00:00Z", "saleStartsAtLocal": "2026-11-05T10:00",
  "saleEndsAt": "2026-11-12T10:00:00Z", "saleEndsAtLocal": "2026-11-12T17:00",
  "highDemand": true,
  "ticketCodePrefix": "GM",
  "rowVersion": 7,
  "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff",
  "ticketTypes": [
    { "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082", "name": "Hạng A", "model": "SEAT", "price": 1200000, "currency": "VND", "colorIndex": 1, "gaCapacity": null, "sortOrder": 1, "used": 0 },
    { "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "name": "Fanzone", "model": "ZONE", "price": 450000, "currency": "VND", "colorIndex": 2, "gaCapacity": null, "sortOrder": 2, "used": 0 }
  ],
  "steps": { "info": "DONE", "ticketTypes": "DONE", "seatMap": "TODO", "preview": "TODO", "publish": "TODO" },
  "mapLocked": false,
  "publishedAt": null,
  "createdAt": "2026-10-20T08:00:00Z",
  "updatedAt": "2026-10-21T09:30:00Z"
}
```

`steps.<bước>` ∈ `TODO`, `DONE` (bước Thông tin `DONE` khi mọi trường DR-25 hợp lệ; Loại vé khi có ≥ 1 loại vé hợp lệ; Sơ đồ khi có phiên bản đã xuất bản và qua validate, hoặc `null` khi không có loại vé `SEAT`/`ZONE`, DR-70). `seatMapId` là `null` khi chưa có sơ đồ. `mapLocked = true` khi `status <> 'DRAFT'` và `now() >= saleStartsAt` (DR-37). `rowVersion` tăng 1 mỗi lần `PATCH` hoặc đổi trạng thái.

### E-23 `POST /organizer` · `createOrganizer`

- **Mục đích / UC:** Lập hồ sơ tổ chức; tài khoản có thêm vai trò `ORGANIZER` (UC-07, FL-05; DR-23).
- **Quyền:** Đăng nhập; CSRF.
- **Tham số (body):**

  | Trường | Kiểu | Bắt buộc | Ràng buộc |
  | --- | --- | --- | --- |
  | `name` | string | có | 1–120 ký tự sau trim; `required`, `too_long` |
  | `contactEmail` | string | không | Quy tắc email của DR-21; trống hoặc vắng → `null` (dùng email đăng nhập khi gửi cho người mua) |

- **Request:** `{ "name": "Giao Mùa Studio", "contactEmail": "lienhe@giaomua.example" }`
- **Response 201:** `{ "organizerId": "0199a000-2222-7333-8444-555566667777", "name": "Giao Mùa Studio", "contactEmail": "lienhe@giaomua.example", "createdAt": "2026-10-20T07:55:00Z" }`. Sau đó `GET /me` (E-04) trả `roles: ["BUYER","ORGANIZER"]` và `organizer`.
- **Lỗi:** 409 `ORGANIZER_EXISTS` (tài khoản đã có hồ sơ); 422 `VALIDATION_FAILED` (`name`: `required`, `too_long`; `contactEmail`: `invalid_email`).
- **Nguồn dữ liệu:** `INSERT INTO organizer (owner_user_id, name, contact_email)`; vi phạm `organizer_owner_user_id_key` (UNIQUE) → 409. Xóa cache session Caffeine của người gọi để `GET /me` thấy hồ sơ mới.
- **Cache:** không. **Hiệu năng:** p95 < 15 ms.

### E-24 `GET /organizer/events` · `listOrganizerEvents`

- **Mục đích / UC:** Danh sách sự kiện của tổ chức kèm số vé, cho Studio 01 và 07 (UC-07, UC-10, FL-32; DR-65, DR-71).
- **Quyền:** Tổ chức.
- **Tham số:** `limit` (query, mặc định 20, 1–100), `cursor` (query); `status` (query, tùy chọn, một trong `DRAFT`, `PUBLISHED`, `PAUSED`, `ENDED`, `CANCELLED`).
- **Response 200:** sắp xếp `(createdAt, eventId)` giảm dần.

  ```json
  {
    "items": [
      { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Hòa nhạc Giao Mùa", "status": "PUBLISHED", "displayStatus": "ON_SALE",
        "startsAt": "2026-11-12T12:00:00Z", "timezone": "Asia/Ho_Chi_Minh", "imageUrl": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
        "capacity": 220, "sold": 48, "held": 5, "available": 167, "rowVersion": 12 },
      { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34", "name": "Hội thảo Kiến trúc Hệ thống", "status": "DRAFT", "displayStatus": "DRAFT",
        "startsAt": null, "timezone": "Asia/Ho_Chi_Minh", "imageUrl": null,
        "capacity": 0, "sold": 0, "held": 0, "available": 0, "rowVersion": 2 }
    ],
    "nextCursor": null
  }
  ```

  `capacity = sold + held + available` (không tính `REMOVED`); sự kiện `DRAFT` chưa có unit nên cả bốn số là 0.
- **Lỗi:** 400 `BAD_REQUEST` (`invalid_cursor`); 422 `VALIDATION_FAILED` (`limit`, `status`).
- **Nguồn dữ liệu:** trang `event` theo `event_organizer_idx`; một truy vấn gộp cho ≤ 20 event của trang: `SELECT event_id, status, count(*) FROM inventory_unit WHERE event_id = ANY(:ids) AND status <> 'REMOVED' GROUP BY 1, 2` (`unit_event_idx`; DR-71).
- **Cache:** không. **Hiệu năng:** p95 < 40 ms (20 event, mỗi event ≤ 100.000 unit: đọc theo chỉ mục, kế hoạch; đo ở S-03).

### E-25 `POST /organizer/events` · `createEvent`

- **Mục đích / UC:** Tạo sự kiện nháp chỉ với tên, rồi vào bước Thông tin (UC-07, FL-06; DR-70, DR-25).
- **Quyền:** Tổ chức; CSRF.
- **Tham số (body):** `name` (string, 1–120 ký tự, bắt buộc).
- **Request:** `{ "name": "Hòa nhạc Giao Mùa" }`
- **Response 201:** `OrganizerEventView` với `status = DRAFT`, `description = ""`, `venue = ""`, `timezone = "Asia/Ho_Chi_Minh"` (`PLATFORM_TIMEZONE`), mọi thời gian `null`, `ticketCodePrefix = "TK"`, `highDemand = false`, `ticketTypes = []`, `seatMapId = null`, `rowVersion = 0`, `steps` toàn `TODO`. Header `Location: /api/v1/organizer/events/{eventId}`.
- **Lỗi:** 422 `VALIDATION_FAILED` (`name`: `required`, `too_long`).
- **Nguồn dữ liệu:** `INSERT INTO event (organizer_id, name, ticket_code_prefix) VALUES (:org, :name, 'TK')`.
- **Cache:** không. **Hiệu năng:** p95 < 15 ms.

### E-26 `GET /organizer/events/{eventId}` · `getOrganizerEvent`

- **Mục đích / UC:** Đọc một sự kiện để sửa, kèm loại vé, `rowVersion` và trạng thái từng bước (UC-07, FL-06, FL-07; DR-65, DR-70).
- **Quyền:** Tổ chức (chủ sự kiện, ngược lại 404).
- **Tham số:** `eventId` (path).
- **Response 200:** `OrganizerEventView` (ví dụ ở đầu mục 7). `ticketTypes` không gồm loại vé đã xóa (`deleted_at IS NOT NULL`), sắp theo `sortOrder`; `used` = số unit `HELD` + `SOLD` (DR-30).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `event` (PK, `organizer_id` khớp), `ticket_type` (`ticket_type_event_idx`), `seat_map` (`event_id` UNIQUE), `seat_map_version`; `used` bằng `SELECT ticket_type_id, count(*) FROM inventory_unit WHERE event_id = :e AND status IN ('HELD','SOLD') GROUP BY 1`. `…Local` suy ra bằng `at time zone event.timezone`.
- **Cache:** không. **Hiệu năng:** p95 < 20 ms.

### E-27 `PATCH /organizer/events/{eventId}` · `updateEvent`

- **Mục đích / UC:** Sửa thông tin sự kiện; sau khi đã xuất bản thì thông báo cho người mua nếu đổi giờ hoặc địa điểm (UC-07, UC-17, FL-06, FL-11; DR-25, DR-29, DR-70, DR-12).
- **Quyền:** Tổ chức; CSRF.
- **Tham số (body, mọi trường trừ `rowVersion` là tùy chọn; chỉ trường có mặt được ghi):**

  | Trường | Kiểu | Ghi chú |
  | --- | --- | --- |
  | `rowVersion` | int | **bắt buộc**; khác giá trị hiện tại → 409 `STALE_EVENT_VERSION` |
  | `name`, `description`, `venue` | string | DR-25 |
  | `timezone` | string IANA | `invalid_timezone`; sau khi xuất bản → `locked_after_publish` |
  | `startsAtLocal`, `endsAtLocal`, `saleStartsAtLocal`, `saleEndsAtLocal` | `yyyy-MM-dd'T'HH:mm` hoặc `null` | giờ địa phương theo `timezone` trong cùng request hoặc đã lưu; `null` chỉ khi `DRAFT` |
  | `imageMediaId` | uuid hoặc `null` | ảnh phải thuộc tổ chức, `purpose = EVENT_IMAGE`; sai → 422 `invalid_media` |
  | `highDemand` | boolean | bật phòng chờ có xáo ngẫu nhiên (DR-57) |
  | `ticketCodePrefix` | string `^[A-Z]{2}$` | chỉ khi `DRAFT`, sau đó `locked_after_publish` (DR-52) |

- **Request:** `{ "rowVersion": 7, "venue": "Nhà hát Thành phố, TP.HCM", "startsAtLocal": "2026-11-12T20:00", "endsAtLocal": "2026-11-12T23:00" }`
- **Response 200:** `{ "event": <OrganizerEventView>, "notifiedOrders": <int> }`; `event` là `OrganizerEventView` đầy đủ (ví dụ ở đầu mục 7) với `rowVersion` đã tăng (7 → 8). `notifiedOrders` là số email đổi lịch xếp hàng: `0` khi `DRAFT` hoặc không đổi `startsAt`/`endsAt`/`venue` (DR-29). Ví dụ sau `PATCH` ở trên với 48 đơn `PAID`: `"notifiedOrders": 48`, `event.venue = "Nhà hát Thành phố, TP.HCM"`, `event.startsAtLocal = "2026-11-12T20:00"`, `event.startsAt = "2026-11-12T13:00:00Z"`, `event.rowVersion = 8`.

- **Quy tắc:**
  - `DRAFT`: chỉ `name` phải hợp lệ; trường khác kiểm tra theo kiểu (độ dài, múi giờ) nhưng không bắt buộc đủ (DR-25).
  - `PUBLISHED`/`PAUSED`: mọi quy tắc DR-25 áp dụng cho bộ giá trị sau khi sửa (422 với `rule` tương ứng, `field` theo tên trường trong body, vd `saleEndsAtLocal`); `saleStartsAtLocal` bị khóa khi `now() >= saleStartsAt` (422 `locked_after_sale_start`, DR-143) để khóa sơ đồ (DR-37) không bị mở lại.
  - `ENDED`, `CANCELLED`: 409 `EVENT_STATE_CONFLICT`.
- **Lỗi:** 409 `STALE_EVENT_VERSION` (kèm `currentRowVersion`); 409 `EVENT_STATE_CONFLICT`; 422 `VALIDATION_FAILED` (`rule` ∈ `required`, `too_long`, `invalid_timezone`, `locked_after_publish`, `locked_after_sale_start`, `must_be_future`, `must_be_after_start`, `too_long_event`, `must_be_after_sale_start`, `after_event_start`, `invalid_media`, `invalid_prefix`).
- **Nguồn dữ liệu:** một transaction: `UPDATE event SET …, row_version = row_version + 1, updated_at = now() WHERE event_id = :e AND organizer_id = :org AND row_version = :v RETURNING *` (0 dòng → đọc lại để phân biệt 404 và 409); nếu `status <> 'DRAFT'` và `starts_at`, `ends_at` hoặc `venue` đổi: `INSERT INTO outbox (kind, payload) SELECT 'EMAIL_EVENT_CHANGED', … FROM orders WHERE event_id = :e AND status = 'PAID'` (`orders_event_idx`; payload `before`/`after`/`changed` theo DOC-15 §7.2). `CHECK event_schedule_ck` là lớp bảo vệ cuối.
- **Cache:** không. Hiệu năng: p95 < 25 ms; thêm ~1 ms mỗi 100 đơn `PAID` khi fan-out email đổi lịch (kế hoạch).

### E-28 `GET /organizer/events/{eventId}/publish-checks` · `getPublishChecks`

- **Mục đích / UC:** Danh sách điều kiện xuất bản cho Studio 06 (UC-09, FL-08; DR-25, DR-65, DR-27).
- **Quyền:** Tổ chức.
- **Tham số:** `eventId` (path).
- **Response 200:** luôn liệt kê đủ mục theo thứ tự bước studio:

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "ok": false,
    "checks": [
      { "key": "name", "ok": true },
      { "key": "venue", "ok": true },
      { "key": "timezone", "ok": true },
      { "key": "starts_at", "ok": true },
      { "key": "ends_at", "ok": true },
      { "key": "sale_window", "ok": true },
      { "key": "has_ticket_type", "ok": true },
      { "key": "ticket_type_capacity", "ok": true },
      { "key": "seat_map_published", "ok": false },
      { "key": "seat_map_valid", "ok": false, "params": { "errorCount": 3 } }
    ]
  }
  ```

  `seat_map_published` và `seat_map_valid` chỉ có khi có loại vé `SEAT`/`ZONE`. `ticket_type_capacity` đòi ≥ 1 loại vé có sức chứa > 0 (GA: `gaCapacity ≥ 1`; SEAT/ZONE: có ghế/zone gán). `seat_map_valid` là kết quả validate server của phiên bản mới nhất (DR-35). Mỗi `key` có key i18n `publish.check.<key>` (DOC-40).
- **Lỗi:** chỉ lỗi chung. Sự kiện không còn `DRAFT` vẫn trả danh sách (mọi mục `ok` tính theo dữ liệu hiện tại).
- **Nguồn dữ liệu:** `event`, `ticket_type`, `seat_map` + `seat_map_version.document` của phiên bản mới nhất; validate lại bằng JTS khi `seat_map_version` chưa có kết quả đã lưu (chi phí < 500 ms với 20.000 ghế, S-05).
- **Cache:** không. **Hiệu năng:** p95 < 30 ms (không có sơ đồ); < 600 ms (20.000 ghế).

### E-29 `POST /organizer/events/{eventId}/publish` · `publishEvent`

- **Mục đích / UC:** Xuất bản và dựng kho vé trong một transaction (UC-09, FL-08; DR-27, DR-25).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `eventId` (path); body `{ "rowVersion": 7 }` (bắt buộc, chống xuất bản bản đã đổi ở tab khác).
- **Response 200:**

  ```json
  { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "status": "PUBLISHED", "displayStatus": "UPCOMING", "rowVersion": 8,
    "publishedAt": "2026-10-21T09:45:00Z", "inventory": { "seatUnits": 180, "pools": 1, "poolUnits": 40 } }
  ```

- **Lỗi:** 409 `EVENT_STATE_CONFLICT` (không phải `DRAFT`); 409 `STALE_EVENT_VERSION`; 422 `PUBLISH_PRECONDITIONS_FAILED` (kèm `checks` các mục chưa đạt, cùng dạng E-28); 503 `OVERLOADED` khi quá `statement_timeout` 30 giây (rollback, thử lại được).
- **Nguồn dữ liệu:** các bước DR-27: `SELECT … FROM event … FOR UPDATE` (kèm `organizer_id`); kiểm tra điều kiện; `INSERT INTO inventory_unit … SELECT … FROM jsonb_path_query(document, …)` cho ghế (bỏ `blocked`), `INSERT INTO inventory_pool` rồi `CROSS JOIN generate_series(1, capacity)` cho zone và GA; `UPDATE event SET status = 'PUBLISHED', published_at = now(), seat_map_version_id = :v, row_version = row_version + 1`. Hết hạn nginx 60 giây cho route này.
- **Cache:** không. **Hiệu năng:** mục tiêu < 5 giây cho 20.000 ghế + 80.000 unit pool (kế hoạch, S-03); GA 5.000 vé < 1 giây.

### E-30 `POST /organizer/events/{eventId}/pause` · `pauseEvent`

- **Mục đích / UC:** Tạm dừng bán; reservation đang `ACTIVE` vẫn thanh toán được (UC-09, FL-09; DR-24).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `eventId` (path); body `{}`.
- **Response 200:** `EventStateResponse` (§3.2): `{ "eventId": "…", "status": "PAUSED", "displayStatus": "PAUSED", "rowVersion": 9 }`.
- **Lỗi:** 409 `EVENT_STATE_CONFLICT` (không phải `PUBLISHED`; gọi lặp khi đã `PAUSED` cũng 409).
- **Nguồn dữ liệu:** `UPDATE event SET status = 'PAUSED', row_version = row_version + 1, updated_at = now() WHERE event_id = :e AND organizer_id = :org AND status = 'PUBLISHED'`.
- **Cache:** không (client giữ `no-store`; E-07 hết hạn trong 5 giây nên người mua thấy thay đổi chậm tối đa 5 giây, `EVENT_NOT_ON_SALE` ở E-14 là nguồn sự thật). **Hiệu năng:** p95 < 10 ms.

### E-31 `POST /organizer/events/{eventId}/resume` · `resumeEvent`

- **Mục đích / UC:** Mở lại sau khi tạm dừng (UC-09, FL-09; DR-24).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `eventId` (path); body `{}`.
- **Response 200:** `EventStateResponse` với `status = PUBLISHED` và `displayStatus` tính lại (`ON_SALE`, `UPCOMING`, `SOLD_OUT` hoặc `SALE_CLOSED`).
- **Lỗi:** 409 `EVENT_STATE_CONFLICT` (không phải `PAUSED`).
- **Nguồn dữ liệu:** `UPDATE event SET status = 'PUBLISHED', … WHERE event_id = :e AND organizer_id = :org AND status = 'PAUSED' AND ends_at > now()`; 0 dòng vì quá `ends_at` → 409.
- **Cache:** không. **Hiệu năng:** p95 < 10 ms.

### E-32 `POST /organizer/events/{eventId}/close-sale` · `closeEventSale`

- **Mục đích / UC:** Đóng bán sớm, không mở lại được (UC-09, FL-09; DR-24).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `eventId` (path); body `{}`.
- **Response 200:** `EventStateResponse` với `displayStatus = "SALE_CLOSED"` và `saleEndsAt` mới (= `now()`):

  ```json
  { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "status": "PUBLISHED", "displayStatus": "SALE_CLOSED", "rowVersion": 10, "saleEndsAt": "2026-11-08T07:10:00Z" }
  ```

- **Lỗi:** 409 `EVENT_STATE_CONFLICT` (chưa tới `saleStartsAt`, đã đóng, `DRAFT`, `ENDED`, `CANCELLED`).
- **Nguồn dữ liệu:** `UPDATE event SET sale_ends_at = now(), row_version = row_version + 1 WHERE event_id = :e AND organizer_id = :org AND status IN ('PUBLISHED','PAUSED') AND sale_starts_at <= now() AND sale_ends_at > now()` (DR-24). `event_schedule_ck` vẫn đúng vì `now() <= sale_ends_at_cũ <= starts_at`.
- **Cache:** không. **Hiệu năng:** p95 < 10 ms.

### E-33 `POST /organizer/events/{eventId}/cancel` · `cancelEvent`

- **Mục đích / UC:** Hủy sự kiện, kể cả khi đã bán; đơn đã thanh toán sang chờ hoàn tiền (UC-13, FL-10; DR-28, DR-44).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `eventId` (path); body `{ "rowVersion": 12 }` (bắt buộc: hộp thoại đã hiện số đơn dựa trên bản này).
- **Response 200:** `{ "eventId": "…", "status": "CANCELLED", "displayStatus": "CANCELLED", "rowVersion": 13, "refundPendingOrders": 12, "releasingReservations": 3 }`.
- **Lỗi:** 409 `EVENT_STATE_CONFLICT` (không phải `PUBLISHED`/`PAUSED`); 409 `STALE_EVENT_VERSION`; 503 `OVERLOADED` khi quá `statement_timeout` 60 giây (rollback).
- **Nguồn dữ liệu:** transaction sáu bước của DR-28: `SELECT … FROM event … FOR UPDATE`; `UPDATE event SET status = 'CANCELLED', cancelled_at = now()`; `UPDATE orders SET status = 'REFUND_PENDING', refund_reason = 'EVENT_CANCELLED' WHERE event_id = :e AND status = 'PAID' RETURNING order_id`; `UPDATE ticket SET status = 'VOID' WHERE event_id = :e AND status = 'ISSUED'`; `UPDATE reservation SET status = 'EXPIRING', close_reason = 'EVENT_CANCELLED', expiring_since = now() WHERE event_id = :e AND status = 'ACTIVE'` (job trả vé xử lý tiếp; đơn `PENDING_PAYMENT` của chúng sang `CANCELLED` theo DR-91); `INSERT INTO outbox` một dòng `EMAIL_REFUND_PENDING` mỗi đơn ở bước 3.
- **Cache:** không. **Hiệu năng:** p95 < 2 giây với 5.000 đơn `PAID` (kế hoạch; chưa đo, ca `ENDP-` bên dưới).

### E-34 `POST /organizer/events/{eventId}/ticket-types` · `createTicketType`

- **Mục đích / UC:** Thêm loại vé (UC-07, FL-07; DR-26, DR-13).
- **Quyền:** Tổ chức; CSRF.
- **Tham số (body):**

  | Trường | Kiểu | Bắt buộc | Ràng buộc |
  | --- | --- | --- | --- |
  | `name` | string | có | 1–60 ký tự; duy nhất trong sự kiện không phân biệt hoa thường (`duplicate`) |
  | `model` | `SEAT` \| `ZONE` \| `GA` | có | |
  | `price` | int (VND) | có | `0` hoặc trong khoảng của DR-13 (`out_of_range`) |
  | `gaCapacity` | int | khi `model = GA` | 1–100.000; phải `null` hoặc vắng với `SEAT`/`ZONE` |

- **Request:** `{ "name": "Vé tiêu chuẩn", "model": "GA", "price": 750000, "gaCapacity": 5000 }`
- **Response 201:** `OrganizerTicketType` (§3.2): `{ "ticketTypeId": "0199b1c2-7b01-7f10-8a22-3c4d5e6f7081", "name": "Vé tiêu chuẩn", "model": "GA", "price": 750000, "currency": "VND", "colorIndex": 1, "gaCapacity": 5000, "sortOrder": 1, "used": 0 }`.
- **Lỗi:** 422 `TICKET_TYPE_LIMIT_REACHED` (đã đủ 5 loại vé); 422 `VALIDATION_FAILED` (`name`: `required`, `too_long`, `duplicate`; `price`: `out_of_range`; `gaCapacity`: `required`, `out_of_range`, `not_allowed`); 409 `EVENT_STATE_CONFLICT` (không còn `DRAFT`, DR-144).
- **Nguồn dữ liệu:** transaction: `SELECT … FROM event … FOR UPDATE` (tuần tự hóa hai request cùng lúc), đếm loại vé chưa xóa, chọn `color_index` nhỏ nhất chưa dùng (`SELECT min(g) FROM generate_series(1,5) g WHERE g NOT IN (…)`), `sort_order = max + 1`, `INSERT INTO ticket_type`; `ticket_type_name_uq` bắt trùng tên (`23505` → 422 `duplicate`).
- **Cache:** không. **Hiệu năng:** p95 < 15 ms.

### E-35 `PATCH /organizer/events/{eventId}/ticket-types/{ticketTypeId}` · `updateTicketType`

- **Mục đích / UC:** Sửa tên, giá, sức chứa GA; đổi mô hình khi còn nháp (UC-07, FL-07; DR-26, DR-30).
- **Quyền:** Tổ chức; CSRF.
- **Tham số (body, trường tùy chọn):** `name`, `price`, `gaCapacity`, `model` (chỉ khi `DRAFT`; sau xuất bản → 422 `locked_after_publish`).
- **Request:** `{ "gaCapacity": 4500 }`
- **Response 200:** `OrganizerTicketType` sau khi sửa.
- **Lỗi:** 409 `CAPACITY_BELOW_USED` (kèm `used`, không thấp hơn số unit `HELD` + `SOLD`; DR-30); 409 `EVENT_STATE_CONFLICT` (`ENDED`, `CANCELLED`); 422 `VALIDATION_FAILED` (như E-34, thêm `locked_after_publish`); 404 khi loại vé đã xóa.
- **Nguồn dữ liệu:** `DRAFT`: `UPDATE ticket_type`. Đã xuất bản: tăng sức chứa chèn thêm `Δ` unit vào pool và `UPDATE inventory_pool SET capacity = capacity + Δ`; giảm theo `WITH victims AS (SELECT unit_id FROM inventory_unit WHERE pool_id = :p AND status = 'AVAILABLE' LIMIT :Δ FOR UPDATE SKIP LOCKED) UPDATE inventory_unit SET status = 'REMOVED' FROM victims …`, số dòng phải bằng `Δ` (ít hơn → rollback → 409); đổi giá chỉ `UPDATE ticket_type SET price` (reservation mới dùng giá mới, DR-30). Sau khi tăng sức chứa xóa `soldout:pool:{poolId}` (DR-46).
- **Cache:** không. **Hiệu năng:** p95 < 20 ms; tăng/giảm 1.000 unit < 200 ms (kế hoạch).

### E-36 `DELETE /organizer/events/{eventId}/ticket-types/{ticketTypeId}` · `deleteTicketType`

- **Mục đích / UC:** Xóa loại vé (UC-07, FL-07; DR-26).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `eventId`, `ticketTypeId` (path).
- **Response 204:** không có body. Gọi lại với loại vé đã xóa → 404.
- **Lỗi:** 409 `TICKET_TYPE_IN_USE` (kèm `used`: có unit `HELD`/`SOLD`); 409 `EVENT_STATE_CONFLICT` (`ENDED`, `CANCELLED`).
- **Nguồn dữ liệu:** `DRAFT`: `DELETE FROM ticket_type WHERE ticket_type_id = :t AND event_id = :e`. Đã xuất bản và `count(HELD+SOLD) = 0`: một transaction `UPDATE ticket_type SET deleted_at = now()`; `UPDATE inventory_unit SET status = 'REMOVED' WHERE ticket_type_id = :t AND status = 'AVAILABLE'`; `UPDATE inventory_pool SET removed_at = now() WHERE ticket_type_id = :t`. Màu `color_index` được dùng lại cho loại vé tạo sau (DR-26).
- **Cache:** không. **Hiệu năng:** p95 < 50 ms với 5.000 unit.

### E-37 `GET /organizer/events/{eventId}/sales` · `getEventSales`

- **Mục đích / UC:** Số vé theo loại vé cho Studio 07 và hộp thoại hủy (UC-10, FL-32; DR-71, FR-20).
- **Quyền:** Tổ chức.
- **Tham số:** `eventId` (path).
- **Response 200:**

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "generatedAt": "2026-11-10T03:00:03Z",
    "totals": { "capacity": 220, "sold": 48, "held": 5, "available": 167 },
    "byTicketType": [
      { "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082", "name": "Hạng A", "model": "SEAT", "colorIndex": 1, "capacity": 180, "sold": 40, "held": 4, "available": 136 },
      { "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "name": "Fanzone", "model": "ZONE", "colorIndex": 2, "capacity": 40, "sold": 8, "held": 1, "available": 31 }
    ],
    "orders": { "paid": 31, "refundPending": 0 }
  }
  ```

  `orders.paid` và `orders.refundPending` để hộp thoại hủy (Studio 06) báo trước số đơn sẽ chuyển sang chờ hoàn tiền (DR-28).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `SELECT ticket_type_id, status, count(*) FROM inventory_unit WHERE event_id = :e AND status <> 'REMOVED' GROUP BY 1, 2` (`unit_event_idx`); `SELECT status, count(*) FROM orders WHERE event_id = :e AND status IN ('PAID','REFUND_PENDING') GROUP BY 1` (`orders_event_idx`).
- **Cache:** Redis `sales:event:{eventId}` TTL 5 giây (DR-71; tên key chốt ở DOC-17); response `no-store`. Giao diện tự làm mới mỗi 15 giây và khi bấm "Làm mới".
- **Hiệu năng:** p95 < 15 ms khi trúng cache; < 150 ms khi dựng với 100.000 unit (kế hoạch; S-03).

### E-38 `GET /organizer/events/{eventId}/seat-status` · `getEventSeatStatus`

- **Mục đích / UC:** Sơ đồ tô theo trạng thái ghế cho Studio 07 (UC-10, FL-32; DR-71, DR-62).
- **Quyền:** Tổ chức.
- **Tham số:** `eventId` (path).
- **Response 200:** cùng snapshot với E-09 (`mapVersion`, `generatedAt`, `seats.{count,held,sold}` bitset base64, `pools`), thêm `capacity` và `sold` cho từng pool và `ticketTypes[].sold`:

  ```json
  {
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "mapVersion": 2, "generatedAt": "2026-11-10T03:00:01.200Z",
    "seats": { "count": 180, "held": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "sold": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" },
    "pools": [ { "poolId": "0199b1c2-5001-7aaa-8bbb-111122223333", "zoneKey": "zone-ab12cd34", "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083", "capacity": 40, "available": 31, "held": 1, "sold": 8 } ],
    "ticketTypes": [ { "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082", "available": 136, "held": 4, "sold": 40 } ]
  }
  ```

  Sự kiện chỉ GA: `seats` là `{ "count": 0, "held": "", "sold": "" }`, `mapVersion = null`. Client tải sơ đồ bằng E-08 (`?version=`) rồi tô theo bitset.
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** dùng chung bộ dựng snapshot và cache Caffeine 2 giây của E-09 (DR-62, DR-71); thêm đếm `SOLD` theo pool từ `unit_pool_available_idx`/`unit_ticket_type_idx`.
- **Cache:** `no-store`. **Hiệu năng:** như E-09.

### E-39 `POST /organizer/media` · `uploadMedia`

- **Mục đích / UC:** Tải ảnh sự kiện hoặc ảnh nền mặt bằng lên object storage (UC-07, UC-08, FL-06, FL-26; DR-38, ADR-0015).
- **Quyền:** Tổ chức; CSRF. nginx cho body ≤ 6 MB trên route này (DOC-62).
- **Tham số (multipart/form-data):**

  | Phần | Kiểu | Bắt buộc | Ràng buộc |
  | --- | --- | --- | --- |
  | `purpose` | text | có | `EVENT_IMAGE` (≤ 2 MB) hoặc `FLOOR_PLAN` (≤ 5 MB) |
  | `file` | file | có | `image/jpeg`, `image/png`, `image/webp`; kiểm tra magic bytes, không tin `Content-Type`; mỗi cạnh ≤ 8000 px |

- **Response 201:**

  ```json
  { "mediaId": "0199b1c2-1111-7aaa-8bbb-ccccddddeeee", "purpose": "EVENT_IMAGE", "url": "/media/0199b1c2-1111-7aaa-8bbb-ccccddddeeee",
    "contentType": "image/jpeg", "sizeBytes": 430080, "width": 1920, "height": 1080 }
  ```

- **Lỗi:** 413 `PAYLOAD_TOO_LARGE` (vượt giới hạn theo `purpose`); 415 `UNSUPPORTED_MEDIA_TYPE` (request không phải `multipart/form-data`); 422 `MEDIA_INVALID` (kèm `reason` ∈ `type`, `decode`, `dimensions`); 422 `VALIDATION_FAILED` (`purpose`: `required`, `invalid`); 503 `OVERLOADED` khi storage lỗi hoặc quá timeout.
- **Nguồn dữ liệu:** sinh `media_id`; `PutObject` vào bucket `STORAGE_S3_BUCKET` với key `media/<media_id>` **trước**, rồi `INSERT INTO media (…, sha256)`; chèn lỗi → xóa object một lần, nếu xóa lỗi thì chấp nhận object sót (DR-38, DR-74). Không giữ connection DB trong lúc gọi storage. Ảnh gắn vào sự kiện qua E-27 (`imageMediaId`) và vào sơ đồ qua `canvas.background.mediaId` của E-44.
- **Cache:** không. **Hiệu năng:** p95 < 400 ms với ảnh 2 MB (chủ yếu là `PutObject` và giải mã ảnh; kế hoạch).

## 8. Studio: sơ đồ chỗ ngồi (E-40…48)

Mọi endpoint nhóm này có quyền `Tổ chức`. Tài liệu sơ đồ theo JSON Schema `seat-map.v1.json` (DR-32); các ví dụ rút gọn `rows[].seats` còn hai ghế (ID thật là UUID, `…` ở dưới là rút gọn chỉ để ví dụ ngắn; bản gửi thật không có `…`). Ví dụ tài liệu dùng chung (`MapDocument`):

```json
{
  "schemaVersion": 1,
  "canvas": { "width": 4000, "height": 3000, "seatDiameter": 20, "minSpacing": 24, "grid": 10, "background": null },
  "sections": [ { "id": "sec-a1b2c3d4", "name": "Khán đài A" } ],
  "rows": [ {
    "id": "row-1a2b3c4d", "sectionId": "sec-a1b2c3d4", "label": "A",
    "path": { "type": "arc", "start": [800, 1200], "end": [1600, 1200], "through": [1200, 1320] },
    "numbering": { "start": 1, "direction": "forward", "scheme": "sequential" },
    "ticketTypeId": "0199b1c2-7b02-7f10-8a22-3c4d5e6f7082",
    "seats": [
      { "id": "0199b1c2-9a01-7a10-9c2e-6d1f0a4b8c11", "number": "1", "x": 800, "y": 1200, "angle": 0.52 },
      { "id": "0199b1c2-9a02-7a10-9c2e-6d1f0a4b8c12", "number": "2", "x": 829, "y": 1216, "angle": 0.48, "flags": ["accessible"] }
    ]
  } ],
  "zones": [ { "id": "zone-ab12cd34", "name": "Fanzone", "capacity": 40, "ticketTypeId": "0199b1c2-7b03-7f10-8a22-3c4d5e6f7083",
    "shape": { "type": "polygon", "points": [[900, 1500], [1500, 1500], [1650, 1900], [750, 1900]] } } ],
  "decorations": [ { "id": "deco-0f1e2d3c", "kind": "stage", "text": "Sân khấu", "shape": { "type": "rect", "x": 1000, "y": 900, "w": 400, "h": 120, "rotation": 0 } } ]
}
```

Ba mã khóa dùng chung: `MAP_LOCKED_AFTER_SALE` khi `now() >= event.sale_starts_at` (DR-37; `PUT draft` và `publish`; `clone` luôn được), `REVISION_CONFLICT` khi `revision` không khớp (DR-36), `MAP_PUBLISH_BUSY` khi chờ khóa quá 5 giây (DR-37).

### E-40 `GET /organizer/events/{eventId}/map` · `getOrganizerEventMap`

- **Mục đích / UC:** Mở editor cho một sự kiện: sơ đồ, bản nháp, `revision`, phiên bản mới nhất (UC-08, FL-26; DR-65).
- **Quyền:** Tổ chức (chủ sự kiện).
- **Tham số:** `eventId` (path).
- **Response 200:**

  ```json
  {
    "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff",
    "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35",
    "name": "Sơ đồ Nhà hát Hòa Bình",
    "revision": 41,
    "draft": { "schemaVersion": 1, "...": "MapDocument" },
    "draftUpdatedAt": "2026-10-21T09:30:00Z",
    "latestVersionNo": 2,
    "latestVersion": { "versionNo": 2, "checksum": "9f2c7a41d0b3c85e6a77f21b04d9e3c15a8b6d7e2f90a1b3c4d5e6f708192a3b", "seatCount": 180, "sellableSeatCount": 176, "zoneCount": 1, "publishedAt": "2026-10-20T08:00:00Z" },
    "locked": false,
    "lockReason": null,
    "clonedFromSeatMapId": null
  }
  ```

  `draft` là `MapDocument` đầy đủ (placeholder `"...": "MapDocument"` chỉ để ví dụ ngắn). Sự kiện chưa có sơ đồ (màn "Sơ đồ trống", 04k): `200` với `seatMapId = null`, `revision = null`, `draft = null`, `latestVersionNo = 0`, `latestVersion = null`. `locked = true` kèm `lockReason = "SALE_STARTED"` khi `status <> 'DRAFT'` và `now() >= saleStartsAt` (editor mở chế độ chỉ đọc, DR-37).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `seat_map` (`event_id` UNIQUE), `seat_map_version` theo `(seat_map_id, version_no)` UNIQUE, `event`. Tài liệu `draft` ≤ 5 MB đọc một dòng.
- **Cache:** `no-store`; nên bật gzip ở nginx. **Hiệu năng:** p95 < 60 ms (bản nháp 2 MB; kế hoạch).

### E-41 `GET /organizer/maps` · `listMaps`

- **Mục đích / UC:** Danh sách sơ đồ của tổ chức để chọn nguồn nhân bản (UC-22, FL-28; DR-31, DR-65).
- **Quyền:** Tổ chức.
- **Tham số:** `limit` (query, mặc định 20, 1–100), `cursor` (query).
- **Response 200:** sắp xếp `(createdAt, seatMapId)` giảm dần; không gồm tài liệu.

  ```json
  {
    "items": [ { "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff", "name": "Sơ đồ Nhà hát Hòa Bình",
      "event": { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Hòa nhạc Giao Mùa", "status": "ENDED" },
      "latestVersionNo": 2, "seatCount": 180, "hasDraft": true, "draftUpdatedAt": "2026-10-21T09:30:00Z", "createdAt": "2026-10-10T08:00:00Z" } ],
    "nextCursor": null
  }
  ```

  Gồm cả sơ đồ của sự kiện `ENDED`, `CANCELLED` (DR-31). `seatCount` là `seat_map_version.seat_count` của phiên bản mới nhất, `0` khi chưa xuất bản.
- **Lỗi:** 400 `BAD_REQUEST` (`invalid_cursor`); 422 (`limit`).
- **Nguồn dữ liệu:** `seat_map` theo `seat_map_organizer_idx` JOIN `event`, LEFT JOIN `seat_map_version` (`seat_map_id, version_no`).
- **Cache:** không. **Hiệu năng:** p95 < 30 ms.

### E-42 `POST /organizer/maps` · `createMap`

- **Mục đích / UC:** Tạo sơ đồ trống cho một sự kiện (UC-08, FL-26; DR-31, DR-16).
- **Quyền:** Tổ chức; CSRF.
- **Tham số (body):** `eventId` (uuid, bắt buộc), `name` (string 1–120, bắt buộc); `document` (`MapDocument`, tùy chọn, mặc định tài liệu trống `schemaVersion 1` với `canvas` mặc định 4000 × 3000 của DR-32).
- **Request:** `{ "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35", "name": "Sơ đồ Nhà hát Hòa Bình" }`
- **Response 201:** `{ "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff", "eventId": "…", "name": "Sơ đồ Nhà hát Hòa Bình", "revision": 0, "latestVersionNo": 0 }`.
- **Lỗi:** 404 `NOT_FOUND` (sự kiện không tồn tại hoặc của tổ chức khác); 409 `MAP_ALREADY_EXISTS` (sự kiện đã có sơ đồ); 409 `MAP_LOCKED_AFTER_SALE`; 422 `VALIDATION_FAILED` (`name`; `document`: `schema_invalid`, kèm `errors[].path`); 413 `PAYLOAD_TOO_LARGE` (> 5 MB).
- **Nguồn dữ liệu:** `INSERT INTO seat_map (event_id, organizer_id, name, draft)`; `seat_map_event_id_key` UNIQUE bắt đua hai request (→ 409). Không có loại vé `SEAT`/`ZONE` nào của sự kiện cũng tạo được (bước Sơ đồ ẩn ở giao diện, không chặn ở API).
- **Cache:** không. **Hiệu năng:** p95 < 20 ms.

### E-43 `GET /organizer/maps/{seatMapId}` · `getMap`

- **Mục đích / UC:** Đọc một sơ đồ theo `seatMapId` (UC-08, FL-26): cùng dạng response với E-40.
- **Quyền:** Tổ chức (chủ sơ đồ, ngược lại 404).
- **Tham số:** `seatMapId` (path).
- **Response 200:** như E-40 (luôn có `seatMapId`).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** như E-40, tra theo `seat_map_id` (PK) và `organizer_id`.
- **Cache:** `no-store`. **Hiệu năng:** như E-40.

### E-44 `PUT /organizer/maps/{seatMapId}/draft` · `saveMapDraft`

- **Mục đích / UC:** Tự lưu bản nháp với khóa lạc quan theo `revision` (UC-08, FL-26; DR-36).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `seatMapId` (path); body JSON (có thể `Content-Encoding: gzip` khi > 256 KB, ≤ 5 MB sau giải nén, DR-36):

  ```json
  { "revision": 41, "document": { "schemaVersion": 1, "canvas": { "…": "như MapDocument" }, "sections": [], "rows": [], "zones": [], "decorations": [] } }
  ```

- **Response 200:** `{ "revision": 42, "draftUpdatedAt": "2026-10-21T09:30:02Z" }`.
- **Lỗi:** 409 `REVISION_CONFLICT` (kèm `currentRevision`; DR-36); 409 `MAP_LOCKED_AFTER_SALE`; 413 `PAYLOAD_TOO_LARGE`; 400 `BAD_REQUEST` (gzip hỏng); 422 `VALIDATION_FAILED` (`document`: `schema_invalid`, `limit_exceeded` với `params.limit` ∈ `size`, `seats`, `rows`, `zones`, `decorations`). Chỉ kiểm tra **schema và giới hạn**, không chạy validate nghiệp vụ (DR-35): bản nháp dở dang vẫn lưu được (DR-145).
- **Nguồn dữ liệu:** `UPDATE seat_map SET draft = :doc, draft_revision = draft_revision + 1, draft_updated_at = now() WHERE seat_map_id = :id AND organizer_id = :org AND draft_revision = :rev RETURNING draft_revision`; 0 dòng → đọc lại để phân biệt 404 và 409. Kiểm tra `now() >= sale_starts_at` bằng `JOIN event` trong cùng câu (`AND NOT EXISTS (SELECT 1 FROM event e WHERE e.event_id = seat_map.event_id AND e.status <> 'DRAFT' AND now() >= e.sale_starts_at)`), không đọc rồi so ở Java (DR-37).
- **Cache:** không. **Hiệu năng:** p95 < 150 ms (1 MB), < 600 ms (5 MB); ghi `jsonb` lớn, đo ở EXP-09 (kế hoạch).

### E-45 `POST /organizer/maps/{seatMapId}/validate` · `validateMap`

- **Mục đích / UC:** Validate bản nháp đã lưu ở server, kết quả server là kết quả cuối (UC-08, FL-27; DR-35).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `seatMapId` (path); body `{}` (validate bản nháp đang lưu; client lưu trước khi gọi).
- **Response 200:** luôn 200, kể cả khi có lỗi:

  ```json
  {
    "ok": false,
    "revision": 42,
    "issues": [
      { "code": "SEAT_OVERLAP", "level": "error", "objectIds": ["row-1a2b3c4d"], "params": { "row": "C", "count": 2 } },
      { "code": "OUT_OF_CANVAS", "level": "warning", "objectIds": ["deco-0f1e2d3c"], "params": {} }
    ]
  }
  ```

  `ok = true` khi không có `issue` mức `error`. Mã vấn đề theo bảng DR-35 (`SEAT_LABEL_DUPLICATE`, `SEAT_OVERLAP`, `ROW_LABEL_MISSING`, `TICKET_TYPE_MISSING`, `TICKET_TYPE_UNUSED`, `TICKET_TYPE_MODEL_MISMATCH`, `ZONE_CAPACITY_INVALID`, `POLYGON_SELF_INTERSECTS`, `SEAT_INSIDE_ZONE`, `SEAT_LIMIT_EXCEEDED`, `ZONES_OVERLAP`, `OUT_OF_CANVAS`).
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `seat_map.draft` + `ticket_type` của sự kiện; JTS và lưới băm (DR-35), không ghi gì. Loại vé của sự kiện đã xóa mà tài liệu còn tham chiếu → `TICKET_TYPE_MISSING`.
- **Cache:** không. **Hiệu năng:** p95 < 500 ms với 20.000 ghế + 200 zone (S-05, kế hoạch).

### E-46 `POST /organizer/maps/{seatMapId}/publish` · `publishMap`

- **Mục đích / UC:** Xuất bản bản nháp thành phiên bản bất biến; nếu sự kiện đã `PUBLISHED` nhưng chưa tới giờ mở bán thì dựng lại kho vé (UC-08, UC-14, FL-27, FL-31; DR-16, DR-37, DR-35).
- **Quyền:** Tổ chức; CSRF.
- **Tham số:** `seatMapId` (path); body `{ "revision": 42 }` (bắt buộc: xuất bản đúng bản đã validate).
- **Response 201:**

  ```json
  { "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff", "versionNo": 3, "checksum": "9f2c7a41d0b3c85e6a77f21b04d9e3c15a8b6d7e2f90a1b3c4d5e6f708192a3b",
    "seatCount": 180, "sellableSeatCount": 176, "zoneCount": 1, "publishedAt": "2026-10-21T09:45:00Z", "inventoryRebuilt": true }
  ```

  `inventoryRebuilt = false` khi sự kiện còn `DRAFT` (kho vé tạo lúc xuất bản sự kiện, E-29). `checksum` là SHA-256 hex của JSON chuẩn hóa JCS (DR-32).
- **Lỗi:** 409 `REVISION_CONFLICT`; 409 `MAP_LOCKED_AFTER_SALE` (kể cả khi đua đúng giây mở bán, DR-37); 409 `MAP_PUBLISH_BUSY` (`lock_timeout` 5 giây); 422 `MAP_VALIDATION_FAILED` (kèm `issues` mức `error`); 503 `OVERLOADED`.
- **Nguồn dữ liệu:** một transaction: `SELECT … FROM event … FOR UPDATE`; `SET LOCAL lock_timeout = '5s'`; so `draft_revision`; validate server; `INSERT INTO seat_map_version (seat_map_id, version_no, document, checksum, seat_count, sellable_seat_count, zone_count)` với `version_no = latest_version_no + 1`; `UPDATE seat_map SET latest_version_no = :n`. Nếu sự kiện `PUBLISHED` và `now() < sale_starts_at`: `DELETE FROM inventory_unit WHERE event_id = :e AND ticket_type_id = ANY(:seatAndZoneTypes) AND status = 'AVAILABLE'` (số dòng phải bằng tổng unit loại vé đó, ngược lại rollback → 409 `MAP_LOCKED_AFTER_SALE`), xóa pool zone, chèn lại như E-29, `UPDATE event SET seat_map_version_id = :v`. Trigger `seat_map_version_immutable` chặn mọi `UPDATE`/`DELETE` bản đã xuất bản.
- **Cache:** không. **Hiệu năng:** p95 < 1,5 giây khi sự kiện còn nháp (validate + ghi 2 MB); < 6 giây khi dựng lại kho 20.000 ghế (kế hoạch; S-03, S-05).

### E-47 `GET /organizer/maps/{seatMapId}/versions` · `listMapVersions`

- **Mục đích / UC:** Danh sách phiên bản đã xuất bản cho hộp thoại xuất bản và chọn nguồn nhân bản (UC-08, FL-27, FL-28; DR-65).
- **Quyền:** Tổ chức.
- **Tham số:** `seatMapId` (path).
- **Response 200:** sắp xếp `versionNo` giảm dần; không phân trang (một sơ đồ có ít phiên bản vì khóa từ giờ mở bán, DR-37).

  ```json
  {
    "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff",
    "items": [
      { "versionNo": 2, "checksum": "9f2c7a41d0b3c85e6a77f21b04d9e3c15a8b6d7e2f90a1b3c4d5e6f708192a3b", "seatCount": 180, "sellableSeatCount": 176, "zoneCount": 1, "publishedAt": "2026-10-20T08:00:00Z", "inUse": true },
      { "versionNo": 1, "checksum": "1b7d3e90c2a4f6e8d0b2a4c6e8f01234567890abcdef1234567890abcdef1234", "seatCount": 176, "sellableSeatCount": 172, "zoneCount": 1, "publishedAt": "2026-10-12T08:00:00Z", "inUse": false }
    ]
  }
  ```

  `inUse = true` cho phiên bản mà `event.seat_map_version_id` đang trỏ tới.
- **Lỗi:** chỉ lỗi chung.
- **Nguồn dữ liệu:** `seat_map_version` theo `(seat_map_id, version_no)`; không đọc `document`.
- **Cache:** không. **Hiệu năng:** p95 < 15 ms.

### E-48 `POST /organizer/maps/{seatMapId}/clone` · `cloneMap`

- **Mục đích / UC:** Dùng lại sơ đồ của sự kiện khác bằng cách nhân bản toàn bộ và sinh ID mới (UC-22, FL-28; DR-31). `{seatMapId}` là sơ đồ nguồn.
- **Quyền:** Tổ chức (sở hữu cả sơ đồ nguồn và sự kiện đích); CSRF.
- **Tham số (body):**

  | Trường | Kiểu | Bắt buộc | Ràng buộc |
  | --- | --- | --- | --- |
  | `eventId` | uuid | có | sự kiện đích, của cùng tổ chức, chưa có sơ đồ |
  | `source` | `draft` \| `version` | có | |
  | `versionNo` | int | khi `source = version` | phải tồn tại; vắng khi `draft` |

- **Request:** `{ "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c36", "source": "version", "versionNo": 2 }`
- **Response 201:**

  ```json
  { "seatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeee0001", "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c36", "name": "Sơ đồ Nhà hát Hòa Bình", "revision": 0,
    "latestVersionNo": 0, "clonedFromSeatMapId": "0199b1c2-3333-7bbb-8ccc-ddddeeeeffff",
    "seatCount": 180, "unmappedTicketTypes": [ { "sourceName": "Hạng B", "affected": { "rows": 3, "zones": 0 } } ] }
  ```

  `unmappedTicketTypes` liệt kê loại vé của nguồn không khớp tên và mô hình với loại vé của sự kiện đích; các hàng, ghế, zone đó để trống `ticketTypeId` và validate báo `TICKET_TYPE_MISSING` (DR-31).
- **Lỗi:** 404 `NOT_FOUND` (nguồn, phiên bản hoặc sự kiện đích không thuộc tổ chức); 409 `MAP_ALREADY_EXISTS` (sự kiện đích đã có sơ đồ); 409 `MAP_LOCKED_AFTER_SALE` (sự kiện đích đã qua giờ mở bán); 422 `VALIDATION_FAILED` (`versionNo`: `required` khi `source = version`, `not_allowed` khi `draft`).
- **Nguồn dữ liệu:** một transaction: đọc `seat_map.draft` hoặc `seat_map_version.document` của nguồn; sinh lại mọi ID (UUIDv7 mới cho ghế; `^(sec|row|zone|deco)-[a-z0-9]{8}$` mới cho phần còn lại) trong bộ nhớ; ánh xạ `ticketTypeId` theo `lower(name)` và `model` sang `ticket_type` của sự kiện đích; giữ nguyên `canvas.background.mediaId` (ảnh bất biến, không sao chép byte, DR-38); `INSERT INTO seat_map (event_id, organizer_id, name, draft, cloned_from_seat_map_id)` với `draft_revision = 0`.
- **Cache:** không. **Hiệu năng:** p95 < 400 ms với 20.000 ghế (sinh 20.000 UUIDv7 và ghi 2–5 MB; kế hoạch).

## 9. Test bắt buộc (`ENDP-`)

Test cho hợp đồng; test hành vi của từng endpoint nằm ở tài liệu thiết kế tương ứng (`AU-`, `INVT-`, `IDEM-`, `PAY-`, `EV-`, `MV-`, `ADM-`…). Ca `ENDP-` chạy ở module `api` bằng `MockMvc`/Testcontainers.

| ID | Ca | Kết quả mong đợi |
| --- | --- | --- |
| ENDP-01 | `OpenApiExportTest` xuất `openapi.json` rồi so tập `operationId` với cột `operationId` ở §2 | Trùng đúng 48; thừa hoặc thiếu làm test đỏ |
| ENDP-02 | Mọi mục §2 có đúng một `E-xx` ở §4–§8 và ngược lại | Không lệch |
| ENDP-03 | Gọi từng endpoint không `Công khai` không có session | 401 `UNAUTHENTICATED`, không endpoint nào 200 |
| ENDP-04 | Gọi từng endpoint `POST/PUT/PATCH/DELETE` có session nhưng thiếu `X-CSRF-Token` (trừ E-01, E-02, E-22) | 403 `CSRF_TOKEN_INVALID` |
| ENDP-05 | Tài khoản không có hồ sơ tổ chức gọi từng `/organizer/**` (trừ E-23) | 403 `ORGANIZER_PROFILE_REQUIRED` |
| ENDP-06 | Tổ chức B gọi từng `/organizer/**` có `{id}` với tài nguyên của tổ chức A | 404 `NOT_FOUND`, không 403, không lộ tồn tại (DR-23) |
| ENDP-07 | Mọi response thành công của §4–§8 khớp schema OpenAPI, không trường `null` bị bỏ | Hợp lệ; `additionalProperties = false` |
| ENDP-08 | `PATCH /organizer/events/{id}` với `rowVersion` cũ | 409 `STALE_EVENT_VERSION` kèm `currentRowVersion`; DB không đổi |
| ENDP-09 | `PATCH` đổi `venue` của sự kiện `PUBLISHED` có 3 đơn `PAID` | `notifiedOrders = 3` và đúng 3 dòng `outbox` `EMAIL_EVENT_CHANGED` |
| ENDP-10 | `PATCH` `saleStartsAtLocal` sau khi đã qua `saleStartsAt` | 422 `locked_after_sale_start` |
| ENDP-11 | `POST …/ticket-types` lần thứ 6 | 422 `TICKET_TYPE_LIMIT_REACHED`; hai request song song khi đang có 4 loại → đúng một thành công |
| ENDP-12 | `PUT …/draft` hai tab cùng `revision` 41 | Một bên 200 `revision = 42`, bên kia 409 `REVISION_CONFLICT` |
| ENDP-13 | `PUT …/draft` với `now() >= saleStartsAt` | 409 `MAP_LOCKED_AFTER_SALE`; `draft` không đổi |
| ENDP-14 | `POST …/maps/{id}/clone` sơ đồ 180 ghế | 201; 180 UUID ghế khác hoàn toàn nguồn; hình học và nhãn trùng (DR-31) |
| ENDP-15 | `POST …/cancel` sự kiện có 12 đơn `PAID` | `refundPendingOrders = 12`; 12 dòng outbox `EMAIL_REFUND_PENDING`; không vé `ISSUED` |
| ENDP-16 | `POST /organizer/media` file `.png` có nội dung là JPEG hoặc 9000 px | 422 `MEDIA_INVALID` (`decode`/`dimensions`); không object nào trong bucket |
| ENDP-17 | Mọi `errors[].rule` của 422 nằm trong tập `rule` đã liệt kê ở mục tương ứng | Không `rule` lạ |

## 10. Quyết định phát sinh khi viết

Các mục dưới đây do Claude chốt theo quyền Owner ủy quyền (master plan §0.1); chưa có số DR, cần merge vào sổ quyết định.

### DR-143 · Khóa `saleStartsAt` sau giờ mở bán — **Chốt** (Claude, Owner ủy quyền)

- **Vấn đề:** DR-37 khóa sơ đồ theo `now() >= sale_starts_at`. DR-25 và DR-30 không cấm dời `saleStartsAt` sang tương lai sau khi đã qua; làm vậy sẽ mở khóa sơ đồ và cho dựng lại kho vé sau khi đã có đơn.
- **Quyết định:** Khi `now() >= saleStartsAt` lưu, `PATCH` không được đổi `saleStartsAtLocal` (422 `locked_after_sale_start`). Trước giờ mở bán đổi tự do.
- **Hệ quả:** Giờ mở bán không đảo ngược được sau khi mở. Ghi vào DOC-20, DOC-55, DOC-35 (`rule` mới), DOC-40 (`validation.locked_after_sale_start`).

### DR-144 · Loại vé chỉ thêm khi sự kiện còn nháp — **Chốt** (Claude, Owner ủy quyền)

- **Vấn đề:** DR-26 và DR-30 nói đổi giá, sức chứa, xóa loại vé khi đang bán, nhưng không nói thêm loại vé mới sau xuất bản; loại vé `SEAT`/`ZONE` mới cần sửa sơ đồ (khóa từ giờ mở bán), loại GA mới cần dựng pool.
- **Quyết định:** `POST …/ticket-types` chỉ khi `DRAFT`; sau đó 409 `EVENT_STATE_CONFLICT`. Sau xuất bản chỉ sửa (E-35) và xóa (E-36).
- **Hệ quả:** Muốn thêm hạng vé khi đang bán phải tạo sự kiện mới. Ghi vào DOC-20, DOC-56.

### DR-145 · `PUT draft` chỉ kiểm tra schema, không validate nghiệp vụ — **Chốt** (Claude, Owner ủy quyền)

- **Vấn đề:** DR-35 validate ở client liên tục và server khi xuất bản; DR-36 tự lưu mỗi 2 giây. Nếu `PUT draft` từ chối bản có lỗi nghiệp vụ, người dùng không lưu được bản dở.
- **Quyết định:** E-44 kiểm tra JSON Schema và giới hạn cứng (5 MB, 20.000 ghế, 1.000 hàng, 200 zone, 200 trang trí); mọi mã DR-35 chỉ chạy ở E-45 và E-46.
- **Hệ quả:** Bản nháp có thể chứa ghế chồng nhau; `publish-checks` (E-28) và E-46 chặn xuất bản. Ghi vào DOC-21, DOC-22.

## 11. Câu hỏi còn mở

- **Tên key Redis `sales:event:{eventId}`** (E-37) tạm đặt; chốt ở DOC-17 (P5).
- **Số liệu hiệu năng** ở mọi mục là kế hoạch; chưa có số đo. Đo ở EXP-02, EXP-09, EXP-10 và S-03, S-05.
- **Mã `rule` mới** (`locked_after_sale_start`, `invalid_media`, `invalid_prefix`, `schema_invalid`, `limit_exceeded`, `duplicate`, `out_of_range`, `not_allowed`, `invalid_email`) cần thêm vào bảng `rule` của DOC-35 §3 và key `validation.<rule>` của DOC-40.
