# Kho vé và giữ vé

> Trạng thái: **Draft** · Cập nhật: 2026-10-14 · DOC-24
> Phụ thuộc: SDD gốc §8.1–8.5, §10.3, §10.4, [Sổ quyết định](../00-decision-register.md) (DR-11, 12, 17, 20, 24, 27, 28, 30, 41, 42, 43, 44, 45, 46, 61, 76, 90, 91, 108, 152, 153), [DOC-06](../02-glossary.md), [DOC-07](../03-architecture/system-context-and-containers.md) §4, [DOC-12](../03-architecture/code-architecture.md) §2–§3, [DOC-14](../05-data/domain-model.md) §4.4, §5, §6.2–6.4, §7, §8, [DOC-15](../05-data/ops-model.md) §5, §8.2, [DOC-25](idempotency.md), [DOC-33](observability.md) §4.2, [DOC-34](configuration-reference.md) §3.4, [DOC-35](error-handling.md) §2–§4, [DOC-37](../07-api/api-endpoints.md) (E-14, E-15, E-16, E-24, E-35, E-36, E-37), [ADR-0003](../04-adr/0003-one-row-per-ticket-skip-locked.md), [ADR-0004](../04-adr/0004-reservation-row-as-arbiter-expiring-state.md)
> Người dùng chính: P2-06 (`SkipLockedClaimer`), P2-08 (giữ vé GA), P2-09 (job trả vé, hủy giữ đường nhanh), P2-05.1 (tạo pool khi xuất bản, đổi sức chứa), P2-13 (`CounterClaimer`, `NaiveClaimer`), P3 (hủy PaymentIntent trong thủ tục trả vé), P5-02 (claim ghế và zone); [DOC-20](events-and-ticket-types.md), [DOC-26](checkout-and-payment.md), [DOC-30](invariant-checker.md), [DOC-85](flows/ga-purchase.md), EXP-02, EXP-04, EXP-10

Tài liệu mô tả kho vé (`inventory_pool`, `inventory_unit`) và vòng giữ vé: transaction giữ vé từng câu, câu claim ghế và pool, mã lỗi 409, thủ tục trả vé dùng chung cho job và hủy giữ đường nhanh, job trả vé, đổi sức chứa, ba bản cài đặt `InventoryClaimer` và tác động của index một phần lên bảng nóng. Nó không định nghĩa DDL (DOC-14 §4.4), giao thức idempotency (DOC-25), transaction xác nhận thanh toán và hủy PaymentIntent (DOC-26), transaction xuất bản sự kiện (DOC-20; tài liệu này chỉ cung cấp câu SQL tạo pool của `inventory`), cờ hết vé và kiểm tra lượt vào (DOC-28, DOC-17) hay snapshot tình trạng chỗ (DOC-29). Payload endpoint ở DOC-37.

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi thành DR-167…173, DR-182 và tóm tắt ở §13; DR-155 (index `unit_event_idx`) do Claude chốt trước khi viết.

## 1. Mục đích và phạm vi

| Lớp chính | Module · package | Chạy ở | Kích hoạt | Đọc | Ghi | Việc |
| --- | --- | --- | --- | --- | --- | --- |
| `ReservationController` | `reservation.controller` | `api` | E-14, E-15, E-16 | — | — | Nhận request, `@Valid`, gọi service |
| `HoldService` | `reservation.service` | `api` | E-14 | Redis (cờ hết vé) | — | Điều phối không transaction: kiểm tra hình thức, bulkhead, gợi ý Redis, metric; gọi `HoldTxService` |
| `HoldTxService` | `reservation.service` | `api` | `HoldService` | `event`, `ticket_type`, `inventory_pool` (qua `…Api`) | `idempotency_key`, `reservation`, `reservation_item`, `inventory_unit`, `orders` (qua `…Api`) | Transaction giữ vé (DR-41) |
| `ReleaseService` | `reservation.service` | `api` | job, E-16, hủy sự kiện (gián tiếp qua job) | `orders.payment_intent_id` (qua `OrderApi`) | — | Điều phối không transaction của thủ tục trả vé (DR-43): hủy PaymentIntent ngoài transaction, rồi gọi `ReleaseTxService` |
| `ReleaseTxService` | `reservation.service` | `api` | `ReleaseService`, job | `reservation` | `reservation`, `inventory_unit`, `orders` | Các transaction ngắn của job và của đường nhanh |
| `ReleaseExpiredHoldsJob` | `reservation.job` | `api` | `@Scheduled` mỗi `reservation.expiry.interval` | — | — | Gọi `ReleaseService.runOnce()` |
| `InventoryService` | `inventory.service` | `api` | `InventoryApi` | `inventory_pool`, `inventory_unit` | như cột Đọc | Cài đặt `InventoryApi`: claim, xác nhận, trả, tạo pool, đổi sức chứa, đếm |
| `InventoryClaimer` (`SkipLockedClaimer`, `CounterClaimer`, `NaiveClaimer`) | `inventory.repository` | `api` | `InventoryService` | `inventory_unit`, `inventory_pool_counter` | như cột Đọc | Câu claim, chọn bằng `inventory.strategy` (DR-76) |

Tài liệu bao các thao tác GA của P2; claim ghế và zone dùng cùng câu và cùng transaction, được bật ở P5 (P5-02) không đổi thiết kế. Hủy PaymentIntent trong thủ tục trả vé được thiết kế ở đây dưới dạng SPI (§3.4) và được cài đặt ở P3 (DOC-26).

## 2. Bất biến

| # | Bất biến | Được ép bằng | Kiểm tra lại ở |
| --- | --- | --- | --- |
| B1 | Mỗi unit có tối đa một chủ: một reservation đang mở hoặc một vé đã bán (SDD gốc 8.1) | Câu claim `… status = 'AVAILABLE' FOR UPDATE SKIP LOCKED`; `unit_owner_ck` (`status IN ('HELD','SOLD')` ⇔ `reservation_id IS NOT NULL`, DOC-14 §4.4); `ticket_unit_issued_uq` | DOC-30 (unit ↔ vé) |
| B2 | Pool không vượt sức chứa: số unit `HELD` + `SOLD` của pool ≤ `capacity` | Số dòng unit chưa `REMOVED` của pool **bằng** `inventory_pool.capacity`; không thể claim dòng không tồn tại (ADR-0003) | DOC-30 (đếm unit theo pool) |
| B3 | `capacity` của pool = số unit của pool có `status <> 'REMOVED'` | Tăng/giảm sức chứa đổi số dòng và `capacity` trong cùng transaction (§4.7); xuất bản chèn đúng `capacity` dòng (§4.8) | DOC-30 |
| B4 | Unit `REMOVED` không bao giờ bị claim, không có chủ | Câu claim lọc `status = 'AVAILABLE'`; `unit_owner_ck` | DOC-30 |
| B5 | Reservation rời `ACTIVE` đúng một lần; rời `EXPIRING` đúng một lần | `UPDATE reservation … WHERE status = 'ACTIVE'` / `'EXPIRING'`, kiểm số dòng (ADR-0004) | DOC-30 (`ACTIVE` quá hạn > 60 giây, `EXPIRING` > 2 phút, DR-42) |
| B6 | Mỗi người tối đa một reservation `ACTIVE` hoặc `EXPIRING` mỗi sự kiện | `reservation_open_uq` (DOC-14 §4.4) | — (ràng buộc, không thể sai) |
| B7 | Unit `HELD` thuộc một reservation `ACTIVE` hoặc `EXPIRING`; unit của reservation đã đóng không còn `HELD` | Thủ tục trả vé và xác nhận đổi reservation và unit trong cùng transaction (§4.4, DOC-26) | DOC-30 |
| B8 | Số unit `HELD`/`SOLD` của một reservation = Σ `quantity` của các `reservation_item` | Transaction giữ vé chèn item sau khi claim đủ (§4.1 bước T6); thiếu thì rollback | DOC-30 |

Kho vé không có cột bộ đếm: số vé còn lại là số unit `AVAILABLE` (SDD gốc 8.1). Redis không quyết định gì ở đây; cờ hết vé chỉ là gợi ý (§4.1 bước P4).

## 3. Thành phần và interface

### 3.1 Phân bổ class

```
io.ticket.inventory
├── InventoryApi.java                 # §3.2
├── ClaimItem.java, ClaimResult.java, PoolQuantity.java, GaPoolSpec.java, UnitCounts.java   # record
├── SeatsUnavailableException.java, InsufficientCapacityException.java, CapacityBelowUsedException.java
├── repository/
│   ├── InventoryClaimer.java         # §3.3
│   ├── SkipLockedClaimer.java, CounterClaimer.java, NaiveClaimer.java
│   ├── InventoryUnitRepository.java  # @Query đọc (đếm, số liệu)
│   └── InventoryPoolRepository.java  # @Query đọc, @Modifying đổi capacity
├── service/InventoryService.java     # @Service implements InventoryApi
└── config/InventoryStrategyConfig.java

io.ticket.reservation
├── ReservationApi.java, PaymentIntentCanceller.java (SPI §3.4), ActiveReservationExistsException.java
├── controller/ReservationController.java
├── job/ReleaseExpiredHoldsJob.java
├── service/HoldService.java, HoldTxService.java, ReleaseService.java, ReleaseTxService.java
└── repository/ReservationRepository.java, ReservationItemWriter.java
```

