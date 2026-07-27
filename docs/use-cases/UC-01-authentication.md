# UC-01 — Xác Thực & Quản Lý Tài Khoản (Authentication & Account Security)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Xác thực và Quản lý tài khoản.

---

## 🔑 UC-01.1: Đăng Ký Tài Khoản (User Registration)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest (Người dùng chưa đăng nhập).
- **Mục tiêu:** Tạo tài khoản người dùng mới ở trạng thái chờ kích hoạt (`isActive=false`) và tạo mã OTP xác minh gửi về email.
- **Tiền điều kiện (Pre-conditions):** Email chưa tồn tại trong hệ thống (`userRepository.existsByEmail = false`).
- **Hậu điều kiện (Post-conditions):** Bản ghi User được tạo với mật khẩu mã hóa BCrypt, bản ghi `OtpVerification` được lưu với hạn sử dụng 5 phút, email chứa mã 6 chữ số được gửi qua Brevo SMTP.
- **Endpoint:** `POST /api/v1/auth/register`

### 📐 2. Class Diagram (UC-01.1)
```mermaid
classDiagram
    class AuthController {
        +register(RegisterRequestDTO req) ResponseEntity~ApiResponse~
    }
    class AuthService {
        <<interface>>
        +registerUser(RegisterRequestDTO req) UserResponseDTO
    }
    class AuthServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -OtpVerificationRepository otpRepo
        -EmailService emailService
    }
    class RegisterRequestDTO {
        +String name
        +String email
        +String password
        +UserRole role
    }
    class User {
        +String id
        +String email
        +String password
        +boolean isActive
        +UserRole role
    }
    class OtpVerification {
        +String id
        +String email
        +String otpCode
        +LocalDateTime expiresAt
    }
    AuthController --> AuthService
    AuthServiceImpl ..|> AuthService
    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> OtpVerificationRepository
    AuthServiceImpl --> EmailService
    UserRepository --> User
    OtpVerificationRepository --> OtpVerification
```

### 🔄 3. Sequence Diagram (UC-01.1)
```mermaid
sequenceDiagram
    autonumber
    actor Guest as Guest User
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant Encrypter as BCryptPasswordEncoder
    participant DB as MongoDB Atlas
    participant Email as Brevo Email Service

    Guest->>Controller: POST /api/v1/auth/register (name, email, password)
    Controller->>Service: registerUser(req)
    Service->>DB: existsByEmail(email)
    DB-->>Service: false (Hợp lệ)
    
    Service->>Encrypter: encode(password)
    Encrypter-->>Service: hashedBCryptPassword
    
    Service->>DB: save(User: isActive=false, password=hashedBCryptPassword)
    DB-->>Service: User Record Created
    
    Service->>Service: Generate 6-digit OTP code (ex: 849201)
    Service->>DB: save(OtpVerification: expiresAt = now + 5 mins)
    DB-->>Service: OtpRecord Saved
    
    Service->>Email: sendOtpEmail(email, otpCode)
    Email-->>Service: SMTP Sent Status 200
    
    Service-->>Controller: UserResponseDTO
    Controller-->>Guest: 201 Created (Đăng ký thành công, vui lòng kiểm tra Email nhận OTP)
```

### 🧪 4. Testing & Verification (UC-01.1)
- **Unit Test Method:** `AuthServiceImplTest.java` -> `register_success()`
- **Assertions:** 
  - `userRepository.save()` được gọi 1 lần.
  - `passwordEncoder.encode()` được thực thi (Mật khẩu không lưu dạng plain text).
  - `emailService.sendOtpEmail()` được gọi đúng tham số email.

---

## 🔑 UC-01.2: Xác Minh Mã OTP (OTP Verification)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest (Người dùng đã đăng ký nhưng chưa kích hoạt).
- **Mục tiêu:** Kiểm tra mã OTP do user nhập, kích hoạt tài khoản `isActive=true` và phát hành JWT Access Token.
- **Rules:** Mã OTP phải khớp exact match và chưa quá mốc `expiresAt`. Nếu hết hạn, trả về `INVALID_OTP`.
- **Endpoint:** `POST /api/v1/auth/verify-otp`

### 📐 2. Class Diagram (UC-01.2)
```mermaid
classDiagram
    class AuthController {
        +verifyOtp(VerifyOtpRequestDTO req) ResponseEntity~ApiResponse~
    }
    class AuthService {
        <<interface>>
        +verifyOtp(String email, String otp) AuthResponseDTO
    }
    class AuthServiceImpl {
        -UserRepository userRepository
        -OtpVerificationRepository otpRepo
        -JwtService jwtService
    }
    class VerifyOtpRequestDTO {
        +String email
        +String otpCode
    }
    class AuthResponseDTO {
        +String accessToken
        +String refreshToken
        +UserResponseDTO user
    }
    AuthController --> AuthService
    AuthServiceImpl ..|> AuthService
    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> OtpVerificationRepository
    AuthServiceImpl --> JwtService
```

