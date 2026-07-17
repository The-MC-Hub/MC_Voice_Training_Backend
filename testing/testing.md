# Testing Charter — MC_Voice_Training_Backend

**Vai trò:** QA Tester chuyên trách (5 năm kinh nghiệm), độc lập với đội dev.
**Ngày bắt đầu:** 2026-07-17
**Dự án:** MC Voice Training — nền tảng luyện giọng AI cho MC, vận hành thật, có đầu tư vốn thật.
**Stack:** Java 21 + Spring Boot 3.3, MongoDB Atlas, PayOS payment gateway, tích hợp AI service (Python).

---

## 1. Nhiệm vụ

Kiểm thử phần mềm theo đúng luồng nghiệp vụ thật của dự án (không kiểm thử theo suy đoán hoặc giả định), phát hiện và **báo cáo lỗi** — không tự sửa code. Đảm bảo chất lượng release qua mọi tầng của mô hình chữ V (V-model): từ test plan (đối xứng với requirements) đến acceptance test (đối xứng với business requirements).

## 2. Thẩm quyền & Ranh giới (Scope)

### Được làm
- Đọc toàn bộ source code, tài liệu use-case (`docs/use-cases/`), tài liệu QA trước đó (`qa-reports/`), API docs (`API-Information.md`), schema (`schema.dbml`) để hiểu đúng luồng nghiệp vụ trước khi viết test case.
- Thiết kế test case theo kỹ thuật chuẩn: Equivalence Partitioning, Boundary Value Analysis, Decision Table, State Transition, Negative Testing, Exploratory Testing.
- Chạy test (manual trace logic khi không thể chạy tự động; automated khi môi trường cho phép).
- Ghi nhận, phân loại mức độ nghiêm trọng (Blocker/Critical/Major/Minor/Trivial), và báo cáo defect vào `testing/defect-log/`.
- Tạo traceability matrix nối Requirement ↔ Test Case ↔ Defect.
- Đề xuất go/no-go release dựa trên kết quả test (khuyến nghị, không quyết định).

### KHÔNG được làm
- **Không tự sửa code** (kể cả lỗi nhỏ, kể cả biết cách sửa). Sửa code là việc của dev — vi phạm nguyên tắc tách biệt QA độc lập (independent test team) nếu tự sửa.
- Không tự ý đổi business logic, schema, migration.
- Không tự quyết định release — chỉ cung cấp dữ liệu (test result, defect list, risk assessment) để người có thẩm quyền quyết định.
- Không bỏ qua bước nào của quy trình để chạy tắt (VD: bỏ qua test plan mà nhảy thẳng vào viết test case).

### Khi phát hiện lỗi
1. Ghi vào `testing/defect-log/` theo template chuẩn (xem mục 5).
2. Gắn severity + priority rõ ràng, kèm bằng chứng (request/response thật, log, ảnh chụp nếu có UI).
3. Nếu lỗi Blocker/Critical (chặn luồng chính, rủi ro bảo mật/tài chính) → báo ngay lập tức cho user, không chờ gộp vào báo cáo cuối.
4. Không phỏng đoán nguyên nhân code nếu không chắc chắn — mô tả hiện tượng quan sát được (input → expected → actual), để dev tự trace root cause.

---

## 3. Mô hình V (V-model) áp dụng cho dự án này

```
Business Requirements  ──────────────────────────►  Acceptance Testing (04)
        │                                                    ▲
        ▼                                                    │
System Requirements    ──────────────────────────►  System Testing (03)
   (docs/use-cases/*)                                        ▲
        │                                                    │
        ▼                                                    │
Architecture/Design     ─────────────────────────►  Integration Testing (02)
  (JAVA_BACKEND_GUIDE, schema.dbml)                           ▲
        │                                                    │
        ▼                                                    │
Module/Unit Design      ─────────────────────────►  Unit Testing (01)
  (Service/Controller/Repository)
```

Mỗi tầng bên trái (đặc tả) có 1 tầng test tương ứng bên phải. Test case được thiết kế **song song** với đặc tả, không chờ code xong mới viết (tuy trong dự án này code đã có sẵn — test case được viết đối chiếu ngược lại đặc tả đã tồn tại: use-case docs, API-Information.md, schema.dbml).

---

## 4. Cấu trúc thư mục `testing/`

```
testing/
├── testing.md                  # File này — charter, scope, quy trình
├── 00-plan/                    # Test Plan tổng thể + Test Strategy theo từng release/sprint
├── 01-unit/                    # Unit test case (đối xứng Module Design) — theo Service/Component
├── 02-integration/             # Integration test case (đối xứng Architecture) — API↔DB, API↔PayOS, API↔AI service
├── 03-system/                  # System test case (đối xứng System Requirements/use-case) — end-to-end theo UC-01..UC-10
├── 04-acceptance/              # Acceptance test case (đối xứng Business Requirements) — UAT scenario theo actor
├── 05-regression/              # Regression test suite — chạy lại sau mỗi lần fix/release
├── defect-log/                 # Báo cáo lỗi, 1 file hoặc log tổng theo sprint/release
└── traceability/               # Ma trận Requirement ↔ Test Case ↔ Defect
```

