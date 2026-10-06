# ADR-0016: Giao diện đa ngôn ngữ vi (mặc định) và en

- Status: Accepted
- Date: 2026-10-06 · Related: DR-10, DR-12, DOC-31, DOC-40, FR-17

## Bối cảnh

Owner chọn giao diện đa ngôn ngữ; SDD gốc không có i18n và canvas chỉ có chuỗi tiếng Việt. Email và lỗi API cũng là giao diện. (Nguồn: DR-10.)

## Các phương án

1. **Một ngôn ngữ (vi).** Nhanh nhưng vi phạm lựa chọn của Owner.
2. **Hai locale `vi`/`en` qua i18n, key tiếng Anh theo nghĩa.**

## Quyết định

Chọn phương án **2**.
- Locale `vi` (mặc định và dự phòng, chuỗi từ canvas) và `en`; thêm locale là thêm file.
- Key tiếng Anh theo nghĩa (`checkout.hold.expired.title`); namespace theo feature.
- Chọn locale: `app_user.locale` → cookie `tb_lang` → `Accept-Language` → `vi`.
- Frontend i18next; backend `MessageSource` (`messages_vi`, `messages_en`) cho email và `title`/`detail` của Problem Details (`code` luôn tiếng Anh).
- Số, tiền, ngày qua `Intl`; giờ theo múi giờ sự kiện. Dữ liệu người tổ chức nhập không dịch.
- CI: `pnpm i18n:check` và test JUnit bắt lệch key.

## Hệ quả

**Tích cực**
- Đáp ứng yêu cầu Owner; thêm ngôn ngữ không đổi code.

**Tiêu cực**
- Gấp đôi khối lượng microcopy và kiểm thử; E2E phải chạy hai locale.

**Việc phát sinh**
- DOC-31, DOC-40; script `i18n:check`.
