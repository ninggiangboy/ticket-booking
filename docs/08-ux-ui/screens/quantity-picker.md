# Màn hình: Chọn số lượng

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-47
> Phụ thuộc: SDD gốc §5.3, §8.2, §13.2, [Sổ quyết định](../../00-decision-register.md) (DR-10, 24, 41, 42, 45, 46, 57, 58, 62, 64, 66, 67, 68, 132, 136), [DOC-02](../../01-product/personas-and-journeys.md) (J-1), [DOC-03](../../01-product/requirements.md) (FR-06, FR-07, FR-08), [DOC-04](../../01-product/use-cases.md) (UC-03, BR-01), [DOC-06](../../02-glossary.md), [DOC-37](../../07-api/api-endpoints.md) (E-07, E-09, E-14, E-16), [DOC-38](../ux-principles-and-ia.md) §2.1, §3, §5, §6.1, §7.2, §8.2, §8.5, [DOC-39](../design-system.md) §7.3, §7.5, §7.8, §7.9, [DOC-40](../ui-states-and-copy.md) §3.5, §4.1, [DOC-41](README.md) §3 (`SeatsRoute`), [DOC-43](event-detail.md), [DOC-44](login.md), [DOC-48](checkout.md), [DOC-82](../../06-design/flows/README.md) (FL-13, FL-34); canvas: artboard `05b`, `M05b`
> Người dùng chính: P2-10 (giao diện mua vé GA), P6-07 (giữ vé khi đã có lượt), tác giả DOC-85 (FL-13), người viết test E2E mua vé GA

