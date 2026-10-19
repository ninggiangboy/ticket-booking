# Màn hình Studio: Tổng quan

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-54
> Phụ thuộc: SDD gốc §12.1, §13, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 23, 24, 65, 70, 71, 132, 136, 137, 155), [DOC-02](../../01-product/personas-and-journeys.md) (PS-3, PS-4, J-3, J-4), [DOC-03](../../01-product/requirements.md) (FR-20), [DOC-04](../../01-product/use-cases.md) (UC-07, UC-09, UC-10), [DOC-06](../../02-glossary.md), [DOC-20](../../06-design/events-and-ticket-types.md) §4, [DOC-37](../../07-api/api-endpoints.md) (E-04, E-24, E-31), [DOC-38](../ux-principles-and-ia.md) §2.2, §3, §4, §6.3, §8.2, [DOC-39](../design-system.md) §7.1, §7.6, §7.9, §7.10, §7.14, §7.16, §7.17, [DOC-40](../ui-states-and-copy.md) §1, §2.2, §2.3, §3.4, §4.1, [DOC-41](README.md), [DOC-53](studio-organizer-profile.md) (cổng Studio), [DOC-82](../../06-design/flows/README.md) (FL-09, FL-32); canvas: artboard "Studio 01"
> Người dùng chính: P2-05.2 (frontend Studio 01), P2-03.1 và P2-05.1 (hợp đồng E-24, E-31), P5-06.2 (đổi đích "Theo dõi bán vé"), tác giả DOC-84 (FL-09), DOC-89 (FL-32)

Tài liệu mô tả màn Tổng quan ở `/studio`: danh sách sự kiện của tổ chức, mỗi thẻ hiện theo `displayStatus` với số đã bán, đang giữ, còn trống (DR-71). Đây là trang chủ của Studio và là đích sau khi lập hồ sơ (DOC-53). Số liệu chi tiết theo loại vé và sơ đồ tô theo trạng thái ở Theo dõi bán vé (DOC-60, P5); các thao tác xuất bản, tạm dừng, đóng bán, hủy ở Xuất bản và mở bán (DOC-59). Cổng vào Studio ở DOC-53 §2. Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.4).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-3, PS-4 (máy tính ≥ 1024 px; dùng được một cột ở màn hẹp, DOC-38 §6.3) |
| Use case | UC-10 bước 1 (số vé mỗi sự kiện); UC-07 (đường vào "Tạo sự kiện", "Soạn tiếp"); UC-09 (mở bán lại sự kiện đang tạm dừng) |
| Yêu cầu | FR-20; FR-13 (đích sau khi lập hồ sơ) |
| Hành trình | J-3 bước 2, 8; J-4 bước 1, 5 |
| Luồng chi tiết | FL-09 (nút "Mở bán lại"), FL-32 (danh sách, P5 cho phần Theo dõi bán vé) |
| Quyền | `ORGANIZER` (DOC-38 §3). Qua cổng Studio (DOC-53 §2): khách → đăng nhập; chưa có hồ sơ → `/studio/profile` |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/studio` | không có | Search param lạ bị bỏ qua |

Không lọc theo trạng thái trên giao diện (E-24 có `status` nhưng canvas không có bộ lọc). Không lưu vị trí trang trên URL: tải lại trang về trang đầu.

## 3. Wireframe

Desktop (≥ 1024 px), canvas "Studio 01". Lưới thẻ 3 cột ở ≥ 1280 px, 2 cột ở 1024–1279 px:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.● Studio                              [vi|en] [Nhà hát Bến Sông ▾]│
├──────────────────────────────────────────────────────────────────────────┤
│ SỰ KIỆN CỦA BẠN   3 sự kiện                            [ TẠO SỰ KIỆN  ]  │
│ ┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────────┐│
│ │ [ảnh 16:9]           │ │ [tem ngày 14.11]     │ │ [ảnh 16:9]           ││
│ │ (Đang mở bán)        │ │ (┄Bản nháp┄)         │ │ (Tạm dừng)           ││
│ │ HÒA NHẠC GIAO MÙA    │ │ HỘI THẢO NGHỀ THIẾT  │ │ ĐÊM NHẠC SÂN THƯỢNG  ││
│ │ Thứ Năm 12.11.2026 · │ │ KẾ                   │ │ Thứ Sáu 20.11.2026 · ││
│ │ 19:00                │ │ Chưa xuất bản        │ │ 21:00                ││
│ │ 249 / 480 vé đã bán  │ │ Đã xong Thông tin.   │ │ Đang không bán. Vé   ││
│ │ ███████▒▒░░░░░░░     │ │ Còn thiếu Loại vé.   │ │ đã bán vẫn hợp lệ.   ││
│ │ ĐÃ BÁN ĐANG GIỮ CÒN  │ │                      │ │ 120 / 300 vé đã bán  ││
│ │ 249    35      196   │ │                      │ │ ████░░░░░░░░░░░░     ││
│ │ [Theo dõi bán vé]    │ │ [Soạn tiếp]          │ │ [Mở bán lại]         ││
│ └──────────────────────┘ └──────────────────────┘ └──────────────────────┘│
│                          [ Xem thêm sự kiện ]  (chỉ khi còn trang sau)   │
└──────────────────────────────────────────────────────────────────────────┘
```

