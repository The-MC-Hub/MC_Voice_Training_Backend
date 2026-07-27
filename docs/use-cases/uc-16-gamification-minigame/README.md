# UC-16 — Minigame & Bookmark MC Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Trò chơi tương tác giọng nói Minigame, Bookmark MC yêu thích và Quản lý FlashDeal Admin.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-16.1-voice-minigame.md](UC-16.1-voice-minigame.md) | Thách đấu Voice Minigame ngày | `GET /api/v1/minigames/daily` | Guest / User |
| [UC-16.2-favorite-mcs.md](UC-16.2-favorite-mcs.md) | Bookmark MC yêu thích | `POST /api/v1/favorites/mcs/{mcId}` | User |
| [UC-16.3-admin-flashdeal-crud.md](UC-16.3-admin-flashdeal-crud.md) | Quản lý FlashDeal & Gói VIP | `POST /api/v1/admin/plans/flash-deals` | Admin |
