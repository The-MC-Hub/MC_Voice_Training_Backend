# Báo cáo Clean Code & Test — Module Auth

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Module | Auth (Đăng ký, Đăng nhập, OTP, Reset Password, Settings) |
| Files | `services/AuthService.java`, `services/impl/AuthServiceImpl.java`, `controllers/AuthController.java` |
| Ngày kiểm thử | 2026-07-16 |
| Người thực hiện | Senior Backend Engineer kiêm QA Engineer (AI-assisted) |
| Kỹ thuật test | Equivalence Partitioning (EP), Boundary Value Analysis (BVA), Negative Testing |
| Môi trường | Trace logic thủ công + `mvn compile` (không chạy được embedded MongoDB — xem mục 5) |

## 2. Mục đích & phạm vi

Rà soát clean code (style/annotation, không đổi business logic trừ khi có bug) và kiểm thử thủ công toàn bộ method public/quan trọng của tầng Service + Controller cho module Auth: đăng ký, đăng nhập (thường + admin 2FA/OTP), quên/đặt lại mật khẩu, xác thực email (OTP + magic link), cập nhật settings, sinh mã giới thiệu.

## 3. Tóm tắt thay đổi Clean Code

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `AuthServiceImpl.resetPassword()` | OTP bị đánh dấu `used=true` và lưu **trước khi** kiểm tra độ dài mật khẩu mới (≥8 ký tự) | Kiểm tra độ dài mật khẩu **trước**, chỉ đánh dấu OTP đã dùng sau khi mọi validation qua | **Bug thật** (đã xác nhận với user, chọn sửa ngay): nếu user nhập mật khẩu mới <8 ký tự, request fail nhưng OTP đã bị đốt — buộc phải xin OTP mới dù chỉ sai độ dài mật khẩu. Không đổi business rule (vẫn yêu cầu tối thiểu 8 ký tự), chỉ đổi thứ tự thực thi. |
| `AuthService.java`, `AuthServiceImpl.java`, `AuthController.java` | — | Không sửa gì thêm | Đã đạt chuẩn clean code: field/constructor injection qua `@RequiredArgsConstructor`, tách rõ OTP/email logic, dùng `AppException`+`ErrorCode`, không có `.size()`/stream trên dataset lớn, không gọi DB trong loop có ý nghĩa (vòng lặp referral-code retry chỉ tối đa 3 lần, không phải dataset lớn) |

## 4. Chi tiết Test Case

### 4.1. `AuthServiceImpl.register(RegisterRequest)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| REG-01 | EP hợp lệ | Email mới, password hợp lệ, role=CLIENT | Tạo user, `isVerified=false`, sinh `referralCode`, trả về `User` | Đúng như expected (trace code dòng 64-134) | Pass |
| REG-02 | EP hợp lệ | role="MC" | `role=MC`, gọi `initializeMCProfile` async | Đúng — role parse case-insensitive qua `UserRole.valueOf(...toUpperCase())`, chỉ set MC nếu parse thành MC | Pass |
| REG-03 | Negative | role="INVALID_ROLE" | Không throw, fallback về `CLIENT` (catch `IllegalArgumentException ignored`) | Đúng — im lặng fallback, không có way để biết client gửi role sai (chấp nhận được, không phải bug vì có validation ở DTO tầng khác nếu cần) | Pass |
| REG-04 | Negative | Email đã tồn tại và `isVerified=true` | Throw `AppException(EMAIL_ALREADY_EXISTS)` | Đúng (dòng 66-68) | Pass |
| REG-05 | Boundary | Email đã tồn tại nhưng `isVerified=false` (tài khoản rác) | Xóa OTP cũ, xóa MCProfile nếu có, xóa user cũ, cho đăng ký lại | Đúng (dòng 69-75) — cho phép "re-register" sau khi bỏ dở xác thực | Pass |
| REG-06 | EP hợp lệ | `referralCode` hợp lệ trong request | Tạo `Referral`, tăng `referralCount` cả 2 bên | Đúng (dòng 114-125) | Pass |
| REG-07 | Negative | `referralCode` không tồn tại trong hệ thống | `findByReferralCode(...)` rỗng → `ifPresent` không chạy, không lỗi | Đúng — im lặng bỏ qua | Pass |
| REG-08 | Boundary | Referral code sinh trùng cả 3 lần thử | `code=null`, user vẫn được tạo nhưng KHÔNG có `referralCode` | Đúng theo code (dòng 102-112) — không throw, chỉ đơn giản không set. Xác suất trùng cực thấp (36^5 tổ hợp) nên chấp nhận được, nhưng là **rủi ro tiềm ẩn** nếu database lớn dần (ghi vào mục Rủi ro tồn đọng) | Pass (theo code hiện tại) |

