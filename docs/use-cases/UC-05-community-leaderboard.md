# UC-05 — Cộng Đồng & Bảng Xếp Hạng (Community & Leaderboard)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Cộng đồng và Bảng xếp hạng thi đua.

---

## 🏆 UC-05.1: Thống Kê Tổng Quan Cộng Đồng (Community Stats)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest / User.
- **Mục tiêu:** Lấy tổng hợp các chỉ số cộng đồng: Tổng số thành viên, số lượng MC chuyên nghiệp và số phiên luyện giọng đã hoàn thành trên toàn hệ thống.
- **Endpoint:** `GET /api/v1/community/stats`

### 📐 2. Class Diagram (UC-05.1)
```mermaid
classDiagram
    class CommunityController {
        +getStats() ResponseEntity~ApiResponse~
    }
    class CommunityStatsDTO {
        +long totalUsers
        +long totalMcs
        +long totalPracticeSessions
    }
    CommunityController --> CommunityStatsDTO
```

### 🔄 3. Sequence Diagram (UC-05.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / Guest
    participant Controller as CommunityController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/community/stats
    Controller->>DB: countUsers(), countMcs(), countPracticeSessions()
    DB-->>Controller: Totals (Users: 15000, MCs: 450, Sessions: 120000)
    Controller-->>User: 200 OK (Thống kê tổng quan cộng đồng)
```

### 🧪 4. Testing & Verification (UC-05.1)
- **Unit Test Method:** `CommunityControllerTest.java` -> `getStats_returnsAggregatedTotals()`
- **Assertions:** Trả về các tổng số > 0.

---

## 🏆 UC-05.2: Bảng Xếp Hạng Leaderboard (Leaderboard Ranking)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Lấy danh sách học viên đứng đầu phân trang theo tiêu chí (`streak`, `precision`, `sessions`) và chu kỳ thời gian (`weekly`, `all_time`).
- **Endpoint:** `GET /api/v1/community/leaderboard`

### 📐 2. Class Diagram (UC-05.2)
```mermaid
classDiagram
    class CommunityController {
        +getLeaderboard(String type, String period, Pageable pageable) ResponseEntity~ApiResponse~
    }
    class LeaderboardEntryDTO {
        +String userId
        +String userName
        +String avatarUrl
        +int rank
        +long score
        +int streak
    }
    CommunityController --> LeaderboardEntryDTO
```

### 🔄 3. Sequence Diagram (UC-05.2)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as CommunityController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/community/leaderboard?type=streak&period=weekly&page=0&size=20
    Controller->>DB: findTopByStreak(PageRequest)
    DB-->>Controller: Page<UserStats>
    Controller->>Controller: Map to LeaderboardEntryDTO list & set rank indices
    Controller-->>User: 200 OK (Danh sách Top 20 học viên có streak cao nhất)
```

### 🧪 4. Testing & Verification (UC-05.2)
- **Unit Test Method:** `CommunityServiceImplTest.java` -> `getLeaderboard_streak_returnsRankedList()`
- **Assertions:** Danh sách trả về được sắp xếp giảm dần theo chỉ số requested.

---

## 🏆 UC-05.3: Tra Cứu Thứ Hạng Cá Nhân (My Rank Position)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Tính toán vị trí xếp hạng chính xác của chính user đang đăng nhập trên bảng xếp hạng chung.
- **Endpoint:** `GET /api/v1/community/leaderboard/me`

### 📐 2. Class Diagram (UC-05.3)
```mermaid
classDiagram
    class CommunityController {
        +getMyRank(String type, String period) ResponseEntity~ApiResponse~
    }
    CommunityController --> CommunityServiceImpl
```

### 🔄 3. Sequence Diagram (UC-05.3)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as CommunityController
    participant Service as CommunityServiceImpl
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/community/leaderboard/me?type=streak
    Controller->>Service: getUserRank(currentUserId, "streak")
    Service->>DB: countUsersWithStreakGreaterThan(userStreak)
    DB-->>Service: count (ex: 14)
    Service-->>Controller: LeaderboardEntryDTO (rank = count + 1 = 15)
    Controller-->>User: 200 OK (Thứ hạng của bạn: #15)
```

### 🧪 4. Testing & Verification (UC-05.3)
- **Unit Test Method:** `CommunityServiceImplTest.java` -> `getMyRank_validUser_returnsUserPosition()`
- **Assertions:** Trả về vị trí rank hợp lệ >= 1.

---

## 🏆 UC-05.4: Đấu Trường Giọng Nói (Active Voice Arenas)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Lấy danh sách các cuộc thi thử thách thi đấu giọng nói đang mở đăng ký (`active-arenas`).
- **Endpoint:** `GET /api/v1/community/active-arenas`

### 📐 2. Class Diagram (UC-05.4)
```mermaid
classDiagram
    class CommunityController {
        +getActiveArenas() ResponseEntity~ApiResponse~
    }
    class Competition {
        +String id
        +String title
        +LocalDateTime startDate
        +LocalDateTime endDate
        +int totalParticipants
    }
    CommunityController --> CompetitionRepository
    CompetitionRepository --> Competition
```

### 🔄 3. Sequence Diagram (UC-05.4)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as CommunityController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/community/active-arenas
    Controller->>DB: findByEndDateAfter(now)
    DB-->>Controller: List<Competition>
    Controller-->>User: 200 OK (Danh sách các đấu trường giọng nói đang mở)
```

### 🧪 4. Testing & Verification (UC-05.4)
- **Unit Test Method:** `CommunityControllerTest.java` -> `getActiveArenas_returnsActiveCompetitions()`
- **Assertions:** Trả về danh sách cuộc thi có `endDate > now`.

---

## 🏆 UC-05.5: Bài Đăng Mạng Xã Hội & Click Tracking (Social Posts & Click Tracking)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest / User.
- **Mục tiêu:** Hiển thị bài viết truyền cảm hứng và ghi nhận lượt click tương tác của người dùng vào link bài viết.
- **Endpoint:** `GET /api/v1/social-posts`, `POST /api/v1/social-posts/{id}/click`

### 📐 2. Class Diagram (UC-05.5)
```mermaid
classDiagram
    class SocialPostController {
        +getPosts() ResponseEntity~ApiResponse~
        +trackClick(String id) ResponseEntity~ApiResponse~
    }
    class SocialPost {
        +String id
        +String title
        +String linkUrl
        +int clickCount
    }
    SocialPostController --> SocialPostRepository
    SocialPostRepository --> SocialPost
```

### 🔄 3. Sequence Diagram (UC-05.5)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / Reader
    participant Controller as SocialPostController
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/social-posts/{id}/click
    Controller->>DB: findById(id)
    DB-->>Controller: SocialPost Record
    Controller->>DB: update(clickCount = clickCount + 1)
    Controller-->>User: 200 OK (Đã ghi nhận lượt click)
```

### 🧪 4. Testing & Verification (UC-05.5)
- **Unit Test Method:** `SocialPostServiceImplTest.java` -> `trackClick_incrementsClickCount()`
- **Assertions:** `clickCount` tăng thêm 1 sau khi gọi API.
