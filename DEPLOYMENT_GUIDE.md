# Production Deployment & Infrastructure Guide

Document Version: 2.0.0
Target Host: Render Cloud Platform (`render.yaml`)

---

## 1. Overview & Cloud Architecture

The backend application is containerized via Docker and deployed on Render cloud environment.

- **Production URL**: `https://mc-voice-training-backend.onrender.com`
- **Instance Type**: Free / Web Service
- **Auto-Sleep Behavior**: Free tier instances enter sleep mode after 15 minutes of HTTP inactivity.
- **Cold Start Duration**: 30 to 60 seconds upon receiving the first wake-up HTTP request.

---

## 2. Docker Build & Deployment Configuration

### Dockerfile Specification
Multi-stage Docker build utilizing Eclipse Temurin 21 JRE:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 3. Render Blueprint (`render.yaml`) Deployment

The service deployment is governed by `render.yaml` at the root directory:

```yaml
services:
  - type: web
    name: mc-voice-training-backend
    env: docker
    plan: free
    healthCheckPath: /actuator/health
    envVars:
      - key: PORT
        value: 5000
      - key: MONGODB_URI
        sync: false
      - key: JWT_SECRET
        sync: false
```

---

## 4. Production Health Monitoring & Incident Mitigation

- **Health Check Endpoint**: `GET /actuator/health` (Returns `{ "status": "UP" }`)
- **System Metrics**: Monitored via `Actuator` and `AdminSystemController` (`/api/v1/admin/health`).
- **Mitigation for Cold Starts**: Configure external cron keep-alive ping every 10 minutes to maintain active status.
