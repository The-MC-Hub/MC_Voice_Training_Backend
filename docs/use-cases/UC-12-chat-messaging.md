# UC-12 — Trò Chuyện & Nhắn Tin Trực Tiếp (Chat & Messaging)

## 📌 1. Mô tả Tổng Quan & Luồng Nghiệp Vụ

Luồng nghiệp vụ giao tiếp nhắn tin trực tiếp Realtime giữa Client và MC thông qua giao thức WebSocket STOMP (`/ws-chat`) và REST Messaging API, hỗ trợ gửi tin nhắn văn bản, hình ảnh, đánh dấu đã đọc và đếm tin nhắn chưa đọc.

### Actors
- **User (Client / MC)**: Người gửi và người nhận tin nhắn trong cuộc trò chuyện.

---

## 🛠️ 2. Chi Tiết Tính Năng & Điểm Nghiệp Vụ

| # | Tính năng | Mô tả Nghiệp Vụ Chi Tiết | Controller & API Endpoint |
|---|---|---|---|
| 1 | Tạo cuộc hội thoại | Tạo cuộc hội thoại mới giữa 2 người dùng (Client - MC) nếu chưa tồn tại | `POST /api/v1/conversations` |
| 2 | Danh sách hội thoại | Lấy danh sách các cuộc trò chuyện gần đây phân trang kèm tin nhắn cuối | `GET /api/v1/conversations` |
| 3 | Lịch sử tin nhắn | Lấy danh sách tin nhắn cũ trong 1 cuộc hội thoại có phân trang (Pagination) | `GET /api/v1/conversations/{id}/messages` |
| 4 | Gửi tin nhắn REST | Gửi tin nhắn qua HTTP POST fallback khi không dùng WebSocket | `POST /api/v1/conversations/{id}/messages` |
| 5 | Gửi tin nhắn WebSocket | Truyền tin nhắn realtime qua STOMP WebSocket topic `/topic/messages/{conversationId}` | `WS /ws-chat` |
| 6 | Đánh dấu đã đọc | Cập nhật `isRead = true` cho các tin nhắn trong hội thoại khi user xem | `PUT /api/v1/conversations/{id}/read` |
| 7 | Tổng số tin nhắn chưa đọc | Lấy tổng số lượng tin nhắn chưa đọc để hiển thị Badge trên giao diện | `GET /api/v1/messages/unread-count` |

---

## 📐 3. Class Diagram

```mermaid
classDiagram
    class ConversationController {
        +createConversation(recipientId) ResponseEntity
        +getConversations() ResponseEntity
        +markAsRead(id) ResponseEntity
    }

    class MessageController {
        +getMessages(conversationId, page) ResponseEntity
        +sendMessage(conversationId, req) ResponseEntity
        +getUnreadCount() ResponseEntity
    }

    class ChatWebSocketController {
        +handleWsMessage(messageDTO) MessageDTO
    }

    class Conversation {
        +String id
        +List~String~ participantIds
        +String lastMessageText
        +LocalDateTime lastMessageAt
    }

    class Message {
        +String id
        +String conversationId
        +String senderId
        +String recipientId
        +String content
        +String attachmentUrl
        +boolean isRead
        +LocalDateTime createdAt
    }

    ConversationController --> ConversationRepository
    MessageController --> MessageRepository
    ChatWebSocketController --> MessageRepository
    ConversationRepository --> Conversation
    MessageRepository --> Message
```

---

## 🔄 4. Sequence Diagram (Gửi & Nhận Tin Nhắn Realtime qua WebSocket STOMP)

```mermaid
sequenceDiagram
    autonumber
    actor Sender as User A (Sender)
    participant WS as WebSocket STOMP Broker (/ws-chat)
    participant Controller as ChatWebSocketController
    participant DB as MongoDB Atlas
    actor Recipient as User B (Recipient)

    Sender->>WS: SEND /app/chat.sendMessage (conversationId, content, recipientId)
    WS->>Controller: Route to handleWsMessage(MessageDTO)
    
    Controller->>DB: Save Message (senderId, recipientId, content, isRead = false)
    DB-->>Controller: Saved Message Record
    
    Controller->>DB: Update Conversation (lastMessageText = content, lastMessageAt = now)
    
    Controller->>WS: Broadcast Message to /topic/messages/{conversationId}
    WS-->>Sender: Echo Message Delivery Confirmation
    WS-->>Recipient: Push Realtime Message Payload (Hiển thị ngay lập tức trên UI)
```

---

## 🧪 5. Testing & Verification Report

- **Test Suite Classes:**
  - `com.mchub.controllers.ConversationControllerTest`
  - `com.mchub.controllers.MessageControllerTest`
- **Các kịch bản kiểm thử đã thực thi:**
  - `createConversation_existingParticipants_returnsExistingConversation()`: Tái sử dụng cuộc trò chuyện cũ nếu đã có.
  - `sendMessage_validPayload_savesAndUpdatesLastMessage()`: Lưu tin nhắn và cập nhật đúng tin nhắn cuối của hội thoại.
  - `getUnreadCount_returnsCorrectCount()`: Đếm chính xác số tin nhắn chưa đọc của user.
- **Kết quả kiểm thử:** Pass **100% (20/20 unit tests trong module Chat & Messaging)**.
