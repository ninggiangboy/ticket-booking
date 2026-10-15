# Màn hình: Danh sách sự kiện

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-42
> Phụ thuộc: SDD gốc §13.1, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 24, 38, 62, 63, 67, 68, 86, 132, 136, 137), [DOC-02](../../01-product/personas-and-journeys.md) (J-1, J-2), [DOC-04](../../01-product/use-cases.md) (UC-02), [DOC-06](../../02-glossary.md), [DOC-37](../../07-api/api-endpoints.md) (E-06, E-13), [DOC-38](../ux-principles-and-ia.md) §2.1, §3, §6.1, §8.2, [DOC-39](../design-system.md) §7.6, §7.14, §7.17, [DOC-40](../ui-states-and-copy.md) §1, §2, §3.1, §3.3, [DOC-41](README.md), [DOC-82](../../06-design/flows/README.md) (FL-12); canvas: artboard `00`, `M00`
> Người dùng chính: P2-10 (giao diện mua vé GA), tác giả DOC-85 (FL-12), người viết test E2E mua vé

Tài liệu mô tả trang chủ `/`: danh sách sự kiện đang hiển thị công khai. Quy tắc sự kiện nào xuất hiện và `displayStatus` tính ra sao nằm ở DR-24 và E-06 (DOC-37); nhãn trạng thái và định dạng ngày, tiền ở DOC-40 §2; thẻ sự kiện và tem ngày ở DOC-39 §7.14; từng lời gọi qua các tầng ở FL-12 (DOC-85). Màn này chỉ dẫn **key** chuỗi.

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-1 (điện thoại, ưu tiên), PS-2 (máy tính trong giờ mở bán); mọi persona khi vào trang chủ |
| Use case | UC-02 (xem danh sách và chi tiết sự kiện) |
| Yêu cầu | FR-02.8 (`displayStatus` tính ở server), FR-02.15 (danh sách công khai), FR-18.3 (ảnh có cache) |
| Hành trình | J-1 bước 1, J-2 bước 1 |
| Luồng chi tiết | FL-12 |
| Quyền | Công khai (khách và người đã đăng nhập thấy như nhau). Header khác nhau theo vai trò (DOC-38 §4) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/` | không có | Trang không có tham số URL. Cursor phân trang (E-06) chỉ giữ trong bộ nhớ của TanStack Query, không đưa lên URL; tải lại trang bắt đầu lại từ trang đầu |

Tham số lạ trên `/` bị bỏ qua (không chuyển E1).

## 3. Wireframe

Desktop (≥ 1024 px), canvas `00`. Lưới 3 cột, thẻ cao bằng nhau; ô ảnh 16:9:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.                                [vi|en]   Vé của tôi  [Tài khoản]│  header 64 px
├──────────────────────────────────────────────────────────────────────────┤
│ SỰ KIỆN SẮP DIỄN RA                                                      │  h1
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐│
│ │ [ ảnh 16:9         ] │ │ THỨ BẢY              │ │ [ ảnh 16:9         ] ││
│ │                      │ │ 14.11      (tem ngày)│ │                      ││
│ │                      │ │ 2026 · 20:00         │ │                      ││
│ │ (Đang mở bán)        │ │ (Sắp mở bán)         │ │ (Hết vé)             ││  EventStatusBadge
│ │ HÒA NHẠC GIAO MÙA    │ │ HỘI THẢO KIẾN TRÚC…  │ │ ĐÊM NHẠC SÂN THƯỢNG  ││
│ │ Thứ Năm 12.11.2026 · │ │ Thứ Bảy 14.11.2026 · │ │ …                    ││
│ │ 19:00                │ │ 20:00                │ │                      ││
│ │ Nhà hát Hòa Bình     │ │ Dreamplex, Quận 1    │ │ …                    ││
│ │ Giá từ 450.000 ₫     │ │ Giá từ Miễn phí      │ │ Giá từ 300.000 ₫     ││
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────┘│
│                         [ Xem thêm sự kiện ]                             │  Button secondary
└──────────────────────────────────────────────────────────────────────────┘
```

Điện thoại 390 px, canvas `M00`: một cột, thẻ tràn ngang trừ lề 16 px; ảnh hoặc tem ngày ở trên, chữ ở dưới:

```text
┌────────────────────────┐
│ ticket.  [vi|en] [≡]   │
│ SỰ KIỆN SẮP DIỄN RA    │
│ ┌────────────────────┐ │
│ │ [ ảnh 16:9       ] │ │
│ │ (Đang mở bán)      │ │
│ │ HÒA NHẠC GIAO MÙA  │ │
│ │ Thứ Năm 12.11.2026 │ │
│ │ · 19:00            │ │
│ │ Nhà hát Hòa Bình   │ │
│ │ Giá từ 450.000 ₫   │ │
│ └────────────────────┘ │
│ ┌────────────────────┐ │
│ │ THỨ BẢY  14.11     │ │
│ │ …                  │ │
│ └────────────────────┘ │
│ [ Xem thêm sự kiện   ] │
└────────────────────────┘
```

