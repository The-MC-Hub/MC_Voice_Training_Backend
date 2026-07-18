## DEFECT-019: `CertificateController` (add/verify/delete certificate) là stub đã deprecated nhưng vẫn active trên route thật — ném `UnsupportedOperationException` không bắt được, trả HTTP 500 "System error" gây hiểu lầm là lỗi hệ thống

- **Module:** User & MC Profile (Certificate — legacy manual flow)
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-PROF-09 (system test UC-02).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `POST /api/v1/certificates` bằng JWT MC hợp lệ, có `mcProfile` hợp lệ, trên `mchub_test`.

### Root Cause

`CertificateServiceImpl.java` có comment rõ ràng ở đầu file:
```java
/**
 * Manual MC certificate system is deprecated.
 * Certificates are now auto-issued on course quiz completion via CourseService.
 * These stubs keep the old CertificateController compiling during migration.
 */
```
Cả 3 method ghi (`addCertificate`, `verifyCertificate`, `deleteCertificate`) đều là stub ném `UnsupportedOperationException` với message giải thích rõ ràng lý do deprecated — đây là code CHỦ ĐÍCH giữ lại để compile được trong giai đoạn migrate sang hệ thống certificate mới (gắn liền UC-04 course completion, đã QA xác nhận hoạt động đúng ở phiên test trước — xem `UC-04-Courses-Learning_TestCases.md` TC-COURSE-17/18/19).

Tuy nhiên, `CertificateController` vẫn **active hoàn toàn trên route thật** (`/api/v1/certificates`, không có version cũ/mới tách biệt, không có deprecation warning trả về client), và `UnsupportedOperationException` **không được `GlobalExceptionHandler` xử lý riêng** — rơi vào nhánh catch-all generic, trả HTTP 500 "System error, please try again later" y hệt lỗi crash hệ thống thật, dù bản chất đây là hành vi ĐÃ BIẾT TRƯỚC và CHỦ ĐÍCH (không phải bug ngẫu nhiên).

### Impact nghiệp vụ

- Client (frontend/mobile) gọi các endpoint này (nếu UI cũ chưa được gỡ theo kịp migration) sẽ nhận thông báo lỗi hệ thống chung chung, không có cách nào phân biệt "đây là tính năng đã ngừng hỗ trợ, dùng luồng khác" với "server thật sự đang lỗi" — gây khó khăn debug và trải nghiệm người dùng tệ (MC tưởng hệ thống lỗi khi họ cố thêm chứng chỉ theo cách cũ).
- `GET /certificates/mc/{mcProfileId}` (route đọc, feature UC-02 #6) vẫn hoạt động bình thường (không phải stub — có `getCertificatesByMCProfile` triển khai thật, dùng `findByUserId`) — chỉ 3 route GHI (POST/PUT-verify/DELETE) bị ảnh hưởng.
- Rủi ro về mặt tài liệu API: nếu Swagger/API-docs chưa đánh dấu các endpoint này là deprecated, dev/frontend mới vào dự án dễ vô tình tích hợp nhầm vào luồng đã bỏ.

### Evidence

```
$ curl -X POST "http://localhost:5555/api/v1/certificates" -H "Authorization: Bearer <MC-JWT>" \
    -d '{"name":"Chung chi MC chuyen nghiep","issuer":"Hoc vien Bao chi","issueDate":"2023-05-01"}'
HTTP 500 {"status":"error","message":"System error, please try again later","errorCode":"ERR_9001"}

# Server log xác nhận nguyên nhân thật:
java.lang.UnsupportedOperationException: Manual certificates are deprecated. Certificates are issued automatically upon course completion.
```

Source: `CertificateServiceImpl.java` dòng 26-29/32-35/44-47; `CertificateController.java` — không có xử lý riêng cho trường hợp deprecated.

### Status

**Open.** Đề xuất dev (không phải QA quyết định): 1 trong 2 hướng
1. Nếu route này vẫn cần giữ tạm thời cho backward-compat: thêm `@ExceptionHandler(UnsupportedOperationException.class)` trong `GlobalExceptionHandler` trả về HTTP 410 Gone (hoặc 400) kèm message rõ ràng "Tính năng này đã ngừng hỗ trợ, chứng chỉ được cấp tự động khi hoàn thành khoá học" — thay vì lọt xuống nhánh 500 generic.
2. Nếu route này không còn ai gọi (frontend đã migrate xong sang luồng course completion): xoá hẳn `CertificateController`, `CertificateService`, `CertificateServiceImpl` và route liên quan khỏi codebase — tránh code chết gây nhầm lẫn, đúng nguyên tắc dọn dẹp sau migration.
