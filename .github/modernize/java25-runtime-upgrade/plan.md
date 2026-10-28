# Modernization Plan: Java 25 Runtime and Compatibility Upgrade

**Project**: medicare-ai

## Technical Framework

- **Language**: Java; currently configured for Java 21
- **Framework**: Spring Boot 3.3.5 and Spring Framework
- **Build Tool**: Maven
- **Database**: Existing MySQL/H2 dependencies remain unchanged
- **Key Dependencies**: Spring Web, Security, Data JPA, Thymeleaf, Flyway

## Overview

Move the application's runtime and Maven build compatibility to Java 25 LTS.
The assessment also reports that the current Spring Boot and Spring Framework
versions have reached the end of OSS support. Align those managed framework
versions only as needed to provide a supported Java 25-compatible stack and
resolve those findings. Preserve existing application behavior and unrelated
worktree changes.

## Scope and Compatibility Decisions

- Target Java 25 LTS, the version verified for this request.
- The Java upgrade guideline lists Spring Boot 4.x / Spring Framework 7.x as
  the compatible framework line for Java 25. Select a supported compatible
  patch level during implementation.
- Keep Maven and update compiler/build settings and dependencies only when
  directly required for Java 25 or the compatible Spring stack.
- Do not include cloud-readiness remediation, Docker/containerization,
  credential or identity-provider changes, database migrations, or deployment.
- Preserve pre-existing source changes and files; limit edits to those needed
  for runtime/build/framework compatibility.

## Migration Impact Summary

| Scope | Current | Target |
|---|---|---|
| Java runtime | Java 21 | Java 25 LTS |
| Spring stack | Boot 3.3.5 | Compatible supported Boot 4.x / Framework 7.x |
| Build | Maven | Retain Maven; update only for Java 25 compatibility |
| Cloud and data | Existing configuration | No changes |

## Assessment Findings Addressed

- **Java Version is not the latest LTS**
  (`azure-java-version-02000`; optional).
- **Spring Boot Version Has Reached the End of OSS Support**
  (`spring-boot-to-azure-spring-boot-version-01000`; mandatory).
- **Spring Framework Version Has Reached the End of OSS Support**
  (`spring-framework-version-01000`; mandatory).

The findings were checked against the current `pom.xml`: it configures
`java.version` as 21 and uses Spring Boot parent 3.3.5. Assessment incidents
point to this Maven file for the Java and Spring findings.

## Validation and Worktree Preservation

After implementation, run the focused Maven build and the project's existing
automated tests using JDK 25 (for example, `mvn -B clean verify`). Address
failures attributable to the runtime/framework compatibility changes; do not
expand test scope to unrelated functionality. Do not reset, stage, or overwrite
pre-existing worktree content while implementing or validating this plan.
