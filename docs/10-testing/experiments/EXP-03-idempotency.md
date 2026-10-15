# EXP-03 · Idempotency

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-73 / EXP-03
> Phụ thuộc: SDD gốc §8.6, §10.5, §15.2, [Sổ quyết định](../../00-decision-register.md) (DR-41, 43, 45, 61, 75, 174, 175, 176, 177), [DOC-70](README.md) (giao thức chung, DR-204…213), [DOC-03](../../01-product/requirements.md) (FR-08, NFR-04), [DOC-25](../../06-design/idempotency.md) §3, §4, §6, §8, [DOC-24](../../06-design/inventory-and-reservation.md) §4.1, §4.6, §7.2, [DOC-27](../../06-design/tickets-and-notifications.md) (`EMAIL_TICKETS`), [DOC-15](../../05-data/ops-model.md) §5 (`idempotency_key`), §7 (`outbox`), [DOC-30](../../06-design/invariant-checker.md) (INV-05, INV-06, INV-10), [DOC-33](../../06-design/observability.md) §4.2, [DOC-37](../../07-api/api-endpoints.md) (E-14, E-16, E-18), [EXP-02](EXP-02-pool-contention.md) (DR-214)
> Người dùng chính: P2-13 (`load/exp03.ts`, `experiments/exp03/params.env`), P2-14 (chạy), M2

Thực nghiệm kiểm chứng NFR-04 dưới tải: gửi lại cùng `Idempotency-Key` **tuần tự và song song** cho ba lệnh ghi có lưu key — giữ vé (E-14, `HOLD`), hủy giữ vé (E-16, `CANCEL_HOLD`), nhận vé đơn 0 đồng (E-18, `CONFIRM_FREE`) — chỉ tạo đúng một tác dụng mỗi key và mọi response 2xx của một key khớp nhau. Giao thức, khóa và hành vi song song ở DOC-25; test đơn lẻ IDEM-02…11 ở DOC-25 §8 kiểm cùng tính chất ở mức tích hợp, EXP-03 kiểm nó khi hàng nghìn người cùng gửi lại, có bulkhead, 503 và lỗi mạng thật. Máy, quy trình `make exp`, công thức `M…`, quy tắc hợp lệ và lưu kết quả ở [DOC-70](README.md). E-17 (`payment-intent`) không lưu key (DR-47) và thuộc EXP-06/07 (P3). Mọi con số ở đây là **kế hoạch** (planned) cho tới khi §11 có số đo.

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi thành DR-217 và tóm tắt ở mục "Quyết định phát sinh khi viết tài liệu này".

## 1. Giả thuyết

| # | Giả thuyết | Đo bằng |
| --- | --- | --- |
| H1 (NFR-04, FR-08.2) | Mỗi key `HOLD` tạo đúng một reservation, dù gửi lại 5 lần tuần tự, 20 lần song song hay sau khi mất response | Y1, Y3, Y6 |
| H2 (FR-08.3, DR-176) | Mọi response 2xx của một key `HOLD` hoặc `CONFIRM_FREE` bằng nhau về JSON; đúng một response không có `Idempotent-Replayed` (hoặc không có cái nào nếu response đầu bị mất); các response còn lại có `Idempotent-Replayed: true` | Z1, Z2 |
| H3 (DR-175) | Mỗi key `CANCEL_HOLD` chuyển reservation `ACTIVE → EXPIRING` đúng một lần; mọi response là 200 `CANCELLED` hoặc 202 `EXPIRING`, không có 409; unit được trả đúng một lần | Y2, Y5, Z1, Z3 |
| H4 (FR-08.3) | Mỗi key `CONFIRM_FREE` phát hành đúng Σ `quantity` vé và đúng một dòng outbox `EMAIL_TICKETS` | Y4, M14 (INV-06, INV-10) |
| H5 (FR-08.5, DR-45) | Cùng key với body khác, hoặc dùng ở endpoint khác, nhận 422 `IDEMPOTENCY_KEY_REUSED` và không đổi gì | Z4, Y6 |
| H6 | Gửi lại không bao giờ biến thành lỗi nghiệp vụ người dùng thấy: 0 response 409 `ACTIVE_RESERVATION_EXISTS` hoặc `RESERVATION_NOT_ACTIVE` | Z3 |

