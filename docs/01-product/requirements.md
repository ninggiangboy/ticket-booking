# Yêu cầu

> Trạng thái: **Approved** · Cập nhật: 2026-10-07 · DOC-03
> Phụ thuộc: SDD gốc §3.3–§3.4, [DOC-01](vision-and-scope.md), [DOC-02](personas-and-journeys.md), [DOC-06](../02-glossary.md), [Sổ quyết định](../00-decision-register.md) (DR-10…13, 21…28, 31…38, 41…49, 52, 55…66, 70, 71, 73, 74, 75)
> Người dùng chính: P0-11; [DOC-04](use-cases.md), [DOC-05](feature-catalog.md); mọi tài liệu thiết kế; mọi task `Pn-xx` khi viết test nghiệm thu

Tài liệu liệt kê yêu cầu chức năng FR-01…21, yêu cầu phi chức năng NFR-01…08 và tiêu chí nghiệm thu có số. Cách làm (thuật toán, SQL, cấu hình) nằm ở tài liệu thiết kế ở ma trận cuối (mục 4); luồng người dùng ở [DOC-04](use-cases.md).

## 0. Quy ước

- **Mã.** FR-01…12 và NFR-01…08 giữ mã của SDD gốc; FR-13…21 là yêu cầu bổ sung, đánh dấu "(bổ sung)". Yêu cầu con là `FR-xx.y`. Một số đã nhận mã không bao giờ đổi hoặc tái dùng (master plan §0.4).
- **Ưu tiên (MoSCoW).** M = Must (không có thì dự án thất bại), S = Should, C = Could, W = Won't ở giai đoạn này. Mọi yêu cầu bắt buộc của M2/M3 đều là M.
- **Tiêu chí nghiệm thu** viết **G** (Given: điều kiện đầu), **W** (When: hành động), **T** (Then: kết quả quan sát được), kèm con số. Tiền là VND (số nguyên đồng). Giờ trong ví dụ là UTC.
- **Kiểm chứng.** Tên test dùng tiền tố của tài liệu thiết kế (`AU-`, `EV-`, `INVT-`, `IDEM-`, `PAY-`…, danh mục ở DOC-69), `EXP-xx`, hoặc E2E.
- **Số "planned".** Mọi con số tải và hiệu năng là mục tiêu thiết kế; số đo thật ghi ở tài liệu thực nghiệm (DOC-01 §4).
- **Một yêu cầu bị đổi sau này** mang dòng `*Sửa {ngày} (DR-xx): …*` ngay trong mục của nó và tài liệu đích được sửa trong cùng thay đổi.
- Mã lỗi (`code`) viết tiếng Anh theo SDD gốc 12.3 và DR-64; bảng đầy đủ ở DOC-35.

## 1. Yêu cầu chức năng

### FR-01 · Đăng nhập bằng magic link

Nguồn: SDD gốc §5, DR-21, DR-22. UC-01. Thiết kế: DOC-19.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-01.1 | Xin magic link chuẩn hóa email, luôn trả 202 | **G** email ` Alice@Example.com ` **W** `POST /auth/magic-link` **T** 202; email lưu `alice@example.com`; 1 dòng `login_token` có `expires_at = now() + 15 phút` và `token_hash` 32 byte; 1 thư trong Mailpit; không cột nào chứa token thô | M | AU-01, E2E |
| FR-01.2 | Link dùng một lần; đúng một request thắng | **G** một token hợp lệ **W** 50 `POST /auth/verify` song song **T** đúng 1 response 200 kèm `Set-Cookie: tb_session`; 49 response 401 `LOGIN_LINK_INVALID` | M | AU-02 |
| FR-01.3 | Link hết hạn sau 15 phút | **G** token tạo lúc 03:00:00 **W** verify lúc 03:15:01 **T** 401 `LOGIN_LINK_INVALID`; `used_at` vẫn NULL | M | AU-03 |
| FR-01.4 | Yêu cầu link mới thay link cũ | **G** link A rồi link B cho cùng email **W** verify A **T** 401; verify B → 200; `login_token` của A có `superseded_at` | M | AU-04 |
| FR-01.5 | Giới hạn gửi 3 link/email/15 phút và 10 link/IP/giờ; vượt vẫn 202 | **G** đã 3 token của một email trong 15 phút **W** yêu cầu lần 4 **T** 202 kèm `X-Magic-Link-Throttled: 1`, không chèn token, không gửi thư; tương tự khi IP đã 10 lần trong 60 phút | M | AU-05 |
| FR-01.6 | SMTP lỗi báo lỗi rõ, không để lại outbox | **G** SMTP tắt **W** `POST /auth/magic-link` **T** 503 `EMAIL_PROVIDER_UNAVAILABLE` sau ≤ 5 giây; bảng `outbox` không có dòng mới; lần gửi vẫn tính vào giới hạn | M | AU-06 |
| FR-01.7 | Email chưa có tài khoản được tạo ở lần verify đầu, response 202 không lộ tài khoản đã có hay chưa | **G** email mới **W** xin link và verify **T** 202 giống hệt email đã có; sau verify có 1 dòng `app_user` | M | AU-07 |
| FR-01.8 | `return_to` chỉ nhận đường dẫn tương đối cùng origin | **G** `returnTo` = `/checkout/0199…`, `//evil.example`, `/\evil`, chuỗi 513 ký tự **W** verify **T** giá trị đầu giữ nguyên; ba giá trị sau thành `/` | M | AU-08 |
| FR-01.9 | Trang callback không tiêu thụ token khi tải | **G** token hợp lệ **W** `GET /auth/callback?token=…` 5 lần (bộ quét link) **T** `used_at` vẫn NULL; chỉ `POST /auth/verify` tiêu thụ | M | AU-09, E2E |
| FR-01.10 | Session cookie an toàn, hết hạn 30 ngày không hoạt động | **G** session tạo lúc t **W** request ở t + 30 ngày + 1 giây **T** 401 `UNAUTHENTICATED`; cookie `tb_session` có `HttpOnly; Secure; SameSite=Lax` (`Secure` tắt ở profile `dev`); `last_seen_at` chỉ cập nhật khi cũ hơn 1 giờ | M | AU-10 |
| FR-01.11 | CSRF cho mọi request ghi | **G** session hợp lệ **W** `POST/PUT/PATCH/DELETE` thiếu `X-CSRF-Token` **T** 403 `CSRF_TOKEN_INVALID`; ngoại lệ `POST /webhooks/stripe`, `POST /auth/magic-link`, `POST /auth/verify` | M | AU-11 |
| FR-01.12 | Đăng xuất xóa session phía server | **G** session hợp lệ **W** `POST /auth/logout` **T** `revoked_at` khác NULL; request kế tiếp trong ≤ 1 giây nhận 401 `UNAUTHENTICATED` | M | AU-12 |

### FR-02 · Quản lý sự kiện và loại vé

