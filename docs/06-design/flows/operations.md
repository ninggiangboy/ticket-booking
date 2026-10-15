# Luồng: Vận hành và thực nghiệm

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-86
> Phụ thuộc: [DOC-82](README.md) (tên thành phần tham gia), [DOC-04](../../01-product/use-cases.md) UC-18, UC-21, [DOC-30](../invariant-checker.md) §1–§9, [DOC-70](../../10-testing/experiments/README.md) §3–§11, [RB-02](../../09-operations/runbooks/RB-02-invariant-violation.md), [DOC-24](../inventory-and-reservation.md) §4.5, §5, [DOC-33](../observability.md) §4.2, [DOC-34](../configuration-reference.md) §2, §3, §6, [DOC-15](../../05-data/ops-model.md), [DOC-61](../../09-operations/local-dev.md) §3, [DOC-62](../../09-operations/deploy-compose.md) §9, [DOC-08](../../03-architecture/data-flows.md), [DOC-06](../../02-glossary.md), [Sổ quyết định](../../00-decision-register.md) (DR-42, 73, 75, 76, 147, 178, 179, 180, 181)
> Người dùng chính: P2-11 (`InvariantChecker`, CLI, target `make invariants`), P2-13 (hạ tầng thực nghiệm, `experiments/run.sh`), P2-14 và mọi task chạy EXP, tác giả DOC-71…80, người vận hành theo RB-02

Tài liệu mô tả hai luồng không có giao diện: `FL-18` kiểm tra bất biến (định kỳ trong `api` và chạy tay bằng `make invariants`) và `FL-19` chạy một thực nghiệm bằng `make exp`. Với mỗi luồng: ai gọi ai, transaction nào, đầu ra ở đâu, và nếu một tiến trình chết giữa chừng thì người vận hành thấy gì. Danh mục `INV-xx`, câu SQL, JSON và mã thoát ở [DOC-30](../invariant-checker.md); cách điều tra từng sai lệch ở [RB-02](../../09-operations/runbooks/RB-02-invariant-violation.md); giao thức thực nghiệm (máy, mô hình tải, công thức số đo, quy tắc hợp lệ, mẫu bảng) ở [DOC-70](../../10-testing/experiments/README.md). Tài liệu này không lặp các phần đó. Luồng giữ vé, nhận vé 0 đồng, hủy giữ và job trả vé mà k6 kích hoạt trong FL-19 ở FL-13…16 (DOC-85).

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi thành DR-218…221 và tóm tắt ở mục "Quyết định mới khi viết tài liệu này".

Mọi sơ đồ dùng tên thành phần của DOC-82 §1. Thành phần cụ thể của hai luồng:

| Tên | Loại | Mã | Ghi chú |
| --- | --- | --- | --- |
| `Operator` | Tác nhân | — | Người vận hành/nghiên cứu (PS-5) |
| `make` | Công cụ | `Makefile` target `invariants`, `exp`; với `exp` gồm cả `experiments/run.sh` | DOC-61 §3; một participant cho cả target và script nó gọi |
| `k6` | Công cụ | service `k6` của `docker-compose.experiment.yml`, kịch bản `load/*.ts` | DOC-70 §3.3, §4.1 (DR-205) |
| `psql` | Công cụ | `psql` trong container `postgres` | Nạp `experiments/seed/users.sql` |
| `prometheus` | Công cụ | service `prometheus` (profile `obs`), cổng host 9091 | DOC-70 §3.2 (DR-206); thêm vào DOC-82 §1 (DR-220) |
| `nginx` | Edge | `limit_req`, allowlist `172.28.0.250/32` | DOC-70 §3.3 |
| `api` | API | container `api` đang phục vụ; hoặc container `api` chạy một lần với profile `invariants` | DOC-30 §5.1 |
| `ReleaseExpiredHoldsJob` | Job | `io.ticket.reservation.job.ReleaseExpiredHoldsJob` | DOC-24 §4.5; chạy nền trong FL-19 |
| `InvariantChecker` | Job + service | `io.ticket.invariant.job.InvariantCheckJob`, `io.ticket.invariant.job.InvariantCliRunner`, `io.ticket.invariant.service.InvariantChecker` | DOC-30 §1: một participant bao cả job, CLI runner và service |
| `db` | Cơ sở dữ liệu | PostgreSQL 18 | Mọi bảng ở DOC-30 §3 |

## FL-18 · Kiểm tra bất biến định kỳ và chạy tay

- **UC / FR:** UC-18, FR-12, NFR-01 · **Màn hình:** không có · **Endpoint:** không có · **Sự kiện:** không
- **Trigger:** (a) `InvariantCheckJob` theo `@Scheduled(fixedDelayString = "${invariant.interval}")`, chạy ngay khi `api` khởi động rồi cách nhau 5 phút; (b) người vận hành gõ `make invariants`; (c) `experiments/run.sh` ở bước 11 của mỗi lần chạy (FL-19 bước 13).
- **Điều kiện đầu:** schema đã migrate (CLI không chạy Flyway, DR-179); với (b) và (c), container `postgres` đang chạy (CLI không tự khởi động nó, DR-219).

### Thành phần

| Tên | Loại | Mã | Định nghĩa ở |
| --- | --- | --- | --- |
| `Operator` | Tác nhân | — | DOC-04 UC-18 |
| `make` | Công cụ | target `invariants` | §FL-18 Chi tiết bước, DOC-61 |
| `api` | API (container) | `api` đang phục vụ (a); `docker compose run --rm --no-deps -T api` (b) | DOC-62, DOC-30 §5.1 |
| `InvariantChecker` | Job + service | `InvariantCheckJob.run()`, `InvariantCliRunner.run(args)`, `InvariantChecker.runAll()` | DOC-30 §2 |
| `db` | Cơ sở dữ liệu | `inventory_pool`, `inventory_unit`, `reservation`, `reservation_item`, `orders`, `ticket`, `event`, `outbox`, `seat_map_version` | DOC-30 §3.3 |

### Sơ đồ tuần tự (a): định kỳ trong `api`

