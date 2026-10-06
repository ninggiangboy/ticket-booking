# Stack và phiên bản

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 (S-01 xong) · DOC-11
> Phụ thuộc: SDD gốc §4.3, [DOC-07](system-context-and-containers.md), [Sổ quyết định](../00-decision-register.md) (DR-01…05, 08, 38, 51, 68, 72, 77, 81), [ADR-0011](../04-adr/0011-java-25-spring-boot-4.md), [ADR-0012](../04-adr/0012-spring-data-jdbc-and-modulith-boundaries.md)
> Người dùng chính: P0-02 (spike S-01), P1-01 (khởi tạo monorepo), P1-02 (compose), P1-09 (khung frontend); DOC-12, DOC-61, DOC-62, DOC-63

Tài liệu khóa thư viện và công cụ của dự án kèm lý do và giấy phép, và là nơi ghi bảng tương thích của spike S-01. Cách dùng thư viện trong code nằm ở [DOC-12](code-architecture.md); image hạ tầng và cấu hình compose ở [DOC-62](../09-operations/deploy-compose.md); lệnh `make` ở [DOC-61](../09-operations/local-dev.md).

## 1. Quy tắc khóa phiên bản

- **Không có "latest".** Mọi thứ khóa theo version cụ thể: backend trong version catalog `backend/gradle/libs.versions.toml`; frontend bằng `pnpm-lock.yaml` cùng `packageManager` trong `package.json`; image Docker theo tag minor (DR-04).
- **Cột "Khóa" có ba mức.** `Khóa` (có số trong DR, dùng nguyên), `Dòng` (DR chốt dòng chính, patch khóa ở P1-01 sau S-01), `Theo BOM` (phiên bản do Spring Boot BOM quản lý, không tự khóa riêng).
- **Một lần nâng cấp = một PR.** Đổi version ở catalog hoặc lockfile, chạy `make lint test it`, ghi lý do vào PR; thư viện cần đổi API thì sửa [DOC-12](code-architecture.md) cùng PR.
- **Spike thất bại thì lùi toàn bộ, không trộn** (DR-02): nếu S-01 làm thư viện nào không khởi động được, cả dự án lùi về Java 21 + Spring Boot 3.5, ghi ADR thay ADR-0011 và đổi bảng ở đây.
- Số version cụ thể của mọi dòng "Dòng" và "Theo BOM" nằm ở §8 (kết quả S-01, 2026-10-06) và được khóa trong `backend/gradle/libs.versions.toml`.

## 2. Runtime và build

| Thành phần | Lựa chọn | Khóa | Lý do | Giấy phép | Nguồn |
| --- | --- | --- | --- | --- | --- |
| JDK | Java 25, Temurin | Khóa: 25 (patch mới nhất lúc P1-01) | LTS mới nhất tại 2026-10; Spring Boot 3.5 đã hết hỗ trợ OSS (DR-02). Không dùng virtual thread cho đường giữ vé (số luồng phải nhỏ hơn connection pool, DR-61) | GPLv2 + Classpath Exception | DR-02 |
| Framework | Spring Boot 4.0.x | Dòng: 4.0.x (patch mới nhất lúc P1-01) | Dòng được hỗ trợ; structured logging ECS sẵn (DR-09) | Apache-2.0 | DR-02 |
| Build | Gradle 9.x, Kotlin DSL, wrapper commit sẵn | Dòng: 9.x | Một project duy nhất `api` trong `backend/` (DR-01) | Apache-2.0 | DR-01 |
| Node | Node 22 LTS (`.nvmrc`) | Khóa: 22 | Bản LTS tại thời điểm chốt | MIT | DR-01 |
| Trình quản lý gói | pnpm 10 (`packageManager` khóa) | Khóa: 10 | Lockfile nhanh, workspace không cần (một package `map-core` là thư mục `src/map-core`) | MIT | DR-01 |
| Container | Docker, Docker Compose v2 | — | NFR-08; OrbStack dùng được ở macOS | Apache-2.0 | DR-72 |
| CI | GitHub Actions, runner `ubuntu-latest` | — | Đủ chạy PostgreSQL + Redis qua Testcontainers (DR-08) | — | DR-08 |
| Make | GNU Make | — | Giao diện lệnh duy nhất (DR-01) | GPL | DR-01 |

