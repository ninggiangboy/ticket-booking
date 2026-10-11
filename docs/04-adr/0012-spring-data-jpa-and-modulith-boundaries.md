# ADR-0012: Spring Data JPA (Hibernate) cho truy cập dữ liệu, trạng thái đổi bằng @Modifying @Query có điều kiện, ranh giới module bằng Spring Modulith

- Status: Accepted
- Date: 2026-10-06, đổi 2026-10-11 · Related: DR-05, DOC-11, DOC-12, DOC-07

## Bối cảnh

Mọi đổi trạng thái phải là câu ghi có điều kiện, trong khi `save()` và dirty checking ghi đè cả dòng không điều kiện. Transaction giữ vé chạm bảng của bốn module. Ranh giới "không truy vấn bảng của nhau" cần được ép bằng công cụ. (Nguồn: DR-05.)

## Các phương án

1. **`JdbcClient` thuần.** Toàn SQL tay, nhiều mã lặp cho CRUD của studio.
2. **Spring Data JDBC cho aggregate + SQL viết tay trong custom fragment dùng `JdbcClient`.** Phương án đã chốt 2026-10-06; không có ORM, mỗi thao tác CRUD vẫn phải tự ánh xạ nhiều.
3. **Spring Data JPA (Hibernate) cho CRUD; truy vấn đặc biệt viết bằng `@Query`.**

## Quyết định

Chọn phương án **3** (Owner đổi 2026-10-11).
- CRUD thường dùng entity `@Entity` và repository `JpaRepository`; không viết SQL tay cho chúng.
- Thao tác đặc biệt dùng `@Query` (JPQL hoặc `nativeQuery = true`): đổi trạng thái có điều kiện, claim `SKIP LOCKED`, `generate_series`, snapshot, bất biến, báo cáo. Không dùng `JdbcClient`.
- Không bao giờ đổi `status` bằng `save()`, setter hay dirty checking: cột `status` khai báo `@Column(updatable = false)` và không có setter; mọi chuyển trạng thái là `@Modifying(flushAutomatically = true, clearAutomatically = true) @Query("UPDATE … WHERE … AND status = :from")` trả `int`, service kiểm tra.
- Mọi quan hệ `LAZY`; `spring.jpa.open-in-view=false`; `spring.jpa.hibernate.ddl-auto=none` (Flyway sở hữu schema); đọc cho giao diện dùng projection hoặc `@EntityGraph`.
- ID UUIDv7 gán trước (DR-11), entity cài `Persistable<UUID>` để `save()` chạy `persist`; cột `jsonb` bằng `@JdbcTypeCode(SqlTypes.JSON)`; enum bằng `@Enumerated(EnumType.STRING)`; `@Version` cho `Event.rowVersion`.
- Ranh giới module: Spring Modulith `verify()` + ArchUnit (layer, `@Transactional`, sở hữu bảng, bảo vệ `status`).

## Hệ quả

**Tích cực**
- Ít mã lặp CRUD hơn; hỗ trợ cascade, `@Version`, batch của Hibernate.
- Quy tắc được ép bằng test.

**Tiêu cực**
- Lazy loading và dirty checking có thể gây N+1 hoặc ghi ngoài ý muốn; giảm bằng quan hệ `LAZY`, `@EntityGraph`, `updatable = false` và checklist review.
- Persistence context có thể giữ bản cũ sau `@Modifying`; giảm bằng `clearAutomatically`.
- Phụ thuộc Hibernate 7 và Spring Data JPA của Boot 4 trên Java 25 (cần kiểm khi chuyển mã).
- ARC-05 phải đọc thêm `@Table` và JPQL để biết bảng.

**Việc phát sinh**
- Mã P1-05 và P1-06 đang theo Spring Data JDBC (`JdbcConfig`, `OutboxRepository`, `NoStatusSaveTest`) cần chuyển; checklist review ở DOC-12 §7.
