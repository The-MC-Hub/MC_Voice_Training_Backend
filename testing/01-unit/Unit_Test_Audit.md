# Unit Test Audit — MC_Voice_Training_Backend

**Vai trò:** QA Tester độc lập — tài liệu này là AUDIT (kiểm toán độc lập) đối với bộ unit test đã có sẵn do dev viết tại `src/test/java/com/mchub/`. QA **không viết/sửa** bất kỳ file test nào trong `src/test/java` hay code nào trong `src/main/java` — toàn bộ nội dung dưới đây là quan sát, đối chiếu, và phân tích dựa trên source code đã đọc.

**Ngày thực hiện:** 2026-07-18
**Đối xứng với:** Mục 4 `testing/testing.md` — "01-unit/ — Unit test case (đối xứng Module Design) — theo Service/Component". Tài liệu này đối xứng theo hướng AUDIT (đánh giá bộ test hiện có), không phải viết lại test case unit từ đầu, vì unit test đã tồn tại và đã pass — công việc QA giá trị nhất ở tầng này là kiểm toán độ phủ và cross-reference với defect thật đã tìm thấy ở tầng System (03).

---

## 1. Tổng quan

| Mục | Giá trị |
|---|---|
| Tổng số test method (`@Test`) | **490** |
| Kết quả `mvn test` | **BUILD SUCCESS** — 490 run, 0 failures, 0 errors, 0 skipped |
| Số file test | **46** (23 `*ControllerTest.java` tại `controllers/` + 3 `*ControllerTest.java` tại `controllers/admin/` + 4 service test không theo suffix `ServiceImpl` tại `services/` + 16 `*ServiceImplTest.java` tại `services/impl/`) |
| Framework | JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `when()/verify()`) |
| Kỹ thuật | Mock toàn bộ Repository/dependency ở tầng dưới — **KHÔNG kết nối DB thật**, không khởi tạo Spring `ApplicationContext`, không có `@WebMvcTest`/`@SpringBootTest` |
| Lý do không dùng DB thật | `de.flapdoodle.embed.mongo` (embedded MongoDB) không tải được binary phù hợp trên máy Windows hiện tại — xem `testing/testing.md` mục 7 |
| Cách chạy | `mvn test` (local, chưa có CI/CD pipeline) |

**Ý nghĩa của "490/490 PASS":** con số này xác nhận code hiện tại **khớp với kỳ vọng mà chính dev đã viết ra trong test** — không phải bằng chứng "không có bug". Phần lớn 24 defect tìm thấy ở tầng System Test (03) không mâu thuẫn với bất kỳ assertion nào trong 490 test này, vì các test này đơn giản là không có scenario tương ứng để phát hiện — chi tiết ở Mục 4.

---

## 2. Coverage map — 46 file test

### 2.1 Controller tests (26 file)

| File | Nested class (scenario nhóm) | Số `@Test` |
|---|---|---|
| `AdminCompetitionControllerTest` | GetAll, Create, Delete | 4 |
| `AdminControllerTest` | ReadOnlyDelegation, UpdateUserStatus, UpdateUserPlan, CreateUser, ChangePassword, DeleteUser, NotifyEmail, MigrateDb, GuestCooldownSettings | 19 |
| `AnnouncementControllerTest` | Create, Send, EmailPreview, TriggerNewLesson, TriggerDiscount | 9 |
| `AuditLogControllerTest` | Queries, PurgeLogs | 6 |
| `AuthControllerTest` | Register, Login, VerifyOtp, PasswordReset, UpdateSettings | 14 |
| `CertificateControllerTest` | AddCertificate, GetCertificates, Verify, DeleteCertificate | 6 |
| `CommunityControllerTest` | GetStats, GetLeaderboard, GetMyRank, GetActiveArenas | 6 |
| `ContactControllerTest` | SendContact | 4 |
| `CourseControllerTest` | ListCourses, GetRoadmap, Enroll, CompleteLesson, SubmitQuiz, MyEndpoints | 9 |
| `EmailCampaignControllerTest` | TemplateEndpoints, DeleteTemplate, SendCampaign, CountRecipients, TestSend, GetCampaignLogs | 8 |
| `LogControllerTest` | GetLogs, Ingest, Stream | 4 |
| `MCControllerTest` | GetDashboard, UpdateProfile | 2 |
| `MediaControllerTest` | UploadMedia | 3 |
| `MinigameControllerTest` | GetPrompts, SubmitRun, GetLeaderboard | 5 |
| `PaymentControllerTest` | PublicEndpoints, CreateOrder, CreateCourseOrder, Webhook, GetPaymentStatus, SimulateSuccess, AdminCompleteTransaction | 24 |
| `PublicControllerTest` | LandingAndFeatured, GetMcProfile, DiscoverMcs, Enums | 7 |
| `QuestControllerTest` | GetProgress, CompleteQuest, ClaimVoucher | 8 |
| `ReportControllerTest` | CreateReport, GetAllReports, ResolveReport | 7 |
| `SocialPostControllerTest` | GetActive, RecordClick | 2 |
| `UserControllerTest` | GetLoginStreak, UseFreeze | 5 |
| `UserHighlightControllerTest` | GetHighlights, CreateHighlight, UpdateHighlight, DeleteHighlight | 7 |
| `VoiceControllerTest` | GetLessons, AnalyzePractice, GetHistory, AnalyzeGuestVoice, GetAdaptiveStats, GetGuestCooldownHours | 17 |
| `VoucherControllerTest` | GetMyVouchers, GetAvailableVouchers | 3 |
| `admin/AdminCourseControllerTest` | Read, Create, UpdatePricing, Delete | 8 |
| `admin/AdminPlanControllerTest` | PlanCrud, SeedDailyPlan, DiscountCrud, DeleteDiscount | 7 |
| `admin/AdminSocialPostControllerTest` | Create, Delete, Toggle | 5 |

