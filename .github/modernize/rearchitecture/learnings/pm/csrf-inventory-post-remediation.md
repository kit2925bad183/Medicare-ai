# CSRF Inventory After Remediation

When re-running a feature inventory after a security remediation, record the verified behavior as part of the current baseline and keep it as an acceptance requirement for new flows.

## What Happened
During MediCare AI task t2, the prior inventory listed CSRF as an open gap. The upstream verification later showed tokenless login, registration, and logout POSTs rejected and valid token-bearing requests accepted. Updating the inventory also revealed stale claims that no tests or migrations existed: the current state included a V2 users migration and seven passing tests.

## Takeaway
Reconcile prior inventories with the latest verification artifacts. Distinguish resolved baseline behavior from remaining feature gaps, preserve the required behavior in acceptance criteria, and explicitly label upstream test evidence rather than implying the PM task ran the suite.

## History
- 2026-10-27 (MediCare AI/t2): initial
