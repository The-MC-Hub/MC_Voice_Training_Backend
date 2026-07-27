# CHANGELOG

All notable changes to the MC Voice Training & Booking Platform Backend project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-07-27

### Added
- Complete IEEE 1016-2009 technical architecture documentation suite (`docs/architecture/SYSTEM_OVERVIEW.md`, `DATA_MODEL.md`).
- Exhaustive Use Case documentation covering 80 Sub-UC files across 18 functional modules (`docs/use-cases/`).
- Architecture Decision Records (ADRs 001 through 005) covering MongoDB, Elasticsearch BM25, PayOS, FastAPI AI Engine, and Brevo Email.
- Integration specifications for PayOS Payment Gateway, FastAPI AI Scoring Engine, Brevo Email Service, and Cloudinary Media Cloud.
- Technical reference guides for Central Error Code Registry, STOMP WebSocket Protocol, Environment Variables, and Domain Glossary.
- System operational guides (`DEVELOPMENT_GUIDE.md`, `DEPLOYMENT_GUIDE.md`, `API_DOCUMENTATION.md`).

### Changed
- Standardized document formatting across all Markdown files to formal academic/engineering specifications (0% emoji usage).

### Security
- Enforced HMAC SHA-256 webhook signature verification for PayOS payment notifications.
- Integrated Bucket4j IP token bucket rate limiting on public REST endpoints.
