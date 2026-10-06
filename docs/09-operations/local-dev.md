# Môi trường dev

> Trạng thái: **Review** · Cập nhật: 2026-10-06 · DOC-61
> Phụ thuộc: SDD gốc §14, [DOC-07](../03-architecture/system-context-and-containers.md) §2, [DOC-11](../03-architecture/tech-stack-and-versions.md) §7, [DOC-12](../03-architecture/code-architecture.md), [DOC-62](deploy-compose.md), [DOC-06](../02-glossary.md), [Sổ quyết định](../00-decision-register.md) (DR-01, 04, 07, 22, 51, 72, 73, 77, 78, 80)
> Người dùng chính: P1-01, P1-02, P1-11 (người mới phải đăng nhập `buyer1@demo.test` trong ≤ 15 phút); mọi task `Pn-xx` khi chạy và kiểm thử cục bộ; DOC-81 (kịch bản demo)

Tài liệu hướng dẫn dựng và chạy hệ thống trên máy dev: yêu cầu máy, cài đặt, các target `make`, bảng cổng, tài khoản demo và cách đăng nhập qua Mailpit, seed và reset, chạy với Stripe thật, lỗi thường gặp. Cấu hình compose (profile, healthcheck, giới hạn tài nguyên, nginx, `.env`) nằm ở [DOC-62](deploy-compose.md); workflow CI ở [DOC-63](ci-cd.md); chiến lược kiểm thử ở DOC-69. Mọi lệnh dưới đây chạy nguyên văn từ thư mục gốc repo. Số liệu thời gian là **dự kiến** (planned) cho tới khi P1-02 đo trên máy thật.

> **Chưa chạy thử.** Repo chưa có `Makefile`, `deploy/compose/` hay mã nguồn tại thời điểm viết (P0). Các lệnh và đầu ra mẫu là đặc tả cho P1-01, P1-02, P1-11; task đó phải cập nhật tài liệu này nếu thực tế khác.

## 1. Yêu cầu máy

| Thành phần | Yêu cầu | Kiểm tra | Ghi chú |
| --- | --- | --- | --- |
| RAM | ≥ 16 GB | — | `docker compose up` (7 container) + Gradle + IDE; `obs` cần thêm ~1 GB |
| CPU, đĩa | ≥ 4 nhân, ≥ 15 GB trống | — | Image, volume `postgres-data`, `storage-data`, cache Gradle |
| Docker | Docker Engine 29 + Compose v2 (OrbStack dùng được ở macOS) | `docker compose version` | Testcontainers cần Docker socket |
| JDK | 25 (Temurin) | `java -version` → `openjdk version "25…"` | Gradle toolchain tự tải nếu thiếu (DR-02) |
| Node | 22 LTS (`.nvmrc`) | `node -v` → `v22.x` | Dùng `fnm`/`nvm use` |
| pnpm | 10 (khóa bằng `packageManager`) | `pnpm -v` → `10.x` | `corepack enable` |
| GNU Make | 3.81 trở lên (bản có sẵn của macOS dùng được) | `make -v` | Giao diện lệnh duy nhất (DR-01) |
| `curl`, `jq` | bất kỳ | `jq --version` | Dùng bởi `make login` |
| Git | 2.40 trở lên | `git --version` | Quy trình nhánh theo DR-07 |
| Tùy chọn | Stripe CLI v1.30; k6; Chromium và WebKit của Playwright | — | Chỉ khi chạy Stripe thật, thực nghiệm, E2E |

## 2. Cài đặt lần đầu

```bash
git clone <repo-url> ticket-booking && cd ticket-booking
git checkout dev
corepack enable && pnpm --dir frontend install --frozen-lockfile
make up            # dựng toàn bộ stack; tự tạo deploy/compose/.env từ .env.example nếu chưa có
make seed          # tạo tài khoản và sự kiện demo
```

Đầu ra mong đợi của `make up` (dự kiến, ≤ 3 phút với image đã tải, ≤ 8 phút lần đầu):

```text
✔ Container ticket-postgres-1  Healthy
✔ Container ticket-redis-1     Healthy
✔ Container ticket-storage-1   Healthy
✔ Container ticket-mailpit-1   Healthy
✔ Container ticket-api-1       Healthy
✔ Container ticket-nginx-1     Healthy
Ready: http://localhost:8080   Mailpit: http://localhost:8025
```

