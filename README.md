# MediCare AI - Phase 2 Platform

MediCare AI is a production-oriented, modular healthcare web application built with **Java 25**, **Spring Boot 4.1**, **Thymeleaf**, **Spring Security**, **Spring Data JPA**, and **Flyway**. It provides deterministic, rule-based medication scheduling, appointment management, multi-channel notification dispatch, caregiver authorization, interactive 3D healthcare visuals, and verifiable adherence reporting.

---

## Key Features & Architecture

### 1. Local-First, Deterministic AI Reminder Assistant
- **Zero Paid AI / No External LLM APIs**: Powered entirely by deterministic Java business rules without external API dependencies or data privacy leaks.
- **Controlled Command Grammar**:
  1. `show today's medicine schedule` / `show today schedule`: Summarizes today's pending and available doses.
  2. `summarize upcoming appointments` / `upcoming appointments`: Lists verified healthcare provider visits.
  3. `explain saved reminder settings`: Explains time zones, quiet hours, and channel states.
  4. `propose notification preferences`: Recommends optimal quiet hours and delivery channels.
  5. `identify conflicting or incomplete schedules`: Analyzes schedule overlaps within 15 minutes, exact time clashes, or missing meal/dosage instructions.
  6. `plan daily reminder for <name> at <HH:mm>` / `plan weekly reminder for <name> on <weekday> at <HH:mm>`: Generates a structured proposal.
- **Safety & Validation**:
  - Validates medicine names, future start dates, and time zones. Prevents duplicate schedules.
  - Generates a preview with a secure, time-bounded session confirmation token.
  - Explicit confirmation is mandatory before any persistent schedule is created.
  - Rejects arbitrary medical advice, diagnostic claims, and SQL/code execution. Ambiguous requests return structured help.
  - Every assistant inquiry and action records a privacy-conscious audit event.

### 2. Persistent, Recoverable Reminder Scheduling
- **Storage**: Reminder occurrences persist in the database with UTC timestamps, rolling 8-day forward horizon, and unique idempotency keys (`scheduleId:localDate:localTime`).
- **Time Zone & DST Handling**: Recurrence calculation evaluates user IANA time zones via `java.time.ZoneId`. Daylight saving transition gaps are shifted forward by the gap interval; ambiguous overlaps resolve to the earlier offset.
- **Crash Recovery**: Pending work is safely recovered after application restarts without silent loss or duplicate generation.
- **Single-Instance Safe Locking**: Uses short transactions and database row locks (`SELECT ... FOR UPDATE` via `PessimisticLock`) during due-reminder polling.
- **Multi-Instance Locking Considerations**: For multi-instance clustered deployments, implement distributed locking (e.g., ShedLock with JDBC provider or Redis Redlock) or partitioned consumer queues to guarantee single-worker processing across distributed nodes.

### 3. Multi-Channel Notification Dispatch & Bounded Retries
- **Dispatch Engine**: Decoupled `NotificationChannel` abstraction supporting `IN_APP`, `EMAIL`, `BROWSER`, and `PUSH`.
- **Bounded Retry with Exponential Backoff**:
  - Maximum 3 attempts before terminal failure (`FAILED`).
  - Attempt 1 failure: retry scheduled in 2 minutes (`RETRYING`).
  - Attempt 2 failure: retry scheduled in 5 minutes (`RETRYING`).
  - Attempt 3 failure: transitioned to `FAILED`.
  - Opted-out channels (`CHANNEL_DISABLED`) fail immediately without retry.
- **Sanitized Error Categorization**: Records non-sensitive error categories (`SMTP_NOT_CONFIGURED`, `QUIET_HOURS_ACTIVE`, `CHANNEL_DISABLED`, `SMTP_SEND_FAILED`, `BROWSER_PUSH_FAILED`).

