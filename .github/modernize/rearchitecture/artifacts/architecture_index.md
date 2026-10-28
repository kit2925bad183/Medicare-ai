# MediCare AI — Architecture Implementation Guide

This index is not the full contract. Do not implement from this file alone; follow the artifact paths below.

## Current Analysis Boundary

This is the current runnable application after the CSRF and startup/schema remediation. It has three production entry units only: application bootstrap/migration, identity web, and security/session. Phase 2 reminder/appointment scheduling, delivery, caregiver, reporting, export, settings, and assistant flows are not represented as existing units because no production implementation exists for them yet. The design phase should define new units from those requirements; do not infer them from navigation links.

## Global Artifacts

- `unit_graph.yaml` — all implementation workers; exact production entrypoints, source anchors, dependencies, public signatures, dynamic entrypoints, and shared references. Find the assigned unit first.
- `migration_boundary.yaml` — all workers modifying existing code; implementation scope is `must_rewrite`, not `source_anchors`. Current `must_rewrite` is empty because the CSRF correction is already present and upstream runtime-verified; Phase 2 feature implementation adds new units and additive migrations.
- `wire_contracts.yaml` — workers touching browser routes/forms; filter rows by `unit`, preserve listed methods, paths, input names, response/redirects, error behavior, and CSRF requirements.
- `shared_modules.yaml` — workers touching shared identity persistence; filter by `used_by_units`; only access listed fields/methods and preserve existing enum/user semantics.
- `cross_unit_state.yaml` — workers handling principal/session state; filter flows by writer/reader unit. Current file has no statically paired cross-unit flow; `CurrentUserService` is not currently invoked by an entry unit.
- `project-structure.md`, `tech-stack.md`, and `data-model.md` — current architecture, POM/framework facts, persistence configuration, and the sole current entity.
- `seams.yaml` — omitted: no deliberate or inferred partial-migration cut point was found for this same-runtime feature addition.

## Implementation Guide

### Global artifacts — required for every implementation unit

- `unit_graph.yaml`
  - Use for: boundaries, entrypoints, public signatures, dependencies, dynamic Spring discovery, and shared-ref subset names.
  - How to consume: find your unit and follow only its anchors, dependency edges, and `shared_refs`.
- `migration_boundary.yaml`
  - Use for: defining modifications to current production files.
  - How to consume: `must_rewrite` is empty for already-remediated existing code; new Phase 2 feature files are separately allocated by design/tasks. `source_anchors` are evidence and are NOT rewrite targets.
- `wire_contracts.yaml`
  - Use for: externally visible HTTP method/path/form fields/auth/CSRF/redirect/error contracts.
  - How to consume: filter `contracts` where `unit` equals your assigned unit; do not broaden auth/security exemptions.
- `shared_modules.yaml`
  - Use for: cross-unit entity/repository/enum fields and handling guidance.
  - How to consume: filter `modules` where `used_by_units` includes your unit; keep `shared_refs[].used_fields` within the module field whitelist.
- `cross_unit_state.yaml`
  - Use for: implicit session/principal state.
  - How to consume: filter flows where `writer.unit` or `reader.unit` matches your unit; any non-`matched` flow requires runtime confirmation.
- `project-structure.md`, `tech-stack.md`, `data-model.md`
  - Use for: whole-project package/layout, declared technologies/config, entity and migration baseline.
  - How to consume: read as current-state evidence, not as target design or a feature-completeness claim.

### Unit: `application-bootstrap`

- External trigger: Java `main()` / Spring Boot context startup.
- Must read:
  - `units/application-bootstrap/behavior.yaml` — synchronous startup, additive V2 migration side effect, and startup-failure behavior.
  - `units/application-bootstrap/bindings.yaml` — Spring Boot scanning, Flyway discovery, datasource/JPA/migration configuration.
  - `units/application-bootstrap/unit_decomposition.yaml` — optional split candidates only; `commit: false`.
- Relevant global rows: current implementation boundary in `migration_boundary.yaml`; no shared-module or cross-unit-state rows currently touch this unit.
- Before DONE report: artifact paths read, migration/schema preservation evidence, relevant startup/build evidence, and database engine/persistence behavior actually exercised.

### Unit: `identity-web`

- External trigger: `GET /`, `GET /login`, `GET /register`, `POST /register`.
- Must read:
  - `units/identity-web/behavior.yaml` — redirects, registration effects/transaction, validation branches, and errors.
  - `units/identity-web/bindings.yaml` — MVC, Bean Validation, Thymeleaf forms, active CSRF tokens.
  - `units/identity-web/unit_decomposition.yaml` — optional split candidates only; `commit: false`.
- Relevant global rows: `wire_contracts.yaml` where `unit: identity-web`; `shared_modules.yaml` where `used_by_units` includes `identity-web`; `cross_unit_state.yaml` flows touching `identity-web`; `migration_boundary.yaml` for existing-code scope.
- Before DONE report: paths read, preserved registration/page contracts, CSRF behavior for writes, unresolved routes/errors, and runtime verification.

### Unit: `security-session`

- External trigger: Spring Security-managed `POST /login` and `POST /logout`.
- Must read:
  - `units/security-session/behavior.yaml` — route authorization, authentication redirects, logout invalidation, remember-me/session behavior, and verified CSRF contract.
  - `units/security-session/bindings.yaml` — filter chain, default CSRF filter, DAO provider, session-cookie settings.
  - `units/security-session/unit_decomposition.yaml` — optional split candidates only; `commit: false`.
- Relevant global rows: `wire_contracts.yaml` where `unit: security-session`; `shared_modules.yaml` where `used_by_units` includes `security-session`; `cross_unit_state.yaml` flows touching `security-session`; `migration_boundary.yaml` for existing-code scope.
- Before DONE report: paths read, preserved auth/session behavior, CSRF outcomes for every state-changing request, authorization enforcement location, and runtime/build evidence.

## Known Source Constraints

- CSRF is active by default in the current filter chain and was verified with live tokenless 403/token-bearing requests after the application became bootable; see dependency artifact `t1.1-backend.md`.
- The default datasource is in-memory H2. It is not durable across process restarts; configure and verify a persistent datasource before claiming restart-safe occurrence/delivery behavior.
- `V2__create_users_table.sql` is additive and was applied on H2; the upstream verification did not establish MySQL migration compatibility.
- `GET /` redirects to `/dashboard`, while the target route/template is absent. Shared navigation links to several absent feature pages.
- The current URL policy is public-vs-authenticated only, and no existing business service enforces role/ownership/caregiver authorization.
- Existing production source contains no scheduler, durable occurrence records, safe delivery claims, idempotency/retry model, email transport, browser/web-push implementation, audit/export/settings backend, or assistant preview/confirm flow.

## Required Completion Evidence

For each assigned unit, report exact artifact paths consumed; map preserved `must_preserve` behavior and `must_appear_in_target` bindings to implementation evidence; list unresolved/deferred contracts; and provide applicable tests/build/runtime evidence. Do not claim Phase 2 coverage from these current-state architecture artifacts alone.
