# UC-01 — Xác thực & Tài khoản (Authentication)

Luồng đăng ký, đăng nhập, xác minh email, khôi phục mật khẩu.

| # | Tính năng | Mô tả |
|---|---|---|
| 1 | Đăng ký tài khoản | User tạo tài khoản mới bằng email/mật khẩu, hệ thống gửi mã OTP xác minh qua email |
| 2 | Xác minh email qua link | User xác minh email bằng link được gửi trong email, trả về token đăng nhập |
| 3 | Xác minh email qua OTP | User nhập mã OTP để xác minh email, tự động đăng nhập sau khi xác minh thành công |
| 4 | Gửi lại OTP | User yêu cầu gửi lại mã OTP xác minh nếu chưa nhận được hoặc hết hạn |
| 5 | Đăng nhập | User đăng nhập bằng email/mật khẩu, nhận JWT token |
| 6 | Xác minh OTP đăng nhập Admin | Admin phải xác minh thêm mã OTP sau khi đăng nhập để tăng bảo mật |
| 7 | Quên mật khẩu | User yêu cầu gửi mã đặt lại mật khẩu qua email |
| 8 | Đặt lại mật khẩu | User đặt mật khẩu mới bằng mã đã nhận qua email |
| 9 | Xem thông tin tài khoản hiện tại | User xem thông tin profile của chính mình đang đăng nhập |
| 10 | Tạo mã giới thiệu (referral code) | User tạo hoặc lấy mã giới thiệu cá nhân để mời bạn bè |
| 11 | Cập nhật cài đặt tài khoản | User cập nhật các thiết lập tài khoản cá nhân (ngôn ngữ, thông báo, v.v.) |
