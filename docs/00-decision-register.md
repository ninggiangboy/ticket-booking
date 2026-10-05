# Sổ quyết định mở

> Trạng thái: **Review** · Cập nhật: 2026-10-05 · Nguồn: phân tích `event-ticket-booking-sdd.md` (**SDD gốc**) và canvas thiết kế màn hình "Ticket — Design system & luồng mua vé" (50 artboard)

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

---

## A. Quy trình và nền tảng

### DR-01 · Bố cục repo, công cụ build và lệnh chuẩn
- **Vấn đề:** Phụ lục SDD gốc đặt mọi thứ dưới thư mục `event-ticketing/`, nhưng repo hiện tại là `ticket-booking/` và đã có SDD ở gốc. SDD nói "Gradle" mà không nói Kotlin hay Groovy DSL, không nói trình quản lý gói frontend, không có lệnh chuẩn để chạy test hay dựng môi trường.
- **Quyết định (đề xuất):**
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

### DR-02 · Phiên bản Java và Spring Boot — ⚠ lệch SDD gốc · 🔬 spike
- **Vấn đề:** SDD gốc mục 4.3 chọn Java 21 và Spring Boot 3. Tại 2026-10, Java 25 là bản LTS mới nhất và Spring Boot 4.x là dòng được hỗ trợ; Spring Boot 3.5 đã hết hỗ trợ OSS. Bắt đầu một dự án mới trên dòng đã hết hỗ trợ buộc phải nâng cấp lớn giữa chừng.
- **Các phương án:** (1) Java 21 + Boot 3.5: đúng SDD, thư viện chắc chắn tương thích, nhưng đã hết hỗ trợ OSS; (2) Java 25 + Boot 4.0.x: được hỗ trợ dài, virtual thread và structured logging sẵn, nhưng cần kiểm tra tương thích springdoc, stripe-java, Testcontainers, Spring Modulith.
- **Quyết định (đề xuất):** Phương án 2: Java 25 (Temurin), Spring Boot 4.0.x bản patch mới nhất tại lúc P1-01, khóa trong version catalog. Spike **S-01** dựng một ứng dụng rỗng với Spring Web, Security, JDBC, Flyway (PostgreSQL 18), springdoc-openapi, stripe-java, Spring Modulith, Testcontainers và chạy một test tích hợp. Spike thất bại ở thư viện nào thì lùi về phương án 1 cho toàn bộ dự án, không trộn.
- **Hệ quả:** Không dùng virtual thread cho đường giữ vé ở P2 (số luồng phải nhỏ hơn connection pool, DR-61); để mặc định platform thread, đo lại ở EXP-05.
- **Ghi vào:** DOC-11, ADR-0011.

### DR-03 · Stack và phiên bản frontend
- **Vấn đề:** SDD gốc mục 4.3 nêu React, TypeScript, Vite, TanStack Query, React Router, Konva, rbush nhưng không có phiên bản, không có thư viện form, state của editor, styling, cách sinh client từ OpenAPI, i18n.
- **Quyết định (đề xuất):**

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

### DR-04 · Phiên bản các image hạ tầng
- **Vấn đề:** SDD gốc mục 14.1 liệt kê container mà không có phiên bản.
- **Quyết định (đề xuất):** Khóa tag theo minor, cập nhật có chủ đích:

  | Container | Image | Ghi chú |
  | --- | --- | --- |
  | `postgres` | `postgres:18-alpine` | Có hàm `uuidv7()` sẵn (DR-11) |
  | `redis` | `redis:8.2-alpine` | Giấy phép AGPLv3/RSALv2/SSPL; dùng nội bộ, không sửa mã nguồn nên không ảnh hưởng. Valkey 8 thay thế được không đổi code nếu cần |
  | `nginx` | `nginx:1.28-alpine` | |
  | `mailpit` | `axllent/mailpit:v1.27` | |
  | `stripe-cli` | `stripe/stripe-cli:v1.30` | |
  | `api` | build từ `eclipse-temurin:25-jre-alpine` | |
- **Ghi vào:** DOC-11, DOC-62.

### DR-05 · Truy cập dữ liệu và kiểm tra ranh giới module
- **Vấn đề:** SDD gốc mục 4.3 chọn Spring JDBC; mục 4.1 yêu cầu module "chỉ gọi nhau qua interface công khai, không truy vấn bảng của nhau" nhưng không nói ép bằng gì. Transaction giữ vé lại phải chạm `idempotency_key`, `inventory_unit`, `reservation`, `orders` của bốn module trong một transaction.
- **Quyết định (đề xuất):**
  - `JdbcClient` của Spring cho mọi truy vấn; SQL viết tay trong repository của từng module; không JPA, không Spring Data.
  - Transaction mở ở application service của module điều phối (`reservation.HoldService`), các module khác tham gia qua interface công khai chạy trong cùng transaction (propagation `MANDATORY` cho các method chỉ được gọi trong transaction).
  - Ranh giới module kiểm tra bằng Spring Modulith (`ApplicationModules.of(Application.class).verify()` trong một unit test) cộng ArchUnit cho luật "chỉ repository của module X được nhắc tên bảng của X" (quét chuỗi SQL theo danh sách bảng sở hữu ở DOC-07).
- **Hệ quả:** Không có lazy loading hay N+1 ẩn; mỗi truy vấn đọc được trong code review. Cần bảng sở hữu dữ liệu (bảng nào thuộc module nào) trong DOC-07.
- **Ghi vào:** ADR-0012, DOC-12, DOC-07.

### DR-06 · Cấu trúc package trong mỗi module và luật phụ thuộc
- **Vấn đề:** SDD gốc liệt kê module nhưng không nói bên trong mỗi module chia thế nào.
- **Quyết định (đề xuất):** Gói gốc `io.ticket`. Mỗi module `io.ticket.<module>`:

  | Package | Nội dung | Ai được dùng |
  | --- | --- | --- |
  | `<module>` (gốc) | Interface công khai (`InventoryApi`), DTO dạng `record`, event nội bộ | Mọi module |
  | `<module>.web` | Controller, request/response DTO, mapper | Chỉ module đó |
  | `<module>.internal` | Service, repository, entity dạng `record`, job | Chỉ module đó |

  Luật: không module nào import `*.internal` hay `*.web` của module khác; `common` không phụ thuộc module nghiệp vụ; controller không gọi repository trực tiếp. Module bổ sung so với SDD gốc: `media` (DR-38), `i18n` không phải module (chỉ là cấu hình trong `common`).
- **Ghi vào:** DOC-12, ADR-0002.

### DR-07 · Quy trình Git, commit và ngôn ngữ
- **Vấn đề:** SDD gốc không nói nhánh, quy ước commit, ngôn ngữ code.
- **Quyết định (đề xuất):** `main` luôn chạy được; làm việc trên `dev` và nhánh `feat/<task-id>-<slug>`, merge vào `dev` qua PR, `dev` → `main` ở mỗi milestone. Conventional Commits tiếng Anh, scope là module (`feat(inventory): claim pool units with skip locked`), footer `Refs: P2-03`. Code, log, mã lỗi, tên bảng, commit, tiêu đề PR bằng tiếng Anh; chuỗi giao diện qua i18n (DR-10); tài liệu `docs/` bằng tiếng Việt. Migration Flyway đặt tên `V<yyyymmddHHmm>__<snake_case>.sql`.
- **Ghi vào:** master plan §7.3, DOC-63.

### DR-08 · CI tối thiểu ngay từ P1 — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 14.1 để CI/CD sang giai đoạn sau. Nhưng chứng minh "không bán vượt" dựa vào test đồng thời và test tích hợp chạy lặp lại; không có CI thì hồi quy không bị phát hiện.
- **Quyết định (đề xuất):** GitHub Actions, một workflow `ci.yml` chạy trên PR và push `dev`/`main`: (1) `make lint`; (2) `make test`; (3) `make it` (Testcontainers, runner `ubuntu-latest` đủ chạy PostgreSQL + Redis); (4) kiểm tra `schema.d.ts` khớp OpenAPI; (5) build image `api` và bundle frontend. Chặn merge khi đỏ. Không có CD; E2E và thực nghiệm tải chạy tay.
- **Ghi vào:** DOC-63.

### DR-09 · Log và metric tối thiểu cho thực nghiệm — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc để observability sang sau (mục 2.3) nhưng EXP-05 cần p95, số connection đang dùng, thời gian giữ connection; mục 12.3 yêu cầu `X-Request-Id` trùng `trace_id` trong log.
- **Quyết định (đề xuất):**
  - Log JSON bằng structured logging có sẵn của Spring Boot (format `ecs`), trường bắt buộc: `@timestamp`, `log.level`, `message`, `trace_id`, `user_id` (nếu có), `event_id`, `reservation_id`, `order_id`. Email không bao giờ vào log (DR-22).
  - Actuator mở `health`, `prometheus` trên cổng quản trị 9090 (không qua nginx). Metric Hikari, HTTP server và metric riêng: `ticket_hold_duration_seconds` (histogram), `ticket_hold_result_total{result}`, `ticket_connection_hold_seconds`, `ticket_expiry_batch_size`, `ticket_outbox_pending`, `ticket_queue_admitted`.
  - Compose profile `obs` thêm Prometheus + Grafana với một dashboard dựng sẵn; không bật mặc định. Không alerting.
- **Hệ quả:** Thêm hai container chỉ khi chạy thực nghiệm; NFR-08 (`docker compose up`) không đổi.
- **Ghi vào:** DOC-33, DOC-62.

### DR-10 · Giao diện đa ngôn ngữ — ⚠ lệch SDD gốc
- **Vấn đề:** Owner chọn giao diện đa ngôn ngữ; SDD gốc không có i18n và canvas thiết kế chỉ có chuỗi tiếng Việt. Email (magic link, vé, đổi lịch) cũng là giao diện.
- **Quyết định (đề xuất):**
  - Hai locale ở giai đoạn này: `en` (mặc định và là ngôn ngữ của key) và `vi` (dịch từ canvas). Thêm locale là thêm file, không đổi code.
  - Frontend: i18next, namespace theo feature (`events`, `checkout`, `studio`, `editor`, `common`), file `frontend/src/locales/<lng>/<ns>.json`, key dạng `checkout.hold.expired.title`. Chọn ngôn ngữ: `app_user.locale` nếu đã đăng nhập → cookie `tb_lang` → `Accept-Language` → `en`. Bộ chọn ngôn ngữ ở header.
  - Backend: `MessageSource` với `messages_en.properties`, `messages_vi.properties` cho email và cho trường `title`/`detail` của Problem Details (mã `code` luôn tiếng Anh). Email gửi theo `app_user.locale` lúc ghi outbox.
  - Định dạng số, tiền, ngày qua `Intl` theo locale; tiền VND luôn hiện không có phần lẻ (`1.800.000 ₫` ở `vi`, `₫1,800,000` ở `en`). Giờ sự kiện hiện theo múi giờ của sự kiện (DR-12), không theo máy người xem.
  - Tài liệu sơ đồ và tên do người tổ chức nhập (tên sự kiện, loại vé, khu vực) không dịch.
- **Hệ quả:** Mọi microcopy trong DOC-40 có cột `en` và `vi`; màn hình spec trích key, không trích chuỗi cứng. CI kiểm tra mọi key `en` có bản `vi` (script `pnpm i18n:check`).
- **Ghi vào:** DOC-31, ADR-0016, DOC-40, DOC-27.

---

## B. Quy ước dữ liệu

### DR-11 · Khóa chính và định danh công khai
- **Vấn đề:** SDD gốc dùng `id`, `reservation_id`, `unit_id` mà không nói kiểu. ID xuất hiện trong URL (`/orders/{id}`) nên không được đoán được; ID dạng tăng dần làm lộ số đơn.
- **Các phương án:** (1) `bigint identity` + ID công khai riêng: hai cột cho mỗi bảng; (2) UUIDv4: ngẫu nhiên, index B-tree phân mảnh khi chèn nhiều (`inventory_unit` 100.000 dòng mỗi lần xuất bản); (3) UUIDv7: có thứ tự thời gian, chèn vào cuối index, không đoán được phần ngẫu nhiên.
- **Quyết định (đề xuất):** Phương án 3. Mọi bảng nghiệp vụ có khóa chính `uuid` tên `<bảng>_id`, mặc định `uuidv7()` của PostgreSQL 18; code Java sinh trước UUIDv7 khi cần biết ID trước khi chèn (`reservation_id` được sinh trước khi claim unit, DR-41). Ngoại lệ: ID ghế, khu vực, hàng trong tài liệu sơ đồ do client sinh (`crypto.randomUUID()`, UUIDv4 cho ghế; chuỗi `zone-<8 ký tự>`/`row-<8 ký tự>` cho đối tượng khác, DR-32). ID của Stripe (`pi_…`, `evt_…`) lưu kiểu `text`.
- **Ghi vào:** DOC-14, ADR-0018.

### DR-12 · Thời gian và múi giờ
- **Vấn đề:** SDD gốc mục 12.3 nói thời gian là ISO 8601 theo UTC và mục 17 nói mọi so sánh dùng `now()` của database, nhưng không nói sự kiện hiển thị theo múi giờ nào. Canvas hiện "Thứ Bảy 14.11.2026 · 20:00" không kèm múi giờ; người tổ chức nhập giờ địa phương ở form `datetime-local`.
- **Quyết định (đề xuất):**
  - Mọi cột thời điểm là `timestamptz`; API trả chuỗi UTC dạng `2026-11-14T13:00:00Z`.
  - Mỗi event có cột `timezone` (tên IANA), mặc định cấu hình `PLATFORM_TIMEZONE=Asia/Ho_Chi_Minh`; giai đoạn này form không cho đổi (một quốc gia), cột có sẵn để mở rộng.
  - Form studio gửi giờ địa phương kèm `timezone` của event; server chuyển sang UTC. Giao diện hiển thị theo `timezone` của event bằng `Intl.DateTimeFormat(locale, { timeZone })`, không theo máy người xem.
  - So sánh hạn (giữ vé, token, khung mở bán) luôn trong SQL bằng `now()`; Java không gọi `Instant.now()` cho các quyết định đó.
- **Ghi vào:** DOC-14, DOC-36, DOC-31.

