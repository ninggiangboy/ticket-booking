# Persona và hành trình

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-02
> Phụ thuộc: SDD gốc §3.1, §13, §16.1, [DOC-01](vision-and-scope.md), [DOC-06](../02-glossary.md), [Sổ quyết định](../00-decision-register.md) (DR-23, 41, 44, 57, 67, 69, 70, 72)
> Người dùng chính: P0-10, P0-11; [DOC-03](requirements.md), [DOC-04](use-cases.md); tác giả các màn hình DOC-42…60

Tài liệu mô tả năm persona và hành trình của từng người qua màn hình và endpoint. Tên persona chỉ để minh họa, không phải người thật. Mỗi hành trình `J-x` đánh số bước để màn hình và test dẫn lại ("J-2 bước 4–6"). Endpoint ghi dạng `METHOD /đường dẫn` (tiền tố `/api/v1`, DR-63); mã `E-xx` gán ở DOC-37. Hành vi từng bước nằm ở use case ([DOC-04](use-cases.md)) và luồng chi tiết `FL-xx` (DOC-82); tài liệu này chỉ nêu điểm chạm.

## 1. Tổng quan

| ID | Persona | Vai trò | Thiết bị chính | Trình độ kỹ thuật | Tần suất |
| --- | --- | --- | --- | --- | --- |
| PS-1 | Người mua trên điện thoại (ưu tiên) | `BUYER` | Điện thoại 390 px | Thấp–trung bình | Vài lần mỗi năm; dồn vào giờ mở bán |
| PS-2 | Người mua trên máy tính trong đợt mở bán đông | `BUYER` | Laptop/desktop | Trung bình–cao | Ít, nhưng đúng giây mở bán |
| PS-3 | Người tổ chức nhà hát (sơ đồ ghế) | `ORGANIZER` | Laptop ≥ 1024 px | Trung bình | Vài sự kiện mỗi tháng |
| PS-4 | Người tổ chức hội thảo (chỉ GA) | `ORGANIZER` | Laptop | Thấp–trung bình | Vài sự kiện mỗi quý |
| PS-5 | Người vận hành/nghiên cứu | Chạy lệnh và SQL, không có giao diện admin | Terminal | Cao | Mỗi phase, mỗi thực nghiệm |

PS-1 là persona ưu tiên: trang mua vé thiết kế cho 390 px trước (SDD gốc 13.2, DR-69). PS-5 không là vai trò trong hệ thống (không có `ADMIN`, DR-23); người này thao tác bằng `make`, Stripe Dashboard và SQL.

## 2. Chi tiết persona

### PS-1 · Người mua trên điện thoại: "Linh, sinh viên, mua vé hòa nhạc trên đường đi học"

- **Mục tiêu:** Có vé sự kiện mình thích ở mức giá chấp nhận được; xong trong vài phút, bằng một tay.
- **Nỗi đau:** Chữ nhỏ, sơ đồ khó chạm; sợ hết giờ giữ vé khi đang nhập thẻ; email đăng nhập đến chậm đúng lúc mở bán; mạng di động chập chờn khiến request lặp.
- **Cần từ hệ thống:** Chọn chỗ bằng chạm; giữ lựa chọn khi phải đăng nhập giữa chừng (DR-67); đồng hồ đếm ngược đáng tin (DR-66); biết rõ khi ghế vừa bị người khác lấy mà không mất cả đơn; thanh toán thẻ một lần; vé và mã vé trong email và trong "Vé của tôi".
- **Không cần:** Tạo mật khẩu; chọn múi giờ; tính năng tổ chức.
- **Màn hình:** DOC-42 Danh sách sự kiện, DOC-43 Sự kiện, DOC-44 Đăng nhập, DOC-46 Chọn chỗ (bản M05) hoặc DOC-47 Chọn số lượng, DOC-48 Thanh toán, DOC-49 Kết quả, DOC-50 Vé của tôi, DOC-51 Email.