## 3. Backend

| Mảng | Thư viện | Khóa | Lý do | Giấy phép | Nguồn |
| --- | --- | --- | --- | --- | --- |
| Web | Spring Web MVC (starter-web), Bean Validation | Theo BOM | REST/JSON; platform thread | Apache-2.0 | SDD gốc 4.3 |
| Bảo mật | Spring Security | Theo BOM | Bộ lọc session tự viết trên SecurityContext, CSRF synchronizer, header (DR-22) | Apache-2.0 | DR-22 |
| Truy cập dữ liệu | Spring Data JDBC + `JdbcClient` cho custom fragment; **không JPA** | Theo BOM | Aggregate + SQL viết tay; không lazy loading (DR-05) | Apache-2.0 | DR-05 |
| Ranh giới module | Spring Modulith (`spring-modulith-starter-core`, test) | Dòng: dòng tương thích Boot 4 (xác nhận ở S-01) | `ApplicationModules.verify()` (DR-05, DR-06) | Apache-2.0 | DR-05 |
| Kiểm tra kiến trúc | ArchUnit | Dòng: 1.x mới nhất tương thích Java 25 (xác nhận ở S-01) | `layeredArchitecture()`, luật `@Transactional`, sở hữu bảng, không `save()` đổi `status` | Apache-2.0 | DR-06 |
| Database | PostgreSQL JDBC driver, HikariCP (kèm Boot) | Theo BOM | Pool 40, `connection-timeout` 1000 ms (DR-61) | BSD-2-Clause; Apache-2.0 | DR-61 |
| Migration | Flyway + `flyway-database-postgresql` | Theo BOM | `V<yyyymmddHHmm>__<snake_case>.sql`; thư mục thêm `db/migration-fake`, `db/migration-experiment` (DR-07, DR-51, DR-76) | Apache-2.0 | DR-07 |
| Redis | Spring Data Redis (Lettuce) | Theo BOM | Lua script (`token_bucket.lua`, `admit.lua`, `join.lua`, `leave.lua`); timeout 200 ms (DR-56) | Apache-2.0 | DR-56, DR-81 |
| Cache trong tiến trình | Caffeine | Theo BOM (nếu Boot quản lý) hoặc Dòng 3.x | Session cache, snapshot tình trạng chỗ (DR-22, DR-62) | Apache-2.0 | DR-62 |
| OpenAPI | springdoc-openapi (starter-webmvc-api) | Dòng: dòng tương thích Boot 4 (xác nhận ở S-01) | OpenAPI 3.1 ở `/v3/api-docs` (DR-63); sinh `schema.d.ts` cho frontend | Apache-2.0 | DR-03, DR-63 |
| Thanh toán | `stripe-java` | Dòng: mới nhất lúc S-01; khóa API version theo SDK | PaymentIntent, `Webhook.constructEvent` (DR-47, DR-48) | MIT | SDD gốc 4.3 |
| Object storage | AWS SDK for Java v2 (`s3` + `url-connection-client`) | Dòng: 2.x mới nhất lúc S-01 | `S3Client`, `forcePathStyle(true)`, `requestChecksumCalculation(WHEN_REQUIRED)` với SeaweedFS (DR-38) | Apache-2.0 | DR-38 |
| Email | Spring Mail (Jakarta Mail), Thymeleaf | Theo BOM | Mẫu HTML + text, `MessageSource` theo locale (DR-54) | Apache-2.0; EPL-2.0 | DR-54 |
| i18n | Spring `MessageSource` (`messages_vi`, `messages_en`) | Theo BOM | Email và Problem Details (DR-10) | Apache-2.0 | DR-10 |
| Hình học | JTS Topology Suite (`org.locationtech.jts:jts-core`) | Dòng: 1.20.x | `IsSimpleOp`, `PreparedGeometry.contains` cho validate sơ đồ (DR-35) | EPL-2.0 / EDL-1.0 (BSD-3-Clause) | DR-35 |
| JSON chuẩn hóa | `io.github.erdtman:java-json-canonicalization` | Dòng: 1.x | RFC 8785 JCS cho checksum phiên bản sơ đồ (DR-32) | Apache-2.0 | DR-32, DR-81 |
| JSON Schema | Thư viện validate JSON Schema 2020-12 cho Java (`com.networknt:json-schema-validator`) | Khóa: 3.0.8 (S-01: Boot 4 dùng Jackson 3 `tools.jackson`, dòng 1.5.x dùng Jackson 2 nên không nhận `JsonNode` của Boot; API 3.x là `SchemaRegistry`, `Schema`, `SpecificationVersion`) | Server nạp cùng file `seat-map.v1.json` với client (DR-32) | Apache-2.0 | DR-32, DR-81 |
| JSON | Jackson (kèm Boot) | Theo BOM | camelCase, `null` thay vì bỏ trường (DR-63) | Apache-2.0 | DR-63 |
| Observability | Spring Boot Actuator, Micrometer + `micrometer-registry-prometheus` | Theo BOM | Cổng 9090 (`health`, `prometheus`); metric riêng `ticket_*` (DR-09) | Apache-2.0 | DR-09 |
| Log | Logback + structured logging `ecs` của Boot | Theo BOM | Trường bắt buộc `trace_id`, `user_id`… (DR-09) | EPL-1.0 / LGPL-2.1 | DR-09 |

