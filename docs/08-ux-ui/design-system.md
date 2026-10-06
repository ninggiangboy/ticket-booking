# Design system "Vé giấy"

> Trạng thái: **Review** · Cập nhật: 2026-10-06 · DOC-39
> Phụ thuộc: SDD gốc §13, [Sổ quyết định](../00-decision-register.md) (DR-03, 10, 26, 54, 68, 69), [DOC-06](../02-glossary.md), [DOC-38](ux-principles-and-ia.md) (UXP-01, 08, 09), [DOC-11](../03-architecture/tech-stack-and-versions.md), [DOC-12](../03-architecture/code-architecture.md) §6
> Người dùng chính: P1-01 (khung frontend), mọi task frontend; DOC-41…60 (màn hình dẫn component theo tên ở đây); DOC-51 (email dùng cùng token)

Tài liệu định nghĩa token, mã hóa trạng thái ghế, họa tiết vé, catalog component và tiêu chí tiếp cận của design system "Vé giấy" v0.1. Nguồn là các artboard `Design system · Nền tảng` (`Main`), `Design system · Component` (`Components`) và `Design system · Component studio và trạng thái` (`ComponentsStudio`) của canvas thiết kế. Bố cục màn hình ở DOC-41…60; nguyên tắc dùng ở [DOC-38](ux-principles-and-ia.md); chuỗi giao diện ở [DOC-40](ui-states-and-copy.md).

Hướng thiết kế: giấy xám lạnh, mực đen, **một** màu nhấn đỏ như con dấu; góc nhỏ, nổi khối bằng viền mực chứ không bằng bóng đổ; mọi thứ là mã hoặc số (mã ghế, giá, đồng hồ, mã đơn) dùng chữ mono để cột thẳng hàng. Không có dark mode ở giai đoạn này (DR-68). Mọi số liệu mẫu trong canvas (tên sự kiện, giá, mã vé) là dữ liệu mẫu.

Quyết định mới khi viết tài liệu này ghi DR-131…137 (mục 11).

## 1. Cách dùng token trong code

- Token là **CSS custom properties** khai báo một lần ở `frontend/src/styles/tokens.css`, nạp trước mọi CSS Module (DR-68, DR-03: "CSS Modules + CSS custom properties, không thêm framework CSS").
- Component chỉ dùng `var(--…)`, không viết mã hex trong `*.module.css`. ESLint (`stylelint-declaration-strict-value` cho `color`, `background`, `border-color`, `fill`, `stroke`) chặn hex ngoài `tokens.css`.
- Stripe Payment Element và Konva (canvas không đọc CSS) nhận giá trị qua một đối tượng `tokens` đọc từ `getComputedStyle(document.documentElement)` lúc khởi động (mục 8, mục 5.3).
- Email dùng cùng giá trị nhưng viết thẳng hex trong mẫu Thymeleaf (client email không hỗ trợ custom properties); bảng map ở DOC-51.
- Tên sản phẩm lấy từ `APP_NAME` (DR-54); dấu chấm đỏ sau tên (`ticket.`) là một phần của logo và dùng `--stamp`.

```css
/* frontend/src/styles/tokens.css (trích) */
:root {
  --paper: #ECEEF1;
  --surface: #FFFFFF;
  --line: #D3D7DE;
  --ink-muted: #5B6272;
  --ink-2: #2E323C;
  --ink: #12141A;
  --stamp: #D63312;
  --stamp-deep: #B5290C;
  --stamp-tint: #FCE9E4;
  --confirm: #1B7F4B;
  --type-1: #BF2A78; --type-2: #2747D9; --type-3: #E5A21A; --type-4: #0B7F6F; --type-5: #6A3FD0;
  --font-display: "Barlow Condensed", "Arial Narrow", sans-serif;
  --font-body: "Be Vietnam Pro", "Segoe UI", system-ui, sans-serif;
  --font-mono: "IBM Plex Mono", Menlo, Consolas, monospace;
}
```

## 2. Màu

### 2.1 Giấy và mực (canvas `Main` mục A)

| Token | Tên canvas | Giá trị | Dùng cho | Tương phản (planned, tính ngày 2026-10-06) |
| --- | --- | --- | --- | --- |
| `--paper` | Nền giấy | `#ECEEF1` | Nền trang, lỗ khuyết đường xé | — |
| `--surface` | Mặt thẻ | `#FFFFFF` | Thẻ, ô nhập, hộp thoại | — |
| `--line` | Đường kẻ | `#D3D7DE` | Viền thẻ 1 px, đường phân cách | 1,44 trên trắng (chỉ trang trí, không mang nghĩa) |
| `--ink-muted` | Mực nhạt | `#5B6272` | Nhãn mono, chữ phụ, chú thích | 6,11 trên trắng · 5,26 trên `--paper` |
| `--ink-2` | Mực vừa | `#2E323C` | Chữ thân bài | 12,82 trên trắng · 11,03 trên `--paper` |
| `--ink` | Mực | `#12141A` | Tiêu đề, nút chính, viền nút | 18,41 trên trắng · 15,84 trên `--paper` |

