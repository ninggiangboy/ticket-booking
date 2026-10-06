# Observability

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-33
> Phụ thuộc: SDD gốc §2.3, §10.4, §14.3, §15, [DOC-06](../02-glossary.md), [DOC-07](../03-architecture/system-context-and-containers.md) §3, [DOC-11](../03-architecture/tech-stack-and-versions.md), [DOC-35](error-handling.md) §5, [DOC-36](../07-api/api-guidelines.md), [DOC-62](../09-operations/deploy-compose.md) §2, §7, [Sổ quyết định](../00-decision-register.md) (DR-09, 22, 42, 53, 56, 57, 60, 61, 62, 73, 75), [DOC-03](../01-product/requirements.md) NFR-02, NFR-03
> Người dùng chính: P1-01 (khung log), P6-08 (profile `obs`), mọi task thêm metric (checklist [DOC-12](../03-architecture/code-architecture.md) §7 mục 10), người chạy EXP-02, EXP-04, EXP-05, EXP-08 ([DOC-70](../10-testing/experiments/README.md))

Tài liệu là **nguồn duy nhất** của trường log bắt buộc và của danh mục metric. Nó trả lời ba câu hỏi: dòng log có gì và không có gì; mỗi metric tên gì, đo cái gì, phát ở đâu; dashboard của EXP-05 có những panel nào. Không có alerting ở giai đoạn này (DR-09): sự cố được phát hiện bằng `InvariantChecker` mỗi 5 phút (DR-73) và bằng mắt trên dashboard khi chạy thực nghiệm. Tên key cấu hình ở DOC-34; thực nghiệm dùng số liệu ở DOC-70…80.

## 1. Nguyên tắc

1. **Đo để trả lời câu hỏi của thực nghiệm**, không đo mọi thứ. Mỗi metric ở §4 có cột "Dùng cho"; metric không ai dùng thì không thêm.
2. **Không dữ liệu cá nhân và không nhãn có lực lượng lớn.** Label chỉ nhận tập giá trị đóng (enum, mã lỗi, tên job). Cấm làm label: `user_id`, `email`, `order_id`, `reservation_id`, `event_id`, `ip`, đường dẫn có tham số (dùng mẫu `/events/{id}`).
3. **Một `trace_id` cho một request**: nginx sinh, API đặt vào MDC, mọi dòng log, response (`X-Request-Id`, `requestId`) và job dùng chung (DR-09, DOC-36 §3).
4. **Số đo ghi planned hay measured**: trong tài liệu này mọi ngưỡng và bucket là **dự kiến** cho tới khi EXP-05 đo.

## 2. Log

### 2.1 Định dạng

JSON một dòng mỗi bản ghi, ra stdout, dùng structured logging có sẵn của Spring Boot với `logging.structured.format.console=ecs` (DR-09). Docker thu qua log driver mặc định; xem bằng `make logs` (`docker compose logs -f api | jq`). Không ghi file, không thu gom tập trung ở giai đoạn này.

### 2.2 Trường

| Trường | Kiểu | Bắt buộc | Nguồn | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `@timestamp` | ISO 8601 UTC | Luôn | Logback | Thời điểm ghi |
| `log.level` | chuỗi | Luôn | Logback | `TRACE…ERROR` |
| `message` | chuỗi | Luôn | Mã gọi | Tiếng Anh, ngắn (DR-07); với lỗi nghiệp vụ `message` = mã lỗi (DOC-35 §5.2) |
| `log.logger` | chuỗi | Luôn | Logback | Tên class |
| `service.name` | chuỗi | Luôn | `logging.structured.ecs.service.name` | `ticket-api` |
| `trace_id` | chuỗi | Mọi dòng trong request; dòng job dùng ID sinh cho mỗi lượt chạy | MDC | Trùng `X-Request-Id`; job: `job-<tên>-<UUID ngắn>` |
| `user_id` | uuid | Khi đã đăng nhập | MDC | Không bao giờ là email |
| `event_id` | uuid | Khi dòng log liên quan một sự kiện | MDC | |
| `reservation_id` | uuid | Khi liên quan | MDC | |
| `order_id` | uuid | Khi liên quan | MDC | |
| `error.code` | chuỗi | Dòng log lỗi | `ApiExceptionHandler` | Mã ở DOC-35 §3 |
| `http.status` | số | Dòng log lỗi trong request | idem | |
| `dependency` | chuỗi | Lỗi `TRANSIENT` | idem | `postgres`, `redis`, `stripe`, `smtp`, `s3` |
| `duration_ms` | số | Dòng tổng kết và lỗi hạ tầng | Mã gọi | |
| `error.type`, `error.message`, `error.stack_trace` | chuỗi | Lớp `DEFECT` (và `TRANSIENT` ở DEBUG) | Logback | Không bao giờ chứa email hoặc body request |

