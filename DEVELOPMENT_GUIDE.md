# Development & Developer Onboarding Manual

Document Version: 2.0.0
Target Platform: MC Voice Training Backend

---

## 1. System Requirements & Software Stack

### Minimum Hardware Requirements
- **CPU**: Dual-Core 2.0 GHz+ (Apple Silicon M-Series or Intel Core i5/i7 recommended)
- **RAM**: 8 GB minimum (16 GB recommended for concurrent Spring Boot + MongoDB + Elasticsearch execution)
- **Disk**: 10 GB free SSD storage

### Core Software Dependencies
| Software | Required Version | Verification Command |
|---|---|---|
| OpenJDK | 21.0.x | `java -version` |
| Apache Maven | 3.9.x+ | `mvn -v` |
| Git | 2.40.x+ | `git --version` |
| MongoDB | 7.0.x (or MongoDB Atlas) | `mongosh --version` |
| Docker & Compose | 24.x+ (Optional) | `docker --version` |

---

## 2. Environment Configuration (`.env`)

Copy `.env.example` to `.env` in the project root:

```bash
cp .env.example .env
```

### Exhaustive Configuration Parameter Table

```properties
# Server Core Settings
PORT=5000
SPRING_PROFILES_ACTIVE=dev

# Database Configuration
MONGODB_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/mchub?retryWrites=true&w=majority
MONGODB_TEST_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/mchub_test?retryWrites=true&w=majority

# JWT Security Credentials
JWT_SECRET=401b09eab3c013d4ca54922bb802bec8fd5318192b0a75f201d8b3727429090fb337591abd3e44453b954555b7a0812e1081c39b740293f765eae731f5a65ed1
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# Elasticsearch Endpoint (Optional for local full-text search)
ELASTICSEARCH_URIS=http://localhost:9200

# Cloudinary Storage Credentials
CLOUDINARY_CLOUD_NAME=mchub-demo
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=aBcDeFgHiJkLmNoPqRsTuVwXyZ

# PayOS Gateway Integration
PAYOS_CLIENT_ID=payos_client_id_sample
PAYOS_API_KEY=payos_api_key_sample
PAYOS_CHECKSUM_KEY=payos_checksum_key_sample

# External FastAPI AI Engine
AI_ANALYZE_URL=https://mc-voice-ai.hf.space/analyze
AI_TTS_URL=https://mc-voice-ai.hf.space/tts

# Brevo Email Service
BREVO_SMTP_KEY=xkeysib-sample-key
MAIL_FROM_ADDRESS=no-reply@mchub.vn
MAIL_FROM_NAME=MC Hub System

# CORS Security
ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

---

## 3. IDE Setup & MapStruct Code Generation

### 3.1 IntelliJ IDEA Configuration
1. **Lombok Plugin**: Ensure `Lombok` plugin is installed and enabled (`Settings -> Plugins -> Lombok`).
2. **Annotation Processing**: Enable Annotation Processing (`Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors` -> Check `Enable annotation processing`).
3. **SDK Config**: Set Project SDK to Java 21 (`File -> Project Structure -> Project -> SDK: 21`).

### 3.2 MapStruct DTO Compilation
MapStruct generates implementation mappers during compilation under `target/generated-sources/annotations/`. If you add new fields to DTOs or Entities:

```bash
# Clean target directory and regenerate MapStruct mappers
mvn clean compile
```

---

## 4. Local Database Seeding & Mock Data

Seed sample voice lessons into MongoDB using the provided script:

```bash
# Seed voice lessons (Script sets up sample lessons with Cloudinary images)
mongosh "MONGODB_URI_HERE" seed-data/seed_voice_lessons.js

# Patch missing thumbnails on existing voice lessons
mongosh "MONGODB_URI_HERE" seed-data/patch_thumbnails.js
```

---

## 5. Development Workflow & Commands

### Running Development Server
```bash
mvn spring-boot:run
```

### Running Unit & Integration Test Suites
```bash
# Run all tests (46 test classes, 493+ @Test methods)
mvn test

# Run a single test class
mvn test -Dtest=AuthControllerTest

# Run a specific test method
mvn test -Dtest=AuthControllerTest#registerUser_Success
```

---

## 6. Common Development Gotchas & Troubleshooting

1. **`MapStruct mapper implementation not found`**:
   - Solution: Run `mvn compile` to force annotation processors to run and generate target classes.
2. **`MongoSocketOpenException: Exception opening socket`**:
   - Solution: Check network connection to MongoDB Atlas or verify IP Whitelist in MongoDB Atlas Security Settings (`0.0.0.0/0` for development).
3. **`Elasticsearch client connection refused`**:
   - Solution: Elasticsearch is optional for dev mode. If Elasticsearch is disabled, the system automatically falls back to MongoDB regex search (`VoiceLessonSearchServiceImpl`).
