# 📘 API Information — The MC Hub Backend

**Base URL:** `http://localhost:5000/api/v1`  
**Interface:** Swagger UI → `http://localhost:5000/swagger-ui/index.html`  
**Authentication:** Bearer Token (JWT) — Thêm vào header: `Authorization: Bearer <token>`  
**Response Format:** Tất cả API protected đều dùng cấu trúc `ApiResponse<T>`:
```json
{
  "status": "success" | "fail",
  "message": "...",
  "data": { ... }
}
```

---

## 🔓 1. Auth — Xác thực (`/auth`)

Không yêu cầu JWT (Public).

---

### POST `/auth/register`
**Mô tả:** Đăng ký tài khoản mới (Client hoặc MC).

**Request Body:**
```json
{
  "name": "Nguyễn Văn A",
  "email": "a@example.com",
  "password": "password123",
  "role": "CLIENT"
}
```
| Trường | Kiểu | Bắt buộc | Ghi chú |
|--------|------|----------|---------|
| name | String | ✅ | Tên hiển thị |
| email | String | ✅ | Phải duy nhất |
| password | String | ✅ | Tối thiểu 6 ký tự |
| role | Enum | ✅ | `CLIENT` hoặc `MC` |

**Response `201 Created`:**
```json
{
  "status": "success",
  "message": "Đăng ký thành công",
  "data": {
    "id": "...",
    "name": "Nguyễn Văn A",
    "email": "a@example.com",
    "role": "CLIENT",
    "isVerified": false
  }
}
```

---

### POST `/auth/login`
**Mô tả:** Đăng nhập và nhận JWT Token.

**Request Body:**
```json
{
  "email": "a@example.com",
  "password": "password123"
}
```

**Response `200 OK`:**
```json
{
  "status": "success",
  "message": "Đăng nhập thành công",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": "...",
      "name": "Nguyễn Văn A",
      "email": "a@example.com",
      "role": "CLIENT",
      "avatar": "default-avatar.png",
      "isVerified": false
    }
  }
}
```

**Response `401 Unauthorized`:** Email hoặc mật khẩu sai.

---

### GET `/auth/fix-passwords`
**Mô tả:** ⚠️ **Chỉ dành cho Dev** — Reset toàn bộ mật khẩu seeded về `password123`.

**Response `200 OK`:** Plain text xác nhận thành công.

---

## 🌐 2. Public — Trang Công khai (`/public`)

Không yêu cầu JWT.

---

### GET `/public/landing`
**Mô tả:** Lấy thống kê tổng quan để hiển thị trên trang Landing/Home.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "stats": {
      "totalMCs": 45,
      "totalClients": 200,
      "totalBookings": 350
    }
  }
}
```

---

### GET `/public/mcs`
**Mô tả:** Lấy danh sách tất cả MC (có thể filter).

**Query Params:**
| Tham số | Kiểu | Bắt buộc | Ghi chú |
|---------|------|----------|---------|
| category | String | ❌ | Filter theo loại sự kiện |
| minPrice | Double | ❌ | Giá tối thiểu |
| maxPrice | Double | ❌ | Giá tối đa |

**Response `200 OK`:**
```json
{
  "status": "success",
  "results": 10,
  "data": {
    "mcs": [
      {
        "id": "mc-profile-id",
        "userId": "user-id",
        "name": "Trần Minh MC",
        "email": "mc@example.com",
        "avatar": "https://...",
        "isVerified": true,
        "experience": 5,
        "styles": ["Professional", "Bilingual"],
        "biography": "...",
        "rates": { "min": 5000000, "max": 15000000, "currency": "VND" },
        "eventTypes": ["WEDDING", "CORPORATE"],
        "rating": 4.8,
        "reviewsCount": 24
      }
    ]
  }
}
```

---

### GET `/public/mcs/{id}`
**Mô tả:** Xem hồ sơ chi tiết của một MC theo User ID hoặc Profile ID.

**Path Params:** `id` — User ID của MC.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "profile": {
      "id": "...",
      "userId": "...",
      "name": "Trần Minh MC",
      "avatar": "https://...",
      "isVerified": true,
      "experience": 5,
      "styles": [...],
      "biography": "...",
      "rates": { "min": 5000000, "max": 15000000, "currency": "VND" },
      "eventTypes": ["WEDDING"],
      "rating": 4.8,
      "reviewsCount": 24
    }
  }
}
```

