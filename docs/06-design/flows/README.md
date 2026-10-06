# Mục lục luồng chi tiết

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-82
> Phụ thuộc: [DOC-04](../../01-product/use-cases.md) (22 UC), [DOC-12](../../03-architecture/code-architecture.md) §2 (module và layer), [DOC-07](../../03-architecture/system-context-and-containers.md), [DOC-06](../../02-glossary.md), [Sổ quyết định](../../00-decision-register.md) (DR-06, 10, 21, 41, 57, 58, 62, 63, 65, 67), master plan §3.2, §3.4
> Người dùng chính: tác giả DOC-83…90; tác giả màn hình DOC-42…60 (cột "Luồng"); mọi task `Pn-xx` có tham chiếu `FL-xx`

Tài liệu này là mục lục của 35 luồng chi tiết `FL-01…35` (DOC-83…90) và là nơi duy nhất định nghĩa **tên thành phần tham gia** dùng trong mọi sơ đồ tuần tự. Nó không mô tả từng bước của luồng (nằm ở DOC-83…90), không lặp payload (DOC-37), DDL (DOC-14, 15) hay chuỗi giao diện (DOC-40). Use case nói người dùng làm gì; luồng chi tiết nói từng tầng gọi gì, ở transaction nào, và người dùng thấy gì nếu tiến trình chết sau mỗi commit.

## 1. Quy ước tên thành phần tham gia

Mọi sơ đồ `sequenceDiagram` trong `flows/` khai báo `participant` bằng đúng tên ở bảng dưới (cột "Tên trong sơ đồ"). Cột "Mã" là chỗ tìm thành phần trong repo.

| Loại | Tên trong sơ đồ | Mã | Ghi chú |
| --- | --- | --- | --- |
| Tác nhân | `Buyer`, `Organizer`, `Operator`, `Guest` | — | `actor`. Người mua, người tổ chức, người vận hành; khách chưa đăng nhập |
| Màn hình | tên component PascalCase của màn, ví dụ `EventDetail`, `SeatPicker`, `Checkout` | `frontend/src/pages/<Name>.tsx` (hoặc `features/<feature>/<Name>.tsx`) | Tên chính thức do bảng ở DOC-41 quyết định; bảng §3 dưới đây là tên dùng ở thời điểm viết (DR-142) |
| SPA | `web` | `frontend/` | Phần không thuộc một màn: `apiClient` (interceptor `X-Server-Time`, `Idempotency-Key`, CSRF), store, `localStorage` |
| Edge | `nginx` | `ops/nginx/` | Rate limit (DR-55), cache `/media/`, proxy `/api` |
| API | `api` | `io.ticket.<module>.controller.<Name>Controller.<method>` | Ghi cả module, ví dụ `reservation.ReservationController.create` |
| Service | tên class service, ví dụ `ReservationService`, `OrderService`, `PublishService` | `io.ticket.<module>.service.<Name>` | Ranh giới transaction (DOC-12 §2). Gọi chéo module qua `…Api` |
| Job | `OutboxRelay`, `ReservationExpiryJob`, `PaymentReconcileJob`, `AdmissionTicker`, `EventLifecycleJob`, `InvariantChecker`, `RetentionJob` | `io.ticket.<module>.job.<Name>` | `ReservationExpiryJob` là "job trả vé" trong plan; `InvariantChecker` thuộc module `invariant` |
| Database | `db` | PostgreSQL 18 | Ghi tên bảng ở mũi tên, ví dụ `db: inventory_unit` |
| Redis | `redis` | Redis 8 | Ghi tên key hoặc script Lua (`admit.lua`) ở mũi tên |
| Object storage | `s3` | SeaweedFS (S3) | Ảnh sự kiện, ảnh nền sơ đồ |
| Cổng thanh toán | `stripe` (profile `stripe`) hoặc `fake` (cổng giả, DR-51) | `io.ticket.payment.client.PaymentGateway` | Sơ đồ nào chia hai nhánh dùng một participant `gateway` và ghi `stripe` hoặc `fake` ở mũi tên |
| Mail | `smtp` | Mailpit (dev) hoặc nhà cung cấp SMTP | Chỉ `smtp` mới gửi; `OutboxRelay` và `AuthService` là hai bên gọi |
| Công cụ | `make`, `k6`, `psql` | — | Chỉ dùng ở FL-18, FL-19, FL-25 |

