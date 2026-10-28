---
schema: clarification-answers/v1
status: submitted
generated_at: "2026-10-27T17:05:48.512Z"
scope: [frontend, backend, generic]
questions_file: clarification-questions.json
---

# Rearchitecture Clarification Answers

## 🖥️ Frontend

- **F1** Which frontend framework and version should Phase 2 use? Should it preserve the existing framework (to be identified during analysis), or do you want a specific target?
  - answer: React 18
  - source: user
- **F2** Which component/UI library should Phase 2 use? Is preserving the existing components and avoiding a new UI dependency preferred?
  - answer: Tailwind CSS
  - source: user
- **F3** Please provide screenshots, screen recordings, or a staging URL for the existing screens that Phase 2 will update. If none are available, say so and identify any key screens or states to preserve.
  - answer: _none_
  - source: skipped (required, no default — blocking gap)
- **F4** What design system, brand tokens, or visual reference should guide the UI updates? You may provide a Figma/Storybook URL or token-file path, or confirm that the current application styling should be preserved.
  - answer: match existing pixel-by-pixel using extracted CSS custom properties
  - source: user
- **F5** Which accessibility standard should the updated UI target?
  - answer: WCAG 2.1 AA
  - source: user
- **F6** Which browsers and versions must support the updated application?
  - answer: modern evergreen (Chrome, Firefox, Safari, Edge — latest 2 major versions)
  - source: user
- **F7** What responsive strategy should the UI updates follow?
  - answer: mobile-first using existing breakpoints detected from codebase
  - source: user
- **F8** Which locales should remain in scope for Phase 2?
  - answer: preserve current locales; keep existing i18n library if present
  - source: user
- **F9** Do you have a preference for client-side state management for the Phase 2 UI?
  - answer: preserve existing pattern if identifiable; otherwise recommend minimal (component state + server-state library)
  - source: user
- **F10** Should Phase 2 preserve the existing routing approach, or do you have a routing preference?
  - answer: use the de-facto router for the chosen target framework
  - source: user

## ⚙️ Backend

- **B1** Which backend framework and version should Phase 2 target? The profile confirms Java/Maven but not the server framework or version. Should the existing framework/version be preserved and identified during analysis, or is a specific target required?
  - answer: _none_
  - source: skipped (required, no default — blocking gap)
- **B2** What API contract policy should apply to Phase 2?
  - answer: must preserve
  - source: user
- **B3** What database/data migration approach should be used? The brief requires additive-only migrations; please confirm that schema changes should be applied in place and additive only, or specify another additive-compatible approach.
  - answer: in-place; additive-only schema migrations
  - source: user
- **B4** Which authentication/authorization mechanism should the implementation retain or integrate with? The brief requires caregiver approval/revocation authorization to be enforced in services.
  - answer: preserve existing auth mechanism; detect from codebase
  - source: user
- **B5** What latency, throughput, availability, or other service-level targets should the expanded application meet?
  - answer: match current production baseline; no regression
  - source: user

## 📋 General

- **G1** What observable outcome defines successful completion? Confirm or refine the brief's success criteria.
  - answer: Deliver the stated Phase 2 capabilities in the existing MediCare AI application, with deterministic preview-and-confirm assistant actions, durable restart-safe and idempotent scheduling, reliable notifications and preferences, service-enforced caregiver authorization, reporting/audit/settings/export, secure and privacy-conscious behavior, comprehensive tests, documentation, additive-only migrations, CSRF preserved, and no arbitrary command execution or paid AI dependency.
  - source: user
- **G2** Confirm or add any features, modules, or behaviors that must remain out of scope. The brief excludes arbitrary command execution and does not require a paid AI service.
  - answer: Arbitrary command execution is out of scope. A paid AI service is not required; do not introduce a paid AI dependency.
  - source: user
- **G3** How should the existing test suite be treated during implementation?
  - answer: must pass
  - source: user
- **G5** Add any additional constraints not already captured above, or accept the provided default.
  - answer: None beyond the decisions listed in this specification.
  - source: user
