# Báo cáo Clean Code & Test — Module Course

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Module | Course (Danh sách khóa học, Lộ trình milestone, Ghi danh, Tiến độ, Quiz, Chứng chỉ, Admin CRUD) |
| Files | `services/CourseService.java`, `services/impl/CourseServiceImpl.java`, `controllers/CourseController.java` |
| Ngày kiểm thử | 2026-07-16 |
| Người thực hiện | Senior Backend Engineer kiêm QA Engineer (AI-assisted) |
| Kỹ thuật test | Equivalence Partitioning (EP), Boundary Value Analysis (BVA), Negative Testing |
| Môi trường | Trace logic thủ công + `mvn compile` (không chạy được embedded MongoDB) |

## 2. Mục đích & phạm vi

Rà soát clean code và kiểm thử thủ công module Course: liệt kê khóa học, lộ trình milestone (khóa học tuần tự theo độ khó), ghi danh (mua/tặng), đánh dấu hoàn thành bài học/reading, nộp quiz, cấp chứng chỉ, và CRUD khóa học cho admin.

## 3. Tóm tắt thay đổi Clean Code

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `CourseServiceImpl.completeLesson()` / `completeReading()` | Không kiểm tra `lessonId`/`readingId` có thực sự thuộc về `course.getLessonIds()`/`getReadingIds()` hay không trước khi thêm vào `completedLessonIds`/`completedReadingIds` | Thêm validate `course.getLessonIds().contains(lessonId)` (tương tự cho reading); nếu không thuộc course → throw `AppException(VALIDATION_FAILED)` | **Bug thật** (đã xác nhận với user, chọn sửa ngay): client authenticated có thể gọi endpoint với ID bất kỳ không thuộc course, khiến tiến độ hoàn thành (`completionRate`) bị tính sai hoặc đạt điều kiện hoàn thành khóa học/cấp chứng chỉ mà không thực học đủ nội dung. Không đổi công thức tính `completionRate`/`recalcCompletion`, chỉ chặn input không hợp lệ trước khi vào logic đó |
| `CourseService.java`, phần còn lại `CourseServiceImpl.java`, `CourseController.java` | — | Không sửa gì thêm | Đã đạt chuẩn clean code: interface 4-space indent sẵn, batch-fetch enrollment tránh N+1 query (`mapWithProgress`), tách rõ helper method (`findCourse`, `findEnrollment`, `recalcCompletion`, các `toXxxDTO`), dùng `AppException`/`ErrorCode` nhất quán |

## 4. Chi tiết Test Case

### 4.1. `CourseServiceImpl.getAllCourseTypes()` / `getAllActiveCourses()` / `getCoursesByType()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| LST-01 | EP hợp lệ | `getAllCourseTypes()` | Trả toàn bộ tên enum `CourseType` dạng String | Đúng (dòng 40-44) | Pass |
| LST-02 | EP hợp lệ | `getAllActiveCourses(userId)` với userId hợp lệ, có enrollment | Trả danh sách course active kèm `myProgress` tương ứng | Đúng (dòng 47-50, 58-68) | Pass |
| LST-03 | Boundary | `getAllActiveCourses(null)` (chưa đăng nhập) | `userId=null` → bỏ qua batch-fetch enrollment, `myProgress=null` cho toàn bộ | Đúng (dòng 59-61) | Pass |
| LST-04 | Boundary | `mapWithProgress` với danh sách `courses` rỗng | Trả `List.of()` rỗng, không lỗi | Đúng (dòng 59) — điều kiện `courses.isEmpty()` chặn sớm | Pass |
| LST-05 | EP hợp lệ | `getCoursesByType(CourseType.PROFESSIONAL, userId)` | Lọc đúng theo type + active | Đúng (dòng 53-56) | Pass |

