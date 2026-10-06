# Danh mục màn hình

> Trạng thái: **Approved** · Cập nhật: 2026-10-06 · DOC-41
> Phụ thuộc: SDD gốc §13, [Sổ quyết định](../../00-decision-register.md) (DR-10, 24, 37, 41, 67, 68, 69, 70), [DOC-06](../../02-glossary.md), [DOC-38](../ux-principles-and-ia.md) §3 (bản đồ URL), [DOC-39](../design-system.md), [DOC-40](../ui-states-and-copy.md), [DOC-82](../../06-design/flows/README.md) §3–§4, [DOC-44](login.md), [DOC-51](emails.md), [DOC-52](error-pages.md)
> Người dùng chính: tác giả DOC-42…60 (mẫu A.5 và tên component), mọi task `Pn-xx.2` (frontend) khi tìm màn hình cần dựng, người viết sơ đồ luồng DOC-83…90

Tài liệu này là mục lục của 19 route (bản đồ URL của DOC-38) và của email trong giao diện, và là **nguồn cuối cùng của tên component màn hình** (DOC-82 §3 lấy tên từ đây). Nó không mô tả nội dung từng màn (nằm ở file spec), không lặp bản đồ URL và chunk tải lười (DOC-38 §3), token và component dùng chung (DOC-39) hay chuỗi giao diện (DOC-40).

## 1. Danh mục

Cột "Artboard" là tên artboard trong canvas "Ticket — Design system & luồng mua vé"; tiền tố `M` là bản điện thoại 390 px. Cột "Luồng" là các `FL-xx` mà màn tham gia (định nghĩa ở DOC-82 §2). Cột "Phase" là phase cần spec `Approved` (gate ở master plan §3.2); màn dựng thật theo task `Pn-xx.2`.

### 1.1 Phía người mua

| Component | Màn | URL | Artboard | Spec | Luồng | Phase |
| --- | --- | --- | --- | --- | --- | --- |
| `EventList` | Danh sách sự kiện | `/` | `00`, `M00` | DOC-42 (`event-list.md`) | FL-12 | P2 |
| `EventDetail` | Sự kiện | `/events/:eventId` | `01`, `M01` | DOC-43 (`event-detail.md`) | FL-12, FL-33 (nút vào phòng chờ), FL-04 | P2 |
| `Login` | Đăng nhập (7 biến thể) | `/login?returnTo=&reason=` | `02`, `02b`, `E2`, `M02` | [DOC-44](login.md) | FL-01, FL-03, FL-04 | P1 |
| `AuthCallback` | Xác minh đường dẫn đăng nhập | `/auth/callback?token=` | `03` (email), biến thể "đang xác minh" của `02` | [DOC-44](login.md) | FL-02 | P1 |
| `WaitingRoom` | Phòng chờ | `/events/:eventId/queue` | `04`, `M04` | DOC-45 (`waiting-room.md`) | FL-33, FL-34, FL-35 | P6 |
| `SeatPicker` | Chọn chỗ | `/events/:eventId/seats` (sự kiện có `SEAT`/`ZONE`) | `05`, `M05` | DOC-46 (`seat-picker.md`) | FL-29, FL-30, FL-34 | P5 |
| `QuantityPicker` | Chọn số lượng | `/events/:eventId/seats` (sự kiện chỉ GA) | `05b`, `M05b` | DOC-47 (`quantity-picker.md`) | FL-13, FL-34 | P2 |
| `Checkout` | Thanh toán | `/checkout/:reservationId` | `06`, `M06` | DOC-48 (`checkout.md`) | FL-14, FL-15, FL-20 | P2 (đơn 0 đồng), P3 (thẻ) |
| `OrderResult` | Kết quả | `/orders/:orderId` | `07`, `M07` | DOC-49 (`order-result.md`) | FL-14, FL-17, FL-20, FL-21, FL-23 | P2 |
| `MyTickets` | Vé của tôi | `/me/tickets` | `08`, `M08` | DOC-50 (`my-tickets.md`) | FL-17 | P2 |
| `NotFoundPage`, `ForbiddenPage`, `UnavailablePage`, `ServerErrorPage` | Trang lỗi E1, E3, E4, E5 | theo ngữ cảnh (E1 là route `*`) | `E1`, `E3`, `E4`, `E5` | [DOC-52](error-pages.md) | FL-03 (tham chiếu khi 401), mọi luồng khi lỗi toàn trang | P1 |

