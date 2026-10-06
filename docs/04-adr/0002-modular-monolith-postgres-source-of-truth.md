# ADR-0002: Modular monolith, bên trong mỗi module chia layer, PostgreSQL là nguồn chuẩn duy nhất

- Status: Accepted
- Date: 2026-10-06 · Related: DR-05, DR-06, DR-79, DOC-07, DOC-12, NFR-01

## Bối cảnh

Tính đúng đắn của giữ vé và đơn hàng dựa vào transaction cục bộ của database. Dự án do một người làm để học, nên cấu trúc code phải quen thuộc và ranh giới module phải được ép bằng test chứ không bằng review. (Nguồn: SDD gốc 4, DR-05, DR-06, DR-79.)

## Các phương án

1. **Microservice.** Tách theo miền — cần transaction phân tán cho giữ vé, không có lợi ích ở quy mô này.
2. **Monolith không chia module.** Đơn giản nhất nhưng ranh giới rỗng, khó kiểm soát phụ thuộc.
3. **Modular monolith, module chia layer (n-layer), kiểm bằng Spring Modulith + ArchUnit.**

## Quyết định

Chọn phương án **3**.
- Một ứng dụng `api`, job nền cùng tiến trình, PostgreSQL là nguồn chuẩn duy nhất; Redis chỉ giảm tải.
- Gói gốc `io.ticket`; mỗi module `io.ticket.<module>` chia `controller`/`job`/`listener` → `service` → `repository`/`client`, cùng `entity`, `dto` (DR-06).
- Mỗi bảng và họ key Redis có đúng một module sở hữu; module khác chỉ dùng package gốc (`…Api`, DTO, event); không vòng phụ thuộc.
- Đồ thị phụ thuộc cụ thể, module `studio` và `invariant` chỉ đọc theo DR-79; khai báo bằng `@ApplicationModule(allowedDependencies)`.
- Kiểm tự động: Modulith `verify()` và ArchUnit (DOC-12 §5).

## Hệ quả

**Tích cực**
- Người mới chỉ nhớ một khuôn package; ranh giới được ép bằng test.
- Transaction giữ vé và xác nhận nằm trong một database.

**Tiêu cực**
- Mỗi module phải có `…Api` và DTO riêng; nhiều class ánh xạ.
- Cache Caffeine và job chỉ đúng với một bản sao API.
- Thêm module `studio` để tránh vòng phụ thuộc.

**Việc phát sinh**
- P1-05: khung module và test kiến trúc.
- Nếu cần nhiều bản sao: chuyển cache dùng chung.
