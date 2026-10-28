# t2 — MediCare AI Existing Feature Inventory and Phase 2 Acceptance Criteria

**Role:** Product Manager  
**Phase:** Discovery  
**Status:** Re-verified after CSRF and startup/schema remediation; ready for planning  
**Scope:** Preserve existing user-visible account behavior and define observable Phase 2 outcomes. This document states what users must be able to do; it intentionally does not prescribe internal architecture.

## Upstream Artifacts Consumed
- `t1.1-backend.md` — current live CSRF verification and clean Maven verification after the startup fixes.
- `t1.1.1-dba.md` — confirms the additive users-table migration and migration tests are present and passing.
- `t1.1.1.1-backend.md` — confirms the Spring Boot startup/Jackson remediation and full-context test evidence.
- `project-structure.md`, `tech-stack.md`, and `data-model.md` — source-derived inventory of existing routes, user journeys, framework, and persisted data.
- `units/identity-web/behavior.yaml` and `units/security-session/behavior.yaml` — existing account, authentication, logout, and security contracts.
- `clarification-answers.md` — resolved user constraints and the recorded missing UI-reference/backend-target answers.
- Prior `t2-pm.md` — starting inventory and Phase 2 acceptance set, re-evaluated against the post-remediation evidence.
- `learnings/pm/distinguish-navigation-from-features.md` — keeps advertised but unimplemented navigation separate from delivered functionality.

## Evidence Mapping
- `t1.1-backend.md#Runtime Verification` and `#Test Results` → `BASE-07`, `REQ-016`, and `SC-007` record CSRF as verified behavior rather than an open baseline defect.
- `t1.1.1-dba.md#Verification Scope` and `#Test Results` → the baseline now records the existing additive users migration and the current seven-test suite.
- `project-structure.md#Runtime Entry Points` and `#Existing Functional Areas and Gaps` → the existing account flows, unavailable dashboard/domain destinations, and missing Phase 2 workflows.
- `units/identity-web/behavior.yaml#side_effects` and `#branches` → `BASE-02`–`BASE-04` and `REQ-001` account/registration contracts.
- `units/security-session/behavior.yaml#side_effects` and `#branches`, reconciled with `t1.1-backend.md#Runtime Verification` → `BASE-05`, `BASE-07`, and the security acceptance criteria.
- `clarification-answers.md` (answers F1–F10, B1–B5, and G1–G5) → recorded user scope constraints and planning handoff notes; no clarification is re-opened.

## Scope Baseline

- **Discovery method:** Reconciled the prior route and workflow inventory with the project structure, technology/data-model research, identity/security behavior maps, the submitted clarification answers, and the post-remediation CSRF/runtime verification. This re-run used the provided research artifacts rather than reopening source files.
- **Total current user-facing route behaviors identified:** 6 primary route behaviors (root redirect, login page and submission, registration page and submission, logout submission), plus the now-verified CSRF contract for the three state-changing authentication submissions.
- **Existing working business workflows identified:** 0 reminder, appointment, caregiver, notification, report, settings, or export workflows.
- **Items in scope:** All 6 existing route behaviors are captured as regression expectations; all requested Phase 2 domains below are in scope.
- **Test and migration baseline:** After remediation, the current evidence reports an additive Flyway V2 users-table migration and 7 passing tests (startup/Jackson, CSRF, and migration/persistence). Verification used the default H2 database in MySQL compatibility mode; MySQL execution is not established by this evidence.

### Existing user-facing behavior inventory

