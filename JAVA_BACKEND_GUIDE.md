# 📘 The MC Hub — Java Backend Guide

> **Phiên bản tài liệu:** 4.0 — Cập nhật ngày 2026-05-09  
> Backend Java Spring Boot cho nền tảng đặt lịch MC. Chuyển đổi từ Node.js và mở rộng theo chuẩn **3NF + MapStruct + Type-Safe Enum**.

---

## 🏗️ Kiến trúc Hệ thống

Tuân thủ nghiêm ngặt mô hình **4 lớp** của Spring Boot:

```text
Request → Controller → Service → Repository → MongoDB Atlas
                ↑           ↑
            DTO Layer (Data Objects)
                ↑           ↑
            Mapper Layer (MapStruct)
                ↑
           SecurityUtils (Auth context)
```

### Trách nhiệm các lớp

| Lớp            | Annotation        | Trách nhiệm                                                     |
| :------------- | :---------------- | :-------------------------------------------------------------- |
| **Controller** | `@RestController` | Nhận request, validate DTO, trả về `ApiResponse<T>`             |
| **Service**    | `@Service`        | Toàn bộ business logic, `@Async`, `CompletableFuture`           |
| **Mapper**     | `@Mapper`         | MapStruct interfaces, tự động sinh code chuyển đổi Entity ↔ DTO |
| **Repository** | `@Repository`     | Spring Data MongoDB, chỉ chứa queries                           |
| **Model**      | `@Document`       | MongoDB Document, dùng Lombok, enum type-safe                   |
| **DTO**        | POJO              | Request/Response riêng biệt, validation `@Valid`                |
| **Util**       | `final class`     | `SecurityUtils` - shared helper không có state                  |

---

## 📁 Cấu trúc Thư mục

