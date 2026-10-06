# Danh mục tính năng

> Trạng thái: **Review** · Cập nhật: 2026-10-06 · DOC-05
> Phụ thuộc: [DOC-01](vision-and-scope.md), [DOC-03](requirements.md), [DOC-04](use-cases.md), [DOC-06](../02-glossary.md), [Master plan](../00-master-plan.md) §4.1, §4.3, §5, [Sổ quyết định](../00-decision-register.md)
> Người dùng chính: P0-11; lập kế hoạch từng phase; quyết định cắt giảm khi trễ milestone

Danh mục liệt kê mọi tính năng của giai đoạn này theo nhóm, kèm yêu cầu, use case, phase hoàn thành, mức ưu tiên, cờ cấu hình, ứng dụng chạy nó và bước trong thứ tự cắt giảm. Mô tả hành vi nằm ở [DOC-03](requirements.md) và [DOC-04](use-cases.md); thiết kế nằm ở tài liệu thiết kế theo ma trận ở DOC-03 §4.

## 0. Quy ước cột

| Cột | Ý nghĩa |
| --- | --- |
| ID | `F-<NHÓM>-xx`: `AUTH` xác thực và tài khoản, `EVT` sự kiện và loại vé, `MAP` sơ đồ, `INV` kho vé và giữ vé, `PAY` thanh toán, `TKT` vé và thông báo, `ADM` kiểm soát tiếp nhận, `STU` giao diện studio, `OPS` vận hành và thực nghiệm. Mã đã cấp không đổi, không tái dùng |
| FR, UC | Yêu cầu (DOC-03) và use case (DOC-04) tương ứng |
| Phase | Phase **hoàn thành** tính năng (P1…P7). Tính năng nhiều phase ghi từng phần: "P2 (GA), P5 (SEAT/ZONE)" |
| MoSCoW | M Must · S Should · C Could · W Won't (để sau, ghi để giữ chỗ). Cột này là mức của chính tính năng; yêu cầu liên quan có mức riêng ở DOC-03 |
| Cờ | Cờ cấu hình hoặc profile bật/tắt tính năng: `PAYMENTS_MODE`, `inventory.strategy`, `high_demand` (theo event), profile `fake-payments`, `experiment`, `invariants`, `obs`, `stripe` (DOC-34) |
| App | Nơi chạy: `web` (SPA React), `nginx`, `api` (gồm job nền), `postgres`, `redis`, `storage`, `ci`, `tooling` (`make`, k6, runner) |
| Bước cắt | Số bước của thứ tự cắt giảm (§6): 1…5; `—` là không có trong thứ tự cắt; **không cắt** là hạng mục không được cắt (DOC-01 §8) |

## 1. Tổng quan

| Nhóm | Số tính năng | P1 | P2 | P3 | P4 | P5 | P6 | P7 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `F-AUTH` | 4 | 3 | 1 | | | | | |
| `F-EVT` | 8 | | 7 | 1 | | | | |
| `F-MAP` | 11 | | | | 9 | 2 | | |
| `F-INV` | 6 | | 6 | | | | | |
| `F-PAY` | 6 | | 1 | 5 | | | | |
| `F-TKT` | 4 | 1 | 2 | 1 | | | | |
| `F-ADM` | 5 | | | | | | 5 | |
| `F-STU` | 4 | | 3 | | | 1 | | |
| `F-OPS` | 7 | 4 | 3 | | | | | |
| **Tổng** | **55** | **8** | **23** | **7** | **9** | **3** | **5** | **0** |

Đếm theo phase **đầu tiên** giao tính năng (cột Phase của từng bảng ghi cả các phase sau, ví dụ "P2 (GA), P5 (SEAT/ZONE)"). Phase P7 chỉ gom việc hoàn thiện (E2E, rà tiếp cận, dữ liệu demo, chạy lại EXP) và không thêm tính năng mới.