| ID | Existing behavior | Observed state / evidence |
|---|---|---|
| BASE-01 | Visiting `/` redirects to `/dashboard`. | `IdentityController.index()`. A dashboard route/template was not found in the enumerated application source. |
| BASE-02 | A visitor can open the login page, enter an email and password, and submit the login form. Invalid credentials produce the generic message “Invalid email or password.” | `IdentityController.login()`, Spring form-login configuration, `templates/auth/login.html`. |
| BASE-03 | A visitor can open registration and submit first name, last name, email, password, and a time zone. | `IdentityController` and `templates/auth/register.html`; the current selector offers UTC, America/New_York, Europe/London, Asia/Kolkata, and Australia/Sydney. |
| BASE-04 | Registration rejects blank names/email/password/time zone, malformed email, overlong values, passwords outside 8–128 characters, and duplicate email addresses; successful registration redirects to login. Email is normalized for duplicate checks and stored lowercase. | `RegistrationRequest`, `IdentityService`, `IdentityController`. Server-side field validation exists; per-field validation messages are not visibly rendered by the registration template. |
| BASE-05 | An authenticated user can submit the logout form and is redirected to login with a logged-out notice. | `SecurityConfig` and `templates/fragments/layout.html`. |
| BASE-06 | The authenticated layout advertises Dashboard, Medicines, Appointments, Doctors, Preferences, and Add medicine, but no matching business controllers/templates were found in the current source inventory. | `templates/fragments/layout.html`; only `IdentityController` declares application MVC routes. Treat these links as intended navigation, not as evidence of working features. |
| BASE-07 | Login, registration, and logout submissions reject requests without a valid CSRF token and accept token-bearing requests; login/logout retain their expected redirect behavior. | `t1.1-backend.md#Runtime Verification`: live tokenless POSTs returned 403; token-bearing requests produced the expected redirects; login/register pages rendered a token. This is now verified baseline behavior and remains a requirement for new state-changing browser actions. |
| BASE-08 | The shared JavaScript can show a fallback for an unavailable 3D healthcare visual if an element with ID `healthcare-scene` exists; no current template contains that element. | `project-structure.md#Existing Functional Areas and Gaps`; not an active user journey in the current templates. |

### Baseline gaps that Phase 2 must address

- Authentication is the only implemented end-to-end user workflow found. The medicines, appointments, doctor, dashboard, and preference links do not currently have corresponding application routes/templates.
- CSRF protection was re-enabled and re-verified after the prior inventory: the packaged app rendered CSRF tokens, rejected tokenless login/registration/logout submissions, and accepted valid token-bearing submissions. Keep this protection for all new state-changing browser interactions.
- The existing exception handler can return an `error` view for validation/not-found conditions, but no `error` template was present in the enumerated templates.
- An additive Flyway V2 users-table migration now exists and was applied successfully in the verified H2 startup path. Phase 2 still requires additive-only changes and must preserve existing users; MySQL migration execution remains unverified.
- The runtime verification shows successful login redirects to `/dashboard`, but the project structure inventory still has no corresponding dashboard handler/template. A usable destination remains an acceptance gap.

## User Scenarios & Testing

### User Story 1 — Sign in and retain the current account journey (Priority: P1)

As a patient, I can register, sign in, and sign out so I can use my private care workspace.

**Why this priority:** The Phase 2 care features are useful only when attached to a private user account, and existing account flows must not regress.

**Independent Test:** Register a patient, sign in with valid credentials, navigate to the care workspace, sign out, and verify an unauthenticated visitor cannot reopen protected content.

**Acceptance Scenarios:**
1. **Given** a visitor with valid registration details, **When** they submit registration, **Then** an account is created and they are directed to sign in.
2. **Given** an existing account, **When** the user submits valid credentials, **Then** they enter their own workspace; invalid credentials show a non-disclosing error and do not authenticate.
3. **Given** an authenticated user, **When** they submit sign out, **Then** the session ends and protected pages require authentication again.
4. **Given** the existing supported registration validations and duplicate-email case, **When** each invalid value is submitted, **Then** registration is rejected without creating another account and the user receives understandable feedback.

### User Story 2 — Safely propose and confirm care changes (Priority: P1)

As a patient, I can describe a supported care task and review exactly what the assistant proposes before any reminder or appointment is changed.

**Why this priority:** The assistant must be useful without silently changing healthcare-related data or depending on a paid AI service.

**Independent Test:** Submit a supported deterministic request, inspect its preview, confirm it, and compare persisted care data; repeat and cancel the preview and verify no change occurred.

**Acceptance Scenarios:**
1. **Given** a supported, unambiguous request, **When** the patient asks the assistant to create or change a reminder or appointment, **Then** the assistant presents the exact item, schedule/time zone, and action for review before saving.
2. **Given** a preview, **When** the patient explicitly confirms it, **Then** only the displayed change is applied once and the patient can see the result.
3. **Given** a preview, **When** the patient cancels or lets it expire, **Then** no care data is changed.
4. **Given** an unsupported or ambiguous request, **When** the patient submits it, **Then** the assistant asks for clarification or explains the supported options and does not guess or mutate data.
5. **Given** any assistant input, **When** it is processed, **Then** it cannot invoke arbitrary operating-system commands, scripts, or unrelated actions; assistant decisions are deterministic and do not require a paid AI service.

### User Story 3 — Manage medicines and receive durable reminders (Priority: P1)

As a patient, I can create, review, edit, pause, and remove medication reminders and understand each scheduled occurrence.

