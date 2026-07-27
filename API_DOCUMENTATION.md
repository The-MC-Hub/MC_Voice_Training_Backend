# API Documentation & OpenAPI Specification Index

Document Version: 2.0.0
Base URL: `/api/v1`
Interactive Swagger UI: `http://localhost:<PORT>/swagger-ui/index.html` (Requires ADMIN authentication)

---

## 1. Authentication & Security Headers

All protected endpoints require the HTTP Authorization Header:
```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

---

## 2. API Module Catalog & Complete Use Case Mapping

For exhaustive endpoint specifications, Class Diagrams, Sequence Diagrams, and Step-by-Step Test Verification Reports, consult the 80 Sub-UC document files located under `docs/use-cases/`:

| Sub-System Module | Base Path | Endpoints Count | Technical Reference Link |
|---|---|:---:|---|
| **01. Authentication** | `/api/v1/auth` | 6 APIs | [uc-01-authentication](../docs/use-cases/uc-01-authentication/README.md) |
| **02. User Profile** | `/api/v1/users` | 6 APIs | [uc-02-user-profile](../docs/use-cases/uc-02-user-profile/README.md) |
| **03. Voice Training** | `/api/v1/voice` | 6 APIs | [uc-03-voice-training](../docs/use-cases/uc-03-voice-training/README.md) |
| **04. Courses & Learning** | `/api/v1/courses` | 6 APIs | [uc-04-courses-learning](../docs/use-cases/uc-04-courses-learning/README.md) |
| **05. Community & Leaderboard**| `/api/v1/community` | 6 APIs | [uc-05-community-leaderboard](../docs/use-cases/uc-05-community-leaderboard/README.md) |
| **06. Payment & Subscription** | `/api/v1/payments` | 7 APIs | [uc-06-payment-subscription](../docs/use-cases/uc-06-payment-subscription/README.md) |
| **07. Onboarding Quest** | `/api/v1/quests` | 2 APIs | [uc-07-onboarding-quest](../docs/use-cases/uc-07-onboarding-quest/README.md) |
| **08. Support & Public** | `/api/v1/public` | 4 APIs | [uc-08-support-public](../docs/use-cases/uc-08-support-public/README.md) |
| **09. Admin Dashboard** | `/api/v1/admin` | 10 APIs | [uc-09-admin-dashboard](../docs/use-cases/uc-09-admin-dashboard/README.md) |
| **10. Marketing Communication**| `/api/v1/admin/email` | 4 APIs | [uc-10-marketing-communication](../docs/use-cases/uc-10-marketing-communication/README.md) |
| **11. MC Booking & Hiring** | `/api/v1/bookings` | 6 APIs | [uc-11-mc-booking-hiring](../docs/use-cases/uc-11-mc-booking-hiring/README.md) |
| **12. Chat & Messaging** | `/api/v1/chats` | 6 APIs | [uc-12-chat-messaging](../docs/use-cases/uc-12-chat-messaging/README.md) |
| **13. Peer Review** | `/api/v1/peer-reviews` | 5 APIs | [uc-13-peer-review](../docs/use-cases/uc-13-peer-review/README.md) |
| **14. Announcement & Banner** | `/api/v1/announcements` | 2 APIs | [uc-14-announcement-banner](../docs/use-cases/uc-14-announcement-banner/README.md) |
| **15. CV & Portfolio** | `/api/v1/cv` | 3 APIs | [uc-15-cv-portfolio](../docs/use-cases/uc-15-cv-portfolio/README.md) |
| **16. Gamification & Minigame**| `/api/v1/minigames` | 3 APIs | [uc-16-gamification-minigame](../docs/use-cases/uc-16-gamification-minigame/README.md) |
| **17. Availability Calendar** | `/api/v1/availability` | 3 APIs | [uc-17-mc-availability-calendar](../docs/use-cases/uc-17-mc-availability-calendar/README.md) |
| **18. Content Moderation** | `/api/v1/admin/moderation`| 3 APIs | [uc-18-admin-content-moderation](../docs/use-cases/uc-18-admin-content-moderation/README.md) |

---

## 3. Standard Response Envelope Format

### 3.1 Success Envelope (HTTP 200 / 201)
```json
{
  "status": "success",
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### 3.2 Error Envelope (HTTP 4xx / 5xx)
```json
{
  "status": "fail",
  "message": "Detailed error description",
  "data": {
    "errorCode": "VALIDATION_FAILED"
  }
}
```
