# Kiểm tra bất biến

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-30
> Phụ thuộc: SDD gốc §8.1, §14.3, §15.2, [DOC-03](../01-product/requirements.md) (FR-12, NFR-01, NFR-05), [DOC-06](../02-glossary.md), [DOC-07](../03-architecture/system-context-and-containers.md) §4.3, [DOC-12](../03-architecture/code-architecture.md) §2, [DOC-14](../05-data/domain-model.md) §4–§6, [DOC-15](../05-data/ops-model.md) §7, [DOC-33](observability.md) §4.2, [DOC-34](configuration-reference.md) §2, §3.4, [DOC-61](../09-operations/local-dev.md), [DOC-62](../09-operations/deploy-compose.md), [DOC-69](../10-testing/test-strategy.md) §8.1, [Sổ quyết định](../00-decision-register.md) (DR-17, 28, 42, 44, 73, 79, 91, 93, 153)
> Người dùng chính: P2-11 (`InvariantChecker` và CLI), mọi test tích hợp chạm kho vé (`InvariantsExtension`), `make exp` (DOC-70), [RB-02](../09-operations/runbooks/RB-02-invariant-violation.md), [DOC-86](flows/operations.md) FL-18, FL-19

Tài liệu định nghĩa danh mục kiểm tra bất biến `INV-xx`, câu SQL chạy được của từng mục, ngưỡng, lịch chạy, chế độ dòng lệnh `make invariants` với JSON đầu ra và mã thoát, và cách thực nghiệm gọi lệnh. Cách điều tra và xử lý từng sai lệch nằm ở RB-02 (DOC-66); luồng qua từng tầng ở FL-18, FL-19 (DOC-86); DDL ở DOC-14, DOC-15. Lệnh chỉ báo cáo, không tự sửa (SDD gốc §14.3).

Mọi quyết định mới phát sinh khi viết tài liệu này được đánh dấu `DR-NEW-DOC30-n` và tóm tắt ở §11.

## 1. Mục đích và phạm vi

NEVER OVERSELL được ép ở database bằng câu ghi có điều kiện và ràng buộc (DOC-14). Kiểm tra bất biến là lưới thứ hai: nó đọc toàn bộ kho vé, reservation, đơn, vé và tìm trạng thái mà thiết kế nói không thể xảy ra. Mọi thực nghiệm chỉ đạt khi lệnh sạch (SDD gốc §15.2, FR-12.4).

| Thành phần | Package | Chạy ở | Kích hoạt | Đọc | Ghi | Việc |
| --- | --- | --- | --- | --- | --- | --- |
| `InvariantChecker` | `io.ticket.invariant.service` | `api` | Job, CLI, test | mọi bảng ở §3 (ngoại lệ chỉ đọc, DR-79) | — | `runAll()`: chạy mọi mục trong một transaction chỉ đọc, dựng báo cáo, đặt metric, ghi log |
| `InvariantQueries` | `io.ticket.invariant.repository` | `api` | `InvariantChecker` | như trên | — | Câu SQL của §3 bằng `JdbcClient` |
| `InvariantCheckJob` | `io.ticket.invariant.job` | `api` (mọi profile trừ `invariants`) | `@Scheduled(fixedDelayString = "${invariant.interval}")`, 5 phút | — | — | Gọi `runAll()` |
| `InvariantCliRunner` | `io.ticket.invariant.job` | container `api` chạy một lần, profile `invariants` | `ApplicationRunner` khi khởi động | — | stdout | In JSON, thoát 0/1/2 |
| `InvariantsExtension` | `src/test/java/io/ticket/testing` | JUnit | sau mỗi test tích hợp chạm kho vé | — | — | Gọi `runAll()`, fail test nếu có vi phạm không được `@ExpectViolation` (DOC-69 §8.1) |

Trong sơ đồ luồng, participant `InvariantChecker` (DOC-82 §1) bao cả job và service.

**Ranh giới module.** Package `io.ticket.invariant` được `SELECT` bảng của mọi module; ArchUnit cấm `INSERT`, `UPDATE`, `DELETE` trong package này (DR-79, ARC-07). Thêm lớp bảo vệ thứ hai ở database: transaction của checker là `READ ONLY` (§4).

## 2. Thành phần và interface

```java
package io.ticket.invariant;

public enum Severity { VIOLATION, REPORT }

/** Mục kiểm tra; name() là giá trị label "check" của metric và của JSON. */
public enum InvariantCheck {
  POOL_UNIT_COUNT("INV-01", Severity.VIOLATION),
  SEAT_UNIT_COUNT("INV-02", Severity.VIOLATION),
  UNIT_OWNER("INV-03", Severity.VIOLATION),
  HELD_ORPHAN("INV-04", Severity.VIOLATION),
  RESERVATION_UNIT_COUNT("INV-05", Severity.VIOLATION),
  SOLD_WITHOUT_TICKET("INV-06", Severity.VIOLATION),
  TICKET_WITHOUT_SALE("INV-07", Severity.VIOLATION),
  ACTIVE_OVERDUE("INV-08", Severity.VIOLATION),
  EXPIRING_STUCK("INV-09", Severity.VIOLATION),
  PAID_ORDER_TICKETS("INV-10", Severity.VIOLATION),
  ORDER_RESERVATION_STATE("INV-11", Severity.VIOLATION),
  ORDER_AMOUNT("INV-12", Severity.VIOLATION),
  CANCELLED_EVENT_SALES("INV-13", Severity.VIOLATION),
  OUTBOX_FAILED("INV-14", Severity.VIOLATION),
  REFUND_PENDING("INV-15", Severity.REPORT),
  LATE_PAYMENT_24H("INV-16", Severity.REPORT);
  …
}

public record Finding(String id, InvariantCheck check, long count, List<String> sample) {}  // sample ≤ 10
public record InvariantReport(Instant checkedAt, long durationMs, List<Finding> violations, List<Finding> reports) {
  public boolean clean() { return violations.isEmpty(); }
}
```

