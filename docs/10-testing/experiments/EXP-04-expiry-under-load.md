# EXP-04 · Hết hạn dưới tải

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-74 / EXP-04
> Phụ thuộc: SDD gốc §8.4, §8.5, §15.2, [Sổ quyết định](../../00-decision-register.md) (DR-42, 43, 75, 76, 169, 170, 181, 182), [DOC-03](../../01-product/requirements.md) (NFR-01, NFR-03, FR-07.2, FR-07.3, FR-07.8), [DOC-06](../../02-glossary.md), [DOC-10](../../03-architecture/quality-attributes.md) §8, [DOC-14](../../05-data/domain-model.md) (`reservation`, `inventory_unit`), [DOC-24](../../06-design/inventory-and-reservation.md) §4.4, §4.5, §7.3, §8, [DOC-30](../../06-design/invariant-checker.md) (INV-04, 05, 08, 09), [DOC-33](../../06-design/observability.md) §4.2, [DOC-34](../../06-design/configuration-reference.md) §3.4, [DOC-37](../../07-api/api-endpoints.md) (E-14, E-18), [DOC-62](../../09-operations/deploy-compose.md) §4, §8, [DOC-70](README.md), [ADR-0004](../../04-adr/0004-reservation-row-as-arbiter-expiring-state.md)
> Người dùng chính: P2-13 (dựng `experiments/exp04/`, `load/exp04.ts`), P2-14 (chạy), Owner khi chốt DR-182, P2-09 (job trả vé: số kế hoạch ở DOC-24 §4.5 được thay bằng số đo ở đây)

Thực nghiệm kiểm chứng NFR-03 (vé hết hạn về kho ≤ 30 giây sau `expires_at`) và FR-07.2, FR-07.3, FR-07.8 khi **5.000 reservation hết hạn cùng một giây** trong lúc người mua khác vẫn giữ vé trên cùng pool, và khi `api` bị giết giữa lúc job đang trả vé. Tài liệu chỉ ghi phần riêng của EXP-04; máy, giới hạn container, seed, quy trình `make exp`, công thức `M1…M14`, quy tắc hợp lệ và cách lưu kết quả theo [DOC-70](README.md). Thuật toán job trả vé ở DOC-24 §4.4–4.5.

Mọi con số trong tài liệu là **kế hoạch** (planned) cho tới khi §11 có số đo. Mọi quyết định mới được ghi thành DR-230…233 và tóm tắt ở mục "Quyết định phát sinh khi viết tài liệu này".

## 1. Giả thuyết

| # | Giả thuyết | Đo bằng | Biến thể |
| --- | --- | --- | --- |
| H1 (NFR-03, FR-07.2) | Khi job chạy liên tục, mọi reservation trong 5.000 reservation hết hạn cùng lúc được trả (unit về `AVAILABLE`, reservation và đơn `EXPIRED`) ≤ 30 giây sau `expires_at` | X2 (trễ trả vé lớn nhất) | `steady` |
| H2 (FR-07.3, NFR-01) | Trả vé không trả nhầm: không unit nào của reservation khác (đang `ACTIVE` hoặc `CONFIRMED`) về `AVAILABLE`, không unit nào trả hai lần, kể cả khi người mua khác đang claim chính các unit vừa trả | X3, X4, M6, M14 | mọi biến thể |
| H3 (FR-07.8) | `api` bị giết (`SIGKILL`) giữa lúc trả vé, nằm im 5 phút: vé bị giữ lâu hơn nhưng không bán trùng; sau khi khởi động lại, job trả hết phần còn lại (gồm reservation kẹt ở `EXPIRING`) trong ≤ 30 giây kể từ lúc `api` sẵn sàng | X5, X6, M14 | `crash` |
| H4 (DR-182) | Không có cơ chế "chạy lại ngay khi đầy lô", nhịp cố định 5 giây với lô 200 trả 5.000 reservation sau ≈ 150 giây, tức vượt NFR-03 | X2 | `nodrain` (baseline) |

## 2. Biến

| Loại | Biến | Giá trị |
| --- | --- | --- |
| Độc lập | Biến thể (`VARIANT`, DR-232) | `steady`: job chạy liên tục; `crash`: giết `api` giữa chừng, nằm im `DOWN_S` giây; `nodrain`: như `steady` nhưng tắt cơ chế chạy lại ngay (DR-231) |
| Phụ thuộc | Thời gian trả hết, trễ trả vé, trả nhầm, số unit `AVAILABLE` cuối, thời gian hồi phục | X1…X8 (§6), M1, M2, M6, M7, M14 (DOC-70 §8.2) |
| Kiểm soát | Số reservation hết hạn `N = 5.000 × SCALE`; số vé mỗi reservation `q_n = 1 + (n mod 4)`; sức chứa pool `20.000 × SCALE`; tải nền nhóm B (§5.3); `reservation.expiry.interval` 5 giây, `batch-size` 200, `lease` 30 giây, `batch-budget` 20 giây (DR-42, DR-170); `STRATEGY = skip-locked`; phần chung ở DOC-70 §6.1 | |

