# Màn hình: Thanh toán

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-48
> Phụ thuộc: SDD gốc §8.3–8.4, §9.1–9.2, §13.2, [Sổ quyết định](../../00-decision-register.md) (DR-10, 13, 41, 42, 43, 44, 45, 47, 50, 51, 64, 66, 67, 68, 126, 132, 136), [DOC-02](../../01-product/personas-and-journeys.md) (J-1), [DOC-03](../../01-product/requirements.md) (FR-07, FR-08, FR-09, FR-10), [DOC-04](../../01-product/use-cases.md) (UC-04, UC-05, UC-06, UC-11), [DOC-06](../../02-glossary.md), [DOC-37](../../07-api/api-endpoints.md) (E-15, E-16, E-17, E-18, E-19), [DOC-38](../ux-principles-and-ia.md) §3, §3.1, §8.1–8.3, §8.5, [DOC-39](../design-system.md) §7.1, §7.8, §7.9, §7.15, §7.17, §8, [DOC-40](../ui-states-and-copy.md) §3.5, §4.1, [DOC-41](README.md), [DOC-47](quantity-picker.md), [DOC-49](order-result.md), [DOC-82](../../06-design/flows/README.md) (FL-14, FL-15, FL-16, FL-20); canvas: artboard `06`, `M06`
> Người dùng chính: P2-10 (đơn 0 đồng, hủy giữ vé), P3 (Payment Element, task giao diện thanh toán thẻ), tác giả DOC-85 (FL-14, FL-15, FL-16) và DOC-87 (FL-20), người viết test E2E mua vé

Tài liệu mô tả màn `/checkout/:reservationId`: đồng hồ giữ vé, tóm tắt đơn, và một trong hai cách hoàn tất: **"Nhận vé" cho đơn 0 đồng (P2)** hoặc **Payment Element trả bằng thẻ (P3)**; cùng việc hủy giữ vé. Transaction xác nhận ở DOC-26/DOC-27; hủy giữ vé đường nhanh ở DR-43 và DOC-24; thời điểm phía client của thanh toán ở DR-50; lời gọi qua các tầng ở FL-14, FL-15, FL-16 (DOC-85) và FL-20 (DOC-87). Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.5).

