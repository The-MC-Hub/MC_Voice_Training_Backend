## DEFECT-017: `POST /users/me/streak/freeze` không thực hiện hành động nào — chỉ trả lại streak hiện tại, không tiêu tốn lượt freeze như tên/method gợi ý

- **Module:** User Profile (Streak / Gamification)
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-PROF-04 (system test UC-02).
- **Severity:** Minor
- **Priority:** P2
- **Môi trường:** LIVE — gọi thật `POST /api/v1/users/me/streak/freeze` trên `mchub_test`.

### Root Cause

`UserController.useFreeze()`:
```java
@PostMapping("/me/streak/freeze")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<LoginStreakDTO>> useFreeze() {
    String userId = SecurityUtils.getCurrentUserId();
    UserStats stats = gamificationService.getOrCreateUserStats(userId);

    if (stats.getFreezesAvailable() <= 0) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("Không còn lượt freeze. Freeze được nạp lại vào đầu tháng."));
    }
    // Freeze is consumed automatically in processLoginStreak when gap==2.
    // This endpoint lets user manually see / confirm their freeze count.
    return getLoginStreak();
}
```
Comment trong code xác nhận rõ: cơ chế tiêu freeze thực sự nằm trong `GamificationServiceImpl.processLoginStreak()` (tự động kích hoạt khi user đăng nhập sau 2 ngày vắng mặt — `gap==2`), HOÀN TOÀN không liên quan tới endpoint `POST /users/me/streak/freeze` này.

Endpoint `useFreeze()` chỉ kiểm tra `freezesAvailable > 0` rồi gọi lại `getLoginStreak()` (không có side-effect nào), trả về y hệt kết quả `GET /users/me/streak`. Gọi endpoint này bao nhiêu lần cũng không thay đổi `freezesAvailable`.

### Impact nghiệp vụ

Đặc tả UC-02 tính năng #2 mô tả: "User dùng lượt đóng băng để bảo toàn streak khi bỏ lỡ một ngày luyện tập" — ngữ nghĩa này ngụ ý user **chủ động** dùng 1 nút "Freeze" để chủ động bảo vệ streak (pattern phổ biến trong app gamification như Duolingo). Nhưng thực tế cơ chế đang là **hoàn toàn tự động, bị động** (chỉ kích hoạt khi đăng nhập sau khi đã bỏ lỡ đúng 1 ngày, `gap==2`) — user không hề có quyền chủ động "dùng trước" 1 freeze để phòng ngừa việc sắp bỏ lỡ ngày mai, và endpoint tồn tại với tên/method (`POST .../freeze`) gây hiểu lầm rằng gọi nó sẽ có tác dụng.

Nếu frontend hiện có nút "Dùng Freeze" gọi endpoint này với kỳ vọng người dùng chủ động bảo toàn streak trước khi lỡ hẹn (use case chính đáng — VD: biết trước ngày mai sẽ không luyện tập được) — tính năng đó **không tồn tại thực sự trên backend**, người dùng bấm nút không có tác dụng gì, dễ gây hiểu lầm mất lòng tin vào tính năng.

### Evidence

```
$ curl "http://localhost:5555/api/v1/users/me/streak" -H "Authorization: Bearer <jwt>"
{"data":{"freezesAvailable":1, ...}}

$ curl -X POST "http://localhost:5555/api/v1/users/me/streak/freeze" -H "Authorization: Bearer <jwt>"
{"data":{"freezesAvailable":1, ...}}   ← không đổi, y hệt trước khi gọi

$ curl "http://localhost:5555/api/v1/users/me/streak" -H "Authorization: Bearer <jwt>"
{"data":{"freezesAvailable":1, ...}}   ← vẫn không đổi sau khi gọi freeze
```

Source: `UserController.java` — `useFreeze()`; `GamificationServiceImpl.java` dòng 144-148 — logic tiêu freeze thật nằm trong `processLoginStreak()`, tách biệt hoàn toàn khỏi endpoint trên.

### Status

**Open.** Đề xuất dev (không phải QA quyết định) xác nhận lại chủ đích thiết kế với Product Owner: nếu tính năng CHỦ ĐỘNG dùng freeze trước là mong muốn thật sự (khớp đặc tả UC-02 #2), cần bổ sung logic thực sự trong `useFreeze()` (VD: đánh dấu ngày hôm nay là "đã bảo vệ", trừ `freezesAvailable`, cập nhật cờ để lần login tiếp theo không tính là "gap" dù có bỏ lỡ). Nếu thiết kế chủ đích chỉ là cơ chế tự động bị động (đúng như code hiện tại), nên đổi tên endpoint (VD: `GET /users/me/streak/freeze-status`) và bỏ method `POST` gây hiểu lầm, đồng thời cập nhật lại mô tả đặc tả UC-02 #2 cho khớp hành vi thật.
