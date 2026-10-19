# EXP-10 · Một dòng mỗi vé và bộ đếm

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-80 / EXP-10
> Phụ thuộc: SDD gốc §10.3, §15.2, [Sổ quyết định](../../00-decision-register.md) (DR-41, 61, 75, 76, 172, 173), [ADR-0003](../../04-adr/0003-one-row-per-ticket-skip-locked.md), [DOC-03](../../01-product/requirements.md) (NFR-01), [DOC-06](../../02-glossary.md), [DOC-10](../../03-architecture/quality-attributes.md) §2, §4.3, §8, [DOC-14](../../05-data/domain-model.md) (`inventory_unit`, `ticket`), [DOC-15](../../05-data/ops-model.md) §8.2 (`inventory_pool_counter`), [DOC-24](../../06-design/inventory-and-reservation.md) §4.2, §5, §6, §7.1, §10, [DOC-30](../../06-design/invariant-checker.md) (INV-05), [DOC-33](../../06-design/observability.md) §4.2, [DOC-62](../../09-operations/deploy-compose.md) §9, [DOC-70](README.md), [DOC-72](EXP-02-pool-contention.md), [DOC-74](EXP-04-expiry-under-load.md) (DR-232)
> Người dùng chính: P2-13 (`CounterClaimer`, `experiments/exp10/`), P2-14 (chạy), người đọc ADR-0003 cần số đo cho lựa chọn §10.3 của SDD gốc

Thực nghiệm chạy **kịch bản EXP-02** (DOC-72) trên hai bản `InventoryClaimer`: `skip-locked` (phương án D, một dòng mỗi vé, ADR-0003) và `counter` (phương án A, một dòng bộ đếm mỗi pool, DR-76), cùng tải, cùng máy, cùng git SHA, rồi so thông lượng, p95 và thời gian chờ lock. Đây là số đo kiểm chứng lý do 5 của SDD gốc §10.3. Máy, quy trình `make exp`, công thức `M1…M14`, quy tắc hợp lệ và cách lưu kết quả theo [DOC-70](README.md); hành vi người dùng ảo theo DOC-72 §5.3.

Mọi con số là **kế hoạch** (planned) cho tới khi §11 có số đo. Mọi quyết định mới được ghi thành DR-234, DR-235 và tóm tắt ở mục "Quyết định phát sinh khi viết tài liệu này".

## 1. Giả thuyết

| # | Giả thuyết | Đo bằng |
| --- | --- | --- |
| H1 (SDD gốc §10.3, §15.2) | Với `skip-locked`, không lệnh giữ vé nào chờ lock ở câu claim: L1 `claim_unit` = 0 conn·giây và M11 ≤ 1 conn·giây | L1, M11 |
| H2 (SDD gốc §10.3 phương án A) | Với `counter`, mọi lệnh giữ của pool xếp hàng trên một dòng: L1 `claim_counter` > 0, thông lượng giữ thành công (M3) thấp hơn và thời gian bán hết (M4) dài hơn `skip-locked`, với khoảng min–max của ba lần không chồng nhau (DOC-70 §13 T10) | L1, M3, M4, M1, M2, M8 |
| H3 (NFR-01) | Cả hai bản đều đúng: không bán vượt, bán hết, bất biến sạch | M5, M6, M14 |

Không có ngưỡng hiệu năng: EXP-10 **ghi lại mức chênh** (SDD gốc §15.2). H1 và H3 là tiêu chí đạt; H2 là kết quả được báo cáo, không đạt H2 thì ghi vào Phân tích chứ không làm EXP-10 trượt.

## 2. Biến