```java
// io.ticket.invariant.service.InvariantChecker
@Service
public class InvariantChecker {
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
  public InvariantReport runAll() { … }          // §3.1
}

// io.ticket.invariant.repository.InvariantQueries  (@Repository, JdbcClient, chỉ SELECT)
List<String> offenders(InvariantCheck check, Duration activeOverdue, Duration expiringStuck);  // tối đa 11 ID để biết "> 10"
long count(InvariantCheck check, Duration activeOverdue, Duration expiringStuck);
List<ReportRow> refundPendingByReason();          // INV-15
```

Mỗi mục có hai câu: câu `count` (đếm đúng) và câu `offenders` (lấy tối đa 10 ID mẫu, sắp theo ID để kết quả ổn định). Câu ở §3 là câu `offenders` đầy đủ; câu `count` là `SELECT count(*) FROM (<câu offenders không LIMIT>) x`.

## 3. Danh mục `INV-xx`

### 3.1 Thuật toán chung

```
runAll():
 1. bắt đầu transaction READ ONLY, REPEATABLE READ        -- một snapshot cho mọi mục
 2. SET LOCAL statement_timeout = invariant.statement-timeout (30 giây)
 3. t0 = clock; checkedAt = now() của database
 4. với mỗi check theo thứ tự INV-01 … INV-16:
      n = count(check)
      nếu n > 0: sample = offenders(check) giới hạn 10; thêm Finding vào violations hoặc reports theo severity
      gauge ticket_invariant_violations{check} = (severity = VIOLATION ? n : bỏ qua)
 5. histogram ticket_invariant_check_seconds.observe(clock − t0)
 6. mỗi Finding VIOLATION → log ERROR "invariant.violation" {check, id, count, sample}
    không có → log INFO "invariant.clean" {durationMs}
 7. trả InvariantReport
```

Mục `REPORT` (INV-15, INV-16) là số liệu cần người vận hành xem nhưng không phải sai lệch: một sự kiện bị hủy hợp lệ sinh đơn `REFUND_PENDING`. Chúng nằm trong JSON (`reports`) và log INFO, không làm mã thoát thành 1 (`DR-178`).

Ngoại lệ chung (DR-28, DR-73): các mục so **unit với vé** (INV-06) bỏ qua sự kiện `CANCELLED`, vì hủy giữ unit `SOLD` để lưu vết còn vé chuyển `VOID`. Sự kiện bị hủy được kiểm riêng bằng INV-13.

Ở P2 dữ liệu chỉ có GA: INV-02 và phần ghế của INV-05 luôn sạch (không có unit ghế) cho tới P5; câu SQL vẫn chạy và có test (IC-03) bằng dữ liệu ghế chèn tay.

### 3.2 Bảng tóm tắt

| ID | `check` | Mức | Phát hiện | Ngưỡng | Nguồn |
| --- | --- | --- | --- | --- | --- |
| INV-01 | `POOL_UNIT_COUNT` | VIOLATION | Pool có số unit chưa `REMOVED` khác `capacity` (thiếu hoặc thừa vé) | 0 | SDD gốc 14.3, DR-17, DR-30 |
| INV-02 | `SEAT_UNIT_COUNT` | VIOLATION | Sự kiện có sơ đồ: số unit ghế chưa `REMOVED` khác `sellable_seat_count` của phiên bản đang dùng | 0 | DR-27, DR-37, DR-40 |
| INV-03 | `UNIT_OWNER` | VIOLATION | Unit `AVAILABLE`/`REMOVED` còn `reservation_id`, hoặc `HELD`/`SOLD` không có | 0 | DR-73 (đã có `unit_owner_ck`) |
| INV-04 | `HELD_ORPHAN` | VIOLATION | Unit `HELD` mà reservation không còn mở | 0 | SDD gốc 14.3 |
| INV-05 | `RESERVATION_UNIT_COUNT` | VIOLATION | Số unit của reservation không khớp Σ `quantity` của item theo trạng thái | 0 | SDD gốc 14.3 |
| INV-06 | `SOLD_WITHOUT_TICKET` | VIOLATION | Unit `SOLD` (sự kiện chưa hủy) không có đúng một vé `ISSUED` | 0 | SDD gốc 14.3, DR-73 |
| INV-07 | `TICKET_WITHOUT_SALE` | VIOLATION | Vé `ISSUED` mà unit không `SOLD`, đơn không `PAID`, hoặc unit thuộc reservation khác | 0 | SDD gốc 14.3, NFR-05 |
| INV-08 | `ACTIVE_OVERDUE` | VIOLATION | Reservation `ACTIVE` quá `expires_at` hơn 60 giây | `invariant.active-overdue` | DR-42 |
| INV-09 | `EXPIRING_STUCK` | VIOLATION | Reservation `EXPIRING` có `expiring_since` cũ hơn 2 phút | `invariant.expiring-stuck` | DR-42 |
| INV-10 | `PAID_ORDER_TICKETS` | VIOLATION | Đơn `PAID` mà reservation không `CONFIRMED` hoặc số vé `ISSUED` ≠ Σ `quantity` | 0 | SDD gốc 14.3, DR-73 |
| INV-11 | `ORDER_RESERVATION_STATE` | VIOLATION | Cặp trạng thái đơn/reservation không có trong bảng §3.3 INV-11 | 0 | DR-44, DR-90, DR-91 |
| INV-12 | `ORDER_AMOUNT` | VIOLATION | `orders.amount` ≠ Σ `quantity × unit_price` | 0 | DR-20, DR-41 |
| INV-13 | `CANCELLED_EVENT_SALES` | VIOLATION | Sự kiện `CANCELLED` còn đơn `PAID` hoặc vé `ISSUED` | 0 | DR-28, DR-73 |
| INV-14 | `OUTBOX_FAILED` | VIOLATION | Dòng outbox `FAILED` | 0 | DR-53, DR-73 |
| INV-15 | `REFUND_PENDING` | REPORT | Số đơn `REFUND_PENDING` theo `refund_reason` | — | DR-44, DR-73 |
| INV-16 | `LATE_PAYMENT_24H` | REPORT | Số đơn sang `REFUND_PENDING` lý do `LATE_PAYMENT` trong 24 giờ qua | — (EXP-06 kỳ vọng 0) | DR-44 |