Màn hẹp (390 px, không có artboard riêng): một cột thẻ; nút "Tạo sự kiện" nằm dưới tiêu đề, rộng toàn hàng:

```text
┌────────────────────────┐
│ ticket.● Studio [vi|en]│
│ SỰ KIỆN CỦA BẠN        │
│ 3 sự kiện              │
│ [    TẠO SỰ KIỆN     ] │
│ ┌────────────────────┐ │
│ │ [ảnh 16:9]         │ │
│ │ (Đang mở bán)      │ │
│ │ HÒA NHẠC GIAO MÙA  │ │
│ │ 249 / 480 vé đã bán│ │
│ │ ███████▒▒░░░░░     │ │
│ │ 249 · 35 · 196     │ │
│ │ [Theo dõi bán vé]  │ │
│ └────────────────────┘ │
│ ┌────────────────────┐ │
│ │ …                  │ │
└────────────────────────┘
```

## 4. Vùng và component

Trang là `StudioOverview` (`frontend/src/features/studio/StudioOverview.tsx`, DOC-41 §1.2), chunk `studio`.

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Header | header Studio + `LanguageSwitch` + `AccountMenu` (§7.16) hiện tên tổ chức | Tên tổ chức từ E-04 `organizer.name`; menu theo DOC-38 §4 dòng "Người tổ chức trong Studio" |
| Tiêu đề | `h1` `studio.overview.title` + số `studio.overview.count` | Số chỉ hiện khi đã tải hết (`nextCursor = null`); còn trang sau thì ẩn số (E-24 không trả tổng) |
| Nút chính | `Button primary` `studio.overview.create` → `/studio/events/new` (DOC-55) | Ẩn ở trạng thái rỗng (nút của `EmptyState` thay chỗ, giữ một nút chính mỗi màn) |
| Thẻ sự kiện | `EventCard` + `EventImage` (§7.14) + `EventStatusBadge` (§7.6) | Thứ tự theo E-24 (`createdAt` giảm dần). Tên là liên kết tới đích chính của thẻ (bảng dưới). Ngày giờ: mẫu "ngày giờ đầy đủ" theo `timezone` của sự kiện, có hậu tố offset khi khác `PLATFORM_TIMEZONE` (DOC-40 §2.2, DR-12, DR-137); `startsAt = null` → không có dòng ngày, ô ảnh dùng tem trống |
| Số liệu | `studio.overview.soldOf` + `SalesBar` (§7.10, `aria-label` = `studio.sales.bar.aria`) + ba `Metric` `studio.overview.sold` · `.held` · `.free` | Chỉ ở thẻ có số (bảng dưới). `capacity = 0` → không vẽ thanh. Phần trăm = `round(sold × 100 / capacity)` |
| Xem thêm | `Button secondary` `studio.overview.more` | Chỉ khi `nextCursor ≠ null` |
| Thông báo | `Notice` (§7.9) trên lưới | Kết quả "Mở bán lại", lỗi trạng thái (§6) |

Nội dung thẻ theo `displayStatus` (DR-24):

