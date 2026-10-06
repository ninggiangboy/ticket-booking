# Màn hình: Đăng nhập

> Trạng thái: **Review** · Cập nhật: 2026-10-06 · DOC-44
> Phụ thuộc: SDD gốc §5, §13, [Sổ quyết định](../../00-decision-register.md) (DR-10, 21, 22, 23, 56, 63, 64, 66, 67, 68), [DOC-03](../../01-product/requirements.md) (FR-01, FR-17), [DOC-04](../../01-product/use-cases.md) (UC-01, UC-20), [DOC-06](../../02-glossary.md), [DOC-37](../../07-api/api-endpoints.md) (E-01…05), [DOC-38](../ux-principles-and-ia.md) §3, §5, §8.5, [DOC-39](../design-system.md), [DOC-40](../ui-states-and-copy.md) §3.2, §4, [DOC-41](README.md), [DOC-82](../../06-design/flows/README.md) (FL-01…04); canvas: artboard `02`, `02b`, `E2`, `M02` (và email `03`)
> Người dùng chính: P1-01 (khung frontend), P1-04 (nhóm xác thực), tác giả DOC-83 (FL-01…04) và DOC-19, người viết test E2E đăng nhập

Tài liệu mô tả hai route: `/login` (biểu mẫu và các trạng thái sau khi gửi) và `/auth/callback` (xác minh đường dẫn). Hành vi phía server (token, giới hạn gửi, session) ở DOC-19 và DR-21/22; từng lời gọi qua các tầng ở DOC-83; chuỗi giao diện ở DOC-40 §3.2 (màn này chỉ dẫn **key**); email magic link ở DOC-51; trang lỗi toàn trang (E1, E3…E5) ở DOC-52. Biến thể "Hết phiên đăng nhập" (artboard `E2`) là một trạng thái của màn này, không phải trang riêng (DR-139).

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | Mọi persona (PS-1…PS-5) khi chưa có session; PS-1 (390 px) là ưu tiên |
| Use case | UC-01 (đăng nhập, đăng xuất, hết phiên), UC-20 (ngôn ngữ giao diện: bộ chọn `LanguageSwitch` ở header) |
| Yêu cầu | FR-01.1…01.12, FR-17 |
| Hành trình | J-1 bước 3–4, J-2 bước 2, J-3 bước 1 |
| Luồng chi tiết | FL-01 (xin link), FL-02 (mở link, nhận session), FL-03 (đăng xuất, hết phiên), FL-04 (đổi ngôn ngữ) |
| Quyền | Công khai. Người **đã có session** vào `/login` được chuyển thẳng tới `returnTo` (hoặc `/`); vào `/auth/callback` khi đã có session thì vẫn xác minh token (người dùng có thể đang đổi tài khoản) |

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/login` | `returnTo` | Đường dẫn nội bộ, bắt đầu bằng một `/`, không bắt đầu bằng `//` hoặc `/\`, dài ≤ 512; sai thì dùng `/` (DR-21 mục 6, FR-01.8). Client kiểm trước; server kiểm lại ở E-02 |
| `/login` | `reason` | `signed_out` hoặc `session_expired`; chọn biến thể thông báo đầu thẻ. Giá trị khác bị bỏ (DR-138) |
| `/auth/callback` | `token` | Đúng 43 ký tự `[A-Za-z0-9_-]` (32 byte base64url, DR-21 mục 2). Thiếu hoặc sai dạng → hiện biến thể "Đường dẫn hết hạn" **không gọi API** |

Ví dụ: `/login?returnTo=%2Fevents%2F0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35%2Fseats`; sau khi hết phiên: `/login?returnTo=%2Fcheckout%2F0199b1c2-8d14-7e50-a1b3-4c5d6e7f8091&reason=session_expired`.