Với `n = 1…5.000`, `n mod 4` lần lượt 1, 2, 3, 0 mỗi giá trị 1.250 lần, nên tổng vé của nhóm A là `5.000 + 1.250 × (1 + 2 + 3) = 12.500` vé (trung bình 2,5 vé mỗi reservation, khớp tỉ lệ ở DOC-10 §4.2).

## 3. Baseline

Biến thể `nodrain` là baseline: job chạy đúng nhịp cố định của DR-42 (`fixedDelay` 5 giây, lô 200, không chạy lại khi đầy lô). Kế hoạch theo DOC-24 §4.5: 25 vòng × (5 giây + ~1 giây) ≈ **150 giây** cho reservation cuối. Baseline chỉ chạy khi DR-182 chốt phương án (a); nếu Owner chọn (b) thì baseline là `batch-size = 200` và biến thể chính là `batch-size = 1000`; nếu chọn (c) thì không có baseline và §7 đổi ngưỡng H1 theo NFR-03 mới (§8.3).

EXP-04 không chạy bản `naive` hay `counter`: thủ tục trả vé (DOC-24 §4.4) giống nhau ở ba bản, và câu trả unit của `naive` có điều kiện `status = 'HELD'` như bản thật (DOC-24 §5); bằng chứng bán vượt của bản ngây thơ nằm ở EXP-01, EXP-02.

## 4. Môi trường

Theo DOC-70 §3 (máy, giới hạn container, mạng `172.28.0.0/16`, allowlist). Điểm khác:

- `api` **không có restart policy** trong compose (DOC-62 §4), nên `docker compose kill` để container dừng hẳn cho tới khi `run.sh` khởi động lại; quy tắc V4 của DOC-70 §9.1 không áp dụng cho lần dừng có chủ đích của biến thể `crash` (DOC-70 §9.1 đã ghi EXP-04 là ngoại lệ).
- P2 chưa có Stripe: bước R2 (hủy PaymentIntent) của DOC-24 §4.4 không chạy vì reservation không có `payment_intent_id`. Trễ do Stripe đo ở EXP-06 (P3).

## 5. Các bước

### 5.1 Lệnh

```bash
make exp EXP=04 VARIANT=steady            # 3 lần — H1, H2
make exp EXP=04 VARIANT=crash             # 3 lần — H2, H3
make exp EXP=04 VARIANT=nodrain RUNS=1    # baseline — H4 (chỉ khi DR-182 chốt (a))
make exp EXP=04 VARIANT=steady SCALE=0.01 RUNS=1   # thử script trên máy dev (P2-13), không ghi vào §11
```

Ba biến thể ≈ 7 + 3 × 12 + 3 × 7 ≈ **65 phút** (planned: một lần `steady` ≈ 7 phút, một lần `crash` ≈ 12 phút vì nằm im 5 phút).

### 5.2 Tham số: `experiments/exp04/`

```bash
# experiments/exp04/params.env — chung cho mọi biến thể (DOC-70 §6.2)
USERS=10000             # nhóm A: n = 1…5000; nhóm B: n = 5001…10000 (nhân SCALE)
CAPACITY=20000          # một loại vé GA giá 0 (nhân SCALE)
N_EXPIRE=5000           # số reservation hết hạn cùng lúc (nhân SCALE)
A_WINDOW_S=20           # nhóm A giữ vé đều trong [T0, T0 + 20 s]
TE_OFFSET_S=90          # T_E = T0 + 90 s: mọi reservation nhóm A hết hạn ở giây này
B_WINDOW_S=60           # nhóm B đến đều trong [T_E − 30 s, T_E + 30 s]
VUS=50
MAX_DURATION_S=900
DRAIN_S=40
KILL=0
DOWN_S=0
API_OVERRIDES='{}'
```

```bash
# experiments/exp04/params.steady.env
KILL=0
# experiments/exp04/params.crash.env
KILL=1
DOWN_S=300              # FR-07.8: dừng API 5 phút
# experiments/exp04/params.nodrain.env
API_OVERRIDES='{"reservation":{"expiry":{"drain-max":"PT0S"}}}'   # DR-231
```

`A_WINDOW_S = 20` cho nhịp 250 lệnh giữ/giây, dưới trần bulkhead ≈ 436/giây (DOC-10 §4.3), nên nhóm A không bị 503 và đúng `N` reservation được tạo.

### 5.3 Dòng thời gian của một lần chạy