Trường `event_id`, `reservation_id`, `order_id`, `user_id` được đặt vào MDC; định dạng ECS của Boot xuất khóa MDC thành trường cấp cao nên tên khớp DR-09. Điểm này phải kiểm ở P1-01 (test `OBS-01`); nếu Boot 4 đổi cách xuất, chỉnh bằng `logging.structured.json.rename`, không đổi tên trường ở tài liệu.

### 2.3 Đặt MDC

| Khóa | Đặt ở | Xóa khi |
| --- | --- | --- |
| `trace_id` | `RequestIdFilter` (đầu chuỗi filter, trước bảo mật): lấy `X-Request-Id` hợp lệ, không có thì tự sinh | Cuối request (`finally`) |
| `user_id` | `UserMdcFilter` sau xác thực session | Cuối request |
| `event_id`, `reservation_id`, `order_id` | Service bằng helper `MdcScope.with(key, value)` (try-with-resources) ngay khi đã biết ID | Hết khối `with` |
| Tất cả | Job: mỗi lượt chạy và mỗi dòng xử lý đặt `trace_id` và ID của dòng đang xử lý | Hết lượt, hết dòng |

Luồng bất đồng bộ không có (không `@Async`); thread của job và request không dùng chung MDC. Test `OBS-02`: hai request đồng thời không lẫn `user_id`.

### 2.4 Mức log

| Mức | Dùng cho | Ví dụ |
| --- | --- | --- |
| `ERROR` | Lỗi lập trình `DEFECT`; vi phạm bất biến; webhook không tìm thấy đơn; outbox `FAILED` | `INV: POOL_UNIT_COUNT count=1` |
| `WARN` | Lỗi hạ tầng tạm thời; 401, 403, `QUEUE_REQUIRED`, 413, `IDEMPOTENCY_KEY_REUSED`; webhook `charge.refunded`/`dispute.created`; hủy PaymentIntent lỗi | `dependency=redis duration_ms=200 error.code=OVERLOADED` |
| `INFO` | Sự kiện nghiệp vụ một dòng: giữ vé thành công, xác nhận đơn, trả vé theo lô, phát hành vé, đổi trạng thái sự kiện, kết quả `InvariantChecker` sạch; lỗi nghiệp vụ còn lại (409, 422, 429 `RATE_LIMITED`) | `reservation held units=2 duration_ms=14` |
| `DEBUG` | Chi tiết chẩn đoán (SQL claim, từng lượt `AdmissionTicker`); tắt mặc định | |

Request thành công của endpoint đọc **không** log từng dòng (tránh làm ngợp dưới tải); đã có `http_server_requests_seconds`. Mức đặt qua `logging.level.io.ticket` (mặc định `INFO`); thực nghiệm tải cao đặt `WARN` bằng `LOGGING_LEVEL_IO_TICKET=WARN` để log không thành nút cổ chai.

### 2.5 Không bao giờ có trong log

Email, token đăng nhập, `tb_session`, `X-CSRF-Token`, `clientSecret`, `whsec_`/`sk_`, body request, payload webhook thô, nội dung email. Phát hiện bằng test `OBS-03` (quét log của một lần chạy đầy đủ bằng regex email và secret; liên quan SEC-16). Khi cần tìm một người dùng, dùng `user_id`.

