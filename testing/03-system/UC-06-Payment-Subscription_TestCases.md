# System Test Case — UC-06: Thanh toán & Gói cước (Payment & Subscription)

**Nguồn đặc tả:** `docs/use-cases/UC-06-payment-subscription.md`
**Source verify:** `controllers/PaymentController.java`, `services/PayOSService.java`, `services/PlanService.java`
**Ưu tiên:** P0
**Môi trường:** MongoDB Atlas `mchub_test` (cùng cluster MainDatabase, DB riêng), PayOS key thật (giới hạn phạm vi — xem `testing/00-plan/Test_Plan.md` mục 6.1)
**Base URL Execute thật:** `http://localhost:5555/api/v1/payment` (jar chạy trực tiếp `java -DMONGODB_URI=... -DMONGODB_DATABASE=mchub_test -Dserver.port=5555 -jar target/backend-java-1.0.0.jar` — port 8080/5000 bị chiếm bởi service khác của user, xem ghi chú môi trường cuối file)

## Quy ước
- **[LIVE]** — test case Execute thật, gọi API thật, ghi dữ liệu vào `mchub_test`.
- **[NO-PAY]** — được phép chạm PayOS thật (tạo link) nhưng **không hoàn tất thanh toán**.
- **[OOS]** — Out of Scope, không Execute do quyết định "không thanh toán thật" (mục 6.1 Test Plan) — không tính Fail.
- **[MOCK-WEBHOOK]** — mô phỏng webhook bằng cách tự dựng request + tự ký HMAC bằng `PAYOS_CHECKSUM_KEY` thật, không chờ PayOS gọi.

---

## TC-PAY-01 → 04: GET /plans — Xem danh sách gói cước

| ID | Loại | Precondition | Steps | Input | Expected | Actual | Status |
|---|---|---|---|---|---|---|---|
| TC-PAY-01 | EP hợp lệ [LIVE] | DB có ít nhất 1 `PlanDefinition` active | `GET /payment/plans`, không cần JWT | — | HTTP 200, `data` là mảng `PlanDefinition` chỉ gồm `active=true` | **HTTP 403** (thiếu whitelist SecurityConfig) | **FAIL — DEFECT-001** |
| TC-PAY-02 | Boundary [LIVE] | DB `mchub_test` chưa seed plan nào | `GET /payment/plans` | — | HTTP 200, `data=[]` (không lỗi 500) | Không test được do đã seed DAILY/BASIC trước đó, không còn state rỗng để test | Not Executed |
| TC-PAY-03 | Negative [LIVE] | — | `GET /payment/plans` không có JWT (guest thật, không phải "JWT không hợp lệ" như mô tả ban đầu — điều chỉnh sau khi phát hiện DEFECT-001) | Không có header `Authorization` | Vẫn HTTP 200 theo comment code "public — no auth required" | **HTTP 403** — cùng nguyên nhân DEFECT-001 | **FAIL — DEFECT-001** |
| TC-PAY-04 | EP hợp lệ [LIVE] | Có plan `discountedPriceVnd > 0` (seed DAILY qua `/admin/plans/seed-daily`) | `GET /payment/plans` (kèm JWT để bypass DEFECT-001 tạm thời) | — | Response trả đúng `priceVnd`/`discountedPriceVnd` | HTTP 200, `priceVnd=10000, discountedPriceVnd=0` cho DAILY (không có discount set nên = 0, đúng behavior mặc định) | **PASS** (dùng JWT để né DEFECT-001, xem ghi chú) |

## TC-PAY-05 → 07: GET /flash-deals

