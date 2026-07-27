# UC-03 — Luyện Giọng Nói AI (Voice Practice & AI Analysis)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Luyện giọng và Chấm điểm AI.

---

## 🎙️ UC-03.1: Danh Sách & Chi Tiết Bài Luyện Giọng (Get Voice Lessons)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest / Client / MC.
- **Mục tiêu:** Tra cứu kho bài luyện tập theo danh mục (Phát âm, Tròn vành rõ chữ, Nhấn giọng, Cảm xúc) và xem chi tiết văn bản bài luyện kèm audio đọc mẫu.
- **Endpoint:** `GET /api/v1/voice/lessons`, `GET /api/v1/voice/lessons/{id}`

### 📐 2. Class Diagram (UC-03.1)
```mermaid
classDiagram
    class VoiceController {
        +getLessons(String category) ResponseEntity~ApiResponse~
        +getLessonDetail(String id) ResponseEntity~ApiResponse~
    }
    class VoiceLesson {
        +String id
        +String title
        +String category
        +String scriptContent
        +String sampleAudioUrl
        +int targetWpm
    }
    VoiceController --> VoiceLessonRepository
    VoiceLessonRepository --> VoiceLesson
```

### 🔄 3. Sequence Diagram (UC-03.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as VoiceController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/voice/lessons?category=PRONUNCIATION
    Controller->>DB: findByCategory("PRONUNCIATION")
    DB-->>Controller: List<VoiceLesson>
    Controller-->>User: 200 OK (Danh sách bài tập luyện phát âm)
```

### 🧪 4. Testing & Verification (UC-03.1)
- **Unit Test Method:** `VoiceControllerTest.java` -> `getLessons_returnsCategoryLessons()`
- **Assertions:** Trả về danh sách `VoiceLesson` có đúng category requested.

---

## 🎙️ UC-03.2: Chấm Điểm Bài Tập Luyện Giọng AI (Analyze Voice Practice)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Client / MC (Học viên đã đăng nhập).
- **Mục tiêu:** Tải file ghi âm `.wav`/`.mp3` bài đọc, upload lên Cloudinary, gửi URL tới Python AI Engine, tính toán chỉ số (Overall Score, Accuracy, Rhythm, Pitch, WPM) và trừ 1 lượt AI session của user.
- **Rules:** Nếu `aiSessionsUsed >= maxAllowed` (dựa trên gói VIP), ném lỗi `AI_SESSIONS_EXHAUSTED`.
- **Endpoint:** `POST /api/v1/voice/practice/analyze`

### 📐 2. Class Diagram (UC-03.2)
```mermaid
classDiagram
    class VoiceController {
        +analyzePractice(MultipartFile file, String lessonId) ResponseEntity~ApiResponse~
    }
    class VoiceService {
        <<interface>>
        +analyzePractice(MultipartFile file, String lessonId, String userId) PracticeResultDTO
    }
    class VoiceServiceImpl {
        -CloudinaryService storageService
        -WebClient aiServiceClient
        -PracticeSessionRepository sessionRepo
        -UserRepository userRepo
    }
    class PracticeSession {
        +String id
        +String userId
        +String lessonId
        +String audioUrl
        +double overallScore
        +double accuracyScore
        +double rhythmScore
        +double speakingRateWpm
        +String feedbackVi
    }
    VoiceController --> VoiceService
    VoiceServiceImpl ..|> VoiceService
    VoiceServiceImpl --> PracticeSessionRepository
    PracticeSessionRepository --> PracticeSession
```

### 🔄 3. Sequence Diagram (UC-03.2)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as VoiceController
    participant Service as VoiceServiceImpl
    participant Cloudinary as Cloudinary Storage
    participant AI as Python AI Engine (FastAPI)
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/voice/practice/analyze (audioFile, lessonId)
    Controller->>Service: analyzePractice(audioFile, lessonId, currentUserId)
    Service->>DB: findUserById(currentUserId)
    DB-->>Service: User Record
    
    alt Hết Lượt AI Sessions
        Service-->>Controller: AppException(AI_SESSIONS_EXHAUSTED)
        Controller-->>User: 403 Forbidden (Gợi ý nâng cấp VIP)
    else Còn Lượt
        Service->>Cloudinary: uploadAudio(audioFile)
        Cloudinary-->>Service: audioUrl (HTTPS)
        
        Service->>AI: POST /analyze (audioUrl, targetScript)
        AI-->>Service: JSON { overallScore: 88, accuracyScore: 92, rhythmScore: 84, wpm: 150, feedbackVi: "Giọng đọc truyền cảm..." }
        
        Service->>DB: save(PracticeSession)
        Service->>DB: Update User (aiSessionsUsed++, XP += 50)
        Service-->>Controller: PracticeResultDTO
        Controller-->>User: 200 OK (Trả về bảng điểm & nhận xét AI)
    end
```

### 🧪 4. Testing & Verification (UC-03.2)
- **Unit Test Method:** `VoiceServiceImplTest.java` -> `analyzePractice_success_returnsScoresAndFeedback()`
- **Assertions:** `PracticeSession` được lưu với điểm số > 0, `aiSessionsUsed` tăng thêm 1.

