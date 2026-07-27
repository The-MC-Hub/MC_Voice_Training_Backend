# UC-09 — Quản Trị Hệ Thống Index

Bảng tổng hợp tất cả các Use Case con (Sub-UC) thuộc luồng Quản trị hệ thống, Giám sát, Kiểm duyệt và Log Server.

| File Use Case | Tên Tính Năng | Endpoint | Actor |
|---|---|---|---|
| [UC-09.1-health-monitoring.md](UC-09.1-health-monitoring.md) | Giám sát sức khỏe Realtime | `GET /api/v1/admin/system/health` | Admin |
| [UC-09.2-maintenance-mode.md](UC-09.2-maintenance-mode.md) | Bật/tắt Chế độ Bảo trì | `PUT /api/v1/admin/system/settings/dynamic` | Admin |
| [UC-09.3-temporary-sanction.md](UC-09.3-temporary-sanction.md) | Tạm khóa tài khoản | `PUT /api/v1/admin/users/{id}/suspend-temporary` | Admin |
| [UC-09.4-auto-reactivation-scheduler.md](UC-09.4-auto-reactivation-scheduler.md) | Tự động mở khóa hết hạn | `Scheduled Task (5 mins)` | System |
| [UC-09.5-manual-unsuspend.md](UC-09.5-manual-unsuspend.md) | Mở khóa thủ công | `PUT /api/v1/admin/users/{id}/unsuspend` | Admin |
| [UC-09.6-export-csv-audit.md](UC-09.6-export-csv-audit.md) | Xuất file CSV Audit Log | `GET /api/v1/audit-logs/export-csv` | Admin |
| [UC-09.7-refund-transaction.md](UC-09.7-refund-transaction.md) | Hoàn tiền đơn hàng | `POST /api/v1/admin/transactions/{id}/refund` | Admin |
| [UC-09.8-manual-grant-plan.md](UC-09.8-manual-grant-plan.md) | Cấp tặng gói VIP thủ công | `POST /api/v1/admin/transactions/manual-grant` | Admin |
| [UC-09.9-bulk-resolve-reports.md](UC-09.9-bulk-resolve-reports.md) | Duyệt báo cáo hàng loạt | `PUT /api/v1/reports/bulk-resolve` | Admin |
| [UC-09.10-server-runtime-logs.md](UC-09.10-server-runtime-logs.md) | Log hệ thống Server Realtime | `GET /api/v1/logs/system` | Admin |
