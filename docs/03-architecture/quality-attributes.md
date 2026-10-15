# Thuộc tính chất lượng

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-10
> Phụ thuộc: SDD gốc §3.4, §10, §14, §15, [DOC-01](../01-product/vision-and-scope.md) §4 (mục tiêu đo được), [DOC-03](../01-product/requirements.md) (NFR-01…08), [DOC-07](system-context-and-containers.md), [DOC-08](data-flows.md), [DOC-14](../05-data/domain-model.md) §4.4, §5, [DOC-33](../06-design/observability.md) §4, [DOC-34](../06-design/configuration-reference.md), [DOC-62](../09-operations/deploy-compose.md) §4, §9, [Sổ quyết định](../00-decision-register.md) (DR-09, 17, 27, 39, 41, 42, 45, 46, 55, 56, 57, 59, 60, 61, 62, 75, 124, 152)
> Người dùng chính: P2-13 (hạ tầng thực nghiệm), P6-xx (chịu tải), tác giả DOC-24, DOC-28, DOC-29, DOC-70 và các EXP; người đọc báo cáo khi so số đo với kế hoạch

Tài liệu nối mỗi yêu cầu phi chức năng NFR-01…08 với chiến thuật, cơ chế, nơi cài đặt và cách kiểm chứng; chia ngân sách độ trễ của lệnh giữ vé theo chặng; và ước lượng dung lượng (số dòng kho vé, tải phòng chờ, băng thông tình trạng chỗ, connection, tài nguyên container). Chỉ tiêu và tiêu chí nghiệm thu của NFR ở DOC-03; giao thức thực nghiệm ở DOC-70; tên metric ở DOC-33; giá trị cấu hình ở DOC-34.

**Mọi con số ở đây là kế hoạch** (planned), suy ra từ DDL, DR và công thức ghi kèm; chưa có số đo. S-03 (thời gian xuất bản, kích thước bảng) và S-06 (trần người dùng ảo) chạy trước P2-00 (DR-152); EXP-02, 04, 05, 10 đo phần còn lại. Số đo được ghi vào §8 kèm ngày, máy, git SHA và script; không sửa con số kế hoạch ở các mục khác.

## 1. NFR → chiến thuật → cơ chế → nơi cài đặt → kiểm chứng

