# EXP-02 · Tranh một pool

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-72 / EXP-02
> Phụ thuộc: SDD gốc §8.2, §10.3, §15.2, [Sổ quyết định](../../00-decision-register.md) (DR-41, 45, 46, 61, 75, 76, 152, 172), [DOC-70](README.md) (giao thức chung, DR-204…213), [DOC-03](../../01-product/requirements.md) (FR-06, NFR-01, NFR-02), [DOC-10](../../03-architecture/quality-attributes.md) §2, §4.3, §5, §8, [DOC-24](../../06-design/inventory-and-reservation.md) §4.1, §4.2, §5, §7.2, [DOC-25](../../06-design/idempotency.md) §3.6, [DOC-30](../../06-design/invariant-checker.md) §3, §6 (INV-01, INV-05), [DOC-33](../../06-design/observability.md) §4.2, [DOC-37](../../07-api/api-endpoints.md) (E-14, E-18), [DOC-47](../../08-ux-ui/screens/quantity-picker.md) (QTY-AC-08)
> Người dùng chính: P2-13 (`load/exp02.ts`, `experiments/exp02/params.env`), P2-14 (chạy ở mô hình M-B), P6-09 (chạy lại ở mô hình M-Q), DOC-80 (EXP-10 dùng lại kịch bản), M2

Thực nghiệm kiểm chứng rằng 100.000 người dùng ảo tranh một pool GA 5.000 vé, mỗi lệnh 1–4 vé, **không bao giờ bán vượt và bán hết** (NFR-01), và cho bằng chứng ngược lại trên bản `naive` (SDD gốc §15.2, DR-76). Máy, giới hạn container, seed, quy trình `make exp`, công thức `M1…M14`, quy tắc hợp lệ và cách lưu kết quả nằm ở [DOC-70](README.md); tài liệu này chỉ ghi phần riêng của EXP-02. Kịch bản này cũng là kịch bản của EXP-10 (DOC-80) trên `counter`.

EXP-02 chạy hai lần trong dự án (DR-204): ở **P2-14** trên mô hình **M-B** (mở bán dồn, chưa có phòng chờ), là bằng chứng tính đúng của M2; ở **P6-09** trên mô hình **M-Q** (qua phòng chờ), là bằng chứng NFR-02 cùng EXP-05. Mọi con số ở đây là **kế hoạch** (planned) cho tới khi §11 có số đo.

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi thành DR-214…216 và tóm tắt ở mục "Quyết định phát sinh khi viết tài liệu này".

## 1. Giả thuyết

| # | Giả thuyết | Đo bằng (DOC-70 §8.2) | Lần chạy |
| --- | --- | --- | --- |
| H1 (NFR-01) | Với `skip-locked`, tổng unit `HELD` + `SOLD` của pool không vượt 5.000 và số vé đã hứa (Σ `quantity` của reservation chưa đóng và `CONFIRMED`) không vượt 5.000, ở **cả 3 lần** | M5, M6, M14 | P2 (M-B), P6 (M-Q) |
| H2 (FR-06, SDD gốc §15.2 "bán hết") | Pool **bán hết**: M5 = 5.000 khi tải kết thúc, dù `SKIP LOCKED` có thể từ chối oan khi gần hết vé (DOC-24 §7.2) | M5, M4 | P2, P6 |
| H3 (NFR-02 "không sập") | Không có 5xx nào ngoài 503 `OVERLOADED`; lỗi mạng (`status 0`) = 0 | M7 | P2, P6 |
| H4 (DR-76) | Với `naive`, ít nhất một lần chạy có bán vượt: M6 > 0 và `make invariants` thoát 1 với INV-05 (hoặc INV-01) | M6, M14 | P2 |
| H5 (NFR-02, chỉ P6) | Qua phòng chờ, p95 lệnh giữ vé được tiếp nhận < 500 ms (trung vị 3 lần) | M1, M2 | P6 |

