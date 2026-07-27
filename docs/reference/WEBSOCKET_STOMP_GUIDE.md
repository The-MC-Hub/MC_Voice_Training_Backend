# Technical Reference: WebSocket & STOMP Protocol Deep Spec

Document Version: 2.1.0  
WebSocket Endpoints: `/ws-chat`, `/ws-script/{bookingId}`  
Supported Protocols: STOMP over SockJS / Pure WebSocket  

---

## 1. Connection Lifecycle & STOMP Protocol Handshake

```mermaid
sequenceDiagram
    autonumber
    actor Client as Web / Mobile App
    participant Handshake as HandshakeInterceptor
    participant STOMPEngine as Spring STOMP Broker
    participant Topic as /topic/conversation/{id} /topic/script/{bookingId}

    Client->>Handshake: CONNECT /ws-chat (Authorization: Bearer <JWT>)
    Handshake->>Handshake: Validate JWT Access Token
    alt JWT Invalid
        Handshake-->>Client: 401 Unauthorized (Close WS Connection)
    else JWT Valid
        Handshake->>STOMPEngine: Pass Security Context
        STOMPEngine-->>Client: CONNECTED { heart-beat: "10000,10000", version: "1.2" }
        Client->>Topic: SUBSCRIBE /topic/script/{bookingId}
        STOMPEngine-->>Client: RECEIPT { receipt-id: "sub-0" }
    end
```

---

## 2. STOMP Frame Structures

### 2.1 CONNECT Frame
```stomp
CONNECT
accept-version:1.1,1.2
heart-beat:10000,10000
Authorization:Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

^@
```

### 2.2 Chat Message Frame (`/ws-chat`)
```stomp
SEND
destination:/app/chat.sendMessage
content-type:application/json

{"conversationId":"66a01b2c3d4e5f6789012344","content":"Xin chào MC!","messageType":"TEXT"}
^@
```

### 2.3 Real-Time Live Script Collaboration Frame (`/ws-script/{bookingId}`)
```stomp
SEND
destination:/app/script.edit/book_100
content-type:application/json

{"bookingId":"book_100","content":"MC: Kính chào quý vị đại biểu...","editedByUserId":"user_101"}
^@
```

---

## 3. Real-Time Topics & Subscription Matrix

| Endpoint | Destination / Topic | Description | Payload Model |
|---|---|---|---|
| `/ws-chat` | `/topic/conversation/{id}` | Real-time chat message broadcast | `ChatMessage` |
| `/ws-chat` | `/user/queue/notifications` | User-specific private notifications | `NotificationDTO` |
| `/ws-script/{bookingId}` | `/topic/script/{bookingId}` | Real-time live script text edit synchronization | `ScriptDocument` |

---

## 4. Frontend Client Implementation Example (React + `@stomp/stompjs`)

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const initScriptWebSocket = (jwtToken, bookingId, onScriptUpdated) => {
  const stompClient = new Client({
    webSocketFactory: () => new SockJS('https://mc-voice-training-backend.onrender.com/ws-script/' + bookingId),
    connectHeaders: { Authorization: `Bearer ${jwtToken}` },
    reconnectDelay: 5000,
  });

  stompClient.onConnect = () => {
    stompClient.subscribe(`/topic/script/${bookingId}`, (frame) => {
      onScriptUpdated(JSON.parse(frame.body));
    });
  };

  stompClient.activate();
  return stompClient;
};
```
