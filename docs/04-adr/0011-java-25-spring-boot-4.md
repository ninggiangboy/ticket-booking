# ADR-0011: Java 25 và Spring Boot 4

- Status: Accepted
- Date: 2026-10-06 · Related: DR-02, DOC-11, spike S-01

## Bối cảnh

SDD gốc chọn Java 21 và Spring Boot 3, nhưng tại 2026-10 Java 25 là LTS mới nhất, Spring Boot 4.x là dòng được hỗ trợ và Boot 3.5 đã hết hỗ trợ OSS; bắt đầu trên dòng hết hỗ trợ buộc phải nâng cấp lớn giữa chừng. (Nguồn: DR-02.)

## Các phương án

1. **Java 21 + Boot 3.5.** Đúng SDD, thư viện chắc chắn tương thích nhưng hết hỗ trợ OSS.
2. **Java 25 + Boot 4.0.x.** Hỗ trợ dài, structured logging sẵn; cần kiểm tra springdoc, stripe-java, Testcontainers, Spring Modulith.

## Quyết định

Chọn phương án **2**, có điều kiện của spike S-01.
- Java 25 (Temurin), Spring Boot 4.0.x, patch khóa trong version catalog ở P1-01.
- S-01 dựng ứng dụng rỗng với toàn bộ thư viện của DOC-11 §8 và chạy một test tích hợp. Thư viện nào thất bại thì **lùi toàn bộ** về phương án 1, không trộn, và ghi ADR mới thay ADR này.
- Không dùng virtual thread cho đường giữ vé (số luồng phải nhỏ hơn connection pool); đo lại ở EXP-05.

## Hệ quả

**Tích cực**
- Dòng được hỗ trợ lâu; không phải nâng cấp lớn sớm.

**Tiêu cực**
- Hệ sinh thái mới có thể thiếu tương thích; rủi ro dồn vào S-01.
- Tài liệu mạng cho Boot 4 còn ít.

**Việc phát sinh**
- Chạy S-01 (P0-02) và điền DOC-11 §8.
