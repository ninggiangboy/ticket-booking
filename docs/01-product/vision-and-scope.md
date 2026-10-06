# Tầm nhìn và phạm vi

> Trạng thái: **Review** · Cập nhật: 2026-10-06 · DOC-01
> Phụ thuộc: SDD gốc §1–§3, §16–§17, [Sổ quyết định](../00-decision-register.md) (DR-13, 28, 37, 41, 44, 57, 68, 75), [Master plan](../00-master-plan.md) §4.3, [DOC-06](../02-glossary.md)
> Người dùng chính: P0-09, P0-10; mọi người mới đọc dự án; tài liệu DOC-03, DOC-05 khi cần "vì sao"

Tài liệu nói dự án làm gì, vì sao, đo thành công bằng gì và không làm gì. Yêu cầu chi tiết nằm ở [DOC-03](requirements.md), người dùng ở [DOC-02](personas-and-journeys.md), use case ở [DOC-04](use-cases.md), danh mục tính năng và thứ tự cắt giảm theo tính năng ở [DOC-05](feature-catalog.md). Thuật ngữ có định nghĩa ở [DOC-06](../02-glossary.md).

## 1. Bối cảnh

Bán vé sự kiện là bài toán phân phối một lượng hàng hữu hạn, không thể bổ sung, cho lượng người mua có thể lớn hơn hàng chục lần trong vài phút đầu mở bán (SDD gốc 1.1). Mỗi sự kiện có cách bố trí khác nhau: nhà hát bán từng ghế, sân vận động bán theo khu đứng, hội thảo bán vé vào cửa tự do. Người tổ chức cần tự dựng sơ đồ, tự mở bán và theo dõi số vé đã bán mà không nhờ đội kỹ thuật.

Dự án là hệ thống đặt vé sự kiện chạy trên một máy bằng Docker Compose, làm từ đầu đến cuối với hai mục đích: chứng minh bằng thực nghiệm đo được rằng lõi giữ vé không bán vượt dưới tải cao, và có một trình tạo sơ đồ chỗ ngồi dùng được. Người làm là một lập trình viên; dự án cũng để học (DR-06).

### 1.1 Bốn vấn đề thực tế (SDD gốc 1.2)

| # | Vấn đề | Hậu quả | Phần thiết kế trả lời |
| --- | --- | --- | --- |
| 1 | **Bán vượt và đặt trùng ghế.** Hàng nghìn request cùng tranh một ghế hoặc một kho vé; kiểm tra rồi mới ghi (check-then-act) cho hai người cùng một chỗ | Lỗi không sửa được sau khi đã thu tiền | Một dòng mỗi vé, claim bằng `SKIP LOCKED`, mọi chuyển trạng thái là câu ghi có điều kiện (ADR-0003, DOC-24) |
| 2 | **Vé bị giữ mà không ai mua.** Người dùng chọn ghế rồi bỏ đi | Kho vé cạn giả trong khi ghế vẫn trống | Giữ vé 10 phút, job trả vé, trạng thái `EXPIRING` (DR-42, ADR-0004) |
| 3 | **Tiền và vé lệch nhau.** Thanh toán thành công nhưng vé đã hết hạn giữ, webhook đến hai lần, client retry sau timeout | Vé không có tiền hoặc tiền không có vé | Idempotency, loại trùng webhook, `REFUND_PENDING`, đối chiếu (DR-44, DR-45, DR-49) |
| 4 | **Sơ đồ chỗ ngồi khó dựng.** Nhập ghế bằng bảng tính không thể hiện hàng cong, khán đài hình quạt, khu đứng đa giác | Người tổ chức phải nhờ kỹ thuật; sửa sơ đồ khi đã bán dễ làm hỏng kho vé | Trình tạo sơ đồ trên canvas, phiên bản bất biến, khóa sơ đồ từ giờ mở bán (DR-37, ADR-0009) |

## 2. Tầm nhìn: hai lớp giá trị

