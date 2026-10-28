# Distinguish Navigation from Features

Visible links in a shared application layout may advertise routes that have no working controller or page; verify route handlers and templates before counting a journey as an existing feature.

## What Happened
During MediCare AI task t2, the shared layout advertised dashboard, medicines, appointments, doctors, and preferences, but a source-wide mapping search found only identity routes and the source inventory had no domain controllers or views. Counting the links as delivered functionality would have overstated feature parity.

## Takeaway
For brownfield inventories, report visible navigation separately from verified user journeys. Treat an unimplemented destination as a gap and include a working destination in the acceptance criteria when the task intends to deliver that capability.

## History
- 2026-10-27 (MediCare AI/t2): initial
