# Màn hình: Vé của tôi

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-50
> Phụ thuộc: SDD gốc §9.4, §13.1, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 24, 28, 44, 52, 63, 66, 91, 108, 110, 112, 119, 132, 136, 137), [DOC-02](../../01-product/personas-and-journeys.md) (J-1), [DOC-03](../../01-product/requirements.md) (FR-10, FR-14), [DOC-04](../../01-product/use-cases.md) (UC-05, UC-13, UC-15, UC-19), [DOC-06](../../02-glossary.md), [DOC-14](../../05-data/domain-model.md) §6.4, §6.5, [DOC-37](../../07-api/api-endpoints.md) (E-20, E-21), [DOC-38](../ux-principles-and-ia.md) §3, §4, §8.5, [DOC-39](../design-system.md) §7.6, §7.12, §7.14, §7.17, [DOC-40](../ui-states-and-copy.md) §2.3, §3.6, [DOC-41](README.md), [DOC-49](order-result.md), [DOC-82](../../06-design/flows/README.md) (FL-17, FL-10); canvas: artboard `08`, `M08`
> Người dùng chính: P2-10 (giao diện mua vé GA), P3 (đơn chờ hoàn tiền do thanh toán trễ), tác giả DOC-85 (FL-17), DOC-84 (FL-10, người mua thấy sự kiện bị hủy)

Tài liệu mô tả màn `/me/tickets`: mọi vé của người đăng nhập, nhóm theo sự kiện, tách "Sắp diễn ra" và "Đã diễn ra", cùng các đơn đã trả tiền nhưng **không có vé** vì đang chờ hoàn tiền (thanh toán trễ, số tiền lệch, DR-44) và vé `VOID` của sự kiện bị hủy (DR-28). Trạng thái đơn và vé ở DOC-14 §6.4–6.5; mã vé ở DR-52; lời gọi qua các tầng ở FL-17 (DOC-85). Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.6).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-1 (390 px, ưu tiên: mở màn này ở cửa ra vào), PS-2 |
| Use case | UC-05 (xem vé của tôi), UC-13 (vé `VOID`, đơn chờ hoàn tiền khi sự kiện bị hủy), UC-15 (đơn trả trễ chờ hoàn tiền), UC-19 (đơn đã hoàn tiền, người mua thấy kết quả) |
| Yêu cầu | FR-10.3 (vé xem được ở Vé của tôi), FR-14.2 |
| Hành trình | J-1 bước 7 |
| Luồng chi tiết | FL-17; FL-10 (kết quả phía người mua của việc hủy sự kiện) |
| Quyền | Đăng nhập. Chưa đăng nhập → `/login?returnTo=%2Fme%2Ftickets` (DOC-38 §3). Mục "Vé của tôi" ở menu tài khoản (DOC-38 §4) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/me/tickets` | không có | Không có tham số; cursor phân trang chỉ ở bộ nhớ; tham số lạ bị bỏ qua |

## 3. Wireframe

Desktop (≥ 1024 px), canvas `08`. Một cột rộng tối đa 960 px; mỗi sự kiện một khối, mỗi đơn một dòng đầu đơn và lưới thẻ vé 2 cột:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.                                [vi|en]   Vé của tôi  [Tài khoản]│
├──────────────────────────────────────────────────────────────────────────┤
│ VÉ CỦA TÔI                                                               │ h1
│ SẮP DIỄN RA                                                              │ h2
│ ┌──────────────────────────────────────────────────────────────────────┐ │
│ │ [ảnh] HỘI THẢO KIẾN TRÚC HỆ THỐNG  (Đang mở bán)                      │ │ khối sự kiện
│ │       Thứ Bảy 14.11.2026 · 20:00 · Dreamplex, Quận 1                  │ │
│ │       Xem trang sự kiện                                               │ │
│ │ 2 vé · đã trả 0 ₫ · Xem đơn                                           │ │ đầu đơn
│ │ ┌──────────────────────────┐ ┌──────────────────────────┐             │ │
│ │ │ VÉ VÀO CỬA · 1/2         │ │ VÉ VÀO CỬA · 2/2         │             │ │ TicketCard
│ │ │ … LOẠI VÉ  Phổ thông     │ │ …                        │             │ │
│ │ │ MÃ VÉ  TK-4K7P-92XD      │ │ MÃ VÉ  TK-8M2Q-1HZT      │             │ │
│ │ └──────────────────────────┘ └──────────────────────────┘             │ │
│ └──────────────────────────────────────────────────────────────────────┘ │
│ ┌──────────────────────────────────────────────────────────────────────┐ │
│ │ [ảnh] ĐÊM NHẠC SÂN THƯỢNG  (Đã hủy)                                   │ │
│ │ 1 vé · (Chờ hoàn tiền) · Xem đơn                                      │ │
│ │ ┌─ vé mờ, nhãn "Đã vô hiệu" ─┐                                        │ │
│ └──────────────────────────────────────────────────────────────────────┘ │
│ ĐÃ DIỄN RA                                                               │ h2
│ …                                                                        │
│                         [ Xem thêm vé ]                                  │
└──────────────────────────────────────────────────────────────────────────┘
```