Ví dụ một dòng (xuống dòng để dễ đọc, thực tế một dòng):

```json
{
  "@timestamp": "2026-10-06T09:41:12.318Z",
  "log.level": "INFO",
  "message": "reservation held units=2 duration_ms=14",
  "log.logger": "io.ticket.reservation.service.HoldService",
  "service.name": "ticket-api",
  "trace_id": "7f3c9a1e5b2d4e8a",
  "user_id": "0192f7c0-1c3e-7a10-8d52-3f6b1c9a0b11",
  "event_id": "0192f7b8-52a4-7c3e-9a10-6e2f4d8c1b07",
  "reservation_id": "0192f7c1-0d4f-7b22-a3c1-9f5e6d7a8b90",
  "order_id": "0192f7c1-0d50-7e04-b1a7-2c3d4e5f6a7b"
}
```

## 3. Endpoint quản trị

| Endpoint | Cổng | Khi nào mở | Dùng cho |
| --- | --- | --- | --- |
| `/actuator/health` (`liveness`, `readiness`) | 9090 | Luôn | Healthcheck compose (DOC-62 §3) |
| `/actuator/prometheus` | 9090 | Profile Spring `obs` (và `make dev`) | Prometheus scrape mỗi 5 giây |

Không mở endpoint Actuator nào khác (`management.endpoints.web.exposure.include=health,prometheus`). Cổng 9090 không qua nginx và không publish ra host ở `make up` (SEC-23, DOC-32 §8). `readiness` gồm kiểm tra `db` và `redis`; Redis `DOWN` **không** làm `readiness` DOWN (hệ thống vẫn phục vụ, DR-56), chỉ hiện trong `health` chi tiết.

## 4. Danh mục metric

Micrometer + `micrometer-registry-prometheus`. Tên Prometheus dạng snake_case; kiểu: **C** counter, **G** gauge, **H** histogram. Metric riêng có tiền tố `ticket_`.

### 4.1 Metric của Spring và Hikari (dùng sẵn, chỉ bật)

| Metric | Kiểu | Label chính | Bật bằng | Dùng cho |
| --- | --- | --- | --- | --- |
| `http_server_requests_seconds` | H | `method`, `uri` (mẫu), `status`, `outcome` | `management.metrics.distribution.percentiles-histogram.http.server.requests=true`; `slo=…` ở 0.05, 0.1, 0.25, 0.5, 1, 2 giây | p95 theo endpoint (NFR-02), tỉ lệ 4xx/5xx, tổng nhịp request |
| `hikaricp_connections_active`, `_idle`, `_pending`, `_max`, `_min` | G | `pool` | Mặc định | Số connection đang dùng (EXP-05) |
| `hikaricp_connections_acquire_seconds`, `_usage_seconds`, `_timeout_total` | H/C | `pool` | Mặc định | Thời gian chờ và giữ connection |
| `jvm_memory_used_bytes`, `jvm_gc_pause_seconds`, `jvm_threads_live_threads`, `process_cpu_usage`, `system_cpu_usage` | G/H | | Mặc định | Giải thích điểm gãy (EXP-05) |
| `cache_gets_total{cache="session"}`, `cache_size{cache="session"}` | C/G | `cache`, `result` | `CaffeineCacheMetrics` cho cache session | Tỉ lệ trúng cache session (DR-22) |
| `logback_events_total` | C | `level` | Mặc định | Số `ERROR`/`WARN` theo thời gian |

### 4.2 Metric riêng

Cột "Nơi phát" là lớp (package `io.ticket.<module>`) chịu trách nhiệm tăng metric. Chỉ module sở hữu phát metric của mình.

