# Vòng đời dữ liệu

> Trạng thái: **Review** · Cập nhật: 2026-10-14 · DOC-18
> Phụ thuộc: [DOC-07](../03-architecture/system-context-and-containers.md) §4, [DOC-12](../03-architecture/code-architecture.md) §2, [DOC-14](domain-model.md) §4.2, [DOC-15](ops-model.md) §3–§7, §10, [DOC-20](../06-design/events-and-ticket-types.md) §6.8, [DOC-33](../06-design/observability.md) §2.5, [DOC-34](../06-design/configuration-reference.md) §3.8, [Sổ quyết định](../00-decision-register.md) (DR-21, 22, 31, 38, 45, 53, 74, 79, 95, 153), [ADR-0015](../04-adr/0015-media-in-s3-compatible-object-storage.md)
> Người dùng chính: P2-12 (`RetentionJob`), module `auth`, `reservation`, `payment`, `notification`, `media`, `studio`; DOC-32 (dữ liệu cá nhân)

Tài liệu nói dữ liệu nào được giữ bao lâu, ai xóa, bằng câu SQL nào, và dữ liệu cá nhân nằm ở đâu. DDL và index nằm ở DOC-14, DOC-15 (tài liệu này chỉ dẫn tên); khóa cấu hình chuẩn ở DOC-34 §3.8; quy tắc log ở DOC-33 §2.5. Xóa dữ liệu demo bằng `make reset` thuộc DOC-61.

Mọi quyết định mới phát sinh khi viết tài liệu này được ghi `DR-NEW-DOC18-n` và tóm tắt ở §9.

## 1. Bảng lưu giữ

Nguồn: DR-74. Cột "Xóa bởi" là bean `RetentionContributor` (§2) của module sở hữu dữ liệu (DOC-07 §4.1).

| Dữ liệu | Giữ | Điều kiện xóa | Khóa cấu hình | Index | Xóa bởi |
| --- | --- | --- | --- | --- | --- |
| `login_token` | 24 giờ sau `expires_at` | `expires_at < now() - retention.login-token` | `retention.login-token` (`PT24H`) | `login_token_expires_idx` | `auth.service.LoginTokenRetention` |
| `session` | 7 ngày sau khi hết hạn (không hoạt động `auth.session.idle-timeout` = 30 ngày) hoặc bị thu hồi | `last_seen_at < now() - (idle-timeout + retention.session)`; `revoked_at < now() - retention.session` | `retention.session` (`P7D`) | `session_last_seen_idx`, `session_revoked_idx` | `auth.service.SessionRetention` |
| `idempotency_key` | 24 giờ | `created_at < now() - retention.idempotency-key` | `retention.idempotency-key` (`PT24H`) | `idempotency_key_created_idx` | `reservation.service.IdempotencyKeyRetention` |
| `stripe_event` | 30 ngày | `received_at < now() - retention.stripe-event` | `retention.stripe-event` (`P30D`) | `stripe_event_received_idx` | `payment.service.StripeEventRetention` |
| `outbox` `SENT` | 7 ngày sau `sent_at` | `status = 'SENT' AND sent_at < now() - retention.outbox-sent` | `retention.outbox-sent` (`P7D`) | `outbox_done_idx` | `notification.service.OutboxRetention` |
| `outbox` `FAILED` | 30 ngày sau `created_at` | `status = 'FAILED' AND created_at < now() - retention.outbox-failed` | `retention.outbox-failed` (`P30D`) | `outbox_done_idx` | `notification.service.OutboxRetention` |
| `media` không được tham chiếu | 24 giờ sau khi tạo | §4 | `retention.unreferenced-media` (`PT24H`) | `media_organizer_idx` (không dùng); quét tuần tự bảng nhỏ | `studio.service.MediaRetention` (DR-159) |
| Object S3 không có dòng `media` | Không xóa tự động | — | — | — | Không ai (DR-38, DR-74); `make reset` xóa sạch volume |
| `event`, `ticket_type`, `seat_map`, `seat_map_version` | Giữ nguyên | — | — | — | — |
| `inventory_pool`, `inventory_unit`, `reservation`, `reservation_item`, `orders`, `ticket` | Giữ nguyên (đối soát, EXP, hoàn tiền về sau) | — | — | — | — |
| `app_user`, `organizer` | Giữ nguyên; chưa có xóa tài khoản ở giai đoạn này | — | — | — | — |
| `fake_payment_intent`, `inventory_pool_counter` (profile) | Giữ tới `make reset` | — | — | — | — |
| Redis (`rl:*`, `queue:*`, `admitted:*`, `soldout:*`, `sales:*`) | TTL của từng key (DOC-17) | — | — | — | Redis |
| Cache Caffeine (session, tình trạng chỗ) | TTL trong tiến trình | — | — | — | Caffeine |

