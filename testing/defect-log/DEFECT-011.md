## DEFECT-011: `updatePricing`/`createCourse` gửi `"isActive": true` bị lưu thành `false` — Lombok boolean property naming mismatch với Jackson

- **Module:** Courses & Learning (Admin Course CRUD)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-COURSE-14 (system test UC-04, `POST /admin/courses`).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `POST /api/v1/admin/courses` trên `mchub_test`.

### Root Cause

`SaveCourseRequest.java`:
```java
private boolean isActive;
```
Field đặt tên `isActive` (có tiền tố `is`) — theo JavaBean spec, Lombok `@Data` sinh getter `isActive()` / setter `setActive(boolean)` (KHÔNG phải `setIsActive`), vì tiền tố `is` trên boolean field bị coi là một phần của tên getter, làm bean property name thực chất là `active`, không phải `isActive`.

Jackson (dùng cho `@RequestBody` deserialize) map JSON property theo bean property name → JSON key đúng phải là `"active"`, không phải `"isActive"`. Khi client gửi `"isActive": true` (tên trực quan, đúng theo field name nhìn thấy trong code — cách hầu hết dev/QA sẽ đoán), Jackson **không tìm thấy setter khớp**, âm thầm bỏ qua field này (không có lỗi validation nào được ném ra), giữ nguyên giá trị mặc định của primitive `boolean` là `false`.

Kết quả: `POST /admin/courses` với `"isActive": true` trong body → course được tạo thành công (HTTP 200) nhưng lưu vào DB với `isActive: false` — course **không hiển thị công khai** (`GET /courses` chỉ trả active courses) dù response API trả về `"active": false` (đã đúng tên field JSON output, không gây nhầm ở chiều đọc — chỉ sai ở chiều ghi).

### Impact nghiệp vụ

Admin tạo/sửa khoá học qua Swagger UI hoặc bất kỳ client nào gửi field tên `isActive` (tên hiển nhiên nhất để đoán, khớp tên field Java) sẽ **luôn tạo ra khoá học ẩn** mà không có bất kỳ cảnh báo/lỗi nào — course biến mất khỏi listing công khai, admin tưởng đã publish thành công. Đây là bug loại "silent data corruption qua API contract mơ hồ", không phải lỗi logic nghiệp vụ, nhưng hậu quả thực tế giống hệt (nội dung không lên được production).

### Evidence

```
$ curl -X POST ".../admin/courses" -d '{"isActive": true, ...}'
HTTP 200 {"data":{"active": false, ...}}   ← response field tên "active", đã bị set false

# DB xác nhận:
db.courses.findOne({slug:"uc04-test-course-wedding"}, {isActive:1})
{ isActive: false }

# Xác nhận nguyên nhân — đổi tên field JSON thành "active" thì set đúng:
$ curl -X PUT ".../admin/courses/{id}" -d '{"active": true, ...}'
HTTP 200 {"data":{"active": true, ...}}   ← đúng
```

Source: `SaveCourseRequest.java` field `private boolean isActive;` — Lombok sinh `isActive()`/`setActive(boolean)`, không sinh `setIsActive(boolean)`.

### Status

**Open.** Đề xuất dev (không phải QA quyết định): đổi tên field thành `active` (bỏ tiền tố `is`) để JSON contract nhất quán, hoặc thêm `@JsonProperty("isActive")` trên field để ép Jackson map đúng theo tên trực quan. Nên rà soát toàn bộ codebase tìm pattern tương tự (`boolean isXxx` field dùng Lombok `@Data`) — đã phát hiện **1 trường hợp liên quan khác cùng gốc** trong `CourseEnrollment.isCompleted` (xem DEFECT-012), khả năng còn nhiều chỗ khác chưa rà hết.
