# Màn hình Studio: Loại vé và giá

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-56
> Phụ thuộc: SDD gốc §6.2–6.3, §13.1, [Sổ quyết định](../../00-decision-register.md) (DR-10, 13, 23, 24, 26, 30, 41, 46, 64, 68, 70, 132, 144, 164), [DOC-02](../../01-product/personas-and-journeys.md) (J-3 bước 3, J-4 bước 2), [DOC-03](../../01-product/requirements.md) (FR-02.5, FR-02.6, FR-02.12, FR-02.13, FR-02.14, FR-02.16), [DOC-04](../../01-product/use-cases.md) (UC-07), [DOC-06](../../02-glossary.md), [DOC-20](../../06-design/events-and-ticket-types.md) §5, [DOC-37](../../07-api/api-endpoints.md) (E-26, E-34, E-35, E-36), [DOC-38](../ux-principles-and-ia.md) §2.2, §3, §6.3, §8.3, §8.4, [DOC-39](../design-system.md) §2.3, §7.1, §7.2, §7.5, §7.9, §7.13, §7.15, §7.17, [DOC-40](../ui-states-and-copy.md) §3.1, §3.4, §3.8, §4.1, [DOC-41](README.md) §1.2, §2, [DOC-82](../../06-design/flows/README.md) (FL-07); canvas: artboard "Studio 03"
> Người dùng chính: P2-04.2 (Studio 03 phần GA), P5-06.2 (phần `SEAT`/`ZONE`), tác giả DOC-84 (FL-07), người viết test E2E soạn sự kiện

Tài liệu mô tả bước **Loại vé** của Studio ở `/studio/events/:eventId/ticket-types`: danh sách tối đa 5 loại vé, mỗi loại có tên, mô hình (Ghế, Khu vực, Tự do), giá và sức chứa GA. Quy tắc nghiệp vụ và transaction ở DOC-20 §5 (DR-26, DR-30, DR-144); schema request/response ở DOC-37 E-34…36; lời gọi qua các tầng ở FL-07 (DOC-84). Màn này chỉ dẫn **key** chuỗi (DOC-40 §3.4). Phần `SEAT`/`ZONE` dựng được từ P2 (lưu loại vé), nhưng sức chứa của chúng lấy từ sơ đồ (P4, DOC-57) và sự kiện có loại vé này chỉ xuất bản được từ P5.

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-4 (sự kiện chỉ GA, P2), PS-3 (có sơ đồ, P5); máy tính ≥ 1024 px, vẫn dùng được một cột dưới 1024 px (DOC-38 §6.3) |
| Use case | UC-07 bước 4 (thêm, sửa, xóa loại vé); UC-09 gián tiếp (điều kiện `has_ticket_type`, `ticket_type_capacity` của E-28) |
| Yêu cầu | FR-02.5, FR-02.6, FR-02.12, FR-02.13, FR-02.14, FR-02.16 |
| Hành trình | J-3 bước 3, J-4 bước 2 |
| Luồng chi tiết | FL-07 |
| Quyền | Vai trò `ORGANIZER` và là chủ sự kiện. Chưa đăng nhập → E2 (DOC-38 §8.5); chưa có hồ sơ (403 `ORGANIZER_PROFILE_REQUIRED`) → `/studio/profile`; không có vai trò → E3; sự kiện của tổ chức khác hoặc không tồn tại → E1 (404, DR-23) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/studio/events/:eventId/ticket-types` | `eventId` | UUID (DR-11); sai định dạng → E1 không gọi API; E-26 trả 404 → E1 |

Không có search param. Bản nháp chưa lưu chỉ nằm trong bộ nhớ của form (không ghi `localStorage`); rời trang khi còn thay đổi chưa lưu thì hỏi (DOC-38 §8.4).

## 3. Wireframe

Desktop (≥ 1024 px), canvas "Studio 03", biến thể sự kiện `DRAFT` có 3 loại vé GA. Cột trái là danh sách thẻ loại vé, cột phải là thẻ "Bán theo cách nào" (dính khi cuộn):

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ STUDIO   Sự kiện của bạn / Hội thảo Kiến trúc Hệ thống        [vi|en] [GM ▾] │
│ (✓)Thông tin ─ (2)Loại vé ─ (3)Xem trước ─ (4)Xuất bản            [Bản nháp] │ StudioStepper
├──────────────────────────────────────────────────────────────────────────────┤
│ LOẠI VÉ VÀ GIÁ                                       ┌─────────────────────┐ │
│ 3 loại vé · sức chứa 450                             │ BÁN THEO CÁCH NÀO   │ │
│ ┌──────────────────────────────────────────────────┐ │ Ghế: người mua chọn │ │
│ │ ■1 Tên loại vé                         [🗑 Xóa]  │ │ từng ghế…           │ │
│ │ [Đặt sớm                              ]          │ │ Khu vực: …          │ │
│ │ Bán theo  (Ghế) (Khu vực) (●Tự do)               │ │ Tự do: …            │ │
│ │ Giá (₫) [300.000      ]  Sức chứa [100        ]  │ │ Một sự kiện có thể  │ │
│ ├──────────────────────────────────────────────────┤ │ trộn cả ba cách…    │ │
│ │ ■2 Phổ thông · Tự do · 450.000 ₫ · 300 vé  …     │ └─────────────────────┘ │
│ ├──────────────────────────────────────────────────┤                         │
│ │ ■3 Sinh viên · Tự do · 0 ₫ · 50 vé               │                         │
│ │    Miễn phí, không cần thẻ                       │                         │
│ └──────────────────────────────────────────────────┘                         │
│ [+ Thêm loại vé]   Tối đa 5 loại vé mỗi sự kiện.                             │
│ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │
│ Chỉ có vé tự do nên không cần sơ đồ.                                         │
│ [Quay lại Thông tin]          [Lưu bản nháp]  [LƯU VÀ SANG XEM TRƯỚC      ]  │ Button primary
└──────────────────────────────────────────────────────────────────────────────┘
```

