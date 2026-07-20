# CLAUDE.md — MC_Voice_Training_Backend

Guidance for Claude Code when working in this repo. Written from a direct code audit (2026-07-20), not from assumptions — see "Divergence from The-MC-Hub-Java-Backend baseline" below for where this repo differs from the root `CLAUDE.md`'s description of the primary backend.

## What this service actually is

A **standalone voice-training + course + gamification + admin/marketing platform**, forked from `The-MC-Hub-Java-Backend`. It shares the package root `com.mchub` and general Spring Boot conventions, but **the entire MC-booking domain is absent**: no `Booking`, `Conversation`, `Message`, `Notification`, `Favorite`, `Schedule`, or `Script` models exist in this codebase. Do not assume booking-platform features apply here.

- **Java 21**, **Spring Boot 3.3.10**, MongoDB Atlas (`mchub` database, cluster `MainDatabase`), Elasticsearch (search index for voice lessons)
- **API prefix:** `/api/v1/`
- **Port:** `5000` (per `.env` `PORT`), some docs may say 8080 — check `.env` `PORT` for the actual value
- **Production:** deployed on Render (`mc-voice-training-backend.onrender.com`), free tier — **auto-sleeps after ~15min idle, wake-up takes 30-60s**. Frontend timeout must exceed this (see Frontend CLAUDE.md).

## Commands

```bash
mvn spring-boot:run      # dev server
mvn compile               # also regenerates MapStruct mappers — rerun after adding DTO fields
mvn clean package         # full build + tests
mvn test                  # run all tests
mvn test -Dtest=AuthControllerTest   # single test class
```

Env: copy `.env.example` → `.env` (`MONGODB_URI`, `MONGODB_TEST_URI`/`MONGODB_TEST_DATABASE` for QA, `JWT_SECRET`, `CLOUDINARY_*`, `PAYOS_*`, `MBBANK_*`, `AI_ANALYZE_URL`/`AI_TTS_URL`, `BREVO_SMTP_KEY`, `ELASTICSEARCH_URIS`, `ALLOWED_ORIGINS`).

**Important:** there is no Spring test profile. The app always connects using `.env`'s `MONGODB_URI` (production `mchub`), never `MONGODB_TEST_URI` automatically — the two must be switched manually if you need the backend to run against the QA database (`mchub_test`).

## Architecture — 4-layer pattern, NOT strictly followed here

The baseline rule (Controller → Service → Repository, never bypass layers) is violated in roughly a third of controllers:

- **No service layer at all** (repository accessed directly from controller): `VoucherController`, `QuestController`, `UserHighlightController`
- **Partial bypass** (injects repositories directly alongside a real service): `AuthController` (`UserRepository`, `ReferralRepository`), `CertificateController` (`UserRepository`), `AdminController` (`SystemSettingRepository`), `VoiceController` (`GuestVoiceUsageRepository`, `SystemSettingRepository`), `PaymentController` (`PaymentTransactionRepository`, `UserRepository`, `CourseRepository`, `CourseEnrollmentRepository` — plus business logic like `applyPlanUpgrade`/`grantCoursePurchase` living in the controller itself)

When touching these controllers, prefer extracting logic into a proper service rather than adding more direct repository calls — but don't block unrelated fixes on a full refactor.

### Response envelope (unchanged from baseline)

```json
{ "status": "success", "message": "...", "data": { ... } }
{ "status": "fail",    "message": "...", "data": null }
```

## REST API surface

**25 controller classes** (22 in `controllers/`, 3 in `controllers/admin/`), **~130 endpoints total**. Full list by controller:

| Controller | Base path | Auth |
|---|---|---|
| `AuthController` | `/api/v1/auth` | register/login/OTP/forgot-password public; `/me`, `/referral-code/generate` authenticated |
| `UserController` | `/api/v1/users` | all authenticated (practice stats, streak, streak-freeze) |
| `MCController` | `/api/v1/mcs` | authenticated (no explicit `@PreAuthorize`, relies on global rule) |
| `CertificateController` | `/api/v1/certificates` | create/list/delete authenticated; verify = ADMIN |
| `ReportController` | `/api/v1/reports` | create/my authenticated; admin list/resolve = ADMIN |
| `PublicController` | `/api/v1/public` | all public (landing, featured-training, MC directory, enums) |
| `LogController` | `/api/v1/admin/logs` | class-level ADMIN (SSE stream, list, ingest) |
| `SocialPostController` | `/api/v1/social-posts` | public (list + click tracking) |
| `EmailCampaignController` | `/api/v1/admin/email` | class-level ADMIN (templates, campaigns, test-send) |
| `AuditLogController` | `/api/v1/audit-logs` | class-level ADMIN |
| `ContactController` | `/api/v1/public/contact` | public |
| `MediaController` | `/api/v1/media` | authenticated upload |
| `CommunityController` | `/api/v1/community` | mostly public (stats, leaderboard, active-arenas); `/leaderboard/me` authenticated |
| `VoucherController` | `/api/v1/vouchers` | authenticated (**no service layer**) |
| `QuestController` | `/api/v1/quests` | authenticated (**no service layer**) |
| `MinigameController` | `/api/v1/minigames` | prompts/leaderboard public; submit authenticated |
| `CourseController` | `/api/v1/courses` | list/detail/reading-guides public; enroll/progress/quiz authenticated |
| `VoiceController` | `/api/v1/voice` | lessons public; practice/analyze/TTS require MC or CLIENT; admin lesson CRUD = ADMIN; `/practice/analyze-guest` public (guest trial w/ cooldown) |
| `AdminController` | `/api/v1/admin` | class-level ADMIN (dashboard, users CRUD, transactions, analytics, DB migration, guest-cooldown settings) |
| `UserHighlightController` | `/api/v1/highlights` | authenticated, manual ownership check (**no service layer**) |
| `AdminCompetitionController` | `/api/v1/admin/competitions` | class-level ADMIN |
| `AnnouncementController` | `/api/v1/admin/announcements` | class-level ADMIN (drafts, triggers for new-lesson/discount/maintenance/social-post/feature-update/competition) |
| `PaymentController` | `/api/v1/payment` | plans/flash-deals/apply-discount/webhook public; create-order/course-order authenticated; simulate-success/admin-complete = ADMIN |
| `AdminPlanController` | `/api/v1/admin/plans` | class-level ADMIN |
| `AdminCourseController` | `/api/v1/admin/courses` | class-level ADMIN |
| `AdminSocialPostController` | `/api/v1/admin/social-posts` | class-level ADMIN |

Full per-endpoint method+path list is long — grep the controller directly (`src/main/java/com/mchub/controllers/`) rather than trusting a static copy of this table if precision matters (e.g. before calling `gitnexus_impact` on a specific route).

## MongoDB collections (30 documents + 1 Elasticsearch index)

`users`, `mcprofiles`, `certificates`, `courses`, `course_enrollments`, `reading_guides`, `user_highlights`, `voice_lessons` (dual: Mongo doc + Elasticsearch index `voice_lessons` via `VoiceLessonSearchDocument`), `practice_sessions`, `lesson_adaptive_stats`, `guest_voice_usage`, `payment_transactions`, `plan_definitions`, `discount_codes`, `user_vouchers`, `competitions`, `competition_records`, `minigame_results`, `user_stats`, `announcements`, `email_templates`, `email_campaigns`, `email_logs`, `social_posts`, `reports`, `audit_logs` (class `AuditLog`), `system_logs`, `system_settings`, `otp_verifications`, `refreshtokens`, `referrals`.

No `bookings`, `conversations`, `messages`, `notifications`, `favorites`, `schedules`, or `scripts` collections exist.

## WebSocket

Single STOMP/SockJS endpoint: **`/ws-chat`** (not `/ws`), broker prefixes `/topic` + `/queue`, destination prefix `/app`, `setAllowedOriginPatterns("*")` (independent of the HTTP CORS config below).

**No chat feature is wired to it** — no `@MessageMapping` controller, no `Conversation`/`Message` model. The endpoint is configured but unused; don't assume a working chat system exists.

## CORS (`SecurityConfig.java`)

- Base allow-list from property `mchub.cors.allowed-origins` (env `ALLOWED_ORIGINS`), default `http://localhost:5173`
- **Always appends `https://*.vercel.app`** as a wildcard pattern on top of whatever's configured (added 2026-07-20 to handle Vercel preview-deploy domains, which change per deploy and can't be individually whitelisted)
- Uses `setAllowedOriginPatterns` (supports wildcards), methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers `Authorization`/`Content-Type`/`Accept`/`X-Requested-With`, credentials allowed

