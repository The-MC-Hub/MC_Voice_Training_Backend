# Migration Tracking — Java → .NET

Đi kèm `MIGRATION_PLAN.md`. Cập nhật trạng thái tại đây mỗi khi 1 mục hoàn thành — đây là nguồn sự thật duy nhất về tiến độ, không suy đoán từ trí nhớ session.

**Chú thích trạng thái**: `[ ]` chưa làm · `[~]` đang làm · `[x]` xong + đạt Definition of Done (mục 4 trong MIGRATION_PLAN.md) · `[!]` bị chặn/có vấn đề cần quyết định

---

## Phase 0 — Nền tảng

- [ ] Cập nhật `CLAUDE.md` (dọn sai lệch: booking/chat domain, endpoint count, test count, CORS, rate-limit cache)
- [ ] Field-level audit 49 Model (`@Document`) — field name, type, embedded object, index
- [ ] Field-level audit 63 DTO — field name, type, nullable, JSON naming
- [ ] Đọc `AsyncConfig.java` (thread pool sizing)
- [ ] Đọc `CloudinaryConfig.java` (bean setup)
- [ ] Đọc request/response shape 2 AI endpoint (`/analyze-voice`, `/tts/stream`) trong `VoiceServiceImpl.java`
- [ ] Đọc `DataSeeder.java`, `OpenApiConfig.java`/Swagger config
- [ ] Khởi tạo solution .NET (Domain/Application/Infrastructure/Api/Tests projects)
- [ ] CI build rỗng chạy được (chưa cần logic)
- [ ] Middleware response envelope `{status,message,data}` + global exception handler
- [ ] JWT issuer/validator (claim `id`/`role`, thời hạn đúng, token phụ pending-Google 10 phút)
- [ ] MongoDB.Driver connection tới đúng Atlas cluster Java đang dùng
- [x] Connection string: dùng chung với Java, không staging DB riêng (chốt 2026-07-28, xem MIGRATION_PLAN.md mục 7)
- [ ] Setup GitHub Actions CI (build+test .NET solution mỗi push)

---

## Phase 1 — Module (theo thứ tự phụ thuộc)

### 1. Auth
- [ ] `AuthController` — 15 endpoint (register/login/OTP/Google/forgot-reset/me/referral/settings)
- [ ] `JwtService` — issue/validate, claim `id`/`role`
- [ ] `GoogleTokenVerifierService`
- [ ] Rate limit: `/auth/login`+`/auth/google` (20/15min), `/auth/register`+`/auth/google/complete-registration` (20/hour), `/auth/verify-otp`+`/auth/verify-admin-login-otp` (20/5min), per-email OTP throttle (10/10min)
- [ ] Unit test port: (đếm số `@Test` trong `AuthControllerTest`, `AuthServiceImplTest`, `JwtServiceImplTest`)
- [ ] Definition of Done đạt đủ 6 mục

### 2. User / MCProfile / ClientProfile
- [ ] `UserController` — 4 endpoint (practice-stats, streak, streak/freeze)
- [ ] `MCController` — 3 endpoint (dashboard, profile GET/PUT)
- [ ] `ClientProfileController` — 3 endpoint
- [ ] `MCProfileService`
- [ ] Unit test port: (`UserControllerTest`, `MCControllerTest`, `MCProfileServiceImplTest`)
- [ ] Definition of Done đạt đủ 6 mục

### 3. Public
- [ ] `PublicController` — 8 endpoint (landing, featured-training, mcs, mcs/{id}, search, 3 enum endpoint)
- [ ] `PublicServiceImplTest` port
- [ ] Definition of Done đạt đủ 6 mục

### 4. Booking
- [ ] `BookingController` — 5 endpoint
- [ ] `BookingDetailController` — 2 endpoint
- [ ] `AvailabilityController` — 3 endpoint
- [ ] `BookingRepository.sumPriceByMcAndStatusPaid` — port Aggregation pipeline
- [ ] Unit test port: (`BookingControllerTest`, `BookingDetailControllerTest`?, `AvailabilityControllerTest`?, `BookingServiceImplTest`)
- [ ] Definition of Done đạt đủ 6 mục

