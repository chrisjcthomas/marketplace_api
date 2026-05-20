# Deployment Guide

## Local Run

Start PostgreSQL:

```bash
docker compose up -d
```

Run the API:

```bash
./mvnw spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Google Cloud Run and Cloud SQL

Create a Cloud SQL PostgreSQL instance and database named `marketplace`.

Build and push the container image:

```bash
gcloud builds submit --tag gcr.io/PROJECT_ID/marketplace-order-api
```

Deploy to Cloud Run:

```bash
gcloud run deploy marketplace-order-api \
  --image gcr.io/PROJECT_ID/marketplace-order-api \
  --region us-central1 \
  --no-allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://PRIVATE_IP:5432/marketplace,DB_USERNAME=marketplace \
  --set-secrets DB_PASSWORD=marketplace-db-password:latest \
  --vpc-connector SERVERLESS_VPC_CONNECTOR_NAME
```

This keeps Cloud Run behind IAM. The application does not include user authentication yet, so do not expose it publicly with real data. For a throwaway demo, you can choose unauthenticated Cloud Run access, but treat every endpoint as public.

The simple first deployment path is Cloud SQL private IP plus a Serverless VPC connector. If you prefer Cloud SQL Unix sockets, use a socket JDBC URL:

```text
jdbc:postgresql://google/marketplace?cloudSqlInstance=PROJECT_ID:REGION:INSTANCE_NAME&socketFactory=com.google.cloud.sql.postgres.SocketFactory
```

If using that socket URL, add the Cloud SQL JDBC Socket Factory dependency before deploying.