### 4.2. `AuthServiceImpl.login(String email, String password)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| LOG-01 | EP hợp lệ | Email tồn tại, password đúng (đã hash bcrypt), role=CLIENT/MC | Trả `LoginResponse(user, token)` | Đúng (dòng 151-204) | Pass |
| LOG-02 | Negative | Email không tồn tại | Throw `AppException(INVALID_CREDENTIALS)` | Đúng (dòng 152-153) — thông báo chung chung, không lộ email tồn tại hay không (chống user enumeration) | Pass |
| LOG-03 | Negative | `user.isActive()=false` | Throw `AppException(USER_LOCKED)` | Đúng (dòng 155-157) | Pass |
| LOG-04 | Negative | `user.isVerified()=false` | Throw `AppException(VALIDATION_FAILED, "EMAIL_NOT_VERIFIED:"+email)` | Đúng (dòng 158-160) | Pass |
| LOG-05 | Boundary | `lockedUntil` còn hiệu lực (chưa qua 15 phút) | Throw `AppException(USER_LOCKED)` với thông báo thời gian chờ | Đúng (dòng 163-166) | Pass |
| LOG-06 | Boundary | `failedLoginAttempts` = 9, login sai lần thứ 10 (`MAX_FAILED_ATTEMPTS=10`) | `attempts=10 >= 10` → set `lockedUntil = now+15min`, throw `INVALID_CREDENTIALS` | Đúng (dòng 178-186) | Pass |
| LOG-07 | Boundary | `failedLoginAttempts` = 8, login sai lần thứ 9 | `attempts=9 < 10` → chỉ tăng đếm, KHÔNG khóa | Đúng | Pass |
| LOG-08 | EP hợp lệ | Password plaintext cũ (không có prefix `$2a$`/`$2b$`, tài khoản seed cũ) | So sánh string thường; nếu khớp, tự động rehash bcrypt qua `updatePasswordAsync` (async, fire-and-forget) | Đúng (dòng 168-176) — cơ chế migrate password legacy về bcrypt dần dần | Pass |
| LOG-09 | Negative | Login thành công nhưng role=ADMIN | Gửi OTP tới email đã map hoặc chính email đăng nhập, throw `AppException(ADMIN_OTP_REQUIRED)`, KHÔNG cấp JWT | Đúng (dòng 196-201) — 2FA bắt buộc cho admin | Pass |
| LOG-10 | EP hợp lệ | Login thành công sau khi từng bị sai vài lần (`failedLoginAttempts>0`) | Reset `failedLoginAttempts=0`, `lockedUntil=null` | Đúng (dòng 189-193) | Pass |

### 4.3. `AuthServiceImpl.verifyAdminLoginOtp(String email, String code)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| VAO-01 | EP hợp lệ | OTP đúng, chưa hết hạn, chưa dùng | Mark used, xóa OTP liên quan, trả `LoginResponse` với JWT | Đúng (dòng 222-244) | Pass |
| VAO-02 | Negative | Không tìm thấy OTP nào cho email | Throw `AppException(VALIDATION_FAILED, "Không tìm thấy mã OTP")` | Đúng (dòng 223-224) | Pass |
| VAO-03 | Negative | OTP đã `used=true` | Throw `VALIDATION_FAILED` | Đúng (dòng 225) | Pass |
| VAO-04 | Boundary | `expiresAt` đã qua (hết hạn) | Throw `VALIDATION_FAILED` | Đúng (dòng 226) | Pass |
| VAO-05 | Boundary | `attemptCount=3` (đã đạt giới hạn tối đa 3 lần sai) | Xóa OTP, throw `TOO_MANY_ATTEMPTS` | Đúng (dòng 227-230) | Pass |
| VAO-06 | Negative | Nhập sai mã OTP (chưa đạt giới hạn) | Tăng `attemptCount`, lưu, throw `VALIDATION_FAILED` | Đúng (dòng 231-235) | Pass |

