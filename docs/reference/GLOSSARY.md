# TECHNICAL GLOSSARY & DOMAIN TERMINOLOGY

Document Reference: SPEC-GLOSSARY-2026-V1  
Standard Compliance: ISO/IEC 2382 (Information Technology Vocabulary)  

---

## 1. Domain Terminology Registry

| Term | Domain Category | Definition & Technical Scope |
|---|---|---|
| **MC (Master of Ceremonies)** | User Roles | A verified host or presenter account (`ROLE_MC`) maintaining an `MCProfile`, portfolio case studies, availability schedules, and booking quotes. |
| **Voice Training Lesson** | Core Training | A phonetic script and sample audio reference stored in `voice_lessons` collection used for AI pronunciation, intonation, and speed analysis. |
| **Practice Session** | Core Training | A recorded user attempt stored in `practice_sessions` containing raw audio Cloudinary URL and AI scoring breakdown (overall, pronunciation, intonation, speed). |
| **Voice Arena** | Gamification | A competitive voice contest event stored in `competitions` where users submit audio entries (`competition_records`) for public/peer scoring. |
| **Streak Freeze** | Gamification | An inventory item in `UserStats` allowing a user to preserve their consecutive daily learning streak if a calendar day is missed. |
| **Peer Review** | Learning & Review | A feedback mechanism (`PracticeReview`) where verified MCs evaluate and provide qualitative voice coaching to student practice sessions. |
| **PayOS VietQR** | Financial Gateway | An automated payment processing system using interbank VietQR transfers, HMAC SHA-256 webhook callbacks, and `PaymentTransaction` ledgers. |
| **BM25 Search** | Search Engine | Okapi BM25 relevance scoring algorithm implemented in Elasticsearch (`voice_lessons_index`) for full-text phonetic lesson retrieval. |
| **Virtual Threads** | Infrastructure | Lightweight threads introduced in Java 21 (Project Loom) enabling high-concurrency non-blocking I/O execution. |
| **Guest Cooldown** | Security & Limits | A rate-limiting mechanism (`GuestVoiceUsage`) restricting unauthenticated guest users from overusing AI scoring endpoints. |
