# Màn hình Studio: Xem trước

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-58
> Phụ thuộc: SDD gốc §13.1, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 23, 24, 26, 37, 41, 42, 62, 66, 68, 70, 132, 185, 186, 187), [DOC-02](../../01-product/personas-and-journeys.md) (J-3 bước 6, J-4 bước 3), [DOC-03](../../01-product/requirements.md) (FR-02.8, FR-02.16), [DOC-04](../../01-product/use-cases.md) (UC-09), [DOC-05](../../01-product/feature-catalog.md) (F-STU-03), [DOC-06](../../02-glossary.md), [DOC-20](../../06-design/events-and-ticket-types.md) §3.2, §6.7, [DOC-32](../../06-design/security.md) (CSP), [DOC-37](../../07-api/api-endpoints.md) (E-07, E-13, E-26, E-40), [DOC-38](../ux-principles-and-ia.md) §2.2, §3, §3.1, §6.3, §9, [DOC-39](../design-system.md) §7.1, §7.6, §7.9, §7.13, §7.17, [DOC-40](../ui-states-and-copy.md) §2.2, §3.4, [DOC-41](README.md) §1.2, §2, [DOC-43](event-detail.md), [DOC-47](quantity-picker.md), [DOC-56](studio-ticket-types.md), [DOC-82](../../06-design/flows/README.md) (FL-08); canvas: artboard "Studio 05"
> Người dùng chính: P2-05.2 (Studio 05 phần GA), P5-06.2 (xem trước có sơ đồ), tác giả DOC-84 (FL-08), người viết test E2E soạn sự kiện