## 4. Kiểm thử và công cụ kiểm soát chất lượng

| Mảng | Công cụ | Khóa | Lý do | Giấy phép | Nguồn |
| --- | --- | --- | --- | --- | --- |
| Unit, tích hợp | JUnit Jupiter, AssertJ, Spring Boot Test | Theo BOM | Ngày chuẩn của hệ sinh thái Spring | EPL-2.0; Apache-2.0 | SDD gốc 15.1 |
| Database và Redis thật | Testcontainers (`postgres:18-alpine`, `redis:8.2-alpine`, SeaweedFS) | Dòng: dòng tương thích Docker API 29 và Boot 4 (xác nhận ở S-01) | Test tích hợp và đồng thời trên cùng image với chạy thật (DR-04) | MIT | DR-77 |
| Đồng thời | `ExecutorService` + `CountDownLatch`, 64 luồng, lặp 20 lần | — | DR-77 | — | DR-77 |
| Chờ bất đồng bộ | Awaitility | Dòng: 4.x | Chờ job nền, outbox | Apache-2.0 | DR-81 |
| Coverage | JaCoCo | Theo BOM | `inventory`, `reservation`, `order`, `payment`, `admission` ≥ 85%; module khác ≥ 70% (DR-77) | EPL-2.0 | DR-77 |
| Định dạng Java | Spotless + google-java-format | Dòng: mới nhất lúc P1-01 | `make lint` (master plan §7.3) | Apache-2.0 | DR-81 |
| Hợp đồng API | `oasdiff` | Dòng: mới nhất lúc P1-01 | `oasdiff breaking` so OpenAPI của PR với `dev` (DR-77) | Apache-2.0 | DR-77 |
| Tải | k6 (script TypeScript build bằng esbuild của k6) | Dòng: mới nhất lúc S-06 | Mô hình VU đa người dùng (DR-75) | AGPL-3.0 (dùng như công cụ, không nhúng) | DR-75 |
| Webhook cục bộ | Stripe CLI | `stripe/stripe-cli:v1.30` | DR-04 | Apache-2.0 | DR-04 |