Mực là màu của chữ và của nút chính. Mặt thẻ trắng đặt trên nền giấy; **không dùng bóng đổ mềm** (UXP-01, canvas `Main` mục A).

### 2.2 Dấu đỏ và ngữ nghĩa (canvas `Main` mục B)

| Token | Tên canvas | Giá trị | Dùng cho | Tương phản |
| --- | --- | --- | --- | --- |
| `--stamp` | Dấu đỏ | `#D63312` | Viền con dấu, viền ô lỗi, nút "gấp", dấu chấm logo, vòng focus | 4,84 trên trắng (đạt 4,5; chữ trắng trên nền này cũng 4,84) |
| `--stamp-deep` | Dấu đỏ đậm | `#B5290C` | **Chữ** đỏ trên nền sáng (thông báo lỗi, nhãn lỗi, đồng hồ gấp), `a:hover` | 6,39 trên trắng · 5,45 trên `--stamp-tint` · 5,50 trên `--paper` |
| `--stamp-tint` | Dấu đỏ nhạt | `#FCE9E4` | Nền thông báo lỗi, nền thẻ đồng hồ khi dưới 2 phút | — |
| `--confirm` | Đã xác nhận | `#1B7F4B` | Biểu tượng "xong", viền thông báo thành công | 5,02 trên trắng |

Quy tắc: dấu đỏ là **màu nhấn duy nhất**, dành cho thứ cần chú ý ngay: đồng hồ giữ vé gần hết, giới hạn, lỗi, việc không hoàn tác (UXP-01). Chữ đỏ trên nền sáng dùng `--stamp-deep`, không dùng `--stamp`. Nút đỏ chỉ xuất hiện khi còn dưới 2 phút.

### 2.3 Màu loại vé (canvas `Main` mục C)

Gán cho loại vé bằng `ticket_type.color_index` (1–5): `color_index` nhỏ nhất chưa dùng trong event lúc tạo; xóa loại vé thì màu được dùng lại (DR-26, tối đa 5 loại mỗi event). Loại vé có `color_index = N` dùng `--type-N`; màu lưu trong database, không suy ra từ vị trí.

| Token | Tên | Giá trị | Tương phản trên trắng | Chữ lên trên màu này |
| --- | --- | --- | --- | --- |
| `--type-1` | Hồng sen | `#BF2A78` | 5,50 | Trắng |
| `--type-2` | Lam | `#2747D9` | 7,02 | Trắng |
| `--type-3` | Hổ phách | `#E5A21A` | **2,21** | **Mực** `--ink` (không dùng chữ trắng) |
| `--type-4` | Ngọc | `#0B7F6F` | 4,91 | Trắng |
| `--type-5` | Tím | `#6A3FD0` | 6,49 | Trắng |

Năm màu khác nhau cả sắc lẫn độ sáng và không trùng dấu đỏ. Vì `--type-3` không đạt 3:1 cho đồ họa, **màu loại vé không bao giờ là cách duy nhất** để biết loại vé: luôn kèm tên loại vé và giá (chip, chú giải; UXP-08).

### 2.4 Token mở rộng (có trong canvas, chưa có tên ở DR-68)

Canvas dùng thêm một số giá trị viết thẳng. Chúng được đặt tên ở đây để code không rải mã hex (DR-133).

| Token | Giá trị | Dùng cho | Nguồn canvas |
| --- | --- | --- | --- |
| `--line-strong` | `#868C99` | Viền ô nhập, ghế "có người giữ", đoạn "đang giữ" của thanh số liệu | `DangNhap` (ô email), `Main` mục D, `ComponentsStudio` mục J |
| `--tear` | `#B9BEC8` | Đường xé nét đứt `2px dashed` | `TrangLoi`, `KetQua` |
| `--seat-sold` | `#C9CDD5` | Ghế đã bán (đặc, xám nhạt) | `Main` mục D, `ChonCho` |
| `--warn-fg` | `#6E4A00` | Chữ cảnh báo/tạm thời (E4 "Đang đông", thông báo hết phiên) | `TrangLoi`, `DangNhap` |
| `--warn-bg` | `#FBF0D6` | Nền cảnh báo | `DangNhap` |
| `--warn-bd` | `#B8860B` | Viền cảnh báo, con dấu "Đang đông" | `TrangLoi`, `DangNhap` |
| `--confirm-fg` | `#135C36` | Chữ thông báo thành công | `DangNhap` ("Bạn đã đăng xuất…") |
| `--confirm-bg` | `#E6F3EC` | Nền thông báo thành công | `DangNhap` |

Tương phản: `--warn-fg` trên `--warn-bg` 7,01; `--confirm-fg` trên `--confirm-bg` 7,04; `--line-strong` trên trắng 3,37 (đạt 3:1 cho viền điều khiển).

## 3. Chữ (canvas `Main` mục E)

Ba họ chữ, **tự host** bằng `@fontsource` (DR-68), không gọi Google Fonts lúc chạy. Cắt tập con `latin` và `vietnamese` để giữ dung lượng.

