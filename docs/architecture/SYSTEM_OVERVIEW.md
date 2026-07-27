# SOFTWARE ARCHITECTURE SPECIFICATION
## MC Voice Training & Booking Platform Backend

**Document Reference**: SPEC-ARCH-2026-V2  
**Standard Compliance**: IEEE 1016-2009 (Software Architecture Description)  
**Target Environment**: Java 21 LTS / Spring Boot 3.3 / MongoDB Atlas / Elasticsearch 8.x  

---

## 1. System Overview and Architectural Goals

This specification documents the architectural layout, security mechanisms, data structures, and integration interfaces for the MC Voice Training & Booking Platform backend service.

### 1.1 Fundamental Design Objectives
- **Scalability**: The system MUST support concurrent audio evaluation requests through Java 21 Virtual Thread execution (`spring.threads.virtual.enabled=true`).
- **Data Integrity**: Document consistency within MongoDB Atlas SHALL be guaranteed via application-level boundary validation and explicit Mongo indexes.
- **Interoperability**: Inter-service communication with external compute nodes (FastAPI AI Engine, PayOS Gateway, Brevo Email Service, Cloudinary CDN) MUST follow stateless HTTP REST and STOMP WebSocket standards.
- **Security**: Access control SHALL enforce stateless JSON Web Token (JWT) authentication, BCrypt password hashing, and role-based privilege isolation.

---

## 2. High-Level System Architecture

```mermaid
graph TD
    subgraph ClientTier["Client Application Tier"]
        WebApp["Web Client (React / Vite)"]
        MobileApp["Mobile Client (Cross-Platform)"]
    end

    subgraph SecurityTier["Security and Rate Limiting Tier"]
        CORS["CorsFilter"]
        RateLimit["RateLimitFilter"]
        JwtAuth["JwtAuthenticationFilter"]
    end

    subgraph ServiceTier["Application Container (Spring Boot 3.3 / Java 21)"]
        RESTControllers["REST Controllers (42 Classes)"]
        STOMPBroker["STOMP WebSocket Handler (/ws-chat)"]
        ServiceLayer["Business Service Layer"]
        DataMappers["MapStruct Object Mappers"]
        RepoLayer["Spring Data Mongo Repositories"]
    end

    subgraph PersistenceTier["Data Persistence and Search Tier"]
        MongoDB[("MongoDB Atlas Database Cluster")]
        Elasticsearch[("Elasticsearch 8.x (BM25 Index Engine)")]
    end

    subgraph ExternalTier["External Provider Integrations"]
        FastAPI["Python FastAPI AI Service"]
        PayOS["PayOS Payment Gateway"]
        Brevo["Brevo Transactional Email Engine"]
        Cloudinary["Cloudinary Media Asset CDN"]
    end

    WebApp -->|HTTP REST / API v1| CORS
    MobileApp -->|HTTP REST / API v1| CORS
    WebApp -->|WSS STOMP| STOMPBroker

    CORS --> RateLimit
    RateLimit --> JwtAuth
    JwtAuth --> RESTControllers
    STOMPBroker --> ServiceLayer

    RESTControllers --> DataMappers
    RESTControllers --> ServiceLayer
    ServiceLayer --> RepoLayer
    RepoLayer --> MongoDB
    ServiceLayer -->|Native ES Client| Elasticsearch

    ServiceLayer -->|HTTP WebClient| FastAPI
    ServiceLayer -->|HMAC SHA-256 REST| PayOS
    ServiceLayer -->|Async SMTP / API| Brevo
    ServiceLayer -->|Cloudinary SDK| Cloudinary
```

---

## 3. Layered Component Boundaries

### 3.1 Interface Layer (`com.mchub.controllers`)
- **Responsibility**: Accepts incoming HTTP requests, performs preliminary syntax validation (`@Valid`), and serializes domain models into standardized response envelopes.
- **Constraint**: Controllers MUST NOT execute direct business logic or initiate raw database operations unless invoking specialized single-purpose repositories.

### 3.2 Service Layer (`com.mchub.services`)
- **Responsibility**: Encapsulates core business rules, transactional state management, user privilege evaluation, and external API orchestration.
- **Constraint**: Services SHALL handle external network failures gracefully through fallback mechanisms.

### 3.3 Data Access Layer (`com.mchub.repositories`)
- **Responsibility**: Provides data persistence interfaces inheriting from `MongoRepository<T, String>`.
- **Constraint**: Queries involving bulk aggregation MUST utilize native database aggregation pipelines (`@Aggregation`) rather than loading raw entity lists into Java heap memory.

---

## 4. Security Architecture and Request Validation

```mermaid
sequenceDiagram
    autonumber
    actor Client as External Client
    participant CORS as CorsFilter
    participant RL as RateLimitFilter
    participant JWT as JwtAuthenticationFilter
    participant SecCtx as SecurityContextHolder
    participant Ctrl as REST Controller

    Client->>CORS: Inbound HTTP Request
    alt Invalid Origin Header
        CORS-->>Client: HTTP 403 Forbidden
    end
    CORS->>RL: Allowed Origin Validated
    alt Rate Exceeded (> 100 req/min per IP)
        RL-->>Client: HTTP 429 Too Many Requests
    end
    RL->>JWT: Request Within Rate Limit
    alt Public Endpoint Unrestricted
        JWT->>Ctrl: Pass-through
    else Protected Endpoint
        JWT->>JWT: Verify Bearer Token Signature (HMAC SHA-256)
        alt Token Expired or Signature Invalid
            JWT-->>Client: HTTP 401 Unauthorized
        else Token Valid
            JWT->>SecCtx: Populate Authentication Principal
            JWT->>Ctrl: Dispatch to Handler Method
        end
    end
    Ctrl-->>Client: HTTP 200 OK Response Envelope
```

---

## 5. Requirement Key Terms (RFC 2119)
- **MUST / SHALL**: Absolute requirement of the technical specification.
- **MUST NOT / SHALL NOT**: Absolute prohibition of the technical specification.
- **SHOULD / RECOMMENDED**: Valid reasons may exist to deviate under specific operational conditions.
- **MAY / OPTIONAL**: Truly optional capabilities.
