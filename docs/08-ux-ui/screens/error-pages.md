# Màn hình: Trang lỗi

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-52
> Phụ thuộc: SDD gốc §10.5, §12.3, §13, [Sổ quyết định](../../00-decision-register.md) (DR-10, 23, 55, 56, 63, 64, 66, 67, 68), [DOC-06](../../02-glossary.md), [DOC-35](../../06-design/error-handling.md) §3–§7, [DOC-36](../../07-api/api-guidelines.md), [DOC-37](../../07-api/api-endpoints.md) (E-04), [DOC-38](../ux-principles-and-ia.md) §3, §8.2, §8.5, [DOC-39](../design-system.md) §7.18, [DOC-40](../ui-states-and-copy.md) §1.4, §1.5, §3.7, §4, [DOC-41](README.md), [DOC-44](login.md) (E2); canvas: artboard `E1`, `E2`, `E3`, `E4`, `E5`
> Người dùng chính: P1-01 (khung frontend, router, error boundary), mọi task màn hình (trang lỗi là đích của lỗi toàn trang), người viết test E2E lỗi

Tài liệu mô tả năm trang lỗi toàn trang E1…E5: khi nào hiện, hiện gì, làm gì tiếp. Thông báo lỗi **trong** một màn (thông báo, lỗi theo ô, hộp thoại) không thuộc tài liệu này: bảng mã lỗi → thông báo và quy tắc hiển thị ở DOC-40 §4; mapping exception → mã lỗi → HTTP ở DOC-35. Chuỗi giao diện ở DOC-40 §3.7 (màn này chỉ dẫn key). E2 (hết phiên) là biến thể của màn Đăng nhập và được đặc tả ở DOC-44; mục 2 dưới đây chỉ nêu điều kiện vào và cách chuyển.

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | Mọi persona; PS-1 (390 px) gặp E4 trong giờ mở bán đông, PS-3/PS-4 gặp E3 ở studio |
| Use case | Mọi UC có lỗi toàn trang; trực tiếp UC-01 (E2), UC-03 (E4 lúc giữ vé), UC-07 (E3) |
| Yêu cầu | NFR-02 (hành vi dưới tải), NFR-03, FR-11.6 (tự thử lại), FR-17 (đa ngôn ngữ) |
| Luồng chi tiết | FL-03 (E2), FL-13/FL-30 (E4 khi giữ vé), FL-05…FL-11 (E3 ở studio) |
| Quyền | Công khai: trang lỗi hiện được cả khi chưa đăng nhập và cả khi `GET /me` lỗi |

## 2. URL và search params

Trang lỗi **không có URL riêng**: chúng được router hiện thay cho route đang lỗi và **giữ nguyên URL** để nút "Thử lại" và nút Back hoạt động. Duy nhất E1 gắn với route `*`.

| Trang | Mã | Hiện khi | Thành phần router |
| --- | --- | --- | --- |
| E1 Không tìm thấy | 404 | (a) URL không khớp route nào (`*`); (b) tham số `:eventId`, `:reservationId`, `:orderId` sai định dạng UUID; (c) API trả 404 `NOT_FOUND` cho dữ liệu chính của route, kể cả tài nguyên của tổ chức khác (DR-23); (d) bước Sơ đồ khi sự kiện không có loại vé `SEAT`/`ZONE` **không** hiện E1 mà chuyển về `…/preview` (DOC-38 §3) | `NotFoundPage` |
| E2 Hết phiên | 401 | API trả 401 `UNAUTHENTICATED` ở bất kỳ request nào ngoài `POST /auth/verify` | **Không có trang**: chuyển tới `/login?returnTo=…&reason=session_expired` (DOC-44 §6.5) |
| E3 Không có quyền | 403 | API trả 403 `FORBIDDEN` ở route `/studio/**` (DR-140). Không hiện cho `ORGANIZER_PROFILE_REQUIRED` (chuyển tới `/studio/profile`) | `ForbiddenPage` |
| E4 Đang đông | 503 / 429 | Dữ liệu chính của route chưa tải được lần đầu vì 503 (`OVERLOADED`, `PAYMENT_PROVIDER_UNAVAILABLE`), 429 `RATE_LIMITED` hoặc lỗi mạng; **chưa có nội dung nào để giữ lại** | `UnavailablePage` |
| E5 Lỗi máy chủ | 500 | API trả 500 `INTERNAL_ERROR` cho dữ liệu chính của route; hoặc ngoại lệ khi render (error boundary); hoặc lỗi tải chunk lần thứ hai | `ServerErrorPage` |