Mọi thẻ luôn mở (không thu gọn) để lỗi theo ô luôn nhìn thấy; wireframe rút gọn dòng 2–3 cho gọn. Ô "Sức chứa" của loại `SEAT`/`ZONE` thay bằng chữ `studio.types.capacity.fromMap` và một dòng hướng dẫn (`…fromMap.seat` / `…fromMap.zone`).

Biến thể sự kiện đã xuất bản (`PUBLISHED`), canvas "Studio 03" khung "Đang bán":

```text
│ ⓘ Giá mới chỉ áp dụng cho đơn giữ vé sau lúc đổi. Loại vé đã có vé giữ   │ Notice info
│   hoặc bán thì không xóa được.                                            │
│ ┌──────────────────────────────────────────────────┐                      │
│ │ ■1 Phổ thông                                     │                      │
│ │ Bán theo  Tự do  (khóa: Không đổi được sau khi xuất bản.)               │
│ │ Giá (₫) [450.000 ]  Áp dụng cho đơn mới                                 │
│ │ Sức chứa [300    ]  Tối thiểu 48: đang giữ và đã bán                    │
│ │ [🗑 Xóa – vô hiệu] Không xóa được: đã có 48 vé đang giữ hoặc đang bán   │
│ └──────────────────────────────────────────────────┘                      │
│ Sự kiện đã xuất bản nên không thêm loại vé được. …   (thay nút Thêm)      │
```

Dưới 1024 px (390 px): một cột; thẻ "Bán theo cách nào" chuyển xuống dưới danh sách và thu thành `<details>` đóng sẵn; ba nút cuối xếp dọc, nút chính ở trên cùng:

```text
┌────────────────────────┐
│ STUDIO  [vi|en] [GM ▾] │
│ ✓ · 2 Loại vé · 3 · 4  │
│ LOẠI VÉ VÀ GIÁ         │
│ 3 loại vé · sức chứa…  │
│ ┌────────────────────┐ │
│ │ ■1 Tên loại vé  🗑 │ │
│ │ [Đặt sớm         ] │ │
│ │ (Ghế)(Khu vực)(●Tự do)│
│ │ Giá (₫) [300.000 ] │ │
│ │ Sức chứa [100    ] │ │
│ └────────────────────┘ │
│ [+ Thêm loại vé]       │
│ ▸ Bán theo cách nào    │
│ [LƯU VÀ SANG XEM TRƯỚC]│
│ [Lưu bản nháp]         │
│ Quay lại Thông tin     │
└────────────────────────┘
```

## 4. Vùng và component

