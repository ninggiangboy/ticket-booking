# Màn hình: Sự kiện

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-43
> Phụ thuộc: SDD gốc §6.1, §13.1–13.2, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 24, 28, 38, 41, 42, 57, 62, 66, 67, 68, 86, 132, 136, 137), [DOC-02](../../01-product/personas-and-journeys.md) (J-1, J-2), [DOC-03](../../01-product/requirements.md) (FR-02.8, FR-02.9, FR-02.10, FR-14), [DOC-04](../../01-product/use-cases.md) (UC-02, UC-13), [DOC-06](../../02-glossary.md), [DOC-37](../../07-api/api-endpoints.md) (E-04, E-07, E-10, E-13), [DOC-38](../ux-principles-and-ia.md) §2.1, §3, §3.1, §5, §7.1, [DOC-39](../design-system.md) §7.1, §7.5, §7.6, §7.14, §7.17, [DOC-40](../ui-states-and-copy.md) §2.2, §2.3, §3.3, [DOC-41](README.md), [DOC-42](event-list.md), [DOC-47](quantity-picker.md), [DOC-82](../../06-design/flows/README.md) (FL-12, FL-33); canvas: artboard `01`, `M01`
> Người dùng chính: P2-10 (giao diện mua vé GA), P5 (sự kiện có sơ đồ), P6-07 (nút vào phòng chờ), tác giả DOC-85 (FL-12), DOC-45, DOC-46

Tài liệu mô tả trang `/events/:eventId`: thông tin sự kiện, bảng loại vé, "Cách mua" và **một nút chính** do `displayStatus` quyết định (DR-24, DOC-38 §7.1). Quy tắc `displayStatus` ở DR-24 và E-07; lời gọi qua các tầng ở FL-12 (DOC-85); việc vào phòng chờ khi bấm "Mua vé" ở P6 thuộc DOC-45 và FL-33. Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.3).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-1 (390 px, ưu tiên), PS-2 (máy tính, chờ giờ mở bán); PS-3, PS-4 xem sự kiện của mình như người mua |
| Use case | UC-02 (xem chi tiết), UC-03 (điểm bắt đầu giữ vé), UC-13 (người mua thấy sự kiện đã hủy) |
| Yêu cầu | FR-02.8, FR-02.9, FR-02.10, FR-14.2 (hiển thị phía người mua), FR-18.3 |
| Hành trình | J-1 bước 2, J-2 bước 2–3 |
| Luồng chi tiết | FL-12; FL-33 (P6: nút "Mua vé" vào phòng chờ) |
| Quyền | Công khai. Sự kiện `DRAFT` hoặc không tồn tại → E1 (404) với mọi người, kể cả chủ sự kiện (chủ xem qua Studio "Xem trước", DOC-58). Sự kiện `ENDED`, `CANCELLED` mở được bằng URL (DR-24) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/events/:eventId` | `eventId` | UUID (DR-11); sai định dạng → E1 **không gọi API**; E-07 trả 404 → E1 |

Không có search param. Liên kết chia sẻ là URL này; không có slug.

## 3. Wireframe

Desktop (≥ 1024 px), canvas `01`, biến thể `ON_SALE`. Hai cột: nội dung trái (~ 2/3), khung mua phải dính khi cuộn (`position: sticky; top: 88px`):

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.                                [vi|en]   Vé của tôi  [Tài khoản]│
├──────────────────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────┐ ┌──────────────────────────┐ │
│ │ [ ảnh 16:9 hoặc tem ngày              ] │ │ (Đang mở bán)            │ │ EventStatusBadge
│ │                                         │ │ HÒA NHẠC GIAO MÙA        │ │ h1
│ └─────────────────────────────────────────┘ │ THỜI GIAN                │ │
│ GIỚI THIỆU                                  │ Thứ Năm 12.11.2026 · 19:00│ │
│ Đêm nhạc giao mùa với dàn nhạc …            │ 19:00 đến 22:00          │ │
│ Cửa mở lúc 18:30.                           │ ĐỊA ĐIỂM                 │ │
│                                             │ Nhà hát Hòa Bình, TP.HCM │ │
│ LOẠI VÉ                                     │ MỞ BÁN                   │ │
│ ■ Hạng A   chọn ghế trên sơ đồ  1.200.000 ₫ │ Đến 17:00 · 12.11.2026   │ │
│ ■ Fanzone  không số ghế          450.000 ₫  │ GIÁ TỪ  450.000 ₫        │ │
│ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │ [        MUA VÉ        ] │ │ Button primary
│ CÁCH MUA                                    │ NGƯỜI TỔ CHỨC            │ │
│ 1 Chọn chỗ trên sơ đồ  2 Giữ vé trong 10 phút│ Giao Mùa Studio · email │ │
│ 3 Trả bằng thẻ, nhận vé qua email           │                          │ │
└─────────────────────────────────────────────┴──────────────────────────┘ │
```

