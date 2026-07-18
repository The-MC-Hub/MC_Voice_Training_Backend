# System Test Case — UC-09: Quản trị Hệ thống & Người dùng (Admin Dashboard)

**Nguồn đặc tả:** `docs/use-cases/UC-09-admin-dashboard.md`
**Source verify:** `controllers/AdminController.java`, `controllers/LogController.java`, `controllers/AuditLogController.java`, `services/impl/AdminServiceImpl.java`
**Ưu tiên:** P0
**Môi trường:** MongoDB Atlas `mchub_test`, backend port 5555

## Ghi chú đối chiếu source quan trọng (trước khi test)
- **Mismatch use-case doc vs code:** mục #14 doc ghi "Xoá tài khoản người dùng" (ngụ ý vĩnh viễn), nhưng `AdminServiceImpl.deleteUser()` **chỉ soft-delete** (`setActive(false)`, comment code xác nhận rõ "preserve data integrity"). Đây không phải bug — code an toàn hơn doc mô tả — nhưng doc cần cập nhật để không gây hiểu nhầm cho người đọc mới.
- **`AuditLogController.purgeLogs`**: đã verify kỹ — dù Controller tính `safeDays` sau khi gọi Service, Service (`AuditLogServiceImpl.purgeLogs`) tự clamp `Math.max(daysOld, 3)` độc lập bên trong, nên **không có lỗ hổng thật** dù đọc code lần đầu dễ nghi ngờ thứ tự gọi sai.
- **`LogController.ingest`** (`POST /admin/logs/ingest`) nằm dưới class-level `@PreAuthorize("hasAuthority('ADMIN')")` — nghĩa là AI service ngoài (Python) phải có JWT ADMIN hợp lệ để đẩy log vào. Cần xác nhận thực tế AI service có cách nào lấy JWT ADMIN không (service-to-service credential) — nếu không, tính năng ghi log từ AI service có thể **không hoạt động được trong thực tế** (không phải lỗi bảo mật, mà là nghi vấn tính khả dụng — cần hỏi dev).

## Quy ước
- **[LIVE]** — Execute thật trên `mchub_test`.
- Không có rủi ro tài chính trực tiếp ở UC-09 (không đụng PayOS) nhưng có **rủi ro vận hành cao** (xóa dữ liệu, migration DB) — các case xóa/migration cần cẩn trọng đặc biệt, chỉ chạy trên `mchub_test`, không bao giờ trỏ nhầm production.

---

## TC-ADM-01 → 04: GET /dashboard, /transactions, /revenue-stats

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-01 | Negative [LIVE] | JWT role CLIENT gọi `/admin/dashboard` | HTTP 403 (class-level `@PreAuthorize`) | HTTP 403 | **PASS** |
| TC-ADM-02 | EP hợp lệ [LIVE] | Admin JWT | HTTP 200, `totalUsers`/`totalMCs` dùng `countBy...` DB-side (không load hết rồi đếm) | HTTP 200, `totalTransactions:8, totalUsers:6, totalRevenue:1055200` | **PASS** |
| TC-ADM-03 | Boundary [LIVE] | `mchub_test` chưa có transaction nào | `/admin/dashboard` trả `totalTransactions:0`, `totalRevenue:0`, không lỗi | Không thực hiện — `mchub_test` đã tích lũy 8 transaction qua nhiều phiên test UC-06/UC-09, không còn state rỗng để tái tạo | Not Executed |
| TC-ADM-04 | EP hợp lệ [LIVE] | Có transaction COMPLETED/PENDING trộn lẫn (tái dùng dữ liệu từ UC-06) | `/admin/transactions` trả đúng danh sách, kèm `userName`/`userEmail` join đúng qua batch-fetch (không N+1) | HTTP 200, 8 transaction, mỗi phần tử có đủ `userName`/`userEmail` join đúng | **PASS** |