Phần ghi **(P3)** là đặc tả cho phase 3; P2 cài phần còn lại (§7 dòng 13 nói P2 làm gì với đơn có tiền).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-1 (390 px, ưu tiên), PS-2 |
| Use case | UC-04 (thanh toán bằng thẻ, gồm nhánh đơn 0 đồng), UC-05 (nhận vé), UC-06 (hủy giữ vé), UC-11 (người mua thấy hết hạn giữ) |
| Yêu cầu | FR-07.1, FR-07.4, FR-08.1, FR-09 (P3), FR-10 |
| Hành trình | J-1 bước 5–6 |
| Luồng chi tiết | FL-14 (nhận vé đơn 0 đồng), FL-15 (hủy giữ vé), FL-16 (hết hạn thấy ở màn này), FL-20 (P3, thanh toán thẻ) |
| Quyền | Đăng nhập, chủ reservation. Reservation của người khác hoặc không tồn tại → E1 (404, DR-23). Chưa đăng nhập hoặc hết phiên → `Login` với `returnTo=/checkout/{reservationId}` (DOC-38 §8.5) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/checkout/:reservationId` | `reservationId` | UUID; sai định dạng → E1 không gọi API; E-15 404 → E1 |

Không có search param. Stripe 3-D Secure quay về `return_url = <APP_BASE_URL>/orders/<orderId>` (DR-50), không quay về màn này.

## 3. Wireframe

Desktop (≥ 1024 px), canvas `06`, biến thể "Đang giữ" của đơn có tiền (P3). Hai cột: trái là khối thanh toán, phải là đồng hồ và tóm tắt đơn:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.   (✓) Chọn vé ─ (2) Thanh toán ─ (3) Nhận vé   [vi|en] [Tài khoản]│ Stepper
├──────────────────────────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────┐ ┌─────────────────────────────┐ │
│ │ THANH TOÁN                           │ │ GIỮ VÉ CÒN          08:42   │ │ HoldTimer
│ │ THẺ THANH TOÁN                       │ └─────────────────────────────┘ │
│ │ [ Payment Element (Stripe)         ] │ ┌─────────────────────────────┐ │
│ │ [                                  ] │ │ ĐƠN CỦA BẠN                 │ │
│ │ Thông tin thẻ đi thẳng tới Stripe…   │ │ Hội thảo Kiến trúc Hệ thống │ │
│ │ [ THANH TOÁN NGAY · 900.000 ₫      ] │ │ Thứ Bảy 14.11.2026 · 20:00  │ │ Button primary
│ │ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │ │ Dreamplex, Quận 1           │ │
│ │ Đổi ý? Ghế được trả lại ngay …       │ │ Phổ thông × 2   900.000 ₫   │ │
│ │ Hủy giữ vé                           │ │ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │ │ Button text
│ └──────────────────────────────────────┘ │ TỔNG CỘNG       900.000 ₫   │ │
│                                          └─────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

Biến thể "Đơn 0 đồng" (P2), khối trái:

```text
│ ĐƠN 0 ĐỒNG                           │  nhãn mono
│ KHÔNG CẦN THANH TOÁN                 │
│ Đơn này không mất phí nên bạn không… │
│ [             NHẬN VÉ              ] │  Button primary
│ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │
│ Đổi ý? … · Hủy giữ vé                │
```

Điện thoại 390 px, canvas `M06`: `HoldTimer` dính dưới header; tóm tắt đơn thu gọn (một dòng "2 vé · 900.000 ₫", chạm để mở); khối thanh toán; nút chính rộng 100% ở cuối khối (không cố định, để không che Payment Element khi bàn phím mở):

```text
┌────────────────────────┐
│ ticket.        [vi|en] │
│ ✓ · 2 Thanh toán · 3   │
│ GIỮ VÉ CÒN     08:42   │  dính
│ ▸ 2 vé · 900.000 ₫     │  tóm tắt thu gọn
│ THANH TOÁN             │
│ [ Payment Element    ] │
│ Thông tin thẻ đi …     │
│ [THANH TOÁN NGAY · …]  │
│ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄   │
│ Đổi ý? … Hủy giữ vé    │
└────────────────────────┘
```

## 4. Vùng và component

Trang là `Checkout` (`frontend/src/features/checkout/Checkout.tsx`, DOC-41 §1.1), chunk `checkout` (Stripe.js chỉ tải khi đơn có tiền, DOC-38 §3.1).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Thanh bước | `Stepper` (§7.8) | Bước 1 xong (`checkout.steps.tickets` khi sự kiện chỉ GA, `checkout.steps.seats` khi có sơ đồ), bước 2 `checkout.steps.pay` đang ở |
| Đồng hồ | `HoldTimer` (§7.8) | `checkout.timer.label` + `mm:ss`; < 2 phút: kiểu gấp, nhãn `checkout.timer.urgent`; về 0: `checkout.timer.over` và gọi E-15 (§6). Tính theo `expiresAt` và `X-Server-Time` (DR-66), cập nhật mỗi 250 ms |
| Tóm tắt đơn | thẻ `checkout.summary.title` + sự kiện (tên, ngày giờ, địa điểm) + một dòng mỗi `items[]` + đường xé + `checkout.summary.total` | GA: `checkout.order.item.ga` (`{name}` = `ticketTypeName`, `{qty}` = `quantity`); SEAT: `checkout.order.item.seat` + tên section; ZONE: `checkout.order.item.zone`. Giá dòng = `quantity × unitPrice`; dòng 0 đồng → `checkout.summary.invite`. Tổng = `amount` của server (tổng 0 → định dạng tiền `0 ₫`, DOC-40 §2.1) |
| Khối đơn 0 đồng (P2) | nhãn `checkout.pay.free.label`, tiêu đề `checkout.pay.free.title`, đoạn `checkout.pay.free.body`, `Button primary` `checkout.pay.free.cta` | Chỉ khi `amount = 0` |
| Khối thẻ (P3) | tiêu đề `checkout.pay.title`, `checkout.pay.card.title`, Payment Element (§7.20, §8, `appearance` từ token, `locale` theo giao diện), `checkout.pay.secure` | Chỉ khi `amount > 0`. `VITE_PAYMENTS=fake`: form giả thay Payment Element, giữ nguyên nút chính và chuỗi (DR-51) |
| Nút trả tiền (P3) | `Button primary` `checkout.pay.cta` (`{amount}` định dạng tiền); 390 px dùng `checkout.pay.cta.short` khi chiều rộng < 360 px | < 2 phút: biến thể `urgent` (UXP-01); < 30 giây: vô hiệu (DR-50); khi chờ: `loading` `checkout.pay.cta.busy` |
| Thẻ bị từ chối (P3) | `Notice` `error` `checkout.pay.declined.title/.body` | Ngay trên nút trả tiền |
| Hủy giữ vé | đường xé + `checkout.pay.changeMind` + `Button text` `checkout.pay.release` | Mở `ConfirmDialog` (§7.15, DOC-38 §8.3, UXP-11). Vô hiệu khi đang trả tiền hoặc đang nhận vé |
| Hộp thoại hủy giữ | `ConfirmDialog` | Nhãn `checkout.release.dialog.label`, tiêu đề `.title`, đoạn `.body`, nút trái `.keep` (tiêu điểm khi mở, `Esc`), nút phải `urgent` `.confirm` |
| Màn hết hạn | thẻ: nhãn `checkout.expired.label`, `checkout.expired.title`, `checkout.expired.body`, `Button primary` `checkout.expired.cta` | Thay cả khối trái; tóm tắt đơn vẫn hiện (mờ) |
| Đang tải, lỗi tải | `Skeleton`, `LoadError` (§7.17) | §7 |

Khác canvas: số phút và số tiền lấy từ server (`expiresAt`, `amount`); thông báo hủy giữ dùng hộp thoại xác nhận theo UXP-11 (canvas chỉ có nút "Hủy giữ vé").

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Reservation và đơn | `GET /reservations/{reservationId}` (E-15) | Vào màn (dùng cache từ E-14 nếu vừa giữ vé); đồng hồ về 0; tab lấy lại tiêu điểm; sau lỗi `RESERVATION_NOT_ACTIVE` | Trường: `status`, `closeReason`, `expiresAt`, `amount`, `orderId`, `orderStatus`, `event`, `items[]`. **Không** hỏi theo nhịp khi `ACTIVE` |
| Kiểm tra sau hết giờ | E-15 | Khi `status = EXPIRING` | Mỗi 2 giây, tối đa 30 lần (60 giây) (DR-188) |
| Nhận vé đơn 0 đồng (P2) | `POST /orders/{orderId}/confirm-free` (E-18) | Bấm "Nhận vé" | `Idempotency-Key` sinh lúc bấm lần đầu, giữ cho mọi lần thử lại trên màn này; body `{}` |
| PaymentIntent (P3) | `POST /orders/{orderId}/payment-intent` (E-17) | Ngay khi E-15 trả `ACTIVE` và `amount > 0` (DR-50) | `Idempotency-Key` theo DR-45 (server không lưu; idempotent theo `orderId`) |
| Xác nhận thẻ (P3) | Stripe.js `confirmPayment({ redirect: "if_required", confirmParams: { return_url } })` | Bấm "Thanh toán ngay" | Không qua `api`; kết quả cuối từ webhook, màn Kết quả hỏi E-19 (DR-50) |
| Hủy giữ vé | `DELETE /reservations/{reservationId}` (E-16) | Xác nhận trong hộp thoại | `Idempotency-Key` sinh lúc xác nhận, giữ cho các lần tự thử lại |
| Giờ server | header `X-Server-Time` mọi response | `apiClient` | Trung vị 5 mẫu (DR-66) |

Không có kênh real-time. Rời trang không hủy giữ vé; quay lại `/checkout/{reservationId}` dựng lại màn từ E-15 (SDD gốc 13.2).

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-15 | Theo `status` (bảng dưới) | 404 → E1; 401 → `Login` hết phiên; mạng/5xx → `LoadError` (§7) |
| Bấm "Nhận vé" (đơn 0 đồng) | E-18 | 200 `OrderView` `PAID` → đặt vào cache `['order', orderId]`, điều hướng `/orders/{orderId}` (DOC-49) bằng `replace`. `Idempotent-Replayed: true` xử lý như 200 | 409 `RESERVATION_NOT_ACTIVE` → gọi E-15, chuyển trạng thái theo kết quả; 503/mạng → `Notice` `warning` `errors.generic` + `common.retry` **bấm tay**, gửi lại cùng khóa (DOC-38 §8.2 không tự thử lại POST này); 422 `amount_not_zero` → gọi lại E-15 (lỗi lập trình, `errors.generic`) |
| (P3) Màn có tiền vừa tải | E-17 | `clientSecret` → nạp Payment Element | 409 `PAYMENT_WINDOW_TOO_SHORT` → thẻ `checkout.pay.tooShort` + nút `checkout.expired.cta`; 409 `RESERVATION_NOT_ACTIVE` → gọi E-15; 409 `PAYMENT_ALREADY_SUCCEEDED` → `/orders/{orderId}`; 503 `PAYMENT_PROVIDER_UNAVAILABLE` → `errors.payment_provider_unavailable.*`, tự thử lại theo `Retry-After` (DR-132), đồng hồ vẫn chạy |
| (P3) Bấm "Thanh toán ngay" | Stripe `confirmPayment` | Không chuyển trang → điều hướng `/orders/{orderId}` (biến thể "Đang xác nhận"); 3-D Secure → Stripe chuyển trang rồi quay về `return_url` | Lỗi `card_error` → `checkout.pay.declined.*` (giữ màn, đồng hồ chạy, nút bật lại); lỗi khác của Stripe.js → `errors.payment_provider_unavailable.*` |
| Bấm "Hủy giữ vé" | — | Mở hộp thoại, tiêu điểm ở "Giữ vé" | — |
| Xác nhận hủy giữ | E-16 | 200 `CANCELLED` → điều hướng `/events/{eventId}/seats` (`replace`); 202 `EXPIRING` → như 200 kèm `Notice` `info` `checkout.release.pending` ở màn chọn (truyền qua state điều hướng) | 409 `PAYMENT_ALREADY_SUCCEEDED` → `Notice` `checkout.pay.release.error` rồi điều hướng `/orders/{orderId}` sau 2 giây; 503/429/mạng → tự thử lại cùng khóa (DR-132) |
| Đồng hồ về 0 | E-15 | Theo `status` | Không tự kết luận hết hạn (UXP-04) |
| Tab lấy lại tiêu điểm | E-15 | Đồng bộ trạng thái (ví dụ tab khác đã hủy) | — |

Xử lý `status` của E-15:

| `status` (`closeReason`) | Hiển thị |
| --- | --- |
| `ACTIVE`, `amount = 0` | Đang giữ, khối đơn 0 đồng |
| `ACTIVE`, `amount > 0` | Đang giữ, khối thẻ (P3) |
| `EXPIRING` | Trạng thái "Đang kiểm tra" (§7 dòng 8): hỏi E-15 mỗi 2 giây tới khi khác `EXPIRING`, tối đa 60 giây, rồi hiện "Hết hạn giữ" (DR-188) |
| `CONFIRMED` | Điều hướng `/orders/{orderId}` (`replace`) |
| `EXPIRED` | Hết hạn giữ (§7 dòng 4) |
| `CANCELLED` (`BUYER_CANCELLED`) | Thẻ `errors.reservation_not_active.*` + nút `checkout.expired.cta` (đã hủy ở tab khác) |
| `CANCELLED` (`EVENT_CANCELLED`) | Thẻ `errors.reservation_not_active.*` + nút `common.nav.toEventPage` (trang sự kiện hiện trạng thái đã hủy) |

Luồng: FL-14, FL-15, FL-16; FL-20 (P3).

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Đang giữ (`06`, `M06`) | `ACTIVE`, còn ≥ 2 phút | `HoldTimer` thường; khối thanh toán theo `amount` |
| 2 | Sắp hết giờ (`06` "Sắp hết giờ") | `ACTIVE`, còn < 2 phút | `HoldTimer` gấp (`checkout.timer.urgent`, thông báo một lần qua `aria-live="polite"`); nút trả tiền `urgent` (P3); nút "Nhận vé" giữ `primary` |
| 3 | Thẻ bị từ chối (`06` "Thẻ bị từ chối", P3) | `confirmPayment` trả `card_error` | `Notice` `error` `checkout.pay.declined.*` trên nút; đồng hồ chạy |
| 4 | Hết hạn giữ (`06` "Hết hạn") | E-15 trả `EXPIRED`, hoặc hết 60 giây kiểm tra ở dòng 8 | Thẻ `checkout.expired.*`; `HoldTimer` `checkout.timer.over`; nút chính `checkout.expired.cta` → `/events/{eventId}/seats`; "Hủy giữ vé" ẩn |
| 5 | Đơn 0 đồng (`06` "Đơn 0 đồng") | `ACTIVE`, `amount = 0` | Khối `checkout.pay.free.*`, nút chính "Nhận vé"; không tải Stripe.js |
| 6 | Đang tải (`06` "Đang tải") | Chờ E-15 > 150 ms và không có cache | `Skeleton` đồng hồ, tóm tắt, khối thanh toán; dòng ẩn `checkout.loading` |
| 7 | Lỗi tải (`06` "Lỗi tải") | E-15 lỗi mạng/5xx sau tự thử lại | `LoadError`: `common.loadError.label`, `checkout.error.title`, `checkout.error.body` ("Vé vẫn đang được giữ…"), `common.retry` |
| 8 | Đang kiểm tra sau hết giờ | E-15 trả `EXPIRING` | `HoldTimer` `00:00` `checkout.timer.over`; nút chính vô hiệu; `Notice` `info` `checkout.expiring.body`; hỏi E-15 mỗi 2 giây |
| 9 | Đang nhận vé | Chờ E-18 | Nút "Nhận vé" `loading`; "Hủy giữ vé" vô hiệu |
| 10 | Đang trả tiền (P3) | Chờ `confirmPayment` | Nút `loading` `checkout.pay.cta.busy`; Payment Element khóa; "Hủy giữ vé" vô hiệu |
| 11 | Đang tạo PaymentIntent (P3) | Chờ E-17 | Khung chờ cho Payment Element; nút trả tiền vô hiệu |
| 12 | Còn quá ít thời gian (P3) | 409 `PAYMENT_WINDOW_TOO_SHORT`, hoặc còn < 30 giây | Nút trả tiền vô hiệu; với 409: thẻ `checkout.pay.tooShort` + `checkout.expired.cta` |
| 13 | Đơn có tiền ở bản build P2 | `amount > 0` khi khối thẻ (P3) chưa cài | Khối thẻ thay bằng `Skeleton` tĩnh và nút trả tiền vô hiệu; "Hủy giữ vé" vẫn dùng được. Không thêm chuỗi người dùng: trạng thái chỉ tồn tại giữa P2 và P3 trên máy dev, M2 chỉ đòi đơn 0 đồng |
| 14 | Cổng thanh toán lỗi (P3) | 503 `PAYMENT_PROVIDER_UNAVAILABLE` | `Notice` `warning` tự thử lại; đồng hồ chạy |
| 15 | Lượt giữ đã đóng | E-15 `CANCELLED` | Thẻ `errors.reservation_not_active.*` (§6) |
| 16 | Hủy không được vì đã trả tiền | E-16 409 `PAYMENT_ALREADY_SUCCEEDED` | `Notice` `checkout.pay.release.error`, rồi sang Kết quả |
| 17 | Không quyền / không tìm thấy | E-15 404 | E1 |

Tiêu điểm khi vào: `h1` của khối thanh toán. Khi đổi sang dòng 4 hoặc 15: tiêu đề thẻ mới.

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.5, §4.1. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Thanh bước | `checkout.steps.seats`, `checkout.steps.tickets`, `checkout.steps.pay`, `checkout.steps.receive` |
| Đồng hồ | `checkout.timer.label`, `checkout.timer.urgent`, `checkout.timer.over` |
| Tóm tắt | `checkout.summary.title`, `checkout.summary.total`, `checkout.summary.invite`, `checkout.order.item.seat`, `checkout.order.item.zone`, `checkout.order.item.ga` (mới, DOC-47) |
| Đơn 0 đồng | `checkout.pay.free.label`, `checkout.pay.free.title`, `checkout.pay.free.body`, `checkout.pay.free.cta` |
| Thẻ (P3) | `checkout.pay.title`, `checkout.pay.card.title`, `checkout.pay.cta`, `checkout.pay.cta.short`, `checkout.pay.cta.busy`, `checkout.pay.secure`, `checkout.pay.declined.title`, `checkout.pay.declined.body`, `checkout.pay.tooShort` |
| Hủy giữ | `checkout.pay.changeMind`, `checkout.pay.release`, `checkout.pay.release.error` |
| Hết hạn | `checkout.expired.label`, `checkout.expired.title`, `checkout.expired.body`, `checkout.expired.cta` |
| Tải, lỗi | `checkout.loading`, `checkout.error.title`, `checkout.error.body`, `common.loadError.label`, `common.retry`, `common.loading.slow`, `errors.reservation_not_active.*`, `errors.payment_provider_unavailable.*`, `errors.generic.*`, `common.nav.toEventPage` |
| **Key mới** (thêm vào DOC-40 §3.5) | `checkout.release.dialog.label` "Hủy giữ vé" / "Release hold"; `checkout.release.dialog.title` "Trả lại vé đang giữ?" / "Give back your tickets?"; `checkout.release.dialog.body` "Vé được trả lại ngay cho người khác. Bạn có thể chọn lại nếu vẫn còn chỗ." / "Your tickets go straight back to others. You can pick again if there's still room."; `checkout.release.dialog.keep` "Giữ vé" / "Keep tickets"; `checkout.release.dialog.confirm` "Hủy giữ vé" / "Release hold"; `checkout.release.pending` "Đang trả vé, có thể mất vài giây trước khi bạn giữ vé mới." / "Releasing your tickets. It may take a few seconds before you can hold new ones."; `checkout.expiring.body` "Đang kiểm tra trạng thái giữ vé của bạn…" / "Checking the status of your hold…" |

## 9. Tiêu chí nghiệm thu

Tiền tố `CHK-` (đã kiểm tra chưa dùng ở `docs/`). Ca ghi (P3) chỉ chạy từ phase 3.

| ID | Given | When | Then |
| --- | --- | --- | --- |
| CHK-AC-01 | E-15 `ACTIVE`, `amount = 0`, còn 9:30 | Mở màn | Khối đơn 0 đồng, một nút chính "Nhận vé"; **không** tải `js.stripe.com`; đồng hồ `09:30` |
| CHK-AC-02 | Như 01 | Bấm "Nhận vé", E-18 trả 200 `PAID` | Request có `Idempotency-Key`; điều hướng `/orders/{orderId}` bằng `replace`; nút Back không quay lại màn này |
| CHK-AC-03 | E-18 lỗi mạng | Bấm "Nhận vé", rồi "Thử lại" | Lần hai gửi **cùng** `Idempotency-Key`; 200 kèm `Idempotent-Replayed` → điều hướng như 02 |
| CHK-AC-04 | E-18 trả 409 `RESERVATION_NOT_ACTIVE`, E-15 trả `EXPIRED` | Bấm "Nhận vé" | Hiện "Đã hết thời gian giữ vé" |
| CHK-AC-05 | Đồng hồ máy lệch −5 phút, `X-Server-Time` đúng | Mở màn | Đồng hồ khớp giờ server ± 1 giây (UX-03) |
| CHK-AC-06 | Còn 2:01 | Chờ 2 giây | `HoldTimer` đổi kiểu gấp với "Sắp hết giờ"; trình đọc màn hình đọc đúng một lần |
| CHK-AC-07 | Đồng hồ về 0, E-15 trả `ACTIVE` | Chờ | **Không** hiện hết hạn (UX-04) |
| CHK-AC-08 | Đồng hồ về 0, E-15 trả `EXPIRING` 3 lần rồi `EXPIRED` | Chờ | Trạng thái "Đang kiểm tra" rồi "Hết hạn giữ" sau lần trả `EXPIRED`; nút "Chọn lại chỗ" tới `/events/{eventId}/seats` |
| CHK-AC-09 | E-15 trả `EXPIRING` liên tục | Chờ 60 giây | Hiện "Hết hạn giữ"; dừng hỏi |
| CHK-AC-10 | E-15 trả `CONFIRMED` | Mở màn | Điều hướng `/orders/{orderId}` |
| CHK-AC-11 | `ACTIVE` | Bấm "Hủy giữ vé" | Hộp thoại mở, tiêu điểm ở "Giữ vé"; `Esc` đóng, không request |
| CHK-AC-12 | Hộp thoại mở | Bấm "Hủy giữ vé" trong hộp thoại, E-16 trả 200 | Request có `Idempotency-Key`; điều hướng `/events/{eventId}/seats` |
| CHK-AC-13 | E-16 trả 202 `EXPIRING` | Xác nhận hủy | Điều hướng như 12, màn chọn có `checkout.release.pending` |
| CHK-AC-14 | E-16 trả 409 `PAYMENT_ALREADY_SUCCEEDED` | Xác nhận hủy | `checkout.pay.release.error`, rồi `/orders/{orderId}` |
| CHK-AC-15 | E-15 404 (reservation của người khác) | Mở màn | E1 |
| CHK-AC-16 | E-15 401 | Mở màn | `/login?returnTo=%2Fcheckout%2F{id}&reason=session_expired` |
| CHK-AC-17 | E-15 lỗi 500 hai lần | Mở màn | `LoadError` với câu "Vé vẫn đang được giữ cho bạn và đồng hồ vẫn chạy" |
| CHK-AC-18 | (P3) `amount = 900000`, `ACTIVE` | Mở màn | E-17 gọi ngay; Payment Element nạp với `locale` hiện tại; nút "Thanh toán ngay · 900.000 ₫" |
| CHK-AC-19 | (P3) Còn 0:29 | Quan sát | Nút trả tiền vô hiệu |
| CHK-AC-20 | (P3) Thẻ test `4000 0000 0000 0002` | Bấm trả tiền | `checkout.pay.declined.*`; vẫn ở màn; đồng hồ chạy |
| CHK-AC-21 | (P3) E-17 409 `PAYMENT_WINDOW_TOO_SHORT` | Mở màn | Thẻ `checkout.pay.tooShort` + "Chọn lại chỗ" |
| CHK-AC-22 | 390 px | Mở màn | `HoldTimer` dính đầu; tóm tắt thu gọn mở được; không cuộn ngang; một nút chính |
| CHK-AC-23 | axe-core | Mọi trạng thái §7 | 0 vi phạm |

## 10. Ca kiểm thử E2E

Playwright; API thật với `PAYMENTS_MODE=fake` và `make seed`; mock cho trạng thái hiếm. Mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| CHK-01 | `free-order-get-tickets` | Giữ 2 vé GA 0 đồng (DOC-47 QTY-01), "Nhận vé" | `/orders/{orderId}` hiện 2 mã vé; Mailpit có email `tickets` (khớp P2-10) |
| CHK-02 | `release-hold` | Giữ vé, "Hủy giữ vé", xác nhận | Về `/events/{id}/seats`; giữ vé lại được ngay (không gặp `ACTIVE_RESERVATION_EXISTS`) |
| CHK-03 | `hold-expires` | Chạy với `reservation.hold-duration=PT1M`; giữ vé, chờ 70 giây | "Hết hạn giữ" xuất hiện trong ≤ 30 giây sau `expiresAt` (NFR-03) |
| CHK-04 | `checkout-reload` | Giữ vé, tải lại trang | Màn dựng lại từ E-15, đồng hồ tiếp tục đúng |
| CHK-05 | `checkout-other-user` | Tài khoản B mở URL checkout của A | E1 |
| CHK-06 | `card-pay-fake` (P3) | Đơn có tiền, `VITE_PAYMENTS=fake`, bấm trả tiền | Sang Kết quả "Đang xác nhận" rồi "Đã thanh toán" |
| CHK-07 | `card-declined` (P3, `make e2e-stripe`) | Thẻ test bị từ chối | CHK-AC-20 |
| CHK-08 | `checkout-mobile` | 390 × 844, như CHK-01 | CHK-AC-22 |

## Quyết định phát sinh khi viết tài liệu này

**DR-188 · Reservation `EXPIRING` trên màn Thanh toán: hỏi lại 60 giây rồi coi như hết hạn.** *Vấn đề:* UXP-04 chỉ cho hiện "Hết hạn giữ" khi server trả `EXPIRED`, nhưng sau khi đồng hồ về 0 server có thể trả `EXPIRING` trong lúc job trả vé đang hủy PaymentIntent (DR-42, tối đa ~5 giây + gọi Stripe; kẹt tới lease 30 giây khi Stripe lỗi), và `EXPIRING` có thể kết thúc ở `CONFIRMED` nếu tiền về (ADR-0004). *Quyết định:* khi E-15 trả `EXPIRING`, màn hiện trạng thái "Đang kiểm tra" (`checkout.expiring.body`, mọi nút vô hiệu) và hỏi E-15 mỗi 2 giây, tối đa 30 lần (60 giây); `CONFIRMED` → Kết quả; `EXPIRED`/`CANCELLED` → biến thể tương ứng; hết 60 giây vẫn `EXPIRING` → hiện "Hết hạn giữ" (vé không phát hành nếu tiền về sau đó: đơn sang `REFUND_PENDING`, người mua nhận email theo DR-44). *Hệ quả:* thêm key `checkout.expiring.body`; tối đa 30 request E-15 mỗi người mua hết hạn, nằm trong ngân sách `api_ip` (DR-55). *Ghi vào:* DOC-48, DOC-40.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-188 (mục trên).
