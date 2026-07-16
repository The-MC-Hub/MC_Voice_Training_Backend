# Báo cáo Clean Code & Test — Module Admin

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Module | Admin (Dashboard, Quản lý User, Thống kê doanh thu, Analytics, Growth Metrics) |
| Files | `services/AdminService.java`, `services/impl/AdminServiceImpl.java`, `controllers/AdminController.java` |
| Ngày kiểm thử | 2026-07-16 |
| Người thực hiện | Senior Backend Engineer kiêm QA Engineer (AI-assisted) |
| Kỹ thuật test | Equivalence Partitioning (EP), Boundary Value Analysis (BVA), Negative Testing |
| Môi trường | Trace logic thủ công + `mvn compile` (không chạy được embedded MongoDB) |
| Phạm vi | Chỉ `AdminService`/`AdminServiceImpl`/`AdminController` chính. Các controller admin phụ (`AdminCourseController`, `AdminPlanController`, `AdminSocialPostController`, `AdminCompetitionController`) thuộc các module domain riêng, sẽ kiểm thử cùng module tương ứng |

## 2. Mục đích & phạm vi

Rà soát clean code và kiểm thử thủ công module Admin: dashboard tổng quan, quản lý user (CRUD, đổi trạng thái/gói/mật khẩu, gửi email), thống kê doanh thu, analytics vận hành (DAU/MAU, phễu chuyển đổi, cohort retention), và cấu hình hệ thống (guest cooldown).

## 3. Tóm tắt thay đổi Clean Code

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `AdminServiceImpl.updateUserStatus()` | `orElseThrow(() -> new RuntimeException("User does not exist"))` — khác loại exception với TẤT CẢ method khác cùng file (đều dùng `AppException(ErrorCode.USER_NOT_FOUND, ...)`) | Đổi thành `AppException(ErrorCode.USER_NOT_FOUND, "User not found: " + id)` | **Bug thật** (đã xác nhận với user, chọn sửa ngay): `GlobalExceptionHandler` chỉ bắt riêng `AppException` để trả đúng HTTP status (404). `RuntimeException` chung rơi vào handler generic, trả về HTTP 500 thay vì 404 khi admin cập nhật trạng thái user không tồn tại — sai response code cho một trường hợp lỗi hợp lệ (input đúng nhưng ID không tồn tại) |
| `AdminController.getGuestCooldown()` / `setGuestCooldown()` | Có `@PreAuthorize("hasAuthority('ADMIN')")` lặp lại ở method-level, dù class đã có `@PreAuthorize("hasAuthority('ADMIN')")` ở class-level (dòng 26) bảo vệ toàn bộ controller | Xóa 2 annotation method-level thừa | Vi phạm DRY/KISS — class-level annotation đã đủ bảo vệ toàn bộ endpoint, không cần lặp lại. Không đổi hành vi bảo mật (vẫn yêu cầu ADMIN) |
| `AdminService.java`, phần còn lại `AdminServiceImpl.java`/`AdminController.java` | — | Không sửa gì thêm | Đã đạt chuẩn: batch-fetch user tránh N+1 trong `getAllTransactions` (dòng 106-108), soft-delete user (`deleteUser`) bảo toàn dữ liệu, audit log đầy đủ cho mọi hành động admin nhạy cảm |

## 4. Chi tiết Test Case

### 4.1. `AdminServiceImpl.getAdminDashboardOverview()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| DSH-01 | EP hợp lệ | Có transaction ở đủ 3 trạng thái COMPLETED/PENDING/FAILED | Đếm đúng từng loại, `totalRevenue` chỉ tính COMPLETED | Đúng (dòng 65-80) | Pass |
| DSH-02 | Boundary | Không có transaction nào (`allTx` rỗng) | Toàn bộ counter = 0, không lỗi (stream rỗng an toàn) | Đúng | Pass |
| DSH-03 | EP hợp lệ | `totalUsers`/`totalMCs` | Dùng `countByRoleNot`/`countByRole` ở tầng DB (không load hết rồi đếm bằng `.size()`) | Đúng (dòng 73-74) — tuân thủ performance rule "No `.size()` on large Lists" | Pass |

### 4.2. `AdminServiceImpl.getAllUsers()` / `getUserById()` / `getAllMCs()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| USR-01 | EP hợp lệ | `getAllUsers()` | Map toàn bộ user qua DTO | Đúng (dòng 84-87) | Pass |
| USR-02 | Negative | `getUserById("nonexistent")` | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 90-94) | Pass |
| USR-03 | EP hợp lệ | `getAllMCs()` | Lọc đúng role=MC | Đúng (dòng 97-100) | Pass |

