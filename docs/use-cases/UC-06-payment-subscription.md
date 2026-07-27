# UC-06 — Gói Cước & Thanh Toán (Payment & Subscriptions)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Thanh toán và Quản lý Gói cước VIP.

---

## 💳 UC-06.1: Xem Danh Sách Gói Cước VIP (Get VIP Subscription Plans)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest / User.
- **Mục tiêu:** Tra cứu bảng giá các gói VIP (`BASIC`, `FULL`, `ANNUAL`), số lượt AI sessions đi kèm và các chương trình khuyến mãi giảm giá active (`FlashDeals`).
- **Endpoint:** `GET /api/v1/payment/plans`

### 📐 2. Class Diagram (UC-06.1)
```mermaid
classDiagram
    class PaymentController {
        +getPlans() ResponseEntity~ApiResponse~
    }
    class SubscriptionPlanDTO {
        +SubscriptionPlan plan
        +int originalPrice
        +int discountedPrice
        +int aiSessionsPerMonth
        +List~String~ features
    }
    PaymentController --> SubscriptionPlanDTO
```

### 🔄 3. Sequence Diagram (UC-06.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as PaymentController
    participant PlanService as PlanServiceImpl
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/payment/plans
    Controller->>PlanService: getActivePlansWithDiscounts()
    PlanService->>DB: findActiveFlashDeals(now)
    DB-->>PlanService: FlashDeal Discounts
    PlanService->>PlanService: Calculate discounted prices
    PlanService-->>Controller: List<SubscriptionPlanDTO>
    Controller-->>User: 200 OK (Danh sách gói VIP & Giá sau giảm)
```

### 🧪 4. Testing & Verification (UC-06.1)
- **Unit Test Method:** `PlanServiceTest.java` -> `getPlans_returnsPlansWithActiveDiscounts()`
- **Assertions:** Trả về đủ 3 gói VIP, giá giảm được tính chính xác theo % FlashDeal.

---

## 💳 UC-06.2: Tạo Đơn Hàng & Link Thanh Toán PayOS (Create PayOS Order)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Tạo bản ghi `PaymentTransaction` ở trạng thái `PENDING`, gọi PayOS REST API sinh QR Code và trả về `checkoutUrl`.
- **Endpoint:** `POST /api/v1/payment/create-order`

### 📐 2. Class Diagram (UC-06.2)
```mermaid
classDiagram
    class PaymentController {
        +createOrder(CreateOrderRequestDTO req) ResponseEntity~ApiResponse~
    }
    class PayOSService {
        +createPaymentLink(PaymentTransaction tx) String
    }
    class PaymentTransaction {
        +String id
        +long orderCode
        +String userId
        +int amount
        +SubscriptionPlan plan
        +TransactionStatus status
    }
    PaymentController --> PayOSService
    PaymentController --> PaymentTransactionRepository
    PaymentTransactionRepository --> PaymentTransaction
```

### 🔄 3. Sequence Diagram (UC-06.2)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as PaymentController
    participant DB as MongoDB Atlas
    participant PayOS as PayOS SDK / API

    User->>Controller: POST /api/v1/payment/create-order (plan = "FULL", voucherCode)
    Controller->>Controller: Tính tổng tiền sau voucher (amount = 499,000 VNĐ)
    Controller->>Controller: Sinh ngẫu nhiên duy nhất long orderCode (ex: 89341029)
    
    Controller->>DB: save(PaymentTransaction: status = PENDING)
    DB-->>Controller: Saved PaymentTransaction Record
    
    Controller->>PayOS: createPaymentLink(orderCode, amount, description)
    PayOS-->>Controller: checkoutUrl ("https://pay.payos.vn/web/89341029")
    
    Controller-->>User: 200 OK (checkoutUrl, orderCode, amount)
```

### 🧪 4. Testing & Verification (UC-06.2)
- **Unit Test Method:** `PayOSServiceTest.java` -> `createPaymentLink_validTx_returnsUrl()`
- **Assertions:** `orderCode` > 0, `checkoutUrl` chứa tiền tố `https://pay.payos.vn`.

---

## 💳 UC-06.3: Xử Lý Webhook Thanh Toán PayOS (PayOS Webhook Handler)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** PayOS Gateway System.
- **Mục tiêu:** Tự động nhận kết quả thanh toán thành công, xác minh chữ ký bảo mật HMAC-SHA256, chuyển trạng thái đơn sang `PAID`, nâng cấp `User.plan` và cấp thêm lượt AI session cho người dùng.
- **Rules:** Nếu chữ ký `x-payos-signature` không khớp, từ chối với HTTP 400 Bad Request.
- **Endpoint:** `POST /api/v1/payment/webhook`

### 📐 2. Class Diagram (UC-06.3)
```mermaid
classDiagram
    class PaymentController {
        +handleWebhook(String payload, String signature) ResponseEntity~ApiResponse~
    }
    class PayOSService {
        +verifyWebhookSignature(String payload, String signature) boolean
    }
    PaymentController --> PayOSService
    PaymentController --> UserRepository
    PaymentController --> PaymentTransactionRepository
```

