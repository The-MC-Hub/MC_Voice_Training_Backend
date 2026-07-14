# MC Hub Voice Training — Use Case Documentation

Tài liệu liệt kê toàn bộ tính năng (117 endpoints / 24 controllers) theo luồng nghiệp vụ, dựa trên đọc trực tiếp các Controller trong `src/main/java/com/mchub/controllers/`.

## Danh sách file theo luồng

| File | Luồng |
|---|---|
| [UC-01-authentication.md](UC-01-authentication.md) | Đăng ký, đăng nhập, xác minh email, khôi phục mật khẩu |
| [UC-02-user-profile.md](UC-02-user-profile.md) | Hồ sơ user, streak, hồ sơ MC, chứng chỉ |
| [UC-03-voice-training.md](UC-03-voice-training.md) | Luyện giọng AI — luồng chính sản phẩm |
| [UC-04-courses-learning.md](UC-04-courses-learning.md) | Khoá học, lộ trình học, quiz, chứng chỉ hoàn thành |
| [UC-05-community-leaderboard.md](UC-05-community-leaderboard.md) | Cộng đồng, bảng xếp hạng, giải đấu, bài đăng mạng xã hội |
| [UC-06-payment-subscription.md](UC-06-payment-subscription.md) | Gói cước, thanh toán PayOS, mã giảm giá, voucher |
| [UC-07-onboarding-quest.md](UC-07-onboarding-quest.md) | Quest onboarding cho người dùng mới |
| [UC-08-support-public.md](UC-08-support-public.md) | Trang công khai, liên hệ, báo cáo vi phạm, upload media |
| [UC-09-admin-dashboard.md](UC-09-admin-dashboard.md) | Quản trị hệ thống, người dùng, log, audit |
| [UC-10-marketing-communication.md](UC-10-marketing-communication.md) | Thông báo, email campaign, mẫu email |

## Quy ước

- Mỗi dòng = 1 tính năng độc lập ánh xạ 1:1 với 1 API endpoint trong controller tương ứng.
- Không mô tả kỹ thuật (HTTP method, path) — chỉ tên và mục đích nghiệp vụ.
- Nhóm theo actor: User thường, MC, Admin, Guest (khách chưa đăng nhập), System (tự động).