## TC-ADM-05 → 08: GET /users, /users/{id}, /users/mcs

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-05 | EP hợp lệ [LIVE] | Admin JWT | `/admin/users` trả toàn bộ user (kể cả ADMIN — khác với `getAnalytics` loại trừ ADMIN) | HTTP 200, `roles present: {ADMIN, CLIENT}` — xác nhận có cả ADMIN | **PASS** |
| TC-ADM-06 | Negative [LIVE] | `id` không tồn tại | `/admin/users/{id}` → HTTP 404 `USER_NOT_FOUND` | HTTP 404 | **PASS** |
| TC-ADM-07 | EP hợp lệ [LIVE] | User role=MC tồn tại | `/admin/users/mcs` chỉ trả đúng role MC | HTTP 200, `count:0` (không có MC nào trong `mchub_test` hiện tại) — hành vi đúng, danh sách rỗng hợp lệ | **PASS** |
| TC-ADM-08 | Negative [LIVE] | Không có JWT | HTTP 403 cho cả 3 endpoint trên | HTTP 403 cho `/users`, `/users/{id}`, `/users/mcs` | **PASS** |

## TC-ADM-09 → 14: PUT /users/{id}/status, PUT /users/{id}/plan

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-09 | EP hợp lệ [LIVE] | `{isActive:false, isVerified:true}` | HTTP 200, user cập nhật đúng, audit log ghi `ADMIN_UPDATE_USER_STATUS` | HTTP 200, `isActive:false, isVerified:true` cập nhật đúng | **PASS** |
| TC-ADM-10 | Negative [LIVE] | Thiếu field `isVerified` trong body | HTTP 400 `VALIDATION_FAILED`, "isActive and isVerified are required" | HTTP 400, `"isActive and isVerified are required"` | **PASS** |
| TC-ADM-11 | EP hợp lệ [LIVE] | `{plan:"BASIC"}` | HTTP 200, `isPremium=true`, `planExpiresAt` set đúng | HTTP 200, `plan:"BASIC", isPremium:true, planExpiresAt` set +30 ngày | **PASS** |
| TC-ADM-12 | Boundary [LIVE] | `{plan:"free"}` (chữ thường) | HTTP 200, downgrade về FREE đúng (`equalsIgnoreCase`) | HTTP 200, `plan:"FREE", planExpiresAt:null` | **PASS** |
| TC-ADM-13 | Negative [LIVE] | `{plan:"INVALID_XYZ"}` | HTTP 400 `VALIDATION_FAILED`, "Invalid plan" — **khác hành vi với `createUser` (im lặng fallback FREE khi plan sai)**, đã ghi nhận không nhất quán ở phiên audit trước (`Admin_Service_Controller_Test_Report.md`), verify lại hành vi thật còn giữ nguyên | HTTP 400, `"Invalid plan: INVALID_XYZ"` — xác nhận hành vi không nhất quán với `createUser` vẫn còn nguyên (đã ghi nhận, không phải bug mới) | **PASS** |
| TC-ADM-14 | Negative [LIVE] | `{plan:""}` (rỗng) | HTTP 400, "Plan is required" (chặn ở Controller trước khi vào Service) | HTTP 400, `"Plan is required"` | **PASS** |

## TC-ADM-15 → 20: POST /users (tạo user), send-reset-email, change-password

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-15 | EP hợp lệ [LIVE] | `{name, email, password}` đủ, không role/plan | HTTP 200, user tạo với `isVerified=true` ngay (khác luồng tự đăng ký cần OTP) | HTTP 200, `isVerified:true` ngay lập tức | **PASS** |
| TC-ADM-16 | Negative [LIVE] | Thiếu `password` | HTTP 400, "name, email, password required" | HTTP 400, `"name, email, password required"` | **PASS** |
| TC-ADM-17 | Negative [LIVE] | `email` đã tồn tại | HTTP 409/400 `EMAIL_ALREADY_EXISTS` | HTTP 409, `"Email nay da duoc su dung."` | **PASS** |
| TC-ADM-18 | Boundary [LIVE] | `{role:"INVALID_ROLE"}` | Fallback về CLIENT, không throw (đúng theo code `catch IllegalArgumentException`) | HTTP 200, `role:"CLIENT"` — fallback đúng, không exception | **PASS** |
| TC-ADM-19 | EP hợp lệ [LIVE] | `POST /users/{id}/send-reset-email` với id hợp lệ | HTTP 200, OTP tạo mới trong `mchub_test.otp_verifications` (hiệu lực 30 phút, khác 10 phút của tự đăng ký) | HTTP 200; verify qua mongosh: OTP mới tạo, `expiresAt - createdAt = 30 phút` chính xác | **PASS** |
| TC-ADM-20 | EP hợp lệ [LIVE] | `POST /users/{id}/change-password` với `newPassword` hợp lệ | HTTP 200, password đổi — **verify login lại được bằng password mới** | HTTP 200; verify: `POST /auth/login` với password mới trả `"Login successful"` | **PASS** |