**Response `404 Not Found`:** Khi không tìm thấy MC.

---

## 🔐 3. Booking — Quản lý Đặt lịch (`/bookings`)

> **Yêu cầu JWT.**

---

### POST `/bookings`
**Mô tả:** Client tạo yêu cầu đặt lịch MC. Trạng thái ban đầu là `PENDING`.

**Request Body:**
```json
{
  "mcId": "mc-user-id",
  "eventName": "Gala Night 2025",
  "eventType": "CORPORATE",
  "eventDate": "2025-12-20T18:00:00",
  "startTime": "18:00",
  "endTime": "22:00",
  "location": "GEM Center, TP.HCM",
  "description": "Buổi gala thường niên của công ty...",
  "audienceSize": 500,
  "budget": 10000000,
  "specialRequests": "Song ngữ Anh - Việt"
}
```

**Response `201 Created`:** Trả về `BookingResponseDTO`.

---

### GET `/bookings/my`
**Mô tả:** Lấy danh sách booking của chính mình theo vai trò.

**Query Params:**
| Tham số | Kiểu | Bắt buộc | Ghi chú |
|---------|------|----------|---------|
| role | String | ❌ | `client` (mặc định) hoặc `mc` |

**Ví dụ:** `GET /bookings/my?role=mc`

**Response `200 OK`:** Mảng `BookingResponseDTO[]`.

---

### GET `/bookings/{id}`
**Mô tả:** Lấy chi tiết một booking theo ID.

**Path Params:** `id` — Booking ID.

**Response `200 OK`:** Một đối tượng `BookingResponseDTO`.

---

### PUT `/bookings/{id}/status`
**Mô tả:** MC cập nhật trạng thái booking (chấp nhận, từ chối,...).

**Path Params:** `id` — Booking ID.

**Request Body:**
```json
{
  "status": "ACCEPTED",
  "price": 12000000,
  "rejectionReason": ""
}
```
| Trường | Ghi chú |
|--------|---------|
| status | `ACCEPTED`, `REJECTED`, `COMPLETED`, `CANCELLED`, `PAID` |
| price | Giá MC đề xuất (optional, chỉ khi ACCEPTED) |
| rejectionReason | Lý do từ chối (optional, khi REJECTED) |

**Response `200 OK`:** Booking đã được cập nhật trạng thái.

---

### PUT `/bookings/{id}/cancel`
**Mô tả:** Client hủy booking đang ở trạng thái PENDING.

**Path Params:** `id` — Booking ID.

**Response `200 OK`:** Booking với status `CANCELLED`.

---

## 📋 4. Booking Detail — Chi tiết Sự kiện (`/bookings/{bookingId}/detail`)

> **Yêu cầu JWT.**

---

### GET `/bookings/{bookingId}/detail`
**Mô tả:** Lấy thông tin chi tiết kỹ thuật của một booking (run sheet, ghi chú MC,...).

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "id": "...",
    "bookingId": "...",
    "runSheet": "...",
    "dressCode": "...",
    "venueNotes": "...",
    "mcNotes": "...",
    "attachments": []
  }
}
```

---

### PUT `/bookings/{bookingId}/detail`
**Mô tả:** Tạo hoặc cập nhật toàn bộ chi tiết booking.

**Request Body:**
```json
{
  "runSheet": "18:00 - Khai mạc...",
  "dressCode": "Business Formal",
  "venueNotes": "Sân khấu rộng 10m x 6m",
  "attachments": []
}
```

**Response `200 OK`:** `BookingDetailResponseDTO` đã cập nhật.

---

### PUT `/bookings/{bookingId}/detail/mc-notes`
**Mô tả:** MC cập nhật ghi chú cá nhân cho sự kiện.

**Request Body:**
```json
{
  "mcNotes": "Khách VIP: Ông A - CEO, cần gọi đúng chức danh..."
}
```

**Response `200 OK`:** `BookingDetailResponseDTO` đã cập nhật ghi chú MC.

---

## 💬 5. Chat — Nhắn tin (`/chat`)

> **Yêu cầu JWT.**

---

### GET `/chat/conversations`
**Mô tả:** Lấy toàn bộ cuộc hội thoại của user đang đăng nhập.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "conversations": [
      {
        "id": "...",
        "participants": ["userId1", "userId2"],
        "bookingId": "...",
        "lastMessage": "messageId",
        "isActive": true,
        "updatedAt": "..."
      }
    ]
  }
}
```

