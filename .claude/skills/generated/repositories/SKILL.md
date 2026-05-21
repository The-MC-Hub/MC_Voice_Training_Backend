---
name: repositories
description: "Skill for the Repositories area of The-MC-Hub-Java-Backend. 11 symbols across 11 files."
---

# Repositories

11 symbols | 11 files | Cohesion: 79%

## When to Use

- Working with code in `src/`
- Understanding how MCProfile, findByMc, findByUser work
- Modifying repositories-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/mchub/repositories/ReviewRepository.java` | findByMc |
| `src/main/java/com/mchub/repositories/MCProfileRepository.java` | findByUser |
| `src/main/java/com/mchub/models/MCProfile.java` | MCProfile |
| `src/main/java/com/mchub/services/MCProfileService.java` | updateProfile |
| `src/main/java/com/mchub/controllers/MCController.java` | updateProfile |
| `src/main/java/com/mchub/services/impl/ReviewServiceImpl.java` | updateMCProfileRatingAsync |
| `src/main/java/com/mchub/services/impl/MCProfileServiceImpl.java` | updateProfile |
| `src/main/java/com/mchub/repositories/FavoriteRepository.java` | countByMcUserId |
| `src/main/java/com/mchub/services/impl/FavoriteServiceImpl.java` | getMCFavoriteCount |
| `src/main/java/com/mchub/repositories/ConversationRepository.java` | findExisting |

## Entry Points

Start here when exploring this area:

- **`MCProfile`** (Class) — `src/main/java/com/mchub/models/MCProfile.java:22`
- **`findByMc`** (Method) — `src/main/java/com/mchub/repositories/ReviewRepository.java:15`
- **`findByUser`** (Method) — `src/main/java/com/mchub/repositories/MCProfileRepository.java:15`
- **`updateProfile`** (Method) — `src/main/java/com/mchub/services/MCProfileService.java:21`
- **`updateProfile`** (Method) — `src/main/java/com/mchub/controllers/MCController.java:29`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `MCProfile` | Class | `src/main/java/com/mchub/models/MCProfile.java` | 22 |
| `findByMc` | Method | `src/main/java/com/mchub/repositories/ReviewRepository.java` | 15 |
| `findByUser` | Method | `src/main/java/com/mchub/repositories/MCProfileRepository.java` | 15 |
| `updateProfile` | Method | `src/main/java/com/mchub/services/MCProfileService.java` | 21 |
| `updateProfile` | Method | `src/main/java/com/mchub/controllers/MCController.java` | 29 |
| `updateMCProfileRatingAsync` | Method | `src/main/java/com/mchub/services/impl/ReviewServiceImpl.java` | 46 |
| `updateProfile` | Method | `src/main/java/com/mchub/services/impl/MCProfileServiceImpl.java` | 59 |
| `countByMcUserId` | Method | `src/main/java/com/mchub/repositories/FavoriteRepository.java` | 16 |
| `getMCFavoriteCount` | Method | `src/main/java/com/mchub/services/impl/FavoriteServiceImpl.java` | 44 |
| `findExisting` | Method | `src/main/java/com/mchub/repositories/ConversationRepository.java` | 23 |
| `createConversationForBooking` | Method | `src/main/java/com/mchub/services/impl/ChatServiceImpl.java` | 74 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `CreateReview → FindByMc` | cross_community | 5 |
| `CreateReview → FindByUser` | cross_community | 5 |
| `UpdateProfile → ToString` | cross_community | 3 |
| `UpdateProfile → FindByUser` | intra_community | 3 |
| `UpdateProfile → MCProfile` | intra_community | 3 |
| `GetMCReviews → FindByMc` | cross_community | 3 |
| `GetMCProfile → FindByUser` | cross_community | 3 |
| `GetDashboard → FindByUser` | cross_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Controllers | 2 calls |

## How to Explore

1. `gitnexus_context({name: "MCProfile"})` — see callers and callees
2. `gitnexus_query({query: "repositories"})` — find related execution flows
3. Read key files listed above for implementation details