Controller test đặc điểm chung: dùng `@MockBean`/`@Mock` thay cho toàn bộ Service, gọi trực tiếp method Controller trong JVM (không qua `DispatcherServlet`, không qua `MockMvc` HTTP layer thật trong đa số file — xác nhận qua cách gọi method Java trực tiếp thay vì `mockMvc.perform(...)`), assertion tập trung vào: (a) response envelope đúng field, (b) Service được gọi đúng tham số qua `verify()`, (c) exception từ Service được propagate/convert đúng.

### 2.2 Service tests (20 file)

| File | Nested class | Số `@Test` |
|---|---|---|
| `services/AnnouncementServiceTest` | Create, Update, Delete, GetUsersByPlan, ApproveAndSendByTargetPlans, ApproveAndSendByExplicitIds, CreateFromTrigger, PreviewStats, GetById | 17 |
| `services/CompetitionServiceTest` | CreateCompetition, UpdateCompetition, DeleteCompetition | 7 |
| `services/PayOSServiceTest` | CreatePaymentLink, VerifyWebhookSignature | 10 |
| `services/PlanServiceTest` | ApplyDiscount, ConsumeDiscount, GetActiveFlashDeals, PlanCrud, DiscountCrud | 24 |
| `services/impl/AdminServiceImplTest` | UpdateUserStatus, UpdateUserPlan, DeleteUser, CreateUser, Lookups, PasswordOps, NotificationEmail, UserStats | 22 |
| `services/impl/AuditLogServiceImplTest` | Log, LogError, PurgeLogs, Queries | 9 |
| `services/impl/AuthServiceImplTest` | Login, VerifyAdminLoginOtp, ResetPassword, ForgotPassword, VerifyOtp, VerifyEmailByToken, Register, UpdateSettings, ResendOtp | 35 |
| `services/impl/CertificateServiceImplTest` | DeprecatedStubs, GetCertificatesByMCProfile | 5 |
| `services/impl/CommunityServiceImplTest` | GetCommunityStats, GetLeaderboardDispatch, GetUserRank, GetActiveArenas | 16 |
| `services/impl/CourseServiceImplTest` | CompleteLesson, CompleteReading, HasCourseAccess, Enroll | 15 |
| `services/impl/EmailCampaignServiceImplTest` | ResolveRecipients, SendCampaign, TemplateCrud, GetCampaignById | 15 |
| `services/impl/GamificationServiceImplTest` | GetOrCreate, XpAndTier, PracticeStreak, HighScoreStreak, BadgeIdempotency, LoginStreak, AddMinigameXp, Competitions | 25 |
| `services/impl/JwtServiceImplTest` | GenerateAndExtract, IsTokenValid | 7 |
| `services/impl/LogServiceImplTest` | GetLogs, IngestExternal, CleanOldLogs | 9 |
| `services/impl/MCProfileServiceImplTest` | GetDashboardStats, UpdateProfile | 8 |
| `services/impl/MinigameServiceImplTest` | GetSpeedReaderPrompts, SubmitSpeedReaderRun, GetLeaderboard | 18 |
| `services/impl/PublicServiceImplTest` | GetFeaturedMCTrainingStats, GetLandingData, DiscoverMCs, GetMCProfile, EnumOptions | 11 |
| `services/impl/ReportServiceImplTest` | CreateReport, ResolveReport, Queries | 6 |
| `services/impl/SocialPostServiceImplTest` | Listing, CreateAndUpdate, DeletePost, ToggleActive, RecordClick | 10 |
| `services/impl/VoiceServiceImplTest` | AnalyzePracticePlanExpiry, AnalyzePracticeSessionLimit, AnalyzePracticeAiSessionCounter, AnalyzePracticeSideEffects, CrudOperations, History | 23 |