INV-01 cùng `unit_seat_uq`, `unit_owner_ck`, `ticket_unit_issued_uq` (DOC-14) là bằng chứng NEVER OVERSELL: số unit của mỗi pool đúng bằng sức chứa, mỗi unit một trạng thái và tối đa một chủ, nên số vé giữ và bán không vượt sức chứa; mỗi ghế tối đa một unit.

### 3.3 Câu SQL

Tham số `:active_overdue`, `:expiring_stuck` là `interval` lấy từ cấu hình (mặc định `'60 seconds'`, `'2 minutes'`). Mọi câu chạy được nguyên văn trong `make psql` sau khi thay tham số (RB-02 có bản đã thay).

**INV-01 `POOL_UNIT_COUNT`** · mẫu: `pool_id`

```sql
SELECT p.pool_id
FROM inventory_pool p
LEFT JOIN inventory_unit u ON u.pool_id = p.pool_id AND u.status <> 'REMOVED'
WHERE p.removed_at IS NULL                      -- pool của loại vé đã xóa: unit đã REMOVED hết (DR-26)
GROUP BY p.pool_id, p.capacity
HAVING count(u.unit_id) <> p.capacity
ORDER BY p.pool_id
LIMIT 11;
```

**INV-02 `SEAT_UNIT_COUNT`** · mẫu: `event_id`

```sql
SELECT e.event_id
FROM event e
JOIN seat_map_version v ON v.seat_map_version_id = e.seat_map_version_id
LEFT JOIN inventory_unit u ON u.event_id = e.event_id AND u.seat_id IS NOT NULL AND u.status <> 'REMOVED'
WHERE e.status <> 'DRAFT'
GROUP BY e.event_id, v.sellable_seat_count
HAVING count(u.unit_id) <> v.sellable_seat_count
ORDER BY e.event_id
LIMIT 11;
```

**INV-03 `UNIT_OWNER`** · mẫu: `unit_id`

```sql
SELECT unit_id
FROM inventory_unit
WHERE (status IN ('AVAILABLE','REMOVED') AND reservation_id IS NOT NULL)
   OR (status IN ('HELD','SOLD') AND reservation_id IS NULL)
ORDER BY unit_id
LIMIT 11;
```

Ràng buộc `unit_owner_ck` làm câu này luôn rỗng; giữ lại để phát hiện ràng buộc bị gỡ nhầm bằng migration (DR-73).

**INV-04 `HELD_ORPHAN`** · mẫu: `unit_id`

```sql
SELECT u.unit_id
FROM inventory_unit u
LEFT JOIN reservation r ON r.reservation_id = u.reservation_id
WHERE u.status = 'HELD'
  AND (r.reservation_id IS NULL OR r.status NOT IN ('ACTIVE','EXPIRING'))
ORDER BY u.unit_id
LIMIT 11;
```

**INV-05 `RESERVATION_UNIT_COUNT`** · mẫu: `reservation_id`

```sql
WITH expected AS (
  SELECT reservation_id, sum(quantity) AS qty FROM reservation_item GROUP BY reservation_id
), actual AS (
  SELECT reservation_id,
         count(*) FILTER (WHERE status = 'HELD') AS held,
         count(*) FILTER (WHERE status = 'SOLD') AS sold
  FROM inventory_unit WHERE reservation_id IS NOT NULL GROUP BY reservation_id
)
SELECT r.reservation_id
FROM reservation r
LEFT JOIN expected e ON e.reservation_id = r.reservation_id
LEFT JOIN actual   a ON a.reservation_id = r.reservation_id
WHERE (r.status IN ('ACTIVE','EXPIRING')     AND (coalesce(a.held,0) <> coalesce(e.qty,0) OR coalesce(a.sold,0) <> 0))
   OR (r.status = 'CONFIRMED'                 AND (coalesce(a.sold,0) <> coalesce(e.qty,0) OR coalesce(a.held,0) <> 0))
   OR (r.status IN ('EXPIRED','CANCELLED')    AND coalesce(a.held,0) + coalesce(a.sold,0) <> 0)
ORDER BY r.reservation_id
LIMIT 11;
```

