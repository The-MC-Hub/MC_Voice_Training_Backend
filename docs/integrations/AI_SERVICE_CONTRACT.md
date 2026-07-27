# Integration Specification: Python FastAPI AI Scoring Engine

Document Version: 1.0.0
Target Integration: External FastAPI AI Service (`AI_ANALYZE_URL`, `AI_TTS_URL`)

---

## 1. Service Endpoints & Protocols

The core backend proxies audio analysis and speech synthesis requests to the external FastAPI instance via HTTP REST APIs.

### Environment Configuration
- `AI_ANALYZE_URL`: Base URL for audio evaluation (e.g. `https://mc-voice-ai.hf.space/analyze`)
- `AI_TTS_URL`: Base URL for speech synthesis (e.g. `https://mc-voice-ai.hf.space/tts`)

---

## 2. API Contracts

### 2.1 Audio Evaluation (`POST /analyze`)

#### Request Payload
```json
{
  "audio_url": "https://res.cloudinary.com/mchub/raw/upload/v1234/practice_102.wav",
  "reference_text": "Xin chào quý vị khán giả đang theo dõi chương trình hôm nay.",
  "difficulty_level": "INTERMEDIATE"
}
```

#### Response Payload (HTTP 200)
```json
{
  "status": "success",
  "data": {
    "overall_score": 85.5,
    "pronunciation_score": 88.0,
    "intonation_score": 82.0,
    "speed_pacing_score": 86.5,
    "accuracy_score": 86.0,
    "word_analysis": [
      { "word": "Xin", "score": 95, "feedback": "Clear" },
      { "word": "chào", "score": 90, "feedback": "Good tone" }
    ],
    "detailed_feedback": "Giọng đọc truyền cảm, cần chú ý ngắt nghỉ ở dấu phẩy."
  }
}
```

### 2.2 Text-To-Speech Generation (`POST /tts`)

#### Request Payload
```json
{
  "text": "Nội dung bài tập đọc mẫu.",
  "voice_id": "vi_female_01",
  "speed": 1.0
}
```

#### Response Payload (HTTP 200)
```json
{
  "status": "success",
  "data": {
    "audio_url": "https://res.cloudinary.com/mchub/raw/upload/v1234/tts_output.mp3",
    "duration_seconds": 12.4
  }
}
```

---

## 3. Resilience & Fallback Protocol

- **Cold-Start Latency**: HuggingFace Spaces free tier auto-sleeps after inactivity. Initial request can take up to 45 seconds. Frontend request timeout MUST be set to 60,000ms.
- **Service Failure**: If FastAPI returns 5xx or times out, backend returns HTTP 503 `AI_SERVICE_UNAVAILABLE` with fallback mock guidance to preserve application uptime.
