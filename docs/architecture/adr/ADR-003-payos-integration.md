# ADR-003: Integration of PayOS as Payment Gateway

## Status
Accepted

## Date
2026-02-10

## Context
The platform requires automated VietQR payment links for course enrollment, VIP plan subscriptions, and MC booking deposits without manual bank reconciliation.

## Decision
We integrate **PayOS** gateway via standard HTTP REST APIs and HMAC SHA-256 webhook callbacks.

## Consequences
- **Positive**: Zero transaction fee structure for domestic VietQR transfers, instant webhook notifications, automated QR rendering.
- **Negative**: Dependency on external PayOS gateway availability; requires robust HMAC signature verification to prevent spoofed callbacks.