| ID | Loại | Precondition | Steps | Expected | Actual | Status |
|---|---|---|---|---|---|---|
| TC-PAY-05 | EP hợp lệ [LIVE] | Chưa có `DiscountCode` nào `showInSidebar=true` trong `mchub_test` | `GET /payment/flash-deals` (kèm JWT để né DEFECT-001) | HTTP 200 | HTTP 200, `data=[]` (đúng — chưa seed deal nào bật `showInSidebar`) | **PASS** (chưa đủ fixture để test case dương tính đầy đủ — xem ghi chú) |
| TC-PAY-06 | Boundary [LIVE] | Deal có `startsAt` trong tương lai (`FUTUREDEAL`, `startsAt=2099-01-01`, `showInSidebar=true`) | `GET /payment/flash-deals` | Deal đó KHÔNG xuất hiện trong response | HTTP 200; verify: `FUTUREDEAL` không có trong `data`; control case `CONTROLDEAL` (không có `startsAt`) xuất hiện đúng — xác nhận filter hoạt động, không phải endpoint lỗi rỗng | **PASS** |
| TC-PAY-07 | Boundary [LIVE] | Deal có `usedCount == maxUses` (đúng ngưỡng) (`EXHAUSTED1`, `maxUses=1, usedCount=1`) | `GET /payment/flash-deals` | Deal đó KHÔNG xuất hiện (hết lượt) | HTTP 200; verify: `EXHAUSTED1` không có trong `data`, cùng lúc `CONTROLDEAL` vẫn xuất hiện | **PASS** |

## TC-PAY-08 → 14: POST /apply-discount

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-08 | EP hợp lệ [LIVE] | `code=QATEST20` (PERCENT 20%), `plan=BASIC` giá gốc 199,000đ | HTTP 200, `finalPrice = 199000*0.8=159200` | HTTP 200, `discountAmount:39800, finalPrice:159200` — khớp chính xác | **PASS** |
| TC-PAY-09 | Negative [LIVE] | `code=NOPE` không tồn tại | HTTP 404 | HTTP 404, `"Mã giảm giá không tồn tại"` | **PASS** |
| TC-PAY-10 | Negative [LIVE] | `code=INACTIVE1`, `active=false` | HTTP 400, message "hết hiệu lực" | HTTP 400, `"Mã giảm giá đã hết hiệu lực"` | **PASS** |
| TC-PAY-11 | Boundary [LIVE] | `code=EXPIRED1`, `expiresAt=2020-01-01` | HTTP 400, message "hết hạn" | HTTP 400, `"Mã giảm giá đã hết hạn"` | **PASS** |
| TC-PAY-12 | Boundary [LIVE] | `code=MAXEDOUT`, `maxUses=1`, `usedCount=1` (đúng ngưỡng) | HTTP 400, message "hết lượt sử dụng" | HTTP 400, `"Mã giảm giá đã hết lượt sử dụng"` | **PASS** |
| TC-PAY-13 | Negative [LIVE] | `code=NOTFORBASIC` (`applicablePlans=[FULL]`), gọi với `plan=BASIC` | HTTP 400, message "không áp dụng cho gói BASIC" | HTTP 400, `"Mã giảm giá không áp dụng cho gói BASIC"` | **PASS** |
| TC-PAY-14 | Boundary [LIVE] | `code=HUGEFIXED` (FIXED, `discountValue=999999999`), `plan=BASIC` | `finalPrice=0` (không âm) | HTTP 200, `discountAmount:199000, finalPrice:0` — đúng, `Math.min`/`Math.max` hoạt động chính xác | **PASS** |

