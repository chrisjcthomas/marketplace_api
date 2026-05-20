# Technical Decision Log

## 1. Start as a Modular Monolith

The first version is one Spring Boot service. This keeps the project realistic for a 2-3 day build and lets the order workflow become clear before adding distributed-system complexity.

## 2. Use PostgreSQL for Transactional Data

Users, businesses, products, orders, order items, payments, and notifications are structured relational data. PostgreSQL is the right default because the system needs foreign keys and transactions.

## 3. Keep Order Creation Transactional

Order creation validates the customer, validates products, checks stock, calculates totals, saves the order, reduces inventory, and creates a notification. These changes should succeed or fail together.

## 4. Simulate Payments

The payment endpoint intentionally avoids real payment providers. It demonstrates the business flow without introducing external compliance, secrets, webhooks, or provider-specific SDK complexity.

## 5. Cloud Run First

Cloud Run accepts a standard HTTP container and keeps the first cloud deployment lightweight. Cloud SQL provides managed PostgreSQL. ECS/RDS, GKE, or EKS can be considered later if team standards or scaling needs require them.

## Future Improvements

- Authentication and authorization.
- Kafka or Pub/Sub for order and payment events.
- Redis for caching or rate limiting.
- Separate catalog, order, payment, and notification services.
- Terraform for repeatable infrastructure.
- Centralized logging, metrics, tracing, and alerting.