Cả bốn component trang đều là lớp mỏng quanh `ErrorPage` dùng chung (DOC-39 §7.18, `frontend/src/components/ErrorPage.tsx`); các trang nằm ở `frontend/src/app/errors/` (DR-139). Mỗi trang đặt `<title>` dạng `{tiêu đề trang} · {APP_NAME}` và `<meta name="robots" content="noindex">`.

Về mã HTTP thật: nginx trả `index.html` với **200** cho mọi đường dẫn SPA (DOC-62), nên "404" của E1 chỉ là trang hiển thị chứ không phải mã của response. Chỉ các đường dẫn `/api/**` và `/media/**` mới có mã HTTP thật (DOC-36). Hệ thống không tối ưu công cụ tìm kiếm nên chấp nhận.

Khi **đã có nội dung** (ví dụ đang xem sơ đồ, polling `availability` lỗi một lần, đồng hồ giữ vé đang chạy) thì **không** thay cả trang bằng E4/E5: hiện thông báo trong khung (`Notice` `warning`, DOC-40 §1.4) và giữ nguyên nội dung (UXP-05).

## 3. Wireframe

Mọi trang dùng một thẻ 584 px căn giữa dưới header (canvas `E1`…`E5`). Cấu trúc từ trên xuống: nhãn mono + con dấu mã (xoay +4°) · tiêu đề · câu giải thích · (E4: thanh tự thử lại) · (E5: hộp Mã yêu cầu) · hàng nút · đường xé · chân thẻ.

