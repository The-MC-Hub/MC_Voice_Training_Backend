# MC_Voice_Training_Backend — Java/Spring Boot → .NET 8 Migration Plan

**Status:** DRAFT — chưa thực hiện, chờ duyệt.
**Ngày lập:** 2026-07-27
**Phạm vi:** Toàn bộ `MC_Voice_Training_Backend` (175 endpoint, 39 service, 49 repository, 49 model, 58 DTO, 549 unit test).

**Số liệu sửa lại (2026-07-28)**: DTO đếm lại chính xác qua field-level audit (`MC_Voice_Training_Backend_NET/migration/FIELD_AUDIT.md`) là **58**, không phải 63 như ghi ban đầu — models (49) đúng.

## 0. Quyết định đã chốt với user

| Hạng mục | Quyết định |
|---|---|
| Backend migrate | Chỉ `MC_Voice_Training_Backend` (không đụng `The-MC-Hub-Java-Backend`) |
| .NET version | .NET 8 LTS + ASP.NET Core Web API (chốt 2026-07-28, không dùng 9 — ưu tiên ổn định dài hạn) |
| Database | Giữ nguyên MongoDB — dùng `MongoDB.Driver`, KHÔNG chuyển sang SQL, KHÔNG migrate data |
| Kiến trúc | Clean Architecture + CQRS/MediatR (không giữ 4-layer đơn giản kiểu Spring) |
| Chiến lược cutover | Song song — build .NET dần theo module, verify từng module xong mới cắt sang, không "big bang" |
| API contract | Giữ nguyên 100% — cùng path `/api/v1/...`, cùng response envelope `{status,message,data}`, cùng field name — frontend không sửa dòng nào |
| WebSocket `/ws-chat` | Đã xác nhận dead code ở Java (không có `@MessageMapping` handler nào). → .NET: SignalR Hub rỗng, chỉ giữ endpoint sống, không port logic không tồn tại |
| Unit test | Port 1:1 toàn bộ 549 test Mockito → xUnit, không cắt giảm coverage |
| Ưu tiên | Đúng đắn luồng nghiệp vụ > tốc độ hoàn thành |
| AI service (`TrainingAiSample`) | Ngoài scope — giữ Python vĩnh viễn. .NET chỉ port proxy gọi ra 2 endpoint (`/analyze-voice`, `/tts/stream`) |
| `DatabaseMigrationService`/MBBank VietQR | Cả 2 KHÔNG port — tool one-off đã dùng xong / đã thay bằng PayOS (vestigial) |
| SSE stream (`LogController`) | Port bằng `IAsyncEnumerable`/`Response.WriteAsync` native, giữ `text/event-stream`, không đổi sang SignalR |

## 1. Khảo sát hiện trạng (đã xác nhận, số liệu thật — không phải ước lượng)

**⚠️ CLAUDE.md hiện tại của repo đã lỗi thời (ghi ngày 2026-07-20), sai lệch với code thật ở nhiều điểm quan trọng** — plan này dùng số liệu khảo sát trực tiếp code, không dùng số liệu trong CLAUDE.md cũ:

| | CLAUDE.md ghi (SAI/CŨ) | Thực tế đo được |
|---|---|---|
| Booking/Chat/Notification/Favorite domain | "không tồn tại" | Tồn tại đầy đủ, có controller/service/repo/model riêng |
| Tổng endpoint | ~130 | **175** |
| Service không qua layer | VoucherController/QuestController/UserHighlightController "không có service" | Cả 3 đã có service đúng chuẩn — claim cũ sai |
| Test suite | 46 file / 493 test | **56 file / 549 `@Test` method** |
| WebSocket | "không dùng" | Đúng — xác nhận lại, dead code thật |
| CORS | luôn append wildcard `*.vercel.app` | Sai — đọc từ env `VERCEL_PREVIEW_PATTERNS`, rỗng mặc định |
| Rate limit cache | `ConcurrentHashMap` không bao giờ evict | Sai — Caffeine cache, TTL 2h, max 10000 entries |