### DR-13 · Tiền, tiền tệ và giới hạn của Stripe — 🔬 spike
- **Vấn đề:** SDD gốc mục 6.2 chọn một tiền tệ cấu hình `PLATFORM_CURRENCY` (mặc định VND), lưu số nguyên đơn vị nhỏ nhất; mục 17 để ngỏ quốc gia của tài khoản Stripe. VND là tiền tệ không có phần lẻ ở Stripe (số tiền gửi lên là số đồng), Stripe có mức thu tối thiểu theo tiền tệ thanh toán, và tài khoản Stripe không mở được ở Việt Nam nên phải dùng tài khoản nước khác nhận tiền VND (presentment currency).
- **Quyết định (đề xuất):**
  - Cột tiền là `bigint` theo đơn vị nhỏ nhất của `PLATFORM_CURRENCY`; với VND, 1 đơn vị = 1 đồng. Bảng hệ số `minorUnitDigits` (VND 0, USD 2) nằm trong module `common`, dùng khi gọi Stripe và khi định dạng.
  - Giá loại vé: `0` (miễn phí) hoặc trong khoảng `[payment.min-amount, payment.max-amount]`, mặc định VND `[20000, 100000000]`. Tổng đơn khác 0 luôn ≥ `payment.min-amount` vì mỗi vé có giá đã ≥ mức đó.
  - Đổi `PLATFORM_CURRENCY` sau khi đã có đơn là không hợp lệ: khi khởi động, nếu `orders` có `currency` khác cấu hình thì API dừng với lỗi rõ ràng.
  - Spike **S-02** trên Stripe test mode: tạo PaymentIntent VND chỉ thẻ, đo mức tối thiểu thật, thử hủy ở các trạng thái `requires_payment_method`, `requires_action`, `processing`, `succeeded`, ghi lại mã lỗi trả về; xác nhận stripe-cli forward webhook trong compose. Kết quả chỉnh `payment.min-amount` và DR-47.
- **Ghi vào:** DOC-14, DOC-26, DOC-34.

---

## C. Schema cơ sở dữ liệu

SDD gốc mục 11 cố ý chỉ nêu mô hình khái niệm. DDL dưới đây là đề xuất để chốt trước P1/P2; DOC-14 và DOC-15 chép lại đầy đủ kèm comment. Quy ước chung: trạng thái là `text` + `CHECK` (đổi giá trị bằng migration đơn giản hơn `ENUM`), mọi bảng có `created_at timestamptz NOT NULL DEFAULT now()`, khóa ngoại có index.

### DR-14 · DDL tài khoản, tổ chức, token đăng nhập, session
- **Vấn đề:** SDD gốc mục 5 mô tả hành vi nhưng không có cột; canvas thêm "Email liên hệ" của tổ chức (không bắt buộc) và ngôn ngữ người dùng (DR-10).
- **Quyết định (đề xuất):**

  ```sql
  CREATE TABLE app_user (
    user_id       uuid PRIMARY KEY DEFAULT uuidv7(),
    email         text NOT NULL,                       -- đã chuẩn hóa, DR-21
    locale        text NOT NULL DEFAULT 'en' CHECK (locale IN ('en','vi')),
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

### DR-15 · DDL sự kiện và loại vé
- **Vấn đề:** SDD gốc mục 6 nêu thuộc tính và trạng thái; canvas thêm mô tả, ảnh, cờ "Bật phòng chờ khi mở bán", tiền tố mã vé (`GM-…`), màu loại vé theo thứ tự tạo. Bản nháp phải lưu được khi chưa đủ trường (nút "Lưu bản nháp" ở Studio 02).
- **Quyết định (đề xuất):**

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
    max_tickets_per_order smallint NOT NULL DEFAULT 8 CHECK (max_tickets_per_order BETWEEN 1 AND 10),
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
- **Hệ quả:** Ràng buộc `event_schedule_ck` ép quy tắc "đóng bán không muộn hơn giờ bắt đầu" có trên canvas (Studio 02) nhưng không có trong SDD gốc (DR-25).
- **Ghi vào:** DOC-14.

### DR-16 · DDL sơ đồ và phiên bản sơ đồ
- **Vấn đề:** SDD gốc mục 7.8 và 11.1 nêu bản nháp kèm `revision` và phiên bản bất biến kèm checksum, không có cột; "bất biến" không nói được ép thế nào.
- **Quyết định (đề xuất):**

  ```sql
  CREATE TABLE seat_map (
    seat_map_id       uuid PRIMARY KEY DEFAULT uuidv7(),
    event_id          uuid NOT NULL UNIQUE REFERENCES event,    -- một sơ đồ cho mỗi event, DR-31
    organizer_id      uuid NOT NULL REFERENCES organizer,
    name              text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 120),
    draft             jsonb NOT NULL,
    draft_revision    int  NOT NULL DEFAULT 0,
    draft_updated_at  timestamptz NOT NULL DEFAULT now(),
    latest_version_no int  NOT NULL DEFAULT 0,
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

### DR-17 · DDL kho vé, thêm trạng thái `REMOVED` cho unit — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 7.8 và 14.3 dùng trạng thái `REMOVED` của unit, nhưng vòng đời unit ở mục 8.1 chỉ có `AVAILABLE`, `HELD`, `SOLD`. Unit ghế cần nhãn (Section, hàng, số) để in lên vé và email, nhưng SDD gốc không có bảng ghế. Trình xem sơ đồ cần ánh xạ ghế sang vị trí trong bitmap tình trạng (DR-62).
- **Quyết định (đề xuất):** Vòng đời unit có thêm `REMOVED`: `AVAILABLE → REMOVED` khi xóa ghế/khu vực hoặc giảm sức chứa; `REMOVED → AVAILABLE` khi ghế cùng UUID được thêm lại trong phiên bản sau. Unit ghế mang nhãn chụp từ phiên bản sơ đồ (DR-40).

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

### DR-18 · DDL reservation, đơn hàng và vé
- **Vấn đề:** SDD gốc mục 8.3, 9.2, 9.4, 11 nêu trạng thái và ràng buộc, không có cột; lý do đóng reservation (hết hạn, người mua hủy, sự kiện bị hủy) cần cho thông báo và kiểm tra bất biến.
- **Quyết định (đề xuất):**

  ```sql
  CREATE TABLE reservation (
    reservation_id uuid PRIMARY KEY,                   -- sinh trước ở Java, DR-11
    event_id       uuid NOT NULL REFERENCES event,
    user_id        uuid NOT NULL REFERENCES app_user,
    status         text NOT NULL CHECK (status IN ('ACTIVE','EXPIRING','CONFIRMED','EXPIRED','CANCELLED')),
    expires_at     timestamptz NOT NULL,
    close_reason   text CHECK (close_reason IN ('TIMEOUT','BUYER_CANCELLED','EVENT_CANCELLED')),
    expiring_since timestamptz,
    confirmed_late boolean NOT NULL DEFAULT false,     -- xác nhận qua luồng thanh toán đến trễ, DR-44
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
    status             text NOT NULL CHECK (status IN ('PENDING_PAYMENT','PAID','EXPIRED','CANCELLED','NEEDS_REVIEW')),
    amount             bigint NOT NULL CHECK (amount >= 0),
    currency           text NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    payment_intent_id  text UNIQUE,
    last_payment_error text,                           -- mã decline_code gần nhất
    paid_at            timestamptz,
    review_reason      text,                           -- LATE_PAYMENT_NO_INVENTORY | EVENT_CANCELLED | AMOUNT_MISMATCH, DR-44
    refund_reference   text,                           -- mã hoàn tiền Stripe ghi tay, DR-44
    review_resolved_at timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now(),
    updated_at         timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT orders_paid_ck CHECK ((status = 'PAID') = (paid_at IS NOT NULL))
  );
  CREATE INDEX orders_user_idx       ON orders (user_id, created_at DESC);
  CREATE INDEX orders_event_paid_idx ON orders (event_id) WHERE status = 'PAID';
  CREATE INDEX orders_pending_idx    ON orders (created_at) WHERE status = 'PENDING_PAYMENT';

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

### DR-19 · DDL idempotency, webhook đã xử lý, outbox
- **Vấn đề:** SDD gốc mục 8.6, 9.3, 4.2 nêu ba bảng không có cột; cách "request thứ hai bị chặn ở unique index rồi nhận lại response" cần dòng key được chèn đầu transaction và điền response cuối transaction.
- **Quyết định (đề xuất):**

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
    outcome           text NOT NULL,                   -- CONFIRMED | LATE_CONFIRMED | NEEDS_REVIEW | FAILURE_RECORDED | IGNORED
    received_at       timestamptz NOT NULL DEFAULT now()
  );

  CREATE TABLE outbox (
    outbox_id       uuid PRIMARY KEY DEFAULT uuidv7(),
    kind            text NOT NULL CHECK (kind IN ('EMAIL_MAGIC_LINK','EMAIL_TICKETS','EMAIL_EVENT_CHANGED','EMAIL_PAYMENT_REVIEW')),
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

### DR-20 · Giá chụp lại nằm ở đâu — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 6.2 nói giá và tên loại vé "được chụp lại vào dòng đơn hàng", còn mục 11.1 nói `reservation_item` lưu "giá đã chụp" và không có bảng dòng đơn hàng. Hai chỗ mâu thuẫn; thêm `order_item` thì có hai bản chụp phải giữ khớp.
- **Quyết định (đề xuất):** Bản chụp duy nhất nằm ở `reservation_item` (tên loại vé, giá đơn vị, nhãn vị trí). `orders.amount` = Σ `quantity × unit_price` của các item, tính trong transaction giữ vé và không đổi sau đó. Không có bảng `order_item`. Vé chép lại tên, giá, nhãn từ item lúc phát hành để vé tự đứng được khi in.
- **Hệ quả:** Đổi giá loại vé sau khi mở bán chỉ ảnh hưởng reservation tạo sau (SDD gốc 6.3) mà không cần cơ chế riêng.
- **Ghi vào:** DOC-14, DOC-24.

---

## D. Xác thực và phân quyền

### DR-21 · Chi tiết magic link — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 5 chốt luồng, thời hạn, giới hạn gửi nhưng để ngỏ: chuẩn hóa email; cách vô hiệu link cũ; `return_to` lưu ở đâu khi người dùng mở link trên thiết bị khác; giới hạn gửi đếm ở đâu; và **email magic link đi qua outbox** (SDD gốc 4.1) **trong khi token chỉ được lưu dạng hash** (NFR-07): job outbox cần token thô để dựng link.
- **Các phương án cho mâu thuẫn outbox:** (1) gửi email trực tiếp sau commit, không qua outbox: SMTP lỗi thì người dùng phải bấm "Gửi lại"; (2) outbox giữ token thô dạng mã hóa AES-256-GCM bằng khóa `OUTBOX_ENCRYPTION_KEY`, xóa trường đó ngay khi gửi xong; (3) outbox giữ token thô dạng rõ: vi phạm NFR-07.
- **Quyết định (đề xuất):**
  1. Chuẩn hóa email: `trim`, chữ thường toàn bộ, kiểm tra theo regex `^[^@\s]+@[^@\s]+\.[^@\s]+$` và dài ≤ 254. Không bỏ dấu chấm hay phần `+tag`.
  2. Token: 32 byte từ `SecureRandom`, base64url không padding (43 ký tự). Lưu `SHA-256`.
  3. Một transaction cho `POST /auth/magic-link`: `UPDATE login_token SET superseded_at = now() WHERE email = :email AND used_at IS NULL AND superseded_at IS NULL`; chèn token mới (`expires_at = now() + 15 phút`, `return_to`, `locale`); chèn outbox `EMAIL_MAGIC_LINK` với payload `{"email","locale","returnTo","tokenCiphertext","nonce"}` theo phương án 2. Job outbox giải mã, gửi, rồi `UPDATE outbox SET payload = payload - 'tokenCiphertext' - 'nonce'`. Đây là ⚠ so với NFR-07 theo nghĩa đen; token chỉ tồn tại dạng mã hóa tối đa đến khi gửi xong hoặc hết hạn 15 phút (job dọn, DR-74).
  4. Giới hạn gửi (3 mỗi email/15 phút, 10 mỗi IP/giờ, SDD gốc 5.2) đếm bằng truy vấn trên `login_token` trong cùng transaction (`count(*) WHERE email = :email AND created_at > now() - interval '15 minutes'`), không cần Redis; vượt ngưỡng vẫn trả **202** (không lộ thông tin) nhưng không chèn token; giao diện "Gửi quá nhiều lần" hiện khi response có header `X-Magic-Link-Throttled: 1`. Kiểm tra theo IP dùng Redis token bucket (DR-56) nếu có, bỏ qua khi Redis không khả dụng.
  5. `POST /auth/verify` tiêu thụ token: `UPDATE login_token SET used_at = now() WHERE token_hash = :h AND used_at IS NULL AND superseded_at IS NULL AND expires_at > now() RETURNING email, return_to, locale`. Response chứa `returnTo`; trình duyệt nào mở link thì trình duyệt đó có session. Trình duyệt đã yêu cầu link không tự đăng nhập (không có "đăng nhập chéo thiết bị", tránh lừa đảo bằng link).
  6. `return_to` chỉ chấp nhận chuỗi bắt đầu bằng `/`, không bắt đầu bằng `//` hoặc `/\`, dài ≤ 512; sai thì dùng `/`.
- **Hệ quả:** Cần khóa `OUTBOX_ENCRYPTION_KEY` (32 byte base64) trong `.env`; thêm test "payload outbox đã gửi không còn token".
- **Ghi vào:** DOC-19, DOC-27, DOC-32, DOC-44.

