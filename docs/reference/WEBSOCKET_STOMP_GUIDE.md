# Technical Reference: WebSocket & STOMP Protocol Deep Spec

Document Version: 2.0.0
WebSocket Endpoint: `/ws-chat`
Supported Protocols: STOMP over SockJS / Pure WebSocket

---

## 1. Connection Lifecycle & STOMP Protocol Handshake

```mermaid
sequenceDiagram
    autonumber
    actor Client as Web / Mobile App
    participant Handshake as HandshakeInterceptor
    participant STOMPEngine as Spring STOMP Broker
    participant Topic as /topic/conversation/{id}

    Client->>Handshake: CONNECT /ws-chat (Authorization: Bearer <JWT>)
    Handshake->>Handshake: Validate JWT Access Token
    alt JWT Invalid
        Handshake-->>Client: 401 Unauthorized (Close WS Connection)
    else JWT Valid
        Handshake->>STOMPEngine: Pass Security Context
        STOMPEngine-->>Client: CONNECTED { heart-beat: "10000,10000", version: "1.2" }
        Client->>Topic: SUBSCRIBE /topic/conversation/{conversationId}
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

### 2.2 SEND Message Frame
```stomp
SEND
destination:/app/chat.sendMessage
content-type:application/json

{"conversationId":"66a01b2c3d4e5f6789012344","content":"Xin chào MC!","messageType":"TEXT"}
^@
```

---

## 3. Frontend Client Implementation Example (React + `@stomp/stompjs`)

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export const initWebSocket = (jwtToken, conversationId, onMessageReceived) => {
  const stompClient = new Client({
    // Use SockJS fallback if native WebSocket fails
    webSocketFactory: () => new SockJS('https://mc-voice-training-backend.onrender.com/ws-chat'),
    connectHeaders: {
      Authorization: `Bearer ${jwtToken}`,
    },
    debug: (str) => console.log('[STOMP]:', str),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  stompClient.onConnect = (frame) => {
    console.log('Connected to STOMP Broker');
    
    // Subscribe to specific conversation topic
    stompClient.subscribe(`/topic/conversation/${conversationId}`, (messageFrame) => {
      const messageData = JSON.parse(messageFrame.body);
      onMessageReceived(messageData);
    });

    // Subscribe to private user notification queue
    stompClient.subscribe('/user/queue/notifications', (notifFrame) => {
      const notifData = JSON.parse(notifFrame.body);
      console.log('Private Notification Received:', notifData);
    });
  };

  stompClient.onStompError = (frame) => {
    console.error('Broker error:', frame.headers['message']);
  };

  stompClient.activate();
  return stompClient;
};
```