Mở http://localhost:8080. Trang danh sách sự kiện hiện sau khi `make seed` xong. Có hai cách chạy, chọn theo việc đang làm:

| Chế độ | Lệnh | API chạy ở | Frontend chạy ở | Dùng khi |
| --- | --- | --- | --- | --- |
| **Stack đầy đủ** | `make up` | container `api` | container `nginx` (bản build) — http://localhost:8080 | Xem hệ thống như người dùng, chạy E2E, demo, thực nghiệm |
| **Dev** | `make dev` | `./gradlew bootRun` trên máy, cổng 8081 | `pnpm dev` (Vite, HMR) — http://localhost:5173, proxy `/api` và `/media` tới `localhost:8081` | Viết code: hot reload, debugger của IDE |

`make dev` chỉ khởi động hạ tầng bằng compose (`postgres`, `redis`, `storage`, `mailpit`) cộng file ghi đè `docker-compose.dev.yml` để mở cổng `8333` của `storage` và `9090` ra máy host (DR-124). Không chạy `nginx` nên **không có rate limit và cache của nginx** trong chế độ này; kiểm tra hành vi edge bằng `make up`.

## 3. Các target `make`

Mỗi target là một dòng trong `Makefile`; CI gọi đúng các target này (DR-01, DR-08, [DOC-63](ci-cd.md)). Cột "Tương đương" là lệnh thật bên dưới, để biết khi cần chạy riêng.

| Target | Làm gì | Tương đương | Ghi chú |
| --- | --- | --- | --- |
| `make up` | Dựng và chạy toàn bộ stack, chờ healthy | `docker compose --env-file deploy/compose/.env -f deploy/compose/docker-compose.yml up -d --build --wait` | Tạo `.env` từ `.env.example` nếu chưa có |
| `make down` | Dừng stack, **giữ** volume | `docker compose … down` | |
| `make reset` | Xóa mọi dữ liệu (volume `postgres-data`, `storage-data`, Mailpit) rồi dựng lại, **không** seed | `docker compose … down -v` rồi `up -d --wait` | Dùng `make reset seed` để về trạng thái demo (DR-78) |
| `make seed` | Tạo tài khoản và sự kiện demo (§5) | `docker compose … run --rm api --spring.profiles.active=seed` | Chạy một lần rồi thoát; idempotent; xem DR-123 |
| `make dev` | Hạ tầng bằng compose, API bằng `bootRun`, frontend bằng `pnpm dev` (§2) | `docker compose … -f docker-compose.dev.yml up -d --wait postgres redis storage mailpit`, rồi `./gradlew -p backend bootRun` và `pnpm --dir frontend dev` song song | Ctrl+C dừng cả hai; hạ tầng vẫn chạy, dừng bằng `make down` |
| `make test` | Unit test backend và frontend | `./gradlew -p backend test` và `pnpm --dir frontend test --run` | Gồm test kiến trúc (Spring Modulith, ArchUnit) |
| `make it` | Test tích hợp và đồng thời bằng Testcontainers | `./gradlew -p backend integrationTest` | Cần Docker; 64 luồng × 20 lần (DR-77); kèm `make invariants` kiểm tra sau test khi task chạm kho vé |
| `make e2e` | Playwright (Chromium + WebKit) trên stack `PAYMENTS_MODE=fake`, đọc magic link qua Mailpit | `pnpm --dir frontend e2e` | Cần `make up seed` trước |
| `make e2e-stripe` | E2E với thẻ test thật | như trên với `PAYMENTS_MODE=stripe` | Cần khóa Stripe (§7) |
| `make lint` | Spotless, ESLint, Prettier, `tsc --noEmit`, `i18n:check`, kiểm tra tên migration | `./gradlew -p backend spotlessCheck`; `pnpm --dir frontend lint && pnpm --dir frontend format:check && pnpm --dir frontend typecheck && pnpm --dir frontend i18n:check`; `scripts/check-migrations.sh` | `make fmt` sửa tự động |
| `make fmt` | Tự định dạng Java và TypeScript | `./gradlew -p backend spotlessApply`; `pnpm --dir frontend format` | |
| `make contract` | So OpenAPI do springdoc sinh với `api/openapi.yaml` và kiểm tra `schema.d.ts` khớp | `./gradlew -p backend test --tests OpenApiExportTest` (ghi `backend/build/openapi.json`, DR-87), rồi `oasdiff` so với `api/openapi.yaml`; `pnpm --dir frontend gen:api && git diff --exit-code frontend/src/api/schema.d.ts` | Sửa hợp đồng theo quy trình master plan §7.4 |
| `make invariants` | Kiểm tra bất biến một lần, in JSON, thoát 0 khi sạch, 1 khi có sai lệch (DR-73) | `docker compose … run --rm api --spring.profiles.active=invariants` | |
| `make exp EXP=02` | Chạy một thực nghiệm (DOC-70) | `experiments/run.sh 02` | Dùng `docker-compose.experiment.yml` (DOC-62 §9) |
| `make up-stripe` | Chạy với Stripe thật: bật profile compose `stripe`, `PAYMENTS_MODE=stripe` | xem §7 | Cần khóa trong `.env` |
| `make up-obs` | Thêm Prometheus + Grafana (profile `obs`) | `docker compose … --profile obs up -d --wait` | Grafana http://localhost:3000 |
| `make logs [S=api]` | Theo dõi log, mặc định mọi service | `docker compose … logs -f --tail=200 $(S)` | Log API là JSON ECS (DR-09); lọc bằng `jq` |
| `make psql` | Mở `psql` vào database | `docker compose … exec postgres psql -U ticket ticket` | |
| `make login EMAIL=…` | In magic link mới nhất của một email từ Mailpit (§6) | `curl` + `jq` tới API Mailpit | Chỉ có ở môi trường dev |
| `make build` | Build image `api` và bundle frontend như CI | `docker build` + `pnpm --dir frontend build` | Kèm `scripts/check-bundle-size.sh` (≤ 200 KB gzip, DR-81) |