```mermaid
sequenceDiagram
  autonumber
  participant A as api
  participant I as InvariantChecker
  participant D as db
  Note over A: Khởi động (mọi profile trừ invariants); @EnableScheduling
  loop Mỗi invariant.interval (PT5M, fixedDelay; lần đầu ngay khi khởi động)
    A->>I: InvariantCheckJob.run() (JobMetrics bao ngoài)
    Note over I,D: T1 begin READ ONLY, REPEATABLE READ
    I->>D: SET LOCAL statement_timeout = '30s'
    I->>D: SELECT now()  (checkedAt)
    loop INV-01 … INV-16
      I->>D: SELECT count(*) FROM (<offenders>) x
      opt count > 0
        I->>D: <offenders> LIMIT 11
      end
    end
    alt E1/E2/E3 mất kết nối, quá 30 giây, lỗi SQL
      Note over I,D: T1 rollback
      I-->>A: log ERROR invariant.failed {error, check}; ticket_job_runs_total{job="invariant_checker",result="error"} += 1; gauge giữ giá trị cũ
    else chạy hết
      Note over I,D: T1 commit (chỉ đọc, không có gì để ghi)
      Note right of D: Nếu tiến trình chết ở đây: không có gì để dọn; lần khởi động sau chạy lại ngay
      I->>I: gauge ticket_invariant_violations{check} = count (0 cho mục sạch); histogram ticket_invariant_check_seconds
      alt E4 có violation
        I-->>A: log ERROR invariant.violation {check, id, count, sample} cho mỗi mục
      else sạch
        I-->>A: log INFO invariant.clean {durationMs}
      end
      opt có report (INV-15, INV-16)
        I-->>A: log INFO invariant.report {check, id, reason, count}
      end
      I-->>A: ticket_job_runs_total{job="invariant_checker",result="ok"} += 1
    end
  end
```

### Sơ đồ tuần tự (b): chạy tay bằng `make invariants`

```mermaid
sequenceDiagram
  autonumber
  actor O as Operator
  participant M as make
  participant A as api
  participant I as InvariantChecker
  participant D as db
  O->>M: make invariants > inv.json
  M->>A: docker compose run --rm --no-deps -T -e INVENTORY_STRATEGY=skip-locked api --spring.profiles.active=invariants
  Note over A: Container mới, profile invariants: không web, không Flyway, không @Scheduled, log ra stderr (DR-179)
  alt E5 context không khởi động được (cấu hình sai, không lấy được dialect từ db)
    A-->>M: JVM thoát 1, stdout rỗng
    M-->>O: stderr "invariants: exit=1"; make thoát 2; inv.json rỗng → đọc là "không biết" (DR-219)
  end
  A->>I: InvariantCliRunner.run(args)
  I->>D: T1 như sơ đồ (a), bước 3–7
  alt E1/E2/E3
    I-->>A: JSON {checkedAt, durationMs, error, check} ra stdout; mã 2
  else có violation
    I-->>A: JSON {violations:[…], reports:[…]}; mã 1
  else sạch
    I-->>A: JSON {violations:[], reports:[…]}; mã 0
  end
  A-->>M: SpringApplication.exit → container thoát 0/1/2; --rm xóa container
  M-->>O: stdout: một dòng JSON; stderr: "invariants: exit=<0|1|2>"; make thoát 0 hoặc 2
  opt mã 1
    O->>O: Theo RB-02 (dừng bán nếu nhóm kho vé)
  end
```

### Chi tiết bước

| Bước | Từ → Đến | Lệnh | Dữ liệu | Quy tắc | Lỗi → xử lý |
| --- | --- | --- | --- | --- | --- |
| (a) 1 | `api` → `InvariantChecker` | `InvariantCheckJob.run()` | — | `@Scheduled(fixedDelay)`: lần chạy sau tính từ lúc lần trước **kết thúc**, không chồng lần; không có lease vì chỉ đọc và compose chỉ có một `api` (DOC-30 §4) | Ngoại lệ không lọt ra ngoài job; `JobMetrics` ghi `result="error"` |
| (a) 2–4 | `InvariantChecker` → `db` | `BEGIN READ ONLY ISOLATION LEVEL REPEATABLE READ; SET LOCAL statement_timeout = '30s'; SELECT now()` | `checkedAt = 2026-11-10T03:05:00.123Z` | Một snapshot cho cả 16 mục (DOC-30 §3.1) | E1, E2 |
| (a) 5–7 | → `db` | câu `count` rồi câu `offenders` (≤ 11 ID) của từng mục theo thứ tự INV-01…16 | Ví dụ INV-01 sau khi cố ý `REMOVED` một unit: `count = 1`, `sample = ["0199b1c2-5001-7aaa-8bbb-111122223333"]` | Câu `offenders` chỉ chạy khi `count > 0` | E2 (một câu quá 30 giây), E3 |
| (a) 10 | `InvariantChecker` | đặt gauge | `ticket_invariant_violations{check="POOL_UNIT_COUNT"} 1`, 13 mục còn lại `0` | Mục `REPORT` không có gauge (DR-178) | — |
| (a) 11–14 | `InvariantChecker` → log | `invariant.violation` (ERROR), `invariant.clean` / `invariant.report` (INFO) | `{"check":"POOL_UNIT_COUNT","id":"INV-01","count":1,"sample":[…]}` | Không email, không token trong log (DOC-33) | — |
| (b) 1–2 | `Operator` → `make` → `api` | `make invariants > inv.json` → recipe ở dưới | — | `--no-deps`: không khởi động `postgres` hộ (lệnh chỉ đọc không đổi trạng thái stack); `-T`: không cấp TTY để stderr không lẫn vào stdout; `-e INVENTORY_STRATEGY=skip-locked`: không thừa hưởng chiến lược của lần `make exp` trước (DR-211) | E5 |
| (b) 4–8 | `InvariantChecker` → `db` → stdout | `runAll()` rồi in JSON một dòng | JSON của DOC-30 §5.2 | stdout chỉ có đúng một dòng JSON | E1–E3 → JSON có `error`, mã 2 |
| (b) 9–10 | `api` → `make` → `Operator` | mã thoát của container; recipe in `invariants: exit=<code>` ra stderr | `invariants: exit=1` | GNU make luôn thoát 2 khi recipe lỗi, nên **mã của `make` không phân biệt 1 và 2**; mã chính xác đọc ở dòng stderr hoặc gọi thẳng `docker compose run` như `run.sh` (DR-219) | — |

