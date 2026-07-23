# Security Audit — MC Voice Training Backend

Audit ngày 2026-07-23, fix áp dụng cùng ngày. Phạm vi: `MC_Voice_Training_Backend/` only (Java Spring Boot). Không bao gồm frontend, mobile backend, hay dự án booking gốc.

**Trạng thái: 7/9 lỗi đã fix + verify (build + 49 test pass). 2 lỗi còn lại (M2, M3) cần thêm infrastructure mới, ghi nhận là follow-up riêng.**

---

## 🔴 Critical

### ✅ C1 — JWT secret có fallback hardcoded, không fail-fast khi thiếu env var — FIXED
**File:** `src/main/resources/application.properties:27`, `src/main/java/com/mchub/services/impl/JwtServiceImpl.java`

**Vấn đề (trước fix):** `mchub.jwt.secret=${JWT_SECRET:default_secret_key_change_me}` — nếu `JWT_SECRET` không set lúc deploy, app fallback về secret cố định đã lộ công khai trong source code. Attacker biết secret này tự ký JWT hợp lệ cho bất kỳ `userId`/`role` nào (kể cả `ADMIN`).

**Fix đã áp dụng:**
- Xóa fallback trong `application.properties` → `mchub.jwt.secret=${JWT_SECRET:}`
- Thêm `@PostConstruct validateSecret()` trong `JwtServiceImpl`: throw `IllegalStateException` nếu secret rỗng hoặc dưới 32 bytes (yêu cầu tối thiểu cho HS256) → app **không khởi động được** nếu thiếu/yếu secret, thay vì âm thầm chạy với secret không an toàn.

---

### ✅ C2 — Legacy plaintext password fallback trong login — FIXED
**File:** `src/main/java/com/mchub/services/impl/AuthServiceImpl.java`

**Vấn đề (trước fix):** Nếu password trong DB không bắt đầu `$2a$`/`$2b$`, hệ thống so sánh plaintext trực tiếp (`password.equals(user.getPassword())`) thay vì bắt buộc BCrypt.

**Fix đã áp dụng:** Xóa hoàn toàn nhánh else — login giờ luôn dùng `passwordEncoder.matches(password, user.getPassword())`. Nếu DB còn user với password không phải BCrypt hash, login sẽ fail (đúng hành vi mong muốn — buộc reset password qua flow `forgot-password` thay vì âm thầm chấp nhận).

**Test:** `AuthServiceImplTest.plainTextPasswordIsRejected()` — verify plaintext password bị từ chối, không phát hành JWT.

---

## 🟠 Cao

### ✅ H1 — Rate limit chỉ áp dụng cho 4 route auth — FIXED
**File:** `src/main/java/com/mchub/config/RateLimitFilter.java`

**Fix đã áp dụng:** Thêm bucket `sensitiveBuckets` (10 req/10 phút/IP) áp dụng cho:
- `POST /api/v1/voice/practice/analyze-guest` (AI compute tốn kém)
- `POST /api/v1/payment/apply-discount` (abuse-prone discount probing)

---

### ✅ H2 — Rate limit tin tưởng `X-Forwarded-For` không nhất quán giữa các nơi — FIXED
**File:** `src/main/java/com/mchub/util/ClientIpResolver.java` (mới), `RateLimitFilter.java`, `VoiceController.java`

**Vấn đề (trước fix):** Logic extract IP từ `X-Forwarded-For` chỉ tồn tại trong `RateLimitFilter`, còn `VoiceController.analyzeGuestVoice()` dùng `request.getRemoteAddr()` trực tiếp — 2 cơ chế khác nhau cho cùng mục đích chống-abuse, dễ lệch hành vi khi đổi hạ tầng proxy.

**Fix đã áp dụng:** Tạo `ClientIpResolver` dùng chung, có Javadoc ghi rõ giả định (chỉ tin `X-Forwarded-For` vì production chạy sau Render proxy; nếu app từng expose trực tiếp phải audit lại toàn bộ caller). Cả `RateLimitFilter` và `VoiceController` giờ gọi cùng 1 hàm.

**Còn lại (không phải bug, là vận hành):** Vẫn cần xác nhận thủ công Render có set `X-Forwarded-For` đúng cách (không cho client override) — đây là việc kiểm tra hạ tầng, không sửa được bằng code.

---

### ✅ H3 — OTP brute-force theo email không bị giới hạn, chỉ giới hạn theo IP — FIXED
**File:** `src/main/java/com/mchub/config/RateLimitFilter.java`

**Fix đã áp dụng:** Thêm `otpEmailBuckets` — rate limit riêng theo email (10 req/10 phút) áp dụng cho `/verify-otp`, `/verify-admin-login-otp`, `/resend-otp`, đọc `email` từ request body qua `ContentCachingRequestWrapper` (không phá luồng đọc body của controller phía sau). Đóng gap "attacker rotate nhiều IP để né giới hạn theo IP".

---

