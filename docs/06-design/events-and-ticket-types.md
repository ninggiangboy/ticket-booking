# Sự kiện và loại vé

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-20
> Phụ thuộc: SDD gốc §6.1–6.3, [DOC-03](../01-product/requirements.md) (FR-02, FR-13, FR-14, FR-18, FR-19), [DOC-04](../01-product/use-cases.md) (UC-07, UC-09, UC-13, UC-17), [DOC-06](../02-glossary.md), [DOC-07](../03-architecture/system-context-and-containers.md) §3, §4, [DOC-12](../03-architecture/code-architecture.md) §2, [DOC-14](../05-data/domain-model.md) §4.3, §4.4, §6.1, [DOC-15](../05-data/ops-model.md) §7.2, [DOC-24](inventory-and-reservation.md), [DOC-27](tickets-and-notifications.md) §8, [DOC-34](configuration-reference.md), [DOC-35](error-handling.md), [DOC-37](../07-api/api-endpoints.md) (E-06, E-07, E-13, E-23…36, E-39), [Sổ quyết định](../00-decision-register.md) (DR-12, 13, 23, 24, 25, 26, 27, 28, 29, 30, 38, 52, 70, 89, 91, 106, 143, 144, 153, 155), [ADR-0015](../04-adr/0015-media-in-s3-compatible-object-storage.md)
> Người dùng chính: P2-03.1, P2-04.1, P2-05.1 (module `event`, `studio`, `media`); tác giả DOC-84 (FL-05…11), DOC-55, DOC-56, DOC-59; P5 (đường xuất bản có sơ đồ)

Tài liệu này mô tả vòng đời của sự kiện và loại vé: trạng thái lưu và trạng thái hiển thị, quy tắc trường và múi giờ, transaction xuất bản tạo kho vé, tạm dừng, đóng bán sớm, hủy sự kiện, đổi lịch và job chuyển `ENDED`. DDL nằm ở DOC-14 §4.3; schema request/response ở DOC-37; câu claim, trả vé và câu đổi sức chứa ở mức kho vé ở DOC-24; email đổi lịch và chờ hoàn tiền (mẫu, relay) ở DOC-27 và DOC-51; luồng từng tầng ở DOC-84 (FL-05…11) và DOC-85 (FL-12). Xuất bản phiên bản sơ đồ trước giờ mở bán thuộc DOC-23.

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi thành DR-161…166 và tóm tắt ở §12.

## 1. Mục đích và phạm vi

| Class chính | Module · package | Chạy khi | Đọc | Ghi | Việc |
| --- | --- | --- | --- | --- | --- |
| `EventController` | `event.controller` | E-06, E-07 | qua `EventQueryService` | — | Đọc công khai |
| `EventQueryService` | `event.service` | E-06, E-07 | `event`, `ticket_type`; `InventoryApi.availabilityOf`; `AuthApi.organizerPublicInfo` | — | Danh sách, chi tiết, `displayStatus` |
| `EventService` (cài `EventApi`) | `event.service` | Gọi từ `studio`, `reservation`, `payment` | `event` | `event` | Tạo, sửa có điều kiện, đổi trạng thái, khóa dòng |
| `TicketTypeService` | `event.service` | Gọi qua `EventApi` | `ticket_type`, `event` | `ticket_type` | Thêm, sửa, xóa loại vé (DR-26) |
| `DisplayStatusCalculator` | `event.service` | Mọi response có sự kiện | pha tính trong SQL + tóm tắt tình trạng | — | §3.2 |
| `EventLifecycleJob` | `event.job` | `fixedDelay` `event.lifecycle.interval` (60 giây) | `event` | `event` | `PUBLISHED`/`PAUSED → ENDED` |
| `OrganizerEventController` | `studio.controller` | E-24…36 | — | — | Điểm vào HTTP của studio |
| `StudioEventService` | `studio.service` | E-24, E-25, E-26 | `EventApi`, `InventoryApi`, `MapApi` | qua `EventApi` | Tạo nháp, đọc để sửa, danh sách kèm số vé |
| `EventChangeService` | `studio.service` | E-27 | `EventApi`, `OrderApi`, `TicketApi`, `AuthApi` | `event`, `outbox` (qua API) | Sửa thông tin, fan-out email đổi lịch (DR-29) |
| `TicketTypeChangeService` | `studio.service` | E-34, E-35, E-36 | `EventApi`, `InventoryApi` | `ticket_type`, `inventory_*` (qua API) | Loại vé và đổi sức chứa khi đang bán (DR-30) |
| `PublishCheckService` | `studio.service` | E-28, đầu E-29 | `EventApi`, `MapApi` | — | Danh sách điều kiện xuất bản |
| `PublishService` | `studio.service` | E-29 | `EventApi`, `MapApi` | `event`, `inventory_pool`, `inventory_unit` (qua API) | Transaction xuất bản tạo kho vé (DR-27) |
| `SaleControlService` | `studio.service` | E-30, E-31, E-32 | — | `event` (qua `EventApi`) | Tạm dừng, mở lại, đóng bán sớm |
| `CancelEventService` | `studio.service` | E-33 | `EventApi`, `OrderApi`, `TicketApi`, `ReservationApi`, `AuthApi` | `event`, `orders`, `ticket`, `reservation`, `outbox` (qua API) | Hủy sự kiện (DR-28) |
| `MediaService` | `media.service` | E-39, E-13 | `media`, `s3` | `media`, `s3` | Ảnh sự kiện (§6.8, ADR-0015) |

Ranh giới module theo DOC-07 §4: `studio` điều phối mọi transaction chạm bảng của nhiều module; `event` sở hữu `event`, `ticket_type`; không module nào truy vấn bảng của module khác (DOC-12 §2.2).

## 2. Thành phần và interface

### 2.1 `EventApi` (package gốc `io.ticket.event`)

```java
public interface EventApi {

  /** Đọc không khóa; ném NotFoundException khi không có. */
  EventFacts get(UUID eventId);

  /** Như get nhưng thêm AND organizer_id = :org; không thuộc tổ chức → NotFoundException (404, DR-23). */
  EventFacts getOwned(UUID eventId, UUID organizerId);

  /** SELECT … FOR UPDATE kèm organizer_id. propagation = MANDATORY. */
  EventFacts lockForUpdate(UUID eventId, UUID organizerId);

  /** SELECT status … FOR SHARE (DR-28). propagation = MANDATORY; dùng bởi payment (DOC-26). */
  EventStatus lockForShare(UUID eventId);

  /** Bước 2 của giữ vé (DR-41): ném EventNotOnSaleException kèm displayStatus. MANDATORY. */
  void requireOnSale(UUID eventId);

  UUID createDraft(UUID organizerId, String name);

  /** UPDATE có điều kiện row_version (DR-70); trả bản trước và sau để EventChangeService so sánh. MANDATORY. */
  EventChange update(UUID eventId, UUID organizerId, int expectedRowVersion, EventPatch patch);

  /** Các chuyển trạng thái đơn câu; 0 dòng → EventStateConflictException. MANDATORY. */
  EventStateView markPublished(UUID eventId, UUID seatMapVersionIdOrNull);
  EventStateView pause(UUID eventId, UUID organizerId);
  EventStateView resume(UUID eventId, UUID organizerId);
  EventStateView closeSale(UUID eventId, UUID organizerId);
  EventStateView markCancelled(UUID eventId);

  List<TicketTypeView> ticketTypes(UUID eventId);                    // chưa xóa, theo sort_order
  TicketTypeView createTicketType(UUID eventId, TicketTypeDraft draft); // MANDATORY, sau lockForUpdate
  TicketTypeView updateTicketType(UUID eventId, UUID ticketTypeId, TicketTypePatch patch); // MANDATORY
  void deleteTicketType(UUID eventId, UUID ticketTypeId, boolean soft); // MANDATORY

  record EventFacts(UUID eventId, UUID organizerId, EventStatus status, String name, String venue,
                    String description, ZoneId timezone, Instant startsAt, Instant endsAt,
                    Instant saleStartsAt, Instant saleEndsAt, boolean highDemand,
                    String ticketCodePrefix, UUID imageMediaId, UUID seatMapVersionId, int rowVersion,
                    SalePhase phase) {}

  /** Pha bán tính trong SQL bằng now() (§3.2); chưa gồm tình trạng còn vé. */
  enum SalePhase { NOT_PUBLISHED, BEFORE_SALE, IN_SALE, AFTER_SALE }

  record EventChange(EventFacts before, EventFacts after) {}
  record EventStateView(UUID eventId, EventStatus status, DisplayStatus displayStatus, int rowVersion, Instant saleEndsAt) {}
}
```

`EventStatus` ∈ `DRAFT`, `PUBLISHED`, `PAUSED`, `ENDED`, `CANCELLED`; `DisplayStatus` ∈ `DRAFT`, `UPCOMING`, `ON_SALE`, `SOLD_OUT`, `SALE_CLOSED`, `PAUSED`, `ENDED`, `CANCELLED` (DR-24). `EventPatch` là `record` các trường tùy chọn đã chuyển sang UTC (§4.3); trường vắng là `null` và không ghi.

### 2.2 Lời gọi sang module khác

Chữ ký chuẩn của các API dưới đây nằm ở tài liệu của module sở hữu; bảng chỉ nêu việc tài liệu này cần.

