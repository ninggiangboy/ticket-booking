# Tham chiếu cấu hình

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-34
> Phụ thuộc: SDD gốc §14, [Sổ quyết định](../00-decision-register.md) (DR-09, 12, 13, 21, 22, 38, 41, 42, 44, 47–49, 51, 53–63, 66, 72, 74–76, 80), [DOC-06](../02-glossary.md), [DOC-12](../03-architecture/code-architecture.md), [DOC-15](../05-data/ops-model.md) §9, [DOC-19](auth-and-sessions.md) §12, [DOC-27](tickets-and-notifications.md) §13, [DOC-33](observability.md) §6, [DOC-36](../07-api/api-guidelines.md) §10.4, [DOC-62](../09-operations/deploy-compose.md) §2, §6
> Người dùng chính: P1-01 (`application.yml`, `*Properties`), P1-02 (`.env.example`, compose), mọi task thêm khóa cấu hình; mọi tài liệu thiết kế khi trích khóa

Tài liệu này là **nguồn duy nhất** của khóa cấu hình, biến môi trường và profile (conventions §7). Bảng cấu hình trong tài liệu khác (DOC-19 §12, DOC-27, DOC-33 §6, DOC-36 §10.4, DOC-62 §6) là bản trích và phải khớp. Tài liệu này không chứa metric (DOC-33), mã lỗi (DOC-35) hay chuỗi giao diện (DOC-40). Khung ở P1: khóa của DOC-20, 24, 26, 28 được kiểm lại khi các tài liệu đó được viết (§9).

## 1. Quy ước

1. **Tên khóa.** Khóa Spring dạng `kebab-case` phân cấp bằng dấu chấm (`reservation.expiry.batch-size`). Khóa của Spring/thư viện giữ nguyên tên chuẩn (`spring.datasource.hikari.maximum-pool-size`).
2. **Biến môi trường.** Khóa tự định nghĩa chỉ có biến môi trường khi cột "Biến" ghi rõ; `application.yml` nối bằng `${BIEN:mặc-định}`. Khóa không có biến thì đổi bằng `SPRING_APPLICATION_JSON` hoặc quan hệ lỏng lẻo của Spring (`RESERVATION_EXPIRY_BATCH_SIZE`), không ghi vào `.env.example`. Khóa Spring chuẩn dùng quan hệ lỏng lẻo (`SPRING_DATASOURCE_URL`).
3. **Kiểu `duration`** luôn viết ISO-8601 (`PT10M`, `PT5S`) trong `application.yml`, `.env` và tài liệu; không viết `15s` hay `10m` dù Spring chấp nhận (DR-146).
4. **Mặc định ở một chỗ.** Giá trị mặc định đặt trong `application.yml` của module sở hữu (`<module>.config`, DOC-12 §3), mỗi khóa một `@ConfigurationProperties` có kiểm tra `@Validated`. Profile chỉ ghi đè những khóa ở cột "Profile".
5. **Thứ tự ưu tiên** (cao → thấp): tham số dòng lệnh `--key=value` → biến môi trường → `application-<profile>.yml` → `application.yml`.
6. **Secret** (§5) chỉ đến từ biến môi trường, không có mặc định thật, không vào log, không vào image (DOC-32 §6).
7. **Thêm khóa mới** nghĩa là cập nhật bảng này trong cùng PR. Kiểm tra ở CFG-01, CFG-02.

## 2. Profile

Hai loại profile, không nhầm (DOC-62 §2): **profile Spring** (đặt bằng `SPRING_PROFILES_ACTIVE`, quyết định bean và khóa của `api`) và **profile compose** (quyết định container nào chạy).

| Profile | Loại | Bật bằng | Tác dụng lên cấu hình |
| --- | --- | --- | --- |
| `dev` | Spring | `.env.example` mặc định | `auth.cookie-secure=false` (DR-22), `springdoc.swagger-ui.enabled=true`, `management.endpoints.web.exposure.include=health,prometheus`, log dễ đọc |
| `fake-payments` | Spring | mặc định cùng `PAYMENTS_MODE=fake` | `FakePaymentGateway`, `db/migration-fake` thêm vào `spring.flyway.locations`, endpoint `/fake-payments/**` (DR-51) |
| `stripe` | Spring | `make up-stripe` (`PAYMENTS_MODE=stripe`) | `StripePaymentGateway`; đòi `payment.stripe.secret-key`, `payment.stripe.webhook-secret` |
| `experiment` | Spring | `make exp` | Cho phép `inventory.strategy` ≠ `skip-locked`; `db/migration-experiment`; nạp 100.000 người dùng (DR-75, DR-76); `logging.level.io.ticket=WARN` |
| `invariants` | Spring | `make invariants` | Chạy `InvariantChecker` một lần, in JSON, thoát 0/1 (DR-73) |
| `seed` | Spring | `make seed` | Nạp dữ liệu demo rồi thoát (DOC-61 §5, DR-78) |
| `obs` | Spring | cùng compose `obs` | Mở `prometheus` trên cổng quản trị 9090 (DR-09) |
| (mặc định) | compose | `make up` | `nginx`, `api`, `postgres`, `redis`, `storage`, `mailpit` |
| `stripe` | compose | `make up-stripe` | Thêm `stripe-cli` |
| `obs` | compose | `make up-obs` | Thêm `prometheus`, `grafana` |

