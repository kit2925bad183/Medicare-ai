# Existing Data Model

## Persisted Entities

### `User` — `users`

Source: `src/main/java/com/medicare/identity/User.java:11-54`.

| Property | Java type | Mapping / constraint |
|---|---|---|
| `id` | `Long` | Primary key; `GenerationType.IDENTITY`. |
| `email` | `String` | Required, unique, max length 254; `@Email`; table constraint `uk_users_email` and column-level unique constraint are both declared. |
| `passwordHash` | `String` | Required; no explicit column name or length. |
| `firstName` | `String` | Required; max length 100. |
| `lastName` | `String` | Required; max length 100. |
| `role` | `Role` | Required; stored as enum name using `EnumType.STRING`, max length 20. |
| `enabled` | `boolean` | Required; Java default is `true`. |
| `createdAt` | `Instant` | Required; initialized and set on persist. |
| `updatedAt` | `Instant` | Required; initialized and refreshed on persist/update. |
| `timeZone` | `String` | Optional DB column, max length 64; Java field defaults to `"UTC"`. |

### `Role`

`src/main/java/com/medicare/identity/Role.java:3-7` declares `PATIENT`, `DOCTOR`, and `ADMIN`. `User.role` stores the enum name, not the ordinal.

## Relationships

- No JPA relationship annotations or relationships between entities were found.
- No other persisted entity class was found under production Java source.

## Persistence Configuration and Migration Evidence

- Default datasource is `jdbc:h2:mem:medicare` with MySQL compatibility flags (`src/main/resources/application.yml:6-11`); this default is process-memory storage, not restart-durable.
- Datasource URL, username, password, and driver can be overridden with environment variables.
- Hibernate uses `ddl-auto: validate`; it does not create missing schema.
- Flyway is enabled with `baseline-on-migrate: true`.
- `src/main/resources/db/migration/V2__create_users_table.sql` creates the `users` table if absent, matching the entity's ID, email, password hash, first/last names, role, enabled state, timestamps, and time-zone column. It is an additive migration; its source comment says it follows configured baseline version 1 for legacy schemas.
- Upstream live evidence reports Flyway applied version 2 on H2; `UserSchemaMigrationIntegrationTest` checks persistence and migration rerun behavior. MySQL migration/runtime behavior was not verified.
- The Phase 2 brief's durable occurrences, reminders, appointments, deliveries, notification preferences, caregiver-consent/audit, reporting/export, and assistant-action data have no current entities or schema tables in the inspected source.
- `IdentityService.register()` is `@Transactional` (`src/main/java/com/medicare/identity/IdentityService.java:18-38`); duplicate lookup and user save execute in that service transaction. No other business transaction was found.

## Key Entity Summary

- `User` — account identifier, password hash, role, enabled state, creation/update timestamps, and time-zone preference.
- `Role` — persisted user authorization category (`PATIENT`, `DOCTOR`, or `ADMIN`).
- No reminder, occurrence, appointment, delivery, notification preference, caregiver consent, audit, report, or assistant-action entity currently exists.
