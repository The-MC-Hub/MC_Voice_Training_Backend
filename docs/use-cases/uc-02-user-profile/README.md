# UC-02 — Hồ Sơ Người Dùng & MC Profile Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Hồ sơ người dùng và MC Talent.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-02.1-get-profile.md](UC-02.1-get-profile.md) | Xem hồ sơ cá nhân | `GET /api/v1/users/me` | User |
| [UC-02.2-update-profile.md](UC-02.2-update-profile.md) | Cập nhật hồ sơ | `PUT /api/v1/users/profile` | User |
| [UC-02.3-streak-info.md](UC-02.3-streak-info.md) | Xem chuỗi Streak | `GET /api/v1/users/streak` | User |
| [UC-02.4-use-streak-freeze.md](UC-02.4-use-streak-freeze.md) | Đóng băng Streak | `POST /api/v1/users/streak/freeze` | User |
| [UC-02.5-public-mc-profile.md](UC-02.5-public-mc-profile.md) | Xem profile MC công khai | `GET /api/v1/mcs/{id}/public` | Public / Guest |
