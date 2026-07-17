## DEFECT-004: `JwtServiceImpl.isTokenValid()` throws `ExpiredJwtException` for expired tokens instead of returning `false` — plus the method is dead code

- **Module:** Auth (JwtService)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi viết unit test JUnit thật (`JwtServiceImplTest.java`), test tự nhiên fail khi giả định method trả `false` êm cho token hết hạn.
- **Severity:** Minor (không exploitable trong production — xem phân tích bên dưới)
- **Priority:** P3
- **Môi trường:** Unit test thuần (JUnit5 + JJWT thật, không cần Spring context/DB).

### Root Cause
```java
public boolean isTokenValid(String token, String userId) {
    final String extractedId = extractUserId(token);          // (1) parse token — throws HERE if expired
    return Objects.equals(extractedId, userId) && !isTokenExpired(token); // (2) never reached if expired
}
```
`extractUserId()` gọi `extractAllClaims()` → `Jwts.parser().parseSignedClaims(token)`. Thư viện JJWT ném `ExpiredJwtException` **ngay tại bước parse** nếu `exp` claim đã qua — không đợi đến bước `isTokenExpired()` so sánh `Date` thủ công ở dòng return. Code đọc như thể "trả `false` nếu hết hạn" nhưng thực tế **không bao giờ chạy tới nhánh đó** cho token hết hạn — nó throw exception chưa được catch trong chính method này.

### Tại sao KHÔNG phải bug nghiêm trọng (đã verify kỹ trước khi báo)
`grep -rn "isTokenValid" src/main/java/com/mchub/` xác nhận **không có bất kỳ nơi nào trong code production gọi `isTokenValid()`**. Luồng xác thực request thật (`JwtAuthenticationFilter.doFilterInternal`) gọi trực tiếp `extractUserId()`/`extractRole()`/`extractIssuedAt()` bên trong khối `try { ... } catch (Exception e) { log.warn(...); }` riêng của nó (dòng 54-88) — nên nếu token hết hạn, exception bị bắt ở tầng Filter, request tiếp tục xử lý như chưa đăng nhập (an toàn, đúng thiết kế). `isTokenValid()` tồn tại trong `JwtService` interface nhưng là **dead code** — không phải lỗ hổng đang bị khai thác.

### Impact nghiệp vụ
Hiện tại: không có, vì không dùng. Rủi ro tiềm ẩn: nếu tương lai có dev khác thấy method `isTokenValid(token, userId)` có vẻ tiện dùng và gọi nó ở chỗ khác (VD: validate token trong 1 luồng nghiệp vụ mới), họ sẽ nhận `ExpiredJwtException` không mong muốn thay vì `false` như tên method gợi ý — nếu chỗ gọi mới đó không có try/catch phù hợp, sẽ gây lỗi 500 không kiểm soát cho trường hợp "token hết hạn" (một trường hợp rất phổ biến, không phải edge case hiếm).

### Evidence
Unit test `JwtServiceImplTest.IsTokenValid.expiredTokenThrowsInsteadOfReturningFalse` xác nhận bằng cách generate token đã hết hạn và gọi `isTokenValid()` — ném `ExpiredJwtException` thay vì trả `false`. Test đã lưu vào `src/test/java/com/mchub/services/impl/JwtServiceImplTest.java`, chạy pass (test này PASS nghĩa là xác nhận đúng hành vi thật, không phải bug được fix).

### Status
**Open — mức độ ưu tiên thấp.** Đề xuất dev cân nhắc 1 trong 2 hướng (không phải QA quyết định):
1. Nếu `isTokenValid()` thực sự không dùng và không có kế hoạch dùng, xoá khỏi interface `JwtService` (dead code cleanup, đã note ở `Remaining_Modules_Audit_Report.md` mục 4 dạng chung — bổ sung method cụ thể này vào danh sách).
2. Nếu muốn giữ lại cho tương lai, bọc `try/catch (JwtException e) { return false; }` bên trong `isTokenValid()` để hành vi khớp đúng tên method và tránh bẫy cho dev sau này.