| API | Method | Dùng ở | Tài liệu nguồn |
| --- | --- | --- | --- |
| `InventoryApi` | `createGaPools(eventId, List<GaPoolSpec>)` → số pool, số unit | Xuất bản (§6.2) | DOC-24 |
| `InventoryApi` | `createSeatUnits(eventId, ticketTypeIdsByKey, documentJson)`, `createZonePools(…)` | Xuất bản có sơ đồ (P5) | DOC-24, DOC-23 |
| `InventoryApi` | `changeGaCapacity(ticketTypeId, newCapacity)`; ném `CapacityBelowUsedException(used)` | E-35 (§5.4) | DOC-24 (đổi sức chứa, DR-30) |
| `InventoryApi` | `removeTicketType(ticketTypeId)`; ném `TicketTypeInUseException(used)` | E-36 | DOC-24 |
| `InventoryApi` | `countsByTicketType(eventId)` (`used` = `held + sold`), `countsByEvent(Collection<UUID>)` | E-24, E-26, E-35 | DOC-24 (`unit_event_idx`, DR-155) |
| `InventoryApi` | `availabilityOf(Collection<UUID> eventIds)` → `Map<UUID, AvailabilitySummary>` | `displayStatus`, `soldOut` (§3.2) | DOC-24, DR-161 |
| `OrderApi` | `markRefundPendingForEvent(eventId)` → `List<OrderRef>`; `paidOrdersOf(eventId)` → `List<OrderRef>` | Hủy (§6.4), đổi lịch (§6.5) | DOC-26 |
| `TicketApi` | `voidByEvent(eventId)` → số vé; `byOrders(Collection<UUID>)` | Hủy, đổi lịch | DOC-27 |
| `ReservationApi` | `expireAllForEvent(eventId)` → số reservation | Hủy | DOC-24 |
| `AuthApi` | `recipientsOf(userIds)`, `organizerContactEmail(organizerId)`, `organizerPublicInfo(organizerId)` | Payload email, E-07 | DOC-19 §11, DR-165 |
| `NotificationApi` | `enqueueAll(kind, payloads)` (lô 500, DR-106) | Hủy, đổi lịch | DOC-27 §4 |
| `MapApi` | `latestVersion(eventId)`, `validationSummary(seatMapVersionId)` | `publish-checks`, xuất bản (P4/P5) | DOC-23 |
| `MediaApi` | `isUsableEventImage(mediaId, organizerId)` | E-27 (`imageMediaId`) | ADR-0015 |

### 2.3 Endpoint → service

| E | Endpoint | Service · method | Transaction |
| --- | --- | --- | --- |
| E-06 | `GET /events` | `EventQueryService.list(cursor, limit)` | `readOnly` |
| E-07 | `GET /events/{eventId}` | `EventQueryService.get(eventId)` | `readOnly` |
| E-24 | `GET /organizer/events` | `StudioEventService.list(org, status, cursor, limit)` | `readOnly` |
| E-25 | `POST /organizer/events` | `StudioEventService.create(org, name)` | ngắn |
| E-26 | `GET /organizer/events/{id}` | `StudioEventService.get(org, eventId)` | `readOnly` |
| E-27 | `PATCH /organizer/events/{id}` | `EventChangeService.update(org, eventId, request)` | §6.5, timeout 60 giây |
| E-28 | `GET …/publish-checks` | `PublishCheckService.check(org, eventId)` | `readOnly` |
| E-29 | `POST …/publish` | `PublishService.publish(org, eventId, rowVersion)` | §6.2, timeout 30 giây |
| E-30…32 | `POST …/pause`, `…/resume`, `…/close-sale` | `SaleControlService.pause|resume|closeSale(org, eventId)` | một câu |
| E-33 | `POST …/cancel` | `CancelEventService.cancel(org, eventId, rowVersion)` | §6.4, timeout 60 giây |
| E-34…36 | `…/ticket-types` | `TicketTypeChangeService.create|update|delete` | §5 |
| E-39, E-13 | `POST /organizer/media`, `GET /media/{id}` | `MediaService.upload|stream` | không bọc gọi S3 (§6.8) |

Mọi method `/organizer/**` nhận `organizerId` từ principal của session, không từ request (DR-23); mọi câu SQL trên `event` của nhóm này có `AND organizer_id = :org`, không thấy dòng → 404 `NOT_FOUND` (§10).

## 3. Trạng thái lưu và trạng thái hiển thị

### 3.1 Trạng thái lưu

Máy trạng thái và bảng chuyển trạng thái của `event.status` ở DOC-14 §6.1. Bảng dưới gắn mỗi chuyển với endpoint và câu ghi; mọi câu kiểm tra số dòng, 0 dòng → 409 `EVENT_STATE_CONFLICT` kèm `status` hiện tại (DOC-35 §4).

| Từ → sang | Endpoint · bởi | Câu ghi (rút gọn) | Mục |
| --- | --- | --- | --- |
| — → `DRAFT` | E-25 | `INSERT INTO event (organizer_id, name, ticket_code_prefix, timezone) VALUES (:org, :name, 'TK', :platformTz)` | §6.7 |
| `DRAFT → PUBLISHED` | E-29 | `UPDATE event SET status='PUBLISHED', published_at=now(), seat_map_version_id=:v, row_version=row_version+1, updated_at=now() WHERE event_id=:e AND status='DRAFT'` | §6.2 |
| `PUBLISHED → PAUSED` | E-30 | `… WHERE event_id=:e AND organizer_id=:org AND status='PUBLISHED'` | §6.3 |
| `PAUSED → PUBLISHED` | E-31 | `… AND status='PAUSED' AND ends_at > now()` | §6.3 |
| `PUBLISHED`/`PAUSED → ENDED` | `EventLifecycleJob` | `… WHERE status IN ('PUBLISHED','PAUSED') AND ends_at <= now()` | §6.6 |
| `PUBLISHED`/`PAUSED → CANCELLED` | E-33 | sau `FOR UPDATE`: `… SET status='CANCELLED', cancelled_at=now()` | §6.4 |

Đóng bán sớm (E-32) không đổi `status`, chỉ đặt `sale_ends_at = now()` (DR-24, §6.3). Mọi câu ghi của studio tăng `row_version` để tab khác thấy `STALE_EVENT_VERSION` (DR-70); job `ENDED` cũng tăng `row_version`.

### 3.2 Tính `displayStatus`

`displayStatus` tính ở server cho mọi response có sự kiện (DR-24), theo hai bước: pha bán tính trong SQL bằng `now()` (DR-12: Java không gọi `Instant.now()` cho khung bán), rồi tình trạng còn vé lấy từ `InventoryApi.availabilityOf`.

**Bước 1: pha bán, trong cùng câu đọc sự kiện.**

```sql
SELECT e.*,
       CASE
         WHEN e.status <> 'PUBLISHED'       THEN 'NOT_PUBLISHED'
         WHEN now() <  e.sale_starts_at     THEN 'BEFORE_SALE'
         WHEN now() >= e.sale_ends_at       THEN 'AFTER_SALE'
         ELSE 'IN_SALE'
       END AS phase
  FROM event e
 WHERE e.event_id = :e;
```

**Bước 2: ánh xạ.**

1. `status ∈ {DRAFT, PAUSED, ENDED, CANCELLED}` → `displayStatus = status`.
2. `status = PUBLISHED` và `phase = BEFORE_SALE` → `UPCOMING`.
3. `phase = AFTER_SALE` → `SALE_CLOSED` (gồm cả sự kiện đã qua `ends_at` nhưng job chưa chạy: `sale_ends_at ≤ starts_at < ends_at` theo `event_schedule_ck`).
4. `phase = IN_SALE`: còn ít nhất một unit `AVAILABLE` của sự kiện → `ON_SALE`; không còn (kể cả khi còn unit `HELD`) → `SOLD_OUT`.

**Nguồn "còn unit `AVAILABLE`" ở P2 (DR-161).** Bitmap tình trạng chỗ (DR-62) chỉ có từ P5. Ở P2, `InventoryApi.availabilityOf(eventIds)` trả `AvailabilitySummary(Map<UUID ticketTypeId, Boolean hasAvailable>)` cho mỗi sự kiện, dựng bằng một câu cho cả trang:

```sql
SELECT p.event_id, p.ticket_type_id,
       EXISTS (SELECT 1 FROM inventory_unit u
                WHERE u.pool_id = p.pool_id AND u.status = 'AVAILABLE') AS has_available
  FROM inventory_pool p
 WHERE p.event_id = ANY(:event_ids) AND p.removed_at IS NULL;
```

`EXISTS` dừng ở dòng đầu của index một phần `unit_pool_available_idx` nên chi phí không phụ thuộc số unit; `inventory_pool.event_id` dùng index của ràng buộc `UNIQUE (event_id, zone_key)`. Kết quả cache Caffeine theo `eventId`, TTL `availability.cache-ttl` (2 giây), cùng cache mà snapshot của P5 sẽ thay thế (DOC-29); sự kiện chưa có trong cache được đọc gộp trong một câu. `soldOut` của từng loại vé ở E-07 là `!hasAvailable`. Từ P5, ghế thêm nguồn bitmap: sự kiện còn chỗ khi còn ghế trống **hoặc** còn pool có `has_available`.

Hệ quả: `ON_SALE ↔ SOLD_OUT` hiển thị trễ tối đa 2 giây (cộng `proxy_cache` 5 giây của E-06, E-07). Trạng thái chỉ là gợi ý; kết quả thật là lệnh giữ vé (UX-01 của DOC-38).

### 3.3 Ví dụ

Giờ server `now() = 2026-11-10T03:00:00Z`; sự kiện có `sale_starts_at = 2026-11-05T03:00Z`, `sale_ends_at = 2026-11-12T10:00Z`, `ends_at = 2026-11-12T15:00Z`.