Ở P2, số đo hiệu năng (M1–M4, M8–M13) được ghi lại để tham khảo và để EXP-10 so sánh, **không** có ngưỡng đạt: không có phòng chờ thì 100.000 người dồn vào trong 60 giây vượt trần bulkhead ≈ 436 lệnh/giây (DOC-10 §4.3) theo thiết kế, và 503 `OVERLOADED` là hành vi đúng (DR-61).

## 2. Biến

| Loại | Biến | Giá trị |
| --- | --- | --- |
| Độc lập | `STRATEGY` | `skip-locked` (chính), `naive` (phụ lục) |
| Độc lập | Mô hình tải | M-B (P2), M-Q (P6) |
| Độc lập | `W` (cửa sổ đến của M-B) | 60 giây; 10 giây khi `naive` không lộ bán vượt (§5.4) |
| Phụ thuộc | Tính đúng | M5, M6, M7, M14 |
| Phụ thuộc | Hiệu năng | M1, M2, M3, M3b, M4, M8, M9, M10, M11, M12, M13 |
| Phụ thuộc | Riêng EXP-02 | X1…X5 (§6): số vé `SOLD`, lệnh giữ lại sau 409, phân bổ kết quả, email vé, p95 lệnh thành công |
| Kiểm soát | Chung | DOC-70 §6.1 (profile, `PAYMENTS_MODE=fake`, allowlist, log `WARN`) |
| Kiểm soát | Riêng | Một loại vé `GA`, giá 0, sức chứa `5.000 × SCALE`; `USERS = 100.000 × SCALE` (tỉ lệ 20:1, NFR-02); phân phối số vé và thời điểm đến cố định theo `n` (DR-214); `reservation.hold-duration` mặc định 10 phút (không reservation nào hết hạn trong cửa sổ đo) |

## 3. Baseline

Baseline là bản `naive` (`NaiveClaimer`, DOC-24 §5): đọc unit `AVAILABLE` không khóa rồi ghi `HELD` không điều kiện trạng thái, luôn báo đủ `:qty`. Bán vượt của bản này **không** làm số unit `HELD` vượt sức chứa, vì unit bị ghi đè `reservation_id` chứ không nhân đôi; nó lộ ra ở M6 (vé đã hứa > sức chứa) và ở INV-05 (reservation có ít unit hơn Σ `quantity`) (DOC-24 §5, DOC-30 §6).

Bản `naive` chạy **một lượt 3 lần** cùng kịch bản, trừ một điểm: người dùng ảo chỉ giữ vé, không gọi `confirm-free` (DR-216). Kết quả của nó là phụ lục, không ảnh hưởng đạt/không đạt (DOC-70 §9.3).

Bản `counter` không phải baseline của EXP-02; so sánh `skip-locked` với `counter` thuộc EXP-10 (DOC-80) trên cùng kịch bản.

## 4. Môi trường

Theo DOC-70 §3 (máy, giới hạn container, mạng `172.28.0.0/16`, `k6` ở `172.28.0.250` trong allowlist). Không có điểm khác.

## 5. Các bước

### 5.1 Tham số: `experiments/exp02/params.env`

```bash
# experiments/exp02/params.env — DOC-72 §5.1
USERS=100000            # × SCALE; người dùng ảo n = 1…USERS (n = 0 là người tổ chức)
CAPACITY=5000           # × SCALE; một loại GA, giá 0
ARRIVAL_WINDOW_S=60     # W của M-B
VUS=200                 # K, chốt lại theo S-06 (DOC-70 §4.3)
MAX_DURATION_S=300      # trần maxDuration của k6
DRAIN_S=40              # DOC-70 §6.2
CONFIRM=1               # 1: người thắng gọi confirm-free; run.sh đặt 0 khi STRATEGY=naive (DR-216)
CONFIRM_DELAY_MIN_S=2   # thời gian "đọc trang thanh toán" trước khi bấm "Nhận vé"
CONFIRM_DELAY_MAX_S=8
CAPACITY_RETRIES=3      # số lần giữ lại sau 409 INSUFFICIENT_CAPACITY có available ≥ 1 (DR-215)
API_OVERRIDES='{}'
```

