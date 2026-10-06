# Thuật ngữ

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-06
> Phụ thuộc: SDD gốc §1–§17, [Sổ quyết định](00-decision-register.md) (DR-01…151), [Master plan](00-master-plan.md) §0.4, §3.2
> Người dùng chính: mọi tài liệu khác; P0-08; mọi task `Pn-xx` khi đặt tên class, bảng, key cấu hình, chuỗi giao diện

Tài liệu này là nguồn duy nhất của thuật ngữ dự án: cột **Thuật ngữ** là tên dùng trong code, API và giao diện; cột **Tiếng Việt** là tên dùng trong văn bản. Mọi tài liệu khác dùng đúng từ này và không định nghĩa lại. Tài liệu chỉ nói cách dự án dùng từ, không giải thích khái niệm phổ thông; định nghĩa dài của từng cơ chế nằm ở tài liệu thiết kế được dẫn ở cột Liên quan.

Hai thuật ngữ có hai nghĩa trong dự án được tách rõ: `displayStatus` (trạng thái hiển thị của **sự kiện**, mục 1) và display status (trạng thái hiển thị của **chỗ**, mục 6); `reservation` (một lần giữ vé, mục 3) khác "đặt chỗ" thông thường.

## 1. Sự kiện và loại vé

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| event | sự kiện | Đúng một show của một tổ chức; bảng `event`. Có trạng thái lưu `DRAFT`, `PUBLISHED`, `PAUSED`, `ENDED`, `CANCELLED`. Sự kiện nhiều suất hoặc nhiều ngày ngoài phạm vi | DR-15, DOC-14, DOC-20 |
| show | suất diễn | Một lần diễn: một địa điểm, một khung giờ. Hệ thống có đúng một show cho mỗi event | DOC-01 |
| organizer | người tổ chức | Hồ sơ tổ chức (`organizer`) của người tạo sự kiện. Một tài khoản có tối đa một hồ sơ, một hồ sơ có đúng một chủ (`owner_user_id`). Có hồ sơ là có vai trò `ORGANIZER`; không có cột vai trò riêng | DR-23, DOC-19 |
| buyer | người mua | Vai trò `BUYER`: mọi tài khoản đều có. Tìm sự kiện, giữ vé, thanh toán, xem đơn và vé của mình | DR-23, PS-1, PS-2 |
| ticket type | loại vé | Một mức giá và một mô hình kho vé của một event (ví dụ VIP, Thường, Sinh viên); bảng `ticket_type`. Tối đa 5 loại mỗi event, tên duy nhất không phân biệt hoa thường, màu `type-1…5` theo thứ tự tạo | DR-26, DOC-20 |
| inventory model | mô hình kho vé | Cách một loại vé tạo ra unit: `SEAT`, `ZONE` hoặc `GA`. Không đổi sau khi xuất bản | DR-26, DOC-24 |
| SEAT | mô hình theo ghế | Người mua chọn ghế cụ thể; mỗi ghế là một `inventory_unit` có `seat_id`. Cần sơ đồ | DOC-24 |
| ZONE | mô hình theo khu vực | Người mua chọn một zone và số lượng; sức chứa của zone thành một `inventory_pool` kind `ZONE` với `capacity` unit vô danh. Cần sơ đồ | DOC-24 |
| GA | vé vào cửa tự do (general admission) | Người mua chọn loại vé và số lượng; sức chứa (`ga_capacity`, 1–100.000) thành một `inventory_pool` kind `GA`. Không cần sơ đồ | DOC-24 |
| publish | xuất bản | Chuyển event `DRAFT → PUBLISHED`; trong cùng một transaction tạo `inventory_pool` và `inventory_unit` từ loại vé và sơ đồ | DR-27, DOC-20 |
| sale window | khung mở bán | Khoảng `sale_starts_at` … `sale_ends_at` của event; chỉ trong khung này `reservation` mới được tạo. `sale_ends_at` không muộn hơn `starts_at` | DR-25, DR-41 |
| displayStatus | trạng thái hiển thị của sự kiện | Giá trị suy ra ở server, không lưu: `DRAFT`, `UPCOMING`, `ON_SALE`, `SOLD_OUT`, `SALE_CLOSED`, `PAUSED`, `ENDED`, `CANCELLED`. Chỉ là gợi ý cho giao diện; kết quả thật là kết quả của lệnh giữ vé | DR-24, DOC-20, DOC-40 |
| pause / resume | tạm dừng / mở lại bán | `PUBLISHED ↔ PAUSED`. Chặn lệnh giữ vé mới (`EVENT_NOT_ON_SALE`); reservation đang `ACTIVE` vẫn thanh toán được | DR-24 |
| close sale | đóng bán sớm | `POST …/close-sale` đặt `sale_ends_at = now()`; event thành `SALE_CLOSED`, không mở lại được. Không có trạng thái lưu mới | DR-24 |
| cancel event | hủy sự kiện | Chuyển `CANCELLED` từ `PUBLISHED`/`PAUSED`, kể cả khi đã có đơn `PAID`: mọi đơn `PAID` sang `REFUND_PENDING` (lý do `EVENT_CANCELLED`), vé `VOID` | DR-28, DOC-20 |
| high demand event | sự kiện nhu cầu cao | Event có `high_demand = true`: thêm phòng chờ trước giờ mở bán (từ `prequeue_opens`) và xáo ngẫu nhiên lúc mở bán; mất Redis thì lệnh giữ vé trả 503 `OVERLOADED` | DR-57, DR-56 |
| ticket code prefix | tiền tố mã vé | Hai chữ cái in hoa (`^[A-Z]{2}$`) do người tổ chức nhập khi event còn `DRAFT`, mặc định `TK` | DR-52 |
| event timezone | múi giờ sự kiện | Tên IANA trong `event.timezone`; giờ hiển thị theo múi giờ này chứ không theo máy người xem. Khóa sau khi xuất bản | DR-12, DOC-31 |
| rowVersion | phiên bản dòng | Cột `event.row_version` dùng cho khóa lạc quan của `PATCH`; lệch → 409 `STALE_EVENT_VERSION` | DR-70, DR-64 |
| platform currency | tiền tệ nền tảng | Chỉ VND, gán cứng; số tiền là số nguyên đồng, không có phần lẻ | DR-13 |