Nhiều file có docblock ghi rõ mục đích là **regression guard** cho các fix hiệu năng trước đó (VD: `PublicServiceImplTest` — "Regression guard for N+1 DB-call-in-loop fix"; `MCProfileServiceImplTest` — "Verifies the DEFECT fix... practiceSessionRepository.findByUserId() twice... now a single fetch"). Đây là bằng chứng bộ test được duy trì có chủ đích, không chỉ để đạt coverage số lượng.

---

## 3. Gaps — Class không có unit test trực tiếp

Đối chiếu toàn bộ `src/main/java/com/mchub/controllers/**` (26 file) và `src/main/java/com/mchub/services/impl/**` (17 file) với 46 file test:

- **Tất cả 26 Controller đều có file `*ControllerTest.java` tương ứng.** Không có gap ở tầng Controller.
- **16/17 class trong `services/impl/` có file `*ServiceImplTest.java` tương ứng.**
- **1 gap tìm thấy: `DatabaseMigrationService.java` — KHÔNG có `DatabaseMigrationServiceTest.java`.**
  - Class này chỉ xuất hiện trong test suite dưới dạng `@MockBean private DatabaseMigrationService migrationService;` bên trong `AdminControllerTest` (nhóm nested `MigrateDb`) — nghĩa là `AdminControllerTest` chỉ verify Controller gọi đúng `migrationService.migrateFromMcHub()` (qua `verify(migrationService).migrateFromMcHub()`), **hoàn toàn không test logic bên trong** `migrateFromMcHub()`.
  - Đây chính xác là class chứa root cause của **DEFECT-003** (hardcode tên database `mchub`/`voice-tranning`, bỏ qua cấu hình môi trường, `drop()` ghi đè dữ liệu). Vì không có unit test riêng cho `DatabaseMigrationService`, không có cách nào ở tầng unit để phát hiện việc tên database bị hardcode — kể cả nếu có unit test, một test dùng Mockito mock `MongoClient` cũng khó lòng bắt được loại lỗi "đọc nhầm sang database khác với config đang chạy" vì bản chất bug này chỉ lộ ra khi so sánh với **giá trị cấu hình thật** (`application.properties`/env var), điều mà mock hoàn toàn không tham chiếu tới.

**Kết luận Mục 3:** gap về coverage-tồn-tại-hay-không chỉ có 1 trường hợp (`DatabaseMigrationService`), nhưng như phân tích ở Mục 4, "có file test" không đồng nghĩa "test đó có khả năng bắt được lớp lỗi mà System Test tìm thấy" — đa số 24 defect nằm ở những class ĐÃ có test file đầy đủ.

---

## 4. Đối chiếu 24 Defect ↔ Khả năng bắt lỗi ở tầng Unit Test

Cột "Unit test có thể bắt?" đánh giá: nếu bộ Mockito unit test (kiến trúc hiện tại — mock repo, không Spring context, không Jackson thật, không security filter chain thật) được viết đầy đủ nhất có thể trong đúng kiến trúc này, nó có CẤU TRÚC để phát hiện lớp lỗi này không — không phải "test hiện tại có case đó chưa".

