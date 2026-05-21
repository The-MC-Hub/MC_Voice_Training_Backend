# 📊 The MC Hub — Java Backend Migration & Change Tracker

> **Cập nhật lần cuối:** 2026-05-09  
> Theo dõi tiến độ migration từ Node.js sang Java Spring Boot và các thay đổi kiến trúc.

---

## 🚀 Trạng thái dự án

| Hạng mục | Trạng thái | Chi tiết |
| :---- | :----------- | :---- |
| **Tiến độ tổng thể** | ✅ ~98% | Search, Media & Swagger đã tích hợp thành công |
| **Database** | ✅ Online | MongoDB Atlas + Elasticsearch |
| **Compile** | ✅ BUILD SUCCESS | 0 errors, SpringDoc generated |
| **UI Modernization** | ✅ Completed | "Deep Navy & Gold" Cinematic Design implemented |
| **Ngày bắt đầu** | 2026-03-25 | — |

---

## 📅 Nhật ký Module

| STT | Module | Trạng thái | Phiên bản | Ghi chú |
| :--- | :------- | :---------- | :--------- | :---- |
| 1 | Khởi tạo dự án | ✅ Hoàn thành | v1.0 | `pom.xml`, Java 21, Spring Boot 3.2 |
| 2 | Virtual Threads config | ✅ Hoàn thành | v1.0 | `AsyncConfig.java` |
| 3 | MongoDB Atlas kết nối | ✅ Hoàn thành | v1.1 | Cluster mới `MainDatabase`, `.env` |
| 4 | Authentication (JWT) | ✅ Hoàn thành | v1.0 | jjwt, bcrypt, `JwtAuthenticationFilter` |
| 5 | User Model + Repository | ✅ Hoàn thành | v2.0 | Enum `UserRole`, `findByRole()`, `countByRole()` |
| 6 | Booking Module | ✅ Hoàn thành | v2.0 | Enum `BookingStatus`, DTO, cancel/status update |
| 7 | BookingDetail (3NF mới) | ✅ Hoàn thành | v2.0 | Tách từ Booking, `DressCode`, `VenueType` enum |
| 8 | MCProfile Module | ✅ Hoàn thành | v1.0 | Dashboard stats parallel, auto-init khi đăng ký |
| 9 | Review Module | ✅ Hoàn thành | v2.0 | `CreateReviewRequest` DTO, async rating update |
| 10 | Script Module | ✅ Hoàn thành | v2.0 | `ScriptResponseDTO`, `ScriptCategory` enum |
| 11 | Notification Module | ✅ Hoàn thành | v2.0 | `NotificationResponseDTO`, `NotificationType` enum |
| 12 | Availability/Schedule | ✅ Hoàn thành | v2.0 | `ScheduleStatus` enum, import chuẩn |
| 13 | Chat (WebSocket STOMP) | ✅ Hoàn thành | v2.0 | `MessageType` enum, Virtual Threads broadcast |
| 14 | Payment (PayOS) | ✅ Hoàn thành | v2.0 | `TransactionType`, `TransactionStatus` enum |
| 15 | **Favorite** (3NF mới) | ✅ Hoàn thành | v2.0 | Toggle favorite MC, FavoriteService + Controller |
| 16 | **Report** (3NF mới) | ✅ Hoàn thành | v2.0 | `ReportReason`, `ReportStatus` enum, Admin resolve |
| 17 | **Certificate** (3NF mới) | ✅ Hoàn thành | v2.0 | MC credentials, Admin verify, null-safe |
| 18 | **Coupon** (3NF mới) | ✅ Hoàn thành | v2.0 | `DiscountType` enum, discount calculation, validate API |
| 19 | **AuditLog** (3NF mới) | ✅ Hoàn thành | v2.0 | `AuditAction` enum, Async logging, Admin-only |
| 20 | RefreshToken (3NF mới) | ✅ Model + Repo | v2.0 | Multi-device session, chưa tích hợp vào AuthService |
| 21 | Admin Module | ✅ Hoàn thành | v2.0 | Dashboard parallel, UserResponseDTO (ẩn password) |
| 22 | Public/Discovery API | ✅ Hoàn thành | v2.0 | `UserRole` enum, parallel landing stats |
| 23 | **DTO Package** | ✅ Hoàn thành | v2.0 | 22 DTOs — mỗi model có Request + Response riêng |
| 24 | **Enum Package** | ✅ Hoàn thành | v2.0 | 18 enums type-safe thay toàn bộ String thô |
| 25 | **SecurityUtils** | ✅ Hoàn thành | v2.0 | Shared utility, null-safe auth + error message |
| 26 | Seed Data Script | ✅ Hoàn thành | v2.0 | `seed-data/seed_mchub.js`, 17 collections |
| 27 | **MapStruct Mapper Layer** | ✅ Hoàn thành | v3.0 | 16 Mappers, xóa toàn bộ manual mapping logic |
| 28 | **Search Module** | ✅ Hoàn thành | v4.0 | Elasticsearch integration, sync dữ liệu tự động |
| 29 | **Media Module** | ✅ Hoàn thành | v4.0 | Cloudinary integration, upload/delete API |
| 30 | **Swagger/OpenAPI** | ✅ Hoàn thành | v4.0 | SpringDoc OpenAPI 3, UI tại `/swagger-ui.html` |