Ngay khi trang callback đọc được `token`, nó gọi `history.replaceState` thành `/auth/callback` (bỏ `token` khỏi thanh địa chỉ, lịch sử và header `Referer` của các request sau). Tải lại trang callback sau đó rơi vào nhánh "thiếu token" (token dùng một lần nên không mất gì).

## 3. Wireframe

Desktop (≥ 1024 px), biểu mẫu `Nhập email`. Thẻ rộng tối đa 504 px, căn giữa (canvas `02`):

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.                                    [vi|en]   Quay lại sự kiện    │  header 64 px
├──────────────────────────────────────────────────────────────────────────┤
│                    ┌──────────────────────────────┐                      │
│                    │ BƯỚC ĐĂNG NHẬP               │  nhãn mono           │
│                    │ ĐĂNG NHẬP ĐỂ MUA VÉ          │  h1 40 px            │
│                    │ Nhập email, chúng tôi gửi... │                      │
│                    │ Email                        │                      │
│                    │ [ ban@example.com          ] │  52 px               │
│                    │ [ Gửi đường dẫn đăng nhập  ] │  nút chính           │
│                    │ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗ │  đường xé            │
│                    │ SAU KHI ĐĂNG NHẬP            │                      │
│                    │ Bạn quay lại chọn chỗ cho …  │                      │
│                    └──────────────────────────────┘                      │
└──────────────────────────────────────────────────────────────────────────┘
```

Điện thoại 390 px (canvas `M02`): thẻ tràn chiều ngang trừ lề 16 px, `padding: 28 px 20 px`, `h1` 32 px; liên kết "Quay lại sự kiện" xuống dòng dưới logo khi hết chỗ; nút chính rộng 100%.

```text
┌────────────────────────┐
│ ticket.        [vi|en] │
│ Quay lại sự kiện       │
│ ┌────────────────────┐ │
│ │ BƯỚC ĐĂNG NHẬP     │ │
│ │ ĐĂNG NHẬP ĐỂ MUA VÉ│ │
│ │ Nhập email, ...    │ │
│ │ Email              │ │
│ │ [ ban@example.com ]│ │
│ │ [ Gửi đường dẫn…  ]│ │
│ │ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │ │
│ │ SAU KHI ĐĂNG NHẬP  │ │
│ │ Bạn quay lại …     │ │
│ └────────────────────┘ │
└────────────────────────┘
```

## 4. Vùng và component

Tên component theo [DOC-39 §7](../design-system.md); trang là `Login` (`/login`) và `AuthCallback` (`/auth/callback`), đặt ở `frontend/src/features/auth/` (DOC-12 §6). `AuthCallback` dùng lại thẻ của `Login` ở biến thể "Đang xác minh" và "Đường dẫn hết hạn".

| Vùng | Component | Ghi chú |
| --- | --- | --- |
| Header rút gọn | logo `APP_NAME` + dấu chấm đỏ, `LanguageSwitch` (§7.19), liên kết quay lại | Không có menu tài khoản (chưa đăng nhập). Liên kết quay lại: nếu `returnTo` khớp `^/events/<uuid>(/|$)` → "Quay lại sự kiện" (`auth.event.backToEvent`) tới `/events/<uuid>`; còn lại → "Về danh sách sự kiện" (`common.nav.toEvents`) tới `/` |
| Thẻ đăng nhập | thẻ trắng bo 6 px, `padding 28 px`, mép xé ở chân (DOC-39 §6) | `max-width 504 px`; một thẻ cho mọi biến thể |
| Thông báo đầu thẻ | `Notice` `success` (đã đăng xuất, `role="status"`) hoặc `warning` (hết phiên, `role="alert"`) | Chỉ ở trạng thái biểu mẫu |
| Nhãn + tiêu đề | nhãn mono 12 px chữ hoa + `h1` Barlow Condensed 40 px (32 px ở 390 px) | `h1` nhận tiêu điểm khi đổi trạng thái (DOC-39 §9) |
| Ô email | `TextField` (§7.2), `type="email"` **không** dùng (kiểm bằng regex riêng), `autocomplete="email"`, `inputmode="email"` | Lỗi theo ô dưới ô nhập, viền 2 px `--stamp`, `aria-invalid`, `role="alert"` |
| Nút chính | `Button primary` 52 px | Một nút chính mỗi trạng thái (UXP-01) |
| Việc phụ | `Button text` cao 44 px | "Gửi lại", "Dùng email khác" |
| Thông báo giới hạn | `Notice` `error`-tông tem (canvas dùng nền `--stamp-tint`) | Chỉ ở "Gửi quá nhiều lần" |
| Thanh tiến trình | thanh 4 px, `role="status"` ở vùng chứa | Chỉ ở "Đang xác minh"; không phải thanh giả: không có giá trị thật nên hiển thị chuyển động vô định, tắt khi `prefers-reduced-motion` |
| Ghi chú "Sau khi đăng nhập" | nhãn mono + một câu 14 px | Chọn câu theo §6.4 |

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Người dùng hiện tại | `GET /me` (E-04) | Vào `/login`; khi tab lấy lại tiêu điểm trong lúc ở trạng thái "Đã gửi" (§6.3) | 200 → đã đăng nhập → `navigate(returnTo, { replace: true })`. 401 → ở lại. Kết quả không cache khi 401 |
| Tên sự kiện cho ghi chú | `GET /events/{eventId}` (E-07) | Chỉ khi `returnTo` khớp `^/events/<uuid>/(seats|queue)$` và có `localStorage["tb.selection.<eventId>"]` | Công khai, cache 5 giây (DOC-36); lỗi hoặc chậm > 1 giây → dùng ghi chú chung `auth.after.page`. Không chặn việc hiển thị biểu mẫu |
| Xin link | `POST /auth/magic-link` (E-01) | Bấm "Gửi đường dẫn đăng nhập", "Gửi lại" | §6.1 |
| Xác minh | `POST /auth/verify` (E-02) | Mount `AuthCallback` | §6.2 |
| Đăng xuất | `POST /auth/logout` (E-03) | Mục "Đăng xuất" ở `AccountMenu` (không thuộc màn này; màn này hiện kết quả) | §6.5 |
| Ngôn ngữ | `PATCH /me` (E-05) | Đổi `LanguageSwitch` khi đã đăng nhập; khi chưa chỉ đặt cookie `tb_lang` | Màn Đăng nhập luôn là trường hợp "chưa đăng nhập" |

Không có kênh real-time; không có làm mới theo nhịp.

Tham số gửi và nhận (khớp DR-21, DR-22; schema đầy đủ ở E-01, E-02):

```http
POST /api/v1/auth/magic-link
Content-Type: application/json
Accept-Language: vi