Tổ hợp mặc định: `SPRING_PROFILES_ACTIVE=dev,fake-payments`, `PAYMENTS_MODE=fake`. Luật khởi động ở §6.

## 3. Khóa cấu hình

Cột "Profile": "mọi" = không đổi theo profile; tên profile = giá trị khác mặc định ở profile đó. "—" ở cột Biến = không có biến riêng.

### 3.1 Ứng dụng và nền tảng

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `app.name` | string | `ticket` | `APP_NAME` | mọi | Tên sản phẩm trong email (DR-54); tên tạm theo design system |
| `app.base-url` | URL | `http://localhost:8080` | `APP_BASE_URL` | mọi (`make dev`: `http://localhost:5173`) | Gốc của link tuyệt đối trong email và `return_url` Stripe (DR-50, DR-54) |
| `app.domain` | string | `ticket.localhost` | `APP_DOMAIN` | mọi | Phần sau `@` của `Message-ID` (DR-53) |
| `platform.timezone` | tên IANA | `Asia/Ho_Chi_Minh` | `PLATFORM_TIMEZONE` | mọi | Múi giờ mặc định của sự kiện mới, giờ chạy `RetentionJob` (DR-12, DR-74) |
| `server.port` | int | `8081` | — | mọi | Cổng nội bộ của `api` (DR-80) |
| `management.server.port` | int | `9090` | — | mọi | Cổng quản trị Actuator; không qua nginx (DR-09, DR-80) |
| `payment.min-amount` | int (VND) | `20000` | — | mọi | Giá vé khác 0 tối thiểu; S-02 hiệu chỉnh (DR-13) |
| `payment.max-amount` | int (VND) | `100000000` | — | mọi | Giá vé tối đa (DR-13) |
| `support.email` | string | rỗng | `SUPPORT_EMAIL` | mọi | Email hỗ trợ trong email `refund-pending` (DR-44) |
| `support.refund-sla-text` | key i18n | `email.refund-pending.sla` | — | mọi | Thời hạn hoàn tiền hiển thị: "5–10 ngày làm việc" / "5–10 business days" (DR-44) |

Không có `PLATFORM_CURRENCY`: tiền tệ chỉ VND, gán cứng (DR-13).

### 3.2 Xác thực và session

Nguồn: DOC-19 §12.1.

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `auth.cookie-secure` | bool | `true` | `AUTH_COOKIE_SECURE` | `dev`: `false` | Cờ `Secure` của cookie `tb_session` (DR-22) |
| `auth.session.idle-timeout` | duration | `P30D` | — | mọi | Không hoạt động quá mức này thì session hết hạn |
| `auth.session.touch-interval` | duration | `PT1H` | — | mọi | Chỉ cập nhật `last_seen_at` khi cũ hơn mức này |
| `auth.session.cache-ttl` | duration | `PT60S` | — | mọi | TTL cache Caffeine của session |
| `auth.session.cache-max-size` | int | `200000` | — | mọi | Số mục tối đa của cache |
| `auth.magic-link.ttl` | duration | `PT15M` | — | mọi | Thời hạn token (DR-21) |
| `auth.magic-link.email-limit` | int | `3` | — | mọi | Số link tối đa mỗi email trong cửa sổ |
| `auth.magic-link.email-window` | duration | `PT15M` | — | mọi | Cửa sổ giới hạn theo email |
| `auth.magic-link.ip-limit` | int | `10` | — | mọi | Số link tối đa mỗi IP trong cửa sổ |
| `auth.magic-link.ip-window` | duration | `PT1H` | — | mọi | Cửa sổ giới hạn theo IP |

### 3.3 Email và outbox