## 2. F-AUTH: xác thực và tài khoản

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-AUTH-01 | Đăng nhập magic link | Xin link (supersede, giới hạn 3/email/15 phút, 10/IP/giờ), gửi SMTP trực tiếp sau commit, verify bằng câu ghi có điều kiện, trang callback không tiêu thụ token khi tải | FR-01 | UC-01 | P1 | M | — | web, api, postgres | F-OPS-03 | không cắt |
| F-AUTH-02 | Session, CSRF, đăng xuất | Cookie `tb_session`, cache Caffeine 60 giây, CSRF synchronizer, hết hạn 30 ngày không hoạt động, `GET /me` | FR-01 | UC-01 | P1 | M | `auth.cookie-secure` | api, postgres | F-AUTH-01 | không cắt |
| F-AUTH-03 | Hồ sơ tổ chức | `POST /organizer`, một tài khoản một tổ chức, kiểm tra sở hữu trả 404, `ORGANIZER_PROFILE_REQUIRED` | FR-13 | UC-07 | P2 | M | — | web, api, postgres | F-AUTH-02 | không cắt |
| F-AUTH-04 | Ngôn ngữ giao diện của tài khoản | Bộ chọn `vi`/`en`, cookie `tb_lang`, `PATCH /me`, email và Problem Details theo locale | FR-17 | UC-20 | P1 | M | — | web, api | F-AUTH-02, F-OPS-07 | không cắt |

## 3. F-EVT: sự kiện và loại vé

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-EVT-01 | Danh sách và chi tiết sự kiện công khai | `GET /events` (cursor), `GET /events/{id}`, `displayStatus`, 6 biến thể bán, đếm ngược `UPCOMING` | FR-02 | UC-02 | P2 | M | — | web, api, nginx | F-EVT-04 | không cắt |
| F-EVT-02 | Soạn thông tin sự kiện | Tên, mô tả, địa điểm, múi giờ IANA, giờ, khung mở bán, tiền tố mã vé, cờ phòng chờ; `rowVersion`; lỗi theo ô | FR-02 | UC-07 | P2 | M | `high_demand` | web, api, postgres | F-AUTH-03 | không cắt |
| F-EVT-03 | Loại vé và giá | Tối đa 5 loại, ba mô hình, giá 0 hoặc trong khoảng, màu tự gán, đổi sức chứa và giá khi đang bán | FR-02 | UC-07 | P2 (GA), P5 (SEAT/ZONE) | M | — | web, api, postgres | F-EVT-02 | không cắt |
| F-EVT-04 | Xuất bản và mở bán | `publish-checks`, một transaction tạo pool và unit (`FOR UPDATE`, `statement_timeout` 30 giây), `EventLifecycleJob` chuyển `ENDED` | FR-02 | UC-09 | P2 (GA), P5 (có sơ đồ) | M | — | web, api, postgres | F-EVT-03, F-INV-01 | không cắt |
| F-EVT-05 | Tạm dừng, mở lại, đóng bán sớm | `pause`, `resume`, `close-sale` (không mở lại được) | FR-02, FR-15 | UC-09 | P2 | S | — | web, api | F-EVT-04 | — |
| F-EVT-06 | Hủy sự kiện kể cả khi đã bán | `FOR UPDATE` event, đơn `PAID → REFUND_PENDING`, vé `VOID`, reservation `EXPIRING`, đua với webhook bằng `FOR SHARE` | FR-14 | UC-13 | P2 (hủy), P3 (đua với webhook) | M | — | web, api, postgres | F-EVT-04, F-INV-03 | không cắt |
| F-EVT-07 | Email đổi giờ, địa điểm | Fan-out outbox `EMAIL_EVENT_CHANGED` mỗi đơn `PAID`, `notifiedOrders` | FR-19 | UC-17 | P3 | S | — | api | F-EVT-02, F-TKT-02 | — |
| F-EVT-08 | Ảnh sự kiện | `POST /organizer/media` (S3), `GET /media/{id}` qua `proxy_cache`, giới hạn 2 MB, kiểm tra magic bytes | FR-18 | UC-07 | P2 | S | `STORAGE_S3_*` | web, api, storage, nginx | F-EVT-02 | — |

