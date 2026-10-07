# Bảo mật

> Trạng thái: **Approved** · Cập nhật: 2026-10-07 · DOC-32
> Phụ thuộc: SDD gốc §5, §9.3, §12, §14.1, [DOC-06](../02-glossary.md), [DOC-07](../03-architecture/system-context-and-containers.md) §5, [DOC-15](../05-data/ops-model.md), [DOC-35](error-handling.md), [DOC-36](../07-api/api-guidelines.md), [DOC-62](../09-operations/deploy-compose.md) §8, [DOC-63](../09-operations/ci-cd.md) §9, [Sổ quyết định](../00-decision-register.md) (DR-21, 22, 23, 38, 48, 51, 55, 56, 58, 61, 64, 65), [ADR-0007](../04-adr/0007-magic-link-server-sessions.md)
> Người dùng chính: P1-07 (auth), P1-02 (nginx), mọi task viết endpoint, [DOC-19](auth-and-sessions.md), DOC-37, DOC-69, người review PR (checklist [DOC-12](../03-architecture/code-architecture.md) §7)

Tài liệu gom mọi quyết định bảo mật vào một chỗ: ai được gọi endpoint nào, request ghi được bảo vệ ra sao, secret nằm đâu, nginx đặt header gì, và ba ranh giới tin cậy bị tấn công theo cách nào. Cơ chế magic link và session chi tiết nằm ở DOC-19; mã lỗi ở DOC-35; cấu hình nginx chạy được ở DOC-62; tên key cấu hình ở DOC-34. Hệ thống ở giai đoạn này chạy ở chế độ thử nghiệm (Stripe test mode, một máy); mục 9 nói rõ những gì **chưa** làm.

## 1. Xác thực và phiên

Tóm tắt (nguồn chuẩn ở DOC-19, DR-21, DR-22):

