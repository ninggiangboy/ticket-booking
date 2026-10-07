# Email

> Trạng thái: **Approved** · Cập nhật: 2026-10-07 · DOC-51
> Phụ thuộc: SDD gốc §9.4, [DOC-06](../../02-glossary.md), [DOC-15](../../05-data/ops-model.md) §3, §7 (payload outbox), [DOC-14](../../05-data/domain-model.md) (`ticket.label`, `event.ticket_code_prefix`), [DOC-31](../../06-design/i18n.md) §5, [DOC-39](../design-system.md), [DOC-40](../ui-states-and-copy.md) §2, [Sổ quyết định](../../00-decision-register.md) (DR-10, 12, 21, 28, 29, 44, 52, 53, 54, 78), canvas `EmailDangNhap` (03), `EmailVe` (07b), `EmailDoiLich` (07c)
> Người dùng chính: P1-07 (email `magic-link`), P2-xx (email `tickets`), P3-xx (email `refund-pending`), [DOC-27](../../06-design/tickets-and-notifications.md) (relay và dựng email), [DOC-83](../../06-design/flows/auth-and-account.md) (FL-01), người dịch

Tài liệu đặc tả **bốn mẫu email** (DR-54): `magic-link`, `tickets`, `event-changed`, `refund-pending`. Mỗi mẫu có tiêu đề, nội dung HTML và text, biến, key i18n `vi`/`en` và link. Cách relay gửi, lease, backoff, `Message-ID` ở DOC-27 và DR-53; payload outbox ở DOC-15 §7.2; định dạng số, tiền, ngày ở DOC-40 §2 và DOC-31 §6. Tài liệu này không lặp các phần đó. Bốn mẫu không có "màn hình"; email là bề mặt giao diện thứ hai nên dùng mẫu A.5 rút gọn (không wireframe giao diện, không E2E).

## 1. Tổng quan

| Mẫu | Thông điệp | Gửi bởi | Khi nào | Nguồn dữ liệu | Có dòng outbox |
| --- | --- | --- | --- | --- | --- |
| `magic-link` | Đường dẫn đăng nhập | `MagicLinkService` gọi SMTP trực tiếp sau commit (DR-21) | `POST /auth/magic-link` (FL-01) | `login_token.locale`, token thô trong bộ nhớ request | Không |
| `tickets` | Vé của bạn | `OutboxRelay` (`EMAIL_TICKETS`) | Cùng transaction xác nhận đơn (DOC-26) | payload DOC-15 §7.2 | Có |
| `event-changed` | Đổi giờ, địa điểm hoặc cả hai | `OutboxRelay` (`EMAIL_EVENT_CHANGED`) | `PATCH /organizer/events/{id}` khi event không phải `DRAFT` (DR-29, FL-11) | payload DOC-15 §7.2 | Có, một dòng mỗi đơn `PAID` |
| `refund-pending` | Chờ hoàn tiền, ba lý do | `OutboxRelay` (`EMAIL_REFUND_PENDING`) | Đơn sang `REFUND_PENDING` (DR-44, DR-28) | payload DOC-15 §7.2 | Có |

Chỉ `magic-link` mất email khi SMTP lỗi mà người dùng biết ngay (503 `EMAIL_PROVIDER_UNAVAILABLE`); ba mẫu còn lại được thử lại tối đa 12 lần (DR-53).

## 2. Quy ước chung

### 2.1 Người gửi, header, ngôn ngữ

| Mục | Giá trị | Nguồn |
| --- | --- | --- |
| `From` | `${APP_NAME} <${MAIL_FROM}>`; mặc định `ticket <no-reply@ticket.localhost>` | DR-54 |
| `Subject` | key `email.<mẫu>.subject`, theo locale người nhận | DR-54 |
| `Message-ID` | `<outbox_id@APP_DOMAIN>` cho ba mẫu qua outbox; `magic-link` dùng `<login-{12 ký tự đầu hex của token_hash}@APP_DOMAIN>` (không lộ token) | DR-53, DR-109 |
| `Content-Language` | `vi` hoặc `en` | DR-10 |
| `Reply-To` | không đặt. Email là thông báo một chiều; liên hệ người tổ chức nằm trong thân `event-changed`, liên hệ hỗ trợ nằm trong `refund-pending` | DR-109 |
| Cấu trúc | `multipart/alternative`: `text/plain` rồi `text/html`, UTF-8 | DR-54 |
| Đính kèm, QR, ảnh | không có (SDD gốc 9.4). Không có pixel theo dõi, không có link theo dõi | DR-54 |
| Locale | `payload.locale` (ba mẫu qua outbox, chốt lúc ghi dòng, DR-10); `login_token.locale` cho `magic-link` | DOC-31 §2 |