E2 (hết phiên) **không có trang riêng**: là biến thể thứ 7 của `Login` với `reason=session_expired` ([DOC-52](error-pages.md) §1, [DOC-44](login.md) §6.5).

### 1.2 Studio của người tổ chức

| Component | Màn | URL | Artboard | Spec | Luồng | Phase |
| --- | --- | --- | --- | --- | --- | --- |
| `StudioOrganizerProfile` | Lập hồ sơ tổ chức | `/studio/profile` | Studio `00` | DOC-53 (`studio-organizer-profile.md`) | FL-05 | P2 |
| `StudioOverview` | Tổng quan | `/studio` | Studio `01` | DOC-54 (`studio-overview.md`) | FL-32 (danh sách), FL-09 | P2 |
| `StudioEventInfo` | Thông tin sự kiện (tạo và sửa) | `/studio/events/new`, `/studio/events/:eventId/info` | Studio `02` | DOC-55 (`studio-event-info.md`) | FL-06, FL-11 | P2 |
| `StudioTicketTypes` | Loại vé và giá | `/studio/events/:eventId/ticket-types` | Studio `03` | DOC-56 (`studio-ticket-types.md`) | FL-07 | P2 |
| `StudioMapEditor` | Seat map editor | `/studio/events/:eventId/map` | Studio `04`, `04b…04m` | DOC-57 (`studio-map-editor.md`) | FL-26, FL-27, FL-28, FL-31 | P4 |
| `StudioPreview` | Xem trước | `/studio/events/:eventId/preview` | Studio `05` | DOC-58 (`studio-preview.md`) | FL-08 (bước kiểm tra trước khi xuất bản) | P2 |
| `StudioPublish` | Xuất bản và mở bán | `/studio/events/:eventId/publish` | Studio `06` | DOC-59 (`studio-publish.md`) | FL-08, FL-09, FL-10 | P2 |
| `StudioSales` | Theo dõi bán vé | `/studio/events/:eventId/sales` | Studio `07` | DOC-60 (`studio-sales.md`) | FL-32 | P5 |

### 1.3 Tài liệu không phải route

| Tài liệu | Nội dung | Artboard | Luồng | Phase |
| --- | --- | --- | --- | --- |
| [DOC-51](emails.md) | 4 mẫu email `magic-link`, `tickets`, `event-changed`, `refund-pending` | `03`, `07b`, `07c` (và mẫu mới `refund-pending`) | FL-01 (`magic-link`), FL-14 và FL-21 (`tickets`), FL-11 (`event-changed`), FL-10 và FL-23 (`refund-pending`) | P1 |

Đối chiếu: bản đồ URL ở [DOC-38](../ux-principles-and-ia.md) §3 có 19 dòng; mỗi dòng ở đó có đúng một dòng ở §1.1–1.2 (`/events/:eventId/seats` ứng với hai component, chọn theo mô hình sự kiện, §3).

## 2. Quy ước viết spec màn hình (mẫu A.5)

Mỗi file spec dùng đúng mười một mục dưới đây, theo thứ tự, bằng tiếng Việt. Mục không áp dụng ghi "Không áp dụng." kèm lý do một câu; không xóa mục.

