# UC-18 — Admin Moderation & Booking Contract Details Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Kiểm duyệt bài viết Admin và Chi tiết hợp đồng đặt MC.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-18.1-admin-social-post-crud.md](UC-18.1-admin-social-post-crud.md) | Bài viết truyền thông Admin CRUD | `POST /api/v1/admin/social-posts` | Admin |
| [UC-18.2-admin-community-moderation.md](UC-18.2-admin-community-moderation.md) | Kiểm duyệt bài đăng cộng đồng | `DELETE /api/v1/admin/community/posts/{id}` | Admin |
| [UC-18.3-booking-contract-details.md](UC-18.3-booking-contract-details.md) | Hợp đồng & Thanh toán cọc MC | `GET /api/v1/bookings/{id}/details` | Client / MC |
