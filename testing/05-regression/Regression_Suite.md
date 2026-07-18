# Regression Suite — MC Voice Training Backend

**Ngày tạo:** 2026-07-18
**Người tạo:** QA Tester (độc lập)
**Nguồn đối chiếu:** `testing/traceability/Traceability_Matrix.md` (311 test case, 24 defect), `testing/03-system/UC-01..UC-10*_TestCases.md`, `testing/defect-log/DEFECT-001.md` → `DEFECT-024.md`

**Mục đích:** Đây KHÔNG phải bộ test mới. Đây là tập con được chọn lọc từ 311 test case hệ thống đã thực thi, đóng gói thành checklist tái sử dụng để chạy nhanh sau mỗi lần dev báo fix hoặc deploy — theo `testing/testing.md` mục 4 và mục 6.5 (Retest & Regression).

---

## Cách dùng

| Tình huống | Chạy gì |
|---|---|
| Sau **mỗi lần deploy**, bất kể sửa gì | **Tier 1 — Smoke Test** (~13 case) |
| Sau khi dev báo **fix xong 1 defect cụ thể** (VD: DEFECT-016) | **Tier 1** + **Tier 2 — phần DEFECT-016** |
| Sau khi dev **sửa nhiều chỗ trong cùng 1 module** (VD: refactor toàn bộ Payment) | **Tier 3 — module đó** (chạy full file `03-system/UC-XX...`) + **Tier 1** |
| Trước khi release / trước UAT | Tier 1 + Tier 2 (toàn bộ 24 defect còn OPEN) + Tier 3 cho các module có defect Critical/Major |

Nguyên tắc: Tier 1 luôn chạy trước tiên, không bỏ qua — nó là "đèn báo cháy" cross-module. Tier 2/3 chỉ mở rộng phạm vi khi biết cụ thể chỗ nào bị đụng tới.

---

## Tier 1 — Smoke Test (chạy sau MỌI lần deploy)

Mục tiêu: xác nhận luồng sống-còn của từng module chưa bị phá vỡ, không quan tâm sửa gì. Nếu bất kỳ case nào FAIL ở đây, dừng lại — không tiếp tục retest defect cụ thể cho tới khi smoke pass.

| # | TC ID | Mô tả | Module |
|---|---|---|---|
| 1 | TC-AUTH-01 | Đăng ký tài khoản thành công | UC-01 Auth |
| 2 | TC-AUTH-06 | Xác minh email qua OTP | UC-01 Auth |
| 3 | TC-AUTH-12 | Đăng nhập thành công | UC-01 Auth |
| 4 | TC-AUTH-26 | Đăng nhập Admin + xác minh OTP 2FA | UC-01 Auth |
| 5 | TC-VOICE-09 | Phân tích giọng nói AI (happy path) | UC-03 Voice Training |
| 6 | TC-VOICE-24 | Tạo giọng đọc mẫu (TTS) | UC-03 Voice Training (lưu ý: đang FAIL — DEFECT-009, xem case này có tự hết khi deploy khác không) |
| 7 | TC-COURSE-12 | Đăng ký khoá học | UC-04 Courses |
| 8 | TC-COURSE-17 | Nộp bài quiz cuối khoá | UC-04 Courses |
| 9 | TC-PAY-15 | Tạo đơn thanh toán gói cước (happy path) | UC-06 Payment |
| 10 | TC-PAY-29 | Webhook xác nhận thanh toán | UC-06 Payment |
| 11 | TC-SUP-11 | Gửi báo cáo vi phạm (happy path) | UC-08 Support |
| 12 | TC-ADM-02 | Xem tổng quan dashboard Admin | UC-09 Admin |
| 13 | TC-ANN-19 | Duyệt và gửi thông báo (happy path) | UC-10 Marketing |

**Ghi chú:** case #6 (TC-VOICE-24) và bất kỳ case Tier 1 nào khác đang gắn defect OPEN — nếu vẫn FAIL đúng như log cũ thì KHÔNG phải regression mới, chỉ xác nhận defect chưa fix. Regression thật sự là khi một case Tier 1 trước đây PASS nay lại FAIL.

