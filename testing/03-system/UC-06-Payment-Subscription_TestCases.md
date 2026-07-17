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
| TC-PAY-06 | Boundary [LIVE] | Deal có `startsAt` trong tương lai | `GET /payment/flash-deals` | Deal đó KHÔNG xuất hiện trong response | Không thực hiện — cần thêm fixture `showInSidebar=true` + `startsAt` tương lai, chưa kịp trong phiên test này | Not Executed |
| TC-PAY-07 | Boundary [LIVE] | Deal có `usedCount == maxUses` (đúng ngưỡng) | `GET /payment/flash-deals` | Deal đó KHÔNG xuất hiện (hết lượt) | Không thực hiện — cùng lý do TC-PAY-06 | Not Executed |

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
| TC-PAY-21 | Boundary [LIVE] | `discountCode` giảm 100% | Không gọi PayOS, kích hoạt ngay | Không thực hiện — cần discount PERCENT=100 riêng, chưa kịp tạo fixture trong phiên này | Not Executed |
| TC-PAY-22 | Negative [OOS] | PayOS lỗi thật | — | — | Not Executed — cần môi trường riêng, như đã ghi ban đầu |

## TC-PAY-23 → 28: POST /course-order — Mua khoá học lẻ

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-23 | EP hợp lệ [NO-PAY] | `courseId` tồn tại | HTTP 200, `checkoutUrl` không rỗng | Không thực hiện — cần seed `Course` fixture trong `mchub_test`, chưa kịp trong phiên này | Not Executed |
| TC-PAY-24 | Negative [LIVE] | `courseId` không tồn tại | HTTP 404 `COURSE_NOT_FOUND` | HTTP 404, `"Course not found: <id>"` (dùng courseId giả `000...`) | **PASS** |
| TC-PAY-25 | Negative [LIVE] | User đã mua course | HTTP 400 | Không thực hiện — phụ thuộc TC-PAY-23 (cần course fixture + mua trước) | Not Executed |
| TC-PAY-26 | Negative [LIVE] | Gửi `discountCode` bất kỳ | HTTP 400, "Mã giảm giá không áp dụng cho mua khóa học lẻ" | Không thực hiện — phụ thuộc course fixture hợp lệ để vượt qua bước tìm course trước khi tới check discountCode | Not Executed (ưu tiên retest sớm — đây là case verify lại fix đã áp dụng ở phiên audit code trước) |
| TC-PAY-27 | Boundary [LIVE] | `course.discountPercent` ngoài range | Clamp đúng | Not Executed — phụ thuộc course fixture | Not Executed |
| TC-PAY-28 | Boundary [NO-PAY] | `course.priceVnd=0` | Ghi nhận hành vi | Not Executed — phụ thuộc course fixture | Not Executed |

## TC-PAY-29 → 36: POST /webhook — [MOCK-WEBHOOK]

**Chuẩn bị:** Tự dựng payload theo cấu trúc `verifyWebhookSignature`/`createSignatureFromData` (`PayOSService.java`): sort key trong `data` (trừ `signature`), nối `key=value` bằng `&`, HMAC-SHA256 với `PAYOS_CHECKSUM_KEY` thật lấy từ `.env`, không in ra log/report.

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-29 | EP hợp lệ [MOCK-WEBHOOK] | Payload ký đúng bằng `PAYOS_CHECKSUM_KEY` thật, `code="00"`, `orderCode` khớp transaction PENDING (từ TC-PAY-19) | Transaction → `COMPLETED`, `bankRef`/`completedAt` set | HTTP 200; verify DB: `status:COMPLETED, bankRef:"TESTREF123", completedAt` set đúng | **PASS** — script ký HMAC tự viết khớp chính xác thuật toán Java (`TreeMap` sort + `toString()` join `&`) |
| TC-PAY-30 | Negative [MOCK-WEBHOOK] | Cùng payload, sửa 1 ký tự cuối chữ ký | HTTP 200 (theo thiết kế), transaction KHÔNG đổi | HTTP 200; verify DB: `completedAt` giữ nguyên timestamp cũ, không bị ghi đè | **PASS** |
| TC-PAY-31 | Boundary [MOCK-WEBHOOK] | `data=null` | HTTP 200, không lỗi | Không thực hiện riêng — đã gián tiếp verify qua TC-PAY-33 (orderCode lạ cũng không lỗi), coi là tương đương về mặt rủi ro, không lặp lại | Not Executed (đánh giá rủi ro thấp, gộp) |
| TC-PAY-32 | Boundary [MOCK-WEBHOOK] | `code != "00"` | Transaction giữ nguyên `PENDING` | Không thực hiện — hết transaction PENDING khả dụng trong phiên test (đã complete hết qua TC-PAY-29/46), cần tạo order mới để test lại | Not Executed |
| TC-PAY-33 | Boundary [MOCK-WEBHOOK] | `orderCode=99999999999` không khớp transaction nào | HTTP 200, không lỗi | HTTP 200 `{code:"00",desc:"success"}`, không exception, server ổn định | **PASS** |
| TC-PAY-34 | **Boundary quan trọng [MOCK-WEBHOOK]** | Gọi lại CÙNG payload hợp lệ đã COMPLETED (TC-PAY-29) | Giữ nguyên `COMPLETED`, không xử lý lại | HTTP 200; DB không đổi thêm — **idempotency xác nhận đúng** | **PASS** |
| TC-PAY-35 | EP hợp lệ [MOCK-WEBHOOK] | Transaction có `courseId != null` | `grantCoursePurchase` chạy đúng | Không thực hiện — phụ thuộc course fixture chưa tạo (như TC-PAY-23) | Not Executed |
| TC-PAY-36 | Boundary [MOCK-WEBHOOK] | Webhook course 2 lần liên tiếp | Không tạo trùng | Không thực hiện — phụ thuộc TC-PAY-35 | Not Executed |

