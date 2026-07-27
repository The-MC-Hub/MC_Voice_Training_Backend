# UC-12 Trò Chuyện & Nhắn Tin Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Trò chuyện và Nhắn tin trực tiếp Realtime.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-12.1-create-conversation.md](UC-12.1-create-conversation.md) | Tạo cuộc trò chuyện mới | `POST /api/v1/conversations` | User |
| [UC-12.2-get-conversations.md](UC-12.2-get-conversations.md) | Danh sách trò chuyện gần đây | `GET /api/v1/conversations` | User |
| [UC-12.3-message-history.md](UC-12.3-message-history.md) | Lịch sử tin nhắn | `GET /api/v1/conversations/{id}/messages` | User |
| [UC-12.4-send-ws-message.md](UC-12.4-send-ws-message.md) | Gửi tin nhắn STOMP WebSocket | `WS /ws-chat` | User |
| [UC-12.5-mark-as-read.md](UC-12.5-mark-as-read.md) | Đánh dấu đã đọc | `PUT /api/v1/conversations/{id}/read` | User |
| [UC-12.6-unread-count.md](UC-12.6-unread-count.md) | Số tin nhắn chưa đọc | `GET /api/v1/messages/unread-count` | User |
| [UC-12.7-mc-quick-replies.md](UC-12.7-mc-quick-replies.md) | Mẫu phản hồi nhanh cho MC | `GET/POST/DELETE /api/v1/mcs/quick-replies` | MC |
| [UC-12.8-collaborative-script-editor.md](UC-12.8-collaborative-script-editor.md) | Cùng chỉnh sửa kịch bản (Live) | `GET/POST /api/v1/scripts/booking/{bookingId}` | Client, MC |
