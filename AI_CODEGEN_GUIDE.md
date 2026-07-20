# 🧭 The MC Hub — AI Intelligence & Codegen Guide

Bản hướng dẫn này hợp nhất các quy tắc từ GitNexus, Caveman Mode và Performance Optimization. Đây là kim chỉ nam **bắt buộc** cho mọi AI Agent khi làm việc trong dự án này.

---

## PHẦN 1: 🧠 Code Intelligence (GitNexus)

Dự án được index bởi GitNexus dưới tên **MC_Voice_Training_Backend**. Sử dụng các công cụ MCP để hiểu mã nguồn và đánh giá tác động an toàn.

### 1. Quy trình bắt buộc (Always Do)
- **Impact Analysis**: LUÔN LUÔN chạy `gitnexus_impact` trước khi sửa bất kỳ symbol nào. Báo cáo Blast Radius (risk level) cho người dùng.
- **Detect Changes**: LUÔN LUÔN chạy `gitnexus_detect_changes()` trước khi commit để xác nhận thay đổi chỉ nằm trong phạm vi dự kiến.
- **Cảnh báo**: Phải cảnh báo người dùng nếu Risk Level là **HIGH** hoặc **CRITICAL**.
- **Exploration**: Dùng `gitnexus_query` để tìm Execution Flows thay vì dùng `grep`.

### 2. Những điều cấm (Never Do)
- KHÔNG sửa code mà không chạy `impact analysis`.
- KHÔNG bỏ qua các cảnh báo HIGH/CRITICAL.
- KHÔNG rename symbol bằng tìm-và-thay-thế (Find-and-Replace) — Hãy dùng `gitnexus_rename`.

---

## PHẦN 2: 🪨 Triết lý "Caveman Talk" (Giao tiếp tinh gọn)

Dự án sử dụng **Caveman Mode (Mức độ Full)**. Cắt giảm 75% lượng từ ngữ dư thừa, chỉ giữ lại độ chính xác kỹ thuật 100%.

**Quy tắc phản hồi:**
1. **Không giải thích dài dòng**: Không chào hỏi, cảm ơn, xin lỗi.
2. **Ngôn ngữ tiền sử**: Dùng từ khóa, mệnh đề ngắn. (Ví dụ: "Bug in filter args. Fix:").
3. **Mã nguồn tối thiểu**: Chỉ in ra các dòng thực sự thay đổi qua `replace_file_content`.

---

## PHẦN 3: ⚡ Nguyên tắc tối ưu hiệu suất (Performance & Optimization)

AI PHẢI tuân thủ các quy tắc tối ưu hóa để hệ thống vận hành mượt mà với dữ liệu lớn:

1. **Không dùng `.size()` trên List lớn**: Luôn ưu tiên dùng `countBy...()` trong Repository để DB tự đếm.
2. **Không tính toán bằng Java Stream trên tập dữ liệu lớn**: Dùng `@Aggregation` trong MongoDB để tính SUM/AVG trực tiếp tại Database.
3. **Giải quyết N+1 Query**: Cấm gọi truy vấn DB bên trong vòng lặp (`map`, `forEach`). Phải dùng Batch Fetching (`findAllById`).
4. **Xử lý song song**: Các API thống kê nhiều nguồn phải dùng `CompletableFuture` kết hợp Virtual Threads.

*Chi tiết kỹ thuật xem tại: [BACKEND_PERFORMANCE_GUIDE.md](./BACKEND_PERFORMANCE_GUIDE.md)*

---

## 🛠️ Danh mục tài nguyên nhanh (GitNexus)

| Tài nguyên | Mục đích |
| :--- | :--- |
| `context` | Xem view 360 độ của một class. |
| `process` | Truy vết luồng thực thi (Execution Trace). |
| `impact` | Kiểm tra ảnh hưởng trước khi sửa code. |
| `cypher` | Truy vấn các mối quan hệ phức tạp trong Graph. |

**Ghi chú**: Nếu index bị cũ, hãy chạy `npx gitnexus analyze` trong terminal.