1. **Dựng sự kiện không cần kỹ thuật.** Vẽ một đường, nhập số ghế, hệ thống tự rải ghế; vẽ một shape để tạo khu vực; dùng lại sơ đồ của sự kiện khác bằng nhân bản (DR-31).
2. **Bán vé đúng tuyệt đối.** Mỗi ghế có tối đa một chủ, mỗi kho vé không bao giờ âm, mỗi đơn thu tiền đúng một lần, kể cả khi 100.000 người cùng tranh 5.000 vé.

Lớp thứ ba, dòng tiền minh bạch cho người tổ chức (phí, số dư, rút tiền), thuộc giai đoạn sau (mục 5).

## 3. Bài toán cốt lõi

> Làm thế nào để quản lý một kho vé hữu hạn dưới lượng request đồng thời rất lớn mà không bao giờ bán vượt, không đặt trùng ghế, và tiền thu được luôn khớp với vé đã phát hành — kể cả khi request bị gửi lại, tiến trình dừng đột ngột hoặc dịch vụ thanh toán phản hồi trễ?

Kịch bản mục tiêu để thiết kế và kiểm chứng: **100.000 người dùng đồng thời tranh 5.000 vé** của một sự kiện, hoặc cùng tranh một ghế A1 (SDD gốc 2.1). "Đồng thời" theo nghĩa của DR-75: có mặt trong cùng cửa sổ mở bán.

Yêu cầu đúng đắn đứng trên mọi yêu cầu khác:

```
NEVER OVERSELL
```

Hệ quả thiết kế: mọi bất biến ép tại database; Redis chỉ giảm tải, không quyết định; khi sự cố, hệ thống nghiêng về giữ vé lâu hơn hoặc từ chối bán, không bao giờ về phía bán trùng (SDD gốc 4.2).

### 3.1 Mức đầu tư theo module

| Module | Vai trò | Mức độ đầu tư |
| --- | --- | --- |
| Inventory và reservation | Trọng tâm kỹ thuật: claim nguyên tử, giữ vé có thời hạn, idempotency, chống bán vượt dưới tải | Đào sâu, có thực nghiệm đo |
| Seat map editor | Trọng tâm sản phẩm: vẽ hàng ghế theo đường, zone theo shape, validate, phiên bản | Đào sâu |
| Checkout và thanh toán Stripe | Nối giữ vé với tiền thật: PaymentIntent, webhook, tranh chấp thanh toán với hết hạn | Triển khai chắc chắn |
| Chịu tải | Rate limit, cờ hết vé, phòng chờ | Làm sau khi lõi đúng |
| Phát hành vé | Vé điện tử có mã duy nhất, gửi qua email | Mức cơ bản |
| Xác thực | Chỉ magic link | Mức cơ bản |

## 4. Mục tiêu đo được

Mỗi mục tiêu có chỉ tiêu, chỉ số và bằng chứng. Con số tải là **mục tiêu thiết kế** (planned); chúng được hiệu chỉnh theo máy thực nghiệm ở S-06 (DR-75). Kết quả đo (measured) chỉ được ghi vào tài liệu thực nghiệm, kèm ngày, máy và git SHA.

