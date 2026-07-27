# UC-09 — Quản Trị Hệ Thống & Kiểm Duyệt (Admin Dashboard & System Health)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Quản trị hệ thống, Giám sát và Kiểm duyệt.

---

## 🛠️ UC-09.1: Giám Sát Sức Khỏe Hệ Thống Realtime (System Health Monitoring)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Đo đạc và hiển thị các chỉ số hiệu năng realtime: JVM Heap/Non-heap RAM MB & %, Active Virtual Threads, Peak Threads, System Uptime hours, MongoDB ping response status.
- **Endpoint:** `GET /api/v1/admin/system/health`

### 📐 2. Class Diagram (UC-09.1)
```mermaid
classDiagram
    class AdminSystemController {
        +getSystemHealth() ResponseEntity~ApiResponse~
    }
    class SystemHealthService {
        <<interface>>
        +getSystemHealth() HealthMetricsDTO
    }
    class SystemHealthServiceImpl {
        -MongoTemplate mongoTemplate
    }
    class HealthMetricsDTO {
        +String status
        +MemoryMetrics memory
        +ThreadMetrics threads
        +SystemMetrics system
        +String dbStatus
    }
    AdminSystemController --> SystemHealthService
    SystemHealthServiceImpl ..|> SystemHealthService
    SystemHealthServiceImpl --> HealthMetricsDTO
```

### 🔄 3. Sequence Diagram (UC-09.1)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Administrator
    participant Controller as AdminSystemController
    participant Service as SystemHealthServiceImpl
    participant JVM as Java ManagementFactory MXBean
    participant DB as MongoDB Atlas

    Admin->>Controller: GET /api/v1/admin/system/health
    Controller->>Service: getSystemHealth()
    Service->>JVM: getMemoryMXBean(), getThreadMXBean(), getRuntimeMXBean()
    JVM-->>Service: Memory & Thread Metrics
    
    Service->>DB: executeCommand("{ ping: 1 }")
    DB-->>Service: DB Response ok = 1
    
    Service->>Service: Build HealthMetricsDTO
    Service-->>Controller: HealthMetricsDTO
    Controller-->>Admin: 200 OK (Chi tiết RAM, Threads, Uptime, DB Status)
```

### 🧪 4. Testing & Verification (UC-09.1)
- **Unit Test Method:** `SystemHealthServiceImplTest.java` -> `getSystemHealth_returnsValidMetrics()`
- **Assertions:** RAM Heap MB > 0, `dbStatus` bằng `"UP"`.

---

## 🛠️ UC-09.2: Bật / Tắt Chế Độ Bảo Trì Hệ Thống (Dynamic Maintenance Mode)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Toggle giá trị `MAINTENANCE_MODE` trong `SystemSettingRepository`. Khi bằng `true`, `MaintenanceModeFilter` chặn toàn bộ client không phải Admin với HTTP 503 Service Unavailable.
- **Endpoint:** `PUT /api/v1/admin/system/settings/dynamic`

### 📐 2. Class Diagram (UC-09.2)
```mermaid
classDiagram
    class AdminSystemController {
        +updateSetting(UpdateSettingRequestDTO req) ResponseEntity~ApiResponse~
    }
    class MaintenanceModeFilter {
        -SystemSettingRepository settingRepo
        +doFilter(request, response, chain) void
    }
    AdminSystemController --> SystemSettingRepository
    MaintenanceModeFilter --> SystemSettingRepository
```

### 🔄 3. Sequence Diagram (UC-09.2)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminSystemController
    participant DB as MongoDB Atlas
    actor Client as User Browser

    Admin->>Controller: PUT /api/v1/admin/system/settings/dynamic (key = "MAINTENANCE_MODE", value = "true")
    Controller->>DB: save(SystemSetting: key = "MAINTENANCE_MODE", value = "true")
    Controller-->>Admin: 200 OK (Đã bật Chế độ Bảo trì hệ thống)

    note over Client: Client Gửi Request Khi Bảo Trì Đang Bật
    Client->>MaintenanceModeFilter: GET /api/v1/users/me
    MaintenanceModeFilter->>DB: findByKey("MAINTENANCE_MODE")
    DB-->>MaintenanceModeFilter: SystemSetting (value = "true")
    MaintenanceModeFilter-->>Client: 503 Service Unavailable ("Hệ thống đang bảo trì")
```

### 🧪 4. Testing & Verification (UC-09.2)
- **Unit Test Method:** `AdminSystemControllerTest.java` -> `updateSetting_maintenanceMode_updatesState()`
- **Assertions:** Key `MAINTENANCE_MODE` được cập nhật thành công thành `"true"`.

---

## 🛠️ UC-09.3: Tạm Khóa Tài Khoản Người Dùng Có Thời Hạn (Temporary Sanction)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Khóa truy cập tài khoản vi phạm quy chuẩn theo số ngày (1, 3, 7, 30 ngày), thiết lập `isActive = false` và `suspendedUntil = now + N days`.
- **Endpoint:** `PUT /api/v1/admin/users/{id}/suspend-temporary`

