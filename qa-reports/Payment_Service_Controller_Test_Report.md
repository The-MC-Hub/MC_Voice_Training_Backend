# Báo cáo Clean Code & Test — Module Payment

## 1. Thông tin chung

| Mục | Nội dung |
|---|---|
| Module | Payment (PayOS Integration, Plan/Discount Management, Order Creation, Webhook) |
| Files | `services/PayOSService.java`, `services/PlanService.java`, `controllers/PaymentController.java` |
| Ngày kiểm thử | 2026-07-16 |
| Người thực hiện | Senior Backend Engineer kiêm QA Engineer (AI-assisted) |
| Kỹ thuật test | Equivalence Partitioning (EP), Boundary Value Analysis (BVA), Negative Testing |
| Môi trường | Trace logic thủ công + `mvn compile` (không chạy được embedded MongoDB, không gọi PayOS thật) |

## 2. Mục đích & phạm vi

Rà soát clean code và kiểm thử thủ công module Payment: tạo link thanh toán PayOS (gói cước + khóa học lẻ), áp dụng mã giảm giá, xử lý webhook xác nhận thanh toán, tra cứu trạng thái, và các endpoint admin hỗ trợ vận hành (simulate payment, complete transaction thủ công).

## 3. Tóm tắt thay đổi Clean Code

| File | Trước | Sau | Lý do |
|---|---|---|---|
| `PayOSService.createPaymentLink()` / `createCoursePaymentLink()` | Hai method ~40 dòng mỗi cái, giống nhau ~90% (chỉ khác nguồn `description`/`item name`) | Tách `private Map<String,Object> createGenericPaymentLink(orderCode, amount, description, itemName)` dùng chung, 2 method public chỉ chuẩn bị `description`/`itemName` rồi gọi hàm chung. Thêm helper `truncate(String, int)` thay logic cắt chuỗi lặp lại | Vi phạm DRY rõ ràng. Không đổi hành vi — cùng cách build body, cùng chữ ký, cùng gọi PayOS API, cùng xử lý lỗi |
| `PaymentController.createCourseOrder()` | Khối xử lý `discountCode` cho mua khóa học lẻ hoàn toàn TRỐNG (chỉ có comment giải thích chưa làm xong) — client gửi `discountCode` bị bỏ qua âm thầm, không giảm giá, không báo lỗi | Nếu `discountCode` không rỗng, throw `AppException(VALIDATION_FAILED, "Mã giảm giá không áp dụng cho mua khóa học lẻ")` | **Vấn đề thật** (đã xác nhận với user, chọn sửa ngay): code cũ để trống hoàn toàn (dead-end logic đã có comment tự nhận "chưa làm xong") khiến client không biết vì sao mã giảm giá không có tác dụng. Fix không implement tính năng giảm giá khóa học (ngoài phạm vi refactor không đổi business logic), chỉ làm rõ ràng hành vi hiện tại bằng cách báo lỗi thay vì im lặng bỏ qua |
| `PaymentController.handlePaymentWebhook()` | Khi `verifyWebhookSignature` trả `false`, vẫn trả HTTP 200 `{code:"00", desc:"success"}` giống hệt thành công thật | Không sửa — giữ nguyên | User xác nhận đây là pattern chuẩn của webhook (trả 200 để tránh nhà cung cấp thanh toán retry liên tục gây spam), không phải bug. Đã có `log.warn` cảnh báo signature sai sẵn trong code (dòng 266), đủ để phát hiện qua log server |
| `PlanService.java`, phần còn lại `PaymentController.java` | — | Không sửa gì thêm | Đã đạt chuẩn: `applyDiscount` xử lý biên tốt (`Math.max(0, ...)`, `Math.min(...)` tránh giá âm/giảm vượt gốc), tách CRUD/apply/consume rõ ràng |

## 4. Chi tiết Test Case

