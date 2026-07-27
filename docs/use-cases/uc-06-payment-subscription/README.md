# UC-06 — Gói Cước & Thanh Toán Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Thanh toán, Gói cước VIP và Ví Voucher.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-06.1-vip-plans.md](UC-06.1-vip-plans.md) | Xem danh sách gói VIP | `GET /api/v1/payment/plans` | Guest / User |
| [UC-06.2-create-payos-order.md](UC-06.2-create-payos-order.md) | Tạo đơn hàng PayOS | `POST /api/v1/payment/create-order` | User |
| [UC-06.3-payos-webhook.md](UC-06.3-payos-webhook.md) | Webhook Auto-VIP PayOS | `POST /api/v1/payment/webhook` | PayOS System |
| [UC-06.4-order-status.md](UC-06.4-order-status.md) | Tra cứu trạng thái đơn | `GET /api/v1/payment/order/{orderCode}` | User |
| [UC-06.5-apply-voucher.md](UC-06.5-apply-voucher.md) | Áp dụng Voucher | `POST /api/v1/payment/apply-voucher` | User |
| [UC-06.6-payment-history.md](UC-06.6-payment-history.md) | Lịch sử thanh toán cá nhân | `GET /api/v1/payment/history` | User |
| [UC-06.7-voucher-wallet.md](UC-06.7-voucher-wallet.md) | Ví Voucher giảm giá cá nhân | `GET /api/v1/vouchers/my/available` | User |
