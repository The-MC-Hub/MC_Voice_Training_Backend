# UC-03 — Luyện Giọng AI Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Luyện giọng và Chấm điểm AI.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-03.1-get-lessons.md](UC-03.1-get-lessons.md) | Danh sách & Chi tiết bài luyện | `GET /api/v1/voice/lessons` | Guest / User |
| [UC-03.2-analyze-practice.md](UC-03.2-analyze-practice.md) | Chấm điểm bài tập Luyện giọng AI | `POST /api/v1/voice/practice/analyze` | Client / MC |
| [UC-03.3-analyze-guest.md](UC-03.3-analyze-guest.md) | Chấm điểm dùng thử cho Khách | `POST /api/v1/voice/practice/analyze-guest` | Guest |
| [UC-03.4-practice-history.md](UC-03.4-practice-history.md) | Lịch sử luyện tập cá nhân | `GET /api/v1/voice/history` | User |
| [UC-03.5-generate-tts.md](UC-03.5-generate-tts.md) | Tạo âm thanh đọc mẫu TTS | `POST /api/v1/voice/tts` | User / MC |
