# Màn hình Studio: Thông tin sự kiện

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-55
> Phụ thuộc: SDD gốc §6.1, §6.3, §13.1, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 23, 24, 25, 29, 38, 41, 52, 57, 66, 70, 85, 89, 132, 143, 162, 163), [DOC-02](../../01-product/personas-and-journeys.md) (J-3 bước 2 và 9, J-4 bước 1), [DOC-03](../../01-product/requirements.md) (FR-02.1…4, FR-02.16, FR-17.6, FR-18.1…2, FR-19.1…3), [DOC-04](../../01-product/use-cases.md) (UC-07, UC-17), [DOC-06](../../02-glossary.md), [DOC-18](../../05-data/data-lifecycle.md) §4, [DOC-20](../../06-design/events-and-ticket-types.md) §4, §6.5, §6.8, [DOC-37](../../07-api/api-endpoints.md) (E-25, E-26, E-27, E-39, E-13), [DOC-38](../ux-principles-and-ia.md) §2.2, §3, §6.3, §8.2…8.5, §9, [DOC-39](../design-system.md) §7.1, §7.2, §7.9, §7.13, §7.14, §7.15, §7.17, §9, [DOC-40](../ui-states-and-copy.md) §1, §2.2, §3.1, §3.4, §3.8, §4.1, [DOC-41](README.md) §1.2, §2, §3, [DOC-54](studio-overview.md), [DOC-56](studio-ticket-types.md), [DOC-59](studio-publish.md), [DOC-82](../../06-design/flows/README.md) (FL-06, FL-11); canvas: artboard "Studio 02"
> Người dùng chính: P2-03.2 (Studio 02), P3-07 (thông báo đổi lịch trên giao diện), tác giả DOC-84 (FL-06, FL-11), người viết test E2E soạn sự kiện

Tài liệu mô tả bước **Thông tin** của Studio: tạo sự kiện ở `/studio/events/new` và sửa ở `/studio/events/:eventId/info`, cùng một biểu mẫu `StudioEventInfo` với hai chế độ (DOC-41 §3 dòng 4). Quy tắc trường, đổi giờ địa phương ↔ UTC và transaction `PATCH` ở DOC-20 §4, §6.5; lưu ảnh ở DOC-20 §6.8 và ADR-0015; schema request/response ở DOC-37 E-25…27, E-39; lời gọi qua các tầng ở FL-06, FL-11 (DOC-84). Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.4, §3.8).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-4 (sự kiện chỉ GA, P2), PS-3 (có sơ đồ, P5); máy tính ≥ 1024 px, vẫn dùng được một cột dưới 1024 px (DOC-38 §6.3) |
| Use case | UC-07 bước 2–3 (tạo nháp, nhập thông tin, ảnh, lưu với `rowVersion`); UC-17 (đổi giờ hoặc địa điểm sau khi bán) |
| Yêu cầu | FR-02.1, FR-02.2 (lỗi theo ô), FR-02.3, FR-02.4, FR-02.16, FR-17.6, FR-18.1, FR-18.2, FR-19.1…3 |
| Hành trình | J-3 bước 2 và 9, J-4 bước 1 |
| Luồng chi tiết | FL-06 (tạo và sửa, kèm ảnh), FL-11 (đổi giờ hoặc địa điểm) |
| Quyền | Vai trò `ORGANIZER`; ở `…/:eventId/info` còn phải là chủ sự kiện. Chưa đăng nhập → E2 (DOC-38 §8.5); chưa có hồ sơ (403 `ORGANIZER_PROFILE_REQUIRED`) → `/studio/profile`; không có vai trò → E3; sự kiện của tổ chức khác hoặc không tồn tại → E1 (404, DR-23) |

## 2. URL và search params

| Route | Chế độ | Tham số | Quy tắc |
| --- | --- | --- | --- |
| `/studio/events/new` | Tạo | — | Không gọi E-26. Lần lưu đầu tạo sự kiện rồi `replace` sang `/studio/events/{eventId}/info` (§6, DR-225) |
| `/studio/events/:eventId/info` | Sửa | `eventId` | UUID (DR-11); sai định dạng → E1, không gọi API; E-26 trả 404 → E1 |

Không có search param. Thay đổi chưa lưu chỉ nằm trong bộ nhớ của form (không ghi `localStorage`); rời trang khi còn thay đổi chưa lưu thì hỏi (DOC-38 §8.4).

## 3. Wireframe