---

### GET `/chat/conversations/{id}`
**Mô tả:** Lấy chi tiết một cuộc hội thoại theo ID.

**Path Params:** `id` — Conversation ID.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "conversation": { ... }
  }
}
```

---

### PATCH `/chat/conversations/{id}/read`
**Mô tả:** Đánh dấu cuộc hội thoại là đã đọc (cập nhật `updatedAt`).

**Path Params:** `id` — Conversation ID.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Marked as read" }
```

---

### GET `/chat/messages/{conversationId}`
**Mô tả:** Lấy lịch sử tin nhắn trong một cuộc hội thoại (phân trang).

**Path Params:** `conversationId`

**Query Params:**
| Tham số | Kiểu | Mặc định | Ghi chú |
|---------|------|----------|---------|
| page | int | 0 | Trang (bắt đầu từ 0) |
| size | int | 50 | Số tin nhắn mỗi trang |

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "messages": [
      {
        "id": "...",
        "conversationId": "...",
        "senderId": "...",
        "content": "Xin chào!",
        "type": "TEXT",
        "createdAt": "..."
      }
    ]
  }
}
```

---

### POST `/chat/messages/{conversationId}`
**Mô tả:** Gửi tin nhắn mới vào cuộc hội thoại. Tin nhắn sẽ được đẩy qua WebSocket realtime.

**Path Params:** `conversationId`

**Request Body:**
```json
{
  "content": "Xin chào, tôi muốn xác nhận lại lịch trình...",
  "type": "TEXT"
}
```
| Trường | Ghi chú |
|--------|---------|
| type | `TEXT`, `IMAGE`, `FILE` |

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "message": { ... }
  }
}
```

---

## 🎖️ 6. Certificate — Chứng chỉ MC (`/certificates`)

> **Yêu cầu JWT.** POST và DELETE yêu cầu role MC, PUT /verify yêu cầu role ADMIN.

---

### POST `/certificates`
**Mô tả:** MC thêm chứng chỉ vào hồ sơ.

**Request Body:**
```json
{
  "name": "Certified Event MC",
  "issuedBy": "Vietnam Event Association",
  "issuedDate": "2023-06-15",
  "expiryDate": "2026-06-15",
  "fileUrl": "https://..."
}
```

**Response `201 Created`:** `CertificateResponseDTO`.

---

### GET `/certificates/mc/{mcProfileId}`
**Mô tả:** Lấy danh sách chứng chỉ của một MC (công khai).

**Path Params:** `mcProfileId` — ID của MCProfile.

**Response `200 OK`:** Mảng `CertificateResponseDTO[]`.

---

### PUT `/certificates/{id}/verify`
**Mô tả:** Admin xác minh chứng chỉ là hợp lệ.

**Path Params:** `id` — Certificate ID.

**Yêu cầu:** `ADMIN` role.

**Response `200 OK`:** Chứng chỉ đã được cập nhật `isVerified = true`.

---

### DELETE `/certificates/{id}`
**Mô tả:** MC xóa chứng chỉ của mình.

**Path Params:** `id` — Certificate ID.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Xóa thành công" }
```

---

## 🎟️ 7. Coupon — Mã giảm giá (`/coupons`)

> **Yêu cầu JWT.** POST, /admin, /toggle yêu cầu role ADMIN.

---

### POST `/coupons`
**Mô tả:** Admin tạo mã giảm giá mới.

**Yêu cầu:** `ADMIN` role.

**Request Body:**
```json
{
  "code": "MCHUB2025",
  "discountType": "PERCENTAGE",
  "discountValue": 15,
  "minOrderValue": 5000000,
  "maxUsage": 100,
  "validFrom": "2025-01-01",
  "validTo": "2025-12-31"
}
```

**Response `201 Created`:** `CouponResponseDTO`.

---

### GET `/coupons`
**Mô tả:** Lấy danh sách coupon đang còn hiệu lực (public cho user dùng).

**Response `200 OK`:** Mảng `CouponResponseDTO[]`.

---

### GET `/coupons/admin`
**Mô tả:** Admin lấy toàn bộ coupon (kể cả hết hạn).

**Yêu cầu:** `ADMIN` role.

**Response `200 OK`:** Mảng `CouponResponseDTO[]` đầy đủ.

---

### POST `/coupons/validate`
**Mô tả:** Kiểm tra tính hợp lệ của mã coupon và tính số tiền giảm.

**Request Body:**
```json
{
  "code": "MCHUB2025",
  "bookingAmount": 10000000
}
```

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "code": "MCHUB2025",
    "discountAmount": 1500000,
    "finalAmount": 8500000
  }
}
```