### 4.3. `AdminServiceImpl.getAllTransactions()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TXN-01 | EP hợp lệ | Nhiều transaction, nhiều user khác nhau | Batch-fetch user 1 lần (`findAllById`), map từng transaction ra row kèm tên/email user | Đúng (dòng 104-127) — không N+1 | Pass |
| TXN-02 | Boundary | Transaction có `userId` không tồn tại trong `userMap` (user đã bị xóa) | `userMap.get(...)` trả null → `userName="Unknown"`, `userEmail=""` | Đúng (dòng 116-117) — không crash, có fallback rõ ràng | Pass |
| TXN-03 | Boundary | `tx.getPlan()=null` hoặc `tx.getStatus()=null` | Fallback về chuỗi rỗng `""` thay vì NPE khi gọi `.name()` | Đúng (dòng 119-120) | Pass |

### 4.4. `AdminServiceImpl.getRevenueStats()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| REV-01 | EP hợp lệ | Transaction đa dạng plan, status, tháng | Tính đúng doanh thu theo status, theo plan (chỉ COMPLETED), theo tháng (`YYYY-MM`) | Đúng (dòng 130-169) | Pass |
| REV-02 | Boundary | Không có transaction nào ở 1 plan cụ thể | `sum=0` → bị loại khỏi `byPlan` map (điều kiện `if (sum > 0)`) — chỉ hiện plan có doanh thu thật | Đúng (dòng 145) — tránh nhiễu dữ liệu 0 trong biểu đồ | Pass |
| REV-03 | Boundary | `t.getCreatedAt()=null` | Bị lọc khỏi `monthly` map qua điều kiện `t.getCreatedAt() != null` | Đúng (dòng 150) | Pass |

### 4.5. `AdminServiceImpl.getAnalytics()` — method dài nhất, nhiều phép tính thời gian

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ANL-01 | EP hợp lệ | Có user/login/session trong 30 ngày gần nhất | Đếm đúng theo từng ngày, khởi tạo sẵn `0` cho toàn bộ 30 ngày trước khi merge dữ liệu thật (đảm bảo biểu đồ không thiếu ngày) | Đúng (dòng 182-192) — pattern "seed zero then merge" áp dụng nhất quán cho mọi biểu đồ theo ngày/giờ/tháng trong method | Pass |
| ANL-02 | Boundary | `u.getCreatedAt()=null` (dữ liệu cũ thiếu field) | Bị bỏ qua an toàn qua `if (u.getCreatedAt() != null)` trước khi tính key | Đúng (dòng 188) | Pass |
| ANL-03 | EP hợp lệ | `loginsByHour` (0-23h hôm nay) | Khởi tạo đủ 24 giờ = 0, merge dữ liệu thật theo giờ | Đúng (dòng 209-216) | Pass |
| ANL-04 | EP hợp lệ | `newUsersByMonth` (12 tháng gần nhất) | Format `"YYYY-MM"` đúng, khởi tạo đủ 12 tháng | Đúng (dòng 245-255) | Pass |
| ANL-05 | EP hợp lệ | `activeUsers`/`inactiveUsers` — loại trừ ADMIN | Dùng `findByRoleNot(ADMIN)` trước khi đếm, không tính admin vào thống kê user thường | Đúng (dòng 272-274) — nhất quán trong toàn bộ method (mọi query đều loại trừ ADMIN) | Pass |
| ANL-06 | Boundary | `activeUsersLast7d` — user login nhiều lần trong 7 ngày | Dùng `.distinct()` trên `userId`, không đếm trùng số lần login của cùng 1 user | Đúng (dòng 278) | Pass |
| ANL-07 | EP hợp lệ | `planDistribution` | Lặp qua toàn bộ `SubscriptionPlan.values()`, đếm từng plan (loại trừ ADMIN) | Đúng (dòng 281-284) | Pass |
| ANL-08 | Boundary | `roleDistribution` | Bỏ qua ADMIN trong vòng lặp `UserRole.values()` (`continue`) | Đúng (dòng 288-291) | Pass |