Desktop (≥ 1024 px), canvas "Studio 02", biến thể **Bản nháp** của sự kiện chỉ GA. Cột trái là biểu mẫu, cột phải là khung "Người mua sẽ thấy" (dính khi cuộn):

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ STUDIO   Sự kiện của bạn / Hòa nhạc Giao Mùa                  [vi|en] [GM ▾] │
│ (1)Thông tin ─ (2)Loại vé ─ (3)Xem trước ─ (4)Xuất bản            [Bản nháp] │ StudioStepper
├──────────────────────────────────────────────────────────────────────────────┤
│ THÔNG TIN SỰ KIỆN                                     ┌────────────────────┐ │
│ CƠ BẢN                                                │ NGƯỜI MUA SẼ THẤY  │ │
│ Tên sự kiện  [Hòa nhạc Giao Mùa                    ]  │ ┌────────────────┐ │ │
│ Mô tả        [Đêm nhạc giao mùa với dàn nhạc…      ]  │ │  ảnh 16:9 hoặc │ │ │
│              [                              84/5.000]  │ │  tem ngày      │ │ │
│ Ảnh sự kiện  ┌──────────────┐ 1920 × 1080 · 420 KB    │ └────────────────┘ │ │
│              │  ảnh 16:9    │ [Đổi ảnh] [Gỡ ảnh]      │ HÒA NHẠC GIAO MÙA  │ │
│              └──────────────┘ Ảnh ngang tỷ lệ 16:9…   │ Thứ Năm 12.11.2026 │ │
│ Địa điểm     [Nhà hát Hòa Bình, TP.HCM              ] │ · 19:00 đến 22:00  │ │
│ Múi giờ      [Asia/Ho_Chi_Minh (GMT+7)            ▾] │ Nhà hát Hòa Bình…  │ │
│ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │ Mở bán 10:00 ·     │ │
│ THỜI GIAN DIỄN RA                                     │ 05.11.2026         │ │
│ Bắt đầu [12.11.2026 19:00]  Kết thúc [12.11.2026 22:00]│ Phòng chờ trước giờ│ │
│ Mỗi sự kiện là một suất diễn: một địa điểm, một khung… │ mở bán: Bật        │ │
│ KHUNG MỞ BÁN                                          │ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │ │
│ Mở bán từ [05.11.2026 10:00]  Đóng bán lúc [12.11.2026 17:00]│ Bản nháp chỉ   │ │
│ Tiền tố mã vé [GM]  Hai chữ cái in hoa, ví dụ GM → …  │ mình bạn thấy…     │ │
│ [■] Bật phòng chờ trước giờ mở bán                    └────────────────────┘ │
│     Dành cho sự kiện đông người…                                             │
│ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │
│                                 [Lưu bản nháp]  [LƯU VÀ SANG LOẠI VÉ       ] │ Button primary
└──────────────────────────────────────────────────────────────────────────────┘
```

Biến thể **Lỗi theo ô** (canvas "Studio 02") thêm `Notice` `error` đầu biểu mẫu và lỗi dưới từng ô:

```text
│ ┌ ▲ Còn 2 mục cần sửa ─────────────────────────────────────────┐ │
│ │ Sửa các ô được đánh dấu bên dưới rồi lưu lại.                │ │
│ └──────────────────────────────────────────────────────────────┘ │
│ Địa điểm     [                                     ] ← viền đỏ  │
│              ▲ Hãy nhập địa điểm.                               │
│ Đóng bán lúc [12.11.2026 21:00]                    ← viền đỏ    │
│              ▲ Đóng bán không được muộn hơn giờ bắt đầu sự kiện.│
```

Biến thể **Đang mở bán** (canvas "Studio 02"): `Notice` `info` `studio.info.live.*` đầu biểu mẫu; ô Múi giờ và Tiền tố mã vé khóa; ô "Mở bán từ" khóa từ giờ mở bán; thanh bước thêm "Bán vé"; hàng nút còn một nút chính "Lưu thay đổi". Sau khi lưu đổi giờ có người mua:

```text
│ ┌ ✓ Đã lưu. Đã gửi thông báo tới 48 người mua. ────────────────┐ │
```

Màn hẹp (390 px, không có artboard riêng; DOC-38 §6.3): một cột, khung "Người mua sẽ thấy" nằm **dưới** hàng nút, thanh bước xuống hàng thứ hai:

```text
┌──────────────────────────────┐
│ STUDIO            [vi] [GM▾] │
│ (1)Thông tin (2)Loại vé (3)… │
│ THÔNG TIN SỰ KIỆN [Bản nháp] │
│ Tên sự kiện                  │
│ [Hòa nhạc Giao Mùa         ] │
│ Mô tả                        │
│ [                          ] │
│ Ảnh sự kiện                  │
│ ┌──────────────────────────┐ │
│ │        ảnh 16:9          │ │
│ └──────────────────────────┘ │
│ [Đổi ảnh] [Gỡ ảnh]           │
│ …(các ô như desktop, mỗi ô   │
│ một hàng)…                   │
│ [Lưu bản nháp]               │
│ [LƯU VÀ SANG LOẠI VÉ       ] │
│ NGƯỜI MUA SẼ THẤY            │
│ …                            │
└──────────────────────────────┘
```

## 4. Vùng và component

Trang là `StudioEventInfo` (`frontend/src/features/studio/StudioEventInfo.tsx`, DOC-41 §1.2), trong khung Studio chung (header, breadcrumb, `StudioStepper`). Form dùng react-hook-form + zod (chunk `studio`, DOC-38 §3.1).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Header, breadcrumb | khung Studio, `AccountMenu` (§7.16) | `studio.brand`, `studio.breadcrumb.events` + tên sự kiện đã lưu (không dịch); chế độ tạo: `studio.overview.create` |
| Thanh bước | `StudioStepper` (§7.13) | Theo `steps` của E-26 (DR-70): Sơ đồ ẩn khi `steps.seatMap = null`; "Bán vé" chỉ khi `status ≠ DRAFT`. Chế độ tạo: chỉ "Thông tin" ở trạng thái "đang ở", các bước khác "chưa tới". Nhãn lưu bên phải: `studio.save.draft` / `studio.save.saved` / `studio.save.unsaved` (DOC-38 §8.4) |
| Tiêu đề | `h1` `studio.info.title` | Tiêu điểm vào `h1` sau điều hướng (DOC-39 §9) |
| Thông báo trạng thái | `Notice` `info` (§7.9) | `status` ∈ {`PUBLISHED`, `PAUSED`}: tiêu đề `studio.info.live.title` khi `displayStatus` ∈ {`ON_SALE`, `SOLD_OUT`}, ngược lại `studio.info.live.titlePublished` (key mới); nội dung `studio.info.live.body`. `ENDED`/`CANCELLED`: `studio.info.readonly` (key mới) |
| Tóm tắt lỗi | `Notice` `error` `studio.info.summary.errors` + `studio.info.summary.fix` | Sau lần lưu có lỗi theo ô (zod hoặc 422); `{count}` = số ô lỗi; bấm vào focus ô lỗi đầu tiên |
| Thông báo lưu | `Notice` `success` | `notifiedOrders > 0` → `studio.info.notified` (key mới, `{count}`); tự ẩn khi sửa ô bất kỳ |
| Nhóm "Cơ bản" | tiêu đề nhóm `studio.info.basic` | |
| Tên | `TextField` (§7.2) `studio.info.name.label` | 1–120 ký tự sau trim, bắt buộc ở mọi trạng thái (DR-25) |
| Mô tả | `TextField` nhiều dòng (`<textarea>`, 6 dòng, tự giãn) `studio.info.description.label` | ≤ 5.000 ký tự; văn bản thuần, giữ xuống dòng; bộ đếm `studio.info.description.counter` (key mới) hiện khi ≥ 4.500 ký tự |
| Ảnh | `EventImage` (§7.14) + `Button secondary` `studio.info.image.choose` / `…change`, `Button text` `…remove`; `<input type="file" accept="image/jpeg,image/png,image/webp">` ẩn | Chưa có ảnh: tem ngày từ `startsAtLocal` (hoặc khung trống nếu chưa đặt giờ) + `studio.info.image.empty`. Có ảnh: kích thước và dung lượng `studio.info.image.meta` (key mới) từ response E-39. Trợ giúp `studio.info.image.hint` + `studio.info.image.limits` (key mới). Đang tải lên: nút `loading` `studio.info.image.uploading` (key mới) |
| Địa điểm | `TextField` `studio.info.venue.label` | ≤ 200 ký tự; trống được khi `DRAFT` (DR-25) |
| Múi giờ | `Combobox` (§7.2) `studio.info.timezone.label` | Danh sách và tìm kiếm theo DR-228; nhãn mỗi mục `Asia/Tokyo (GMT+9)`. `status ≠ DRAFT`: vô hiệu, trợ giúp `studio.info.timezone.locked` (DR-12) |
| Nhóm "Thời gian diễn ra" | tiêu đề `studio.info.time.title`; hai `TextField` `type="datetime-local"` `step="60"`: `studio.info.start.label`, `studio.info.end.label`; trợ giúp `studio.info.time.hint` | Giá trị là giờ địa phương của sự kiện dạng `yyyy-MM-ddTHH:mm` (DR-12, DR-89); đổi múi giờ **không** đổi giá trị trong ô |
| Nhóm "Khung mở bán" | tiêu đề `studio.info.sale.title`; hai `datetime-local`: `studio.info.saleStart.label`, `studio.info.saleEnd.label` | "Mở bán từ" vô hiệu khi `status ≠ DRAFT` và giờ server ≥ `saleStartsAt` (DR-143); "Đóng bán lúc" vô hiệu khi `status ≠ DRAFT` và giờ server ≥ `saleEndsAt` (DR-163); trợ giúp khi vô hiệu: `validation.locked_after_sale_start` / `validation.locked_after_sale_end`. Giờ server = `Date.now() + offset` (DR-66) |
| Tiền tố mã vé | `TextField` 2 ký tự, `autocapitalize="characters"`, chữ thường tự đổi sang hoa `studio.info.prefix.label` | `^[A-Z]{2}$` (DR-52); trợ giúp `studio.info.prefix.hint`; `status ≠ DRAFT`: vô hiệu, trợ giúp `validation.locked_after_publish` |
| Phòng chờ | công tắc (`<input type="checkbox" role="switch">`, vùng chạm 44 px) `studio.info.highDemand.label` + `studio.info.highDemand.hint` | Ghi `highDemand` (DR-57). Không khóa sau xuất bản (E-27 cho đổi) |
| Khung "Người mua sẽ thấy" | thẻ phụ dính khi cuộn: `studio.info.preview.title`; `EventImage`; tên; ngày giờ đầy đủ + khung giờ (DOC-40 §2.2); địa điểm; mốc mở bán; dòng phòng chờ `studio.info.preview.queue.label`: `…queue.on` / `…queue.off`; chú thích `studio.info.preview.draftNote` khi `DRAFT` | Đọc **giá trị form đang gõ**, không chờ lưu. Thiếu giờ → `studio.info.preview.noTime`; thiếu địa điểm → `…noVenue`; thiếu mốc mở bán → `…notSet`. Giờ hiển thị theo múi giờ trên form, hậu tố offset khi khác `PLATFORM_TIMEZONE` (DR-12, DR-229) |
| Hàng nút | `DRAFT` và chế độ tạo: `Button secondary` `studio.save.draftBtn`; `Button primary` `studio.save.next` (`{step}` = `studio.step.ticketTypes`). `PUBLISHED`/`PAUSED`: chỉ `Button primary` `studio.save.changes` (key mới). `ENDED`/`CANCELLED`: không có nút | Đúng một nút chính (UXP-01). Đang gửi: nút bấm `loading` `studio.save.saving` (key mới), mọi ô vô hiệu |
| Hộp thoại đổi lịch | `ConfirmDialog` (§7.15), nút phải `primary` | Trước khi lưu thay đổi `startsAtLocal`, `endsAtLocal` hoặc `venue` của sự kiện đã xuất bản (DR-226); key mới `studio.info.reschedule.confirm.*` |
| Hộp thoại phiên bản cũ | `ConfirmDialog` một nút `studio.stale.*` | 409 `STALE_EVENT_VERSION` (DR-70, DOC-38 §8.4) |
| Hộp thoại rời trang | `ConfirmDialog` `common.unsaved.*` | DOC-38 §8.4 |

**Khác canvas** (ghi cả ở DOC-38 §9): bỏ ô "Số vé tối đa mỗi đơn" và dòng "Mỗi đơn" ở khung "Người mua sẽ thấy" (DR-41); công tắc phòng chờ là `highDemand`, nhãn theo DOC-40 §3.4 (DR-57); thêm ô "Múi giờ" (DR-12) và "Tiền tố mã vé" (DR-52) mà canvas chưa vẽ; thêm hộp thoại xác nhận đổi lịch (DR-226).

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Sự kiện | `GET /organizer/events/{eventId}` (E-26) | Vào màn (chế độ sửa); sau "Tải lại" ở hộp thoại phiên bản cũ; `refetchOnWindowFocus` **chỉ khi form không có thay đổi chưa lưu** | `OrganizerEventView`: `status`, `displayStatus`, `name`, `description`, `venue`, `timezone`, `imageMediaId`, `imageUrl`, `startsAtLocal`, `endsAtLocal`, `saleStartsAtLocal`, `saleEndsAtLocal`, `saleStartsAt`, `saleEndsAt`, `highDemand`, `ticketCodePrefix`, `rowVersion`, `steps`. Khóa truy vấn `['organizerEvent', eventId]` dùng chung với DOC-56, DOC-58, DOC-59 |
| Tạo nháp | `POST /organizer/events` (E-25) | Lần lưu đầu ở chế độ tạo | `{ "name" }`; response `OrganizerEventView` `rowVersion = 0` |
| Lưu | `PATCH /organizer/events/{eventId}` (E-27) | "Lưu bản nháp", "Lưu và sang Loại vé", "Lưu thay đổi" | `rowVersion` + **chỉ trường đã đổi** so với bản E-26 gần nhất; response `{ event, notifiedOrders }` ghi thẳng vào cache `['organizerEvent', eventId]` |
| Ảnh | `POST /organizer/media` (E-39), `multipart/form-data` `purpose=EVENT_IMAGE` | Ngay khi chọn tệp (không chờ lưu) | Response `{ mediaId, url, width, height, sizeBytes }`; form đặt `imageMediaId = mediaId` → trạng thái `studio.save.unsaved`. Ảnh tải lên mà không lưu sẽ bị dọn sau 24 giờ (DOC-18 §4) |
| Hiển thị ảnh | `GET /media/{mediaId}` (E-13) | `<img src>` | `object-fit: cover`, 16:9 (DOC-39 §7.14) |

Không có kênh real-time; không hỏi theo nhịp.

Ví dụ sửa sự kiện đã xuất bản (FL-11): E-26 trả `rowVersion: 7`, `venue: "Nhà hát Hòa Bình, TP.HCM"`, `startsAtLocal: "2026-11-12T19:00"`, `endsAtLocal: "2026-11-12T22:00"`. Người tổ chức đổi địa điểm và dời giờ một tiếng, xác nhận hộp thoại đổi lịch:

```http
PATCH /api/v1/organizer/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35
X-CSRF-Token: …
Content-Type: application/json

