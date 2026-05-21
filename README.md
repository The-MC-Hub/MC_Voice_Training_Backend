# MC Voice Training — Backend

> Spring Boot 3.3 REST API powering the MC Voice Training platform. Handles user authentication, voice lesson management, AI session processing, academy courses, community competitions, payment integration, and real-time notifications.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot) ![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?logo=mongodb) ![License](https://img.shields.io/badge/license-MIT-green)

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Domain Models](#domain-models)
- [API Endpoints](#api-endpoints)
- [Environment Variables](#environment-variables)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Overview

MC Voice Training Backend is a Java 21 + Spring Boot 3.3 application providing the complete REST API for the MC Voice Training platform. It integrates with MongoDB Atlas for data persistence, Cloudinary for media storage, PayOS and MBBank VietQR for payment processing, and exposes a WebSocket endpoint for real-time notifications.

Default port: **5000** (configurable via `PORT` env var).

Swagger UI available at: `http://localhost:5000/swagger-ui/index.html`

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.10 |
| Database | MongoDB Atlas (Spring Data MongoDB) |
| Security | Spring Security + JWT (JJWT 0.12.5) |
| Real-time | Spring WebSocket + STOMP |
| Media Storage | Cloudinary |
| Payment | PayOS SDK + MBBank VietQR |
| Email | Spring Mail (SMTP) |
| Mapping | MapStruct + Lombok |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven |
| Testing | JUnit 5 + Flapdoodle Embedded MongoDB |

---

## Architecture

Strict **4-layer** pattern — never bypass layers:

```
HTTP Request
  → Controller   (@RestController)  — validate DTO, call service, return ApiResponse<T>
  → Service      (@Service)         — all business logic, @Async, CompletableFuture
  → Repository   (@Repository)      — Spring Data MongoDB queries only
  → MongoDB Atlas
         ↕
  Mapper (MapStruct)   — Entity ↔ DTO conversion
  SecurityUtils        — getCurrentUserId(), safeMessage(e)
```

### Response Envelope

All endpoints return a unified `ApiResponse<T>`:

```json
{ "status": "success", "message": "...", "data": { ... } }
{ "status": "fail",    "message": "...", "data": null }
```

### Performance Rules
- No `.size()` on large Lists → use `repository.countBy...()` for DB-side counting
- No Java Streams on large datasets → use `@Aggregation` for SUM/AVG
- No DB calls inside loops → batch-fetch with `findAllById(ids)`, map in-memory
- Stats APIs with 2+ independent sources → `CompletableFuture.supplyAsync()` + `allOf().join()`
- Fire-and-forget tasks (notifications, audit logs) → `@Async`

---

## Domain Models

| Model | Description |
|---|---|
| `User` | Platform user — roles: `USER`, `MC`, `ADMIN` |
| `MCProfile` | Extended MC profile (bio, rates, portfolio, specialties) |
| `VoiceLesson` | Practice script with category, difficulty, content |
| `PracticeSession` | Recorded voice session + AI analysis results |
| `Course` | Academy course with milestones |
| `CourseEnrollment` | User enrollment + progress tracking |
| `ReadingGuide` | Theory reading content (Markdown) |
| `Competition` | Voice Duel Arena (daily/weekly) |
| `CompetitionRecord` | User submission to a competition |
| `PaymentTransaction` | Payment record (Premium subscription) |
| `Notification` | In-app notification per user |
| `Certificate` | MC certification record |
| `AuditLog` | Admin audit trail |
| `Report` | Abuse/content reports |
| `UserStats` | Aggregated gamification stats |
| `RefreshToken` | JWT refresh token store |

---

## API Endpoints

Base path: `/api/v1`

### Auth — `/api/v1/auth`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/register` | Register new user | No |
| POST | `/login` | Login, returns JWT | No |
| POST | `/forgot-password` | Send password reset email | No |
| PUT | `/update` | Update profile / password | Yes |

### Voice — `/api/v1/voice`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/lessons` | List all practice scripts | No |
| GET | `/lessons/:id` | Get single lesson | No |
| POST | `/practice` | Submit recording + get AI analysis | Yes |
| GET | `/history` | Get user practice history | Yes |
| GET | `/sessions/:id` | Get session detail + report | Yes |
| POST | `/admin/lessons` | Create lesson (Admin) | Admin |
| PUT | `/admin/lessons/:id` | Update lesson (Admin) | Admin |
| DELETE | `/admin/lessons/:id` | Delete lesson (Admin) | Admin |

### Academy / Courses — `/api/v1/courses`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/` | List all courses | No |
| GET | `/:id` | Get course with milestones | No |
| POST | `/enroll/:id` | Enroll in course | Yes |
| PUT | `/progress/:enrollmentId` | Update milestone progress | Yes |
| GET | `/my` | Get enrolled courses | Yes |
| POST | `/admin` | Create course (Admin) | Admin |
| PUT | `/admin/:id` | Update course (Admin) | Admin |
| DELETE | `/admin/:id` | Delete course (Admin) | Admin |

### Community — `/api/v1/community`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/competitions` | List active competitions | No |
| GET | `/competitions/:id` | Get competition detail | No |
| POST | `/competitions/:id/submit` | Submit entry | Yes |
| GET | `/leaderboard/:id` | Competition leaderboard | No |
| POST | `/admin/competitions` | Create arena (Admin) | Admin |
| PUT | `/admin/competitions/:id` | Update arena (Admin) | Admin |
| DELETE | `/admin/competitions/:id` | Delete arena (Admin) | Admin |

### Payment — `/api/v1/payment`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/create-order` | Generate VietQR payment order | Yes |
| POST | `/webhook` | PayOS webhook handler | No |
| GET | `/status` | Check payment / premium status | Yes |

### Public — `/api/v1/public`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/mc/:id` | Get MC public profile | No |
| GET | `/mc/search` | Search MCs | No |

### Admin — `/api/v1/admin`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/users` | List all users | Admin |
| PUT | `/users/:id/verify` | Toggle MC verification | Admin |
| PUT | `/users/:id/suspend` | Toggle user suspension | Admin |
| GET | `/transactions` | All transactions | Admin |
| GET | `/overview` | Platform statistics | Admin |

### Notifications — `/api/v1/notifications`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/` | Get user notifications | Yes |
| PUT | `/read-all` | Mark all as read | Yes |

### Media — `/api/v1/media`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/upload` | Upload file to Cloudinary | Yes |

---

## Environment Variables

Create a `.env` file in the project root (loaded automatically via `dotenv-java`):

```env
# MongoDB Atlas
MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/<dbname>?retryWrites=true&w=majority
MONGODB_DATABASE=voice-tranning

# JWT
JWT_SECRET=your_jwt_secret_min_32_chars

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# PayOS
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_CHECKSUM_KEY=your_checksum_key

# MBBank VietQR
MBBANK_ACCOUNT_NO=your_account_number
MBBANK_ACCOUNT_NAME=YOUR ACCOUNT NAME

# Email (SMTP)
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_password

# Server
PORT=5000
```

| Variable | Required | Description |
|---|---|---|
| `MONGODB_URI` | Yes | MongoDB Atlas connection string |
| `JWT_SECRET` | Yes | Min 32 chars, used to sign tokens |
| `CLOUDINARY_*` | Yes | For avatar + portfolio uploads |
| `PAYOS_*` | Yes | PayOS payment gateway credentials |
| `MBBANK_ACCOUNT_NO` | Yes | VietQR bank account number |
| `SPRING_MAIL_*` | Optional | Email for password reset |
| `PORT` | Optional | Server port (default: 5000) |

---

## Getting Started

### Prerequisites

- Java 21
- Maven 3.9+
- MongoDB Atlas cluster (or local MongoDB)
- Cloudinary account

### Run Locally

```bash
# Clone the repository
git clone https://github.com/The-MC-Hub/MC_Voice_Training_Backend.git
cd MC_Voice_Training_Backend

# Create .env from example and fill in values
cp .env.example .env   # (create manually if no example exists)

# Compile (also generates MapStruct implementations)
mvn compile

# Run development server
mvn spring-boot:run

# Full build + tests
mvn clean package
```

Server starts at `http://localhost:5000`
Swagger UI at `http://localhost:5000/swagger-ui/index.html`

### Docker (optional)

```bash
# Build image
docker build -t mc-voice-backend .

# Run with env file
docker run --env-file .env -p 5000:5000 mc-voice-backend
```

---

## Testing

Tests use **Flapdoodle Embedded MongoDB** — no real database connection required.

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=VoiceControllerTest

# Run tests + generate report
mvn verify
```

Test files live in `src/test/java/com/mchub/controllers/`.

---

## Project Structure

```
MC_Voice_Training_Backend/
├── src/
│   ├── main/
│   │   ├── java/com/mchub/
│   │   │   ├── config/          # Security, CORS, WebSocket, Cloudinary config
│   │   │   ├── controllers/     # REST controllers per domain
│   │   │   │   └── admin/       # Admin-specific controllers
│   │   │   ├── dto/             # Request/Response DTOs
│   │   │   ├── enums/           # UserRole, BookingStatus, EventType, etc.
│   │   │   ├── exception/       # Global exception handler
│   │   │   ├── mapper/          # MapStruct entity ↔ DTO mappers
│   │   │   ├── models/          # MongoDB @Document entities
│   │   │   ├── repositories/    # Spring Data MongoDB repositories
│   │   │   ├── services/        # Business logic layer
│   │   │   │   └── impl/        # Service implementations
│   │   │   ├── util/            # SecurityUtils, helpers
│   │   │   ├── websocket/       # STOMP WebSocket handlers
│   │   │   └── TheMCHubApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/mchub/
│           └── controllers/     # Integration tests
├── .env.example
├── .gitignore
├── pom.xml
└── README.md
```

---

## WebSocket

Protocol: STOMP over SockJS at `ws://localhost:5000/ws`

| Topic | Purpose |
|---|---|
| `/topic/notifications/{userId}` | Push notifications to specific user |
| `/topic/messages/{conversationId}` | Live chat messages |
| `/app/chat/send` | Send message (client → server) |

---

## License

MIT © The MC Hub Team