Quy tắc vẽ:

1. Thứ tự participant từ trái sang phải: tác nhân → màn hình → `web` → `nginx` → `api` → service → job → `db` → `redis` → `s3` → cổng thanh toán → `smtp`.
2. Mỗi transaction mở bằng `Note over <service>,db: T<n> begin` và đóng bằng `T<n> commit` hoặc `T<n> rollback`.
3. Mũi tên gọi API ghi `METHOD /path (E-xx)`; chưa có `E-xx` thì ghi `(E-?)` và để câu hỏi mở (DOC-37 gán mã).
4. Mỗi `alt` gắn nhãn lỗi `E1`, `E2`… khớp bảng "Luồng lỗi" của cùng FL; dòng đầu của nhánh nêu mã lỗi API (`code` tiếng Anh, DR-64).
5. Sau mỗi commit có một dòng "Nếu tiến trình chết ở đây: …" nêu người dùng thấy gì.
6. FL con đánh `FL-xx.y` khi một luồng có nhánh dài đủ tách (hiện chưa có FL con nào; số `FL-xx` đã gán không đổi).

## 2. Bảng luồng

Cột "Màn hình" dùng tên component ở §3. "Endpoint" bỏ tiền tố `/api/v1` (DR-63); mã `E-xx` gán ở DOC-37.