Nguồn: DOC-19 §12.1, DOC-27.

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `mail.from` | string | `ticket <no-reply@ticket.localhost>` | `MAIL_FROM` | mọi | Người gửi (DR-54) |
| `mail.smtp.timeout` | duration | `PT5S` | `MAIL_SMTP_TIMEOUT` | mọi | Timeout kết nối và đọc SMTP; dùng cho magic link và `OutboxRelay` (DR-21) |
| `spring.mail.host` | string | `mailpit` | `SPRING_MAIL_HOST` | mọi | Máy chủ SMTP |
| `spring.mail.port` | int | `1025` | `SPRING_MAIL_PORT` | mọi | Cổng SMTP |
| `spring.mail.username`, `spring.mail.password` | string | rỗng | `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | mọi | Chỉ khi dùng SMTP thật; secret |
| `outbox.relay.enabled` | bool | `true` | `OUTBOX_RELAY_ENABLED` | mọi (`experiment`: EXP-08 tắt để tích hàng đợi) | Bật `OutboxRelay` |
| `outbox.relay.interval` | duration | `PT1S` | — | mọi | Nhịp `OutboxRelay` (DR-53) |
| `outbox.relay.batch-size` | int | `50` | — | mọi | Số dòng claim mỗi lượt |
| `outbox.relay.lease` | duration | `PT60S` | — | mọi | Lease ghi vào `next_attempt_at` lúc claim |
| `outbox.relay.backoff-base` | duration | `PT10S` | — | mọi | Backoff: `base × 2^(attempts−1)` |
| `outbox.relay.backoff-max` | duration | `PT1H` | — | mọi | Trần backoff |
| `outbox.relay.max-attempts` | int | `12` | — | mọi | Từ ngưỡng này `FAILED` |
| `outbox.relay.batch-budget` | duration | `PT40S` | — | mọi | Ngân sách thời gian một lượt; `batch-budget + mail.smtp.timeout < lease` (§6, DR-104) |
| `outbox.enqueue-batch-size` | int | `500` | — | mọi | Cỡ lô JDBC của `NotificationApi.enqueueAll` (DR-106) |

### 3.4 Giữ vé và job trả vé

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `reservation.hold-duration` | duration | `PT10M` | — | mọi | Thời hạn giữ, một giá trị toàn nền tảng; giao diện nhận qua `holdMinutes` ở `GET /events/{id}` (DR-42) |
| `reservation.max-units-per-hold` | int | `50` | — | mọi | Số unit tối đa mỗi lệnh giữ; giới hạn kỹ thuật, không theo sự kiện (DR-41) |
| `reservation.expiry.interval` | duration | `PT5S` | — | mọi | Nhịp job trả vé (`fixedDelay`) |
| `reservation.expiry.batch-size` | int | `200` | — | mọi | Số reservation mỗi vòng |
| `reservation.expiry.lease` | duration | `PT30S` | — | mọi | `EXPIRING` được lấy lại khi `expiring_since` cũ hơn mức này |
| `reservation.payment-min-remaining` | duration | `PT30S` | — | mọi | Tạo PaymentIntent bị từ chối nếu còn ít hơn (DR-47) |
| `hold.bulkhead.permits` | int | `24` | `HOLD_BULKHEAD_PERMITS` | mọi | Số lệnh giữ/hủy giữ đồng thời; < `maximum-pool-size` (DR-61) |
| `hold.bulkhead.wait` | duration | `PT0S` | — | mọi | Chờ permit; 0 = thử lấy, không chờ |
| `hold.statement-timeout` | duration | `PT2S` | — | mọi | `SET LOCAL statement_timeout` trong transaction giữ vé |
| `hold.lock-timeout` | duration | `PT1S` | — | mọi | `SET LOCAL lock_timeout` |
| `hold.overloaded-retry-min`, `hold.overloaded-retry-max` | int (giây) | `1`, `3` | — | mọi | Khoảng ngẫu nhiên của `Retry-After` khi hết permit (DR-61) |
| `invariant.interval` | duration | `PT5M` | — | mọi | Nhịp `InvariantChecker` (DR-73) |
| `invariant.active-overdue` | duration | `PT60S` | — | mọi | `ACTIVE` quá `expires_at` hơn mức này là sai lệch (DR-42) |
| `invariant.expiring-stuck` | duration | `PT2M` | — | mọi | `EXPIRING` kẹt hơn mức này là sai lệch (DR-42) |
| `inventory.strategy` | enum | `skip-locked` | `INVENTORY_STRATEGY` | `experiment` cho `counter`, `naive` | Bản cài đặt `InventoryClaimer`; khác `skip-locked` mà không có `experiment` thì `api` không khởi động (DR-76) |

### 3.5 Thanh toán

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `payment.mode` | enum `fake`\|`stripe` | `fake` | `PAYMENTS_MODE` | mọi | Phải khớp profile Spring (§6); build `nginx` đọc cùng biến thành `VITE_PAYMENTS` (DR-51, DR-125) |
| `payment.stripe.secret-key` | secret | rỗng | `STRIPE_SECRET_KEY` | `stripe` | `sk_test_…`; `sk_live_…` bị từ chối khi `payment.mode=fake` |
| `payment.stripe.webhook-secret` | secret | rỗng | `STRIPE_WEBHOOK_SECRET` | `stripe` | `whsec_…`; `stripe-cli` cấp lúc chạy |
| `payment.stripe.publishable-key` | string | rỗng | `STRIPE_PUBLISHABLE_KEY` | `stripe` | Công khai; build vào frontend qua `VITE_STRIPE_PUBLISHABLE_KEY` (DR-126) |
| `payment.stripe.cancel-timeout` | duration | `PT5S` | — | `stripe` | Timeout gọi hủy PaymentIntent (DR-42) |
| `payment.stripe.connect-timeout` | duration | `PT2S` | — | `stripe` | Timeout kết nối client Stripe (DR-47) |
| `payment.stripe.read-timeout` | duration | `PT10S` | — | `stripe` | Timeout đọc client Stripe (DR-47) |
| `payment.stripe.max-network-retries` | int | `2` | — | `stripe` | `maxNetworkRetries` của stripe-java (DR-47) |
| `payment.stripe.webhook-tolerance` | duration | `PT300S` | — | `stripe`, `fake-payments` | Dung sai thời gian chữ ký webhook (DR-48) |
| `payment.reconcile.interval` | duration | `PT60S` | — | mọi | Nhịp `PaymentReconcileJob` (DR-49) |
| `payment.reconcile.batch-size` | int | `50` | — | mọi | Số đơn mỗi vòng |
| `payment.reconcile.pending-after` | duration | `PT15M` | — | mọi | Đơn `PENDING_PAYMENT` cũ hơn mức này được đối chiếu |
| `payment.reconcile.expiring-after` | duration | `PT2M` | — | mọi | Reservation `EXPIRING` cũ hơn mức này được đối chiếu |

### 3.6 Kiểm soát tiếp nhận và giới hạn theo người dùng

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `admission.max-active` | int | `500` | — | mọi | Số người tối đa trong khu chọn chỗ, kích thước `admitted` (DR-57) |
| `admission.admit-rate` | int | `50` | — | `experiment` (EXP-05 tăng dần) | Số người cấp lượt mỗi giây; không tự điều chỉnh (DR-60) |
| `admission.pass-ttl` | duration | `PT5M` | — | mọi | Hạn lượt vào để chọn chỗ (DR-57; ⚠ SDD gốc 10 phút) |
| `admission.prequeue-opens` | duration | `PT30M` | — | mọi | Phòng chờ mở trước giờ mở bán, chỉ `high_demand` |
| `admission.idle-timeout` | duration | `PT2M` | — | mọi | Bỏ người không hỏi vị trí quá mức này |
| `admission.repick-grace` | duration | `PT2M` | — | mọi | Thêm thời gian chọn lại sau khi reservation đóng mà chưa thanh toán |
| `admission.ticker-interval` | duration | `PT1S` | — | mọi | Nhịp `AdmissionTicker` (`fixedRate`, DR-59) |
| `admission.lock-ttl` | duration | `PT0.9S` | — | mọi | TTL của `admit-lock:{e}` (DR-59: `PX 900`) |
| `admission.evict-limit` | int | `1000` | — | mọi | Số người bị loại tối đa mỗi lượt (DR-59) |
| `admission.shuffle-batch-size` | int | `1000` | — | mọi | Lô `ZADD` khi xáo nhóm chờ trước lúc mở bán (DR-59) |
| `ratelimit.hold.capacity`, `ratelimit.hold.refill-per-second` | int, number | `5`, `0.5` | — | `experiment` | Bucket `rl:hold:{userId}`: giữ và hủy giữ (DR-56) |
| `ratelimit.payment-intent.capacity`, `ratelimit.payment-intent.refill-per-second` | int, number | `5`, `0.2` | — | `experiment` | Bucket `rl:pi:{userId}` |
| `ratelimit.queue.capacity`, `ratelimit.queue.refill-per-second` | int, number | `10`, `1` | — | `experiment` | Bucket `rl:queue:{userId}` |
| `spring.data.redis.host` | string | `redis` | `SPRING_DATA_REDIS_HOST` | mọi | Máy chủ Redis |
| `spring.data.redis.port` | int | `6379` | `SPRING_DATA_REDIS_PORT` | mọi | Cổng Redis |
| `spring.data.redis.timeout` | duration | `PT0.2S` | — | mọi | Quá hạn thì coi như mất Redis; không limiter dự phòng (DR-56) |
| `availability.cache-ttl` | duration | `PT2S` | — | mọi | Cache Caffeine snapshot tình trạng chỗ theo `eventId` (DR-62) |
| `sales.cache-ttl` | duration | `PT5S` | — | mọi | Cache Redis số liệu bán vé của studio (DR-71) |

### 3.7 Database

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `spring.datasource.url` | JDBC URL | `jdbc:postgresql://postgres:5432/ticket` | `SPRING_DATASOURCE_URL` | mọi | |
| `spring.datasource.username` | string | `ticket` | `SPRING_DATASOURCE_USERNAME` | mọi | |
| `spring.datasource.password` | secret | — (bắt buộc) | `SPRING_DATASOURCE_PASSWORD` | mọi | `.env.example` chỉ có giá trị dev |
| `spring.datasource.hikari.maximum-pool-size` | int | `40` | `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE` | mọi | PostgreSQL `max_connections` = 100 (DR-61) |
| `spring.datasource.hikari.connection-timeout` | duration (ms) | `1000` | `SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT` | mọi | Hết hạn thì 503 `OVERLOADED` (DR-61) |
| `spring.flyway.enabled` | bool | `true` | — | mọi | Chạy migration trước khi readiness `UP` |
| `spring.flyway.locations` | list | `classpath:db/migration` | — | `fake-payments` thêm `db/migration-fake`; `experiment` thêm `db/migration-experiment` | DR-51, DR-76 |
| `spring.flyway.validate-on-migrate` | bool | `true` | — | mọi | Sai checksum thì `api` thoát |
| `spring.flyway.out-of-order` | bool | `true` | — | mọi | Cho phép phiên bản thấp hơn bản đã áp dụng ở thư mục profile (DR-94) |
| `spring.flyway.ignore-migration-patterns` | string | `*:missing` | — | mọi | Tắt profile sau khi đã áp dụng thì không lỗi (DR-94) |

