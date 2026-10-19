# Màn hình Studio: Lập hồ sơ tổ chức

> Trạng thái: **Review** · Cập nhật: 2026-10-15 · DOC-53
> Phụ thuộc: SDD gốc §3.1, §5.3, [Sổ quyết định](../../00-decision-register.md) (DR-10, 21, 22, 23, 85, 88, 131, 132, 136, 140), [DOC-02](../../01-product/personas-and-journeys.md) (PS-3, PS-4, J-3 bước 1, J-4 bước 1), [DOC-03](../../01-product/requirements.md) (FR-13), [DOC-04](../../01-product/use-cases.md) (UC-07), [DOC-06](../../02-glossary.md), [DOC-19](../../06-design/auth-and-sessions.md), [DOC-37](../../07-api/api-endpoints.md) (E-04, E-23), [DOC-38](../ux-principles-and-ia.md) §2.2, §3, §4, §6.3, §8.5, [DOC-39](../design-system.md) §7.1, §7.2, §7.9, §7.16, §7.17, [DOC-40](../ui-states-and-copy.md) §1, §3.4, §3.8, §4.1, [DOC-41](README.md), [DOC-52](error-pages.md) (E3), [DOC-82](../../06-design/flows/README.md) (FL-05); canvas: artboard "Studio 00"
> Người dùng chính: P2-02.2 (frontend màn Studio 00), P2-02.1 (hợp đồng E-23), tác giả DOC-84 (FL-05), người viết test E2E studio

Tài liệu mô tả màn Lập hồ sơ tổ chức ở `/studio/profile`: tài khoản đã đăng nhập nhưng chưa có hồ sơ nhập tên tổ chức và email liên hệ để có vai trò `ORGANIZER` (DR-23). Tài liệu cũng chốt **cổng vào Studio** (guard của mọi route `/studio/**`) vì màn này là đích của cổng đó. Ràng buộc dữ liệu và lỗi của API ở E-23 (DOC-37); lời gọi qua các tầng ở FL-05 (DOC-84); chuỗi `vi`/`en` ở DOC-40 §3.4. Màn này chỉ dẫn **key**.

## 1. Persona, use case, quyền

| | |
| --- | --- |
| Persona | PS-3, PS-4 (máy tính); người mua bất kỳ vào từ menu tài khoản (cả 390 px, DR-131) |
| Use case | UC-07 bước 1; lỗi E1 (`ORGANIZER_EXISTS`), E2 (`ORGANIZER_PROFILE_REQUIRED` dẫn tới màn này) |
| Yêu cầu | FR-13 |
| Hành trình | J-3 bước 1, J-4 bước 1 |
| Luồng chi tiết | FL-05 |
| Quyền | **Đã đăng nhập** (DOC-38 §3). Khách → `/login?returnTo=%2Fstudio%2Fprofile`. Tài khoản **đã có** hồ sơ (`GET /me` có `organizer`) vào URL này → `replace` tới `/studio`, không render form (DR-192) |

Đường vào: (1) cổng Studio chuyển tới khi `organizer = null` (§2); (2) mục `common.nav.createOrganizer` trong menu tài khoản (DOC-38 §4, DR-131).

## 2. URL và search params

| Route | Tham số | Quy tắc |
| --- | --- | --- |
| `/studio/profile` | không có | Search param lạ bị bỏ qua, không lỗi |

**Cổng vào Studio** (`StudioGate`, phần tử của router bọc mọi route `/studio/**`, cùng chỗ với `SeatsRoute` của DOC-41 §3; DR-192):

| Người xem | `/studio/profile` | Route `/studio/**` khác |
| --- | --- | --- |
| Khách (E-04 trả 401) | `/login?returnTo=%2Fstudio%2Fprofile` | `/login?returnTo=<đường dẫn hiện tại>` |
| Đã đăng nhập, `organizer = null` | Render màn này | `replace` tới `/studio/profile` (không hiện E3) |
| Đã đăng nhập, `organizer ≠ null` | `replace` tới `/studio` | Render màn được yêu cầu |