Thiếu idempotency, H6 là chỗ lộ ra trước tiên: lần gửi lại E-14 sẽ đâm vào `reservation_open_uq` và nhận 409 `ACTIVE_RESERVATION_EXISTS`, lần gửi lại E-18 sẽ nhận 409 `RESERVATION_NOT_ACTIVE` vì reservation đã `CONFIRMED` (DOC-24 §7.2). Index một phần chặn được reservation thứ hai, nhưng không trả lại được response của lần đầu.

## 2. Biến

| Loại | Biến | Giá trị |
| --- | --- | --- |
| Độc lập | Nhóm người dùng (cách gửi lại) | A, B, C, D (§5.3) |
| Phụ thuộc | Tính đúng | Y1…Y6, Z1…Z4, M7, M14 |
| Phụ thuộc | Hiệu năng (tham khảo) | M1, M8, M9, M10, M13; Z5 (p95 lần phát lại so với lần chạy thật) |
| Kiểm soát | Chung | DOC-70 §6.1; `STRATEGY = skip-locked` |
| Kiểm soát | Riêng | Một loại `GA` giá 0 với sức chứa 20.000 (gấp 4 × `USERS`, không lệnh nào thiếu vé); nhóm và số vé của người dùng `n` tất định theo `n` (DR-214); `reservation.hold-duration` mặc định 10 phút |

## 3. Baseline

Không có bản cài đặt thay thế: idempotency bắt buộc ở E-14, E-16, E-18 (DR-45) và không có cấu hình tắt nó; EXP-03 chỉ chạy `skip-locked` (DOC-70 §2). Baseline là **kỳ vọng lý thuyết khi thiếu idempotency** ghi ở H6: mỗi lần gửi lại sau lần đầu thành công là một 409. Số đo Z3 = 0 so với baseline đó (≈ 63.500 lần gửi lại `HOLD` và `CONFIRM_FREE` thành 409 ở `USERS = 5.000`: A 15, B 21, C 2 mỗi người; §5.3) là bằng chứng.

## 4. Môi trường

Theo DOC-70 §3. Không có điểm khác.

## 5. Các bước

### 5.1 Tham số: `experiments/exp03/params.env`

```bash
# experiments/exp03/params.env — DOC-73 §5.1
USERS=5000              # × SCALE
CAPACITY=20000          # × SCALE; 4 × USERS: không lệnh nào thiếu vé
ARRIVAL_WINDOW_S=60     # thời điểm đến đều trong [0, W), như M-B
VUS=50                  # 100 người dùng ảo mỗi VU
MAX_DURATION_S=300
DRAIN_S=40              # đủ cho ReleaseExpiredHoldsJob lấy lại reservation EXPIRING bỏ dở (lease 30 giây)
SEQ_RESENDS=4           # nhóm A: gửi lại tuần tự sau lần đầu
HOLD_PAR=20             # nhóm B: số request song song cùng key giữ vé (như IDEM-02)
CANCEL_PAR=10           # nhóm B: số request song song cùng key hủy
CONFIRM_PAR=10          # nhóm A: số request song song cùng key nhận vé
TAIL_RESENDS=2          # gửi lại tuần tự sau đợt song song
LOST_TIMEOUT_MS=20      # nhóm C: timeout phía client của lần gửi đầu, ngắn hơn transaction (≈ 50 ms)
API_OVERRIDES='{}'
```

`SCALE` nhân `USERS` và `CAPACITY`; `SCALE = 0,1` (500 người dùng) đủ cho thử script trên máy dev.

### 5.2 Fixture

Theo DOC-70 §5.2 với một loại vé `name = "EXP-03 GA"`, `model = GA`, `price = 0`, `capacity = CAPACITY × SCALE`.

### 5.3 Nhóm người dùng và hành vi: `load/exp03.ts`

