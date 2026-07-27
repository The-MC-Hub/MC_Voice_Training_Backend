# UC-13 — Đánh Giá Đồng Nghiệp (Peer Review & Feedback)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Đánh giá đồng nghiệp và Chấm điểm chéo bài luyện giọng.

---

## 🎧 UC-13.1: Gửi Yêu Cầu Nhận Xét Đồng Nghiệp (Request Peer Review)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** MC (Requester).
- **Mục tiêu:** Đưa bài ghi âm luyện giọng đã hoàn thành vào danh sách chờ nhận xét để cộng đồng MC khác đóng góp ý kiến.
- **Endpoint:** `POST /api/v1/peer-reviews/request`

### 📐 2. Class Diagram (UC-13.1)
```mermaid
classDiagram
    class PeerReviewController {
        +requestReview(RequestReviewDTO req) ResponseEntity~ApiResponse~
    }
    class PeerReviewRequest {
        +String id
        +String requesterMcId
        +String practiceSessionId
        +String audioUrl
        +PeerReviewStatus status
        +LocalDateTime createdAt
    }
    PeerReviewController --> PeerReviewRequestRepository
    PeerReviewRequestRepository --> PeerReviewRequest
```

### 🔄 3. Sequence Diagram (UC-13.1)
```mermaid
sequenceDiagram
    autonumber
    actor Requester as MC A (Requester)
    participant Controller as PeerReviewController
    participant Service as PeerReviewServiceImpl
    participant DB as MongoDB Atlas

    Requester->>Controller: POST /api/v1/peer-reviews/request (practiceSessionId)
    Controller->>Service: requestReview(mcUserId, practiceSessionId)
    Service->>DB: findPracticeSessionById(practiceSessionId)
    DB-->>Service: PracticeSession Record
    
    Service->>DB: save(PeerReviewRequest: status = PENDING, audioUrl = session.audioUrl)
    DB-->>Service: Saved Record
    Service-->>Controller: PeerReviewRequestDTO
    Controller-->>Requester: 201 Created (Đã gửi yêu cầu nhận xét bài luyện tới cộng đồng MC)
```

### 🧪 4. Testing & Verification (UC-13.1)
- **Unit Test Method:** `PeerReviewServiceImplTest.java` -> `requestReview_success_createsPendingRequest()`
- **Assertions:** Tạo `PeerReviewRequest` với trạng thái `PENDING`.

---

## 🎧 UC-13.2: Danh Sách Bài Tập Chờ Review (Get Pending Peer Reviews)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** MC (Reviewer).
- **Mục tiêu:** Tra cứu danh sách các bài luyện giọng từ đồng nghiệp đang chờ nhận xét (phân trang).
- **Endpoint:** `GET /api/v1/peer-reviews/pending`

### 📐 2. Class Diagram (UC-13.2)
```mermaid
classDiagram
    class PeerReviewController {
        +getPendingReviews(Pageable pageable) ResponseEntity~ApiResponse~
    }
    PeerReviewController --> PeerReviewRequestRepository
```

### 🔄 3. Sequence Diagram (UC-13.2)
```mermaid
sequenceDiagram
    autonumber
    actor Reviewer as MC B (Reviewer)
    participant Controller as PeerReviewController
    participant DB as MongoDB Atlas

    Reviewer->>Controller: GET /api/v1/peer-reviews/pending?page=0&size=10
    Controller->>DB: findByStatusOrderByCreatedAtDesc("PENDING", Pageable)
    DB-->>Controller: Page<PeerReviewRequest>
    Controller-->>Reviewer: 200 OK (Danh sách 10 bài luyện đang chờ review)
```

### 🧪 4. Testing & Verification (UC-13.2)
- **Unit Test Method:** `PeerReviewControllerTest.java` -> `getPending_returnsPendingRequests()`
- **Assertions:** Trả về danh sách bài có trạng thái `PENDING`.

---

## 🎧 UC-13.3: Chấm Điểm & Gửi Feedback Chuyên Môn (Submit Peer Review)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** MC (Reviewer).
- **Mục tiêu:** Nghe audio bài luyện, chấm điểm 1-5 sao và gửi lời khuyên chuyên môn cho đồng nghiệp.
- **Rules:** MC không được tự chấm điểm bài luyện của chính mình (`requesterMcId != reviewerMcId`).
- **Endpoint:** `POST /api/v1/peer-reviews/{id}/review`

### 📐 2. Class Diagram (UC-13.3)
```mermaid
classDiagram
    class PeerReviewController {
        +submitReview(String id, SubmitReviewDTO req) ResponseEntity~ApiResponse~
    }
    class ReviewFeedback {
        +String reviewerMcId
        +int rating
        +String comment
        +LocalDateTime createdAt
    }
    PeerReviewController --> PeerReviewService
    PeerReviewService --> ReviewFeedback
```