### ✅ H4 — Guest voice analysis dùng `getRemoteAddr()` thay vì `X-Forwarded-For` — FIXED
**File:** `src/main/java/com/mchub/controllers/VoiceController.java:212`

**Fix đã áp dụng:** Đổi `request.getRemoteAddr()` → `ClientIpResolver.resolve(request)`, nhất quán với `RateLimitFilter` (xem H2).

---

## 🟡 Trung bình

### ✅ M1 — CORS wildcard toàn bộ subdomain Vercel — FIXED
**File:** `src/main/java/com/mchub/config/SecurityConfig.java`, `application.properties`, `.env.example`

**Vấn đề (trước fix):** `origins.add("https://*.vercel.app")` hardcode — bất kỳ project Vercel nào (kể cả người lạ) đều pass CORS check.

**Fix đã áp dụng:** Xóa wildcard hardcode. Thêm property `mchub.cors.vercel-preview-patterns` (env `VERCEL_PREVIEW_PATTERNS`), mặc định **rỗng** — opt-in, chỉ set pattern cụ thể cho project của mình (vd `https://mc-voice-training-*.vercel.app`) nếu cần preview-deploy domain động. Documented trong `.env.example` với cảnh báo rõ không được set thành `*.vercel.app`.

---

### ⏳ M2 — Không có cơ chế revoke/blacklist token khi logout — CHƯA FIX
**File:** `src/main/java/com/mchub/config/JwtAuthenticationFilter.java`

**Lý do chưa fix:** Cần thêm infrastructure mới (Redis, hoặc Mongo TTL collection lưu token đã revoke) — vượt phạm vi "sửa code hiện có", cần quyết định kiến trúc riêng (Redis thêm 1 service phải deploy/maintain, hay TTL collection trong Mongo hiện có — trade-off latency vs. đơn giản hạ tầng).

**Khuyến nghị:** Follow-up riêng — đề xuất dùng Mongo TTL collection (`revoked_tokens`, index TTL = `jwtExpiration`) vì không cần thêm hạ tầng mới, `JwtAuthenticationFilter` check thêm 1 query trước khi authenticate.

---

### ⏳ M3 — Admin 2FA phụ thuộc hoàn toàn vào 1 email đích — CHƯA FIX
**File:** `src/main/java/com/mchub/services/impl/AuthServiceImpl.java`

**Lý do chưa fix:** Thêm TOTP (Google Authenticator) là tính năng mới (UI setup QR code, secret storage, verify flow riêng) — không phải bug fix, cần thiết kế UX riêng cho admin settings.

**Khuyến nghị:** Follow-up riêng nếu số lượng admin/giá trị tài sản bảo vệ đủ lớn để đầu tư — ở quy mô hiện tại (email OTP + rate limit theo email đã fix ở H3) là chấp nhận được tạm thời.

---

## 🟢 Đã kiểm tra — không phải lỗi

- **`RegisterRequest.java`** — đã có `@Size(min=8, max=128)` cho password.
- **`GlobalExceptionHandler.java`** — bắt `Exception.class` chung, trả message generic, không leak stack trace.
- **Password hashing** — BCrypt cost factor mặc định 10, chấp nhận được.
- **PayOS webhook** — verify HMAC signature trước khi xử lý, đúng chuẩn.
- **`passwordChangedAt` check** — token phát hành trước thời điểm đổi password bị từ chối đúng cách.

---

## Tổng kết

| Mức độ | Tổng | Đã fix | Còn lại |
|--------|------|--------|---------|
| 🔴 Critical | 2 | 2 | 0 |
| 🟠 Cao | 4 | 4 | 0 |
| 🟡 Trung bình | 3 | 1 | 2 (cần infra mới) |

**Verify:** `mvn compile` clean, `mvn test -Dtest=AuthServiceImplTest,AuthControllerTest` → 49/49 pass.

**File mới:** `src/main/java/com/mchub/util/ClientIpResolver.java`
**File sửa:** `application.properties`, `.env.example`, `JwtServiceImpl.java`, `AuthServiceImpl.java`, `RateLimitFilter.java`, `VoiceController.java`, `SecurityConfig.java`, `AuthServiceImplTest.java`

**Việc cần làm thủ công (ngoài phạm vi code):**
1. Set `JWT_SECRET` mới (32+ bytes random) trên Render production — app sẽ crash lúc start nếu thiếu, đúng như thiết kế.
2. Audit MongoDB `users` collection tìm password không phải BCrypt hash (không bắt đầu `$2a$`/`$2b$`) — force reset cho các user này vì giờ họ không login được nữa (đây là tác dụng phụ mong muốn của C2, cần thông báo trước cho user bị ảnh hưởng).
3. Xác nhận Render có set `X-Forwarded-For` đúng cách, không cho client override.
4. Nếu dùng Vercel preview deploy, set `VERCEL_PREVIEW_PATTERNS` với pattern cụ thể của project, không dùng wildcard rộng.
