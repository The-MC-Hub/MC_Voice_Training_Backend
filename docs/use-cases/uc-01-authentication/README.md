# UC-01 — Xác Thực & Quản Lý Tài Khoản (Authentication) Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Xác thực và Quản lý tài khoản.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-01.1-register.md](UC-01.1-register.md) | Đăng ký tài khoản mới | `POST /api/v1/auth/register` | Guest |
| [UC-01.2-verify-otp.md](UC-01.2-verify-otp.md) | Xác minh mã OTP kích hoạt | `POST /api/v1/auth/verify-otp` | Guest |
| [UC-01.3-login-email.md](UC-01.3-login-email.md) | Đăng nhập Email / Password | `POST /api/v1/auth/login` | User / Admin |
| [UC-01.4-login-admin-2fa.md](UC-01.4-login-admin-2fa.md) | Xác minh 2FA Admin | `POST /api/v1/auth/verify-admin-login-otp` | Admin |
| [UC-01.5-login-google.md](UC-01.5-login-google.md) | Đăng nhập Google OAuth | `POST /api/v1/auth/google` | Guest / User |
| [UC-01.6-password-reset.md](UC-01.6-password-reset.md) | Quên & Đặt lại mật khẩu | `POST /api/v1/auth/reset-password` | User |