Với `SCALE = 0,01` (thử trên máy dev, P2-13): 1.000 người dùng ảo, pool 50 vé.

### 5.2 Fixture

Theo DOC-70 §5.2 với một loại vé: `name = "EXP-02 GA"`, `model = GA`, `price = 0`, `capacity = CAPACITY × SCALE`. `fixture.json` có đúng một `ticketTypeId` và một `poolId`.

### 5.3 Hành vi người dùng ảo: `load/exp02.ts`

Số vé, thời điểm đến và thời gian chờ trước khi xác nhận của người dùng `n` là hàm tất định của `n` (DR-214), nên mọi lần chạy và mọi chiến lược nhận cùng một tải:

| Tham số của người dùng `n` | Công thức | Phân phối |
| --- | --- | --- |
| `q_n` (số vé xin) | `1 + ⌊4 × uniform(n, 'exp02-qty')⌋` | Đều trên {1, 2, 3, 4}; trung bình 2,5 |
| `a_n` (thời điểm đến sau `T0`) | `W × uniform(n, 'exp02-arrival')` | Đều trên [0, W) |
| `c_n` (chờ trước `confirm-free`) | `CONFIRM_DELAY_MIN_S + (CONFIRM_DELAY_MAX_S − CONFIRM_DELAY_MIN_S) × uniform(n, 'exp02-confirm')` | Đều trên [2, 8) giây |

`uniform(n, salt) = u32(sha256('<salt>-<n>')) / 2³²` ∈ [0, 1), với `u32(h)` là 4 byte đầu của `h` đọc big-endian; hàm thêm vào `load/lib/multiuser.ts` cạnh `user(n)` (DR-214). Ví dụ `q_n` đổi theo băm chứ không theo `n mod 4`, nên trong một VU (người dùng `n ≡ vu mod 200`) vẫn đủ bốn mức.

```ts
// load/exp02.ts — khung hành vi
// send(method, path, body, user, name): tạo một Idempotency-Key cho một lần "bấm", gửi lại cùng key
//   và cùng body khi 503/429/status 0 theo DOC-70 §4.1, gọi record() cho mỗi response; trả response cuối.
// sleepS(s): await của setTimeout. Cả hai là hàm nội bộ của load/lib/multiuser.ts (P2-13).
import { at, shard, send, sleepS, uniform } from './lib/multiuser.ts';
import { Counter } from 'k6/metrics';

const capacityRetry = new Counter('exp02_capacity_retry');           // X2
const fx = JSON.parse(open(__ENV.FIXTURE));
const W = Number(__ENV.ARRIVAL_WINDOW_S) * 1000;
const T0 = Date.parse(fx.saleStartsAt);

export const options = {
  scenarios: { main: { executor: 'per-vu-iterations', vus: Number(__ENV.VUS), iterations: 1,
                       maxDuration: `${__ENV.MAX_DURATION_S}s` } },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  thresholds: { 'http_req_duration{name:hold}': ['p(95)<600000'],               // DOC-70 §8.2 M1
                'http_req_duration{name:hold,expected_response:true}': ['p(95)<600000'], // X5
                'http_req_duration{name:confirm_free}': ['p(95)<600000'] },
};

async function buyer(u) {
  let qty = 1 + Math.floor(4 * uniform(u.n, 'exp02-qty'));
  await at(T0 + Math.floor(W * uniform(u.n, 'exp02-arrival')));
  for (let attempt = 0; attempt <= Number(__ENV.CAPACITY_RETRIES); attempt++) {
    const body = JSON.stringify({ items: [{ type: 'GA', ticketTypeId: fx.ticketTypeIds[0], quantity: qty }] });
    const res = await send('POST', `/api/v1/events/${fx.eventId}/reservations`, body, u, 'hold'); // key mới mỗi vòng for
    if (res.status === 201) { if (__ENV.CONFIRM === '1') await confirm(u, res); return; }
    if (res.status === 409 && res.json('code') === 'INSUFFICIENT_CAPACITY') {
      const available = Number(res.json('available'));
      if (available < 1) return;                                       // hết vé: rời đi
      qty = Math.min(qty, available); capacityRetry.add(1); continue;  // như QTY-AC-08: bấm lại với số còn
    }
    return;                                                            // 409/422 khác, 5xx khác: dừng (DOC-70 §4.1)
  }
}

async function confirm(u, hold) {
  const lo = Number(__ENV.CONFIRM_DELAY_MIN_S), hi = Number(__ENV.CONFIRM_DELAY_MAX_S);
  await sleepS(lo + (hi - lo) * uniform(u.n, 'exp02-confirm'));
  await send('POST', `/api/v1/orders/${hold.json('orderId')}/confirm-free`, '{}', u, 'confirm_free');
}

export default async function () {
  await Promise.all(shard(__VU, Number(__ENV.VUS), Number(__ENV.USERS)).map(buyer));
}
```