`retention.batch-size` = 5.000 dòng mỗi lô cho mọi bảng; ảnh dùng cùng kích thước lô.

## 2. `RetentionJob`

### 2.1 Interface

```java
package io.ticket.common.retention;

/** SPI: mỗi module cài đặt cho bảng của mình (DR-79). */
public interface RetentionContributor {

  /** Tên ổn định, dùng làm label metric và trường log: "login_token", "session", "media"… */
  String name();

  /** Gọi một lần đầu mỗi lượt chạy; contributor có con trỏ (ảnh, §4) đặt lại ở đây. */
  default void beginRun() {}

  /** Xử lý tối đa batchSize dòng quá hạn trong một transaction ngắn. */
  BatchResult purgeBatch(int batchSize);

  /** deleted: số dòng đã xóa; more: còn dòng cần xét trong lượt này. */
  record BatchResult(int deleted, boolean more) {}
}
```

```java
package io.ticket.common.job;

@Component
class RetentionJob {
  @Scheduled(cron = "${retention.cron}", zone = "${platform.timezone}")
  void run() { retentionRunner.runAll(); }
}
```

`RetentionRunner` (`common.retention`, không `@Transactional`, DR-153) gọi lần lượt từng contributor theo thứ tự tên; mỗi lần gọi `purgeBatch` là một transaction riêng mở ở service của module sở hữu (`@Transactional` trên method `public`). Contributor là bean của module sở hữu nên SQL chỉ nhắc bảng của module đó (DOC-12 §2.2).

### 2.2 Thuật toán

1. 03:00 theo `platform.timezone` (`retention.cron` = `0 0 3 * * *`).
2. Với mỗi contributor: gọi `beginRun()`, rồi
   1. `r = contributor.purgeBatch(retention.batch-size)`.
   2. Cộng `r.deleted` vào `ticket_retention_deleted_total{table}`.
   3. `r.more` thì lặp lại bước 2.1; ngược lại sang contributor kế. Contributor của bảng vận hành đặt `more = (deleted == batchSize)`.
   4. Lỗi một lô: log ERROR `retention.batch_failed` kèm `table`, dừng contributor đó, chuyển contributor kế (dòng còn lại xóa ở đêm sau).
3. Log INFO `retention.finished` kèm số dòng mỗi bảng và `duration_ms`; `ticket_job_runs_total{job="retention"}`.

Không giới hạn số lô mỗi đêm: dữ liệu quá hạn tối đa là lượng sinh ra trong một ngày, cộng phần tồn của đêm lỗi trước.

### 2.3 Câu SQL mỗi lô

Mọi câu dùng cùng mẫu: chọn khóa chính trong subquery có `LIMIT` và `FOR UPDATE SKIP LOCKED` (hai replica cùng chạy không chờ nhau, không xóa trùng), rồi `DELETE` theo khóa. Tham số khoảng thời gian truyền dạng `interval` từ `Duration` của cấu hình.

