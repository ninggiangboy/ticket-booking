# ADR-0015: Ảnh lưu ở object storage tương thích S3, API phục vụ qua cache nginx

- Status: Proposed
- Date: 2026-10-14 · Related: DR-38, DR-74, DR-31, DR-04, DR-72, FR-18, DOC-07, DOC-14, DOC-18, DOC-20, DOC-37 (E-13, E-39), DOC-62

## Bối cảnh

Người tổ chức tải ảnh sự kiện (Studio 02, "1920 × 1080 · 420 KB", tỷ lệ 16:9) và ảnh nền mặt bằng cho editor (SDD gốc §6.3, §7.3, §14.4). Kiến trúc của SDD gốc chỉ có PostgreSQL và Redis, không có kho file. Ảnh được đọc nhiều hơn ghi rất nhiều: mỗi lượt xem danh sách và trang sự kiện tải ảnh, kể cả lúc mở bán đông, trong khi PostgreSQL phải dành connection và bộ đệm cho đường giữ vé (NFR-02). Dự án chạy bằng `docker compose` trên một máy (NFR-08) nhưng không nên gắn chặt vào một máy khi chuyển lên hạ tầng thật. (Nguồn: DR-38.)

## Các phương án

1. **Cột `bytea` trong bảng `media` của PostgreSQL.** Một nơi lưu, backup chung, không thêm container. Ngược lại: ảnh 2–5 MB chiếm bộ đệm và WAL của database, mỗi lần cache nginx trượt là một connection đọc vài MB.
2. **Volume Docker do nginx phục vụ trực tiếp.** Nhanh, không thêm SDK. Ngược lại: gắn vào hệ tệp của một máy, API và nginx phải chia sẻ volume, chuyển lên nhiều máy phải làm lại.
3. **Object storage tương thích S3, API phục vụ ảnh, nginx cache.** Byte ảnh tách khỏi database; đổi sang AWS S3, Cloudflare R2 hay Garage chỉ bằng cấu hình. Ngược lại: thêm container, SDK và một loại lỗi hạ tầng mới.
4. **Như 3 nhưng trình duyệt tải trực tiếp bằng presigned URL.** Bớt tải cho API. Ngược lại: URL ảnh đổi theo thời hạn chữ ký nên không cache dài hạn được; muốn URL ổn định thì phải mở bucket công khai, mà chính sách bucket công khai mỗi kho S3 cài một khác.

## Quyết định

Chọn phương án **3**.

- Code chỉ phụ thuộc API S3: AWS SDK for Java v2 `S3Client` với `forcePathStyle(true)` và `requestChecksumCalculation(WHEN_REQUIRED)` (kho tương thích S3 thường không nhận checksum CRC mặc định của SDK mới). Cấu hình `STORAGE_S3_ENDPOINT`, `STORAGE_S3_REGION`, `STORAGE_S3_BUCKET` (`ticket-media`), `STORAGE_S3_ACCESS_KEY`, `STORAGE_S3_SECRET_KEY` (DOC-34 §3.9).
- Compose chạy service `storage` bằng SeaweedFS (`weed server -s3`, volume `storage-data`, cổng 8333 chỉ trong mạng compose). Không dùng MinIO vì bản cộng đồng đã ngừng phát hành image và repo bị lưu trữ năm 2026 (DR-38, DR-04). Bucket private; API tạo bucket lúc khởi động nếu chưa có.
- Bảng `media` (module `media`) chỉ giữ metadata: `purpose`, `content_type`, `object_key = media/<media_id>` (bất biến), kích thước, `sha256` (DOC-14 §4.2).
- Tải lên `POST /organizer/media` (E-39): kiểm magic bytes và kích thước ảnh, `PutObject` **trước**, rồi chèn dòng `media` trong transaction ngắn. Chèn lỗi → xóa object một lần; xóa cũng lỗi thì object sót lại, chấp nhận. Không transaction nào giữ connection trong lúc gọi storage (DOC-20 §6.8, DR-153).
- Phục vụ `GET /media/{id}` (E-13, ngoài tiền tố `/api/v1`): API đọc dòng `media`, stream object, `Cache-Control: public, max-age=31536000, immutable`, `ETag` = sha256. nginx `proxy_cache` 1 ngày (DOC-62) nên storage chỉ bị gọi khi cache trượt. Không presigned URL.
- Không resize ở server; client hiển thị `object-fit: cover`. Nhân bản sơ đồ dùng chung `media_id` (DR-31).
- Ảnh tải lên mà không được tham chiếu sau 24 giờ bị `RetentionJob` xóa (dòng trước, object sau); object sót trong bucket không có job quét (DR-74, DOC-18 §4).

## Hệ quả

**Tích cực**

- Database không chứa byte ảnh: backup nhỏ, bộ đệm dành cho kho vé.
- URL ảnh ổn định và bất biến, cache được một năm ở trình duyệt và một ngày ở nginx; lúc mở bán đông ảnh gần như không chạm `api`.
- Chuyển sang S3/R2 thật không sửa code, chỉ đổi biến môi trường.

**Tiêu cực**

- Thêm một container, một volume và một phụ thuộc (AWS SDK v2) cần theo dõi phiên bản.
- Hai kho dữ liệu không chung transaction: object có thể sót lại không có dòng `media` (chèn lỗi, tiến trình chết giữa hai bước, xóa object lỗi); chỉ dọn bằng `make reset`.
- Backup database không chứa ảnh; khôi phục database mà thiếu volume `storage-data` thì ảnh hiện lỗi 404.
- Khi cache nginx trượt, API phải stream ảnh qua chính nó (không như presigned URL): tốn một luồng xử lý trong vài chục mili giây mỗi ảnh.
- Test tích hợp module `media` cần Testcontainers với image SeaweedFS (thêm thời gian `make it`).

**Việc phát sinh**

- P1-02: service `storage` trong compose, healthcheck, biến `STORAGE_S3_*` (đã xong).
- P2-03.1: `MediaService` tải lên và phục vụ; test EV-21…24 (DOC-20).
- P2-12: dọn ảnh không còn tham chiếu (DOC-18 §4, test RET-05…08).
- P4: ảnh nền mặt bằng (`FLOOR_PLAN`) trong editor, tham chiếu ở `canvas.background.mediaId` (DOC-16).