Quy tắc:

1. Mỗi lần **bấm** (lần giữ đầu, mỗi lần giữ lại sau 409, `confirm-free`) tạo một `Idempotency-Key` mới; gửi lại vì 503/429/lỗi mạng dùng **cùng** key và cùng chuỗi body (DOC-25 §3.6, DOC-70 §4.1).
2. 409 `INSUFFICIENT_CAPACITY` với `available ≥ 1`: giữ lại ngay với `quantity = min(q, available)`, tối đa `CAPACITY_RETRIES` lần (DR-215). `available = 0`: rời đi. `available` là ước lượng (DOC-24 §4.2, DR-172), nên lần giữ lại có thể lại thua.
3. 409 khác (`ACTIVE_RESERVATION_EXISTS`, `EVENT_NOT_ON_SALE`), 422, 5xx khác `OVERLOADED`: ghi nhận qua `record()` và dừng. Kịch bản đúng thì hai mã 409 này bằng 0 (mỗi người một lệnh giữ thành công; mọi người đến sau `T0`).
4. `confirm-free` gặp 409 `RESERVATION_NOT_ACTIVE`: ghi nhận, dừng (không xảy ra vì giữ 10 phút > cửa sổ đo).

### 5.4 Lệnh chạy

```bash
make exp EXP=02                          # 3 lần, skip-locked, SCALE=1 (P2-14)
make exp EXP=02 STRATEGY=naive           # 3 lần, naive, CONFIRM=0 (phụ lục)
make exp EXP=02 SCALE=0.01 RUNS=1        # thử script trên máy dev (P2-13), không ghi vào §11
```

Nếu cả 3 lần `naive` đều cho M6 = 0 (DOC-70 §9.2 dòng cuối), chạy thêm `make exp EXP=02 STRATEGY=naive RUNS=1` với `ARRIVAL_WINDOW_S=10` sửa tạm trong `params.env` (commit riêng, ghi vào §11); không đổi `W` của `skip-locked`.

### 5.5 Khối P6 (M-Q)

Ở P6-09, cùng fixture, cùng phân phối `q_n`; người dùng ảo đi theo M-Q (vào hàng E-10, hỏi E-11 theo `retryAfterSeconds`, giữ vé khi được cấp lượt) theo đặc tả hành vi ở DOC-75. Tham số riêng của khối này (`admit_rate`, `max_active` dùng giá trị mặc định DR-57, DR-60 trừ khi EXP-05 đã hiệu chỉnh) ghi vào `experiments/exp02q/params.env` khi viết DOC-75; kết quả là một khối riêng ở §11.

## 6. Số đo và công thức

