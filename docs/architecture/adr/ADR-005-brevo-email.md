# ADR-005: Transactional & Campaign Email Dispatch via Brevo API

## Status
Accepted

## Date
2026-02-20

## Context
The system dispatches high-volume transactional emails (OTP codes, password reset links, booking confirmations) and bulk marketing campaigns (newsletter updates, streak reminders).

## Decision
We adopt **Brevo** (formerly Sendinblue) as the unified email delivery platform utilizing SMTP relay and Brevo REST API endpoints.

## Consequences
- **Positive**: High deliverability rates, built-in template manager, native webhooks for bounce/open tracking.
- **Negative**: Daily quota limits on free/standard tiers; requires async dispatch (`CompletableFuture`) to avoid blocking request handling threads.