| NFR | Chiến thuật | Cơ chế | Nơi cài đặt (module, class, khóa cấu hình) | Kiểm chứng |
| --- | --- | --- | --- | --- |
| NFR-01 Không bán vượt | Bất biến ép ở database; một nguồn sự thật; mọi đổi trạng thái có điều kiện | Một dòng mỗi vé, claim `UPDATE … FOR UPDATE SKIP LOCKED` (ADR-0003); `CHECK unit_owner_ck`, `reservation_open_uq`, `ticket_unit_issued_uq`; `EXPIRING` làm trọng tài giữa xác nhận và trả vé (ADR-0004); `FOR SHARE` trên reservation khi lưu PaymentIntent (DR-47) và trên event khi xác nhận (DR-28); Redis chỉ là gợi ý | `inventory.repository.SkipLockedClaimer` (`inventory.strategy=skip-locked`), `reservation.service.HoldService`, `ReleaseService`, `payment.service.ConfirmationService`, DDL DOC-14 §4.4; `invariant.InvariantChecker` (`invariant.interval` PT5M) | INVT-01, INVT-02, DM-12…15; EXP-01, 02, 04, 08 với `make invariants` sạch; bản `naive` phải cho thấy bán vượt; metric `ticket_invariant_violations` = 0 |
| NFR-02 Chịu tải | Chặn sớm, rẻ trước khi tới database; giới hạn đồng thời; trả 503 thay vì xếp hàng | Phòng chờ luôn chạy (`max_active` 500, `admit_rate` 50/giây, DR-57, DR-60); `limit_req` ở nginx (DR-55); token bucket theo người dùng (DR-56); cờ hết vé và bitmap chặn trước (DR-46, DR-62); bulkhead 24 permit < pool 40, `hold.bulkhead.wait` 0 (DR-61); `statement_timeout` 2 giây, `lock_timeout` 1 giây; transaction ngắn, không gọi ra ngoài; cache nginx cho `GET /events`, `GET /events/{id}` 5 giây | nginx (DOC-62 §8); `admission` (`AdmissionTicker`, `admission.*`, `ratelimit.*`); `reservation.service.HoldService` (`hold.bulkhead.permits`, `hold.statement-timeout`, `hold.lock-timeout`); `spring.datasource.hikari.maximum-pool-size`, `connection-timeout` | S-06, EXP-02 (với phòng chờ), EXP-05; `ticket_hold_duration_seconds` p95 < 0,5 giây; `ticket_hold_result_total`; k6 `http_req_duration` |
| NFR-03 Thời hạn giữ vé | Job quét theo index một phần, lô có giới hạn, lease thay cho khóa dài | `ReleaseExpiredHoldsJob` nhịp 5 giây, lô 200, `FOR UPDATE SKIP LOCKED` trên `reservation_due_idx`; lease 30 giây cho `EXPIRING`; thủ tục trả vé dùng chung với đường nhanh (DR-42, DR-43) | `reservation.job.ReleaseExpiredHoldsJob`, `ReleaseService`; `reservation.expiry.interval`, `.batch-size`, `.lease` | EXP-04, INVT-12, INVT-13; `ticket_expiry_lag_seconds` p100 ≤ 30 giây |
| NFR-04 Idempotency | Khóa idempotency trong cùng transaction nghiệp vụ | `INSERT idempotency_key … ON CONFLICT DO NOTHING` là câu đầu, response lưu trước commit, chỉ lưu 2xx (DR-45); PaymentIntent idempotent theo `order_id` với khóa Stripe `pi-create:<orderId>` (DR-47); webhook loại trùng bằng `stripe_event` | `reservation.service.IdempotencyService`; `payment.service.PaymentIntentService`, `WebhookService` | IDEM-02…05, PAY-01; EXP-03 |
| NFR-05 Tiền khớp vé | Trạng thái đơn chỉ đổi theo webhook đã xác minh; tiền đến sau thì chờ hoàn tiền, không phát hành vé | Chữ ký webhook, dung sai 300 giây (DR-48); kiểm số tiền; xác nhận trong một transaction phát hành vé và ghi outbox; thanh toán trễ → `REFUND_PENDING` (DR-44); `PaymentReconcileJob` (DR-49) | `payment` (`WebhookService`, `ConfirmationService`, `PaymentReconcileJob`); `ticket.service.TicketService`; `notification` (outbox); `payment.reconcile.*` | EXP-06, EXP-07; INV-xx về đơn `PAID`/vé (DOC-30); `ticket_payment_webhook_total{outcome}` |
| NFR-06 Hiệu năng editor | Không render qua React cho từng ghế; vẽ theo mức chi tiết | Store Zustand ngoài React, ghế vẽ bằng một Konva custom shape, R-tree cho hit test, bitmap khi kéo (DR-39, ADR-0014) | `frontend/src/map-core`, `frontend/src/features/studio-map` | S-04, EXP-09 (p5 fps ≥ 30 với 10.000 ghế) |
| NFR-07 Bảo mật | Không giữ bí mật mà không cần; giảm bề mặt | Payment Element gửi thẻ thẳng tới Stripe; token đăng nhập và session lưu SHA-256; secret trong `.env`; không ghi email, token vào log (DR-21, DR-22, DR-99) | `auth` (`MagicLinkService`, `SessionService`); `common` (lọc log); `scripts/check-secrets.sh` | AU-01, SEC-xx (DOC-32); bước quét secret trong CI (DOC-63) |
| NFR-08 Tái lập môi trường | Một lệnh, mặc định không cần tài khoản ngoài | Compose có healthcheck mọi service, `PAYMENTS_MODE=fake` mặc định, Flyway chạy trước readiness, SeaweedFS tạo bucket lúc khởi động (DR-72, DR-125) | `deploy/compose/docker-compose.yml`, `.env.example`; `spring.flyway.*` | OPS-xx (DOC-61, 62); M1: healthy ≤ 3 phút |

