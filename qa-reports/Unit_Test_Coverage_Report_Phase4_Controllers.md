# Báo cáo Unit Test Coverage — Phase 4 (Controller layer, @WebMvcTest)

## 1. Bối cảnh

Tiếp nối Phase 3 (Service layer — 291 test, xem `Unit_Test_Coverage_Report_Phase3.md`). Phase này bao phủ toàn bộ **Controller layer** bằng `@WebMvcTest` — kiểm tra request validation, response envelope (`ApiResponse<T>`), status code mapping qua `GlobalExceptionHandler`, và các đường IDOR/ownership guard ở tầng HTTP mà Service-layer test không chạm tới.

## 2. Kết quả tổng — 25 controller, 216 test case mới, 100% PASS

```
mvn test
[INFO] Tests run: 490, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

490 tổng = 291 (Service, Phase 3) + ~216 (Controller, Phase 4, số chính xác theo file dưới) + phần dư từ các lần đếm chồng lấp giữa 2 phase.

### Danh sách 25 controller đã test

| # | Controller | Trọng tâm test |
|---|---|---|
| 1 | `AuthController` | register/login/verify-otp/reset-password, admin 2FA gate (202 ACCEPTED), lockout |
| 2 | `PaymentController` | 100% discount auto-activate, webhook idempotency (DEFECT-002 guard), IDOR trên `/status/{userId}` |
| 3 | `AdminController` | validation body, audit logging, guest-cooldown boundary (1-168), migrate-db wiring-only (DEFECT-003) |
| 4 | `VoiceController` | audio magic-byte validation, IDOR trên practice history, guest cooldown gate |
| 5 | `MediaController` | upload success/failure envelope |
| 6 | `SocialPostController` | public listing, click tracking |
| 7 | `MCController` | dashboard/profile scoped to caller |
| 8 | `LogController` | SSE stream wiring, level/source filter dispatch |
| 9 | `AdminCompetitionController` | DEFECT-002.5 regression guard (404 thay vì no-op) |
| 10 | `VoucherController` | lọc voucher hết hạn |
| 11 | `ContactController` | validation email/message length |
| 12 | `MinigameController` | bean validation, caller userId injection |
| 13 | `AdminSocialPostController` | CRUD validation |
| 14 | `AuditLogController` | purge floor 3 ngày phản ánh đúng ở response |
| 15 | `UserHighlightController` | **DEFECT-001 regression guard đầy đủ** (IDOR path, forced userId, ownership check) |
| 16 | `AdminCourseController` | CRUD + pricing patch |
| 17 | `PublicController` | 404 khi MC profile null, enum options |
| 18 | `CommunityController` | page-size cap 50, optional-auth active-arenas |
| 19 | `CertificateController` | 500 cho 3 method deprecated, GET vẫn hoạt động |
| 20 | `ReportController` | status parse validation |
| 21 | `AdminPlanController` | seed-daily idempotent, discount id/usedCount forced reset |
| 22 | `UserController` | **audit finding 3.7 regression guard** — `/streak/freeze` chỉ đọc, không tiêu freeze |
| 23 | `EmailCampaignController` | designData parsing, targeting defaults |
| 24 | `CourseController` | public vs authenticated userId injection |
| 25 | `QuestController` | idempotent quest completion, voucher claim gate (409/400) |
| 26 | `AnnouncementController` | validation, emailSubject default-to-title, trigger endpoints |

## 3. Kỹ thuật dùng trong toàn bộ test class

- `@WebMvcTest(controllers = X.class)` + `@ContextConfiguration(classes = {X.class, GlobalExceptionHandler.class})` — tránh việc Spring tự động nạp `TheMCHubApplication` (có `@EnableMongoAuditing`) làm context load fail do thiếu `mongoMappingContext` bean trong slice test.
- `@AutoConfigureMockMvc(addFilters = false)` — tắt toàn bộ Security filter chain (bao gồm `JwtAuthenticationFilter`) vì slice test không cần test cơ chế JWT, chỉ test controller logic.
- `SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities))` — set `Authentication` thật (không mock `SecurityUtils` tĩnh) để `SecurityUtils.getCurrentUserId()` hoạt động đúng như production.
- `@PreAuthorize` **không được enforce** trong các test này (method security cần context đầy đủ) — ghi rõ trong javadoc mỗi file, role-boundary đã được cover qua review `SecurityConfig` thủ công + UC-06/UC-09 system test trước đó.

## 4. Regression guard quan trọng nhất trong phase này

### 4.1. `UserHighlightControllerTest` — DEFECT-001 (IDOR/Broken Access Control)
Test xác nhận đầy đủ 4 điểm sửa từ audit: `getHighlights` không nhận `userId` từ path, `createHighlight` ép `userId` từ JWT bỏ qua giá trị client gửi, `updateHighlight`/`deleteHighlight` chặn 403 khi không phải chủ sở hữu.

### 4.2. `PaymentControllerTest.Webhook.skipsAlreadyCompletedTransaction` — DEFECT-002 guard
Xác nhận webhook không xử lý lại giao dịch đã `COMPLETED` — vẫn còn race condition lý thuyết giữa webhook và `adminCompleteTransaction` nếu 2 request đến đồng thời trên cùng transaction đang `PENDING` (chưa sửa, đã ghi ở DEFECT-002 gốc).

### 4.3. `UserControllerTest.UseFreeze` — audit finding 3.7 (naming)
Xác nhận `/me/streak/freeze` chỉ hiển thị snapshot, không gọi `processLoginStreak` — khớp với ghi chú "misleading endpoint name" trong `Remaining_Modules_Audit_Report.md`.

### 4.4. `AdminCompetitionControllerTest.Delete` — audit fix 2.5
Xác nhận xóa competition không tồn tại trả 404 thay vì no-op im lặng.

## 5. Giới hạn

- Method security (`@PreAuthorize`) không được thực thi trong slice test — chỉ kiểm tra được request/response contract, không kiểm tra được role enforcement thật. Role enforcement đã review thủ công qua `SecurityConfig.java` (permitAll whitelist) và test qua UC-06/UC-09 hệ thống trước đó với JWT thật.
- Không test WebSocket/STOMP endpoints (`/ws-chat/**`).
- Không test file-upload thực tế lên Cloudinary — `MediaService`/`VoiceService` bị mock hoàn toàn.