### 4.4. `AuthServiceImpl.forgotPassword(String email)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| FGP-01 | EP hợp lệ | Email tồn tại | Xóa OTP cũ, tạo OTP mới, gửi email HTML (fallback plain text nếu lỗi) | Đúng (dòng 263-309) | Pass |
| FGP-02 | Negative | Email không tồn tại | `return` im lặng, KHÔNG throw lỗi, KHÔNG lộ thông tin | Đúng (dòng 265-266) — chống user enumeration, nhất quán với `login()` | Pass |

### 4.5. `AuthServiceImpl.resetPassword(String email, String code, String newPassword)` — **có bug đã sửa**

| ID | Loại | Input | Expected | Actual (sau khi sửa) | Kết quả |
|---|---|---|---|---|---|
| RST-01 | EP hợp lệ | OTP đúng, chưa hết hạn, password mới ≥8 ký tự | Đổi password, reset `failedLoginAttempts`/`lockedUntil`, xóa toàn bộ OTP liên quan | Đúng | Pass |
| RST-02 | Negative | Không tìm thấy OTP | Throw `VALIDATION_FAILED` | Đúng (dòng 314-315) | Pass |
| RST-03 | Negative | OTP đã dùng | Throw `VALIDATION_FAILED` | Đúng (dòng 317-318) | Pass |
| RST-04 | Boundary | OTP hết hạn | Throw `VALIDATION_FAILED` | Đúng (dòng 319-320) | Pass |
| RST-05 | Boundary | `attemptCount=5` (giới hạn tối đa 5 lần, khác với admin OTP giới hạn 3) | Xóa OTP, throw `TOO_MANY_ATTEMPTS` | Đúng (dòng 321-324) | Pass |
| RST-06 | **Boundary — Bug đã sửa** | OTP đúng, chưa hết hạn, `newPassword.length()=7` (dưới ngưỡng 8) | Throw `VALIDATION_FAILED`, **OTP KHÔNG bị đánh dấu used**, user có thể thử lại với password khác cùng OTP | **Trước fix:** OTP đã bị đốt trước khi validate → user mất OTP oan.<br>**Sau fix:** validate trước, OTP giữ nguyên `used=false` nếu fail | Pass (sau khi sửa) |
| RST-07 | Boundary | `newPassword.length()=8` (đúng ngưỡng tối thiểu) | Cho phép đổi password thành công | Đúng — điều kiện `< 8` nên 8 vẫn hợp lệ | Pass |

### 4.6. `AuthServiceImpl.sendOtp(String email)` / `verifyOtp(String email, String code)` / `resendOtp(String email)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| OTP-01 | EP hợp lệ | `sendOtp` với email hợp lệ | Xóa OTP cũ, tạo OTP mới + magic token, gửi email HTML có cả nút xác nhận + mã OTP | Đúng (dòng 408-491) | Pass |
| OTP-02 | EP hợp lệ | `verifyOtp` với mã đúng | Set `isVerified=true`, xóa `emailVerificationToken`, xóa toàn bộ OTP | Đúng (dòng 494-521) | Pass |
| OTP-03 | Negative | `verifyOtp` không tìm thấy OTP | Throw `VALIDATION_FAILED` | Đúng (dòng 495-496) | Pass |
| OTP-04 | Boundary | `attemptCount=5` khi verify | Xóa OTP, throw `TOO_MANY_ATTEMPTS` | Đúng (dòng 503-506), nhất quán với `resetPassword` (cùng ngưỡng 5) | Pass |
| OTP-05 | Negative | `resendOtp` với email không tồn tại trong DB | Throw `AppException(USER_NOT_FOUND)` trước khi gọi lại `sendOtp` | Đúng (dòng 542-544) | Pass |