{ "email": " An@Example.com ", "returnTo": "/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35/seats", "locale": "vi" }
```
```http
HTTP/1.1 202 Accepted
X-Magic-Link-Throttled: 1        ← chỉ khi vượt giới hạn gửi; không có thân
```
```http
POST /api/v1/auth/verify
{ "token": "q3J8Xk1b0Zp7n2VwYtR5uLcA9dEoGhIm4sFjKxN6vTa" }

HTTP/1.1 200 OK
Set-Cookie: tb_session=…; Path=/; HttpOnly; SameSite=Lax; Secure
{ "returnTo": "/events/0199b1c2-7a3e-7c40-8f11-2d5e9a6b0c35/seats", "csrfToken": "…" }
```

`locale` gửi theo ngôn ngữ giao diện hiện tại (`i18n.language`); email do server dựng theo giá trị này (DR-10).

## 6. Tương tác

### 6.1 Biểu mẫu và gửi link (FL-01)

| Thao tác | Gọi | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Gõ vào ô email | không | Xóa lỗi theo ô | — |
| Bấm "Gửi đường dẫn đăng nhập" hoặc `Enter` | client: chuẩn hóa `trim` rồi kiểm regex `^[^@\s]+@[^@\s]+\.[^@\s]+$` và dài ≤ 254 | Sai → lỗi theo ô `auth.form.email.error`, tiêu điểm về ô; **không gọi API** | — |
| (tiếp) | `POST /auth/magic-link` (E-01) | Nút `aria-busy`, giữ chiều rộng. 202 → trạng thái "Đã gửi" với email đã chuẩn hóa (chữ thường). Có header `X-Magic-Link-Throttled: 1` → "Gửi quá nhiều lần" | 422 `VALIDATION_FAILED` (server chặt hơn client) → lỗi theo ô như trên. 503 `EMAIL_PROVIDER_UNAVAILABLE` → ở lại biểu mẫu, giữ email, `Notice` `error` `errors.email_provider_unavailable.*`, nút bật lại. 429 `RATE_LIMITED` (nginx `auth_ip`, DR-55) → `Notice` `warning` `errors.rate_limited.*` với `{seconds}` = `retryAfterSeconds`; nút bị vô hiệu đến hết đếm ngược rồi bật lại, **không tự gửi lại** (POST này không thuộc nhóm tự thử lại, DOC-38 §8.2). Lỗi mạng hoặc 500 → `Notice` `error` `errors.generic.*` kèm `requestId` nếu có |
| Bấm "Gửi lại" ở "Đã gửi" | `POST /auth/magic-link` (E-01) với email cũ | Như trên; link cũ bị thay (FR-01.4); giữ trạng thái "Đã gửi" | Như trên; lỗi hiện `Notice` phía trên nút "Mở hộp thư" |
| Bấm "Dùng email khác" | không | Về biểu mẫu, **xóa** email đã nhập, tiêu điểm vào ô email | — |
| Bấm "Mở hộp thư" | không (liên kết) | Mở webmail của miền email trong tab mới: `gmail.com` → `https://mail.google.com`, `outlook.com`/`hotmail.com`/`live.com` → `https://outlook.live.com/mail`, `yahoo.com` → `https://mail.yahoo.com`, `icloud.com` → `https://www.icloud.com/mail`; profile `dev` → Mailpit (`VITE_MAILPIT_URL`, mặc định `http://localhost:8025`). Miền khác: **ẩn** nút (DR-138) | — |

