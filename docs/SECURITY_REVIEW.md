# Security review

Date: 2026-05-20

Scope: repository-wide review of the Spring Boot API, configuration, Docker files, database migration, CI workflow, and deployment documentation.

## Threat model summary

The main assets are user data, business data, product inventory, order/payment state, database credentials, and cloud runtime configuration.

The main trust boundary is the HTTP API. Public clients send JSON into controller methods, DTO validation checks the first layer of input, service methods enforce business rules, repositories persist data through JPA, and PostgreSQL stores the final state.

Security-sensitive invariants:

- Controllers should not expose JPA entities directly.
- Request payloads should be validated before service logic.
- Order creation should be transactional.
- Payment simulation should reject duplicate payments for the same order.
- Production secrets should come from environment variables or secret management.
- Cloud deployment should not expose unauthenticated write endpoints with real data.

## What was checked

- HTTP routes in `src/main/java/com/example/marketplace/controller`.
- DTO validation in `src/main/java/com/example/marketplace/dto`.
- Business rules and transaction boundaries in `src/main/java/com/example/marketplace/service`.
- JPA entities and relationships in `src/main/java/com/example/marketplace/domain`.
- Spring Data repositories in `src/main/java/com/example/marketplace/repository`.
- Global exception handling in `src/main/java/com/example/marketplace/exception`.
- PostgreSQL schema migration in `src/main/resources/db/migration/V1__init_schema.sql`.
- Runtime configuration in `application.yml` and `application-prod.yml`.
- Docker, Docker Compose, GitHub Actions, README, and deployment docs.

## Findings and fixes

### Cloud Run should not default to public access

The deployment guide previously used unauthenticated Cloud Run access. That is fine for a throwaway demo, but this application has no built-in authentication or authorization yet. Public deployment would make every create/update endpoint callable by anyone who can reach the service.

Changed:

- `docs/DEPLOYMENT.md` now uses `--no-allow-unauthenticated`.
- `docs/HOW_DID_I_BUILD_THIS.md` now explains that Cloud Run should stay behind IAM until real auth exists.
- `README.md` now states that unauthenticated deployment should use only throwaway demo data.

Status: fixed in documentation and deployment defaults.

### API input sizes should be enforced before the database

Several string fields relied on database column limits. That is not an injection issue because the app uses JPA repositories rather than string-built SQL, but oversized payloads should still fail at the API validation layer instead of reaching the database.

Changed:

- Added `@Size(max = 255)` to user, business, and product text fields.
- Added `@Size(max = 100)` to order item lists.

Status: fixed.

## Reviewed and not reportable

SQL injection:

No string-built SQL, native queries, or JPQL `@Query` methods were found. Repositories use Spring Data JPA methods.

Path traversal and file access:

No user-controlled filesystem paths, file uploads, archive extraction, or file download endpoints were found.

SSRF:

No user-controlled network clients, callbacks, `RestTemplate`, or `WebClient` calls were found.

Command execution:

No `Runtime.exec`, `ProcessBuilder`, shell execution, or script execution paths were found in application code.

Unsafe deserialization:

No custom `ObjectMapper.readValue`, Java serialization, XML parsing, YAML parsing, or polymorphic deserialization sinks were found in application code.

Secrets in source:

The local Docker Compose password is a development-only default. Production configuration reads database values from environment variables. Deployment docs use Secret Manager for the database password.

Actuator exposure:

Only `health` and `info` are exposed through actuator configuration.

## Residual risks

These are known gaps, not hidden findings:

- The application does not implement authentication or authorization.
- There is no idempotency key for order creation.
- There is no rate limiting.
- There is no audit log for sensitive state changes.
- There are no controller integration tests yet.
- Concurrent inventory updates rely on JPA optimistic locking, but the API does not yet map optimistic-lock failures to a user-friendly conflict response.

For this learning project, those are acceptable if the service is deployed privately or with throwaway data. For production, add application auth, authorization rules, idempotency, rate limiting, audit logs, and stronger API tests.

## Verification

Command:

```powershell
.\mvnw.cmd test
```

Result:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