Người dùng `n` thuộc nhóm theo `g = uniform(n, 'exp03-group')` và xin `q_n = 1 + ⌊4 × uniform(n, 'exp03-qty')⌋` vé (hàm `uniform` của DR-214); đến ở `T0 + W × uniform(n, 'exp03-arrival')`.

| Nhóm | `g` | Tỉ lệ | Chuỗi lệnh (mỗi dòng một key) | Trạng thái cuối kỳ vọng |
| --- | --- | --- | --- | --- |
| A · tuần tự | [0; 0,4) | 40 % | 1. `HOLD` key K₁: gửi, chờ response, gửi lại `SEQ_RESENDS` = 4 lần tuần tự. 2. `CONFIRM_FREE` key K₂: `CONFIRM_PAR` = 10 request song song, rồi `TAIL_RESENDS` = 2 lần tuần tự | reservation `CONFIRMED`, đơn `PAID`, `q_n` vé `ISSUED` |
| B · song song | [0,4; 0,7) | 30 % | 1. `HOLD` key K₁: `HOLD_PAR` = 20 request song song, rồi 2 lần tuần tự. 2. `CANCEL_HOLD` key K₂: `CANCEL_PAR` = 10 song song, rồi 2 lần tuần tự | reservation `CANCELLED`, đơn `CANCELLED`, unit `AVAILABLE` |
| C · mất response | [0,7; 0,9) | 20 % | 1. `HOLD` key K₁: lần đầu với `timeout = LOST_TIMEOUT_MS`, rồi gửi lại cùng key (timeout 10 giây) tới khi có 2xx. 2. `CONFIRM_FREE` key K₂: như bước 1 | reservation `CONFIRMED`, đơn `PAID` |
| D · dùng sai key | [0,9; 1) | 10 % | 1. `HOLD` key K₁ body `quantity = q_n` → 201. 2. Cùng K₁, body `quantity = q_n mod 4 + 1` (khác q_n) → 422. 3. `CONFIRM_FREE` với K₁ (khác endpoint) → 422. 4. `CONFIRM_FREE` key K₂ → 200 | reservation `CONFIRMED` với `q_n` vé, đơn `PAID` |

Ở `USERS = 5.000` (planned): A ≈ 2.000, B ≈ 1.500, C ≈ 1.000, D ≈ 500 người; ≈ 2.000 × 17 + 1.500 × 34 + 1.000 × 4 + 500 × 4 ≈ **91.000 request** trong ≈ 70 giây, trong đó ≈ 81.000 là gửi lại (mỗi người hai key, mỗi key một lần gửi đầu). Kích thước nhóm thật do k6 đếm (`exp03_users{group}`) và dùng làm số kỳ vọng ở §6.

Quy tắc chung (DOC-70 §4.1, DOC-25 §3.6):

1. Một key = một chuỗi body cố định (`JSON.stringify` một lần); mọi lần gửi lại của key đó, kể cả gửi lại do 503 `OVERLOADED`, 429 hay lỗi mạng (`status 0`), dùng đúng chuỗi đó. Nhóm D bước 2 là ngoại lệ có chủ đích.
2. "Song song" là `Promise.all` của N `http.asyncRequest` phát cùng một tick của event loop; mỗi request tự gửi lại theo quy tắc 503/lỗi mạng cho tới khi có response cuối (2xx, 409, 422).
3. 503 trong đợt song song là **kỳ vọng**: các request cùng key chờ khóa dòng `idempotency_key` trong khi giữ permit bulkhead (DOC-25 §4.1), nên đợt 20 request của nhóm B ăn gần hết 24 permit. Chúng được gửi lại cùng key và kết thúc bằng phát lại (IDEM-08).
4. Nhóm C: lần đầu bị k6 hủy sau 20 ms (`status 0`); nginx đóng kết nối upstream, còn transaction ở `api` có thể đã commit hoặc chưa. Cả hai trường hợp đều hợp lệ: lần gửi lại hoặc chạy thật, hoặc phát lại (DOC-25 §4.3).

So sánh response trong chuỗi của một key (k6, không cần file trung gian):