Điện thoại 390 px, canvas `M08`: một cột; thẻ vé rộng 100%, xếp dọc; mã vé mono 16 px đủ lớn để nhân viên đọc tại cửa:

```text
┌────────────────────────┐
│ ticket.  [vi|en] [≡]   │
│ VÉ CỦA TÔI             │
│ SẮP DIỄN RA            │
│ HỘI THẢO KIẾN TRÚC …   │
│ Thứ Bảy 14.11 · 20:00  │
│ 2 vé · đã trả · Xem đơn│
│ ┌────────────────────┐ │
│ │ VÉ VÀO CỬA · 1/2   │ │
│ │ …                  │ │
│ │ MÃ VÉ TK-4K7P-92XD │ │
│ └────────────────────┘ │
│ ┌────────────────────┐ │
│ │ VÉ VÀO CỬA · 2/2   │ │
│ └────────────────────┘ │
└────────────────────────┘
```

## 4. Vùng và component

Trang là `MyTickets` (`frontend/src/features/orders/MyTickets.tsx`, DOC-41 §1.1).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Tiêu đề | `h1` `tickets.title` | |
| Nhóm thời gian | `h2` `tickets.upcoming`, `tickets.past` | "Sắp diễn ra": `event.endsAt > now` (giờ server, DR-66), sắp theo `event.startsAt` tăng dần. "Đã diễn ra": còn lại, sắp giảm dần. Nhóm rỗng thì ẩn tiêu đề của nhóm đó |
| Khối sự kiện | ảnh nhỏ 16:9 (`EventImage` §7.14, rộng 120 px; 390 px ẩn ảnh) + tên + ngày giờ đầy đủ + địa điểm + `EventStatusBadge` (§7.6, theo `displayStatus` của E-21) + liên kết `tickets.order.viewEvent` | Một khối mỗi `eventId` |
| Đầu đơn | dòng chữ 14 px | Đơn có vé `ISSUED`: `tickets.order.paid` (`{count}` = số vé của đơn) + tiền (`Σ unitPrice`, định dạng tiền) + `tickets.order.byCard` khi tổng > 0, hoặc `tickets.order.free` khi tổng = 0. Đơn `REFUND_PENDING`/`REFUNDED`: nhãn trạng thái `tickets.order.refundPending` / `tickets.order.refunded` (kiểu `Notice` nhỏ: biểu tượng + chữ, không chỉ màu); sự kiện bị hủy thêm `tickets.order.cancelledEvent`. Luôn có liên kết `tickets.order.viewOrder` tới `/orders/{orderId}` (DOC-49) |
| Thẻ vé | `TicketCard` (§7.12) | Như DOC-49 §4: nhãn `tickets.card.label` (`{n}/{total}` trong đơn), chân thẻ theo `label` (GA `{}` chỉ có loại vé, DR-108), mã vé `user-select: all`. Vé `VOID`: mờ 60%, nhãn `common.ticketStatus.VOID`, mã vé gạch ngang **không dùng** (vẫn đọc được để đối chiếu) |
| Đơn chờ hoàn tiền không có vé | khối sự kiện + đầu đơn + đoạn lý do | `LATE_PAYMENT` → `orders.refund.late.title`; `AMOUNT_MISMATCH` → `orders.refund.mismatch.title`; không có thẻ vé. Dữ liệu từ E-20 (§5) |
| Tải thêm | `Button secondary` `tickets.more` | Khi E-21 hoặc E-20 còn `nextCursor` |
| Rỗng | `EmptyState` (§7.17): `tickets.empty.title`, `.body`, nút `tickets.empty.cta` → `/` | |
| Đang tải, lỗi | `Skeleton`, `LoadError` | §7 |