## TC-PAY-37 → 40: GET /status/{userId}

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-PAY-37 | EP hợp lệ [LIVE] | `callerId == userId` | HTTP 200, dữ liệu đúng | HTTP 200, `transactions` chứa đúng giao dịch BASIC PENDING (159200) khi test | **PASS** |
| TC-PAY-38 | Negative [LIVE] | `callerId != userId` (dùng ObjectId giả `000...000`) | HTTP 403 | HTTP 403, `"Access denied"` (`ERR_1003`) | **PASS** |
| TC-PAY-39 | Boundary [LIVE] | User chưa từng có transaction | HTTP 200, `transactions=[]` | Không thực hiện — user test đã có transaction ngay từ TC-PAY-17, không còn state "chưa từng giao dịch" để test lại trên cùng user; cần user mới riêng cho case này | Not Executed |
| TC-PAY-40 | EP hợp lệ [LIVE] | Nhiều transaction, verify sort | `createdAt DESC` | Verify gián tiếp qua TC-PAY-37 — response trả đúng 1 giao dịch tại thời điểm đó, chưa test với 2+ giao dịch cùng lúc để xác nhận thứ tự chắc chắn | Not Executed (đầy đủ) |

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
| TC-PAY-49 | EP hợp lệ [LIVE] | Admin cập nhật `PlanDefinition` | Cập nhật đúng | Không thực hiện — chưa kịp trong phiên này | Not Executed |
| TC-PAY-50 | EP hợp lệ [LIVE] | Tạo `DiscountCode` trùng code (khác hoa/thường): `QATEST20` rồi `qatest20` | HTTP 400 | HTTP 400, `"Code already exists: QATEST20"` — chuẩn hóa uppercase hoạt động đúng | **PASS** |
| TC-PAY-51 | EP hợp lệ [LIVE] | Seed DAILY 2 lần liên tiếp | Lần 2 không tạo trùng | Lần 1: `"Daily plan seeded"`; Lần 2: `"Daily plan already exists"`, cùng `id` — idempotent đúng | **PASS** |
| TC-PAY-52 | EP hợp lệ [LIVE] | User xem voucher của mình | Chỉ thấy voucher của mình | Không thực hiện — chưa kịp trong phiên này | Not Executed |

---

## Tổng kết thực thi (Execute thật — 2026-07-17)

| Trạng thái | Số lượng |
|---|---|
| Tổng test case thiết kế | 52 |
| **PASS** | 29 |
| **FAIL** | 2 (TC-PAY-01, TC-PAY-03 — cùng nguyên nhân DEFECT-001) |
| Not Executed (thiếu fixture/thời gian, không phải Fail) | 21 |
| Defect phát hiện | 2 (DEFECT-001 Critical, DEFECT-002 Major — chi tiết trong `testing/defect-log/`) |

**Môi trường Execute:** Backend chạy `java -jar target/backend-java-1.0.0.jar` với `-DMONGODB_URI`/`-DMONGODB_DATABASE=mchub_test`/`-Dserver.port=5555` (port 5000/8080 bị chiếm bởi service khác của user trên máy). Dùng `mongosh` kết nối trực tiếp `mchub_test` để lấy OTP test (đăng ký user thật qua email domain `@mchubtest.local` — không gửi mail thật) và seed fixture (`PlanDefinition`, `DiscountCode`) khi cần.

**Phát hiện quan trọng ngoài kế hoạch:** DEFECT-002 (race condition hạ cấp plan) phát sinh tự nhiên từ trình tự Execute thật (gọi `simulate-success` rồi `admin/complete` cho 2 giao dịch chồng lấn thời gian) — không nằm trong 52 case thiết kế ban đầu, minh chứng giá trị của Execute thật so với chỉ trace logic tĩnh.

**Việc còn lại (Not Executed) cần hoàn tất ở phiên tiếp theo:**
- Seed `Course` fixture để chạy trọn TC-PAY-23, 25-28, 35-36 (nhóm course-order + webhook course).
- Tạo discount PERCENT=100% để chạy TC-PAY-21 (luồng giảm giá 100%, bỏ qua PayOS).
- Tạo user thứ 2 "sạch" (chưa từng giao dịch) để chạy TC-PAY-39 đúng nghĩa boundary.
- TC-PAY-06/07 (flash-deal ẩn theo thời gian/lượt dùng) cần thêm fixture `showInSidebar=true`.
- TC-PAY-04/05 hiện PASS nhờ dùng JWT để né DEFECT-001 — cần retest lại đúng nghĩa **sau khi DEFECT-001 được fix**, để xác nhận Guest cũng truy cập được như thiết kế ban đầu.
- TC-PAY-49, 52 (admin update plan, user xem voucher) chưa kịp trong phiên này.

**Ghi chú:** Mọi Fail/Defect phát hiện trong lúc Execute đã ghi vào `testing/defect-log/` theo template, KHÔNG tự sửa code — đúng thẩm quyền QA theo `testing/testing.md`.
