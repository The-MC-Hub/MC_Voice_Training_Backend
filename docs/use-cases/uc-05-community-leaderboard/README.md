# UC-05 — Cộng Đồng & Bảng Xếp Hạng Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Cộng đồng, Bảng xếp hạng thi đua và Giải đấu Voice Arena.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-05.1-community-stats.md](UC-05.1-community-stats.md) | Thống kê cộng đồng | `GET /api/v1/community/stats` | Guest / User |
| [UC-05.2-leaderboard-ranking.md](UC-05.2-leaderboard-ranking.md) | Bảng xếp hạng Leaderboard | `GET /api/v1/community/leaderboard` | User |
| [UC-05.3-my-rank.md](UC-05.3-my-rank.md) | Tra cứu thứ hạng cá nhân | `GET /api/v1/community/leaderboard/me` | User |
| [UC-05.4-active-arenas.md](UC-05.4-active-arenas.md) | Đấu trường giọng nói | `GET /api/v1/community/active-arenas` | User |
| [UC-05.5-social-posts.md](UC-05.5-social-posts.md) | Bài đăng & Click tracking | `POST /api/v1/social-posts/{id}/click` | Guest / User |
| [UC-05.6-admin-competition-crud.md](UC-05.6-admin-competition-crud.md) | Quản lý giải đấu Voice Arena Admin | `POST /api/v1/admin/competitions` | Admin |
