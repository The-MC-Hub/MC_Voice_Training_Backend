# ⚡ The MC Hub — Backend Performance Optimization Guide

Tài liệu này ghi lại các nguyên tắc và kỹ thuật tối ưu hóa hiệu suất cho hệ thống Java Backend của The MC Hub.

---

## 1. Xử lý Song song (Parallel Processing)
**Vấn đề**: Các API Dashboard hoặc Landing Page thường gọi nhiều hàm thống kê nối tiếp nhau, gây trễ tích lũy.

**Giải pháp**:
- Sử dụng `CompletableFuture` để chạy song song các tác vụ độc lập.
- Tận dụng **Java 21 Virtual Threads** qua cấu hình `AsyncConfig` để không gây nghẽn Thread Pool.

**Nguyên tắc**:
- Mọi API thống kê (>2 nguồn dữ liệu) phải dùng xử lý bất đồng bộ.
- Dùng `CompletableFuture.allOf(...).join()` để đồng bộ hóa kết quả trước khi trả về.

## 2. Loại bỏ lỗi N+1 Query (Batch Fetching)
**Vấn đề**: Truy vấn danh sách chính (MC) rồi lại gọi truy vấn lẻ cho từng phần tử (User) bên trong vòng lặp.

**Giải pháp**:
- **Batching**: Thu thập tất cả ID cần thiết, gọi 1 câu truy vấn `findAllById(ids)`.
- **Mapping**: Sử dụng Map (RAM) để ánh xạ lại dữ liệu thay vì gọi DB liên tục.

## 3. Tối ưu truy vấn MongoDB (Native Operations)
**Vấn đề**: Sử dụng `.findAll().size()` hoặc Java Stream để tính toán trên tập dữ liệu lớn.

**Giải pháp**:
- **Native Count**: Dùng `repository.countBy...()` để DB tự đếm số lượng (chỉ trả về 1 con số).
- **Native Aggregation**: Dùng `@Aggregation` cho các phép tính tổng (sum), trung bình (average) để xử lý hoàn toàn tại Database.

---
*Cập nhật bởi Antigravity Agent - 2026-05-11*