Nguồn: SDD gốc §6, DR-24…27, DR-30. UC-02, UC-07, UC-09. Thiết kế: DOC-20.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-02.1 | Lưu bản nháp chỉ cần `name` | **G** organizer có hồ sơ **W** `POST /organizer/events {"name":"Hòa nhạc Giao Mùa"}` **T** 201, event `DRAFT`, `timezone = Asia/Ho_Chi_Minh`, `ticket_code_prefix = TK`; `name` rỗng hoặc 121 ký tự → 422 `required`/`too_long` | M | EV-01 |
| FR-02.2 | Xuất bản yêu cầu đủ trường theo quy tắc DR-25, lỗi theo ô | **G** `startsAt` 2026-11-14T13:00Z, `saleEndsAt` 2026-11-14T14:00Z **W** publish **T** 422 `VALIDATION_FAILED` với `errors[{field:"saleEndsAt", rule:"after_event_start"}]`; `endsAt` = `startsAt` + 73 giờ → `too_long_event`; `startsAt` quá khứ → `must_be_future` | M | EV-02 |
| FR-02.3 | Khóa lạc quan khi sửa | **G** event `rowVersion = 3`, tab A và B cùng đọc **W** A rồi B `PATCH` kèm `rowVersion: 3` **T** A 200 (`rowVersion` 4); B 409 `STALE_EVENT_VERSION` | M | EV-03 |
| FR-02.4 | Múi giờ là tên IANA, khóa sau xuất bản | **G** event `DRAFT` **W** `timezone = "+07:00"` **T** 422 `invalid_timezone`; `"Asia/Tokyo"` → 200; sau publish đổi → 422 `locked_after_publish` | M | EV-04 |
| FR-02.5 | Loại vé: tối đa 5, tên duy nhất, giá hợp lệ | **G** event có 5 loại vé **W** thêm loại thứ 6 **T** 422 `TICKET_TYPE_LIMIT_REACHED`; tên "vip" khi đã có "VIP" → 422; giá 10.000 → 422 (dưới `payment.min-amount` 20.000); giá 0 hoặc 20.000 → 201; `color_index` là số nhỏ nhất chưa dùng | M | EV-05 |
| FR-02.6 | Mô hình loại vé không đổi sau xuất bản; GA có sức chứa 1–100.000 | **G** event đã `PUBLISHED` **W** đổi `model` **T** 409 `EVENT_STATE_CONFLICT`; `ga_capacity = 100001` → 422 | M | EV-06 |
| FR-02.7 | Xuất bản tạo kho vé trong một transaction | **G** event GA, pool 5.000 vé **W** `POST …/publish` **T** đúng 5.000 `inventory_unit` `AVAILABLE`; event `PUBLISHED` cùng transaction; thiếu điều kiện → 422 `PUBLISH_PRECONDITIONS_FAILED` kèm danh sách điều kiện chưa đạt | M | EV-07, INVT-xx |
| FR-02.8 | `displayStatus` tính ở server | **G** event `PUBLISHED`, `now() < sale_starts_at` **T** `UPCOMING`; trong khung và còn unit `AVAILABLE` → `ON_SALE`; trong khung, mọi unit `HELD`/`SOLD` → `SOLD_OUT`; `now() >= sale_ends_at` → `SALE_CLOSED` | M | EV-08 |
| FR-02.9 | Tạm dừng, mở lại | **G** event `ON_SALE` **W** `POST …/pause` **T** lệnh giữ vé mới 409 `EVENT_NOT_ON_SALE`; reservation đã `ACTIVE` vẫn tạo PaymentIntent và thanh toán tới hết hạn; `…/resume` → giữ vé được lại | M | EV-09 |
| FR-02.10 | Đóng bán sớm | **G** event `ON_SALE` **W** `POST …/close-sale` **T** `sale_ends_at = now()`, `displayStatus = SALE_CLOSED`, giữ vé mới 409; gọi khi chưa tới giờ mở bán hoặc đã đóng → 409 `EVENT_STATE_CONFLICT` | M | EV-10 |
| FR-02.11 | `EventLifecycleJob` chuyển `ENDED` | **G** event `PUBLISHED` có `ends_at` đã qua **W** job chạy (60 giây một lần) **T** `status = ENDED`, `ended_at` được đặt trong ≤ 60 giây | M | EV-11 |
| FR-02.12 | Đổi sức chứa pool khi đang bán | **G** pool GA capacity 100, 40 unit `HELD`+`SOLD` **W** giảm xuống 30 **T** 409 `CAPACITY_BELOW_USED` kèm `used = 40`; giảm xuống 40 → 200, 60 unit `REMOVED`; tăng lên 150 → thêm 50 unit `AVAILABLE` | M | EV-12 |
| FR-02.13 | Xóa loại vé | **G** loại vé có 1 unit `HELD` **W** xóa **T** 409 `TICKET_TYPE_IN_USE`; loại vé không có unit `HELD`/`SOLD` → `deleted_at` đặt, unit `AVAILABLE` sang `REMOVED`; bản nháp xóa thật | M | EV-13 |
| FR-02.14 | Đổi giá chỉ áp dụng cho reservation tạo sau | **G** reservation R1 giữ vé giá 100.000 **W** đổi giá thành 150.000, rồi tạo R2 **T** R1 giữ `unit_price = 100000`; R2 `150000` | M | EV-14 |
| FR-02.15 | Danh sách công khai | **G** 30 event `PUBLISHED`/`PAUSED` còn `ends_at > now()` và 5 event `ENDED`/`CANCELLED` **W** `GET /events` **T** `items` 20, sắp theo `startsAt` tăng dần, `nextCursor` khác null; trang 2 có 10; event `ENDED`/`CANCELLED` vẫn mở được bằng URL trực tiếp nhưng không có trong danh sách | M | EV-15 |
| FR-02.16 | Chỉ chủ tổ chức thao tác sự kiện của mình | **G** event của tổ chức A **W** tổ chức B `GET`/`PATCH` event đó **T** 404 `NOT_FOUND` (không lộ tồn tại) | M | EV-16, SEC-xx |

### FR-03 · Seat map editor: hàng ghế

Nguồn: SDD gốc §7.1–§7.6, DR-32…34, DR-36, DR-39. UC-08. Thiết kế: DOC-21, DOC-22.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-03.1 | Rải ghế cách đều theo độ dài cung | **G** đường tổng độ dài L = 480, N = 21 **W** rải ghế **T** 21 ghế cách nhau 24,0 theo độ dài cung (sai số ≤ 0,5), ghế đầu và cuối trùng hai đầu mút; N = 1 → ghế ở giữa đường | M | GEO-xx (fast-check) |
| FR-03.2 | Bốn kiểu đường: thẳng, cung tròn (qua 3 điểm), gấp khúc (2–200 điểm), bezier bậc ba | **G** mỗi kiểu đường **W** nhập 20 ghế **T** 20 ghế nằm trên đường; bezier lấy mẫu 64 điểm; ba điểm gần thẳng hàng (`|cross| < 1e-6 × |AB|²`) coi như đường thẳng | M (thẳng, cung) · S (gấp khúc, bezier) | GEO-xx |
| FR-03.3 | Kiểm tra khoảng cách tối thiểu và đề xuất sửa | **G** L = 480, `minSpacing` = 24 **W** nhập 22 ghế **T** popover báo "Đặt vừa tối đa 21 ghế" (`floor(480/24)+1`) và đề xuất hai cách: giảm số ghế hoặc kéo dài đường tới `L' = (22−1)×24 = 504` | M | GEO-xx, E2E |
| FR-03.4 | Đổi số ghế giữ UUID các ghế cũ | **G** hàng 14 ghế **W** đổi thành 18, rồi thành 10 **T** 14 UUID cũ còn nguyên, 4 UUID mới ở cuối hàng; giảm còn 10 → bỏ 8 ghế cuối; mọi ghế rải lại vị trí | M | GEO-xx |
| FR-03.5 | Đánh số `sequential` và `odd-even-center` | **G** N = 7, `start` = 1, `scheme = odd-even-center`, `forward` **T** thứ tự trái→phải `6 4 2 1 3 5 7`; `sequential` `reverse`, N = 5, `start` = 1 → `5 4 3 2 1` | M | GEO-xx |
| FR-03.6 | Nhân bản song song | **G** một hàng cung bán kính 600 **W** nhân 9 hàng, khoảng cách 40, bật thêm ghế **T** 9 hàng đồng tâm bán kính 640, 680, …; số ghế mỗi hàng `round(N × r_k / r)`; nhãn hàng tự tăng A→J | M | GEO-xx, E2E |
| FR-03.7 | Sửa từng ghế: loại vé, `accessible`, `blocked`, ghi đè số | **G** hàng 14 ghế **W** đặt `blocked` cho ghế 7, đổi loại vé 4 ghế giữa **T** ghế 7 không có unit sau xuất bản; sức chứa SEAT = số ghế không blocked gán loại vé đó | M | GEO-xx, E2E |
| FR-03.8 | Undo/redo 200 bước, một command mỗi lần kéo | **G** 200 command **W** undo hết **T** tài liệu trùng ban đầu byte-for-byte; một lần kéo sinh đúng 1 command | M | Vitest |
| FR-03.9 | Tự lưu bản nháp sau 2 giây không thay đổi | **G** thay đổi cuối lúc t **W** t + 2 giây **T** `PUT /organizer/maps/{id}/draft` với `revision` hiện tại → 200 `{"revision": r+1}`; đang lưu thì gom thay đổi vào lần sau | M | MV-xx, E2E |
| FR-03.10 | Xung đột giữa hai tab | **G** hai tab cùng `revision = 41` **W** tab A lưu, rồi tab B lưu **T** B nhận 409 `REVISION_CONFLICT`, dừng tự lưu, hiện "Một tab khác đã lưu bản mới hơn"; "Tải lại bản mới nhất" bỏ thay đổi cục bộ | M | MV-xx, E2E |
| FR-03.11 | Mất mạng không mất bản nháp | **G** tắt mạng 30 giây rồi bật **T** thử lại sau 2, 4, 8, 16, 30 giây; thanh trên báo "Chưa lưu được"; tài liệu chưa lưu giữ trong IndexedDB theo `seat_map_id`, khôi phục sau khi tải lại | S | E2E |
| FR-03.12 | Khối ghế (công cụ B) | **G** hình chữ nhật cao 200 **W** chia 6 hàng × 20 ghế, `minSpacing` 24 **T** khoảng cách giữa hàng `200/(6−1) = 40 ≥ 24` đạt; 10 hàng → `200/9 = 22,2 < 24` báo "Khối này đặt vừa tối đa …" | S | GEO-xx |
| FR-03.13 | Ảnh nền mặt bằng | **G** ảnh PNG 1,5 MB **W** tải lên làm nền, độ mờ 0,4, khóa **T** `canvas.background = {mediaId, x, y, w, h, opacity: 0.4, locked: true}`; ảnh không có ở layer ghế | S | E2E |
| FR-03.14 | Hiệu năng render | **G** sơ đồ 10.000 ghế **W** pan/zoom 10 giây **T** ≥ 30 fps (p5) trên máy chuẩn DR-39; mở tài liệu 20.000 ghế < 2 giây | M | EXP-09, S-04 |