| Lệnh | Hai response 2xx của cùng key "khớp" khi | Số response không có `Idempotent-Replayed` |
| --- | --- | --- |
| `HOLD` | Bằng nhau về JSON (so sánh sâu sau `JSON.parse`, DR-176) | ≤ 1 (nhóm A, B, D: đúng 1; nhóm C: 0 hoặc 1) |
| `CONFIRM_FREE` | Bằng nhau về JSON (cùng `orderId`, `status = "PAID"`, cùng danh sách `tickets[].code`) | ≤ 1 |
| `CANCEL_HOLD` | Cùng `reservationId`; `status` ∈ {`EXPIRING`, `CANCELLED`} với HTTP 202/200 tương ứng; dãy response theo thời gian không đi lùi từ `CANCELLED` về `EXPIRING` (response tính từ trạng thái hiện tại, DR-175) | ≤ 1 |

Sai lệch tăng counter `exp03_mismatch{op}`; mã lỗi ngoài kỳ vọng tăng `exp03_unexpected{op, code}`.

### 5.4 Lệnh chạy

```bash
make exp EXP=03                          # 3 lần, skip-locked, SCALE=1 (P2-14)
make exp EXP=03 SCALE=0.1 RUNS=1         # thử script (P2-13), không ghi vào §11
```

## 6. Số đo và công thức

Dùng của DOC-70 §8.2: M1 (với tag `name` ∈ {`hold`, `cancel`, `confirm_free`}, mỗi tag một giá trị), M7, M8, M9, M10, M13, M14. `:e` là `eventId`, `:p` là `poolId`, `:t0` là `T0` (epoch giây) của `fixture.json`; lọc theo `created_at >= to_timestamp(:t0)` để loại pha khởi động (DOC-70 §5.2 bước 4 dùng người dùng `n = 1…1000` trên sự kiện khác, trước `T0`).

**Phía database (đo sau bước xả):**

| # | Đại lượng | Kỳ vọng | Câu SQL |
| --- | --- | --- | --- |
| Y1 | Người dùng có số reservation ≠ 1 | 0 | `SELECT count(*) FROM (SELECT u.user_id FROM app_user u LEFT JOIN reservation r ON r.user_id = u.user_id AND r.event_id = :e WHERE u.email LIKE 'exp-%@load.test' AND u.email <> 'exp-000000@load.test' AND u.email <= format('exp-%s@load.test', lpad(:users::text, 6, '0')) GROUP BY u.user_id HAVING count(r.reservation_id) <> 1) x` |
| Y2 | Reservation theo trạng thái | `CONFIRMED` = \|A\|+\|C\|+\|D\|; `CANCELLED` = \|B\|; khác = 0 | `SELECT status, count(*) FROM reservation WHERE event_id = :e GROUP BY status` |
| Y3 | Dòng `idempotency_key` theo `operation` | `HOLD` = `USERS`; `CANCEL_HOLD` = \|B\|; `CONFIRM_FREE` = \|A\|+\|C\|+\|D\|; mọi dòng `response_status` ∈ 200…299 | `SELECT operation, count(*), count(*) FILTER (WHERE response_status NOT BETWEEN 200 AND 299 OR response_status IS NULL) AS bad FROM idempotency_key WHERE created_at >= to_timestamp(:t0) GROUP BY operation` |
| Y4 | Đơn `PAID` có số dòng outbox `EMAIL_TICKETS` ≠ 1 | 0 | `SELECT count(*) FROM (SELECT o.order_id FROM orders o LEFT JOIN outbox x ON x.kind = 'EMAIL_TICKETS' AND x.payload->>'orderId' = o.order_id::text WHERE o.event_id = :e AND o.status = 'PAID' GROUP BY o.order_id HAVING count(x.outbox_id) <> 1) z` (khóa `orderId` của payload theo DOC-27) |
| Y5 | Unit theo trạng thái | `SOLD` = Σ `q_n` của A, C, D (k6 đếm `exp03_expected_sold`); `HELD` = 0; `AVAILABLE` = `CAPACITY × SCALE` − `SOLD` | `SELECT status, count(*) FROM inventory_unit WHERE pool_id = :p GROUP BY status` |
| Y6 | Metric idempotency | `executed`: `HOLD` = `USERS`, `CANCEL_HOLD` = \|B\|, `CONFIRM_FREE` = \|A\|+\|C\|+\|D\|; `reused`: `HOLD` = \|D\|, `CONFIRM_FREE` = \|D\|; `replayed` = Z2 theo từng `operation` | `sum by (operation, result) (increase(ticket_idempotency_requests_total[R]))` (DR-177; pha khởi động nằm ngoài `R`) |