| Họ | Gói `@fontsource` | Trọng số nạp | Dùng cho |
| --- | --- | --- | --- |
| Barlow Condensed | `@fontsource/barlow-condensed` | 600, 700 | Tiêu đề (chữ hoa, hẹp như mặt vé), logo |
| Be Vietnam Pro | `@fontsource/be-vietnam-pro` | 400, 500, 600 | Thân bài, nút, nhãn |
| IBM Plex Mono | `@fontsource/ibm-plex-mono` | 400, 500, 600 | Mã ghế, giá, đồng hồ, mã đơn, mã vé, nhãn mono |

Thang chữ:

| Cấp | Họ · trọng số | Cỡ / dòng | Ví dụ canvas | Ghi chú |
| --- | --- | --- | --- | --- |
| Display | Barlow Condensed 700 | 64 / 1,1 | "Chọn chỗ của bạn" | Chữ hoa; `clamp(36px, 7vw, 64px)` trên điện thoại |
| H1 | Barlow Condensed 700 | 40 / 1,15 | "Hòa nhạc Giao Mùa" | Chữ hoa; `clamp(32px, 7vw, 40px)` |
| H2 | Barlow Condensed 600 | 28 / 1,2 | "Đơn của bạn" | |
| Title | Be Vietnam Pro 600 | 18 | "Hàng C · Ghế 8" | |
| Body | Be Vietnam Pro 400 | 16 / 1,5 | Văn bản thường | Màu `--ink-2` |
| Small | Be Vietnam Pro 400 | 14 | Chú thích | Màu `--ink-muted` hoặc `--ink-2` |
| Mono | IBM Plex Mono 500 | 16 | `C-08 · 950.000 ₫ · 09:42` | Số dùng `font-variant-numeric: tabular-nums` |
| Label | IBM Plex Mono 600 | 12, chữ hoa, `letter-spacing: 0.08em` | "Hàng · Ghế · Loại vé" | Màu `--ink-muted`; cỡ 11 px chỉ ở nhãn trong thẻ vé |

Quy tắc:

- Tiêu đề in hoa bằng `text-transform: uppercase` (CSS), **không** gõ hoa trong chuỗi i18n, để `en` và `vi` dùng cùng chuỗi nguồn.
- Dấu tiếng Việt: dòng của H1/Display tối thiểu 1,1; không đặt `line-height` dưới 1,1 (dấu mũ bị cắt ở Barlow Condensed).
- Văn bản dài (mô tả sự kiện) tối đa 70 ký tự mỗi dòng (`max-width: 65ch`).
- Số tiền, mã, đồng hồ luôn mono.

## 4. Khoảng cách, bo góc, viền (canvas `Main` mục F, G)

| Mục | Giá trị |
| --- | --- |
| Thang khoảng cách (bước 4 px) | `--space-1…8` = 4, 8, 12, 16, 24, 32, 48, 64 px |
| Trong thẻ | 16 và 24 px |
| Giữa các khối | 24 và 40 px |
| Vùng chạm tối thiểu | 44 px (UXP-09); nút chính cao 52 px, nút phụ cao 44 px |
| Gutter trang | `clamp(16px, 4vw, 32px)`; nội dung tối đa 1360 px (trang), 944 px (kết quả), 584/504 px (thẻ đơn lẻ như đăng nhập, lỗi) |
| Bo góc | `--radius-sm` 4 px (thẻ, ô nhập, thông báo) · `--radius-md` 6 px (nút, thẻ lớn) · `--radius-pill` 999 px (chip) |
| Viền thẻ | 1 px `--line` |
| Viền nút | 1,5 px `--ink` |
| Viền ô nhập | 1 px `--line-strong`; lỗi 2 px `--stamp` |
| Vòng focus | `outline: 2px solid var(--stamp); outline-offset: 2px` cho `button`, `a`, `input`, `summary` |
| Bóng đổ | **Không dùng** (nổi khối bằng viền mực) |

Chú ý: vòng focus `--stamp` trên nền trắng 4,84 và trên `--paper` 4,16 (đạt 3:1 cho thành phần giao diện). Trên nền `--ink` (nút chính) vòng focus `--stamp` vẫn nhìn thấy nhờ `outline-offset: 2px` đặt ngoài nút.

## 5. Mã hóa trạng thái ghế và khu vực (canvas `Main` mục D, `Components` mục D, `ComponentsStudio` mục K)

### 5.1 Bốn trạng thái ghế

Trạng thái hiển thị của chỗ (glossary: display status of a seat). **Không chỉ dựa vào màu**: phân biệt được khi in đen trắng (UXP-08).