Server **không** tiết lộ email đã có tài khoản hay chưa: 202 giống hệt (FR-01.7); nhãn "Đã gửi" luôn nói "Nếu địa chỉ hợp lệ…" (`auth.sent.body`).

### 6.2 Mở link và nhận session (FL-02)

1. Người dùng mở `/auth/callback?token=…` (thường ở tab mới từ email).
2. `AuthCallback` kiểm dạng token, `replaceState` bỏ token khỏi URL, hiện "Đang xác minh đường dẫn" (`auth.verifying.*`).
3. Gọi `POST /auth/verify` (E-02) **đúng một lần**: lời gọi được giữ trong một `Map<token, Promise>` cấp module để `React.StrictMode` (hai lần mount ở `dev`) và việc mount lại không gửi lần hai, vì lần hai chắc chắn nhận 401 (token đã dùng).
4. 200 → lưu `csrfToken` **trong bộ nhớ** (không `localStorage`), `GET /me` (E-04) để nạp người dùng, vai trò, `locale`; nếu `me.locale` khác ngôn ngữ đang hiển thị thì `i18n.changeLanguage(me.locale)` (DR-10: `app_user.locale` thắng cookie); `navigate(returnTo, { replace: true })` với `returnTo` từ response (server đã kiểm lại, sai thì `/`).
5. 401 `LOGIN_LINK_INVALID` (hết hạn, đã dùng, bị thay) hoặc token sai dạng → "Đường dẫn không còn dùng được" (`auth.invalid.*`) với nút "Gửi đường dẫn mới" (`auth.invalid.cta`): chuyển tới `/login` (giữ `returnTo` nếu còn biết; sau `replaceState` thì không còn, nên về `/login` trống `returnTo`). 
6. 429/503/lỗi mạng ở bước này: hiện cùng thẻ nhưng nội dung `errors.generic.*` hoặc `errors.rate_limited.*` kèm nút "Thử lại" (`common.retry`) gọi lại E-02 **với cùng token**: an toàn vì nếu lần trước đã tiêu thụ token thì nhận 401 (hiện "không còn dùng được") và nếu chưa thì thành công. Không tự động thử lại (POST không idempotent theo `Idempotency-Key`).