Claim, xác nhận và trả vé đổi unit và reservation trong cùng một transaction (DOC-14 §7), nên trong một snapshot hai bên luôn khớp. Reservation `CONFIRMED` của sự kiện bị hủy vẫn giữ unit `SOLD` (DR-28), nên không cần ngoại lệ.

**INV-06 `SOLD_WITHOUT_TICKET`** · mẫu: `unit_id`

```sql
SELECT u.unit_id
FROM inventory_unit u
JOIN event e ON e.event_id = u.event_id
LEFT JOIN ticket t ON t.unit_id = u.unit_id AND t.status = 'ISSUED'
WHERE u.status = 'SOLD' AND e.status <> 'CANCELLED'
GROUP BY u.unit_id
HAVING count(t.ticket_id) <> 1
ORDER BY u.unit_id
LIMIT 11;
```

Hai vé `ISSUED` cho một unit bị `ticket_unit_issued_uq` chặn, nên trên thực tế mục này bắt unit `SOLD` thiếu vé.

**INV-07 `TICKET_WITHOUT_SALE`** · mẫu: `ticket_id`

```sql
SELECT t.ticket_id
FROM ticket t
JOIN inventory_unit u ON u.unit_id = t.unit_id
JOIN orders o ON o.order_id = t.order_id
WHERE t.status = 'ISSUED'
  AND (u.status <> 'SOLD' OR o.status <> 'PAID'
       OR u.reservation_id IS DISTINCT FROM o.reservation_id
       OR u.event_id <> t.event_id)
ORDER BY t.ticket_id
LIMIT 11;
```

**INV-08 `ACTIVE_OVERDUE`** · mẫu: `reservation_id`

```sql
SELECT reservation_id
FROM reservation
WHERE status = 'ACTIVE' AND expires_at < now() - :active_overdue    -- reservation_due_idx
ORDER BY expires_at
LIMIT 11;
```

**INV-09 `EXPIRING_STUCK`** · mẫu: `reservation_id`

```sql
SELECT reservation_id
FROM reservation
WHERE status = 'EXPIRING' AND expiring_since < now() - :expiring_stuck   -- reservation_expiring_idx
ORDER BY expiring_since
LIMIT 11;
```

`now()` trong transaction `REPEATABLE READ` là giờ bắt đầu transaction; chênh với giờ thật tối đa bằng `durationMs` của một lần chạy (dưới 1 giây ở quy mô dự án).

**INV-10 `PAID_ORDER_TICKETS`** · mẫu: `order_id`

```sql
SELECT o.order_id
FROM orders o
JOIN reservation r ON r.reservation_id = o.reservation_id
LEFT JOIN (SELECT reservation_id, sum(quantity) AS qty FROM reservation_item GROUP BY reservation_id) e
       ON e.reservation_id = o.reservation_id
LEFT JOIN (SELECT order_id, count(*) AS n FROM ticket WHERE status = 'ISSUED' GROUP BY order_id) t
       ON t.order_id = o.order_id
WHERE o.status = 'PAID'
  AND (r.status <> 'CONFIRMED' OR coalesce(t.n,0) <> coalesce(e.qty,0))
ORDER BY o.order_id
LIMIT 11;
```

**INV-11 `ORDER_RESERVATION_STATE`** · mẫu: `order_id`

Đơn và reservation đổi trạng thái trong cùng transaction (giữ vé, xác nhận, trả vé), nên chỉ các cặp sau hợp lệ:

| `orders.status` | `reservation.status` hợp lệ | Vì sao |
| --- | --- | --- |
| `PENDING_PAYMENT` | `ACTIVE`, `EXPIRING` | Đang giữ hoặc đang trả vé |
| `PAID` | `CONFIRMED` | Transaction xác nhận |
| `EXPIRED` | `EXPIRED` | Trả vé `TIMEOUT` |
| `CANCELLED` | `CANCELLED` | Trả vé `BUYER_CANCELLED` hoặc `EVENT_CANCELLED` (DR-91) |
| `REFUND_PENDING`, `REFUNDED` lý do `LATE_PAYMENT` | `EXPIRED`, `CANCELLED` | Tiền về sau khi reservation đã đóng (DR-44) |
| `REFUND_PENDING`, `REFUNDED` lý do `EVENT_CANCELLED`, `AMOUNT_MISMATCH` | mọi trạng thái trừ `ACTIVE` đối với `EVENT_CANCELLED`; mọi trạng thái với `AMOUNT_MISMATCH` | Đường đi phụ thuộc thời điểm webhook (DOC-26 chốt ở P3, có thể siết lại) |

```sql
SELECT o.order_id
FROM orders o
JOIN reservation r ON r.reservation_id = o.reservation_id
WHERE NOT (
     (o.status = 'PENDING_PAYMENT' AND r.status IN ('ACTIVE','EXPIRING'))
  OR (o.status = 'PAID'            AND r.status = 'CONFIRMED')
  OR (o.status = 'EXPIRED'         AND r.status = 'EXPIRED')
  OR (o.status = 'CANCELLED'       AND r.status = 'CANCELLED')
  OR (o.status IN ('REFUND_PENDING','REFUNDED') AND (
         (o.refund_reason = 'LATE_PAYMENT'    AND r.status IN ('EXPIRED','CANCELLED'))
      OR (o.refund_reason = 'EVENT_CANCELLED' AND r.status <> 'ACTIVE')
      OR  o.refund_reason = 'AMOUNT_MISMATCH'))
)
ORDER BY o.order_id
LIMIT 11;
```