Bước 1–7 và 9–12 theo DOC-70 §7.2. Bước 8 (tải) của EXP-04 gồm hai tiến trình chạy song song: k6 (`load/exp04.ts`, trong container `k6`) và `experiments/exp04/during.sh` (trên host, DR-232); bước 8 kết thúc khi **cả hai** thoát, `T1` = lúc tiến trình thoát sau cùng.

| Thời điểm | Ai | Việc |
| --- | --- | --- |
| `T0` (= `saleStartsAt` của fixture, DOC-70 §5.2) | k6 | Nhóm A, người dùng ảo `n = 1…N`, đến đều trong `[T0, T0 + A_WINDOW_S]`; mỗi người gửi một E-14 `{"items":[{"type":"GA","ticketTypeId":<id>,"quantity":q_n}]}` với `Idempotency-Key` mới, **không** gọi E-18. Lỗi tạm thời xử lý theo DOC-70 §4.1 |
| `T_E − 10 s` | `during.sh` | Kiểm tra nhóm A đủ: câu A1 (§5.4) trả đúng `N` dòng `ACTIVE`; thiếu → ghi `status: "ERROR"` lý do `group_a_incomplete`, thoát 1 (V6) |
| `T_E − 10 s` | `during.sh` | Căn hạn: câu A2 đặt `expires_at = T_E` cho đúng `N` reservation nhóm A (DR-230); số dòng ≠ `N` → ERROR |
| `[T_E − 30 s, T_E + 30 s]` | k6 | Nhóm B, `n = N+1…2N`, đến đều; mỗi người E-14 với `q_n`, nhận 201 thì gọi ngay E-18 `confirm-free`. 409 `INSUFFICIENT_CAPACITY` → dừng (không thử lại); các lỗi khác theo DOC-70 §4.1 |
| `T_E` | `api` | 5.000 reservation nhóm A quá hạn cùng lúc; job trả vé bắt đầu ở vòng kế tiếp (≤ 5 giây) |
| `T_E …` | `during.sh` | Mỗi 1 giây chạy câu A3, ghi `raw/drain.csv` (số `ACTIVE`/`EXPIRING`/`EXPIRED` của nhóm A theo thời gian) |
| Chỉ `crash`: khi `EXPIRED ≥ N/2` **và** `EXPIRING > 0` | `during.sh` | Giết `api` (§5.5); ghi `raw/kill.json` |
| Khi nhóm A hết `ACTIVE` và `EXPIRING` | `during.sh` | Ghi `t_drained`, thoát 0. Quá `T_E + 600 s` mà chưa hết → `status: "ERROR"` lý do `drain_timeout` |

Nhóm B chạy song song với job để tạo **tải trên chính pool đang được trả**: trước `T_E` pool còn `20.000 − 12.500 = 7.500` vé; nhóm B cần tới `12.500` vé, nên nửa sau của nhóm B claim lại các unit vừa được trả. Đây là điều kiện để H2 có ý nghĩa: nếu job trả nhầm hoặc trả hai lần một unit đã được người khác claim, INV-05 và X3 bắt được.

### 5.4 Câu SQL của `during.sh`

`during.sh` chạy SQL bằng `$EXPCOMPOSE exec -T postgres psql -v ON_ERROR_STOP=1 -At -U "$POSTGRES_USER" "$POSTGRES_DB"`, với `:e` = `eventId` và `:te` = `T_E` (epoch giây) lấy từ `fixture.json`.

```sql
-- A1: nhóm A đã giữ đủ (người dùng n = 1…N, email exp-000001…)
SELECT count(*)
  FROM reservation r JOIN app_user u USING (user_id)
 WHERE r.event_id = :'e' AND r.status = 'ACTIVE'
   AND u.email BETWEEN 'exp-000001@load.test' AND format('exp-%s@load.test', lpad(:'n', 6, '0'));

-- A2: căn hạn của nhóm A về một giây (DR-230); kỳ vọng UPDATE N
UPDATE reservation r
   SET expires_at = to_timestamp(:te)
  FROM app_user u
 WHERE u.user_id = r.user_id
   AND r.event_id = :'e' AND r.status = 'ACTIVE'
   AND u.email BETWEEN 'exp-000001@load.test' AND format('exp-%s@load.test', lpad(:'n', 6, '0'));

-- A3: tiến độ trả vé của nhóm A (mỗi giây)
SELECT extract(epoch FROM clock_timestamp())::bigint AS ts,
       count(*) FILTER (WHERE status = 'ACTIVE')   AS active,
       count(*) FILTER (WHERE status = 'EXPIRING') AS expiring,
       count(*) FILTER (WHERE status = 'EXPIRED')  AS expired
  FROM reservation
 WHERE event_id = :'e' AND expires_at = to_timestamp(:te);
```