Quy ước: mọi target đứng độc lập (không cần chạy theo thứ tự) trừ `e2e`, `e2e-stripe` cần stack đang chạy. Target nào thất bại trả mã thoát khác 0 để CI chặn. Biến ghi đè: `ENV_FILE` (mặc định `deploy/compose/.env`), `S`, `EXP`, `EMAIL`.

## 4. Bảng cổng

Nguồn: DR-72, DR-80, [DOC-07](../03-architecture/system-context-and-containers.md) §2.1. Cột "Ở đâu" cho biết cổng có trên host ở chế độ nào.

| Cổng host | Dịch vụ | Dùng để | Ở đâu |
| --- | --- | --- | --- |
| 8080 | `nginx` | Ứng dụng (SPA + `/api` + `/media`) | `make up` |
| 5173 | Vite dev server | Ứng dụng khi viết frontend | `make dev` |
| 8081 | `api` (HTTP) | API; `make dev` proxy `/api` và `/media` tới đây | `make dev` (trong `make up` chỉ trong mạng compose) |
| 9090 | `api` (quản trị: `health`, `prometheus`) | Actuator, scrape metric | `make dev`, trong mạng compose ở `make up` |
| 5432 | `postgres` | `psql`, DBeaver | cả hai |
| 6379 | `redis` | `redis-cli` | cả hai |
| 8025 | `mailpit` (UI và API) | Đọc email, magic link | cả hai |
| 1025 | `mailpit` (SMTP) | API gửi thư | cả hai |
| 8333 | `storage` (SeaweedFS S3) | API đọc ghi ảnh | `make dev` (trong `make up` chỉ trong mạng compose) |
| 9091 | `prometheus` | Truy vấn metric | profile `obs` |
| 3000 | `grafana` | Dashboard EXP-05 | profile `obs` |

Cổng bị chiếm: xem §8, mục "bind: address already in use".

## 5. Dữ liệu demo

`make seed` chạy ứng dụng với Spring profile `seed`, gọi cùng các service như API thật (không `INSERT` SQL trực tiếp, để unit của kho vé được sinh qua đường xuất bản thật, DR-27) rồi thoát. Idempotent: nếu `organizer@demo.test` đã có thì in `Seed already applied` và thoát 0; muốn nạp lại thì `make reset seed`.

| Tài khoản | Vai trò | Ghi chú |
| --- | --- | --- |
| `organizer@demo.test` | `ORGANIZER` (đã có hồ sơ tổ chức "Nhà hát Bến Sông") | Chủ ba sự kiện mẫu |
| `buyer1@demo.test`, `buyer2@demo.test`, `buyer3@demo.test` | `BUYER` | Dùng cho demo, E2E, thử tranh vé |