Bộ quét link của hộp thư mở `/auth/callback?token=…` bằng `GET` (không chạy JavaScript) không làm token mất hiệu lực, vì chỉ `POST /auth/verify` tiêu thụ (FR-01.9, SDD gốc 5).

### 6.3 Đồng bộ giữa các tab

Tab đã gửi link vẫn hiện "Kiểm tra hộp thư" sau khi người dùng đăng nhập ở tab mới. Khi tab này nhận lại tiêu điểm (`visibilitychange` → `visible`) trong trạng thái "Đã gửi", nó gọi `GET /me` (E-04, tối đa một lần mỗi 5 giây); 200 → `navigate(returnTo, { replace: true })` (DR-138). Trình duyệt khác thiết bị không có session và không tự đăng nhập (DR-21 mục 5).

### 6.4 Ghi chú "Sau khi đăng nhập"

Chọn một câu duy nhất, theo thứ tự ưu tiên:

| Điều kiện | Key | Ví dụ hiển thị |
| --- | --- | --- |
| `reason=signed_out` | `auth.after.tickets` | "Vé đã mua vẫn nằm ở trang Vé của tôi." |
| `returnTo` khớp `^/events/<uuid>/(seats|queue)$`, có `tb.selection.<eventId>` và lấy được tên sự kiện | `auth.after.seats` (`{event}` in đậm) | "Bạn quay lại chọn chỗ cho **Hòa nhạc Giao Mùa**. Chỗ đã chọn vẫn được nhớ." |
| Còn lại, kể cả `reason=session_expired` | `auth.after.page` | "Bạn quay lại đúng trang đang xem dở." |

Tên sự kiện là dữ liệu của người tổ chức, hiện nguyên văn không dịch (UXP-14).

### 6.5 Đăng xuất và hết phiên (FL-03)

- **Đăng xuất:** `AccountMenu` gọi `POST /auth/logout` (E-03, cần `X-CSRF-Token`), xóa cache TanStack Query và `csrfToken` trong bộ nhớ, rồi `navigate("/login?reason=signed_out", { replace: true })`. Lỗi mạng của E-03 vẫn chuyển (cookie hết hạn dần phía server): hiện biến thể đã đăng xuất.
- **Hết phiên:** mọi 401 `UNAUTHENTICATED` ngoài E-02 (DOC-38 §8.5) → `navigate("/login?returnTo=<đường dẫn đang xem>&reason=session_expired", { replace: true })`. Lựa chọn chưa giữ đã nằm sẵn ở `localStorage` (DOC-38 §5).
- Các tab khác không được báo; chúng nhận 401 ở request kế tiếp và tự vào biến thể "Hết phiên".

## 7. Trạng thái

Bảy biến thể của canvas ánh xạ vào một máy trạng thái; `reason` chỉ chọn thông báo đầu thẻ khi ở "Nhập email".

