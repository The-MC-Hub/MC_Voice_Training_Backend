# Technical Reference: Central Error Code Registry

Document Version: 1.0.0
Application Error Enum: `com.mchub.enums.ErrorCode` / System Exceptions

---

## 1. Error Envelope Structure

All application errors return an HTTP failure envelope:

```json
{
  "status": "fail",
  "message": "Human-readable error description",
  "data": {
    "errorCode": "ERROR_CODE_STRING",
    "timestamp": "2026-07-27T19:15:00Z"
  }
}
```

---

## 2. Complete Error Registry

| Error Code | HTTP Status | Description | Trigger Scenarios |
|---|---|---|---|
| `VALIDATION_FAILED` | 400 Bad Request | Invalid request body or parameter format | `@Valid` fails on request DTO |
| `EMAIL_ALREADY_EXISTS` | 400 Bad Request | Email address is already registered | `POST /auth/register` with duplicate email |
| `INVALID_CREDENTIALS` | 401 Unauthorized | Wrong email or password | `POST /auth/login` |
| `TOKEN_EXPIRED` | 401 Unauthorized | Expired JWT or Refresh Token | Accessing protected API with expired JWT |
| `UNAUTHORIZED` | 401 Unauthorized | Missing authentication token | Request without `Authorization: Bearer` header |
| `ACCESS_DENIED` | 403 Forbidden | User lacks necessary Role permissions | Non-admin accessing `/api/v1/admin/*` |
| `ACCOUNT_SUSPENDED` | 403 Forbidden | Account is currently temp-banned or frozen | Login attempt while `user.isActive = false` |
| `REQUIRE_2FA` | 403 Forbidden | Admin 2FA verification required | Login attempt on Admin account without 2FA code |
| `USER_NOT_FOUND` | 404 Not Found | Requested User ID does not exist | `GET /users/{id}` with bad ID |
| `LESSON_NOT_FOUND` | 404 Not Found | Voice Lesson ID does not exist | `GET /voice/lessons/{id}` |
| `RESOURCE_NOT_FOUND` | 404 Not Found | Generic target resource not found | Missing DB document lookup |
| `STREAK_FREEZE_UNAVAILABLE` | 400 Bad Request | Insufficient freeze count or invalid state | Using streak freeze when streak is inactive |
| `INSUFFICIENT_FUNDS` | 400 Bad Request | Balance or XP balance is lower than cost | Claiming reward without required XP |
| `INVALID_PAYMENT_SIGNATURE`| 400 Bad Request | PayOS HMAC signature verification failed | Spoofed or corrupt webhook delivery |
| `AI_SERVICE_UNAVAILABLE` | 503 Service Unavailable | FastAPI scoring engine unreachable/sleeping | Audio evaluation API timeout |
| `INTERNAL_SERVER_ERROR` | 500 Server Error | Unhandled runtime exception | Database failure or unhandled null pointer |