Cổng đọc `organizer` từ cache `['me']` (E-04), không gọi API riêng. Ngoài cổng, mọi 403 `ORGANIZER_PROFILE_REQUIRED` từ API trong Studio (ví dụ hồ sơ bị xóa bằng tay khi tab đang mở) cũng `replace` tới `/studio/profile` sau khi `invalidate(['me'])` (DOC-40 §4.1, DR-140). E3 chỉ hiện khi API trả 403 `FORBIDDEN` (DR-140, DOC-52).

## 3. Wireframe

Desktop (≥ 1024 px), canvas "Studio 00". Header Studio (logo + nhãn `Studio`), menu tài khoản ở dạng người mua (chưa có tên tổ chức). Hai cột: thẻ biểu mẫu trái, khối "Trong Studio bạn làm được" phải:

```text
┌──────────────────────────────────────────────────────────────────────────┐
│ ticket.● Studio                                     [vi|en] [Tài khoản ▾]│
├──────────────────────────────────────────────────────────────────────────┤
│ LẬP HỒ SƠ TỔ CHỨC                                                        │
│ Hồ sơ tổ chức cho phép tài khoản của bạn tạo và bán vé sự kiện. Bạn vẫn  │
│ mua vé bằng tài khoản này như bình thường.                               │
│ ┌──────────────────────────────────────┐   TRONG STUDIO BẠN LÀM ĐƯỢC     │
│ │ Tên tổ chức                          │   ✓ Tạo sự kiện: tên, địa điểm, │
│ │ Mọi sự kiện và sơ đồ bạn tạo đều…    │     thời gian, khung mở bán.    │
│ │ ┌──────────────────────────────────┐ │   ✓ Đặt loại vé và giá, rồi vẽ  │
│ │ │ Nhà hát Bến Sông                 │ │     sơ đồ chỗ ngồi nếu …        │
│ │ └──────────────────────────────────┘ │   ✓ Xuất bản, mở bán và theo    │
│ │ Email liên hệ                        │     dõi số vé đã bán.           │
│ │ Không bắt buộc. Để trống thì dùng…   │                                 │
│ │ ┌──────────────────────────────────┐ │   ⓘ Đối soát doanh thu và rút   │
│ │ │                                  │ │     tiền chưa có ở giai đoạn này│
│ │ └──────────────────────────────────┘ │                                 │
│ │ [ TẠO HỒ SƠ VÀ VÀO STUDIO          ] │                                 │
│ └──────────────────────────────────────┘                                 │
└──────────────────────────────────────────────────────────────────────────┘
```

Biến thể "Đã tạo hồ sơ" (canvas "Studio 00", biến thể 3) thay thẻ biểu mẫu bằng thẻ xác nhận; cột phải giữ nguyên:

```text
│ ┌──────────────────────────────────────┐                                 │
│ │ ✓ ĐÃ TẠO HỒ SƠ                       │                                 │
│ │ NHÀ HÁT BẾN SÔNG ĐÃ SẴN SÀNG         │                                 │
│ │ Tài khoản của bạn giờ là tài khoản   │                                 │
│ │ người tạo sự kiện. Bước tiếp theo là │                                 │
│ │ tạo sự kiện đầu tiên.                │                                 │
│ │ [ VÀO STUDIO                       ] │                                 │
│ └──────────────────────────────────────┘                                 │
```

Điện thoại 390 px (không có artboard riêng; Studio thiết kế cho ≥ 1024 px nhưng màn này mở được từ menu tài khoản trên điện thoại, DOC-38 §6.3): một cột, khối "Trong Studio bạn làm được" xuống dưới thẻ biểu mẫu:

```text
┌────────────────────────┐
│ ticket.● Studio [vi|en]│
│ LẬP HỒ SƠ TỔ CHỨC      │
│ Hồ sơ tổ chức cho phép…│
│ ┌────────────────────┐ │
│ │ Tên tổ chức        │ │
│ │ [                ] │ │
│ │ Email liên hệ      │ │
│ │ [                ] │ │
│ │ [TẠO HỒ SƠ VÀ VÀO  │ │
│ │  STUDIO          ] │ │
│ └────────────────────┘ │
│ TRONG STUDIO BẠN LÀM…  │
│ ✓ …                    │
└────────────────────────┘
```