### FR-04 · Khu vực bằng shape

Nguồn: SDD gốc §7.5, DR-32, DR-34. UC-08. Thiết kế: DOC-21, DOC-22.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-04.1 | Ba shape: chữ nhật, ellipse, đa giác (3–200 đỉnh) | **G** đa giác 5 đỉnh **W** nhập sức chứa 1.000 **T** zone có `capacity = 1000`, shape `polygon` 5 điểm; xuất bản tạo 1 `inventory_pool` kind `ZONE` với đúng 1.000 unit | M | GEO-xx, E2E |
| FR-04.2 | Sức chứa do người tổ chức nhập, ≥ 1 | **G** zone **W** nhập 0 **T** validate lỗi `ZONE_CAPACITY_INVALID`; diện tích chỉ hiển thị tham khảo | M | GEO-xx |
| FR-04.3 | Nhãn zone nằm trong shape kể cả đa giác lõm | **G** đa giác chữ L **T** điểm đặt nhãn (polylabel, độ chính xác 1 đơn vị) nằm trong đa giác | S | GEO-xx |
| FR-04.4 | Đa giác tự cắt bị báo lỗi | **G** đa giác bốn đỉnh thắt nơ **T** validate `POLYGON_SELF_INTERSECTS` mức error | M | GEO-xx |
| FR-04.5 | Phía người mua chọn zone bằng phép thử điểm trong đa giác | **G** zone đa giác **W** click tại điểm trong/ngoài **T** trong → mở ô chọn số lượng; ngoài → không | M | E2E |

### FR-05 · Validate sơ đồ và phiên bản bất biến

Nguồn: SDD gốc §7.7–§7.8, DR-16, DR-32, DR-35, DR-37. UC-08, UC-14. Thiết kế: DOC-16, DOC-21, DOC-23.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-05.1 | 12 quy tắc validate với mã cố định, dùng chung client và server | **G** hai ghế cách nhau 15 đơn vị (`seatDiameter` 20) **W** validate **T** `SEAT_OVERLAP` mức error với `objectIds` của hàng; tài liệu có 20.001 ghế → `SEAT_LIMIT_EXCEEDED`; zone chồng nhau → `ZONES_OVERLAP` mức warning; mọi fixture cho cùng danh sách mã ở Vitest và JUnit | M | GEO-xx, fixture chung |
| FR-05.2 | Kết quả của server là kết quả cuối cùng | **G** client báo đạt **W** `POST /organizer/maps/{id}/publish` mà server thấy lỗi **T** 422 `MAP_VALIDATION_FAILED` kèm `issues`; không tạo phiên bản | M | MV-xx |
| FR-05.3 | Validate server đủ nhanh | **G** 20.000 ghế + 200 zone **T** `POST …/validate` < 500 ms | M | S-05, GEO-xx |
| FR-05.4 | Xuất bản tạo phiên bản bất biến có checksum | **G** bản nháp đạt validate **W** `POST …/publish` **T** 1 dòng `seat_map_version` (`version_no` tăng 1, `checksum` SHA-256 của JCS); `UPDATE seat_map_version …` bị trigger từ chối với "seat_map_version is immutable" | M | MV-xx |
| FR-05.5 | Giới hạn kích thước tài liệu | **G** tài liệu 5,1 MB **W** `PUT …/draft` **T** 413 `PAYLOAD_TOO_LARGE`; body > 256 KB do client nén `gzip`, API giải nén tới 5 MB | M | MV-xx |
| FR-05.6 | Xuất bản phiên bản mới trước giờ mở bán dựng lại kho vé | **G** event `UPCOMING` có 178 unit ghế **W** thêm 10 ghế, xuất bản **T** số unit ghế = số ghế không blocked mới (188), `seat_index` liên tục từ 0 và khớp tài liệu, không unit nào `HELD` | M | MV-xx |

### FR-06 · Giữ vé nguyên tử cho ba mô hình

Nguồn: SDD gốc §8.1–§8.2, DR-41. UC-03. Thiết kế: DOC-24.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-06.1 | Một ghế có tối đa một chủ | **G** ghế A1 `AVAILABLE` **W** 64 luồng cùng giữ A1, lặp 20 lần **T** mỗi lần đúng 1 thành công, 63 nhận 409 `SEATS_UNAVAILABLE`; không deadlock | M | INVT-01, EXP-01 |
| FR-06.2 | Pool không bao giờ vượt sức chứa | **G** pool 50 vé **W** 64 luồng, mỗi luồng xin ngẫu nhiên 1–4 vé, lặp 20 lần **T** tổng unit `HELD` ≤ 50 mỗi lần; không deadlock; số unit của pool vẫn 50 | M | INVT-02, EXP-02 |
| FR-06.3 | Tất cả hoặc không | **G** request SEAT 2 ghế + ZONE 3 vé + GA 2 vé, một ghế đã `HELD` **W** giữ vé **T** 409 `SEATS_UNAVAILABLE` kèm `unavailableSeatIds` (1 phần tử); không unit nào `HELD`; transaction rollback cả `reservation` | M | INVT-03 |
| FR-06.4 | Giới hạn kỹ thuật 50 unit mỗi lệnh giữ | **G** 51 vé GA trong một lệnh **W** giữ vé **T** 422 `VALIDATION_FAILED` `rule = too_many_units` (không lấy connection); 50 vé → 201 | M | INVT-04 |
| FR-06.5 | Kiểm tra hình thức request | **G** `items` rỗng, 21 dòng, `seatIds` trùng, `quantity = 0`, hai dòng cùng `zoneId` **T** mỗi trường hợp 422 `VALIDATION_FAILED` trước khi mở transaction | M | INVT-05 |
| FR-06.6 | Mỗi người một reservation mở mỗi sự kiện | **G** người dùng đã có reservation `ACTIVE` **W** giữ vé lần nữa **T** 409 `ACTIVE_RESERVATION_EXISTS` kèm `reservationId` đang mở; hai request song song → đúng 1 tạo được | M | INVT-06 |
| FR-06.7 | Chỉ giữ trong khung mở bán | **G** event `PAUSED`, `DRAFT`, `SALE_CLOSED`, `now() < sale_starts_at` **W** giữ vé **T** 409 `EVENT_NOT_ON_SALE` kèm `displayStatus` | M | INVT-07 |
| FR-06.8 | Loại item khớp mô hình loại vé | **G** `seatIds` của ghế thuộc loại vé `ZONE` **W** giữ vé **T** 422 `VALIDATION_FAILED`; `zoneId` không phải pool ZONE của event, `ticketTypeId` GA không phải loại GA → 422 | M | INVT-08 |
| FR-06.9 | Thiếu sức chứa trả mã lỗi rõ | **G** pool còn 3 vé **W** xin 4 **T** 409 `INSUFFICIENT_CAPACITY` kèm `poolId`, `requested = 4`; không giữ gì | M | INVT-09 |
| FR-06.10 | Giá chụp lúc giữ và tổng tiền tính ở server | **G** giữ 2 vé 100.000 + 1 vé 250.000 **T** `orders.amount = 450000`, `status = PENDING_PAYMENT`; client không gửi số tiền; tổng 0 đồng vẫn tạo order (`confirm-free`) | M | INVT-10 |

