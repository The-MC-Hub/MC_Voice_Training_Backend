# UC-07 — Nhiệm Vụ Tân Thủ (Onboarding Quest & Rewards)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Nhiệm vụ tân thủ.

---

## 🎯 UC-07.1: Danh Sách Nhiệm Vụ Tân Thủ (Get Onboarding Quests)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Tra cứu danh sách các nhiệm vụ tân thủ kèm trạng thái tiến độ (`isCompleted`, `isClaimed`).
- **Endpoint:** `GET /api/v1/quests`

### 📐 2. Class Diagram (UC-07.1)
```mermaid
classDiagram
    class QuestController {
        +getQuests() ResponseEntity~ApiResponse~
    }
    class Quest {
        +String id
        +String title
        +String description
        +int rewardXp
        +boolean rewardStreakFreeze
    }
    class UserQuestProgress {
        +String userId
        +String questId
        +boolean isCompleted
        +boolean isClaimed
    }
    QuestController --> QuestRepository
    QuestController --> UserQuestProgressRepository
    QuestRepository --> Quest
    UserQuestProgressRepository --> UserQuestProgress
```

### 🔄 3. Sequence Diagram (UC-07.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / Student
    participant Controller as QuestController
    participant QuestRepo as QuestRepository
    participant ProgressRepo as UserQuestProgressRepository
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/quests
    Controller->>QuestRepo: findAll()
    QuestRepo-->>Controller: List<Quest> allQuests
    
    Controller->>ProgressRepo: findByUserId(currentUserId)
    ProgressRepo-->>Controller: List<UserQuestProgress> userProgress
    
    Controller->>Controller: Map quests with user progress status
    Controller-->>User: 200 OK (Danh sách nhiệm vụ & Trạng thái hoàn thành/đã nhận)
```

### 🧪 4. Testing & Verification (UC-07.1)
- **Unit Test Method:** `QuestControllerTest.java` -> `getQuests_returnsQuestListWithStatus()`
- **Assertions:** Trả về đúng tiến độ cá nhân của user.

---

## 🎯 UC-07.2: Nhận Phần Thưởng Quest Tân Thủ (Claim Quest Reward)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Nhận phần thưởng (XP / Streak Freeze) cho nhiệm vụ đã `isCompleted = true` và chưa `isClaimed`.
- **Endpoint:** `POST /api/v1/quests/{id}/claim`

### 📐 2. Class Diagram (UC-07.2)
```mermaid
classDiagram
    class QuestController {
        +claimQuest(String id) ResponseEntity~ApiResponse~
    }
    QuestController --> UserQuestProgressRepository
    QuestController --> UserStatsRepository
```

### 🔄 3. Sequence Diagram (UC-07.2)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / Student
    participant Controller as QuestController
    participant ProgressRepo as UserQuestProgressRepository
    participant QuestRepo as QuestRepository
    participant UserStatsRepo as UserStatsRepository
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/quests/{id}/claim
    Controller->>ProgressRepo: findByUserIdAndQuestId(currentUserId, questId)
    ProgressRepo-->>Controller: UserQuestProgress Record
    
    alt Quest Chưa Hoàn Thành Hoặc Đã Nhận Thưởng
        Controller-->>User: 400 Bad Request (QUEST_NOT_CLAIMABLE)
    else Hợp Lệ (isCompleted = true & isClaimed = false)
        Controller->>QuestRepo: findById(questId)
        QuestRepo-->>Controller: Quest Details (rewardXp = 100, rewardStreakFreeze = true)
        
        Controller->>UserStatsRepo: Cộng 100 XP & Nạp +1 lượt Streak Freeze
        Controller->>ProgressRepo: Update isClaimed = true
        ProgressRepo-->>Controller: Saved Record
        Controller-->>User: 200 OK (Đã cộng +100 XP và +1 lượt Đóng Băng Streak)
    end
```

### 🧪 4. Testing & Verification (UC-07.2)
- **Unit Test Method:** `QuestControllerTest.java` -> `claimQuest_validCompletedQuest_grantsReward()`
- **Assertions:** `isClaimed` chuyển sang `true`, User XP được cộng thêm đúng bằng `rewardXp`.