## 5. Frontend

| Mảng | Lựa chọn | Khóa | Lý do | Giấy phép | Nguồn |
| --- | --- | --- | --- | --- | --- |
| Ngôn ngữ, build | TypeScript `strict`, Vite | Khóa: TypeScript 5.9, Vite 7 | SDD gốc 4.3 | Apache-2.0; MIT | DR-03 |
| UI | React | Khóa: 19 | SDD gốc 4.3 | MIT | DR-03 |
| Routing | React Router (data router, `createBrowserRouter`, route lazy) | Khóa: 7 | Tách bundle theo route (DR-67) | MIT | DR-03 |
| Server state | TanStack Query | Khóa: 5 | Nhịp hỏi `availability`, `orders`, `queue` | MIT | DR-03 |
| Store editor | Zustand (vanilla store, không qua context) | Khóa: 5 | Store tách khỏi chu trình render (DR-39) | MIT | DR-03, DR-39 |
| Immer | Immer | Dòng: 10.x | Áp command bất biến (DR-39) | MIT | DR-39, DR-81 |
| Canvas | Konva + react-konva; rbush | Khóa: Konva 10, react-konva 19, rbush 4 | Một `Konva.Shape` cho ghế; R-tree (DR-39) | MIT; ISC | DR-03 |
| Form | react-hook-form + zod | Khóa: 7, 4 | Lỗi theo ô như Studio 02, 03 | MIT | DR-03 |
| API client | `openapi-typescript` + `openapi-fetch` | Dòng: mới nhất lúc P1-01 | `frontend/src/api/schema.d.ts` sinh bằng `pnpm gen:api`; client mỏng không sinh code runtime | MIT | DR-03 |
| i18n | i18next + react-i18next + `i18next-icu` | Dòng: mới nhất lúc P1-01 | `fallbackLng: vi`, namespace theo feature (DR-10) | MIT | DR-03, DR-10 |
| Styling | CSS Modules + CSS custom properties từ token | — | Không thêm framework CSS (DR-68) | — | DR-03, DR-68 |
| Font tự host | `@fontsource/barlow-condensed` (600, 700), `@fontsource/be-vietnam-pro` (400, 500, 600), `@fontsource/ibm-plex-mono` (400, 500, 600) | Dòng: mới nhất lúc P1-01 | Không gọi Google Fonts lúc chạy (DR-68) | OFL-1.1 | DR-68 |
| Thanh toán | `@stripe/stripe-js` + `@stripe/react-stripe-js` | Dòng: mới nhất lúc P3-08 | Payment Element (DR-50) | MIT | SDD gốc 4.3 |
| Mock API | MSW | Dòng: 2.x | Bật bằng `VITE_API_MOCK=1` (master plan §7.4) | MIT | DR-81 |
| Test | Vitest, fast-check, Testing Library, Playwright | Khóa: Vitest 3, fast-check 4, Playwright 1.5x | Unit, property test, E2E WebKit + Chromium (DR-22, DR-77) | MIT; Apache-2.0 | DR-03 |
| Lint, format | ESLint (flat config), Prettier | Khóa: 9, 3 | `make lint` | MIT | DR-03 |
| Kiểm tra bundle | `rollup-plugin-visualizer` hoặc script kích thước gzip | — | JS ban đầu trang sự kiện ≤ 200 KB gzip (DR-81, DOC-38) | MIT | DOC-38 |

## 6. Hạ tầng (image Docker)

Khóa tag theo minor (DR-04); cập nhật có chủ đích bằng một PR.