| `status` | Khác biệt | Còn `AVAILABLE` | `displayStatus` |
| --- | --- | --- | --- |
| `DRAFT` | — | — | `DRAFT` |
| `PUBLISHED` | `sale_starts_at = 2026-11-11T03:00Z` | có | `UPCOMING` |
| `PUBLISHED` | — | có | `ON_SALE` |
| `PUBLISHED` | — | không (40 `HELD`, 0 `AVAILABLE`) | `SOLD_OUT` |
| `PUBLISHED` | đã `close-sale`: `sale_ends_at = 2026-11-09T07:10Z` | có | `SALE_CLOSED` |
| `PAUSED` | — | có | `PAUSED` |
| `PUBLISHED` | `ends_at = 2026-11-10T02:59Z`, job chưa chạy | có | `SALE_CLOSED` (job đặt `ENDED` trong ≤ 60 giây) |
| `CANCELLED` | — | — | `CANCELLED` |

## 4. Quy tắc trường và múi giờ

### 4.1 Bảng quy tắc

`field` trong lỗi 422 là tên trường của body (DR-89), `rule` theo bảng (DR-25, DR-83); client tra key `validation.<rule>` (DOC-40 §3.8).

| Trường (body) | Cột | Quy tắc | `rule` | Nguồn |
| --- | --- | --- | --- | --- |
| `name` | `name` | 1–120 ký tự sau `strip()` | `required`, `too_long` | DR-25 |
| `venue` | `venue` | 1–200 ký tự sau `strip()`; trống được khi `DRAFT` | `required`, `too_long` | DR-25 |
| `description` | `description` | ≤ 5.000 ký tự, văn bản thuần, giữ `\n` | `too_long` | DR-25 |
| `timezone` | `timezone` | Tên trong `ZoneId.getAvailableZoneIds()`, không nhận offset `+07:00`; khóa sau xuất bản | `invalid_timezone`, `locked_after_publish` | DR-12 |
| `startsAtLocal` | `starts_at` | `> now()` khi xuất bản và khi đổi sau xuất bản (§4.2) | `required`, `must_be_future`, `nonexistent_local_time` | DR-25, DR-162 |
| `endsAtLocal` | `ends_at` | `> starts_at`, `≤ starts_at + 72 giờ` | `required`, `must_be_after_start`, `too_long_event` | DR-25 |
| `saleStartsAtLocal` | `sale_starts_at` | `< sale_ends_at`; khóa khi `now() ≥ sale_starts_at` của sự kiện đã xuất bản | `required`, `locked_after_sale_start` | DR-25, DR-143 |
| `saleEndsAtLocal` | `sale_ends_at` | `> sale_starts_at`, `≤ starts_at`; sau xuất bản: `> now()` khi đổi, khóa khi `now() ≥ sale_ends_at` | `required`, `must_be_after_sale_start`, `after_event_start`, `must_be_future`, `locked_after_sale_end` | DR-25, DR-163 |
| `imageMediaId` | `image_media_id` | Ảnh của cùng tổ chức, `purpose = EVENT_IMAGE` | `invalid_media` | DR-38 |
| `highDemand` | `high_demand` | boolean | — | DR-57 |
| `ticketCodePrefix` | `ticket_code_prefix` | `^[A-Z]{2}$`; chỉ đổi khi `DRAFT` | `invalid_prefix`, `locked_after_publish` | DR-52 |

Hằng số (không phải cấu hình, DOC-34 §3.13): độ dài 120/200/5.000, sự kiện dài tối đa 72 giờ, tối đa 5 loại vé.

### 4.2 Khi nào kiểm quy tắc nào

| Trạng thái | Lưu (`PATCH`) | Xuất bản (E-29) |
| --- | --- | --- |
| `DRAFT` | Chỉ `name` bắt buộc; trường khác kiểm **kiểu** (độ dài, múi giờ, định dạng giờ, prefix, ảnh); quan hệ thứ tự giữa các mốc **không** kiểm | Mọi quy tắc §4.1 trên bộ giá trị đã lưu, cộng điều kiện loại vé và sơ đồ (§6.1) |
| `PUBLISHED`, `PAUSED` | Mọi quy tắc §4.1 trên bộ giá trị **sau khi sửa**; `must_be_future` chỉ kiểm trường **có trong request**; khóa `timezone`, `ticketCodePrefix`; khóa `saleStartsAtLocal` từ giờ mở bán (DR-143) và `saleEndsAtLocal` từ giờ đóng bán (DR-163) | — (409 `EVENT_STATE_CONFLICT`) |
| `ENDED`, `CANCELLED` | 409 `EVENT_STATE_CONFLICT` | 409 |

Lý do `must_be_future` chỉ kiểm trường có trong request: sự kiện đang diễn ra vẫn sửa được `description` mà không bị chặn bởi `startsAt` đã qua. Lý do khóa `saleEndsAtLocal` sau giờ đóng bán: đóng bán sớm không mở lại được (DR-24); cho dời `saleEndsAt` về tương lai sẽ mở lại bán và vượt qua quy tắc đó.

Ví dụ lỗi khi xuất bản (FR-02.2):

```json
{
  "type": "https://errors.ticket.dev/PUBLISH_PRECONDITIONS_FAILED",
  "title": "Chưa đủ điều kiện xuất bản",
  "status": 422,
  "code": "PUBLISH_PRECONDITIONS_FAILED",
  "checks": [ { "key": "sale_window", "ok": false } ],
  "errors": [ { "field": "saleEndsAtLocal", "rule": "after_event_start" } ],
  "requestId": "0199b1c2-aaaa-7bbb-8ccc-000000000001"
}
```

E-29 trả `PUBLISH_PRECONDITIONS_FAILED` kèm `checks` (mục chưa đạt) và `errors` (lỗi theo ô) để màn Studio 06 dẫn người tổ chức về đúng ô ở Studio 02.

### 4.3 Giờ địa phương ↔ UTC

Request ghi gửi `…Local` dạng `yyyy-MM-dd'T'HH:mm` cùng `timezone` của sự kiện (DR-12, DR-89); server đổi sang UTC, lưu `timestamptz`; response trả UTC (`…Z`) cùng `…Local` để form điền lại (DOC-37 §3.2 `EventTimes`).

```java
static Instant toInstant(String local, ZoneId zone) {
  LocalDateTime ldt = LocalDateTime.parse(local);           // yyyy-MM-dd'T'HH:mm
  ZoneOffsetTransition gap = zone.getRules().getTransition(ldt);
  if (gap != null && gap.isGap()) {
    throw new FieldValidationException(fieldName, "nonexistent_local_time");
  }
  return ldt.atZone(zone).toInstant();                       // giờ lặp (overlap): lấy offset sớm hơn
}
```

| `timezone` | `…Local` gửi lên | Lưu (UTC) | Ghi chú |
| --- | --- | --- | --- |
| `Asia/Ho_Chi_Minh` | `2026-11-14T20:00` | `2026-11-14T13:00:00Z` | Không có giờ mùa hè |
| `Asia/Tokyo` | `2026-11-14T20:00` | `2026-11-14T11:00:00Z` | Giao diện hiện `20:00 GMT+9` (DR-12) |
| `Europe/Berlin` | `2026-03-29T02:30` | — | 422 `nonexistent_local_time` (giờ bị nhảy qua) |
| `Europe/Berlin` | `2026-10-25T02:30` | `2026-10-25T00:30:00Z` | Giờ lặp: lấy offset sớm hơn (`+02:00`) |

**Đổi `timezone` khi `DRAFT`.** Giữ nguyên giờ trên đồng hồ địa phương (DR-12): mốc nào **không** có trong request được đọc lại thành `…Local` theo múi giờ cũ rồi đổi sang UTC theo múi giờ mới. Ví dụ `starts_at = 2026-11-14T13:00Z` theo `Asia/Ho_Chi_Minh` (20:00), `PATCH { "timezone": "Asia/Tokyo" }` → `starts_at = 2026-11-14T11:00Z` (vẫn 20:00 tại Tokyo).

`…Local` của response tính bằng `starts_at AT TIME ZONE event.timezone` trong SQL, định dạng `to_char(…, 'YYYY-MM-DD"T"HH24:MI')`.

### 4.4 Sửa có điều kiện `rowVersion`

```sql
UPDATE event
   SET name = :name, venue = :venue, description = :description, timezone = :tz,
       starts_at = :starts, ends_at = :ends, sale_starts_at = :sale_starts, sale_ends_at = :sale_ends,
       image_media_id = :media, high_demand = :hd, ticket_code_prefix = :prefix,
       row_version = row_version + 1, updated_at = now()
 WHERE event_id = :e AND organizer_id = :org AND row_version = :v
   AND status IN ('DRAFT','PUBLISHED','PAUSED')
RETURNING *;
```

Câu thật chỉ gán cột có trong `EventPatch` (SQL dựng động bằng `JdbcClient`, DOC-12 §2.5). 0 dòng → đọc lại bằng `getOwned`: không có → 404; `status` là `ENDED`/`CANCELLED` → 409 `EVENT_STATE_CONFLICT`; còn lại → 409 `STALE_EVENT_VERSION` kèm `currentRowVersion`. Quy tắc §4.1–4.2 chạy trước câu này trên bản ghép (dòng đã đọc + patch); `event_schedule_ck` là lớp bảo vệ cuối (DOC-14 §4.3).

## 5. Loại vé

### 5.1 Quy tắc (DR-26, DR-13, DR-144)