Recipe của target `invariants` (P2-11; thay dòng "Lệnh thật" của DOC-61 §3):

```make
invariants: env ## Check invariants once: one JSON line on stdout, exit code on stderr
	@$(COMPOSE) run --rm --no-deps -T -e INVENTORY_STRATEGY=skip-locked \
	  api --spring.profiles.active=invariants; \
	code=$$?; echo "invariants: exit=$$code" >&2; exit $$code
```

`@` để `make` không in dòng lệnh ra stdout; `make invariants 2>/dev/null | jq .violations` dùng được.

**Đọc kết quả** (mọi nơi gọi lệnh: người vận hành, `run.sh`, test, DR-219):

| Mã thoát container | stdout | Kết luận |
| --- | --- | --- |
| 0 | JSON hợp lệ, `violations = []` | Sạch |
| 1 | JSON hợp lệ, `violations` khác rỗng | Có sai lệch → RB-02 |
| 2 | JSON có `error` | Không biết (DOC-30 §5.3) |
| bất kỳ ≠ 0 | rỗng hoặc không phải JSON | Không biết: context không khởi động (E5) |
| 0 hoặc 1 | JSON không khớp mã (ví dụ mã 0 mà `violations` khác rỗng) | Không biết; lỗi code của `InvariantCliRunner` |

`run.sh` dùng: `jq -e 'has("checkedAt")' invariants.json >/dev/null || code=2`.

### Giao dịch và đồng thời

- **T1** là transaction duy nhất, `READ ONLY`, `REPEATABLE READ`, không khóa (DOC-30 §4). Nó chạy song song với giữ vé, job trả vé, webhook mà không chặn ai; snapshot giữ `xmin` vài giây (planned < 5 giây ở 1.000.000 unit, DOC-30 §3.4).
- **Chế độ job và CLI chạy cùng lúc** (người vận hành gõ `make invariants` khi `api` đang phục vụ): hai snapshot độc lập, vô hại. Kết quả có thể khác nhau nếu dữ liệu đổi giữa hai snapshot; mỗi kết quả đúng với snapshot của nó.
- **CLI không có job.** Profile `invariants` không bật `@EnableScheduling`, nên `ReleaseExpiredHoldsJob` của tiến trình CLI không trả vé và không che INV-08 (DR-179, IC-20). Job trả vé của container `api` đang phục vụ vẫn chạy bình thường.
- **Trong thực nghiệm**, `invariant.interval` của profile `experiment` là `PT1H` để lần chạy định kỳ không rơi vào cửa sổ đo; kiểm tra của lần chạy do CLI ở FL-19 bước 13 đảm nhận (DR-218).
- **Nếu tiến trình chết** (cả hai chế độ): T1 chỉ đọc, PostgreSQL tự hủy transaction khi mất kết nối; không có lease, không có dòng nào để dọn. Chế độ job: gauge giữ giá trị lần trước cho tới khi `api` lên lại và chạy ngay lần đầu. Chế độ CLI: `make` nhận mã ≠ 0 không có JSON → "không biết", chạy lại.
- **Không tự sửa, không tự dừng bán.** Có sai lệch chỉ là log, gauge và JSON (SDD gốc §14.3); dừng bán là việc của người vận hành theo RB-02 ("Dừng bán một sự kiện").

### Luồng lỗi

| Mã | Tình huống | Bước | Đầu ra | Người vận hành thấy |
| --- | --- | --- | --- | --- |
| E1 | Mất kết nối database (Hikari `connection-timeout`) | (a) 2, (b) 4 | Job: ERROR `invariant.failed {error: DATABASE_UNAVAILABLE}`, `ticket_job_runs_total{result="error"}`. CLI: mã 2, `{"error":"DATABASE_UNAVAILABLE"}` | RB-02 mục "Thoát 2": khởi động lại `postgres`, chạy lại |
| E2 | Một câu quá `invariant.statement-timeout` (30 giây) | (a) 5–7, (b) 4 | Job: như E1 với `STATEMENT_TIMEOUT` và `check`; các mục sau không chạy lần này. CLI: mã 2 | Chạy lại khi tải thấp; lặp lại thì `--invariant.statement-timeout=PT60S` (RB-02) |
| E3 | Lỗi SQL (cột đổi tên sau migration) | (a) 5–7, (b) 4 | `QUERY_FAILED`; CLI mã 2 | Sửa `InvariantQueries`; IC-01 đỏ trong CI trước khi tới đây |
| E4 | Có ít nhất một `VIOLATION` | (a) 12, (b) 6 | Log ERROR mỗi mục, gauge > 0; CLI mã 1 | RB-02 theo `INV-xx`; nhóm kho vé (INV-01…05) → tạm dừng sự kiện |
| E5 | Context không khởi động: `INVENTORY_STRATEGY` ≠ `skip-locked` thừa hưởng mà thiếu `-e`, `ConfigurationGuard` (DR-147) từ chối, hoặc không mở được kết nối khi Spring Data JDBC dò dialect | (b) 3 | JVM thoát 1, stdout rỗng, stack trace ở stderr | Bảng "Đọc kết quả": "không biết"; đọc stderr, sửa cấu hình, chạy lại |
| E6 | `postgres` chưa chạy khi gõ `make invariants` | (b) 2 | `--no-deps` không khởi động nó → E1 hoặc E5 | `make up` rồi chạy lại |

### Test bắt buộc

Mức theo DOC-69 §2 (I: tích hợp, Testcontainers hoặc stack compose; S: script trên stack compose). IC-01…25 (DOC-30 §10) kiểm từng mục và JSON; các ca dưới kiểm luồng qua các tầng.