### 5. Payment (rủi ro cao — tiền thật)
- [ ] `PaymentController` — 9 endpoint (dọn business logic khỏi controller vào Command Handler đúng CQRS)
- [ ] `BookingPaymentController` — 2 endpoint
- [ ] `PayOSService` — HMAC-SHA256 signature (tạo link + verify webhook), constant-time compare
- [ ] `PaymentTransactionRepository.sumAmountByStatus` — port Aggregation
- [x] MBBank VietQR — vestigial, đã thay bằng PayOS, KHÔNG port (chốt 2026-07-28)
- [ ] Unit test port: (`PaymentControllerTest`, `PayOSServiceTest`, `PlanServiceTest`)
- [ ] **Test thủ công bắt buộc**: webhook signature verify đúng với payload thật (sandbox PayOS)
- [ ] Definition of Done đạt đủ 6 mục

### 6. Chat
- [ ] `ChatController` — 5 endpoint (đã có `assertParticipant` guard, giữ nguyên khi port)
- [ ] `ScriptCollaborativeController` — 3 endpoint + STOMP `@MessageMapping("/script.edit/{bookingId}")` → port sang `ScriptEditHub` (Hub THẬT riêng biệt, chốt 2026-07-28 — không nhầm với `/ws-chat` chính)
- [ ] `LegacyWsChatHub` — Hub rỗng riêng cho `/ws-chat` dead code (không port logic không tồn tại, tách khỏi `ScriptEditHub`)
- [ ] `handleTyping` STOMP handler → port sang `ScriptEditHub`
- [ ] Unit test port: (`ChatControllerTest`, `ChatServiceImplTest`, `ScriptCollaborativeControllerTest`)
- [ ] Definition of Done đạt đủ 6 mục

### 7. Voice Training
- [ ] `VoiceController` — 16 endpoint
- [ ] `AdaptiveCalibrationService` — port `MongoTemplate.aggregate()` thủ công (avg/min/max/percentile theo lesson)
- [ ] `VoiceLessonSearchService` + `VoiceLessonSearchRepository` — Elasticsearch multi_match, giữ đúng fallback Mongo khi ES lỗi
- [ ] AI service proxy: `/analyze-voice`, `/tts/stream` — giữ đúng request/response shape (field-level audit ở Phase 0)
- [ ] Guest cooldown logic (`GuestVoiceUsageRepository`, `SystemSettingRepository`)
- [ ] Unit test port: (`VoiceControllerTest`, `VoiceServiceImplTest` — nhiều nested class theo khảo sát)
- [ ] Definition of Done đạt đủ 6 mục

### 8. Course / Gamification
- [ ] `CourseController` — 13 endpoint
- [ ] `QuestController` — 3 endpoint
- [ ] `VoucherController` — 2 endpoint
- [ ] `CommunityController` — 4 endpoint
- [ ] `MinigameController` — 3 endpoint
- [ ] `GamificationService`, `CompetitionService`
- [ ] Unit test port: (`CourseControllerTest`, `QuestControllerTest`, `VoucherControllerTest`, `CommunityControllerTest`, `MinigameControllerTest`, `CourseServiceImplTest`, `GamificationServiceImplTest`, `CommunityServiceImplTest`, `MinigameServiceImplTest`)
- [ ] Definition of Done đạt đủ 6 mục

### 9. Notification / Favorite / Review / Report / Certificate
- [ ] `NotificationController` — 4 endpoint
- [ ] `FavoriteController` — 3 endpoint
- [ ] `ReviewController` — 3 endpoint
- [ ] `ReportController` — 7 endpoint
- [ ] `CertificateController` — 6 endpoint (dọn bypass `UserRepository` trực tiếp vào đúng layer)
- [ ] `CVController` — 3 endpoint + Supabase Storage
- [ ] `PeerReviewController` — 8 endpoint
- [ ] `QuickReplyController` — 3 endpoint
- [ ] `UserHighlightController` — 4 endpoint
- [ ] `NotificationSchedulerService` — cron `0 0 12 * * *`
- [ ] Unit test port: (`NotificationControllerTest`?, `FavoriteControllerTest`?, `ReportControllerTest`, `CertificateControllerTest`, `PeerReviewControllerTest`, `QuickReplyControllerTest`, `UserHighlightControllerTest`, `NotificationServiceImplTest`, `ReportServiceImplTest`, `CertificateServiceImplTest`)
- [ ] Definition of Done đạt đủ 6 mục

