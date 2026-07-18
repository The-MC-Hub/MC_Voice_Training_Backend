# Traceability Matrix — MC Voice Training Backend

**Ngày tạo:** 2026-07-18
**Người tạo:** QA Tester (độc lập)
**Nguồn đối chiếu:**
- `docs/use-cases/UC-01..UC-10*.md` (canonical requirement list — feature # theo bảng trong từng file)
- `testing/03-system/UC-01..UC-10*_TestCases.md` (9 file, UC-06 có tài liệu test riêng — không thiếu UC nào)
- `testing/defect-log/DEFECT-001.md` → `DEFECT-024.md` (24 defect)

**Ghi chú phạm vi:** Tất cả 10 UC đều có file spec trong `docs/use-cases/` VÀ đều có file system-test tương ứng trong `testing/03-system/` (UC-06 có file `UC-06-Payment-Subscription_TestCases.md`, không bị thiếu như giả định ban đầu trong đề bài). Do đó bảng dưới đây bao phủ đủ UC-01 → UC-10, không có UC nào bị bỏ qua.

---

## UC-01 — Xác thực & Tài khoản (Authentication)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Đăng ký tài khoản | TC-AUTH-01, 02, 03, 04, 05 | PASS | — |
| 2 | Xác minh email qua link | Chưa có TC riêng (chỉ OTP flow được test) | N/A | — |
| 3 | Xác minh email qua OTP | TC-AUTH-06, 07, 08, 09 | PASS | — |
| 4 | Gửi lại OTP | TC-AUTH-10, 11 | PASS | — |
| 5 | Đăng nhập | TC-AUTH-12, 13, 14 | PASS | — |
| 6 | Xác minh OTP đăng nhập Admin | TC-AUTH-26, 27 | PASS | — |
| 7 | Quên mật khẩu | TC-AUTH-17, 18 | PASS | — |
| 8 | Đặt lại mật khẩu | TC-AUTH-19, 20, 21, 22, 23, 25 | FAIL (TC-AUTH-23, 25) | DEFECT-016 (Critical) |
| 9 | Xem thông tin tài khoản hiện tại | TC-AUTH-15, 16 | FAIL (TC-AUTH-16) | DEFECT-015 (Major) |
| 10 | Tạo mã giới thiệu (referral code) | TC-AUTH-24 (chỉ negative — không JWT) | PASS (case dương tính chưa có TC riêng) | — |
| 11 | Cập nhật cài đặt tài khoản | Gián tiếp qua TC-AUTH-25 (bị chặn bởi DEFECT-016, không phải lỗi riêng của `/settings`) | FAIL (gián tiếp) | DEFECT-016 (cùng defect, không file riêng) |

**Ghi chú bổ sung:** `JwtServiceImpl.isTokenValid()` (dead code, không dùng trong luồng production) có lỗi ném `ExpiredJwtException` thay vì trả `false` — phát hiện qua unit test (`JwtServiceImplTest`), không qua system test UC-01, không gắn với feature # cụ thể nào trong bảng trên. Xem DEFECT-004.

---

## UC-02 — Hồ sơ Người dùng & MC (User & MC Profile)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem chuỗi đăng nhập (streak) | TC-PROF-01, 02 | PASS | — |
| 2 | Đóng băng streak | TC-PROF-04 | FAIL | DEFECT-017 (Minor) |
| 3 | Xem dashboard MC | TC-PROF-05, 06 | FAIL (TC-PROF-06) | DEFECT-018 (Major) |
| 4 | Cập nhật hồ sơ MC | TC-PROF-07, 08 | PASS | — (xem ghi chú DEFECT-005 dưới) |
| 5 | Thêm chứng chỉ | TC-PROF-09, 10, 11 | FAIL (TC-PROF-09) | DEFECT-019 (Major) |
| 6 | Xem chứng chỉ theo hồ sơ MC | TC-PROF-12 | PASS | — |
| 7 | Admin duyệt chứng chỉ | TC-PROF-13 | FAIL | DEFECT-019 (cùng defect, không file riêng) |
| 8 | Xoá chứng chỉ | TC-PROF-14 | FAIL | DEFECT-019 (cùng defect, không file riêng) |
| 9 | Khám phá hồ sơ MC công khai | TC-PROF-15, 16 | FAIL (TC-PROF-16); TC-PROF-15 PASS nhưng có ghi chú | DEFECT-020 (Major, filter category); DEFECT-021 (Major, lộ email) |
| 10 | Xem hồ sơ MC công khai chi tiết | TC-PROF-17, 18 | FAIL (TC-PROF-17) | DEFECT-021 (Major, lộ email + field `verified` gây hiểu lầm) |

**Ghi chú bổ sung:** `MCProfileServiceImpl.updateProfile()` âm thầm xoá `personality`/`hostingStyle` khi partial update (Lombok default `""` không phải `null`) — phát hiện qua unit test (`MCProfileServiceImplTest`), không qua system test TC-PROF-08 (test đó dùng payload đầy đủ field nên không lộ lỗi). Vẫn thuộc feature UC-02 #4. Xem DEFECT-005 (Major).

---

## UC-03 — Luyện giọng AI (Voice Training)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Duyệt thư viện bài luyện | TC-VOICE-05, 06, 07 | PASS | — |
| 2 | Xem chi tiết bài luyện | TC-VOICE-08 | PASS | — |
| 3 | Bài luyện nổi bật | Chưa có TC riêng | N/A | — |
| 4 | Phân tích giọng nói AI | TC-VOICE-09, 10, 11, 12, 13, 14 | PASS | — |
| 5 | Luyện tập thử cho khách (guest) | TC-VOICE-21, 22, 23 | FAIL (TC-VOICE-21) | DEFECT-008 (Major) |
| 6 | Proxy phân tích giọng trực tiếp | Gián tiếp qua ghi chú DEFECT-008 (dùng chung `proxyAnalyzeVoice()`, chưa có TC riêng độc lập) | N/A (chưa có TC riêng, chỉ nghi vấn qua code review) | DEFECT-008 (nghi vấn ảnh hưởng, chưa xác nhận độc lập) |
| 7 | Xem lịch sử luyện tập | TC-VOICE-19 | PASS | — |
| 8 | Xem chi tiết một phiên luyện tập | TC-VOICE-18, 19, 20 (IDOR guard) | PASS | — |
| 9 | Thống kê độ khó thích ứng | TC-VOICE-17 | PASS | — |
| 10 | Tạo giọng đọc mẫu (TTS) | TC-VOICE-24, 25 | FAIL (TC-VOICE-24) | DEFECT-009 (Critical) |
| 11 | Xem thời gian cooldown dùng thử | TC-VOICE-26 | FAIL | DEFECT-010 (Major) |
| 12 | Admin thêm bài luyện | TC-VOICE-02 | PASS | — |
| 13 | Admin sửa bài luyện | TC-VOICE-03 | PASS | — |
| 14 | Admin xoá bài luyện | TC-VOICE-01, 04 | PASS | — |

**Ghi chú bổ sung:** TC-VOICE-15, 16 xác nhận side-effect (UserStats, practiceCount) sau khi phân tích giọng — không map trực tiếp 1 feature riêng, thuộc bổ trợ cho feature #4/#9.

---

## UC-04 — Khoá học & Lộ trình học (Courses & Learning Path)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem lộ trình học (roadmap) | TC-COURSE-01 | PASS | — |
| 2 | Xem loại khoá học | TC-COURSE-02 | PASS | — |
| 3 | Danh sách khoá học | TC-COURSE-03, 08, 11 | PASS | — |
| 4 | Xem chi tiết khoá học | TC-COURSE-09, 10 | PASS | — |
| 5 | Xem bài đọc lý thuyết | TC-COURSE-05 | FAIL | DEFECT-013 (Major); ghi chú thêm: không có Admin API tạo ReadingGuide (gap chức năng, xem DEFECT-011 phần status) |
| 6 | Đăng ký khoá học | TC-COURSE-12, 35, 36 | PASS | — |
| 7 | Nhận khoá học tặng | TC-COURSE-13, 14 | PASS | — |
| 8 | Đánh dấu hoàn thành bài luyện | TC-COURSE-15, 16 | PASS | — |
| 9 | Đánh dấu hoàn thành bài đọc | TC-COURSE-16 (gộp chung với bài luyện) | PASS | — |
| 10 | Nộp bài quiz cuối khoá | TC-COURSE-17, 19, 20 | PASS | — |
| 11 | Xem khoá học đã đăng ký | Chưa có TC riêng (gián tiếp qua TC-COURSE-14 xác nhận `hasAccess`) | N/A (chỉ gián tiếp) | — |
| 12 | Xem chứng chỉ đã đạt được | TC-COURSE-18 | PASS | — |
| 13 | Ghi chú/highlight bài đọc | TC-COURSE-26, 27, 28, 29, 30, 31 | PASS | — (phụ thuộc ReadingGuide fixture seed thủ công, xem DEFECT-011 ghi chú) |
| 14 | Admin quản lý khoá học | TC-COURSE-04, 06, 07, 21, 32, 33, 34, 37, 38, 39 | FAIL (TC-COURSE-06, 32) | DEFECT-011 (Major, `isActive` bị bỏ qua); DEFECT-012 (Major, `totalCompletions` luôn 0) |
| 15 | Admin chỉnh giá khoá học | TC-COURSE-22, 23, 24, 25 | PASS | — |

---

## UC-05 — Cộng đồng & Bảng xếp hạng (Community & Leaderboard)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem thống kê cộng đồng | TC-COMM-01 | PASS | — |
| 2 | Xem bảng xếp hạng | TC-COMM-02, 03, 04 | PASS | — |
| 3 | Xem thứ hạng của bản thân | TC-COMM-05, 06 | FAIL (TC-COMM-06) | DEFECT-015 (Major, cùng defect UC-01, bổ sung evidence) |
| 4 | Xem giải đấu đang diễn ra | TC-COMM-07, 08, 13, 17 | PASS | — |
| 5 | Admin tạo giải đấu | TC-COMM-11, 12 | PASS | — |
| 6 | Admin sửa giải đấu | TC-COMM-14, 26 | PASS | — |
| 7 | Admin xoá giải đấu | TC-COMM-15, 16 | PASS | — |
| 8 | Xem bài đăng mạng xã hội | TC-COMM-09, 19, 22 | PASS | — |
| 9 | Ghi nhận click bài đăng | TC-COMM-10, 20 | FAIL (TC-COMM-10) | DEFECT-022 (Major) |
| 10 | Admin quản lý bài đăng mạng xã hội | TC-COMM-18, 21, 23, 24, 25 | PASS | — |

---

## UC-06 — Thanh toán & Gói cước (Payment & Subscription)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem danh sách gói cước | TC-PAY-01, 02, 03, 04 | FAIL (TC-PAY-01, 03) | DEFECT-001 (Critical) |
| 2 | Xem ưu đãi giới hạn thời gian | TC-PAY-05, 06, 07 | PASS (dùng JWT né DEFECT-001) | DEFECT-001 (cùng nhóm nguyên nhân, guest bị chặn) |
| 3 | Áp dụng mã giảm giá | TC-PAY-08, 09, 10, 11, 12, 13, 14 | PASS | — |
| 4 | Tạo đơn thanh toán gói cước | TC-PAY-15, 16, 17, 18, 19, 20, 21, 22 | PASS (TC-PAY-46 đối chứng phát hiện phụ) | DEFECT-002 (Major, race condition downgrade plan — phát hiện phụ, không phải TC thiết kế sẵn) |
| 5 | Tạo đơn thanh toán khoá học lẻ | TC-PAY-23, 24, 25, 26, 27, 28 | FAIL (TC-PAY-28) | DEFECT-006 (Major) |
| 6 | Webhook xác nhận thanh toán | TC-PAY-29, 30, 31, 32, 33, 34, 35, 36 | PASS | — |
| 7 | Xem trạng thái thanh toán | TC-PAY-37, 38, 39, 40 | PASS | — |
| 8 | Admin giả lập thanh toán thành công | TC-PAY-41, 42, 43, 44 | PASS (TC-PAY-42 liên quan DEFECT-002) | DEFECT-002 (liên đới) |
| 9 | Admin xử lý thủ công giao dịch | TC-PAY-45, 46, 47, 48 | PASS (TC-PAY-46 liên quan DEFECT-002) | DEFECT-002 (liên đới) |
| 10 | Admin quản lý gói cước | TC-PAY-49 | PASS | — |
| 11 | Admin khởi tạo gói Ngày (DAILY) | TC-PAY-51 | PASS | — |
| 12 | Admin quản lý mã giảm giá | TC-PAY-50 | PASS | — |
| 13 | Xem voucher của tôi | TC-PAY-52 | PASS | — |
| 14 | Xem voucher khả dụng | Chưa có TC riêng (chỉ voucher đã claim được test qua UC-07 TC-QUEST-10/11) | N/A (gián tiếp qua UC-07) | — |

---

## UC-07 — Quest Onboarding & Ưu đãi Người mới

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem tiến trình quest | TC-QUEST-01, 02, 08, 13 | PASS | — |
| 2 | Hoàn thành một quest | TC-QUEST-03, 05, 06, 07 | PASS | — |
| 3 | Nhận voucher người mới | TC-QUEST-04, 07, 09, 10, 11, 12 | PASS | — |

**Ghi chú:** UC-07 là module duy nhất đạt 100% PASS, 0 defect trong toàn bộ đợt test.

---

## UC-08 — Trang công khai, Hỗ trợ & Báo cáo

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem trang landing công khai | TC-SUP-01 | PASS | — |
| 2 | Xem thống kê luyện tập nổi bật | TC-SUP-02 | PASS | — |
| 3 | Xem danh sách enum công khai | TC-SUP-03, 04 | PASS | — |
| 4 | Gửi liên hệ/hỗ trợ | TC-SUP-05, 06, 07, 08 | PASS | — |
| 5 | Upload file media | TC-SUP-09, 10 | PASS | — |
| 6 | Gửi báo cáo vi phạm | TC-SUP-11, 12, 13, 14, 24, 25 | FAIL (TC-SUP-14) | DEFECT-023 (Major, ảnh hưởng toàn hệ thống) |
| 7 | Xem báo cáo của tôi | TC-SUP-15, 16 | PASS | — |
| 8 | Admin xem danh sách báo cáo | TC-SUP-17, 18, 19 | PASS | — (ghi chú: `reporterName`/`reportedName` luôn null, không phải defect — chủ đích) |
| 9 | Admin xử lý báo cáo | TC-SUP-20, 21, 22, 23 | FAIL (TC-SUP-23) | DEFECT-024 (Minor) |

---

## UC-09 — Quản trị Hệ thống & Người dùng (Admin Dashboard)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem tổng quan dashboard | TC-ADM-02, 03 | PASS (TC-ADM-03 Not Executed) | — |
| 2 | Xem danh sách giao dịch | TC-ADM-04 | PASS | — |
| 3 | Xem thống kê doanh thu | Gián tiếp qua TC-ADM-02 (`totalRevenue`), chưa có TC riêng cho `/revenue-stats` | N/A (gián tiếp) | — |
| 4 | Xem phân tích nền tảng | TC-ADM-41 | PASS | — |
| 5 | Xem phân tích tăng trưởng | TC-ADM-42, 43, 44 | PASS (TC-ADM-42, 43 verify qua code review, không phải live reproduction) | — |
| 6 | Xem danh sách người dùng | TC-ADM-05, 08 | PASS | — |
| 7 | Xem chi tiết một người dùng | TC-ADM-06 | PASS | — |
| 8 | Xem danh sách MC | TC-ADM-07 | PASS | — |
| 9 | Đổi trạng thái tài khoản | TC-ADM-09, 10 | PASS | — |
| 10 | Đổi gói cước thủ công | TC-ADM-11, 12, 13, 14 | PASS | — |
| 11 | Tạo tài khoản mới | TC-ADM-15, 16, 17, 18 | PASS | — |
| 12 | Gửi email đặt lại mật khẩu | TC-ADM-19 | PASS | — |
| 13 | Đổi mật khẩu người dùng | TC-ADM-20 | PASS | — |
| 14 | Xoá tài khoản người dùng | TC-ADM-21, 22 | PASS | — (ghi chú: doc mô tả "xoá vĩnh viễn" nhưng code chỉ soft-delete — mismatch tài liệu, không phải bug) |
| 15 | Xem thống kê sử dụng của user | TC-ADM-23 | PASS | — |
| 16 | Gửi email thông báo cho user | TC-ADM-24, 25, 26 | PASS | — |
| 17 | Chạy migration database | TC-ADM-27, 28 | FAIL/Not Executed (TC-ADM-28 cố ý không chạy) | DEFECT-003 (Critical) |
| 18 | Xem/sửa cấu hình cooldown dùng thử | TC-ADM-29, 30, 31, 32 | PASS | — |
| 19 | Xem log hệ thống realtime (SSE) | Chưa có TC riêng — `GET /admin/logs/stream` không được Execute trong system test | N/A (chưa test, chỉ log tra cứu thường được test — xem feature #20) | — |
| 20 | Xem log hệ thống theo bộ lọc | TC-ADM-33, 34 | FAIL (TC-ADM-33) | DEFECT-007 (Minor) |
| 21 | Ghi nhận log từ AI service | TC-ADM-35, 36 | PASS (TC-ADM-36 là "nghi vấn thiết kế", không phải Fail chính thức) | Không file defect — ghi nhận là gap khả dụng cần dev xác nhận (AI service có JWT ADMIN để gọi ingest hay không) |
| 22 | Xem audit log toàn hệ thống | TC-ADM-37 | PASS | — |
| 23 | Xem audit log theo user | TC-ADM-38 | PASS | — |
| 24 | Xoá audit log cũ | TC-ADM-39, 40 | PASS | — |

---

## UC-10 — Marketing & Truyền thông (Email Campaign & Announcement)

| Feature # | Feature mô tả | Test Case ID(s) | Kết quả | Defect ID |
|---|---|---|---|---|
| 1 | Xem danh sách thông báo | TC-ANN-01 | PASS | — |
| 2 | Xem thông báo nháp | TC-ANN-02 | PASS | — |
| 3 | Xem chi tiết thông báo | TC-ANN-05 | PASS | — |
| 4 | Xem trước số liệu người nhận | TC-ANN-06 | PASS | — |
| 5 | Tạo thông báo mới | TC-ANN-03, 04 | PASS | — |
| 6 | Sửa thông báo | TC-ANN-07 | PASS | — |
| 7 | Xoá thông báo | TC-ANN-21, 21b, 22 | PASS | — |
| 8 | Xem trước email HTML | TC-ANN-08 | PASS | — |
| 9 | Xem trước email từ nội dung nháp | TC-ANN-09 | PASS | — |
| 10 | Lọc người dùng theo gói cước | TC-ANN-10, 11, 12 | PASS | — |
| 11 | Duyệt và gửi thông báo | TC-ANN-19, 20, 23, 24 | FAIL (TC-ANN-20) | DEFECT-014 (Major) |
| 12 | Tự động tạo thông báo bài học mới | TC-ANN-13 | PASS | — |
| 13 | Tự động tạo thông báo khuyến mãi | TC-ANN-14 | PASS | — |
| 14 | Tự động tạo thông báo bảo trì | TC-ANN-15 | PASS | — |
| 15 | Tự động tạo thông báo bài đăng mới | TC-ANN-16 | PASS | — |
| 16 | Tự động tạo thông báo tính năng mới | TC-ANN-17 | PASS | — |
| 17 | Tự động tạo thông báo giải đấu mới | TC-ANN-18 | PASS | — |
| 18 | Tạo mẫu email | TC-CAMP-01 | PASS | — |
| 19 | Xem danh sách mẫu email | TC-CAMP-02 | PASS | — |
| 20 | Sửa mẫu email | TC-CAMP-03 | PASS | — |
| 21 | Xoá mẫu email | TC-CAMP-16 | PASS | — |
| 22 | Gửi chiến dịch email hàng loạt | TC-CAMP-09, 14, 17 | PASS | — |
| 23 | Xem trước người nhận chiến dịch | TC-CAMP-04, 06 | PASS | — |
| 24 | Đếm số người nhận chiến dịch | TC-CAMP-05, 07, 08 | PASS | — |
| 25 | Xem danh sách chiến dịch | TC-CAMP-10, 15 | PASS | — |
| 26 | Xem chi tiết một chiến dịch | TC-CAMP-11 | PASS | — |
| 27 | Xem log gửi của chiến dịch | TC-CAMP-12 | PASS | — |
| 28 | Gửi email thử nghiệm | TC-CAMP-13 | PASS | — |

---

## Tổng kết Traceability

### 1. Tổng số feature đã đánh số trong toàn bộ đặc tả UC

| UC | Số feature |
|---|---|
| UC-01 | 11 |
| UC-02 | 10 |
| UC-03 | 14 |
| UC-04 | 15 |
| UC-05 | 10 |
| UC-06 | 14 |
| UC-07 | 3 |
| UC-08 | 9 |
| UC-09 | 24 |
| UC-10 | 28 |
| **Tổng** | **138** |

### 2. Độ bao phủ test case

- Feature có ít nhất 1 test case (trực tiếp hoặc gián tiếp) bao phủ: **130 / 138** (~94.2%)
- Feature hoàn toàn **chưa có test case riêng** (N/A — "Chưa có TC riêng" hoặc chỉ gián tiếp/chưa test):
  1. UC-01 #2 — Xác minh email qua link (chỉ luồng OTP được test, không có TC cho luồng link)
  2. UC-03 #3 — Bài luyện nổi bật (chưa có TC riêng)
  3. UC-03 #6 — Proxy phân tích giọng trực tiếp (chỉ được nhắc tới gián tiếp qua ghi chú DEFECT-008, chưa có TC độc lập xác nhận)
  4. UC-04 #11 — Xem khoá học đã đăng ký (chỉ gián tiếp qua TC-COURSE-14 xác nhận `hasAccess`, không có TC gọi endpoint danh sách "khoá học đã đăng ký" trực tiếp)
  5. UC-06 #14 — Xem voucher khả dụng (chỉ gián tiếp qua UC-07 TC-QUEST-10/11, không có TC riêng cho endpoint "voucher khả dụng")
  6. UC-09 #3 — Xem thống kê doanh thu (`/admin/revenue-stats`) — chỉ gián tiếp qua field `totalRevenue` trong TC-ADM-02, không có TC gọi riêng endpoint revenue-stats
  7. UC-09 #19 — Xem log hệ thống realtime qua SSE (`/admin/logs/stream`) — hoàn toàn chưa được Execute trong system test (chỉ log tra cứu thường — feature #20 — được test)
  8. UC-09 #21 — Ghi nhận log từ AI service — có TC (TC-ADM-35, 36) nhưng TC-ADM-36 chỉ dừng ở "nghi vấn thiết kế", chưa xác nhận đầy đủ khả năng vận hành thật với AI service ngoài

  (7 mục N/A hoàn toàn + phần "gián tiếp/chưa đầy đủ" như UC-04 #9, #13, UC-08 #8 đã có TC riêng dù kèm ghi chú — không tính vào nhóm N/A)

### 3. Reverse-index — Defect ID → UC/Feature

- DEFECT-001 → UC-06 #1, #2, #3 (Xem gói cước / flash-deal / apply-discount — thiếu whitelist SecurityConfig cho Guest) — TC-PAY-01, TC-PAY-03, TC-PAY-05, TC-PAY-08
- DEFECT-002 → UC-06 #4, #8, #9 (Tạo đơn thanh toán gói cước / Admin simulate / Admin complete — race condition ghi đè plan) — TC-PAY-42, TC-PAY-46
- DEFECT-003 → UC-09 #17 (Chạy migration database — hardcode tên DB, rủi ro production) — TC-ADM-28 (cố ý không Execute)
- DEFECT-004 → UC-01 (JwtService.isTokenValid — dead code, không gắn feature # cụ thể, phát hiện qua unit test) — không có TC hệ thống
- DEFECT-005 → UC-02 #4 (Cập nhật hồ sơ MC — partial update xoá field âm thầm, phát hiện qua unit test) — không có TC hệ thống trực tiếp lộ ra (TC-PROF-08 dùng payload đầy đủ)
- DEFECT-006 → UC-06 #5 (Tạo đơn thanh toán khoá học lẻ — course giá 0đ trả 500) — TC-PAY-28
- DEFECT-007 → UC-09 #20 (Xem log hệ thống theo bộ lọc — tham số `limit` bị bỏ qua) — TC-ADM-33
- DEFECT-008 → UC-03 #5 (Luyện tập thử cho khách — thiếu `scriptOrigin` gây 500) — TC-VOICE-21
- DEFECT-009 → UC-03 #10 (Tạo giọng đọc mẫu TTS — hardcode sai URL) — TC-VOICE-24
- DEFECT-010 → UC-03 #11 (Xem thời gian cooldown dùng thử — thiếu whitelist) — TC-VOICE-26
- DEFECT-011 → UC-04 #14 (Admin quản lý khoá học — field `isActive` bị bỏ qua khi tạo) — TC-COURSE-06
- DEFECT-012 → UC-04 #14 (Admin quản lý khoá học — `totalCompletions` luôn = 0) — TC-COURSE-32
- DEFECT-013 → UC-04 #5 (Xem bài đọc lý thuyết — 403 cho Guest, thiếu whitelist) — TC-COURSE-05
- DEFECT-014 → UC-10 #11 (Duyệt và gửi thông báo — `@Async` nuốt exception guard khi gửi lại) — TC-ANN-20
- DEFECT-015 → UC-01 #9 (Xem thông tin tài khoản `/auth/me`) và UC-05 #3 (Xem thứ hạng bản thân `/community/leaderboard/me`) — trả 500 thay vì 401 cho Guest — TC-AUTH-16, TC-COMM-06
- DEFECT-016 → UC-01 #8 (Đặt lại mật khẩu — lệch múi giờ khiến JWT mới bị từ chối) — TC-AUTH-23, TC-AUTH-25
- DEFECT-017 → UC-02 #2 (Đóng băng streak — endpoint không có tác dụng thật) — TC-PROF-04
- DEFECT-018 → UC-02 #3 (Xem dashboard MC — CLIENT gọi được do thiếu điều kiện role trong `@PreAuthorize`) — TC-PROF-06
- DEFECT-019 → UC-02 #5, #7, #8 (Thêm/duyệt/xoá chứng chỉ — route deprecated trả 500) — TC-PROF-09, TC-PROF-13, TC-PROF-14
- DEFECT-020 → UC-02 #9 (Khám phá hồ sơ MC công khai — tham số `category` không lọc) — TC-PROF-16
- DEFECT-021 → UC-02 #9, #10 (Khám phá/Xem chi tiết hồ sơ MC công khai — lộ email + field `verified` gây hiểu lầm) — TC-PROF-15, TC-PROF-17
- DEFECT-022 → UC-05 #9 (Ghi nhận click bài đăng mạng xã hội — 403 cho Guest, thiếu whitelist) — TC-COMM-10
- DEFECT-023 → UC-08 #6 (Gửi báo cáo vi phạm — enum sai trả 500 thay vì 400, ảnh hưởng toàn hệ thống) — TC-SUP-14
- DEFECT-024 → UC-08 #9 (Admin xử lý báo cáo — report không tồn tại trả 500 thay vì 404) — TC-SUP-23

---

## Ghi chú tổng hợp

- Tổng số test case đã thực thi trên toàn bộ 10 UC (theo tổng kết từng file system-test): 27 (UC-01) + 18 (UC-02) + 26 (UC-03) + 39 (UC-04) + 26 (UC-05) + 52 (UC-06) + 13 (UC-07) + 25 (UC-08) + 44 (UC-09) + 41 (UC-10) = **311 test case**.
- Nhóm defect lặp lại nhiều lần cùng 1 nguyên nhân gốc (thiếu whitelist `SecurityConfig` cho endpoint public): DEFECT-001, DEFECT-010, DEFECT-013, DEFECT-022 — xuất hiện ở 4 module khác nhau (Payment, Voice, Courses, Community), đề xuất dev rà soát tổng thể 1 lần thay vì vá từng chỗ.
- Nhóm defect lặp lại nguyên nhân "generic exception trả 500 thay vì mã lỗi đúng ngữ nghĩa": DEFECT-015 (IllegalStateException), DEFECT-019 (UnsupportedOperationException), DEFECT-023 (HttpMessageNotReadableException), DEFECT-024 (RuntimeException trần) — cùng gốc `GlobalExceptionHandler` thiếu handler chuyên biệt.
- Nhóm defect lặp lại nguyên nhân "Lombok boolean `isXxx` field naming mismatch": DEFECT-011 (Jackson deserialize), DEFECT-012 (Spring Data derived query) — cùng gốc coding convention.
- DEFECT-004 và DEFECT-005 được phát hiện qua **unit test**, không qua system test (`testing/03-system/`) — vẫn đưa vào reverse-index vì có thể truy về đúng UC/feature theo nội dung defect, nhưng không có Test Case ID hệ thống (TC-XXX) trực tiếp gắn kèm.