| Mục | Quy tắc |
| --- | --- |
| Cách đăng nhập | Magic link gửi email; token 32 byte `SecureRandom`, DB chỉ lưu SHA-256, hiệu lực 15 phút, dùng một lần, link mới thay link cũ (DR-21) |
| Session | Cookie `tb_session` 32 byte base64url; DB chỉ lưu SHA-256 (`session.session_hash`); `HttpOnly; SameSite=Lax; Secure` (`auth.cookie-secure`, `false` chỉ ở profile `dev`); hết hạn sau 30 ngày không hoạt động (DR-22) |
| Cố định phiên (session fixation) | Mỗi lần `POST /auth/verify` thành công sinh session **mới**; không bao giờ tái dùng giá trị `tb_session` có sẵn trong request |
| Liệt kê tài khoản | `POST /auth/magic-link` luôn trả 202 với email hợp lệ, kể cả khi vượt giới hạn (header `X-Magic-Link-Throttled`), nên không lộ email đã có tài khoản hay chưa |
| Đăng nhập chéo thiết bị | Không có: trình duyệt mở link là trình duyệt có session (DR-21 mục 5) |
| `return_to` | Chỉ nhận chuỗi bắt đầu bằng `/`, không bắt đầu bằng `//` hoặc `/\`, dài ≤ 512; sai thì dùng `/` (chống open redirect, DR-21 mục 6) |
| Đăng xuất | Đặt `session.revoked_at`, xóa mục khỏi cache Caffeine (một bản sao duy nhất); request kế tiếp 401 `UNAUTHENTICATED` |

Hai nơi dùng so sánh hằng thời gian (`MessageDigest.isEqual`): so khớp `X-CSRF-Token` với `session.csrf_token`, và so hash khi tra token. Tra token đăng nhập và session bằng khóa chính là hash, không so chuỗi thô.

## 2. Vai trò và phân quyền

### 2.1 Vai trò

| Vai trò | Cách có | Quyền |
| --- | --- | --- |
| Khách | Chưa có session | Đọc công khai (danh sách, chi tiết, sơ đồ, tình trạng chỗ, ảnh); xin và đổi magic link |
| Người mua | Có session hợp lệ (`roles` có `BUYER`) | Mọi quyền của khách; phòng chờ, giữ vé, thanh toán, xem đơn và vé **của chính mình** |
| Người tổ chức | Có dòng `organizer` với `owner_user_id` = người dùng (`roles` có `ORGANIZER`, DR-23) | Mọi quyền của người mua; mọi endpoint `/organizer/**` với tài nguyên **của tổ chức mình** |
| Stripe | Chữ ký `Stripe-Signature` hợp lệ (không có session) | Chỉ `POST /webhooks/stripe` |

Không có vai trò `ADMIN`. Việc của người vận hành (hoàn tiền, chạy thực nghiệm, `make invariants`) làm bằng Stripe Dashboard, `psql` và lệnh `make` trên máy chủ, không qua API (DOC-07 §2).

### 2.2 Quy tắc sở hữu (DR-23, DR-116)

1. `organizer_id` luôn lấy từ session ở server, không bao giờ từ path, query hay body. Mọi truy vấn tài nguyên của tổ chức thêm `AND organizer_id = :sessionOrg`.
2. Tài nguyên của tổ chức khác, hoặc không tồn tại → **404** `NOT_FOUND` (không lộ tồn tại). Tài khoản chưa có hồ sơ gọi `/organizer/**` (trừ `POST /organizer`) → **403** `ORGANIZER_PROFILE_REQUIRED`; màn E3 hiện cho cả hai trường hợp.
3. Tài nguyên của người mua (`reservation`, `orders`, `ticket`) lọc `AND user_id = :sessionUser`. Của người khác → **404** `NOT_FOUND` (⚠ khác SDD gốc 12.3 ghi 403 cho "không sở hữu tài nguyên"; lý do ở DR-116: một quy tắc cho mọi tài nguyên, không lộ ID đơn nào tồn tại). `403 FORBIDDEN` còn lại chỉ dành cho vi phạm quyền không gắn với một tài nguyên cụ thể.
4. Kiểm tra sở hữu nằm ở **tầng service** (câu truy vấn có điều kiện), không ở controller; test `SEC-05…08` phủ từng nhóm.

### 2.3 Ma trận endpoint × vai trò

Ký hiệu: ✔ cho phép; — không cho phép (mã lỗi trong ngoặc); **C** cần `X-CSRF-Token`; **I** cần `Idempotency-Key` (DR-45); **own** lọc sở hữu như §2.2. Đường dẫn đều sau tiền tố `/api/v1` (DR-63) trừ `/media/{id}`. Mã `E-xx` của từng dòng nằm ở DOC-37; bảng này chỉ định nghĩa quyền, và DOC-37 phải khớp (test `SEC-01`).

| Endpoint | Khách | Người mua | Người tổ chức (chủ) | Stripe | Ghi chú |
| --- | --- | --- | --- | --- | --- |
| `POST /auth/magic-link` | ✔ | ✔ | ✔ | — | Không cần CSRF (chưa có session); nginx `auth_ip` 10 r/phút; 3/email/15 phút và 10/IP/giờ trong DB (DR-21) |
| `POST /auth/verify` | ✔ | ✔ | ✔ | — | Không cần CSRF; sai/hết hạn/đã dùng → 401 `LOGIN_LINK_INVALID` |
| `POST /auth/logout` | — (401) | ✔ **C** | ✔ **C** | — | |
| `GET /me` | — (401) | ✔ | ✔ | — | Trả `csrfToken`; response `Cache-Control: no-store` |
| `PATCH /me` | — (401) | ✔ **C** | ✔ **C** | — | Chỉ sửa `locale` |
| `GET /events` | ✔ | ✔ | ✔ | — | Chỉ sự kiện `displayStatus` công khai (không `DRAFT`) |
| `GET /events/{id}` | ✔ | ✔ | ✔ | — | `DRAFT` hoặc không tồn tại → 404, kể cả với chủ sự kiện (chủ xem bản nháp qua `/organizer/events/{id}`) |
| `GET /events/{id}/map` | ✔ | ✔ | ✔ | — | Chỉ phiên bản đã xuất bản của sự kiện công khai |
| `GET /events/{id}/availability` | ✔ | ✔ | ✔ | — | Không có dữ liệu cá nhân |
| `GET /media/{id}` | ✔ | ✔ | ✔ | — | Công khai; ID là UUIDv7 khó đoán nhưng **không** là bí mật |
| `POST /events/{id}/queue` | — (401) | ✔ **C** | ✔ **C** | — | Bucket `rl:queue:{userId}` (DR-56) |
| `GET /events/{id}/queue` | — (401) | ✔ | ✔ | — | |
| `DELETE /events/{id}/queue` | — (401) | ✔ **C** | ✔ **C** | — | |
| `POST /events/{id}/reservations` | — (401) | ✔ **C I** | ✔ **C I** | — | `hold_ip` 5 r/s; `rl:hold:{userId}`; lượt vào kiểm bằng `ZSCORE` (DR-58) → 429 `QUEUE_REQUIRED`; bulkhead (DR-61) |
| `GET /reservations/{id}` | — (401) | ✔ own | ✔ own | — | |
| `DELETE /reservations/{id}` | — (401) | ✔ **C** own | ✔ **C** own | — | |
| `POST /orders/{id}/payment-intent` | — (401) | ✔ **C I** own | ✔ **C I** own | — | `rl:pi:{userId}` |
| `POST /orders/{id}/confirm-free` | — (401) | ✔ **C I** own | ✔ **C I** own | — | Chỉ đơn 0 đồng |
| `GET /orders/{id}` | — (401) | ✔ own | ✔ own | — | Kèm vé khi `PAID` |
| `GET /me/orders`, `GET /me/tickets` | — (401) | ✔ | ✔ | — | Luôn lọc `user_id` của session |
| `POST /webhooks/stripe` | — | — | — | ✔ (chữ ký) | Không session, không CSRF (DR-22); sai chữ ký → 400 `INVALID_SIGNATURE` |
| `POST /organizer` | — (401) | ✔ **C** | — (409 `ORGANIZER_EXISTS`) | — | Người tổ chức đã có hồ sơ |
| `GET /organizer/events` | — (401) | — (403 `ORGANIZER_PROFILE_REQUIRED`) | ✔ | — | Chỉ sự kiện của tổ chức |
| `POST /organizer/events` | — (401) | — (403) | ✔ **C** | — | |
| `GET /organizer/events/{id}` | — (401) | — (403) | ✔ own | — | Tài nguyên tổ chức khác → 404 |
| `PATCH /organizer/events/{id}` | — (401) | — (403) | ✔ **C** own | — | |
| `GET /organizer/events/{id}/publish-checks` | — (401) | — (403) | ✔ own | — | |
| `POST /organizer/events/{id}/publish`, `/pause`, `/resume`, `/close-sale`, `/cancel` | — (401) | — (403) | ✔ **C** own | — | |
| `POST`, `PATCH`, `DELETE /organizer/events/{id}/ticket-types[/{typeId}]` | — (401) | — (403) | ✔ **C** own | — | `typeId` phải thuộc sự kiện đó |
| `GET /organizer/events/{id}/sales`, `/seat-status` | — (401) | — (403) | ✔ own | — | |
| `GET /organizer/events/{id}/map` | — (401) | — (403) | ✔ own | — | |
| `GET /organizer/maps` | — (401) | — (403) | ✔ | — | Chỉ sơ đồ của tổ chức |
| `POST /organizer/maps` | — (401) | — (403) | ✔ **C** | — | |
| `GET /organizer/maps/{id}`, `GET /organizer/maps/{id}/versions` | — (401) | — (403) | ✔ own | — | |
| `PUT /organizer/maps/{id}/draft` | — (401) | — (403) | ✔ **C** own | — | `revision` (409 `REVISION_CONFLICT`); body ≤ 6 MB nginx, ≤ 2 MB tài liệu (DR-32) |
| `POST /organizer/maps/{id}/validate`, `/publish`, `/clone` | — (401) | — (403) | ✔ **C** own | — | `clone` chỉ nguồn thuộc tổ chức |
| `POST /organizer/media` | — (401) | — (403) | ✔ **C** | — | Xem §7 |
| `GET /actuator/**` (cổng 9090) | không qua nginx | không qua nginx | không qua nginx | — | Chỉ trong mạng compose (§8) |

Mặc định chặn: `SecurityFilterChain` cấu hình `anyRequest().denyAll()` sau danh sách cho phép tường minh, nên endpoint mới quên khai quyền sẽ trả 403 chứ không mở toang (test `SEC-02`).

## 3. CSRF

Theo DR-22 (synchronizer token):

- `session.csrf_token` sinh lúc tạo session; client lấy từ body `POST /auth/verify` hoặc `GET /me`, giữ **trong bộ nhớ** (không `localStorage`, không cookie đọc được), gửi `X-CSRF-Token` cho mọi `POST/PUT/PATCH/DELETE`.
- Miễn: `POST /webhooks/stripe` (xác thực bằng chữ ký), `POST /auth/magic-link`, `POST /auth/verify` (chưa có session).
- Thiếu hoặc sai → 403 `CSRF_TOKEN_INVALID` (DR-82); client gọi `GET /me` lấy token mới và thử lại **một** lần (DR-85).
- Lớp thứ hai: `SameSite=Lax` chặn cookie ở POST chéo trang; nginx không cho `Origin` lạ — request ghi có `Origin` khác `APP_BASE_URL` bị bộ lọc `OriginCheckFilter` từ chối 403 `CSRF_TOKEN_INVALID` (DR-117). Request không có `Origin` (curl, k6) chỉ cần CSRF token.

Ví dụ request ghi hợp lệ:

```http
POST /api/v1/events/0192f7c0-1c3e-7a10-8d52-3f6b1c9a0b11/reservations HTTP/1.1
Cookie: tb_session=Zk3…(43 ký tự)
X-CSRF-Token: 8c1f0a52d9b44e0f9c63a1e2b7d05f44
Idempotency-Key: 6f1d2c4e-9a3b-4d77-8e10-5b2f0c9a7d31
Origin: http://localhost:8080
Content-Type: application/json
```

## 4. CORS

Tắt. SPA và API cùng origin (nginx phục vụ cả hai, `/api` proxy tới `api:8081`), nên không có `Access-Control-Allow-*` nào; Spring không đăng ký `CorsConfigurationSource`. `OPTIONS` từ origin khác nhận 405 `METHOD_NOT_ALLOWED`. Ở `make dev` (Vite trên 5173 gọi API qua proxy của Vite) vẫn cùng origin từ góc nhìn trình duyệt. Test `SEC-10`.

## 5. Rate limit và chống lạm dụng

Ba lớp, mỗi lớp một nguồn chuẩn; bảng này chỉ tổng hợp để rà lỗ hổng:

| Lớp | Cơ chế | Nguồn chuẩn | Chống |
| --- | --- | --- | --- |
| Edge | nginx `limit_req`: `api_ip` 20 r/s (burst 40), `auth_ip` 10 r/phút (burst 5), `hold_ip` 5 r/s (burst 10); 429 `RATE_LIMITED`; `RATE_LIMIT_ALLOWLIST` miễn máy sinh tải | DR-55, DOC-62 §8 | Quét, brute force, tràn request từ một IP |
| Người dùng | Token bucket Lua trong Redis: giữ vé 5 + 1/2 giây; PaymentIntent 5 + 1/5 giây; hỏi vị trí 10 + 1/giây; mất Redis thì bỏ qua (không có limiter dự phòng) | DR-56, DOC-28 | Một tài khoản gom vé; spam |
| Nghiệp vụ trong DB | Magic link 3/email/15 phút và 10/IP/giờ (đếm trên `login_token`); `Idempotency-Key`; một reservation `ACTIVE` mỗi người mỗi sự kiện (`reservation_open_uq`) | DR-21, DR-45, DOC-14 | Spam email, giữ vé trùng |

Giới hạn số vé mỗi người **không** có (DR-41); chống đầu cơ dựa vào bucket, phòng chờ và một reservation mỗi người. Bulkhead của lệnh giữ vé (24 permit, DR-61) là lớp bảo vệ cuối khi Redis mất. IP lấy từ `X-Forwarded-For` do nginx đặt (`real_ip`); `api` chỉ tin header này khi request đến từ mạng compose, và `requested_ip` trong `login_token` lưu IP đó.

## 6. Dữ liệu nhạy cảm

| Dữ liệu | Quy tắc |
| --- | --- |
| Thẻ | Không bao giờ qua `api` (Payment Element gửi thẳng Stripe, NFR-07); `api` chỉ giữ `payment_intent_id`. CSP `frame-src` chỉ `js.stripe.com`, `hooks.stripe.com` |
| Token đăng nhập, session | Chỉ lưu SHA-256; token thô chỉ trong email và bộ nhớ của request (DR-21) |
| Email người dùng | Không vào log, không vào `detail` của lỗi, không vào tên metric hay label (DR-22); log dùng `user_id`. Payload outbox có email để gửi và bị xóa theo DR-74 |
| Body request | Không log body (có thể chứa token hoặc secret) |
| Secret | Mục 7.1 |
| Mã vé | Ngẫu nhiên không đoán được (DR-52); lộ mã không đủ để vào cửa ở giai đoạn này vì chưa có luồng soát vé |

## 7. Secret, upload và cấu hình an toàn

### 7.1 Secret theo môi trường

Mọi secret nằm trong `deploy/compose/.env` (đã `.gitignore`); `.env.example` chỉ có giá trị giả hoặc để trống (DOC-62 §6). Không secret nào ở `application.yml`, image hay log.

| Secret | Dùng bởi | Ghi chú |
| --- | --- | --- |
| `STRIPE_SECRET_KEY` (`sk_test_…`) | `api` → Stripe | `api` thoát khi khởi động nếu `PAYMENTS_MODE=fake` mà khóa bắt đầu `sk_live_` (DR-51, DR-125) |
| `STRIPE_WEBHOOK_SECRET` (`whsec_…`) | Xác minh webhook | |
| `STRIPE_PUBLISHABLE_KEY` | Frontend | Công khai, không phải secret |
| `POSTGRES_PASSWORD`, `SPRING_DATASOURCE_PASSWORD` (Redis không có mật khẩu ở dev) | `api` | Postgres và Redis không mở cổng ra host ngoài `make dev` |
| `STORAGE_S3_ACCESS_KEY`, `STORAGE_S3_SECRET_KEY` | `api` → SeaweedFS | Bucket private, không presigned URL (DR-38) |
| `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` | `api` → SMTP | Mailpit ở dev không cần |

CI không có secret nào ngoài `GITHUB_TOKEN` (DOC-63 §1). `scripts/check-secrets.sh` (chạy trong `make lint`) chặn commit chứa `sk_test_|sk_live_|whsec_` thật. Đổi secret = sửa `.env` rồi `docker compose up -d`; chưa có luân chuyển tự động.

### 7.2 Upload ảnh (DR-38)

`POST /organizer/media` kiểm theo thứ tự, lỗi đầu tiên trả 422 `MEDIA_INVALID` (hoặc 413 `PAYLOAD_TOO_LARGE` nếu vượt `client_max_body_size 6m` của nginx):

1. `Content-Type` multipart; `purpose` ∈ {`EVENT_IMAGE`, `FLOOR_PLAN`}.
2. Kích thước ≤ 2 MB (`EVENT_IMAGE`) hoặc ≤ 5 MB (`FLOOR_PLAN`).
3. **Magic bytes** khớp JPEG, PNG hoặc WebP; bỏ qua `Content-Type` client gửi. SVG bị từ chối (có thể chứa script).
4. `ImageIO` đọc được và mỗi cạnh ≤ 8000 px.
5. Tên file client gửi không dùng; `object_key = media/<media_id>`.

Phục vụ lại luôn với `Content-Type` lấy từ dòng `media` (không từ object) và `X-Content-Type-Options: nosniff`, nên ảnh không thể bị trình duyệt hiểu thành HTML.

### 7.3 Dữ liệu do người tổ chức nhập

Văn bản (tên sự kiện, mô tả, địa điểm, nhãn sơ đồ, tên tổ chức) là dữ liệu, **không dịch và không là HTML**: React escape khi hiển thị, không dùng `dangerouslySetInnerHTML`; email Thymeleaf dùng `th:text` (escape), không `th:utext`. ESLint `react/no-danger` ở mức `error`. Test `SEC-14`.

## 8. Header bảo mật và bề mặt mạng

Do nginx đặt (cấu hình đầy đủ ở DOC-62 §8; tài liệu này chốt **giá trị**):

| Header | Giá trị | Mục đích |
| --- | --- | --- |
| `Content-Security-Policy` | `default-src 'self'; script-src 'self' https://js.stripe.com; frame-src https://js.stripe.com https://hooks.stripe.com; connect-src 'self' https://api.stripe.com; img-src 'self' data: https://*.stripe.com; style-src 'self' 'unsafe-inline'; font-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'` | Chặn script lạ; cho phép Stripe |
| `X-Content-Type-Options` | `nosniff` | Chặn đoán MIME |
| `Referrer-Policy` | `strict-origin-when-cross-origin`; riêng `/auth/callback` là `no-referrer` (DR-99) | Không lộ `returnTo`/query sang bên ngoài; token magic link không vào `Referer` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=()` | Tắt API không dùng |
| `Strict-Transport-Security` | `max-age=31536000` | Chỉ khi có TLS (không ở dev) |
| `Cache-Control: no-store` | Với `GET /me`, mọi `/auth/*`, `/organizer/**`, `/me/**`, `/orders/**`, `/reservations/**` | Không cache dữ liệu cá nhân ở trình duyệt hay nginx |

Ghi chú: `style-src 'unsafe-inline'` cần cho Stripe Elements; thử siết bằng hash/nonce khi làm P3-08 (DOC-62 câu hỏi mở). `frame-ancestors 'none'` thay cho `X-Frame-Options`. Cookie `tb_lang` cố ý không `HttpOnly` (i18next đọc, DOC-31) và không chứa dữ liệu nhạy cảm.

Bề mặt mạng: chỉ `nginx` publish cổng (8080). Cổng quản trị 9090 (`/actuator/health`, `/actuator/prometheus`) và cổng 8081 của `api` không đi qua nginx; Prometheus trong profile `obs` scrape trong mạng compose. nginx không proxy `/actuator`, `/v3/` (OpenAPI) hay `/swagger-ui`. Actuator chỉ mở `health` và `prometheus` (DR-09), không `env`, `heapdump`, `beans`.

## 9. STRIDE rút gọn cho ba ranh giới tin cậy

Ranh giới theo [DOC-07 §5](../03-architecture/system-context-and-containers.md): **TB1** trình duyệt ↔ nginx ↔ `api`; **TB2** Stripe → `api` (webhook); **TB3** trình duyệt → Stripe. Mỗi dòng: mối đe dọa → biện pháp → rủi ro còn lại. "Test" là mã ở mục 11.

### 9.1 TB1: trình duyệt ↔ nginx ↔ `api`

| | Mối đe dọa | Biện pháp | Rủi ro còn lại | Test |
| --- | --- | --- | --- | --- |
| **S** (giả mạo) | Đoán hoặc đánh cắp token đăng nhập/session | Token 256 bit, lưu hash, một lần, 15 phút; cookie `HttpOnly; SameSite=Lax; Secure`; session mới mỗi lần verify | Máy người dùng bị chiếm hoặc hộp thư bị lộ: ngoài phạm vi | SEC-03, SEC-04 |
| **T** (sửa đổi) | Sửa ID trong path để đổi tài nguyên người khác (IDOR) | Truy vấn có `organizer_id`/`user_id` từ session; 404 thay 403 | Lỗi lập trình ở một truy vấn mới: checklist PR + SEC-05…08 | SEC-05…08 |
| **T** | CSRF khiến người dùng đã đăng nhập đặt hoặc sửa | Synchronizer token + `SameSite=Lax` + kiểm `Origin` | — | SEC-09, SEC-10 |
| **T** | Chèn script qua văn bản người tổ chức nhập (XSS lưu) | React escape, `th:text`, CSP không `unsafe-inline` cho script, ESLint `no-danger` | `style-src 'unsafe-inline'` (chỉ style) | SEC-14, SEC-17 |
| **R** (chối bỏ) | Người mua chối đã đặt, người tổ chức chối đã đổi | Log có `trace_id`, `user_id`, `order_id`; `orders`, `stripe_event` bất biến về lịch sử; `updated_at` | Không có audit log riêng; chấp nhận ở giai đoạn này | — |
| **I** (lộ thông tin) | Đọc đơn/vé người khác; liệt kê email; lộ stack trace; lộ ảnh nháp | Sở hữu như §2.2; 202 cố định cho magic link; `INTERNAL_ERROR` không kèm stack (DOC-35); ảnh phục vụ công khai theo ID (ảnh sự kiện vốn công khai) | ID ảnh biết thì xem được ảnh nháp: chấp nhận vì ảnh không nhạy cảm | SEC-05, SEC-06, SEC-11, SEC-15 |
| **D** (từ chối dịch vụ) | Tràn request, spam magic link, gom vé bằng nhiều tài khoản | Ba lớp §5; bulkhead 24 permit; 503 `OVERLOADED` thay vì xếp hàng | Nhiều IP + nhiều tài khoản: phòng chờ làm chậm nhưng không chặn hẳn; EXP-05, EXP-08 đo | SEC-12, SEC-13 |
| **E** (nâng quyền) | Người mua gọi `/organizer/**`; sửa `organizer_id` trong body | `denyAll` mặc định; `organizer_id` từ session, body có trường lạ → 400 (DR-88) | — | SEC-02, SEC-07, SEC-08 |

### 9.2 TB2: Stripe → `api`

| | Mối đe dọa | Biện pháp | Rủi ro còn lại | Test |
| --- | --- | --- | --- | --- |
| **S** | Kẻ lạ gửi webhook giả "đã thanh toán" | `Webhook.constructEvent` với body thô, `STRIPE_WEBHOOK_SECRET`, dung sai 300 giây; sai → 400 `INVALID_SIGNATURE`; chỉ webhook đã xác minh mới đổi đơn sang `PAID` | Lộ `whsec_` = giả mạo được: giữ trong `.env` | SEC-18 |
| **T** | Sửa số tiền trong payload | Chữ ký phủ cả body; thêm kiểm `amount_received`, `currency` khớp đơn (DR-48) | — | SEC-19 |
| **R** | Stripe gửi lại, hai lần ghi | `stripe_event` PK + `ON CONFLICT DO NOTHING` trong cùng transaction (DR-48) | — | SEC-20 |
| **I** | Webhook làm lộ thông tin qua phản hồi | Phản hồi chỉ 200/400/500 rỗng hoặc Problem Details tối thiểu; không echo payload | — | SEC-18 |
| **D** | Làm tràn webhook | nginx `api_ip`; `allowlist` IP Stripe **không** dùng (danh sách đổi theo thời gian); chữ ký sai bị loại ở bước đầu, không chạm DB | Chữ ký đúng mà tràn: chỉ Stripe làm được | SEC-18 |
| **E** | Webhook gây hiệu ứng ngoài `PAID`/lỗi | Chỉ 5 loại event đăng ký; `charge.refunded` và `charge.dispute.created` chỉ ghi WARN (DR-48) | — | SEC-20 |

### 9.3 TB3: trình duyệt → Stripe

| | Mối đe dọa | Biện pháp | Rủi ro còn lại | Test |
| --- | --- | --- | --- | --- |
| **S** | Trang giả mạo nhập thẻ | CSP `frame-ancestors 'none'`, `form-action 'self'`; Payment Element chạy trong iframe `js.stripe.com` | Tên miền giả ngoài tầm kiểm soát | SEC-17 |
| **T** | Sửa số tiền ở client | Số tiền do server đặt khi tạo PaymentIntent; client chỉ nhận `clientSecret` | — | SEC-19 |
| **I** | Dữ liệu thẻ rò qua `api` hoặc log | Thẻ không bao giờ qua `api`; không log body; `clientSecret` chỉ trả cho chủ đơn (`own`) và không ghi log | `clientSecret` nằm trong bộ nhớ client: do Stripe thiết kế | SEC-16 |
| **E** | Người khác dùng `clientSecret` của đơn khác | `POST /orders/{id}/payment-intent` lọc `own`; mỗi đơn một PaymentIntent (DR-47) | — | SEC-06 |

## 10. Quét phụ thuộc và kiểm tra trong CI

(Chi tiết bước ở [DOC-63](../09-operations/ci-cd.md) §9.)

| Kiểm tra | Chặn merge? | Phủ |
| --- | --- | --- |
| `scripts/check-secrets.sh` (trong `make lint`) | Có | Khóa Stripe, `whsec_` bị commit |
| ArchUnit/Modulith: không controller nào bỏ qua service để đọc repository | Có | Gốc của lỗi IDOR |
| `SecurityMatrixTest` (SEC-01, SEC-02): mọi endpoint trong OpenAPI có dòng ở §2.3 và ngược lại | Có | Endpoint quên khai quyền |
| OSV-Scanner trên `gradle.lockfile`, `pnpm-lock.yaml` (job `audit`, hằng tuần và mỗi PR) | **Không** (tham khảo, DR-129) | Lỗ hổng phụ thuộc đã biết |
| ESLint `react/no-danger`, `i18next/no-literal-string` | Có | XSS ở frontend |

## 11. Test bắt buộc (tiền tố `SEC-`)

Tiền tố `SEC-` dành riêng cho tài liệu này (master plan §0.4). Chạy chủ yếu ở mức tích hợp (Testcontainers + MockMvc), một số ở E2E.

| ID | Tình huống | Kết quả mong đợi |
| --- | --- | --- |
| SEC-01 | Đối chiếu OpenAPI sinh ra với bảng §2.3 | Mỗi cặp (method, path) có đúng một dòng ở bảng và ngược lại (test đọc bảng từ `src/test/resources/security-matrix.csv`, file này sinh từ §2.3) |
| SEC-02 | Gọi một đường dẫn `/api/v1/x` chưa khai quyền | 403, không phải 200/404 từ handler; bean `SecurityFilterChain` kết thúc bằng `denyAll` |
| SEC-03 | Gọi mọi endpoint "— (401)" không cookie | 401 `UNAUTHENTICATED` |
| SEC-04 | Verify thành công khi request có sẵn `tb_session` cũ | `Set-Cookie` giá trị mới; session cũ không bị dùng lại; DB chỉ chứa hash (cột không chứa chuỗi thô) |
| SEC-05 | A tạo reservation và đơn; B (người mua khác) `GET`/`DELETE /reservations/{id}`, `GET /orders/{id}` | 404 `NOT_FOUND` cả ba; log không chứa email |
| SEC-06 | B gọi `POST /orders/{id}/payment-intent` và `confirm-free` của đơn A | 404; không PaymentIntent nào được tạo (đếm `fake_payment_intent`) |
| SEC-07 | Tổ chức O2 gọi `GET`/`PATCH`/`publish` sự kiện của O1, `PUT .../maps/{id}/draft` của O1 | 404 mọi lần; dữ liệu O1 không đổi |
| SEC-08 | Người mua chưa có hồ sơ gọi `GET /organizer/events`; body `POST /organizer/events` chứa `organizerId` của người khác | 403 `ORGANIZER_PROFILE_REQUIRED`; body có trường lạ → 400 `BAD_REQUEST` |
| SEC-09 | `POST /events/{id}/reservations` có session nhưng thiếu `X-CSRF-Token`; rồi sai token | 403 `CSRF_TOKEN_INVALID` cả hai; không có reservation nào |
| SEC-10 | Request ghi có `Origin: https://evil.example` và token đúng; `OPTIONS` từ origin lạ | 403 `CSRF_TOKEN_INVALID`; `OPTIONS` 405; không header `Access-Control-Allow-*` |
| SEC-11 | `POST /auth/magic-link` với email đã có tài khoản và email mới | Cùng 202, cùng độ dài body; không khác biệt header (trừ `X-Request-Id`) |
| SEC-12 | 25 request/giây tới `/api/v1/events` từ một IP không trong allowlist (qua nginx) | Có 429 `RATE_LIMITED` kèm `Retry-After` và `retryAfterSeconds`; IP trong `RATE_LIMIT_ALLOWLIST` không bị 429 |
| SEC-13 | Một người dùng gọi lệnh giữ vé 6 lần liên tiếp trong 1 giây | Lần thứ 6 trả 429 `RATE_LIMITED`; tắt Redis thì bucket bị bỏ qua và không lỗi 5xx ngoài 503 `OVERLOADED` ở sự kiện `high_demand` (DR-56) |
| SEC-14 | Tạo sự kiện tên `<img src=x onerror=alert(1)>`; render trang sự kiện (E2E) và email đổi lịch | Hiển thị nguyên văn như văn bản; không `alert`; HTML email chứa `&lt;img` |
| SEC-15 | Gọi API gây lỗi 500 (đưa cursor hỏng, ép exception) | Body chỉ `INTERNAL_ERROR` + `requestId`; không stack trace, không tên bảng, không email |
| SEC-16 | Chạy luồng thanh toán fake; quét toàn bộ log của `api` bằng regex email và `sk_|whsec_|clientSecret|pi_.*_secret` | 0 kết quả |
| SEC-17 | `curl -sI` trang chủ qua nginx | Có đủ header ở §8; CSP chứa `https://js.stripe.com` và không chứa `script-src` với `'unsafe-inline'` hay `'unsafe-eval'` |
| SEC-18 | Webhook không chữ ký, chữ ký sai, chữ ký đúng nhưng timestamp cũ 10 phút | 400 `INVALID_SIGNATURE` cả ba; `stripe_event` không có dòng mới |
| SEC-19 | Webhook `succeeded` đúng chữ ký nhưng `amount_received` khác số tiền đơn | Đơn không sang `PAID`; ghi log ERROR; `outcome` ghi nhận lệch (DR-44, DR-48) |
| SEC-20 | Gửi cùng event 3 lần; gửi event loại `charge.dispute.created` | Đúng một dòng `stripe_event`/một bộ vé; loại thứ hai chỉ log WARN, đơn không đổi |
| SEC-21 | Upload: file `.svg`; file `.png` thật nhưng đuôi `.jpg`; file `.png` giả (nội dung HTML); ảnh 9000 px; 3 MB cho `EVENT_IMAGE` | SVG, giả và 9000 px → 422 `MEDIA_INVALID`; 3 MB → 422 `MEDIA_INVALID`; PNG thật đuôi sai → 201 |
| SEC-22 | `GET /media/{id}` | `Content-Type` = giá trị trong `media.content_type`, có `X-Content-Type-Options: nosniff` |
| SEC-23 | `curl` tới `localhost:8080/actuator/prometheus`, `/v3/api-docs`, `/swagger-ui` | 404; cổng 9090 không publish ra host ở `make up` |
| SEC-24 | `GET /me`, `GET /orders/{id}`, `GET /organizer/events` | Có `Cache-Control: no-store` |
| SEC-25 | Khởi động `api` với `PAYMENTS_MODE=fake` và `STRIPE_SECRET_KEY=sk_live_abc…` | Thoát khi khởi động (DR-51); trùng OPS-12, thêm ở đây để phủ ma trận rủi ro |
| SEC-26 | Commit mẫu chứa `sk_test_` + 24 ký tự | `make lint` thoát khác 0 |

## 12. Quyết định phát sinh khi viết tài liệu này

Mọi quyết định do Claude chốt (Owner ủy quyền); đang chờ số DR thật từ phiên gộp.

| ID | Quyết định | Lý do |
| --- | --- | --- |
| DR-116 | ⚠ Tài nguyên của người mua (`reservation`, `orders`, `ticket`) thuộc người khác trả **404** `NOT_FOUND`, không phải 403 như SDD gốc 12.3. 403 `FORBIDDEN` chỉ cho vi phạm quyền không gắn tài nguyên cụ thể | DR-23 đã chọn 404 cho studio để không lộ tồn tại; hai quy tắc khác nhau cho cùng loại lỗi dễ gây IDOR do sai sót; ID đơn lộ qua URL chia sẻ |
| DR-117 | Bộ lọc `OriginCheckFilter`: request ghi có header `Origin` mà khác `APP_BASE_URL` bị từ chối 403 `CSRF_TOKEN_INVALID`; không có `Origin` thì chỉ kiểm CSRF token | Lớp phòng thủ thứ hai cho CSRF, rẻ, không đụng k6/curl |
| DR-118 | Spring Security mặc định `denyAll`; mọi endpoint khai quyền tường minh và có dòng ở §2.3; `SecurityMatrixTest` đối chiếu OpenAPI với `security-matrix.csv` | Endpoint quên khai quyền thì đóng, không mở |
| DR-119 | `Cache-Control: no-store` cho nhóm endpoint cá nhân (§8) đặt ở `api`, nginx tôn trọng và không `proxy_cache` | Tránh cache dữ liệu người dùng ở nginx hay trình duyệt dùng chung |

## Câu hỏi còn mở

Không có. (Việc siết `style-src` với Payment Element là việc kiểm ở P3-08, không phải câu hỏi chặn tài liệu; kết quả cập nhật §8.)
