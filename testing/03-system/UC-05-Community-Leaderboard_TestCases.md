# UC-05 — Community & Leaderboard — System Test Cases

**Ngày thực thi:** 2026-07-18
**Môi trường:** LIVE — `mchub_test` (MongoDB Atlas, database riêng), backend chạy port `5555`
**QA users:** `qa.admin.uc04@mchubtest.local` (ADMIN), `qa.client.uc04@mchubtest.local` (CLIENT) — tái sử dụng từ session trước

---

## Danh sách Test Case

| ID | Mô tả | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TC-COMM-01 | GET community stats (Guest, public) | `GET /community/stats` | 200, `totalUsers`, `totalPracticeHours`, `mostPopularScriptTitle`, `activeCompetitionsCount` | Đúng | PASS |
| TC-COMM-02 | GET leaderboard mặc định (streak, all_time) | `GET /community/leaderboard` | 200, danh sách xếp hạng phân trang | Đúng | PASS |
| TC-COMM-03 | GET leaderboard type không hợp lệ (negative/EP) | `GET /community/leaderboard?type=invalid_type` | Lỗi rõ ràng hoặc fallback an toàn | Fallback về kết quả mặc định (không crash) — chấp nhận được, ghi chú kỹ thuật không phải Fail | PASS |
| TC-COMM-04 | GET leaderboard size vượt giới hạn (BVA — biên trên) | `GET /community/leaderboard?size=100` | Size bị cap ở 50 | Đúng, `size:50` | PASS |
| TC-COMM-05 | GET leaderboard/me với JWT hợp lệ | `GET /community/leaderboard/me`, JWT | 200, entry xếp hạng của chính user | Đúng | PASS |
| TC-COMM-06 | GET leaderboard/me KHÔNG có JWT (negative — auth) | không header `Authorization` | 401 | **HTTP 500 — cùng nguyên nhân DEFECT-015 (`SecurityUtils.getCurrentUserId()` ném `IllegalStateException` không được xử lý đúng)** | **FAIL — cùng DEFECT-015, không file riêng, đã cập nhật bổ sung evidence vào DEFECT-015** |
| TC-COMM-07 | GET active-arenas (Guest, không JWT) | `GET /community/active-arenas` | 200, danh sách giải đấu đang hoạt động (rỗng lúc đầu) | Đúng, `data:[]` — Controller xử lý đúng bằng try/catch quanh `getCurrentUserId()`, không bị lỗi như TC-COMM-06 | PASS |
| TC-COMM-08 | GET active-arenas (đã đăng nhập) | `GET /community/active-arenas`, JWT | 200 | Đúng | PASS |
| TC-COMM-09 | GET social posts active (Guest, public) | `GET /social-posts` | 200, danh sách bài đăng đang active | Đúng, `data:[]` (chưa có bài đăng) | PASS |
| TC-COMM-10 | POST click bài đăng (Guest, không JWT — theo đặc tả phải công khai) | `POST /social-posts/{id}/click` không JWT | 200, ghi nhận click (hoặc 404 nếu id không tồn tại) | **HTTP 403 — thiếu whitelist SecurityConfig cho route POST click, chặn nhầm cả Guest hợp lệ** | **FAIL — DEFECT-022** |
| TC-COMM-11 | Admin tạo giải đấu (competition/arena) | `POST /admin/competitions`, JWT ADMIN, `startDate`/`endDate` định dạng `Instant` (`...Z`) | 200, giải đấu tạo thành công | Đúng — lần đầu test dùng sai định dạng ngày (`LocalDateTime` không có hậu tố `Z`) gây `HttpMessageNotReadableException`, đây là lỗi test-script (input sai kiểu dữ liệu API yêu cầu), không phải defect hệ thống. Retest với định dạng `Instant` đúng cho kết quả PASS | PASS |
| TC-COMM-12 | Non-admin (CLIENT) tạo giải đấu (negative — auth) | `POST /admin/competitions`, JWT CLIENT | 403 | Đúng | PASS |
| TC-COMM-13 | Xác nhận giải đấu vừa tạo xuất hiện trong active-arenas | `GET /community/active-arenas` | 200, chứa giải đấu vừa tạo | Đúng | PASS |
| TC-COMM-14 | Admin sửa giải đấu | `PUT /admin/competitions/{id}` | 200, nội dung cập nhật | Đúng | PASS |
| TC-COMM-15 | Admin xem toàn bộ giải đấu | `GET /admin/competitions` | 200, list đầy đủ | Đúng | PASS |
| TC-COMM-16 | Admin xoá giải đấu | `DELETE /admin/competitions/{id}` | 200 | Đúng | PASS |
| TC-COMM-17 | Xác nhận active-arenas rỗng lại sau khi xoá | `GET /community/active-arenas` | 200, `data:[]` | Đúng | PASS |
| TC-COMM-18 | Admin tạo bài đăng mạng xã hội | `POST /admin/social-posts` (field đúng: `image`, `description`, `fbLink`, `sortOrder`, `active`) | 200, bài đăng tạo `active:true` | Đúng — lần đầu test dùng sai tên field (`imageUrl`/`linkUrl`/`title` không tồn tại trong DTO thật `image`/`fbLink`/`description`), lỗi test-script không phải defect. Retest đúng field cho PASS, xác nhận `active:true` lưu đúng (field DTO đã đặt tên `active` thay vì `isActive`, không dính lỗi Lombok naming như DEFECT-011) | PASS |
| TC-COMM-19 | Xác nhận bài đăng hiện trong public list | `GET /social-posts` | 200, 1 bài | Đúng | PASS |
| TC-COMM-20 | Click tracking tăng `clickCount` (dùng JWT admin làm workaround cho DEFECT-022 vì Guest bị chặn nhầm) | `POST /social-posts/{id}/click`, JWT ADMIN | `clickCount` +1 | Đúng, `clickCount:1` | PASS |
| TC-COMM-21 | Admin toggle active off | `PATCH /admin/social-posts/{id}/toggle` | 200, `active:false` | Đúng | PASS |
| TC-COMM-22 | Xác nhận bài đăng ẩn khỏi public list sau toggle off | `GET /social-posts` | 200, `data:[]` | Đúng | PASS |
| TC-COMM-23 | Admin sửa bài đăng | `PUT /admin/social-posts/{id}` | 200, nội dung cập nhật | Đúng | PASS |
| TC-COMM-24 | Non-admin (CLIENT) xem admin social-posts list (negative) | `GET /admin/social-posts`, JWT CLIENT | 403 | Đúng | PASS |
| TC-COMM-25 | Admin xoá bài đăng | `DELETE /admin/social-posts/{id}` | 200 | Đúng | PASS |
| TC-COMM-26 | Admin sửa giải đấu không tồn tại (negative) | `PUT /admin/competitions/{fake-id}` | 404 | Đúng, `ERR_9003` "Competition not found" | PASS |