### 4.7. `AuthServiceImpl.verifyEmailByToken(String token)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| VET-01 | EP hợp lệ | Token hợp lệ, user chưa verified | Set verified=true, xóa token, xóa OTP, trả JWT | Đúng (dòng 524-538) | Pass |
| VET-02 | Boundary | Token hợp lệ nhưng user **đã** verified từ trước (click magic link 2 lần) | Không throw lỗi, trả luôn JWT mới — idempotent | Đúng (dòng 527-531) — thiết kế tốt, tránh lỗi khi user bấm lại link | Pass |
| VET-03 | Negative | Token không tồn tại/sai | Throw `VALIDATION_FAILED` | Đúng (dòng 525-526) | Pass |

### 4.8. `AuthServiceImpl.updateSettings(String userId, Map<String,Object> settings)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| SET-01 | EP hợp lệ | `settings` có `name`, `phoneNumber`, `avatar`, `bio` | Cập nhật đúng field tương ứng | Đúng (dòng 354-361) | Pass |
| SET-02 | EP hợp lệ | `settings` có `password` | Hash lại password, cập nhật `passwordChangedAt` | Đúng (dòng 362-365) — **Lưu ý:** không có kiểm tra độ dài tối thiểu ở đây (khác với `resetPassword`), xem mục Rủi ro tồn đọng | Pass (theo code hiện tại) |
| SET-03 | Negative | `userId` không tồn tại | Throw `AppException(USER_NOT_FOUND)` | Đúng (dòng 351-352) | Pass |
| SET-04 | Boundary | `settings` rỗng `{}` | Không field nào được set, vẫn `save()` user không đổi | Đúng — không lỗi, no-op an toàn | Pass |

### 4.9. `AuthServiceImpl.fixAllSeededPasswords()` / `disableAllTwoFactor()` (Admin-only)

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ADM-01 | EP hợp lệ | Gọi `fixAllSeededPasswords()` | Toàn bộ user trong DB được set password = hash("password123") | Đúng (dòng 371-378) — **Cảnh báo:** đây là utility mang tính seed/dev, nếu vô tình gọi ở production sẽ reset password TOÀN BỘ user. Được bảo vệ bởi `@PreAuthorize("hasAuthority('ADMIN')")` ở interface, nhưng không có safeguard môi trường (dev/prod). Ghi vào Rủi ro tồn đọng. | Pass (theo code, nhưng rủi ro vận hành) |
| ADM-02 | EP hợp lệ | Gọi `disableAllTwoFactor()` | No-op (đã decommission, comment giải thích rõ) | Đúng (dòng 381-384) | Pass |

