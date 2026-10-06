# Trạng thái giao diện và microcopy

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-40
> Phụ thuộc: SDD gốc §12.3, §13, [Sổ quyết định](../00-decision-register.md) (DR-10, 12, 13, 24, 25, 26, 28, 41, 44, 45, 52, 57, 58, 59, 63, 64, 66, 70), [DOC-06](../02-glossary.md), [DOC-31](../06-design/i18n.md) (quy trình thêm chuỗi), [DOC-35](../06-design/error-handling.md) (bảng exception → mã lỗi), [DOC-38](ux-principles-and-ia.md), [DOC-39](design-system.md)
> Người dùng chính: P1-01 (khung frontend, i18n), mọi task màn hình P1–P6, DOC-41…60 (màn hình dẫn key ở đây), DOC-51 (email dùng cùng phong cách), người dịch

Tài liệu là nguồn duy nhất của **mẫu trạng thái** (đang tải, rỗng, lỗi, quá tải, không quyền), **định dạng** số, tiền, ngày, **nhãn trạng thái**, **ánh xạ mã lỗi → key thông báo** và **toàn bộ chuỗi giao diện** dạng bảng `key · vi · en`. Chuỗi `vi` lấy từ canvas thiết kế (artboard ghi ở cột "Nguồn"); chuỗi `en` do tài liệu này dịch (DR-10). Màn hình spec (DOC-42…60) dẫn **key**, không chép chuỗi. Email nằm ở DOC-51 (`08-ux-ui/screens/emails.md`); tài liệu này chỉ nêu cách đặt key. Quy trình thêm chuỗi và script `i18n:check` ở [DOC-31](../06-design/i18n.md).

Quyết định mới khi viết tài liệu này ghi DR-131…137 (số liên tục với DOC-38, DOC-39), tóm tắt ở mục 7.

## 0. Quy ước

- **Key** viết tiếng Anh theo nghĩa, dạng `<namespace>.<khu vực>.<ý>.<phần>` (DR-10): `checkout.hold.expired.title`. Phần cuối thường là `title`, `body`, `cta`, `label`, `hint`, `error`, `aria`.
- **Namespace** = file `frontend/src/locales/<lng>/<ns>.json`. DR-10 chốt `events`, `checkout`, `studio`, `editor`, `common`; tài liệu này thêm `auth`, `queue`, `orders`, `errors`, `validation` (DR-135; mục 7). Phía backend dùng `messages_vi.properties`, `messages_en.properties` với tiền tố `email.` và `problem.` (mục 4.3).
- **Placeholder** dạng ICU của `i18next-icu`: `{name}`, số nhiều `{count, plural, one {# vé} other {# vé}}`. `vi` chỉ có dạng `other`; `en` có `one` và `other`.
- **Chữ hoa**: tiêu đề hiển thị chữ hoa do CSS (`text-transform`, DOC-39 §3); chuỗi viết như câu thường (chữ cái đầu hoa).
- **Dấu và ký tự**: dấu chấm lửng là `…` (U+2026), dấu trừ trong bộ đếm là `−` (U+2212), dấu nhân `×`, dấu giữa `·` có khoảng trắng hai bên.
- **Giọng**: ngôi "bạn", câu ngắn, chủ động, nói điều đã xảy ra rồi việc làm tiếp (UXP-06). Không từ "vui lòng", không "xin lỗi" thừa.
- **Giá trị động từ server**: `title`/`detail` của Problem Details (DR-63) chỉ là **dự phòng** cho mã chưa có key (mục 4.1); giao diện ưu tiên key của tài liệu này.
- Dữ liệu người tổ chức nhập (tên sự kiện, địa điểm, loại vé, khu vực, mô tả) **không dịch** và không đi qua key (DR-10).

## 1. Mẫu trạng thái

Mỗi vùng dữ liệu của một màn hình có đủ năm trạng thái dưới đây (canvas `ComponentsStudio` mục P và mỗi màn có biến thể "Đang tải", "Lỗi tải", "Chưa có …"). Cấu trúc thành phần ở DOC-39 §7.17.

### 1.1 Đang tải

| Mẫu | Quy tắc |
| --- | --- |
| Khung chờ (`Skeleton`) | Giữ đúng hình và chiều cao của nội dung sắp hiện (UXP-07). Kèm một dòng chữ ẩn cho trình đọc màn hình (`role="status"`, `aria-live="polite"`): key `<ns>.<vùng>.loading` ("Đang tải sự kiện…") |
| Thời điểm hiện | Chỉ hiện nếu chờ > 150 ms (tránh nháy); tối thiểu 300 ms khi đã hiện |
| Quá 10 giây | Hiện thêm dòng `common.loading.slow` ("Đang mất nhiều thời gian hơn bình thường…") và nút `common.retry` |
| Nút đang xử lý | Giữ chiều rộng; đổi nhãn sang dạng tiến hành ("Đang giữ vé…"); `disabled` + `aria-busy` |

### 1.2 Rỗng

Luôn gồm: tiêu đề nói điều đang thiếu + một câu + **một nút việc kế tiếp** (UXP-07). Ví dụ canvas: "Chưa có sự kiện nào" / "Tạo sự kiện đầu tiên để bắt đầu bán vé." / nút "Tạo sự kiện". Danh sách key ở mục 3.

### 1.3 Lỗi tải

Nhãn mono "Lỗi tải" + tiêu đề nói **cái gì** không tải được + một câu nói **hệ quả với việc đang làm** + nút "Thử lại" (`common.retry`). Câu hệ quả khác nhau theo bối cảnh: ở Thanh toán nói "Vé vẫn đang được giữ cho bạn và đồng hồ vẫn chạy"; ở Studio nói "Việc bán vé không bị ảnh hưởng"; ở Vé của tôi nói "Vé của bạn vẫn an toàn và vẫn có trong email". Không bao giờ để người dùng tưởng dữ liệu đã mất.

### 1.4 Quá tải (503 `OVERLOADED`, 429 `RATE_LIMITED`)

Thông báo `warning` (DOC-39 §7.9): tiêu đề "Đang rất đông" + "Hệ thống tự thử lại sau {seconds} giây. Bạn không cần bấm lại hay tải lại trang." Quy tắc chờ ở DOC-38 §8.2. Trang toàn trang là E4 (mục 3.9). Người mua đang giữ vé được nói rõ vé không bị ảnh hưởng.

### 1.5 Không có quyền, không tìm thấy, hết phiên

| Tình huống | Hiển thị | Căn cứ |
| --- | --- | --- |
| Tài nguyên không tồn tại hoặc không thuộc người xem | E1 (404) "Trang này không tồn tại"; không tiết lộ tồn tại hay không | DR-23, DR-64 `NOT_FOUND` |
| Chưa đăng nhập hoặc hết phiên | Màn Đăng nhập biến thể "Hết phiên đăng nhập" (E2), trang đang xem dở được mở lại sau đăng nhập | 401 `UNAUTHENTICATED` |
| Đã đăng nhập nhưng không phải người tổ chức vào `/studio/**` | E3 (403) "Bạn không quản lý sự kiện này" | 403 `FORBIDDEN` |
| Chưa có hồ sơ tổ chức | Chuyển tới `/studio/profile` | 403 `ORGANIZER_PROFILE_REQUIRED` |
| Lỗi máy chủ | E5 (500) với mã yêu cầu (`requestId`) chọn được (`user-select: all`) | 500 `INTERNAL_ERROR` |

## 2. Định dạng và nhãn

### 2.1 Số và tiền (DR-10, DR-13)

Dùng `Intl.NumberFormat(locale, …)`; không ghép chuỗi tay. Tiền là **số nguyên đồng VND**, không phần lẻ, không cấu hình tiền tệ.

| Giá trị (`amount`) | `vi` | `en` | Cách dựng |
| --- | --- | --- | --- |
| `1800000` | `1.800.000 ₫` | `₫1,800,000` | `new Intl.NumberFormat(l, { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })` |
| `0` | `Miễn phí` (vé), `0 ₫` (tổng đơn) | `Free` (vé), `₫0` (tổng đơn) | key `common.price.free` cho giá vé; tổng đơn dùng định dạng tiền |
| `249` (số vé) | `249` | `249` | `Intl.NumberFormat(l)` |
| `0.52` (tỉ lệ) | `52%` | `52%` | `style: 'percent', maximumFractionDigits: 0` |

Canvas hiển thị `1.800.000đ` (không có ký hiệu ₫ chuẩn); `Intl` ở `vi` ra `1.800.000 ₫` (có dấu cách không ngắt U+00A0). Đây là khác biệt có chủ ý (DOC-38 §9). Số và tiền trong thẻ vé, đồng hồ, mã luôn mono (`font-variant-numeric: tabular-nums`).

### 2.2 Ngày giờ (DR-12)

Mọi mốc nhận từ API là chuỗi UTC (`2026-11-14T13:00:00Z`). Hiển thị bằng `Intl.DateTimeFormat(locale, { timeZone: event.timezone, … })`, **không** theo múi giờ máy xem.

| Vị trí | Mẫu | `vi` | `en` | Tùy chọn `Intl` |
| --- | --- | --- | --- | --- |
| Ngày giờ đầy đủ (thẻ, trang sự kiện, vé) | thứ · ngày · giờ | `Thứ Bảy 14.11.2026 · 20:00` | `Saturday 14.11.2026 · 20:00` | `weekday: 'long'`, `day`, `month`, `year`, `hour`, `minute`, `hour12: false` |
| Khung giờ | giờ đến giờ | `20:00 đến 22:30` | `20:00 to 22:30` | `hour`, `minute` |
| Mốc mở/đóng bán | giờ · ngày | `10:00 · 10.10.2026` | `10:00 · 10.10.2026` | như trên |
| Đếm ngược "Mở bán sau" | `{d} ngày hh:mm:ss` | `4 ngày 22:10:05` | `4 days 22:10:05` | tự tính từ `saleStartsAt − (now + offset)` (DR-66) |
| Tem ngày (không ảnh) | thứ / `dd.MM` / năm · giờ | `Thứ Bảy` / `14.11` / `2026 · 20:00` | `Sat` / `14.11` / `2026 · 20:00` | `weekday: 'short'` |
| Cập nhật lúc (Studio 07) | `HH:mm:ss` | `Cập nhật lúc 10:42:07` | `Updated at 10:42:07` | múi giờ của sự kiện |

Ghi chú: để định dạng **giống nhau ở hai locale** (dạng `dd.MM.yyyy` như canvas, DR-12), ghép bằng `formatToParts`: ngày `dd.MM.yyyy` + giờ `HH:mm`; chỉ **tên thứ** lấy từ `weekday: 'long'` theo locale (`en`: "Saturday"; `vi`: "Thứ Bảy"). Dạng `en` mặc định của `Intl` (`Saturday, 14 November 2026 at 20:00`) không dùng (DR-137).

**Hậu tố offset**: khi `event.timezone` khác `PLATFORM_TIMEZONE`, thêm `timeZoneName: 'shortOffset'` → `Thứ Bảy 14.11.2026 · 20:00 GMT+9`; ở cả giao diện và email (DR-12). Khi trùng múi giờ mặc định không thêm.

Form studio hiển thị **giờ địa phương của sự kiện** và gửi kèm `timezone`; server đổi sang UTC (DR-12). Bộ chọn múi giờ ở Studio 02 là combobox tìm được lấy từ `Intl.supportedValuesOf('timeZone')`, mặc định `PLATFORM_TIMEZONE`.

### 2.3 Nhãn trạng thái

**Trạng thái hiển thị của sự kiện** (`displayStatus`, DR-24); key `common.eventStatus.<displayStatus>`:

| `displayStatus` | `vi` | `en` | Ngữ cảnh khác |
| --- | --- | --- | --- |
| `DRAFT` | Bản nháp | Draft | |
| `UPCOMING` | Sắp mở bán | On sale soon | |
| `ON_SALE` | Đang mở bán | On sale | |
| `SOLD_OUT` | Hết vé | Sold out | |
| `SALE_CLOSED` | Đã đóng bán | Sales closed | |
| `PAUSED` | Tạm dừng | Paused | Trang sự kiện: `events.detail.paused.title` = "Tạm dừng bán" |
| `ENDED` | Đã kết thúc | Ended | |
| `CANCELLED` | Đã hủy | Cancelled | |
| (`PUBLISHED`, Studio 06) | Đã xuất bản | Published | key `studio.publish.status.published` |

Trang Sự kiện (DOC-43) cho `CANCELLED` dùng hai chuỗi tùy có đơn đã thanh toán hay không (DR-28): `events.status.cancelled.noRefund` ("Người tổ chức đã hủy sự kiện này. Chưa có vé nào được bán nên không ai bị trừ tiền.") và `events.status.cancelled.refund` ("Người tổ chức đã hủy sự kiện này. Nếu bạn đã mua vé, bạn sẽ được hoàn lại toàn bộ; xem Vé của tôi.").

**Trạng thái chỗ** (`common.seat.<state>`): `available` "Còn trống" / "Available"; `selected` "Đang chọn" / "Selected"; `held` "Có người giữ" / "Held by someone"; `sold` "Đã bán" / "Sold".

**Trạng thái đơn và vé** (Kết quả, Vé của tôi; `common.orderStatus.<status>`):

