# UC-08 — Public/Support — System Test Cases

**Ngày thực thi:** 2026-07-18
**Môi trường:** LIVE — `mchub_test` (MongoDB Atlas, database riêng), backend chạy port `5555`, Cloudinary thật cho media upload
**QA users:** `qa.quest.uc07@mchubtest.local` (CLIENT, tái sử dụng từ UC-07), `qa.client.uc04@mchubtest.local` (CLIENT), `qa.admin.uc04@mchubtest.local` (ADMIN)

---

## Danh sách Test Case

| ID | Mô tả | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TC-SUP-01 | GET landing data (Guest, public) | `GET /public/landing` | 200, `stats` tổng quan | Đúng, `totalProfessionals:1`, `totalMCs:1` | PASS |
| TC-SUP-02 | GET featured training stats (Guest, public) | `GET /public/featured-training` | 200, list MC nổi bật | Đúng | PASS |
| TC-SUP-03 | GET enum user-roles (Guest, public) | `GET /public/enums/user-roles` | 200, list `{value,label}` | Đúng, 3 role | PASS |
| TC-SUP-04 | GET enum report-reasons (Guest, public) | `GET /public/enums/report-reasons` | 200, list `{value,label}` | Đúng, 8 lý do | PASS |
| TC-SUP-05 | Gửi liên hệ hợp lệ | `POST /public/contact` `{name,email,message}` | 200, email gửi tới hộp thư support | Đúng | PASS |
| TC-SUP-06 | Gửi liên hệ thiếu field (negative) | thiếu `email` | 400, "Vui lòng điền đầy đủ thông tin" | Đúng | PASS |
| TC-SUP-07 | Gửi liên hệ email sai định dạng (negative/EP) | `email:"not-an-email"` | 400, "Email không hợp lệ" | Đúng | PASS |
| TC-SUP-08 | Gửi liên hệ message vượt 2000 ký tự (negative/BVA — biên trên) | `message` dài 2001 ký tự | 400, "không được vượt quá 2000 ký tự" | Đúng | PASS |
| TC-SUP-09 | Upload media KHÔNG có JWT (negative — auth) | `POST /media/upload` không JWT | 401/403 | 403 | PASS |
| TC-SUP-10 | Upload media hợp lệ (có JWT) | `POST /media/upload`, file nhỏ, JWT hợp lệ | 200, trả về `url` Cloudinary thật | Đúng, URL Cloudinary hợp lệ | PASS |
| TC-SUP-11 | Gửi báo cáo vi phạm hợp lệ | `POST /reports` `{reportedId, reason, description}` | 201, `status:PENDING` | Đúng | PASS |
| TC-SUP-12 | Gửi báo cáo KHÔNG có JWT (negative — auth) | không header `Authorization` | 401/403 | 403 — đúng, khác với `/auth/me` và `/community/leaderboard/me` (không có `@PreAuthorize` nhưng vẫn bị chặn ở tầng filter mặc định `.anyRequest().authenticated()` trước khi vào controller, không rơi vào lỗi 500 như DEFECT-015) | PASS |
| TC-SUP-13 | Gửi báo cáo thiếu `description` (negative/validation) | thiếu `description` | 400, "Description cannot be empty" | Đúng | PASS |
| TC-SUP-14 | Gửi báo cáo với `reason` không hợp lệ (negative/EP) | `reason:"INVALID_REASON"` (không thuộc enum) | 400, thông báo rõ ràng giá trị không hợp lệ | **HTTP 500 "System error" — Jackson ném `InvalidFormatException` khi deserialize enum sai, không được `GlobalExceptionHandler` xử lý riêng** | **FAIL — DEFECT-023 (ảnh hưởng toàn hệ thống, không riêng module này)** |
| TC-SUP-15 | GET báo cáo của tôi | `GET /reports/my` | 200, đúng report vừa gửi | Đúng, 1 report | PASS |
| TC-SUP-16 | Cách ly dữ liệu — user KHÁC xem "báo cáo của tôi" | `GET /reports/my`, JWT user khác chưa gửi report nào | 200, `data:[]`, không lẫn dữ liệu | Đúng | PASS |
| TC-SUP-17 | Non-admin (CLIENT) xem `/reports/admin` (negative — auth) | JWT CLIENT | 403 | Đúng | PASS |
| TC-SUP-18 | Admin xem toàn bộ báo cáo | `GET /reports/admin`, JWT ADMIN | 200, toàn bộ report | Đúng | PASS |
| TC-SUP-19 | Admin lọc báo cáo `status=pending` | `GET /reports/admin?status=pending` | 200, chỉ report PENDING | Đúng | PASS |
| TC-SUP-20 | Admin xử lý báo cáo hợp lệ | `PUT /reports/{id}/resolve` `{status:RESOLVED, adminNote}` | 200, `status` cập nhật, `resolvedAt`/`resolvedBy` ghi nhận | Đúng — lần đầu test dùng tiếng Việt qua Git Bash heredoc gặp lỗi encoding client-side (`Invalid UTF-8 middle byte`), retest bằng nội dung ASCII cho PASS đúng | PASS |
| TC-SUP-21 | Admin xử lý báo cáo thiếu `status` (negative/validation) | thiếu `status` trong body | 400, "Field 'status' cannot be empty" | Đúng | PASS |
| TC-SUP-22 | Admin xử lý báo cáo với `status` không hợp lệ (negative — xử lý ĐÚNG cách bằng try/catch thủ công) | `status:"NOT_A_STATUS"` | 400, "Invalid status" | Đúng — Controller này tự parse enum bằng `ReportStatus.valueOf()` trong try/catch thủ công, KHÔNG dựa vào Jackson tự động deserialize như trường hợp TC-SUP-14, nên tránh được lỗi tương tự DEFECT-023. Đây là bằng chứng đối chứng tốt cho thấy dev đã biết cách xử lý đúng ở 1 chỗ (điểm khác biệt: field ở đây nhận dạng `Map<String,String>` raw rồi tự parse, còn field ở TC-SUP-14 map trực tiếp qua DTO có kiểu enum) | PASS |
| TC-SUP-23 | Admin xử lý báo cáo KHÔNG tồn tại (negative) | id giả | 404, "Report not found" | **HTTP 500 — `ReportServiceImpl.resolveReport()` ném `RuntimeException` trần thay vì `AppException` chuẩn, không map đúng 404** | **FAIL — DEFECT-024** |
| TC-SUP-24 | Gửi báo cáo kèm `evidenceUrls` (tích hợp với Media upload) | `POST /reports` có `evidenceUrls:[url Cloudinary thật từ TC-SUP-10]` | 200, `evidenceUrls` lưu đúng | Đúng | PASS |
| TC-SUP-25 | Gửi báo cáo thiếu `reportedId` (negative/validation) | thiếu `reportedId` | 400, "Reported ID cannot be empty" | Đúng | PASS |

