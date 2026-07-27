# UC-12 — Trò Chuyện & Nhắn Tin Trực Tiếp (Chat & Messaging)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Trò chuyện và Nhắn tin trực tiếp Realtime.

---

## 💬 UC-12.1: Tạo Cuộc Trò Chuyện Mới (Create Conversation)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User (Client / MC).
- **Mục tiêu:** Mở cuộc trò chuyện trực tiếp giữa Client và MC. Tái sử dụng hội thoại cũ nếu đã tồn tại.
- **Endpoint:** `POST /api/v1/conversations`

### 📐 2. Class Diagram (UC-12.1)
```mermaid
classDiagram
    class ConversationController {
        +createConversation(CreateConversationRequestDTO req) ResponseEntity~ApiResponse~
    }
    class Conversation {
        +String id
        +List~String~ participantIds
        +String lastMessageText
        +LocalDateTime lastMessageAt
    }
    ConversationController --> ConversationRepository
    ConversationRepository --> Conversation
```

### 🔄 3. Sequence Diagram (UC-12.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / MC
    participant Controller as ConversationController
    participant DB as MongoDB Atlas

    User->>Controller: POST /api/v1/conversations (recipientId = "mc_123")
    Controller->>DB: findByParticipantIdsContainingBoth(senderId, recipientId)
    
    alt Đã Tồn Tại Hội Thoại
        DB-->>Controller: Existing Conversation Record
        Controller-->>User: 200 OK (Existing Conversation DTO)
    else Chưa Tồn Tại
        Controller->>DB: save(New Conversation: participantIds = [senderId, recipientId])
        DB-->>Controller: Created Conversation
        Controller-->>User: 201 Created (New Conversation DTO)
    end
```

### 🧪 4. Testing & Verification (UC-12.1)
- **Unit Test Method:** `ConversationControllerTest.java` -> `createConversation_existingParticipants_returnsExistingConversation()`
- **Assertions:** Trả về đối tượng `Conversation` với đúng 2 participant IDs.

---

## 💬 UC-12.2: Danh Sách Cuộc Trò Chuyện Gần Đây (Get Conversation List)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Tra cứu danh sách các cuộc trò chuyện cá nhân phân trang, sắp xếp theo thời gian tin nhắn mới nhất.
- **Endpoint:** `GET /api/v1/conversations`

### 📐 2. Class Diagram (UC-12.2)
```mermaid
classDiagram
    class ConversationController {
        +getConversations(Pageable pageable) ResponseEntity~ApiResponse~
    }
    ConversationController --> ConversationRepository
```

### 🔄 3. Sequence Diagram (UC-12.2)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as ConversationController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/conversations?page=0&size=10
    Controller->>DB: findByParticipantIdsContainingOrderByLastMessageAtDesc(currentUserId, Pageable)
    DB-->>Controller: Page<Conversation>
    Controller-->>User: 200 OK (Danh sách 10 cuộc hội thoại mới nhất)
```

### 🧪 4. Testing & Verification (UC-12.2)
- **Unit Test Method:** `ConversationControllerTest.java` -> `getConversations_returnsUserConversations()`
- **Assertions:** Danh sách trả về chỉ chứa các hội thoại mà `currentUserId` tham gia.

---

## 💬 UC-12.3: Lịch Sử Tin Nhắn Phân Trang (Get Message History)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Tải danh sách tin nhắn cũ trong 1 cuộc hội thoại có phân trang (Pagination).
- **Endpoint:** `GET /api/v1/conversations/{id}/messages`

### 📐 2. Class Diagram (UC-12.3)
```mermaid
classDiagram
    class MessageController {
        +getMessages(String conversationId, Pageable pageable) ResponseEntity~ApiResponse~
    }
    class Message {
        +String id
        +String conversationId
        +String senderId
        +String recipientId
        +String content
        +boolean isRead
        +LocalDateTime createdAt
    }
    MessageController --> MessageRepository
    MessageRepository --> Message
```

### 🔄 3. Sequence Diagram (UC-12.3)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as MessageController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/conversations/{id}/messages?page=0&size=20
    Controller->>DB: findByConversationIdOrderByCreatedAtDesc(conversationId, Pageable)
    DB-->>Controller: Page<Message>
    Controller-->>User: 200 OK (Danh sách 20 tin nhắn gần nhất)
