# Màn hình: Kết quả

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-49
> Phụ thuộc: SDD gốc §9.2, §9.4–9.5, §13.2, [Sổ quyết định](../../00-decision-register.md) (DR-10, 13, 28, 44, 50, 52, 54, 64, 66, 91, 108, 110, 132, 136, 137), [DOC-02](../../01-product/personas-and-journeys.md) (J-1), [DOC-03](../../01-product/requirements.md) (FR-09, FR-10, FR-14), [DOC-04](../../01-product/use-cases.md) (UC-04, UC-05, UC-13, UC-15), [DOC-06](../../02-glossary.md), [DOC-14](../../05-data/domain-model.md) §6.4, [DOC-27](../../06-design/tickets-and-notifications.md), [DOC-37](../../07-api/api-endpoints.md) (E-19), [DOC-38](../ux-principles-and-ia.md) §3, §8.2, §9, [DOC-39](../design-system.md) §6, §7.1, §7.9, §7.12, §7.17, [DOC-40](../ui-states-and-copy.md) §2.3, §3.6, [DOC-41](README.md), [DOC-48](checkout.md), [DOC-50](my-tickets.md), [DOC-51](emails.md), [DOC-82](../../06-design/flows/README.md) (FL-14, FL-17, FL-20, FL-21, FL-23); canvas: artboard `07`, `M07`
> Người dùng chính: P2-10 (đơn 0 đồng), P3 (thanh toán thẻ, thanh toán đến trễ), tác giả DOC-85 (FL-14, FL-17) và DOC-87 (FL-20, FL-21, FL-23)

Tài liệu mô tả màn `/orders/:orderId`: kết quả cuối của một đơn. Màn hỏi `GET /orders/{orderId}` theo nhịp DR-50 khi đơn còn chờ và hiện biến thể theo `orders.status` và `refundReason` (DR-44, DR-28). Trạng thái đơn và bảng chuyển ở DOC-14 §6.4; mã vé ở DR-52; email tương ứng ở DOC-51; lời gọi qua các tầng ở FL-14, FL-17 (DOC-85) và FL-20, FL-21, FL-23 (DOC-87). Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.6).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-1, PS-2 |
| Use case | UC-04 (kết thúc thanh toán), UC-05 (nhận vé), UC-13 (đơn của sự kiện bị hủy), UC-15 (thanh toán đến trễ) |
| Yêu cầu | FR-09 (P3), FR-10.1…10.6, FR-14.2 |
| Hành trình | J-1 bước 6–7 |
| Luồng chi tiết | FL-14 (từ đơn 0 đồng), FL-17 (xem lại đơn), FL-20/FL-21 (P3, sau khi trả bằng thẻ), FL-23 (P3, tiền về trễ) |
| Quyền | Đăng nhập, chủ đơn. Đơn của người khác hoặc không tồn tại → E1 (404, DR-23). 401 → `Login` hết phiên với `returnTo=/orders/{orderId}` (đường link trong email vé mở được sau đăng nhập) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/orders/:orderId` | `orderId` | UUID; sai định dạng → E1 không gọi API |
| `/orders/:orderId` | `redirect_status` (P3) | Stripe thêm `payment_intent`, `payment_intent_client_secret`, `redirect_status` khi quay về từ 3-D Secure (DR-50). Màn đọc `redirect_status` một lần rồi `history.replaceState` bỏ cả ba (client secret không nằm lại trong lịch sử). `failed` → §6; giá trị khác bỏ qua |

## 3. Wireframe

Desktop (≥ 1024 px), canvas `07`, biến thể "Đã thanh toán". Một cột giữa rộng tối đa 720 px:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.   (✓) Chọn vé ─ (✓) Thanh toán ─ (3) Nhận vé   [vi|en] [Tài khoản]│ Stepper
├──────────────────────────────────────────────────────────────────────────┤
│              ╔════════════════╗                                          │
│              ║ ĐÃ THANH TOÁN  ║  con dấu xoay −3°                        │
│              ╚════════════════╝                                          │
│              VÉ CỦA BẠN ĐÃ SẴN SÀNG                                      │ h1
│              Chúng tôi đã gửi 2 vé tới email của bạn. …                  │
│              2 vé Phổ thông · trả bằng thẻ         Đã trả 900.000 ₫      │
│ ┌──────────────────────────────────┐ ┌──────────────────────────────────┐│
│ │ VÉ VÀO CỬA · 1/2                 │ │ VÉ VÀO CỬA · 2/2                 ││ TicketCard
│ │ HỘI THẢO KIẾN TRÚC HỆ THỐNG      │ │ …                                ││
│ │ Thứ Bảy 14.11.2026 · 20:00       │ │                                  ││
│ │ Dreamplex, Quận 1                │ │                                  ││
│ │ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │ │                                  ││
│ │ LOẠI VÉ  Phổ thông               │ │                                  ││
│ │ MÃ VÉ    TK-4K7P-92XD            │ │                                  ││
│ └──────────────────────────────────┘ └──────────────────────────────────┘│
│              [ XEM VÉ CỦA TÔI ]   Về trang sự kiện                       │ primary + secondary
└──────────────────────────────────────────────────────────────────────────┘
```

