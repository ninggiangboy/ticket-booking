# Kiến trúc code

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-12
> Phụ thuộc: SDD gốc §4, §13.2, phụ lục, [DOC-07](system-context-and-containers.md), [DOC-11](tech-stack-and-versions.md), [DOC-06](../02-glossary.md), [Sổ quyết định](../00-decision-register.md) (DR-01, 03, 05, 06, 10, 11, 39, 41, 67, 76, 79, 81), [ADR-0002](../04-adr/0002-modular-monolith-postgres-source-of-truth.md), [ADR-0012](../04-adr/0012-spring-data-jdbc-and-modulith-boundaries.md)
> Người dùng chính: P1-01, P1-05 (khung module và test kiến trúc); mọi task backend và frontend; người review PR

Tài liệu cho biết đặt class ở đâu, layer nào được làm gì, module gọi nhau thế nào và cách Spring Data JDBC được dùng trong dự án; phần cuối là checklist review. Không giải thích lại Spring. Bảng sở hữu dữ liệu và đồ thị module ở [DOC-07](system-context-and-containers.md) §3–§4; thư viện và phiên bản ở [DOC-11](tech-stack-and-versions.md); mã lỗi và exception ở DOC-35; cấu hình ở DOC-34.

## 1. Bố cục repo

Monorepo ngay tại gốc repo (DR-01), không thư mục `event-ticketing/` lồng thêm:

```
backend/                 # Gradle Kotlin DSL, wrapper commit sẵn, một project duy nhất `api`
  gradle/libs.versions.toml
  src/main/java/io/ticket/…
  src/main/resources/{application.yml, db/migration, db/migration-fake, db/migration-experiment, messages_vi.properties, messages_en.properties, templates/, lua/}
  src/test/java/io/ticket/…
frontend/                # pnpm 10, Vite 7; `map-core` là thư mục src/map-core, không phải package npm
deploy/compose/          # docker-compose.yml, nginx.conf, .env.example, init scripts, scratch/ddl-check.sql
load/                    # k6 (TypeScript)
experiments/             # runner EXP-01…10 (bash + k6 + SQL), seed/users.sql, notebook phân tích
docs/
Makefile                 # giao diện lệnh duy nhất
```

Lệnh chuẩn (chi tiết ở DOC-61): `make up`, `down`, `reset`, `seed`, `dev`, `test` (backend + frontend unit), `it` (integration Testcontainers), `e2e`, `lint`, `invariants`, `exp EXP=02`, `contract`, `up-stripe`. CI gọi đúng các target này (DR-08).

## 2. Backend: layer trong module

Gói gốc `io.ticket`. Mỗi module nghiệp vụ `io.ticket.<module>` là một module Spring Modulith (DR-06). Bên trong chia theo layer, **mỗi layer một package**; không phải module nào cũng có đủ, tạo package khi có class đầu tiên.

| Package | Layer | Chứa | Được gọi bởi | Không được làm |
| --- | --- | --- | --- | --- |
| `<module>` (gốc) | API công khai | `…Api` (interface), `record` DTO trao đổi giữa module, event của module (`OrderPaid`), exception thuộc hợp đồng `…Api` (kế thừa `common.error.DomainException`), SPI (`PaymentIntentCanceller`) | Module khác | Chứa logic hay truy cập dữ liệu |
| `<module>.controller` | Điểm vào | `@RestController`, webhook endpoint: nhận request, validate hình thức (`@Valid`), gọi service, trả DTO | HTTP | Gọi repository, client; mở transaction; chứa logic nghiệp vụ; trả entity |
| `<module>.job` | Điểm vào | `@Scheduled` `…Job`: đọc lịch/cấu hình, gọi service | Lịch | Gọi repository; mở transaction; gọi controller |
| `<module>.listener` | Điểm vào | Nghe event của module khác (`…Listener`), gọi service | Event | Như `job` |
| `<module>.service` | Service | `@Service`: logic nghiệp vụ, **ranh giới transaction**, cài đặt `…Api`, mapper `…Mapper` entity ↔ DTO | Điểm vào cùng module; module khác qua `…Api` | Trả entity ra ngoài layer; gọi controller; gọi `service` của module khác (chỉ qua `…Api`) |
| `<module>.repository` | Truy cập dữ liệu | Repository Spring Data JDBC, custom fragment `…Claims` + `…Impl` dùng `JdbcClient`, `…RedisRepository` | Service cùng module | Gọi service; mở transaction; chứa quyết định nghiệp vụ |
| `<module>.client` | Hệ thống ngoài | Stripe (`PaymentGateway` + hai adapter), S3, SMTP | Service cùng module | Gọi service; chạy trong transaction của người gọi mà không có timeout |
| `<module>.entity` | Dữ liệu | Entity ánh xạ bảng, enum trạng thái | Repository, service cùng module | Có logic gọi ra ngoài |
| `<module>.dto` | Dữ liệu | `record` request/response của REST API | Controller, service cùng module | Dùng ở module khác (DTO chia sẻ nằm ở package gốc) |
| `<module>.config` | Kỹ thuật (không phải layer) | `@Configuration`, `@ConfigurationProperties` (`record`), bean chọn bản cài đặt (`inventory.strategy`) | Spring | Chứa logic nghiệp vụ |