| `displayStatus` | Thân thẻ | Số liệu | Hành động (nút `secondary` 44 px) | Đích chính (liên kết tên) |
| --- | --- | --- | --- | --- |
| `DRAFT` | `studio.overview.draft.body` + dòng tiến độ (dưới bảng) | Không | `studio.overview.continue` → bước `TODO` đầu tiên | như nút |
| `UPCOMING` | `studio.overview.upcoming.body` (`{time}` = `saleStartsAt`, mẫu "Mốc mở/đóng bán") | Có | "Theo dõi bán vé" (`studio.overview.track`) | `…/sales` (từ P5) |
| `ON_SALE` | — | Có; thêm `studio.overview.heldNote` khi `held > 0` | `studio.overview.track` | `…/sales` (từ P5) |
| `SOLD_OUT` | — | Có | `studio.overview.track` | `…/sales` (từ P5) |
| `SALE_CLOSED` | — | Có | `studio.overview.track` | `…/sales` (từ P5) |
| `PAUSED` | `studio.overview.paused.body` | Có | `studio.overview.resume` (gọi E-31) | `…/publish` |
| `ENDED` | `studio.overview.ended.body` | Chỉ `studio.overview.soldOf` | `studio.overview.viewSales` | `…/sales` (từ P5) |
| `CANCELLED` | `studio.overview.cancelled.body` | Không | `studio.overview.viewSales` | `…/sales` (từ P5) |

**Trước P5** (chưa có route `…/sales`, DOC-60): mọi đích `…/sales` ở bảng trên là `…/publish` (DOC-59) và nhãn nút "Theo dõi bán vé"/"Xem số vé" thay bằng `studio.overview.manage`. P5-06.2 đổi lại (DR-194).

**Dòng tiến độ của thẻ `DRAFT`** (từ `steps` của E-24, DR-193): xét ba bước soạn `info`, `ticketTypes`, `seatMap` (bỏ `seatMap` khi giá trị là `null`); `{done}`/`{missing}` là danh sách tên bước `studio.step.*` ghép bằng `Intl.ListFormat(locale, { type: 'conjunction' })`.

| Tình huống | Key | Ví dụ `vi` |
| --- | --- | --- |
| Có bước xong và bước thiếu | `studio.overview.draft.missing` | "Đã xong Thông tin. Còn thiếu Loại vé." |
| Chưa xong bước nào | `studio.overview.draft.none` | "Còn thiếu Thông tin và Loại vé." |
| Đủ cả ba bước | `studio.overview.draft.ready` | "Đã soạn xong. Xem trước rồi xuất bản." |

Đích "Soạn tiếp": bước `TODO` đầu tiên theo thứ tự `info` → `…/info`, `ticketTypes` → `…/ticket-types`, `seatMap` → `…/map`; đủ cả ba → `…/preview`.

Khác canvas: canvas "Studio 01" chỉ vẽ thẻ đang bán, tạm dừng, bản nháp, đã diễn ra. Thẻ `UPCOMING`, `SOLD_OUT`, `SALE_CLOSED`, `CANCELLED` là bổ sung theo tám giá trị `displayStatus` (DR-24), dùng cùng khung.

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Người dùng, tên tổ chức | `GET /me` (E-04) | Cổng Studio, header | Cache `['me']` |
| Danh sách sự kiện kèm số vé | `GET /organizer/events?limit=20` (E-24) | Vào màn; "Xem thêm" với `cursor`; tự làm mới 15 giây khi tab hiện, dừng khi ẩn, hỏi một lần khi hiện lại và khi cửa sổ lấy lại tiêu điểm (DR-71, DR-194) | TanStack Query `useInfiniteQuery(['organizer','events'])`; làm mới gọi lại mọi trang đã tải |
| Mở bán lại | `POST /organizer/events/{eventId}/resume` (E-31) | Bấm "Mở bán lại" | Body `{}`, `X-CSRF-Token` |

Một phần tử của E-24 sau DR-193 (hai trường mới `saleStartsAt`, `steps`; các trường còn lại như DOC-37):

```json
{
  "eventId": "0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34",
  "name": "Hội thảo Nghề Thiết Kế",
  "status": "DRAFT",
  "displayStatus": "DRAFT",
  "startsAt": "2026-11-14T13:00:00Z",
  "saleStartsAt": null,
  "timezone": "Asia/Ho_Chi_Minh",
  "imageUrl": null,
  "capacity": 0, "sold": 0, "held": 0, "available": 0,
  "steps": { "info": "DONE", "ticketTypes": "TODO", "seatMap": null, "preview": "TODO", "publish": "TODO" },
  "rowVersion": 2
}
```