{"rowVersion":7,"venue":"Nhà hát Thành phố, TP.HCM","startsAtLocal":"2026-11-12T20:00","endsAtLocal":"2026-11-12T23:00"}
```

Response 200 `{ "event": { …, "rowVersion": 8, "startsAt": "2026-11-12T13:00:00Z", "startsAtLocal": "2026-11-12T20:00" }, "notifiedOrders": 48 }` → `Notice` `success` "Đã lưu. Đã gửi thông báo tới 48 người mua."

**Giá trị gửi lên.** Ô văn bản gửi sau `trim()` (server cũng trim). Ô giờ để trống gửi `null` (chỉ hợp lệ khi `DRAFT`). Đổi múi giờ khi `DRAFT` chỉ gửi `timezone` (cộng các ô giờ người dùng đã sửa): server giữ nguyên giờ trên đồng hồ địa phương cho các mốc không có trong request (DOC-20 §4.3), khớp với form vì form không đổi giá trị ô. Gỡ ảnh gửi `"imageMediaId": null`.

**Kiểm tra ở client** (zod, chỉ để báo sớm; server là nguồn cuối, DOC-20 §4.2). Client chạy **đúng bộ quy tắc server chạy cho trạng thái hiện tại**, không hơn, để không chặn điều server chấp nhận:

| Trạng thái | Quy tắc zod |
| --- | --- |
| Chế độ tạo, `DRAFT` | `name` 1–120 ký tự sau trim; `venue` ≤ 200; `description` ≤ 5.000; `ticketCodePrefix` `^[A-Z]{2}$`; giờ đúng định dạng hoặc trống. **Không** kiểm thứ tự các mốc (bản nháp lưu được khi chưa đủ; điều kiện xuất bản ở DOC-59) |
| `PUBLISHED`, `PAUSED` | Mọi quy tắc DOC-20 §4.1 trên bộ giá trị sau khi sửa: `venue` bắt buộc; `endsAt > startsAt`, ≤ 72 giờ; `saleStartsAt < saleEndsAt ≤ startsAt`; `must_be_future` (so với giờ server, DR-66) chỉ cho `startsAtLocal`, `saleEndsAtLocal` **đã đổi** (DR-163). So sánh thứ tự làm trên chuỗi `yyyy-MM-ddTHH:mm` của cùng múi giờ (đủ chính xác trừ khoảng giờ mùa hè; trường hợp đó server quyết định) |
| `ENDED`, `CANCELLED` | Không lưu |

Hằng số độ dài 120/200/5.000 và 72 giờ không phải cấu hình (DOC-34 §3.13) nên client viết cứng; giới hạn ảnh là cấu hình, xem DR-227.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào `…/:eventId/info` | E-26 | Form nạp giá trị; nhãn lưu `studio.save.draft` (`DRAFT`) hoặc `studio.save.saved` | 404 → E1; 403 `ORGANIZER_PROFILE_REQUIRED` → `/studio/profile`; mạng/5xx → `LoadError`; 503/429 → tự thử lại (DR-132) |
| Vào `/studio/events/new` | — | Form trống: `timezone = VITE_PLATFORM_TIMEZONE`, `ticketCodePrefix = "TK"`, `highDemand = false`; focus ô Tên | — |
| Sửa ô bất kỳ | — | Nhãn lưu → `studio.save.unsaved`; lỗi của ô đó và `Notice` `success` bị xóa | — |
| Đổi múi giờ (`DRAFT`) | — | Giá trị các ô giờ giữ nguyên; khung xem trước tính lại hậu tố offset | — |
| Chọn ảnh | E-39 | Kiểm trước ở client: loại ∉ JPEG/PNG/WebP → lỗi dưới ô `errors.media_invalid.body`, không gửi; > 2 MB → `errors.payload_too_large.body`, không gửi (DR-227). Hợp lệ → gửi; 201 → ô ảnh hiện `url`, `studio.info.image.meta`; nhãn lưu `unsaved` | 413 `PAYLOAD_TOO_LARGE` (cả 413 của nginx không có body JSON) → `errors.payload_too_large.body` dưới ô ảnh; 422 `MEDIA_INVALID` → `errors.media_invalid.body`; 503 `OVERLOADED` hoặc mạng → `Notice` `warning` dưới ô ảnh + nút `common.retry` (không tự thử lại: tải lại tệp lớn tốn băng thông); ảnh cũ giữ nguyên |
| Gỡ ảnh | — | `imageMediaId = null`, ô ảnh về tem ngày; nhãn lưu `unsaved` | — |
| Bấm "Lưu bản nháp" | E-27 (chế độ tạo: E-25 rồi E-27) | Chạy **thủ tục lưu**; thành công → ở lại, nhãn `studio.save.saved` | Bảng lỗi dưới |
| Bấm "Lưu và sang Loại vé" | như trên | Thủ tục lưu; thành công → `…/ticket-types`. Form không đổi gì → điều hướng ngay, không gọi API | Có lỗi → ở lại |
| Bấm "Lưu thay đổi" (đã xuất bản) | E-27 | Đổi `startsAtLocal`, `endsAtLocal` hoặc `venue` → mở hộp thoại đổi lịch trước; xác nhận → thủ tục lưu; thành công → ở lại; `notifiedOrders > 0` → `Notice` `success` `studio.info.notified` | "Chưa lưu" ở hộp thoại → đóng, form giữ nguyên |
| Bước khác trên `StudioStepper`, breadcrumb, liên kết header | — | Không có thay đổi chưa lưu → điều hướng. Có → hộp thoại `common.unsaved.*`; "Rời đi" bỏ thay đổi | — |
| Đóng tab hoặc tải lại khi có thay đổi chưa lưu | — | `beforeunload` của trình duyệt | — |

**Thủ tục lưu:**

1. Chờ ảnh đang tải lên xong (nút lưu vô hiệu trong lúc tải ảnh).
2. Kiểm zod theo §5; có lỗi → lỗi theo ô, tóm tắt `studio.info.summary.*`, focus ô lỗi đầu tiên; dừng.
3. **Chế độ tạo** (DR-225): gửi E-25 `{ "name" }`. 201 → nếu form còn trường khác giá trị mặc định thì gửi E-27 với `rowVersion` 0 và các trường đó; sau cùng `navigate('/studio/events/{eventId}/info', { replace: true })` (hoặc `…/ticket-types` nếu bấm "Lưu và sang"). E-27 lỗi 422 → vẫn `replace` sang `…/info` của sự kiện vừa tạo, form giữ giá trị và hiện lỗi theo ô. E-25 **không tự thử lại** khi lỗi mạng (không idempotent): hiện `studio.info.saveFailed` (key mới), người dùng bấm lại.
4. **Chế độ sửa:** gửi E-27 với `rowVersion` của bản E-26 gần nhất và các trường đã đổi.
5. 200 → `setQueryData(['organizerEvent', id], event)`, form reset về `event` (giá trị mới thành mốc so sánh), nhãn `studio.save.saved`.

Lỗi của E-25, E-27 (DOC-40 §4.1, §3.8):

| HTTP · `code` | Hiển thị | Hành động |
| --- | --- | --- |
| 422 `VALIDATION_FAILED` `errors[{field, rule}]` | Lỗi dưới ô theo bảng ánh xạ dưới; tóm tắt `studio.info.summary.*` | Form giữ giá trị |
| 409 `STALE_EVENT_VERSION` (`currentRowVersion`) | Hộp thoại `studio.stale.*` một nút "Tải lại" | "Tải lại" → gọi lại E-26, reset form (mất thay đổi của tab này, DR-70) |
| 409 `EVENT_STATE_CONFLICT` | `Notice` `error` `studio.publish.notice.stateConflict` | Gọi lại E-26; form sang biến thể chỉ đọc nếu `ENDED`/`CANCELLED` |
| 404 `NOT_FOUND` | — | E1 |
| 403 `CSRF_TOKEN_INVALID` | — | Lấy token mới, gửi lại đúng một lần (DR-85) |
| 503 `OVERLOADED`, 429 `RATE_LIMITED` | `Notice` `warning` `errors.rate_limited.*` / `common.retry.auto` | E-27: tự thử lại cùng body theo DR-132 (an toàn vì `rowVersion` chặn ghi hai lần: lần đầu đã commit thì lần sau nhận 409 `STALE_EVENT_VERSION`, xem dưới). E-25: không tự thử lại (bước 3) |
| Lỗi mạng ở E-27 | `studio.info.saveFailed` | Không tự thử lại; người dùng bấm lưu lại. Nếu lần đầu đã commit, lần bấm lại nhận 409 `STALE_EVENT_VERSION` với `currentRowVersion = rowVersion + 1`: client gọi E-26, nếu mọi trường đã gửi khớp giá trị server thì coi như đã lưu (không hiện hộp thoại, `notifiedOrders` không biết nên không hiện thông báo gửi email); ngược lại hiện hộp thoại phiên bản cũ |
| 401 `UNAUTHENTICATED` | — | E2 với `returnTo` (DOC-38 §8.5); thay đổi chưa lưu mất |

Ánh xạ `field` → ô và key lỗi. `field` là tên trường của body (`…Local`, DR-89); client bỏ hậu tố `Local` trước khi tra key `validation.required.<tên>` (DOC-40 §3.8 dùng `startsAt`, `saleEndsAt`…):

| `field` | Ô | `rule` → key |
| --- | --- | --- |
| `name` | Tên | `required` → `validation.required.name`; `too_long` → `validation.too_long` (`{max}` 120) |
| `venue` | Địa điểm | `required` → `validation.required.venue`; `too_long` → `validation.too_long` (`{max}` 200) |
| `description` | Mô tả | `too_long` → `validation.too_long` (`{max}` 5.000) |
| `timezone` | Múi giờ | `invalid_timezone`; `locked_after_publish` → `studio.info.timezone.locked` |
| `startsAtLocal` | Bắt đầu | `required` → `validation.required.startsAt`; `must_be_future`; `nonexistent_local_time` (DR-162) |
| `endsAtLocal` | Kết thúc | `required` → `validation.required.endsAt`; `must_be_after_start`; `too_long_event` |
| `saleStartsAtLocal` | Mở bán từ | `required` → `validation.required.saleStartsAt`; `locked_after_sale_start` (DR-143) |
| `saleEndsAtLocal` | Đóng bán lúc | `required` → `validation.required.saleEndsAt`; `must_be_after_sale_start`; `after_event_start`; `must_be_future`; `locked_after_sale_end` (DR-163) |
| `imageMediaId` | Ảnh | `invalid_media` |
| `ticketCodePrefix` | Tiền tố mã vé | `invalid_prefix`; `locked_after_publish` |
| khác hoặc `field` rỗng | — | `Notice` `error` `errors.generic` (lỗi lập trình, ghi log) |

Rule không có trong bảng → `validation.<rule>` nếu key tồn tại, ngược lại `errors.generic` (DOC-40 §4.4).

**Hai tab cùng sửa:** tab B lưu sau tab A nhận 409 `STALE_EVENT_VERSION` (FR-02.3); tab B không tự gọi lại E-26 khi đang có thay đổi chưa lưu nên không mất chữ đang gõ cho tới khi người dùng bấm "Tải lại".

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Đang tải (canvas "Studio 02", biến thể "Đang tải") | E-26 chưa về quá 150 ms | `Skeleton` giữ hình biểu mẫu và khung xem trước (DOC-40 §1.1), `role="status"` `studio.info.loading` (key mới); thanh bước và hàng nút ẩn |
| 2 | Bản nháp (canvas "Studio 02", biến thể "Bản nháp") | `status = DRAFT` | Mọi ô sửa được; hai nút "Lưu bản nháp" và "Lưu và sang Loại vé"; khung xem trước có `studio.info.preview.draftNote` |
| 3 | Lỗi theo ô (canvas "Studio 02", biến thể "Có lỗi") | zod hoặc 422 `VALIDATION_FAILED` | Tóm tắt `studio.info.summary.*`; ô lỗi viền 2 px `--stamp`, thông báo `role="alert"` dưới ô (DOC-39 §7.2) |
| 4 | Đang mở bán (canvas "Studio 02", biến thể "Đang bán") | `status` ∈ {`PUBLISHED`, `PAUSED`} | `Notice` `info` `studio.info.live.*`; Múi giờ, Tiền tố khóa; "Mở bán từ"/"Đóng bán lúc" khóa theo giờ (§4); một nút `studio.save.changes`; thanh bước có "Bán vé" |
| 5 | Tạo mới (không có trong canvas) | `/studio/events/new` | Như #2 nhưng thanh bước chỉ có bước đầu bấm được; breadcrumb `studio.overview.create`; nhãn lưu `studio.save.draft`. Form chỉ có giá trị mặc định → rời trang không hỏi |
| 6 | Đang lưu | Thủ tục lưu đang chạy | Nút bấm `loading` `studio.save.saving`; mọi ô `disabled` |
| 7 | Đã lưu, đã gửi thông báo | E-27 200 với `notifiedOrders > 0` | `Notice` `success` `studio.info.notified` đầu biểu mẫu; nhãn `studio.save.saved` |
| 8 | Đang tải ảnh / lỗi ảnh | Đang chờ E-39; E-39 lỗi | Nút ảnh `loading` `studio.info.image.uploading`; lỗi dưới ô ảnh (§6) |
| 9 | Phiên bản cũ | 409 `STALE_EVENT_VERSION` | Hộp thoại `studio.stale.*` |
| 10 | Chỉ đọc (không có trong canvas) | `status` ∈ {`ENDED`, `CANCELLED`} | `Notice` `info` `studio.info.readonly`; mọi ô `disabled`; không có hàng nút; khung xem trước vẫn hiện |
| 11 | Lỗi tải | E-26 lỗi mạng hoặc 5xx | `LoadError` `studio.info.error.title` + `studio.info.error.body` (key mới; câu hệ quả "Việc bán vé không bị ảnh hưởng", DOC-40 §1.3) + `common.retry` |
| 12 | Quá tải | 503 `OVERLOADED`, 429 `RATE_LIMITED` | `Notice` `warning` tự thử lại (DOC-40 §1.4, DR-132) |
| 13 | Không quyền / không tìm thấy | 403 không có vai trò; 404 | E3; E1 (DOC-52) |
| 14 | Chưa có hồ sơ | 403 `ORGANIZER_PROFILE_REQUIRED` | `replace` tới `/studio/profile` |
| 15 | Màn hẹp (< 1024 px) | `matchMedia('(max-width: 1023px)')` | Một cột; khung xem trước dưới hàng nút (§3) |

Trạng thái rỗng không áp dụng: chế độ tạo (#5) là biểu mẫu trống có sẵn việc kế tiếp (nhập tên).

## 8. Microcopy

Chuỗi có sẵn ở DOC-40: `studio.brand`, `studio.breadcrumb.events`, `studio.step.*`, `studio.save.draft|saved|unsaved|draftBtn|next`, `studio.stale.*`, `studio.info.*` (§3.4), `studio.overview.create`, `studio.publish.notice.stateConflict`, `validation.*` (§3.8), `common.unsaved.*`, `common.retry`, `common.retry.auto`, `errors.payload_too_large.*`, `errors.media_invalid.*`, `errors.rate_limited.*`, `errors.generic.*`.

Key mới (thêm vào DOC-40 §3.4 trong cùng thay đổi khi hợp nhất):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.save.changes` | Lưu thay đổi | Save changes |
| `studio.save.saving` | Đang lưu… | Saving… |
| `studio.info.loading` | Đang tải thông tin sự kiện… | Loading event details… |
| `studio.info.error.title` | Không tải được thông tin sự kiện | Couldn't load the event details |
| `studio.info.error.body` | Việc bán vé không bị ảnh hưởng. Kiểm tra kết nối mạng rồi thử lại. | Ticket sales aren't affected. Check your connection and try again. |
| `studio.info.saveFailed` | Chưa lưu được. Thay đổi vẫn còn trên trang; kiểm tra kết nối mạng rồi bấm lưu lại. | Not saved yet. Your changes are still on the page; check your connection and save again. |
| `studio.info.live.titlePublished` | Sự kiện đã xuất bản | This event is published |
| `studio.info.readonly` | Sự kiện đã kết thúc hoặc đã hủy. Thông tin chỉ để xem. | This event has ended or was cancelled. Details are read-only. |
| `studio.info.notified` | Đã lưu. Đã gửi thông báo tới {count} người mua. | {count, plural, one {Saved. # buyer was notified.} other {Saved. # buyers were notified.}} |
| `studio.info.reschedule.confirm.label` | Thông báo người mua | Notify buyers |
| `studio.info.reschedule.confirm.title` | Gửi thông báo đổi lịch? | Send a schedule change notice? |
| `studio.info.reschedule.confirm.body` | Người đã mua vé sẽ nhận email về thời gian hoặc địa điểm mới. Email đã gửi không thu hồi được. | Buyers will get an email about the new time or venue. Sent emails can't be recalled. |
| `studio.info.reschedule.confirm.keep` | Chưa lưu | Don't save yet |
| `studio.info.reschedule.confirm.cta` | Lưu và gửi thông báo | Save and notify buyers |
| `studio.info.description.counter` | {count}/{max} | {count}/{max} |
| `studio.info.image.meta` | {width} × {height} · {size} | {width} × {height} · {size} |
| `studio.info.image.limits` | JPEG, PNG hoặc WebP, tối đa {maxMb} MB. | JPEG, PNG or WebP, up to {maxMb} MB. |
| `studio.info.image.uploading` | Đang tải ảnh lên… | Uploading image… |
| `studio.info.timezone.empty` | Không tìm thấy múi giờ này. | No matching time zone. |

`{size}` định dạng bằng `Intl.NumberFormat(locale, { style: 'unit', unit: 'kilobyte', maximumFractionDigits: 0 })` (dưới 1 MB) hoặc `unit: 'megabyte'`, `maximumFractionDigits: 1`: `420 kB`, `1,8 MB` (`vi`), `1.8 MB` (`en`).

## 9. Tiêu chí nghiệm thu

Tiền tố `SEI-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| SEI-AC-01 | Tổ chức có hồ sơ | Mở `/studio/events/new` | Form trống, Múi giờ `Asia/Ho_Chi_Minh`, Tiền tố `TK`, công tắc phòng chờ tắt; focus ô Tên; không gọi E-26 |
| SEI-AC-02 | Chế độ tạo, chỉ nhập tên "Hòa nhạc Giao Mùa" | Bấm "Lưu bản nháp" | Đúng một E-25 `{"name":"Hòa nhạc Giao Mùa"}`, **không** có E-27; URL `replace` thành `/studio/events/{id}/info`; nút Back của trình duyệt không quay về `/new` |
| SEI-AC-03 | Chế độ tạo, nhập tên và địa điểm | Bấm "Lưu và sang Loại vé" | E-25 rồi E-27 `{"rowVersion":0,"venue":"…"}`; điều hướng `…/ticket-types` |
| SEI-AC-04 | Chế độ tạo, tên rỗng | Bấm lưu | Không request; lỗi dưới ô Tên "Hãy nhập tên sự kiện."; tóm tắt "Còn 1 mục cần sửa" |
| SEI-AC-05 | `DRAFT` `rowVersion` 3, chỉ sửa Mô tả | Bấm "Lưu bản nháp" | Body E-27 đúng `{"rowVersion":3,"description":"…"}`; sau 200 nhãn "Đã lưu", `rowVersion` trong cache 4 |
| SEI-AC-06 | `DRAFT`, `saleEndsAtLocal` sau `startsAtLocal` | Bấm "Lưu bản nháp" | E-27 được gửi (client không kiểm thứ tự khi `DRAFT`); 200 |
| SEI-AC-07 | Mock E-27 422 với mỗi cặp `field`/`rule` của bảng §6 (14 cặp) | Bấm lưu | Mỗi lỗi hiện dưới đúng ô với đúng chuỗi DOC-40 §3.8 ở cả `vi` và `en`; tóm tắt đếm đúng số ô (P2-03.2) |
| SEI-AC-08 | Mock E-27 422 `{"field":"saleEndsAtLocal","rule":"after_event_start"}` | Bấm lưu | Ô "Đóng bán lúc" viền đỏ, "Đóng bán không được muộn hơn giờ bắt đầu sự kiện."; focus ô đó khi bấm tóm tắt |
| SEI-AC-09 | Mock E-27 409 `STALE_EVENT_VERSION` | Bấm lưu | Hộp thoại "Sự kiện đã được sửa ở nơi khác"; bấm "Tải lại" → E-26 được gọi, form mang giá trị server, nhãn "Đã lưu" |
| SEI-AC-10 | `DRAFT` `Asia/Ho_Chi_Minh`, Bắt đầu `2026-11-14T20:00` | Đổi Múi giờ sang `Asia/Tokyo`, lưu | Ô Bắt đầu vẫn `20:00`; body E-27 chỉ có `rowVersion` và `timezone`; khung xem trước hiện `… · 20:00 GMT+9` |
| SEI-AC-11 | Sự kiện `PUBLISHED` | Mở màn | Múi giờ và Tiền tố vô hiệu kèm trợ giúp khóa; `Notice` `studio.info.live.*`; chỉ một nút "Lưu thay đổi" |
| SEI-AC-12 | `PUBLISHED`, giờ server ≥ `saleStartsAt` | Mở màn | Ô "Mở bán từ" vô hiệu, trợ giúp "Không đổi được giờ mở bán sau khi đã mở bán."; ô "Đóng bán lúc" sửa được |
| SEI-AC-13 | `PUBLISHED`, đổi Địa điểm | Bấm "Lưu thay đổi" | Hộp thoại "Gửi thông báo đổi lịch?"; tiêu điểm ở "Chưa lưu"; bấm "Chưa lưu" → không request |
| SEI-AC-14 | Như 13, E-27 trả `notifiedOrders: 48` | Bấm "Lưu và gửi thông báo" | `Notice` `success` "Đã lưu. Đã gửi thông báo tới 48 người mua." (`en`: "Saved. 48 buyers were notified.") |
| SEI-AC-15 | `PUBLISHED`, chỉ đổi Mô tả | Bấm "Lưu thay đổi" | Không có hộp thoại đổi lịch; E-27 gửi ngay; `notifiedOrders: 0` → không có thông báo gửi email |
| SEI-AC-16 | Chọn ảnh JPEG 1920 × 1080, 430.080 byte | — | E-39 `purpose=EVENT_IMAGE` gửi ngay; ô ảnh hiện ảnh và "1920 × 1080 · 420 kB"; nhãn "Có thay đổi chưa lưu"; lưu → body có `imageMediaId` |
| SEI-AC-17 | Chọn ảnh 3 MB | — | Không gửi E-39; lỗi dưới ô ảnh `errors.payload_too_large.body`. Mock E-39 413 với ảnh 1,5 MB → cùng lỗi (P2-03.2) |
| SEI-AC-18 | Chọn tệp GIF; mock E-39 422 `MEDIA_INVALID` cho tệp PNG | — | GIF không gửi; cả hai hiện `errors.media_invalid.body`; ảnh cũ còn nguyên |
| SEI-AC-19 | Có thay đổi chưa lưu | Bấm bước "Loại vé" trên thanh bước | Hộp thoại "Rời trang mà chưa lưu?"; "Ở lại" giữ form; "Rời đi" điều hướng, không request |
| SEI-AC-20 | Sự kiện `CANCELLED` | Mở màn | `Notice` `studio.info.readonly`; mọi ô vô hiệu; không có nút lưu |
| SEI-AC-21 | Sự kiện của tổ chức khác | Mở `…/info` | E-26 404 → E1 "Trang này không tồn tại" |
| SEI-AC-22 | Mock E-27 503 `Retry-After: 2` rồi 200 | Bấm lưu | `Notice` `warning` tự thử lại; request thứ hai sau 2 giây cùng body byte-by-byte; nhãn "Đã lưu" |
| SEI-AC-23 | Khung "Người mua sẽ thấy", gõ tên mới chưa lưu | Quan sát | Khung hiện tên mới ngay; không có dòng "Mỗi đơn" (DR-41); dòng phòng chờ "Bật"/"Tự bật khi đông" theo công tắc |
| SEI-AC-24 | Bundle | Grep "Số vé tối đa mỗi đơn" | Không có (COPY-10) |
| SEI-AC-25 | Combobox Múi giờ | Gõ "ho chi" | Có mục `Asia/Ho_Chi_Minh (GMT+7)` dù trình duyệt liệt kê `Asia/Saigon`; gõ "xyz" → `studio.info.timezone.empty` |
| SEI-AC-26 | 390 px | Mở màn | Một cột, không cuộn ngang; khung xem trước dưới hàng nút; đúng một nút chính nhìn thấy |
| SEI-AC-27 | axe-core | Mọi trạng thái §7 | 0 vi phạm; ô lỗi có `aria-invalid` và `aria-describedby`; công tắc có `role="switch"` |

## 10. Ca kiểm thử E2E

Playwright; API thật trên `make seed` (tổ chức seed của DR-78) với Testcontainers/compose có SeaweedFS, và mock (MSW, `VITE_API_MOCK=1`) cho lỗi; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| SEI-01 | `studio-info-create-ga` | Từ `/studio` bấm "Tạo sự kiện", nhập tên, địa điểm, giờ (bắt đầu +30 ngày 19:00, kết thúc 22:00, mở bán +1 ngày 10:00, đóng bán ngày diễn 17:00), tiền tố `GM`, "Lưu và sang Loại vé" | URL `…/ticket-types`; `GET /organizer/events/{id}` có `steps.info = "DONE"`, `ticketCodePrefix = "GM"` (khớp P2-03.2) |
| SEI-02 | `studio-info-image` | Tải ảnh JPEG 1920 × 1080, lưu; mở trang xem trước | Ảnh hiện ở khung "Người mua sẽ thấy" và qua `GET /media/{id}` trả 200 `image/jpeg` |
| SEI-03 | `studio-info-field-errors` (mock) | Mock E-27 422 lần lượt từng cặp `field`/`rule` của §6 | SEI-AC-07 |
| SEI-04 | `studio-info-stale` | Mở cùng sự kiện ở hai tab; tab A đổi tên, lưu; tab B đổi mô tả, lưu | Tab B hiện hộp thoại phiên bản cũ; "Tải lại" → tên của tab A, mô tả cũ (FR-02.3) |
| SEI-05 | `studio-info-image-too-large` (mock) | Mock E-39 413 | SEI-AC-17 (P2-03.2) |
| SEI-06 | `studio-info-reschedule` | Seed sự kiện `PUBLISHED` có 3 đơn `PAID` (P3: thẻ; P2: đơn 0 đồng); đổi giờ bắt đầu, xác nhận | Thông báo "Đã gửi thông báo tới 3 người mua"; Mailpit có 3 email `event-changed` (UC-17, khớp P3-07) |
| SEI-07 | `studio-info-published-locks` | Sự kiện seed đang mở bán | SEI-AC-11, SEI-AC-12 |
| SEI-08 | `studio-info-unsaved` | Sửa tên, bấm bước Loại vé | SEI-AC-19 |
| SEI-09 | `studio-info-timezone-tokyo` | Tạo nháp, đổi múi giờ `Asia/Tokyo`, đặt giờ 20:00, lưu, mở xem trước | Giờ hiện `20:00 GMT+9` (FR-17.6); API trả `startsAt` = `…T11:00:00Z` |
| SEI-10 | `studio-info-narrow` | 390 × 844, như SEI-01 | SEI-AC-26 |

## Quyết định phát sinh khi viết tài liệu này

Claude chốt theo ủy quyền của Owner (mục nhỏ, dễ đảo ngược); số DR thật do sổ quyết định gán.

**DR-225 · Chế độ tạo dùng cùng biểu mẫu; lần lưu đầu gọi E-25 rồi E-27.** *Vấn đề:* DR-70 nói "Tạo sự kiện" gọi `POST /organizer/events { name }` rồi vào bước Thông tin; DOC-38 §3 và DOC-41 §3 dòng 4 lại cho `/studio/events/new` là một route của `StudioEventInfo` ("cùng biểu mẫu, hai chế độ"), còn DOC-54 điều hướng tới `/studio/events/new` mà không gọi API. Không nói khi nào E-25 được gọi và người dùng nhập gì trước đó. *Quyết định:* `/studio/events/new` hiện đủ biểu mẫu với giá trị mặc định; không có request cho tới khi bấm lưu. Lần lưu đầu: E-25 `{ name }`, rồi E-27 `rowVersion 0` với các trường khác mặc định (nếu có), rồi `replace` sang `…/{eventId}/info` (hoặc bước sau). E-27 lỗi 422 thì sự kiện nháp đã tồn tại, màn vẫn `replace` sang `…/info` và hiện lỗi. E-25 không tự thử lại khi lỗi mạng (không idempotent); bấm lại sau lỗi mạng có thể tạo hai bản nháp, chấp nhận vì hiếm và người tổ chức thấy ngay ở Studio 01. *Hệ quả:* không thêm endpoint; DR-70 giữ nguyên ý (tên là trường duy nhất E-25 cần). *Ghi vào:* DOC-55, DOC-84 (FL-06).

**DR-226 · Hộp thoại xác nhận trước khi lưu đổi giờ hoặc địa điểm của sự kiện đã xuất bản.** *Vấn đề:* lưu đổi `startsAt`, `endsAt` hoặc `venue` sau xuất bản gửi email cho mọi đơn `PAID` ngay trong transaction (DR-29), không thu hồi được; UXP-11 yêu cầu thao tác không hoàn tác dùng hộp thoại xác nhận, nhưng canvas và DR-29 chỉ có thông báo sau khi lưu. *Quyết định:* khi `status` ∈ {`PUBLISHED`, `PAUSED`} và form đổi một trong ba trường đó, "Lưu thay đổi" mở `ConfirmDialog` (`studio.info.reschedule.confirm.*`, nút trái "Chưa lưu" nhận tiêu điểm, nút phải `primary` "Lưu và gửi thông báo"). Hộp thoại không nêu số người mua (E-26 không có số đơn `PAID`; thêm một truy vấn chỉ để hiện số là không cần). Sự kiện chưa có đơn vẫn thấy hộp thoại. *Hệ quả:* thêm 5 key DOC-40; DOC-38 §9 thêm một dòng "khác canvas". *Ghi vào:* DOC-55, DOC-38, DOC-40, DOC-84 (FL-11).

**DR-227 · Client kiểm loại và kích thước ảnh trước khi tải lên.** *Vấn đề:* ảnh sự kiện tối đa `media.max-event-image-bytes` (2 MB) chỉ kiểm ở server; nginx cho body tới 6 MB trên route này (DOC-62), nên ảnh điện thoại 4 MB vẫn bị tải lên hết rồi mới nhận 413, và ảnh > 6 MB nhận 413 của nginx không có body Problem Details. Lỗi 413 ở DOC-40 nói "ảnh tối đa theo giới hạn ở bước Thông tin" nên màn phải hiện con số. *Quyết định:* frontend có hằng `EVENT_IMAGE_MAX_BYTES = 2_097_152` và danh sách loại `image/jpeg`, `image/png`, `image/webp` trong `frontend/src/features/studio/media.ts`, chú thích trỏ tới `media.max-event-image-bytes`; kiểm trước khi gửi và hiện ở `studio.info.image.limits` (`{maxMb}` = 2). Mọi 413 (có hoặc không có body JSON) hiển thị cùng `errors.payload_too_large`. Server vẫn là nguồn cuối. *Hệ quả:* đổi `media.max-event-image-bytes` phải đổi hằng này trong cùng PR; DOC-34 dòng `media.max-event-image-bytes` ghi chú điều đó. *Ghi vào:* DOC-55, DOC-34, DOC-40.

**DR-228 · Danh sách múi giờ của combobox và tên cũ trong `Intl`.** *Vấn đề:* DR-12 lấy danh sách từ `Intl.supportedValuesOf('timeZone')`, nhưng một số trình duyệt trả tên chuẩn cũ của CLDR (`Asia/Saigon`, `Asia/Calcutta`, `Europe/Kiev`) và không có `Asia/Ho_Chi_Minh`, là giá trị mặc định do server gán (E-25); combobox sẽ không hiện được giá trị đang lưu. *Quyết định:* danh sách = `Intl.supportedValuesOf('timeZone')` ∪ {giá trị hiện tại của sự kiện, `VITE_PLATFORM_TIMEZONE`}; hai tên cùng trỏ một vùng (so bằng `new Intl.DateTimeFormat('en', { timeZone }).resolvedOptions().timeZone`) gộp một mục, giữ tên hiện tại của sự kiện hoặc tên mặc định nền tảng nếu có; nhãn `<tên IANA> (<offset hiện tại>)` dùng `timeZoneName: 'shortOffset'`; tìm không phân biệt hoa thường, coi `_` và `/` như dấu cách. Giá trị gửi lên là tên người dùng chọn; server chấp nhận cả tên cũ (`ZoneId.getAvailableZoneIds()` có cả hai). *Hệ quả:* không đổi API. *Ghi vào:* DOC-55, DOC-31 §6 (ghi chú cạnh quy tắc múi giờ).

**DR-229 · Frontend biết `PLATFORM_TIMEZONE` qua `VITE_PLATFORM_TIMEZONE`.** *Vấn đề:* DR-12 thêm hậu tố offset khi `event.timezone` khác `PLATFORM_TIMEZONE`, và form tạo mới cần múi giờ mặc định trước khi có E-25; nhưng không response nào trả `PLATFORM_TIMEZONE` và DOC-34 §3.12 không có biến build tương ứng. *Quyết định:* thêm biến build `VITE_PLATFORM_TIMEZONE` = `PLATFORM_TIMEZONE` (cùng cách `VITE_PAYMENTS`, DR-125), mặc định `Asia/Ho_Chi_Minh`; mọi màn (DOC-43, DOC-48…50, DOC-55, DOC-58, DOC-59) dùng giá trị này cho quy tắc hậu tố. *Hệ quả:* DOC-34 §3.12 thêm một dòng; Dockerfile `nginx` nhận build arg; đổi `PLATFORM_TIMEZONE` phải build lại frontend. *Ghi vào:* DOC-34, DOC-31, DOC-40 §2.2, DOC-55.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-225…229 (mục trên).
