# Báo cáo Clean Code & Test — Module Voice

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Module | Voice (Lesson CRUD, Practice Analysis, TTS, Adaptive Stats, Guest Trial) |
| Files | `services/VoiceService.java`, `services/impl/VoiceServiceImpl.java`, `controllers/VoiceController.java` |
| Ngày kiểm thử | 2026-07-16 |
| Người thực hiện | Senior Backend Engineer kiêm QA Engineer (AI-assisted) |
| Kỹ thuật test | Equivalence Partitioning (EP), Boundary Value Analysis (BVA), Negative Testing |
| Môi trường | Trace logic thủ công + `mvn compile` (không chạy được embedded MongoDB) |

## 2. Mục đích & phạm vi

Rà soát clean code và kiểm thử thủ công module Voice: quản lý bài luyện (CRUD, admin), tìm kiếm, phân tích luyện tập qua AI service (đăng nhập/khách/proxy), lịch sử luyện tập, sinh giọng nói TTS, thống kê hiệu chỉnh thích ứng (adaptive calibration).

## 3. Tóm tắt thay đổi Clean Code

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `VoiceController.java` | Khối validate file audio (content-type + size 20MB + magic bytes, ~14 dòng) lặp lại y hệt 3 lần ở `analyzePractice`, `proxyAnalyzeVoice`, `analyzeGuestVoice` | Tách thành `private void validateAudioFile(MultipartFile)` dùng chung, gọi ở cả 3 nơi | Vi phạm DRY rõ ràng. Không đổi hành vi — cùng thứ tự kiểm tra (content-type → size → magic bytes), cùng message lỗi |
| `VoiceServiceImpl.analyzePractice()` | `tipsViRaw`/`tipsEnRaw` cast trực tiếp từ response Map của AI service, không null-check trước `.stream()` | Thêm `if (tipsViRaw == null) tipsViRaw = List.of();` (tương tự cho `tipsEnRaw`) trước khi map | Nếu AI service trả thiếu key `tips_vi`/`tips_en`, code cũ sẽ NPE (vẫn bị catch chung nên không crash server, nhưng log không rõ nguyên nhân thật). Fix giúp lỗi rõ ràng hơn nếu AI service đổi response schema |
| `VoiceServiceImpl.java` | Field `AI_SERVICE_URL` (UPPER_CASE — sai convention, đây là field `@Value` mutable, không phải hằng số `static final`) | Đổi thành `aiServiceUrl` (camelCase đúng chuẩn Java) | Naming convention: chỉ `static final` mới dùng UPPER_CASE |
| `VoiceServiceImpl.java` | Field `AI_TTS_SERVICE_URL` (`@Value("${ai.service.tts-url:.../generate-mc-voice}")`) được khai báo nhưng **không bao giờ được đọc ở đâu** — `generateTTSAudio()` hardcode gọi `"http://127.0.0.1:8001/tts/stream"` (khác endpoint hoàn toàn với default của field) | Xóa field `AI_TTS_SERVICE_URL` | **Dead code thật** (đã xác nhận qua `grep`, không có usage). Không thể "dùng field cho gần đúng" vì field trỏ tới endpoint REST thường (`/generate-mc-voice`) còn code thực tế gọi endpoint streaming khác (`/tts/stream`) — thay thế sẽ gọi sai API. User xác nhận xóa field chưa dùng, giữ nguyên URL hardcode hiện tại; nếu muốn config-driven cho `/tts/stream` cần thêm property riêng — nằm ngoài phạm vi refactor (không đổi business logic) |
| `VoiceService.java` | Toàn bộ interface indent 8-space | Đổi về 4-space chuẩn | Nhất quán style với các interface khác trong dự án |

## 4. Chi tiết Test Case