### 4. Email Reminders (Spring Mail)
- Safe, non-sensitive notification subject lines (`MediCare Reminder: Scheduled Medication Due`) without leaking private diagnoses or medical notes.
- Respects user-configured **Quiet Hours** (e.g., 22:00 to 07:00), deferring disruptive notifications until daytime hours.
- If SMTP credentials are not configured, records `SMTP_NOT_CONFIGURED` without falsely marking reminders as delivered.

### 5. Browser Notifications & Web Push (Level A & Level B)
- **Level A (In-Tab)**: In-app notification center and real-time alerts when the portal is open in an eligible tab.
- **Level B (Service Worker & Web Push)**:
  - Service worker at `/sw.js` handles background push events and notification clicks to `/dashboard`.
  - Push subscription endpoints: `POST /api/push/subscribe` and `POST /api/push/unsubscribe` with CSRF protection.
  - Expired or revoked push endpoints are automatically culled upon 410 Gone / 404 responses.
  - *Note*: Web Push in production requires HTTPS and valid VAPID keys. On localhost, modern browsers support service workers under `http://localhost`.

### 6. Delegated Caregiver Authorization
- **Explicit Patient Consent**: Caregivers can be invited by email; relationships remain `PENDING` until approved.
- **Revocable Access**: Patients or caregivers can revoke access at any time.
- **Read-Only Enforcement**: Active caregivers receive read-only access to schedules, appointments, and upcoming doses. Modification attempts are denied at the service layer.
- **Auditing**: Every caregiver invitation, approval, revocation, and patient record access generates an immutable audit record.

### 7. Reporting, Dose History & Adherence
- **Dose Action Tracking**: Users record actions on reminder occurrences: `TAKEN`, `SKIPPED`, or `MISSED`, with optional notes.
- **Adherence Summaries**: 7-day and 30-day compliance percentages computed strictly from recorded actions.
- **Mandatory Adherence Disclaimer**:
  > *"Adherence metrics are calculated exclusively from verified dose action events recorded by the user. A generated occurrence or delivered reminder is not proof that medication was administered."*
- **Personal Data Export**: Authenticated JSON data export available at `/reports/export` containing the user's isolated profile, schedules, doctor appointments, dose logs, and adherence metrics.

### 8. Interactive 3D Healthcare Visual
- Lightweight Three.js double-helix / molecular geometry in the dashboard hero card.
- Loaded as an asynchronous ES module without blocking core interface rendering.
- Automatic WebGL capability detection with graceful static DOM fallback (`#threejs-fallback`).
- Respects `prefers-reduced-motion: reduce` by pausing rotational animations.

---

## Technology Stack

- **Runtime**: Java 25 (LTS)
- **Framework**: Spring Boot 4.1.2 (Spring MVC, Spring Data JPA, Spring Security, Spring Mail, Spring Validation)
- **Database**: H2 (file-based local persistence at `./data/medicare`, in-memory for testing) & MySQL 8.x compatible
- **Schema Migrations**: Flyway (V1 through V6)
- **Templates & UI**: Thymeleaf, Vanilla CSS Design System, Responsive Flexbox/Grid
- **Graphics**: Three.js (r160)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, Spring Security Test

---

## Flyway Migration History

- `V1__init_auth.sql`: Users and role definitions (`PATIENT`, `DOCTOR`, `ADMIN`).
- `V2__initial_schema.sql`: Initial medicine and schedule definitions.
- `V3__medicine_reminders.sql`: Rolling reminder occurrences and in-app deliveries.
- `V4__add_appointments_and_notifications.sql`: Doctor directory, appointment bookings, and notification records.
- `V5__caregivers_reporting_notifications.sql`: Caregivers table, quiet hours, push subscriptions, time-zone settings.
- `V6__dose_actions_and_reporting.sql`: Persistent `dose_logs` table, occurrence lifecycle states (`TAKEN`, `SKIPPED`, `MISSED`), delivery channels (`EMAIL`, `BROWSER`, `PUSH`), and delivery states (`RETRYING`, `FAILED`).

