## DEFECT-022: `POST /social-posts/{id}/click` trả HTTP 403 cho Guest — thiếu whitelist trong SecurityConfig (cùng nhóm nguyên nhân với DEFECT-001/010/013)

- **Module:** Community & Leaderboard (Social Post click tracking) / SecurityConfig
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-COMM-10 (system test UC-05).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `POST /api/v1/social-posts/{id}/click` không kèm JWT trên `mchub_test`.

### Root Cause

`SocialPostController.recordClick()` không có `@PreAuthorize` — cùng Controller với `getActive()` (đã whitelist đúng, GET công khai). Theo đặc tả UC-05 tính năng #9 "Hệ thống ghi nhận lượt click vào bài đăng mạng xã hội để phân tích" — đây rõ ràng là hành động của BẤT KỲ khách truy cập nào (kể cả chưa đăng nhập) khi họ click vào bài đăng hiển thị công khai trên trang chủ/landing page.

`SecurityConfig.java` chỉ whitelist:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/social-posts").permitAll()
```
Không có dòng whitelist cho `POST /api/v1/social-posts/{id}/click` — do rule mặc định `.anyRequest().authenticated()`, Guest click vào bài đăng công khai bị chặn 403.

### Impact nghiệp vụ

Toàn bộ số liệu phân tích lượt click bài đăng mạng xã hội (UC-05 #9) chỉ ghi nhận được từ user ĐÃ đăng nhập — bỏ sót hoàn toàn lượt click từ khách vãng lai (phần lớn traffic marketing thực tế thường là Guest chưa đăng nhập). Dữ liệu phân tích hiệu quả bài đăng bị sai lệch nghiêm trọng, ảnh hưởng quyết định marketing dựa trên số liệu này.

### Evidence

```
$ curl -i -X POST "http://localhost:5555/api/v1/social-posts/{id}/click"
HTTP/1.1 403
Content-Length: 0
```
Đối chiếu code: `SocialPostController.java` — method không có `@PreAuthorize`; `SecurityConfig.java` — chỉ whitelist GET, thiếu POST click.

### Status

**Fixed (2026-07-18).** Thêm rule vào `SecurityConfig.java` (cùng commit với DEFECT-001/010/013):
```java
.requestMatchers(HttpMethod.POST, "/api/v1/social-posts/*/click").permitAll()
```
**Verify live (không JWT, dùng post ID thật):** `POST /social-posts/{id}/click` → 200 "Click recorded" (trước fix: 403). `SocialPostControllerTest` 2/2 PASS, không hồi quy.

Lưu ý phụ phát hiện khi verify: gọi với ID KHÔNG tồn tại trả 500 thay vì 404 (`SocialPostServiceImpl` cũng dùng `RuntimeException` trần, cùng pattern với DEFECT-024 nhưng ở service khác) — đây là defect MỚI chưa nằm trong 24 defect gốc, không thuộc phạm vi fix của DEFECT-022 (vốn chỉ về whitelist/403). Ghi nhận riêng, không tự ý mở rộng fix ngoài phạm vi đã yêu cầu.