### 2.2 Dựng và bố cục

- Thymeleaf, mỗi mẫu hai file: `src/main/resources/templates/email/<mẫu>.html` và `<mẫu>.txt`; khung chung ở `email/_layout.html` (header `ticket.`, chân thư). Chuỗi lấy bằng `#{email.<mẫu>.<phần>(args)}` từ `messages_<locale>.properties` (DOC-31 §5). Tham số là `{0}`, `{1}` theo thứ tự cột "Tham số" ở mục 8; số và ngày **định dạng trước** bằng Java rồi mới truyền vào (DOC-31 §5).
- **Bố cục bảng, CSS inline** (DR-109): canvas vẽ bằng `div` flex, nhưng nhiều ứng dụng thư không hỗ trợ flex. Mỗi mẫu dùng `<table role="presentation">` rộng tối đa 600 px, nền ngoài `#ECEEF1`, mỗi khối trắng là một `<table>` bo 6 px, đệm 32 px (16 px ở khối vé). Dưới 480 px khối thành một cột (`@media` trong `<style>` ở `<head>`; ứng dụng nào bỏ `<style>` thì vẫn đọc được vì bố cục gốc đã một cột).
- **Màu và chữ** lấy từ token DOC-39, ghi thẳng giá trị hex vì email không có biến CSS: chữ `#12141A`, chữ phụ `#5B6272`, thân `#2E323C`, nhấn `#D63312` (chấm sau "ticket" và viền tem), nhấn đậm `#B5290C`, nền `#ECEEF1`, đường đứt `#B9BEC8`.
- **Phông**: ứng dụng thư không tải được web font ổn định, nên chuỗi dự phòng là chính: tiêu đề `'Barlow Condensed','Arial Narrow',Arial,sans-serif` (đậm 700, chữ hoa); thân `'Be Vietnam Pro','Segoe UI',Arial,sans-serif`; số và mã `'IBM Plex Mono',Menlo,Consolas,'Courier New',monospace`. Không `<link>` Google Fonts trong email (DR-109).
- **Cuống vé** (khối vé của `tickets`, khối "vé vẫn hợp lệ" của `event-changed`): canvas dùng hai nửa tròn cắt hai bên và đường đứt. Email dùng viền trên đường đứt `2px dashed #B9BEC8` giữa hai phần; không dùng nửa tròn (cần `position: absolute`, nhiều ứng dụng thư bỏ qua).
- **Nút**: `<a>` cao 52 px, nền `#12141A`, chữ trắng 16 px đậm, bo 6 px, bọc trong `<td bgcolor="#12141A">` để Outlook vẫn vẽ nền. Mỗi nút có link văn bản bên dưới trong bản text và dòng "Nếu nút không bấm được, dán đường dẫn này vào trình duyệt" ở bản HTML của `magic-link`.
- **Tiếp cận**: `lang` đúng locale, `alt` rỗng cho mọi trang trí, tương phản ≥ 4,5:1 (chữ phụ `#5B6272` trên trắng đạt 5,9:1, DOC-39), cỡ chữ thân ≥ 14 px.
- **Preheader** (dòng xem trước trong hộp thư): `<div style="display:none;max-height:0;overflow:hidden">` đầu thân, key `email.<mẫu>.preheader`.

### 2.3 Định dạng giá trị trong email

Backend dựng bằng `java.time` và `java.text` với kết quả khớp DOC-40 §2 (kiểm bằng `EML-14`):

| Giá trị | `vi` | `en` | Cách dựng |
| --- | --- | --- | --- |
| Thời điểm sự kiện | `Thứ Bảy 14.11.2026 · 20:00` | `Saturday 14.11.2026 · 20:00` | `DayOfWeek.getDisplayName(FULL, locale)` + `dd.MM.yyyy · HH:mm` ở `event.timezone` (DR-137) |
| Hậu tố offset | `… · 20:00 GMT+9` | `… · 20:00 GMT+9` | Thêm `DateTimeFormatter.ofPattern("O", locale)` khi `event.timezone` khác `PLATFORM_TIMEZONE` (DR-12) |
| Giờ kết thúc | `22:30` | `22:30` | `HH:mm` |
| Tiền | `1.500.000 ₫` | `₫1,500,000` | `NumberFormat.getCurrencyInstance(locale)`, VND, 0 chữ số lẻ (DR-13). Khác canvas (`3.600.000đ`), cùng lý do với DOC-40 §2.1 |
| Số ghế | `9` hoặc `09` | như `vi` | Số thuần chữ số đệm 2 chữ số trên cuống vé (DR-33); nhãn khác (`A12`) in nguyên chuỗi |

### 2.4 Link

Mọi link tuyệt đối, dựng từ `APP_BASE_URL` (DR-54), không có tham số theo dõi. Chỉ có hai đích:

| Đích | URL | Dùng ở |
| --- | --- | --- |
| Đăng nhập | `${APP_BASE_URL}/auth/callback?token=<43 ký tự base64url>` | `magic-link`. `returnTo` không nằm trong link; nó ở `login_token.return_to` và trả về qua `POST /auth/verify` (DR-21) |
| Vé của tôi | `${APP_BASE_URL}${payload.ordersPath}` (`/me/tickets`) | `tickets`, `event-changed`, `refund-pending` không có nút này (xem mục 7) |

Link vé vào trang `/me/tickets`; người chưa đăng nhập được chuyển `/login?returnTo=/me/tickets` (DR-67).

## 3. Mẫu `magic-link` (canvas 03)

**Mục đích:** người dùng bấm một lần để có session. **Trigger:** FL-01 (DOC-83), sau commit của transaction chèn `login_token`.

| Mục | Nội dung |
| --- | --- |
| Biến | `{0}` `appName` (`APP_NAME`); `{1}` `linkUrl` (đầy đủ, mục 2.4); `{2}` `expiresInMinutes` (15). Không có email người nhận trong thân |
| Link | Một: nút "Đăng nhập" trỏ `linkUrl`, và `linkUrl` in dạng chữ dưới nút |
| Không có | Tên người dùng (có thể chưa có tài khoản), `returnTo`, địa chỉ IP |

Bố cục HTML (từ trên xuống): header `ticket.` → tiêu đề "Đăng nhập vào ticket" → một đoạn → nút → đường đứt → chú thích hết hạn.

Bản text:

```text
Đăng nhập vào ticket

Mở đường dẫn dưới đây để đăng nhập. Bạn sẽ quay lại đúng trang đang xem dở.

https://ticket.example.com/auth/callback?token=Zk3m…(43 ký tự)

Đường dẫn chỉ dùng được một lần và hết hạn sau 15 phút. Nếu bạn không yêu cầu đăng nhập, hãy bỏ qua email này.
```

Key (`messages_<locale>.properties`):

| Key | `vi` | `en` |
| --- | --- | --- |
| `email.magic-link.subject` | Đường dẫn đăng nhập của bạn | Your sign-in link |
| `email.magic-link.preheader` | Bấm để đăng nhập. Đường dẫn hết hạn sau {2} phút. | Click to sign in. The link expires in {2} minutes. |
| `email.magic-link.title` | Đăng nhập vào {0} | Sign in to {0} |
| `email.magic-link.body` | Bấm nút dưới đây để đăng nhập. Bạn sẽ quay lại đúng trang đang xem dở. | Click the button below to sign in. You'll go back to the page you were on. |
| `email.magic-link.cta` | Đăng nhập | Sign in |
| `email.magic-link.fallback` | Nếu nút không bấm được, dán đường dẫn này vào trình duyệt: | If the button doesn't work, paste this link into your browser: |
| `email.magic-link.note` | Đường dẫn chỉ dùng được một lần và hết hạn sau {2} phút. Nếu bạn không yêu cầu đăng nhập, hãy bỏ qua email này. | The link works once and expires after {2} minutes. If you didn't ask to sign in, ignore this email. |

`{2}` lấy từ hằng của `MagicLinkService` (15 phút, DR-21), không phải chuỗi cứng, để email và `login_token.expires_at` không lệch nhau. Tiêu đề "Đăng nhập vào {0}" dùng tên sản phẩm; canvas ghi "ticket" vì tên tạm (DR-54).

Hộp thư có bộ quét link (kiểm tra an toàn) mở URL trước người dùng: việc đó chỉ tải trang `/auth/callback`; token chỉ bị tiêu thụ khi trang gọi `POST /auth/verify` bằng JavaScript (DR-21, glossary "magic link"), nên bộ quét không làm link hết hiệu lực.

## 4. Mẫu `tickets` (canvas 07b)

**Mục đích:** giao vé và là bản lưu dự phòng khi người mua không vào được web. **Trigger:** `EMAIL_TICKETS`, ghi trong transaction xác nhận (DOC-26), kể cả đơn 0 đồng (FL-14).

| Biến (payload DOC-15 §7.2) | Dùng ở |
| --- | --- |
| `event.name`, `event.startsAt`, `event.timezone`, `event.venue` | Khối sự kiện |
| `tickets[]` = `{code, ticketTypeName, unitPrice, label}` | Mỗi vé một khối |
| `tickets.size()` | "2 vé" |
| `amount`, `currency` | Dòng tổng (đơn 0 đồng thì dòng "Miễn phí") |
| `ordersPath` | Nút "Xem vé của tôi" |

Hình dạng khối vé theo `label` (DR-110):