| Trạng thái | Nhãn `vi` | Hình khối | Màu | Dấu phụ | Tương tác |
| --- | --- | --- | --- | --- | --- |
| Còn trống | "Còn trống" | Đặc | Màu loại vé (`--type-N`) | — | Bấm để chọn |
| Đang chọn | "Đang chọn" | Đặc | `--ink` | **Dấu tích** trắng ở giữa, **viền kép** (vòng `--paper` 3 px rồi vòng `--ink` 2 px) | Bấm lần nữa để bỏ chọn |
| Có người giữ | "Có người giữ" | **Rỗng** | Viền `--line-strong` 2 px, nền trong suốt | — | Bấm chỉ hiện tooltip trạng thái, không thêm vào đơn (DR-69) |
| Đã bán | "Đã bán" | Đặc | `--seat-sold` (xám nhạt) | — | Như trên |

Trạng thái ở màn **Chọn chỗ** tô theo **loại vé**; màn **Studio 07 (Bán vé)** tô theo **trạng thái** `Đã bán` (đặc `--ink`), `Đang giữ` (đặc `--line-strong`), `Còn trống` (đặc `--paper`, viền `--line`), `Ngoài bộ lọc` (rất nhạt); loại vé chuyển thành chip lọc (canvas `ComponentsStudio` mục K).

Ghế cho `aria`: mỗi ghế là một phần tử có tên đọc được "Hàng C, ghế 9, VIP, 1.800.000 ₫, còn trống". Chế độ Danh sách là đường tiếp cận chính (DR-69).

### 5.2 Hình ghế

Ghế 26 px vuông, cách nhau 6 px; mặt ghế quay về sân khấu theo đường hàng. Bo góc ghế: hai góc trên **30%** cạnh, hai góc dưới **12%** (như lưng ghế). Nhãn ghế (số) vẽ mono 10–11 px chỉ khi zoom ≥ 100%.

### 5.3 Khu vực (zone, GA khu đứng)

Khối có viền nét đứt dưới loại vé; hiển thị "Còn N vé" và, khi đã chọn, "Đang chọn N vé" với nền `--ink`. Trạng thái "hết" dùng nhãn "Hết vé" và nền xám, không chỉ đổi màu.

### 5.4 Konva

Canvas Konva không đọc CSS. `map-core/render` nhận một đối tượng `SeatTheme` dựng từ token lúc khởi động:

```ts
export interface SeatTheme {
  typeColors: [string, string, string, string, string]; // --type-1…5
  selectedFill: string;   // --ink
  selectedRing: string;   // --paper (vòng trong) rồi --ink (vòng ngoài)
  heldStroke: string;     // --line-strong
  soldFill: string;       // --seat-sold
  labelFont: string;      // --font-mono
}
```

## 6. Họa tiết vé (canvas `Main` mục H)

| Họa tiết | Cách dựng | Dùng ở |
| --- | --- | --- |
| **Đường xé** | Hàng cao 20 px, margin ngang âm bằng padding thẻ; hai hình tròn 20 px nền `--paper` đặt lệch −10 px ở hai đầu (lỗ khuyết), giữa là `border-top: 2px dashed var(--tear)` cách mép 18 px; `aria-hidden="true"` | Giữa phần chọn và phần trả tiền; thẻ vé; thẻ lỗi; thẻ đăng nhập |
| **Cuống vé** | Thẻ vé chia hai phần bằng đường xé: trên là tên sự kiện, ngày giờ, địa điểm; dưới là Hàng · Ghế · Loại vé (mono 24 px) và Mã vé | Thẻ vé (mục 7.12), email vé |
| **Con dấu** | Khung `2px solid --stamp`, `border-radius 4px`, chữ mono 12–22 px chữ hoa `letter-spacing 0.08–0.1em`, xoay −3° đến +4°, màu `--stamp-deep`; `aria-hidden` khi chữ đã có ở nơi khác | Chỉ cho **kết quả cuối**: "Đã thanh toán", mã lỗi 404/503/500, "Có thay đổi" ở email đổi lịch. Không dùng cho trạng thái đang diễn ra |

## 7. Catalog component

Mỗi component có tên React (thư mục `frontend/src/shared/ui/`), nguồn canvas và quy tắc. Tên là thống nhất để DOC-41…60 dẫn lại. "Biến thể" là prop.

### 7.1 Button (`Button`) — canvas `Components` mục A

| Biến thể | Kích thước | Kiểu | Khi dùng |
| --- | --- | --- | --- |
| `primary` | cao 52 px, padding ngang 24–32 px | Nền `--ink`, chữ trắng, viền 1,5 px `--ink` | **Một** nút mỗi màn: "Giữ vé trong 10 phút", "Mua vé" |
| `primary-compact` | cao 44 px | như trên | Trong thẻ nhỏ: "Xem sơ đồ" |
| `secondary` | cao 52 hoặc 44 px | Nền trắng, chữ `--ink`, viền 1,5 px `--ink` | "Về trang sự kiện" |
| `text` | cao 44 px | Không viền, gạch chân `text-underline-offset: 4px`, 14 px 600 | "Hủy giữ vé", "Gửi lại", "Dùng email khác" |
| `urgent` | cao 52 px | Nền `--stamp`, chữ trắng | "Thanh toán ngay" khi còn dưới 2 phút; việc không hoàn tác trong hộp thoại ("Hủy sự kiện") |
| `disabled` | như biến thể gốc | Nền `--paper`, chữ `--ink-muted`, `cursor: not-allowed`, thuộc tính `disabled` (không dùng `aria-disabled` một mình) | "Hết vé", "Mở bán sau" |
| `icon` | 44 × 44 px | Chỉ biểu tượng, **bắt buộc** `aria-label` | Đóng, tăng/giảm số lượng |

