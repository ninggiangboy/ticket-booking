# Thực nghiệm: giao thức chung

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-70
> Phụ thuộc: SDD gốc §3.4, §10.3, §14.3, §15, [Sổ quyết định](../../00-decision-register.md) (DR-42, 45, 51, 55, 61, 73, 75, 76, 78, 152, 158, 180), [DOC-03](../../01-product/requirements.md) (NFR-01…04), [DOC-04](../../01-product/use-cases.md) UC-21, [DOC-06](../../02-glossary.md), [DOC-10](../../03-architecture/quality-attributes.md) §2, §4, §5, §7, §8, [DOC-11](../../03-architecture/tech-stack-and-versions.md), [DOC-07](../../03-architecture/system-context-and-containers.md), [DOC-12](../../03-architecture/code-architecture.md) §1, [DOC-15](../../05-data/ops-model.md) (`session`, `inventory_pool_counter`), [DOC-24](../../06-design/inventory-and-reservation.md) §2, §5, [DOC-30](../../06-design/invariant-checker.md) §5, §6, [DOC-33](../../06-design/observability.md) §4, §5, [DOC-34](../../06-design/configuration-reference.md), [DOC-37](../../07-api/api-endpoints.md) (E-06, E-14, E-16, E-18, E-25, E-29, E-34), [DOC-61](../../09-operations/local-dev.md), [DOC-62](../../09-operations/deploy-compose.md) §4, §9, [DOC-69](../test-strategy.md), [RB-02](../../09-operations/runbooks/RB-02-invariant-violation.md), [DOC-82](../../06-design/flows/README.md) (FL-19)
> Người dùng chính: P0-07 (S-06), P2-13 (hạ tầng thực nghiệm), P2-14, P3-10, P4-10, P5-07, P6-09, P6-10 (chạy EXP), tác giả DOC-71…80, DOC-86 (FL-19)

Tài liệu là giao thức chung của mọi thực nghiệm `EXP-01…10`: máy và giới hạn tài nguyên, mô hình tải, seed người dùng, cấu hình, quy trình `make exp`, công thức số đo, quy tắc hợp lệ, cách lưu kết quả và mẫu bảng kết quả. Mỗi tài liệu `EXP-xx` (DOC-71…80) chỉ ghi phần riêng (giả thuyết, tham số kịch bản, tiêu chí đạt) và dẫn về đây cho phần còn lại. Thiết kế kho vé ở DOC-24, lệnh kiểm tra bất biến ở DOC-30, metric ở DOC-33, compose ở DOC-62, luồng qua từng tầng ở FL-19 (DOC-86). Mọi con số ở đây là **kế hoạch** (planned) trừ khi ghi rõ là số đo kèm ngày, máy và git SHA.

Mọi quyết định mới trong tài liệu được ghi thành DR-204…213 và tóm tắt ở mục "Quyết định phát sinh khi viết tài liệu này".

## 1. Nguyên tắc

1. **Sự thật nền là database.** Tính đúng (bán vượt, ghế hai chủ, vé không có tiền) chỉ đo bằng SQL trên PostgreSQL và `make invariants` (DOC-30); số đếm của k6 hay metric chỉ để đối chiếu (SDD gốc §15).
2. **Mỗi lần chạy kết thúc bằng `make invariants`** (FR-12.4, DR-73). Quy tắc đọc kết quả theo chiến lược ở §9.2.
3. **3 lần lặp, báo trung vị và min–max** cho số đo hiệu năng; số đo tính đúng báo **cả 3 giá trị** (DR-75). Mỗi lần lặp bắt đầu từ volume trống (§7.2 bước 2).
4. **Cùng một máy, cùng giới hạn container** cho mọi lần chạy được so với nhau (§3); cấu hình máy, git SHA và giới hạn thật ghi vào `env.json` của lần chạy (§10).
5. **Bản ngây thơ đi kèm.** Thực nghiệm về đồng thời chạy thêm trên `inventory.strategy=naive` để cho thấy lỗi xuất hiện khi thiếu cơ chế (SDD gốc §15.2, DR-76).
6. **Không sửa con số kế hoạch ở tài liệu khác.** Số đo ghi vào §Kết quả của EXP và bảng số đo DOC-10 §8.

## 2. Danh mục thực nghiệm

| EXP | Tài liệu | Câu hỏi | Mô hình tải (§4.2) | Chiến lược chạy | Task chạy | Gate |
| --- | --- | --- | --- | --- | --- | --- |
| EXP-01 | DOC-71 `EXP-01-single-seat-contention.md` | 10.000 request đồng thời giữ ghế A1: đúng 1 thắng? | M-B (một ghế) | `skip-locked`, `naive` | P5-07 | P5 |
| EXP-02 | DOC-72 `EXP-02-pool-contention.md` | 100.000 người dùng ảo, pool 5.000, 1–4 vé mỗi request: không vượt 5.000, bán hết? | M-B ở P2; M-Q ở P6 (DR-204) | `skip-locked`, `naive` | P2-14 (M-B), P6-09 (M-Q) | P2 |
| EXP-03 | DOC-73 `EXP-03-idempotency.md` | Gửi lại cùng key tuần tự và song song (giữ, hủy giữ, nhận vé 0 đồng): 1 tác dụng mỗi key? | M-R (gửi lại) | `skip-locked` | P2-14 | P2 |
| EXP-04 | DOC-74 `EXP-04-expiry-under-load.md` | 5.000 reservation hết hạn cùng lúc, dừng API giữa chừng: trả hết ≤ 30 giây (NFR-03), 0 trả nhầm? | M-E (hết hạn) | `skip-locked` | P2-14 | P2 |
| EXP-05 | DOC-75 `EXP-05-admission-and-connection-pool.md` | Tăng `admit_rate`: p95, connection, điểm gãy; hiệu chỉnh DR-57, 60, 61 | M-Q | `skip-locked` | P6-09 | P6 |
| EXP-06 | DOC-76 `EXP-06-confirm-vs-expire-race.md` | Webhook ±2 giây quanh `expires_at`; tạo PaymentIntent đua với expire (DR-47) | M-E + cổng giả (DR-51) | `skip-locked` | P3-10 | P3 |
| EXP-07 | DOC-77 `EXP-07-duplicate-out-of-order-webhooks.md` | Mỗi webhook phát lại 3 lần, xáo thứ tự: đúng một bộ vé mỗi đơn? | Cổng giả (DR-51) | `skip-locked` | P3-10 | P3 |
| EXP-08 | DOC-78 `EXP-08-fault-injection.md` | Dừng API giữa transaction, khởi động lại PostgreSQL, tắt Redis khi có tải | M-Q + sự cố | `skip-locked` | P6-10 | P6 |
| EXP-09 | DOC-79 `EXP-09-editor-performance.md` | fps editor ở 1.000/5.000/10.000/20.000 ghế (DR-39) | Không dùng k6 (trình duyệt) | — | P4-10 | P4 |
| EXP-10 | DOC-80 `EXP-10-unit-rows-vs-counter.md` | Kịch bản EXP-02 trên `skip-locked` và `counter`: thông lượng, p95, chờ lock | M-B | `skip-locked`, `counter` | P2-14 | P2 |

EXP-09 đo phía trình duyệt nên chỉ dùng §3.1 (máy), §8.3 (tổng hợp), §10, §11; phần k6, seed và `make invariants` không áp dụng. Spike S-06 (P0-07) dùng §3, §4.3 và chạy trước P2-00 (DR-152).

## 3. Máy và giới hạn tài nguyên

### 3.1 Máy thực nghiệm

