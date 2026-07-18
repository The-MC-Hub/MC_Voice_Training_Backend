# Integration Test Design — MC_Voice_Training_Backend

**Vai trò:** QA Tester độc lập. Tài liệu này là **THIẾT KẾ TEST CASE (test design spec)**, KHÔNG phải test case QA đã thực thi.

**Ngày tạo:** 2026-07-18
**Đối xứng với:** Mục 4 `testing/testing.md` — "02-integration/ — Integration test case (đối xứng Architecture) — API↔DB, API↔PayOS, API↔AI service" — đối chiếu `JAVA_BACKEND_GUIDE.md`, `schema.dbml`.

---

## 0. Phân biệt tài liệu này với `testing/03-system/` — ĐỌC TRƯỚC KHI DÙNG

| | `testing/03-system/*.md` (đã có) | `Integration_Test_Design.md` (tài liệu này) |
|---|---|---|
| QA đã thực thi? | **CÓ** — 311 test case chạy thật (HTTP request thật, JWT thật, MongoDB Atlas `mchub_test` thật, PayOS thật, AI service thật) qua server chạy port 5555 | **KHÔNG** — đây là bản thiết kế, chưa/không được QA chạy |
| Đối tượng test | Toàn bộ 1 luồng nghiệp vụ end-to-end (VD: "user đăng ký → OTP → login → đổi mật khẩu") | 1 SEAM kỹ thuật đơn lẻ giữa 2 tầng (VD: chỉ riêng "SecurityConfig có chặn đúng request không JWT hay không", tách khỏi mọi logic nghiệp vụ khác) |
| Vì sao cần thêm tài liệu này | System test đã VÔ TÌNH chạm các seam này như tác dụng phụ của việc test full flow — nhưng chỉ phát hiện được TỪNG endpoint MỘT khi chính nó nằm trong 1 kịch bản được thiết kế sẵn (VD: DEFECT-001/010/013/022 là 4 defect CÙNG 1 loại lỗi, bị phát hiện rời rạc ở 4 module khác nhau vì system test không có cơ chế quét toàn bộ seam 1 lần) | Cô lập đúng loại lỗi kỹ thuật, thiết kế thành checklist/ma trận có thể chạy lại **toàn bộ** 1 lần duy nhất mỗi khi seam đó bị đụng tới (VD: mỗi lần sửa `SecurityConfig.java`) — bắt được CẢ LỚP lỗi thay vì từng instance |
| Ai thực thi | QA (chạy `curl`/`MockMvc` thủ công đối chiếu response thật) | **Dev** — vì các test case dưới đây cần `@WebMvcTest`/`@SpringBootTest`/`@DataMongoTest` (viết code test trong `src/test/java`), nằm ngoài ranh giới quyền hạn QA theo `testing/testing.md` mục 2 ("KHÔNG được làm" — QA không viết code, kể cả code test) |
| Cột trạng thái | `Status` = Pass/Fail/Not Executed (kết quả thật) | `Trạng thái thiết kế` = Proposed (đề xuất, chưa chạy) — không có giá trị Pass/Fail vì QA chưa/không thể tự thực thi loại test slice này |

**Vì sao integration-layer test design lại cần thiết dù system test đã "PASS phần lớn":** `testing/01-unit/Unit_Test_Audit.md` mục 4 kết luận 15/24 defect (63%) thuộc lớp lỗi mà 490 unit test Mockito (mock toàn bộ dependency, không Spring context thật) **không có cấu trúc để phát hiện được**, vì lỗi chỉ lộ ra qua: Spring Security filter chain thật, Jackson deserialize thật, Spring Data derived-query engine thật, `@Async`/`@ControllerAdvice` thật, `Instant`/timezone JVM thật. System test (03) đã bắt được các defect này, nhưng CHỈ vì mỗi endpoint đó tình cờ nằm trong 1 test case thiết kế theo luồng nghiệp vụ — không có cơ chế đảm bảo MỌI endpoint cùng loại seam đều được quét. Integration test design dưới đây bù đắp đúng khoảng trống đó: biến "phát hiện tình cờ" thành "kiểm tra có hệ thống, lặp lại được, chạy trong CI".

---

## 1. Nhóm A — SecurityConfig ↔ Controller layer (whitelist matrix)