### FR-07 · Giữ vé có thời hạn và tự trả vé

Nguồn: SDD gốc §8.3–§8.5, DR-42, DR-43. UC-03, UC-06, UC-11. Thiết kế: DOC-24, DOC-26.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-07.1 | Thời hạn giữ 10 phút | **G** giữ vé thành công lúc 03:00:00 **T** `expires_at = 03:10:00` (tính bằng `now()` của database); `reservation.hold-duration = PT10M` | M | INVT-11 |
| FR-07.2 | Job trả vé trả mọi unit quá hạn | **G** 1.000 reservation quá hạn **W** job chạy (nhịp 5 giây, lô 200) **T** mọi unit về `AVAILABLE` trong ≤ 30 giây kể từ `expires_at` (NFR-03); reservation `EXPIRED`, order `EXPIRED` | M | INVT-12, EXP-04 |
| FR-07.3 | Trả vé idempotent, không trả nhầm | **G** job chạy hai lần liên tiếp hoặc dừng giữa chừng **T** unit của reservation khác không đổi; không unit nào trả quá một lần | M | INVT-13, EXP-04 |
| FR-07.4 | Hủy giữ vé đường nhanh | **G** reservation `ACTIVE` chưa có PaymentIntent **W** `DELETE /reservations/{id}` **T** 200 `{"status":"CANCELLED"}`, unit `HELD → AVAILABLE` ngay, order `CANCELLED`; gọi lại → 200 trạng thái hiện tại | M | INVT-14 |
| FR-07.5 | Hủy giữ khi Stripe lỗi | **G** reservation có PaymentIntent, Stripe timeout (3 giây) **W** `DELETE` **T** 202 `{"status":"EXPIRING"}`; unit vẫn `HELD`; job hoàn tất sau trong ≤ 30 giây lease | M | PAY-xx |
| FR-07.6 | Không hủy được khi tiền đã về | **G** PaymentIntent `succeeded` **W** `DELETE` **T** 409 `PAYMENT_ALREADY_SUCCEEDED`; giao diện sang màn Kết quả | M | PAY-xx |
| FR-07.7 | Lease cho reservation `EXPIRING` | **G** reservation `EXPIRING` có `expiring_since` cũ hơn 30 giây **W** job vòng sau **T** job lấy lại (`expiring_since = now()`) và xử lý tiếp | M | INVT-15 |
| FR-07.8 | Job dừng không bao giờ gây bán trùng | **G** dừng API 5 phút **T** vé giữ lâu hơn; khi khởi động lại job xử lý hết reservation quá hạn; `make invariants` sạch | M | EXP-04 |

### FR-08 · Idempotency

Nguồn: SDD gốc §8.6, DR-45. Thiết kế: DOC-25.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-08.1 | Header `Idempotency-Key` bắt buộc ở bốn endpoint | **G** `POST /events/{id}/reservations`, `DELETE /reservations/{id}`, `POST /orders/{id}/confirm-free`, `POST /orders/{id}/payment-intent` **W** thiếu header **T** 400 `IDEMPOTENCY_KEY_REQUIRED` | M | IDEM-01 |
| FR-08.2 | Cùng key N lần chỉ một tác dụng | **G** cùng key và body **W** 20 request song song **T** 1 reservation, 20 response giống nhau, 19 có `Idempotent-Replayed: true` | M | IDEM-02, EXP-03 |
| FR-08.3 | Cùng key khác body bị từ chối | **G** key K đã dùng với body B1 **W** gửi K với body B2 **T** 422 `IDEMPOTENCY_KEY_REUSED` | M | IDEM-03 |
| FR-08.4 | Mất response vẫn nhận lại kết quả | **G** API dừng sau commit, trước response **W** client retry cùng key **T** 201 với cùng `reservationId`, `Idempotent-Replayed: true` | M | IDEM-04, EXP-08 |
| FR-08.5 | Chỉ lưu kết quả 2xx | **G** lần đầu nhận 409 **W** gửi lại cùng key **T** chạy lại thật (409 không để lại tác dụng phụ) | M | IDEM-05 |
| FR-08.6 | Dọn key sau 24 giờ | **G** dòng `idempotency_key` cũ 25 giờ **W** `RetentionJob` 03:00 **T** dòng bị xóa; dòng cũ 23 giờ còn | S | IDEM-06 |

### FR-09 · Thanh toán Stripe

Nguồn: SDD gốc §9, DR-44, DR-47…51. UC-04, UC-15, UC-16, UC-19. Thiết kế: DOC-26, DOC-09.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-09.1 | PaymentIntent chỉ thẻ, VND, idempotent theo `order_id` | **G** order 450.000 **W** `POST /orders/{id}/payment-intent` hai lần **T** cùng `clientSecret`; Stripe thấy 1 PaymentIntent `amount = 450000`, `currency = vnd`, `payment_method_types = ["card"]`, `metadata.order_id`; khóa Stripe `pi-create:<order_id>` | M | PAY-01 |
| FR-09.2 | Từ chối khi reservation không còn đủ thời gian | **G** reservation còn 29 giây **W** tạo PaymentIntent **T** 409 `PAYMENT_WINDOW_TOO_SHORT`; reservation không `ACTIVE` → 409 `RESERVATION_NOT_ACTIVE` | M | PAY-02 |
| FR-09.3 | Không để job trả vé đua với việc lưu `payment_intent_id` | **G** chèn độ trễ giữa tạo PaymentIntent và lưu ID, cho reservation hết hạn đúng lúc **W** chạy 500 lần **T** 0 PaymentIntent thành công mà vé đã trả | M | PAY-03, EXP-06 |
| FR-09.4 | Webhook xác minh chữ ký, dung sai 300 giây | **G** `Stripe-Signature` sai, hoặc timestamp lệch 301 giây **W** `POST /webhooks/stripe` **T** 400; không dòng `stripe_event` | M | PAY-04 |
| FR-09.5 | Loại trùng và không phụ thuộc thứ tự | **G** event `payment_intent.succeeded` **W** gửi 3 lần, xáo thứ tự với `payment_failed` **T** đúng 1 bộ vé; 2 lần sau 200 và `outcome` không đổi | M | PAY-05, EXP-07 |
| FR-09.6 | Transaction xác nhận trọn gói | **G** reservation `ACTIVE`, webhook `succeeded` khớp số tiền **T** một transaction: reservation `CONFIRMED`, mọi unit `SOLD`, order `PAID` (`paid_at`), một `ticket` mỗi unit, outbox `EMAIL_TICKETS`; mở đầu bằng `FOR SHARE` trên event | M | PAY-06 |
| FR-09.7 | Thẻ bị từ chối giữ đơn `PENDING_PAYMENT` | **G** webhook `payment_failed` `decline_code = "insufficient_funds"` **T** `last_payment_error` ghi mã; đơn vẫn `PENDING_PAYMENT`; thử thẻ khác trong thời hạn thành công | M | PAY-07 |
| FR-09.8 | Số tiền lệch chuyển chờ hoàn tiền | **G** `amount_received` 400.000 khác order 450.000 **T** order `REFUND_PENDING`, `refund_reason = AMOUNT_MISMATCH`, không vé, 1 outbox `EMAIL_REFUND_PENDING` | M | PAY-08 |
| FR-09.9 | Thanh toán đến trễ không phát hành vé | **G** reservation `EXPIRED` (ghế còn trống hay không) **W** webhook `succeeded` đến **T** order `REFUND_PENDING`, `refund_reason = LATE_PAYMENT`, 0 vé, unit không đổi, 1 email, log ERROR | M | PAY-09, EXP-06 |
| FR-09.10 | Job trả vé hủy PaymentIntent trước khi trả vé | **G** reservation `EXPIRING` có PaymentIntent **W** job chạy **T** hủy được → trả vé; Stripe báo `succeeded` → không trả, gọi handler xác nhận; Stripe lỗi → giữ `EXPIRING`, thử lại sau lease | M | PAY-10 |
| FR-09.11 | Đối chiếu khi webhook thất lạc | **G** bỏ webhook, PaymentIntent `succeeded`, order `PENDING_PAYMENT` quá 15 phút **W** `PaymentReconcileJob` (60 giây) **T** order `PAID` trong ≤ 60 giây kể từ ngưỡng; chạy trùng với webhook vô hại | M | PAY-11 |
| FR-09.12 | Hoàn tiền thủ công cập nhật có điều kiện | **G** order `REFUND_PENDING` **W** `UPDATE … SET status = 'REFUNDED'` thiếu `refund_reference` **T** CHECK `orders_refunded_ck` từ chối; đủ `refund_reference`, `refunded_at` → thành công | M | PAY-12 |
| FR-09.13 | Stripe lỗi khi tạo PaymentIntent | **G** Stripe timeout (read 10 giây) **T** 503 `PAYMENT_PROVIDER_UNAVAILABLE`; reservation giữ tới hết hạn; client thử lại trong thời hạn | M | PAY-13 |
| FR-09.14 | Cổng thanh toán giả thay Stripe trong test | **G** `PAYMENTS_MODE=fake` **T** cùng chuỗi trạng thái và lỗi hủy như Stripe trên 5 kịch bản của S-02; webhook ký HMAC; API từ chối khởi động nếu profile `fake-payments` đi cùng khóa `sk_live_…` | M | PAY-14 |