Tham số của PostgreSQL (`shared_buffers`, `work_mem`, …) không phải khóa của `api`: ở DOC-62 §9.

### 3.8 Sự kiện, ảnh, lưu giữ, job nền

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `event.lifecycle.interval` | duration | `PT60S` | — | mọi | Nhịp `EventLifecycleJob`: `PUBLISHED`/`PAUSED → ENDED` khi `ends_at ≤ now()` (DR-24) |
| `media.max-event-image-bytes` | int | `2097152` | — | mọi | Ảnh sự kiện ≤ 2 MB (DR-38) |
| `media.max-map-image-bytes` | int | `5242880` | — | mọi | Ảnh mặt bằng ≤ 5 MB; khớp `CHECK (size_bytes <= 5242880)` của DOC-14 |
| `media.max-dimension` | int (px) | `8000` | — | mọi | Từ chối ảnh quá mỗi cạnh |
| `retention.cron` | cron | `0 0 3 * * *` | — | mọi | `RetentionJob`, theo `platform.timezone` (DR-74) |
| `retention.batch-size` | int | `5000` | — | mọi | Số dòng xóa mỗi lô |
| `retention.login-token` | duration | `PT24H` | — | mọi | Giữ sau `expires_at` |
| `retention.session` | duration | `P7D` | — | mọi | Giữ sau khi hết hạn hoặc thu hồi |
| `retention.idempotency-key` | duration | `PT24H` | — | mọi | |
| `retention.stripe-event` | duration | `P30D` | — | mọi | |
| `retention.outbox-sent` | duration | `P7D` | — | mọi | |
| `retention.outbox-failed` | duration | `P30D` | — | mọi | |
| `retention.unreferenced-media` | duration | `PT24H` | — | mọi | `DeleteObject` rồi xóa dòng `media` |