---

## 🎙️ UC-03.3: Chấm Điểm Bài Tập Dùng Thử Cho Khách (Analyze Guest Practice)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest (Khách chưa đăng nhập).
- **Mục tiêu:** Cho phép khách trải nghiệm tính năng chấm điểm giọng đọc miễn phí.
- **Rules:** Giới hạn 1 lượt/3 giờ theo IP (`GuestVoiceUsageRepository`). Nếu bấm liên tục sẽ bị từ chối với HTTP 429.
- **Endpoint:** `POST /api/v1/voice/practice/analyze-guest`

### 📐 2. Class Diagram (UC-03.3)
```mermaid
classDiagram
    class VoiceController {
        +analyzeGuest(MultipartFile file) ResponseEntity~ApiResponse~
    }
    class GuestVoiceUsage {
        +String ipAddress
        +LocalDateTime lastUsedAt
    }
    VoiceController --> GuestVoiceUsageRepository
    GuestVoiceUsageRepository --> GuestVoiceUsage
```

### 🔄 3. Sequence Diagram (UC-03.3)
```mermaid
sequenceDiagram
    autonumber
    actor Guest as Guest User
    participant Controller as VoiceController
    participant DB as MongoDB Atlas
    participant AI as Python AI Engine

    Guest->>Controller: POST /api/v1/voice/practice/analyze-guest (audioFile)
    Controller->>DB: findByIpAddress(clientIp)
    DB-->>Controller: GuestVoiceUsage Record
    
    alt Chưa Đủ 3 Giờ Cooldown
        Controller-->>Guest: 429 Too Many Requests (Vui lòng đăng ký để không bị giới hạn)
    else Hợp Lệ
        Controller->>AI: POST /analyze-quick (audioFile)
        AI-->>Controller: JSON Scores & Feedback
        Controller->>DB: save(GuestVoiceUsage: lastUsedAt = now)
        Controller-->>Guest: 200 OK (Trả về điểm dùng thử)
    end
```

### 🧪 4. Testing & Verification (UC-03.3)
- **Unit Test Method:** `VoiceControllerTest.java` -> `analyzeGuest_cooldownActive_returns429()`
- **Assertions:** Đúng IP bị chặn nếu gọi 2 lần trong 3 giờ.

---

## 🎙️ UC-03.4: Lịch Sử Luyện Tập Cá Nhân (Practice History)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Xem lại danh sách các phiên đọc ghi âm đã thực hiện kèm biểu đồ tiến bộ điểm số qua thời gian.
- **Endpoint:** `GET /api/v1/voice/history`

### 📐 2. Class Diagram (UC-03.4)
```mermaid
classDiagram
    class VoiceController {
        +getPracticeHistory(Pageable pageable) ResponseEntity~ApiResponse~
    }
    VoiceController --> PracticeSessionRepository
```

### 🔄 3. Sequence Diagram (UC-03.4)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as VoiceController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/voice/history?page=0&size=10
    Controller->>DB: findByUserIdOrderByCreatedAtDesc(currentUserId, Pageable)
    DB-->>Controller: Page<PracticeSession>
    Controller-->>User: 200 OK (Danh sách 10 phiên luyện tập gần nhất)
```

### 🧪 4. Testing & Verification (UC-03.4)
- **Unit Test Method:** `VoiceControllerTest.java` -> `getHistory_returnsUserSessions()`
- **Assertions:** Trả về danh sách phiên luyện tập thuộc đúng `userId`.

---

## 🎙️ UC-03.5: Phát Tạo Âm Thanh TTS Mẫu (Generate Text-To-Speech)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User / MC / Admin.
- **Mục tiêu:** Chuyển đổi văn bản bài đọc bất kỳ thành giọng nói đọc mẫu chuẩn MC (Giọng Bắc/Giọng Nam).
- **Endpoint:** `POST /api/v1/voice/tts`

### 📐 2. Class Diagram (UC-03.5)
```mermaid
classDiagram
    class VoiceController {
        +generateTts(TtsRequestDTO req) ResponseEntity~ApiResponse~
    }
    class TtsRequestDTO {
        +String text
        +String voiceGender
        +String accent
    }
    VoiceController --> TtsRequestDTO
```

### 🔄 3. Sequence Diagram (UC-03.5)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as VoiceController
    participant AI as Python TTS Engine

    User->>Controller: POST /api/v1/voice/tts (text, voiceGender="FEMALE", accent="NORTH")
    Controller->>AI: POST /generate-tts (text, voiceGender, accent)
    AI-->>Controller: Audio Stream / Cloudinary URL
    Controller-->>User: 200 OK (Audio URL bài đọc mẫu)
```

### 🧪 4. Testing & Verification (UC-03.5)
- **Unit Test Method:** `VoiceControllerTest.java` -> `generateTts_validText_returnsAudioUrl()`
- **Assertions:** Audio URL trả về không rỗng và có định dạng `.mp3`.