**Mục tiêu:** quét TOÀN BỘ route được Controller chủ đích thiết kế là public (comment `// Public`, tên method gợi ý guest/no-auth, hoặc không có `@PreAuthorize` VÀ không dùng `SecurityUtils.getCurrentUserId()`) đối chiếu 1-1 với danh sách `permitAll()` trong `SecurityConfig.java` — thay vì tìm ra từng route một cách tình cờ như đã xảy ra với DEFECT-001/010/013/022 (4 lần lặp lại cùng 1 nguyên nhân gốc, phát hiện rời rạc qua 4 lần Execute system test ở 4 module khác nhau, cách nhau nhiều giờ/ngày).

**Kỹ thuật đề xuất:** `@AutoConfigureMockMvc` + `@SpringBootTest(webEnvironment = RANDOM_PORT)` — dựng đúng `SecurityFilterChain`/`JwtAuthenticationFilter` thật, gọi từng route bằng `MockMvc` **không kèm** header `Authorization`, assert status code.

### TC-INT-SEC — Ma trận whitelist (checklist tái sử dụng)

Cột "Whitelisted trong SecurityConfig.java hiện tại?" đối chiếu trực tiếp `securityFilterChain()` (dòng 55-76) đọc ngày 2026-07-18.

| ID | Route | Method | Nguồn xác nhận "chủ đích public" | Whitelisted hiện tại? | Input (test) | Expected | Trạng thái thiết kế |
|---|---|---|---|---|---|---|---|
| TC-INT-SEC-01 | `/api/v1/payment/plans` | GET | Comment `PaymentController.java:42` "public — frontend fetches pricing" | Không | `MockMvc.perform(get(...))`, không có header Authorization | HTTP 200 | Proposed — target DEFECT-001 |
| TC-INT-SEC-02 | `/api/v1/payment/flash-deals` | GET | Comment `PaymentController.java:50` "public — no auth required" | Không | Như trên | HTTP 200 | Proposed — target DEFECT-001 |
| TC-INT-SEC-03 | `/api/v1/payment/apply-discount` | POST | Đặc tả UC-06 #3 "Áp dụng mã giảm giá" trước khi thanh toán, method không có `@PreAuthorize` | Không | Như trên | HTTP 200 (hoặc 400 nghiệp vụ, không phải 403) | Proposed — target DEFECT-001 |
| TC-INT-SEC-04 | `/api/v1/voice/guest-cooldown-hours` | GET | Tên method `getGuestCooldownHours()` — chủ đích hiển thị cho Guest trước khi dùng thử | Không | Như trên | HTTP 200 | Proposed — target DEFECT-010 |
| TC-INT-SEC-05 | `/api/v1/courses/reading-guides/{id}` | GET | `CourseController.java:22` section `// ── Public ──` | Không (chỉ match `/courses`, `/courses/{id}` 1-segment, không match 2-segment) | Như trên | HTTP 200 | Proposed — target DEFECT-013 |
| TC-INT-SEC-06 | `/api/v1/social-posts/{id}/click` | POST | Cùng Controller với `getActive()` (đã whitelist GET), đặc tả UC-05 #9 "ghi nhận click từ khách truy cập" | Không | Như trên | HTTP 200 | Proposed — target DEFECT-022 |
| TC-INT-SEC-07 | `/api/v1/courses/roadmap` | GET | `CourseController.java:22` section Public | Có (trùng hợp khớp pattern `{id}` 1-segment — xem ghi chú DEFECT-013) | Như trên | HTTP 200 | Proposed — regression guard, đã PASS qua system test nhưng nên đưa vào matrix để không bị fix "vô tình" phá khi dev sửa pattern route |
| TC-INT-SEC-08 | `/api/v1/courses/types` | GET | Như trên | Có (cùng lý do trùng hợp) | Như trên | HTTP 200 | Proposed — regression guard |
| TC-INT-SEC-09 | Toàn bộ route còn lại trong 26 Controller **không có `@PreAuthorize`** và **không** nằm trong 1 trong 8 case trên | GET/POST/PUT/DELETE tương ứng | Tự động hoá: parse `@RequestMapping`/`@GetMapping`/... không có `@PreAuthorize` phía trên | Cần dev tự sinh danh sách qua reflection/annotation scan lúc test chạy | Gọi không JWT | Nếu Controller không có `@PreAuthorize`: hoặc HTTP 200 (nếu whitelisted đúng chủ đích) hoặc HTTP 401/403 nhất quán (nếu KHÔNG whitelisted và KHÔNG chủ đích public — không phải HTTP 500) | Proposed — đây là hạng mục quan trọng nhất: biến việc "QA đọc code tìm case public" (thủ công, dễ sót — đã sót 4 lần) thành 1 test tự động quét TOÀN BỘ Controller mỗi lần CI chạy |