### 🔄 3. Sequence Diagram (UC-13.3)
```mermaid
sequenceDiagram
    autonumber
    actor Reviewer as MC B (Reviewer)
    participant Controller as PeerReviewController
    participant Service as PeerReviewServiceImpl
    participant DB as MongoDB Atlas
    actor Requester as MC A (Requester)

    Reviewer->>Controller: POST /api/v1/peer-reviews/{id}/review (rating = 5, comment = "Nhấn giọng rất chuẩn!")
    Controller->>Service: submitReview(reviewerMcId, requestId, rating, comment)
    Service->>DB: findById(requestId)
    DB-->>Service: PeerReviewRequest Record
    
    alt Reviewer chính là Requester (Tự review bài mình)
        Service-->>Controller: AppException(CANNOT_REVIEW_OWN_PRACTICE)
        Controller-->>Reviewer: 400 Bad Request
    else Hợp Lệ
        Service->>Service: Thêm ReviewFeedback (rating = 5, comment)
        Service->>DB: save(PeerReviewRequest)
        DB-->>Service: Saved Record
        
        Service-->>Controller: PeerReviewResultDTO
        Controller-->>Reviewer: 200 OK (Đã gửi nhận xét thành công)
        note over Requester: Requester Nhận Thông Báo Có Review Mới
    end
```

### 🧪 4. Testing & Verification (UC-13.3)
- **Unit Test Method:** `PeerReviewServiceImplTest.java` -> `submitReview_cannotReviewOwnPractice_throwsException()`
- **Assertions:** Ngăn chặn thành công việc tự chấm điểm bài cá nhân, lưu đúng feedback khi hợp lệ.

---

## 🎧 UC-13.4: Xem Phản Hồi Đã Nhận (Get Received Peer Reviews)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** MC (Requester).
- **Mục tiêu:** Xem lại tất cả ý kiến đóng góp, điểm số và nhận xét chuyên môn từ các đồng nghiệp dành cho bài luyện giọng của mình.
- **Endpoint:** `GET /api/v1/peer-reviews/my-reviews`

### 📐 2. Class Diagram (UC-13.4)
```mermaid
classDiagram
    class PeerReviewController {
        +getMyReviews() ResponseEntity~ApiResponse~
    }
    PeerReviewController --> PeerReviewRequestRepository
```

### 🔄 3. Sequence Diagram (UC-13.4)
```mermaid
sequenceDiagram
    autonumber
    actor Requester as MC A (Requester)
    participant Controller as PeerReviewController
    participant DB as MongoDB Atlas

    Requester->>Controller: GET /api/v1/peer-reviews/my-reviews
    Controller->>DB: findByRequesterMcId(currentUserId)
    DB-->>Controller: List<PeerReviewRequest> with Feedbacks
    Controller-->>Requester: 200 OK (Danh sách các nhận xét chuyên môn từ đồng nghiệp)
```

### 🧪 4. Testing & Verification (UC-13.4)
- **Unit Test Method:** `PeerReviewControllerTest.java` -> `getMyReviews_returnsUserRequests()`
- **Assertions:** Trả về danh sách review thuộc về `currentUserId`.

---

## 🎧 UC-13.5: Kiểm Duyệt & Xóa Review Vi Phạm Admin (Admin Peer Review Control)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Admin.
- **Mục tiêu:** Tra cứu danh sách đánh giá đồng nghiệp toàn hệ thống và gỡ bỏ các nhận xét toxic/vi phạm quy chuẩn.
- **Endpoint:** `GET /api/v1/admin/peer-reviews`, `DELETE /api/v1/admin/peer-reviews/{id}`

### 📐 2. Class Diagram (UC-13.5)
```mermaid
classDiagram
    class AdminPeerReviewController {
        +getAllReviews(Pageable pageable) ResponseEntity~ApiResponse~
        +deleteReview(String id) ResponseEntity~ApiResponse~
    }
    AdminPeerReviewController --> PeerReviewService
```

### 🔄 3. Sequence Diagram (UC-13.5)
```mermaid
sequenceDiagram
    autonumber
    actor Admin as System Admin
    participant Controller as AdminPeerReviewController
    participant Service as PeerReviewServiceImpl
    participant DB as MongoDB Atlas

    Admin->>Controller: DELETE /api/v1/admin/peer-reviews/{id}
    Controller->>Service: deleteReview(adminId, reviewId)
    Service->>DB: deleteById(reviewId)
    DB-->>Service: Deleted Confirmation
    Controller-->>Admin: 200 OK (Đã xóa nhận xét vi phạm khỏi hệ thống)
```

### 🧪 4. Testing & Verification (UC-13.5)
- **Unit Test Method:** `AdminPeerReviewControllerTest.java` -> `deleteReview_adminRole_deletesToxicReview()`
- **Assertions:** Bản ghi `PeerReviewRequest` bị xóa thành công khỏi database.
