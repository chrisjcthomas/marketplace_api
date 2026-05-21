# Deployment & Cloud Operations Guide

This guide describes how to run the Marketplace Order API locally and deploy it as a **Cloud-Ready MVP Foundation** on Google Cloud Platform (GCP).

---

## Local Development

### 1. Start PostgreSQL
Run the backing database using Docker Compose:

```bash
docker compose up -d
```

### 2. Run the Spring Boot API
Start the application locally using the Maven wrapper:

```bash
./mvnw spring-boot:run
```

### 3. Access Swagger UI
Verify API endpoints and interactive documentation in your browser:

```text
http://localhost:8080/swagger-ui.html
```

---

## Google Cloud Architecture (Cloud-Ready MVP)

The application is deployed using the following serverless and managed GCP stack:

```text
Client / Postman / curl (Bearer Token)
        |
        v
[ Google Cloud Run IAM Proxy ] (secures service via --no-allow-unauthenticated)
        |
        v
[ Spring Boot Container ]
        | (Secure Unix Domain Socket via Cloud SQL Java Connector)
        v
[ Cloud SQL PostgreSQL ] (Protected, accepting connections only via IAM proxy)
```

> [!NOTE]
> This architecture represents a **Cloud-Ready MVP Foundation**. While secure and well-configured, it is not yet fully production-grade. To transition this to an enterprise production deployment, the following components are required:
> - **Centralized Authentication & Authorization:** Migrating from basic Cloud Run IAM proxy protection to application-level OAuth2/OIDC (e.g. Okta, Keycloak, or Google Identity Platform).
> - **Idempotency Control:** Deduplication of transactional REST requests (like order payments) using idempotency keys.
> - **Centralized Tracing & Observability:** Integrating Google Cloud Trace or OpenTelemetry to monitor request lifecycles.
> - **Rate Limiting:** Guarding API endpoints against denial-of-service and abuse.

---

## GCP Deployment Tutorial (Managed Socket Factory Connection)

Our primary and verified deployment path leverages the **Google Cloud SQL Java Connector (Socket Factory)**. This connects the serverless Cloud Run container directly and securely to the Cloud SQL instance using native Unix domain sockets, completely bypassing the need for a separate Serverless VPC Access Connector and complex private IP setup.

### Step 1: Set Project Context and Enable API Services
Choose your active GCP project and enable the necessary infrastructure APIs:

```bash
# Set your active GCP project ID
gcloud config set project PROJECT_ID

# Enable the Google APIs required for serverless container operations
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com
```

### Step 2: Create a Google Artifact Registry
Create a secure, modern container repository to store your application images:

```bash
# Create a Docker repository in us-central1
gcloud artifacts repositories create marketplace-repo \
  --repository-format=docker \
  --location=us-central1 \
  --description="Marketplace API Docker images"
```

### Step 3: Create Cloud SQL PostgreSQL
Create a managed PostgreSQL database instance within your project:

```bash
# Create a cost-efficient db-f1-micro PostgreSQL instance
gcloud sql instances create marketplace-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1

# Create the primary database
gcloud sql databases create marketplace \
  --instance=marketplace-db

# Create the application database user
gcloud sql users create marketplace \
  --instance=marketplace-db \
  --password="YOUR_SECURE_DATABASE_PASSWORD"
```

### Step 4: Save Database Password in Secret Manager
Store database secrets safely inside Secret Manager:

```bash
# Store the database password secret securely
echo -n "YOUR_SECURE_DATABASE_PASSWORD" | gcloud secrets create marketplace-db-password \
  --data-file=- \
  --replication-policy="automatic"
```

### Step 5: Package and Push the Container Image
Compile the application and build the container image in the cloud using Google Cloud Build:

```bash
# Compile code, build image and push to your Artifact Registry
gcloud builds submit --tag us-central1-docker.pkg.dev/PROJECT_ID/marketplace-repo/marketplace-order-api:latest
```

### Step 6: Deploy the Container to Cloud Run
Deploy the image to Cloud Run, securing it behind Google IAM proxy authentication, attaching the Cloud SQL instance natively, and passing the Secret Manager secret:

```bash
gcloud run deploy marketplace-order-api \
  --image us-central1-docker.pkg.dev/PROJECT_ID/marketplace-repo/marketplace-order-api:latest \
  --region us-central1 \
  --no-allow-unauthenticated \
  --add-cloudsql-instances PROJECT_ID:us-central1:marketplace-db \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_URL="jdbc:postgresql://google/marketplace?cloudSqlInstance=PROJECT_ID:us-central1:marketplace-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory",DB_USERNAME=marketplace \
  --set-secrets DB_PASSWORD=marketplace-db-password:latest
```

---

## 🛠️ Flyway Database Migration Details

To ensure reliable early database schema migration during startup inside the Cloud Run environment, the project uses a manual migration configuration bean rather than relying solely on default Spring Boot auto-configuration.

The implementation is located at [FlywayConfig.java](file:///C:/Users/cobek/Desktop/test/marketplace-order-api/src/main/java/com/example/marketplace/config/FlywayConfig.java):

```java
package com.example.marketplace.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }
}
```

This guarantees that the migrations defined under `src/main/resources/db/migration` run BEFORE Hibernate validates or initializes JPA components on application startup.

---

## Operational Verification & Testing

Since `--no-allow-unauthenticated` is applied, all public endpoint access is blocked by default. You must verify and test the deployment using valid Google Cloud credentials.

### 1. Authenticate and Generate an Identity Token
Fetch your Google-signed OpenID Connect (OIDC) identity token:

```bash
gcloud auth print-identity-token
```

### 2. Verify Actuator Health Endpoint
Test the container liveness and readiness probe by passing the token in the `Authorization` header:

```bash
curl -H "Authorization: Bearer $(gcloud auth print-identity-token)" \
  https://marketplace-order-api-xxxx-uc.a.run.app/actuator/health
```
*Expected Response:* `{"groups":["liveness","readiness"],"status":"UP"}`

### 3. Verify Flyway Schema Creation
Stream the container startup logs to confirm Flyway migration scripts executed successfully against PostgreSQL:

```bash
gcloud run services logs tail marketplace-order-api
```
*Expected Output:*
```text
Created Schema History table "public"."flyway_schema_history"
Current version of schema "public": << Empty Schema >>
Migrating schema "public" to version "1 - init schema"
Successfully applied 1 migration to schema "public"
```

### 4. Query a Protected REST Endpoint
Ensure the Spring Boot API is operational by creating a dummy user using an authenticated payload:

```bash
curl -X POST \
  -H "Authorization: Bearer $(gcloud auth print-identity-token)" \
  -H "Content-Type: application/json" \
  -d '{"fullName": "Cloud Tester", "email": "cloud.tester@example.com"}' \
  https://marketplace-order-api-xxxx-uc.a.run.app/api/users
```

---

## Alternative Path: Private IP + Serverless VPC Access

If your organization strictly mandates private network endpoints without utilizing the database connector, you can opt for the **Private IP + Serverless VPC Connector** route:

1. Create a VPC address allocation and serverless connection network.
2. Provision a **Serverless VPC Access Connector** (ranges `10.8.0.0/28`) inside your default network.
3. Provision the database instance on private IP: `--no-assign-ip --network=default`.
4. Deploy the Cloud Run service with: `--vpc-connector=marketplace-vpc-connector` and bind the database `DB_URL` directly to `jdbc:postgresql://<PRIVATE_IP>:5432/marketplace`.