**Thiết kế TC-INT-SEC-09 chi tiết (gợi ý kỹ thuật cho dev, không phải code):** viết 1 test dùng Spring `RequestMappingHandlerMapping` để liệt kê toàn bộ `HandlerMethod` đã đăng ký, lọc ra method không có `@PreAuthorize`/`@Secured` trên method hoặc class, gọi từng route bằng `MockMvc` không JWT, assert status **khác 500** và **nhất quán với whitelist**. Test này tự động mở rộng ra khi có Controller/route mới — không cần dev nhớ thêm case thủ công mỗi khi thêm endpoint, khắc phục đúng nguyên nhân gốc khiến DEFECT-001/010/013/022 lặp lại 4 lần.

---

## 2. Nhóm B — DTO ↔ Jackson serialization layer (`boolean isXxx` round-trip)

**Mục tiêu:** verify field `boolean isXxx` trong các DTO nhận qua `@RequestBody` round-trip đúng qua Jackson **thật** (không dựng object Java tay như 490 unit test Mockito hiện tại) — target lớp lỗi DEFECT-011.

**Kỹ thuật đề xuất:** `@WebMvcTest(<Controller>.class)` + `MockMvc.perform(post(...).content(rawJsonString))` với `rawJsonString` viết tay chứa key `"isXxx"` (không dùng `ObjectMapper.writeValueAsString(dtoObject)` — cách đó sẽ tự động dùng đúng getter nên KHÔNG tái tạo được bug, phải viết JSON string thủ công đúng như client thật sẽ gửi).

### Danh sách DTO có field `boolean isXxx` (grep `private boolean is[A-Z]` trong `src/main/java/com/mchub/dto/`, đọc 2026-07-18)

| ID | DTO | Field | Dùng cho | Có phải input `@RequestBody` (rủi ro DEFECT-011)? | Trạng thái thiết kế |
|---|---|---|---|---|---|
| TC-INT-DTO-01 | `SaveCourseRequest.java:28` | `isActive` | `POST/PUT /admin/courses` | **Có — CHÍNH XÁC root cause DEFECT-011** | Proposed |
| TC-INT-DTO-02 | `MinigameResultDTO.java:15` | `isNewPersonalBest` | Response DTO (output, không phải `@RequestBody`) | Không (chỉ chiều đọc, Jackson serialize field→JSON dùng getter `isNewPersonalBest()`, không có mismatch chiều này) | Proposed — kiểm chứng loại trừ, không phải regression risk cao |
| TC-INT-DTO-03 | `MCProfileResponseDTO.java:16` | `isVerified` | Response DTO (output) | Không | Proposed — kiểm chứng loại trừ |
| TC-INT-DTO-04 | `CourseResponseDTO.java:34` | `isActive` | Response DTO (output) | Không | Proposed — kiểm chứng loại trừ |
| TC-INT-DTO-05 | `CourseResponseDTO.java:81` (nested) | `isCompleted` | Response DTO (output) | Không | Proposed — kiểm chứng loại trừ |
| TC-INT-DTO-06 | `CertificateResponseDTO.java:22` | `isVerified` | Response DTO (output) | Không | Proposed — kiểm chứng loại trừ |
| TC-INT-DTO-07 | `UserPreviewDTO.java:18` | `isPremium` | Response DTO (output) | Không | Proposed — kiểm chứng loại trừ |
| TC-INT-DTO-08 | `UserResponseDTO.java:22,25,29` | `isVerified`, `isActive`, `isPremium` | Response DTO (output) | Không | Proposed — kiểm chứng loại trừ |

**Kết luận rà soát:** trong số DTO có field `is*`, **chỉ `SaveCourseRequest.isActive` là input `@RequestBody`** — các DTO còn lại đều là response/output DTO (Jackson serialize theo getter, không có mismatch setter). Điều này khẳng định phạm vi ảnh hưởng thực tế của lớp lỗi DEFECT-011 hẹp hơn cảnh báo ban đầu trong chính DEFECT-011 ("nên rà soát toàn bộ codebase") — nhưng **TC-INT-DTO-01 vẫn phải được viết**, và quan trọng hơn: **bất kỳ DTO input mới nào có field `boolean isXxx` trong tương lai đều cần chạy qua checklist này trước khi merge** (đây là lý do thiết kế dạng bảng tái sử dụng thay vì 1 test case đơn lẻ).