Ba nguyên tắc nằm sau cả bảng: database quyết định, Redis chỉ giảm tải (ADR-0002); không gọi ra ngoài trong transaction (SDD gốc §10.4, DOC-08 §15); khi quá tải thì từ chối nhanh bằng 503 `OVERLOADED` thay vì để request chờ, nên p95 của **request được tiếp nhận** (NFR-02) gần với thời gian phục vụ chứ không phụ thuộc độ dài hàng chờ.

## 2. Ngân sách độ trễ của lệnh giữ vé

Lệnh `POST /events/{id}/reservations` (E-14) với 2 ghế, 3 vé khu vực và 2 vé GA, sự kiện có kiểm soát tiếp nhận, mọi container trên một máy (mạng loopback). Cột "p95 kế hoạch" là ngân sách cho từng chặng; cộng các p95 cho cận trên thận trọng của p95 toàn lệnh.

| # | Chặng | p95 kế hoạch | Lũy kế | Cơ sở |
| --- | --- | --- | --- | --- |
| 1 | `nginx`: nhận, `limit_req` `api_ip` + `hold_ip`, proxy tới `api` | 3 ms | 3 ms | DR-55; loopback |
| 2 | Bộ lọc của `api`: session từ Caffeine, CSRF, `Origin`, đọc JSON, Bean Validation (1–20 dòng, ≤ 50 unit) | 3 ms | 6 ms | DR-22, DR-41; không chạm DB |
| 3 | Redis: `EVALSHA token_bucket.lua` `rl:hold:{userId}` | 2 ms | 8 ms | DR-56; quá `spring.data.redis.timeout` 200 ms thì bỏ qua |
| 4 | Redis: `ZSCORE admitted:{e} <userId>` | 1 ms | 9 ms | DR-58 |
| 5 | Redis: cờ `soldout:pool:{p}` (một `MGET` cho mọi pool của lệnh) | 1 ms | 10 ms | DR-46 |
| 6 | Bitmap tình trạng ghế trong Caffeine (P5); trượt cache thì dựng lại ≤ 20 ms, mỗi sự kiện tối đa một lần mỗi 2 giây | 2 ms | 12 ms | DR-62 |
| 7 | Bulkhead `tryAcquire` | < 0,1 ms | 12 ms | DR-61; hết permit → 503 ngay |
| 8 | Lấy connection Hikari | 2 ms | 14 ms | Pool 40 > 24 permit nên không chờ; quá 1 giây → 503 |
| 9 | `BEGIN`, hai `SET LOCAL` | 1 ms | 15 ms | DOC-12 §2.4 |
| 10 | `INSERT idempotency_key … ON CONFLICT DO NOTHING` | 2 ms | 17 ms | DR-45 |
| 11 | `SELECT event` (không khóa) | 1 ms | 18 ms | PK |
| 12 | `INSERT reservation … ON CONFLICT DO NOTHING` | 2 ms | 20 ms | `reservation_open_uq` |
| 13 | Claim ghế (≤ 50 `seat_id`, `unit_seat_uq`) | 8 ms | 28 ms | DOC-14 §7 |
| 14 | Claim từng pool (ví dụ 2 pool, `unit_pool_available_idx`, `SKIP LOCKED`) | 10 ms | 38 ms | Tăng theo số dòng bị khóa phải bỏ qua; đo ở EXP-02, EXP-10 |
| 15 | Đọc giá, tên loại vé, nhãn; `INSERT reservation_item`, `INSERT orders` | 5 ms | 43 ms | DR-20 |
| 16 | `UPDATE idempotency_key SET response_…` | 2 ms | 45 ms | DR-45 |
| 17 | `COMMIT` (ghi WAL, `synchronous_commit = on`) | 6 ms | 51 ms | DR-61; SSD |
| 18 | Redis: `ZADD admitted:{e} GT` gia hạn lượt vào | 1 ms | 52 ms | DR-57 |
| 19 | Serialize JSON, ghi response, `nginx` trả về | 3 ms | 55 ms | |
| 20 | Dự phòng GC và lập lịch luồng | 20 ms | 75 ms | G1, heap 2,25 GB |