Gói `config` không có trong DR-06; nó là package kỹ thuật cho cấu hình Spring của module và không tham gia luật layer (DR-81).

### 2.1 Luật layer (một module)

1. Chiều gọi chỉ đi xuống: điểm vào (`controller`, `job`, `listener`) → `service` → `repository` / `client`. Điểm vào không gọi repository hay client và không gọi lẫn nhau; repository và client không gọi service.
2. `@Transactional` chỉ đặt ở `service`; điểm vào không tự mở transaction.
3. Entity không ra khỏi layer service: controller nhận và trả DTO; service đổi entity ↔ DTO bằng mapper viết tay (`…Mapper`, không MapStruct).
4. DTO là `record` (Java 25), bất biến; validate bằng Bean Validation trên controller (`@Valid`).
5. Hằng số tên cột/bảng không dùng chung giữa module (mỗi module có SQL của riêng mình).

### 2.2 Luật module (giữa các module, DR-05, DR-06, DR-79)

1. Mỗi bảng PostgreSQL và họ key Redis có đúng một module sở hữu ([DOC-07](system-context-and-containers.md) §4); chỉ repository của module đó đọc/ghi. Chuỗi SQL trong module X chỉ nhắc bảng của X.
2. Module khác chỉ được dùng **package gốc** (`…Api`, DTO, event). Mọi package con là nội bộ (mặc định của Spring Modulith, không cần `@NamedInterface`).
3. Gọi đồng bộ: service A inject interface `BApi` (không inject class cài đặt). Báo ngược chiều hoặc báo cho nhiều module: publish event của module (`ApplicationEventPublisher`), module nhận xử lý ở `listener`. Event cùng transaction dùng `@EventListener` hoặc `@TransactionalEventListener(phase = BEFORE_COMMIT)`; event chỉ để thông báo (không dùng để lấy giá trị trả về).
4. **Không có vòng phụ thuộc.** Đồ thị cho phép là bảng dưới; mỗi module khai báo trong `package-info.java` bằng `@ApplicationModule(allowedDependencies = {…})` nên `verify()` đỏ khi có phụ thuộc ngoài danh sách.
5. Đảo chiều bằng SPI: `reservation` định nghĩa `PaymentIntentCanceller` ở package gốc, `payment` cài đặt (nên `payment → reservation`, không ngược lại); `common` định nghĩa `RetentionContributor`, mỗi module cài đặt.
6. `common` (cấu hình, `@RestControllerAdvice`, i18n, principal, tiện ích, `RetentionJob`) không phụ thuộc module nghiệp vụ.
7. Ngoại lệ chỉ đọc: package `io.ticket.invariant` được `SELECT` bảng của mọi module; ArchUnit cấm `INSERT/UPDATE/DELETE` ở đó.

| Module | `allowedDependencies` |
| --- | --- |
| `auth`, `media`, `notification`, `inventory` | (không có; chỉ `common`) |
| `event` | `inventory` |
| `ticket` | `event` |
| `order` | `event`, `ticket` |
| `map` | `event`, `inventory`, `media` |
| `admission` | `event`, `inventory` |
| `reservation` | `event`, `inventory`, `order`, `admission` |
| `payment` | `reservation`, `order`, `inventory`, `ticket`, `notification`, `event` |
| `studio` | `event`, `map`, `media`, `inventory`, `order`, `ticket`, `notification`, `reservation` |
| `invariant` | (không có; đọc bảng bằng SQL chỉ đọc) |