→ **Việc đầu tiên trong Phase 0 là viết lại CLAUDE.md cho đúng thực tế**, để làm nền tảng đối chiếu xuyên suốt quá trình migrate (nếu không, mọi so sánh "Java làm gì" sẽ dựa trên tài liệu sai).

### 1.1 Controllers (44 class, 175 endpoint)
Đã liệt kê đầy đủ 38 controller thường + 6 admin controller, kèm path + method + auth annotation. Xem chi tiết trong khảo sát gốc — sẽ được chẻ nhỏ theo module ở Phase 2.

### 1.2 Services (39 interface + 33 impl, 5 service dạng concrete class không interface)
Xác nhận: claim "VoucherController/QuestController/UserHighlightController không có service" trong CLAUDE.md cũ **đã sai** — code hiện tại cả 3 đều đi qua service đúng chuẩn.

Vẫn còn bypass thật (kế thừa nguyên trạng khi migrate, không "sửa hộ" kiến trúc Java gốc trừ khi được yêu cầu riêng):
- `CertificateController` → gọi thẳng `UserRepository` cạnh `CertificateService`
- `VoiceController` → gọi thẳng `GuestVoiceUsageRepository`, `SystemSettingRepository`
- `AdminController` → gọi thẳng `SystemSettingRepository`
- `PaymentController` → gọi thẳng 4 repository, business logic (plan upgrade, course grant) nằm luôn trong controller

→ Khi port sang CQRS, đây là cơ hội tự nhiên để đưa các bypass này vào đúng Command/Query Handler — **nhưng phải giữ nguyên hành vi 100%**, không tối ưu/sửa logic song song với việc port ngôn ngữ (rủi ro lẫn lộn bug mới với bug dịch sai).

### 1.3 Repositories (49 total)
- 44 repository: derived-method-name (Spring Data) → port thẳng sang `IMongoCollection<T>.Find(...)` hoặc repository wrapper mỏng.
- **2 dùng `@Aggregation`** (cần viết tay `PipelineDefinition`/`BsonDocument` ở .NET, không có annotation tương đương):
  - `BookingRepository.sumPriceByMcAndStatusPaid`
  - `PaymentTransactionRepository.sumAmountByStatus`
- **1 aggregation ẩn trong Service, không phải Repository method**: `AdaptiveCalibrationService` dùng `MongoTemplate.aggregate()` trực tiếp (tính avg/min/max/percentile theo lesson) — phải đọc kỹ source, không chỉ trace qua Repository interface.
- **1 dùng Elasticsearch `@Query`**: `VoiceLessonSearchRepository.searchByText` (multi_match) — port riêng bằng `Elastic.Clients.Elasticsearch`, tách khỏi luồng Mongo.

### 1.4 Models (49 class = 48 MongoDB `@Document` + 1 Elasticsearch `VoiceLessonSearchDocument`)
Danh sách đầy đủ 48 collection đã liệt kê trong khảo sát. **Chưa audit field-level từng model** — sẽ làm ở Phase 1 (field-by-field) trước khi bắt tay viết Domain Entity ở .NET, bắt buộc để đảm bảo JSON contract giữ nguyên.

### 1.5 DTOs (63) + Mapper (18 MapStruct)
`ApiResponse<T>` là envelope quan trọng nhất — phải replicate chính xác `{status, message, data}`. MapStruct (compile-time) → .NET dùng **Mapperly** (source generator, gần nhất về triết lý compile-time mapping, tránh reflection runtime của AutoMapper).

### 1.6 Enums (23)
Port thẳng sang C# `enum`. Cần cấu hình `JsonStringEnumConverter` (System.Text.Json) để giữ serialize-as-string giống Jackson mặc định của Spring.

### 1.7 Cấu hình đặc biệt (xem chi tiết mục 6 bên dưới trong khảo sát gốc)
JWT (jjwt HMAC-SHA256, claim `id`/`role`, token phụ pending-Google 10 phút), SecurityConfig (CORS/CSP/HSTS/permitAll list), Bucket4j rate limit (Caffeine TTL 2h, per-IP + per-email OTP throttle riêng), WebSocket (dead, chỉ stub), 4 Scheduled job (cron), Async (14 chỗ dùng `@Async`), Elasticsearch, Brevo SMTP, PayOS (HMAC-SHA256 signature, constant-time compare), Cloudinary, Supabase Storage.