**INV-12 `ORDER_AMOUNT`** · mẫu: `order_id`

```sql
SELECT o.order_id
FROM orders o
LEFT JOIN (SELECT reservation_id, sum(quantity::bigint * unit_price) AS total
           FROM reservation_item GROUP BY reservation_id) i ON i.reservation_id = o.reservation_id
WHERE o.amount <> coalesce(i.total, 0)
ORDER BY o.order_id
LIMIT 11;
```

**INV-13 `CANCELLED_EVENT_SALES`** · mẫu: `event_id`

```sql
SELECT e.event_id
FROM event e
WHERE e.status = 'CANCELLED'
  AND (EXISTS (SELECT 1 FROM orders o WHERE o.event_id = e.event_id AND o.status = 'PAID')       -- orders_event_paid_idx
    OR EXISTS (SELECT 1 FROM ticket t WHERE t.event_id = e.event_id AND t.status = 'ISSUED'))   -- ticket_event_idx
ORDER BY e.event_id
LIMIT 11;
```

**INV-14 `OUTBOX_FAILED`** · mẫu: `outbox_id`

```sql
SELECT outbox_id
FROM outbox
WHERE status = 'FAILED'          -- outbox_done_idx; dòng FAILED tồn tại 30 ngày (DR-74)
ORDER BY outbox_id
LIMIT 11;
```

Một email `FAILED` không phải bán vượt nhưng là người mua không nhận được vé hoặc thông báo; thực nghiệm không được sinh dòng nào. Người vận hành xử lý theo RB-02 (đặt lại `PENDING`, DR-93), sau đó mục sạch.

**INV-15 `REFUND_PENDING`** (REPORT) · mẫu: `order_id` của mỗi lý do

```sql
SELECT refund_reason, count(*) AS n,
       (array_agg(order_id ORDER BY created_at))[1:10] AS sample
FROM orders
WHERE status = 'REFUND_PENDING'            -- orders_refund_idx
GROUP BY refund_reason
ORDER BY refund_reason;
```

Mỗi lý do có số đếm khác 0 là một `Finding` với `check = "REFUND_PENDING"`, `id = "INV-15"`, thêm trường `"reason"`.

**INV-16 `LATE_PAYMENT_24H`** (REPORT) · mẫu: `order_id`

```sql
SELECT order_id
FROM orders
WHERE refund_reason = 'LATE_PAYMENT' AND updated_at > now() - interval '24 hours'
ORDER BY updated_at DESC
LIMIT 11;
```

`orders.updated_at` được đặt ở câu chuyển `REFUND_PENDING` (DR-44); đơn sau đó `REFUNDED` vẫn được đếm nếu trong 24 giờ.

### 3.4 Chi phí

Planned, chưa đo: các mục quét toàn bảng là INV-01, INV-03, INV-05, INV-11, INV-12. Với 1.000.000 dòng `inventory_unit` (10 sự kiện 100.000 chỗ) và 200.000 reservation, mỗi câu dự kiến 0,3–1 giây trên máy thực nghiệm; cả lần chạy < 5 giây. Mỗi câu bị giới hạn bởi `invariant.statement-timeout` = 30 giây. Số đo thật ghi lại sau P2-14 (EXP-02 với `ticket_invariant_check_seconds`).

## 4. Transaction và đồng thời

- **Một snapshot.** `runAll()` là một transaction `READ ONLY`, `REPEATABLE READ`: mọi mục thấy cùng trạng thái của database tại một thời điểm. Không có dương tính giả do một transaction giữ vé commit giữa hai câu (IC-23). Transaction đang chạy chưa commit không được thấy; vì mọi đổi trạng thái liên quan là nguyên tử (DOC-14 §7), trạng thái đã commit luôn nhất quán.
- **Không khóa.** Chỉ `SELECT` không `FOR UPDATE`; không chặn đường giữ vé. Snapshot dài vài giây giữ lại `xmin` và làm autovacuum của `inventory_unit` chờ đúng chừng đó, chấp nhận được với nhịp 5 phút.
- **`READ ONLY` ở database.** `@Transactional(readOnly = true)` làm Spring đặt `SET TRANSACTION READ ONLY`; một câu ghi lọt vào package này bị PostgreSQL từ chối (`25006`), bổ sung cho ArchUnit (IC-24).
- **Nhiều replica.** Compose chỉ có một `api`. Nếu chạy nhiều bản, mỗi bản tự chạy mỗi 5 phút; vô hại vì chỉ đọc. Không có khóa phân tán.
- **Tiến trình chết giữa chừng.** Không có gì để dọn; lần sau chạy lại từ đầu.

## 5. Chế độ dòng lệnh (`make invariants`)

### 5.1 Cách chạy

```bash
make invariants
# = docker compose … run --rm api --spring.profiles.active=invariants   (DOC-61, DOC-62, DR-73)
```

Profile `invariants` (`application-invariants.yml`, `DR-179`):

```yaml
spring:
  main:
    web-application-type: none      # không mở cổng 8081, 9090
    banner-mode: off
  flyway:
    enabled: false                  # chỉ đọc: không chạy migration
logging:
  config: classpath:logback-invariants.xml   # mọi log ra stderr; stdout chỉ có JSON
```