### DR-22 · Session và CSRF
- **Vấn đề:** SDD gốc mục 5.2 chốt hash session, cookie `HttpOnly; Secure; SameSite=Lax`, 30 ngày không hoạt động, header `X-CSRF-Token`, nhưng không nói SPA lấy CSRF token từ đâu, cập nhật `last_seen_at` thế nào mà không ghi database ở mọi request, và cookie `Secure` hoạt động ra sao trên `http://localhost`.
- **Quyết định (đề xuất):**
  - Cookie `tb_session`, giá trị 32 byte base64url; `Path=/; HttpOnly; SameSite=Lax; Secure` (cấu hình `auth.cookie-secure`, mặc định `true`, profile `dev` đặt `false` vì Safari không nhận cookie `Secure` trên `http://localhost`).
  - Tra session qua bộ đệm Caffeine trong tiến trình, TTL 60 giây, tối đa 200.000 mục; đăng xuất xóa mục khỏi bộ đệm (một bản sao, SDD gốc 14.1). `last_seen_at` chỉ cập nhật khi đã cũ hơn 1 giờ. Session hết hạn khi `last_seen_at < now() - 30 ngày` hoặc `revoked_at` khác NULL.
  - CSRF theo kiểu synchronizer token: `session.csrf_token` sinh lúc tạo session, trả trong body của `POST /auth/verify` và `GET /me`; client gửi lại trong header `X-CSRF-Token` cho mọi `POST/PUT/PATCH/DELETE`. So sánh hằng thời gian. Ngoại lệ: `POST /api/v1/webhooks/stripe`, `POST /auth/magic-link`, `POST /auth/verify` (chưa có session).
  - Email không bao giờ vào log; log dùng `user_id`.
- **Ghi vào:** DOC-19, DOC-32.

### DR-23 · Vai trò, hồ sơ tổ chức và kiểm tra sở hữu
- **Vấn đề:** SDD gốc mục 3.1, 5.3 nói tài khoản thành người tạo sự kiện khi lập hồ sơ tổ chức; không nói một tài khoản có mấy tổ chức, nội dung hồ sơ, và ai thấy gì. Canvas Studio 00 có "Tên tổ chức" (bắt buộc) và "Email liên hệ" (không bắt buộc, trống thì dùng email đăng nhập); email đổi lịch (07c) hiện tên và email liên hệ của tổ chức cho người mua.
- **Quyết định (đề xuất):**
  - Một tài khoản có tối đa một tổ chức, một tổ chức có đúng một chủ (`organizer.owner_user_id UNIQUE`). Thành viên nhiều người để sau.
  - `POST /organizer` tạo hồ sơ; gọi lần hai trả 409 `ORGANIZER_EXISTS`. Tên 1–120 ký tự; email liên hệ theo quy tắc DR-21.
  - Mọi endpoint `/organizer/**` lấy `organizer_id` từ session (không nhận từ client), và mọi truy vấn tài nguyên có `AND organizer_id = :sessionOrg`; không thấy thì trả **404** `NOT_FOUND` cho tài nguyên của tổ chức khác thay vì 403 (không lộ tồn tại). 403 `FORBIDDEN` chỉ dùng khi tài khoản chưa có hồ sơ tổ chức mà vào `/organizer/**`; giao diện chuyển tới màn Lập hồ sơ. Màn E3 "Không có quyền vào studio" hiển thị cho cả hai trường hợp.
  - `GET /me` trả `{ userId, email, locale, roles: ["BUYER","ORGANIZER"], organizer: { organizerId, name } | null, csrfToken, serverTime }`.
- **Ghi vào:** DOC-19, DOC-32, DOC-53.

---

## E. Sự kiện và loại vé

### DR-24 · Trạng thái hiển thị và các chuyển trạng thái tự động của event
- **Vấn đề:** SDD gốc mục 6.1 có 5 trạng thái lưu. Canvas dùng thêm "Sắp mở bán", "Đang mở bán", "Hết vé", "Đã xuất bản" là trạng thái suy ra. SDD gốc không nói ai chuyển `PUBLISHED → ENDED`, reservation đang mở được thanh toán tiếp không khi event `PAUSED`, và danh sách công khai lọc những gì.
- **Quyết định (đề xuất):**
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
  - `PAUSED` và `SALE_CLOSED` chặn giữ vé mới (409 `EVENT_NOT_ON_SALE`); reservation đã `ACTIVE` vẫn tạo được PaymentIntent và thanh toán tới hết hạn. `ENDED` cũng vậy (thanh toán của reservation còn hạn vẫn được xác nhận).
  - `GET /events` trả event `PUBLISHED`/`PAUSED` có `ends_at > now()`, sắp theo `starts_at` tăng dần, phân trang cursor (DR-63). Event `ENDED`, `CANCELLED` vẫn mở được bằng URL trực tiếp (trang hiện trạng thái), không có trong danh sách.
- **Ghi vào:** DOC-20, DOC-40, DOC-43.

### DR-25 · Quy tắc hợp lệ của trường sự kiện
- **Vấn đề:** SDD gốc mục 6.1 nêu điều kiện xuất bản; canvas Studio 02 có thêm thông báo lỗi theo ô và quy tắc "Đóng bán không được muộn hơn giờ bắt đầu sự kiện", ô "Số vé tối đa mỗi đơn" chỉ nói "từ 1 trở lên".
- **Quyết định (đề xuất):** Lưu bản nháp chỉ cần `name`. Xuất bản cần đủ:

  | Trường | Quy tắc | Mã lỗi trường |
  | --- | --- | --- |
  | `name` | 1–120 ký tự sau trim | `required`, `too_long` |
  | `venue` | 1–200 ký tự | `required` |
  | `description` | ≤ 5.000 ký tự, văn bản thuần, giữ xuống dòng | `too_long` |
  | `startsAt` | > `now()` lúc xuất bản | `must_be_future` |
  | `endsAt` | > `startsAt`, ≤ `startsAt` + 72 giờ | `must_be_after_start`, `too_long_event` |
  | `saleStartsAt` | < `saleEndsAt` | `required` |
  | `saleEndsAt` | > `saleStartsAt`, ≤ `startsAt` | `must_be_after_sale_start`, `after_event_start` |
  | `maxTicketsPerOrder` | số nguyên 1–10, mặc định 8 | `out_of_range` |

  Lỗi trả 422 `VALIDATION_FAILED` với `errors: [{ "field": "saleEndsAt", "rule": "after_event_start" }]`; client tra key i18n `validation.<rule>`.
- **Hệ quả:** Giới hạn trên 10 vé mỗi đơn là quyết định mới (SDD gốc chỉ có mặc định 8).
- **Ghi vào:** DOC-20, DOC-55.

### DR-26 · Quy tắc loại vé — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 6.2–6.3 có mô hình, giá, quy tắc xóa. Canvas thêm: tên không trùng ("Tên này đã dùng cho loại vé khác"), màu gán theo thứ tự tạo với bảng 5 màu, giá "Nhập giá, hoặc 0 nếu miễn phí". SDD gốc không giới hạn số loại vé, không nói đổi mô hình (`SEAT`→`ZONE`) có được không.
- **Quyết định (đề xuất):**
  - Tối đa **5 loại vé** mỗi sự kiện ở giai đoạn này, khớp bảng màu `type-1…type-5` của design system (⚠: SDD gốc không giới hạn). Màu: `color_index` nhỏ nhất chưa dùng trong event lúc tạo; xóa loại vé thì màu được dùng lại.
  - Tên duy nhất trong event, không phân biệt hoa thường (index `ticket_type_name_uq`).
  - Giá: `0` hoặc trong khoảng của DR-13.
  - `model` không đổi được sau khi event đã xuất bản; trong bản nháp đổi tự do (gán ghế/zone trên sơ đồ sẽ báo lỗi validate nếu không khớp).
  - Sức chứa GA: 1–100.000. Sau khi mở bán, giảm tối thiểu tới số unit đang `HELD` + `SOLD` (thông báo "Không thấp hơn N vé đang giữ và đã bán").
  - Xóa: bản nháp thì xóa thật; đã xuất bản thì chỉ khi không có unit `HELD`/`SOLD`, đặt `deleted_at`, unit `AVAILABLE` sang `REMOVED`, pool đặt `removed_at`.
- **Ghi vào:** DOC-20, DOC-56.

### DR-27 · Transaction xuất bản event và tạo kho vé — 🔬 spike
- **Vấn đề:** SDD gốc mục 6.1 nói kho vé tạo trong cùng transaction khi xuất bản. Không nói cách chèn tới 100.000 dòng, thời gian cho phép, và event không có sơ đồ (chỉ GA) đi đường nào.
- **Quyết định (đề xuất):** `POST /organizer/events/{id}/publish` chạy một transaction:
  1. `SELECT … FROM event WHERE event_id = :id AND organizer_id = :org FOR UPDATE`; trạng thái phải là `DRAFT`.
  2. Kiểm tra điều kiện xuất bản (DR-25; có ≥ 1 loại vé sức chứa > 0; nếu có loại vé `SEAT`/`ZONE` thì `seat_map.latest_version_no ≥ 1` và phiên bản mới nhất đã qua validate server).
  3. Unit ghế: một câu `INSERT … SELECT` từ `jsonb_path_query` trên `seat_map_version.document` (bỏ ghế `blocked`), mang `seat_index`, nhãn (DR-40).
  4. Pool zone và GA: chèn `inventory_pool`; unit bằng `INSERT INTO inventory_unit (…) SELECT … FROM inventory_pool p CROSS JOIN generate_series(1, p.capacity)`.
  5. `UPDATE event SET status = 'PUBLISHED', published_at = now(), seat_map_version_id = :v`.
  6. `SET LOCAL statement_timeout = '30s'` cho transaction này.

  Spike **S-03** đo thời gian bước 3–4 với 20.000 ghế + 80.000 unit pool trên PostgreSQL 18 trong compose; mục tiêu < 5 giây. Vượt thì chuyển sang `COPY` qua `PgConnection.getCopyAPI()`.
- **Hệ quả:** Màn Studio 06 hiển thị "Đang xuất bản" cho tới khi response về; timeout HTTP của nginx cho route này là 60 giây.
- **Ghi vào:** DOC-20, DOC-24.

### DR-28 · Hủy sự kiện an toàn khi thanh toán đang đến — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 6.1 chỉ cho hủy khi chưa có đơn `PAID`. Kiểm tra "chưa có đơn PAID" rồi mới đổi trạng thái là check-then-act: một webhook thành công có thể commit ngay sau khi kiểm tra, tạo đơn `PAID` cho sự kiện đã hủy mà không có hoàn tiền tự động.
- **Quyết định (đề xuất):**
  - Hủy: một transaction `SELECT … FROM event … FOR UPDATE`; kiểm tra `NOT EXISTS (SELECT 1 FROM orders WHERE event_id = :id AND status = 'PAID')`; `UPDATE event SET status = 'CANCELLED'`; `UPDATE reservation SET status = 'EXPIRING', close_reason = 'EVENT_CANCELLED', expiring_since = now() WHERE event_id = :id AND status = 'ACTIVE'`. Job trả vé xử lý tiếp như hết hạn (hủy PaymentIntent rồi trả vé).
  - Transaction xác nhận thanh toán (DOC-26) mở đầu bằng `SELECT status FROM event WHERE event_id = :e FOR SHARE`. Hai transaction tuần tự hóa trên dòng event: nếu hủy commit trước, xác nhận thấy `CANCELLED` và đi luồng thanh toán đến trễ với kết quả `NEEDS_REVIEW` (lý do `EVENT_CANCELLED`); nếu xác nhận commit trước, hủy thấy đơn `PAID` và trả 409 `EVENT_HAS_PAID_ORDERS`.
  - Lệnh giữ vé không khóa dòng event (đường nóng); nó đọc `status` không khóa, và reservation sinh ra sau khi hủy sẽ bị câu `UPDATE … WHERE status = 'ACTIVE'` của job dọn hoặc xác nhận đi luồng `NEEDS_REVIEW`.
- **Hệ quả:** `FOR SHARE` trên một dòng event chỉ xảy ra ở webhook (nhịp thấp hơn giữ vé nhiều lần); không tạo hot row cho đường giữ vé. Test bắt buộc: hủy và webhook song song 1.000 lần, không có event `CANCELLED` nào có đơn `PAID`.
- **Ghi vào:** DOC-20, DOC-26.

### DR-29 · Email báo đổi giờ hoặc địa điểm
- **Vấn đề:** SDD gốc mục 6.3 nói người đã mua nhận email khi đổi `starts_at` hoặc địa điểm. Canvas 07c có ba biến thể (đổi giờ, đổi địa điểm, cả hai) kèm danh sách vé và email liên hệ tổ chức. SDD gốc không nói gửi cho ai, mỗi lần sửa gửi một email hay gom lại.
- **Quyết định (đề xuất):** Trong transaction `PATCH /organizer/events/{id}`, nếu event không phải `DRAFT` và `starts_at`, `ends_at` hoặc `venue` đổi, chèn một dòng outbox `EMAIL_EVENT_CHANGED` cho mỗi đơn `PAID` của event, payload `{ orderId, locale, before: { startsAt, endsAt, venue }, after: {…} }`. Không gom; mỗi lần lưu là một đợt. Response của PATCH trả `notifiedOrders: n`, màn Studio 02 hiện "Đã gửi thông báo tới n người mua".
- **Ghi vào:** DOC-20, DOC-27, DOC-51.

### DR-30 · Đổi sức chứa và giá khi đang bán: đồng thời với giữ vé
- **Vấn đề:** SDD gốc mục 6.3 cho tăng/giảm sức chứa zone và GA, nhưng giảm sức chứa đang có người giữ vé đồng thời không được nói cách làm.
- **Quyết định (đề xuất):**
  - Tăng: chèn thêm `Δ` unit vào pool và `UPDATE inventory_pool SET capacity = capacity + Δ` trong một transaction.
  - Giảm `Δ`: `WITH victims AS (SELECT unit_id FROM inventory_unit WHERE pool_id = :p AND status = 'AVAILABLE' LIMIT :Δ FOR UPDATE SKIP LOCKED) UPDATE inventory_unit SET status = 'REMOVED' FROM victims …`; số dòng phải bằng `Δ`, ít hơn thì rollback và trả 409 `CAPACITY_BELOW_USED` kèm `used = count(HELD+SOLD)`; cùng transaction giảm `capacity`.
  - Đổi giá: chỉ `UPDATE ticket_type SET price`; giữ vé đọc giá trong transaction giữ vé nên reservation tạo sau commit dùng giá mới.
- **Ghi vào:** DOC-20, DOC-24.

---

## F. Sơ đồ chỗ ngồi