### 1.8 Tests (56 file, 549 `@Test`)
Toàn bộ Mockito unit test — port 1:1 sang xUnit + Moq/NSubstitute.

### 1.9 AI Service Integration
2 endpoint gọi ra ngoài: `/analyze-voice`, `/tts/stream` (đã xác nhận qua production `/openapi.json`, không phải `/generate-mc-voice` như default cũ — xem DEFECT-009, đã fix 2026-07-18). Cả hai return `Map`/JSON thô, chưa có DTO chặt — sẽ cần đọc kỹ request/response shape ở Phase 1 trước khi viết Command Handler tương ứng.

---

## 2. Kiến trúc .NET đề xuất (Clean Architecture + CQRS/MediatR)

```
MCHub.VoiceTraining.sln
├── src/
│   ├── MCHub.VoiceTraining.Domain/              # Entities (map 1-1 với 49 @Document), Enums (23), Domain exceptions
│   ├── MCHub.VoiceTraining.Application/         # CQRS: Commands, Queries, Handlers (MediatR), DTOs, Validators (FluentValidation), Mapper profiles (Mapperly)
│   │   ├── Features/
│   │   │   ├── Auth/
│   │   │   ├── Booking/
│   │   │   ├── Chat/
│   │   │   ├── Voice/
│   │   │   ├── Course/
│   │   │   ├── Payment/
│   │   │   ├── Admin/
│   │   │   ├── Community/
│   │   │   ├── ... (theo từng module, xem Phase 2)
│   ├── MCHub.VoiceTraining.Infrastructure/      # MongoDB.Driver repositories, Elasticsearch client, PayOS/Cloudinary/Supabase/Brevo integration, JWT issuer, Bucket4j-equivalent rate limiter, Scheduled jobs (IHostedService)
│   └── MCHub.VoiceTraining.Api/                 # Controllers (giữ nguyên route để tương thích frontend), Middleware (auth, CORS, rate-limit, exception handler → envelope {status,message,data}), Program.cs, appsettings.json
│   │       ├── Hubs/ScriptEditHub.cs            # SignalR Hub THẬT — port /script.edit/{bookingId}, có logic realtime
│   │       └── Hubs/LegacyWsChatHub.cs          # SignalR Hub RỖNG — chỉ giữ endpoint sống cho /ws-chat dead code, KHÔNG chứa logic (2 Hub tách biệt hoàn toàn, chốt 2026-07-28 — tránh nhầm code thật với stub)
├── tests/
│   ├── MCHub.VoiceTraining.Application.Tests/   # xUnit — port toàn bộ Service test (Mockito → Moq)
│   ├── MCHub.VoiceTraining.Api.Tests/           # xUnit — port toàn bộ Controller test (WebApplicationFactory thay cho @WebMvcTest)
│   └── MCHub.VoiceTraining.IntegrationTests/    # Test tích hợp thật với MongoDB (Testcontainers.MongoDb) — MỚI, Java gốc không có (chỉ Mockito), nhưng cần để verify đúng luồng nghiệp vụ khi cutover từng module
```

**Vì sao chọn CQRS/MediatR thay vì giữ y hệt Controller→Service→Repository:**
- Java Service hiện tại đã trộn lẫn nhiều trách nhiệm (ví dụ `PaymentController` có business logic ngay trong controller) — CQRS ép tách rõ Command (ghi) / Query (đọc), tự nhiên dọn sạch anti-pattern đó *trong lúc dịch*, không cần refactor thêm 1 lần nữa sau này.
- Mỗi endpoint Java (175 cái) → map thẳng 1 Command hoặc 1 Query — dễ tracking tiến độ theo từng cặp endpoint/handler, khớp với yêu cầu "tracking công việc" của user.
- MediatR Pipeline Behavior thay thế `@PreAuthorize`/validation/logging cross-cutting concern của Spring AOP — không mất tính năng, chỉ đổi cơ chế.