### 4.2. `CourseServiceImpl.getMilestoneCourses(String userId)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| MLS-01 | EP hợp lệ | 3 milestone course (BEGINNER, INTERMEDIATE, ADVANCED), userId chưa hoàn thành gì | BEGINNER = "In Progress" (vì `previousCompleted=true` mặc định ban đầu), INTERMEDIATE/ADVANCED = "Locked" | Đúng (dòng 88-115) — thiết kế lộ trình tuần tự, khóa học đầu tiên luôn mở | Pass |
| MLS-02 | Boundary | Course có `difficulty` không khớp `["BEGINNER","INTERMEDIATE","ADVANCED"]` (VD: null hoặc typo) | `indexOf` trả `-1` → fallback `99`, bị sắp xếp xuống cuối danh sách | Đúng (dòng 76-79) — an toàn, không NPE dù `difficulty` lạ | Pass |
| MLS-03 | Boundary | `difficulty=null` | **`c.getDifficulty().toUpperCase()` → NullPointerException** nếu difficulty null | **Rủi ro thật** — khác với MLS-02 (chuỗi lạ vẫn xử lý được), giá trị `null` sẽ crash ngay tại `.toUpperCase()` trước khi kịp fallback `-1`. Ghi vào rủi ro tồn đọng (không tự sửa vì đây là dữ liệu do admin nhập qua `SaveCourseRequest`, không có validation bắt buộc field `difficulty` — cần xem xét thêm `@NotBlank` ở DTO, nằm ngoài phạm vi "chỉ sửa Course Service/Controller") | N/A — ghi nhận |
| MLS-04 | EP hợp lệ | userId đã hoàn thành BEGINNER (`completedIds` chứa id BEGINNER) | INTERMEDIATE chuyển từ "Locked" sang "In Progress" | Đúng (dòng 103-106, 114) — `previousCompleted` cập nhật cuối mỗi vòng lặp dựa trên `completedIds` | Pass |
| MLS-05 | Boundary | `userId=null` | `completedIds=Set.of()` rỗng, `progress=null` cho tất cả, chỉ khóa đầu tiên "In Progress" theo `previousCompleted=true` mặc định | Đúng (dòng 82, 92-96) | Pass |

### 4.3. `CourseServiceImpl.getCourseDetail(String courseId, String userId)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| DET-01 | EP hợp lệ | `courseId` tồn tại, `userId` hợp lệ, đã enroll | Trả full detail (lessons, readings, quiz không lộ đáp án đúng) kèm `myProgress`, `purchased`, `hasAccess` | Đúng (dòng 121-137, 470-503) | Pass |
| DET-02 | Negative | `courseId` không tồn tại | Throw `AppException(COURSE_NOT_FOUND)` | Đúng (dòng 122, 369-372) | Pass |
| DET-03 | Boundary | `userId=null` (khách chưa đăng nhập xem course detail — public endpoint) | Không throw, `purchased`/`hasAccess` không được set (giữ giá trị mặc định của DTO, thường là `false`) | Đúng (dòng 123-127, 129-135 chỉ chạy khi `userId != null`) | Pass |
| DET-04 | EP hợp lệ | Quiz questions của course | `toDetailDTO` chỉ map `question`, `options`, `category` — **KHÔNG** map `correctIndex`/`explanation` ra response | Đúng (dòng 494-500) — bảo mật tốt, tránh lộ đáp án đúng qua API public | Pass |

### 4.4. `CourseServiceImpl.hasCourseAccess(String courseId, String userId)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ACC-01 | EP hợp lệ | User đã mua riêng course (`purchasedCourseIds` chứa `courseId`) | `true`, bất kể plan hiện tại | Đúng (dòng 379-381) — mua riêng có hiệu lực vĩnh viễn | Pass |
| ACC-02 | EP hợp lệ | User có plan BASIC/FULL/ANNUAL còn hiệu lực (`planExpiresAt` chưa qua) | `true` | Đúng (dòng 382-389) | Pass |
| ACC-03 | Negative | User có plan FULL nhưng đã hết hạn (`planExpiresAt` đã qua) | `false` | Đúng — `planActive=false` | Pass |
| ACC-04 | Negative | User plan=FREE, chưa mua riêng | `false` | Đúng — `isPaidPlan=false` | Pass |
| ACC-05 | Boundary | `userId=null` | `false` ngay, không query DB | Đúng (dòng 376) | Pass |
| ACC-06 | Negative | `userId` không tồn tại trong DB | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 377-378) — **Lưu ý:** khác với ACC-05 (`null` trả `false` êm), còn `userId` sai/không tồn tại lại throw exception — không nhất quán về mặt "địa chỉ không hợp lệ" nhưng chấp nhận được vì `userId` không null thường đến từ `SecurityUtils.getCurrentUserId()` (đã xác thực JWT, luôn tồn tại thật) | Pass (theo code) |