## 4. Vùng và component

Trang là `EventList` (`frontend/src/features/events/EventList.tsx`, DOC-41 §1.1).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Header | header chung, `LanguageSwitch` (§7.19), `AccountMenu` (§7.16) hoặc liên kết "Đăng nhập" | DOC-38 §4 |
| Tiêu đề | `h1` Barlow Condensed | `events.list.title` |
| Lưới thẻ | `EventCard` (§7.14) | Cả thẻ là một liên kết `<a href="/events/{eventId}">`; không có nút trong thẻ (không phải nút chính) |
| Ô ảnh | `EventImage` (§7.14) | Có `imageUrl` → `<img src="{imageUrl}" alt="" loading="lazy" width height>`; `imageUrl = null` → tem ngày (thứ `weekday: 'short'`, `dd.MM`, năm · giờ theo DOC-40 §2.2). Ảnh lỗi tải (`onerror`) → thay bằng tem ngày, không hiện biểu tượng ảnh vỡ |
| Nhãn trạng thái | `EventStatusBadge` (§7.6) | Theo `displayStatus` của E-06: thực tế chỉ gặp `UPCOMING`, `ON_SALE`, `SOLD_OUT`, `SALE_CLOSED`, `PAUSED` (E-06 chỉ trả `PUBLISHED`/`PAUSED` còn `endsAt > now`) |
| Tên sự kiện | Barlow Condensed chữ hoa | Dữ liệu người tổ chức nhập, không dịch (DR-10); cắt 3 dòng (`line-clamp`) |
| Ngày giờ | chữ 14 px | Định dạng "ngày giờ đầy đủ" theo `timezone` của sự kiện, có hậu tố offset khi khác `PLATFORM_TIMEZONE` (DOC-40 §2.2, DR-12) |
| Địa điểm | chữ 14 px `--ink-muted` | Cắt 1 dòng |
| Giá từ | nhãn `common.price.from` + giá mono | `priceFrom = 0` → `common.price.free`; `priceFrom = null` → ẩn dòng |
| Tải thêm | `Button secondary` | Chỉ hiện khi `nextCursor ≠ null` |
| Đang tải | `Skeleton` 3 thẻ (desktop) hoặc 2 thẻ (390 px) đúng chiều cao thẻ | DOC-40 §1.1 |
| Rỗng, lỗi | `EmptyState`, `LoadError` (§7.17) | §7 |

Trang **không có nút chính** (`Button primary`): việc chính là bấm vào một thẻ (UXP-01 cho phép 0 hoặc 1 nút chính).

Khác canvas: giá hiển thị theo `Intl` (`450.000 ₫`, không phải `450.000đ`) và có hậu tố offset múi giờ khi cần (DOC-38 §9).

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Trang sự kiện đầu | `GET /events?limit=20` (E-06) | Vào `/` | `staleTime` 5 giây (khớp `max-age=5` của DR-86); `refetchOnWindowFocus` |
| Trang tiếp | `GET /events?limit=20&cursor={nextCursor}` (E-06) | Bấm "Xem thêm sự kiện" | Nối vào cuối danh sách (`useInfiniteQuery`) |
| Ảnh | `GET /media/{mediaId}` (E-13) | Trình duyệt tải theo `imageUrl` | Cache 1 năm `immutable`; không qua `apiClient` |
| Người dùng hiện tại | `GET /me` (E-04) | Do header chung, không thuộc màn này | Không chặn hiển thị danh sách |

Không có kênh real-time; không làm mới theo nhịp. `displayStatus` có thể cũ tối đa ~5 giây (cache nginx); đây là gợi ý (UXP-02), trang Sự kiện và lệnh giữ vé là nguồn đúng.

Ví dụ response dùng cho mock (rút gọn từ E-06):