| Loại | Biến | Giá trị |
| --- | --- | --- |
| Độc lập | `STRATEGY` | `skip-locked`, `counter` |
| Phụ thuộc | Hiệu năng | M1, M2, M3, M3b, M4, M8, M9, M10, M12, M13 (DOC-70 §8.2) |
| Phụ thuộc | Lock | M11 (gồm `deadlocks`), L1…L5 (§6) |
| Phụ thuộc | Tính đúng | M5, M6, M7, M14 |
| Kiểm soát | Kịch bản | Đúng DOC-72 §5.1–5.3: 100.000 × `SCALE` người dùng ảo, pool GA 5.000 × `SCALE` giá 0, M-B với `W = 60 giây`, số vé `q_n` và thời điểm đến `a_n` theo `uniform(n, 'exp02-qty')`, `uniform(n, 'exp02-arrival')` (DR-214), giữ lại sau 409 `INSUFFICIENT_CAPACITY` có `available ≥ 1` tối đa 3 lần (DR-215) |
| Kiểm soát | Khác EXP-02 | `CONFIRM = 0`: người thắng chỉ giữ vé, không `confirm-free` (DR-234) |
| Kiểm soát | Chung | DOC-70 §6.1; bulkhead 24, Hikari 40, `lock_timeout` 1 giây, `statement_timeout` 2 giây (DR-41, DR-61) giữ nguyên ở cả hai bản |

Vì `q_n`, `a_n` tất định theo `n` và cùng salt `exp02-*`, hai bản nhận **cùng một chuỗi request** tới khi kết quả bắt đầu khác (409, 503 và lần gửi lại).

## 3. Baseline

Baseline là `counter` (`CounterClaimer`, DOC-24 §5): claim pool là một câu

```sql
UPDATE inventory_pool_counter
   SET available = available - :qty, held = held + :qty, updated_at = now()
 WHERE pool_id = :p AND available >= :qty;   -- 1 dòng: giữ được; 0 dòng: không đủ
```

đặt ở cùng vị trí trong transaction giữ vé như câu claim `SKIP LOCKED` (DOC-24 §4.1, bước claim). Khóa dòng bộ đếm giữ từ câu này tới `COMMIT`, tức qua các chặng 14–17 của DOC-10 §2 (`INSERT reservation_item`, `INSERT orders`, ghi response idempotency, `COMMIT`). Mọi lệnh giữ khác của pool chờ khóa đó; `lock_timeout` 1 giây chặn chờ quá lâu bằng 503 `OVERLOADED` (DOC-24 §10).

EXP-10 không chạy `naive`: bằng chứng bán vượt thuộc EXP-02.

## 4. Môi trường

Theo DOC-70 §3. Hai chiến lược chạy **liền nhau trong một phiên** trên cùng máy và cùng SHA (§5.2); không dùng lại các lần `skip-locked` của EXP-02 vì EXP-02 có `CONFIRM = 1` và có thể chạy ở SHA khác.

## 5. Các bước

### 5.1 Tham số và script

```bash
# experiments/exp10/params.env — DOC-80 §5.1
source "$(dirname "${BASH_SOURCE[0]}")/../exp02/params.env"   # cùng kịch bản DOC-72 §5.1
CONFIRM=0                                                     # DR-234
```

```ts
// load/exp10.ts — dùng nguyên hành vi của EXP-02 (salt exp02-*, giữ lại sau 409)
export { options, default } from './exp02.ts';
```

`experiments/exp10/during.sh` (hook của DR-232) lấy mẫu lock mỗi 1 giây tới khi có `$RESULTS/.k6-done` (§6.1), rồi đếm lock timeout trong log PostgreSQL (§6.2) và thoát 0. `experiments/exp10/collect.sql` chạy ở bước 10 của DOC-70 §7.2 (câu L3, L5 ở §6.2).

Phía server, P2-13 phải bảo đảm dòng `inventory_pool_counter` chỉ được tạo khi `inventory.strategy = counter` (DR-235), để bản `skip-locked` chạy trong profile `experiment` không có dòng bộ đếm đứng yên làm sai kiểm tra bất biến của DR-210.

### 5.2 Lệnh

```bash
make exp EXP=10 STRATEGY=skip-locked       # 3 lần
make exp EXP=10 STRATEGY=counter           # 3 lần, ngay sau, cùng SHA
make exp EXP=10 STRATEGY=counter SCALE=0.01 RUNS=1   # thử script trên máy dev (P2-13), không ghi vào §11
```

Sáu lần ≈ 6 × 7 phút ≈ **42 phút** (planned, DOC-70 §7.3; `counter` có thể dài hơn vì bán hết chậm hơn, §7).