Số vé phát hành đúng Σ `quantity` của mỗi đơn được kiểm bởi M14 qua INV-06 và INV-10 (DOC-30), không lặp ở đây.

**Phía k6:**

| # | Đại lượng | Kỳ vọng | Nguồn |
| --- | --- | --- | --- |
| Z1 | Chuỗi key có response không khớp (§5.3) | 0 | `metrics.exp03_mismatch.values.count`, tách theo `op` bằng threshold giả |
| Z2 | Response 2xx có `Idempotent-Replayed: true` | Tham khảo; khớp Y6 `replayed` | `exp_http_outcome` thêm tag `replayed` (`true`/`false`) cho request có `Idempotency-Key` |
| Z3 | Response 409 `ACTIVE_RESERVATION_EXISTS`, `RESERVATION_NOT_ACTIVE`, `PAYMENT_ALREADY_SUCCEEDED`; 422 ngoài nhóm D | 0 | `exp03_unexpected` |
| Z4 | 422 `IDEMPOTENCY_KEY_REUSED` | = 2 × \|D\| (bước 2 và 3 của nhóm D) | `exp_http_outcome{code:IDEMPOTENCY_KEY_REUSED}` |
| Z5 | p95 lần phát lại và lần chạy thật của `HOLD` | Tham khảo: phát lại nhanh hơn (dừng sau câu đầu, DOC-25 §3.2) | Trend `exp03_hold_duration{replayed}` ghi `res.timings.duration` sau khi đọc header |

`collect.sh` chạy Y1…Y6 khi `EXP = 03`; Z1…Z5 lấy từ `k6-summary.json`.

## 7. Tiêu chí đạt

**Tính đúng (mọi lần hợp lệ):** Y1 = 0; Y2, Y3, Y5, Y6 đúng như cột kỳ vọng; Y4 = 0; Z1 = 0; Z3 = 0; Z4 = 2 × \|D\|; M7 = 0 (`status 0` chỉ ở lần đầu của nhóm C, không tính là lỗi); M14 thoát 0.

**Hiệu năng:** không có ngưỡng. M8 (503) cao trong đợt song song là hành vi thiết kế (§5.3 quy tắc 3) và được báo để đọc kết quả.

Tiêu chí thoát M2 (master plan §5 Phase 2: "1 reservation mỗi key") ứng với Y1, Y3 dòng `HOLD` và Y6 `executed{HOLD}`.

## 8. Phân tích

Mục "Phân tích" của §11 trả lời:

1. H1…H6 đạt hay không ở từng lần; nếu Z1 > 0, ghi `op`, nhóm và cặp response khác nhau (từ `raw/k6.ndjson`).
2. Nhóm C: tỉ lệ lần đầu đã commit trước khi bị hủy (= số key `HOLD` nhóm C mà lần gửi lại đầu tiên nhận `Idempotent-Replayed: true`) — cho biết nginx và Tomcat xử lý client bỏ ngang thế nào.
3. Nhóm B: bao nhiêu request song song phải đi qua 503 trước khi được phát lại (M8 theo tag `hold`, `cancel`); có request nào chờ đủ `lock_timeout` 1 giây không (IDEM-08).
4. `CANCEL_HOLD`: tỉ lệ response 202 `EXPIRING` so với 200 `CANCELLED` trong đợt song song (request phát lại đến khi trả vé chưa xong), và mọi reservation nhóm B đã `CANCELLED` sau bước xả (Y2).
5. Z5: phát lại rẻ hơn chạy thật bao nhiêu; M9 `op="hold"` có bị kéo dài bởi thời gian chờ khóa key không.

