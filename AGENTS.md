# Agent Instructions

This project is a learning-first Java 25 / Spring Boot 4 marketplace API. Do not turn small changes into fake enterprise complexity.

Before adding or modifying backend functionality, read and follow:

- `.skills/spring-feature/SKILL.md`

When explaining code to the learner, read and follow:

- `.skills/explain-like-im-learning/SKILL.md`

When changing Docker, environment variables, CI/CD, deployment docs, Cloud Run, Cloud SQL, or production readiness, read and follow:

- `.skills/cloud-deploy-check/SKILL.md`

Default engineering rules:

- Keep the app as one modular monolith until the core behavior is understood.
- Keep business logic in services, not controllers.
- Use DTOs at the API boundary.
- Use Flyway migrations for database schema changes.
- Use transactions for multi-step state changes such as order creation and payment simulation.
- Do not add Kafka, Redis, MongoDB, Kubernetes, Terraform, authentication, or a frontend unless explicitly requested.
- Verify with `mvn test` or `./mvnw test` before claiming a code change works.
