# UC-02 — Hồ Sơ Người Dùng & MC Profile (User & MC Profile)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Quản lý thông tin hồ sơ cá nhân và MC Talent Profile.

---

## 👤 UC-02.1: Xem Thông Tin Hồ Sơ Cá Nhân (Get User Profile)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User (Client/MC/Admin đã đăng nhập).
- **Mục tiêu:** Trả về thông tin cá nhân đầy đủ bao gồm số AI sessions đã dùng, gói VIP hiện tại (`plan`), thời hạn VIP (`planExpiresAt`), số bài tập đã hoàn thành và trạng thái xác minh.
- **Endpoint:** `GET /api/v1/users/me`

### 📐 2. Class Diagram (UC-02.1)
```mermaid
classDiagram
    class UserController {
        +getMe() ResponseEntity~ApiResponse~
    }
    class UserService {
        <<interface>>
        +getUserProfile(String userId) UserResponseDTO
    }
    class UserServiceImpl {
        -UserRepository userRepository
        -UserMapper userMapper
    }
    class UserResponseDTO {
        +String id
        +String email
        +String name
        +SubscriptionPlan plan
        +LocalDateTime planExpiresAt
        +int aiSessionsUsed
    }
    UserController --> UserService
    UserServiceImpl ..|> UserService
    UserServiceImpl --> UserRepository
    UserServiceImpl --> UserMapper
```

### 🔄 3. Sequence Diagram (UC-02.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as UserController
    participant Service as UserServiceImpl
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/users/me (Bearer Token)
    Controller->>Service: getUserProfile(currentUserId)
    Service->>DB: findById(currentUserId)
    DB-->>Service: User Record
    
    alt User Không Tồn Tại
        Service-->>Controller: AppException(USER_NOT_FOUND)
        Controller-->>User: 404 Not Found
    else Hợp lệ
        Service->>Service: userMapper.toResponseDTO(user)
        Service-->>Controller: UserResponseDTO
        Controller-->>User: 200 OK (Thông tin cá nhân & hạn VIP)
    end
```

### 🧪 4. Testing & Verification (UC-02.1)
- **Unit Test Method:** `UserServiceImplTest.java` -> `getUserProfile_validId_returnsDto()`
- **Assertions:** Trả về `UserResponseDTO` không null, thông tin `plan` khớp với database.

---

## 👤 UC-02.2: Cập Nhật Thông Tin Profile (Update Profile)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Cập nhật các trường thông tin cá nhân: `name`, `phoneNumber`, `bio`, `avatarUrl`.
- **Rules:** Tên không được rỗng, số điện thoại đúng format 10 chữ số.
- **Endpoint:** `PUT /api/v1/users/profile`

### 📐 2. Class Diagram (UC-02.2)
```mermaid
classDiagram
    class UserController {
        +updateProfile(UpdateProfileRequestDTO req) ResponseEntity~ApiResponse~
    }
    class UpdateProfileRequestDTO {
        +String name
        +String phoneNumber
        +String bio
        +String avatarUrl
    }
    UserController --> UpdateProfileRequestDTO
```

### 🔄 3. Sequence Diagram (UC-02.2)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as UserController
    participant Service as UserServiceImpl
    participant DB as MongoDB Atlas

    User->>Controller: PUT /api/v1/users/profile (name, phoneNumber, bio, avatarUrl)
    Controller->>Service: updateProfile(currentUserId, req)
    Service->>DB: findById(currentUserId)
    DB-->>Service: User Record
    
    Service->>Service: Set new values (name, phoneNumber, bio, avatarUrl)
    Service->>DB: save(User)
    DB-->>Service: Saved User
    
    Service-->>Controller: UserResponseDTO
    Controller-->>User: 200 OK (Cập nhật hồ sơ thành công)
```

### 🧪 4. Testing & Verification (UC-02.2)
- **Unit Test Method:** `UserServiceImplTest.java` -> `updateProfile_validData_updatesFields()`
- **Assertions:** `user.getName()` và `user.getBio()` được thay đổi thành công trong DB.

---

## 👤 UC-02.3: Xem Chuỗi Ngày Luyện Tập Streak (Get Streak Info)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Lấy chỉ số chuỗi ngày luyện tập liên tục (`currentStreak`), kỷ lục chuỗi ngày dài nhất (`longestStreak`), và trạng thái vật phẩm đóng băng streak (`streakFreezeAvailable`).
- **Endpoint:** `GET /api/v1/users/streak`