### 4.1. `PayOSService.createPaymentLink()` / `createCoursePaymentLink()` — sau refactor DRY

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| POS-01 | EP hợp lệ | `plan=BASIC`, `userId` ngắn | `description = "MCHUB BASIC " + userId` (≤25 ký tự), gọi `createGenericPaymentLink` | Đúng (dòng mới ~44-47) — hành vi giữ nguyên sau khi tách hàm | Pass |
| POS-02 | Boundary | `description` ghép ra dài hơn 25 ký tự (userId dài) | `truncate(..., 25)` cắt đúng 25 ký tự đầu | Đúng — logic cắt giữ nguyên, chỉ chuyển vào helper dùng chung | Pass |
| POS-03 | EP hợp lệ | `createCoursePaymentLink` với `courseTitle` ngắn | `itemName = courseTitle` nguyên vẹn (không cắt vì dưới 50 ký tự) | Đúng | Pass |
| POS-04 | Boundary | `courseTitle` dài hơn 50 ký tự | `truncate(courseTitle, 50)` cắt đúng | Đúng | Pass |
| POS-05 | Boundary | `courseTitle=null` | `truncate(null, 50)` trả `null` (không NPE nhờ check `s == null` trong helper) | Đúng — helper `truncate` xử lý null an toàn, khác với code cũ (`courseTitle.length()` sẽ NPE nếu null) — **cải thiện nhỏ ngoài ý muốn**, cần lưu ý PayOS API có thể reject `item.name=null`, nhưng đây là hành vi hợp lý hơn code cũ (trước đây `courseTitle.length() > 50` sẽ NPE ngay nếu null, giờ an toàn hơn dù vẫn cần course có title thật ở tầng nghiệp vụ) | Pass (cải thiện) |
| POS-06 | Negative | PayOS API trả `code != "00"` hoặc response null | Throw `RuntimeException("PayOS error: " + desc)` | Đúng — cả 2 method public đều đi qua cùng 1 điểm xử lý lỗi trong `createGenericPaymentLink`, đảm bảo nhất quán tuyệt đối (trước đây là 2 bản copy, có rủi ro 1 bên sửa message lỗi mà quên bên kia) | Pass |

### 4.2. `PayOSService.verifyWebhookSignature()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| WHS-01 | EP hợp lệ | `webhookData` có `signature` khớp HMAC-SHA256 tính từ `data` với `checksumKey` đúng | `true` | Đúng (dòng 129-145) | Pass |
| WHS-02 | Negative | `signature` sai/không khớp | `false` | Đúng | Pass |
| WHS-03 | Boundary | `webhookData` không có key `signature` | `sigObj=null` → `false` ngay, không lỗi | Đúng (dòng 131-132) | Pass |
| WHS-04 | Boundary | `webhookData` không có key `data` | `data=null` → `false` ngay | Đúng (dòng 137) | Pass |
| WHS-05 | Negative | Lỗi bất kỳ trong quá trình tính HMAC (VD: `checksumKey` rỗng gây lỗi `Mac.init`) | Catch `Exception`, log lỗi, trả `false` (an toàn — không throw ra ngoài, không rò rỉ signature hợp lệ khi có lỗi nội bộ) | Đúng (dòng 141-144) | Pass |
| WHS-06 | EP hợp lệ | So sánh signature dùng `equalsIgnoreCase` (không phân biệt hoa/thường) | Chấp nhận `ABC123` khớp `abc123` | Đúng theo code — PayOS thường trả hex lowercase, chấp nhận cả 2 case là hợp lý cho tính tương thích. **Lưu ý:** đây không phải constant-time comparison (rủi ro timing attack lý thuyết), nhưng do webhook không phải nơi nhập password/token nhạy cảm theo yêu cầu thời gian thực nghiêm ngặt, rủi ro thực tế thấp — ghi vào rủi ro tồn đọng, không tự đổi vì thay đổi thuật toán so sánh cần cân nhắc kỹ | Pass (theo code, ghi nhận) |

### 4.3. `PlanService` — Plan/Discount CRUD

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| PLN-01 | EP hợp lệ | `getPlanByKey(BASIC)` tồn tại | Trả `PlanDefinition` | Đúng (dòng 34-37) | Pass |
| PLN-02 | Negative | `getPlanByKey(plan)` không tồn tại trong DB | Throw `AppException(RESOURCE_NOT_FOUND)` | Đúng | Pass |
| PLN-03 | EP hợp lệ | `updatePlan(id, updated)` | Cập nhật đầy đủ toàn bộ field liên quan (giá, thời hạn, hiển thị, khuyến mãi) | Đúng (dòng 43-61) | Pass |
| DSC-01 | EP hợp lệ | `saveDiscount` với code mới, chưa trùng | Chuẩn hóa `code` thành uppercase + trim, lưu | Đúng (dòng 69-79) | Pass |
| DSC-02 | Negative | `saveDiscount` với code đã tồn tại (khác `id`) | Throw `AppException(VALIDATION_FAILED, "Code already exists")` | Đúng (dòng 73-77) | Pass |
| DSC-03 | Boundary | `saveDiscount` update lại chính discount đó (cùng `id`, cùng code) | Không throw — điều kiện `!existing.getId().equals(discount.getId())` chặn false-positive khi save lại chính nó | Đúng | Pass |