```text
src/main/java/com/mchub/
├── config/             # Cấu hình hệ thống
│   ├── AsyncConfig.java         # Virtual Threads + ThreadPoolTaskExecutor
│   ├── SecurityConfig.java      # Spring Security 6 + JWT filter chain
│   ├── WebSocketConfig.java     # STOMP broker config
│   ├── CloudinaryConfig.java    # Cấu hình lưu trữ ảnh/video
│   ├── ElasticsearchConfig.java # Cấu hình tìm kiếm nâng cao
│   ├── SwaggerConfig.java       # OpenAPI / Swagger UI documentation
│   └── DotenvConfig.java        # Load biến môi trường từ .env
│
├── mapper/              # MapStruct Mappers (16 mappers)
│   ├── UserMapper.java
│   ├── BookingMapper.java
│   ├── MCProfileMapper.java
│   ├── ChatMapper.java
│   └── ... (toàn bộ 16 models)
│
├── controllers/        # REST API Endpoints (19 controllers)
│   ├── AuthController.java
│   ├── AdminController.java
│   ├── BookingController.java
│   ├── BookingDetailController.java
│   ├── MCController.java
│   ├── ReviewController.java
│   ├── ScriptController.java
│   ├── NotificationController.java
│   ├── AvailabilityController.java
│   ├── ChatController.java
│   ├── PaymentController.java
│   ├── PublicController.java
│   ├── FavoriteController.java
│   ├── ReportController.java
│   ├── CertificateController.java
│   ├── CouponController.java
│   ├── AuditLogController.java
│   ├── MediaController.java          ← MỚI
│   └── SearchController.java         ← MỚI
│
├── services/           # Business Logic (20 services)
│   ├── AuthService.java
│   ├── AdminService.java
│   ├── BookingService.java
│   ├── BookingDetailService.java
│   ├── MCProfileService.java
│   ├── ReviewService.java
│   ├── ScriptService.java
│   ├── NotificationService.java
│   ├── AvailabilityService.java
│   ├── ChatService.java
│   ├── PayOSService.java
│   ├── PublicService.java
│   ├── JwtService.java
│   ├── FavoriteService.java
│   ├── ReportService.java
│   ├── CertificateService.java
│   ├── CouponService.java
│   ├── AuditLogService.java
│   ├── MediaService.java             ← MỚI
│   └── SearchSyncService.java        ← MỚI
│
├── repositories/       # MongoDB Repositories (17 repositories)
│   ├── UserRepository.java
│   ├── BookingRepository.java
│   ├── BookingDetailRepository.java  ← MỚI
│   ├── MCProfileRepository.java
│   ├── ReviewRepository.java
│   ├── ScriptRepository.java
│   ├── NotificationRepository.java
│   ├── ScheduleRepository.java
│   ├── ConversationRepository.java
│   ├── MessageRepository.java
│   ├── TransactionRepository.java
│   ├── FavoriteRepository.java       ← MỚI
│   ├── ReportRepository.java         ← MỚI
│   ├── CertificateRepository.java    ← MỚI
│   ├── CouponRepository.java         ← MỚI
│   ├── RefreshTokenRepository.java   ← MỚI
│   └── AuditLogRepository.java       ← MỚI
│
├── models/             # MongoDB Documents (17 models)
│   ├── User.java
│   ├── Booking.java
│   ├── BookingDetail.java            ← MỚI (3NF)
│   ├── MCProfile.java
│   ├── Review.java
│   ├── Script.java
│   ├── Notification.java
│   ├── Schedule.java
│   ├── Conversation.java
│   ├── Message.java
│   ├── Transaction.java
│   ├── Favorite.java                 ← MỚI (3NF)
│   ├── Report.java                   ← MỚI (3NF)
│   ├── Certificate.java              ← MỚI (3NF)
│   ├── Coupon.java                   ← MỚI (3NF)
│   ├── RefreshToken.java             ← MỚI (3NF)
│   └── AuditLog.java                 ← MỚI (3NF)
│
├── dto/                # Data Transfer Objects (22 files)
│   ├── ApiResponse.java              # Generic wrapper {status, message, data}
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── UserResponseDTO.java
│   ├── UpdateProfileRequest.java
│   ├── CreateBookingRequest.java
│   ├── UpdateBookingStatusRequest.java
│   ├── BookingResponseDTO.java
│   ├── CreateBookingDetailRequest.java
│   ├── BookingDetailResponseDTO.java
│   ├── CreateReviewRequest.java
│   ├── ReviewResponseDTO.java
│   ├── CreateScriptRequest.java
│   ├── ScriptResponseDTO.java
│   ├── NotificationResponseDTO.java
│   ├── CreateReportRequest.java
│   ├── ReportResponseDTO.java
│   ├── CreateCertificateRequest.java
│   ├── CertificateResponseDTO.java
│   ├── CreateCouponRequest.java
│   ├── CouponResponseDTO.java
│   └── AuditLogResponseDTO.java
│
├── enums/              # Type-safe Enums (18 enums)
│   ├── UserRole.java               # CLIENT | MC | ADMIN
│   ├── BookingStatus.java          # PENDING | ACCEPTED | REJECTED | COMPLETED | CANCELLED | PAID
│   ├── PaymentStatus.java          # PENDING | DEPOSIT_PAID | FULLY_PAID
│   ├── EventType.java              # WEDDING | CORPORATE_CONFERENCE | ...
│   ├── MCStatus.java               # AVAILABLE | BUSY | ON_LEAVE | INACTIVE
│   ├── MediaType.java              # IMAGE | VIDEO | AUDIO
│   ├── MessageType.java            # TEXT | IMAGE | FILE | SYSTEM
│   ├── NotificationType.java       # BOOKING_REQUEST | PAYMENT_RECEIVED | ...
│   ├── ScheduleStatus.java         # AVAILABLE | UNAVAILABLE | TENTATIVE
│   ├── ScriptCategory.java         # WEDDING | CORPORATE | GALA | ...
│   ├── TransactionType.java        # DEPOSIT | FULL_PAYMENT | REFUND
│   ├── TransactionStatus.java      # PENDING | COMPLETED | FAILED | REFUNDED
│   ├── DressCode.java              # FORMAL | SEMI_FORMAL | CASUAL | TRADITIONAL | THEMED
│   ├── VenueType.java              # INDOOR | OUTDOOR | HYBRID | VIRTUAL
│   ├── ReportReason.java           # FRAUD | INAPPROPRIATE_BEHAVIOR | ...
│   ├── ReportStatus.java           # PENDING | REVIEWING | RESOLVED | DISMISSED
│   ├── DiscountType.java           # PERCENT | FIXED_AMOUNT
│   └── AuditAction.java            # AUTH_LOGIN | AUTH_REGISTER | BOOKING_CREATE | ...
│
└── util/               # Shared Utilities
    └── SecurityUtils.java          # getCurrentUserId(), safeMessage()
```

---

## 🗄️ Database Schema (MongoDB Atlas — 3NF)

Cluster: `MainDatabase` trên MongoDB Atlas (AP_EAST_1)  
URI: `mongodb+srv://trungle:***@maindatabase.2tirj0y.mongodb.net/mchub`

### Collections & Quan hệ