Tài liệu mô tả màn chọn số lượng vé cho sự kiện **chỉ có loại vé `GA`** ở `/events/:eventId/seats`. Sự kiện có loại vé `SEAT`/`ZONE` dùng màn Chọn chỗ (DOC-46) ở cùng URL; `SeatsRoute` chọn màn theo `hasSeatMap` (DOC-41 §3). Transaction giữ vé, idempotency và mã lỗi ở DOC-24, DOC-25, E-14; lời gọi qua các tầng ở FL-13 (DOC-85). Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.5).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-1 (390 px, ưu tiên), PS-2; PS-4 là người tạo loại sự kiện này |
| Use case | UC-03 (giữ vé GA), UC-06 (hủy giữ để chọn lại khi gặp `ACTIVE_RESERVATION_EXISTS`) |
| Yêu cầu | FR-06.2, FR-06.4, FR-06.5, FR-06.6, FR-06.7, FR-06.9, FR-06.10, FR-08.1, FR-08.2 |
| Hành trình | J-1 bước 3–5 |
| Luồng chi tiết | FL-13; FL-34 (P6: giữ vé khi đã có lượt vào) |
| Quyền | **Xem và chọn: công khai** (khách chọn được, DR-186). **Giữ vé: cần đăng nhập**; khách bấm "Giữ vé" được đưa tới đăng nhập, lựa chọn được giữ (DR-67). Từ P6: cần lượt vào hợp lệ (DR-58), không có thì 429 `QUEUE_REQUIRED` → phòng chờ |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/events/:eventId/seats` | `eventId` | UUID; sai → E1 không gọi API; E-07 404 → E1 |

Không có search param. Lựa chọn **không** nằm trên URL; nằm ở `localStorage["tb.selection.<eventId>"].ga = { "<ticketTypeId>": <qty> }` (DR-67, DOC-38 §5). Khi `hasSeatMap = true`, `SeatsRoute` render `SeatPicker` (DOC-46) thay vì màn này.

## 3. Wireframe

Desktop (≥ 1024 px), canvas `05b`. Hai cột: danh sách loại vé trái, thẻ "Đơn của bạn" phải (dính khi cuộn):

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.   (1) Chọn vé ─ (2) Thanh toán ─ (3) Nhận vé   [vi|en] [Tài khoản]│ Stepper
├──────────────────────────────────────────────────────────────────────────┤
│ ← Hội thảo Kiến trúc Hệ thống · Thứ Bảy 14.11.2026 · 20:00               │ liên kết về DOC-43
│ CHỌN SỐ LƯỢNG VÉ                                    ┌──────────────────┐ │
│ Sự kiện này không có sơ đồ chỗ ngồi. Vé vào cửa…    │ ĐƠN CỦA BẠN      │ │
│ ┌─────────────────────────────────────────────────┐ │ 3 vé             │ │
│ │ ■ Đặt sớm        Đã bán hết           [−] 0 [+] │ │ Phổ thông × 2    │ │
│ │   300.000 ₫ mỗi vé                    (vô hiệu) │ │ Sinh viên × 1    │ │
│ ├─────────────────────────────────────────────────┤ │ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │ │
│ │ ■ Phổ thông      Còn 412 vé           [−] 2 [+] │ │ Tạm tính         │ │
│ │   450.000 ₫ mỗi vé                              │ │ 1.150.000 ₫      │ │
│ ├─────────────────────────────────────────────────┤ │ [GIỮ VÉ TRONG    │ │ Button primary
│ │ ■ Sinh viên      Còn 37 vé            [−] 1 [+] │ │  10 PHÚT       ] │ │
│ │   250.000 ₫ mỗi vé                              │ │ Vé chỉ được giữ  │ │
│ └─────────────────────────────────────────────────┘ │ sau khi bạn bấm… │ │
│                                                     └──────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

Điện thoại 390 px, canvas `M05b`: một cột loại vé; **thanh dưới cố định** gồm số vé, tạm tính và nút giữ vé; chạm vào phần số vé mở bottom sheet "Đơn của bạn" (cùng cách của Chọn chỗ, DR-69):

```text
┌────────────────────────┐
│ ticket.        [vi|en] │
│ 1 Chọn vé · 2 · 3      │
│ ← Hội thảo Kiến trúc…  │
│ CHỌN SỐ LƯỢNG VÉ       │
│ ┌────────────────────┐ │
│ │ ■ Phổ thông        │ │
│ │ 450.000 ₫ mỗi vé   │ │
│ │ Còn 412 vé [−] 2 [+]│ │
│ ├────────────────────┤ │
│ │ ■ Sinh viên  …     │ │
│ └────────────────────┘ │
├────────────────────────┤
│ 3 vé · 1.150.000 ₫  ▲  │ thanh dưới
│ [ GIỮ VÉ TRONG 10 PHÚT]│
└────────────────────────┘
```

## 4. Vùng và component

Trang là `QuantityPicker` (`frontend/src/features/checkout/QuantityPicker.tsx`, DOC-41 §1.1), được `SeatsRoute` render.

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Thanh bước | `Stepper` (§7.8) | `checkout.steps.tickets` (đang ở) → `checkout.steps.pay` → `checkout.steps.receive`; với sự kiện chỉ GA bước 1 là "Chọn vé" (UX-18) |
| Ngữ cảnh | liên kết về `/events/{eventId}` với tên sự kiện và "ngày giờ đầy đủ" | Không dịch tên sự kiện |
| Tiêu đề, dẫn | `h1` `checkout.ga.title`, đoạn `checkout.ga.intro` | |
| Dòng loại vé | `TicketTypeChip` (§7.5) + `checkout.ga.unit` (giá mỗi vé; 0 → `common.price.free`) + `checkout.ga.left` + `QuantityStepper` (§7.3) | Thứ tự theo `ticketTypes[]` của E-07 (đã sắp theo `sortOrder`). Hết vé: `checkout.ga.soldOut` thay `checkout.ga.left`, nút `+` vô hiệu, giá vẫn hiện |
| Số còn lại | chữ mono 14 px | Từ E-09 `ticketTypes[].available` (DR-185). Là gợi ý (UXP-02). Số đang chọn > số còn → dòng đổi màu chữ `--stamp-deep` + biểu tượng chấm than (không chỉ màu), không tự giảm số đã chọn |
| Đơn của bạn | thẻ: `checkout.order.title`, `checkout.order.count`, một dòng mỗi loại có `qty > 0` (`checkout.order.item.ga`), đường xé, `checkout.order.subtotal` + tiền | Rỗng → `checkout.order.empty.ga`. Tạm tính = Σ `qty × price` tính ở client chỉ để hiển thị; số tiền thật do server tính (FR-06.10) |
| Nút chính | `Button primary` `checkout.hold.cta` (`{minutes}` = `holdMinutes`) | Vô hiệu khi tổng = 0; `loading` `checkout.hold.cta.busy` khi chờ E-14 |
| Ghi chú dưới nút | chữ 14 px `checkout.hold.note.ga` | Luôn hiện (UXP-02) |
| Trần mỗi lần giữ | `Notice` `info` `checkout.limit.tooManyUnits` (`{max}` = `maxUnitsPerHold`) | **Chỉ hiện khi tổng chạm trần**; mọi nút `+` vô hiệu. Không hiện như quy tắc bán (DR-41) |
| Thông báo kết quả | `Notice` (§7.9) đặt ngay trên nút chính (desktop) hoặc trên thanh dưới (390 px) | §6, §7 |
| Thanh dưới, bottom sheet (390 px) | thanh cố định + sheet có tay kéo, `role="dialog"` khi mở | Sheet chứa nội dung thẻ "Đơn của bạn"; nút chính nằm ở thanh, không lặp trong sheet |

Khác canvas: bỏ dòng "Mỗi đơn tối đa 8 vé." (DR-41, DOC-38 §9). Canvas không vẽ trạng thái chạm trần 50, `ACTIVE_RESERVATION_EXISTS`, quá tải; các trạng thái này theo §7.

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Sự kiện, loại vé | `GET /events/{eventId}` (E-07) | Vào màn | Dùng `name`, `startsAt`, `timezone`, `displayStatus`, `holdMinutes`, `maxUnitsPerHold` (DR-187), `hasSeatMap`, `ticketTypes[]`; `staleTime` 5 giây |
| Số vé còn | `GET /events/{eventId}/availability` (E-09), phần `ticketTypes[]` | Vào màn; rồi mỗi 4 giây ± 0,5 giây khi tab hiện; dừng khi tab ẩn, hỏi ngay một lần khi tab hiện lại (DR-62) | Sự kiện GA: `seats.count = 0`, chỉ đọc `ticketTypes[].available` |
| Người dùng | `GET /me` (E-04) | Header chung | Biết khách hay đã đăng nhập khi bấm giữ vé |
| Giữ vé | `POST /events/{eventId}/reservations` (E-14) | Bấm nút chính | Body và header ở dưới |
| Hủy giữ vé đang mở | `DELETE /reservations/{reservationId}` (E-16) | Bấm "Hủy giữ vé" trong thông báo `ACTIVE_RESERVATION_EXISTS` | `Idempotency-Key` mới |

Request giữ vé (một dòng `GA` cho mỗi loại có `qty > 0`; thứ tự theo `ticketTypes[]`):

```http
POST /api/v1/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34/reservations
Idempotency-Key: 5d1c9a7e-2b44-4f0a-9c3e-7a1b2c3d4e5f
X-CSRF-Token: …

