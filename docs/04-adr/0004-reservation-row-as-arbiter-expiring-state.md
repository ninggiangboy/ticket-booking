# ADR-0004: Dòng reservation làm trọng tài, trạng thái `EXPIRING`; thanh toán đến trễ chuyển chờ hoàn tiền

- Status: Proposed
- Date: 2026-10-14 · Related: DR-28, DR-42, DR-43, DR-44, DR-47, DR-90, DR-91, FR-07, FR-09, NFR-01, NFR-03, EXP-04, EXP-06, DOC-14, DOC-24, DOC-26

## Bối cảnh

Hai luồng cùng muốn kết thúc một reservation: webhook thanh toán muốn xác nhận (unit `HELD → SOLD`), job trả vé muốn giải phóng kho (unit `HELD → AVAILABLE`). Nếu job trả vé trước khi chắc chắn tiền không thể về, người mua đã trả tiền mà vé đã thuộc người khác. Hủy PaymentIntent là lời gọi Stripe ra ngoài, không được nằm trong transaction (SDD gốc 10.4), nên "kiểm tra rồi trả vé" không thể là một câu SQL. SDD gốc 9.5 còn có luồng "giữ lại đúng ghế" khi tiền về trễ, nhưng máy trạng thái 8.3 coi `EXPIRED`/`CANCELLED` là trạng thái cuối; hai chỗ mâu thuẫn (DR-44). Người mua hủy giữ (DR-43) và người tổ chức hủy sự kiện (DR-28) cũng đi đúng đường trả vé này.

## Các phương án

1. **Trả vé ngay khi hết hạn, xử lý tiền về trễ bằng claim lại** (SDD gốc 9.5). Trả vé nhanh; nhưng cần chuyển `EXPIRED → CONFIRMED`, phá máy trạng thái và có thể thất bại khi ghế đã bán cho người khác.
2. **Khóa dòng reservation `FOR UPDATE` trong khi gọi Stripe.** Đơn giản về logic; giữ connection và lock suốt lời gọi mạng tới 5 giây, vi phạm SDD gốc 10.4.
3. **Trạng thái trung gian `EXPIRING` trên dòng reservation; dòng đó là trọng tài duy nhất; kho vé chỉ trả sau khi PaymentIntent đã hủy chắc chắn hoặc chưa từng có.** Thanh toán đến sau khi reservation đã đóng không phát hành vé, đơn sang `REFUND_PENDING`.

## Quyết định

Chọn phương án **3** (SDD gốc 8.3, 8.4; DR-42, DR-43, DR-44).

- Máy trạng thái reservation: `ACTIVE → EXPIRING → EXPIRED | CANCELLED`, `ACTIVE | EXPIRING → CONFIRMED`; `CONFIRMED`, `EXPIRED`, `CANCELLED` là cuối (DOC-14 §6.3). `close_reason` (`TIMEOUT`, `BUYER_CANCELLED`, `EVENT_CANCELLED`) được đặt khi vào `EXPIRING` và quyết định trạng thái cuối (DR-90).
- Mọi bên muốn kết thúc reservation chuyển trạng thái bằng một câu có điều kiện trên chính dòng đó, kiểm số dòng:
  - Job: `UPDATE … SET status = 'EXPIRING' … WHERE status = 'ACTIVE' AND expires_at < now() … FOR UPDATE SKIP LOCKED` (DR-42); người mua: `… WHERE user_id = :uid AND status = 'ACTIVE'` (DR-43); hủy sự kiện: `… WHERE event_id = :e AND status = 'ACTIVE'` (DR-28).
  - Xác nhận: `UPDATE reservation SET status = 'CONFIRMED' WHERE reservation_id = :rid AND status IN ('ACTIVE','EXPIRING')`.
  - Trả vé: `UPDATE reservation SET status = 'EXPIRED'|'CANCELLED' WHERE reservation_id = :rid AND status = 'EXPIRING'`, **đứng đầu** transaction trả vé, rồi mới tới unit và đơn (DOC-24 §4.4). Lock dòng tuần tự hóa hai bên; bên đến sau thấy 0 dòng và dừng.
