## DEFECT-016: `JwtAuthenticationFilter` từ chối JWT hợp lệ phát hành SAU khi đổi mật khẩu — lệch múi giờ giữa `LocalDateTime.now()` (giờ local server) và `ZoneOffset.UTC` khi so sánh `passwordChangedAt`

- **Module:** Authentication (Reset Password / JWT session invalidation)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-AUTH-23/25 (system test UC-01), sau khi `reset-password` thành công và đăng nhập lại bằng mật khẩu mới.
- **Severity:** Critical
- **Priority:** P0
- **Môi trường:** LIVE — `mchub_test`, máy chủ chạy trên Windows với múi giờ hệ thống UTC+7 (Asia/Ho_Chi_Minh).

### Root Cause

`AuthServiceImpl.java` (3 chỗ: dòng 253, 341, 364 — bao gồm luồng `resetPassword`) ghi nhận thời điểm đổi mật khẩu bằng:
```java
user.setPasswordChangedAt(LocalDateTime.now());
```
`LocalDateTime.now()` lấy giờ theo **múi giờ hệ thống JVM** (máy chủ chạy Windows, múi giờ mặc định UTC+7) — trả về "giờ tường" (wall-clock) local, KHÔNG phải UTC, và hoàn toàn không mang thông tin múi giờ (naive).

`JwtAuthenticationFilter.java` dòng 65 khi so sánh lại hiểu sai giá trị này là UTC:
```java
long pwChangedMs = user.getPasswordChangedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
if (issuedAt.getTime() < pwChangedMs) { ... reject ... }
```
`toInstant(ZoneOffset.UTC)` ép buộc coi các chữ số giờ:phút:giây đã lưu (vốn là giờ local +7) như thể chúng là giờ UTC — kết quả là mốc thời gian `passwordChangedAt` bị dịch sai lên **sớm hơn thực tế 7 tiếng theo hướng ngược** (giờ local 15:55 bị hiểu thành UTC 15:55, trong khi UTC thực tế lúc đó là 08:55) — nhưng vì JWT `issuedAt` được `JwtService` phát hành đúng chuẩn UTC (dùng thư viện JWT chuẩn, tự động dùng `Instant`/`Date` UTC), kết quả so sánh bị lệch **7 tiếng**, khiến MỌI token phát hành trong vòng khoảng 7 tiếng SAU khi đổi mật khẩu vẫn bị coi là "issued before password change" và bị từ chối — dù thực tế token được phát hành sau đó.

Thực nghiệm xác nhận: đổi mật khẩu lúc `15:55:20` (giờ ghi trong Mongo, thực chất là giờ local +7 nên UTC thực là `08:55:20`), sau đó login lại lúc UTC thật `15:56:44` (mới hơn thời điểm đổi mật khẩu tới ~7 tiếng theo giờ UTC thật) — token vẫn bị từ chối, vì so sánh sai coi `passwordChangedAt` là `15:55:20 UTC` (chỉ mới hơn token 84 giây, đúng ra phải hiểu đây là thời điểm TRƯỚC token rất lâu nếu tính đúng theo UTC thật).

### Impact nghiệp vụ — NGHIÊM TRỌNG

**Bất kỳ user nào đổi mật khẩu (qua `reset-password`, hoặc 2 luồng khác dùng cùng pattern ở dòng 253/364 — cần QA xác nhận thêm là luồng nào, khả năng là "change password" khi đã đăng nhập và luồng admin reset hộ) đều KHÔNG THỂ ĐĂNG NHẬP LẠI BÌNH THƯỜNG trong vòng nhiều giờ sau đó** — mọi JWT mới phát hành trong cửa sổ lệch múi giờ (~7 tiếng, tuỳ theo lúc đổi mật khẩu rơi vào giờ nào trong ngày UTC) đều bị `JwtAuthenticationFilter` âm thầm từ chối, khiến mọi API cần xác thực trả về **HTTP 500** (do exception `IllegalStateException` không được xử lý đúng — xem thêm DEFECT-015) thay vì cho phép truy cập.