| Hạng mục | Yêu cầu | Ghi vào `env.json` bằng |
| --- | --- | --- |
| Hệ điều hành | Linux x86_64 hoặc arm64, Docker Engine chạy trực tiếp (không VM của Docker Desktop) | `uname -srm`, `docker version --format '{{.Server.Version}}'` |
| CPU | ≥ 8 nhân vật lý (planned; DOC-10 §7) | `lscpu` (`Model name`, `CPU(s)`, `Thread(s) per core`) |
| RAM | ≥ 16 GB; swap không được dùng trong lúc chạy | `free -b`; `vmstat 5` cột `si`, `so` = 0 (§9.1) |
| Đĩa | SSD NVMe cho volume Docker | `df -h /var/lib/docker`, `lsblk -d -o NAME,ROTA,MODEL` |
| Nguồn và nhiệt | Cắm điện; governor `performance` nếu có | `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` (nếu tồn tại) |
| Tải khác | Không trình duyệt, IDE build, sync đám mây chạy song song | `uptime` trước khi chạy: load 1 phút < 1,0 |

Máy dev macOS (OrbStack, Docker Desktop) chạy được `make exp … SCALE=0.01` để thử script (P2-13), nhưng số đo của nó **không** được ghi vào §Kết quả của EXP.

### 3.2 Giới hạn container

Mọi container chạy với giới hạn cố định trong `deploy/compose/docker-compose.experiment.yml` (DOC-62 §9, DR-124). Bảng dưới là giới hạn của giao thức; nó bằng giá trị khởi điểm của DOC-10 §7 cộng container `k6` (DR-205). **Trạng thái: planned, chờ S-06** (DR-75, DR-152). Khi S-06 đổi một con số, sửa bảng này cùng DOC-10 §7 và DOC-62 §9 trong một thay đổi, ghi ngày ở header.

| Container | CPU (trần) | RAM (trần) | Tham số đáng chú ý |
| --- | --- | --- | --- |
| `postgres` | 4 | 6 GB | Tham số ở DOC-62 §9: `max_connections=100`, `shared_buffers=2GB`, `synchronous_commit=on`, `pg_stat_statements`, `track_io_timing=on` |
| `api` | 4 | 3 GB | `-XX:MaxRAMPercentage=75`; Hikari 40; bulkhead 24 (DR-61) |
| `redis` | 1 | 512 MB | `maxmemory 384mb`, `noeviction` |
| `nginx` | 0,5 | 128 MB | `limit_req` của DR-55; IP của `k6` được miễn (§3.3) |
| `storage` | 1 | 512 MB | Gần như không dùng |
| `mailpit` | 0,5 | 128 MB | `MP_MAX_MESSAGES=5000` |
| `prometheus` | 1 | 512 MB | Scrape `api:9090` mỗi 5 giây (DR-206) |
| `k6` | 3 | 4 GB | Image `grafana/k6` khóa tag ở S-06 (DOC-11) |
| **Cộng trần** | **15** | **≈ 14,8 GB** | |

Trần CPU là quota (CFS), không phải nhân dành riêng; tổng trần lớn hơn số nhân của máy và các container không cùng lúc dùng hết (DOC-10 §7). Để phát hiện máy sinh tải bị nghẽn, `docker stats` được lấy mẫu mỗi 5 giây trong cửa sổ đo (§7.2 bước 7) và quy tắc §9.1 loại lần chạy khi `k6` chạm trần. Grafana không chạy trong thực nghiệm P2 (dashboard dựng ở P6-08); profile `obs` của `make exp` chỉ bật `prometheus` cho tới khi đó.

### 3.3 Mạng và allowlist rate limit

`k6` là service của `docker-compose.experiment.yml` (profile compose `exp`), nằm trên mạng mặc định của project với **subnet cố định `172.28.0.0/16`** và **IP cố định `172.28.0.250`**; file đặt `RATE_LIMIT_ALLOWLIST=172.28.0.250/32` cho `nginx` (DR-55, DR-75, DR-205). k6 gọi `http://nginx:80` (cổng trong container của `nginx`, DOC-62 §8) như trình duyệt đi qua reverse proxy thật; không gọi thẳng `api:8081`.

Ngoài `limit_req` của nginx, thực nghiệm **không** tắt cơ chế nào của ứng dụng: bulkhead, `statement_timeout`, `lock_timeout`, token bucket theo người dùng (từ P6, DR-56) vẫn chạy với giá trị mặc định trừ khi tài liệu EXP ghi đè rõ (§6.2).

## 4. Mô hình tải

### 4.1 Người dùng ảo và VU đa người dùng (DR-75)

"Người dùng đồng thời" là người dùng có mặt trong cùng cửa sổ mở bán (NFR-02, DR-75). k6 không chạy 100.000 VU riêng lẻ; mỗi VU quản lý một **nhóm người dùng ảo**, mỗi người dùng ảo là một chuỗi bất đồng bộ (`http.asyncRequest` + `setTimeout` của `k6/timers`) chờ tới thời điểm kế hoạch của mình rồi gửi request. Cookie và CSRF của người dùng ảo `n` suy ra được từ `n` (§5.1), nên k6 không cần đăng nhập.

Thư viện dùng chung `load/lib/multiuser.ts` (P2-13, thư mục `load/` theo DOC-12 §1):

```ts
// load/lib/multiuser.ts — khung; hành vi cụ thể của từng EXP ở load/expNN.ts
import http from 'k6/http';
import { setTimeout } from 'k6/timers';
import { sha256 } from 'k6/crypto';
import { b64encode } from 'k6/encoding';
import { Trend, Counter } from 'k6/metrics';

export const scheduleLag = new Trend('exp_schedule_lag', true);      // ms: gửi thật − kế hoạch
export const outcome = new Counter('exp_http_outcome');               // tag: name, status, code

export interface VirtualUser { n: number; cookie: string; csrf: string }

export function user(n: number): VirtualUser {                        // khớp experiments/seed/users.sql
  return {
    n,
    cookie: b64encode(sha256(`exp-session-${n}`, 'binary'), 'rawurl'),
    csrf: b64encode(sha256(`exp-csrf-${n}`, 'binary'), 'rawurl'),
  };
}

const delay = (ms: number) => new Promise<void>((r) => setTimeout(r, ms));

/** Chờ tới thời điểm kế hoạch (epoch ms) rồi ghi độ trễ lập lịch. */
export async function at(plannedMs: number): Promise<void> {
  const wait = plannedMs - Date.now();
  if (wait > 0) await delay(wait);
  scheduleLag.add(Date.now() - plannedMs);
}

/** VU thứ `vu` (1-based) nhận người dùng n ≡ vu (mod vus), n = 1…users; n = 0 là người tổ chức. */
export function shard(vu: number, vus: number, users: number): VirtualUser[] {
  const out: VirtualUser[] = [];
  for (let n = vu; n <= users; n += vus) out.push(user(n));
  return out;
}

export function headers(u: VirtualUser, name: string, idemKey?: string) {
  return {
    headers: {
      Cookie: `tb_session=${u.cookie}`,
      'X-CSRF-Token': u.csrf,
      Origin: __ENV.APP_BASE_URL,                                       // OriginCheckFilter (DR-117)
      'Content-Type': 'application/json',
      'Accept-Language': 'vi',
      ...(idemKey ? { 'Idempotency-Key': idemKey } : {}),
    },
    tags: { name },                                                     // hold, cancel, confirm_free, poll…
    timeout: '10s',
  };
}

export function record(res: http.RefinedResponse<'text'>, name: string): void {
  const code = res.status >= 400 ? (res.json('code') as string | undefined) ?? 'NONE' : 'NONE';
  outcome.add(1, { name, status: String(res.status), code });          // status 0 = lỗi mạng hoặc timeout
}
```

Executor của mọi kịch bản M-B, M-R, M-E: `per-vu-iterations`, `iterations: 1`, `vus: K`, `maxDuration` theo EXP; mỗi VU chạy `await Promise.all(shard(...).map(behavior))`. Hành vi của người dùng ảo khi nhận lỗi tạm thời (áp dụng cho mọi EXP trừ khi EXP ghi khác):