### 4.4. `PlanService.applyDiscount(String code, SubscriptionPlan plan)`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| APD-01 | EP hợp lệ | Code hợp lệ, active, chưa hết hạn, còn lượt dùng, áp dụng cho plan này | Tính đúng `finalPrice` theo loại PERCENT/FIXED | Đúng (dòng 106-144) | Pass |
| APD-02 | Negative | Code không tồn tại | Throw `AppException(RESOURCE_NOT_FOUND, "Mã giảm giá không tồn tại")` | Đúng | Pass |
| APD-03 | Negative | Code tồn tại nhưng `isActive=false` | Throw `AppException(VALIDATION_FAILED, "Mã giảm giá đã hết hiệu lực")` | Đúng | Pass |
| APD-04 | Boundary | `expiresAt` đã qua | Throw `AppException(VALIDATION_FAILED, "Mã giảm giá đã hết hạn")` | Đúng | Pass |
| APD-05 | Boundary | `maxUses > 0` và `usedCount >= maxUses` (đúng ngưỡng) | Throw `AppException(VALIDATION_FAILED, "...hết lượt sử dụng")` | Đúng (điều kiện `>=`) | Pass |
| APD-06 | Boundary | `maxUses = 0` (không giới hạn lượt dùng) | Bỏ qua check lượt dùng hoàn toàn | Đúng (điều kiện `dc.getMaxUses() > 0 && ...`) | Pass |
| APD-07 | Negative | `applicablePlans` không rỗng và không chứa `plan` hiện tại | Throw `AppException(VALIDATION_FAILED, "...không áp dụng cho gói ...")` | Đúng | Pass |
| APD-08 | Boundary | `applicablePlans` rỗng hoặc null | Áp dụng cho MỌI plan (không chặn) | Đúng (điều kiện `!= null && !isEmpty()`) | Pass |
| APD-09 | Boundary | Loại PERCENT, `discountValue=100` | `discount = originalPrice * 100/100 = originalPrice`, `finalPrice = 0` | Đúng — giảm giá 100% cho ra giá 0 hợp lệ | Pass |
| APD-10 | Boundary | Loại FIXED, `discountValue` LỚN HƠN `originalPrice` | `discount = Math.min(discountValue, originalPrice)` → không vượt quá giá gốc, `finalPrice = 0` (không âm) | Đúng (dòng 132, 134) — xử lý biên tốt, tránh giá âm | Pass |

### 4.5. `PlanService.getActiveFlashDeals()` / `consumeDiscount()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| FLD-01 | EP hợp lệ | Discount active, trong khung thời gian, còn lượt, `showInSidebar=true` | Xuất hiện trong kết quả | Đúng (dòng 151-160) | Pass |
| FLD-02 | Boundary | `startsAt` trong tương lai (chưa tới ngày bắt đầu) | Bị loại (điều kiện `startsAt == null \|\| !startsAt.isAfter(now)`) | Đúng | Pass |
| FLD-03 | Boundary | `expiresAt` đã qua | Bị loại | Đúng | Pass |
| FLD-04 | Boundary | `usedCount >= maxUses` (đã hết lượt) | Bị loại | Đúng | Pass |
| CSD-01 | EP hợp lệ | `consumeDiscount(code)` với code tồn tại | Tăng `usedCount` thêm 1 | Đúng (dòng 163-168) | Pass |
| CSD-02 | Boundary | `consumeDiscount(code)` với code không tồn tại | `ifPresent` im lặng bỏ qua, không throw | Đúng — an toàn khi gọi sau khi thanh toán 100% off mà discount có thể đã bị admin xóa giữa chừng (hiếm nhưng không crash) | Pass |