{"items":[{"type":"GA","ticketTypeId":"0199b1c2-7b01-7f10-8a22-3c4d5e6f7081","quantity":2}]}
```

**Khóa idempotency gắn với chuỗi body** (DR-45): client sinh `Idempotency-Key` (UUID v4) khi bấm nút và giữ cặp `{ key, body }` (chuỗi đã serialize) trong bộ nhớ của màn. Tự thử lại (503, 429, lỗi mạng, DR-132) gửi lại **đúng** cặp đó. Khi lựa chọn đổi (khác chuỗi body) hoặc sau một response 4xx, lần bấm sau sinh khóa mới.

Lưu lựa chọn (DOC-38 §5): ghi `localStorage["tb.selection.<eventId>"] = { "seatIds": [], "zones": {}, "ga": { "<ticketTypeId>": 2 }, "savedAt": <ms> }` mỗi lần số lượng đổi (debounce 300 ms). Khôi phục khi vào màn nếu `Date.now() − savedAt < 30 phút`, rồi xóa khóa; `ticketTypeId` không còn trong E-07 bị bỏ; số lượng vượt `maxUnitsPerHold` bị cắt về trần theo thứ tự loại vé. Giữ vé thành công (201) cũng xóa khóa. Đọc/ghi bọc `try/catch`.

Không có kênh real-time.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-07, E-09 | Danh sách loại vé, số còn; khôi phục lựa chọn | E-07 404 → E1; `hasSeatMap = true` → `SeatPicker`; lỗi → `LoadError` (§7) |
| Bấm `+` / `−` | — | Số đổi 1; tổng và tạm tính cập nhật; `role="status"` đọc số mới | `+` vô hiệu khi `available = 0` (hết vé) hoặc tổng = `maxUnitsPerHold`; `−` vô hiệu ở 0 |
| Bấm "Giữ vé trong {minutes} phút" khi **chưa đăng nhập** | — | Ghi lựa chọn ngay (không chờ debounce); điều hướng `/login?returnTo=%2Fevents%2F{eventId}%2Fseats` (DOC-44). Sau đăng nhập quay lại màn này với lựa chọn khôi phục; người mua bấm giữ vé lần nữa (không tự gửi) | — |
| Bấm "Giữ vé…" khi đã đăng nhập | E-14 | **201**: lưu response vào cache truy vấn `['reservation', reservationId]`, xóa khóa lựa chọn, tải trước chunk `checkout`, điều hướng `/checkout/{reservationId}` (DOC-48). Có `Idempotent-Replayed: true` xử lý như 201 | Bảng dưới |
| Bấm "Tiếp tục thanh toán" (`ACTIVE_RESERVATION_EXISTS`) | — | Điều hướng `/checkout/{reservationId}` lấy từ lỗi | — |
| Bấm "Hủy giữ vé" (`ACTIVE_RESERVATION_EXISTS`) | E-16 | 200/202 → ẩn thông báo, lựa chọn hiện tại giữ nguyên, người mua bấm giữ vé lại | 409 `PAYMENT_ALREADY_SUCCEEDED` → `/orders/{orderId}` (DOC-49) |
| Bấm liên kết sự kiện | — | Về `/events/{eventId}`; lựa chọn vẫn trong `localStorage` | — |

Lỗi của E-14 (DOC-40 §4.1):

| HTTP · `code` | Hiển thị | Hành động |
| --- | --- | --- |
| 409 `INSUFFICIENT_CAPACITY` (`poolId`, `requested`, `available`) | `Notice` `error` `checkout.conflict.title` + `checkout.conflict.capacity` (`{name}` = tên loại vé của pool, `{left}` = `available`, `{requested}`) | Đặt số lượng của loại đó về `available` (0 → dòng "Đã bán hết"); gọi lại E-09 ngay; **không** tự giữ lại |
| 409 `ACTIVE_RESERVATION_EXISTS` (`reservationId`) | `Notice` `warning` `checkout.conflict.active.title/.body` với `Button text` `.continue` và `.release` | Nút chính vẫn bật |
| 409 `EVENT_NOT_ON_SALE` (`displayStatus`) | `Notice` `error` `errors.event_not_on_sale.*` | Gọi lại E-07; nút chính vô hiệu nếu `displayStatus ≠ ON_SALE` (§7 dòng 9) |
| 429 `QUEUE_REQUIRED` (từ P6) | `errors.queue_required.*` | Điều hướng `/events/{eventId}/queue` (DR-58) |
| 422 `VALIDATION_FAILED` `too_many_units` | `checkout.limit.tooManyUnits` | Không xảy ra khi client đúng; nếu xảy ra (trần đổi ở server) gọi lại E-07 để lấy `maxUnitsPerHold` mới |
| 422 `VALIDATION_FAILED` khác (`wrong_model`, `unknown_reference`) | `errors.generic` | Gọi lại E-07 (loại vé đã đổi), bỏ loại không còn |
| 401 `UNAUTHENTICATED` | — | Ghi lựa chọn, chuyển `Login` biến thể hết phiên với `returnTo` (DOC-38 §8.5) |
| 503 `OVERLOADED`, 429 `RATE_LIMITED`, lỗi mạng | `Notice` `warning` `checkout.busy.*` hoặc `errors.rate_limited.*` với đếm ngược, `common.retryNow` | Tự thử lại cùng `Idempotency-Key` và body (DR-132); quá 10 lần → `Notice` `error` `errors.generic` + nút `common.retry` |
| 422 `IDEMPOTENCY_KEY_REUSED`, 400 `IDEMPOTENCY_KEY_REQUIRED` | `errors.generic` | Lỗi lập trình; khóa mới cho lần bấm sau |

Luồng: FL-13 (giữ vé GA), FL-15 (hủy giữ vé đang mở), FL-34 (P6).

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Chưa chọn (`05b`) | Vào màn, không có lựa chọn khôi phục | Mọi bộ đếm 0; `checkout.order.empty.ga`; nút chính vô hiệu |
| 2 | Đang chọn (`05b`, `M05b`) | Tổng ≥ 1 | Dòng trong "Đơn của bạn", tạm tính, nút chính bật |
| 3 | Khôi phục sau đăng nhập | Có khóa `tb.selection.<eventId>` < 30 phút | Như 2 với số đã lưu; không có thông báo riêng |
| 4 | Một loại hết vé (`05b` dòng "Đặt sớm") | `available = 0` (E-09) hoặc `soldOut = true` (E-07) | `checkout.ga.soldOut`; `+` vô hiệu; số đang chọn (nếu có) vẫn giữ và có cảnh báo dòng |
| 5 | Số chọn vượt số còn | `qty > available` | Dòng cảnh báo (chữ `--stamp-deep` + biểu tượng); không chặn bấm giữ vé (UXP-02) |
| 6 | Chạm trần mỗi lần giữ | Tổng = `maxUnitsPerHold` | `Notice` `info` `checkout.limit.tooManyUnits`; mọi `+` vô hiệu |
| 7 | Đang giữ vé | Chờ E-14 | Nút `loading` `checkout.hold.cta.busy`, bộ đếm vô hiệu |
| 8 | Không đủ vé | 409 `INSUFFICIENT_CAPACITY` | §6, số lượng đặt về `available` |
| 9 | Không mở bán | E-07 `displayStatus ≠ ON_SALE`, hoặc 409 `EVENT_NOT_ON_SALE` | `Notice` `error` `errors.event_not_on_sale.*` + `EventStatusBadge`; bộ đếm và nút chính vô hiệu; liên kết về trang sự kiện |
| 10 | Hết toàn bộ | Mọi loại `available = 0` khi `ON_SALE` | Như 4 cho mọi dòng; nút chính vô hiệu; `events.detail.soldOut.body` dưới nút. Vẫn hỏi E-09 theo nhịp, vé trả lại thì bật lại |
| 11 | Đã có reservation mở | 409 `ACTIVE_RESERVATION_EXISTS` | `Notice` `warning` có hai nút (§6) |
| 12 | Quá tải | 503/429/mạng khi giữ vé | `Notice` `warning` tự thử lại; nút giữ `loading` |
| 13 | Đang tải | Chờ E-07 > 150 ms | `Skeleton`: 3 dòng loại vé và thẻ đơn; dòng ẩn `checkout.ga.loading` |
| 14 | Lỗi tải | E-07 lỗi, hoặc E-09 lỗi ở lần đầu | `LoadError`: `common.loadError.label`, `checkout.ga.error.title`, `checkout.ga.error.body`, `common.retry`. E-09 lỗi ở các lần sau: giữ số cũ, không báo |
| 15 | Không quyền | Không áp dụng cho xem; giữ vé cần đăng nhập (§6) | — |

Tiêu điểm: vào màn → `h1`; sau lỗi 409 → `Notice` (có `role="alert"`, không cướp tiêu điểm trừ khi gửi từ bàn phím, khi đó tiêu điểm vào tiêu đề thông báo).

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.5, §3.3, §4.1. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Thanh bước | `checkout.steps.tickets`, `checkout.steps.pay`, `checkout.steps.receive` |
| Danh sách | `checkout.ga.title`, `checkout.ga.intro`, `checkout.ga.left`, `checkout.ga.soldOut`, `checkout.ga.add`, `checkout.ga.remove`, `checkout.ga.unit`, `common.price.free` |
| Đơn | `checkout.order.title`, `checkout.order.count`, `checkout.order.empty.ga`, `checkout.order.subtotal` |
| Giữ vé | `checkout.hold.cta`, `checkout.hold.cta.busy`, `checkout.hold.note.ga`, `checkout.limit.tooManyUnits` |
| Lỗi giữ vé | `checkout.conflict.title`, `checkout.conflict.capacity`, `checkout.conflict.active.title`, `.body`, `.continue`, `.release`, `checkout.busy.title`, `checkout.busy.body`, `errors.event_not_on_sale.*`, `errors.queue_required.*`, `errors.rate_limited.*`, `errors.generic.*`, `events.detail.soldOut.body`, `common.retryNow`, `common.retry` |
| Tải | `common.loadError.label`, `common.loading.slow` |
| **Key mới** (thêm vào DOC-40 §3.5) | `checkout.order.item.ga` "{name} × {qty}" / "{name} × {qty}"; `checkout.ga.loading` "Đang tải loại vé…" / "Loading ticket types…"; `checkout.ga.error.title` "Không tải được loại vé" / "Couldn't load ticket types"; `checkout.ga.error.body` "Kiểm tra kết nối mạng rồi thử lại. Số vé bạn đã chọn vẫn được nhớ." / "Check your connection and try again. Your selection is remembered."; `checkout.ga.overLeft` "Chỉ còn {count} vé" / "Only {count} left" (cảnh báo dòng ở §7 trạng thái 5) |

## 9. Tiêu chí nghiệm thu

Tiền tố `QTY-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| QTY-AC-01 | Sự kiện GA 3 loại vé, `available` 0/412/37 | Mở màn | 3 dòng; dòng 1 "Đã bán hết", `+` vô hiệu; dòng 2 "Còn 412 vé" |
| QTY-AC-02 | Chưa chọn | Quan sát | Nút chính vô hiệu; `checkout.order.empty.ga` |
| QTY-AC-03 | Chọn Phổ thông 2, Sinh viên 1 (450.000 và 250.000) | Quan sát | "3 vé"; tạm tính `1.150.000 ₫` (`vi`), `₫1,150,000` (`en`) |
| QTY-AC-04 | `maxUnitsPerHold = 50`, tổng 50 | Quan sát | Mọi `+` vô hiệu; hiện `checkout.limit.tooManyUnits` "Mỗi lần giữ vé tối đa 50 vé."; ở tổng 49 không có câu này |
| QTY-AC-05 | Đã đăng nhập, chọn 2 vé | Bấm giữ vé, E-14 trả 201 | Một request có `Idempotency-Key` UUID và body đúng §5; điều hướng `/checkout/{reservationId}`; khóa `tb.selection.<eventId>` bị xóa |
| QTY-AC-06 | Khách chọn 2 vé | Bấm giữ vé | **Không** có request E-14; khóa `localStorage` có `ga: { <id>: 2 }`; điều hướng `/login?returnTo=%2Fevents%2F{id}%2Fseats` |
| QTY-AC-07 | Khóa lựa chọn lưu 29 phút trước | Mở màn | Số lượng khôi phục, khóa bị xóa; lưu 31 phút trước → không khôi phục |
| QTY-AC-08 | E-14 trả 409 `INSUFFICIENT_CAPACITY` `requested 4`, `available 3` | Bấm giữ vé | Thông báo `checkout.conflict.capacity` với "còn 3, bạn chọn 4"; số lượng đặt về 3; không tự gửi lại |
| QTY-AC-09 | E-14 trả 409 `ACTIVE_RESERVATION_EXISTS` `reservationId R` | Bấm "Tiếp tục thanh toán" | Điều hướng `/checkout/R` |
| QTY-AC-10 | Như 09 | Bấm "Hủy giữ vé", E-16 trả 200 | Thông báo ẩn; bấm giữ vé lại gửi E-14 với `Idempotency-Key` **mới** |
| QTY-AC-11 | E-14 trả 503 `OVERLOADED` `Retry-After: 3` rồi 201 | Bấm giữ vé | Thông báo tự thử lại; lần gọi thứ hai sau 3 giây có **cùng** `Idempotency-Key` và cùng body byte-by-byte; điều hướng sau 201 |
| QTY-AC-12 | E-14 trả 409 `EVENT_NOT_ON_SALE` `displayStatus PAUSED` | Bấm giữ vé | Thông báo `errors.event_not_on_sale`; E-07 được gọi lại; nút chính vô hiệu |
| QTY-AC-13 | Mọi loại `available = 0`, sau 4 giây E-09 trả `available = 2` | Ở màn | Nút `+` của loại đó bật lại trong ≤ 5 giây |
| QTY-AC-14 | Tab ẩn 60 giây | Quan sát mạng | Không có request E-09 khi ẩn; một request ngay khi hiện lại |
| QTY-AC-15 | `hasSeatMap = true` | Mở `/events/{id}/seats` | `SeatPicker` render, không phải màn này (SCR-05) |
| QTY-AC-16 | Bundle | Grep "Mỗi đơn tối đa" | Không có (COPY-10) |
| QTY-AC-17 | 390 px | Chọn 2 vé | Thanh dưới hiện "2 vé · …" và nút giữ vé; chạm phần số mở bottom sheet; đúng một nút chính nhìn thấy |
| QTY-AC-18 | Trình đọc màn hình | Bấm `+` | Đọc số mới; nút có nhãn "Thêm một vé Phổ thông" |
| QTY-AC-19 | axe-core | Mọi trạng thái §7 | 0 vi phạm |