### DR-31 · Một sơ đồ cho mỗi sự kiện — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 7.8 nói "một sơ đồ có thể dùng lại cho nhiều sự kiện", nhưng tài liệu sơ đồ gán thẳng `ticketTypeId` (ví dụ `tt-vip`) cho hàng, ghế, zone, mà loại vé thuộc về một event (mục 6.2). Dùng lại cho event thứ hai thì mọi `ticketTypeId` trỏ sai. Canvas cũng đặt sơ đồ là bước 3 trong 5 bước soạn một sự kiện.
- **Các phương án:** (1) Một sơ đồ cho mỗi event, `ticketTypeId` là ID thật của `ticket_type`; dùng lại bằng "nhân bản sơ đồ" ở giai đoạn sau. (2) Tài liệu dùng "hạng chỗ" cục bộ (`category: "cat-1"`), event có bảng ánh xạ hạng chỗ → loại vé; phức tạp hơn ở editor, validate, diff và màn Loại vé.
- **Quyết định (đề xuất):** Phương án 1. `seat_map.event_id UNIQUE`; `POST /organizer/maps` nhận `eventId`. Sơ đồ chỉ sửa được bởi tổ chức sở hữu event.
- **Hệ quả:** Phần "dùng lại cho nhiều sự kiện" chuyển vào danh sách để sau; ERD bỏ quan hệ nhiều-nhiều giữa event và phiên bản.
- **Ghi vào:** ADR-0010, DOC-14, DOC-23.

### DR-32 · JSON Schema của tài liệu sơ đồ v1, giới hạn và checksum
- **Vấn đề:** SDD gốc mục 7.8 có một ví dụ JSON, nhưng không có schema đủ để validate: kiểu điểm điều khiển của bezier, polyline, rect/ellipse có xoay, trang trí loại ảnh, ID của đối tượng, giới hạn kích thước ngoài "5 MB" và "20.000 ghế"; checksum tính trên chuỗi nào. Canvas editor có ô "Khung vẽ 900 × 720", "Đường kính ghế 20", "Khoảng cách ghế tối thiểu 24", "Bắt dính lưới 10" trong thuộc tính sơ đồ.
- **Quyết định (đề xuất):** JSON Schema 2020-12 ở `frontend/src/map-core/schema/seat-map.v1.json`, backend nạp cùng file (copy lúc build). Các điểm chốt:

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

### DR-33 · Đánh số ghế và nhãn hàng
- **Vấn đề:** SDD gốc mục 7.4 nêu kiểu đánh số "liên tục, hoặc lẻ và chẵn tách hai phía từ giữa", hướng đánh số, nhãn gợi ý A…Z, AA, nhưng không định nghĩa chính xác kiểu "lẻ chẵn từ giữa" và ghi đè số ghế tương tác thế nào khi đổi số ghế.
- **Quyết định (đề xuất):**
  - `numbering = { start, direction: "forward"|"reverse", scheme: "sequential"|"odd-even-center" }`.
  - `sequential`: ghế thứ i (0-based, theo chiều đường) có số `start + i` (forward) hoặc `start + N − 1 − i` (reverse).
  - `odd-even-center`, N ghế: chia giữa `m = ceil(N/2)`. Ghế từ giữa sang phải (forward) mang số lẻ `start, start+2, …` (nếu `start` chẵn thì `start+1`), ghế từ giữa sang trái mang số chẵn `start+1, start+3, …`. Ví dụ N = 7, start = 1: vị trí trái→phải `6 4 2 1 3 5 7`. `reverse` đổi hai phía.
  - Nhãn hàng gợi ý: hàng kế tiếp sau nhãn lớn nhất trong cùng section theo thứ tự A…Z, AA…AZ, BA…; không bỏ chữ I, O.
  - Ghi đè số ghế lưu ở `seats[].numberOverride`; đổi số ghế N hoặc kiểu đánh số tính lại `number` cho ghế không có override.
  - Hiển thị: số ghế hiện nguyên chuỗi; trên cuống vé (thẻ "Hàng C · Ghế 09") số thuần chữ số được đệm 2 chữ số.
- **Ghi vào:** DOC-21.

### DR-34 · Thuật toán hình học còn thiếu
- **Vấn đề:** SDD gốc mục 7.4 cho công thức rải ghế theo độ dài cung, nhưng không nói: cung qua ba điểm thẳng hàng; "kéo dài đường cho vừa" làm gì với từng kiểu đường; nhân bản song song cho gấp khúc và bezier; khối ghế (công cụ B) đặt các hàng thế nào; số ghế tối đa đặt vừa tính ra sao.
- **Quyết định (đề xuất):**
  - Cung: tâm là giao của hai trung trực; nếu ba điểm gần thẳng hàng (`|cross| < 1e-6 × |AB|²`) thì coi như đường thẳng.
  - Số ghế tối đa: `floor(L / minSpacing) + 1`.
  - Kéo dài cho vừa N ghế, độ dài cần `L' = (N − 1) × minSpacing`: thẳng → dời điểm cuối dọc hướng đường; cung → giữ tâm và bán kính, tăng góc quét đều hai phía; gấp khúc → kéo dài đoạn cuối; bezier → co giãn đồng dạng quanh điểm đầu với tỉ lệ `L'/L`.
  - Nhân bản song song: thẳng → tịnh tiến theo pháp tuyến; cung → đồng tâm, bán kính `r + k·gap`, tùy chọn thêm ghế `N_k = round(N × (r + k·gap)/r)`; gấp khúc và bezier → tịnh tiến theo pháp tuyến của dây cung nối hai đầu (không tính offset curve).
  - Khối ghế: hình chữ nhật kéo ra chia `rows` hàng thẳng cách đều theo chiều cao, mỗi hàng `per` ghế; kiểm tra khoảng cách cả theo hàng và giữa hàng (`h/(rows−1) ≥ minSpacing`); báo "Khối này đặt vừa tối đa …" như canvas 04e.
  - Nhãn zone đặt tại "pole of inaccessibility" (thuật toán polylabel, độ chính xác 1 đơn vị).
- **Ghi vào:** DOC-21.

### DR-35 · Validate dùng chung giữa client và server — 🔬 spike
- **Vấn đề:** SDD gốc mục 7.7 chạy validate liên tục ở client và chạy lại ở server khi xuất bản, "kết quả của server là kết quả cuối cùng". Hai bản cài đặt (TypeScript và Java) phải cho cùng kết quả, gồm phép thử đa giác tự cắt và ghế nằm trong zone; SDD không nói server dùng thư viện gì và định dạng danh sách vấn đề.
- **Quyết định (đề xuất):**
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

### DR-36 · Tự lưu bản nháp và xung đột giữa tab
- **Vấn đề:** SDD gốc mục 7.6 nói tự lưu sau 2 giây không đổi, `PUT /maps/{id}/draft` kèm `revision`, 409 khi tab khác đã lưu. Không nói khi lưu lỗi mạng thì sao, gửi toàn bộ tài liệu hay diff, và màn 04j "Tab khác đã lưu".
- **Quyết định (đề xuất):**
  - Gửi toàn bộ tài liệu (≤ 5 MB) với body `{ "revision": 41, "document": {…} }`; server `UPDATE seat_map SET draft = :doc, draft_revision = draft_revision + 1 WHERE seat_map_id = :id AND draft_revision = :rev` và trả `{ "revision": 42 }`; 0 dòng → 409 `REVISION_CONFLICT`.
  - Debounce 2 giây sau thay đổi cuối; đang lưu thì gom thay đổi vào lần lưu sau. Body > 256 KB được client nén `Content-Encoding: gzip`; API giải nén bằng một filter `GzipRequestFilter` (giới hạn 5 MB sau giải nén).
  - Lỗi mạng hoặc 5xx: thử lại sau 2, 4, 8, 16, 30 giây; chỉ báo "Chưa lưu được" trên thanh trên. Tài liệu chưa lưu được giữ thêm trong IndexedDB theo `seat_map_id` để khôi phục sau khi tải lại.
  - 409: dừng tự lưu, hiện hộp thoại 04j; "Tải lại bản mới nhất" bỏ thay đổi cục bộ.
  - Ngăn xếp undo không lưu server.
- **Ghi vào:** DOC-22, DOC-57.

### DR-37 · Xuất bản phiên bản mới khi event đang bán: thuật toán so sánh
- **Vấn đề:** SDD gốc mục 7.8 cho bảng quy tắc so sánh theo UUID và "áp dụng trong một transaction, tất cả hoặc không", nhưng không có các bước, không nói khóa unit thế nào khi người mua đang giữ cùng lúc, định dạng danh sách xung đột (canvas 04i: vị trí · thao tác · lý do).
- **Quyết định (đề xuất):** `POST /organizer/maps/{id}/publish` khi event đã `PUBLISHED`/`PAUSED`:
  1. Validate server; lỗi → 422 `MAP_VALIDATION_FAILED` kèm `issues`.
  2. Tính diff giữa `document` mới và phiên bản đang dùng theo ID: `seatsAdded`, `seatsRemoved`, `seatsRelabeled` (đổi section/hàng/số), `seatsRetyped`, `seatsBlocked`, `seatsUnblocked`, `zonesAdded`, `zonesRemoved`, `zoneCapacityChanged`, `zonesRetyped`.
  3. Một transaction: `SELECT … FROM event … FOR UPDATE` (chặn hai lần xuất bản song song). Với mỗi nhóm thao tác chạm unit hiện có (`removed`, `relabeled`, `retyped`, `blocked`): `UPDATE inventory_unit SET … WHERE event_id = :e AND seat_id = ANY(:ids) AND status = 'AVAILABLE'`, so số dòng với số ID; mọi ID không cập nhật được được ghi vào danh sách xung đột (truy vấn `status` hiện tại để ghi lý do `HELD`/`SOLD`). Ghế thêm/bỏ blocked → chèn unit mới hoặc `REMOVED → AVAILABLE`. Zone: theo DR-30.
  4. Danh sách xung đột khác rỗng → `ROLLBACK`, trả 409 `MAP_VERSION_CONFLICT` với `conflicts: [{ "objectId", "label": {"row":"C","seats":["5","7"]}, "action": "REMOVE_SEAT", "reason": "SOLD" }]`.
  5. Rỗng → chèn `seat_map_version`, cập nhật `event.seat_map_version_id`, commit.

  `UPDATE` có điều kiện (không `SKIP LOCKED`) chờ lock của transaction giữ vé đang mở (tối đa `statement_timeout` 2 giây của giữ vé); `SET LOCAL lock_timeout = '5s'` cho transaction xuất bản; hết thời gian → 409 `MAP_PUBLISH_BUSY`, người tổ chức thử lại.
- **Hệ quả:** Đây là mục thứ ba trong thứ tự cắt giảm của SDD gốc (mục 16); nếu cắt, xuất bản phiên bản mới khi đã mở bán trả 409 `MAP_LOCKED_AFTER_SALE`.
- **Ghi vào:** DOC-23.

### DR-38 · Lưu ảnh sự kiện và ảnh mặt bằng
- **Vấn đề:** SDD gốc mục 6.3 và 7.3 có ảnh sự kiện và ảnh nền mặt bằng; mục 14.4 giới hạn loại và kích thước ảnh nền; nhưng kiến trúc không có kho file (chỉ PostgreSQL, Redis). Canvas Studio 02 có "Chọn ảnh … 1920 × 1080 · 420 KB", tỷ lệ 16:9.
- **Các phương án:** (1) Bảng `media` kiểu `bytea` trong PostgreSQL: một nơi lưu, backup chung, không thêm container; tốn dung lượng DB. (2) Volume Docker do nginx phục vụ: nhanh, nhưng thêm một nơi lưu và quyền ghi. (3) MinIO: thêm container và SDK.
- **Quyết định (đề xuất):** Phương án 1.

  ```sql
  CREATE TABLE media (
    media_id     uuid PRIMARY KEY DEFAULT uuidv7(),
    organizer_id uuid NOT NULL REFERENCES organizer,
    purpose      text NOT NULL CHECK (purpose IN ('EVENT_IMAGE','FLOOR_PLAN')),
    content_type text NOT NULL CHECK (content_type IN ('image/jpeg','image/png','image/webp')),
    bytes        bytea NOT NULL,
    size_bytes   int  NOT NULL CHECK (size_bytes <= 5242880),
    width        int  NOT NULL,
    height       int  NOT NULL,
    sha256       bytea NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now()
  );
  ```

  `POST /organizer/media` (multipart, ảnh sự kiện ≤ 2 MB, mặt bằng ≤ 5 MB; kiểm tra magic bytes, đọc kích thước bằng `ImageIO`, từ chối > 8000 px mỗi cạnh). `GET /media/{id}` công khai, `Cache-Control: public, max-age=31536000, immutable`, nginx cache lại (`proxy_cache`). Không resize ở server; client hiển thị `object-fit: cover`.
- **Ghi vào:** ADR-0015, DOC-15, DOC-37.

### DR-39 · Kiến trúc editor và cách đo NFR-06 — 🔬 spike
- **Vấn đề:** SDD gốc mục 7.9 và 13.2 chốt hướng (một shape tùy biến cho ghế, R-tree, mức chi tiết theo zoom, bitmap khi kéo, store ngoài React) nhưng NFR-06 "30 fps trên laptop phổ thông" không có máy chuẩn và cách đo.
- **Quyết định (đề xuất):**
  - Store: Zustand vanilla, trạng thái `{ doc, selection, tool, viewport, history }`; mọi thay đổi `doc` qua `Command { do(doc), undo(doc), label }`, áp dụng bất biến bằng Immer; `history` tối đa 200.
  - Render: Konva Stage với 4 layer như SDD gốc 7.2; layer ghế là một `Konva.Shape` với `sceneFunc` vẽ trực tiếp các ghế trong khung nhìn lấy từ rbush; react-konva chỉ dùng cho overlay và tay nắm. `listening(false)` cho layer ghế; hit-test bằng rbush.
  - Mức chi tiết: < 40% vẽ khối section; 40–150% ghế; ≥ 150% thêm số ghế (SDD gốc 7.9).
  - Máy chuẩn cho NFR-06: laptop 4 nhân, đồ họa tích hợp, Chrome stable, màn hình 1920 × 1080, `devicePixelRatio = 1`; cấu hình thực tế ghi vào EXP-09. Đo fps bằng `requestAnimationFrame` trong script Playwright pan/zoom 10 giây, lấy p5 của fps khung hình.
  - Spike **S-04**: nguyên mẫu 20.000 ghế với Konva custom shape, đo fps pan/zoom và thời gian mở.