Phân bổ 500 ms của NFR-02:

| Phần | Ngân sách | Ghi chú |
| --- | --- | --- |
| Thời gian phục vụ ở `api` (chặng 2–20) | ≤ 120 ms | Khớp chỉ tiêu E-14 (DOC-37); kế hoạch trên là 72 ms |
| `nginx` và mạng loopback (chặng 1) | ≤ 10 ms | |
| Chờ trong hàng của `api` và chờ khóa dòng (`reservation_open_uq`, dòng unit chưa bị `SKIP LOCKED` bỏ qua) | ≤ 200 ms | Chặn trên bởi `lock_timeout` 1 giây; bulkhead không cho hàng chờ dài (§5) |
| Dư cho phía k6 và nhiễu đo | ≥ 170 ms | Máy sinh tải chạy cùng máy (DOC-70) |
| **Tổng** | **500 ms** | |

Nếu chặng 9–17 (một transaction) vượt 60 ms p95 trong EXP-02 thì đó là dấu hiệu tranh chấp khóa hoặc thiếu index; nếu chặng 3–5 vượt 10 ms thì Redis đang là điểm nghẽn. Hai nhóm này đo được riêng qua `ticket_connection_hold_seconds{op="hold"}` và `ticket_hold_duration_seconds` (DOC-33).

## 3. Kho vé: số dòng và kích thước

### 3.1 Kích thước một dòng `inventory_unit`

Theo DDL ở DOC-14 §4.4 (12 cột). Header dòng 23 byte + bitmap NULL 2 byte, căn 8 → 32 byte; mỗi dòng thêm 4 byte con trỏ trong trang. `uuid` căn 1 byte, `int` căn 4, `timestamptz` căn 8; `text` ngắn có 1 byte độ dài.

| Loại unit | Cột có giá trị | Dữ liệu | Dòng (gồm header) | Trong trang (+4) |
| --- | --- | --- | --- | --- |
| Pool (`ZONE`, `GA`) `AVAILABLE` | 4 `uuid` (64) + `status` `'AVAILABLE'` (10) → căn 8 = 80 + `updated_at` (8) | 88 | 120 | **124 byte** |
| Pool `HELD`/`SOLD` | như trên, `status` 5–6 byte, + `reservation_id` (16) → 88 + 8 | 96 | 128 | 132 byte |
| Ghế `AVAILABLE` | 4 `uuid` (64) + `seat_index` (4) + `section_name` "Khán đài A" (14) + `row_label` (2) + `seat_number` (3) + `status` (10) = 97 → căn 8 = 104 + 8 | 112 | 144 | **148 byte** |

Trang 8.192 byte, `fillfactor = 80` (DOC-14): lệnh chèn dừng khi trang còn dưới 20% chỗ trống, nên mỗi trang nhận khoảng 8.192 × 0,8 − 24 (header trang) ≈ **6.530 byte** khi xuất bản. 20% còn lại dành cho phiên bản dòng mới khi claim và trả vé (dòng đổi `status` cần chỗ trong cùng trang).

- Dòng pool mỗi trang: 6.530 / 124 ≈ **52**.
- Dòng ghế mỗi trang: 6.530 / 148 ≈ **44**.

### 3.2 Sự kiện 5.000 và 100.000 chỗ