| ID | Mức | Kịch bản | Kỳ vọng |
| --- | --- | --- | --- |
| FLO-01 | I | `api` khởi động với `invariant.interval=PT2S`, chờ 5 giây | `ticket_job_runs_total{job="invariant_checker",result="ok"}` ≥ 2; lần đầu ở ≤ 1 giây sau khi context sẵn sàng; log INFO `invariant.clean` |
| FLO-02 | I | Pool GA capacity 100; `UPDATE inventory_unit SET status='REMOVED'` một unit `AVAILABLE`; chờ lần chạy kế | Log ERROR `invariant.violation` `check=POOL_UNIT_COUNT`, `count=1`; gauge `POOL_UNIT_COUNT` = 1, 13 gauge khác = 0; unit vẫn `REMOVED` (không tự sửa) |
| FLO-03 | S | `make up seed`; `make invariants 2>err.txt >out.json` từ terminal tương tác | `out.json` đúng một dòng, `jq -e '.violations == []'` thành công; `err.txt` chứa `invariants: exit=0`; `make` thoát 0 |
| FLO-04 | S | Như FLO-02 trên stack compose, rồi `make invariants` | stderr `invariants: exit=1`; `make` thoát 2; `jq -r '.violations[0].id'` = `INV-01` |
| FLO-05 | S | `docker compose … stop postgres`; `make invariants` | `postgres` vẫn dừng sau lệnh (`--no-deps`); stderr `invariants: exit=` 1 hoặc 2; bảng "Đọc kết quả" cho "không biết" (stdout rỗng hoặc có `error`) |
| FLO-06 | S | `INVENTORY_STRATEGY=naive` trong `.env` của shell; `make invariants` trên stack `make up seed` | Container khởi động bình thường, mã 0 (`-e INVENTORY_STRATEGY=skip-locked` thắng) |
| FLO-07 | I | Hủy sự kiện có 3 đơn `PAID` (DR-28), chờ lần chạy định kỳ | Log INFO `invariant.report` `id=INV-15` `count=3`; không log ERROR; không gauge cho `REFUND_PENDING` |
| FLO-08 | S | Chạy `make invariants` trong lúc `make exp EXP=02 SCALE=0.01 RUNS=1` đang ở cửa sổ tải | Mã 0; `exp_http_outcome` của lần chạy không có `status` 500–599 tăng thêm so với lần chạy không gọi CLI |
| FLO-09 | U | `InvariantCliRunner` với `InvariantReport` có `violations` khác rỗng | `ExitCodeGenerator` trả 1; mỗi trường hợp ở bảng "Đọc kết quả" có một ca: hàm `classify(code, stdout)` của `run.sh` (bats) cho đúng kết luận |

## FL-19 · Chạy một thực nghiệm

- **UC / FR:** UC-21, FR-12.4, NFR-01…04 · **Màn hình:** không có · **Endpoint:** k6 gọi E-25, E-34, E-29 (fixture), E-14, E-16, E-18 và các endpoint của EXP qua `nginx` · **Sự kiện:** không
- **Trigger:** người vận hành gõ `make exp EXP=02 [STRATEGY=…] [SCALE=…] [RUNS=…]` (DOC-70 §7.1).
- **Điều kiện đầu:** máy đúng DOC-70 §3.1; image `api` build được; `jq` có trên host (DR-212); không chạy `make up` của stack dev cùng lúc (cùng tên project compose, bước 2 xóa volume của nó).

### Thành phần

| Tên | Loại | Mã | Định nghĩa ở |
| --- | --- | --- | --- |
| `Operator` | Tác nhân | — | DOC-04 UC-21 |
| `make` | Công cụ | target `exp` → `experiments/run.sh "$EXP" "$STRATEGY" "$SCALE" "$RUNS"`; `experiments/lib/*.sh`, `experiments/aggregate.sh` | DOC-70 §7, §15 |
| `k6` | Công cụ | `load/fixture.ts`, `load/expNN.ts`, `load/lib/multiuser.ts` | DOC-70 §4.1, §5.2 |
| `psql` | Công cụ | `experiments/seed/users.sql` | DOC-70 §5.1 |
| `nginx` | Edge | allowlist IP `k6` | DOC-70 §3.3 |
| `api` | API | profile `dev,fake-payments,experiment,obs`, `INVENTORY_STRATEGY=$STRATEGY` | DOC-70 §6.1 |
| `ReleaseExpiredHoldsJob` | Job | trả reservation hết hạn trong pha xả | DOC-24 §4.5 |
| `InvariantChecker` | CLI | như FL-18 (b), gọi thẳng `docker compose run` | FL-18 |
| `db` | Cơ sở dữ liệu | `app_user`, `session`, `organizer`, `event`, `inventory_*`, `reservation*`, `orders`, `ticket`, `pg_stat_*` | DOC-14, DOC-15 |
| `prometheus` | Công cụ | `http://localhost:9091/api/v1/query` | DOC-70 §8.2 |

### Sơ đồ tuần tự

Một lần chạy (một vòng của `RUNS`). Bước đánh số khớp DOC-70 §7.2 ở cột "DOC-70" của bảng "Chi tiết bước".