- **Ghi vào:** ADR-0014, DOC-22, DOC-79.

### DR-40 · Nhãn ghế lưu vào unit; ghế blocked không có unit
- **Vấn đề:** Vé và email cần "Khán đài A · Hàng C · Ghế 9", trình xem cần ánh xạ ghế sang thứ tự, nhưng SDD gốc không có bảng ghế. SDD gốc cũng không nói ghế `blocked` có unit hay không.
- **Quyết định (đề xuất):**
  - Không có bảng ghế. Khi xuất bản event hoặc phiên bản mới, unit ghế nhận `seat_index` (thứ tự ghế trong tài liệu: duyệt `rows` rồi `seats`), `section_name`, `row_label`, `seat_number`. Phiên bản mới cập nhật `seat_index` cho mọi unit ghế ở mọi trạng thái, vì `seat_index` chỉ là vị trí trong bitmap tình trạng của phiên bản đang dùng; nhãn và loại vé thì chỉ đổi được khi unit `AVAILABLE` (DR-37).
  - Ghế `blocked` không có unit (hoặc unit `REMOVED` nếu bị chặn sau khi đã có). Sức chứa SEAT = số ghế không blocked gán loại vé đó.
- **Ghi vào:** DOC-14, DOC-23.

---

## G. Kho vé và giữ vé

### DR-41 · Quy tắc của lệnh giữ vé — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 8.2, 12.2 cho câu claim và định dạng request, nhưng không chốt: giới hạn vé là theo đơn (mục 6.1 "giới hạn vé mỗi đơn") hay theo người (mục 5.3 "áp giới hạn vé mỗi người"); làm gì khi người mua đã có reservation đang mở (ràng buộc mục 8.1) mà muốn chọn lại; thứ tự câu lệnh trong transaction; mã lỗi khi ngoài khung mở bán.
- **Quyết định (đề xuất):**
  - Giới hạn **theo đơn**: tổng số ghế + tổng `quantity` ≤ `event.max_tickets_per_order`; vượt → 422 `MAX_TICKETS_EXCEEDED`. Không cộng dồn qua nhiều đơn của cùng người (⚠: SDD gốc 5.3 nhắc "mỗi người"; giới hạn cộng dồn để sau cùng chống đầu cơ). Một người vẫn chỉ có một reservation đang mở mỗi sự kiện.
  - Kiểm tra request trước khi lấy connection: 1–10 dòng `items`; `seatIds` không trùng, 1–10 mỗi dòng; `quantity` 1–10; cùng `zoneId` hoặc `ticketTypeId` không xuất hiện hai lần.
  - Transaction (READ COMMITTED, `SET LOCAL statement_timeout = '2s'`, `SET LOCAL lock_timeout = '1s'`):
    1. `INSERT INTO idempotency_key … ON CONFLICT DO NOTHING` (DR-45).
    2. Đọc event (không khóa): phải `PUBLISHED` và `sale_starts_at <= now() < sale_ends_at`; không thì 409 `EVENT_NOT_ON_SALE` (kèm `displayStatus`).
    3. `INSERT INTO reservation (…) VALUES (:rid, …, 'ACTIVE', now() + :hold) ON CONFLICT (user_id, event_id) WHERE status IN ('ACTIVE','EXPIRING') DO NOTHING`; 0 dòng → rollback, 409 `ACTIVE_RESERVATION_EXISTS` kèm `reservationId` của reservation đang mở.
    4. Claim ghế (một câu cho mọi ghế), rồi từng pool (SDD gốc 8.2). Thiếu → rollback, 409 `SEATS_UNAVAILABLE` (`unavailableSeatIds`) hoặc `INSUFFICIENT_CAPACITY` (`poolId`, `requested`).
    5. Đọc loại vé (giá, tên) và nhãn unit, chèn `reservation_item`, chèn `orders` (`PENDING_PAYMENT`, `amount` tính ở server).
    6. Cập nhật response vào dòng idempotency; commit.
  - Ghế phải thuộc loại vé `SEAT`, `zoneId` phải là pool `ZONE` của event, `ticketTypeId` của dòng GA phải là loại vé `GA`; sai → 422 `VALIDATION_FAILED`.
  - Tổng 0 đồng vẫn tạo order `PENDING_PAYMENT`; client gọi `POST /orders/{id}/confirm-free`.
- **Hệ quả:** Màn Chọn chỗ khi gặp `ACTIVE_RESERVATION_EXISTS` đưa người mua tới màn Thanh toán của reservation đó (nút "Tiếp tục thanh toán") hoặc cho hủy để chọn lại.
- **Ghi vào:** DOC-24, DOC-37.

### DR-42 · Tham số giữ vé và job trả vé
- **Vấn đề:** SDD gốc mục 17 để ngỏ "thời hạn giữ vé 10 phút … cần chốt". Mục 8.5 nói reservation `EXPIRING` "kẹt quá 2 phút được lấy lại", còn mục 8.4 nói Stripe lỗi thì "thử lại ở vòng sau"; mục 8.5 báo `ACTIVE` quá hạn hơn 60 giây, mục 14.3 lại nói 2 phút.
- **Quyết định (đề xuất):**

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

### DR-43 · Hủy giữ vé: đường nhanh trong request — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 8.3 cho người mua hủy chuyển `ACTIVE → EXPIRING` rồi job trả vé. Canvas Thanh toán hứa "Đổi ý? Ghế được trả lại ngay cho người khác", và người mua bấm "Chọn lại chỗ" sẽ gặp `ACTIVE_RESERVATION_EXISTS` cho tới khi job chạy (tối đa 5 giây + gọi Stripe).
- **Quyết định (đề xuất):** `DELETE /reservations/{id}` (cần `Idempotency-Key`):
  1. Transaction ngắn: `UPDATE reservation SET status = 'EXPIRING', close_reason = 'BUYER_CANCELLED', expiring_since = now() WHERE reservation_id = :id AND user_id = :uid AND status = 'ACTIVE'`.
  2. Ngay trong request, chạy đúng thủ tục "trả vé" mà job dùng: đọc `orders.payment_intent_id` (câu lệnh mới, sau commit bước 1); có PaymentIntent thì gọi hủy (timeout 3 giây); hủy được hoặc không có → transaction trả vé: `EXPIRING → CANCELLED`, unit `HELD → AVAILABLE` (`reservation_id = NULL`), order `PENDING_PAYMENT → CANCELLED`; sau commit xóa cờ hết vé của các pool liên quan.
  3. Kết quả: 200 `{ "status": "CANCELLED" }`; Stripe lỗi hoặc timeout → 202 `{ "status": "EXPIRING" }`, job hoàn tất sau.
  4. Reservation đã `EXPIRING`/đóng → 200 với trạng thái hiện tại. PaymentIntent đã `succeeded` → 409 `PAYMENT_ALREADY_SUCCEEDED`, giao diện chuyển sang màn Kết quả.
- **Ghi vào:** DOC-24, DOC-26.

### DR-44 · Thanh toán đến trễ và các chuyển trạng thái còn thiếu — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 9.5 cho "giữ lại đúng ghế rồi chạy transaction xác nhận", nhưng máy trạng thái reservation (8.3) coi `EXPIRED`, `CANCELLED` là trạng thái cuối, và máy trạng thái order (9.2) chỉ có `EXPIRED → PAID/NEEDS_REVIEW`, thiếu `CANCELLED → …`. Canvas Kết quả có biến thể "Tiền về trễ, hết vé" hứa "Bạn sẽ được hoàn lại toàn bộ" kèm chỗ trống [THỜI GIAN HOÀN TIỀN], [EMAIL HỖ TRỢ]; SDD gốc không gửi gì cho người mua và không có cách đánh dấu đã hoàn tay.
- **Quyết định (đề xuất):**
  - Thêm chuyển trạng thái, chỉ luồng thanh toán đến trễ được dùng: reservation `EXPIRED → CONFIRMED`, `CANCELLED → CONFIRMED` (đặt `confirmed_late = true`); order `EXPIRED → PAID`, `CANCELLED → PAID`, `EXPIRED → NEEDS_REVIEW`, `CANCELLED → NEEDS_REVIEW`, `PENDING_PAYMENT → NEEDS_REVIEW` (số tiền lệch).
  - Luồng trễ trong một transaction: khóa event `FOR SHARE` (DR-28); event `CANCELLED` → `NEEDS_REVIEW` lý do `EVENT_CANCELLED`. Ngược lại claim lại đúng các ghế (`seat_id = ANY`, `status = 'AVAILABLE'`, `SKIP LOCKED`) và đủ `quantity` mỗi pool, đặt thẳng `SOLD` với `reservation_id` cũ; thiếu bất kỳ → rollback phần claim, order `NEEDS_REVIEW` lý do `LATE_PAYMENT_NO_INVENTORY`.
  - `NEEDS_REVIEW` chèn outbox `EMAIL_PAYMENT_REVIEW` (thêm vào danh sách `kind` của DR-19) gửi người mua đúng nội dung màn 07 biến thể 3; chỗ trống lấy từ cấu hình `support.email` và `support.refund-sla-text` (key i18n, mặc định "5–10 ngày làm việc").
  - Thêm cột `orders.refund_reference text` và `orders.review_resolved_at timestamptz`; RB-01 hướng dẫn hoàn tiền trên Stripe Dashboard rồi ghi hai cột này. Kiểm tra bất biến chỉ báo `NEEDS_REVIEW` chưa có `review_resolved_at`.
  - Số tiền của PaymentIntent khác `orders.amount` hoặc tiền tệ khác → `NEEDS_REVIEW` lý do `AMOUNT_MISMATCH`, không phát hành vé.
- **Ghi vào:** DOC-14, DOC-26, ADR-0004, DOC-49, DOC-65.

### DR-45 · Chi tiết idempotency
- **Vấn đề:** SDD gốc mục 8.6 chốt cơ chế nhưng không nói hash request tính trên gì, request lỗi có được lưu không, áp cho endpoint nào, thiếu header thì sao, và tạo PaymentIntent (gọi Stripe ngoài transaction) dùng bảng key thế nào.
- **Quyết định (đề xuất):**
  - Header `Idempotency-Key` (UUID) bắt buộc ở `POST /events/{id}/reservations`, `DELETE /reservations/{id}`, `POST /orders/{id}/confirm-free`, `POST /orders/{id}/payment-intent`; thiếu → 400 `IDEMPOTENCY_KEY_REQUIRED`.
  - `request_hash = SHA-256(operation + "\n" + path + "\n" + JCS(body))`.
  - Câu đầu tiên của transaction: `INSERT … ON CONFLICT (user_id, idem_key) DO NOTHING`. 0 dòng nghĩa là transaction kia đã commit: đọc dòng đã lưu; khác hash → 422 `IDEMPOTENCY_KEY_REUSED`; cùng hash → trả lại `response_status` và `response_body`, header `Idempotent-Replayed: true`.
  - Chỉ kết quả thành công (2xx) được lưu, vì lỗi làm rollback cả dòng key. Gửi lại sau một 409 sẽ chạy lại thật; đúng ý nghĩa vì 409 không để lại tác dụng phụ.
  - `payment-intent` idempotent tự nhiên theo `order_id` (DR-47); header được nhận để client thống nhất nhưng không ghi vào bảng (không lưu `client_secret` vào database).
  - Dọn key cũ hơn 24 giờ (DR-74).
- **Ghi vào:** DOC-25.

### DR-46 · Cờ hết vé
- **Vấn đề:** SDD gốc mục 10.1 có key `soldout:{pool}` "đặt khi đếm lại thấy pool không còn unit AVAILABLE, xóa khi có vé được trả lại", nhưng không nói khi nào đếm lại, ghế (không có pool) dùng cờ nào, và cờ kẹt khi API dừng giữa chừng.
- **Quyết định (đề xuất):**
  - `soldout:pool:{poolId}` = `1`, TTL 30 giây. Đặt sau khi một lệnh giữ vé nhận `INSUFFICIENT_CAPACITY` và câu đếm `SELECT count(*) FROM inventory_unit WHERE pool_id = :p AND status = 'AVAILABLE'` (index một phần) trả 0. Xóa sau commit của mọi transaction trả unit về pool đó. TTL bảo đảm cờ kẹt tự hết.
  - Ghế: không có cờ riêng; lệnh giữ ghế kiểm tra bitmap tình trạng (DR-62): mọi ghế yêu cầu đều đã `HELD`/`SOLD` trong snapshot → 409 `SEATS_UNAVAILABLE` không chạm database.
  - Cờ là gợi ý: Redis không khả dụng thì bỏ qua bước này.
- **Ghi vào:** DOC-28, DOC-17.

---

## H. Thanh toán

### DR-47 · Tham số PaymentIntent và tranh chấp giữa tạo PaymentIntent với job trả vé
- **Vấn đề:** SDD gốc mục 9.2 nói server tạo PaymentIntent ngoài transaction và lưu `payment_intent_id`. Có một khe hở không được nêu: job chuyển reservation sang `EXPIRING` và đọc `payment_intent_id` (đang NULL) trong lúc request tạo PaymentIntent đã gọi Stripe nhưng chưa lưu ID; job trả vé vì "chưa từng có PaymentIntent", rồi người mua trả tiền thành công → thanh toán đến trễ.
- **Quyết định (đề xuất):**
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

### DR-48 · Xử lý webhook
- **Vấn đề:** SDD gốc mục 9.3 chốt luồng; chưa có đường dẫn cụ thể, dung sai thời gian chữ ký, kiểm tra số tiền, các event hoàn tiền/tranh chấp tạo ngoài hệ thống (hoàn tay theo DR-44).
- **Quyết định (đề xuất):**
  - `POST /api/v1/webhooks/stripe`, đọc body thô (không qua Jackson), `Webhook.constructEvent(payload, sigHeader, secret, 300)`; sai → 400.
  - Event đăng ký: `payment_intent.succeeded`, `payment_intent.payment_failed`, `payment_intent.canceled`, `charge.refunded`, `charge.dispute.created`. Hai loại cuối chỉ ghi log mức WARN kèm `order_id` (giai đoạn này không có luồng hoàn tiền).
  - Một transaction: `INSERT INTO stripe_event … ON CONFLICT DO NOTHING`; 0 dòng → 200. Tìm order theo `payment_intent_id`, không thấy thì theo `metadata.order_id`; không thấy nữa → log ERROR, `outcome = IGNORED`, 200.
  - `succeeded`: kiểm tra `amount_received`, `currency` khớp order (DR-44), rồi transaction xác nhận (SDD gốc 9.2) hoặc luồng trễ. `payment_failed`: `UPDATE orders SET last_payment_error = :decline_code WHERE … status = 'PENDING_PAYMENT'`. `canceled`: chỉ ghi `stripe_event`.
  - Lỗi bất kỳ → rollback, 500; Stripe gửi lại.
