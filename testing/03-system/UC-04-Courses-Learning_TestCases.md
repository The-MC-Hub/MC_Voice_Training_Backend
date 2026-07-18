# UC-04 — Courses & Learning Path — System Test Cases

**Ngày thực thi:** 2026-07-17
**Môi trường:** LIVE — `mchub_test` (MongoDB Atlas, database riêng), backend chạy port `5555`
**QA users:** `qa.client.uc04@mchubtest.local` (CLIENT, plan FREE), `qa.admin.uc04@mchubtest.local` (ADMIN, promoted qua mongosh)
**Fixtures tạo qua API thật:** 10 `VoiceLesson` (`POST /voice/admin/lessons`), 1 `Course` (`POST /admin/courses`)
**Fixtures seed trực tiếp DB (workaround DEFECT-011):** 3 `ReadingGuide` — không có admin API để tạo, xem DEFECT-011

---

## Danh sách Test Case

| ID | Mô tả | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TC-COURSE-01 | GET roadmap (Guest, public) | `GET /courses/roadmap` không JWT | 200, list milestone courses | 200, `data:[]` (chưa có milestone course) | PASS |
| TC-COURSE-02 | GET course types (Guest, public) | `GET /courses/types` | 200, list `CourseType` enum values | 200, `["WEDDING_MC","CORPORATE_EVENT","TALKSHOW_MC","SPEAKING"]` | PASS |
| TC-COURSE-03 | GET list courses (Guest, chưa có course active) | `GET /courses` | 200, `data:[]` | 200, `data:[]` | PASS |
| TC-COURSE-04 | Admin tạo 10 VoiceLesson làm fixture | `POST /voice/admin/lessons` x10 (multipart) | 200 mỗi lần, trả `id` | 200 x10, đủ 10 id | PASS |
| TC-COURSE-05 | GET reading guide by id (Guest, public — theo comment code) | `GET /courses/reading-guides/{id}` không JWT | 200, trả nội dung reading guide | **HTTP 403** | **FAIL — DEFECT-013** |
| TC-COURSE-06 | Admin tạo Course với `isActive:true` | `POST /admin/courses` body `{"isActive": true, ...}` | Course tạo active, hiển thị trong `GET /courses` | 200 nhưng `active:false` trong response + DB — course ẩn | **FAIL — DEFECT-011** |
| TC-COURSE-07 | Admin sửa Course dùng đúng field `active` (xác nhận root cause DEFECT-011) | `PUT /admin/courses/{id}` body `{"active": true, ...}` | Course active | 200, `active:true` — xác nhận đúng nguyên nhân | PASS (regression khẳng định root cause) |
| TC-COURSE-08 | GET courses list sau khi active | `GET /courses` | 200, 1 course | 200, 1 course | PASS |
| TC-COURSE-09 | GET course detail (Guest, không JWT) | `GET /courses/{id}` | 200, `hasAccess:null`, `purchased:null` (không xác định được vì không có userId) | 200, đúng như kỳ vọng | PASS |
| TC-COURSE-10 | GET course detail (CLIENT FREE, có JWT) | `GET /courses/{id}` kèm JWT | 200, `hasAccess:false`, `purchased:false` | 200, đúng | PASS |
| TC-COURSE-11 | GET courses filter theo type | `GET /courses?type=WEDDING_MC` | 200, chỉ course loại này | 200, đúng 1 course | PASS |
| TC-COURSE-12 | Enroll khi user FREE, chưa mua lẻ (negative — gating) | `POST /courses/{id}/enroll` (CLIENT FREE) | 403/400, `COURSE_REQUIRES_PLAN` | Lỗi đúng `ERR_7007` "Course requires BASIC plan or higher, or individual purchase" | PASS |
| TC-COURSE-13 | Gift-enroll (bypass plan check) | `POST /courses/{id}/gift-enroll` (CLIENT FREE) | 200, enrollment tạo, bỏ qua kiểm tra gói cước | 200, enrollment tạo thành công | PASS |
| TC-COURSE-14 | Xác nhận access sau gift-enroll | `GET /courses/{id}` sau gift | `hasAccess:true`, `purchased:true` | Đúng | PASS |
| TC-COURSE-15 | Hoàn thành 1 lesson | `POST /courses/{id}/lessons/{lessonId}/complete` | 200, `completedLessonIds` +1, `completionRate` cập nhật | 200, completionRate 7.1% → 14.3% đúng công thức (1/14 → 2/14) | PASS |
| TC-COURSE-16 | Hoàn thành toàn bộ 10 lesson + 3 reading | Lặp lại complete cho từng lesson/reading | 200 mỗi lần, `completionRate` tăng dần tới 92.9% (13/14) | Đúng, 92.9% sau khi xong 10 lesson + 3 reading, còn thiếu quiz | PASS |
| TC-COURSE-17 | Nộp quiz đúng 100% | `POST /courses/{id}/quiz/submit` `{"answers":[0,1,2,3,0,1,2,3]}` | `score:100`, `passed:true`, `certificateEarned:true`, cấp certificate | Đúng, score 100, cert issued | PASS |
| TC-COURSE-18 | GET my/certificates sau khi đạt | `GET /courses/my/certificates` | 200, 1 certificate với `completionScore:100` | Đúng | PASS |
| TC-COURSE-19 | Nộp quiz lần 2 (đã có cert) — không cấp cert trùng | `POST /courses/{id}/quiz/submit` lần 2, đáp án đúng | `certificateEarned:false`, `certificateId:null`, KHÔNG tạo cert thứ 2 | Đúng, vẫn chỉ 1 cert trong danh sách | PASS |
| TC-COURSE-20 | Nộp quiz sai số lượng câu trả lời (BVA/negative) | `POST /courses/{id}/quiz/submit` `{"answers":[...7 items]}` (thiếu 1) | 400, `QUIZ_ANSWER_MISMATCH` | Đúng, `ERR_7005` "Expected 8 answers, got 7" | PASS |
| TC-COURSE-21 | User khác (chưa enroll, FREE plan) cố hoàn thành lesson của course không thuộc về mình | `POST /courses/{id}/lessons/{lessonId}/complete` (ADMIN user, FREE plan, chưa enroll) | 403, `COURSE_REQUIRES_PLAN` | Đúng, chặn bởi `hasCourseAccess` trước khi chạm enrollment | PASS |
| TC-COURSE-22 | Admin cập nhật giá + giảm giá hợp lệ | `PATCH /admin/courses/{id}/pricing?priceVnd=99000&discountPercent=20` | 200, `finalPriceVnd` tính đúng = priceVnd × (1-discount%) | Đúng, `finalPriceVnd:79200` | PASS |
| TC-COURSE-23 | Admin pricing giá âm (BVA biên dưới, negative) | `PATCH .../pricing?priceVnd=-100` | 400, validation reject | Đúng, `ERR_9002` "priceVnd must be >= 0" | PASS |
| TC-COURSE-24 | Admin pricing discount > 100 (BVA biên trên, negative) | `PATCH .../pricing?discountPercent=101` | 400, validation reject | Đúng, `ERR_9002` "discountPercent must be 0-100" | PASS |
| TC-COURSE-25 | Non-admin (CLIENT) gọi pricing endpoint (negative — authorization) | `PATCH .../pricing` với CLIENT JWT | 403 | Đúng, 403 | PASS |
| TC-COURSE-26 | Tạo highlight trên reading guide | `POST /highlights` (CLIENT JWT) | 200, highlight gắn đúng `userId` | Đúng | PASS |
| TC-COURSE-27 | GET highlights theo reading guide | `GET /highlights/reading-guides/{id}` | 200, list đúng highlight của user | Đúng, 1 highlight | PASS |
| TC-COURSE-28 | Update highlight của chính mình | `PUT /highlights/{id}` (owner) | 200, `noteContent` cập nhật | Đúng | PASS |
| TC-COURSE-29 | IDOR — user khác sửa highlight không thuộc về mình | `PUT /highlights/{id}` (ADMIN JWT, không phải owner) | 403, `ACCESS_DENIED` | Đúng, `ERR_1003` "Access denied" | PASS |
| TC-COURSE-30 | IDOR — user khác xoá highlight không thuộc về mình | `DELETE /highlights/{id}` (ADMIN JWT, không phải owner) | 403, `ACCESS_DENIED` | Đúng | PASS |
| TC-COURSE-31 | Xoá highlight của chính mình | `DELETE /highlights/{id}` (owner) | 200 | Đúng | PASS |
| TC-COURSE-32 | Admin list all courses kèm enrollment stats | `GET /admin/courses` | 200, `totalEnrollments`, `totalCompletions` chính xác | `totalEnrollments:1` đúng, nhưng **`totalCompletions:0`** dù enrollment đã `isCompleted:true` trong DB | **FAIL — DEFECT-012** |
| TC-COURSE-33 | Tạo course trùng slug (negative — unique constraint) | `POST /admin/courses` slug đã tồn tại | 400, `COURSE_SLUG_EXISTS` | Đúng, `ERR_7002` "Slug already exists" | PASS |
| TC-COURSE-34 | Tạo course thiếu field bắt buộc (negative — validation) | `POST /admin/courses` `title:""`, thiếu `lessonIds`/`readingIds`/`quizQuestions` | 400, validation message | Đúng, `ERR_9002` "must not be blank; must not be blank; must not be null" | PASS |
| TC-COURSE-35 | Enroll course không tồn tại (negative) | `POST /courses/{fake-id}/enroll` | 404, `COURSE_NOT_FOUND` | Đúng, `ERR_7001` | PASS |
| TC-COURSE-36 | Guest (không JWT) cố enroll (negative — auth) | `POST /courses/{id}/enroll` không JWT | 403 | Đúng, 403 | PASS |
| TC-COURSE-37 | Admin xoá course không tồn tại (negative) | `DELETE /admin/courses/{fake-id}` | 404, `COURSE_NOT_FOUND` | Đúng | PASS |
| TC-COURSE-38 | Non-admin cố tạo course với body hợp lệ (authorization đúng thứ tự) | `POST /admin/courses` (CLIENT JWT, body đầy đủ hợp lệ) | 403 | Đúng, 403 — xác nhận `@PreAuthorize` chặn đúng khi body hợp lệ (ghi chú: nếu body thiếu field, lỗi 400 validation trả về TRƯỚC 403 — thứ tự filter chain, không phải lỗ hổng bảo mật vì không có hành động trái phép nào thành công, không file defect riêng) | PASS |
| TC-COURSE-39 | Admin xoá course (cleanup, xác nhận hard-delete) | `DELETE /admin/courses/{id}` | 200, course biến mất khỏi `GET /courses` | Đúng, 0 courses sau xoá | PASS |