### 🔄 3. Sequence Diagram (UC-01.2)
```mermaid
sequenceDiagram
    autonumber
    actor Guest as User
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant DB as MongoDB Atlas
    participant JWT as JwtService

    Guest->>Controller: POST /api/v1/auth/verify-otp (email, otpCode)
    Controller->>Service: verifyOtp(email, otpCode)
    Service->>DB: findTopByEmailOrderByExpiresAtDesc(email)
    DB-->>Service: OtpVerification Record
    
    alt OTP Không Khớp Hoặc Đã Hết Hạn
        Service-->>Controller: AppException(INVALID_OTP / OTP_EXPIRED)
        Controller-->>Guest: 400 Bad Request
    else OTP Hợp Lệ & Còn Hạn
        Service->>DB: findByEmail(email)
        DB-->>Service: User Record
        Service->>Service: User.setActive(true)
        Service->>DB: save(User)
        Service->>DB: delete(OtpVerification)
        
        Service->>JWT: generateToken(User)
        JWT-->>Service: JWT Access Token String
        Service-->>Controller: AuthResponseDTO (accessToken, user)
        Controller-->>Guest: 200 OK (Kích hoạt thành công & Đăng nhập)
    end
```

### 🧪 4. Testing & Verification (UC-01.2)
- **Unit Test Method:** `AuthServiceImplTest.java` -> `verifyOtp_validOtp_activatesUserAndReturnsToken()`
- **Assertions:** `user.isActive` chuyển sang `true`, token JWT được sinh ra và không null.

---

## 🔑 UC-01.3: Đăng Nhập Email / Password (Email Login)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Client / MC / Admin.
- **Mục tiêu:** Xác thực thông tin đăng nhập và cấp JWT Token `Bearer`.
- **Rules:** Nếu nhập sai quá 5 lần (`failedLoginAttempts > 5`), tự động khóa tài khoản trong 15 phút (`lockedUntil = now + 15m`).
- **Endpoint:** `POST /api/v1/auth/login`

### 📐 2. Class Diagram (UC-01.3)
```mermaid
classDiagram
    class AuthController {
        +login(LoginRequestDTO req) ResponseEntity~ApiResponse~
    }
    class AuthService {
        <<interface>>
        +login(LoginRequestDTO req) AuthResponseDTO
    }
    class AuthServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtService jwtService
    }
    class LoginRequestDTO {
        +String email
        +String password
    }
    AuthController --> AuthService
    AuthServiceImpl ..|> AuthService
    AuthServiceImpl --> UserRepository
    AuthServiceImpl --> JwtService
```

### 🔄 3. Sequence Diagram (UC-01.3)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant Encoder as PasswordEncoder
    participant DB as MongoDB Atlas
    participant JWT as JwtService

    User->>Controller: POST /api/v1/auth/login (email, password)
    Controller->>Service: login(req)
    Service->>DB: findByEmail(email)
    DB-->>Service: User Record
    
    alt Tài Khoản Đang Bị Khóa
        Service-->>Controller: AppException(ACCOUNT_LOCKED)
        Controller-->>User: 403 Forbidden
    else Tài Khoản Khả Dụng
        Service->>Encoder: matches(rawPassword, encodedPassword)
        alt Sai Mật Khẩu
            Service->>Service: Tăng failedLoginAttempts++
            Service->>DB: save(User)
            Service-->>Controller: AppException(INVALID_CREDENTIALS)
            Controller-->>User: 400 Bad Request
        else Đúng Mật Khẩu
            Service->>Service: Reset failedLoginAttempts = 0
            Service->>DB: save(User)
            Service->>JWT: generateToken(User)
            JWT-->>Service: Bearer JWT Token
            Service-->>Controller: AuthResponseDTO
            Controller-->>User: 200 OK (Trả về Bearer Token)
        end
    end
```

### 🧪 4. Testing & Verification (UC-01.3)
- **Unit Test Method:** `AuthControllerTest.java` -> `login_validCredentials_returnsToken()`
- **Assertions:** Status Code 200, Token dạng Bearer String hợp lệ.

---

## 🔑 UC-01.4: Đăng Nhập 2FA Cho Admin (Admin 2FA Verification)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Yêu cầu Admin nhập mã OTP 2FA gửi về email Admin trước khi cấp quyền Admin Token.
- **Endpoint:** `POST /api/v1/auth/verify-admin-login-otp`

### 📐 2. Class Diagram (UC-01.4)
```mermaid
classDiagram
    class AuthController {
        +verifyAdminLoginOtp(VerifyAdminOtpRequestDTO req) ResponseEntity~ApiResponse~
    }
    class AuthService {
        <<interface>>
        +verifyAdminOtp(String email, String otpCode) AuthResponseDTO
    }
    class AdminOtpVerification {
        +String email
        +String otpCode
        +LocalDateTime expiresAt
    }
    AuthController --> AuthService
    AuthService --> AdminOtpVerification