| Trường | Quy tắc | `rule` / mã |
| --- | --- | --- |
| Số lượng | ≤ 5 loại chưa xóa mỗi sự kiện | 422 `TICKET_TYPE_LIMIT_REACHED` |
| `name` | 1–60 ký tự sau `strip()`; duy nhất trong sự kiện, không phân biệt hoa thường (`ticket_type_name_uq`) | `required`, `too_long`, `duplicate` |
| `model` | `SEAT`, `ZONE`, `GA`; đổi tự do khi `DRAFT`, khóa sau xuất bản | `required`, `locked_after_publish` |
| `price` | `0` hoặc `[payment.min-amount, payment.max-amount]` = `[20.000, 100.000.000]` VND | `out_of_range` |
| `gaCapacity` | Bắt buộc khi `GA`, 1–100.000; phải vắng hoặc `null` với `SEAT`/`ZONE` | `required`, `out_of_range`, `not_allowed` |
| `color_index` | Số nhỏ nhất trong 1–5 chưa dùng bởi loại vé chưa xóa | — (server gán) |
| Thêm | Chỉ khi `DRAFT` | 409 `EVENT_STATE_CONFLICT` (DR-144) |

Ví dụ `color_index`: sự kiện có loại vé màu 1, 2, 3; xóa loại màu 2; loại mới nhận màu 2.

```sql
SELECT min(g) FROM generate_series(1, 5) g
 WHERE g NOT IN (SELECT color_index FROM ticket_type WHERE event_id = :e AND deleted_at IS NULL);
```

### 5.2 Thêm (E-34)

Một transaction ở `TicketTypeChangeService.create`:

1. `EventApi.lockForUpdate(eventId, org)`: tuần tự hóa với lệnh thêm khác, với xuất bản và với `PATCH` của cùng sự kiện. `status <> 'DRAFT'` → 409 (DR-144).
2. `SELECT count(*) FROM ticket_type WHERE event_id = :e AND deleted_at IS NULL`; = 5 → 422 `TICKET_TYPE_LIMIT_REACHED`.
3. Chọn `color_index` (§5.1), `sort_order = coalesce(max(sort_order), 0) + 1`.
4. `INSERT INTO ticket_type (…)`. Vi phạm `ticket_type_name_uq` (SQLState `23505`) → 422 `duplicate` ở `name`.

### 5.3 Sửa và xóa theo trạng thái sự kiện

| Thao tác | `DRAFT` | `PUBLISHED`, `PAUSED` | `ENDED`, `CANCELLED` |
| --- | --- | --- | --- |
| Đổi `name` | `UPDATE ticket_type` | `UPDATE ticket_type`; reservation và vé đã có giữ tên đã chụp (DR-20); `inventory_pool.name` không đổi (DR-164) | 409 |
| Đổi `price` | `UPDATE` | `UPDATE ticket_type SET price`; chỉ reservation tạo sau commit dùng giá mới (DR-30, FR-02.14) | 409 |
| Đổi `model` | `UPDATE` (đặt `ga_capacity` theo `ticket_type_ga_ck`) | 422 `locked_after_publish` | 409 |
| Đổi `gaCapacity` | `UPDATE ticket_type` | `InventoryApi.changeGaCapacity` + `UPDATE ticket_type SET ga_capacity` (§5.4) | 409 |
| Xóa | `DELETE FROM ticket_type` (xóa thật) | `InventoryApi.removeTicketType` (không còn `HELD`/`SOLD`, ngược lại 409 `TICKET_TYPE_IN_USE`) rồi `UPDATE ticket_type SET deleted_at = now()` | 409 |

Mọi thao tác ghi loại vé mở đầu bằng `EventApi.lockForUpdate` như §5.2. Loại vé đã xóa trả 404.

### 5.4 Đổi sức chứa GA khi đang bán (DR-30)

`TicketTypeChangeService.update` với `gaCapacity` mới `c'`, sức chứa cũ `c`:

1. `EventApi.lockForUpdate`.
2. `c' > c`: `InventoryApi.changeGaCapacity` chèn `Δ = c' − c` unit `AVAILABLE` vào pool và tăng `inventory_pool.capacity`; sau commit xóa cờ `soldout:pool:{poolId}` (DR-46, từ P6).
3. `c' < c`: chuyển `Δ = c − c'` unit `AVAILABLE` sang `REMOVED` bằng câu `SKIP LOCKED` của DOC-24 (đổi sức chứa); số dòng < `Δ` → rollback, 409 `CAPACITY_BELOW_USED` kèm `used = count(HELD + SOLD)`.
4. `UPDATE ticket_type SET ga_capacity = :c', updated_at = now()`.

Ví dụ (FR-02.12), mỗi ca bắt đầu từ pool 100 unit có 40 `HELD` + `SOLD`: giảm còn 30 → 409, `used = 40`; giảm còn 40 → 200, 60 unit `REMOVED`, `capacity = 40`; tăng lên 150 → thêm 50 unit `AVAILABLE`, `capacity = 150`.

## 6. Thuật toán

### 6.1 Điều kiện xuất bản (`publish-checks`, E-28)

`PublishCheckService.check` đọc sự kiện (`getOwned`), loại vé, và nếu có loại vé `SEAT`/`ZONE` thì `MapApi.latestVersion`. Mỗi mục là một `key` cố định, theo thứ tự bước studio; key i18n `publish.check.<key>` (DOC-40).

| `key` | `ok` khi | Có khi |
| --- | --- | --- |
| `name` | quy tắc `name` §4.1 đạt | luôn |
| `venue` | `venue` 1–200 ký tự | luôn |
| `timezone` | tên IANA hợp lệ | luôn |
| `starts_at` | có và `> now()` | luôn |
| `ends_at` | có, `> starts_at`, `≤ starts_at + 72 giờ` | luôn |
| `sale_window` | `sale_starts_at < sale_ends_at ≤ starts_at` | luôn |
| `has_ticket_type` | ≥ 1 loại vé chưa xóa | luôn |
| `ticket_type_capacity` | ≥ 1 loại vé có sức chứa > 0 (GA: `ga_capacity ≥ 1`; SEAT/ZONE: có ghế hoặc zone gán trong phiên bản mới nhất) | luôn |
| `seat_map_published` | `seat_map.latest_version_no ≥ 1` | có loại vé `SEAT`/`ZONE` |
| `seat_map_valid` | validate server của phiên bản mới nhất không có lỗi mức `ERROR`; `params.errorCount` | có loại vé `SEAT`/`ZONE` |

`ok` tổng = mọi mục `ok`. Bốn mục giờ dùng `now()` của database: câu đọc sự kiện trả thêm `starts_at > now() AS starts_in_future`. Endpoint trả danh sách cả khi sự kiện không còn `DRAFT` (DOC-37 E-28). Ở P2 sự kiện có loại vé `SEAT`/`ZONE` luôn có `seat_map_published = false` (sơ đồ có ở P4) nên chưa xuất bản được.

### 6.2 Xuất bản và tạo kho vé (DR-27)

`PublishService.publish(org, eventId, rowVersion)`, `@Transactional`, một transaction:

1. `SET LOCAL statement_timeout = '30s'` (`studio.publish.statement-timeout`).
2. `EventApi.lockForUpdate(eventId, org)` → `SELECT … FROM event WHERE event_id = :e AND organizer_id = :org FOR UPDATE`. Không có → 404. `status <> 'DRAFT'` → 409 `EVENT_STATE_CONFLICT`. `row_version <> :rowVersion` → 409 `STALE_EVENT_VERSION`.
3. `PublishCheckService.evaluate(facts, ticketTypes, mapVersion)` (cùng hàm của E-28, dữ liệu đọc **sau** khóa); chưa đạt → 422 `PUBLISH_PRECONDITIONS_FAILED` (rollback, không ghi gì).
4. Loại vé `GA`: `InventoryApi.createGaPools(eventId, specs)`, `specs` = `(ticketTypeId, name, gaCapacity)` của mọi loại GA. Câu mẫu (bản chuẩn ở DOC-24):

   ```sql
   WITH p AS (
     INSERT INTO inventory_pool (event_id, kind, ticket_type_id, name, capacity)
     SELECT :event_id, 'GA', t.ticket_type_id, t.name, t.capacity
       FROM unnest(:type_ids::uuid[], :names::text[], :capacities::int[]) AS t(ticket_type_id, name, capacity)
     RETURNING pool_id, event_id, ticket_type_id, capacity)
   INSERT INTO inventory_unit (event_id, ticket_type_id, pool_id)
   SELECT p.event_id, p.ticket_type_id, p.pool_id
     FROM p CROSS JOIN LATERAL generate_series(1, p.capacity);
   ```

5. Loại vé `SEAT`/`ZONE` (P5): `MapApi.latestVersion(eventId)` trả `seatMapVersionId` và `document`; `InventoryApi.createSeatUnits(eventId, ticketTypeIdsByKey, document)` chèn unit ghế bằng `INSERT … SELECT … FROM jsonb_path_query(:doc::jsonb, …)` bỏ ghế `blocked`, mang `seat_index` và nhãn (DR-40); `createZonePools` chèn pool zone rồi unit như bước 4 (DOC-07 §4.3, DOC-23).
6. `EventApi.markPublished(eventId, seatMapVersionId)` (câu ở §3.1); phải 1 dòng.
7. Commit. Response E-29: `status`, `displayStatus` (tính lại theo §3.2), `rowVersion`, `publishedAt`, `inventory { seatUnits, pools, poolUnits }`.

Kết quả kiểm tra: tổng unit = Σ `ga_capacity` + ghế không `blocked` + Σ sức chứa zone (`INV-xx` "số unit chưa `REMOVED` = sức chứa" của DOC-30 chạy được ngay sau commit).