Trang là `StudioTicketTypes` (`frontend/src/features/studio/StudioTicketTypes.tsx`, DOC-41 §1.2), trong khung Studio chung (header, breadcrumb, `StudioStepper`). Form dùng react-hook-form + zod (chunk `studio`, DOC-38 §3.1) với một mảng `ticketTypes[]`.

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Header, breadcrumb | khung Studio, `AccountMenu` (§7.16) | `studio.brand`, `studio.breadcrumb.events` + tên sự kiện (không dịch) |
| Thanh bước | `StudioStepper` (§7.13) | Bước Sơ đồ chỉ hiện khi **đã lưu** ít nhất một loại `SEAT`/`ZONE` (theo E-26, không theo form chưa lưu); nhãn trạng thái lưu bên phải: `studio.save.draft` / `studio.save.saved` / `studio.save.unsaved` (DOC-38 §8.4) |
| Tiêu đề, tóm tắt | `h1` `studio.types.title`; dòng `studio.types.summary` | `{count}` = số thẻ trên form; `{capacity}` = Σ `gaCapacity` của thẻ GA (§5). Loại `SEAT`/`ZONE` cộng 0 ở P2–P4; P5 cộng số ghế/sức chứa khu vực theo DOC-57 |
| Thông báo đang bán | `Notice` `info` (§7.9) `studio.types.live.body` | Chỉ khi `status` ∈ {`PUBLISHED`, `PAUSED`} |
| Thông báo chỉ đọc | `Notice` `info` `studio.types.readonly` (key mới) | Chỉ khi `status` ∈ {`ENDED`, `CANCELLED`}; mọi ô và nút ghi vô hiệu |
| Tóm tắt lỗi | `Notice` `error` `studio.types.errors` | Sau lần lưu có lỗi theo ô; `{count}` = số ô lỗi; bấm vào focus ô lỗi đầu tiên |
| Thẻ loại vé | thẻ viền `--line`, đầu thẻ có ô màu 12 px `--type-{colorIndex}` + số thứ tự mono | Thẻ chưa lưu (mới thêm) chưa có `colorIndex`: ô màu viền nét đứt `--line-strong`; màu thật do server gán (DR-26, DOC-39 §2.3) |
| Ô tên | `TextField` (§7.2) `studio.types.name.label` | 1–60 ký tự sau trim; lỗi `studio.types.name.error`, `studio.types.name.duplicate`, `validation.too_long` (`{max}` = 60) |
| Ô mô hình | nhóm 3 nút `role="radiogroup"` `studio.types.model.label`: `…model.seat`, `…model.zone`, `…model.ga` | Sau xuất bản: vô hiệu, chú thích `validation.locked_after_publish` (FR-02.6) |
| Ô giá | `TextField` `inputmode="numeric"` `studio.types.price.label` | Nhập chữ số, hiển thị phân cách nghìn theo locale khi rời ô (DOC-40 §2.1). Giá 0 → chú thích `studio.types.price.free`. Sau xuất bản → chú thích `studio.types.price.live` |
| Ô sức chứa | `TextField` `inputmode="numeric"` `studio.types.capacity.label` (GA) | Chỉ với `GA`; chú thích `studio.types.capacity.max`. Sau xuất bản và `used > 0`: chú thích `studio.types.capacity.min` (`{count}` = `used`). `SEAT`/`ZONE`: thay ô bằng chữ `…capacity.fromMap` + `…fromMap.seat`/`…fromMap.zone` |
| Nút xóa | `Button` `icon` (§7.1) `aria-label` `studio.types.delete.aria` (`{name}` = tên, rỗng → `…delete.unnamed`), tooltip `studio.types.delete` | `used > 0` → vô hiệu (`disabled`) và chú thích `studio.types.delete.blocked` (`{count}` = `used`) |
| Hộp thoại xóa | `ConfirmDialog` (§7.15) kiểu `cancel-event` (nút phải `urgent`) | Chỉ với loại vé **đã lưu**. Key mới `studio.types.delete.confirm.*` (§8). Tiêu điểm vào nút trái (DOC-38 §8.3) |
| Thêm | `Button` `secondary` `studio.types.add` + chú thích `studio.types.limit` | Vô hiệu khi đủ 5 thẻ. `status ≠ DRAFT`: ẩn nút, thay bằng chữ `studio.types.add.locked` (key mới, DR-144) |
| Trạng thái rỗng | `EmptyState` (§7.17) `studio.types.empty.title`, `…empty.body`, nút `studio.types.add` | Không có thẻ nào |
| Thẻ hướng dẫn | thẻ phụ `studio.types.model.help.title` + `…help.seat`, `…help.zone`, `…help.ga`, `…help.mix` | Tĩnh |
| Bước sau | đoạn chữ trên hàng nút: `studio.types.next.map` (có thẻ `SEAT`/`ZONE`), `…next.noMap` (chỉ GA), `…next.empty` (không thẻ) | Tính theo form hiện tại |
| Hàng nút | `Button text` `studio.save.back` (`{step}` = `studio.step.info`); `Button secondary` `studio.save.draftBtn`; `Button primary` `studio.save.next` (`{step}` = `studio.step.map` khi có thẻ `SEAT`/`ZONE`, ngược lại `studio.step.preview`) | Đúng một nút chính (UXP-01). Nút chính vô hiệu khi không có thẻ. Sau xuất bản: ẩn "Lưu bản nháp", nút chính đổi nhãn thành `studio.save.nextStep` |
| Hộp thoại rời trang | `ConfirmDialog` `common.unsaved.*` | DOC-38 §8.4 |

Khác canvas: không có ô "Mô tả loại vé" (DB không có cột, DOC-14); nút "Thêm loại vé" bị thay bằng chú thích sau xuất bản (DR-144, canvas không vẽ); không có hộp thoại `STALE_EVENT_VERSION` ở bước này vì ghi loại vé không dùng `rowVersion` (DR-195, §6).

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Sự kiện và loại vé | `GET /organizer/events/{eventId}` (E-26) | Vào màn; sau mỗi lần lưu xong; `refetchOnWindowFocus` **chỉ khi form không có thay đổi chưa lưu** | Dùng `name`, `status`, `ticketTypes[]` (`ticketTypeId`, `name`, `model`, `price`, `colorIndex`, `gaCapacity`, `sortOrder`, `used`), `steps`. Khóa truy vấn `['organizerEvent', eventId]` dùng chung với DOC-55, DOC-58, DOC-59 |
| Thêm | `POST /organizer/events/{eventId}/ticket-types` (E-34) | Lưu, mỗi thẻ mới một request | `{ "name", "model", "price", "gaCapacity" }`; `gaCapacity` chỉ gửi với `GA` |
| Sửa | `PATCH /organizer/events/{eventId}/ticket-types/{ticketTypeId}` (E-35) | Lưu, mỗi thẻ đã đổi một request | Chỉ gửi trường đã đổi so với E-26 |
| Xóa | `DELETE /organizer/events/{eventId}/ticket-types/{ticketTypeId}` (E-36) | Xác nhận trong hộp thoại xóa | Gọi ngay, không chờ "Lưu" (DR-195) |

Ví dụ: sự kiện `DRAFT` có "Phổ thông" (đã lưu, `GA`, 450.000, 300); người tổ chức đổi giá thành 500.000 và thêm "Sinh viên" giá 0 sức chứa 50, rồi bấm "Lưu bản nháp". Client gửi tuần tự:

```http
PATCH /api/v1/organizer/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34/ticket-types/0199b1c2-7b01-7f10-8a22-3c4d5e6f7081
X-CSRF-Token: …

{"price":500000}
```

```http
POST /api/v1/organizer/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c34/ticket-types
X-CSRF-Token: …

{"name":"Sinh viên","model":"GA","price":0,"gaCapacity":50}
```

Hai response lần lượt là 200 và 201 `OrganizerTicketType` (`colorIndex = 2`, `used = 0`); sau đó gọi lại E-26 một lần.

Đổi mô hình khi `DRAFT`: `GA → SEAT` gửi `{"model":"SEAT"}` (server tự đặt `ga_capacity = NULL` theo `ticket_type_ga_ck`, DOC-20 §5.3); `SEAT → GA` gửi `{"model":"GA","gaCapacity":<số>}` và ô sức chứa trở thành bắt buộc trên form.

Kiểm tra ở client (zod, chỉ để báo sớm; server là nguồn cuối): tên 1–60 ký tự sau trim, không trùng tên thẻ khác trên form (so sánh `toLocaleLowerCase('vi')` sau trim); giá là số nguyên ≥ 0; sức chứa GA là số nguyên 1–100.000. Client **không** viết cứng mức giá tối thiểu 20.000 (`payment.min-amount`, DR-13): khoảng giá đến từ `params` của lỗi `out_of_range` (DR-196).

Không có kênh real-time; không hỏi theo nhịp.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-26 | Form nạp `ticketTypes[]` theo `sortOrder`; không có loại vé → trạng thái rỗng | 404 → E1; 403 `ORGANIZER_PROFILE_REQUIRED` → `/studio/profile`; mạng/5xx → `LoadError`; 503/429 → tự thử lại (DR-132) |
| Bấm "Thêm loại vé" | — | Thẻ mới cuối danh sách: tên rỗng, mô hình `GA`, giá rỗng, sức chứa rỗng; focus vào ô tên; trạng thái lưu → `studio.save.unsaved` | Vô hiệu khi đủ 5 thẻ (§7) |
| Sửa ô bất kỳ | — | Trạng thái lưu → `studio.save.unsaved`; lỗi theo ô của ô đó bị xóa | — |
| Bấm 🗑 ở thẻ **chưa lưu** | — | Bỏ thẻ khỏi form, không hỏi | — |
| Bấm 🗑 ở thẻ đã lưu | — | Mở hộp thoại xóa (`…delete.confirm.body.draft` khi `DRAFT`, `…body.live` khi đã xuất bản) | Nút vô hiệu khi `used > 0` |
| Xác nhận xóa | E-36 | 204 → bỏ thẻ khỏi form; **thay đổi chưa lưu ở thẻ khác giữ nguyên**; gọi lại E-26 rồi ghép (thẻ đang sửa giữ giá trị form, thẻ không sửa nhận giá trị server) | 409 `TICKET_TYPE_IN_USE` (`used`) → đóng hộp thoại, `Notice` `error` `studio.types.delete.blocked` trên thẻ, cập nhật `used`; 404 → bỏ thẻ (đã bị xóa ở nơi khác); 409 `EVENT_STATE_CONFLICT` → `studio.publish.notice.stateConflict` + gọi lại E-26 |
| Bấm "Lưu bản nháp" | E-35, E-34 | Chạy **lưu** (dưới); thành công hết → ở lại màn, trạng thái `studio.save.saved` | Bảng lỗi dưới |
| Bấm "Lưu và sang {step}" | E-35, E-34 | Chạy **lưu**; thành công hết → điều hướng `…/map` (có thẻ `SEAT`/`ZONE` sau khi lưu) hoặc `…/preview`. Form không đổi gì thì điều hướng ngay | Có lỗi → ở lại, hiện lỗi |
| Bấm "Quay lại Thông tin" / bước khác trên `StudioStepper` / liên kết header | — | Không có thay đổi chưa lưu → điều hướng. Có → hộp thoại `common.unsaved.*` (DOC-38 §8.4); "Rời đi" bỏ thay đổi | — |
| Đóng tab hoặc tải lại khi có thay đổi chưa lưu | — | `beforeunload` của trình duyệt | — |

**Thủ tục lưu** (DR-195):

1. Kiểm tra zod toàn form; có lỗi → hiện lỗi theo ô, tóm tắt `studio.types.errors`, focus ô lỗi đầu tiên; dừng.
2. Lập danh sách request: `PATCH` cho mỗi thẻ đã lưu có trường khác E-26 (theo thứ tự thẻ), rồi `POST` cho mỗi thẻ mới (theo thứ tự thẻ). `PATCH` chạy trước để tên được đổi đi giải phóng tên cho thẻ mới.
3. Gửi **tuần tự**, chờ từng response. Một request lỗi không dừng các request sau (mỗi loại vé là tài nguyên độc lập); lỗi gắn vào thẻ của request đó.
4. Gọi lại E-26 một lần. Thẻ thành công nhận giá trị server (kèm `colorIndex`, `used`); thẻ lỗi giữ giá trị form và lỗi theo ô.
5. Còn thẻ lỗi → trạng thái `studio.save.unsaved` + tóm tắt lỗi, không điều hướng; hết lỗi → `studio.save.saved`.

Nút lưu ở trạng thái `loading` suốt thủ tục; mọi ô vô hiệu trong lúc gửi.