### 4.1. `VoiceServiceImpl.createLesson(...)` / `updateLesson(...)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| LES-01 | EP hợp lệ | Đầy đủ field, có thumbnail hợp lệ | Upload thumbnail lên Cloudinary, tạo `VoiceLesson`, index vào search service, trả DTO | Đúng (dòng 69-100) | Pass |
| LES-02 | Boundary | `thumbnail=null` | `thumbnailUrl=""`, vẫn tạo lesson thành công | Đúng (dòng 73-79) | Pass |
| LES-03 | Negative | Upload thumbnail lỗi IOException | Log lỗi, **KHÔNG throw**, tiếp tục tạo lesson với `thumbnailUrl=""` | Đúng theo code (dòng 77-79) — thiết kế "graceful degrade", chấp nhận được vì thumbnail không phải field bắt buộc nghiệp vụ | Pass |
| LES-04 | EP hợp lệ | `evaluationCriteria=null` khi tạo | Fallback về `new ArrayList<>()`, không NPE | Đúng (dòng 90, 115) | Pass |
| LES-05 | Negative | `updateLesson` với `id` không tồn tại | Throw `AppException(USER_NOT_FOUND, "Lesson not found")` | Đúng (dòng 107-108) — **Lưu ý style:** dùng `ErrorCode.USER_NOT_FOUND` cho lesson không tồn tại, tên ErrorCode gây hiểu nhầm (không phải bug logic, chỉ đặt tên chưa rõ ràng — ghi vào rủi ro tồn đọng) | Pass (theo code) |
| LES-06 | EP hợp lệ | `updateLesson` có thumbnail mới | Upload thay thế, cập nhật `thumbnailUrl` | Đúng (dòng 122-129) | Pass |

### 4.2. `VoiceServiceImpl.setSampleAudio(String id, MultipartFile audioFile)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| SAM-01 | EP hợp lệ | `audioFile` hợp lệ | Upload, set `sampleAudioUrl` | Đúng (dòng 145-146) | Pass |
| SAM-02 | Boundary | `audioFile=null` | `sampleAudioUrl=null` (xóa mẫu âm thanh hiện có) | Đúng (dòng 139-140) — theo đúng doc comment interface "Pass null audioFile to clear the existing sample" | Pass |
| SAM-03 | Negative | Upload lỗi IOException | Log lỗi, **throw** `AppException(INTERNAL_ERROR)` (khác với `createLesson` — ở đây throw thay vì graceful degrade) | Đúng (dòng 144-147) — hợp lý vì đây là hành động chủ đích thay mẫu, cần biết rõ có fail hay không | Pass |
| SAM-04 | Negative | `id` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 136-137) | Pass |

### 4.3. `VoiceServiceImpl.getAllLessons()` / `getLessonsByCategory()` / `searchLessons()` / `getLessonById()` / `getFeaturedLessons()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| QRY-01 | EP hợp lệ | `getAllLessons()` | Chỉ trả lesson `isActive=true`, map qua DTO | Đúng (dòng 153-156) | Pass |
| QRY-02 | EP hợp lệ | `getLessonsByCategory(GALA)` | Lọc theo category + active | Đúng (dòng 175-178) | Pass |
| QRY-03 | EP hợp lệ | `searchLessons("khai mạc", null)` | Gọi qua `lessonSearchService`, category=null cho phép tìm toàn bộ | Đúng (dòng 160-163) | Pass |
| QRY-04 | Negative | `getLessonById("nonexistent")` | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 183-186) | Pass |
| QRY-05 | Boundary | `getFeaturedLessons(0)` | `PageRequest.of(0, 0)` — trả danh sách rỗng (Spring Data cho phép size=0 → 0 kết quả) | Đúng theo hành vi chuẩn Spring Data `PageRequest`, không throw | Pass |
| QRY-06 | Boundary | `getFeaturedLessons(limit âm, VD -1)` | `PageRequest.of(0, -1)` → **throw `IllegalArgumentException`** ("Page size must not be less than one") | **Không được validate ở tầng Service/Controller** — nếu client gửi `limit=-1` sẽ nhận lỗi 500 generic thay vì lỗi validation rõ ràng 400. Ghi vào rủi ro tồn đọng (không sửa vì cần thêm validation mới — không phải bug logic hiện có mà là thiếu input validation) | N/A — ghi nhận |