Biến thể `UPCOMING` (khung mua):

```text
│ (Sắp mở bán)                         │
│ MỞ BÁN  Từ 10:00 · 10.11.2026        │
│ [ MỞ BÁN SAU  4 ngày 22:10:05 ]      │  Button disabled + đồng hồ mono
│ Email đăng nhập có thể đến chậm. …   │
│ Đăng nhập trước                      │  Button text (chỉ khi chưa đăng nhập)
```

Điện thoại 390 px, canvas `M01`: một cột; ảnh, tên, ngày, địa điểm, loại vé, cách mua, người tổ chức; **thanh dưới cố định** (cao 72 px, nền trắng, viền trên `--line`) chứa "Giá từ" và nút chính; nội dung có `padding-bottom` bằng chiều cao thanh:

```text
┌────────────────────────┐
│ ticket.  [vi|en] [≡]   │
│ [ ảnh 16:9           ] │
│ (Đang mở bán)          │
│ HÒA NHẠC GIAO MÙA      │
│ Thứ Năm 12.11.2026 ·   │
│ 19:00 · 19:00 đến 22:00│
│ Nhà hát Hòa Bình       │
│ LOẠI VÉ                │
│ ■ Hạng A   1.200.000 ₫ │
│ ■ Fanzone    450.000 ₫ │
│ CÁCH MUA  1 · 2 · 3    │
│ GIỚI THIỆU …           │
├────────────────────────┤
│ Giá từ 450.000 ₫ [MUA VÉ]│  thanh dưới cố định
└────────────────────────┘
```

## 4. Vùng và component

Trang là `EventDetail` (`frontend/src/features/events/EventDetail.tsx`, DOC-41 §1.1).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Ảnh | `EventImage` (§7.14) | `imageUrl` hoặc tem ngày; `alt=""`; ảnh lỗi → tem ngày |
| Nhãn trạng thái | `EventStatusBadge` (§7.6) | Đủ 8 giá trị trừ `DRAFT` (không bao giờ tới đây) |
| Tên | `h1` Barlow Condensed chữ hoa | Không dịch (DR-10) |
| Thời gian | nhãn `events.detail.time` + "ngày giờ đầy đủ" của `startsAt` + "khung giờ" `startsAt`–`endsAt` | Theo `timezone` sự kiện, hậu tố offset khi khác `PLATFORM_TIMEZONE` (DOC-40 §2.2) |
| Địa điểm | nhãn `events.detail.venue` + `venue` | |
| Mở bán | nhãn `events.detail.sale` + `events.detail.saleFrom` (khi `UPCOMING`) hoặc `events.detail.saleUntil` (khi `ON_SALE`, `SOLD_OUT`, `PAUSED`); ẩn với `SALE_CLOSED`, `ENDED`, `CANCELLED` | Giờ dạng "mốc mở/đóng bán" |
| Giá từ | `common.price.from` + `min(ticketTypes[].price)` (0 → `common.price.free`) | Tính ở client từ E-07; giống `priceFrom` của E-06 |
| Nút chính | `Button primary` / `disabled` theo §7 | Đúng **một** nút chính (UXP-01) |
| Đồng hồ mở bán | số mono trong nút "Mở bán sau" | `saleStartsAt − (Date.now() + offset)` (DR-66), cập nhật mỗi 1 giây; `aria-live="off"`, nhãn đầy đủ trong `aria-label` của nút, cập nhật mỗi phút |
| Ghi chú dưới nút | chữ 14 px | Theo trạng thái (§7) |
| Giới thiệu | `events.detail.about` + `description` | `white-space: pre-line` (giữ xuống dòng, DR-25); văn bản thuần, không markdown, không link tự động. Rỗng → ẩn khối |
| Loại vé | bảng: `TicketTypeChip` (§7.5, màu `type-{colorIndex}`) + mô tả theo `model` + giá | `GA` → `events.detail.types.ga`; `SEAT`, `ZONE` → `events.detail.types.seat`/`.zone` (tham số `{range}`, `{note}` chốt ở DOC-46, P5). Giá 0 → `common.price.free`. `soldOut = true` → thêm nhãn `checkout.ga.soldOut` và làm mờ giá (vẫn đọc được, 4,5:1) |
| Đường xé | họa tiết DOC-39 §6 | Giữa "Loại vé" và "Cách mua" |
| Cách mua | ba bước đánh số mono | §8 chọn key theo loại sự kiện |
| Người tổ chức | nhãn `events.detail.organizer` + `organizer.name` + `organizer.contactEmail` (liên kết `mailto:`) | |
| Thanh dưới (390 px) | thanh cố định chứa "Giá từ" và nút chính | Nút chính chỉ một, không lặp trong khung nội dung |

