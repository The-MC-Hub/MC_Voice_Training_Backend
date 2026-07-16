# Báo cáo Clean Code & Audit — 17+ Module còn lại

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Phạm vi | Email, Community, Announcement, Quest, Report, Certificate, Minigame, Gamification, Social, Voucher, MC Profile, Media, Log, Audit, Public, User, Contact, Highlight, AdminCompetition, JwtService |
| Ngày kiểm thử | 2026-07-16 |
| Người thực hiện | Senior Backend Engineer kiêm QA Engineer (AI-assisted, hỗ trợ bởi subagent khảo sát) |
| Phương pháp | Đọc toàn bộ Service+Controller mỗi module, xác nhận bug bằng đối chiếu code thật, verify bằng `mvn compile` |
| Khác biệt so với 5 module chính (Auth/Voice/Course/Admin/Payment) | Theo phạm vi đã thống nhất với user: các module này KHÔNG viết test case đầy đủ Equivalence Partitioning/Boundary/Negative dạng bảng — chỉ audit clean code + phát hiện bug thật + sửa nếu nghiêm trọng, báo cáo gộp |

## 2. Bug thật đã sửa (5 bug nghiêm trọng)

### 2.1. `UserHighlightController` — IDOR / Broken Access Control (nghiêm trọng nhất)

**Trước:** Không có bất kỳ kiểm tra quyền sở hữu nào:
- `getHighlights(guideId, userId)` — nhận `userId` trực tiếp từ URL path, không đối chiếu với người dùng đang đăng nhập → bất kỳ ai đăng nhập được (hoặc thậm chí không cần đăng nhập nếu route không bị chặn ở tầng Security) đều có thể đọc highlight/ghi chú cá nhân của người khác chỉ bằng cách đổi `userId` trong URL.
- `createHighlight` — nhận `userId` từ body JSON do client tự gửi, không gán từ JWT → có thể tạo highlight giả mạo dưới tên người khác.
- `updateHighlight`/`deleteHighlight` — chỉ dùng `id` (ID của highlight), không kiểm tra highlight đó có thuộc về người gọi hay không → bất kỳ ai đoán/biết được ID có thể sửa/xóa ghi chú của người khác.

**Sau:** 
- `getHighlights(guideId)` — bỏ `userId` khỏi path, lấy từ `SecurityUtils.getCurrentUserId()`.
- `createHighlight` — ép `highlight.setUserId(userId)` từ JWT, bỏ qua giá trị client gửi lên.
- `updateHighlight`/`deleteHighlight` — thêm check `userId.equals(highlight.getUserId())`, nếu không khớp throw `AppException(ErrorCode.ACCESS_DENIED)`.
- Đổi từ `@Autowired` field injection sang `@RequiredArgsConstructor` (nhất quán style dự án).
- Đổi từ `catch (Exception e) → HTTP 500 generic` sang dùng `AppException`/`ErrorCode` (404 khi không tìm thấy, 403 khi không có quyền) — đúng response envelope chuẩn của dự án.

**Lưu ý breaking change:** URL của `getHighlights` đổi từ `/api/v1/highlights/reading-guides/{guideId}/users/{userId}` thành `/api/v1/highlights/reading-guides/{guideId}`. Đã kiểm tra sơ bộ không thấy tham chiếu cứng tới path cũ trong frontend qua tìm kiếm nhanh, nhưng **cần frontend team xác nhận và cập nhật lời gọi API tương ứng** trước khi deploy.

### 2.2. `JwtServiceImpl.isTokenValid()` — NullPointerException với token cũ/hỏng

**Trước:** `extractedId.equals(userId)` — nếu token không có claim `"id"` (token cũ trước khi field này được thêm, hoặc token bị giả mạo thiếu claim), `extractedId` là `null` → gọi `.equals()` trên `null` ném `NullPointerException` thay vì trả về `false` một cách an toàn.

**Sau:** `java.util.Objects.equals(extractedId, userId)` — null-safe, trả `false` đúng nghĩa "token không hợp lệ" thay vì crash.

### 2.3. `PublicServiceImpl.getFeaturedMCTrainingStats()` — DB call trong loop

**Trước:** Vòng lặp `for (MCProfile profile : profiles)` gọi `practiceSessionRepository.findByUserId(uId)` cho TỪNG profile — vi phạm trực tiếp quy tắc performance bắt buộc của dự án ("No DB calls inside loops → batch-fetch với `findAllById(ids)`"). Với N MC profile, tốn N query DB thay vì 1.

**Sau:** Thêm method `findByUserIdIn(List<String>)` vào `PracticeSessionRepository`, batch-fetch toàn bộ session của tất cả user 1 lần trước vòng lặp, group theo `userId` bằng `Collectors.groupingBy`, vòng lặp chỉ đọc từ map trong bộ nhớ.

### 2.4. `MCProfileServiceImpl.getDashboardStats()` — Duplicate DB round-trip

**Trước:** Hai `CompletableFuture` riêng biệt (`avgAccuracyFuture`, `avgWpmFuture`) đều gọi `practiceSessionRepository.findByUserId(userId)` — cùng 1 dữ liệu bị fetch 2 lần song song, lãng phí round-trip DB không cần thiết.