| FL | Luồng | UC | Màn hình | Endpoint | File | Gate |
| --- | --- | --- | --- | --- | --- | --- |
| FL-01 | Xin magic link | UC-01 | `Login` (DOC-44) | `POST /auth/magic-link` | `auth-and-account.md` (DOC-83) | P1 |
| FL-02 | Mở link và nhận session | UC-01 | `AuthCallback`, `Login` (DOC-44) | `POST /auth/verify`, `GET /me` | DOC-83 | P1 |
| FL-03 | Đăng xuất và hết phiên | UC-01 | mọi màn; `Login` (`reason=session_expired`, DOC-44) | `POST /auth/logout`, mọi endpoint (401 `UNAUTHENTICATED`) | DOC-83 | P1 |
| FL-04 | Đổi ngôn ngữ giao diện | UC-20 | mọi màn (`LanguageSwitcher` ở header) | `PATCH /me`, `GET /me` | DOC-83 | P1 |
| FL-05 | Lập hồ sơ tổ chức | UC-07 | `StudioOrganizerProfile` (DOC-53) | `POST /organizer` | `studio-events.md` (DOC-84) | P2 |
| FL-06 | Tạo và sửa thông tin sự kiện kèm ảnh | UC-07 | `StudioEventInfo` (DOC-55) | `POST /organizer/events`, `GET/PATCH /organizer/events/{id}`, `POST /organizer/media`, `GET /media/{id}` | DOC-84 | P2 |
| FL-07 | Quản lý loại vé | UC-07 | `StudioTicketTypes` (DOC-56) | `POST/PATCH/DELETE /organizer/events/{id}/ticket-types` | DOC-84 | P2 |
| FL-08 | Xuất bản và mở bán | UC-09 | `StudioPublish` (DOC-59), `StudioPreview` (DOC-58) | `GET …/publish-checks`, `POST …/publish` | DOC-84 | P2 |
| FL-09 | Tạm dừng, mở lại và đóng bán sớm | UC-09 | `StudioPublish` (DOC-59), `StudioOverview` (DOC-54) | `POST …/pause`, `POST …/resume`, `POST …/close-sale` | DOC-84 | P2 |
| FL-10 | Hủy sự kiện đã bán | UC-13 | `StudioPublish` (DOC-59); `MyTickets` (DOC-50) | `POST …/cancel` | DOC-84 | P2 |
| FL-11 | Đổi giờ hoặc địa điểm | UC-17 | `StudioEventInfo` (DOC-55) | `PATCH /organizer/events/{id}` | DOC-84 | P2 |
| FL-12 | Xem danh sách và chi tiết sự kiện | UC-02 | `EventList` (DOC-42), `EventDetail` (DOC-43) | `GET /events`, `GET /events/{id}`, `GET /media/{id}` | `ga-purchase.md` (DOC-85) | P2 |
| FL-13 | Giữ vé GA | UC-03 | `QuantityPicker` (DOC-47) | `POST /events/{id}/reservations` | DOC-85 | P2 |
| FL-14 | Nhận vé đơn 0 đồng | UC-04, UC-05 | `Checkout` (DOC-48), `OrderResult` (DOC-49) | `POST /orders/{id}/confirm-free`, `GET /orders/{id}` | DOC-85 | P2 |
| FL-15 | Hủy giữ vé (đường nhanh) | UC-06 | `Checkout` (DOC-48) | `DELETE /reservations/{id}` | DOC-85 | P2 |
| FL-16 | Job trả vé khi hết hạn | UC-11 | — (job); `Checkout` thấy `RESERVATION_NOT_ACTIVE` | không có (job `ReservationExpiryJob`) | DOC-85 | P2 |
| FL-17 | Xem vé của tôi | UC-05 | `MyTickets` (DOC-50), `OrderResult` (DOC-49) | `GET /me/tickets`, `GET /me/orders`, `GET /orders/{id}` | DOC-85 | P2 |
| FL-18 | Kiểm tra bất biến định kỳ và chạy tay | UC-18 | — | không có (`InvariantChecker`, `make invariants`) | `operations.md` (DOC-86) | P2 |
| FL-19 | Chạy một thực nghiệm | UC-21 | — | không có (`make exp EXP=xx`) | DOC-86 | P2 |
| FL-20 | Thanh toán bằng thẻ | UC-04 | `Checkout` (DOC-48), `OrderResult` (DOC-49) | `GET /reservations/{id}`, `POST /orders/{id}/payment-intent`, `GET /orders/{id}` | `payment.md` (DOC-87) | P3 |
| FL-21 | Xác nhận qua webhook | UC-04 | `OrderResult` (DOC-49) | `POST /webhooks/stripe` | DOC-87 | P3 |
| FL-22 | Job trả vé hủy PaymentIntent | UC-11 | — (job) | không có (job; cổng `stripe`/`fake`) | DOC-87 | P3 |
| FL-23 | Thanh toán đến trễ | UC-15 | `OrderResult` (DOC-49); email DOC-51 | `POST /webhooks/stripe` | DOC-87 | P3 |
| FL-24 | Đối chiếu khi webhook thất lạc | UC-16 | — (job) | không có (`PaymentReconcileJob`; RB-03) | DOC-87 | P3 |
| FL-25 | Hoàn tiền thủ công | UC-19 | — (Stripe Dashboard, `psql`) | không có (RB-01) | DOC-87 | P3 |
| FL-26 | Vẽ hàng ghế và tự lưu bản nháp | UC-08 | `StudioMapEditor` (DOC-57) | `GET /organizer/events/{id}/map`, `POST /organizer/maps`, `PUT /organizer/maps/{id}/draft`, `POST /organizer/media` | `seat-map-editing.md` (DOC-88) | P4 |
| FL-27 | Validate và xuất bản sơ đồ trước mở bán | UC-08 | `StudioMapEditor` (DOC-57) | `POST /organizer/maps/{id}/validate`, `POST /organizer/maps/{id}/publish`, `GET /organizer/maps/{id}/versions` | DOC-88 | P4 |
| FL-28 | Dùng lại sơ đồ từ sự kiện khác | UC-22 | `StudioMapEditor` (DOC-57) | `GET /organizer/maps`, `POST /organizer/maps/{id}/clone` | DOC-88 | P4 |
| FL-29 | Xem sơ đồ kèm tình trạng chỗ | UC-02 | `SeatPicker` (DOC-46) | `GET /events/{id}/map?version=`, `GET /events/{id}/availability` | `seat-sales.md` (DOC-89) | P5 |
| FL-30 | Giữ ghế và vé khu vực | UC-03 | `SeatPicker` (DOC-46) | `POST /events/{id}/reservations` | DOC-89 | P5 |
| FL-31 | Sửa sơ đồ của sự kiện đã xuất bản | UC-14 | `StudioMapEditor` (DOC-57) | `POST /organizer/maps/{id}/publish` | DOC-89 | P5 |
| FL-32 | Theo dõi bán vé | UC-10 | `StudioOverview` (DOC-54), `StudioSales` (DOC-60) | `GET /organizer/events`, `GET …/sales`, `GET …/seat-status` | DOC-89 | P5 |
| FL-33 | Vào phòng chờ và nhận lượt | UC-12 | `WaitingRoom` (DOC-45) | `POST /events/{id}/queue`, `GET /events/{id}/queue` | `waiting-room.md` (DOC-90) | P6 |
| FL-34 | Giữ vé khi đã có lượt | UC-12, UC-03 | `SeatPicker` (DOC-46) hoặc `QuantityPicker` (DOC-47) | `POST /events/{id}/reservations` | DOC-90 | P6 |
| FL-35 | Rời hàng hoặc mất kết nối | UC-12 | `WaitingRoom` (DOC-45) | `DELETE /events/{id}/queue` | DOC-90 | P6 |

