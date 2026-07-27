# UC-13 — Đánh Giá Đồng Nghiệp (Peer Review & Feedback)

## 📌 1. Mô tả Tổng Quan & Luồng Nghiệp Vụ

Luồng nghiệp vụ chuyên sâu dành cho cộng đồng MC: MC gửi yêu cầu đánh giá bài ghi âm luyện giọng của mình tới các MC khác trong cộng đồng để nhận góp ý chuyên môn, chấm điểm chéo và Admin quản lý/gỡ bỏ các đánh giá vi phạm.

### Actors
- **MC (Requester)**: MC gửi bài luyện giọng yêu cầu nhận xét.
- **MC (Reviewer)**: MC nghe file ghi âm, chấm 1-5 sao và đưa ra lời khuyên chuyên môn.
- **Admin**: Quản trị viên xóa review toxic/vi phạm quy chuẩn cộng đồng.

---

## 🛠️ 2. Chi Tiết Tính Năng & Điểm Nghiệp Vụ

| # | Tính năng | Mô tả Nghiệp Vụ Chi Tiết | Controller & API Endpoint |
|---|---|---|---|
| 1 | Gửi yêu cầu Peer Review | MC gửi bài luyện giọng đã hoàn thành vào danh sách chờ nhận xét từ đồng nghiệp | `POST /api/v1/peer-reviews/request` |
| 2 | Danh sách bài chờ review | Lấy danh sách các bài luyện giọng từ đồng nghiệp đang chờ được nhận xét | `GET /api/v1/peer-reviews/pending` |
| 3 | Chấm điểm & Viết feedback | MC khác nghe audio, chấm điểm (1-5 sao) và viết lời khuyên chuyên môn | `POST /api/v1/peer-reviews/{id}/review` |
| 4 | Xem phản hồi đã nhận | MC gửi bài xem lại toàn bộ đánh giá, nhận xét và điểm số từ các đồng nghiệp | `GET /api/v1/peer-reviews/my-reviews` |
| 5 | Quản lý Peer Review Admin | Admin xem bảng thống kê review, xem điểm trung bình và xóa review vi phạm | `GET /api/v1/admin/peer-reviews`, `DELETE /api/v1/admin/peer-reviews/{id}` |

---

## 📐 3. Class Diagram

```mermaid
classDiagram
    class PeerReviewController {
        +requestReview(req) ResponseEntity
        +getPendingReviews(page) ResponseEntity
        +submitReview(id, req) ResponseEntity
        +getMyReviews() ResponseEntity
    }

    class AdminPeerReviewController {
        +getAllReviews(page) ResponseEntity
        +deleteReview(id) ResponseEntity
    }

    class PeerReviewService {
        <<interface>>
        +requestReview(mcUserId, practiceSessionId) PeerReviewRequestDTO
        +submitReview(reviewerMcId, requestId, rating, comment) PeerReviewResultDTO
        +deleteReview(adminId, reviewId) void
    }

    class PeerReviewRequest {
        +String id
        +String requesterMcId
        +String practiceSessionId
        +String audioUrl
        +PeerReviewStatus status
        +List~ReviewFeedback~ feedbacks
        +LocalDateTime createdAt
    }

    class ReviewFeedback {
        +String reviewerMcId
        +int rating
        +String comment
        +LocalDateTime createdAt
    }

    PeerReviewController --> PeerReviewService
    AdminPeerReviewController --> PeerReviewService
    PeerReviewService --> PeerReviewRequestRepository
    PeerReviewRequestRepository --> PeerReviewRequest
    PeerReviewRequest --> ReviewFeedback
```

---

## 🔄 4. Sequence Diagram (Gửi Nhận Xét Chuyên Môn Cho Bài Luyện Của Đồng Nghiệp)

```mermaid
sequenceDiagram
    autonumber
    actor Reviewer as MC B (Reviewer)
    participant Controller as PeerReviewController
    participant Service as PeerReviewServiceImpl
    participant DB as MongoDB Atlas
    actor Requester as MC A (Requester)

    Reviewer->>Controller: POST /api/v1/peer-reviews/{id}/review (rating = 5, comment = "Phát âm rất tròn vành rõ chữ...")
    Controller->>Service: submitReview(reviewerMcId, requestId, rating, comment)
    Service->>DB: findById(requestId)
    DB-->>Service: PeerReviewRequest Record
    
    alt Reviewer chính là Requester (Tự review bài mình)
        Service-->>Controller: AppException(CANNOT_REVIEW_OWN_PRACTICE)
        Controller-->>Reviewer: 400 Bad Request
    else Hợp lệ
        Service->>Service: Thêm ReviewFeedback (rating = 5, comment)
        Service->>DB: save(PeerReviewRequest)
        DB-->>Service: Updated Record
        
        Service-->>Controller: PeerReviewResultDTO
        Controller-->>Reviewer: 200 OK (Đã gửi nhận xét chuyên môn thành công)
        
        note over Requester: Requester Nhận Thông Báo Có Góp Ý Mới
    end
```

---

## 🧪 5. Testing & Verification Report

- **Test Suite Classes:**
  - `com.mchub.controllers.PeerReviewControllerTest`
  - `com.mchub.controllers.AdminPeerReviewControllerTest`
  - `com.mchub.services.impl.PeerReviewServiceImplTest`
- **Các kịch bản kiểm thử đã thực thi:**
  - `requestReview_success_createsPendingRequest()`: Tạo bài chờ nhận xét thành công.
  - `submitReview_cannotReviewOwnPractice_throwsException()`: Ngăn chặn MC tự chấm điểm bài của chính mình.
  - `deleteReview_adminRole_deletesToxicReview()`: Admin xóa thành công các đánh giá không phù hợp.
- **Kết quả kiểm thử:** Pass **100% (18/18 unit tests trong module Peer Review & Feedback)**.