| `orders.status` | `vi` | `en` |
| --- | --- | --- |
| `PENDING_PAYMENT` | Chờ thanh toán | Awaiting payment |
| `PAID` | Đã thanh toán | Paid |
| `EXPIRED` | Hết hạn giữ | Hold expired |
| `CANCELLED` | Đã hủy | Cancelled |
| `REFUND_PENDING` | Chờ hoàn tiền | Refund pending |
| `REFUNDED` | Đã hoàn tiền | Refunded |

`ticket.status`: `ISSUED` hiện "Còn hiệu lực" / "Valid"; `VOID` hiện "Đã vô hiệu" / "Void" (khi sự kiện bị hủy, DR-28).

**Trạng thái hàng đợi** (`queue.status.<state>`, DR-57): `PRE_QUEUE` "Chờ trước giờ mở bán"; `WAITING` "Đang xếp hàng"; `ADMITTED` "Tới lượt"; `PAUSED` "Tạm hết vé"; `SOLD_OUT` "Đã hết vé"; `NOT_IN_QUEUE` (không hiện, chuyển trang); `DISCONNECTED` (chỉ client) "Mất kết nối".

## 3. Microcopy

Cột **Nguồn** là artboard canvas (viết tắt tên file). Chuỗi có `{…}` là placeholder. Chuỗi nào không có ở canvas mà tài liệu thêm cho khớp DR được đánh dấu `†` ở cột Nguồn.

### 3.1 `common`

| Key | `vi` | `en` | Nguồn |
| --- | --- | --- | --- |
| `common.retry` | Thử lại | Try again | mọi màn |
| `common.retryNow` | Thử lại ngay | Try again now | `TrangLoi` 503 |
| `common.retry.auto` | Tự thử lại sau {seconds} giây | Retrying in {seconds} s | `TrangLoi` |
| `common.retry.running` | Đang thử lại… | Retrying… | `TrangLoi` |
| `common.loading.slow` | Đang mất nhiều thời gian hơn bình thường… | This is taking longer than usual… | † |
| `common.loading.events` | Đang tải sự kiện… | Loading events… | `DanhSachSuKien`, `SuKien` |
| `common.loadError.label` | Lỗi tải | Couldn't load | mọi màn |
| `common.price.free` | Miễn phí | Free | `StudioLoaiVe` |
| `common.price.from` | Giá từ | From | `DanhSachSuKien`, `SuKien` |
| `common.nav.skipToContent` | Bỏ qua tới nội dung | Skip to content | † (DOC-39 §9) |
| `common.nav.myTickets` | Vé của tôi | My tickets | `ComponentsStudio` O |
| `common.nav.studio` | Studio của bạn | Your Studio | `ComponentsStudio` O |
| `common.nav.buyerSite` | Trang mua vé | Ticket shop | `StudioTongQuan` |
| `common.nav.account` | Tài khoản | Account | `ComponentsStudio` O |
| `common.nav.signedInAs` | Đang đăng nhập | Signed in as | `ComponentsStudio` O |
| `common.nav.signOut` | Đăng xuất | Sign out | `ComponentsStudio` O |
| `common.nav.signIn` | Đăng nhập | Sign in | `SuKien` |
| `common.nav.createOrganizer` | Tạo hồ sơ tổ chức | Create organizer profile | † (DR-131 của DOC-38) |
| `common.nav.toEvents` | Về danh sách sự kiện | Back to events | `TrangLoi` |
| `common.nav.toEventPage` | Về trang sự kiện | Back to event | `KetQua` |
| `common.lang.label` | Ngôn ngữ | Language | † (DR-10) |
| `common.lang.vi` | Tiếng Việt | Tiếng Việt | † |
| `common.lang.en` | English | English | † |
| `common.dialog.close` | Đóng | Close | † |
| `common.unsaved.title` | Rời trang mà chưa lưu? | Leave without saving? | † (DR-70) |
| `common.unsaved.body` | Các thay đổi chưa lưu sẽ mất. | Your unsaved changes will be lost. | † |
| `common.unsaved.stay` | Ở lại | Stay | † |
| `common.unsaved.leave` | Rời đi | Leave | † |
| `common.copy.done` | Đã chép | Copied | † |

### 3.2 `auth`

Nguồn: `DangNhap` (7 biến thể), `DangXuat`, `HetPhien`. Khi bước thành công: `auth.sent.*`; sau bước đăng nhập là ghi chú ngữ cảnh `auth.after.*` (DOC-44).

| Key | `vi` | `en` |
| --- | --- | --- |
| `auth.form.label` | Bước đăng nhập | Sign in |
| `auth.form.title` | Đăng nhập để mua vé | Sign in to buy tickets |
| `auth.form.title.again` | Đăng nhập lại | Sign in again |
| `auth.form.intro` | Nhập email, chúng tôi gửi cho bạn một đường dẫn đăng nhập. Không cần mật khẩu. | Enter your email and we'll send you a sign-in link. No password needed. |
| `auth.form.email.label` | Email | Email |
| `auth.form.email.placeholder` | ban@example.com | you@example.com |
| `auth.form.email.error` | Email chưa đúng định dạng. | That email doesn't look right. |
| `auth.form.submit` | Gửi đường dẫn đăng nhập | Send sign-in link |
| `auth.signedOut.notice` | Bạn đã đăng xuất khỏi trình duyệt này. | You've signed out of this browser. |
| `auth.signedOut.label` | Đã đăng xuất | Signed out |
| `auth.sessionOver.label` | Hết phiên đăng nhập | Session ended |
| `auth.sessionOver.notice` | Phiên đăng nhập đã hết hạn. Đăng nhập lại để tiếp tục; trang bạn đang xem dở sẽ mở lại. | Your session has expired. Sign in again to continue; the page you were on will reopen. |
| `auth.sent.label` | Đã gửi | Sent |
| `auth.sent.title` | Kiểm tra hộp thư | Check your inbox |
| `auth.sent.body` | Nếu địa chỉ hợp lệ, một đường dẫn đăng nhập đã được gửi tới {email}. | If the address is valid, a sign-in link has been sent to {email}. |
| `auth.sent.emailFallback` | email của bạn | your email |
| `auth.sent.hint` | Đường dẫn chỉ dùng được một lần và hết hạn sau 15 phút. Nếu bạn yêu cầu nhiều lần, chỉ đường dẫn mới nhất dùng được. | The link works once and expires after 15 minutes. If you request several, only the newest works. |
| `auth.sent.openMail` | Mở hộp thư | Open inbox |
| `auth.sent.resend` | Gửi lại | Resend |
| `auth.sent.otherEmail` | Dùng email khác | Use a different email |
| `auth.throttled.title` | Bạn đã yêu cầu quá nhiều lần | You've asked too many times |
| `auth.throttled.body` | Mỗi email nhận tối đa 3 đường dẫn trong 15 phút. Hãy dùng đường dẫn mới nhất trong hộp thư, hoặc thử lại sau. | Each email gets at most 3 links per 15 minutes. Use the newest link in your inbox, or try again later. |
| `auth.verifying.label` | Đang đăng nhập | Signing in |
| `auth.verifying.title` | Đang xác minh đường dẫn | Checking your link |
| `auth.verifying.body` | Chỉ mất một giây. Bạn sẽ được đưa về đúng trang đang xem dở. | This takes a second. You'll go back to the page you were on. |
| `auth.invalid.label` | Không đăng nhập được | Couldn't sign in |
| `auth.invalid.title` | Đường dẫn không còn dùng được | This link no longer works |
| `auth.invalid.body` | Đường dẫn này đã hết hạn hoặc đã được dùng một lần. Hãy lấy một đường dẫn mới. | This link has expired or was already used. Get a new one. |
| `auth.invalid.cta` | Gửi đường dẫn mới | Send a new link |
| `auth.after.label` | Sau khi đăng nhập | After you sign in |
| `auth.after.seats` | Bạn quay lại chọn chỗ cho {event}. Chỗ đã chọn vẫn được nhớ. | You'll go back to picking seats for {event}. Your selection is remembered. |
| `auth.after.tickets` | Vé đã mua vẫn nằm ở trang Vé của tôi. | Tickets you've bought stay on My tickets. |
| `auth.after.page` | Bạn quay lại đúng trang đang xem dở. | You'll go back to the page you were on. |
| `auth.event.backToEvent` | Quay lại sự kiện | Back to event |

### 3.3 `events` (Danh sách, Sự kiện) và `queue` (Phòng chờ)

Danh sách (`DanhSachSuKien`):

| Key | `vi` | `en` |
| --- | --- | --- |
| `events.list.title` | Sự kiện sắp diễn ra | Upcoming events |
| `events.list.empty.title` | Chưa có sự kiện nào đang mở | No events on sale right now |
| `events.list.empty.body` | Sự kiện mới sẽ xuất hiện ở đây ngay khi người tổ chức xuất bản. | New events appear here as soon as organizers publish them. |
| `events.list.error.title` | Không tải được danh sách sự kiện | Couldn't load events |
| `events.list.error.body` | Kiểm tra kết nối mạng rồi thử lại. | Check your connection and try again. |
| `events.card.imageAlt` | (rỗng, ảnh trang trí) | (empty, decorative) |

Trang Sự kiện (`SuKien`):

| Key | `vi` | `en` |
| --- | --- | --- |
| `events.detail.time` | Thời gian | When |
| `events.detail.venue` | Địa điểm | Where |
| `events.detail.organizer` | Người tổ chức | Organizer |
| `events.detail.sale` | Mở bán | Sales |
| `events.detail.saleFrom` | Từ {time} | From {time} |
| `events.detail.saleUntil` | Đến {time} | Until {time} |
| `events.detail.cta.buy` | Mua vé | Buy tickets |
| `events.detail.cta.soon` | Mở bán sau | On sale in |
| `events.detail.cta.soon.countdown` | {d} ngày {time} | {d} days {time} |
| `events.detail.cta.loginFirst` | Đăng nhập trước | Sign in first |
| `events.detail.soon.noteSignedOut` | Email đăng nhập có thể đến chậm. Hãy đăng nhập trước để sẵn sàng khi mở bán. | The sign-in email can be slow. Sign in beforehand so you're ready when sales open. |
| `events.detail.soon.noteSignedIn` | Bạn đã đăng nhập. Quay lại đúng giờ mở bán để chọn chỗ. | You're signed in. Come back when sales open to pick your seats. |
| `events.detail.soldOut.body` | Tất cả vé của sự kiện này đã được bán. | All tickets for this event have been sold. |
| `events.detail.soldOut.cta` | Hết vé | Sold out |
| `events.detail.soldOut.other` | Xem sự kiện khác | See other events |
| `events.detail.paused.title` | Tạm dừng bán | Sales paused |
| `events.detail.paused.body` | Người tổ chức đang tạm dừng bán vé cho sự kiện này. Vé đã mua vẫn hợp lệ. | The organizer has paused ticket sales for this event. Tickets already bought stay valid. |
| `events.detail.closed.title` | Đã đóng bán | Sales closed |
| `events.detail.closed.body` | Đã quá thời hạn bán vé của sự kiện này. | The sales period for this event is over. |
| `events.detail.ended.body` | Sự kiện đã diễn ra. Vé bạn đã mua vẫn xem lại được ở trang Vé của tôi. | This event has taken place. Tickets you bought are still on My tickets. |
| `events.detail.cancelled.cta` | Không mở bán | Not on sale |
| `events.status.cancelled.noRefund` | Người tổ chức đã hủy sự kiện này. Chưa có vé nào được bán nên không ai bị trừ tiền. | The organizer cancelled this event. No tickets were sold, so nobody was charged. |
| `events.status.cancelled.refund` | Người tổ chức đã hủy sự kiện này. Nếu bạn đã mua vé, bạn sẽ được hoàn lại toàn bộ; xem Vé của tôi. | The organizer cancelled this event. If you bought tickets, you'll be refunded in full; see My tickets. |
| `events.detail.about` | Giới thiệu | About |
| `events.detail.types` | Loại vé | Ticket types |
| `events.detail.types.seat` | {range} · chọn ghế trên sơ đồ | {range} · pick seats on the map |
| `events.detail.types.zone` | {note} · không số ghế | {note} · no seat numbers |
| `events.detail.types.ga` | Vé vào cửa tự do · chọn số lượng | General admission · pick a quantity |
| `events.detail.how.title` | Cách mua | How to buy |
| `events.detail.how.1.title` | Chọn chỗ trên sơ đồ | Pick your seats |
| `events.detail.how.1.body` | Chọn từng ghế, hoặc chọn số lượng vé khu đứng. | Choose individual seats, or a quantity for standing zones. |
| `events.detail.how.2.title` | Giữ vé trong {minutes} phút | Hold them for {minutes} minutes |
| `events.detail.how.2.body` | Chỗ được giữ riêng cho bạn trong lúc thanh toán. | Your seats are held just for you while you pay. |
| `events.detail.how.3.title` | Trả bằng thẻ, nhận vé qua email | Pay by card, get tickets by email |
| `events.detail.how.3.body` | Vé cũng nằm ở trang Vé của tôi. | Tickets also appear on My tickets. |
| `events.detail.loading` | Đang tải sự kiện… | Loading event… |
| `events.detail.error.title` | Không tải được sự kiện | Couldn't load this event |
| `events.detail.error.body` | Kiểm tra kết nối mạng rồi thử lại. | Check your connection and try again. |
| `events.dateStamp.label` | Ngày diễn | Event date |