Trang **không có nút chính** khi có vé (việc chính là đọc mã vé); trạng thái rỗng có một nút chính (UXP-07).

Khác canvas: canvas `08` chỉ có vé `ISSUED`; tài liệu thêm vé `VOID`, đơn chờ hoàn tiền và đã hoàn tiền (DR-28, DR-44, DOC-38 §9).

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Vé | `GET /me/tickets?limit=100` (E-21) | Vào màn; `refetchOnWindowFocus` | Trường dùng: `ticketId`, `orderId`, `code`, `status`, `ticketTypeName`, `unitPrice`, `label`, `issuedAt`, `event`, `displayStatus`, và trường mới `orderStatus`, `refundReason` (DR-191). `Cache-Control: no-store` (DR-119) |
| Vé cũ hơn | E-21 với `cursor` | Bấm "Xem thêm vé" | Nối vào; nhóm tính lại trên toàn bộ danh sách đã tải |
| Đơn chờ hoàn tiền không có vé | `GET /me/orders?limit=100` (E-20) | Vào màn, song song với E-21 | Lấy phần tử có `status ∈ {REFUND_PENDING, REFUNDED}` **và** `ticketCount = 0`, kèm `refundReason` (DR-191). Đơn khác của E-20 không hiện ở màn này |

Ghép: vé gom theo `event.eventId`, trong một sự kiện gom theo `orderId` (đơn mới trước theo `issuedAt`). Đơn của E-20 chèn vào khối sự kiện tương ứng (tạo khối nếu chưa có). Một vé có `orderStatus` khác `PAID` chỉ xảy ra với `REFUND_PENDING`/`REFUNDED` (vé `VOID`).

Không có kênh real-time; không làm mới theo nhịp.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-21, E-20 | Danh sách theo §5 | 401 → `Login` hết phiên với `returnTo=/me/tickets`; E-21 lỗi → `LoadError`; chỉ E-20 lỗi → hiện vé bình thường, bỏ khối đơn không vé, `Notice` `warning` `tickets.refunds.error` có `common.retry` |
| Bấm "Xem thêm vé" | E-21 (và E-20 nếu còn cursor) | Nối vào danh sách | Lỗi → `Notice` `error` trên nút + `common.retry`; danh sách giữ nguyên |
| Bấm "Xem đơn" | — | `/orders/{orderId}` (DOC-49) | — |
| Bấm "Xem trang sự kiện" | — | `/events/{eventId}` (DOC-43) | — |
| Chạm mã vé | — | Chọn cả mã | — |
| Đổi ngôn ngữ | — | Nhãn, ngày, tiền định dạng lại; không tải lại | — |