```sql
-- login_token (auth)
DELETE FROM login_token
 WHERE token_hash IN (
   SELECT token_hash FROM login_token
    WHERE expires_at < now() - :keep::interval            -- PT24H
    ORDER BY expires_at LIMIT :batch FOR UPDATE SKIP LOCKED);

-- session hết hạn vì không hoạt động (auth); :keep = idle-timeout + retention.session = 37 ngày
DELETE FROM session
 WHERE session_hash IN (
   SELECT session_hash FROM session
    WHERE last_seen_at < now() - :keep::interval
    ORDER BY last_seen_at LIMIT :batch FOR UPDATE SKIP LOCKED);

-- session đã thu hồi (auth); :keep = P7D. Câu riêng để dùng session_revoked_idx
DELETE FROM session
 WHERE session_hash IN (
   SELECT session_hash FROM session
    WHERE revoked_at < now() - :keep::interval
    ORDER BY revoked_at LIMIT :batch FOR UPDATE SKIP LOCKED);

-- idempotency_key (reservation)
DELETE FROM idempotency_key
 WHERE (user_id, idem_key) IN (
   SELECT user_id, idem_key FROM idempotency_key
    WHERE created_at < now() - :keep::interval            -- PT24H
    ORDER BY created_at LIMIT :batch FOR UPDATE SKIP LOCKED);

-- stripe_event (payment)
DELETE FROM stripe_event
 WHERE stripe_event_id IN (
   SELECT stripe_event_id FROM stripe_event
    WHERE received_at < now() - :keep::interval           -- P30D
    ORDER BY received_at LIMIT :batch FOR UPDATE SKIP LOCKED);

-- outbox (notification): SENT 7 ngày từ sent_at, FAILED 30 ngày từ created_at;
-- điều kiện viết trên COALESCE(sent_at, created_at) để khớp biểu thức của outbox_done_idx
DELETE FROM outbox
 WHERE outbox_id IN (
   SELECT outbox_id FROM outbox
    WHERE status <> 'PENDING'
      AND COALESCE(sent_at, created_at) < now() - :keep_sent::interval     -- P7D, ngưỡng nhỏ hơn: lọc bằng index
      AND ((status = 'SENT'   AND sent_at    < now() - :keep_sent::interval)
        OR (status = 'FAILED' AND created_at < now() - :keep_failed::interval))   -- P30D
    ORDER BY COALESCE(sent_at, created_at) LIMIT :batch FOR UPDATE SKIP LOCKED);
```

Session có hai câu, nên `SessionRetention.purgeBatch` chạy câu thứ nhất, còn chỗ trong lô thì chạy câu thứ hai với phần còn lại của `batchSize`, trả tổng và `more = (tổng == batchSize)`. `idempotency_key` chỉ cần sống tới khi client hết thử lại (vài phút); 24 giờ là biên rộng (DR-45). Xóa dòng `outbox` `SENT` không ảnh hưởng gửi trùng: `Message-ID` cố định nằm ở hộp thư người nhận (DR-53).

## 3. Thứ tự và đồng thời

- Mỗi lô là một transaction ngắn (≤ 5.000 dòng theo index), không gọi ra ngoài, trừ ảnh (§4 bước 5, sau commit).
- Không bảng nào trong §1 là nguồn của bất biến (DOC-30): xóa không làm sai kiểm tra bất biến.
- Đồng thời với nghiệp vụ: `login_token` đang được verify đã quá hạn từ 24 giờ nên không bao giờ hợp lệ; `idempotency_key` 24 giờ tuổi không còn request nào gửi lại; dòng `outbox` `PENDING` không bao giờ bị chọn; `stripe_event` 30 ngày tuổi nếu Stripe gửi lại sẽ được xử lý như event mới nhưng mọi chuyển trạng thái đơn là câu có điều kiện nên không có tác dụng phụ (DR-48).
- Hai replica cùng chạy lúc 03:00: `SKIP LOCKED` chia dòng giữa hai bên; tổng số dòng xóa đúng bằng một replica.

## 4. Ảnh không còn tham chiếu

**Định nghĩa.** Một dòng `media` là **không còn tham chiếu** khi đủ cả ba:

1. `created_at < now() - retention.unreferenced-media` (24 giờ);
2. không sự kiện nào có `event.image_media_id = media_id`;
3. không tài liệu sơ đồ nào trỏ tới nó ở `canvas.background.mediaId`: cả bản nháp `seat_map.draft` lẫn mọi `seat_map_version.document` (sơ đồ nhân bản dùng chung `media_id`, DR-31). Điều kiện 3 có từ P4; ở P2 chưa có sơ đồ nên luôn đúng.

Bảng `media` thuộc `media`, còn `event` thuộc `event` và `seat_map*` thuộc `map`; module `media` không phụ thuộc module nào (DOC-12 §2.2) nên không tự biết ảnh nào đang dùng. Contributor ảnh đặt ở `studio`, module đã được phụ thuộc cả ba (DR-159).

**`studio.service.MediaRetention.purgeBatch(batch)`** (không `@Transactional` ở mức method điều phối; các bước 1, 4 là method `@Transactional` của module sở hữu):