```text
users ─────────────────────────────────────────────────────┐
  │                                                         │
  ├─► mcprofiles (user → mcprofiles._id)                   │
  │     └─► certificates (mcProfileId → mcprofiles._id)    │
  │                                                         │
  ├─► bookings (client, mc → users._id)                    │
  │     └─► booking_details (bookingId → bookings._id)     │
  │     └─► reviews (booking → bookings._id)               │
  │     └─► transactions (booking → bookings._id)          │
  │     └─► reports (bookingId → bookings._id)             │
  │                                                         │
  ├─► favorites (clientId, mcUserId → users._id)           │
  ├─► notifications (user → users._id)                     │
  ├─► refreshtokens (userId → users._id)                   │
  ├─► audit_logs (userId → users._id)                      │
  │                                                         │
  ├─► conversations (participants[] → users._id)           │
  │     └─► messages (conversationId → conversations._id)  │
  │                                                         │
  ├─► schedules (mc → users._id)                           │
  ├─► scripts (createdBy → users._id)                      │
  └─► coupons (createdBy → users._id)
```

---

## 📦 DTO Pattern — Quy tắc Request/Response

### Nguyên tắc bắt buộc

1. **Controller chỉ nhận DTO** — không nhận trực tiếp Model/Entity
2. **Controller chỉ trả DTO** — không trả trực tiếp Model (tránh lộ `password`, trường internal)
3. **Sử dụng Mapper (MapStruct)** — không gọi `new ResponseDTO(model)` thủ công.
4. **Mỗi model có ít nhất 2 DTO**: `XxxRequest` (nhận vào) và `XxxResponseDTO` (trả ra)
5. **Validate tại DTO** dùng `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Email`, `@Size`

### Ví dụ luồng Request → Response

```java
@PostMapping
public ResponseEntity<ApiResponse<BookingResponseDTO>> createBooking(
        @RequestBody @Valid CreateBookingRequest req) {
    String clientId = SecurityUtils.getCurrentUserId();
    Booking saved = bookingService.createBooking(req, clientId);
    // ✅ Dùng mapper thay vì new BookingResponseDTO(saved)
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Đặt lịch thành công", bookingMapper.toResponseDTO(saved)));
}
```

---

## 🗺️ Mapper Pattern (MapStruct)

Chúng ta sử dụng **MapStruct** để tự động hóa việc chuyển đổi giữa Entities và DTOs. Điều này giúp giảm code thừa và tránh lỗi khi gán thủ công từng field.

### 1. Định nghĩa Mapper

Mapper là một interface với annotation `@Mapper`.

```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    @Mapping(target = "name", source = "name", defaultValue = "Unknown")
    UserResponseDTO toResponseDTO(User user);
}
```

### 2. Sử dụng Mapper

Tiêm Mapper vào Controller hoặc Service qua Constructor (`@RequiredArgsConstructor`).

```java
@RequiredArgsConstructor
public class UserController {
    private final UserMapper userMapper;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable String id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(userMapper.toResponseDTO(user)));
    }
}
```

### 3. Compile-time generation

Mỗi khi bạn chạy `mvn compile`, MapStruct sẽ sinh ra lớp `UserMapperImpl` trong thư mục `target`. Nếu bạn thêm field mới vào Model hoặc DTO, hãy chạy lại lệnh build để cập nhật Mapper.

### Format Response chuẩn

Tất cả API sử dụng `ApiResponse<T>`:

```json
{
  "status": "success",
  "message": "Đặt lịch thành công",
  "data": { ... }
}
```

```json
{
  "status": "fail",
  "message": "Booking không tồn tại",
  "data": null
}
```

---

## 🔒 Bảo mật (Security)

### JWT + Spring Security 6

- Token chứa `userId` (subject), validate qua `JwtAuthenticationFilter`
- `SecurityConfig` định nghĩa whitelist và phân quyền theo role

### SecurityUtils (Shared Utility)

```java
// Thay vì lặp getLoggedInUserId() trong mỗi Controller:
String userId = SecurityUtils.getCurrentUserId();   // Ném IllegalStateException nếu chưa login
String msg    = SecurityUtils.safeMessage(e);       // Không bao giờ trả null (phòng NPE)
```

### Phân quyền

```java
@PreAuthorize("hasAuthority('ADMIN')")   // Chỉ Admin
@PreAuthorize("hasAuthority('MC')")      // Chỉ MC
// Không annotation = yêu cầu đăng nhập nhưng không check role
```

---

## ⚡ Đa luồng & Hiệu năng

### Virtual Threads (Java 21)

Cấu hình trong `AsyncConfig.java` — mỗi request HTTP và task Async chạy trên Virtual Thread riêng.

### @Async — Fire and Forget

Dùng cho các tác vụ **không cần chờ kết quả**:

```java
@Async
public void sendBookingNotifications(Booking booking) { ... }  // BookingService

@Async
public void log(String userId, AuditAction action, ...) { ... } // AuditLogService

@Async
public void updateMCProfileRatingAsync(String mcId) { ... }    // ReviewService

@Async
public void initializeMCProfile(String userId) { ... }          // AuthService
```