## 9. Mẫu bảng kết quả

Theo DOC-70 §11 với năm cột bắt buộc, rồi các cột riêng:

| Lần | Thời điểm (UTC) | SHA | Hợp lệ | Bất biến | Người dùng A/B/C/D | Y1 | Y2 `CONFIRMED`/`CANCELLED`/khác | Y3 `HOLD`/`CANCEL`/`CONFIRM` | Y4 | Y5 `SOLD`/`HELD` | Z1 | Z3 | Z4 | Phát lại (Z2) | 503 (%) | 5xx khác | p95 giữ chạy thật / phát lại (ms) | p95 hủy (ms) | p95 nhận vé (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| r1 | | | | | | | | | | | | | | | | | | | |

Dòng "Tổng hợp" theo DOC-70 §8.3: cột tính đúng ghi cả 3 giá trị, cột p95 ghi trung vị (min–max).

## 10. Mối đe dọa độ tin cậy

Chung ở DOC-70 §13. Riêng EXP-03:

| # | Mối đe dọa | Ảnh hưởng | Giảm thiểu |
| --- | --- | --- | --- |
| E1 | "Song song" trong k6 là các request phát cùng tick nhưng đi qua một tiến trình và nginx; không bảo đảm chúng chạm PostgreSQL cùng lúc | Ít va chạm trên khóa key hơn kịch bản xấu nhất | Đợt 20 request làm nhiều request chờ khóa thật (Z5, M9); IDEM-02 và IDEM-08 kiểm va chạm chắc chắn bằng `CountDownLatch` và `pg_sleep` |
| E2 | Lần đầu của nhóm C có thể bị hủy trước khi tới `api` (20 ms vẫn đủ để nginx chưa chuyển) | Ít ca "commit rồi mất response" hơn mong muốn | Phân tích mục 2 báo tỉ lệ thật; IDEM-04 kiểm ca này chắc chắn |
| E3 | Mỗi người dùng chỉ giữ một lần | Không đo cùng người gửi hai key khác nhau đua nhau | Thuộc INVT-06 (DOC-24) |
| E4 | Không có sự kiện hết hạn hay xóa key trong cửa sổ đo | Không kiểm key quá 24 giờ | Thuộc IDEM-06 |
| E5 | Đơn 0 đồng; chưa có thanh toán thật | Không kiểm E-17 | E-17 không lưu key (DR-47); EXP-06, EXP-07 ở P3 |

## 11. Kết quả

Chưa chạy.

## Quyết định phát sinh khi viết tài liệu này

**DR-217 · Nhóm gửi lại và cách so response trong EXP-03.** *Vấn đề:* yêu cầu "gửi lại tuần tự và song song, gồm cả hủy giữ và nhận vé 0 đồng" (master plan §3.2) và SDD gốc §15.2 "gửi lại một phần request" không nói tỉ lệ, số lần, cách giả lập mất response, hay "đúng" được kiểm ở đâu khi response của `CANCEL_HOLD` thay đổi theo trạng thái (DR-175). *Quyết định:* bốn nhóm tất định theo `n` (A tuần tự 40 %, B song song 30 %, C mất response bằng timeout client 20 ms 20 %, D dùng sai key 10 %; §5.3); mỗi người dùng ảo tự so mọi response 2xx của từng key trong chuỗi của mình (bằng nhau về JSON với `HOLD`, `CONFIRM_FREE`; không đi lùi trạng thái với `CANCEL_HOLD`) và báo counter `exp03_mismatch`, `exp03_unexpected`; database kiểm bằng Y1…Y6. Sức chứa gấp 4 lần số người để không lệnh nào thua vì hết vé. *Hệ quả:* EXP-03 không cần file trung gian ghi response; tag `replayed` thêm vào `exp_http_outcome` của DOC-70 §4.1 cho request có key. *Ghi vào:* DOC-73, DOC-70 §4.1 (tag `replayed` của `record()`).

## 12. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-217 (mục trên).
