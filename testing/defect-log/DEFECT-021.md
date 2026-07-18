## DEFECT-021: `GET /public/mcs` / `GET /public/mcs/{id}` lộ email thật của MC trên endpoint công khai không cần đăng nhập — và field `verified` gây hiểu lầm (chỉ là email đã xác minh, không phải "MC đã được xác minh chuyên môn")

- **Module:** User & MC Profile (Public MC Discovery)
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-PROF-15/17 (system test UC-02).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — `GET /api/v1/public/mcs` không kèm JWT trên `mchub_test`.

### Root Cause — Phần 1: lộ email

`MCProfileMapper.java`:
```java
@Mapping(target = "email", source = "user.email")
```
`MCProfileResponseDTO` (dùng chung cho cả `discoverMCs()` và `getMCProfile()`, cả 2 đều PUBLIC — whitelist trong `SecurityConfig.java` dòng 57: `.requestMatchers("/api/v1/public/**").permitAll()`) trả về trực tiếp `user.email` — địa chỉ email thật của MC, hiển thị cho BẤT KỲ ai truy cập trang khám phá MC mà không cần đăng nhập.

### Evidence

```
$ curl "http://localhost:5555/api/v1/public/mcs"
{"data":{"mcs":[{"id":"...","email":"qa.mc.uc02@mchubtest.local", ...}]}}
```
Không cần JWT, không cần bất kỳ xác thực nào.

### Impact nghiệp vụ — Phần 1

Rò rỉ PII (email) hàng loạt qua endpoint công khai — bất kỳ ai (kể cả bot scraping) đều thu thập được toàn bộ email của MC trong hệ thống chỉ bằng 1 lệnh gọi API không xác thực. Rủi ro: spam, phishing nhắm vào MC, vi phạm nguyên tắc bảo vệ dữ liệu cá nhân cơ bản (email không nên là thông tin công khai mặc định trên trang profile marketing/khám phá, trừ khi MC chủ động chọn hiển thị).

### Root Cause — Phần 2: field `verified` gây hiểu lầm ngữ nghĩa

```java
@Mapping(target = "verified", source = "user.verified")
```
Field `verified` trong `MCProfileResponseDTO` lấy từ `User.isVerified` — đây là cờ **xác minh email lúc đăng ký** (`isVerified = true` ngay sau khi user hoàn tất OTP verify email, xem `AuthController.verifyOtp()`), HOÀN TOÀN không liên quan tới việc "hồ sơ MC đã được xác minh chuyên môn/chứng chỉ" mà một badge "verified" trên trang khám phá MC công khai thường ngụ ý.

Thực nghiệm xác nhận: MC test trong session này (`qa.mc.uc02@mchubtest.local`) có `verified:true` ngay sau khi verify email — dù CHƯA từng có certificate nào được duyệt (route thêm certificate đang bị lỗi hoàn toàn, xem DEFECT-019) và tài khoản MC vừa tạo, chưa từng được Admin xem xét/duyệt hồ sơ.

### Impact nghiệp vụ — Phần 2

Người dùng công khai xem trang khám phá MC thấy badge "verified" (hoặc tương đương trên UI) trên MỌI MC (vì hầu như 100% MC đã verify email khi đăng ký) — badge này về bản chất luôn `true` và không mang giá trị phân biệt "MC uy tín/đã xác minh chuyên môn" với "MC mới đăng ký, chưa ai kiểm chứng gì" — làm mất tác dụng tin cậy (trust signal) mà 1 badge "verified" đáng lẽ phải mang lại, có thể gây hiểu lầm cho khách hàng khi chọn MC.

### Status

**Open.** Đề xuất dev (không phải QA quyết định):
1. **Phần 1 (email):** loại bỏ field `email` khỏi `MCProfileResponseDTO` dùng cho public endpoint, hoặc tách riêng 1 DTO khác cho luồng public (không chứa email) — chỉ giữ email trong response cho chính chủ MC xem hồ sơ của mình (`/mcs/profile` — nếu có endpoint GET riêng) hoặc Admin.
2. **Phần 2 (verified):** đổi nguồn field `verified` sang phản ánh đúng "hồ sơ MC đã được Admin xác minh/duyệt" (VD: dựa vào việc MC có ít nhất 1 certificate đã verify, hoặc thêm field `MCProfile.isVerifiedByAdmin` riêng) — không dùng chung với `User.isVerified` (email verification), tránh nhầm lẫn ngữ nghĩa giữa 2 khái niệm "verified" hoàn toàn khác nhau.