Lỗi của E-34, E-35 (DOC-40 §4.1, §3.8):

| HTTP · `code` | Hiển thị | Hành động |
| --- | --- | --- |
| 422 `VALIDATION_FAILED` `name` `required` / `too_long` / `duplicate` | Lỗi dưới ô tên: `studio.types.name.error` / `validation.too_long` (`{max}` 60) / `studio.types.name.duplicate` | Thẻ giữ giá trị |
| 422 `VALIDATION_FAILED` `price` `out_of_range` (`params.min`, `params.max`) | Giá > 0 và < `min` → `studio.types.price.min` (`{min}` định dạng tiền); ngược lại `validation.out_of_range` | — |
| 422 `VALIDATION_FAILED` `gaCapacity` `required` / `out_of_range` / `not_allowed` | `studio.types.capacity.error` / `validation.out_of_range` / `errors.generic` (lỗi lập trình: gửi sức chứa với `SEAT`/`ZONE`) | — |
| 422 `VALIDATION_FAILED` `model` `locked_after_publish` | Lỗi dưới ô mô hình `validation.locked_after_publish`; trả ô về giá trị E-26 | Gọi lại E-26 (sự kiện vừa xuất bản ở tab khác) |
| 422 `TICKET_TYPE_LIMIT_REACHED` | `Notice` `error` `studio.types.limit` trên thẻ mới | Gọi lại E-26; thẻ mới giữ trên form để người tổ chức xóa bớt |
| 409 `CAPACITY_BELOW_USED` (`used`) | Lỗi dưới ô sức chứa `studio.types.capacity.floor` (`{count}` = `used`) | Cập nhật `used` của thẻ (DR-30) |
| 409 `EVENT_STATE_CONFLICT` | `Notice` `error` `studio.publish.notice.stateConflict` đầu trang | Gọi lại E-26. POST khi không còn `DRAFT` (DR-144) rơi vào đây: thẻ mới bị bỏ sau khi người tổ chức đọc thông báo và màn chuyển sang biến thể đã xuất bản |
| 404 `NOT_FOUND` (PATCH) | — | Loại vé đã bị xóa ở nơi khác: bỏ thẻ khỏi form, `Notice` `info` `studio.types.removedElsewhere` (key mới) |
| 503 `OVERLOADED`, 429 `RATE_LIMITED`, lỗi mạng | `Notice` `warning` `errors.rate_limited.*` / `common.retry.auto` | Tự thử lại **request đó** theo DR-132, rồi tiếp thủ tục |
| Lỗi mạng ở `POST` rồi thử lại nhận 422 `duplicate` | Không hiện lỗi nếu E-26 có loại vé trùng tên (không phân biệt hoa thường) và cùng `model`, `price`, `gaCapacity` với thẻ | Coi như POST đầu đã commit: nhận loại vé đó cho thẻ. Khác giá trị → lỗi `duplicate` như thường |
| 401 `UNAUTHENTICATED` | — | E2 với `returnTo` (DOC-38 §8.5); thay đổi chưa lưu mất (đã nói trong hộp thoại hết phiên) |

**Hai tab cùng sửa** (DR-195): không có `rowVersion` cho loại vé. Hai tab sửa cùng loại vé thì lần ghi sau thắng theo từng trường gửi đi; ràng buộc 5 loại và tên duy nhất do server giữ dưới khóa `FOR UPDATE` dòng sự kiện (DOC-20 §5.2). Màn không tự gọi lại E-26 khi đang có thay đổi chưa lưu, nên tab B không mất dữ liệu đang gõ; sau khi lưu, E-26 cho thấy giá trị cuối.