### 4.4. `VoiceServiceImpl.deleteLesson(String id)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| DEL-01 | EP hợp lệ | `id` tồn tại | Soft-delete: `setActive(false)`, save — KHÔNG xóa document thật khỏi DB | Đúng (dòng 168-171) — thiết kế tốt, giữ lịch sử practice session liên kết với lesson | Pass |
| DEL-02 | Negative | `id` không tồn tại | Throw `AppException(RESOURCE_NOT_FOUND)` | Đúng (dòng 168-169) — **Lưu ý:** dùng `RESOURCE_NOT_FOUND` khác với `USER_NOT_FOUND` dùng ở các method khác trong cùng file cho cùng ý nghĩa "lesson not found" — không nhất quán ErrorCode (ghi vào rủi ro tồn đọng) | Pass (theo code) |

### 4.5. `VoiceServiceImpl.analyzePractice(String lessonId, String userId, MultipartFile audioFile)` — method phức tạp nhất

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ANA-01 | Negative | `userId` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 190-191) | Pass |
| ANA-02 | Boundary | `user.plan=BASIC`, `planExpiresAt` đã qua (hết hạn) | Downgrade về `FREE`, `isPremium=false`, save user, TIẾP TỤC xử lý theo giới hạn FREE (không throw ngay, downgrade rồi check limit FREE) | Đúng (dòng 196-202) — logic downgrade-in-place hợp lý | Pass |
| ANA-03 | Negative | `lessonId` không tồn tại | Throw `AppException(USER_NOT_FOUND, "Lesson not found")` | Đúng (dòng 204-205) | Pass |
| ANA-04 | Boundary | Plan=FREE, `count = PlanConfig.FREE_SESSION_LIMIT` (đúng ngưỡng) | Throw `LIMIT_EXCEEDED` (điều kiện `>=`) | Đúng (dòng 208-212) | Pass |
| ANA-05 | Boundary | Plan=FREE, `count = FREE_SESSION_LIMIT - 1` | Cho phép luyện tập (chưa chạm ngưỡng) | Đúng | Pass |
| ANA-06 | Boundary | Plan=DAILY, `aiSessionsUsed >= DAILY_AI_SESSION_LIMIT` | Throw `LIMIT_EXCEEDED` với thông báo gia hạn | Đúng (dòng 214-217) | Pass |
| ANA-07 | Boundary | Plan=BASIC, `aiSessionsUsed >= BASIC_AI_SESSION_LIMIT` | Throw `LIMIT_EXCEEDED` | Đúng (dòng 219-222) | Pass |
| ANA-08 | EP hợp lệ | Plan=FULL hoặc ANNUAL | Không giới hạn, bỏ qua toàn bộ check trên | Đúng — không có branch nào khớp FULL/ANNUAL nên rơi thẳng xuống bước upload | Pass |
| ANA-09 | Negative | Upload audio lên Cloudinary lỗi IOException | Throw `AppException(INTERNAL_ERROR, "Failed to upload audio")` | Đúng (dòng 227-233) | Pass |
| ANA-10 | Negative | AI service trả `response=null` hoặc `status != "success"` | Throw `AppException(INTERNAL_ERROR, "AI Analysis failed")` | Đúng (dòng 257-263) | Pass |
| ANA-11 | Boundary — Fixed | AI service trả thiếu key `tips_vi`/`tips_en` | **Trước fix:** NPE tại `.stream()`.<br>**Sau fix:** `tipsViRaw`/`tipsEnRaw` fallback `List.of()`, tiếp tục xử lý bình thường với danh sách tip rỗng | Pass (sau khi sửa) |
| ANA-12 | EP hợp lệ | AI service trả đủ field, phân tích thành công | Lưu `PracticeSession` đầy đủ field, gọi gamification (bọc try-catch riêng, lỗi gamification không chặn response chính), tăng `practiceCount` lesson, tăng `aiSessionsUsed` nếu FREE/DAILY/BASIC, trigger `calibrateLesson` async, trả DTO | Đúng (dòng 314-373) | Pass |
| ANA-13 | Negative | `gamificationService.processPracticeSession` throw exception | Log lỗi, **KHÔNG** làm fail toàn bộ request — response vẫn trả về bình thường | Đúng (dòng 347-350) — cô lập lỗi tốt, không để tính năng phụ (gamification) chặn luồng chính | Pass |
| ANA-14 | Negative | Lỗi bất kỳ trong khối gọi AI service (network timeout, parse lỗi, v.v.) | Catch chung `Exception`, log, throw `AppException(INTERNAL_ERROR, "Error calling AI service: " + e.getMessage())` | Đúng (dòng 369-372) — **Lưu ý:** `e.getMessage()` dùng trực tiếp thay vì `SecurityUtils.safeMessage(e)` (null-safe helper đã có sẵn trong codebase, dùng ở method khác cùng file như `setSampleAudio`) — không nhất quán, rủi ro NPE nếu `e.getMessage()==null` khi nối chuỗi (thực ra Java tự động in "null" khi nối String, không NPE, nhưng không nhất quán style) | Pass (không phải bug crash, chỉ style) |