1. `MediaApi.candidatesOlderThan(keep, after, batch)`:

   ```sql
   SELECT media_id FROM media
    WHERE created_at < now() - :keep::interval
      AND media_id > :after                       -- con trỏ trong lượt chạy; beginRun() đặt lại về 00000000-…
    ORDER BY media_id LIMIT :batch;               -- khóa chính; UUIDv7 tăng theo thời gian
   ```

   Ảnh đang được dùng không bị xóa nên lượt sau vẫn gặp lại chúng; con trỏ `:after` (bằng `media_id` cuối của lô trước, giữ trong bean, job chạy một luồng) bảo đảm mỗi ảnh chỉ được xét một lần mỗi lượt.
2. `EventApi.referencedMediaIds(ids)`:

   ```sql
   SELECT image_media_id FROM event WHERE image_media_id = ANY(:ids);   -- event_media_idx
   ```

3. Từ P4, `MapApi.referencedMediaIds(ids)`:

   ```sql
   SELECT (draft #>> '{canvas,background,mediaId}')::uuid FROM seat_map
    WHERE draft #>> '{canvas,background,mediaId}' = ANY(:ids_text)
   UNION
   SELECT (document #>> '{canvas,background,mediaId}')::uuid FROM seat_map_version
    WHERE document #>> '{canvas,background,mediaId}' = ANY(:ids_text);
   ```

   Quét tuần tự hai bảng (vài nghìn dòng ở quy mô dự án), một lần mỗi lô, ban đêm; không thêm index. Đường dẫn `canvas.background.mediaId` do DOC-16 chốt ở P4.
4. `MediaApi.deleteRows(unreferenced)` (`@Transactional`):

   ```sql
   DELETE FROM media
    WHERE media_id = ANY(:ids) AND created_at < now() - :keep::interval
   RETURNING media_id, object_key;
   ```

   Khóa ngoại `event.image_media_id → media` là lưới an toàn cho tranh chấp: nếu người tổ chức gắn ảnh vào sự kiện giữa bước 2 và bước 4, câu `DELETE` lỗi `23503` và cả lô rollback; `MediaRetention` thử lại lô đó từng dòng một, bỏ qua dòng lỗi. Tham chiếu trong JSON sơ đồ không có khóa ngoại; cửa sổ đua (ảnh nền được chọn đúng trong vài mili giây giữa bước 3 và 4 của một ảnh đã 24 giờ không ai dùng) được chấp nhận, ảnh nền khi đó hiện ô trống và người tổ chức tải lại.
5. Sau commit, với mỗi `object_key` trả về: `DeleteObject` (ngoài transaction). Lỗi → log WARN `retention.object_delete_failed` kèm `object_key`; object sót trong bucket, chấp nhận (DR-38).
6. Trả `BatchResult(deleted = số dòng đã xóa, more = bước 1 trả đủ batch dòng)`.

**Thứ tự xóa dòng trước, object sau** (DR-159) khác chữ của DR-74 ("`DeleteObject` rồi xóa dòng"): xóa object trước mà câu `DELETE` sau đó rollback (ảnh vừa được gắn) thì sự kiện trỏ tới ảnh mất byte; xóa dòng trước thì lỗi xấu nhất chỉ là object sót, đúng loại lỗi DR-38 đã chấp nhận.

**Object sót.** Có ba nguồn: chèn `media` lỗi và xóa object cũng lỗi (DOC-20 §6.8), tiến trình chết giữa `PutObject` và chèn dòng, `DeleteObject` lỗi ở bước 5. Không có job quét bucket (DR-74): object sót chỉ tốn dung lượng, không ai đọc được vì `GET /media/{id}` đi qua dòng `media`. `make reset` xóa volume `storage-data`. nginx có thể còn phục vụ ảnh đã xóa từ `proxy_cache` tối đa 1 ngày; chấp nhận vì ảnh đó không được trang nào dẫn tới.

## 5. Dữ liệu cá nhân

Dữ liệu cá nhân của hệ thống: email, IP, và liên kết giữa người dùng với đơn hàng.

