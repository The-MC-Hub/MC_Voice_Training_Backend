# 🌐 The MC Hub — Central API Documentation

Tài liệu mô tả chi tiết tất cả REST APIs, cấu trúc Request/Response, chuẩn Authentication và Mã lỗi hệ thống (ErrorCode).

---

## 🔑 Authentication & Headers

- **Base URL:** `/api/v1`
- **Header xác thực:** `Authorization: Bearer <JWT_TOKEN>`
- **Response Format:**
```json
{
  "status": "success",
  "message": "Nội dung thông báo",
  "data": { ... },
  "errorCode": null
}
```

---

## 📋 Danh Mục Endpoints Chính

### 1. Authentication & User Account (`/api/v1/auth`, `/api/v1/users`)
| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| `POST` | `/auth/register` | Public | Đăng ký tài khoản người dùng mới |
| `POST` | `/auth/login` | Public | Đăng nhập bằng Email/Password, nhận JWT |
| `POST` | `/auth/google` | Public | Đăng nhập/Đăng ký qua Google ID Token |
| `GET` | `/users/me` | User | Lấy thông tin cá nhân hiện tại |
| `PUT` | `/users/profile` | User | Cập nhật thông tin profile cá nhân |

### 2. Voice Practice & AI Analysis (`/api/v1/voice`)
| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| `GET` | `/voice/lessons` | Public | Danh sách bài luyện giọng theo danh mục |
| `GET` | `/voice/lessons/{id}` | Public | Chi tiết bài luyện giọng |
| `POST` | `/voice/practice/analyze` | User | Tải lên file ghi âm bài tập để AI chấm điểm |
| `POST` | `/voice/practice/analyze-guest` | Public | Chấm điểm bài đọc dùng thử cho Khách |

### 3. Payment & Pricing (`/api/v1/payment`)
| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| `GET` | `/payment/plans` | Public | Danh sách các gói cước VIP (BASIC, FULL, ANNUAL) |
| `POST` | `/payment/create-order` | User | Tạo link thanh toán PayOS nhận `checkoutUrl` |
| `POST` | `/payment/webhook` | Public | Webhook xử lý phản hồi tự động từ PayOS |

### 4. Admin Management (`/api/v1/admin`)
| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| `GET` | `/admin/system/health` | Admin | Giám sát RAM, Threads, Uptime, DB Status |
| `PUT` | `/admin/system/settings/dynamic` | Admin | Bật/Tắt Chế độ Bảo trì (Maintenance Mode) |
| `PUT` | `/admin/users/{id}/suspend-temporary` | Admin | Tạm khóa tài khoản theo số ngày (1, 3, 7, 30 ngày) |
| `PUT` | `/admin/users/{id}/unsuspend` | Admin | Mở khóa tài khoản ngay lập tức |
| `POST` | `/admin/transactions/{id}/refund` | Admin | Đánh dấu hoàn tiền và ghi lý do |
| `GET` | `/admin/bookings/stats` | Admin | Thống kê lịch đặt MC theo trạng thái |
| `PUT` | `/admin/bookings/{id}/force-cancel` | Admin | Bắt buộc hủy đơn đặt MC |

### 5. Reports & Moderation (`/api/v1/reports`)
| Method | Endpoint | Auth | Mô tả |
|---|---|---|---|
| `POST` | `/reports` | User | Gửi báo cáo nội dung hoặc hành vi vi phạm |
| `GET` | `/reports/admin` | Admin | Xem toàn bộ báo cáo vi phạm |
| `PUT` | `/reports/{id}/resolve` | Admin | Duyệt (Resolve) hoặc Bỏ qua (Dismiss) báo cáo |
| `PUT` | `/reports/bulk-resolve` | Admin | Duyệt/bỏ qua hàng loạt báo cáo vi phạm |

---

## ⚠️ Danh Mục Mã Lỗi System (ErrorCodes)

| Code | HTTP Status | Mô tả |
|---|---|---|
| `VALIDATION_FAILED` | 400 Bad Request | Dữ liệu gửi lên không phù hợp validator |
| `USER_NOT_AUTHENTICATED` | 401 Unauthorized | JWT hết hạn hoặc chưa đăng nhập |
| `ACCESS_DENIED` | 403 Forbidden | Không đủ quyền hạn thực hiện hành động |
| `RESOURCE_NOT_FOUND` | 404 Not Found | Tài nguyên requested không tồn tại |
| `USER_NOT_FOUND` | 404 Not Found | Tài khoản người dùng không tồn tại |
| `ACCOUNT_LOCKED` | 403 Forbidden | Tài khoản bị tạm khóa/khóa có thời hạn |
| `MAINTENANCE_MODE` | 503 Service Unavailable | Hệ thống đang bật Chế độ Bảo trì |
| `INTERNAL_ERROR` | 500 Internal Error | Lỗi nội bộ hệ thống |
