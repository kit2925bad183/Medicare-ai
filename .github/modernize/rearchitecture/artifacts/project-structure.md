# Project Structure

## Project Classification

**Full-stack single-module web application**: one Spring Boot MVC runtime serves Thymeleaf pages and static browser assets. It is not a monorepo, standalone SPA, library, or CLI product.

## Top-Level Structure

- `pom.xml` — Maven application descriptor and managed Spring Boot dependency set.
- `src/main/java/com/medicare/` — Java application code; 11 source files and 368 measured Java lines.
- `src/main/resources/application.yml` — runtime configuration.
- `src/main/resources/db/migration/V2__create_users_table.sql` — additive Flyway schema migration.
- `src/main/resources/templates/` — login/registration pages and shared layout fragment.
- `src/main/resources/static/` — CSS and browser JavaScript.
- `src/test/java/` — 3 Java test classes covering startup/Jackson serialization, CSRF, and user-schema migration.
- `src/test/resources/templates/test/logout-form.html` — test-only logout form.
- `target/` — generated Maven build output; excluded from source anchors.
- `.github/modernize/` — modernization workflow artifacts; not application runtime source.

There is no Maven wrapper, separate frontend package, README, or independent application module in the inspected project tree.

## Package and Layer Breakdown

The measured 11 Java files are distributed as follows:

- `com.medicare` — 1 file: `MediCareAiApplication`; Spring Boot entry point.
- `com.medicare.common` — 2 files: `ApiExceptionHandler`, `CurrentUserService`; MVC error mapping and authenticated-principal lookup.
- `com.medicare.config` — 1 file: `SecurityConfig`; Spring Security filter-chain and authentication wiring.
- `com.medicare.identity` — 7 files: controller, service, registration request, user entity/repository, user-details service, and role enum.

Effective layers:

1. **Web and server-rendered UI** — `IdentityController`, `ApiExceptionHandler`, Thymeleaf authentication forms and shared layout.
2. **Services** — `IdentityService` (registration transaction) and `CurrentUserService` (principal lookup; no current route/service caller was found).
3. **Persistence/model** — `UserRepository`, `User`, and `Role`.
4. **Framework configuration** — `SecurityConfig` and `application.yml`.
5. **Presentation assets** — shared layout, authentication templates, CSS, and a JavaScript visualization module.

The source follows a compact package-by-feature structure, not a strict multi-module layered architecture. `CurrentUserService` is a Spring bean but is not invoked by an existing externally triggered unit.

## Runtime Entry Points

- `MediCareAiApplication.main()` — Spring Boot startup at `src/main/java/com/medicare/MediCareAiApplication.java:9`.
- `IdentityController` — `GET /`, `GET /login`, `GET /register`, and `POST /register`.
- Spring Security filter chain — `POST /login` and `POST /logout`; no MVC controller handles these submissions.
- Flyway startup migration — `V2__create_users_table.sql` runs through the configured migration runner before the application serves requests.
- Static assets — public `/css/**`, `/js/**`, and `/images/**` matchers are configured; existing source contains CSS and JS files.
- No production scheduled jobs, message handlers, dedicated API controllers, email/push integrations, reminder or appointment flows, or assistant-action handlers were found.

## Existing Functional Areas

- **Identity/session** — login and logout, registration validation, password hashing, persisted user creation, session authentication, remember-me, and CSRF-protected state-changing requests.
- **Navigation shell** — a shared fragment links to dashboard, medicines, appointments, doctors, and notification preferences. No matching application controller/page implementation was found.
- **Landing route** — `/` redirects to `/dashboard`, but no dashboard handler/template was found.
- **3D visual** — `static/js/app.js` dynamically imports Three.js from jsDelivr when its canvas exists; login and registration pages do not include the shared layout/canvas.
- The Phase 2 brief's assistant, medicine/reminder, appointment, caregiver approval, audit/report/export/settings, email delivery, and browser/web-push behavior are not implemented in the current production source inventory.

## Runtime and Persistence Constraints

- The POM declares Java 25 and Spring Boot parent 4.1.2.
- The configured server port is 8080.
- Default datasource is in-memory H2, with environment overrides for URL, username, password, and driver. In-memory H2 does not provide durable storage across process restarts.
- Hibernate uses `ddl-auto: validate`; Flyway is enabled with `baseline-on-migrate: true`. `V2` creates `users` with `IF NOT EXISTS`; the live verification artifact reports migration version 2 on H2.
- CSRF is enabled by Spring Security's default filter-chain behavior because the current `SecurityConfig` does not disable or customize it. Integration and live checks are documented in the consumed `t1.1-backend.md`.
- Session cookie configuration uses `HttpOnly=true`, `SameSite=LAX`, and `secure=false` in `application.yml`.
- The Git root is `C:\`; the project itself has no `.git` metadata. Scope Git operations to this project path; do not stage/reset/clean the parent worktree broadly.
