## DEFECT-001: Endpoint public UC-06 (plans/flash-deals/apply-discount) bị chặn 403 do thiếu whitelist trong SecurityConfig

- **Module:** Payment & Subscription (UC-06)
- **Ngày phát hiện:** 2026-07-17
- **Test case liên quan:** TC-PAY-01, TC-PAY-05, TC-PAY-08 (`testing/03-system/UC-06-Payment-Subscription_TestCases.md`)
- **Severity:** ~~Blocker~~ → **Critical** (xem "Cập nhật quan trọng" bên dưới — chỉ chặn Guest chưa đăng nhập, không chặn user đã login)
- **Priority:** P0
- **Môi trường:** Backend chạy local (`java -jar target/backend-java-1.0.0.jar`), port 5555, kết nối MongoDB Atlas database `mchub_test` (tách biệt production), không qua proxy/CDN nào có thể gây nhiễu.

### Steps to Reproduce
1. Khởi động backend (không cần seed data, không cần JWT).
2. Gọi `GET /api/v1/payment/plans` (không có header `Authorization`).
3. Gọi `GET /api/v1/payment/flash-deals` (không có header `Authorization`).
4. Gọi `POST /api/v1/payment/apply-discount?code=TEST&plan=BASIC` (không có header `Authorization`).

### Expected Result
Theo `docs/use-cases/UC-06-payment-subscription.md` (tính năng #1, #2, #3: "User xem...") và theo code `PaymentController.java`:
- `getActivePlans()`, `getFlashDeals()`, `applyDiscount()` đều **không có `@PreAuthorize`** trên method — chủ đích thiết kế là endpoint public (comment code dòng 42 ghi rõ `// GET /api/v1/payment/plans (public — frontend fetches pricing)`, dòng 50 ghi `// GET /api/v1/payment/flash-deals (public — no auth required)`).
- Cả 3 request trên phải trả **HTTP 200** kèm dữ liệu, không cần JWT.

### Actual Result
Cả 3 request đều trả **HTTP 403 Forbidden** (body rỗng, `Content-Length: 0`), dù không gửi JWT.

### Root Cause (quan sát được, không tự sửa)
Đối chiếu `src/main/java/com/mchub/config/SecurityConfig.java` dòng 55-70 (`authorizeHttpRequests`): danh sách whitelist chỉ khai báo:
```
.requestMatchers("/api/v1/payment/webhook").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/payment/status/**").authenticated()
```
Không có bất kỳ rule nào cho `/api/v1/payment/plans`, `/api/v1/payment/flash-deals`, `/api/v1/payment/apply-discount`. Toàn bộ 3 path này rơi vào rule cuối `.anyRequest().authenticated()` → bị chặn nếu không có JWT hợp lệ.

**Mismatch rõ ràng giữa 2 tầng:** Controller code + comment + use-case doc đều khẳng định 3 endpoint này PUBLIC, nhưng cấu hình Security lại yêu cầu authenticated. Đây là lỗi thiếu đồng bộ giữa Controller và SecurityConfig — không phải vấn đề ở tầng Controller.

### Impact nghiệp vụ
- User (kể cả Guest) **không thể xem được giá gói cước** trước khi đăng nhập → landing page/pricing page (nếu gọi API này để hiển thị) sẽ lỗi hoặc rỗng cho khách chưa đăng nhập.
- Flash-deal (khuyến mãi giới hạn thời gian) — vốn dùng để thu hút user MỚI — không hiển thị được cho khách chưa có tài khoản, làm mất tác dụng marketing.
- `apply-discount` là bước preview giá trước khi bấm thanh toán — nếu FE gọi bước này trước khi user đăng nhập (luồng phổ biến: xem giá → nhập mã → mới yêu cầu đăng nhập để thanh toán), toàn bộ luồng UX bị gãy.

### Evidence
```
$ curl -i http://localhost:5555/api/v1/payment/plans
HTTP/1.1 403
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Set-Cookie: JSESSIONID=...; Path=/; HttpOnly
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Length: 0
```
(Tương tự cho `flash-deals` và `apply-discount`, đã verify riêng từng endpoint.)

### Cập nhật quan trọng sau khi test thêm (2026-07-17, cùng ngày)
Đã test lại 2 endpoint (`/plans`, `/apply-discount`) **CÓ kèm JWT hợp lệ** (user CLIENT đã đăng nhập) → **HTTP 200/404 theo logic nghiệp vụ bình thường, KHÔNG còn bị 403.** Nghĩa là:
- Lỗi 403 **CHỈ xảy ra với Guest chưa đăng nhập** (không có JWT).
- User đã đăng nhập gọi các endpoint này hoàn toàn bình thường.

**Điều chỉnh Severity:** hạ từ Blocker → **Critical** (không phải Blocker vì không chặn hoàn toàn mọi actor, chỉ chặn đúng nhóm Guest — nhưng vẫn nghiêm trọng vì đây chính xác là nhóm actor mà 3 endpoint này được thiết kế để phục vụ ưu tiên: xem giá TRƯỚC KHI đăng nhập).

### Status
**Fixed (2026-07-18).** Thêm 3 rule vào `SecurityConfig.java`:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/payment/plans").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/payment/flash-deals").permitAll()
.requestMatchers(HttpMethod.POST, "/api/v1/payment/apply-discount").permitAll()
```
**Verify live (port 5555, `mchub_test`, không JWT):** `plans` → 200, `flash-deals` → 200, `apply-discount` (mã giả) → 404 "Mã giảm giá không tồn tại" (lỗi nghiệp vụ hợp lệ, không còn bị chặn 403 ở tầng bảo mật). `PaymentControllerTest` 22/22 PASS, không hồi quy.

### Ghi chú cho Retest
Sau khi dev fix, retest lại đúng 3 case TC-PAY-01/05/08 **ở cả 2 điều kiện: có JWT và không có JWT** + quét toàn bộ endpoint còn lại trong `PaymentController` không có `@PreAuthorize` để xác nhận không còn endpoint public nào khác bị lọt vào tình trạng tương tự.
