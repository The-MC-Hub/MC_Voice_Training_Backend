## DEFECT-010: `GET /voice/guest-cooldown-hours` trả HTTP 403 cho Guest — thiếu whitelist trong SecurityConfig (cùng nhóm nguyên nhân với DEFECT-001)

- **Module:** Voice Training (Guest cooldown display) / SecurityConfig
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-VOICE-26 (system test UC-03).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `GET /api/v1/voice/guest-cooldown-hours` không kèm JWT trên `mchub_test`.

### Root Cause

`VoiceController.getGuestCooldownHours()` không có `@PreAuthorize`, tên method và mục đích (hiển thị số giờ cooldown còn lại cho Guest TRƯỚC khi họ dùng thử tính năng phân tích giọng) rõ ràng cho thấy đây là endpoint công khai — nhưng `SecurityConfig.java` không có dòng whitelist tương ứng:

```java
.requestMatchers(HttpMethod.GET, "/api/v1/voice/lessons/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/voice/lessons").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/voice/practice/analyze-guest").permitAll()
```
Chỉ có 3 route voice được whitelist — thiếu `GET /api/v1/voice/guest-cooldown-hours`. Do rule mặc định `.anyRequest().authenticated()` ở cuối, mọi request không match whitelist đều cần JWT, khiến Guest gọi endpoint này nhận HTTP 403.

**Đây là cùng một loại lỗi với DEFECT-001** (thiếu whitelist SecurityConfig cho endpoint công khai) — nhưng là một endpoint khác (`/voice/guest-cooldown-hours` thay vì `/payment/plans`, `/payment/flash-deals`, `/payment/apply-discount`), phát hiện ở module khác (UC-03 thay vì UC-06). Ghi thành defect riêng để dev có thể fix cả 4 endpoint cùng lúc trong 1 lần sửa `SecurityConfig.java`.

### Impact nghiệp vụ

Frontend không thể hiển thị cho Guest biết "còn bao lâu nữa được dùng thử tiếp" trước khi họ bấm nút — Guest chỉ biết bị chặn SAU khi thử phân tích giọng và nhận lỗi 400 "Bạn đã sử dụng lượt thử miễn phí...". Ảnh hưởng UX, không phải lỗi bảo mật/mất dữ liệu.

### Evidence

```
$ curl "http://localhost:5555/api/v1/voice/guest-cooldown-hours"
HTTP 403 (Content-Length: 0)
```
Đối chiếu code: `VoiceController.java` — method không có `@PreAuthorize`; `SecurityConfig.java` — không có whitelist entry khớp path này.

### Status

**Fixed (2026-07-18).** Thêm rule vào `SecurityConfig.java` (cùng commit với DEFECT-001/013/022):
```java
.requestMatchers(HttpMethod.GET, "/api/v1/voice/guest-cooldown-hours").permitAll()
```
**Verify live (không JWT):** `GET /voice/guest-cooldown-hours` → 200 `{"hours":3}`. `VoiceControllerTest` 17/17 PASS, không hồi quy.
