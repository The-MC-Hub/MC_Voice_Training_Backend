# MC Hub Voice Training — Central Use Case Documentation

Tài liệu thiết kế chi tiết 100% toàn bộ các tính năng nghiệp vụ của hệ thống (phủ đầy đủ 100% tất cả 127 API Endpoints & 42 REST Controllers trong `src/main/java/com/mchub/controllers/`), được mô đun hóa theo 18 thư mục và 80 file `.md` độc lập cho từng tính năng nhỏ (Sub-UC).

---

##  Danh Sách Cấu Trúc Các Thư Mục Use Case (80 Sub-UC Files)

| Thư Mục / Domain | Mô Tả Luồng Nghiệp Vụ | Số Lượng File Sub-UC |
|---|---|:---:|
|  [uc-01-authentication/](uc-01-authentication/README.md) | Đăng ký, Đăng nhập, OTP, Admin 2FA, Google OAuth, Reset Password | 6 Files |
|  [uc-02-user-profile/](uc-02-user-profile/README.md) | Hồ sơ cá nhân, Update Bio/Avatar, Streak, Freeze Streak, Public MC, Client Profile | 6 Files |
|  [uc-03-voice-training/](uc-03-voice-training/README.md) | Kho bài đọc, Chấm điểm AI, Guest Cooldown, History, Audio TTS, Admin Lesson CRUD | 6 Files |
|  [uc-04-courses-learning/](uc-04-courses-learning/README.md) | Danh mục khóa học, Enroll, Tiến độ, Lesson Complete, Quiz & Cert, Admin Course CRUD | 6 Files |
|  [uc-05-community-leaderboard/](uc-05-community-leaderboard/README.md) | Stats, Bảng xếp hạng, Tra cứu My Rank, Voice Arena, Social Posts, Admin Competition CRUD | 6 Files |
|  [uc-06-payment-subscription/](uc-06-payment-subscription/README.md) | Gói VIP, Link PayOS, HMAC Webhook, Status, Voucher, Lịch sử, Voucher Wallet | 7 Files |
|  [uc-07-onboarding-quest/](uc-07-onboarding-quest/README.md) | Quests tân thủ & Claim phần thưởng XP / Freeze Streak | 2 Files |
|  [uc-08-support-public/](uc-08-support-public/README.md) | Landing Metrics, Contact Form, Submit Report, Cloudinary Upload | 4 Files |
|  [uc-09-admin-dashboard/](uc-09-admin-dashboard/README.md) | System Health, Maintenance Mode, Temp Ban, Auto Unban, Export CSV, Refund, Manual VIP, Bulk Resolve, Server Logs | 10 Files |
|  [uc-10-marketing-communication/](uc-10-marketing-communication/README.md) | Brevo Email Campaign, Test Email, Segmented Push, Daily Streak Reminders | 4 Files |
|  [uc-11-mc-booking-hiring/](uc-11-mc-booking-hiring/README.md) | Booking Request, MC Quote, PayOS Payment, Cancel, Review MC, Admin Force Cancel | 6 Files |
|  [uc-12-chat-messaging/](uc-12-chat-messaging/README.md) | Create Conversation, Recent Chats, History, STOMP WS Realtime (`/ws-chat`), Mark Read, Unread Count | 6 Files |
|  [uc-13-peer-review/](uc-13-peer-review/README.md) | Review Request, Pending List, Rating & Feedback, My Feedback, Admin Delete Review | 5 Files |
|  [uc-14-announcement-banner/](uc-14-announcement-banner/README.md) | Thông báo hệ thống, Banner khuyến mãi, Quản lý banner Admin CRUD | 2 Files |
|  [uc-15-cv-portfolio/](uc-15-cv-portfolio/README.md) | CV Builder truyền thông, Case Study dự án sự kiện, Voice Highlights | 3 Files |
|  [uc-16-gamification-minigame/](uc-16-gamification-minigame/README.md) | Thách đấu Voice Minigame ngày, Bookmark MC yêu thích, Admin FlashDeal CRUD | 3 Files |
|  [uc-17-mc-availability-calendar/](uc-17-mc-availability-calendar/README.md) | Lịch bận / rảnh nhận show MC, Xác minh chứng chỉ qua mã, Notification đẩy cá nhân | 3 Files |
|  [uc-18-admin-content-moderation/](uc-18-admin-content-moderation/README.md) | Bài viết truyền thông Admin CRUD, Kiểm duyệt bài cộng đồng, Chi tiết hợp đồng booking | 3 Files |

---

##  Quy Ước Cấu Trúc Mỗi File Sub-UC (`.md`):

1. ** 1. Bảng Mô Tả Nghiệp Vụ (Business Description Table)**:
   - Mã UC, Tên Tính Năng, Actor, Mục Tiêu, Endpoint, Tiền Điều Kiện, Hậu Điều Kiện, Quy Tắc Nghiệp Vụ, Mã Lỗi ErrorCodes.
2. ** 2. Class Diagram (Mermaid `classDiagram`)**:
   - Cấu trúc Class, Interface, DTOs, Service, Repositories liên quan trực tiếp.
3. ** 3. Sequence Diagram (Mermaid `sequenceDiagram`)**:
   - Sơ đồ tương tác từng bước Client → Security Filter → Controller → Service → DB / Cloudinary / PayOS / Brevo / AI Engine.
4. ** 4. Testing & Verification Report**:
   - **Mục tiêu kiểm thử (Test Objective)**
   - **Dữ liệu đầu vào (Test Input Data)**
   - **Quy trình thực hiện (Step-by-Step Test Procedure)**
   - **Kịch bản kỳ vọng (Expected Result)**
   - **Kết quả thực tế đã xác minh (Empirical Verification Result)**: Trạng thái Pass 100% từ `mvn clean test`.
