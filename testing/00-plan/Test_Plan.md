# Test Plan — MC Voice Training Backend

**Version:** 1.0
**Ngày:** 2026-07-17
**Tác giả:** QA Tester (độc lập, theo `testing/testing.md`)
**Trạng thái:** Draft — chờ phê duyệt phạm vi trước khi vào Test Design

---

## 1. Giới thiệu

### 1.1. Mục đích
Xác định phạm vi, chiến lược, tài nguyên, lịch trình và tiêu chí chấp nhận cho việc kiểm thử `MC_Voice_Training_Backend` — hệ thống vận hành thật, có giao dịch tài chính thật qua PayOS.

### 1.2. Đối tượng kiểm thử (System Under Test)
- **Component:** `MC_Voice_Training_Backend` (Java 21, Spring Boot 3.3, MongoDB Atlas)
- **Quy mô xác nhận được:** 117 endpoint / 24 controller (theo `docs/use-cases/README.md`, đối chiếu trực tiếp từ source `src/main/java/com/mchub/controllers/`)
- **10 luồng nghiệp vụ chính** (UC-01 → UC-10, xem mục 3)

### 1.3. Lưu ý quan trọng về nguồn tài liệu
Phát hiện trong quá trình khảo sát: `API-Information.md` và `schema.dbml` ở root project **là tài liệu của module khác** (`The-MC-Hub-Java-Backend` — nền tảng booking MC, có collection `bookings`/`mcprofiles` khác hoàn toàn) — **KHÔNG phải tài liệu của backend đang kiểm thử**. Không dùng 2 file này làm nguồn đặc tả cho test plan này. Nguồn đặc tả chính thức duy nhất đáng tin cậy: `docs/use-cases/UC-01..UC-10` (đối chiếu trực tiếp từ controller source) + đọc trực tiếp source code khi cần chi tiết kỹ thuật.

**Khuyến nghị gửi dev:** cần dọn/tách rõ 2 file `API-Information.md`/`schema.dbml` này ra khỏi thư mục `MC_Voice_Training_Backend` (copy nhầm từ module khác), tránh gây nhầm lẫn cho người mới vào dự án.

---

## 2. Phạm vi kiểm thử (Scope)

### 2.1. Trong phạm vi (In-Scope)
- Toàn bộ 117 endpoint thuộc 24 controller, theo 10 luồng UC.
- Luồng thanh toán thật qua PayOS (sandbox/test credentials — **không test với giao dịch tiền thật**).
- Tích hợp AI voice-scoring service, TTS service (thông qua sandbox/mock nếu có, xác nhận trước khi test).
- Luồng email (OTP, thông báo, campaign) — kiểm tra gửi đúng, không kiểm tra nội dung hiển thị trình email client cụ thể (out of scope hiển thị).
- Authorization/ownership theo từng actor: Guest, User (Client/MC), Admin, System.
- Business logic: giới hạn gói cước, tính điểm, tính streak, tính hoa hồng/giảm giá.

### 2.2. Ngoài phạm vi (Out-of-Scope, giai đoạn này)
- Frontend (Web/Mobile) — chỉ test backend API trực tiếp (qua HTTP client/Postman/script), không test UI.
- Load/Performance/Stress testing chính thức (không có tiêu chí SLA được cung cấp) — chỉ ghi nhận nghi vấn hiệu năng nếu phát hiện qua code (VD: N+1 query) để dev tự đánh giá.
- Penetration testing chuyên sâu (out of scope cho QA functional — cần chuyên gia security riêng nếu công ty yêu cầu).
- Test module khác trong monorepo (`The-MC-Hub-Java-Backend`, `FPT_S7_*`, `The-MC-Hub-LandingPage`...) — không thuộc phạm vi lần này.