**Why this priority:** Reliable medication reminders are the central recurring care workflow.

**Independent Test:** Create a reminder with a repeat schedule, inspect upcoming occurrences, change and cancel it, and verify occurrence history remains understandable.

**Acceptance Scenarios:**
1. **Given** a valid medicine reminder, **When** the patient saves it, **Then** its name/instructions, schedule, time zone, and active state are visible in the patient’s medicine/reminder view.
2. **Given** an active recurring reminder, **When** its schedule produces an occurrence, **Then** that occurrence is individually identifiable with its scheduled local date/time and current delivery state.
3. **Given** an occurrence has already been processed, **When** the application restarts or retries processing, **Then** the patient sees no duplicate logical occurrence or duplicate successful delivery for that occurrence.
4. **Given** a reminder is paused, changed, or removed, **When** its former future schedule comes due, **Then** no unintended notification is sent for the superseded schedule; already-recorded history remains available.
5. **Given** a schedule falls in a daylight-saving transition or the user changes time zone, **When** occurrences are displayed and delivered, **Then** the displayed local time and chosen schedule remain unambiguous and consistent.

### User Story 4 — Manage appointments and appointment reminders (Priority: P1)

As a patient, I can record upcoming appointments, update or cancel them, and receive reminders using my selected time zone and channels.

**Why this priority:** Appointments are a distinct, named Phase 2 workflow and must share the same reliable notification experience.

**Independent Test:** Create an appointment, verify it in the upcoming list and reminder history, update it, then cancel it and confirm no future notification is sent for it.

**Acceptance Scenarios:**
1. **Given** an appointment with a title/provider (when supplied), date/time, and time zone, **When** the patient saves it, **Then** it appears in the patient’s upcoming appointments.
2. **Given** an appointment, **When** the patient changes its time or cancels it, **Then** future reminders reflect the updated or cancelled appointment and the patient can see the resulting state.
3. **Given** an appointment reminder is due, **When** a configured delivery channel is available, **Then** the occurrence is delivered at most once as a logical notification, including across restart and retry.
4. **Given** an appointment time is invalid or ambiguous, **When** the patient tries to save it, **Then** the user is asked to correct/resolve the time and no unintended appointment is saved.

### User Story 5 — Choose notification channels and receive retry-safe notices (Priority: P1)

As a patient, I can control whether reminders use in-app, email, or browser notifications and see whether an attempted delivery succeeded.

**Why this priority:** Delivery is a core outcome of scheduling, and patients need control over personal health information shared through each channel.

**Independent Test:** Enable and disable each channel, trigger a test/due occurrence, and verify both channel preference and the visible delivery outcome.

**Acceptance Scenarios:**
1. **Given** a patient opens preferences, **When** they enable or disable email, in-app, or browser notifications, **Then** the saved choice is visible and future delivery follows it.
2. **Given** a channel is disabled or email has not been opted into, **When** a reminder becomes due, **Then** no message is sent through that channel.
3. **Given** email is enabled and a delivery attempt succeeds, **When** the patient views the notification, **Then** the notice identifies the reminder/appointment without exposing unnecessary sensitive health detail in the email subject or preview.
4. **Given** a delivery attempt fails temporarily, **When** a retry is made, **Then** the patient can see a pending/retried/final outcome and does not receive duplicate successful messages for one occurrence.
5. **Given** email is unavailable or not configured, **When** the patient saves email preferences or a due reminder is processed, **Then** the unavailable state is clearly explained and other enabled channels remain usable.

### User Story 6 — Use browser and in-app notification support (Priority: P2)

As a patient, I can see due and recent reminders in the application and optionally receive a browser notification when the browser supports and permits it.

**Why this priority:** In-app notification visibility is dependable across browsers; web push adds convenience but is explicitly conditional on feasibility and permission.

**Independent Test:** Trigger an in-app notification; separately test a supported browser with permission granted, permission denied, and unsupported push capability.

**Acceptance Scenarios:**
1. **Given** an enabled in-app channel, **When** a reminder becomes due, **Then** it appears in the patient’s notification view with a link to the related care item.
2. **Given** a supported browser and the patient grants notification permission, **When** web push is available, **Then** a due reminder may be delivered through that channel without bypassing the patient’s preference.
3. **Given** the browser denies permission, does not support push, or push is not deployably feasible, **When** the patient views notification preferences, **Then** the limitation is explained and in-app plus other available channels remain usable.

### User Story 7 — Grant and revoke caregiver access (Priority: P1)

As a patient, I can approve a caregiver’s access to selected care information and revoke it later.