## 6. Số đo và công thức

Dùng chung từ DOC-70 §8.2: M1, M2, M3, M3b, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14. M5 dùng nhánh theo chiến lược (`counter` đọc `held + sold` từ `inventory_pool_counter`). M6 tính từ `reservation_item` ở cả hai bản nên so được. Số đo riêng X1…X5 của DOC-72 §6 **không** dùng: X1, X4 cần `confirm-free`; X2, X3, X5 lấy được nhưng không thuộc câu hỏi của EXP-10.

Số đo riêng:

| # | Đại lượng | Đơn vị | Nguồn | Công thức |
| --- | --- | --- | --- | --- |
| L1 | Thời gian chờ lock theo loại câu | conn·giây | `raw/pg-locks.csv` (§6.1) | Σ `waiters` × 1 giây theo cột `stmt` (`claim_unit`, `claim_counter`, `reservation`, `idempotency`, `other`) trong `[T0, T1]` |
| L2 | Lần chờ lâu nhất quan sát được | giây | `raw/pg-locks.csv` | `max(max_wait_s)` theo `stmt`; là chặn trên (tính từ `query_start`, gồm cả thời gian chạy trước khi chờ) |
| L3 | Thời gian thực thi câu claim | ms | `pg_stat_statements` | `calls`, `mean_exec_time`, `max_exec_time`, `stddev_exec_time`, `total_exec_time` của câu claim (câu L3); với `counter` gồm cả thời gian chờ khóa dòng |
| L4 | Lock timeout | lần | Log `postgres` | Số dòng `canceling statement due to lock timeout` từ `T0` (§6.2); mỗi lần là một 503 `OVERLOADED` (DOC-24 §10) |
| L5 | Hoạt động ghi trên bảng kho vé | dòng | `pg_stat_user_tables` | `n_tup_upd`, `n_tup_hot_upd`, `n_dead_tup`, `autovacuum_count` của `inventory_unit` và `inventory_pool_counter` (câu L5); cho thấy `counter` dồn mọi lần ghi vào một dòng (HOT được) còn `skip-locked` ghi rải trên unit (không HOT, DOC-24 §6.1) |

### 6.1 Lấy mẫu `pg_locks`: `experiments/exp10/during.sh`

Mỗi 1 giây, chạy câu dưới qua `$EXPCOMPOSE exec -T postgres psql -At -F, …` và nối vào `raw/pg-locks.csv`:

```sql
SELECT extract(epoch FROM clock_timestamp())::bigint                 AS ts,
       a.wait_event,                                                  -- transactionid, tuple, relation…
       coalesce(l.locktype, '-')                                      AS locktype,
       coalesce(l.mode, '-')                                          AS mode,
       coalesce(l.relation::regclass::text, '-')                      AS relation,
       CASE WHEN a.query ILIKE '%inventory_pool_counter%' THEN 'claim_counter'
            WHEN a.query ILIKE '%inventory_unit%' AND a.query ILIKE '%SKIP LOCKED%' THEN 'claim_unit'
            WHEN a.query ILIKE '%INSERT INTO reservation %' THEN 'reservation'
            WHEN a.query ILIKE '%idempotency_key%' THEN 'idempotency'
            ELSE 'other' END                                          AS stmt,
       count(*)                                                       AS waiters,
       round(max(extract(epoch FROM clock_timestamp() - a.query_start))::numeric, 3) AS max_wait_s
  FROM pg_stat_activity a
  LEFT JOIN pg_locks l ON l.pid = a.pid AND NOT l.granted
 WHERE a.datname = current_database() AND a.wait_event_type = 'Lock'
 GROUP BY 1, 2, 3, 4, 5, 6;
```

Chờ khóa dòng trong PostgreSQL hiện ra là chờ `transactionid` (khóa `ShareLock` trên xid của bên đang giữ) hoặc `tuple`, không mang tên bảng; vì vậy câu phân loại theo nội dung câu SQL đang chờ (`stmt`). Mẫu 1 giây bỏ sót lần chờ ngắn hơn 1 giây giữa hai mẫu; L1 là ước lượng theo lấy mẫu như M11 (DOC-70 §8.2), L3 là số đo đầy đủ.

