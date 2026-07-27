# UC-07 — Nhiệm Vụ Tân Thủ (Onboarding Quest & Rewards)

## 📌 1. Mô tả Tổng Quan & Luồng Nghiệp Vụ

Luồng nghiệp vụ hướng dẫn người dùng mới (Newbies) làm quen với nền tảng qua chuỗi nhiệm vụ: Đọc thử bài tập đầu tiên, cập nhật avatar, giới thiệu bạn bè, nhận thưởng Freeze Streak hoặc XP.

### Actors
- **User (Client/MC)**: Học viên hoàn thành nhiệm vụ và nhận phần thưởng.

---

## 🛠️ 2. Chi Tiết Tính Năng & Điểm Nghiệp Vụ

| # | Tính năng | Mô tả Nghiệp Vụ Chi Tiết | Controller & API Endpoint |
|---|---|---|---|
| 1 | Danh sách nhiệm vụ tân thủ | Lấy danh sách các quest chưa/đã hoàn thành (`isCompleted`, `isClaimed`) | `GET /api/v1/quests` |
| 2 | Nhận phần thưởng quest | Nhận phần thưởng (XP/Streak Freeze) khi quest đạt `isCompleted = true` | `POST /api/v1/quests/{id}/claim` |

---

## 📐 3. Class Diagram

```mermaid
classDiagram
    class QuestController {
        +getQuests() ResponseEntity
        +claimQuest(id) ResponseEntity
    }

    class Quest {
        +String id
        +String title
        +String description
        +QuestType type
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

---

## 🔄 4. Sequence Diagram (Nhận Phần Thưởng Quest Tân Thủ)

```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as QuestController
    participant QuestRepo as QuestRepository
    participant ProgressRepo as UserQuestProgressRepository
    participant UserStatsRepo as UserStatsRepository
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/quests/{id}/claim
    Controller->>ProgressRepo: findByUserIdAndQuestId(userId, questId)
    ProgressRepo-->>Controller: UserQuestProgress Record
    
    alt Quest chưa hoàn thành hoặc đã nhận thưởng
        Controller-->>User: 400 Bad Request (QUEST_NOT_CLAIMABLE)
    else Quest hoàn thành & Chưa nhận thưởng
        Controller->>QuestRepo: findById(questId)
        QuestRepo-->>Controller: Quest Details (rewardXp = 100, rewardStreakFreeze = true)
        
        Controller->>UserStatsRepo: Cộng 100 XP & Nạp +1 lượt Streak Freeze cho User
        Controller->>ProgressRepo: Update isClaimed = true
        ProgressRepo-->>Controller: Saved Record
        Controller-->>User: 200 OK (Đã nhận thành công +100 XP và 1 lượt Freeze Streak)
    end
```

---

## 🧪 5. Testing & Verification Report

- **Test Suite Classes:**
  - `com.mchub.controllers.QuestControllerTest`
- **Các kịch bản kiểm thử đã thực thi:**
  - `getQuests_returnsQuestListWithStatus()`: Lấy đúng trạng thái các quest cá nhân.
  - `claimQuest_validCompletedQuest_grantsReward()`: Cộng thưởng XP và Streak Freeze chính xác.
- **Kết quả kiểm thử:** Pass **100% (14/14 unit tests trong module Onboarding Quest)**.