---

## Tier 2 — Defect-specific regression (chạy sau khi dev fix 1 defect cụ thể)

Mỗi mục dưới đây gồm: case gốc phát hiện defect (bắt buộc retest) + case "nearby" cùng module/feature nên chạy kèm để bắt regression phụ. Case nearby lấy từ cùng khối feature trong `Traceability_Matrix.md`.

### DEFECT-001 (Critical) — UC-06 Payment: thiếu whitelist SecurityConfig cho Guest xem gói cước/flash-deal
- Case gốc: TC-PAY-01, TC-PAY-03
- Nearby: TC-PAY-02, TC-PAY-04, TC-PAY-05 (flash-deal, cùng lỗi whitelist), TC-PAY-08 (apply-discount, dùng chung guard)

### DEFECT-002 (Major) — UC-06 Payment: race condition ghi đè plan khi tạo đơn/simulate/complete
- Case gốc: TC-PAY-42, TC-PAY-46
- Nearby: TC-PAY-15 đến TC-PAY-22 (tạo đơn thanh toán gói cước), TC-PAY-41, TC-PAY-43, TC-PAY-44 (admin simulate), TC-PAY-45, TC-PAY-47, TC-PAY-48 (admin xử lý thủ công)

### DEFECT-003 (Critical) — UC-09 Admin: chạy migration database hardcode tên DB
- Case gốc: TC-ADM-28 (trước đó cố ý Not Executed — khi fix, PHẢI Execute thật)
- Nearby: TC-ADM-27 (migration case liền kề)

### DEFECT-004 — UC-01 Auth: `JwtServiceImpl.isTokenValid()` ném exception thay vì trả false (dead code, phát hiện qua unit test)
- Case gốc: không có TC hệ thống — retest bằng unit test `JwtServiceImplTest`
- Nearby: TC-AUTH-12, 13, 14 (đăng nhập, dùng chung JwtService), TC-AUTH-19–23 (đặt lại mật khẩu, cũng dùng JWT)

### DEFECT-005 (Major) — UC-02 Profile: `updateProfile()` partial update xoá âm thầm `personality`/`hostingStyle`
- Case gốc: không có TC hệ thống trực tiếp — retest bằng unit test `MCProfileServiceImplTest`
- Nearby: TC-PROF-07, TC-PROF-08 (cập nhật hồ sơ MC, đổi payload thành partial để xác nhận lộ ra)

### DEFECT-006 (Major) — UC-06 Payment: course giá 0đ trả lỗi 500
- Case gốc: TC-PAY-28
- Nearby: TC-PAY-23, 24, 25, 26, 27 (tạo đơn thanh toán khoá học lẻ)

### DEFECT-007 (Minor) — UC-09 Admin: tham số `limit` bị bỏ qua khi xem log hệ thống
- Case gốc: TC-ADM-33
- Nearby: TC-ADM-34 (log filter khác), TC-ADM-35, TC-ADM-36 (ingest log AI service — cùng khu vực logging)

### DEFECT-008 (Major) — UC-03 Voice: thiếu `scriptOrigin` gây lỗi 500 khi guest luyện tập thử
- Case gốc: TC-VOICE-21
- Nearby: TC-VOICE-22, TC-VOICE-23 (cùng luồng guest trial), TC-VOICE-09–14 (proxy phân tích giọng dùng chung `proxyAnalyzeVoice()`)

### DEFECT-009 (Critical) — UC-03 Voice: hardcode sai URL khi tạo giọng đọc mẫu (TTS)
- Case gốc: TC-VOICE-24
- Nearby: TC-VOICE-25 (TTS case liền kề)

### DEFECT-010 (Major) — UC-03 Voice: thiếu whitelist khi xem cooldown dùng thử
- Case gốc: TC-VOICE-26
- Nearby: TC-VOICE-21, 22, 23 (guest trial flow, cùng nhóm nguyên nhân whitelist)

