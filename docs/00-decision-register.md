# Sổ quyết định mở

> Trạng thái: **Approved v1.0** · Cập nhật: 2026-10-07 (mọi DR-01…151 đã Chốt hoặc Đổi; Owner chốt DR-123…130 (ops) theo đề xuất; Owner đổi DR-05, 06, 10, 12, 13, 21, 24, 28, 37, 31, 38, 41, 44, 45, 52, 56, 58, 60, 62, 74; các mục nhỏ do Claude chốt theo ủy quyền) · Nguồn: phân tích `event-ticket-booking-sdd.md` (**SDD gốc**) và canvas thiết kế màn hình "Ticket — Design system & luồng mua vé" (50 artboard)

SDD gốc mạnh ở phần cốt lõi: bất biến chống bán vượt, câu claim `SKIP LOCKED`, trạng thái `EXPIRING` làm trọng tài giữa confirm và expire, idempotency và webhook đều được lập luận kỹ và có thực nghiệm đi kèm. Phần còn thiếu là "chính xác làm thế nào": SDD tự hoãn DDL (SDD gốc mục 2.3, 11), không chốt phiên bản thư viện, không nêu tham số của rate limit, token vào cửa, outbox, và có vài chỗ mâu thuẫn nội tại (trạng thái `REMOVED` của unit, chuyển trạng thái của thanh toán đến trễ, sơ đồ "dùng lại cho nhiều sự kiện" trong khi tài liệu sơ đồ chứa ID loại vé của một sự kiện). Canvas màn hình thêm yêu cầu mà SDD chưa có endpoint hay dữ liệu cho: danh sách sự kiện của studio kèm số vé, sơ đồ tô theo trạng thái ở màn Bán vé, nút "Rời hàng", ảnh sự kiện, email báo đổi lịch, đồng hồ lượt vào 5 phút. Owner chọn giao diện đa ngôn ngữ, điều SDD gốc không đề cập. Các mục chặn P1 và P2 cố định schema, hợp đồng API và bố cục repo; đoán sai ở tầng này tốn kém nhất khi sửa, nên phải chốt trước khi viết code.

**Cách dùng**

- Mỗi mục có trạng thái: `Đề xuất` → (Owner duyệt) → `Chốt` hoặc `Đổi` (ghi lại phương án được chọn).
- Mục đã chốt được chép vào tài liệu đích (dòng *Ghi vào*). Mục ở mức kiến trúc trở thành ADR trong `docs/04-adr/`.
- **⚠ lệch SDD gốc**: đề xuất khác SDD gốc; lý do được nêu ngay trong mục.
- **🔬 spike**: cần một thực nghiệm ngắn (≤ 1 ngày) trước khi chốt; spike `S-xx` nằm ở Phase 0 của master plan.
- Mọi đề xuất đều là trạng thái `Đề xuất` cho tới khi Owner duyệt.

## Nhật ký chốt

