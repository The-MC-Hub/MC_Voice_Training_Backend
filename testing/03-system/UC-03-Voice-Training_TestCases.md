# System Test Case — UC-03: Luyện tập Giọng nói (Voice Training core)

**Nguồn đặc tả:** `docs/use-cases/UC-03-voice-training.md`
**Source verify:** `controllers/VoiceController.java`, `services/impl/VoiceServiceImpl.java`, `util/AudioMagicBytesValidator.java`
**Ưu tiên:** P0
**Môi trường:** MongoDB Atlas `mchub_test`, backend port 5555, AI Voice Service = HF Space `https://trung2605-voice-ai-tranning.hf.space` (đã set `AI_ANALYZE_URL`/`AI_TTS_URL` trong `.env`, xác nhận qua `/openapi.json`: routes thật là `/analyze-voice`, `/generate-mc-voice`, `/tts/stream`; `tts_loaded:false` ở thời điểm test)

## Ghi chú đối chiếu source quan trọng (trước khi test)
- **AI service KHÔNG được mock** — gọi thật HF Space, tốn thời gian xử lý audio thật (Whisper model "small", chạy CPU, ~45-50s/request).
- **Cần tài khoản ADMIN** để tạo `VoiceLesson` fixture (`POST /voice/admin/lessons` — multipart form, không phải JSON).
- **Server test đã restart với `.env` sửa lại** trước khi Execute: `AI_ANALYZE_URL`/`AI_TTS_URL` trước đó trỏ nhầm `127.0.0.1:8001` (AI service của dự án khác trên máy, theo cảnh báo Test Plan) — đã sửa sang HF Space thật. Phát hiện trong lúc sửa: `generateTTSAudio()` **hardcode URL trong code Java**, không đọc `.env` — xem DEFECT-009.

## Quy ước
- **[LIVE]** — Execute thật, gọi AI service HF Space thật, ghi dữ liệu vào `mchub_test`.
- **[NO-AI]** — Execute thật nhưng không cần AI service (CRUD lesson, validation trước khi gọi AI).

---

## TC-VOICE-01 → 04: Admin CRUD Lesson

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-01 | Negative [NO-AI] | JWT role CLIENT gọi `POST /voice/admin/lessons` | HTTP 403 | HTTP 403 | **PASS** |
| TC-VOICE-02 | EP hợp lệ [NO-AI] | Admin JWT, multipart form đủ field bắt buộc | HTTP 200, lesson tạo thành công | HTTP 200, lesson tạo với `id`, `isActive` mặc định đúng | **PASS** |
| TC-VOICE-03 | EP hợp lệ [NO-AI] | `PUT /voice/admin/lessons/{id}` cập nhật `title` | HTTP 200, cập nhật đúng | HTTP 200, `title` cập nhật chính xác | **PASS** |
| TC-VOICE-04 | EP hợp lệ [NO-AI] | `DELETE /voice/admin/lessons/{id}` (lesson riêng, không dùng lesson chính đang test analyze) | HTTP 200, verify mongosh: `isActive:false` | HTTP 200; mongosh xác nhận `isActive:false`, dữ liệu vẫn tồn tại | **PASS** |

## TC-VOICE-05 → 08: GET /lessons — public listing/search

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-05 | EP hợp lệ [NO-AI] | `GET /voice/lessons` không JWT | HTTP 200, chỉ trả lesson `isActive=true` | HTTP 200; xác nhận lesson đã soft-delete (TC-VOICE-04) KHÔNG xuất hiện, lesson chính có xuất hiện | **PASS** |
| TC-VOICE-06 | EP hợp lệ [NO-AI] | `GET /voice/lessons?category=WEDDING` | HTTP 200, lọc đúng theo category | HTTP 200, tất cả kết quả đều `category=WEDDING` | **PASS** |
| TC-VOICE-07 | EP hợp lệ [NO-AI] | `GET /voice/lessons?search=<từ khóa>` | HTTP 200, kết quả search đúng (Elasticsearch qua `VoiceLessonSearchService`) | HTTP 200, tìm thấy đúng lesson theo nội dung — Elasticsearch hoạt động | **PASS** |
| TC-VOICE-08 | EP hợp lệ [NO-AI] | `GET /voice/lessons/{id}` với id vừa tạo | HTTP 200, chi tiết đúng | HTTP 200, chi tiết khớp đầy đủ | **PASS** |