| # | Mục tiêu | Chỉ tiêu | NFR | Bằng chứng |
| --- | --- | --- | --- | --- |
| G1 | Không bán vượt | 0 vé vượt sức chứa, 0 ghế có hai chủ trong mọi thực nghiệm; `make invariants` sạch sau mỗi lần chạy | NFR-01 | EXP-01, EXP-02, EXP-04, EXP-08; chạy thêm bản `naive` để thấy lỗi xuất hiện khi thiếu cơ chế |
| G2 | Chịu tải mở bán | 100.000 người dùng đồng thời tranh 5.000 vé; không sập; p95 của request được tiếp nhận < 500 ms (hoặc trần đo ở S-06 giữ tỉ lệ 20:1) | NFR-02 | EXP-02 (với phòng chờ), EXP-05, S-06 |
| G3 | Trả vé nhanh | Vé hết hạn về kho trong ≤ 30 giây sau `expires_at` khi job chạy | NFR-03 | EXP-04 |
| G4 | Idempotency | Gửi lại cùng key N lần chỉ tạo đúng một reservation và một lần thu tiền | NFR-04 | EXP-03, test IDEM, test PAY |
| G5 | Tiền khớp vé | Mỗi đơn `PAID` có đúng một giao dịch Stripe thành công và đủ vé; không vé nào thiếu đơn `PAID`; thanh toán đến trễ không phát hành vé | NFR-05 | EXP-06, EXP-07, `INV-xx` |
| G6 | Editor mượt | Sơ đồ 10.000 ghế kéo, zoom ≥ 30 fps (p5) trên máy chuẩn của DR-39 | NFR-06 | EXP-09, S-04 |
| G7 | Bảo mật cơ bản | Không lưu dữ liệu thẻ; token đăng nhập và session chỉ lưu dạng hash; không hard-code secret | NFR-07 | Test payload outbox, quét secret trong CI, test SEC |
| G8 | Tái lập môi trường | Một lệnh `docker compose up` đưa mọi container về healthy ≤ 3 phút trên máy sạch | NFR-08 | M1, [DOC-61](../09-operations/local-dev.md) |
| G9 | Dùng được bằng hai ngôn ngữ | Toàn bộ giao diện và email có `vi` và `en`; không chuỗi cứng | FR-17 | `pnpm i18n:check`, E2E hai locale |

Chỉ tiêu đầy đủ, cách đo và công cụ của NFR-01…08 ở [DOC-03](requirements.md) §NFR.

## 5. Phạm vi

### 5.1 Trong phạm vi

- Mỗi event là đúng một show (một địa điểm, một khung giờ).
- Ba mô hình kho vé: `SEAT`, `ZONE`, `GA`; một sơ đồ có thể chứa cả ghế lẫn khu vực.
- Trình tạo sơ đồ trên canvas cho người tổ chức; trình xem và chọn ghế cho người mua.
- Đăng nhập bằng magic link; hai vai trò `BUYER` và `ORGANIZER`; hồ sơ tổ chức (FR-13).
- Giữ vé có thời hạn 10 phút, idempotency, kiểm soát tiếp nhận (phòng chờ có rời hàng, FR-16).
- Thanh toán thẻ qua Stripe (VND, test mode); toàn bộ tiền vào tài khoản Stripe của nền tảng.
- Vé điện tử gửi qua email sau khi thanh toán; email đổi lịch và email chờ hoàn tiền.
- Hủy hoặc tạm dừng sự kiện kể cả khi đã bán, đóng bán sớm, khóa sơ đồ từ giờ mở bán (FR-14, FR-15).
- Giao diện `vi` và `en`, đa múi giờ sự kiện, ảnh sự kiện (FR-17, FR-18, DR-12, DR-38).
- Triển khai đơn giản: một ứng dụng API, PostgreSQL, Redis, object storage S3, chạy bằng Docker Compose.
- Mười thực nghiệm EXP-01…10 và lệnh kiểm tra bất biến.

### 5.2 Để sau (thuộc sản phẩm, chưa làm ở giai đoạn này)