Sau A2, nhóm A được nhận diện bằng `expires_at = T_E`; reservation nhóm B có `expires_at = created_at + 10 phút` (DR-42) nên không trùng. Câu A1 và A2 đi qua `reservation_event_idx`, A3 quét ≤ 10.000 dòng của một sự kiện, mỗi lần < 10 ms (planned), không đáng kể so với tải.

### 5.5 Giết và khởi động lại `api` (biến thể `crash`)

`during.sh`, khi điều kiện ở §5.3 thỏa (thăm dò A3 mỗi 0,5 giây sau `T_E`):

```bash
# 1. Giết không báo trước: không graceful shutdown, transaction đang chạy bị rollback (DR-233)
$EXPCOMPOSE kill -s SIGKILL api
t_kill=$(date +%s.%N)
# ghi raw/kill.json: t_kill và một dòng A3 chụp ngay sau lệnh kill
# 2. Nằm im DOWN_S giây (300 = FR-07.8)
sleep "$DOWN_S"
# 3. Khởi động lại cùng container, cùng biến môi trường như bước 3 của DOC-70 §7.2
INVENTORY_STRATEGY=skip-locked SPRING_APPLICATION_JSON="$API_OVERRIDES" \
  $EXPCOMPOSE --profile obs up -d --no-deps --wait api
t_ready=$(date +%s.%N)
# 4. nginx phân giải upstream `api` một lần lúc khởi động (DOC-62 §8): nạp lại để chắc chắn trỏ đúng container
$EXPCOMPOSE exec -T nginx nginx -s reload
```

Kỳ vọng sau khởi động lại (DOC-24 §4.5, §7.3): reservation nhóm A còn `ACTIVE` đã quá hạn > 300 giây nên `claimDue` nhặt ngay; reservation kẹt ở `EXPIRING` lúc bị giết có `expiring_since` cũ hơn `lease` 30 giây, nên `reclaimStale` nhặt chúng khi một vòng lấy chưa đủ lô (tức sau khi phần `ACTIVE` quá hạn đã cạn). `InvariantChecker` trong `api` có thể chạy ngay sau khởi động và log ERROR INV-08 (`ACTIVE` quá hạn > 60 giây) cho nhóm A: đây là hệ quả dự kiến của lần dừng 5 phút, không phải lỗi; kết quả bất biến của lần chạy chỉ lấy ở bước 11 (DOC-70 §7.2).

Trong lúc `api` chết, người dùng ảo nhóm B đang đến nhận 502/503 từ nginx hoặc lỗi mạng, thử lại theo DOC-70 §4.1 rồi bỏ cuộc sau 10 lần; các request này không tính vào M7 (§6).

### 5.6 `load/exp04.ts`

Dùng `load/lib/multiuser.ts` (DOC-70 §4.1); executor `per-vu-iterations`, `vus = VUS`, `iterations = 1`. Thời điểm kế hoạch của người dùng ảo:

- Nhóm A, `n = 1…N`: `T0 + (n − 1) × A_WINDOW_S / N`.
- Nhóm B, `n = N+1…2N`: `T_E − 30 + (n − N − 1) × B_WINDOW_S / N`.

`T0` lấy từ `fixture.json` (`saleStartsAt`), `T_E = T0 + TE_OFFSET_S`. Tag `name`: `hold_a`, `hold_b`, `confirm_b`. Script kết thúc khi người dùng ảo cuối của nhóm B xong (≈ `T_E + 30 s` cộng đuôi thử lại).

## 6. Số đo và công thức

Dùng chung từ DOC-70 §8.2: **M1, M2** (với tag `name:hold_b`: độ trễ giữ vé của nhóm B trong cơn bão hết hạn), **M6**, **M7**, **M10**, **M11**, **M12**, **M14**. Với biến thể `crash`, M7 không đếm request có thời điểm gửi trong `[t_kill, t_ready + 5 s]`.

Số đo riêng của EXP-04 (`T_E` = epoch giây, `:e` = `eventId`):