### 2.3 Cây mẫu của một module: `reservation`

```
io.ticket.reservation
├── ReservationApi.java                     # interface công khai: get, confirm, beginExpiry, expireAllForEvent, itemsOf
├── IdempotencyApi.java                     # công khai cho payment (confirm-free)
├── PaymentIntentCanceller.java             # SPI do payment cài đặt
├── ReservationView.java                    # record DTO dùng giữa module
├── ActiveReservationExistsException.java   # exception thuộc hợp đồng Api (DomainException)
├── package-info.java                       # @ApplicationModule(allowedDependencies = {"event","inventory","order","admission"})
├── controller/
│   ├── ReservationController.java          # POST /events/{id}/reservations, GET|DELETE /reservations/{id}
├── job/
│   └── ReleaseExpiredHoldsJob.java         # @Scheduled(fixedDelayString = "${reservation.expiry.interval}")
├── listener/                               # (module này chưa nghe event nào)
├── service/
│   ├── HoldService.java                    # điều phối transaction giữ vé (DR-41)
│   ├── ReleaseService.java                 # thủ tục trả vé dùng chung cho job và đường nhanh (DR-43)
│   ├── IdempotencyService.java             # cài đặt IdempotencyApi
│   ├── ReservationService.java             # cài đặt ReservationApi
│   └── ReservationMapper.java
├── repository/
│   ├── ReservationRepository.java          # ListCrudRepository<Reservation, UUID> + @Modifying @Query
│   ├── ReservationClaims.java              # custom fragment: câu SQL claim reservation đến hạn (JdbcClient)
│   ├── ReservationClaimsImpl.java
│   └── IdempotencyKeyRepository.java
├── entity/
│   ├── Reservation.java                    # aggregate, @MappedCollection reservation_item
│   ├── ReservationItem.java
│   └── ReservationStatus.java              # enum ↔ text
├── dto/
│   ├── CreateReservationRequest.java       # record
│   └── ReservationResponse.java            # record
└── config/
    └── ReservationProperties.java          # @ConfigurationProperties("reservation") record
```

Module `inventory` tương tự: `InventoryApi`, `ClaimResult` (gốc); `repository/InventoryClaimer` (interface) với `SkipLockedClaimer`, `CounterClaimer`, `NaiveClaimer` (DR-76); `repository/InventoryUnitRepository` + `InventoryUnitClaims` + `InventoryUnitClaimsImpl`; `service/InventoryService implements InventoryApi`; `entity/InventoryUnit`, `UnitStatus`; `config/InventoryStrategyConfig` chọn bean theo `inventory.strategy` (khác `skip-locked` thì cần profile `experiment`, nếu không API từ chối khởi động).

### 2.4 Transaction

- **Chỉ `service` mở transaction** (`@Transactional`). Mặc định `REQUIRED`, `READ COMMITTED` (mặc định PostgreSQL).
- **Module điều phối** của transaction liên module ([DOC-07](system-context-and-containers.md) §4.3) mở transaction; method của `…Api` chỉ được gọi bên trong transaction khác đánh `@Transactional(propagation = Propagation.MANDATORY)` để nổ lỗi ngay nếu bị gọi ngoài transaction. Method `…Api` đọc độc lập dùng `REQUIRED` (hoặc `readOnly`).
- Transaction nóng đặt `SET LOCAL statement_timeout` và `lock_timeout` bằng `JdbcClient` ở đầu transaction (giữ vé: 2 giây và 1 giây; xuất bản: 30 giây; hủy event: 60 giây; dựng lại kho vé theo sơ đồ: `lock_timeout` 5 giây).
- **Không gọi ra ngoài khi đang mở transaction:** Stripe, SMTP, S3, Redis chạy trước hoặc sau transaction, hoặc qua outbox. Kiểm tra rẻ (Redis, `ZSCORE`, cờ hết vé, rate limit, bulkhead) chạy **trước** khi lấy connection.
- So sánh thời gian quyết định nghiệp vụ nằm trong SQL bằng `now()`; Java không gọi `Instant.now()` cho hạn giữ vé, hạn token, khung mở bán (DR-12).

