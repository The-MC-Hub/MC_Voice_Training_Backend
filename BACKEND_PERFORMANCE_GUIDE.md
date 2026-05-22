# MC Voice Training Backend — Performance Optimization Guide

> **Version:** 2.0 — Updated 2026-05-22  
> **Scope:** Java 21 + Spring Boot 3.3 + MongoDB Atlas  
> This document defines mandatory performance patterns for the MC Voice Training Backend. Every engineer and AI assistant working on this codebase must read and apply these rules before writing service-layer code.

---

## Table of Contents

1. [The Three Performance Laws](#1-the-three-performance-laws)
2. [Parallel Queries with CompletableFuture](#2-parallel-queries-with-completablefuture)
3. [Eliminating N+1 Queries (Batch Fetching)](#3-eliminating-n1-queries-batch-fetching)
4. [MongoDB-side Aggregation (Never Java Streams)](#4-mongodb-side-aggregation-never-java-streams)
5. [Asynchronous Fire-and-Forget (@Async)](#5-asynchronous-fire-and-forget-async)
6. [Java 21 Virtual Threads](#6-java-21-virtual-threads)
7. [Response Size Optimization](#7-response-size-optimization)
8. [Anti-Pattern Reference](#8-anti-pattern-reference)
9. [Quick Checklist](#9-quick-checklist)

---

## 1. The Three Performance Laws

Before writing any service method that touches the database, ask yourself three questions:

**Law 1 — Can it be counted at the DB?**  
If you need a number (total users, count of sessions, etc.), never retrieve the full list just to call `.size()`. Use `repository.countBy...()` — MongoDB returns a single integer instead of fetching and deserializing thousands of documents.

**Law 2 — Can it run in parallel?**  
If your API endpoint needs data from 2+ independent sources (e.g., user stats + session history + lesson info), never fetch them sequentially. Run them concurrently with `CompletableFuture`.

**Law 3 — Are you looping over DB calls?**  
If you find yourself calling a repository method inside a `for` loop or `.stream().map()`, stop. Collect all IDs first, batch-fetch in one query, then map in memory.

---

## 2. Parallel Queries with CompletableFuture

### Problem

Dashboard and stats endpoints often need data from multiple independent collections. Sequential execution accumulates latency:

```
// ❌ BAD — 4 sequential DB calls, each waits for the previous
long sessions   = sessionRepository.countByUserId(userId);       // 30ms
double avgScore = ... compute from history ...;                  // 25ms
long lessons    = lessonRepository.count();                      // 20ms
int streak      = computeStreak(userId);                         // 15ms
// Total: 90ms
```

### Solution — CompletableFuture + allOf

```java
// ✅ GOOD — all 4 queries run concurrently
CompletableFuture<Long> sessionCount = CompletableFuture.supplyAsync(
    () -> sessionRepository.countByUserId(userId)
);
CompletableFuture<Double> avgScore = CompletableFuture.supplyAsync(
    () -> sessionRepository.findAverageScoreByUserId(userId)
);
CompletableFuture<Long> lessonCount = CompletableFuture.supplyAsync(
    () -> lessonRepository.count()
);
CompletableFuture<Integer> streak = CompletableFuture.supplyAsync(
    () -> computeStreak(userId)
);

// Wait for all results simultaneously
CompletableFuture.allOf(sessionCount, avgScore, lessonCount, streak).join();
// Total: ~30ms (the slowest single query), not 90ms
```

### Rule

**Any API endpoint that reads from 2 or more independent data sources must use `CompletableFuture`.**

This includes:
- Dashboard overview stats
- Landing page statistics
- User profile pages showing aggregated data
- Admin analytics endpoints

### AdminService — Full Example

```java
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final LessonRepository lessonRepository;

    @Override
    public DashboardStatsDTO getDashboardStats() {
        CompletableFuture<Long> totalUsers    = CompletableFuture.supplyAsync(
                () -> userRepository.count());
        CompletableFuture<Long> totalSessions = CompletableFuture.supplyAsync(
                () -> sessionRepository.count());
        CompletableFuture<Long> totalLessons  = CompletableFuture.supplyAsync(
                () -> lessonRepository.count());
        CompletableFuture<Double> avgScore    = CompletableFuture.supplyAsync(
                () -> sessionRepository.findPlatformAverageScore());

        CompletableFuture.allOf(totalUsers, totalSessions, totalLessons, avgScore).join();

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers.join())
                .totalSessions(totalSessions.join())
                .totalLessons(totalLessons.join())
                .platformAvgScore(avgScore.join())
                .build();
    }
}
```

---

## 3. Eliminating N+1 Queries (Batch Fetching)

### Problem — The N+1 Pattern

N+1 is the most common performance bug in MongoDB applications. It occurs whenever you fetch a list and then query for related data per item:

```java
// ❌ BAD — 1 query for sessions + N queries for lessons
List<PracticeSession> sessions = sessionRepository.findByUserId(userId);

for (PracticeSession session : sessions) {
    // This fires a separate DB query for EACH session!
    VoiceLesson lesson = lessonRepository.findById(session.getLessonId()).orElse(null);
    session.setLessonTitle(lesson.getTitle()); // mutating entity — also wrong
}
// If user has 50 sessions: 1 + 50 = 51 DB round-trips
```

### Solution — Collect IDs → Batch Fetch → Map in Memory

```java
// ✅ GOOD — exactly 2 DB queries regardless of list size
List<PracticeSession> sessions = sessionRepository.findByUserId(userId);

// Step 1: Collect all lesson IDs needed
List<String> lessonIds = sessions.stream()
        .map(PracticeSession::getLessonId)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

// Step 2: One batch query for all lessons
Map<String, VoiceLesson> lessonMap = lessonRepository.findAllById(lessonIds).stream()
        .collect(Collectors.toMap(VoiceLesson::getId, l -> l, (a, b) -> a));

// Step 3: Map in memory — zero DB calls
List<PracticeSessionResponseDTO> result = sessions.stream()
        .map(session -> {
            VoiceLesson lesson = lessonMap.get(session.getLessonId());
            PracticeSessionResponseDTO dto = sessionMapper.toResponseDTO(session);
            if (lesson != null) {
                dto.setLessonTitle(lesson.getTitle());
                dto.setLessonCategory(lesson.getCategory() != null
                        ? lesson.getCategory().name() : null);
                dto.setLessonDifficulty(lesson.getDifficulty());
                dto.setLessonContent(lesson.getContent());
                dto.setLessonDescription(lesson.getDescription());
            }
            return dto;
        })
        .collect(Collectors.toList());
// If user has 50 sessions: always 2 DB round-trips
```

### When batch fetching is required

| Scenario | Wrong | Correct |
|---|---|---|
| Sessions → lesson details | `findById()` per session | `findAllById(lessonIds)` |
| Users → profile data | `findByUserId()` per user | `findAllByUserId(userIds)` |
| Competitions → participant details | `findById()` per participant | `findAllById(participantIds)` |

---

## 4. MongoDB-side Aggregation (Never Java Streams)

### Problem

Never pull large datasets into Java to compute averages, sums, or counts. MongoDB's aggregation pipeline is orders of magnitude faster and uses zero application memory.

```java
// ❌ BAD — loads ALL sessions into JVM heap, then computes average in Java
List<PracticeSession> allSessions = sessionRepository.findByUserId(userId);
double avg = allSessions.stream()
        .mapToDouble(PracticeSession::getOverallScore)
        .average()
        .orElse(0.0);
// For a user with 500 sessions: deserializes 500 objects, uses ~5MB heap
```

### Solution — `@Aggregation` in Repository

```java
// ✅ GOOD — MongoDB computes the average server-side, returns one number
public interface SessionRepository extends MongoRepository<PracticeSession, String> {

    // Count — always use countBy* instead of findAll().size()
    long countByUserId(String userId);

    // Average score per user
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0 } }",
        "{ $group: { _id: null, avgScore: { $avg: '$overallScore' } } }"
    })
    Double findAverageScoreByUserId(String userId);

    // Total practice time in seconds per user
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0 } }",
        "{ $group: { _id: null, totalSeconds: { $sum: '$durationSeconds' } } }"
    })
    Long findTotalDurationSecondsByUserId(String userId);

    // Best score achieved by user
    @Aggregation(pipeline = {
        "{ $match: { userId: ?0 } }",
        "{ $group: { _id: null, maxScore: { $max: '$overallScore' } } }"
    })
    Double findBestScoreByUserId(String userId);

    // Platform-wide average (Admin stats)
    @Aggregation(pipeline = {
        "{ $group: { _id: null, avg: { $avg: '$overallScore' } } }"
    })
    Double findPlatformAverageScore();
}
```

### Aggregation Reference

| Use case | MongoDB operator | Repository method naming |
|---|---|---|
| Count documents | `$count` or `countBy*()` | `countByUserId(id)` |
| Sum a field | `$sum` in `$group` | `@Aggregation + findTotalX` |
| Average a field | `$avg` in `$group` | `@Aggregation + findAverageX` |
| Maximum value | `$max` in `$group` | `@Aggregation + findBestX` |
| Filter + count | `$match` then `$count` | `countByUserIdAndStatus(id, status)` |

---

## 5. Asynchronous Fire-and-Forget (@Async)

### Problem

Some operations need to happen after an API response is sent, but should not make the client wait:

- Sending push notifications after a session is saved
- Writing audit log entries
- Updating MC profile statistics after a review
- Initializing user resources after registration

### Solution — @Async

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Override
    public PracticeSession savePracticeSession(PracticeSessionRequest request, String userId) {
        // 1. Synchronous — must complete before returning response
        PracticeSession saved = sessionRepository.save(buildSession(request, userId));

        // 2. Asynchronous — client does NOT wait for these
        notificationService.sendSessionCompleteNotification(userId, saved.getId());
        auditLogService.log(userId, AuditAction.SESSION_SAVED, saved.getId());

        return saved;
    }
}
```

```java
// NotificationService — fire and forget
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Async
    @Override
    public void sendSessionCompleteNotification(String userId, String sessionId) {
        // Runs on a separate Virtual Thread — does not block the request thread
        try {
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title("Buổi luyện tập hoàn thành!")
                    .message("Xem kết quả phân tích AI của bạn.")
                    .type(NotificationType.SESSION_COMPLETE)
                    .sessionId(sessionId)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to send session notification for user {}: {}", userId,
                    SecurityUtils.safeMessage(e));
        }
    }
}
```

### AsyncConfig — Virtual Thread Executor

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        // Java 21 Virtual Threads — scales to millions of concurrent tasks
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async error in {}: {}", method.getName(), ex.getMessage());
    }
}
```

### When to use @Async

| Operation | @Async? | Reason |
|---|---|---|
| Push notifications | ✅ Yes | Client does not need to wait |
| Audit log writes | ✅ Yes | Non-blocking, lower priority |
| Profile stat updates | ✅ Yes | Eventually consistent is acceptable |
| Email sending | ✅ Yes | SMTP is slow, never block request |
| Session analysis result save | ❌ No | Response depends on this completing |
| JWT validation | ❌ No | Must complete before proceeding |
| Payment record creation | ❌ No | Financial data must be synchronous |

---

## 6. Java 21 Virtual Threads

### What are Virtual Threads?

Java 21 Virtual Threads (Project Loom) allow the JVM to handle millions of concurrent tasks without the overhead of OS threads. A Virtual Thread is lightweight — creating 10,000 of them costs approximately the same as creating 10 traditional OS threads.

In Spring Boot 3.3, Virtual Threads are enabled at the server level:

```properties
# application.properties
spring.threads.virtual.enabled=true
```

Combined with `AsyncConfig` using `Executors.newVirtualThreadPerTaskExecutor()`, every `@Async` task and every incoming HTTP request gets its own Virtual Thread.

### Impact

| Scenario | Traditional Threads | Virtual Threads |
|---|---|---|
| 1,000 concurrent voice analysis requests | Thread pool exhaustion at ~200 | Handles all 1,000 |
| I/O wait (MongoDB, Cloudinary calls) | Thread blocked, wasted | Thread parked, CPU free |
| @Async tasks | Limited by pool size | Unlimited (JVM-managed) |

### Rules for Virtual Thread compatibility

- **No `synchronized` blocks on shared state** — use `ReentrantLock` instead. Synchronized blocks can pin Virtual Threads to carrier threads, defeating the benefit.
- **No thread-local caching of DB connections** — the pool handles this.
- **No manual `Thread.sleep()` in production** — use `CompletableFuture` delays or scheduling if needed.

---

## 7. Response Size Optimization

### Use Projection (return only needed fields)

When a frontend component only needs a few fields from a large document, define a projection interface instead of returning the full document.

```java
// ✅ MongoDB projection — only fetches id, title, category, difficulty
public interface LessonSummaryProjection {
    String getId();
    String getTitle();
    String getCategory();
    String getDifficulty();
}

// In repository:
List<LessonSummaryProjection> findAllProjectedBy();
```

### Pagination for history endpoints

All list endpoints that return session history, audit logs, or notifications must use pagination. Never return unbounded lists.

```java
@GetMapping("/history")
public ResponseEntity<ApiResponse<Page<PracticeSessionResponseDTO>>> getPracticeHistory(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<PracticeSession> sessions = sessionRepository.findByUserId(
            SecurityUtils.getCurrentUserId(), pageable);

    Page<PracticeSessionResponseDTO> dtos = sessions.map(sessionMapper::toResponseDTO);
    return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử thành công", dtos));
}
```

---

## 8. Anti-Pattern Reference

A quick reference of what never to write in this codebase.

### ❌ `.findAll().size()` — always use `countBy*()`

```java
// ❌
int count = lessonRepository.findAll().size();

// ✅
long count = lessonRepository.count();
```

### ❌ Java Stream aggregations on DB lists

```java
// ❌
double avg = sessionRepository.findAll().stream()
    .mapToDouble(PracticeSession::getOverallScore).average().orElse(0);

// ✅
double avg = sessionRepository.findPlatformAverageScore();
```

### ❌ `findById()` inside a loop

```java
// ❌
for (String lessonId : lessonIds) {
    VoiceLesson lesson = lessonRepository.findById(lessonId).orElse(null);
    // ...
}

// ✅
Map<String, VoiceLesson> lessonMap = lessonRepository.findAllById(lessonIds)
    .stream().collect(Collectors.toMap(VoiceLesson::getId, l -> l));
```

### ❌ Sequential queries where parallel is possible

```java
// ❌
long users    = userRepository.count();      // waits
long sessions = sessionRepository.count();   // waits
long lessons  = lessonRepository.count();    // waits

// ✅ — run concurrently
var u = CompletableFuture.supplyAsync(() -> userRepository.count());
var s = CompletableFuture.supplyAsync(() -> sessionRepository.count());
var l = CompletableFuture.supplyAsync(() -> lessonRepository.count());
CompletableFuture.allOf(u, s, l).join();
```

### ❌ Blocking operations in `@Async` without error handling

```java
// ❌ — uncaught exception swallowed silently
@Async
public void sendNotification(String userId) {
    notificationRepository.save(...); // throws, nobody catches
}

// ✅ — wrap in try-catch, log errors
@Async
public void sendNotification(String userId) {
    try {
        notificationRepository.save(...);
    } catch (Exception e) {
        log.error("Notification failed for user {}: {}", userId, SecurityUtils.safeMessage(e));
    }
}
```

### ❌ Bare `orElseThrow()`

```java
// ❌
VoiceLesson lesson = lessonRepository.findById(id).orElseThrow();

// ✅
VoiceLesson lesson = lessonRepository.findById(id)
    .orElseThrow(() -> new RuntimeException("Bài học không tồn tại: " + id));
```

### ❌ `e.getMessage()` directly in response

```java
// ❌ — can be null, leaks internal stack info
return ApiResponse.fail(e.getMessage());

// ✅ — null-safe wrapper
return ApiResponse.fail(SecurityUtils.safeMessage(e));
```

---

## 9. Quick Checklist

Before submitting any service-layer code, verify:

- [ ] **Count queries:** Used `countBy*()` instead of `findAll().size()`
- [ ] **Aggregations:** Used `@Aggregation` in repository for any SUM/AVG/MAX/MIN
- [ ] **N+1 check:** No `findById()` inside loops — using `findAllById()` + in-memory map
- [ ] **Parallel queries:** APIs with 2+ independent sources use `CompletableFuture`
- [ ] **Async:** Notifications, emails, audit logs, and profile updates are annotated `@Async`
- [ ] **Error handling:** All `@Async` methods have try-catch + `log.error()`
- [ ] **Exception messages:** Using `SecurityUtils.safeMessage(e)` not `e.getMessage()`
- [ ] **orElseThrow:** Always has a message lambda, never bare `orElseThrow()`
- [ ] **Pagination:** List endpoints that can return large datasets use `Pageable`
- [ ] **No synchronized:** Not using `synchronized` blocks inside `@Async` methods

---

*Last updated: 2026-05-22*