`steps` cùng định nghĩa với `OrganizerEventView.steps` (DOC-37 §7, DR-70). Thẻ không phải `DRAFT` bỏ qua `steps`.

Không có kênh real-time.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-04 (cổng), E-24 | Lưới thẻ hoặc trạng thái rỗng | 403 `ORGANIZER_PROFILE_REQUIRED` → `/studio/profile` (DOC-53 §2); lỗi khác → `LoadError` (§7) |
| Bấm "Tạo sự kiện" | — | `navigate('/studio/events/new')` (DOC-55 tạo nháp bằng E-25) | — |
| Bấm tên sự kiện hoặc nút hành động điều hướng | — | Tới đích theo bảng §4 | Sự kiện đã bị xóa khỏi tổ chức → màn đích nhận 404 → E1 |
| Bấm "Mở bán lại" | E-31 | **200**: cập nhật thẻ trong cache bằng `status`, `displayStatus`, `rowVersion` của response; `Notice` `success` `studio.overview.resumed` (`{name}`); rồi gọi lại E-24 | 409 `EVENT_STATE_CONFLICT` → `Notice` `error` `studio.publish.notice.stateConflict` + gọi lại E-24 (sự kiện đã đổi trạng thái ở tab khác hoặc đã qua `ends_at`); 503/429/mạng → tự thử lại (DR-132), nút `loading` `studio.overview.resume.busy`; 403 `CSRF_TOKEN_INVALID` → lấy lại token, gửi lại một lần (DR-85) |
| Bấm "Xem thêm sự kiện" | E-24 `cursor=nextCursor` | Nối 20 thẻ tiếp; tiêu điểm vào thẻ đầu tiên của trang mới | 400 `invalid_cursor` (dữ liệu đổi) → tải lại từ trang đầu; lỗi khác → `Notice` `error` `studio.overview.more.error` + nút `common.retry`, thẻ đã có giữ nguyên |

"Mở bán lại" không có hộp thoại xác nhận: việc hoàn tác được bằng "Tạm dừng" ở DOC-59 (DOC-38 §8.3 chỉ xác nhận việc không hoàn tác hoặc có hệ quả lớn).

Luồng: FL-09 (mở bán lại, DOC-84); FL-32 (P5, DOC-89).

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Có sự kiện (Studio 01) | E-24 trả ≥ 1 phần tử | Lưới thẻ theo §4 |
| 2 | Rỗng (Studio 01, biến thể "Chưa có sự kiện nào") | E-24 trả `items = []` ở trang đầu | `EmptyState`: `studio.overview.empty.title`, `.body`, nút `primary` `studio.overview.empty.cta` → `/studio/events/new`; nút "Tạo sự kiện" ở tiêu đề ẩn; không có số đếm |
| 3 | Đang tải (Studio 01, biến thể "Đang tải") | Chờ E-24 lần đầu > 150 ms | `Skeleton` 3 thẻ đúng khung (ảnh 16:9, nhãn, 2 dòng chữ, thanh, ba ô số); dòng ẩn `studio.overview.loading` |
| 4 | Lỗi tải (Studio 01, biến thể "Lỗi tải") | E-24 lần đầu lỗi (500, mạng, hết 10 lần tự thử lại khi 503) | `LoadError`: `common.loadError.label`, `studio.overview.error.title`, `studio.overview.error.body`, `common.retry` |
| 5 | Làm mới lỗi | Lần làm mới 15 giây lỗi khi đã có dữ liệu | Giữ số cũ, không báo; lần sau thử lại theo nhịp |
| 6 | Quá tải | E-24 lần đầu trả 503/429 | `Notice` `warning` đếm ngược theo DOC-40 §1.4, `Skeleton` vẫn hiện; tự thử lại (DR-132) |
| 7 | Đang mở bán lại | Chờ E-31 | Nút của thẻ đó `loading` `studio.overview.resume.busy`; thẻ khác dùng bình thường |
| 8 | Mở bán lại xong | E-31 200 | Thẻ đổi nhãn theo `displayStatus` mới (`ON_SALE`, `UPCOMING`, `SOLD_OUT` hoặc `SALE_CLOSED`); `Notice` `success` |
| 9 | Xung đột trạng thái | E-31 409 `EVENT_STATE_CONFLICT` | `Notice` `error`; thẻ hiện theo dữ liệu E-24 mới |
| 10 | Đang tải thêm | Chờ E-24 có `cursor` | Nút "Xem thêm" `loading`; thẻ đã có giữ nguyên |
| 11 | Không quyền | Khách hoặc chưa có hồ sơ | Không render: cổng Studio (DOC-53 §2). API trả 403 `FORBIDDEN` → E3 (DR-140) |