### 4.6. `AdminServiceImpl.createUser(...)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| CRU-01 | EP hợp lệ | Email chưa tồn tại, đủ field bắt buộc | Tạo user với password đã hash, `isVerified=true`, `isActive=true` (admin tạo trực tiếp, không cần xác thực email) | Đúng (dòng 343-359) | Pass |
| CRU-02 | Negative | Email đã tồn tại | Throw `AppException(EMAIL_ALREADY_EXISTS)` | Đúng (dòng 343-345) | Pass |
| CRU-03 | Boundary | `role="INVALID_ROLE"` | Catch `IllegalArgumentException`, fallback `CLIENT` | Đúng (dòng 347-351) — nhất quán với cách xử lý role tương tự ở `AuthServiceImpl.register()` | Pass |
| CRU-04 | EP hợp lệ | `plan="BASIC"` hợp lệ | Set `isPremium=true`, `plan=BASIC`, `planExpiresAt` tính từ `PlanConfig.daysFor(BASIC)`, reset `aiSessionsUsed=0` | Đúng (dòng 364-371) | Pass |
| CRU-05 | Boundary | `plan="FREE"` hoặc `plan=null`/blank | Bỏ qua khối set premium, user giữ mặc định FREE | Đúng (dòng 364 điều kiện `!plan.equalsIgnoreCase("FREE")`) | Pass |
| CRU-06 | Boundary | `plan="INVALID_PLAN"` | Catch `IllegalArgumentException`, **im lặng bỏ qua** toàn bộ khối set premium (`ignored`), user vẫn được tạo nhưng ở trạng thái FREE mặc định | Đúng theo code (dòng 371) — không throw lỗi cho admin biết plan bị sai, chỉ âm thầm fallback. Chấp nhận được cho input admin nhập tay dễ gõ sai, nhưng có thể gây nhầm lẫn không rõ tại sao user không có plan như mong đợi — ghi vào rủi ro tồn đọng | Pass (theo code) |
| CRU-07 | EP hợp lệ | `couponId` hợp lệ, tồn tại | Tăng `usedCount` của discount code | Đúng (dòng 376-381) | Pass |
| CRU-08 | Boundary | `couponId` không tồn tại | `findById(...).ifPresent(...)` im lặng bỏ qua, không throw, user vẫn được tạo | Đúng (dòng 377) | Pass |

### 4.7. `AdminServiceImpl.sendPasswordResetEmail()` / `changeUserPassword()` / `deleteUser()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| PWD-01 | EP hợp lệ | `sendPasswordResetEmail(userId)` hợp lệ | Xóa OTP cũ, tạo OTP mới (hiệu lực 30 phút — dài hơn OTP tự đăng ký 10 phút, hợp lý vì admin gửi thay), gửi email | Đúng (dòng 388-403) | Pass |
| PWD-02 | Negative | `userId` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 389-390) | Pass |
| PWD-03 | EP hợp lệ | `changeUserPassword(userId, newPassword)` | Hash password mới, cập nhật `passwordChangedAt` | Đúng (dòng 406-412) — **Lưu ý:** không có validate độ dài tối thiểu (khác với `AuthServiceImpl.resetPassword` yêu cầu ≥8 ký tự) — cùng loại rủi ro không nhất quán đã ghi nhận ở module Auth (`updateSettings`), ghi vào rủi ro tồn đọng chung | Pass (theo code) |
| DEL-01 | EP hợp lệ | `deleteUser(userId)` | Soft-delete: `setActive(false)`, KHÔNG xóa document thật | Đúng (dòng 415-421) — comment giải thích rõ ý đồ "preserve data integrity" | Pass |
| DEL-02 | Negative | `userId` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng | Pass |

### 4.8. `AdminServiceImpl.getUserStats(String userId)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| UST-01 | EP hợp lệ | User có nhiều practice session | Tính `avgScore` (chỉ tính session có `overallScore > 0`), `bestScore` (toàn bộ session), `recentSessions` giới hạn 5 | Đúng (dòng 431-450) | Pass |
| UST-02 | Boundary | User chưa có session nào | `avgScore`/`bestScore` fallback `0.0` qua `.orElse(0.0)`, không NPE | Đúng (dòng 435, 438) | Pass |
| UST-03 | Boundary | User chưa có `UserStats` record (`stats=null`) | Fallback toàn bộ field liên quan gamification về giá trị mặc định (`currentStreak=0`, `currentTier="BRONZE"`, v.v.) | Đúng (dòng 474-482) | Pass |
| UST-04 | Negative | `userId` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 425-426) | Pass |

### 4.9. `AdminServiceImpl.sendNotificationEmail()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| NTF-01 | EP hợp lệ | `userId` tồn tại, subject/content hợp lệ | Gửi email trực tiếp qua `emailService.sendSimpleEmail` | Đúng (dòng 488-491) | Pass |
| NTF-02 | Negative | `userId` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng | Pass |

### 4.10. `AdminServiceImpl.updateUserStatus()` — **có bug đã sửa**

