# UC-04 — Đào Tạo & Khóa Học Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Đào tạo, Khóa học và Chứng chỉ.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-04.1-get-courses.md](UC-04.1-get-courses.md) | Danh sách & Chi tiết khóa học | `GET /api/v1/courses/{id}` | Guest / User |
| [UC-04.2-enroll-course.md](UC-04.2-enroll-course.md) | Đăng ký & Mua khóa học | `POST /api/v1/courses/{id}/enroll` | User |
| [UC-04.3-course-progress.md](UC-04.3-course-progress.md) | Theo dõi tiến độ học tập | `GET /api/v1/courses/{id}/progress` | User |
| [UC-04.4-complete-lesson.md](UC-04.4-complete-lesson.md) | Hoàn thành bài học video | `POST /api/v1/courses/{id}/lessons/{lessonId}/complete` | User |
| [UC-04.5-submit-quiz-cert.md](UC-04.5-submit-quiz-cert.md) | Nộp bài Quiz & Cấp chứng chỉ | `POST /api/v1/courses/{id}/quiz/submit` | User |