Gate của một FL là gate của file chứa nó. Mục "Màn hình" ghi "—" khi luồng không có giao diện; người đọc khi đó lấy trigger từ UC.

## 3. Tên màn hình dùng trong sơ đồ

| Tên component | Màn | URL (DR-67) | Spec |
| --- | --- | --- | --- |
| `EventList` | Danh sách sự kiện | `/` | DOC-42 |
| `EventDetail` | Sự kiện | `/events/:eventId` | DOC-43 |
| `Login` | Đăng nhập | `/login?returnTo=&reason=` | DOC-44 |
| `AuthCallback` | Xác minh magic link | `/auth/callback?token=` | DOC-44 |
| `WaitingRoom` | Phòng chờ | `/events/:eventId/queue` | DOC-45 |
| `SeatsRoute` | Phần tử router chọn `SeatPicker` hoặc `QuantityPicker`, không phải màn hình | `/events/:eventId/seats` | DOC-41 §3 |
| `SeatPicker` | Chọn chỗ | `/events/:eventId/seats` (sự kiện có `SEAT`/`ZONE`) | DOC-46 |
| `QuantityPicker` | Chọn số lượng | `/events/:eventId/seats` (sự kiện chỉ GA) | DOC-47 |
| `Checkout` | Thanh toán | `/checkout/:reservationId` | DOC-48 |
| `OrderResult` | Kết quả | `/orders/:orderId` | DOC-49 |
| `MyTickets` | Vé của tôi | `/me/tickets` | DOC-50 |
| `NotFoundPage`, `ForbiddenPage`, `UnavailablePage`, `ServerErrorPage` | Trang lỗi E1–E5 | theo ngữ cảnh | DOC-52 |
| `StudioOverview` | Studio: Tổng quan | `/studio` | DOC-54 |
| `StudioOrganizerProfile` | Studio: Lập hồ sơ | `/studio/profile` | DOC-53 |
| `StudioEventInfo` | Studio: Thông tin | `/studio/events/new`, `…/:eventId/info` | DOC-55 |
| `StudioTicketTypes` | Studio: Loại vé và giá | `…/ticket-types` | DOC-56 |
| `StudioMapEditor` | Studio: Seat map editor | `…/map` | DOC-57 |
| `StudioPreview` | Studio: Xem trước | `…/preview` | DOC-58 |
| `StudioPublish` | Studio: Xuất bản và mở bán | `…/publish` | DOC-59 |
| `StudioSales` | Studio: Theo dõi bán vé | `…/sales` | DOC-60 |