Luồng: FL-07 (DOC-84).

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Chưa có loại vé ("Studio 03" rỗng) | E-26 `ticketTypes = []`, form không thẻ | `EmptyState` `studio.types.empty.*` + nút thêm; `…next.empty`; nút chính vô hiệu; nhãn lưu `studio.save.draft` |
| 2 | Đang soạn (`DRAFT`, "Studio 03") | `DRAFT`, ≥ 1 thẻ | §3 wireframe 1; mọi ô sửa được |
| 3 | Có lỗi theo ô ("Studio 03" lỗi) | Lỗi zod hoặc 422/409 sau lưu | Ô lỗi viền `--stamp` (DOC-39 §7.2), tóm tắt `studio.types.errors` đầu danh sách |
| 4 | Đủ 5 loại vé | 5 thẻ trên form | Nút thêm vô hiệu, chú thích `studio.types.limit` vẫn hiện |
| 5 | Có mô hình Ghế/Khu vực | ≥ 1 thẻ `SEAT`/`ZONE` | Ô sức chứa thay bằng `…capacity.fromMap` + hướng dẫn; `…next.map`; nút chính "Lưu và sang Sơ đồ". Ở P2–P3 bước Sơ đồ vẫn mở được nhưng là màn của P4 (DOC-57); sự kiện chưa xuất bản được (E-28 `seat_map_published = false`, DOC-20 §6.1) |
| 6 | Giá 0 | Ô giá = 0 | Chú thích `studio.types.price.free` dưới ô giá |
| 7 | Đang lưu | Thủ tục lưu đang chạy | Nút bấm ở `loading` (`aria-busy`), ô vô hiệu |
| 8 | Đã lưu | Lưu xong không lỗi | Nhãn `studio.save.saved`; ở lại màn (nút "Lưu bản nháp") |
| 9 | Đang bán ("Studio 03" đang bán) | `PUBLISHED` hoặc `PAUSED` | Wireframe 2: `Notice` `studio.types.live.body`; mô hình khóa; `…price.live`; `…capacity.min` khi `used > 0`; nút thêm thay bằng `studio.types.add.locked`; không có "Lưu bản nháp" |
| 10 | Không xóa được | `used > 0` | Nút 🗑 vô hiệu + `studio.types.delete.blocked` |
| 11 | Hộp thoại xóa | Bấm 🗑 thẻ đã lưu | `ConfirmDialog` (§4) |
| 12 | Đã kết thúc hoặc đã hủy | `ENDED` hoặc `CANCELLED` | `Notice` `studio.types.readonly`; mọi ô, nút thêm, nút xóa, nút lưu vô hiệu; nút chính đổi thành điều hướng `studio.save.nextStep` không lưu |
| 13 | Quá tải | 503/429/mạng khi lưu hoặc xóa | `Notice` `warning` tự thử lại; nút giữ `loading` |
| 14 | Có thay đổi chưa lưu | Form khác E-26 | Nhãn `studio.save.unsaved`; rời trang hỏi |
| 15 | Đang tải | Chờ E-26 > 150 ms | `Skeleton`: tiêu đề, 2 thẻ cao như thẻ thật, thẻ hướng dẫn; dòng ẩn `studio.types.loading` (key mới) |
| 16 | Lỗi tải | E-26 lỗi (không phải 404, 401, 403) | `LoadError`: `common.loadError.label`, `studio.types.error.title`, `studio.types.error.body` (key mới), `common.retry` |
| 17 | Không quyền, không tìm thấy | 404, 403 | §1 (E1, E3, `/studio/profile`) |

Tiêu điểm: vào màn → `h1`; thêm thẻ → ô tên của thẻ mới; lưu có lỗi → ô lỗi đầu tiên; đóng hộp thoại xóa → về nút 🗑 (hoặc nút "Thêm loại vé" khi thẻ đã bị xóa).

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.4, §3.1, §3.8, §4.1. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Khung, bước, lưu | `studio.brand`, `studio.breadcrumb.events`, `studio.step.info`, `studio.step.ticketTypes`, `studio.step.map`, `studio.step.preview`, `studio.step.publish`, `studio.save.draft`, `studio.save.saved`, `studio.save.unsaved`, `studio.save.draftBtn`, `studio.save.next`, `studio.save.nextStep`, `studio.save.back`, `common.unsaved.title`, `.body`, `.stay`, `.leave` |
| Danh sách | `studio.types.title`, `studio.types.summary`, `studio.types.live.body`, `studio.types.errors`, `studio.types.empty.title`, `studio.types.empty.body`, `studio.types.add`, `studio.types.limit` |
| Ô | `studio.types.name.label`, `.name.error`, `.name.duplicate`, `.model.label`, `.model.seat`, `.model.zone`, `.model.ga`, `.price.label`, `.price.error`, `.price.free`, `.price.live`, `.price.min`, `.capacity.label`, `.capacity.error`, `.capacity.floor`, `.capacity.min`, `.capacity.fromMap`, `.capacity.fromMap.seat`, `.capacity.fromMap.zone`, `.capacity.none`, `.capacity.max` |
| Xóa | `studio.types.delete`, `.delete.aria`, `.delete.unnamed`, `.delete.blocked` |
| Hướng dẫn, bước sau | `studio.types.model.help.title`, `.help.seat`, `.help.zone`, `.help.ga`, `.help.mix`, `studio.types.next.map`, `.next.noMap`, `.next.empty` |
| Lỗi | `validation.too_long`, `validation.out_of_range`, `validation.locked_after_publish`, `studio.publish.notice.stateConflict`, `errors.rate_limited.*`, `errors.generic.*`, `common.retry`, `common.retry.auto`, `common.loadError.label`, `common.loading.slow` |
| **Key mới** (thêm vào DOC-40 §3.4) | `studio.types.loading` "Đang tải loại vé…" / "Loading ticket types…"; `studio.types.error.title` "Không tải được loại vé" / "Couldn't load ticket types"; `studio.types.error.body` "Việc bán vé không bị ảnh hưởng. Kiểm tra kết nối mạng rồi thử lại." / "Ticket sales aren't affected. Check your connection and try again."; `studio.types.readonly` "Sự kiện đã kết thúc hoặc đã hủy nên loại vé chỉ còn để xem." / "This event has ended or was cancelled, so its ticket types are read-only."; `studio.types.add.locked` "Sự kiện đã xuất bản nên không thêm loại vé được. Muốn bán thêm hạng vé, hãy tạo sự kiện mới." / "This event is published, so you can't add ticket types. To sell another tier, create a new event."; `studio.types.removedElsewhere` "Một loại vé vừa bị xóa ở nơi khác nên đã được bỏ khỏi danh sách." / "A ticket type was deleted elsewhere, so it was removed from this list."; `studio.types.delete.confirm.title` "Xóa loại vé {name}?" / "Delete ticket type {name}?"; `studio.types.delete.confirm.body.draft` "Loại vé bị xóa hẳn khỏi bản nháp. Màu của nó sẽ dùng lại cho loại vé tạo sau." / "The ticket type is removed from the draft for good. Its colour will be reused by the next ticket type you add."; `studio.types.delete.confirm.body.live` "Người mua sẽ không thấy loại vé này nữa và số vé còn trống của nó bị bỏ khỏi kho vé. Không hoàn tác được." / "Buyers will no longer see this ticket type, and its remaining tickets are removed from the inventory. This can't be undone."; `studio.types.delete.confirm.keep` "Giữ loại vé" / "Keep ticket type"; `studio.types.delete.confirm.cta` "Xóa loại vé" / "Delete ticket type" |

