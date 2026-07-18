## DEFECT-023: `GlobalExceptionHandler` không xử lý `HttpMessageNotReadableException` — request body chứa giá trị enum không hợp lệ trả HTTP 500 thay vì 400 validation rõ ràng

- **Module:** Toàn hệ thống (mọi endpoint nhận `@RequestBody` chứa field kiểu enum) — phát hiện cụ thể qua UC-08 Report
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-SUP-14 (system test UC-08).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — `POST /api/v1/reports` với `reason:"INVALID_REASON"` (không thuộc enum `ReportReason`) trên `mchub_test`.

### Root Cause

`CreateReportRequest.reason` kiểu `ReportReason` (enum). Khi client gửi 1 giá trị string không khớp bất kỳ hằng số nào trong enum, Jackson ném `InvalidFormatException` (subclass của `HttpMessageNotReadableException`) NGAY TẠI BƯỚC DESERIALIZE request body — xảy ra TRƯỚC KHI Spring MVC kịp chạy `@Valid`/Bean Validation trên object đã map xong. `GlobalExceptionHandler.java` không có `@ExceptionHandler(HttpMessageNotReadableException.class)` — exception này rơi vào nhánh catch-all generic, trả HTTP 500 "System error, please try again later" thay vì HTTP 400 với message rõ ràng liệt kê các giá trị enum hợp lệ.

Đây là lỗi cấu trúc chung của `GlobalExceptionHandler`, KHÔNG giới hạn riêng `ReportController`/`ReportReason` — bất kỳ DTO nào trong codebase có field kiểu enum nhận qua `@RequestBody` đều có nguy cơ gặp lỗi tương tự khi client gửi sai giá trị (kể cả gõ nhầm chữ hoa/thường, hoặc gửi giá trị enum đã bị xoá/đổi tên qua các version API cũ).

### Impact nghiệp vụ

- Trải nghiệm lỗi rất tệ cho mọi client tích hợp API — gõ sai 1 giá trị enum (lỗi input rất phổ biến, đặc biệt phía mobile/web khi đồng bộ chưa kịp enum mới) nhận về "System error" chung chung thay vì biết chính xác cần sửa gì.
- Log server bị nhiễu bởi rất nhiều "lỗi hệ thống" giả (thực chất chỉ là lỗi input người dùng/client) — khó phân biệt sự cố thật với input sai khi rà log production.
- Phạm vi ảnh hưởng rộng — QA chỉ verify được qua `ReportReason` (do đang test UC-08), nhưng cần dev xác nhận toàn bộ enum field khác trong các DTO khác (`CourseType`, `VoiceLessonCategory`, `DiscountCode.DiscountType`, `SubscriptionPlan`, v.v.) đều có cùng lỗ hổng tiềm ẩn.

### Evidence

```
$ curl -X POST "http://localhost:5555/api/v1/reports" -H "Authorization: Bearer <jwt>" \
    -d '{"reportedId":"x","reason":"INVALID_REASON","description":"test"}'
HTTP 500 {"status":"error","message":"System error, please try again later","errorCode":"ERR_9001"}

# Server log xác nhận nguyên nhân thật:
org.springframework.http.converter.HttpMessageNotReadableException: JSON parse error:
Cannot deserialize value of type `com.mchub.enums.ReportReason` from String "INVALID_REASON":
not one of the values accepted for Enum class: [OTHER, PAYMENT_BYPASS, FRAUD, MISLEADING_PROFILE,
NO_SHOW, INAPPROPRIATE_CONTENT, UNPROFESSIONAL, SPAM]
```

Source: `GlobalExceptionHandler.java` — không có `@ExceptionHandler` riêng cho `HttpMessageNotReadableException`.

### Status

**Fixed (2026-07-18).** Thêm `@ExceptionHandler(HttpMessageNotReadableException.class)` trong `GlobalExceptionHandler.java` (cùng commit với fix DEFECT-015, do cả 2 handler được thêm trong cùng 1 lần sửa file), trả HTTP 400 với message chung "Dữ liệu gửi lên không đúng định dạng" thay vì rơi xuống 500 generic.

**Verify trực tiếp (live, port 5555, `mchub_test`):**
```
$ curl -X POST ".../reports" -d '{"reportedId":"x","reason":"INVALID_REASON","description":"test"}'
HTTP 400 {"message":"Dữ liệu gửi lên không đúng định dạng","errorCode":"ERR_9002"}
```
Đúng như kỳ vọng (trước fix: HTTP 500).

Vì đây là exception handler toàn cục, fix áp dụng cho MỌI endpoint nhận `@RequestBody` chứa enum sai giá trị trong toàn hệ thống, không chỉ riêng `ReportController` — đúng phạm vi ảnh hưởng đã ghi nhận. Chưa làm chi tiết hoá message theo từng loại enum (liệt kê giá trị hợp lệ) — để đơn giản, message chung đã đủ rõ ràng hơn nhiều so với "System error" trước đó; có thể cải thiện thêm sau nếu cần.
