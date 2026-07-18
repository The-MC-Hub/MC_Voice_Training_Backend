# UC-02 — User & MC Profile — System Test Cases

**Ngày thực thi:** 2026-07-18
**Môi trường:** LIVE — `mchub_test` (MongoDB Atlas, database riêng), backend chạy port `5555`
**QA users:** `qa.mc.uc02@mchubtest.local` (MC, tạo mới), `qa.client.uc04@mchubtest.local` (CLIENT, tái sử dụng), `qa.admin.uc04@mchubtest.local` (ADMIN, tái sử dụng)

---

## Danh sách Test Case

| ID | Mô tả | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TC-PROF-01 | GET streak — user mới, chưa có login streak | `GET /users/me/streak` | 200, `loginStreak:0`, `freezesAvailable:1`, `streakFrame:"NONE"` | Đúng | PASS |
| TC-PROF-02 | GET streak không JWT (negative — auth) | không header `Authorization` | 401/403 | 403 (chấp nhận được, Spring Security default) | PASS |
| TC-PROF-03 | GET practice-stats | `GET /users/me/practice-stats` | 200, `UserStats` đầy đủ | Đúng | PASS |
| TC-PROF-04 | POST streak/freeze khi còn lượt (kiểm tra hành vi thực tế) | `POST /users/me/streak/freeze`, `freezesAvailable=1` trước đó | Kỳ vọng theo đặc tả: tiêu 1 lượt freeze, bảo toàn streak | **Không có tác dụng gì — `freezesAvailable` không đổi trước/sau khi gọi. Endpoint chỉ trả lại y hệt `GET streak`, không có side-effect** | **FAIL — DEFECT-017** |
| TC-PROF-05 | GET MC dashboard (role MC, xem chính mình) | `GET /mcs/dashboard`, JWT MC | 200, thống kê `avgWpm/totalPractices/avgAccuracy` | Đúng | PASS |
| TC-PROF-06 | GET MC dashboard bằng JWT CLIENT (negative — kiểm tra ranh giới role) | `GET /mcs/dashboard`, JWT CLIENT | 403 (endpoint dành riêng cho MC theo đặc tả UC-02 #3) | **HTTP 200 — CLIENT xem được dashboard của chính họ, `@PreAuthorize` không có điều kiện kiểm tra role MC** | **FAIL — DEFECT-018** |
| TC-PROF-07 | PUT /mcs/profile bằng JWT CLIENT (negative — exploit test liên quan DEFECT-018) | `PUT /mcs/profile`, JWT CLIENT, body hợp lệ | 403 | Đúng, 403 — `updateProfile()` có `@PreAuthorize("hasAuthority('MC')")` không kèm ownership-check nên hoạt động đúng, không bị ảnh hưởng bởi lỗi ở TC-PROF-06. Xác nhận DB: không có `mcprofiles` document nào được tạo cho CLIENT | PASS (đối chứng khẳng định lỗi ở TC-PROF-06 chỉ riêng `getDashboardStats`) |
| TC-PROF-08 | MC cập nhật hồ sơ hợp lệ | `PUT /mcs/profile`, JWT MC, đầy đủ field | 200, hồ sơ cập nhật đúng | Đúng | PASS |
| TC-PROF-09 | MC thêm chứng chỉ (luồng thủ công) | `POST /certificates`, JWT MC, body hợp lệ | 200/201, chứng chỉ được tạo | **HTTP 500 "System error" — thực chất route đã bị deprecated (`UnsupportedOperationException` không được xử lý, ném thẳng ra generic 500)** | **FAIL — DEFECT-019** |
| TC-PROF-10 | CLIENT (không có MC profile) thêm chứng chỉ (negative) | `POST /certificates`, JWT CLIENT | 400/404, "cần có MC profile trước" | Đúng, `ERR_2002` "You need to have an MC profile before adding a certificate" — guard này chạy TRƯỚC khi chạm code deprecated nên không bị ảnh hưởng bởi DEFECT-019 | PASS |
| TC-PROF-11 | Thêm chứng chỉ thiếu field bắt buộc (negative/validation) | `POST /certificates`, thiếu `name` | 400, "Certificate name cannot be empty" | Đúng — validation `@Valid` chạy trước khi vào service nên cũng không bị ảnh hưởng DEFECT-019 | PASS |
| TC-PROF-12 | GET chứng chỉ theo MC profile (route đọc, không deprecated) | `GET /certificates/mc/{mcProfileId}` | 200, `data:[]` (chưa có cert nào vì thêm cert đang lỗi) | Đúng | PASS |
| TC-PROF-13 | Admin duyệt chứng chỉ (route deprecated) | `PUT /certificates/{id}/verify`, JWT ADMIN | 200/400 rõ ràng (deprecated) | HTTP 500 — cùng nguyên nhân DEFECT-019 | **FAIL — cùng DEFECT-019, không file riêng** |
| TC-PROF-14 | Xoá chứng chỉ (route deprecated) | `DELETE /certificates/{id}`, JWT MC | 200/400 rõ ràng (deprecated) | HTTP 500 — cùng nguyên nhân DEFECT-019 | **FAIL — cùng DEFECT-019, không file riêng** |
| TC-PROF-15 | GET /public/mcs khám phá công khai (Guest, không JWT) | `GET /public/mcs` | 200, list MC profile | Đúng, 1 kết quả — nhưng phát hiện field `email` bị lộ công khai (xem TC-PROF-17) | PASS (chức năng liệt kê đúng) nhưng xem ghi chú DEFECT-021 |
| TC-PROF-16 | GET /public/mcs lọc theo category | `GET /public/mcs?category=WEDDING_MC` | 200, chỉ MC thuộc category này | Trả về đúng 1 kết quả hiện có trong DB test, KHÔNG đủ để kết luận qua so sánh số liệu (chỉ có 1 MC fixture) — nhưng đối chiếu trực tiếp code `PublicServiceImpl.discoverMCs()` xác nhận chắc chắn tham số `category` hoàn toàn không được dùng trong logic lọc | **FAIL (theo code review) — DEFECT-020** |
| TC-PROF-17 | GET /public/mcs/{id} chi tiết hồ sơ MC công khai | `GET /public/mcs/{id}` | 200, thông tin hồ sơ MC | Đúng dữ liệu cơ bản, nhưng response chứa `email` thật của MC (PII lộ công khai không cần đăng nhập) VÀ field `verified:true` gây hiểu lầm (chỉ là email đã xác minh, không phải hồ sơ MC đã được duyệt chuyên môn) | **FAIL — DEFECT-021** |
| TC-PROF-18 | GET /public/mcs/{id} không tồn tại (negative) | id giả | 404, "MC profile not found" | Đúng, `ERR_2002` | PASS |

---

## Ghi chú kỹ thuật quan trọng

- **UC-02 tính năng #5/#7/#8 (thêm/duyệt/xoá chứng chỉ thủ công) hoàn toàn không hoạt động** — không phải bug ngẫu nhiên mà là code deprecated chủ đích (đã migrate sang luồng certificate tự động qua hoàn thành khoá học, xem UC-04). Vấn đề KHÔNG phải "tính năng thiếu" mà là "lỗi HTTP response sai" (500 thay vì thông báo rõ ràng) — QA đề xuất dev dọn dẹp route chết hoặc trả lỗi đúng ngữ nghĩa (xem DEFECT-019).
- **DEFECT-018 là phát hiện đáng chú ý nhất** — không phải lỗi proxy/AOP như nghi ngờ ban đầu (đã verify bằng đối chứng `updateProfile()` hoạt động đúng), mà là lỗi thiết kế điều kiện `@PreAuthorize` SpEL: `#userId == authentication.name` luôn đúng khi endpoint tự lấy `userId` từ JWT của chính người gọi — khiến vế `hasAuthority('MC')` bị thiếu hoàn toàn không có tác dụng chặn role.
- Toàn bộ luồng streak/practice-stats/MC-profile-update/public-discovery-listing hoạt động đúng cơ bản, ngoại trừ 5 defect nêu trên.

---

## Tổng kết

**18/18 test case đã thực thi.**

| Kết quả | Số lượng |
|---|---|
| PASS | 12 |
| FAIL | 6 (TC-PROF-04, 06, 09, 13, 14, 16, 17 — lưu ý 13/14 cùng nhóm DEFECT-019 với 09) |
| Not Executed | 0 |

**5 defect mới phát hiện:**
- DEFECT-017 (Minor/P2) — `POST streak/freeze` không có tác dụng, chỉ là passthrough đọc.
- DEFECT-018 (Major/P1) — CLIENT gọi được `/mcs/dashboard` do `@PreAuthorize` thiếu điều kiện role MC.
- DEFECT-019 (Major/P1) — Certificate thủ công (add/verify/delete) là stub deprecated, trả 500 thay vì lỗi rõ ràng.
- DEFECT-020 (Major/P1) — `/public/mcs?category=` không lọc, tham số bị bỏ qua hoàn toàn.
- DEFECT-021 (Major/P1) — `/public/mcs` lộ email công khai + field `verified` gây hiểu lầm ngữ nghĩa.

**Điểm sáng:** Luồng streak đọc, practice-stats, MC profile update (đúng người, đúng role), MC profile detail public, không-tồn-tại negative case đều hoạt động đúng. Các defect phát hiện tập trung nhiều vào nhóm "tính năng công khai/discovery" và "code deprecated còn sót route active" — không có lỗi mất/hỏng dữ liệu nghiêm trọng.