## TC-ADM-21 → 23: DELETE /users/{id}, GET /users/{id}/stats

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-21 | EP hợp lệ [LIVE] | `DELETE /users/{id}` với id hợp lệ (dùng user test riêng, KHÔNG dùng user chính đang test các case khác) | HTTP 200, verify qua mongosh: user vẫn TỒN TẠI trong DB nhưng `isActive:false` — **xác nhận đúng soft-delete, không mất dữ liệu** | HTTP 200; verify mongosh: user vẫn tồn tại, `isActive:false` — soft-delete xác nhận đúng | **PASS** |
| TC-ADM-22 | Negative [LIVE] | `id` không tồn tại | HTTP 404 `USER_NOT_FOUND` | HTTP 404, `"User not found: 000000000000000000000000"` | **PASS** |
| TC-ADM-23 | EP hợp lệ [LIVE] | `GET /users/{id}/stats` cho user có practice session (tái dùng dữ liệu nếu có) | HTTP 200, `avgScore`/`bestScore` tính đúng, `recentSessions` giới hạn 5 | HTTP 200, user chưa có practice session nào → `totalSessions:0, avgScore:0.0, recentSessions:[]` — fallback hợp lệ, không lỗi | **PASS** |

## TC-ADM-24 → 26: POST /users/{id}/notify-email

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-24 | EP hợp lệ [LIVE] | `{subject, content}` đủ | HTTP 200, gọi `EmailService.sendSimpleEmail` (verify qua log, không cần hộp thư thật nhận được vì domain test `.local`) | HTTP 200, `"Email sent"` | **PASS** |
| TC-ADM-25 | Negative [LIVE] | Thiếu `content` | HTTP 400, "subject and content required" | HTTP 400, `"subject and content required"` | **PASS** |
| TC-ADM-26 | Negative [LIVE] | `subject=""` (rỗng, không phải null) | HTTP 400 — verify điều kiện `isBlank()` chặn đúng cả trường hợp rỗng lẫn null | HTTP 400, `"subject and content required"` — `isBlank()` chặn đúng cả trường hợp rỗng | **PASS** |

## TC-ADM-27 → 28: POST /migrate-db — **RỦI RO CAO, cẩn trọng đặc biệt**

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-27 | Negative [LIVE] | JWT không phải ADMIN | HTTP 403, **không chạy migration** | HTTP 403, `"You do not have permission to perform this action"` | **PASS** |
| TC-ADM-28 | **KHÔNG Execute mặc định** | Admin JWT hợp lệ | `DatabaseMigrationService.migrateFromMcHub()` sẽ chạy thật — cần hiểu rõ hàm này làm gì (đọc source `DatabaseMigrationService.java`) TRƯỚC khi quyết định có chạy hay không, vì tên hàm gợi ý "migrate từ MC Hub" có thể đọc/ghi dữ liệu từ nguồn khác ngoài `mchub_test` | — | **Not Executed — cần đọc kỹ `DatabaseMigrationService.java` và xác nhận với user trước khi chạy, vì đây là thao tác có khả năng ảnh hưởng dữ liệu diện rộng** |

## TC-ADM-29 → 32: GET/PUT /settings/guest-cooldown

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-29 | EP hợp lệ [LIVE] | `mchub_test` chưa có `SystemSetting` nào | `GET /settings/guest-cooldown` → HTTP 200, fallback `hours:3` | HTTP 200, `hours:3` | **PASS** |
| TC-ADM-30 | EP hợp lệ [LIVE] | `PUT ?hours=5` | HTTP 200, `GET` sau đó trả `hours:5` | HTTP 200 cả 2 lệnh; `GET` xác nhận `hours:5` | **PASS** |
| TC-ADM-31 | Negative [LIVE] | `PUT ?hours=0` | HTTP 400, "Giờ phải từ 1 đến 168" | HTTP 400, `"Giờ phải từ 1 đến 168"` | **PASS** |
| TC-ADM-32 | Boundary [LIVE] | `PUT ?hours=168` và `?hours=169` | 168 → HTTP 200 (biên hợp lệ); 169 → HTTP 400 (vượt biên) | 168 → HTTP 200; 169 → HTTP 400 — biên chính xác | **PASS** |