### 6.2 Câu thu ở cuối lần chạy

```sql
-- L3 (collect.sql): câu claim của từng bản
SELECT CASE WHEN query ILIKE '%inventory_pool_counter%' THEN 'claim_counter' ELSE 'claim_unit' END AS stmt,
       calls,
       round(mean_exec_time::numeric, 2)   AS mean_ms,
       round(max_exec_time::numeric, 2)    AS max_ms,
       round(stddev_exec_time::numeric, 2) AS stddev_ms,
       round(total_exec_time::numeric)     AS total_ms
  FROM pg_stat_statements
 WHERE dbid = (SELECT oid FROM pg_database WHERE datname = current_database())
   AND (query ILIKE '%inventory_pool_counter%available = available -%'
        OR (query ILIKE '%inventory_unit%' AND query ILIKE '%pool_id%' AND query ILIKE '%SKIP LOCKED%'));

-- L5 (collect.sql)
SELECT relname, n_tup_upd, n_tup_hot_upd, n_dead_tup, autovacuum_count
  FROM pg_stat_user_tables
 WHERE relname IN ('inventory_unit', 'inventory_pool_counter');
```

```bash
# L4 (during.sh, sau khi thấy $RESULTS/.k6-done); T0_ISO lấy từ fixture.json
$EXPCOMPOSE logs --no-color --since "$T0_ISO" postgres \
  | grep -c 'canceling statement due to lock timeout' > "$RESULTS/raw/lock-timeouts.txt"
```

`pg_stat_statements_reset()` và `pg_stat_reset()` chạy ở `T0 − 5 giây` (DOC-70 §7.2 bước 6), nên L3 và L5 chỉ gồm tải của sự kiện đo (pha khởi động JIT nằm trước đó).

## 7. Tiêu chí đạt

| # | Tiêu chí | Chiến lược | Loại |
| --- | --- | --- | --- |
| C1 | M6 = 0; M5 = `5.000 × SCALE` (không vượt, bán hết); M14 thoát 0 (với `counter`: kiểm tra theo bộ đếm, DR-210) | cả hai, mọi lần | Tính đúng |
| C2 | M7 = 0; `status 0` = 0; `deadlocks` = 0 | cả hai, mọi lần | Tính đúng |
| C3 | L1 `claim_unit` = 0 và M11 ≤ 1 conn·giây | `skip-locked`, mọi lần | Tính đúng (H1) |
| — | M3, M4, M1, M2, M8, L1…L4: báo trung vị (min–max) và tỉ số `counter / skip-locked` | cả hai | Ghi mức chênh (H2) |

EXP-10 **đạt** khi C1–C3 thỏa. Lý do C3 là tiêu chí tính đúng chứ không phải hiệu năng: chờ lock ở câu claim `SKIP LOCKED` nghĩa là câu không làm đúng điều ADR-0003 dựa vào (bỏ qua dòng đang khóa).

Kết quả dự kiến (planned), từ DOC-10 §2 và §4.3:

| Đại lượng | `skip-locked` | `counter` | Cơ sở |
| --- | --- | --- | --- |
| Trần thông lượng giữ vé | ≈ 436 lệnh/giây | ≈ 77 lệnh/giây | `skip-locked`: bulkhead 24 / 55 ms phục vụ; `counter`: 1 / thời gian giữ khóa dòng ≈ 5 + 2 + 6 = 13 ms (chặng 15–17 của DOC-10 §2) |
| M4 bán hết sau | 5–15 giây | ≈ 26–40 giây | ≈ 2.000 lệnh thành công / trần, cộng chờ `Retry-After` |
| L1 claim | 0 | ≈ 500–600 conn·giây | `counter`: tới 23 lệnh chờ sau một lệnh giữ khóa, suốt ≈ 26 giây |
| p95 phía API (M2) | ≈ 60–100 ms | ≈ 300 ms | `counter`: lệnh cuối hàng chờ ≈ 23 × 13 ms |
| L4 lock timeout | 0 | ít (≈ 0–vài chục) | Chờ ≈ 300 ms < `lock_timeout` 1 giây khi hàng chờ ≤ 23 |
| M8 tỉ lệ 503 | cao (đến 1.667 lệnh/giây vượt trần, DOC-72 §1) | cao hơn | Bulkhead đầy lâu hơn |
| L5 `n_tup_hot_upd / n_tup_upd` | ≈ 0 trên `inventory_unit` | ≈ 1 trên `inventory_pool_counter` | DOC-24 §6.1; bộ đếm không có index trên cột bị sửa |