## 4. F-MAP: sơ đồ chỗ ngồi

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-MAP-01 | Hàng ghế theo đường | Rải N ghế cách đều theo độ dài cung; thẳng và cung tròn (M), gấp khúc và cong tự do (S); popover số ghế; kiểm tra khoảng cách tối thiểu, kéo dài đường | FR-03 | UC-08 | P4 | M (thẳng, cung) · S (gấp khúc, bezier) | — | web | — | 2 (gấp khúc, cong tự do) |
| F-MAP-02 | Đánh số, nhãn hàng, nhân bản song song, khối ghế | `sequential`/`odd-even-center`, nhãn A…Z AA…, nhân hàng cung đồng tâm, công cụ Khối ghế | FR-03 | UC-08 | P4 | M (đánh số, nhân hàng) · S (khối ghế) | — | web | F-MAP-01 | 4 (khối ghế) |
| F-MAP-03 | Khu vực bằng shape | Chữ nhật, ellipse, đa giác; sức chứa người nhập; nhãn polylabel | FR-04 | UC-08 | P4 | M | — | web | — | — |
| F-MAP-04 | Chỉnh sửa trong editor | Biến đổi, sửa điểm điều khiển, sửa từng ghế (loại vé, `accessible`, `blocked`, ghi đè số), chọn nhiều, undo 200 bước | FR-03 | UC-08 | P4 | M | — | web | F-MAP-01 | — |
| F-MAP-05 | Tự lưu bản nháp | Debounce 2 giây, `revision`, 409 `REVISION_CONFLICT`, thử lại 2…30 giây, IndexedDB | FR-03 | UC-08 | P4 | M | — | web, api, postgres | F-MAP-04 | — |
| F-MAP-06 | Validate dùng chung client và server | 12 quy tắc, mã cố định, fixture chung TS/Java, JTS ở server, < 500 ms | FR-05 | UC-08 | P4 | M | — | web, api | F-MAP-03 | — |
| F-MAP-07 | Phiên bản bất biến | `seat_map_version` có checksum JCS + SHA-256, trigger bất biến, danh sách phiên bản | FR-05 | UC-08 | P4 | M | — | api, postgres | F-MAP-06 | — |
| F-MAP-08 | Nhân bản sơ đồ | Sao chép toàn bộ, sinh lại mọi ID, ánh xạ loại vé theo tên, `cloned_from_seat_map_id` | FR-21 | UC-22 | P4 | S | — | web, api | F-MAP-07 | — |
| F-MAP-09 | Khóa sơ đồ và dựng lại kho vé trước giờ mở bán | Khóa từ `sale_starts_at`; trước giờ đó xuất bản phiên bản mới dựng lại kho vé (`lock_timeout` 5 giây) | FR-15 | UC-14 | P5 | S | — | web, api, postgres | F-MAP-07, F-EVT-04 | 3 (khóa ngay từ lúc xuất bản) |
| F-MAP-10 | Hiệu năng render, ảnh nền, trang trí | Một Konva custom shape cho ghế, rbush, mức chi tiết theo zoom, ảnh nền, sân khấu, lối vào, nhãn | FR-03 | UC-08 | P4 | M (hiệu năng) · S (ảnh nền, trang trí) | — | web | F-MAP-04 | 4 (ảnh nền) |
| F-MAP-11 | Trình xem sơ đồ và tình trạng chỗ | Renderer chỉ đọc dùng chung `map-core`, bitmap `held`/`sold`, cache Caffeine 2 giây, hỏi mỗi ~4 giây, chế độ Danh sách | FR-20 | UC-02 | P5 | M | — | web, api | F-MAP-07, F-INV-01 | — |

## 5. Các nhóm còn lại

### 5.1 F-INV: kho vé và giữ vé

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-INV-01 | Kho vé một dòng mỗi vé | Bảng `inventory_unit`, pool GA/ZONE, unit ghế có nhãn và `seat_index`, ràng buộc CHECK, index một phần | FR-06 | UC-03 | P2 (GA), P5 (SEAT/ZONE) | M | `inventory.strategy` | api, postgres | — | không cắt |
| F-INV-02 | Giữ vé nguyên tử | Claim `SKIP LOCKED` cho ghế và pool trong một transaction, tất cả hoặc không, giới hạn 50 unit, một reservation mở mỗi người | FR-06 | UC-03 | P2 (GA), P5 (SEAT/ZONE) | M | `inventory.strategy` | api, postgres | F-INV-01, F-INV-05 | không cắt |
| F-INV-03 | Hết hạn và trả vé | Job 5 giây, lô 200, lease 30 giây, `EXPIRING` làm trọng tài, thủ tục trả vé dùng chung | FR-07 | UC-11 | P2 (không PaymentIntent), P3 (có PaymentIntent) | M | — | api, postgres | F-INV-02 | không cắt |
| F-INV-04 | Hủy giữ vé đường nhanh | `DELETE /reservations/{id}` chạy luôn thủ tục trả vé; 202 khi Stripe lỗi | FR-07 | UC-06 | P2 (chưa PaymentIntent), P3 | M | — | web, api | F-INV-03 | — |
| F-INV-05 | Idempotency | `Idempotency-Key` bắt buộc ở 4 endpoint, băm body thô, chèn key đầu transaction, `Idempotent-Replayed` | FR-08 | UC-03 | P2 | M | — | api, postgres | — | không cắt |
| F-INV-06 | Đổi sức chứa và giá khi đang bán | Tăng thêm unit, giảm bằng `SKIP LOCKED` tới số đang dùng, `CAPACITY_BELOW_USED` | FR-02 | UC-07 | P2 | S | — | api, postgres | F-INV-01 | — |