```

### 🧪 4. Testing & Verification (UC-12.3)
- **Unit Test Method:** `MessageControllerTest.java` -> `getMessages_returnsConversationMessages()`
- **Assertions:** Trả về danh sách tin nhắn khớp với `conversationId`.

---

## 💬 UC-12.4: Gửi Tin Nhắn Realtime Qua WebSocket STOMP (Send WS Message)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User (Sender).
- **Mục tiêu:** Gửi tin nhắn qua cổng STOMP WebSocket `/ws-chat`. Hệ thống tự động lưu DB, cập nhật `lastMessageText` và broadcast tới `/topic/messages/{conversationId}`.
- **Endpoint:** `WS /ws-chat` (Topic: `/app/chat.sendMessage`)

### 📐 2. Class Diagram (UC-12.4)
```mermaid
classDiagram
    class ChatWebSocketController {
        +handleWsMessage(MessageDTO messageDTO) MessageDTO
    }
    ChatWebSocketController --> MessageRepository
    ChatWebSocketController --> ConversationRepository
```

### 🔄 3. Sequence Diagram (UC-12.4)
```mermaid
sequenceDiagram
    autonumber
    actor Sender as User A (Sender)
    participant WS as WebSocket STOMP Broker (/ws-chat)
    participant Controller as ChatWebSocketController
    participant DB as MongoDB Atlas
    actor Recipient as User B (Recipient)

    Sender->>WS: SEND /app/chat.sendMessage (conversationId, content = "Xin chào MC!")
    WS->>Controller: handleWsMessage(MessageDTO)
    
    Controller->>DB: save(Message: senderId, recipientId, content, isRead = false)
    DB-->>Controller: Saved Message
    
    Controller->>DB: updateConversation(lastMessageText = content, lastMessageAt = now)
    
    Controller->>WS: Broadcast Message to /topic/messages/{conversationId}
    WS-->>Sender: Echo Delivery Success
    WS-->>Recipient: Push Realtime Message (Hiển thị ngay trên UI Chat Box)
```

### 🧪 4. Testing & Verification (UC-12.4)
- **Unit Test Method:** `MessageControllerTest.java` -> `sendMessage_validPayload_savesAndUpdatesLastMessage()`
- **Assertions:** Tin nhắn được lưu DB và `lastMessageText` của hội thoại được cập nhật.

---

## 💬 UC-12.5: Đánh Dấu Đã Đọc Tin Nhắn (Mark Conversation Read)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Cập nhật `isRead = true` cho toàn bộ tin nhắn trong cuộc trò chuyện khi user mở xem.
- **Endpoint:** `PUT /api/v1/conversations/{id}/read`

### 📐 2. Class Diagram (UC-12.5)
```mermaid
classDiagram
    class ConversationController {
        +markAsRead(String id) ResponseEntity~ApiResponse~
    }
    ConversationController --> MessageRepository
```

### 🔄 3. Sequence Diagram (UC-12.5)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as ConversationController
    participant DB as MongoDB Atlas

    User->>Controller: PUT /api/v1/conversations/{id}/read
    Controller->>DB: updateIsReadByConversationIdAndRecipientId(conversationId, currentUserId, true)
    DB-->>Controller: Modified Count N
    Controller-->>User: 200 OK (Đã đánh dấu đã đọc)
```

### 🧪 4. Testing & Verification (UC-012.5)
- **Unit Test Method:** `ConversationControllerTest.java` -> `markAsRead_updatesUnreadMessages()`
- **Assertions:** Tất cả tin nhắn gửi cho user trong hội thoại đó chuyển sang `isRead = true`.

---

## 💬 UC-12.6: Tổng Số Tin Nhắn Chưa Đọc (Get Unread Message Count)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Lấy tổng số lượng tin nhắn chưa đọc của user trên toàn hệ thống để hiển thị Badge đỏ trên Navigation Bar.
- **Endpoint:** `GET /api/v1/messages/unread-count`

### 📐 2. Class Diagram (UC-12.6)
```mermaid
classDiagram
    class MessageController {
        +getUnreadCount() ResponseEntity~ApiResponse~
    }
    MessageController --> MessageRepository
```

### 🔄 3. Sequence Diagram (UC-12.6)
```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated User
    participant Controller as MessageController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/messages/unread-count
    Controller->>DB: countByRecipientIdAndIsReadFalse(currentUserId)
    DB-->>Controller: Total Count (ex: 3)
    Controller-->>User: 200 OK (unreadCount = 3)
```

### 🧪 4. Testing & Verification (UC-12.6)
- **Unit Test Method:** `MessageControllerTest.java` -> `getUnreadCount_returnsCorrectCount()`
- **Assertions:** Trả về số lượng tin nhắn chưa đọc chính xác.