### CompletableFuture — Parallel Queries

Dùng khi cần **nhiều truy vấn độc lập chạy cùng lúc**:

```java
// AdminService — Dashboard chạy 5 queries song song
CompletableFuture<Long> clientCount = CompletableFuture.supplyAsync(() -> userRepository.countByRole(UserRole.CLIENT));
CompletableFuture<Long> mcCount     = CompletableFuture.supplyAsync(() -> userRepository.countByRole(UserRole.MC));
CompletableFuture<Long> pending     = CompletableFuture.supplyAsync(() -> bookingRepository.countByStatus(BookingStatus.PENDING));
CompletableFuture.allOf(clientCount, mcCount, pending).join();
```

---

## 🎯 Enum Type-Safety — Quy tắc bắt buộc

**KHÔNG** dùng String thô cho các trường phân loại:

```java
// ❌ SAI — dễ typo, không compile-time safe
booking.setStatus("accepted");
userRepository.countByRole("client");

// ✅ ĐÚNG — type-safe, IDE hỗ trợ, compile-time check
booking.setStatus(BookingStatus.ACCEPTED);
userRepository.countByRole(UserRole.CLIENT);
```

---

## 🛠️ Công nghệ Tích hợp

| Thành phần      | Công nghệ                      | Chi tiết                                              |
| :-------------- | :----------------------------- | :---------------------------------------------------- |
| **Database**    | MongoDB Atlas                  | Cluster `MainDatabase`, replica set AWS AP_EAST_1     |
| **Auth**        | JWT (jjwt) + Spring Security 6 | Token = userId, bcrypt password                       |
| **Payment**     | PayOS                          | Webhook xử lý Async, Transaction enum-safe            |
| **Real-time**   | WebSocket STOMP                | Topic: `/topic/chat/{userId}`, `/topic/notifications` |
| **Media**       | Cloudinary                     | API upload/delete media, CDN optimization             |
| **Search**      | Elasticsearch                  | Sync dữ liệu qua `SearchSyncService`                   |
| **Documentation** | SpringDoc OpenAPI (Swagger)  | UI tại `/swagger-ui.html`                             |
| **Environment** | dotenv-java                    | File `.env` tại root project                          |
| **Async**       | Virtual Threads Java 21        | `AsyncConfig.java`                                    |
| **Validation**  | Jakarta Bean Validation        | `@Valid` tại Controller, constraints tại DTO          |

---

## 📋 Quy tắc Null-Safety

```java
// 1. Kiểm tra null trước khi dùng Optional
String mcProfileId = user.getMcProfile();
if (mcProfileId == null || mcProfileId.isBlank()) {
    throw new RuntimeException("Không tìm thấy hồ sơ MC");
}

// 2. orElseThrow() phải có message rõ ràng
userRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("User không tồn tại: " + id));

// 3. Không dùng bare orElseThrow()
// ❌  .orElseThrow()
// ✅  .orElseThrow(() -> new RuntimeException("..."))

// 4. Exception message an toàn
return ApiResponse.fail(SecurityUtils.safeMessage(e)); // không bao giờ null
```

---

## 🤖 Hướng dẫn cho AI/Copilot

Khi yêu cầu AI viết code, sử dụng các convention sau:

| Việc cần làm     | Prompt mẫu                                                                                                                             |
| :--------------- | :------------------------------------------------------------------------------------------------------------------------------------- |
| Tạo endpoint mới | _"Thêm endpoint `POST /api/v1/xxx` nhận `XxxRequest` DTO, trả `ApiResponse<XxxResponseDTO>`, dùng `SecurityUtils.getCurrentUserId()`"_ |
| Tác vụ async     | _"Thêm `@Async` vào hàm `sendNotification` trong `XxxService`"_                                                                        |
| Dashboard stats  | _"Dùng `CompletableFuture.supplyAsync()` để chạy song song 3 queries đếm trong AdminService"_                                          |
| Phân quyền       | _"Thêm `@PreAuthorize(\"hasAuthority('ADMIN')\")` cho endpoint này"_                                                                   |
| Enum mới         | _"Thêm enum `XxxType` với các giá trị A, B, C vào package `com.mchub.enums`"_                                                          |

---

> **LƯU Ý QUAN TRỌNG:**
>
> - Không viết business logic trong Controller
> - Không truy cập Repository trực tiếp từ Controller
> - Không dùng `String` thô thay cho Enum đã định nghĩa
> - Không dùng `e.getMessage()` trực tiếp — dùng `SecurityUtils.safeMessage(e)`
> - Không dùng `orElseThrow()` không có message