### 2.3. Giả định & Ràng buộc
- Không có CI/CD pipeline chạy test tự động (`.github/workflows/` không có workflow test) — test chạy local/thủ công.
- Không thể chạy embedded MongoDB tự động trên máy hiện tại (giới hạn môi trường đã ghi trong `testing/testing.md` mục 7) — cần MongoDB thật (local Docker qua `docker-compose.yml` có sẵn, hoặc Atlas dev cluster riêng) để chạy integration/system test thật.
- PayOS cần test credentials riêng (sandbox), chưa xác nhận đã có hay chưa — **cần hỏi dev/user trước khi bắt đầu UC-06**.
- AI voice service là dependency ngoài (Python service riêng) — cần xác nhận endpoint test/mock trước khi test UC-03.

---

## 3. Ma trận rủi ro theo luồng nghiệp vụ (Risk-Based Prioritization)

| # | UC | Luồng | Actor chính | Rủi ro tài chính | Rủi ro bảo mật | Tích hợp ngoài | Độ phức tạp | **Ưu tiên test** |
|---|---|---|---|:---:|:---:|---|:---:|:---:|
| 1 | UC-06 | Payment & Subscription | User, Admin, PayOS(System) | **Cao** | **Cao** | PayOS webhook | Rất cao (2 luồng checkout song song, webhook async, admin bypass path) | **P0** |
| 2 | UC-09 | Admin Dashboard | Admin, AI(System) | Trung bình (đọc revenue) | **Cao** (xóa user vĩnh viễn, migration DB, log ingestion không rõ auth) | AI log ingestion, SSE | Cao | **P0** |
| 3 | UC-03 | Voice Training (core) | User, Guest, AI(System) | Thấp (gián tiếp qua quota) | Trung bình (guest cooldown bypass) | AI scoring, TTS | Rất cao (2 luồng phân tích song song, async, guest abuse) | **P0** |
| 4 | UC-04 | Courses & Learning | User, Admin | Trung bình (mua lẻ course) | Trung bình (gating subscription vs purchase) | — | Cao (quiz→certificate transaction) | **P1** |
| 5 | UC-10 | Marketing & Communication | Admin, System | Thấp | Thấp | Email (bulk send) | Cao (6 auto-draft trigger liên module) | **P1** |
| 6 | UC-01 | Authentication | Guest, User, Admin | — | **Cao** (cổng vào toàn hệ thống) | Email | Trung bình (2 luồng verify song song, admin 2FA) | **P1** |
| 7 | UC-02 | User/MC Profile | User, MC, Admin | — | Trung bình | — | Trung bình (streak freeze, certificate approval workflow) | **P2** |
| 8 | UC-05 | Community & Leaderboard | User, Admin | — | Thấp | — | Trung bình (tính toán đa tiêu chí, đa kỳ hạn) | **P2** |
| 9 | UC-07 | Onboarding Quest | User | Thấp (voucher issuance) | Thấp | — | Thấp (nhưng cần test idempotency) | **P2** |
| 10 | UC-08 | Public/Support | Guest, User, Admin | — | Trung bình (file upload) | Email, file storage | Thấp | **P3** |

**Thứ tự thực thi đề xuất:** P0 trước (UC-06 → UC-09 → UC-03), sau đó P1 (UC-04 → UC-10 → UC-01), P2, P3 theo tài nguyên còn lại.

---

## 4. Chiến lược kiểm thử theo tầng V-model

| Tầng | Kỹ thuật áp dụng | Công cụ dự kiến | Thư mục output |
|---|---|---|---|
| Unit | EP, BVA, mock dependency | JUnit5 + Mockito (nếu dev cung cấp/cho phép chạy); nếu không, trace logic thủ công có ghi chú | `testing/01-unit/` |
| Integration | API↔DB thật, API↔PayOS sandbox, API↔AI service | Postman/Newman hoặc script HTTP thật, MongoDB Docker | `testing/02-integration/` |
| System | End-to-end theo từng UC, đa actor | Postman collection theo luồng, hoặc script kịch bản thật | `testing/03-system/` |
| Acceptance | Kịch bản nghiệp vụ thật theo góc nhìn actor (không kỹ thuật) | Checklist thủ công, demo có kịch bản | `testing/04-acceptance/` |
| Regression | Tập hợp case đã pass, chạy lại sau mỗi fix | Tái sử dụng case ở 01-04 đánh dấu `[REGRESSION]` | `testing/05-regression/` |