Tên loại vé do người tổ chức nhập không dịch (DR-10).

## 9. Tiêu chí nghiệm thu

Tiền tố `STT-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| STT-AC-01 | Sự kiện `DRAFT` chưa có loại vé | Mở màn | `EmptyState` `studio.types.empty.*`; nút chính vô hiệu; dòng `studio.types.next.empty` |
| STT-AC-02 | `DRAFT`, 4 thẻ | Bấm "Thêm loại vé" | Thẻ thứ 5 có mô hình `GA`, focus ở ô tên; nút thêm vô hiệu, `studio.types.limit` hiện |
| STT-AC-03 | Thẻ mới tên "vip", thẻ đã lưu "VIP" | Bấm "Lưu bản nháp" | Không gửi request; lỗi `studio.types.name.duplicate` ở ô tên thẻ mới; tóm tắt "Còn 1 mục cần sửa…" |
| STT-AC-04 | Mock E-34 trả 422 `duplicate` ở `name` | Lưu | Lỗi đúng ô tên của thẻ đó; thẻ khác đã lưu thành công nhận `colorIndex` từ E-26 |
| STT-AC-05 | Mock E-34 trả 422 `out_of_range` `price` `params {min:20000, max:100000000}` với giá 10.000 | Lưu | Ô giá hiện "Giá vé khác 0 phải từ 20.000 ₫." (`vi`) / "A paid ticket must cost at least ₫20,000." (`en`) |
| STT-AC-06 | Mock E-34 trả 422 `TICKET_TYPE_LIMIT_REACHED` | Lưu thẻ mới | `Notice` `studio.types.limit` trên thẻ; E-26 được gọi lại; thẻ vẫn trên form |
| STT-AC-07 | Đổi giá thẻ 1 và thêm thẻ 2 | Lưu | Đúng 2 request theo thứ tự `PATCH` rồi `POST`, body chỉ chứa trường đổi (§5); sau đó đúng 1 lần E-26 |
| STT-AC-08 | Thẻ `SEAT` | Quan sát | Không có ô sức chứa; hiện `studio.types.capacity.fromMap` + `…fromMap.seat`; nút chính "Lưu và sang Sơ đồ" |
| STT-AC-09 | Chỉ thẻ `GA`, đã lưu | Bấm "Lưu và sang Xem trước" khi không đổi gì | Không request ghi; điều hướng `/studio/events/{id}/preview` |
| STT-AC-10 | Giá 0 | Quan sát | Chú thích "Miễn phí, không cần thẻ" |
| STT-AC-11 | Sự kiện `PUBLISHED`, thẻ GA `gaCapacity 300`, `used 48` | Quan sát | Mô hình vô hiệu kèm `validation.locked_after_publish`; "Tối thiểu 48: đang giữ và đã bán"; nút 🗑 vô hiệu + "Không xóa được: đã có 48 vé…"; không có nút thêm, có `studio.types.add.locked`; `Notice` `studio.types.live.body` |
| STT-AC-12 | Như 11 | Nhập sức chứa 40, mock E-35 409 `CAPACITY_BELOW_USED` `used 52` | Ô sức chứa lỗi "Không thấp hơn 52 vé đang giữ và đã bán."; chú thích tối thiểu cập nhật 52 |
| STT-AC-13 | Thẻ đã lưu `used 0` | Bấm 🗑, "Xóa loại vé" | `DELETE` gửi ngay; 204 → thẻ biến mất; thẻ khác đang có thay đổi chưa lưu giữ nguyên giá trị đang gõ |
| STT-AC-14 | Mock E-36 409 `TICKET_TYPE_IN_USE` `used 1` | Xác nhận xóa | Hộp thoại đóng; thẻ còn; nút 🗑 vô hiệu với `{count}` = 1 |
| STT-AC-15 | Hộp thoại xóa mở | Bấm `Esc` | Đóng, không request; tiêu điểm về nút 🗑 |
| STT-AC-16 | Có thay đổi chưa lưu | Bấm bước "Thông tin" | Hộp thoại "Rời trang mà chưa lưu?"; "Ở lại" giữ thay đổi; "Rời đi" điều hướng |
| STT-AC-17 | Mock E-35 503 `Retry-After: 2` rồi 200 | Lưu | `Notice` `warning` đếm ngược; request gửi lại đúng 1 lần sau 2 giây; lưu xong `studio.save.saved` |
| STT-AC-18 | Lỗi mạng ở `POST` thẻ "Sinh viên" (0 ₫, 50), lần thử lại 422 `duplicate`, E-26 có "Sinh viên" `GA` 0 ₫ 50 | Lưu | Không hiện lỗi; thẻ nhận `ticketTypeId` của loại vé đó |
| STT-AC-19 | Sự kiện `CANCELLED` | Mở màn | `studio.types.readonly`; mọi ô và nút ghi vô hiệu |
| STT-AC-20 | Tab B có thay đổi chưa lưu, tab A lưu | Tab B lấy lại focus | Tab B **không** gọi E-26, giá trị đang gõ giữ nguyên |
| STT-AC-21 | 390 px | Mở màn có 2 thẻ | Một cột; thẻ hướng dẫn là `<details>` đóng; đúng một nút chính nhìn thấy |
| STT-AC-22 | axe-core | Mọi trạng thái §7 | 0 vi phạm; nhóm mô hình là `radiogroup` có nhãn; nút 🗑 có `aria-label` chứa tên loại vé |

## 10. Ca kiểm thử E2E

Playwright, `frontend/e2e/studio/`; API thật trên `make up seed` (người dùng tổ chức tạo trong spec, `e2e-<uuid>@demo.test`), mock cho lỗi; mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| STT-01 | `studio-types-ga-three` | Tạo sự kiện, sang Loại vé, thêm "Đặt sớm" 300.000 × 100, "Phổ thông" 450.000 × 300, "Sinh viên" 0 × 50; "Lưu và sang Xem trước" | 3 request `POST`; `colorIndex` 1, 2, 3; về `…/preview`; bước Sơ đồ không có trên thanh bước (J-4 bước 2) |
| STT-02 | `studio-types-limit` (mock) | Thêm tới 5 thẻ; mock 422 `TICKET_TYPE_LIMIT_REACHED` cho thẻ thứ 5 | Nút thêm vô hiệu ở 5; thông báo đúng thẻ (khớp P2-04.2) |
| STT-03 | `studio-types-duplicate-name` | Thêm "VIP" và "vip" | Lỗi ở ô tên thẻ 2 trước khi gửi (khớp P2-04.2) |
| STT-04 | `studio-types-delete-reuse-colour` | 3 thẻ đã lưu; xóa thẻ màu 2; thêm thẻ mới; lưu | Thẻ mới `colorIndex = 2` (DR-26) |
| STT-05 | `studio-types-live-capacity` | Sự kiện GA đã xuất bản, 1 vé đang giữ (seed); giảm sức chứa xuống 0; rồi về 1 | Lỗi `capacity.floor` với 1; giá trị 1 lưu được |
| STT-06 | `studio-types-unsaved-guard` | Sửa giá, bấm "Quay lại Thông tin", "Ở lại", rồi "Rời đi" | Hộp thoại hiện; lần hai điều hướng, giá không đổi ở server |
| STT-07 | `studio-types-narrow` | 390 × 844, như STT-01 | STT-AC-21 |

## Quyết định phát sinh khi viết tài liệu này

**DR-195 · Bước Loại vé lưu theo từng loại vé, không dùng `rowVersion`.** *Vấn đề:* DR-70 và DOC-38 §6.3 ghi bước Thông tin và Loại vé lưu thủ công "`PATCH` kèm `rowVersion`", nhưng E-34…36 là ba endpoint theo từng loại vé, không nhận `rowVersion` (DOC-37, DOC-20 §5), và canvas có nút "Lưu bản nháp" cho cả danh sách. Chưa nói một lần lưu gồm nhiều thay đổi gửi thế nào, lỗi một phần thì sao, xóa có chờ "Lưu" không. *Quyết định:* form giữ cả danh sách; "Lưu" gửi tuần tự `PATCH` (thẻ đã đổi) rồi `POST` (thẻ mới), lỗi một request không dừng request sau, cuối cùng gọi lại E-26 một lần; xóa loại vé đã lưu gọi E-36 ngay sau hộp thoại xác nhận (không chờ "Lưu"); không có `rowVersion` và không có hộp thoại `STALE_EVENT_VERSION` ở bước này, hai tab sửa cùng loại vé thì lần ghi sau thắng theo trường; ràng buộc 5 loại và tên duy nhất vẫn do server giữ dưới khóa dòng sự kiện. *Hệ quả:* không đổi API; DOC-38 §6.3 và §8.4 phải ghi rõ `rowVersion` chỉ áp dụng cho Studio 02; một lần lưu có thể thành công một phần (màn hiện lỗi theo thẻ). *Ghi vào:* DOC-56, DOC-38, DOC-84 (FL-07).

**DR-196 · Lỗi `out_of_range` của loại vé mang `params.min` và `params.max`.** *Vấn đề:* canvas cần câu "Giá vé khác 0 phải từ {min}" nhưng `payment.min-amount` là cấu hình server (DR-13, chờ S-02 hiệu chỉnh); viết cứng 20.000 ở client sẽ lệch khi đổi cấu hình. *Quyết định:* lỗi 422 `VALIDATION_FAILED` với `rule = out_of_range` ở `price` của E-34, E-35 có `params: {"min": <payment.min-amount>, "max": <payment.max-amount>}`; ở `gaCapacity` có `params: {"min": 1, "max": 100000}`. Client không kiểm khoảng giá trước khi gửi, chỉ kiểm số nguyên ≥ 0. *Hệ quả:* DOC-37 E-34, E-35 ghi `params` của `out_of_range`; DOC-20 §5.1 thêm cột `params`; DOC-35 không đổi (`params?` đã có). *Ghi vào:* DOC-37, DOC-20, DOC-56.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-195, DR-196 (mục trên).