- **Ghi vào:** DOC-26, DOC-09.

### DR-49 · Job đối chiếu khi webhook thất lạc
- **Vấn đề:** SDD gốc mục 9.3 có "một job nền hỏi lại Stripe … quá 15 phút", không có nhịp, giới hạn, cách gọi handler.
- **Quyết định (đề xuất):** `PaymentReconcileJob` mỗi 60 giây chọn tối đa 50 order có `payment_intent_id` khác NULL và một trong hai: `status = 'PENDING_PAYMENT' AND created_at < now() - 15 phút`; hoặc reservation `EXPIRING` có `expiring_since < now() - 2 phút`. Với mỗi order, lấy PaymentIntent; `succeeded` → gọi đúng handler `PaymentSucceededHandler` của webhook (các câu ghi có điều kiện nên chạy trùng vô hại); trạng thái khác → bỏ qua. Mỗi lần gọi handler ghi log INFO `reconciled=true`.
- **Ghi vào:** DOC-26.

### DR-50 · Thời điểm trong luồng checkout phía client
- **Vấn đề:** SDD gốc mục 9.2, 13.2 nói client hiển thị Payment Element cùng đồng hồ và hỏi `GET /orders/{id}` "cho đến khi đơn là PAID", không có nhịp hỏi, giới hạn, hành vi 3-D Secure chuyển trang.
- **Quyết định (đề xuất):**
  - Màn Thanh toán tải → `GET /reservations/{id}`; tổng > 0 thì gọi `payment-intent` ngay, nạp Payment Element với `locale` của giao diện và `appearance` từ token (DR-68).
  - Nút trả tiền vô hiệu khi còn < 30 giây; đồng hồ chuyển kiểu "Sắp hết giờ" khi < 2 phút (design system).
  - `stripe.confirmPayment({ redirect: "if_required", confirmParams: { return_url: <APP_BASE_URL>/orders/<orderId> } })`. Không redirect → chuyển sang màn Kết quả.
  - Màn Kết quả hỏi `GET /orders/{id}` mỗi 2 giây trong 60 giây, sau đó mỗi 10 giây tới 10 phút; trong lúc chờ hiện "Đang xác nhận thanh toán". `PAID` → vé; `NEEDS_REVIEW` → biến thể "Tiền về trễ"; `EXPIRED`/`CANCELLED` → về Chọn chỗ.
- **Ghi vào:** DOC-26, DOC-48, DOC-49.

### DR-51 · Cổng thanh toán giả cho test và thực nghiệm
- **Vấn đề:** EXP-06 cần "Stripe giả lập" bơm webhook quanh `expires_at`; EXP-02, EXP-05 chạy hàng nghìn thanh toán, vượt rate limit của Stripe test mode; E2E không nên phụ thuộc mạng. SDD gốc không nói giả lập bằng gì (stripe-mock của Stripe không gửi webhook).
- **Quyết định (đề xuất):**
  - Port `PaymentGateway { createIntent, retrieveIntent, cancelIntent }` và `WebhookSignatureVerifier` trong module `payment`; adapter `StripePaymentGateway` (mặc định) và `FakePaymentGateway` (profile `fake-payments`).
  - Fake lưu PaymentIntent trong bảng `fake_payment_intent` (chỉ có trong profile, migration riêng `db/migration-fake`), mô phỏng trạng thái và lỗi hủy như Stripe, ký webhook bằng HMAC giống định dạng `Stripe-Signature` và POST tới chính webhook endpoint. Endpoint điều khiển `/fake-payments/intents/{id}/succeed?delayMs=&duplicates=&shuffle=` chỉ bật trong profile.
  - Frontend: biến môi trường build `VITE_PAYMENTS=fake` thay Payment Element bằng form giả có nút "Pay (fake)".
  - API từ chối khởi động nếu profile `fake-payments` bật cùng khóa `sk_live_…`.
- **Ghi vào:** ADR-0017, DOC-26, DOC-70.

---

## I. Vé và thông báo

### DR-52 · Định dạng mã vé
- **Vấn đề:** SDD gốc mục 9.4 nói "mã vé ngẫu nhiên, duy nhất"; canvas dùng `GM-4K7P-92XD`.
- **Quyết định (đề xuất):** `PP-XXXX-XXXX`: `PP` là `event.ticket_code_prefix`, `XXXX-XXXX` là 8 ký tự Crockford Base32 (40 bit từ `SecureRandom`). Tiền tố mặc định là chữ cái đầu của hai từ cuối trong tên sự kiện sau khi bỏ dấu, viết hoa ("Hòa nhạc Giao Mùa" → `GM`); không đủ chữ thì `TK`; người tổ chức sửa được ở Studio 02 khi event còn `DRAFT`. Trùng `code` (xác suất không đáng kể) → sinh lại trong cùng transaction tối đa 3 lần.
- **Ghi vào:** DOC-27, DOC-14.

### DR-53 · Outbox: nhịp, lease, backoff; hủy PaymentIntent không qua outbox — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 4.1–4.2 nói outbox gửi "email, lệnh hủy PaymentIntent"; mục 8.4 lại để job trả vé tự gọi Stripe hủy và chờ kết quả trước khi trả vé. Outbox chạy bất đồng bộ nên không cho job biết khi nào hủy xong. SDD gốc không có nhịp, số lần thử, backoff. Gửi email trong lúc giữ transaction vi phạm mục 10.4.
- **Quyết định (đề xuất):**
  - Hủy PaymentIntent do job trả vé (và đường nhanh DR-43) gọi trực tiếp; outbox chỉ chứa email.
  - `OutboxRelay` chạy mỗi 1 giây: (1) transaction claim `UPDATE outbox SET attempts = attempts + 1, next_attempt_at = now() + interval '60 seconds' WHERE outbox_id IN (SELECT outbox_id FROM outbox WHERE status = 'PENDING' AND next_attempt_at <= now() ORDER BY next_attempt_at LIMIT 50 FOR UPDATE SKIP LOCKED) RETURNING *`; commit. (2) Gửi ngoài transaction. (3) Thành công → `status = 'SENT', sent_at = now()`; lỗi → `next_attempt_at = now() + least(10 s × 2^(attempts−1), 1 giờ)`, `last_error`; `attempts >= 12` → `FAILED`, log ERROR.
  - Giao ít nhất một lần; `Message-ID` cố định `<outbox_id@APP_DOMAIN>` để hộp thư gộp bản trùng.
- **Ghi vào:** DOC-27, DOC-15, ADR-0006.

### DR-54 · Mẫu email và người gửi
- **Vấn đề:** SDD gốc mục 9.4 nêu nội dung email vé; canvas có 3 mẫu (đăng nhập, vé, đổi lịch) bằng tiếng Việt và tên tạm "ticket". Chưa có công cụ dựng mẫu, người gửi, đa ngôn ngữ, bản text.
- **Quyết định (đề xuất):** Thymeleaf, mỗi mẫu một bản HTML (CSS inline theo design system) và một bản text, chuỗi lấy từ `messages_<locale>` (DR-10). Mẫu: `magic-link`, `tickets`, `event-changed`, `payment-review`. Người gửi `MAIL_FROM` (mặc định `ticket <no-reply@ticket.localhost>`); tên sản phẩm `APP_NAME` (mặc định `ticket`, tên tạm theo design system). Link tuyệt đối dựng từ `APP_BASE_URL`. Không đính kèm, không QR (SDD gốc 9.4).
- **Ghi vào:** DOC-27, DOC-51.

---

## J. Chịu tải và kiểm soát tiếp nhận

### DR-55 · Rate limit ở edge (nginx)
- **Vấn đề:** SDD gốc mục 10.1 có "rate limit theo IP tại Nginx; cache trang sự kiện và tài liệu sơ đồ", không có con số. Bộ sinh tải k6 chạy từ một IP sẽ bị chính giới hạn này chặn.
- **Quyết định (đề xuất):**

  | Zone `limit_req` | Áp cho | Nhịp | Burst |
  | --- | --- | --- | --- |
  | `api_ip` | `/api/` | 20 r/s | 40 `nodelay` |
  | `auth_ip` | `/api/v1/auth/magic-link` | 10 r/phút | 5 |
  | `hold_ip` | `POST /api/v1/events/*/reservations` | 5 r/s | 10 |

  `limit_req_status 429`; trang lỗi JSON dạng Problem Details `RATE_LIMITED`. IP trong biến `RATE_LIMIT_ALLOWLIST` (map `geo`) được miễn: dùng cho máy sinh tải trong thực nghiệm. Cache: `GET /api/v1/events/{id}/map?version=n` cache 1 ngày ở nginx (`proxy_cache`), `GET /api/v1/events/{id}` cache 5 giây, `/media/` cache 1 ngày. `client_max_body_size 6m`.
- **Ghi vào:** DOC-28, DOC-62.

### DR-56 · Token bucket theo người dùng và dự phòng khi mất Redis
- **Vấn đề:** SDD gốc mục 10.1 có token bucket bằng Lua theo người dùng và mục 10.2 có "rate limiter trong bộ nhớ với ngưỡng thấp" khi mất Redis, không có tham số.
- **Quyết định (đề xuất):** Một script `token_bucket.lua` (key, capacity, refill/giây, now_ms, cost) trả `allowed`, `retryAfterMs`.

  | Bucket | Key | Sức chứa | Nạp lại |
  | --- | --- | --- | --- |
  | Giữ vé, hủy giữ | `rl:hold:{userId}` | 5 | 1 mỗi 2 giây |
  | Tạo PaymentIntent | `rl:pi:{userId}` | 5 | 1 mỗi 5 giây |
  | Hỏi vị trí hàng đợi | `rl:queue:{userId}` | 10 | 1 mỗi giây |
  | Magic link theo IP | `rl:magic:{ip}` | 10 | 10 mỗi giờ |

  Mất Redis: Bucket4j trong bộ nhớ với cùng tham số chia đôi, cộng một giới hạn toàn cục 200 lệnh giữ vé/giây cho sự kiện `high_demand`; vượt → 503 `OVERLOADED` kèm `Retry-After: 5`. Phát hiện Redis hỏng bằng circuit breaker (3 lỗi liên tiếp → mở 10 giây).
- **Ghi vào:** DOC-28, DOC-17.

### DR-57 · Mô hình phòng chờ, lượt vào và rời hàng — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 10.2 cho hàng đợi "tự bật khi số người trong khu đặt vé chạm `max_active`", nhưng khi hàng đợi chưa bật thì không có tập `admitted` nào để đếm. `pass_ttl` = 10 phút trong SDD gốc, canvas Phòng chờ hiện "Lượt vào còn 04:58" và "Khi đã giữ vé, bạn có đủ 10 phút để thanh toán". Canvas có nút "Rời hàng"/"Rời phòng chờ" và trạng thái "Tạm hết vé"; SDD gốc không có endpoint rời hàng.
- **Quyết định (đề xuất):**
  - Kiểm soát tiếp nhận **luôn chạy** cho mọi sự kiện trong khung mở bán. Vào màn Chọn chỗ, client gọi `POST /events/{id}/queue`: còn chỗ (`|admitted| < max_active`), hàng trống và còn hạn mức nhịp trong giây đó → `ADMITTED` ngay, giao diện không hiện phòng chờ (hàng đợi "trong suốt"). Ngược lại → `WAITING`, giao diện chuyển tới Phòng chờ. `high_demand = true` chỉ thêm hai điều: phòng chờ trước giờ mở bán từ `prequeue_opens` và xáo ngẫu nhiên lúc mở bán.
  - `pass_ttl` = **5 phút** để chọn chỗ (⚠: SDD gốc 10 phút); giữ vé thành công thì lượt vào được kéo tới `expires_at` của reservation (SDD gốc 10.2 bước 5). Reservation đóng mà không thanh toán thì lượt vào còn thêm 2 phút để chọn lại (`admission.repick-grace`), sau đó hết.
  - `DELETE /events/{id}/queue` xóa người dùng khỏi `prequeue`, `queue`, `admitted`, `seen`; chỗ được cấp cho người kế tiếp ở vòng job sau.
  - Trạng thái trả về: `PRE_QUEUE`, `WAITING`, `ADMITTED`, `PAUSED` ("Tạm hết vé": mọi vé còn lại đang được giữ), `SOLD_OUT`, `NOT_IN_QUEUE`. "Mất kết nối" là trạng thái chỉ của client.
  - Tham số theo sự kiện lấy mặc định cấu hình (SDD gốc 10.2) và không sửa từ giao diện ở giai đoạn này: `max_active` 500, `admit_rate` 50/giây, `pass_ttl` 5 phút, `prequeue_opens` 30 phút, `idle_timeout` 2 phút.
  - Redis không khả dụng: sự kiện thường giữ vé không cần token; sự kiện `high_demand` dùng giới hạn toàn cục của DR-56.
- **Ghi vào:** DOC-28, ADR-0008, DOC-45.

### DR-58 · Định dạng token vào cửa
- **Vấn đề:** SDD gốc mục 10.2 nói "token vào cửa có chữ ký, gắn với `user_id` và sự kiện", không nói định dạng, khóa, header.
- **Quyết định (đề xuất):** `v1.<base64url(payload)>.<base64url(HMAC-SHA256(secret, "v1." + payload))>`, payload `{"u":"<userId>","e":"<eventId>","iat":1791183600,"exp":1791184500}`; `exp = iat + 15 phút` là trần cứng. Khóa `ADMISSION_TOKEN_SECRET` (32 byte base64). Client gửi header `X-Admission-Token`. Endpoint giữ vé kiểm tra: chữ ký; `u` khớp session; `e` khớp đường dẫn; `exp > now`; và `ZSCORE admitted:{e} u > now_ms` (hạn thật nằm trong Redis, được kéo dài khi giữ vé). Thiếu một điều → 429 `QUEUE_REQUIRED`.
- **Ghi vào:** DOC-28, DOC-32.

