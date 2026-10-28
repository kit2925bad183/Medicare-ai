# CSRF with Thymeleaf on Spring Boot 4

Spring Security's default CSRF filter protects unsafe browser methods and Thymeleaf's Spring integration adds hidden CSRF inputs to POST forms rendered with `th:action`.

## What Happened

For MediCare AI task t1.1, the global CSRF disablement was removed. MVC regression coverage confirmed token fields in the login, registration, and shared logout forms, 403 responses for tokenless POSTs, and preserved request outcomes when a test token is present. In the project's Spring Boot 4.1.2 setup, MVC test annotations are provided by `spring-boot-starter-webmvc-test`; its version is managed by the Spring Boot parent.

## Takeaway

Prefer Spring Security's default CSRF configuration rather than endpoint exemptions. Test rendered forms as well as filter behavior: make tokenless requests to each state-changing browser endpoint and confirm a token-bearing request reaches the existing handler/filter behavior. For Boot 4 MVC slice tests, add the managed `spring-boot-starter-webmvc-test` test dependency when MVC test APIs are otherwise absent. After successful authentication, fetch a new CSRF token before testing another unsafe request such as logout; Spring Security invalidates the pre-authentication token as part of session protection.

## History

- 2026-10-27 (MediCare AI/t1.1): initial
- 2026-10-27 (MediCare AI/t1.1): confirmed live auth rotates the CSRF token; use the post-authentication token for logout verification