| # | Đại lượng | Đơn vị | Nguồn | Công thức |
| --- | --- | --- | --- | --- |
| X1 | Thời gian trả hết | giây | SQL | `max(closed_at) − T_E` của nhóm A (câu X-A) |
| X2 | Trễ trả vé từng reservation: p95 và lớn nhất | giây | SQL | `percentile_cont(0.95)` và `max` của `closed_at − expires_at` trên nhóm A (câu X-A). NFR-03 xét **giá trị lớn nhất** |
| X3 | Trả nhầm | reservation | SQL + JSON | (a) số reservation **ngoài** nhóm A có `close_reason = 'TIMEOUT'` (câu X-B); cộng (b) số mục INV-04, INV-05 trong `invariants.json` |
| X4 | Unit `AVAILABLE` cuối so với kỳ vọng | unit | SQL | Câu X-C: `available` thực và `capacity − Σ quantity` của reservation `ACTIVE`/`EXPIRING`/`CONFIRMED`; đạt khi bằng nhau |
| X5 | Hồi phục sau khởi động lại (`crash`) | giây | SQL + `during.sh` | `max(closed_at)` của nhóm A − `t_ready` |
| X6 | Trạng thái lúc giết (`crash`) | reservation | `raw/kill.json` | `active`, `expiring`, `expired` của nhóm A ngay sau `kill`; `expiring > 0` là điều kiện để lần chạy kiểm được đường lease (§9) |
| X7 | Hoạt động của job | vòng, reservation | Prometheus | Số vòng: `sum(increase(ticket_job_runs_total{job="release_expired"}[R]))`; lô p95: `histogram_quantile(0.95, sum by (le) (increase(ticket_expiry_batch_size_bucket[R])))`; thời gian vòng p95: `histogram_quantile(0.95, sum by (le) (increase(ticket_job_duration_seconds_bucket{job="release_expired"}[R])))`; thời gian một lần trả vé: `1000 × sum(increase(ticket_connection_hold_seconds_sum{op="release"}[R])) / sum(increase(ticket_connection_hold_seconds_count{op="release"}[R]))` ms |
| X8 | Unit trả bởi job | unit | Prometheus | `sum(increase(ticket_expiry_released_units_total[R]))`; kỳ vọng đúng `12.500 × SCALE` cộng unit của reservation nhóm B hết hạn (0, câu X-B). Chỉ dùng ở `steady`, `nodrain`: ở `crash` các lần tăng chưa kịp scrape trước `SIGKILL` bị mất |

Câu SQL (chạy ở bước 10 của DOC-70 §7.2 bởi `experiments/lib/collect.sh`, với hook riêng `experiments/exp04/collect.sql`):

```sql
-- X-A: X1, X2 và kiểm đủ nhóm A
SELECT count(*)                                              AS group_a,
       count(*) FILTER (WHERE r.status = 'EXPIRED' AND r.close_reason = 'TIMEOUT') AS expired_timeout,
       count(*) FILTER (WHERE o.status = 'EXPIRED')          AS orders_expired,
       extract(epoch FROM max(r.closed_at) - to_timestamp(:te))                     AS x1_drain_s,
       percentile_cont(0.95) WITHIN GROUP (ORDER BY extract(epoch FROM r.closed_at - r.expires_at)) AS x2_p95_s,
       extract(epoch FROM max(r.closed_at - r.expires_at))                          AS x2_max_s
  FROM reservation r
  JOIN orders o ON o.reservation_id = r.reservation_id
 WHERE r.event_id = :'e' AND r.expires_at = to_timestamp(:te);

-- X-B: trả nhầm loại (a): reservation ngoài nhóm A bị đóng do hết hạn
SELECT count(*) AS x3a_wrong_timeout
  FROM reservation
 WHERE event_id = :'e' AND expires_at <> to_timestamp(:te) AND close_reason = 'TIMEOUT';

-- X-C: số unit AVAILABLE cuối so với kỳ vọng (một pool GA)
SELECT p.capacity,
       (SELECT count(*) FROM inventory_unit u
         WHERE u.pool_id = p.pool_id AND u.status = 'AVAILABLE')                   AS available_actual,
       p.capacity - (SELECT coalesce(sum(ri.quantity), 0)
                       FROM reservation_item ri
                       JOIN reservation r ON r.reservation_id = ri.reservation_id
                      WHERE ri.pool_id = p.pool_id
                        AND r.status IN ('ACTIVE','EXPIRING','CONFIRMED'))         AS available_expected
  FROM inventory_pool p
 WHERE p.event_id = :'e';
```

`closed_at` là `now()` của transaction trả vé R3 (DOC-24 §4.4), tức thời điểm **bắt đầu** transaction; unit về `AVAILABLE` ở lúc commit, chậm hơn vài ms. Sai số này nhỏ hơn ba bậc so với ngưỡng 30 giây nên không hiệu chỉnh. `ticket_expiry_lag_seconds` (DOC-33) chỉ dùng để đối chiếu: bucket 20/30/60 giây quá thô để kết luận quanh ngưỡng 30 giây.

## 7. Tiêu chí đạt

Theo DOC-70 §9.3: tính đúng phải thỏa ở **mọi** lần hợp lệ của mọi biến thể; hiệu năng xét **trung vị** của 3 lần.

| # | Tiêu chí | Biến thể | Loại |
| --- | --- | --- | --- |
| C1 | `group_a = expired_timeout = orders_expired = N` (câu X-A) | mọi | Tính đúng |
| C2 | X3 = 0 (cả (a) và (b)) | mọi | Tính đúng |
| C3 | X4: `available_actual = available_expected` | mọi | Tính đúng |
| C4 | M6 = 0; M14: `invariants.exit = 0` | mọi | Tính đúng |
| C5 | X8 = `12.500 × SCALE` | `steady`, `nodrain` | Tính đúng (không trả hai lần, FR-07.3) |
| C6 | M7 = 0 (ngoài cửa sổ `crash`) | mọi | Tính đúng (không sập) |
| P1 | X2 lớn nhất ≤ 30 giây (NFR-03) | `steady` | Hiệu năng |
| P2 | X5 ≤ 30 giây (DR-233) | `crash` | Hiệu năng |
| — | Không có ngưỡng: ghi số để so | `nodrain` | Baseline |