## 2. Sơ đồ chỗ ngồi

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| seat map | sơ đồ chỗ ngồi | Tài liệu JSON mô tả hàng ghế, zone, trang trí của một sự kiện; bảng `seat_map`. Mỗi event có tối đa một sơ đồ (`seat_map.event_id UNIQUE`) | DR-31, DOC-16 |
| draft | bản nháp | Cột `seat_map.draft`: tài liệu đang sửa, tự lưu sau 2 giây, kèm `draft_revision` | DR-36, DOC-22 |
| revision | revision (số lần lưu nháp) | `seat_map.draft_revision`; `PUT …/draft` gửi `revision` đã biết, lệch → 409 `REVISION_CONFLICT` | DR-36 |
| seat map version | phiên bản sơ đồ | Bản đã xuất bản, bất biến (`seat_map_version`, trigger `forbid_update`), có `version_no` và `checksum`. Event trỏ tới một phiên bản qua `seat_map_version_id` | DR-16, DOC-23 |
| map clone | nhân bản sơ đồ | Cách dùng lại sơ đồ của sự kiện khác: sao chép toàn bộ tài liệu, sinh lại mọi ID, ánh xạ loại vé theo tên (không phân biệt hoa thường). Hai sơ đồ không còn liên hệ, chỉ lưu `cloned_from_seat_map_id` | DR-31, DOC-23 |
| checksum | checksum | SHA-256 của tài liệu chuẩn hóa theo RFC 8785 (JCS), tính ở server lúc xuất bản; client không gửi | DR-32 |
| map lock | khóa sơ đồ | Từ `sale_starts_at` sơ đồ không sửa, không xuất bản được (409 `MAP_LOCKED_AFTER_SALE`), bất kể đã có ai mua hay chưa | DR-37, FR-15 |
| section | khu (section) | Nhóm logic của các hàng ghế (ví dụ "Khán đài A"); không tạo kho vé. Hàng không có section vào section mặc định "Main" | DR-32, DOC-16 |
| row | hàng ghế | Đối tượng tham số: một `path`, số ghế, quy tắc đánh số, loại vé mặc định. Vị trí từng ghế tính ra từ đây | DOC-16, DOC-21 |
| seat | ghế | Một điểm trên đường của row; có UUID ổn định, `number`, `x`, `y`, `angle`, cờ `flags`. Tạo một unit SEAT (trừ ghế `blocked`) | DR-32, DR-40 |
| zone | khu vực | Shape (`rect`, `ellipse`, `polygon`) kèm tên, sức chứa do người tổ chức nhập, loại vé. Tạo một pool ZONE | DR-32, DOC-21 |
| decoration | trang trí | Sân khấu, lối vào, nhãn chữ, ảnh; không tạo kho vé | DR-32 |
| path | đường (path) | Hình học của một row: `line`, `arc` (qua 3 điểm), `polyline` (2–200 điểm), `bezier` (bậc ba) | DR-32, DR-34 |
| seat diameter | đường kính ghế | `canvas.seatDiameter` 10–60, mặc định 20 đơn vị | DR-32 |
| min spacing | khoảng cách ghế tối thiểu | `canvas.minSpacing` ≥ `seatDiameter`, mặc định 24; khoảng cách hai tâm ghế liền nhau là `L/(N−1)` phải ≥ mức này | DR-32, DR-34 |
| numbering scheme | kiểu đánh số | `sequential` (liên tục) hoặc `odd-even-center` (lẻ chẵn tách hai phía từ giữa), cùng `start` và `direction` | DR-33 |
| accessible seat | ghế cho xe lăn | Cờ `accessible` của seat; vẫn bán bình thường | DR-32 |
| blocked seat | ghế chặn | Cờ `blocked`: không bán, không có unit; không tính vào sức chứa | DR-32, DR-40 |
| seat_index | thứ tự ghế | Số thứ tự ghế trong tài liệu (duyệt `rows` rồi `seats`), lưu ở `inventory_unit.seat_index`; bit thứ i của bitmap tình trạng ứng với `seat_index = i` | DR-40, DR-62 |
| canvas (khung vẽ) | khung vẽ | Vùng tọa độ thế giới của sơ đồ, 500–20.000 mỗi cạnh, mặc định 4000 × 3000; khác artboard của canvas thiết kế | DR-32 |
| map-core | map-core | Thư mục `frontend/src/map-core`: mô hình tài liệu, hình học, validate, renderer dùng chung cho editor và trình xem | DR-01, DOC-21 |
| map validation issue | vấn đề của sơ đồ | Kết quả validate: mã (`SEAT_OVERLAP`…), mức `error`/`warning`, `objectIds`, `params`. Server là kết quả cuối cùng | DR-35, DOC-21 |

