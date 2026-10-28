# Existing Technology Stack

## Language and Runtime

- Language: Java.
- Maven property: `java.version=25` (`pom.xml`).
- Spring Boot parent: `org.springframework.boot:spring-boot-starter-parent:4.1.2`.
- The upstream runtime verification used Java 25.0.4.1; the POM is the project's declared source of version requirements.
- Packaging: Maven with `spring-boot-maven-plugin`; no Maven Wrapper was found.
- Application coordinates: `com.medicare:medicare-ai:0.0.1-SNAPSHOT`.

## POM-Declared Dependencies

The POM directly declares the following; dependency versions are omitted where managed by the Spring Boot parent and are not independently inferred here:

- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-thymeleaf`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.springframework.boot:spring-boot-starter-actuator`
- `org.springframework.boot:spring-boot-starter-flyway`
- `org.flywaydb:flyway-mysql`
- `com.mysql:mysql-connector-j` — runtime scope.
- `com.h2database:h2` — runtime scope.
- `org.thymeleaf.extras:thymeleaf-extras-springsecurity6`
- `org.springframework.boot:spring-boot-starter-test` — test scope.
- `org.springframework.boot:spring-boot-starter-webmvc-test` — test scope.
- `org.springframework.security:spring-security-test` — test scope.

Build plugins:

- `org.springframework.boot:spring-boot-maven-plugin` — version managed by the parent.
- `org.apache.maven.plugins:maven-surefire-plugin` — version unspecified in the POM; module-path use is disabled.

No new dependency coordinates or versions are proposed by this analysis.

## Server and UI

- Synchronous Spring MVC / servlet-based request processing.
- Thymeleaf server-rendered HTML templates.
- Static CSS and JavaScript. `static/js/app.js` dynamically imports Three.js from `https://cdn.jsdelivr.net/npm/three@0.161.0/build/three.module.js`; the URL is pinned in the existing source.
- Server port: 8080 by default.

## Data and Persistence

- Spring Data JPA and Hibernate.
- Default datasource: H2 in-memory URL with MySQL compatibility mode; environment variables can override URL, username, password, and driver class.
- `spring.jpa.hibernate.ddl-auto=validate`; Flyway is enabled with `baseline-on-migrate=true`.
- One production migration exists: `V2__create_users_table.sql`, which creates the `users` table if absent. Upstream live verification applied it on H2 and reported Flyway current version 2.
- MySQL Connector/J and H2 are declared. `application.yml` specifies H2 dialect despite allowing a datasource URL override; MySQL runtime behavior was not verified by the consumed task artifact.
- Open EntityManager in View is disabled.
- Boot's configured `JsonMapper` is verified by `ApplicationStartupIntegrationTest` to serialize an `Instant` as a JSON string. No custom Jackson serialization property appears in the current `application.yml`.

## Security

- Spring Security form login with `CustomUserDetailsService`, a `DaoAuthenticationProvider`, and BCrypt password encoder.
- Current `SecurityConfig` has no `.csrf().disable()` or CSRF customization; Spring Security's default CSRF filter remains active. The consumed upstream verification reports missing-token 403 responses and token-bearing login/registration/logout behavior on a booted packaged app.
- The authentication route policy is public-vs-authenticated only; no role-specific URL rule is configured in the current filter chain.
- Remember-me is enabled with a literal source key and 1,209,600-second validity.
- Session cookie configuration: HttpOnly true, Secure false, SameSite LAX.
- `spring.security.user.name/password` properties are present in YAML, but current authentication is configured with the database-backed `DaoAuthenticationProvider`; the default-user properties' operational effect was not established here.

## Configuration Inputs

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `DB_DRIVER_CLASS_NAME` override datasource defaults.
- `application.yml` holds server port, Actuator exposure, logging levels, Thymeleaf cache behavior, cookie flags, JPA settings, and Flyway switches.
- `.env.example` contains sample MySQL configuration values; it is not a production secret store.
- No profile-specific YAML file was found in the source resource inventory.

## Verification Context and Constraints

- Dependency artifact `t1.1-backend.md` records packaged startup, CSRF HTTP checks, and `mvn -B clean verify` passing on the repaired application. This architecture task did not rerun tests or build.
- An adjacent Java 25 upgrade plan conflicts with the POM. Use the checked-in POM as the current local version baseline unless the coordinator resolves the stale artifact.
- Parent-managed transitive versions should be established from Maven's effective dependency tree when needed. Verify official metadata before adding any dependency; this analysis makes no dependency/version choices.