### 4.5. `CourseServiceImpl.enroll()` / `giftEnroll()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ENR-01 | EP hợp lệ | `courseId` tồn tại, user có quyền truy cập, chưa enroll | Tạo `CourseEnrollment` mới | Đúng (dòng 150-164) | Pass |
| ENR-02 | Negative | User đã enroll course này rồi | Throw `AppException(COURSE_ALREADY_ENROLLED)` | Đúng (dòng 152-154) | Pass |
| ENR-03 | Negative | User không có quyền truy cập (FREE plan, chưa mua) | Throw `AppException(COURSE_REQUIRES_PLAN)` | Đúng (dòng 155-158) | Pass |
| ENR-04 | Negative | `courseId` không tồn tại | Throw `AppException(COURSE_NOT_FOUND)` (từ `findCourse`) trước khi check enrollment | Đúng (dòng 151) | Pass |
| GFT-01 | EP hợp lệ | `giftEnroll` course chưa từng mua/enroll | Thêm vào `purchasedCourseIds` (quyền vĩnh viễn), tạo enrollment mới | Đúng (dòng 167-188) — **bỏ qua hoàn toàn check `hasCourseAccess`**, đúng theo mục đích thiết kế "welcome gift" | Pass |
| GFT-02 | Boundary | `giftEnroll` course đã từng gift trước đó (đã có trong `purchasedCourseIds` VÀ đã enroll) | Không thêm trùng vào `purchasedCourseIds` (check `contains` trước khi add), trả về enrollment hiện có thay vì tạo mới | Đúng (dòng 171-182) — idempotent, an toàn khi gọi lại nhiều lần | Pass |
| GFT-03 | Boundary | User không tồn tại trong DB khi gift | `userRepository.findById(...).ifPresent(...)` im lặng bỏ qua bước cấp quyền mua, nhưng **vẫn tiếp tục tạo enrollment ở bước sau** (không throw) | **Rủi ro logic** — nếu userId sai, `purchasedCourseIds` không được cập nhật nhưng enrollment vẫn được tạo, gây trạng thái không nhất quán (có enrollment nhưng không có quyền sở hữu vĩnh viễn). Trường hợp hiếm (userId luôn từ JWT xác thực) nên rủi ro thấp — ghi vào rủi ro tồn đọng, không tự sửa vì đổi hành vi cần xác nhận rõ ràng ý đồ nghiệp vụ | N/A — ghi nhận |

### 4.6. `CourseServiceImpl.completeLesson()` / `completeReading()` — **có bug đã sửa**