### DR-59 · Job cấp lượt, script Lua và nhịp hỏi vị trí
- **Vấn đề:** SDD gốc mục 10.2 mô tả job mỗi giây và cấu trúc key, không có script, cách xáo nhóm chờ trước, ngưỡng của `retryAfterSeconds`, công thức ước lượng khi chưa có dữ liệu.
- **Quyết định (đề xuất):**
  - `AdmissionTicker` (`fixedRate` 1 giây) duyệt tập `queue:events` (sự kiện đang trong khung từ `prequeue_opens` tới `sale_ends_at`). Mỗi sự kiện: `SET admit-lock:{e} <node> NX PX 900`; chạy `admit.lua` (ARGV: `now_ms, max_active, admit_rate, pass_ttl_ms, idle_timeout_ms`): `ZREMRANGEBYSCORE admitted:{e} -inf now`; loại tối đa 1.000 người có `seen` cũ hơn `idle_timeout`; `n = min(max_active − ZCARD admitted, admit_rate, ZCARD queue)`; `ZPOPMIN queue n` → `ZADD admitted now+pass_ttl`; ghi số người được cấp vào `admit-hist:{e}` (list 60 phần tử).
  - Trước khi cấp, đọc snapshot tình trạng (DR-62): không còn vé `AVAILABLE` → không cấp; còn vé `HELD` → `queue-state:{e} = PAUSED`; không còn gì → `SOLD_OUT`.
  - Mở bán với `high_demand`: Java đọc `SMEMBERS prequeue:{e}`, gán điểm `SecureRandom` trong [0, 1), `ZADD` theo lô 1.000, rồi đặt `queue-state:{e} = OPEN`; người đến sau nhận điểm `1 + INCR queue-seq:{e}`.
  - `retryAfterSeconds`: vị trí ≤ 200 → 3; ≤ 2.000 → 10; còn lại → 30; `PRE_QUEUE` → 30.
  - `estimatedWaitSeconds = position / (Σ admit-hist / 60)`; chưa có dữ liệu → `null`, giao diện hiện "Đang ước tính".
- **Ghi vào:** DOC-28, DOC-17.

### DR-60 · Backpressure cho nhịp vào
- **Vấn đề:** SDD gốc mục 10.2 nói "`admit_rate` giảm khi p95 của lệnh giữ vé vượt ngưỡng và tăng lại khi database rảnh", không có ngưỡng hay thuật toán.
- **Quyết định (đề xuất):** AIMD toàn cục (database dùng chung cho mọi sự kiện): mỗi 5 giây đọc p95 của `ticket_hold_duration_seconds` trong 30 giây gần nhất và `hikaricp_connections_pending`. p95 > 300 ms hoặc có connection đang chờ → `rateFactor = max(0,1; rateFactor × 0,7)`; p95 < 150 ms và không chờ → `rateFactor = min(1; rateFactor + 0,1)`. `admit_rate` hiệu lực = `round(admit_rate × rateFactor)`, tối thiểu 5. `rateFactor` lưu trong Redis `admission:rate-factor`. Ngưỡng là cấu hình, hiệu chỉnh ở EXP-05.
- **Ghi vào:** DOC-28, DOC-34.

### DR-61 · Connection pool, bulkhead và timeout
- **Vấn đề:** SDD gốc mục 10.4 chốt nguyên tắc (bulkhead nhỏ hơn pool, trả 503 thay vì chờ, `statement_timeout` 2 giây) mà không có số.
- **Quyết định (đề xuất):**

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

### DR-62 · Định dạng và cache của tình trạng chỗ
- **Vấn đề:** SDD gốc mục 7.10 nói `GET /events/{id}/availability` trả "danh sách ghế không còn trống" và cache 1–2 giây. Với 20.000 ghế, danh sách UUID có thể tới ~750 KB mỗi lần, client hỏi mỗi 3–5 giây. Canvas phân biệt "Có người giữ" và "Đã bán", SDD gốc không tách.
- **Quyết định (đề xuất):**

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

  `held`/`sold` là bitset base64, bit `i` (LSB trước trong mỗi byte) ứng với `seat_index = i` (DR-40); 20.000 ghế ≈ 2,5 KB mỗi bitset. Dựng bằng hai truy vấn (ghế `HELD`/`SOLD` theo index `unit_seat_taken_idx`; đếm `AVAILABLE` theo pool và loại vé). Cache Redis `avail:{e}` PX 2000 với single-flight `SET avail-lock:{e} NX PX 2000`; người thua dùng bản cũ `avail-stale:{e}` (TTL 60 giây). Mất Redis → Caffeine 2 giây trong tiến trình. Client hỏi mỗi 4 giây ± 0,5 giây, dừng khi tab ẩn; response `mapVersion` khác bản đang có → tải lại sơ đồ.
- **Ghi vào:** ADR-0013, DOC-29, DOC-37.

---

## K. API

### DR-63 · Quy ước API
- **Vấn đề:** SDD gốc mục 12 có danh sách đường dẫn không tiền tố, trong khi nginx proxy `/api` (mục 14.1); "problem details" và "cursor" chưa có định dạng; SDD gốc 7.6 viết `PUT /maps/{id}/draft` còn 12.1 viết `/organizer/maps/{id}/draft`.
- **Quyết định (đề xuất):**
  - Tiền tố `/api/v1`; mọi đường dẫn trong SDD gốc hiểu là sau tiền tố; đường dẫn sơ đồ là `/organizer/maps/…`. Ngoại lệ không tiền tố: `/media/{id}`.
  - JSON camelCase; trường không có giá trị trả `null` (không bỏ).
  - Lỗi: RFC 9457 `application/problem+json`, `{ "type": "https://errors.ticket.dev/<code>", "title", "status", "detail", "code", "requestId", …mở rộng }`; `title`/`detail` theo `Accept-Language` (DR-10).
  - Phân trang: `?limit=` (mặc định 20, tối đa 100) và `?cursor=`; response `{ "items": [...], "nextCursor": "…" | null }`; cursor là base64url của `{"k":<khóa sắp xếp>,"id":"<uuid>"}`.
  - `X-Request-Id`: nginx sinh `$request_id` nếu client không gửi; API đặt vào MDC `trace_id` và trả lại trong header.
  - OpenAPI 3.1 ở `/v3/api-docs` (chỉ trong mạng nội bộ compose và profile `dev`), tag theo module.
- **Ghi vào:** DOC-36.

### DR-64 · Bổ sung bảng mã lỗi
- **Vấn đề:** Bảng mã lỗi ở SDD gốc mục 12.3 thiếu các tình huống mà SDD gốc và canvas đều có (ngoài khung bán, đã có reservation mở, quá giới hạn vé, hủy sự kiện có đơn, xung đột phiên bản sơ đồ…).
- **Quyết định (đề xuất):** Giữ mọi mã của SDD gốc, thêm:

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
  | 409 | `EVENT_HAS_PAID_ORDERS` | Hủy sự kiện đã có đơn `PAID` |
  | 409 | `STALE_EVENT_VERSION` | `PATCH` với `rowVersion` cũ |
  | 409 | `ORGANIZER_EXISTS` | Lập hồ sơ lần hai |
  | 409 | `TICKET_TYPE_IN_USE`, `CAPACITY_BELOW_USED` | DR-26, DR-30 |
  | 409 | `MAP_VERSION_CONFLICT`, `MAP_PUBLISH_BUSY`, `MAP_LOCKED_AFTER_SALE` | DR-37 |
  | 413 | `PAYLOAD_TOO_LARGE` | Tài liệu sơ đồ > 5 MB, ảnh vượt giới hạn |
  | 422 | `MAX_TICKETS_EXCEEDED` | DR-41 |
  | 422 | `PUBLISH_PRECONDITIONS_FAILED` | Kèm danh sách điều kiện chưa đạt |
  | 422 | `MAP_VALIDATION_FAILED` | Kèm `issues` (DR-35) |
  | 422 | `TICKET_TYPE_LIMIT_REACHED`, `MEDIA_INVALID` | DR-26, DR-38 |
  | 500 | `INTERNAL_ERROR` | Kèm `requestId` (màn E5) |
  | 503 | `PAYMENT_PROVIDER_UNAVAILABLE` | Stripe lỗi khi tạo PaymentIntent |
- **Ghi vào:** DOC-35, DOC-36, DOC-40.

### DR-65 · Endpoint còn thiếu cho các màn hình
- **Vấn đề:** Canvas cần dữ liệu mà SDD gốc mục 12.1 không có endpoint: danh sách sự kiện của studio kèm số vé (Studio 01), đọc một sự kiện để sửa (Studio 02–06), danh sách điều kiện xuất bản (Studio 06), sơ đồ tô theo trạng thái (Studio 07), rời hàng (Phòng chờ), tải ảnh, đổi ngôn ngữ, đọc sơ đồ theo phiên bản.
- **Quyết định (đề xuất):** Thêm:

  | Endpoint | Màn |
  | --- | --- |
  | `GET /organizer/events` (kèm `sold`, `held`, `available`, `capacity`) | Studio 01 |
  | `GET /organizer/events/{id}` (kèm loại vé, `rowVersion`, trạng thái từng bước) | Studio 02–06 |
  | `GET /organizer/events/{id}/publish-checks` | Studio 06 |
  | `GET /organizer/events/{id}/seat-status` | Studio 07 |
  | `GET /organizer/events/{id}/map` (trả `seatMapId`, bản nháp, `revision`, phiên bản mới nhất) | Studio 04 |
  | `GET /organizer/maps/{id}/versions` | Studio 04 hộp thoại xuất bản |
  | `POST /organizer/media`, `GET /media/{id}` | Studio 02, Studio 04 ảnh nền |
  | `DELETE /events/{id}/queue` | Phòng chờ |
  | `PATCH /me` (`locale`) | Bộ chọn ngôn ngữ |
  | `GET /events/{id}/map?version=n` (cache dài hạn; không có `version` → 302 tới phiên bản đang dùng) | Chọn chỗ |

  `GET /orders/{id}` trả kèm danh sách vé khi `PAID` (SDD gốc 9.2 bước 6).
- **Ghi vào:** DOC-37.

### DR-66 · Giờ máy chủ cho đồng hồ đếm ngược
- **Vấn đề:** SDD gốc mục 13.2 nói đồng hồ tính từ `expiresAt` và "độ lệch giờ so với server", không nói lấy giờ server từ đâu. Canvas có ba đồng hồ: giữ vé, lượt vào, mở bán sau.
- **Quyết định (đề xuất):** Mọi response API có header `X-Server-Time` (epoch ms). Interceptor của client tính `offset = serverTime − (t_gửi + t_nhận)/2`, lấy trung vị 5 mẫu gần nhất. Đồng hồ = `expiresAt − (Date.now() + offset)`, cập nhật mỗi 250 ms, hiển thị `mm:ss`; về 0 thì gọi lại API thay vì tự kết luận.
- **Ghi vào:** DOC-36, DOC-29, DOC-48.

---

## L. Frontend và trải nghiệm

### DR-67 · Bản đồ URL và giữ lựa chọn qua đăng nhập
- **Vấn đề:** SDD gốc mục 13.1 có luồng màn hình, không có URL; mục 5.3 nói "lựa chọn ghế được giữ ở client và khôi phục sau khi đăng nhập" mà không nói lưu ở đâu.
- **Quyết định (đề xuất):**

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

### DR-68 · Áp dụng design system "Vé giấy"
- **Vấn đề:** Canvas có design system v0.1 (màu, chữ, khoảng cách, bo góc, trạng thái ghế, component); SDD gốc không nhắc. Font đang nạp từ Google Fonts; tên sản phẩm "ticket" là tên tạm.
- **Quyết định (đề xuất):** Chép nguyên token v0.1 thành CSS custom properties (`--paper #ECEEF1`, `--surface #FFFFFF`, `--line #D3D7DE`, `--ink-muted #5B6272`, `--ink-2 #2E323C`, `--ink #12141A`, `--stamp #D63312`, `--stamp-deep #B5290C`, `--stamp-tint #FCE9E4`, `--confirm #1B7F4B`, `--type-1…5 #BF2A78 #2747D9 #E5A21A #0B7F6F #6A3FD0`), thang khoảng cách bước 4 px, bo 4/6/999. Font tự host bằng `@fontsource` (Barlow Condensed 600/700, Be Vietnam Pro 400/500/600, IBM Plex Mono 400/500/600), không gọi Google Fonts lúc chạy. Không có dark mode ở giai đoạn này. Tên sản phẩm lấy từ `APP_NAME` (DR-54). Màu của Stripe Payment Element cấu hình qua `appearance.variables` từ cùng token.
- **Ghi vào:** DOC-39.

### DR-69 · Chế độ danh sách, điện thoại và tiếp cận của màn Chọn chỗ
- **Vấn đề:** SDD gốc mục 13.2 yêu cầu chế độ danh sách dùng bằng bàn phím và ưu tiên điện thoại; canvas có nút chuyển "Sơ đồ / Danh sách", bản 390 px (M05), thông báo "Sơ đồ đang thu nhỏ … Phóng to để chọn ghế". Chưa có quy tắc chi tiết.
- **Quyết định (đề xuất):**
  - Danh sách: nhóm Section → Hàng; mỗi ghế còn trống là `<button aria-pressed>` có nhãn "Hàng C, ghế 9, VIP, 1.800.000 ₫"; ghế đang giữ/đã bán không hiện; khu vực và GA là bộ tăng giảm có `aria-label`.
  - Sơ đồ: dưới 40% zoom không chọn được ghế (chỉ thấy khối section) và hiện thông báo; chạm hai ngón để zoom, chạm một ngón để chọn; vùng chạm ghế tối thiểu 24 px màn hình (zoom tự đẩy lên khi chạm vào section ở mức thấp).
  - Điện thoại: tóm tắt đơn là thanh dưới cố định (số vé, tạm tính, nút giữ vé), mở thành bottom sheet.
  - Click ghế đang giữ hoặc đã bán: không thêm, hiện tooltip trạng thái.
- **Ghi vào:** DOC-46, DOC-38.