| Metric | Kiểu | Label (tập giá trị) | Nơi phát | Ý nghĩa | Dùng cho |
| --- | --- | --- | --- | --- | --- |
| `ticket_hold_duration_seconds` | H | `model` ∈ {`GA`,`ZONE`,`SEAT`} | `reservation.service.HoldService` (bao quanh toàn bộ lệnh giữ vé từ lúc vào service tới lúc trả kết quả, **kể cả** khi lỗi) | Thời gian xử lý một lệnh giữ vé phía API. Bucket (giây): 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2, 5 — có mốc 0.5 để đọc trực tiếp p95 < 500 ms | NFR-02, EXP-02, EXP-05 |
| `ticket_hold_result_total` | C | `result` ∈ {`success`, `idempotent_replay`, `seats_unavailable`, `insufficient_capacity`, `queue_required`, `rate_limited`, `overloaded`, `not_on_sale`, `other`} | `HoldService` và `HoldController` (cho `rate_limited` từ bucket Redis) | Số lệnh giữ vé theo kết quả. `overloaded` gồm cả hết bulkhead lẫn hết `connection-timeout` | EXP-01, EXP-02, EXP-05, EXP-08 |
| `ticket_connection_hold_seconds` | H | `op` ∈ {`hold`, `cancel`, `confirm`, `release`, `webhook`, `studio`, `read`} | `common.db.ConnectionHoldTimer` (bao `DataSource`: đo từ lúc lấy connection tới lúc trả) | Thời gian mỗi request/job **giữ** một connection. Bucket: 0.002, 0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1, 2 | EXP-05 (trực tiếp), SDD gốc 10.4 |
| `ticket_hold_bulkhead_in_use` | G | — | `HoldService` (số permit đang giữ, tối đa `hold.bulkhead.permits`) | Mức dùng bulkhead (DR-61) | EXP-05 |
| `ticket_expiry_batch_size` | H | — | `reservation.service.ReleaseService` (mỗi lô job trả vé) | Số reservation mỗi lô; bucket 1, 5, 20, 50, 100, 200 (lô tối đa 200, DR-42) | EXP-04 |
| `ticket_expiry_lag_seconds` | H | — | `ReleaseService`: `now() − expires_at` khi unit về `AVAILABLE` | Trễ trả vé; bucket 1, 5, 10, 20, 30, 60, 120 | NFR-03, EXP-04 |
| `ticket_expiry_released_units_total` | C | — | `ReleaseService` | Tổng unit trả về kho bởi job | EXP-04 |
| `ticket_outbox_pending` | G | — | `notification.OutboxMetrics` (lấy mẫu mỗi 15 giây: `SELECT count(*) FROM outbox WHERE status='PENDING'`) | Số email chưa gửi | DR-53, EXP-08 |
| `ticket_outbox_oldest_pending_age_seconds` | G | — | idem (`now() − min(created_at)` của `PENDING`) | Độ trễ của email cũ nhất | DR-53 |
| `ticket_outbox_attempts_total` | C | `kind` ∈ {`EMAIL_TICKETS`, `EMAIL_EVENT_CHANGED`, `EMAIL_REFUND_PENDING`}, `result` ∈ {`sent`, `retry`, `failed`} | `notification.OutboxRelay` | Lượt gửi theo kết quả | DOC-27 |
| `ticket_queue_admitted` | G | — | `queue.AdmissionTicker` (lấy mẫu: tổng `ZCARD admitted:*` của các sự kiện đang bán) | Số người đang giữ lượt vào (`max_active` 500) | EXP-05, DR-57 |
| `ticket_queue_waiting` | G | — | idem (`ZCARD queue:*`) | Số người đang đợi trong hàng | EXP-02, EXP-05 |
| `ticket_queue_admit_total` | C | — | `AdmissionTicker` (số lượt cấp mỗi nhịp) | Nhịp vào cửa thực tế (so với `admit_rate` 50/giây, DR-60) | EXP-05 |
| `ticket_queue_left_total` | C | `reason` ∈ {`left`, `idle_timeout`, `pass_expired`} | `queue` | Người rời hàng | EXP-05, DR-57 |
| `ticket_ratelimit_total` | C | `bucket` ∈ {`hold`, `pi`, `queue`}, `result` ∈ {`allowed`, `denied`, `bypassed`} | `common.ratelimit.TokenBucket` (`bypassed` = Redis lỗi hoặc quá `spring.data.redis.timeout`, DR-56) | Quyết định của bucket người dùng | EXP-08, SEC-13 |
| `ticket_http_errors_total` | C | `code` ∈ bảng mã lỗi DOC-35 §3 (40 giá trị) | `common.error.ApiExceptionHandler` | Số response lỗi theo mã | DOC-35, EXP-02, EXP-08 |
| `ticket_dependency_failures_total` | C | `dependency` ∈ {`postgres`, `redis`, `stripe`, `smtp`, `s3`} | `ApiExceptionHandler` và adapter gọi ngoài | Lỗi hạ tầng tạm thời | EXP-08 |
| `ticket_payment_webhook_total` | C | `type` ∈ {`succeeded`, `payment_failed`, `canceled`, `charge_refunded`, `dispute_created`, `other`}, `outcome` ∈ {`confirmed`, `late_refund_pending`, `duplicate`, `ignored`, `amount_mismatch`, `failed`} | `payment.service.WebhookService` | Kết quả xử lý webhook (khớp `stripe_event.outcome`) | EXP-06, EXP-07 |
| `ticket_payment_reconcile_total` | C | `result` ∈ {`confirmed`, `canceled`, `still_pending`, `error`} | `payment.job.PaymentReconcileJob` | Kết quả đối chiếu webhook thất lạc (DR-49) | RB-03 |
| `ticket_payment_cancel_failures_total` | C | — | `ReleaseService` khi hủy PaymentIntent lỗi (reservation ở lại `EXPIRING`) | Hủy PaymentIntent thất bại | EXP-06, EXP-08 |
| `ticket_orders_refund_pending` | G | `reason` ∈ {`LATE_PAYMENT`, `EVENT_CANCELLED`, `AMOUNT_MISMATCH`} | `order.OrderMetrics` (mẫu 60 giây, `orders WHERE status='REFUND_PENDING'`) | Hàng chờ hoàn tiền thủ công (DR-44) | RB-01 |
| `ticket_auth_magic_link_total` | C | `result` ∈ {`sent`, `throttled`, `smtp_error`} | `auth.service.MagicLinkService` | Magic link theo kết quả (DR-21) | DOC-19 |
| `ticket_job_runs_total` | C | `job` ∈ {`outbox_relay`, `release_expired`, `payment_reconcile`, `admission_ticker`, `event_lifecycle`, `invariant_checker`, `retention`}, `result` ∈ {`ok`, `error`} | `common.job.JobMetrics` (bao `@Scheduled`) | Số lượt chạy job | Mọi EXP |
| `ticket_job_duration_seconds` | H | `job` (như trên) | idem | Thời gian một lượt chạy | EXP-04, EXP-05 |
| `ticket_invariant_violations` | G | `check` ∈ tên kiểm tra của DR-73 (ví dụ `POOL_UNIT_COUNT`) | `invariant.InvariantChecker` (giá trị lần chạy gần nhất; 0 khi sạch) | Số sai lệch theo mục | NFR-01, mọi EXP |
| `ticket_invariant_check_seconds` | H | — | idem | Thời gian một lần kiểm tra | DR-73 |
| `ticket_availability_snapshot_age_seconds` | G | — | `inventory.AvailabilitySnapshotCache` — **chỉ từ P5** (DR-62) | Tuổi snapshot tình trạng chỗ trong tiến trình | EXP-01 |