| ID | Input (raw JSON) | Expected | Trạng thái thiết kế |
|---|---|---|---|
| TC-INT-DTO-01 | `MockMvc.perform(post("/api/v1/admin/courses").content("{\"isActive\":true, ...}"))` | HTTP 200, DB xác nhận course lưu `isActive:true` (round-trip đúng) — hiện tại (DEFECT-011 chưa fix) sẽ FAIL vì lưu `false` | Proposed — target DEFECT-011 trực tiếp |

---

## 3. Nhóm C — Repository ↔ MongoDB derived-query layer

**Mục tiêu:** verify tên method derived-query Spring Data MongoDB (`countByXxxIsYyyTrue`, `findByIsYyyTrue`, ...) thực sự build đúng query khớp field lưu trong MongoDB — target lớp lỗi DEFECT-012, vốn KHÔNG thể phát hiện bằng Mockito mock Repository (test hiện tại chỉ verify giá trị mock trả về đúng như đã `when(...).thenReturn(...)`, không bao giờ chạm cơ chế parse tên method thật).

**Kỹ thuật đề xuất:** `@DataMongoTest` (dùng embedded Mongo nếu chạy được trên máy CI, hoặc Testcontainers MongoDB nếu embedded tiếp tục không tải được binary trên Windows — xem giới hạn môi trường đã ghi ở `testing/testing.md` mục 7) — insert document thật với field đã biết giá trị, gọi derived-query method thật, assert kết quả đúng.

### Danh sách derived-query pattern `...IsXxxTrue`/`...IsXxxFalse`/`findByIsXxxTrue` (grep repositories, đọc 2026-07-18)

| ID | Repository method | File:line | Field liên quan (đã Lombok hoá) | Rủi ro cùng lớp DEFECT-012? | Trạng thái thiết kế |
|---|---|---|---|---|---|
| TC-INT-REPO-01 | `countByCourseIdAndIsCompletedTrue(String)` | `CourseEnrollmentRepository.java:17` | `CourseEnrollment.isCompleted` | **Có — CHÍNH XÁC root cause DEFECT-012** (đã xác nhận thực nghiệm trả về 0 sai) | Proposed — target DEFECT-012 trực tiếp |
| TC-INT-REPO-02 | `findByIsActiveTrue()` | `CourseRepository.java:14` | `Course.isActive` | Có — cùng pattern `IsXxxTrue`, chưa xác nhận qua test riêng dù `GET /courses` (dùng method này) đã PASS ở system test (TC-COURSE-02/03/08/11) — PASS đó chỉ xác nhận "trả đúng danh sách active hiện có", không cô lập test đúng derived-query parse | Proposed — cần verify độc lập dù system test đang PASS |
| TC-INT-REPO-03 | `findByTypeAndIsActiveTrue(CourseType)` | `CourseRepository.java:15` | `Course.isActive` | Có | Proposed |
| TC-INT-REPO-04 | `findByLearningPathTypeAndIsActiveTrue(LearningPathType)` | `CourseRepository.java:16` | `Course.isActive` | Có | Proposed |
| TC-INT-REPO-05 | `countByIsPremiumTrue()` | `UserRepository.java:31` | `User.isPremium` | Có — dùng cho thống kê Admin dashboard (tương tự bối cảnh DEFECT-012: số liệu thống kê sai không lộ ra qua luồng user thường) | Proposed — ưu tiên cao, cùng bối cảnh nghiệp vụ với defect đã xác nhận |
| TC-INT-REPO-06 | `countByIsPremiumTrueAndRoleNot(UserRole)` | `UserRepository.java:42` | `User.isPremium` | Có | Proposed |
| TC-INT-REPO-07 | `findByIsPremiumTrue()` | `UserRepository.java:52` | `User.isPremium` | Có | Proposed |
| TC-INT-REPO-08 | `findByUserIdAndUsedAtIsNullAndActiveTrue(String)` | `UserVoucherRepository.java:13` | `UserVoucher.active` (lưu ý: field này grep không thấy trong `models/`, cần dev xác nhận tên field gốc — nếu field gốc KHÔNG có tiền tố `is` thì đây có thể là case AN TOÀN, khác các case còn lại) | Cần xác nhận | Proposed — cần dev xác nhận tên field trước khi xếp loại rủi ro |
| TC-INT-REPO-09 | `findByIsActiveTrue()` | `VoiceLessonRepository.java:15` | `VoiceLesson.isActive` | Có | Proposed |
| TC-INT-REPO-10 | `findByCategoryAndIsActiveTrue(VoiceLessonCategory)` | `VoiceLessonRepository.java:16` | `VoiceLesson.isActive` | Có | Proposed |
| TC-INT-REPO-11 | `findByPracticeCountGreaterThanAndIsActiveTrueOrderByPracticeCountDesc(int, Pageable)` | `VoiceLessonRepository.java:17` | `VoiceLesson.isActive` | Có | Proposed |