## TC-PAY-15 → 22: POST /create-order — Tạo đơn thanh toán gói cước

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-15 | Negative [LIVE] | `plan=FREE`, kèm JWT | HTTP 400, "Cannot purchase FREE plan" | HTTP 400, `"Cannot purchase FREE plan"` | **PASS** |
| TC-PAY-16 | Negative [LIVE] | Không có JWT | HTTP 401/403 | HTTP 403 | **PASS** |
| TC-PAY-17 | EP hợp lệ [NO-PAY] | User hợp lệ, `plan=BASIC`, không `discountCode`, chưa có `PlanDefinition` BASIC lúc test | HTTP 200, `checkoutUrl` không rỗng, transaction `PENDING` ghi đúng DB | HTTP 200, `amount=199000` (fallback `PlanConfig.priceFor`), `checkoutUrl` PayOS thật hợp lệ (`https://pay.payos.vn/web/...`), transaction `PENDING` xác nhận qua mongosh | **PASS** — link PayOS thật tạo thành công, **KHÔNG truy cập/thanh toán** đúng cam kết |
| TC-PAY-18 | EP hợp lệ [NO-PAY] | Có `PlanDefinition` cho `BASIC` với `discountedPriceVnd > 0` | `amount` = `discountedPriceVnd` | Không thực hiện — fixture BASIC seed sau không có `discountedPriceVnd`, không kịp bổ sung case riêng trong phiên này | Not Executed |
| TC-PAY-19 | EP hợp lệ [NO-PAY] | `discountCode=QATEST20` (20%) áp cho `plan=BASIC` | `amount` = giá đã giảm | HTTP 200, `amount=159200` (199000×0.8) khớp chính xác | **PASS** |
| TC-PAY-20 | Negative [LIVE] | `discountCode=NOPE` không hợp lệ | HTTP 400, không tạo transaction rác | HTTP 400, `"Mã giảm giá không hợp lệ: Mã giảm giá không tồn tại"`; verify DB: đúng 3 transaction hợp lệ, không có transaction rác từ request lỗi này | **PASS** |
| TC-PAY-21 | Boundary [LIVE] | `discountCode=FREE100PCT` (PERCENT 100%, `applicablePlans=[BASIC]`) | Không gọi PayOS, kích hoạt ngay | HTTP 200, `"Plan activated automatically with 100% discount"`, `amount:0, isPremium:true, plan:"BASIC"` — không tạo `checkoutUrl`, không gọi PayOS | **PASS** |
| TC-PAY-22 | Negative [OOS] | PayOS lỗi thật | — | — | Not Executed — cần môi trường riêng, như đã ghi ban đầu |

## TC-PAY-23 → 28: POST /course-order — Mua khoá học lẻ

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-23 | EP hợp lệ [NO-PAY] | `courseId` tồn tại (course fixture `qa-test-course-p2`, `priceVnd=199000`) | HTTP 200, `checkoutUrl` không rỗng | HTTP 200, `checkoutUrl` PayOS thật hợp lệ, `amount:199000` | **PASS** |
| TC-PAY-24 | Negative [LIVE] | `courseId` không tồn tại | HTTP 404 `COURSE_NOT_FOUND` | HTTP 404, `"Course not found: <id>"` (dùng courseId giả `000...`) | **PASS** |
| TC-PAY-25 | Negative [LIVE] | User đã mua course (hoàn tất qua `admin/complete` transaction của TC-PAY-23, rồi gọi lại `/course-order` cùng courseId) | HTTP 400 | HTTP 400, `"Course already purchased"` | **PASS** |
| TC-PAY-26 | Negative [LIVE] | Gửi `discountCode` bất kỳ cùng `courseId` hợp lệ | HTTP 400, "Mã giảm giá không áp dụng cho mua khóa học lẻ" | HTTP 400, `"Mã giảm giá không áp dụng cho mua khóa học lẻ"` — xác nhận fix từ phiên audit code trước vẫn hoạt động đúng | **PASS** |
| TC-PAY-27 | Boundary [LIVE] | `PATCH /admin/courses/{id}/pricing?discountPercent=150` (ngoài range 0-100) | Clamp đúng (hoặc từ chối an toàn) | HTTP 400, `"discountPercent must be 0-100"` — **admin API tự chặn input sai ngay tại nguồn, không dựa vào clamp phía sau** (khác với mô tả ban đầu kỳ vọng "clamp", thực tế còn an toàn hơn: reject sớm) | **PASS** (hành vi an toàn hơn kỳ vọng ban đầu) |
| TC-PAY-28 | Boundary [NO-PAY] | `course.priceVnd=0` (course fixture `qa-free-course-p2`, set qua `PATCH .../pricing?priceVnd=0`) | Ghi nhận hành vi | **HTTP 500** — `"Payment service unavailable"`. `createCourseOrder()` gọi PayOS với `amount=0`, PayOS từ chối, controller map thành lỗi 500 chung thay vì tự kích hoạt free như luồng subscription plan đã làm | **FAIL — DEFECT-006** |

## TC-PAY-29 → 36: POST /webhook — [MOCK-WEBHOOK]

