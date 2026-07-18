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

**Open — nghiêm trọng, đề xuất ưu tiên fix sớm.** Đề xuất dev (không phải QA quyết định): thêm field `@Value("${ai.service.tts-url:http://127.0.0.1:8001/tts/stream}") private String ttsServiceUrl;` (tương tự `aiServiceUrl` đã có), thay `"http://127.0.0.1:8001/tts/stream"` bằng biến này. Cần xác nhận thêm với team AI route chính xác trên HF Space là `/generate-mc-voice` hay `/tts/stream` (2 route khác nhau tồn tại trong `openapi.json` của HF Space: cả `/generate-mc-voice` và `/tts/stream` đều có — cần dev xác nhận route nào đúng cho luồng streaming này).
