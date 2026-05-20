---
name: cloud-deploy-check
description: Use when preparing, reviewing, documenting, or verifying cloud deployment readiness for a Java 25 Spring Boot marketplace app, Dockerized backend, managed PostgreSQL database, environment variables, logs, or CI/CD deployment flow.
---

Use this skill to make sure cloud deployment is understandable, minimal, and interview-ready.

## 1. Deployment Goal

Restate what is being deployed and why.

Identify:
- local application
- Docker image/container
- cloud compute service
- managed PostgreSQL service
- environment variables
- logs and monitoring
- deployment documentation

If the provider is not specified, recommend one simple path and state the assumption clearly.

## 2. Architecture Flow

Always include this flow and adapt it to the project:

```text
Developer machine
   |
   v
Spring Boot app
   |
   v
Docker image
   |
   v
Cloud service
   |
   v
Managed PostgreSQL
   |
   v
Logs / metrics / health checks
```

Explain what runs locally, what runs in the cloud, and what the database provider manages.

## 3. Inspect Current Deployment Files

Before changing anything, inspect existing files such as:
- `Dockerfile`
- `docker-compose.yml`
- `compose.yaml`
- `.env.example`
- `application.properties`
- `application.yml`
- CI/CD workflow files
- README or deployment guide

Do not invent a deployment setup without checking the current project conventions.

## 4. Cloud Readiness Checklist

Check:
- app can run from environment variables
- database URL, username, and password are not hardcoded
- secrets are excluded from git
- production profile is documented
- health endpoint exists or is planned
- Docker image builds cleanly
- app exposes the expected port
- database migrations are handled or documented
- logs go to stdout/stderr
- CORS and allowed origins are explicit if needed
- CI/CD commands are documented

## 5. Database and Transaction Safety

For PostgreSQL-backed deployment, explain:
- how the app connects to the managed database
- which values come from environment variables
- whether schema creation uses migrations or JPA auto-DDL
- what must happen if the database is unreachable
- which operations require transactions

Prefer migration-based production schema management when the project is ready for it.

## 6. Provider Tradeoffs

When relevant, compare the chosen provider briefly against alternatives.

Explain:
- why this provider is beginner-friendly
- what it manages for the user
- what the user still owns
- cost/free-tier risks
- scaling limits
- vendor lock-in concerns

Keep the recommendation practical. Do not turn a beginner marketplace app into a complex platform architecture.

## 7. Verification Commands

Run or recommend the strongest relevant commands:

```bash
./mvnw test
./mvnw verify
docker compose up --build
docker build -t marketplace-api .
```

If a cloud CLI is configured, verify deployment status and logs with the provider's normal commands.

If a command cannot run, explain exactly why and name the command the user should run.

## 8. Deployment Guide Output

When documenting deployment, include:
- chosen provider/service
- required environment variables
- build command
- run/start command
- database setup steps
- migration/schema notes
- health check URL
- log location
- rollback or redeploy basics

Do not include real secrets.

## 9. Failure Modes

List realistic deployment failures and responses:
- missing environment variable
- database credentials are wrong
- cloud database blocks network access
- migrations fail
- app starts on the wrong port
- Docker image builds but app crashes
- CORS blocks frontend requests
- free-tier service sleeps or cold-starts

Explain how to diagnose each one using logs, health checks, or configuration review.

## 10. Interview Readiness Check

End with:

```text
Interview explanation:
"I containerized the Spring Boot backend, configured it through environment variables, deployed it to X, and connected it to managed PostgreSQL. The main production concern is Y. I avoided hardcoding secrets because Z. If scaling this later, I would improve A, B, and C."
```