| Sự kiện | Unit | Trang heap | Heap | Index (ước lượng, §3.3) | Tổng |
| --- | --- | --- | --- | --- | --- |
| GA 5.000 vé (EXP-02, M2) | 5.000 pool | 5.000 / 52 = 97 | 97 × 8 KB ≈ **0,8 MB** | ≈ 0,3 MB | ≈ 1,1 MB |
| 100.000 chỗ (S-03: 20.000 ghế + 80.000 pool) | 20.000 ghế + 80.000 pool | 20.000 / 44 + 80.000 / 52 = 455 + 1.539 = 1.994 | 1.994 × 8 KB ≈ **15,6 MB** | ≈ 7 MB | ≈ 23 MB |

Bảng `reservation`, `reservation_item`, `orders`, `ticket` nhỏ hơn nhiều: 5.000 vé với trung bình 2,5 vé mỗi đơn (k6 xin đều 1–4 vé) là khoảng 2.000 reservation và 2.000 đơn.

Hệ quả: kho vé của sự kiện lớn nhất nằm gọn trong `shared_buffers` 2 GB (DOC-62 §9), nên đường giữ vé không đọc đĩa. Chi phí thật nằm ở ghi: mỗi claim hoặc trả vé tạo một phiên bản dòng mới **không HOT**, vì `status` nằm trong điều kiện của các index một phần (`unit_pool_available_idx`, `unit_seat_taken_idx`, DR-17), nên mỗi lần đổi trạng thái chèn thêm mục vào mọi index của bảng. `autovacuum_vacuum_scale_factor = 0,02` cho bảng này (DOC-14) nghĩa là vacuum chạy sau khoảng 0,02 × 100.000 + 50 = 2.050 dòng chết ở sự kiện 100.000 chỗ, và sau 150 dòng chết ở sự kiện 5.000 vé. S-03 đo tác động này (DR-17, DR-27).

### 3.3 Index của `inventory_unit` (sự kiện 100.000 chỗ, khi bán hết)

Mục B-tree: 8 byte header + khóa, căn 8, + 4 byte con trỏ; lá `fillfactor` 90 → 7.350 byte dùng được mỗi trang. Index không duy nhất có khử trùng (PostgreSQL 13+) nên khóa lặp lại chỉ tốn ~6 byte mỗi dòng.

| Index | Mục | Số mục | Ước lượng |
| --- | --- | --- | --- |
| PK `unit_id` | 8 + 16 = 24, + 4 = 28 byte → 262 mục/trang | 100.000 | 382 trang ≈ 3,0 MB |
| `unit_seat_uq` `(event_id, seat_id)` | 8 + 32 = 40, + 4 = 44 byte (duy nhất, không khử trùng) | 20.000 | 120 trang ≈ 0,9 MB |
| `unit_ticket_type_idx` | khử trùng, ~6 byte/dòng | 100.000 | ≈ 0,7 MB |
| `unit_pool_available_idx` (một phần, `AVAILABLE`) | khử trùng | ≤ 80.000 | ≈ 0,5 MB lúc mở bán, nhỏ dần khi bán |
| `unit_reservation_idx` (một phần) | khử trùng theo reservation (2,5 unit/reservation) | ≤ 100.000 | ≈ 1,5 MB khi bán hết |
| `unit_seat_taken_idx` (một phần) | 8 + 20 → 32, + 4 = 36 byte | ≤ 20.000 | ≈ 0,8 MB khi bán hết |
| **Cộng** | | | **≈ 7,4 MB** |

Con số không tính phình index do phiên bản dòng chết giữa hai lần vacuum; S-03 đo bằng `pg_total_relation_size('inventory_unit')` và `pg_relation_size` từng index sau khi xuất bản và sau một vòng giữ/trả 100.000 unit.

## 4. Phòng chờ với 100.000 người

### 4.1 Tần suất hỏi vị trí

Mô hình "người dùng đồng thời" theo DR-75: mỗi người hỏi `GET /events/{id}/queue` (E-11) đúng theo `retryAfterSeconds` (DR-59): vị trí ≤ 200 → 3 giây; 201–2.000 → 10 giây; còn lại → 30 giây; `PRE_QUEUE` → 30 giây.