- Thủ tục trả vé dùng chung cho job, đường nhanh và hủy sự kiện: (1) đặt `EXPIRING`, commit; (2) ngoài transaction, nếu đơn có PaymentIntent thì gọi hủy qua SPI `PaymentIntentCanceller`; (3) hủy được hoặc chưa từng có → transaction trả vé; Stripe báo đã `succeeded` → không trả vé, xác nhận chạy; Stripe lỗi → giữ `EXPIRING`, lease 30 giây lấy lại (DR-42).
- Tạo PaymentIntent khóa reservation `FOR SHARE` khi lưu `payment_intent_id` (DR-47), nên job chỉ đọc `payment_intent_id` sau khi ID đã được lưu: không còn PaymentIntent "mồ côi" mà job không biết để hủy.
- **Thanh toán đến sau khi reservation đã đóng không phát hành vé**, kể cả khi chỗ còn trống. Đơn `EXPIRED`/`CANCELLED` sang `REFUND_PENDING` với `refund_reason = 'LATE_PAYMENT'`, outbox `EMAIL_REFUND_PENDING`, log ERROR; không chạm `inventory_unit`. `NEEDS_REVIEW` của SDD gốc được thay bằng `REFUND_PENDING`/`REFUNDED`; người vận hành hoàn tiền thủ công theo RB-01 (DR-44). Đơn chưa thu tiền khi đóng vì hủy sự kiện sang `CANCELLED`, không phải `REFUND_PENDING` (DR-91).

## Hệ quả

**Tích cực**

- Không có trạng thái nào mà kho vé đã trả trong khi tiền vẫn có thể về cho reservation đó; NEVER OVERSELL giữ được dưới mọi thứ tự của webhook và job.
- Không transaction nào giữ lock hay connection trong lúc gọi Stripe.
- Một thủ tục trả vé cho ba nguồn kích hoạt; một máy trạng thái reservation không có chuyển ngược.
- Luồng tiền về trễ ngắn, không tranh chấp kho vé, dễ kiểm tra: số đơn `REFUND_PENDING` lý do `LATE_PAYMENT` khác 0 là tín hiệu cần điều tra (EXP-06 kỳ vọng 0).

**Tiêu cực**

- Vé hết hạn về kho chậm hơn: trễ một nhịp job (5 giây) cộng thời gian gọi Stripe; khi Stripe lỗi, vé bị giữ thêm ít nhất một lease (30 giây). Với nhiều reservation hết hạn cùng lúc, NFR-03 cần được kiểm bằng EXP-04 (DOC-24 câu hỏi Q1).
- Người mua trả tiền muộn luôn phải chờ hoàn tiền thủ công, kể cả khi chỗ còn trống; khối việc hoàn tiền thủ công tăng khi hủy sự kiện đã bán nhiều.
- Thủ tục trả vé gồm hai transaction và một lời gọi ngoài; tiến trình chết giữa chừng để reservation ở `EXPIRING` tới khi lease hết.

**Việc phát sinh**

- P2-09: job `ReleaseExpiredHoldsJob` và đường nhanh `DELETE /reservations/{id}` (chưa có PaymentIntent); test INVT-12…15, INVT-18, INVT-25.
- P3: cài đặt `PaymentIntentCanceller`, transaction xác nhận và luồng tiền về trễ (DOC-26, FL-21…23); EXP-06 bơm webhook trong ±2 giây quanh `expires_at`.
- DOC-30: kiểm tra `ACTIVE` quá hạn > 60 giây, `EXPIRING` > 2 phút, số đơn `REFUND_PENDING` theo lý do.
- Hoàn tiền tự động (ngoài phạm vi) cần ADR riêng.