Trạng thái: `hover` (nền sáng thêm 8%), `focus-visible` (vòng mục 4), `loading` (thêm chữ "Đang giữ vé…" và `aria-busy="true"`, vẫn `disabled`, không đổi chiều rộng).

### 7.2 Ô nhập (`TextField`, `Select`, `Combobox`) — mục B

Nhãn luôn nhìn thấy phía trên (14 px 600), cao 52 px, viền 1 px `--line-strong`, bo 4 px. Lỗi: viền 2 px `--stamp`, thông báo bên dưới `role="alert"` chữ `--stamp-deep` 14 px 500, `aria-invalid="true"` và `aria-describedby` trỏ thông báo. Trợ giúp bên dưới nhãn lỗi là chữ `--ink-muted` 14 px. Email: `type="email"` ở ô thật; canvas dùng `type="text"` để mô phỏng lỗi định dạng, code dùng `type="email"` kèm `noValidate` và kiểm bằng zod (DR-03).

### 7.3 Bộ tăng giảm số lượng (`QuantityStepper`) — mục B ("Số lượng · khu đứng, vé GA")

Hai nút `icon` (44 px) `−` và `+` với `aria-label` "Bớt một vé <tên>" / "Thêm một vé <tên>", giữa là số mono 18 px `role="status"`. Giá trị tối thiểu 0; nút `−` vô hiệu ở 0; nút `+` vô hiệu khi hết vé hoặc chạm trần 50 vé mỗi lần giữ (DR-41), trần có chú thích `checkout.limit.tooManyUnits`.

### 7.4 Công tắc chế độ xem (`ViewToggle`) — mục B ("Sơ đồ | Danh sách")

Nhóm hai nút `role="radiogroup"`/`radio`; mũi tên trái phải chuyển; nhớ lựa chọn trong `localStorage["tb.view"]` (bọc `try/catch`).

### 7.5 Chip loại vé (`TicketTypeChip`) — mục C

Viên thuốc (`--radius-pill`, viền 1 px `--line`): ô vuông 12 px màu `--type-N` + tên loại vé + giá mono. Dùng cho chú giải sơ đồ, danh sách loại vé, bộ lọc ở Studio 07 (`aria-pressed`). Ô màu là bổ sung, tên và giá là nội dung chính (UXP-08).

### 7.6 Nhãn trạng thái sự kiện (`EventStatusBadge`) — mục C, `ComponentsStudio` mục I

Tám giá trị lấy từ `displayStatus` (DR-24): `DRAFT` "Bản nháp", `UPCOMING` "Sắp mở bán", `ON_SALE` "Đang mở bán", `SOLD_OUT` "Hết vé", `SALE_CLOSED` "Đã đóng bán", `PAUSED` "Tạm dừng", `ENDED` "Đã kết thúc", `CANCELLED` "Đã hủy". Nhãn "Đã xuất bản" của canvas Studio là trạng thái lưu `PUBLISHED` chưa đến giờ bán, hiện như `UPCOMING`/`ON_SALE` tùy giờ; nhãn "Đã xuất bản" dùng ở Studio 06 khi cần nói riêng. Khung viên thuốc; **viền nét đứt chỉ dùng cho thứ chưa công khai** (`DRAFT`); màu không đứng một mình: luôn có chữ. Cặp màu/viền theo bảng:

| `displayStatus` | Nền | Chữ | Viền |
| --- | --- | --- | --- |
| `DRAFT` | trong suốt | `--ink-muted` | 1 px nét đứt `--line-strong` |
| `UPCOMING` | trắng | `--ink` | 1 px `--ink` |
| `ON_SALE` | `--ink` | trắng | `--ink` |
| `SOLD_OUT` | `--stamp-tint` | `--stamp-deep` | 1 px `--stamp` |
| `SALE_CLOSED`, `ENDED` | `--paper` | `--ink-2` | 1 px `--line-strong` |
| `PAUSED` | `--warn-bg` | `--warn-fg` | 1 px `--warn-bd` |
| `CANCELLED` | trắng | `--stamp-deep` | 1 px `--stamp`, chữ gạch ngang không dùng |

### 7.7 Khu vực (`ZoneBlock`) — `Components` mục D

Xem mục 5.3. Hai trạng thái canvas: trống ("Còn 86 vé") và đang chọn ("Đang chọn 2 vé").

### 7.8 Đồng hồ giữ vé (`HoldTimer`) và nhịp bước (`Stepper`) — `Components` mục E

`HoldTimer`: thẻ có nhãn "Giữ vé còn" và `mm:ss` mono 28–32 px. Bình thường nền trắng viền `--ink`; dưới 2 phút nền `--stamp-tint`, viền `--stamp`, nhãn "Sắp hết giờ" (UXP-04, DOC-38 §8.1). Một vùng `aria-live="polite"` riêng chỉ thông báo khi vào trạng thái gấp, không đọc từng giây. Đồng hồ tính theo mốc `expiresAt` của server (DR-66).