### PS-2 · Người mua trên máy tính trong đợt mở bán đông: "Quân, đã canh giờ mở bán, muốn ghế hàng đầu"

- **Mục tiêu:** Giữ được ghế cụ thể ngay trong những giây đầu mở bán, thanh toán trước khi hết giờ.
- **Nỗi đau:** Hàng nghìn người tranh cùng ghế; trang treo hoặc trắng khi quá tải; không biết mình đứng ở đâu trong hàng; ghế đổi màu sau khi bấm.
- **Cần từ hệ thống:** Phòng chờ công bằng (đến sớm hay muộn trước giờ mở bán đều như nhau, DR-57); vị trí và ước tính chờ; sơ đồ pan/zoom mượt; nhận 409 rõ ràng ("Hàng C · Ghế 10 vừa có người giữ") và chọn lại ngay; tự thử lại khi "Đang rất đông".
- **Không cần:** Tính năng studio; bảo đảm có vé (hệ thống chỉ bảo đảm không bán trùng).
- **Màn hình:** DOC-43 Sự kiện (đếm ngược, nhắc đăng nhập trước), DOC-44, DOC-45 Phòng chờ, DOC-46 Chọn chỗ, DOC-48, DOC-49, DOC-50.

### PS-3 · Người tổ chức nhà hát: "Hà, quản lý nhà hát nhỏ, bán 180 ghế và khu đứng 300 chỗ"

- **Mục tiêu:** Dựng đúng sơ đồ nhà hát (hàng cong, ghế cho xe lăn, ghế chặn), đặt giá theo hạng, mở bán đúng giờ và theo dõi số vé.
- **Nỗi đau:** Bảng tính không vẽ được hàng cong; dựng lại sơ đồ cho mỗi sự kiện; sợ sửa sơ đồ làm lệch vé đã bán; lỡ nhập sai giờ hoặc địa điểm sau khi đã bán.
- **Cần từ hệ thống:** Editor vẽ cung, nhập số ghế, nhân bản song song; validate có chỉ dẫn; xem trước "Người mua sẽ thấy"; "Dùng lại sơ đồ từ sự kiện khác" (DR-31); danh sách điều kiện xuất bản; biết sơ đồ khóa từ giờ mở bán (DR-37); số đã bán/đang giữ/còn trống, sơ đồ tô theo trạng thái (DR-71); hủy sự kiện kèm báo trước số đơn chờ hoàn tiền (DR-28).
- **Không cần:** Quy trình rút tiền, báo cáo tài chính (để sau); soát vé tại cổng.
- **Màn hình:** DOC-53 Lập hồ sơ, DOC-54 Tổng quan, DOC-55 Thông tin, DOC-56 Loại vé, DOC-57 Editor, DOC-58 Xem trước, DOC-59 Xuất bản, DOC-60 Theo dõi bán vé.

### PS-4 · Người tổ chức hội thảo (chỉ GA): "Minh, tổ chức hội thảo thiết kế, ba hạng vé vào cửa"

- **Mục tiêu:** Tạo sự kiện nhanh, đặt vài loại vé với sức chứa, mở bán, không phải vẽ sơ đồ.
- **Nỗi đau:** Công cụ quá nặng so với nhu cầu; muốn biết còn bao nhiêu vé mỗi hạng.
- **Cần từ hệ thống:** Luồng ngắn: thông tin → loại vé → xem trước → xuất bản (bước Sơ đồ ẩn khi chỉ có GA, DR-70); giá 0 cho vé miễn phí; đóng bán sớm khi đủ người (DR-24); đổi giờ mà người mua nhận email báo (DR-29).
- **Không cần:** Seat map editor, phòng chờ (thường không bật `high_demand`).
- **Màn hình:** DOC-53…56, DOC-58, DOC-59, DOC-60.

### PS-5 · Người vận hành/nghiên cứu: "An, chạy thực nghiệm và xử lý đơn chờ hoàn tiền"