## 3. Kho vé và giữ vé

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| inventory pool | pool kho vé | Định nghĩa một zone hoặc một loại vé GA và sức chứa của nó (`inventory_pool`, kind `ZONE`/`GA`); không có cột bộ đếm | DR-17, DOC-24 |
| inventory unit | unit kho vé | Một dòng `inventory_unit` cho mỗi vé bán được: `seat_id` với ghế, `pool_id` với zone và GA (unit vô danh, thay thế được cho nhau). Số vé còn lại là số unit `AVAILABLE` | DR-17, ADR-0003 |
| unit status | trạng thái unit | `AVAILABLE`, `HELD`, `SOLD`, `REMOVED`. `REMOVED` là unit bị bỏ do xóa ghế/zone hoặc giảm sức chứa; không tính vào sức chứa | DR-17, DOC-14 |
| claim | claim | Khóa và đổi `AVAILABLE → HELD` các unit cần giữ bằng một câu `UPDATE … FROM (SELECT … FOR UPDATE SKIP LOCKED)`; số dòng phải bằng số yêu cầu, thiếu thì rollback toàn bộ | DR-41, DOC-24 |
| SKIP LOCKED | SKIP LOCKED | Tùy chọn của `SELECT … FOR UPDATE`: bỏ qua dòng đang bị khóa thay vì chờ. Nhờ đó không có deadlock và người thua trả về trong vài mili giây | ADR-0003 |
| hot row | hot row | Một dòng bị mọi request tranh lock (ví dụ bộ đếm của pool). Thiết kế một dòng mỗi vé loại bỏ hot row | SDD gốc 10.3, DR-76 |
| reservation | reservation (lượt giữ vé) | Một lần giữ vé của một người cho một event: bảng `reservation` kèm `reservation_item`. Mỗi người có tối đa một reservation `ACTIVE` hoặc `EXPIRING` mỗi event | DR-18, DR-41 |
| reservation item | dòng giữ vé | Một ghế, hoặc một pool kèm số lượng, kèm tên loại vé, giá đơn vị và nhãn vị trí chụp lúc giữ (`reservation_item`); bản chụp giá duy nhất | DR-20 |
| hold duration | thời hạn giữ vé | `reservation.hold-duration` = `PT10M`, một giá trị cho toàn nền tảng | DR-42 |
| reservation status | trạng thái reservation | `ACTIVE`, `EXPIRING`, `CONFIRMED`, `EXPIRED`, `CANCELLED`. `ACTIVE` rời đi đúng một lần | DR-18, DOC-14 |
| EXPIRING | EXPIRING | Trạng thái trung gian: unit vẫn `HELD`, job đang hủy PaymentIntent. Dòng reservation là trọng tài giữa xác nhận và trả vé; lease 30 giây | ADR-0004, DR-42 |
| release | trả vé | Thủ tục đưa unit `HELD → AVAILABLE`, reservation `EXPIRING → EXPIRED/CANCELLED`, order `→ EXPIRED/CANCELLED`; dùng chung cho job và đường nhanh | DR-43, DOC-24 |
| fast cancel path | đường nhanh hủy giữ vé | `DELETE /reservations/{id}` chạy luôn thủ tục trả vé trong request; Stripe lỗi thì trả 202 và job hoàn tất sau | DR-43 |
| release job | job trả vé | Job `fixedDelay` 5 giây, lô 200, `FOR UPDATE SKIP LOCKED`, lease 30 giây; đưa reservation quá hạn qua `EXPIRING` rồi trả vé | DR-42, DOC-24 |
| lease | lease | Thời hạn một worker được xử lý dòng `EXPIRING` hoặc `outbox`; hết lease thì dòng được lấy lại | DR-42, DR-53 |
| max units per hold | giới hạn unit mỗi lệnh giữ | `reservation.max-units-per-hold` = 50: giới hạn kỹ thuật, không phải tính năng bán. Không có giới hạn theo đơn hay theo người | DR-41 |
| InventoryClaimer | InventoryClaimer | Interface claim với ba bản: `SkipLockedClaimer` (mặc định), `CounterClaimer`, `NaiveClaimer`; chọn bằng `inventory.strategy` | DR-76 |
| OrganizerEventView | dạng xem sự kiện của người tổ chức | DTO của `GET /organizer/events/{id}` gồm giờ UTC và `…Local` | DOC-37 |
| MapDocument | tài liệu sơ đồ (DTO) | Tài liệu JSON của sơ đồ ở request và response của nhóm `/organizer/maps` | DR-32, DOC-37 |
| publish-checks | kiểm tra trước xuất bản | `GET …/publish-checks` trả danh sách điều kiện xuất bản (có/không đạt) | DOC-37, DOC-59 |