Sáu metric đầu của DR-09 (`ticket_hold_duration_seconds`, `ticket_hold_result_total`, `ticket_connection_hold_seconds`, `ticket_expiry_batch_size`, `ticket_outbox_pending`, `ticket_queue_admitted`) giữ nguyên tên. Metric còn lại do tài liệu này bổ sung (DR-120); `ticket_http_errors_total` và `ticket_dependency_failures_total` do DOC-35 đặt tên (DR-82…89).

Quy tắc phát metric:

- **Đếm ở một chỗ.** `ticket_hold_result_total` tăng đúng một lần mỗi lệnh giữ vé, ở `finally` của `HoldService` (hoặc của controller với `rate_limited`/`queue_required`/`overloaded` bị chặn trước khi vào service). Tổng các `result` = số request `POST /reservations` (test `OBS-06`).
- **`ticket_http_errors_total` là tổng quát, `ticket_hold_result_total` là riêng cho giữ vé**: một 409 `SEATS_UNAVAILABLE` tăng cả hai; không dùng cái nào để suy ra cái kia.
- **Gauge lấy mẫu** (`ticket_outbox_pending`, `ticket_queue_*`, `ticket_orders_refund_pending`) chạy bằng `@Scheduled` riêng, không truy vấn khi Prometheus scrape (scrape không được chạm DB).
- Mọi thao tác ghi metric chịu lỗi: lỗi của metric không bao giờ làm hỏng request (`try/catch` trong helper).

