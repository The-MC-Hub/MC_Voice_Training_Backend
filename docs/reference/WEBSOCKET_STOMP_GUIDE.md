# Technical Reference: WebSocket & STOMP Protocol Specification

Document Version: 1.0.0
WebSocket Endpoint: `/ws-chat`
Supported Protocols: STOMP over SockJS / Pure WebSocket

---

## 1. Overview & Connection Establishment

The chat subsystem utilizes Spring WebSocket with STOMP messaging protocol to deliver real-time messages between Client users and MCs.

### Connection Handshake
- **URL**: `ws://<server-host>:<port>/ws-chat` (or `wss://` in production)
- **SockJS Fallback**: `http://<server-host>:<port>/ws-chat`
- **Authentication**: JWT token MUST be passed in STOMP CONNECT frame header:
  ```stomp
  CONNECT
  accept-version:1.1,1.2
  heart-beat:10000,10000
  Authorization:Bearer <JWT_ACCESS_TOKEN>

  ^@
  ```

---

## 2. Topic Subscription & Destinations

### 2.1 Receiving Realtime Messages
Client subscribes to individual conversation channels:
- **Destination**: `/topic/conversation/{conversationId}`
- **Payload Schema**:
  ```json
  {
    "id": "66a01b2c3d4e5f6789012345",
    "conversationId": "66a01b2c3d4e5f6789012344",
    "senderId": "66a01b2c3d4e5f6789012300",
    "senderName": "Nguyen Van A",
    "content": "Xin chào MC, em muốn nhận tư vấn show ngày 30/10.",
    "messageType": "TEXT",
    "attachmentUrl": null,
    "createdAt": "2026-07-27T19:15:00Z"
  }
  ```

### 2.2 Sending Realtime Messages
Client dispatches messages to destination prefix `/app`:
- **Destination**: `/app/chat.sendMessage`
- **Payload Schema**:
  ```json
  {
    "conversationId": "66a01b2c3d4e5f6789012344",
    "content": "Nội dung tin nhắn gửi đi.",
    "messageType": "TEXT",
    "attachmentUrl": null
  }
  ```

---

## 3. Realtime Notification Channel

Each user receives system notifications via private topic:
- **Destination**: `/user/queue/notifications`
- **Payload**:
  ```json
  {
    "id": "66a01b2c3d4e5f6789012999",
    "title": "Booking Quote Received",
    "message": "MC đã gửi báo giá cho sự kiện của bạn.",
    "type": "BOOKING_UPDATE",
    "createdAt": "2026-07-27T19:15:00Z"
  }
  ```