```mermaid
sequenceDiagram
  autonumber
  actor O as Operator
  participant M as make
  participant K as k6
  participant P as psql
  participant N as nginx
  participant A as api
  participant J as ReleaseExpiredHoldsJob
  participant I as InvariantChecker
  participant D as db
  participant R as prometheus
  O->>M: make exp EXP=02 STRATEGY=skip-locked SCALE=1 RUNS=3
  M->>M: Kiểm tham số; đọc experiments/exp02/params.env; nhân USERS, CAPACITY với SCALE
  M->>M: git status --porcelain, uptime, lscpu, free → env.json (máy, gitSha)
  alt E1 cây git bẩn
    M->>M: ghi invalidReasons += V1, vẫn chạy tiếp
  end
  M->>A: EXPCOMPOSE --profile obs --profile exp down -v
  M->>A: INVENTORY_STRATEGY, SPRING_APPLICATION_JSON=API_OVERRIDES; EXPCOMPOSE --profile obs up -d --build --wait
  A->>D: Flyway db/migration + migration-fake + migration-experiment
  alt E2 api không healthy (ConfigurationGuard, strategy thiếu profile)
    M->>M: status ERROR, invalidReasons V6; sang lần sau
  end
  M->>P: exec -T postgres psql -v users=100000 < users.sql
  P->>D: INSERT app_user, session, organizer; ANALYZE
  Note right of D: Mỗi câu autocommit. Nếu script chết ở đây: dữ liệu dở dang bị xóa ở bước reset của lần sau
  M->>K: run --rm k6 run /load/fixture.ts
  K->>N: POST /organizer/events (E-25), POST …/ticket-types (E-34), POST …/publish (E-29)
  N->>A: forward (IP 172.28.0.250 miễn limit_req)
  A->>D: transaction xuất bản tạo pool và 5.000 unit (DR-27)
  K->>N: Pha khởi động: 1.000 × (E-14 rồi E-16) trên sự kiện khởi động, 60 giây
  K-->>M: fixture.json {eventId, ticketTypeIds, poolIds, saleStartsAt, warmupEventId}
  M->>M: chờ tới T0 − 5 giây
  M->>D: SELECT pg_stat_statements_reset(); SELECT pg_stat_reset(); chụp pg_stat_database
  M->>M: bật sample-docker.sh (5 giây), sample-pg.sh (1 giây)
  M->>K: run --rm k6 run /load/exp02.ts -e FIXTURE=… --summary-export=…
  loop 100.000 người dùng ảo, đến đều trong W = 60 giây từ T0
    K->>N: POST /events/{id}/reservations (E-14) + Idempotency-Key
    N->>A: forward
    A->>D: FL-13: T idempotency → reservation → claim SKIP LOCKED → response, commit
    alt 503 OVERLOADED / 429 / lỗi mạng
      K->>K: chờ Retry-After, gửi lại cùng key (≤ 10 lần)
    end
  end
  K-->>M: thoát; T1 = now; k6-summary.json
  M->>D: M6 bán vượt (SQL DOC-70 §8.2), trước pha xả
  M->>M: chờ DRAIN_S (40) + 10 giây
  J->>D: (nền) FL-16 trả reservation hết hạn, nếu EXP có hết hạn
  M->>M: dừng bộ lấy mẫu; chụp pg_stat_database lần hai
  M->>R: collect.sh: PromQL M2, M3, M9, M10 tại Te
  M->>D: collect.sh: SQL M4, M5, n_dead_tup
  alt E7 prometheus không trả lời
    M->>M: status ERROR, V6
  end
  M->>I: EXPCOMPOSE run --rm --no-deps -T -e INVENTORY_STRATEGY=skip-locked api --spring.profiles.active=invariants
  I->>D: T1 READ ONLY, REPEATABLE READ (FL-18)
  I-->>M: invariants.json, invariants.exit
  alt E6 không biết (mã 2 hoặc không có JSON)
    M->>I: chờ 10 giây, chạy lại một lần
    opt vẫn không biết
      M->>M: invalidReasons += V5
    end
  end
  M->>M: Phân loại theo DOC-70 §9.1, §9.2 → verdict VALID / INVALID / ERROR trong env.json
  alt E5 skip-locked hoặc counter, invariants.exit = 1
    M-->>O: in "INVARIANT VIOLATION: EXP-02 FAILS" + violations[].id; summary.md đánh dấu không đạt
    O->>O: Mở issue kèm invariants.json; điều tra theo RB-02
  end
  Note over M: Lặp tới RUNS lần hợp lệ (tối đa 5 lượt, DOC-70 §8.3)
  M->>M: aggregate.sh EXP-02 → summary.md
  M-->>O: In bảng kết quả (DOC-70 §11)
```

### Chi tiết bước