Khác canvas: bỏ dòng "Mỗi đơn tối đa 8 vé." (DR-41, DOC-38 §9); số phút giữ vé lấy từ `holdMinutes` (DR-42), không viết cứng 10; thêm biến thể `SALE_CLOSED` (DR-24, canvas không vẽ) dùng bố cục của `PAUSED`.

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Sự kiện | `GET /events/{eventId}` (E-07) | Vào trang | `placeholderData` từ thẻ của E-06 nếu có; `staleTime` 5 giây; `refetchOnWindowFocus` |
| Làm mới theo nhịp | E-07 | Mỗi 30 giây khi tab hiện và `displayStatus` ∈ {`ON_SALE`, `SOLD_OUT`} | Để nhãn "Hết vé" và nút đổi khi vé được trả lại (DR-24: `SOLD_OUT` có thể quay về `ON_SALE`). Trúng cache nginx 5 giây nên không chạm `api` thêm |
| Làm mới khi tới giờ | E-07 | Đồng hồ "Mở bán sau" về 0 (`UPCOMING`) | Không tự đổi sang `ON_SALE` ở client (UXP-04); gọi lại tới khi server trả khác `UPCOMING`, tối đa mỗi 2 giây × 10 lần, rồi theo nhịp 30 giây |
| Người dùng | `GET /me` (E-04) | Do header chung | Chỉ dùng để chọn ghi chú `noteSignedIn`/`noteSignedOut` và liên kết "Đăng nhập trước" |
| Ảnh | `GET /media/{mediaId}` (E-13) | Theo `imageUrl` | |
| Phòng chờ (P6) | `POST /events/{eventId}/queue` (E-10) | Bấm "Mua vé" từ P6 | Ngoài phạm vi P2; hành vi ở DOC-45, FL-33 |

Trường E-07 dùng: `name`, `description`, `venue`, `timezone`, `imageUrl`, `startsAt`, `endsAt`, `saleStartsAt`, `saleEndsAt`, `displayStatus`, `holdMinutes`, `hasSeatMap`, `organizer`, `ticketTypes[]` (`ticketTypeId`, `name`, `model`, `price`, `colorIndex`, `soldOut`), và trường mới `cancelledWithRefunds` (DR-184). `highDemand` và `mapVersion` không dùng ở màn này.