| Defect | Root cause class | Có file unit test? | Unit test có thể bắt? | Lý do |
|---|---|---|---|---|
| DEFECT-001 (Payment whitelist 403 cho Guest) | `SecurityConfig.java` | Không có `SecurityConfigTest` | **Không** | Mockito unit test gọi thẳng method Controller trong JVM, không đi qua `SecurityFilterChain`/`JwtAuthenticationFilter` thật. Whitelist path trong `SecurityConfig` chỉ có hiệu lực khi có `MockMvc` + `@WebMvcTest`/`@SpringBootTest` dựng filter chain thật. |
| DEFECT-002 (Race condition ghi đè plan) | `PaymentController` + `PaymentService` | Có (`PaymentControllerTest`) | **Không** | Đây là lỗi về THỨ TỰ 2 request độc lập chạy tuần tự thật (giao dịch cũ hoàn tất sau giao dịch mới) — unit test gọi 1 method đơn lẻ với input cố định, không mô phỏng được kịch bản 2 lệnh gọi API chồng lấn theo thời gian thực. Đây là lỗi thuộc tầng Integration/System, không phải Unit. |
| DEFECT-003 (Migrate DB hardcode) | `DatabaseMigrationService` | **Không có file test riêng** (chỉ mock trong AdminControllerTest) | **Không** | Không có test nào gọi trực tiếp `migrateFromMcHub()`. Ngay cả khi có, bug là "tên database hardcode SAI SO VỚI CONFIG ĐANG CHẠY" — một unit test mock `MongoClient` sẽ chỉ verify `getDatabase("mchub")` được gọi đúng như code viết, nó không có cách nào biết "mchub" có khớp với `application.properties` thật hay không trừ khi test đọc `@Value` từ context Spring thật. |
| DEFECT-004 (`isTokenValid` throw thay vì false) | `JwtServiceImpl` | Có (`JwtServiceImplTest`, nested `IsTokenValid`) | **Có — VÀ ĐÃ bắt được** | Đây là trường hợp DUY NHẤT trong 24 defect được phát hiện TRỰC TIẾP qua unit test thật (JJWT thật, không mock). Ghi chú trong chính DEFECT-004: "phát hiện khi viết unit test JUnit thật". Chứng minh unit test layer hoạt động đúng chức năng khi bug nằm gọn trong logic 1 method không phụ thuộc Spring context. |
| DEFECT-005 (`updateProfile` xóa personality/hostingStyle) | `MCProfileServiceImpl` | Có (`MCProfileServiceImplTest`) | **Có — VÀ ĐÃ bắt được** | Tương tự DEFECT-004 — bug logic thuần Java (guard `!= null` thiếu `!isBlank()`), Mockito test với input/output cụ thể phát hiện được ngay, không cần Spring/DB/Jackson thật. Ghi chú DEFECT-005 xác nhận: "phát hiện khi viết unit test JUnit thật". |
| DEFECT-006 (Course giá 0đ → HTTP 500) | `PaymentController.createCourseOrder()` | Có (`PaymentControllerTest`, nested `CreateCourseOrder`) | **Partial** | Về mặt cấu trúc, 1 test case "effectiveAmount = 0 → verify KHÔNG gọi PayOS, verify tạo transaction COMPLETED" hoàn toàn khả thi trong Mockito (so sánh với `createPremiumOrder()` đã có nhánh này). Nhưng cần rà kỹ file test hiện tại — nested `CreateCourseOrder` không có case này, nghĩa là bug tồn tại được vì **thiếu use-case cụ thể trong test**, không phải vì kiến trúc unit test không đủ khả năng — khác với các case "Không" ở trên vốn không thể bắt được dù thiết kế test tốt cỡ nào. |
| DEFECT-007 (`limit` param bị bỏ qua) | `LogServiceImpl.getLogs()` | Có (`LogServiceImplTest`, nested `GetLogs`) | **Có, nhưng KHÔNG được test khai thác** | Xác nhận trực tiếp: mọi test trong `GetLogs` gọi `service.getLogs("error", "java", 200)` rồi `verify(logRepository).findTop200By...(...)` — không có case nào gọi với `limit=10` để assert số lượng bản ghi trả về khác 200. Đây là bug hoàn toàn nằm trong tầm với của Mockito unit test (logic thuần Java, dùng field `limit` hay không), nhưng bộ test hiện tại thiếu đúng 1 assertion cần thiết — case "test tồn tại nhưng thiếu đúng 1 scenario" thay vì "kiến trúc không cho phép". |
| DEFECT-008 (`scriptOrigin` rỗng → AI service 422→500) | `VoiceServiceImpl.proxyAnalyzeVoice()` | Có (`VoiceServiceImplTest`) | **Không** | Bug phụ thuộc hành vi THẬT của AI service ngoài (FastAPI/Pydantic coi multipart field rỗng là "missing"). Mockito mock `RestTemplate`/HTTP client — không bao giờ gọi AI service thật, nên không thể phát hiện quirk parse phía server ngoài. Đây là lỗi tầng Integration (API↔AI service), đúng như phân loại trong `testing/testing.md` mục 4. |
| DEFECT-009 (TTS URL hardcode sai `127.0.0.1:8001`) | `VoiceServiceImpl.generateTTSAudio()` | Có (`VoiceServiceImplTest`) | **Partial** | Nếu test mock `RestTemplate.exchange()` và dùng `ArgumentCaptor` để assert URL truyền vào KHỚP với giá trị `@Value("${ai.service.tts-url}")` (thay vì hardcode), unit test HOÀN TOÀN có thể bắt được bug này (verify literal string "127.0.0.1:8001/tts/stream" khác field cấu hình). Nhưng vì code hiện tại còn không có field `@Value` cho TTS URL, viết được 1 test kiểu này đòi hỏi dev sửa code trước — về bản chất bug ở đây là code không đọc config, nên unit test dù viết tốt cỡ nào cũng chỉ xác nhận "code hardcode URL X" chứ không tự phát hiện "X sai với env" nếu không có phép so sánh với nguồn cấu hình thật. |
| DEFECT-010 (Voice guest-cooldown-hours 403) | `SecurityConfig.java` | Không có `SecurityConfigTest` | **Không** | Cùng lý do DEFECT-001 — whitelist chỉ có hiệu lực qua filter chain thật. |
| DEFECT-011 (`isActive` Lombok/Jackson binding) | `SaveCourseRequest.java` (DTO) | **Không có file test cho DTO** | **Không** | Đây là ví dụ rõ nhất của lớp lỗi "Unit test cấu trúc không thể bắt". Mockito unit test gọi `courseService.createCourse(SaveCourseRequestObject)` bằng cách **tự dựng object Java trực tiếp** (`new SaveCourseRequest(); req.setActive(true);` hoặc builder) — không bao giờ đi qua bước Jackson deserialize JSON→Object thật. Bug chỉ tồn tại ở chính bước đó (`"isActive": true` trong JSON không khớp setter Lombok sinh ra `setActive()`). Không có `@WebMvcTest` + `MockMvc.perform(post(...).content(rawJsonString))` thì lớp lỗi này **cấu trúc không thể lộ ra** trong bất kỳ unit test nào, dù viết bao nhiêu case. |
| DEFECT-012 (`countByCourseIdAndIsCompletedTrue` derived query sai) | `CourseEnrollmentRepository.java` (Repository interface) | **Không có file test cho Repository** | **Không** | Repository interface của Spring Data MongoDB không có triển khai Java thật — chỉ có method signature, Spring Data sinh query lúc runtime dựa trên tên method. Mockito mock Repository nghĩa là mock TOÀN BỘ hành vi này đi (`when(repo.countByCourseIdAndIsCompletedTrue(...)).thenReturn(x)`) — test sẽ luôn "pass" theo đúng giá trị mock, không bao giờ chạm vào cơ chế parse tên method thật của Spring Data. Đây là lỗi CHỈ lộ ra khi có DB thật (embedded Mongo hoặc Testcontainers) chạy `@DataMongoTest`. |
| DEFECT-013 (`reading-guides/{id}` 403) | `SecurityConfig.java` | Không có `SecurityConfigTest` | **Không** | Cùng lý do DEFECT-001/010. |
| DEFECT-014 (`@Async` nuốt exception) | `AnnouncementService.approveAndSend()` | Có (`services/AnnouncementServiceTest`, nested `ApproveAndSendByTargetPlans`/`ApproveAndSendByExplicitIds`) | **Không** | Mockito gọi method `@Async` trực tiếp, ĐỒNG BỘ trong thread test (Spring AOP proxy tạo hành vi async chỉ tồn tại khi có `ApplicationContext` thật quản lý bean này qua `@EnableAsync`). Trong unit test, exception ném ra từ `approveAndSend()` sẽ propagate bình thường lên test method (`assertThrows` hoạt động đúng) — nghĩa là bản thân guard logic ("throw nếu đã SENT") ĐƯỢC test đúng, nhưng lớp lỗi thật (exception bị nuốt bởi `SimpleAsyncUncaughtExceptionHandler` KHI CHẠY THẬT QUA THREAD POOL ASYNC) không hề xuất hiện trong môi trường test đồng bộ — nghịch lý: unit test PASS chính vì nó không tái tạo được điều kiện gây lỗi. |
| DEFECT-015 (`getMe()` 500 thay vì 401 cho Guest) | `SecurityUtils` + `AuthController`/`CommunityController` | Có (`AuthControllerTest`, `CommunityControllerTest`) | **Partial** | Unit test controller thường mock `SecurityUtils.getCurrentUserId()` (qua Mockito static mock hoặc gọi trực tiếp) để trả về userId hợp lệ — nếu có thêm case "SecurityUtils ném IllegalStateException → verify Controller/GlobalExceptionHandler trả đúng mã lỗi", về lý thuyết unit test CÓ THỂ mô phỏng được exception này (không cần Spring Security thật, chỉ cần mock `SecurityUtils` throw). Nhưng phần quan trọng nhất của bug — `GlobalExceptionHandler` không có handler riêng cho `IllegalStateException` — là hành vi Ở TẦNG SPRING MVC exception resolution, chỉ lộ rõ khi có `@ControllerAdvice` thật xử lý exception ném ra từ controller, tức cần `@WebMvcTest` để bắt trọn vẹn. Unit test kiến trúc hiện tại có thể phát hiện "SecurityUtils throw đúng loại exception" nhưng khó xác nhận "response HTTP status cuối cùng client nhận được là gì". |
| DEFECT-016 (Lệch múi giờ `passwordChangedAt` vs JWT `iat`) | `AuthServiceImpl` + `JwtAuthenticationFilter` | Có (`AuthServiceImplTest`) nhưng `JwtAuthenticationFilter` **không có file test riêng** | **Không** | Bug là do SO SÁNH giữa 2 nguồn thời gian khác nhau (`LocalDateTime.now()` hệ thống local UTC+7 lưu DB, vs `Instant`/JWT `iat` chuẩn UTC) — cả `AuthServiceImplTest` (mock, không gọi `LocalDateTime.now()` thật theo múi giờ hệ thống thật để so sánh) VÀ việc hoàn toàn không có `JwtAuthenticationFilterTest` khiến bug này không thể lộ ra ở tầng unit. Ngay cả khi viết test cho `AuthServiceImpl`, việc mock `Clock`/verify giá trị lưu là "UTC hay local" không tự nhiên xuất hiện trừ khi test viết CHỦ ĐÍCH giả lập máy chạy ở múi giờ UTC+7 (`TimeZone.setDefault(...)` trong test) rồi so sánh chéo với `JwtAuthenticationFilter` xử lý riêng — 2 class này không hề được test cùng nhau trong bất kỳ file nào. |
| DEFECT-017 (`useFreeze()` không có side-effect) | `UserController` / `GamificationServiceImpl` | Có (`UserControllerTest`, nested `UseFreeze`) | **Có, nhưng KHÔNG được test khai thác** | Đây là lỗi hoàn toàn logic thuần Java (method chỉ gọi lại `getLoginStreak()`, không có side-effect nào) — Mockito hoàn toàn có khả năng verify `gamificationService` KHÔNG bị gọi method nào làm thay đổi `freezesAvailable` sau `useFreeze()`. Việc bug tồn tại là do THIẾU 1 test case dạng "gọi useFreeze() 2 lần, assert freezesAvailable giảm" — không phải giới hạn kiến trúc. |
| DEFECT-018 (`getDashboardStats` thiếu check role MC trong `@PreAuthorize`) | `MCProfileService` interface (`@PreAuthorize` annotation) | Có (`MCProfileServiceImplTest`) | **Không** | Xác nhận trực tiếp: `MCProfileServiceImplTest.GetDashboardStats` gọi `service.getDashboardStats(USER_ID)` bằng Java method call thuần, hoàn toàn không đi qua Spring AOP proxy — `@PreAuthorize` là annotation chỉ có hiệu lực khi Spring Security's `MethodSecurityInterceptor` bọc quanh bean thật trong `ApplicationContext`. Mockito test gọi thẳng vào instance `new MCProfileServiceImpl(...)`, annotation trên interface hoàn toàn bị bỏ qua — không thể phát hiện SpEL sai (`#userId == authentication.name` luôn đúng, thiếu `hasAuthority('MC')`) trừ khi có `@SpringBootTest` + security context giả lập role CLIENT thật. |
| DEFECT-019 (`CertificateController` stub → 500 thay vì lỗi rõ ràng) | `CertificateServiceImpl` + `GlobalExceptionHandler` | Có (`CertificateServiceImplTest`, nested `DeprecatedStubs`) | **Partial** | `CertificateServiceImplTest.DeprecatedStubs` xác nhận đúng `UnsupportedOperationException` được ném (test PASS nghĩa là xác nhận hành vi này, không phải bug ở tầng Service). Nhưng bug thật (client nhận HTTP 500 generic thay vì mã lỗi rõ nghĩa) nằm ở việc `GlobalExceptionHandler` không có handler riêng — hoàn toàn không có `GlobalExceptionHandlerTest` trong 46 file, nên phần "exception này được Spring MVC convert thành response HTTP nào" không được unit test chạm tới. |
| DEFECT-020 (`discoverMCs(category)` bỏ qua filter) | `PublicServiceImpl.discoverMCs()` | Có (`PublicServiceImplTest`, nested `DiscoverMCs`) | **Có, nhưng KHÔNG được test khai thác** | Xác nhận trực tiếp: test case DUY NHẤT trong nested `DiscoverMCs` gọi `service.discoverMCs(null)` — không có case nào gọi với `category="WEDDING_MC"` rồi assert kết quả bị lọc đúng/sai. Đây là ví dụ rõ ràng nhất của "tham số dead code hoàn toàn nằm trong tầm với Mockito test, chỉ là chưa có ai viết case đó". |
| DEFECT-021 (Lộ email + field `verified` sai ngữ nghĩa) | `MCProfileMapper.java` (MapStruct) | Không có `MCProfileMapperTest` riêng (mapper được mock trong `PublicServiceImplTest`) | **Không** | `PublicServiceImplTest` luôn `when(mcProfileMapper.toResponseDTO(...)).thenReturn(dto)` — tức MOCK hẳn kết quả mapping, không bao giờ chạy code MapStruct sinh ra thật (`@Mapping(target = "email", source = "user.email")`). Muốn bắt bug này cần test riêng cho `MCProfileMapperImpl` (class MapStruct tự sinh lúc compile) — hiện không tồn tại trong 46 file. Về mặt kiến trúc, MapStruct mapper CÓ THỂ unit test độc lập (không cần Spring/DB), nhưng hiện tại không ai viết test này — vừa là gap kiến trúc (thiếu MapperTest layer) vừa là gap use-case. |
| DEFECT-022 (`social-posts/{id}/click` 403 cho Guest) | `SecurityConfig.java` | Không có `SecurityConfigTest` | **Không** | Cùng lý do DEFECT-001/010/013. |
| DEFECT-023 (`HttpMessageNotReadableException` → 500 thay vì 400 cho enum sai) | `GlobalExceptionHandler.java` | **Không có `GlobalExceptionHandlerTest`** | **Không** | Bug xảy ra ở bước Jackson deserialize JSON→Object THẤT BẠI trước khi vào bất kỳ Controller method nào. Unit test gọi trực tiếp method Controller bằng object Java đã dựng sẵn hợp lệ — không bao giờ mô phỏng được "JSON string chứa giá trị enum sai" vì không có bước parse JSON nào trong luồng test. Cần `@WebMvcTest` + `MockMvc.perform(post(...).content(rawInvalidJson))` để tái tạo. |
| DEFECT-024 (`RuntimeException` trần thay vì `AppException`) | `ReportServiceImpl.resolveReport()` | Có (`ReportServiceImplTest`, nested `ResolveReport`) | **Có, nhưng KHÔNG được test khai thác đúng chỗ** | Test hiện tại (dựa theo tên nested `ResolveReport`) hoàn toàn có thể assert loại exception ném ra khi `reportId` không tồn tại (`assertThrows(RuntimeException.class, ...)` — Mockito mock `findById()` trả `Optional.empty()` dễ dàng). Nếu test đã viết case này, PASS của nó chỉ xác nhận "có ném exception", không tự động cảnh báo "nhưng đây là SAI LOẠI exception theo convention dự án" — đây là giới hạn thuộc về việc unit test không kiểm tra "đúng kiểu Exception nghiệp vụ" (`AppException` cụ thể) mà assert chung chung `RuntimeException`, khiến quy ước coding convention bị lệch mà test vẫn xanh. Không phải giới hạn kiến trúc — là giới hạn độ chặt của assertion. |

