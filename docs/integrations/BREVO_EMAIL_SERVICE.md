# Integration Specification: Brevo Email Dispatch Service

Document Version: 1.0.0
Target Service: Brevo Transactional & Marketing API (`https://api.brevo.com/v3`)

---

## 1. Overview & Authentication

Brevo handles all transactional notifications (OTP codes, password reset verification links, booking quote updates) and scheduled marketing campaigns.

### Environment Configuration
- `BREVO_SMTP_KEY`: API v3 Secret Key.
- `MAIL_FROM_ADDRESS`: Default sender email (`no-reply@mchub.vn`).
- `MAIL_FROM_NAME`: Default sender display name (`MC Hub Team`).

---

## 2. Supported Email Flows

| Flow Name | Trigger Endpoint | Template / Strategy | Async Thread |
|---|---|---|---|
| OTP Verification | `POST /api/v1/auth/register` | HTML Template (`otp-email.html`) | Sync via JavaMailSender |
| Password Reset | `POST /api/v1/auth/forgot-password` | HTML Template (`reset-password.html`) | Sync via JavaMailSender |
| Booking Quote Alert | `POST /api/v1/bookings` | Dynamic HTML Body | Async `CompletableFuture` |
| Email Campaign | `POST /api/v1/admin/email/send-campaign` | Brevo REST API v3 `/smtp/email` | Batch Async Task |

---

## 3. Rate Limits & Failures

- **Brevo Daily Limits**: Free tier allows up to 300 emails/day.
- **Fail-Safe Mechanism**: In case of SMTP auth failure or quota exhaustion, `EmailLog` records the failure state (`FAILED`) along with stacktrace for admin manual re-trigger.