---

### PUT `/coupons/{id}/toggle`
**Mô tả:** Admin bật/tắt trạng thái hoạt động của coupon.

**Yêu cầu:** `ADMIN` role.

**Path Params:** `id` — Coupon ID.

**Response `200 OK`:** `CouponResponseDTO` với trạng thái mới.

---

## ⭐ 8. Favorite — MC Yêu thích (`/favorites`)

> **Yêu cầu JWT** (Client).

---

### POST `/favorites/{mcUserId}`
**Mô tả:** Toggle yêu thích/bỏ yêu thích một MC.

**Path Params:** `mcUserId` — User ID của MC.

**Response `200 OK`:**
```json
{
  "status": "success",
  "message": "Đã thêm vào danh sách yêu thích",
  "data": {
    "isFavorited": true,
    "mcUserId": "..."
  }
}
```

---

### GET `/favorites/my`
**Mô tả:** Lấy danh sách MC yêu thích của client đang đăng nhập.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "favorites": [...],
    "total": 5
  }
}
```

---

### GET `/favorites/check/{mcUserId}`
**Mô tả:** Kiểm tra xem client đã yêu thích MC này chưa.

**Path Params:** `mcUserId` — User ID của MC.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "isFavorited": true
  }
}
```

---

## 🎤 9. MC — Hồ sơ MC (`/mcs`)

> **Yêu cầu JWT** (role MC).

---

### GET `/mcs/dashboard`
**Mô tả:** Lấy thống kê dashboard cá nhân của MC (tổng booking, doanh thu,...).

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "totalBookings": 50,
    "totalRevenue": 250000000,
    "pendingBookings": 3,
    "rating": 4.9
  }
}
```

---

### PUT `/mcs/profile`
**Mô tả:** MC cập nhật hồ sơ của mình (styles, biography, rates,...).

**Request Body (các trường cần cập nhật):**
```json
{
  "biography": "Tôi là MC chuyên nghiệp...",
  "styles": ["Professional", "Bilingual"],
  "experience": 7,
  "rates": {
    "min": 6000000,
    "max": 20000000,
    "currency": "VND"
  },
  "eventTypes": ["WEDDING", "CORPORATE", "GALA"]
}
```

**Response `200 OK`:** `MCProfile` đã cập nhật.

---

## 🔔 10. Notification — Thông báo (`/notifications`)

> **Yêu cầu JWT.**

---

### GET `/notifications`
**Mô tả:** Lấy danh sách thông báo của user, bao gồm số thông báo chưa đọc.

**Query Params:**
| Tham số | Kiểu | Mặc định | Ghi chú |
|---------|------|----------|---------|
| page | int | 0 | Trang (bắt đầu từ 0) |
| limit | int | 20 | Số thông báo mỗi trang |

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "notifications": [
      {
        "id": "...",
        "title": "Booking mới!",
        "message": "Bạn có booking mới từ client A",
        "type": "BOOKING",
        "isRead": false,
        "createdAt": "..."
      }
    ],
    "total": 15,
    "unreadCount": 3
  }
}
```

---

### GET `/notifications/unread-count`
**Mô tả:** Lấy số lượng thông báo chưa đọc (dùng cho badge trên icon chuông).

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": { "count": 3 }
}
```

---

### PATCH `/notifications/{id}/read`
**Mô tả:** Đánh dấu một thông báo đã đọc.

**Path Params:** `id` — Notification ID.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Đã đánh dấu là đã đọc" }
```

---