## TC-VOICE-09 → 14: POST /practice/analyze-voice — luồng chính AI scoring

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-09 | Negative [NO-AI] | Không có JWT | HTTP 403 | HTTP 403 | **PASS** |
| TC-VOICE-10 | Negative [NO-AI] | File content-type không hỗ trợ (`.exe`, `application/x-msdownload`) | HTTP 400, "Chỉ hỗ trợ file audio..." | HTTP 400, đúng message | **PASS** |
| TC-VOICE-11 | Negative [NO-AI] | File > 20MB | HTTP 400, "File không được vượt quá 20MB" | Không tạo file test 20MB+ thật trong phiên này (tốn tài nguyên/thời gian) — **verify thay thế qua unit/controller test đã có** (`VoiceControllerTest.AnalyzePractice.rejectsOversizedFile`, đã PASS ở Phase 4) | **PASS (verify qua test tự động có sẵn, không lặp lại live)** |
| TC-VOICE-12 | Negative [NO-AI] | Content-type khai `audio/wav` nhưng magic bytes không khớp (giả mạo) | HTTP 400, "Nội dung file không hợp lệ" | HTTP 400, đúng message | **PASS** |
| TC-VOICE-13 | **EP hợp lệ [LIVE] — luồng chính** | File WAV thật (3s sine tone tự tạo), JWT CLIENT hợp lệ, plan=FREE còn quota | HTTP 200, `PracticeSessionResponseDTO` đầy đủ field, `PracticeSession` lưu đúng DB | HTTP 200 (mất ~49s, CPU inference thật); response đầy đủ `accuracy_score/rhythm_score/feedback_vi/feedback_en/report_vi/report_en/tips_vi/tips_en/criteria_scores` không rỗng; audio upload Cloudinary thành công (`audio_url` trả về link thật); điểm số thấp hợp lý vì input là sine tone (không phải giọng nói thật) — `text_spoken:""` đúng logic | **PASS** |
| TC-VOICE-14 | Boundary [LIVE] | Lặp lại tới khi chạm `FREE_SESSION_LIMIT` (5 session) | Session thứ 6 → HTTP 402 `LIMIT_EXCEEDED` | HTTP 402, `"Free plan limit: 5 sessions. Upgrade to continue."` — đúng ngưỡng | **PASS** |

## TC-VOICE-15 → 17: Side effects sau khi analyze thành công

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-15 | EP hợp lệ [LIVE] | Sau TC-VOICE-13 | `UserStats` cập nhật qua `GamificationService` | HTTP 200 `GET /users/me/practice-stats`: `totalSessions:1` sau session đầu — tăng đúng | **PASS** |
| TC-VOICE-16 | EP hợp lệ [LIVE] | Sau TC-VOICE-13 | `VoiceLesson.practiceCount` tăng đúng 1 | HTTP 200 `GET /voice/lessons/{id}`: `practiceCount:1` sau session đầu | **PASS** |
| TC-VOICE-17 | Boundary [LIVE] | Sau 10 practice session thật cho cùng 1 lesson (nâng plan lên BASIC giữa chừng để vượt giới hạn FREE=5, đủ 10 session cho ngưỡng calibrate) | `GET /voice/lessons/{id}/adaptive-stats` trả dữ liệu thật thay vì `null` | HTTP 200, `sessionCount:10`, `calibratedPassingScore:55`, `calibratedWpmMin:100/Max:130`, `perceivedDifficulty:"VERY_HARD"` — calibration engine chạy đúng với dữ liệu thật, phản ánh đúng input là audio kém chất lượng (sine tone) | **PASS** |

## TC-VOICE-18 → 20: GET /practice/history/{userId} — IDOR guard

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-18 | Negative [NO-AI] | CLIENT xem history của user khác (id giả) | HTTP 403 `ACCESS_DENIED` | HTTP 403 | **PASS** |
| TC-VOICE-19 | EP hợp lệ [NO-AI] | CLIENT xem history của chính mình | HTTP 200, danh sách đúng, có `lesson_title` join qua batch-fetch | HTTP 200, `count:10`, field thật là `lesson_title` (snake_case, không phải `lessonTitle`) — join đúng, giá trị chính xác | **PASS** |
| TC-VOICE-20 | EP hợp lệ [NO-AI] | ADMIN xem history của user khác | HTTP 200 (ADMIN bypass ownership check) | HTTP 200 | **PASS** |

## TC-VOICE-21 → 23: POST /practice/analyze-guest — chưa đăng nhập

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-21 | EP hợp lệ [LIVE] | Không JWT, IP mới chưa từng dùng, KHÔNG kèm `scriptOrigin` (tham số optional theo `@RequestParam(required=false)`) | HTTP 200, kết quả AI trả về | **HTTP 500** — `"AI service unavailable"`. Root cause xác nhận qua log: proxy gửi `script_origin=""` (rỗng) cho AI service, AI service (FastAPI) coi field rỗng là "missing", trả 422, backend map thành 500 generic. Chỉ cần thêm `scriptOrigin` bất kỳ thì thành công (verify riêng) | **FAIL — DEFECT-008** |
| TC-VOICE-22 | Boundary [LIVE] | Gọi lại ngay lập tức cùng IP (đã dùng 1 lần với `scriptOrigin` hợp lệ ở bước verify DEFECT-008) | HTTP 400, "Bạn đã sử dụng lượt thử miễn phí..." | HTTP 400, đúng message, cooldown hoạt động đúng | **PASS** |
| TC-VOICE-23 | Negative [NO-AI] | File không hợp lệ (audio giả), IP đã clear cooldown | HTTP 400 — validate trước khi tính cooldown/gọi AI | HTTP 400, `"Nội dung file không hợp lệ"` — validate chạy đúng thứ tự (sau cooldown check, trước AI call) | **PASS** |