Thời gian (kế hoạch, chưa đo; S-03 chạy trước P2-00 theo DR-152): GA 5.000 unit < 1 giây; 20.000 ghế + 80.000 unit pool < 5 giây. S-03 vượt 5 giây → bước 4–5 chuyển sang `COPY` (DR-27) và tài liệu này ghi số đo. nginx cho route này timeout 60 giây (DOC-62).

### 6.3 Tạm dừng, mở lại, đóng bán sớm (DR-24)

| E | Câu (một câu, không khóa trước) | 0 dòng |
| --- | --- | --- |
| E-30 `pause` | `UPDATE event SET status='PAUSED', row_version=row_version+1, updated_at=now() WHERE event_id=:e AND organizer_id=:org AND status='PUBLISHED' RETURNING …` | 409 `EVENT_STATE_CONFLICT` (kể cả gọi lặp khi đã `PAUSED`) |
| E-31 `resume` | `… SET status='PUBLISHED' … WHERE … AND status='PAUSED' AND ends_at > now()` | 409 |
| E-32 `close-sale` | `UPDATE event SET sale_ends_at=now(), row_version=row_version+1, updated_at=now() WHERE event_id=:e AND organizer_id=:org AND status IN ('PUBLISHED','PAUSED') AND sale_starts_at <= now() AND sale_ends_at > now() RETURNING …` | 409 (chưa tới giờ mở bán, đã đóng, trạng thái khác) |

0 dòng thì `SaleControlService` gọi `getOwned` để phân biệt 404 và 409. Tác động lên các luồng khác:

- Giữ vé mới bị chặn ở bước 2 của transaction giữ vé (`EventApi.requireOnSale`, đọc không khóa): 409 `EVENT_NOT_ON_SALE` kèm `displayStatus` (DR-41).
- Reservation đang `ACTIVE` vẫn tạo PaymentIntent và thanh toán tới `expires_at` khi `PAUSED`, `SALE_CLOSED`, `ENDED` (DR-24); xác nhận chỉ chặn `CANCELLED` (DR-28).
- `close-sale` đặt `sale_ends_at = now()` vẫn thỏa `event_schedule_ck` vì `sale_starts_at ≤ now() < sale_ends_at_cũ ≤ starts_at`.

### 6.4 Hủy sự kiện (DR-28, DR-91)

`CancelEventService.cancel(org, eventId, rowVersion)`, `@Transactional`, một transaction:

1. `SET LOCAL statement_timeout = '60s'` (`studio.cancel.statement-timeout`).
2. `EventApi.lockForUpdate(eventId, org)`; `status ∉ {PUBLISHED, PAUSED}` → 409 `EVENT_STATE_CONFLICT` (gồm `DRAFT`, FR-14.1); `row_version <> :rowVersion` → 409 `STALE_EVENT_VERSION` (hộp thoại đã báo số đơn dựa trên bản cũ).
3. `EventApi.markCancelled` → `UPDATE event SET status='CANCELLED', cancelled_at=now(), row_version=row_version+1, updated_at=now() WHERE event_id=:e AND status IN ('PUBLISHED','PAUSED')`.
4. `OrderApi.markRefundPendingForEvent` → `UPDATE orders SET status='REFUND_PENDING', refund_reason='EVENT_CANCELLED', updated_at=now() WHERE event_id=:e AND status='PAID' RETURNING order_id, user_id, amount` (`orders_event_paid_idx`).
5. `TicketApi.voidByEvent` → `UPDATE ticket SET status='VOID' WHERE event_id=:e AND status='ISSUED'` (`ticket_event_idx`).
6. `ReservationApi.expireAllForEvent` → `UPDATE reservation SET status='EXPIRING', close_reason='EVENT_CANCELLED', expiring_since=now() WHERE event_id=:e AND status='ACTIVE'` (`reservation_event_idx`). `ReleaseExpiredHoldsJob` xử lý tiếp như hết hạn: hủy PaymentIntent nếu có, trả unit, reservation `CANCELLED`, đơn `PENDING_PAYMENT → CANCELLED` (DR-91, không phải `REFUND_PENDING`).
7. `AuthApi.recipientsOf(userIds của bước 4)`, dựng payload `EMAIL_REFUND_PENDING` (`refundReason = EVENT_CANCELLED`, DOC-15 §7.2), `NotificationApi.enqueueAll` theo lô 500 (DR-106).
8. Commit. Response `{ status: CANCELLED, displayStatus: CANCELLED, rowVersion, refundPendingOrders: <bước 4>, releasingReservations: <bước 6> }`.

Unit `SOLD` giữ nguyên để lưu vết; kiểm tra bất biến về vé và unit bỏ qua sự kiện `CANCELLED` (DR-73). Hoàn tiền làm tay theo RB-01.

Ví dụ (FR-14.2): sự kiện có 3 đơn `PAID` (8 vé) và 0 reservation mở → 3 đơn `REFUND_PENDING`, 8 vé `VOID`, 3 dòng outbox, response `{"refundPendingOrders":3,"releasingReservations":0}`.

### 6.5 Sửa thông tin và email đổi lịch (DR-29, DR-106)

`EventChangeService.update(org, eventId, request)`, `@Transactional`:

1. `SET LOCAL statement_timeout = '60s'` (`studio.event-change.statement-timeout`).
2. Đọc sự kiện (`getOwned`), đổi `…Local` sang UTC (§4.3), kiểm `imageMediaId` bằng `MediaApi.isUsableEventImage`, ghép bản sau, chạy quy tắc §4.1–4.2.
3. `EventApi.update(…, expectedRowVersion, patch)` → câu §4.4; nhận `EventChange(before, after)`. Câu này lấy khóa dòng `event` **trước** bước đọc đơn ở bước 4 (§7.2).
4. Nếu `before.status <> DRAFT` và một trong `starts_at`, `ends_at`, `venue` đổi: `OrderApi.paidOrdersOf(eventId)` (`orders_event_paid_idx`), `TicketApi.byOrders(orderIds)`, `AuthApi.recipientsOf(userIds)`, `AuthApi.organizerContactEmail(org)`; dựng một payload `EMAIL_EVENT_CHANGED` cho mỗi đơn (`before`, `after`, `changed`, `tickets`, `organizerContactEmail`, DOC-15 §7.2); `NotificationApi.enqueueAll` lô 500.
5. Commit; response `{ event: OrganizerEventView, notifiedOrders: n }`.

Không gom: hai lần lưu đổi `venue` gửi hai đợt (FR-19.3). Đổi `description`, `name`, `highDemand`, ảnh: `notifiedOrders = 0`. Ví dụ: sự kiện `PUBLISHED`, 3 đơn `PAID`, `PATCH` đổi `startsAtLocal` từ `2026-11-14T20:00` sang `2026-11-21T20:00` → 3 dòng outbox, `changed = ["startsAt"]`, `notifiedOrders = 3`.

### 6.6 `EventLifecycleJob`

`@Scheduled(fixedDelayString = "${event.lifecycle.interval}")` (mặc định `PT60S`), gọi `EventService.endPastEvents()` (`@Transactional`):

```sql
UPDATE event
   SET status = 'ENDED', ended_at = now(), row_version = row_version + 1, updated_at = now()
 WHERE status IN ('PUBLISHED','PAUSED') AND ends_at <= now()
RETURNING event_id;
```

Câu dùng `event_public_idx` (một phần trên `status IN ('PUBLISHED','PAUSED')`). Idempotent: chạy lại không đổi gì; nhiều replica cùng chạy thì mỗi dòng chỉ một câu cập nhật được (khóa dòng). Log INFO `event.ended` mỗi `event_id`; metric `ticket_job_runs_total{job="event_lifecycle"}`. Reservation đang mở của sự kiện vừa `ENDED` vẫn thanh toán được tới hạn (DR-24).

### 6.7 Đọc công khai và đọc của studio

- **E-06** (`GET /events`): `SELECT … FROM event WHERE status IN ('PUBLISHED','PAUSED') AND ends_at > now() AND (starts_at, event_id) > (:k, :id) ORDER BY starts_at, event_id LIMIT :limit + 1` (`event_public_idx`); `priceFrom` = `min(price)` của `ticket_type` chưa xóa (một câu `GROUP BY event_id` cho trang); `displayStatus` theo §3.2 với `availabilityOf` gộp cả trang.
- **E-07** (`GET /events/{id}`): `DRAFT` hoặc không có → 404; `ENDED`, `CANCELLED` vẫn 200 (DR-24). `organizer` từ `AuthApi.organizerPublicInfo` (DR-165); `holdMinutes` = `reservation.hold-duration` tính ra phút; `hasSeatMap = seat_map_version_id IS NOT NULL`; `mapVersion` từ P5 (§12, DR-165).
- **E-24**: trang `event` theo `event_organizer_idx`; số `sold`, `held`, `available` của tối đa 20 sự kiện bằng một câu `InventoryApi.countsByEvent` (`unit_event_idx`, DR-155); sự kiện `DRAFT` có cả bốn số 0.
- **E-25**: `INSERT` như §3.1, `timezone = platform.timezone`, `ticket_code_prefix = 'TK'` (DR-52); response `OrganizerEventView` với `steps` toàn `TODO`.
- **E-26**: `getOwned` + `ticketTypes` + `InventoryApi.countsByTicketType` (`used` = `held + sold` của từng loại vé) + `MapApi` (`seatMapId`, từ P4); `steps` theo DR-70 (DOC-37 §7); `mapLocked = status <> 'DRAFT' AND now() >= sale_starts_at` tính trong SQL.

### 6.8 Ảnh sự kiện (DR-38)

Thiết kế lưu trữ ở ADR-0015; schema endpoint ở E-39, E-13. `MediaService.upload(org, purpose, file)` **không** có `@Transactional` bao quanh (DR-153, DOC-12 §2.4):