EXP-04 **đạt** khi `steady` và `crash` đều đạt. Một lần `crash` có X6 `expiring = 0` (giết đúng lúc giữa hai vòng) vẫn hợp lệ về tính đúng nhưng không kiểm được đường lease; `run.sh` chạy bù một lần (tối đa 5 lần mỗi lượt theo DOC-70 §8.3) cho tới khi đủ 3 lần có `expiring > 0`.

Kết quả dự kiến (planned):

| Biến thể | X2 lớn nhất | X5 | Cơ sở |
| --- | --- | --- | --- |
| `nodrain` | ≈ 150 giây | — | 25 vòng × (5 giây chờ + ~1 giây xử lý), DOC-24 §4.5 |
| `steady` | ≈ 25 giây (khoảng 15–35) | — | ≤ 5 giây tới vòng đầu + 5.000 × ~3,5 ms trả vé tuần tự ≈ 17,5 giây; nếu tải nền làm một lần trả vé > 5 ms thì chạm `drain-max` 25 giây, chờ thêm một nhịp 5 giây, và **vượt** 30 giây |
| `crash` | — | ≈ 15 giây | ≈ 2.500 reservation còn lại × ~3,5 ms + ≤ 5 giây tới vòng đầu; reservation `EXPIRING` lúc giết được `reclaimStale` nhặt ở vòng cuối |

## 8. Phân tích

### 8.1 Câu hỏi cần trả lời

1. H1: X2 lớn nhất của `steady` so với 30 giây; đồ thị `raw/drain.csv` (số `EXPIRED` theo giây) có dốc đều không, có bậc 5 giây (chạm `drain-max` rồi chờ nhịp) không.
2. Thời gian một lần trả vé (X7) dưới tải nền so với kế hoạch 2–5 ms (DOC-24 §4.5); nếu vượt, nguyên nhân từ M11 (chờ lock), M12 (CPU `postgres`, `api`) hay M10 (chờ connection).
3. H2: tách X3 theo nguồn; nếu C3 sai thì unit nào lệch (INV-04/INV-05 trong `invariants.json`) và điều tra theo [RB-02](../../09-operations/runbooks/RB-02-invariant-violation.md).
4. H3: X6 (bao nhiêu reservation kẹt `EXPIRING`), thời điểm từng reservation đó được đóng so với `t_ready` (chúng phải được đóng sau khi phần `ACTIVE` quá hạn cạn, DOC-24 §4.5 bước 2), và X5.
5. Ảnh hưởng của cơn bão hết hạn lên người mua: M1, M2 của `hold_b` trong `[T_E, T_E + 30 s]` so với trước `T_E`.

### 8.2 Kết quả nuôi DR-182

DR-182 (job chạy lại ngay khi vòng trước đầy lô) đang ở trạng thái **Đề xuất** và phải được chốt trước P2-09; EXP-04 chạy ở P2-14, sau khi job đã cài. Vì vậy EXP-04 **kiểm chứng** quyết định chứ không đi trước nó. Đọc kết quả như sau:

| Kết quả | Hành động |
| --- | --- |
| `nodrain` ≈ 150 giây và `steady` đạt P1 | Xác nhận DR-182 (a); ghi số đo vào DR-182, thay con số kế hoạch ở DOC-24 §4.5 và DOC-10 §8 |
| `steady` không đạt P1, X7 cho thấy thời gian một lần trả vé là nút thắt (5.000 × thời gian > 25 giây) | DR mới: trả vé GA theo lô bằng một câu (reservation không có PaymentIntent) hoặc nhiều luồng trả song song; hoặc DR-182 (c) hiệu chỉnh NFR-03. Owner chọn |
| `steady` không đạt P1, thời gian một lần trả vé đúng kế hoạch nhưng có khoảng trống giữa các vòng | Lỗi cài đặt của cơ chế chạy lại (DOC-24 §4.5); sửa và chạy lại |
| `nodrain` đã ≤ 30 giây | Kế hoạch ở DOC-24 §4.5 sai; ghi số đo, DR-182 có thể bỏ (Owner quyết) |

Nếu Owner chốt DR-182 (b) hoặc (c) thay vì (a), §3 và §7 được sửa theo trước P2-13, và DR-231 bị bỏ.

## 9. Mẫu bảng kết quả

