## DEFECT-012: `AdminCourseController` / `getAllCoursesAdmin()` — `totalCompletions` luôn trả về 0 do Spring Data derived query `countByCourseIdAndIsCompletedTrue` không khớp property name

- **Module:** Courses & Learning (Admin Course stats) / Admin Dashboard
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-COURSE-19 (system test UC-04, `GET /admin/courses`), sau khi đã có 1 enrollment thật hoàn thành 100% (đủ điều kiện `isCompleted=true` xác nhận trực tiếp trong DB).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `GET /api/v1/admin/courses` trên `mchub_test`, sau khi 1 user thật hoàn thành đầy đủ course (10 lessons + 3 readings + quiz 100%, `certificateEarned=true`).

### Root Cause

Cùng gốc nguyên nhân với DEFECT-011 (Lombok boolean property naming), nhưng ở phía Spring Data MongoDB derived query thay vì Jackson.

`CourseEnrollment.java`:
```java
private boolean isCompleted = false;
```
Lombok sinh bean property name là `completed` (không phải `isCompleted`) — xác nhận qua `recalcCompletion()` trong `CourseServiceImpl.java` dùng đúng `e.setCompleted(true)` / `e.isCompleted()`, KHÔNG dùng `setIsCompleted`.

`CourseEnrollmentRepository.java`:
```java
long countByCourseIdAndIsCompletedTrue(String courseId);
```
Spring Data derived query parser suy ra tên property từ method name là `IsCompleted` → tìm property Java bean tên `isCompleted`. Nhưng property thực tế (theo Lombok) là `completed`. Do MongoDB không có compile-time property validation cho derived query (khác JPA), Spring Data **âm thầm không lỗi khi start** (không có property `isCompleted` nào để so khớp field MongoDB `_class`/index), nhưng khi build query MongoDB thực tế lại đúng gọi field `isCompleted` (đây là tên field trong document MongoDB, do Spring Data MongoDB mapping field theo tên Java field gốc `isCompleted`, không theo bean property `completed`) — nghĩa là ở tầng lưu trữ field tên đúng là `isCompleted` (xác nhận qua mongosh: `db.course_enrollments.findOne()` trả về field `isCompleted: true`).

**Kết luận thực nghiệm:** dù field MongoDB tên đúng `isCompleted`, method `countByCourseIdAndIsCompletedTrue` vẫn trả về 0 sai — nghĩa là lỗi nằm ở bước Spring Data parse `IsCompletedTrue` thành keyword `True` cắt nhầm vị trí (parse thành property `isCompletedTrue` không tồn tại, hoặc property `isCompleted` + keyword nhưng thất bại khi so khớp kiểu). Cần dev xác nhận thêm bằng cách bật debug query log (`logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG`) để thấy chính xác câu query Mongo được sinh ra — QA không có quyền sửa/thêm log debug vào code theo `testing/testing.md`.

### Impact nghiệp vụ

- **Admin Dashboard / Admin Course List luôn hiển thị "0 hoàn thành"** cho MỌI khoá học, bất kể có bao nhiêu user thực sự đã hoàn thành — admin không thể đánh giá được tỷ lệ hoàn thành khoá học (completion rate), một chỉ số kinh doanh quan trọng để quyết định giữ/xoá/cải thiện nội dung khoá học.
- Không ảnh hưởng trải nghiệm user cuối (user vẫn nhận chứng chỉ đúng, `completed=true` vẫn lưu đúng trong enrollment của chính họ) — chỉ ảnh hưởng số liệu thống kê phía Admin.

### Evidence

```
# Enrollment DB xác nhận đã hoàn thành thật:
db.course_enrollments.findOne({courseId:"..."}, {isCompleted:1, quizScore:1})
{ isCompleted: true, quizScore: 100 }

# Nhưng admin list vẫn báo 0:
$ curl "http://localhost:5555/api/v1/admin/courses" -H "Authorization: Bearer <admin-jwt>"
HTTP 200 {"data":[{..., "totalEnrollments": 1, "totalCompletions": 0}]}
```

Source: `CourseEnrollmentRepository.java` dòng 17 — `countByCourseIdAndIsCompletedTrue(String courseId)`; `CourseEnrollment.java` — field `private boolean isCompleted`.

### Status

**Open.** Đề xuất dev (không phải QA quyết định): đổi tên method thành `countByCourseIdAndCompletedTrue` (khớp bean property name `completed` sau khi Lombok xử lý) — tương tự các method khác trong cùng file có thể đang dùng đúng convention (`existsByUserIdAndCourseId` không bị ảnh hưởng vì không liên quan tới boolean `is` field). Cần rà soát toàn bộ các derived query khác trong project có pattern `...IsXxxTrue`/`...IsXxxFalse` để tìm case tương tự (cùng gốc với DEFECT-011).
