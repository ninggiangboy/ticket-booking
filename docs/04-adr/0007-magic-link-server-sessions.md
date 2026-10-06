# ADR-0007: Đăng nhập bằng magic link và session phía server

- Status: Accepted
- Date: 2026-10-06 · Related: DR-14, DR-21, DR-22, DR-23, DOC-19, NFR-07

## Bối cảnh

Chỉ có một cách đăng nhập để không phải lưu hay làm lộ mật khẩu; bộ quét link của hộp thư có thể mở link trước người dùng; token và session phải lưu dạng hash (NFR-07). (Nguồn: SDD gốc 5, DR-21, DR-22.)

## Các phương án

1. **Mật khẩu.** Ngoài phạm vi.
2. **JWT không trạng thái.** Khó thu hồi, cần khóa ký.
3. **Magic link một lần + session ID ngẫu nhiên lưu hash ở server.**

## Quyết định

Chọn phương án **3**.
- Token 32 byte `SecureRandom` (base64url 43 ký tự), DB lưu SHA-256; hạn 15 phút, một lần, chỉ token mới nhất hợp lệ; giới hạn 3/email/15 phút và 10/IP/giờ đếm trong DB.
- Trang callback không tiêu thụ token khi tải; `POST /auth/verify` chạy `UPDATE login_token … WHERE used_at IS NULL AND superseded_at IS NULL AND expires_at > now() RETURNING`.
- Session: cookie `tb_session` `HttpOnly; Secure; SameSite=Lax`, DB lưu SHA-256, cache Caffeine 60 giây, hết hạn sau 30 ngày không hoạt động, `last_seen_at` cập nhật khi cũ hơn 1 giờ.
- CSRF synchronizer token (`X-CSRF-Token`). Không đăng nhập chéo thiết bị.

## Hệ quả

**Tích cực**
- Không có mật khẩu; email được xác minh ngay lần đầu.
- Thu hồi session tức thì (xóa dòng và cache).

**Tiêu cực**
- Phụ thuộc email đến kịp.
- Cache session chỉ đúng với một bản sao API.

**Việc phát sinh**
- P1-07, P1-08.