## 4. Đơn hàng, thanh toán và vé

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| order | đơn hàng | Bảng `orders`, một đơn cho mỗi reservation (`reservation_id UNIQUE`); `amount` = Σ `quantity × unit_price`, tính ở server trong transaction giữ vé và không đổi sau đó | DR-18, DR-20 |
| order status | trạng thái đơn | `PENDING_PAYMENT`, `PAID`, `EXPIRED`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED`. Chỉ đổi sang `PAID` khi nhận webhook đã xác minh chữ ký (hoặc đơn 0 đồng) | DR-44, DOC-14 |
| refund reason | lý do hoàn tiền | `LATE_PAYMENT`, `AMOUNT_MISMATCH`, `EVENT_CANCELLED`; có giá trị khi và chỉ khi đơn ở `REFUND_PENDING` hoặc `REFUNDED` | DR-44 |
| REFUND_PENDING | chờ hoàn tiền | Đã nhận tiền, phải hoàn thủ công; không phát hành vé (hoặc vé `VOID`). Gửi email `refund-pending` | DR-44, DOC-65 |
| REFUNDED | đã hoàn tiền | Người vận hành đã hoàn trên Stripe Dashboard và ghi `refund_reference`, `refunded_at` | DR-44 |
| manual refund | hoàn tiền thủ công | Quy trình RB-01: hoàn trên Stripe Dashboard rồi `UPDATE orders … WHERE status = 'REFUND_PENDING'`. Hoàn tiền tự động để sau | RB-01, DOC-65 |
| ticket | vé | Bảng `ticket`, một dòng cho mỗi unit của đơn `PAID`; chép tên loại vé, giá, nhãn từ reservation item để tự đứng được khi in | DR-18, DOC-27 |
| ticket status | trạng thái vé | `ISSUED`, `VOID` (khi event bị hủy). `ticket.unit_id` duy nhất trong các vé `ISSUED` | DR-18, DR-28 |
| ticket code | mã vé | `PP-XXXX-XXXX`: `PP` là tiền tố mã vé, `XXXX-XXXX` là 8 ký tự Crockford Base32 (40 bit `SecureRandom`); duy nhất | DR-52 |
| PaymentIntent | PaymentIntent | Đối tượng thanh toán của Stripe; chỉ thẻ, `capture_method = automatic`, `currency = vnd`, `metadata` có `order_id`, `reservation_id`, `event_id`. Idempotent theo `order_id` | DR-47, ADR-0005 |
| client secret | client secret | Giá trị server trả cho trình duyệt để Payment Element xác nhận PaymentIntent; không lưu vào database | DR-45, DR-47 |
| Payment Element | Payment Element | Thành phần giao diện của Stripe.js nhúng trong trang thanh toán; dữ liệu thẻ đi thẳng tới Stripe | NFR-07, DR-50 |
| webhook | webhook | `POST /api/v1/webhooks/stripe`: đọc body thô, xác minh `Stripe-Signature` với dung sai 300 giây, loại trùng bằng `stripe_event` | DR-48, DOC-09 |
| Stripe event | event của Stripe | Một thông điệp webhook (`evt_…`); bảng `stripe_event` lưu để loại trùng và ghi `outcome` | DR-19, DR-48 |
| late payment | thanh toán đến trễ | `payment_intent.succeeded` đến khi reservation đã `EXPIRED`/`CANCELLED`: không phát hành vé, đơn sang `REFUND_PENDING` lý do `LATE_PAYMENT`; luồng hiếm, khác 0 là dấu hiệu cần điều tra | DR-44, UC-15 |
| reconciliation | đối chiếu | `PaymentReconcileJob` mỗi 60 giây hỏi Stripe các PaymentIntent của đơn quá 15 phút hoặc reservation `EXPIRING` quá 2 phút và gọi đúng handler của webhook | DR-49, UC-16 |
| idempotency key | idempotency key | Header `Idempotency-Key` (UUID); dòng `(user_id, idem_key)` chèn đầu transaction, response lưu trước commit; chỉ lưu 2xx; dọn sau 24 giờ | DR-45, DOC-25 |
| outbox | outbox | Bảng `outbox`: email phát sinh từ nghiệp vụ (`EMAIL_TICKETS`, `EMAIL_EVENT_CHANGED`, `EMAIL_REFUND_PENDING`) ghi cùng transaction với thay đổi, `OutboxRelay` gửi ít nhất một lần. Không chứa magic link và không chứa lệnh hủy PaymentIntent | DR-53, DR-21, ADR-0006 |
| PaymentGateway | cổng thanh toán | Port trong module `payment` với `createIntent`, `retrieveIntent`, `cancelIntent`; adapter `StripePaymentGateway` và `FakePaymentGateway` | DR-51, ADR-0017 |
| fake payment gateway | cổng thanh toán giả | Adapter profile `fake-payments`: mô phỏng trạng thái, lỗi hủy, ký webhook HMAC giống Stripe, có endpoint điều khiển; dùng cho test và thực nghiệm. `PAYMENTS_MODE=fake` là mặc định | DR-51, DR-72 |
| payment.min-amount | mức thanh toán tối thiểu | Giá vé khác 0 phải ≥ mức này (mặc định 20.000 VND, chỉnh theo S-02) | DR-13 |

## 5. Xác thực

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| magic link | magic link | Đường dẫn `/auth/callback?token=…` gửi qua email; dùng một lần, hết hạn sau 15 phút. Trang callback chỉ tiêu thụ token bằng `POST /auth/verify`, nên bộ quét link của hộp thư không làm token hết hiệu lực | DR-21, ADR-0007 |
| login token | token đăng nhập | 32 byte `SecureRandom`, base64url 43 ký tự; chỉ lưu SHA-256 (`login_token.token_hash`); token thô chỉ nằm trong bộ nhớ của request | DR-21, NFR-07 |
| supersede | thay thế token | Yêu cầu link mới đặt `superseded_at` cho token chưa dùng của cùng email; chỉ token mới nhất hợp lệ | DR-21 |
| throttled magic link | gửi quá nhiều | Vượt 3 link mỗi email/15 phút hoặc 10 mỗi IP/giờ: vẫn trả 202 nhưng không gửi, kèm header `X-Magic-Link-Throttled: 1` | DR-21 |
| return_to | đường dẫn quay lại | Đường dẫn tương đối của cùng origin, bắt đầu `/`, không bắt đầu `//` hoặc `/\`, ≤ 512 ký tự; sai thì dùng `/`. Trả về dưới tên `returnTo` | DR-21 |
| session | session | Bảng `session`; ID thô 32 byte chỉ nằm trong cookie `tb_session`, database lưu SHA-256. Hết hạn khi `last_seen_at` quá 30 ngày hoặc đã `revoked_at` | DR-22 |
| session cache | cache session | Caffeine trong tiến trình, TTL 60 giây, tối đa 200.000 mục; đăng xuất xóa mục | DR-22 |
| CSRF token | CSRF token | `session.csrf_token` (synchronizer token), trả trong `POST /auth/verify` và `GET /me`; client gửi lại trong header `X-CSRF-Token` cho mọi `POST/PUT/PATCH/DELETE` | DR-22, DOC-32 |
| role | vai trò | `BUYER` (mọi tài khoản), `ORGANIZER` (có hồ sơ tổ chức). Vai trò `ADMIN` để sau | DR-23 |
| ownership check | kiểm tra sở hữu | Mọi truy vấn `/organizer/**` có `AND organizer_id = :sessionOrg`; tài nguyên của tổ chức khác trả 404 `NOT_FOUND` chứ không phải 403 | DR-23 |
| LocaleResolver | bộ chọn locale | Thành phần ở `common` chọn locale của request theo thứ tự cookie `tb_lang` → `app_user.locale` → `Accept-Language` → `vi`; dùng cho email và Problem Details | DR-10, DOC-31 |
| SUPPORTED_LOCALES | tập locale hỗ trợ | Hằng số ở `common` (`vi`, `en`); không phải khóa cấu hình | DR-10, DOC-31 |

