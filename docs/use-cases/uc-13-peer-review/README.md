# UC-13 — Đánh Giá Đồng Nghiệp Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Đánh giá đồng nghiệp và Chấm điểm chéo bài luyện giọng.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-13.1-request-review.md](UC-13.1-request-review.md) | Gửi yêu cầu nhận xét | `POST /api/v1/peer-reviews/request` | MC |
| [UC-13.2-get-pending-reviews.md](UC-13.2-get-pending-reviews.md) | Danh sách bài chờ review | `GET /api/v1/peer-reviews/pending` | MC |
| [UC-13.3-submit-review.md](UC-13.3-submit-review.md) | Chấm điểm & Gửi feedback | `POST /api/v1/peer-reviews/{id}/review` | MC |
| [UC-13.4-get-my-reviews.md](UC-13.4-get-my-reviews.md) | Xem phản hồi đã nhận | `GET /api/v1/peer-reviews/my-reviews` | MC |
| [UC-13.5-admin-delete-review.md](UC-13.5-admin-delete-review.md) | Admin gỡ review vi phạm | `DELETE /api/v1/admin/peer-reviews/{id}` | Admin |