### DEFECT-011 (Major) — UC-04 Courses: field `isActive` bị bỏ qua khi Admin tạo khoá học (Lombok boolean naming)
- Case gốc: TC-COURSE-06
- Nearby: TC-COURSE-04, TC-COURSE-07, TC-COURSE-21 (admin quản lý khoá học), TC-COURSE-32 (cùng nhóm lỗi Lombok với DEFECT-012)

### DEFECT-012 (Major) — UC-04 Courses: `totalCompletions` luôn = 0 (Spring Data derived query)
- Case gốc: TC-COURSE-32
- Nearby: TC-COURSE-33, TC-COURSE-34 (admin course stats), TC-COURSE-15, 16 (đánh dấu hoàn thành, nguồn dữ liệu completion)

### DEFECT-013 (Major) — UC-04 Courses: 403 cho Guest khi xem bài đọc lý thuyết, thiếu whitelist
- Case gốc: TC-COURSE-05
- Nearby: TC-COURSE-26–31 (ghi chú/highlight bài đọc, phụ thuộc ReadingGuide)

### DEFECT-014 (Major) — UC-10 Marketing: `@Async` nuốt exception khi gửi lại thông báo
- Case gốc: TC-ANN-20
- Nearby: TC-ANN-19, TC-ANN-23, TC-ANN-24 (duyệt và gửi thông báo, cùng feature)

### DEFECT-015 (Major) — UC-01 `/auth/me` + UC-05 `/community/leaderboard/me`: trả 500 thay vì 401 cho Guest
- Case gốc: TC-AUTH-16, TC-COMM-06
- Nearby: TC-AUTH-15 (xem thông tin tài khoản), TC-COMM-05 (xem thứ hạng bản thân) — cùng gốc `GlobalExceptionHandler`