| Hạng mục | Nội dung | Giai đoạn này xử lý thế nào |
| --- | --- | --- |
| Tài chính | Phí nền tảng (dự kiến 5% giá vé), sổ cái, số dư của người tổ chức | Đơn `PAID` lưu đủ số tiền để tính lại về sau; chưa tính phí |
| Rút tiền | Người tổ chức yêu cầu rút; nền tảng chuyển khoản | Chưa có |
| Soát vé | QR trên vé, quét vào cửa | Vé có mã duy nhất; chưa có QR và màn hình quét |
| **Hoàn tiền tự động** | Hoàn khi hủy sự kiện đã bán, khi thanh toán đến trễ, khi lệch số tiền | Đơn sang `REFUND_PENDING` kèm email; người vận hành hoàn tay trên Stripe Dashboard theo RB-01 rồi ghi `REFUNDED` (DR-28, DR-44) |
| Admin | Console giám sát, đình chỉ sự kiện, xử lý rút tiền | Chưa có vai trò admin; người vận hành dùng SQL theo runbook |
| Vận hành | Alerting, backup, chạy nhiều bản sao | Log JSON, metric Prometheus ở profile `obs`, lệnh kiểm tra bất biến |
| Thành viên nhiều người trong một tổ chức | Mời, phân quyền | Một tài khoản một tổ chức, một tổ chức một chủ (DR-23) |

### 5.3 Ngoài phạm vi

| Hạng mục | Lý do |
| --- | --- |
| Sự kiện nhiều suất hoặc nhiều ngày trong một event | Đổi mô hình kho vé và sơ đồ; không phục vụ mục tiêu của dự án |
| Định giá động, mã giảm giá, khuyến mãi | Làm phức tạp việc chụp giá; không thuộc bài toán không bán vượt |
| Đổi vé, bán lại, chuyển nhượng vé | Cần danh tính vé và chính sách; ngoài phạm vi |
| Đăng nhập bằng mật khẩu, mạng xã hội, xác thực hai lớp | Magic link đủ cho giai đoạn này và không có mật khẩu để lộ |
| Đa tiền tệ, thuế, hóa đơn điện tử | Chỉ VND, gán cứng (DR-13) |
| Ứng dụng di động riêng | Trang mua vé ưu tiên điện thoại qua trình duyệt (DR-69) |
| **Giới hạn số vé mỗi đơn hoặc mỗi người, chống đầu cơ theo số lượng** | Owner bỏ ngày 2026-10-06 (DR-41). Chỉ còn giới hạn kỹ thuật 50 unit mỗi lệnh giữ vé và một reservation mở mỗi người mỗi event; lớp hạn chế duy nhất là rate limit và phòng chờ. Rủi ro ghi ở master plan §8 |
| **Dark mode** | Design system "Vé giấy" v0.1 chỉ có một bộ màu sáng (DR-68) |
| Sửa sơ đồ sau giờ mở bán (diff phiên bản đang bán) | Bỏ khi chốt DR-37: sơ đồ khóa toàn bộ từ `sale_starts_at`; muốn đổi bố cục thì hủy sự kiện và nhân bản sơ đồ |
| Backpressure tự động (AIMD) cho nhịp cấp lượt, token vào cửa ký, rate limiter dự phòng khi mất Redis | Đơn giản hóa theo DR-56, DR-58, DR-60 vì dự án để học và chạy một máy |

## 6. Giả định

| # | Giả định | Nguồn | Nếu sai |
| --- | --- | --- | --- |
| A1 | Một máy chạy cả hệ thống, một bản sao API | SDD gốc 14.1 | Job và cache trong tiến trình phải đổi sang dùng chung (Redis); các job đã viết để chạy an toàn trên nhiều bản sao |
| A2 | Stripe ở test mode, tiền tệ VND; tài khoản đăng ký ở nước Stripe hỗ trợ, không cần kích hoạt | DR-13 | S-02 nâng `payment.min-amount`; vẫn chỉ VND |
| A3 | `PAYMENTS_MODE=fake` là mặc định của `.env.example`; Stripe thật chỉ bật bằng `make up-stripe` | DR-72, DR-51 | `docker compose up` cần khóa Stripe, vi phạm NFR-08 |
| A4 | Số liệu tải đo trên một máy; đọc kết quả theo tương quan với baseline, không phải hiệu năng production | SDD gốc 17, DR-75 | — |
| A5 | Người mua dùng trình duyệt hiện đại có JavaScript; email đến được hộp thư (Mailpit ở dev) | SDD gốc 5 | Magic link chậm: session 30 ngày, nhắc đăng nhập trước giờ mở bán |
| A6 | Tên sản phẩm "ticket" là tên tạm, lấy từ `APP_NAME` | DR-54, DR-68 | Đổi một biến cấu hình |
| A7 | Chỉ dùng cho dev và demo; không bán vé thật trước khi làm phần vận hành | SDD gốc 17 | — |

