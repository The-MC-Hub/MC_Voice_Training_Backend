# UC-07 — Onboarding Quest — System Test Cases

**Ngày thực thi:** 2026-07-18
**Môi trường:** LIVE — `mchub_test` (MongoDB Atlas, database riêng), backend chạy port `5555`
**QA users:** `qa.quest.uc07@mchubtest.local` (CLIENT, tạo mới, plan FREE), `qa.client.uc04@mchubtest.local` (CLIENT, tái sử dụng — dùng để verify cách ly dữ liệu giữa user)

---

## Danh sách Test Case

| ID | Mô tả | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| TC-QUEST-01 | GET progress — user mới, chưa làm quest nào | `GET /quests/progress` | 200, `doneCount:0`, `totalQuests:4`, `allDone:false`, `voucherClaimed:false` | Đúng | PASS |
| TC-QUEST-02 | GET progress không JWT (negative — auth) | không header `Authorization` | 401/403 | 403 — đúng, có `@PreAuthorize("isAuthenticated()")` nên Spring Security chặn trước khi vào logic nghiệp vụ, không gặp lỗi 500 như pattern DEFECT-015 | PASS |
| TC-QUEST-03 | Hoàn thành quest "profile" | `POST /quests/complete/profile` | 200, `doneCount:1` | Đúng | PASS |
| TC-QUEST-04 | Nhận voucher TRƯỚC KHI hoàn thành hết quest (negative) | `POST /quests/claim-voucher`, mới xong 1/4 quest | 400, yêu cầu hoàn thành hết trước | Đúng, `ERR_9002` "Hoàn thành tất cả nhiệm vụ tân binh trước khi nhận voucher." | PASS |
| TC-QUEST-05 | Hoàn thành quest ID không tồn tại (negative/EP) | `POST /quests/complete/unknown_quest` | 400, "Unknown quest" | Đúng, `ERR_9002` | PASS |
| TC-QUEST-06 | Hoàn thành lại quest ĐÃ xong (idempotency) | `POST /quests/complete/profile` lần 2 | 200, `doneCount` không tăng thêm (vẫn 1, dùng `Set`) | Đúng, `doneCount:1` cả 2 lần gọi | PASS |
| TC-QUEST-07 | Hoàn thành 3 quest còn lại rồi nhận voucher | `POST /quests/complete/{practice,courses,leaderboard}` tuần tự, rồi `POST /quests/claim-voucher` | 200 mỗi bước, `allDone:true` sau quest thứ 4, voucher tạo thành công với `discountPercent:50`, `applicablePlan:BASIC` | Đúng | PASS |
| TC-QUEST-08 | GET progress sau khi nhận voucher | `GET /quests/progress` | 200, `voucherClaimed:true`, `allDone:true` | Đúng | PASS |
| TC-QUEST-09 | Nhận voucher LẦN 2 (negative — đã nhận rồi) | `POST /quests/claim-voucher` lần 2 | 400, "đã được nhận rồi" | Đúng, `ERR_6007` "Newbie voucher đã được nhận rồi." | PASS |
| TC-QUEST-10 | Xác nhận voucher lưu đúng trong `user_vouchers` | Query trực tiếp DB | 1 document, `discountPercent:50`, `source:NEWBIE_QUEST`, `active:true`, `expiresAt` = `createdAt` + 30 ngày chính xác | Đúng, không có lệch múi giờ (khác với pattern DEFECT-016 — ở đây `expiresAt` tính bằng cộng thời lượng tương đối, không so sánh chéo với nguồn UTC khác nên không lộ lỗi tương tự) | PASS |
| TC-QUEST-11 | Tích hợp: áp dụng mã voucher vào checkout gói BASIC (đúng plan) | `POST /payment/apply-discount?code=NEWBIE50-...&plan=BASIC` | 200, `discountValue:50`, `finalPrice` = 50% giá gốc | Đúng, `originalPrice:199000` → `finalPrice:99500` | PASS |
| TC-QUEST-12 | Tích hợp: áp dụng mã voucher vào gói SAI (negative — `applicablePlans` chỉ có BASIC) | `POST /payment/apply-discount?code=NEWBIE50-...&plan=FULL` | 400, từ chối vì không áp dụng cho gói này | Đúng, "Mã giảm giá không áp dụng cho gói FULL" | PASS |
| TC-QUEST-13 | Cách ly dữ liệu giữa các user (kiểm tra không rò rỉ tiến trình) | `GET /quests/progress` bằng JWT của 1 user KHÁC chưa từng làm quest | 200, tiến trình rỗng độc lập, không lẫn dữ liệu từ user đã test trước | Đúng, `doneCount:0` — hoàn toàn độc lập | PASS |

---

## Ghi chú kỹ thuật

- Mã voucher sinh theo công thức `"NEWBIE50-" + 8 ký tự cuối userId (uppercase)` — đảm bảo tính duy nhất theo user, đã verify DB lưu đúng và mã hoạt động thật trong luồng thanh toán UC-06 (tích hợp cross-UC).
- Toàn bộ luồng UC-07 (3/3 tính năng theo đặc tả: xem tiến trình, hoàn thành quest, nhận voucher) hoạt động đúng 100% — không phát hiện defect nào trong module này.
- Ghi nhận nhỏ (không phải defect): code có 4 quest ID cố định (`profile, practice, courses, leaderboard`) hard-code trong `ALL_QUEST_IDS`, không có API cho phép Admin thêm/bớt quest linh động — nếu cần mở rộng thêm quest trong tương lai sẽ cần sửa code, không cấu hình được qua DB. Đây là nhận xét kiến trúc, không phải lỗi hiện tại, không file defect.

---

## Tổng kết

**13/13 test case đã thực thi.**

| Kết quả | Số lượng |
|---|---|
| PASS | 13 |
| FAIL | 0 |
| Not Executed | 0 |

**0 defect mới phát hiện.** Đây là module đầu tiên trong toàn bộ đợt test hệ thống (UC-06, UC-09, UC-03, UC-04, UC-10, UC-01, UC-02, UC-05, UC-07) đạt 100% PASS — luồng nghiệp vụ đơn giản, rõ ràng, có idempotency đúng, cách ly dữ liệu đúng, và tích hợp cross-module (voucher dùng trong Payment) hoạt động chính xác.