Dùng các số đo của DOC-70 §8.2: M1, M2, M3, M3b, M4, M5, M6, M7, M8, M9, M10, M11, M12, M13, M14. M5 dùng nhánh `skip-locked`/`naive`. M6 đo ngay sau `T1`, trước bước xả (DOC-70 §8.2).

Số đo riêng:

| # | Đại lượng | Đơn vị | Nguồn | Công thức |
| --- | --- | --- | --- | --- |
| X1 | Unit đã bán | unit | SQL | `SELECT count(*) FROM inventory_unit WHERE pool_id = :pool AND status = 'SOLD'`; đo sau bước xả. Với `CONFIRM=1` kỳ vọng = M5 (mọi người thắng đều xác nhận) |
| X2 | Lệnh giữ lại sau 409 có `available ≥ 1` | lệnh | k6 | `metrics.exp02_capacity_retry.values.count` |
| X3 | Phân bổ kết quả giữ vé | request | Prometheus | `sum by (result) (increase(ticket_hold_result_total[R]))` (DOC-33 §4.2); tổng các `result` = M3b × Δ trừ request bị nginx chặn (0, vì allowlist) |
| X5 | p95 lệnh giữ **thành công** phía client | ms | k6 | `metrics["http_req_duration{name:hold,expected_response:true}"].values["p(95)"]` (k6 đánh dấu `expected_response` cho status 200–399); script khai threshold giả cho submetric này như M1 |
| X4 | Email vé | thư | Mailpit | `curl -s localhost:8025/api/v1/messages?limit=1 \| jq .total` sau bước xả; kỳ vọng = số đơn `PAID` (`SELECT count(*) FROM orders WHERE event_id = :e AND status = 'PAID'`) khi outbox đã xả hết |

`collect.sh` chạy thêm X1, X3, X4 khi `EXP = 02` hoặc `10`; X2, X5 lấy từ `k6-summary.json`.

## 7. Tiêu chí đạt

**Tính đúng (mọi lần hợp lệ, `skip-locked`):**

| Số đo | Đạt khi |
| --- | --- |
| M6 | = 0 |
| M5 | = `CAPACITY × SCALE` (5.000): không vượt **và** bán hết |
| M14 | Thoát 0 |
| M7 | = 0; `status 0` = 0 |
| X1 | = M5 (P2, `CONFIRM=1`) |

**Hiệu năng (trung vị):** không có ngưỡng ở khối P2 (§1). Khối P6: M1 < 500 ms và M7 = 0 (H5, NFR-02).

**Bản `naive`:** H4 thỏa khi ít nhất một lần hợp lệ có M6 > 0 và M14 = 1 với INV-05 hoặc INV-01. Không thỏa sau cả lần `W = 10 giây` (§5.4) thì ghi "không tái hiện được" vào §11 và mở issue: kịch bản hoặc `NaiveClaimer` có lỗi (INVT-27 tái hiện được ở 64 luồng).

Tiêu chí thoát M2 (master plan §5 Phase 2) ứng với dòng M5, M6 và M14 của bảng tính đúng.

## 8. Phân tích

Mục "Phân tích" của §11 trả lời bằng số của bảng:

1. H1–H3 đạt hay không, ở từng lần.
2. Bán hết sau bao lâu (M4), thông lượng giữ thành công (M3) so với trần bulkhead ≈ 436 lệnh/giây của DOC-10 §4.3. Thấp hơn nhiều → xem M9 (connection giữ lâu: tranh chấp hay thiếu index) và M12 (`postgres` hay `api` chạm trần CPU).
3. Tỉ lệ 503 (M8) theo thời gian: dồn ở mấy giây đầu (trước khi bán hết) hay kéo dài; sau khi bán hết, lệnh giữ vé còn tới database bao nhiêu (X3 `insufficient_capacity`) và có qua cờ hết vé không (DR-46, nếu cờ đã có ở P2).
4. X2: bao nhiêu lần từ chối oan gần hết vé (SDD gốc §8.2) và lần giữ lại có thành công không.
5. M11 của `skip-locked`: kỳ vọng ≈ 0 conn·giây (không chờ lock unit; `reservation_open_uq` không bao giờ trùng vì mỗi người một lệnh). Khác 0 → tìm `wait_event` trong `raw/pg-activity.csv`.
6. Chặng 9–17 (một transaction) so với 60 ms p95 của DOC-10 §2 qua M9.
7. `naive`: số vé bán vượt mỗi lần, và vì sao số unit `HELD` không vượt sức chứa (ghi đè `reservation_id`).

