# UC-04 — Đào Tạo & Khóa Học (Courses & Learning Path)

Tài liệu thiết kế chi tiết từng Use Case con (Sub-UC) thuộc luồng Đào tạo, Khóa học và Cấp chứng chỉ.

---

## 📚 UC-04.1: Danh Sách & Chi Tiết Khóa Học (Get Courses & Details)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** Guest / User.
- **Mục tiêu:** Tra cứu danh sách các khóa học đào tạo kỹ năng MC, xem giáo trình, giảng viên phụ trách và danh sách bài học dùng thử (Free Preview).
- **Endpoint:** `GET /api/v1/courses`, `GET /api/v1/courses/{id}`

### 📐 2. Class Diagram (UC-04.1)
```mermaid
classDiagram
    class CourseController {
        +getCourses() ResponseEntity~ApiResponse~
        +getCourseById(String id) ResponseEntity~ApiResponse~
    }
    class Course {
        +String id
        +String title
        +String description
        +int price
        +List~Lesson~ lessons
    }
    CourseController --> CourseRepository
    CourseRepository --> Course
```

### 🔄 3. Sequence Diagram (UC-04.1)
```mermaid
sequenceDiagram
    autonumber
    actor User as Client / Student
    participant Controller as CourseController
    participant DB as MongoDB Atlas

    User->>Controller: GET /api/v1/courses/{id}
    Controller->>DB: findById(id)
    DB-->>Controller: Course Record
    Controller-->>User: 200 OK (Chi tiết giáo trình khóa học & video mẫu)
```

### 🧪 4. Testing & Verification (UC-04.1)
- **Unit Test Method:** `CourseControllerTest.java` -> `getCourseById_validId_returnsCourse()`
- **Assertions:** Trả về đối tượng `Course` có đúng `id`.

---

## 📚 UC-04.2: Đăng Ký & Mua Khóa Học (Enroll Course)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Đăng ký khóa học (miễn phí cho VIP hoặc mua khóa lẻ). Tạo bản ghi `CourseEnrollment`.
- **Endpoint:** `POST /api/v1/courses/{id}/enroll`

### 📐 2. Class Diagram (UC-04.2)
```mermaid
classDiagram
    class CourseController {
        +enrollCourse(String id) ResponseEntity~ApiResponse~
    }
    class CourseEnrollment {
        +String id
        +String userId
        +String courseId
        +double progressPercent
        +boolean isCompleted
    }
    CourseController --> CourseEnrollmentRepository
    CourseEnrollmentRepository --> CourseEnrollment
```

### 🔄 3. Sequence Diagram (UC-04.2)
```mermaid
sequenceDiagram
    autonumber
    actor Student as User
    participant Controller as CourseController
    participant DB as MongoDB Atlas

    Student->>Controller: POST /api/v1/courses/{id}/enroll
    Controller->>DB: existsByUserIdAndCourseId(userId, courseId)
    DB-->>Controller: false (Chưa đăng ký)
    
    Controller->>DB: save(CourseEnrollment: progressPercent = 0, isCompleted = false)
    DB-->>Controller: Saved Enrollment Record
    Controller-->>Student: 200 OK (Đã đăng ký khóa học thành công)
```

### 🧪 4. Testing & Verification (UC-04.2)
- **Unit Test Method:** `CourseServiceImplTest.java` -> `enroll_success()`
- **Assertions:** Bản ghi `CourseEnrollment` được khởi tạo với tiến độ `0%`.

---

## 📚 UC-04.3: Theo Dõi Tiến Độ Học Tập (Course Progress)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Lấy phần trăm hoàn thành khóa học (`progressPercent`), danh sách bài đã học và vị trí bài học tiếp theo.
- **Endpoint:** `GET /api/v1/courses/{id}/progress`

### 📐 2. Class Diagram (UC-04.3)
```mermaid
classDiagram
    class CourseController {
        +getProgress(String id) ResponseEntity~ApiResponse~
    }
    CourseController --> CourseEnrollmentRepository
```

### 🔄 3. Sequence Diagram (UC-04.3)
```mermaid
sequenceDiagram
    autonumber
    actor Student as User
    participant Controller as CourseController
    participant DB as MongoDB Atlas

    Student->>Controller: GET /api/v1/courses/{id}/progress
    Controller->>DB: findByUserIdAndCourseId(userId, courseId)
    DB-->>Controller: CourseEnrollment Record
    Controller-->>Student: 200 OK (Progress % & Completed Lesson IDs)
```