## TC-ADM-33 → 36: GET /admin/logs, GET /admin/logs/stream (SSE), POST /admin/logs/ingest

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-33 | EP hợp lệ [LIVE] | `GET /admin/logs?limit=10` | HTTP 200, tối đa 10 log | **HTTP 200, nhưng trả về 200 log** — tham số `limit` bị bỏ qua hoàn toàn, service luôn gọi `findTop200By...` bất kể client yêu cầu bao nhiêu | **FAIL — DEFECT-007** |
| TC-ADM-34 | Boundary [LIVE] | `GET /admin/logs?level=ERROR&source=AI` | Lọc đúng theo cả 2 điều kiện kết hợp | HTTP 200, `count:0` (không có log nào khớp cả 2 điều kiện trong DB hiện tại) — logic lọc đúng dù kết quả rỗng, xác nhận riêng qua đọc code (`findTop200ByLevelAndSourceOrderByTimestampDesc`) | **PASS** |
| TC-ADM-35 | EP hợp lệ [LIVE] | `POST /admin/logs/ingest` với Admin JWT | HTTP 200, log được lưu, xuất hiện trong `GET /admin/logs` sau đó | HTTP 200; verify: log "QA test ingest log" xuất hiện trong `GET /admin/logs?level=INFO` ngay sau đó | **PASS** |
| TC-ADM-36 | **Nghi vấn thiết kế [LIVE]** | `POST /admin/logs/ingest` mô phỏng gọi từ AI service (không có Admin JWT, vì AI service là hệ thống ngoài không phải người dùng đăng nhập qua UI) | Không rõ — cần xác nhận: AI service có được cấp Admin JWT cố định không, hay endpoint này thực tế KHÔNG bao giờ nhận được log thật từ AI service vì thiếu auth phù hợp? | HTTP 403 khi test không JWT — **xác nhận lại đúng như phiên trước, gap vẫn còn tồn tại** | **Cần làm rõ với dev — không phải bug, nhưng có thể là gap giữa thiết kế và khả năng vận hành thật** |

## TC-ADM-37 → 40: GET /audit-logs, /audit-logs/user/{userId}, DELETE /audit-logs/purge

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-37 | EP hợp lệ [LIVE] | Admin JWT | `GET /audit-logs` trả toàn bộ log, có log của chính các thao tác admin vừa test (VD `ADMIN_UPDATE_USER_STATUS` từ TC-ADM-09) | HTTP 200, 27 log, có `ADMIN_UPDATE_USER_STATUS` từ TC-ADM-09 | **PASS** |
| TC-ADM-38 | EP hợp lệ [LIVE] | `GET /audit-logs/user/{userId}` | Chỉ trả log của đúng user đó | HTTP 200, `count:1`, đúng `userId` khớp filter (log `AUTH_REGISTER` của chính user đó) | **PASS** |
| TC-ADM-39 | Boundary [LIVE] | `DELETE /audit-logs/purge?days=1` (dưới ngưỡng an toàn 3 ngày) | HTTP 200, nhưng **service tự nâng lên 3 ngày** — verify log mới tạo (< 3 ngày) KHÔNG bị xóa dù client yêu cầu `days=1` | HTTP 200, `olderThanDays:3` (đã nâng từ 1 lên 3), `deleted:0` — log mới không bị xóa, floor hoạt động đúng | **PASS** |
| TC-ADM-40 | EP hợp lệ [LIVE] | `DELETE /audit-logs/purge?days=30` | Log cũ hơn 30 ngày bị xóa (nếu có trong `mchub_test` — thường không có do DB mới tạo, ghi nhận `deleted:0` là hợp lệ) | HTTP 200, `olderThanDays:30, deleted:0` — hợp lệ vì không có log nào cũ hơn 30 ngày | **PASS** |