Planned để đối chiếu (không phải tiêu chí):

| Đại lượng | Kế hoạch | Cơ sở |
| --- | --- | --- |
| Số lệnh giữ thành công | ≈ 2.000 (5.000 / 2,5) cộng phần giữ lại với số nhỏ hơn | Phân phối `q_n` |
| M4 bán hết sau | 5–15 giây | ≥ 2.000 / 436 ≈ 4,6 giây, cộng chờ `Retry-After` 1–3 giây của các lần 503 |
| M9 p95 | ≤ 60 ms | DOC-10 §2 chặng 9–17 |
| M10 connection đang dùng tối đa | ≤ 24 (bulkhead) + vài connection của job | DOC-10 §5 |
| M11 | ≈ 0 | `SKIP LOCKED` |
| X4 | ≈ 2.000 thư | DOC-10 §7 (`mailpit`) |

## 9. Mẫu bảng kết quả

Dùng nguyên bảng mẫu của DOC-70 §11 (đã gồm M5, M6, M4, M1, M2, M3, M3b, M8, M7, M9, M10, M11, M13), thêm bốn cột: **Đã bán (X1)** và **Giữ lại sau 409 (X2)** sau cột "Bán vượt", **p95 k6 thành công (X5)** sau cột "p95 k6", **Email (X4)** ở cuối. Mỗi chiến lược và mỗi khối (P2 M-B, P6 M-Q) một bảng riêng, tiêu đề dạng `### EXP-02 · skip-locked · M-B · SCALE 1 · <ngày>`.

## 10. Mối đe dọa độ tin cậy

Chung ở DOC-70 §13. Riêng EXP-02:

| # | Mối đe dọa | Ảnh hưởng | Giảm thiểu |
| --- | --- | --- | --- |
| E1 | M-B không có phòng chờ, khác cách bán thật từ P6 | Số đo hiệu năng P2 không đại diện cho NFR-02 | H5 chỉ xét ở khối P6 (DR-204); P2 chỉ kết luận tính đúng |
| E2 | Đến đều trong 60 giây là giả định | Đỉnh thật dồn hơn làm nhiều 503 hơn | Bản `naive` có thêm lần `W = 10 giây` (§5.4); tính đúng không phụ thuộc `W` |
| E3 | Bán hết sau vài giây nên phần lớn 60 giây là lệnh giữ thua | M1, M2 trộn lệnh thắng với lệnh thua nhanh (409 sau khi hết vé) | Báo thêm X5 (p95 chỉ lệnh 201); `ticket_hold_duration_seconds` không có label kết quả nên M2 không tách được, ghi rõ trong Phân tích |
| E4 | `naive` không xác nhận nên khác `skip-locked` ở pha sau giữ vé | Không so được hiệu năng hai bản | Bản `naive` chỉ dùng cho M6/M14 (§3) |
| E5 | `q_n` và `a_n` tất định | Ba lần lặp cùng tải; khác nhau chỉ do lập lịch | Đúng mục đích (so được giữa các lần và với EXP-10); biến thiên giữa lần đo phần lập lịch thật |
| E6 | Một loại vé, một pool | Không đo tranh chấp nhiều pool trong một lệnh (thứ tự `poolId` tăng dần) | Thuộc INVT-03/INVT-04 (DOC-24) và EXP-01 (ghế) |