---

## Environment Configuration

Configure the application via environment variables or `application.yml`:

```powershell
# Database (MySQL 8.x - Optional, defaults to file-based H2 at ./data/medicare)
$env:DB_URL = "jdbc:mysql://localhost:3306/medicare_ai?serverTimezone=UTC&useSSL=false"
$env:DB_USERNAME = "medicare_app"
$env:DB_PASSWORD = "your_secure_password"
$env:DB_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver"

# Spring Mail / SMTP (Optional for live email delivery)
$env:SPRING_MAIL_HOST = "localhost"
$env:SPRING_MAIL_PORT = "1025"
$env:SPRING_MAIL_USERNAME = ""
$env:SPRING_MAIL_PASSWORD = ""
$env:MEDICARE_MAIL_FROM = "noreply@medicare.local"

# Security & Sessions
$env:SESSION_COOKIE_SECURE = "false" # Set to "true" for production HTTPS
$env:REMEMBER_ME_KEY = "custom-strong-remember-me-key"
```

### Development Mail Catcher (Mailpit)
To capture emails locally without a real SMTP server:
```bash
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
```
View captured reminder emails at `http://localhost:8025`.

---

## Production Deployment

MediCare AI provides turnkey, production-grade containerization and cloud deployment workflows:

### Option 1: 1-Click Docker Compose (Recommended)
Orchestrates the Spring Boot app, a dedicated MySQL 8 database, and a local Mailpit email catcher with healthchecks and persistent storage:

```bash
# 1. Initialize environment configuration
cp .env.example .env

# 2. Build and launch all services in background
docker compose up -d --build

# 3. View live logs
docker compose logs -f app
```

- **Web Portal**: [http://localhost:8080](http://localhost:8080)
- **Actuator Health**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Mailpit Email Testing UI**: [http://localhost:8025](http://localhost:8025)

Alternatively, run the automated deployment script:
- Windows: `.\deploy.ps1`
- Linux / macOS: `./deploy.sh`

### Option 2: Standalone Production JAR
Build an executable, self-contained JAR with production profile:

```powershell
# Build executable production package
& "C:\Users\vishv\.maven\maven-3.9.15\bin\mvn.cmd" clean package -DskipTests

# Run with production configuration
java -jar -Dspring.profiles.active=prod target/medicare-ai-0.0.1-SNAPSHOT.jar
```

### Option 3: Cloud Container Deployments (GCP Cloud Run / AWS ECS / Render / Railway)
The multi-stage `Dockerfile` uses **Eclipse Temurin OpenJDK 25** with a secure unprivileged service user (`medicare:1001`), container-aware memory allocation (`-XX:MaxRAMPercentage=75.0`), and built-in Actuator healthcheck (`/actuator/health`).

```bash
# Build production Docker image
docker build -t medicare-ai:latest .

# Run with external MySQL database
docker run -d -p 8080:8080 \
  -e DB_URL="jdbc:mysql://your-cloud-db:3306/medicare_ai?useSSL=true" \
  -e DB_USERNAME="your_username" \
  -e DB_PASSWORD="your_password" \
  -e SESSION_COOKIE_SECURE="true" \
  -e REMEMBER_ME_KEY="your-strong-production-key" \
  --name medicare-app medicare-ai:latest
```

---

## REST Endpoints Summary

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/dashboard/summary` | User upcoming reminders, appointments, adherence rate & disclaimer |
| `GET` | `/api/doses/today` | List of today's dose occurrences for the authenticated user |
| `POST` | `/api/doses/{id}/actions` | Record dose action (`TAKEN`, `SKIPPED`, `MISSED`) with optional notes |
| `POST` | `/api/push/subscribe` | Register browser Web Push subscription |
| `POST` | `/api/push/unsubscribe` | Unregister Web Push subscription |
| `GET` | `/reports/export` | Download authenticated patient health record in JSON format |
