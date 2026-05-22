# MC Voice Training — Backend

> Spring Boot 3.3 REST API powering the MC Voice Training platform. Handles user authentication, voice lesson management, AI session processing, academy courses, community competitions, payment integration, and real-time notifications.

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?logo=springboot) ![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?logo=mongodb) ![License](https://img.shields.io/badge/license-MIT-green)

---

## Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Tech Stack](#tech-stack)
- [Request Data Flow](#request-data-flow)
- [Domain Models](#domain-models)
- [API Endpoints](#api-endpoints)
- [Authentication & Authorization](#authentication--authorization)
- [WebSocket Real-time](#websocket-real-time)
- [Environment Variables](#environment-variables)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

---

## Overview

MC Voice Training Backend is a Java 21 + Spring Boot 3.3 application providing the complete REST API for the MC Voice Training platform. It integrates with:

- **MongoDB Atlas** — primary data persistence (cloud-hosted, AP East 1 region)
- **Cloudinary** — media storage for avatar and audio file uploads
- **PayOS + MBBank VietQR** — payment processing for Premium subscriptions
- **Spring WebSocket + STOMP** — real-time push notifications
- **Python AI Service** (`TrainingAiSample/main.py`) — voice analysis engine (called by `VoiceService` via HTTP)

**Default port:** `5000` (configurable via `PORT` env var)  
**Swagger UI:** `http://localhost:5000/swagger-ui/index.html`

---

## System Architecture

### High-Level System Map

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           MC Voice Training System                        │
│                                                                            │
│  ┌─────────────────┐        REST + JWT         ┌────────────────────┐   │
│  │   React Frontend │ ◄────────────────────────► │  Spring Boot API   │   │
│  │   (Vite, port    │                            │  (Java 21, :5000)  │   │
│  │    5173)         │        WebSocket/STOMP     │                    │   │
│  │                  │ ◄────────────────────────► │                    │   │
│  └─────────────────┘                            └────────┬───────────┘   │
│                                                          │                │
│                          ┌───────────────┬──────────────┼──────────────┐ │
│                          ▼               ▼              ▼              ▼ │
│                  ┌──────────────┐ ┌──────────┐ ┌─────────────┐ ┌──────┐ │
│                  │ MongoDB Atlas│ │Cloudinary│ │  PayOS API  │ │  AI  │ │
│                  │  (Database)  │ │  (Media) │ │ MBBank QR   │ │ :8000│ │
│                  └──────────────┘ └──────────┘ └─────────────┘ └──────┘ │
└──────────────────────────────────────────────────────────────────────────┘
```

### Internal Spring Boot Architecture — 4-Layer Pattern

The application enforces a strict 4-layer architecture. Bypassing any layer is a code review violation.

```
HTTP Request
     │
     ▼
┌────────────────────────────────────────────────────────────┐
│  Layer 1: CONTROLLER (@RestController)                       │
│  • Receives request, validates DTO with @Valid               │
│  • Calls service method — zero business logic here           │
│  • Wraps result in ApiResponse<T> and returns HTTP status    │
│  • Gets current user ID via SecurityUtils.getCurrentUserId() │
└────────────────────────────┬───────────────────────────────┘
                             │ calls
                             ▼
┌────────────────────────────────────────────────────────────┐
│  Layer 2: SERVICE (@Service)                                  │
│  • Contains ALL business logic                               │
│  • Orchestrates multiple repository calls                    │
│  • Uses @Async for fire-and-forget tasks                     │
│  • Uses CompletableFuture for parallel queries               │
│  • Converts between Entity and DTO via MapStruct mapper       │
└────────────────────────────┬───────────────────────────────┘
                             │ calls
                             ▼
┌────────────────────────────────────────────────────────────┐
│  Layer 3: REPOSITORY (@Repository)                            │
│  • Extends MongoRepository<Entity, String>                   │
│  • Contains ONLY query definitions (findBy*, countBy*)       │
│  • Complex queries use @Aggregation for server-side ops      │
│  • Never contains business logic                             │
└────────────────────────────┬───────────────────────────────┘
                             │ reads/writes
                             ▼
┌────────────────────────────────────────────────────────────┐
│  Layer 4: MONGODB ATLAS                                       │
│  • Cluster: MainDatabase, AP_EAST_1                          │
│  • Database: voice-tranning                                  │
│  • Collections mirror Domain Models below                    │
└────────────────────────────────────────────────────────────┘

                    Cross-cutting:
┌──────────────────────────────────────────────┐
│  MapStruct Mappers — Entity ↔ DTO conversion  │
│  SecurityUtils     — getCurrentUserId()       │
│                      safeMessage(e)           │
└──────────────────────────────────────────────┘
```

### API Response Envelope

Every endpoint returns a unified `ApiResponse<T>` wrapper — never raw objects.

```json
// Success
{
  "status": "success",
  "message": "Lấy lịch sử thành công",
  "data": { ... }
}

// Failure
{
  "status": "fail",
  "message": "Bài học không tồn tại: lesson_id_123",
  "data": null
}
```

---

## Tech Stack

| Category | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 21 | Virtual Threads, modern syntax |
| Framework | Spring Boot | 3.3.10 | Web, Security, Data, WebSocket |
| Database | MongoDB Atlas | Spring Data | Document persistence |
| Security | Spring Security + JWT (JJWT) | 0.12.5 | Authentication, role-based access |
| Real-time | Spring WebSocket + STOMP | — | Push notifications, live updates |
| Code Generation | MapStruct | — | Entity ↔ DTO compile-time mapping |
| Boilerplate | Lombok | — | `@Data`, `@Builder`, `@Slf4j`, etc. |
| Media | Cloudinary SDK | — | Avatar + audio file storage |
| Payment | PayOS SDK | — | VietQR payment link generation |
| API Docs | SpringDoc OpenAPI | — | Swagger UI auto-generation |
| Build | Maven | 3.9+ | Dependency management, compilation |
| Testing | JUnit 5 + Flapdoodle | — | Unit + integration tests (no real DB) |

---

## Request Data Flow

### Example: User submits voice recording

This illustrates the complete lifecycle of a `POST /api/v1/voice/practice` request.

```
1. Frontend sends multipart request:
   POST /api/v1/voice/practice
   Authorization: Bearer <jwt>
   Body: { audioFile, lessonId, criteria... }

2. JwtAuthenticationFilter validates token → extracts userId

3. VoiceController:
   a. @Valid validates PracticeRequest DTO
   b. Calls voiceService.submitPractice(request, userId)

4. VoiceService:
   a. Fetches VoiceLesson from DB to get script + target WPM
   b. Uploads audio file to Cloudinary (synchronous — URL needed)
   c. Calls Python AI Service: POST http://localhost:8000/analyze-voice
      → Receives: accuracy, rhythm, pacing, criteria_scores, overall_score,
                  feedback_vi, feedback_en, tips_vi, report_vi, ...
   d. Builds PracticeSession entity with all scores
   e. Saves PracticeSession to MongoDB (synchronous — ID needed for response)
   f. @Async: sends push notification to user
   g. @Async: writes audit log entry
   h. Returns saved session to controller

5. VoiceController:
   a. Maps PracticeSession → PracticeSessionResponseDTO via mapper
   b. Returns ApiResponse.success("Phân tích hoàn thành", dto)

6. Frontend receives JSON with full analysis report
```

---

## Domain Models

| Model | Collection | Description |
|---|---|---|
| `User` | `users` | Platform user — roles: `USER`, `MC`, `ADMIN` |
| `MCProfile` | `mcprofiles` | Extended MC profile (bio, rates, portfolio, specialties) |
| `VoiceLesson` | `voice_lessons` | Practice script — title, content, category, difficulty, target WPM |
| `PracticeSession` | `practice_sessions` | Recorded session + full AI analysis results |
| `Course` | `courses` | Academy course with milestones |
| `CourseEnrollment` | `course_enrollments` | User enrollment + milestone progress tracking |
| `ReadingGuide` | `reading_guides` | Theory content (Markdown format) |
| `Competition` | `competitions` | Voice Duel Arena (daily/weekly contests) |
| `CompetitionRecord` | `competition_records` | User submission to a competition |
| `PaymentTransaction` | `transactions` | Payment record (Premium subscription) |
| `Notification` | `notifications` | In-app notification per user |
| `Certificate` | `certificates` | MC certification record (verified by admin) |
| `AuditLog` | `audit_logs` | Admin audit trail — who did what |
| `Report` | `reports` | Abuse/content reports |
| `UserStats` | `user_stats` | Aggregated gamification stats (streak, total time) |
| `RefreshToken` | `refresh_tokens` | JWT refresh token storage |

### Collection Relationships

```
users ─────────────────────────────────────────────────────────┐
  │                                                             │
  ├─► mcprofiles (userId → users._id)                          │
  │                                                             │
  ├─► voice_lessons (createdBy → users._id)                    │
  │                                                             │
  ├─► practice_sessions (userId → users._id)                   │
  │     └── lessonId → voice_lessons._id                       │
  │                                                             │
  ├─► course_enrollments (userId → users._id)                  │
  │     └── courseId → courses._id                             │
  │                                                             │
  ├─► competitions (─)                                         │
  │     └─► competition_records (userId → users._id)           │
  │                                                             │
  ├─► notifications (userId → users._id)                       │
  ├─► refresh_tokens (userId → users._id)                      │
  └─► audit_logs (userId → users._id)
```

---

## API Endpoints

Base path: `/api/v1`

### Auth — `/api/v1/auth`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/register` | Register new user (email + password) | Public |
| POST | `/login` | Login, returns JWT + user info | Public |
| POST | `/forgot-password` | Send password reset email | Public |
| PUT | `/update` | Update profile or change password | JWT |

### Voice — `/api/v1/voice`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/lessons` | List all practice scripts (with filters) | Public |
| GET | `/lessons/:id` | Get single lesson with full content | Public |
| POST | `/practice` | Submit recording → get AI analysis | JWT |
| GET | `/history` | Get user practice session history | JWT |
| GET | `/sessions/:id` | Get session detail + full AI report | JWT |
| GET | `/public/stats` | Get aggregated platform stats (landing page) | Public |
| POST | `/admin/lessons` | Create new lesson | Admin |
| PUT | `/admin/lessons/:id` | Update lesson | Admin |
| DELETE | `/admin/lessons/:id` | Delete lesson | Admin |

### Academy / Courses — `/api/v1/courses`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/` | List all published courses | Public |
| GET | `/:id` | Get course with full milestone list | Public |
| POST | `/enroll/:id` | Enroll in a course | JWT |
| PUT | `/progress/:enrollmentId` | Update milestone completion | JWT |
| GET | `/my` | Get all enrolled courses + progress | JWT |
| POST | `/admin` | Create course (Admin) | Admin |
| PUT | `/admin/:id` | Update course (Admin) | Admin |
| DELETE | `/admin/:id` | Delete course (Admin) | Admin |

### Community — `/api/v1/community`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/competitions` | List active competitions | Public |
| GET | `/competitions/:id` | Get competition detail + leaderboard | Public |
| POST | `/competitions/:id/submit` | Submit voice entry | JWT |
| GET | `/leaderboard/:id` | Competition leaderboard (ranked) | Public |
| POST | `/admin/competitions` | Create competition (Admin) | Admin |

### Payment — `/api/v1/payment`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/create-order` | Generate VietQR payment order for Premium | JWT |
| POST | `/webhook` | PayOS webhook (called by PayOS server) | Public |
| GET | `/status` | Check user's current Premium status | JWT |

### Notifications — `/api/v1/notifications`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/` | Get user notifications (paginated) | JWT |
| PUT | `/read-all` | Mark all as read | JWT |
| DELETE | `/delete-all` | Clear all notifications | JWT |

### Admin — `/api/v1/admin`

| Method | Path | Description | Auth |
|---|---|---|---|
| GET | `/dashboard` | Platform overview statistics | Admin |
| GET | `/users` | List all users | Admin |
| PUT | `/users/:id/verify` | Toggle MC verification badge | Admin |
| PUT | `/users/:id/suspend` | Toggle user suspension | Admin |
| GET | `/transactions` | All payment transactions | Admin |

### Media — `/api/v1/media`

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/upload` | Upload file to Cloudinary, returns URL | JWT |

---

## Authentication & Authorization

### JWT Flow

```
1. Client: POST /auth/login { email, password }
2. Server: validates credentials → generates JWT (contains userId as subject)
3. Client: stores JWT, sends in every subsequent request:
   Authorization: Bearer eyJhbGci...
4. JwtAuthenticationFilter: validates signature + expiry → injects userId into SecurityContext
5. Controllers: call SecurityUtils.getCurrentUserId() to get the authenticated user's ID
```

### Role-Based Access

| Annotation | Who can access |
|---|---|
| *(no annotation)* | Any authenticated user |
| `@PreAuthorize("hasAuthority('MC')")` | Only MC role accounts |
| `@PreAuthorize("hasAuthority('ADMIN')")` | Only Admin role accounts |
| *(public endpoint in SecurityConfig whitelist)* | Anyone — no JWT required |

### SecurityUtils

```java
// Get current authenticated user ID (throws IllegalStateException if not logged in)
String userId = SecurityUtils.getCurrentUserId();

// Null-safe exception message (never returns null — prevents NPE in error responses)
return ApiResponse.fail(SecurityUtils.safeMessage(e));
```

---

## WebSocket Real-time

**Protocol:** STOMP over SockJS  
**Endpoint:** `ws://localhost:5000/ws`

| Topic | Direction | Purpose |
|---|---|---|
| `/topic/notifications/{userId}` | Server → Client | Push in-app notifications |
| `/topic/messages/{conversationId}` | Server → Client | Live chat messages |
| `/app/chat/send` | Client → Server | Send a chat message |

### Client connection example (JavaScript)

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:5000/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => {
    // Subscribe to personal notification stream
    client.subscribe(`/topic/notifications/${userId}`, (message) => {
      const notification = JSON.parse(message.body);
      // Handle notification
    });
  },
});

client.activate();
```

---

## Environment Variables

Create `.env` in the project root (loaded automatically by `DotenvConfig.java`):

```env
# ─── Database ────────────────────────────────────────────────────────────────
MONGODB_URI=mongodb+srv://<user>:<pass>@<cluster>.mongodb.net/<dbname>?retryWrites=true&w=majority
MONGODB_DATABASE=voice-tranning

# ─── Security ────────────────────────────────────────────────────────────────
JWT_SECRET=your_jwt_secret_minimum_32_characters_long

# ─── Media Storage ───────────────────────────────────────────────────────────
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# ─── Payment ─────────────────────────────────────────────────────────────────
PAYOS_CLIENT_ID=your_client_id
PAYOS_API_KEY=your_api_key
PAYOS_CHECKSUM_KEY=your_checksum_key

# ─── Bank Account (VietQR) ───────────────────────────────────────────────────
MBBANK_ACCOUNT_NO=your_bank_account_number
MBBANK_ACCOUNT_NAME=YOUR ACCOUNT FULL NAME

# ─── Email (SMTP) ────────────────────────────────────────────────────────────
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_app_password          # Gmail App Password (not login password)

# ─── Server ──────────────────────────────────────────────────────────────────
PORT=5000
```

| Variable | Required | Description |
|---|---|---|
| `MONGODB_URI` | **Yes** | MongoDB Atlas connection string with credentials |
| `JWT_SECRET` | **Yes** | Secret key for JWT signing — minimum 32 characters |
| `CLOUDINARY_*` | **Yes** | Used for avatar and audio file uploads |
| `PAYOS_*` | **Yes** | PayOS payment gateway credentials |
| `MBBANK_ACCOUNT_NO` | **Yes** | VietQR bank account for Premium payments |
| `SPRING_MAIL_*` | Optional | Required only for password reset email feature |
| `PORT` | Optional | Defaults to 5000 if not set |

> **Security:** Never commit `.env` to version control. Add it to `.gitignore` immediately. The `.env.example` file in the repo shows variable names without values — copy it to `.env` and fill in your credentials.

---

## Getting Started

### Prerequisites

- Java 21 (verify: `java -version`)
- Maven 3.9+ (verify: `mvn -version`)
- MongoDB Atlas cluster (or local MongoDB 7.0+)
- Cloudinary account (free tier works)

### Quick Start

```bash
# 1. Clone repository
git clone https://github.com/The-MC-Hub/MC_Voice_Training_Backend.git
cd MC_Voice_Training_Backend

# 2. Create .env from example and fill in your credentials
cp .env.example .env
# Edit .env with your MongoDB URI, JWT secret, etc.

# 3. Compile (generates MapStruct implementations)
mvn compile

# 4. Run development server
mvn spring-boot:run

# Server starts at http://localhost:5000
# Swagger UI at http://localhost:5000/swagger-ui/index.html
```

### Build for Production

```bash
# Full build with tests
mvn clean package

# Run the JAR
java -jar target/mc-voice-training-*.jar
```

### Docker (Optional)

```bash
# Build Docker image
docker build -t mc-voice-backend .

# Run with environment file
docker run --env-file .env -p 5000:5000 mc-voice-backend
```

---

## Project Structure

```
MC_Voice_Training_Backend/
├── src/
│   ├── main/
│   │   ├── java/com/mchub/
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java         # Virtual Threads + @EnableAsync
│   │   │   │   ├── SecurityConfig.java      # JWT filter chain + CORS + whitelist
│   │   │   │   ├── WebSocketConfig.java     # STOMP broker configuration
│   │   │   │   ├── CloudinaryConfig.java    # Media upload configuration
│   │   │   │   ├── SwaggerConfig.java       # OpenAPI / Swagger UI
│   │   │   │   └── DotenvConfig.java        # Loads .env file at startup
│   │   │   │
│   │   │   ├── controllers/                 # Layer 1: HTTP endpoints
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── VoiceController.java
│   │   │   │   ├── CourseController.java
│   │   │   │   ├── CommunityController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── NotificationController.java
│   │   │   │   ├── MediaController.java
│   │   │   │   └── admin/
│   │   │   │       └── AdminController.java
│   │   │   │
│   │   │   ├── services/                    # Layer 2: Business logic
│   │   │   │   ├── VoiceService.java        # Interface
│   │   │   │   └── impl/
│   │   │   │       └── VoiceServiceImpl.java # Implementation
│   │   │   │
│   │   │   ├── repositories/               # Layer 3: DB queries
│   │   │   │   ├── VoiceLessonRepository.java
│   │   │   │   └── PracticeSessionRepository.java
│   │   │   │
│   │   │   ├── models/                     # MongoDB @Document entities
│   │   │   │   ├── VoiceLesson.java
│   │   │   │   └── PracticeSession.java
│   │   │   │
│   │   │   ├── dto/                        # Request/Response DTOs
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── PracticeSessionResponseDTO.java
│   │   │   │   └── ... (one per model)
│   │   │   │
│   │   │   ├── mapper/                     # MapStruct interfaces
│   │   │   │   └── PracticeSessionMapper.java
│   │   │   │
│   │   │   ├── enums/                      # Type-safe enumerations
│   │   │   │   ├── UserRole.java           # USER | MC | ADMIN
│   │   │   │   ├── LessonCategory.java     # WEDDING | NEWS | PRESENTATION | ...
│   │   │   │   └── LessonDifficulty.java   # EASY | MEDIUM | HARD
│   │   │   │
│   │   │   ├── util/
│   │   │   │   └── SecurityUtils.java      # getCurrentUserId(), safeMessage()
│   │   │   │
│   │   │   └── TheMCHubApplication.java    # Main class
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│       └── java/com/mchub/
│           └── controllers/               # Integration tests
│               └── VoiceControllerTest.java
│
├── .env.example                           # Template — copy to .env and fill in
├── .gitignore                             # .env and build artifacts excluded
├── pom.xml                                # Maven dependencies and build config
└── README.md                              # This file
```

---

## Testing

Tests use **Flapdoodle Embedded MongoDB** — no real database connection or internet required.

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=VoiceControllerTest

# Run all tests and generate Surefire report
mvn verify

# Run tests without rebuilding (faster)
mvn test -o
```

**Test location:** `src/test/java/com/mchub/controllers/`

Each test class corresponds to a controller (e.g., `VoiceControllerTest` covers `VoiceController`). Tests use `@SpringBootTest` with `EmbeddedMongoAutoConfiguration` to spin up an in-memory MongoDB instance.

---

## Troubleshooting

### Server fails to start — "Cannot connect to MongoDB"

**Cause:** `MONGODB_URI` in `.env` is missing, wrong, or the Atlas cluster is paused.

```bash
# Check .env exists and has MONGODB_URI
cat .env | grep MONGODB_URI

# Verify Atlas cluster is running at cloud.mongodb.com
# Check that your IP is whitelisted in Atlas Network Access
```

### `mvn compile` fails — "cannot find symbol" for Lombok methods

**Cause:** Lombok annotation processor not enabled in IDE, or Maven annotation processing disabled.

```bash
# Force clean recompile — clears cached stubs
mvn clean compile

# If still failing, check pom.xml has both:
# 1. lombok dependency
# 2. maven-compiler-plugin with annotationProcessorPaths for both lombok AND mapstruct
```

### IDE shows red on `log.info()`, `getId()`, `getTitle()` in service files

**Cause:** IDE language server does not run Maven annotation processors — this is a false positive. The code is correct.

**Fix:** Run `mvn compile` once. The generated `target/` classes satisfy the IDE. Alternatively, install the Lombok plugin for your IDE (IntelliJ: `Settings → Plugins → Lombok`).

### MapStruct mapper is out of date after adding DTO fields

**Symptom:** New field in DTO is not being mapped — response is missing the field.

**Fix:**
```bash
# Regenerate MapStruct implementations
mvn compile
# MapStruct reads @Data fields at compile time and generates new *MapperImpl.java
```

### PayOS webhook is not being received locally

**Cause:** PayOS needs a public URL to call. `localhost:5000` is not reachable from the internet.

**Fix:** Use [ngrok](https://ngrok.com/) to expose your local port:

```bash
ngrok http 5000
# Copy the https://xxxxx.ngrok.io URL
# Set it as webhook URL in PayOS dashboard
```

### WebSocket connection drops immediately

**Cause:** JWT is not being passed in the WebSocket connection headers, or the token has expired.

**Fix:** Ensure the frontend passes the token in `connectHeaders`:
```javascript
const client = new Client({
  connectHeaders: { Authorization: `Bearer ${token}` },
  // ...
});
```

Also verify `WebSocketConfig.java` allows the frontend origin in its CORS configuration.

### Spring Boot starts but all endpoints return 403

**Cause:** `SecurityConfig.java` whitelist does not include the endpoint, or `@PreAuthorize` annotation has wrong role name.

**Check:**
1. Is the endpoint path listed in `SecurityConfig` `permitAll()` section?
2. Is the JWT being sent with `Bearer ` prefix (note the space)?
3. Does the user's role match what `@PreAuthorize` requires?

---

## License

MIT © The MC Hub Team