### FR-10 · Vé điện tử và email

Nguồn: SDD gốc §9.4, DR-52…54. UC-04, UC-05. Thiết kế: DOC-27.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-10.1 | Mỗi unit của đơn `PAID` có đúng một vé với mã duy nhất | **G** đơn 3 vé, tiền tố `GM` **T** 3 dòng `ticket` `ISSUED` có `code` dạng `GM-4K7P-92XD` (`PP-XXXX-XXXX`, Crockford Base32); `ticket_unit_issued_uq` chặn vé thứ hai cho một unit | M | TN-01 |
| FR-10.2 | Email vé đủ nội dung | **G** đơn `PAID` **T** 1 email `tickets` gồm tên sự kiện, giờ và địa điểm theo múi giờ sự kiện, vị trí ("Khán đài A · Hàng C · Ghế 09" hoặc tên khu vực), mã vé, link tới "Vé của tôi"; không đính kèm, không QR | M | TN-02 |
| FR-10.3 | Outbox gửi ít nhất một lần, backoff | **G** SMTP lỗi **W** `OutboxRelay` 3 lần **T** `attempts = 3`, `next_attempt_at` tăng theo `least(10 s × 2^(attempts−1), 1 giờ)`; lần 4 thành công → `SENT`; Mailpit nhận đúng 1 thư (`Message-ID` cố định); `attempts ≥ 12` → `FAILED` | M | TN-03 |
| FR-10.4 | Vé luôn xem được dù email lỗi | **G** outbox `FAILED` **W** `GET /me/tickets` **T** vẫn trả đủ vé của người dùng, nhóm theo sự kiện | M | TN-04 |
| FR-10.5 | Nhận vé đơn 0 đồng | **G** đơn 0 đồng, reservation `ACTIVE` **W** `POST /orders/{id}/confirm-free` **T** vé `ISSUED` đủ số, outbox `EMAIL_TICKETS`; reservation không `ACTIVE` → 409 `RESERVATION_NOT_ACTIVE` | M | TN-05 |
| FR-10.6 | Email chờ hoàn tiền theo lý do | **G** order `REFUND_PENDING` **T** email `refund-pending` có nội dung theo `refund_reason` (`LATE_PAYMENT`, `AMOUNT_MISMATCH`, `EVENT_CANCELLED`), `support.email`, `support.refund-sla-text` (mặc định "5–10 ngày làm việc") | M | TN-06 |

### FR-11 · Kiểm soát tiếp nhận

Nguồn: SDD gốc §10.1, §10.4, §10.5, DR-46, DR-55…56, DR-61. UC-12. Thiết kế: DOC-28.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-11.1 | Rate limit theo IP ở nginx | **G** `api_ip` 20 r/s burst 40 **W** 41 request/giây từ một IP **T** request vượt burst nhận 429 `RATE_LIMITED`; IP trong `RATE_LIMIT_ALLOWLIST` không bị chặn | M | ADM-01 |
| FR-11.2 | Token bucket theo người dùng | **G** `rl:hold:{userId}` sức chứa 5, nạp 1 mỗi 2 giây **W** 6 lệnh giữ liên tiếp **T** lệnh thứ 6 nhận 429 `RATE_LIMITED` kèm `Retry-After` | M | ADM-02 |
| FR-11.3 | Cờ hết vé | **G** pool hết vé **W** lệnh giữ kế tiếp **T** 409 `INSUFFICIENT_CAPACITY` không chạm database (số truy vấn = 0); trả vé → cờ xóa | S | ADM-03 |
| FR-11.4 | Bulkhead | **G** 24 permit, database bị chặn **W** 100 lệnh giữ đồng thời **T** 24 vào, 76 nhận 503 `OVERLOADED` kèm `Retry-After` 1–3 giây; webhook vẫn xử lý được | M | ADM-04, EXP-05 |
| FR-11.5 | Mất Redis không làm sai | **G** dừng Redis **T** sự kiện thường vẫn giữ vé được; sự kiện `high_demand` nhận 503 `OVERLOADED` với `Retry-After: 5`; `make invariants` sạch | M | ADM-05, EXP-08 |
| FR-11.6 | Quy ước retry phía client | **G** 409 → không tự retry, cập nhật giao diện; 429/503 → backoff có jitter, tôn trọng `Retry-After`, cùng `Idempotency-Key`; timeout mạng → retry cùng key | M | E2E |
| FR-11.7 | Tình trạng chỗ không tạo tải lên database | **G** 1.000 request `GET /events/{id}/availability` trong 1 giây **T** ≤ 1 lần dựng từ database (Caffeine, hết hạn 2 giây) | M | AV-xx |

### FR-12 · Lệnh kiểm tra bất biến

Nguồn: SDD gốc §14.3, DR-73. UC-18, UC-21. Thiết kế: DOC-30.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-12.1 | Chạy định kỳ mỗi 5 phút | **G** API chạy **T** `InvariantChecker` chạy mỗi 5 phút; có sai lệch thì log ERROR | M | IC-xx |
| FR-12.2 | Chạy tay bằng `make invariants` | **G** dữ liệu sạch **W** `make invariants` **T** in JSON `{checkedAt, durationMs, violations: []}`, thoát 0; cố ý `UPDATE` sai một unit → thoát 1, JSON nêu đúng `INV-xx` với tối đa 10 ID mẫu | M | IC-xx |
| FR-12.3 | Danh mục kiểm tra | **T** gồm: số unit chưa `REMOVED` của pool = `capacity`; unit `HELD` trỏ tới reservation đang mở và số unit khớp item; unit `SOLD` có đúng một vé `ISSUED`; reservation `ACTIVE` quá `expires_at` hơn 60 giây; `EXPIRING` quá 2 phút; order `PAID` có reservation `CONFIRMED` và đủ vé; event `CANCELLED` còn đơn `PAID` hoặc vé `ISSUED`; outbox `FAILED`; số đơn `REFUND_PENDING` theo lý do | M | IC-xx |
| FR-12.4 | Chạy sau mọi thực nghiệm | **G** một lần chạy EXP **T** bất biến đỏ → lần chạy bị loại; `make exp` luôn kết thúc bằng `make invariants` | M | EXP-xx |

### FR-13 · Hồ sơ tổ chức (bổ sung)