## 10. Ca kiểm thử E2E

Playwright; API thật trên `make seed` (sự kiện "Hội thảo Nghề Thiết Kế", DR-78) với `PAYMENTS_MODE=fake`, và mock cho lỗi; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| QTY-01 | `ga-guest-select-login-hold` | Khách mở sự kiện GA, "Mua vé", chọn 2 vé, "Giữ vé"; đăng nhập qua Mailpit ở tab mới | Tab mới về `/events/{id}/seats` với 2 vé đã chọn (UX-10); bấm giữ vé → `/checkout/{reservationId}` (khớp P2-10) |
| QTY-02 | `ga-hold-50` | Chọn 50 vé, giữ vé | 201; nút `+` vô hiệu ở 50 (FR-06.4) |
| QTY-03 | `ga-active-reservation` | Giữ vé, quay lại màn, giữ vé lần nữa | Thông báo `ACTIVE_RESERVATION_EXISTS`; "Tiếp tục thanh toán" về đúng reservation |
| QTY-04 | `ga-insufficient` | Seed pool 3 vé; tài khoản khác giữ 2; chọn 2 rồi giữ | 409 hiển thị "còn 1"; số đặt về 1 |
| QTY-05 | `ga-sold-out-type` | Seed loại "Đặt sớm" đã hết | Dòng "Đã bán hết", `+` vô hiệu |
| QTY-06 | `ga-overloaded` (mock) | 503 rồi 201 | QTY-AC-11 |
| QTY-07 | `ga-mobile` | 390 × 844, như QTY-01 | QTY-AC-17 |