Tiêu điểm: vào màn → `h1`. Sau `Notice` kết quả: không cướp tiêu điểm (`role="status"`), trừ khi bấm bằng bàn phím thì tiêu điểm vẫn ở nút của thẻ.

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.4 (khối "Tổng quan"), §2.2, §2.3, §4.1. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Tiêu đề | `studio.overview.title`, `studio.overview.count`, `studio.overview.create` |
| Thẻ | `common.eventStatus.<displayStatus>`, `studio.overview.soldOf`, `.sold`, `.held`, `.free`, `.heldNote`, `.track`, `.paused.body`, `.resume`, `.draft.body`, `.draft.missing`, `.continue`, `.ended.body`, `.viewSales`, `studio.step.info`, `studio.step.ticketTypes`, `studio.step.map`, `studio.sales.bar.aria` |
| Rỗng, tải, lỗi | `studio.overview.empty.title`, `.empty.body`, `.empty.cta`, `studio.overview.loading`, `studio.overview.error.title`, `.error.body`, `common.loadError.label`, `common.retry`, `common.loading.slow` |
| Kết quả, lỗi thao tác | `studio.publish.notice.stateConflict`, `errors.rate_limited.*`, `errors.generic.*` |
| **Key mới** (thêm vào DOC-40 §3.4, khối "Tổng quan") | `studio.overview.upcoming.body` "Mở bán lúc {time}." / "Sales open at {time}."; `studio.overview.cancelled.body` "Sự kiện đã hủy. Vé đã bán bị vô hiệu, đơn đã thanh toán đang chờ hoàn tiền." / "This event was cancelled. Sold tickets are void and paid orders are awaiting a refund."; `studio.overview.draft.none` "Còn thiếu {missing}." / "Still missing: {missing}."; `studio.overview.draft.ready` "Đã soạn xong. Xem trước rồi xuất bản." / "All set. Preview it, then publish."; `studio.overview.manage` "Quản lý sự kiện" / "Manage event"; `studio.overview.more` "Xem thêm sự kiện" / "Show more events"; `studio.overview.more.error` "Chưa tải thêm được sự kiện. Thử lại." / "Couldn't load more events. Try again."; `studio.overview.resume.busy` "Đang mở bán lại…" / "Resuming sales…"; `studio.overview.resumed` "Đã mở bán lại {name}." / "Sales resumed for {name}." |

Tên sự kiện không dịch (DOC-40 §0).

## 9. Tiêu chí nghiệm thu

