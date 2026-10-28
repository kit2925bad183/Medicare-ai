## [t1.1.1] Added the users schema migration and verified entity persistence
- The project declared `flyway-core` and `flyway-mysql`, but Spring Boot 4 did not activate migration autoconfiguration without `spring-boot-starter-flyway`; official Spring Boot 4 docs name that starter.
- Versioned the first source migration as V2 because `baseline-on-migrate` is already enabled; `CREATE TABLE IF NOT EXISTS` leaves any legacy users table unchanged and the Hibernate validator remains authoritative for schema compatibility.
- H2's `DATABASE_TO_UPPER=false` caused schema validation to miss the migration-created lower-case table; removing the option from the default H2 URL allows Hibernate validation and repository insert/read to pass.
- The integration test excludes Jackson autoconfiguration to isolate DBA behavior. A real packaged-app launch exposed a separate Jackson 3 property-binding startup blocker, reported to the coordinator/backend.
- No destructive DDL was used; MySQL behavior was not runtime-tested.
- Learnings consumed: [(none)]

## [t1.1.1] Re-verified the users migration after Jackson startup remediation
- The full clean Maven verification passes after the Jackson property correction: the full application startup/serialization test and the migration tests now pass together.
- The migration integration tests intentionally exclude Jackson to isolate schema behavior; a separate full-context test covers Jackson startup. The Maven test log showed Flyway applying V2 during the application context startup.
- No schema or Java source edit was required for this re-verification. MySQL remains untested; all current database verification is H2 in MySQL compatibility mode.
- Learnings consumed: [dba/spring-boot-flyway-h2-schema]