Đây là lỗi **chặn hoàn toàn luồng nghiệp vụ cốt lõi** (đổi mật khẩu xong không dùng được tài khoản), không phải edge case hiếm — xảy ra với MỌI lần đổi mật khẩu, tại MỌI thời điểm trong ngày mà UTC hiện tại nhỏ hơn (giờ local +7 vừa lưu). Với máy chủ đặt tại Việt Nam (UTC+7) chạy 24/7, cửa sổ lỗi này lặp lại liên tục theo chu kỳ ngày — cụ thể: nếu đổi mật khẩu vào khoảng 00:00–07:00 giờ UTC (07:00–14:00 giờ Việt Nam), user hoàn toàn không đăng nhập lại được cho tới khi qua mốc UTC tương ứng.

### Evidence

```
# DB: passwordChangedAt sau khi reset-password thành công lúc thực tế UTC = 2026-07-17T08:55:20 UTC
db.users.findOne({email:"..."}, {passwordChangedAt:1})
{ passwordChangedAt: ISODate('2026-07-17T15:55:20.654Z') }   ← lệch +7h so với UTC thật (bị lưu như giờ local)

# Login lại NGAY LẬP TỨC (84 giây sau, UTC thật) — token mới hoàn toàn:
$ curl -X POST ".../auth/login" -d '{"email":"...","password":"NewPass@123"}'
→ token với iat=2026-07-17T15:56:44 UTC (đúng UTC thật)

$ curl "http://localhost:5555/api/v1/auth/me" -H "Authorization: Bearer <token mới>"
HTTP 500 {"message":"System error, please try again later"}

# Server log xác nhận nguyên nhân:
WARN c.m.config.JwtAuthenticationFilter - ⚠️ [JWT] Token issued before password change — userId=...
```

Source: `AuthServiceImpl.java` dòng 253/341/364 — `LocalDateTime.now()` không dùng UTC rõ ràng; `JwtAuthenticationFilter.java` dòng 65 — `toInstant(ZoneOffset.UTC)` ép sai múi giờ khi convert.

### Status

**Fixed (2026-07-18).** Áp dụng hướng 1: đổi cả 3 call site trong `AuthServiceImpl.java` (`updatePasswordAsync`, `resetPassword`, `updateSettings`) từ `LocalDateTime.now()` sang `LocalDateTime.now(ZoneOffset.UTC)` — đảm bảo giá trị application-level luôn là UTC thật, khớp với cách `JwtAuthenticationFilter` diễn giải khi so sánh (`toInstant(ZoneOffset.UTC)`).

**Verify trực tiếp (live, port 5555, `mchub_test`):**
1. Reset password lúc UTC thật `05:58:26` → login lại → gọi `GET /auth/me` bằng token mới → **HTTP 200** (trước fix: 500, token luôn bị coi là "issued before password change").
2. Lặp lại lần 2 (reset lúc `06:00:06`) → cùng kết quả PASS.
3. Regression check quan trọng: dùng lại token CŨ (phát hành trước lần reset thứ 2) gọi `/auth/me` → bị từ chối đúng như thiết kế (không làm mất tác dụng bảo vệ, vẫn chặn đúng token thật sự cũ).

**Lưu ý kỹ thuật cho dev tương lai:** Khi xem trực tiếp qua `mongosh`, giá trị `passwordChangedAt` lưu trong BSON trông như lệch 7 tiếng so với đồng hồ UTC thật (VD: reset lúc UTC `06:00:06` nhưng Mongo hiển thị `ISODate('...T23:00:06Z')` — ngày hôm trước). Đây là hành vi của Spring Data MongoDB's `LocalDateTime`↔`Date` converter mặc định (áp dụng `ZoneId.systemDefault()` khi convert, máy chủ đang chạy múi giờ `Asia/Saigon` UTC+7) — **không phải bug mới**, và không ảnh hưởng logic vì cả chiều ghi (`AuthServiceImpl`) lẫn chiều đọc (`JwtAuthenticationFilter` đọc lại qua cùng entity/converter) đều đi qua cùng 1 lớp convert, nên đối xứng và cho kết quả so sánh đúng ở tầng application. Không nên "sửa" cách hiển thị BSON này — chỉ cần đảm bảo mọi so sánh xảy ra ở tầng Java (LocalDateTime/Instant), không tự parse trực tiếp giá trị BSON.