### 3.9 Object storage

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `storage.s3.endpoint` | URL | `http://storage:8333` | `STORAGE_S3_ENDPOINT` | mọi | SeaweedFS S3, chỉ trong mạng compose (DR-38) |
| `storage.s3.region` | string | `us-east-1` | `STORAGE_S3_REGION` | mọi | |
| `storage.s3.bucket` | string | `ticket-media` | `STORAGE_S3_BUCKET` | mọi | Bucket private, không presigned URL |
| `storage.s3.access-key` | secret | — (bắt buộc) | `STORAGE_S3_ACCESS_KEY` | mọi | |
| `storage.s3.secret-key` | secret | — (bắt buộc) | `STORAGE_S3_SECRET_KEY` | mọi | |
| `storage.s3.path-style` | bool | `true` | — | mọi | SeaweedFS cần path-style (DR-148) |

### 3.10 API và tài liệu OpenAPI

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `api.pagination.default-limit` | int | `20` | — | mọi | Mặc định `limit` (DR-63) |
| `api.pagination.max-limit` | int | `100` | — | mọi | Trần `limit` |
| `springdoc.api-docs.enabled` | bool | `true` | — | mọi (nginx không proxy `/v3/`) | |
| `springdoc.swagger-ui.enabled` | bool | `false` | — | `dev`: `true` | |
| `spring.jackson.deserialization.fail-on-unknown-properties` | bool | `true` | — | mọi | Trường lạ trong body → 400 `BAD_REQUEST` (DR-88) |

### 3.11 Observability

Nguồn: DOC-33 §6.

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `logging.structured.format.console` | enum | `ecs` | — | mọi | Log JSON (DR-09) |
| `logging.level.io.ticket` | enum | `INFO` | — | `experiment`: `WARN` | Mức log ứng dụng |
| `management.endpoints.web.exposure.include` | list | `health` | — | `dev`, `obs`: `health,prometheus` | Endpoint Actuator |
| `management.metrics.distribution.percentiles-histogram.http.server.requests` | bool | `true` | — | mọi | Histogram cho p95 |
| `ticket.metrics.sampler-interval` | duration | `PT15S` | — | mọi | Nhịp lấy mẫu gauge `ticket_outbox_pending`, `ticket_queue_*` (DR-120) |
| `ticket.metrics.refund-pending-interval` | duration | `PT60S` | — | mọi | Nhịp lấy mẫu `ticket_orders_refund_pending` |