- **Mục tiêu:** Chứng minh bằng số đo rằng không có bán vượt; xử lý đơn `REFUND_PENDING` và sai lệch bất biến.
- **Nỗi đau:** Số đo không lặp lại được; không biết đơn nào cần hoàn; sai lệch bất biến không có chỉ dẫn điều tra.
- **Cần từ hệ thống:** `make exp EXP=xx` lặp lại được (reset, seed, k6, `make invariants`); `make invariants` in JSON, thoát 0/1 (DR-73); runbook RB-01 (hoàn tiền thủ công), RB-02 (sai lệch bất biến), RB-03 (reservation kẹt, webhook thất lạc); cổng thanh toán giả với endpoint điều khiển; metric và dashboard ở profile `obs`.
- **Không cần:** Giao diện admin (chưa có); quyền `ADMIN`.
- **Màn hình:** không có; terminal, Stripe Dashboard, Mailpit, Grafana.

## 3. Hành trình

Cảm xúc dùng ba mức: 🙂 tích cực, 😐 trung tính, 😟 lo lắng. Endpoint thuộc tiền tố `/api/v1`.

### J-1 · PS-1 mua vé GA trên điện thoại

| Bước | Hành động | Điểm chạm (màn hình · endpoint) | Cảm xúc | Cơ hội |
| --- | --- | --- | --- | --- |
| 1 | Mở trang chủ, xem sự kiện | DOC-42 · `GET /events` | 🙂 | Ảnh 16:9 hoặc tem ngày; nhãn "Đang mở bán" |
| 2 | Mở một sự kiện, xem loại vé và giá | DOC-43 · `GET /events/{id}` | 🙂 | "Giá từ"; "Cách mua" 3 bước |
| 3 | Chọn 2 vé "Phổ thông" | DOC-47 · bộ tăng giảm | 🙂 | Tổng tạm tính; không hiện giới hạn 50 như quy tắc bán (DR-41) |
| 4 | Bấm giữ vé, chưa đăng nhập → đăng nhập | DOC-44 · `POST /auth/magic-link` | 😟 | Lựa chọn giữ trong `localStorage` (DR-67); nhắc kiểm tra hộp thư |
| 5 | Mở email, bấm link | DOC-51 · `POST /auth/verify` | 😐 | Quay về `returnTo` còn nguyên lựa chọn |
| 6 | Vào phòng chờ (nếu đông) hoặc `ADMITTED` ngay | DOC-45 · `POST /events/{id}/queue` | 😐 | Hàng trong suốt khi còn chỗ (DR-57) |
| 7 | Giữ vé | DOC-47 · `POST /events/{id}/reservations` (`Idempotency-Key`) | 🙂 | Retry cùng key khi mất mạng |
| 8 | Thanh toán: nhập thẻ (hoặc "Nhận vé" nếu 0 đồng) | DOC-48 · `POST /orders/{id}/payment-intent` hoặc `POST /orders/{id}/confirm-free` | 😟 | Đồng hồ 10 phút theo giờ server (DR-66); cảnh báo < 2 phút |
| 9 | Chờ xác nhận, xem vé | DOC-49 · `GET /orders/{id}` (nhịp DR-50) | 🙂 | "Đang xác nhận thanh toán"; thẻ vé có mã |
| 10 | Nhận email vé, xem lại ở "Vé của tôi" | DOC-51, DOC-50 · `GET /me/tickets` | 🙂 | Mã vé `PP-XXXX-XXXX` |

### J-2 · PS-2 mua ghế trong đợt mở bán đông (gồm phòng chờ và tranh ghế)