`HoldService`/`ReleaseService` không có `@Transactional`; `HoldTxService`/`ReleaseTxService` chỉ có method `public @Transactional`. Tách như vậy theo DR-153 vì bước kiểm tra trước (bulkhead, Redis) phải chạy **trước khi lấy connection**, và thủ tục trả vé gọi Stripe giữa hai transaction (DR-167).

### 3.2 `InventoryApi`

```java
package io.ticket.inventory;

public interface InventoryApi {

  /** Trong transaction của HoldTxService. Claim ghế trước, rồi từng pool theo poolId tăng dần.
   *  Thiếu → ném SeatsUnavailableException / InsufficientCapacityException (bên gọi để rollback). */
  @Transactional(propagation = Propagation.MANDATORY)
  ClaimResult claim(UUID eventId, UUID reservationId, List<ClaimItem> items);

  /** HELD → SOLD cho mọi unit của reservation (DOC-26). Trả số dòng. */
  @Transactional(propagation = Propagation.MANDATORY)
  int confirmSold(UUID reservationId, List<PoolQuantity> poolItems);

  /** HELD → AVAILABLE, reservation_id = NULL. Trả các poolId có unit được trả (để xóa cờ hết vé). */
  @Transactional(propagation = Propagation.MANDATORY)
  Set<UUID> release(UUID reservationId, List<PoolQuantity> poolItems);

  /** Xuất bản sự kiện GA: chèn pool và `capacity` unit cho mỗi loại vé GA trong một câu (§4.8). */
  @Transactional(propagation = Propagation.MANDATORY)
  int createGaPools(UUID eventId, List<GaPoolSpec> specs);

  /** Đổi sức chứa pool GA (DR-30, §4.7). Giảm thiếu → CapacityBelowUsedException(used). */
  @Transactional(propagation = Propagation.MANDATORY)
  CapacityChange changeGaCapacity(UUID eventId, UUID ticketTypeId, int newCapacity);

  /** Xóa loại vé đã xuất bản: AVAILABLE → REMOVED, pool.removed_at (DR-26). Có HELD/SOLD → TicketTypeInUse. */
  @Transactional(propagation = Propagation.MANDATORY)
  int removeTicketType(UUID eventId, UUID ticketTypeId);

  /** Pool đang dùng của sự kiện (ZONE và GA chưa removed). Đọc trong transaction giữ vé. */
  List<PoolView> poolsOf(UUID eventId);

  long countAvailable(UUID poolId);                                   // §4.9
  Map<UUID, UnitCounts> countsByTicketType(UUID eventId);             // §4.9, DR-155
  Map<UUID, UnitCounts> countsByEvent(Collection<UUID> eventIds);     // E-24, ≤ 20 event

  /** Còn unit AVAILABLE theo loại vé, cho displayStatus và soldOut ở P2 (DOC-20 §3.2, DR-161). Cache Caffeine theo eventId. */
  Map<UUID, AvailabilitySummary> availabilityOf(Collection<UUID> eventIds);

  /** Từ P5 (DOC-23): unit ghế từ tài liệu sơ đồ truyền dạng jsonb, pool ZONE (DR-27 bước 3–4, DR-79). */
  @Transactional(propagation = Propagation.MANDATORY)
  int createSeatUnits(UUID eventId, Map<String, UUID> ticketTypeIdsByKey, String documentJson);
}

public record ClaimItem(Kind kind, List<UUID> seatIds, UUID poolId, int quantity) {
  public enum Kind { SEAT, POOL }
}
public record ClaimedSeat(UUID seatId, UUID ticketTypeId, String sectionName, String rowLabel, String seatNumber) {}
public record ClaimResult(List<ClaimedSeat> seats, Map<UUID, Integer> poolUnits) {}
public record PoolQuantity(UUID poolId, int quantity) {}
public record GaPoolSpec(UUID ticketTypeId, String name, int capacity) {}
public record UnitCounts(long available, long held, long sold) {
  public long capacity() { return available + held + sold; }
}
public record AvailabilitySummary(Map<UUID, Boolean> hasAvailableByTicketType) {}
public record CapacityChange(UUID poolId, int oldCapacity, int newCapacity, int unitsAdded, int unitsRemoved) {}
```

`poolItems` ở `confirmSold`/`release` chỉ dùng cho `CounterClaimer` (cộng trừ bộ đếm, §5); hai bản kia bỏ qua tham số và làm việc theo `reservation_id` (DR-173). `ReleaseTxService` và transaction xác nhận (DOC-26, qua `ReservationApi.itemsOf`) lấy `poolItems` từ `reservation_item` của module `reservation`.

### 3.3 `InventoryClaimer`

```java
package io.ticket.inventory.repository;

/** Một bản cài đặt được chọn bằng inventory.strategy (DR-76). Mọi method chạy trong transaction của bên gọi. */
public interface InventoryClaimer {
  /** Trả các ghế claim được; bên gọi so với seatIds. */
  List<ClaimedSeat> claimSeats(UUID eventId, UUID reservationId, List<UUID> seatIds);
  /** Trả số unit claim được (0…quantity). */
  int claimPool(UUID poolId, UUID reservationId, int quantity);
  int confirm(UUID reservationId, List<PoolQuantity> poolItems);
  Set<UUID> release(UUID reservationId, List<PoolQuantity> poolItems);
}
```

`InventoryStrategyConfig` tạo đúng một bean theo `inventory.strategy` (`skip-locked` | `counter` | `naive`). Giá trị khác `skip-locked` mà profile `experiment` không bật → `IllegalStateException` lúc khởi động, `api` dừng (DR-76; `ConfigurationGuard` của DOC-34 §6).

### 3.4 Interface phía `reservation`

```java
package io.ticket.reservation;

/** SPI do module payment cài đặt (DOC-07 §3.1, DR-79). Gọi NGOÀI transaction. */
public interface PaymentIntentCanceller {
  CancelOutcome cancel(UUID orderId, String paymentIntentId, Duration timeout);

  enum CancelOutcome {
    CANCELED,          // đã hủy, hoặc Stripe báo đã canceled từ trước → được trả vé
    SUCCEEDED,         // tiền đã về: không trả vé; payment tự chạy xác nhận (DOC-26, FL-22)
    FAILED             // lỗi mạng, timeout, 5xx → giữ EXPIRING, lease lấy lại sau
  }
}

// io.ticket.reservation.service
@Service public class HoldService {               // không @Transactional
  public HoldOutcome hold(UUID userId, UUID eventId, IdempotencyRequest idem, CreateReservationRequest req); // DOC-25 §2.2
}
@Service public class HoldTxService {
  @Transactional public HoldOutcome holdInTransaction(HoldCommand cmd);      // §4.1 T1…T8
}
@Service public class ReleaseService {            // không @Transactional
  public CancelResult cancelByBuyer(UUID userId, UUID reservationId, IdempotencyRequest idem); // E-16
  public ReleaseResult release(UUID reservationId);                          // §4.4, dùng chung
  public int runOnce();                                                      // một vòng job, §4.5
}
@Service public class ReleaseTxService {
  @Transactional public BeginCancel beginCancelByBuyer(UUID userId, UUID reservationId, IdempotencyCommand idem);
  @Transactional public List<UUID> claimDue(int limit);
  @Transactional public List<UUID> reclaimStale(int limit, Duration lease);
  @Transactional public int makeReclaimable(List<UUID> ids, Duration lease);
  @Transactional public FinishResult finish(UUID reservationId);             // §4.4 bước R3
}
```

`HoldCommand` mang `userId`, `eventId`, `reservationId` và `orderId` (UUIDv7 sinh ở Java trước transaction, DR-11), `idemKey`, `requestHash`, danh sách item đã kiểm tra hình thức.

P2 chưa có PaymentIntent: `orders.payment_intent_id` luôn `NULL`, nên `ReleaseService` không gọi SPI. Module `payment` vẫn đăng ký một bean `PaymentIntentCanceller` từ P2 (bản tạm trả `FAILED` và log ERROR nếu bị gọi) để constructor injection không đổi khi P3 thay bằng bản thật.

## 4. Thuật toán

### 4.1 Giữ vé (E-14, DR-41)

**Trước transaction** (`HoldService`, không connection):

1. **P1 · Hình thức.** `@Valid` ở controller và `HoldRequestValidator`: 1–20 dòng `items`; `seatIds` ≥ 1 mỗi dòng, không trùng trong toàn request; `quantity` ≥ 1; cùng `zoneId` hoặc `ticketTypeId` không xuất hiện hai lần; Σ (số ghế + `quantity`) ≤ `reservation.max-units-per-hold` (50). Sai → 422 `VALIDATION_FAILED` với `rule` ∈ `required`, `min`, `too_many_items`, `duplicate_item`, `too_many_units` (`params.max = 50`). Thiếu `Idempotency-Key` → 400 `IDEMPOTENCY_KEY_REQUIRED` (DOC-25).
2. **P2 · Rate limit và lượt vào** (từ P6, DOC-28): token bucket `rl:hold:{userId}`, `ZSCORE admitted:{e}`. P2 bỏ qua bước này.
3. **P3 · Bulkhead.** `Semaphore.tryAcquire()` với `hold.bulkhead.permits` = 24 permit dùng chung cho E-14 và E-16, chờ `hold.bulkhead.wait` = 0. Hết permit → 503 `OVERLOADED`, `Retry-After` ngẫu nhiên trong [`hold.overloaded-retry-min`, `hold.overloaded-retry-max`] = [1, 3] giây (DR-61). Permit được trả ở `finally`.
4. **P4 · Gợi ý Redis.** Với mỗi dòng ZONE/GA, `AdmissionApi.isSoldOut(poolId)`; một pool có cờ → 409 `INSUFFICIENT_CAPACITY` với `available = 0`, không lấy connection (DR-46). Pool lấy từ `zoneId`/`ticketTypeId` cần tra trong DB nên P4 chỉ áp cho dòng GA, nơi `poolId` có trong cache `poolIdByTicketType` (Caffeine, 60 giây, theo `eventId`); dòng ZONE dùng cờ từ P5. Redis lỗi hoặc quá `spring.data.redis.timeout` → bỏ qua bước này. Ghế: kiểm tra bitmap snapshot từ P5 (DR-46, DOC-29).