Theo DOC-70 §11; mỗi biến thể một khối. Cột bắt buộc của DOC-70 giữ nguyên; cột riêng:

```markdown
### EXP-04 · `skip-locked` · `steady` · SCALE 1 · <ngày>

Máy: … · Giới hạn: DOC-70 §3.2 (<ngày bảng>) · Tham số: `experiments/exp04/params.env`, `params.steady.env` @ <sha>

| Lần | Thời điểm (UTC) | SHA | Hợp lệ | Bất biến | Nhóm A hết hạn / N | Trả nhầm | AVAILABLE thực / kỳ vọng | Unit job trả | Trả hết sau T_E (s) | Trễ p95 (s) | Trễ max (s) | Vòng job | Lô p95 | Một lần trả (ms) | p95 giữ nhóm B k6 (ms) | 5xx khác |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| r1 | | | | | 5.000 / 5.000 | 0 | | 12.500 | | | | | | | | 0 |
| r2 | | | | | | | | | | | | | | | | |
| r3 | | | | | | | | | | | | | | | | |
| **Tổng hợp** | | | 3/3 | 0 / 0 / 0 | | 0 / 0 / 0 | | | trung vị (min–max) | … | … | … | … | … | … | 0 / 0 / 0 |
```

Khối `crash` thay cột "Unit job trả" bằng ba cột: **Lúc giết: ACTIVE / EXPIRING / EXPIRED**, **Nằm im (s)**, **Hồi phục sau sẵn sàng (s)** (X5).

## 10. Mối đe dọa độ tin cậy

Chung ở DOC-70 §13 (T1…T11). Riêng EXP-04:

| # | Mối đe dọa | Ảnh hưởng | Giảm thiểu |
| --- | --- | --- | --- |
| E4-T1 | Căn hạn bằng SQL (A2) tạo tình huống mọi reservation hết hạn đúng một giây, khắc nghiệt hơn thực tế (thực tế trải theo nhịp giữ vé) | X2 cao hơn thực tế | Có chủ đích: đây là ca xấu nhất NFR-03 yêu cầu; ghi trong Phân tích |
| E4-T2 | A2 sửa `expires_at` từ ngoài ứng dụng | Có thể che lỗi ở cách `api` tính `expires_at` | INVT-11 kiểm `expires_at` do `api` tính; A2 chỉ đổi đầu vào của job, không đổi trạng thái hay unit |
| E4-T3 | P2 không gọi Stripe (R2 bỏ qua) | X2 thấp hơn khi có thanh toán thẻ | EXP-06 (P3) đo với cổng giả có độ trễ (DOC-70 T7) |
| E4-T4 | `SIGKILL` khác dừng thật (OOM, mất điện, `SIGTERM` có graceful) | Không đo đường graceful | `SIGKILL` là ca khó hơn: transaction đang chạy rollback và reservation kẹt `EXPIRING`; graceful thuộc DOC-62 |
| E4-T5 | nginx giữ IP cũ của `api` sau khởi động lại | 502 kéo dài, nhóm B thất bại nhiều hơn | `nginx -s reload` sau khi `api` sẵn sàng (§5.5) |
| E4-T6 | Counter Prometheus mất phần chưa scrape khi `SIGKILL` | X8 sai ở `crash` | Không dùng X8 ở `crash`; tính đúng dựa trên SQL (C1–C4) |
| E4-T7 | Nhóm B nhận 409 `INSUFFICIENT_CAPACITY` hoặc cờ hết vé trước khi job kịp trả | Tải nền thấp hơn dự kiến ở nửa sau | Ghi số 409 của `hold_b` (`exp_http_outcome`); không ảnh hưởng tính đúng |
| E4-T8 | `InvariantChecker` trong `api` chạy giữa lần chạy và log ERROR INV-08/INV-09 (`nodrain`, `crash`) | Nhiễu log, không ảnh hưởng số đo | Ghi nhận; chỉ dùng kết quả bước 11 |

## 11. Kết quả

Chưa chạy. P2-14 dán các bảng theo §9 vào đây kèm link thư mục `experiments/results/EXP-04/` (DOC-70 §10).

## Quyết định phát sinh khi viết tài liệu này

**DR-230 · Căn hạn của nhóm reservation hết hạn về một giây bằng SQL.** *Vấn đề:* "5.000 reservation hết hạn cùng lúc" (DOC-03 NFR-03, SDD gốc §15.2) không làm được qua API: `expires_at = now() + hold-duration` nên reservation tạo trong 20 giây hết hạn trải 20 giây; rút `hold-duration` cũng không gom được về một giây. *Quyết định:* sau khi nhóm A giữ xong, `experiments/exp04/during.sh` chạy `UPDATE reservation SET expires_at = to_timestamp(:te)` cho đúng `N` reservation `ACTIVE` của nhóm A (§5.4 câu A2), kiểm số dòng = `N`. Chỉ trên database của máy thực nghiệm; không đổi trạng thái, unit hay đơn. *Hệ quả:* ca xấu nhất được đo; nhóm A nhận diện bằng `expires_at = T_E`. *Ghi vào:* DOC-74.

