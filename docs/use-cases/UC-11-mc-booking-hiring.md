# UC-11 — Đặt Lịch & Thuê MC (MC Booking & Hiring)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Đặt lịch thuê MC Talent.

---

## 📅 UC-11.1: Tạo Đơn Đặt Lịch MC (Create Booking Request)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Client (Khách hàng thuê MC).
- **Mục tiêu:** Tạo đơn yêu cầu thuê MC cho sự kiện (tên sự kiện, ngày giờ, địa điểm, ghi chú đặc biệt). Trạng thái đơn ban đầu = `PENDING_RESPONSE`.
- **Endpoint:** `POST /api/v1/bookings`

### 📐 2. Class Diagram (UC-11.1)
```mermaid
classDiagram
    class BookingController {
        +createBooking(CreateBookingRequestDTO req) ResponseEntity~ApiResponse~
    }
    class Booking {
        +String id
        +String clientId
        +String mcId
        +String eventName
        +LocalDateTime eventDate
        +BookingStatus status
    }
    BookingController --> BookingRepository
    BookingRepository --> Booking
```

### 🔄 3. Sequence Diagram (UC-11.1)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Khách Hàng
    participant Controller as BookingController
    participant Service as BookingServiceImpl
    participant DB as MongoDB Atlas

    Client->>Controller: POST /api/v1/bookings (mcId, eventName, eventDate)
    Controller->>Service: createBooking(clientId, req)
    Service->>DB: save(Booking: status = PENDING_RESPONSE)
    DB-->>Service: Saved Booking Record
    Service-->>Controller: BookingDTO
    Controller-->>Client: 201 Created (Đã gửi đơn yêu cầu thuê tới MC)
```

### 🧪 4. Testing & Verification (UC-11.1)
- **Unit Test Method:** `BookingServiceImplTest.java` -> `createBooking_validRequest_createsPendingBooking()`
- **Assertions:** Đơn hàng được tạo ở trạng thái `PENDING_RESPONSE`.

---

## 📅 UC-11.2: MC Phản Hồi & Báo Giá (MC Respond & Quote Price)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** MC Talent.
- **Mục tiêu:** MC xem yêu cầu đặt lịch, lựa chọn Chấp nhận (`ACCEPTED`) kèm báo giá dịch vụ (VNĐ) hoặc Từ chối (`REJECTED`) kèm lý do bận lịch.
- **Endpoint:** `PUT /api/v1/bookings/{id}/respond`

### 📐 2. Class Diagram (UC-11.2)
```mermaid
classDiagram
    class BookingController {
        +respondBooking(String id, RespondBookingDTO req) ResponseEntity~ApiResponse~
    }
    BookingController --> BookingService
```

### 🔄 3. Sequence Diagram (UC-11.2)
```mermaid
sequenceDiagram
    autonumber
    actor MC as MC Talent
    participant Controller as BookingController
    participant Service as BookingServiceImpl
    participant DB as MongoDB Atlas

    MC->>Controller: PUT /api/v1/bookings/{id}/respond (status = "ACCEPTED", quotePrice = 5000000)
    Controller->>Service: respondBooking(mcId, bookingId, ACCEPTED, 5000000)
    Service->>DB: findById(bookingId)
    DB-->>Service: Booking Record
    
    Service->>Service: Update status = ACCEPTED, quotePrice = 5,000,000 VNĐ
    Service->>DB: save(Booking)
    Service-->>Controller: BookingDTO
    Controller-->>MC: 200 OK (Đã báo giá 5,000,000 VNĐ tới Client)
```

### 🧪 4. Testing & Verification (UC-11.2)
- **Unit Test Method:** `BookingServiceImplTest.java` -> `respondBooking_accepted_updatesStatusAndQuotePrice()`
- **Assertions:** `quotePrice` được gán chính xác, status chuyển sang `ACCEPTED`.

---

## 📅 UC-11.3: Thanh Toán Đơn Đặt MC PayOS (Pay Booking Order)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Client.
- **Mục tiêu:** Client tiến hành thanh toán tiền dịch vụ sau khi MC đã chấp nhận báo giá. Sinh QR Code PayOS.
- **Endpoint:** `POST /api/v1/bookings/{id}/pay`

### 📐 2. Class Diagram (UC-11.3)
```mermaid
classDiagram
    class BookingController {
        +payBooking(String id) ResponseEntity~ApiResponse~
    }
    BookingController --> PayOSService
```

### 🔄 3. Sequence Diagram (UC-11.3)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Khách Hàng
    participant Controller as BookingController
    participant PayOS as PayOS Gateway API

    Client->>Controller: POST /api/v1/bookings/{id}/pay
    Controller->>PayOS: createPaymentLink(quotePrice, bookingDescription)
    PayOS-->>Controller: checkoutUrl
    Controller-->>Client: 200 OK (Trả về QR Code / Link thanh toán PayOS)
```

