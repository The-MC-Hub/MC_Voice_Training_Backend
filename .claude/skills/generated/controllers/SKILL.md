---
name: controllers
description: "Skill for the Controllers area of The-MC-Hub-Java-Backend. 292 symbols across 90 files."
---

# Controllers

292 symbols | 90 files | Cohesion: 74%

## When to Use

- Working with code in `src/`
- Understanding how Notification, Conversation, Script work
- Modifying controllers-related functionality

## Key Files

| File | Symbols |
|------|---------|
| `src/test/java/com/mchub/controllers/ChatControllerTest.java` | sendMessage_emptyContent, markAsRead_success, stubConversation, getConversations_success, getConversations_empty (+7) |
| `src/test/java/com/mchub/controllers/BookingControllerTest.java` | stubBooking, getMyBookings_client_success, getMyBookings_mc_success, baseCreateRequest, createBooking_success (+7) |
| `src/test/java/com/mchub/controllers/ReviewControllerTest.java` | baseRequest, createReview_success, createReview_bookingNotCompleted, createReview_alreadyExists, stubReview (+5) |
| `src/test/java/com/mchub/controllers/PublicControllerTest.java` | stubProfile, discoverMCs_noFilter, discoverMCs_withCategoryFilter, discoverMCs_withPriceFilter, discoverMCs_noResults (+5) |
| `src/test/java/com/mchub/controllers/NotificationControllerTest.java` | stubNotif, getNotifications_success, getNotifications_empty, getNotifications_customPagination, getUnreadCount_success (+4) |
| `src/test/java/com/mchub/controllers/CouponControllerTest.java` | stubCoupon, createCoupon_adminSuccess, createCoupon_forbiddenForClient, toggleCoupon_adminSuccess, validateCoupon_valid (+4) |
| `src/test/java/com/mchub/controllers/AdminControllerTest.java` | stubUserDTO, getAllUsers_success, getDashboard_adminSuccess, getDashboard_forbiddenForClient, getAllMCs_success (+4) |
| `src/main/java/com/mchub/services/impl/BookingServiceImpl.java` | getClientBookings, getMCBookings, createBooking, sendBookingNotifications, initializeChatRoom (+3) |
| `src/test/java/com/mchub/controllers/ScriptControllerTest.java` | stubScript, getAllScripts_noFilter, getAllScripts_withCategory, getAllScripts_empty, favoriteScript_success (+3) |
| `src/test/java/com/mchub/controllers/AvailabilityControllerTest.java` | deleteAvailability_success, deleteAvailability_notFound, deleteAvailability_notOwner, stubSchedule, createAvailability_success (+2) |

## Entry Points

Start here when exploring this area:

- **`Notification`** (Class) — `src/main/java/com/mchub/models/Notification.java:18`
- **`Conversation`** (Class) — `src/main/java/com/mchub/models/Conversation.java:20`
- **`Script`** (Class) — `src/main/java/com/mchub/models/Script.java:20`
- **`CreateReviewRequest`** (Class) — `src/main/java/com/mchub/dto/CreateReviewRequest.java:13`
- **`MCProfileResponseDTO`** (Class) — `src/main/java/com/mchub/dto/MCProfileResponseDTO.java:11`

## Key Symbols

| Symbol | Type | File | Line |
|--------|------|------|------|
| `Notification` | Class | `src/main/java/com/mchub/models/Notification.java` | 18 |
| `Conversation` | Class | `src/main/java/com/mchub/models/Conversation.java` | 20 |
| `Script` | Class | `src/main/java/com/mchub/models/Script.java` | 20 |
| `CreateReviewRequest` | Class | `src/main/java/com/mchub/dto/CreateReviewRequest.java` | 13 |
| `MCProfileResponseDTO` | Class | `src/main/java/com/mchub/dto/MCProfileResponseDTO.java` | 11 |
| `Coupon` | Class | `src/main/java/com/mchub/models/Coupon.java` | 22 |
| `CreateCouponRequest` | Class | `src/main/java/com/mchub/dto/CreateCouponRequest.java` | 17 |
| `Review` | Class | `src/main/java/com/mchub/models/Review.java` | 17 |
| `Favorite` | Class | `src/main/java/com/mchub/models/Favorite.java` | 19 |
| `CreateBookingRequest` | Class | `src/main/java/com/mchub/dto/CreateBookingRequest.java` | 16 |
| `Message` | Class | `src/main/java/com/mchub/models/Message.java` | 20 |
| `User` | Class | `src/main/java/com/mchub/models/User.java` | 20 |
| `LoginRequest` | Class | `src/main/java/com/mchub/dto/LoginRequest.java` | 11 |
| `UpdateBookingStatusRequest` | Class | `src/main/java/com/mchub/dto/UpdateBookingStatusRequest.java` | 11 |
| `Schedule` | Class | `src/main/java/com/mchub/models/Schedule.java` | 18 |
| `UserResponseDTO` | Class | `src/main/java/com/mchub/dto/UserResponseDTO.java` | 10 |
| `Booking` | Class | `src/main/java/com/mchub/models/Booking.java` | 20 |
| `countByUserAndIsReadFalse` | Method | `src/main/java/com/mchub/repositories/NotificationRepository.java` | 19 |
| `getUserNotifications` | Method | `src/main/java/com/mchub/services/NotificationService.java` | 16 |
| `getUnreadCount` | Method | `src/main/java/com/mchub/services/NotificationService.java` | 19 |

## Execution Flows

| Flow | Type | Steps |
|------|------|-------|
| `CreateReview → FindByMc` | cross_community | 5 |
| `CreateReview → FindByUser` | cross_community | 5 |
| `CreateBooking → FindByMcAndStatus` | cross_community | 4 |
| `CreateBooking → IsOverlapping` | cross_community | 4 |
| `CreateBooking → GetDefaultMessage` | cross_community | 4 |
| `UpdateStatus → GetDefaultMessage` | cross_community | 4 |
| `CancelBooking → GetDefaultMessage` | cross_community | 4 |
| `HandlePayOSWebhook → FindByPayosOrderCode` | cross_community | 4 |
| `CreateReview → ToString` | cross_community | 3 |
| `CreateCoupon → ToString` | cross_community | 3 |

## Connected Areas

| Area | Connections |
|------|-------------|
| Services | 28 calls |
| Impl | 7 calls |
| Repositories | 3 calls |

## How to Explore

1. `gitnexus_context({name: "Notification"})` — see callers and callees
2. `gitnexus_query({query: "controllers"})` — find related execution flows
3. Read key files listed above for implementation details