### 🔄 3. Sequence Diagram (UC-06.3)
```mermaid
sequenceDiagram
    autonumber
    actor PayOS as PayOS System
    participant Controller as PaymentController
    participant PayOSService as PayOSServiceImpl
    participant TxRepo as PaymentTransactionRepository
    participant UserRepo as UserRepository
    participant DB as MongoDB Atlas

    PayOS->>Controller: POST /api/v1/payment/webhook (Payload JSON, Header: x-payos-signature)
    Controller->>PayOSService: verifyWebhookSignature(payload, signature)
    
    alt Chữ ký sai hoặc bị giả mạo
        PayOSService-->>Controller: false
        Controller-->>PayOS: 400 Bad Request (Invalid Webhook Signature)
    else Chữ ký hợp lệ & Status Code == "00"
        Controller->>TxRepo: findByOrderCode(orderCode)
        TxRepo-->>Controller: PaymentTransaction Record
        
        Controller->>TxRepo: update(status = TransactionStatus.PAID)
        Controller->>UserRepo: findById(tx.userId)
        UserRepo-->>Controller: User Record
        
        Controller->>Controller: Cập nhật User.plan = tx.plan, gia hạn planExpiresAt += 30 ngày, cộng AI Sessions
        Controller->>UserRepo: save(User)
        UserRepo-->>Controller: Updated User
        
        Controller-->>PayOS: 200 OK (Xử lý webhook nâng cấp gói VIP thành công)
    end
```

### 🧪 4. Testing & Verification (UC-06.3)
- **Unit Test Method:** `PayOSServiceTest.java` -> `verifyWebhookSignature_valid_returnsTrue()`
- **Assertions:** Xác minh đúng chữ ký HMAC-SHA256, `User.plan` được nâng cấp chính xác.

---

## 💳 UC-06.4: Tra Cứu Trạng Thái Đơn Hàng (Order Status Check)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Polling trạng thái đơn hàng khi đang ở màn hình quét mã QR thanh toán để tự động chuyển hướng khi thành công.
- **Endpoint:** `GET /api/v1/payment/order/{orderCode}`

### 📐 2. Class Diagram (UC-06.4)
```mermaid
classDiagram
    class PaymentController {
        +getOrderStatus(long orderCode) ResponseEntity~ApiResponse~
    }
    PaymentController --> PaymentTransactionRepository
```

### 🔄 3. Sequence Diagram (UC-06.4)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client App
    participant Controller as PaymentController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/payment/order/89341029
    Controller->>DB: findByOrderCode(89341029)
    DB-->>Controller: PaymentTransaction (status = PAID)
    Controller-->>User: 200 OK (status = "PAID")
```

### 🧪 4. Testing & Verification (UC-06.4)
- **Unit Test Method:** `PaymentControllerTest.java` -> `getOrderStatus_returnsTransactionStatus()`
- **Assertions:** Trả về trạng thái `PAID` hoặc `PENDING`.

---

## 💳 UC-06.5: Áp Dụng Mã Giảm Giá Voucher (Apply Discount Voucher)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Kiểm tra mã giảm giá (Voucher), tính toán số tiền được giảm trước khi bấm thanh toán.
- **Endpoint:** `POST /api/v1/payment/apply-voucher`

### 📐 2. Class Diagram (UC-06.5)
```mermaid
classDiagram
    class PaymentController {
        +applyVoucher(ApplyVoucherRequestDTO req) ResponseEntity~ApiResponse~
    }
    class Voucher {
        +String code
        +int discountPercent
        +int maxDiscountAmount
        +LocalDateTime expiresAt
    }
    PaymentController --> VoucherRepository
    VoucherRepository --> Voucher
```

### 🔄 3. Sequence Diagram (UC-06.5)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client
    participant Controller as PaymentController
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/payment/apply-voucher (code = "MCHUB50", originalPrice = 500000)
    Controller->>DB: findByCode("MCHUB50")
    DB-->>Controller: Voucher Record (discountPercent = 20%)
    
    alt Voucher Hết Hạn Hoặc Không Tồn Tại
        Controller-->>User: 400 Bad Request (VOUCHER_INVALID)
    else Hợp Lệ
        Controller->>Controller: Tính finalPrice = 500,000 - 100,000 = 400,000 VNĐ
        Controller-->>User: 200 OK (finalPrice, discountAmount = 100,000 VNĐ)
    end
```

### 🧪 4. Testing & Verification (UC-06.5)
- **Unit Test Method:** `PlanServiceTest.java` -> `applyDiscount_validCode_calculatesDiscount()`
- **Assertions:** Số tiền giảm không vượt quá `maxDiscountAmount`.

---

## 💳 UC-06.6: Lịch Sử Thanh Toán Cá Nhân (Payment History)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Xem danh sách các hóa đơn/giao dịch nâng cấp gói cước cá nhân đã thực hiện.
- **Endpoint:** `GET /api/v1/payment/history`

### 📐 2. Class Diagram (UC-06.6)
```mermaid
classDiagram
    class PaymentController {
        +getPaymentHistory() ResponseEntity~ApiResponse~
    }
    PaymentController --> PaymentTransactionRepository
```

### 🔄 3. Sequence Diagram (UC-06.6)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as PaymentController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/payment/history
    Controller->>DB: findByUserIdOrderByCreatedAtDesc(currentUserId)
    DB-->>Controller: List<PaymentTransaction>
    Controller-->>User: 200 OK (Lịch sử các hóa đơn đã thanh toán)
```

### 🧪 4. Testing & Verification (UC-06.6)
- **Unit Test Method:** `PaymentControllerTest.java` -> `getHistory_returnsUserTransactions()`
- **Assertions:** Danh sách trả về chỉ chứa giao dịch của `currentUserId`.