Sự kiện mẫu (DR-78): **"Hòa nhạc Giao Mùa"** (180 ghế VIP/Thường/Sinh viên + khu đứng 300 = 480 vé; đang mở bán), **"Hội thảo Nghề Thiết Kế"** (chỉ GA: Đặt sớm đã hết, Phổ thông, Sinh viên), **"Đêm nhạc Sân Thượng"** (sắp mở bán, `high_demand`). Tên sự kiện là dữ liệu của người tổ chức, không dịch (DR-10). Tài khoản demo không có mật khẩu: hệ thống chỉ có magic link (DR-21). Tên miền `demo.test` không tồn tại thật; thư chỉ tới Mailpit.

Kiểm tra seed: `make psql` rồi `SELECT title, status FROM event ORDER BY created_at;` trả 3 dòng (`PUBLISHED`, `PUBLISHED`, `PUBLISHED`).

## 6. Đăng nhập qua Mailpit

1. Mở http://localhost:8080 (hoặc :5173 ở `make dev`) → "Đăng nhập" → nhập `buyer1@demo.test` → "Gửi đường dẫn".
2. Mở Mailpit http://localhost:8025, mở thư mới nhất "Đường dẫn đăng nhập" (tiêu đề theo locale, DOC-51), bấm nút trong thư. Trình duyệt mở `/auth/callback?token=…`, tạo session và quay về trang trước đó (DR-67).
3. Đường tắt cho script và E2E: `make login EMAIL=buyer1@demo.test` in đường dẫn ra terminal:

```bash
curl -s http://localhost:8025/api/v1/message/latest | jq -r '.Text' | grep -Eo 'http[^ ]*/auth/callback\?token=[A-Za-z0-9_-]+' | head -1
```

Đầu ra mẫu: `http://localhost:8080/auth/callback?token=3Zk1…(43 ký tự)`. Link hết hạn sau 15 phút, dùng một lần; xin link mới thì link cũ vô hiệu (DR-21). Gửi quá 3 lần trong 15 phút cho một email: UI hiện biến thể "gửi quá nhiều" và không có thư mới. Mailpit giữ thư trong bộ nhớ, mất khi `make down`; thư cũ còn khi chưa reset nên luôn lấy thư **mới nhất**.

Link trong thư dựng từ `APP_BASE_URL`: phải là `http://localhost:8080` ở `make up` và `http://localhost:5173` ở `make dev`; `make dev` tự đặt đúng.

## 7. Chạy với Stripe thật (test mode)

Mặc định `PAYMENTS_MODE=fake`: không cần tài khoản Stripe (DR-72, DR-51). Muốn thử thẻ test thật:

1. Lấy khóa test từ Stripe Dashboard (tài khoản nước khác nhận tiền VND, DR-13): `sk_test_…` và `pk_test_…`.
2. Điền vào `deploy/compose/.env`: `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`. Không điền `STRIPE_WEBHOOK_SECRET`; `stripe-cli` tự cấp.
3. `make up-stripe`. Target này: (a) đặt `PAYMENTS_MODE=stripe` và `SPRING_PROFILES_ACTIVE=dev,stripe`; (b) build lại image `nginx` với `VITE_PAYMENTS=stripe` (biến build, không đổi được lúc chạy); (c) bật profile compose `stripe` để `stripe-cli` chạy `stripe listen --forward-to http://api:8081/api/v1/webhooks/stripe` và ghi `whsec_…` vào volume dùng chung; `api` đọc secret đó khi khởi động.
4. Thanh toán bằng thẻ `4242 4242 4242 4242`, hạn bất kỳ trong tương lai, CVC bất kỳ (thẻ 3-D Secure: `4000 0027 6000 3184`). Số tiền tối thiểu theo `payment.min-amount` (S-02).
5. Quay lại fake: `make down && make up` (đọc lại `PAYMENTS_MODE` từ `.env`; `make up-stripe` không sửa file `.env`).

Trong chế độ Stripe, `make e2e-stripe` chạy Playwright với thẻ test. API từ chối khởi động nếu `PAYMENTS_MODE=fake` đi cùng `sk_live_…`, hoặc nếu `PAYMENTS_MODE` và profile `fake-payments`/`stripe` lệch nhau (DR-51, DR-125).

## 8. Lỗi thường gặp