`Stepper` (thanh bước mua vé): `Chọn chỗ → Thanh toán → Nhận vé`, vòng 24 px: xong = viền `--ink` + dấu tích, đang ở = nền `--ink` + số mono + `aria-current="step"`, chưa tới = viền `--line-strong`. Trên điện thoại xuống hàng thứ hai và chia đều. Với sự kiện chỉ GA bước 1 là "Chọn vé".

### 7.9 Thông báo (`Notice`) — `Components` mục F, `ComponentsStudio` mục L

Bốn loại, **có biểu tượng và `role`** ngoài màu (UXP-08):

| Loại | Nền · viền · chữ | Biểu tượng | `role` | Ví dụ |
| --- | --- | --- | --- | --- |
| `info` | trắng · `--line` · `--ink-2` | chữ "i" trong vòng | `status` | "Tình trạng ghế chỉ là gợi ý" |
| `success` | `--confirm-bg` · `--confirm` · `--confirm-fg` | dấu tích trong vòng | `status` | "Đã xuất bản. Kho vé đã tạo đủ 480 vé." |
| `warning` | `--warn-bg` · `--warn-bd` · `--warn-fg` | đồng hồ hoặc tam giác | `status` | "Đang rất đông… tự thử lại sau 3 giây" |
| `error` | `--stamp-tint` · `--stamp` · `--stamp-deep` (tiêu đề) và `--ink-2` (nội dung) | tam giác chấm than | `alert` | "Ghế C-8 vừa có người giữ" |

Cấu trúc: tiêu đề đậm (tùy chọn) + một câu nội dung + hành động (nút `text`). **Lỗi luôn nói điều đã xảy ra và việc làm tiếp** (UXP-06).

### 7.10 Ô số liệu và thanh trạng thái (`Metric`, `SalesBar`) — `ComponentsStudio` mục J

`Metric`: nhãn mono + số lớn mono + chú thích. `SalesBar`: thanh nhiều đoạn cách nhau 2 px, không viền: `Đã bán` `--ink` · `Đang giữ` `--line-strong` · `Còn trống` `--paper`. **Số luôn viết bằng chữ bên cạnh; thanh chỉ để nhìn nhanh**; thanh có `role="img"` và `aria-label` "52% sức chứa 480: 249 vé đã bán, 35 vé đang giữ, 196 vé còn trống" (chuỗi `studio.sales.bar.aria`).

### 7.11 Danh sách điều kiện (`ChecklistRow`) — `ComponentsStudio` mục M

Một dòng: tên điều kiện, chi tiết, nhãn `Đạt` (biểu tượng tích + chữ) hoặc `Chưa đạt` (biểu tượng chấm than + chữ, `--stamp-deep`), liên kết "Mở sơ đồ" tới bước cần sửa. Dùng ở Studio 06 (`GET /organizer/events/{id}/publish-checks`, DR-65).

### 7.12 Thẻ vé (`TicketCard`) — `Components` mục G

Cuống vé (mục 6): đầu thẻ mono nhãn "Vé vào cửa · 1/2", tên sự kiện Barlow 30 px chữ hoa, ngày giờ và địa điểm 14 px; đường xé; chân thẻ `Hàng` · `Ghế` · `Loại vé` (mono 24 px) và `Mã vé` (mono 16 px, `user-select: all`). Với vé GA/khu vực không có số ghế, các ô `Hàng` và `Ghế` thay bằng tên khu/loại vé (DOC-27). Thẻ này dùng cho trang Kết quả, Vé của tôi và email vé. Mã vé theo DR-52 (`PP-XXXX-XXXX`).

### 7.13 Bước soạn sự kiện (`StudioStepper`) — `ComponentsStudio` mục H

Sáu bước `Thông tin · Loại vé · Sơ đồ · Xem trước · Xuất bản · Bán vé` (bước Bán vé chỉ sau khi xuất bản; Sơ đồ ẩn khi chỉ GA, DR-70). Trạng thái từng bước: xong (tích), đang ở, chưa tới. Chỉ bước xong hoặc đang ở bấm được. Có `<nav aria-label>` và `aria-current="step"`.

### 7.14 Thẻ sự kiện và ô ảnh (`EventCard`, `EventImage`) — `ComponentsStudio` mục S

Ô ảnh **16:9**, `object-fit: cover`, không kéo giãn; thẻ sự kiện và trang sự kiện dùng chung một ô nên lưới thẳng hàng. Có ảnh: `<img>` với `loading="lazy"`, `decoding="async"`, `width`/`height` để không nhảy layout, `src="/media/{id}"` (DR-38). Không có ảnh: **tem ngày** thay vào đúng ô: thứ (mono), `dd.MM` (Barlow 56 px), năm và giờ (mono). `alt=""` (trang trí) vì tên sự kiện đã nằm cạnh.