| # | Mục | Phải có |
| --- | --- | --- |
| 1 | Persona, use case, quyền | `PS-x`, `UC-xx`, vai trò vào được (đối chiếu DOC-38 §3 cột "Quyền vào") |
| 2 | URL và search params | đường dẫn, tham số, giá trị mặc định, tham số sai định dạng đi đâu (E1) |
| 3 | Wireframe | ASCII, một khối desktop (≥ 1024 px) và một khối 390 px; ghi tên artboard bên trên |
| 4 | Vùng và component | tên component theo DOC-39 §7; component riêng của màn mô tả ở đây |
| 5 | Dữ liệu | endpoint `E-xx` (DOC-37), kênh real-time (mặc định "không có"), nhịp làm mới |
| 6 | Tương tác | bảng thao tác → endpoint `E-xx` → kết quả → lỗi (mã lỗi và hiển thị theo DOC-40 §4); luồng `FL-xx` |
| 7 | Trạng thái | đang tải, rỗng, lỗi, quá tải, không quyền và mọi biến thể của canvas; mỗi trạng thái một dòng với điều kiện vào |
| 8 | Microcopy | chỉ dẫn **key** của DOC-40 (`key · vi · en` nằm ở DOC-40, không lặp); màn có chuỗi riêng thì thêm vào DOC-40 trong cùng thay đổi |
| 9 | Tiêu chí nghiệm thu | Given/When/Then, mỗi tiêu chí một ID `<tiền tố màn>-AC-nn` |
| 10 | Ca kiểm thử E2E | bảng ca Playwright, chạy cả `vi` và `en`, mock và thật |
| 11 | Câu hỏi còn mở | rỗng ("Không có.") hoặc mỗi câu có DR đã chốt |

Header: dòng `Phụ thuộc` thêm `canvas: artboard "<tên>"` (ví dụ DOC-44). Quy tắc chung:

- **Một nguồn cho mỗi thứ** ([conventions](../../00-master-plan.md) §7 của plan): token và component ở DOC-39; chuỗi ở DOC-40; endpoint ở DOC-37; luồng ở DOC-83…90. Spec chỉ dẫn tới chúng.
- **Khác canvas thì ghi rõ.** Chỗ spec cố ý khác canvas (ví dụ bỏ ô "Số vé tối đa mỗi đơn", DR-41) ghi vào mục 4 hoặc 7 và vào DOC-38 §9.
- **Mọi biến thể của canvas đều có dòng** ở mục 7, kể cả biến thể mà server quyết định (ví dụ 6 biến thể bán của `EventDetail` suy từ `displayStatus`, DR-24).
- **Màn có trạng thái chờ dữ liệu chính** dùng `Skeleton` (DOC-39), không spinner toàn trang.
- **Điện thoại trước** cho màn người mua (390 px); Studio và editor thiết kế cho ≥ 1024 px, màn hình hẹp chỉ hiện thông báo (DOC-38 §6).

## 3. Tên component: khác với DOC-82 §3

DOC-82 §3 là bản tạm (DR-142). DOC-41 chốt như sau; DOC-82 cần sửa các điểm 1–3 trong cùng thay đổi khi hợp nhất.

| # | DOC-82 §3 ghi | DOC-41 chốt | Lý do |
| --- | --- | --- | --- |
| 1 | `SessionExpired` là tên component (cột trang lỗi, và cột Màn hình của FL-03) | Bỏ. Hết phiên là biến thể `Login` với `reason=session_expired` | E2 không có trang ([DOC-52](error-pages.md) §1) |
| 2 | Hàng `Login`, `AuthCallback` gộp một dòng URL | Tách hai dòng; spec chung DOC-44 | Hai route, hai component, hai `FL` (FL-01/03/04 và FL-02) |
| 3 | `SeatPicker`/`QuantityPicker` cùng URL, không nói ai chọn | Thêm `SeatsRoute` (phần tử của router, không phải màn) chọn component theo `event.hasSeatMap`; vào bằng URL mà sự kiện không có `SEAT`/`ZONE` thì `QuantityPicker`; vào `…/preview` thì xem DOC-38 §3 | Một URL, hai màn; chọn ở một chỗ để tránh hai nơi tự quyết |
| 4 | `StudioEventInfo` | Giữ; ghi rõ phục vụ cả `/studio/events/new` và `…/:eventId/info` | Cùng biểu mẫu, hai chế độ (tạo, sửa) |

Các tên còn lại (`EventList`, `EventDetail`, `WaitingRoom`, `Checkout`, `OrderResult`, `MyTickets`, `NotFoundPage`, `ForbiddenPage`, `UnavailablePage`, `ServerErrorPage`, `Studio*`) giữ nguyên như DOC-82 §3.

Quy tắc đặt tên: PascalCase tiếng Anh, danh từ theo nghĩa của màn, không hậu tố `Page` ngoại trừ trang lỗi (để phân biệt với màn nghiệp vụ); file ở `frontend/src/features/<nhóm>/<Tên>.tsx` (DOC-12 §6).

