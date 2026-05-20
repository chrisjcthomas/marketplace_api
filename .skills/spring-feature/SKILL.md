---
name: spring-feature
description: Use when adding or modifying a small Java 25 Spring Boot backend feature, endpoint, service method, DTO, repository behavior, database change, or cloud-readiness improvement.
---

Follow this workflow strictly:

## 1. Understand the Feature

Restate the feature in plain English.

Identify:
- user-facing behavior
- backend behavior
- business rule
- expected API input
- expected API output
- failure cases

If the requirement is unclear, make a reasonable assumption and state it clearly. Do not block unless the missing detail would break the implementation.

## 2. Inspect the Current Project First

Before changing code, inspect the existing structure.

Identify affected layers:
- controller
- request DTO
- response DTO
- service
- repository
- entity/domain model
- mapper/helper
- exception handling
- validation
- database migration
- tests
- configuration
- documentation

Do not invent files or packages without checking the current project conventions.

## 3. Explain the Concepts Before Coding

Briefly explain the Java and Spring concepts involved.

Include only concepts relevant to the change, such as:
- controller vs service vs repository
- DTO vs entity
- dependency injection
- validation
- transaction boundaries
- JPA relationship mapping
- exception handling
- HTTP status codes
- database migration
- environment variables
- Docker/cloud configuration

For Java 25, prefer clear production-style code over flashy syntax. Use modern Java features only when they improve readability.

## 4. Define Acceptance Criteria

Write acceptance criteria before implementation.

Include:
- successful request behavior
- invalid request behavior
- database state changes
- expected HTTP status codes
- test expectations
- documentation updates if needed

Example:

- Creating an order returns 201 Created.
- Creating an order fails with 404 if the product does not exist.
- Creating an order fails with 400 if requested quantity is invalid.
- Product stock decreases only when the order is successfully created.
- Order creation is transactional.

## 5. Plan the Smallest End-to-End Slice

Implement the smallest working version first.

Avoid:
- unnecessary abstractions
- premature microservices
- extra dependencies
- unrelated refactors
- fake enterprise complexity

Prefer:
- simple layered architecture
- readable code
- narrow feature scope
- one working vertical slice

## 6. Write or Update Tests

Add tests when possible before or alongside implementation.

Prioritize:
- service unit tests
- controller/API tests
- repository tests only when query behavior matters
- integration tests for critical flows

At minimum, test:
- happy path
- validation failure
- not-found failure
- important business rule failure

## 7. Implement the Feature

Rules:
- Keep business logic out of controllers.
- Keep persistence details out of controllers.
- Do not expose JPA entities directly from APIs.
- Use request and response DTOs.
- Validate input using Bean Validation where appropriate.
- Use meaningful exceptions and global exception handling.
- Use `@Transactional` for multi-step database state changes.
- Do not hardcode secrets, URLs, credentials, or cloud values.
- Do not add dependencies unless necessary.

## 8. Verify Locally

Run the strongest available verification command:

```bash
./mvnw test
```

If appropriate, also run:

```bash
./mvnw verify
./mvnw spring-boot:run
```

If Docker/cloud files changed, verify with:

```bash
docker compose up --build
```

If verification cannot run, explain exactly why and what command the user should run locally.

Never mark the feature complete without verification or an honest verification limitation.

## 9. Explain the Result

Summarize:

- feature completed
- files changed
- tests added or updated
- commands run
- command results
- assumptions made
- what could break
- what the user should study next

## 10. Interview Readiness Check

End with a short explanation the user can say to a senior engineer.

Format:

```text
Interview explanation:
"I implemented X by doing Y. The main design choice was Z. I kept A out of B because C. The main failure cases are D and E. If scaling this later, I would improve it by F."
```