| Bước | DOC-70 | Từ → Đến | Lệnh | Dữ liệu | Quy tắc | Lỗi → xử lý |
| --- | --- | --- | --- | --- | --- | --- |
| 1–2 | §7.1, §6.2 | `Operator` → `make` | `make exp EXP=02 STRATEGY=skip-locked SCALE=1 RUNS=3` | `params.env`: `USERS=100000`, `CAPACITY=5000`, `ARRIVAL_WINDOW_S=60`, `VUS=200`, `DRAIN_S=40`, `API_OVERRIDES='{}'` | Thiếu `EXP`, `SCALE` ngoài `(0, 1]`, `STRATEGY` lạ → in cách dùng, thoát 2, không đụng stack | — |
| 3 | 1 | `make` | `git status --porcelain`, `uptime`, `lscpu`, `free -b`, `git rev-parse HEAD` | `env.json.machine`, `gitSha` | Load 1 phút ≥ 1,0 chỉ cảnh báo; cây bẩn → V1 | E1 |
| 5 | 2 | `make` → stack | `$EXPCOMPOSE --profile obs --profile exp down -v` | — | Xóa volume database, storage, Mailpit, Prometheus: mỗi lần chạy độc lập (DR-208) | — |
| 6–7 | 3 | `make` → `api` → `db` | `INVENTORY_STRATEGY=$STRATEGY SPRING_APPLICATION_JSON="$API_OVERRIDES" $EXPCOMPOSE --profile obs up -d --build --wait` | `env.json.compose` (`docker compose config`), `env.json.postgres` (`SHOW ALL` rút gọn) | `api` từ chối `counter`/`naive` khi thiếu profile `experiment` (DR-76); `ConfigurationGuard` kiểm `API_OVERRIDES` (DR-147); profile `experiment` đặt `invariant.interval=PT1H` (DR-218) | E2 |
| 9–10 | 4 | `make` → `psql` → `db` | `$EXPCOMPOSE exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -v users=100000 -U "$POSTGRES_USER" "$POSTGRES_DB"' < experiments/seed/users.sql` | 100.001 `app_user` và `session`, 1 `organizer` | Script từ chối khi thiếu `inventory_pool_counter` (không có profile `experiment`) | psql thoát ≠ 0 → E2 (V6) |
| 11–15 | 5 | `make` → `k6` → `nginx` → `api` | `$EXPCOMPOSE --profile exp run --rm k6 run /load/fixture.ts --out json=/results/raw/fixture.ndjson` | `fixture.json`: `{"eventId":"0199…","ticketTypeIds":["0199…"],"poolIds":["0199…"],"saleStartsAt":"2026-11-03T10:14:00Z","warmupEventId":"0199…"}` | Sự kiện tạo qua API thật bằng người tổ chức `n = 0` (DR-27); `T0` làm tròn lên phút | E-25/E-34/E-29 lỗi → k6 thoát ≠ 0 → V6 |
| 16–18 | 6–7 | `make` → `db` | `SELECT pg_stat_statements_reset(); SELECT pg_stat_reset();`; bật `sample-docker.sh`, `sample-pg.sh` | `raw/docker-stats.csv`, `raw/pg-activity.csv` | Bộ lấy mẫu chạy nền tới bước 25; `trap` dừng chúng khi `run.sh` thoát (DR-221) | — |
| 19–25 | 8 | `make` → `k6` → … → `db` | `$EXPCOMPOSE --profile exp run --rm k6 run /load/exp02.ts -e FIXTURE=/results/fixture.json --summary-export=/results/k6-summary.json` | Mỗi lệnh giữ: `{"items":[{"ticketTypeId":"0199…","quantity":3}]}`, 1–4 vé (DOC-72) | Hành vi khi 503/429/lỗi mạng theo DOC-70 §4.1; transaction từng request theo FL-13 | k6 OOM hoặc lag > 1 giây → V2 (E3) |
| 26 | 8 | `make` | `T1=$(date +%s)` | `env.json.t1` | — | — |
| 27 | §8.2 M6 | `make` → `db` | câu M6 của DOC-70 §8.2 | `metrics.json.oversold = 0` | Đo **trước** pha xả, khi reservation chưa kịp hết hạn | — |
| 28–30 | 9 | `make`; `ReleaseExpiredHoldsJob` → `db` | `sleep $((DRAIN_S + 10))` | — | `DRAIN_S = 2 × reservation.expiry.interval + reservation.expiry.lease` = 2 × 5 + 30 = 40 giây; job trả vé chạy như thật | — |
| 31–34 | 10 | `make` → `prometheus`, `db` | `experiments/lib/collect.sh` | `metrics.json` M2…M13 | Truy vấn tại `Te = T1 + DRAIN_S + 10`, cửa sổ `R = Te − T0` | E7 |
| 35–40 | 11 | `make` → `InvariantChecker` → `db` | `$EXPCOMPOSE run --rm --no-deps -T -e INVENTORY_STRATEGY=skip-locked api --spring.profiles.active=invariants > invariants.json; echo $? > invariants.exit` | `invariants.json`, `invariants.exit` | Đọc theo bảng "Đọc kết quả" của FL-18; `durationMs` là số đo thời gian kiểm tra trên dữ liệu đầy đủ (thay cho metric, DR-218) | E6 |
| 41 | 12 | `make` | phân loại | `env.json.verdict`, `invalidReasons` | DOC-70 §9.1 (V1…V6), §9.2 (theo chiến lược) | E5 |
| 44–45 | §12 | `make` | `experiments/aggregate.sh EXP-02` | `summary.md`, bảng ra stdout | Chỉ gộp lần `VALID`; tính đúng ghi cả 3 giá trị, hiệu năng trung vị (min–max) | Thiếu lần hợp lệ sau 5 lượt → "không đủ lần hợp lệ" |

### Giao dịch và đồng thời

- **FL-19 không có transaction riêng của ứng dụng.** Mọi ghi vào kho vé, reservation, đơn, vé thuộc FL-08 (xuất bản), FL-13 (giữ vé), FL-14 (nhận vé 0 đồng), FL-15 (hủy giữ), FL-16 (job trả vé) và tuân thủ điểm commit của các luồng đó. `users.sql` là các câu autocommit trong một phiên `psql`; `ON CONFLICT DO NOTHING` làm nó chạy lại được.
- **Cái gì commit cùng nhau với kết quả:** không có. Kết quả là file trên đĩa host (`experiments/results/EXP-NN/<UTC>-<strategy>-s<SCALE>-r<n>/`), ghi tuần tự theo bước; thư mục của một lần chạy dở vẫn còn để điều tra và có `status` trong `env.json`.
- **Kiểm tra bất biến song song với `api` đang chạy.** Bước 11 chạy khi `api` và `ReleaseExpiredHoldsJob` vẫn sống; snapshot `REPEATABLE READ` thấy trạng thái đã commit nhất quán (DOC-30 §4). Pha xả 40 giây bảo đảm reservation hết hạn trong tải đã được trả, nên INV-08 (`ACTIVE` quá hạn > 60 giây) không đỏ oan.
- **Lần chạy định kỳ của `InvariantCheckJob`** không rơi vào cửa sổ đo: với `invariant.interval=PT5M`, lần thứ hai rơi vào khoảng `T0 + 2,5 phút`, giữa đuôi tải và pha xả của EXP-02 (≈ 7 phút một lần, DOC-70 §7.3), quét toàn bảng và làm nhiễu M9–M12; profile `experiment` đặt `PT1H` (DR-218).
- **Hai `make exp` cùng lúc** trên một máy dùng chung tên project compose: lần sau `down -v` xóa dữ liệu của lần trước. `run.sh` giữ khóa `flock experiments/results/.lock`; lần thứ hai in "another experiment is running" và thoát 2 (DR-221).
- **Nếu tiến trình chết:**
  - `run.sh` bị Ctrl-C hoặc chết ở bất kỳ bước nào: `trap` dừng bộ lấy mẫu, ghi `status: "ERROR"`, `invalidReasons: ["INTERRUPTED"]` vào `env.json`, không chạy `aggregate.sh`, thoát 130; stack để nguyên để điều tra (`make logs S=api`). Lần `make exp` sau bắt đầu bằng `down -v` (DR-221).
  - `api` chết ngoài kịch bản trong lúc tải: request của k6 nhận lỗi mạng và gửi lại cùng key tới khi hết 10 lần; DOC-62 không đặt `restart` cho `api` nên nó không tự lên lại; lần chạy bị loại theo V4 (`docker inspect` thấy container đã thoát hoặc `OOMKilled`). Dữ liệu đã commit vẫn nhất quán và bước 11 vẫn kiểm được vì CLI chỉ cần `postgres`.
  - Container `k6` chết: `k6-summary.json` thiếu → bước 8 lỗi → V6.
  - Container CLI chết ở bước 11: mã ≠ 0 không JSON → "không biết" → chạy lại một lần (E6).

