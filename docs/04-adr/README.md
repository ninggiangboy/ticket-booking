# Danh mục ADR

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-13
> Phụ thuộc: [Master plan](../00-master-plan.md) §3.2, [Sổ quyết định](../00-decision-register.md), [ADR-0001](0001-record-architecture-decisions.md)
> Người dùng chính: P0-12; mọi người cần biết vì sao một quyết định kiến trúc được chọn

Mục lục các ADR (MADR rút gọn, mẫu A.1). Mỗi ADR một quyết định, không sửa nội dung; muốn đổi thì viết ADR mới và đánh dấu ADR cũ `Superseded by ADR-yyyy`. ADR chưa viết ghi `Chưa viết` và được viết trước gate của phase tương ứng.

| ADR | Tiêu đề | Trạng thái | Gate | Nguồn |
| --- | --- | --- | --- | --- |
| [ADR-0001](0001-record-architecture-decisions.md) | Ghi quyết định kiến trúc bằng ADR (MADR rút gọn) | Accepted | P1 | Quy ước |
| [ADR-0002](0002-modular-monolith-postgres-source-of-truth.md) | Modular monolith, bên trong mỗi module chia layer, PostgreSQL là nguồn chuẩn duy nhất | Accepted | P1 | SDD gốc 4, DR-06, DR-79 |
| ADR-0003 | Một dòng mỗi vé, claim bằng `SKIP LOCKED` | Chưa viết | P2 | SDD gốc 10.3 |
| ADR-0004 | Dòng reservation làm trọng tài, trạng thái `EXPIRING`; thanh toán đến trễ chuyển chờ hoàn tiền | Chưa viết | P2 | SDD gốc 8.4, DR-44 |
| ADR-0005 | Stripe PaymentIntent + Payment Element, chỉ thẻ, trạng thái đơn chỉ đổi theo webhook | Chưa viết | P3 | SDD gốc 9.1, DR-47 |
| [ADR-0006](0006-transactional-outbox-for-email.md) | Outbox giao dịch cho email; hủy PaymentIntent gọi trực tiếp | Accepted | P1 | SDD gốc 4.2, DR-53 |
| [ADR-0007](0007-magic-link-server-sessions.md) | Magic link và session phía server | Accepted | P1 | SDD gốc 5, DR-21, DR-22 |
| ADR-0008 | Redis chỉ để giảm tải; phòng chờ bằng Lua, luôn chạy | Chưa viết | P6 | SDD gốc 10.2, DR-57 |
| ADR-0009 | Tài liệu sơ đồ tham số, phiên bản bất biến | Chưa viết | P4 | SDD gốc 7.1, 7.8, DR-32 |
| ADR-0010 | Một sơ đồ cho mỗi sự kiện, dùng lại bằng nhân bản sinh ID mới | Chưa viết | P4 | DR-31 |
| [ADR-0011](0011-java-25-spring-boot-4.md) | Java 25 và Spring Boot 4 | Accepted | P1 | DR-02 |
| [ADR-0012](0012-spring-data-jdbc-and-modulith-boundaries.md) | Spring Data JDBC (trạng thái đổi bằng `@Modifying @Query` có điều kiện), ranh giới module bằng Spring Modulith | Accepted | P1 | DR-05 |
| ADR-0013 | Tình trạng chỗ dạng bitmap, snapshot cache trong tiến trình | Chưa viết | P5 | DR-62 |
| ADR-0014 | Store editor ngoài React, ghế vẽ bằng một Konva custom shape | Chưa viết | P4 | SDD gốc 7.9, 13.2, DR-39 |
| ADR-0015 | Ảnh lưu ở object storage tương thích S3, API phục vụ qua cache nginx | Chưa viết | P2 | DR-38 |
| [ADR-0016](0016-multilingual-ui-vi-en.md) | Giao diện đa ngôn ngữ `vi` (mặc định)/`en` | Accepted | P1 | DR-10 |
| ADR-0017 | Port cổng thanh toán với adapter giả | Chưa viết | P3 | DR-51 |
| [ADR-0018](0018-uuidv7-primary-keys.md) | Khóa chính UUIDv7 | Accepted | P1 | DR-11 |

## Câu hỏi còn mở

Không có.