### POST `/notifications/mark-all-read`
**Mô tả:** Đánh dấu tất cả thông báo của user là đã đọc.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Đã đánh dấu tất cả là đã đọc" }
```

---

### DELETE `/notifications/delete-all`
**Mô tả:** Xóa toàn bộ thông báo của user.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Đã xóa toàn bộ thông báo" }
```

---

## 💳 11. Payment — Thanh toán (`/payments`)

> **Yêu cầu JWT** (các API thanh toán). Webhook là public.

---

### POST `/payments/{bookingId}/checkout`
**Mô tả:** Tạo link thanh toán PayOS cho một booking đã được MC chấp nhận.

**Path Params:** `bookingId` — Booking ID.

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "checkoutUrl": "https://pay.payos.vn/...",
    "paymentLinkId": "...",
    "orderCode": 123456
  }
}
```

---

### POST `/payments/webhook`
**Mô tả:** Webhook nhận callback từ PayOS khi thanh toán thành công. **Không cần Auth**.

**Request Body:** Payload JSON từ PayOS.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Webhook received successfully" }
```

---

## 📝 12. Report — Báo cáo vi phạm (`/reports`)

> **Yêu cầu JWT.** `/reports/admin` và `/resolve` yêu cầu ADMIN.

---

### POST `/reports`
**Mô tả:** User báo cáo một MC hoặc nội dung vi phạm.

**Request Body:**
```json
{
  "targetId": "mc-user-id",
  "targetType": "MC",
  "reason": "Hành vi không chuyên nghiệp",
  "description": "MC đã không đến theo lịch hẹn..."
}
```

**Response `201 Created`:** `ReportResponseDTO`.

---

### GET `/reports/my`
**Mô tả:** Lấy danh sách báo cáo do user đang đăng nhập tạo ra.

**Response `200 OK`:** Mảng `ReportResponseDTO[]`.

---

### GET `/reports/admin`
**Mô tả:** Admin xem toàn bộ báo cáo, có thể filter theo trạng thái.

**Yêu cầu:** `ADMIN` role.

**Query Params:**
| Tham số | Kiểu | Ghi chú |
|---------|------|---------|
| status | String | `pending` để lọc chưa xử lý |

**Response `200 OK`:** Mảng `ReportResponseDTO[]`.

---

### PUT `/reports/{id}/resolve`
**Mô tả:** Admin xử lý và đóng một báo cáo.

**Yêu cầu:** `ADMIN` role.

**Path Params:** `id` — Report ID.

**Request Body:**
```json
{
  "status": "RESOLVED",
  "adminNote": "Đã cảnh báo MC liên quan"
}
```
| `status` | Ghi chú |
|----------|---------|
| `RESOLVED` | Đã giải quyết |
| `DISMISSED` | Bác bỏ báo cáo |

**Response `200 OK`:** `ReportResponseDTO` đã cập nhật.

---

## ⭐ 13. Review — Đánh giá MC (`/reviews`)

> **Yêu cầu JWT** (POST, PATCH, DELETE). GET là public.

---

### POST `/reviews`
**Mô tả:** Client đánh giá MC sau khi sự kiện hoàn thành.

**Request Body:**
```json
{
  "mcId": "mc-user-id",
  "bookingId": "booking-id",
  "rating": 5,
  "comment": "MC rất chuyên nghiệp, dẫn chương trình tuyệt vời!"
}
```

**Response `201 Created`:** `ReviewResponseDTO`.

---

### GET `/reviews/mc/{mcId}`
**Mô tả:** Lấy danh sách đánh giá của một MC (public).

**Path Params:** `mcId` — User ID của MC.

**Response `200 OK`:** Mảng `ReviewResponseDTO[]`.

---

## 📜 14. Script — Kịch bản (`/scripts`)

> **Yêu cầu JWT.** GET list và detail là public (nếu cấu hình publicList trong config).

---

### GET `/scripts`
**Mô tả:** Lấy danh sách kịch bản, có thể filter theo category.

**Query Params:**
| Tham số | Ghi chú |
|---------|---------|
| category | Filter theo loại kịch bản (Wedding, Corporate...) |

**Response `200 OK`:** Mảng `ScriptResponseDTO[]`.

---

### GET `/scripts/{id}`
**Mô tả:** Xem chi tiết một kịch bản.