Không có kênh real-time.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào trang | E-07 | Hiện theo `displayStatus` (§7) | 404 → E1; mạng/5xx → `LoadError`; 503/429 → tự thử lại (DR-132) |
| Rê chuột hoặc chạm nút "Mua vé" | — | Tải trước chunk của `/events/:eventId/seats`: `seat-picker` khi `hasSeatMap = true`, `quantity-picker` khi `false` (DOC-38 §3.1) | Lỗi tải chunk không báo; xử lý ở lần điều hướng (DR-140) |
| Bấm "Mua vé" (`ON_SALE`), P2–P5 | — | Điều hướng tới `/events/{eventId}/seats` (cả khách lẫn người đã đăng nhập; khách chọn trước rồi đăng nhập ở bước giữ vé, DR-186). `SeatsRoute` chọn `QuantityPicker` hoặc `SeatPicker` theo `hasSeatMap` | — |
| Bấm "Mua vé" (`ON_SALE`), từ P6 | E-10 | Theo DOC-45 (FL-33): `ADMITTED` → `/events/{eventId}/seats`; ngược lại → `/events/{eventId}/queue`; khách → `/login?returnTo=/events/{eventId}` | DOC-45 |
| Bấm "Đăng nhập trước" (`UPCOMING`, chưa đăng nhập) | — | `/login?returnTo=%2Fevents%2F{eventId}` (DOC-44) | — |
| Bấm "Xem sự kiện khác" (`SOLD_OUT`) | — | Về `/` | — |
| Bấm liên kết `Vé của tôi` trong `events.status.cancelled.refund`, `events.detail.ended.body` | — | `/me/tickets` (khách → đăng nhập với `returnTo=/me/tickets`) | — |
| Đồng hồ "Mở bán sau" về 0 | E-07 | §5 "Làm mới khi tới giờ" | Vẫn `UPCOMING` sau 10 lần → giữ đồng hồ ở `0 giờ 00:00:00`, nhịp 30 giây |
| Bấm email người tổ chức | — | `mailto:` | — |

Luồng: FL-12 (đọc), FL-13 bắt đầu từ trang này (DOC-47).

## 7. Trạng thái

Biến thể bán theo `displayStatus` (DR-24, DOC-38 §7.1). Mỗi dòng: nút chính và nội dung ghi chú dưới nút.

| # | Biến thể (canvas) | Điều kiện vào | Nút chính | Ghi chú / thông báo |
| --- | --- | --- | --- | --- |
| 1 | `ON_SALE` (`01`, `M01`) | `displayStatus = ON_SALE` | `Button primary` `events.detail.cta.buy` | Mở bán: `events.detail.saleUntil` |
| 2 | `UPCOMING`, chưa đăng nhập (`01` "Sắp mở bán") | `UPCOMING`, `GET /me` 401 | `Button disabled` `events.detail.cta.soon` + đồng hồ (`events.detail.cta.soon.countdown` khi ≥ 1 ngày, `events.detail.cta.soon.countdownToday` khi < 1 ngày) | `events.detail.soon.noteSignedOut` + `Button text` `events.detail.cta.loginFirst` |
| 3 | `UPCOMING`, đã đăng nhập | `UPCOMING`, `GET /me` 200 | như 2 | `events.detail.soon.noteSignedIn`; không có "Đăng nhập trước" |
| 4 | `SOLD_OUT` (`01` "Hết vé") | `SOLD_OUT` | `Button disabled` `events.detail.soldOut.cta` | `events.detail.soldOut.body` + liên kết `events.detail.soldOut.other`; mọi dòng loại vé có nhãn "Đã bán hết" |
| 5 | `PAUSED` (`01` "Tạm dừng") | `PAUSED` | `Button disabled` `events.detail.paused.title` | `Notice` `warning`: `events.detail.paused.title`, `events.detail.paused.body` |
| 6 | `SALE_CLOSED` (không có ở canvas, DR-24) | `SALE_CLOSED` | `Button disabled` `events.detail.closed.title` | `Notice` `info`: `events.detail.closed.title`, `events.detail.closed.body` |
| 7 | `ENDED` (`01` "Đã kết thúc") | `ENDED` | Không có nút | `Notice` `info`: `events.detail.ended.body`; khối "Cách mua" ẩn |
| 8 | `CANCELLED`, chưa có ai trả tiền (`01` "Đã hủy") | `CANCELLED`, `cancelledWithRefunds = false` | `Button disabled` `events.detail.cancelled.cta` | `Notice` `error`: `events.status.cancelled.noRefund`; khối "Cách mua" ẩn |
| 9 | `CANCELLED`, có đơn đã trả tiền | `CANCELLED`, `cancelledWithRefunds = true` | như 8 | `Notice` `error`: `events.status.cancelled.refund` (có liên kết Vé của tôi) (DR-28) |
| 10 | Đang tải | Chờ E-07 > 150 ms, không có `placeholderData` | Khung chờ | `Skeleton` đúng hình ảnh 16:9 + khung mua; dòng ẩn `events.detail.loading` |
| 11 | Đang tải có dữ liệu tạm | Có `placeholderData` từ danh sách | Nút chính ở trạng thái khung chờ (chưa bấm được) | Tên, ngày, ảnh hiện ngay; loại vé, giới thiệu là khung chờ |
| 12 | Lỗi tải | E-07 lỗi mạng/5xx hoặc hết tự thử lại | Không có | `LoadError`: `common.loadError.label`, `events.detail.error.title`, `events.detail.error.body`, `common.retry` |
| 13 | Quá tải | 503 `OVERLOADED` / 429 | Khung chờ | `Notice` `warning` tự thử lại (DR-132) |
| 14 | Không tìm thấy | 404, `eventId` sai, `DRAFT` | — | Trang E1 (DOC-52) |
| 15 | Không quyền | Không áp dụng | — | Công khai |