Nguồn: DR-23. UC-07. Thiết kế: DOC-19.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-13.1 | Lập hồ sơ một lần | **G** tài khoản chưa có hồ sơ **W** `POST /organizer {"name":"Nhà hát Bến Sông"}` **T** 201; `GET /me` có `roles ["BUYER","ORGANIZER"]` và `organizer`; gọi lần hai → 409 `ORGANIZER_EXISTS` | M | AU-13 |
| FR-13.2 | Kiểm tra dữ liệu hồ sơ | **G** tên rỗng hoặc 121 ký tự, `contactEmail` sai định dạng **T** 422 `VALIDATION_FAILED`; `contactEmail` trống → dùng email đăng nhập | M | AU-14 |
| FR-13.3 | `/organizer/**` cần hồ sơ | **G** tài khoản chưa có hồ sơ **W** `GET /organizer/events` **T** 403 `ORGANIZER_PROFILE_REQUIRED`; giao diện chuyển tới màn Lập hồ sơ | M | AU-15 |

### FR-14 · Tạm dừng và hủy sự kiện, kể cả khi đã bán (bổ sung)

Nguồn: DR-24, DR-28, DR-44. UC-13, UC-19. Thiết kế: DOC-20, DOC-26.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-14.1 | Hủy từ `PUBLISHED`/`PAUSED`; `DRAFT` không hủy | **G** event `DRAFT` **W** `POST …/cancel` **T** 409 `EVENT_STATE_CONFLICT`; từ `PUBLISHED` → 200 | M | EV-17 |
| FR-14.2 | Đơn đã trả tiền chuyển chờ hoàn tiền | **G** event có 3 đơn `PAID` (8 vé) **W** hủy **T** 3 đơn `REFUND_PENDING` `EVENT_CANCELLED`; 8 vé `VOID`; 3 outbox `EMAIL_REFUND_PENDING`; response `{"refundPendingOrders":3,"releasingReservations":0}` | M | EV-18 |
| FR-14.3 | Reservation đang mở được trả | **G** 2 reservation `ACTIVE` **W** hủy **T** chuyển `EXPIRING` `close_reason = EVENT_CANCELLED`; job hủy PaymentIntent rồi trả vé; response `releasingReservations = 2` | M | EV-19 |
| FR-14.4 | Hủy không đua được với webhook | **G** webhook xác nhận và lệnh hủy chạy song song **W** lặp 1.000 lần **T** 0 sự kiện `CANCELLED` còn đơn `PAID` hoặc vé `ISSUED`; mỗi đơn đã trả tiền đúng 1 email chờ hoàn tiền | M | EV-20, PAY-xx |
| FR-14.5 | Hộp thoại hủy báo trước số đơn | **G** event có 12 đơn `PAID` **T** hộp thoại ở Studio 06 nêu "12 đơn sẽ chuyển sang chờ hoàn tiền" | M | E2E |

### FR-15 · Khóa sơ đồ từ giờ mở bán và đóng bán sớm (bổ sung)

Nguồn: DR-24, DR-37. UC-09, UC-14. Thiết kế: DOC-20, DOC-23.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-15.1 | Khóa từ `sale_starts_at`, kể cả khi chưa ai mua | **G** `now() >= sale_starts_at`, chưa đơn nào **W** `PUT …/draft` và `POST …/publish` **T** cả hai 409 `MAP_LOCKED_AFTER_SALE`; editor mở chỉ đọc "Sơ đồ đã khóa vì sự kiện đã mở bán"; nhân bản vẫn được | M | MV-xx |
| FR-15.2 | Trước giờ mở bán xuất bản được và dựng lại kho vé | **G** event `UPCOMING` **W** xuất bản phiên bản mới **T** transaction `lock_timeout 5s` xóa mọi unit ghế/zone `AVAILABLE`, chèn lại từ phiên bản mới; `event.seat_map_version_id` cập nhật | M | MV-xx |
| FR-15.3 | Đua với giây mở bán an toàn | **G** xuất bản đúng giây mở bán, song song 100 lệnh giữ vé **T** hoặc 409 `MAP_LOCKED_AFTER_SALE`, hoặc kho vé mới đầy đủ; không unit `HELD` nào bị xóa (số dòng xóa khác tổng unit → rollback) | M | MV-xx |
| FR-15.4 | Chờ lock quá lâu báo bận | **G** lock bị giữ > 5 giây **T** 409 `MAP_PUBLISH_BUSY` | S | MV-xx |

### FR-16 · Phòng chờ có rời hàng (bổ sung)

Nguồn: SDD gốc §10.2, DR-57…60. UC-12. Thiết kế: DOC-28.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-16.1 | Hàng trong suốt khi còn chỗ | **G** `|admitted| < max_active`, hàng trống **W** `POST /events/{id}/queue` **T** `ADMITTED` ngay, giao diện không hiện phòng chờ | M | ADM-06 |
| FR-16.2 | Không bao giờ vượt `max_active` | **G** `max_active = 10`, 100 người vào **T** không lúc nào `|admitted| > 10`; người rời hàng → người kế tiếp vào ở vòng job sau (1 giây) | M | ADM-07 |
| FR-16.3 | Lệnh giữ vé yêu cầu lượt vào | **G** sự kiện có kiểm soát tiếp nhận, người chưa có lượt hoặc lượt hết hạn **W** giữ vé **T** 429 `QUEUE_REQUIRED` (kiểm `ZSCORE admitted:{eventId} <userId> > now_ms`) | M | ADM-08 |
| FR-16.4 | Rời hàng | **G** người trong `queue` **W** `DELETE /events/{id}/queue` **T** bị xóa khỏi `prequeue`, `queue`, `admitted`, `seen`; chỗ cấp cho người kế tiếp | M | ADM-09 |
| FR-16.5 | Xáo ngẫu nhiên lúc mở bán cho sự kiện `high_demand` | **G** 1.000 người trong `prequeue` **W** đúng giờ mở bán **T** điểm `queue` là số ngẫu nhiên [0,1); người đến sau có điểm `1 + INCR queue-seq`; đến sớm 30 phút hay 1 phút như nhau | M | ADM-10 |
| FR-16.6 | `idle_timeout` | **G** người ngừng hỏi vị trí 121 giây **T** bị loại khỏi `queue` ở vòng `admit.lua` kế tiếp | M | ADM-11 |
| FR-16.7 | `pass_ttl` 5 phút, gia hạn khi giữ vé | **G** lượt vào cấp lúc t **T** hết hạn lúc t + 5 phút nếu chưa giữ vé; giữ vé thành công → kéo tới `expires_at` của reservation; reservation đóng mà chưa trả tiền → còn 2 phút để chọn lại | M | ADM-12 |
| FR-16.8 | Nhịp hỏi do server quyết định | **G** vị trí 150 / 1.500 / 20.000 **T** `retryAfterSeconds` = 3 / 10 / 30; `estimatedWaitSeconds` null khi chưa có số liệu `admit-hist` | S | ADM-13 |

### FR-17 · Đa ngôn ngữ (bổ sung)

Nguồn: DR-10, DR-12. UC-20. Thiết kế: DOC-31.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-17.1 | Hai locale `vi` (mặc định) và `en` cùng tập key | **G** `frontend/src/locales/vi/*.json` và `en/*.json`, `messages_vi.properties` và `messages_en.properties` **T** `pnpm i18n:check` và test JUnit xanh; thiếu key ở một locale → đỏ | M | i18n:check |
| FR-17.2 | Thứ tự chọn locale | **G** người dùng đã đăng nhập `locale = en`, cookie `tb_lang = vi` **T** giao diện `en`; chưa đăng nhập, cookie `tb_lang = vi`, `Accept-Language: en` → `vi`; không cookie → `Accept-Language`; không khớp → `vi` | M | E2E |
| FR-17.3 | Đổi ngôn ngữ cập nhật tài khoản | **G** đã đăng nhập **W** chọn `en` **T** cookie `tb_lang = en`; `PATCH /me {"locale":"en"}` → 200; email và Problem Details sau đó theo `en` | M | E2E |
| FR-17.4 | Problem Details theo `Accept-Language` | **G** lỗi validate **W** `Accept-Language: vi` **T** `title`/`detail` tiếng Việt; `code` luôn tiếng Anh | M | P1-04 |
| FR-17.5 | Định dạng tiền theo locale | **G** 1.800.000 VND **T** `vi` → `1.800.000 ₫`; `en` → `₫1,800,000`; không có phần lẻ | M | E2E |
| FR-17.6 | Giờ sự kiện theo múi giờ của sự kiện | **G** event `Asia/Tokyo`, `PLATFORM_TIMEZONE = Asia/Ho_Chi_Minh` **T** giờ hiển thị theo `Asia/Tokyo` kèm hậu tố `GMT+9`; event theo múi giờ mặc định không hậu tố | M | E2E |
| FR-17.7 | Dữ liệu do người tổ chức nhập không dịch | **T** tên sự kiện, loại vé, khu vực hiển thị nguyên văn ở cả hai locale | M | E2E |