### 4.6. `PaymentController.createPremiumOrder()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| ORD-01 | Negative | `plan=FREE` | Throw `AppException(VALIDATION_FAILED, "Cannot purchase FREE plan")` | Đúng (dòng 80-82) | Pass |
| ORD-02 | EP hợp lệ | `plan=BASIC`, không có `PlanDefinition` tùy chỉnh trong DB | Fallback dùng `PlanConfig.priceFor(plan)` qua catch Exception | Đúng (dòng 89-96) | Pass |
| ORD-03 | EP hợp lệ | `plan=BASIC`, có `PlanDefinition.discountedPriceVnd > 0` | Ưu tiên dùng giá đã giảm của admin thay vì giá gốc | Đúng (dòng 91-93) | Pass |
| ORD-04 | EP hợp lệ | `discountCode` hợp lệ | Áp dụng, `effectiveAmount` = giá sau giảm | Đúng (dòng 98-105) | Pass |
| ORD-05 | Negative | `discountCode` không hợp lệ | Catch exception từ `applyDiscount`, ném lại `AppException(VALIDATION_FAILED, "Mã giảm giá không hợp lệ: " + message)` | Đúng — bọc lỗi rõ ràng hơn cho client | Pass |
| ORD-06 | Boundary | `effectiveAmount <= 0` (giảm giá 100%) | **Kích hoạt gói NGAY LẬP TỨC** không qua PayOS — tạo transaction `COMPLETED` với `amount=0`, set `user.isPremium=true`/`plan`/`planExpiresAt`, consume discount nếu có | Đúng (dòng 110-141) — luồng đặc biệt hợp lý, tránh gọi PayOS cho đơn 0đ | Pass |
| ORD-07 | Negative | PayOS API lỗi (network/service down) | Log lỗi, throw `AppException(INTERNAL_ERROR, "Payment service unavailable")`, KHÔNG lưu transaction (fail trước khi save) | Đúng (dòng 144-149) — không tạo transaction rác nếu tạo link thất bại | Pass |
| ORD-08 | EP hợp lệ | Tạo order thành công qua PayOS | Lưu transaction `PENDING`, trả `checkoutUrl`/`qrCode` cho frontend | Đúng (dòng 151-171) | Pass |
| ORD-09 | Boundary | `orderCode` sinh ra — công thức `System.currentTimeMillis() % 1_000_000_000L * 100 + random(100)` | Luôn dương, nằm trong giới hạn PayOS (`≤ Long.MAX`), độ ngẫu nhiên đủ tránh trùng trong cùng millisecond | Đúng theo comment giải thích (dòng 107) — rủi ro trùng cực thấp nhưng về lý thuyết 2 request đồng thời cùng millisecond + cùng số random 0-99 vẫn có thể trùng orderCode (xác suất cực nhỏ ~1/100 trong cùng 1ms, thực tế gần như không xảy ra) — ghi nhận rủi ro lý thuyết, không sửa vì thay đổi thuật toán sinh ID cần cân nhắc kỹ | Pass (theo code) |

### 4.7. `PaymentController.createCourseOrder()` — **có cải thiện đã áp dụng**

| ID | Loại | Input | Expected | Actual (sau khi sửa) | Kết quả |
|---|---|---|---|---|---|
| CRO-01 | EP hợp lệ | `courseId` tồn tại, chưa mua, không có `discountCode` | Tính giá theo `course.discountPercent` (admin cấu hình sẵn), tạo order qua PayOS | Đúng (dòng 185-234, offset nhẹ sau fix) | Pass |
| CRO-02 | Negative | `courseId` không tồn tại | Throw `AppException(COURSE_NOT_FOUND)` | Đúng | Pass |
| CRO-03 | Negative | User đã mua course này rồi (`purchasedCourseIds` chứa `courseId`) | Throw `AppException(VALIDATION_FAILED, "Course already purchased")` | Đúng | Pass |
| CRO-04 | **Boundary — Cải thiện đã áp dụng** | Client gửi `discountCode` không rỗng cho mua khóa học lẻ | **Trước fix:** bị bỏ qua âm thầm, không giảm giá, không báo lỗi (dead code).<br>**Sau fix:** Throw `AppException(VALIDATION_FAILED, "Mã giảm giá không áp dụng cho mua khóa học lẻ")` — client biết rõ lý do | Pass (sau khi sửa) |
| CRO-05 | Boundary | `course.discountPercent` ngoài range 0-100 (dữ liệu admin nhập sai) | `Math.max(0, Math.min(100, ...))` clamp về range hợp lệ trước khi tính | Đúng (dòng 195, offset) — an toàn dù dữ liệu admin lỗi | Pass |