## 5. Prometheus và Grafana (profile `obs`)

`make up-obs` thêm hai container (DOC-62 §2, §7); NFR-08 (`docker compose up` không có hai container này) không đổi.

- `prometheus/prometheus.yml`: một job `ticket-api`, target `api:9090`, `metrics_path: /actuator/prometheus`, `scrape_interval: 5s`, `evaluation_interval: 15s`. Không có `rule_files`, không có Alertmanager (DR-09). Lưu giữ 2 ngày (`--storage.tsdb.retention.time=2d`).
- Grafana: datasource Prometheus và **một** dashboard `ticket-exp05.json` được provision từ `grafana/provisioning/` (DOC-62 §1). Đăng nhập ẩn danh quyền Viewer (`GF_AUTH_ANONYMOUS_ENABLED=true`) vì chỉ chạy trên máy cá nhân; cổng 3000.
- Prometheus xuất ra host ở cổng 9091 (DOC-61 §4) để người vận hành truy vấn tay.

### 5.1 Dashboard "Ticket · EXP-05" (planned, dựng ở P6-08)

Lấy mẫu làm mới 5 giây; hàng thứ nhất trả lời "hệ thống có giữ được p95 < 500 ms không", hàng thứ hai "vì sao".

| # | Hàng | Panel | Truy vấn PromQL (rút gọn) | Câu hỏi trả lời |
| --- | --- | --- | --- | --- |
| 1 | Kết quả | p95/p99 lệnh giữ vé | `histogram_quantile(0.95, sum by (le) (rate(ticket_hold_duration_seconds_bucket[30s])))` (và 0.99) | NFR-02: p95 < 500 ms? Đường ngưỡng 0.5 |
| 2 | Kết quả | Nhịp giữ vé theo kết quả | `sum by (result) (rate(ticket_hold_result_total[10s]))` (xếp chồng) | Bao nhiêu lệnh/giây thành công, bị từ chối vì sao |
| 3 | Kết quả | Tỉ lệ lỗi theo mã | `sum by (code) (rate(ticket_http_errors_total[10s]))` | 5xx ngoài `OVERLOADED` có xuất hiện không (NFR-02 "không sập") |
| 4 | Connection | Connection đang dùng / chờ / tối đa | `hikaricp_connections_active`, `hikaricp_connections_pending`, `hikaricp_connections_max` | Pool có cạn không (EXP-05) |
| 5 | Connection | Thời gian giữ connection p50/p95 theo `op` | `histogram_quantile(0.95, sum by (le, op) (rate(ticket_connection_hold_seconds_bucket[30s])))` | Request giữ connection bao lâu: gốc của điểm gãy (SDD gốc 10.4) |
| 6 | Connection | Bulkhead đang dùng | `ticket_hold_bulkhead_in_use` so với `hold.bulkhead.permits` (24) | Có chạm bulkhead trước pool không |
| 7 | Phòng chờ | `admitted`, `waiting` | `ticket_queue_admitted`, `ticket_queue_waiting` | Phòng chờ giữ ở `max_active` 500? |
| 8 | Phòng chờ | Nhịp vào cửa thực tế và người rời hàng | `rate(ticket_queue_admit_total[10s])`, `sum by (reason) (rate(ticket_queue_left_total[10s]))` | `admit_rate` thực có bằng cấu hình (DR-60)? |
| 9 | Phòng chờ | Quyết định bucket người dùng | `sum by (bucket, result) (rate(ticket_ratelimit_total[10s]))` | Bao nhiêu lệnh bị `denied`; `bypassed` > 0 nghĩa là Redis lỗi |
| 10 | Tài nguyên | CPU và GC | `process_cpu_usage`, `rate(jvm_gc_pause_seconds_sum[10s])` | Điểm gãy do CPU/GC hay do DB |
| 11 | Tài nguyên | Tuổi dòng đợi của job | `ticket_expiry_lag_seconds` p95, `ticket_expiry_batch_size` p95, `ticket_outbox_pending` | Job nền có theo kịp dưới tải (EXP-04, NFR-03) |
| 12 | Bất biến | Vi phạm bất biến | `sum(ticket_invariant_violations)` | Phải luôn 0 (NFR-01) |