**Lưu ý quan trọng cho dev đọc bảng trên:** không phải toàn bộ 10 method còn lại (TC-INT-REPO-02 → 11) chắc chắn lỗi giống DEFECT-012 — hiện tại `GET /courses`, `GET /voice/lessons` đều PASS qua system test, nghĩa là các method này **có thể đang hoạt động đúng** (khả năng Spring Data MongoDB map field-level bằng tên field Java gốc `isActive` nhất quán ở 1 số pattern nhưng không nhất quán ở pattern `count...IsXxxTrue` như DEFECT-012 — đây chính là điều DEFECT-012 khuyến nghị dev xác nhận bằng debug query log, QA không có quyền tự thêm log). Mục đích của bảng này là liệt kê ĐẦY ĐỦ diện tích rủi ro để dev quyết định mức độ ưu tiên viết `@DataMongoTest`, không khẳng định tất cả đều đang lỗi.

---

## 4. Nhóm D — JwtAuthenticationFilter ↔ Instant/timezone thật (target DEFECT-016, Critical/P0)

**Mục tiêu:** verify logic so sánh `passwordChangedAt` (lưu bằng `LocalDateTime.now()` — giờ local JVM) với JWT `issuedAt` (chuẩn UTC, `JwtAuthenticationFilter.java` dòng 65: `user.getPasswordChangedAt().toInstant(ZoneOffset.UTC)`) hoạt động đúng khi JVM server chạy ở múi giờ khác UTC — đây là defect Critical/P0 duy nhất trong nhóm 15 defect không thể bắt bằng unit test, nên tách riêng thành mục ưu tiên độc lập thay vì gộp chung nhóm A.

**Kỹ thuật đề xuất:** test cần chạy được với JVM timezone giả lập UTC+7 (`TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))` trong `@BeforeEach`, hoặc dùng `Clock` injectable nếu dev refactor theo khuyến nghị Mục 5.7 của `Unit_Test_Audit.md`) + `@SpringBootTest` dựng `JwtAuthenticationFilter` thật trong filter chain, gọi request thật với JWT thật.

| ID | Input | Expected | Trạng thái thiết kế |
|---|---|---|---|
| TC-INT-JWT-01 | JVM timezone = `Asia/Ho_Chi_Minh` (UTC+7). Đổi mật khẩu lúc UTC thật = `T`. Login lại ngay lập tức (JWT `iat` ≈ `T` + vài giây, UTC chuẩn). Gọi `GET /auth/me` với JWT mới. | HTTP 200 (token mới hơn `passwordChangedAt`, không bị từ chối) | Proposed — target DEFECT-016 trực tiếp, tái tạo đúng điều kiện thực nghiệm đã quan sát trong DEFECT-016 (token bị từ chối sai trong cửa sổ ~7 tiếng) |
| TC-INT-JWT-02 | Cùng điều kiện trên nhưng JVM timezone = `UTC` (kiểm chứng loại trừ — nếu server chạy UTC thì bug KHÔNG xuất hiện, xác nhận đúng root cause là lệch múi giờ chứ không phải lỗi logic khác) | HTTP 200 ở cả 2 timezone — nếu chỉ timezone `UTC` PASS còn `Asia/Ho_Chi_Minh` FAIL, xác nhận chính xác root cause | Proposed — kiểm chứng root cause, không phải case production (server thật luôn chạy UTC+7 theo môi trường đã ghi nhận) |
| TC-INT-JWT-03 | JWT phát hành TRƯỚC thời điểm đổi mật khẩu thật (token cũ, hợp lệ phải bị từ chối) | HTTP 401 (đúng — đây là hành vi guard MUỐN có, phải tiếp tục đúng sau khi fix DEFECT-016, không được fix quá tay làm mất tác dụng bảo mật gốc) | Proposed — regression guard đảm bảo fix không phá vỡ mục đích bảo mật ban đầu của guard |