### 5.2 F-PAY: thanh toán

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-PAY-01 | PaymentIntent | Port `PaymentGateway`, tham số thẻ/VND, giao thức `FOR SHARE` chống đua với job trả vé | FR-09 | UC-04 | P3 | M | `PAYMENTS_MODE` | web, api | F-INV-03 | không cắt |
| F-PAY-02 | Webhook và transaction xác nhận | Chữ ký 300 giây, loại trùng `stripe_event`, `FOR SHARE` event, `PAID`, phát hành vé | FR-09 | UC-04 | P3 | M | `PAYMENTS_MODE` | api, postgres | F-PAY-01 | không cắt |
| F-PAY-03 | Thanh toán đến trễ, chờ hoàn tiền | `REFUND_PENDING` (`LATE_PAYMENT`, `AMOUNT_MISMATCH`, `EVENT_CANCELLED`), `REFUNDED` qua RB-01 | FR-09, FR-14 | UC-15, UC-19 | P3 | M | — | api, postgres | F-PAY-02 | không cắt |
| F-PAY-04 | Đối chiếu khi webhook thất lạc | `PaymentReconcileJob` 60 giây, 50 order mỗi vòng | FR-09 | UC-16 | P3 | M | — | api | F-PAY-02 | — |
| F-PAY-05 | Cổng thanh toán giả | `FakePaymentGateway`, bảng `fake_payment_intent`, webhook ký HMAC, endpoint điều khiển, form giả `VITE_PAYMENTS=fake` | FR-09 | UC-04 | P3 | M | `PAYMENTS_MODE=fake`, profile `fake-payments` | api, web | F-PAY-01 | không cắt |
| F-PAY-06 | Đơn 0 đồng | `confirm-free` chạy transaction xác nhận không qua Stripe | FR-10 | UC-05 | P2 | M | — | web, api | F-INV-02 | không cắt |

### 5.3 F-TKT: vé và thông báo

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-TKT-01 | Phát hành vé và mã vé | Một `ticket` mỗi unit, mã `PP-XXXX-XXXX`, `ticket_unit_issued_uq`, `VOID` khi sự kiện hủy | FR-10 | UC-05 | P2 | M | — | api, postgres | F-PAY-06 | không cắt |
| F-TKT-02 | Outbox và email vé | `OutboxRelay` 1 giây, lease, backoff, `EMAIL_TICKETS`, Thymeleaf theo locale, `Message-ID` cố định | FR-10 | UC-05 | P1 (relay), P2 (email vé) | M | — | api, postgres | F-OPS-03 | không cắt |
| F-TKT-03 | Vé của tôi | `GET /me/tickets`, `GET /me/orders`, nhóm theo sự kiện, trạng thái chờ hoàn tiền, vé `VOID` | FR-10 | UC-05 | P2 | M | — | web, api | F-TKT-01 | — |
| F-TKT-04 | Email chờ hoàn tiền | Mẫu `refund-pending` theo `refund_reason`, `support.email`, `support.refund-sla-text` | FR-10 | UC-15, UC-13 | P3 | M | — | api | F-TKT-02, F-PAY-03 | — |

