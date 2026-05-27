# ── Stage 1: Build ───────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache deps layer (chỉ re-run khi pom.xml thay đổi)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build JAR (skip tests — chạy test riêng trong CI)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy JAR từ build stage
COPY --from=builder /app/target/backend-java-1.0.0.jar app.jar

# Render inject PORT env var — Spring đọc từ ${PORT:5000}
EXPOSE 5000

ENTRYPOINT ["java", "-jar", \
  "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:prod}", \
  "-Dserver.port=${PORT:5000}", \
  "app.jar"]