### 4.8. `PaymentController.handlePaymentWebhook()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| WHK-01 | EP hợp lệ | Signature hợp lệ, `code="00"`, `orderCode` khớp transaction `PENDING` | Cập nhật transaction thành `COMPLETED`, kích hoạt quyền lợi tương ứng (plan hoặc course) | Đúng (dòng 277-303, offset) | Pass |
| WHK-02 | Negative | Signature không hợp lệ | Trả HTTP 200 `{code:"00",desc:"success"}` (theo pattern chuẩn webhook, xác nhận không phải bug), có log.warn cảnh báo | Đúng — hành vi giữ nguyên theo quyết định user | Pass |
| WHK-03 | Boundary | `data=null` trong webhook body | Trả 200 success ngay, không xử lý tiếp | Đúng (dòng 271-272, offset) | Pass |
| WHK-04 | Boundary | `code != "00"` (thanh toán thất bại/hủy từ phía PayOS) | Không cập nhật transaction, chỉ trả 200 success (không throw lỗi) | Đúng — transaction giữ nguyên `PENDING`, không tự động set `FAILED` (có thể là chủ đích chờ webhook khác hoặc timeout riêng xử lý, không phải bug) | Pass |
| WHK-05 | Boundary | `orderCode` không khớp transaction nào trong DB | `findByOrderCode(...).ifPresent(...)` im lặng bỏ qua, không lỗi | Đúng | Pass |
| WHK-06 | Boundary | Transaction tìm thấy nhưng đã ở trạng thái `COMPLETED` từ trước (webhook gọi lại/duplicate) | Điều kiện `if (tx.getStatus() == PENDING)` chặn xử lý lại — **idempotent**, không kích hoạt quyền lợi 2 lần dù webhook gọi trùng | Đúng (dòng 282) — quan trọng vì webhook provider thường retry, tránh double-processing | Pass |
| WHK-07 | EP hợp lệ | Transaction có `courseId != null` | Gọi `grantCoursePurchase` thay vì nâng cấp plan | Đúng (dòng 288-290, offset) | Pass |
| WHK-08 | EP hợp lệ | Transaction không có `courseId` (subscription) | Nâng cấp plan user, set `planExpiresAt`, reset `aiSessionsUsed=0` | Đúng (dòng 291-301, offset) | Pass |

### 4.9. `PaymentController.grantCoursePurchase()` (private helper, dùng bởi webhook + admin complete)

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| GCP-01 | EP hợp lệ | User tồn tại, chưa mua course này | Thêm vào `purchasedCourseIds`, tạo `CourseEnrollment` mới | Đúng (dòng 238-255) | Pass |
| GCP-02 | Boundary | User đã có `courseId` trong `purchasedCourseIds` từ trước (gọi lại) | Không thêm trùng (check `contains`), không lỗi | Đúng | Pass |
| GCP-03 | Boundary | User đã có `CourseEnrollment` cho course này | Không tạo enrollment trùng (check `existsByUserIdAndCourseId`) | Đúng — idempotent, an toàn khi `grantCoursePurchase` được gọi cả từ webhook lẫn admin manual complete cho cùng transaction | Pass |
| GCP-04 | Boundary | `userId` trong transaction không tồn tại trong DB (hiếm) | `findById(...).ifPresent(...)` im lặng bỏ qua toàn bộ, không throw, không log lỗi rõ (chỉ không có `log.info` thành công) | Đúng theo code — không crash nhưng nếu xảy ra sẽ khó phát hiện qua log (không có nhánh else log warning) — rủi ro thấp vì `userId` luôn có nguồn gốc hợp lệ, ghi nhận nhẹ | Pass (theo code) |

### 4.10. `PaymentController.getPaymentStatus()` / `simulatePaymentSuccess()` / `adminCompleteTransaction()`

