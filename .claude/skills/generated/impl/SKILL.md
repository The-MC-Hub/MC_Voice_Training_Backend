---
name: impl
description: "Skill for the Impl area of The-MC-Hub-Java-Backend. 42 symbols across 19 files."
---

# Impl

42 symbols | 19 files | Cohesion: 74%

## When to Use

- Working with code in `src/`
- Understanding how RegisterRequest, findByEmail, existsByEmail work
- Modifying impl-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/mchub/services/impl/JwtServiceImpl.java` | generateToken, generateToken, extractUserId, isTokenValid, isTokenExpired (+5) |
| `src/main/java/com/mchub/services/impl/AuthServiceImpl.java` | register, login, updatePasswordAsync, initializeMCProfile |
| `src/test/java/com/mchub/controllers/AuthControllerTest.java` | register_success, register_emailAlreadyExists, register_missingEmail |
| `src/main/java/com/mchub/services/JwtService.java` | generateToken, extractUserId, extractRole |
| `src/main/java/com/mchub/services/impl/AuditLogServiceImpl.java` | log, logError, getClientIp |
| `src/main/java/com/mchub/repositories/UserRepository.java` | findByEmail, existsByEmail |
| `src/main/java/com/mchub/controllers/AuthController.java` | register, login |
| `src/main/java/com/mchub/services/impl/BookingServiceImpl.java` | checkAvailabilityParallel, isOverlapping |
| `src/main/java/com/mchub/services/AuditLogService.java` | log, logError |
| `src/main/java/com/mchub/services/impl/PayOSServiceImpl.java` | handlePaymentWebhook, processSuccessfulPayment |

## Entry Points

Start here when exploring this area:

- **`RegisterRequest`** (Class) — `src/main/java/com/mchub/dto/RegisterRequest.java:13`
- **`findByEmail`** (Method) — `src/main/java/com/mchub/repositories/UserRepository.java:18`
- **`existsByEmail`** (Method) — `src/main/java/com/mchub/repositories/UserRepository.java:21`
- **`generateToken`** (Method) — `src/main/java/com/mchub/services/JwtService.java:16`
- **`register`** (Method) — `src/main/java/com/mchub/services/AuthService.java:13`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `RegisterRequest` | Class | `src/main/java/com/mchub/dto/RegisterRequest.java` | 13 |
| `findByEmail` | Method | `src/main/java/com/mchub/repositories/UserRepository.java` | 18 |
| `existsByEmail` | Method | `src/main/java/com/mchub/repositories/UserRepository.java` | 21 |
| `generateToken` | Method | `src/main/java/com/mchub/services/JwtService.java` | 16 |
| `register` | Method | `src/main/java/com/mchub/services/AuthService.java` | 13 |
| `register` | Method | `src/main/java/com/mchub/controllers/AuthController.java` | 30 |
| `generateToken` | Method | `src/main/java/com/mchub/services/impl/JwtServiceImpl.java` | 44 |
| `generateToken` | Method | `src/main/java/com/mchub/services/impl/JwtServiceImpl.java` | 59 |
| `register` | Method | `src/main/java/com/mchub/services/impl/AuthServiceImpl.java` | 33 |
| `login` | Method | `src/main/java/com/mchub/services/impl/AuthServiceImpl.java` | 69 |
| `updatePasswordAsync` | Method | `src/main/java/com/mchub/services/impl/AuthServiceImpl.java` | 96 |
| `initializeMCProfile` | Method | `src/main/java/com/mchub/services/impl/AuthServiceImpl.java` | 115 |
| `findByMcAndStatus` | Method | `src/main/java/com/mchub/repositories/BookingRepository.java` | 13 |
| `getDashboardStats` | Method | `src/main/java/com/mchub/services/MCProfileService.java` | 15 |
| `getDashboard` | Method | `src/main/java/com/mchub/controllers/MCController.java` | 21 |
| `getDashboardStats` | Method | `src/main/java/com/mchub/services/impl/MCProfileServiceImpl.java` | 24 |
| `checkAvailabilityParallel` | Method | `src/main/java/com/mchub/services/impl/BookingServiceImpl.java` | 126 |
| `isOverlapping` | Method | `src/main/java/com/mchub/services/impl/BookingServiceImpl.java` | 138 |
| `extractUserId` | Method | `src/main/java/com/mchub/services/JwtService.java` | 10 |
| `extractRole` | Method | `src/main/java/com/mchub/services/JwtService.java` | 12 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `IsTokenValid → GetSignInKey` | cross_community | 6 |
| `DoFilterInternal → GetSignInKey` | cross_community | 5 |
| `CreateBooking → FindByMcAndStatus` | cross_community | 4 |
| `CreateBooking → IsOverlapping` | cross_community | 4 |
| `HandlePayOSWebhook → FindByPayosOrderCode` | cross_community | 4 |
| `Register → ExistsByEmail` | intra_community | 3 |
| `Register → AppException` | intra_community | 3 |
| `Register → InitializeMCProfile` | intra_community | 3 |
| `Register → GetSignInKey` | cross_community | 3 |
| `CreateReview → ToString` | cross_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Controllers | 8 calls |
| Services | 1 calls |
| Repositories | 1 calls |

## How to Explore

1. `gitnexus_context({name: "RegisterRequest"})` — see callers and callees
2. `gitnexus_query({query: "impl"})` — find related execution flows
3. Read key files listed above for implementation details
