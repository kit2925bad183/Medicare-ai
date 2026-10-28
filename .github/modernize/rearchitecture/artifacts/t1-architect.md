# t1 — Current Architecture Re-analysis

## Summary

Refreshed the current-state architecture after the application became bootable and CSRF enforcement was restored. The production graph remains a single Spring Boot/Thymeleaf application with three externally triggered units: bootstrap, identity web, and security/session. CSRF is active and upstream runtime-verified. Flyway V2 now additively creates the existing `users` table. Phase 2 feature domains remain absent from production source and must be designed as new units, not inferred from navigation links.

## Key Current-State Constraints

- **HIGH:** the default datasource remains in-memory H2; this does not meet process-restart durability for future occurrence/scheduler state. A persistent datasource must be selected and verified before claiming restart-safe delivery.
- Existing source has no durable reminder/appointment occurrence records, scheduler claim/idempotency/retry mechanism, email/web-push delivery, caregiver authorization, audit/report/export/settings services, or assistant preview/confirm flow.
- Current route authorization is public-vs-authenticated; no service-level ownership or caregiver-consent authorization is implemented.
- `V2__create_users_table.sql` was applied and verified on H2; MySQL behavior was not verified in the upstream task.
- `GET /` redirects to absent `/dashboard`; several shared navigation links also have no current page/controller.

## Deliverables

- [architecture_index.md](./architecture_index.md) — implementation guide and per-unit navigation.
- [project-structure.md](./project-structure.md) — current modules/packages, layers, entrypoints, and product type.
- [tech-stack.md](./tech-stack.md) — POM-declared stack and runtime/configuration facts.
- [data-model.md](./data-model.md) — current `User` entity, `Role`, and V2 schema evidence.
- [unit_graph.yaml](./unit_graph.yaml) — current externally triggered units and source anchors.
- [migration_boundary.yaml](./migration_boundary.yaml) — current existing-code scope; no current source rewrite remains required.
- [wire_contracts.yaml](./wire_contracts.yaml) — login, registration, logout, and root redirect contracts.
- [shared_modules.yaml](./shared_modules.yaml) — shared identity entity/repository/role field whitelist.
- [cross_unit_state.yaml](./cross_unit_state.yaml) — no current statically paired cross-unit state flow.
- `units/application-bootstrap/` — startup, migration, and runtime binding details.
- `units/identity-web/` — current authentication/registration behavior and forms.
- `units/security-session/` — active CSRF, route policy, authentication, and session details.
- `seams.yaml` intentionally omitted; no partial-migration seam was found.

## Upstream Artifacts Consumed

- `.github/modernize/rearchitecture/artifacts/t1.1-backend.md` — current booted-app, Flyway V2, and live CSRF verification; supersedes the earlier blocked-startup state.
- `.github/modernize/rearchitecture/board.md` — task sequencing and Phase 2 scope; read-only.
- Existing `artifacts/t1-architect.md` and architecture artifacts — compared prior source inventory to the repaired current state; refreshed stale CSRF, migration, startup, test-inventory, and default datasource facts.
- `.github/modernize/rearchitecture/learnings/architect/csrf-and-worktree-baseline.md` — preserved CSRF verification expectations, current POM precedence, and narrow Git-worktree handling.
- `context.md`, `decisions.md`, `team/architect/log.md`, and `team/architect/inbox.md` were checked; context/decisions were absent, log/inbox were present.
- No `*-fanout.md` topology artifact was found and no topology path was supplied.

## Evidence Mapping

- `t1.1-backend.md#Runtime Verification` → updated `units/security-session/behavior.yaml`, `wire_contracts.yaml`, and `tech-stack.md` reflect active CSRF and live outcomes (tokenless unsafe POSTs → 403; valid-token forms proceed).
- `t1.1-backend.md#Runtime Verification` + `#Test Results` → updated bootstrap/data-model artifacts record successful packaged startup, Flyway version 2 on H2, and the 7-test verification result as upstream evidence, not as a test rerun by this task.
- `pom.xml#parent,properties,dependencies` and `application.yml#datasource,jpa,flyway` → `tech-stack.md`, `project-structure.md`, `data-model.md`, and `units/application-bootstrap/bindings.yaml`.
- `V2__create_users_table.sql#table-definition` + `User.java#entity-fields` → `data-model.md` records the additive users schema and unchanged entity mapping.
- `SecurityConfig.java#filter-chain` + `CsrfProtectionIntegrationTest.java#unsafe-POST-regression` → `units/security-session/behavior.yaml`, `units/security-session/bindings.yaml`, and `wire_contracts.yaml`.
- `IdentityController.java#routes` + `IdentityService.java#registration-transaction` → `units/identity-web/behavior.yaml`, `units/identity-web/bindings.yaml`, and `wire_contracts.yaml`.
- Measured source inventory (11 production Java files, 368 Java lines, 7 main resource files, 3 Java test classes) → `project-structure.md`.

## Verification Scope

- Architecture artifact completeness and source-reference consistency were checked after writing.
- No project tests or build were run by this architecture-only task. Upstream `t1.1-backend.md` reports `mvn -B clean verify`: 7 passed, 0 failed, 0 errors, 0 skipped; its live packaged app probes passed.
- Context and decisions files were absent; the board was read-only.

## Findings

- 1 HIGH: default H2 memory database is not restart durable for the brief's persistent scheduler/occurrence requirements.
- 0 CRITICAL findings.
