# Tài liệu dự án: Hệ thống đặt vé sự kiện

Tài liệu thiết kế gốc: [`../event-ticket-booking-sdd.md`](../event-ticket-booking-sdd.md) (**SDD gốc**). Thiết kế màn hình: canvas "Ticket — Design system & luồng mua vé" (https://claude.ai/artifact/2WEpFawM4q1J9g6zr7vSdU). Tài liệu viết bằng tiếng Việt; code, log, commit bằng tiếng Anh; giao diện đa ngôn ngữ `en`/`vi` (master plan §0.1).

## Bắt đầu từ đây

1. [00-master-plan.md](00-master-plan.md): master plan. Khoảng trống của SDD gốc, toàn bộ 90 tài liệu cần viết kèm nội dung bắt buộc và gate, công việc theo phase P0–P7, ma trận truy vết.
2. [00-decision-register.md](00-decision-register.md): sổ quyết định mở. 151 quyết định (DR-01…151); DR-01…122 và DR-131…151 đã Chốt hoặc Đổi (2026-10-06), DR-123…130 (nhóm ops) đang Đề xuất chờ Owner; spike S-01…S-06 vẫn có thể mở lại mục tương ứng.

Thứ tự đọc cho người mới: master plan §1 → §4 → phase hiện tại ở §5 → các tài liệu phase đó tham chiếu.

## Tài liệu

Cột Gate là phase cần tài liệu ở trạng thái `Approved` trước khi bắt đầu.

| DOC | Tài liệu | Gate | Trạng thái |
| --- | --- | --- | --- |
| DOC-01 | Tầm nhìn và phạm vi ([01-product/vision-and-scope.md](01-product/vision-and-scope.md)) | P1 | Approved |
| DOC-02 | Persona và hành trình ([01-product/personas-and-journeys.md](01-product/personas-and-journeys.md)) | P1 | Approved |
| DOC-03 | Yêu cầu ([01-product/requirements.md](01-product/requirements.md)) | P1 | Approved |
| DOC-04 | Use case ([01-product/use-cases.md](01-product/use-cases.md)) | P1 | Approved |
| DOC-05 | Danh mục tính năng ([01-product/feature-catalog.md](01-product/feature-catalog.md)) | P1 | Approved |
| DOC-06 | Thuật ngữ ([02-glossary.md](02-glossary.md)) | P1 | Approved |
| DOC-07 | Bối cảnh hệ thống và container ([03-architecture/system-context-and-containers.md](03-architecture/system-context-and-containers.md)) | P1 | Approved |
| DOC-08 | Luồng dữ liệu (`03-architecture/data-flows.md`) | P2 | Chưa viết |
| DOC-09 | Hợp đồng tích hợp (`03-architecture/integration-contracts.md`) | P3 | Chưa viết |
| DOC-10 | Thuộc tính chất lượng (`03-architecture/quality-attributes.md`) | P2 | Chưa viết |
| DOC-11 | Stack và phiên bản ([03-architecture/tech-stack-and-versions.md](03-architecture/tech-stack-and-versions.md)) | P1 | Approved |
| DOC-12 | Kiến trúc code ([03-architecture/code-architecture.md](03-architecture/code-architecture.md)) | P1 | Approved |
| DOC-13 | Danh mục ADR ([04-adr/README.md](04-adr/README.md)) | P1 | Approved |
| DOC-14 | Mô hình miền ([05-data/domain-model.md](05-data/domain-model.md)) | P1 | Approved |
| DOC-15 | Mô hình vận hành ([05-data/ops-model.md](05-data/ops-model.md)) | P1 | Approved |
| DOC-16 | Tài liệu sơ đồ (`05-data/seat-map-document.md`) | P4 | Chưa viết |
| DOC-17 | Key Redis (`05-data/redis-keys.md`) | P5 | Chưa viết |
| DOC-18 | Vòng đời dữ liệu (`05-data/data-lifecycle.md`) | P2 | Chưa viết |
| DOC-19 | Xác thực và session ([06-design/auth-and-sessions.md](06-design/auth-and-sessions.md)) | P1 | Approved |
| DOC-20 | Sự kiện và loại vé (`06-design/events-and-ticket-types.md`) | P2 | Chưa viết |
| DOC-21 | Hình học và validate của map-core (`06-design/map-core-geometry-and-validation.md`) | P4 | Chưa viết |
| DOC-22 | Seat map editor (`06-design/seat-map-editor.md`) | P4 | Chưa viết |
| DOC-23 | Phiên bản sơ đồ, nhân bản và so sánh (`06-design/map-versioning-and-diff.md`) | P4 | Chưa viết |
| DOC-24 | Kho vé và giữ vé (`06-design/inventory-and-reservation.md`) | P2 | Chưa viết |
| DOC-25 | Idempotency (`06-design/idempotency.md`) | P2 | Chưa viết |
| DOC-26 | Checkout và thanh toán (`06-design/checkout-and-payment.md`) | P3 | Chưa viết |
| DOC-27 | Vé và thông báo ([06-design/tickets-and-notifications.md](06-design/tickets-and-notifications.md)) | P1 | Approved |
| DOC-28 | Kiểm soát tiếp nhận (`06-design/admission-control.md`) | P6 | Chưa viết |
| DOC-29 | Trình xem sơ đồ và tình trạng chỗ (`06-design/seat-viewer-and-availability.md`) | P5 | Chưa viết |
| DOC-30 | Kiểm tra bất biến (`06-design/invariant-checker.md`) | P2 | Chưa viết |
| DOC-31 | Đa ngôn ngữ ([06-design/i18n.md](06-design/i18n.md)) | P1 | Approved |
| DOC-32 | Bảo mật ([06-design/security.md](06-design/security.md)) | P1 | Approved |
| DOC-33 | Observability ([06-design/observability.md](06-design/observability.md)) | P1 | Approved |
| DOC-34 | Tham chiếu cấu hình ([06-design/configuration-reference.md](06-design/configuration-reference.md)) | P1 | Approved |
| DOC-35 | Xử lý lỗi ([06-design/error-handling.md](06-design/error-handling.md)) | P1 | Approved |
| DOC-36 | Hướng dẫn API ([07-api/api-guidelines.md](07-api/api-guidelines.md)) | P1 | Approved |
| DOC-37 | Danh mục endpoint ([07-api/api-endpoints.md](07-api/api-endpoints.md)) | P1 | Approved |
| DOC-38 | Nguyên tắc UX và kiến trúc thông tin ([08-ux-ui/ux-principles-and-ia.md](08-ux-ui/ux-principles-and-ia.md)) | P1 | Approved |
| DOC-39 | Design system ([08-ux-ui/design-system.md](08-ux-ui/design-system.md)) | P1 | Approved |
| DOC-40 | Trạng thái giao diện và microcopy ([08-ux-ui/ui-states-and-copy.md](08-ux-ui/ui-states-and-copy.md)) | P1 | Approved |
| DOC-41 | Danh mục màn hình ([08-ux-ui/screens/README.md](08-ux-ui/screens/README.md)) | P1 | Approved |
| DOC-42 | Màn: Danh sách sự kiện (`08-ux-ui/screens/event-list.md`) | P2 | Chưa viết |
| DOC-43 | Màn: Sự kiện (`08-ux-ui/screens/event-detail.md`) | P2 | Chưa viết |
| DOC-44 | Màn: Đăng nhập ([08-ux-ui/screens/login.md](08-ux-ui/screens/login.md)) | P1 | Approved |
| DOC-45 | Màn: Phòng chờ (`08-ux-ui/screens/waiting-room.md`) | P6 | Chưa viết |
| DOC-46 | Màn: Chọn chỗ (`08-ux-ui/screens/seat-picker.md`) | P5 | Chưa viết |
| DOC-47 | Màn: Chọn số lượng (`08-ux-ui/screens/quantity-picker.md`) | P2 | Chưa viết |
| DOC-48 | Màn: Thanh toán (`08-ux-ui/screens/checkout.md`) | P2 | Chưa viết |
| DOC-49 | Màn: Kết quả (`08-ux-ui/screens/order-result.md`) | P2 | Chưa viết |
| DOC-50 | Màn: Vé của tôi (`08-ux-ui/screens/my-tickets.md`) | P2 | Chưa viết |
| DOC-51 | Email ([08-ux-ui/screens/emails.md](08-ux-ui/screens/emails.md)) | P1 | Approved |
| DOC-52 | Trang lỗi ([08-ux-ui/screens/error-pages.md](08-ux-ui/screens/error-pages.md)) | P1 | Approved |
| DOC-53 | Màn Studio: Lập hồ sơ tổ chức (`08-ux-ui/screens/studio-organizer-profile.md`) | P2 | Chưa viết |
| DOC-54 | Màn Studio: Tổng quan (`08-ux-ui/screens/studio-overview.md`) | P2 | Chưa viết |
| DOC-55 | Màn Studio: Thông tin sự kiện (`08-ux-ui/screens/studio-event-info.md`) | P2 | Chưa viết |
| DOC-56 | Màn Studio: Loại vé và giá (`08-ux-ui/screens/studio-ticket-types.md`) | P2 | Chưa viết |
| DOC-57 | Màn Studio: Seat map editor (`08-ux-ui/screens/studio-map-editor.md`) | P4 | Chưa viết |
| DOC-58 | Màn Studio: Xem trước (`08-ux-ui/screens/studio-preview.md`) | P2 | Chưa viết |
| DOC-59 | Màn Studio: Xuất bản và mở bán (`08-ux-ui/screens/studio-publish.md`) | P2 | Chưa viết |
| DOC-60 | Màn Studio: Theo dõi bán vé (`08-ux-ui/screens/studio-sales.md`) | P5 | Chưa viết |
| DOC-61 | Môi trường dev ([09-operations/local-dev.md](09-operations/local-dev.md)) | P1 | Approved |
| DOC-62 | Triển khai compose ([09-operations/deploy-compose.md](09-operations/deploy-compose.md)) | P1 | Approved |
| DOC-63 | CI ([09-operations/ci-cd.md](09-operations/ci-cd.md)) | P1 | Approved |
| DOC-64 | Danh mục runbook (`09-operations/runbooks/README.md`) | P3 | Chưa viết |
| DOC-65 | RB-01 Hoàn tiền thủ công đơn REFUND_PENDING (`09-operations/runbooks/RB-01-manual-refund.md`) | P3 | Chưa viết |
| DOC-66 | RB-02 Sai lệch bất biến (`09-operations/runbooks/RB-02-invariant-violation.md`) | P2 | Chưa viết |
| DOC-67 | RB-03 Reservation kẹt và webhook thất lạc (`09-operations/runbooks/RB-03-stuck-reservations-and-webhooks.md`) | P3 | Chưa viết |
| DOC-68 | RB-04 Đặt lại dữ liệu demo (`09-operations/runbooks/RB-04-reset-demo-data.md`) | P7 | Chưa viết |
| DOC-69 | Chiến lược kiểm thử ([10-testing/test-strategy.md](10-testing/test-strategy.md)) | P1 | Approved |
| DOC-70 | Thực nghiệm: giao thức chung (`10-testing/experiments/README.md`) | P2 | Chưa viết |
| DOC-71 | EXP-01 Tranh một ghế (`10-testing/experiments/EXP-01-single-seat-contention.md`) | P5 | Chưa viết |
| DOC-72 | EXP-02 Tranh một pool (`10-testing/experiments/EXP-02-pool-contention.md`) | P2 | Chưa viết |
| DOC-73 | EXP-03 Idempotency (`10-testing/experiments/EXP-03-idempotency.md`) | P2 | Chưa viết |
| DOC-74 | EXP-04 Hết hạn dưới tải (`10-testing/experiments/EXP-04-expiry-under-load.md`) | P2 | Chưa viết |
| DOC-75 | EXP-05 Kiểm soát tiếp nhận và connection pool (`10-testing/experiments/EXP-05-admission-and-connection-pool.md`) | P6 | Chưa viết |
| DOC-76 | EXP-06 Confirm đua với expire (`10-testing/experiments/EXP-06-confirm-vs-expire-race.md`) | P3 | Chưa viết |
| DOC-77 | EXP-07 Webhook trùng và sai thứ tự (`10-testing/experiments/EXP-07-duplicate-out-of-order-webhooks.md`) | P3 | Chưa viết |
| DOC-78 | EXP-08 Sự cố (`10-testing/experiments/EXP-08-fault-injection.md`) | P6 | Chưa viết |
| DOC-79 | EXP-09 Hiệu năng editor (`10-testing/experiments/EXP-09-editor-performance.md`) | P4 | Chưa viết |
| DOC-80 | EXP-10 Một dòng mỗi vé và bộ đếm (`10-testing/experiments/EXP-10-unit-rows-vs-counter.md`) | P2 | Chưa viết |
| DOC-81 | Kịch bản demo (`10-testing/demo-script.md`) | P7 | Chưa viết |
| DOC-82 | Mục lục luồng chi tiết ([06-design/flows/README.md](06-design/flows/README.md)) | P1 | Approved |
| DOC-83 | Luồng: Đăng nhập và tài khoản ([06-design/flows/auth-and-account.md](06-design/flows/auth-and-account.md)) | P1 | Approved |
| DOC-84 | Luồng: Studio sự kiện (`06-design/flows/studio-events.md`) | P2 | Chưa viết |
| DOC-85 | Luồng: Mua vé GA (`06-design/flows/ga-purchase.md`) | P2 | Chưa viết |
| DOC-86 | Luồng: Vận hành và thực nghiệm (`06-design/flows/operations.md`) | P2 | Chưa viết |
| DOC-87 | Luồng: Thanh toán (`06-design/flows/payment.md`) | P3 | Chưa viết |
| DOC-88 | Luồng: Soạn sơ đồ chỗ ngồi (`06-design/flows/seat-map-editing.md`) | P4 | Chưa viết |
| DOC-89 | Luồng: Bán theo ghế và khu vực (`06-design/flows/seat-sales.md`) | P5 | Chưa viết |
| DOC-90 | Luồng: Phòng chờ (`06-design/flows/waiting-room.md`) | P6 | Chưa viết |

ADR: danh sách ADR-0001…0018 ở master plan §3.2; mục lục `04-adr/README.md` (DOC-13) được tạo khi viết ADR đầu tiên.

Tài liệu chưa viết ghi đường dẫn dạng code; khi file được tạo, đường dẫn đổi thành link.

## Cấu trúc

| Thư mục | Nội dung |
| --- | --- |
| `01-product/` | Tầm nhìn, persona, yêu cầu, use case, danh mục tính năng |
| `02-glossary.md` | Thuật ngữ |
| `03-architecture/` | Bối cảnh và container, luồng dữ liệu, hợp đồng tích hợp, thuộc tính chất lượng, stack, kiến trúc code |
| `04-adr/` | Quyết định kiến trúc |
| `05-data/` | Mô hình miền và DDL, bảng vận hành, tài liệu sơ đồ, key Redis, vòng đời dữ liệu |
| `06-design/` | Thiết kế từng thành phần, các tài liệu xuyên suốt (bảo mật, observability, cấu hình, lỗi, i18n) và luồng chi tiết `flows/` (FL-01…35, mỗi UC qua từng tầng) |
| `07-api/` | Quy ước API, danh mục endpoint |
| `08-ux-ui/` | Nguyên tắc UX, design system, microcopy, đặc tả từng màn hình |
| `09-operations/` | Môi trường dev, triển khai compose, CI, runbook |
| `10-testing/` | Chiến lược kiểm thử, 10 thực nghiệm, kịch bản demo |

## Quy ước

- Dòng đầu mỗi file ghi trạng thái (`Draft | Review | Approved | Superseded`) và ngày cập nhật.
- Sơ đồ dùng Mermaid.
- Định danh theo master plan §0.4.
- Thay đổi hành vi thì sửa tài liệu trong cùng PR. Đổi quyết định thì viết ADR mới thay ADR cũ.