### Tổng kết Mục 4

| Phân loại | Số defect | Danh sách |
|---|---|---|
| **Không** — kiến trúc unit test (Mockito, không Spring context, không Jackson thật, không filter chain thật, không DB thật) về nguyên lý không thể bắt được lớp lỗi này | **15** | 001, 002, 003, 008, 010, 011, 012, 013, 014, 015 (partial→tính vào nhóm chính vì phần cốt lõi là GlobalExceptionHandler), 016, 018, 021, 022, 023 |
| **Có, nhưng test hiện tại thiếu đúng scenario/assertion cần thiết** (nằm trong tầm với Mockito, chỉ là chưa viết case đó) | **6** | 006 (partial), 007, 017, 019 (partial), 020, 024 |
| **Có — và ĐÃ thực sự bắt được qua unit test** (bằng chứng: chính defect ghi "phát hiện khi viết unit test") | **2** | 004, 005 |
| **Partial** (tính riêng, một phần trong tầm với nhưng phần cốt lõi ngoài tầm) | **1 thêm** (009) | 009 |

**Diễn giải cho PO/dev:** khoảng **63% (15/24)** defect thuộc lớp lỗi mà **không có cách nào** để 490 unit test hiện tại (hay bất kỳ unit test Mockito nào viết thêm trong đúng kiến trúc này) phát hiện được — đây không phải là bộ test viết tệ, mà là giới hạn cố hữu của kỹ thuật "unit test cô lập, mock toàn bộ dependency": nó không đi qua Spring Security filter chain, không deserialize JSON qua Jackson thật, không chạy Spring Data derived-query engine thật, không chạy `@Async`/`@ControllerAdvice` thật. Khoảng **25% (6/24)** là gap thực sự trong bộ test (dev có công cụ, chỉ thiếu case) — nhóm này đáng lưu ý nhất vì sửa được nhanh, rẻ, ngay trong kiến trúc hiện tại. Chỉ **~8% (2/24)** minh chứng rằng viết unit test thuần Java-logic (không phụ thuộc framework) VẪN có giá trị phát hiện bug thật, đúng như kỳ vọng — 2 case này (`isTokenValid`, `updateProfile`) đều là logic tính toán/điều kiện thuần Java, không chạm framework.