**Why this priority:** Caregiver access involves sensitive records and must remain under the patient’s control.

**Independent Test:** Approve a caregiver, verify only approved information is available, revoke access, then repeat access attempts from both the UI and direct requests.

**Acceptance Scenarios:**
1. **Given** a patient initiates a caregiver relationship, **When** the caregiver has not yet been approved, **Then** no patient care information is visible to that caregiver.
2. **Given** a pending caregiver request, **When** the patient explicitly approves it, **Then** the caregiver can see only the patient information and capabilities the patient approved.
3. **Given** an approved caregiver, **When** the patient revokes access, **Then** subsequent attempts—including direct requests and already-open sessions—are denied for the revoked scope.
4. **Given** a caregiver attempts to access or change another patient’s record, or an unapproved/revoked record, **When** the request is made, **Then** access is denied and the affected patient’s data is not disclosed.
5. **Given** a caregiver asks for a care change, **When** no explicit capability permits that change, **Then** it is not applied; any permitted change still follows the patient’s preview-and-confirm requirement.

### User Story 8 — Review reports, change settings, and export personal data (Priority: P2)

As a patient, I can understand upcoming and past reminder/appointment activity, review important account activity, update preferences, and export my own information.

**Why this priority:** Visibility and portability support safe daily use and informed control over personal data.

**Independent Test:** Create representative reminder/appointment and delivery events, inspect reports/audit history, change settings, and export; verify the export is limited to the authenticated patient.

**Acceptance Scenarios:**
1. **Given** a patient selects a period, **When** they view a report, **Then** it summarizes that patient’s scheduled occurrences, appointments, and notification outcomes for that period.
2. **Given** a reminder notification was delivered, **When** a report is shown, **Then** it distinguishes notification delivery from medication-taking/adherence; the system does not claim a dose was taken without an explicit patient-recorded confirmation.
3. **Given** a patient reviews the audit/activity view, **When** account, reminder/appointment, notification preference, caregiver approval, or revocation events exist, **Then** the patient can identify the event, time, and outcome without seeing another user’s private information.
4. **Given** a patient changes a supported account or notification setting, **When** they save it, **Then** the setting is confirmed and applied to subsequent behavior.
5. **Given** an authenticated patient requests an export, **When** it is prepared, **Then** it includes that patient’s supported personal records and preferences and excludes other users’ records and secrets.
6. **Given** an unauthenticated user or a different patient requests a report, audit entry, setting, or export, **When** access is attempted, **Then** the request is denied without disclosing protected data.

## Requirements

### Functional Requirements