**Sau:** Gộp thành 1 `CompletableFuture` fetch 1 lần, tính cả `avgAccuracy` và `avgWpm` từ cùng 1 danh sách session trong bộ nhớ.

### 2.5. `AdminCompetitionController` — Bỏ qua tầng Service (vi phạm kiến trúc 4-tầng bắt buộc)

**Trước:** Controller inject thẳng `CompetitionRepository`, gọi trực tiếp `findAll()`/`save()`/`deleteById()` — vi phạm rule CLAUDE.md "Strict 4-layer Spring Boot pattern — never bypass layers". Ngoài ra `updateCompetition` dùng `IllegalArgumentException` thay vì `AppException`, và `deleteCompetition` không kiểm tra tồn tại trước khi xóa (Spring Data `deleteById` im lặng no-op nếu ID không tồn tại, khiến client tưởng xóa thành công dù ID sai).

**Sau:** Tạo mới `services/CompetitionService.java` (thin service, theo đúng pattern các service khác trong dự án), chuyển toàn bộ logic từ Controller sang đây, dùng `AppException(ErrorCode.RESOURCE_NOT_FOUND)` nhất quán, thêm check `existsById` trước khi xóa để trả lỗi rõ ràng thay vì no-op im lặng. Controller giờ chỉ gọi Service, đúng kiến trúc 4 tầng.

## 3. Vấn đề ghi nhận — chưa sửa, chờ quyết định (ưu tiên thấp hơn)

| # | Module | Vấn đề | Mức độ |
|---|---|---|---|
| 1 | `AnnouncementService` | 2 overload `approveAndSend(id, recipientIds)` và `approveAndSend(id)` trùng lặp ~35 dòng logic gửi thông báo gần như y hệt; overload 1 tham số có thể là dead code (cần xác nhận có nơi nào gọi không trước khi xóa) | Style/DRY |
| 2 | `ReportServiceImpl` | Dòng ~37 dùng `new RuntimeException("Report does not exist")` thay vì `AppException(ErrorCode.RESOURCE_NOT_FOUND, ...)` — không nhất quán response envelope | Style |
| 3 | `SocialPostServiceImpl` | 4 vị trí (dòng ~47, 59, 67, 75) cùng dùng `new RuntimeException("Social post not found...")` thay vì `AppException` — cùng loại vấn đề như #2, lặp lại 4 lần trong 1 file | Style |
| 4 | `EmailCampaignServiceImpl` | Dòng ~114 bọc exception gửi mail bằng `RuntimeException` thô thay vì `AppException` | Style |
| 5 | `GamificationServiceImpl` | Race condition lý thuyết: khi login đúng lúc giao thời tháng mới, thứ tự "refill freeze" và "check gap==2 dùng freeze" có thể phụ thuộc thứ tự thực thi trong cùng 1 lần gọi — rủi ro thấp, cần nhiều điều kiện trùng hợp mới xảy ra | Note (edge case hiếm) |
| 6 | `QuestController.claimVoucher` | Thao tác đọc-sửa-ghi qua 3 collection không bọc transaction (MongoDB standalone không hỗ trợ transaction ACID đầy đủ nếu không phải replica set) — có khe hở lý thuyết cho 2 request đồng thời cùng vượt qua check "chưa claim" — rủi ro thấp vì voucher giá trị nhỏ | Note |
| 7 | `UserController.useFreeze` | Tên endpoint gây hiểu nhầm — thực chất chỉ hiển thị số freeze còn lại, không tiêu freeze (freeze được tiêu ở `GamificationServiceImpl.processLoginStreak` khi có gap ngày) | Note (naming) |

## 4. Module đã audit và xác nhận sạch (không cần sửa)

`EmailService` (template builder, tự escape HTML đúng), `CommunityServiceImpl`/`CommunityController`, `QuestController` (ownership check tốt qua `SecurityUtils`), `CertificateServiceImpl`/`CertificateController`, `MinigameServiceImpl`/`MinigameController` (ownership + bounded input tốt), `VoucherController`, `MediaService`/`MediaController`, `LogServiceImpl`/`LogController`, `AuditLogServiceImpl`/`AuditLogController`, `ContactController`, `VoiceLessonSearchService`.

## 5. Xác minh build

Toàn bộ 5 bug đã sửa được verify bằng `mvn -q compile -DskipTests` — compile sạch sau mỗi lần sửa, không có lỗi thật (chỉ có lombok IDE annotation processor warning đã biết từ trước, không phải lỗi build thật).

## 6. Giới hạn

- Không thể chạy embedded MongoDB integration test thật (môi trường thiếu binary phù hợp) — toàn bộ xác nhận dựa trên trace logic thủ công + biên dịch thành công.
- Do khối lượng lớn (20 module), các module này KHÔNG có tài liệu test case Equivalence Partitioning/Boundary/Negative đầy đủ dạng bảng như 5 module chính (Auth/Voice/Course/Admin/Payment) — theo đúng phạm vi đã thống nhất với user ("test đầy đủ chỉ áp dụng Service+Controller của module ưu tiên, còn lại audit + báo cáo gộp").
- `getFeaturedMCTrainingStats`/`getDashboardStats` sau khi sửa CHƯA được đo lại hiệu năng thực tế (cần môi trường có dữ liệu thật + DB thật để benchmark trước/sau).