## 4. Vùng và component

Trang là `StudioOrganizerProfile` (`frontend/src/features/studio/StudioOrganizerProfile.tsx`, DOC-41 §1.2), chunk `studio` (DOC-38 §3.1). Biểu mẫu dùng react-hook-form + zod (DR-03).

| Vùng | Component (DOC-39 §7) | Ghi chú |
| --- | --- | --- |
| Header | header Studio + `LanguageSwitch` (§7.19) + `AccountMenu` (§7.16) | Menu ở dạng người mua đã đăng nhập (DOC-38 §4); không có `StudioStepper` |
| Tiêu đề, dẫn | `h1` `studio.profile.title`, đoạn `studio.profile.intro` | |
| Ô tên | `TextField` (§7.2) `studio.profile.name.label`, trợ giúp `studio.profile.name.hint` | `autoComplete="organization"`, tiêu điểm khi vào màn |
| Ô email | `TextField` `type="email"` `noValidate`, `studio.profile.email.label`, trợ giúp `studio.profile.email.hint` | `autoComplete="email"`; **không** điền sẵn email đăng nhập (để trống nghĩa là dùng email đăng nhập, DR-23) |
| Nút chính | `Button primary` `studio.profile.submit` | `loading` `studio.profile.submit.busy` khi chờ E-23 |
| Thông báo | `Notice` (§7.9) ngay trên nút chính | §6, §7 |
| Thẻ đã tạo | thẻ trắng: nhãn mono `studio.profile.done.label` + biểu tượng tích, `h2` `studio.profile.done.title`, `studio.profile.done.body`, `Button primary` `studio.profile.done.cta` | Thay chỗ thẻ biểu mẫu; tiêu điểm vào `h2` |
| Cột phải | `h2` `studio.profile.can.title`, danh sách 3 dòng `studio.profile.can.1…3`, `Notice` `info` `studio.profile.note` | Tĩnh, không phụ thuộc dữ liệu |

Quy tắc ô (khớp E-23, DR-23, DR-21):

| Ô | Trường gửi | Kiểm ở client (zod) | Gửi đi |
| --- | --- | --- | --- |
| Tên tổ chức | `name` | `trim`; rỗng → `required`; `Array.from(value).length > 120` → `too_long` | Chuỗi đã `trim` |
| Email liên hệ | `contactEmail` | `trim`; rỗng → hợp lệ; khác rỗng thì `toLowerCase()`, regex `^[^@\s]+@[^@\s]+\.[^@\s]+$`, ≤ 254 ký tự, sai → `invalid_email` | Rỗng → **không gửi trường**; ngược lại chuỗi đã chuẩn hóa |

Kiểm ở client chỉ để báo sớm; server là nguồn quyết định (422 vẫn hiện đúng ô, §6).

## 5. Dữ liệu

| Dữ liệu | Endpoint | Khi nào | Ghi chú |
| --- | --- | --- | --- |
| Người dùng, `organizer`, `csrfToken` | `GET /me` (E-04) | Cổng Studio, header | Cache `['me']` dùng chung toàn app |
| Tạo hồ sơ | `POST /organizer` (E-23) | Bấm nút chính | Không cần `Idempotency-Key` (DR-45 chỉ 4 endpoint); lặp lại an toàn nhờ `organizer.owner_user_id UNIQUE` → 409 |

Request (body chặt, DR-88: không gửi trường lạ):

```http
POST /api/v1/organizer
X-CSRF-Token: Qm9ZpL1xVw3Rk8TnYc2HfJ6uSd0aEiGb4PzOq7MtXlA
Content-Type: application/json

{"name":"Nhà hát Bến Sông","contactEmail":"lienhe@bensong.example"}
```

