## DEFECT-002: Transaction hoàn tất trễ có thể downgrade ngược plan user (race condition giữa các giao dịch PENDING chồng lấn)

- **Module:** Payment & Subscription (UC-06)
- **Ngày phát hiện:** 2026-07-17
- **Test case liên quan:** TC-PAY-42, TC-PAY-46 (`testing/03-system/UC-06-Payment-Subscription_TestCases.md`) — phát hiện phụ khi Execute tuần tự, không phải test case thiết kế sẵn cho race condition này
- **Severity:** Major (không chặn luồng chính, nhưng gây sai lệch dữ liệu gói cước — ảnh hưởng trực tiếp quyền lợi user đã trả tiền)
- **Priority:** P1
- **Môi trường:** `mchub_test`, port 5555

### Steps to Reproduce
1. User tạo order `BASIC` (199,000đ) qua `POST /create-order?plan=BASIC` → transaction A tạo lúc T0, trạng thái `PENDING`.
2. (Giữa chừng, trước khi transaction A được xác nhận) Admin dùng `POST /simulate-success?userId=...&plan=FULL` → user được set `plan=FULL`, `isPremium=true`, `planExpiresAt` = T1+30 ngày.
3. Sau đó, transaction A (BASIC, tạo từ T0) được xác nhận hoàn tất qua `POST /admin/complete/{transactionId}` (mô phỏng webhook đến trễ) → user bị ghi đè `plan=BASIC`.

### Expected Result
Cần làm rõ với Product Owner đây là hành vi mong muốn hay không, nhưng về trực giác nghiệp vụ: nếu user đã đang ở gói FULL (cao hơn/mới hơn), 1 giao dịch BASIC cũ hoàn tất trễ **không nên tự động hạ cấp** xuống BASIC — ít nhất cần business rule rõ ràng (VD: chỉ áp dụng nếu `tx.completedAt > user.planExpiresAt` gần nhất, hoặc chặn hoàn toàn giao dịch cũ hơn ghi đè giao dịch mới hơn).

### Actual Result
User bị ghi đè thẳng từ `FULL` → `BASIC` chỉ vì transaction cũ (tạo trước) được xác nhận sau. Xác nhận qua truy vấn trực tiếp `mchub_test.users`: `{plan: "BASIC", isPremium: true}` sau chuỗi thao tác trên, dù thao tác gần nhất về mặt "ý định nâng cấp" là FULL.

### Root Cause (quan sát được, không tự sửa)
`PaymentController.adminCompleteTransaction()` (dòng ~424-428) và `handlePaymentWebhook()` (dòng ~290-298) đều set `user.setPlan(tx.getPlan())` **vô điều kiện** khi transaction chuyển `COMPLETED`, không so sánh với plan hiện tại của user hay thời điểm tạo giao dịch so với lần nâng cấp gần nhất. Cùng logic tồn tại ở cả webhook thật lẫn 2 endpoint admin bypass (`simulate-success` không bị ảnh hưởng vì nó tự set trực tiếp, nhưng là nguồn gây ra plan "cao" bị 1 giao dịch "thấp hơn nhưng cũ hơn" ghi đè).

### Impact nghiệp vụ
Trong thực tế: user thanh toán BASIC, chưa kịp nhận xác nhận (webhook PayOS có độ trễ), rồi đổi ý nâng cấp lên FULL ngay lập tức và thanh toán thành công trước. Nếu webhook của giao dịch BASIC cũ đến sau (hoàn toàn có thể xảy ra với hệ thống thanh toán bất đồng bộ), user bị hạ cấp ngược xuống BASIC dù đã trả tiền cho FULL — **rủi ro tài chính/trải nghiệm khách hàng thật**, có thể gây khiếu nại.

### Evidence
Trace lại từ 2 lần gọi API (giữ log terminal): `simulate-success plan=FULL` lúc 10:49:57 trả `plan:"FULL"`; `admin/complete` transaction BASIC (tạo lúc 10:45:58, trước đó) lúc 10:50:30 trả `plan:"BASIC"`; truy vấn DB sau đó xác nhận user ở trạng thái `plan:"BASIC"`.

### Status
**Fixed (2026-07-18).** Thêm helper `applyPlanUpgrade(User user, SubscriptionPlan newPlan)` dùng chung cho cả `handlePaymentWebhook` và `adminCompleteTransaction` (2 nơi từng ghi đè plan độc lập, dễ lệch logic nếu sửa riêng lẻ). Guard: nếu user đang có plan còn hiệu lực (`planExpiresAt` chưa qua) VÀ plan mới thấp hơn plan hiện tại (so theo thứ tự khai báo enum `SubscriptionPlan`: `FREE < DAILY < BASIC < FULL < ANNUAL`), bỏ qua việc áp dụng — chỉ log cảnh báo, KHÔNG ghi đè `plan`/`planExpiresAt`. Giao dịch vẫn được đánh dấu `COMPLETED` đúng (không mất dấu vết kế toán), chỉ riêng phần cập nhật plan bị chặn có kiểm soát.

**Verify live (port 5555, `mchub_test`)** — tái hiện đúng kịch bản gốc:
1. Tạo order BASIC (transaction A, `PENDING`).
2. `simulate-success` FULL cho cùng user → `plan:FULL`, `planExpiresAt` = +30 ngày.
3. `admin/complete/{transactionA}` (BASIC, giao dịch cũ) → response vẫn trả thành công (giao dịch được đánh dấu hoàn tất đúng), nhưng verify DB: `db.users.findOne(...)` → **`plan` vẫn là `FULL`, `planExpiresAt` không đổi** — không bị ghi đè xuống BASIC như trước.
4. Log server xác nhận guard hoạt động: `PLAN UPGRADE SKIPPED (would downgrade): user=... currentPlan=FULL (expires ...) — incoming plan=BASIC ignored`.

`PaymentControllerTest` — 24/24 PASS, không hồi quy.

**Lưu ý nhỏ chưa xử lý:** response message của `adminCompleteTransaction` khi bị guard chặn vẫn ghi "Plan activated" dù thực tế không đổi gì — hơi gây hiểu lầm cho admin đọc response, nhưng không sai về mặt dữ liệu (chỉ là UX message, có thể cải thiện thêm sau nếu cần).

### Ghi chú
Đây không nằm trong 52 test case thiết kế ban đầu — phát sinh tự nhiên từ thứ tự Execute thật (đúng giá trị của integration/system test thật so với chỉ trace logic tĩnh). Đề xuất bổ sung test case chính thức cho race condition này vào bộ regression sau khi fix.