## 3. Spring Data JDBC trong dự án

### 3.1 Aggregate

| Module | Aggregate (có `save()` để chèn và sửa trường không phải trạng thái) | Truy cập không qua aggregate |
| --- | --- | --- |
| `auth` | `AppUser`, `Organizer` | `login_token`, `session` (`JdbcClient`/`@Modifying @Query`, không `save()`) |
| `media` | `Media` | — |
| `event` | `Event` (`@Version` trên `row_version`), `TicketType` | — |
| `map` | `SeatMap`, `SeatMapVersion` | — |
| `inventory` | — | `inventory_unit`, `inventory_pool`: chỉ đọc bằng repository, ghi bằng SQL riêng; `inventory_pool_counter` |
| `reservation` | `Reservation` (kèm `ReservationItem` qua `@MappedCollection`) | `idempotency_key` (`JdbcClient`) |
| `order` | `Order` (`@Table("orders")`) | — |
| `ticket` | `Ticket` | — |
| `payment` | — | `stripe_event`, `fake_payment_intent` (`JdbcClient`) |
| `notification` | `OutboxMessage` | claim bằng `@Modifying @Query` |

### 3.2 Luật

1. **Không bao giờ đổi `status` bằng `save()`.** Mọi chuyển trạng thái là method `@Modifying @Query("UPDATE … WHERE … AND status = :from")` trả `int` số dòng; service kiểm tra số dòng và quyết định (SDD gốc 4.2). `save()` chỉ dùng để chèn dòng mới và sửa trường không phải trạng thái (tên, mô tả, giá, bản nháp sơ đồ).
2. **ID gán trước bằng UUIDv7** (DR-11): chèn bằng `JdbcAggregateTemplate.insert(entity)` (hoặc entity cài `Persistable.isNew()`), vì `save()` với ID khác null sẽ chạy `UPDATE`.
3. **`Reservation` chỉ `save()` một lần lúc chèn:** aggregate có `@MappedCollection` bị xóa-chèn lại toàn bộ con khi `save()`.
4. **SQL phức tạp** (claim `SKIP LOCKED` có CTE và `RETURNING`, chèn hàng loạt `generate_series`, snapshot tình trạng, kiểm tra bất biến) nằm trong custom repository fragment dùng `JdbcClient`; vẫn là SQL viết tay, ở trong module sở hữu bảng.
5. **Cột `jsonb`** (`seat_map.draft`, `seat_map_version.document`, `reservation_item.label`, `ticket.label`, `outbox.payload`) ánh xạ bằng cặp `@ReadingConverter`/`@WritingConverter` qua `PGobject`; cột trạng thái ánh xạ Java `enum` ↔ `text`.
6. **Khóa lạc quan:** `Event.rowVersion` dùng `@Version`; `PATCH` so `rowVersion` của client với entity đã đọc, lệch → `StaleEventVersionException` (409 `STALE_EVENT_VERSION`); `OptimisticLockingFailureException` do đua cũng ánh xạ về mã này (DR-70).
7. **Truy vấn đọc cho giao diện** viết bằng `@Query` trả projection `record`, không nạp aggregate.

```java
// repository/ReservationRepository.java
public interface ReservationRepository
    extends ListCrudRepository<Reservation, UUID>, ReservationClaims {

  @Modifying
  @Query("""
      UPDATE reservation
         SET status = 'EXPIRING', close_reason = :reason, expiring_since = now()
       WHERE reservation_id = :id AND user_id = :userId AND status = 'ACTIVE'
      """)
  int beginExpiryByBuyer(UUID id, UUID userId, String reason);   // service kiểm tra == 1
}
```