**Transaction** (`HoldTxService.holdInTransaction`, `READ COMMITTED`). Thứ tự câu là thứ tự của DR-41 và DOC-14 §8:

| Bước | Câu (rút gọn) | Kết quả và lỗi |
| --- | --- | --- |
| T0 | `SELECT set_config('statement_timeout', '2000', true), set_config('lock_timeout', '1000', true)` | Giá trị từ `hold.statement-timeout`, `hold.lock-timeout`. `SET LOCAL` không nhận tham số bind nên dùng `set_config(…, true)` |
| T1 | `INSERT INTO idempotency_key … ON CONFLICT (user_id, idem_key) DO NOTHING` (DOC-15 §5, DOC-25) | 0 dòng → đọc dòng: hash khác → 422 `IDEMPOTENCY_KEY_REUSED`; cùng hash → trả response đã lưu, `Idempotent-Replayed: true`, **không chạm kho vé** |
| T2 | `EventApi.requireOnSale(eventId)` (DOC-20): `SELECT status, sale_starts_at <= now() AND now() < sale_ends_at AS in_window … FROM event WHERE event_id = :e` (không khóa) | Không `PUBLISHED` hoặc ngoài khung → 409 `EVENT_NOT_ON_SALE` kèm `displayStatus` (và `saleStartsAt` khi `UPCOMING`) |
| T3 | `EventApi.ticketTypes(eventId)` và `InventoryApi.poolsOf(eventId)`; ghép item → pool, giá, tên | `zoneId` không phải pool `ZONE` của sự kiện, `ticketTypeId` không phải loại `GA` → 422 `wrong_model`; id không thuộc sự kiện → 422 `unknown_reference`. Giá đọc **trong** transaction nên reservation tạo sau khi đổi giá commit dùng giá mới (DR-30) |
| T4 | `INSERT INTO reservation … ON CONFLICT … DO NOTHING RETURNING expires_at` (§4.1.1) | 0 dòng → `SELECT reservation_id` của reservation đang mở → 409 `ACTIVE_RESERVATION_EXISTS` kèm `reservationId`; rollback |
| T5 | `InventoryApi.claim`: ghế (một câu), rồi từng pool theo `poolId` tăng dần (§4.2) | Thiếu ghế → 409 `SEATS_UNAVAILABLE`; thiếu pool → 409 `INSUFFICIENT_CAPACITY`; rollback cả T1, T4 |
| T6 | Chèn `reservation_item` theo lô (`JdbcTemplate.batchUpdate`); `OrderApi.createPending(orderId, reservationId, eventId, userId, amount)` | `amount` = Σ `quantity × unitPrice` tính ở Java từ giá ở T3 (DR-20) |
| T7 | `UPDATE idempotency_key SET response_status = 201, response_body = :json …` | Response dựng từ dữ liệu đã có trong bộ nhớ, không có `SELECT` thêm |
| T8 | `COMMIT` | |

**Sau commit** (`HoldService`): ghi metric (§9); nếu kết quả là `INSUFFICIENT_CAPACITY` với `available = 0` thì `AdmissionApi.markSoldOut(poolId)` (Redis, `soldout:pool:{poolId}` TTL 30 giây, lỗi thì bỏ qua; DR-46); từ P6 gia hạn lượt vào (`AdmissionApi.extendPass`).

#### 4.1.1 Câu chèn reservation (T4)

```sql
INSERT INTO reservation (reservation_id, event_id, user_id, status, expires_at)
VALUES (:rid, :event, :user, 'ACTIVE', now() + make_interval(secs => :holdSeconds))   -- 600
ON CONFLICT (user_id, event_id) WHERE status IN ('ACTIVE','EXPIRING') DO NOTHING
RETURNING expires_at;

-- 0 dòng: lấy ID để client chuyển tới /checkout/:id
SELECT reservation_id FROM reservation
 WHERE user_id = :user AND event_id = :event AND status IN ('ACTIVE','EXPIRING');
```

`reservation` được chèn bằng `@Modifying @Query` (cần `ON CONFLICT` và phải đứng trước claim), còn `reservation_item` chèn sau claim bằng `ReservationItemWriter` (`JdbcTemplate.batchUpdate`). Aggregate `Reservation` chỉ dùng để đọc (DR-168). `expires_at` lấy từ `now()` của database (DR-12).

Hai request song song của cùng người với hai key khác nhau: request sau chờ ở `reservation_open_uq` cho tới khi request trước commit hoặc rollback (tối đa `lock_timeout` 1 giây, quá thì 503 `OVERLOADED`). Trước commit → request sau nhận 0 dòng → 409; trước rollback → request sau chèn được.

#### 4.1.2 Dòng `reservation_item` và nhãn

| Loại | Số dòng | `quantity` | `label` (jsonb) | `unit_price` |
| --- | --- | --- | --- | --- |
| SEAT | một dòng mỗi ghế | 1 | `{"section":"Khán đài A","row":"C","seat":"9"}` từ `RETURNING` của câu claim | giá loại vé `SEAT` của ghế |
| ZONE | một dòng mỗi zone | `quantity` | `{"zone":"Fanzone"}` (`inventory_pool.name`) | giá loại vé của pool |
| GA | một dòng mỗi loại vé | `quantity` | `{}` (DR-108) | giá loại vé `GA` |

Ví dụ GA: giữ 2 vé "Vé tiêu chuẩn" giá 750.000 → một dòng `kind = 'GA'`, `quantity = 2`, `unit_price = 750000`, `ticket_type_name = 'Vé tiêu chuẩn'`, `label = '{}'`; `orders.amount = 1500000`.

### 4.2 Câu claim

Câu chuẩn ở DOC-14 §7; `SkipLockedClaimer` thêm `RETURNING` để biết ghế nào được giữ và nhãn của ghế.

**Ghế** (một câu cho mọi ghế của lệnh; P5):

```sql
WITH picked AS (
  SELECT unit_id FROM inventory_unit
   WHERE event_id = :event AND seat_id = ANY(:seatIds) AND status = 'AVAILABLE'
   FOR UPDATE SKIP LOCKED
)
UPDATE inventory_unit u
   SET status = 'HELD', reservation_id = :rid, updated_at = now()
  FROM picked
 WHERE u.unit_id = picked.unit_id
RETURNING u.seat_id, u.ticket_type_id, u.section_name, u.row_label, u.seat_number;
```

Số dòng < số ghế: `InventoryService` tìm `missing = seatIds − returned`, chạy `SELECT seat_id FROM inventory_unit WHERE event_id = :event AND seat_id = ANY(:missing)`: id không có dòng nào → 422 `unknown_reference`; còn lại (đang bị khóa, `HELD`, `SOLD`, `REMOVED`) → 409 `SEATS_UNAVAILABLE` với `unavailableSeatIds = missing`. Câu thứ hai chỉ chạy trên đường thua.

**Pool** (zone, GA; một câu cho mỗi dòng `quantity`, pool theo `poolId` tăng dần):

```sql
WITH picked AS (
  SELECT unit_id FROM inventory_unit
   WHERE pool_id = :pool AND status = 'AVAILABLE'
   LIMIT :qty
   FOR UPDATE SKIP LOCKED
)
UPDATE inventory_unit u
   SET status = 'HELD', reservation_id = :rid, updated_at = now()
  FROM picked
 WHERE u.unit_id = picked.unit_id;
```

Số dòng < `:qty` → `available = SELECT count(*) FROM inventory_unit WHERE pool_id = :pool AND status = 'AVAILABLE'` (trong cùng transaction, trước rollback; là ước lượng vì unit đang bị khóa vẫn đếm là `AVAILABLE`) → 409 `INSUFFICIENT_CAPACITY` với `poolId`, `requested = :qty`, `available` (DR-172). Dừng ở pool thiếu đầu tiên, không thử pool sau.

Vì sao không deadlock: câu chọn dùng `SKIP LOCKED` nên không bao giờ chờ lock unit; câu chèn reservation (T4) chỉ chờ trên khóa của chính khóa unique. `READ COMMITTED` kiểm lại `status = 'AVAILABLE'` trên phiên bản mới nhất của dòng sau khi khóa (SDD gốc 8.2).

### 4.3 Mã lỗi và payload

Thành viên mở rộng theo DOC-35 §4; mọi 409 dưới đây rollback toàn bộ, không có tác dụng phụ.

