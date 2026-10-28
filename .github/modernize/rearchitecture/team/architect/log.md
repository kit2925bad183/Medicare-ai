## [t1] Existing architecture, persistence, security, and worktree baseline
- The source is a compact single-module Spring Boot/Thymeleaf application: current runtime routes are identity and registration; reminder, appointment, delivery, caregiver, reporting, and settings code is absent.
- H2 is the default in-memory datasource, Hibernate is set to validate, Flyway is enabled, and no migration SQL or source tests were found. Do not assume restart-safe persistence or an existing migration baseline.
- `SecurityConfig` explicitly disables CSRF, while the Phase 2 brief requires CSRF preservation. Authentication is session-based and database-backed; current route authorization is authenticated-vs-public, not service-level role/ownership authorization.
- The project folder is nested under a `C:\` Git root and is not an independent Git repository. Broad root status is noisy and includes unrelated siblings; use project-scoped status and do not stage/reset/clean broadly.
- A prior Java runtime upgrade plan conflicts with the current `pom.xml` versions; use the checked-in POM as the current local evidence and resolve the stale plan separately.
- Initial full-root status emitted unrelated OS permission warnings; narrowing Git status to the project path produced usable evidence. A quoted ripgrep regex was parsed incorrectly by PowerShell; `Select-String` was used for precise source anchors.
- Learnings consumed: none.

## [t1] Current architecture re-analysis after CSRF/startup remediation
- Re-read the current source after database migration and Jackson startup repair; `SecurityConfig` now leaves Spring Security CSRF active, and upstream t1.1 reports live 403/token-bearing request verification.
- V2 additively creates `users` with `IF NOT EXISTS`, but the default datasource remains H2 memory; schema migration by itself does not make scheduler state durable between process restarts.
- Current production source still has only bootstrap, identity web, and security/session entry units; Phase 2 reminders, occurrences, deliveries, caregiver authorization, audit/reporting, settings, push, and assistant flows remain unimplemented.
- Use the current POM for versions; this architecture task did not make dependency choices or rerun build/tests. Upstream t1.1 records 7 passing tests and successful package verification.
- Validation scripts confirmed all 3 units have behavior/bindings/decomposition files, no duplicate anchors, valid shared refs, no unresolved contract targets, and all source anchors exist.
- Learnings consumed: [architect/csrf-and-worktree-baseline].