```java
// repository/InventoryUnitClaimsImpl.java: claim ghế, một câu cho mọi ghế (DR-41, SDD gốc 8.2)
class InventoryUnitClaimsImpl implements InventoryUnitClaims {
  private final JdbcClient jdbc;

  @Override
  public int claimSeats(UUID eventId, List<UUID> seatIds, UUID reservationId) {
    return jdbc.sql("""
        WITH picked AS (
          SELECT unit_id FROM inventory_unit
           WHERE event_id = :event AND seat_id = ANY(:seatIds) AND status = 'AVAILABLE'
           FOR UPDATE SKIP LOCKED
        )
        UPDATE inventory_unit u
           SET status = 'HELD', reservation_id = :rid, updated_at = now()
          FROM picked WHERE u.unit_id = picked.unit_id
        """)
        .param("event", eventId).param("seatIds", seatIds.toArray(UUID[]::new))
        .param("rid", reservationId)
        .update();                                   // HoldService so sánh với seatIds.size()
  }
}
```

```java
// config/JsonbConverters.java
@WritingConverter
class MapToJsonb implements Converter<Map<String, Object>, PGobject> {
  public PGobject convert(Map<String, Object> source) {
    PGobject o = new PGobject();
    o.setType("jsonb");
    o.setValue(objectMapper.writeValueAsString(source));
    return o;
  }
}
```

### 3.3 `…Api` và nơi mở transaction (ví dụ)

```java
// io.ticket.inventory.InventoryApi (package gốc)
public interface InventoryApi {
  /** Bên trong transaction của HoldService. Trả số unit đã claim; bên gọi so với số yêu cầu. */
  ClaimResult claim(UUID eventId, UUID reservationId, List<ClaimRequest> items);

  /** Xác nhận: HELD → SOLD cho mọi unit của reservation. */
  int confirmSold(UUID reservationId);

  /** Trả vé: HELD → AVAILABLE cho mọi unit của reservation. */
  int release(UUID reservationId);

  AvailabilitySnapshot snapshot(UUID eventId);           // đọc độc lập, cache Caffeine 2 giây
  int createSeatUnits(UUID eventId, Map<UUID, UUID> ticketTypeBySeat, String documentJson);
}

// io.ticket.reservation.service.HoldService
@Service
class HoldService {
  @Transactional
  public ReservationResponse hold(UUID userId, UUID eventId, UUID idemKey, String rawBody, HoldCommand cmd) {
    // SET LOCAL statement_timeout = '2s'; lock_timeout = '1s'
    // 1. idempotency  2. eventApi.requireOnSale  3. insert reservation
    // 4. inventoryApi.claim  5. orderApi.createPending  6. idempotency.complete(…)
  }
}
```

## 4. DTO, lỗi và log

- **DTO** là `record`; tên trường camelCase; trường không có giá trị là `null` (không bỏ, DR-63). Mapper entity ↔ DTO viết tay, đặt ở `service`.
- **Lỗi nghiệp vụ** là exception kế thừa `common.error.DomainException(code, status)`; exception thuộc hợp đồng `…Api` nằm ở package gốc, exception nội bộ ở `service`. `@RestControllerAdvice` trong `common` đổi sang Problem Details; bảng exception → mã lỗi → HTTP ở DOC-35. Service không trả `ResponseEntity`.
- **Log:** JSON ECS; MDC `trace_id` (từ `X-Request-Id`), `user_id`, `event_id`, `reservation_id`, `order_id`; không bao giờ log email (DR-09, DR-22). Lỗi nghiệp vụ mức INFO/WARN, lỗi lập trình mức ERROR (DOC-35).

## 5. Test kiến trúc

Chạy trong `make test`; mỗi quy tắc có test thất bại có chủ đích ở P1-05 (cho controller gọi repository, module import `service` của module khác, hai module gọi vòng nhau, `save()` trên `Order` sau khi chèn).

