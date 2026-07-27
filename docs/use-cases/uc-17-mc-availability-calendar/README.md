# UC-17 — Lịch Availability, Chứng Chỉ & Notification Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Lịch nhận show MC, Xác minh chứng chỉ và Thông báo đẩy cá nhân.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-17.1-mc-availability.md](UC-17.1-mc-availability.md) | Lịch bận / rảnh nhận show MC | `POST /api/v1/mcs/availability` | MC |
| [UC-17.2-certificate-verification.md](UC-17.2-certificate-verification.md) | Xác minh chứng chỉ qua mã | `GET /api/v1/certificates/verify/{certNumber}` | Guest / Public |
| [UC-17.3-user-notifications.md](UC-17.3-user-notifications.md) | Thông báo đẩy cá nhân | `GET /api/v1/notifications` | User |
