# MC Voice Training — Backend

> Java 21 + Spring Boot 3.3 REST API for the MC Voice Training platform: voice practice/AI feedback, course/academy content, community leaderboards & competitions, gamification (quests/vouchers/minigames), admin tooling, and marketing (email campaigns, announcements). Forked from `The-MC-Hub-Java-Backend` but does **not** include the MC-booking domain (no bookings/chat/notifications/favorites/schedules).

![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.10-6DB33F?logo=springboot) ![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-47A248?logo=mongodb) ![License](https://img.shields.io/badge/license-MIT-green)

For full architectural detail (endpoint list, layer-architecture deviations, WebSocket status, CORS/rate-limit config, known divergence from the primary backend) see **[CLAUDE.md](./CLAUDE.md)** — this README is a quick-start only.

---

## Overview

- **Port:** reads from `.env` `PORT` (do not assume a hardcoded default — check `.env` directly)
- **Database:** MongoDB Atlas, cluster `MainDatabase`, database `mchub`
- **Search:** Elasticsearch (voice-lesson full-text search, BM25 — see [BM25_SEARCH.md](./BM25_SEARCH.md))
- **AI service:** external Python FastAPI service (see `HF-Space-Deploy`/`TrainingAiSample` repos) called via `VoiceController`'s proxy endpoints
- **Payment:** PayOS + MBBank VietQR
- **Swagger UI:** `http://localhost:<PORT>/swagger-ui/index.html` (ADMIN role required)
- **Production:** deployed on Render, free tier — auto-sleeps after ~15min idle, first request after sleep takes 30-60s to wake up

## Commands

```bash
mvn spring-boot:run          # dev server
mvn compile                  # also regenerates MapStruct mappers
mvn clean package            # full build + tests
mvn test                     # run all tests
mvn test -Dtest=AuthControllerTest
```

Copy `.env.example` → `.env` and fill in `MONGODB_URI`, `JWT_SECRET`, `CLOUDINARY_*`, `PAYOS_*`, `MBBANK_*`, `AI_ANALYZE_URL`/`AI_TTS_URL`, `BREVO_SMTP_KEY`, `ELASTICSEARCH_URIS`, `ALLOWED_ORIGINS` before running. There is no Spring test profile — the app always connects to whatever `MONGODB_URI` points at, even during manual QA against `MONGODB_TEST_URI`.

## Architecture at a glance

Controller → Service → Repository, MapStruct for Entity↔DTO mapping — but **not strictly enforced**: `VoucherController`, `QuestController`, and `UserHighlightController` have no service layer at all, and several others (`AuthController`, `PaymentController`, `AdminController`, `VoiceController`, `CertificateController`) inject repositories directly alongside their service. See [CLAUDE.md](./CLAUDE.md) for the full breakdown.

All endpoints return the standard envelope:
```json
{ "status": "success", "message": "...", "data": { ... } }
{ "status": "fail",    "message": "...", "data": null }
```

25 controllers, ~130 endpoints, 30 MongoDB collections + 1 Elasticsearch index. Full endpoint table lives in [CLAUDE.md](./CLAUDE.md) — grep `src/main/java/com/mchub/controllers/` directly if you need exact, current signatures.

## Testing

`src/test/java/com/mchub/` — 46 files, ~493 `@Test` methods, JUnit 5 + Mockito (`@MockBean`), no real or embedded database is touched by any test despite `flapdoodle` (embedded MongoDB) being declared in `pom.xml`.

## Seed data (`seed-data/`)

- **`seed_voice_lessons.js`** — seeds 20 sample `voice_lessons` documents with category-matched Unsplash thumbnails. **Runs `use("voice-tranning")`, not `use("mchub")`** — check which database name your `mongosh` connection string actually targets before running; if you intend to seed the real `mchub` database (the one `.env`'s `MONGODB_URI` points at), edit the `use(...)` line first or the data lands in the wrong database.
- **`patch_thumbnails.js`** — non-destructive update: backfills `thumbnailUrl` on existing lessons that don't have one, by category. Same `use("voice-tranning")` caveat applies.
- **There is no seed script for most of the other 29 MongoDB collections** (`users`, `courses`, `payment_transactions`, etc.) in this repo — if you need broader seed data, check whether it exists elsewhere before writing one from scratch, and make sure any new script's `use(...)` line matches the database you actually intend to seed.

⚠️ A third script, `seed_mchub.js`, previously lived in this directory and has been removed — it was `The-MC-Hub-Java-Backend`'s booking-platform seed data (collections `bookings`, `schedules`, `messages`, `scripts`, `favorites`, `coupons` — none of which exist in this repo's schema), copied here by mistake. It ran `deleteMany({})` on collection names before inserting wrong-schema data. If you find a similar file reappear (e.g. from an old branch merge), do not run it against this repo's database.

## Further reference

- [CLAUDE.md](./CLAUDE.md) — full architecture, endpoint table, divergence from baseline
- [JAVA_BACKEND_GUIDE.md at The-MC-Hub-Java-Backend](../The-MC-Hub-Java-Backend/JAVA_BACKEND_GUIDE.md) — general Spring Boot conventions this repo was forked from (booking-domain examples in that guide do not apply here)
- [BACKEND_PERFORMANCE_GUIDE.md](./BACKEND_PERFORMANCE_GUIDE.md) — mandatory performance patterns (no `.size()` on large lists, `@Aggregation` over Streams, batch-fetch, `CompletableFuture` for multi-source stats)
- [BM25_SEARCH.md](./BM25_SEARCH.md) — Elasticsearch voice-lesson search + MongoDB fallback
- [AI_CODEGEN_GUIDE.md](./AI_CODEGEN_GUIDE.md) — GitNexus + Caveman Mode + performance rules for AI agents working in this repo