Ghi chú: dòng "Mỗi đơn tối đa 8 vé." của canvas bị **bỏ** (DR-41, DOC-38 §9). Số phút giữ vé là `{minutes}`, lấy từ trường `holdMinutes` mà API trả cùng sự kiện (suy ra từ `reservation.hold-duration`, mặc định `PT10M`, một giá trị cho cả nền tảng, DR-42, DOC-34); canvas viết cứng "10" là giá trị mặc định. Mọi chuỗi bên dưới có "10 phút" ở bản canvas dùng `{minutes}` trong key.

Phòng chờ (`PhongCho`, namespace `queue`; DOC-45):

| Key | `vi` | `en` |
| --- | --- | --- |
| `queue.title` | Phòng chờ | Waiting room |
| `queue.waiting.label` | Bạn đang trong hàng | You're in line |
| `queue.waiting.position` | Vị trí của bạn | Your position |
| `queue.waiting.eta` | Ước tính chờ | Estimated wait |
| `queue.waiting.eta.value` | {minutes} phút | {minutes} min |
| `queue.waiting.eta.unknown` | Đang ước tính | Estimating |
| `queue.waiting.auto` | Trang tự cập nhật vị trí và tự chuyển sang màn chọn chỗ khi tới lượt. Bạn không cần tải lại. | This page updates your position and moves on to seat picking when it's your turn. No need to reload. |
| `queue.waiting.oneSpot` | Mỗi tài khoản có một chỗ trong hàng. Mở thêm tab hay thiết bị không giúp lên trước. | Each account has one spot in line. Opening more tabs or devices won't move you ahead. |
| `queue.waiting.idle` | Nếu bạn đóng trang quá 2 phút, chỗ của bạn được nhường cho người sau. | If you close this page for more than 2 minutes, your spot goes to the next person. |
| `queue.leave` | Rời hàng | Leave the line |
| `queue.pre.label` | Bạn đã ở trong phòng chờ | You're in the waiting room |
| `queue.pre.opensIn` | Mở bán sau | Sales open in |
| `queue.pre.body` | Đúng giờ mở bán, mọi người trong phòng được xếp thứ tự ngẫu nhiên. Vào sớm 30 phút hay 1 phút đều như nhau, nên bạn không cần tải lại trang. | When sales open, everyone in the room is put in random order. Arriving 30 minutes or 1 minute early makes no difference, so there's no need to reload. |
| `queue.pre.leave` | Rời phòng chờ | Leave the waiting room |
| `queue.admitted.label` | Tới lượt bạn | It's your turn |
| `queue.admitted.remaining` | Lượt vào còn | Your pass lasts |
| `queue.admitted.body` | Hãy chọn chỗ và giữ vé trước khi lượt vào hết hạn. Khi đã giữ vé, bạn có đủ {minutes} phút để thanh toán. | Pick your seats and hold them before your pass runs out. Once held, you have {minutes} minutes to pay. |
| `queue.admitted.cta` | Vào chọn chỗ | Go pick seats |
| `queue.paused.label` | Vé đang tạm hết | Tickets temporarily gone |
| `queue.paused.heldByOthers` | Mọi vé đang được người khác giữ | All remaining tickets are held by other buyers |
| `queue.paused.body` | Nếu họ không thanh toán trong {minutes} phút, vé quay lại và hàng tiếp tục chạy. Bạn vẫn giữ nguyên vị trí. | If they don't pay within {minutes} minutes, the tickets return and the line moves again. You keep your position. |
| `queue.soldOut.label` | Đã hết vé | Sold out |
| `queue.soldOut.body` | Tất cả vé đã được bán và hàng đợi đã đóng. Cảm ơn bạn đã chờ. | All tickets are sold and the line is closed. Thanks for waiting. |
| `queue.soldOut.cta` | Về trang sự kiện | Back to the event |
| `queue.offline.label` | Đang kết nối lại | Reconnecting |
| `queue.offline.lastPosition` | Vị trí gần nhất của bạn | Your last known position |
| `queue.offline.title` | Mất kết nối với phòng chờ | Lost connection to the waiting room |
| `queue.offline.body` | Trang đang tự thử lại. Bạn vẫn giữ vị trí nếu kết nối lại trong vòng 2 phút. | The page is retrying. You keep your position if it reconnects within 2 minutes. |

### 3.4 `studio`

Nguồn: `StudioHoSo`, `StudioTongQuan`, `StudioThongTin`, `StudioLoaiVe`, `StudioXemTruoc`, `StudioXuatBan`, `StudioBanVe`, `ComponentsStudio`. Tên bước dùng chung key `studio.step.*`.

Chung và bước:

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.brand` | Studio | Studio |
| `studio.breadcrumb.events` | Sự kiện của bạn | Your events |
| `studio.step.info` | Thông tin | Details |
| `studio.step.ticketTypes` | Loại vé | Ticket types |
| `studio.step.map` | Sơ đồ | Seat map |
| `studio.step.preview` | Xem trước | Preview |
| `studio.step.publish` | Xuất bản | Publish |
| `studio.step.sales` | Bán vé | Sales |
| `studio.save.draft` | Bản nháp | Draft |
| `studio.save.saved` | Đã lưu | Saved |
| `studio.save.unsaved` | Có thay đổi chưa lưu | Unsaved changes |
| `studio.save.draftBtn` | Lưu bản nháp | Save draft |
| `studio.save.next` | Lưu và sang {step} | Save and go to {step} |
| `studio.save.nextStep` | Lưu và sang bước sau | Save and continue |
| `studio.save.back` | Quay lại {step} | Back to {step} |
| `studio.stale.title` | Sự kiện đã được sửa ở nơi khác | This event was edited elsewhere |
| `studio.stale.body` | Để không ghi đè lên bản đó, hãy tải lại bản mới nhất. Thay đổi chưa lưu ở tab này sẽ mất. | To avoid overwriting it, reload the latest version. Unsaved changes in this tab will be lost. |
| `studio.stale.cta` | Tải lại | Reload |

Hồ sơ tổ chức (DOC-53):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.profile.title` | Lập hồ sơ tổ chức | Create your organizer profile |
| `studio.profile.intro` | Hồ sơ tổ chức cho phép tài khoản của bạn tạo và bán vé sự kiện. Bạn vẫn mua vé bằng tài khoản này như bình thường. | An organizer profile lets your account create and sell event tickets. You can still buy tickets with this account as usual. |
| `studio.profile.name.label` | Tên tổ chức | Organization name |
| `studio.profile.name.error` | Hãy nhập tên tổ chức. | Enter an organization name. |
| `studio.profile.name.hint` | Mọi sự kiện và sơ đồ bạn tạo đều thuộc về tổ chức này. | Every event and seat map you create belongs to this organization. |
| `studio.profile.email.label` | Email liên hệ | Contact email |
| `studio.profile.email.error` | Email chưa đúng định dạng. | That email doesn't look right. |
| `studio.profile.email.hint` | Không bắt buộc. Để trống thì dùng email đăng nhập của bạn. | Optional. Leave blank to use your sign-in email. |
| `studio.profile.submit` | Tạo hồ sơ và vào Studio | Create profile and open Studio |
| `studio.profile.done.label` | Đã tạo hồ sơ | Profile created |
| `studio.profile.done.title` | {name} đã sẵn sàng | {name} is ready |
| `studio.profile.done.body` | Tài khoản của bạn giờ là tài khoản người tạo sự kiện. Bước tiếp theo là tạo sự kiện đầu tiên. | Your account can now create events. Next, create your first event. |
| `studio.profile.done.cta` | Vào Studio | Open Studio |
| `studio.profile.can.title` | Trong Studio bạn làm được | In Studio you can |
| `studio.profile.can.1` | Tạo sự kiện: tên, địa điểm, thời gian, khung mở bán. | Create events: name, venue, time and sales window. |
| `studio.profile.can.2` | Đặt loại vé và giá, rồi vẽ sơ đồ chỗ ngồi nếu bán theo ghế hoặc khu vực. | Set ticket types and prices, then draw a seat map if you sell by seat or zone. |
| `studio.profile.can.3` | Xuất bản, mở bán và theo dõi số vé đã bán. | Publish, open sales and track tickets sold. |
| `studio.profile.note` | Đối soát doanh thu và rút tiền chưa có ở giai đoạn này. | Revenue reconciliation and payouts aren't available yet. |
| `studio.profile.exists` | Tổ chức của bạn đã có hồ sơ. | You already have an organizer profile. |