---

## Ghi chú kỹ thuật

- **DEFECT-023 là phát hiện quan trọng nhất module này** — không chỉ ảnh hưởng `ReportController`, mà là lỗ hổng cấu trúc trong `GlobalExceptionHandler` (thiếu handler cho `HttpMessageNotReadableException`) có khả năng lặp lại ở BẤT KỲ endpoint nào khác trong toàn hệ thống nhận enum qua `@RequestBody` — đề xuất dev ưu tiên fix ở tầng handler chung thay vì từng endpoint riêng lẻ.
- `ReportResponseDTO.reporterName`/`reportedName` luôn `null` (mapper có `@Mapping(ignore = true)` chủ đích) — không phải bug, nhưng là gap tính năng đáng chú ý: Admin xem danh sách báo cáo (UC-08 #8) chỉ thấy ID thô của người báo cáo/bị báo cáo, không thấy tên — ảnh hưởng khả năng sử dụng thực tế của trang quản trị báo cáo. Không file defect (hành vi chủ đích rõ ràng trong code, có thể do tính năng populate tên chưa được triển khai/đang chờ), chỉ ghi chú để dev cân nhắc bổ sung.
- Toàn bộ luồng landing/contact/upload/report hoạt động đúng ngoại trừ 2 vấn đề nêu trên.

---

## Tổng kết

**25/25 test case đã thực thi.**

| Kết quả | Số lượng |
|---|---|
| PASS | 23 |
| FAIL | 2 (TC-SUP-14, TC-SUP-23) |
| Not Executed | 0 |

**2 defect mới phát hiện:**
- DEFECT-023 (Major/P1) — `GlobalExceptionHandler` không xử lý `HttpMessageNotReadableException`, enum sai trong request body trả 500 thay vì 400. Ảnh hưởng toàn hệ thống.
- DEFECT-024 (Minor/P2) — `ReportServiceImpl.resolveReport()` dùng `RuntimeException` trần thay vì `AppException`, report không tồn tại trả 500 thay vì 404.

**Điểm sáng:** Toàn bộ luồng public landing/contact/enum, upload media thật qua Cloudinary, submit/xem/xử lý báo cáo, cách ly dữ liệu giữa user, authorization admin-only đều hoạt động đúng. Đây là UC cuối cùng trong toàn bộ kế hoạch test — tổng kết chung 9/9 UC đã hoàn thành.
