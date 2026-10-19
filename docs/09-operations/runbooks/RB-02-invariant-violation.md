# RB-02: Sai lệch bất biến

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-66 / RB-02
> Phụ thuộc: [DOC-30](../../06-design/invariant-checker.md) (danh mục `INV-xx`, câu SQL, mã thoát), [DOC-14](../../05-data/domain-model.md) §4–§7, [DOC-15](../../05-data/ops-model.md) §7, [DOC-37](../../07-api/api-endpoints.md) (E-30), [DOC-61](../local-dev.md) (`make psql`, `make invariants`, `make logs`), [DOC-33](../../06-design/observability.md), [Sổ quyết định](../../00-decision-register.md) (DR-28, 42, 44, 53, 73, 93)
> Người dùng chính: PS-5 người vận hành/nghiên cứu; P2-11; người chạy thực nghiệm khi `make invariants` thoát khác 0
>
> Alert: không có (không alerting ở giai đoạn này, DOC-33) · Dashboard: Grafana profile `obs`, panel `ticket_invariant_violations` · Liên quan: DOC-30 §3, FL-18 (DOC-86)

Runbook dùng khi `make invariants` thoát 1 (hoặc 2), khi log có `invariant.violation` mức ERROR, hoặc khi gauge `ticket_invariant_violations{check}` > 0. Mỗi mục `INV-xx` có: ý nghĩa, câu SQL điều tra, hướng xử lý, khi nào dừng bán. Lệnh kiểm tra không tự sửa gì (SDD gốc §14.3); mọi sửa dữ liệu ở đây là thao tác tay có chủ đích, chạy trong `BEGIN … COMMIT` và kiểm số dòng.

## Triệu chứng và ảnh hưởng

| Dấu hiệu | Nguồn |
| --- | --- |
| `make invariants` thoát 1, JSON có `violations[]` khác rỗng | DOC-30 §5 |
| `make invariants` thoát 2, JSON có `error` | Không chạy hết: xem mục "Thoát 2" cuối tài liệu |
| Log ERROR `invariant.violation` với `check`, `count`, `sample` | `make logs S=api` |
| Gauge `ticket_invariant_violations{check="…"}` > 0 | Prometheus (profile `obs`) |
| Lần chạy thực nghiệm bị đánh `invariants: FAIL` | `make exp` (DOC-70) |

Ảnh hưởng theo nhóm:

| Nhóm | Mục | Ảnh hưởng | Dừng bán? |
| --- | --- | --- | --- |
| Kho vé (bán vượt có thể xảy ra) | INV-01, 02, 03, 04, 05 | Số vé bán được khác sức chứa; vé có thể bán quá hoặc mất | **Có**, tạm dừng sự kiện bị ảnh hưởng ngay |
| Tiền và vé | INV-06, 07, 10, 11, 12, 13 | Người mua trả tiền mà không có vé, có vé mà không trả tiền, hoặc số tiền sai | Có nếu `count` tăng giữa hai lần chạy; không nếu là một đơn lẻ đã đóng |
| Job kẹt | INV-08, 09 | Vé không được trả về kho đúng hạn (NFR-03) | Không; sửa job |
| Email | INV-14 | Người mua không nhận được email | Không |

## Bắt đầu

1. Lưu JSON của lần chạy: `make invariants > /tmp/inv-$(date +%s).json; echo "exit=$?"`.
2. Ghi lại `git rev-parse --short HEAD`, giờ, sự kiện nào đang mở bán.
3. Mở `make psql`. Mọi câu dưới dùng biến `psql`: thay UUID trong `\set` bằng một ID trong `sample` của JSON.

```sql
\set id '0199b1c2-5001-7aaa-8bbb-111122223333'
```

4. Tìm sự kiện của đối tượng (dùng ở bước dừng bán):

```sql
-- pool → event
SELECT event_id FROM inventory_pool WHERE pool_id = :'id';
-- unit → event
SELECT event_id FROM inventory_unit WHERE unit_id = :'id';
-- reservation hoặc order → event
SELECT event_id FROM reservation WHERE reservation_id = :'id'
UNION ALL SELECT event_id FROM orders WHERE order_id = :'id';
```

### Dừng bán một sự kiện

Cách ưu tiên: người tổ chức bấm "Tạm dừng bán" ở Studio (E-30). Khi không có giao diện, chạy SQL tương đương E-30 (cùng điều kiện trạng thái, tăng `row_version`):

```sql
\set e '0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34'
BEGIN;
UPDATE event SET status = 'PAUSED', row_version = row_version + 1, updated_at = now()
 WHERE event_id = :'e' AND status = 'PUBLISHED';     -- kỳ vọng UPDATE 1
COMMIT;
```