**Path Params:** `id` — Script ID.

**Response `200 OK`:** `ScriptResponseDTO`.

---

### POST `/scripts/{id}/favorite`
**Mô tả:** Tăng lượt yêu thích cho kịch bản.

**Path Params:** `id` — Script ID.

**Response `200 OK`:** `ScriptResponseDTO` với `favoritesCount` đã tăng.

---

## 🔑 15. Availability — Lịch bận của MC (`/availability`)

> **Yêu cầu JWT** (POST, DELETE yêu cầu role MC). GET là public.

---

### POST `/availability`
**Mô tả:** MC thêm lịch bận/lịch đã đặt vào lịch cá nhân.

**Request Body:**
```json
{
  "date": "2025-12-20",
  "startTime": "09:00",
  "endTime": "22:00",
  "title": "Wedding tại Bến Thành",
  "status": "Booked"
}
```

**Response `201 Created`:** `Schedule` đã tạo.

---

### GET `/availability/{mcId}`
**Mô tả:** Lấy toàn bộ lịch của một MC để hiển thị trên trang client.

**Path Params:** `mcId` — User ID của MC.

**Response `200 OK`:** Mảng `Schedule[]`.

---

### DELETE `/availability/{id}`
**Mô tả:** MC xóa một mục lịch bận.

**Path Params:** `id` — Schedule ID.

**Response `200 OK`:**
```json
{ "status": "success", "message": "Xóa thành công" }
```

---

## 🛡️ 16. Admin — Quản trị viên (`/admin`)

> **Yêu cầu JWT + ADMIN role.** Tất cả endpoint yêu cầu quyền ADMIN.

---

### GET `/admin/dashboard`
**Mô tả:** Lấy tổng quan số liệu quản trị (user, booking, doanh thu,...).

**Response `200 OK`:**
```json
{
  "status": "success",
  "data": {
    "totalUsers": 500,
    "totalMCs": 80,
    "totalClients": 420,
    "totalBookings": 1200,
    "totalRevenue": 5000000000
  }
}
```

---

### GET `/admin/users`
**Mô tả:** Lấy toàn bộ danh sách user (mọi role).

**Response `200 OK`:** Mảng `UserResponseDTO[]`.

---

### GET `/admin/users/mcs`
**Mô tả:** Lấy danh sách toàn bộ tài khoản MC.

**Response `200 OK`:** Mảng `UserResponseDTO[]`.

---

### GET `/admin/users/clients`
**Mô tả:** Lấy danh sách toàn bộ tài khoản Client.

**Response `200 OK`:** Mảng `UserResponseDTO[]`.

---

### PUT `/admin/users/{id}/status`
**Mô tả:** Admin kích hoạt/vô hiệu hóa hoặc xác minh tài khoản user.

**Path Params:** `id` — User ID.

**Request Body:**
```json
{
  "isActive": true,
  "isVerified": true
}
```

**Response `200 OK`:** `UserResponseDTO` với trạng thái mới.

---

### GET `/admin/bookings`
**Mô tả:** Lấy toàn bộ danh sách booking trong hệ thống.

**Response `200 OK`:** Mảng `BookingResponseDTO[]`.

---

## 📊 17. Audit Log — Nhật ký hệ thống (`/audit-logs`)

> **Yêu cầu JWT + ADMIN role.**

---

### GET `/audit-logs`
**Mô tả:** Lấy toàn bộ nhật ký hành động trong hệ thống.

**Response `200 OK`:** Mảng `AuditLogResponseDTO[]`.

---

### GET `/audit-logs/user/{userId}`
**Mô tả:** Lấy nhật ký hành động của một user cụ thể.

**Path Params:** `userId` — User ID cần tra cứu.

**Response `200 OK`:** Mảng `AuditLogResponseDTO[]`.

---

## 📌 Tổng hợp Endpoint