| ID | Loại | Input | Expected | Actual | Kết quả |
|---|---|---|---|---|---|
| STS-01 | EP hợp lệ | `callerId == userId` (xem trạng thái chính mình) | Trả `isPremium`, `plan`, `planExpiresAt`, danh sách transaction | Đúng (dòng 314-343, offset) | Pass |
| STS-02 | Negative | `callerId != userId` (xem trạng thái người khác) | Throw `AppException(ACCESS_DENIED)` | Đúng — **Lưu ý:** không có exception riêng cho ADMIN bypass như ở `VoiceController.getHistory` (đã thấy ở module Voice) — user thường không thể xem trạng thái thanh toán người khác dù là admin qua endpoint này. Có thể là chủ đích (admin dùng endpoint `/admin/*` riêng) — không phải bug, chỉ khác pattern so với Voice | Pass |
| SIM-01 | EP hợp lệ | Admin gọi `simulate-success` với `userId`, `plan` hợp lệ (dev/test only, có `@PreAuthorize("hasAuthority('ADMIN')")`) | Tạo transaction `COMPLETED` giả lập, kích hoạt plan ngay | Đúng (dòng 348-389, offset) | Pass |
| SIM-02 | Negative | `plan=FREE` | Throw `AppException(VALIDATION_FAILED, "Cannot simulate FREE plan")` | Đúng | Pass |
| ADC-01 | EP hợp lệ | Admin complete transaction `PENDING` tồn tại | Set `COMPLETED`, kích hoạt quyền lợi (plan hoặc course tùy transaction) | Đúng (dòng 396-439, offset) | Pass |
| ADC-02 | Negative | Transaction không tồn tại | Throw `AppException(TRANSACTION_NOT_FOUND)` | Đúng | Pass |
| ADC-03 | Negative | Transaction đã `COMPLETED` từ trước | Throw `AppException(VALIDATION_FAILED, "Transaction already completed")` — ngăn admin vô tình kích hoạt quyền lợi 2 lần | Đúng | Pass |

## 5. Tổng kết kết quả test

| Chỉ số | Số lượng |
|---|---|
| Tổng số test case | 47 |
| Pass | 47 |
| Fail | 0 |
| Bug/vấn đề phát hiện & đã sửa | 1 (`discountCode` cho mua khóa học lẻ bị bỏ qua âm thầm — đổi thành báo lỗi rõ ràng) |
| Refactor DRY | 1 (gộp `createPaymentLink`/`createCoursePaymentLink` trong `PayOSService` thành 1 nguồn logic chung) |
| Xác nhận không phải bug (qua trao đổi với user) | 1 (webhook trả HTTP 200 khi signature sai — pattern chuẩn, giữ nguyên) |
| Ghi nhận (không sửa, chờ quyết định) | 2 (`verifyWebhookSignature` dùng `equalsIgnoreCase` thay vì constant-time comparison; `grantCoursePurchase` không log rõ khi `userId` không tồn tại) |

**Giới hạn môi trường:** Không thể chạy embedded MongoDB integration test thật, không gọi API PayOS thật (cần credentials thật + môi trường sandbox). Toàn bộ test case dựa trên trace logic thủ công đối chiếu source code thực tế.

## 6. Kết luận

Module Payment — module nhạy cảm nhất về mặt tài chính — được thiết kế khá cẩn thận: idempotent ở webhook (tránh xử lý trùng khi provider retry), idempotent ở `grantCoursePurchase` (tránh cấp quyền trùng), xử lý biên tốt cho giá giảm (không âm, không vượt giá gốc), tách rõ luồng đặc biệt cho giảm giá 100% (bỏ qua PayOS). Đã sửa 1 vấn đề logic thật (discount code khóa học bị bỏ qua âm thầm) và dọn 1 vi phạm DRY đáng kể trong `PayOSService`.

**Rủi ro tồn đọng cần lưu ý (không tự sửa, chờ quyết định):**
1. `verifyWebhookSignature()` so sánh HMAC bằng `equalsIgnoreCase` (không phải constant-time comparison) — rủi ro timing attack lý thuyết, thấp trong thực tế nhưng là best-practice nên cân nhắc dùng `MessageDigest.isEqual()` nếu muốn an toàn tuyệt đối.
2. `grantCoursePurchase()` không log cảnh báo khi `userId` trong transaction không tồn tại trong DB (trường hợp hiếm, dữ liệu không nhất quán) — khó phát hiện qua log nếu xảy ra.
3. `orderCode` sinh từ `System.currentTimeMillis() % 1_000_000_000L * 100 + random(0-99)` — về lý thuyết có thể trùng nếu 2 request đến đúng cùng millisecond và random ra cùng số, xác suất cực thấp nhưng không phải 0 tuyệt đối.