| Ngày | Người chốt | Nội dung | Mục bị ảnh hưởng |
| --- | --- | --- | --- |
| 2026-10-05 | Owner | Tài liệu viết bằng tiếng Việt (vocabulary `locales/vi.md`); code, log, mã lỗi API, commit, PR bằng tiếng Anh; giao diện đa ngôn ngữ (chuỗi UI qua i18n, xem DR-10) | Master plan §0.1, DR-07, DR-10 |
| 2026-10-05 | Owner | Claude được tự chốt các câu hỏi nhỏ, dễ đảo ngược khi viết tài liệu, ghi là "Claude (Owner ủy quyền)"; mục mức kiến trúc vẫn chờ Owner | Master plan §0, §7 |
| 2026-10-05 | Owner | Chốt theo đề xuất: tạo PaymentIntent khóa reservation `FOR SHARE` | DR-47, DOC-26 |
| 2026-10-05 | Owner | Đổi: hủy được sự kiện đã mở bán; mọi đơn `PAID` chuyển `REFUND_PENDING`, vé `VOID`, email chờ hoàn tiền | DR-28, DR-18, DR-64, DR-73, DOC-20, DOC-26, DOC-59, UC-13 |
| 2026-10-05 | Owner | Đổi: thanh toán đến trễ không giữ lại vé, đơn sang `REFUND_PENDING`; `NEEDS_REVIEW` thay bằng `REFUND_PENDING`/`REFUNDED` | DR-44, DR-18, DR-19, DR-50, DR-54, DOC-14, DOC-26, DOC-49, DOC-65, UC-15, UC-19 |
| 2026-10-05 | Owner | Đổi: mỗi sự kiện một sơ đồ, dùng lại bằng nhân bản toàn bộ và sinh ID mới | DR-31, DR-16, DR-64, DR-65, ADR-0010, DOC-22, DOC-23, DOC-57, P4-04 |
| 2026-10-05 | Owner | Chốt theo đề xuất: magic link với outbox mã hóa token | DR-21, DOC-19 |
| 2026-10-05 | Owner | Chốt theo đề xuất: kiểm soát tiếp nhận (hàng chờ) luôn bật cho mọi sự kiện đang bán | DR-57, DOC-28 |
| 2026-10-05 | Owner | Chốt theo đề xuất: tình trạng chỗ dạng bitmap | DR-62, DOC-29 |
| 2026-10-05 | Owner | Chốt theo đề xuất: DDL tài khoản, sự kiện, sơ đồ, kho vé (thêm trạng thái unit `REMOVED`), reservation/đơn/vé, bảng vận hành — gồm các chỉnh sửa theo DR-28, DR-31, DR-44 | DR-14–19, DOC-14, DOC-15 |
| 2026-10-06 | Owner | Đổi: Spring Data JDBC thay cho `JdbcClient` thuần; trạng thái chỉ đổi bằng `@Modifying @Query` có điều kiện, SQL phức tạp trong custom fragment | DR-05, DR-02 (S-01), ADR-0012, DOC-11, DOC-12 |
| 2026-10-06 | Owner | Claude được tự chốt mọi mục nhỏ trên toàn bộ sổ (mọi phase); mục mơ hồ thì hỏi Owner | Master plan §0 |
| 2026-10-06 | Owner | Chốt: VND với Stripe test mode (VND là tiền tệ thanh toán Stripe hỗ trợ, không có phần lẻ); chỉ cần tài khoản Stripe đăng ký ở một nước Stripe hỗ trợ, dùng test mode, không cần kích hoạt | DR-13, S-02 |
| 2026-10-06 | Owner | Đổi: ngôn ngữ mặc định (dự phòng) là `vi`; key i18n vẫn viết bằng tiếng Anh | DR-10, DR-14, ADR-0016, DOC-31, DOC-40 |
| 2026-10-06 | Owner | Đổi: bỏ tính năng giới hạn số vé mỗi đơn (bỏ cột `max_tickets_per_order`, ô ở Studio 02, mã `MAX_TICKETS_EXCEEDED`); chỉ giữ giới hạn kỹ thuật kích thước một lệnh giữ vé | DR-41, DR-15, DR-25, DR-64, DOC-03, DOC-04, DOC-43, DOC-46, DOC-47, DOC-55, P2-08 |
| 2026-10-06 | Owner | Chốt theo đề xuất các mục mức kiến trúc: Java 25 + Spring Boot 4, package và luật phụ thuộc, cache session trong tiến trình, JSON Schema sơ đồ, kiến trúc editor, hủy giữ đường nhanh, outbox chỉ email, cổng thanh toán giả | DR-02, DR-06, DR-22, DR-32, DR-39, DR-43, DR-51, DR-53 |
| 2026-10-06 | Owner | Đổi: ảnh lưu ở object storage tương thích S3 (SeaweedFS trong compose), bảng `media` chỉ giữ metadata | DR-38, DR-04, DR-72, DR-74, ADR-0015, DOC-07, DOC-15, DOC-18, DOC-62 |
| 2026-10-06 | Claude (Owner ủy quyền) | Chốt theo đề xuất các mục còn lại: DR-01, 03, 04, 07–09, 11, 12, 20, 23–27, 29, 30, 33–37, 40, 42, 45, 46, 48–50, 52, 54–56, 58–61, 63–78 (DR-11, 12, 20 đã nằm sẵn trong DDL của DR-14–19) | Như từng DR |
| 2026-10-06 | Owner | Đổi: mở ô chọn múi giờ IANA ở form studio (mặc định `Asia/Ho_Chi_Minh`, khóa sau khi xuất bản); tiền tệ giữ chỉ VND | DR-12, DR-25, DOC-20, DOC-31, DOC-40, DOC-55 |
| 2026-10-06 | Owner | Đổi: chỉ VND, gán cứng; bỏ `PLATFORM_CURRENCY`, bảng `minorUnitDigits` và kiểm tra tiền tệ khi khởi động; `orders.currency` CHECK `= 'VND'`; phương án dự phòng của S-02 là nâng `payment.min-amount`, không đổi tiền tệ | DR-13, DR-18, DOC-14, DOC-26, DOC-34 |
| 2026-10-06 | Owner | Cập nhật master plan theo template mới của skill: thêm nhóm luồng chi tiết `06-design/flows/` (DOC-82…90, FL-01…35, mẫu A.8), gate và task tương ứng; không đổi DR nào | Master plan §0.4, §1.3, §3, §5, §6, Phụ lục A; README |
| 2026-10-06 | Owner | Đổi: bên trong mỗi module chia theo layer (n-layer): điểm vào `controller`/`job`/`listener` → `service` → `repository`/`client`, cùng `entity`, `dto`; thay cho gốc/`web`/`internal`, vì dự án để học và người làm là dev backend thiếu kinh nghiệm. Giữ luật modular monolith: module khác chỉ dùng package gốc (`…Api`, DTO, event), mỗi bảng một module sở hữu, không vòng phụ thuộc | DR-06, DR-05, ADR-0002, DOC-07, DOC-12, P1-05 |
| 2026-10-06 | Owner | Đổi theo đề xuất đơn giản hóa của Claude (dự án để học): magic link gửi trực tiếp sau commit, bỏ mã hóa token trong outbox; bỏ token vào cửa, kiểm tra `ZSCORE admitted` theo session; `admit_rate` cố định, bỏ AIMD; bỏ rate limiter dự phòng khi mất Redis; cache tình trạng chỗ bằng Caffeine trong tiến trình; idempotency băm body thô; tiền tố mã vé do người tổ chức nhập; bỏ quét bucket | DR-21, DR-19, DR-38, DR-45, DR-52, DR-56, DR-57, DR-58, DR-60, DR-62, DR-64, DR-74, ADR-0007, ADR-0013, DOC-17, DOC-18, DOC-19, DOC-28, DOC-29, DOC-32, DOC-36, DOC-83, DOC-90, P1-07, P5-03, P6-02, P6-05, P6-06 |
| 2026-10-06 | Owner | Đổi: sơ đồ khóa toàn bộ từ giờ mở bán, kể cả khi chưa ai mua; trước giờ mở bán xuất bản phiên bản mới thì dựng lại kho vé; bỏ diff và `MAP_VERSION_CONFLICT`. Thêm đóng bán sớm (`close-sale`). DR-36 (tự lưu, IndexedDB) giữ nguyên | DR-37, DR-24, DR-40, DR-64, FR-15, UC-09, UC-14, DOC-20, DOC-22, DOC-23, DOC-57, DOC-59, DOC-84, DOC-89, P2-05.1, P5-05.1 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-79 (đồ thị phụ thuộc module, module `studio`), DR-80 (cổng `api` 8081), DR-81 (thư viện bổ sung) khi viết DOC-07, 11, 12 | DR-79, DR-80, DR-81, DOC-07, DOC-11, DOC-12, ADR-0002 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-82–89: Mã lỗi bổ sung, `rule` khác `code`, `retryAfterSeconds`, retry CSRF, cache `GET /events`, xuất OpenAPI, body chặt, trường giờ `…Local` (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-82–89, DOC-35, DOC-36, DOC-37, DOC-62 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-90–95: Ngữ nghĩa đóng reservation, đơn chưa thu tiền khi hủy sự kiện, index khóa ngoại, DDL bảng profile, Flyway profile, index dọn dữ liệu (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-90–95, DOC-14, DOC-15 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-96–115: Gửi SMTP ở `common.mail`, khóa advisory, cookie sliding, token ngoài log, IP, upsert user; `AuthApi` cho email; mã vé, ngân sách relay, lỗi SMTP vĩnh viễn, fan-out, metric, nhãn GA; mẫu email và luồng đăng nhập (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-96–115, DOC-19, DOC-27, DOC-51, DOC-83 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-116–122: Ma trận quyền: 404 cho tài nguyên người khác, `OriginCheckFilter`, `denyAll`, `no-store`; metric bổ sung, tập label đóng, Redis `DOWN` không làm readiness DOWN (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-116–122, DOC-32, DOC-33 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-123–130: Profile `seed`, ba file compose, `PAYMENTS_MODE` khớp profile, khóa Stripe cho frontend, 429 do nginx, E2E riêng, `audit`/`breaking-ok`, branch protection (đề xuất) (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-123–130, DOC-61, DOC-62, DOC-63 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-131–142: Menu tài khoản, tự thử lại, token mở rộng, lint hex, namespace i18n, lỗi theo `code`, định dạng ngày; chi tiết màn Đăng nhập và trang lỗi; tên component do DOC-41 chốt (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-131–142, DOC-38, DOC-39, DOC-40, DOC-41, DOC-44, DOC-52, DOC-82 |
| 2026-10-06 | Claude (Owner ủy quyền) | Thêm DR-143–151: Khóa `saleStartsAt`, loại vé chỉ thêm khi DRAFT, `PUT draft` chỉ kiểm schema; quy ước cấu hình, `ConfigurationGuard`, tên khóa; mã mức test, ID test trong `@DisplayName`, ngưỡng nhánh 75% (khi viết gate P1; nhóm `ops` ở trạng thái Đề xuất) | DR-143–151, DOC-37, DOC-34, DOC-69 |
| 2026-10-06 | Claude (Owner yêu cầu chạy) | Chốt qua spike S-01: giữ Java 25 + Spring Boot 4.0.8, không lùi Java 21; 9 kịch bản tích hợp xanh trên PostgreSQL 18 và SeaweedFS 4.48. Phát hiện: Boot 4 dùng Jackson 3 nên networknt đổi từ 1.5.x sang 3.0.8; SeaweedFS cần `-s3.config` | DR-02, DR-81, ADR-0011, DOC-11 |
| 2026-10-07 | Owner | Chốt theo đề xuất DR-123–130 (ops): profile `seed` và target `make` bổ sung, ba file compose, `PAYMENTS_MODE` khớp profile, khóa công khai Stripe bằng build arg, body 429 của nginx và CSP cho Stripe Elements, `e2e.yml` riêng, job `audit` và `breaking-ok`, branch protection `dev`/`main`. Sổ quyết định và master plan lên `Approved v1.0` (P0-01) | DR-123–130, DOC-61, DOC-62, DOC-63, master plan P0-00, P0-01 |

---

## A. Quy trình và nền tảng

### DR-01 · Bố cục repo, công cụ build và lệnh chuẩn — **Chốt**
- **Vấn đề:** Phụ lục SDD gốc đặt mọi thứ dưới thư mục `event-ticketing/`, nhưng repo hiện tại là `ticket-booking/` và đã có SDD ở gốc. SDD nói "Gradle" mà không nói Kotlin hay Groovy DSL, không nói trình quản lý gói frontend, không có lệnh chuẩn để chạy test hay dựng môi trường.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Monorepo ngay tại gốc repo (không có thư mục `event-ticketing/` lồng thêm):

    ```
    backend/            # Gradle Kotlin DSL, wrapper commit sẵn, một project duy nhất `api`
    frontend/           # pnpm, Vite; package `map-core` là thư mục src/map-core (không tách package npm)
    deploy/compose/     # docker-compose.yml, nginx.conf, .env.example, init scripts
    load/               # k6 scripts (TypeScript, build bằng esbuild của k6)
    experiments/        # runner EXP-01…10 (bash + k6 + SQL) và notebook phân tích
    docs/
    Makefile
    ```
  - Gradle 9.x, Kotlin DSL, version catalog `backend/gradle/libs.versions.toml`. Frontend dùng pnpm 10 với `packageManager` khóa trong `package.json`, Node 22 LTS (`.nvmrc`).
  - `Makefile` ở gốc là giao diện duy nhất: `make up`, `make down`, `make reset`, `make seed`, `make test` (backend + frontend unit), `make it` (integration Testcontainers), `make e2e`, `make lint`, `make invariants`, `make exp EXP=02`.
- **Hệ quả:** Phụ lục SDD gốc được giữ nguyên về nội dung, chỉ bỏ thư mục gốc lồng. CI (DR-08) gọi đúng các target `make`.
- **Ghi vào:** DOC-12, DOC-61, DOC-63.

### DR-02 · Phiên bản Java và Spring Boot — ⚠ lệch SDD gốc · 🔬 spike — **Chốt**
- **Vấn đề:** SDD gốc mục 4.3 chọn Java 21 và Spring Boot 3. Tại 2026-10, Java 25 là bản LTS mới nhất và Spring Boot 4.x là dòng được hỗ trợ; Spring Boot 3.5 đã hết hỗ trợ OSS. Bắt đầu một dự án mới trên dòng đã hết hỗ trợ buộc phải nâng cấp lớn giữa chừng.
- **Các phương án:** (1) Java 21 + Boot 3.5: đúng SDD, thư viện chắc chắn tương thích, nhưng đã hết hỗ trợ OSS; (2) Java 25 + Boot 4.0.x: được hỗ trợ dài, virtual thread và structured logging sẵn, nhưng cần kiểm tra tương thích springdoc, stripe-java, Testcontainers, Spring Modulith.
- **Quyết định (Owner chốt):** Phương án 2: Java 25 (Temurin), Spring Boot 4.0.x bản patch mới nhất tại lúc P1-01, khóa trong version catalog. Spike **S-01** dựng một ứng dụng rỗng với Spring Web, Security, Spring Data JDBC, Flyway (PostgreSQL 18), springdoc-openapi, stripe-java, AWS SDK v2 S3 (với SeaweedFS, DR-38), Spring Modulith, Testcontainers và chạy một test tích hợp. Spike thất bại ở thư viện nào thì lùi về phương án 1 cho toàn bộ dự án, không trộn.
- **Hệ quả:** Không dùng virtual thread cho đường giữ vé ở P2 (số luồng phải nhỏ hơn connection pool, DR-61); để mặc định platform thread, đo lại ở EXP-05.
- **Kết quả S-01 (2026-10-06):** đạt, giữ phương án 2. Boot 4.0.8, Modulith 2.0.8, springdoc 3.0.2, Testcontainers 2.0.5, networknt 3.0.8 (Jackson 3). Chi tiết ở DOC-11 §8.
- **Ghi vào:** DOC-11, ADR-0011.

### DR-03 · Stack và phiên bản frontend — **Chốt**
- **Vấn đề:** SDD gốc mục 4.3 nêu React, TypeScript, Vite, TanStack Query, React Router, Konva, rbush nhưng không có phiên bản, không có thư viện form, state của editor, styling, cách sinh client từ OpenAPI, i18n.
- **Quyết định (Claude chốt, Owner ủy quyền):**

  | Mảng | Lựa chọn | Lý do |
  | --- | --- | --- |
  | Ngôn ngữ, build | TypeScript 5.9 `strict`, Vite 7 | SDD gốc 4.3 |
  | UI | React 19 | SDD gốc 4.3 |
  | Routing | React Router 7 (data router, `createBrowserRouter`, lazy route) | Tách bundle theo route (SDD gốc 13.2) |
  | Server state | TanStack Query 5 | SDD gốc 13.2 |
  | Store của editor | Zustand 5 (vanilla store, không qua React context) | Store tách khỏi chu trình render (SDD gốc 13.2), DR-39 |
  | Canvas | Konva 10 + react-konva 19; rbush 4 | SDD gốc 4.3 |
  | Form | react-hook-form 7 + zod 4 | Lỗi theo ô như màn Studio 02, 03 |
  | API client | `openapi-typescript` sinh kiểu + `openapi-fetch` | Client mỏng, không sinh code runtime |
  | i18n | i18next + react-i18next, ICU qua `i18next-icu` | DR-10 |
  | Styling | CSS Modules + CSS custom properties từ token (DR-68) | Không thêm framework CSS |
  | Thanh toán | `@stripe/stripe-js` + `@stripe/react-stripe-js` | SDD gốc 4.3 |
  | Test | Vitest 3, fast-check 4, Testing Library, Playwright 1.5x | SDD gốc 15.1 |
  | Lint | ESLint 9 flat config, Prettier 3 | |
- **Hệ quả:** `frontend/src/api/schema.d.ts` sinh từ `/v3/api-docs` bằng `pnpm gen:api`; CI kiểm tra file sinh ra không lệch (DR-08).
- **Ghi vào:** DOC-11, DOC-12.

### DR-04 · Phiên bản các image hạ tầng — **Chốt**
- **Vấn đề:** SDD gốc mục 14.1 liệt kê container mà không có phiên bản.
- **Quyết định (Claude chốt, Owner ủy quyền):** Khóa tag theo minor, cập nhật có chủ đích:

  | Container | Image | Ghi chú |
  | --- | --- | --- |
  | `postgres` | `postgres:18-alpine` | Có hàm `uuidv7()` sẵn (DR-11) |
  | `redis` | `redis:8.2-alpine` | Giấy phép AGPLv3/RSALv2/SSPL; dùng nội bộ, không sửa mã nguồn nên không ảnh hưởng. Valkey 8 thay thế được không đổi code nếu cần |
  | `nginx` | `nginx:1.28-alpine` | |
  | `mailpit` | `axllent/mailpit:v1.27` | |
  | `stripe-cli` | `stripe/stripe-cli:v1.30` | |
  | `storage` | `chrislusf/seaweedfs`, khóa tag theo minor tại lúc P1-01 | Object storage tương thích S3 cho ảnh (DR-38) |
  | `api` | build từ `eclipse-temurin:25-jre-alpine` | |
- **Ghi vào:** DOC-11, DOC-62.

### DR-05 · Truy cập dữ liệu và kiểm tra ranh giới module — **Đổi: Spring Data JDBC**
- **Vấn đề:** SDD gốc mục 4.3 chọn Spring JDBC; mục 4.1 yêu cầu module "chỉ gọi nhau qua interface công khai, không truy vấn bảng của nhau" nhưng không nói ép bằng gì. Transaction giữ vé lại phải chạm `idempotency_key`, `inventory_unit`, `reservation`, `orders` của bốn module trong một transaction. Mọi đổi trạng thái phải là câu ghi có điều kiện (SDD gốc 4.2), trong khi `save()` của Spring Data ghi đè cả dòng không điều kiện.
- **Quyết định (Owner chốt):** Spring Data JDBC (dòng đi kèm Spring Boot 4, kiểm tra trong S-01); không JPA.
  - Mỗi module có repository `ListCrudRepository` cho aggregate của mình. Aggregate: `AppUser`; `Organizer`; `Event` (có `@Version` trên `row_version`, dùng cho `PATCH` và lỗi `STALE_EVENT_VERSION`); `TicketType`; `SeatMap`; `SeatMapVersion`; `Reservation` kèm `reservation_item` qua `@MappedCollection`; `Order`; `Ticket`; `OutboxMessage`; `Media`. `inventory_unit` và `inventory_pool` không là aggregate có `save()`: chỉ đọc bằng repository, ghi bằng câu SQL riêng.
  - **Luật đổi trạng thái:** không bao giờ đổi `status` bằng `save()`. Mọi chuyển trạng thái là method `@Modifying @Query("UPDATE … WHERE … AND status = :from")` trả `int` số dòng, service kiểm tra số dòng. `save()` chỉ dùng để chèn dòng mới và sửa trường không phải trạng thái (tên, mô tả, giá, bản nháp sơ đồ).
  - ID gán trước bằng UUIDv7 (DR-11): chèn bằng `JdbcAggregateTemplate.insert(…)` (hoặc entity cài `Persistable.isNew()`), vì `save()` với ID khác null sẽ chạy `UPDATE`.
  - Câu lệnh mà `@Query` không diễn đạt gọn (claim `SKIP LOCKED` có CTE và `RETURNING`, chèn hàng loạt bằng `generate_series`, diff phiên bản sơ đồ, snapshot tình trạng, kiểm tra bất biến) nằm trong custom repository fragment (`InventoryUnitClaims` + `…Impl`) dùng `JdbcClient`. Vẫn là SQL viết tay, ở trong module sở hữu bảng.
  - Cột `jsonb` (`seat_map.draft`, `seat_map_version.document`, `reservation_item.label`, `ticket.label`, `outbox.payload`) ánh xạ bằng cặp `@ReadingConverter`/`@WritingConverter` qua `PGobject`; cột trạng thái ánh xạ Java `enum` ↔ `text`.
  - Transaction mở ở service của module điều phối (`reservation.service.HoldService`), module khác tham gia qua interface `…Api` của mình, chạy trong cùng transaction (DR-06) (propagation `MANDATORY` cho method chỉ được gọi trong transaction).
  - Ranh giới module kiểm tra bằng Spring Modulith (`ApplicationModules.of(Application.class).verify()` trong một unit test) cộng ArchUnit: entity và repository của module X chỉ được dùng trong X; chuỗi SQL chỉ nhắc bảng mà X sở hữu (danh sách ở DOC-07).
- **Hệ quả:** Bớt mã lặp cho CRUD của studio và các bảng vận hành; giữ được SQL viết tay cho đường nóng. Spring Data JDBC không có lazy loading nên không có N+1 ẩn, nhưng aggregate có `@MappedCollection` bị xóa-chèn lại toàn bộ con khi `save()`: `Reservation` chỉ `save()` một lần lúc chèn. Thêm test kiến trúc: không method nào ngoài lớp chèn gọi `save()` trên aggregate có cột `status`. Checklist review ở DOC-12 thêm mục này.
- *Đổi 2026-10-06:* đề xuất cũ là `JdbcClient` cho mọi truy vấn, không dùng Spring Data.
- **Ghi vào:** ADR-0012, DOC-11, DOC-12, DOC-07.

### DR-06 · Cấu trúc package trong mỗi module và luật phụ thuộc — **Đổi: kiến trúc n-layer bên trong, luật modular monolith bên ngoài**
- **Vấn đề:** SDD gốc liệt kê module nhưng không nói bên trong mỗi module chia thế nào. Dự án dùng để học và người viết code là dev backend chưa nhiều kinh nghiệm: cấu trúc phải là thứ họ đã quen từ tài liệu Spring phổ biến, nhìn tên package là biết đặt class ở đâu. Đồng thời vẫn phải giữ ranh giới module của SDD gốc mục 4.1 (DR-05).
- **Quyết định (Owner chốt):** Gói gốc `io.ticket`. Mỗi module nghiệp vụ `io.ticket.<module>` là một module của Spring Modulith; bên trong chia theo layer, mỗi layer một package:

  | Package | Layer | Nội dung | Được gọi bởi |
  | --- | --- | --- | --- |
  | `<module>` (gốc) | API công khai của module | Interface `…Api` (ví dụ `InventoryApi`), `record` DTO trao đổi giữa module, event của module (`OrderPaid`) | Module khác |
  | `<module>.controller` | Điểm vào | `@RestController`, webhook endpoint: nhận request, validate hình thức (`@Valid`), gọi service, trả response | Không ai (HTTP) |
  | `<module>.job` | Điểm vào | `@Scheduled` (`…Job`): đọc lịch/cấu hình, gọi service | Không ai (lịch) |
  | `<module>.listener` | Điểm vào | Nghe event của module khác (`…Listener`), gọi service | Không ai (event) |
  | `<module>.service` | Service | `@Service` chứa logic nghiệp vụ và ranh giới transaction (`@Transactional` chỉ đặt ở đây); cài đặt `…Api` của module; mapper viết tay entity ↔ DTO (`…Mapper`) | Điểm vào cùng module |
  | `<module>.repository` | Truy cập dữ liệu | Repository Spring Data JDBC, custom fragment `JdbcClient` (DR-05), truy cập Redis (`…RedisRepository`) | Service cùng module |
  | `<module>.client` | Truy cập hệ thống ngoài | Stripe (`PaymentGateway` và hai bản cài đặt, DR-51), S3 (DR-38), SMTP; chỉ module nào cần mới có | Service cùng module |
  | `<module>.entity` | (dữ liệu) | Entity ánh xạ bảng, enum trạng thái | Repository, service cùng module |
  | `<module>.dto` | (dữ liệu) | `record` request/response của REST API | Controller, service cùng module |

  Không phải module nào cũng có đủ package; tạo package khi có class đầu tiên. Ví dụ module `inventory`: `InventoryApi`, `ClaimResult` (gốc); `controller/AvailabilityController`; `job/ReleaseExpiredHoldsJob`; `listener/EventPublishedListener`; `service/InventoryService implements InventoryApi`, `service/InventoryMapper`; `repository/InventoryUnitRepository` + `InventoryUnitClaims` + `InventoryUnitClaimsImpl`; `entity/InventoryUnit`; `dto/AvailabilityResponse`.

  **Luật layer (trong một module):**
  - Chiều gọi chỉ đi xuống: điểm vào (`controller`, `job`, `listener`) → `service` → `repository`/`client`. Điểm vào không gọi repository hay client và không gọi lẫn nhau; repository và client không gọi service.
  - `@Transactional` chỉ ở `service`; điểm vào không tự mở transaction.
  - Entity không ra khỏi layer service: controller nhận và trả DTO, service đổi entity ↔ DTO bằng mapper.

  **Luật modular monolith (giữa các module), giữ như SDD gốc 4.1 và DR-05:**
  - Mỗi bảng PostgreSQL và họ key Redis có đúng một module sở hữu (DOC-07); chỉ repository của module đó được đọc/ghi. Không module nào truy vấn bảng của module khác, kể cả bằng SQL thuần hay join.
  - Module khác chỉ được dùng package gốc (`…Api`, DTO, event); mọi package con (`controller`, `job`, `listener`, `service`, `repository`, `client`, `entity`, `dto`) là nội bộ. Đây là mặc định của Spring Modulith, không cần `@NamedInterface`.
  - Gọi đồng bộ: service của module A gọi `BApi` của module B (inject interface, không inject class cài đặt). Báo ngược chiều hoặc báo cho nhiều module: publish event của module, module nhận xử lý ở `listener`. Không có vòng phụ thuộc giữa các module.
  - `common` (cấu hình, xử lý lỗi `@RestControllerAdvice`, i18n, bảo mật, tiện ích) là module dùng chung, không phụ thuộc module nghiệp vụ. Module bổ sung so với SDD gốc: `media` (DR-38); `i18n` không phải module (chỉ là cấu hình trong `common`).

  **Kiểm tra tự động:** Spring Modulith `ApplicationModules.verify()` (chỉ dùng package gốc của module khác, không vòng phụ thuộc); ArchUnit `layeredArchitecture()` cho chiều gọi trong module, "`@Transactional` chỉ trong `..service..`", "`..controller..` không dùng `..entity..`", và luật sở hữu bảng của DR-05.
- **Hệ quả:** Người mới chỉ cần nhớ một khuôn package cho mọi module; ranh giới module vẫn được ép bằng test, không dựa vào review. Mỗi module có thêm một interface `…Api` cho phần dùng chung; method chỉ module đó dùng không đưa lên interface. Cổng thanh toán (DR-51) vẫn là interface với hai bản cài đặt, đặt ở `payment.client`.
- *Đổi 2026-10-06:* đề xuất cũ chia mỗi module thành gốc (interface `…Api`), `web` (controller, DTO) và `internal` (service, repository, entity, job); Owner đổi sang chia theo layer vì dự án để học và người làm là dev backend thiếu kinh nghiệm, giữ nguyên luật ranh giới module.
- **Ghi vào:** DOC-12, DOC-07, ADR-0002.

### DR-07 · Quy trình Git, commit và ngôn ngữ — **Chốt**
- **Vấn đề:** SDD gốc không nói nhánh, quy ước commit, ngôn ngữ code.
- **Quyết định (Claude chốt, Owner ủy quyền):** `main` luôn chạy được; làm việc trên `dev` và nhánh `feat/<task-id>-<slug>`, merge vào `dev` qua PR, `dev` → `main` ở mỗi milestone. Conventional Commits tiếng Anh, scope là module (`feat(inventory): claim pool units with skip locked`), footer `Refs: P2-06`. Code, log, mã lỗi, tên bảng, commit, tiêu đề PR bằng tiếng Anh; chuỗi giao diện qua i18n (DR-10); tài liệu `docs/` bằng tiếng Việt. Migration Flyway đặt tên `V<yyyymmddHHmm>__<snake_case>.sql`.
- **Ghi vào:** master plan §7.3, DOC-63.

### DR-08 · CI tối thiểu ngay từ P1 — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 14.1 để CI/CD sang giai đoạn sau. Nhưng chứng minh "không bán vượt" dựa vào test đồng thời và test tích hợp chạy lặp lại; không có CI thì hồi quy không bị phát hiện.
- **Quyết định (Claude chốt, Owner ủy quyền):** GitHub Actions, một workflow `ci.yml` chạy trên PR và push `dev`/`main`: (1) `make lint`; (2) `make test`; (3) `make it` (Testcontainers, runner `ubuntu-latest` đủ chạy PostgreSQL + Redis); (4) kiểm tra `schema.d.ts` khớp OpenAPI; (5) build image `api` và bundle frontend. Chặn merge khi đỏ. Không có CD; E2E và thực nghiệm tải chạy tay.
- **Ghi vào:** DOC-63.

### DR-09 · Log và metric tối thiểu cho thực nghiệm — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc để observability sang sau (mục 2.3) nhưng EXP-05 cần p95, số connection đang dùng, thời gian giữ connection; mục 12.3 yêu cầu `X-Request-Id` trùng `trace_id` trong log.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Log JSON bằng structured logging có sẵn của Spring Boot (format `ecs`), trường bắt buộc: `@timestamp`, `log.level`, `message`, `trace_id`, `user_id` (nếu có), `event_id`, `reservation_id`, `order_id`. Email không bao giờ vào log (DR-22).
  - Actuator mở `health`, `prometheus` trên cổng quản trị 9090 (không qua nginx). Metric Hikari, HTTP server và metric riêng: `ticket_hold_duration_seconds` (histogram), `ticket_hold_result_total{result}`, `ticket_connection_hold_seconds`, `ticket_expiry_batch_size`, `ticket_outbox_pending`, `ticket_queue_admitted`.
  - Compose profile `obs` thêm Prometheus + Grafana với một dashboard dựng sẵn; không bật mặc định. Không alerting.
- **Hệ quả:** Thêm hai container chỉ khi chạy thực nghiệm; NFR-08 (`docker compose up`) không đổi.
- **Ghi vào:** DOC-33, DOC-62.

### DR-10 · Giao diện đa ngôn ngữ — ⚠ lệch SDD gốc — **Đổi: `vi` là ngôn ngữ mặc định**
- **Vấn đề:** Owner chọn giao diện đa ngôn ngữ; SDD gốc không có i18n và canvas thiết kế chỉ có chuỗi tiếng Việt. Email (magic link, vé, đổi lịch) cũng là giao diện.
- **Quyết định (Owner chốt):**
  - Hai locale ở giai đoạn này: `vi` (mặc định và dự phòng, chuỗi lấy từ canvas) và `en`. Key viết bằng tiếng Anh theo nghĩa (`checkout.hold.expired.title`), không phải câu tiếng Anh. Thêm locale là thêm file, không đổi code.
  - Frontend: i18next, namespace theo feature (`events`, `checkout`, `studio`, `editor`, `common`), file `frontend/src/locales/<lng>/<ns>.json`, key dạng `checkout.hold.expired.title`. Chọn ngôn ngữ: `app_user.locale` nếu đã đăng nhập → cookie `tb_lang` → `Accept-Language` → `vi`. Bộ chọn ngôn ngữ ở header. `fallbackLng` của i18next là `vi`.
  - Backend: `MessageSource` với `messages_vi.properties` (mặc định, `setDefaultLocale(vi)`, `fallbackToSystemLocale = false`) và `messages_en.properties` cho email và cho trường `title`/`detail` của Problem Details (mã `code` luôn tiếng Anh). Email gửi theo `app_user.locale` lúc ghi outbox.
  - Định dạng số, tiền, ngày qua `Intl` theo locale; tiền VND luôn hiện không có phần lẻ (`1.800.000 ₫` ở `vi`, `₫1,800,000` ở `en`). Giờ sự kiện hiện theo múi giờ của sự kiện (DR-12), không theo máy người xem.
  - Tài liệu sơ đồ và tên do người tổ chức nhập (tên sự kiện, loại vé, khu vực) không dịch.
- **Hệ quả:** Mọi microcopy trong DOC-40 có cột `vi` và `en`; màn hình spec trích key, không trích chuỗi cứng. CI kiểm tra hai locale có cùng tập key (script `pnpm i18n:check`, và test JUnit cho `messages_*.properties`).
- *Đổi 2026-10-06:* đề xuất cũ lấy `en` làm ngôn ngữ mặc định và dự phòng.
- **Ghi vào:** DOC-31, ADR-0016, DOC-40, DOC-27.

### DR-79 · Đồ thị phụ thuộc giữa module, module `studio`, ngoại lệ chỉ đọc của `invariant` — **Chốt** (Claude, Owner ủy quyền)
- **Vấn đề:** DR-05/06 cấm vòng phụ thuộc nhưng không nêu đồ thị cụ thể. Các transaction liên module (xuất bản, hủy event, xác nhận thanh toán) và việc job trả vé cần hủy PaymentIntent tạo vòng nếu để mỗi module gọi tự do; kiểm tra bất biến cần đọc mọi bảng.
- **Quyết định:** Đồ thị ở DOC-07 §3.1 và `allowedDependencies` ở DOC-12 §2.2. Thêm module `studio` (API người tổ chức cho event và các thao tác xuyên module: xuất bản, hủy, đổi lịch). Đảo chiều bằng SPI: `reservation.PaymentIntentCanceller` do `payment` cài đặt; `common.RetentionContributor` do từng module cài đặt cho `RetentionJob`. Tài liệu sơ đồ truyền cho `InventoryApi.createSeatUnits` dưới dạng tham số `jsonb` để SQL của `inventory` chỉ nhắc bảng của mình. Package `io.ticket.invariant` được `SELECT` mọi bảng, cấm ghi (ArchUnit).
- **Hệ quả:** Module bổ sung so với SDD gốc: `media`, `studio`. Transaction liên module có module điều phối cố định (DOC-07 §4.3).
- **Ghi vào:** DOC-07, DOC-12, ADR-0002.

### DR-80 · Thông số hạ tầng nhỏ — **Chốt** (Claude, Owner ủy quyền)
- **Quyết định:** Cổng nội bộ `api` là 8081 (`server.port`), quản trị 9090, để `make dev` (API chạy trên host) không đụng cổng 8080 của nginx.
- **Ghi vào:** DOC-07, DOC-62, DOC-34.

### DR-81 · Thư viện và package kỹ thuật bổ sung — **Chốt** (Claude, Owner ủy quyền)
- **Quyết định:** Thêm ngoài DR-02…05: JCS `io.github.erdtman:java-json-canonicalization`, `com.networknt:json-schema-validator`, Spring Data Redis (Lettuce), Awaitility, Spotless + google-java-format, Immer, MSW, `oasdiff`, `@fontsource/*`. Package kỹ thuật `<module>.config` (`@Configuration`, `@ConfigurationProperties`) không phải layer. Ngưỡng JS ban đầu trang sự kiện ≤ 200 KB gzip kiểm bằng script kích thước.
- **Ghi vào:** DOC-11, DOC-12.

---

## B. Quy ước dữ liệu

### DR-11 · Khóa chính và định danh công khai — **Chốt**
- **Vấn đề:** SDD gốc dùng `id`, `reservation_id`, `unit_id` mà không nói kiểu. ID xuất hiện trong URL (`/orders/{id}`) nên không được đoán được; ID dạng tăng dần làm lộ số đơn.
- **Các phương án:** (1) `bigint identity` + ID công khai riêng: hai cột cho mỗi bảng; (2) UUIDv4: ngẫu nhiên, index B-tree phân mảnh khi chèn nhiều (`inventory_unit` 100.000 dòng mỗi lần xuất bản); (3) UUIDv7: có thứ tự thời gian, chèn vào cuối index, không đoán được phần ngẫu nhiên.
- **Quyết định (Claude chốt, Owner ủy quyền):** Phương án 3. Mọi bảng nghiệp vụ có khóa chính `uuid` tên `<bảng>_id`, mặc định `uuidv7()` của PostgreSQL 18; code Java sinh trước UUIDv7 khi cần biết ID trước khi chèn (`reservation_id` được sinh trước khi claim unit, DR-41). Ngoại lệ: ID ghế, khu vực, hàng trong tài liệu sơ đồ do client sinh (`crypto.randomUUID()`, UUIDv4 cho ghế; chuỗi `zone-<8 ký tự>`/`row-<8 ký tự>` cho đối tượng khác, DR-32). ID của Stripe (`pi_…`, `evt_…`) lưu kiểu `text`.
- **Ghi vào:** DOC-14, ADR-0018.

### DR-12 · Thời gian và múi giờ — **Đổi: người tổ chức chọn múi giờ của sự kiện**
- **Vấn đề:** SDD gốc mục 12.3 nói thời gian là ISO 8601 theo UTC và mục 17 nói mọi so sánh dùng `now()` của database, nhưng không nói sự kiện hiển thị theo múi giờ nào. Canvas hiện "Thứ Bảy 14.11.2026 · 20:00" không kèm múi giờ; người tổ chức nhập giờ địa phương ở form `datetime-local`.
- **Quyết định (Claude chốt, Owner ủy quyền; Owner đổi phần múi giờ):**
  - Mọi cột thời điểm là `timestamptz`; API trả chuỗi UTC dạng `2026-11-14T13:00:00Z`.
  - Mỗi event có cột `timezone` (tên IANA), mặc định cấu hình `PLATFORM_TIMEZONE=Asia/Ho_Chi_Minh`. Bước Thông tin ở studio có ô chọn múi giờ (combobox tìm được, danh sách lấy từ `Intl.supportedValuesOf('timeZone')`, mặc định `PLATFORM_TIMEZONE`). Server chỉ nhận tên có trong `ZoneId.getAvailableZoneIds()`, không nhận offset dạng `+07:00`; tên lạ → 422 `invalid_timezone` (DR-25).
  - Múi giờ đổi tự do khi event còn `DRAFT`, khóa sau khi xuất bản (422 `locked_after_publish`). Lý do: đổi múi giờ là dịch mọi mốc `starts_at`, `ends_at`, `sale_*` cùng lúc, kéo theo email đổi lịch (DR-29) và có thể mở hoặc đóng bán ngay; muốn sửa thì sửa giờ, như mọi lần đổi lịch.
  - Đổi múi giờ trong bản nháp giữ nguyên giờ trên form (giờ địa phương), mốc UTC được tính lại ở lần lưu kế tiếp.
  - Giờ hiển thị có thêm hậu tố offset (`timeZoneName: 'shortOffset'`, ví dụ `20:00 GMT+9`) khi múi giờ của event khác `PLATFORM_TIMEZONE`, ở cả giao diện và email; event theo múi giờ mặc định hiển thị như canvas, không hậu tố.
  - Form studio gửi giờ địa phương kèm `timezone` của event; server chuyển sang UTC. Giao diện hiển thị theo `timezone` của event bằng `Intl.DateTimeFormat(locale, { timeZone })`, không theo máy người xem.
  - So sánh hạn (giữ vé, token, khung mở bán) luôn trong SQL bằng `now()`; Java không gọi `Instant.now()` cho các quyết định đó.
- *Đổi 2026-10-06:* đề xuất cũ khóa ô múi giờ ở `PLATFORM_TIMEZONE` (một quốc gia). Tiền tệ vẫn chỉ VND (DR-13).
- **Ghi vào:** DOC-14, DOC-36, DOC-31, DOC-20, DOC-55, DOC-40.

### DR-13 · Tiền, tiền tệ và giới hạn của Stripe — 🔬 spike — **Đổi: chỉ VND, không cấu hình tiền tệ**
- **Vấn đề:** SDD gốc mục 6.2 chọn một tiền tệ cấu hình `PLATFORM_CURRENCY` (mặc định VND), lưu số nguyên đơn vị nhỏ nhất; mục 17 để ngỏ quốc gia của tài khoản Stripe. VND là tiền tệ không có phần lẻ ở Stripe (số tiền gửi lên là số đồng), Stripe có mức thu tối thiểu theo tiền tệ thanh toán, và tài khoản Stripe không mở được ở Việt Nam nên phải dùng tài khoản nước khác nhận tiền VND (presentment currency).
- **Quyết định (Owner chốt):**
  - Hệ thống chỉ dùng VND, gán cứng: không có biến cấu hình `PLATFORM_CURRENCY`, không có bảng hệ số đơn vị lẻ. Cột tiền là `bigint` tính bằng đồng; VND không có phần lẻ ở Stripe nên `amount` gửi Stripe chính là số đồng, `currency = "vnd"`.
  - `orders.currency` vẫn giữ với `CHECK (currency = 'VND')` để đối chiếu với PaymentIntent khi nhận webhook (DR-44); muốn thêm tiền tệ sau này chỉ cần một migration nới CHECK.
  - Frontend định dạng bằng `Intl.NumberFormat(locale, { style: 'currency', currency: 'VND' })` (DR-10).
  - Giá loại vé: `0` (miễn phí) hoặc trong khoảng `[payment.min-amount, payment.max-amount]`, mặc định VND `[20000, 100000000]`. Tổng đơn khác 0 luôn ≥ `payment.min-amount` vì mỗi vé có giá đã ≥ mức đó.
  - Tài khoản: Stripe test mode trên một tài khoản đăng ký ở nước Stripe hỗ trợ (chưa cần kích hoạt hay xác minh doanh nghiệp vì chỉ dùng khóa `sk_test_…`); VND là tiền tệ thanh toán (presentment currency) được hỗ trợ và là tiền tệ không có phần lẻ. Mức tối thiểu thật phụ thuộc tiền tệ quyết toán của tài khoản, nên S-02 đo trên chính tài khoản đó.
  - Spike **S-02** trên Stripe test mode: tạo PaymentIntent VND chỉ thẻ, đo mức tối thiểu thật, thử hủy ở các trạng thái `requires_payment_method`, `requires_action`, `processing`, `succeeded`, ghi lại mã lỗi trả về; xác nhận stripe-cli forward webhook trong compose. Kết quả chỉnh `payment.min-amount` và DR-47.
- *Đổi 2026-10-06:* đề xuất cũ cho cấu hình một tiền tệ `PLATFORM_CURRENCY` kèm bảng `minorUnitDigits` (VND 0, USD 2) và kiểm tra khi khởi động; Owner chỉ cần VND nên bỏ.
- **Ghi vào:** DOC-14, DOC-26, DOC-34.

---

## C. Schema cơ sở dữ liệu

SDD gốc mục 11 cố ý chỉ nêu mô hình khái niệm. DDL dưới đây đã chốt; DOC-14 và DOC-15 chép lại đầy đủ kèm comment. Quy ước chung: trạng thái là `text` + `CHECK` (đổi giá trị bằng migration đơn giản hơn `ENUM`), mọi bảng có `created_at timestamptz NOT NULL DEFAULT now()`, khóa ngoại có index.

### DR-14 · DDL tài khoản, tổ chức, token đăng nhập, session — **Chốt**
- **Vấn đề:** SDD gốc mục 5 mô tả hành vi nhưng không có cột; canvas thêm "Email liên hệ" của tổ chức (không bắt buộc) và ngôn ngữ người dùng (DR-10).
- **Quyết định (Owner chốt):**

  ```sql
  CREATE TABLE app_user (
    user_id       uuid PRIMARY KEY DEFAULT uuidv7(),
    email         text NOT NULL,                       -- đã chuẩn hóa, DR-21
    locale        text NOT NULL DEFAULT 'vi' CHECK (locale IN ('vi','en')),   -- DR-10: đổi mặc định 2026-10-06
    created_at    timestamptz NOT NULL DEFAULT now(),
    last_login_at timestamptz
  );
  CREATE UNIQUE INDEX app_user_email_uq ON app_user (email);

  CREATE TABLE organizer (
    organizer_id  uuid PRIMARY KEY DEFAULT uuidv7(),
    owner_user_id uuid NOT NULL UNIQUE REFERENCES app_user,   -- 1 tài khoản ↔ tối đa 1 tổ chức
    name          text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    contact_email text,                                        -- NULL: dùng email đăng nhập
    created_at    timestamptz NOT NULL DEFAULT now()
  );

  CREATE TABLE login_token (
    token_hash    bytea PRIMARY KEY,                -- SHA-256 của token thô 32 byte
    email         text NOT NULL,
    return_to     text,                             -- đường dẫn tương đối đã kiểm tra
    locale        text NOT NULL,
    requested_ip  inet,
    created_at    timestamptz NOT NULL DEFAULT now(),
    expires_at    timestamptz NOT NULL,
    used_at       timestamptz,
    superseded_at timestamptz                       -- bị thay bởi token mới hơn của cùng email
  );
  CREATE INDEX login_token_email_idx ON login_token (email, created_at DESC);

  CREATE TABLE session (
    session_hash  bytea PRIMARY KEY,                -- SHA-256 của session ID thô 32 byte
    user_id       uuid NOT NULL REFERENCES app_user,
    csrf_token    text NOT NULL,                    -- 32 byte base64url, DR-22
    created_at    timestamptz NOT NULL DEFAULT now(),
    last_seen_at  timestamptz NOT NULL DEFAULT now(),
    revoked_at    timestamptz
  );
  CREATE INDEX session_user_idx ON session (user_id);
  ```
- **Hệ quả:** Vai trò `ORGANIZER` không lưu cột riêng: có dòng `organizer` là có vai trò (DR-23).
- **Ghi vào:** DOC-14 (`app_user`, `organizer`), DOC-15 (`login_token`, `session`).

### DR-15 · DDL sự kiện và loại vé — **Chốt**
- **Vấn đề:** SDD gốc mục 6 nêu thuộc tính và trạng thái; canvas thêm mô tả, ảnh, cờ "Bật phòng chờ khi mở bán", tiền tố mã vé (`GM-…`), màu loại vé theo thứ tự tạo. Bản nháp phải lưu được khi chưa đủ trường (nút "Lưu bản nháp" ở Studio 02).
- **Quyết định (Owner chốt):**

  ```sql
  CREATE TABLE event (
    event_id              uuid PRIMARY KEY DEFAULT uuidv7(),
    organizer_id          uuid NOT NULL REFERENCES organizer,
    status                text NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','PUBLISHED','PAUSED','ENDED','CANCELLED')),
    name                  text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    description           text NOT NULL DEFAULT '' CHECK (char_length(description) <= 5000),
    venue                 text NOT NULL DEFAULT '' CHECK (char_length(venue) <= 200),
    timezone              text NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    image_media_id        uuid REFERENCES media,                  -- DR-38
    starts_at             timestamptz,
    ends_at               timestamptz,
    sale_starts_at        timestamptz,
    sale_ends_at          timestamptz,
    high_demand           boolean NOT NULL DEFAULT false,         -- DR-57
    ticket_code_prefix    text NOT NULL CHECK (ticket_code_prefix ~ '^[A-Z]{2}$'),  -- DR-52
    seat_map_version_id   uuid REFERENCES seat_map_version,
    row_version           int  NOT NULL DEFAULT 0,                -- optimistic lock cho PATCH
    published_at          timestamptz,
    cancelled_at          timestamptz,
    ended_at              timestamptz,
    created_at            timestamptz NOT NULL DEFAULT now(),
    updated_at            timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT event_schedule_ck CHECK (
      status = 'DRAFT' OR (
        starts_at IS NOT NULL AND ends_at > starts_at
        AND sale_starts_at IS NOT NULL AND sale_ends_at > sale_starts_at
        AND sale_ends_at <= starts_at))
  );
  CREATE INDEX event_public_idx ON event (starts_at) WHERE status IN ('PUBLISHED','PAUSED');
  CREATE INDEX event_organizer_idx ON event (organizer_id, created_at DESC);

  CREATE TABLE ticket_type (
    ticket_type_id uuid PRIMARY KEY DEFAULT uuidv7(),
    event_id       uuid NOT NULL REFERENCES event,
    name           text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
    model          text NOT NULL CHECK (model IN ('SEAT','ZONE','GA')),
    price          bigint NOT NULL CHECK (price >= 0),
    ga_capacity    int CHECK (ga_capacity BETWEEN 1 AND 100000),
    color_index    smallint NOT NULL CHECK (color_index BETWEEN 1 AND 5),
    sort_order     smallint NOT NULL,
    deleted_at     timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ticket_type_ga_ck CHECK ((model = 'GA') = (ga_capacity IS NOT NULL))
  );
  CREATE UNIQUE INDEX ticket_type_name_uq ON ticket_type (event_id, lower(name)) WHERE deleted_at IS NULL;
  ```
- **Hệ quả:** Ràng buộc `event_schedule_ck` ép quy tắc "đóng bán không muộn hơn giờ bắt đầu" có trên canvas (Studio 02) nhưng không có trong SDD gốc (DR-25). Cột `max_tickets_per_order` bị bỏ ngày 2026-10-06 theo DR-41.
- **Ghi vào:** DOC-14.

### DR-16 · DDL sơ đồ và phiên bản sơ đồ — **Chốt**
- **Vấn đề:** SDD gốc mục 7.8 và 11.1 nêu bản nháp kèm `revision` và phiên bản bất biến kèm checksum, không có cột; "bất biến" không nói được ép thế nào.
- **Quyết định (Owner chốt):**

  ```sql
  CREATE TABLE seat_map (
    seat_map_id       uuid PRIMARY KEY DEFAULT uuidv7(),
    event_id          uuid NOT NULL UNIQUE REFERENCES event,    -- một sơ đồ cho mỗi event; dùng lại bằng nhân bản, DR-31
    organizer_id      uuid NOT NULL REFERENCES organizer,
    name              text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    draft             jsonb NOT NULL,
    draft_revision    int  NOT NULL DEFAULT 0,
    draft_updated_at  timestamptz NOT NULL DEFAULT now(),
    latest_version_no int  NOT NULL DEFAULT 0,
    cloned_from_seat_map_id uuid REFERENCES seat_map,          -- chỉ để lưu vết, DR-31
    created_at        timestamptz NOT NULL DEFAULT now()
  );

  CREATE TABLE seat_map_version (
    seat_map_version_id uuid PRIMARY KEY DEFAULT uuidv7(),
    seat_map_id         uuid NOT NULL REFERENCES seat_map,
    version_no          int  NOT NULL,
    document            jsonb NOT NULL,
    checksum            bytea NOT NULL,                -- SHA-256 của JSON chuẩn hóa, DR-32
    seat_count          int  NOT NULL,
    sellable_seat_count int  NOT NULL,                 -- không tính ghế blocked
    zone_count          int  NOT NULL,
    published_at        timestamptz NOT NULL DEFAULT now(),
    UNIQUE (seat_map_id, version_no)
  );

  CREATE FUNCTION forbid_update() RETURNS trigger LANGUAGE plpgsql AS
  $$ BEGIN RAISE EXCEPTION '% is immutable', TG_TABLE_NAME; END $$;
  CREATE TRIGGER seat_map_version_immutable
    BEFORE UPDATE OR DELETE ON seat_map_version FOR EACH ROW EXECUTE FUNCTION forbid_update();
  ```
- **Hệ quả:** Phiên bản bất biến được database ép, kể cả khi code có lỗi.
- **Ghi vào:** DOC-14.

### DR-17 · DDL kho vé, thêm trạng thái `REMOVED` cho unit — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 7.8 và 14.3 dùng trạng thái `REMOVED` của unit, nhưng vòng đời unit ở mục 8.1 chỉ có `AVAILABLE`, `HELD`, `SOLD`. Unit ghế cần nhãn (Section, hàng, số) để in lên vé và email, nhưng SDD gốc không có bảng ghế. Trình xem sơ đồ cần ánh xạ ghế sang vị trí trong bitmap tình trạng (DR-62).
- **Quyết định (Owner chốt):** Vòng đời unit có thêm `REMOVED`: `AVAILABLE → REMOVED` khi xóa ghế/khu vực hoặc giảm sức chứa; `REMOVED → AVAILABLE` khi ghế cùng UUID được thêm lại trong phiên bản sau. Unit ghế mang nhãn chụp từ phiên bản sơ đồ (DR-40).

  ```sql
  CREATE TABLE inventory_pool (
    pool_id        uuid PRIMARY KEY DEFAULT uuidv7(),
    event_id       uuid NOT NULL REFERENCES event,
    kind           text NOT NULL CHECK (kind IN ('ZONE','GA')),
    zone_key       text,                               -- id của zone trong tài liệu sơ đồ
    ticket_type_id uuid NOT NULL REFERENCES ticket_type,
    name           text NOT NULL,                      -- tên zone, hoặc tên loại vé GA
    capacity       int  NOT NULL CHECK (capacity >= 0),
    removed_at     timestamptz,
    CONSTRAINT pool_zone_ck CHECK ((kind = 'ZONE') = (zone_key IS NOT NULL)),
    UNIQUE (event_id, zone_key)
  );
  CREATE UNIQUE INDEX pool_ga_uq ON inventory_pool (ticket_type_id) WHERE kind = 'GA';

  CREATE TABLE inventory_unit (
    unit_id        uuid PRIMARY KEY DEFAULT uuidv7(),
    event_id       uuid NOT NULL REFERENCES event,
    ticket_type_id uuid NOT NULL REFERENCES ticket_type,
    pool_id        uuid REFERENCES inventory_pool,
    seat_id        uuid,
    seat_index     int,                                -- thứ tự ghế trong phiên bản sơ đồ, DR-62
    section_name   text,
    row_label      text,
    seat_number    text,
    status         text NOT NULL DEFAULT 'AVAILABLE'
                   CHECK (status IN ('AVAILABLE','HELD','SOLD','REMOVED')),
    reservation_id uuid REFERENCES reservation,
    updated_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT unit_kind_ck  CHECK ((seat_id IS NULL) <> (pool_id IS NULL)),
    CONSTRAINT unit_seat_ck  CHECK (seat_id IS NULL OR (seat_index IS NOT NULL AND row_label IS NOT NULL AND seat_number IS NOT NULL)),
    CONSTRAINT unit_owner_ck CHECK ((status IN ('HELD','SOLD')) = (reservation_id IS NOT NULL))
  ) WITH (fillfactor = 80, autovacuum_vacuum_scale_factor = 0.02);

  CREATE UNIQUE INDEX unit_seat_uq       ON inventory_unit (event_id, seat_id) WHERE seat_id IS NOT NULL;
  CREATE INDEX unit_pool_available_idx   ON inventory_unit (pool_id) WHERE status = 'AVAILABLE';
  CREATE INDEX unit_reservation_idx      ON inventory_unit (reservation_id) WHERE reservation_id IS NOT NULL;
  CREATE INDEX unit_seat_taken_idx       ON inventory_unit (event_id, seat_index) WHERE seat_id IS NOT NULL AND status IN ('HELD','SOLD');
  ```

  `fillfactor = 80` để cập nhật `status` là HOT update (không đụng cột có index trừ các index một phần, xem DOC-24 §4 về tác động).
- **Hệ quả:** Kiểm tra bất biến "số unit chưa `REMOVED` của pool bằng `capacity`" (SDD gốc 14.3) có dữ liệu để chạy. Index một phần trên `status` làm HOT update không áp dụng khi `status` đổi; S-03 đo tác động.
- **Ghi vào:** DOC-14, DOC-24.

### DR-18 · DDL reservation, đơn hàng và vé — **Chốt**
- **Vấn đề:** SDD gốc mục 8.3, 9.2, 9.4, 11 nêu trạng thái và ràng buộc, không có cột; lý do đóng reservation (hết hạn, người mua hủy, sự kiện bị hủy) cần cho thông báo và kiểm tra bất biến.
- **Quyết định (Owner chốt):**

  ```sql
  CREATE TABLE reservation (
    reservation_id uuid PRIMARY KEY,                   -- sinh trước ở Java, DR-11
    event_id       uuid NOT NULL REFERENCES event,
    user_id        uuid NOT NULL REFERENCES app_user,
    status         text NOT NULL CHECK (status IN ('ACTIVE','EXPIRING','CONFIRMED','EXPIRED','CANCELLED')),
    expires_at     timestamptz NOT NULL,
    close_reason   text CHECK (close_reason IN ('TIMEOUT','BUYER_CANCELLED','EVENT_CANCELLED')),
    expiring_since timestamptz,
    created_at     timestamptz NOT NULL DEFAULT now(),
    closed_at      timestamptz
  );
  CREATE UNIQUE INDEX reservation_open_uq ON reservation (user_id, event_id) WHERE status IN ('ACTIVE','EXPIRING');
  CREATE INDEX reservation_due_idx        ON reservation (expires_at) WHERE status = 'ACTIVE';
  CREATE INDEX reservation_expiring_idx   ON reservation (expiring_since) WHERE status = 'EXPIRING';

  CREATE TABLE reservation_item (
    reservation_item_id uuid PRIMARY KEY DEFAULT uuidv7(),
    reservation_id      uuid NOT NULL REFERENCES reservation,
    kind                text NOT NULL CHECK (kind IN ('SEAT','ZONE','GA')),
    seat_id             uuid,
    pool_id             uuid REFERENCES inventory_pool,
    quantity            int  NOT NULL CHECK (quantity >= 1),
    ticket_type_id      uuid NOT NULL REFERENCES ticket_type,
    ticket_type_name    text NOT NULL,                 -- chụp lúc giữ vé, DR-20
    unit_price          bigint NOT NULL CHECK (unit_price >= 0),
    label               jsonb NOT NULL,                -- {"section":"Khán đài A","row":"C","seat":"9"} hoặc {"zone":"Fanzone"}
    CONSTRAINT item_seat_ck CHECK (kind <> 'SEAT' OR (seat_id IS NOT NULL AND quantity = 1 AND pool_id IS NULL)),
    CONSTRAINT item_pool_ck CHECK (kind = 'SEAT' OR (pool_id IS NOT NULL AND seat_id IS NULL))
  );
  CREATE INDEX reservation_item_res_idx ON reservation_item (reservation_id);

  CREATE TABLE orders (
    order_id           uuid PRIMARY KEY,
    reservation_id     uuid NOT NULL UNIQUE REFERENCES reservation,
    event_id           uuid NOT NULL REFERENCES event,
    user_id            uuid NOT NULL REFERENCES app_user,
    status             text NOT NULL CHECK (status IN ('PENDING_PAYMENT','PAID','EXPIRED','CANCELLED','REFUND_PENDING','REFUNDED')),
    amount             bigint NOT NULL CHECK (amount >= 0),
    currency           text NOT NULL DEFAULT 'VND' CHECK (currency = 'VND'),   -- DR-13: chỉ VND
    payment_intent_id  text UNIQUE,
    last_payment_error text,                           -- mã decline_code gần nhất
    paid_at            timestamptz,
    refund_reason      text CHECK (refund_reason IN ('LATE_PAYMENT','AMOUNT_MISMATCH','EVENT_CANCELLED')),  -- DR-44
    refund_reference   text,                           -- mã hoàn tiền Stripe ghi tay theo RB-01
    refunded_at        timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT orders_paid_ck   CHECK (status <> 'PAID' OR paid_at IS NOT NULL),
    CONSTRAINT orders_refund_ck CHECK ((status IN ('REFUND_PENDING','REFUNDED')) = (refund_reason IS NOT NULL)),
    CONSTRAINT orders_refunded_ck CHECK (status <> 'REFUNDED' OR (refund_reference IS NOT NULL AND refunded_at IS NOT NULL))
  );
  CREATE INDEX orders_user_idx       ON orders (user_id, created_at DESC);
  CREATE INDEX orders_event_paid_idx ON orders (event_id) WHERE status = 'PAID';
  CREATE INDEX orders_pending_idx    ON orders (created_at) WHERE status = 'PENDING_PAYMENT';
  CREATE INDEX orders_refund_idx     ON orders (created_at) WHERE status = 'REFUND_PENDING';

  CREATE TABLE ticket (
    ticket_id        uuid PRIMARY KEY DEFAULT uuidv7(),
    order_id         uuid NOT NULL REFERENCES orders,
    unit_id          uuid NOT NULL REFERENCES inventory_unit,
    event_id         uuid NOT NULL REFERENCES event,
    code             text NOT NULL UNIQUE,             -- DR-52
    status           text NOT NULL DEFAULT 'ISSUED' CHECK (status IN ('ISSUED','VOID')),
    ticket_type_name text NOT NULL,
    unit_price       bigint NOT NULL,
    label            jsonb NOT NULL,
    issued_at        timestamptz NOT NULL DEFAULT now()
  );
  CREATE UNIQUE INDEX ticket_unit_issued_uq ON ticket (unit_id) WHERE status = 'ISSUED';
  CREATE INDEX ticket_order_idx ON ticket (order_id);
  ```
- **Hệ quả:** `reservation` được chèn **trước** câu claim trong cùng transaction để khóa ngoại `inventory_unit.reservation_id` hợp lệ; claim thiếu dòng thì rollback cả hai (DOC-24).
- **Ghi vào:** DOC-14.

### DR-19 · DDL idempotency, webhook đã xử lý, outbox — **Chốt**
- **Vấn đề:** SDD gốc mục 8.6, 9.3, 4.2 nêu ba bảng không có cột; cách "request thứ hai bị chặn ở unique index rồi nhận lại response" cần dòng key được chèn đầu transaction và điền response cuối transaction.
- **Quyết định (Owner chốt):**

  ```sql
  CREATE TABLE idempotency_key (
    user_id         uuid NOT NULL,
    idem_key        uuid NOT NULL,
    operation       text NOT NULL CHECK (operation IN ('HOLD','CANCEL_HOLD','CONFIRM_FREE')),
    request_hash    bytea NOT NULL,                    -- SHA-256, DR-45
    response_status smallint,                          -- điền trước COMMIT
    response_body   jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, idem_key)
  );
  CREATE INDEX idempotency_key_created_idx ON idempotency_key (created_at);

  CREATE TABLE stripe_event (
    stripe_event_id   text PRIMARY KEY,                -- evt_…
    type              text NOT NULL,
    payment_intent_id text,
    outcome           text NOT NULL,                   -- CONFIRMED | REFUND_PENDING | FAILURE_RECORDED | IGNORED
    received_at       timestamptz NOT NULL DEFAULT now()
  );

  CREATE TABLE outbox (
    outbox_id       uuid PRIMARY KEY DEFAULT uuidv7(),
    kind            text NOT NULL CHECK (kind IN ('EMAIL_TICKETS','EMAIL_EVENT_CHANGED','EMAIL_REFUND_PENDING')),   -- magic link gửi trực tiếp, DR-21
    payload         jsonb NOT NULL,
    status          text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SENT','FAILED')),
    attempts        int  NOT NULL DEFAULT 0,
    next_attempt_at timestamptz NOT NULL DEFAULT now(),
    last_error      text,
    created_at      timestamptz NOT NULL DEFAULT now(),
    sent_at         timestamptz
  );
  CREATE INDEX outbox_due_idx ON outbox (next_attempt_at) WHERE status = 'PENDING';
  ```
- **Hệ quả:** Thao tác tạo PaymentIntent không dùng bảng này (idempotent tự nhiên theo `order_id`, DR-45). Hủy PaymentIntent không đi qua outbox (DR-53).
- **Ghi vào:** DOC-15.

### DR-20 · Giá chụp lại nằm ở đâu — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 6.2 nói giá và tên loại vé "được chụp lại vào dòng đơn hàng", còn mục 11.1 nói `reservation_item` lưu "giá đã chụp" và không có bảng dòng đơn hàng. Hai chỗ mâu thuẫn; thêm `order_item` thì có hai bản chụp phải giữ khớp.
- **Quyết định (Claude chốt, Owner ủy quyền):** Bản chụp duy nhất nằm ở `reservation_item` (tên loại vé, giá đơn vị, nhãn vị trí). `orders.amount` = Σ `quantity × unit_price` của các item, tính trong transaction giữ vé và không đổi sau đó. Không có bảng `order_item`. Vé chép lại tên, giá, nhãn từ item lúc phát hành để vé tự đứng được khi in.
- **Hệ quả:** Đổi giá loại vé sau khi mở bán chỉ ảnh hưởng reservation tạo sau (SDD gốc 6.3) mà không cần cơ chế riêng.
- **Ghi vào:** DOC-14, DOC-24.

---

## D. Xác thực và phân quyền

### DR-21 · Chi tiết magic link — ⚠ lệch SDD gốc — **Đổi: gửi email trực tiếp sau commit, không qua outbox**
- **Vấn đề:** SDD gốc mục 5 chốt luồng, thời hạn, giới hạn gửi nhưng để ngỏ: chuẩn hóa email; cách vô hiệu link cũ; `return_to` lưu ở đâu khi người dùng mở link trên thiết bị khác; giới hạn gửi đếm ở đâu; và **email magic link đi qua outbox** (SDD gốc 4.1) **trong khi token chỉ được lưu dạng hash** (NFR-07): job outbox cần token thô để dựng link.
- **Các phương án cho mâu thuẫn outbox:** (1) gửi email trực tiếp sau commit, không qua outbox: SMTP lỗi thì người dùng phải bấm "Gửi lại"; (2) outbox giữ token thô dạng mã hóa AES-256-GCM bằng khóa `OUTBOX_ENCRYPTION_KEY`, xóa trường đó ngay khi gửi xong; (3) outbox giữ token thô dạng rõ: vi phạm NFR-07.
- **Quyết định (Owner chốt; Owner đổi cách gửi 2026-10-06):**
  1. Chuẩn hóa email: `trim`, chữ thường toàn bộ, kiểm tra theo regex `^[^@\s]+@[^@\s]+\.[^@\s]+$` và dài ≤ 254. Không bỏ dấu chấm hay phần `+tag`.
  2. Token: 32 byte từ `SecureRandom`, base64url không padding (43 ký tự). Lưu `SHA-256`.
  3. `POST /auth/magic-link` theo phương án 1: (a) một transaction: kiểm tra giới hạn gửi (mục 4), `UPDATE login_token SET superseded_at = now() WHERE email = :email AND used_at IS NULL AND superseded_at IS NULL`, chèn token mới (`expires_at = now() + 15 phút`, `return_to`, `locale`, `requested_ip`); commit. (b) Sau commit, gửi email `magic-link` qua SMTP ngay trong request (timeout 5 giây, không thử lại). Gửi được → **202**. SMTP lỗi → **503** `EMAIL_PROVIDER_UNAVAILABLE`; giao diện hiện "Chưa gửi được email, thử lại" và người dùng bấm "Gửi lại", lần xin mới thay token cũ như thường. Token thô chỉ nằm trong bộ nhớ của request, không bao giờ ghi xuống đâu (NFR-07 đúng theo nghĩa đen).
  4. Giới hạn gửi (3 mỗi email/15 phút, 10 mỗi IP/giờ, SDD gốc 5.2) đếm bằng truy vấn trên `login_token` trong cùng transaction (`count(*) WHERE email = :email AND created_at > now() - interval '15 minutes'` và `count(*) WHERE requested_ip = :ip AND created_at > now() - interval '1 hour'`), không cần Redis; vượt ngưỡng vẫn trả **202** (không lộ thông tin) nhưng không chèn token và không gửi; giao diện "Gửi quá nhiều lần" hiện khi response có header `X-Magic-Link-Throttled: 1`. Lần gửi SMTP lỗi vẫn tính vào giới hạn.
  5. `POST /auth/verify` tiêu thụ token: `UPDATE login_token SET used_at = now() WHERE token_hash = :h AND used_at IS NULL AND superseded_at IS NULL AND expires_at > now() RETURNING email, return_to, locale`. Response chứa `returnTo`; trình duyệt nào mở link thì trình duyệt đó có session. Trình duyệt đã yêu cầu link không tự đăng nhập (không có "đăng nhập chéo thiết bị", tránh lừa đảo bằng link).
  6. `return_to` chỉ chấp nhận chuỗi bắt đầu bằng `/`, không bắt đầu bằng `//` hoặc `/\`, dài ≤ 512; sai thì dùng `/`.
- **Hệ quả:** Không cần khóa mã hóa, không có loại outbox `EMAIL_MAGIC_LINK` (DR-19); outbox chỉ còn email phát sinh từ nghiệp vụ (vé, đổi lịch, chờ hoàn tiền). Thêm mã lỗi `EMAIL_PROVIDER_UNAVAILABLE` (DR-64). Đổi lại: SMTP chập chờn thì người dùng phải tự bấm gửi lại; chấp nhận vì đăng nhập là thao tác người dùng đang chờ trên màn hình.
- *Đổi 2026-10-06:* đề xuất cũ (phương án 2) cho email magic link đi qua outbox, token thô mã hóa AES-256-GCM bằng `OUTBOX_ENCRYPTION_KEY` và xóa khỏi payload sau khi gửi; giới hạn theo IP đếm bằng Redis token bucket (DR-56). Owner đổi để bớt mã hóa và một loại outbox.
- **Ghi vào:** DOC-19, DOC-27, DOC-32, DOC-44.

### DR-22 · Session và CSRF — **Chốt**
- **Vấn đề:** SDD gốc mục 5.2 chốt hash session, cookie `HttpOnly; Secure; SameSite=Lax`, 30 ngày không hoạt động, header `X-CSRF-Token`, nhưng không nói SPA lấy CSRF token từ đâu, cập nhật `last_seen_at` thế nào mà không ghi database ở mọi request, và cookie `Secure` hoạt động ra sao trên `http://localhost`.
- **Quyết định (Owner chốt):**
  - Cookie `tb_session`, giá trị 32 byte base64url; `Path=/; HttpOnly; SameSite=Lax; Secure` (cấu hình `auth.cookie-secure`, mặc định `true`, profile `dev` đặt `false` vì Safari không nhận cookie `Secure` trên `http://localhost`).
  - Tra session qua bộ đệm Caffeine trong tiến trình, TTL 60 giây, tối đa 200.000 mục; đăng xuất xóa mục khỏi bộ đệm (một bản sao, SDD gốc 14.1). `last_seen_at` chỉ cập nhật khi đã cũ hơn 1 giờ. Session hết hạn khi `last_seen_at < now() - 30 ngày` hoặc `revoked_at` khác NULL.
  - CSRF theo kiểu synchronizer token: `session.csrf_token` sinh lúc tạo session, trả trong body của `POST /auth/verify` và `GET /me`; client gửi lại trong header `X-CSRF-Token` cho mọi `POST/PUT/PATCH/DELETE`. So sánh hằng thời gian. Ngoại lệ: `POST /api/v1/webhooks/stripe`, `POST /auth/magic-link`, `POST /auth/verify` (chưa có session).
  - Email không bao giờ vào log; log dùng `user_id`.
- **Ghi vào:** DOC-19, DOC-32.

### DR-23 · Vai trò, hồ sơ tổ chức và kiểm tra sở hữu — **Chốt**
- **Vấn đề:** SDD gốc mục 3.1, 5.3 nói tài khoản thành người tạo sự kiện khi lập hồ sơ tổ chức; không nói một tài khoản có mấy tổ chức, nội dung hồ sơ, và ai thấy gì. Canvas Studio 00 có "Tên tổ chức" (bắt buộc) và "Email liên hệ" (không bắt buộc, trống thì dùng email đăng nhập); email đổi lịch (07c) hiện tên và email liên hệ của tổ chức cho người mua.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Một tài khoản có tối đa một tổ chức, một tổ chức có đúng một chủ (`organizer.owner_user_id UNIQUE`). Thành viên nhiều người để sau.
  - `POST /organizer` tạo hồ sơ; gọi lần hai trả 409 `ORGANIZER_EXISTS`. Tên 1–120 ký tự; email liên hệ theo quy tắc DR-21.
  - Mọi endpoint `/organizer/**` lấy `organizer_id` từ session (không nhận từ client), và mọi truy vấn tài nguyên có `AND organizer_id = :sessionOrg`; không thấy thì trả **404** `NOT_FOUND` cho tài nguyên của tổ chức khác thay vì 403 (không lộ tồn tại). 403 `FORBIDDEN` chỉ dùng khi tài khoản chưa có hồ sơ tổ chức mà vào `/organizer/**`; giao diện chuyển tới màn Lập hồ sơ. Màn E3 "Không có quyền vào studio" hiển thị cho cả hai trường hợp.
  - `GET /me` trả `{ userId, email, locale, roles: ["BUYER","ORGANIZER"], organizer: { organizerId, name } | null, csrfToken, serverTime }`.
- **Ghi vào:** DOC-19, DOC-32, DOC-53.

---

## E. Sự kiện và loại vé

### DR-24 · Trạng thái hiển thị và các chuyển trạng thái tự động của event — **Đổi: thêm đóng bán sớm**
- **Vấn đề:** SDD gốc mục 6.1 có 5 trạng thái lưu. Canvas dùng thêm "Sắp mở bán", "Đang mở bán", "Hết vé", "Đã xuất bản" là trạng thái suy ra. SDD gốc không nói ai chuyển `PUBLISHED → ENDED`, reservation đang mở được thanh toán tiếp không khi event `PAUSED`, và danh sách công khai lọc những gì.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Trạng thái hiển thị (`displayStatus`) tính ở server, trả trong mọi response event:

    | `displayStatus` | Điều kiện |
    | --- | --- |
    | `DRAFT` | `status = DRAFT` |
    | `UPCOMING` ("Sắp mở bán") | `PUBLISHED` và `now() < sale_starts_at` |
    | `ON_SALE` ("Đang mở bán") | `PUBLISHED`, trong khung mở bán, còn unit `AVAILABLE` |
    | `SOLD_OUT` ("Hết vé") | `PUBLISHED`, trong khung mở bán, không còn unit `AVAILABLE` (kể cả khi còn `HELD`) |
    | `SALE_CLOSED` ("Đã đóng bán") | `PUBLISHED`, `now() >= sale_ends_at`, chưa `ENDED` |
    | `PAUSED`, `ENDED`, `CANCELLED` | Như trạng thái lưu |
  - Job `EventLifecycleJob` chạy mỗi 60 giây: `UPDATE event SET status = 'ENDED', ended_at = now() WHERE status IN ('PUBLISHED','PAUSED') AND ends_at <= now()`.
  - Đóng bán sớm (Owner thêm 2026-10-06): `POST /organizer/events/{id}/close-sale` chạy `UPDATE event SET sale_ends_at = now() WHERE event_id = :e AND organizer_id = :org AND status IN ('PUBLISHED','PAUSED') AND sale_starts_at <= now() AND sale_ends_at > now()`; 0 dòng → 409 `EVENT_STATE_CONFLICT`. Sự kiện chuyển `SALE_CLOSED`, không có trạng thái lưu mới, không mở lại được (muốn dừng tạm thì dùng `PAUSED`); `ENDED` vẫn do job đặt khi qua `ends_at`.
  - `PAUSED` và `SALE_CLOSED` chặn giữ vé mới (409 `EVENT_NOT_ON_SALE`); reservation đã `ACTIVE` vẫn tạo được PaymentIntent và thanh toán tới hết hạn. `ENDED` cũng vậy (thanh toán của reservation còn hạn vẫn được xác nhận).
  - `GET /events` trả event `PUBLISHED`/`PAUSED` có `ends_at > now()`, sắp theo `starts_at` tăng dần, phân trang cursor (DR-63). Event `ENDED`, `CANCELLED` vẫn mở được bằng URL trực tiếp (trang hiện trạng thái), không có trong danh sách.
- **Ghi vào:** DOC-20, DOC-40, DOC-43.

### DR-25 · Quy tắc hợp lệ của trường sự kiện — **Chốt**
- **Vấn đề:** SDD gốc mục 6.1 nêu điều kiện xuất bản; canvas Studio 02 có thêm thông báo lỗi theo ô và quy tắc "Đóng bán không được muộn hơn giờ bắt đầu sự kiện", ô "Số vé tối đa mỗi đơn" chỉ nói "từ 1 trở lên".
- **Quyết định (Claude chốt, Owner ủy quyền):** Lưu bản nháp chỉ cần `name`. Xuất bản cần đủ:

  | Trường | Quy tắc | Mã lỗi trường |
  | --- | --- | --- |
  | `name` | 1–120 ký tự sau trim | `required`, `too_long` |
  | `venue` | 1–200 ký tự | `required` |
  | `description` | ≤ 5.000 ký tự, văn bản thuần, giữ xuống dòng | `too_long` |
  | `timezone` | Tên IANA trong `ZoneId.getAvailableZoneIds()`; không đổi sau khi xuất bản (DR-12) | `invalid_timezone`, `locked_after_publish` |
  | `startsAt` | > `now()` lúc xuất bản | `must_be_future` |
  | `endsAt` | > `startsAt`, ≤ `startsAt` + 72 giờ | `must_be_after_start`, `too_long_event` |
  | `saleStartsAt` | < `saleEndsAt` | `required` |
  | `saleEndsAt` | > `saleStartsAt`, ≤ `startsAt` | `must_be_after_sale_start`, `after_event_start` |

  Lỗi trả 422 `VALIDATION_FAILED` với `errors: [{ "field": "saleEndsAt", "rule": "after_event_start" }]`; client tra key i18n `validation.<rule>`.
- **Hệ quả:** Không có ô "Số vé tối đa mỗi đơn" (bỏ theo DR-41); màn Studio 02 bỏ ô này khỏi canvas.
- **Ghi vào:** DOC-20, DOC-55.

### DR-26 · Quy tắc loại vé — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 6.2–6.3 có mô hình, giá, quy tắc xóa. Canvas thêm: tên không trùng ("Tên này đã dùng cho loại vé khác"), màu gán theo thứ tự tạo với bảng 5 màu, giá "Nhập giá, hoặc 0 nếu miễn phí". SDD gốc không giới hạn số loại vé, không nói đổi mô hình (`SEAT`→`ZONE`) có được không.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Tối đa **5 loại vé** mỗi sự kiện ở giai đoạn này, khớp bảng màu `type-1…type-5` của design system (⚠: SDD gốc không giới hạn). Màu: `color_index` nhỏ nhất chưa dùng trong event lúc tạo; xóa loại vé thì màu được dùng lại.
  - Tên duy nhất trong event, không phân biệt hoa thường (index `ticket_type_name_uq`).
  - Giá: `0` hoặc trong khoảng của DR-13.
  - `model` không đổi được sau khi event đã xuất bản; trong bản nháp đổi tự do (gán ghế/zone trên sơ đồ sẽ báo lỗi validate nếu không khớp).
  - Sức chứa GA: 1–100.000. Sau khi mở bán, giảm tối thiểu tới số unit đang `HELD` + `SOLD` (thông báo "Không thấp hơn N vé đang giữ và đã bán").
  - Xóa: bản nháp thì xóa thật; đã xuất bản thì chỉ khi không có unit `HELD`/`SOLD`, đặt `deleted_at`, unit `AVAILABLE` sang `REMOVED`, pool đặt `removed_at`.
- **Ghi vào:** DOC-20, DOC-56.

### DR-27 · Transaction xuất bản event và tạo kho vé — 🔬 spike — **Chốt**
- **Vấn đề:** SDD gốc mục 6.1 nói kho vé tạo trong cùng transaction khi xuất bản. Không nói cách chèn tới 100.000 dòng, thời gian cho phép, và event không có sơ đồ (chỉ GA) đi đường nào.
- **Quyết định (Claude chốt, Owner ủy quyền):** `POST /organizer/events/{id}/publish` chạy một transaction:
  1. `SELECT … FROM event WHERE event_id = :id AND organizer_id = :org FOR UPDATE`; trạng thái phải là `DRAFT`.
  2. Kiểm tra điều kiện xuất bản (DR-25; có ≥ 1 loại vé sức chứa > 0; nếu có loại vé `SEAT`/`ZONE` thì `seat_map.latest_version_no ≥ 1` và phiên bản mới nhất đã qua validate server).
  3. Unit ghế: một câu `INSERT … SELECT` từ `jsonb_path_query` trên `seat_map_version.document` (bỏ ghế `blocked`), mang `seat_index`, nhãn (DR-40).
  4. Pool zone và GA: chèn `inventory_pool`; unit bằng `INSERT INTO inventory_unit (…) SELECT … FROM inventory_pool p CROSS JOIN generate_series(1, p.capacity)`.
  5. `UPDATE event SET status = 'PUBLISHED', published_at = now(), seat_map_version_id = :v`.
  6. `SET LOCAL statement_timeout = '30s'` cho transaction này.

  Spike **S-03** đo thời gian bước 3–4 với 20.000 ghế + 80.000 unit pool trên PostgreSQL 18 trong compose; mục tiêu < 5 giây. Vượt thì chuyển sang `COPY` qua `PgConnection.getCopyAPI()`.
- **Hệ quả:** Màn Studio 06 hiển thị "Đang xuất bản" cho tới khi response về; timeout HTTP của nginx cho route này là 60 giây.
- **Ghi vào:** DOC-20, DOC-24.

### DR-28 · Hủy sự kiện khi đã mở bán và tranh chấp với thanh toán đang đến — ⚠ lệch SDD gốc — **Đổi: cho hủy cả khi đã có đơn PAID, mọi đơn đã thanh toán chuyển sang `REFUND_PENDING`**
- **Vấn đề:** SDD gốc mục 6.1 chỉ cho hủy khi chưa có đơn `PAID` vì chưa có hoàn tiền. Owner muốn hủy được sự kiện đã mở bán. Dù quy tắc nào, việc hủy vẫn tranh chấp với webhook thanh toán đang đến: một đơn có thể thành `PAID` ngay sau khi sự kiện bị hủy.
- **Quyết định (Owner chốt):**
  - Hủy được từ `PUBLISHED` hoặc `PAUSED` (⚠: SDD gốc 6.1 chỉ cho hủy khi chưa có đơn `PAID`). `DRAFT` không hủy; trạng thái khác → 409 `EVENT_STATE_CONFLICT`.
  - Một transaction (`SET LOCAL statement_timeout = '60s'`):
    1. `SELECT … FROM event WHERE event_id = :id AND organizer_id = :org FOR UPDATE`.
    2. `UPDATE event SET status = 'CANCELLED', cancelled_at = now()`.
    3. `UPDATE orders SET status = 'REFUND_PENDING', refund_reason = 'EVENT_CANCELLED', updated_at = now() WHERE event_id = :id AND status = 'PAID' RETURNING order_id`.
    4. `UPDATE ticket SET status = 'VOID' WHERE event_id = :id AND status = 'ISSUED'`.
    5. `UPDATE reservation SET status = 'EXPIRING', close_reason = 'EVENT_CANCELLED', expiring_since = now() WHERE event_id = :id AND status = 'ACTIVE'`; job trả vé xử lý tiếp như hết hạn (hủy PaymentIntent rồi trả vé).
    6. Chèn một outbox `EMAIL_REFUND_PENDING` (lý do `EVENT_CANCELLED`) cho mỗi đơn ở bước 3.
  - Transaction xác nhận thanh toán (DOC-26) mở đầu bằng `SELECT status FROM event WHERE event_id = :e FOR SHARE`. Hủy commit trước → xác nhận thấy `CANCELLED`, đơn sang `REFUND_PENDING` (lý do `EVENT_CANCELLED`), không phát hành vé. Xác nhận commit trước → bước 3 của lệnh hủy bắt được đơn đó.
  - Unit `SOLD` giữ nguyên để lưu vết; kiểm tra bất biến về vé và unit bỏ qua sự kiện `CANCELLED` (DR-73).
  - Hoàn tiền vẫn làm thủ công trên Stripe Dashboard theo RB-01 (SDD gốc 2.3: hoàn tiền tự động để sau); đơn chuyển `REFUND_PENDING → REFUNDED` khi người vận hành ghi `refund_reference` (DR-44).
  - Response `200 { "refundPendingOrders": 12, "releasingReservations": 3 }`. Hộp thoại hủy ở Studio 06 báo trước số đơn sẽ chuyển sang chờ hoàn tiền; câu "Chỉ hủy được khi chưa có đơn đã thanh toán" trên canvas bị bỏ.
- **Hệ quả:** `FOR SHARE` trên dòng event chỉ xảy ra ở webhook; không tạo hot row cho đường giữ vé. Người tổ chức hủy sự kiện đã bán nhiều vé tạo khối việc hoàn tiền thủ công lớn (§8 master plan). Test bắt buộc: hủy và webhook song song 1.000 lần → không có đơn `PAID` và không có vé `ISSUED` nào trên sự kiện `CANCELLED`; mỗi đơn đã trả tiền có đúng một email chờ hoàn tiền.
- *Đổi 2026-10-05:* đề xuất cũ là chỉ cho hủy khi chưa có đơn `PAID`: khóa event `FOR UPDATE`, kiểm tra không có đơn `PAID`, trả 409 `EVENT_HAS_PAID_ORDERS` nếu có; thanh toán đến sau khi hủy đi luồng `NEEDS_REVIEW`.
- **Ghi vào:** DOC-20, DOC-26, DOC-27, DOC-59.

### DR-29 · Email báo đổi giờ hoặc địa điểm — **Chốt**
- **Vấn đề:** SDD gốc mục 6.3 nói người đã mua nhận email khi đổi `starts_at` hoặc địa điểm. Canvas 07c có ba biến thể (đổi giờ, đổi địa điểm, cả hai) kèm danh sách vé và email liên hệ tổ chức. SDD gốc không nói gửi cho ai, mỗi lần sửa gửi một email hay gom lại.
- **Quyết định (Claude chốt, Owner ủy quyền):** Trong transaction `PATCH /organizer/events/{id}`, nếu event không phải `DRAFT` và `starts_at`, `ends_at` hoặc `venue` đổi, chèn một dòng outbox `EMAIL_EVENT_CHANGED` cho mỗi đơn `PAID` của event, payload `{ orderId, locale, before: { startsAt, endsAt, venue }, after: {…} }`. Không gom; mỗi lần lưu là một đợt. Response của PATCH trả `notifiedOrders: n`, màn Studio 02 hiện "Đã gửi thông báo tới n người mua".
- **Ghi vào:** DOC-20, DOC-27, DOC-51.

### DR-30 · Đổi sức chứa và giá khi đang bán: đồng thời với giữ vé — **Chốt**
- **Vấn đề:** SDD gốc mục 6.3 cho tăng/giảm sức chứa zone và GA, nhưng giảm sức chứa đang có người giữ vé đồng thời không được nói cách làm.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Tăng: chèn thêm `Δ` unit vào pool và `UPDATE inventory_pool SET capacity = capacity + Δ` trong một transaction.
  - Giảm `Δ`: `WITH victims AS (SELECT unit_id FROM inventory_unit WHERE pool_id = :p AND status = 'AVAILABLE' LIMIT :Δ FOR UPDATE SKIP LOCKED) UPDATE inventory_unit SET status = 'REMOVED' FROM victims …`; số dòng phải bằng `Δ`, ít hơn thì rollback và trả 409 `CAPACITY_BELOW_USED` kèm `used = count(HELD+SOLD)`; cùng transaction giảm `capacity`.
  - Đổi giá: chỉ `UPDATE ticket_type SET price`; giữ vé đọc giá trong transaction giữ vé nên reservation tạo sau commit dùng giá mới.
- **Ghi vào:** DOC-20, DOC-24.

---

## F. Sơ đồ chỗ ngồi

### DR-31 · Dùng lại sơ đồ cho nhiều sự kiện — ⚠ lệch SDD gốc — **Đổi: mỗi sự kiện một sơ đồ; dùng lại bằng nhân bản toàn bộ, sinh ID mới**
- **Vấn đề:** SDD gốc mục 7.8 nói "một sơ đồ có thể dùng lại cho nhiều sự kiện", nhưng tài liệu sơ đồ gán thẳng `ticketTypeId` (ví dụ `tt-vip`) cho hàng, ghế, zone, mà loại vé thuộc về một event (mục 6.2). Dùng chung một sơ đồ cho event thứ hai thì mọi `ticketTypeId` trỏ sai. Canvas cũng đặt sơ đồ là bước 3 trong 5 bước soạn một sự kiện.
- **Các phương án:** (1) Một sơ đồ cho mỗi event, dùng lại bằng nhân bản; (2) Tài liệu dùng "hạng chỗ" cục bộ (`category: "cat-1"`), event có bảng ánh xạ hạng chỗ → loại vé; phức tạp hơn ở editor, validate, diff và màn Loại vé.
- **Quyết định (Owner chốt):** Phương án 1, với nhân bản làm ngay ở giai đoạn này.
  - `seat_map.event_id UNIQUE`; `POST /organizer/maps` nhận `eventId`. Sơ đồ chỉ sửa được bởi tổ chức sở hữu event.
  - `POST /organizer/maps/{sourceMapId}/clone` với body `{ "eventId": "…", "source": "draft" | "version", "versionNo": 2 }` tạo `seat_map` mới cho event đích (event đích chưa có sơ đồ, nếu có → 409 `MAP_ALREADY_EXISTS`). Nguồn là bất kỳ sơ đồ nào của cùng tổ chức, kể cả của sự kiện đã kết thúc hoặc đã hủy.
  - Nhân bản sao chép **toàn bộ** tài liệu (hình học, nhãn, đánh số, cờ `accessible`/`blocked`, ghi đè số ghế, trang trí, cấu hình canvas) và **sinh lại mọi ID**: UUID mới cho từng ghế, ID mới cho section, row, zone, decoration. Không ID nào của sơ đồ nguồn xuất hiện trong sơ đồ mới; hai sơ đồ không còn liên hệ (chỉ lưu `seat_map.cloned_from_seat_map_id` để lưu vết).
  - `ticketTypeId` được ánh xạ sang loại vé của event đích theo tên (không phân biệt hoa thường) và cùng mô hình; không khớp thì để trống, validate báo `TICKET_TYPE_MISSING` và người tổ chức gán lại trong editor.
  - Ảnh nền mặt bằng: tham chiếu cùng `media_id` (ảnh là bất biến), không sao chép byte.
  - Sơ đồ mới bắt đầu là bản nháp `revision = 0`, chưa có phiên bản; xuất bản như sơ đồ vẽ mới.
  - Giao diện: màn "Sơ đồ trống" (04k) thêm lựa chọn "Dùng lại sơ đồ từ sự kiện khác" mở danh sách sơ đồ của tổ chức (`GET /organizer/maps?limit=`), chọn nguồn bản nháp hoặc một phiên bản đã xuất bản.
- **Hệ quả:** Không có quan hệ nhiều-nhiều giữa event và phiên bản; diff khi đang bán chỉ so các phiên bản của cùng một sơ đồ. Thêm cột `seat_map.cloned_from_seat_map_id uuid REFERENCES seat_map`. Test bắt buộc: nhân bản sơ đồ 180 ghế → 180 ghế với UUID khác hoàn toàn nguồn, hình học và nhãn trùng, loại vé ánh xạ đúng theo tên.
- *Đổi 2026-10-05:* đề xuất cũ là một sơ đồ cho mỗi event và chuyển "dùng lại cho nhiều sự kiện" sang giai đoạn sau.
- **Ghi vào:** ADR-0010, DOC-14, DOC-16, DOC-22, DOC-23, DOC-37, DOC-57.

### DR-32 · JSON Schema của tài liệu sơ đồ v1, giới hạn và checksum — **Chốt**
- **Vấn đề:** SDD gốc mục 7.8 có một ví dụ JSON, nhưng không có schema đủ để validate: kiểu điểm điều khiển của bezier, polyline, rect/ellipse có xoay, trang trí loại ảnh, ID của đối tượng, giới hạn kích thước ngoài "5 MB" và "20.000 ghế"; checksum tính trên chuỗi nào. Canvas editor có ô "Khung vẽ 900 × 720", "Đường kính ghế 20", "Khoảng cách ghế tối thiểu 24", "Bắt dính lưới 10" trong thuộc tính sơ đồ.
- **Quyết định (Owner chốt):** JSON Schema 2020-12 ở `frontend/src/map-core/schema/seat-map.v1.json`, backend nạp cùng file (copy lúc build). Các điểm chốt:

  | Phần | Quy tắc |
  | --- | --- |
  | `schemaVersion` | hằng `1` |
  | `canvas` | `width`, `height` 500–20.000 (mặc định 4000 × 3000), `seatDiameter` 10–60 (20), `minSpacing` ≥ `seatDiameter` (24), `grid` 0–100 (10), `background`: `{ mediaId, x, y, w, h, opacity 0–1, locked }` hoặc vắng |
  | Điểm | mảng `[x, y]` số thực, làm tròn 0,1 |
  | `path` | `line {start,end}`, `arc {start,end,through}`, `polyline {points[2..200]}`, `bezier {start,c1,c2,end}` |
  | `shape` | `rect {x,y,w,h,rotation}`, `ellipse {cx,cy,rx,ry,rotation}`, `polygon {points[3..200]}`; `rotation` độ |
  | ID | ghế: UUID; section, row, zone, decoration: `^(sec|row|zone|deco)-[a-z0-9]{8}$` |
  | `rows[].seats[]` | `id`, `number` (chuỗi 1–6 ký tự), `x`, `y`, `angle` (rad), `ticketTypeId` (tùy chọn, ghi đè loại vé của hàng), `flags` ⊂ `["accessible","blocked"]` |
  | `sections` | ≥ 1; hàng không có section thì editor gán vào section mặc định "Main" (key i18n) |
  | `decorations[]` | `kind`: `stage`, `entrance`, `label`, `image`; `text` ≤ 60 |
  | Giới hạn | ≤ 5 MB, ≤ 20.000 ghế, ≤ 1.000 hàng, ≤ 200 zone, ≤ 200 trang trí |

  Checksum: SHA-256 của chuỗi JSON chuẩn hóa theo RFC 8785 (JCS), tính ở server lúc xuất bản; client không gửi checksum.
- **Hệ quả:** Server và client dùng chung một file schema; mọi thay đổi schema tăng `schemaVersion` và kèm hàm nâng cấp tài liệu.
- **Ghi vào:** DOC-16, ADR-0009.

### DR-33 · Đánh số ghế và nhãn hàng — **Chốt**
- **Vấn đề:** SDD gốc mục 7.4 nêu kiểu đánh số "liên tục, hoặc lẻ và chẵn tách hai phía từ giữa", hướng đánh số, nhãn gợi ý A…Z, AA, nhưng không định nghĩa chính xác kiểu "lẻ chẵn từ giữa" và ghi đè số ghế tương tác thế nào khi đổi số ghế.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - `numbering = { start, direction: "forward"|"reverse", scheme: "sequential"|"odd-even-center" }`.
  - `sequential`: ghế thứ i (0-based, theo chiều đường) có số `start + i` (forward) hoặc `start + N − 1 − i` (reverse).
  - `odd-even-center`, N ghế: chia giữa `m = ceil(N/2)`. Ghế từ giữa sang phải (forward) mang số lẻ `start, start+2, …` (nếu `start` chẵn thì `start+1`), ghế từ giữa sang trái mang số chẵn `start+1, start+3, …`. Ví dụ N = 7, start = 1: vị trí trái→phải `6 4 2 1 3 5 7`. `reverse` đổi hai phía.
  - Nhãn hàng gợi ý: hàng kế tiếp sau nhãn lớn nhất trong cùng section theo thứ tự A…Z, AA…AZ, BA…; không bỏ chữ I, O.
  - Ghi đè số ghế lưu ở `seats[].numberOverride`; đổi số ghế N hoặc kiểu đánh số tính lại `number` cho ghế không có override.
  - Hiển thị: số ghế hiện nguyên chuỗi; trên cuống vé (thẻ "Hàng C · Ghế 09") số thuần chữ số được đệm 2 chữ số.
- **Ghi vào:** DOC-21.

### DR-34 · Thuật toán hình học còn thiếu — **Chốt**
- **Vấn đề:** SDD gốc mục 7.4 cho công thức rải ghế theo độ dài cung, nhưng không nói: cung qua ba điểm thẳng hàng; "kéo dài đường cho vừa" làm gì với từng kiểu đường; nhân bản song song cho gấp khúc và bezier; khối ghế (công cụ B) đặt các hàng thế nào; số ghế tối đa đặt vừa tính ra sao.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Cung: tâm là giao của hai trung trực; nếu ba điểm gần thẳng hàng (`|cross| < 1e-6 × |AB|²`) thì coi như đường thẳng.
  - Số ghế tối đa: `floor(L / minSpacing) + 1`.
  - Kéo dài cho vừa N ghế, độ dài cần `L' = (N − 1) × minSpacing`: thẳng → dời điểm cuối dọc hướng đường; cung → giữ tâm và bán kính, tăng góc quét đều hai phía; gấp khúc → kéo dài đoạn cuối; bezier → co giãn đồng dạng quanh điểm đầu với tỉ lệ `L'/L`.
  - Nhân bản song song: thẳng → tịnh tiến theo pháp tuyến; cung → đồng tâm, bán kính `r + k·gap`, tùy chọn thêm ghế `N_k = round(N × (r + k·gap)/r)`; gấp khúc và bezier → tịnh tiến theo pháp tuyến của dây cung nối hai đầu (không tính offset curve).
  - Khối ghế: hình chữ nhật kéo ra chia `rows` hàng thẳng cách đều theo chiều cao, mỗi hàng `per` ghế; kiểm tra khoảng cách cả theo hàng và giữa hàng (`h/(rows−1) ≥ minSpacing`); báo "Khối này đặt vừa tối đa …" như canvas 04e.
  - Nhãn zone đặt tại "pole of inaccessibility" (thuật toán polylabel, độ chính xác 1 đơn vị).
- **Ghi vào:** DOC-21.

### DR-35 · Validate dùng chung giữa client và server — 🔬 spike — **Chốt**
- **Vấn đề:** SDD gốc mục 7.7 chạy validate liên tục ở client và chạy lại ở server khi xuất bản, "kết quả của server là kết quả cuối cùng". Hai bản cài đặt (TypeScript và Java) phải cho cùng kết quả, gồm phép thử đa giác tự cắt và ghế nằm trong zone; SDD không nói server dùng thư viện gì và định dạng danh sách vấn đề.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Mã vấn đề cố định, dùng chung:

    | Mã | Mức | Quy tắc (SDD gốc 7.7) |
    | --- | --- | --- |
    | `SEAT_LABEL_DUPLICATE` | error | Trùng (section, hàng, số ghế) |
    | `SEAT_OVERLAP` | error | Hai tâm ghế cách nhau < `seatDiameter` |
    | `ROW_LABEL_MISSING` | error | Hàng không có nhãn |
    | `TICKET_TYPE_MISSING` | error | Ghế hoặc zone chưa gán loại vé |
    | `TICKET_TYPE_UNUSED` | error | Loại vé SEAT/ZONE không có ghế/zone |
    | `TICKET_TYPE_MODEL_MISMATCH` | error | Ghế gán loại vé không phải SEAT, zone gán loại vé không phải ZONE (bổ sung) |
    | `ZONE_CAPACITY_INVALID` | error | Sức chứa < 1 |
    | `POLYGON_SELF_INTERSECTS` | error | Đa giác tự cắt |
    | `SEAT_INSIDE_ZONE` | error | Tâm ghế nằm trong zone |
    | `SEAT_LIMIT_EXCEEDED` | error | > 20.000 ghế |
    | `ZONES_OVERLAP` | warning | Hai zone chồng nhau |
    | `OUT_OF_CANVAS` | warning | Đối tượng ngoài khung vẽ |
  - Server: kiểm tra chồng ghế bằng lưới băm ô cỡ `seatDiameter` (O(n)); đa giác bằng JTS (`IsSimpleOp`, `PreparedGeometry.contains`); ellipse/rect xoay được đa giác hóa 64 đỉnh ở cả hai phía.
  - Bộ test vector chung `map-core/fixtures/validation/*.json` (tài liệu + danh sách mã vấn đề mong đợi), chạy bằng cả Vitest và JUnit.
  - Response của `POST /organizer/maps/{id}/validate`: `{ "ok": false, "issues": [{ "code": "SEAT_OVERLAP", "level": "error", "objectIds": ["row-1a2b3c4d"], "params": { "row": "C", "count": 2 } }] }`; client dịch theo `code` + `params`.
  - Spike **S-05**: validate server 20.000 ghế + 200 zone < 500 ms.
- **Ghi vào:** DOC-21, DOC-37.

### DR-36 · Tự lưu bản nháp và xung đột giữa tab — **Chốt**
- **Vấn đề:** SDD gốc mục 7.6 nói tự lưu sau 2 giây không đổi, `PUT /maps/{id}/draft` kèm `revision`, 409 khi tab khác đã lưu. Không nói khi lưu lỗi mạng thì sao, gửi toàn bộ tài liệu hay diff, và màn 04j "Tab khác đã lưu".
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Gửi toàn bộ tài liệu (≤ 5 MB) với body `{ "revision": 41, "document": {…} }`; server `UPDATE seat_map SET draft = :doc, draft_revision = draft_revision + 1 WHERE seat_map_id = :id AND draft_revision = :rev` và trả `{ "revision": 42 }`; 0 dòng → 409 `REVISION_CONFLICT`.
  - Debounce 2 giây sau thay đổi cuối; đang lưu thì gom thay đổi vào lần lưu sau. Body > 256 KB được client nén `Content-Encoding: gzip`; API giải nén bằng một filter `GzipRequestFilter` (giới hạn 5 MB sau giải nén).
  - Lỗi mạng hoặc 5xx: thử lại sau 2, 4, 8, 16, 30 giây; chỉ báo "Chưa lưu được" trên thanh trên. Tài liệu chưa lưu được giữ thêm trong IndexedDB theo `seat_map_id` để khôi phục sau khi tải lại.
  - 409: dừng tự lưu, hiện hộp thoại 04j; "Tải lại bản mới nhất" bỏ thay đổi cục bộ.
  - Ngăn xếp undo không lưu server.
- **Ghi vào:** DOC-22, DOC-57.

### DR-37 · Sửa sơ đồ sau khi xuất bản sự kiện: khóa từ giờ mở bán — ⚠ lệch SDD gốc — **Đổi: khóa toàn bộ sơ đồ từ giờ mở bán**
- **Vấn đề:** SDD gốc mục 7.8 cho sửa sơ đồ cả khi đang bán: so phiên bản theo UUID (thêm/bỏ ghế, đổi nhãn, đổi loại vé, zone), áp dụng trong một transaction, tất cả hoặc không. Đây là phần nặng nhất của P5 (diff nhiều nhóm, câu `UPDATE` có điều kiện cho từng nhóm, danh sách xung đột, chờ lock của lệnh giữ vé) và là mục thứ ba trong thứ tự cắt giảm của SDD gốc (mục 16).
- **Quyết định (Owner chốt):** Sơ đồ **khóa toàn bộ từ `sale_starts_at`**, bất kể đã có ai mua hay chưa. Không có diff, không có chặn ghế lẻ khi đang bán.
  - Trước giờ mở bán (event `DRAFT`, hoặc `PUBLISHED` với `now() < sale_starts_at`): sửa và xuất bản phiên bản sơ đồ tự do như ở P4.
  - Xuất bản phiên bản mới khi event đã `PUBLISHED` nhưng chưa tới giờ mở bán: **dựng lại kho vé ghế và zone** trong một transaction, `SET LOCAL lock_timeout = '5s'`:
    1. `SELECT … FROM event … FOR UPDATE`; trong SQL kiểm tra `now() < sale_starts_at`, sai → 409 `MAP_LOCKED_AFTER_SALE`.
    2. `DELETE FROM inventory_unit WHERE event_id = :e AND ticket_type_id = ANY(:seatAndZoneTypes) AND status = 'AVAILABLE'`; số dòng phải bằng tổng unit của các loại vé đó, khác thì `ROLLBACK` và 409 `MAP_LOCKED_AFTER_SALE` (chỉ xảy ra khi đua đúng lúc mở bán). Xóa pool zone.
    3. Chèn lại unit ghế và pool zone từ phiên bản mới như bước 3–4 của DR-27; cập nhật `event.seat_map_version_id`; commit. Hết `lock_timeout` → 409 `MAP_PUBLISH_BUSY`.

    Trước giờ mở bán không lệnh giữ vé nào thành công (DR-41 bước 2), nên mọi unit đều `AVAILABLE`; phép kiểm số dòng ở bước 2 chặn trường hợp duy nhất còn lại là đua với giây mở bán.
  - Từ giờ mở bán (event `PUBLISHED`, `PAUSED`, `ENDED`, `CANCELLED` với `now() >= sale_starts_at`): `PUT /organizer/maps/{id}/draft` và `POST /organizer/maps/{id}/publish` đều trả 409 `MAP_LOCKED_AFTER_SALE`. Editor mở ở chế độ chỉ đọc với thông báo "Sơ đồ đã khóa vì sự kiện đã mở bán"; vẫn nhân bản được sang sự kiện khác (DR-31).
  - Sau giờ mở bán, người tổ chức vẫn: sửa thông tin sự kiện (DR-25, email đổi lịch DR-29), đổi giá và sức chứa loại vé GA/zone (DR-30), tạm dừng/mở lại, đóng bán sớm (DR-24), hủy sự kiện (DR-28).
- **Hệ quả:** Không có thuật toán diff, không có danh sách xung đột, không đổi nhãn hay loại vé trên unit đang tồn tại; `seat_index` luôn khớp phiên bản đang dùng vì kho vé được dựng lại cùng phiên bản. Bỏ mã `MAP_VERSION_CONFLICT` (DR-64). Muốn đổi bố cục sau giờ mở bán thì hủy sự kiện và tạo sự kiện mới bằng nhân bản sơ đồ.
- *Đổi 2026-10-06:* đề xuất cũ cho mọi loại thay đổi khi đang bán, so theo 10 nhóm thao tác với câu `UPDATE` có điều kiện cho từng nhóm, trả `MAP_VERSION_CONFLICT` kèm danh sách xung đột và cập nhật lại `seat_index`.
- **Ghi vào:** DOC-23, DOC-22, DOC-57, DOC-89.

### DR-38 · Lưu ảnh sự kiện và ảnh mặt bằng — **Đổi: ảnh lưu ở object storage tương thích S3**
- **Vấn đề:** SDD gốc mục 6.3 và 7.3 có ảnh sự kiện và ảnh nền mặt bằng; mục 14.4 giới hạn loại và kích thước ảnh nền; nhưng kiến trúc không có kho file (chỉ PostgreSQL, Redis). Canvas Studio 02 có "Chọn ảnh … 1920 × 1080 · 420 KB", tỷ lệ 16:9.
- **Các phương án:** (1) Bảng `media` kiểu `bytea` trong PostgreSQL: một nơi lưu, backup chung, không thêm container; tốn dung lượng và bộ đệm của DB. (2) Volume Docker do nginx phục vụ: nhanh, nhưng gắn chặt vào một máy. (3) Object storage tương thích S3: tách byte khỏi DB, chuyển sang S3/R2 thật chỉ bằng cấu hình; thêm container và SDK.
- **Quyết định (Owner chốt):** Phương án 3.
  - Code chỉ phụ thuộc API S3: AWS SDK for Java v2 `S3Client`, `forcePathStyle(true)`, `requestChecksumCalculation(WHEN_REQUIRED)` (bản SDK mới mặc định gửi checksum CRC mà nhiều kho tương thích S3 không nhận). Cấu hình `STORAGE_S3_ENDPOINT`, `STORAGE_S3_REGION`, `STORAGE_S3_BUCKET` (mặc định `ticket-media`), `STORAGE_S3_ACCESS_KEY`, `STORAGE_S3_SECRET_KEY`. Đổi sang AWS S3, Cloudflare R2 hay Garage không sửa code.
  - Trong compose: service `storage` dùng SeaweedFS (`chrislusf/seaweedfs`, `weed server -s3`, volume `storage-data`). Không dùng MinIO: bản cộng đồng đã ngừng phát hành image và repo bị lưu trữ năm 2026. Bucket private; API tạo bucket lúc khởi động nếu chưa có.
  - Bảng `media` chỉ giữ metadata:

    ```sql
    CREATE TABLE media (
      media_id     uuid PRIMARY KEY DEFAULT uuidv7(),
      organizer_id uuid NOT NULL REFERENCES organizer,
      purpose      text NOT NULL CHECK (purpose IN ('EVENT_IMAGE','FLOOR_PLAN')),
      content_type text NOT NULL CHECK (content_type IN ('image/jpeg','image/png','image/webp')),
      object_key   text NOT NULL UNIQUE,              -- 'media/<media_id>', bất biến
      size_bytes   int  NOT NULL CHECK (size_bytes <= 5242880),
      width        int  NOT NULL,
      height       int  NOT NULL,
      sha256       bytea NOT NULL,
      created_at   timestamptz NOT NULL DEFAULT now()
    );
    ```

  - Tải lên `POST /organizer/media` (multipart; ảnh sự kiện ≤ 2 MB, mặt bằng ≤ 5 MB; kiểm tra magic bytes, đọc kích thước bằng `ImageIO`, từ chối > 8000 px mỗi cạnh): sinh `media_id`, `PutObject` **trước** (kèm `Content-Type`), rồi chèn dòng `media`. Chèn lỗi → xóa object (cố gắng một lần); nếu xóa cũng lỗi thì object sót lại trong bucket, chấp nhận (hiếm, chỉ tốn dung lượng; `make reset` xóa sạch), không có job quét bucket (DR-74). Không có transaction nào giữ connection DB trong lúc gọi storage.
  - Phục vụ `GET /media/{id}` (công khai, không tiền tố `/api/v1`): API đọc dòng, stream object từ storage, `Cache-Control: public, max-age=31536000, immutable`, `ETag` = sha256; nginx `proxy_cache` 1 ngày nên storage chỉ bị gọi khi cache trượt. Storage không mở ra ngoài, không dùng presigned URL: URL ảnh ổn định, cache được, và không cần chính sách bucket công khai (mỗi kho S3 cài khác nhau).
  - Không resize ở server; client hiển thị `object-fit: cover`. Nhân bản sơ đồ (DR-31) tham chiếu cùng `media_id`.
- **Hệ quả:** Thêm container `storage` và volume; `make reset` xóa cả volume này. Backup DB không chứa ảnh. Test tích hợp module `media` chạy Testcontainers với cùng image SeaweedFS. Spike S-01 kiểm tra thêm `PutObject`, `GetObject`, `DeleteObject` của AWS SDK v2 với SeaweedFS.
- *Đổi 2026-10-06:* đề xuất cũ là phương án 1 (byte ảnh trong cột `bytea`).
- **Ghi vào:** ADR-0015, DOC-07, DOC-15, DOC-18, DOC-37, DOC-62.

### DR-39 · Kiến trúc editor và cách đo NFR-06 — 🔬 spike — **Chốt**
- **Vấn đề:** SDD gốc mục 7.9 và 13.2 chốt hướng (một shape tùy biến cho ghế, R-tree, mức chi tiết theo zoom, bitmap khi kéo, store ngoài React) nhưng NFR-06 "30 fps trên laptop phổ thông" không có máy chuẩn và cách đo.
- **Quyết định (Owner chốt):**
  - Store: Zustand vanilla, trạng thái `{ doc, selection, tool, viewport, history }`; mọi thay đổi `doc` qua `Command { do(doc), undo(doc), label }`, áp dụng bất biến bằng Immer; `history` tối đa 200.
  - Render: Konva Stage với 4 layer như SDD gốc 7.2; layer ghế là một `Konva.Shape` với `sceneFunc` vẽ trực tiếp các ghế trong khung nhìn lấy từ rbush; react-konva chỉ dùng cho overlay và tay nắm. `listening(false)` cho layer ghế; hit-test bằng rbush.
  - Mức chi tiết: < 40% vẽ khối section; 40–150% ghế; ≥ 150% thêm số ghế (SDD gốc 7.9).
  - Máy chuẩn cho NFR-06: laptop 4 nhân, đồ họa tích hợp, Chrome stable, màn hình 1920 × 1080, `devicePixelRatio = 1`; cấu hình thực tế ghi vào EXP-09. Đo fps bằng `requestAnimationFrame` trong script Playwright pan/zoom 10 giây, lấy p5 của fps khung hình.
  - Spike **S-04**: nguyên mẫu 20.000 ghế với Konva custom shape, đo fps pan/zoom và thời gian mở.
- **Ghi vào:** ADR-0014, DOC-22, DOC-79.

### DR-40 · Nhãn ghế lưu vào unit; ghế blocked không có unit — **Chốt**
- **Vấn đề:** Vé và email cần "Khán đài A · Hàng C · Ghế 9", trình xem cần ánh xạ ghế sang thứ tự, nhưng SDD gốc không có bảng ghế. SDD gốc cũng không nói ghế `blocked` có unit hay không.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Không có bảng ghế. Khi xuất bản event hoặc phiên bản mới, unit ghế nhận `seat_index` (thứ tự ghế trong tài liệu: duyệt `rows` rồi `seats`), `section_name`, `row_label`, `seat_number`. Phiên bản mới chỉ xuất bản được trước giờ mở bán và dựng lại toàn bộ unit ghế (DR-37), nên `seat_index` và nhãn luôn khớp phiên bản đang dùng.
  - Ghế `blocked` không có unit (hoặc unit `REMOVED` nếu bị chặn sau khi đã có). Sức chứa SEAT = số ghế không blocked gán loại vé đó.
- **Ghi vào:** DOC-14, DOC-23.

---

## G. Kho vé và giữ vé

### DR-41 · Quy tắc của lệnh giữ vé — ⚠ lệch SDD gốc — **Đổi: bỏ giới hạn số vé mỗi đơn**
- **Vấn đề:** SDD gốc mục 8.2, 12.2 cho câu claim và định dạng request, nhưng không chốt: giới hạn vé là theo đơn (mục 6.1 "giới hạn vé mỗi đơn") hay theo người (mục 5.3 "áp giới hạn vé mỗi người"); làm gì khi người mua đã có reservation đang mở (ràng buộc mục 8.1) mà muốn chọn lại; thứ tự câu lệnh trong transaction; mã lỗi khi ngoài khung mở bán.
- **Quyết định (Owner chốt):**
  - **Không có giới hạn số vé mỗi đơn hay mỗi người** (⚠: SDD gốc 6.1 có "giới hạn vé mỗi đơn", 5.3 có "giới hạn vé mỗi người"; Owner bỏ ngày 2026-10-06). Người tổ chức không cấu hình gì; giao diện không hiện câu "Mỗi đơn tối đa …". Một người vẫn chỉ có một reservation đang mở mỗi sự kiện (ràng buộc SDD gốc 8.1).
  - Giới hạn kỹ thuật, không phải tính năng: một lệnh giữ vé claim tối đa `reservation.max-units-per-hold` = 50 unit (tổng số ghế + tổng `quantity`), để transaction giữ vé nằm trong `statement_timeout` 2 giây và giữ lock ngắn. Cấu hình nền tảng, không theo sự kiện. Vượt → 422 `VALIDATION_FAILED` với `rule = too_many_units`; bộ tăng giảm và giỏ ghế ở giao diện dừng ở mức này.
  - Kiểm tra request trước khi lấy connection: 1–20 dòng `items`; `seatIds` không trùng, ≥ 1 mỗi dòng; `quantity` ≥ 1; cùng `zoneId` hoặc `ticketTypeId` không xuất hiện hai lần; tổng ≤ 50.
  - Transaction (READ COMMITTED, `SET LOCAL statement_timeout = '2s'`, `SET LOCAL lock_timeout = '1s'`):
    1. `INSERT INTO idempotency_key … ON CONFLICT DO NOTHING` (DR-45).
    2. Đọc event (không khóa): phải `PUBLISHED` và `sale_starts_at <= now() < sale_ends_at`; không thì 409 `EVENT_NOT_ON_SALE` (kèm `displayStatus`).
    3. `INSERT INTO reservation (…) VALUES (:rid, …, 'ACTIVE', now() + :hold) ON CONFLICT (user_id, event_id) WHERE status IN ('ACTIVE','EXPIRING') DO NOTHING`; 0 dòng → rollback, 409 `ACTIVE_RESERVATION_EXISTS` kèm `reservationId` của reservation đang mở.
    4. Claim ghế (một câu cho mọi ghế), rồi từng pool (SDD gốc 8.2). Thiếu → rollback, 409 `SEATS_UNAVAILABLE` (`unavailableSeatIds`) hoặc `INSUFFICIENT_CAPACITY` (`poolId`, `requested`).
    5. Đọc loại vé (giá, tên) và nhãn unit, chèn `reservation_item`, chèn `orders` (`PENDING_PAYMENT`, `amount` tính ở server).
    6. Cập nhật response vào dòng idempotency; commit.
  - Ghế phải thuộc loại vé `SEAT`, `zoneId` phải là pool `ZONE` của event, `ticketTypeId` của dòng GA phải là loại vé `GA`; sai → 422 `VALIDATION_FAILED`.
  - Tổng 0 đồng vẫn tạo order `PENDING_PAYMENT`; client gọi `POST /orders/{id}/confirm-free`.
- **Hệ quả:** Màn Chọn chỗ khi gặp `ACTIVE_RESERVATION_EXISTS` đưa người mua tới màn Thanh toán của reservation đó (nút "Tiếp tục thanh toán") hoặc cho hủy để chọn lại. Không có chống đầu cơ theo số lượng: một người mua được nhiều đơn liên tiếp, mỗi đơn tối đa 50 vé; rate limit (DR-55, DR-56) và phòng chờ (DR-57) là lớp hạn chế duy nhất. Rủi ro này ghi ở master plan §8.
- *Đổi 2026-10-06:* đề xuất cũ là giới hạn theo đơn bằng cột `event.max_tickets_per_order` (mặc định 8, tối đa 10), vượt → 422 `MAX_TICKETS_EXCEEDED`.
- **Ghi vào:** DOC-24, DOC-37, DOC-03, DOC-46, DOC-47.

### DR-42 · Tham số giữ vé và job trả vé — **Chốt**
- **Vấn đề:** SDD gốc mục 17 để ngỏ "thời hạn giữ vé 10 phút … cần chốt". Mục 8.5 nói reservation `EXPIRING` "kẹt quá 2 phút được lấy lại", còn mục 8.4 nói Stripe lỗi thì "thử lại ở vòng sau"; mục 8.5 báo `ACTIVE` quá hạn hơn 60 giây, mục 14.3 lại nói 2 phút.
- **Quyết định (Claude chốt, Owner ủy quyền):**

  | Key | Mặc định | Ý nghĩa |
  | --- | --- | --- |
  | `reservation.hold-duration` | `PT10M` | Thời hạn giữ, một giá trị cho toàn nền tảng |
  | `reservation.expiry.interval` | `PT5S` | Nhịp job (`fixedDelay`) |
  | `reservation.expiry.batch-size` | `200` | Số reservation mỗi vòng |
  | `reservation.expiry.lease` | `PT30S` | Reservation `EXPIRING` được lấy lại khi `expiring_since` cũ hơn mức này |
  | `reservation.payment-min-remaining` | `PT30S` | Tạo PaymentIntent bị từ chối nếu còn ít hơn |
  | `payment.stripe.cancel-timeout` | `PT5S` | Timeout gọi hủy PaymentIntent |

  Vòng job: (a) `UPDATE reservation SET status = 'EXPIRING', expiring_since = now(), close_reason = 'TIMEOUT' WHERE reservation_id IN (SELECT reservation_id FROM reservation WHERE status = 'ACTIVE' AND expires_at < now() ORDER BY expires_at LIMIT :batch FOR UPDATE SKIP LOCKED) RETURNING reservation_id`; (b) lấy thêm reservation `EXPIRING` có `expiring_since < now() - lease` bằng `UPDATE … SET expiring_since = now() … FOR UPDATE SKIP LOCKED` (gia hạn lease); (c) xử lý từng cái theo DR-43 bước "trả vé". Stripe lỗi → để nguyên, lease 30 giây sau lấy lại.

  Kiểm tra bất biến báo: `ACTIVE` quá `expires_at` hơn 60 giây; `EXPIRING` có `expiring_since` cũ hơn 2 phút (hai ngưỡng tách cho hai lỗi khác nhau).
- **Hệ quả:** Thời hạn 10 phút được chốt và không cấu hình theo sự kiện. NFR-03 (trả vé trong 30 giây sau `expires_at`) đạt được khi job chạy: độ trễ tối đa ≈ 5 giây nhịp + thời gian gọi Stripe.
- **Ghi vào:** DOC-24, DOC-34.

### DR-43 · Hủy giữ vé: đường nhanh trong request — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 8.3 cho người mua hủy chuyển `ACTIVE → EXPIRING` rồi job trả vé. Canvas Thanh toán hứa "Đổi ý? Ghế được trả lại ngay cho người khác", và người mua bấm "Chọn lại chỗ" sẽ gặp `ACTIVE_RESERVATION_EXISTS` cho tới khi job chạy (tối đa 5 giây + gọi Stripe).
- **Quyết định (Owner chốt):** `DELETE /reservations/{id}` (cần `Idempotency-Key`):
  1. Transaction ngắn: `UPDATE reservation SET status = 'EXPIRING', close_reason = 'BUYER_CANCELLED', expiring_since = now() WHERE reservation_id = :id AND user_id = :uid AND status = 'ACTIVE'`.
  2. Ngay trong request, chạy đúng thủ tục "trả vé" mà job dùng: đọc `orders.payment_intent_id` (câu lệnh mới, sau commit bước 1); có PaymentIntent thì gọi hủy (timeout 3 giây); hủy được hoặc không có → transaction trả vé: `EXPIRING → CANCELLED`, unit `HELD → AVAILABLE` (`reservation_id = NULL`), order `PENDING_PAYMENT → CANCELLED`; sau commit xóa cờ hết vé của các pool liên quan.
  3. Kết quả: 200 `{ "status": "CANCELLED" }`; Stripe lỗi hoặc timeout → 202 `{ "status": "EXPIRING" }`, job hoàn tất sau.
  4. Reservation đã `EXPIRING`/đóng → 200 với trạng thái hiện tại. PaymentIntent đã `succeeded` → 409 `PAYMENT_ALREADY_SUCCEEDED`, giao diện chuyển sang màn Kết quả.
- **Ghi vào:** DOC-24, DOC-26.

### DR-44 · Thanh toán đến trễ và trạng thái chờ hoàn tiền — ⚠ lệch SDD gốc — **Đổi: không giữ lại vé; đơn sang `REFUND_PENDING`**
- **Vấn đề:** SDD gốc mục 9.5 cho "giữ lại đúng ghế rồi chạy transaction xác nhận", nhưng máy trạng thái reservation (8.3) coi `EXPIRED`, `CANCELLED` là trạng thái cuối, và máy trạng thái order (9.2) chỉ có `EXPIRED → PAID/NEEDS_REVIEW`, thiếu `CANCELLED → …`. Canvas Kết quả có biến thể "Tiền về trễ, hết vé" hứa "Bạn sẽ được hoàn lại toàn bộ" kèm chỗ trống [THỜI GIAN HOÀN TIỀN], [EMAIL HỖ TRỢ]; SDD gốc không gửi gì cho người mua và không có cách đánh dấu đã hoàn tay.
- **Quyết định (Owner chốt):**
  - **Thanh toán đến sau khi reservation đã đóng thì không phát hành vé**, kể cả khi ghế vẫn còn trống (⚠: SDD gốc 9.5 thử giữ lại vé). Máy trạng thái reservation giữ nguyên như SDD gốc 8.3, không thêm chuyển trạng thái nào.
  - Trạng thái `NEEDS_REVIEW` của SDD gốc được thay bằng hai trạng thái rõ nghĩa (⚠ đổi tên): `REFUND_PENDING` (đã nhận tiền, phải hoàn) và `REFUNDED` (người vận hành đã hoàn). Order có các trạng thái `PENDING_PAYMENT`, `PAID`, `EXPIRED`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED`.

    | Từ | Sang | Kích hoạt | `refund_reason` |
    | --- | --- | --- | --- |
    | `EXPIRED`, `CANCELLED` | `REFUND_PENDING` | Webhook `succeeded` (hoặc job đối chiếu) đến sau khi reservation đã đóng | `LATE_PAYMENT` |
    | `PENDING_PAYMENT` | `REFUND_PENDING` | Số tiền hoặc tiền tệ của PaymentIntent khác order | `AMOUNT_MISMATCH` |
    | `PENDING_PAYMENT` | `REFUND_PENDING` | Xác nhận thấy event `CANCELLED` (DR-28) | `EVENT_CANCELLED` |
    | `PAID` | `REFUND_PENDING` | Người tổ chức hủy sự kiện (DR-28) | `EVENT_CANCELLED` |
    | `REFUND_PENDING` | `REFUNDED` | Người vận hành ghi kết quả hoàn tiền theo RB-01 | giữ nguyên |
  - Luồng trễ là một transaction ngắn: `UPDATE orders SET status = 'REFUND_PENDING', refund_reason = 'LATE_PAYMENT' WHERE order_id = :o AND status IN ('EXPIRED','CANCELLED')`, chèn outbox `EMAIL_REFUND_PENDING`, log mức ERROR (thiết kế ở DR-43/DR-47 làm luồng này hiếm; khác 0 là dấu hiệu cần điều tra).
  - Cột `orders`: `refund_reason text CHECK (refund_reason IN ('LATE_PAYMENT','AMOUNT_MISMATCH','EVENT_CANCELLED'))`, `refund_reference text`, `refunded_at timestamptz`. `REFUND_PENDING → REFUNDED` bằng `UPDATE … WHERE status = 'REFUND_PENDING'` trong RB-01 (chưa có giao diện admin).
  - Email `EMAIL_REFUND_PENDING` một mẫu `refund-pending` với nội dung theo `refund_reason`; chỗ trống của canvas lấy từ cấu hình `support.email` và `support.refund-sla-text` (key i18n, mặc định "5–10 ngày làm việc"). Màn Kết quả biến thể 3 và màn Vé của tôi hiện trạng thái "Chờ hoàn tiền"/"Đã hoàn tiền".
  - Kiểm tra bất biến báo số đơn `REFUND_PENDING` (theo lý do) và số khoản tiền về trễ trong 24 giờ.
- **Hệ quả:** Bỏ cột `reservation.confirmed_late`; luồng trễ không chạm `inventory_unit`, nên không có tranh chấp kho vé nào ở đây. Người mua trả tiền muộn luôn phải chờ hoàn tiền thủ công, kể cả khi chỗ còn trống. EXP-06 đo thêm "số đơn `REFUND_PENDING` lý do `LATE_PAYMENT`" với kỳ vọng 0.
- *Đổi 2026-10-05:* đề xuất cũ là thêm chuyển `EXPIRED/CANCELLED → CONFIRMED` cho reservation, claim lại đúng ghế và số lượng, phát hành vé nếu claim đủ, ngược lại `NEEDS_REVIEW`; cột `review_reason`, `review_resolved_at`, email `EMAIL_PAYMENT_REVIEW`.
- **Ghi vào:** DOC-14, DOC-26, DOC-27, ADR-0004, DOC-49, DOC-50, DOC-51, DOC-65.

### DR-45 · Chi tiết idempotency — **Đổi: băm body thô**
- **Vấn đề:** SDD gốc mục 8.6 chốt cơ chế nhưng không nói hash request tính trên gì, request lỗi có được lưu không, áp cho endpoint nào, thiếu header thì sao, và tạo PaymentIntent (gọi Stripe ngoài transaction) dùng bảng key thế nào.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Header `Idempotency-Key` (UUID) bắt buộc ở `POST /events/{id}/reservations`, `DELETE /reservations/{id}`, `POST /orders/{id}/confirm-free`, `POST /orders/{id}/payment-intent`; thiếu → 400 `IDEMPOTENCY_KEY_REQUIRED`.
  - `request_hash = SHA-256(operation + "\n" + path + "\n" + body thô)`, băm đúng các byte nhận được, không chuẩn hóa JSON. Client gửi lại với cùng key thì gửi lại đúng chuỗi body đã gửi lần đầu (frontend giữ chuỗi đã serialize cùng với key).
  - Câu đầu tiên của transaction: `INSERT … ON CONFLICT (user_id, idem_key) DO NOTHING`. 0 dòng nghĩa là transaction kia đã commit: đọc dòng đã lưu; khác hash → 422 `IDEMPOTENCY_KEY_REUSED`; cùng hash → trả lại `response_status` và `response_body`, header `Idempotent-Replayed: true`.
  - Chỉ kết quả thành công (2xx) được lưu, vì lỗi làm rollback cả dòng key. Gửi lại sau một 409 sẽ chạy lại thật; đúng ý nghĩa vì 409 không để lại tác dụng phụ.
  - `payment-intent` idempotent tự nhiên theo `order_id` (DR-47); header được nhận để client thống nhất nhưng không ghi vào bảng (không lưu `client_secret` vào database).
  - Dọn key cũ hơn 24 giờ (DR-74).
- *Đổi 2026-10-06:* đề xuất cũ băm `JCS(body)` (chuẩn hóa JSON theo RFC 8785); bỏ để không cần thư viện chuẩn hóa ở đường giữ vé. Checksum tài liệu sơ đồ (DR-32) vẫn dùng JCS.
- **Ghi vào:** DOC-25.

### DR-46 · Cờ hết vé — **Chốt**
- **Vấn đề:** SDD gốc mục 10.1 có key `soldout:{pool}` "đặt khi đếm lại thấy pool không còn unit AVAILABLE, xóa khi có vé được trả lại", nhưng không nói khi nào đếm lại, ghế (không có pool) dùng cờ nào, và cờ kẹt khi API dừng giữa chừng.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - `soldout:pool:{poolId}` = `1`, TTL 30 giây. Đặt sau khi một lệnh giữ vé nhận `INSUFFICIENT_CAPACITY` và câu đếm `SELECT count(*) FROM inventory_unit WHERE pool_id = :p AND status = 'AVAILABLE'` (index một phần) trả 0. Xóa sau commit của mọi transaction trả unit về pool đó. TTL bảo đảm cờ kẹt tự hết.
  - Ghế: không có cờ riêng; lệnh giữ ghế kiểm tra bitmap tình trạng (DR-62): mọi ghế yêu cầu đều đã `HELD`/`SOLD` trong snapshot → 409 `SEATS_UNAVAILABLE` không chạm database.
  - Cờ là gợi ý: Redis không khả dụng thì bỏ qua bước này.
- **Ghi vào:** DOC-28, DOC-17.

---

## H. Thanh toán

### DR-47 · Tham số PaymentIntent và tranh chấp giữa tạo PaymentIntent với job trả vé — **Chốt**
- **Vấn đề:** SDD gốc mục 9.2 nói server tạo PaymentIntent ngoài transaction và lưu `payment_intent_id`. Có một khe hở không được nêu: job chuyển reservation sang `EXPIRING` và đọc `payment_intent_id` (đang NULL) trong lúc request tạo PaymentIntent đã gọi Stripe nhưng chưa lưu ID; job trả vé vì "chưa từng có PaymentIntent", rồi người mua trả tiền thành công → thanh toán đến trễ.
- **Quyết định (Owner chốt):**
  - Gọi Stripe: `amount` theo DR-13, `currency` chữ thường, `payment_method_types = ["card"]`, `capture_method = automatic`, `metadata = { order_id, reservation_id, event_id }`, `description = "Order <order_id>"`, idempotency key Stripe `pi-create:<order_id>`. Client Stripe: connect timeout 2 giây, read timeout 10 giây, `maxNetworkRetries = 2`. Khóa phiên bản API theo SDK.
  - Thủ tục `POST /orders/{id}/payment-intent`:
    1. Đọc order và reservation; reservation phải `ACTIVE` và `expires_at - now() >= 30 giây` (SQL), không thì 409 `RESERVATION_NOT_ACTIVE` hoặc `PAYMENT_WINDOW_TOO_SHORT`.
    2. Order đã có `payment_intent_id` → lấy PaymentIntent từ Stripe, trả `clientSecret`.
    3. Chưa có → tạo PaymentIntent (ngoài transaction).
    4. Transaction ngắn: `SELECT 1 FROM reservation WHERE reservation_id = :r AND status = 'ACTIVE' FOR SHARE`; có dòng → `UPDATE orders SET payment_intent_id = :pi WHERE order_id = :o AND payment_intent_id IS NULL`; commit; trả `{ clientSecret, amount, currency, expiresAt }`.
    5. Không có dòng (job đã chuyển `EXPIRING`) → hủy PaymentIntent vừa tạo, trả 409 `RESERVATION_NOT_ACTIVE`.

    `FOR SHARE` ở bước 4 tuần tự hóa với câu `UPDATE … SET status = 'EXPIRING'` của job: job chờ tới khi ID đã lưu rồi mới đọc `payment_intent_id`.
- **Hệ quả:** Test bắt buộc: chèn độ trễ giữa bước 3 và 4, cho reservation hết hạn đúng lúc đó, chạy 500 lần, không có PaymentIntent nào thành công mà vé đã trả.
- **Ghi vào:** DOC-26, DOC-09, ADR-0005.

### DR-48 · Xử lý webhook — **Chốt**
- **Vấn đề:** SDD gốc mục 9.3 chốt luồng; chưa có đường dẫn cụ thể, dung sai thời gian chữ ký, kiểm tra số tiền, các event hoàn tiền/tranh chấp tạo ngoài hệ thống (hoàn tay theo DR-44).
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - `POST /api/v1/webhooks/stripe`, đọc body thô (không qua Jackson), `Webhook.constructEvent(payload, sigHeader, secret, 300)`; sai → 400.
  - Event đăng ký: `payment_intent.succeeded`, `payment_intent.payment_failed`, `payment_intent.canceled`, `charge.refunded`, `charge.dispute.created`. Hai loại cuối chỉ ghi log mức WARN kèm `order_id` (giai đoạn này không có luồng hoàn tiền).
  - Một transaction: `INSERT INTO stripe_event … ON CONFLICT DO NOTHING`; 0 dòng → 200. Tìm order theo `payment_intent_id`, không thấy thì theo `metadata.order_id`; không thấy nữa → log ERROR, `outcome = IGNORED`, 200.
  - `succeeded`: kiểm tra `amount_received`, `currency` khớp order (DR-44), rồi transaction xác nhận (SDD gốc 9.2) hoặc luồng trễ. `payment_failed`: `UPDATE orders SET last_payment_error = :decline_code WHERE … status = 'PENDING_PAYMENT'`. `canceled`: chỉ ghi `stripe_event`.
  - Lỗi bất kỳ → rollback, 500; Stripe gửi lại.
- **Ghi vào:** DOC-26, DOC-09.

### DR-49 · Job đối chiếu khi webhook thất lạc — **Chốt**
- **Vấn đề:** SDD gốc mục 9.3 có "một job nền hỏi lại Stripe … quá 15 phút", không có nhịp, giới hạn, cách gọi handler.
- **Quyết định (Claude chốt, Owner ủy quyền):** `PaymentReconcileJob` mỗi 60 giây chọn tối đa 50 order có `payment_intent_id` khác NULL và một trong hai: `status = 'PENDING_PAYMENT' AND created_at < now() - 15 phút`; hoặc reservation `EXPIRING` có `expiring_since < now() - 2 phút`. Với mỗi order, lấy PaymentIntent; `succeeded` → gọi đúng handler `PaymentSucceededHandler` của webhook (các câu ghi có điều kiện nên chạy trùng vô hại); trạng thái khác → bỏ qua. Mỗi lần gọi handler ghi log INFO `reconciled=true`.
- **Ghi vào:** DOC-26.

### DR-50 · Thời điểm trong luồng checkout phía client — **Chốt**
- **Vấn đề:** SDD gốc mục 9.2, 13.2 nói client hiển thị Payment Element cùng đồng hồ và hỏi `GET /orders/{id}` "cho đến khi đơn là PAID", không có nhịp hỏi, giới hạn, hành vi 3-D Secure chuyển trang.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Màn Thanh toán tải → `GET /reservations/{id}`; tổng > 0 thì gọi `payment-intent` ngay, nạp Payment Element với `locale` của giao diện và `appearance` từ token (DR-68).
  - Nút trả tiền vô hiệu khi còn < 30 giây; đồng hồ chuyển kiểu "Sắp hết giờ" khi < 2 phút (design system).
  - `stripe.confirmPayment({ redirect: "if_required", confirmParams: { return_url: <APP_BASE_URL>/orders/<orderId> } })`. Không redirect → chuyển sang màn Kết quả.
  - Màn Kết quả hỏi `GET /orders/{id}` mỗi 2 giây trong 60 giây, sau đó mỗi 10 giây tới 10 phút; trong lúc chờ hiện "Đang xác nhận thanh toán". `PAID` → vé; `REFUND_PENDING` → biến thể "Tiền về trễ" (chờ hoàn tiền, DR-44); `EXPIRED`/`CANCELLED` → về Chọn chỗ.
- **Ghi vào:** DOC-26, DOC-48, DOC-49.

### DR-51 · Cổng thanh toán giả cho test và thực nghiệm — **Chốt**
- **Vấn đề:** EXP-06 cần "Stripe giả lập" bơm webhook quanh `expires_at`; EXP-02, EXP-05 chạy hàng nghìn thanh toán, vượt rate limit của Stripe test mode; E2E không nên phụ thuộc mạng. SDD gốc không nói giả lập bằng gì (stripe-mock của Stripe không gửi webhook).
- **Quyết định (Owner chốt):**
  - Port `PaymentGateway { createIntent, retrieveIntent, cancelIntent }` và `WebhookSignatureVerifier` trong module `payment`; adapter `StripePaymentGateway` (mặc định) và `FakePaymentGateway` (profile `fake-payments`).
  - Fake lưu PaymentIntent trong bảng `fake_payment_intent` (chỉ có trong profile, migration riêng `db/migration-fake`), mô phỏng trạng thái và lỗi hủy như Stripe, ký webhook bằng HMAC giống định dạng `Stripe-Signature` và POST tới chính webhook endpoint. Endpoint điều khiển `/fake-payments/intents/{id}/succeed?delayMs=&duplicates=&shuffle=` chỉ bật trong profile.
  - Frontend: biến môi trường build `VITE_PAYMENTS=fake` thay Payment Element bằng form giả có nút "Pay (fake)".
  - API từ chối khởi động nếu profile `fake-payments` bật cùng khóa `sk_live_…`.
- **Ghi vào:** ADR-0017, DOC-26, DOC-70.

---

## I. Vé và thông báo

### DR-52 · Định dạng mã vé — **Đổi: tiền tố do người tổ chức nhập**
- **Vấn đề:** SDD gốc mục 9.4 nói "mã vé ngẫu nhiên, duy nhất"; canvas dùng `GM-4K7P-92XD`.
- **Quyết định (Claude chốt, Owner ủy quyền):** `PP-XXXX-XXXX`: `PP` là `event.ticket_code_prefix`, `XXXX-XXXX` là 8 ký tự Crockford Base32 (40 bit từ `SecureRandom`). Tiền tố mặc định `TK`; người tổ chức nhập 2 chữ cái in hoa ở Studio 02 khi event còn `DRAFT` (dữ liệu mẫu đặt `GM` cho "Hòa nhạc Giao Mùa" như canvas, DR-78). Trùng `code` (xác suất không đáng kể) → sinh lại trong cùng transaction tối đa 3 lần.
- *Đổi 2026-10-06:* đề xuất cũ tự suy tiền tố từ chữ cái đầu của hai từ cuối trong tên sự kiện sau khi bỏ dấu; bỏ vì cần xử lý bỏ dấu tiếng Việt mà giá trị thấp.
- **Ghi vào:** DOC-27, DOC-14.

### DR-53 · Outbox: nhịp, lease, backoff; hủy PaymentIntent không qua outbox — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 4.1–4.2 nói outbox gửi "email, lệnh hủy PaymentIntent"; mục 8.4 lại để job trả vé tự gọi Stripe hủy và chờ kết quả trước khi trả vé. Outbox chạy bất đồng bộ nên không cho job biết khi nào hủy xong. SDD gốc không có nhịp, số lần thử, backoff. Gửi email trong lúc giữ transaction vi phạm mục 10.4.
- **Quyết định (Owner chốt):**
  - Hủy PaymentIntent do job trả vé (và đường nhanh DR-43) gọi trực tiếp; outbox chỉ chứa email.
  - `OutboxRelay` chạy mỗi 1 giây: (1) transaction claim `UPDATE outbox SET attempts = attempts + 1, next_attempt_at = now() + interval '60 seconds' WHERE outbox_id IN (SELECT outbox_id FROM outbox WHERE status = 'PENDING' AND next_attempt_at <= now() ORDER BY next_attempt_at LIMIT 50 FOR UPDATE SKIP LOCKED) RETURNING *`; commit. (2) Gửi ngoài transaction. (3) Thành công → `status = 'SENT', sent_at = now()`; lỗi → `next_attempt_at = now() + least(10 s × 2^(attempts−1), 1 giờ)`, `last_error`; `attempts >= 12` → `FAILED`, log ERROR.
  - Giao ít nhất một lần; `Message-ID` cố định `<outbox_id@APP_DOMAIN>` để hộp thư gộp bản trùng.
- **Ghi vào:** DOC-27, DOC-15, ADR-0006.

### DR-54 · Mẫu email và người gửi — **Chốt**
- **Vấn đề:** SDD gốc mục 9.4 nêu nội dung email vé; canvas có 3 mẫu (đăng nhập, vé, đổi lịch) bằng tiếng Việt và tên tạm "ticket". Chưa có công cụ dựng mẫu, người gửi, đa ngôn ngữ, bản text.
- **Quyết định (Claude chốt, Owner ủy quyền):** Thymeleaf, mỗi mẫu một bản HTML (CSS inline theo design system) và một bản text, chuỗi lấy từ `messages_<locale>` (DR-10). Mẫu: `magic-link`, `tickets`, `event-changed`, `refund-pending` (nội dung theo `refund_reason`, DR-44). Người gửi `MAIL_FROM` (mặc định `ticket <no-reply@ticket.localhost>`); tên sản phẩm `APP_NAME` (mặc định `ticket`, tên tạm theo design system). Link tuyệt đối dựng từ `APP_BASE_URL`. Không đính kèm, không QR (SDD gốc 9.4).
- **Ghi vào:** DOC-27, DOC-51.

---

## J. Chịu tải và kiểm soát tiếp nhận

### DR-55 · Rate limit ở edge (nginx) — **Chốt**
- **Vấn đề:** SDD gốc mục 10.1 có "rate limit theo IP tại Nginx; cache trang sự kiện và tài liệu sơ đồ", không có con số. Bộ sinh tải k6 chạy từ một IP sẽ bị chính giới hạn này chặn.
- **Quyết định (Claude chốt, Owner ủy quyền):**

  | Zone `limit_req` | Áp cho | Nhịp | Burst |
  | --- | --- | --- | --- |
  | `api_ip` | `/api/` | 20 r/s | 40 `nodelay` |
  | `auth_ip` | `/api/v1/auth/magic-link` | 10 r/phút | 5 |
  | `hold_ip` | `POST /api/v1/events/*/reservations` | 5 r/s | 10 |

  `limit_req_status 429`; trang lỗi JSON dạng Problem Details `RATE_LIMITED`. IP trong biến `RATE_LIMIT_ALLOWLIST` (map `geo`) được miễn: dùng cho máy sinh tải trong thực nghiệm. Cache: `GET /api/v1/events/{id}/map?version=n` cache 1 ngày ở nginx (`proxy_cache`), `GET /api/v1/events/{id}` cache 5 giây, `/media/` cache 1 ngày. `client_max_body_size 6m`.
- **Ghi vào:** DOC-28, DOC-62.

### DR-56 · Token bucket theo người dùng; mất Redis thì không có dự phòng trong bộ nhớ — ⚠ lệch SDD gốc — **Đổi: bỏ rate limiter dự phòng**
- **Vấn đề:** SDD gốc mục 10.1 có token bucket bằng Lua theo người dùng và mục 10.2 có "rate limiter trong bộ nhớ với ngưỡng thấp" khi mất Redis, không có tham số.
- **Quyết định (Owner chốt):** Một script `token_bucket.lua` (key, capacity, refill/giây, now_ms, cost) trả `allowed`, `retryAfterMs`.

  | Bucket | Key | Sức chứa | Nạp lại |
  | --- | --- | --- | --- |
  | Giữ vé, hủy giữ | `rl:hold:{userId}` | 5 | 1 mỗi 2 giây |
  | Tạo PaymentIntent | `rl:pi:{userId}` | 5 | 1 mỗi 5 giây |
  | Hỏi vị trí hàng đợi | `rl:queue:{userId}` | 10 | 1 mỗi giây |

  Giới hạn magic link theo IP đếm trong database (DR-21), không dùng Redis.

  Mất Redis (lệnh Redis lỗi hoặc quá `spring.data.redis.timeout` = 200 ms): bỏ qua bucket theo người dùng, không có limiter dự phòng. Lớp bảo vệ còn lại: rate limit theo IP ở nginx (DR-55) và bulkhead của lệnh giữ vé (DR-61) trả 503 `OVERLOADED` khi database đầy. Lệnh giữ vé của sự kiện `high_demand` trả 503 `OVERLOADED` kèm `Retry-After: 5` trong lúc mất Redis (không có phòng chờ thì không mở cửa cho cả đám đông). Không có circuit breaker: mỗi request tự thử Redis, timeout ngắn giữ độ trễ trong giới hạn.
- **Hệ quả:** Không có Bucket4j, không có circuit breaker. Đổi lại: khi mất Redis, sự kiện thường không còn giới hạn theo người dùng, chỉ còn theo IP; tính đúng đắn không đổi vì database vẫn quyết định (SDD gốc 4.2). EXP-08 kiểm tra kịch bản này.
- *Đổi 2026-10-06:* đề xuất cũ: khi mất Redis dùng Bucket4j trong bộ nhớ với tham số chia đôi, giới hạn toàn cục 200 lệnh giữ vé/giây cho sự kiện `high_demand`, phát hiện Redis hỏng bằng circuit breaker; bucket `rl:magic:{ip}` cho magic link.
- **Ghi vào:** DOC-28, DOC-17.

### DR-57 · Mô hình phòng chờ, lượt vào và rời hàng — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 10.2 cho hàng đợi "tự bật khi số người trong khu đặt vé chạm `max_active`", nhưng khi hàng đợi chưa bật thì không có tập `admitted` nào để đếm. `pass_ttl` = 10 phút trong SDD gốc, canvas Phòng chờ hiện "Lượt vào còn 04:58" và "Khi đã giữ vé, bạn có đủ 10 phút để thanh toán". Canvas có nút "Rời hàng"/"Rời phòng chờ" và trạng thái "Tạm hết vé"; SDD gốc không có endpoint rời hàng.
- **Quyết định (Owner chốt):**
  - Kiểm soát tiếp nhận **luôn chạy** cho mọi sự kiện trong khung mở bán. Vào màn Chọn chỗ, client gọi `POST /events/{id}/queue`: còn chỗ (`|admitted| < max_active`), hàng trống và còn hạn mức nhịp trong giây đó → `ADMITTED` ngay, giao diện không hiện phòng chờ (hàng đợi "trong suốt"). Ngược lại → `WAITING`, giao diện chuyển tới Phòng chờ. `high_demand = true` chỉ thêm hai điều: phòng chờ trước giờ mở bán từ `prequeue_opens` và xáo ngẫu nhiên lúc mở bán.
  - `pass_ttl` = **5 phút** để chọn chỗ (⚠: SDD gốc 10 phút); giữ vé thành công thì lượt vào được kéo tới `expires_at` của reservation (SDD gốc 10.2 bước 5). Reservation đóng mà không thanh toán thì lượt vào còn thêm 2 phút để chọn lại (`admission.repick-grace`), sau đó hết.
  - `DELETE /events/{id}/queue` xóa người dùng khỏi `prequeue`, `queue`, `admitted`, `seen`; chỗ được cấp cho người kế tiếp ở vòng job sau.
  - Trạng thái trả về: `PRE_QUEUE`, `WAITING`, `ADMITTED`, `PAUSED` ("Tạm hết vé": mọi vé còn lại đang được giữ), `SOLD_OUT`, `NOT_IN_QUEUE`. "Mất kết nối" là trạng thái chỉ của client.
  - Tham số theo sự kiện lấy mặc định cấu hình (SDD gốc 10.2) và không sửa từ giao diện ở giai đoạn này: `max_active` 500, `admit_rate` 50/giây, `pass_ttl` 5 phút, `prequeue_opens` 30 phút, `idle_timeout` 2 phút.
  - Redis không khả dụng: sự kiện thường giữ vé không cần lượt vào; sự kiện `high_demand` trả 503 `OVERLOADED` (DR-56).
- **Ghi vào:** DOC-28, ADR-0008, DOC-45.

### DR-58 · Kiểm tra lượt vào ở lệnh giữ vé — ⚠ lệch SDD gốc — **Đổi: không có token vào cửa, kiểm tra thẳng trong Redis**
- **Vấn đề:** SDD gốc mục 10.2 nói "token vào cửa có chữ ký, gắn với `user_id` và sự kiện", không nói định dạng, khóa, header. Nhưng hạn thật của lượt vào nằm trong Redis (`admitted:{e}`, được kéo dài khi giữ vé), nên endpoint giữ vé đằng nào cũng phải tra Redis; người dùng thì đã có từ session.
- **Quyết định (Owner chốt):** Không có token vào cửa. Endpoint giữ vé, khi sự kiện đang trong khung có kiểm soát tiếp nhận (DR-57), kiểm tra `ZSCORE admitted:{eventId} <userId> > now_ms` với `userId` lấy từ session. Không có điểm hoặc đã hết hạn → 429 `QUEUE_REQUIRED`; giao diện đưa người mua về Phòng chờ. Redis không khả dụng → theo DR-56, DR-57.
- **Hệ quả:** Không có secret `ADMISSION_TOKEN_SECRET`, không có header `X-Admission-Token`, không có định dạng token phải ký và kiểm. Lượt vào không chuyển được cho người khác vì gắn với session. Đổi lại, mỗi lệnh giữ vé tốn một lệnh `ZSCORE` (đã tốn sẵn ở thiết kế cũ).
- *Đổi 2026-10-06:* đề xuất cũ: token `v1.<payload>.<HMAC-SHA256>` chứa `userId`, `eventId`, `exp`, gửi qua header `X-Admission-Token`, kiểm tra chữ ký rồi mới tra `ZSCORE`.
- **Ghi vào:** DOC-28, DOC-32.

### DR-59 · Job cấp lượt, script Lua và nhịp hỏi vị trí — **Chốt**
- **Vấn đề:** SDD gốc mục 10.2 mô tả job mỗi giây và cấu trúc key, không có script, cách xáo nhóm chờ trước, ngưỡng của `retryAfterSeconds`, công thức ước lượng khi chưa có dữ liệu.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - `AdmissionTicker` (`fixedRate` 1 giây) duyệt tập `queue:events` (sự kiện đang trong khung từ `prequeue_opens` tới `sale_ends_at`). Mỗi sự kiện: `SET admit-lock:{e} <node> NX PX 900`; chạy `admit.lua` (ARGV: `now_ms, max_active, admit_rate, pass_ttl_ms, idle_timeout_ms`): `ZREMRANGEBYSCORE admitted:{e} -inf now`; loại tối đa 1.000 người có `seen` cũ hơn `idle_timeout`; `n = min(max_active − ZCARD admitted, admit_rate, ZCARD queue)`; `ZPOPMIN queue n` → `ZADD admitted now+pass_ttl`; ghi số người được cấp vào `admit-hist:{e}` (list 60 phần tử).
  - Trước khi cấp, đọc snapshot tình trạng (DR-62): không còn vé `AVAILABLE` → không cấp; còn vé `HELD` → `queue-state:{e} = PAUSED`; không còn gì → `SOLD_OUT`.
  - Mở bán với `high_demand`: Java đọc `SMEMBERS prequeue:{e}`, gán điểm `SecureRandom` trong [0, 1), `ZADD` theo lô 1.000, rồi đặt `queue-state:{e} = OPEN`; người đến sau nhận điểm `1 + INCR queue-seq:{e}`.
  - `retryAfterSeconds`: vị trí ≤ 200 → 3; ≤ 2.000 → 10; còn lại → 30; `PRE_QUEUE` → 30.
  - `estimatedWaitSeconds = position / (Σ admit-hist / 60)`; chưa có dữ liệu → `null`, giao diện hiện "Đang ước tính".
- **Ghi vào:** DOC-28, DOC-17.

### DR-60 · Nhịp vào cố định, không tự điều chỉnh — ⚠ lệch SDD gốc — **Đổi: bỏ backpressure tự động**
- **Vấn đề:** SDD gốc mục 10.2 nói "`admit_rate` giảm khi p95 của lệnh giữ vé vượt ngưỡng và tăng lại khi database rảnh", không có ngưỡng hay thuật toán.
- **Quyết định (Owner chốt):** Không tự điều chỉnh. `admit_rate` lấy thẳng từ cấu hình `admission.admit-rate` (mặc định 50/giây, DR-57). EXP-05 tăng dần nhịp vào để tìm điểm gãy; giá trị an toàn tìm được ghi thành mặc định mới bằng một dòng trong nhật ký chốt. Database quá tải giữa chừng vẫn được bulkhead (DR-61) chặn bằng 503.
- **Hệ quả:** Không có vòng điều khiển đọc metric, không có key `admission:rate-factor`. Đổi lại, nhịp vào không tự thích nghi khi máy chậm hơn lúc đo; chấp nhận vì môi trường chạy là một máy cố định.
- *Đổi 2026-10-06:* đề xuất cũ: AIMD toàn cục mỗi 5 giây theo p95 của `ticket_hold_duration_seconds` và `hikaricp_connections_pending` (nhân 0,7 khi quá tải, cộng 0,1 khi rảnh), lưu `admission:rate-factor` trong Redis.
- **Ghi vào:** DOC-28, DOC-34.

### DR-61 · Connection pool, bulkhead và timeout — **Chốt**
- **Vấn đề:** SDD gốc mục 10.4 chốt nguyên tắc (bulkhead nhỏ hơn pool, trả 503 thay vì chờ, `statement_timeout` 2 giây) mà không có số.
- **Quyết định (Claude chốt, Owner ủy quyền):**

  | Key | Mặc định |
  | --- | --- |
  | `spring.datasource.hikari.maximum-pool-size` | 40 |
  | `spring.datasource.hikari.connection-timeout` | 1000 ms |
  | `hold.bulkhead.permits` (giữ + hủy giữ) | 24 |
  | `hold.bulkhead.wait` | 0 (thử lấy, không chờ) |
  | `hold.statement-timeout` / `hold.lock-timeout` | 2 s / 1 s (`SET LOCAL`) |
  | PostgreSQL `max_connections` | 100 |

  16 connection còn lại cho webhook, job, studio, đọc. Hết permit → 503 `OVERLOADED`, `Retry-After` 1–3 giây ngẫu nhiên. Tham số PostgreSQL cho máy thực nghiệm (`shared_buffers`, `work_mem`, `synchronous_commit` giữ `on`) ghi ở DOC-62.
- **Ghi vào:** DOC-28, DOC-24, DOC-34.

### DR-62 · Định dạng và cache của tình trạng chỗ — **Đổi: cache trong tiến trình**
- **Vấn đề:** SDD gốc mục 7.10 nói `GET /events/{id}/availability` trả "danh sách ghế không còn trống" và cache 1–2 giây. Với 20.000 ghế, danh sách UUID có thể tới ~750 KB mỗi lần, client hỏi mỗi 3–5 giây. Canvas phân biệt "Có người giữ" và "Đã bán", SDD gốc không tách.
- **Quyết định (Owner chốt):**

  ```json
  {
    "eventId": "0199b1c2-…",
    "mapVersion": 2,
    "generatedAt": "2026-11-10T03:00:01.200Z",
    "seats": { "count": 180, "held": "AAAAAAAg…", "sold": "AAAAAAAA…" },
    "pools": [{ "poolId": "0199…", "zoneKey": "zone-fan", "ticketTypeId": "0199…", "available": 86 }],
    "ticketTypes": [{ "ticketTypeId": "0199…", "available": 42, "held": 3 }]
  }
  ```

  `held`/`sold` là bitset base64, bit `i` (LSB trước trong mỗi byte) ứng với `seat_index = i` (DR-40); 20.000 ghế ≈ 2,5 KB mỗi bitset. Dựng bằng hai truy vấn (ghế `HELD`/`SOLD` theo index `unit_seat_taken_idx`; đếm `AVAILABLE` theo pool và loại vé). Cache Caffeine trong tiến trình theo `eventId`, hết hạn 2 giây sau khi ghi; lấy bằng `cache.get(eventId, loader)` nên nhiều request cùng lúc cho một sự kiện chỉ dựng một lần (Caffeine tự gộp). Hệ thống chạy một instance API (SDD gốc 14.1) nên không cần cache chung; không phụ thuộc Redis. Client hỏi mỗi 4 giây ± 0,5 giây, dừng khi tab ẩn; response `mapVersion` khác bản đang có → tải lại sơ đồ.
- *Đổi 2026-10-06:* đề xuất cũ cache trong Redis `avail:{e}` với khóa single-flight `avail-lock:{e}`, bản cũ `avail-stale:{e}` cho người thua và Caffeine dự phòng khi mất Redis. Định dạng bitmap giữ nguyên.
- **Ghi vào:** ADR-0013, DOC-29, DOC-37.

---

## K. API

### DR-63 · Quy ước API — **Chốt**
- **Vấn đề:** SDD gốc mục 12 có danh sách đường dẫn không tiền tố, trong khi nginx proxy `/api` (mục 14.1); "problem details" và "cursor" chưa có định dạng; SDD gốc 7.6 viết `PUT /maps/{id}/draft` còn 12.1 viết `/organizer/maps/{id}/draft`.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Tiền tố `/api/v1`; mọi đường dẫn trong SDD gốc hiểu là sau tiền tố; đường dẫn sơ đồ là `/organizer/maps/…`. Ngoại lệ không tiền tố: `/media/{id}`.
  - JSON camelCase; trường không có giá trị trả `null` (không bỏ).
  - Lỗi: RFC 9457 `application/problem+json`, `{ "type": "https://errors.ticket.dev/<code>", "title", "status", "detail", "code", "requestId", …mở rộng }`; `title`/`detail` theo `Accept-Language` (DR-10).
  - Phân trang: `?limit=` (mặc định 20, tối đa 100) và `?cursor=`; response `{ "items": [...], "nextCursor": "…" | null }`; cursor là base64url của `{"k":<khóa sắp xếp>,"id":"<uuid>"}`.
  - `X-Request-Id`: nginx sinh `$request_id` nếu client không gửi; API đặt vào MDC `trace_id` và trả lại trong header.
  - OpenAPI 3.1 ở `/v3/api-docs` (chỉ trong mạng nội bộ compose và profile `dev`), tag theo module.
- **Ghi vào:** DOC-36.

### DR-64 · Bổ sung bảng mã lỗi — **Chốt**
- **Vấn đề:** Bảng mã lỗi ở SDD gốc mục 12.3 thiếu các tình huống mà SDD gốc và canvas đều có (ngoài khung bán, đã có reservation mở, hủy sự kiện có đơn, xung đột phiên bản sơ đồ…).
- **Quyết định (Claude chốt, Owner ủy quyền):** Giữ mọi mã của SDD gốc, thêm:

  | HTTP | `code` | Khi nào |
  | --- | --- | --- |
  | 400 | `IDEMPOTENCY_KEY_REQUIRED` | Thiếu header (DR-45) |
  | 401 | `LOGIN_LINK_INVALID` | Token magic link hết hạn, đã dùng hoặc bị thay |
  | 403 | `ORGANIZER_PROFILE_REQUIRED` | Vào `/organizer/**` khi chưa có hồ sơ |
  | 404 | `NOT_FOUND` | Không có hoặc không thuộc tổ chức của session |
  | 409 | `EVENT_NOT_ON_SALE` | Ngoài khung bán, `PAUSED`, `DRAFT`, `ENDED`, `CANCELLED` |
  | 409 | `ACTIVE_RESERVATION_EXISTS` | Đã có reservation mở cho sự kiện |
  | 409 | `PAYMENT_WINDOW_TOO_SHORT` | Còn < 30 giây |
  | 409 | `PAYMENT_ALREADY_SUCCEEDED` | Hủy giữ khi tiền đã về |
  | 409 | `EVENT_STATE_CONFLICT` | Chuyển trạng thái không hợp lệ |
  | 409 | `STALE_EVENT_VERSION` | `PATCH` với `rowVersion` cũ |
  | 409 | `ORGANIZER_EXISTS` | Lập hồ sơ lần hai |
  | 409 | `TICKET_TYPE_IN_USE`, `CAPACITY_BELOW_USED` | DR-26, DR-30 |
  | 409 | `MAP_LOCKED_AFTER_SALE` (sửa hoặc xuất bản sơ đồ từ giờ mở bán), `MAP_PUBLISH_BUSY` | DR-37 |
  | 409 | `MAP_ALREADY_EXISTS` | Nhân bản sơ đồ vào event đã có sơ đồ (DR-31) |
  | 413 | `PAYLOAD_TOO_LARGE` | Tài liệu sơ đồ > 5 MB, ảnh vượt giới hạn |
  | 422 | `PUBLISH_PRECONDITIONS_FAILED` | Kèm danh sách điều kiện chưa đạt |
  | 422 | `MAP_VALIDATION_FAILED` | Kèm `issues` (DR-35) |
  | 422 | `TICKET_TYPE_LIMIT_REACHED`, `MEDIA_INVALID` | DR-26, DR-38 |
  | 500 | `INTERNAL_ERROR` | Kèm `requestId` (màn E5) |
  | 503 | `PAYMENT_PROVIDER_UNAVAILABLE` | Stripe lỗi khi tạo PaymentIntent |
  | 503 | `EMAIL_PROVIDER_UNAVAILABLE` | SMTP lỗi khi gửi magic link (DR-21) |
- **Ghi vào:** DOC-35, DOC-36, DOC-40.

### DR-65 · Endpoint còn thiếu cho các màn hình — **Chốt**
- **Vấn đề:** Canvas cần dữ liệu mà SDD gốc mục 12.1 không có endpoint: danh sách sự kiện của studio kèm số vé (Studio 01), đọc một sự kiện để sửa (Studio 02–06), danh sách điều kiện xuất bản (Studio 06), sơ đồ tô theo trạng thái (Studio 07), rời hàng (Phòng chờ), tải ảnh, đổi ngôn ngữ, đọc sơ đồ theo phiên bản.
- **Quyết định (Claude chốt, Owner ủy quyền):** Thêm:

  | Endpoint | Màn |
  | --- | --- |
  | `GET /organizer/events` (kèm `sold`, `held`, `available`, `capacity`) | Studio 01 |
  | `GET /organizer/events/{id}` (kèm loại vé, `rowVersion`, trạng thái từng bước) | Studio 02–06 |
  | `GET /organizer/events/{id}/publish-checks` | Studio 06 |
  | `GET /organizer/events/{id}/seat-status` | Studio 07 |
  | `GET /organizer/events/{id}/map` (trả `seatMapId`, bản nháp, `revision`, phiên bản mới nhất) | Studio 04 |
  | `GET /organizer/maps/{id}/versions` | Studio 04 hộp thoại xuất bản |
  | `GET /organizer/maps` (sơ đồ của tổ chức, kèm tên sự kiện), `POST /organizer/maps/{id}/clone` | Studio 04 "Dùng lại sơ đồ từ sự kiện khác" (DR-31) |
  | `POST /organizer/media`, `GET /media/{id}` | Studio 02, Studio 04 ảnh nền |
  | `DELETE /events/{id}/queue` | Phòng chờ |
  | `PATCH /me` (`locale`) | Bộ chọn ngôn ngữ |
  | `GET /events/{id}/map?version=n` (cache dài hạn; không có `version` → 302 tới phiên bản đang dùng) | Chọn chỗ |

  `GET /orders/{id}` trả kèm danh sách vé khi `PAID` (SDD gốc 9.2 bước 6).
- **Ghi vào:** DOC-37.

### DR-66 · Giờ máy chủ cho đồng hồ đếm ngược — **Chốt**
- **Vấn đề:** SDD gốc mục 13.2 nói đồng hồ tính từ `expiresAt` và "độ lệch giờ so với server", không nói lấy giờ server từ đâu. Canvas có ba đồng hồ: giữ vé, lượt vào, mở bán sau.
- **Quyết định (Claude chốt, Owner ủy quyền):** Mọi response API có header `X-Server-Time` (epoch ms). Interceptor của client tính `offset = serverTime − (t_gửi + t_nhận)/2`, lấy trung vị 5 mẫu gần nhất. Đồng hồ = `expiresAt − (Date.now() + offset)`, cập nhật mỗi 250 ms, hiển thị `mm:ss`; về 0 thì gọi lại API thay vì tự kết luận.
- **Ghi vào:** DOC-36, DOC-29, DOC-48.

---

## L. Frontend và trải nghiệm

### DR-67 · Bản đồ URL và giữ lựa chọn qua đăng nhập — **Chốt**
- **Vấn đề:** SDD gốc mục 13.1 có luồng màn hình, không có URL; mục 5.3 nói "lựa chọn ghế được giữ ở client và khôi phục sau khi đăng nhập" mà không nói lưu ở đâu.
- **Quyết định (Claude chốt, Owner ủy quyền):**

  | URL | Màn |
  | --- | --- |
  | `/` | Danh sách sự kiện |
  | `/events/:eventId` | Sự kiện |
  | `/login?returnTo=` · `/auth/callback?token=` | Đăng nhập · xác minh |
  | `/events/:eventId/queue` | Phòng chờ |
  | `/events/:eventId/seats` | Chọn chỗ hoặc Chọn số lượng (sự kiện chỉ có GA) |
  | `/checkout/:reservationId` | Thanh toán |
  | `/orders/:orderId` | Kết quả |
  | `/me/tickets` | Vé của tôi |
  | `/studio` · `/studio/profile` · `/studio/events/new` | Tổng quan · Lập hồ sơ · Tạo sự kiện |
  | `/studio/events/:eventId/(info|ticket-types|map|preview|publish|sales)` | Các bước studio |

  Lựa chọn lưu ở `localStorage["tb.selection.<eventId>"] = { seatIds, zones: {zoneKey: qty}, ga: {ticketTypeId: qty}, savedAt }` (không dùng `sessionStorage` vì link đăng nhập thường mở ở tab mới), khôi phục nếu < 30 phút rồi xóa; đọc/ghi bọc `try/catch`, lỗi thì bỏ qua. Chunk tải lười: `seat-picker` (Konva), `studio-map` (Konva + editor), `checkout` (Stripe.js).
- **Ghi vào:** DOC-38.

### DR-68 · Áp dụng design system "Vé giấy" — **Chốt**
- **Vấn đề:** Canvas có design system v0.1 (màu, chữ, khoảng cách, bo góc, trạng thái ghế, component); SDD gốc không nhắc. Font đang nạp từ Google Fonts; tên sản phẩm "ticket" là tên tạm.
- **Quyết định (Claude chốt, Owner ủy quyền):** Chép nguyên token v0.1 thành CSS custom properties (`--paper #ECEEF1`, `--surface #FFFFFF`, `--line #D3D7DE`, `--ink-muted #5B6272`, `--ink-2 #2E323C`, `--ink #12141A`, `--stamp #D63312`, `--stamp-deep #B5290C`, `--stamp-tint #FCE9E4`, `--confirm #1B7F4B`, `--type-1…5 #BF2A78 #2747D9 #E5A21A #0B7F6F #6A3FD0`), thang khoảng cách bước 4 px, bo 4/6/999. Font tự host bằng `@fontsource` (Barlow Condensed 600/700, Be Vietnam Pro 400/500/600, IBM Plex Mono 400/500/600), không gọi Google Fonts lúc chạy. Không có dark mode ở giai đoạn này. Tên sản phẩm lấy từ `APP_NAME` (DR-54). Màu của Stripe Payment Element cấu hình qua `appearance.variables` từ cùng token.
- **Ghi vào:** DOC-39.

### DR-69 · Chế độ danh sách, điện thoại và tiếp cận của màn Chọn chỗ — **Chốt**
- **Vấn đề:** SDD gốc mục 13.2 yêu cầu chế độ danh sách dùng bằng bàn phím và ưu tiên điện thoại; canvas có nút chuyển "Sơ đồ / Danh sách", bản 390 px (M05), thông báo "Sơ đồ đang thu nhỏ … Phóng to để chọn ghế". Chưa có quy tắc chi tiết.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Danh sách: nhóm Section → Hàng; mỗi ghế còn trống là `<button aria-pressed>` có nhãn "Hàng C, ghế 9, VIP, 1.800.000 ₫"; ghế đang giữ/đã bán không hiện; khu vực và GA là bộ tăng giảm có `aria-label`.
  - Sơ đồ: dưới 40% zoom không chọn được ghế (chỉ thấy khối section) và hiện thông báo; chạm hai ngón để zoom, chạm một ngón để chọn; vùng chạm ghế tối thiểu 24 px màn hình (zoom tự đẩy lên khi chạm vào section ở mức thấp).
  - Điện thoại: tóm tắt đơn là thanh dưới cố định (số vé, tạm tính, nút giữ vé), mở thành bottom sheet.
  - Click ghế đang giữ hoặc đã bán: không thêm, hiện tooltip trạng thái.
- **Ghi vào:** DOC-46, DOC-38.

### DR-70 · Luồng soạn sự kiện trong studio và cách lưu — **Chốt**
- **Vấn đề:** SDD gốc mục 13.1 có "Soạn sự kiện: các bước thông tin, loại vé, sơ đồ, xem trước, xuất bản". Canvas có 5 bước + bước 6 "Bán vé" sau xuất bản, nút "Lưu bản nháp", "Lưu và sang …", "Có thay đổi chưa lưu", và bước Sơ đồ bị bỏ qua khi chỉ có GA. Chưa nói lưu tự động hay thủ công, hai tab sửa cùng event thì sao.
- **Quyết định (Claude chốt, Owner ủy quyền):** Bước Thông tin và Loại vé lưu thủ công (`PATCH` kèm `rowVersion`; 409 `STALE_EVENT_VERSION` → hộp thoại tải lại); rời trang khi có thay đổi chưa lưu → hộp thoại xác nhận. Bước Sơ đồ tự lưu (DR-36). "Tạo sự kiện" gọi `POST /organizer/events { name }` tạo `DRAFT` rồi vào bước Thông tin. Thanh bước cho nhảy tới bước đã xong; bước Sơ đồ ẩn khi không có loại vé `SEAT`/`ZONE`. Màn hình < 1024 px: các bước khác dùng được, bước Sơ đồ hiện màn 04m.
- **Ghi vào:** DOC-38, DOC-55, DOC-56, DOC-57, DOC-58, DOC-59, DOC-60.

### DR-71 · Số liệu bán vé cho người tổ chức — **Chốt**
- **Vấn đề:** SDD gốc mục 12.1 có `GET /organizer/events/{id}/sales` "theo loại vé"; canvas Studio 07 có thêm sơ đồ tô theo trạng thái, bộ lọc loại vé, "Cập nhật lúc …", nút "Làm mới"; Studio 01 có số đã bán/đang giữ/còn trống cho mỗi sự kiện.
- **Quyết định (Claude chốt, Owner ủy quyền):** `sales` tính bằng `SELECT ticket_type_id, status, count(*) FROM inventory_unit WHERE event_id = :e AND status <> 'REMOVED' GROUP BY 1, 2`, cache Redis 5 giây. `seat-status` trả đúng snapshot của DR-62 (dùng chung cache). Giao diện tự làm mới mỗi 15 giây và khi bấm "Làm mới". Studio 01 dùng một truy vấn gộp theo `event_id` cho tối đa 20 sự kiện mỗi trang.
- *Đổi (DR-62, DR-148):* "dùng chung cache" chỉ còn đúng ở nghĩa dùng chung một nguồn dữ liệu: snapshot ghế nằm trong Caffeine (`availability.cache-ttl`), còn số liệu `sales` vẫn nằm trong Redis (`sales.cache-ttl`).
- **Ghi vào:** DOC-60, DOC-37, DOC-29.

---

## M. Vận hành, kiểm thử và thực nghiệm

### DR-72 · Topology compose, cổng, lệnh và dữ liệu mẫu — **Chốt**
- **Vấn đề:** SDD gốc mục 14.1 có danh sách container. NFR-08 yêu cầu `docker compose up` chạy toàn bộ, nhưng `stripe-cli` cần khóa Stripe thật trong `.env`; chưa có cổng, healthcheck, tài khoản demo.
- **Quyết định (Claude chốt, Owner ủy quyền):**

  | Service | Cổng host | Healthcheck | Profile |
  | --- | --- | --- | --- |
  | `nginx` (frontend build + proxy) | 8080 | `GET /healthz` | mặc định |
  | `api` | — (9090 quản trị luôn mở trong container, không bao giờ ra host; `prometheus` chỉ ở `dev` và `obs`; *Đổi, DR-148*) | `/actuator/health/readiness` | mặc định |
  | `postgres` | 5432 | `pg_isready` | mặc định |
  | `redis` | 6379 | `redis-cli ping` | mặc định |
  | `mailpit` | 8025 (UI), 1025 (SMTP) | HTTP | mặc định |
  | `storage` (SeaweedFS S3) | — (8333 chỉ trong mạng compose) | HTTP | mặc định |
  | `stripe-cli` | — | — | `stripe` |
  | `prometheus`, `grafana` | 9091, 3000 | — | `obs` |

  `.env.example` mặc định `PAYMENTS_MODE=fake` để `docker compose up` chạy được không cần tài khoản Stripe; `make up-stripe` bật profile `stripe` với khóa test. `make dev` chạy hạ tầng trong compose, API bằng `./gradlew bootRun`, frontend `pnpm dev` (proxy `/api`). `make seed` tạo tài khoản `organizer@demo.test`, `buyer1@demo.test`…`buyer3@demo.test` và các sự kiện mẫu (DR-78).
- **Ghi vào:** DOC-61, DOC-62.

### DR-73 · Lệnh kiểm tra bất biến — ⚠ lệch SDD gốc — **Chốt**
- **Vấn đề:** SDD gốc mục 14.3 nói "một lệnh duy nhất … tự chạy mỗi 5 phút, và chạy tay", không nói chạy tay bằng cách nào, định dạng kết quả, mã thoát; ngưỡng reservation quá hạn không thống nhất (60 giây ở 8.5, 2 phút ở 14.3).
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - `InvariantChecker` trong module `invariant`; `@Scheduled` mỗi 5 phút; chạy tay `make invariants` = `docker compose run --rm api --spring.profiles.active=invariants` (chạy một lần, in JSON ra stdout, thoát 0 khi sạch, 1 khi có sai lệch).
  - Kiểm tra: các mục của SDD gốc 14.3, ngưỡng theo DR-42, cộng: đơn `PAID` có số vé `ISSUED` = Σ `quantity` của item; event `CANCELLED` còn đơn `PAID` hoặc vé `ISSUED` (DR-28); outbox `FAILED`; số đơn `REFUND_PENDING` theo `refund_reason` (DR-44); các kiểm tra unit ↔ vé bỏ qua event `CANCELLED`; unit `AVAILABLE`/`REMOVED` mà `reservation_id` khác NULL (đã có CHECK, kiểm lại cho chắc).
  - Kết quả: `{ "checkedAt", "durationMs", "violations": [{ "check": "POOL_UNIT_COUNT", "count": 1, "sample": ["<poolId>"] }] }`, tối đa 10 ID mỗi mục; có sai lệch thì log ERROR.
- **Ghi vào:** DOC-30, DOC-66.

### DR-74 · Lưu giữ dữ liệu và job dọn — **Đổi: bỏ quét bucket**
- **Vấn đề:** SDD gốc mục 8.6 dọn key idempotency sau 24 giờ; các bảng khác (token, session, webhook, outbox, ảnh không dùng) không có chính sách.
- **Quyết định (Claude chốt, Owner ủy quyền):** `RetentionJob` chạy 03:00 theo `PLATFORM_TIMEZONE`, xóa theo lô 5.000 dòng:

  | Dữ liệu | Giữ |
  | --- | --- |
  | `login_token` | Xóa 24 giờ sau `expires_at` |
  | `session` | Xóa 7 ngày sau khi hết hạn hoặc thu hồi |
  | `idempotency_key` | 24 giờ |
  | `stripe_event` | 30 ngày |
  | `outbox` | `SENT` 7 ngày, `FAILED` 30 ngày |
  | `media` không được tham chiếu | 24 giờ sau khi tạo: `DeleteObject` rồi xóa dòng |
  | reservation, order, ticket, unit | Giữ nguyên (đối soát về sau) |

  Dữ liệu cá nhân: email chỉ ở `app_user`, `login_token`, `organizer.contact_email`, payload outbox; không vào log.
- *Đổi 2026-10-06:* đề xuất cũ quét bucket bằng `ListObjectsV2` mỗi ngày để xóa object không có dòng `media`, và xóa token mã hóa trong outbox magic link; cả hai bỏ theo DR-38, DR-21.
- **Ghi vào:** DOC-18.

### DR-75 · Môi trường và mô hình tải của thực nghiệm — 🔬 spike — **Chốt**
- **Vấn đề:** NFR-02 nói "100.000 người dùng đồng thời"; một máy chạy k6 với 100.000 VU riêng lẻ không khả thi về bộ nhớ, và SDD gốc mục 3.4 tự nói con số sẽ hiệu chỉnh theo máy. Chưa có cấu hình máy, số lần lặp, cách tạo 100.000 người dùng có session (đăng nhập bằng magic link từng người là không thực tế).
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - "Người dùng đồng thời" = người dùng có mặt (đang chờ, chọn chỗ hoặc thanh toán) trong cùng cửa sổ mở bán. k6 mô phỏng bằng VU đa người dùng: mỗi VU quản lý một nhóm người dùng ảo bằng `http.asyncRequest`, tôn trọng `retryAfterSeconds`; S-06 đo số người dùng ảo tối đa một máy chạy được và chốt tỉ lệ.
  - Người dùng tải được tạo bằng script SQL `experiments/seed/users.sql` (100.000 `app_user` + session có giá trị biết trước), chỉ chạy trong profile `experiment`.
  - Mọi thực nghiệm chạy với `PAYMENTS_MODE=fake`, IP máy sinh tải trong `RATE_LIMIT_ALLOWLIST`, giới hạn tài nguyên container cố định (ghi ở DOC-70), lặp 3 lần, báo trung vị và min–max, ghi git SHA và cấu hình máy.
  - Spike **S-06**: k6 trên máy thực nghiệm, mục tiêu 100.000 người dùng ảo trong phòng chờ hỏi vị trí theo `retryAfter`; nếu không đạt, ghi trần thực tế và hiệu chỉnh NFR-02 bằng DR mới.
- **Ghi vào:** DOC-70, DOC-75.

### DR-76 · Bản cài đặt kho vé thay thế cho thực nghiệm — **Chốt**
- **Vấn đề:** SDD gốc mục 10.3 và 15.2 cần so phương án D với phương án A (EXP-10) và với "bản cài đặt ngây thơ" (đọc rồi ghi, không điều kiện). Không nói hai bản này sống ở đâu để không lọt vào chạy thật.
- **Quyết định (Claude chốt, Owner ủy quyền):** Interface `InventoryClaimer` trong `inventory.internal`; ba bản: `SkipLockedClaimer` (mặc định), `CounterClaimer` (phương án A, bảng `inventory_pool_counter` tạo bởi migration riêng `db/migration-experiment`), `NaiveClaimer` (`SELECT … status = 'AVAILABLE'` rồi `UPDATE … WHERE unit_id = …` không điều kiện trạng thái). Chọn bằng `inventory.strategy=skip-locked|counter|naive`; API từ chối khởi động với giá trị khác `skip-locked` nếu không bật profile `experiment`.
- **Ghi vào:** DOC-70, DOC-80, DOC-24.

### DR-77 · Chi tiết chiến lược kiểm thử — **Chốt**
- **Vấn đề:** SDD gốc mục 15.1 có loại test và công cụ; chưa có ngưỡng coverage, quy ước đặt tên, cách E2E lấy magic link, cách bắt thay đổi phá hợp đồng API.
- **Quyết định (Claude chốt, Owner ủy quyền):**
  - Coverage dòng (JaCoCo): `inventory`, `reservation`, `order`, `payment`, `admission` ≥ 85%; module khác ≥ 70%. Frontend `map-core` ≥ 90% (Vitest).
  - Test đồng thời: 64 luồng, `CountDownLatch` xuất phát cùng lúc, lặp 20 lần mỗi test.
  - E2E Playwright đọc magic link qua API Mailpit (`GET /api/v1/message/latest`), dùng `PAYMENTS_MODE=fake` trong CI; `make e2e-stripe` chạy với thẻ test thật khi có khóa.
  - Hợp đồng: `oasdiff breaking` so OpenAPI của PR với `dev`, báo lỗi khi có thay đổi phá vỡ.
  - Mỗi tài liệu thiết kế có tiền tố test riêng, đăng ký ở DOC-69.
- **Ghi vào:** DOC-69.

### DR-78 · Dữ liệu mẫu và kịch bản demo — **Chốt**
- **Vấn đề:** SDD gốc mục 16.1 có kịch bản 6 bước; canvas dùng ba sự kiện mẫu. Demo không có dữ liệu dựng sẵn hay phương án dự phòng khi một bước hỏng (ví dụ vẽ sơ đồ lỗi, máy demo yếu không chạy nổi k6 100.000).
- **Quyết định (Claude chốt, Owner ủy quyền):** `make seed` tạo đúng các sự kiện trên canvas: "Hòa nhạc Giao Mùa" (Nhà hát Bến Sông, 180 ghế VIP/Thường/Sinh viên + khu đứng 300 = 480 vé), "Hội thảo Nghề Thiết Kế" (chỉ GA: Đặt sớm đã hết, Phổ thông, Sinh viên), "Đêm nhạc Sân Thượng" (sắp mở bán, `high_demand`). Tên sự kiện là dữ liệu, không phải chuỗi giao diện. Mỗi bước demo có dự phòng: sơ đồ dựng sẵn để xuất bản nếu vẽ hỏng; bước k6 có cấu hình rút gọn (10.000 người dùng ảo) cho máy yếu; checklist trước demo và `make reset seed`.
- **Ghi vào:** DOC-81, DOC-61.

---

## N. Quyết định phát sinh khi viết tài liệu gate P1

Các mục dưới đây phát sinh khi viết DOC-14…83 của gate P1 (2026-10-06). Mỗi mục có nguồn đầy đủ ở tài liệu ghi trong dòng *Nguồn*; sổ chỉ tóm tắt. Người chốt là Claude (Owner ủy quyền); nhóm `ops` ở trạng thái Đề xuất chờ Owner duyệt. Các mục này xếp theo nhóm chủ đề riêng thay vì rải vào A–M để giữ số DR liên tục.

### Mã lỗi, quy ước API và cấu hình

#### DR-82 · Mã lỗi bổ sung cho lỗi kỹ thuật chung — **Chốt**
- **Vấn đề:** SDD gốc 12.3 và DR-64 không có mã cho JSON hỏng, sai phương thức, sai `Content-Type`, sai chữ ký webhook và sai CSRF; không có mã thì bộ xử lý phải bịa hoặc rơi vào `INTERNAL_ERROR`, sai về lớp lỗi.
- **Quyết định:** thêm 5 mã: 400 `BAD_REQUEST`, 400 `INVALID_SIGNATURE`, 403 `CSRF_TOKEN_INVALID`, 405 `METHOD_NOT_ALLOWED`, 415 `UNSUPPORTED_MEDIA_TYPE` (mục 3).
- **Hệ quả:** bảng có 40 mã; mỗi mã có key `errors.<code viết thường>` (giao diện) và `problem.<code viết thường>.*` (backend) trong DOC-40.
- **Nguồn:** `06-design/error-handling.md`

#### DR-83 · `rule` của `VALIDATION_FAILED` là `snake_case` và không phải `code` — **Chốt**
- **Vấn đề:** DR-12 và DR-25 viết "422 `invalid_timezone`", lẫn `rule` với `code`.
- **Quyết định:** mọi giá trị chữ thường như vậy là `rule` trong `errors[]`; `code` luôn chữ hoa (mục 3).
- **Hệ quả:** không thêm mã lỗi, test `ERR-03`.
- **Nguồn:** `06-design/error-handling.md`

#### DR-84 · `retryAfterSeconds` trong body trùng `Retry-After` — **Chốt**
- **Vấn đề:** SPA đọc header `Retry-After` qua CORS-less fetch được nhưng body tiện hơn cho interceptor và test.
- **Quyết định:** mọi 429/503 có cả header và thành viên `retryAfterSeconds` cùng giá trị nguyên giây ≥ 1.
- **Hệ quả:** nginx `RATE_LIMITED` (DR-55) cũng phải đặt hai nơi (DOC-62).
- **Nguồn:** `06-design/error-handling.md`

#### DR-85 · Một lần retry cho `CSRF_TOKEN_INVALID` — **Chốt**
- **Quyết định:** client gọi `GET /me` lấy `csrfToken` mới rồi gửi lại đúng một lần; lần hai vẫn lỗi coi như lỗi lập trình (mục 7).
- **Nguồn:** `06-design/error-handling.md`

#### DR-86 · Cache công khai của `GET /events` — **Chốt**
- **Vấn đề:** DR-55 chỉ nêu cache `GET /events/{id}` (5 giây), không nêu danh sách; trang chủ là endpoint được đọc nhiều nhất lúc mở bán.
- **Quyết định:** `GET /events` cache `public, max-age=5` cùng `proxy_cache` 5 giây, khóa gồm query string và `Accept-Language`.
- **Hệ quả:** sự kiện mới xuất bản hiện chậm tối đa 5 giây; thêm `proxy_cache_path` ở DOC-62.
- **Nguồn:** `07-api/api-guidelines.md`

#### DR-87 · Xuất OpenAPI cho CI bằng test tích hợp — **Chốt**
- **Vấn đề:** sinh `schema.d.ts` và `oasdiff` cần file OpenAPI mà không muốn thêm plugin Gradle chưa chắc tương thích Boot 4 (S-01).
- **Quyết định:** `OpenApiExportTest` (Testcontainers) ghi `backend/build/openapi.json`; `make contract` gọi nó.
- **Hệ quả:** thêm một test chậm ~vài giây; đổi sang plugin sau không đổi hợp đồng.
- **Nguồn:** `07-api/api-guidelines.md`

#### DR-88 · Body request chặt — **Chốt**
- **Vấn đề:** mặc định Jackson bỏ qua trường lạ, che lỗi gõ sai (`quantiy`).
- **Quyết định:** `spring.jackson.deserialization.fail-on-unknown-properties=true` → 400 `BAD_REQUEST`; client sinh từ OpenAPI không gửi trường lạ.
- **Hệ quả:** thêm trường request ở server là thay đổi phải đi cùng client mới (không phá vỡ nếu trường tùy chọn và client cũ không gửi).
- **Nguồn:** `07-api/api-guidelines.md`

#### DR-89 · Trường giờ của studio có hậu tố `Local` — **Chốt**
- **Vấn đề:** DR-12 nói form studio gửi giờ địa phương kèm `timezone` nhưng chưa nêu tên trường.
- **Quyết định:** request ghi dùng `startsAtLocal`, `endsAtLocal`, `saleStartsAtLocal`, `saleEndsAtLocal`, `prequeueOpensLocal` (`yyyy-MM-dd'T'HH:mm`) kèm `timezone`; response trả `startsAt`… UTC và `timezone`, cộng thêm các trường `…Local` để form điền lại không phải tính.
- **Hệ quả:** DOC-37 mục E của event dùng đúng tên này; DOC-20 chỉnh nếu khác.
- **Nguồn:** `07-api/api-guidelines.md`

### Mô hình dữ liệu (DOC-14, DOC-15)

#### DR-90 · Ngữ nghĩa đóng của `reservation` (`closed_at`, `close_reason`) — **Chốt**
- **Vấn đề:** DR-18 có `close_reason` và `closed_at` nhưng không nói mỗi lý do dẫn tới trạng thái cuối nào.
- **Quyết định (Claude, Owner ủy quyền):** `closed_at` đặt khi vào `CONFIRMED`, `EXPIRED`, `CANCELLED`; `TIMEOUT → EXPIRED`, `BUYER_CANCELLED` và `EVENT_CANCELLED → CANCELLED`; `close_reason` luôn có giá trị ở `EXPIRING`, `EXPIRED`, `CANCELLED` và NULL ở `ACTIVE`.
- **Hệ quả:** Job trả vé và đường hủy nhanh dùng chung một quy tắc; thêm `CHECK` theo `status` khi triển khai (DOC-14 §10).
- **Nguồn:** `05-data/domain-model.md`

#### DR-91 · Đơn `PENDING_PAYMENT` bị đóng vì hủy sự kiện chuyển `CANCELLED`, không phải `REFUND_PENDING` — **Chốt**
- **Vấn đề:** DR-28 hủy reservation `ACTIVE` bằng `EVENT_CANCELLED`, DR-44 không nói đơn `PENDING_PAYMENT` của chúng đi đâu.
- **Quyết định (Claude, Owner ủy quyền):** Đơn chưa thu tiền sang `CANCELLED`; chỉ đơn đã có tiền (`PAID`, hoặc `EXPIRED`/`CANCELLED` nhận thanh toán trễ) sang `REFUND_PENDING`.
- **Hệ quả:** Hàng đợi hoàn tiền chỉ chứa hoàn tiền thật; không gửi email chờ hoàn tiền cho đơn chưa trả tiền.
- **Nguồn:** `05-data/domain-model.md`

#### DR-92 · Index khóa ngoại còn thiếu, `inventory_pool.created_at` và khóa ngoại vòng của `event` — **Chốt**
- **Vấn đề:** DDL của DR-15…18 có khóa ngoại không index (trái quy ước DR-14), có vòng `event → seat_map_version → seat_map → event`, và `inventory_pool` thiếu `created_at`.
- **Quyết định (Claude, Owner ủy quyền):** Thêm 13 index khóa ngoại (`media_organizer_idx`, `event_media_idx`, `event_seat_map_version_idx`, `seat_map_organizer_idx`, `seat_map_clone_idx`, `pool_ticket_type_idx`, `reservation_event_idx`, `unit_ticket_type_idx`, `reservation_item_pool_idx`, `reservation_item_type_idx`, `orders_event_idx`, `ticket_event_idx`, `ticket_unit_idx`); thêm `inventory_pool.created_at`; tạo `event` không có khóa ngoại `seat_map_version_id` rồi `ALTER TABLE` thêm sau.
- **Hệ quả:** Chèn khi xuất bản chậm hơn đôi chút; `unit_ticket_type_idx` là index duy nhất thêm vào bảng nóng nên S-03 đo lại.
- **Nguồn:** `05-data/domain-model.md`

#### DR-93 · DDL bảng profile, đặt lại outbox `FAILED → PENDING` bằng tay, `CHECK` của `stripe_event.outcome` — **Chốt**
- **Vấn đề:** DDL của `fake_payment_intent` và `inventory_pool_counter` chưa nằm trong DR nào; dòng outbox `FAILED` không có đường phục hồi; `stripe_event.outcome` chưa có `CHECK`.
- **Quyết định (Claude, Owner ủy quyền):** Định nghĩa hai DDL ở DOC-15 §8; cho phép `FAILED → PENDING` bằng `UPDATE` tay (không có giao diện admin); thêm `CHECK` cho `outcome` khi DOC-26 chốt tập giá trị.
- **Hệ quả:** EXP-06 và EXP-08 chèn lỗi hủy PaymentIntent qua `fake_payment_intent.cancel_failure`.
- **Nguồn:** `05-data/ops-model.md`

#### DR-94 · Quy ước Flyway cho thư mục migration theo profile — **Chốt**
- **Vấn đề:** Thư mục migration của profile có thể có phiên bản thấp hơn bản đã áp dụng, hoặc profile bị tắt sau khi đã chạy; Flyway mặc định từ chối cả hai.
- **Quyết định (Claude, Owner ủy quyền):** `spring.flyway.out-of-order=true` và `spring.flyway.ignore-migration-patterns=*:missing`.
- **Hệ quả:** Lỏng hơn mặc định, chấp nhận được ở dự án một người; DOC-34 và DOC-62 mang cả hai khóa.
- **Nguồn:** `05-data/ops-model.md`

#### DR-95 · Index dọn dữ liệu và index giới hạn gửi magic link theo IP — **Chốt**
- **Vấn đề:** Job dọn của DR-74 và phép đếm giới hạn theo IP của DR-21 không có index hỗ trợ.
- **Quyết định (Claude, Owner ủy quyền):** Thêm `login_token_ip_idx`, `login_token_expires_idx`, `session_last_seen_idx`, `session_revoked_idx`, `stripe_event_received_idx`, `stripe_event_pi_idx`, `outbox_done_idx`.
- **Hệ quả:** Index nhỏ hoặc một phần trên bảng ít dòng.
- **Nguồn:** `05-data/ops-model.md`

### Xác thực, session và thông báo

#### DR-96 · Gửi SMTP và dựng mẫu email nằm ở `common.mail` — **Chốt**
- **Vấn đề:** DR-21 bắt `auth` gửi email magic link trực tiếp, nhưng DOC-12 §2.2 chỉ cho `auth` phụ thuộc `common`, còn mẫu Thymeleaf và SMTP thuộc `notification`.
- **Quyết định:** đặt `MailSender` (cài đặt `SmtpMailSender`, Spring Mail) và `EmailRenderer` (Thymeleaf + `MessageSource` theo locale) ở package `io.ticket.common.mail`; `auth` dùng trực tiếp, `notification.OutboxRelay` dùng cùng class. Mẫu email vẫn ở `resources/templates/email/`.
- **Hệ quả:** không cần nới `allowedDependencies`; `common` không phụ thuộc module nghiệp vụ nên luật 6 của DOC-12 §2.2 giữ nguyên.
- **Nguồn:** `06-design/auth-and-sessions.md`

#### DR-97 · Khóa advisory theo email khi đếm giới hạn gửi — **Chốt**
- **Vấn đề:** DR-21 đếm trong transaction nhưng hai transaction song song cùng đếm 2 rồi cùng chèn sẽ đạt 4 token.
- **Quyết định:** mở đầu T1 bằng `pg_advisory_xact_lock(hashtextextended(:email, 0))`. Giới hạn theo IP không khóa (giới hạn mềm, vượt tối đa vài đơn vị khi tải song song cao).
- **Hệ quả:** các yêu cầu cùng email xếp hàng, đúng ý; khác email không chặn nhau.
- **Nguồn:** `06-design/auth-and-sessions.md`

#### DR-98 · Cookie sliding — **Chốt**
- **Vấn đề:** DR-22 chốt hết hạn 30 ngày không hoạt động ở phía server nhưng không nói thuộc tính `Max-Age` của cookie.
- **Quyết định:** `Max-Age = 2592000` (30 ngày) khi tạo session và cấp lại cùng lúc `last_seen_at` được cập nhật (mỗi giờ nhiều nhất). Cookie không có `Expires` cố định theo ngày tạo.
- **Hệ quả:** cookie và DB hết hạn gần như cùng lúc; người dùng thường xuyên không bao giờ bị hỏi lại.
- **Nguồn:** `06-design/auth-and-sessions.md`

#### DR-99 · Giữ token magic link ngoài log và `Referer` — **Chốt**
- **Vấn đề:** token nằm trong query string của URL `GET /auth/callback`, dễ vào log truy cập, lịch sử trình duyệt và `Referer` của request tiếp theo.
- **Quyết định:** SPA xóa token khỏi URL bằng `history.replaceState` trước khi gọi verify; nginx đặt `Referrer-Policy: no-referrer` cho `/auth/callback` và ghi access log bằng `$uri`, không bằng `$request`.
- **Hệ quả:** token chỉ còn trong URL ở khoảng thời gian trang vừa tải; vẫn một lần dùng và hết hạn 15 phút.
- **Nguồn:** `06-design/auth-and-sessions.md`

#### DR-100 · IP của người xin link lấy từ `X-Real-IP` — **Chốt**
- **Vấn đề:** `login_token.requested_ip` (giới hạn 10 link/IP/giờ) cần IP thật của máy khách phía sau nginx.
- **Quyết định:** nginx đặt `proxy_set_header X-Real-IP $remote_addr` (ghi đè giá trị client gửi); `api` chỉ nghe trong mạng compose nên không có đường tới `api` mà không qua nginx; thiếu header thì dùng `remoteAddr`. Không tin `X-Forwarded-For`.
- **Hệ quả:** khi một NAT chung có nhiều người dùng, 10 link/giờ là chung cho cả nhóm (chấp nhận).
- **Nguồn:** `06-design/auth-and-sessions.md`

#### DR-101 · Tạo hoặc cập nhật người dùng bằng một upsert; locale của người đã có không bị ghi đè — **Chốt**
- **Vấn đề:** DR-21 nói "tạo `app_user` nếu chưa có" nhưng không nói đua giữa hai verify của hai email-mới cùng lúc (không thể, vì mỗi email chỉ một token hợp lệ) và locale của người đã có.
- **Quyết định:** `INSERT … ON CONFLICT (email) DO UPDATE SET last_login_at = now()`; `locale` chỉ lấy từ `login_token.locale` khi tạo mới. Người dùng đổi ngôn ngữ bằng `PATCH /me`.
- **Hệ quả:** đăng nhập từ trình duyệt đặt ngôn ngữ khác không đổi tùy chọn đã lưu của người dùng.
- **Nguồn:** `06-design/auth-and-sessions.md`

#### DR-102 · `AuthApi` cung cấp địa chỉ nhận email cho `payment` và `studio` — **Chốt**
- **Vấn đề:** `payment` và `studio` cần địa chỉ người nhận để dựng payload outbox nhưng không được đọc bảng của `auth` (DOC-12 §2.2).
- **Quyết định (Claude, Owner ủy quyền):** `auth` mở giao diện ở package gốc `io.ticket.auth` trả địa chỉ email theo `userId`; bên gọi dựng payload đủ dữ liệu rồi gọi `NotificationApi.enqueue` trong transaction của mình.
- **Hệ quả:** Giữ ranh giới module; mô tả ở DOC-19 §9 và DOC-27 §2.
- **Nguồn:** ``

#### DR-103 · Trùng mã vé không được thử lại ở câu `INSERT` — **Chốt**
- **Vấn đề:** DR-52 nói "sinh lại trong cùng transaction tối đa 3 lần" nhưng một `unique violation` làm hỏng cả transaction PostgreSQL, nên không thể bắt lỗi rồi thử tiếp trong chính transaction xác nhận.
- **Quyết định:** kiểm tra trước bằng `SELECT code … WHERE code = ANY(:codes)` và loại trùng trong lô, lặp tối đa 3 vòng; trùng sót lại sau kiểm tra thì transaction thất bại như lỗi tạm và lượt chạy lại sinh mã mới (webhook gửi lại hoặc `PaymentReconcileJob`).
- **Hệ quả:** thêm một câu `SELECT` theo index `UNIQUE` mỗi lần phát hành; xác suất thất bại tạm thời ~ `N × 2^-40` nhân cửa sổ đua vài mili giây.
- **Nguồn:** `06-design/tickets-and-notifications.md`

#### DR-104 · Ngân sách thời gian của một lượt `OutboxRelay` — **Chốt**
- **Vấn đề:** 50 dòng × SMTP timeout 5 giây vượt lease 60 giây, gây gửi trùng khi SMTP chậm (DR-53 không nói).
- **Quyết định:** khóa `outbox.relay.batch-budget` (40 giây); dòng chưa xử lý khi hết ngân sách được trả lại bằng `attempts - 1`, `next_attempt_at = now()`; kiểm tra khi khởi động `batch-budget + mail.smtp.timeout < lease`.
- **Hệ quả:** khi SMTP rất chậm, thông lượng giảm thay vì gửi trùng.
- **Nguồn:** `06-design/tickets-and-notifications.md`

#### DR-105 · Lỗi vĩnh viễn của SMTP không đi hết 12 lần thử — **Chốt**
- **Vấn đề:** DR-53 backoff áp dụng cho mọi lỗi; thư tới địa chỉ bị từ chối (550) sẽ chiếm hàng đợi 3 giờ 25 phút vô ích.
- **Quyết định:** phản hồi SMTP loại 5xx từ chối người nhận (550, 551, 553, 554) → `FAILED` ngay; 4xx và lỗi kết nối vẫn theo backoff.
- **Hệ quả:** địa chỉ sai lộ ra sớm trong log ERROR; thư có địa chỉ đúng nhưng bị lọc spam cũng `FAILED` ngay (đặt lại tay được).
- **Nguồn:** `06-design/tickets-and-notifications.md`

#### DR-106 · Fan-out theo lô JDBC 500 — **Chốt**
- **Vấn đề:** đổi lịch và hủy sự kiện ghi một dòng cho mỗi đơn `PAID` trong một transaction; một event 5.000 đơn không nên gọi `enqueue` 5.000 lần qua 5.000 round-trip.
- **Quyết định:** `NotificationApi.enqueueAll` dùng `JdbcClient.batchUpdate` theo lô `outbox.enqueue-batch-size` = 500, vẫn trong transaction của người gọi.
- **Hệ quả:** transaction `PATCH` và hủy sự kiện giữ lâu hơn một chút với event lớn (`statement_timeout` 60 giây của DR-28 đủ).
- **Nguồn:** `06-design/tickets-and-notifications.md`

#### DR-107 · Metric của vé và thông báo — **Chốt**
- **Vấn đề:** DR-09 chỉ có `ticket_outbox_pending` cho mảng này; thiếu số đo gửi, phát hành, va chạm mã.
- **Quyết định:** thêm `ticket_outbox_enqueued_total{kind}`, `ticket_mail_send_seconds{kind,result}`, `ticket_tickets_issued_total`, `ticket_ticket_code_collisions_total` (mục 11).
- **Hệ quả:** DOC-33 thêm bốn dòng; không alert (không alerting ở giai đoạn này).
- **Nguồn:** `06-design/tickets-and-notifications.md`

#### DR-108 · `ticket.label` của vé GA là `{}` — **Chốt**
- **Vấn đề:** DOC-14 chỉ mô tả `label` của ghế và khu vực, và DOC-51 (`DR-110`) cần biết GA ghi gì.
- **Quyết định:** GA ghi `{}`, không `null`, không khóa `ticketType`; loại vé nằm ở `ticket_type_name`.
- **Hệ quả:** mọi nơi đọc `label` kiểm tra bằng khóa có mặt, không bằng `label IS NULL`.
- **Nguồn:** `06-design/tickets-and-notifications.md`

#### DR-109 · Bố cục và header kỹ thuật của email — **Chốt**
- **Vấn đề:** canvas vẽ email bằng `div` flex, phông web và nửa tròn cuống vé định vị tuyệt đối; nhiều ứng dụng thư (Outlook, Gmail cũ) bỏ qua cả ba. DR-54 chưa nói `Message-ID` của email không qua outbox, và `Reply-To`.
- **Quyết định:** bố cục `<table role="presentation">` tối đa 600 px với CSS inline; chuỗi phông dự phòng, không nhúng web font; cuống vé dùng đường đứt thay nửa tròn; `Message-ID` của `magic-link` là `<login-{12 hex đầu của token_hash}@APP_DOMAIN>`; không đặt `Reply-To`.
- **Hệ quả:** hình dạng trong hộp thư khác canvas một chút (không nửa tròn, phông hệ thống); không email nào cho phép trả lời. Đổi bằng cách sửa template.
- **Nguồn:** `08-ux-ui/screens/emails.md`

#### DR-110 · Khối vé theo `ticket.label` — **Chốt**
- **Vấn đề:** `label` có ba dạng (ghế, khu vực, GA) mà canvas chỉ vẽ ghế.
- **Quyết định:** khóa `section`/`row`/`seat` → cột Hàng, Ghế, Loại vé; khóa `zone` → Khu vực, Loại vé; không khóa → Loại vé; mã vé luôn ở cột phải.
- **Hệ quả:** DOC-27 và DOC-85 phải giữ `label` của GA không chứa khóa nào ở trên (câu hỏi mở).
- **Nguồn:** `08-ux-ui/screens/emails.md`

#### DR-111 · Chọn biến thể `event-changed` — **Chốt**
- **Vấn đề:** DR-29 cho `endsAt` kích hoạt email nhưng canvas chỉ có ba biến thể theo giờ bắt đầu và địa điểm.
- **Quyết định:** `startsAt` hoặc `endsAt` đổi → nhóm "giờ"; `venue` đổi → nhóm "địa điểm"; chỉ `endsAt` đổi → hàng "Giờ kết thúc" thay cho hàng "Thời gian".
- **Hệ quả:** thêm một ca nhỏ vào test.
- **Nguồn:** `08-ux-ui/screens/emails.md`

#### DR-112 · `refundReason` ngoài ba giá trị — **Chốt**
- **Vấn đề:** mẫu `refund-pending` chọn đoạn lý do từ enum; giá trị lạ không có đoạn nào.
- **Quyết định:** ném lỗi khi dựng; dòng outbox thử lại theo DR-53 rồi `FAILED`, log ERROR; không gửi email chung chung.
- **Hệ quả:** lỗi lập trình hiện ra ở outbox thay vì gửi email sai nghĩa về tiền.
- **Nguồn:** `08-ux-ui/screens/emails.md`

#### DR-113 · Giới hạn gửi magic link không phải bất biến chính xác — **Chốt**
- **Vấn đề:** hai request đồng thời của cùng email đều đếm trước khi chèn.
- **Quyết định:** không khóa (không `pg_advisory_xact_lock`); chấp nhận tối đa vài link thừa trong cửa sổ 15 phút vì giới hạn là chống lạm dụng, không phải bất biến NEVER OVERSELL.
- **Hệ quả:** test `FLA-03` chạy tuần tự; không có test đồng thời cho giới hạn.
- **Nguồn:** `06-design/flows/auth-and-account.md`

#### DR-114 · `AuthCallback` bỏ token khỏi URL và gọi `verify` đúng một lần — **Chốt**
- **Vấn đề:** token nằm trong URL của lịch sử trình duyệt và `Referer`; React StrictMode chạy effect hai lần ở dev nên gọi `verify` hai lần, lần hai nhận 401.
- **Quyết định:** gọi `history.replaceState(null, "", "/auth/callback")` trước khi gọi API, giữ token trong biến cục bộ; bảo đảm một lời gọi bằng một `Promise` mức module theo token. Thêm `<meta name="referrer" content="no-referrer">` cho trang callback.
- **Hệ quả:** làm mới trang sau khi mất token trong URL cho màn "link không còn dùng được"; người dùng xin link mới.
- **Nguồn:** `06-design/flows/auth-and-account.md`

#### DR-115 · Đăng xuất idempotent; lý do vào `Login` đi qua state điều hướng; ghi đè locale sau đăng nhập — **Chốt**
- **Vấn đề:** (1) `POST /auth/logout` với cookie đã mất không nên 401 vì người dùng chỉ muốn thoát; (2) màn `Login` cần biết biến thể "Đã đăng xuất"/"Hết phiên" mà không thêm tham số URL có thể bị dán; (3) khách đổi sang `en` rồi đăng nhập vào tài khoản `vi` thì không rõ ai thắng.
- **Quyết định:** (1) `/auth/logout` luôn 204 và xóa cookie; chỉ kiểm CSRF khi session hợp lệ; (2) router `navigate("/login?returnTo=…", { state: { reason: "signed_out" | "session_expired" } })`; mở `/login` trực tiếp luôn hiện biểu mẫu thường; (3) sau `FL-02`, nếu cookie `tb_lang` khác `me.locale` thì cookie thắng và client gửi một `PATCH /me` (lựa chọn gần nhất của người dùng).
- **Hệ quả:** làm mới trang `/login` mất biến thể (chấp nhận); locale tài khoản có thể đổi vì người dùng đã chọn ở trình duyệt này.
- **Nguồn:** `06-design/flows/auth-and-account.md`

### Bảo mật và observability

#### DR-116 · Tài nguyên của người mua thuộc người khác trả 404 — **Chốt**
- **Quyết định:** ⚠ Tài nguyên của người mua (`reservation`, `orders`, `ticket`) thuộc người khác trả **404** `NOT_FOUND`, không phải 403 như SDD gốc 12.3. 403 `FORBIDDEN` chỉ cho vi phạm quyền không gắn tài nguyên cụ thể
- **Hệ quả / lý do:** DR-23 đã chọn 404 cho studio để không lộ tồn tại; hai quy tắc khác nhau cho cùng loại lỗi dễ gây IDOR do sai sót; ID đơn lộ qua URL chia sẻ
- **Nguồn:** `06-design/security.md`

#### DR-117 · `OriginCheckFilter` kiểm header `Origin` ở request ghi — **Chốt**
- **Quyết định:** Bộ lọc `OriginCheckFilter`: request ghi có header `Origin` mà khác `APP_BASE_URL` bị từ chối 403 `CSRF_TOKEN_INVALID`; không có `Origin` thì chỉ kiểm CSRF token
- **Hệ quả / lý do:** Lớp phòng thủ thứ hai cho CSRF, rẻ, không đụng k6/curl
- **Nguồn:** `06-design/security.md`

#### DR-118 · Spring Security `denyAll` mặc định và `SecurityMatrixTest` — **Chốt**
- **Quyết định:** Spring Security mặc định `denyAll`; mọi endpoint khai quyền tường minh và có dòng ở §2.3; `SecurityMatrixTest` đối chiếu OpenAPI với `security-matrix.csv`
- **Hệ quả / lý do:** Endpoint quên khai quyền thì đóng, không mở
- **Nguồn:** `06-design/security.md`

#### DR-119 · `Cache-Control: no-store` cho nhóm endpoint cá nhân — **Chốt**
- **Quyết định:** `Cache-Control: no-store` cho nhóm endpoint cá nhân (§8) đặt ở `api`, nginx tôn trọng và không `proxy_cache`
- **Hệ quả / lý do:** Tránh cache dữ liệu người dùng ở nginx hay trình duyệt dùng chung
- **Nguồn:** `06-design/security.md`

#### DR-120 · Metric bổ sung ngoài sáu metric của DR-09 — **Chốt**
- **Quyết định:** Bổ sung metric ngoài sáu metric của DR-09 (bảng §4.2), gồm `ticket_hold_bulkhead_in_use`, `ticket_expiry_lag_seconds`, `ticket_queue_waiting`, `ticket_queue_admit_total`, `ticket_ratelimit_total`, `ticket_payment_*`, `ticket_job_*`, `ticket_invariant_*`, `ticket_orders_refund_pending`; hai key lấy mẫu `ticket.metrics.sampler-interval`, `ticket.metrics.refund-pending-interval`; tên dashboard `ticket-exp05` với 12 panel
- **Hệ quả / lý do:** DR-09 chỉ liệt kê sáu metric đủ để vẽ p95 và connection; EXP-02/04/06/08 và NFR-03 cần trễ trả vé, nhịp vào cửa, kết quả webhook, số sai lệch
- **Nguồn:** `06-design/observability.md`

#### DR-121 · Label metric thuộc tập đóng, không chứa ID, email, IP — **Chốt**
- **Quyết định:** Label không bao giờ chứa ID, email, IP hoặc đường dẫn có tham số; mọi label lấy từ tập đóng ghi ở §4.2; test `OBS-12` đếm series
- **Hệ quả / lý do:** Giữ Prometheus nhẹ khi 100.000 người dùng ảo và không lộ dữ liệu cá nhân
- **Nguồn:** `06-design/observability.md`

#### DR-122 · Redis `DOWN` không làm `readiness` DOWN — **Chốt**
- **Quyết định:** Redis `DOWN` không làm `readiness` DOWN
- **Hệ quả / lý do:** DR-56: hệ thống tiếp tục phục vụ khi mất Redis; compose không được khởi động lại `api` vì Redis
- **Nguồn:** `06-design/observability.md`

### Vận hành, CI và môi trường dev

#### DR-123 · Profile `seed` và các target `make` bổ sung — **Chốt**
- **Quyết định:** Thêm Spring profile `seed` (chạy một lần, idempotent, gọi service thật) cho `make seed`; thêm target `make login`, `logs`, `psql`, `fmt`, `up-obs`, `build`, `e2e-stripe`, `contract`; `make reset` không tự seed
- **Hệ quả / lý do:** DR-01 chỉ liệt kê 10 target; seed qua service bảo đảm unit kho vé sinh đúng như xuất bản thật (DR-27)
- **Nguồn:** `09-operations/local-dev.md`

#### DR-124 · Ba file compose và giới hạn tài nguyên khởi điểm — **Chốt**
- **Quyết định:** Ba file compose (gốc, `.dev`, `.experiment`); giới hạn tài nguyên khởi điểm ở §4; `redis` `noeviction`
- **Hệ quả / lý do:** Cho phép mở cổng khi `make dev`, cố định tài nguyên thực nghiệm mà không sửa file gốc
- **Nguồn:** `09-operations/deploy-compose.md`

#### DR-125 · `PAYMENTS_MODE` phải khớp profile Spring — **Chốt**
- **Quyết định:** `PAYMENTS_MODE` (`fake`\|`stripe`) và profile `fake-payments`/`stripe` phải khớp; API từ chối khởi động nếu lệch
- **Hệ quả / lý do:** `docker compose up` thuần phải chạy được (NFR-08) nên `.env.example` đặt cả hai; kiểm tra lúc khởi động chặn cấu hình nửa vời
- **Nguồn:** `09-operations/local-dev.md`

#### DR-126 · Khóa công khai Stripe vào frontend bằng build arg — **Chốt**
- **Quyết định:** Khóa công khai Stripe vào frontend bằng build arg `VITE_STRIPE_PUBLISHABLE_KEY` (biến `STRIPE_PUBLISHABLE_KEY`)
- **Hệ quả / lý do:** DR-50 dùng Payment Element nhưng chưa nói khóa công khai tới trình duyệt bằng cách nào; build arg khớp `VITE_PAYMENTS` của DR-51
- **Nguồn:** `09-operations/deploy-compose.md`

#### DR-127 · Body 429 do nginx sinh, bỏ `Cookie` ở location cache, CSP cho Stripe Elements — **Chốt**
- **Quyết định:** Body 429 do nginx sinh đủ trường Problem Details; `nginx` bỏ `Cookie` ở location cache; CSP có `style-src 'unsafe-inline'` cho Stripe Elements
- **Hệ quả / lý do:** DR-55 chỉ nêu "Problem Details `RATE_LIMITED`"; cần chốt nội dung; CSP cần xác nhận với Payment Element ở P3-08
- **Nguồn:** `09-operations/deploy-compose.md`

#### DR-128 · E2E ở workflow `e2e.yml` riêng — **Chốt**
- **Quyết định:** E2E ở workflow `e2e.yml` riêng, bắt buộc với PR `dev → main`, không bắt buộc với PR vào `dev`
- **Hệ quả / lý do:** Hòa hợp DR-08 (E2E chạy tay) và DR-77 (E2E trong CI với `PAYMENTS_MODE=fake`)
- **Nguồn:** `09-operations/ci-cd.md`

#### DR-129 · Job `audit`, dependency locking, nhãn `breaking-ok`, kiểm tiêu đề PR — **Chốt**
- **Quyết định:** Job `audit` (OSV-Scanner, chạy hằng tuần) **không** bắt buộc; bật Gradle dependency locking; `oasdiff` có nhãn `breaking-ok`; tiêu đề PR kiểm bằng script
- **Hệ quả / lý do:** DOC-32 yêu cầu quét phụ thuộc; DR-08 chưa chọn công cụ; tránh chặn PR vì CVE ngoài phạm vi PR
- **Nguồn:** `09-operations/ci-cd.md`

#### DR-130 · Branch protection cho `dev` và `main` — **Chốt**
- **Quyết định:** Branch protection cho cả `dev` và `main` với danh sách check ở §10
- **Hệ quả / lý do:** DR-08 nói "chặn merge khi đỏ" nhưng chưa nêu tên check
- **Nguồn:** `09-operations/ci-cd.md`

### UX, design system và màn hình

#### DR-131 · Mục "Tạo hồ sơ tổ chức" trong menu tài khoản; `returnTo` chỉ nhận đường dẫn nội bộ — **Chốt**
- **Quyết định:** Thêm mục "Tạo hồ sơ tổ chức" vào menu tài khoản của người chưa có hồ sơ; `returnTo` chỉ nhận đường dẫn nội bộ một dấu `/`
- **Hệ quả / lý do:** DOC-38, DOC-44, DOC-53, DOC-19
- **Nguồn:** `08-ux-ui/ux-principles-and-ia.md`

#### DR-132 · Quy tắc tự thử lại phía client — **Chốt**
- **Quyết định:** Quy tắc tự thử lại: `clamp(Retry-After,1,32)` hoặc 8→16→32 giây; tối đa 10 lần liên tiếp rồi dừng và hiện nút thử lại
- **Hệ quả / lý do:** DOC-38, DOC-40, DOC-36
- **Nguồn:** `08-ux-ui/ux-principles-and-ia.md`

#### DR-133 · Tên và giá trị token mở rộng của canvas — **Chốt**
- **Quyết định:** Tên và giá trị token mở rộng của canvas (xem DOC-39 §2.4)
- **Hệ quả / lý do:** DOC-39
- **Nguồn:** `08-ux-ui/ux-principles-and-ia.md`

#### DR-134 · Lint chặn mã hex ngoài `tokens.css` — **Chốt**
- **Quyết định:** Lint chặn mã hex ngoài `tokens.css` (`stylelint`)
- **Hệ quả / lý do:** DOC-39, DOC-63
- **Nguồn:** `08-ux-ui/design-system.md`

#### DR-135 · Namespace i18n bổ sung và key lỗi `errors.<code>` — **Chốt**
- **Quyết định:** Thêm namespace i18n `auth`, `queue`, `orders`, `tickets`, `errors`, `validation` ngoài năm namespace của DR-10; key lỗi API là `errors.<code viết thường>`; chuỗi `problem.*` của backend sinh từ cùng JSON
- **Hệ quả / lý do:** DOC-31, DOC-40, DOC-35
- **Nguồn:** `08-ux-ui/ui-states-and-copy.md`

#### DR-136 · Hiển thị lỗi theo `code`, không theo `title`/`detail` của server — **Chốt**
- **Quyết định:** Hiển thị lỗi theo `code`, không theo `title`/`detail` của server (chỉ dự phòng); khi mã chưa ánh xạ dùng `errors.generic`
- **Hệ quả / lý do:** DOC-36, DOC-40
- **Nguồn:** `08-ux-ui/ui-states-and-copy.md`

#### DR-137 · Định dạng ngày giữ `dd.MM.yyyy · HH:mm` ở cả hai locale — **Chốt**
- **Quyết định:** Giữ cùng định dạng ngày `dd.MM.yyyy · HH:mm` ở cả `vi` và `en`, chỉ tên thứ đổi theo locale (khác `Intl` mặc định của `en`)
- **Hệ quả / lý do:** DOC-31, DOC-40
- **Nguồn:** `08-ux-ui/ui-states-and-copy.md`

#### DR-138 · Chi tiết màn Đăng nhập: `reason`, mở hộp thư, đồng bộ giữa các tab — **Chốt**
- **Vấn đề:** DOC-44 cần chốt các chi tiết mà DR-21/22/67 không nêu.
- **Quyết định (Claude, Owner ủy quyền):** Query `reason` nhận `signed_out` hoặc `session_expired`, giá trị khác bị bỏ; nút "Mở hộp thư" mở webmail theo miền email; tab đã gửi link gọi `GET /me` (tối đa một lần mỗi 5 giây) khi lấy lại tiêu điểm để đồng bộ với tab đăng nhập khác.
- **Hệ quả:** Chi tiết ở DOC-44.
- **Nguồn:** ``

#### DR-139 · Biến thể E2 (hết phiên) là trạng thái của `Login`; vị trí component trang lỗi — **Chốt**
- **Vấn đề:** Canvas vẽ E2 (hết phiên) như một trang riêng.
- **Quyết định (Claude, Owner ủy quyền):** E2 là biến thể của `Login` với `reason=session_expired`, không có route riêng; component trang lỗi (E1, E3, E4, E5) là lớp mỏng quanh `ErrorPage` dùng chung, đặt ở `frontend/src/app/errors/`.
- **Hệ quả:** `SessionExpired` bị bỏ khỏi danh sách component (DR-141).
- **Nguồn:** ``

#### DR-140 · Điều kiện hiện E3, con dấu khi lỗi mạng, tải lại một lần khi lỗi chunk — **Chốt**
- **Vấn đề:** DOC-52 cần điều kiện hiển thị còn thiếu.
- **Quyết định (Claude, Owner ủy quyền):** E3 hiện khi API trả 403 `FORBIDDEN` ở route `/studio/**` (không hiện cho `ORGANIZER_PROFILE_REQUIRED`, chuyển tới `/studio/profile`); con dấu hiện mã HTTP của response lỗi gần nhất; lỗi tải chunk tự `window.location.reload()` một lần (cờ `sessionStorage["tb.chunkReload"]`), lần hai trong phiên hiện E5 không hộp mã.
- **Hệ quả:** Chi tiết ở DOC-52.
- **Nguồn:** ``

#### DR-141 · Tên component màn hình do DOC-41 chốt, bỏ `SessionExpired`, thêm `SeatsRoute` — **Chốt**
- **Vấn đề:** DOC-82 §3 là bản tạm; DOC-44 và DOC-52 đã chốt E2 không phải trang riêng.
- **Quyết định:** theo bảng §3: bỏ `SessionExpired`, tách hai dòng `Login` và `AuthCallback`, thêm `SeatsRoute`. Người quyết định: Claude (Owner ủy quyền).
- **Hệ quả:** DOC-82 §2 (cột Màn hình của FL-03) và §3 sửa khi hợp nhất; các spec DOC-42…60 dùng tên ở §1.
- **Nguồn:** `08-ux-ui/screens/README.md`

#### DR-142 · Tên component màn hình ở DOC-82 §3 là bản tạm, DOC-41 chốt — **Chốt**
- **Vấn đề:** DOC-82 phải đặt tên component màn hình trước khi DOC-41 tồn tại.
- **Quyết định (Claude, Owner ủy quyền):** Tên ở DOC-82 §3 là bản tạm; khi lệch, DOC-41 thắng.
- **Hệ quả:** Đã áp dụng: DR-141 sửa DOC-82 theo DOC-41.
- **Nguồn:** ``

### Danh mục endpoint, cấu hình và kiểm thử

#### DR-143 · Khóa `saleStartsAt` sau giờ mở bán — **Chốt** (Claude, Owner ủy quyền) — **Chốt**
- *
- **Vấn đề:** * DR-37 khóa sơ đồ theo `now() >= sale_starts_at`. DR-25 và DR-30 không cấm dời `saleStartsAt` sang tương lai sau khi đã qua; làm vậy sẽ mở khóa sơ đồ và cho dựng lại kho vé sau khi đã có đơn.
- *
- **Quyết định:** * Khi `now() >= saleStartsAt` lưu, `PATCH` không được đổi `saleStartsAtLocal` (422 `locked_after_sale_start`). Trước giờ mở bán đổi tự do.
- *
- **Hệ quả:** * Giờ mở bán không đảo ngược được sau khi mở. Ghi vào DOC-20, DOC-55, DOC-35 (`rule` mới), DOC-40 (`validation.locked_after_sale_start`).
- **Nguồn:** `07-api/api-endpoints.md`

#### DR-144 · Loại vé chỉ thêm khi sự kiện còn nháp — **Chốt** (Claude, Owner ủy quyền) — **Chốt**
- *
- **Vấn đề:** * DR-26 và DR-30 nói đổi giá, sức chứa, xóa loại vé khi đang bán, nhưng không nói thêm loại vé mới sau xuất bản; loại vé `SEAT`/`ZONE` mới cần sửa sơ đồ (khóa từ giờ mở bán), loại GA mới cần dựng pool.
- *
- **Quyết định:** * `POST …/ticket-types` chỉ khi `DRAFT`; sau đó 409 `EVENT_STATE_CONFLICT`. Sau xuất bản chỉ sửa (E-35) và xóa (E-36).
- *
- **Hệ quả:** * Muốn thêm hạng vé khi đang bán phải tạo sự kiện mới. Ghi vào DOC-20, DOC-56.
- **Nguồn:** `07-api/api-endpoints.md`

#### DR-145 · `PUT draft` chỉ kiểm tra schema, không validate nghiệp vụ — **Chốt** (Claude, Owner ủy quyền) — **Chốt**
- *
- **Vấn đề:** * DR-35 validate ở client liên tục và server khi xuất bản; DR-36 tự lưu mỗi 2 giây. Nếu `PUT draft` từ chối bản có lỗi nghiệp vụ, người dùng không lưu được bản dở.
- *
- **Quyết định:** * E-44 kiểm tra JSON Schema và giới hạn cứng (5 MB, 20.000 ghế, 1.000 hàng, 200 zone, 200 trang trí); mọi mã DR-35 chỉ chạy ở E-45 và E-46.
- *
- **Hệ quả:** * Bản nháp có thể chứa ghế chồng nhau; `publish-checks` (E-28) và E-46 chặn xuất bản. Ghi vào DOC-21, DOC-22.
- **Nguồn:** `07-api/api-endpoints.md`

#### DR-146 · Một quy ước đặt tên và định dạng cấu hình — **Chốt**
- **Vấn đề:** các tài liệu trộn `15s`/`PT15S`, biến môi trường tự đặt và biến Spring lỏng lẻo.
- **Quyết định:** duration luôn ISO-8601; khóa tự định nghĩa dùng `kebab-case`, biến môi trường chỉ có khi đã ghi ở bảng (§1); mỗi nhóm khóa một `@ConfigurationProperties` có `@Validated`.
- **Hệ quả:** DOC-33 đổi `15s` thành `PT15S`; dễ kiểm tự động (CFG-01).
- **Nguồn:** `06-design/configuration-reference.md`

#### DR-147 · Kiểm tra cấu hình lúc khởi động (§6) — **Chốt**
- **Vấn đề:** nhiều ràng buộc giữa các khóa (bulkhead < pool, lease > ngân sách gửi) chỉ nằm trong văn bản.
- **Quyết định:** một bean `ConfigurationGuard` kiểm tra mười điều kiện ở §6 và thoát khi sai.
- **Hệ quả:** lỗi cấu hình hiện ở lúc khởi động thay vì ở EXP.
- **Nguồn:** `06-design/configuration-reference.md`

#### DR-148 · Khóa mới do tài liệu này đặt — **Chốt**
- **Vấn đề:** DR-56, 57, 59, 62, 74 nêu con số nhưng không đặt tên khóa.
- **Quyết định:** đặt `ratelimit.*`, `admission.ticker-interval|lock-ttl|evict-limit|shuffle-batch-size`, `availability.cache-ttl`, `sales.cache-ttl`, `retention.*`, `event.lifecycle.interval`, `invariant.*`, `payment.reconcile.*`, `payment.stripe.connect-timeout|read-timeout|max-network-retries|webhook-tolerance`, `media.*`, `storage.s3.path-style` với đúng giá trị mặc định của DR gốc.
- **Hệ quả:** EXP có thể đổi nhịp mà không sửa code; DOC-24, 26, 28, 29, 30, 18 dùng đúng các tên này.
- **Nguồn:** `06-design/configuration-reference.md`

#### DR-149 · Mã mức test và ngoại lệ tiền tố dùng chung — **Chốt**
- (**Chốt**)
- **Nguồn:** `10-testing/test-strategy.md`

#### DR-150 · ID test nằm trong `@DisplayName`, có script kiểm tra hai chiều — **Chốt**
- (**Chốt**)
- **Nguồn:** `10-testing/test-strategy.md`

#### DR-151 · Ngưỡng nhánh 75% cho năm module lõi — **Chốt**
- (**Chốt**)
- **Nguồn:** `10-testing/test-strategy.md`

---

## Tổng hợp theo mức ảnh hưởng

Mỗi DR được xếp vào phase đầu tiên mà nó chặn. Đây là đầu vào của task P0-01 trong master plan.

| Mức | Mục | Vì sao cần chốt sớm |
| --- | --- | --- |
| Chặn P1 | DR-01–12, DR-14, DR-19, DR-21–23, DR-53, DR-54, DR-63, DR-64, DR-67, DR-68, DR-72, DR-77 | Cố định bố cục repo, phiên bản, cách truy cập dữ liệu, khóa chính, bảng xác thực và outbox, quy ước API và lỗi, i18n, design system, compose |
| Chặn P2 | DR-13, DR-15–18, DR-20, DR-24–28, DR-30, DR-38, DR-40–43, DR-45, DR-52, DR-61, DR-65, DR-66, DR-70, DR-73–76 | Schema kho vé và reservation, câu claim, job trả vé, idempotency, xuất bản và hủy sự kiện, kiểm tra bất biến, mô hình tải của EXP-02/03/04/10 |
| Chặn P3 | DR-29, DR-44, DR-47–51 | Tranh chấp tạo PaymentIntent với job trả vé, webhook, thanh toán đến trễ, cổng thanh toán giả cho EXP-06/07 |
| Chặn P4 | DR-31–36, DR-39 | Schema tài liệu sơ đồ, thuật toán hình học, validate dùng chung, tự lưu, kiến trúc editor |
| Chặn P5 | DR-37, DR-62, DR-69, DR-71 | Khóa sơ đồ từ giờ mở bán và dựng lại kho vé trước đó, định dạng tình trạng chỗ, chọn ghế, số liệu bán vé |
| Chặn P6 | DR-46, DR-55–60 | Tham số rate limit, phòng chờ, kiểm tra lượt vào, nhịp vào |
| Phát sinh khi viết gate P1 | DR-82–151 | Tóm tắt ở mục N; chi tiết ở tài liệu nguồn của từng mục |
| Chặn P7 | DR-78 | Dữ liệu và kịch bản demo |

Mục lệch SDD gốc (⚠): DR-02, DR-08, DR-09, DR-10, DR-17, DR-20, DR-21, DR-26, DR-28, DR-31, DR-37, DR-41, DR-43, DR-44, DR-53, DR-56, DR-57, DR-58, DR-60, DR-73.
Mục cần spike (🔬): DR-02 (S-01), DR-13 (S-02), DR-27 (S-03), DR-39 (S-04), DR-35 (S-05), DR-75 (S-06).

Mọi DR đã ở trạng thái Chốt hoặc Đổi (2026-10-07; DR-123–130 do Owner chốt theo đề xuất). Spike vẫn có thể lật lại mục tương ứng: kết quả xấu được ghi thành dòng mới trong nhật ký chốt và DR được đổi.