- `@EnableScheduling` đặt trên một `@Configuration` có `@Profile("!invariants")`: không job nào chạy trong tiến trình CLI (nếu không, `ReleaseExpiredHoldsJob` của tiến trình CLI sẽ trả vé và che mất INV-08).
- Mọi việc có tác dụng phụ lúc khởi động (tạo bucket S3 của `media`, kiểm tra kết nối Redis, `OutboxRelay`) có `@Profile("!invariants")`.
- `InvariantCliRunner` (`@Profile("invariants")`, `ApplicationRunner`): gọi `runAll()`, ghi JSON một dòng ra stdout, rồi `System.exit(SpringApplication.exit(ctx, () -> code))`.

### 5.2 JSON đầu ra

Dạng của DR-73, thêm `id` cho mỗi mục và mảng `reports` (`DR-178`). In trên một dòng; ví dụ dưới được xuống dòng để đọc.

Sạch (thoát 0):

```json
{
  "checkedAt": "2026-11-10T03:05:00.123Z",
  "durationMs": 412,
  "violations": [],
  "reports": [
    { "id": "INV-15", "check": "REFUND_PENDING", "reason": "EVENT_CANCELLED", "count": 3,
      "sample": ["0199b1c2-8d15-7a22-9b0c-1d2e3f405162", "0199b1c2-8d15-7a22-9b0c-1d2e3f405163", "0199b1c2-8d15-7a22-9b0c-1d2e3f405164"] }
  ]
}
```

Có sai lệch (thoát 1), sau khi cố ý `UPDATE inventory_unit SET status = 'REMOVED' WHERE unit_id = …` một unit `AVAILABLE` của pool GA:

```json
{
  "checkedAt": "2026-11-10T03:05:00.123Z",
  "durationMs": 398,
  "violations": [
    { "id": "INV-01", "check": "POOL_UNIT_COUNT", "count": 1, "sample": ["0199b1c2-5001-7aaa-8bbb-111122223333"] }
  ],
  "reports": []
}
```

Không chạy được (thoát 2):

```json
{ "checkedAt": "2026-11-10T03:05:00.123Z", "durationMs": 30012, "error": "STATEMENT_TIMEOUT", "check": "RESERVATION_UNIT_COUNT" }
```

| Trường | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `checkedAt` | string UTC | `now()` của database lúc bắt đầu transaction |
| `durationMs` | int | Thời gian cả lần chạy |
| `violations[]` | mảng `Finding` | Mục `VIOLATION` có `count > 0`, theo thứ tự INV |
| `reports[]` | mảng `Finding` | Mục `REPORT` có `count > 0` |
| `Finding.id`, `check` | string | `INV-xx` và tên hằng (§2) |
| `Finding.count` | int | Số đối tượng vi phạm (đếm đủ, không giới hạn) |
| `Finding.sample` | string[] | ≤ 10 ID (UUID), sắp theo ID hoặc theo thời gian như câu SQL |
| `Finding.reason` | string | Chỉ INV-15 |
| `error` | string | Chỉ khi thoát 2: `DATABASE_UNAVAILABLE`, `STATEMENT_TIMEOUT`, `QUERY_FAILED` |

### 5.3 Mã thoát (`DR-180`)

| Mã | Khi nào |
| --- | --- |
| 0 | Chạy hết, `violations` rỗng (`reports` có thể khác rỗng) |
| 1 | Chạy hết, có ít nhất một `violation` |
| 2 | Không chạy hết: không kết nối được database (Hikari `connection-timeout`), một câu quá `statement_timeout`, lỗi SQL |

Script gọi phải coi 2 là "không biết", không phải "sạch": `make exp` loại lần chạy khi mã ≠ 0 (§6).

## 6. Thực nghiệm và test gọi lệnh

- **Thực nghiệm.** `make exp EXP=xx` luôn kết thúc bằng `make invariants` sau khi k6 dừng và sau 2 × `reservation.expiry.interval` + `reservation.expiry.lease` (≈ 40 giây) để job trả vé xử lý reservation vừa hết hạn; mã ≠ 0 → lần chạy bị loại và bảng kết quả ghi `invariants: FAIL` kèm JSON (FR-12.4, DOC-70, FL-19). Riêng EXP-06 đọc thêm `reports` INV-16 (kỳ vọng `count` = 0, DR-44).
- **Bản `naive` (DR-76).** Chạy EXP-02 với `inventory.strategy=naive` phải cho `make invariants` thoát 1 với INV-01 hoặc INV-05 (bán vượt được phát hiện); đây là kiểm chứng rằng lệnh có răng.
- **Test tích hợp.** `InvariantsExtension` gọi `runAll()` sau mỗi test chạm kho vé, reservation, order; vi phạm không khai báo bằng `@ExpectViolation("INV-xx")` làm test đỏ (DOC-69 §8.1, TST-03, TST-04). `reports` không làm test đỏ.
- **Sau `make seed`.** Thoát 0 (OPS-06).

## 7. Cấu hình

| Khóa | Kiểu | Mặc định | Profile | Mô tả |
| --- | --- | --- | --- | --- |
| `invariant.interval` | duration | `PT5M` | mọi (trừ `invariants`) | Nhịp `InvariantCheckJob` (`fixedDelay`, DR-73) |
| `invariant.active-overdue` | duration | `PT60S` | mọi | Ngưỡng INV-08 (DR-42) |
| `invariant.expiring-stuck` | duration | `PT2M` | mọi | Ngưỡng INV-09 (DR-42) |
| `invariant.statement-timeout` | duration | `PT30S` | mọi | **Mới** (`DR-181`): `SET LOCAL statement_timeout` cho mỗi câu của checker |
| `invariant.sample-size` | int | `10` | mọi | **Không phải khóa**: hằng số theo DR-73 |