1. Kiểm kích thước theo `purpose` (`media.max-event-image-bytes` 2 MB, `media.max-map-image-bytes` 5 MB) → 413 `PAYLOAD_TOO_LARGE`.
2. Kiểm magic bytes (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `RIFF….WEBP`), đọc kích thước bằng `ImageIO`; sai loại, không giải mã được, cạnh > `media.max-dimension` (8.000 px) → 422 `MEDIA_INVALID` kèm `reason` ∈ `type`, `decode`, `dimensions`.
3. Sinh `media_id` (UUIDv7), tính SHA-256, `PutObject` key `media/<media_id>` kèm `Content-Type`.
4. `MediaRecordService.insert(…)` (`@Transactional` ngắn) chèn dòng `media`. Lỗi → `DeleteObject` một lần; lỗi tiếp thì object sót lại, chấp nhận (DR-38, DOC-18 §5).

Gắn ảnh vào sự kiện bằng E-27 (`imageMediaId`). Ảnh tải lên mà không gắn sau 24 giờ bị `RetentionJob` xóa (DOC-18 §5).

## 7. Transaction và đồng thời

### 7.1 Bảng transaction

| Transaction | Mở ở | Khóa | Timeout | Gọi ra ngoài |
| --- | --- | --- | --- | --- |
| Tạo nháp (E-25) | `StudioEventService.create` | — | mặc định | không |
| Sửa thông tin + fan-out (E-27) | `EventChangeService.update` | khóa dòng `event` do `UPDATE` (ngầm `FOR NO KEY UPDATE`) | `statement_timeout` 60 giây | không (email qua outbox) |
| Loại vé (E-34…36) | `TicketTypeChangeService` | `event FOR UPDATE`; unit `SKIP LOCKED` khi giảm sức chứa | mặc định | không |
| Xuất bản (E-29) | `PublishService.publish` | `event FOR UPDATE` | `statement_timeout` 30 giây | không |
| Tạm dừng, mở lại, đóng bán sớm | `SaleControlService` | khóa dòng của câu `UPDATE` | mặc định | không |
| Hủy (E-33) | `CancelEventService.cancel` | `event FOR UPDATE` | `statement_timeout` 60 giây | không |
| Kết thúc (job) | `EventService.endPastEvents` | khóa dòng của câu `UPDATE` | mặc định | không |
| Tải ảnh (E-39) | `MediaRecordService.insert` (chỉ bước chèn) | — | mặc định | `PutObject` **trước**, ngoài transaction |

### 7.2 Tranh chấp và trọng tài

| Tranh chấp | Trọng tài | Kết quả |
| --- | --- | --- |
| Hủy sự kiện ↔ webhook xác nhận | Dòng `event`: hủy giữ `FOR UPDATE`, xác nhận mở đầu bằng `FOR SHARE` (DR-28) | Hủy commit trước → xác nhận thấy `CANCELLED`, đơn `REFUND_PENDING` lý do `EVENT_CANCELLED`, không phát hành vé. Xác nhận commit trước → bước 4 của hủy bắt được đơn `PAID` vừa tạo. Không bao giờ có đơn `PAID` trên sự kiện `CANCELLED` (EV-20) |
| Hủy ↔ lệnh giữ vé | Không khóa chung: giữ vé đọc `event` không khóa (DR-41) | Lệnh giữ vé đã qua bước 2 trước khi hủy commit có thể commit sau bước 6 của hủy, để lại reservation `ACTIVE` trên sự kiện `CANCELLED` (cửa sổ ≤ 2 giây, `hold.statement-timeout`). Reservation đó hết hạn bình thường sau 10 phút; xác nhận của nó gặp `FOR SHARE` thấy `CANCELLED` nên không phát hành vé. DOC-24 quyết định có quét lại hay không (§13) |
| `PATCH` đổi lịch ↔ webhook xác nhận | Dòng `event`: `UPDATE` của `PATCH` xung đột với `FOR SHARE` | Xác nhận trước → `PATCH` chờ, đọc đơn `PAID` ở bước 4 bằng snapshot mới (READ COMMITTED) nên đơn mới nhận email đổi lịch. `PATCH` trước → xác nhận chờ, đọc sự kiện sau khi `PATCH` commit nên email vé mang giờ mới. Không đơn nào lọt email |
| Xuất bản ↔ `PATCH` cùng sự kiện | `FOR UPDATE` của xuất bản + `row_version` | `PATCH` chờ khóa rồi gặp `row_version` đã tăng → 409 `STALE_EVENT_VERSION` |
| Xuất bản ↔ thêm/xóa loại vé | `FOR UPDATE` ở cả hai | Tuần tự; loại vé thêm sau khi xuất bản commit → 409 (DR-144) |
| Hai lệnh thêm loại vé đồng thời | `FOR UPDATE` | Đếm đúng; lệnh thứ hai thấy 5 → 422 |
| Giảm sức chứa ↔ giữ vé | Unit: câu `SKIP LOCKED` của DOC-24 | Không giảm vượt số unit `AVAILABLE` thật; thiếu → 409 `CAPACITY_BELOW_USED` |
| Đổi giá ↔ giữ vé | Giữ vé đọc giá trong transaction của nó | Reservation commit trước dùng giá cũ, sau dùng giá mới (FR-02.14) |
| `pause`/`close-sale` ↔ giữ vé | Không khóa chung | Lệnh giữ vé đã qua bước 2 có thể commit ngay sau; tối đa các lệnh đang chạy (≤ 2 giây) lọt qua. Chấp nhận: trạng thái bán là cổng mềm, không phải bất biến |
| Job `ENDED` ↔ `resume` | Câu `UPDATE` có điều kiện `ends_at > now()` | `resume` sau `ends_at` nhận 0 dòng → 409 |

### 7.3 Nếu tiến trình chết

| Sau khi | Người dùng thấy | Hệ thống |
| --- | --- | --- |
| Xuất bản commit, trước response | Studio 06 báo lỗi mạng; tải lại thấy `PUBLISHED` | Kho vé đủ; bấm lại → 409 `EVENT_STATE_CONFLICT`, giao diện tải lại trạng thái |
| Xuất bản chết giữa transaction | Lỗi mạng | Rollback toàn bộ: không pool, không unit, vẫn `DRAFT` |
| Hủy commit, trước response | Lỗi mạng; tải lại thấy `CANCELLED` | Email đã nằm trong outbox; job trả vé xử lý reservation `EXPIRING` |
| `PATCH` commit, trước response | Lỗi mạng; gửi lại với `rowVersion` cũ → 409, tải lại thấy bản mới | Email đổi lịch đã xếp hàng đúng một lần |
| `PutObject` xong, chết trước khi chèn `media` | Lỗi mạng; tải ảnh lại | Object sót trong bucket, chấp nhận (DR-38) |

Mọi transaction của tài liệu này không gọi Stripe, SMTP, S3 hay Redis khi đang mở (DOC-12 §2.4); xóa cờ hết vé sau tăng sức chứa chạy sau commit.

## 8. Cấu hình

| Khóa | Kiểu | Mặc định | Biến | Profile | Mô tả |
| --- | --- | --- | --- | --- | --- |
| `event.lifecycle.interval` | duration | `PT60S` | — | mọi | Nhịp `EventLifecycleJob` (DOC-34 §3.8) |
| `studio.publish.statement-timeout` | duration | `PT30S` | — | mọi | `SET LOCAL statement_timeout` của xuất bản (DR-27); **mới** |
| `studio.cancel.statement-timeout` | duration | `PT60S` | — | mọi | Của hủy sự kiện (DR-28); **mới** |
| `studio.event-change.statement-timeout` | duration | `PT60S` | — | mọi | Của `PATCH` có fan-out (DR-106); **mới** |
| `platform.timezone` | tên IANA | `Asia/Ho_Chi_Minh` | `PLATFORM_TIMEZONE` | mọi | `timezone` của sự kiện mới (DR-12) |
| `payment.min-amount`, `payment.max-amount` | int (VND) | `20000`, `100000000` | — | mọi | Khoảng giá loại vé khác 0 (DR-13) |
| `availability.cache-ttl` | duration | `PT2S` | — | mọi | Cache tóm tắt tình trạng cho `displayStatus` (§3.2) |
| `reservation.hold-duration` | duration | `PT10M` | — | mọi | Trả ra `holdMinutes` ở E-07 |
| `media.max-event-image-bytes`, `media.max-map-image-bytes`, `media.max-dimension` | int | `2097152`, `5242880`, `8000` | — | mọi | §6.8 |
| `outbox.enqueue-batch-size` | int | `500` | — | mọi | Lô fan-out (DR-106) |

Ba khóa đánh dấu **mới** cần thêm vào DOC-34 §3.8 (§12, DR-166).

## 9. Metrics và log

| Metric | Kiểu | Label | Nơi phát | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `ticket_event_publish_seconds` | H | — | `PublishService` (bao toàn transaction, kể cả lỗi) | Thời gian xuất bản; bucket 0,1, 0,5, 1, 2, 5, 10, 30 giây. **Mới** (DR-166); kiểm chỉ tiêu < 5 giây của DR-27 ngoài S-03 |
| `ticket_job_runs_total{job="event_lifecycle"}`, `ticket_job_duration_seconds{job="event_lifecycle"}` | C, H | `job`, `result` | `common.job.JobMetrics` | Đã có (DOC-33 §4.2) |
| `ticket_outbox_enqueued_total{kind}` | C | `kind` | `NotificationApi` | Số email đổi lịch và chờ hoàn tiền xếp hàng (DR-107) |
| `ticket_http_errors_total{code}` | C | `code` | `ApiExceptionHandler` | Gồm `EVENT_STATE_CONFLICT`, `STALE_EVENT_VERSION`, `PUBLISH_PRECONDITIONS_FAILED` |