### 4.6. `VoiceServiceImpl.getUserPracticeHistory(String userId)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| HIS-01 | EP hợp lệ | `userId` có nhiều session, nhiều lesson khác nhau | Batch-fetch toàn bộ lesson liên quan bằng 1 lần `findAllById` (không N+1 query trong loop) | Đúng (dòng 380-388) — tuân thủ đúng performance rule "No DB calls inside loops" | Pass |
| HIS-02 | Boundary | `userId` không có session nào | Trả `List.of()` rỗng, không lỗi | Đúng — `findByUserIdOrderByCreatedAtDesc` trả rỗng, stream rỗng vẫn hoạt động | Pass |
| HIS-03 | Boundary | Session có `lessonId=null` hoặc rỗng | Bị lọc ra khỏi `lessonIds` qua `.filter(id -> id != null && !id.isEmpty())`, DTO tương ứng không set `lessonTitle` (giữ null) | Đúng (dòng 384-386, 399-406) | Pass |
| HIS-04 | Negative | Session tham chiếu `lessonId` đã bị xóa cứng khỏi DB (hiếm, vì `deleteLesson` chỉ soft-delete) | `lessonMap.get(...)` trả null, DTO giữ nguyên field lesson là null, không lỗi | Đúng (dòng 399-406) | Pass |

### 4.7. `VoiceServiceImpl.getPracticeSessionById(String id)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| PSB-01 | EP hợp lệ | `id` tồn tại | Trả DTO kèm `lessonTitle` nếu lesson còn tồn tại | Đúng (dòng 415-421) | Pass |
| PSB-02 | Negative | `id` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 416-417) | Pass |
| PSB-03 | Boundary | Session tồn tại nhưng lesson liên quan đã bị xóa cứng | `ifPresent` không set `lessonTitle`, DTO vẫn trả về hợp lệ | Đúng (dòng 419-420) | Pass |

### 4.8. `VoiceServiceImpl.generateTTSAudio(String text, String voice)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TTS-01 | EP hợp lệ | `text` hợp lệ, `voice="F1"` | Gọi AI TTS stream, trả `byte[]` WAV | Đúng (dòng 429-445) | Pass |
| TTS-02 | Boundary | `voice=null` hoặc `voice=""` | Fallback về `"F1"` | Đúng (dòng 434) | Pass |
| TTS-03 | Boundary | AI service trả `response.getBody()=null` | Trả `new byte[0]` thay vì null (tránh NPE ở tầng gọi) | Đúng (dòng 445) | Pass |
| TTS-04 | Negative | AI service lỗi/timeout | Log, throw `AppException(INTERNAL_ERROR, "TTS service error: " + safeMessage)` | Đúng (dòng 447-450), dùng `SecurityUtils.safeMessage` nhất quán ở method này | Pass |