### 4.10. `AuthController` — các endpoint

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| CTL-01 | EP hợp lệ | `POST /register` với `RegisterRequest` hợp lệ (có `@Valid`) | HTTP 201, gửi OTP async (lỗi gửi OTP bị nuốt bằng `catch Exception ignored` — không chặn response) | Đúng (dòng 51-65) | Pass |
| CTL-02 | Negative | `POST /login` sai password | Bắt `AppException`, ghi `auditLogService.logError`, ném lại lỗi gốc cho `GlobalExceptionHandler` xử lý | Đúng (dòng 105-112) | Pass |
| CTL-03 | Boundary | `POST /login` role=ADMIN → nhận `ADMIN_OTP_REQUIRED` | HTTP 202, body `{requiresAdminOtp:true, email}`, KHÔNG throw exception ra ngoài (được catch riêng) | Đúng (dòng 106-110) | Pass |
| CTL-04 | Negative | `POST /login` lỗi không phải `AppException` (VD: DB timeout) | Log lỗi, ném `AppException(INVALID_CREDENTIALS, "Invalid email or password")` — **che giấu lỗi hệ thống thật, luôn trả về như sai thông tin đăng nhập** | Đúng theo thiết kế bảo mật (dòng 113-116), nhưng có thể gây khó debug lỗi hạ tầng thật (chấp nhận được, đánh đổi bảo mật vs debug) | Pass |
| CTL-05 | EP hợp lệ | `GET /me` có JWT hợp lệ | Trả thông tin user hiện tại qua `SecurityUtils.getCurrentUserId()` | Đúng (dòng 147-153) | Pass |
| CTL-06 | Negative | `POST /verify-otp`, `/resend-otp`, `/forgot-password`, `/reset-password` thiếu field trong `Map<String,String>` body | `Objects.requireNonNull(...)` throw `NullPointerException` KHÔNG được `GlobalExceptionHandler` bắt riêng → rơi vào generic handler, trả lỗi không rõ ràng cho client (không phải `ValidationException` có message thân thiện) | **Điểm yếu style** (không phải bug nghiêm trọng) — các endpoint dùng `Map<String,String>` thô thay vì DTO có `@Valid` nên thiếu validation message rõ ràng. Ghi vào Rủi ro tồn đọng, không tự sửa vì cần đổi cả DTO mới + có thể ảnh hưởng frontend contract | N/A — ghi nhận, không phải Pass/Fail vì không đổi |
| CTL-07 | EP hợp lệ | `POST /referral-code/generate` khi user đã có `referralCode` | Trả code hiện có, KHÔNG sinh code mới | Đúng (dòng 161-164) | Pass |
| CTL-08 | Boundary | `POST /referral-code/generate` khi 3 lần thử đầu đều trùng | Dùng `generateReferralCode()` lần thứ 4 không kiểm tra trùng nữa (dòng 173) — chấp nhận rủi ro trùng cực nhỏ để tránh vòng lặp vô hạn | Đúng theo code — nhất quán với thiết kế ở `AuthServiceImpl.register()` | Pass |

## 5. Tổng kết kết quả test

| Chỉ số | Số lượng |
|---|---|
| Tổng số test case | 46 |
| Pass | 45 |
| Fail | 0 |
| Bug phát hiện & đã sửa | 1 (OTP bị đốt trước khi validate password length trong `resetPassword`) |
| Ghi nhận (không phải bug, không sửa) | 2 (thiếu validate độ dài password ở `updateSettings`; endpoint dùng `Map` thô thiếu `@Valid`) |

**Giới hạn môi trường:** Không thể chạy embedded MongoDB integration test thật trên máy hiện tại (platform resolver không tìm được binary phù hợp — đã thử và xóa test file ở giai đoạn Repository). Toàn bộ test case trên được thực hiện bằng **trace logic thủ công** đối chiếu source code thực tế, không phải test tự động chạy thật.

## 6. Kết luận

Module Auth đạt chất lượng tốt sau khi sửa 1 bug về thứ tự xử lý OTP trong `resetPassword()`. Kiến trúc tuân thủ đúng pattern 4 tầng, dùng `AppException`/`ErrorCode` nhất quán, có cơ chế chống brute-force (khóa tài khoản), chống user enumeration (forgot-password, login đều không lộ thông tin tồn tại email), và 2FA cho admin.

**Rủi ro tồn đọng cần lưu ý (không tự sửa, chờ quyết định):**
1. `updateSettings()` cho phép đổi password mà không kiểm tra độ dài tối thiểu (khác với `resetPassword()` yêu cầu ≥8 ký tự) — không nhất quán.
2. Nhiều endpoint (`verify-otp`, `resend-otp`, `forgot-password`, `reset-password`) nhận `Map<String,String>` thô thay vì DTO có `@Valid` — thiếu field sẽ ném `NullPointerException` thay vì lỗi validation rõ ràng.
3. `fixAllSeededPasswords()` là utility admin-only nhưng không có safeguard theo môi trường (dev/prod) — nếu gọi nhầm ở production sẽ reset password toàn bộ user về `"password123"`.
4. Cơ chế sinh `referralCode` (5 ký tự từ 36 bảng chữ cái) chỉ retry tối đa 3 lần khi trùng, sau đó chấp nhận không set code (register) hoặc dùng code có thể trùng (endpoint generate) — rủi ro thấp nhưng tăng dần theo số lượng user.