```json
{
  "items": [
    { "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34", "name": "Hội thảo Kiến trúc Hệ thống", "venue": "Dreamplex, Quận 1",
      "startsAt": "2026-11-14T13:00:00Z", "endsAt": "2026-11-14T17:00:00Z", "timezone": "Asia/Ho_Chi_Minh",
      "saleStartsAt": "2026-11-10T03:00:00Z", "imageUrl": null, "displayStatus": "UPCOMING", "priceFrom": 0, "currency": "VND" }
  ],
  "nextCursor": null
}
```

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào `/` | E-06 | Lưới thẻ theo thứ tự server (`startsAt` tăng dần) | Mạng hoặc 5xx → `LoadError` (§7); 503 `OVERLOADED`, 429 `RATE_LIMITED` → tự thử lại theo DR-132 trong khung chờ |
| Bấm (hoặc Enter) một thẻ | — | Điều hướng tới `/events/{eventId}` (DOC-43); dữ liệu thẻ dùng làm `placeholderData` cho E-07 để trang sự kiện hiện ngay tên, ngày, ảnh | — |
| Bấm "Xem thêm sự kiện" | E-06 với `cursor` | Thẻ mới nối vào; tiêu điểm chuyển tới thẻ đầu tiên của nhóm mới | Lỗi → `Notice` `error` ngay trên nút với `common.retry`; danh sách đã có giữ nguyên |
| Bấm "Thử lại" ở `LoadError` | E-06 | Tải lại từ trang đầu | — |
| Đổi ngôn ngữ | — (FL-04) | Nhãn, ngày giờ, tiền định dạng lại theo locale; dữ liệu không tải lại (E-06 không phụ thuộc locale ngoài `Vary: Accept-Language`) | — |

Luồng: FL-12.

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Có sự kiện (`00`, `M00`) | E-06 trả `items` ≥ 1 | Lưới thẻ; nút "Xem thêm sự kiện" khi `nextCursor ≠ null` |
| 2 | Thẻ có ảnh | `imageUrl ≠ null` | Ảnh 16:9 `object-fit: cover` |
| 3 | Thẻ không ảnh (tem ngày) | `imageUrl = null`, hoặc ảnh lỗi tải | Tem ngày đúng ô ảnh (DOC-39 §7.14) |
| 4 | Đang tải (`00` biến thể "Đang tải") | Chờ E-06 > 150 ms | `Skeleton` 3 thẻ (2 thẻ ở 390 px); dòng ẩn `common.loading.events`; quá 10 giây thêm `common.loading.slow` và `common.retry` |
| 5 | Rỗng (`00` biến thể "Chưa có sự kiện") | E-06 trả `items = []` ở trang đầu | `EmptyState`: `events.list.empty.title`, `events.list.empty.body`; **không có nút** (DR-183) |
| 6 | Lỗi tải (`00` biến thể "Lỗi tải") | E-06 lỗi mạng, 5xx, hoặc hết 10 lần tự thử lại | `LoadError`: `common.loadError.label`, `events.list.error.title`, `events.list.error.body`, `common.retry` |
| 7 | Quá tải | 503 `OVERLOADED` hoặc 429 `RATE_LIMITED` | Giữ khung chờ, `Notice` `warning` `errors.rate_limited` hoặc `checkout.busy.*` với đếm ngược; tự thử lại (DR-132) |
| 8 | Đang tải thêm | Chờ trang tiếp | Nút "Xem thêm sự kiện" ở trạng thái `loading` (`aria-busy`), không khung chờ |
| 9 | Không quyền | Không áp dụng | Trang công khai |

Nhãn trạng thái trên thẻ dùng `common.eventStatus.<displayStatus>` (DOC-40 §2.3). Thứ tự tiêu điểm: liên kết bỏ qua tới nội dung → header → thẻ theo thứ tự lưới → nút "Xem thêm".

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.1, §3.3, §2.3, §4.1. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Tiêu đề, rỗng, lỗi | `events.list.title`, `events.list.empty.title`, `events.list.empty.body`, `events.list.error.title`, `events.list.error.body`, `common.loadError.label`, `common.retry` |
| Thẻ | `events.card.imageAlt` (rỗng), `common.price.from`, `common.price.free`, `common.eventStatus.UPCOMING`, `.ON_SALE`, `.SOLD_OUT`, `.SALE_CLOSED`, `.PAUSED`, `events.dateStamp.label` (nhãn ẩn của tem ngày cho trình đọc màn hình) |
| Đang tải | `common.loading.events`, `common.loading.slow` |
| Quá tải | `errors.rate_limited.title/.body`, `checkout.busy.title/.body`, `common.retryNow` |
| **Key mới** (thêm vào DOC-40 §3.3) | `events.list.more` "Xem thêm sự kiện" / "Show more events"; `events.list.more.error` "Không tải được thêm sự kiện. Danh sách hiện có vẫn giữ nguyên." / "Couldn't load more events. The list you have is unchanged." |

## 9. Tiêu chí nghiệm thu