| Nhóm | Người | Nhịp | Request/giây |
| --- | --- | --- | --- |
| Vị trí 1–200 | 200 | 3 giây | 66,7 |
| Vị trí 201–2.000 | 1.800 | 10 giây | 180,0 |
| Vị trí 2.001–100.000 | 98.000 | 30 giây | 3.266,7 |
| **Cộng (sau giờ mở bán)** | 100.000 | | **≈ 3.513** |
| Trước giờ mở bán (`PRE_QUEUE`) | 100.000 | 30 giây | 3.333 |

Đợt vào hàng: `POST /events/{id}/queue` (E-10) một lần mỗi người; với `high_demand` trải trong 30 phút nhóm chờ trước (`admission.prequeue-opens`) nên trung bình 100.000 / 1.800 giây ≈ 56 request/giây; không có nhóm chờ trước mà mọi người vào trong 60 giây đầu thì ≈ 1.667 request/giây.

Mỗi lần hỏi vị trí tốn khoảng 4 lệnh Redis (token bucket `rl:queue`, `ZRANK queue`, `ZSCORE admitted`, `ZADD seen`) và không chạm PostgreSQL: 3.513 × 4 ≈ **14.000 lệnh Redis/giây**, dưới xa khả năng một tiến trình Redis (cỡ 100.000 lệnh đơn giản/giây trên một nhân, kế hoạch). Giới hạn `rl:queue` (10, nạp 1/giây) và `api_ip` 20 r/s (DR-55) không chạm người theo đúng nhịp; máy sinh tải được miễn `limit_req` bằng `RATE_LIMIT_ALLOWLIST`.

Bộ nhớ Redis: mỗi phần tử sorted set khoảng 100 byte (chuỗi `userId` 36 byte + điểm + nút skiplist); `queue:{e}`, `seen:{e}`, `prequeue:{e}` với 100.000 người ≈ 3 × 10 MB = **30 MB**, dưới `maxmemory 384mb` (DOC-62 §4).

### 4.2 Thông lượng bán và thời gian chờ

Số người rời hàng mỗi giây = `min(admit_rate, max_active / T_admitted)`, với `T_admitted` là thời gian trung bình một người giữ lượt vào (chọn chỗ, giữ vé, thanh toán; lượt vào được kéo tới `expires_at` khi đã giữ vé, DR-57).

| `T_admitted` | `max_active / T` | Người/giây (min với 50) | 2.000 người mua (5.000 vé / 2,5) cần |
| --- | --- | --- | --- |
| 30 giây | 16,7 | 16,7 | 120 giây |
| 120 giây | 4,2 | 4,2 | 480 giây (8 phút) |
| 600 giây (giữ hết 10 phút) | 0,8 | 0,8 | 2.400 giây (40 phút) |

Với tham số mặc định, `max_active` 500 là giới hạn ràng buộc chứ không phải `admit_rate` 50/giây. EXP-05 đo `T_admitted` thật và dùng bảng này để chọn hai tham số (DR-60).

### 4.3 Lệnh giữ vé tới database

Người được cấp lượt tối đa 50/giây; mỗi người gửi trung bình 1–2 lệnh giữ (lần đầu và lần thử lại sau 409). Đỉnh kế hoạch **λ ≈ 100 lệnh/giây**. Định luật Little: permit đang dùng = λ × thời gian phục vụ = 100 × 0,055 ≈ **5,5** trên 24 (ρ ≈ 0,23). Trần thông lượng của bulkhead: 24 / 0,055 ≈ **436 lệnh/giây**; vượt mức đó request nhận 503 `OVERLOADED` thay vì xếp hàng.

## 5. Connection database

