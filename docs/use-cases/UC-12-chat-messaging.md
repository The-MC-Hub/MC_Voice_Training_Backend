# UC-12 — Trò Chuyện & Nhắn Tin Trực Tiếp (Chat & Messaging)

Luồng giao tiếp trực tiếp giữa Client và MC thông qua WebSocket và REST Messaging API.

| # | Tính năng | Mô tả | Actor |
|---|---|---|---|
| 1 | Tạo cuộc trò chuyện mới | Client mở cuộc trò chuyện mới với MC từ trang profile hoặc đơn đặt | Client |
| 2 | Xem danh sách hội thoại | Xem danh sách các cuộc hội thoại gần đây kèm tin nhắn mới nhất | User |
| 3 | Gửi tin nhắn trực tiếp | Gửi tin nhắn văn bản hoặc đính kèm hình ảnh/file tới người nhận | User |
| 4 | Xem lịch sử tin nhắn | Tải danh sách tin nhắn theo cuộc hội thoại có phân trang | User |
| 5 | Nhận tin nhắn Realtime | Nhận tin nhắn ngay lập tức qua kết nối WebSocket (`/ws-chat`) | User |
| 6 | Đánh dấu đã đọc | Tự động cập nhật trạng thái tin nhắn thành đã đọc khi xem hội thoại | User |
| 7 | Xem tổng tin nhắn chưa đọc | Lấy tổng số tin nhắn chưa đọc để hiển thị badge thông báo trên thanh điều hướng | User |
