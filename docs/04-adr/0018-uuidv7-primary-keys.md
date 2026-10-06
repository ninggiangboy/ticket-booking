# ADR-0018: Khóa chính UUIDv7

- Status: Accepted
- Date: 2026-10-06 · Related: DR-11, DOC-14

## Bối cảnh

ID xuất hiện trong URL nên không được đoán được; ID tăng dần làm lộ số đơn. `inventory_unit` chèn tới 100.000 dòng mỗi lần xuất bản nên index khóa chính không được phân mảnh. (Nguồn: DR-11.)

## Các phương án

1. **`bigint identity` + ID công khai riêng.** Hai cột mỗi bảng.
2. **UUIDv4.** Ngẫu nhiên, index B-tree phân mảnh khi chèn nhiều.
3. **UUIDv7.** Có thứ tự thời gian, chèn vào cuối index, phần ngẫu nhiên không đoán được.

## Quyết định

Chọn phương án **3**.
- Mọi bảng nghiệp vụ có khóa chính `uuid` tên `<bảng>_id`, mặc định `uuidv7()` của PostgreSQL 18; Java sinh trước khi cần biết ID (ví dụ `reservation_id` trước khi claim).
- Ngoại lệ: ID ghế, section, row, zone, decoration trong tài liệu sơ đồ do client sinh (DR-32); ID Stripe lưu `text`.

## Hệ quả

**Tích cực**
- Chèn nhanh, không phân mảnh; ID không đoán được.

**Tiêu cực**
- UUID 16 byte lớn hơn `bigint`; lộ thời điểm tạo trong ID.

**Việc phát sinh**
- Cần PostgreSQL 18 (có `uuidv7()`).
