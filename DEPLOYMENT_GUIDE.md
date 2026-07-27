# 🚀 The MC Hub — System Deployment & Operations Guide

Hướng dẫn triển khai hệ thống (Deployment), cấu hình môi trường Production (Render, Docker, Vercel) và vận hành hệ thống.

---

## 🛠️ 1. Môi Trường & Yêu Cầu Cần Thiết

- **Backend:** Java 21 LTS (OpenJDK), Maven 3.9+, Spring Boot 3.3.10
- **Frontend:** Node.js 20+, React 18, Vite 5, TailwindCSS / Vanilla CSS
- **Database:** MongoDB Atlas (Cluster `MainDatabase`), Elasticsearch (Docker/Cloud)
- **External Services:** PayOS, Cloudinary, Brevo SMTP (Email), Google OAuth 2.0

---

## ⚙️ 2. Cấu Hình Biến Môi Trường (.env)

Tạo file `.env` từ mẫu `.env.example`:

```env
# Server & DB
PORT=5000
MONGODB_URI=mongodb+srv://<user>:<password>@cluster0.mongodb.net/mchub?retryWrites=true&w=majority

# Security & Auth
JWT_SECRET=your-32-byte-secret-key-here
JWT_EXPIRATION_MS=86400000

# Integrations
CLOUDINARY_CLOUD_NAME=mc-voice
CLOUDINARY_API_KEY=123456789
CLOUDINARY_API_SECRET=your-secret

PAYOS_CLIENT_ID=your-client-id
PAYOS_API_KEY=your-api-key
PAYOS_CHECKSUM_KEY=your-checksum-key

ALLOWED_ORIGINS=http://localhost:5173,https://mchub.vn
```

---

## 🐳 3. Triển Khai Bằng Docker Container

### Build Docker Image
```bash
docker build -t mchub-backend:latest .
```

### Chạy bằng Docker Compose
```bash
docker-compose up -d
```

---

## ☁️ 4. Triển Khai Production (Render & Vercel)

### Backend (Render Service)
1. Đăng nhập Render.com -> New Web Service.
2. Connect Repo Git -> Select Dockerfile / Maven Runtime.
3. Configure Env Variables (`MONGODB_URI`, `JWT_SECRET`, `PAYOS_*`).
4. Set Health Check Path: `/api/v1/public/landing`.

### Frontend (Vercel)
1. Import Frontend Repo trên Vercel.
2. Build Command: `npm run build`, Output Directory: `dist`.
3. Configure `VITE_API_BASE_URL=https://mc-voice-training-backend.onrender.com`.

---

## 🔍 5. Vận Hành & Giám Sát (Operations & Maintenance)

- **Realtime Logs:** Truy cập `/api/v1/admin/logs` qua SSE Stream.
- **System Health:** Kiểm tra `/api/v1/admin/system/health` để theo dõi Heap RAM, Virtual Threads và MongoDB Status.
- **Bảo trì Hệ thống:** Đổi `MAINTENANCE_MODE=true` trong Bảng quản trị để chặn kết nối tạm thời cho đợt nâng cấp DB.