E1, desktop:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.                                     [vi|en]   Vé của tôi         │
├──────────────────────────────────────────────────────────────────────────┤
│                  ┌───────────────────────────────────┐                   │
│                  │ KHÔNG TÌM THẤY             ╭─────╮│                   │
│                  │                            │ 404 ││  con dấu +4°      │
│                  │                            ╰─────╯│                   │
│                  │ TRANG NÀY KHÔNG TỒN TẠI           │                   │
│                  │ Đường dẫn có thể đã gõ sai, ...   │                   │
│                  │ [ Xem các sự kiện ]  Vé của tôi   │                   │
│                  │ ◖┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄◗  │                   │
│                  │ Nếu bạn vào từ một đường dẫn ...  │                   │
│                  └───────────────────────────────────┘                   │
└──────────────────────────────────────────────────────────────────────────┘
```

E4, điện thoại 390 px (thẻ tràn ngang trừ lề 16 px, `h1` 32 px, nút xuống hàng):

```text
┌────────────────────────┐
│ ticket.        [vi|en] │
│ ┌────────────────────┐ │
│ │ ĐANG ĐÔNG    ╭────╮│ │
│ │              │503 ││ │
│ │              ╰────╯│ │
│ │ HỆ THỐNG ĐANG RẤT  │ │
│ │ ĐÔNG               │ │
│ │ Trang sẽ tự thử    │ │
│ │ lại, ...           │ │
│ │ Tự thử lại sau 8 g.│ │
│ │ ▓▓▓▓░░░░░░░░░░░░░░ │ │
│ │ [ Thử lại ngay   ] │ │
│ │ Về danh sách sự kiện│ │
│ │ ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄ │ │
│ │ Đồng hồ giữ vé ... │ │
│ └────────────────────┘ │
└────────────────────────┘
```

E5 thêm hộp `Mã yêu cầu  9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b` giữa câu giải thích và hàng nút; E3 thêm nhãn `Studio` cạnh logo.

## 4. Vùng và component

| Vùng | Component | Ghi chú |
| --- | --- | --- |
| Header | logo, `LanguageSwitch` (§7.19), liên kết "Vé của tôi" (E1, E4, E5), nhãn `Studio` (E3) | **Không phụ thuộc dữ liệu**: hiện được khi mọi API lỗi. Menu tài khoản chỉ hiện nếu người dùng đã có trong cache TanStack Query; không gọi API để dựng header lỗi |
| Thẻ | `ErrorPage` (§7.18): thẻ trắng 584 px, bo 6 px, đường xé, `padding 28 px` | `role="alert"` ở E1, E3, E5; `role="status"` ở E4 (cảnh báo tạm, không giật tiêu điểm) |
| Con dấu mã | khung 2 px bo 4 px, chữ mono 22 px, xoay +4°, `aria-hidden` | E1/E3/E5: màu `--stamp`/`--stamp-deep`; E4: bộ `--warn-bd`/`--warn-fg`. Chữ trong con dấu là mã HTTP của response lỗi gần nhất (503 hoặc 429); khi là lỗi mạng (không có response) hiện `···` (DR-140). Hoạt ảnh con dấu bị tắt khi `prefers-reduced-motion` |
| Thanh tự thử lại (E4) | dòng mono 14 px + thanh 4 px | §6.2 |
| Hộp Mã yêu cầu (E5) | nhãn mono 11 px + giá trị mono 15 px, `user-select: all` | Hiện giá trị **đầy đủ** 32 ký tự hex của `X-Request-Id`/`requestId` (DOC-35 §7), không rút gọn (canvas rút gọn 12 ký tự vì là ví dụ); xuống dòng bằng `overflow-wrap: anywhere` |
| Hàng nút | `Button primary` 52 px hoặc `Button text` 44 px | Một nút chính mỗi trang (UXP-01) |
| Chân thẻ | câu 14 px `--ink-2` | Luôn có |

## 5. Dữ liệu

| Trang | Dữ liệu | Nguồn |
| --- | --- | --- |
| E1 | Không có | — |
| E3 | `{email}` của người đang đăng nhập | `GET /me` (E-04) từ cache; thiếu cache thì gọi một lần, lỗi thì bỏ câu "Bạn đang đăng nhập bằng …" |
| E4 | Mã trạng thái, `Retry-After`/`retryAfterSeconds` của response lỗi gần nhất | Request đã lỗi (không gọi thêm endpoint riêng) |
| E5 | `requestId` | Thân Problem Details của response 500 (DOC-36 §5); với ngoại lệ phía client không có → ẩn hộp |

Không có kênh real-time. Nhịp làm mới chỉ có ở E4 (§6.2).

## 6. Tương tác

### 6.1 E1, E3, E5

| Trang | Thao tác | Kết quả |
| --- | --- | --- |
| E1 | "Xem các sự kiện" (`errors.page.notFound.cta`) | `navigate("/")` |
| E1 | "Vé của tôi" (`common.nav.myTickets`) | `/me/tickets` (chưa đăng nhập thì `/login?returnTo=%2Fme%2Ftickets`) |
| E3 | "Về Sự kiện của bạn" (`errors.page.forbidden.cta`) | `/studio` |
| E3 | "Đăng nhập tài khoản khác" (`errors.page.forbidden.other`) | `POST /auth/logout` (E-03) rồi `/login?returnTo=%2Fstudio&reason=signed_out` |
| E5 | "Thử lại" (`common.retry`) | Lỗi từ API: `refetch()` truy vấn lỗi; lỗi render: `window.location.reload()`. Trạng thái nút `aria-busy` trong lúc chờ |
| E5 | "Về danh sách sự kiện" (`common.nav.toEvents`) | `navigate("/")` |

Lỗi tải chunk (`ChunkLoadError`, thường do bản mới đã thay tên file băm): lần đầu **tự `window.location.reload()` một lần** (cờ `sessionStorage["tb.chunkReload"]`); lần hai trong cùng phiên → E5 không hộp mã (DR-140).

### 6.2 E4: tự thử lại

Quy tắc ở DOC-38 §8.2; tài liệu này áp dụng cho trang toàn trang.

1. Chờ `wait = clamp(Retry-After, 1, 32)` giây nếu response có `Retry-After`; không thì 8, rồi 16, rồi 32 giây và giữ 32.
2. Dòng trạng thái đếm ngược `common.retry.auto` ("Tự thử lại sau {seconds} giây"), thanh tiến trình đầy dần theo `(wait − còn lại) / wait`. Về 0 → `common.retry.running` ("Đang thử lại…"), thanh đầy, gọi lại **đúng truy vấn đã lỗi** (`refetch`).
3. Thành công → E4 biến mất và route hiện nội dung, tiêu điểm vào `h1` của trang. Thất bại → vòng mới với `wait` mới (§1).
4. "Thử lại ngay" (`common.retryNow`) bỏ phần chờ còn lại, không đổi `wait` của vòng sau.
5. Tối đa 10 lần liên tiếp cho một truy vấn; sau đó dừng tự thử, ẩn thanh, nút đổi thành "Thử lại" (`common.retry`) bằng tay. Tab ẩn (`document.hidden`) thì tạm dừng đếm; hiện lại thì tiếp tục.
6. Liên kết "Về danh sách sự kiện" (`common.nav.toEvents`) dẫn về `/`; rời trang hủy các lần thử.
7. Chỉ truy vấn **đọc** hoặc thao tác có `Idempotency-Key` (giữ vé, hủy giữ vé; DR-45) được tự thử lại; POST khác dừng ở lỗi trong khung (DOC-38 §8.2).

Vé đã giữ không bị ảnh hưởng và đồng hồ vẫn chạy theo giờ server (DR-66): chân thẻ `errors.page.busy.foot` nói rõ điều này.

## 7. Trạng thái

| Trang | Biến thể canvas | Nhãn · tiêu đề (key) | Bộ màu |
| --- | --- | --- | --- |
| E1 | `E1` | `errors.page.notFound.label`, `.title` | `--stamp` |
| E2 | `E2` | Xem DOC-44 §7 biến thể 7 | `--warn-*` |
| E3 | `E3` | `errors.page.forbidden.label`, `.title` | `--stamp` |
| E4 | `E4` | `errors.page.busy.label`, `.title` | `--warn-*` |
| E5 | `E5` | `errors.page.server.label`, `.title` | `--stamp` |

Mỗi trang chỉ có một trạng thái; riêng E4 có trạng thái con: `đếm ngược` · `đang thử lại` · `đã dừng sau 10 lần` (§6.2). E5 có hai trạng thái con: `có mã yêu cầu` và `không có mã yêu cầu` (lỗi phía client). E3 có hai: `có email` và `không có email` (thiếu `GET /me`).

Thứ tự tiêu điểm: khi trang lỗi hiện, tiêu điểm vào `h1` (`tabindex="-1"`; DOC-39 §9). `Esc` không làm gì. Người dùng đọc màn hình nghe `role="alert"` (E1, E3, E5) hoặc `role="status"` (E4).

## 8. Microcopy

Chuỗi đầy đủ ở [DOC-40 §3.7](../ui-states-and-copy.md); không chép lại.

| Trang | Key |
| --- | --- |
| E1 | `errors.page.notFound.code`, `.label`, `.title`, `.body`, `.cta`, `.foot` |
| E3 | `errors.page.forbidden.label`, `.title`, `.body` (`{email}`), `.cta`, `.other`, `.foot` |
| E4 | `errors.page.busy.label`, `.title`, `.body`, `.foot`; `common.retry.auto`, `common.retry.running`, `common.retryNow`, `common.retry`, `common.nav.toEvents` |
| E5 | `errors.page.server.label`, `.title`, `.body`, `.requestId`, `.foot`; `common.retry`, `common.nav.toEvents` |
| Chung | `common.nav.myTickets` |

Không thiếu key nào so với DOC-40. E4 dùng lại `errors.page.busy.*` cho cả lỗi 429 và lỗi mạng; không có chuỗi riêng cho lỗi mạng.

## 9. Tiêu chí nghiệm thu

Tiền tố `ERP-` (trang lỗi).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| ERP-01 | Đường dẫn `/khong-co-trang-nay` | Mở trang | Hiện E1 với `errors.page.notFound.*`, con dấu `404`; `<title>` có "Trang này không tồn tại"; URL giữ nguyên |
| ERP-02 | `/events/abc` (không phải UUID) | Mở trang | Hiện E1; không có request `GET /events/abc` |
| ERP-03 | `/events/<uuid hợp lệ không tồn tại>` | Mở trang | API trả 404 `NOT_FOUND` → E1 (cùng nội dung với ERP-01) |
| ERP-04 | Tổ chức A, người dùng của tổ chức B | Mở `/studio/events/<eventId của A>/info` | API trả 404 → E1 trong khung Studio; **không** có câu nào tiết lộ sự kiện thuộc tổ chức khác |
| ERP-05 | API trả 403 `FORBIDDEN` ở `/studio/events/<id>/info` | Mở trang | Hiện E3, nhãn `Studio` ở header, thân có `{email}` của người đăng nhập; nút "Về Sự kiện của bạn" tới `/studio` |
| ERP-06 | API trả 403 `ORGANIZER_PROFILE_REQUIRED` ở `/studio` | Mở trang | **Không** hiện E3; chuyển tới `/studio/profile` bằng `replace` |
| ERP-07 | E3 đang hiện | Bấm "Đăng nhập tài khoản khác" | `POST /auth/logout` rồi tới `/login?returnTo=%2Fstudio&reason=signed_out` |
| ERP-08 | Tải `/` lần đầu, API trả 503 `OVERLOADED` với `Retry-After: 6` | Mở trang | Hiện E4, con dấu `503`; dòng "Tự thử lại sau 6 giây" đếm 6 → 1; sau đó đúng một request lặp lại |
| ERP-09 | Như ERP-08, không có `Retry-After` | Quan sát 3 vòng thất bại | Chờ 8, 16, 32 giây; vòng thứ 4 chờ 32 giây |
| ERP-10 | `Retry-After: 120` | Quan sát | Chờ 32 giây (kẹp trên); `Retry-After: 0` chờ 1 giây (kẹp dưới) |
| ERP-11 | E4 hiện | Bấm "Thử lại ngay" | Request lặp lại ngay; vòng sau vẫn dùng `wait` cũ |
| ERP-12 | 10 lần liên tiếp thất bại | Quan sát | Thanh tiến trình biến mất; nút hiện "Thử lại" (không "ngay"); không còn tự thử |
| ERP-13 | E4 hiện, tab chuyển sang nền 20 giây | Quay lại | Đồng hồ không trôi trong lúc tab ẩn; tiếp tục từ giá trị còn lại |
| ERP-14 | API trả 429 `RATE_LIMITED` | Mở trang | E4 với con dấu `429`; cùng nội dung `errors.page.busy.*` |
| ERP-15 | Mất mạng (request bị hủy, không có response) | Mở trang | E4 với con dấu `···`; tự thử lại 8, 16, 32 giây; khi có mạng lại thì tự khôi phục |
| ERP-16 | Có nội dung đang hiện (ví dụ sơ đồ), `GET availability` trả 503 một lần | Quan sát | **Không** thay trang bằng E4; `Notice` `warning` trong khung; nội dung vẫn thấy |
| ERP-17 | API trả 500 `INTERNAL_ERROR`, `requestId` `9f2c1d7e4b3a4e6f8a0b1c2d3e4f5a6b` | Mở trang | Hiện E5; hộp "Mã yêu cầu" có đủ 32 ký tự; nhấp ba lần chọn trọn giá trị (`user-select: all`) |
| ERP-18 | Component ném lỗi khi render | Quan sát | Error boundary hiện E5 **không** có hộp mã; nút "Thử lại" gọi `window.location.reload()` |
| ERP-19 | `import()` của chunk `seat-picker` thất bại | Lần 1 | Tự tải lại trang một lần; lần 2 trong cùng phiên → E5 |
| ERP-20 | Mọi trang lỗi, 390 px | Quan sát | Không cuộn ngang; con dấu không che tiêu đề; mọi nút cao ≥ 44 px |
| ERP-21 | Mọi trang lỗi | Dùng bàn phím và `axe` | Tiêu điểm vào `h1` khi trang hiện; thứ tự Tab theo thứ tự đọc; không lỗi `axe`; tương phản ≥ 4,5:1 (đặc biệt `--warn-fg` trên nền trắng) |
| ERP-22 | `prefers-reduced-motion: reduce` | Mở E4 | Thanh tiến trình đổi theo bước 1 giây không chuyển động mượt; con dấu không hoạt ảnh |
| ERP-23 | Ngôn ngữ `en` | Mở E1…E5 | Mọi chuỗi ở `en`; không có key thô |
| ERP-24 | API sập hoàn toàn (mọi request lỗi), `GET /me` lỗi | Mở E1 | Header vẫn hiện (logo, `LanguageSwitch`); trang lỗi không gọi API nào để dựng |

## 10. Ca kiểm thử E2E

Playwright; trạng thái lỗi dựng bằng `page.route` (chặn và trả Problem Details mẫu của DOC-35 §4) và `page.clock` để điều khiển đếm ngược.

| Tên | Bước | Kỳ vọng |
| --- | --- | --- |
| `error-404-unknown-route` | Mở đường dẫn lạ; bấm "Xem các sự kiện" | ERP-01; về `/` |
| `error-404-foreign-studio-event` | Đăng nhập tổ chức B, mở sự kiện của A | ERP-04 |
| `error-403-studio` | Chặn `GET /organizer/events` trả 403 `FORBIDDEN` | ERP-05, ERP-07 |
| `error-503-autoretry` | Chặn `GET /events` trả 503 `Retry-After: 3` hai lần rồi 200; dùng `page.clock` | ERP-08; sau lần thành công danh sách hiện và tiêu điểm ở `h1` |
| `error-503-give-up` | Chặn luôn 503 | ERP-12 |
| `error-offline` | `context.setOffline(true)` rồi tắt | ERP-15 |
| `error-500-request-id` | Chặn trả 500 kèm `requestId` | ERP-17 |
| `error-boundary` | Dựng route thử ném lỗi (chỉ ở build `e2e`) | ERP-18 |
| `error-chunk-reload` | Chặn request chunk trả 404 | ERP-19 |
| `error-pages-a11y` | Mở E1, E3, E4, E5 ở 390 px và 1280 px; chạy `axe` | ERP-20, ERP-21 |

## 11. Câu hỏi còn mở

Không có. Quyết định mới: `DR-139` (E2 là trạng thái của `Login`; vị trí component trang lỗi) và `DR-140` (điều kiện hiện E3, con dấu khi lỗi mạng, tải lại khi lỗi chunk), nội dung ở DOC-41 §7.