### DR-70 · Luồng soạn sự kiện trong studio và cách lưu
- **Vấn đề:** SDD gốc mục 13.1 có "Soạn sự kiện: các bước thông tin, loại vé, sơ đồ, xem trước, xuất bản". Canvas có 5 bước + bước 6 "Bán vé" sau xuất bản, nút "Lưu bản nháp", "Lưu và sang …", "Có thay đổi chưa lưu", và bước Sơ đồ bị bỏ qua khi chỉ có GA. Chưa nói lưu tự động hay thủ công, hai tab sửa cùng event thì sao.
- **Quyết định (đề xuất):** Bước Thông tin và Loại vé lưu thủ công (`PATCH` kèm `rowVersion`; 409 `STALE_EVENT_VERSION` → hộp thoại tải lại); rời trang khi có thay đổi chưa lưu → hộp thoại xác nhận. Bước Sơ đồ tự lưu (DR-36). "Tạo sự kiện" gọi `POST /organizer/events { name }` tạo `DRAFT` rồi vào bước Thông tin. Thanh bước cho nhảy tới bước đã xong; bước Sơ đồ ẩn khi không có loại vé `SEAT`/`ZONE`. Màn hình < 1024 px: các bước khác dùng được, bước Sơ đồ hiện màn 04m.
- **Ghi vào:** DOC-38, DOC-55, DOC-56, DOC-57, DOC-58, DOC-59, DOC-60.

### DR-71 · Số liệu bán vé cho người tổ chức
- **Vấn đề:** SDD gốc mục 12.1 có `GET /organizer/events/{id}/sales` "theo loại vé"; canvas Studio 07 có thêm sơ đồ tô theo trạng thái, bộ lọc loại vé, "Cập nhật lúc …", nút "Làm mới"; Studio 01 có số đã bán/đang giữ/còn trống cho mỗi sự kiện.
- **Quyết định (đề xuất):** `sales` tính bằng `SELECT ticket_type_id, status, count(*) FROM inventory_unit WHERE event_id = :e AND status <> 'REMOVED' GROUP BY 1, 2`, cache Redis 5 giây. `seat-status` trả đúng snapshot của DR-62 (dùng chung cache). Giao diện tự làm mới mỗi 15 giây và khi bấm "Làm mới". Studio 01 dùng một truy vấn gộp theo `event_id` cho tối đa 20 sự kiện mỗi trang.
- **Ghi vào:** DOC-60, DOC-37, DOC-29.

---

## M. Vận hành, kiểm thử và thực nghiệm

### DR-72 · Topology compose, cổng, lệnh và dữ liệu mẫu
- **Vấn đề:** SDD gốc mục 14.1 có danh sách container. NFR-08 yêu cầu `docker compose up` chạy toàn bộ, nhưng `stripe-cli` cần khóa Stripe thật trong `.env`; chưa có cổng, healthcheck, tài khoản demo.
- **Quyết định (đề xuất):**

  | Service | Cổng host | Healthcheck | Profile |
  | --- | --- | --- | --- |
  | `nginx` (frontend build + proxy) | 8080 | `GET /healthz` | mặc định |
  | `api` | — (9090 quản trị chỉ trong profile `obs`) | `/actuator/health/readiness` | mặc định |
  | `postgres` | 5432 | `pg_isready` | mặc định |
  | `redis` | 6379 | `redis-cli ping` | mặc định |
  | `mailpit` | 8025 (UI), 1025 (SMTP) | HTTP | mặc định |
  | `stripe-cli` | — | — | `stripe` |
  | `prometheus`, `grafana` | 9091, 3000 | — | `obs` |

  `.env.example` mặc định `PAYMENTS_MODE=fake` để `docker compose up` chạy được không cần tài khoản Stripe; `make up-stripe` bật profile `stripe` với khóa test. `make dev` chạy hạ tầng trong compose, API bằng `./gradlew bootRun`, frontend `pnpm dev` (proxy `/api`). `make seed` tạo tài khoản `organizer@demo.test`, `buyer1@demo.test`…`buyer3@demo.test` và các sự kiện mẫu (DR-78).
- **Ghi vào:** DOC-61, DOC-62.

### DR-73 · Lệnh kiểm tra bất biến — ⚠ lệch SDD gốc
- **Vấn đề:** SDD gốc mục 14.3 nói "một lệnh duy nhất … tự chạy mỗi 5 phút, và chạy tay", không nói chạy tay bằng cách nào, định dạng kết quả, mã thoát; ngưỡng reservation quá hạn không thống nhất (60 giây ở 8.5, 2 phút ở 14.3).
- **Quyết định (đề xuất):**
  - `InvariantChecker` trong module `invariant`; `@Scheduled` mỗi 5 phút; chạy tay `make invariants` = `docker compose run --rm api --spring.profiles.active=invariants` (chạy một lần, in JSON ra stdout, thoát 0 khi sạch, 1 khi có sai lệch).
  - Kiểm tra: các mục của SDD gốc 14.3, ngưỡng theo DR-42, cộng: đơn `PAID` có số vé `ISSUED` = Σ `quantity` của item; event `CANCELLED` có đơn `PAID`; outbox `FAILED`; `NEEDS_REVIEW` chưa `review_resolved_at`; unit `AVAILABLE`/`REMOVED` mà `reservation_id` khác NULL (đã có CHECK, kiểm lại cho chắc).
  - Kết quả: `{ "checkedAt", "durationMs", "violations": [{ "check": "POOL_UNIT_COUNT", "count": 1, "sample": ["<poolId>"] }] }`, tối đa 10 ID mỗi mục; có sai lệch thì log ERROR.
- **Ghi vào:** DOC-30, DOC-66.

### DR-74 · Lưu giữ dữ liệu và job dọn
- **Vấn đề:** SDD gốc mục 8.6 dọn key idempotency sau 24 giờ; các bảng khác (token, session, webhook, outbox, ảnh không dùng) không có chính sách.
- **Quyết định (đề xuất):** `RetentionJob` chạy 03:00 theo `PLATFORM_TIMEZONE`, xóa theo lô 5.000 dòng:

  | Dữ liệu | Giữ |
  | --- | --- |
  | `login_token` | Xóa 24 giờ sau `expires_at`; outbox magic link chưa gửi mà token đã hết hạn → xóa trường token |
  | `session` | Xóa 7 ngày sau khi hết hạn hoặc thu hồi |
  | `idempotency_key` | 24 giờ |
  | `stripe_event` | 30 ngày |
  | `outbox` | `SENT` 7 ngày, `FAILED` 30 ngày |
  | `media` không được tham chiếu | 24 giờ sau khi tạo |
  | reservation, order, ticket, unit | Giữ nguyên (đối soát về sau) |

  Dữ liệu cá nhân: email chỉ ở `app_user`, `login_token`, `organizer.contact_email`, payload outbox; không vào log.
- **Ghi vào:** DOC-18.

### DR-75 · Môi trường và mô hình tải của thực nghiệm — 🔬 spike
- **Vấn đề:** NFR-02 nói "100.000 người dùng đồng thời"; một máy chạy k6 với 100.000 VU riêng lẻ không khả thi về bộ nhớ, và SDD gốc mục 3.4 tự nói con số sẽ hiệu chỉnh theo máy. Chưa có cấu hình máy, số lần lặp, cách tạo 100.000 người dùng có session (đăng nhập bằng magic link từng người là không thực tế).
- **Quyết định (đề xuất):**
  - "Người dùng đồng thời" = người dùng có mặt (đang chờ, chọn chỗ hoặc thanh toán) trong cùng cửa sổ mở bán. k6 mô phỏng bằng VU đa người dùng: mỗi VU quản lý một nhóm người dùng ảo bằng `http.asyncRequest`, tôn trọng `retryAfterSeconds`; S-06 đo số người dùng ảo tối đa một máy chạy được và chốt tỉ lệ.
  - Người dùng tải được tạo bằng script SQL `experiments/seed/users.sql` (100.000 `app_user` + session có giá trị biết trước), chỉ chạy trong profile `experiment`.
  - Mọi thực nghiệm chạy với `PAYMENTS_MODE=fake`, IP máy sinh tải trong `RATE_LIMIT_ALLOWLIST`, giới hạn tài nguyên container cố định (ghi ở DOC-70), lặp 3 lần, báo trung vị và min–max, ghi git SHA và cấu hình máy.
  - Spike **S-06**: k6 trên máy thực nghiệm, mục tiêu 100.000 người dùng ảo trong phòng chờ hỏi vị trí theo `retryAfter`; nếu không đạt, ghi trần thực tế và hiệu chỉnh NFR-02 bằng DR mới.
- **Ghi vào:** DOC-70, DOC-75.

### DR-76 · Bản cài đặt kho vé thay thế cho thực nghiệm
- **Vấn đề:** SDD gốc mục 10.3 và 15.2 cần so phương án D với phương án A (EXP-10) và với "bản cài đặt ngây thơ" (đọc rồi ghi, không điều kiện). Không nói hai bản này sống ở đâu để không lọt vào chạy thật.
- **Quyết định (đề xuất):** Interface `InventoryClaimer` trong `inventory.internal`; ba bản: `SkipLockedClaimer` (mặc định), `CounterClaimer` (phương án A, bảng `inventory_pool_counter` tạo bởi migration riêng `db/migration-experiment`), `NaiveClaimer` (`SELECT … status = 'AVAILABLE'` rồi `UPDATE … WHERE unit_id = …` không điều kiện trạng thái). Chọn bằng `inventory.strategy=skip-locked|counter|naive`; API từ chối khởi động với giá trị khác `skip-locked` nếu không bật profile `experiment`.
- **Ghi vào:** DOC-70, DOC-80, DOC-24.

### DR-77 · Chi tiết chiến lược kiểm thử
- **Vấn đề:** SDD gốc mục 15.1 có loại test và công cụ; chưa có ngưỡng coverage, quy ước đặt tên, cách E2E lấy magic link, cách bắt thay đổi phá hợp đồng API.
- **Quyết định (đề xuất):**
  - Coverage dòng (JaCoCo): `inventory`, `reservation`, `order`, `payment`, `admission` ≥ 85%; module khác ≥ 70%. Frontend `map-core` ≥ 90% (Vitest).
  - Test đồng thời: 64 luồng, `CountDownLatch` xuất phát cùng lúc, lặp 20 lần mỗi test.
  - E2E Playwright đọc magic link qua API Mailpit (`GET /api/v1/message/latest`), dùng `PAYMENTS_MODE=fake` trong CI; `make e2e-stripe` chạy với thẻ test thật khi có khóa.
  - Hợp đồng: `oasdiff breaking` so OpenAPI của PR với `dev`, báo lỗi khi có thay đổi phá vỡ.
  - Mỗi tài liệu thiết kế có tiền tố test riêng, đăng ký ở DOC-69.
- **Ghi vào:** DOC-69.

### DR-78 · Dữ liệu mẫu và kịch bản demo
- **Vấn đề:** SDD gốc mục 16.1 có kịch bản 6 bước; canvas dùng ba sự kiện mẫu. Demo không có dữ liệu dựng sẵn hay phương án dự phòng khi một bước hỏng (ví dụ vẽ sơ đồ lỗi, máy demo yếu không chạy nổi k6 100.000).
- **Quyết định (đề xuất):** `make seed` tạo đúng các sự kiện trên canvas: "Hòa nhạc Giao Mùa" (Nhà hát Bến Sông, 180 ghế VIP/Thường/Sinh viên + khu đứng 300 = 480 vé), "Hội thảo Nghề Thiết Kế" (chỉ GA: Đặt sớm đã hết, Phổ thông, Sinh viên), "Đêm nhạc Sân Thượng" (sắp mở bán, `high_demand`). Tên sự kiện là dữ liệu, không phải chuỗi giao diện. Mỗi bước demo có dự phòng: sơ đồ dựng sẵn để xuất bản nếu vẽ hỏng; bước k6 có cấu hình rút gọn (10.000 người dùng ảo) cho máy yếu; checklist trước demo và `make reset seed`.
- **Ghi vào:** DOC-81, DOC-61.

---

## Tổng hợp theo mức ảnh hưởng

Mỗi DR được xếp vào phase đầu tiên mà nó chặn. Đây là đầu vào của task P0-01 trong master plan.

| Mức | Mục | Vì sao cần chốt sớm |
| --- | --- | --- |
| Chặn P1 | DR-01–12, DR-14, DR-19, DR-21–23, DR-53, DR-54, DR-63, DR-64, DR-67, DR-68, DR-72, DR-77 | Cố định bố cục repo, phiên bản, cách truy cập dữ liệu, khóa chính, bảng xác thực và outbox, quy ước API và lỗi, i18n, design system, compose |
| Chặn P2 | DR-13, DR-15–18, DR-20, DR-24–28, DR-30, DR-38, DR-40–43, DR-45, DR-52, DR-61, DR-65, DR-66, DR-70, DR-73–76 | Schema kho vé và reservation, câu claim, job trả vé, idempotency, xuất bản và hủy sự kiện, kiểm tra bất biến, mô hình tải của EXP-02/03/04/10 |
| Chặn P3 | DR-29, DR-44, DR-47–51 | Tranh chấp tạo PaymentIntent với job trả vé, webhook, thanh toán đến trễ, cổng thanh toán giả cho EXP-06/07 |
| Chặn P4 | DR-31–36, DR-39 | Schema tài liệu sơ đồ, thuật toán hình học, validate dùng chung, tự lưu, kiến trúc editor |
| Chặn P5 | DR-37, DR-62, DR-69, DR-71 | So sánh phiên bản khi đang bán, định dạng tình trạng chỗ, chọn ghế, số liệu bán vé |
| Chặn P6 | DR-46, DR-55–60 | Tham số rate limit, phòng chờ, token vào cửa, backpressure |
| Chặn P7 | DR-78 | Dữ liệu và kịch bản demo |

Mục lệch SDD gốc (⚠): DR-02, DR-08, DR-09, DR-10, DR-17, DR-20, DR-21, DR-26, DR-28, DR-31, DR-41, DR-43, DR-44, DR-53, DR-57, DR-73.
Mục cần spike (🔬): DR-02 (S-01), DR-13 (S-02), DR-27 (S-03), DR-39 (S-04), DR-35 (S-05), DR-75 (S-06).