## 6. Kiểm soát tiếp nhận và chịu tải

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| admission control | kiểm soát tiếp nhận | Sáu lớp giảm tải trước database: rate limit nginx, phòng chờ, token bucket theo người dùng, cờ hết vé, bulkhead, database. Redis chỉ giảm tải, không quyết định đúng sai | SDD gốc 10.1, DOC-28 |
| waiting room | phòng chờ | Màn và cơ chế hàng đợi theo sự kiện, luôn chạy cho mọi event trong khung mở bán; khi còn chỗ thì `ADMITTED` ngay (trong suốt) | DR-57, DOC-45 |
| pre-queue | nhóm chờ trước | Tập `prequeue:{event}` (từ `prequeue_opens`, 30 phút trước giờ mở bán) của event `high_demand`; chưa có thứ tự. Đúng giờ mở bán được xáo ngẫu nhiên thành `queue` | DR-57, DR-59 |
| queue | hàng đợi | Sorted set `queue:{event}`; điểm là số ngẫu nhiên [0,1) cho nhóm được xáo, `1 + INCR queue-seq` cho người đến sau | DR-59, DOC-17 |
| admitted set | tập đã được vào | Sorted set `admitted:{event}`; điểm là thời điểm hết hạn của lượt vào. Số phần tử không vượt `max_active` | DR-57, DR-59 |
| admission pass | lượt vào | Quyền ở trong khu đặt vé: một phần tử trong `admitted:{event}` gắn với `userId` của session; không chuyển cho người khác | DR-57, DR-58 |
| admission token | token vào cửa | Khái niệm của SDD gốc 10.2 (token ký, header `X-Admission-Token`); **bị bỏ** — lượt vào được kiểm bằng `ZSCORE admitted:{eventId} <userId>` theo session | DR-58 |
| pass TTL | thời hạn lượt vào | `admission.pass-ttl` = 5 phút để chọn chỗ; giữ vé xong thì kéo tới `expires_at` của reservation; reservation đóng mà chưa trả tiền còn thêm 2 phút (`admission.repick-grace`) | DR-57 |
| admit rate | nhịp cấp lượt | `admission.admit-rate` = 50 người/giây, cố định, không tự điều chỉnh; EXP-05 tìm điểm gãy | DR-57, DR-60 |
| max active | số người tối đa trong khu đặt vé | `admission.max-active` = 500 | DR-57 |
| idle timeout | thời gian rảnh | Ngừng hỏi vị trí quá 2 phút thì mất chỗ trong hàng (`seen:{event}`) | DR-57, DR-59 |
| retryAfter | nhịp hỏi lại | `retryAfterSeconds` server trả cho client: vị trí ≤ 200 → 3, ≤ 2.000 → 10, còn lại và `PRE_QUEUE` → 30 | DR-59 |
| queue status | trạng thái hàng đợi | `PRE_QUEUE`, `WAITING`, `ADMITTED`, `PAUSED` ("Tạm hết vé"), `SOLD_OUT`, `NOT_IN_QUEUE`; "mất kết nối" chỉ có ở client | DR-57 |
| backpressure | giảm áp | Ở dự án này chỉ còn bulkhead trả 503 khi database đầy; vòng điều khiển tự động `admit_rate` (AIMD) **bị bỏ** | DR-60, DR-61 |
| sold-out flag | cờ hết vé | `soldout:pool:{poolId}`, TTL 30 giây, đặt khi lệnh giữ nhận `INSUFFICIENT_CAPACITY` và đếm unit `AVAILABLE` = 0; xóa sau khi trả unit. Chỉ là gợi ý | DR-46 |
| bulkhead | bulkhead | Giới hạn 24 lệnh giữ và hủy giữ đồng thời (nhỏ hơn pool 40); hết permit → 503 `OVERLOADED` với `Retry-After` 1–3 giây, không chờ | DR-61 |
| token bucket | token bucket | `token_bucket.lua` theo người dùng: `rl:hold:{userId}` (5, nạp 1/2 giây), `rl:pi:{userId}` (5, 1/5 giây), `rl:queue:{userId}` (10, 1/giây). Mất Redis thì bỏ qua, không có limiter dự phòng | DR-56 |
| availability snapshot | snapshot tình trạng chỗ | Kết quả `GET /events/{id}/availability`: bitmap base64 `held`/`sold` theo `seat_index`, số `available` theo pool và loại vé; cache Caffeine trong tiến trình, hết hạn 2 giây | DR-62, ADR-0013 |
| display status (of a seat) | trạng thái hiển thị của chỗ | Bốn trạng thái ghế trong trình xem: còn trống (màu theo loại vé), đang được giữ, đã bán, đang chọn; mã hóa không chỉ bằng màu | DR-62, DOC-39 |
| server time offset | độ lệch giờ server | `X-Server-Time` (epoch ms) trên mọi response; client lấy trung vị 5 mẫu để đồng hồ đếm ngược không tin đồng hồ máy | DR-66 |