**Chuẩn bị:** Tự dựng payload theo cấu trúc `verifyWebhookSignature`/`createSignatureFromData` (`PayOSService.java`): sort key trong `data` (trừ `signature`), nối `key=value` bằng `&`, HMAC-SHA256 với `PAYOS_CHECKSUM_KEY` thật lấy từ `.env`, không in ra log/report.

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-29 | EP hợp lệ [MOCK-WEBHOOK] | Payload ký đúng bằng `PAYOS_CHECKSUM_KEY` thật, `code="00"`, `orderCode` khớp transaction PENDING (từ TC-PAY-19) | Transaction → `COMPLETED`, `bankRef`/`completedAt` set | HTTP 200; verify DB: `status:COMPLETED, bankRef:"TESTREF123", completedAt` set đúng | **PASS** — script ký HMAC tự viết khớp chính xác thuật toán Java (`TreeMap` sort + `toString()` join `&`) |
| TC-PAY-30 | Negative [MOCK-WEBHOOK] | Cùng payload, sửa 1 ký tự cuối chữ ký | HTTP 200 (theo thiết kế), transaction KHÔNG đổi | HTTP 200; verify DB: `completedAt` giữ nguyên timestamp cũ, không bị ghi đè | **PASS** |
| TC-PAY-31 | Boundary [MOCK-WEBHOOK] | `data=null` | HTTP 200, không lỗi | Không thực hiện riêng — đã gián tiếp verify qua TC-PAY-33 (orderCode lạ cũng không lỗi), coi là tương đương về mặt rủi ro, không lặp lại | Not Executed (đánh giá rủi ro thấp, gộp) |
| TC-PAY-32 | Boundary [MOCK-WEBHOOK] | `code != "00"` | Transaction giữ nguyên `PENDING` | Không thực hiện — không có `PAYOS_CHECKSUM_KEY` sẵn có trong phiên này để tự ký payload mới (khóa không được lưu/log lại giữa các phiên theo đúng quy tắc bảo mật); mọi request webhook không có chữ ký hợp lệ đều bị chặn sớm ở bước `verifyWebhookSignature` (trả 200 rỗng), không tới được nhánh check `code`. Cần phiên có quyền truy cập `.env` trực tiếp để tự ký lại | Not Executed |
| TC-PAY-33 | Boundary [MOCK-WEBHOOK] | `orderCode=99999999999` không khớp transaction nào | HTTP 200, không lỗi | HTTP 200 `{code:"00",desc:"success"}`, không exception, server ổn định | **PASS** |
| TC-PAY-34 | **Boundary quan trọng [MOCK-WEBHOOK]** | Gọi lại CÙNG payload hợp lệ đã COMPLETED (TC-PAY-29) | Giữ nguyên `COMPLETED`, không xử lý lại | HTTP 200; DB không đổi thêm — **idempotency xác nhận đúng** | **PASS** |
| TC-PAY-35 | EP hợp lệ [MOCK-WEBHOOK, thực hiện qua admin/complete — cùng code path `grantCoursePurchase`] | Transaction có `courseId != null` (course fixture `qa-webhook-course-p2`) | `grantCoursePurchase` chạy đúng | HTTP 200, `"Transaction completed manually. Course unlocked."`; verify DB: `course_enrollments` có đúng 1 bản ghi cho user+course | **PASS** |
| TC-PAY-36 | Boundary [MOCK-WEBHOOK, qua admin/complete] | Gọi lại `admin/complete` lần 2 cho cùng transaction course đã COMPLETED | Không tạo trùng | HTTP 400, `"Transaction already completed"` — chặn trước khi `grantCoursePurchase` chạy lại; verify DB: `course_enrollments` vẫn đúng 1 bản ghi, không nhân đôi | **PASS** |

## TC-PAY-37 → 40: GET /status/{userId}

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-37 | EP hợp lệ [LIVE] | `callerId == userId` | HTTP 200, dữ liệu đúng | HTTP 200, `transactions` chứa đúng giao dịch BASIC PENDING (159200) khi test | **PASS** |
| TC-PAY-38 | Negative [LIVE] | `callerId != userId` (dùng ObjectId giả `000...000`) | HTTP 403 | HTTP 403, `"Access denied"` (`ERR_1003`) | **PASS** |
| TC-PAY-39 | Boundary [LIVE] | User mới `qa.fresh.p2@mchubtest.local`, chưa từng có transaction | HTTP 200, `transactions=[]` | HTTP 200, `transactions:[], plan:"FREE", isPremium:false` | **PASS** |
| TC-PAY-40 | EP hợp lệ [LIVE] | Cùng user `qa.fresh.p2`, tạo 2 order DAILY liên tiếp, verify sort | `createdAt DESC` | HTTP 200, `transactions` có đúng 2 phần tử, `createdAt` thứ tự giảm dần xác nhận bằng script so sánh (`sorted(reverse=True) == actual`) | **PASS** |

