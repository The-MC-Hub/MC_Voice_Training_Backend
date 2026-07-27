# UC-11 — Đặt Lịch Thuê MC Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Đặt lịch thuê MC Talent.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-11.1-create-booking.md](UC-11.1-create-booking.md) | Tạo đơn đặt lịch MC | `POST /api/v1/bookings` | Client |
| [UC-11.2-mc-respond-quote.md](UC-11.2-mc-respond-quote.md) | MC Phản hồi & Báo giá | `PUT /api/v1/bookings/{id}/respond` | MC |
| [UC-11.3-pay-booking.md](UC-11.3-pay-booking.md) | Thanh toán đơn PayOS | `POST /api/v1/bookings/{id}/pay` | Client |
| [UC-11.4-cancel-booking.md](UC-11.4-cancel-booking.md) | Hủy đơn đặt lịch | `PUT /api/v1/bookings/{id}/cancel` | Client / MC |
| [UC-11.5-review-mc.md](UC-11.5-review-mc.md) | Đánh giá & Chấm điểm MC | `POST /api/v1/mcs/{mcId}/reviews` | Client |
| [UC-11.6-admin-booking-control.md](UC-11.6-admin-booking-control.md) | Admin Bắt buộc hủy đơn | `PUT /api/v1/admin/bookings/{id}/force-cancel` | Admin |