### FR-18 · Ảnh sự kiện (bổ sung)

Nguồn: DR-38. UC-07. Thiết kế: DOC-20, DOC-15.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-18.1 | Giới hạn ảnh | **G** ảnh `EVENT_IMAGE` 3 MB **W** `POST /organizer/media` **T** 413 `PAYLOAD_TOO_LARGE`; 2 MB → 201; `FLOOR_PLAN` tối đa 5 MB | M | EV-21 |
| FR-18.2 | Kiểm tra định dạng bằng magic bytes | **G** tệp `.png` chứa nội dung không phải ảnh, hoặc ảnh 8.001 px một cạnh **T** 422 `MEDIA_INVALID`; chỉ nhận `image/jpeg`, `image/png`, `image/webp` | M | EV-22 |
| FR-18.3 | Phục vụ ảnh có cache | **G** ảnh đã tải **W** `GET /media/{id}` **T** đúng byte, `Cache-Control: public, max-age=31536000, immutable`, `ETag` = sha256; lần hai qua nginx `proxy_cache` không gọi storage | M | EV-23 |
| FR-18.4 | Không giữ connection DB khi gọi storage | **G** tải lên **T** `PutObject` trước, rồi chèn dòng `media`; chèn lỗi → thử `DeleteObject` một lần | M | EV-24 |

### FR-19 · Email đổi lịch (bổ sung)

Nguồn: DR-29. UC-17. Thiết kế: DOC-20, DOC-27.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-19.1 | Fan-out khi đổi giờ hoặc địa điểm | **G** event `PUBLISHED` có 3 đơn `PAID` **W** `PATCH` đổi `startsAt` **T** 3 dòng outbox `EMAIL_EVENT_CHANGED` với `{orderId, locale, before, after}`; response `notifiedOrders: 3` | M | EV-25, TN-07 |
| FR-19.2 | Không gửi cho bản nháp hoặc thay đổi không liên quan | **G** event `DRAFT`, hoặc chỉ đổi `description` **T** 0 dòng outbox; `notifiedOrders: 0` | M | EV-26 |
| FR-19.3 | Mỗi lần lưu là một đợt, không gom | **G** hai lần `PATCH` liên tiếp đổi `venue` **T** 3 + 3 = 6 dòng outbox | S | EV-27 |

### FR-20 · Số liệu bán vé theo trạng thái ghế (bổ sung)

Nguồn: DR-62, DR-71. UC-10. Thiết kế: DOC-29, DOC-60 (màn hình).

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-20.1 | Số vé theo loại vé | **G** sau một lần mua 2 vé VIP **W** `GET /organizer/events/{id}/sales` **T** VIP: `sold` tăng 2, `held` giảm tương ứng, `available` đúng; không đếm unit `REMOVED` | M | AV-xx |
| FR-20.2 | Trạng thái từng ghế cho studio | **G** event có sơ đồ **W** `GET /organizer/events/{id}/seat-status` **T** trả đúng snapshot (bitmap `held`/`sold`) dùng chung cache với người mua | M | AV-xx |
| FR-20.3 | Làm mới tự động | **G** màn Theo dõi bán vé **T** tự làm mới mỗi 15 giây và khi bấm "Làm mới"; hiện "Cập nhật lúc …" | S | E2E |
| FR-20.4 | Tổng quan studio | **G** 20 sự kiện **T** một truy vấn gộp theo `event_id` cho tối đa 20 sự kiện mỗi trang, kèm `sold`, `held`, `available`, `capacity` | M | AV-xx |

### FR-21 · Nhân bản sơ đồ từ sự kiện khác (bổ sung)

Nguồn: DR-31. UC-22. Thiết kế: DOC-16, DOC-23.

| ID | Yêu cầu | Tiêu chí nghiệm thu | Ưu tiên | Kiểm chứng |
| --- | --- | --- | --- | --- |
| FR-21.1 | Sinh lại mọi ID | **G** sơ đồ nguồn 180 ghế **W** `POST /organizer/maps/{sourceId}/clone {"eventId","source":"version","versionNo":2}` **T** sơ đồ mới 180 ghế với 180 UUID khác hoàn toàn nguồn; section, row, zone, decoration có ID mới; hình học, nhãn, đánh số, cờ trùng nguồn | M | MV-xx |
| FR-21.2 | Ánh xạ loại vé theo tên | **G** nguồn dùng "VIP" và "Thường"; event đích chỉ có "vip" (cùng mô hình) **T** ghế VIP ánh xạ sang "vip"; ghế "Thường" để trống `ticketTypeId`, validate báo `TICKET_TYPE_MISSING` | M | MV-xx |
| FR-21.3 | Mỗi event một sơ đồ | **G** event đích đã có sơ đồ **W** clone **T** 409 `MAP_ALREADY_EXISTS` | M | MV-xx |
| FR-21.4 | Chỉ nhân bản sơ đồ của cùng tổ chức, ảnh nền dùng chung `media_id` | **G** nguồn của tổ chức khác **T** 404 `NOT_FOUND`; nguồn hợp lệ có ảnh nền → sơ đồ mới trỏ cùng `media_id`, không sao chép byte; bắt đầu là bản nháp `revision = 0`, `cloned_from_seat_map_id` được lưu | M | MV-xx |

## 2. Yêu cầu phi chức năng

| ID | Yêu cầu | Chỉ tiêu | Cách đo | Công cụ | Kiểm chứng |
| --- | --- | --- | --- | --- | --- |
| NFR-01 | Không bán vượt | 0 vé vượt sức chứa; 0 ghế có hai chủ trong mọi thực nghiệm; mọi `INV-xx` sạch | Chạy `make invariants` sau mỗi lần chạy; đếm unit `HELD`+`SOLD` so với `capacity` | `InvariantChecker`, k6, JUnit đồng thời | EXP-01, EXP-02, EXP-04, EXP-08; bản `naive` phải cho thấy bán vượt |
| NFR-02 | Chịu tải | **100.000 người dùng đồng thời tranh 5.000 vé** theo mô hình của DR-75: "đồng thời" = có mặt trong cùng cửa sổ mở bán; mỗi VU k6 quản lý một nhóm người dùng ảo bằng `http.asyncRequest`, tôn trọng `retryAfterSeconds`; request được tiếp nhận có p95 < 500 ms; không sập (không 5xx ngoài 503 `OVERLOADED`). Trần thực tế do S-06 chốt; nếu < 100.000 thì DR mới hiệu chỉnh, giữ tỉ lệ người/vé 20:1 (planned) | Histogram `ticket_hold_duration_seconds` phía API và k6 `http_req_duration` | k6 trên máy thực nghiệm, Prometheus/Grafana (profile `obs`) | EXP-02 (với phòng chờ), EXP-05, S-06 |
| NFR-03 | Thời hạn giữ vé | Vé hết hạn về kho ≤ 30 giây sau `expires_at` khi job chạy (nhịp 5 giây + thời gian gọi Stripe) | Thời gian từ `expires_at` tới lúc unit cuối `AVAILABLE`, 5.000 reservation hết hạn cùng lúc | SQL trên `inventory_unit`, runner EXP-04 | EXP-04, INVT-12 |
| NFR-04 | Idempotency | Gửi lại cùng key N lần chỉ tạo đúng một reservation và một lần thu tiền | Đếm `reservation` theo `idem_key`; đếm PaymentIntent theo `order_id` | JUnit song song, k6 | EXP-03, IDEM-02, PAY-01 |
| NFR-05 | Tiền khớp vé | Mỗi đơn `PAID` có đúng một giao dịch Stripe thành công và đủ vé `ISSUED`; không vé nào thiếu đơn `PAID`; thanh toán trễ → `REFUND_PENDING`, 0 vé | `INV-xx` về đơn `PAID`/vé; số vé không có tiền và tiền không có vé | `InvariantChecker`, cổng thanh toán giả | EXP-06, EXP-07, PAY-xx |
| NFR-06 | Hiệu năng editor | Sơ đồ 10.000 ghế kéo và zoom ≥ 30 fps (p5 của fps khung hình) trên máy chuẩn: laptop 4 nhân, đồ họa tích hợp, Chrome stable, 1920 × 1080, `devicePixelRatio = 1` (DR-39) | Script Playwright pan/zoom 10 giây đo `requestAnimationFrame`; cấu hình thực ghi vào EXP-09 | Playwright | EXP-09, S-04 |
| NFR-07 | Bảo mật | Không lưu dữ liệu thẻ (Payment Element gửi thẳng Stripe); token đăng nhập và session chỉ lưu SHA-256; không hard-code secret (secret ở `.env`); email không vào log | Quét secret trong CI; kiểm tra payload outbox và cột `login_token`; kiểm log | CI, test SEC | AU-01, SEC-xx |
| NFR-08 | Tái lập môi trường | Một lệnh `docker compose up` đưa mọi container (nginx, api, postgres, redis, storage, mailpit) về healthy ≤ 3 phút trên máy sạch; mặc định `PAYMENTS_MODE=fake` | Đồng hồ từ `up` tới khi mọi healthcheck xanh | Docker Compose | M1, DOC-61 |