Tài liệu mô tả bước **Xem trước** của Studio ở `/studio/events/:eventId/preview`: người tổ chức thấy trang sự kiện và trang chọn vé **như người mua sẽ thấy**, ở khung máy tính hoặc điện thoại, trước khi sang bước Xuất bản (DOC-59). Màn chỉ đọc: không giữ vé, không thu tiền, không ghi gì vào server. Nội dung trang người mua (bố cục, nhãn, nút theo `displayStatus`) thuộc DOC-43 và DOC-47; màn này chỉ quy định cách **nạp dữ liệu nháp** vào các view đó và khung bao quanh. Biến thể có sơ đồ chọn chỗ (DOC-46) làm ở P5.

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-4 (chỉ GA, P2), PS-3 (có sơ đồ, P5); máy tính ≥ 1024 px, vẫn dùng được dưới 1024 px (DOC-38 §6.3) |
| Use case | UC-09 (bước kiểm tra trước khi xuất bản); F-STU-03 |
| Yêu cầu | FR-02.8 (cách `displayStatus` hiện cho người mua), FR-02.16 (chỉ chủ sự kiện xem) |
| Hành trình | J-3 bước 6, J-4 bước 3 |
| Luồng chi tiết | FL-08 (bước trước `publish-checks`) |
| Quyền | Vai trò `ORGANIZER` và là chủ sự kiện; như DOC-56 §1 (E2, E3, E1, `/studio/profile`). Đây là cách duy nhất để chủ xem sự kiện `DRAFT` dưới dạng trang người mua: `/events/:eventId` trả E1 cho `DRAFT` với mọi người (DOC-43 §1) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/studio/events/:eventId/preview` | `eventId` | UUID; sai định dạng → E1 không gọi API; E-26 trả 404 → E1 |
| | `device` (query, tùy chọn) | `desktop` hoặc `phone`. Vắng → `desktop` khi cửa sổ ≥ 1024 px, `phone` khi hẹp hơn. Giá trị khác → bỏ, dùng mặc định (không E1) |
| | `page` (query, tùy chọn) | `event` (mặc định) hoặc `select`. Giá trị khác → `event` |

Đổi khung hoặc trang dùng `replace` (không thêm mục lịch sử), để nút Back của trình duyệt về bước trước của Studio.

## 3. Wireframe

Desktop (≥ 1024 px), canvas "Studio 05", khung **Máy tính**, trang **sự kiện**, sự kiện chỉ GA. Khung xem trước bên trái (~ 3/4), cột tóm tắt bên phải:

```text
┌───────────────────────────────────────────────────────────────────────────────┐
│ STUDIO   Sự kiện của bạn / Hội thảo Kiến trúc Hệ thống         [vi|en] [GM ▾] │
│ (✓)Thông tin ─ (✓)Loại vé ─ (3)Xem trước ─ (4)Xuất bản              [Bản nháp] │ StudioStepper
├───────────────────────────────────────────────────────────────────────────────┤
│ XEM TRƯỚC                                                                     │
│ Đây là trang người mua sẽ thấy sau khi bạn xuất bản. Mọi chỗ đều còn trống…   │
│ [Máy tính | Điện thoại · 390 px]   [Trang sự kiện | Trang chọn vé]            │ hai nhóm radio
│ ┌ BẢN XEM TRƯỚC ───────────────────────────────────────┐ ┌──────────────────┐ │
│ │ ⓘ Bản xem trước không giữ vé và không thu tiền.     │ │ SẼ MỞ BÁN        │ │
│ │ ┌──────────────────────────────────────────────────┐ │ │ 10:00 Thứ Năm    │ │
│ │ │ ticket.                                           │ │ │ 05.11.2026       │ │
│ │ │ [ảnh 16:9 hoặc tem ngày]   (Sắp mở bán)           │ │ │ TỔNG SỨC CHỨA    │ │
│ │ │ HỘI THẢO KIẾN TRÚC HỆ THỐNG     ┌──────────────┐  │ │ │ 450 vé           │ │
│ │ │ Thời gian · Địa điểm · Mở bán   │ Giá từ       │  │ │ │ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │ │
│ │ │ Giới thiệu …                    │ Miễn phí     │  │ │ │ Sửa thông tin    │ │
│ │ │ Loại vé: ■ Đặt sớm 300.000 ₫    │ [MỞ BÁN SAU  │  │ │ │ Sửa loại vé      │ │
│ │ │          ■ Phổ thông 450.000 ₫  │  6 ngày …]   │  │ │ │                  │ │
│ │ │          ■ Sinh viên Miễn phí   └──────────────┘  │ │ │ [SANG BƯỚC       │ │ Button primary
│ │ │ Cách mua 1 · 2 · 3                                │ │ │  XUẤT BẢN      ] │ │
│ │ └──────────────────────────────────────────────────┘ │ └──────────────────┘ │
│ └──────────────────────────────────────────────────────┘                      │
└───────────────────────────────────────────────────────────────────────────────┘
```

Khung máy tính là trang người mua dựng ở bề rộng 1280 px rồi thu nhỏ cho vừa cột (§4). Khung **Điện thoại · 390 px**, trang **chọn vé** (canvas "Studio 05" bản điện thoại): khung 390 × 844 px có viền thiết bị, cuộn bên trong, thanh dưới cố định của Chọn số lượng nằm trong khung:

```text
│ [Máy tính | ●Điện thoại · 390 px]   [Trang sự kiện | ●Trang chọn vé]  │
│        ┌ BẢN XEM TRƯỚC ────────────┐                                  │
│        │ ⓘ Bản xem trước không giữ │                                  │
│        │ ┌────────────────────────┐│                                  │
│        │ │ ticket.        [vi|en] ││                                  │
│        │ │ 1 Chọn vé · 2 · 3      ││                                  │
│        │ │ CHỌN SỐ LƯỢNG VÉ       ││                                  │
│        │ │ ■ Phổ thông 450.000 ₫  ││                                  │
│        │ │ Còn 300 vé  [−] 2 [+]  ││                                  │
│        │ │ ■ Sinh viên  Miễn phí  ││                                  │
│        │ │ Còn 50 vé   [−] 0 [+]  ││                                  │
│        │ ├────────────────────────┤│                                  │
│        │ │ 2 vé · 900.000 ₫     ▲ ││                                  │
│        │ │ [GIỮ VÉ – vô hiệu     ]││ title = noHold                   │
│        │ └────────────────────────┘│                                  │
│        └───────────────────────────┘                                  │
```

Dưới 1024 px (390 px cửa sổ): cột tóm tắt chuyển lên trên khung, thu gọn thành một dòng "Sẽ mở bán … · Tổng sức chứa …"; mặc định khung Điện thoại; khung Máy tính vẫn chọn được và thu nhỏ theo bề rộng cửa sổ. Nút chính ở dưới khung.

## 4. Vùng và component

Trang là `StudioPreview` (`frontend/src/features/studio/StudioPreview.tsx`, DOC-41 §1.2), trong khung Studio chung. Nội dung khung xem trước **dùng lại view của trang người mua**, không vẽ lại.

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Header, thanh bước | khung Studio, `StudioStepper` (§7.13) | Như DOC-56 §4; bước Sơ đồ chỉ hiện khi có loại vé `SEAT`/`ZONE` |
| Tiêu đề, dẫn | `h1` `studio.preview.title`; đoạn `studio.preview.intro` | Sự kiện đã xuất bản: thay `intro` bằng `studio.preview.intro.live` (key mới) |
| Chọn khung | nhóm 2 nút `role="radiogroup"`: `studio.preview.desktop`, `studio.preview.phone` | Ghi `device` vào URL |
| Chọn trang | nhóm 2 nút `role="radiogroup"`: `studio.preview.page.event`, `studio.preview.page.select` (key mới) | Ghi `page` vào URL |
| Khung xem trước | `PreviewViewport` (component riêng của màn, mô tả dưới) có nhãn mono `studio.preview.frame` | `role="region"`, `aria-label` = `studio.preview.frame`; `inert` với mọi liên kết thoát ra ngoài (§6) |
| Dải cảnh báo trong khung | `Notice` `info` (§7.9) `studio.preview.noHold` | Luôn hiện ở đầu khung, ngoài phần trang người mua |
| Trang sự kiện | `EventDetailView` (phần trình bày của `EventDetail`, DOC-43 §4) | Dữ liệu từ ánh xạ §5 |
| Trang chọn vé, chỉ GA | `QuantityPickerView` (phần trình bày của `QuantityPicker`, DOC-47 §4) | Bộ đếm bấm được để xem tổng và tạm tính; nút giữ vé luôn `disabled` với `title` = `studio.preview.noHold` |
| Trang chọn vé, có sơ đồ (P5) | `SeatPickerView` (DOC-46) ở trạng thái mọi ghế còn trống, chọn được ghế cục bộ | Nguồn tài liệu sơ đồ ở §5. Chưa có phiên bản sơ đồ nào → `Notice` `info` `studio.preview.mapMissing` (key mới) thay cho trang chọn vé |
| Cột tóm tắt | thẻ: nhãn mono `studio.preview.willSell` + giờ `saleStartsAt` (dạng "mốc mở/đóng bán", DOC-40 §2.2); nhãn `studio.preview.capacity` + `studio.preview.ticketsUnit` và, từ P5, `studio.preview.seatsUnit` | Chưa đặt `saleStartsAt` → `studio.info.preview.notSet` |
| Liên kết sửa | `Button text` `studio.preview.editInfo` → `…/info`; `studio.preview.editTypes` → `…/ticket-types`; `studio.preview.editMap` → `…/map` (chỉ khi có loại vé `SEAT`/`ZONE`) | |
| Nút chính | `Button primary` `studio.preview.next` → `…/publish` | Đúng một nút chính; trang người mua trong khung có nút chính riêng nhưng nằm trong vùng `inert`/vô hiệu nên không tính (UX-01 bỏ qua phần tử trong `[data-preview-frame]`) |

**`PreviewViewport`.** Một khối `div` (không phải `iframe`: CSP `frame-ancestors 'none'` và `frame-src` không có `'self'`, DOC-32) với:

- `container-type: inline-size; container-name: app` để bố cục người mua đổi theo **bề rộng khung** chứ không theo cửa sổ (DR-197);
- `transform: scale(k)` với `k = min(1, bề rộng cột / 1280)` ở khung Máy tính (bề rộng logic 1280 px, cao 800 px), `transform: none` nhưng `contain: layout paint` ở khung Điện thoại (390 × 844 px) để phần tử `position: fixed` (thanh dưới, bottom sheet) bám vào khung, không bám vào cửa sổ;
- `overflow: auto`, `overscroll-behavior: contain`;
- một `PreviewModeContext` = `true` cho mọi view bên trong (§6).

Khác canvas: không có dòng "Mỗi đơn tối đa 8 vé." (DR-41, DOC-38 §9); thêm nhóm "Trang sự kiện | Trang chọn vé" vì canvas chỉ vẽ trang sự kiện, còn trang chọn vé thì nút "Mua vé" không bấm được khi sự kiện chưa mở bán (DR-197).

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Sự kiện, loại vé, thông tin cho trang người mua | `GET /organizer/events/{eventId}` (E-26) | Vào màn; `refetchOnWindowFocus` | Khóa `['organizerEvent', eventId]` dùng chung với DOC-55, DOC-56, DOC-59. Cần thêm `holdMinutes`, `maxUnitsPerHold`, `organizer` (DR-198) |
| Ảnh | `GET /media/{mediaId}` (E-13) | Theo `imageUrl` | Như DOC-43 |
| Sơ đồ (P5) | `GET /organizer/events/{eventId}/map` (E-40) lấy `seatMapId`, `latestVersionNo`; rồi tài liệu của phiên bản `latestVersionNo` (DR-199) | Khi chọn trang chọn vé và có loại vé `SEAT`/`ZONE` | Không gọi ở P2 |

Màn **không** gọi E-07, E-09, E-14 hay endpoint ghi nào. Không có kênh real-time; không hỏi theo nhịp.

**Ánh xạ E-26 → view người mua** (`toBuyerEventView(organizerEvent, now)`, hàm thuần trong `features/studio/preview.ts`):

| Trường view (dạng E-07) | Lấy từ E-26 | Quy tắc |
| --- | --- | --- |
| `eventId`, `name`, `description`, `venue`, `timezone`, `imageUrl`, `startsAt`, `endsAt`, `saleStartsAt`, `saleEndsAt`, `highDemand` | cùng tên | Trường giờ `null` ở bản nháp → view hiện `studio.info.preview.noTime`; `venue` rỗng → `studio.info.preview.noVenue` |
| `displayStatus` | `status`, `displayStatus`, `saleStartsAt` | `status = DRAFT`: `UPCOMING` khi `saleStartsAt` là `null` hoặc `> now`, ngược lại `ON_SALE` (trạng thái người mua sẽ thấy nếu xuất bản ngay). Đã xuất bản: dùng `displayStatus` của E-26 |
| `holdMinutes`, `maxUnitsPerHold` | cùng tên (DR-198) | |
| `hasSeatMap` | `ticketTypes[]` | `true` khi có loại vé `SEAT`/`ZONE` |
| `mapVersion` | — | `null` ở P2; P5 lấy `latestVersionNo` |
| `organizer` | `organizer` (DR-198) | `contactEmail` đã thay bằng email chủ khi trống (như E-07) |
| `ticketTypes[]` | `ticketTypes[]` sắp theo `sortOrder` | `ticketTypeId`, `name`, `model`, `price`, `currency`, `colorIndex`; `soldOut` = `false` khi `DRAFT`; đã xuất bản: `GA` → `used ≥ gaCapacity`, `SEAT`/`ZONE` → `false` (P5 thay bằng số từ sơ đồ) |
| `cancelledWithRefunds` | — | `null` (sự kiện `CANCELLED` hiện biến thể theo `displayStatus`, không có số đơn) |

**Số còn của trang chọn vé GA** (thay E-09): `available = gaCapacity − used` cho mỗi loại `GA` (`used = 0` khi `DRAFT`). **Tổng sức chứa** ở cột tóm tắt: `{tickets}` = Σ `gaCapacity` của loại `GA`; từ P5 cộng sức chứa khu vực và `{seats}` = `sellableSeatCount` của phiên bản sơ đồ mới nhất.

Ví dụ: E-26 của "Hội thảo Kiến trúc Hệ thống" (`DRAFT`, `saleStartsAt = 2026-11-05T03:00:00Z`, `now = 2026-10-30T08:00:00Z`, 3 loại `GA` 100/300/50) cho view `displayStatus = UPCOMING`, nút "Mở bán sau 5 ngày 19:00:00", trang chọn vé "Còn 100 vé", "Còn 300 vé", "Còn 50 vé", tổng sức chứa "450 vé".

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-26 | Khung theo `device`, trang theo `page` | 404 → E1; 403 `ORGANIZER_PROFILE_REQUIRED` → `/studio/profile`; mạng/5xx → `LoadError`; 503/429 → tự thử lại (DR-132) |
| Chọn "Máy tính" / "Điện thoại · 390 px" | — | Đổi khung, giữ trang và lựa chọn số lượng đang có; `device` vào URL (`replace`) | — |
| Chọn "Trang sự kiện" / "Trang chọn vé" | — | Đổi trang trong khung; cuộn khung về đầu; `page` vào URL | — |
| Bấm "Mua vé" trong khung (khi view hiện nút này) | — | Như chọn "Trang chọn vé" | — |
| Bấm liên kết sự kiện trên trang chọn vé trong khung | — | Như chọn "Trang sự kiện" | — |
| Bấm `+`/`−` trong khung | — | Đổi số và tạm tính cục bộ; **không** ghi `localStorage["tb.selection.*"]` (DR-67) | — |
| Bấm nút giữ vé, "Đăng nhập trước", `mailto:`, liên kết `Vé của tôi`, bộ chuyển ngôn ngữ, menu tài khoản trong khung | — | Không làm gì: nút `disabled` (giữ vé) hoặc phần tử nằm trong vùng `inert`; con trỏ `not-allowed`; `title` = `studio.preview.noHold` cho nút giữ vé | — |
| Bấm "Sửa thông tin" / "Sửa loại vé" / "Sửa sơ đồ" | — | Điều hướng `…/info` / `…/ticket-types` / `…/map` | — |
| Bấm "Sang bước Xuất bản" | — | Điều hướng `…/publish` (DOC-59) | — |
| Đồng hồ "Mở bán sau" trong khung về 0 | — | **Không** gọi E-07; tính lại ánh xạ §5 với `now` mới (bản nháp chuyển `UPCOMING → ON_SALE` cục bộ; sự kiện đã xuất bản: gọi lại E-26 một lần) | — |

**Chế độ xem trước của view người mua** (`PreviewModeContext = true`, DR-197): view không tự gọi API (dữ liệu nhận qua props), không hỏi E-09, không ghi `localStorage`, không tải trước chunk `seat-picker`/`checkout`, không điều hướng router của ứng dụng (liên kết nội bộ chuyển thành callback đổi `page`), bỏ điều kiện `displayStatus` khỏi bộ đếm của Chọn số lượng (bộ đếm bấm được cả khi `UPCOMING`, để người tổ chức thử) nhưng nút giữ vé luôn vô hiệu. Header người mua trong khung (logo, `LanguageSwitch`, `AccountMenu`) vẽ ở trạng thái khách và bị `inert`. Ngôn ngữ trong khung theo ngôn ngữ đang dùng của Studio.

Luồng: FL-08 (DOC-84), bước trước `publish-checks`.

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Máy tính, trang sự kiện, chỉ GA ("Studio 05") | `device=desktop`, `page=event`, không loại vé `SEAT`/`ZONE` | Wireframe 1 |
| 2 | Điện thoại, trang sự kiện, chỉ GA ("Studio 05" điện thoại) | `device=phone`, `page=event` | Khung 390 × 844; thanh dưới "Giá từ" + nút chính của DOC-43 trong khung |
| 3 | Trang chọn vé, chỉ GA | `page=select`, chỉ GA | `QuantityPickerView` với "Còn {gaCapacity − used} vé"; nút giữ vé vô hiệu |
| 4 | Máy tính hoặc điện thoại, có sơ đồ (P5) | Có loại vé `SEAT`/`ZONE`, có phiên bản sơ đồ | Trang sự kiện như 1–2; trang chọn vé là `SeatPickerView` mọi ghế còn trống; tóm tắt có `{seats} ghế` |
| 5 | Có loại vé Ghế/Khu vực, chưa có sơ đồ | Có `SEAT`/`ZONE`, `latestVersionNo = 0` (luôn đúng ở P2–P3) | Trang sự kiện như 1–2 (bảng loại vé vẫn có dòng `SEAT`/`ZONE`); trang chọn vé thay bằng `Notice` `studio.preview.mapMissing` + liên kết `studio.preview.editMap` |
| 6 | Thông tin chưa đủ | Bản nháp thiếu tên địa điểm, giờ | View hiện chữ thay thế `studio.info.preview.noTime`, `…noVenue`, `…notSet` ở đúng chỗ; không chặn xem trước |
| 7 | Chưa có loại vé | `ticketTypes = []` | Trang sự kiện không có bảng loại vé và "Giá từ"; trang chọn vé `QuantityPickerView` rỗng; `Notice` `info` `studio.types.empty.body` trên khung với liên kết `studio.preview.editTypes` |
| 8 | Sắp mở bán (bản nháp) | `DRAFT`, `saleStartsAt > now` hoặc chưa đặt | View `UPCOMING` (nút "Mở bán sau" có đếm ngược theo giờ server, DR-66) |
| 9 | Mở bán ngay khi xuất bản | `DRAFT`, `saleStartsAt ≤ now` | View `ON_SALE`; "Mua vé" chuyển sang trang chọn vé |
| 10 | Đã xuất bản | `status ≠ DRAFT` | `intro.live`; view theo `displayStatus` thật của E-26 (`ON_SALE`, `SOLD_OUT`, `PAUSED`, `SALE_CLOSED`, `ENDED`, `CANCELLED`), số còn = `gaCapacity − used`; cột tóm tắt thêm `Button text` `studio.publish.openPublic` → `/events/{eventId}` (tab mới) |
| 11 | Đang tải | Chờ E-26 > 150 ms | `Skeleton` của khung (hình trang sự kiện) và cột tóm tắt; dòng ẩn `studio.preview.loading` (key mới) |
| 12 | Lỗi tải | E-26 lỗi (không phải 404, 401, 403) | `LoadError`: `common.loadError.label`, `studio.preview.error.title`, `studio.preview.error.body` (key mới), `common.retry` |
| 13 | Không quyền, không tìm thấy | 404, 403 | Như DOC-56 §7 dòng 17 |

Màn không có trạng thái rỗng riêng (dòng 7 thay), không có trạng thái quá tải riêng ngoài tự thử lại của E-26.

Tiêu điểm: vào màn → `h1`; đổi khung hoặc trang → tiêu điểm giữ ở nút radio vừa chọn, khung cuộn về đầu; `Tab` đi vào khung chỉ tới các phần tử còn tương tác (bộ đếm, nút đổi trang trong view).

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.4, §3.3, §3.5, §2.2. Key dùng ở màn này (key của trang người mua bên trong khung theo DOC-43 §8 và DOC-47 §8):

| Nhóm | Key |
| --- | --- |
| Khung, bước | `studio.brand`, `studio.breadcrumb.events`, `studio.step.*`, `studio.save.draft` |
| Xem trước | `studio.preview.title`, `.intro`, `.desktop`, `.phone`, `.frame`, `.noHold`, `.willSell`, `.capacity`, `.seatsUnit`, `.ticketsUnit`, `.editInfo`, `.editTypes`, `.editMap`, `.next` |
| Chữ thay thế | `studio.info.preview.noTime`, `studio.info.preview.noVenue`, `studio.info.preview.notSet`, `studio.types.empty.body` |
| Đã xuất bản | `studio.publish.openPublic` |
| Tải | `common.loadError.label`, `common.retry`, `common.loading.slow` |
| **Key mới** (thêm vào DOC-40 §3.4) | `studio.preview.intro.live` "Đây là trang người mua đang thấy. Bản xem trước dùng số vé đã giữ và đã bán lúc tải trang." / "This is the page buyers see now. The preview uses the tickets held and sold when the page loaded."; `studio.preview.page.event` "Trang sự kiện" / "Event page"; `studio.preview.page.select` "Trang chọn vé" / "Ticket selection"; `studio.preview.mapMissing` "Chưa có sơ đồ đã xuất bản nên chưa xem trước được trang chọn chỗ. Vẽ và xuất bản sơ đồ ở bước Sơ đồ." / "There's no published seat map yet, so the seat selection page can't be previewed. Draw and publish the map in the Seat map step."; `studio.preview.loading` "Đang tải bản xem trước…" / "Loading preview…"; `studio.preview.error.title` "Không tải được bản xem trước" / "Couldn't load the preview"; `studio.preview.error.body` "Sự kiện và việc bán vé không bị ảnh hưởng. Kiểm tra kết nối mạng rồi thử lại." / "Your event and ticket sales aren't affected. Check your connection and try again." |

## 9. Tiêu chí nghiệm thu

Tiền tố `SPV-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| SPV-AC-01 | Sự kiện `DRAFT` chỉ GA, `saleStartsAt` sau `now` 6 ngày | Mở màn ở cửa sổ 1440 px | Khung Máy tính, trang sự kiện; nhãn "Sắp mở bán"; nút "Mở bán sau 6 ngày …"; dải `studio.preview.noHold` ở đầu khung |
| SPV-AC-02 | Như 01 | Quan sát mạng | Chỉ có E-26 và E-13 (ảnh); **không** có E-07, E-09 hay request ghi nào trong suốt phiên |
| SPV-AC-03 | Như 01 | Chọn "Điện thoại · 390 px" | Khung 390 px; bố cục trang sự kiện là bản điện thoại của DOC-43 (một cột, thanh dưới cố định **trong** khung) dù cửa sổ rộng 1440 px; URL có `device=phone` |
| SPV-AC-04 | Như 01 | Chọn "Trang chọn vé", bấm `+` hai lần ở "Phổ thông" 450.000 | "2 vé", tạm tính `900.000 ₫` (`vi`) / `₫900,000` (`en`); nút giữ vé vô hiệu, `title` = noHold; `localStorage` không có khóa `tb.selection.*` |
| SPV-AC-05 | 3 loại GA sức chứa 100, 300, 50 | Quan sát cột tóm tắt | "Tổng sức chứa 450 vé"; "Sẽ mở bán" đúng giờ địa phương của sự kiện, có hậu tố offset khi múi giờ khác `PLATFORM_TIMEZONE` (DOC-40 §2.2) |
| SPV-AC-06 | `DRAFT`, `saleStartsAt` đã qua | Mở màn | View `ON_SALE` với nút "Mua vé"; bấm → trang chọn vé trong khung, URL `page=select`, đường dẫn trang không đổi |
| SPV-AC-07 | Bản nháp chưa có địa điểm và giờ | Mở màn | Chữ "Chưa có địa điểm", "Chưa đặt thời gian" ở đúng ô; không lỗi |
| SPV-AC-08 | Có loại vé `ZONE`, chưa có sơ đồ (P2) | Chọn "Trang chọn vé" | `studio.preview.mapMissing` + "Sửa sơ đồ"; trang sự kiện vẫn liệt kê loại vé `ZONE` |
| SPV-AC-09 | Sự kiện `PUBLISHED`, `ON_SALE`, GA 300 `used 48` | Mở màn | `intro.live`; nhãn "Đang mở bán"; trang chọn vé "Còn 252 vé"; có "Mở trang công khai" |
| SPV-AC-10 | Bấm "Sang bước Xuất bản" | — | Điều hướng `/studio/events/{id}/publish` |
| SPV-AC-11 | Trong khung, `Tab` qua mọi phần tử | — | Không tiêu điểm nào vào liên kết `mailto:`, bộ chuyển ngôn ngữ, menu tài khoản của khung; UX-01 vẫn đếm 1 nút chính ngoài khung |
| SPV-AC-12 | `device=tablet&page=foo` | Mở màn ở cửa sổ 800 px | Khung Điện thoại, trang sự kiện; không E1 |
| SPV-AC-13 | E-26 lỗi 500 | Mở màn | `LoadError` `studio.preview.error.*`; "Thử lại" gọi lại E-26 |
| SPV-AC-14 | axe-core | Trạng thái 1, 2, 3, 5, 10 | 0 vi phạm; hai nhóm chọn là `radiogroup` có nhãn; khung có `role="region"` và nhãn |