## 8. Phân tích

Mục "Phân tích" của §11 trả lời:

1. H1: L1 và M11 của `skip-locked`; nếu khác 0, `wait_event` và `stmt` nào (`raw/pg-locks.csv`), và có phải do `reservation_open_uq` hay `idempotency_key` (không kỳ vọng vì mỗi người một lệnh và một key).
2. H2: tỉ số `counter / skip-locked` của M3, M4, M2, M8; min–max có chồng nhau không (DOC-70 §13 T10).
3. Trần thông lượng của `counter` đo được so với 77 lệnh/giây kế hoạch; suy ra thời gian giữ khóa dòng thực = 1 / M3 và so với L3 `mean_ms`.
4. Với `skip-locked`, nút thắt là bulkhead, CPU `postgres` hay CPU `api` (M10, M12)? Nếu là bulkhead thì so sánh đo **trần của bulkhead** chứ không phải trần của câu claim; ghi rõ.
5. L5: số lần ghi và dead tuple của hai bản; chi phí autovacuum của `skip-locked` (DOC-24 §6.2) so với lợi ích không chờ lock.
6. Kết luận một câu cho ADR-0003: số đo có ủng hộ phương án D trên tải EXP-02 không; nếu không, mở DR mới.

## 9. Mẫu bảng kết quả

Theo DOC-70 §11, mỗi chiến lược một khối `### EXP-10 · <strategy> · M-B · SCALE 1 · <ngày>`. Cột chọn từ mẫu DOC-70: Unit giữ+bán / sức chứa, Bán vượt, Bán hết sau (s), p95 k6 (ms), p95 API (ms), Giữ thành công/s, Request giữ/s, 503 (%), 5xx khác, Conn p95 (ms), Conn max, Chờ lock (conn·s), Lag k6 p95 (ms); thêm cột riêng:

| Cột | Số đo |
| --- | --- |
| Chờ lock claim (conn·s) | L1 `claim_unit` hoặc `claim_counter` |
| Chờ lâu nhất (s) | L2 |
| Claim mean / max (ms) | L3 |
| Lock timeout | L4 |
| Deadlock | M11 `deadlocks` |

Sau hai khối, một bảng so sánh:

```markdown
| Đại lượng | skip-locked trung vị (min–max) | counter trung vị (min–max) | counter / skip-locked | Phân biệt được? |
| --- | --- | --- | --- | --- |
| Giữ thành công/s (M3) | | | | có / không (T10) |
| Bán hết sau (M4, s) | | | | |
| p95 API (M2, ms) | | | | |
| p95 k6 (M1, ms) | | | | |
| 503 (M8, %) | | | | |
| Chờ lock claim (L1, conn·s) | | | — | |
| Claim mean (L3, ms) | | | | |
```

## 10. Mối đe dọa độ tin cậy

Chung ở DOC-70 §13. Riêng EXP-10:

| # | Mối đe dọa | Ảnh hưởng | Giảm thiểu |
| --- | --- | --- | --- |
| E10-T1 | Bulkhead 24 chặn trước database ở cả hai bản | `skip-locked` có thể bị đo ở trần bulkhead thay vì trần của câu claim; chênh lệch nhỏ hơn thực chất | Ghi M10, M12; Phân tích câu 4. Không nới bulkhead: EXP-10 so hai bản trong cấu hình chạy thật |
| E10-T2 | Không `confirm-free` (DR-234) | `counter` thật còn chịu thêm lần ghi bộ đếm khi xác nhận (`held − q, sold + q`), nên chênh lệch thực tế lớn hơn số đo | Ghi trong Phân tích; kết luận theo hướng bảo thủ cho phương án A |
| E10-T3 | L1, L2 lấy mẫu mỗi 1 giây | Bỏ sót lần chờ ngắn | L3 đầy đủ; L4 đếm từ log |
| E10-T4 | Phân loại `stmt` theo chuỗi SQL | Câu đổi chữ (P2-13 viết khác DOC-24 §4.2) làm rơi vào `other` | `collect.sh` kiểm `calls` của L3 > 0 ở mỗi bản, không thì ghi `INVALID` lý do V6 |
| E10-T5 | Hai chiến lược chạy nối tiếp, máy nóng dần | Nhiễu hệ thống giữa hai khối | Reset volume mỗi lần (DOC-70 §7.2); xen kẽ không bắt buộc, nhưng nếu khoảng min–max chồng nhau thì chạy lại cả hai khối theo thứ tự ngược |
| E10-T6 | Một pool, một loại vé | Không đo `counter` với nhiều pool (hàng chờ chia theo pool) | Đúng câu hỏi của SDD gốc §10.3 (hot row của một pool) |

## 11. Kết quả

Chưa chạy. P2-14 dán hai khối và bảng so sánh theo §9 vào đây kèm link `experiments/results/EXP-10/` (DOC-70 §10).

## Quyết định phát sinh khi viết tài liệu này

**DR-234 · EXP-10 chỉ giữ vé, không `confirm-free`, ở cả hai bản.** *Vấn đề:* kịch bản EXP-02 cho người thắng gọi `confirm-free` (DOC-72 `CONFIRM = 1`), nhưng với `CounterClaimer` unit pool không đổi trạng thái (DOC-24 §5) trong khi `ticket.unit_id` là `NOT NULL` và INV-06/INV-07 đòi vé gắn với unit `SOLD` (DOC-14, DOC-30): phát hành vé ở chế độ bộ đếm chưa được đặc tả và sẽ lỗi hoặc làm bất biến đỏ. *Quyết định:* `experiments/exp10/params.env` đặt `CONFIRM = 0` cho **cả hai** chiến lược để tải giống nhau; EXP-10 chỉ so đường giữ vé, đúng phạm vi câu hỏi của SDD gốc §10.3. *Hệ quả:* không dùng lại các lần `skip-locked` của EXP-02; `counter` không cần đường phát hành vé; chênh lệch đo được là cận dưới (E10-T2). *Ghi vào:* DOC-80, DOC-72 (ghi chú "EXP-10 dùng `CONFIRM = 0`"), DOC-24 §5 (ghi chú: `CounterClaimer` không hỗ trợ phát hành vé).

**DR-235 · Dòng `inventory_pool_counter` chỉ được tạo khi `inventory.strategy = counter`.** *Vấn đề:* DOC-15 §8.2 tạo một dòng bộ đếm cho mỗi pool lúc xuất bản trong profile `experiment`, mà mọi thực nghiệm (kể cả `skip-locked`, `naive`) đều chạy profile này (DOC-70 §6.1). DR-210 nhận diện pool chế độ bộ đếm bằng việc pool có dòng trong bảng; khi đó ở bản `skip-locked`, dòng bộ đếm đứng yên (`held = 0`) trong khi có reservation đang mở, nên mục kiểm tra bộ đếm báo sai và INV-05 bị bỏ qua sai. *Quyết định:* bước tạo dòng bộ đếm lúc xuất bản chỉ chạy khi `inventory.strategy = counter`. *Hệ quả:* nhận diện theo dữ liệu của DR-210 đúng ở mọi chiến lược; M5 nhánh `counter` không đổi. *Ghi vào:* DOC-15 §8.2, DOC-24 §4.8, DOC-30 §3, DOC-80.

## 12. Câu hỏi còn mở

Không có. Quyết định phát sinh là DR-234, DR-235 (mục trên). Tiêu chí C1 cho `counter` phụ thuộc DR-210 (mục kiểm tra bộ đếm ở DOC-30) được hợp nhất trước P2-13.