---

## 5. Nhóm E — GlobalExceptionHandler ↔ Spring MVC exception dispatch thật

**Mục tiêu:** verify MỌI loại exception có thể được ném ra từ tầng Controller/Service/Jackson deserialize đều được `GlobalExceptionHandler` map đúng HTTP status — target lớp lỗi lặp lại 4 lần với cùng nguyên nhân gốc (generic catch-all `Exception.class` trả 500) đã ghi nhận trong `traceability/Traceability_Matrix.md`: DEFECT-015 (`IllegalStateException`), DEFECT-019 (`UnsupportedOperationException`), DEFECT-023 (`HttpMessageNotReadableException`), DEFECT-024 (`RuntimeException` trần).

**Xác nhận qua đọc trực tiếp `GlobalExceptionHandler.java` (đọc 2026-07-18):** hiện có 7 `@ExceptionHandler` (`AppException`, `MethodArgumentNotValidException`, `ConstraintViolationException`, `AccessDeniedException`, `AuthenticationException`, `NoHandlerFoundException`, `HttpRequestMethodNotSupportedException`) + 1 catch-all `Exception.class` → HTTP 500. **`IllegalStateException`, `UnsupportedOperationException`, `HttpMessageNotReadableException`, `RuntimeException` (khi không phải `AppException`) đều rơi vào catch-all** — xác nhận đúng root cause ghi trong cả 4 defect.

**Kỹ thuật đề xuất:** không cần `@SpringBootTest` đầy đủ — instantiate `new GlobalExceptionHandler()` thật (không mock), gọi trực tiếp từng `@ExceptionHandler` method với exception giả lập, assert `ResponseEntity` trả về đúng status/body. Riêng case Jackson-level (`HttpMessageNotReadableException`) cần `@WebMvcTest` + `MockMvc` với raw JSON sai để tái tạo đúng luồng thật (Jackson tự ném exception trước khi vào Controller).

| ID | Exception type | Input | Expected (sau khi dev bổ sung handler theo đề xuất) | Hiện tại (chưa fix) | Trạng thái thiết kế |
|---|---|---|---|---|---|
| TC-INT-EXC-01 | `IllegalStateException` (từ `SecurityUtils.getCurrentUserId()`) | `GlobalExceptionHandler().handleGenericException(new IllegalStateException("Could not determine current user"))`, hoặc `MockMvc` gọi `/auth/me` không JWT | HTTP 401 (nếu dev thêm handler riêng) | HTTP 500 (catch-all) | Proposed — target DEFECT-015 |
| TC-INT-EXC-02 | `UnsupportedOperationException` (từ `CertificateServiceImpl` stub) | `MockMvc` gọi `POST /certificates` với JWT MC hợp lệ | HTTP 410/400 (nếu dev thêm handler riêng, theo đề xuất trong DEFECT-019) | HTTP 500 (catch-all) | Proposed — target DEFECT-019 |
| TC-INT-EXC-03 | `HttpMessageNotReadableException` (Jackson deserialize enum sai) | `MockMvc.perform(post("/reports").content("{\"reason\":\"INVALID_REASON\", ...}"))` | HTTP 400 kèm message liệt kê giá trị enum hợp lệ (nếu dev thêm handler riêng) | HTTP 500 (catch-all) | Proposed — target DEFECT-023, ưu tiên cao vì ảnh hưởng TOÀN HỆ THỐNG (mọi DTO có field enum qua `@RequestBody`) |
| TC-INT-EXC-04 | `RuntimeException` trần (không phải `AppException`) | `MockMvc` gọi `PUT /reports/{id-không-tồn-tại}/resolve` với JWT ADMIN | HTTP 404 (nếu dev đổi `ReportServiceImpl` sang `AppException` theo đề xuất DEFECT-024) | HTTP 500 (catch-all) | Proposed — target DEFECT-024 |
| TC-INT-EXC-05 | Rà soát tất cả `orElseThrow(() -> new RuntimeException(...))` (không phải `AppException`) còn sót trong codebase — quét bằng grep `new RuntimeException(` trong `services/impl/` | — | Danh sách đầy đủ các chỗ lệch convention, không chỉ `ReportServiceImpl` | Chưa rà soát toàn bộ, chỉ xác nhận qua 1 case (DEFECT-024) | Proposed — công việc rà soát tĩnh (grep), không cần chạy test, nhưng nên làm cùng đợt vì cùng gốc |