## 10. Ca kiểm thử E2E

Playwright, `frontend/e2e/studio/`; API thật trên `make up seed`, mock cho lỗi; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| SPV-01 | `studio-preview-ga-desktop` | Tạo sự kiện GA 3 loại vé (như STT-01), sang Xem trước | Trang sự kiện trong khung đúng tên, giá, "Sắp mở bán"; không request E-07/E-09 (SPV-AC-02); khớp P2-05.2 |
| SPV-02 | `studio-preview-ga-phone-select` | Như 01; chọn Điện thoại, Trang chọn vé; bấm `+` | SPV-AC-03, SPV-AC-04 |
| SPV-03 | `studio-preview-to-publish` | Như 01; "Sang bước Xuất bản" | Màn Xuất bản (DOC-59) với danh sách điều kiện |
| SPV-04 | `studio-preview-published` | Seed sự kiện GA đã xuất bản có 2 vé đã bán; mở Xem trước | SPV-AC-09 |
| SPV-05 | `studio-preview-seat-map` (P5) | Sự kiện có sơ đồ đã xuất bản phiên bản 1; Trang chọn vé | Sơ đồ mọi ghế còn trống; chọn 2 ghế cục bộ; không request giữ vé (khớp P5-06.2) |

## Quyết định phát sinh khi viết tài liệu này