### 4.1. Kỹ thuật thiết kế test case bắt buộc
- **Equivalence Partitioning (EP):** phân vùng input hợp lệ/không hợp lệ cho mọi field.
- **Boundary Value Analysis (BVA):** biên trên/dưới cho số lượng (giới hạn session, %discount, độ dài password...), biên thời gian (hết hạn OTP, hết hạn gói cước, cooldown).
- **Decision Table:** cho logic nhiều điều kiện kết hợp (VD: gating course theo subscription/purchase, tính giá sau giảm giá + flash-deal).
- **State Transition:** cho luồng có trạng thái rõ ràng (OTP: chưa dùng→đã dùng→hết hạn; Transaction: PENDING→COMPLETED; Report: pending→resolved; Certificate: submitted→approved).
- **Negative Testing:** input sai định dạng, thiếu field bắt buộc, token hết hạn/giả mạo, vượt quyền (IDOR).

---

## 5. Tiêu chí Entry / Exit

### 5.1. Entry Criteria (điều kiện bắt đầu test 1 UC)
- Đặc tả UC tương ứng đã đọc và hiểu rõ.
- Môi trường test có DB (Docker Mongo hoặc Atlas dev cluster) sẵn sàng.
- Với UC-06: có PayOS sandbox credentials.
- Với UC-03: xác nhận AI service test endpoint khả dụng.
- Build backend compile thành công (`mvn -q compile -DskipTests`).

### 5.2. Exit Criteria (điều kiện coi 1 UC đã test xong)
- 100% test case P0/P1 đã thực thi (Pass hoặc có Defect ghi nhận).
- Không còn Defect mức **Blocker** chưa xử lý.
- Traceability matrix cập nhật đầy đủ cho UC đó.
- Báo cáo test case dạng bảng (Input→Expected→Actual→Pass/Fail) đã lưu vào thư mục tầng tương ứng.

---

## 6. Môi trường kiểm thử

| Thành phần | Trạng thái | Ghi chú |
|---|---|---|
| Backend local | Sẵn sàng (`mvn spring-boot:run`, port 8080 hoặc 5000 tùy config) | |
| MongoDB | **Sẵn sàng — xác nhận 2026-07-17** | Quyết định user: dùng CHUNG cluster Atlas `MainDatabase` (tránh phí tạo cluster mới) nhưng tách riêng **database** `mchub_test` (khác hẳn `mchub` production). Đã thêm `MONGODB_TEST_URI`/`MONGODB_TEST_DATABASE` vào `.env`, `src/test/resources/application-test.properties` đã trỏ đúng biến này. **Quy tắc bắt buộc:** mọi lệnh test/script phải xác nhận đang connect `mchub_test`, không phải `mchub`, trước khi ghi/xóa dữ liệu — tự kiểm tra bằng `spring.data.mongodb.database` hoặc log connection trước khi chạy bất kỳ thao tác ghi nào |
| PayOS | **Đã có — nhưng là tài khoản THẬT, không phải sandbox riêng** | Xác nhận 2026-07-17: `.env` và `.env.production` đều có đủ `PAYOS_CLIENT_ID`(36 ký tự)/`PAYOS_API_KEY`(36 ký tự)/`PAYOS_CHECKSUM_KEY`(64 ký tự), đúng format thật, không phải placeholder. **PayOS không có môi trường sandbox tách biệt** — mọi giao dịch qua API này đều là giao dịch thật trên merchant thật. Xem Quy tắc an toàn Payment Testing bên dưới trước khi Test Execution |
| AI Voice Service | **Sẵn sàng — đã xác nhận 2026-07-17** | Dùng HF Space đã deploy: `https://trung2605-voice-ai-tranning.hf.space` (root trả `{"message":"MC Hub AI Service is running","whisper_model":"small"}`, HTTP 200). **Lưu ý:** port `127.0.0.1:8001` local KHÔNG dùng cho test này — đó là AI service của dự án khác (đang chạy riêng trên máy), tuyệt đối không gọi nhầm. Cần set `ai.service.analyze-url`/`ai.service.tts-url` trỏ về HF Space khi test UC-03 |
| Email | **Cần xác nhận** | Cần hộp thư test riêng, không gửi nhầm user thật |