| Thành phần | Connection | Công thức (Little: λ × thời gian giữ connection) |
| --- | --- | --- |
| PostgreSQL `max_connections` | 100 | DR-61 |
| Hikari của `api` | 40 | `spring.datasource.hikari.maximum-pool-size` |
| Giữ vé và hủy giữ (bulkhead) | ≤ 24; trung bình 5,5 ở đỉnh | 100/giây × 55 ms (§4.3) |
| Webhook và `confirm-free` | ≈ 1,5 | 50/giây × 30 ms |
| Hỏi `GET /orders/{id}` khi chờ xác nhận | ≈ 0,8 | 500 người × 0,5/giây (DR-50) × 3 ms |
| `GET /reservations/{id}`, `/me/*`, studio | ≈ 1 | |
| Job: `ReleaseExpiredHoldsJob`, `OutboxRelay`, `PaymentReconcileJob`, `EventLifecycleJob`, `InvariantChecker`, `RetentionJob`, nạp snapshot cho `AdmissionTicker` | ≤ 7 (mỗi job một connection, chạy tuần tự trong job) | |
| **Ngoài bulkhead, đỉnh** | **≈ 11 trên 16** | 40 − 24 |
| `make invariants` (JVM riêng, profile `invariants`) | 2 | DR-158 |
| `psql`, script seed, Prometheus exporter (nếu có) | ≤ 5 | |
| **Tổng tối đa** | 40 + 2 + 5 = **47 trên 100** | |

Quy tắc phải giữ: `hold.bulkhead.permits` < `maximum-pool-size` (DOC-34 §6 kiểm khi khởi động) để giữ vé không bao giờ chiếm hết connection của webhook và job; `max_connections` ≥ tổng các pool cộng dự phòng.

## 6. Băng thông tình trạng chỗ (P5)

Định dạng bitmap của DR-62 cho sơ đồ 20.000 ghế:

| Phần | Byte |
| --- | --- |
| Một bitset: 20.000 bit / 8 | 2.500 |
| Base64: ⌈2.500 / 3⌉ × 4 | 3.336 |
| Hai bitset (`held`, `sold`) | 6.672 |
| `pools`, `ticketTypes` (5 loại vé, ≤ 10 zone) và phần bao JSON | ≈ 600 |
| **Một response** | **≈ 7,3 KB** (trước gzip) |

Người hỏi là người đang ở màn Chọn chỗ, tối đa `max_active` = 500, mỗi người hỏi mỗi 4 giây (DR-62): 500 / 4 = **125 request/giây × 7,3 KB ≈ 0,9 MB/giây (≈ 7,3 Mbit/giây)**. So với phương án danh sách UUID của SDD gốc §7.10 (20.000 × ~37 byte ≈ 740 KB mỗi response): 125 × 740 KB ≈ 92 MB/giây, gấp khoảng 100 lần.

Phía database: cache Caffeine hết hạn 2 giây và gộp các lần trượt đồng thời (DR-62), nên mỗi sự kiện dựng snapshot tối đa 0,5 lần/giây; mỗi lần đọc ≤ 20.000 mục của `unit_seat_taken_idx` và đếm `AVAILABLE` theo pool, kế hoạch ≤ 20 ms.

## 7. Tài nguyên container trên máy thực nghiệm

Giới hạn khởi điểm của `docker-compose.experiment.yml` và `docker-compose.yml` (DOC-62 §4, §9; DR-124). Giới hạn chính thức do DOC-70 chốt sau S-06 (DR-75). Máy thực nghiệm giả định 16 GB RAM, ≥ 8 nhân (cấu hình thật ghi vào báo cáo).

