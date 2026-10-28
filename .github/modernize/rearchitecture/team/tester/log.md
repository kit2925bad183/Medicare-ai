## [t1.2] Final Maven verification and scoped diff review
- Codebase/domain discoveries: Java 25.0.4.1 and Maven 3.9.15 are available; the focused suite consists of application startup/Jackson (1), CSRF (4), and users migration (2) tests. Both focused tests and `clean verify` passed all 7 tests.
- Wrong assumptions and corrections: the Git worktree root is `C:\`, not the project. `C:\Medicare AI` is untracked by the parent and has no local `.git`; there is no file-level diff baseline.
- Debugging dead-ends and what actually worked: `Get-Date -AsUTC` is unsupported in this PowerShell version; `[DateTime]::UtcNow` works. Scope Git status with `git -C C:\ ... -- "Medicare AI"`; avoid expanding all untracked files from the drive-root repository.
- Techniques/patterns worth reusing: use Surefire XML reports to confirm per-suite test/failure/error/skip counts after Maven commands; distinguish Maven integration coverage from live endpoint and browser E2E evidence.
- Learnings consumed: [architect/csrf-and-worktree-baseline, architect/phase2-additive-runtime-boundary, backend/csrf-thymeleaf-spring-boot4, backend/jackson3-date-serialization-config, dba/spring-boot-flyway-h2-schema]