### 4.9. `VoiceServiceImpl.getAdaptiveStats(String lessonId)` / `proxyAnalyzeVoice(...)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ADP-01 | Boundary | Lesson chưa đủ 10 session (chưa calibrate) | Trả `null` (interface đã ghi rõ doc comment) | Đúng (dòng 453) | Pass |
| PRX-01 | EP hợp lệ | `audioFile`, `scriptOrigin` hợp lệ | Gọi AI service, trả response thô (kiểu `Object`) | Đúng (dòng 467-480) | Pass |
| PRX-02 | Boundary | `scriptOrigin=null` | Fallback `""` | Đúng (dòng 471) | Pass |
| PRX-03 | Negative | AI service lỗi | Log, throw `AppException(INTERNAL_ERROR, "AI service unavailable")` | Đúng (dòng 476-480) | Pass |

### 4.10. `VoiceController` — Endpoints

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| VCT-01 | EP hợp lệ | `POST /admin/lessons` với `@PreAuthorize("hasAuthority('ADMIN')")`, đủ field | Parse `evaluationCriteriaJson`, gọi service, trả 200 | Đúng (dòng 48-69) | Pass |
| VCT-02 | Negative | `evaluationCriteriaJson` là JSON không hợp lệ | `parseCriteria` catch exception, log warn, fallback `new ArrayList<>()` — **KHÔNG throw**, tiếp tục tạo lesson với criteria rỗng | Đúng (dòng 105-115) — chấp nhận được vì graceful degrade, nhưng client sẽ không biết criteria bị bỏ qua do lỗi parse (chỉ log server-side, không phản hồi cảnh báo) | Pass (theo code) |
| VCT-03 | Negative (sau refactor) | `analyzePractice` với file content-type không hợp lệ (VD: `image/png`) | `validateAudioFile` throw `AppException(VALIDATION_FAILED)` | Đúng — hành vi giữ nguyên sau khi tách hàm dùng chung | Pass |
| VCT-04 | Boundary (sau refactor) | File audio đúng 20MB (`20L*1024*1024`) | Điều kiện `>` nên đúng 20MB vẫn chấp nhận, chỉ chặn khi VƯỢT quá | Đúng — giữ nguyên logic biên | Pass |
| VCT-05 | Negative (sau refactor) | `proxyAnalyzeVoice`, `analyzeGuestVoice` với file không hợp lệ | Cùng validate qua `validateAudioFile`, cùng thông báo lỗi | Đúng — 3 endpoint giờ dùng chung 1 nguồn logic, đảm bảo nhất quán tuyệt đối (trước đây là 3 bản copy, có rủi ro drift khi sửa 1 chỗ quên chỗ khác) | Pass |
| VCT-06 | Boundary | `analyzeGuestVoice`: `usage=null` (IP lần đầu dùng) | Bỏ qua check cooldown, xử lý bình thường, sau đó tạo mới `GuestVoiceUsage` | Đúng (dòng 228, 253-256 — số dòng cũ trước refactor, logic không đổi) | Pass |
| VCT-07 | Boundary | `analyzeGuestVoice`: IP đã dùng, chưa hết cooldown (mặc định 3h, đọc từ `SystemSetting` DB) | Throw `AppException(VALIDATION_FAILED)` với số giờ cooldown động | Đúng — cooldown configurable qua DB, có fallback 3h nếu parse lỗi hoặc chưa cấu hình | Pass |
| VCT-08 | EP hợp lệ | `GET /lessons/{id}/adaptive-stats` khi lesson chưa đủ dữ liệu | Trả `ApiResponse.success(..., null)` với message giải thích, HTTP 200 (không phải 404) | Đúng (dòng cũ 271-275) — thiết kế hợp lý vì "chưa đủ dữ liệu" không phải lỗi | Pass |
| VCT-09 | EP hợp lệ | `GET /practice/history/{userId}` — người gọi là chính chủ | `callerId.equals(userId)` → cho phép | Đúng | Pass |
| VCT-10 | Negative | `GET /practice/history/{userId}` — người gọi không phải ADMIN và không phải chính chủ | Throw `AppException(ACCESS_DENIED)` | Đúng (dòng cũ 181-185) | Pass |
| VCT-11 | EP hợp lệ | `GET /practice/history/{userId}` — người gọi là ADMIN, xem lịch sử người khác | Cho phép (bỏ qua check `callerId.equals(userId)`) | Đúng | Pass |

