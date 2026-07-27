# UC-11 — Đặt Lịch & Thuê MC (MC Booking & Hiring)

Luồng nghiệp vụ tìm kiếm, đặt lịch thuê MC sự kiện, xác nhận báo giá, thanh toán và hoàn tất sự kiện.

| # | Tính năng | Mô tả | Actor |
|---|---|---|---|
| 1 | Tạo yêu cầu đặt lịch MC | Client tạo đơn đặt lịch MC cho sự kiện (ngày, thời gian, tên sự kiện, vị trí) | Client |
| 2 | Kiểm tra lịch rảnh của MC | Client xem danh sách lịch bận/rảnh của MC trước khi gửi yêu cầu | Client |
| 3 | Xem danh sách lịch đặt cá nhân | Client hoặc MC xem danh sách các lịch đặt của mình theo vai trò | Client, MC |
| 4 | Xem chi tiết đơn đặt lịch | Xem đầy đủ thông tin sự kiện, báo giá, yêu cầu đặc biệt và trạng thái | Client, MC |
| 5 | MC phản hồi đơn đặt lịch | MC chấp nhận (Accept) kèm báo giá hoặc từ chối (Reject) kèm lý do | MC |
| 6 | Hủy đơn đặt lịch | Client hoặc MC hủy đơn đặt lịch trước giờ sự kiện | Client, MC |
| 7 | Thanh toán đơn đặt MC | Client thanh toán tiền thuê MC qua cổng PayOS sau khi MC chấp nhận | Client |
| 8 | Đánh giá MC sau sự kiện | Client viết nhận xét và chấm điểm sao cho MC sau khi sự kiện hoàn thành | Client |
| 9 | Xem danh sách đánh giá của MC | Xem tất cả nhận xét và điểm rating trung bình của một MC cụ thể | Công khai |
| 10 | Quản lý lịch bận/rảnh cá nhân | MC cập nhật các khoảng thời gian bận/rảnh theo tuần hoặc ngày | MC |
| 11 | Admin can thiệp đơn đặt | Admin bắt buộc hủy (Force Cancel) hoặc hoàn tất (Force Complete) đơn đặt | Admin |