| HTTP · `code` | Khi nào | Payload mẫu (phần mở rộng) |
| --- | --- | --- |
| 409 `SEATS_UNAVAILABLE` | T5: thiếu ghế; P4 (P5): bitmap báo mọi ghế đã `HELD`/`SOLD` | `{ "unavailableSeatIds": ["0199b1c2-9a02-7a10-9c2e-6d1f0a4b8c12"] }` |
| 409 `INSUFFICIENT_CAPACITY` | T5: pool thiếu; P4: có cờ hết vé | `{ "poolId": "0199b1c2-5002-7aaa-8bbb-111122223334", "requested": 4, "available": 3 }` |
| 409 `ACTIVE_RESERVATION_EXISTS` | T4 | `{ "reservationId": "0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091" }` |
| 409 `EVENT_NOT_ON_SALE` | T2 | `{ "displayStatus": "UPCOMING", "saleStartsAt": "2026-11-10T03:00:00Z" }` hoặc `{ "displayStatus": "PAUSED" }` |
| 422 `VALIDATION_FAILED` | P1, T3 | `{ "errors": [{ "field": "items", "rule": "too_many_units", "params": { "max": 50 } }] }` |
| 503 `OVERLOADED` | P3 hết permit; Hikari hết connection sau `connection-timeout` 1 giây; SQLState `57014` (statement timeout) hoặc `55P03` (lock timeout) | `{ "retryAfterSeconds": 2 }` + `Retry-After: 2` |

Ví dụ đầy đủ:

```http
HTTP/1.1 409 Conflict
Content-Type: application/problem+json

{
  "type": "https://errors.ticket.dev/INSUFFICIENT_CAPACITY",
  "title": "Không đủ vé",
  "status": 409,
  "detail": "Loại vé này chỉ còn 3 vé.",
  "code": "INSUFFICIENT_CAPACITY",
  "requestId": "4b7a0c1d2e3f40516273849506a7b8c9",
  "poolId": "0199b1c2-5002-7aaa-8bbb-111122223334",
  "requested": 4,
  "available": 3
}
```

### 4.4 Thủ tục trả vé (dùng chung cho job và đường nhanh, DR-43)

Đầu vào: một reservation đã ở `EXPIRING` (do job, do người mua hủy, hoặc do hủy sự kiện, DR-28). `ReleaseService.release(reservationId)`:

1. **R1 · Đọc PaymentIntent.** `OrderApi.paymentIntentIdOf(reservationId)` (câu đọc độc lập, sau commit của bước đặt `EXPIRING`; DR-47 bảo đảm ID đã lưu nếu có).
2. **R2 · Hủy PaymentIntent** (chỉ khi có ID; ngoài transaction): `canceller.cancel(orderId, piId, timeout)` với `timeout` = `payment.stripe.cancel-timeout` (5 giây) ở job, `reservation.fast-cancel.timeout` (3 giây) ở đường nhanh.
   - `CANCELED` → sang R3.
   - `SUCCEEDED` → dừng, trả `PAYMENT_SUCCEEDED`; reservation ở lại `EXPIRING`, unit vẫn `HELD`; `payment` chạy xác nhận (`EXPIRING → CONFIRMED`, DOC-26).
   - `FAILED` → dừng, trả `PENDING`; tăng `ticket_payment_cancel_failures_total`; lease lấy lại sau (§4.5).
3. **R3 · Transaction trả vé** (`ReleaseTxService.finish`), thứ tự cố định: reservation → unit → đơn (DR-169):

   ```sql
   -- (a) Trọng tài: chỉ một bên rời EXPIRING (ADR-0004)
   UPDATE reservation
      SET status = CASE close_reason WHEN 'TIMEOUT' THEN 'EXPIRED' ELSE 'CANCELLED' END,
          closed_at = now()
    WHERE reservation_id = :rid AND status = 'EXPIRING'
   RETURNING status, close_reason, extract(epoch FROM now() - expires_at) AS lag_seconds;
   -- 0 dòng → reservation đã CONFIRMED (webhook) hoặc đã được bên khác trả: dừng, không đụng unit

   -- (b) Trả unit (InventoryApi.release)
   UPDATE inventory_unit
      SET status = 'AVAILABLE', reservation_id = NULL, updated_at = now()
    WHERE reservation_id = :rid AND status = 'HELD'
   RETURNING pool_id;

   -- (c) Đóng đơn chưa thu tiền (OrderApi.close; DR-91)
   UPDATE orders
      SET status = :orderFinal, updated_at = now()      -- 'EXPIRED' khi TIMEOUT, ngược lại 'CANCELLED'
    WHERE reservation_id = :rid AND status = 'PENDING_PAYMENT';
   ```

   Số dòng (b) bằng Σ `quantity` của item (B8); khác thì log ERROR `reservation.release.unit_mismatch` nhưng vẫn commit (không có gì để sửa trong transaction; DOC-30 sẽ báo).
4. **R4 · Sau commit:** `AdmissionApi.clearSoldOut(poolIds)` cho các `pool_id` khác NULL ở (b) (Redis, lỗi thì bỏ qua; cờ tự hết sau 30 giây); metric `ticket_expiry_released_units_total`, `ticket_expiry_lag_seconds` (chỉ `TIMEOUT`).

Thứ tự reservation → unit trùng thứ tự của transaction xác nhận (DOC-14 §7: event `FOR SHARE` → reservation → unit → đơn), nên trả vé và xác nhận chạy song song không khóa chéo nhau: bên đến sau chờ khóa dòng reservation, rồi thấy 0 dòng và dừng.

### 4.5 Job trả vé (`ReleaseExpiredHoldsJob`, DR-42)

`@Scheduled(fixedDelayString = "${reservation.expiry.interval}")` (5 giây) gọi `ReleaseService.runOnce()`:

1. `due = ReleaseTxService.claimDue(batch)` với `batch` = `reservation.expiry.batch-size` (200):

   ```sql
   UPDATE reservation
      SET status = 'EXPIRING', close_reason = 'TIMEOUT', expiring_since = now()
    WHERE reservation_id IN (
          SELECT reservation_id FROM reservation
           WHERE status = 'ACTIVE' AND expires_at < now()
           ORDER BY expires_at
           LIMIT :batch
           FOR UPDATE SKIP LOCKED)
      AND status = 'ACTIVE'
   RETURNING reservation_id;
   ```

2. Nếu `due.size() < batch`: `stale = reclaimStale(batch − due.size(), lease)` với `lease` = `reservation.expiry.lease` (30 giây):

   ```sql
   UPDATE reservation SET expiring_since = now()
    WHERE reservation_id IN (
          SELECT reservation_id FROM reservation
           WHERE status = 'EXPIRING'
             AND expiring_since < now() - make_interval(secs => :leaseSeconds)
           ORDER BY expiring_since
           LIMIT :n
           FOR UPDATE SKIP LOCKED)
      AND status = 'EXPIRING'
   RETURNING reservation_id;
   ```

   Câu này cũng nhặt reservation do hủy sự kiện đặt `EXPIRING` (DR-28 bước 5; `expiring_since = now()` lúc hủy), nên chúng được trả 30–35 giây sau khi hủy.
3. Với từng id trong `due ∪ stale`, tuần tự: `release(id)` (§4.4). Hết ngân sách `reservation.expiry.batch-budget` (20 giây, tính từ đầu bước 3) thì dừng và gọi `makeReclaimable(remaining, lease)`: `UPDATE reservation SET expiring_since = now() - make_interval(secs => :leaseSeconds) WHERE reservation_id = ANY(:ids) AND status = 'EXPIRING'`, để vòng sau (5 giây) nhặt ngay thay vì đợi lease (DR-170).
4. Metric `ticket_expiry_batch_size` (khi `due ∪ stale` không rỗng), `ticket_job_runs_total{job="release_expired"}`, `ticket_job_duration_seconds`.

Nhiều bản `api` chạy cùng lúc không giẫm nhau: bước 1–2 dùng `SKIP LOCKED` và chuyển trạng thái trong cùng câu; bước 3 có trọng tài ở R3(a). Kiểm tra lúc khởi động: `reservation.expiry.batch-budget + payment.stripe.cancel-timeout < reservation.expiry.lease` (20 + 5 < 30), để không reservation nào bị bản khác lấy lại khi đang xử lý.

Ngân sách thời gian (planned, P2 không gọi Stripe): một `finish` GA ≈ 3 câu theo khóa chính và index một phần, ~2–5 ms; 200 reservation ≈ 1 giây. NFR-03 (≤ 30 giây sau `expires_at`): độ trễ tối đa ≈ nhịp 5 giây + thời gian một vòng; 5.000 reservation hết hạn cùng lúc = 25 vòng × (5 giây + ~1 giây) ≈ 150 giây cho reservation cuối, **vượt** NFR-03 với một bản `api`. EXP-04 đo con số thật; nếu vượt, giảm `reservation.expiry.interval` khi vòng trước lấy đủ `batch` (chạy lại ngay, không chờ nhịp) — xem câu hỏi mở Q1.

### 4.6 Hủy giữ vé đường nhanh (E-16, DR-43)

`ReleaseService.cancelByBuyer(userId, reservationId, idem)`, sau bulkhead P3 (§4.1):