| ID | Công cụ | Quy tắc | Kiểm tra |
| --- | --- | --- | --- |
| ARC-01 | Spring Modulith `ApplicationModules.of(Application.class).verify()` | Chỉ dùng package gốc của module khác; không vòng phụ thuộc; chỉ `allowedDependencies` khai báo | Một unit test `ModularityTests` |
| ARC-02 | ArchUnit `layeredArchitecture()` | Chiều gọi trong module: điểm vào → service → repository/client; điểm vào không gọi nhau | `LayerRulesTest` |
| ARC-03 | ArchUnit | `@Transactional` chỉ trong `..service..` | `TransactionRulesTest` |
| ARC-04 | ArchUnit | `..controller..` không dùng `..entity..` | `LayerRulesTest` |
| ARC-05 | ArchUnit tùy biến | Chuỗi SQL (hằng, `@Query`, text block) trong module X chỉ nhắc bảng của X theo danh sách ở DOC-07 §4.1 | `TableOwnershipTest` |
| ARC-06 | ArchUnit tùy biến | Không method nào ngoài lớp chèn gọi `save()` trên aggregate có cột `status` (`Reservation`, `Order`, `Ticket`, `Event`, `OutboxMessage`) | `NoStatusSaveTest` |
| ARC-07 | ArchUnit tùy biến | Package `io.ticket.invariant` không chứa `INSERT/UPDATE/DELETE` | `InvariantReadOnlyTest` |
| ARC-08 | ArchUnit | `…Api` implementation method gọi từ module khác đánh `Propagation.MANDATORY` khi nằm trong danh sách transaction liên module | `TransactionRulesTest` |
| ARC-09 | JUnit | `application.yml` không có secret cứng; mỗi key của module có dòng ở DOC-34 | `ConfigKeysTest` (khung ở P1) |

## 6. Frontend

```
frontend/
├── src/
│   ├── app/                    # router (createBrowserRouter, lazy route), providers, khởi tạo i18n, layout chung
│   ├── api/                    # schema.d.ts (sinh bằng `pnpm gen:api`), client openapi-fetch, interceptor X-Server-Time và CSRF
│   ├── features/
│   │   ├── auth/               # đăng nhập, callback, menu tài khoản, bộ chọn ngôn ngữ
│   │   ├── events/             # danh sách, chi tiết sự kiện
│   │   ├── queue/              # phòng chờ
│   │   ├── seat-picker/        # trình xem sơ đồ, chọn ghế, chế độ Danh sách, chọn số lượng GA
│   │   ├── checkout/           # Payment Element, đồng hồ giữ vé, kết quả
│   │   ├── tickets/            # Vé của tôi
│   │   └── studio/
│   │       ├── event-form/     # hồ sơ, thông tin, loại vé
│   │       ├── map-editor/     # tools/, geometry/, commands/, validation/, store/
│   │       ├── publish/
│   │       └── sales/
│   ├── map-core/               # schema/seat-map.v1.json, model, geometry, validation, render, fixtures/validation/*.json
│   ├── components/             # component design system (DOC-39): Button, Field, Chip, SeatLegend, HoldTimer…
│   ├── lib/                    # offset giờ server, idempotency key, lưu lựa chọn (localStorage), định dạng Intl
│   ├── locales/{vi,en}/*.json  # common, events, seats, checkout, tickets, queue, studio, editor, auth, error, validation
│   ├── mocks/                  # MSW handler theo nhóm endpoint (VITE_API_MOCK=1)
│   └── styles/tokens.css       # token "Vé giấy" (DR-68)
├── e2e/                        # Playwright
└── package.json                # packageManager pnpm 10
```

Luật phụ thuộc (ESLint `no-restricted-imports`/`import/no-restricted-paths`):

1. `features/*` không import lẫn nhau; dùng chung thì chuyển lên `components/`, `lib/`, `api/` hoặc `map-core/`.
2. `map-core` không import React, Konva hay `features/*` ngoài phần render (`map-core/render` dùng canvas thuần/Konva); phần mô hình, hình học, validate là TypeScript thuần để chạy ở Vitest.
3. `features/studio/map-editor/store` là Zustand vanilla, thay đổi `doc` chỉ qua `Command { do, undo, label }` (DR-39); component không đọc store cục bộ ngoài hook selector.
4. Mọi chuỗi giao diện qua i18next (`t('checkout.hold.expired.title')`); `pnpm i18n:check` đỏ khi hai locale lệch key; không chuỗi cứng trong JSX.
5. Mọi lời gọi API qua `api/` (kiểu từ `schema.d.ts`); không `fetch` rải rác. Request có `Idempotency-Key` giữ chuỗi body đã serialize cùng key (DR-45).
6. Chunk tải lười: `seat-picker` (Konva), `studio-map` (Konva + editor), `checkout` (Stripe.js) (DR-67).