**Cập nhật blocker (2026-07-17):**
- ✅ AI Voice Service — đã xác nhận sẵn sàng, hết blocker. Đã dùng thật để Execute UC-03 (analyze-voice, adaptive calibration).
- ✅ MongoDB — dùng chung cluster Atlas `MainDatabase`, database riêng `mchub_test`, hết blocker. Đã dùng thật xuyên suốt UC-06/UC-09/UC-03.
- ✅ PayOS — có key thật, KHÔNG có sandbox. Đã áp dụng Quy tắc an toàn Payment Testing (mục 6.1) khi Execute UC-06 — chỉ tạo link/validate, không thanh toán thật, hết blocker.

### 6.1. Quy tắc an toàn Payment Testing (PayOS dùng key thật)

Vì PayOS không có sandbox, mọi test động tới API PayOS thật đều tạo giao dịch/dữ liệu thật trên merchant thật. Áp dụng nghiêm ngặt các quy tắc sau khi Test Execution UC-06:

1. **Ưu tiên tuyệt đối: test không cần gọi PayOS thật trước.**
   - Toàn bộ luồng KHÔNG bắt buộc gọi PayOS thật vẫn test bình thường: validate input, tính giá/discount/coupon (`applyDiscount`, flash-deal), authorization, luồng giảm giá 100% (bỏ qua PayOS hoàn toàn — xem `createPremiumOrder` đã audit trước đó).
   - `admin/simulate-success` và `admin/complete/{id}` (đã có sẵn trong code, dùng cho dev/test) — dùng 2 endpoint này để test **toàn bộ luồng kích hoạt gói/khóa học sau thanh toán** mà KHÔNG cần chạm PayOS thật.

2. **Chỉ gọi PayOS thật ở bước cuối cùng, có kiểm soát chặt:**
   - Chỉ tạo **1 link thanh toán thật số tiền tối thiểu** (PayOS yêu cầu tối thiểu tùy ngân hàng, thường 1,000-2,000 VNĐ) để xác nhận: request tạo link đúng format, response đúng cấu trúc, redirect URL hoạt động.
   - **KHÔNG tự thanh toán** trừ khi có xác nhận rõ ràng từ user muốn xác nhận full round-trip (bao gồm webhook thật).
   - Không tạo nhiều order test liên tiếp — mỗi order test cần huỷ (`cancelUrl`) ngay sau khi xác nhận xong, tránh rác dữ liệu transaction `PENDING` tồn đọng trong DB thật.

3. **Webhook testing — mô phỏng thay vì chờ PayOS thật gọi:**
   - Test `handlePaymentWebhook` bằng cách tự dựng request POST giả lập payload PayOS (đã biết cấu trúc từ code `PayOSService.verifyWebhookSignature`), tự tính chữ ký HMAC bằng đúng `checksumKey` thật để test cả trường hợp signature đúng/sai — không cần chờ PayOS gọi thật.
   - Việc này an toàn vì webhook endpoint ch�ỉ đọc `orderCode` khớp DB, không tự tạo giao dịch tiền thật nào.

4. **Không test trên `.env.production`.** Nếu `.env` (dev) và `.env.production` dùng CHUNG 1 bộ key PayOS thật (cần xác nhận thêm), thì về bản chất không có phân biệt — báo ngay cho user nếu phát hiện đây là rủi ro (dev/prod dùng chung merchant key, không tách được môi trường).