1. **C1 · Transaction ngắn** `beginCancelByBuyer`: T0 (timeout), T1 idempotency với `operation = 'CANCEL_HOLD'` (DOC-25), rồi

   ```sql
   UPDATE reservation
      SET status = 'EXPIRING', close_reason = 'BUYER_CANCELLED', expiring_since = now()
    WHERE reservation_id = :rid AND user_id = :uid AND status = 'ACTIVE';
   ```

   - 1 dòng → ghi response `202 {"reservationId": …, "status": "EXPIRING"}` vào dòng idempotency; commit.
   - 0 dòng → đọc `status` của reservation theo `reservation_id` **và** `user_id`: không có → 404 `NOT_FOUND`; `CONFIRMED` → 409 `PAYMENT_ALREADY_SUCCEEDED` kèm `orderId` (rollback); `EXPIRING`, `EXPIRED`, `CANCELLED` → ghi response `200 {"status": <hiện tại>}`; commit; trả luôn.
2. **C2 · `release(rid)`** (§4.4) với timeout hủy 3 giây.
3. **C3 · Kết quả:** R3 commit → 200 `CANCELLED`; `PENDING` (Stripe lỗi) → 202 `EXPIRING`, job hoàn tất sau; `PAYMENT_SUCCEEDED` → 409 `PAYMENT_ALREADY_SUCCEEDED`. R3 **không** chạm dòng idempotency.

Dòng idempotency của E-16 chỉ nằm ở C1. Khi phát lại cùng key, response của `CANCEL_HOLD` **tính từ trạng thái hiện tại** của reservation, không lấy `response_body` đã lưu (bảng ở DOC-25 §3.5, DR-175): đã đóng → 200 với trạng thái đó, còn `EXPIRING` → 202, `CONFIRMED` → 409 `PAYMENT_ALREADY_SUCCEEDED`; luôn kèm `Idempotent-Replayed: true`. Ví dụ tiến trình chết sau C1, trước R3: lần gửi lại đầu nhận 202 `EXPIRING`; sau khi job trả vé (tối đa lease 30 giây) lần gửi lại tiếp nhận 200 `CANCELLED`.

P2 không có PaymentIntent nên C2 luôn đi thẳng tới R3: E-16 trả 200 `CANCELLED`, unit về `AVAILABLE` trước khi response về (FR-07.4).

### 4.7 Đổi sức chứa pool GA (DR-30)

`InventoryApi.changeGaCapacity(eventId, ticketTypeId, newCapacity)` chạy trong transaction của `studio`/`event` (E-35, DOC-20):

```sql
-- 1. Tuần tự hóa các lần đổi sức chứa cùng pool. Câu claim không đụng dòng pool nên không bị chặn.
SELECT pool_id, capacity FROM inventory_pool
 WHERE event_id = :event AND ticket_type_id = :tt AND kind = 'GA' AND removed_at IS NULL
 FOR UPDATE;

-- 2a. Tăng Δ = new − old
INSERT INTO inventory_unit (event_id, ticket_type_id, pool_id)
SELECT :event, :tt, :pool FROM generate_series(1, :delta);
UPDATE inventory_pool SET capacity = capacity + :delta WHERE pool_id = :pool;

-- 2b. Giảm Δ = old − new
WITH victims AS (
  SELECT unit_id FROM inventory_unit
   WHERE pool_id = :pool AND status = 'AVAILABLE'
   LIMIT :delta
   FOR UPDATE SKIP LOCKED
)
UPDATE inventory_unit u SET status = 'REMOVED', updated_at = now()
  FROM victims WHERE u.unit_id = victims.unit_id;          -- phải đúng :delta dòng
UPDATE inventory_pool SET capacity = capacity - :delta WHERE pool_id = :pool;
```

Giảm thiếu dòng → đọc `used = SELECT count(*) FROM inventory_unit WHERE pool_id = :pool AND status IN ('HELD','SOLD')` → ném `CapacityBelowUsedException(used)` → 409 `CAPACITY_BELOW_USED` kèm `used`, rollback. Vì `SKIP LOCKED`, lệnh giảm có thể thiếu dòng oan khi một lệnh giữ vé đang khóa unit cuối; người tổ chức thử lại. Ví dụ (FR-02.12): pool 100, 40 unit `HELD`+`SOLD`: giảm về 30 → 409 `used = 40`; về 40 → 60 unit `REMOVED`, `capacity = 40`; tăng lên 150 → thêm 110 unit, `capacity = 150`.

Sau commit (bên gọi): tăng sức chứa → `AdmissionApi.clearSoldOut(poolId)`. Sức chứa zone đổi theo sơ đồ (DR-37, DOC-23), ngoài phạm vi P2.

Xóa loại vé đã xuất bản (`removeTicketType`, DR-26): `SELECT count(*) … status IN ('HELD','SOLD')` > 0 → 409 `TICKET_TYPE_IN_USE`; không thì `UPDATE inventory_unit SET status = 'REMOVED' WHERE ticket_type_id = :tt AND status = 'AVAILABLE'` và `UPDATE inventory_pool SET removed_at = now(), capacity = 0 WHERE ticket_type_id = :tt` (B3 vẫn đúng: 0 unit chưa `REMOVED`). Lệnh giữ vé chen vào giữa câu đếm và câu `UPDATE` không bị mất: câu đếm chạy sau khi đã khóa dòng pool, và lệnh giữ vé mới thấy pool `removed_at` ở T3 sau commit; lệnh đã claim trước commit làm câu `UPDATE … status = 'AVAILABLE'` bỏ qua unit của nó → `used` > 0 ở lần đếm lại cuối transaction (`InventoryService` đếm lại sau `UPDATE`, khác 0 → ném `TicketTypeInUse`, rollback).

### 4.8 Tạo pool GA khi xuất bản (DR-27 bước 4)

Transaction xuất bản thuộc `studio.service.PublishService` (DOC-20); `inventory` cung cấp câu chèn, nhận dữ liệu loại vé qua tham số (DR-79):

```sql
WITH spec AS (
  SELECT * FROM unnest(:ticketTypeIds::uuid[], :names::text[], :capacities::int[])
           AS s(ticket_type_id, name, capacity)
), pools AS (
  INSERT INTO inventory_pool (event_id, kind, ticket_type_id, name, capacity)
  SELECT :event, 'GA', ticket_type_id, name, capacity FROM spec
  RETURNING pool_id, ticket_type_id, capacity
)
INSERT INTO inventory_unit (event_id, ticket_type_id, pool_id)
SELECT :event, p.ticket_type_id, p.pool_id
  FROM pools p CROSS JOIN LATERAL generate_series(1, p.capacity);
```

Số dòng trả về phải bằng Σ `capacity`; khác → `IllegalStateException`, rollback. Profile `experiment` chèn thêm `inventory_pool_counter` (DOC-15 §8.2). Thời gian kế hoạch: 5.000 unit < 1 giây; 80.000 unit pool + 20.000 ghế < 5 giây (S-03 đo, §6.3).

### 4.9 Đếm còn lại và số liệu (DR-46, DR-71, DR-155)

```sql
-- Còn lại của một pool (cờ hết vé, INSUFFICIENT_CAPACITY): unit_pool_available_idx
SELECT count(*) FROM inventory_unit WHERE pool_id = :pool AND status = 'AVAILABLE';

-- Số liệu theo loại vé của một sự kiện (E-26 used, E-37, SOLD_OUT của DOC-20): unit_event_idx
SELECT ticket_type_id, status, count(*) FROM inventory_unit
 WHERE event_id = :event AND status <> 'REMOVED'
 GROUP BY ticket_type_id, status;

-- Studio 01, tối đa 20 sự kiện mỗi trang (E-24): unit_event_idx
SELECT event_id, status, count(*) FROM inventory_unit
 WHERE event_id = ANY(:eventIds) AND status <> 'REMOVED'
 GROUP BY event_id, status;
```

**DR-155 · index `unit_event_idx`.** DOC-14 không có index nào bắt đầu bằng `event_id` phủ unit pool (`unit_seat_uq` chỉ phủ ghế), nên ba câu sau quét cả bảng. Thêm:

```sql
CREATE INDEX unit_event_idx ON inventory_unit (event_id, ticket_type_id, status);
```

Cả ba cột câu cần đều nằm trong index nên PostgreSQL chạy index-only scan khi visibility map đủ mới; sự kiện 100.000 unit đếm < 150 ms (kế hoạch, E-37). Chi phí ở §6.2. Cache: số liệu `sales` nằm ở Redis `sales.cache-ttl` 5 giây (DR-71), không phải trách nhiệm của câu này.

## 5. Ba bản cài đặt `InventoryClaimer` (DR-76)

| Bản | `inventory.strategy` | Claim pool | Xác nhận / trả | Đúng? | Dùng ở |
| --- | --- | --- | --- | --- | --- |
| `SkipLockedClaimer` | `skip-locked` (mặc định) | Câu §4.2 | `WHERE reservation_id = :rid AND status = 'HELD'` | Có | Mọi môi trường |
| `CounterClaimer` | `counter` (profile `experiment`) | `UPDATE inventory_pool_counter SET available = available - :qty, held = held + :qty, updated_at = now() WHERE pool_id = :p AND available >= :qty` (1 dòng hoặc 0) | Cộng trừ ngược lại theo `poolItems`: trả `available + q, held − q`; xác nhận `held − q, sold + q` | Có (CHECK `available >= 0`), nhưng hot row | EXP-10 (phương án A, SDD gốc 10.3) |
| `NaiveClaimer` | `naive` (profile `experiment`) | `SELECT unit_id … WHERE pool_id = :p AND status = 'AVAILABLE' LIMIT :qty` (không khóa) rồi `UPDATE inventory_unit SET status = 'HELD', reservation_id = :rid WHERE unit_id = ANY(:ids)` (không điều kiện trạng thái); luôn báo đủ `:qty` | Như `SkipLockedClaimer` | **Không**: hai transaction chọn cùng unit, bên sau ghi đè `reservation_id` của bên trước | EXP-02, EXP-10 làm bằng chứng bán vượt |