## 7. Checklist review PR

Người review (và tác giả) đối chiếu từng mục; mục nào liên quan thì có test hoặc nêu lý do bỏ qua.

| # | Mục | Cách kiểm tra |
| --- | --- | --- |
| 1 | **Câu ghi có điều kiện:** mọi đổi trạng thái là `UPDATE … WHERE … AND status = :from`, kiểm số dòng trả về; không đọc rồi mới quyết định ghi | Đọc diff; `NoStatusSaveTest` |
| 2 | **Không gọi ra ngoài trong transaction:** Stripe, SMTP, S3, Redis nằm trước, sau transaction hoặc qua outbox; kiểm tra rẻ chạy trước khi lấy connection | Đọc diff; không `@Transactional` gọi `client`/Redis |
| 3 | **Không chuỗi giao diện cứng** trong JSX, email, message; có key ở `vi` và `en` | `pnpm i18n:check`, test `messages_*.properties` |
| 4 | Layer đúng: controller chỉ nhận/trả DTO, entity không ra khỏi `service`, `@Transactional` chỉ ở `service` | `LayerRulesTest`, `TransactionRulesTest` |
| 5 | Chỉ dùng package gốc của module khác; bảng của module khác không xuất hiện trong SQL | `ModularityTests`, `TableOwnershipTest` |
| 6 | `…Api` mới: method chỉ gọi trong transaction đánh `MANDATORY` và có trong bảng giao dịch liên module ở DOC-07 §4.3 | Đọc diff, cập nhật DOC-07 |
| 7 | Thời gian quyết định nghiệp vụ dùng `now()` của database | `grep Instant.now` trong service nghiệp vụ |
| 8 | Job idempotent và an toàn khi chạy lại: câu ghi kèm điều kiện trạng thái và `reservation_id` | Test chạy hai lần |
| 9 | Lỗi mới là exception `DomainException` có `code`; có dòng trong bảng DOC-35 và key `error.<code>` | DOC-35, DOC-40 |
| 10 | Key cấu hình, metric, họ key Redis, chuỗi i18n mới đã ghi vào DOC-34, DOC-33, DOC-17, DOC-40 | Đọc diff |
| 11 | Không log email; log có `trace_id` và id nghiệp vụ | Test log, đọc diff |
| 12 | Tiền là `long` VND, không `double`; `payment.min-amount` kiểm ở nơi nhận giá | Đọc diff |
| 13 | Migration mới theo `V<yyyymmddHHmm>__<snake_case>.sql`, không sửa migration đã merge, chạy sạch trên `postgres:18-alpine` | `make it` |
| 14 | `FL-xx` và `E-xx` liên quan đã cập nhật khi đổi endpoint, câu SQL, mã lỗi hoặc thông báo giao diện | Checklist PR (master plan §7.2) |
| 15 | Test mới có tiền tố của tài liệu thiết kế và đăng ký ở DOC-69 | Đọc diff |

## 8. Test bắt buộc

Tiền tố `ARC-` (đăng ký ở DOC-69). Các test ở §5 (`ARC-01…09`) là danh mục đầy đủ; cố ý vi phạm từng quy tắc phải làm test đỏ (P1-05).

| ID | Kịch bản | Kỳ vọng |
| --- | --- | --- |
| ARC-10 | Cho một class `…Controller` gọi `…Repository` | `LayerRulesTest` đỏ |
| ARC-11 | Module `payment` import `io.ticket.reservation.service.HoldService` | `ModularityTests` đỏ |
| ARC-12 | `reservation` phụ thuộc `payment` (vòng với `payment → reservation`) | `ModularityTests` đỏ |
| ARC-13 | Gọi `orderRepository.save(order)` sau khi chèn để đổi `status` | `NoStatusSaveTest` đỏ |
| ARC-14 | Module `ticket` chứa SQL nhắc `reservation` | `TableOwnershipTest` đỏ |
| ARC-15 | `InvariantChecker` chứa `UPDATE` | `InvariantReadOnlyTest` đỏ |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: DR-79 (đồ thị phụ thuộc, `allowedDependencies`, SPI), DR-81 (package kỹ thuật `config`, danh sách thư viện bổ sung).
