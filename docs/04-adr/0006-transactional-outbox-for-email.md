# ADR-0006: Outbox giao dịch cho email; hủy PaymentIntent gọi trực tiếp

- Status: Accepted
- Date: 2026-10-06 · Related: DR-19, DR-21, DR-53, DOC-15, DOC-27

## Bối cảnh

Email vé, đổi lịch và chờ hoàn tiền phát sinh từ nghiệp vụ và không được mất khi SMTP lỗi; gửi email trong transaction vi phạm nguyên tắc không gọi ra ngoài. SDD gốc cũng đưa lệnh hủy PaymentIntent vào outbox, nhưng job trả vé cần biết kết quả hủy ngay để quyết định trả vé. (Nguồn: SDD gốc 4.2, DR-21, DR-53.)

## Các phương án

1. **Gửi trực tiếp trong request.** Đơn giản nhưng mất email khi lỗi và giữ transaction/connection.
2. **Outbox cho email và cả hủy PaymentIntent.** Bất đồng bộ nên job không biết khi nào hủy xong.
3. **Outbox chỉ cho email; hủy PaymentIntent gọi trực tiếp; magic link gửi trực tiếp sau commit.**

## Quyết định

Chọn phương án **3**.
- Bảng `outbox` (kind `EMAIL_TICKETS`, `EMAIL_EVENT_CHANGED`, `EMAIL_REFUND_PENDING`) ghi cùng transaction nghiệp vụ.
- `OutboxRelay` mỗi 1 giây: claim 50 dòng bằng `FOR UPDATE SKIP LOCKED` kèm lease 60 giây, gửi ngoài transaction, `SENT`; lỗi thì backoff `least(10 s × 2^(attempts−1), 1 giờ)`, `attempts ≥ 12` → `FAILED`.
- Giao ít nhất một lần; `Message-ID` cố định `<outbox_id@APP_DOMAIN>`.
- Hủy PaymentIntent do job trả vé và đường nhanh gọi trực tiếp (timeout 5 và 3 giây). Magic link gửi SMTP trực tiếp sau commit, không qua outbox (DR-21).

## Hệ quả

**Tích cực**
- Không mất email khi SMTP lỗi; vé vẫn xem được ở "Vé của tôi".
- Không cần mã hóa token trong outbox.

**Tiêu cực**
- Có thể gửi trùng (hộp thư gộp theo `Message-ID`).
- Magic link gặp SMTP lỗi thì người dùng phải bấm gửi lại.

**Việc phát sinh**
- P1-06: `OutboxRelay`.
- P1-07: gửi magic link trực tiếp sau commit, kèm mẫu email `magic-link`.
- Hoàn tiền tự động (giai đoạn sau) cần ADR riêng.