| # | Biến thể (canvas) | Bước | Điều kiện vào | Key chính (DOC-40 §3.2) |
| --- | --- | --- | --- | --- |
| 1 | Nhập email (`02`, `M02`) | `form` | `/login` không `reason` | `auth.form.label`, `auth.form.title`, `auth.form.intro`, `auth.form.email.*`, `auth.form.submit` |
| 2 | Đã gửi (`02`) | `sent` | E-01 trả 202 không có header giới hạn | `auth.sent.label`, `.title`, `.body`, `.hint`, `.openMail`, `.resend`, `.otherEmail` |
| 3 | Gửi quá nhiều lần (`02`) | `sent` + thông báo | E-01 trả 202 kèm `X-Magic-Link-Throttled: 1` | như 2 + `auth.throttled.title`, `auth.throttled.body` |
| 4 | Đang xác minh (`02`) | `verifying` | `/auth/callback` đang chờ E-02 | `auth.verifying.label`, `.title`, `.body` |
| 5 | Đường dẫn hết hạn (`02`) | `expired` | E-02 trả 401 `LOGIN_LINK_INVALID`, hoặc token sai dạng | `auth.invalid.label`, `.title`, `.body`, `.cta` |
| 6 | Đã đăng xuất (`02b`) | `form` + `success` | `reason=signed_out` | `auth.signedOut.label`, `auth.signedOut.notice`, `auth.form.title.again` |
| 7 | Hết phiên đăng nhập (`E2`) | `form` + `warning` | `reason=session_expired` | `auth.sessionOver.label`, `auth.sessionOver.notice`, `auth.form.title.again` |

Trạng thái khác:

| Trạng thái | Hiển thị |
| --- | --- |
| Đang kiểm tra session (`GET /me` lúc vào `/login`) | Hiện biểu mẫu ngay (không chờ); nếu `GET /me` về 200 thì chuyển đi. Không có khung chờ vì 401 là trường hợp thường |
| Đang gửi | Nút `aria-busy`, nhãn giữ nguyên, ô email `readonly` |
| Lỗi tạm thời khi gửi (503, 429, mạng) | `Notice` ngay trên nút, không đổi bước (§6.1); `role="alert"` cho lỗi, `role="status"` cho cảnh báo tạm |
| Quá tải toàn trang | Không dùng E4: lỗi nằm trong thẻ để người dùng không mất email đã nhập |
| Không quyền | Không áp dụng (công khai) |
| JavaScript tắt | Không hỗ trợ: SPA (DOC-38). Trang callback bắt buộc JavaScript (token không tiêu thụ bằng `GET`) |

Thứ tự tiêu điểm: vào `/login` → ô email (hoặc `h1` nếu có thông báo đầu thẻ); sang "Đã gửi" → `h1`; lỗi theo ô → ô email; "Dùng email khác" → ô email.

## 8. Microcopy

Chuỗi `vi`/`en` đầy đủ nằm ở [DOC-40 §3.2](../ui-states-and-copy.md) (namespace `auth`) và §3.1 (`common`), §4.1 (`errors`); tài liệu này không chép lại. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Biểu mẫu | `auth.form.label`, `auth.form.title`, `auth.form.title.again`, `auth.form.intro`, `auth.form.email.label`, `auth.form.email.placeholder`, `auth.form.email.error`, `auth.form.submit` |
| Thông báo đầu thẻ | `auth.signedOut.label`, `auth.signedOut.notice`, `auth.sessionOver.label`, `auth.sessionOver.notice` |
| Đã gửi | `auth.sent.label`, `auth.sent.title`, `auth.sent.body`, `auth.sent.emailFallback`, `auth.sent.hint`, `auth.sent.openMail`, `auth.sent.resend`, `auth.sent.otherEmail` |
| Giới hạn | `auth.throttled.title`, `auth.throttled.body` |
| Xác minh | `auth.verifying.label`, `auth.verifying.title`, `auth.verifying.body`, `auth.invalid.label`, `auth.invalid.title`, `auth.invalid.body`, `auth.invalid.cta` |
| Ghi chú sau đăng nhập | `auth.after.label`, `auth.after.seats`, `auth.after.tickets`, `auth.after.page` |
| Liên kết | `auth.event.backToEvent`, `common.nav.toEvents`, `common.lang.label` |
| Lỗi | `errors.email_provider_unavailable.title/.body`, `errors.rate_limited.title/.body`, `errors.generic.title/.body`, `common.retry` |