### 📐 2. Class Diagram (UC-09.3)
```mermaid
classDiagram
    class AdminController {
        +suspendUserTemporary(String id, SuspendRequestDTO req) ResponseEntity~ApiResponse~
    }
    class AdminService {
        +suspendUserTemporary(String userId, int days, String reason, String adminId) UserResponseDTO
    }
    AdminController --> AdminService
```

### 🔄 3. Sequence Diagram (UC-09.3)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminController
    participant Service as AdminServiceImpl
    participant DB as MongoDB Atlas

    Admin->>Controller: PUT /api/v1/admin/users/{id}/suspend-temporary (days = 7, reason = "Spam")
    Controller->>Service: suspendUserTemporary(userId, 7, "Spam", adminId)
    Service->>DB: findById(userId)
    DB-->>Service: User Record
    
    Service->>Service: Set isActive = false, suspendedUntil = now + 7 days
    Service->>Service: Append to sanctionHistory log
    Service->>DB: save(User)
    DB-->>Service: Saved User
    
    Service-->>Controller: UserResponseDTO
    Controller-->>Admin: 200 OK (Tài khoản bị tạm khóa 7 ngày)
```

### 🧪 4. Testing & Verification (UC-09.3)
- **Unit Test Method:** `AdminControllerTest.java` -> `suspendTemporary_setsCorrectExpiration()`
- **Assertions:** `user.isActive == false`, `user.suspendedUntil` bằng 7 ngày tới.

---

## 🛠️ UC-09.4: Tự Động Mở Khóa Khi Hết Hạn Ban (Auto-Reactivation Scheduler)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** System Scheduler (Cron Job chạy 5 phút/lần).
- **Mục tiêu:** Quét các tài khoản có `isActive = false` và `suspendedUntil < now` để tự động mở khóa truy cập lại.
- **Endpoint:** `Scheduled Task (Every 5 mins)`

### 📐 2. Class Diagram (UC-09.4)
```mermaid
classDiagram
    class UserSanctionScheduler {
        -UserRepository userRepository
        +autoReactivateExpiredBans() void
    }
    UserSanctionScheduler --> UserRepository
```

### 🔄 3. Sequence Diagram (UC-09.4)
```mermaid
sequenceDiagram
    autonumber
    actor Scheduler as Cron Task (Every 5m)
    participant Service as UserSanctionScheduler
    participant DB as MongoDB Atlas

    Scheduler->>Service: Trigger autoReactivateExpiredBans()
    Service->>DB: findByIsActiveFalseAndSuspendedUntilBefore(now)
    DB-->>Service: List<User> expiredUsers
    
    loop Mỗi User Hết Hạn Ban
        Service->>Service: Set isActive = true, suspendedUntil = null
        Service->>DB: save(User)
    end
    
    Service-->>Scheduler: Done (Reactivated N users)
```

### 🧪 4. Testing & Verification (UC-09.4)
- **Unit Test Method:** `UserSanctionSchedulerTest.java` -> `autoReactivateExpiredBans_reactivatesEligibleUsers()`
- **Assertions:** User có `suspendedUntil` quá hạn chuyển sang `isActive = true`.

---

## 🛠️ UC-09.5: Mở Khóa Tài Khoản Thủ Công (Manual Unsuspend)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Mở khóa truy cập ngay lập tức cho user đang bị ban trước khi hết hạn.
- **Endpoint:** `PUT /api/v1/admin/users/{id}/unsuspend`

### 📐 2. Class Diagram (UC-09.5)
```mermaid
classDiagram
    class AdminController {
        +unsuspendUser(String id) ResponseEntity~ApiResponse~
    }
    AdminController --> AdminService
```

### 🔄 3. Sequence Diagram (UC-09.5)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminController
    participant Service as AdminServiceImpl
    participant DB as MongoDB Atlas

    Admin->>Controller: PUT /api/v1/admin/users/{id}/unsuspend
    Controller->>Service: unsuspendUser(userId, adminId)
    Service->>DB: findById(userId)
    DB-->>Service: User Record
    
    Service->>Service: Set isActive = true, suspendedUntil = null
    Service->>DB: save(User)
    Service-->>Controller: UserResponseDTO
    Controller-->>Admin: 200 OK (Mở khóa tài khoản thành công)
```

### 🧪 4. Testing & Verification (UC-09.5)
- **Unit Test Method:** `AdminServiceImplTest.java` -> `unsuspendUser_success()`
- **Assertions:** `user.isActive == true`, `suspendedUntil == null`.

---

## 🛠️ UC-09.6: Xuất File CSV Audit Log (Export Audit Log CSV)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Tải xuống toàn bộ nhật ký an ninh kiểm toán hệ thống dưới dạng file CSV stream.
- **Endpoint:** `GET /api/v1/audit-logs/export-csv`

### 📐 2. Class Diagram (UC-09.6)
```mermaid
classDiagram
    class AuditLogController {
        +exportCsv() ResponseEntity~String~
    }
    AuditLogController --> AuditLogService
```