### DEFECT-016 (Critical) — UC-01 Auth: lệch múi giờ khiến JWT mới bị từ chối khi đặt lại mật khẩu
- Case gốc: TC-AUTH-23, TC-AUTH-25
- Nearby: TC-AUTH-17, 18 (quên mật khẩu, bước trước đó trong cùng luồng), TC-AUTH-19–22 (đặt lại mật khẩu, các case còn lại trong feature #8)

### DEFECT-017 (Minor) — UC-02 Profile: đóng băng streak không có tác dụng thật
- Case gốc: TC-PROF-04
- Nearby: TC-PROF-01, TC-PROF-02 (xem chuỗi đăng nhập, cùng feature streak)

### DEFECT-018 (Major) — UC-02 Profile: CLIENT gọi được dashboard MC do thiếu điều kiện role trong `@PreAuthorize`
- Case gốc: TC-PROF-06
- Nearby: TC-PROF-05 (xem dashboard MC, case liền kề)

### DEFECT-019 (Major) — UC-02 Profile: route deprecated (thêm/duyệt/xoá chứng chỉ) trả 500
- Case gốc: TC-PROF-09, TC-PROF-13, TC-PROF-14
- Nearby: TC-PROF-10, TC-PROF-11 (thêm chứng chỉ, case còn lại), TC-PROF-12 (xem chứng chỉ theo hồ sơ MC)

### DEFECT-020 (Major) — UC-02 Profile: tham số `category` không lọc khi khám phá hồ sơ MC công khai
- Case gốc: TC-PROF-16
- Nearby: TC-PROF-15 (cùng feature khám phá, có ghi chú liên quan DEFECT-021)

### DEFECT-021 (Major) — UC-02 Profile: lộ email + field `verified` gây hiểu lầm ở hồ sơ MC công khai
- Case gốc: TC-PROF-15, TC-PROF-17
- Nearby: TC-PROF-16 (khám phá, cùng nhóm feature #9), TC-PROF-18 (xem chi tiết hồ sơ công khai, case liền kề)

### DEFECT-022 (Major) — UC-05 Community: 403 cho Guest khi ghi nhận click bài đăng, thiếu whitelist
- Case gốc: TC-COMM-10
- Nearby: TC-COMM-09, TC-COMM-19, TC-COMM-22 (xem bài đăng mạng xã hội), TC-COMM-20 (ghi nhận click, case liền kề)

### DEFECT-023 (Major) — UC-08 Support: enum sai trả 500 thay vì 400 khi gửi báo cáo vi phạm (ảnh hưởng toàn hệ thống)
- Case gốc: TC-SUP-14
- Nearby: TC-SUP-11, 12, 13 (gửi báo cáo vi phạm, case còn lại), TC-SUP-24, TC-SUP-25 (case bổ sung cùng feature)

### DEFECT-024 (Minor) — UC-08 Support: report không tồn tại trả 500 thay vì 404 khi Admin xử lý báo cáo
- Case gốc: TC-SUP-23
- Nearby: TC-SUP-20, 21, 22 (admin xử lý báo cáo, case còn lại)

---

## Tier 3 — Full module regression (chạy khi sửa nhiều thứ trong 1 module cùng lúc)

Dùng khi thay đổi không khoanh vùng được vào 1 defect cụ thể (VD: refactor lớn, đổi schema, đổi thư viện). Chạy full bộ test-case doc tương ứng.

| Module (UC) | File test case | Số case | Ước lượng thời gian* |
|---|---|---|---|
| UC-01 Authentication | `testing/03-system/UC-01-Authentication_TestCases.md` | 27 | ~45–60 phút |
| UC-02 User & MC Profile | `testing/03-system/UC-02-User-MC-Profile_TestCases.md` | 18 | ~30–40 phút |
| UC-03 Voice Training | `testing/03-system/UC-03-Voice-Training_TestCases.md` | 26 | ~45–60 phút (có gọi AI service, có thể chậm hơn) |
| UC-04 Courses & Learning Path | `testing/03-system/UC-04-Courses-Learning_TestCases.md` | 39 | ~60–75 phút |
| UC-05 Community & Leaderboard | `testing/03-system/UC-05-Community-Leaderboard_TestCases.md` | 26 | ~40–50 phút |
| UC-06 Payment & Subscription | `testing/03-system/UC-06-Payment-Subscription_TestCases.md` | 52 | ~75–90 phút (nhiều luồng thanh toán + webhook) |
| UC-07 Onboarding Quest | `testing/03-system/UC-07-Onboarding-Quest_TestCases.md` | 13 | ~20 phút |
| UC-08 Public, Support & Report | `testing/03-system/UC-08-Public-Support_TestCases.md` | 25 | ~35–45 phút |
| UC-09 Admin Dashboard | `testing/03-system/UC-09-Admin-Dashboard_TestCases.md` | 44 | ~60–75 phút |
| UC-10 Marketing & Communication | `testing/03-system/UC-10-Marketing-Communication_TestCases.md` | 41 | ~55–70 phút |
| **Tổng** | | **311** | ~7–9 giờ (toàn bộ, hiếm khi cần chạy hết cùng lúc) |

*Ước lượng dựa trên tốc độ thực thi thủ công tương tự đợt system test ban đầu, không tính thời gian setup môi trường/data seed.

---

## Ghi chú vận hành

- Case nào đang gắn defect còn **OPEN** thì FAIL lại là chuyện đương nhiên, không tính là regression mới — chỉ regression khi 1 case **trước đó PASS** nay FAIL.
- 4 defect cùng gốc "thiếu whitelist SecurityConfig cho Guest" (DEFECT-001, 010, 013, 022) nên được dev vá đồng loạt 1 lần; khi đó chạy Tier 2 của cả 4 defect cùng lúc là hợp lý thay vì tách 4 lần.
- 4 defect cùng gốc "generic exception trả 500" (DEFECT-015, 019, 023, 024) tương tự — nếu dev sửa `GlobalExceptionHandler` chung, retest cả 4 mục Tier 2 tương ứng cùng lúc.
- Sau khi hoàn tất retest, cập nhật lại trạng thái tương ứng trong `testing/defect-log/DEFECT-XXX.md` (Verified Fixed / Reopened) — không sửa `testing/03-system/*.md` gốc, chỉ log kết quả retest mới.