## Rate limiting (`RateLimitFilter.java`)

Bucket4j, per-IP, **POST-only**, in-memory `ConcurrentHashMap` (never evicted — grows for app lifetime, acceptable at current scale but worth knowing):

- `/auth/login` → 20 req / 15 min
- `/auth/verify-otp`, `/auth/verify-admin-login-otp` → 20 req / 5 min (shared bucket)
- `/auth/register`, `/auth/resend-otp` → 20 req / hour (shared bucket)
- Everything else (including all GETs) is unthrottled by this filter
- IP extraction trusts `X-Forwarded-For` if present — spoofable without a trusted reverse proxy stripping it; acceptable behind Render's proxy, not if exposed directly

## Testing

`src/test/java/com/mchub/` — 46 files, ~493 `@Test` methods. **Pure Mockito unit/slice tests** (`@MockBean` mocking `Service`/`Repository` layers) — despite `de.flapdoodle.embed.mongo.spring30x` being declared in `pom.xml`, **no test actually uses embedded MongoDB**. If you see a claim that "tests use embedded MongoDB, no real DB connection needed," that's describing the primary backend's baseline, not this repo — verify before relying on it.

## Key dependencies (`pom.xml`)

Spring Boot 3.3.10 parent · `spring-security` · `jjwt` 0.12.5 (JWT) · `spring-data-mongodb` · `spring-data-elasticsearch` · `spring-boot-starter-mail` · `spring-boot-starter-websocket` · `springdoc-openapi` 2.5.0 (Swagger at `/swagger-ui/index.html`, ADMIN-only) · Lombok 1.18.36 · Cloudinary 1.36.0 · `dotenv-java` 3.0.0 · MapStruct 1.5.5 · Bucket4j 8.10.1.

## Feature areas present in code

- **Auth**: register/login/OTP verify/forgot-reset password/admin 2FA OTP, referral codes, refresh tokens
- **Payment**: PayOS integration (own implementation, separate from the primary backend's), webhook HMAC verification, plan subscriptions + single-course purchases, discount codes, flash deals
- **Voice training**: lesson library, practice sessions with adaptive difficulty stats, guest trial with cooldown, AI proxy (`/voice/proxy/analyze-voice`) to an external FastAPI service (see AI service repos below), TTS generation proxy
- **Courses/Academy**: course catalog, enrollment, reading guides with highlight annotations, quizzes, milestones/roadmap
- **Community/Gamification**: leaderboards (streak/diligent/precision/session-count), competitions/arenas, minigames (speed-reader), quests, vouchers, login-streak-freeze mechanic
- **Admin**: dashboard stats, user management, transaction/revenue views, analytics, growth-analytics, DB migration tool, audit logs, live SSE log stream
- **Marketing**: email campaign builder + bulk send with recipient targeting, trigger-based announcement drafts (new lesson / discount / maintenance / social post / feature update / competition), social post feed

## AI service integration

`AI_ANALYZE_URL` / `AI_TTS_URL` in `.env` point at an external Python FastAPI service (Hugging Face Space or Render, see `HF-Space-Deploy`/`TrainingAiSample` repos). `VoiceController`'s `/proxy/analyze-voice` and `/tts/generate` forward to it. That service being asleep/cold-starting is a common source of apparent backend failures — check it independently before assuming a Java-side bug.

## Divergence from `The-MC-Hub-Java-Backend` baseline (root CLAUDE.md)

If you've read the root `d:\ProjectCode\TheMCHub\CLAUDE.md`, note these differences specific to this fork:

- No booking-platform domain at all (see "What this service actually is" above)
- WebSocket endpoint is `/ws-chat`, not `/ws`; no chat feature is actually implemented despite the endpoint existing
- 4-layer architecture is not strictly enforced (3 controllers have zero service layer)
- Active Elasticsearch integration (`VoiceLessonSearchDocument`), not just docker-compose infra
- Independent PayOS implementation
- Test suite uses Mockito mocks exclusively; the declared embedded-MongoDB dependency is vestigial