Không nhập email liên hệ: `{"name":"Nhà hát Bến Sông"}`.

Response 201 (E-23): `{ "organizerId": "0199a000-2222-7333-8444-555566667777", "name": "Nhà hát Bến Sông", "contactEmail": "lienhe@bensong.example", "createdAt": "2026-10-20T07:55:00Z" }`. Sau 201 client `invalidateQueries(['me'])` và chờ E-04 trả `organizer ≠ null` rồi mới hiện thẻ "Đã tạo" (server đã xóa mục Caffeine của session, E-23), để menu tài khoản đổi sang dạng người tổ chức và cổng Studio cho qua.

Không có kênh real-time, không làm mới định kỳ.

## 6. Tương tác

| Thao tác | Endpoint | Kết quả | Lỗi |
| --- | --- | --- | --- |
| Vào màn | E-04 (cache) | Cổng §2; render biểu mẫu trống, tiêu điểm ô tên | E-04 401 → đăng nhập; `organizer ≠ null` → `replace` `/studio` |
| Rời ô (blur) | — | Kiểm ô đó theo §4; lỗi hiện dưới ô | — |
| Bấm "Tạo hồ sơ và vào Studio" | E-23 | Kiểm toàn biểu mẫu; sai → tiêu điểm ô sai đầu tiên, không gửi. Đúng → gửi; **201** → `invalidate(['me'])` → thẻ "Đã tạo" (§7 trạng thái 3) | Bảng dưới |
| Bấm "Vào Studio" (thẻ đã tạo) | — | `navigate('/studio')` (DOC-54) | — |
| Đổi ngôn ngữ | E-05 | Chuỗi đổi; giá trị đã nhập giữ nguyên | — |

Lỗi của E-23 (DOC-40 §4.1, hiển thị theo `code`, DR-136):

| HTTP · `code` | Hiển thị | Hành động |
| --- | --- | --- |
| 422 `VALIDATION_FAILED`, `errors[]` | Lỗi dưới ô theo `field`: `name`/`required` → `studio.profile.name.error`; `contactEmail`/`invalid_email` → `studio.profile.email.error`; `rule` khác → `validation.<rule>` (`too_long` với `{max}` = 120) | Tiêu điểm ô lỗi đầu tiên; giữ giá trị đã nhập |
| 409 `ORGANIZER_EXISTS` | Thẻ biến thể "Đã có hồ sơ" (§7 trạng thái 5): tiêu đề `studio.profile.exists`, `studio.profile.exists.body`, nút `studio.profile.done.cta` | `invalidate(['me'])`; nút đi `/studio`. Gặp khi tab khác đã tạo, hoặc lần tự thử lại sau khi lần đầu đã thành công nhưng mất response |
| 403 `CSRF_TOKEN_INVALID` | — | Gọi lại E-04 lấy `csrfToken` mới, gửi lại **một** lần (DR-85); lần hai vẫn lỗi → `Notice` `error` `errors.generic` |
| 401 `UNAUTHENTICATED` | — | Chuyển `Login` biến thể hết phiên, `returnTo=/studio/profile` (DOC-38 §8.5); giá trị đã nhập mất (biểu mẫu ngắn, không lưu) |
| 503 `OVERLOADED`, 429 `RATE_LIMITED`, lỗi mạng | `Notice` `warning` `errors.rate_limited.*` (429) hoặc tiêu đề "Đang rất đông" của DOC-40 §1.4 (503, mạng) với đếm ngược | Tự gửi lại **cùng body** theo DR-132 (`clamp(Retry-After,1,32)` hoặc 8→16→32 giây, tối đa 10 lần); nút `loading` suốt thời gian chờ; quá 10 lần → `Notice` `error` `errors.generic` + `common.retry` |
| 500 `INTERNAL_ERROR` | `Notice` `error` `errors.generic` kèm `requestId` chọn được | Nút chính bật lại |

Luồng: FL-05 (DOC-84).

## 7. Trạng thái