| Response | Người dùng ảo làm | Lý do |
| --- | --- | --- |
| 503 `OVERLOADED`, 429 `RATE_LIMITED` | Chờ `Retry-After` giây (thiếu header → 1 giây), gửi lại **cùng** `Idempotency-Key` và cùng body; tối đa 10 lần | Hành vi client của DR-45, DR-61; SDD gốc §10.5 |
| Lỗi mạng hoặc timeout 10 giây (`status 0`) | Như 503, cùng key | Mất response sau commit vẫn an toàn nhờ idempotency (DR-45) |
| 409, 422 | Dừng hoặc làm theo kịch bản EXP | Kết quả nghiệp vụ, không thử lại mù |
| 5xx khác | Ghi nhận, dừng | Đếm vào "5xx khác" (§8.2 M7) |

`K` (số VU) chọn theo S-06 (§4.3); planned: `K = 200`, tức 500 người dùng ảo mỗi VU ở 100.000.

### 4.2 Bốn họ kịch bản

| Mã | Tên | Dùng ở | Mô tả |
| --- | --- | --- | --- |
| M-B | Mở bán dồn, không phòng chờ | EXP-01, EXP-02 (P2), EXP-10 | Sự kiện mở bán ở `T0` (§5.2). Người dùng ảo `n` đến ở `T0 + a_n`, `a_n` phân phối đều trong `[0, W]` (planned `W = 60 giây`), gửi một lệnh giữ vé E-14 rồi làm theo kịch bản (nhận vé 0 đồng E-18, hủy E-16 hoặc dừng). Không có phòng chờ vì `admission` chỉ có từ P6; tầng chặn trước database là bulkhead và 503 (DR-61) |
| M-Q | Qua phòng chờ | EXP-02 (P6), EXP-05, EXP-08 | Người dùng ảo vào hàng E-10, hỏi vị trí E-11 theo `retryAfterSeconds` (3/10/30 giây, DR-59; DOC-10 §4.1), giữ vé khi được cấp lượt. Đặc tả chi tiết ở DOC-75 |
| M-R | Gửi lại | EXP-03 | Mỗi người dùng ảo gửi lại cùng request với cùng key theo nhịp kịch bản, tuần tự hoặc song song (DOC-73) |
| M-E | Hết hạn đồng loạt | EXP-04, EXP-06 | Tạo N reservation trong cửa sổ ngắn với `reservation.hold-duration` rút ngắn (§6.2), rồi quan sát job trả vé (DOC-74) |

Tỉ lệ người/vé giữ 20:1 (100.000 / 5.000) ở mọi mức `SCALE` (NFR-02).

### 4.3 S-06: trần người dùng ảo của một máy (DR-75, DR-152)

S-06 đo trần của **máy sinh tải**, không đo ứng dụng. Phòng chờ chưa có ở thời điểm chạy (trước P2-00), nên S-06 dùng nhịp hỏi của mô hình M-Q trên một endpoint rẻ có sẵn từ P1: E-06 `GET /api/v1/events` (nginx cache 5 giây, DR-86), cùng chi phí phía nginx như hỏi vị trí (DR-213). Mã spike ở `spikes/s06/` (như `spikes/s01/`), dùng lại `load/lib/multiuser.ts` khi P2-13 có.

1. `make up` với `docker-compose.experiment.yml` (giới hạn §3.2); không cần seed.
2. Mỗi người dùng ảo hỏi E-06 với chu kỳ theo nhóm của DOC-10 §4.1: 0,2 % chu kỳ 3 giây, 1,8 % chu kỳ 10 giây, 98 % chu kỳ 30 giây; pha đầu ngẫu nhiên đều trong chu kỳ. Ở 100.000: kế hoạch ≈ 3.513 request/giây.
3. Chạy 5 phút ở N = 25.000, 50.000, 75.000, 100.000 (dừng ở mức đầu tiên không đạt).
4. Một mức **đạt** khi cả bốn: `exp_schedule_lag` p95 < 1.000 ms; nhịp request thực ≥ 95 % kế hoạch; CPU của `k6` < 90 % trần (§3.2) ở p95 các mẫu `docker stats`; RSS của `k6` < 3,5 GB và không bị OOM.
5. Trần = mức đạt lớn nhất. Ghi `N`, `K`, CPU và RAM của k6 vào DOC-10 §8 và nhật ký chốt (DR-75). Trần < 100.000 → DR mới hiệu chỉnh NFR-02 và giữ 20:1 (DR-75); các EXP dùng `SCALE = trần / 100.000`.

## 5. Dữ liệu

### 5.1 Người dùng và session: `experiments/seed/users.sql` (DR-75)

Script tạo người dùng `n = 0…USERS` (`n = 0` là người tổ chức, có dòng `organizer`) với session có giá trị biết trước (DR-207). Giá trị cookie và CSRF của `n` là hàm băm của chuỗi cố định, nên k6 tự tính được (§4.1); DB chỉ lưu SHA-256 của session ID thô như session thật (DOC-15, DR-22).

| Trường | Giá trị cho người dùng `n` |
| --- | --- |
| `app_user.email` | `exp-<n 6 chữ số>@load.test`, ví dụ `exp-000042@load.test` |
| Session ID thô (32 byte) | `sha256('exp-session-<n>')` |
| Cookie `tb_session` | base64url không đệm của session ID thô (43 ký tự) |
| `session.session_hash` | `sha256(session ID thô)` |
| `session.csrf_token` | base64url không đệm của `sha256('exp-csrf-<n>')` |

```sql
-- experiments/seed/users.sql — psql -v ON_ERROR_STOP=1 -v users=100000 -f …   (DR-75)
-- Chỉ chạy khi api đã khởi động với profile experiment: bảng của db/migration-experiment phải có.
DO $$
BEGIN
  IF to_regclass('public.inventory_pool_counter') IS NULL THEN
    RAISE EXCEPTION 'users.sql requires the experiment profile (inventory_pool_counter is missing)';
  END IF;
END $$;

INSERT INTO app_user (email, locale)
SELECT format('exp-%s@load.test', lpad(n::text, 6, '0')), 'vi'
FROM generate_series(0, :users) AS n
ON CONFLICT (email) DO NOTHING;

INSERT INTO session (session_hash, user_id, csrf_token)
SELECT sha256(sha256(convert_to('exp-session-' || n, 'UTF8'))),
       u.user_id,
       translate(rtrim(encode(sha256(convert_to('exp-csrf-' || n, 'UTF8')), 'base64'), '='), '+/', '-_')
FROM generate_series(0, :users) AS n
JOIN app_user u ON u.email = format('exp-%s@load.test', lpad(n::text, 6, '0'))
ON CONFLICT (session_hash) DO NOTHING;

INSERT INTO organizer (owner_user_id, name)
SELECT user_id, 'EXP Organizer' FROM app_user WHERE email = 'exp-000000@load.test'
ON CONFLICT (owner_user_id) DO NOTHING;

ANALYZE app_user;
ANALYZE session;
```

Kiểm tra sau khi chạy (`USERS = 100000`): `SELECT count(*) FROM session s JOIN app_user u USING (user_id) WHERE u.email LIKE 'exp-%@load.test'` trả `100001`. Thời gian kế hoạch: < 15 giây. Các session này là bí mật đã biết; chúng chỉ tồn tại trên volume của máy thực nghiệm và bị xóa ở bước reset của lần chạy sau (§7.2 bước 2).

### 5.2 Sự kiện thử: `load/fixture.ts`

Sự kiện được tạo **qua API thật** bằng người tổ chức `n = 0`, để kho vé sinh đúng như đường xuất bản (DR-27; cùng lý do `make seed` không `INSERT` trực tiếp, DR-123). `fixture.ts` chạy một lần trước kịch bản chính (DR-208):