| `label` có khóa | Cột trên cuống | Ví dụ |
| --- | --- | --- |
| `section`, `row`, `seat` (ghế) | Hàng · Ghế · Loại vé; `section` in thành dòng nhỏ phía trên khi có | `Hàng C · Ghế 09 · VIP` |
| `zone` (khu vực) | Khu vực · Loại vé | `Khu vực Fanzone · Thường` |
| không khóa nào (GA) | Loại vé | `Vé thường` |

Mã vé `GM-4K7P-92XD` luôn ở cột phải, chữ mono (DR-52). Số vé lớn: tối đa **50 khối vé** trong một email (giới hạn 50 unit mỗi lệnh giữ vé, FR-06); không phân trang.

Dòng tổng: `{n} vé {ticketTypeSummary} · trả bằng thẻ` + tiền. `ticketTypeSummary` là tên loại vé nếu cả đơn một loại (`VIP`), ngược lại bỏ qua. Đơn miễn phí: "2 vé · miễn phí".

Subject: `Vé của bạn: {event.name}`. Preheader: `{n} vé · {ngày giờ}`.

Bản text (mỗi vé một khối, dễ sao chép):

```text
Vé của bạn đã sẵn sàng

Cảm ơn bạn đã mua vé. Dưới đây là 2 vé cho sự kiện:
Hòa nhạc Giao Mùa
Thứ Bảy 14.11.2026 · 20:00 · Nhà hát Thành phố

Hàng C · Ghế 09 · VIP
Mã vé: GM-4K7P-92XD

Hàng C · Ghế 10 · VIP
Mã vé: GM-8H3N-Q0TR

2 vé VIP · trả bằng thẻ: 1.500.000 ₫

Xem vé của tôi: https://ticket.example.com/me/tickets

Mỗi vé có một mã riêng. Hãy giữ lại email này; bạn cũng có thể xem vé bất cứ lúc nào ở trang Vé của tôi.
```