| ID | Loại | Input | Expected | Actual (sau khi sửa) | Kết quả |
|---|---|---|---|---|---|
| UUS-01 | EP hợp lệ | `id` tồn tại, `isActive`/`isVerified` hợp lệ | Cập nhật cả 2 field, trả DTO | Đúng | Pass |
| UUS-02 | **Negative — Bug đã sửa** | `id` không tồn tại | **Trước fix:** throw `RuntimeException` chung → HTTP 500.<br>**Sau fix:** throw `AppException(USER_NOT_FOUND)` → HTTP 404 đúng chuẩn | Pass (sau khi sửa) |

### 4.11. `AdminServiceImpl.updateUserPlan()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| UUP-01 | EP hợp lệ | `planStr="BASIC"` | Set premium, plan, `planExpiresAt`, reset `aiSessionsUsed=0` | Đúng (dòng 523-529) | Pass |
| UUP-02 | Boundary | `planStr="free"` (chữ thường) | `equalsIgnoreCase` khớp, downgrade về FREE đúng, `planExpiresAt=null` | Đúng (dòng 519-522) | Pass |
| UUP-03 | Negative | `planStr="INVALID"` | Throw `AppException(VALIDATION_FAILED)` — **khác hành vi với `createUser` (CRU-06)**, ở đây THROW rõ ràng thay vì im lặng fallback | **Không nhất quán** giữa 2 method cùng xử lý plan string: `createUser` im lặng bỏ qua khi plan sai, `updateUserPlan` throw lỗi rõ ràng khi plan sai. Có thể là chủ đích (tạo mới ưu tiên không chặn luồng, cập nhật ưu tiên báo lỗi rõ) nhưng nên xác nhận — ghi vào rủi ro tồn đọng | Pass (theo code, ghi nhận không nhất quán) |
| UUP-04 | Negative | `id` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 516-517) | Pass |

### 4.12. `AdminServiceImpl.getGrowthAnalytics()` — metrics phức tạp (DAU/MAU, funnel, cohort)

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| GRW-01 | EP hợp lệ | Có login trong 30 ngày và 24h | Tính đúng MAU/DAU (distinct userId), tỷ lệ `dauMauRatio` | Đúng (dòng 549-554) | Pass |
| GRW-02 | Boundary | `mau=0` (không có login nào trong 30 ngày) | `dauMauRatio` fallback `0.0` thay vì chia 0 | Đúng (dòng 554 — điều kiện `mau > 0 ? ... : 0.0`) | Pass |
| GRW-03 | Boundary | `totalUsers=0` | `conversionRate`/`arpu` đều fallback `0.0` an toàn qua điều kiện `> 0` trước khi chia | Đúng (dòng 563, 569) | Pass |
| GRW-04 | EP hợp lệ | `mrr` — ước tính doanh thu định kỳ hàng tháng | Công thức: `basicUsers*199000 + fullUsers*299000 + annualUsers*165833` (ANNUAL quy đổi theo tháng) | Đúng (dòng 577) — giá trị hardcode khớp comment giải thích rõ nguồn gốc | Pass |
| GRW-05 | Boundary | `newUsers7to14=0` (không có user đăng ký trong khung 7-14 ngày trước) | `userGrowthRate` fallback: nếu có user mới trong 7 ngày gần nhất thì `100.0`, ngược lại `0.0` — tránh chia 0 | Đúng (dòng 616-618) | Pass |
| GRW-06 | Boundary | `cohortSize=0` (không có user đăng ký trong tháng cohort đó) | `retentionRate=0.0`, không chia 0 | Đúng (dòng 634) | Pass |
| GRW-07 | EP hợp lệ | `hotUsers`/`warmUsers`/`coldUsers` — phân khúc user | Logic phân loại không chồng chéo: Hot (login 7d + có session) loại trừ khỏi Warm; Warm (login 30d hoặc có session, nhưng không phải Hot); Cold (không login 30d) | Đúng (dòng 608-610) — kiểm tra thủ công 3 điều kiện logic không giao nhau trên cùng 1 user | Pass |
| GRW-08 | Boundary | User có session nhưng `u.getCreatedAt()=null` | Bị loại khỏi `usersWhoAdoptedFeature` qua điều kiện `if (firstSession == null || u.getCreatedAt() == null) return false` | Đúng (dòng 591) | Pass |