| Container | CPU (trần) | RAM (trần) | Dùng cho | Đủ không (kế hoạch) |
| --- | --- | --- | --- | --- |
| `postgres` | 4 | 6 GB | `shared_buffers` 2 GB; `work_mem` 16 MB × ≤ 100 connection ≤ 1,6 GB | Kho vé 100.000 chỗ ≈ 23 MB (§3) nằm trong bộ đệm |
| `api` | 4 | 3 GB | Heap `MaxRAMPercentage=75` ≈ 2,25 GB; cache session ≤ 200.000 mục × ~0,5 KB ≈ 100 MB | Đủ |
| `redis` | 1 | 512 MB (`maxmemory 384mb`, `noeviction`) | Phòng chờ ≈ 30 MB (§4.1) | Đủ; một nhân cho ≈ 14.000 lệnh/giây |
| `nginx` | 0,5 | 128 MB | ≈ 3.600 request/giây ở đỉnh phòng chờ | Có thể thiếu CPU; xem dưới |
| `storage` | 1 | 512 MB | Gần như không dùng trong EXP | Đủ |
| `mailpit` | 0,5 | 128 MB | Email vé của EXP-02 (≈ 2.000 thư; `MP_MAX_MESSAGES` 5.000) | Đủ |
| `prometheus`, `grafana` | 1 + 1 | 512 MB + 512 MB | Profile `obs` | Đủ |
| **Cộng trần** | **13** | **≈ 11,3 GB** | | |
| Còn lại cho k6 và hệ điều hành | | 16 − 11,3 ≈ **4,7 GB** | k6 với VU nhiều người dùng (DR-75) | S-06 đo |

Trần CPU cộng 13 nhân lớn hơn số nhân của máy: trần là mức chặn trên, không phải phần dành riêng, và các container không cùng lúc dùng hết. Điểm cần đo ở S-06: `nginx` 0,5 nhân có phục vụ được ≈ 3.600 request/giây không (kế hoạch: khoảng 50 µs CPU mỗi request proxy → 0,18 nhân); k6 cạnh tranh CPU với `api` và `postgres` trên cùng máy là mối đe dọa độ tin cậy ghi ở DOC-70.

## 8. Số đo (trống tới khi chạy)

| Đại lượng | Kế hoạch | Đo ở | Số đo | Ngày · máy · SHA |
| --- | --- | --- | --- | --- |
| Thời gian transaction xuất bản 20.000 ghế + 80.000 unit pool | < 5 giây (DR-27) | S-03 | — | — |
| `pg_total_relation_size('inventory_unit')`, sự kiện 100.000 chỗ | ≈ 23 MB (§3) | S-03 | — | — |
| Trần người dùng ảo một máy | 100.000 | S-06 | — | — |
| p95 lệnh giữ vé được tiếp nhận | < 500 ms; phục vụ ở `api` ≈ 75 ms | EXP-02, EXP-05 | — | — |
| Thông lượng giữ vé tối đa trước 503 | ≈ 436/giây (§4.3) | EXP-05 | — | — |
| Trễ trả vé (5.000 reservation hết hạn cùng lúc) | ≤ 30 giây | EXP-04 | — | — |
| Request/giây hỏi vị trí, 100.000 người | ≈ 3.513 (§4.1) | EXP-05 | — | — |

## 9. Quyết định phát sinh khi viết tài liệu này

**DR-158 · Profile `invariants` dùng pool 2 connection.** *Vấn đề:* `make invariants` chạy `docker compose run --rm api --spring.profiles.active=invariants` (DR-73), tức một JVM thứ hai với cấu hình Hikari mặc định của `api`: `maximum-pool-size` 40 và `minimumIdle` mặc định bằng nó, nên mở thêm 40 connection trong khi `api` chính đang giữ 40; cộng `psql` và script seed thì sát `max_connections` 100 đúng lúc thực nghiệm đang chạy. *Quyết định:* profile `invariants` đặt `spring.datasource.hikari.maximum-pool-size=2` (kiểm tra bất biến chạy tuần tự từng truy vấn). *Hệ quả:* lệnh kiểm tra chiếm 2 connection; tổng tối đa còn 47 trên 100 (§5). *Ghi vào:* DOC-10, DOC-34 (§3.7, cột Profile), DOC-30.

## Câu hỏi còn mở

Không còn. Tài liệu không có tiền tố test riêng: mỗi chỉ tiêu được kiểm ở test hoặc EXP ghi trong cột "Kiểm chứng" (§1) và bảng số đo (§8). Một quyết định phát sinh ở §9 chờ đánh số trong sổ quyết định.
