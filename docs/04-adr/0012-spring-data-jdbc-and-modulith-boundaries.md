# ADR-0012: Spring Data JDBC (trạng thái đổi bằng @Modifying @Query có điều kiện), ranh giới module bằng Spring Modulith

- Status: Accepted
- Date: 2026-10-06 · Related: DR-05, DOC-11, DOC-12, DOC-07

## Bối cảnh

Mọi đổi trạng thái phải là câu ghi có điều kiện, trong khi `save()` của Spring Data ghi đè cả dòng không điều kiện. Transaction giữ vé chạm bảng của bốn module. Ranh giới "không truy vấn bảng của nhau" cần được ép bằng công cụ. (Nguồn: DR-05.)

## Các phương án

1. **`JdbcClient` thuần.** Toàn SQL tay, nhiều mã lặp cho CRUD của studio.
2. **JPA.** Lazy loading, `save()` không điều kiện, khó kiểm soát câu SQL ở đường nóng.
3. **Spring Data JDBC cho aggregate + SQL viết tay trong custom fragment.**

## Quyết định

Chọn phương án **3**.
- Aggregate có `save()` để chèn và sửa trường không phải trạng thái; `inventory_unit` và `inventory_pool` không là aggregate.
- Không bao giờ đổi `status` bằng `save()`: dùng `@Modifying @Query("UPDATE … WHERE … AND status = :from")` trả `int`, service kiểm tra.
- ID UUIDv7 gán trước, chèn bằng `JdbcAggregateTemplate.insert`; `Reservation` chỉ `save()` một lần; converter `jsonb` qua `PGobject`.
- SQL phức tạp (claim `SKIP LOCKED`, `generate_series`, snapshot, bất biến) trong custom fragment dùng `JdbcClient`.
- Ranh giới module: Spring Modulith `verify()` + ArchUnit (layer, `@Transactional`, sở hữu bảng, không `save()` đổi `status`).

## Hệ quả

**Tích cực**
- Bớt mã lặp CRUD, vẫn giữ SQL tay ở đường nóng; không có N+1 ẩn.
- Quy tắc được ép bằng test.

**Tiêu cực**
- Aggregate có `@MappedCollection` bị xóa-chèn lại khi `save()`.
- Phụ thuộc Spring Data JDBC của Boot 4 (kiểm ở S-01).

**Việc phát sinh**
- P1-05; checklist review ở DOC-12 §7.