Mọi biến thể giữ bảng loại vé (để người mua xem giá), trừ trạng thái tải/lỗi. "Cách mua" ẩn ở `ENDED`, `CANCELLED`.

Tiêu điểm khi vào trang: `h1`. Thứ tự Tab: header → nút chính (trong khung mua) → liên kết dưới nút → nội dung trái; trên 390 px nút ở thanh dưới nằm cuối thứ tự Tab, liên kết "Bỏ qua tới nội dung" có thêm đích "Tới nút mua vé".

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.3, §3.1, §2.3. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Khung mua | `events.detail.time`, `events.detail.venue`, `events.detail.sale`, `events.detail.saleFrom`, `events.detail.saleUntil`, `common.price.from`, `common.price.free`, `common.eventStatus.*` |
| Nút và ghi chú | `events.detail.cta.buy`, `events.detail.cta.soon`, `events.detail.cta.soon.countdown`, `events.detail.cta.loginFirst`, `events.detail.soon.noteSignedOut`, `events.detail.soon.noteSignedIn`, `events.detail.soldOut.body`, `events.detail.soldOut.cta`, `events.detail.soldOut.other`, `events.detail.paused.title`, `events.detail.paused.body`, `events.detail.closed.title`, `events.detail.closed.body`, `events.detail.ended.body`, `events.detail.cancelled.cta`, `events.status.cancelled.noRefund`, `events.status.cancelled.refund` |
| Nội dung | `events.detail.about`, `events.detail.types`, `events.detail.types.seat`, `events.detail.types.zone`, `events.detail.types.ga`, `checkout.ga.soldOut`, `events.detail.organizer`, `events.dateStamp.label` |
| Cách mua | `events.detail.how.title`, `events.detail.how.1.title/.body` (sự kiện có sơ đồ), `events.detail.how.2.title/.body` (`{minutes}` = `holdMinutes`), `events.detail.how.3.title/.body` (có loại vé > 0 đồng) |
| Tải, lỗi | `events.detail.loading`, `events.detail.error.title`, `events.detail.error.body`, `common.loadError.label`, `common.retry`, `common.loading.slow` |
| **Key mới** (thêm vào DOC-40 §3.3) | `events.detail.cta.soon.countdownToday` "{time}" / "{time}" (đồng hồ khi còn < 1 ngày, tránh "0 ngày"); `events.detail.how.1ga.title` "Chọn số lượng vé" / "Choose how many tickets"; `events.detail.how.1ga.body` "Chọn số vé cho từng loại; vé vào cửa tự do, không số ghế." / "Pick a quantity for each type; general admission, no seat numbers."; `events.detail.how.3free.title` "Nhận vé qua email" / "Get your tickets by email"; `events.detail.how.3free.body` "Vé miễn phí không cần thẻ. Vé cũng nằm ở trang Vé của tôi." / "Free tickets need no card. They also appear on My tickets." |