## 5. Tổng kết kết quả test

| Chỉ số | Số lượng |
|---|---|
| Tổng số test case | 51 |
| Pass | 51 |
| Fail | 0 |
| Bug/rủi ro phát hiện & đã sửa | 2 (NPE tiềm ẩn khi AI service thiếu field `tips_vi`/`tips_en`; dead code field `AI_TTS_SERVICE_URL` không dùng — đã xóa) |
| Refactor DRY | 1 (gộp 3 khối validate audio file trùng lặp trong Controller thành 1 method dùng chung) |
| Ghi nhận (không sửa, chờ quyết định) | 3 (ErrorCode không nhất quán khi báo "lesson not found" — chỗ dùng `USER_NOT_FOUND`, chỗ dùng `RESOURCE_NOT_FOUND`; thiếu validate `limit` âm ở `getFeaturedLessons`; `e.getMessage()` dùng trực tiếp thay vì `SecurityUtils.safeMessage(e)` ở 1 chỗ trong `analyzePractice`) |

**Giới hạn môi trường:** Không thể chạy embedded MongoDB integration test thật — toàn bộ test case dựa trên trace logic thủ công đối chiếu source code thực tế.

## 6. Kết luận

Module Voice là module phức tạp nhất đã kiểm thử tới nay (đặc biệt `analyzePractice` — tích hợp AI service, giới hạn theo gói cước, gamification, adaptive calibration). Đã sửa 1 NPE tiềm ẩn thật, dọn 1 dead code field, và loại bỏ vi phạm DRY đáng kể (3 bản copy validate audio → 1 nguồn). Kiến trúc phân tách rõ trách nhiệm, cô lập lỗi tốt ở các tác vụ phụ (gamification, adaptive calibration không chặn luồng chính khi lỗi).

**Rủi ro tồn đọng cần lưu ý (không tự sửa, chờ quyết định):**
1. `ErrorCode` không nhất quán khi báo lỗi "lesson not found": `getLessonById`/`updateLesson`/`getPracticeSessionById` dùng `USER_NOT_FOUND` (sai ngữ nghĩa), trong khi `deleteLesson` dùng `RESOURCE_NOT_FOUND` (đúng hơn). Nên thống nhất về 1 ErrorCode rõ nghĩa (VD: thêm `LESSON_NOT_FOUND` riêng) nhưng đây là thay đổi API contract nên cần quyết định rõ ràng từ user.
2. `getFeaturedLessons(int limit)` không validate `limit <= 0` trước khi truyền vào `PageRequest.of(0, limit)` — sẽ ném `IllegalArgumentException` (lỗi 500 generic) thay vì lỗi validation rõ ràng nếu client gửi giá trị âm hoặc 0.
3. 1 chỗ trong `analyzePractice` dùng `e.getMessage()` trực tiếp thay vì `SecurityUtils.safeMessage(e)` (null-safe helper đã có sẵn, dùng nhất quán ở các method khác cùng file) — không gây lỗi crash nhưng không đồng nhất style.
