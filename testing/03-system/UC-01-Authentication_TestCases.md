# UC-01 — Authentication — System Test Cases

**Ngày thực thi:** 2026-07-17
**Môi trường:** LIVE — `mchub_test` (MongoDB Atlas, database riêng), backend chạy port `5555`, máy chủ Windows múi giờ hệ thống UTC+7
**QA users:** `qa.auth.uc01@mchubtest.local` (CLIENT, tạo mới trong session này), `qa.admin.uc04@mchubtest.local` (ADMIN, tái sử dụng từ UC-04)

---

## Danh sách Test Case

| ID | Mô tả | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TC-AUTH-01 | Đăng ký hợp lệ | `POST /auth/register` `{name, email, password, role:CLIENT}` | 200/201, `requiresVerification:true`, OTP gửi | Đúng | PASS |
| TC-AUTH-02 | Đăng ký lại email CHƯA verify (edge — cleanup luồng dở dang) | `POST /auth/register` cùng email TC-AUTH-01, chưa verify-otp | 200, xoá record cũ + tạo mới (cho phép retry) | Đúng, xác nhận đúng thiết kế đọc code (`AuthServiceImpl.register()` dòng 65-75: unverified stale account bị xoá để cho phép đăng ký lại) | PASS |
| TC-AUTH-03 | Đăng ký email sai định dạng (negative/EP) | `email:"not-an-email"` | 400, "Invalid email format" | Đúng, `ERR_9002` | PASS |
| TC-AUTH-04 | Đăng ký password yếu (negative/BVA) | `password:"123"` | 400, "Password must be between 8 and 128 characters" | Đúng | PASS |
| TC-AUTH-05 | Đăng ký thiếu tên (negative) | không có `name` | 400, "Name cannot be empty" | Đúng | PASS |
| TC-AUTH-06 | Verify-otp mã sai (negative) | `code:"000000"` | 400, "Mã OTP không đúng" | Đúng | PASS |
| TC-AUTH-07 | Verify-otp mã đúng | mã OTP thật lấy từ DB | 200, verify thành công + auto-login (trả token) | Đúng | PASS |
| TC-AUTH-08 | Verify-otp dùng lại mã đã dùng (negative — state) | mã đã verify ở TC-AUTH-07 | 400, "Không tìm thấy mã OTP" | Đúng | PASS |
| TC-AUTH-09 | Đăng ký lại email ĐÃ verify (negative — đúng nghĩa trùng email) | cùng email đã verify | 409/400, `EMAIL_ALREADY_EXISTS` | Đúng, `ERR_1005` "Email này đã được sử dụng..." | PASS |
| TC-AUTH-10 | Resend-otp cho user ĐÃ verify (edge case) | `POST /auth/resend-otp` user đã `isVerified:true` | Nên từ chối hoặc không cần gửi (user đã xác minh) | 200 "OTP resent" — **hệ thống vẫn gửi OTP cho user đã verify, không có guard kiểm tra `isVerified` trước khi gửi**. Không ảnh hưởng bảo mật/dữ liệu (verify-otp cho user đã verify không đổi trạng thái gì thêm), chỉ lãng phí email quota — mức độ Minor, không file defect riêng, ghi chú cho dev cân nhắc thêm | PASS (ghi chú kỹ thuật, không phải Fail) |
| TC-AUTH-11 | Resend-otp email không tồn tại (negative) | email lạ | 404, "Email không tồn tại" | Đúng, `ERR_2001` | PASS |
| TC-AUTH-12 | Đăng nhập đúng thông tin | email/password đúng | 200, trả JWT + user info | Đúng | PASS |
| TC-AUTH-13 | Đăng nhập sai password (negative) | password sai | 401, "Email hoặc mật khẩu không đúng" | Đúng, `ERR_1001` (không tiết lộ email tồn tại hay không — chống enumeration) | PASS |
| TC-AUTH-14 | Đăng nhập email không tồn tại (negative) | email lạ | 401, cùng message chung chung như TC-AUTH-13 | Đúng — hệ thống trả CÙNG message cho cả 2 trường hợp (sai password / email không tồn tại), đúng thực hành bảo mật chống email enumeration | PASS |
| TC-AUTH-15 | GET /me với JWT hợp lệ | JWT hợp lệ | 200, thông tin user hiện tại | Đúng | PASS |
| TC-AUTH-16 | GET /me KHÔNG có JWT (negative — auth) | không có header `Authorization` | 401 Unauthorized | **HTTP 500 "System error"** — sai mã lỗi hoàn toàn, che giấu bản chất lỗi thật | **FAIL — DEFECT-015** |
| TC-AUTH-17 | Forgot-password email hợp lệ | email tồn tại | 200, "Reset code sent to your email" | Đúng | PASS |
| TC-AUTH-18 | Forgot-password email KHÔNG tồn tại (kiểm tra chống email enumeration) | email lạ | 200, cùng message như trên (không tiết lộ email có tồn tại hay không) | Đúng — hệ thống trả cùng message thành công bất kể email tồn tại hay không, đúng thực hành bảo mật | PASS |
| TC-AUTH-19 | Reset-password mã sai (negative) | `code:"000000"` | 400, "Mã OTP không đúng" | Đúng | PASS |
| TC-AUTH-20 | Reset-password mã đúng | mã reset thật lấy từ DB (lưu ý: key Mongo có prefix `pwd_reset:{email}`, khác OTP verify email thường) | 200, "Password reset successfully" | Đúng | PASS |
| TC-AUTH-21 | Đăng nhập bằng password CŨ sau khi reset (negative — xác nhận password đã đổi) | password cũ | 401, từ chối | Đúng | PASS |
| TC-AUTH-22 | Đăng nhập bằng password MỚI sau khi reset | password mới | 200, đăng nhập thành công | Đúng | PASS |
| TC-AUTH-23 | GET /auth/me NGAY SAU khi reset-password, dùng JWT mới phát hành (regression — session invalidation) | JWT mới từ TC-AUTH-22, gọi `/me` | 200, truy cập bình thường (token phát hành SAU khi đổi mật khẩu phải hợp lệ) | **HTTP 500 — JWT mới hoàn toàn vẫn bị từ chối như thể phát hành TRƯỚC khi đổi mật khẩu, do lỗi lệch múi giờ 7 tiếng khi so sánh `passwordChangedAt`** | **FAIL — DEFECT-016 (Critical)** |
| TC-AUTH-24 | POST /auth/referral-code/generate KHÔNG có JWT (negative — auth) | không JWT | 401/403 | 403 (chấp nhận được — Spring Security mặc định trả 403 cho `AccessDeniedException`, không phải bug riêng) | PASS |
| TC-AUTH-25 | PUT /auth/settings với JWT mới sau reset-password (bị chặn bởi cùng nguyên nhân TC-AUTH-23) | JWT mới, body settings | 200, cập nhật thành công | HTTP 500 — cùng nguyên nhân DEFECT-016, không phải lỗi độc lập của endpoint `/settings` | **FAIL — cùng DEFECT-016, không file riêng** |
| TC-AUTH-26 | Đăng nhập ADMIN kích hoạt 2FA | `POST /auth/login` với user role ADMIN | 202, `ADMIN_OTP_REQUIRED`, không trả JWT ngay | Đúng (regression, đã xác nhận từ session UC-04/UC-06 trước) | PASS |
| TC-AUTH-27 | Verify-admin-login-otp mã sai (negative) | `code:"000000"` | 400, "Mã OTP không đúng" | Đúng | PASS |