### 🔄 3. Sequence Diagram (UC-09.6)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AuditLogController
    participant DB as MongoDB Atlas

    Admin->>Controller: GET /api/v1/audit-logs/export-csv
    Controller->>DB: findAllAuditLogs()
    DB-->>Controller: List<AuditLog>
    Controller->>Controller: Format CSV header & escape double quotes
    Controller-->>Admin: 200 OK (Content-Type: text/csv, Attachment "audit-logs.csv")
```

### 🧪 4. Testing & Verification (UC-09.6)
- **Unit Test Method:** `AuditLogControllerTest.java` -> `exportCsv_returnsValidCsvStream()`
- **Assertions:** Header chứa `ID,CreatedAt,UserId,Action,Resource`, Content-Type là `text/csv`.

---

## 🛠️ UC-09.7: Hoàn Tiền Giao Dịch Nâng Cấp VIP (Refund Transaction)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Đánh dấu đơn hàng giao dịch thành `REFUNDED`, lưu lý do đền bù và số tiền hoàn trả.
- **Endpoint:** `POST /api/v1/admin/transactions/{id}/refund`

### 📐 2. Class Diagram (UC-09.7)
```mermaid
classDiagram
    class AdminController {
        +refundTransaction(String id, RefundRequestDTO req) ResponseEntity~ApiResponse~
    }
    AdminController --> AdminService
```

### 🔄 3. Sequence Diagram (UC-09.7)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminController
    participant Service as AdminServiceImpl
    participant DB as MongoDB Atlas

    Admin->>Controller: POST /api/v1/admin/transactions/{id}/refund (reason = "Lỗi cổng PayOS")
    Controller->>Service: refundTransaction(txId, reason, adminId)
    Service->>DB: findTransactionById(txId)
    DB-->>Service: PaymentTransaction Record
    
    Service->>Service: Set status = REFUNDED, refundedAmount = amount, refundReason = reason
    Service->>DB: save(PaymentTransaction)
    Service-->>Controller: Map Result
    Controller-->>Admin: 200 OK (Đã hoàn tiền đơn hàng)
```

### 🧪 4. Testing & Verification (UC-09.7)
- **Unit Test Method:** `AdminServiceImplTest.java` -> `refundTransaction_success()`
- **Assertions:** Status chuyển sang `REFUNDED`, `refundReason` lưu đúng lý do.

---

## 🛠️ UC-09.8: Cấp Tặng Gói VIP Thủ Công (Manual Grant VIP Plan)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Cộng thêm số ngày VIP (BASIC/FULL/ANNUAL) thủ công cho học viên kèm lý do đền bù.
- **Endpoint:** `POST /api/v1/admin/transactions/manual-grant`

### 📐 2. Class Diagram (UC-09.8)
```mermaid
classDiagram
    class AdminController {
        +manualGrantPlan(ManualGrantRequestDTO req) ResponseEntity~ApiResponse~
    }
    AdminController --> AdminService
```

### 🔄 3. Sequence Diagram (UC-09.8)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminController
    participant Service as AdminServiceImpl
    participant DB as MongoDB Atlas

    Admin->>Controller: POST /api/v1/admin/transactions/manual-grant (userId, plan = "FULL", days = 30)
    Controller->>Service: manualGrantPlan(userId, "FULL", 30, reason, adminId)
    Service->>DB: findUserById(userId)
    DB-->>Service: User Record
    
    Service->>Service: Set plan = FULL, isPremium = true, planExpiresAt += 30 days
    Service->>DB: save(User)
    Service-->>Controller: UserResponseDTO
    Controller-->>Admin: 200 OK (Đã nâng cấp 30 ngày VIP FULL cho user)
```

### 🧪 4. Testing & Verification (UC-09.8)
- **Unit Test Method:** `AdminServiceImplTest.java` -> `manualGrantPlan_success()`
- **Assertions:** `user.plan` chuyển thành `FULL`, `planExpiresAt` được gia hạn thêm 30 ngày.

---

## 🛠️ UC-09.9: Duyệt & Xử Lý Báo Cáo Vi Phạm Hàng Loạt (Bulk Resolve Reports)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Duyệt (Resolve) hoặc Bỏ qua (Dismiss) nhiều báo cáo vi phạm cùng lúc.
- **Endpoint:** `PUT /api/v1/reports/bulk-resolve`

### 📐 2. Class Diagram (UC-09.9)
```mermaid
classDiagram
    class ReportController {
        +bulkResolve(BulkResolveRequestDTO req) ResponseEntity~ApiResponse~
    }
    ReportController --> ReportRepository
```

### 🔄 3. Sequence Diagram (UC-09.9)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as ReportController
    participant DB as MongoDB Atlas

    Admin->>Controller: PUT /api/v1/reports/bulk-resolve (reportIds = [id1, id2], status = "RESOLVED")
    Controller->>DB: updateStatusForReportIds(reportIds, "RESOLVED")
    DB-->>Controller: Modified Count = 2
    Controller-->>Admin: 200 OK (Đã duyệt thành công 2 báo cáo vi phạm)
```

### 🧪 4. Testing & Verification (UC-09.9)
- **Unit Test Method:** `ReportControllerTest.java` -> `bulkResolve_validIds_updatesStatus()`
- **Assertions:** Tất cả report ID trong danh sách chuyển sang trạng thái `RESOLVED`.
