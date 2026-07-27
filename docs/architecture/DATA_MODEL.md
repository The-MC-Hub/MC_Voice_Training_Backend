# Architecture Document: Data Model & Database Schema Specification

Document Version: 2.0.0
Database Target: MongoDB Atlas (`mchub` database)
Total Collections: 46 Document Models

---

## 1. Complete Collection Catalog & Index Registry

| Collection Name | Document Class | Primary Key | Key Indexes & Options | Purpose |
|---|---|---|---|---|
| `users` | `User.java` | `_id` | `email` (Unique, Asc), `role` (Asc), `createdAt` (Desc) | Account credentials & core profile |
| `user_stats` | `UserStats.java` | `_id` | `userId` (Unique, Asc), `totalXp` (Desc), `currentStreak` (Desc) | Leaderboard & gamification stats |
| `client_profiles` | `ClientProfile.java` | `_id` | `userId` (Unique, Asc), `companyName` (Asc) | Employer / Client profile data |
| `mc_profiles` | `MCProfile.java` | `_id` | `userId` (Unique, Asc), `categories` (Multikey), `rating` (Desc) | MC Directory & discovery search |
| `voice_lessons` | `VoiceLesson.java` | `_id` | `category` (Asc), `difficulty` (Asc), `isPublic` (Asc) | Practice lesson script metadata |
| `practice_sessions` | `PracticeSession.java`| `_id` | `userId` (Asc), `lessonId` (Asc), `createdAt` (Desc) | Voice scoring attempt history |
| `lesson_adaptive_stats`| `LessonAdaptiveStats.java`| `_id` | `userId` (Asc) + `lessonId` (Asc) (Compound Unique) | Per-user adaptive lesson performance |
| `guest_voice_usages` | `GuestVoiceUsage.java`| `_id` | `ipAddress` (Asc), `createdAt` (Desc) | Guest practice trial cooldown tracker |
| `courses` | `Course.java` | `_id` | `title` (Text), `category` (Asc), `isPublished` (Asc) | Academy courses & lesson structures |
| `course_enrollments` | `CourseEnrollment.java` | `_id` | `userId` (Asc) + `courseId` (Asc) (Compound Unique) | Course enrollment & progress records |
| `certificates` | `Certificate.java` | `_id` | `certificateCode` (Unique, Asc), `userId` (Asc) | Issued course completion certs |
| `bookings` | `Booking.java` | `_id` | `clientId` (Asc), `mcId` (Asc), `status` (Asc), `eventDate` (Desc) | Show booking transactions |
| `booking_details` | `BookingDetail.java` | `_id` | `bookingId` (Unique, Asc) | Detailed event parameters & requirements |
| `schedules` | `Schedule.java` | `_id` | `mcId` (Asc), `startTime` (Asc), `endTime` (Asc) | MC availability calendar slots |
| `payment_transactions`| `PaymentTransaction.java`| `_id` | `orderCode` (Unique, Asc), `userId` (Asc), `status` (Asc) | PayOS payments & ledger |
| `plan_definitions` | `PlanDefinition.java` | `_id` | `planCode` (Unique, Asc) | VIP subscription plan configurations |
| `conversations` | `Conversation.java` | `_id` | `participantIds` (Multikey, Asc), `updatedAt` (Desc) | In-app chat threads |
| `messages` | `Message.java` | `_id` | `conversationId` (Asc), `createdAt` (Desc) | In-app chat messages |
| `competitions` | `Competition.java` | `_id` | `status` (Asc), `startDate` (Asc), `endDate` (Desc) | Voice Arena competitions |
| `competition_records` | `CompetitionRecord.java`| `_id` | `competitionId` (Asc) + `userId` (Asc) (Compound Unique) | User contest entries & scoring |
| `user_vouchers` | `UserVoucher.java` | `_id` | `userId` (Asc), `isUsed` (Asc), `expiresAt` (Asc) | Gamification voucher wallet |
| `discount_codes` | `DiscountCode.java` | `_id` | `code` (Unique, Asc), `isActive` (Asc) | System promotional promo codes |
| `reports` | `Report.java` | `_id` | `reporterId` (Asc), `status` (Asc), `createdAt` (Desc) | Moderation content reports |
| `audit_logs` | `AuditLog.java` | `_id` | `adminId` (Asc), `action` (Asc), `timestamp` (Desc) | System administrative audit logs |
| `system_logs` | `SystemLog.java` | `_id` | `level` (Asc), `timestamp` (Desc) | Application runtime execution logs |
| `announcements` | `Announcement.java` | `_id` | `isActive` (Asc), `publishedAt` (Desc) | In-app system announcements |