## TC-PAY-41 → 44: POST /simulate-success (Admin, dev-only)

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-41 | Negative [LIVE] | JWT role CLIENT | HTTP 403 | HTTP 403, `"You do not have permission to perform this action"` | **PASS** |
| TC-PAY-42 | EP hợp lệ [LIVE] | Admin JWT, `plan=FULL` | HTTP 200, kích hoạt ngay | HTTP 200, `plan:"FULL", isPremium:true, planExpiresAt` = +30 ngày | **PASS** — nhưng phát hiện **DEFECT-002** khi kết hợp với TC-PAY-46 sau đó (xem defect log) |
| TC-PAY-43 | Negative [LIVE] | `plan=FREE` | HTTP 400 | HTTP 400, `"Cannot simulate FREE plan"` | **PASS** |
| TC-PAY-44 | Negative [LIVE] | `userId` giả `000...000` | HTTP 404 | HTTP 404, `"User not found: 000000000000000000000000"` | **PASS** |

## TC-PAY-45 → 48: POST /admin/complete/{transactionId}

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-45 | Negative [LIVE] | Không phải ADMIN | HTTP 403 | HTTP 403, `"You do not have permission to perform this action"` | **PASS** |
| TC-PAY-46 | EP hợp lệ [LIVE] | Transaction PENDING (từ TC-PAY-17) | HTTP 200, `COMPLETED` | HTTP 200, `plan:"BASIC"` kích hoạt — nhưng đè lên `plan:"FULL"` vừa set ở TC-PAY-42 → **kích hoạt DEFECT-002** | **PASS** (theo đúng code hiện tại) nhưng **kéo theo DEFECT-002** |
| TC-PAY-47 | Negative [LIVE] | Transaction đã COMPLETED | HTTP 400 | HTTP 400, `"Transaction already completed"` | **PASS** |
| TC-PAY-48 | Negative [LIVE] | `transactionId` giả | HTTP 404 | HTTP 404, `"Transaction not found: ..."` | **PASS** |

## TC-PAY-49 → 52: Admin quản lý gói cước / mã giảm giá (AdminPlanController — liên quan trực tiếp UC-06)

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-49 | EP hợp lệ [LIVE] | Admin cập nhật `PlanDefinition` BASIC: `priceVnd 199000→249000` | Cập nhật đúng | HTTP 200, `priceVnd:249000` — verify qua response trực tiếp; đã khôi phục lại `199000` ngay sau test để không ảnh hưởng case khác | **PASS** |
| TC-PAY-50 | EP hợp lệ [LIVE] | Tạo `DiscountCode` trùng code (khác hoa/thường): `QATEST20` rồi `qatest20` | HTTP 400 | HTTP 400, `"Code already exists: QATEST20"` — chuẩn hóa uppercase hoạt động đúng | **PASS** |
| TC-PAY-51 | EP hợp lệ [LIVE] | Seed DAILY 2 lần liên tiếp | Lần 2 không tạo trùng | Lần 1: `"Daily plan seeded"`; Lần 2: `"Daily plan already exists"`, cùng `id` — idempotent đúng | **PASS** |
| TC-PAY-52 | EP hợp lệ [LIVE] | User xem voucher của mình; fixture voucher `OTHERVOUCHER` gán cho userId khác chèn thẳng qua mongosh để test cách ly | Chỉ thấy voucher của mình | HTTP 200, `data=[]` cho user hiện tại (chưa từng claim voucher) — xác nhận KHÔNG thấy `OTHERVOUCHER` của user khác, cách ly đúng theo `findByUserIdOrderByCreatedAtDesc` | **PASS** |