| Controller | Endpoint Prefix | Auth | Số API |
|-----------|----------------|------|--------|
| Auth | `/auth` | Public | 3 |
| Public | `/public` | Public | 3 |
| Booking | `/bookings` | JWT | 5 |
| BookingDetail | `/bookings/{id}/detail` | JWT | 3 |
| Chat | `/chat` | JWT | 5 |
| Certificate | `/certificates` | JWT | 4 |
| Coupon | `/coupons` | JWT/ADMIN | 5 |
| Favorite | `/favorites` | JWT | 3 |
| MC | `/mcs` | JWT (MC) | 2 |
| Notification | `/notifications` | JWT | 5 |
| Payment | `/payments` | JWT/Public | 2 |
| Report | `/reports` | JWT/ADMIN | 4 |
| Review | `/reviews` | JWT/Public | 2 |
| Script | `/scripts` | JWT/Public | 3 |
| Availability | `/availability` | JWT/Public | 3 |
| Admin | `/admin` | ADMIN | 6 |
| AuditLog | `/audit-logs` | ADMIN | 2 |
| **Tổng** | | | **60 API** |

---

## 🔑 Enum Reference

### BookingStatus
`PENDING` → `ACCEPTED` → `COMPLETED`  
`PENDING` → `REJECTED`  
`PENDING` → `CANCELLED`  
`ACCEPTED` → `PAID`

### EventType
`WEDDING`, `CORPORATE`, `GALA`, `PRIVATE_PARTY`, `AWARD_CEREMONY`, `PRODUCT_LAUNCH`, `CONFERENCE`, `CONCERT`

### UserRole
`CLIENT`, `MC`, `ADMIN`

### ReportStatus
`PENDING`, `RESOLVED`, `DISMISSED`

### MessageType
`TEXT`, `IMAGE`, `FILE`

### PaymentStatus
`UNPAID`, `PAID`, `REFUNDED`

---

## 🗂️ Frontend Service Files Mapping

| Service File | API Group | Số hàm | Ghi chú |
|-------------|-----------|--------|---------|
| `authService.js` | `/auth` | 5 | login, register, fixPasswords, updateSettings, submitKYC |
| `publicService.js` | `/public` | 4 | getLandingData, discoverMCs, getMCProfile, getResources |
| `bookingService.js` | `/bookings` | 8 | createBooking, getMyBookings, getClientBookings, getMCBookings, getBookingById, updateBookingStatus, acceptBooking, rejectBooking, completeBooking, cancelBooking |
| `bookingDetailService.js` | `/bookings/{id}/detail` | 3 | getBookingDetail, saveBookingDetail, updateMcNotes |
| `conversationService.js` | `/chat` | 5 | getConversations, getConversationById, markConversationAsRead, getMessages, sendMessage |
| `certificateService.js` | `/certificates` | 4 | addCertificate, getCertificatesByMC, verifyCertificate, deleteCertificate |
| `couponService.js` | `/coupons` | 5 | createCoupon, getActiveCoupons, getAllCouponsAdmin, validateCoupon, toggleCoupon |
| `favoriteService.js` | `/favorites` | 3 | toggleFavorite, getMyFavorites, checkFavorite |
| `mcService.js` | `/mcs` | 6 | getMCDashboard, updateMCProfile, getMCCalendar, createBlockout, getMCWallet, requestPayout |
| `notificationService.js` | `/notifications` | 5 | getNotifications, getUnreadCount, markAsRead, markAllAsRead, clearAllNotifications |
| `paymentService.js` | `/payments` | 3 | createPaymentLink, getPaymentHistory, checkPaymentStatus |
| `reportService.js` | `/reports` | 4 | createReport, getMyReports, getAllReportsAdmin, resolveReport |
| `reviewService.js` | `/reviews` | 4 | createReview, getMCReviews, updateReview, deleteReview |
| `scriptService.js` | `/scripts` | 4 | getScripts, getScriptById, favoriteScript, getScriptReader |
| `availabilityService.js` | `/availability` | 3 | createAvailability, getMCAvailability, removeAvailability |
| `adminService.js` | `/admin` | 6 | getAdminDashboard, getAllUsers, getAllMCs, getAllClients, updateUserStatus, getAllBookings |
| `auditLogService.js` | `/audit-logs` | 2 | getAllLogs, getUserLogs |
| `aiService.js` | External AI API | 1 | analyzeVoice (kết nối AI service riêng biệt) |

> **Lưu ý import:** Tất cả service file đều import từ `./api` (axios instance đã có JWT interceptor).
> **Tất cả hàm** đều là `async/await` và unwrap `response.data.data` trừ khi cần toàn bộ `response.data` (login, register).
