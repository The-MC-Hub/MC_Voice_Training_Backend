---
name: services
description: "Skill for the Services area of The-MC-Hub-Java-Backend. 84 symbols across 49 files."
---

# Services

84 symbols | 49 files | Cohesion: 84%

## When to Use

- Working with code in `src/`
- Understanding how ScriptServiceImpl, ReviewServiceImpl, ReportServiceImpl work
- Modifying services-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/mchub/services/ReportService.java` | createReport, resolveReport, getMyReports, getPendingReports, getAllReports (+1) |
| `src/main/java/com/mchub/services/impl/ReportServiceImpl.java` | createReport, resolveReport, getMyReports, getPendingReports, getAllReports (+1) |
| `src/main/java/com/mchub/controllers/ReportController.java` | createReport, getMyReports, resolveReport, getAllReports |
| `src/main/java/com/mchub/services/CertificateService.java` | addCertificate, verifyCertificate, getCertificatesByMCProfile, CertificateService |
| `src/main/java/com/mchub/services/impl/CertificateServiceImpl.java` | addCertificate, verifyCertificate, getCertificatesByMCProfile, CertificateServiceImpl |
| `src/main/java/com/mchub/services/BookingDetailService.java` | createOrUpdate, updateMcNotes, findByBookingId, BookingDetailService |
| `src/main/java/com/mchub/services/impl/BookingDetailServiceImpl.java` | createOrUpdate, updateMcNotes, findByBookingId, BookingDetailServiceImpl |
| `src/main/java/com/mchub/controllers/CertificateController.java` | addCertificate, getCertificates, verify |
| `src/main/java/com/mchub/controllers/BookingDetailController.java` | getDetail, createOrUpdate, updateMcNotes |
| `src/main/java/com/mchub/services/AuditLogService.java` | getUserLogs, getAllLogs, AuditLogService |

## Entry Points

Start here when exploring this area:

- **`ScriptServiceImpl`** (Class) — `src/main/java/com/mchub/services/impl/ScriptServiceImpl.java:15`
- **`ReviewServiceImpl`** (Class) — `src/main/java/com/mchub/services/impl/ReviewServiceImpl.java:18`
- **`ReportServiceImpl`** (Class) — `src/main/java/com/mchub/services/impl/ReportServiceImpl.java:17`
- **`PublicServiceImpl`** (Class) — `src/main/java/com/mchub/services/impl/PublicServiceImpl.java:23`
- **`PayOSServiceImpl`** (Class) — `src/main/java/com/mchub/services/impl/PayOSServiceImpl.java:25`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `ScriptServiceImpl` | Class | `src/main/java/com/mchub/services/impl/ScriptServiceImpl.java` | 15 |
| `ReviewServiceImpl` | Class | `src/main/java/com/mchub/services/impl/ReviewServiceImpl.java` | 18 |
| `ReportServiceImpl` | Class | `src/main/java/com/mchub/services/impl/ReportServiceImpl.java` | 17 |
| `PublicServiceImpl` | Class | `src/main/java/com/mchub/services/impl/PublicServiceImpl.java` | 23 |
| `PayOSServiceImpl` | Class | `src/main/java/com/mchub/services/impl/PayOSServiceImpl.java` | 25 |
| `NotificationServiceImpl` | Class | `src/main/java/com/mchub/services/impl/NotificationServiceImpl.java` | 18 |
| `MCProfileServiceImpl` | Class | `src/main/java/com/mchub/services/impl/MCProfileServiceImpl.java` | 17 |
| `JwtServiceImpl` | Class | `src/main/java/com/mchub/services/impl/JwtServiceImpl.java` | 19 |
| `FavoriteServiceImpl` | Class | `src/main/java/com/mchub/services/impl/FavoriteServiceImpl.java` | 14 |
| `CouponServiceImpl` | Class | `src/main/java/com/mchub/services/impl/CouponServiceImpl.java` | 16 |
| `ChatServiceImpl` | Class | `src/main/java/com/mchub/services/impl/ChatServiceImpl.java` | 21 |
| `CertificateServiceImpl` | Class | `src/main/java/com/mchub/services/impl/CertificateServiceImpl.java` | 17 |
| `BookingServiceImpl` | Class | `src/main/java/com/mchub/services/impl/BookingServiceImpl.java` | 23 |
| `BookingDetailServiceImpl` | Class | `src/main/java/com/mchub/services/impl/BookingDetailServiceImpl.java` | 15 |
| `AvailabilityServiceImpl` | Class | `src/main/java/com/mchub/services/impl/AvailabilityServiceImpl.java` | 15 |
| `AuthServiceImpl` | Class | `src/main/java/com/mchub/services/impl/AuthServiceImpl.java` | 24 |
| `AuditLogServiceImpl` | Class | `src/main/java/com/mchub/services/impl/AuditLogServiceImpl.java` | 17 |
| `AdminServiceImpl` | Class | `src/main/java/com/mchub/services/impl/AdminServiceImpl.java` | 26 |
| `ScriptService` | Interface | `src/main/java/com/mchub/services/ScriptService.java` | 11 |
| `ReviewService` | Interface | `src/main/java/com/mchub/services/ReviewService.java` | 12 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `CreateReport → ToString` | cross_community | 3 |
| `AddCertificate → ToString` | cross_community | 3 |
| `AddCertificate → GetDefaultMessage` | cross_community | 3 |
| `ResolveReport → ToString` | cross_community | 3 |
| `CreateOrUpdate → FindByBookingId` | intra_community | 3 |
| `UpdateMcNotes → FindByBookingId` | intra_community | 3 |
| `Verify → ToString` | cross_community | 3 |
| `GetAllReports → FindByStatus` | intra_community | 3 |
| `GetMyReports → ToString` | cross_community | 3 |
| `GetMyReports → FindByReporterId` | intra_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Controllers | 11 calls |

## How to Explore

1. `gitnexus_context({name: "ScriptServiceImpl"})` — see callers and callees
2. `gitnexus_query({query: "services"})` — find related execution flows
3. Read key files listed above for implementation details
