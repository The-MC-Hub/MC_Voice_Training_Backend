## DEFECT-008: `POST /voice/practice/analyze-guest` (và `/voice/proxy/analyze-voice`) trả HTTP 500 khi gọi mà không kèm `scriptOrigin` — AI service HF Space từ chối `script_origin` rỗng

- **Module:** Voice Training (Guest analyze / Proxy analyze)
- **Ngày phát hiện:** 2026-07-17 — phát hiện khi thực thi TC-VOICE-21 (system test UC-03, lần đầu tiên test end-to-end với AI service thật qua HTTP, trước đây chỉ có unit test mock).
- **Severity:** Major
- **Priority:** P1
- **Môi trường:** LIVE — gọi thật `POST /api/v1/voice/practice/analyze-guest` trên `mchub_test`, AI service thật là HF Space `https://trung2605-voice-ai-tranning.hf.space`.

### Root Cause

`VoiceServiceImpl.proxyAnalyzeVoice()`:
```java
body.add("script_origin", scriptOrigin != null ? scriptOrigin : "");
```
Khi client không gửi `scriptOrigin` (tham số `@RequestParam(required = false)` ở Controller), code fallback gửi chuỗi RỖNG `""` cho AI service — không phải bỏ qua field, không phải `null` literal, mà là `script_origin=""`.

Test trực tiếp AI service HF Space xác nhận: multipart form field `script_origin=""` (rỗng) bị FastAPI/Pydantic coi là **"missing"** (`{"detail":[{"type":"missing","loc":["body","script_origin"],"msg":"Field required","input":null}]}`, HTTP 422) — đây là hành vi đặc thù của cách AI service (Python/FastAPI) parse multipart form field rỗng, không phải lỗi ở phía Java backend theo nghĩa "gửi sai request", nhưng fallback `""` trong code Java **không đạt mục đích** (là cho AI service optional field xử lý được), khiến toàn bộ request thất bại.

Backend Java bắt exception này và trả về generic:
```json
{"status":"error","message":"AI service unavailable","errorCode":"ERR_9001"}
```
HTTP 500 — che giấu nguyên nhân thật (client không gửi `scriptOrigin`), khiến người dùng cuối và dev khó chẩn đoán.

### Impact nghiệp vụ

**Guest dùng thử tính năng phân tích giọng nói KHÔNG kèm sẵn kịch bản mẫu (`scriptOrigin`) sẽ luôn nhận lỗi 500** — đây là use case rất phổ biến vì `scriptOrigin` là tham số optional trong thiết kế Controller (`@RequestParam(required = false)`), ngụ ý "cho phép bỏ trống". Test trực tiếp xác nhận: chỉ cần thêm `scriptOrigin` bất kỳ (VD `"Xin chao"`) thì request thành công HTTP 200 ngay lập tức — chứng minh rõ nguyên nhân.

Endpoint `/voice/proxy/analyze-voice` (dùng chung `proxyAnalyzeVoice()`) có khả năng gặp lỗi tương tự vì cùng logic.

### Evidence

```
# Không có scriptOrigin — 500
$ curl -X POST ".../voice/practice/analyze-guest" -F "audioFile=@test.wav;type=audio/wav"
HTTP 500 {"errorCode":"ERR_9001","message":"AI service unavailable"}

# Server log:
AI proxy call failed: 422 Unprocessable Entity: "{"detail":[{"type":"missing","loc":["body","script_origin"],"msg":"Field required","input":null}]}"

# Có scriptOrigin bất kỳ — 200 OK
$ curl -X POST ".../voice/practice/analyze-guest" -F "audioFile=@test.wav;type=audio/wav" -F "scriptOrigin=Xin chao"
HTTP 200 {"status":"success", ...}

# Test trực tiếp HF Space xác nhận nguyên nhân gốc — script_origin="" (rỗng) bị coi là missing:
$ curl -X POST "https://trung2605-voice-ai-tranning.hf.space/analyze-voice" -F "file=@test.wav" -F "script_origin="
HTTP 422 {"detail":[{"type":"missing","loc":["body","script_origin"],"msg":"Field required","input":null}]}
```

### Status

**Open.** Đề xuất dev (không phải QA quyết định): 1 trong 2 hướng
1. Khi `scriptOrigin` null/rỗng, KHÔNG add key `script_origin` vào multipart body (bỏ hẳn field thay vì gửi chuỗi rỗng) — nếu AI service coi field hoàn toàn vắng mặt là hợp lệ (cần verify riêng với AI service, hành vi FastAPI có thể khác nhau giữa "field vắng mặt" và "field rỗng").
2. Hoặc gửi 1 placeholder non-empty mặc định (VD `" "` hoặc chuỗi rỗng đại diện đã biết AI service chấp nhận) nếu team AI xác nhận đó là cách dùng đúng.
Trong mọi trường hợp, catch block nên phân biệt lỗi 4xx từ AI service (client-side, nên trả 400 kèm chi tiết) với lỗi kết nối/timeout thật (nên trả 500/502) — hiện tại gộp chung thành 500 "AI service unavailable" gây khó chẩn đoán.