**Rủi ro của lựa chọn này (nói rõ để user biết đánh đổi):**
- Setup ban đầu (Phase 0) tốn công hơn giữ 4-layer đơn giản — nhưng user đã xác nhận không quan trọng thời gian.
- Rủi ro lỗi logic khi vừa dịch vừa tái kiến trúc **cao hơn** dịch thẳng 1-1 — bù lại bằng nguyên tắc bắt buộc ở mục 4 (Definition of Done mỗi module).

---

## 3. Danh sách package .NET dự kiến

| Java | .NET tương đương |
|---|---|
| Spring Data MongoDB | `MongoDB.Driver` (chính thức, không ORM trung gian) |
| Spring Data Elasticsearch | `Elastic.Clients.Elasticsearch` |
| Spring Security + JWT (jjwt) | `Microsoft.AspNetCore.Authentication.JwtBearer` + `System.IdentityModel.Tokens.Jwt` |
| Bucket4j | `AspNetCoreRateLimit` (hoặc hand-roll `IMemoryCache` token bucket nếu cần đúng behavior per-email throttle) |
| MapStruct | `Mapperly` (source generator, compile-time — gần triết lý MapStruct nhất) |
| Lombok | Không cần — C# có property/record built-in |
| spring-boot-starter-validation | `FluentValidation` |
| spring-boot-starter-mail (Brevo SMTP) | `MailKit` + `MimeKit` |
| Cloudinary Java SDK | `CloudinaryDotNet` |
| Supabase Storage (raw REST) | `HttpClient` thuần (không có SDK .NET chính thức ổn định) |
| google-api-client (Google Sign-In verify) | `Google.Apis.Auth` |
| springdoc-openapi (Swagger) | `Swashbuckle.AspNetCore` |
| `@Scheduled` | `Cronos` (parse cron) + `IHostedService`/`BackgroundService`, hoặc Quartz.NET nếu cần persistence job state |
| `@Async` | native `async`/`await`, `Task.Run` cho fire-and-forget |
| WebSocket/STOMP (dead) | SignalR Hub rỗng (stub) |
| Mockito | Moq hoặc NSubstitute |
| JUnit 5 | xUnit |
| `de.flapdoodle.embed.mongo` (vestigial) | `Testcontainers.MongoDb` (dùng thật cho integration test mới, không vestigial) |

---

## 4. Definition of Done — mỗi module (bắt buộc, không thương lượng)

Một module chỉ được coi là "xong" và cho phép cutover khi **TẤT CẢ** điều sau đúng — đây là cơ chế chính giảm rủi ro sai lệch nghiệp vụ:

1. **Field-level DTO parity**: mọi DTO trong module đã đối chiếu field-by-field với JSON response thật của Java (chạy Java backend thật, gọi endpoint, lưu response mẫu — so sánh với response .NET byte-để-byte về field name/type/null-handling).
2. **Side-by-side response test**: viết thành xUnit test thật trong `MCHub.VoiceTraining.IntegrationTests` (mục 2), gọi cả 2 backend (Java đang chạy + .NET đang chạy) cùng input, so sánh response — chạy tự động mỗi lần CI (không phải bước thủ công), kết quả nằm trong CI log/report. Đây là bước **KHÔNG có trong Java gốc**, thêm mới để đảm bảo đúng nghiệp vụ khi cutover.
3. **Unit test port 1:1**: toàn bộ `@Test` method của module đó (service + controller) có `[Fact]`/`[Theory]` tương ứng ở .NET, pass 100%.
4. **Auth/permission parity**: mọi `@PreAuthorize`/route công khai (permitAll) map đúng `[Authorize]`/`[AllowAnonymous]` — test riêng case 401/403 cho từng route nhạy cảm.
5. **Business rule verify thủ công**: với luồng phức tạp (payment webhook signature, rate limit throttle, JWT claim, gamification XP calculation), test thủ công qua Postman/curl xác nhận hành vi giống hệt — không chỉ tin unit test.
6. Frontend **không cần đổi 1 dòng code** khi trỏ `VITE_API_URL` sang backend .NET của module đó (test bằng cách chạy frontend thật, trỏ tạm sang .NET, không lỗi runtime nào liên quan format response).