Không thiếu key nào so với DOC-40. DOC-04 UC-01 dùng tên key cũ (`auth.login.email.invalid`, `auth.sent.throttled.*`, `auth.verify.invalid.*`, `auth.session.expired.notice`, `auth.sent.emailUnavailable`, `error.rateLimited.body`); DOC-40 là nguồn, DOC-04 cần đồng bộ (ghi ở phần báo cáo của DOC-44).

## 9. Tiêu chí nghiệm thu

Tiền tố `LGN-` (màn Đăng nhập). Mỗi mục là test có thể tự động.

| ID | Given | When | Then |
| --- | --- | --- | --- |
| LGN-01 | `/login`, ô email trống | Bấm "Gửi đường dẫn đăng nhập" | Hiện `auth.form.email.error`, tiêu điểm ở ô email, **không có request** `POST /auth/magic-link` |
| LGN-02 | Email `an@example` (không có dấu chấm sau `@`) | Bấm gửi | Như LGN-01; email ` An@Example.com ` (có khoảng trắng, chữ hoa) được chấp nhận và gửi dạng `an@example.com` |
| LGN-03 | Email hợp lệ, server trả 202 | Bấm gửi | Hiện "Đã gửi" với `an@example.com`; `h1` nhận tiêu điểm; có nút "Gửi lại", "Dùng email khác" |
| LGN-04 | Server trả 202 kèm `X-Magic-Link-Throttled: 1` | Bấm gửi | Hiện "Đã gửi" **và** thông báo `auth.throttled.*`; thân thông báo nói đúng "3 đường dẫn trong 15 phút" |
| LGN-05 | Server trả 503 `EMAIL_PROVIDER_UNAVAILABLE` | Bấm gửi | Ở lại biểu mẫu, ô email giữ nguyên giá trị, thông báo lỗi `errors.email_provider_unavailable.*`, nút bấm được lại |
| LGN-06 | nginx trả 429 `RATE_LIMITED`, `retryAfterSeconds: 6` | Bấm gửi | Thông báo `warning` đếm ngược "6, 5, …"; nút bị vô hiệu; hết đếm ngược nút bật lại; **không** tự gửi lại |
| LGN-07 | Ở "Đã gửi" | Bấm "Dùng email khác" | Về biểu mẫu, ô email **trống**, tiêu điểm ở ô email |
| LGN-08 | Ở "Đã gửi" với `gmail.com` | Quan sát | Có liên kết "Mở hộp thư" tới `https://mail.google.com`, `target="_blank"`, `rel="noopener noreferrer"`; với `example.org` (profile không `dev`) nút không có |
| LGN-09 | Token hợp lệ 43 ký tự | Mở `/auth/callback?token=…` | Hiện "Đang xác minh đường dẫn"; URL không còn `token` ngay sau khi trang đọc được; đúng **một** `POST /auth/verify` kể cả `React.StrictMode` |
| LGN-10 | E-02 trả 200 `{ returnTo: "/events/<id>/seats" }` | Sau xác minh | `GET /me` được gọi; `csrfToken` có trong bộ nhớ và **không** có trong `localStorage`/`sessionStorage`; chuyển tới `/events/<id>/seats` bằng `replace` (nút Back không quay lại callback) |
| LGN-11 | E-02 trả 401 `LOGIN_LINK_INVALID` | Sau xác minh | Hiện "Đường dẫn không còn dùng được" và nút "Gửi đường dẫn mới" đưa tới `/login` |
| LGN-12 | `/auth/callback` không có `token`, hoặc `token=abc` | Mở trang | Hiện biến thể hết hạn; **không có request** `POST /auth/verify` |
| LGN-13 | `returnTo=//evil.example` hoặc `returnTo=/\evil` hoặc 513 ký tự | Mở `/login?returnTo=…` rồi gửi | Request E-01 mang `returnTo: "/"` |
| LGN-14 | `/login?reason=signed_out` | Mở trang | Thông báo `success` `auth.signedOut.notice`, tiêu đề "Đăng nhập lại", ghi chú `auth.after.tickets` |
| LGN-15 | `/login?returnTo=%2Fcheckout%2F…&reason=session_expired` | Mở trang | Thông báo `warning` `auth.sessionOver.notice` có `role="alert"`; ghi chú `auth.after.page` |
| LGN-16 | `returnTo=/events/<id>/seats`, `tb.selection.<id>` tồn tại, E-07 trả tên "Hòa nhạc Giao Mùa" | Mở `/login?returnTo=…` | Ghi chú `auth.after.seats` có tên sự kiện in đậm; E-07 lỗi → `auth.after.page` và biểu mẫu vẫn hiện bình thường |
| LGN-17 | Đã có session hợp lệ | Mở `/login?returnTo=/me/tickets` | Chuyển tới `/me/tickets` bằng `replace` |
| LGN-18 | Tab A ở "Đã gửi"; tab B hoàn tất đăng nhập cùng trình duyệt | Chuyển lại tab A | Trong ≤ 1 giây sau khi tab A hiện, gọi `GET /me` và chuyển tới `returnTo` |
| LGN-19 | Đang ở 390 px | Mở `/login` | Không cuộn ngang; mọi điều khiển cao ≥ 44 px; nút chính rộng 100% thẻ |
| LGN-20 | Bàn phím | Dùng Tab, Enter | Thứ tự Tab: bộ chọn ngôn ngữ → liên kết quay lại → ô email → nút gửi; `Enter` trong ô email gửi biểu mẫu; `axe` không có lỗi |
| LGN-21 | `prefers-reduced-motion: reduce` | Ở "Đang xác minh" | Thanh tiến trình không chuyển động |
| LGN-22 | Ngôn ngữ `en` | Mở `/login` | Mọi chuỗi từ key `auth.*` ở `en`; email gửi đi ở `en` (`locale: "en"` trong request E-01) |