```

### 🔄 3. Sequence Diagram (UC-01.4)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant DB as MongoDB Atlas
    participant JWT as JwtService

    Admin->>Controller: POST /api/v1/auth/verify-admin-login-otp (email, otpCode)
    Controller->>Service: verifyAdminOtp(email, otpCode)
    Service->>DB: findAdminOtp(email)
    DB-->>Service: AdminOtpRecord
    
    alt OTP 2FA Không Hợp Lệ
        Service-->>Controller: AppException(INVALID_2FA_CODE)
        Controller-->>Admin: 400 Bad Request
    else OTP 2FA Đúng
        Service->>JWT: generateAdminToken(AdminUser)
        JWT-->>Service: Admin Bearer Token
        Service-->>Controller: AuthResponseDTO
        Controller-->>Admin: 200 OK (Cấp Token Admin Quyền Cao Nhất)
    end
```

### 🧪 4. Testing & Verification (UC-01.4)
- **Unit Test Method:** `AuthControllerTest.java` -> `admin2FA_validOtp_returnsAdminToken()`
- **Assertions:** Claim `role` trong JWT token chứa `ROLE_ADMIN`.

---

## 🔑 UC-01.5: Đăng Nhập Qua Google OAuth 2.0 (Google Social Auth)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest / User.
- **Mục tiêu:** Đăng nhập hoặc đăng ký tài khoản tức thì bằng Google ID Token.
- **Endpoint:** `POST /api/v1/auth/google`

### 📐 2. Class Diagram (UC-01.5)
```mermaid
classDiagram
    class AuthController {
        +googleAuth(GoogleAuthRequestDTO req) ResponseEntity~ApiResponse~
    }
    class GoogleAuthService {
        -GoogleIdTokenVerifier verifier
        +verifyAndLogin(String idToken) AuthResponseDTO
    }
    AuthController --> GoogleAuthService
```

### 🔄 3. Sequence Diagram (UC-01.5)
```mermaid
sequenceDiagram
    autonumber
    actor User as User / Mobile App
    participant Controller as AuthController
    participant Service as GoogleAuthService
    participant GoogleAPI as Google OAuth API
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/auth/google (idToken)
    Controller->>Service: verifyAndLogin(idToken)
    Service->>GoogleAPI: verify(idToken)
    GoogleAPI-->>Service: GooglePayload (email, name, picture, googleId)
    
    Service->>DB: findByEmail(email)
    alt Chưa có tài khoản
        Service->>DB: save(New User: googleId, isActive=true)
    else Đã có tài khoản
        Service->>DB: update(googleId)
    end
    
    Service-->>Controller: AuthResponseDTO (JWT)
    Controller-->>User: 200 OK (Đăng nhập Google thành công)
```

### 🧪 4. Testing & Verification (UC-01.5)
- **Unit Test Method:** `AuthServiceImplTest.java` -> `googleAuth_validToken_createsUserAndReturnsJwt()`
- **Assertions:** `user.isGoogleLinked = true`, trả về JWT Token hợp lệ.

---

## 🔑 UC-01.6: Quên & Đặt Lại Mật Khẩu (Password Reset Flow)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Khôi phục mật khẩu khi quên bằng cách gửi token mã hóa qua Email.
- **Endpoint:** `POST /api/v1/auth/forgot-password`, `POST /api/v1/auth/reset-password`

### 📐 2. Class Diagram (UC-01.6)
```mermaid
classDiagram
    class AuthController {
        +forgotPassword(ForgotPasswordDTO req) ResponseEntity~ApiResponse~
        +resetPassword(ResetPasswordDTO req) ResponseEntity~ApiResponse~
    }
    class PasswordResetToken {
        +String email
        +String resetToken
        +LocalDateTime expiresAt
    }
    AuthController --> PasswordResetToken
```

### 🔄 3. Sequence Diagram (UC-01.6)
```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant Controller as AuthController
    participant Service as AuthServiceImpl
    participant DB as MongoDB Atlas
    participant Email as EmailService

    User->>Controller: POST /api/v1/auth/forgot-password (email)
    Controller->>Service: sendResetToken(email)
    Service->>DB: findByEmail(email)
    Service->>Service: Tạo Secure UUID Reset Token (Hạn 15 phút)
    Service->>DB: save(PasswordResetToken)
    Service->>Email: sendResetEmail(email, resetToken)
    Controller-->>User: 200 OK (Đã gửi link reset mật khẩu)

    note over User: User Nhấn Link & Nhập Password Mới
    User->>Controller: POST /api/v1/auth/reset-password (resetToken, newPassword)
    Controller->>Service: resetPassword(resetToken, newPassword)
    Service->>DB: findByResetToken(resetToken)
    
    alt Token Không Tồn Tại Hoặc Hết Hạn
        Controller-->>User: 400 Bad Request (INVALID_RESET_TOKEN)
    else Token Hợp Lệ
        Service->>DB: Update User (password = BCrypt(newPassword), passwordChangedAt = now)
        Service->>DB: delete(PasswordResetToken)
        Controller-->>User: 200 OK (Đặt lại mật khẩu thành công)
    end
```

### 🧪 4. Testing & Verification (UC-01.6)
- **Unit Test Method:** `AuthServiceImplTest.java` -> `resetPassword_validToken_updatesPassword()`
- **Assertions:** Mật khẩu mới được mã hóa BCrypt, `passwordChangedAt` được cập nhật timestamp hiện tại.
