# Báo cáo Unit Test Coverage — Phase 3 (viết test code thật vào src/test/)

## 1. Bối cảnh

Theo yêu cầu của user ("Tôi cần viết các code test thành các file test trong src/test/ chứ không chỉ test đơn giản qua console"), toàn bộ phase này chuyển từ thực thi test thủ công (curl + mongosh, đã ghi trong `testing/03-system/`) sang viết **JUnit 5 + Mockito test thật**, chạy độc lập không cần MongoDB/SMTP/AI service thật.

Lý do kỹ thuật: embedded MongoDB (`de.flapdoodle`) không hoạt động được trên môi trường Windows này (đã ghi nhận từ đầu dự án, xem `testing/testing.md` mục 7) — nên toàn bộ test dùng Mockito mock repository/service, không kết nối DB thật.

## 2. Kết quả tổng — 20 test class, 291 test case, 100% PASS

```
mvn test
[INFO] Tests run: 291, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

| # | Test class | Module | Số test | Kết quả |
|---|---|---|---|---|
| 1 | `AdminServiceImplTest` | Admin | 22 | PASS |
| 2 | `PayOSServiceTest` | Payment (PayOS) | 10 | PASS |
| 3 | `CourseServiceImplTest` | Course | 15 | PASS |
| 4 | `JwtServiceImplTest` | Auth (JwtService) | 7 | PASS |
| 5 | `VoiceServiceImplTest` | Voice Training (core) | 23 | PASS |
| 6 | `MCProfileServiceImplTest` | MC Profile / Dashboard | 8 | PASS |
| 7 | `AuthServiceImplTest` | Auth (AuthService) | 33 | PASS |
| 8 | `PlanServiceTest` | Payment (Plan/Discount) | 24 | PASS |
| 9 | `GamificationServiceImplTest` | Gamification | 27 | PASS |
| 10 | `CompetitionServiceTest` | Competition (Admin) | 7 | PASS |
| 11 | `AuditLogServiceImplTest` | Audit Log | 9 | PASS |
| 12 | `LogServiceImplTest` | System Log (SSE/ingest) | 9 | PASS |
| 13 | `PublicServiceImplTest` | Public (landing/discover) | 11 | PASS |
| 14 | `EmailCampaignServiceImplTest` | Email Campaign | 14 | PASS |
| 15 | `ReportServiceImplTest` | Report | 6 | PASS |
| 16 | `SocialPostServiceImplTest` | Social Post | 10 | PASS |
| 17 | `MinigameServiceImplTest` | Minigame (Speed Reader) | 18 | PASS |
| 18 | `CertificateServiceImplTest` | Certificate (deprecated) | 5 | PASS |
| 19 | `CommunityServiceImplTest` | Community / Leaderboard | 16 | PASS |
| 20 | `AnnouncementServiceTest` | Announcement | 17 | PASS |

(Tổng theo file test ở trên đã khớp `mvn test` — 291 test tổng.)

## 2.1. Phase 3b — 11 module còn lại (tiếp nối tuần tự, cùng ngày)

Toàn bộ 11 module còn thiếu test đã liệt kê ở mục 5 (bản trước) nay đã có JUnit coverage đầy đủ: `CompetitionService`, `AuditLogServiceImpl`, `LogServiceImpl`, `PublicServiceImpl`, `EmailCampaignServiceImpl`, `ReportServiceImpl`, `SocialPostServiceImpl`, `MinigameServiceImpl`, `CertificateServiceImpl`, `CommunityServiceImpl`, `AnnouncementService`. Không phát hiện bug nghiêm trọng mới (khác Phase 3a) — các module này đã được audit sạch ở Phase 2 (`Remaining_Modules_Audit_Report.md`), test JUnit ở đây chủ yếu là regression-guard cho các fix đã áp dụng và xác nhận lại các finding style/DRY đã ghi nhận trước đó (ví dụ `RuntimeException` thay vì `AppException` ở `ReportServiceImpl`/`SocialPostServiceImpl` — test cố tình assert đúng hành vi hiện tại, không "sửa hộ" bằng cách đổi expectation).

## 3. Defect mới phát hiện trong phase này (viết test tự nhiên lộ ra, không phải chủ đích tìm bug)

### DEFECT-005 (Major/P1) — `MCProfileServiceImpl.updateProfile()` xóa âm thầm `personality`/`hostingStyle`

Phát hiện khi viết `MCProfileServiceImplTest`. Root cause: 2 field String này default `""` (không phải `null`) trong model `MCProfile`, nhưng guard trong `updateProfile()` chỉ check `!= null` — nên bất kỳ partial-update payload nào dựng qua `new MCProfile()` (không set 2 field này) sẽ vô tình ghi đè chúng thành rỗng. Đã ghi chi tiết đầy đủ tại `testing/defect-log/DEFECT-005.md`.

**Đây là defect thứ 5 của toàn bộ audit — nâng tổng số bug thật đã tìm thấy qua QA process (audit đọc code + test JUnit) lên 5, cộng thêm 5 vấn đề style/DRY chưa sửa (xem `Remaining_Modules_Audit_Report.md` mục 3).**

## 4. Trọng tâm coverage theo module

- **AuthServiceImpl** (33 test): brute-force lockout (10 lần sai → khóa 15 phút), admin 2FA OTP gate (chặn JWT tới khi verify OTP), OTP attempt-limit (3 lần cho admin login, 5 lần cho reset password/email verify), register dedup (email đã verify → chặn; email chưa verify → dọn dẹp & cho đăng ký lại), password legacy plaintext → tự nâng cấp bcrypt.
- **VoiceServiceImpl** (23 test): plan-expiry auto-downgrade về FREE, session-limit theo từng plan (FREE/DAILY/BASIC/FULL), aiSessionsUsed increment logic khác nhau theo plan, gamification/adaptive-calibration fire-and-forget không làm fail request chính khi lỗi.
- **PlanService** (24 test): toàn bộ chuỗi validate mã giảm giá (`applyDiscount`) — hết hạn, hết lượt, không active, không áp dụng cho plan — khớp với các trường hợp đã test thủ công ở UC-06; flash-deal filter logic (time window, active, remaining uses).
- **GamificationServiceImpl** (27 test): streak luyện tập (gap 0/1/>1 ngày), streak đăng nhập + cơ chế "freeze" (gap 2 ngày dùng freeze, gap 3+ luôn reset dù còn freeze), badge idempotency (không trao trùng), tier calculation theo ngưỡng XP, competition record fire-and-forget (lỗi competition không làm mất gamification stats).
- **MCProfileServiceImpl** (8 test): xác nhận fix hiệu năng đã ghi trong audit (chỉ fetch practice session 1 lần cho dashboard stats, không 2 lần song song), đồng thời phát hiện DEFECT-005 nêu trên.

## 4.1. Trọng tâm coverage — Phase 3b (11 module bổ sung)

- **CompetitionService** (7 test): regression guard cho fix kiến trúc 4-tầng (audit 2.5) — `deleteCompetition` throw `RESOURCE_NOT_FOUND` thay vì no-op im lặng khi id không tồn tại.
- **AuditLogServiceImpl** (9 test): `purgeLogs()` 3-ngày safety floor (đầu vào < 3 luôn bị ép về 3, đầu vào > 3 giữ nguyên), client IP resolution (X-Forwarded-For ưu tiên, fallback remoteAddr, "unknown" khi request null).
- **LogServiceImpl** (9 test): dispatch 4 nhánh của `getLogs()` theo level/source, `ingestExternal()` luôn force source="AI" bất kể input, TTL cleanup 7 ngày.
- **PublicServiceImpl** (11 test): regression guard cho fix N+1 query (audit 2.3) — xác nhận `findByUserIdIn()` chỉ gọi đúng 1 lần bất kể số lượng MC profile.
- **EmailCampaignServiceImpl** (14 test): dedup người nhận theo email case-insensitive, dispatch 5 kiểu targeting (PLAN/ROLE/PREMIUM/CUSTOM/ALL).
- **ReportServiceImpl** (6 test): xác nhận finding style đã ghi trong audit — `resolveReport()` throw raw `RuntimeException` thay vì `AppException` cho id không tồn tại (test assert đúng hành vi lỗi hiện tại, không sửa).
- **SocialPostServiceImpl** (10 test): tương tự — 4 vị trí throw raw `RuntimeException` (audit 3.3), toggle/click-count logic.
- **MinigameServiceImpl** (18 test): công thức tính XP/score cho Speed Reader minigame, clamp round-count, leaderboard aggregation (MongoTemplate mock) với fallback "Ẩn danh" khi thiếu user.
- **CertificateServiceImpl** (5 test): service deprecated — 3/4 method cố ý throw `UnsupportedOperationException`, chỉ `getCertificatesByMCProfile` còn hoạt động (repurposed thành userId lookup).
- **CommunityServiceImpl** (16 test): dispatch loại sort leaderboard (streak/precision/sessions/hours/weekly), rank offset theo page, sort arena leaderboard theo accuracy+rhythm.
- **AnnouncementService** (17 test): 2 overload `approveAndSend` (audit 3.1, đã ghi nhận trùng lặp logic — test xác nhận cả 2 đường đều hoạt động đúng, không gộp code), độ ưu tiên resolve người nhận (explicit > saved > targetPlans), chặn sửa/xóa announcement đã SENT.

## 5. Giới hạn

- Không test tầng Controller (`@WebMvcTest`) — chỉ Service layer, theo đúng pattern đã thống nhất từ đầu (Mockito thuần, không Spring context).
- `DatabaseMigrationService` cố tình KHÔNG viết test/thực thi — do DEFECT-003 (hardcode production DB name), rủi ro quá cao để chạy kể cả trong môi trường test.
- Toàn bộ Service layer trong `com.mchub.services`/`com.mchub.services.impl` nay đã có JUnit coverage, ngoại trừ `DatabaseMigrationService` (cố ý loại trừ) và `VoiceLessonSearchService`/`MediaService`/`AdaptiveCalibrationService` (đã audit sạch ở Phase 2, wrapper mỏng quanh Elasticsearch/Cloudinary/external call, giá trị test thấp so với effort).