---

## 🏗️ Thay đổi Kiến trúc lớn (v3.0 — 2026-04-19)

### 1. Chuẩn hóa 3NF Schema — 7 Models mới

| Model | Lý do tách | Enum sử dụng |
| :---- | :---- | :---- |
| `BookingDetail` | Tách chi tiết kỹ thuật ra khỏi `Booking` | `DressCode`, `VenueType` |
| `Favorite` | Junction table Client ↔ MC (thay array) | — |
| `Report` | Tách báo cáo vi phạm thành entity riêng | `ReportReason`, `ReportStatus` |
| `Certificate` | Tách chứng chỉ MC ra khỏi `MCProfile` | — |
| `Coupon` | Quản lý mã giảm giá độc lập | `DiscountType`, `EventType` |
| `RefreshToken` | Multi-device session management | — |
| `AuditLog` | Nhật ký hành động hệ thống | `AuditAction` |

### 2. DTO Layer hoàn chỉnh (từ sơ sài → 22 files)

**Trước (v1.0):** Controller nhận/trả thẳng Model, lộ `password`, không validate.

**Sau (v3.0):**

```text
Model → Mapper (MapStruct) → ResponseDTO (tự động, type-safe)
DTO Request → @Valid                     (validate trước khi xử lý)
ApiResponse<T>                           (format nhất quán)
```

### 3. Mapper Layer (Mới — v3.0)

Xóa bỏ hoàn toàn việc gọi `new ResponseDTO(model)` rải rác khắp code.

- **Tự động:** MapStruct sinh code implementation tại thời điểm compile.
- **Tiết kiệm:** Không viết code map tay, giảm sai sót, code controller/service cực gọn.
- **Injected:** Mappers được tiêm vào qua Spring IoC Container.

### 4. Enum Type-Safety (từ String thô → 18 Enums)

**Trước:**

```java
booking.setStatus("Accepted");          // Runtime error nếu typo
userRepository.countByRole("client");   // Không compile-time check
tx.setType("Deposit");                  // Không có autocomplete
```

**Sau:**

```java
booking.setStatus(BookingStatus.ACCEPTED);
userRepository.countByRole(UserRole.CLIENT);
tx.setType(TransactionType.DEPOSIT);
```

### 5. SecurityUtils — Xóa code lặp

**Trước:** Mỗi controller có một phương thức `getLoggedInUserId()` riêng (11 lần lặp), không check null, không handle `anonymousUser`.

**Sau:**

```java
// Một nơi duy nhất, null-safe, xử lý anonymousUser
String userId = SecurityUtils.getCurrentUserId();
String msg    = SecurityUtils.safeMessage(e);  // Phòng NPE khi getMessage() null
```

### 6. Import chuẩn Java (không dùng fully-qualified path)

**Trước:**

```java
scheduleData.setStatus(com.mchub.enums.ScheduleStatus.UNAVAILABLE);
userRepository.countByRole(com.mchub.enums.UserRole.CLIENT);
java.util.List<User> findByRole(com.mchub.enums.UserRole role);
```

**Sau:** Khai báo `import` đúng chuẩn tại đầu file, dùng tên ngắn trong code.

---

## 🔧 Thay đổi từng file (v1.0 → v2.0)

### Controllers cập nhật

| File | Thay đổi chính |
| :---- | :---- |
| `AuthController` | `RegisterRequest` DTO, `AuditLog` khi login/register, `safeMessage()` |
| `BookingController` | `CreateBookingRequest` DTO, `UpdateBookingStatusRequest`, cancel endpoint |
| `AdminController` | DTO responses (ẩn password), dashboard parallel stats, MC/Client filter |
| `MCController` | `SecurityUtils`, typed `ApiResponse<>` |
| `ReviewController` | `CreateReviewRequest` DTO thay raw `Review` model |
| `ScriptController` | `ScriptResponseDTO`, typed response, `HttpStatus.NOT_FOUND` |
| `NotificationController` | `NotificationResponseDTO`, typed response |
| `AvailabilityController` | `SecurityUtils`, `ApiResponse<Schedule>` typed |
| *7 controllers mới* | `BookingDetail`, `Favorite`, `Report`, `Certificate`, `Coupon`, `AuditLog` |