`PAUSED` chặn giữ vé mới (409 `EVENT_NOT_ON_SALE`); reservation đang mở vẫn thanh toán tới hết hạn (DR-24). Người mua thấy thay đổi chậm tối đa 5 giây (cache E-07). Mở lại bằng E-31 sau khi đã xử lý xong và `make invariants` thoát 0.

## Kiểm tra và xử lý theo mục

### INV-01 `POOL_UNIT_COUNT`

- **Ý nghĩa:** số unit chưa `REMOVED` của pool khác `capacity`. Thừa unit → bán được quá sức chứa; thiếu → bán thiếu.
- **Điều tra:**

```sql
SELECT p.pool_id, p.event_id, p.kind, p.name, p.capacity, p.removed_at,
       count(*) FILTER (WHERE u.status = 'AVAILABLE') AS available,
       count(*) FILTER (WHERE u.status = 'HELD')      AS held,
       count(*) FILTER (WHERE u.status = 'SOLD')      AS sold,
       count(*) FILTER (WHERE u.status = 'REMOVED')   AS removed
FROM inventory_pool p LEFT JOIN inventory_unit u ON u.pool_id = p.pool_id
WHERE p.pool_id = :'id'
GROUP BY p.pool_id;
-- Unit đổi gần đây nhất: tìm thời điểm lệch
SELECT unit_id, status, reservation_id, updated_at FROM inventory_unit
WHERE pool_id = :'id' ORDER BY updated_at DESC LIMIT 20;
```

- **Xử lý:** dừng bán. Nếu `held + sold > capacity`: đã bán vượt; giữ nguyên dữ liệu, ghi lại các `reservation_id` dư (unit `updated_at` muộn nhất), báo Owner; hoàn tiền đơn thừa theo RB-01 (P3). Nếu chỉ `available` lệch (thường do giảm sức chứa bị ngắt giữa chừng hoặc sửa tay): đưa số unit về đúng `capacity` bằng E-35 (đổi `gaCapacity` về giá trị đúng) thay vì SQL tay. Tìm nguyên nhân trong log quanh `updated_at` (`make logs S=api | grep <poolId>`); với thực nghiệm, `inventory.strategy` khác `skip-locked` là nguyên nhân thường gặp (DR-76).

### INV-02 `SEAT_UNIT_COUNT`

- **Ý nghĩa:** số unit ghế chưa `REMOVED` của sự kiện khác số ghế bán được của phiên bản sơ đồ đang dùng (P5 trở đi).
- **Điều tra:**

```sql
SELECT e.event_id, e.status, v.version_no, v.sellable_seat_count,
       count(u.unit_id) FILTER (WHERE u.status <> 'REMOVED') AS seat_units
FROM event e
JOIN seat_map_version v ON v.seat_map_version_id = e.seat_map_version_id
LEFT JOIN inventory_unit u ON u.event_id = e.event_id AND u.seat_id IS NOT NULL
WHERE e.event_id = :'id'
GROUP BY e.event_id, v.version_no, v.sellable_seat_count;
```

- **Xử lý:** dừng bán. Lệch thường do dựng lại kho vé khi xuất bản phiên bản mới (DR-37) bị lỗi giữa chừng; đối chiếu `seat_id` trong `seat_map_version.document` với unit (DOC-23). Không tự chèn unit bằng tay; báo Owner.

### INV-03 `UNIT_OWNER`

- **Ý nghĩa:** unit `AVAILABLE`/`REMOVED` còn trỏ tới reservation, hoặc `HELD`/`SOLD` không có chủ. Chỉ xảy ra khi ràng buộc `unit_owner_ck` đã bị gỡ.
- **Điều tra:**

```sql
SELECT conname FROM pg_constraint WHERE conrelid = 'inventory_unit'::regclass AND conname = 'unit_owner_ck';
SELECT unit_id, status, reservation_id, updated_at FROM inventory_unit WHERE unit_id = :'id';
```

- **Xử lý:** dừng bán. Không có `unit_owner_ck` → tìm migration đã gỡ (`SELECT version, description FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;`), viết migration mới thêm lại ràng buộc sau khi sửa dữ liệu. Báo Owner trước khi sửa.

### INV-04 `HELD_ORPHAN`

- **Ý nghĩa:** unit `HELD` mà reservation đã đóng hoặc không tồn tại: vé bị giữ mãi, không ai trả.
- **Điều tra:**