| Key | `vi` | `en` |
| --- | --- | --- |
| `email.tickets.subject` | Vé của bạn: {0} | Your tickets: {0} |
| `email.tickets.preheader` | {0} vé · {1} | {0, plural, one {# ticket} other {# tickets}} · {1} |
| `email.tickets.title` | Vé của bạn đã sẵn sàng | Your tickets are ready |
| `email.tickets.intro` | Cảm ơn bạn đã mua vé. Dưới đây là {0} vé cho sự kiện: | Thanks for your purchase. Here are your {0} tickets for: |
| `email.tickets.col.section` | Khu | Section |
| `email.tickets.col.row` | Hàng | Row |
| `email.tickets.col.seat` | Ghế | Seat |
| `email.tickets.col.zone` | Khu vực | Zone |
| `email.tickets.col.type` | Loại vé | Ticket type |
| `email.tickets.col.code` | Mã vé | Ticket code |
| `email.tickets.total.paid` | {0} vé {1} · trả bằng thẻ | {0} tickets {1} · paid by card |
| `email.tickets.total.free` | {0} vé {1} · miễn phí | {0} tickets {1} · free |
| `email.tickets.cta` | Xem vé của tôi | View my tickets |
| `email.tickets.note` | Mỗi vé có một mã riêng. Hãy giữ lại email này; bạn cũng có thể xem vé bất cứ lúc nào ở trang Vé của tôi. | Each ticket has its own code. Keep this email; you can also see your tickets any time on My tickets. |

`en` của `intro` dùng số nhiều cố định vì đơn một vé dùng `email.tickets.intro.one` ("Here is your ticket for:"). Hai key `.intro.one` và `.preheader.one` có trong file, không liệt kê lại; `i18n:check` bắt thiếu (DOC-31).

## 5. Mẫu `event-changed` (canvas 07c, ba biến thể)

**Mục đích:** nói rõ cái gì đổi, cái gì không, và vé vẫn hợp lệ. **Trigger:** DR-29, FL-11; một email cho mỗi đơn `PAID`, mỗi lần lưu là một đợt (không gom).

Chọn biến thể từ `payload.changed` (DOC-15 §7.2; DR-111):

| `changed` chứa | Biến thể | Hàng "đổi" | Hàng "không đổi" |
| --- | --- | --- | --- |
| `startsAt` và/hoặc `endsAt`, không có `venue` | Đổi giờ | Thời gian | Địa điểm |
| chỉ `venue` | Đổi địa điểm | Địa điểm | Thời gian |
| cả hai nhóm | Đổi cả hai | Thời gian, Địa điểm | không có |

Hàng "Thời gian" hiện `startsAt` mới và cũ khi `startsAt` đổi. Nếu **chỉ `endsAt` đổi**, hàng đó là "Giờ kết thúc" với `HH:mm` mới và cũ (canvas không có ca này; DR-29 nói `ends_at` cũng kích hoạt email).

| Biến | Nguồn |
| --- | --- |
| `event.name`, `event.timezone` | `payload.event` |
| `before.*`, `after.*`, `changed` | `payload` |
| `tickets[]` = `{code, ticketTypeName, label}` | Danh sách "vé vẫn hợp lệ", mỗi dòng `C-09 · VIP · GM-4K7P-92XD` |
| `organizerContactEmail` | Dòng cuối: liên hệ người tổ chức (DR-23: `organizer.contact_email`, trống thì email đăng nhập của chủ tổ chức, đã được giải quyết lúc ghi dòng) |
| `after.venue` | Câu cuối ghi nơi chốn mới kèm email liên hệ như canvas ("Nhà hát Cầu Mây, lienhe@…"); payload không có tên tổ chức nên không in tên đó |

Subject theo biến thể: `Đổi giờ: {event.name}`, `Đổi địa điểm: {event.name}`, `Đổi giờ và địa điểm: {event.name}`. Tiêu đề h1: `Sự kiện đổi giờ`, `Sự kiện đổi địa điểm`, `Đổi giờ và địa điểm`. Tem "Có thay đổi" (viền `#D63312`, xoay −2°) không dùng `transform` trong email: chỉ viền và chữ, không xoay.

Bản text, biến thể "Đổi cả hai":

```text
Đổi giờ và địa điểm: Hòa nhạc Giao Mùa

Người tổ chức vừa cập nhật sự kiện bạn đã mua vé: Hòa nhạc Giao Mùa.

THỜI GIAN
Mới: Chủ Nhật 15.11.2026 · 19:30
Trước đây: Thứ Bảy 14.11.2026 · 20:00

ĐỊA ĐIỂM
Mới: Nhà hát Cầu Mây
Trước đây: Nhà hát Bến Sông

Vé của bạn vẫn hợp lệ. Bạn không cần làm gì thêm. Chỗ ngồi và mã vé giữ nguyên:
C-09 · VIP · GM-4K7P-92XD
C-10 · VIP · GM-8T2M-51QA

Xem vé của tôi: https://ticket.example.com/me/tickets

Nếu lịch mới không phù hợp, hãy liên hệ người tổ chức: Nhà hát Cầu Mây, lienhe@example.com.
```

| Key | `vi` | `en` |
| --- | --- | --- |
| `email.event-changed.subject.time` | Đổi giờ: {0} | Time changed: {0} |
| `email.event-changed.subject.venue` | Đổi địa điểm: {0} | Venue changed: {0} |
| `email.event-changed.subject.both` | Đổi giờ và địa điểm: {0} | Time and venue changed: {0} |
| `email.event-changed.preheader` | Vé của bạn vẫn hợp lệ. Xem điều gì đã đổi. | Your tickets are still valid. See what changed. |
| `email.event-changed.badge` | Có thay đổi | Changed |
| `email.event-changed.title.time` | Sự kiện đổi giờ | The event time has changed |
| `email.event-changed.title.venue` | Sự kiện đổi địa điểm | The event venue has changed |
| `email.event-changed.title.both` | Đổi giờ và địa điểm | Time and venue have changed |
| `email.event-changed.intro` | Người tổ chức vừa cập nhật sự kiện bạn đã mua vé: {0}. | The organizer has updated an event you bought tickets for: {0}. |
| `email.event-changed.row.time` | Thời gian | Time |
| `email.event-changed.row.end` | Giờ kết thúc | End time |
| `email.event-changed.row.venue` | Địa điểm | Venue |
| `email.event-changed.new` | Mới | New |
| `email.event-changed.before` | Trước đây | Before |
| `email.event-changed.unchanged` | không đổi | unchanged |
| `email.event-changed.valid.title` | Vé của bạn vẫn hợp lệ | Your tickets are still valid |
| `email.event-changed.valid.body` | Bạn không cần làm gì thêm. Chỗ ngồi và mã vé giữ nguyên: | You don't need to do anything. Your seats and ticket codes stay the same: |
| `email.event-changed.cta` | Xem vé của tôi | View my tickets |
| `email.event-changed.contact` | Nếu lịch mới không phù hợp, hãy liên hệ người tổ chức: {0}, {1}. | If the new plan doesn't work for you, contact the organizer: {0}, {1}. |

`{0}` là `after.venue`, `{1}` là `organizerContactEmail` trong key `contact`. Chữ trên tem in hoa bằng CSS `text-transform`; ứng dụng nào bỏ qua thì chữ vẫn đọc được.

## 6. Mẫu `refund-pending` (mới, không có trong canvas)

**Mục đích:** nói người mua đã trả tiền nhưng không có vé, vì sao, và khi nào nhận lại tiền. **Trigger:** đơn sang `REFUND_PENDING` (DR-44, DR-28 bước 6). Một mẫu, đoạn lý do chọn theo `payload.refundReason` (DR-44). Bố cục theo `tickets` (header, tiêu đề, khối thân, đường đứt), **không có nút**: người mua không có việc gì phải làm, và trang Vé của tôi chưa có thứ gì mới để xem ngoài trạng thái đơn.

| Biến | Nguồn |
| --- | --- |
| `refundReason` | `payload`: `LATE_PAYMENT`, `AMOUNT_MISMATCH`, `EVENT_CANCELLED` |
| `event.name`, `event.startsAt`, `event.timezone` | `payload.event` |
| `amount`, `currency` | `payload` (tiền người mua đã trả) |
| `{sla}` | cấu hình `support.refund-sla-text` theo locale, đọc **lúc dựng email**, không có trong payload (DOC-15 §7.2) |
| `{email}` | cấu hình `support.email`, như trên |

| `refundReason` | Tiêu đề h1 | Đoạn lý do |
| --- | --- | --- |
| `LATE_PAYMENT` | Chúng tôi sẽ hoàn tiền cho bạn | Khoản thanh toán của bạn đến sau khi thời gian giữ vé đã kết thúc, nên vé đã được trả lại và không được phát hành. |
| `AMOUNT_MISMATCH` | Chúng tôi sẽ hoàn tiền cho bạn | Số tiền chúng tôi nhận được không khớp với giá trị đơn hàng nên vé không được phát hành. |
| `EVENT_CANCELLED` | Sự kiện đã bị hủy | Người tổ chức đã hủy sự kiện, nên vé của bạn không còn hiệu lực. |

Tiêu đề và subject ở hai lý do đầu giống nhau có chủ ý: cả hai là lỗi của thanh toán, người mua chỉ cần biết "tiền sẽ về". Dòng chung bên dưới đoạn lý do: số tiền hoàn là **toàn bộ** `amount`, "Bạn không cần làm gì thêm", thời gian và liên hệ hỗ trợ.

Bản text, lý do `LATE_PAYMENT`:

```text
Chúng tôi sẽ hoàn tiền cho bạn

Khoản thanh toán của bạn đến sau khi thời gian giữ vé đã kết thúc, nên vé đã được trả lại và không được phát hành.

Sự kiện: Giao Mua Concert · Thứ Bảy 14.11.2026 · 20:00
Số tiền hoàn: 750.000 ₫ (toàn bộ số tiền bạn đã trả)

Bạn không cần làm gì thêm. Thời gian hoàn tiền: 5–10 ngày làm việc. Cần hỗ trợ: support@example.com.
```

| Key | `vi` | `en` |
| --- | --- | --- |
| `email.refund-pending.subject.refund` | Chúng tôi sẽ hoàn tiền cho đơn của bạn | We'll refund your order |
| `email.refund-pending.subject.cancelled` | Sự kiện đã bị hủy: {0} | Event cancelled: {0} |
| `email.refund-pending.preheader` | Bạn không cần làm gì thêm. Tiền sẽ được hoàn lại toàn bộ. | You don't need to do anything. You'll get a full refund. |
| `email.refund-pending.title.refund` | Chúng tôi sẽ hoàn tiền cho bạn | We'll refund you |
| `email.refund-pending.title.cancelled` | Sự kiện đã bị hủy | The event has been cancelled |
| `email.refund-pending.reason.LATE_PAYMENT` | Khoản thanh toán của bạn đến sau khi thời gian giữ vé đã kết thúc, nên vé đã được trả lại và không được phát hành. | Your payment arrived after the ticket hold had ended, so the tickets were released and not issued. |
| `email.refund-pending.reason.AMOUNT_MISMATCH` | Số tiền chúng tôi nhận được không khớp với giá trị đơn hàng nên vé không được phát hành. | The amount we received didn't match the order total, so the tickets were not issued. |
| `email.refund-pending.reason.EVENT_CANCELLED` | Người tổ chức đã hủy sự kiện, nên vé của bạn không còn hiệu lực. | The organizer cancelled the event, so your tickets are no longer valid. |
| `email.refund-pending.event` | Sự kiện: {0} · {1} | Event: {0} · {1} |
| `email.refund-pending.amount` | Số tiền hoàn: {0} (toàn bộ số tiền bạn đã trả) | Refund amount: {0} (everything you paid) |
| `email.refund-pending.note` | Bạn không cần làm gì thêm. Thời gian hoàn tiền: {0}. Cần hỗ trợ: {1}. | You don't need to do anything. Refund time: {0}. Need help: {1}. |

Khóa `reason.<refundReason>` dùng đúng giá trị enum (chữ hoa) vì tra trực tiếp từ payload; ngoại lệ có chủ ý của quy ước key chữ thường ở DOC-31 §4 (chỉ cho khóa động từ enum DB). `refundReason` không thuộc ba giá trị → ném lỗi, dòng outbox `FAILED` sau 12 lần, log ERROR (DR-112).

`{0}` của `note` là `support.refund-sla-text`, `{1}` là `support.email`; chuỗi giống `orders.refund.note` của DOC-40 §3.7 để email và màn Kết quả nói cùng một câu (key riêng vì backend dùng `messages_*.properties`, DOC-31 §5).

## 7. Điều email cố ý không làm

| Không làm | Lý do |
| --- | --- |
| Nút "Xem vé của tôi" ở `refund-pending` | Không có vé; đơn chưa có hành động nào của người mua |
| Email xác nhận giữ vé, nhắc hết hạn giữ vé, nhắc trước giờ diễn | Ngoài phạm vi (SDD gốc 9.4, DOC-01) |
| Email cho chính người tổ chức khi có đơn hoặc khi hủy | Ngoài phạm vi; Studio có số liệu (FR-20) |
| Email "đơn bị hủy" khi giữ vé hết hạn | Người mua chưa trả tiền; thấy trạng thái ở màn Thanh toán |
| Mã QR, tệp đính kèm | SDD gốc 9.4 |

## 8. Danh mục biến (tham số) theo mẫu

| Mẫu | Biến Thymeleaf | Kiểu | Nguồn |
| --- | --- | --- | --- |
| `magic-link` | `appName`, `linkUrl`, `expiresInMinutes` | string, string, int | `APP_NAME`, `MagicLinkService`, hằng 15 |
| `tickets` | `event{name,venue,startsAtText,timezoneSuffix}`, `tickets[]{code,typeName,label}`, `count`, `amountText`, `isFree`, `ordersUrl`, `singleType` | định dạng sẵn | payload `EMAIL_TICKETS` |
| `event-changed` | `variant` (`time`/`venue`/`both`), `rows[]{kind,now,before}`, `sameRows[]{kind,value}`, `tickets[]`, `contactEmail`, `ordersUrl`, `event.name` | định dạng sẵn | payload `EMAIL_EVENT_CHANGED` |
| `refund-pending` | `reason`, `event{name,startsAtText}`, `amountText`, `sla`, `supportEmail` | định dạng sẵn | payload `EMAIL_REFUND_PENDING` + `support.*` |

`MailModelFactory` (lớp trong module `notification`) đổi payload JSON thành các biến này; Thymeleaf không định dạng gì (DOC-31 §5).

## 9. Quyết định mới khi viết tài liệu này

| ID tạm | Tóm tắt | Trạng thái |
| --- | --- | --- |
| DR-109 | Bố cục bảng + CSS inline, phông có dự phòng, không web font, không nửa tròn cuống vé; `Message-ID` của `magic-link`; không `Reply-To` | Chốt (Claude, Owner ủy quyền) |
| DR-110 | Hình dạng khối vé theo khóa của `ticket.label` (ghế / khu vực / GA) | Chốt (Claude, Owner ủy quyền) |
| DR-111 | Chọn biến thể `event-changed` từ `changed`; chỉ `endsAt` đổi → hàng "Giờ kết thúc" | Chốt (Claude, Owner ủy quyền) |
| DR-112 | `refundReason` lạ → lỗi và `FAILED`, không gửi email chung chung | Chốt (Claude, Owner ủy quyền) |

Mọi quyết định mới của tài liệu này là DR-109…115 (cùng dãy với DOC-83) trong sổ quyết định. Nội dung đầy đủ:

**DR-109 · Bố cục và header kỹ thuật của email.** *Vấn đề:* canvas vẽ email bằng `div` flex, phông web và nửa tròn cuống vé định vị tuyệt đối; nhiều ứng dụng thư (Outlook, Gmail cũ) bỏ qua cả ba. DR-54 chưa nói `Message-ID` của email không qua outbox, và `Reply-To`. *Quyết định:* bố cục `<table role="presentation">` tối đa 600 px với CSS inline; chuỗi phông dự phòng, không nhúng web font; cuống vé dùng đường đứt thay nửa tròn; `Message-ID` của `magic-link` là `<login-{12 hex đầu của token_hash}@APP_DOMAIN>`; không đặt `Reply-To`. *Hệ quả:* hình dạng trong hộp thư khác canvas một chút (không nửa tròn, phông hệ thống); không email nào cho phép trả lời. Đổi bằng cách sửa template.

**DR-110 · Khối vé theo `ticket.label`.** *Vấn đề:* `label` có ba dạng (ghế, khu vực, GA) mà canvas chỉ vẽ ghế. *Quyết định:* khóa `section`/`row`/`seat` → cột Hàng, Ghế, Loại vé; khóa `zone` → Khu vực, Loại vé; không khóa → Loại vé; mã vé luôn ở cột phải. *Hệ quả:* DOC-27 và DOC-85 phải giữ `label` của GA không chứa khóa nào ở trên (câu hỏi mở).

**DR-111 · Chọn biến thể `event-changed`.** *Vấn đề:* DR-29 cho `endsAt` kích hoạt email nhưng canvas chỉ có ba biến thể theo giờ bắt đầu và địa điểm. *Quyết định:* `startsAt` hoặc `endsAt` đổi → nhóm "giờ"; `venue` đổi → nhóm "địa điểm"; chỉ `endsAt` đổi → hàng "Giờ kết thúc" thay cho hàng "Thời gian". *Hệ quả:* thêm một ca nhỏ vào test.

**DR-112 · `refundReason` ngoài ba giá trị.** *Vấn đề:* mẫu `refund-pending` chọn đoạn lý do từ enum; giá trị lạ không có đoạn nào. *Quyết định:* ném lỗi khi dựng; dòng outbox thử lại theo DR-53 rồi `FAILED`, log ERROR; không gửi email chung chung. *Hệ quả:* lỗi lập trình hiện ra ở outbox thay vì gửi email sai nghĩa về tiền.

## 10. Test bắt buộc

Tiền tố `EML-` (đăng ký ở DOC-69). Test dựng email chạy ở mức tích hợp với `GreenMail` hoặc Mailpit (DOC-69); `EML-14` là test đơn vị.

| ID | Kịch bản | Kỳ vọng |
| --- | --- | --- |
| EML-01 | Dựng `magic-link` locale `vi` với token 43 ký tự | Subject "Đường dẫn đăng nhập của bạn"; HTML và text đều chứa đúng một URL `…/auth/callback?token=<token>`; không có email người nhận trong thân |
| EML-02 | Cùng `magic-link` locale `en` | Subject "Your sign-in link"; không còn ký tự tiếng Việt |
| EML-03 | `tickets`, 2 vé ghế C-9, C-10 hạng VIP, đơn 1.500.000 | Hai khối vé; "Hàng C · Ghế 09"; mã `GM-4K7P-92XD`, `GM-8H3N-Q0TR`; tổng `1.500.000 ₫` |
| EML-04 | `tickets`, đơn 0 đồng, 1 vé GA | Dòng "miễn phí"; khối vé chỉ có Loại vé và Mã vé; intro dạng một vé |
| EML-05 | `tickets`, `label` có `zone` | Cột "Khu vực" thay cho Hàng/Ghế |
| EML-06 | `tickets` 50 vé | 50 khối, kích thước HTML < 100 KB (Gmail cắt thư lớn hơn 102 KB) |
| EML-07 | `event-changed`, `changed=["startsAt","endsAt"]` | Biến thể "Đổi giờ"; hàng Địa điểm ghi "không đổi" |
| EML-08 | `event-changed`, `changed=["venue"]` | Biến thể "Đổi địa điểm"; hàng Thời gian "không đổi" |
| EML-09 | `event-changed`, cả hai | Hai hàng đổi, không có hàng "không đổi" |
| EML-10 | `event-changed`, `changed=["endsAt"]` | Hàng "Giờ kết thúc" với `HH:mm` mới và cũ |
| EML-11 | `refund-pending`, ba giá trị `refundReason` | Mỗi giá trị đúng một đoạn lý do; `EVENT_CANCELLED` có h1 "Sự kiện đã bị hủy" |
| EML-12 | `refund-pending`, `support.refund-sla-text` `vi`/`en` đặt khác nhau | `{0}` của `note` theo locale của người nhận |
| EML-13 | `refund-pending`, `refundReason="X"` | Ném lỗi; dòng outbox thử lại rồi `FAILED`; không gửi |
| EML-14 | Định dạng ngày/tiền cùng giá trị ở hai locale; event `Asia/Tokyo` | `Thứ Bảy 14.11.2026 · 20:00 GMT+9` / `Saturday 14.11.2026 · 20:00 GMT+9`; tiền `1.500.000 ₫` / `₫1,500,000` |
| EML-15 | Thiếu một key `email.*` trong `messages_en` | `MessagesParityTest` thất bại (DOC-31) |
| EML-16 | Mọi bản HTML | Có `lang`, mọi `<a>` có chữ, không `<link>` ngoài, không `<script>`, mọi URL bắt đầu bằng `APP_BASE_URL` |

## Câu hỏi còn mở

Không còn. Nhãn vé GA là `{}` theo DR-108, đúng giả định ở mục 4; bản dịch `en` đã được Owner duyệt cùng tài liệu.