| # | Trạng thái (canvas) | Điều kiện vào | Hiển thị |
| --- | --- | --- | --- |
| 1 | Biểu mẫu trống (Studio 00, biến thể 1) | Vào màn, `organizer = null` | Hai ô trống, nút chính bật (lỗi chỉ hiện sau blur hoặc bấm) |
| 2 | Lỗi theo ô (Studio 00, biến thể 2) | Kiểm client sai, hoặc 422 | Ô lỗi viền `--stamp`, thông báo dưới ô `role="alert"`, `aria-invalid="true"`; tiêu điểm ô lỗi đầu tiên |
| 3 | Đã tạo hồ sơ (Studio 00, biến thể 3) | 201 và E-04 đã trả `organizer` | Thẻ "Đã tạo" với `{name}` = `name` của response; tiêu điểm vào `h2`; menu tài khoản đã có "Studio của bạn" |
| 4 | Đang gửi | Chờ E-23 (hoặc E-04 sau 201) | Nút `loading` `studio.profile.submit.busy`, `disabled` + `aria-busy`; hai ô `readOnly` |
| 5 | Đã có hồ sơ | 409 `ORGANIZER_EXISTS` | Thẻ cùng khung biến thể 3: `studio.profile.exists`, `studio.profile.exists.body`, nút `studio.profile.done.cta` |
| 6 | Quá tải | 503/429/mạng khi gửi | `Notice` `warning` đếm ngược, tự gửi lại (§6) |
| 7 | Lỗi gửi | 500, hoặc quá 10 lần tự thử lại | `Notice` `error` `errors.generic`; nút chính bật lại; giá trị giữ nguyên |
| 8 | Đang tải | Cache `['me']` chưa có, E-04 chờ > 150 ms | `Skeleton` đúng hình thẻ biểu mẫu và cột phải; dòng ẩn `studio.profile.loading` |
| 9 | Lỗi tải | E-04 lỗi khác 401 | `LoadError`: `common.loadError.label`, `studio.profile.error.title`, `studio.profile.error.body`, `common.retry` |
| 10 | Không quyền | Khách; hoặc đã có hồ sơ | Không render: chuyển trang theo §2 |
| 11 | Rỗng | Không áp dụng: màn là biểu mẫu, không có danh sách | — |

Khác canvas: không có. Canvas không vẽ trạng thái 4–9; các trạng thái này theo mẫu chung của DOC-40 §1.

## 8. Microcopy

Chuỗi `vi`/`en` ở [DOC-40](../ui-states-and-copy.md) §3.4 (khối "Hồ sơ tổ chức"), §3.8, §4.1. Key dùng ở màn này:

| Nhóm | Key |
| --- | --- |
| Biểu mẫu | `studio.profile.title`, `studio.profile.intro`, `studio.profile.name.label`, `.name.hint`, `.name.error`, `studio.profile.email.label`, `.email.hint`, `.email.error`, `studio.profile.submit` |
| Đã tạo, đã có | `studio.profile.done.label`, `.done.title`, `.done.body`, `.done.cta`, `studio.profile.exists` |
| Cột phải | `studio.profile.can.title`, `.can.1`, `.can.2`, `.can.3`, `studio.profile.note` |
| Lỗi | `validation.too_long`, `validation.<rule>`, `errors.rate_limited.*`, `errors.generic.*`, `common.retry`, `common.loadError.label` |
| Header | `studio.brand`, `common.nav.*` |
| **Key mới** (thêm vào DOC-40 §3.4) | `studio.profile.submit.busy` "Đang tạo hồ sơ…" / "Creating profile…"; `studio.profile.exists.body` "Mỗi tài khoản có một hồ sơ tổ chức. Bạn vào Studio được ngay." / "Each account has one organizer profile. You can open Studio now."; `studio.profile.loading` "Đang tải hồ sơ…" / "Loading your profile…"; `studio.profile.error.title` "Không tải được thông tin tài khoản" / "Couldn't load your account"; `studio.profile.error.body` "Kiểm tra kết nối mạng rồi thử lại." / "Check your connection and try again." |

Tên tổ chức người dùng nhập không dịch (DOC-40 §0).