Dashboard không hiển thị mọi metric; những metric không có panel (`ticket_payment_*`, `ticket_orders_refund_pending`, `ticket_auth_magic_link_total`) truy vấn tay ở cổng 9091 khi chạy EXP-06/07 hoặc RB-01. Số liệu thật của thực nghiệm lưu ở file kết quả của từng EXP; ảnh chụp dashboard là phụ lục, không phải nguồn.

## 6. Cấu hình liên quan

Định nghĩa chính thức ở DOC-34 (khung P1). Khóa tài liệu này dùng:

| Key | Mặc định | Profile | Ý nghĩa |
| --- | --- | --- | --- |
| `logging.structured.format.console` | `ecs` | mọi | Định dạng log JSON |
| `logging.level.io.ticket` | `INFO` | mọi (`WARN` ở `make exp`) | Mức log ứng dụng |
| `management.server.port` | `9090` | mọi | Cổng quản trị (DR-80) |
| `management.endpoints.web.exposure.include` | `health` (`health,prometheus` ở `obs`, `dev`) | | Endpoint Actuator |
| `management.metrics.distribution.percentiles-histogram.http.server.requests` | `true` | mọi | Histogram cho p95 |
| `ticket.metrics.sampler-interval` | `PT15S` | mọi | Nhịp lấy mẫu gauge (`ticket_outbox_pending`, `ticket_queue_*`) (DR-120) |
| `ticket.metrics.refund-pending-interval` | `PT60S` | mọi | Nhịp lấy mẫu `ticket_orders_refund_pending` |

## 7. Test bắt buộc (tiền tố `OBS-`)

Tiền tố `OBS-` dành cho tài liệu này.