Ghi chú:

- `CounterClaimer` chỉ thay claim pool; claim ghế vẫn dùng câu §4.2. Ở chế độ này unit pool không đổi trạng thái, nên `InventoryApi.release/confirmSold` dựa vào `poolItems` (§3.2). `InvariantChecker` profile `experiment` đối chiếu `available + held + sold = capacity` (DOC-30).
- Bán vượt của `NaiveClaimer` không làm số unit `HELD` vượt sức chứa (unit bị ghi đè chứ không nhân đôi); nó lộ ra ở B8: Σ `quantity` của reservation `ACTIVE`/`CONFIRMED` > số unit `HELD`+`SOLD`, tức số vé đã hứa > `capacity`. EXP-02 đo "số vé bán vượt = Σ `quantity` của reservation chưa đóng và `CONFIRMED` − `capacity`" (DOC-70, DOC-72).
- Ba bản cùng chạy qua `InventoryService`, nên transaction giữ vé, metric và mã lỗi không đổi theo chiến lược; chỉ câu claim khác.

## 6. Index một phần, HOT update và autovacuum

### 6.1 Vì sao claim không phải HOT update

PostgreSQL chỉ dùng HOT (heap-only tuple) khi không cột nào được index thay đổi, kể cả cột nằm trong **điều kiện** của index một phần. `status` nằm trong điều kiện của `unit_pool_available_idx` và `unit_seat_taken_idx`, `reservation_id` là khóa và điều kiện của `unit_reservation_idx`. Vậy mọi lần claim, trả, xác nhận đều là update thường: phiên bản mới của dòng được chèn vào **mọi** index mà nó thỏa điều kiện.

Số mục index chèn mỗi lần đổi một unit pool (đã tính DR-155):

| Chuyển | PK | `unit_ticket_type_idx` | `unit_event_idx` | `unit_reservation_idx` | `unit_pool_available_idx` | Tổng |
| --- | --- | --- | --- | --- | --- | --- |
| `AVAILABLE → HELD` (claim) | 1 | 1 | 1 | 1 | — | 4 |
| `HELD → AVAILABLE` (trả) | 1 | 1 | 1 | — | 1 | 4 |
| `HELD → SOLD` (xác nhận) | 1 | 1 | 1 | 1 | — | 4 |

Unit ghế thêm `unit_seat_uq` (luôn) và `unit_seat_taken_idx` (khi `HELD`/`SOLD`). Không có `unit_event_idx` thì mỗi chuyển là 3 mục; DR-155 thêm khoảng 1/3 chi phí index cho bảng nóng. Chấp nhận vì claim vốn đã không HOT, mục thêm vào là B-tree ba cột nhỏ, và các câu đếm của studio không còn quét cả bảng (§4.9).

`fillfactor = 80` (DOC-14 §4.4) để phiên bản mới thường nằm cùng trang heap: không cứu được HOT nhưng giảm số trang bị chạm và giữ index trỏ vào trang "nóng" đã có trong bộ đệm.

### 6.2 Dead tuple và autovacuum

- Mỗi vòng giữ → trả để lại một dead tuple trong heap và một mục chết trong mỗi index ở bảng trên. Pool 5.000 vé dưới EXP-02 (~2.000 lệnh giữ/giây, 1–4 vé) sinh ~5.000 dead tuple mỗi giây.
- `autovacuum_vacuum_scale_factor = 0.02` cho riêng bảng (DOC-14 §4.4): bảng 100.000 dòng vacuum khi có ~2.050 dead tuple (ngưỡng 50 + 2%), thay vì 20.050 với mặc định 0,2.
- Câu claim pool quét `unit_pool_available_idx` từ đầu cho mỗi lệnh. Mục chết và mục đang bị khóa nằm đầu index làm mỗi lệnh duyệt qua nhiều mục hơn; PostgreSQL đánh dấu `LP_DEAD` mục chết sau lần quét đầu và dọn chúng bằng bottom-up deletion (B-tree, từ PostgreSQL 14) khi trang đầy, nên chi phí không tăng mãi, nhưng p95 claim dưới tải liên tục có thể cao hơn lúc bảng mới.
- `autovacuum_naptime` mặc định 60 giây có thể là nút thắt khi churn cao; tham số PostgreSQL của máy thực nghiệm thuộc DOC-62, quyết định sau S-03.

### 6.3 S-03 phải đo gì (chưa chạy, DR-152)

S-03 chạy trước P2-00 trên PostgreSQL 18 trong compose; mọi con số ở tài liệu này tới lúc đó là **kế hoạch**.

| # | Đo | Cách | Ngưỡng / quyết định |
| --- | --- | --- | --- |
| S3-a | Thời gian câu §4.8 (80.000 unit pool) và câu dựng unit ghế (20.000) với đủ index, có và không `unit_event_idx` | `EXPLAIN (ANALYZE, BUFFERS)`, 3 lần, trung vị | < 5 giây tổng (DR-27); vượt → `COPY` qua `PgConnection.getCopyAPI()` |
| S3-b | Tỉ lệ HOT | `n_tup_hot_upd / n_tup_upd` từ `pg_stat_user_tables` sau 100.000 lượt claim/trả trên pool 5.000 | Kỳ vọng ≈ 0; xác nhận §6.1 |
| S3-c | p95 câu claim pool khi bảng mới và sau 10 phút churn (2.000 lượt/giây) | `pg_stat_statements` (`mean_exec_time`, `stddev`), k6 nhỏ | p95 sau churn > 2× lúc mới → đề xuất `autovacuum_naptime = 10s` và `autovacuum_vacuum_cost_delay = 0` cho bảng (DR mới) |
| S3-d | Phình index | `pgstatindex('unit_pool_available_idx')` (`avg_leaf_density`, `leaf_fragmentation`) trước và sau S3-c | Ghi số; không ngưỡng |
| S3-e | Số lần autovacuum | `autovacuum_count`, `last_autovacuum` của `inventory_unit` trong S3-c | Ghi số |
| S3-f | Câu đếm §4.9 trên sự kiện 100.000 unit | `EXPLAIN ANALYZE` | < 150 ms; kiểm tra là index-only scan |

Kết quả ghi vào DR-27 và mục này kèm ngày, máy và SHA.

## 7. Transaction và đồng thời

### 7.1 Cái gì commit cùng nhau

| Transaction | Điều phối | Gồm | Timeout |
| --- | --- | --- | --- |
| Giữ vé | `HoldTxService` | idempotency, reservation, unit `HELD`, item, đơn `PENDING_PAYMENT`, response idempotency | `statement_timeout` 2 giây, `lock_timeout` 1 giây |
| Đặt `EXPIRING` (job) | `ReleaseTxService.claimDue/reclaimStale` | tối đa 200 reservation | mặc định |
| Đặt `EXPIRING` (đường nhanh) | `ReleaseTxService.beginCancelByBuyer` | idempotency, 1 reservation | 2 giây / 1 giây |
| Trả vé | `ReleaseTxService.finish` | reservation đóng, unit `AVAILABLE`, đơn đóng, (E-16) response idempotency | 2 giây / 1 giây |
| Đổi sức chứa, xóa loại vé | của bên gọi (DOC-20) | dòng pool, unit thêm/`REMOVED`, `capacity` | của bên gọi |

Không transaction nào ở đây gọi Redis, Stripe hay SMTP (SDD gốc 10.4); mọi lời gọi ra ngoài ở `HoldService`/`ReleaseService` trước hoặc sau.

### 7.2 Tranh chấp

| Tranh chấp | Trọng tài | Kết quả |
| --- | --- | --- |
| N lệnh giữ cùng ghế | `FOR UPDATE SKIP LOCKED` trên unit | Đúng 1 thắng, còn lại 0 dòng → 409 ngay, không chờ (INVT-01) |
| N lệnh giữ cùng pool | `SKIP LOCKED` + `LIMIT` | Mỗi lệnh khóa dòng khác nhau; gần hết vé có thể từ chối oan (SDD gốc 8.2) (INVT-02) |
| Cùng người, hai key, cùng sự kiện | `reservation_open_uq` | Một reservation; bên kia 409 `ACTIVE_RESERVATION_EXISTS` (INVT-06) |
| Cùng key gửi lại song song | PK `idempotency_key` (DOC-25) | Một tác dụng; các bản sau phát lại response |
| Xác nhận đua với trả vé | Dòng `reservation` (ADR-0004) | `ACTIVE/EXPIRING → CONFIRMED` hoặc `EXPIRING → EXPIRED/CANCELLED`, không cả hai (INVT-18) |
| Job ở hai bản `api` | `SKIP LOCKED` ở `claimDue`/`reclaimStale`; R3(a) | Mỗi reservation trả đúng một lần (INVT-25) |
| Đường nhanh đua với job | `WHERE status = 'ACTIVE'` ở C1 và ở `claimDue` | Một bên đặt `EXPIRING`; bên kia 0 dòng |
| Giảm sức chứa đua với giữ vé | `SKIP LOCKED` ở cả hai | Không bán vượt; lệnh giảm có thể 409 oan (INVT-23) |
| Hủy sự kiện đua với giữ vé | T2 đọc event không khóa | Lệnh giữ commit ngay trước lệnh hủy vẫn được tạo; lệnh hủy (DR-28 bước 5) chuyển nó sang `EXPIRING` |