## 9. Tiêu chí nghiệm thu

Tiền tố `SOP-` (đã kiểm tra chưa dùng ở `docs/`).

| ID | Given | When | Then |
| --- | --- | --- | --- |
| SOP-AC-01 | Khách | Mở `/studio/profile` | Điều hướng `/login?returnTo=%2Fstudio%2Fprofile`; không có request E-23 |
| SOP-AC-02 | Đã đăng nhập, `organizer = null` | Mở `/studio` | `replace` tới `/studio/profile`; **không** hiện E3; lịch sử trình duyệt không có `/studio` |
| SOP-AC-03 | Đã đăng nhập, `organizer = null` | Mở `/studio/events/<id>/info` | `replace` tới `/studio/profile`; không có request `/organizer/**` |
| SOP-AC-04 | `organizer ≠ null` | Mở `/studio/profile` | `replace` tới `/studio`; biểu mẫu không render |
| SOP-AC-05 | Biểu mẫu trống | Bấm nút chính | Không có request; ô tên hiện "Hãy nhập tên tổ chức."; tiêu điểm ở ô tên |
| SOP-AC-06 | Tên "  Nhà hát Bến Sông  ", email trống | Bấm nút chính | Body đúng `{"name":"Nhà hát Bến Sông"}` (không có `contactEmail`), có header `X-CSRF-Token` |
| SOP-AC-07 | Email "LienHe@BenSong.Example " | Bấm nút chính | Body có `"contactEmail":"lienhe@bensong.example"` |
| SOP-AC-08 | Email "lienhe@bensong" | Rời ô | Hiện "Email chưa đúng định dạng." dưới ô email; `aria-invalid="true"` |
| SOP-AC-09 | Tên 121 ký tự | Bấm nút chính | Hiện `validation.too_long` với "Tối đa 120 ký tự."; không gửi |
| SOP-AC-10 | E-23 trả 422 `errors:[{field:"contactEmail",rule:"invalid_email"}]` | Bấm nút chính | Lỗi hiện dưới ô email, tiêu điểm vào ô email; giá trị giữ nguyên |
| SOP-AC-11 | E-23 trả 201, E-04 sau đó trả `organizer` | Bấm nút chính | Thẻ "Đã tạo hồ sơ" với "Nhà hát Bến Sông đã sẵn sàng"; menu tài khoản có "Studio của bạn"; bấm "Vào Studio" → `/studio` |
| SOP-AC-12 | E-23 trả 409 `ORGANIZER_EXISTS` | Bấm nút chính | Thẻ "Tổ chức của bạn đã có hồ sơ."; E-04 được gọi lại; nút "Vào Studio" → `/studio` |
| SOP-AC-13 | E-23 trả 503 `Retry-After: 2` rồi 201 | Bấm nút chính | `Notice` `warning` đếm ngược; lần gửi thứ hai sau 2 giây có body giống byte-by-byte; kết thúc ở trạng thái 3 |
| SOP-AC-14 | E-23 trả 403 `CSRF_TOKEN_INVALID` rồi 201 | Bấm nút chính | Đúng một lần gọi lại E-04 và một lần gửi lại E-23; kết thúc ở trạng thái 3 |
| SOP-AC-15 | Đang gửi | Quan sát | Nút `disabled` + `aria-busy="true"`, nhãn "Đang tạo hồ sơ…"; bấm lại không tạo request thứ hai |
| SOP-AC-16 | 390 px | Mở màn | Một cột, không cuộn ngang; khối "Trong Studio bạn làm được" dưới thẻ biểu mẫu |
| SOP-AC-17 | axe-core | Trạng thái 1, 2, 3, 5 | 0 vi phạm; mỗi ô có nhãn nhìn thấy và `aria-describedby` trỏ trợ giúp hoặc lỗi |

## 10. Ca kiểm thử E2E

Playwright; API thật với `make reset seed` và tài khoản mới `e2e-<ngẫu nhiên>@demo.test` đăng nhập qua Mailpit (cách của DOC-44); mock cho lỗi. Mỗi ca chạy `vi` và `en`.

