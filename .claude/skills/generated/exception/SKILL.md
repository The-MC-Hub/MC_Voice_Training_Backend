---
name: exception
description: "Skill for the Exception area of The-MC-Hub-Java-Backend. 13 symbols across 3 files."
---

# Exception

13 symbols | 3 files | Cohesion: 100%

## When to Use

- Working with code in `src/`
- Understanding how handleAppException, handleValidationException, handleConstraintViolation work
- Modifying exception-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | handleAppException, handleValidationException, handleConstraintViolation, handleAccessDeniedException, handleAuthenticationException (+4) |
| `src/main/java/com/mchub/exception/ErrorCode.java` | getHttpStatus, getCode, getDefaultMessage |
| `src/main/java/com/mchub/exception/AppException.java` | getErrorCode |

## Entry Points

Start here when exploring this area:

- **`handleAppException`** (Method) — `src/main/java/com/mchub/exception/GlobalExceptionHandler.java:25`
- **`handleValidationException`** (Method) — `src/main/java/com/mchub/exception/GlobalExceptionHandler.java:37`
- **`handleConstraintViolation`** (Method) — `src/main/java/com/mchub/exception/GlobalExceptionHandler.java:50`
- **`handleAccessDeniedException`** (Method) — `src/main/java/com/mchub/exception/GlobalExceptionHandler.java:63`
- **`handleAuthenticationException`** (Method) — `src/main/java/com/mchub/exception/GlobalExceptionHandler.java:72`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `handleAppException` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 25 |
| `handleValidationException` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 37 |
| `handleConstraintViolation` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 50 |
| `handleAccessDeniedException` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 63 |
| `handleAuthenticationException` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 72 |
| `handleNotFound` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 82 |
| `handleMethodNotAllowed` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 92 |
| `handleGenericException` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 100 |
| `buildErrorResponse` | Method | `src/main/java/com/mchub/exception/GlobalExceptionHandler.java` | 110 |
| `getHttpStatus` | Method | `src/main/java/com/mchub/exception/ErrorCode.java` | 92 |
| `getCode` | Method | `src/main/java/com/mchub/exception/ErrorCode.java` | 96 |
| `getDefaultMessage` | Method | `src/main/java/com/mchub/exception/ErrorCode.java` | 100 |
| `getErrorCode` | Method | `src/main/java/com/mchub/exception/AppException.java` | 49 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `CreateBooking → GetDefaultMessage` | cross_community | 4 |
| `UpdateStatus → GetDefaultMessage` | cross_community | 4 |
| `CancelBooking → GetDefaultMessage` | cross_community | 4 |
| `AddCertificate → GetDefaultMessage` | cross_community | 3 |
| `FavoriteScript → GetDefaultMessage` | cross_community | 3 |
| `Toggle → GetDefaultMessage` | cross_community | 3 |
| `DeleteCertificate → GetDefaultMessage` | cross_community | 3 |
| `GetBookingById → GetDefaultMessage` | cross_community | 3 |
| `GetScript → GetDefaultMessage` | cross_community | 3 |
| `GetConversation → GetDefaultMessage` | cross_community | 3 |

## How to Explore

1. `gitnexus_context({name: "handleAppException"})` — see callers and callees
2. `gitnexus_query({query: "exception"})` — find related execution flows
3. Read key files listed above for implementation details