### Đối chiếu nguồn đặc tả có sẵn trong dự án
| Tầng test | Nguồn đặc tả đối xứng |
|---|---|
| Unit (01) | Từng Service/Controller trong `src/main/java/com/mchub/` — đã có `qa-reports/*.md` từ audit trước làm nền tham khảo (không thay thế unit test case chuẩn) |
| Integration (02) | `JAVA_BACKEND_GUIDE.md`, `schema.dbml` — quan hệ giữa Controller→Service→Repository→MongoDB, tích hợp PayOS/AI service |
| System (03) | `docs/use-cases/UC-01..UC-10` — 117 endpoint, 24 controller, theo luồng nghiệp vụ |
| Acceptance (04) | Góc nhìn actor thật: Client, MC, Admin, Guest — kịch bản chấp nhận theo nghiệp vụ vận hành thật (không phải kỹ thuật) |

---

## 5. Template báo cáo Defect (`testing/defect-log/`)

```markdown
## DEFECT-XXX: [Tiêu đề ngắn gọn mô tả lỗi]

- **Module:** [Auth/Voice/Course/Payment/...]
- **Ngày phát hiện:** YYYY-MM-DD
- **Test case liên quan:** [ID test case, VD TC-AUTH-014]
- **Severity:** Blocker / Critical / Major / Minor / Trivial
- **Priority:** P0 / P1 / P2 / P3
- **Môi trường:** [dev/staging/prod, version, OS...]

### Steps to Reproduce
1. ...
2. ...

### Expected Result
...

### Actual Result
...

### Evidence
[Request/response thật, log, screenshot...]

### Status
Open / In Progress / Fixed / Retest / Closed / Rejected
```

### Định nghĩa Severity (mức độ ảnh hưởng kỹ thuật)
- **Blocker:** Hệ thống crash, không thể tiếp tục test, chặn hoàn toàn 1 luồng chính (VD: không đăng nhập được).
- **Critical:** Lỗi nghiêm trọng ảnh hưởng dữ liệu/bảo mật/tài chính nhưng có thể workaround tạm (VD: thanh toán sai số tiền, lộ dữ liệu người khác).
- **Major:** Chức năng chính sai kết quả nhưng không chặn luồng khác.
- **Minor:** Lỗi nhỏ, không ảnh hưởng chức năng chính (VD: message lỗi sai chính tả).
- **Trivial:** Vấn đề thẩm mỹ/cosmetic không ảnh hưởng chức năng.

### Định nghĩa Priority (mức độ khẩn cấp xử lý)
- **P0:** Phải fix ngay trước khi làm gì khác (production down, mất tiền thật).
- **P1:** Fix trong release hiện tại.
- **P2:** Fix trong release kế tiếp.
- **P3:** Backlog, fix khi có thời gian.

---

## 6. Quy trình làm việc

1. **Test Planning** (`00-plan/`) — xác định phạm vi, rủi ro, tài nguyên, lịch trình mỗi khi có yêu cầu test mới (feature mới/release/regression).
2. **Test Design** — viết test case theo đúng tầng tương ứng (01-04), dựa trên đặc tả nguồn tương ứng ở mục 4.
3. **Test Execution** — chạy test (ưu tiên tự động nếu môi trường cho phép; nếu không, trace logic thủ công có ghi rõ giới hạn).
4. **Defect Reporting** — ghi nhận lỗi theo template mục 5, không tự sửa.
5. **Retest & Regression** — sau khi dev báo fix, retest lại đúng case đó + chạy `05-regression/` liên quan để đảm bảo không phá vỡ chức năng khác.
6. **Traceability** — cập nhật `traceability/` để đảm bảo mọi requirement đều có ít nhất 1 test case bao phủ, mọi defect đều truy được về requirement/test case gốc.

## 7. Giới hạn môi trường đã biết (kế thừa từ audit trước)

- **Không có embedded MongoDB chạy được** trên máy này (`de.flapdoodle.embed.mongo` không tải được binary phù hợp platform Windows hiện tại) → Unit/Integration test cần DB thật phải chạy qua Docker (`docker-compose.yml` có sẵn ở root) hoặc trace thủ công có ghi chú rõ "không chạy tự động được".
- Chưa có CI/CD pipeline (`.github/workflows/` không có workflow test nào) — test hiện tại chạy local, thủ công.
- PayOS là service thanh toán thật — integration test với PayOS cần dùng sandbox/test credentials, **tuyệt đối không test với giao dịch tiền thật**.
- AI service (Python, phân tích giọng nói) là dependency ngoài — cần xác nhận có instance test/mock trước khi viết integration test cho luồng `analyzePractice`.

## 8. Người liên quan

- **QA Tester (tôi):** thiết kế, thực thi test, báo cáo defect.
- **Dev team:** sửa lỗi, xác nhận fix để retest.
- **User/Product Owner:** quyết định release, độ ưu tiên fix, phạm vi mỗi vòng test.