### 3.12 Biến chỉ thuộc compose, nginx và build frontend

Không phải khóa của `api`; nguồn của giá trị mẫu là `.env.example` (DOC-62 §6).

| Biến | Kiểu | Mặc định | Dùng bởi | Mô tả |
| --- | --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | list | `dev,fake-payments` | `api` | Profile Spring (§2) |
| `NGINX_PORT`, `POSTGRES_PORT`, `REDIS_PORT`, `MAILPIT_UI_PORT`, `MAILPIT_SMTP_PORT` | int | `8080`, `5432`, `6379`, `8025`, `1025` | compose | Cổng host; đổi khi bị chiếm (DR-72) |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | string | `ticket`, `ticket`, `ticket-dev-password` | `postgres` | Khởi tạo database; password là secret |
| `SEAWEEDFS_TAG` | string | rỗng (bắt buộc điền ở P1-02) | compose | Tag image `storage` |
| `PROMETHEUS_TAG`, `GRAFANA_TAG` | string | chốt ở P6-08 | compose `obs` | Tag image |
| `MP_MAX_MESSAGES` | int | `5000` | `mailpit` | Trần số thư lưu |
| `RATE_LIMIT_ALLOWLIST` | danh sách IP/CIDR | rỗng | `nginx` | IP được miễn `limit_req`; máy sinh tải (DR-55, DR-75) |
| `VITE_PAYMENTS` | `fake`\|`stripe` | = `PAYMENTS_MODE` | build `nginx` | Form giả hoặc Payment Element (DR-51) |
| `VITE_STRIPE_PUBLISHABLE_KEY` | string | = `STRIPE_PUBLISHABLE_KEY` | build `nginx` | Khóa công khai (DR-126) |
| `VITE_API_MOCK` | `1` | không đặt | `pnpm dev` | Bật MSW (master plan §7.4) |

Tham số `limit_req` của nginx (`api_ip` 20 r/s burst 40, `auth_ip` 10 r/phút burst 5, `hold_ip` 5 r/s burst 10, `client_max_body_size 6m`, cache `/media/` 1 ngày, `GET /events/{id}` 5 giây, `GET /events` 5 giây) là hằng số trong file cấu hình nginx (DOC-62 §8, DR-55, DR-86), không có khóa Spring; chỉ `RATE_LIMIT_ALLOWLIST` đổi được bằng biến.

### 3.13 Hằng số, không phải cấu hình

Giá trị đã chốt, đổi bằng sửa code và DR mới, không bằng khóa cấu hình, để tránh nhiều biến thể phải kiểm thử.

| Hằng số | Giá trị | Nguồn |
| --- | --- | --- |
| Tên cookie session, `Path`, `SameSite` | `tb_session`, `/`, `Lax` | DR-22 |
| Băm body của `Idempotency-Key` | SHA-256 trên body thô | DR-45 |
| `SUPPORTED_LOCALES` | `vi` (mặc định), `en` | DR-10, DOC-31 |
| Giới hạn tài liệu sơ đồ | ≤ 5 MB, ≤ 20.000 ghế, ≤ 1.000 hàng, ≤ 200 zone, ≤ 200 trang trí; `canvas` 500–20.000 | DR-32 |
| Tối đa loại vé mỗi event, sức chứa GA | 5; 1–100.000 | DR-26 |
| Độ dài tên tổ chức, `return_to` | 1–120; ≤ 512 | DR-23, DR-21 |
| Số lần thử lại tự động ở client | `clamp(Retry-After, 1, 32)` giây, tối đa 10 lần | DR-132 |

## 4. Bản đồ nơi mỗi nhóm khóa được dùng

| Nhóm | Module `io.ticket.<module>` | Tài liệu thiết kế |
| --- | --- | --- |
| `auth.*`, `mail.*`, `app.*` | `identity`, `notification` | DOC-19, DOC-27 |
| `reservation.*`, `hold.*`, `inventory.*` | `reservation`, `inventory` | DOC-24 |
| `payment.*` | `payment` | DOC-26 |
| `admission.*`, `ratelimit.*`, `availability.*` | `admission`, `inventory` | DOC-28, DOC-29 |
| `outbox.*`, `support.*` | `notification` | DOC-27 |
| `retention.*`, `event.lifecycle.*`, `media.*`, `storage.*` | `common`, `event`, `media` | DOC-18, DOC-20 |
| `invariant.*` | `invariant` | DOC-30 |
| `logging.*`, `management.*`, `ticket.metrics.*` | `common` | DOC-33 |

## 5. Secret