1. E-25 tạo sự kiện đo: `timezone = "Asia/Ho_Chi_Minh"`, `startsAt = now + 2 ngày`, `saleStartsAt = T0 = now + 120 giây` (làm tròn lên phút), `saleEndsAt = now + 1 ngày`.
2. E-34 thêm loại vé theo kịch bản EXP; mặc định một loại `GA` giá `0`, `capacity = CAPACITY × SCALE` (EXP-02: 5.000). Giá 0 để không cần thanh toán ở P2; từ P3, EXP có giá > 0 dùng cổng giả (`PAYMENTS_MODE=fake`, DR-51).
3. E-29 xuất bản.
4. Tạo sự kiện khởi động (cùng cách, một loại `GA` 10.000 vé, `saleStartsAt = now + 20 giây`). Từ `now + 25 giây`, 1.000 người dùng ảo đầu (`n = 1…1000`) mỗi người giữ 1 vé (E-14) rồi hủy ngay (E-16), trong 60 giây: làm nóng JIT, pool Hikari, cache session của nhóm này. Số đo của pha này bị loại khỏi cửa sổ đo (§8.1).
5. Ghi `fixture.json` vào thư mục kết quả: `{ "eventId", "ticketTypeIds": [...], "poolIds": [...], "saleStartsAt", "warmupEventId" }`; `poolIds` lấy bằng `SELECT pool_id FROM inventory_pool WHERE ticket_type_id = ANY(:ids)`.

## 6. Cấu hình

### 6.1 Cấu hình cố định của mọi thực nghiệm

| Hạng mục | Giá trị | Nguồn |
| --- | --- | --- |
| Profile Spring của `api` | `dev,fake-payments,experiment,obs` | DOC-62 §9, DOC-34 §2 |
| `PAYMENTS_MODE` | `fake` (khớp profile `fake-payments`) | DR-51, DR-75, DR-125 |
| `INVENTORY_STRATEGY` | `skip-locked` (mặc định), `counter` hoặc `naive` theo tham số `STRATEGY` | DR-76, DOC-34 §3 |
| `logging.level.io.ticket` | `WARN` (profile `experiment`) | DOC-34 |
| `RATE_LIMIT_ALLOWLIST` | `172.28.0.250/32` (IP của `k6`) | §3.3 |
| Mailpit | Nhận email vé thật qua outbox (≈ 2.000 thư ở EXP-02) | DOC-62 §4 |
| Giờ | Mọi container dùng đồng hồ của host; `T0`, `T1` ghi bằng epoch giây UTC | — |

### 6.2 Ghi đè theo EXP

Mỗi EXP có file `experiments/expNN/params.env` (P2-13), được `run.sh` đọc. Khóa Spring ghi đè bằng `API_OVERRIDES` (chuyển thành `SPRING_APPLICATION_JSON` của `api`), không sửa `application.yml`:

```bash
# experiments/exp02/params.env (ví dụ; giá trị thật do DOC-72 chốt)
USERS=100000            # nhân với SCALE
CAPACITY=5000           # nhân với SCALE
ARRIVAL_WINDOW_S=60     # W của M-B
VUS=200                 # K, theo S-06
MAX_DURATION_S=600
DRAIN_S=40              # 2 × reservation.expiry.interval + reservation.expiry.lease (DOC-30 §6)
API_OVERRIDES='{}'      # ví dụ EXP-04: '{"reservation":{"hold-duration":"PT2M"}}'
```

Chỉ khóa có trong DOC-34 với cột Profile ghi `experiment` (hoặc ghi rõ "EXP được ghi đè") mới được đặt ở `API_OVERRIDES`; giá trị đã dùng được chép vào `env.json`.

### 6.3 Ba bản cài đặt kho vé (DR-76)

| `STRATEGY` | Bản | Mục đích trong thực nghiệm | Bất biến mong đợi (§9.2) |
| --- | --- | --- | --- |
| `skip-locked` | `SkipLockedClaimer` (mặc định) | Bản thật; mọi chỉ tiêu NFR đo trên bản này | Thoát 0 |
| `counter` | `CounterClaimer` (phương án A, SDD gốc §10.3; bảng `inventory_pool_counter`, DOC-15 §8.2) | Baseline thông lượng và chờ lock của EXP-10 | Thoát 0 với kiểm tra theo bộ đếm (DR-210) |
| `naive` | `NaiveClaimer` (đọc rồi ghi không điều kiện) | Bằng chứng bán vượt khi thiếu cơ chế (EXP-01, EXP-02) | **Thoát 1** với INV-05 (hoặc INV-01) (DOC-30 §6, IC-25) |

`api` từ chối khởi động với `counter`/`naive` nếu thiếu profile `experiment` (DR-76, CFG-05); `run.sh` không tự thêm profile, nó dùng đúng file `docker-compose.experiment.yml`.

## 7. Quy trình

### 7.1 Giao diện lệnh

```bash
make exp EXP=02                                   # 3 lần, skip-locked, SCALE=1
make exp EXP=02 STRATEGY=naive RUNS=1             # bản ngây thơ, một lần
make exp EXP=10 STRATEGY=counter
make exp EXP=02 SCALE=0.01 RUNS=1                 # thử nhanh trên máy dev (P2-13); không ghi vào §Kết quả
```

| Tham số | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `EXP` | (bắt buộc) | `01…10` hoặc `S06` |
| `STRATEGY` | `skip-locked` | `inventory.strategy` (§6.3) |
| `SCALE` | `1` | Nhân `USERS` và `CAPACITY`; `0 < SCALE ≤ 1` |
| `RUNS` | `3` | Số lần lặp (DR-75) |

`make exp` gọi `experiments/run.sh "$EXP" "$STRATEGY" "$SCALE" "$RUNS"` (DOC-61). Biến `EXPCOMPOSE` trong script là `docker compose --env-file deploy/compose/.env -f deploy/compose/docker-compose.yml -f deploy/compose/docker-compose.experiment.yml`.

### 7.2 Các bước của một lần chạy

`run.sh` lặp `RUNS` lần các bước dưới; một bước lỗi dừng lần chạy, ghi `status: "ERROR"` và lý do vào `env.json`, rồi sang lần sau.

1. **Điều kiện trước.** `git status --porcelain` rỗng (cây làm việc sạch); `uptime` load 1 phút < 1,0; ghi `env.json` phần máy (§3.1) và `git rev-parse HEAD`.
2. **Reset.** `$EXPCOMPOSE --profile obs --profile exp down -v` (xóa volume database, storage, Mailpit, Prometheus).
3. **Khởi động.** `INVENTORY_STRATEGY=$STRATEGY SPRING_APPLICATION_JSON="$API_OVERRIDES" $EXPCOMPOSE --profile obs up -d --build --wait`. Ghi `docker compose config` (đã thế biến) và `SHOW ALL` của PostgreSQL vào `env.json`.
4. **Seed người dùng.** `$EXPCOMPOSE exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -v users='"$USERS"' -U "$POSTGRES_USER" "$POSTGRES_DB"' < experiments/seed/users.sql`.
5. **Fixture và khởi động JIT.** `$EXPCOMPOSE --profile exp run --rm k6 run /load/fixture.ts --out json=/results/raw/fixture.ndjson` → `fixture.json` (§5.2).
6. **Chờ `T0 − 5 giây`**, rồi `SELECT pg_stat_statements_reset(); SELECT pg_stat_reset();` và ghi `pg_stat_database` (lần chụp đầu).
7. **Bật bộ lấy mẫu** chạy nền tới bước 9: `experiments/lib/sample-docker.sh` (`docker stats --no-stream` mỗi 5 giây → `raw/docker-stats.csv`) và `experiments/lib/sample-pg.sh` (mỗi 1 giây → `raw/pg-activity.csv`, câu ở §8.2 M11).
8. **Tải.** `$EXPCOMPOSE --profile exp run --rm k6 run /load/expNN.ts -e FIXTURE=/results/fixture.json --summary-export=/results/k6-summary.json`. Khi k6 thoát, `T1` = epoch giây hiện tại.
9. **Xả.** Chờ `DRAIN_S` giây (mặc định 40, DOC-30 §6), cộng 10 giây (hai nhịp scrape); dừng bộ lấy mẫu; chụp `pg_stat_database` lần hai.
10. **Thu số đo.** `experiments/lib/collect.sh` chạy các truy vấn PromQL ở `http://localhost:9091/api/v1/query` và SQL ở §8.2, ghi `metrics.json`.
11. **Kiểm tra bất biến.** `$EXPCOMPOSE run --rm -e INVENTORY_STRATEGY=skip-locked api --spring.profiles.active=invariants > invariants.json; echo $? > invariants.exit` (DOC-30 §5; DR-211). Mã 2 → chạy lại một lần sau 10 giây.
12. **Phân loại** lần chạy theo §9.1 và §9.2, ghi `verdict` vào `env.json`.