Tiền tố `EVL-` (màn Danh sách sự kiện; đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| EVL-AC-01 | E-06 trả 2 sự kiện (một có ảnh, một `imageUrl = null`) | Mở `/` | 2 thẻ theo đúng thứ tự response; thẻ thứ hai hiện tem ngày `Thứ Bảy` / `14.11` / `2026 · 20:00` ở `vi` |
| EVL-AC-02 | Sự kiện `timezone = Asia/Tokyo`, `startsAt = 2026-11-14T11:00:00Z` | Mở `/` | Thẻ ghi `Thứ Bảy 14.11.2026 · 20:00 GMT+9` |
| EVL-AC-03 | `priceFrom = 0` và `priceFrom = 450000` | Mở `/` ở `vi` rồi `en` | `Giá từ Miễn phí`, `Giá từ 450.000 ₫`; ở `en`: `From Free`, `From ₫450,000` |
| EVL-AC-04 | Mỗi `displayStatus` trong {`UPCOMING`, `ON_SALE`, `SOLD_OUT`, `SALE_CLOSED`, `PAUSED`} | Mở `/` | Nhãn đúng chữ của DOC-40 §2.3 và đúng kiểu của DOC-39 §7.6; không nhãn nào chỉ phân biệt bằng màu |
| EVL-AC-05 | E-06 trả `items = []` | Mở `/` | `EmptyState` với `events.list.empty.*`; không có nút |
| EVL-AC-06 | E-06 trả 500 | Mở `/` | `LoadError` với `events.list.error.*`; bấm "Thử lại" gọi lại E-06 |
| EVL-AC-07 | E-06 trả 503 `OVERLOADED` `Retry-After: 3` rồi 200 | Mở `/` | Khung chờ giữ nguyên, thông báo tự thử lại; đúng một lần gọi lại ở giây 3; danh sách hiện sau đó |
| EVL-AC-08 | E-06 trang đầu có `nextCursor = "c1"` | Bấm "Xem thêm sự kiện" | Request có `cursor=c1`; thẻ mới nối vào cuối; nút ẩn khi trang mới có `nextCursor = null` |
| EVL-AC-09 | Trang 2 lỗi | Bấm "Xem thêm sự kiện" | Thẻ trang 1 còn nguyên; thông báo `events.list.more.error` ngay trên nút |
| EVL-AC-10 | Ảnh trả 404 | Mở `/` | Thẻ đó đổi sang tem ngày; không có biểu tượng ảnh vỡ |
| EVL-AC-11 | Bàn phím | Tab tới một thẻ, Enter | Điều hướng tới `/events/{eventId}`; vòng tiêu điểm thấy rõ (DOC-39 §4) |
| EVL-AC-12 | Mock trễ 2 giây | Mở `/` | Khung chờ hiện sau 150 ms; CLS < 0,05 khi danh sách thay khung chờ (UX-07) |
| EVL-AC-13 | Viewport 390 px | Mở `/` | Một cột, không cuộn ngang; vùng chạm của thẻ ≥ 44 px |
| EVL-AC-14 | Tên sự kiện 120 ký tự | Mở `/` | Tên cắt ở 3 dòng có `…`; chiều cao các thẻ cùng hàng bằng nhau |
| EVL-AC-15 | axe-core | Mọi trạng thái §7 | 0 vi phạm (UX-09) |

## 10. Ca kiểm thử E2E

Playwright, `frontend/e2e/`; chạy với mock (`VITE_API_MOCK=1`, MSW) và với API thật trên dữ liệu `make seed`; mỗi ca chạy ở `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| EVL-01 | `event-list-seed` | `make seed`; mở `/` | Thấy "Hội thảo Nghề Thiết Kế" và "Đêm nhạc Sân Thượng" (DR-78) với nhãn đúng `displayStatus` server trả; "Hòa nhạc Giao Mùa" có thẻ khi đã xuất bản |
| EVL-02 | `event-list-to-detail` | Bấm thẻ đầu | URL `/events/{eventId}`; tên sự kiện hiện ngay (từ `placeholderData`) |
| EVL-03 | `event-list-empty` (mock) | E-06 rỗng | EVL-AC-05 |
| EVL-04 | `event-list-error-retry` (mock) | 500 rồi 200 | EVL-AC-06 |
| EVL-05 | `event-list-paging` (mock) | 25 sự kiện, `limit=20` | 20 thẻ, bấm "Xem thêm" ra 25 |
| EVL-06 | `event-list-mobile` | Viewport 390 × 844 | EVL-AC-13 |
| EVL-07 | `event-list-locale` | Đổi `vi` → `en` | Nhãn, giá, ngày đổi theo locale; tên sự kiện không đổi |

## Quyết định phát sinh khi viết tài liệu này

**DR-183 · Trạng thái rỗng của Danh sách sự kiện không có nút.** *Vấn đề:* UXP-07 nói trạng thái rỗng "luôn mời một bước kế tiếp", nhưng trên trang chủ không còn trang nào khác để người mua đi tiếp (người tổ chức vào Studio qua menu tài khoản). *Quyết định:* `EmptyState` ở `/` chỉ có tiêu đề và câu `events.list.empty.*`, không có nút; đây là ngoại lệ có chủ ý của UXP-07. *Hệ quả:* UX-01, UX-07 không đổi; DOC-38 UXP-07 thêm ghi chú ngoại lệ. *Ghi vào:* DOC-42, DOC-38.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-183 (mục trên).