```sql
SELECT u.unit_id, u.pool_id, u.updated_at, r.reservation_id, r.status, r.close_reason, r.closed_at
FROM inventory_unit u LEFT JOIN reservation r ON r.reservation_id = u.reservation_id
WHERE u.unit_id = :'id';
```

- **Xử lý:** dừng bán. Nếu reservation `EXPIRED`/`CANCELLED` và đơn không `PAID`, trả unit về kho bằng đúng câu trả vé của DOC-14 §7, có điều kiện:

```sql
\set r '0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091'
BEGIN;
UPDATE inventory_unit SET status = 'AVAILABLE', reservation_id = NULL, updated_at = now()
 WHERE reservation_id = :'r' AND status = 'HELD'
   AND EXISTS (SELECT 1 FROM reservation WHERE reservation_id = :'r' AND status IN ('EXPIRED','CANCELLED'));
-- kỳ vọng: số dòng = số unit HELD của reservation đó
COMMIT;
```

Nếu reservation `CONFIRMED` (unit lẽ ra `SOLD`): không sửa, báo Owner (đường xác nhận bị lỗi).

### INV-05 `RESERVATION_UNIT_COUNT`

- **Ý nghĩa:** số unit của reservation không khớp Σ `quantity` của item theo trạng thái (mở: `HELD`; `CONFIRMED`: `SOLD`; đóng: 0).
- **Điều tra:**

```sql
SELECT r.reservation_id, r.status, r.close_reason,
       (SELECT sum(quantity) FROM reservation_item i WHERE i.reservation_id = r.reservation_id) AS expected,
       (SELECT count(*) FROM inventory_unit u WHERE u.reservation_id = r.reservation_id AND u.status = 'HELD') AS held,
       (SELECT count(*) FROM inventory_unit u WHERE u.reservation_id = r.reservation_id AND u.status = 'SOLD') AS sold
FROM reservation r WHERE r.reservation_id = :'id';
```

- **Xử lý:** dừng bán. Giữ thiếu (claim trả ít dòng mà vẫn commit) là lỗi nghiêm trọng của `InventoryClaimer` (DOC-24); giữ thừa đi kèm INV-01. Với reservation đóng còn unit `HELD`: xử lý như INV-04. Các trường hợp còn lại: không sửa tay, báo Owner, giữ JSON và log.

### INV-06 `SOLD_WITHOUT_TICKET`

- **Ý nghĩa:** unit `SOLD` của sự kiện chưa hủy không có đúng một vé `ISSUED`: người mua trả tiền nhưng thiếu vé.
- **Điều tra:**

```sql
SELECT u.unit_id, u.reservation_id, o.order_id, o.status AS order_status,
       t.ticket_id, t.status AS ticket_status, t.code
FROM inventory_unit u
JOIN orders o ON o.reservation_id = u.reservation_id
LEFT JOIN ticket t ON t.unit_id = u.unit_id
WHERE u.unit_id = :'id';
```

- **Xử lý:** dừng bán nếu nhiều unit cùng sự kiện. Phát hành vé thiếu là việc của transaction xác nhận (DOC-27); không chèn `ticket` bằng tay vì mã vé sinh ở Java (DR-52). Báo Owner, ghi `order_id`.

### INV-07 `TICKET_WITHOUT_SALE`

- **Ý nghĩa:** vé `ISSUED` mà unit không `SOLD`, đơn không `PAID`, hoặc unit thuộc reservation khác: vé không có tiền tương ứng.
- **Điều tra:**

```sql
SELECT t.ticket_id, t.code, t.status, u.unit_id, u.status AS unit_status, u.reservation_id AS unit_res,
       o.order_id, o.status AS order_status, o.reservation_id AS order_res
FROM ticket t JOIN inventory_unit u ON u.unit_id = t.unit_id JOIN orders o ON o.order_id = t.order_id
WHERE t.ticket_id = :'id';
```

- **Xử lý:** dừng bán. Vé hợp lệ ở tay người mua nhưng không có tiền là rủi ro thật; không `VOID` vé tự động, báo Owner quyết định.

### INV-08 `ACTIVE_OVERDUE`

- **Ý nghĩa:** reservation `ACTIVE` quá `expires_at` hơn 60 giây: `ReleaseExpiredHoldsJob` không chạy hoặc không theo kịp (DR-42).
- **Điều tra:**

```sql
SELECT count(*), min(expires_at), now() - min(expires_at) AS max_overdue
FROM reservation WHERE status = 'ACTIVE' AND expires_at < now() - interval '60 seconds';
```

```bash
make logs S=api | grep -E 'release_expired|ReleaseExpiredHoldsJob' | tail -20
```