| Triệu chứng | Nguyên nhân | Cách xử lý |
| --- | --- | --- |
| Đăng nhập xong vẫn bị đưa về trang đăng nhập; chỉ xảy ra ở Safari | Safari không nhận cookie `Secure` trên `http://localhost` (DR-22) | Dùng `make up` hoặc `make dev` (đặt `AUTH_COOKIE_SECURE=false`, hoặc `auth.cookie-secure=false` ở profile `dev`). Không đặt `true` ở dev. Nếu vẫn lỗi, mở bằng `http://localhost`, không dùng IP LAN hay `127.0.0.1` khác với `APP_BASE_URL` |
| `bind: address already in use` ở 5432, 6379, 8080, 8025 | Cổng bị tiến trình khác (PostgreSQL, Redis cài sẵn) chiếm | `lsof -nP -iTCP:5432 -sTCP:LISTEN` để tìm; tắt dịch vụ đó, hoặc đổi cổng host trong `.env` (`POSTGRES_PORT`, `REDIS_PORT`, `NGINX_PORT`, `MAILPIT_UI_PORT`) rồi sửa `APP_BASE_URL` theo `NGINX_PORT` |
| `make up` treo ở `api` "starting" quá 3 phút | Flyway chạy migration chậm hoặc `postgres` chưa healthy; hoặc thiếu RAM cho Docker | `make logs S=api`; kiểm tra Docker Desktop cấp ≥ 8 GB RAM; `make reset` nếu volume cũ lệch migration |
| `api` thoát ngay: `Validate failed: Migration checksum mismatch` | Sửa migration đã chạy (cấm, DR-07) | Viết migration mới; ở máy dev dùng `make reset` |
| `api` thoát: `payments mode and profile mismatch` hoặc từ chối `sk_live_` | `PAYMENTS_MODE` và `SPRING_PROFILES_ACTIVE` lệch, hoặc khóa live trong `.env` | Dùng `make up` hoặc `make up-stripe`; không sửa tay từng biến; xóa khóa live |
| Không thấy thư magic link | Mở nhầm thư cũ, hoặc SMTP lỗi → UI báo 503 `EMAIL_PROVIDER_UNAVAILABLE` | `make logs S=mailpit`; kiểm tra `SPRING_MAIL_HOST=mailpit` (compose) hoặc `localhost` (`make dev`); bấm "Gửi lại" |
| 429 `RATE_LIMITED` khi chạy script nhiều request | Giới hạn nginx (DR-55: `api_ip` 20 r/s) | Thêm IP vào `RATE_LIMIT_ALLOWLIST` trong `.env`, `make up` lại; hoặc dùng `make dev` (không qua nginx) |
| Ảnh sự kiện không tải lên được | `storage` chưa healthy, hoặc sai `STORAGE_S3_*`; bucket do API tạo lúc khởi động | `make logs S=storage`; `make reset` nếu volume hỏng |
| `make it` lỗi `Could not find a valid Docker environment` | Docker không chạy hoặc socket không có quyền | Khởi động Docker/OrbStack; macOS: `export DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock` nếu dùng OrbStack |
| `make it` chậm ở lần đầu | Testcontainers kéo `postgres:18-alpine`, `redis:8.2-alpine`, SeaweedFS | Chạy `docker pull` trước; lần sau dùng cache |
| `pnpm dev` báo lỗi proxy `ECONNREFUSED 8081` | `make dev` chưa khởi động API xong | Chờ dòng `Started Application` trong terminal, hoặc dùng `VITE_API_MOCK=1 pnpm dev` để chạy frontend không cần backend (master plan §7.4) |
| JDK sai: `Unsupported class file major version 69` hoặc ngược lại | Máy dùng JDK khác 25 | `sdk use java 25-tem` hoặc để Gradle toolchain tải JDK 25 |
| Phòng chờ và rate limit theo người dùng "không hoạt động" | Redis chưa chạy (mất Redis thì không có dự phòng, DR-56) | `docker compose … ps redis`; `make up` lại |

## 9. Quy trình hằng ngày