| ID | Tình huống | Kết quả mong đợi |
| --- | --- | --- |
| OBS-01 | Gọi `GET /events` kèm `X-Request-Id: 7f3c9a1e-5b2d-4e8a` đã đăng nhập; đọc dòng log của request qua `jq` | Có `@timestamp`, `log.level`, `message`, `trace_id` = `7f3c9a1e-5b2d-4e8a`, `user_id`; `trace_id` là khóa cấp cao (không lồng trong `mdc`) |
| OBS-02 | 50 request song song của 5 người dùng khác nhau | Mọi dòng log của một request có đúng `user_id` của request đó; sau request, MDC trống |
| OBS-03 | Chạy luồng mua vé đầy đủ (magic link, giữ vé, thanh toán fake, email); quét log bằng regex email, `tb_session`, `whsec_`, `sk_`, `clientSecret` | 0 kết quả |
| OBS-04 | Response lỗi 409 `SEATS_UNAVAILABLE` | `requestId` trong body = header `X-Request-Id` = `trace_id` của dòng log INFO tương ứng, có `error.code` |
| OBS-05 | `curl localhost:9090/actuator/prometheus` (profile `obs`) | 200; có `http_server_requests_seconds_bucket`, `hikaricp_connections_active`, và đủ 6 metric của DR-09 |
| OBS-06 | 100 lệnh giữ vé: 60 thành công, 30 `SEATS_UNAVAILABLE`, 10 thiếu `Idempotency-Key` bị từ chối | `sum(ticket_hold_result_total)` tăng 90 (lệnh thiếu key bị chặn trước service và không thuộc `ticket_hold_result_total`; ghi ở `ticket_http_errors_total{code="IDEMPOTENCY_KEY_REQUIRED"}` = 10); `result="success"` = 60 |
| OBS-07 | Chạy lệnh giữ vé với bulkhead 1 permit và 2 request đồng thời | Một thành công, một `overloaded`; `ticket_hold_bulkhead_in_use` về 0 sau đó |
| OBS-08 | Tắt Redis rồi gọi lệnh giữ vé sự kiện thường | `ticket_ratelimit_total{result="bypassed"}` tăng; `ticket_dependency_failures_total{dependency="redis"}` tăng; request vẫn thành công |
| OBS-09 | Tạo 5 dòng outbox `PENDING`, tắt SMTP | `ticket_outbox_pending` = 5 sau ≤ 15 giây (`sampler-interval`); `ticket_outbox_oldest_pending_age_seconds` tăng theo thời gian |
| OBS-10 | Cho `ReleaseService` trả 3 reservation quá hạn 20 giây trước | `ticket_expiry_batch_size_count` +1, `_sum` +3; `ticket_expiry_lag_seconds` có mẫu ≥ 20 |
| OBS-11 | Làm sai bất biến bằng SQL (đổi một unit `HELD` thành `AVAILABLE` còn `reservation_id`... không hợp lệ; thay bằng `DELETE` một unit của pool) rồi chạy `InvariantChecker` | `ticket_invariant_violations{check="POOL_UNIT_COUNT"}` = 1; chạy lại sau khi sửa → 0 |
| OBS-12 | Quét `/actuator/prometheus` sau 1.000 request từ 20 người dùng | Không có label nào chứa UUID, email hoặc IP; số chuỗi thời gian (series) của mọi metric `ticket_*` ≤ 400 |
| OBS-13 | Khởi động profile `obs`; mở `localhost:3000` | Dashboard "Ticket · EXP-05" có đủ 12 panel §5.1, mỗi panel có dữ liệu sau khi chạy một kịch bản giữ vé ngắn |
| OBS-14 | `curl localhost:8080/actuator/prometheus` (qua nginx) | 404 (cùng SEC-23) |

## 8. Quyết định phát sinh khi viết tài liệu này

Mọi quyết định do Claude chốt (Owner ủy quyền); chờ số DR thật khi gộp.

| ID | Quyết định | Lý do |
| --- | --- | --- |
| DR-120 | Bổ sung metric ngoài sáu metric của DR-09 (bảng §4.2), gồm `ticket_hold_bulkhead_in_use`, `ticket_expiry_lag_seconds`, `ticket_queue_waiting`, `ticket_queue_admit_total`, `ticket_ratelimit_total`, `ticket_payment_*`, `ticket_job_*`, `ticket_invariant_*`, `ticket_orders_refund_pending`; hai key lấy mẫu `ticket.metrics.sampler-interval`, `ticket.metrics.refund-pending-interval`; tên dashboard `ticket-exp05` với 12 panel | DR-09 chỉ liệt kê sáu metric đủ để vẽ p95 và connection; EXP-02/04/06/08 và NFR-03 cần trễ trả vé, nhịp vào cửa, kết quả webhook, số sai lệch |
| DR-121 | Label không bao giờ chứa ID, email, IP hoặc đường dẫn có tham số; mọi label lấy từ tập đóng ghi ở §4.2; test `OBS-12` đếm series | Giữ Prometheus nhẹ khi 100.000 người dùng ảo và không lộ dữ liệu cá nhân |
| DR-122 | Redis `DOWN` không làm `readiness` DOWN | DR-56: hệ thống tiếp tục phục vụ khi mất Redis; compose không được khởi động lại `api` vì Redis |

## Câu hỏi còn mở

Không có. (Việc xác nhận Boot 4 xuất khóa MDC thành trường cấp cao là kiểm tra của `OBS-01` ở P1-01; nếu sai chỉ cần cấu hình đổi tên.)