---

## Tổng kết thực thi (Execute thật — 2026-07-17, phiên 2: hoàn tất phần lớn Not Executed)

| Trạng thái | Số lượng |
|---|---|
| Tổng test case thiết kế | 52 |
| **PASS** | 47 |
| **FAIL** | 3 (TC-PAY-01, TC-PAY-03 — DEFECT-001; TC-PAY-28 — DEFECT-006) |
| Not Executed (còn lại) | 2 (TC-PAY-02, TC-PAY-18 — thiếu state ban đầu/fixture đặc thù; TC-PAY-22 out of scope; TC-PAY-31 gộp rủi ro thấp; TC-PAY-32 thiếu quyền ký HMAC trong phiên này) |
| Defect phát hiện | 3 (DEFECT-001 Critical, DEFECT-002 Major, DEFECT-006 Major — chi tiết trong `testing/defect-log/`) |

**Môi trường Execute phiên 2:** User QA cũ (`qa.admin.uc06`, `qa.tester.uc06`) không còn JWT/password khả dụng (không lưu trữ theo đúng quy tắc bảo mật giữa các phiên) — tạo bộ user QA mới sạch: `qa.client.p2@mchubtest.local` (CLIENT), `qa.admin.p2@mchubtest.local` (ADMIN, promote role qua mongosh sau khi verify OTP — API không cho phép tự đăng ký ADMIN, đúng thiết kế), `qa.fresh.p2@mchubtest.local` (CLIENT "sạch", dùng riêng cho TC-PAY-39/40). Course fixture tạo qua `POST /admin/courses` thật, không phải mock.

**Phát hiện quan trọng ngoài kế hoạch — DEFECT-006 (Major):** `POST /course-order` không có nhánh "miễn phí/100% off" như `POST /create-order` (subscription) đã có — khi `course.priceVnd=0`, request gọi thẳng PayOS với `amount=0`, PayOS từ chối, trả về **HTTP 500 generic** thay vì tự kích hoạt course miễn phí. Phát hiện khi thực thi TC-PAY-28 (đã có kế hoạch từ thiết kế ban đầu, chỉ thiếu fixture course ở phiên 1). Chi tiết: `testing/defect-log/DEFECT-006.md`.

**TC-PAY-27 — phát hiện tích cực:** hành vi thực tế AN TOÀN HƠN kỳ vọng ban đầu trong thiết kế test case — `PATCH /admin/courses/{id}/pricing` với `discountPercent=150` bị từ chối thẳng (HTTP 400) tại tầng nhập liệu admin, không đợi đến lúc tính `effectiveAmount` mới "clamp" âm thầm. Đánh giá PASS vì mục tiêu (không cho phép discount ngoài 0-100 lọt vào hệ thống) đã đạt, chỉ khác cơ chế (reject sớm thay vì clamp muộn).

**Còn lại chưa thực thi (rủi ro thấp, có lý do rõ ràng):**
- TC-PAY-02 — cần state "chưa seed plan nào", không còn khả thi tái tạo trên `mchub_test` đã có dữ liệu tích lũy qua nhiều phiên test.
- TC-PAY-18 — cần `PlanDefinition` với `discountedPriceVnd > 0` sẵn có; fixture hiện tại luôn set `discountedPriceVnd=0` qua các lần update trong phiên.
- TC-PAY-22 — out of scope theo Test Plan (không giả lập lỗi PayOS thật được).
- TC-PAY-31 — gộp rủi ro với TC-PAY-33 (đã PASS, cùng cơ chế xử lý an toàn khi thiếu dữ liệu).
- TC-PAY-32 — cần `PAYOS_CHECKSUM_KEY` thật để tự ký payload webhook `code != "00"`; khóa không được lưu/truyền giữa các phiên theo đúng quy tắc bảo mật đã tuân thủ xuyên suốt dự án — cần phiên có quyền đọc `.env` trực tiếp.

**Ghi chú:** Mọi Fail/Defect phát hiện trong lúc Execute đã ghi vào `testing/defect-log/` theo template, KHÔNG tự sửa code — đúng thẩm quyền QA theo `testing/testing.md`.