### 📐 2. Class Diagram (UC-02.3)
```mermaid
classDiagram
    class UserController {
        +getStreakInfo() ResponseEntity~ApiResponse~
    }
    class UserStats {
        +String userId
        +int currentStreak
        +int longestStreak
        +boolean streakFreezeAvailable
        +LocalDateTime lastPracticeAt
    }
    UserController --> UserStatsRepository
    UserStatsRepository --> UserStats
```

### 🔄 3. Sequence Diagram (UC-02.3)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as UserController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/users/streak
    Controller->>DB: findUserStatsByUserId(currentUserId)
    DB-->>Controller: UserStats Record
    Controller-->>User: 200 OK (CurrentStreak, LongestStreak, FreezeAvailable)
```

### 🧪 4. Testing & Verification (UC-02.3)
- **Unit Test Method:** `UserControllerTest.java` -> `getStreak_returnsUserStreakInfo()`
- **Assertions:** Trả về số ngày streak hợp lệ >= 0.

---

## 👤 UC-02.4: Đóng Băng Chuỗi Ngày Streak (Use Streak Freeze)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Đóng băng streak khi user không học trong 1 ngày để không bị mất chuỗi `currentStreak`.
- **Rules:** Phải có `streakFreezeAvailable = true`. Mỗi lần sử dụng sẽ trừ 1 lượt freeze.
- **Endpoint:** `POST /api/v1/users/streak/freeze`

### 📐 2. Class Diagram (UC-02.4)
```mermaid
classDiagram
    class UserController {
        +useStreakFreeze() ResponseEntity~ApiResponse~
    }
    class UserService {
        +useStreakFreeze(String userId) StreakDTO
    }
    UserController --> UserService
```

### 🔄 3. Sequence Diagram (UC-02.4)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as UserController
    participant Service as UserServiceImpl
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/users/streak/freeze
    Controller->>Service: useStreakFreeze(currentUserId)
    Service->>DB: findUserStatsByUserId(currentUserId)
    DB-->>Service: UserStats Record
    
    alt Hết lượt Freeze hoặc Đã Đóng Băng
        Service-->>Controller: AppException(STREAK_FREEZE_UNAVAILABLE)
        Controller-->>User: 400 Bad Request
    else Còn lượt Freeze
        Service->>Service: Set streakFreezeAvailable = false
        Service->>DB: save(UserStats)
        DB-->>Service: Saved Record
        Service-->>Controller: StreakDTO
        Controller-->>User: 200 OK (Đã đóng băng chuỗi ngày streak)
    end
```

### 🧪 4. Testing & Verification (UC-02.4)
- **Unit Test Method:** `UserServiceImplTest.java` -> `useStreakFreeze_success()`
- **Assertions:** `streakFreezeAvailable` chuyển sang `false`, streak không bị ngắt.

---

## 👤 UC-02.5: Xem Hồ Sơ MC Công Khai (Public MC Talent Profile)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Visitor / Public / Client.
- **Mục tiêu:** Hiển thị trang Profile truyền thông công khai của MC Talent (Kinh nghiệm, loại hình sự kiện, mức giá tham khảo, audio mẫu và đánh giá từ khách hàng).
- **Endpoint:** `GET /api/v1/mcs/{id}/public`

### 📐 2. Class Diagram (UC-02.5)
```mermaid
classDiagram
    class MCController {
        +getPublicMCProfile(String id) ResponseEntity~ApiResponse~
    }
    class MCProfileResponseDTO {
        +String id
        +String stageName
        +List~String~ eventTypes
        +int startingPrice
        +double avgRating
        +List~ReviewDTO~ reviews
    }
    MCController --> MCProfileResponseDTO
```

### 🔄 3. Sequence Diagram (UC-02.5)
```mermaid
sequenceDiagram
    autonumber
    actor Visitor as Guest / Client
    participant Controller as MCController
    participant DB as MongoDB Atlas

    Visitor->>Controller: GET /api/v1/mcs/{id}/public
    Controller->>DB: findMcProfileByUserId(id)
    DB-->>Controller: MC Profile & Reviews
    
    alt MC Không Tồn Tại Hoặc Chưa Xác Minh
        Controller-->>Visitor: 404 Not Found
    else Hợp Lệ
        Controller-->>Visitor: 200 OK (MC Profile, Rating, Audio Demos)
    end
```

### 🧪 4. Testing & Verification (UC-02.5)
- **Unit Test Method:** `MCControllerTest.java` -> `getPublicProfile_validId_returnsMcDetails()`
- **Assertions:** Status Code 200, thông tin giá dịch vụ và rating hiển thị đúng.
