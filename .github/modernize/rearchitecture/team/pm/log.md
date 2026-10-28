## [t2] Inventory existing behavior and define Phase 2 acceptance
- Codebase/domain discoveries: The current runnable source contains authentication and registration only; the shared layout advertises dashboard, medicines, appointments, doctors, and preferences, but no matching domain controllers or views were found. Existing registration validates names/email/password/time zone, while the login and logout flows are the only other implemented end-user journeys.
- Important baseline gap: `SecurityConfig` explicitly disables CSRF, which conflicts with the user's Phase 2 requirement to preserve CSRF protection; the acceptance artifact requires active protection and flags the current setting as a gap.
- Wrong assumptions and corrections: Do not treat visible navigation links as implemented features. Do not equate reminder notification delivery with a patient taking medicine; requirements explicitly keep these outcomes distinct.
- Techniques/patterns worth reusing: Trace existing routes and template form actions before defining parity; separate verified baseline behavior from requested future scope and list absence of a controller/view as a gap rather than an existing capability.
- Learnings consumed: []

## [t2] Re-run inventory after CSRF remediation
- Codebase/domain discoveries: The post-remediation evidence now shows CSRF rejection/acceptance verified on login, registration, and logout, a successfully applied additive V2 users migration, and 7 passing tests. The browser workflow inventory remains otherwise unchanged; the dashboard destination and advertised care destinations are still unimplemented.
- Wrong assumptions and corrections: The prior baseline’s “no tests or migrations” statement was stale after the upstream remediation. Reconcile inventories with current evidence rather than carrying forward old counts or describing resolved CSRF as an open defect.
- Debugging dead-ends and what actually worked: `context.md`, `decisions.md`, `team/pm/inbox.md`, a constitution, and topology fanout were absent. The existing research artifacts and t1.1 verification report were sufficient; direct source exploration was avoided because prebuilt research artifacts were available.
- Techniques/patterns worth reusing: Preserve CSRF as a required acceptance contract even after it passes its remediation gate; report both task-local non-execution and upstream test/runtime evidence accurately.
- Learnings consumed: [pm/distinguish-navigation-from-features]