### 5.4 F-ADM: kiểm soát tiếp nhận

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-ADM-01 | Rate limit nginx | `api_ip` 20 r/s, `auth_ip` 10 r/phút, `hold_ip` 5 r/s, allowlist cho máy sinh tải, cache edge | FR-11 | UC-12 | P6 | M | `RATE_LIMIT_ALLOWLIST` | nginx | — | — |
| F-ADM-02 | Token bucket theo người dùng | `token_bucket.lua`, 3 bucket, bỏ qua khi mất Redis | FR-11 | UC-03 | P6 | M | — | api, redis | F-ADM-01 | — |
| F-ADM-03 | Phòng chờ có rời hàng | `POST/GET/DELETE …/queue`, `admit.lua`, `AdmissionTicker`, xáo prequeue, `ZSCORE admitted` ở lệnh giữ vé, 6 trạng thái | FR-11, FR-16 | UC-12 | P6 | S | `high_demand` | web, api, redis | F-ADM-02 | 1 |
| F-ADM-04 | Cờ hết vé | `soldout:pool:{p}` TTL 30 giây, kiểm tra ghế từ snapshot | FR-11 | UC-03 | P6 | S | — | api, redis | F-ADM-02 | — |
| F-ADM-05 | Bulkhead và connection pool | 24 permit, pool 40, `statement_timeout` 2 giây, 503 `OVERLOADED` | FR-11 | UC-03 | P6 | M | `hold.bulkhead.permits` | api | — | — |

### 5.5 F-STU: giao diện studio

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-STU-01 | Tổng quan studio | Thẻ sự kiện theo trạng thái với số đã bán, đang giữ, còn trống | FR-02, FR-20 | UC-07, UC-10 | P2 | M | — | web, api | F-AUTH-03 | — |
| F-STU-02 | Soạn sự kiện theo bước | Thanh bước, lưu thủ công, hộp thoại thay đổi chưa lưu, bước Sơ đồ ẩn khi chỉ GA | FR-02 | UC-07 | P2 | M | — | web | F-EVT-02, F-EVT-03 | — |
| F-STU-03 | Xem trước và xuất bản | Xem trước máy tính/điện thoại, danh sách điều kiện, hộp thoại xuất bản/hủy/đóng bán | FR-02, FR-14 | UC-09, UC-13 | P2 (GA), P5 (có sơ đồ) | M | — | web | F-EVT-04 | — |
| F-STU-04 | Theo dõi bán vé | `sales`, `seat-status`, sơ đồ tô theo trạng thái, làm mới 15 giây | FR-20 | UC-10 | P5 | S | — | web, api | F-MAP-11 | — |

### 5.6 F-OPS: vận hành và thực nghiệm

| ID | Tính năng | Mô tả | FR | UC | Phase | MoSCoW | Cờ | App | Phụ thuộc | Bước cắt |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| F-OPS-01 | Kiểm tra bất biến | `InvariantChecker` 5 phút, `make invariants` (JSON, thoát 0/1), danh mục `INV-xx` | FR-12 | UC-18 | P2 | M | profile `invariants` | api, tooling | F-INV-01 | không cắt |
| F-OPS-02 | Thực nghiệm EXP-01…10 | `make exp EXP=xx`, seed 100.000 người dùng, k6, `CounterClaimer`, `NaiveClaimer`, bảng kết quả | FR-12 | UC-21 | P2…P6 | M | profile `experiment`, `inventory.strategy` | tooling, api | F-OPS-01 | 5 (EXP-08 chỉ một kịch bản); EXP-01…04, 06, 07 không cắt |
| F-OPS-03 | Compose, seed, reset | `docker compose up`, healthcheck, `.env.example` (`PAYMENTS_MODE=fake`), `make dev/seed/reset` | — (NFR-08) | — | P1 | M | `PAYMENTS_MODE` | tooling, nginx, api, postgres, redis, storage, mailpit | — | không cắt |
| F-OPS-04 | Observability profile `obs` | Log JSON ECS, metric Prometheus, Grafana với dashboard EXP-05 | — (NFR-02) | — | P1 (log, metric), P6 (dashboard) | S | profile `obs` | api, tooling | F-OPS-03 | 4 (dashboard Grafana) |
| F-OPS-05 | RetentionJob | Dọn `login_token`, `session`, `idempotency_key`, `stripe_event`, `outbox`, `media` không tham chiếu | — (DR-74) | — | P2 | S | — | api | F-OPS-03 | — |
| F-OPS-06 | CI | `ci.yml`: lint, test, integration, kiểm tra `schema.d.ts`, `oasdiff`, `i18n:check`, build image | — (DR-08) | — | P1 | M | — | ci | — | không cắt |
| F-OPS-07 | Hạ tầng đa ngôn ngữ | i18next, `MessageSource` `vi`/`en`, `pnpm i18n:check`, định dạng số/tiền/ngày theo locale và múi giờ sự kiện | FR-17 | UC-20 | P1 | M | — | web, api | — | không cắt |