**DR-197 · Xem trước dựng lại view người mua trong một khung `div` dùng container query; chế độ xem trước của view.** *Vấn đề:* canvas "Studio 05" cho xem trang người mua ở khung máy tính và điện thoại, nhưng (a) `iframe` cùng nguồn bị CSP chặn (`frame-ancestors 'none'`, `frame-src` không có `'self'`, DOC-32), (b) bố cục người mua đổi theo `@media` của cửa sổ nên khung 390 px trong cửa sổ 1440 px vẫn ra bản máy tính, (c) trang người mua tự gọi E-07/E-09 (404 với `DRAFT`) và ghi `localStorage`, (d) nút "Mua vé" không bấm được khi `UPCOMING` nên không xem được trang chọn vé. *Quyết định:* `PreviewViewport` là `div` có `container-type: inline-size; container-name: app`; điểm ngắt bố cục của các màn người mua (≥ 1024 px hai cột) viết bằng `@container app (min-width: 1024px)` và gốc ứng dụng cũng là container `app` (ở trang thường container rộng bằng cửa sổ nên kết quả như `@media`). `EventDetail`, `QuantityPicker`, `SeatPicker` tách phần trình bày (`…View`, nhận dữ liệu qua props) khỏi phần route (gọi API); `PreviewModeContext` tắt gọi API, hỏi theo nhịp, `localStorage`, tải trước chunk, điều hướng và nút giữ vé. Màn có nhóm chọn "Trang sự kiện | Trang chọn vé", lưu ở query `page`; khung ở query `device`. *Hệ quả:* DOC-39 §1 (hoặc mục bố cục) ghi quy tắc `@container app` thay `@media` cho điểm ngắt 1024 px của màn người mua; DOC-43 §4, DOC-47 §4, DOC-46 ghi tách `…View`; DOC-38 §3 dòng `…/preview` ghi hai query `device`, `page`; không đổi CSP. *Ghi vào:* DOC-58, DOC-38, DOC-39, DOC-43, DOC-46, DOC-47.