## 7. Kiến trúc và hạ tầng

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| modular monolith | monolith chia module | Một ứng dụng API, job nền chạy cùng tiến trình, PostgreSQL là nguồn chuẩn duy nhất; module chỉ gọi nhau qua package gốc | ADR-0002, DR-06 |
| module | module | `io.ticket.<module>`: `auth`, `event`, `map`, `inventory`, `reservation`, `order`, `payment`, `ticket`, `admission`, `notification`, `invariant`, `media` và `common` (dùng chung). Mỗi bảng và họ key Redis có đúng một module sở hữu | DR-05, DR-06, DOC-07 |
| `…Api` | interface công khai của module | Interface ở package gốc của module (ví dụ `InventoryApi`), cùng DTO và event; module khác chỉ dùng package gốc | DR-06 |
| layer | layer | Trong module: điểm vào (`controller`, `job`, `listener`) → `service` → `repository`/`client`, cùng `entity`, `dto`. `@Transactional` chỉ ở `service` | DR-06, DOC-12 |
| aggregate | aggregate | Entity Spring Data JDBC có `save()` (`AppUser`, `Event`, `Reservation`…); `inventory_unit` và `inventory_pool` không phải aggregate. Không đổi `status` bằng `save()` | DR-05, ADR-0012 |
| conditional write | câu ghi có điều kiện | Mọi chuyển trạng thái là `UPDATE … WHERE … AND status = :from` kiểm số dòng trả về; code không đọc rồi mới quyết định ghi | DR-05, SDD gốc 4.2 |
| Spring Modulith | Spring Modulith | Thư viện kiểm ranh giới module (`ApplicationModules.verify()`), kèm ArchUnit cho layer và sở hữu bảng | DR-05 |
| UUIDv7 | UUIDv7 | Kiểu khóa chính `uuid` mặc định `uuidv7()` của PostgreSQL 18; Java sinh trước khi cần biết ID. ID trong tài liệu sơ đồ do client sinh | DR-11, ADR-0018 |
| Problem Details | Problem Details | Lỗi `application/problem+json` (RFC 9457) kèm `code`, `requestId`; `title`/`detail` theo `Accept-Language` | DR-63, DOC-36 |
| cursor pagination | phân trang cursor | `?limit=` (mặc định 20, tối đa 100) và `?cursor=`; response `{ items, nextCursor }` | DR-63 |
| object storage | object storage | Kho tương thích S3 cho ảnh (SeaweedFS trong compose); bucket private, API phục vụ `GET /media/{id}` qua cache nginx | DR-38, ADR-0015 |
| media | media | Bảng `media` chỉ giữ metadata ảnh (`EVENT_IMAGE`, `FLOOR_PLAN`); byte nằm ở object storage; bất biến | DR-38 |
| profile | profile cấu hình | `dev`, `fake-payments`, `experiment`, `invariants`, `obs`, `stripe` (Spring) và profile compose `stripe`, `obs` | DR-72, DOC-34 |
| Mailpit | Mailpit | Hộp thư SMTP của môi trường dev; E2E đọc magic link qua API của nó | DR-72, DR-77 |
| stripe-cli | stripe-cli | Container chuyển tiếp webhook của Stripe test mode về `api` (profile `stripe`) | DR-72 |
| statement_timeout | statement_timeout | `SET LOCAL statement_timeout = '2s'` và `lock_timeout = '1s'` trong transaction giữ vé | DR-41, DR-61 |
| Flyway migration | migration | `V<yyyymmddHHmm>__<snake_case>.sql`; không sửa migration đã merge; thư mục riêng `db/migration-fake`, `db/migration-experiment` | DR-07, DR-51, DR-76 |
| ErrorCode | mã lỗi (enum) | Enum duy nhất chứa 40 mã lỗi API, mỗi giá trị có `status`, `errorClass` và key i18n | DOC-35 |
| error class | lớp lỗi | Ba lớp: `BUSINESS` (nghiệp vụ), `TRANSIENT` (hạ tầng tạm thời), `DEFECT` (lập trình); quyết định mức log và retry | DOC-35 |
| rule | quy tắc kiểm tra | Giá trị chữ thường `snake_case` trong `errors[]` của `VALIDATION_FAILED` (ví dụ `invalid_timezone`); khác `code` luôn chữ hoa | DR-83, DOC-35 |
| batch-budget | ngân sách một lượt relay | Thời gian tối đa (40 giây) một lượt `OutboxRelay` được xử lý dòng; dòng chưa kịp xử lý được trả lại | DR-104, DOC-27 |
| TicketCodeGenerator | bộ sinh mã vé | Sinh mã vé theo DR-52 (bảng chữ Crockford Base32), kiểm trùng bằng `SELECT` trước khi chèn | DR-103, DOC-27 |
| NotificationApi | giao diện thông báo | Giao diện của module `notification` (`enqueue`, `enqueueAll`); ghi `outbox` trong transaction của bên gọi | DOC-27 |
| OriginCheckFilter | bộ lọc Origin | Request ghi có `Origin` khác `APP_BASE_URL` bị từ chối 403 `CSRF_TOKEN_INVALID` | DR-117, DOC-32 |
| ConfigurationGuard | bộ kiểm tra cấu hình | Bean kiểm các ràng buộc giữa khóa lúc khởi động và thoát nếu sai | DR-147, DOC-34 |
| e2e.yml | workflow E2E | Workflow CI riêng chạy Playwright; bắt buộc với PR `dev → main` | DR-128, DOC-63 |
| breaking-ok | nhãn cho phép thay đổi phá vỡ | Nhãn PR bỏ qua kiểm tra `oasdiff breaking` | DR-129, DOC-63 |

