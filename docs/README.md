# MC Hub Master Documentation Repository

Welcome to the central technical documentation suite for MC Voice Training & Booking Platform Backend.

---

## Technical Documentation Index

### 1. Architectural Specifications (`docs/architecture/`)
- [System Architecture Overview](architecture/SYSTEM_OVERVIEW.md): IEEE 1016-2009 System component diagram, HTTP/STOMP request flow, 4-tier layer isolation rules.
- [Data Model & ERD Specification](architecture/DATA_MODEL.md): Complete Entity Relationship Diagrams for 46 MongoDB collections across 8 domain clusters.
- [Architecture Decision Records (ADRs)](architecture/adr/):
  - [ADR-001: MongoDB Atlas Selection](architecture/adr/ADR-001-mongodb-atlas.md)
  - [ADR-002: Elasticsearch BM25 Search](architecture/adr/ADR-002-elasticsearch-bm25.md)
  - [ADR-003: PayOS Payment Integration](architecture/adr/ADR-003-payos-integration.md)
  - [ADR-004: External FastAPI AI Engine](architecture/adr/ADR-004-external-ai-fastapi.md)
  - [ADR-005: Brevo Email Dispatch](architecture/adr/ADR-005-brevo-email.md)

### 2. External Integration Specs (`docs/integrations/`)
- [PayOS Payment Integration Guide](integrations/PAYOS_INTEGRATION.md): Webhook HMAC SHA-256 signature verification and QR settlement flow.
- [AI Service API Contract](integrations/AI_SERVICE_CONTRACT.md): FastAPI HTTP payload schema for audio scoring & speech synthesis.
- [Brevo Email Service](integrations/BREVO_EMAIL_SERVICE.md): Transactional & marketing email dispatch protocol.
- [Cloudinary Storage Pipeline](integrations/CLOUDINARY_STORAGE.md): CDN folder structure and media upload pipeline.

### 3. Technical References & Testing (`docs/reference/`)
- [Master Test Strategy & Test Suite Matrix](reference/TEST_STRATEGY_AND_MATRIX.md): IEEE 829-2008 software test specification covering 493 unit/integration/security test cases.
- [Central Error Code Registry](reference/ERROR_CODES.md): Error code enum table and HTTP status mapping.
- [WebSocket & STOMP Protocol Guide](reference/WEBSOCKET_STOMP_GUIDE.md): Realtime `/ws-chat` channel topics and frame structure.
- [Environment Variables Registry](reference/ENVIRONMENT_VARIABLES.md): Complete environment variable parameters and defaults.
- [Technical Glossary & Terminology](reference/GLOSSARY.md): ISO/IEC 2382 domain vocabulary definitions.

### 4. Business & Sub-System Use Cases (`docs/use-cases/`)
- [Use Case Directory](use-cases/README.md): 80 detailed Sub-UC specifications across 18 modules (127 REST APIs, 42 Controllers).

### 5. Onboarding & Operations
- [API Documentation Overview](../API_DOCUMENTATION.md): High-level REST endpoint index.
- [Local Development Guide](../DEVELOPMENT_GUIDE.md): Local environment setup, MapStruct compilation, and testing workflow.
- [Production Deployment Guide](../DEPLOYMENT_GUIDE.md): Docker build, Render deployment, and health monitoring.
- [Changelog](../CHANGELOG.md): Version release history following Keep-a-Changelog specification.
- [Performance & Optimization Guide](../BACKEND_PERFORMANCE_GUIDE.md): Aggregation rules, N+1 query prevention, and Virtual Threads.
- [Security Audit Report](../SECURITY_AUDIT.md): Security analysis and vulnerability remediation details.