### Services cập nhật

| File | Thay đổi chính |
| :---- | :---- |
| `AuthService` | `RegisterRequest` DTO, `UserRole` enum, async `initializeMCProfile` |
| `BookingService` | `CreateBookingRequest` DTO, `BookingStatus` enum, parallel availability check |
| `AdminService` | `UserRole` enum, `UserResponseDTO` (ẩn password), `BookingStatus` enum |
| `PayOSService` | `TransactionType`, `TransactionStatus`, `PaymentStatus` enum |
| `AvailabilityService` | `ScheduleStatus` enum (import đúng chuẩn) |
| `ChatService` | `MessageType` enum, import chuẩn |
| `PublicService` | `UserRole` enum, import chuẩn, inject `MCProfileMapper` |
| `ReviewService` | Overload `createReview()`, null-safe check, inject `ReviewMapper` |
| *Mapper Services* | Toàn bộ service giờ dùng mappers thay vì gọi constructor DTO trực tiếp |
| *5 services mới* | `BookingDetailService`, `FavoriteService`, `ReportService`, `CertificateService`, `CouponService`, `AuditLogService` |

### Repositories cập nhật

| File | Thay đổi chính |
| :---- | :---- |
| `BookingRepository` | `BookingStatus` enum thay `String` trong tất cả method |
| `UserRepository` | `UserRole` enum, thêm `findByRole()`, `import` chuẩn |
| *7 repositories mới* | `BookingDetail`, `Favorite`, `Report`, `Certificate`, `Coupon`, `RefreshToken`, `AuditLog` |

---

## 🗃️ Database Connection

```properties
# application.properties
spring.data.mongodb.uri=${MONGODB_URI}
```

```env
# .env
MONGODB_URI=mongodb+srv://trungle:Pitngu%401234@maindatabase.2tirj0y.mongodb.net/mchub?retryWrites=true&w=majority&appName=MainDatabase
```

### Seed Data

- Script: `seed-data/seed_mchub.js`
- Tool: `mongosh`
- Database: `mchub` trên cluster `MainDatabase`
- Số collections: 17, mỗi collection ≥ 30 records

---

## ⚠️ Việc còn lại (TODO)

| Hạng mục | Ưu tiên | Mô tả |
| :---- | :---- | :---- |
| `RefreshToken` tích hợp | Cao | Kết nối vào `AuthService.login()` để lưu refresh token |
| `UpdateMCProfileRequest` DTO | Trung bình | Hiện `MCController` còn nhận raw `MCProfile` model |
| Pagination | Trung bình | Cần `PagedResponse<T>` DTO cho các list API như Chat, AuditLog |
| Refresh Token Integration | Trung bình | Tích hợp RefreshToken vào luồng Auth chính |

---

## 📝 Ghi chú kỹ thuật

- **Token secret** phải đồng bộ với frontend (`.env` cùng key)
- **`@Document`** annotation bắt buộc cho mọi Model
- **`@CreatedDate` / `@LastModifiedDate`** cần `@EnableMongoAuditing` trong config
- **Virtual Threads** yêu cầu Java 21+, cấu hình trong `AsyncConfig.java`
- **`@Async`** chỉ hoạt động khi gọi từ Bean khác (không gọi trong cùng class)
- **Enum serialization** MongoDB: mặc định lưu tên String của enum (UPPERCASE)
 
+---
+
+## 💎 Đồng bộ Frontend & Hiện đại hóa UI (2026-05)
+
+| Thành phần | Chi tiết | Tương quan Backend |
+| :---- | :---- | :---- |
+| **Theme Cinematic** | Deep Navy & Gold | Phù hợp với Brand Image cao cấp của dự án |
+| **DotField Background** | Canvas particles | Tạo không gian công nghệ cho AI features |
+| **About Us Page** | Storytelling + Team Grid | Kết nối với MCProfile & Team data |
+| **AI Waveform** | Mô phỏng phân tích giọng nói | Interface cho AI Voice Metrics service |
+| **Luxury Spacing** | Utility-based padding | Đảm bảo Dashboard hiển thị chuyên nghiệp |
+
+*Tài liệu được cập nhật tự động bởi Antigravity Agent.*
+