Luồng: FL-17.

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Có vé (`08`, `M08`) | E-21 trả ≥ 1 vé | Nhóm "Sắp diễn ra" / "Đã diễn ra", khối sự kiện, thẻ vé |
| 2 | Chưa có vé (`08` "Chưa có vé") | E-21 trả 0 vé và E-20 không có đơn chờ/đã hoàn tiền không vé | `EmptyState` `tickets.empty.*` với nút "Xem sự kiện" |
| 3 | Đang tải (`08` "Đang tải") | Chờ E-21 > 150 ms | `Skeleton`: 1 khối sự kiện và 2 thẻ vé; dòng ẩn `tickets.loading` |
| 4 | Lỗi tải (`08` "Lỗi tải") | E-21 lỗi sau tự thử lại | `LoadError`: `common.loadError.label`, `tickets.error.title`, `tickets.error.body` ("Vé của bạn vẫn an toàn và vẫn có trong email"), `common.retry` |
| 5 | Sự kiện bị hủy, chờ hoàn tiền | Vé `VOID`, `orderStatus = REFUND_PENDING`, `refundReason = EVENT_CANCELLED` | Khối sự kiện có nhãn `CANCELLED`; đầu đơn `tickets.order.refundPending` + `tickets.order.cancelledEvent`; thẻ vé mờ "Đã vô hiệu" |
| 6 | Sự kiện bị hủy, đã hoàn tiền | Vé `VOID`, `orderStatus = REFUNDED` | Như 5 với `tickets.order.refunded` |
| 7 | Thanh toán trễ, chờ hoàn tiền | E-20: `REFUND_PENDING`, `LATE_PAYMENT`, `ticketCount = 0` | Khối sự kiện, đầu đơn `tickets.order.refundPending`, đoạn `orders.refund.late.title`, liên kết "Xem đơn"; không thẻ vé |
| 8 | Số tiền lệch, chờ hoàn tiền | E-20: `REFUND_PENDING`, `AMOUNT_MISMATCH` | Như 7 với `orders.refund.mismatch.title` |
| 9 | Đã hoàn tiền (không vé) | E-20: `REFUNDED`, `ticketCount = 0` | Như 7 với `tickets.order.refunded` |
| 10 | Sự kiện đã diễn ra | `event.endsAt ≤ now` | Khối ở nhóm "Đã diễn ra"; thẻ vé vẫn đủ (DR-24 `ENDED`) |
| 11 | Không tải được đơn chờ hoàn tiền | E-20 lỗi, E-21 thành công | Như 1 + `Notice` `warning` `tickets.refunds.error` |
| 12 | Đang tải thêm | Chờ trang tiếp | Nút `loading` |
| 13 | Quá tải | 503/429 | Tự thử lại khi đọc (DR-132), giữ khung chờ |
| 14 | Không quyền | Không áp dụng (chỉ thấy của mình) | — |

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.6, §2.3. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Tiêu đề, nhóm | `tickets.title`, `tickets.upcoming`, `tickets.past` |
| Đơn | `tickets.order.paid`, `tickets.order.byCard`, `tickets.order.viewEvent`, `tickets.order.refundPending`, `tickets.order.refunded`, `tickets.order.cancelledEvent`, `orders.refund.late.title`, `orders.refund.mismatch.title` |
| Thẻ vé | `tickets.card.label`, `tickets.card.row`, `tickets.card.seat`, `tickets.card.type`, `tickets.card.code`, `common.ticketStatus.VOID` (mới, DOC-49) |
| Trạng thái sự kiện | `common.eventStatus.*` |
| Rỗng, tải, lỗi | `tickets.empty.title`, `tickets.empty.body`, `tickets.empty.cta`, `tickets.loading`, `tickets.error.title`, `tickets.error.body`, `common.loadError.label`, `common.retry`, `common.loading.slow` |
| **Key mới** (thêm vào DOC-40 §3.6) | `tickets.order.free` "{count, plural, other {# vé}} · miễn phí" / "{count, plural, one {# ticket} other {# tickets}} · free"; `tickets.order.viewOrder` "Xem đơn" / "View order"; `tickets.more` "Xem thêm vé" / "Show more tickets"; `tickets.refunds.error` "Không tải được các đơn đang chờ hoàn tiền. Vé của bạn bên dưới vẫn đầy đủ." / "Couldn't load orders awaiting a refund. Your tickets below are complete." |

## 9. Tiêu chí nghiệm thu