## 11. Kết quả

Chưa chạy.

## Quyết định phát sinh khi viết tài liệu này

**DR-214 · Tham số của người dùng ảo là hàm tất định của `n`.** *Vấn đề:* "1–4 vé mỗi request" và "đến trong cửa sổ W" cần một phân phối; dùng `Math.random()` thì ba lần lặp và hai chiến lược (EXP-02 `naive`, EXP-10 `counter`) nhận tải khác nhau, so sánh giữa chúng lẫn nhiễu của bộ sinh số. *Quyết định:* `uniform(n, salt) = u32(sha256('<salt>-<n>')) / 2³²` ở `load/lib/multiuser.ts`; số vé `q_n = 1 + ⌊4 × uniform(n, 'exp02-qty')⌋` (đều trên 1–4), thời điểm đến `a_n = W × uniform(n, 'exp02-arrival')`, thời gian chờ xác nhận theo salt `exp02-confirm`. Phân phối theo băm, không theo `n mod 4`, vì `shard()` chia người dùng theo `n mod K` và `K = 200` chia hết cho 4 (mọi người của một VU sẽ cùng số vé). *Hệ quả:* EXP-10 dùng đúng salt `exp02-*` để cùng tải; DOC-70 §4.1 thêm `uniform()`, `send()`, `sleepS()` vào khung thư viện. *Ghi vào:* DOC-72, DOC-80, DOC-70 §4.1.

**DR-215 · Giữ lại sau 409 `INSUFFICIENT_CAPACITY` có `available ≥ 1`.** *Vấn đề:* `SKIP LOCKED` có thể từ chối oan khi gần hết vé (DOC-24 §7.2) và lệnh xin 4 vé thua khi chỉ còn 3; nếu người dùng ảo rời đi ngay, pool có thể kết thúc với vài vé `AVAILABLE` và H2 "bán hết" thất bại vì kịch bản chứ không vì hệ thống. Người mua thật thì bấm lại với số còn lại (màn Chọn số lượng đặt số về `available`, QTY-AC-08). *Quyết định:* người dùng ảo nhận 409 `INSUFFICIENT_CAPACITY` với `available ≥ 1` gửi lệnh giữ mới (key mới) với `quantity = min(q, available)`, tối đa `CAPACITY_RETRIES = 3` lần; `available = 0` thì rời đi. *Hệ quả:* số lệnh giữ lớn hơn số người dùng một chút (X2); "bán hết" đo được mà không cần người dùng ảo đến sau. *Ghi vào:* DOC-72, DOC-80.

**DR-216 · Bản `naive` chỉ giữ vé, không `confirm-free`.** *Vấn đề:* với `NaiveClaimer`, unit của một reservation có thể bị reservation khác ghi đè; xác nhận reservation đó (`UPDATE inventory_unit SET status = 'SOLD' WHERE reservation_id = :r AND status = 'HELD'`) cập nhật ít dòng hơn Σ `quantity`, và cách transaction xác nhận xử lý sai lệch này chưa được đặc tả (DOC-26, P3). Lỗi đó sẽ rơi vào M7 và làm nhiễu bằng chứng bán vượt. *Quyết định:* `run.sh` đặt `CONFIRM=0` khi `STRATEGY=naive`; bằng chứng bán vượt đọc từ M6 (đã tính cả reservation `ACTIVE`) và INV-05. *Hệ quả:* bản `naive` không so được hiệu năng với `skip-locked` (§10 E4); không cần định nghĩa hành vi xác nhận trên dữ liệu đã hỏng. *Ghi vào:* DOC-72, DOC-70 §6.3 (ghi chú cột `naive`).

## 12. Câu hỏi còn mở

Không có. `VUS` và giới hạn container là planned tới khi S-06 chạy (DOC-70 §4.3); tham số của khối P6 chốt khi viết DOC-75. Quyết định phát sinh: DR-214…216 (mục trên).