Biến thể "Tiền về trễ" (canvas `07` "Tiền về trễ, hết vé"):

```text
│ CHƯA PHÁT HÀNH VÉ                                    │ nhãn mono
│ THANH TOÁN ĐẾN SAU KHI HẾT GIỜ GIỮ VÉ                │
│ Chúng tôi đã nhận được tiền của bạn, nhưng …         │
│ ┌──────────────────────────────────────────────────┐ │
│ │ ✓ BẠN SẼ ĐƯỢC HOÀN LẠI TOÀN BỘ   900.000 ₫       │ │ Notice success
│ │ Bạn không cần làm gì thêm. Thời gian hoàn tiền:  │ │
│ │ 5–10 ngày làm việc. Cần hỗ trợ: hotro@…          │ │
│ └──────────────────────────────────────────────────┘ │
│ [ VỀ TRANG SỰ KIỆN ]                                 │
```

Điện thoại 390 px, canvas `M07`: cùng thứ tự một cột; thẻ vé xếp dọc, rộng 100%; con dấu nhỏ 16 px chữ; nút chính rộng 100%, nút phụ là `Button text` bên dưới.

## 4. Vùng và component

Trang là `OrderResult` (`frontend/src/features/orders/OrderResult.tsx`, DOC-41 §1.1).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Thanh bước | `Stepper` (§7.8) | Chỉ hiện khi vào từ Thanh toán (state điều hướng có `fromCheckout`); vào từ email hoặc Vé của tôi thì ẩn |
| Con dấu | họa tiết con dấu (§6) | Chỉ ở biến thể `PAID` (`orders.paid.stamp`); `aria-hidden` vì `h1` đã nói |
| Tiêu đề, đoạn | `h1` + đoạn 16 px | Theo biến thể (§7) |
| Tóm tắt đơn | dòng chữ + số tiền mono | Một dòng mỗi loại vé: `orders.paid.summary` (đơn có tiền) hoặc `orders.paid.summary.free` (đơn 0 đồng), `{type}` = `ticketTypeName`, `{count}` = số vé loại đó; cạnh phải `orders.paid.paidLabel` + `amount` |
| Thẻ vé | `TicketCard` (§7.12) | Một thẻ mỗi phần tử `tickets[]`; nhãn `tickets.card.label` (`{n}/{total}`); chân thẻ theo `label`: có `row`/`seat` → `tickets.card.row`, `tickets.card.seat`, `tickets.card.type`; `{ "zone": … }` → ô khu vực thay hàng/ghế; GA `{}` (DR-108) → chỉ `tickets.card.type`. `tickets.card.code` + `code` mono `user-select: all`. Vé `VOID` mờ 60% và có nhãn `common.ticketStatus.VOID` |
| Khung hoàn tiền | `Notice` `success` (§7.9) | `orders.refund.full` + số tiền + `orders.refund.note` (hoặc `.noEmail`) với `{sla}`, `{email}` từ `support` của E-19 (DR-189) |
| Tiến trình chờ | thanh 4 px vô định, `role="status"` | Chỉ ở "Đang xác nhận"; tắt chuyển động khi `prefers-reduced-motion` |
| Nút chính | `Button primary` | `PAID`: `orders.paid.cta.tickets` → `/me/tickets`. Hoàn tiền, `REFUNDED`: `common.nav.toEventPage`. `EXPIRED`, `CANCELLED`: `checkout.expired.cta` → `/events/{eventId}/seats`. Đang xác nhận: không có nút chính |
| Nút phụ | `Button secondary` (desktop) / `text` (390 px) | `PAID`: `common.nav.toEventPage` |
| Đang tải, lỗi tải | `Skeleton`, `LoadError` (§7.17) | §7 |