Tiền tố `MYT-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| MYT-AC-01 | 3 vé: 2 của sự kiện A (một đơn), 1 của sự kiện B | Mở màn | 2 khối sự kiện; khối A có một đầu đơn "2 vé · …" và 2 thẻ "1/2", "2/2" |
| MYT-AC-02 | Sự kiện A kết thúc hôm qua, B ngày mai (theo giờ server) | Mở màn | B ở "Sắp diễn ra", A ở "Đã diễn ra"; đồng hồ máy lệch +2 ngày không đổi kết quả |
| MYT-AC-03 | Đơn GA 0 đồng 2 vé | Mở màn | Đầu đơn "2 vé · miễn phí", không có "bằng thẻ" |
| MYT-AC-04 | Vé GA `label = {}` | Mở màn | Thẻ chỉ có "Loại vé" và "Mã vé" |
| MYT-AC-05 | Sự kiện bị hủy: 2 vé `VOID`, `orderStatus = REFUND_PENDING` | Mở màn | Nhãn sự kiện "Đã hủy"; đầu đơn "Chờ hoàn tiền" và "Sự kiện đã bị hủy"; 2 thẻ mờ "Đã vô hiệu"; mã vẫn đọc được |
| MYT-AC-06 | Như 05 nhưng `REFUNDED` | Mở màn | Đầu đơn "Đã hoàn tiền" |
| MYT-AC-07 | E-20 có đơn `REFUND_PENDING` `LATE_PAYMENT` `ticketCount = 0` | Mở màn | Khối sự kiện với "Chờ hoàn tiền" và "Thanh toán đến sau khi hết giờ giữ vé"; liên kết "Xem đơn" tới `/orders/{orderId}` |
| MYT-AC-08 | E-20 có đơn `EXPIRED` và `CANCELLED` (không tiền) | Mở màn | Không hiện các đơn này |
| MYT-AC-09 | Không vé, không đơn hoàn tiền | Mở màn | `EmptyState`; nút "Xem sự kiện" về `/` |
| MYT-AC-10 | E-21 500 | Mở màn | `LoadError` với "Vé của bạn vẫn an toàn và vẫn có trong email" |
| MYT-AC-11 | E-20 500, E-21 200 | Mở màn | Vé hiện đủ; thông báo `tickets.refunds.error` |
| MYT-AC-12 | E-21 có `nextCursor` | Bấm "Xem thêm vé" | Request có `cursor`; vé mới gộp vào khối sự kiện sẵn có nếu cùng sự kiện |
| MYT-AC-13 | Chưa đăng nhập | Mở `/me/tickets` | `/login?returnTo=%2Fme%2Ftickets` |
| MYT-AC-14 | 390 px | Mở màn | Thẻ vé rộng 100%; mã vé ≥ 16 px; không cuộn ngang |
| MYT-AC-15 | Màn có vé | Đếm nút chính | 0 nút chính (UX-01: tối đa 1) |
| MYT-AC-16 | axe-core | Mọi trạng thái §7 | 0 vi phạm; nhãn trạng thái đơn không chỉ dựa vào màu |

## 10. Ca kiểm thử E2E

Playwright; API thật (`PAYMENTS_MODE=fake`, `make seed`) và mock cho trạng thái hiếm; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| MYT-01 | `my-tickets-after-free-order` | Như CHK-01 (DOC-48), rồi "Xem vé của tôi" | 2 vé trong "Sắp diễn ra" với đúng mã ở màn Kết quả và email (khớp P2-10) |
| MYT-02 | `my-tickets-event-cancelled` | Mua vé 0 đồng; người tổ chức hủy sự kiện (DOC-59) | MYT-AC-05 |
| MYT-03 | `my-tickets-refunded` | Như MYT-02, người vận hành chạy câu `UPDATE … REFUNDED` của RB-01 | MYT-AC-06 |
| MYT-04 | `my-tickets-empty` | Tài khoản mới | MYT-AC-09 |
| MYT-05 | `my-tickets-late-payment` (P3) | Cổng giả bơm `succeeded` sau khi reservation hết hạn | MYT-AC-07 |
| MYT-06 | `my-tickets-variants` (mock) | Lỗi E-21, lỗi E-20, phân trang | MYT-AC-10…12 |
| MYT-07 | `my-tickets-mobile` | 390 × 844 | MYT-AC-14 |

## Quyết định phát sinh khi viết tài liệu này

**DR-191 · E-21 trả trạng thái đơn; E-20 trả lý do hoàn tiền; Vé của tôi ghép hai nguồn.** *Vấn đề:* màn phải hiện "Chờ hoàn tiền"/"Đã hoàn tiền" cho vé `VOID` (DR-28) và cho đơn đã trả tiền nhưng không có vé (thanh toán trễ, số tiền lệch, DR-44). E-21 chỉ có vé, không có trạng thái đơn; E-20 có trạng thái đơn nhưng không có `refundReason`. Đơn trả trễ không có dòng `ticket` nên không bao giờ xuất hiện ở E-21. *Quyết định:* (1) Mỗi phần tử E-21 thêm `orderStatus` và `refundReason` (lấy từ `orders` đã JOIN sẵn trong truy vấn của E-21, không thêm truy vấn). (2) Mỗi phần tử E-20 thêm `refundReason`. (3) Màn gọi song song E-21 và E-20 (cùng `limit=100`) và chỉ lấy từ E-20 các đơn `REFUND_PENDING`/`REFUNDED` có `ticketCount = 0`. Không thêm endpoint mới. *Hệ quả:* DOC-37 E-20, E-21 thêm trường và ví dụ; hai request mỗi lần vào màn (đều `no-store`, p95 < 40 ms). Người có hơn 100 đơn chỉ thấy đơn hoàn tiền không vé trong 100 đơn mới nhất cho tới khi bấm "Xem thêm" (chấp nhận được với quy mô của dự án). *Ghi vào:* DOC-37, DOC-50.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-191 (mục trên).