## 8. Kiểm thử, thực nghiệm và quy trình tài liệu

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| NEVER OVERSELL | không bao giờ bán vượt | Yêu cầu đúng đắn đứng trên mọi yêu cầu khác: mỗi ghế tối đa một chủ, mỗi pool không bao giờ âm, mỗi đơn thu tiền đúng một lần. Ép ở database | NFR-01, DOC-01 |
| invariant check | kiểm tra bất biến | `InvariantChecker`: các truy vấn `INV-xx` chạy mỗi 5 phút và bằng `make invariants` (JSON, thoát 0 khi sạch, 1 khi có sai lệch); chỉ báo cáo, không tự sửa | DR-73, DOC-30 |
| naive implementation | bản cài đặt ngây thơ | `NaiveClaimer`: đọc rồi ghi không điều kiện trạng thái; chỉ chạy được với profile `experiment`, để chứng minh lỗi bán vượt xuất hiện khi thiếu cơ chế | DR-76 |
| counter implementation | bản bộ đếm | `CounterClaimer` (phương án A của SDD gốc 10.3): một dòng bộ đếm mỗi pool, bảng `inventory_pool_counter`; baseline của EXP-10 | DR-76, DOC-80 |
| virtual user | người dùng ảo | Một người dùng mô phỏng trong k6; một VU quản lý nhóm người dùng ảo và tôn trọng `retryAfterSeconds`. "Đồng thời" nghĩa là có mặt trong cùng cửa sổ mở bán | DR-75 |
| EXP | thực nghiệm | `EXP-01…10`; mỗi thực nghiệm kết thúc bằng `make invariants` sạch, chạy 3 lần, báo trung vị và min–max | DOC-70 |
| spike | spike | Thực nghiệm ngắn ≤ 1–2 ngày `S-01…06` ở Phase 0 để chốt DR cần số đo | Master plan §5 |
| DR | quyết định (DR) | Mục `DR-xx` của sổ quyết định; mục ở mức kiến trúc thành ADR | DOC-13 |
| ADR | quyết định kiến trúc (ADR) | Tệp `04-adr/NNNN-*.md`, MADR rút gọn; không sửa nội dung, chỉ thay bằng ADR mới | DOC-13 |
| FL | luồng chi tiết | `FL-xx`: một luồng của UC qua từng tầng (màn hình → endpoint → SQL → giao diện), ở `06-design/flows/` | DOC-82 |
| gate | gate tài liệu | Phase chỉ bắt đầu khi mọi tài liệu và ADR có Gate = phase đó ở trạng thái `Approved` | Master plan §0.3 |
| runbook | runbook | Tài liệu `RB-xx` làm theo từng bước để xử lý một sự cố hoặc thao tác vận hành | DOC-64 |
| seed | dữ liệu mẫu | `make seed`: tài khoản `organizer@demo.test`, `buyer1…3@demo.test` và ba sự kiện mẫu của canvas | DR-72, DR-78 |
| test level codes | mã mức test | Tám mã `U`, `P`, `A`, `I`, `C`, `K`, `E`, `L` dùng ở ma trận kiểm thử | DR-149, DOC-69 |
| InvariantsExtension | tiện ích bất biến | Extension JUnit chạy `InvariantChecker` sau mỗi test tích hợp; `@ExpectViolation` đánh dấu test cố ý vi phạm | DOC-69 |
| AbstractIT | lớp nền test tích hợp | Lớp cơ sở dùng Testcontainers PostgreSQL 18, Redis và Mailpit | DOC-69 |
| Fixtures | bộ dữ liệu test | Bộ dựng dữ liệu test dùng chung cho mọi module | DOC-69 |
| UXP-xx | nguyên tắc UX | Mã nguyên tắc UX đánh số `UXP-01…15` | DOC-38 |