**Quyết định đã chốt (user, 2026-07-17):** Dừng ở mức test tạo link/validate — **KHÔNG thanh toán thật**. Cụ thể áp dụng cho điểm 2:
- Được phép: gọi `POST /payment/create-order`, `/payment/course-order` để xác nhận request build đúng, response chứa `checkoutUrl`/`qrCode` hợp lệ, transaction `PENDING` được ghi đúng vào DB.
- **Không được:** truy cập `checkoutUrl` trả về và hoàn tất thanh toán thật (quét QR/chuyển khoản thật).
- Do đó **luồng full round-trip thật (thanh toán → webhook PayOS gọi thật → COMPLETED)** sẽ **không được Test Execution thật** — chỉ verify được qua:
  - Mô phỏng webhook (điểm 3 — tự dựng request, tự ký HMAC đúng key thật) để test logic xử lý webhook.
  - `admin/simulate-success`, `admin/complete/{id}` để test logic kích hoạt gói/khóa học sau khi transaction COMPLETED.
- Test case nào yêu cầu thanh toán thật để hoàn tất sẽ đánh dấu **"Not Executed — Out of Scope (no real payment per user decision)"**, không phải Fail, không phải bỏ sót.
- Test case tạo order sau khi verify xong (không thanh toán) phải **huỷ/không để tồn transaction PENDING rác** — nếu code không có cơ chế tự dọn transaction PENDING quá hạn, ghi nhận đây là note kỹ thuật (không phải bug, nhưng ảnh hưởng vệ sinh dữ liệu test) gửi dev.

---

## 7. Vai trò & Trách nhiệm

Xem `testing/testing.md` mục 1-2 (Nhiệm vụ, Thẩm quyền & Ranh giới) — không lặp lại ở đây.

---

## 8. Rủi ro của chính kế hoạch test (Test Risk)

| Rủi ro | Ảnh hưởng | Giảm thiểu |
|---|---|---|
| Không có CI/CD, không chạy được embedded MongoDB | Không tự động hóa được, tốn thời gian trace thủ công | Đề xuất Docker MongoDB cho integration test thật; ghi rõ case nào chỉ trace logic (không phải test tự động chạy thật) |
| PayOS là dịch vụ thật, rủi ro test nhầm giao dịch thật | Mất tiền thật, ảnh hưởng uy tín | Bắt buộc xác nhận sandbox credentials trước khi động vào UC-06; test case ghi rõ "cần sandbox" và dừng lại chờ xác nhận nếu chưa có |
| Tài liệu `API-Information.md`/`schema.dbml` sai (thuộc module khác) có thể gây hiểu nhầm nếu người khác đọc nhầm | Test case sai nguồn, sai phạm vi | Đã ghi rõ ở mục 1.3, loại khỏi nguồn tham khảo chính thức |
| AI service ngoài có thể không ổn định/không có sẵn instance test | Không test được UC-03 đầy đủ | Xác nhận trước; nếu không có mock, ghi nhận là "Not Testable — External Dependency Unavailable" thay vì bỏ qua âm thầm |

---

## 9. Phê duyệt

- [x] User/Product Owner xác nhận phạm vi (mục 2)
- [x] User/Dev xác nhận môi trường test sẵn sàng (mục 6) — PayOS (key thật, không sandbox, đã áp dụng Quy tắc an toàn mục 6.1), MongoDB `mchub_test`, AI Voice Service (HF Space)
- [x] Bắt đầu Test Design cho P0 (UC-06 → UC-09 → UC-03) sau khi 2 mục trên được xác nhận

### 9.1. Tiến độ thực thi theo UC (cập nhật 2026-07-17)