DOC-41 là nguồn cuối cùng của tên component; khi DOC-41 đổi tên, sửa bảng này trong cùng thay đổi.

## 4. Bảng UC → FL

Mọi UC ở master plan §3.4 có ít nhất một FL. Cột "Khớp DOC-04" lấy từ dòng "Liên quan" của từng UC.

| UC | Tên | FL | Khớp DOC-04 |
| --- | --- | --- | --- |
| UC-01 | Đăng nhập magic link, đăng xuất | FL-01, FL-02, FL-03 | có |
| UC-02 | Xem sự kiện và sơ đồ | FL-12, FL-29 | có |
| UC-03 | Giữ vé (ghế, khu vực, GA) | FL-13, FL-30, FL-34 | có |
| UC-04 | Thanh toán bằng thẻ | FL-14 (đơn 0 đồng), FL-20, FL-21 | có (DOC-04 ghi FL-20, FL-21; FL-14 thêm nhánh 0 đồng) |
| UC-05 | Nhận vé, xem đơn và vé của tôi | FL-14, FL-17 | có |
| UC-06 | Hủy giữ vé | FL-15 | có |
| UC-07 | Hồ sơ tổ chức, sự kiện, loại vé | FL-05, FL-06, FL-07 | có |
| UC-08 | Vẽ sơ đồ chỗ ngồi | FL-26, FL-27 | có |
| UC-09 | Xuất bản, tạm dừng, đóng bán sớm | FL-08, FL-09 | có |
| UC-10 | Theo dõi số vé đã bán | FL-32 | có |
| UC-11 | Tự trả vé khi hết hạn giữ | FL-16, FL-22 | có |
| UC-12 | Phòng chờ | FL-33, FL-34, FL-35 | có |
| UC-13 | Hủy sự kiện, chờ hoàn tiền | FL-10 | có |
| UC-14 | Sửa sơ đồ trước giờ mở bán | FL-31 | có |
| UC-15 | Thanh toán đến trễ | FL-23 | có (DOC-04 ghi thêm FL-21) |
| UC-16 | Đối chiếu khi webhook thất lạc | FL-24 | có |
| UC-17 | Thông báo đổi giờ, địa điểm | FL-11 | có |
| UC-18 | Kiểm tra bất biến | FL-18 | có |
| UC-19 | Hoàn tiền thủ công | FL-25 | có |
| UC-20 | Đổi ngôn ngữ | FL-04 | có |
| UC-21 | Chạy một thực nghiệm | FL-19 | có |
| UC-22 | Nhân bản sơ đồ | FL-28 | có |

Chiều ngược: mỗi FL ở §2 nêu UC ở cột 3, không FL nào mồ côi (FL-01…35 đều có UC).

## 5. Quy ước dùng chung

- Mỗi FL có tối thiểu: bảng participant, một sơ đồ tuần tự, bảng "Luồng lỗi" (`E1…`), mục "Nếu tiến trình chết" sau mỗi commit, danh sách test (tiền tố test theo file: `FLA-`, `FLS-`, `FLG-`, `FLO-`, `FLP-`, `FLM-`, `FLV-`, `FLW-`, đã dành ở master plan §0.4).
- Ví dụ giá trị cụ thể nằm trong bảng bước; schema ở DOC-37, DDL ở DOC-14/15, chuỗi ở DOC-40.
- Cập nhật use case → cập nhật FL tương ứng trong cùng thay đổi, và ngược lại.

## Câu hỏi còn mở

Không có. Tên component màn hình (§3) chờ DOC-41 xác nhận; nếu khác, DOC-41 thắng (DR-142).
