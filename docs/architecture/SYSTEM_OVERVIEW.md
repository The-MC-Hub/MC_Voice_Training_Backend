# Architecture Document: MC Hub System Overview

Document Version: 1.0.0
Target System: MC Voice Training & Booking Platform Backend
Technology Stack: Java 21, Spring Boot 3.3.10, MongoDB Atlas, Elasticsearch, PayOS, Brevo, FastAPI AI Engine

---

## 1. System Context & Component Architecture

System Diagram representing external client applications, the core Spring Boot REST API service, persistence layers, and third-party integrations:

```mermaid
graph TD
    subgraph Clients
        WebClient["Web Client (React / Vite)"]
        MobileClient["Mobile Client (Cross-platform)"]
    end

    subgraph CoreBackend["Core Backend Service (Spring Boot 3.3 / Java 21)"]
        SecurityLayer["Spring Security & JWT Filter"]
        RateLimit["Bucket4j / In-Memory Rate Limiter"]
        RestControllers["REST Controllers (42 Controllers)"]
        WebSocketHandler["STOMP WebSocket Handler (/ws-chat)"]
        ServiceLayer["Service Layer (Business Logic)"]
        RepoLayer["Spring Data Repositories"]
    end

    subgraph DataStores["Data Persistence & Search"]
        MongoDB[("MongoDB Atlas (Main Cluster - mchub DB)")]
        Elasticsearch[("Elasticsearch Engine (BM25 Voice Search)")]
    end

    subgraph ExternalServices["External Services & Integrations"]
        FastAPI_AI["Python FastAPI Service (AI Voice Scoring & TTS)"]
        PayOS_API["PayOS Gateway (Payment & Webhook)"]
        Brevo_SMTP["Brevo Email Service (Transactional & Campaign)"]
        Cloudinary_CDN["Cloudinary CDN (Audio & Image Storage)"]
    end

    WebClient -->|HTTP REST / API v1| SecurityLayer
    MobileClient -->|HTTP REST / API v1| SecurityLayer
    WebClient -->|WSS STOMP| WebSocketHandler
    
    SecurityLayer --> RateLimit
    RateLimit --> RestControllers
    RestControllers --> ServiceLayer
    WebSocketHandler --> ServiceLayer
    
    ServiceLayer --> RepoLayer
    ServiceLayer -->|Sync / Async WebClient| FastAPI_AI
    ServiceLayer -->|PayOS SDK / HMAC| PayOS_API
    ServiceLayer -->|SMTP / Rest API| Brevo_SMTP
    ServiceLayer -->|SDK / REST| Cloudinary_CDN
    
    RepoLayer --> MongoDB
    ServiceLayer -->|ES Native Client| Elasticsearch
```

---

## 2. Layered Architecture Specifications

The application enforces a 4-tier modular architecture with standardized isolation rules:

### 2.1 Controller Layer (`com.mchub.controllers`)
- Receives HTTP/REST requests and maps path variables, query params, and request bodies.
- Enforces `@Valid` DTO validation annotations.
- Converts Domain Entities to DTOs via MapStruct (`com.mchub.mapper`) or standard response envelope builders.
- Standard Envelope Format:
  - Success: `{ "status": "success", "message": "...", "data": { ... } }`
  - Failure: `{ "status": "fail", "message": "...", "data": null }`

### 2.2 Service Layer (`com.mchub.services`)
- Implements core business logic, transaction boundaries, and state transitions.
- Evaluates authorization rules and user ownership constraints.
- Handles integrations with external HTTP clients (`PayOS`, `Brevo`, `Cloudinary`, `FastAPI AI`).

### 2.3 Repository Layer (`com.mchub.repositories`)
- Extends `MongoRepository<T, String>` for MongoDB persistence.
- Implements native `@Query` annotations and Mongo `@Aggregation` pipelines for high-performance aggregations.
- Handles batch processing (`findAllById`) to avoid N+1 query overhead.

### 2.4 Data Models (`com.mchub.models`)
- MongoDB Documents annotated with `@Document(collection = "...")`.
- Relational mapping handled via ObjectId references or embedded sub-documents.

---

## 3. High-Level Execution Flows

### 3.1 Voice Scoring & Practice Flow
```mermaid
sequenceDiagram
    autonumber
    actor User as Client User
    participant Controller as VoiceController
    participant Service as VoiceLessonService
    participant Cloudinary as Cloudinary Service
    participant AIEngine as External FastAPI AI
    participant DB as MongoDB

    User->>Controller: POST /api/v1/voice/analyze (Audio File + Lesson ID)
    Controller->>Service: analyzePractice(userId, lessonId, audioFile)
    Service->>Cloudinary: uploadAudio(audioFile)
    Cloudinary-->>Service: audioUrl
    Service->>AIEngine: POST /analyze-audio (audioUrl, referenceText)
    AIEngine-->>Service: AI Score (pronunciation, intonation, speed, accuracy)
    Service->>DB: Save PracticeSession record
    Service->>DB: Update UserStats (XP, streak, total practice time)
    Service-->>Controller: PracticeSessionResponseDTO
    Controller-->>User: HTTP 200 OK Response Envelope
```

### 3.2 Payment & Subscription Upgrade Flow
```mermaid
sequenceDiagram
    autonumber
    actor User as Client User
    participant PaymentCtrl as PaymentController
    participant PayOS as PayOS Gateway
    participant WebhookCtrl as WebhookController
    participant DB as MongoDB

    User->>PaymentCtrl: POST /api/v1/payments/create-payos-link (planId)
    PaymentCtrl->>PayOS: createPaymentLink(orderCode, amount, returnUrl)
    PayOS-->>PaymentCtrl: checkoutUrl
    PaymentCtrl-->>User: checkoutUrl
    User->>PayOS: Completes Payment on VietQR / Banking App
    PayOS->>WebhookCtrl: POST /api/v1/payments/payos-webhook (Payload + Signature)
    WebhookCtrl->>WebhookCtrl: Verify HMAC SHA-256 Signature
    WebhookCtrl->>DB: Update PaymentTransaction status to SUCCESS
    WebhookCtrl->>DB: Upgrade User VIP plan & grant benefits
    WebhookCtrl-->>PayOS: HTTP 200 OK (Webhook Ack)
```

---

## 4. Key Quality Attributes & Constraints

1. **Security**: Stateless JWT Authentication, BCrypt password hashing, HMAC SHA-256 webhook signature verification, role-based access control (`ROLE_USER`, `ROLE_MC`, `ROLE_ADMIN`).
2. **Performance**: Virtual Threads (Java 21) for concurrent operations, MongoDB aggregation pipelines, Elasticsearch BM25 text index for sub-millisecond lesson search.
3. **Resilience**: Rate limiting via `RateLimitFilter`, fault-tolerant external API fallbacks, structured system audit logging via `SystemLog`.