**DR-198 · E-26 trả thêm `holdMinutes`, `maxUnitsPerHold`, `organizer`; định nghĩa `steps.preview` và `steps.publish`.** *Vấn đề:* view người mua cần `holdMinutes` (nút "Giữ vé trong 10 phút", "Cách mua"), `maxUnitsPerHold` (trần bộ đếm, DR-187) và `organizer.name`/`contactEmail` (khối người tổ chức), nhưng `OrganizerEventView` của E-26 không có, E-07 trả 404 với `DRAFT`, và E-04 không có `contactEmail`. DOC-37 §7 định nghĩa `steps.info`, `steps.ticketTypes`, `steps.seatMap` nhưng không nói khi nào `preview`, `publish` là `DONE`, trong khi `StudioStepper` chỉ cho bấm bước đã xong hoặc đang ở. *Quyết định:* `OrganizerEventView` thêm `holdMinutes` (int), `maxUnitsPerHold` (int) cùng nguồn với E-07, và `organizer: { name, contactEmail }` cùng cách tính với E-07 (`AuthApi.organizerPublicInfo`, DR-165). `steps.preview = DONE` khi `info`, `ticketTypes` là `DONE` và `seatMap` là `DONE` hoặc `null` (server không ghi nhận "đã xem"); `steps.publish = DONE` khi `status ≠ DRAFT`. *Hệ quả:* DOC-37 §7 (ví dụ `OrganizerEventView` và đoạn `steps`) thêm ba trường và hai quy tắc; DOC-20 §6.7 dòng E-26 thêm nguồn `holdMinutes`, `maxUnitsPerHold`, `organizer`; bước Xem trước bấm được ngay khi Thông tin và Loại vé xong. *Ghi vào:* DOC-37, DOC-20, DOC-58, DOC-59 (thanh bước).

**DR-199 · Xem trước có sơ đồ đọc tài liệu của phiên bản sơ đồ mới nhất qua endpoint studio (P5).** *Vấn đề:* trang chọn chỗ trong khung cần tài liệu sơ đồ **đã xuất bản** mới nhất (cái người mua sẽ thấy, DR-27), nhưng E-08 trả 404 với `DRAFT`, còn E-40/E-43 chỉ trả bản nháp (`draft`) và tóm tắt `latestVersion` không có `document`. *Quyết định:* thêm `GET /organizer/maps/{seatMapId}/versions/{versionNo}` (quyền chủ sơ đồ, response cùng dạng E-08: `versionNo`, `checksum`, `seatCount`, `document`; `Cache-Control: private, max-age=86400, immutable`, `ETag` = `checksum`); màn này gọi với `versionNo = latestVersionNo` của E-40. Làm ở P5 cùng P5-06.2. *Hệ quả:* DOC-37 §8 thêm một endpoint `E-xx` mới khi viết phần P5; P2 không đổi. *Ghi vào:* DOC-37 (P5), DOC-58, DOC-57.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-197…199 (mục trên).