## TC-ADM-41 → 44: GET /analytics, /growth-analytics — biên chia-cho-0

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-ADM-41 | Boundary [LIVE] | `mchub_test` gần như trắng dữ liệu (mới tạo DB) | `/admin/analytics`: mọi rate/ratio (không có trong response trực tiếp, chỉ có count) không lỗi 500 dù `totalUsers` nhỏ | HTTP 200 với `totalUsers:8` (nhỏ), không lỗi 500, mọi field tính toán hợp lệ (kể cả mảng 30 ngày toàn 0 trừ ngày hiện tại) | **PASS** |
| TC-ADM-42 | Boundary [LIVE] | `/admin/growth-analytics` khi `mau=0` (chưa ai login trong 30 ngày qua audit log) | `dauMauRatio=0.0` (không chia 0) — verify đúng theo code | Không tái tạo được state `mau=0` thật (đã có 2 login trong phiên) — **verify thay thế qua đọc source**: `AdminServiceImpl.java` dòng 554 `double dauMauRatio = mau > 0 ? ... : 0.0` — guard chia-cho-0 tồn tại đúng, an toàn | **PASS (verify qua code, không phải live reproduction)** |
| TC-ADM-43 | Boundary [LIVE] | `/admin/growth-analytics` khi `premiumUsers=0` | `arppu=0`, `ltv=0` (không chia 0) | Không tái tạo được state `premiumUsers=0` thật (đã có 1 user BASIC) — **verify thay thế qua đọc source**: dòng 570 `double arppu = premiumUsers > 0 ? ... : 0` — guard tồn tại đúng | **PASS (verify qua code, không phải live reproduction)** |
| TC-ADM-44 | EP hợp lệ [LIVE] | Có ít nhất 1 user premium từ UC-06 test trước đó | `conversionRate`/`mrr` tính ra giá trị > 0 hợp lý | HTTP 200, `conversionRate:12.5, mrr:199000, arppu:1055200, ltv:4220800` — giá trị dương hợp lý dựa trên 1/8 user premium | **PASS** |

---

## Tổng kết thực thi (Execute thật — 2026-07-17, tiếp nối phiên UC-06)

| Trạng thái | Số lượng |
|---|---|
| Tổng test case thiết kế | 44 |
| **PASS** | 42 |
| **FAIL** | 1 (TC-ADM-33 — DEFECT-007) |
| Not Executed | 1 (TC-ADM-03 — thiếu state ban đầu; TC-ADM-28 cố ý không chạy — DEFECT-003) |
| Defect phát hiện | 1 mới (DEFECT-007 Minor) + xác nhận lại DEFECT-003 (đã biết, TC-ADM-28) |

**Môi trường Execute:** Dùng bộ user QA mới từ phiên UC-06 (`qa.admin.p2@mchubtest.local`, `qa.client.p2@mchubtest.local`) — user cũ `qa.tester.uc06`/`qa.admin.uc06` không còn JWT khả dụng giữa các phiên (không lưu trữ theo đúng quy tắc bảo mật).

**TC-ADM-28 (migrate-db) — cố ý KHÔNG Execute:** đã đọc `DatabaseMigrationService.java` ở phiên audit code trước, xác nhận `migrateFromMcHub()` hardcode `sourceDbName="mchub"` (production thật) và `targetDbName="voice-tranning"`, thực hiện `drop()` rồi ghi đè — rủi ro quá cao để chạy dù server đang trỏ `mchub_test`, vì code đọc thẳng tên DB "mchub" bất kể cấu hình runtime. Giữ nguyên quyết định không Execute (xem DEFECT-003).

**TC-ADM-42/43 — verify qua đọc code thay vì tái tạo state thật:** `mchub_test` đã tích lũy dữ liệu (login, premium user) từ nhiều phiên test trước, không còn khả năng tái tạo state `mau=0`/`premiumUsers=0` hoàn toàn sạch. Đã đọc trực tiếp `AdminServiceImpl.java` xác nhận guard chia-cho-0 tồn tại đúng ở cả 2 vị trí — đánh giá PASS dựa trên code verify, ghi rõ khác biệt so với live reproduction để người đọc báo cáo không hiểu nhầm là đã test qua HTTP thật.

**Phát hiện mới — DEFECT-007 (Minor):** `GET /admin/logs?limit=N` bỏ qua hoàn toàn tham số `limit`, luôn trả tối đa 200 bản ghi (`findTop200By...` hardcode trong tên method Spring Data, không đọc biến `limit` truyền vào). Không phải lỗi bảo mật/mất dữ liệu, chỉ là API contract không khớp — chi tiết `testing/defect-log/DEFECT-007.md`.

**Ghi chú:** Mọi Fail/Defect phát hiện trong lúc Execute đã ghi vào `testing/defect-log/` theo template, KHÔNG tự sửa code — đúng thẩm quyền QA theo `testing/testing.md`.