Hai thuộc tính bổ sung đo bằng cùng cách và không có mã riêng: giao diện mua vé có JS ban đầu ≤ 200 KB gzip cho trang sự kiện (DOC-38), p95 các endpoint đọc phổ biến nêu ở từng `E-xx` (DOC-37).

## 3. Mã lỗi của yêu cầu

Các `code` dùng trong tiêu chí nghiệm thu thuộc bảng ở SDD gốc 12.3 và DR-64: `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `SEATS_UNAVAILABLE`, `INSUFFICIENT_CAPACITY`, `RESERVATION_NOT_ACTIVE`, `REVISION_CONFLICT`, `VALIDATION_FAILED`, `IDEMPOTENCY_KEY_REUSED`, `IDEMPOTENCY_KEY_REQUIRED`, `RATE_LIMITED`, `QUEUE_REQUIRED`, `OVERLOADED`, `LOGIN_LINK_INVALID`, `ORGANIZER_PROFILE_REQUIRED`, `ORGANIZER_EXISTS`, `EVENT_NOT_ON_SALE`, `ACTIVE_RESERVATION_EXISTS`, `PAYMENT_WINDOW_TOO_SHORT`, `PAYMENT_ALREADY_SUCCEEDED`, `EVENT_STATE_CONFLICT`, `STALE_EVENT_VERSION`, `TICKET_TYPE_IN_USE`, `CAPACITY_BELOW_USED`, `MAP_LOCKED_AFTER_SALE`, `MAP_PUBLISH_BUSY`, `MAP_ALREADY_EXISTS`, `PAYLOAD_TOO_LARGE`, `PUBLISH_PRECONDITIONS_FAILED`, `MAP_VALIDATION_FAILED`, `TICKET_TYPE_LIMIT_REACHED`, `MEDIA_INVALID`, `INTERNAL_ERROR`, `PAYMENT_PROVIDER_UNAVAILABLE`, `EMAIL_PROVIDER_UNAVAILABLE`. Lỗi trường 422 dùng `rule` của DR-25 (`required`, `too_long`, `must_be_future`, …, `too_many_units`).

## 4. Ma trận FR → UC → tính năng → thiết kế

Mã tính năng `F-…` định nghĩa ở [DOC-05](feature-catalog.md); UC ở [DOC-04](use-cases.md); tài liệu thiết kế theo master plan §3.2.

| FR | UC | Tính năng | Tài liệu thiết kế | Màn hình |
| --- | --- | --- | --- | --- |
| FR-01 | UC-01 | F-AUTH-01, F-AUTH-02 | DOC-19, DOC-32, DOC-83 | DOC-44, DOC-52 |
| FR-02 | UC-02, UC-07, UC-09 | F-EVT-01…05, F-STU-01…03 | DOC-20, DOC-84, DOC-85 | DOC-42, 43, 54…56, 58, 59 |
| FR-03 | UC-08 | F-MAP-01, 02, 04, 05, 10 | DOC-21, DOC-22, DOC-88 | DOC-57 |
| FR-04 | UC-08 | F-MAP-03 | DOC-21, DOC-22 | DOC-57 |
| FR-05 | UC-08, UC-14 | F-MAP-06, 07 | DOC-16, DOC-21, DOC-23, DOC-88, DOC-89 | DOC-57 |
| FR-06 | UC-03 | F-INV-01, 02 | DOC-24, DOC-14, DOC-85, DOC-89 | DOC-46, DOC-47 |
| FR-07 | UC-03, UC-06, UC-11 | F-INV-03, 04 | DOC-24, DOC-26, DOC-85, DOC-87 | DOC-48 |
| FR-08 | UC-03, UC-04, UC-05, UC-06 | F-INV-05 | DOC-25, DOC-85 | — |
| FR-09 | UC-04, UC-15, UC-16, UC-19 | F-PAY-01…05 | DOC-26, DOC-09, DOC-87 | DOC-48, DOC-49 |
| FR-10 | UC-04, UC-05 | F-TKT-01…03, F-PAY-06 | DOC-27, DOC-51, DOC-85 | DOC-49, DOC-50, DOC-51 |
| FR-11 | UC-12 | F-ADM-01…05 | DOC-28, DOC-17, DOC-90 | DOC-45 |
| FR-12 | UC-18, UC-21 | F-OPS-01, 02 | DOC-30, DOC-66, DOC-86 | — |
| FR-13 | UC-07 | F-AUTH-03 | DOC-19, DOC-84 | DOC-53 |
| FR-14 | UC-13, UC-19 | F-EVT-06, F-PAY-03, F-TKT-04 | DOC-20, DOC-26, DOC-65, DOC-84, DOC-87 | DOC-59 |
| FR-15 | UC-09, UC-14 | F-MAP-09, F-EVT-05 | DOC-20, DOC-23, DOC-89 | DOC-57, DOC-59 |
| FR-16 | UC-12 | F-ADM-03 | DOC-28, DOC-90 | DOC-45 |
| FR-17 | UC-20 | F-AUTH-04, F-OPS-07 | DOC-31, DOC-40, DOC-83 | mọi màn |
| FR-18 | UC-07 | F-EVT-08 | DOC-15, DOC-20, DOC-84 | DOC-55 |
| FR-19 | UC-17 | F-EVT-07 | DOC-20, DOC-27, DOC-84 | DOC-55, DOC-51 |
| FR-20 | UC-10 | F-STU-04, F-MAP-11 | DOC-29, DOC-89 | DOC-60 |
| FR-21 | UC-22 | F-MAP-08 | DOC-16, DOC-23, DOC-88 | DOC-57 |

NFR → tài liệu thiết kế: NFR-01 DOC-24, DOC-14, DOC-30; NFR-02 DOC-10, DOC-28, DOC-70; NFR-03 DOC-24; NFR-04 DOC-25, DOC-26; NFR-05 DOC-26, DOC-30; NFR-06 DOC-22; NFR-07 DOC-19, DOC-32; NFR-08 DOC-62, DOC-61.

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: không có DR mới. Tiền tố test (`AU-`, `EV-`, `INVT-`, `IDEM-`, `PAY-`, `TN-`, `ADM-`, `MV-`, `GEO-`, `AV-`, `IC-`, `SEC-`) đã dành trước ở master plan §0.4. Số thứ tự đã gán trong cột Kiểm chứng (`AU-01`…`AU-15`, `EV-01`…`EV-27`, `INVT-01`…`INVT-15`, `IDEM-01`…`IDEM-06`, `PAY-01`…`PAY-14`, `TN-01`…`TN-07`, `ADM-01`…`ADM-13`) là số bắt buộc: mục "Test bắt buộc" của tài liệu thiết kế tương ứng (DOC-19, 20, 24, 25, 26, 27, 28) phải có đúng các dòng đó, được thêm số sau nhưng không đổi số; DOC-69 lập danh mục. Mã `xx` còn để trống (`GEO-xx`, `MV-xx`, `AV-xx`, `IC-xx`, `SEC-xx`) do tài liệu thiết kế (DOC-21, 23, 29, 30, 32) gán số.
