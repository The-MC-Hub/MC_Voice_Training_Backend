# 💻 The MC Hub — Developer Onboarding & Contribution Guide

Hướng dẫn thiết lập môi trường phát triển (Local Development), quy trình viết code, chạy kiểm thử và chuẩn Commit cho lập trình viên mới.

---

## 🚀 1. Thiết Lập Môi Trường Local

### Yêu Cầu Cài Đặt
- JDK 21 (Amazon Corretto hoặc Temurin Java 21)
- Apache Maven 3.9+
- Node.js v20.x, npm v10.x
- Git & GitNexus CLI (`npm install -g gitnexus`)

### Clone & Chạy Backend
```bash
cd MC_Voice_Training_Backend
cp .env.example .env
mvn clean compile spring-boot:run
```
> API Server sẽ khởi chạy tại: `http://localhost:5000`

### Chạy Frontend
```bash
cd MC_Voice_Training_Frontend
npm install
npm run dev
```
> App UI sẽ khởi chạy tại: `http://localhost:5173`

---

## 🧪 2. Chạy Kiểm Thử (Testing & Verification)

### Unit & Integration Tests (Backend)
```bash
mvn clean test
```
*Tất cả 533+ unit tests phải vượt qua 100% trước khi thực hiện pull request / commit.*

### E2E Tests (Frontend)
```bash
npx playwright test
```

---

## 📌 3. Quy Trình Phân Nhánh & Conventional Commits

### Đặt tên Branch:
- `feature/<tên-tính-năng>`: Tính năng mới.
- `fix/<tên-lỗi>`: Sửa lỗi bug.
- `refactor/<tên-module>`: Tối ưu hóa code.

### Cấu trúc Message Commit:
- `feat(scope): ...` — Thêm tính năng mới.
- `fix(scope): ...` — Sửa lỗi bug.
- `refactor(scope): ...` — Cải tiến cấu trúc mã nguồn.
- `docs(scope): ...` — Cập nhật tài liệu.
- `test(scope): ...` — Thêm/sửa Unit Test.
