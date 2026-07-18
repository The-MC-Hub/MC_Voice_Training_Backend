## DEFECT-015: `GET /auth/me` (và có thể các endpoint khác dựa vào `SecurityUtils.getCurrentUserId()` mà không có `@PreAuthorize`) trả HTTP 500 thay vì 401 khi Guest gọi không kèm JWT

- **Module:** Authentication / SecurityUtils
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-AUTH-16 (system test UC-01).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `GET /api/v1/auth/me` không kèm JWT trên `mchub_test`.

### Root Cause

`AuthController.getMe()` không có `@PreAuthorize`, dựa hoàn toàn vào `SecurityUtils.getCurrentUserId()` để lấy userId:
```java
public static String getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
        throw new IllegalStateException("User not authenticated");
    }
    Object principal = auth.getPrincipal();
    if (principal == null || "anonymousUser".equals(principal.toString())) {
        throw new IllegalStateException("Could not determine current user");
    }
    return principal.toString();
}
```
Khi Guest gọi (không có JWT hoặc `SecurityConfig` không chặn được ở tầng filter trước khi vào controller), Spring Security gán principal là `anonymousUser` — `SecurityUtils.getCurrentUserId()` ném `IllegalStateException`. Exception này KHÔNG được `GlobalExceptionHandler` map riêng sang HTTP 401/403 — rơi vào nhánh catch-all generic, trả về HTTP 500 "System error, please try again later" thay vì mã lỗi đúng ngữ nghĩa (401 Unauthorized).

Route `/api/v1/auth/me` không nằm trong danh sách permitAll của `SecurityConfig` (đúng thiết kế — cần đăng nhập), và cũng không match `.anyRequest().authenticated()` theo cách khiến Spring Security tự trả 401/403 trước khi vào controller — có khả năng do cấu hình JWT filter cho phép request đi qua (không có token vẫn set `SecurityContext` thành anonymous thay vì chặn ở tầng filter), đẩy trách nhiệm validate xuống tận logic nghiệp vụ trong `SecurityUtils`, nơi lại ném sai loại exception.

### Impact nghiệp vụ

- Sai mã lỗi HTTP gây khó khăn cho frontend/mobile client phân biệt "lỗi hệ thống thật" (500, cần retry/báo lỗi khác) với "chưa đăng nhập" (401, cần redirect tới trang login) — logic xử lý lỗi phía client dựa theo status code chuẩn REST sẽ hoạt động sai.
- Che giấu bản chất lỗi thật — dev debug log sẽ thấy tràn ngập log `ERROR ... System error` cho trường hợp hoàn toàn bình thường (user chưa đăng nhập gọi nhầm endpoint), làm nhiễu log thật cần chú ý.
- Khả năng ảnh hưởng diện rộng — bất kỳ endpoint nào khác dùng trực tiếp `SecurityUtils.getCurrentUserId()` mà không có `@PreAuthorize("isAuthenticated()")` phía trên (QA chưa rà soát hết toàn bộ codebase để liệt kê danh sách đầy đủ) đều có nguy cơ gặp cùng lỗi này.

### Evidence

```
$ curl -i "http://localhost:5555/api/v1/auth/me"
HTTP/1.1 500
{"status":"error","message":"System error, please try again later","data":null,"success":false,"errorCode":"ERR_9001"}

# Server log xác nhận root cause:
java.lang.IllegalStateException: Could not determine current user
	at com.mchub.util.SecurityUtils.getCurrentUserId(SecurityUtils.java:19)
```

Source: `SecurityUtils.java` dòng 15/19 — ném `IllegalStateException` (không phải `AppException` với `ErrorCode` phù hợp); `AuthController.java` — `getMe()` không có `@PreAuthorize`.

### Cập nhật 2026-07-18 — xác nhận thêm 1 endpoint khác cùng lỗi

Phát hiện thêm trong system test UC-05 (TC-COMM-06): `GET /api/v1/community/leaderboard/me` cũng thiếu `@PreAuthorize`, dùng trực tiếp `SecurityUtils.getCurrentUserId()` trong `CommunityController.getMyRank()` — gọi không kèm JWT cũng trả HTTP 500 y hệt `/auth/me`. Xác nhận đây đúng là lỗi hệ thống/pattern lặp lại (không phải case đơn lẻ), củng cố đề xuất fix ở tầng 2 (bổ sung `@ExceptionHandler(IllegalStateException.class)` trong `GlobalExceptionHandler`) thay vì chỉ vá từng endpoint riêng lẻ.

```
$ curl -i "http://localhost:5555/api/v1/community/leaderboard/me"
HTTP/1.1 500 {"message":"System error, please try again later"}
```

Lưu ý đối chứng: cùng Controller này có `getActiveArenas()` xử lý ĐÚNG cách (dùng `try { SecurityUtils.getCurrentUserId() } catch (Exception e) { /* unauthenticated */ }` để cho phép Guest truy cập) — chứng minh dev đã biết cách xử lý đúng ở 1 chỗ nhưng áp dụng không nhất quán across codebase.

### Status

**Open.** Đề xuất dev (không phải QA quyết định): 2 hướng khắc phục, có thể kết hợp cả 2:
1. Thêm `@PreAuthorize("isAuthenticated()")` cho `getMe()` (và rà soát các method tương tự) để Spring Security tự trả 401/403 TRƯỚC khi vào logic nghiệp vụ — tương tự cách `generateReferralCodeEndpoint()` đã làm đúng trong cùng file (`@PreAuthorize("isAuthenticated()")` dòng 156).
2. Bổ sung `@ExceptionHandler(IllegalStateException.class)` trong `GlobalExceptionHandler` map về HTTP 401 — phòng trường hợp còn chỗ khác trong codebase gọi `SecurityUtils.getCurrentUserId()` mà thiếu `@PreAuthorize` (bảo vệ tầng 2, không phụ thuộc dev nhớ thêm annotation đúng mọi chỗ).