Sau `RUNS` lần: `experiments/aggregate.sh EXP-NN` gộp các lần hợp lệ thành `summary.md` (§11) và in bảng ra stdout (P2-13: "in bảng kết quả").

### 7.3 Thời gian dự kiến (planned)

| Bước | Thời gian |
| --- | --- |
| 2–3 reset và khởi động (image đã build) | ≈ 60 giây |
| 4 seed 100.000 người dùng | < 15 giây |
| 5 fixture và khởi động JIT, tới `T0` | ≈ 120 giây |
| 8 tải | theo EXP (EXP-02 M-B: W 60 giây + đuôi thử lại ≤ 60 giây) |
| 9–11 xả, thu, bất biến | ≈ 70 giây |
| **Một lần EXP-02** | **≈ 7 phút**; 3 lần ≈ 21 phút |

## 8. Số đo và công thức

### 8.1 Cửa sổ đo

- `T0` = `saleStartsAt` của sự kiện đo (M-B, M-R, M-E) hoặc lúc mở hàng (M-Q); `T1` = lúc k6 kết thúc (§7.2 bước 8); `Δ = T1 − T0` (giây).
- Truy vấn Prometheus đánh giá ở `Te = T1 + DRAIN_S + 10` với cửa sổ `R = Te − T0` giây. Sau `T1` không còn tải nên phần đuôi chỉ cộng hoạt động nền (job trả vé, outbox); số đo nào bị ảnh hưởng bởi đuôi thì EXP phải ghi.
- k6: chỉ request gắn tag `name` của kịch bản chính được tính; pha khởi động có tag `name: warmup_*` và chạy trong process k6 khác (§7.2 bước 5).

### 8.2 Danh mục số đo

| # | Đại lượng | Đơn vị | Nguồn | Công thức |
| --- | --- | --- | --- | --- |
| M1 | p95 lệnh giữ vé phía client | ms | k6 | `metrics["http_req_duration{name:hold}"].values["p(95)"]` trong `k6-summary.json`. Script khai `summaryTrendStats: ['avg','med','p(90)','p(95)','p(99)','max']` và một threshold giả `'http_req_duration{name:hold}': ['p(95)<600000']` để submetric có trong summary. Chỉ gồm request có response (status ≠ 0) |
| M2 | p95 lệnh giữ vé phía API | ms | Prometheus | `1000 × histogram_quantile(0.95, sum by (le) (increase(ticket_hold_duration_seconds_bucket[R])))` (DOC-33 §4.2) |
| M3 | Thông lượng giữ thành công | lệnh/giây | SQL + Prometheus | `S / (t_end − T0)`, `S = sum(increase(ticket_hold_result_total{result="success"}[R]))`, `t_end` = `t_soldout` (M4) nếu bán hết, ngược lại `T1` |
| M3b | Nhịp request giữ vé | request/giây | k6 | `metrics["http_reqs{name:hold}"].values.count / Δ` (gồm cả gửi lại) |
| M4 | Thời gian bán hết | giây | SQL | `SELECT extract(epoch FROM max(r.created_at) - :t0) FROM reservation r WHERE r.event_id = :e AND r.status IN ('ACTIVE','EXPIRING','CONFIRMED')`, chỉ tính khi M5 = sức chứa |
| M5 | Unit đã giữ và bán | unit | SQL | `skip-locked`, `naive`: `SELECT count(*) FROM inventory_unit WHERE pool_id = ANY(:pools) AND status IN ('HELD','SOLD')`; `counter`: `SELECT sum(held + sold) FROM inventory_pool_counter WHERE pool_id = ANY(:pools)` |
| M6 | Vé bán vượt | vé | SQL | `SELECT greatest(0, coalesce(sum(ri.quantity), 0) - p.capacity) FROM inventory_pool p LEFT JOIN reservation_item ri ON ri.pool_id = p.pool_id LEFT JOIN reservation r ON r.reservation_id = ri.reservation_id AND r.status IN ('ACTIVE','EXPIRING','CONFIRMED') WHERE p.pool_id = ANY(:pools) AND (ri.reservation_item_id IS NULL OR r.reservation_id IS NOT NULL) GROUP BY p.pool_id, p.capacity`, cộng mọi pool (DOC-24 §5). Đo ngay sau `T1`, trước bước xả |
| M7 | 5xx khác `OVERLOADED` | request | k6 | Σ `exp_http_outcome` có `status` 500–599 trừ (`status = 503` và `code = OVERLOADED`); kèm số `status = 0` riêng. NFR-02 "không sập" ⇔ M7 = 0 |
| M8 | Tỉ lệ 503 `OVERLOADED` | % | k6 | `exp_http_outcome{name:hold,status:503,code:OVERLOADED} / http_reqs{name:hold}` |
| M9 | Thời gian giữ connection | ms | Prometheus | p95: `1000 × histogram_quantile(0.95, sum by (le) (increase(ticket_connection_hold_seconds_bucket{op="hold"}[R])))`; trung bình: `1000 × sum(increase(ticket_connection_hold_seconds_sum{op="hold"}[R])) / sum(increase(ticket_connection_hold_seconds_count{op="hold"}[R]))` |
| M10 | Connection đang dùng, đang chờ (tối đa) | connection | Prometheus | `max_over_time(sum(hikaricp_connections_active)[R:5s])`, `max_over_time(sum(hikaricp_connections_pending)[R:5s])` |
| M11 | Thời gian chờ lock của database | connection·giây | `pg_stat_activity` mỗi 1 giây | Mỗi mẫu: `SELECT count(*) FROM pg_stat_activity WHERE datname = current_database() AND wait_event_type = 'Lock'`; M11 = Σ mẫu × 1 giây trong `[T0, T1]`. Kèm `deadlocks` = hiệu `pg_stat_database.deadlocks` giữa hai lần chụp |
| M12 | CPU theo container | % một nhân | `docker stats` mỗi 5 giây | p95 của `CPUPerc` từng container trong `[T0, T1]` |
| M13 | Độ trễ lập lịch của k6 | ms | k6 | `metrics.exp_schedule_lag.values["p(95)"]` |
| M14 | Bất biến | — | `make invariants` | Mã thoát và `violations[].id` của `invariants.json` (DOC-30 §5.2) |

Chiến lược `counter` không đổi trạng thái unit pool, nên M5 đọc bộ đếm; M6 vẫn tính từ `reservation_item` cho cả ba bản để so được với nhau. Thông lượng và p95 theo endpoint khác E-14 (E-16, E-18, E-11) dùng cùng công thức với tag `name` và metric `http_server_requests_seconds` (DOC-33 §4.1) thay cho `ticket_hold_*`; EXP ghi rõ biến thể nó dùng.

Ghi chú độ chính xác: `histogram_quantile` nội suy tuyến tính trong bucket (DOC-33 §4.2: mốc 0,25 và 0,5 giây), nên M2 và M9 chỉ chính xác tới độ rộng bucket; M1 là phân vị chính xác trên mọi mẫu của k6. Khi M1 và M2 lệch nhau hơn 2 lần, ghi vào "Phân tích" của EXP.

### 8.3 Tổng hợp ba lần lặp

