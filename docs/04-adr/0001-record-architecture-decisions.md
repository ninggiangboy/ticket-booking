# ADR-0001: Ghi quyết định kiến trúc bằng ADR theo mẫu MADR rút gọn

- Status: Accepted
- Date: 2026-10-06 · Related: DOC-13, master plan Phụ lục A.1

## Bối cảnh

Dự án cần nơi ghi lại các quyết định khó đảo ngược và lý do. Sổ quyết định (`00-decision-register.md`) chứa mọi DR, kể cả mục nhỏ; chỉ những mục mức kiến trúc cần một bản ghi bất biến, đọc độc lập. (Nguồn: Quy ước.)

## Các phương án

1. **Chỉ dùng sổ quyết định.** Một nơi, nhưng DR có thể bị sửa và lẫn mục nhỏ với mục kiến trúc.
2. **ADR theo MADR rút gọn bên cạnh sổ quyết định.** Mỗi ADR một quyết định, không sửa nội dung.

## Quyết định

Chọn phương án **2**.
- DR ở mức kiến trúc (khó đảo, ảnh hưởng nhiều module) thành ADR trong `docs/04-adr/NNNN-kebab-title.md`, mẫu A.1 (Bối cảnh, Các phương án, Quyết định, Hệ quả).
- Tiêu đề nêu quyết định, không nêu chủ đề. Trạng thái `Proposed → Accepted → Superseded by ADR-yyyy`.
- ADR không bị sửa nội dung; muốn đổi thì viết ADR mới và đánh dấu ADR cũ `Superseded by`.
- Mục lục ở `04-adr/README.md` (DOC-13).

## Hệ quả

**Tích cực**
- Quyết định quan trọng có bản ghi ngắn, đọc được không cần đọc cả sổ.
- Quy ước thống nhất cho người làm mới.

**Tiêu cực**
- Hai nơi cần giữ khớp (DR và ADR); mỗi ADR phải dẫn DR gốc.

**Việc phát sinh**
- Cập nhật DOC-13 mỗi khi thêm hoặc đổi trạng thái ADR.