Prometheus: `increase(ticket_job_runs_total{job="release_expired"}[5m])` (0 là job chết), `ticket_expiry_batch_size` luôn 200 (job không theo kịp).

- **Xử lý:** không dừng bán. Job chết → khởi động lại `api` (`docker compose … restart api`); job chạy nhưng không kịp → kiểm `reservation.expiry.interval`, `batch-size` (DOC-34) và thời gian mỗi lô (`ticket_job_duration_seconds{job="release_expired"}`). Không trả vé bằng tay: job tự xử lý khi chạy lại.

### INV-09 `EXPIRING_STUCK`

- **Ý nghĩa:** reservation `EXPIRING` có `expiring_since` cũ hơn 2 phút: job không lấy lại được lease, thường do hủy PaymentIntent lỗi liên tục (P3).
- **Điều tra:**

```sql
SELECT r.reservation_id, r.close_reason, r.expiring_since, o.order_id, o.payment_intent_id, o.status
FROM reservation r JOIN orders o ON o.reservation_id = r.reservation_id
WHERE r.status = 'EXPIRING' AND r.expiring_since < now() - interval '2 minutes'
ORDER BY r.expiring_since LIMIT 20;
```

Prometheus: `increase(ticket_payment_cancel_failures_total[10m])`.

- **Xử lý:** không dừng bán. Không có `payment_intent_id` (P2): job lỗi, xử lý như INV-08. Có PaymentIntent: kiểm Stripe/cổng giả; khi P3 có RB-03 thì theo RB-03.

### INV-10 `PAID_ORDER_TICKETS`

- **Ý nghĩa:** đơn `PAID` mà reservation không `CONFIRMED` hoặc số vé `ISSUED` khác số vé đã mua.
- **Điều tra:**

```sql
SELECT o.order_id, o.status, o.paid_at, r.status AS res_status,
       (SELECT sum(quantity) FROM reservation_item i WHERE i.reservation_id = o.reservation_id) AS bought,
       (SELECT count(*) FROM ticket t WHERE t.order_id = o.order_id AND t.status = 'ISSUED') AS issued
FROM orders o JOIN reservation r ON r.reservation_id = o.reservation_id
WHERE o.order_id = :'id';
```

- **Xử lý:** như INV-06; báo Owner kèm `order_id`. Dừng bán nếu nhiều đơn của cùng sự kiện.

### INV-11 `ORDER_RESERVATION_STATE`

- **Ý nghĩa:** cặp trạng thái đơn/reservation không có trong bảng của DOC-30 §3.3: một transaction đã đổi một bên mà không đổi bên kia.
- **Điều tra:**

```sql
SELECT o.order_id, o.status, o.refund_reason, o.updated_at,
       r.reservation_id, r.status AS res_status, r.close_reason, r.closed_at
FROM orders o JOIN reservation r ON r.reservation_id = o.reservation_id
WHERE o.order_id = :'id';
```

- **Xử lý:** không sửa tay; tìm transaction trong log theo `order_id` và `reservation_id`. Dừng bán nếu có unit `HELD`/`SOLD` lệch đi kèm (INV-04, INV-05 cùng báo).

### INV-12 `ORDER_AMOUNT`

- **Ý nghĩa:** `orders.amount` khác Σ `quantity × unit_price` của item: thu sai tiền.
- **Điều tra:**

```sql
SELECT o.order_id, o.amount, sum(i.quantity::bigint * i.unit_price) AS items_total
FROM orders o JOIN reservation_item i ON i.reservation_id = o.reservation_id
WHERE o.order_id = :'id' GROUP BY o.order_id, o.amount;
```

- **Xử lý:** đơn chưa thanh toán: để hết hạn, không sửa. Đơn `PAID`: báo Owner, hoàn phần chênh theo RB-01 (P3). Dừng bán nếu lặp lại ở đơn mới (lỗi tính tiền ở `HoldService`).

### INV-13 `CANCELLED_EVENT_SALES`

- **Ý nghĩa:** sự kiện `CANCELLED` còn đơn `PAID` hoặc vé `ISSUED`: lệnh hủy và webhook đã đua sai (DR-28).
- **Điều tra:**

```sql
SELECT e.event_id, e.cancelled_at,
       (SELECT count(*) FROM orders o WHERE o.event_id = e.event_id AND o.status = 'PAID') AS paid_orders,
       (SELECT count(*) FROM ticket t WHERE t.event_id = e.event_id AND t.status = 'ISSUED') AS issued_tickets
FROM event e WHERE e.event_id = :'id';
SELECT order_id, paid_at FROM orders WHERE event_id = :'id' AND status = 'PAID' ORDER BY paid_at;
```

