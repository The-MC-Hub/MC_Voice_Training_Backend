# Development & Onboarding Guide

Document Version: 2.0.0
Target Platform: MC Voice Training Backend

---

## 1. Prerequisites & Environment Setup

Ensure the following tools are installed on your local development machine:

- **Java Development Kit (JDK)**: OpenJDK 21 or Amazon Corretto 21
- **Build Tool**: Apache Maven 3.9+
- **Database**: MongoDB 7.0+ (Local instance or MongoDB Atlas account)
- **Search Engine**: Elasticsearch 8.x (Optional for local search testing)
- **IDE**: IntelliJ IDEA (Recommended) or VS Code with Java Extension Pack

---

## 2. Getting Started (Step-by-Step)

### Step 1: Clone & Configure Environment
```bash
git clone https://github.com/mchub/MC_Voice_Training_Backend.git
cd MC_Voice_Training_Backend
cp .env.example .env
```

Open `.env` and configure required parameters:
- `MONGODB_URI`
- `JWT_SECRET`
- `CLOUDINARY_*`
- `PAYOS_*`
- `BREVO_SMTP_KEY`

### Step 2: Build & Generate MapStruct Mappers
```bash
mvn clean compile
```

### Step 3: Launch Local Development Server
```bash
mvn spring-boot:run
```
The server will start on port `5000` (or the port defined in `.env`).

---

## 3. Running Unit & Integration Tests

```bash
# Run full test suite (46 test classes, 493+ @Test methods)
mvn test

# Run a single test class
mvn test -Dtest=AuthControllerTest

# Run full build package including tests
mvn clean package
```

---

## 4. MapStruct Code Regeneration Workflow

MapStruct mappers are defined under `src/main/java/com/mchub/mapper/`. Whenever you add fields to DTOs or Entities, you MUST re-run compilation to regenerate target implementation classes:

```bash
mvn compile
```