### 🧪 4. Testing & Verification (UC-11.3)
- **Unit Test Method:** `BookingControllerTest.java` -> `payBooking_returnsPaymentLink()`
- **Assertions:** Trả về `checkoutUrl` PayOS hợp lệ.

---

## 📅 UC-11.4: Hủy Đơn Đặt Lịch MC (Cancel Booking)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Client / MC.
- **Mục tiêu:** Hủy đơn đặt lịch trước giờ diễn ra sự kiện theo chính sách hủy.
- **Endpoint:** `PUT /api/v1/bookings/{id}/cancel`

### 📐 2. Class Diagram (UC-11.4)
```mermaid
classDiagram
    class BookingController {
        +cancelBooking(String id, CancelBookingRequestDTO req) ResponseEntity~ApiResponse~
    }
    BookingController --> BookingService
```

### 🔄 3. Sequence Diagram (UC-11.4)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as BookingController
    participant DB as MongoDB Atlas

    User->>Controller: PUT /api/v1/bookings/{id}/cancel (reason = "Thay đổi lịch sự kiện")
    Controller->>DB: findById(id)
    DB-->>Controller: Booking Record
    Controller->>DB: save(Booking: status = CANCELLED, cancelReason = reason)
    Controller-->>User: 200 OK (Đã hủy đơn đặt lịch)
```

### 🧪 4. Testing & Verification (UC-11.4)
- **Unit Test Method:** `BookingServiceImplTest.java` -> `cancelBooking_updatesStatus()`
- **Assertions:** Status chuyển thành `CANCELLED`.

---

## 📅 UC-11.5: Đánh Giá & Chấm Điểm MC (Review MC Talent)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Client.
- **Mục tiêu:** Viết nhận xét và chấm điểm sao (1-5) cho MC sau khi sự kiện hoàn thành.
- **Endpoint:** `POST /api/v1/mcs/{mcId}/reviews`

### 📐 2. Class Diagram (UC-11.5)
```mermaid
classDiagram
    class ReviewController {
        +createReview(String mcId, ReviewRequestDTO req) ResponseEntity~ApiResponse~
    }
    class Review {
        +String id
        +String clientId
        +String mcId
        +int rating
        +String comment
    }
    ReviewController --> ReviewRepository
    ReviewRepository --> Review
```

### 🔄 3. Sequence Diagram (UC-11.5)
```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Khách Hàng
    participant Controller as ReviewController
    participant DB as MongoDB Atlas

    Client->>Controller: POST /api/v1/mcs/{mcId}/reviews (rating = 5, comment = "MC dẫn chương trình rất chuyên nghiệp!")
    Controller->>DB: save(Review: rating = 5)
    Controller->>DB: Recalculate MC avgRating
    Controller-->>Client: 200 OK (Đánh giá MC thành công)
```

### 🧪 4. Testing & Verification (UC-11.5)
- **Unit Test Method:** `ReviewControllerTest.java` -> `createReview_success_updatesMcAvgRating()`
- **Assertions:** Rating trung bình của MC được cập nhật chính xác.

---

## 📅 UC-11.6: Thống Kê & Bắt Buộc Hủy Admin (Admin Booking Controls)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Tra cứu số liệu thống kê đơn đặt MC toàn hệ thống và bắt buộc hủy đơn (Force Cancel) khi có tranh chấp.
- **Endpoint:** `GET /api/v1/admin/bookings/stats`, `PUT /api/v1/admin/bookings/{id}/force-cancel`

### 📐 2. Class Diagram (UC-11.6)
```mermaid
classDiagram
    class AdminBookingController {
        +getBookingStats() ResponseEntity~ApiResponse~
        +forceCancelBooking(String id, ForceCancelRequestDTO req) ResponseEntity~ApiResponse~
    }
    AdminBookingController --> BookingService
```

### 🔄 3. Sequence Diagram (UC-11.6)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminBookingController
    participant Service as BookingServiceImpl
    participant DB as MongoDB Atlas

    Admin->>Controller: PUT /api/v1/admin/bookings/{id}/force-cancel (reason = "Tranh chấp điều khoản")
    Controller->>Service: forceCancelBooking(bookingId, reason)
    Service->>DB: save(Booking: status = FORCE_CANCELLED)
    Service-->>Controller: BookingDTO
    Controller-->>Admin: 200 OK (Bắt buộc hủy đơn thành công)
```

### 🧪 4. Testing & Verification (UC-11.6)
- **Unit Test Method:** `AdminBookingControllerTest.java` -> `forceCancelBooking_adminRole_cancelsBooking()`
- **Assertions:** Status đơn hàng thành `FORCE_CANCELLED`.
