## DEFECT-013: `GET /courses/reading-guides/{id}` trả HTTP 403 cho Guest — thiếu whitelist trong SecurityConfig (cùng nhóm nguyên nhân với DEFECT-001/DEFECT-010)

- **Module:** Courses & Learning (Reading Guide detail) / SecurityConfig
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-COURSE-05 (system test UC-04).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `GET /api/v1/courses/reading-guides/{id}` không kèm JWT trên `mchub_test`.

### Root Cause

`CourseController.getReadingGuide()` nằm trong section code comment `// ── Public ──` cùng với `getRoadmap()`, `getCourseTypes()`, `listCourses()`, `getCourse()` — rõ ràng chủ đích thiết kế là endpoint công khai (user cần đọc nội dung bài đọc lý thuyết kể cả trước khi đăng nhập/đăng ký khoá học, để xem trước nội dung).

`SecurityConfig.java` chỉ whitelist:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/courses").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/courses/{id}").permitAll()
```
Route `/api/v1/courses/reading-guides/{id}` có 2 segment sau `/courses/` (`reading-guides` + `{id}`) nên KHÔNG khớp pattern `/api/v1/courses/{id}` (chỉ khớp 1 segment). Do rule mặc định `.anyRequest().authenticated()`, request Guest bị chặn 403.

**Lưu ý phát hiện phụ:** `GET /courses/roadmap` và `GET /courses/types` (cũng nằm trong section Public, không có whitelist riêng) lại hoạt động bình thường (200) cho Guest — vì tình cờ khớp pattern `/api/v1/courses/{id}` (Spring coi `roadmap`/`types` như giá trị của `{id}`, method tương ứng may mắn không bị nhầm route nhờ Spring xử lý theo thứ tự khai báo `@GetMapping` cụ thể trước `@GetMapping("/{id}")`). Đây là sự trùng hợp về path-shape (1 segment), không phải whitelist đúng được thiết kế chủ đích — nhưng do 2 route đó hoạt động đúng nên không file defect riêng cho chúng.

### Impact nghiệp vụ

Guest (chưa đăng nhập) không thể xem trước nội dung bài đọc lý thuyết của khoá học — một phần quan trọng để thuyết phục Guest đăng ký/mua khoá học (preview content trước khi trả tiền). Ảnh hưởng trực tiếp conversion funnel, không phải lỗi bảo mật/mất dữ liệu.

### Evidence

```
$ curl -v "http://localhost:5555/api/v1/courses/reading-guides/6a5a4c65fe9893c6e03682d1"
< HTTP/1.1 403
< Content-Length: 0
```
Đối chiếu code: `CourseController.java` — method không có `@PreAuthorize`, comment section `// ── Public ──`; `SecurityConfig.java` — không có whitelist entry khớp path 2-segment này.

### Status

**Fixed (2026-07-18).** Thêm rule vào `SecurityConfig.java` (cùng commit với DEFECT-001/010/022):
```java
.requestMatchers(HttpMethod.GET, "/api/v1/courses/reading-guides/**").permitAll()
```
**Verify live (không JWT):** `GET /courses/reading-guides/{fake-id}` → 404 "Reading Guide not found" (lỗi nghiệp vụ hợp lệ, không còn 403). `CourseControllerTest` 9/9 PASS, không hồi quy.

Đã rà soát thêm 1 route cùng pattern trong phiên fix này (`/social-posts/*/click`, DEFECT-022) — tổng cộng 4 route Public bị thiếu whitelist đã tìm và fix trong 1 lần sửa `SecurityConfig.java`.