Tiền tố `SOV-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| SOV-AC-01 | Tổ chức chưa có sự kiện (E-24 `items: []`) | Mở `/studio` | `EmptyState` "Chưa có sự kiện nào" với nút "Tạo sự kiện đầu tiên" → `/studio/events/new`; không có nút "Tạo sự kiện" ở tiêu đề |
| SOV-AC-02 | E-24 trả 3 sự kiện, `nextCursor: null` | Mở màn | 3 thẻ theo thứ tự của response; tiêu đề có "3 sự kiện" |
| SOV-AC-03 | Thẻ `ON_SALE`, `capacity 480`, `sold 249`, `held 35`, `available 196` | Quan sát | "249 / 480 vé đã bán"; ba ô 249 · 35 · 196; thanh có `aria-label` "52% sức chứa 480: 249 vé đã bán, 35 vé đang giữ, 196 vé còn trống" |
| SOV-AC-04 | Thẻ `DRAFT`, `steps.info DONE`, `ticketTypes TODO`, `seatMap null` | Quan sát | "Chưa xuất bản"; "Đã xong Thông tin. Còn thiếu Loại vé."; không có số liệu; bấm "Soạn tiếp" → `/studio/events/{id}/ticket-types` |
| SOV-AC-05 | Thẻ `DRAFT`, ba bước đều `DONE` (`seatMap DONE`) | Bấm "Soạn tiếp" | "Đã soạn xong. Xem trước rồi xuất bản."; điều hướng `…/preview` |
| SOV-AC-06 | Thẻ `PAUSED`; E-31 trả 200 `displayStatus ON_SALE` | Bấm "Mở bán lại" | Một request E-31 có `X-CSRF-Token`; nhãn thẻ đổi "Đang mở bán"; `Notice` "Đã mở bán lại {name}." |
| SOV-AC-07 | Thẻ `PAUSED`; E-31 trả 409 `EVENT_STATE_CONFLICT` | Bấm "Mở bán lại" | `Notice` "Trạng thái sự kiện vừa đổi…"; E-24 được gọi lại; thẻ hiện theo dữ liệu mới |
| SOV-AC-08 | Thẻ `UPCOMING`, `saleStartsAt 2026-11-05T03:00:00Z`, `timezone Asia/Ho_Chi_Minh` | Quan sát (`vi`) | "Mở bán lúc 10:00 · 05.11.2026." |
| SOV-AC-09 | Thẻ sự kiện `timezone Asia/Tokyo`, `startsAt 2026-11-14T11:00:00Z` | Quan sát (`vi`) | "Thứ Bảy 14.11.2026 · 20:00 GMT+9" (DR-12) |
| SOV-AC-10 | Phase P2 (chưa có route `…/sales`), thẻ `ON_SALE` | Quan sát, bấm nút | Nút "Quản lý sự kiện" → `/studio/events/{id}/publish`; không có liên kết nào tới `…/sales` |
| SOV-AC-11 | E-24 trả 20 phần tử và `nextCursor` | Bấm "Xem thêm sự kiện" | Request có `cursor`; 40 thẻ; tiêu đề không hiện số đến khi `nextCursor = null` |
| SOV-AC-12 | Màn đang mở, tab hiện | Chờ 31 giây | Đúng 2 request làm mới E-24 (± 1); ẩn tab 60 giây → 0 request; hiện lại → 1 request ngay |
| SOV-AC-13 | E-24 lỗi 500 ở lần đầu | Mở màn | `LoadError` "Không tải được danh sách sự kiện" + "Việc bán vé không bị ảnh hưởng…"; "Thử lại" gọi lại E-24 |
| SOV-AC-14 | Đang có dữ liệu, lần làm mới lỗi | Chờ 15 giây | Thẻ giữ số cũ; không có thông báo |
| SOV-AC-15 | Thẻ `CANCELLED` | Quan sát | Nhãn "Đã hủy"; "Sự kiện đã hủy. Vé đã bán bị vô hiệu…"; không có thanh số liệu |
| SOV-AC-16 | Sự kiện không ảnh | Quan sát | Ô 16:9 là tem ngày (DOC-39 §7.14); `startsAt = null` → tem không có ngày, thẻ không có dòng ngày |
| SOV-AC-17 | 390 px | Mở màn | Một cột thẻ, không cuộn ngang; nút "Tạo sự kiện" rộng toàn hàng |
| SOV-AC-18 | axe-core | Trạng thái 1, 2, 3, 4 | 0 vi phạm; nhãn trạng thái có chữ, không chỉ màu |

## 10. Ca kiểm thử E2E

Playwright; API thật trên `make reset seed` với `organizer@demo.test` (chủ ba sự kiện mẫu, DR-78, DOC-61) và mock cho các biến thể. Mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| SOV-01 | `studio-overview-seed` | Đăng nhập `organizer@demo.test`, mở `/studio` | Thấy các sự kiện seed với nhãn đúng `displayStatus`; số trên thẻ bằng `sold`/`held`/`available` của E-24 |
| SOV-02 | `studio-overview-empty` | Tài khoản mới lập hồ sơ (SOP-01) rồi vào `/studio` | SOV-AC-01 |
| SOV-03 | `studio-overview-variants` (mock) | Mock E-24 trả 8 thẻ, mỗi `displayStatus` một thẻ | Mỗi thẻ đúng thân, số liệu và nút theo §4 |
| SOV-04 | `studio-overview-resume` | Seed tạm dừng một sự kiện bằng E-30; mở `/studio`, bấm "Mở bán lại" | SOV-AC-06; trang Sự kiện công khai (DOC-43) hiện "Đang mở bán" trong ≤ 5 giây (cache E-07) |
| SOV-05 | `studio-overview-held-live` | Tab A mở `/studio`; tab B (`buyer1@demo.test`) giữ 2 vé GA của sự kiện đang bán | Trong ≤ 16 giây thẻ ở tab A hiện `held` tăng 2 (khớp P2-05.2) |
| SOV-06 | `studio-overview-error` (mock) | E-24 trả 500 rồi 200 | SOV-AC-13, bấm "Thử lại" hiện lưới |
| SOV-07 | `studio-overview-mobile` | 390 × 844, như SOV-01 | SOV-AC-17 |

## Quyết định phát sinh khi viết tài liệu này

**DR-193 · E-24 trả thêm `saleStartsAt` và `steps` cho mỗi sự kiện.** *Vấn đề:* canvas "Studio 01" có thẻ bản nháp "Đã xong {done}. Còn thiếu {missing}." (DOC-40 §3.4) và thẻ sắp mở bán cần giờ mở bán, nhưng E-24 chỉ trả `startsAt` và bốn số; `steps` chỉ có ở E-26 (một sự kiện). Gọi E-26 cho từng thẻ là 20 request mỗi trang. *Quyết định:* mỗi phần tử E-24 thêm `saleStartsAt` (UTC hoặc `null`) và `steps` (cùng định nghĩa `OrganizerEventView.steps`, DR-70). `StudioEventService.list` tính `steps` bằng cùng hàm của E-26, với một câu gộp thêm cho ≤ 20 sự kiện: `SELECT event_id, count(*) AS types, bool_or(model IN ('SEAT','ZONE')) AS needs_map FROM ticket_type WHERE event_id = ANY(:ids) AND deleted_at IS NULL GROUP BY event_id` (dùng `ticket_type_name_uq` theo tiền tố `event_id`); trạng thái sơ đồ lấy qua `MapApi` từ P4 (trước P4 `seatMap` luôn `null` vì chưa có loại vé `SEAT`/`ZONE`). Người quyết định: Claude (Owner ủy quyền). *Hệ quả:* DOC-37 E-24 thêm hai trường vào ví dụ và mô tả nguồn dữ liệu; DOC-20 §4 (dòng E-24) thêm câu gộp; p95 kế hoạch của E-24 giữ < 40 ms. *Ghi vào:* DOC-37, DOC-20, DOC-54.

**DR-194 · Thẻ Tổng quan: đích điều hướng trước và sau P5, nhịp làm mới, phân trang.** *Vấn đề:* nút "Theo dõi bán vé"/"Xem số vé" của canvas dẫn tới Studio 07 (DOC-60) mà route này chỉ có từ P5 (P5-06.2), trong khi Tổng quan làm ở P2 (P2-05.2); DR-71 chỉ nói nhịp làm mới 15 giây cho màn Bán vé; E-24 phân trang bằng `cursor` không có tổng. *Quyết định:* (1) P2–P4: mọi đích `…/sales` thay bằng `…/publish` với nhãn `studio.overview.manage`; P5-06.2 đổi về `…/sales` và nhãn của canvas. (2) Tổng quan làm mới E-24 mỗi 15 giây khi tab hiện, dừng khi ẩn, hỏi ngay khi hiện lại (cùng nhịp DR-71). (3) Nút "Xem thêm sự kiện" tải trang tiếp 20 sự kiện; số đếm ở tiêu đề chỉ hiện khi đã tải hết. Người quyết định: Claude (Owner ủy quyền). *Hệ quả:* P5-06.2 thêm việc đổi đích nút ở `StudioOverview`; DOC-40 thêm `studio.overview.manage`, `studio.overview.more`, `studio.overview.more.error`. *Ghi vào:* DOC-54, DOC-40, master plan §5 (P5-06.2), DOC-60.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-193, DR-194 (mục trên).