| UC | Ưu tiên | Trạng thái | Kết quả |
|---|---|---|---|
| UC-06 Payment & Subscription | P0 | [x] Done | 47/52 PASS, 3 FAIL — xem `testing/03-system/UC-06-Payment-Subscription_TestCases.md` |
| UC-09 Admin Dashboard | P0 | [x] Done | 42/44 PASS, 1 FAIL — xem `testing/03-system/UC-09-Admin-Dashboard_TestCases.md` |
| UC-03 Voice Training (core) | P0 | [x] Done | 22/26 PASS, 4 FAIL — xem `testing/03-system/UC-03-Voice-Training_TestCases.md` |
| UC-04 Courses & Learning | P1 | [x] Done | 36/39 PASS, 3 FAIL — xem `testing/03-system/UC-04-Courses-Learning_TestCases.md` |
| UC-10 Marketing & Communication | P1 | [x] Done | 40/41 PASS, 1 FAIL — xem `testing/03-system/UC-10-Marketing-Communication_TestCases.md` |
| UC-01 Authentication | P1 | [x] Done | 24/27 PASS, 3 FAIL — xem `testing/03-system/UC-01-Authentication_TestCases.md` |
| UC-02 User/MC Profile | P2 | [x] Done | 12/18 PASS, 6 FAIL — xem `testing/03-system/UC-02-User-MC-Profile_TestCases.md` |
| UC-05 Community & Leaderboard | P2 | [x] Done | 24/26 PASS, 2 FAIL — xem `testing/03-system/UC-05-Community-Leaderboard_TestCases.md` |
| UC-07 Onboarding Quest | P2 | [x] Done | 13/13 PASS, 0 FAIL — xem `testing/03-system/UC-07-Onboarding-Quest_TestCases.md` |
| UC-08 Public/Support | P3 | [x] Done | 23/25 PASS, 2 FAIL — xem `testing/03-system/UC-08-Public-Support_TestCases.md` |

**24 defect đã file** (`DEFECT-001` → `DEFECT-024`, xem `testing/defect-log/`), chưa cái nào được dev fix — đúng thẩm quyền QA (không tự sửa code theo `testing/testing.md`). **DEFECT-016 mức Critical/P0** — lệch múi giờ khiến JWT hợp lệ bị từ chối sau đổi mật khẩu, chặn hoàn toàn luồng reset-password, đề xuất ưu tiên fix trước go-live. **DEFECT-001/010/013/022 cùng nhóm nguyên nhân** — thiếu SecurityConfig whitelist cho route Public, lặp lại ở 4 module khác nhau. **DEFECT-023 ảnh hưởng toàn hệ thống** — `GlobalExceptionHandler` thiếu handler cho `HttpMessageNotReadableException`, mọi endpoint nhận enum qua request body đều có nguy cơ trả 500 thay vì 400 khi client gửi sai giá trị.

### 9.2. Toàn bộ 10 UC đã hoàn thành System Test (2026-07-18)

Tất cả UC-01 → UC-10 đã được Execute qua system test thủ công (trừ UC-06/09/03 thực hiện ở phiên trước, còn lại UC-04/10/01/02/05/07/08 thực hiện tiếp trong các phiên sau). Tổng hợp:

| UC | Test case | PASS | FAIL |
|---|---|---|---|
| UC-06 Payment & Subscription | 52 | 47 | 3 (+2 Not Executed, out of scope thanh toán thật) |
| UC-09 Admin Dashboard | 44 | 42 | 1 (+1 Not Executed) |
| UC-03 Voice Training | 26 | 22 | 4 |
| UC-04 Courses & Learning | 39 | 36 | 3 |
| UC-10 Marketing & Communication | 41 | 40 | 1 |
| UC-01 Authentication | 27 | 24 | 3 |
| UC-02 User/MC Profile | 18 | 12 | 6 |
| UC-05 Community & Leaderboard | 26 | 24 | 2 |
| UC-07 Onboarding Quest | 13 | 13 | 0 |
| UC-08 Public/Support | 25 | 23 | 2 |
| **Tổng** | **311** | **283** | **25** |

**24 defect đã file, 0 đã fix.** Đáng chú ý nhất: DEFECT-016 (Critical/P0 — timezone bug chặn đăng nhập sau đổi mật khẩu), DEFECT-018 (Major — role check thiếu cho phép CLIENT truy cập endpoint MC-only), DEFECT-023 (Major — lỗ hổng exception handling ảnh hưởng toàn hệ thống). Nhóm lặp lại nhiều lần: thiếu SecurityConfig whitelist cho route Public (DEFECT-001/010/013/022 — 4 lần cùng 1 nguyên nhân qua 4 module khác nhau).

**Unit/Controller test tự động:** 490 test (20 service class + 25 controller class), 100% PASS — bổ trợ cho system test, không thay thế.