---

## 6. Nhóm F — MongoDB database targeting (target DEFECT-003, Critical/P0)

**Mục tiêu:** verify chính xác database nào (`mchub` production / `mchub_test` / `voice-tranning` default fallback) mà 1 kết nối `MongoClient` thực sự đọc/ghi, khi biến môi trường `MONGODB_DATABASE` được set khác nhau — target defect nghiêm trọng nhất trong toàn bộ danh sách (khả năng đọc production thật + xóa đè database khác từ 1 API call).

**Đặc thù seam này khác các nhóm trên:** đây KHÔNG phải lỗi logic code sai, mà là hardcode tên database bỏ qua config — bản chất bug chỉ lộ ra khi SO SÁNH giá trị hardcode với giá trị `@Value`/biến môi trường thật đang chạy. Vì vậy test design ở đây tập trung vào **assertion đối chiếu cấu hình**, không phải test hành vi CRUD thông thường.

**Kỹ thuật đề xuất:** `@SpringBootTest` với `@TestPropertySource` set `MONGODB_DATABASE=mchub_test_isolated_marker` (tên database CHỈ DÙNG CHO TEST, không trùng bất kỳ DB thật nào) — gọi `DatabaseMigrationService.migrateFromMcHub()` (hoặc code sau khi dev fix đọc từ config), assert `MongoDatabase` object thực sự trỏ đúng `mchub_test_isolated_marker`, KHÔNG phải `mchub`/`voice-tranning` hardcode.

| ID | Input | Expected (sau khi dev fix theo đề xuất DEFECT-003 — đọc từ config) | Hiện tại (chưa fix) | Trạng thái thiết kế |
|---|---|---|---|---|
| TC-INT-DB-01 | `MONGODB_DATABASE=mchub_test_isolated_marker`, gọi `migrateFromMcHub()` | `sourceDb`/`targetDb` phải resolve theo giá trị config, KHÔNG phải chuỗi hardcode `"mchub"`/`"voice-tranning"` | Hiện tại source hardcode `"mchub"` bất kể config — test này sẽ FAIL cho tới khi dev fix, đúng mục đích | Proposed — target DEFECT-003 trực tiếp |
| TC-INT-DB-02 | Test riêng: assert `mongoClient.getDatabase(name).getName()` khớp `spring.data.mongodb.database` đang active trong `ApplicationContext`, áp dụng cho MỌI bean nào gọi `mongoClient.getDatabase(...)` bằng chuỗi trực tiếp thay vì qua `MongoTemplate`/`@Autowired MongoDatabase` chuẩn của Spring Data | Không có bean nào tự ý gọi `getDatabase()` với tên khác `@Value` config | Cần dev grep `mongoClient.getDatabase(` toàn bộ codebase để liệt kê đầy đủ, hiện tại xác nhận có ít nhất 1 chỗ (`DatabaseMigrationService`) | Proposed — mở rộng phạm vi rà soát ngoài riêng migration service, phòng chỗ khác cùng pattern |
| TC-INT-DB-03 | **Safeguard test** (không phải bug logic, mà đảm bảo an toàn vận hành): assert endpoint `/admin/migrate-db` **không thể gọi được** khi `spring.profiles.active=prod` (nếu dev áp dụng đề xuất `@Profile("dev")` từ DEFECT-003) | HTTP 403/404 khi profile = `prod` | Chưa có safeguard nào — endpoint hoạt động bất kể profile | Proposed — kiểm tra 1 trong các hướng fix đề xuất trong DEFECT-003, không bắt buộc nếu dev chọn hướng khác (xóa hẳn endpoint) |

---

## 7. Bảng tổng hợp — Nhóm test design ↔ DEFECT-XXX