Chọn bước 1: `hasSeatMap = true` → `how.1.*`; `false` → `how.1ga.*`. Chọn bước 3: mọi `price = 0` → `how.3free.*`; ngược lại `how.3.*`.

## 9. Tiêu chí nghiệm thu

Tiền tố `EVD-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| EVD-AC-01 | E-07 `ON_SALE`, chỉ GA | Mở trang | Một `Button primary` "Mua vé"; "Mở bán · Đến …"; "Cách mua" bước 1 là `how.1ga` |
| EVD-AC-02 | `ON_SALE`, khách | Bấm "Mua vé" | Điều hướng `/events/{id}/seats`, `QuantityPicker` hiện (không qua đăng nhập ở P2) |
| EVD-AC-03 | `UPCOMING`, `saleStartsAt` sau 4 ngày 22:10:05 theo `X-Server-Time`, đồng hồ máy lệch +1 giờ | Mở trang | Nút vô hiệu "Mở bán sau 4 ngày 22:10:05" theo giờ server (± 1 giây) |
| EVD-AC-04 | `UPCOMING` còn 3 giờ | Mở trang | Đồng hồ dạng `03:00:00` (key `countdownToday`), không có "0 ngày" |
| EVD-AC-05 | `UPCOMING`, khách | Mở trang | Có `noteSignedOut` và "Đăng nhập trước" tới `/login?returnTo=%2Fevents%2F{id}`; người đã đăng nhập thấy `noteSignedIn`, không có liên kết |
| EVD-AC-06 | `UPCOMING`, đồng hồ về 0, mock E-07 trả `UPCOMING` 2 lần rồi `ON_SALE` | Chờ | Không tự bật nút ở client; nút "Mua vé" bật sau response `ON_SALE` (lần gọi thứ 3) |
| EVD-AC-07 | `SOLD_OUT` | Mở trang | Nút vô hiệu "Hết vé", `soldOut.body`, liên kết "Xem sự kiện khác" về `/`; mọi loại vé có "Đã bán hết" |
| EVD-AC-08 | `SOLD_OUT`, sau 30 giây mock trả `ON_SALE` | Chờ ở tab hiện | Nút đổi thành "Mua vé" trong ≤ 31 giây |
| EVD-AC-09 | `PAUSED` | Mở trang | Nút vô hiệu "Tạm dừng bán"; `Notice` `warning` với `paused.body` có câu "Vé đã mua vẫn hợp lệ" |
| EVD-AC-10 | `SALE_CLOSED` | Mở trang | Nút vô hiệu "Đã đóng bán"; `closed.body`; không có dòng "Mở bán" |
| EVD-AC-11 | `ENDED` | Mở trang | Không có nút chính; `ended.body`; không có "Cách mua" |
| EVD-AC-12 | `CANCELLED`, `cancelledWithRefunds = false` | Mở trang | `events.status.cancelled.noRefund`; nút vô hiệu "Không mở bán" |
| EVD-AC-13 | `CANCELLED`, `cancelledWithRefunds = true` | Mở trang | `events.status.cancelled.refund` có liên kết tới `/me/tickets` |
| EVD-AC-14 | E-07 404 hoặc `eventId=abc` | Mở trang | E1; với `abc` không có request E-07 |
| EVD-AC-15 | `holdMinutes = 5` | Mở trang | "Giữ vé trong 5 phút" ở "Cách mua" |
| EVD-AC-16 | Mọi loại vé `price = 0` | Mở trang | "Giá từ Miễn phí"; "Cách mua" bước 3 là `how.3free` |
| EVD-AC-17 | `description` có 2 dòng | Mở trang | Hiện 2 dòng, không render HTML (`<b>` hiện nguyên chữ) |
| EVD-AC-18 | Bundle `vi` và `en` | Grep "Mỗi đơn tối đa" | Không có (DR-41, COPY-10) |
| EVD-AC-19 | Viewport 390 px | Mở trang `ON_SALE` | Thanh dưới cố định có "Giá từ" và "Mua vé"; đúng một nút chính nhìn thấy (UX-01); không cuộn ngang |
| EVD-AC-20 | Rê chuột "Mua vé", `hasSeatMap = false` | — | Chunk `quantity-picker` được tải trước (một request) |
| EVD-AC-21 | axe-core | Mọi biến thể §7 | 0 vi phạm (UX-09) |

## 10. Ca kiểm thử E2E

Playwright; mock (MSW) cho các biến thể khó dựng, API thật trên `make seed` cho đường chính; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| EVD-01 | `event-detail-ga-on-sale` | Seed "Hội thảo Nghề Thiết Kế" đang bán; mở trang; bấm "Mua vé" | EVD-AC-01, EVD-AC-02 |
| EVD-02 | `event-detail-upcoming-countdown` | Seed "Đêm nhạc Sân Thượng" (`UPCOMING`); mở trang khi chưa đăng nhập | EVD-AC-03, EVD-AC-05 |
| EVD-03 | `event-detail-upcoming-opens` (mock) | `saleStartsAt` sau 5 giây | EVD-AC-06 |
| EVD-04 | `event-detail-variants` (mock) | Lặp 6 biến thể `SOLD_OUT`, `PAUSED`, `SALE_CLOSED`, `ENDED`, `CANCELLED` (2 dạng) | EVD-AC-07, 09…13 |
| EVD-05 | `event-detail-paused-real` | Người tổ chức tạm dừng sự kiện GA qua Studio (DOC-59); người mua tải lại trang | Biến thể `PAUSED` trong ≤ 5 giây (cache) |
| EVD-06 | `event-detail-not-found` | Mở `/events/<uuid lạ>` | EVD-AC-14 |
| EVD-07 | `event-detail-mobile` | Viewport 390 × 844 | EVD-AC-19 |

## Quyết định phát sinh khi viết tài liệu này

**DR-184 · E-07 trả `cancelledWithRefunds` cho sự kiện đã hủy.** *Vấn đề:* DOC-38 §7.1 và DOC-40 §2.3 chọn câu `events.status.cancelled.noRefund` hay `.refund` theo "sự kiện đã có đơn `PAID` lúc hủy hay chưa", nhưng E-07 không có trường nào cho biết điều này; module `event` (chủ E-07) không được phụ thuộc `order` (DOC-12 §2.2) nên không đếm đơn được lúc đọc. *Quyết định:* thêm cột `event.cancelled_with_refunds boolean` (NULL khi chưa hủy). Transaction hủy sự kiện (DR-28, `studio.CancelEventService`) đặt cột này trong cùng transaction, sau bước 3 (đã biết số đơn `PAID` chuyển `REFUND_PENDING`): `UPDATE event SET cancelled_with_refunds = (:refundPendingOrders > 0) WHERE event_id = :e` qua `EventApi`. E-07 trả `cancelledWithRefunds` (boolean): `null` khi `displayStatus ≠ CANCELLED`. Trường chỉ nói có/không, không lộ số đơn. *Hệ quả:* DOC-14 §4.3 thêm một cột (migration của P2-01); DOC-20 (bước 2 của transaction hủy) và DOC-37 E-07 (trường và ví dụ) cập nhật; cache 5 giây không đổi. *Ghi vào:* DOC-14, DOC-20, DOC-37 (E-07), DOC-43.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-184 (mục trên). Nội dung `{range}` và `{note}` của dòng loại vé `SEAT`/`ZONE` chốt ở DOC-46 (gate P5), không ảnh hưởng P2 vì sự kiện P2 chỉ có GA.