`ConfigurationGuard` (DR-147) thêm điều kiện: `invariant.active-overdue > reservation.expiry.interval` và `invariant.expiring-stuck > reservation.expiry.lease`, để job chạy đúng không bao giờ làm INV-08, INV-09 đỏ.

## 8. Metrics và log

| Metric | Kiểu | Label | Ghi chú |
| --- | --- | --- | --- |
| `ticket_invariant_violations` | G | `check` ∈ 14 giá trị `VIOLATION` của §2 (`POOL_UNIT_COUNT` … `OUTBOX_FAILED`) | Giá trị lần chạy gần nhất; đặt 0 cho mục sạch để chuỗi thời gian liên tục. Mục `REPORT` không phát qua metric này (hàng chờ hoàn tiền đã có `ticket_orders_refund_pending`, DOC-33) |
| `ticket_invariant_check_seconds` | H | — | Thời gian một lần `runAll()` |
| `ticket_job_runs_total{job="invariant_checker", result}` | C | (DOC-33) | `error` khi thoát 2 ở chế độ job |

Ở chế độ CLI không có endpoint Prometheus (`web-application-type: none`); metric chỉ có ý nghĩa ở chế độ job.

| Log | Mức | Trường |
| --- | --- | --- |
| `invariant.violation` | ERROR | `check`, `id`, `count`, `sample` (UUID, không có email) |
| `invariant.report` | INFO | `check`, `id`, `reason`, `count` |
| `invariant.clean` | INFO | `durationMs` |
| `invariant.failed` | ERROR | `error`, `check`, stack trace |

Không có alerting ở giai đoạn này (DOC-33 §1); người vận hành thấy sai lệch qua log ERROR, gauge trên Grafana (profile `obs`) hoặc `make invariants`.

## 9. Lỗi và cách xử lý

| Tình huống | Hành vi |
| --- | --- |
| Có vi phạm | Báo cáo, log ERROR, gauge > 0; **không** tự sửa, không tạm dừng sự kiện (SDD gốc 14.3). Người vận hành theo RB-02 |
| Một câu quá 30 giây | Chế độ job: log ERROR `invariant.failed`, `ticket_job_runs_total{result="error"}`, các mục còn lại không chạy lần này (transaction đã hỏng); gauge giữ giá trị lần trước. Chế độ CLI: thoát 2 |
| Mất kết nối database | Như trên; job thử lại ở nhịp sau |
| Lỗi SQL (cột đổi tên sau migration) | Thoát 2 với `QUERY_FAILED`; IC-01 bắt được trong CI |
| Sự kiện bị hủy giữa lần chạy | Snapshot `REPEATABLE READ` thấy trạng thái trước hoặc sau hủy, không thấy nửa vời |

## 10. Test bắt buộc

Tiền tố `IC-`. Mức theo DOC-69 §2. Mỗi ca "cố ý hỏng" chèn dữ liệu sai bằng SQL thô trong test (`TransactionTemplate`, DOC-12 §2.5), tạm gỡ ràng buộc khi cần (INV-03), rồi gọi `runAll()`.

| ID | Mức | Kịch bản | Kỳ vọng |
| --- | --- | --- | --- |
| IC-01 | I | Database sau `make seed` (3 sự kiện mẫu, DR-78) | `violations` rỗng; CLI thoát 0 |
| IC-02 | I | Pool GA capacity 100; đổi 1 unit `AVAILABLE` sang `REMOVED` bằng SQL | INV-01, `count` 1, `sample` = [poolId] |
| IC-03 | I | Sự kiện có phiên bản sơ đồ `sellable_seat_count` 180; xóa 1 unit ghế | INV-02, `sample` = [eventId] |
| IC-04 | I | `ALTER TABLE inventory_unit DROP CONSTRAINT unit_owner_ck`; đặt `reservation_id` cho unit `AVAILABLE` | INV-03 |
| IC-05 | I | Unit `HELD` của reservation đã `EXPIRED` | INV-04 và INV-05 cùng báo |
| IC-06 | I | Reservation `ACTIVE` có item `quantity` 3 nhưng chỉ 2 unit `HELD` | INV-05, `sample` = [reservationId] |
| IC-07 | I | Unit `SOLD` của sự kiện `PUBLISHED` không có vé | INV-06 (và INV-10 báo đơn) |
| IC-08 | I | Vé `ISSUED` trỏ tới unit `HELD` | INV-07 |
| IC-09 | I | Reservation `ACTIVE`, `expires_at` = now − 61 giây; và một cái now − 59 giây | INV-08 chỉ báo cái đầu |
| IC-10 | I | Reservation `EXPIRING`, `expiring_since` = now − 121 giây; và now − 119 giây | INV-09 chỉ báo cái đầu |
| IC-11 | I | Đơn `PAID` 3 vé chỉ có 2 vé `ISSUED` | INV-10 |
| IC-12 | I | Đơn `EXPIRED` với reservation `ACTIVE`; đơn `REFUND_PENDING` `LATE_PAYMENT` với reservation `CONFIRMED` | INV-11 báo cả hai |
| IC-13 | I | `orders.amount` = 450.000 trong khi item 2 × 100.000 + 1 × 200.000 | INV-12 |
| IC-14 | I | Sự kiện `CANCELLED` còn một đơn `PAID` | INV-13 |
| IC-15 | I | Một dòng outbox `FAILED` | INV-14; sau `UPDATE outbox SET status='PENDING', attempts=0, next_attempt_at=now()` (DR-93) thì sạch |
| IC-16 | I | Hủy sự kiện có 3 đơn `PAID` (8 vé) theo DR-28 | Không vi phạm; `reports` có INV-15 `EVENT_CANCELLED` `count` 3; thoát 0 |
| IC-17 | I | 15 pool hỏng | INV-01 `count` 15, `sample` 10 phần tử |
| IC-18 | I | Tắt PostgreSQL rồi chạy CLI | Thoát 2, JSON `error = DATABASE_UNAVAILABLE`, stdout chỉ có một dòng JSON |
| IC-19 | I | Đặt `invariant.statement-timeout = PT0.001S` | Thoát 2, `error = STATEMENT_TIMEOUT` |
| IC-20 | I | Có reservation `ACTIVE` quá hạn 2 phút; chạy CLI | INV-08 báo; sau khi CLI thoát reservation vẫn `ACTIVE` (không job nào chạy trong profile `invariants`) |
| IC-21 | I | Chạy một lần với vi phạm INV-01, rồi sửa, chạy lại | `ticket_invariant_violations{check="POOL_UNIT_COUNT"}` = 1 rồi 0 |
| IC-22 | U | `InvariantCheck` | Đúng 16 hằng, ID `INV-01…16` liên tiếp, không trùng tên |
| IC-23 | C | Chạy `runAll()` lặp liên tục trong lúc 64 luồng giữ, hủy và xác nhận vé 0 đồng (DR-77) | 0 vi phạm ở mọi lần chạy |
| IC-24 | I | Trong transaction của `runAll()`, thử `UPDATE event SET name = name` qua cùng `JdbcClient` (fixture) | PostgreSQL lỗi `25006 read-only transaction` |
| IC-25 | I | EXP-02 thu nhỏ (`SCALE=0.01`) với `inventory.strategy=naive` | Thoát 1, có INV-01 hoặc INV-05 |