### 7.3 Nếu tiến trình chết

| Chết ở | Người dùng thấy | Hệ thống tự hồi phục |
| --- | --- | --- |
| Trong transaction giữ vé | Lỗi mạng | Rollback; gửi lại cùng key chạy như lần đầu |
| Sau commit giữ vé, trước response | Lỗi mạng; gửi lại cùng key → 201 y hệt, `Idempotent-Replayed: true` (IDEM-04) | — |
| Sau `claimDue` commit, trước R3 | Không ai (reservation đã quá hạn) | Lease 30 giây → `reclaimStale`; unit bị giữ thêm ≤ 35 giây |
| Sau C1 (đường nhanh) commit, trước R3 | Lỗi mạng; `GET /reservations/{id}` trả `EXPIRING`; gửi lại cùng key → 202 | Job lấy lại sau lease, đơn `CANCELLED` |
| Sau R3 commit, trước xóa cờ hết vé | Lệnh giữ vé pool đó nhận 409 oan tối đa 30 giây | TTL cờ 30 giây (DR-46) |

## 8. Cấu hình

Khóa có sẵn ở DOC-34 §3.4 (không đổi): `reservation.hold-duration` (PT10M), `reservation.max-units-per-hold` (50), `reservation.expiry.interval` (PT5S), `reservation.expiry.batch-size` (200), `reservation.expiry.lease` (PT30S), `hold.bulkhead.permits` (24), `hold.bulkhead.wait` (PT0S), `hold.statement-timeout` (PT2S), `hold.lock-timeout` (PT1S), `hold.overloaded-retry-min`/`-max` (1, 3), `inventory.strategy` (`skip-locked`); và `payment.stripe.cancel-timeout` (PT5S, DOC-34 §3.5).

Khóa mới (thêm vào DOC-34 §3.4):

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `reservation.expiry.batch-budget` | duration | `PT20S` | — | mọi | Ngân sách thời gian xử lý một vòng job trả vé; phần còn lại được trả về để vòng sau nhặt ngay (DR-170) |
| `reservation.fast-cancel.timeout` | duration | `PT3S` | — | mọi | Timeout hủy PaymentIntent trong `DELETE /reservations/{id}` (DR-43) |

Điều kiện khởi động thêm vào DOC-34 §6: `reservation.expiry.batch-budget + payment.stripe.cancel-timeout < reservation.expiry.lease`; `hold.bulkhead.permits < spring.datasource.hikari.maximum-pool-size` (đã có).

## 9. Metrics và log

Metric (định nghĩa ở DOC-33 §4.2, không thêm metric mới):

| Metric | Nơi tăng ở tài liệu này |
| --- | --- |
| `ticket_hold_duration_seconds{model}` | `HoldService`, bao từ P1 tới khi có kết quả, kể cả lỗi. Request trộn nhiều loại: `model = SEAT` nếu có ghế, ngược lại `ZONE` nếu có zone, ngược lại `GA` (DR-171) |
| `ticket_hold_result_total{result}` | `HoldService` `finally`, đúng một lần mỗi request: `success`, `idempotent_replay`, `seats_unavailable`, `insufficient_capacity` (cả khi do cờ hết vé), `overloaded`, `not_on_sale`, `other` (422, `ACTIVE_RESERVATION_EXISTS`) |
| `ticket_hold_bulkhead_in_use` | `HoldService` (permit đang giữ) |
| `ticket_connection_hold_seconds{op}` | `op = hold` (giữ), `cancel` (C1), `release` (R3, `claimDue`, `reclaimStale`) |
| `ticket_expiry_batch_size`, `ticket_expiry_lag_seconds`, `ticket_expiry_released_units_total` | `ReleaseService` (§4.4 R4, §4.5) |
| `ticket_payment_cancel_failures_total` | `ReleaseService` khi `CancelOutcome.FAILED` (từ P3) |
| `ticket_job_runs_total{job="release_expired"}`, `ticket_job_duration_seconds` | `common.job.JobMetrics` |

Log (trường chung theo DOC-33 §2; không email, không body request):

| Sự kiện | Mức | Trường riêng |
| --- | --- | --- |
| `reservation.hold.created` | INFO | `reservation_id`, `event_id`, `units`, `amount` |
| `reservation.hold.rejected` | INFO | `event_id`, `code` |
| `reservation.released` | INFO | `reservation_id`, `close_reason`, `units`, `lag_ms` |
| `reservation.release.cancel_failed` | WARN | `reservation_id`, `order_id`, `outcome` |
| `reservation.release.unit_mismatch` | ERROR | `reservation_id`, `expected`, `actual` |
| `reservation.expiry.reclaimed` | WARN | `count` (lấy lại do lease; khác 0 là dấu hiệu job chết hoặc Stripe chậm) |
| `inventory.capacity.changed` | INFO | `pool_id`, `old`, `new` |

## 10. Lỗi và cách xử lý

| Tình huống | Hành vi |
| --- | --- |
| Hết permit bulkhead | 503 `OVERLOADED`, `Retry-After` 1–3 giây; không lấy connection |
| Hikari hết connection (`connection-timeout` 1 giây) | 503 `OVERLOADED` (`TRANSIENT`, DOC-35 §1) |
| `statement_timeout` (SQLState `57014`) hoặc `lock_timeout` (`55P03`) trong giữ vé | Rollback; 503 `OVERLOADED`; client thử lại cùng key |
| Redis lỗi ở P4 hoặc R4 | Bỏ qua (gợi ý); log WARN `ticket_dependency_failures_total{dependency="redis"}` |
| Mất PostgreSQL giữa job | Vòng job lỗi, `ticket_job_runs_total{result="error"}`; reservation đã `EXPIRING` được lease lấy lại khi DB về |
| `CancelOutcome.FAILED` | Giữ `EXPIRING`; job: lease; đường nhanh: 202 |
| `CancelOutcome.SUCCEEDED` | Không trả vé; job bỏ qua; đường nhanh 409 `PAYMENT_ALREADY_SUCCEEDED` |
| R3(b) số unit khác Σ `quantity` | Log ERROR, vẫn commit; DOC-30 báo, RB-02 điều tra |
| `NaiveClaimer`/`CounterClaimer` không có profile `experiment` | `api` không khởi động |

## 11. Test bắt buộc

Tiền tố `INVT-`. INVT-01…15 là các ID mà DOC-03 đã dẫn (giữ đúng nghĩa); test đồng thời theo DOC-69 §8 (64 luồng, `CountDownLatch`, lặp 20 lần), chạy bằng `make it` trên Testcontainers PostgreSQL 18; mỗi test đồng thời kết thúc bằng kiểm tra bất biến (DOC-69 §8.1).