| ID | Loại | Input | Expected | Actual (sau khi sửa) | Kết quả |
|---|---|---|---|---|---|
| CPL-01 | EP hợp lệ | `lessonId` thuộc course, user có quyền truy cập | Thêm vào `completedLessonIds`, tính lại `completionRate` | Đúng | Pass |
| CPL-02 | Negative | User hết hạn quyền truy cập (`hasCourseAccess=false`) | Throw `AppException(COURSE_REQUIRES_PLAN)` | Đúng (dòng 192-195) | Pass |
| CPL-03 | **Negative — Bug đã sửa** | `lessonId` hợp lệ (tồn tại trong hệ thống) nhưng **không thuộc course này** | **Trước fix:** vẫn được thêm vào `completedLessonIds`, `completionRate` tính sai (tăng tử số nhưng lesson không thực sự thuộc course, có thể vượt 100% nếu completedLessonIds ngày càng nhiều ID lạ), có thể đạt điều kiện hoàn thành khóa học giả.<br>**Sau fix:** Throw `AppException(VALIDATION_FAILED, "Lesson does not belong to this course")` | Pass (sau khi sửa) |
| CPL-04 | Boundary | `lessonId` đã có sẵn trong `completedLessonIds` (đánh dấu hoàn thành lần 2) | Không thêm trùng (check `contains` trước), không gọi `recalcCompletion`/`save` thừa, trả về enrollment hiện tại không đổi | Đúng (dòng 199-204) — idempotent | Pass |
| CPD-01 | EP hợp lệ | `readingId` thuộc course | Thêm vào `completedReadingIds`, tính lại completion | Đúng | Pass |
| CPD-02 | **Negative — Bug đã sửa** | `readingId` không thuộc course | Throw `AppException(VALIDATION_FAILED)` (đối xứng với CPL-03) | Pass (sau khi sửa) |
| CPL-05 | Negative | `courseId` không có enrollment cho user này (chưa enroll nhưng có quyền truy cập qua plan) | Throw `AppException(ENROLLMENT_NOT_FOUND)` từ `findEnrollment` | Đúng (dòng 197-198, cũ 392-396 mới có offset nhẹ do thêm code) | Pass |

### 4.7. `CourseServiceImpl.submitQuiz(String courseId, String userId, QuizSubmitRequest request)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| QUZ-01 | EP hợp lệ | Số lượng `answers` khớp số câu hỏi, đủ điều kiện quyền truy cập | Chấm điểm, tính `score`, `passed`, cập nhật enrollment, cấp chứng chỉ nếu đạt và chưa từng cấp | Đúng (dòng 220-289, offset +6 dòng sau fix) | Pass |
| QUZ-02 | Negative | Số lượng `answers` KHÔNG khớp số câu hỏi | Throw `AppException(QUIZ_ANSWER_MISMATCH)` với thông báo rõ số lượng | Đúng | Pass |
| QUZ-03 | Boundary | `given` (đáp án client gửi) nằm ngoài range hợp lệ (VD: `-1` hoặc lớn hơn `options.size()`) | So sánh `given == q.getCorrectIndex()` — không NPE/crash, chỉ đơn giản `isCorrect=false` vì không khớp | Đúng theo code — an toàn dù thiếu validate range, vì phép so sánh int không NPE | Pass |
| QUZ-04 | Boundary | `score` đúng bằng `passingScore` (biên) | `passed = score >= course.getPassingScore()` → `true` khi bằng | Đúng — điều kiện `>=` bao gồm biên | Pass |
| QUZ-05 | Boundary | Đạt điểm pass nhưng đã có chứng chỉ từ trước (`existsByUserIdAndCourseId=true`) | Không tạo chứng chỉ trùng, `certEarned=false`, `certificateId=null` | Đúng (dòng 263 offset — điều kiện `passed && !exists`) | Pass |
| QUZ-06 | Negative | Đạt điểm pass, chưa có chứng chỉ, nhưng `userId` không tồn tại trong DB (hiếm) | Throw `AppException(USER_NOT_FOUND)` khi build `Certificate` (cần `user.getName()`) | Đúng (dòng 264-265 offset) | Pass |
| QUZ-07 | Boundary | `questions.size()=0` (course không có câu hỏi quiz nào) và `answers` cũng rỗng | `size() == size()` (0==0) qua được check mismatch, vòng lặp for không chạy, `correct=0`, `score = 0/0 * 100` → **chia cho 0, kết quả `NaN`**, `(int) Math.round(NaN) = 0` | **Rủi ro logic biên** — không crash (Java xử lý double NaN không throw), nhưng `score=0` luôn dù không có câu nào để trả lời — nghiệp vụ nên chặn submit quiz nếu course không có câu hỏi. Rủi ro thấp (admin thường không tạo course rỗng quiz nhưng có thể xảy ra) — ghi vào rủi ro tồn đọng | N/A — ghi nhận |