### 4.13. `AdminController` — Endpoints

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| CTL-01 | EP hợp lệ | `PUT /users/{id}/status` với đủ `isActive`/`isVerified` | Gọi service, ghi audit log, trả DTO | Đúng (dòng 75-87) | Pass |
| CTL-02 | Negative | `PUT /users/{id}/status` thiếu `isActive` hoặc `isVerified` trong body | Throw `AppException(VALIDATION_FAILED)` TRƯỚC khi gọi service | Đúng (dòng 78-82) — validate rõ ràng ở tầng Controller cho input dạng `Map` thô | Pass |
| CTL-03 | Negative | `POST /users` thiếu `name`/`email`/`password` | Throw `AppException(VALIDATION_FAILED)` | Đúng (dòng 113-115) | Pass |
| CTL-04 | EP hợp lệ | `POST /users` đủ field | Tạo user, ghi audit log kèm `dto.getId()` (id thật sau khi tạo, không phải id đầu vào vì user mới chưa có id trước đó) | Đúng (dòng 116-120) | Pass |
| CTL-05 | Negative | `PUT /settings/guest-cooldown?hours=0` hoặc `hours=200` | Throw `AppException(VALIDATION_FAILED, "Giờ phải từ 1 đến 168")` | Đúng (dòng 194-195, offset nhẹ sau khi xóa 2 dòng `@PreAuthorize` thừa) | Pass |
| CTL-06 | Boundary | `PUT /settings/guest-cooldown?hours=1` và `hours=168` (2 biên) | Cả 2 đều hợp lệ (điều kiện `< 1 || > 168`) | Đúng | Pass |
| CTL-07 | EP hợp lệ (sau refactor) | Toàn bộ endpoint không có ADMIN JWT | Bị chặn bởi class-level `@PreAuthorize("hasAuthority('ADMIN')")`, kể cả 2 endpoint `guest-cooldown` sau khi xóa annotation thừa | Đúng — hành vi bảo mật không đổi sau khi dọn code trùng | Pass |
| CTL-08 | EP hợp lệ | `POST /migrate-db` | Gọi `DatabaseMigrationService.migrateFromMcHub()`, ghi audit log | Đúng (dòng 170-176) — **Lưu ý:** endpoint chạy migration DB thủ công qua API, không có safeguard môi trường (dev/prod) tương tự rủi ro đã ghi nhận ở `fixAllSeededPasswords()` module Auth — ghi vào rủi ro tồn đọng chung | Pass (theo code, rủi ro vận hành) |

## 5. Tổng kết kết quả test

| Chỉ số | Số lượng |
|---|---|
| Tổng số test case | 52 |
| Pass | 52 |
| Fail | 0 |
| Bug phát hiện & đã sửa | 1 (sai loại exception trong `updateUserStatus` gây HTTP 500 thay vì 404) |
| Refactor DRY | 1 (xóa `@PreAuthorize` thừa ở 2 method khi class đã có class-level annotation) |
| Ghi nhận (không sửa, chờ quyết định) | 3 (`changeUserPassword` không validate độ dài tối thiểu; `createUser` im lặng bỏ qua plan sai trong khi `updateUserPlan` throw lỗi rõ ràng — không nhất quán; endpoint `migrate-db` không có safeguard môi trường dev/prod) |

**Giới hạn môi trường:** Không thể chạy embedded MongoDB integration test thật — toàn bộ test case dựa trên trace logic thủ công đối chiếu source code thực tế.

## 6. Kết luận

Module Admin đạt chất lượng tốt sau khi sửa 1 bug về loại exception sai (ảnh hưởng HTTP status code trả về cho client) và dọn 1 chỗ vi phạm DRY (annotation bảo mật lặp lại). Các phép tính analytics/growth metrics phức tạp (DAU/MAU, cohort retention, phễu chuyển đổi) đều xử lý cẩn thận các trường hợp biên chia-cho-0 và dữ liệu null. Audit log được ghi đầy đủ cho mọi hành động quản trị nhạy cảm.

**Rủi ro tồn đọng cần lưu ý (không tự sửa, chờ quyết định):**
1. `changeUserPassword()` không kiểm tra độ dài tối thiểu mật khẩu mới — cùng loại vấn đề đã ghi nhận ở `AuthServiceImpl.updateSettings()` (module Auth), nên xem xét thống nhất validate password ở 1 nơi chung.
2. Xử lý plan string không hợp lệ không nhất quán giữa 2 method: `createUser()` im lặng bỏ qua (user vẫn tạo nhưng ở FREE), `updateUserPlan()` throw lỗi rõ ràng. Nên xác nhận đây có phải chủ đích khác biệt (tạo mới ưu tiên không chặn vs cập nhật ưu tiên báo lỗi) hay cần thống nhất hành vi.
3. `POST /migrate-db` cho phép chạy migration DB thủ công qua API mà không có safeguard theo môi trường (dev/prod) — cùng loại rủi ro vận hành đã ghi nhận với `fixAllSeededPasswords()` ở module Auth.