## 9. Giao diện và đa ngôn ngữ

| Thuật ngữ | Tiếng Việt | Định nghĩa | Liên quan |
| --- | --- | --- | --- |
| locale | ngôn ngữ giao diện | `vi` (mặc định và dự phòng) hoặc `en`. Thứ tự chọn: `app_user.locale` → cookie `tb_lang` → `Accept-Language` → `vi` | DR-10, ADR-0016 |
| i18n key | key chuỗi giao diện | Key tiếng Anh theo nghĩa (`checkout.hold.expired.title`), namespace theo feature; `pnpm i18n:check` bắt đủ hai locale | DR-10, DOC-31 |
| microcopy | microcopy | Mọi chuỗi giao diện và email, lưu dạng key · `vi` · `en` | DOC-40 |
| "Vé giấy" design system | design system "Vé giấy" | Bộ token v0.1 của canvas (màu `--paper`, `--stamp`…; chữ Barlow Condensed, Be Vietnam Pro, IBM Plex Mono); không có dark mode | DR-68, DOC-39 |
| canvas thiết kế | canvas thiết kế | Artifact "Ticket — Design system & luồng mua vé" (50 artboard), nguồn của màn hình và microcopy `vi` | DR-68, DOC-41 |
| artboard | artboard | Một khung màn hình trong canvas thiết kế (ví dụ `05` Chọn chỗ, `M05` bản 390 px) | DOC-41 |
| screen spec | đặc tả màn hình | Tệp `08-ux-ui/screens/*.md` theo mẫu A.5 | DOC-41 |
| session-less selection | lựa chọn giữ qua đăng nhập | `localStorage["tb.selection.<eventId>"]` giữ ghế, khu vực, GA đã chọn tới 30 phút để khôi phục sau khi đăng nhập | DR-67 |
| HoldTimer | đồng hồ giữ vé | Component đếm ngược theo `X-Server-Time` và `expiresAt` (DR-66) | DOC-39 |
| Skeleton | khung chờ | Component hiển thị trạng thái đang tải | DOC-39, DOC-40 |
| SeatsRoute | phần tử route chọn chỗ | Phần tử router (không phải màn hình) chọn `SeatPicker` hoặc `QuantityPicker` ở `/events/:eventId/seats` | DR-141, DOC-41 |
| artboard | artboard | Một bố cục màn hình trong canvas thiết kế; tiền tố `M` là bản 390 px | DOC-41 |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: không có DR mới.