| Nơi | Dữ liệu | Giữ | Ghi chú |
| --- | --- | --- | --- |
| `app_user.email` | Email đăng nhập đã chuẩn hóa | Giữ nguyên | Nguồn duy nhất lâu dài của email người mua |
| `organizer.contact_email` | Email liên hệ của tổ chức | Giữ nguyên | Gửi cho người mua trong email đổi lịch (DR-29) |
| `login_token.email`, `login_token.requested_ip` | Email, IP người xin link | 24 giờ sau hết hạn | §1 |
| `outbox.payload.to` | Email người nhận | 7 ngày (`SENT`), 30 ngày (`FAILED`) | DOC-15 §7.2 |
| `session.user_id` | Liên kết phiên ↔ người dùng | ≤ 37 ngày | Không có IP hay user agent |
| `orders.user_id`, `reservation.user_id` | Liên kết đơn ↔ người dùng | Giữ nguyên | Không chứa email |
| Mailpit (dev) | Bản sao email đã gửi | Tới `make reset` | Chỉ môi trường dev và CI |
| Stripe | Dữ liệu thẻ | Ở Stripe | Server không nhận dữ liệu thẻ (DOC-32); PaymentIntent không có `receipt_email`, `metadata` chỉ có `order_id`, `reservation_id`, `event_id` |

**Không bao giờ có dữ liệu cá nhân ở:**

- Log ứng dụng và log nginx: không email, không token, không cookie, không query string của `/auth/callback` (DOC-33 §2.5, DR-99).
- Label metric: không `user_id`, email, IP (DR-121).
- Key và giá trị Redis: chỉ `userId` dạng UUID trong `rl:*`, `queue:*`, `admitted:*` (DOC-17), không email.
- `idempotency_key.response_body`: response giữ vé và hủy giữ không chứa email.
- `ticket`, `stripe_event`, `media`, `inventory_*`: không cột nào chứa email hay IP.
- Object S3: chỉ ảnh do người tổ chức tải lên.

Xóa tài khoản theo yêu cầu người dùng (quên tôi) chưa có ở giai đoạn này; khi thêm thì cần DR mới vì `orders.user_id` và `app_user` là khóa ngoại của dữ liệu đối soát.

## 6. Cấu hình

Mọi khóa đã có ở DOC-34 §3.8; bảng dưới chỉ để đọc cùng thuật toán.

| Khóa | Kiểu | Mặc định | Mô tả |
| --- | --- | --- | --- |
| `retention.cron` | cron | `0 0 3 * * *` | Theo `platform.timezone` |
| `retention.batch-size` | int | `5000` | Dòng mỗi lô |
| `retention.login-token`, `retention.idempotency-key`, `retention.unreferenced-media` | duration | `PT24H` | §1 |
| `retention.session`, `retention.outbox-sent` | duration | `P7D` | §1 |
| `retention.stripe-event`, `retention.outbox-failed` | duration | `P30D` | §1 |
| `auth.session.idle-timeout` | duration | `P30D` | Cộng `retention.session` cho ngưỡng xóa session không hoạt động |

## 7. Metrics và log

| Metric | Kiểu | Label | Nơi phát | Ý nghĩa |
| --- | --- | --- | --- | --- |
| `ticket_job_runs_total{job="retention"}`, `ticket_job_duration_seconds{job="retention"}` | C, H | `job`, `result` | `common.job.JobMetrics` | Đã có (DOC-33 §4.2) |
| `ticket_retention_deleted_total` | C | `table` ∈ {`login_token`, `session`, `idempotency_key`, `stripe_event`, `outbox`, `media`} | `RetentionRunner` | Số dòng đã xóa. **Mới** (DR-160) |

Log: INFO `retention.finished` (`deleted` theo bảng, `duration_ms`); ERROR `retention.batch_failed` (`table`, lỗi); WARN `retention.object_delete_failed` (`object_key`). Không ghi giá trị dòng bị xóa.

## 8. Test bắt buộc

Tiền tố `RET-` (mới, DR-160; chưa dùng ở `docs/` và master plan §0.4). Mức I (Testcontainers PostgreSQL và SeaweedFS). Test của từng câu trên bảng vận hành đã có ở DOC-15 (`OM-14`, `IDEM-06`); bảng dưới kiểm phần còn lại.