Khác canvas: chỗ giữ `[THỜI GIAN HOÀN TIỀN]`, `[EMAIL HỖ TRỢ]` điền từ cấu hình (DR-44); thêm biến thể `AMOUNT_MISMATCH`, `EVENT_CANCELLED`, `REFUNDED`, `EXPIRED`, `CANCELLED` (DOC-38 §9); "trả bằng thẻ" không hiện với đơn 0 đồng.

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Đơn và vé | `GET /orders/{orderId}` (E-19) | Vào màn (dùng cache từ E-18 nếu vừa nhận vé) | Trường: `status`, `amount`, `paidAt`, `refundReason`, `lastPaymentError`, `reservationId`, `event`, `tickets[]`, `support` (DR-189) |
| Hỏi theo nhịp | E-19 | Chỉ khi `status = PENDING_PAYMENT`: mỗi 2 giây trong 60 giây đầu kể từ khi vào màn, sau đó mỗi 10 giây tới phút thứ 10 (DR-50); tạm dừng khi tab ẩn, hỏi ngay khi tab hiện | Dừng ngay khi `status` khác `PENDING_PAYMENT`. Quá 10 phút: dừng hỏi, hiện `orders.confirming.slow` (DR-190) |

Ví dụ `support` trong E-19 (chỉ khi `status = REFUND_PENDING`; `null` ở trạng thái khác):

```json
"support": { "email": "hotro@ticket.localhost", "refundSlaText": "5–10 ngày làm việc" }
```

`refundSlaText` do server dịch theo `Accept-Language` từ `support.refund-sla-text`; `email` là `support.email`, `null` khi cấu hình rỗng.

Không có kênh real-time.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-19 | Biến thể theo §7 | 404 → E1; 401 → `Login` hết phiên; mạng/5xx → tự thử lại khi đọc (DR-132), hết lượt → `LoadError` |
| Đơn chuyển từ `PENDING_PAYMENT` sang trạng thái khác trong lúc hỏi | E-19 | Thay biến thể tại chỗ, tiêu điểm vào `h1` mới, `aria-live="polite"` đọc tiêu đề một lần | — |
| (P3) Quay về với `redirect_status=failed` | E-19 | Đơn vẫn `PENDING_PAYMENT` → điều hướng `/checkout/{reservationId}` kèm state `declined` để màn Thanh toán hiện `checkout.pay.declined.*` (DOC-48) | Reservation đã đóng → Thanh toán tự hiện hết hạn |
| Bấm "Xem vé của tôi" | — | `/me/tickets` (DOC-50) | — |
| Bấm "Về trang sự kiện" | — | `/events/{eventId}` | — |
| Bấm "Chọn lại chỗ" | — | `/events/{eventId}/seats` | — |
| Chép mã vé | — | Chọn hết bằng một lần chạm (`user-select: all`); không có nút chép riêng | — |
| Bấm email hỗ trợ | — | `mailto:` | — |

Luồng: FL-14, FL-17; FL-20, FL-21, FL-23 (P3).

## 7. Trạng thái