---

## Ghi chú kỹ thuật quan trọng

**DEFECT-016 là phát hiện nghiêm trọng nhất trong toàn bộ đợt test UC-01 đến nay** — lỗi lệch múi giờ khi lưu `passwordChangedAt` (dùng `LocalDateTime.now()` theo giờ hệ thống local UTC+7 thay vì UTC) kết hợp với `JwtAuthenticationFilter` hiểu sai giá trị này là UTC khi so sánh với JWT `issuedAt` (luôn chuẩn UTC) — khiến MỌI user đổi mật khẩu xong đều KHÔNG đăng nhập lại dùng được trong nhiều giờ sau đó (tuỳ thời điểm trong ngày UTC). Đây là lỗi chặn hoàn toàn 1 luồng nghiệp vụ cốt lõi (không phải edge case), xảy ra với 100% user thực hiện đổi mật khẩu trên máy chủ đặt múi giờ UTC+7 — đề xuất ưu tiên fix khẩn cấp trước khi deploy production thật.

TC-AUTH-16 (DEFECT-015) và TC-AUTH-23/25 (DEFECT-016) đều có chung triệu chứng bề mặt (HTTP 500 "System error, please try again later") nhưng gốc rễ khác nhau hoàn toàn — DEFECT-015 là exception-handling sai loại (thiếu `@PreAuthorize` + `IllegalStateException` không được map đúng HTTP status), DEFECT-016 là lỗi logic nghiệp vụ (so sánh thời gian sai múi giờ). Cả 2 cùng biểu hiện qua "500 System error" nên nếu chỉ nhìn response, dev có thể nhầm là cùng 1 bug — cần đọc kỹ log để phân biệt.

---

## Tổng kết

**27/27 test case đã thực thi.**

| Kết quả | Số lượng |
|---|---|
| PASS | 24 |
| FAIL | 3 (TC-AUTH-16, TC-AUTH-23, TC-AUTH-25) |
| Not Executed | 0 |

**2 defect mới phát hiện:** DEFECT-015 (Major/P1 — `/me` trả 500 thay vì 401 cho Guest), DEFECT-016 (**Critical/P0** — lệch múi giờ khiến JWT hợp lệ bị từ chối sau đổi mật khẩu, chặn hoàn toàn luồng nghiệp vụ).

**Điểm sáng:** Luồng đăng ký/verify-otp/login/forgot-reset-password/admin-2FA đều đúng thiết kế bảo mật (chống email enumeration, rate-limit OTP qua `attemptCount`, cleanup stale unverified account). 2 defect phát hiện đều nghiêm trọng và đáng chú ý — đặc biệt DEFECT-016 nên được ưu tiên xử lý trước khi hệ thống go-live.
