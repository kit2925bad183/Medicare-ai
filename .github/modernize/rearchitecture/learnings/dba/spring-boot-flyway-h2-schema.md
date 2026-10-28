# Spring Boot Flyway and H2 Schema Startup

Spring Boot 4 needs the Flyway starter for migration autoconfiguration, and H2 identifier casing can affect Hibernate schema validation.

## What Happened

In MediCare AI task t1.1.1, the project had Flyway core and the MySQL module but no Boot Flyway starter, so a migration file alone did not run. Adding `spring-boot-starter-flyway` enabled the runner. The default H2 URL also had `DATABASE_TO_UPPER=false`; Flyway created the lower-case table, but Hibernate `ddl-auto: validate` did not discover it. Removing that option allowed migration, validation, and JPA persistence to pass. Spring Boot's 4.1.2 docs specify the Flyway starter for H2 and a database-specific module for MySQL.

## Takeaway

For Spring Boot 4 projects, verify the Boot Flyway integration starter is present rather than assuming `flyway-core` alone enables autoconfiguration. Keep the database-specific module for each non-embedded database. When schema validation reports a missing H2 table despite Flyway logging successful DDL, check H2 identifier settings and verify with the actual application URL before changing entity mappings. Respect the configured baseline version when choosing migration numbers, and keep Hibernate validation enabled.

## History

- 2026-10-27 (MediCare AI/t1.1.1): initial