### 🧪 4. Testing & Verification (UC-04.3)
- **Unit Test Method:** `CourseControllerTest.java` -> `getProgress_returnsCurrentProgress()`
- **Assertions:** Phần trăm tiến độ khớp với số bài học đã xong.

---

## 📚 UC-04.4: Hoàn Thành Bài Học Video (Complete Lesson)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Đánh dấu hoàn thành 1 bài học nhỏ trong khóa học, tự động tính toán lại % tổng tiến độ khóa học.
- **Endpoint:** `POST /api/v1/courses/{id}/lessons/{lessonId}/complete`

### 📐 2. Class Diagram (UC-04.4)
```mermaid
classDiagram
    class CourseController {
        +completeLesson(String id, String lessonId) ResponseEntity~ApiResponse~
    }
    CourseController --> CourseServiceImpl
```

### 🔄 3. Sequence Diagram (UC-04.4)
```mermaid
sequenceDiagram
    autonumber
    actor Student as User
    participant Controller as CourseController
    participant Service as CourseServiceImpl
    participant DB as MongoDB Atlas

    Student->>Controller: POST /api/v1/courses/{id}/lessons/{lessonId}/complete
    Controller->>Service: completeLesson(userId, courseId, lessonId)
    Service->>DB: findEnrollment(userId, courseId)
    DB-->>Service: CourseEnrollment Record
    
    Service->>Service: Thêm lessonId vào completedLessons list & Recalculate % progress
    Service->>DB: save(CourseEnrollment)
    Service-->>Controller: ProgressDTO
    Controller-->>Student: 200 OK (Cập nhật tiến độ mới)
```

### 🧪 4. Testing & Verification (UC-04.4)
- **Unit Test Method:** `CourseServiceImplTest.java` -> `completeLesson_updatesProgressPercent()`
- **Assertions:** Số lượng bài hoàn thành tăng 1, tiến độ % được cộng thêm.

---

## 📚 UC-04.5: Nộp Bài Quiz & Auto Certificate (Submit Quiz & Auto Cert)

### 📌 1. Mô tả Chi Tiết & Quy Tắc Nghiệp Vụ
- **Actor:** User.
- **Mục tiêu:** Nộp bài trắc nghiệm kiểm tra cuối khóa. Nếu điểm đạt >= 80%, tự động cấp chứng chỉ hoàn thành khóa học vào kho `Certificate`.
- **Endpoint:** `POST /api/v1/courses/{id}/quiz/submit`

### 📐 2. Class Diagram (UC-04.5)
```mermaid
classDiagram
    class CourseController {
        +submitQuiz(String id, QuizSubmitRequestDTO req) ResponseEntity~ApiResponse~
    }
    class Certificate {
        +String id
        +String userId
        +String courseId
        +String certNumber
        +LocalDateTime issuedAt
    }
    CourseController --> CertificateRepository
    CertificateRepository --> Certificate
```

### 🔄 3. Sequence Diagram (UC-04.5)
```mermaid
sequenceDiagram
    autonumber
    actor Student as User
    participant Controller as CourseController
    participant Service as CourseServiceImpl
    participant CertRepo as CertificateRepository
    participant DB as MongoDB Atlas

    Student->>Controller: POST /api/v1/courses/{id}/quiz/submit (answers)
    Controller->>Service: submitQuiz(userId, courseId, answers)
    Service->>Service: Chấm điểm các câu trắc nghiệm
    
    alt Điểm số < 80%
        Service-->>Controller: QuizResultDTO (passed = false, score = 65%)
        Controller-->>Student: 200 OK (Chưa đạt, hãy xem lại lý thuyết và làm lại)
    else Điểm số >= 80%
        Service->>DB: Update CourseEnrollment (isCompleted = true, progress = 100%)
        Service->>CertRepo: save(Certificate: userId, courseId, certNumber, issuedAt = now)
        CertRepo-->>Service: Certificate Record
        Service-->>Controller: QuizResultDTO (passed = true, score = 90%, certId)
        Controller-->>Student: 200 OK (Đạt điểm xuất sắc & Tự động cấp chứng chỉ)
    end
```

### 🧪 4. Testing & Verification (UC-04.5)
- **Unit Test Method:** `CourseServiceImplTest.java` -> `submitQuiz_passed_createsCertificate()`
- **Assertions:** Tạo chứng chỉ mới khi điểm >= 80, gán đúng `courseId` và `userId`.