- **Xử lý:** sự kiện đã hủy nên không còn bán. Chuyển đơn và vé đúng như bước 3–4 của lệnh hủy (DR-28), có điều kiện, rồi tạo email chờ hoàn tiền bằng cách báo Owner (outbox do code dựng payload; không chèn tay):

```sql
\set e '0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34'
BEGIN;
UPDATE orders SET status = 'REFUND_PENDING', refund_reason = 'EVENT_CANCELLED', updated_at = now()
 WHERE event_id = :'e' AND status = 'PAID'
   AND EXISTS (SELECT 1 FROM event WHERE event_id = :'e' AND status = 'CANCELLED')
 RETURNING order_id;
UPDATE ticket SET status = 'VOID'
 WHERE event_id = :'e' AND status = 'ISSUED'
   AND EXISTS (SELECT 1 FROM event WHERE event_id = :'e' AND status = 'CANCELLED');
COMMIT;
```

Ghi lại danh sách `order_id` trả về; đây là lỗi tranh chấp phải có test tái hiện (master plan §7.2).

### INV-14 `OUTBOX_FAILED`

- **Ý nghĩa:** email đã thử 12 lần hoặc bị từ chối vĩnh viễn (DR-53, DR-105).
- **Điều tra:**

```sql
SELECT outbox_id, kind, attempts, last_error, created_at, payload->>'orderId' AS order_id
FROM outbox WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT 20;
```

- **Xử lý:** không dừng bán. Sửa nguyên nhân (SMTP, địa chỉ sai), rồi đặt lại để `OutboxRelay` gửi lại (DR-93):

```sql
BEGIN;
UPDATE outbox SET status = 'PENDING', attempts = 0, next_attempt_at = now(), last_error = NULL
 WHERE outbox_id = :'id' AND status = 'FAILED';     -- kỳ vọng UPDATE 1
COMMIT;
```

Thư không thể gửi (địa chỉ không tồn tại): giữ `FAILED`; mục sẽ hết khi `RetentionJob` xóa sau 30 ngày (DR-74). Ghi nhận ngoại lệ đó trong ghi chú thực nghiệm để lần chạy không bị loại oan.

### INV-15 `REFUND_PENDING`, INV-16 `LATE_PAYMENT_24H` (báo cáo)

Không phải sai lệch; `make invariants` vẫn thoát 0. INV-15 là hàng chờ hoàn tiền thủ công (RB-01, P3). INV-16 khác 0 nghĩa là có thanh toán đến trễ trong 24 giờ, cần xem log ERROR của luồng trễ (DR-44); trong EXP-06 kỳ vọng 0.

### Thoát 2 (không chạy hết)

| `error` | Kiểm tra | Xử lý |
| --- | --- | --- |
| `DATABASE_UNAVAILABLE` | `docker compose … ps postgres`, `make logs S=postgres` | Khởi động lại `postgres`, chạy lại `make invariants` |
| `STATEMENT_TIMEOUT` | `check` trong JSON; `SELECT pid, now() - query_start, query FROM pg_stat_activity WHERE state <> 'idle' ORDER BY 2 DESC;` | Chạy lại khi tải thấp; nếu lặp lại thì chạy lại với đối số `--invariant.statement-timeout=PT60S` và ghi vào DOC-30 §3.4 số đo |
| `QUERY_FAILED` | stderr của lệnh | Lỗi code sau migration: sửa câu SQL trong `InvariantQueries`, không bỏ mục |

## Xác nhận đã xử lý xong

1. `make invariants; echo "exit=$?"` → `exit=0` và `"violations": []`.
2. Gauge `ticket_invariant_violations` về 0 cho mọi `check` ở lần chạy định kỳ kế tiếp (≤ 5 phút).
3. Sự kiện đã tạm dừng được mở lại bằng E-31 chỉ sau bước 1; sau khi mở lại, một lệnh giữ vé thử trả 201.

## Phòng ngừa và việc tiếp theo

- Mỗi sai lệch thật (không phải dữ liệu thử sửa tay) có một issue kèm JSON, log và test tái hiện (master plan §7.2); test mới dùng `@ExpectViolation` hoặc chứng minh không còn vi phạm.
- Trong thực nghiệm, lần chạy có `make invariants` khác 0 bị loại và chạy lại sau khi sửa (FR-12.4).
- Nếu cùng một `check` đỏ lặp lại sau sửa, thêm kiểm tra vào code (ràng buộc database hoặc test đồng thời ở DOC-24) thay vì sửa dữ liệu lần nữa.