## 10. Ca kiểm thử E2E

Playwright, thư mục `frontend/e2e/`, dùng Mailpit để đọc email (DOC-61); chạy ở profile `fake-payments` (không liên quan thanh toán).

| Tên | Bước | Kỳ vọng |
| --- | --- | --- |
| `login-happy-path` | Mở `/login?returnTo=/me/tickets`; nhập email mới; đọc email ở Mailpit; mở link ở tab mới | Tab mới tới `/me/tickets`; `GET /me` 200; tab cũ chuyển theo (LGN-18) |
| `login-link-single-use` | Mở cùng link lần hai | Biến thể hết hạn (LGN-11) |
| `login-link-superseded` | Xin link A rồi "Gửi lại" ra link B; mở A rồi B | A hết hạn; B thành công (FR-01.4) |
| `login-throttle` | Gửi 4 lần cùng email trong 15 phút | Lần 4 hiện "Gửi quá nhiều lần"; Mailpit chỉ có 3 email |
| `login-smtp-down` | Dừng Mailpit rồi gửi | Lỗi 503 theo ô (LGN-05); khởi động lại Mailpit, "Gửi lại" thành công |
| `logout-and-expired` | Đăng nhập; đăng xuất; đăng nhập lại ở hai tab rồi xóa session phía server (`UPDATE session SET revoked_at = now()`) và thao tác ở một tab | Đăng xuất → LGN-14; tab kia → LGN-15 và quay lại đúng trang sau đăng nhập |
| `login-mobile` | Viewport 390 × 844, cùng luồng `login-happy-path` | Qua; LGN-19 |

## 11. Câu hỏi còn mở

Không có. Quyết định mới khi viết tài liệu này đã ghi `DR-138` (chi tiết màn Đăng nhập) và `DR-139` (biến thể E2 là trạng thái của `Login`), nội dung ở báo cáo của DOC-41 và đề xuất chính thức trong DOC-41 §7.
