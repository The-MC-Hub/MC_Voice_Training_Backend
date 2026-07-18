## DEFECT-006: `PaymentController.createCourseOrder()` không có nhánh "miễn phí/100% off" — course giá 0đ không mua được, trả HTTP 500

- **Module:** Payment (Course order)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-PAY-28 (system test UC-06, tiếp nối phiên trước).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `POST /api/v1/payment/course-order` trên `mchub_test` (không phải mock), PayOS credentials thật.

### Root Cause

`createPremiumOrder()` (mua gói subscription) có nhánh xử lý khi `effectiveAmount <= 0`: tự động kích hoạt gói ngay, không gọi PayOS (xem `PaymentController.java` dòng 110-141, tạo `PaymentTransaction` với `status=COMPLETED` trực tiếp).

`createCourseOrder()` (mua khóa học lẻ) **không có nhánh tương tự**. Khi `course.priceVnd = 0` (hoặc `discountPercent = 100` khiến `effectiveAmount` tính ra `0`), code vẫn đi thẳng vào:
```java
checkout = payOSService.createCoursePaymentLink(userId, course.getTitle(), orderCode, effectiveAmount);
```
PayOS API từ chối tạo payment link với amount = 0 (hoặc lỗi tương tự), `payOSService.createCoursePaymentLink()` ném exception, controller catch generic và trả:
```json
{"status":"error","message":"Payment service unavailable","errorCode":"ERR_9001"}
```
HTTP 500 — không phải lỗi nghiệp vụ có kiểm soát (400), mà là internal error thật.

### Impact nghiệp vụ

Nếu admin đặt `priceVnd=0` cho một khóa học (ví dụ khóa học miễn phí quảng bá, hoặc set `discountPercent=100` để tặng free tạm thời), **không người dùng nào có thể "mua" (nhận) khóa học đó qua luồng bình thường** — endpoint luôn trả 500. Đây là tính năng hợp lý về mặt nghiệp vụ (course miễn phí) nhưng code không hỗ trợ, khác hẳn với luồng subscription plan đã xử lý đúng trường hợp này.

### Evidence

```
$ curl -X POST ".../payment/course-order?courseId=<free-course-id>" -H "Authorization: Bearer <client-jwt>"
HTTP 500
{"status":"error","message":"Payment service unavailable","data":null,"success":false,"errorCode":"ERR_9001"}
```
Course fixture: `priceVnd=0`, `discountPercent=0` → `effectiveAmount = 0`, xác nhận qua `PATCH /admin/courses/{id}/pricing?priceVnd=0` trước đó (response xác nhận `priceVnd:0` đã lưu đúng).

### Status

**Fixed (2026-07-18).** Thêm nhánh `if (effectiveAmount <= 0)` trong `createCourseOrder()`, giống pattern `createPremiumOrder()` — tạo `PaymentTransaction` với `status=COMPLETED`, gọi `grantCoursePurchase(tx)` ngay (helper đã có sẵn, tái sử dụng), bỏ qua hoàn toàn PayOS.

**Verify live (port 5555, `mchub_test`):** tạo course mới, set `priceVnd=0` qua `PATCH /admin/courses/{id}/pricing` → `POST /payment/course-order?courseId=...` → HTTP 200 "Course granted automatically with 100% discount" (trước fix: 500 "Payment service unavailable"). Xác nhận `GET /courses/my/enrolled` sau đó trả đúng 1 course — enrollment thật sự được tạo, không chỉ response giả. `PaymentControllerTest` — 24/24 PASS.

Trường hợp `discountPercent=100` dùng chung code path với `priceVnd=0` (cả hai đều rút gọn về `effectiveAmount<=0` qua cùng công thức `Math.round(priceVnd*(100-pct)/100.0)`), nên đã được fix đồng thời — không cần nhánh riêng.
