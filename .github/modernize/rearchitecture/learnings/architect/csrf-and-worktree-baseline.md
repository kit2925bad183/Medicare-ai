# CSRF and Worktree Baseline

For MediCare AI, preserve CSRF by correcting the explicit Spring Security disablement, and scope all Git inspection to the project because the Git root is its parent drive.

## What Happened

During project `MediCare AI`, task `t1`, source inspection found `SecurityConfig` explicitly disables CSRF although browser forms submit POST requests. The current datasource defaults to in-memory H2, with no Flyway SQL migration in source. `git rev-parse --show-toplevel` resolves to `C:\`; the project itself has no `.git` directory and its source is untracked in the parent worktree. A sibling Java upgrade plan describes an older Java/Spring version than the current `pom.xml`.

## Takeaway

- Inspect CSRF at the filter-chain configuration and verify real state-changing form/fetch submissions; do not infer protection from POST forms or user requirements.
- Confirm actual repository root before status or modification; use project-scoped status and never reset/stage broadly when the worktree includes unrelated sibling content.
- Treat the current POM and source as the local version baseline when generated or adjacent plans conflict; resolve stale artifacts without silently changing dependency coordinates.
- For this feature extension, preserve existing user data and add new behavior/schema rather than rewriting the existing stack.

## History
- 2026-10-27 (MediCare AI/t1): initial