---

## Ghi chú kỹ thuật

- Lần chạy đầu của TC-COMM-11 và TC-COMM-18 gặp lỗi do QA truyền sai định dạng dữ liệu (ngày tháng thiếu `Z`, tên field JSON sai) — đã xác nhận qua đọc code (`Competition.startDate/endDate` kiểu `Instant`, `SaveSocialPostRequest` dùng field `image`/`fbLink`/`description`/`active`) đây là lỗi thao tác test, không phải defect hệ thống. Retest đúng input cho kết quả PASS cả 2.
- `SaveSocialPostRequest.active` đặt tên đúng convention Lombok (không có tiền tố `is`) — không gặp lỗi silent-false như DEFECT-011 (`SaveCourseRequest.isActive`). Đây là điểm đối chứng tốt cho thấy 1 module trong cùng codebase làm đúng, khẳng định DEFECT-011 là lỗi cụ thể của riêng `SaveCourseRequest`, không phải quy ước chung toàn dự án.
- TC-COMM-06 chỉ ghi nhận là bằng chứng bổ sung cho DEFECT-015 đã file trước đó (UC-01), không tính là defect mới độc lập.

---

## Tổng kết

**26/26 test case đã thực thi.**

| Kết quả | Số lượng |
|---|---|
| PASS | 24 |
| FAIL | 2 (TC-COMM-06 — cùng DEFECT-015 đã file; TC-COMM-10 — DEFECT-022 mới) |
| Not Executed | 0 |

**1 defect mới phát hiện:** DEFECT-022 (Major/P1 — thiếu SecurityConfig whitelist cho `POST /social-posts/{id}/click`, chặn nhầm Guest, cùng nhóm nguyên nhân với DEFECT-001/010/013).

**Điểm sáng:** Toàn bộ luồng leaderboard, community stats, admin CRUD giải đấu + bài đăng mạng xã hội, toggle visibility, click tracking (khi có auth) đều hoạt động đúng theo đặc tả. Không phát hiện lỗi business logic nghiêm trọng trong module này — 2 vấn đề đều thuộc nhóm hạ tầng đã biết (missing whitelist, generic exception handling).