## 7. Ràng buộc

| # | Ràng buộc | Tác động |
| --- | --- | --- |
| C1 | **Một người làm.** Ước lượng 20,5 tuần toàn thời gian, nhân 2 nếu bán thời gian (master plan §4.1) | Chỉ viết tài liệu gate của phase kế tiếp; tài liệu P4–P7 viết song song với code |
| C2 | **Tài nguyên máy thực nghiệm.** RAM ≥ 16 GB, Docker, giới hạn container cố định ghi ở DOC-70 | 100.000 người dùng ảo có thể không đạt; S-06 chốt trần và hiệu chỉnh NFR-02 bằng DR mới |
| C3 | Stack khóa: Java 25 + Spring Boot 4, PostgreSQL 18, Redis 8.2, React 19 (DR-02…04) | Lùi về Java 21 + Boot 3.5 nếu S-01 thất bại, không trộn |
| C4 | Một nguồn chuẩn: PostgreSQL; không transaction phân tán | Không chia thành nhiều service |
| C5 | Không lưu dữ liệu thẻ; secret nằm trong `.env` không commit | NFR-07 |
| C6 | Dự án để học: cấu trúc code n-layer quen thuộc, ưu tiên cách đơn giản khi hai cách cùng đúng (DR-06, DR-21, DR-58) | Quyết định đơn giản hóa đã chốt, không bàn lại |

## 8. Thứ tự cắt giảm

Kích hoạt khi một milestone trễ quá 50% ước lượng. Cắt theo thứ tự, bước trước rồi mới tới bước sau (master plan §4.3; tính năng tương ứng ở [DOC-05](feature-catalog.md) §6):

1. Phòng chờ: bỏ P6-05, P6-07 (giữ rate limit, cờ hết vé, bulkhead).
2. Kiểu đường gấp khúc và cong tự do trong editor (giữ thẳng và cung tròn).
3. Sửa sơ đồ sau khi xuất bản sự kiện: bỏ P5-05.1, khóa sơ đồ ngay từ lúc xuất bản thay vì từ giờ mở bán.
4. Công cụ khối ghế và ảnh nền; dashboard Grafana P6-08.
5. EXP-08 chỉ chạy kịch bản "dừng API giữa transaction".

**Không được cắt:** P2, P3 (chứng minh NEVER OVERSELL), lệnh kiểm tra bất biến, EXP-01…04, EXP-06, EXP-07, giao diện `vi`/`en`.

## 9. Sản phẩm bàn giao ở mỗi milestone

| Milestone | Có thể chạy được |
| --- | --- |
| M1 | `docker compose up`, đăng nhập magic link qua Mailpit bằng `vi` và `en`, CI xanh |
| M2 | Tạo và xuất bản sự kiện GA từ studio, giữ vé, hết hạn, nhận vé 0 đồng kèm email; EXP-02, 03, 04, 10 |
| M3 | Mua vé GA bằng thẻ test; thanh toán đến trễ ra `REFUND_PENDING`; EXP-06, 07 |
| M4 | Vẽ, validate, xuất bản sơ đồ 180 ghế + 1 zone bằng giao diện; EXP-09 |
| M5 | Mua ghế cụ thể và vé khu vực; hai trình duyệt tranh một ghế; EXP-01 |
| M6 | Phòng chờ với trần S-06; p95 < 500 ms; EXP-05, 08 |
| M7 | E2E xanh cả hai locale; 10 thực nghiệm trên bản cuối; kịch bản demo 6 bước; tag `v1.0.0` |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: không có DR mới.
