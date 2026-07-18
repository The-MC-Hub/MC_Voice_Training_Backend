## DEFECT-024: `ReportServiceImpl.resolveReport()` ném `RuntimeException` trần thay vì `AppException` — báo cáo không tồn tại trả HTTP 500 thay vì 404

- **Module:** Public/Support (Report resolution)
- **Ngày phát hiện:** 2026-07-18 — phát hiện khi thực thi TC-SUP-23 (system test UC-08).
- **Severity:** Minor
- **Priority:** P2
- **Môi trường:** LIVE — `PUT /api/v1/reports/{id}/resolve` với id không tồn tại, JWT ADMIN hợp lệ, trên `mchub_test`.

### Root Cause

`ReportServiceImpl.resolveReport()`:
```java
Report report = reportRepository.findById(Objects.requireNonNull(reportId))
    .orElseThrow(() -> new RuntimeException("Report does not exist"));
```

Toàn bộ các service khác trong codebase đã audit qua các UC trước (`CourseServiceImpl`, `CertificateController`, `AdminCompetitionController` gián tiếp qua `CompetitionService`, v.v.) đều nhất quán dùng pattern:
```java
.orElseThrow(() -> new AppException(ErrorCode.XXX_NOT_FOUND, "message: " + id));
```
`AppException` được `GlobalExceptionHandler` map đúng sang HTTP status code tương ứng (404 cho *_NOT_FOUND). Riêng `ReportServiceImpl` dùng `RuntimeException` trần — không được `GlobalExceptionHandler` nhận diện là lỗi "không tìm thấy", rơi vào nhánh catch-all generic, trả HTTP 500 "System error" thay vì 404 "Report not found".

### Impact nghiệp vụ

Mức độ nhẹ — chỉ ảnh hưởng đúng ngữ nghĩa HTTP status code khi Admin thao tác nhầm với report ID không tồn tại (trường hợp hiếm trong thực tế, vì UI thường chỉ cho phép chọn từ danh sách report có thật). Không mất dữ liệu, không ảnh hưởng luồng chính. Đáng chú ý vì đây là điểm không nhất quán coding convention trong nội bộ dự án — 1 file duy nhất lệch khỏi pattern chung.

### Evidence

```
$ curl -X PUT "http://localhost:5555/api/v1/reports/000000000000000000000000/resolve" -H "Authorization: Bearer <ADMIN-JWT>" -d '{"status":"RESOLVED"}'
HTTP 500 {"status":"error","message":"System error, please try again later","errorCode":"ERR_9001"}

# Server log xác nhận nguyên nhân thật:
java.lang.RuntimeException: Report does not exist
```

Source: `ReportServiceImpl.java` dòng 35-37.

### Status

**Open.** Đề xuất dev (không phải QA quyết định): đổi thành `throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found: " + reportId)` — khớp đúng convention nhất quán với toàn bộ codebase còn lại.