| Bước | Hành động | Điểm chạm | Cảm xúc | Cơ hội |
| --- | --- | --- | --- | --- |
| 1 | Đăng nhập trước giờ mở bán (email có thể đến chậm) | DOC-43 (nhắc đăng nhập), DOC-44 | 😐 | Session 30 ngày; phòng chờ chỉ nhận người đã đăng nhập |
| 2 | Vào phòng chờ trước giờ (event `high_demand`) | DOC-45 · `POST /events/{id}/queue` → `PRE_QUEUE` | 😟 | Đến sớm 30 phút hay 1 phút như nhau; xáo ngẫu nhiên lúc mở bán |
| 3 | Chờ, thấy vị trí và ước tính | DOC-45 · `GET /events/{id}/queue` theo `retryAfterSeconds` | 😟 | Rời hàng được (`DELETE /events/{id}/queue`); mất kết nối tự thử lại trong 2 phút |
| 4 | Tới lượt (`ADMITTED`), chuyển tự động sang chọn chỗ | DOC-46 · đồng hồ lượt vào 5 phút | 🙂 | Lượt kéo dài tới `expires_at` khi giữ vé |
| 5 | Xem sơ đồ, chọn ghế hàng đầu | DOC-46 · `GET /events/{id}/map?version=n`, `GET /events/{id}/availability` mỗi ~4 giây | 🙂 | Bitmap nhỏ; dừng hỏi khi tab ẩn |
| 6 | Giữ vé; một ghế vừa bị người khác lấy | DOC-46 · `POST /events/{id}/reservations` → 409 `SEATS_UNAVAILABLE` | 😟 | Bỏ ghế khỏi đơn, "Hàng C · Ghế 10 vừa có người giữ", chọn lại ngay |
| 7 | Hệ thống quá tải → tự thử lại | DOC-46 · 503 `OVERLOADED` | 😟 | "Đang rất đông", thử lại sau 3 giây cùng key |
| 8 | Giữ được; thanh toán trong 10 phút | DOC-48 · `POST /orders/{id}/payment-intent` | 😟 | Thẻ bị từ chối thì thử thẻ khác trong thời hạn |
| 9 | Thẻ đã trừ nhưng hết hạn giữ (hiếm) | DOC-49 biến thể 3 · `GET /orders/{id}` → `REFUND_PENDING` | 😟 | "Bạn sẽ được hoàn lại toàn bộ" kèm email (DR-44) |
| 10 | Nhận vé | DOC-49, DOC-50, DOC-51 | 🙂 | — |

### J-3 · PS-3 dựng sự kiện có sơ đồ và mở bán

| Bước | Hành động | Điểm chạm | Cảm xúc | Cơ hội |
| --- | --- | --- | --- | --- |
| 1 | Đăng nhập, lập hồ sơ tổ chức | DOC-44, DOC-53 · `POST /organizer` | 😐 | Tên bắt buộc; email liên hệ tùy chọn |
| 2 | Tạo sự kiện, nhập thông tin, ảnh, múi giờ | DOC-55 · `POST /organizer/events`, `PATCH /organizer/events/{id}` (`rowVersion`), `POST /organizer/media` | 😐 | Lỗi theo ô; khung "Người mua sẽ thấy" |
| 3 | Thêm loại vé VIP/Thường/Sinh viên (SEAT) và khu đứng (ZONE) | DOC-56 · `POST /organizer/events/{id}/ticket-types` | 😐 | Màu loại vé tự gán; giới hạn 5 loại |
| 4 | Vẽ một cung, nhập 20 ghế, nhân song song thành 10 hàng; vẽ zone đa giác sức chứa 1.000 | DOC-57 · `GET /organizer/events/{id}/map`, `PUT /organizer/maps/{id}/draft` | 🙂 | Popover số ghế; tự lưu 2 giây; hoặc "Dùng lại sơ đồ từ sự kiện khác" (`POST /organizer/maps/{id}/clone`) |
| 5 | Gán loại vé, sửa vấn đề validate, xuất bản sơ đồ | DOC-57 · `POST /organizer/maps/{id}/validate`, `POST /organizer/maps/{id}/publish` | 😟 | Danh sách vấn đề click tới đối tượng |
| 6 | Xem trước máy tính/điện thoại | DOC-58 | 🙂 | "Bản xem trước không giữ vé và không thu tiền" |
| 7 | Kiểm tra điều kiện và xuất bản sự kiện | DOC-59 · `GET /organizer/events/{id}/publish-checks`, `POST /organizer/events/{id}/publish` | 😟 | Đang xuất bản tới khi response về (≤ 60 giây) |
| 8 | Theo dõi số vé và sơ đồ theo trạng thái | DOC-60 · `GET /organizer/events/{id}/sales`, `GET /organizer/events/{id}/seat-status` (15 giây) | 🙂 | Sơ đồ đã khóa từ giờ mở bán (DR-37) |
| 9 | Đổi giờ hoặc địa điểm sau khi bán | DOC-55 · `PATCH` → `notifiedOrders` | 😟 | Email đổi lịch tới người đã mua |
| 10 | Đóng bán sớm hoặc hủy sự kiện | DOC-59 · `POST …/close-sale`, `POST …/cancel` | 😟 | Hộp thoại hủy báo số đơn sẽ chờ hoàn tiền |

