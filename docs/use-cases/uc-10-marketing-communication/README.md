# UC-10 — Truyền Thông & Email Marketing Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Truyền thông, Email Marketing và Nhắc nhở Streak.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-10.1-create-campaign.md](UC-10.1-create-campaign.md) | Tạo chiến dịch Email Brevo | `POST /api/v1/admin/email/campaigns` | Admin |
| [UC-10.2-test-send-email.md](UC-10.2-test-send-email.md) | Gửi thử Email xem trước | `POST /api/v1/admin/email/test-send` | Admin |
| [UC-10.3-segmented-notification.md](UC-10.3-segmented-notification.md) | Gửi Push phân đoạn | `POST /api/v1/admin/notifications/segmented-send` | Admin |
| [UC-10.4-streak-reminder-scheduler.md](UC-10.4-streak-reminder-scheduler.md) | Nhắc nhở Streak tự động | `Scheduled Task (Daily at 20:00)` | System |