| Nhóm | Seam kỹ thuật | Số test case thiết kế | DEFECT-XXX targeted | Lớp lỗi tương lai được ngăn chặn |
|---|---|---|---|---|
| A | SecurityConfig ↔ Controller (whitelist) | 9 (TC-INT-SEC-01 → 09) | DEFECT-001, 010, 013, 022 | Mọi endpoint public mới thêm sau này thiếu whitelist — TC-INT-SEC-09 tự động quét, không cần nhớ thủ công |
| B | DTO ↔ Jackson (`boolean isXxx`) | 8 (TC-INT-DTO-01 → 08) + checklist tái sử dụng | DEFECT-011 | Mọi DTO input mới có field `boolean isXxx` — checklist yêu cầu chạy lại mỗi khi thêm DTO |
| C | Repository ↔ MongoDB derived-query | 11 (TC-INT-REPO-01 → 11) | DEFECT-012 | Mọi derived-query pattern `...IsXxxTrue/False` khác trong tương lai — không chỉ riêng `CourseEnrollmentRepository` |
| D | JwtAuthenticationFilter ↔ Instant/timezone | 3 (TC-INT-JWT-01 → 03) | DEFECT-016 (Critical/P0) | Mọi so sánh thời gian giữa `LocalDateTime.now()` (naive, JVM-local) và nguồn UTC chuẩn (JWT, `Instant`) — khuyến nghị áp dụng cùng kỹ thuật test cho `planExpiresAt`, `lockedUntil` nếu có logic tương tự |
| E | GlobalExceptionHandler ↔ Spring MVC dispatch | 5 (TC-INT-EXC-01 → 05) | DEFECT-015, 019, 023, 024 | Mọi exception type mới ném ra từ Service/Jackson trong tương lai mà quên thêm `@ExceptionHandler` riêng — vẫn rơi vào catch-all 500 cho tới khi có test này chặn ở CI |
| F | MongoDB database targeting | 3 (TC-INT-DB-01 → 03) | DEFECT-003 (Critical/P0) | Mọi bean tương lai tự ý gọi `mongoClient.getDatabase(hardcodedName)` thay vì qua config — đặc biệt nguy hiểm vì đây là lớp lỗi duy nhất có khả năng ảnh hưởng dữ liệu production thật |

**Tổng cộng: 39 test case thiết kế (6 nhóm), targeting toàn bộ 11 defect thuộc nhóm "Không" trong `Unit_Test_Audit.md` mục 4 mà đề bài yêu cầu (001, 002*, 003, 010, 011, 012, 013, 014*, 016, 022, 023) — riêng DEFECT-002 (race condition 2 request chồng lấn thời gian thực) và DEFECT-014 (`@Async` nuốt exception khi chạy qua thread pool thật) không có nhóm test design riêng ở trên vì bản chất 2 defect này đòi hỏi mô phỏng ĐỒNG THỜI/BẤT ĐỒNG BỘ (concurrency, không phải 1 seam tĩnh giữa 2 tầng) — đây là loại kịch bản phù hợp hơn với `05-regression/` dạng test có `@Async` thật + `CountDownLatch`/`Thread.sleep` kiểm soát thứ tự, nằm ngoài phạm vi "cô lập 1 seam" mà tài liệu này tập trung. Đề xuất bổ sung riêng khi `05-regression/` được thiết lập.*

**Ý nghĩa với dev/PO:** đây không phải 39 test case để "vá 11 chỗ đã biết lỗi" — 6 nhóm trên được thiết kế dạng CHECKLIST/MA TRẬN tái sử dụng, mỗi nhóm sẽ tự động mở rộng phạm vi bao phủ khi codebase có thêm Controller/DTO/Repository/exception type mới cùng loại seam. Nói cách khác: implement 6 nhóm này vào CI (`@WebMvcTest`/`@SpringBootTest`/`@DataMongoTest` chạy cùng `mvn test`) sẽ ngăn được **TOÀN BỘ LỚP lỗi tương lai cùng dạng**, không chỉ 11 instance cụ thể đã tìm thấy — đúng đúng vai trò của tầng Integration Test trong V-model (đối xứng Architecture/Design, không đối xứng từng defect đơn lẻ).

---

*Tài liệu này được tạo bởi QA độc lập dựa trên việc đọc source code thật (`src/main/java`) và 11 file defect liên quan tại `testing/defect-log/`. Đây là THIẾT KẾ test case đề xuất cho dev implement dưới dạng `@WebMvcTest`/`@SpringBootTest`/`@DataMongoTest` trong `src/test/java` — QA không viết, không chạy, không sửa bất kỳ dòng code test hay code nguồn nào trong quá trình tạo tài liệu này, theo đúng ranh giới thẩm quyền tại `testing/testing.md` mục 2.*