Log (JSON, DOC-33 §2), không có email hay tên người mua:

| Sự kiện log | Mức | Trường |
| --- | --- | --- |
| `event.published` | INFO | `event_id`, `organizer_id`, `pools`, `units`, `duration_ms` |
| `event.publish_slow` | WARN | như trên, khi `duration_ms > 5000` |
| `event.cancelled` | INFO | `event_id`, `refund_pending_orders`, `releasing_reservations`, `voided_tickets` |
| `event.changed` | INFO | `event_id`, `changed` (`["startsAt","venue"]`), `notified_orders` |
| `event.state_changed` | INFO | `event_id`, `from`, `to` (`pause`, `resume`, `close-sale`, `ENDED`) |

## 10. Lỗi và cách xử lý

| Tình huống | HTTP · `code` | Xử lý |
| --- | --- | --- |
| Sự kiện không tồn tại hoặc thuộc tổ chức khác (`/organizer/**`) | 404 `NOT_FOUND` | Không lộ tồn tại (DR-23, FR-02.16) |
| Sự kiện `DRAFT` ở E-07 | 404 `NOT_FOUND` | |
| Chưa có hồ sơ tổ chức | 403 `ORGANIZER_PROFILE_REQUIRED` | Bộ lọc của `/organizer/**` (DOC-19) |
| Sai quy tắc trường | 422 `VALIDATION_FAILED` `errors[{field, rule}]` | §4.1 |
| Xuất bản khi chưa đạt điều kiện | 422 `PUBLISH_PRECONDITIONS_FAILED` `checks`, `errors` | §4.2, §6.1 |
| Chuyển trạng thái không hợp lệ | 409 `EVENT_STATE_CONFLICT` `status` | §3.1 |
| `rowVersion` cũ | 409 `STALE_EVENT_VERSION` `currentRowVersion` | Hộp thoại tải lại (DR-70) |
| Loại vé thứ 6 | 422 `TICKET_TYPE_LIMIT_REACHED` | §5.2 |
| Giảm sức chứa dưới số đang dùng | 409 `CAPACITY_BELOW_USED` `used` | §5.4 |
| Xóa loại vé có unit `HELD`/`SOLD` | 409 `TICKET_TYPE_IN_USE` `used` | §5.3 |
| Quá `statement_timeout` của xuất bản hoặc hủy | 503 `OVERLOADED` + `Retry-After` | Rollback; bấm lại an toàn vì chưa đổi gì (DOC-35 lớp hạ tầng tạm thời) |
| Ảnh quá lớn, sai loại | 413 `PAYLOAD_TOO_LARGE`, 422 `MEDIA_INVALID` | §6.8 |
| Storage lỗi khi tải ảnh | 503 `OVERLOADED` | Không có dòng `media` |
| `EventLifecycleJob` lỗi một lượt | — | Log ERROR, `ticket_job_runs_total{result="error"}`; lượt sau chạy lại cùng câu |

## 11. Test bắt buộc

Tiền tố `EV-`. EV-01…27 là cam kết từ DOC-03 (cột Kiểm chứng của FR-02, FR-14, FR-18, FR-19); EV-28 trở đi bổ sung. Mức theo DOC-69 §2 (U đơn vị, I tích hợp Testcontainers, C đồng thời, E E2E).

| ID | Mức | Kịch bản | Kỳ vọng |
| --- | --- | --- | --- |
| EV-01 | I | `POST /organizer/events {"name":"Hòa nhạc Giao Mùa"}`; `name` rỗng; `name` 121 ký tự | 201, `status = DRAFT`, `timezone = Asia/Ho_Chi_Minh`, `ticketCodePrefix = TK`, `rowVersion = 0`; hai ca sau 422 `required`, `too_long` |
| EV-02 | I | Xuất bản với `startsAt = 2026-11-14T13:00Z`, `saleEndsAt = 2026-11-14T14:00Z`; `endsAt = startsAt + 73 giờ`; `startsAt` quá khứ | 422 `PUBLISH_PRECONDITIONS_FAILED`, `errors` lần lượt `after_event_start`, `too_long_event`, `must_be_future`; event vẫn `DRAFT` |
| EV-03 | I | Tab A, B cùng đọc `rowVersion = 3`; A rồi B `PATCH` kèm `rowVersion: 3` | A 200 `rowVersion = 4`; B 409 `STALE_EVENT_VERSION`, `currentRowVersion = 4` |
| EV-04 | I | `DRAFT`: `timezone = "+07:00"`; `"Asia/Tokyo"`; sau xuất bản đổi `timezone` | 422 `invalid_timezone`; 200; 422 `locked_after_publish` |
| EV-05 | I | Thêm loại vé thứ 6; tên `"vip"` khi đã có `"VIP"`; giá 10.000; giá 0 và 20.000; xóa loại màu 2 rồi thêm | 422 `TICKET_TYPE_LIMIT_REACHED`; 422 `duplicate`; 422 `out_of_range`; 201; loại mới `colorIndex = 2` |
| EV-06 | I | Sự kiện `PUBLISHED`: `PATCH` loại vé đổi `model`; `gaCapacity = 100001` | 422 `locked_after_publish`; 422 `out_of_range` |
| EV-07 | I | Xuất bản sự kiện GA 5.000 vé; xuất bản khi thiếu loại vé | 5.000 unit `AVAILABLE`, 1 pool `capacity = 5000`, event `PUBLISHED`, `published_at` đặt; ca sau 422 kèm `checks[{key:"has_ticket_type", ok:false}]`, không pool nào |
| EV-08 | U, I | Bảng §3.3 (8 dòng) với `now()` cố định bằng dữ liệu tương đối | `displayStatus` đúng từng dòng; `SOLD_OUT` khi 0 `AVAILABLE` dù còn `HELD` |
| EV-09 | I | `pause` sự kiện `ON_SALE`; giữ vé mới; reservation `ACTIVE` có sẵn gọi `confirm-free`; `resume`; giữ vé | 200 `PAUSED`; 409 `EVENT_NOT_ON_SALE`; đơn `PAID`; 200 `PUBLISHED`; 201 |
| EV-10 | I | `close-sale` sự kiện `ON_SALE`; gọi lại; gọi khi `UPCOMING` | 200, `saleEndsAt ≈ now()`, `SALE_CLOSED`; giữ vé 409; hai ca sau 409 `EVENT_STATE_CONFLICT` |
| EV-11 | I | Sự kiện `PUBLISHED` và `PAUSED` có `ends_at` đã qua; chạy job; chạy lại | Cả hai `ENDED`, `ended_at` đặt; lần hai 0 dòng |
| EV-12 | I | Mỗi ca từ pool GA 100 có 40 `HELD`+`SOLD`: giảm 30; giảm 40; tăng 150 | 409 `CAPACITY_BELOW_USED` `used = 40`; 200, 60 unit `REMOVED`; 200, thêm 50 unit `AVAILABLE`, `capacity = 150` |
| EV-13 | I | Xóa loại vé có 1 `HELD`; loại vé đã xuất bản không `HELD`/`SOLD`; loại vé của bản nháp | 409 `TICKET_TYPE_IN_USE`; 204, `deleted_at` đặt, unit `AVAILABLE → REMOVED`, `pool.removed_at` đặt; 204, dòng bị xóa thật |
| EV-14 | I | R1 giữ vé giá 100.000; đổi giá 150.000; R2 giữ | `reservation_item.unit_price` R1 = 100.000, R2 = 150.000 |
| EV-15 | I | 30 sự kiện `PUBLISHED`/`PAUSED`, 5 `ENDED`/`CANCELLED`; `GET /events` hai trang | Trang 1: 20, tăng dần `startsAt`, `nextCursor` ≠ null; trang 2: 10; `GET /events/{id}` của sự kiện `ENDED` → 200 |
| EV-16 | I | Tổ chức B `GET`/`PATCH`/`publish`/`cancel` sự kiện của A | 404 `NOT_FOUND` ở cả bốn |
| EV-17 | I | `cancel` sự kiện `DRAFT`; `PUBLISHED` | 409 `EVENT_STATE_CONFLICT`; 200 |
| EV-18 | I | Sự kiện có 3 đơn `PAID` (8 vé); hủy | 3 đơn `REFUND_PENDING` `EVENT_CANCELLED`; 8 vé `VOID`; 3 outbox `EMAIL_REFUND_PENDING`; response `{"refundPendingOrders":3,"releasingReservations":0}` |
| EV-19 | I | 2 reservation `ACTIVE` (chưa PaymentIntent); hủy; chạy job trả vé | Chuyển `EXPIRING`, `close_reason = EVENT_CANCELLED`; response `releasingReservations = 2`; sau job: reservation `CANCELLED`, đơn `CANCELLED` (DR-91), unit `AVAILABLE` |
| EV-20 | C | Hủy song song với xác nhận (`confirm-free` hoặc webhook cổng giả) của nhiều reservation, lặp 1.000 lần | 0 sự kiện `CANCELLED` còn đơn `PAID` hoặc vé `ISSUED`; mỗi đơn đã trả tiền có đúng 1 outbox `EMAIL_REFUND_PENDING` |
| EV-21 | I | `EVENT_IMAGE` 3 MB; 2 MB; `FLOOR_PLAN` 5 MB | 413 `PAYLOAD_TOO_LARGE`; 201; 201 |
| EV-22 | I | Tệp `.png` chứa văn bản; ảnh 8.001 px một cạnh; GIF | 422 `MEDIA_INVALID` `reason` = `type`, `dimensions`, `type` |
| EV-23 | I | Tải ảnh rồi `GET /media/{id}` (Testcontainers SeaweedFS); gửi `If-None-Match` | Đúng byte, `Cache-Control: public, max-age=31536000, immutable`, `ETag` = sha256; 304 |
| EV-24 | I | Chèn `media` bị lỗi (cố ý vi phạm khóa ngoại); storage đếm request | `PutObject` gọi trước, `DeleteObject` gọi đúng 1 lần; không dòng `media`; không connection DB nào mở trong lúc `PutObject` (đo bằng `ticket_connection_hold_seconds{op="studio"}`) |
| EV-25 | I | Sự kiện `PUBLISHED`, 3 đơn `PAID`; `PATCH` đổi `startsAtLocal` | 3 outbox `EMAIL_EVENT_CHANGED` có `orderId`, `locale`, `before`, `after`, `changed = ["startsAt"]`; `notifiedOrders = 3` |
| EV-26 | I | `PATCH` sự kiện `DRAFT` đổi giờ; `PATCH` sự kiện `PUBLISHED` chỉ đổi `description` | 0 dòng outbox; `notifiedOrders = 0` ở cả hai |
| EV-27 | I | Hai `PATCH` liên tiếp đổi `venue`, 3 đơn `PAID` | 6 dòng outbox |
| EV-28 | I | `PATCH` `saleStartsAtLocal` khi `now() ≥ saleStartsAt` | 422 `locked_after_sale_start` (DR-143) |
| EV-29 | I | Sau `close-sale`, `PATCH` `saleEndsAtLocal` về tương lai; trước giờ đóng, `PATCH` `saleEndsAtLocal` về quá khứ | 422 `locked_after_sale_end`; 422 `must_be_future` |
| EV-30 | I | Sự kiện đã bắt đầu (`starts_at` quá khứ, `PUBLISHED`); `PATCH` chỉ `description` | 200 (không `must_be_future`) |
| EV-31 | U | Đổi giờ địa phương theo bảng §4.3 (4 dòng) | UTC đúng; giờ bị nhảy qua → `nonexistent_local_time` |
| EV-32 | I | `DRAFT` có `starts_at = 2026-11-14T13:00Z` (`Asia/Ho_Chi_Minh`); `PATCH {"timezone":"Asia/Tokyo"}` | `starts_at = 2026-11-14T11:00Z`, `startsAtLocal` giữ `2026-11-14T20:00` |
| EV-33 | C | Xuất bản song song với 20 lệnh thêm loại vé GA | Mỗi loại vé thêm thành công đều có pool; loại vé thêm sau xuất bản nhận 409; không loại vé nào thiếu pool |
| EV-34 | I | Xuất bản với `rowVersion` cũ; xuất bản sự kiện đã `PUBLISHED` | 409 `STALE_EVENT_VERSION`; 409 `EVENT_STATE_CONFLICT`; số unit không đổi |
| EV-35 | C | `PATCH` đổi `venue` song song với 50 `confirm-free` trên sự kiện đã xuất bản | Mỗi đơn `PAID` sau cùng có đúng 1 email `EMAIL_EVENT_CHANGED` **hoặc** email vé đã mang `venue` mới (kiểm payload `EMAIL_TICKETS`); không đơn nào thiếu cả hai |
| EV-36 | I | `GET /organizer/events` với 3 sự kiện (một `DRAFT`) | `capacity = sold + held + available`; `DRAFT` có bốn số 0; một câu đếm cho cả trang (đếm câu SQL bằng `datasource-proxy`) |
| EV-37 | I | `GET /organizer/events/{id}/publish-checks` sự kiện GA đủ điều kiện; sự kiện có loại `SEAT` ở P2 | `ok = true`, 8 mục; mục `seat_map_published = false`, `ok = false` |
| EV-38 | I | `resume` sự kiện `PAUSED` đã qua `ends_at` | 409 `EVENT_STATE_CONFLICT` |
| EV-39 | I | `PATCH` với `imageMediaId` của tổ chức khác; của `FLOOR_PLAN` | 422 `invalid_media` cả hai |
| EV-40 | I | Hủy sự kiện có 5.000 đơn `PAID` | < 60 giây; 5.000 outbox ghi theo 10 lô JDBC; log `event.cancelled` đúng số |