### Luồng lỗi

| Mã | Tình huống | Bước | Đầu ra | Người vận hành làm |
| --- | --- | --- | --- | --- |
| E1 | Cây git bẩn | 3 | `invalidReasons: ["V1"]`, lần chạy vẫn hoàn tất | Hợp lệ để thử script (`SCALE=0.01`); không chép vào §Kết quả; commit rồi chạy lại |
| E2 | `api` không healthy: `ConfigurationGuard` từ chối `API_OVERRIDES`, `STRATEGY` thiếu profile `experiment`, migration lỗi | 6–7, 9–10 | `status: "ERROR"`, `V6`; `make logs S=api` có lý do; sang lần sau | Sửa `params.env` hoặc compose; chạy lại |
| E3 | Máy sinh tải nghẽn: `exp_schedule_lag` p95 > 1.000 ms hoặc CPU `k6` ≥ 95 % trần > 10 % mẫu; k6 OOM | 19–25 | `V2` | Giảm `SCALE` hoặc tăng `VUS` theo S-06 (DOC-70 §4.3) |
| E4 | Swap hoặc container khởi động lại ngoài kịch bản | 19–30 | `V3`, `V4` | Đóng ứng dụng khác; xem `docker inspect` |
| E5 | `skip-locked`/`counter`: `invariants.exit = 1` | 41 | Loại khỏi thống kê; `summary.md` ghi "không đạt" kèm `INV-xx`; stdout in `INVARIANT VIOLATION` | Mở issue kèm `invariants.json`; RB-02; **không** chạy lại để lấy lần sạch trước khi sửa lỗi (DOC-70 §9.2, DR-209) |
| E6 | Kiểm tra bất biến "không biết" (mã 2 hoặc không JSON) hai lần liền | 35–40 | `V5` | Xem stderr của CLI; RB-02 mục "Thoát 2" |
| E7 | `prometheus` không trả lời hoặc thiếu target | 31–34 | `status: "ERROR"`, `V6` | `curl -s localhost:9091/api/v1/targets` |
| E8 | `naive` cho `invariants.exit = 0` | 41 | `VALID`, ghi chú "không chứng minh được bán vượt" | Tăng tranh chấp (thu hẹp `W`) theo DOC-70 §9.2 |
| E9 | Đã có `make exp` khác đang chạy | 1 | "another experiment is running", thoát 2, không đụng stack | Chờ lần kia xong |
| E10 | Người vận hành ngắt (Ctrl-C) | bất kỳ | `INTERRUPTED`, thoát 130, stack còn chạy | `make exp …` lại; lần sau tự reset |

Quy tắc đọc `invariants.exit` theo chiến lược và điều kiện "đạt" của một EXP ở DOC-70 §9.2, §9.3; bảng trên chỉ nêu luồng đi tới đó.

### Test bắt buộc

Mức S: chạy trên máy dev bằng `SCALE=0.01 RUNS=1` (≈ 1.000 người dùng, pool 50); đây là kiểm chứng của P2-13, không phải số đo thực nghiệm.

| ID | Mức | Kịch bản | Kỳ vọng |
| --- | --- | --- | --- |
| FLO-10 | S | `make exp` không có `EXP`; `make exp EXP=02 SCALE=2` | In cách dùng, thoát 2; `docker compose ps` không đổi |
| FLO-11 | S | Cây sạch; `make exp EXP=02 SCALE=0.01 RUNS=1` | Thư mục `experiments/results/EXP-02/<UTC>-skip-locked-s0.01-r1/` có `env.json` (`verdict = "VALID"`), `fixture.json`, `k6-summary.json`, `metrics.json` (`oversold = 0`), `invariants.json`, `invariants.exit` = `0`; `summary.md` có dòng "Tổng hợp"; bảng in ra stdout |
| FLO-12 | S | Như FLO-11 với một file sửa chưa commit | `invalidReasons` có `V1`; lần chạy vẫn đủ file; `aggregate.sh` không gộp lần này |
| FLO-13 | S | `make exp EXP=02 STRATEGY=naive SCALE=0.01 RUNS=1` (nếu không đủ tranh chấp, `params.env` đặt `ARRIVAL_WINDOW_S=5`) | `invariants.exit = 1`, `violations[].id` có `INV-05`; `verdict = "VALID"`; `metrics.json.oversold > 0` |
| FLO-14 | S | Như FLO-11, script test chèn `UPDATE inventory_unit SET status='REMOVED'` một unit `AVAILABLE` giữa bước 9 và 11 | `invariants.exit = 1` (`INV-01`); lần chạy loại khỏi thống kê; `summary.md` ghi "không đạt" |
| FLO-15 | S | `API_OVERRIDES='{"invariant":{"active-overdue":"PT1S"}}'` (vi phạm luật 12 của `ConfigurationGuard`) | `api` không healthy; `env.json.status = "ERROR"`, `V6`; lần kế tiếp vẫn chạy |
| FLO-16 | S | Dừng `postgres` ngay trước bước 11 (hook test) | CLI chạy hai lần cách 10 giây; `invalidReasons` có `V5`; `postgres` không được CLI khởi động lại (`--no-deps`) |
| FLO-17 | S | `RUNS=2` liên tiếp | Lần 2: `SELECT count(*) FROM event` = 2 (sự kiện đo và sự kiện khởi động), không phải 4 (volume đã reset) |
| FLO-18 | S | Như FLO-11 | `increase(ticket_job_runs_total{job="invariant_checker"}[R])` = 0 trong cửa sổ đo (DR-218) |
| FLO-19 | S | Gửi SIGINT cho `make exp` khi đang ở bước 8 | Thoát 130; `env.json` có `INTERRUPTED`; không còn tiến trình `sample-*.sh`; lần `make exp` sau chạy bình thường |
| FLO-20 | S | Hai `make exp` cùng lúc | Lệnh thứ hai thoát 2 với "another experiment is running"; lệnh thứ nhất hoàn tất `VALID` |