1. `git switch dev && git pull`; `git switch -c feat/p1-07-magic-link` (DR-07).
2. `make dev` (hoặc `make up` nếu cần nginx). `make seed` lần đầu.
3. Sửa code; `make fmt lint test` trước mỗi commit; `make it` trước khi mở PR nếu chạm database, kho vé, thanh toán; `make invariants` sạch nếu chạm kho vé, reservation, order (DoD, master plan §7.2).
4. Chạm hợp đồng API: sửa `E-xx` (DOC-37) và `api/openapi.yaml` trước, `make contract`, `pnpm gen:api`.
5. Commit theo Conventional Commits (`feat(auth): supersede previous login tokens`, footer `Refs: P1-07`), mở PR vào `dev`. CI chạy các target giống hệt ([DOC-63](ci-cd.md)).

## 10. Kiểm tra chấp nhận

Test của tài liệu, tiền tố `OPS-` (dùng chung cho DOC-61, 62, 63; đăng ký ở DOC-69). Chạy ở P1-02, P1-11 trên máy sạch.

| ID | Kịch bản | Kỳ vọng |
| --- | --- | --- |
| OPS-01 | Máy sạch đã cài công cụ ở §1; `git clone`, `make up` | Mọi service `Healthy` ≤ 3 phút (image đã tải); `curl -s localhost:8080/healthz` trả `ok`; `curl -s localhost:8080/api/v1/events` trả 200 |
| OPS-02 | `make up seed`; lấy link bằng `make login EMAIL=buyer1@demo.test`; mở link | Có session; `GET /api/v1/me` trả `buyer1@demo.test`; tổng thời gian từ máy sạch ≤ 15 phút (P1-11) |
| OPS-03 | Chạy `make seed` hai lần liên tiếp | Lần hai in `Seed already applied`, thoát 0; `SELECT count(*) FROM event` vẫn là 3 |
| OPS-04 | `make reset seed`, rồi `make psql` → `SELECT count(*) FROM app_user` | 4 (`organizer@demo.test` + `buyer1…3@demo.test`) |
| OPS-05 | `make dev`; `curl -s localhost:5173/api/v1/events` | 200 qua proxy Vite; magic link trong Mailpit bắt đầu bằng `http://localhost:5173` |
| OPS-06 | `make invariants` sau `make seed` | Thoát 0; JSON có `"violations": []` |
| OPS-07 | Mở cổng 5432 bằng tiến trình khác rồi `make up` | `make up` thất bại với thông báo `address already in use`, không để stack ở trạng thái nửa chừng (`make down` dọn sạch) |
| OPS-08 | Safari (WebKit), `make up`, đăng nhập bằng link | Có session (cookie `tb_session` được nhận) |

## Quyết định phát sinh khi viết tài liệu này

Mọi quyết định mới dưới đây ở trạng thái đề xuất; main agent gán số DR thật khi gộp.

| ID tạm | Nội dung | Lý do |
| --- | --- | --- |
| DR-123 | Thêm Spring profile `seed` (chạy một lần, idempotent, gọi service thật) cho `make seed`; thêm target `make login`, `logs`, `psql`, `fmt`, `up-obs`, `build`, `e2e-stripe`, `contract`; `make reset` không tự seed | DR-01 chỉ liệt kê 10 target; seed qua service bảo đảm unit kho vé sinh đúng như xuất bản thật (DR-27) |
| DR-124 | Ba file compose ghi đè: `docker-compose.dev.yml` (mở cổng 8333, 9090 cho `make dev`), `docker-compose.experiment.yml` (giới hạn tài nguyên và tham số PostgreSQL của thực nghiệm), file gốc `docker-compose.yml` | DR-72 để `storage` và quản trị `api` không ra host; `make dev` cần chúng |
| DR-125 | `PAYMENTS_MODE` (`fake`\|`stripe`) và profile `fake-payments`/`stripe` phải khớp; API từ chối khởi động nếu lệch | `docker compose up` thuần phải chạy được (NFR-08) nên `.env.example` đặt cả hai; kiểm tra lúc khởi động chặn cấu hình nửa vời |

## Câu hỏi còn mở

- Giá trị `payment.min-amount` và hành vi thẻ 3-D Secure thật phụ thuộc kết quả S-02 (P0-03); §7 ghi số thẻ test chuẩn của Stripe, cần xác nhận lại ở S-02.
- Cách `stripe-cli` ghi `whsec_…` vào volume dùng chung (§7 bước 3) và cách SeaweedFS nhận khóa truy cập từ biến môi trường (DOC-62 §5) cần xác nhận khi dựng compose thật ở P1-02; nếu khác, cập nhật DOC-61 và DOC-62 trong PR của P1-02.