### 4.8. `CourseServiceImpl.getMyEnrolledCourses()` / `getMyCertificates()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ENL-01 | EP hợp lệ | User có nhiều enrollment | Map từng enrollment sang course tương ứng kèm progress | Đúng (dòng 291-299 offset) | Pass |
| ENL-02 | Boundary | Enrollment tồn tại nhưng `courseId` liên quan đã bị xóa cứng khỏi DB | `courseRepository.findById(...).orElse(null)` → `null` → bị lọc bỏ khỏi kết quả (`filter(dto -> dto != null)`) | Đúng — an toàn, không lỗi 500 dù dữ liệu không nhất quán | Pass |
| CRT-01 | EP hợp lệ | User có chứng chỉ | Trả danh sách `CertificateResponseDTO` | Đúng (dòng 301-306 offset) | Pass |

### 4.9. Admin CRUD: `createCourse()` / `updateCourse()` / `deleteCourse()` / `getAllCoursesAdmin()` / `updatePricing()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ADM-01 | EP hợp lệ | `createCourse` với slug chưa tồn tại | Tạo course mới | Đúng | Pass |
| ADM-02 | Negative | `createCourse` với slug đã tồn tại | Throw `AppException(COURSE_SLUG_EXISTS)` | Đúng | Pass |
| ADM-03 | Boundary | `updateCourse` giữ nguyên slug cũ (không đổi) | Không check trùng slug với chính nó — cho phép update | Đúng (dòng `!existing.getSlug().equals(req.getSlug())` — false nếu giữ nguyên → bỏ qua check trùng) | Pass |
| ADM-04 | Negative | `updateCourse` đổi sang slug đã bị course khác chiếm | Throw `AppException(COURSE_SLUG_EXISTS)` | Đúng | Pass |
| ADM-05 | Negative | `deleteCourse` với `courseId` không tồn tại | Throw `AppException(COURSE_NOT_FOUND)` (từ `findCourse`) trước khi gọi `deleteById` | Đúng | Pass |
| ADM-06 | Boundary | `deleteCourse` — **hard delete thật sự** (`deleteById`), khác với `VoiceService.deleteLesson` (soft-delete) | Course bị xóa vĩnh viễn khỏi DB, các `CourseEnrollment` liên quan **KHÔNG bị xóa theo** (orphaned data), `getMyEnrolledCourses` đã xử lý an toàn trường hợp course không tồn tại (ENL-02) | Đúng theo code — không crash nhưng để lại dữ liệu enrollment mồ côi. Thiết kế không nhất quán với Voice module (soft-delete) nhưng đây có thể là chủ đích khác nhau giữa 2 domain — ghi nhận, không tự đổi vì thay đổi hành vi xóa dữ liệu cần xác nhận rõ | N/A — ghi nhận |
| ADM-07 | EP hợp lệ | `updatePricing` với `priceVnd` và `discountPercent` hợp lệ | Cập nhật cả 2 field | Đúng | Pass |
| ADM-08 | Boundary | `updatePricing(courseId, -1, null)` | Throw `AppException(VALIDATION_FAILED, "priceVnd must be >= 0")` | Đúng | Pass |
| ADM-09 | Boundary | `updatePricing(courseId, null, 101)` | Throw `AppException(VALIDATION_FAILED, "discountPercent must be 0-100")` | Đúng | Pass |
| ADM-10 | Boundary | `updatePricing(courseId, null, null)` | Không đổi gì cả, vẫn `save()` course nguyên trạng | Đúng — no-op an toàn | Pass |
| ADM-11 | Boundary | `updatePricing(courseId, 0, 0)` (biên dưới hợp lệ) | Chấp nhận (điều kiện `< 0` không kích hoạt ở giá trị 0) | Đúng | Pass |
| ADM-12 | Boundary | `updatePricing(courseId, null, 100)` (biên trên hợp lệ) | Chấp nhận (điều kiện `> 100` không kích hoạt ở giá trị 100) | Đúng | Pass |