## Quyết định mới khi viết tài liệu này

Mỗi quyết định nhỏ, dễ đảo ngược; Claude chốt theo ủy quyền.

**DR-218 · Profile `experiment` đặt `invariant.interval = PT1H`; thời gian kiểm tra đo từ `invariants.json`.** *Vấn đề:* `InvariantCheckJob` chạy ngay khi `api` khởi động rồi mỗi 5 phút. Mỗi lần thực nghiệm dựng lại `api` ở bước 3 nên lần thứ hai rơi vào khoảng `T0 + 2,5 phút`: đuôi tải hoặc pha xả của EXP-02, EXP-04. Lần quét toàn bảng đó chiếm CPU, connection và giữ snapshot, làm nhiễu M9–M12 một cách ngẫu nhiên giữa các lần lặp. DOC-30 §3.4 lại định đo thời gian kiểm tra bằng `ticket_invariant_check_seconds` trong EXP-02. *Quyết định:* `application-experiment.yml` đặt `invariant.interval: PT1H` (lần chạy lúc khởi động trên database rỗng vẫn có); kiểm tra của lần chạy do CLI ở bước 11 đảm nhận; thời gian kiểm tra trên dữ liệu đầy đủ lấy từ `invariants.json.durationMs`. *Hệ quả:* DOC-34 dòng `invariant.interval` cột Profile thêm "`experiment`: `PT1H`"; DOC-30 §3.4 đổi nguồn số đo thành `durationMs` của `make exp`; DOC-70 §8.2 có thể thêm số đo "thời gian kiểm tra bất biến" từ `invariants.json`. *Ghi vào:* DOC-86, DOC-34, DOC-30 §3.4, DOC-70.

**DR-219 · Target `make invariants`: `@`, `--no-deps`, `-T`, mã thoát in ra stderr; quy tắc "không có JSON thì không biết".** *Vấn đề:* (1) GNU make luôn thoát 2 khi recipe lỗi, nên `make invariants; echo $?` không phân biệt "có sai lệch" (1) với "không chạy hết" (2), trái với DOC-30 §5.3, DOC-61 và RB-02 bước "Bắt đầu"; (2) không có `@` thì `make` in dòng lệnh ra stdout, `make invariants > inv.json` không còn là JSON; (3) `docker compose run` cấp TTY khi chạy từ terminal, gộp stderr (log) vào stdout; (4) `docker compose run` khởi động service phụ thuộc, nên khi `postgres` đã dừng nó tự dựng lại (sai với IC-18) và một lệnh chỉ đọc đổi trạng thái stack; (5) Spring Boot thoát 1 khi context không khởi động (cấu hình sai, không dò được dialect), trùng mã với "có sai lệch". *Quyết định:* recipe `@$(COMPOSE) run --rm --no-deps -T -e INVENTORY_STRATEGY=skip-locked api --spring.profiles.active=invariants; code=$$?; echo "invariants: exit=$$code" >&2; exit $$code`. Mã chính xác đọc ở dòng stderr hoặc gọi thẳng `docker compose run` (như `run.sh`). Mọi nơi đọc kết quả theo bảng "Đọc kết quả" ở FL-18: mã ≠ 0 mà stdout không phải JSON có `checkedAt` → "không biết", như mã 2. *Hệ quả:* RB-02 bước "Bắt đầu" 1 và "Xác nhận" 1 đọc dòng `invariants: exit=` thay cho `$?`; DOC-30 §5.1 thêm `--no-deps -T -e …`, §5.3 thêm câu về mã của `make` và E5; IC-18 chấp nhận "mã 2 có `error`" hoặc "mã ≠ 0 không JSON" nếu Spring Data JDBC dò dialect lúc khởi động; DOC-61 dòng `make invariants` ghi "`make` thoát 0 hoặc 2; mã 0/1/2 ở stderr". *Ghi vào:* DOC-86, DOC-30 §5, DOC-61 §3, RB-02, DOC-70 §7.2 bước 11.

**DR-220 · Thêm `prometheus` vào danh sách công cụ của DOC-82 §1; `make` bao cả `experiments/run.sh`.** *Vấn đề:* FL-19 cần participant cho Prometheus (thu M2, M3, M9, M10) và cho script thực nghiệm; DOC-82 §1 chỉ liệt kê `make`, `k6`, `psql`. *Quyết định:* dòng "Công cụ" của DOC-82 §1 thành `make`, `k6`, `psql`, `prometheus`; `make` trong sơ đồ bao cả target và script nó gọi (`experiments/run.sh`, `lib/*.sh`, `aggregate.sh`). *Hệ quả:* không thêm loại participant mới. *Ghi vào:* DOC-82 §1, DOC-86.

**DR-221 · `run.sh` giữ khóa một lần chạy, dọn khi bị ngắt.** *Vấn đề:* hai `make exp` cùng lúc dùng chung tên project compose: lần sau `down -v` xóa dữ liệu lần trước giữa chừng; Ctrl-C để lại bộ lấy mẫu chạy nền và thư mục kết quả không có trạng thái. *Quyết định:* `run.sh` lấy `flock -n experiments/results/.lock`, không lấy được thì in "another experiment is running" và thoát 2; `trap` trên `INT`, `TERM`, `EXIT` dừng `sample-*.sh`, ghi `status: "ERROR"`, `invalidReasons: ["INTERRUPTED"]` khi chưa tới bước 12, không chạy `aggregate.sh`, thoát 130 với ngắt; stack để nguyên. *Hệ quả:* `.lock` nằm trong `experiments/results/.gitignore`; `flock` (util-linux) là công cụ cần có trên máy thực nghiệm Linux. *Ghi vào:* DOC-86, DOC-70 §7.2, §15, DOC-61 §1.

## Câu hỏi còn mở

Không có. Quyết định phát sinh là DR-218…221 (mục trên).