### 7.15 Hộp thoại xác nhận (`ConfirmDialog`) — `ComponentsStudio` mục N

`role="dialog"`/`alertdialog` với `aria-labelledby`, `aria-describedby`; cấu trúc và tiêu điểm theo DOC-38 §8.3. Nút phải gọi đúng tên việc, việc không hoàn tác dùng `urgent`. Có hai kiểu: `publish` (nhãn "Xuất bản") và `cancel-event` (nhãn "Hủy sự kiện").

### 7.16 Menu tài khoản (`AccountMenu`) — `ComponentsStudio` mục O

Nút "Tài khoản" (avatar chữ cái đầu, 32 px nền `--ink`) hoặc tên tổ chức (Studio); khi mở, panel 264 px, viền 1,5 px `--ink`, bo 6 px: dòng "Đang đăng nhập" (mono nhãn) + email (`overflow-wrap: anywhere`), rồi các mục cao 44 px, "Đăng xuất" ở cuối sau đường kẻ. Mục theo vai trò ở DOC-38 §4.

### 7.17 Khung chờ, trạng thái rỗng, lỗi tải (`Skeleton`, `EmptyState`, `LoadError`) — `ComponentsStudio` mục P

`Skeleton` giữ đúng hình dạng nội dung sắp hiện (cùng chiều cao); chuyển động nhấp nháy tắt khi `prefers-reduced-motion: reduce`. `EmptyState`: tiêu đề + một câu + nút kế tiếp (UXP-07). `LoadError`: nhãn mono "Lỗi tải" + tiêu đề + câu + nút "Thử lại". Chuỗi ở DOC-40 §1.

### 7.18 Trang lỗi (`ErrorPage`) — `ComponentsStudio` mục R, artboard E1–E5

Thẻ 584 px: nhãn mono, con dấu mã lỗi xoay +4°, tiêu đề, câu, (503) thanh tiến trình tự thử lại, (500) hộp "Mã yêu cầu" `user-select: all`, nút/liên kết kế tiếp, đường xé, chân thẻ. Lỗi tạm thời (503) dùng bộ `warning`. Chuỗi ở DOC-52 và DOC-40 §1.5.

### 7.19 Tab hồ sơ, bộ chọn ngôn ngữ (`LanguageSwitch`)

Bổ sung (canvas chưa vẽ ở artboard riêng, DR-10 yêu cầu bộ chọn ở header): nhóm hai nút `vi`/`en` mono 12 px, `aria-pressed`, cao 44 px vùng chạm; cạnh menu tài khoản. Đổi không tải lại trang.

### 7.20 Payment Element

Xem mục 8.

## 8. Stripe Payment Element `appearance` (DR-68)

Màu của Payment Element cấu hình qua `appearance.variables` từ **cùng token**, đọc từ `tokens` lúc khởi động `Elements`:

```ts
const appearance: Appearance = {
  theme: 'flat',
  variables: {
    colorPrimary: tokens.ink,            // #12141A
    colorBackground: tokens.surface,     // #FFFFFF
    colorText: tokens.ink,
    colorTextSecondary: tokens.inkMuted, // #5B6272
    colorDanger: tokens.stampDeep,       // #B5290C
    colorSuccess: tokens.confirm,        // #1B7F4B
    fontFamily: '"Be Vietnam Pro", system-ui, sans-serif',
    fontSizeBase: '16px',
    borderRadius: '4px',
    spacingUnit: '4px',
    focusBoxShadow: '0 0 0 2px #D63312',
    focusOutline: 'none',
  },
  rules: {
    '.Input': { border: '1px solid #868C99', minHeight: '52px' },
    '.Input--invalid': { border: '2px solid #D63312' },
    '.Label': { fontWeight: '600', fontSize: '14px' },
  },
};
```

Quy tắc: Stripe nhúng iframe nên **không** nhận `@fontsource`; truyền `fonts: [{ cssSrc: '/fonts/be-vietnam-pro-stripe.css' }]` trỏ về file tự host cùng origin (CSP của nginx cho phép `font-src 'self'` và `frame-src https://js.stripe.com`, DOC-32). Nhãn trường (Số thẻ, Hết hạn, Mã CVC) do Stripe cung cấp theo `locale` truyền vào `Elements({ locale })` bằng `vi` hoặc `en`. Chỉ thẻ (DR-47). Không vẽ lại ô thẻ.

## 9. Tiếp cận (UXP-08, UXP-09)