| ID | Kịch bản | Kỳ vọng |
| --- | --- | --- |
| RET-01 | Mỗi bảng §1 có một dòng quá hạn 1 phút và một dòng còn hạn 1 phút; chạy `RetentionJob` | Mọi dòng quá hạn bị xóa; mọi dòng còn hạn còn |
| RET-02 | 12.000 dòng `outbox` `SENT` cũ 8 ngày, 300 dòng `FAILED` cũ 29 ngày, 50 dòng `PENDING` cũ 60 ngày | Xóa 12.000 dòng trong 3 lô; 300 `FAILED` và 50 `PENDING` còn |
| RET-03 | Session `last_seen_at` cũ 38 ngày; 36 ngày; `revoked_at` cũ 8 ngày; 6 ngày | Xóa dòng 38 ngày và dòng thu hồi 8 ngày; hai dòng kia còn |
| RET-04 | Hai `RetentionRunner` chạy song song trên 20.000 dòng `login_token` quá hạn | Tổng `ticket_retention_deleted_total{table="login_token"}` = 20.000; không lỗi, không chờ khóa quá 1 giây |
| RET-05 | Ảnh A tạo 25 giờ trước không gắn; ảnh B 25 giờ trước là ảnh của một sự kiện; ảnh C tạo 23 giờ trước | Chỉ A bị xóa dòng và `DeleteObject`; B, C còn cả dòng và object |
| RET-06 | Ảnh D 25 giờ trước được gắn vào sự kiện giữa bước 2 và bước 4 (chèn hook trong test) | Câu `DELETE` lỗi `23503`, lô thử lại từng dòng; D còn; các ảnh khác trong lô vẫn bị xóa |
| RET-07 | `DeleteObject` lỗi (storage dừng sau khi xóa dòng) | Dòng `media` đã xóa; log WARN `retention.object_delete_failed`; job kết thúc `result="ok"` |
| RET-08 | Từ P4: ảnh E là `canvas.background.mediaId` của một `seat_map_version` và không ở bản nháp nào | E còn |
| RET-09 | Contributor `stripe_event` ném lỗi ở lô đầu | Log ERROR `retention.batch_failed`; các contributor sau vẫn chạy; `ticket_job_runs_total{job="retention",result="ok"}` |
| RET-10 | Chạy luồng mua vé đầy đủ rồi quét log ứng dụng, label metric và mọi key Redis | Không chuỗi nào khớp regex email; không IP trong label (§5) |

## 9. Quyết định phát sinh khi viết tài liệu này

Claude chốt theo ủy quyền của Owner; số DR thật do sổ quyết định gán.

- **DR-159 · Contributor ảnh đặt ở `studio`, xóa dòng trước object.** *Vấn đề:* DR-74 giao việc dọn ảnh cho module `media` (DOC-07 §4.3), nhưng tham chiếu ảnh nằm ở bảng của `event` và trong JSON sơ đồ của `map`, còn `media` không được phụ thuộc module nào; DR-74 ghi thứ tự "`DeleteObject` rồi xóa dòng", có thể để sự kiện trỏ tới ảnh mất byte khi người tổ chức gắn ảnh đúng lúc dọn. *Quyết định:* `studio.service.MediaRetention` cài `RetentionContributor`: lấy ứng viên qua `MediaApi.candidatesOlderThan`, lọc bằng `EventApi.referencedMediaIds` và (từ P4) `MapApi.referencedMediaIds`, xóa dòng bằng `MediaApi.deleteRows` (khóa ngoại `event.image_media_id` là lưới an toàn), rồi `DeleteObject` sau commit; object xóa lỗi thì sót lại. *Hệ quả:* không đổi đồ thị phụ thuộc; thêm `EventApi.referencedMediaIds`, `MapApi.referencedMediaIds`, `MediaApi.candidatesOlderThan`, `MediaApi.deleteRows`; SPI `RetentionContributor` trả `BatchResult(deleted, more)` và có `beginRun()` để contributor có con trỏ. *Ghi vào:* DOC-18, DOC-07 §4.3, DOC-20, DOC-23.
- **DR-160 · Metric và tiền tố test của vòng đời dữ liệu.** *Vấn đề:* `ticket_job_runs_total` chỉ cho biết job chạy, không cho biết xóa bao nhiêu; DOC-18 chưa có tiền tố test ở master plan §0.4. *Quyết định:* metric `ticket_retention_deleted_total{table}` (tập `table` đóng 6 giá trị, DR-121); tiền tố test `RET-`. *Hệ quả:* DOC-33 §4.2 thêm một dòng; DOC-69 §12.1 thêm `RET-` (01…10). *Ghi vào:* DOC-18, DOC-33, DOC-69.

## Câu hỏi còn mở

Không còn.