---

## 5. Khuyến nghị — Loại test cần bổ sung để thu hẹp khoảng trống (nhóm "Không")

Không đề xuất code cụ thể (ngoài phạm vi QA) — chỉ nêu LOẠI kỹ thuật test cần có, để dev/PO cân nhắc bổ sung vào chiến lược test tổng thể (có thể thuộc `02-integration/` thay vì `01-unit/`, vì các loại này đều cần it nhất 1 phần Spring context thật):

1. **`@WebMvcTest` (Spring MVC slice test) cho các Controller có DTO chứa field `boolean isXxx`** — dựng `MockMvc` thật, gửi `content(rawJsonString)` với JSON string tay (không dựng object Java trực tiếp) → sẽ lộ ngay lỗi Lombok/Jackson binding (DEFECT-011) vì đi qua đúng bước deserialize thật. Ưu tiên cho `SaveCourseRequest` và rà soát toàn bộ DTO có field `is*`.

2. **Test riêng cho `SecurityConfig` bằng `@SpringBootTest(webEnvironment = RANDOM_PORT)` hoặc `@WebMvcTest` + `@AutoConfigureMockMvc`** — gọi từng endpoint public đã khai báo (hoặc parse toàn bộ `@RequestMapping` trong Controller không có `@PreAuthorize` rồi assert có whitelist tương ứng) mà KHÔNG có JWT, verify status code. Đây là cách duy nhất bắt được nhóm lỗi whitelist thiếu (DEFECT-001, 010, 013, 022 — đã lặp lại 4 lần, cho thấy cần 1 bộ test riêng cho tầng này thay vì phát hiện thủ công từng endpoint).

