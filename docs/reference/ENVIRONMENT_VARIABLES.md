# Technical Reference: Environment Variable Registry

Document Version: 1.0.0
Configuration Source: `.env` file / OS Environment Variables

---

## Complete Environment Variable List

| Variable Name | Required | Default / Example Value | Description |
|---|---|---|---|
| `PORT` | Yes | `5000` | HTTP Server Port |
| `SPRING_PROFILES_ACTIVE` | No | `dev` | Active Spring profile (`dev`, `prod`, `test`) |
| `MONGODB_URI` | Yes | `mongodb+srv://user:pass@cluster.mongodb.net/mchub` | Primary MongoDB Atlas connection string |
| `MONGODB_TEST_URI` | No | `mongodb+srv://user:pass@cluster.mongodb.net/mchub_test` | QA & Integration testing MongoDB URI |
| `JWT_SECRET` | Yes | `LongSecretKeyWithMinimum256BitsLengthForHMACSHA256Signature...` | Base64 / Hex secret for JWT access tokens |
| `JWT_EXPIRATION_MS` | No | `86400000` (24h) | Access Token TTL in milliseconds |
| `JWT_REFRESH_EXPIRATION_MS` | No | `604800000` (7 days) | Refresh Token TTL in milliseconds |
| `ELASTICSEARCH_URIS` | No | `http://localhost:9200` | Elasticsearch cluster endpoint |
| `CLOUDINARY_CLOUD_NAME` | Yes | `mchub-cloud` | Cloudinary cloud account name |
| `CLOUDINARY_API_KEY` | Yes | `123456789012345` | Cloudinary API Key |
| `CLOUDINARY_API_SECRET` | Yes | `aBcDeFgHiJkLmNoPqRsTuVwXyZ` | Cloudinary API Secret Key |
| `PAYOS_CLIENT_ID` | Yes | `payos_client_id_xxx` | PayOS Merchant Client ID |
| `PAYOS_API_KEY` | Yes | `payos_api_key_xxx` | PayOS API Key |
| `PAYOS_CHECKSUM_KEY` | Yes | `payos_checksum_key_xxx` | PayOS HMAC signature validation key |
| `AI_ANALYZE_URL` | Yes | `https://mc-voice-ai.hf.space/analyze` | External FastAPI audio scoring endpoint |
| `AI_TTS_URL` | Yes | `https://mc-voice-ai.hf.space/tts` | External FastAPI Text-To-Speech endpoint |
| `BREVO_SMTP_KEY` | Yes | `xkeysib-xxx` | Brevo API v3 Key |
| `MAIL_FROM_ADDRESS` | No | `no-reply@mchub.vn` | Default sender email address |
| `ALLOWED_ORIGINS` | Yes | `http://localhost:3000,https://mchub.vn` | CORS Allowed origins (comma-separated) |