---

## 2. Exhaustive Schema Specifications (Core Entities)

### 2.1 `User` Entity (`users` Collection)
```json
{
  "_id": { "$oid": "66a01b2c3d4e5f6789012300" },
  "fullName": "Nguyen Van A",
  "email": "user@mchub.vn",
  "passwordHash": "$2a$10$e8N3vU... (BCrypt 10 rounds)",
  "role": "USER",
  "plan": "FREE",
  "avatarUrl": "https://res.cloudinary.com/mchub/avatars/user_101.jpg",
  "bio": "Học viên MC truyền hình chuyên nghiệp",
  "isActive": true,
  "is2FAEnabled": false,
  "twoFASecret": null,
  "createdAt": { "$date": "2026-01-15T08:00:00.000Z" },
  "updatedAt": { "$date": "2026-07-27T10:30:00.000Z" }
}
```

### 2.2 `PracticeSession` Entity (`practice_sessions` Collection)
```json
{
  "_id": { "$oid": "66a01b2c3d4e5f6789012355" },
  "userId": "66a01b2c3d4e5f6789012300",
  "lessonId": "66a01b2c3d4e5f6789012310",
  "audioUrl": "https://res.cloudinary.com/mchub/voice-recordings/rec_99.wav",
  "overallScore": 88.5,
  "pronunciationScore": 90.0,
  "intonationScore": 85.0,
  "speedPacingScore": 89.0,
  "accuracyScore": 90.0,
  "durationSeconds": 45.2,
  "aiFeedback": "Giọng đọc tròn vành rõ chữ, nhịp điệu ổn định.",
  "createdAt": { "$date": "2026-07-27T14:20:00.000Z" }
}
```

### 2.3 `Booking` Entity (`bookings` Collection)
```json
{
  "_id": { "$oid": "66a01b2c3d4e5f6789012400" },
  "clientId": "66a01b2c3d4e5f6789012300",
  "mcId": "66a01b2c3d4e5f6789012305",
  "status": "CONFIRMED",
  "totalAmount": 5000000.0,
  "depositAmount": 2000000.0,
  "eventDate": { "$date": "2026-08-15T09:00:00.000Z" },
  "createdAt": { "$date": "2026-07-25T11:00:00.000Z" },
  "updatedAt": { "$date": "2026-07-26T15:30:00.000Z" }
}
```

### 2.4 `PaymentTransaction` Entity (`payment_transactions` Collection)
```json
{
  "_id": { "$oid": "66a01b2c3d4e5f6789012500" },
  "userId": "66a01b2c3d4e5f6789012300",
  "orderCode": 1722080001,
  "amount": 299000.0,
  "currency": "VND",
  "status": "SUCCESS",
  "paymentMethod": "PAYOS_VIETQR",
  "description": "Nang cap tai khoan VIP 1 Thang",
  "payosTransactionId": "PAYOS_TX_998877",
  "createdAt": { "$date": "2026-07-27T11:15:00.000Z" }
}
```

---

## 3. Database Migration & Index Management Rules

1. **Auto Indexing Configuration**: `spring.data.mongodb.auto-index-creation=true` is enabled in development. In production, indexes are pre-built via MongoDB shell scripts during deployment.
2. **Compound Index Design Pattern**:
   - `practice_sessions`: Compound index on `{ userId: 1, createdAt: -1 }` to optimize user history retrieval queries.
   - `messages`: Compound index on `{ conversationId: 1, createdAt: -1 }` to support instant chat history pagination.