| # | Biến thể (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Đã thanh toán (`07`, `M07`) | `status = PAID` | Con dấu, `orders.paid.title`, `orders.paid.body` (`{count}` = số vé `ISSUED`), tóm tắt, thẻ vé, nút "Xem vé của tôi" |
| 2 | Đã thanh toán, đơn 0 đồng | `PAID`, `amount = 0` | Như 1 nhưng tóm tắt dùng `orders.paid.summary.free`, số tiền `0 ₫` |
| 3 | Đang xác nhận (`07` "Đang xử lý") | `PENDING_PAYMENT` và đang trong 10 phút hỏi | Nhãn `orders.confirming.label`, `orders.confirming.title`, `orders.confirming.body`, thanh tiến trình; không có nút chính |
| 4 | Xác nhận lâu | `PENDING_PAYMENT` sau 10 phút | Như 3 nhưng không còn thanh tiến trình; thêm đoạn `orders.confirming.slow` và nút `common.retry` (hỏi E-19 một lần) |
| 5 | Tiền về trễ (`07` "Tiền về trễ, hết vé") | `REFUND_PENDING`, `refundReason = LATE_PAYMENT` | `orders.refund.label`, `orders.refund.late.title`, `orders.refund.late.body`, khung hoàn tiền; nút `common.nav.toEventPage` |
| 6 | Số tiền lệch | `REFUND_PENDING`, `AMOUNT_MISMATCH` | `orders.refund.label`, `orders.refund.mismatch.title/.body`, khung hoàn tiền |
| 7 | Sự kiện bị hủy | `REFUND_PENDING`, `EVENT_CANCELLED` | `orders.refund.cancelled.title/.body`, khung hoàn tiền, thẻ vé `VOID` (nếu có) bên dưới |
| 8 | Đã hoàn tiền | `REFUNDED` | `orders.refunded.title`, `orders.refunded.body` (`{amount}`); thẻ vé `VOID` nếu có; nút `common.nav.toEventPage` |
| 9 | Hết hạn | `EXPIRED` | `orders.expired.title`, `orders.expired.body`; nút `checkout.expired.cta` |
| 10 | Đã hủy | `CANCELLED` | `orders.cancelled.title`, `orders.cancelled.body`; nút `checkout.expired.cta` |
| 11 | Đang tải | Chờ E-19 > 150 ms, không cache | `Skeleton` tiêu đề và 2 thẻ vé; dòng ẩn `orders.loading` |
| 12 | Lỗi tải | E-19 lỗi sau tự thử lại | `LoadError`: `common.loadError.label`, `orders.error.title`, `orders.error.body`, `common.retry` |
| 13 | Không tìm thấy / không quyền | 404 | E1 |

`refundReason` lạ (ngoài ba giá trị, DR-112) → dùng biến thể 6 không có đoạn lý do (chỉ `orders.refund.label` + khung hoàn tiền).

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.6, §3.1, §2.3. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Đã thanh toán | `orders.paid.stamp`, `orders.paid.title`, `orders.paid.body`, `orders.paid.summary`, `orders.paid.paidLabel`, `orders.paid.cta.tickets`, `common.nav.toEventPage` |
| Thẻ vé | `tickets.card.label`, `tickets.card.row`, `tickets.card.seat`, `tickets.card.type`, `tickets.card.code` |
| Đang xác nhận | `orders.confirming.label`, `orders.confirming.title`, `orders.confirming.body` |
| Hoàn tiền | `orders.refund.label`, `orders.refund.late.title/.body`, `orders.refund.mismatch.title/.body`, `orders.refund.cancelled.title/.body`, `orders.refund.full`, `orders.refund.note`, `orders.refunded.title/.body` |
| Hết hạn, hủy | `orders.expired.title/.body`, `orders.cancelled.title/.body`, `checkout.expired.cta` |
| Thanh bước | `checkout.steps.*` |
| Lỗi | `common.loadError.label`, `common.retry`, `errors.generic.*` |
| **Key mới** (thêm vào DOC-40 §3.6 và §2.3) | `orders.paid.summary.free` "{count, plural, other {# vé}} {type} · miễn phí" / "{count, plural, one {# {type} ticket} other {# {type} tickets}} · free"; `orders.confirming.slow` "Việc xác nhận đang lâu hơn bình thường. Khi có kết quả, chúng tôi gửi email cho bạn; bạn cũng có thể xem lại ở Vé của tôi." / "Confirmation is taking longer than usual. We'll email you when it's done; you can also check My tickets."; `orders.refund.note.noEmail` "Bạn không cần làm gì thêm. Thời gian hoàn tiền: {sla}." / "You don't need to do anything. Refund time: {sla}."; `orders.loading` "Đang tải đơn của bạn…" / "Loading your order…"; `orders.error.title` "Không tải được đơn của bạn" / "Couldn't load your order"; `orders.error.body` "Đơn và vé của bạn vẫn an toàn và vẫn có trong email. Kiểm tra kết nối mạng rồi thử lại." / "Your order and tickets are safe and still in your email. Check your connection and try again."; `common.ticketStatus.ISSUED` "Còn hiệu lực" / "Valid", `common.ticketStatus.VOID` "Đã vô hiệu" / "Void" (DOC-40 §2.3 đã có chuỗi, chưa có tên key) |