## Quyết định phát sinh khi viết tài liệu này

**DR-185 · Số vé còn của sự kiện GA lấy từ E-09 ngay ở P2.** *Vấn đề:* màn này cần "Còn {count} vé" và biết loại nào hết (canvas `05b`), nhưng E-07 chỉ có `soldOut` theo loại vé, còn E-09 (`GET /events/{id}/availability`) được xếp vào P5 ở DOC-37 §2. *Quyết định:* E-09 được cài ở P2 cho phần không phụ thuộc sơ đồ (`ticketTypes[]`, `pools[]`; `seats` là `{count:0,held:"",sold:""}` và `mapVersion = null`); phần bitset ghế bổ sung ở P5 như cũ. Nhịp hỏi 4 giây ± 0,5 giây, dừng khi tab ẩn (DR-62). *Hệ quả:* DOC-37 §2 đổi cột Phase của E-09 thành "P2 (GA), P5 (ghế)"; master plan P2-08 hoặc P2-10 thêm E-09 vào đầu ra; snapshot Caffeine 2 giây có từ P2. *Ghi vào:* DOC-37, DOC-47, master plan §5 (P2-08), DOC-24.

**DR-186 · Màn chọn vé mở cho khách; đăng nhập ở bước giữ vé.** *Vấn đề:* DOC-38 §3 ghi quyền vào `/events/:eventId/seats` là "Đã đăng nhập", nhưng P2-10 và UX-10 yêu cầu "khách chọn 2 vé → đăng nhập → lựa chọn còn nguyên", và DR-67 lưu lựa chọn chưa giữ qua đăng nhập. Nếu màn bắt đăng nhập ngay thì không có gì để giữ. *Quyết định:* khách xem và chọn được ở `/events/:eventId/seats` (E-07, E-09 công khai); bấm "Giữ vé" khi chưa đăng nhập thì lưu lựa chọn và chuyển `/login?returnTo=/events/{id}/seats`. Ở P2–P5, nút "Mua vé" của DOC-43 đi thẳng tới màn chọn với mọi người; từ P6, DOC-45 quyết định có đăng nhập trước khi vào phòng chờ hay không (E-10 cần đăng nhập). Sau khi đăng nhập không tự gửi lệnh giữ vé (người mua bấm lại, UXP-02). *Hệ quả:* DOC-38 §3 cột "Quyền vào" dòng `/events/:eventId/seats` đổi thành "Khách (xem, chọn); giữ vé cần đăng nhập"; DOC-38 §2.1 nhánh "Mua vé (chưa đăng nhập) → /login" áp dụng từ P6. *Ghi vào:* DOC-38, DOC-43, DOC-46, DOC-47.

**DR-187 · E-07 trả `maxUnitsPerHold`.** *Vấn đề:* bộ đếm và giỏ ghế phải dừng ở `reservation.max-units-per-hold` (DR-41) nhưng giá trị này chỉ có ở cấu hình server; viết cứng 50 ở client thì lệch khi đổi cấu hình (như `holdMinutes` đã được trả qua E-07). *Quyết định:* E-07 thêm `maxUnitsPerHold` (int, = `reservation.max-units-per-hold`) cạnh `holdMinutes`. Client dùng giá trị này cho trần của `QuantityStepper`, giỏ ghế (DOC-46) và `{max}` của `checkout.limit.tooManyUnits`. *Hệ quả:* DOC-37 E-07 thêm trường; DOC-34 dòng `reservation.max-units-per-hold` ghi "giao diện nhận qua `maxUnitsPerHold` ở `GET /events/{id}`"; DOC-39 §7.3 thay "50" bằng giá trị này. *Ghi vào:* DOC-37, DOC-34, DOC-39, DOC-46, DOC-47.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-185…187 (mục trên).
