## [t1.1] Restored browser CSRF protection
- Codebase/domain discoveries: CSRF was disabled globally; login, registration, and shared logout are POST forms. Thymeleaf with Spring Security emitted the hidden CSRF field in each rendered form once the default filter was enabled.
- Wrong assumptions and corrections: `spring-boot-starter-test` alone does not provide the MVC slice annotations under the project's Spring Boot 4.1.2 modular test setup; added the verified `spring-boot-starter-webmvc-test` starter, with its version supplied by the existing Boot parent.
- Debugging dead-ends and what actually worked: the app-wide MVC slice first failed binding the existing Jackson 2-era date serialization property to Jackson 3. Excluding Jackson auto-configuration in the focused security slice isolated the request/filter/form behavior without changing production config. Full app startup then confirmed the previously identified missing `users` migration prevents live HTTP verification.
- Techniques/patterns worth reusing: exercise the exact Thymeleaf logout fragment in the test template, verify token rendering separately, and test that each state-changing endpoint returns 403 without a token while preserving its prior response flow with a valid token.
- Learnings consumed: []

## [t1.1.1.1] Corrected Jackson 3 startup binding
- Codebase/domain discoveries: Spring Boot 4.1.2 uses Jackson 3.1.7; its `SerializationFeature` no longer includes `WRITE_DATES_AS_TIMESTAMPS`, while default `Instant` serialization remains a quoted ISO-8601 JSON string.
- Wrong assumptions and corrections: an existing Jackson 2 configuration key cannot be carried forward just because its YAML shape is accepted by earlier Boot/Jackson releases; enum membership and runtime binding are authoritative.
- Debugging dead-ends and what actually worked: the existing persistence integration test excludes Jackson auto-configuration, so it could not verify the production startup path. A separate `@SpringBootTest` exercised the full context and Jackson mapper without test-only exclusions.
- Techniques/patterns worth reusing: for configuration-only startup regressions, assert both full-context startup and the externally meaningful serialization result; package and launch the app separately to confirm real startup.
- Learnings consumed: [backend/csrf-thymeleaf-spring-boot4]

## [t1.1] Re-verified CSRF against the running packaged app
- Codebase/domain discoveries: after a successful login, Spring Security invalidates the prior CSRF token; the logout form/request needs a newly fetched token.
- Wrong assumptions and corrections: the token from the anonymous login page should not be reused to validate authenticated logout; the 403 was correct until the current token was fetched.
- Debugging dead-ends and what actually worked: `mvn` was not on PATH, but the verified Maven 3.9.15 installation under the user's Maven directory ran `clean verify`; the packaged JAR then started and live HTTP checks covered tokenless and token-bearing login, registration, and logout.
- Techniques/patterns worth reusing: boot the packaged app on a temporary port, retain one cookie-backed client, fetch a fresh rendered token after authentication, and test both rejection and legitimate flow statuses.
- Learnings consumed: [backend/csrf-thymeleaf-spring-boot4, backend/jackson3-date-serialization-config]