- Số đo hiệu năng (M1–M3b, M4, M8–M13): `trung vị (min–max)` của các lần **hợp lệ**, làm tròn: ms và giây tới số nguyên, phần trăm 1 chữ số thập phân. Ví dụ: `p95 k6 = 182 (171–203) ms`.
- Số đo tính đúng (M5, M6, M7, M14): ghi **cả ba** giá trị, ví dụ `bán vượt 0 / 0 / 0`; không lấy trung vị.
- Ít hơn 3 lần hợp lệ → chạy bù tới đủ 3, tối đa 5 lần mỗi lượt `make exp`; vẫn thiếu → kết quả ghi "không đủ lần hợp lệ" kèm lý do loại.

## 9. Quy tắc hợp lệ và đạt

### 9.1 Lần chạy không hợp lệ (loại khỏi thống kê)

| # | Điều kiện | Kiểm bằng |
| --- | --- | --- |
| V1 | Cây git bẩn ở bước 1 | `git status --porcelain` |
| V2 | Máy sinh tải nghẽn: M13 > 1.000 ms, hoặc CPU `k6` ≥ 95 % trần trong > 10 % mẫu | `k6-summary.json`, `docker-stats.csv` |
| V3 | Có swap trong cửa sổ đo | `vmstat` cột `si`/`so` > 0 |
| V4 | Container khởi động lại hoặc bị OOM ngoài kịch bản (EXP-04, EXP-08 dừng API có chủ đích) | `docker inspect --format '{{.RestartCount}} {{.State.OOMKilled}}'` |
| V5 | `make invariants` thoát 2 hai lần liền (không biết kết quả) | `invariants.exit` |
| V6 | Bước nào ở §7.2 lỗi | `env.json` `status = "ERROR"` |

Lần chạy không hợp lệ vẫn giữ thư mục kết quả (§10) với `verdict: "INVALID"` và mã `V1…V6`.

### 9.2 Đọc kết quả bất biến theo chiến lược

| Chiến lược | `invariants.exit` | Nghĩa |
| --- | --- | --- |
| `skip-locked` | 0 | Hợp lệ |
| `skip-locked` | 1 | **Phát hiện lỗi**: lần chạy bị loại khỏi thống kê hiệu năng (UC-21 E1), và EXP **không đạt** tới khi lỗi được sửa và chạy lại đủ 3 lần sạch; mở issue kèm `invariants.json`, điều tra theo [RB-02](../../09-operations/runbooks/RB-02-invariant-violation.md). Không được chạy lại để "lấy lần sạch" mà không sửa |
| `counter` | 0 / 1 | Như `skip-locked`, với kiểm tra theo bộ đếm (DR-210) |
| `naive` | 1 với INV-05 hoặc INV-01 | Hợp lệ: bằng chứng bán vượt (DOC-30 §6) |
| `naive` | 0 | Hợp lệ nhưng không chứng minh được gì: kịch bản chưa đủ tranh chấp; EXP ghi lại và tăng tranh chấp (ví dụ thu hẹp `W`) |

### 9.3 Đạt của một EXP

Một EXP **đạt** khi có đủ 3 lần hợp lệ trên `skip-locked` (hoặc chiến lược chính của EXP), **mọi** lần thỏa tiêu chí tính đúng của EXP (ví dụ M6 = 0 và M14 = 0), và **trung vị** thỏa tiêu chí hiệu năng của EXP (ví dụ M1 < 500 ms). Lần chạy `naive` là phụ lục, không ảnh hưởng đạt/không đạt trừ khi EXP ghi khác.

## 10. Lưu kết quả

```
experiments/results/
  EXP-02/
    summary.md                                     # bảng §11, sinh bởi aggregate.sh; commit
    2026-11-03T1012Z-skip-locked-s1-r1/            # <UTC ISO phút>-<strategy>-s<SCALE>-r<lần>
      env.json                                     # máy, git SHA, compose config, SHOW ALL, params, T0, T1, verdict; commit
      fixture.json                                 # commit
      k6-summary.json                              # commit
      metrics.json                                 # M1…M13; commit
      invariants.json, invariants.exit             # commit
      raw/                                         # docker-stats.csv, pg-activity.csv, *.ndjson, pg_stat_statements; .gitignore
```

- Commit cùng PR của task chạy EXP (P2-14 …) các file nhỏ ở trên; `raw/` nằm trong `.gitignore` (dung lượng). Kết quả không bao giờ bị sửa tay; chạy lại tạo thư mục mới.
- `env.json` có tối thiểu: `gitSha`, `startedAt`, `machine` (§3.1), `limits` (giới hạn thật đọc bằng `docker inspect`), `params` (§6.2 sau khi nhân `SCALE`), `strategy`, `t0`, `t1`, `k6Version`, `postgres` (`SHOW ALL` rút gọn: các khóa ở DOC-62 §9), `verdict` (`VALID`, `INVALID`, `ERROR`), `invalidReasons`.
- Bảng trong `summary.md` được chép vào §Kết quả của tài liệu EXP, kèm link tới thư mục; số đo đồng thời ghi vào DOC-10 §8 nếu dòng đó có.

## 11. Mẫu bảng kết quả

Cột bắt buộc in đậm; EXP chọn thêm cột số đo của mình từ §8.2 và giữ đúng tên cột dưới đây để `aggregate.sh` dùng chung.

```markdown
### EXP-02 · `skip-locked` · SCALE 1 · 2026-11-03

Máy: <lscpu Model name>, <CPU(s)> CPU, <RAM> GB, Linux <kernel>, Docker <version>, k6 <version> · Giới hạn: DOC-70 §3.2 (<ngày bảng>) · Tham số: `experiments/exp02/params.env` @ <sha>

| Lần | Thời điểm (UTC) | SHA | Hợp lệ | Bất biến | Unit giữ+bán / sức chứa | Bán vượt | Bán hết sau (s) | p95 k6 (ms) | p95 API (ms) | Giữ thành công/s | Request giữ/s | 503 (%) | 5xx khác | Conn p95 (ms) | Conn max | Chờ lock (conn·s) | Lag k6 p95 (ms) |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| r1 | 2026-11-03 10:12 | `abc1234` | VALID | 0 | 5.000 / 5.000 | 0 | … | … | … | … | … | … | 0 | … | … | … | … |
| r2 | … | | | | | | | | | | | | | | | | |
| r3 | … | | | | | | | | | | | | | | | | |
| **Tổng hợp** | | | 3/3 | 0 / 0 / 0 | 5.000 ×3 | 0 / 0 / 0 | trung vị (min–max) | … | … | … | … | … | 0 / 0 / 0 | … | … | … | … |
```

Cột bắt buộc: **Lần**, **Thời điểm (UTC)**, **SHA**, **Hợp lệ**, **Bất biến** (mã thoát; khi 1 thì thêm `INV-xx`). Dòng "Tổng hợp" theo §8.3. Bảng của bản `naive` hoặc `counter` là một khối riêng cùng mẫu, tiêu đề ghi chiến lược.

## 12. Phân tích

- `experiments/aggregate.sh <EXP-NN>` (bash + `jq`) đọc mọi `*/env.json` có `verdict = "VALID"` và `metrics.json`, tính §8.3, viết `summary.md`.
- Biểu đồ (không bắt buộc) dựng từ `raw/` và Prometheus bằng notebook `experiments/analysis.ipynb` (DOC-12 §1); ảnh là phụ lục, không phải nguồn số.
- Mỗi tài liệu EXP có mục "Phân tích" trả lời giả thuyết bằng số của bảng, nêu số nào lệch kế hoạch DOC-10 và vì sao (dựa M9–M12).

## 13. Mối đe dọa độ tin cậy