---

## 5. Roadmap theo Phase (chi tiết task tracking ở file `TRACKING.md` riêng)

### Phase 0 — Nền tảng (không có business logic, làm 1 lần, chặn mọi phase sau)
- Cập nhật lại `CLAUDE.md` cho đúng thực tế (dọn sai lệch đã phát hiện).
- Field-level audit toàn bộ 49 Model + 63 DTO (hiện chưa có, bắt buộc trước khi viết Domain Entity).
- Đọc kỹ `AsyncConfig.java`, `CloudinaryConfig.java`, request/response shape 2 AI endpoint, `DataSeeder.java`, `OpenApiConfig.java` — các gap đã note ở khảo sát.
- Khởi tạo solution .NET (cấu trúc mục 2), CI build rỗng chạy được.
- Middleware response envelope `{status,message,data}` + global exception handler — dùng chung cho mọi module sau này, làm sai ở đây thì sai toàn bộ.
- JWT issuer/validator (claim `id`/`role`, đúng thời hạn) — nền tảng cho auth mọi module.
- Setup MongoDB.Driver connection, xác nhận connect được cùng 1 MongoDB Atlas cluster mà Java đang dùng (không phải database riêng — bắt buộc để so sánh response thật ở bước Definition of Done).

### Phase 1 — Module theo thứ tự phụ thuộc (module sau phụ thuộc module trước đã xong)
Thứ tự đề xuất dựa trên mức độ làm nền cho module khác + rủi ro nghiệp vụ:

1. **Auth** (AuthController, JwtService, GoogleTokenVerifierService) — mọi module khác cần JWT hoạt động đúng trước.
2. **User/MCProfile/ClientProfile** (UserController, MCController, ClientProfileController, MCProfileService) — cần cho mọi domain có "chủ sở hữu" dữ liệu.
3. **Public** (PublicController — landing, enums, MC directory) — không auth, rủi ro thấp, verify pattern response envelope sớm.
4. **Booking** (BookingController, BookingDetailController, AvailabilityController, ScheduleResponseDTO liên quan) — lõi nghiệp vụ chính.
5. **Payment** (PaymentController, BookingPaymentController, PayOSService) — rủi ro cao nhất (chữ ký HMAC, webhook, tiền thật) — làm sau khi Booking xong vì phụ thuộc.
6. **Chat** (ChatController, ScriptCollaborativeController, SignalR stub) — có auth-guard đã fix ở session trước, phải giữ đúng.
7. **Voice Training** (VoiceController, AdaptiveCalibrationService, AI proxy) — có aggregation phức tạp nhất, làm riêng biệt kỹ.
8. **Course/Gamification** (CourseController, QuestController, VoucherController, CommunityController, MinigameController) — phụ thuộc User+Voice xong trước.
9. **Notification/Favorite/Review/Report/Certificate** — domain phụ trợ, ít phụ thuộc chéo.
10. **Admin** (7 admin controller) — làm cuối vì phụ thuộc hầu hết domain khác đã có Command/Query sẵn để tái sử dụng.
11. **Marketing** (EmailCampaignController, AnnouncementController, SocialPostController) — độc lập, có thể xen kẽ song song nếu có nhiều người làm.

### Phase 2 — Cutover (cơ chế: frontend env branching theo module)

**Đã chốt với user (2026-07-28): Java vẫn là backend chính trong suốt giai đoạn này. .NET port dần, verify từng module, nhưng KHÔNG cutover thật cho tới khi user tự quyết định đủ tin tưởng.**

- Cơ chế: frontend gọi backend theo module qua biến env riêng (vd `VITE_AUTH_API_URL`, `VITE_VOICE_API_URL`...) thay vì 1 `VITE_API_URL` chung — cho phép trỏ từng module sang .NET độc lập mà không cần proxy/DNS riêng.
- Mặc định mọi biến trỏ về Java. Chỉ đổi 1 biến sang .NET khi module đó đạt Definition of Done (mục 4) **và** user xác nhận muốn thử.
- Không có mốc thời gian ép buộc "cutover xong module X vào ngày Y" — do user tự quyết định theo từng module.
- Rollback: vì Java luôn là default, rollback = đổi biến env về Java, tức thì, không cần thao tác gì ở backend.
- Payment module: không có bước sign-off riêng ngoài Definition of Done chuẩn (mục 4) — user xác nhận bỏ qua gate bổ sung.