| Container | Image | Giấy phép | Ghi chú |
| --- | --- | --- | --- |
| `postgres` | `postgres:18-alpine` | PostgreSQL License | Có `uuidv7()` sẵn (DR-11) |
| `redis` | `redis:8.2-alpine` | RSALv2 / SSPLv1 / AGPLv3 | Dùng nội bộ, không sửa mã nguồn nên không ảnh hưởng; Valkey 8 thay thế được không đổi code |
| `nginx` | `nginx:1.28-alpine` | BSD-2-Clause | |
| `mailpit` | `axllent/mailpit:v1.27` | MIT | |
| `stripe-cli` | `stripe/stripe-cli:v1.30` | Apache-2.0 | Profile `stripe` |
| `storage` | `chrislusf/seaweedfs` tag minor khóa lúc P1-01 | Apache-2.0 | Không dùng MinIO (bản cộng đồng ngừng phát hành image, repo lưu trữ năm 2026, DR-38) |
| `api` | build từ `eclipse-temurin:25-jre-alpine` | GPLv2 + Classpath Exception | |
| `prometheus`, `grafana` | tag minor khóa lúc P6-08 | Apache-2.0; AGPL-3.0 | Profile `obs`, chỉ chạy thực nghiệm |

## 7. Công cụ phát triển

| Công cụ | Yêu cầu | Dùng cho |
| --- | --- | --- |
| Máy dev | RAM ≥ 16 GB | `docker compose up` và build song song |
| JDK | 25 (Temurin) | `./gradlew bootRun`, test |
| Node, pnpm | 22 LTS, 10 | `pnpm dev`, `pnpm build`, `pnpm test`, `pnpm i18n:check`, `pnpm gen:api` |
| Docker + Compose | v2 | Hạ tầng, Testcontainers |
| Make | GNU Make | `make up`, `down`, `reset`, `seed`, `test`, `it`, `e2e`, `lint`, `invariants`, `exp`, `dev`, `up-stripe`, `contract` |
| Git | — | Nhánh `main`, `dev`, `feat/<task-id>-<slug>` (DR-07) |
| Stripe CLI (tùy chọn) | v1.30 | `make up-stripe`, gửi lại webhook (RB-03) |
| k6 | mới nhất lúc S-06 | Thực nghiệm tải |
| Playwright browsers | Chromium, WebKit | E2E; WebKit bắt lỗi cookie `Secure` của Safari |

## 8. Bảng tương thích từ spike S-01

S-01 (P0-02) chạy ngày 2026-10-06 trên JDK Temurin 25.0.4, Gradle 9.8.0, Docker 29.4.0 (macOS arm64). Ứng dụng rỗng nằm ở `backend/api` (gói `app.ticket.spike`), test `S01CompatibilityIT` chạy 9 kịch bản trên Testcontainers (`postgres:18-alpine`, `chrislusf/seaweedfs:4.48`), tất cả xanh. **Kết luận: giữ Java 25 + Spring Boot 4.0.x, không lùi về Java 21.**

