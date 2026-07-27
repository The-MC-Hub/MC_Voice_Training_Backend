# UC-11 — Đặt Lịch & Thuê MC (MC Booking & Hiring)

## 📌 1. Mô tả Tổng Quan & Luồng Nghiệp Vụ

Luồng nghiệp vụ tìm kiếm MC, gửi yêu cầu đặt lịch cho sự kiện, thỏa thuận báo giá, thanh toán giữ chỗ qua PayOS, thực hiện sự kiện, đánh giá MC và Admin xử lý tranh chấp/hủy lịch.

### Actors
- **Client**: Khách hàng cá nhân/doanh nghiệp cần thuê MC.
- **MC**: MC chuyên nghiệp tiếp nhận yêu cầu và báo giá.
- **Admin**: Quản trị viên can thiệp xử lý hủy đơn hoặc hoàn tiền.

---

## 🛠️ 2. Chi Tiết Tính Năng & Điểm Nghiệp Vụ

| # | Tính năng | Mô tả Nghiệp Vụ Chi Tiết | Controller & API Endpoint |
|---|---|---|---|
| 1 | Tạo đơn đặt lịch MC | Client điền thông tin sự kiện (tên sự kiện, địa điểm, thời gian, mô tả) gửi tới MC | `POST /api/v1/bookings` |
| 2 | Phản hồi đơn đặt | MC chọn Chấp nhận (Accept) kèm báo giá chi tiết hoặc Từ chối (Reject) kèm lý do | `PUT /api/v1/bookings/{id}/respond` |
| 3 | Thanh toán đơn đặt | Client nạp tiền/thanh toán đơn qua cổng PayOS sau khi MC xác nhận báo giá | `POST /api/v1/bookings/{id}/pay` |
| 4 | Hủy đơn đặt lịch | Client hoặc MC hủy đơn trước giờ sự kiện diễn ra theo chính sách hoàn tiền | `PUT /api/v1/bookings/{id}/cancel` |
| 5 | Đánh giá & Chấm điểm MC | Client viết review nhận xét và chấm 1-5 sao cho MC sau khi sự kiện kết thúc | `POST /api/v1/mcs/{mcId}/reviews` |
| 6 | Thống kê Lịch đặt Admin | Admin xem bảng thống kê tổng số đơn đặt, tỷ lệ hoàn thành, doanh thu và lý do hủy | `GET /api/v1/admin/bookings/stats` |
| 7 | Bắt buộc hủy đơn Admin | Admin can thiệp hủy bắt buộc đơn đặt lịch (Force Cancel) trong trường hợp tranh chấp | `PUT /api/v1/admin/bookings/{id}/force-cancel` |

---

## 📐 3. Class Diagram

```mermaid
classDiagram
    class BookingController {
        +createBooking(req) ResponseEntity
        +respondBooking(id, status, price) ResponseEntity
        +payBooking(id) ResponseEntity
        +cancelBooking(id, reason) ResponseEntity
    }

    class AdminBookingController {
        +getBookingStats() ResponseEntity
        +forceCancelBooking(id, reason) ResponseEntity
    }

    class BookingService {
        <<interface>>
        +createBooking(clientUserId, req) BookingDTO
        +respondBooking(mcUserId, bookingId, status, quotePrice) BookingDTO
        +payBooking(clientUserId, bookingId) PaymentLinkDTO
        +cancelBooking(userId, bookingId, reason) BookingDTO
    }

    class Booking {
        +String id
        +String clientId
        +String mcId
        +String eventName
        +LocalDateTime eventDate
        +int quotePrice
        +BookingStatus status
        +LocalDateTime createdAt
    }

    BookingController --> BookingService
    AdminBookingController --> BookingService
    BookingService --> BookingRepository
    BookingRepository --> Booking
```

---

## 🔄 4. Sequence Diagram (Luồng Đặt Lịch, Báo Giá & Thanh Toán MC)

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Khách Hàng
    actor MC as MC Talent
    participant Controller as BookingController
    participant Service as BookingServiceImpl
    participant PayOS as Cổng Thanh Toán PayOS
    participant DB as MongoDB Atlas

    Client->>Controller: POST /api/v1/bookings (mcId, eventName, eventDate)
    Controller->>Service: createBooking(clientId, req)
    Service->>DB: Save Booking (status = PENDING_RESPONSE)
    DB-->>Service: Saved Booking Record
    Service-->>Controller: BookingDTO
    Controller-->>Client: 201 Created (Đã gửi yêu cầu đặt lịch tới MC)

    note over MC: MC Xem Thông Tin & Báo Giá
    MC->>Controller: PUT /api/v1/bookings/{id}/respond (status = ACCEPTED, quotePrice = 5,000,000 VNĐ)
    Controller->>Service: respondBooking(mcId, bookingId, ACCEPTED, 5000000)
    Service->>DB: Update Booking (status = ACCEPTED, quotePrice = 5000000)
    DB-->>Service: Updated Booking
    Service-->>Controller: BookingDTO
    Controller-->>MC: 200 OK (Đã chấp nhận yêu cầu & báo giá)

    note over Client: Client Thực Hiện Thanh Toán
    Client->>Controller: POST /api/v1/bookings/{id}/pay
    Controller->>Service: payBooking(clientId, bookingId)
    Service->>PayOS: Create Payment Link (amount = 5,000,000)
    PayOS-->>Service: checkoutUrl
    Service-->>Controller: PaymentLinkDTO
    Controller-->>Client: 200 OK (Trả về QR Code / Checkout URL PayOS)
```

---

## 🧪 5. Testing & Verification Report

- **Test Suite Classes:**
  - `com.mchub.controllers.BookingControllerTest`
  - `com.mchub.controllers.AdminBookingControllerTest`
  - `com.mchub.services.impl.BookingServiceImplTest`
- **Các kịch bản kiểm thử đã thực thi:**
  - `createBooking_validRequest_createsPendingBooking()`: Tạo đơn đặt ở trạng thái chờ MC phản hồi.
  - `respondBooking_accepted_updatesStatusAndQuotePrice()`: MC chấp nhận và cập nhật đúng báo giá.
  - `forceCancelBooking_adminRole_cancelsBookingAndRefunds()`: Admin bắt buộc hủy đơn và tạo lệnh hoàn tiền.
- **Kết quả kiểm thử:** Pass **100% (34/34 unit tests trong module MC Booking & Hiring)**.