| # | Mối đe dọa | Ảnh hưởng | Giảm thiểu |
| --- | --- | --- | --- |
| T1 | k6 chạy cùng máy, tranh CPU với `api` và `postgres` | p95 phía client tăng, thông lượng giảm | Giới hạn CPU của `k6` (§3.2); loại lần chạy theo V2; báo cả M1 và M2 |
| T2 | Mạng loopback/bridge, không có độ trễ thật | p95 thấp hơn thực tế vài chục ms | Ngân sách DOC-10 §2 giữ ≥ 170 ms dư cho phía k6 và nhiễu; ghi rõ trong Phân tích |
| T3 | Quota CPU (CFS) không phải nhân riêng, có throttling | Đỉnh độ trễ ngẫu nhiên | Lấy mẫu `docker stats`; 3 lần lặp, báo min–max |
| T4 | JIT và cache nguội | Lần đầu chậm hơn | Pha khởi động 60 giây (§5.2 bước 4); không tính vào cửa sổ đo |
| T5 | Cache session nguội cho 99.000 người dùng ngoài nhóm khởi động | Mỗi người dùng tốn thêm một `SELECT session` ở request đầu | Giống thực tế (người dùng đăng nhập từ trước, cache 60 giây, DR-22); ghi nhận, không khử |
| T6 | Mô hình đến đều trong `W` là giả định | Đỉnh thật có thể dồn hơn | EXP ghi `W`; EXP-02 chạy thêm `W` nhỏ hơn nếu cần |
| T7 | Cổng thanh toán giả không có độ trễ của Stripe | Thời gian hủy PaymentIntent trong job trả vé thấp hơn thực tế | Cổng giả mô phỏng độ trễ khi EXP cần (DR-51 `delayMs`); EXP-06 ghi rõ |
| T8 | Một bản sao `api` | Không đo được tranh chấp giữa nhiều bản sao | Đúng với kiến trúc hiện tại (DOC-07); tranh chấp hai bản sao kiểm ở test INVT-25 |
| T9 | Nội suy bucket histogram | M2, M9 sai tới độ rộng bucket | Báo M1 (chính xác) cạnh M2 |
| T10 | 3 lần lặp là mẫu nhỏ | Không kết luận thống kê mạnh | Báo min–max; so sánh giữa chiến lược chỉ khi khoảng min–max không chồng nhau, ngược lại ghi "không phân biệt được" |
| T11 | Autovacuum chạy lúc khác nhau giữa các lần | Nhiễu p95 | Reset volume mỗi lần; ghi `n_dead_tup` của `inventory_unit` trong `metrics.json` |

## 14. Tài liệu EXP-xx viết gì

Mỗi `EXP-xx-*.md` theo mẫu A.6, header `DOC-xx / EXP-xx`, và chỉ ghi phần riêng:

| Mục A.6 | Ghi ở EXP | Lấy từ DOC-70 |
| --- | --- | --- |
| 1. Giả thuyết | `H1 (NFR-xx): …` đo bằng `M…` | — |
| 2. Biến | Biến độc lập (chiến lược, `W`, `admit_rate`…), phụ thuộc (`M…`), kiểm soát | §6.1 là biến kiểm soát chung |
| 3. Baseline | Bản `naive`/`counter`/không kiểm soát của EXP | §6.3 |
| 4. Môi trường | "Theo DOC-70 §3" + điểm khác (nếu có) | §3 |
| 5. Các bước | `params.env`, hành vi người dùng ảo của `load/expNN.ts`, bước riêng (ví dụ dừng API ở EXP-04) | §7 |
| 6. Số đo | Danh sách `M…` và số đo riêng | §8 |
| 7. Tiêu chí đạt | Ngưỡng tính đúng (mọi lần) và hiệu năng (trung vị) | §9.3 |
| 8. Phân tích | Câu hỏi cần trả lời | §12 |
| 9. Mẫu bảng | Cột chọn thêm | §11 |
| 10. Mối đe dọa | Mối đe dọa riêng | §13 |
| 11. Kết quả | Trống tới khi chạy | §10 |
| 12. Câu hỏi còn mở | | |

## 15. Hạ tầng P2-13 phải dựng

| File | Nội dung | Kiểm chứng |
| --- | --- | --- |
| `deploy/compose/docker-compose.experiment.yml` | Giới hạn §3.2, tham số PostgreSQL DOC-62 §9, mạng `172.28.0.0/16`, service `k6` (profile `exp`, IP `172.28.0.250`, mount `load/` → `/load`, thư mục kết quả → `/results`), `RATE_LIMIT_ALLOWLIST` cho `nginx`, service `prometheus` (profile `obs`, cổng host 9091) | `make exp EXP=02 SCALE=0.01 RUNS=1` chạy xong (P2-13) |
| `deploy/compose/prometheus/prometheus.yml` | Job `ticket-api` theo DOC-33 §5 | `curl -s localhost:9091/api/v1/targets` có `health: "up"` |
| `Makefile` target `exp`, `invariants`, `seed` | Như §7.1, DOC-61 | `make exp` thiếu `EXP` → in cách dùng, thoát 2 |
| `experiments/run.sh`, `experiments/lib/{collect,sample-docker,sample-pg}.sh`, `experiments/aggregate.sh` | §7.2, §8.2, §12 | Bảng in ra stdout ở cuối |
| `experiments/seed/users.sql` | §5.1 | Chạy trên database không có profile `experiment` → lỗi `requires the experiment profile` |
| `experiments/exp02/params.env` … | §6.2 | — |
| `load/lib/multiuser.ts`, `load/fixture.ts`, `load/exp02.ts` | §4.1, §5.2 | Cookie của `n = 1` khớp `session_hash` trong DB: E-04 `GET /me` trả `exp-000001@load.test` |
| `experiments/results/.gitignore` | `*/*/raw/` | — |

## Quyết định phát sinh khi viết tài liệu này

**DR-204 · EXP-02 chạy hai lần: M-B ở P2, M-Q ở P6.** *Vấn đề:* NFR-02 ghi kiểm chứng bằng "EXP-02 (với phòng chờ)", nhưng EXP-02 chạy ở P2-14 khi module `admission` (phòng chờ) chỉ có từ P6; không chạy được EXP-02 "với phòng chờ" ở P2. *Quyết định:* ở P2, EXP-02 chạy mô hình M-B (mở bán dồn, không phòng chờ) và chỉ chứng minh tính đúng (M5, M6, M14) cùng số đo hiệu năng tham khảo; ở P6, EXP-02 chạy lại trên M-Q (cùng tài liệu DOC-72, thêm khối kết quả) và là bằng chứng NFR-02 cùng EXP-05. *Hệ quả:* master plan P6-09 (hoặc task mới của P6) thêm "chạy lại EXP-02 trên M-Q"; DOC-72 có hai khối kết quả; tiêu chí thoát M2 không đòi p95 < 500 ms. *Ghi vào:* DOC-70, DOC-72, master plan §5 (P2-14, P6-09), DOC-03 NFR-02 (cột kiểm chứng: "EXP-02 M-Q (P6)").

**DR-205 · k6 chạy trong container của compose thực nghiệm, IP cố định.** *Vấn đề:* DR-75 cần IP máy sinh tải trong `RATE_LIMIT_ALLOWLIST` và giới hạn tài nguyên cố định; k6 chạy trên host thì nginx thấy IP gateway của bridge (đổi theo máy) và k6 không bị giới hạn CPU, tranh tài nguyên không kiểm soát. *Quyết định:* `docker-compose.experiment.yml` có service `k6` (image `grafana/k6` khóa tag lúc S-06, profile compose `exp`, `cpus: 3`, `memory: 4G`), mạng mặc định subnet `172.28.0.0/16`, `k6` IP `172.28.0.250`, `nginx` nhận `RATE_LIMIT_ALLOWLIST=172.28.0.250/32`. Cài k6 trên host không bắt buộc cho thực nghiệm. *Hệ quả:* bảng giới hạn có thêm 3 CPU / 4 GB; tổng trần RAM ≈ 14,8 GB trên máy 16 GB (planned, S-06 kiểm). *Ghi vào:* DOC-70, DOC-62 §2, §4, §9, DOC-10 §7, DOC-61 §1 (k6 trên host chỉ tùy chọn), DOC-11.