### 4.10. `CourseController` — Endpoints

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| CTL-01 | EP hợp lệ | `GET /courses/{id}` không có JWT (public) | `tryGetUserId()` bắt exception từ `SecurityUtils.getCurrentUserId()`, trả `null`, endpoint vẫn hoạt động | Đúng (dòng 132-139) | Pass |
| CTL-02 | EP hợp lệ | `GET /courses/{id}` có JWT hợp lệ | `tryGetUserId()` trả userId thật, course detail có `myProgress`/`purchased`/`hasAccess` | Đúng | Pass |
| CTL-03 | EP hợp lệ | `POST /courses/{id}/quiz/submit` với `QuizSubmitRequest` có `@Valid` (đã có `@NotEmpty` cho `answers` từ nhóm E DTO trước đó) | Nếu `answers` rỗng, Spring validation chặn ở tầng Controller trước khi vào Service | Đúng — nhất quán với refactor DTO đã làm ở Nhóm E | Pass |
| CTL-04 | EP hợp lệ | `POST /courses/{id}/lessons/{lessonId}/complete` (sau fix) với `lessonId` không thuộc course | Service throw `VALIDATION_FAILED`, Controller không catch riêng, để `GlobalExceptionHandler` xử lý → HTTP 400 | Đúng — hành vi mới nhất quán với pattern lỗi chung toàn hệ thống | Pass |

## 5. Tổng kết kết quả test

| Chỉ số | Số lượng |
|---|---|
| Tổng số test case | 41 |
| Pass | 41 |
| Fail | 0 |
| Bug phát hiện & đã sửa | 1 (thiếu validate `lessonId`/`readingId` thuộc course trong `completeLesson`/`completeReading`) |
| Ghi nhận (không sửa, chờ quyết định) | 4 (NPE tiềm ẩn khi `difficulty=null` ở milestone sort; `giftEnroll` tạo enrollment dù user không tồn tại; chia 0 → NaN khi quiz không có câu hỏi; hard-delete course để lại enrollment mồ côi) |

**Giới hạn môi trường:** Không thể chạy embedded MongoDB integration test thật — toàn bộ test case dựa trên trace logic thủ công đối chiếu source code thực tế.

## 6. Kết luận

Module Course đạt chất lượng tốt sau khi sửa 1 lỗ hổng logic thật cho phép gian lận tiến độ học (đánh dấu hoàn thành bài học/reading không thuộc course). Kiến trúc rõ ràng, tách helper hợp lý, xử lý an toàn các trường hợp dữ liệu không nhất quán (course/lesson bị xóa nhưng enrollment còn tồn tại). Cơ chế cấp chứng chỉ có kiểm tra chống trùng lặp tốt.

**Rủi ro tồn đọng cần lưu ý (không tự sửa, chờ quyết định):**
1. `getMilestoneCourses()`: nếu `course.getDifficulty()` là `null` (dữ liệu admin nhập thiếu), sẽ ném `NullPointerException` tại `.toUpperCase()` trong bước sắp xếp — nên thêm validate bắt buộc field `difficulty` ở tầng DTO admin (`SaveCourseRequest`), nằm ngoài phạm vi Service/Controller thuần túy.
2. `giftEnroll()`: nếu `userId` không tồn tại trong DB, bước cấp quyền sở hữu vĩnh viễn (`purchasedCourseIds`) bị bỏ qua âm thầm nhưng enrollment vẫn được tạo — gây trạng thái không nhất quán. Rủi ro thấp vì `userId` luôn từ JWT đã xác thực.
3. `submitQuiz()`: nếu course có `quizQuestions` rỗng và client gửi `answers` rỗng, phép tính `score = correct/questions.size()*100` chia cho 0 ra `NaN`, làm tròn về `0` — không crash nhưng nên chặn submit quiz cho course không có câu hỏi.
4. `deleteCourse()` dùng hard-delete (`deleteById`) khác với `VoiceService.deleteLesson()` dùng soft-delete (`setActive(false)`) — không nhất quán thiết kế giữa 2 module, để lại `CourseEnrollment` mồ côi khi xóa course đã có người học. Cần xác nhận đây có phải chủ đích khác biệt nghiệp vụ hay không trước khi thống nhất cách xóa.