## 6. Thứ tự cắt giảm theo tính năng

Kích hoạt khi một milestone trễ quá 50% ước lượng; cắt bước 1 trước rồi mới tới bước sau (DOC-01 §8, master plan §4.3).

| Bước | Cắt gì | Tính năng và phần bị ảnh hưởng | Task bỏ | Còn lại |
| --- | --- | --- | --- | --- |
| 1 | Phòng chờ | F-ADM-03 và phần phòng chờ của FR-16 | P6-05, P6-07 | F-ADM-01, F-ADM-02, F-ADM-04, F-ADM-05; sự kiện `high_demand` bị bulkhead và rate limit bảo vệ |
| 2 | Gấp khúc và cong tự do | F-MAP-01 (phần gấp khúc, bezier) | phần tương ứng của P4-06 | Đường thẳng và cung tròn |
| 3 | Sửa sơ đồ sau khi xuất bản sự kiện | F-MAP-09 (dựng lại kho vé) | P5-05.1 | Khóa sơ đồ ngay từ lúc xuất bản sự kiện thay vì từ `sale_starts_at` |
| 4 | Khối ghế, ảnh nền, Grafana | F-MAP-02 (khối ghế), F-MAP-10 (ảnh nền), F-OPS-04 (dashboard) | phần tương ứng của P4-07; P6-08 | Số liệu EXP-05 đọc từ Prometheus hoặc log |
| 5 | EXP-08 rút gọn | F-OPS-02 (EXP-08) | khởi động lại PostgreSQL, tắt Redis | Chỉ kịch bản "dừng API giữa transaction" |

**Không được cắt:** P2, P3 (chứng minh NEVER OVERSELL), F-OPS-01, EXP-01…04, EXP-06, EXP-07, giao diện `vi`/`en` (F-AUTH-04, F-OPS-07). Cột "Bước cắt" ở các bảng trên chép đúng bảng này.

## 7. Cờ tính năng và profile

| Cờ | Giá trị | Ảnh hưởng | Tính năng | Nguồn |
| --- | --- | --- | --- | --- |
| `PAYMENTS_MODE` | `fake` (mặc định), `stripe` | Chọn adapter `PaymentGateway`; form giả ở frontend | F-PAY-01, F-PAY-05, F-OPS-03 | DR-51, DR-72 |
| `inventory.strategy` | `skip-locked` (mặc định), `counter`, `naive` | Bản cài đặt `InventoryClaimer`; khác `skip-locked` cần profile `experiment` | F-INV-01, F-INV-02, F-OPS-02 | DR-76 |
| `event.high_demand` | `true`/`false` mỗi event | Phòng chờ trước giờ mở bán và xáo ngẫu nhiên; 503 khi mất Redis | F-ADM-03, F-EVT-02 | DR-57 |
| Profile `fake-payments` | bật/tắt | Bảng `fake_payment_intent`, endpoint điều khiển; từ chối khởi động nếu đi cùng `sk_live_…` | F-PAY-05 | DR-51 |
| Profile `experiment` | bật/tắt | `CounterClaimer`, `NaiveClaimer`, seed 100.000 người dùng, allowlist rate limit | F-OPS-02 | DR-75, DR-76 |
| Profile `invariants` | bật/tắt | Chạy `InvariantChecker` một lần rồi thoát | F-OPS-01 | DR-73 |
| Profile `obs` | bật/tắt | Prometheus, Grafana, cổng quản trị 9090 | F-OPS-04 | DR-09, DR-72 |
| Profile `stripe` | bật/tắt | Container `stripe-cli`; dùng khóa `sk_test_…` | F-PAY-01 | DR-72 |
| `auth.cookie-secure` | `true` (mặc định), `false` ở profile `dev` | Cookie `Secure` (Safari không nhận trên `http://localhost`) | F-AUTH-02 | DR-22 |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: không có DR mới.