Tổng quan (DOC-54):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.overview.title` | Sự kiện của bạn | Your events |
| `studio.overview.create` | Tạo sự kiện | Create event |
| `studio.overview.count` | {count, plural, other {# sự kiện}} | {count, plural, one {# event} other {# events}} |
| `studio.overview.sold` | Đã bán | Sold |
| `studio.overview.held` | Đang giữ | Held |
| `studio.overview.free` | Còn trống | Available |
| `studio.overview.soldOf` | {sold} / {capacity} vé đã bán | {sold} / {capacity} tickets sold |
| `studio.overview.heldNote` | {count} vé đang được giữ | {count} tickets held |
| `studio.overview.track` | Theo dõi bán vé | Track sales |
| `studio.overview.paused.body` | Đang không bán. Vé đã bán vẫn hợp lệ. | Not on sale. Tickets already sold stay valid. |
| `studio.overview.resume` | Mở bán lại | Resume sales |
| `studio.overview.draft.body` | Chưa xuất bản | Not published |
| `studio.overview.draft.missing` | Đã xong {done}. Còn thiếu {missing}. | {done} done. {missing} still missing. |
| `studio.overview.continue` | Soạn tiếp | Keep editing |
| `studio.overview.ended.body` | Sự kiện đã diễn ra. | This event has taken place. |
| `studio.overview.viewSales` | Xem số vé | View sales |
| `studio.overview.empty.title` | Chưa có sự kiện nào | No events yet |
| `studio.overview.empty.body` | Tạo sự kiện đầu tiên: nhập thông tin, đặt loại vé và giá, vẽ sơ đồ nếu cần, rồi xuất bản để mở bán. | Create your first event: enter details, set ticket types and prices, draw a seat map if needed, then publish to start selling. |
| `studio.overview.empty.cta` | Tạo sự kiện đầu tiên | Create your first event |
| `studio.overview.loading` | Đang tải sự kiện của bạn… | Loading your events… |
| `studio.overview.error.title` | Không tải được danh sách sự kiện | Couldn't load your events |
| `studio.overview.error.body` | Việc bán vé không bị ảnh hưởng. Kiểm tra kết nối mạng rồi thử lại. | Ticket sales aren't affected. Check your connection and try again. |

Thông tin sự kiện (DOC-55; lỗi theo ô lấy từ `validation.*`, mục 3.8):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.info.title` | Thông tin sự kiện | Event details |
| `studio.info.summary.errors` | Còn {count} mục cần sửa | {count, plural, one {# field needs fixing} other {# fields need fixing}} |
| `studio.info.summary.fix` | Sửa các ô được đánh dấu bên dưới rồi lưu lại. | Fix the highlighted fields below, then save. |
| `studio.info.live.title` | Sự kiện đang mở bán | This event is on sale |
| `studio.info.live.body` | Tên, mô tả và ảnh sửa tự do. Nếu đổi thời gian bắt đầu hoặc địa điểm, người đã mua vé sẽ nhận email thông báo. | You can edit the name, description and image freely. If you change the start time or venue, buyers get an email. |
| `studio.info.basic` | Cơ bản | Basics |
| `studio.info.name.label` | Tên sự kiện | Event name |
| `studio.info.description.label` | Mô tả | Description |
| `studio.info.image.label` | Ảnh sự kiện | Event image |
| `studio.info.image.empty` | Chưa có ảnh. Không bắt buộc. | No image yet. Optional. |
| `studio.info.image.choose` | Chọn ảnh | Choose image |
| `studio.info.image.change` | Đổi ảnh | Change image |
| `studio.info.image.remove` | Gỡ ảnh | Remove image |
| `studio.info.image.hint` | Ảnh ngang tỷ lệ 16:9 hiển thị đẹp nhất. Chưa có ảnh thì trang sự kiện hiện ngày diễn ở chỗ đó. | A 16:9 landscape image looks best. Without one, the event page shows the date there. |
| `studio.info.venue.label` | Địa điểm | Venue |
| `studio.info.timezone.label` | Múi giờ | Time zone |
| `studio.info.timezone.locked` | Không đổi được sau khi xuất bản. | Can't be changed after publishing. |
| `studio.info.time.title` | Thời gian diễn ra | Event time |
| `studio.info.start.label` | Bắt đầu | Starts |
| `studio.info.end.label` | Kết thúc | Ends |
| `studio.info.time.hint` | Mỗi sự kiện là một suất diễn: một địa điểm, một khung giờ. | Each event is one show: one venue, one time slot. |
| `studio.info.sale.title` | Khung mở bán | Sales window |
| `studio.info.saleStart.label` | Mở bán từ | Sales open |
| `studio.info.saleEnd.label` | Đóng bán lúc | Sales close |
| `studio.info.prefix.label` | Tiền tố mã vé | Ticket code prefix |
| `studio.info.prefix.hint` | Hai chữ cái in hoa, ví dụ GM → GM-4K7P-92XD. | Two capital letters, e.g. GM → GM-4K7P-92XD. |
| `studio.info.highDemand.label` | Bật phòng chờ trước giờ mở bán | Open the waiting room before sales start |
| `studio.info.highDemand.hint` | Dành cho sự kiện đông người. Người mua vào phòng chờ trước giờ mở bán và được xếp thứ tự ngẫu nhiên. Nếu để tắt, hàng đợi vẫn tự chạy khi quá đông. | For popular events. Buyers join a waiting room before sales open and are put in random order. If off, the queue still runs automatically when it's busy. |
| `studio.info.preview.title` | Người mua sẽ thấy | Buyers will see |
| `studio.info.preview.noTime` | Chưa đặt thời gian | No time set yet |
| `studio.info.preview.noVenue` | Chưa có địa điểm | No venue yet |
| `studio.info.preview.notSet` | chưa đặt | not set |
| `studio.info.preview.queue.on` | Bật | On |
| `studio.info.preview.queue.off` | Tự bật khi đông | Starts automatically when busy |
| `studio.info.preview.queue.label` | Phòng chờ trước giờ mở bán | Pre-sale waiting room |
| `studio.info.preview.draftNote` | Bản nháp chỉ mình bạn thấy cho tới khi xuất bản. | Only you can see a draft until you publish it. |

Ghi chú: ô "Số vé tối đa mỗi đơn" và dòng "Mỗi đơn" của canvas **bị bỏ** (DR-41). Ô "Tiền tố mã vé" và "Múi giờ" là bổ sung theo DR-52 và DR-12 (canvas chưa vẽ).

Loại vé và giá (DOC-56):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.types.title` | Loại vé và giá | Ticket types and prices |
| `studio.types.summary` | {count, plural, other {# loại vé}} · sức chứa {capacity} | {count, plural, one {# ticket type} other {# ticket types}} · capacity {capacity} |
| `studio.types.live.body` | Giá mới chỉ áp dụng cho đơn giữ vé sau lúc đổi. Loại vé đã có vé giữ hoặc bán thì không xóa được. | New prices apply only to holds made after the change. Ticket types with held or sold tickets can't be deleted. |
| `studio.types.errors` | Còn {count} mục cần sửa trong danh sách loại vé. | {count, plural, one {# item needs fixing} other {# items need fixing}} in your ticket types. |
| `studio.types.empty.title` | Chưa có loại vé nào | No ticket types yet |
| `studio.types.empty.body` | Sự kiện cần ít nhất một loại vé có sức chứa lớn hơn 0 mới xuất bản được. | An event needs at least one ticket type with capacity above 0 before it can be published. |
| `studio.types.add` | Thêm loại vé | Add ticket type |
| `studio.types.limit` | Tối đa 5 loại vé mỗi sự kiện. | Up to 5 ticket types per event. |
| `studio.types.name.label` | Tên loại vé | Ticket type name |
| `studio.types.name.error` | Hãy đặt tên cho loại vé. | Give this ticket type a name. |
| `studio.types.name.duplicate` | Tên này đã dùng cho loại vé khác. | This name is already used by another ticket type. |
| `studio.types.model.label` | Bán theo | Sold by |
| `studio.types.model.seat` | Ghế | Seat |
| `studio.types.model.zone` | Khu vực | Zone |
| `studio.types.model.ga` | Tự do | General admission |
| `studio.types.price.label` | Giá (₫) | Price (₫) |
| `studio.types.price.error` | Nhập giá, hoặc 0 nếu miễn phí. | Enter a price, or 0 for free. |
| `studio.types.price.free` | Miễn phí, không cần thẻ | Free, no card needed |
| `studio.types.price.live` | Áp dụng cho đơn mới | Applies to new orders |
| `studio.types.price.min` | Giá vé khác 0 phải từ {min}. | A paid ticket must cost at least {min}. |
| `studio.types.capacity.label` | Sức chứa | Capacity |
| `studio.types.capacity.error` | Nhập sức chứa từ 1 trở lên. | Enter a capacity of 1 or more. |
| `studio.types.capacity.floor` | Không thấp hơn {count} vé đang giữ và đã bán. | Can't go below the {count} tickets held and sold. |
| `studio.types.capacity.min` | Tối thiểu {count}: đang giữ và đã bán | Minimum {count}: held and sold |
| `studio.types.capacity.fromMap` | Lấy từ sơ đồ | Taken from the seat map |
| `studio.types.capacity.fromMap.seat` | Gán ghế ở bước Sơ đồ | Assign seats in the Seat map step |
| `studio.types.capacity.fromMap.zone` | Vẽ khu vực ở bước Sơ đồ | Draw zones in the Seat map step |
| `studio.types.capacity.none` | Chưa có | None yet |
| `studio.types.capacity.max` | Số vé bán tối đa | Maximum tickets for sale |
| `studio.types.delete` | Xóa loại vé này | Delete this ticket type |
| `studio.types.delete.aria` | Xóa loại vé {name} | Delete ticket type {name} |
| `studio.types.delete.unnamed` | chưa đặt tên | unnamed |
| `studio.types.delete.blocked` | Không xóa được: đã có {count} vé đang giữ hoặc đang bán | Can't delete: {count} tickets are held or sold |
| `studio.types.model.help.title` | Bán theo cách nào | How it's sold |
| `studio.types.model.help.seat` | Người mua chọn từng ghế trên sơ đồ. Sức chứa là số ghế bạn gán cho loại vé này khi vẽ sơ đồ. | Buyers pick individual seats on the map. Capacity is the number of seats you assign to this type when drawing the map. |
| `studio.types.model.help.zone` | Người mua chọn một khu vực và số lượng, không có số ghế. Sức chứa do bạn nhập khi vẽ khu vực. | Buyers pick a zone and a quantity, with no seat numbers. You enter the capacity when drawing the zone. |
| `studio.types.model.help.ga` | Vé vào cửa tự do: người mua chỉ chọn số lượng. Sức chứa nhập ngay ở đây và không cần sơ đồ. | General admission: buyers choose only a quantity. Enter the capacity here; no seat map needed. |
| `studio.types.model.help.mix` | Một sự kiện có thể trộn cả ba cách. Giá 0 là vé miễn phí: người mua không phải nhập thẻ. | An event can mix all three. A price of 0 is a free ticket: buyers don't enter a card. |
| `studio.types.next.map` | Có loại vé bán theo ghế hoặc khu vực nên bước sau là vẽ sơ đồ. | You have seat or zone ticket types, so the next step is drawing the seat map. |
| `studio.types.next.noMap` | Chỉ có vé tự do nên không cần sơ đồ. | Only general admission, so no seat map is needed. |
| `studio.types.next.empty` | Thêm ít nhất một loại vé để đi tiếp. | Add at least one ticket type to continue. |

Xem trước (DOC-58):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.preview.title` | Xem trước | Preview |
| `studio.preview.intro` | Đây là trang người mua sẽ thấy sau khi bạn xuất bản. Mọi chỗ đều còn trống vì sự kiện chưa mở bán. | This is the page buyers will see once you publish. Everything shows as available because sales haven't started. |
| `studio.preview.desktop` | Máy tính | Desktop |
| `studio.preview.phone` | Điện thoại · 390 px | Phone · 390 px |
| `studio.preview.frame` | Bản xem trước | Preview |
| `studio.preview.noHold` | Bản xem trước không giữ vé và không thu tiền. | The preview doesn't hold tickets or take payment. |
| `studio.preview.willSell` | Sẽ mở bán | Will go on sale |
| `studio.preview.capacity` | Tổng sức chứa | Total capacity |
| `studio.preview.seatsUnit` | {count} ghế | {count} seats |
| `studio.preview.ticketsUnit` | {count} vé | {count} tickets |
| `studio.preview.editInfo` | Sửa thông tin | Edit details |
| `studio.preview.editTypes` | Sửa loại vé | Edit ticket types |
| `studio.preview.editMap` | Sửa sơ đồ | Edit seat map |
| `studio.preview.next` | Sang bước Xuất bản | Go to Publish |

Xuất bản và mở bán (DOC-59):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.publish.title` | Xuất bản và mở bán | Publish and open sales |
| `studio.publish.state.title` | Trạng thái | Status |
| `studio.publish.ready.title` | Sẵn sàng xuất bản | Ready to publish |
| `studio.publish.ready.body` | Bản nháp chỉ mình bạn thấy. Khi xuất bản, sự kiện hiển thị công khai và hệ thống tạo kho {count} vé từ sơ đồ và loại vé. | Only you can see a draft. When you publish, the event becomes public and the system creates an inventory of {count} tickets from the map and ticket types. |
| `studio.publish.blocked.title` | Chưa xuất bản được | Can't publish yet |
| `studio.publish.blocked.body` | Còn {count} điều kiện chưa đạt. Sửa xong rồi quay lại trang này để xuất bản. | {count, plural, one {# condition isn't met.} other {# conditions aren't met.}} Fix them and come back to publish. |
| `studio.publish.cta` | Xuất bản và mở bán | Publish and open sales |
| `studio.publish.previewAgain` | Xem trước lần nữa | Preview again |
| `studio.publish.publishing` | Đang xuất bản… | Publishing… |
| `studio.publish.error` | Chưa xuất bản được vì mất kết nối tới máy chủ. Sự kiện vẫn là bản nháp, chưa có gì thay đổi. | Couldn't publish because the connection to the server was lost. The event is still a draft and nothing changed. |
| `studio.publish.done.soon` | Sự kiện đang hiển thị công khai. Vé mở bán lúc {time}. | The event is now public. Tickets go on sale at {time}. |
| `studio.publish.done.notice` | Đã xuất bản. Kho vé đã tạo đủ {count} vé. | Published. The inventory of {count} tickets is ready. |
| `studio.publish.onSale.body` | Người mua đang đặt vé. Đóng bán lúc {time}. | Buyers are booking tickets. Sales close at {time}. |
| `studio.publish.paused.title` | Đang tạm dừng bán | Sales paused |
| `studio.publish.paused.body` | Người mua chưa đặt thêm được vé cho tới khi bạn mở bán lại. Vé đã bán vẫn hợp lệ. | Buyers can't book more tickets until you resume sales. Tickets already sold stay valid. |
| `studio.publish.pause` | Tạm dừng bán | Pause sales |
| `studio.publish.resume` | Mở bán lại | Resume sales |
| `studio.publish.closeSale` | Đóng bán sớm | Close sales early |
| `studio.publish.closeSale.body` | Đóng bán ngay bây giờ. Không mở lại được; muốn dừng tạm thì dùng "Tạm dừng bán". | Close sales right now. This can't be undone; to stop temporarily use "Pause sales". |
| `studio.publish.ended.body` | Sự kiện đã qua giờ kết thúc {time} nên không còn bán vé. | The event ended at {time}, so tickets are no longer on sale. |
| `studio.publish.cancelled.body.noRefund` | Sự kiện không còn hiển thị để mua vé. Chưa có đơn nào đã thanh toán nên không phải hoàn tiền. | The event is no longer open for purchase. No paid orders exist, so nothing needs refunding. |
| `studio.publish.cancelled.body.refund` | Sự kiện không còn hiển thị để mua vé. {count} đơn đã thanh toán chuyển sang chờ hoàn tiền; người mua đã được báo qua email. | The event is no longer open for purchase. {count} paid orders moved to refund pending; buyers were notified by email. |
| `studio.publish.toSales` | Theo dõi bán vé | Track sales |
| `studio.publish.toOverview` | Về Sự kiện của bạn | Back to your events |
| `studio.publish.openPublic` | Mở trang công khai | Open public page |
| `studio.publish.checks.title` | Điều kiện xuất bản | Publishing conditions |
| `studio.publish.checks.pass` | Đạt | Met |
| `studio.publish.checks.fail` | Chưa đạt | Not met |
| `studio.publish.checks.ticketType` | Có ít nhất một loại vé với sức chứa lớn hơn 0 | At least one ticket type with capacity above 0 |
| `studio.publish.checks.ticketType.detail` | {count, plural, other {# loại vé}} · sức chứa {capacity} | {count, plural, one {# ticket type} other {# ticket types}} · capacity {capacity} |
| `studio.publish.checks.map` | Sơ đồ đã qua validate | Seat map passes validation |
| `studio.publish.checks.map.detail` | Bản nháp sơ đồ còn {count} vấn đề. | The map draft has {count} issues. |
| `studio.publish.checks.map.open` | Mở sơ đồ | Open map |
| `studio.publish.checks.start` | Sự kiện bắt đầu ở tương lai | The event starts in the future |
| `studio.publish.checks.start.detail` | Bắt đầu {time} | Starts {time} |
| `studio.publish.checks.info` | Thông tin | Details |
| `studio.publish.after.title` | Sửa sau khi mở bán | Editing after sales open |
| `studio.publish.after.info` | Tên, mô tả, ảnh sửa tự do. Đổi giờ bắt đầu hoặc địa điểm thì người đã mua nhận email thông báo. | Name, description and image can be edited freely. Changing the start time or venue emails existing buyers. |
| `studio.publish.after.info.cta` | Sửa thông tin | Edit details |
| `studio.publish.after.types` | Giá mới chỉ áp dụng cho lượt giữ vé tạo sau khi đổi. Sức chứa chỉ giảm được tới số vé đang giữ và đã bán. | New prices apply only to holds made after the change. Capacity can only drop to the number of tickets held and sold. |
| `studio.publish.after.types.cta` | Sửa loại vé | Edit ticket types |
| `studio.publish.after.map` | Sơ đồ sửa trên bản nháp rồi xuất bản thành phiên bản mới. | Edit the map as a draft, then publish it as a new version. |
| `studio.publish.after.map.cta` | Sửa sơ đồ | Edit seat map |
| `studio.publish.after.map.locked` | Từ giờ mở bán không sửa được sơ đồ. | The seat map can't be edited once sales have opened. |
| `studio.publish.summary.inventory` | Kho vé | Inventory |
| `studio.publish.lifecycle.title` | Vòng đời sự kiện | Event lifecycle |
| `studio.publish.lifecycle.current` | Hiện tại | Now |
| `studio.publish.confirm.title` | Xuất bản {name}? | Publish {name}? |
| `studio.publish.confirm.inventory` | Kho vé sẽ tạo | Inventory to create |
| `studio.publish.confirm.map` | Sơ đồ | Seat map |
| `studio.publish.confirm.mapVersion` | Phiên bản {n} | Version {n} |
| `studio.publish.confirm.saleStart` | Mở bán | Sales open |
| `studio.publish.confirm.body` | Sau khi xuất bản, sự kiện hiển thị công khai và không quay lại bản nháp được. Bạn vẫn tạm dừng bán được bất cứ lúc nào. | Once published, the event is public and can't go back to draft. You can still pause sales at any time. |
| `studio.publish.confirm.keep` | Chưa xuất bản | Not yet |
| `studio.publish.cancel.cta` | Hủy sự kiện | Cancel event |
| `studio.publish.cancel.title` | Hủy {name}? | Cancel {name}? |
| `studio.publish.cancel.body.noRefund` | Sự kiện sẽ ngừng hiển thị để mua vé và không mở lại được. Chưa có đơn nào đã thanh toán nên không phải hoàn tiền. | The event will stop being available to buy and can't be reopened. No paid orders exist, so nothing needs refunding. |
| `studio.publish.cancel.body.refund` | Sự kiện sẽ ngừng hiển thị để mua vé và không mở lại được. {count} đơn đã thanh toán sẽ chuyển sang chờ hoàn tiền và người mua nhận email. Việc hoàn tiền làm thủ công. | The event will stop being available to buy and can't be reopened. {count} paid orders will move to refund pending and buyers will be emailed. Refunds are issued manually. |
| `studio.publish.cancel.keep` | Giữ sự kiện | Keep event |
| `studio.publish.cancel.confirm` | Hủy sự kiện | Cancel event |
| `studio.publish.notice.close.success` | Đã đóng bán. | Sales closed. |
| `studio.publish.notice.stateConflict` | Trạng thái sự kiện vừa đổi. Tải lại trang để xem trạng thái mới. | The event's status just changed. Reload the page to see the new status. |

Theo dõi bán vé (DOC-60):

| Key | `vi` | `en` |
| --- | --- | --- |
| `studio.sales.title` | Bán vé | Sales |
| `studio.sales.updatedAt` | Cập nhật lúc {time} | Updated at {time} |
| `studio.sales.refresh` | Làm mới | Refresh |
| `studio.sales.paused` | Đang tạm dừng bán. Người mua chưa đặt thêm được vé; vé đã bán vẫn hợp lệ. | Sales are paused. Buyers can't book more tickets; tickets already sold stay valid. |
| `studio.sales.paused.cta` | Mở bán lại ở bước Xuất bản | Resume sales in the Publish step |
| `studio.sales.none` | Chưa có vé nào được bán. Vé mở bán lúc {time}. | No tickets sold yet. Sales open at {time}. |
| `studio.sales.soldOut` | Đã bán hết {count} vé. | All {count} tickets are sold. |
| `studio.sales.loading` | Đang tải số vé… | Loading ticket numbers… |
| `studio.sales.error.title` | Chưa tải được số vé | Couldn't load ticket numbers |
| `studio.sales.error.body` | Việc bán vé vẫn diễn ra bình thường, chỉ trang này chưa lấy được số liệu. Hãy kiểm tra kết nối rồi thử lại. | Sales carry on as normal; only this page couldn't get the numbers. Check your connection and try again. |
| `studio.sales.sold` | Đã bán | Sold |
| `studio.sales.held` | Đang giữ | Held |
| `studio.sales.held.sub` | Người mua đang thanh toán, giữ tối đa {minutes} phút | Buyers are paying; held for up to {minutes} minutes |
| `studio.sales.free` | Còn trống | Available |
| `studio.sales.free.sub` | Chưa ai giữ | Not held by anyone |
| `studio.sales.sold.sub` | {percent}% sức chứa {capacity} | {percent}% of capacity {capacity} |
| `studio.sales.byType` | Theo loại vé | By ticket type |
| `studio.sales.col.type` | Loại vé | Ticket type |
| `studio.sales.col.capacity` | Sức chứa | Capacity |
| `studio.sales.total` | Tổng | Total |
| `studio.sales.type.sub.seat` | {price} · Ghế | {price} · Seat |
| `studio.sales.type.sub.zone` | {price} · Khu vực | {price} · Zone |
| `studio.sales.type.sub.ga` | {price} · Tự do | {price} · General admission |
| `studio.sales.bar.aria` | {percent}% sức chứa {capacity}: {sold} vé đã bán, {held} vé đang giữ, {free} vé còn trống | {percent}% of capacity {capacity}: {sold} sold, {held} held, {free} available |
| `studio.sales.map.title` | Sơ đồ theo trạng thái | Seat map by status |
| `studio.sales.map.hint` | Rê chuột lên một ghế để xem số ghế, loại vé và trạng thái. | Hover a seat to see its number, ticket type and status. |
| `studio.sales.filter.all` | Tất cả | All |
| `studio.sales.zoneSold` | {name} · {sold} / {capacity} đã bán | {name} · {sold} / {capacity} sold |

### 3.5 `checkout` (Chọn chỗ, Chọn số lượng, Thanh toán)

Chọn chỗ (`ChonCho`, DOC-46) và Chọn số lượng (`ChonSoLuong`, DOC-47):

| Key | `vi` | `en` |
| --- | --- | --- |
| `checkout.steps.seats` | Chọn chỗ | Pick seats |
| `checkout.steps.tickets` | Chọn vé | Pick tickets |
| `checkout.steps.pay` | Thanh toán | Pay |
| `checkout.steps.receive` | Nhận vé | Get tickets |
| `checkout.picker.view.map` | Sơ đồ | Map |
| `checkout.picker.view.list` | Danh sách | List |
| `checkout.picker.view.aria` | Chuyển chế độ xem | Switch view |
| `checkout.picker.stage` | Sân khấu | Stage |
| `checkout.picker.zoom.low` | Sơ đồ đang thu nhỏ cho vừa màn hình. Phóng to để chọn ghế, hoặc chuyển sang Danh sách. | The map is zoomed out to fit your screen. Zoom in to pick seats, or switch to List. |
| `checkout.picker.loading` | Đang tải sơ đồ… | Loading the map… |
| `checkout.picker.error.title` | Không tải được sơ đồ | Couldn't load the map |
| `checkout.picker.error.body` | Kiểm tra kết nối mạng rồi thử lại. Chỗ bạn đã chọn vẫn được nhớ. | Check your connection and try again. Your selection is remembered. |
| `checkout.picker.zone.free` | Còn {count} vé | {count} left |
| `checkout.picker.zone.selected` | Đang chọn {count} vé | {count} selected |
| `checkout.picker.zone.add` | Thêm 1 vé | Add 1 ticket |
| `checkout.picker.legend.selected` | Đang chọn | Selected |
| `checkout.picker.legend.held` | Có người giữ | Held by someone |
| `checkout.picker.legend.sold` | Đã bán | Sold |
| `checkout.picker.seat.aria.available` | Hàng {row}, ghế {seat}, {type}, {price}, còn trống | Row {row}, seat {seat}, {type}, {price}, available |
| `checkout.picker.seat.aria.selected` | Hàng {row}, ghế {seat}, {type}, {price}, đang chọn | Row {row}, seat {seat}, {type}, {price}, selected |
| `checkout.picker.row.free` | còn {count} ghế | {count} seats left |
| `checkout.picker.row.full` | hết ghế | no seats left |
| `checkout.picker.tooltip.held` | Ghế này có người đang giữ | Someone is holding this seat |
| `checkout.picker.tooltip.sold` | Ghế này đã bán | This seat is sold |
| `checkout.order.title` | Đơn của bạn | Your order |
| `checkout.order.count` | {count, plural, other {# vé}} | {count, plural, one {# ticket} other {# tickets}} |
| `checkout.order.empty.seats` | Chưa có chỗ nào. Bấm vào ghế còn trống trên sơ đồ, hoặc bấm vào khu đứng để thêm vé. | Nothing selected. Click an available seat on the map, or a standing zone to add tickets. |
| `checkout.order.empty.ga` | Chưa có vé nào. Bấm dấu cộng bên cạnh loại vé bạn muốn mua. | No tickets yet. Press the plus next to the ticket type you want. |
| `checkout.order.item.seat` | Hàng {row} · Ghế {seat} | Row {row} · Seat {seat} |
| `checkout.order.item.zone` | {zone} × {qty} | {zone} × {qty} |
| `checkout.order.item.removeSeat` | Bỏ hàng {row} ghế {seat} khỏi đơn | Remove row {row} seat {seat} from the order |
| `checkout.order.subtotal` | Tạm tính | Subtotal |
| `checkout.ga.title` | Chọn số lượng vé | Choose ticket quantity |
| `checkout.ga.intro` | Sự kiện này không có sơ đồ chỗ ngồi. Vé vào cửa tự do, ngồi theo thứ tự đến. | This event has no seat map. Tickets are general admission, seated in order of arrival. |
| `checkout.ga.left` | Còn {count} vé | {count} left |
| `checkout.ga.soldOut` | Đã bán hết | Sold out |
| `checkout.ga.add` | Thêm một vé {name} | Add one {name} ticket |
| `checkout.ga.remove` | Bớt một vé {name} | Remove one {name} ticket |
| `checkout.ga.unit` | {price} mỗi vé | {price} each |
| `checkout.hold.cta` | Giữ vé trong {minutes} phút | Hold tickets for {minutes} minutes |
| `checkout.hold.cta.busy` | Đang giữ vé… | Holding tickets… |
| `checkout.hold.note.seats` | Chỗ chỉ được giữ sau khi bạn bấm nút này. Nếu có người nhanh tay hơn, bạn sẽ thấy ngay ghế nào bị trùng. | Seats are only held once you press this button. If someone is quicker, you'll see right away which seat clashed. |
| `checkout.hold.note.ga` | Vé chỉ được giữ sau khi bạn bấm nút này. Nếu không còn đủ vé, bạn sẽ được báo ngay để chọn lại. | Tickets are only held once you press this button. If there aren't enough left, you'll be told right away so you can choose again. |
| `checkout.limit.tooManyUnits` | Mỗi lần giữ vé tối đa {max} vé. | You can hold at most {max} tickets at a time. |
| `checkout.conflict.title` | Chưa giữ được vé | Couldn't hold your tickets |
| `checkout.conflict.seat` | Hàng {row} · Ghế {seat} vừa có người giữ, nên cả đơn chưa được giữ. Ghế này đã được bỏ khỏi đơn; hãy chọn ghế khác rồi giữ vé lại. | Row {row} · Seat {seat} was just taken, so none of your order was held. That seat was removed from your order; pick another and hold again. |
| `checkout.conflict.seats` | {count} ghế vừa có người giữ, nên cả đơn chưa được giữ. Các ghế này đã được bỏ khỏi đơn; hãy chọn ghế khác rồi giữ vé lại. | {count} seats were just taken, so none of your order was held. They were removed from your order; pick others and hold again. |
| `checkout.conflict.capacity` | Không còn đủ vé {name} (còn {left}, bạn chọn {requested}). Hãy giảm số lượng rồi giữ vé lại. | Not enough {name} tickets left ({left} left, you chose {requested}). Lower the quantity and hold again. |
| `checkout.conflict.active.title` | Bạn đang giữ vé cho sự kiện này | You already hold tickets for this event |
| `checkout.conflict.active.body` | Hoàn tất thanh toán, hoặc hủy giữ vé để chọn lại. | Finish paying, or release your hold to choose again. |
| `checkout.conflict.active.continue` | Tiếp tục thanh toán | Continue to payment |
| `checkout.conflict.active.release` | Hủy giữ vé | Release hold |
| `checkout.busy.title` | Đang rất đông | It's very busy |
| `checkout.busy.body` | Hệ thống tự thử lại sau {seconds} giây. Bạn không cần bấm lại hay tải lại trang. | We'll retry automatically in {seconds} seconds. No need to press again or reload. |

Thanh toán (`ThanhToan`, DOC-48):

| Key | `vi` | `en` |
| --- | --- | --- |
| `checkout.timer.label` | Giữ vé còn | Hold ends in |
| `checkout.timer.urgent` | Sắp hết giờ | Almost out of time |
| `checkout.timer.over` | Hết hạn giữ | Hold expired |
| `checkout.pay.title` | Thanh toán | Payment |
| `checkout.pay.card.title` | Thẻ thanh toán | Card payment |
| `checkout.pay.declined.title` | Thẻ bị từ chối | Card declined |
| `checkout.pay.declined.body` | Bạn chưa bị trừ tiền. Hãy thử lại hoặc dùng thẻ khác; vé vẫn được giữ đến khi hết giờ. | You haven't been charged. Try again or use another card; your tickets stay held until time runs out. |
| `checkout.pay.cta` | Thanh toán ngay · {amount} | Pay now · {amount} |
| `checkout.pay.cta.short` | Trả {amount} | Pay {amount} |
| `checkout.pay.cta.busy` | Đang xử lý thanh toán… | Processing payment… |
| `checkout.pay.secure` | Thông tin thẻ đi thẳng tới Stripe. Chúng tôi không nhận và không lưu số thẻ của bạn. | Card details go straight to Stripe. We never receive or store your card number. |
| `checkout.pay.changeMind` | Đổi ý? Ghế được trả lại ngay cho người khác. | Changed your mind? Your seats go straight back to others. |
| `checkout.pay.release` | Hủy giữ vé | Release hold |
| `checkout.pay.release.error` | Không hủy được vì thanh toán vừa thành công. Chúng tôi đang xác nhận đơn của bạn. | Couldn't release because your payment just went through. We're confirming your order. |
| `checkout.pay.free.label` | Đơn 0 đồng | Free order |
| `checkout.pay.free.title` | Không cần thanh toán | No payment needed |
| `checkout.pay.free.body` | Đơn này không mất phí nên bạn không cần nhập thẻ. Bấm Nhận vé để hoàn tất; vé sẽ được gửi tới email của bạn. | This order is free, so you don't need to enter a card. Press Get tickets to finish; your tickets will be emailed to you. |
| `checkout.pay.free.cta` | Nhận vé | Get tickets |
| `checkout.pay.tooShort` | Còn quá ít thời gian để thanh toán. Hãy chọn lại chỗ. | Too little time left to pay. Please pick again. |
| `checkout.expired.label` | Hết hạn giữ | Hold expired |
| `checkout.expired.title` | Đã hết thời gian giữ vé | Your hold has expired |
| `checkout.expired.body` | Chỗ của bạn đã được trả lại để người khác có thể mua. Thẻ của bạn chưa bị trừ tiền. | Your seats were released so others can buy them. Your card wasn't charged. |
| `checkout.expired.cta` | Chọn lại chỗ | Pick again |
| `checkout.summary.title` | Đơn của bạn | Your order |
| `checkout.summary.total` | Tổng cộng | Total |
| `checkout.summary.invite` | Vé mời | Complimentary |
| `checkout.loading` | Đang tải đơn của bạn… | Loading your order… |
| `checkout.error.title` | Không tải được đơn của bạn | Couldn't load your order |
| `checkout.error.body` | Vé vẫn đang được giữ cho bạn và đồng hồ vẫn chạy. Kiểm tra kết nối mạng rồi thử lại. | Your tickets are still held and the clock is still running. Check your connection and try again. |

### 3.6 `orders` (Kết quả) và vé (`tickets`)

Kết quả (`KetQua`, DOC-49):

| Key | `vi` | `en` |
| --- | --- | --- |
| `orders.paid.stamp` | Đã thanh toán | Paid |
| `orders.paid.title` | Vé của bạn đã sẵn sàng | Your tickets are ready |
| `orders.paid.body` | Chúng tôi đã gửi {count, plural, other {# vé}} tới email của bạn. Bạn cũng có thể xem lại bất cứ lúc nào ở trang Vé của tôi. | We've emailed {count, plural, one {# ticket} other {# tickets}} to you. You can also see them any time on My tickets. |
| `orders.paid.summary` | {count, plural, other {# vé}} {type} · trả bằng thẻ | {count, plural, one {# {type} ticket} other {# {type} tickets}} · paid by card |
| `orders.paid.paidLabel` | Đã trả | Paid |
| `orders.paid.cta.tickets` | Xem vé của tôi | See my tickets |
| `orders.confirming.label` | Đang xử lý | Processing |
| `orders.confirming.title` | Đang xác nhận thanh toán | Confirming your payment |
| `orders.confirming.body` | Chúng tôi đang chờ xác nhận cuối cùng từ ngân hàng, thường mất vài giây. Bạn không cần tải lại trang; chỗ của bạn vẫn đang được giữ. | We're waiting for final confirmation from your bank, which usually takes a few seconds. No need to reload; your seats are still held. |
| `orders.refund.label` | Chưa phát hành vé | Tickets not issued |
| `orders.refund.late.title` | Thanh toán đến sau khi hết giờ giữ vé | Your payment arrived after the hold expired |
| `orders.refund.late.body` | Chúng tôi đã nhận được tiền của bạn, nhưng lúc đó thời gian giữ vé đã hết và chỗ đã có người khác mua. Vì vậy chúng tôi không thể phát hành vé cho đơn này. | We received your money, but by then the hold had expired and the seats went to someone else. So we can't issue tickets for this order. |
| `orders.refund.mismatch.title` | Số tiền thanh toán không khớp với đơn | The amount paid doesn't match your order |
| `orders.refund.mismatch.body` | Số tiền chúng tôi nhận được khác với tổng đơn, nên chúng tôi không phát hành vé. | The amount we received differs from the order total, so we didn't issue tickets. |
| `orders.refund.cancelled.title` | Sự kiện đã bị hủy | The event was cancelled |
| `orders.refund.cancelled.body` | Người tổ chức đã hủy sự kiện, nên vé của đơn này không còn hiệu lực. | The organizer cancelled the event, so the tickets in this order are no longer valid. |
| `orders.refund.full` | Bạn sẽ được hoàn lại toàn bộ | You'll be refunded in full |
| `orders.refund.note` | Bạn không cần làm gì thêm. Thời gian hoàn tiền: {sla}. Cần hỗ trợ: {email}. | You don't need to do anything. Refund time: {sla}. Need help: {email}. |
| `orders.refunded.title` | Đơn đã được hoàn tiền | This order has been refunded |
| `orders.refunded.body` | Chúng tôi đã hoàn {amount} cho đơn này. | We refunded {amount} for this order. |
| `orders.expired.title` | Đơn đã hết hạn | This order has expired |
| `orders.expired.body` | Thời gian giữ vé đã hết và bạn chưa bị trừ tiền. | The hold ran out and you weren't charged. |
| `orders.cancelled.title` | Đơn đã bị hủy | This order was cancelled |
| `orders.cancelled.body` | Đơn này đã được hủy và bạn chưa bị trừ tiền. | This order was cancelled and you weren't charged. |

Biến `{sla}` và `{email}` lấy từ cấu hình `support.refund-sla-text` (mặc định `vi`: "5–10 ngày làm việc", `en`: "5–10 business days") và `support.email` (DR-44, DOC-34). Không để chỗ giữ chỗ như canvas.

Vé của tôi (`VeCuaToi`, DOC-50) và thẻ vé:

| Key | `vi` | `en` |
| --- | --- | --- |
| `tickets.title` | Vé của tôi | My tickets |
| `tickets.upcoming` | Sắp diễn ra | Upcoming |
| `tickets.past` | Đã diễn ra | Past |
| `tickets.card.label` | Vé vào cửa · {n}/{total} | Admission ticket · {n}/{total} |
| `tickets.card.row` | Hàng | Row |
| `tickets.card.seat` | Ghế | Seat |
| `tickets.card.type` | Loại vé | Ticket type |
| `tickets.card.code` | Mã vé | Ticket code |
| `tickets.order.paid` | {count, plural, other {# vé}} · đã trả | {count, plural, one {# ticket} other {# tickets}} · paid |
| `tickets.order.byCard` | bằng thẻ | by card |
| `tickets.order.viewEvent` | Xem trang sự kiện | View event page |
| `tickets.order.refundPending` | Chờ hoàn tiền | Refund pending |
| `tickets.order.refunded` | Đã hoàn tiền | Refunded |
| `tickets.order.cancelledEvent` | Sự kiện đã bị hủy | Event cancelled |
| `tickets.empty.title` | Bạn chưa có vé nào | You don't have any tickets yet |
| `tickets.empty.body` | Vé bạn mua sẽ nằm ở đây, và cũng được gửi tới email của bạn. | Tickets you buy will appear here and be emailed to you. |
| `tickets.empty.cta` | Xem sự kiện | Browse events |
| `tickets.loading` | Đang tải vé của bạn… | Loading your tickets… |
| `tickets.error.title` | Không tải được vé của bạn | Couldn't load your tickets |
| `tickets.error.body` | Vé của bạn vẫn an toàn và vẫn có trong email. Kiểm tra kết nối mạng rồi thử lại. | Your tickets are safe and still in your email. Check your connection and try again. |

### 3.7 Trang lỗi (`errors.page`)

Nguồn: `TrangLoi` (E1, E3, E4, E5), `HetPhien` (E2 = biến thể Đăng nhập, mục 3.2). Chi tiết bố cục ở DOC-52.

| Key | `vi` | `en` |
| --- | --- | --- |
| `errors.page.notFound.code` | 404 | 404 |
| `errors.page.notFound.label` | Không tìm thấy | Not found |
| `errors.page.notFound.title` | Trang này không tồn tại | This page doesn't exist |
| `errors.page.notFound.body` | Đường dẫn có thể đã gõ sai, hoặc sự kiện chưa được người tổ chức xuất bản. | The link may be mistyped, or the organizer hasn't published the event. |
| `errors.page.notFound.cta` | Xem các sự kiện | Browse events |
| `errors.page.notFound.foot` | Nếu bạn vào từ một đường dẫn được chia sẻ, hãy hỏi lại người gửi. | If you came from a shared link, ask whoever sent it. |
| `errors.page.forbidden.label` | Không có quyền | No access |
| `errors.page.forbidden.title` | Bạn không quản lý sự kiện này | You don't manage this event |
| `errors.page.forbidden.body` | Sự kiện này thuộc một tổ chức khác. Bạn đang đăng nhập bằng {email}. | This event belongs to another organization. You're signed in as {email}. |
| `errors.page.forbidden.cta` | Về Sự kiện của bạn | Back to your events |
| `errors.page.forbidden.other` | Đăng nhập tài khoản khác | Sign in with another account |
| `errors.page.forbidden.foot` | Mỗi sự kiện chỉ do tổ chức tạo ra nó quản lý. | Only the organization that created an event can manage it. |
| `errors.page.busy.label` | Đang đông | Busy |
| `errors.page.busy.title` | Hệ thống đang rất đông | The system is very busy |
| `errors.page.busy.body` | Trang sẽ tự thử lại, bạn không cần tải lại. Vé bạn đang giữ không bị ảnh hưởng. | This page retries on its own, so you don't need to reload. Tickets you hold aren't affected. |
| `errors.page.busy.foot` | Đồng hồ giữ vé vẫn chạy theo giờ của hệ thống trong lúc chờ. | Your hold timer keeps running on the system clock while you wait. |
| `errors.page.server.label` | Lỗi hệ thống | System error |
| `errors.page.server.title` | Có lỗi ở phía chúng tôi | Something went wrong on our side |
| `errors.page.server.body` | Hãy thử lại. Nếu vẫn lỗi, gửi cho chúng tôi mã yêu cầu bên dưới để tra cứu. | Try again. If it keeps failing, send us the request ID below so we can look into it. |
| `errors.page.server.requestId` | Mã yêu cầu | Request ID |
| `errors.page.server.foot` | Nếu bạn đang thanh toán, hãy xem trang Vé của tôi trước khi trả tiền lần nữa. | If you were paying, check My tickets before paying again. |

### 3.8 `validation` (lỗi theo ô, DR-25)

Server trả `errors: [{ "field", "rule" }]` (422 `VALIDATION_FAILED`); client tra `validation.<rule>`. Quy tắc dùng chung cho mọi form.

| Key (`validation.<rule>`) | `vi` | `en` |
| --- | --- | --- |
| `required` | Hãy nhập {field}. | Enter {field}. |
| `required.name` | Hãy nhập tên sự kiện. | Enter an event name. |
| `required.venue` | Hãy nhập địa điểm. | Enter a venue. |
| `required.startsAt` | Hãy chọn thời gian bắt đầu. | Choose a start time. |
| `required.endsAt` | Hãy chọn thời gian kết thúc. | Choose an end time. |
| `required.saleStartsAt` | Hãy chọn thời điểm mở bán. | Choose when sales open. |
| `required.saleEndsAt` | Hãy chọn thời điểm đóng bán. | Choose when sales close. |
| `too_long` | Tối đa {max} ký tự. | At most {max} characters. |
| `must_be_future` | Thời gian bắt đầu phải ở tương lai. | The start time must be in the future. |
| `must_be_after_start` | Kết thúc phải sau lúc bắt đầu. | The end must be after the start. |
| `too_long_event` | Sự kiện dài tối đa 72 giờ. | An event can last at most 72 hours. |
| `must_be_after_sale_start` | Đóng bán phải sau lúc mở bán. | Sales must close after they open. |
| `after_event_start` | Đóng bán không được muộn hơn giờ bắt đầu sự kiện. | Sales can't close after the event starts. |
| `invalid_timezone` | Múi giờ không hợp lệ. | That time zone isn't valid. |
| `locked_after_publish` | Không đổi được sau khi xuất bản. | Can't be changed after publishing. |
| `invalid_email` | Email chưa đúng định dạng. | That email doesn't look right. |
| `invalid_prefix` | Nhập đúng hai chữ cái in hoa. | Enter exactly two capital letters. |
| `too_many_units` | Mỗi lần giữ vé tối đa {max} vé. | You can hold at most {max} tickets at a time. |

Chuỗi dạng `required.<field>` có trong canvas dùng cho `name`, `venue`, thời gian; `required` chung dùng cho trường khác. `{field}` là tên trường đã dịch (`studio.info.<field>.label`).

### 3.9 `editor` (phần dùng chung; chuỗi từng công cụ ở DOC-57)

Seat map editor có rất nhiều chuỗi riêng (gợi ý theo công cụ, thuộc tính, phím tắt) thuộc DOC-57 (`08-ux-ui/screens/studio-map-editor.md`) và dùng namespace `editor`. Tài liệu này chỉ chốt phần các màn khác dẫn tới: trạng thái lưu, xung đột tab, màn hẹp, sơ đồ trống, xuất bản và **mã vấn đề validate** (DR-35).

| Key | `vi` | `en` |
| --- | --- | --- |
| `editor.title` | {event} · Bước 3/5 · Sơ đồ | {event} · Step 3/5 · Seat map |
| `editor.save.saving` | Đang lưu… | Saving… |
| `editor.save.saved` | Đã lưu | Saved |
| `editor.save.failed` | Chưa lưu được | Couldn't save |
| `editor.validate` | Validate | Validate |
| `editor.publish` | Xuất bản | Publish |
| `editor.empty.title` | Sơ đồ chưa có gì | The map is empty |
| `editor.empty.body` | Chọn công cụ Hàng ghế (R), vẽ một đường rồi nhập số ghế. Hoặc chọn Khu vực (Z) để vẽ một khu đứng và nhập sức chứa. | Pick the Seat row tool (R), draw a line and enter the number of seats. Or pick Zone (Z) to draw a standing area and enter its capacity. |
| `editor.zoom.low` | Sơ đồ đang thu nhỏ dưới 40%. Phóng to để sửa từng ghế. | The map is below 40% zoom. Zoom in to edit individual seats. |
| `editor.version.draft` | Chưa xuất bản | Not published |
| `editor.version.using` | đang dùng phiên bản {n} | using version {n} |
| `editor.publish.title` | Tạo phiên bản {n} từ bản nháp? | Create version {n} from the draft? |
| `editor.publish.body` | Mỗi phiên bản đã xuất bản là bất biến. Bạn vẫn sửa tiếp được bản nháp và xuất bản phiên bản mới sau. | Each published version is immutable. You can keep editing the draft and publish a new version later. |
| `editor.publish.done` | Đã tạo phiên bản {n} | Version {n} created |
| `editor.publish.refused.title` | Chưa xuất bản được phiên bản {n} | Couldn't publish version {n} |
| `editor.publish.refused.body` | Sự kiện đã mở bán, và {count} thay đổi dưới đây đụng tới vé đang được giữ hoặc đã bán. Không thay đổi nào được áp dụng; phiên bản {current} vẫn đang dùng. | Sales are open, and {count} changes below affect tickets that are held or sold. None were applied; version {current} is still in use. |
| `editor.publish.refused.fix` | Hoàn tác các thay đổi này trong bản nháp, hoặc chờ vé đang giữ được trả lại, rồi xuất bản lại. | Undo these changes in the draft, or wait for held tickets to be released, then publish again. |
| `editor.publish.refused.back` | Quay lại sửa | Back to editing |
| `editor.conflict.title` | Một tab khác đã lưu bản mới hơn | Another tab saved a newer version |
| `editor.conflict.body` | Để không ghi đè lên bản đó, tab này đã ngừng lưu. Hãy tải lại bản mới nhất; những thay đổi chưa lưu ở tab này sẽ mất. | To avoid overwriting it, this tab stopped saving. Reload the latest version; unsaved changes in this tab will be lost. |
| `editor.conflict.cta` | Tải lại bản mới nhất | Reload latest |
| `editor.narrow.label` | Màn hình hẹp | Narrow screen |
| `editor.narrow.title` | Sơ đồ cần màn hình rộng hơn | The seat map needs a wider screen |
| `editor.narrow.body` | Trình vẽ sơ đồ chạy trên màn hình rộng từ 1024 px. Hãy mở trên máy tính hoặc nới rộng cửa sổ trình duyệt. Bản nháp của bạn đã lưu và vẫn nguyên. | The map editor needs a screen at least 1024 px wide. Open it on a computer or widen your browser window. Your draft is saved and untouched. |
| `editor.narrow.preview` | Xem trước sự kiện | Preview event |
| `editor.narrow.back` | Về bước Loại vé | Back to Ticket types |
| `editor.narrow.note` | Các bước Thông tin, Loại vé, Xem trước và Xuất bản vẫn dùng được trên điện thoại. | The Details, Ticket types, Preview and Publish steps still work on a phone. |
| `editor.problems.title` | Vấn đề | Problems |
| `editor.problems.none` | Không có vấn đề | No problems |
| `editor.problems.hint` | Click một dòng để tới đối tượng. Lỗi chặn xuất bản, cảnh báo thì không. | Click a line to jump to the object. Errors block publishing; warnings don't. |
| `editor.problems.count.errors` | {count} lỗi cần sửa trước khi xuất bản | {count, plural, one {# error} other {# errors}} to fix before publishing |
| `editor.problems.count.warnings` | {count} cảnh báo | {count, plural, one {# warning} other {# warnings}} |
| `editor.problems.server.ok` | Máy chủ đã validate: đạt | Server validation passed |
| `editor.problems.server.fail` | Máy chủ từ chối: còn {count} lỗi | The server rejected it: {count} errors remain |

Mã vấn đề validate (DR-35); client dịch theo `code` + `params`, key `editor.issue.<CODE>`:

| Mã | Mức | `vi` | `en` |
| --- | --- | --- | --- |
| `SEAT_LABEL_DUPLICATE` | error | Nhãn ghế {section} · {row} · {seat} bị trùng. | The seat label {section} · {row} · {seat} is duplicated. |
| `SEAT_OVERLAP` | error | Hàng {row}: {count} ghế chồng lên nhau, tâm ghế cách nhau dưới {min}. | Row {row}: {count} seats overlap; their centres are closer than {min}. |
| `ROW_LABEL_MISSING` | error | Một hàng ghế chưa có nhãn. | A row has no label. |
| `TICKET_TYPE_MISSING` | error | Hàng {row}: {count} ghế chưa gán loại vé. | Row {row}: {count} seats have no ticket type. |
| `TICKET_TYPE_UNUSED` | error | Loại vé {name} chưa có ghế hoặc khu vực nào. | The ticket type {name} has no seats or zones. |
| `TICKET_TYPE_MODEL_MISMATCH` | error | {name} không khớp mô hình bán của loại vé đã gán. | {name} doesn't match the sales model of its ticket type. |
| `ZONE_CAPACITY_INVALID` | error | Khu vực {name} có sức chứa nhỏ hơn 1. | The zone {name} has a capacity below 1. |
| `POLYGON_SELF_INTERSECTS` | error | Đa giác {name} tự cắt nhau. | The polygon {name} crosses itself. |
| `SEAT_INSIDE_ZONE` | error | {count} ghế nằm bên trong khu vực {name}. | {count} seats lie inside the zone {name}. |
| `SEAT_LIMIT_EXCEEDED` | error | Sơ đồ có hơn 20.000 ghế. | The map has more than 20,000 seats. |
| `ZONES_OVERLAP` | warning | Hai khu vực chồng lên nhau. | Two zones overlap. |
| `OUT_OF_CANVAS` | warning | “{name}” nằm ngoài khung vẽ. | "{name}" lies outside the canvas. |

## 4. Ánh xạ mã lỗi → thông báo

### 4.1 Bảng mã lỗi của API

Mã `code` là tiếng Anh cố định (DR-10, DR-63); người dùng **không bao giờ** thấy mã. Giao diện tra theo `code`, **không** theo văn bản `title`/`detail` của server (chỉ dự phòng, mục 4.4). Key dạng `errors.<code viết thường>.title` và `.body`. Bảng nhóm theo HTTP; cột "Hành động" là việc giao diện làm ngoài hiện thông báo. Danh sách gồm mọi mã của SDD gốc 12.3 và DR-64 (cách ánh xạ exception → mã ở [DOC-35 §bảng exception](../06-design/error-handling.md)).

| HTTP | `code` | Loại | Key | Chuỗi `vi` (tiêu đề · nội dung) | Hành động giao diện |
| --- | --- | --- | --- | --- | --- |
| 400 | `IDEMPOTENCY_KEY_REQUIRED` | Lập trình | `errors.generic` | (như 500) | Lỗi nội bộ của client; ghi log, hiện thông báo chung |
| 401 | `UNAUTHENTICATED` | Phiên | `errors.unauthenticated` | Phiên đăng nhập đã hết hạn · Đăng nhập lại để tiếp tục; trang bạn đang xem dở sẽ mở lại. | Chuyển tới đăng nhập, `returnTo` (DOC-38 §8.5) |
| 401 | `LOGIN_LINK_INVALID` | Nghiệp vụ | `auth.invalid.*` | (mục 3.2) | Hiện biến thể "Đường dẫn hết hạn" |
| 403 | `FORBIDDEN` | Quyền | `errors.forbidden` | Bạn không có quyền làm việc này · Tài khoản của bạn không thuộc tổ chức quản lý mục này. | E3 khi ở `/studio/**`; ngoài ra thông báo `error` |
| 403 | `ORGANIZER_PROFILE_REQUIRED` | Nghiệp vụ | (không thông báo) | — | Chuyển tới `/studio/profile` |
| 404 | `NOT_FOUND` | Nghiệp vụ | `errors.page.notFound.*` | (mục 3.7) | E1 |
| 409 | `SEATS_UNAVAILABLE` | Nghiệp vụ | `checkout.conflict.seat(s)` | (mục 3.5) | Bỏ ghế khỏi đơn, đánh dấu "Có người giữ" (UXP-02, DOC-38 §7.2) |
| 409 | `INSUFFICIENT_CAPACITY` | Nghiệp vụ | `checkout.conflict.capacity` | (mục 3.5) | Giảm số lượng về `left`; cập nhật bộ đếm |
| 409 | `RESERVATION_NOT_ACTIVE` | Nghiệp vụ | `errors.reservation_not_active` | Lượt giữ vé này đã đóng · Chỗ của bạn đã được trả lại hoặc đơn đã xong. Hãy xem Vé của tôi hoặc chọn lại chỗ. | Gọi `GET /orders/{id}`; chuyển tới Kết quả hoặc Chọn chỗ |
| 409 | `REVISION_CONFLICT` | Nghiệp vụ | `editor.conflict.*` | (mục 3.9) | Màn `04j` |
| 409 | `EVENT_NOT_ON_SALE` | Nghiệp vụ | `errors.event_not_on_sale` | Sự kiện hiện không mở bán · Có thể đã tạm dừng, đã đóng bán hoặc chưa tới giờ mở bán. | Tải lại sự kiện; hiện nhãn theo `displayStatus` kèm trong lỗi |
| 409 | `ACTIVE_RESERVATION_EXISTS` | Nghiệp vụ | `checkout.conflict.active.*` | (mục 3.5) | Nút "Tiếp tục thanh toán" tới `/checkout/{reservationId}` |
| 409 | `PAYMENT_WINDOW_TOO_SHORT` | Nghiệp vụ | `checkout.pay.tooShort` | (mục 3.5) | Về Chọn chỗ |
| 409 | `PAYMENT_ALREADY_SUCCEEDED` | Nghiệp vụ | `checkout.pay.release.error` | (mục 3.5) | Chuyển tới Kết quả "Đang xác nhận" |
| 409 | `EVENT_STATE_CONFLICT` | Nghiệp vụ | `studio.publish.notice.stateConflict` | (mục 3.4) | Tải lại sự kiện |
| 409 | `STALE_EVENT_VERSION` | Nghiệp vụ | `studio.stale.*` | (mục 3.4) | Hộp thoại "Tải lại" |
| 409 | `ORGANIZER_EXISTS` | Nghiệp vụ | `studio.profile.exists` | (mục 3.4) | Chuyển tới `/studio` |
| 409 | `TICKET_TYPE_IN_USE` | Nghiệp vụ | `studio.types.delete.blocked` | (mục 3.4) | Vô hiệu nút xóa |
| 409 | `CAPACITY_BELOW_USED` | Nghiệp vụ | `studio.types.capacity.floor` | (mục 3.4) | Lỗi theo ô, kèm `min` từ response |
| 409 | `MAP_LOCKED_AFTER_SALE` | Nghiệp vụ | `studio.publish.after.map.locked` | (mục 3.4) | Khóa editor ở chế độ chỉ xem |
| 409 | `MAP_PUBLISH_BUSY` | Hạ tầng tạm | `errors.map_publish_busy` | Đang có lần xuất bản khác · Hãy đợi vài giây rồi thử lại. | Tự thử lại theo DOC-38 §8.2 |
| 409 | `MAP_ALREADY_EXISTS` | Nghiệp vụ | `errors.map_already_exists` | Sự kiện này đã có sơ đồ · Mỗi sự kiện chỉ có một sơ đồ. Hãy sửa sơ đồ hiện có. | Mở bước Sơ đồ |
| 413 | `PAYLOAD_TOO_LARGE` | Nghiệp vụ | `errors.payload_too_large` | Tệp quá lớn · Sơ đồ tối đa 5 MB, ảnh tối đa theo giới hạn ở bước Thông tin. Hãy giảm kích thước rồi thử lại. | Lỗi theo ô tải ảnh hoặc thông báo ở editor |
| 422 | `VALIDATION_FAILED` | Nghiệp vụ | `validation.<rule>` | (mục 3.8) | Lỗi theo ô theo `errors[].field`; `too_many_units` là thông báo chung |
| 422 | `IDEMPOTENCY_KEY_REUSED` | Lập trình | `errors.generic` | (như 500) | Lỗi nội bộ của client; sinh khóa mới cho thao tác kế tiếp |
| 422 | `PUBLISH_PRECONDITIONS_FAILED` | Nghiệp vụ | `studio.publish.blocked.*` | (mục 3.4) | Cập nhật danh sách điều kiện từ `failed[]` |
| 422 | `MAP_VALIDATION_FAILED` | Nghiệp vụ | `editor.problems.server.fail` | (mục 3.9) | Nạp `issues` vào bảng Vấn đề |
| 422 | `TICKET_TYPE_LIMIT_REACHED` | Nghiệp vụ | `studio.types.limit` | (mục 3.4) | Vô hiệu nút "Thêm loại vé" |
| 422 | `MEDIA_INVALID` | Nghiệp vụ | `errors.media_invalid` | Ảnh không dùng được · Chỉ nhận JPEG, PNG hoặc WebP trong giới hạn kích thước. Hãy chọn ảnh khác. | Lỗi theo ô tải ảnh |
| 429 | `RATE_LIMITED` | Hạ tầng tạm | `errors.rate_limited` | Bạn thao tác hơi nhanh · Hệ thống tự thử lại sau {seconds} giây. | Tự thử lại, DOC-38 §8.2 |
| 429 | `QUEUE_REQUIRED` | Nghiệp vụ | `errors.queue_required` | Bạn cần vào hàng chờ trước · Đang đưa bạn về phòng chờ của sự kiện. | Chuyển tới `/events/{id}/queue` (DR-58) |
| 500 | `INTERNAL_ERROR` | Lập trình | `errors.page.server.*` | (mục 3.7) | E5 hoặc thông báo `error` kèm `requestId` |
| 503 | `OVERLOADED` | Hạ tầng tạm | `checkout.busy.*` / `errors.page.busy.*` | (mục 1.4) | Tự thử lại với `Retry-After` |
| 503 | `PAYMENT_PROVIDER_UNAVAILABLE` | Hạ tầng tạm | `errors.payment_provider_unavailable` | Chưa kết nối được cổng thanh toán · Vé của bạn vẫn đang được giữ. Hệ thống tự thử lại sau {seconds} giây. | Tự thử lại; hiện đồng hồ giữ vé |
| 503 | `EMAIL_PROVIDER_UNAVAILABLE` | Hạ tầng tạm | `errors.email_provider_unavailable` | Chưa gửi được email · Hệ thống chưa gửi được đường dẫn đăng nhập. Hãy thử lại sau ít phút. | Giữ biểu mẫu email, nút "Gửi đường dẫn đăng nhập" bật lại |

Chuỗi `en` của các dòng có key `errors.*` (cặp tiêu đề · nội dung):

| Key | `en` |
| --- | --- |
| `errors.generic.title` / `.body` | Something went wrong · Please try again. If it keeps happening, tell us the request ID. |
| `errors.unauthenticated.title` / `.body` | Your session has expired · Sign in again to continue; the page you were on will reopen. |
| `errors.forbidden.title` / `.body` | You can't do that · Your account doesn't belong to the organization that manages this item. |
| `errors.reservation_not_active.title` / `.body` | This hold has closed · Your seats were released or the order is complete. Check My tickets or pick again. |
| `errors.event_not_on_sale.title` / `.body` | This event isn't on sale right now · It may be paused, closed or not open yet. |
| `errors.map_publish_busy.title` / `.body` | Another publish is running · Wait a few seconds and try again. |
| `errors.map_already_exists.title` / `.body` | This event already has a seat map · Each event has one map. Edit the existing one. |
| `errors.payload_too_large.title` / `.body` | The file is too large · Seat maps can be up to 5 MB; images have the limit shown in the Details step. Reduce the size and try again. |
| `errors.media_invalid.title` / `.body` | That image can't be used · Only JPEG, PNG or WebP within the size limit. Choose another image. |
| `errors.rate_limited.title` / `.body` | You're going a bit fast · We'll retry automatically in {seconds} seconds. |
| `errors.queue_required.title` / `.body` | You need to join the line first · Taking you to the event's waiting room. |
| `errors.payment_provider_unavailable.title` / `.body` | Can't reach the payment provider · Your tickets are still held. We'll retry in {seconds} seconds. |
| `errors.email_provider_unavailable.title` / `.body` | Couldn't send the email · We couldn't send your sign-in link. Try again in a few minutes. |

`errors.generic` ở `vi`: "Có lỗi xảy ra · Hãy thử lại. Nếu vẫn lỗi, gửi cho chúng tôi mã yêu cầu."

### 4.2 Quy tắc hiển thị lỗi

- **Lỗi nghiệp vụ** (4xx có `code` ở bảng): hiện ngay tại vị trí thao tác (thông báo `error` trong khung, hoặc lỗi theo ô). Không điều hướng trừ khi cột "Hành động" nói.
- **Lỗi hạ tầng tạm** (429, 503, mạng): thông báo `warning` + tự thử lại (DOC-38 §8.2); không dùng màu `error`.
- **Lỗi lập trình** (500, `IDEMPOTENCY_KEY_REUSED`, `IDEMPOTENCY_KEY_REQUIRED`): thông báo chung kèm `requestId` (`X-Request-Id`); client ghi `console.error` với `requestId` và `code`, không gửi telemetry bên thứ ba.
- Mọi thông báo lỗi có `role="alert"` (lỗi) hoặc `role="status"` (cảnh báo tạm); focus không bị cướp, trừ lỗi theo ô đầu tiên của form được focus khi submit.
- Mã lạ (không có trong bảng): hiện `errors.generic` và `title`/`detail` của server nếu `Content-Language` khớp locale hiện tại (mục 4.4).

### 4.3 Chuỗi phía backend

Backend dùng `MessageSource` (`messages_vi.properties`, `messages_en.properties`, DR-10) cho **email** và cho `title`/`detail` của Problem Details. Tiền tố `email.<mẫu>.*` (ví dụ `email.magicLink.subject`) thuộc DOC-51 và DOC-27; tiền tố `problem.<code>.title` và `problem.<code>.detail` mang **cùng chuỗi** với bảng 4.1 (nguồn duy nhất là bảng này; hai file `.properties` được sinh từ JSON `errors.*` bằng script `pnpm i18n:sync-problems` để không lệch; CI chạy `i18n:check` cho cả hai). Mã `code` luôn tiếng Anh. Ngôn ngữ chọn theo `Accept-Language` của request, mặc định `vi` (DR-10).

### 4.4 Dự phòng khi thiếu key

Nếu key thiếu ở locale hiện tại, i18next dùng `fallbackLng = vi` (DR-10); nếu thiếu cả ở `vi` thì hiện key thô trong `dev` (và `i18n:check` đã đỏ trước khi vào `main`). Với lỗi API có mã chưa ánh xạ, dùng `detail` của server rồi `errors.generic`.

## 5. Quy trình thêm chuỗi

Chi tiết ở [DOC-31](../06-design/i18n.md). Tóm tắt cho tài liệu này: thêm key vào đúng namespace ở cả `vi` và `en` trong **cùng PR**, thêm dòng vào bảng của mục 3, chạy `pnpm i18n:check` (hai locale cùng tập key, cùng placeholder, ICU hợp lệ). Chuỗi mới cho màn hình mới ghi vào `docs/08-ux-ui/screens/<màn>.md` §8 bằng **key**, không bằng chuỗi.

## 6. Test bắt buộc

Tiền tố `COPY-` (đã kiểm tra chưa dùng ở `docs/`; đăng ký ở DOC-69).

| ID | Kịch bản | Kết quả mong đợi |
| --- | --- | --- |
| COPY-01 | `pnpm i18n:check` trên `vi` và `en` của mọi namespace ở mục 3 | Cùng tập key, cùng tên placeholder, ICU parse được; 0 key thừa |
| COPY-02 | Với mọi `code` ở bảng 4.1, mock response lỗi có `code` đó | Giao diện hiện đúng key ở cột "Key" (kiểm bằng `data-i18n-key` ở chế độ test); không hiện `code` |
| COPY-03 | `code` mới `ABC_NEW` không có trong bảng | Hiện `errors.generic`; ghi `console.error` một lần |
| COPY-04 | `formatMoney(1800000, 'vi')` và `(…, 'en')` | `1.800.000 ₫` (với U+00A0) và `₫1,800,000` |
| COPY-05 | `formatMoney(0, 'vi')` cho giá vé | "Miễn phí" |
| COPY-06 | `formatEventTime(2026-11-14T13:00:00Z, tz='Asia/Ho_Chi_Minh', 'vi')` | `Thứ Bảy 14.11.2026 · 20:00` (không hậu tố) |
| COPY-07 | Cùng thời điểm, `tz='Asia/Tokyo'`, `PLATFORM_TIMEZONE=Asia/Ho_Chi_Minh` | `Thứ Bảy 14.11.2026 · 22:00 GMT+9`; ở `en` `Saturday 14.11.2026 · 22:00 GMT+9` |
| COPY-08 | Trình duyệt đặt `TZ=America/New_York` | Kết quả COPY-06 không đổi |
| COPY-09 | 422 `VALIDATION_FAILED` với `errors:[{field:'saleEndsAt',rule:'after_event_start'}]` | Ô "Đóng bán lúc" hiện "Đóng bán không được muộn hơn giờ bắt đầu sự kiện." |
| COPY-10 | Grep bundle `vi` và `en` tìm "Mỗi đơn tối đa" | Không có kết quả (DR-41) |
| COPY-11 | Grep mã nguồn tìm chuỗi tiếng Việt có dấu trong `*.tsx` | 0 kết quả ngoài `locales/` và test |
| COPY-12 | Màn Kết quả `REFUND_PENDING`, `refund_reason=LATE_PAYMENT`, `support.refund-sla-text` mặc định | Hiện "5–10 ngày làm việc"; không còn chuỗi `[THỜI GIAN HOÀN TIỀN]` |
| COPY-13 | Số nhiều `orders.paid.body` với `count=1` ở `en` và `vi` | `en`: "1 ticket"; `vi`: "1 vé" |
| COPY-14 | Trình duyệt `Accept-Language: en`, chưa đăng nhập, không cookie | Giao diện `en`; sau khi chọn `vi` thì cookie `tb_lang=vi` |
| COPY-15 | Tổng chuỗi `studio.*` có trong `vi`: kiểm không chuỗi nào chứa `{` mà thiếu `}` | Hợp lệ |

## 7. Tóm tắt quyết định phát sinh khi viết tài liệu này

| ID tạm | Quyết định | Quyết bởi | Ghi vào |
| --- | --- | --- | --- |
| DR-135 | Thêm namespace i18n `auth`, `queue`, `orders`, `tickets`, `errors`, `validation` ngoài năm namespace của DR-10; key lỗi API là `errors.<code viết thường>`; chuỗi `problem.*` của backend sinh từ cùng JSON | Claude (Owner ủy quyền) | DOC-31, DOC-40, DOC-35 |
| DR-136 | Hiển thị lỗi theo `code`, không theo `title`/`detail` của server (chỉ dự phòng); khi mã chưa ánh xạ dùng `errors.generic` | Claude (Owner ủy quyền) | DOC-36, DOC-40 |
| DR-137 | Giữ cùng định dạng ngày `dd.MM.yyyy · HH:mm` ở cả `vi` và `en`, chỉ tên thứ đổi theo locale (khác `Intl` mặc định của `en`) | Claude (Owner ủy quyền) | DOC-31, DOC-40 |

## Câu hỏi còn mở

Không có. Quyết định phát sinh khi viết tài liệu này: DR-135…137 (mục 7), đã vào sổ quyết định. Chuỗi `en` do tài liệu này dịch, chờ Owner rà ngữ cảnh khi duyệt.