### J-4 · PS-4 tạo sự kiện chỉ GA

| Bước | Hành động | Điểm chạm | Cảm xúc | Cơ hội |
| --- | --- | --- | --- | --- |
| 1 | Lập hồ sơ, tạo sự kiện, nhập thông tin | DOC-53, DOC-55 | 😐 | Luồng ngắn |
| 2 | Thêm 3 loại vé GA (một loại giá 0) với sức chứa | DOC-56 · `POST /organizer/events/{id}/ticket-types` | 🙂 | Bước Sơ đồ ẩn (DR-70) |
| 3 | Xem trước, kiểm tra điều kiện | DOC-58, DOC-59 | 🙂 | Danh sách điều kiện rõ |
| 4 | Xuất bản; sự kiện sang `UPCOMING` rồi `ON_SALE` đúng giờ | DOC-59 · `POST …/publish`; `EventLifecycleJob` | 🙂 | Pool GA tạo trong một transaction |
| 5 | Theo dõi số vé mỗi hạng; đóng bán sớm khi đủ | DOC-60, DOC-59 | 🙂 | Giảm sức chứa không dưới số đang giữ và đã bán (DR-30) |

### J-5 · PS-5 chạy thực nghiệm và hoàn tiền thủ công

| Bước | Hành động | Điểm chạm | Cảm xúc | Cơ hội |
| --- | --- | --- | --- | --- |
| 1 | Chạy một thực nghiệm | `make exp EXP=02` (reset, seed 100.000 người dùng, profile `experiment`, k6) | 😐 | 3 lần lặp, báo trung vị và min–max (DR-75) |
| 2 | Kiểm tra bất biến | `make invariants` → JSON, thoát 0 hoặc 1 | 😟 | Đỏ thì loại lần chạy; RB-02 chỉ cách điều tra |
| 3 | Đọc số đo | Grafana (profile `obs`), bảng kết quả trong tài liệu EXP | 🙂 | — |
| 4 | Liệt kê đơn `REFUND_PENDING` theo lý do | RB-01 (SQL) | 😟 | Truy vấn theo sự kiện kèm `payment_intent_id` |
| 5 | Hoàn trên Stripe Dashboard, ghi kết quả | RB-01 (`UPDATE … WHERE status = 'REFUND_PENDING'`) | 😐 | CHECK chặn `REFUNDED` thiếu `refund_reference` |
| 6 | Điều tra reservation kẹt, webhook thất lạc | RB-03 (stripe-cli, đối chiếu) | 😟 | `PaymentReconcileJob` tự chạy mỗi 60 giây |

## 4. Bản đồ persona → use case

| Persona | UC chính |
| --- | --- |
| PS-1, PS-2 | UC-01, UC-02, UC-03, UC-04, UC-05, UC-06, UC-12, UC-20 |
| PS-3 | UC-01, UC-07, UC-08, UC-09, UC-10, UC-13, UC-14, UC-17, UC-22 |
| PS-4 | UC-01, UC-07, UC-09, UC-10, UC-13, UC-17 |
| PS-5 | UC-18, UC-19, UC-21 |
| Hệ thống | UC-11, UC-15, UC-16 |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: không có DR mới.
