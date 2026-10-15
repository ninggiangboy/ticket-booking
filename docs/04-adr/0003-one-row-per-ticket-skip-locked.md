# ADR-0003: Một dòng mỗi vé, claim bằng `SKIP LOCKED`

- Status: Proposed
- Date: 2026-10-14 · Related: DR-17, DR-41, DR-76, DR-155, FR-06, NFR-01, NFR-02, EXP-01, EXP-02, EXP-10, DOC-14, DOC-24

## Bối cảnh

Vé của một zone hoặc một loại vé GA không có danh tính riêng, nên cách lưu tự nhiên là một con số "còn lại". Khi hàng nghìn lệnh giữ vé cùng đổi con số đó, mọi transaction xếp hàng trên một dòng (hot row): thông lượng của pool bị chặn bởi thời gian giữ lock, và mỗi transaction đang chờ chiếm một connection, làm cạn connection pool của cả API (SDD gốc 10.3, 10.4). Ghế thì vốn đã là một dòng mỗi ghế. Yêu cầu đứng trên mọi yêu cầu khác là không bán vượt (NFR-01), nên kho vé phải nằm trong cùng transaction với reservation trong PostgreSQL (ADR-0002).

## Các phương án

1. **A · Một dòng bộ đếm mỗi pool** (`UPDATE … SET held = held + :q WHERE held + sold + :q <= capacity`). Đơn giản nhất, bất biến bằng một `CHECK`; nhưng hot row, trần thông lượng cố định mỗi pool.
2. **B · K dòng bộ đếm mỗi pool.** Giảm tranh chấp K lần nhưng không bỏ; vé cuối rải rác giữa các ngăn, phải gom có thứ tự để tránh deadlock; K phải đoán trước.
3. **C1 · Bộ đếm trong Redis là nguồn chuẩn.** Nhanh, không lock; bước trừ Redis và bước ghi database không nguyên tử → có thể bán vượt. Loại ngay.
4. **C2 · Redis làm cổng chặn trước A.** Không bán vượt nhưng hot row vẫn còn cho request thắng; thêm bộ đếm thứ hai phải đồng bộ.
5. **D · Một dòng `inventory_unit` mỗi vé, claim `qty` dòng `AVAILABLE` bất kỳ bằng `FOR UPDATE SKIP LOCKED`.** Không có dòng chung để tranh; cùng cơ chế với ghế.

## Quyết định

Chọn phương án **D** (SDD gốc 10.3).

- Bảng duy nhất `inventory_unit` cho cả SEAT, ZONE và GA (DOC-14 §4.4): ghế là unit có `seat_id`; vé zone/GA là unit vô danh có `pool_id`. Trạng thái `AVAILABLE`, `HELD`, `SOLD`, `REMOVED` (DR-17). Không cột bộ đếm nào; số còn lại là số unit `AVAILABLE`.
- Câu claim: `WITH picked AS (SELECT unit_id … WHERE pool_id = :p AND status = 'AVAILABLE' LIMIT :qty FOR UPDATE SKIP LOCKED) UPDATE … SET status = 'HELD', reservation_id = :rid`; số dòng phải bằng `:qty`, thiếu thì rollback cả lệnh (DOC-24 §4.2). Ghế dùng `seat_id = ANY(:ids)` trong cùng mẫu câu.
- `READ COMMITTED`; điều kiện `status = 'AVAILABLE'` được kiểm lại trên phiên bản mới nhất của dòng sau khi khóa.
- Index một phần `unit_pool_available_idx (pool_id) WHERE status = 'AVAILABLE'` cho câu chọn; xác nhận và trả vé dùng `unit_reservation_idx` (DOC-14 §5). Bảng `fillfactor = 80`, `autovacuum_vacuum_scale_factor = 0.02`.
- Xuất bản tạo đủ `capacity` dòng một lần bằng `generate_series` (DR-27); đổi sức chứa là thêm dòng hoặc chuyển dòng `AVAILABLE` sang `REMOVED` (DR-30).
- Phương án A (`CounterClaimer`) và một bản đọc-rồi-ghi không điều kiện (`NaiveClaimer`) được giữ sau interface `InventoryClaimer`, chỉ bật trong profile `experiment`, để EXP-10 và EXP-02 đo so sánh (DR-76).

## Hệ quả

**Tích cực**

- Không transaction nào chờ transaction nào trên kho vé; không deadlock; request thua trả 409 trong vài mili giây.
- Bất biến pool trở thành cấu trúc: không thể giữ dòng không tồn tại, không có số để âm hay lệch.
- Một bảng, một câu claim, một câu trả vé cho cả ba mô hình bán.
- Kho vé và reservation nằm trong một transaction PostgreSQL; Redis không cần thiết cho tính đúng.

**Tiêu cực**

- Số dòng bằng tổng sức chứa: sự kiện 100.000 chỗ là 100.000 dòng; xuất bản chèn tới 100.000 dòng trong một transaction (S-03 đo, mục tiêu < 5 giây).
- Claim, trả, xác nhận không phải HOT update (cột `status` và `reservation_id` nằm trong index một phần): mỗi lần đổi chèn 4 mục index (kể cả `unit_event_idx` của DR-155) và để lại dead tuple; bảng phụ thuộc autovacuum (DOC-24 §6).
- Gần hết vé có thể từ chối oan khi unit cuối đang bị transaction khác khóa rồi rollback.
- Số còn lại phải đếm trên index thay vì đọc một cột; đếm được cache hoặc làm gợi ý (cờ hết vé DR-46, snapshot DR-62).

**Việc phát sinh**

- S-03 (trước P2-00): thời gian chèn, tỉ lệ HOT, p95 claim sau churn, phình index (DOC-24 §6.3).
- P2-06: `SkipLockedClaimer` và test INVT-01, INVT-02; P2-13: `CounterClaimer`, `NaiveClaimer`.
- EXP-10 so A và D; EXP-02 chạy thêm trên `naive` để chứng minh bán vượt được tái hiện.
- Pool hàng triệu vé (ngoài phạm vi): chuyển sang cách của Shopify (giữ một cửa sổ dòng `AVAILABLE` và bổ sung dần) bằng ADR mới.