3. **`@DataMongoTest` (hoặc Testcontainers MongoDB) cho Repository có derived query pattern `...IsXxxTrue`/`...IsXxxFalse`** — chạy Spring Data MongoDB thật để build query, verify kết quả đúng số lượng document thật trong DB test. Đây là cách duy nhất bắt được DEFECT-012 (không thể mock ra được vì bản chất lỗi nằm trong chính cơ chế parse tên method của framework).

4. **Unit test riêng cho `MCProfileMapperImpl`/các Mapper MapStruct khác** — không cần Spring context (MapStruct sinh code Java thuần, gọi trực tiếp `new MCProfileMapperImpl().toResponseDTO(profile, user)` được), chỉ cần KHÔNG mock Mapper như hiện tại — instantiate thật rồi assert từng field JSON output. Sẽ bắt được DEFECT-021 (field `email`/`verified` map sai ngữ nghĩa) mà không cần nâng cấp lên integration test.

5. **`@SpringBootTest` với security context giả lập role cụ thể (`@WithMockUser(authorities = "CLIENT")`) cho method có `@PreAuthorize`** — bắt buộc cần Spring AOP proxy thật để `MethodSecurityInterceptor` áp dụng SpEL. Cần cho mọi Service method có `@PreAuthorize` phức tạp hơn 1 điều kiện đơn (đặc biệt các case kết hợp `or`/`and` như DEFECT-018).