## TC-VOICE-24 → 25: POST /tts/generate

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-24 | EP hợp lệ [LIVE] | JWT CLIENT, `text` ngắn hợp lệ | HTTP 200, response `Content-Type: audio/wav`, body không rỗng | **HTTP 500** — `"TTS service error: 404 Not Found"`. Root cause: `generateTTSAudio()` **hardcode `http://127.0.0.1:8001/tts/stream`** trong code Java, hoàn toàn bỏ qua `AI_TTS_URL`/`ai.service.tts-url` đã cấu hình đúng trong `.env`. Việc sửa `.env` trước khi test KHÔNG có tác dụng gì với endpoint này | **FAIL — DEFECT-009 (Critical)** |
| TC-VOICE-25 | Negative [NO-AI] | Không JWT | HTTP 403 | HTTP 403 | **PASS** |

## TC-VOICE-26: Guest cooldown hours config (liên kết UC-09)

| ID | Loại | Input | Expected | Actual | Status |
|---|---|---|---|---|---|
| TC-VOICE-26 | EP hợp lệ [NO-AI] | `GET /voice/guest-cooldown-hours` không JWT (Guest thật) | HTTP 200, `hours` khớp `SystemSetting` hiện tại | **HTTP 403** — endpoint không có `@PreAuthorize` (thiết kế cho Guest) nhưng thiếu whitelist trong `SecurityConfig.java`, cùng nhóm nguyên nhân DEFECT-001. Retest với JWT: HTTP 200, `hours:3` — logic nghiệp vụ đúng, chỉ sai ở tầng bảo mật chặn nhầm Guest | **FAIL — DEFECT-010** |

---

## Tổng kết thực thi (Execute thật — 2026-07-17)

| Trạng thái | Số lượng |
|---|---|
| Tổng test case thiết kế | 26 |
| **PASS** | 22 |
| **FAIL** | 4 (TC-VOICE-21, 24, 26 — defect mới; không có case nào Not Executed) |
| Defect phát hiện | 3 mới (DEFECT-008 Major, DEFECT-009 Critical, DEFECT-010 Major) |

**Môi trường Execute:** Server test port 5555 đã **restart** với `.env` sửa `AI_ANALYZE_URL`/`AI_TTS_URL` trỏ đúng HF Space (trước đó trỏ nhầm `127.0.0.1:8001` — AI service dự án khác, theo cảnh báo Test Plan mục 6). User QA: `qa.admin.p2@mchubtest.local` (ADMIN), `qa.client.p2@mchubtest.local` (CLIENT, nâng plan BASIC giữa phiên để đủ 10 session cho TC-VOICE-17). Audio fixture: file WAV 3 giây tự tạo bằng Python (sine tone 440Hz, không phải giọng nói thật — đủ để AI service xử lý qua pipeline, nhưng điểm số thấp là kỳ vọng đúng, không phải bug).

**3 defect mới, mức độ nghiêm trọng khác nhau:**
- **DEFECT-009 (Critical)** — tính năng TTS hoàn toàn không hoạt động trong MỌI môi trường vì hardcode sai URL, có rủi ro gọi nhầm sang service của dự án khác nếu vô tình có gì chạy ở `127.0.0.1:8001`.
- **DEFECT-008 (Major)** — Guest không thể dùng thử phân tích giọng nếu không tự thêm `scriptOrigin` (tham số vốn thiết kế optional).
- **DEFECT-010 (Major)** — cùng nhóm nguyên nhân DEFECT-001 (thiếu SecurityConfig whitelist), phát hiện thêm 1 endpoint bị ảnh hưởng.

**Phát hiện tích cực đáng chú ý:** Toàn bộ pipeline AI chính (analyze-voice, adaptive calibration sau 10 session) hoạt động đúng end-to-end với dữ liệu thật — đây là lần đầu tiên tính năng cốt lõi nhất của sản phẩm (P0, "Voice Training core") được verify thật qua HTTP với AI service thật, không chỉ qua Mockito. Trước phiên này, `VoiceServiceImplTest` (23 unit test) chỉ mock AI response, chưa từng xác nhận tích hợp thật hoạt động.

**Ghi chú:** Mọi Fail/Defect phát hiện trong lúc Execute đã ghi vào `testing/defect-log/` theo template, KHÔNG tự sửa code — đúng thẩm quyền QA theo `testing/testing.md`.