**DR-206 · Prometheus và `docker-compose.experiment.yml` dựng ở P2-13.** *Vấn đề:* DOC-62 ghi `docker-compose.experiment.yml`, `prometheus/`, `grafana/` "chưa có (P6-08)" và DOC-11 khóa tag Prometheus "lúc P6-08", nhưng EXP-02, 03, 04, 10 chạy ở P2-14 và cần M2, M9, M10 từ Prometheus cùng giới hạn tài nguyên cố định. *Quyết định:* P2-13 dựng `docker-compose.experiment.yml` và service `prometheus` (profile `obs`, khóa tag lúc P2-13); Grafana và dashboard EXP-05 vẫn ở P6-08. *Hệ quả:* master plan P2-13 thêm hai file này vào đầu ra; DOC-11 dòng `prometheus` đổi thời điểm khóa tag. *Ghi vào:* DOC-70, master plan §5 (P2-13, P6-08), DOC-62 (ghi chú đầu tài liệu), DOC-11 §6, DOC-33 §5.

**DR-207 · Session thực nghiệm suy ra được từ số thứ tự người dùng.** *Vấn đề:* DR-75 nói "session có giá trị biết trước" nhưng không nói k6 lấy cookie và CSRF thế nào; đọc 100.000 cặp giá trị từ file làm tăng RAM của k6. *Quyết định:* người dùng `n` có session ID thô `sha256('exp-session-<n>')`, cookie là base64url của nó, `session_hash = sha256(session ID thô)`, CSRF là base64url của `sha256('exp-csrf-<n>')`; `n = 0` là người tổ chức có dòng `organizer`; script từ chối chạy khi thiếu bảng `inventory_pool_counter` (chỉ có ở profile `experiment`). *Hệ quả:* k6 tự tính cookie bằng `k6/crypto`; giá trị là bí mật đã biết, chỉ tồn tại trên volume máy thực nghiệm và bị xóa mỗi lần chạy. *Ghi vào:* DOC-70, DOC-15 (ghi chú `session`), DOC-32 (rủi ro chấp nhận).

**DR-208 · Sự kiện thử tạo qua API, mỗi lần chạy reset volume, có pha khởi động.** *Vấn đề:* chưa có cách tạo sự kiện của thực nghiệm; dữ liệu và autovacuum tích lũy giữa các lần làm các lần không độc lập; JIT nguội làm lần đầu chậm. *Quyết định:* mỗi lần chạy `down -v` rồi dựng lại; `load/fixture.ts` tạo sự kiện qua E-25, E-34, E-29 bằng người tổ chức `n = 0`, mở bán ở `T0 = now + 120 giây`; một sự kiện khởi động riêng nhận 1.000 lệnh giữ-rồi-hủy trong 60 giây trước `T0`. *Hệ quả:* một lần EXP-02 ≈ 7 phút (planned); kho vé sinh qua đường xuất bản thật (DR-27). *Ghi vào:* DOC-70, DOC-72…74, DOC-80, DOC-86 (FL-19).

**DR-209 · Quy tắc hợp lệ và đạt.** *Vấn đề:* UC-21 E1 và DOC-30 §6 nói "bất biến đỏ → lần chạy bị loại", nhưng loại rồi chạy lại trên bản `skip-locked` sẽ che một lỗi bán vượt thật; bản `naive` lại phải đỏ. *Quyết định:* §9: lần chạy không hợp lệ khi V1…V6; bất biến đỏ trên `skip-locked`/`counter` loại lần chạy khỏi thống kê **và** đánh EXP không đạt tới khi sửa lỗi; `naive` đỏ là hợp lệ; số đo tính đúng phải thỏa ở mọi lần, hiệu năng xét trung vị. *Hệ quả:* DOC-30 §6 và UC-21 E1 đọc theo §9.2. *Ghi vào:* DOC-70, DOC-30 §6, DOC-04 UC-21, DOC-86.

**DR-210 · Kiểm tra bất biến hiểu chế độ bộ đếm.** *Vấn đề:* với `CounterClaimer`, unit pool không đổi trạng thái (DOC-24 §5), nên INV-05 `RESERVATION_UNIT_COUNT` báo sai lệch cho mọi reservation `ACTIVE`/`CONFIRMED` có item pool; DOC-15 §8.2 và DOC-24 §5 nói checker "đối chiếu `available + held + sold = capacity`" nhưng DOC-30 chưa có mục đó. Không sửa thì mọi lần chạy EXP-10 trên `counter` đều đỏ. *Quyết định:* DOC-30 nhận diện pool đếm bằng bộ đếm qua dữ liệu (pool có dòng trong `inventory_pool_counter`): INV-05 bỏ qua phần pool của các pool đó, và thêm một mục VIOLATION mới: `available + held + sold <> capacity`, hoặc `held` ≠ Σ `quantity` của item thuộc reservation `ACTIVE`/`EXPIRING`, hoặc `sold` ≠ Σ `quantity` của item thuộc reservation `CONFIRMED`. Mã `INV-xx` do DOC-30 cấp. *Hệ quả:* EXP-10 trên `counter` có bất biến dùng được; bảng không tồn tại (ngoài profile `experiment`) thì mục mới trả 0 (dùng `to_regclass`). *Ghi vào:* DOC-30 §3, DOC-80, DOC-24 §5.

**DR-211 · Lệnh kiểm tra bất biến không phụ thuộc `INVENTORY_STRATEGY`.** *Vấn đề:* `make invariants` = `docker compose … run --rm api --spring.profiles.active=invariants`; container `run` thừa hưởng môi trường của service `api`, nên sau `make exp STRATEGY=naive` nó nhận `INVENTORY_STRATEGY=naive` mà không có profile `experiment` và `api` từ chối khởi động (DOC-34 §6 luật 3), thoát khác 0 và bị hiểu nhầm là mã 1 hoặc 2. *Quyết định:* lệnh luôn truyền `-e INVENTORY_STRATEGY=skip-locked` (tiến trình CLI không claim nên chiến lược không có nghĩa); áp dụng cho cả `run.sh` và target `make invariants`. *Hệ quả:* lệnh chạy được sau mọi chiến lược. *Ghi vào:* DOC-70, DOC-61 (bảng target `invariants`), DOC-30 §5.1.

**DR-212 · Lưu kết quả trong repo, gộp bằng `jq`.** *Vấn đề:* DR-75 đòi ghi SHA và cấu hình máy nhưng không nói kết quả nằm đâu, cái gì commit. *Quyết định:* `experiments/results/EXP-NN/<UTC>-<strategy>-s<SCALE>-r<n>/` với file nhỏ được commit và `raw/` bị bỏ qua (§10); `experiments/aggregate.sh` (bash + `jq`) sinh `summary.md` theo mẫu §11. *Hệ quả:* `jq` thành công cụ bắt buộc của máy thực nghiệm. *Ghi vào:* DOC-70, DOC-61 §1 (bảng công cụ: `jq` khi chạy thực nghiệm), DOC-12 §1.

**DR-213 · S-06 đo trên E-06 thay cho phòng chờ.** *Vấn đề:* S-06 phải chạy trước P2-00 (DR-152), khi chưa có endpoint phòng chờ (P6) để đo "100.000 người dùng ảo hỏi vị trí". *Quyết định:* S-06 dùng đúng nhịp hỏi của M-Q (DOC-10 §4.1) trên E-06 `GET /api/v1/events` (nginx cache), tiêu chí đạt ở §4.3; mã ở `spikes/s06/`. *Hệ quả:* trần đo được là trần của k6 và nginx; chi phí Redis của hỏi vị trí đo ở EXP-05. *Ghi vào:* DOC-70, master plan §5 (S-06, P0-07), DOC-10 §8.

## Câu hỏi còn mở

Không có. Giới hạn tài nguyên §3.2 và `K` là planned tới khi S-06 (P0-07) chạy; đây là số đo còn chờ, không phải câu hỏi. Quyết định phát sinh là DR-204…213 (mục trên).