- **REQ-001 — Existing account journey:** Preserve registration, login, invalid-login feedback, and logout as described in BASE-02 through BASE-05; a successfully authenticated patient reaches a working care workspace rather than an unimplemented destination.
- **REQ-002 — Deterministic local-first assistant:** Support the stated reminder/appointment tasks without requiring a paid AI service or sending patient prompts/records to an unapproved external AI service.
- **REQ-003 — Preview and confirmation:** Before the assistant applies a change, show the user the exact proposed care item and values; apply only after explicit confirmation. Cancelled, expired, ambiguous, or unsupported requests produce no change.
- **REQ-004 — Medication reminders:** Let an authenticated patient create, view, update, pause, and remove medication reminder schedules and identify the time zone used.
- **REQ-005 — Durable occurrences:** Make each scheduled reminder and appointment occurrence and its visible processing/delivery outcome recoverable after application restart; occurrence claims must remain safe under concurrent processing, and repeated processing or retries must not create duplicate logical occurrences or duplicate successful channel delivery.
- **REQ-006 — Appointments:** Let an authenticated patient create, view, update, and cancel appointments, including their scheduled time zone, and align future reminders with the current appointment state.
- **REQ-007 — Email preferences and delivery:** Make email delivery explicitly user-configurable; honor disable/opt-out before sending; expose delivery outcomes; use privacy-conscious content that does not unnecessarily reveal sensitive information.
- **REQ-008 — Retry behavior:** Recover from temporary delivery failure with bounded, visible retry/failure outcomes, without treating an unconfirmed attempt as successful or producing duplicate successful delivery.
- **REQ-009 — Browser notifications:** Provide in-app notification history for enabled occurrences. Offer browser/web push only when the browser, permission, and deployment support it; clearly preserve usable alternatives if it is unavailable.
- **REQ-010 — Caregiver consent:** Require patient approval before caregiver data access, make approved scope understandable, and provide patient-controlled revocation.
- **REQ-011 — Authorization:** Enforce ownership and caregiver scope in the service handling every protected user action; UI hiding alone is insufficient. A direct, stale, or cross-user request must not bypass approval or revocation.
- **REQ-012 — Reports and audit:** Provide patient-scoped reports for reminder/appointment activity and delivery outcomes, plus a reviewable history of important changes and caregiver consent events. Do not equate delivery with medication adherence.
- **REQ-013 — Settings:** Let a patient review and change the time zone and supported notification preferences and receive clear feedback when a channel cannot be configured.
- **REQ-014 — Export:** Provide an authenticated export of the requesting patient’s supported personal data and preferences without including another user’s information, credentials, or secrets.
- **REQ-015 — Privacy and safety:** Do not expose protected health details to unauthorized users or unnecessary notification previews. Do not execute arbitrary commands or scripts in response to assistant input.
- **REQ-016 — CSRF:** Keep cross-site request forgery protection active for state-changing browser interactions. A state-changing request without a valid anti-forgery token must be rejected; do not make a blanket exemption to accommodate new forms.
- **REQ-017 — Additive data change:** Existing accounts and records remain usable after upgrade; schema/data changes must be additive and must not drop or rewrite existing user data without an explicitly approved migration.
- **REQ-018 — User experience:** Provide reachable patient-facing dashboard, medicine/reminder, appointment, caregiver, notification/preferences, report/audit, and export entry points; distinguish unavailable/empty/error states from successful actions.
- **REQ-019 — Verification:** Provide automated evidence for the acceptance scenarios, including ownership and revocation denial, preview cancellation, occurrence restart/retry/idempotency, notification opt-out, and export isolation; retain existing account regression coverage.
- **REQ-020 — Documentation:** Explain user-visible reminder and appointment behavior, notification configuration/limitations, caregiver consent and revocation, data export, and privacy/safety limitations in documentation available with the application.
- **REQ-021 — Accessible UI:** Updated patient-facing journeys must meet the user-selected WCAG 2.1 AA target, remain usable on mobile and desktop using the established responsive behavior, and support the latest two major versions of Chrome, Firefox, Safari, and Edge. Preserve any currently supported locales; do not claim new locale support without evidence.

### Key Entities

- **Patient account:** The authenticated person who owns care records, settings, reports, and exports.
- **Medicine reminder:** A patient-owned instruction and recurrence schedule for a medicine-related reminder.
- **Scheduled occurrence:** One distinct expected reminder/appointment notification instance with a scheduled local time and visible processing/delivery outcome.
- **Appointment:** A patient-owned event with date/time and optional provider/details and reminder schedule.
- **Notification preference:** A patient’s opt-in/opt-out choices for available delivery channels and time-zone-related display behavior.
- **Notification attempt/outcome:** A channel-specific attempt and user-visible result associated with one occurrence.
- **Caregiver relationship:** A patient-approved, scoped, revocable relationship between a patient and another authenticated person.
- **Audit event:** A user-visible record of a significant patient/account/consent/care-setting action and its outcome.
- **Export package:** A patient-scoped copy of supported personal records and preferences.
- **Assistant proposal:** A deterministic, reviewable proposed change that remains unapplied until confirmed.

## Edge Cases

- An account submits duplicate email registration with different casing or surrounding spaces.
- A reminder or appointment coincides with a daylight-saving gap/overlap, or its owner changes time zone.
- The application stops during occurrence processing, a notification provider is temporarily unavailable, or retries overlap with a restart.
- The user edits/pauses/cancels a reminder or appointment after an occurrence has been created but before delivery.
- The user disables email/browser notifications after a preview or while an occurrence is pending.
- Browser notifications are unsupported, blocked, or permission is withdrawn.
- A caregiver is revoked while signed in or attempts access through a bookmarked/direct request.
- Assistant input is ambiguous, unsupported, or asks for an arbitrary system action.
- Export/report generation encounters no data, large data, or sensitive caregiver-linked records.
- A deployment upgrades an existing database; old accounts must remain usable and no destructive migration is permitted.

## Assumptions and Scope Decisions