---

## Ghi chú kỹ thuật quan trọng phát hiện trong quá trình test

1. **Không có Admin API để tạo `ReadingGuide`** (model + repository tồn tại, nhưng zero writer trong toàn bộ codebase). Đây là gap chức năng nghiêm trọng — feature #5 ("Xem bài đọc lý thuyết") và feature #13 ("Ghi chú/highlight bài đọc") trong đặc tả UC-04 không thể vận hành đầy đủ vì không ai (kể cả Admin) có cách tạo ReadingGuide qua API. QA phải seed trực tiếp qua mongosh để tiếp tục test các luồng phụ thuộc (course creation cần đúng 3 `readingIds`, highlight cần `readingGuideId` hợp lệ). Ghi nhận trong **DEFECT-011** (phần Status, khuyến nghị dev bổ sung `AdminReadingGuideController`).
2. `GET /courses/roadmap` và `GET /courses/types` hoạt động đúng cho Guest dù không có whitelist SecurityConfig riêng — chỉ vì tình cờ khớp pattern `/courses/{id}` (1-segment path). Đây là "may mắn kỹ thuật", không phải thiết kế chủ đích — nếu sau này đổi route matching, có thể vỡ bất cứ lúc nào. Không file defect (đang hoạt động đúng) nhưng ghi chú cho dev cân nhắc thêm whitelist tường minh để tránh phụ thuộc vào hành vi ngẫu nhiên.

---

## Tổng kết

**39/39 test case đã thực thi.**

| Kết quả | Số lượng |
|---|---|
| PASS | 36 |
| FAIL | 3 |
| Not Executed | 0 |

**3 defect mới phát hiện:** DEFECT-011 (Major/P1 — `isActive` field silently ignored), DEFECT-012 (Major/P1 — `totalCompletions` luôn = 0), DEFECT-013 (Major/P1 — reading-guide detail 403 cho Guest, cùng nhóm SecurityConfig whitelist với DEFECT-001/010).

**Điểm sáng:** Toàn bộ luồng nghiệp vụ cốt lõi (enroll gating, gift-enroll bypass, progress tracking, quiz scoring, certificate issuance/idempotency, admin pricing validation, highlight CRUD + IDOR protection, authorization) đều hoạt động đúng theo đặc tả — 3 defect phát hiện đều thuộc nhóm "silent contract mismatch" (Lombok/Jackson/Spring-Data naming) và "missing SecurityConfig whitelist", không phải lỗi business logic.
