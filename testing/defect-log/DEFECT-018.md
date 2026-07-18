## DEFECT-018: `@PreAuthorize` của `getDashboardStats()` không có điều kiện kiểm tra role MC — bất kỳ CLIENT nào cũng gọi được `/mcs/dashboard` (endpoint dành riêng cho MC)

- **Module:** User & MC Profile (MC Dashboard)
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-PROF-06 (system test UC-02).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `GET /api/v1/mcs/dashboard` bằng JWT của user role CLIENT trên `mchub_test`.

### Root Cause

`MCProfileService.java`:
```java
@PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.name")
Map<String, Object> getDashboardStats(String userId);
```

`MCController.getDashboard()`:
```java
String userId = SecurityUtils.getCurrentUserId();
Map<String, Object> stats = mcProfileService.getDashboardStats(Objects.requireNonNull(userId));
```

Vấn đề: `userId` truyền vào LUÔN LÀ chính id của user đang gọi request (lấy từ JWT `sub` claim qua `SecurityUtils.getCurrentUserId()`), không phải id do client tự chọn truyền vào. Điều kiện `#userId == authentication.name` do đó **LUÔN LUÔN đúng với BẤT KỲ user nào gọi endpoint này cho chính họ** — không phân biệt role CLIENT/MC/ADMIN. Về bản chất, `@PreAuthorize` này chỉ kiểm tra "user có đang gọi cho chính họ không" (ownership check, luôn pass trong trường hợp này) — hoàn toàn không kiểm tra role MC như đặc tả nghiệp vụ yêu cầu (UC-02 tính năng #3: "MC xem dashboard MC").

Đã verify: `@PreAuthorize` bản thân CÓ hoạt động đúng cơ chế Spring Security (không phải lỗi proxy/AOP) — bằng chứng: `updateProfile()` trong CÙNG interface với `@PreAuthorize("hasAuthority('MC')")` (không có phần ownership check) chặn đúng CLIENT với HTTP 403. Vậy lỗi nằm ở THIẾT KẾ điều kiện SpEL của riêng `getDashboardStats()` — thiếu vế `hasAuthority('MC')`, chỉ có ownership check vô nghĩa trong ngữ cảnh này.

### Impact nghiệp vụ

Bất kỳ CLIENT nào (không có role MC) đều gọi được `GET /mcs/dashboard` và nhận về dữ liệu thống kê hợp lệ (dù chỉ là dữ liệu của chính họ — không rò rỉ dữ liệu người khác, vì `userId` luôn lấy từ chính JWT của người gọi). Vi phạm ranh giới vai trò nghiệp vụ (endpoint dành riêng cho MC persona) dù không có rò rỉ dữ liệu thực tế — CLIENT thấy được 1 tính năng UI/API vốn không dành cho họ, có thể gây nhầm lẫn trải nghiệm hoặc bị khai thác nếu sau này endpoint mở rộng thêm dữ liệu nhạy cảm hơn mà vẫn dựa vào cùng rule sai này.

### Evidence

```
$ curl "http://localhost:5555/api/v1/mcs/dashboard" -H "Authorization: Bearer <CLIENT-role JWT>"
HTTP 200 {"data":{"avgWpm":0.0,"totalPractices":0,"avgAccuracy":0.0}}
```
Kỳ vọng: 403 (CLIENT không có quyền, endpoint dành cho MC) — nhưng nhận 200 thành công.

Đối chứng — `updateProfile()` với rule đúng (`hasAuthority('MC')` không có ownership check) chặn đúng:
```
$ curl -X PUT "http://localhost:5555/api/v1/mcs/profile" -H "Authorization: Bearer <CLIENT-role JWT>" -d '{"biography":"..."}'
HTTP 403 {"message":"You do not have permission to perform this action"}
```
Xác nhận DB: không có `mcprofiles` document nào được tạo cho user CLIENT — `updateProfile` bị chặn đúng, chỉ riêng `getDashboardStats` bị lỗi.

Source: `MCProfileService.java` — `@PreAuthorize("hasAuthority('ADMIN') or #userId == authentication.name")` trên `getDashboardStats`.

### Status

**Open.** Đề xuất dev (không phải QA quyết định): sửa điều kiện thành `hasAuthority('ADMIN') or (hasAuthority('MC') and #userId == authentication.name)` — vừa giữ được ý nghĩa ownership check (phòng trường hợp sau này endpoint được mở rộng nhận `userId` tham số từ client thay vì luôn tự lấy từ JWT), vừa bổ sung đúng điều kiện role MC còn thiếu.