- The registered account remains the patient’s primary private workspace; existing role values do not imply that doctor-facing workflows already exist.
- Caregiver access defaults to no access until patient approval. Approved scope is limited to the information/capabilities the patient approves; absent a separate explicit permission, caregivers cannot mutate the patient’s care records.
- In-app notifications are the baseline browser-independent experience. Email requires explicit opt-in; web push is conditional as requested and must degrade clearly when not feasible.
- Reports describe recorded schedules, occurrences, appointment information, and delivery outcomes. They do not infer medication ingestion or clinical outcomes.
- Reasonable, reversible defaults are preferred where the brief is silent. No unresolved question blocks planning; this follows the user’s instruction to continue without a clarification gate.
- Resolved UI direction from clarification is React 18 and Tailwind CSS, with existing visual tokens/breakpoints, WCAG 2.1 AA, mobile-first layouts, current/previous major evergreen browsers, and existing locale behavior. The current project inventory is server-rendered Thymeleaf and has no separate frontend package; architecture must assess the transition without silently substituting a target. This product artifact does not select dependency coordinates or versions beyond the user-provided direction.
- The clarification form did not provide screenshots and left a separate backend target-version question unanswered. The existing project framework/runtime is documented in `tech-stack.md`; do not infer a new backend target from the skipped answer. The user expressly directed the team to continue using conservative, reversible defaults.
- The API-contract answer is “must preserve”; preserve the observed existing browser routes/form outcomes while adding the requested workflows. No independent JSON API contract was identified in the research inventory.

## Success Criteria

- **SC-001:** All existing account behaviors BASE-02 through BASE-05 pass regression checks, and successful sign-in reaches a usable patient workspace.
- **SC-002:** 100% of assistant-proposed care mutations in acceptance tests require explicit confirmation; cancel, expiration, and ambiguous input produce zero persisted changes.
- **SC-003:** In restart, concurrent-processing, and retry scenarios, one scheduled occurrence results in no more than one logical occurrence and no more than one successful delivery per enabled channel.
- **SC-004:** Every temporary delivery failure tested ends in a visible pending/retried/succeeded/failed state; disabled channels have zero delivery attempts.
- **SC-005:** 100% of tested unapproved, revoked, cross-patient, and otherwise out-of-scope caregiver requests are denied, including direct requests after revocation.
- **SC-006:** Every tested unauthenticated or cross-patient report, audit, settings, and export request is denied without returning protected data.
- **SC-007:** 100% of tested state-changing browser requests lacking a valid anti-forgery token are rejected.
- **SC-008:** An upgrade test retains all pre-existing account records and allows an existing patient to sign in after additive changes.
- **SC-009:** Reports and exports match the authenticated patient’s data; reports never present notification delivery alone as proof that a medicine was taken.
- **SC-010:** Every P1 acceptance scenario has automated verification, and the completed implementation is documented with any unavailable conditional browser-push capability clearly identified.
- **SC-011:** All updated in-scope patient journeys satisfy the selected WCAG 2.1 AA target and work at mobile and desktop layouts in the latest two major versions of Chrome, Firefox, Safari, and Edge; existing supported locales are retained.

## Phase 2 Brief Traceability

| User brief item | Acceptance coverage |
|---|---|
| Local-first deterministic assistant; preview and confirm | REQ-002–003, Stories 2, SC-002 |
| Persistent reminder/appointment occurrences; restart-safe scheduler, safe claims/idempotency/retries | REQ-004–006, REQ-008, Stories 3–4, SC-003–004 |
| Email delivery and preferences | REQ-007–008, Story 5, SC-004 |
| Browser notifications, in-app and web push if feasible | REQ-009, Story 6, SC-010 |
| Caregiver approval/revocation; authorization enforced in services | REQ-010–011, Story 7, SC-005 |
| Reporting/audit/settings/export | REQ-012–014, Story 8, SC-006, SC-009 |
| UI updates and accessibility | REQ-018, REQ-021, Stories 1–8, SC-011 |
| Security/privacy; preserve CSRF; no arbitrary commands | REQ-015–016, Stories 2, 5, 7–8, SC-002, SC-005–007 |
| Comprehensive tests; documentation | REQ-019–020, SC-010 |
| Additive-only migrations | REQ-017, SC-008 |
| No paid AI; do not make unsupported dependency choices | REQ-002 and the scope decision above |

## Test Results

- Task-local command: not run — this PM task updates acceptance documentation only; application test execution belongs to downstream validation.
- Upstream command: `& 'C:\Users\vishv\.maven\maven-3.9.15\bin\mvn.cmd' -B clean verify` (reported by `t1.1-backend.md`).
- Upstream result: `BUILD SUCCESS`; passed 7, failed 0, errors 0, skipped 0.
- Live CSRF verification reported by `t1.1-backend.md`: 7 expected HTTP observations passed, including readiness, tokenless POST rejection, and token-bearing authentication/registration/logout behavior.