## 11. Quyết định phát sinh khi viết tài liệu này

**DR-178 · Hai mức `VIOLATION` và `REPORT`; JSON thêm `id` và `reports`**
- **Vấn đề:** DR-73 đưa "số đơn `REFUND_PENDING` theo lý do" vào danh mục kiểm tra, nhưng hủy sự kiện hợp lệ (DR-28) luôn sinh đơn `REFUND_PENDING`; nếu coi là vi phạm thì `make invariants` đỏ sau mỗi lần hủy và test của DR-28 không thể dùng `InvariantsExtension`.
- **Quyết định:** INV-15, INV-16 là `REPORT`: nằm ở mảng `reports` của JSON, log INFO, không ảnh hưởng mã thoát hay test. Mỗi `Finding` có thêm `id` (`INV-xx`). Các trường của DR-73 giữ nguyên.
- **Hệ quả:** EXP-06 đọc `reports` để kiểm "0 đơn `LATE_PAYMENT`". JSON tương thích ngược (chỉ thêm trường).
- **Ghi vào:** DOC-30, DOC-70, DOC-76.

**DR-179 · Profile `invariants` không chạy web, migration, job và tác dụng phụ lúc khởi động**
- **Vấn đề:** `make invariants` khởi động toàn bộ ứng dụng; nếu `@Scheduled` chạy, tiến trình CLI tự trả vé quá hạn và che vi phạm, còn Flyway và tạo bucket là thao tác ghi trong một lệnh chỉ đọc.
- **Quyết định:** `web-application-type: none`, `spring.flyway.enabled: false`, `@EnableScheduling` chỉ ở `@Profile("!invariants")`, mọi khởi tạo có tác dụng phụ có `@Profile("!invariants")`; log ra stderr, stdout chỉ có JSON.
- **Hệ quả:** `make invariants` chạy được song song với `api` đang phục vụ; `make invariants | jq` dùng được.
- **Ghi vào:** DOC-30, DOC-34 §2 (mô tả profile), DOC-62.

**DR-180 · Mã thoát 2 khi không chạy hết**
- **Vấn đề:** DR-73 chỉ có 0 (sạch) và 1 (sai lệch); lỗi kết nối hoặc timeout không được phân biệt với sai lệch.
- **Quyết định:** Thoát 2 kèm JSON `error` ∈ {`DATABASE_UNAVAILABLE`, `STATEMENT_TIMEOUT`, `QUERY_FAILED`}.
- **Hệ quả:** `make exp` loại lần chạy với mọi mã ≠ 0; người vận hành biết "không biết" khác "có lỗi".
- **Ghi vào:** DOC-30, DOC-61 (`make invariants`), DOC-70.

**DR-181 · Khóa `invariant.statement-timeout` = `PT30S`**
- **Vấn đề:** Checker quét toàn bảng; không có giới hạn thì một câu chậm giữ snapshot và connection vô hạn.
- **Quyết định:** `SET LOCAL statement_timeout` = `invariant.statement-timeout` (mặc định `PT30S`) đầu transaction; transaction là `READ ONLY`, `REPEATABLE READ`.
- **Hệ quả:** DOC-34 §3.4 thêm một dòng; `ConfigurationGuard` thêm hai điều kiện ngưỡng ở §7.
- **Ghi vào:** DOC-34, DOC-30.

## Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-178…4 (§11), chờ đánh số khi hợp nhất. INV-11 cho phép rộng với đơn `AMOUNT_MISMATCH` và `EVENT_CANCELLED`; DOC-26 (P3) siết lại khi chốt đường đi của webhook.