| Biến | Dùng bởi | Ghi chú |
| --- | --- | --- |
| `SPRING_DATASOURCE_PASSWORD`, `POSTGRES_PASSWORD` | `api`, `postgres` | Giá trị dev hợp lệ trong `.env.example`; không dùng ngoài máy dev |
| `STORAGE_S3_ACCESS_KEY`, `STORAGE_S3_SECRET_KEY` | `api`, `storage` | Bucket private (DR-38) |
| `STRIPE_SECRET_KEY` | `api` | `sk_test_…`; `sk_live_…` bị từ chối khi fake |
| `STRIPE_WEBHOOK_SECRET` | `api` | Từ `stripe-cli` |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | `api` | Chỉ khi dùng SMTP thật; Mailpit không cần |

`STRIPE_PUBLISHABLE_KEY` công khai, không phải secret. Không có secret nào khác: không `ADMISSION_TOKEN_SECRET` (DR-58), không `OUTBOX_ENCRYPTION_KEY` (DR-21), không `REDIS_PASSWORD` (Redis không mở cổng ra ngoài máy). Secret không vào log, image, hay repo (CI quét `sk_(test|live)_`, DOC-62 §6).

## 6. Kiểm tra lúc khởi động

`api` thoát với mã ≠ 0 và một dòng log ERROR nêu khóa sai khi:

| # | Điều kiện | Lý do |
| --- | --- | --- |
| 1 | `payment.mode` không khớp profile (`fake` ⇔ `fake-payments`, `stripe` ⇔ `stripe`) | Tránh cấu hình nửa vời (DR-125) |
| 2 | `payment.mode=fake` mà `payment.stripe.secret-key` bắt đầu `sk_live_` | Không bao giờ gọi Stripe thật bằng cổng giả (DR-51) |
| 3 | `inventory.strategy` ≠ `skip-locked` mà thiếu profile `experiment` | DR-76 |
| 4 | `hold.bulkhead.permits` ≥ `spring.datasource.hikari.maximum-pool-size` | Bulkhead phải nhỏ hơn pool (DR-61) |
| 5 | `outbox.relay.batch-budget + mail.smtp.timeout ≥ outbox.relay.lease` | Tránh gửi trùng khi hết lease (DR-104) |
| 6 | `reservation.payment-min-remaining ≥ reservation.hold-duration` | Không ai thanh toán được |
| 7 | `payment.min-amount > payment.max-amount`, hoặc `reservation.max-units-per-hold` ngoài 1–200 | Kiểm tra miền giá trị |
| 8 | `platform.timezone` không phải tên IANA hợp lệ | DR-12 |
| 9 | `auth.cookie-secure=true` mà `app.base-url` là `http://` không phải `localhost` | Cookie không bao giờ được gửi lại |
| 10 | `payment.mode=stripe` mà thiếu `payment.stripe.secret-key` hoặc `webhook-secret` | |

## 7. Mâu thuẫn giữa các tài liệu đã phát hiện

Bảng này ghi cái đã được chốt ở đây (DR-146, 2); tài liệu "sai" cần sửa khi bên gọi gộp.

| # | Mâu thuẫn | Tài liệu sai | Chốt |
| --- | --- | --- | --- |
| 1 | Tên biến mật khẩu: `DB_PASSWORD`, `REDIS_PASSWORD`, `SMTP_USERNAME/PASSWORD` | DOC-07 (`system-context-and-containers.md`, bảng tích hợp), DOC-32 (bảng secret) | Theo DOC-62 §6: `SPRING_DATASOURCE_PASSWORD`/`POSTGRES_PASSWORD`, `SPRING_MAIL_*`; không có `REDIS_PASSWORD` |
| 2 | Định dạng duration: `15s`, `60s` | DOC-33 §6 (`ticket.metrics.*`) | ISO-8601 `PT15S`, `PT60S` (§1.3) |
| 3 | Cổng thanh toán có ba tên (`PAYMENTS_MODE`, profile, `VITE_PAYMENTS`) | không tài liệu nào sai | Khóa `payment.mode` = biến `PAYMENTS_MODE`; profile Spring phải khớp; `VITE_PAYMENTS` là bản build của cùng giá trị |
| 4 | `app.name` vắng ở bảng khóa của DOC-19 §12.1 | DOC-19 | Thêm ở §3.1 (DOC-27 và DR-54 đã có) |
| 5 | `api` nghe 9090 "chỉ trong profile `obs`" (DR-72) và "luôn" (DR-09/DOC-33) | DR-72 diễn đạt chưa rõ | Cổng quản trị 9090 luôn mở trong container, không bao giờ ra host; `prometheus` chỉ ở `dev`, `obs` (§3.11) |
| 6 | `cache` snapshot tình trạng chỗ vừa "Redis" (DR-71) vừa "Caffeine" (DR-62) | DR-71 | Snapshot ghế: Caffeine, `availability.cache-ttl`; số liệu `sales`: Redis, `sales.cache-ttl` |