| ID | Tên | Bước | Kỳ vọng |
| --- | --- | --- | --- |
| SOP-01 | `studio-profile-create` | Tài khoản mới mở `/studio` → được đưa tới `/studio/profile`; nhập tên, bỏ trống email; gửi | Thẻ "Đã tạo"; "Vào Studio" → `/studio` hiện trạng thái rỗng của DOC-54 (khớp P2-02.2) |
| SOP-02 | `studio-profile-validation` | Gửi trống; rồi nhập email sai | SOP-AC-05, SOP-AC-08 |
| SOP-03 | `studio-profile-exists` (mock) | Mock E-23 trả 409 `ORGANIZER_EXISTS` | SOP-AC-12 (khớp P2-02.2) |
| SOP-04 | `studio-profile-already-organizer` | `organizer@demo.test` mở `/studio/profile` | Về `/studio` (SOP-AC-04) |
| SOP-05 | `studio-gate-no-profile` | Tài khoản mới mở `/studio/events/<id sự kiện seed>/info` | Về `/studio/profile` (SOP-AC-03) |
| SOP-06 | `studio-profile-from-menu` | `buyer1@demo.test` mở menu tài khoản, chọn "Tạo hồ sơ tổ chức" | Tới `/studio/profile` (DR-131) |
| SOP-07 | `studio-profile-overloaded` (mock) | 503 rồi 201 | SOP-AC-13 |
| SOP-08 | `studio-profile-mobile` | 390 × 844, như SOP-01 | SOP-AC-16 |

## Quyết định phát sinh khi viết tài liệu này

**DR-192 · Cổng vào Studio: chưa có hồ sơ thì chuyển tới Lập hồ sơ ở mọi route `/studio/**`; đã có hồ sơ thì `/studio/profile` về `/studio`.** *Vấn đề:* các tài liệu nói khác nhau về người đã đăng nhập mà chưa có vai trò `ORGANIZER` khi vào Studio: DOC-41 §5 SCR-06 và DOC-40 §1.5 dòng 3 ghi hiện E3 (`ForbiddenPage`); DOC-38 §3 ghi E3 cho `/studio/**` nhưng chuyển tới `/studio/profile` cho riêng `/studio`; DR-140, DOC-52 ERP-06, DOC-40 §4.1 ghi 403 `ORGANIZER_PROFILE_REQUIRED` luôn chuyển tới `/studio/profile`. Theo DR-23 vai trò `ORGANIZER` có khi và chỉ khi có hồ sơ, nên "không phải người tổ chức" và "chưa có hồ sơ" là một trường hợp; E3 ở đây là ngõ cụt vì việc cần làm là lập hồ sơ. Chưa tài liệu nào nói người đã có hồ sơ mở `/studio/profile` thì sao. *Quyết định:* `StudioGate` bọc mọi route `/studio/**`, đọc `organizer` từ cache E-04: khách → đăng nhập với `returnTo`; `organizer = null` → `replace` tới `/studio/profile` (mọi route, không chỉ `/studio`); `organizer ≠ null` mở `/studio/profile` → `replace` tới `/studio`. E3 chỉ hiện khi API thật sự trả 403 `FORBIDDEN` (DR-140). Người quyết định: Claude (Owner ủy quyền). *Hệ quả:* SCR-06 sửa thành "`/studio/*` khi không có `ORGANIZER` → chuyển tới `/studio/profile`; API 403 `FORBIDDEN` → `ForbiddenPage`"; DOC-38 §3 gạch đầu dòng "Riêng người đã có session…" sửa cho khớp; DOC-40 §1.5 dòng "Đã đăng nhập nhưng không phải người tổ chức" đổi thành "API trả 403 `FORBIDDEN` ở `/studio/**`". *Ghi vào:* DOC-53, DOC-38, DOC-40, DOC-41, DOC-84.

## 11. Câu hỏi còn mở

Không có. Quyết định phát sinh: DR-192 (mục trên).