## 9. Tiêu chí nghiệm thu

Tiền tố `ORS-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| ORS-AC-01 | E-19 `PAID`, 2 vé GA, `amount = 0` | Mở màn | Con dấu "Đã thanh toán"; "Chúng tôi đã gửi 2 vé…"; tóm tắt "2 vé Vé tiêu chuẩn · miễn phí" (không có "trả bằng thẻ"); 2 thẻ vé "1/2", "2/2" với mã `TK-XXXX-XXXX` |
| ORS-AC-02 | Thẻ vé GA (`label = {}`) | Quan sát | Chân thẻ chỉ có "Loại vé" và "Mã vé"; không có ô Hàng/Ghế |
| ORS-AC-03 | `PENDING_PAYMENT` 3 lần rồi `PAID` | Mở màn | Gọi E-19 mỗi 2 giây; biến thể đổi sang 1 sau response `PAID`; ngừng hỏi |
| ORS-AC-04 | `PENDING_PAYMENT` liên tục | Đồng hồ giả chạy 70 giây | 31 request trong 60 giây đầu, sau đó cách 10 giây |
| ORS-AC-05 | `PENDING_PAYMENT` liên tục | Đồng hồ giả chạy 10 phút 5 giây | Dừng hỏi; hiện `orders.confirming.slow` và nút "Thử lại" |
| ORS-AC-06 | Tab ẩn trong lúc hỏi | Ẩn 30 giây rồi hiện | Không request khi ẩn; một request ngay khi hiện |
| ORS-AC-07 | `REFUND_PENDING` `LATE_PAYMENT`, `support.email = "hotro@ticket.localhost"`, `refundSlaText` mặc định | Mở màn `vi` | `orders.refund.late.*`; "Bạn sẽ được hoàn lại toàn bộ"; câu chứa "5–10 ngày làm việc" và email; không còn `[THỜI GIAN HOÀN TIỀN]` (COPY-12) |
| ORS-AC-08 | Như 07 với `support.email = null` | Mở màn | Dùng `orders.refund.note.noEmail` |
| ORS-AC-09 | `REFUND_PENDING` `AMOUNT_MISMATCH` | Mở màn | `orders.refund.mismatch.*` + khung hoàn tiền |
| ORS-AC-10 | `REFUND_PENDING` `EVENT_CANCELLED`, 2 vé `VOID` | Mở màn | `orders.refund.cancelled.*`; 2 thẻ vé mờ có nhãn "Đã vô hiệu" |
| ORS-AC-11 | `REFUNDED`, `amount = 900000` | Mở màn | "Chúng tôi đã hoàn 900.000 ₫ cho đơn này." |
| ORS-AC-12 | `EXPIRED` / `CANCELLED` | Mở màn | `orders.expired.*` / `orders.cancelled.*`; nút "Chọn lại chỗ" tới `/events/{eventId}/seats` |
| ORS-AC-13 | E-19 404 | Mở màn | E1 |
| ORS-AC-14 | 401 | Mở `/orders/{id}` từ link email | `/login?returnTo=%2Forders%2F{id}&reason=session_expired`; sau đăng nhập quay lại màn |
| ORS-AC-15 | (P3) URL có `redirect_status=failed&payment_intent_client_secret=…` | Mở màn | URL không còn ba tham số Stripe; điều hướng tới `/checkout/{reservationId}` với thông báo thẻ bị từ chối |
| ORS-AC-16 | Mã vé | Chạm một lần | Cả mã được chọn |
| ORS-AC-17 | 390 px | Mở biến thể 1 | Thẻ vé xếp dọc rộng 100%; không cuộn ngang; một nút chính |
| ORS-AC-18 | axe-core | Mọi biến thể §7 | 0 vi phạm; con dấu `aria-hidden` |

## 10. Ca kiểm thử E2E

Playwright; API thật (`PAYMENTS_MODE=fake`, `make seed`) và mock; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| ORS-01 | `free-order-result` | Như CHK-01 (DOC-48) | ORS-AC-01; Mailpit có email `tickets` với cùng mã vé |
| ORS-02 | `result-from-email-link` | Mở link "Xem vé" trong email vé ở trình duyệt chưa đăng nhập | ORS-AC-14 |
| ORS-03 | `result-other-user` | Tài khoản B mở `/orders/{id}` của A | E1 |
| ORS-04 | `result-event-cancelled` | Người tổ chức hủy sự kiện có đơn của người mua (DOC-59); người mua mở đơn | ORS-AC-10 |
| ORS-05 | `result-confirming-fake` (P3) | Trả bằng cổng giả với `delayMs=5000` (DR-51) | "Đang xác nhận" rồi "Đã thanh toán" |
| ORS-06 | `result-late-payment` (P3) | Cổng giả bơm `succeeded` sau khi reservation `EXPIRED` | ORS-AC-07; Mailpit có email `refund-pending` |
| ORS-07 | `result-variants` (mock) | Lặp `AMOUNT_MISMATCH`, `REFUNDED`, `EXPIRED`, `CANCELLED`, lỗi tải | ORS-AC-09, 11, 12; §7 dòng 12 |
| ORS-08 | `result-mobile` | 390 × 844 | ORS-AC-17 |

## Quyết định phát sinh khi viết tài liệu này

**DR-189 · E-19 trả thông tin hỗ trợ cho đơn chờ hoàn tiền.** *Vấn đề:* DR-44 và DOC-40 §3.6 yêu cầu màn Kết quả điền thời hạn hoàn tiền và email hỗ trợ từ cấu hình `support.refund-sla-text`, `support.email` (DOC-34), nhưng không endpoint nào đưa hai giá trị này tới trình duyệt (chỉ email `refund-pending` có, DOC-51). *Quyết định:* E-19 (`OrderView`) thêm `support`: khi `status = REFUND_PENDING` là `{ "email": <support.email hoặc null>, "refundSlaText": <chuỗi đã dịch theo Accept-Language> }`; trạng thái khác là `null`. Không thêm biến build `VITE_*` (giá trị đổi theo môi trường chạy, không theo bản build). Khi `email = null` giao diện dùng `orders.refund.note.noEmail`. *Hệ quả:* DOC-37 E-19 thêm trường và ví dụ; `order` đọc `support.*` qua cấu hình (DOC-34 §3.1); DOC-34 §4 thêm `order` vào nơi dùng nhóm `support.*` (hiện chỉ ghi `notification`). *Ghi vào:* DOC-37, DOC-34, DOC-49.

**DR-190 · Sau 10 phút hỏi, màn Kết quả dừng và nói rõ.** *Vấn đề:* DR-50 hỏi E-19 tới 10 phút nhưng không nói màn hiện gì sau đó; đơn `PENDING_PAYMENT` quá 15 phút được `PaymentReconcileJob` xử lý (DR-49), kết quả đến người mua qua email. *Quyết định:* hết 10 phút, dừng hỏi tự động, hiện `orders.confirming.slow` và nút "Thử lại" (một lần E-19 mỗi lần bấm). *Hệ quả:* thêm key `orders.confirming.slow`; không tạo tải nền từ tab bị bỏ quên. *Ghi vào:* DOC-49, DOC-40.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-189, DR-190 (mục trên).