**DR-231 · Khóa `reservation.expiry.drain-max` (đi cùng DR-182, trạng thái Đề xuất).** *Vấn đề:* DR-182 (a) chạy lại job ngay khi vòng trước đầy lô "tới khi hết hoặc hết 25 giây"; con số 25 giây chưa có khóa cấu hình, và EXP-04 cần chạy baseline nhịp cố định của DR-42 trên cùng bản build. *Quyết định:* khóa `reservation.expiry.drain-max` (duration, mặc định `PT25S`, mọi profile): thời gian tối đa job chạy lại liên tục khi vòng trước lấy đủ `batch-size`; `PT0S` tắt cơ chế, job chạy đúng `fixedDelay` như DR-42. Điều kiện khởi động: `drain-max < reservation.expiry.lease`. Chỉ có hiệu lực khi Owner chốt DR-182 (a); chọn (b) hoặc (c) thì bỏ DR này. *Hệ quả:* baseline `nodrain` chạy bằng `API_OVERRIDES`, không cần build riêng. *Ghi vào:* DOC-24 §4.5, §8, DOC-34 §3.4, §6, DOC-74.

**DR-232 · `make exp` nhận `VARIANT` và hook `during.sh`.** *Vấn đề:* EXP-04 có ba biến thể cùng một kịch bản và cần thao tác từ host trong lúc tải (căn hạn bằng SQL, giết và khởi động lại `api`); DOC-70 §7 chỉ có `EXP`, `STRATEGY`, `SCALE`, `RUNS` và chỉ chạy k6 ở bước 8. *Quyết định:* (1) tham số `VARIANT` (mặc định rỗng): `run.sh` đọc `experiments/expNN/params.env` rồi ghi đè bằng `params.<VARIANT>.env`; tên thư mục kết quả thành `<UTC>-<strategy>[-<variant>]-s<SCALE>-r<n>`; `env.json` có trường `variant`. (2) Nếu có `experiments/expNN/during.sh`, `run.sh` chạy nó **nền** cùng lúc với k6 ở bước 8 với biến môi trường `EXPCOMPOSE`, `RESULTS` (thư mục lần chạy), `FIXTURE`, cùng các biến của `params.env`; khi k6 thoát, `run.sh` tạo file `$RESULTS/.k6-done`; bước 8 kết thúc khi cả k6 và `during.sh` thoát; `during.sh` thoát khác 0 → lần chạy `ERROR` (V6). (3) Nếu có `experiments/expNN/collect.sql`, `collect.sh` chạy thêm nó ở bước 10 với `-v e=<eventId> -v te=<T_E>` và ghi kết quả vào `metrics.json` mục `custom`. *Hệ quả:* EXP-10 dùng cùng hook để lấy mẫu `pg_locks` (DOC-80). *Ghi vào:* DOC-70 §6.2, §7.1, §7.2 (bước 8, 10), §10, §15, DOC-61 (bảng `make exp`), DOC-74, DOC-80.

**DR-233 · Kịch bản dừng API của EXP-04: `SIGKILL`, nằm im 5 phút, hồi phục ≤ 30 giây.** *Vấn đề:* SDD gốc §15.2 và FR-07.8 nói "dừng API giữa chừng rồi khởi động lại" nhưng không nói dừng kiểu gì, lúc nào, bao lâu, và đạt nghĩa là gì về thời gian. *Quyết định:* giết bằng `docker compose kill -s SIGKILL api` khi nhóm A đã trả ≥ một nửa **và** còn reservation `EXPIRING` (đang xử lý dở); nằm im 300 giây (FR-07.8); khởi động lại bằng `up -d --no-deps --wait api` với cùng biến môi trường, rồi `nginx -s reload`. Ngưỡng hồi phục X5 ≤ 30 giây tính từ lúc `api` sẵn sàng (NFR-03 áp dụng "khi job chạy"). *Hệ quả:* đường lease (`reclaimStale`) được kiểm dưới tải thật; sau khi khởi động lại `api` riêng lẻ phải nạp lại nginx (thêm ghi chú ở DOC-62 §8). *Ghi vào:* DOC-74, DOC-62 §8, DOC-03 FR-07.8 (cột kiểm chứng: "EXP-04 `crash`").

## 12. Câu hỏi còn mở

Không có câu hỏi riêng của tài liệu. Tiêu chí P1 và biến thể `nodrain` phụ thuộc DR-182 (Đề xuất, chờ Owner); quyết định phát sinh là DR-230, DR-232, DR-233 (Chốt) và DR-231 (Đề xuất, đi cùng DR-182).