| ID | Mức | Kịch bản | Kỳ vọng |
| --- | --- | --- | --- |
| INVT-01 | C | Ghế A1 `AVAILABLE`; 64 luồng cùng giữ A1; lặp 20 lần (P5-02) | Mỗi lần đúng 1 thành công, 63 nhận 409 `SEATS_UNAVAILABLE`; không deadlock; A1 `HELD` một reservation |
| INVT-02 | C | Pool GA 50 vé; 64 luồng, mỗi luồng xin ngẫu nhiên 1–4 vé; lặp 20 lần | Mỗi lần Σ unit `HELD` ≤ 50 và = Σ `quantity` của reservation `ACTIVE`; số unit của pool vẫn 50; không deadlock |
| INVT-03 | I | Request SEAT 2 ghế + ZONE 3 vé + GA 2 vé, một ghế đã `HELD` (P5) | 409 `SEATS_UNAVAILABLE`, `unavailableSeatIds` 1 phần tử; 0 unit mới `HELD`; không dòng `reservation`, `orders`, `idempotency_key` mới |
| INVT-04 | I | 51 vé GA trong một lệnh; rồi 50 vé | 422 `too_many_units` (`params.max = 50`), `HikariPool.activeConnections` không tăng; 50 → 201 |
| INVT-05 | U | `items` rỗng; 21 dòng; `seatIds` trùng; `quantity = 0`; hai dòng cùng `zoneId`; hai dòng cùng `ticketTypeId` | Mỗi ca 422 `VALIDATION_FAILED` đúng `rule`, không mở transaction |
| INVT-06 | C | (a) Người dùng có reservation `ACTIVE`, giữ lần nữa; (b) hai request song song khác key | (a) 409 `ACTIVE_RESERVATION_EXISTS` kèm đúng `reservationId`; (b) đúng 1 tạo được, bên kia 409 |
| INVT-07 | I | Sự kiện `PAUSED`, `DRAFT`, `SALE_CLOSED`, `now() < sale_starts_at` | 409 `EVENT_NOT_ON_SALE` với `displayStatus` tương ứng; `UPCOMING` kèm `saleStartsAt` |
| INVT-08 | I | `seatIds` của ghế loại `ZONE`; `zoneId` không phải pool ZONE; `ticketTypeId` là loại `SEAT`; id của sự kiện khác | 422 `wrong_model` ×3; 422 `unknown_reference` |
| INVT-09 | I | Pool còn 3 vé, xin 4 | 409 `INSUFFICIENT_CAPACITY`, `requested = 4`, `available = 3`, `poolId` đúng; 0 unit mới `HELD` |
| INVT-10 | I | Giữ 2 vé giá 100.000 + 1 vé loại khác 250.000; và một lệnh toàn vé 0 đồng | `orders.amount = 450000`, `PENDING_PAYMENT`; `reservation_item.unit_price` đúng giá lúc giữ; lệnh 0 đồng tạo đơn `amount = 0` |
| INVT-11 | I | Giữ vé thành công; đọc `expires_at` và `now()` của DB lúc chèn | `expires_at − created_at = 600 giây` (±1 giây); không dùng giờ JVM |
| INVT-12 | I | 1.000 reservation quá hạn (GA, 1–4 vé), job nhịp 5 giây, lô 200 | Mọi unit về `AVAILABLE` ≤ 30 giây sau `expires_at`; reservation `EXPIRED`, đơn `EXPIRED`, `closed_at` có giá trị |
| INVT-13 | C | Job chạy hai lần liên tiếp trên cùng dữ liệu; và dừng giữa vòng (ngắt sau `claimDue`) rồi chạy lại | Unit của reservation khác không đổi; `ticket_expiry_released_units_total` = số unit ban đầu, không unit nào trả hai lần |
| INVT-14 | I | Reservation `ACTIVE` chưa có PaymentIntent; `DELETE /reservations/{id}`; gọi lại cùng key và khác key | 200 `{"status":"CANCELLED"}`, unit về `AVAILABLE` trước khi response về, đơn `CANCELLED`; gọi lại → 200 `CANCELLED` (cùng key: `Idempotent-Replayed: true`) |
| INVT-15 | I | Reservation `EXPIRING` có `expiring_since = now() − 31 giây` | Vòng job sau đặt lại `expiring_since = now()` và trả vé; log `reservation.expiry.reclaimed` |
| INVT-16 | C | Giữ + hết hạn + giữ lại liên tục: pool 50, `hold-duration = PT2S`, `expiry.interval = PT1S`, 32 luồng giữ 1–4 vé lặp trong 60 giây | Mọi mẫu lấy mỗi 100 ms: Σ unit `HELD` ≤ 50; cuối test `make invariants` sạch; không deadlock |
| INVT-17 | I | Lệnh trộn SEAT 2 + ZONE 3 + GA 2 thành công (P5) | 1 reservation; 4 `reservation_item` (2 SEAT nhãn ghế, 1 ZONE `{"zone":…}`, 1 GA `{}`); 7 unit `HELD`; `amount` đúng |
| INVT-18 | C | Xác nhận (transaction DOC-14 §7) và R3 của cùng reservation `EXPIRING` chạy song song, 500 lần | Không deadlock; mỗi lần đúng một trong hai: (`CONFIRMED`, unit `SOLD`) hoặc (`EXPIRED`, unit `AVAILABLE`, đơn `EXPIRED`) |
| INVT-19 | I | Gọi `ReleaseTxService.finish` hai lần cho cùng reservation | Lần 2: 0 dòng ở R3(a), không đổi unit hay đơn |
| INVT-20 | I | `hold.bulkhead.permits = 1`, một lệnh giữ đang treo (khóa giả lập) | Lệnh thứ hai 503 `OVERLOADED`, `Retry-After` ∈ {1, 2, 3}; không lấy connection |
| INVT-21 | I | Cờ `soldout:pool:{p}` có trong Redis; giữ vé GA của pool đó; rồi tắt Redis và giữ lại | Lần 1: 409 `INSUFFICIENT_CAPACITY` `available = 0`, không câu SQL nào chạy; lần 2 (Redis tắt): đi vào DB như thường |
| INVT-22 | I | Pool hết vé, một reservation hết hạn và được trả | Sau R3 commit, cờ `soldout:pool:{p}` bị xóa |
| INVT-23 | C | Pool 100, 32 luồng giữ liên tục trong khi giảm sức chứa 100 → 60 | Không bán vượt (Σ `HELD`+`SOLD` ≤ `capacity` mọi lúc); lệnh giảm hoặc thành công với đúng 40 unit `REMOVED` và `capacity = 60`, hoặc 409 `CAPACITY_BELOW_USED` không đổi gì |
| INVT-24 | I | Tăng sức chứa GA 100 → 150 | 50 unit mới `AVAILABLE`, `capacity = 150`, cờ hết vé bị xóa |
| INVT-25 | C | Hai `ReleaseService.runOnce()` chạy song song trên 1.000 reservation quá hạn | Mỗi reservation `EXPIRED` đúng một lần; tổng unit trả = tổng unit đã giữ |
| INVT-26 | I | `PaymentIntentCanceller` giả chờ 1 giây mỗi lần, 200 reservation có `payment_intent_id`, `batch-budget = PT5S` | Vòng 1 xử lý ~5; phần còn lại có `expiring_since ≤ now() − lease`; vòng sau nhặt ngay |
| INVT-27 | I | `inventory.strategy=naive` không có profile `experiment`; có profile | Không có profile: `api` không khởi động; có profile, kịch bản INVT-02 lặp 20 lần: ít nhất một lần Σ `quantity` reservation `ACTIVE` > 50 (bán vượt được tái hiện) |
| INVT-28 | C | `inventory.strategy=counter` (profile `experiment`), kịch bản INVT-02 | Không bán vượt; `available + held + sold = 50` sau mỗi lần |
| INVT-29 | I | `createGaPools` hai loại vé 5.000 và 300 | Đúng 5.300 unit `AVAILABLE`, 2 pool `capacity` 5.000 và 300 (FR-02.7) |
| INVT-30 | I | Xóa loại vé GA đã xuất bản khi có 1 unit `HELD`; khi không có | 409 `TICKET_TYPE_IN_USE`; không có: unit `AVAILABLE` → `REMOVED`, `removed_at` đặt, `capacity = 0` |
| INVT-31 | I | Lệnh giữ vé chạm `lock_timeout` (giữ khóa `reservation_open_uq` từ transaction khác > 1 giây) | 503 `OVERLOADED`; không dòng nào còn lại |
| INVT-32 | I | `countsByTicketType` trên sự kiện 100.000 unit (DR-155) | `EXPLAIN` dùng `unit_event_idx`; kết quả khớp `GROUP BY` quét bảng |

## 12. Kiểm chứng ở task sau

- P2-01: migration thêm `unit_event_idx` (DR-155) và CHECK `close_reason` theo `status` (DR-90, DOC-14 "Kiểm chứng ở task sau").
- S-03 (trước P2-00): §6.3; kết quả cập nhật §4.8, §4.9, §6.
- P3: cài đặt thật `PaymentIntentCanceller`; INVT-26 chạy với cổng giả.
- P5-02: INVT-01, INVT-03, INVT-17 (ghế, zone).

## 13. Quyết định phát sinh khi viết tài liệu này

Claude chốt theo ủy quyền của Owner; nội dung đầy đủ chép vào sổ quyết định khi hợp nhất.

- **DR-155 · Index `unit_event_idx`.** §4.9, §6.1.
- **DR-167 · Tách điều phối không transaction và service có transaction cho giữ vé và trả vé.** `HoldService` → `HoldTxService`, `ReleaseService` → `ReleaseTxService` (§3.1).
- **DR-168 · `reservation` chèn bằng câu `INSERT … ON CONFLICT`, `reservation_item` chèn theo lô sau claim; aggregate `Reservation` chỉ để đọc** (§4.1.1).
- **DR-169 · Thứ tự câu trong transaction trả vé: reservation → unit → đơn** (§4.4).
- **DR-170 · Ngân sách thời gian một vòng job `reservation.expiry.batch-budget` = PT20S** (§4.5).
- **DR-171 · Nhãn `model` của `ticket_hold_duration_seconds` với lệnh trộn** (§9).
- **DR-172 · `available` của `INSUFFICIENT_CAPACITY` là số đếm trong cùng transaction, trước rollback** (§4.2).
- **DR-173 · `InventoryApi.release/confirmSold` nhận `poolItems` cho `CounterClaimer`** (§3.2, §5).

## Câu hỏi còn mở

- **Q1 · NFR-03 khi 5.000 reservation hết hạn cùng lúc với một bản `api`.** Với nhịp `fixedDelay` 5 giây và lô 200, reservation cuối được trả sau ~150 giây (§4.5), trong khi NFR-03 và EXP-04 đòi ≤ 30 giây. Phương án: (a) chạy lại ngay (không chờ nhịp) khi vòng trước lấy đủ lô, tới khi hết hoặc hết 25 giây — 5.000 GA ≈ 25 vòng × ~1 giây; (b) tăng `batch-size` lên 1.000; (c) hiệu chỉnh mô tả NFR-03 thành "≤ 30 giây với tải bình thường". Đề xuất (a), ghi là **DR-182 (Đề xuất)**; đây là thay đổi hành vi của job do DR-42 đặt (nhịp cố định) và ảnh hưởng chỉ tiêu NFR-03, nên chờ Owner chốt trước P2-09. Tài liệu ở `Draft` tới khi DR-182 được chốt.