| # | Thành phần | Version khóa | Điều kiểm tra ở S-01 | Kết quả |
| --- | --- | --- | --- | --- |
| 1 | Java 25 + Gradle 9 + Kotlin DSL | Temurin 25.0.4; Gradle 9.8.0 | Build, test trên JDK 25 (toolchain 25) | Đạt |
| 2 | Spring Boot + Spring Security | Boot 4.0.8; Framework 7.0.9; Security 7.0.7 | Context khởi động; `POST` không có token CSRF → 403, có token → 200 | Đạt |
| 3 | Spring Data JDBC | 4.0.7; PostgreSQL JDBC 42.7.13 | `JdbcAggregateTemplate.insert` với UUID gán trước; converter `jsonb` qua `PGobject` (cột đúng kiểu `jsonb`); `@Modifying @Query` có điều kiện trả `int` (1 rồi 0); `@MappedCollection` | Đạt |
| 4 | Flyway + PostgreSQL 18 | Flyway 11.14.1 + `flyway-database-postgresql` | Migration có `uuidv7()` (UUID version 7), index một phần, trigger `forbid_update` chặn UPDATE | Đạt |
| 5 | springdoc-openapi | 3.0.2 | `/v3/api-docs` trả OpenAPI 3.1; `openapi-typescript` 7.13.0 sinh `schema.d.ts` từ tài liệu đó | Đạt |
| 6 | stripe-java | 34.0.0 | `Webhook.constructEvent` xác minh chữ ký (offline). Tạo PaymentIntent thật thuộc S-02 | Đạt (phần offline) |
| 7 | AWS SDK v2 S3 + SeaweedFS | BOM 2.55.11; SeaweedFS 4.48 | `PutObject`, `GetObject`, `DeleteObject`, `forcePathStyle`, `requestChecksumCalculation(WHEN_REQUIRED)`; xóa xong thì `GetObject` trả `NoSuchKey` | Đạt, có điều kiện: SeaweedFS phải chạy với `-s3 -s3.config=<json identities>`, thiếu thì mọi request ký bị 400 "requires setting up SeaweedFS S3 authentication" |
| 8 | Spring Modulith | 2.0.8 | `ApplicationModules.verify()` chạy được. Khung 13 module và đồ thị DR-79 kiểm ở P1-05 | Đạt (phần khởi động) |
| 9 | ArchUnit | 1.5.1 | `layeredArchitecture()` đọc class biên dịch bằng JDK 25 | Đạt |
| 10 | Testcontainers | 2.0.5 | Postgres 18 và SeaweedFS khởi động, test tích hợp xanh; `redis:8.2-alpine` khởi động và trả `PONG` (kiểm bằng `docker run`) | Đạt |
| 11 | JTS, JCS, networknt | JTS 1.20.0; java-json-canonicalization 1.1; networknt 3.0.8 | `IsSimpleOp` nhận ra đa giác tự cắt; `PreparedGeometry.contains`; JCS ra `{"a":[1,0.5],"b":2}`; validate JSON Schema 2020-12 | Đạt, sau khi đổi networknt 1.5.x → 3.0.8 |

**Khác biệt so với giả định ban đầu, cần theo khi viết code:**
- Spring Boot 4 dùng **Jackson 3** (`tools.jackson.databind.*`). Thư viện còn dùng Jackson 2 (`com.fasterxml`) không nhận `JsonNode` của Boot; vì vậy phải chọn networknt 3.x.
- Tên starter và gói mới của Boot 4: `spring-boot-starter-webmvc`, `-flyway`, `-security-test`, `-webmvc-test`; `@AutoConfigureMockMvc` ở `org.springframework.boot.webmvc.test.autoconfigure`; mỗi starter kiểm thử tách riêng.
- Testcontainers 2.x đổi tên: `org.testcontainers:testcontainers-postgresql`, `testcontainers-junit-jupiter`; lớp `org.testcontainers.postgresql.PostgreSQLContainer` (không còn generic type).
- Driver PostgreSQL phải là `implementation`, vì converter `jsonb` dùng `PGobject` trong code chính.
- Spring Modulith `starter-core` và `starter-test` đi chung BOM `spring-modulith-bom`; AWS SDK đi qua `software.amazon.awssdk:bom`.

## Câu hỏi còn mở

- S-02 (P0-03) sẽ kiểm `stripe-java` với PaymentIntent thật ở test mode; nếu thất bại chỉ ảnh hưởng thư viện này, không kéo theo lùi Java.
- `S01CompatibilityIT` và gói `app.ticket.spike` là mã tạm: xóa khi P1-05 dựng khung module thật; các kịch bản còn giá trị (CSRF, `jsonb`, S3, Modulith) chuyển thành test của task tương ứng.

Quyết định phát sinh khi viết tài liệu này: DR-81 (thư viện bổ sung ngoài DR-02…05: JCS, networknt JSON Schema, Immer, MSW, Awaitility, Spotless, Lettuce và các thư viện kèm theo).