## 12. Quyết định phát sinh khi viết tài liệu này

Claude chốt theo ủy quyền của Owner; số DR thật do sổ quyết định gán.

- **DR-161 · Nguồn `SOLD_OUT` trước khi có bitmap.** *Vấn đề:* DR-24 cần biết sự kiện còn unit `AVAILABLE`, DR-62 dựng bitmap ở P5; P2 chỉ có pool GA. *Quyết định:* `InventoryApi.availabilityOf(eventIds)` trả `hasAvailable` theo loại vé, dựng bằng một câu `EXISTS` trên `unit_pool_available_idx` cho cả trang, cache Caffeine theo `eventId` với `availability.cache-ttl`; P5 mở rộng cùng cache với bitmap ghế. *Hệ quả:* `ON_SALE ↔ SOLD_OUT` trễ ≤ 2 giây; không thêm bảng hay key Redis. *Ghi vào:* DOC-20, DOC-24, DOC-29.
- **DR-162 · Giờ địa phương không tồn tại hoặc lặp.** *Vấn đề:* DR-12 đổi `…Local` sang UTC theo `timezone` IANA; múi có giờ mùa hè có giờ bị nhảy qua và giờ lặp. *Quyết định:* giờ bị nhảy qua → 422 `rule = nonexistent_local_time`; giờ lặp lấy offset sớm hơn (mặc định `atZone`). *Hệ quả:* thêm `rule` vào DOC-35 và key `validation.nonexistent_local_time` vào DOC-40; `Asia/Ho_Chi_Minh` không bị ảnh hưởng. *Ghi vào:* DOC-20, DOC-35, DOC-40, DOC-55.
- **DR-163 · Quy tắc giờ khi sửa sự kiện đã xuất bản.** *Vấn đề:* DR-25 chỉ nói kiểm khi xuất bản; áp `must_be_future` cho mọi `PATCH` sẽ chặn sửa mô tả của sự kiện đang diễn ra, còn cho dời `saleEndsAt` sau khi đóng bán sẽ mở lại bán trái DR-24. *Quyết định:* sau xuất bản mọi quy tắc DR-25 chạy trên bộ giá trị sau khi sửa, nhưng `must_be_future` chỉ áp cho trường có trong request (`startsAtLocal`, `saleEndsAtLocal`); `saleEndsAtLocal` bị khóa khi `now() ≥ sale_ends_at` (422 `locked_after_sale_end`), tương tự DR-143. *Hệ quả:* thêm `rule` `locked_after_sale_end` vào DOC-35, DOC-37 E-27, DOC-40. *Ghi vào:* DOC-20, DOC-37, DOC-40, DOC-55.
- **DR-164 · Đổi tên loại vé sau xuất bản.** *Vấn đề:* `inventory_pool.name` của pool GA chép tên loại vé lúc xuất bản. *Quyết định:* cho đổi tên sau xuất bản; chỉ cập nhật `ticket_type.name`; `inventory_pool.name` giữ tên lúc xuất bản và không được hiển thị cho người mua (giao diện đọc `ticket_type.name`, vé đọc tên đã chụp). *Hệ quả:* không có câu ghi chéo module khi đổi tên. *Ghi vào:* DOC-20, DOC-24.
- **DR-165 · Phụ thuộc đọc của module `event`.** *Vấn đề:* E-07 trả tên và email liên hệ của tổ chức (bảng của `auth`) và `mapVersion` (bảng của `map`), nhưng `event` chỉ được phụ thuộc `inventory` (DOC-12 §2.2) và `map → event` đã có. *Quyết định:* thêm `auth` vào `allowedDependencies` của `event` (`AuthApi.organizerPublicInfo(organizerId)` trả `name`, `contactEmail` đã thay bằng email chủ khi trống); `mapVersion` lấy qua SPI `event.SeatMapVersionLookup` (package gốc `event`) do `map` cài đặt từ P5, P2 trả `null`. *Hệ quả:* đồ thị vẫn không vòng (`auth` không phụ thuộc module nghiệp vụ). *Ghi vào:* DOC-07 §3.1, DOC-12 §2.2, DOC-19 §11, DOC-20.
- **DR-166 · Khóa timeout và metric xuất bản.** *Vấn đề:* DR-27, DR-28 nêu `statement_timeout` 30 và 60 giây như hằng số; master plan §2 mục 4 yêu cầu mọi ngưỡng là cấu hình; chỉ tiêu < 5 giây của xuất bản cần đo ngoài S-03. *Quyết định:* khóa `studio.publish.statement-timeout` (`PT30S`), `studio.cancel.statement-timeout` (`PT60S`), `studio.event-change.statement-timeout` (`PT60S`); metric `ticket_event_publish_seconds` (histogram, không label). *Hệ quả:* DOC-34 §3.8 thêm ba dòng, DOC-33 §4.2 thêm một dòng. *Ghi vào:* DOC-20, DOC-33, DOC-34.

## 13. Câu hỏi còn mở

Không có câu hỏi chặn. Một điểm giao với DOC-24: reservation tạo trong cửa sổ ≤ 2 giây quanh lệnh hủy (§7.2) vẫn `ACTIVE` tới khi hết hạn; an toàn vì xác nhận chặn bằng `FOR SHARE`, còn việc trả vé sớm hơn (job quét reservation `ACTIVE` của sự kiện `CANCELLED`) do DOC-24 quyết định.