### 10. Admin (7 controller, làm cuối)
- [x] `DatabaseMigrationService`/`migrate-db` — tool one-off, KHÔNG port (chốt 2026-07-28)
- [ ] `AdminController` — 26 endpoint (dọn bypass `SystemSettingRepository` trực tiếp)
- [ ] `AdminNotificationController` — 5 endpoint
- [ ] `AdminCompetitionController` — 4 endpoint
- [ ] `AdminCaseStudyController` — 5 endpoint
- [ ] `AdminCommunityController` — 3 endpoint
- [ ] `AdminCourseController` — 7 endpoint
- [ ] `AdminPlanController` — 7 endpoint
- [ ] `AdminSocialPostController` — 5 endpoint
- [ ] `AdminSystemController` — 3 endpoint
- [ ] `AuditLogController` — 4 endpoint
- [ ] `LogController` — 3 endpoint (SSE stream `GET /stream` → `IAsyncEnumerable`/`Response.WriteAsync`, chốt 2026-07-28)
- [ ] `LogServiceImpl` — cron `0 0 3 * * *`
- [ ] `UserSanctionScheduler` — `fixedRate = 300000` (5 phút)
- [ ] Unit test port: (28 controller test file + admin test files theo khảo sát — đếm chính xác khi bắt tay)
- [ ] Definition of Done đạt đủ 6 mục

### 11. Marketing
- [ ] `EmailCampaignController` — 11 endpoint
- [ ] `AnnouncementController` — 17 endpoint
- [ ] `SocialPostController` — 2 endpoint
- [ ] `AdminSocialPostController` — đã tính ở mục 10 (Admin), không trùng lặp
- [ ] `RecommendationService` — cron `0 0 */6 * * *`
- [ ] Unit test port: (`EmailCampaignControllerTest`, `AnnouncementControllerTest`, `SocialPostControllerTest`, `EmailCampaignServiceImplTest`, `SocialPostServiceImplTest`)
- [ ] Definition of Done đạt đủ 6 mục

---

## Phase 2 — Cutover (theo từng module, chỉ sau khi Phase 1 module đó = Definition of Done)

- [ ] Auth
- [ ] User/MCProfile/ClientProfile
- [ ] Public
- [ ] Booking
- [ ] Payment
- [ ] Chat
- [ ] Voice Training
- [ ] Course/Gamification
- [ ] Notification/Favorite/Review/Report/Certificate
- [ ] Admin
- [ ] Marketing

## Phase 3 — Retire Java backend

- [ ] Toàn bộ module cutover xong + chạy ổn định (thời gian do user quyết định)
- [ ] Archive/backup Java repo trước khi retire
- [ ] Xác nhận không còn dependency nào (docs, script, CI) trỏ về Java backend

---

## Quyết định bổ sung (chốt với user 2026-07-28)

1. **SSE stream** (`LogController.GET /stream`) → port bằng `IAsyncEnumerable`/`Response.WriteAsync` native ASP.NET Core, giữ đúng `text/event-stream`, không đổi sang SignalR (giữ nguyên tắc API contract 100%).
2. **DatabaseMigrationService** (`POST /admin/migrate-db`) → tool one-off, **không port**. Giữ nguyên trong Java (Java không tắt tới hết dự án).
3. **MBBank VietQR** → đã thay hoàn toàn bằng PayOS, vestigial — **không port**. Chỉ PayOS vào scope Payment module.
4. **AI service (`TrainingAiSample`, Python FastAPI)** → ngoài scope migrate, giữ Python vĩnh viễn. .NET chỉ port phần proxy gọi ra (2 endpoint `/analyze-voice`, `/tts/stream`), không đụng code Python.
5. **.NET version** → chốt hẳn **.NET 8 LTS** (không dùng 9).