## 8. Quyết định phát sinh khi viết tài liệu này

Do Claude chốt theo ủy quyền của Owner (master plan §0.1); bên gọi gán số DR.

- **DR-146 · Một quy ước đặt tên và định dạng cấu hình.** *Vấn đề:* các tài liệu trộn `15s`/`PT15S`, biến môi trường tự đặt và biến Spring lỏng lẻo. *Quyết định:* duration luôn ISO-8601; khóa tự định nghĩa dùng `kebab-case`, biến môi trường chỉ có khi đã ghi ở bảng (§1); mỗi nhóm khóa một `@ConfigurationProperties` có `@Validated`. *Hệ quả:* DOC-33 đổi `15s` thành `PT15S`; dễ kiểm tự động (CFG-01).
- **DR-147 · Kiểm tra cấu hình lúc khởi động (§6).** *Vấn đề:* nhiều ràng buộc giữa các khóa (bulkhead < pool, lease > ngân sách gửi) chỉ nằm trong văn bản. *Quyết định:* một bean `ConfigurationGuard` kiểm tra mười điều kiện ở §6 và thoát khi sai. *Hệ quả:* lỗi cấu hình hiện ở lúc khởi động thay vì ở EXP.
- **DR-148 · Khóa mới do tài liệu này đặt.** *Vấn đề:* DR-56, 57, 59, 62, 74 nêu con số nhưng không đặt tên khóa. *Quyết định:* đặt `ratelimit.*`, `admission.ticker-interval|lock-ttl|evict-limit|shuffle-batch-size`, `availability.cache-ttl`, `sales.cache-ttl`, `retention.*`, `event.lifecycle.interval`, `invariant.*`, `payment.reconcile.*`, `payment.stripe.connect-timeout|read-timeout|max-network-retries|webhook-tolerance`, `media.*`, `storage.s3.path-style` với đúng giá trị mặc định của DR gốc. *Hệ quả:* EXP có thể đổi nhịp mà không sửa code; DOC-24, 26, 28, 29, 30, 18 dùng đúng các tên này.

## 9. Câu hỏi còn mở

Không có câu hỏi chặn. Việc cần làm khi các tài liệu sau được viết:

- DOC-20, DOC-24, DOC-26, DOC-28, DOC-29, DOC-30, DOC-18: đối chiếu khóa nhóm §3.4–3.8 với nội dung và thêm khóa phát sinh vào đây trong cùng PR.
- `payment.min-amount` và `payment.stripe.*` chờ kết quả S-02; `admission.admit-rate` chờ EXP-05; `hold.bulkhead.permits` chờ EXP-05 và S-03.

## 10. Test bắt buộc (tiền tố `CFG-`)

Tiền tố `CFG-` dành cho tài liệu này; DOC-69 ghi vào bảng tiền tố.

| ID | Kịch bản | Kết quả mong đợi |
| --- | --- | --- |
| CFG-01 | `ConfigurationReferenceTest` đọc bảng §3 của tài liệu này và so với metadata của mọi `@ConfigurationProperties` | Mỗi khóa có trong cả hai nơi; kiểu và mặc định khớp |
| CFG-02 | `.env.example` có đủ biến ở §3 mà cột "Biến" ghi và không biến thừa | Không khác biệt |
| CFG-03 | Khởi động `api` với `PAYMENTS_MODE=fake` và `SPRING_PROFILES_ACTIVE=dev,stripe` | Thoát ≠ 0, log nêu `payment.mode` |
| CFG-04 | `PAYMENTS_MODE=fake` kèm `STRIPE_SECRET_KEY=sk_live_x` | Thoát ≠ 0 |
| CFG-05 | `INVENTORY_STRATEGY=naive` không có profile `experiment` | Thoát ≠ 0 |
| CFG-06 | `HOLD_BULKHEAD_PERMITS=40` với pool 40 | Thoát ≠ 0 |
| CFG-07 | `OUTBOX_RELAY_BATCH_BUDGET=PT58S` | Thoát ≠ 0 (58 + 5 ≥ 60) |
| CFG-08 | `reservation.hold-duration=PT10M` đổi thành `PT5M` | `holdMinutes = 5` ở `GET /events/{id}`; `expires_at` khớp |
| CFG-09 | Khóa duration viết `15s` trong `application.yml` | Test kiểm tĩnh báo lỗi (DR-146) |
| CFG-10 | `AUTH_COOKIE_SECURE=true` với `APP_BASE_URL=http://example.test` | Thoát ≠ 0 |
| CFG-11 | Tìm secret: `git grep -nE "sk_(test\|live)_[A-Za-z0-9]{10,}"` ngoài `*.md` | Không có kết quả |
| CFG-12 | Mọi giá trị mặc định của DR-42, 56, 57, 61 | Trùng bảng §3 (so với `application.yml`) |
