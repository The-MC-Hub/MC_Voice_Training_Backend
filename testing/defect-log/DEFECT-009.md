## DEFECT-009: `VoiceServiceImpl.generateTTSAudio()` hardcode `http://127.0.0.1:8001/tts/stream` — bỏ qua hoàn toàn cấu hình `ai.service.tts-url`/`AI_TTS_URL`

- **Module:** Voice Training (Text-to-Speech)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-VOICE-24 (system test UC-03).
- **Severity:** Critical
- **Priority:** P0
- **Môi trường:** LIVE — gọi thật `POST /api/v1/voice/tts/generate` trên `mchub_test`.

### Root Cause

`VoiceController` có field `aiServiceUrl` inject đúng từ config (`@Value("${ai.service.analyze-url:...}")`), và `analyzePractice()`/`proxyAnalyzeVoice()` dùng biến này đúng cách — nhưng `generateTTSAudio()` lại hardcode URL trực tiếp trong code, không dùng bất kỳ field cấu hình nào:

```java
public byte[] generateTTSAudio(String text, String voice) {
    ...
    org.springframework.http.ResponseEntity<byte[]> response = restTemplate.exchange(
            "http://127.0.0.1:8001/tts/stream",   // ← HARDCODE, không đọc application.properties/env
            org.springframework.http.HttpMethod.POST,
            requestEntity,
            byte[].class
    );
```

So sánh với `application.properties`:
```
ai.service.analyze-url=${AI_ANALYZE_URL:http://127.0.0.1:8001/analyze-voice}
ai.service.tts-url=${AI_TTS_URL:http://127.0.0.1:8001/generate-mc-voice}
```
Property `ai.service.tts-url` **tồn tại trong config nhưng KHÔNG được `@Value` inject và sử dụng ở đâu cả** trong `VoiceServiceImpl.java` — dead config, gây hiểu lầm rằng đổi `AI_TTS_URL` sẽ có tác dụng.

**Nghiêm trọng hơn:** `127.0.0.1:8001` theo đúng ghi chú trong `Test_Plan.md` mục 6 là AI service của **một dự án KHÁC** đang chạy cục bộ trên máy dev, không phải service của MC Hub. Nghĩa là code hiện tại của tính năng TTS **luôn cố gắng gọi nhầm sang service của dự án khác** trong mọi môi trường (dev, test, và có khả năng cả production nếu deploy nguyên trạng), bất kể cấu hình `AI_TTS_URL`/`ai.service.tts-url` được set gì.

### Impact nghiệp vụ

- **Môi trường không có gì chạy ở `127.0.0.1:8001`:** tính năng TTS luôn lỗi kết nối/404, hoàn toàn không dùng được.
- **Môi trường vô tình có service khác chạy ở `127.0.0.1:8001`** (như trường hợp máy dev hiện tại, đã cảnh báo rõ trong Test Plan): request TTS của MC Hub bị gửi nhầm sang hệ thống của dự án khác — rủi ro rò rỉ dữ liệu (`text` người dùng nhập) sang hệ thống ngoài phạm vi, và nhận response không xác định (crash hoặc dữ liệu rác) thay vì audio thật.
- Route `/generate-mc-voice` cấu hình đúng trong `.env`/`application.properties` không bao giờ được gọi tới — tính năng TTS trên HF Space thật **chưa từng được dùng dù đã deploy và cấu hình sẵn**.

### Evidence

```
$ curl -X POST ".../voice/tts/generate?text=Xin+chao" -H "Authorization: Bearer <jwt>"
HTTP 500
{"status":"error","message":"TTS service error: 404 Not Found: \"{\"detail\":\"Not Found\"}\"","errorCode":"ERR_9001"}
```
Response 404 xác nhận request đã đi ra ngoài (không phải lỗi kết nối do không có gì lắng nghe ở cổng 8001) — tức đã thật sự chạm phải MỘT service nào đó ở `127.0.0.1:8001` (theo Test Plan, là service của dự án khác), và service đó không có route `/tts/stream`.

Source: `VoiceServiceImpl.java` dòng 438 — string literal `"http://127.0.0.1:8001/tts/stream"`.

### Status

**Fixed (2026-07-18).** Thêm đúng field `@Value("${ai.service.tts-url:http://127.0.0.1:8001/tts/stream}") private String ttsServiceUrl;` như đề xuất, thay `"http://127.0.0.1:8001/tts/stream"` hardcode bằng biến này trong `generateTTSAudio()`.

**Verify live (port 5555, `mchub_test`, `AI_TTS_URL` trỏ HF Space thật):**
```
$ curl -X POST ".../voice/tts/generate?text=Xin+chao" -H "Authorization: Bearer <jwt>"
HTTP 200 (Content-Type: audio/wav) — body: {"status":"error","message":"TTS model not loaded. Check ./models/mms-tts-vie"}
```
Request giờ ĐÃ ĐI ĐÚNG tới HF Space đã cấu hình (không còn kết nối nhầm sang `127.0.0.1:8001` của dự án khác — xác nhận qua log server không có exception route-not-found/connection-refused nào). Lỗi "TTS model not loaded" trong response body là hạn chế NỘI TẠI của chính AI service (đã ghi nhận từ trước: `tts_loaded:false` trên HF Space) — nằm ngoài phạm vi code backend, không phải lỗi của fix này.

`VoiceServiceImplTest`/`VoiceControllerTest` — 40/40 PASS.

**Chưa xác nhận với team AI:** route chính xác `/generate-mc-voice` hay `/tts/stream` cho luồng streaming — `application.properties` hiện đang trỏ `AI_TTS_URL` default là `/generate-mc-voice` trong khi code default fallback (khi biến môi trường không set) vẫn giữ `/tts/stream` (giữ nguyên giá trị hardcode cũ làm fallback, không tự ý đổi vì chưa có xác nhận route nào đúng — xem ghi chú gốc). Cả 2 route đều tồn tại trên HF Space theo `openapi.json` đã audit trước đó.