| Tiêu chí | Quy định | Kiểm bằng |
| --- | --- | --- |
| Vùng chạm | ≥ 44 × 44 px cho mọi điều khiển; ngoại lệ có điều kiện: ghế trên sơ đồ (DOC-38 §7.3) | DS-02 |
| Tương phản chữ | ≥ 4,5:1 (≥ 3:1 từ 24 px, hoặc 18,66 px đậm); tương phản điều khiển và đồ họa mang nghĩa ≥ 3:1. Cặp màu cấm: chữ trắng trên `--type-3`; chữ `--stamp` (không phải `-deep`) trên `--stamp-tint` (4,13 chưa đạt) | DS-01 |
| Không chỉ dựa vào màu | Trạng thái ghế, trạng thái sự kiện, thông báo, Đạt/Chưa đạt luôn có chữ hoặc hình khối | DS-03 |
| Bàn phím | Mọi chức năng dùng được bằng bàn phím; thứ tự Tab theo thứ tự đọc; không bẫy tiêu điểm ngoài hộp thoại; không bắt phím tắt toàn cục ngoài editor (DOC-22) | DS-04 |
| Tiêu điểm | `focus-visible` luôn thấy (mục 4); sau khi điều hướng, tiêu điểm vào `<h1>` của trang (`tabindex="-1"`) | DS-04 |
| Trình đọc màn hình | Một `<main id="main">` mỗi trang, liên kết "Bỏ qua tới nội dung" đầu trang; ảnh trang trí `alt=""`; biểu tượng chỉ có nút thì `aria-label`; thông báo động qua `aria-live` (đồng hồ chỉ thông báo khi vào trạng thái gấp) | DS-05 |
| Chuyển động | Tắt nhấp nháy, thanh tiến trình chạy mượt và hoạt ảnh con dấu khi `prefers-reduced-motion: reduce` | DS-06 |
| Ngôn ngữ | `<html lang>` theo locale hiện tại (`vi`/`en`); đoạn tiếng khác (tên người tổ chức) không đánh dấu riêng | DS-07 |
| Cỡ chữ | Không dùng `px` cho cỡ thân bài ở cấp người dùng đặt (dùng `rem` khi dựng), phóng chữ 200% không mất nội dung | DS-08 |

## 10. Test bắt buộc

Tiền tố `DS-` (kiểm tra chưa dùng ở `docs/`; đăng ký ở DOC-69). `DS-01…08` chạy bằng axe-core + Playwright trên Storybook hoặc trang `/__ds` chỉ có ở profile `dev`.

| ID | Kịch bản | Kết quả mong đợi |
| --- | --- | --- |
| DS-01 | Tính tương phản mọi cặp (chữ, nền) trong `tokens.css` mà catalog dùng (script `pnpm ds:contrast`) | Cặp chữ ≥ 4,5; cặp điều khiển ≥ 3,0; `--ink-muted` trên `--paper` = 5,26 (± 0,01); cặp `--type-3` + chữ trắng bị từ chối |
| DS-02 | Đo `getBoundingClientRect` của mọi `button`, `a`, `input`, `summary` ở 390 px | Cao và rộng ≥ 44 px (trừ ghế sơ đồ và liên kết nằm trong đoạn văn) |
| DS-03 | Chụp màn Chọn chỗ ở thang xám, nhận diện 4 trạng thái ghế bằng so sánh hình (ảnh mẫu) | 4 trạng thái khác nhau theo hình khối |
| DS-04 | Tab qua trang Thanh toán và Studio 02 | Thứ tự Tab theo thứ tự đọc; mọi điều khiển có vòng focus 2 px `--stamp` |
| DS-05 | axe-core trên 18 màn và biến thể trạng thái | 0 vi phạm `button-name`, `label`, `image-alt`, `landmark-one-main`, `aria-valid-attr` |
| DS-06 | Bật `prefers-reduced-motion` | Không có animation chạy quá 100 ms (kiểm bằng `getAnimations()`) |
| DS-07 | Đổi sang `en` | `document.documentElement.lang === "en"`, mọi chuỗi từ file `en` |
| DS-08 | Phóng trình duyệt 200% ở 1280 px | Không cắt nội dung, không thanh cuộn ngang ở trang mua vé |
| DS-09 | Lint: thêm `color: #FFF` vào một `*.module.css` | `pnpm lint` thất bại (quy tắc không hex ngoài `tokens.css`) |
| DS-10 | Fonts: tải trang Sự kiện với mạng ngoài chặn | Không request tới `fonts.googleapis.com`/`fonts.gstatic.com`; chữ vẫn đúng họ |
| DS-11 | Dựng `appearance` ở mục 8 từ `tokens` (Vitest, không gọi Stripe) | `variables.colorPrimary === '#12141A'`, `colorDanger === '#B5290C'`, `rules['.Input--invalid'].border` chứa `#D63312`; không giá trị nào là `undefined` |

## 11. Tóm tắt quyết định phát sinh khi viết tài liệu này

| ID tạm | Quyết định | Quyết bởi | Ghi vào |
| --- | --- | --- | --- |
| DR-133 | Đặt tên và giá trị token mở rộng của canvas: `--line-strong`, `--tear`, `--seat-sold`, `--warn-fg/bg/bd`, `--confirm-fg/bg` (mục 2.4); thang `--space-1…8`, `--radius-sm/md/pill` | Claude (Owner ủy quyền) | DOC-39 |
| DR-134 | Lint chặn mã hex ngoài `tokens.css` (`stylelint`) | Claude (Owner ủy quyền) | DOC-39, DOC-63 |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: DR-133, 4 (mục 11), chờ hợp nhất vào sổ quyết định.
