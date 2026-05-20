# Marketplace Order API

A Java 25 and Spring Boot 4 backend for a marketplace order flow: users, businesses, products, orders, simulated payments, and notifications.

This project starts as a modular monolith on purpose. The order workflow is small enough to keep in one service, and that makes the business logic easier to read, test, and explain before introducing distributed systems.

## What it does

- Creates marketplace users and businesses.
- Lets businesses publish products with stock and pricing.
- Creates customer orders from one or more products.
- Calculates order totals with `BigDecimal`.
- Reduces product stock when an order is placed.
- Stores payments as simulated success or failure events.
- Updates order status after successful payment.
- Records notifications for order and payment activity.
- Documents the API with Swagger UI.
- Uses Flyway migrations instead of Hibernate auto-DDL.

## Tech stack

- Java 25
- Spring Boot 4.0.3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Flyway
- JUnit and Mockito
- Docker and Docker Compose
- Swagger/OpenAPI via springdoc
- GitHub Actions
- Google Cloud Run and Cloud SQL deployment notes

## API

```text
POST   /api/users
POST   /api/businesses
POST   /api/products
GET    /api/products
POST   /api/orders
GET    /api/orders/{id}
PATCH  /api/orders/{id}/status
POST   /api/payments/simulate
GET    /api/notifications
```

## Quick start

Start PostgreSQL:

```bash
docker compose up -d
```

Run the API:

```bash
mvn spring-boot:run
```

Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

Run tests:

```bash
mvn test
```

Build the app:

```bash
mvn -DskipTests package
```

The Maven Wrapper is included too, so `./mvnw` or `.\mvnw.cmd` will also work.

## Example flow

Create a user:

```json
{
  "fullName": "Ada Lovelace",
  "email": "ada@example.com"
}
```

Create a business:

```json
{
  "name": "Ada Market",
  "category": "Retail",
  "ownerUserId": 1
}
```

Create a product:

```json
{
  "name": "Notebook",
  "price": 12.50,
  "stockQuantity": 10,
  "businessId": 1
}
```

Place an order:

```json
{
  "customerUserId": 1,
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

Simulate a successful payment:

```json
{
  "orderId": 1,
  "approved": true
}
```

After that, `GET /api/products` should show lower stock, `GET /api/orders/1` should show the order, and `GET /api/notifications` should show order/payment messages.

## Architecture

```text
Client / Postman / Swagger
        |
        v
Spring Boot API
        |
        v
PostgreSQL
```

Inside the app:

```text
Controller -> DTO -> Service -> Repository -> Entity -> Database
```

The main workflow is order creation. The service validates the customer, validates products, checks inventory, calculates the total, saves the order and items, reduces stock, and creates a notification in one transaction.

## Tradeoffs

This project favors a small, readable backend over a distributed architecture.

| Decision | Why it was chosen | Tradeoff |
| --- | --- | --- |
| Modular monolith | The domain is small, and one service keeps the order flow easy to understand. | Separate teams or high scale domains may eventually need service boundaries. |
| PostgreSQL only | Orders, products, payments, and users are relational data with useful foreign keys. | Flexible event history or search use cases may later need another store. |
| Synchronous notifications | Notifications are simple database rows for the first version. | A real system would likely publish events to Kafka, Pub/Sub, or a queue. |
| Simulated payments | The project can show payment state changes without provider setup or compliance work. | It does not cover real payment webhooks, retries, signatures, or idempotency. |
| Flyway migrations | Schema changes are explicit and repeatable. | Migrations require discipline when changing entities. |
| Cloud Run | Container deployment is simpler than managing servers or Kubernetes. | ECS, GKE, or EKS may fit better for teams with mature platform operations. |
| No authentication yet | The first build stays focused on the order workflow. | Production use would require identity, roles, rate limiting, and audit logging. |

See [Technical decisions](docs/TECHNICAL_DECISIONS.md) for the longer version.

## Configuration

Local defaults are defined in `src/main/resources/application.yml`:

```text
DB_URL=jdbc:postgresql://localhost:5432/marketplace
DB_USERNAME=marketplace
DB_PASSWORD=marketplace
PORT=8080
```

Production config should come from environment variables or a secret manager. Do not hardcode real credentials.

## Documentation

- [How I built this](docs/HOW_DID_I_BUILD_THIS.md): beginner follow-along tutorial from empty folder to working API.
- [Architecture](docs/ARCHITECTURE.md): runtime structure and order flow.
- [Deployment](docs/DEPLOYMENT.md): Cloud Run and Cloud SQL deployment notes.
- [Technical decisions](docs/TECHNICAL_DECISIONS.md): why this project starts as one service and where it can grow.
- [Security review](docs/SECURITY_REVIEW.md): repository-wide security notes, fixes, and residual risks.
- [AI workflow](docs/AI_WORKFLOW.md): prompts and project-specific learning workflow.

Agent instructions stay at the repository root in [AGENTS.md](AGENTS.md) so coding agents can find them quickly.

## Security notes

This is a learning project, not a production payment system. It avoids hardcoded production secrets, validates request bodies, uses DTOs at the API boundary, and keeps multi-step order/payment changes transactional.

Before production, add application-level authentication and authorization, idempotency for order/payment requests, rate limiting, audit logging, stricter CORS, and deeper observability. Until then, deploy Cloud Run behind IAM or use only throwaway demo data.

## Roadmap

Useful next steps:

- Add controller integration tests.
- Add authentication and role-based authorization.
- Add idempotency keys for order and payment requests.
- Add structured logs and metrics.
- Add a real deployment pipeline after the first manual Cloud Run deployment.

Deferred on purpose:

- Kafka or Pub/Sub
- Redis
- MongoDB
- Kubernetes
- Terraform
- Real payment provider
- Frontend
- Admin dashboard

Those can come later. The first job is to keep the order workflow correct and explainable.