## 4. Bảng đối chiếu: UC → màn

Mỗi UC có người dùng thấy (không phải job) phải có ít nhất một màn.

| UC | Màn |
| --- | --- |
| UC-01 | `Login`, `AuthCallback`, [DOC-51](emails.md) `magic-link` |
| UC-02 | `EventList`, `EventDetail`, `SeatPicker` |
| UC-03 | `SeatPicker`, `QuantityPicker` |
| UC-04 | `Checkout`, `OrderResult` |
| UC-05 | `OrderResult`, `MyTickets`, [DOC-51](emails.md) `tickets` |
| UC-06 | `Checkout` |
| UC-07 | `StudioOrganizerProfile`, `StudioEventInfo`, `StudioTicketTypes` |
| UC-08 | `StudioMapEditor` |
| UC-09 | `StudioPreview`, `StudioPublish` |
| UC-10 | `StudioOverview`, `StudioSales` |
| UC-12 | `WaitingRoom` |
| UC-13 | `StudioPublish`, `MyTickets` (vé `VOID`, đơn chờ hoàn tiền), [DOC-51](emails.md) `refund-pending` |
| UC-14 | `StudioMapEditor` (chế độ chỉ đọc sau giờ mở bán, DR-37) |
| UC-15 | `OrderResult` (biến thể "tiền về trễ"), [DOC-51](emails.md) `refund-pending` |
| UC-17 | [DOC-51](emails.md) `event-changed` |
| UC-20 | bộ chuyển ngôn ngữ ở header (DOC-39), mọi màn |
| UC-22 | `StudioMapEditor` (lựa chọn "Dùng lại sơ đồ từ sự kiện khác") |
| UC-11, UC-16, UC-18, UC-19, UC-21 | Không có màn: là job, lệnh `make` hoặc thao tác ngoài ứng dụng (Stripe Dashboard, `psql`; RB-01) |

## 5. Test bắt buộc

Tiền tố `SCR-`. Chạy trong job kiểm tra tài liệu và test frontend khi có router.

| ID | Kiểm tra | Kỳ vọng |
| --- | --- | --- |
| SCR-01 | Mọi dòng bảng URL của DOC-38 §3 xuất hiện ở §1.1–1.2 của tài liệu này | Không thiếu, không thừa |
| SCR-02 | Mọi spec ở cột "Spec" tồn tại đúng đường dẫn của master plan §3.1 hoặc là "chưa viết" kèm DOC-xx | Không có đường dẫn sai |
| SCR-03 | Tên component trong DOC-82 §3 trùng cột "Component" ở §1 (sau khi sửa theo §3) | Bằng nhau |
| SCR-04 | Mỗi `FL-xx` ở cột "Luồng" có cột Màn hình ở DOC-82 §2 chứa component đó | Hai chiều khớp |
| SCR-05 | Router: `/events/:eventId/seats` render `SeatPicker` khi sự kiện có `SEAT`/`ZONE`, `QuantityPicker` khi chỉ GA | Đúng component |
| SCR-06 | Router: URL không khớp route → `NotFoundPage`; `/studio/*` khi không có `ORGANIZER` → `ForbiddenPage` | Đúng trang, đúng mã |
| SCR-07 | Mỗi spec đủ 11 mục theo §2 | Tiêu đề mục đúng thứ tự |

## 6. Tóm tắt quyết định phát sinh khi viết tài liệu này

**DR-141 · Tên component màn hình do DOC-41 chốt, bỏ `SessionExpired`, thêm `SeatsRoute`.** *Vấn đề:* DOC-82 §3 là bản tạm; DOC-44 và DOC-52 đã chốt E2 không phải trang riêng. *Quyết định:* theo bảng §3: bỏ `SessionExpired`, tách hai dòng `Login` và `AuthCallback`, thêm `SeatsRoute`. Người quyết định: Claude (Owner ủy quyền). *Hệ quả:* DOC-82 §2 (cột Màn hình của FL-03) và §3 sửa khi hợp nhất; các spec DOC-42…60 dùng tên ở §1.

## Câu hỏi còn mở

Không có. DR-141 tiếp nối DR-138…140 của DOC-44 và DOC-52; đã vào sổ quyết định.
