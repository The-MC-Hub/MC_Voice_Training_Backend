# UC-09 — Quản trị Hệ thống & Người dùng (Admin Dashboard)

Công cụ vận hành nội bộ dành cho Admin: thống kê, quản lý user, cấu hình hệ thống.

| # | Tính năng | Mô tả |
|---|---|---|
| 1 | Xem tổng quan dashboard | Admin xem số liệu tổng quan toàn hệ thống |
| 2 | Xem danh sách giao dịch | Admin xem toàn bộ giao dịch thanh toán |
| 3 | Xem thống kê doanh thu | Admin xem báo cáo doanh thu theo thời gian |
| 4 | Xem phân tích nền tảng | Admin xem các chỉ số phân tích hoạt động chung |
| 5 | Xem phân tích tăng trưởng | Admin xem xu hướng tăng trưởng người dùng theo thời gian |
| 6 | Xem danh sách người dùng | Admin xem toàn bộ tài khoản user trong hệ thống |
| 7 | Xem chi tiết một người dùng | Admin xem thông tin chi tiết của một user cụ thể |
| 8 | Xem danh sách MC | Admin xem danh sách các tài khoản có vai trò MC |
| 9 | Đổi trạng thái tài khoản | Admin kích hoạt/vô hiệu hoá hoặc xác minh một tài khoản user |
| 10 | Đổi gói cước thủ công | Admin thay đổi gói cước của một user thủ công |
| 11 | Tạo tài khoản mới | Admin tạo tài khoản user mới trực tiếp từ trang quản trị |
| 12 | Gửi email đặt lại mật khẩu | Admin kích hoạt gửi email reset mật khẩu cho một user |
| 13 | Đổi mật khẩu người dùng | Admin đặt mật khẩu mới trực tiếp cho một user |
| 14 | Xoá tài khoản người dùng | Admin xoá vĩnh viễn một tài khoản user |
| 15 | Xem thống kê sử dụng của user | Admin xem chi tiết hoạt động sử dụng của một user cụ thể |
| 16 | Gửi email thông báo cho user | Admin gửi một email thông báo tuỳ chỉnh đến một user |
| 17 | Chạy migration database | Admin kích hoạt tác vụ migrate dữ liệu database |
| 18 | Xem/sửa cấu hình cooldown dùng thử | Admin xem và điều chỉnh thời gian chờ giữa các lượt dùng thử miễn phí cho khách |
| 19 | Xem log hệ thống realtime | Admin theo dõi log hệ thống trực tiếp qua stream (SSE) |
| 20 | Xem log hệ thống theo bộ lọc | Admin tra cứu log gần đây theo mức độ/nguồn |
| 21 | Ghi nhận log từ AI service | AI service bên ngoài đẩy log vào hệ thống trung tâm |
| 22 | Xem audit log toàn hệ thống | Admin xem lịch sử thao tác quan trọng trong hệ thống |
| 23 | Xem audit log theo user | Admin xem lịch sử thao tác của một user cụ thể |
| 24 | Xoá audit log cũ | Admin dọn dẹp audit log cũ hơn N ngày (giữ tối thiểu 3 ngày) |
| 25 | Xuất file CSV Audit Log | Admin tải xuống toàn bộ nhật ký an ninh dạng CSV |
| 26 | Khóa tài khoản có thời hạn | Admin khóa tạm thời tài khoản theo số ngày chỉ định (1, 3, 7, 30 ngày) |
| 27 | Tự động khôi phục tài khoản | System tự động bỏ ban khi hết thời hạn khóa temporary |
| 28 | Theo dõi sức khỏe hệ thống | Admin xem các chỉ số RAM JVM, Active Virtual Threads, Uptime, DB Ping |
| 29 | Bật/tắt Chế độ Bảo trì | Admin toggle `MAINTENANCE_MODE` tức thì (HTTP 503 cho client thường) |
| 30 | Hoàn tiền giao dịch | Admin đánh dấu giao dịch `REFUNDED` và lưu lý do hoàn tiền |
| 31 | Cấp tặng gói cước thủ công | Admin cộng ngày VIP/Plan thủ công cho user kèm lý do đền bù |
| 32 | Quản lý & duyệt báo cáo vi phạm | Admin xem, duyệt (Resolve), bỏ qua (Dismiss) hoặc xóa báo cáo vi phạm |
| 33 | Xử lý báo cáo hàng loạt | Admin chọn nhiều báo cáo để duyệt/bỏ qua cùng lúc |
| 34 | Quản lý & thu hồi chứng chỉ | Admin xem danh sách chứng chỉ, xác minh (Verify) hoặc thu hồi (Revoke) |
| 35 | Quản lý Peer Review | Admin xem điểm trung bình review đồng nghiệp và xóa đánh giá vi phạm |
| 36 | Kiểm duyệt cộng đồng & ghi chú | Admin xem ghi chú bài học của user và gỡ bỏ nội dung vi phạm |
| 37 | Gửi thông báo phân đoạn | Admin gửi thông báo Push/Email theo phân đoạn user (VIP sắp hết hạn, Dormant 14 ngày) |