6. **Test riêng cho `GlobalExceptionHandler`** — không cần Spring context đầy đủ, chỉ cần instantiate `GlobalExceptionHandler` thật và gọi trực tiếp method `@ExceptionHandler` với exception giả lập, assert HTTP status + response body đúng cho từng loại exception hiện có VÀ liệt kê được các exception type CHƯA có handler (`IllegalStateException` — DEFECT-015; `HttpMessageNotReadableException` — DEFECT-023; `UnsupportedOperationException` — DEFECT-019). Đây là 1 file test tương đối rẻ để viết và giá trị cao vì áp dụng cho toàn hệ thống, không riêng 1 module.

7. **Test riêng cho `JwtAuthenticationFilter` với `Clock`/timezone giả lập** — dựng `passwordChangedAt` bằng giờ local UTC+7 rõ ràng, JWT `iat` bằng UTC thật, verify logic so sánh — cần thiết kế class dùng `Clock` injectable thay vì `LocalDateTime.now()` trực tiếp để test được (bản thân đây cũng là 1 gợi ý về hướng fix DEFECT-016 giúp code dễ test hơn, không phải QA quyết định cách fix).

8. **Đối với nhóm "Có, nhưng thiếu scenario"** (DEFECT-006 partial, 007, 017, 019 partial, 020, 024) — không cần loại test mới, chỉ cần **bổ sung thêm test case trong đúng file/nested class đã có sẵn**, theo đúng kiến trúc Mockito hiện tại:
   - `PaymentControllerTest.CreateCourseOrder` → thêm case `effectiveAmount = 0`.
   - `LogServiceImplTest.GetLogs` → thêm case gọi với `limit` khác 200, assert kết quả bị giới hạn đúng.
   - `UserControllerTest.UseFreeze` → thêm case gọi 2 lần liên tiếp, assert `freezesAvailable` có/không đổi theo đúng kỳ vọng nghiệp vụ đã làm rõ với PO.
   - `PublicServiceImplTest.DiscoverMCs` → thêm case gọi với `category` cụ thể, assert danh sách trả về đã lọc đúng.
   - `ReportServiceImplTest.ResolveReport` → đổi assertion từ `assertThrows(RuntimeException.class, ...)` sang kiểm tra cụ thể type `AppException`/`ErrorCode` một khi dev áp dụng convention đúng.

   Nhóm này là "quả treo thấp" (low-hanging fruit) — chi phí thấp nhất, không cần thay đổi kiến trúc test, chỉ cần dev/QA (tuỳ phân công) bổ sung thêm test case vào nested class đã tồn tại sẵn.

---

*Tài liệu này được tạo bởi QA độc lập dựa trên việc đọc source code thật (`src/test/java`, `src/main/java`) và 24 file defect đã ghi nhận tại `testing/defect-log/`. Không có dòng code nào trong `src/test/java` hoặc `src/main/java` bị chỉnh sửa trong quá trình audit.*