### Phase 3 — Retire Java backend
Ngoài phạm vi plan này ở thời điểm hiện tại — user chưa quyết định mốc retire, Java tiếp tục là backend chính đến khi user chủ động yêu cầu đánh giá lại.

---

## 6. Rủi ro đã xác định + cách giảm thiểu

| Rủi ro | Cách giảm thiểu trong plan |
|---|---|
| CLAUDE.md sai lệch dẫn tới hiểu nhầm scope | Đã tự khảo sát code thật, không dùng số liệu cũ; Phase 0 cập nhật lại tài liệu |
| Vừa dịch vừa tái kiến trúc (CQRS) dễ lẫn bug mới | Definition of Done bắt buộc side-by-side response test — so 2 backend thật, không chỉ tin unit test port |
| PayOS webhook signature sai 1 ký tự = mất tiền thật | Payment module làm riêng, test thủ công bắt buộc (mục 4.5), dùng `CryptographicOperations.FixedTimeEquals` đúng như Java `MessageDigest.isEqual` |
| Aggregation MongoDB cú pháp khác hẳn Spring Data | Đã khoanh vùng chính xác 2 Repository + 1 Service (AdaptiveCalibrationService) dùng aggregation — audit riêng ở Phase 0 |
| Elasticsearch tách biệt khỏi Mongo, dễ quên | Voice module (Phase 1.7) note riêng, có fallback Mongo khi ES lỗi — phải giữ đúng fallback behavior |
| Rate limit per-email throttle (lớp thứ 2, dễ bỏ sót) | Đã note rõ trong khảo sát mục 6, đưa vào Auth module Definition of Done |
| 549 test port 1:1 tốn thời gian rất lớn | User đã xác nhận chấp nhận đánh đổi, ưu tiên đúng nghiệp vụ |
| WebSocket dead code — dễ tưởng nhầm cần port đầy đủ | Đã xác nhận với user: chỉ stub, không port logic không tồn tại |

---

## 7. Quyết định bổ sung (chốt với user 2026-07-28)

1. **CI/CD .NET**: chưa có pipeline sẵn — dựng mới bằng GitHub Actions (build + test tự động mỗi push), làm ở Phase 0 cùng lúc khởi tạo solution.
2. **Hosting .NET backend**: Render — cùng nền tảng với Java backend hiện tại, thuận tiện so sánh khi cần deploy thử.
3. **MongoDB Atlas connection**: .NET dùng chung connection string với Java (cùng đọc/ghi database thật `mchub`) trong giai đoạn port/verify — không tạo staging DB riêng. Rủi ro thấp vì Java vẫn là backend chính (xem Phase 2), .NET chỉ đọc/test song song, chưa nhận traffic ghi thật từ user cho tới khi cutover.
4. **Field-level audit độ sâu**: audit kỹ toàn bộ 49 Model + 63 DTO, không rút gọn cho domain phụ — đúng nguyên tắc đã chốt từ đầu ("không quan trọng làm bao lâu, quan trọng chắc luồng nghiệp vụ").
5. **Elasticsearch connection**: .NET kết nối cùng instance ES mà Java đang dùng (không dựng ES riêng) — nhất quán với quyết định MongoDB dùng chung connection string, vì .NET chỉ đọc/test song song trong giai đoạn này.
6. **SignalR Hub cho Chat**: tách 2 Hub riêng biệt — `ScriptEditHub` (port `ScriptCollaborativeController`'s `@MessageMapping("/script.edit/{bookingId}")`, có logic thật) và `LegacyWsChatHub` (rỗng, chỉ giữ endpoint sống cho `/ws-chat` dead code) — xem cập nhật cấu trúc thư mục ở mục 2.
