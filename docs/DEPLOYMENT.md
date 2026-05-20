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
        | (Private IP routing via Serverless VPC Access)
        v
[ Cloud SQL PostgreSQL ] (Runs inside private network)
```

> [!NOTE]
> This architecture represents a **Cloud-Ready MVP Foundation**. While secure and well-configured, it is not yet fully production-grade. To transition this to an enterprise production deployment, the following components are required:
> - **Centralized Authentication & Authorization:** Migrating from basic Cloud Run IAM proxy protection to application-level OAuth2/OIDC (e.g. Okta, Keycloak, or Google Identity Platform).
> - **Idempotency Control:** Deduplication of transactional REST requests (like order payments) using idempotency keys.
> - **Centralized Tracing & Observability:** Integrating Google Cloud Trace or OpenTelemetry to monitor request lifecycles.
> - **Rate Limiting:** Guarding API endpoints against denial-of-service and abuse.

---

## GCP Deployment Tutorial (Private IP Route)

To deploy the application without modifying project code or dependencies, we use the **Private IP + Serverless VPC Connector** architecture. This routes all database traffic securely through a private virtual network.

### Step 1: Set Project Context and Enable API Services
Choose your active GCP project and enable the necessary infrastructure APIs:

```bash
# Set your active GCP project ID
gcloud config set project PROJECT_ID

# Enable the Google APIs required for serverless container operations
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  vpcaccess.googleapis.com \
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

### Step 3: Create Cloud SQL PostgreSQL (Private IP)
Create a managed PostgreSQL database instance within your project's default Virtual Private Cloud (VPC):

```bash
# Reserve an internal IP range for Google managed services.
gcloud compute addresses create google-managed-services-default \
  --global \
  --purpose=VPC_PEERING \
  --prefix-length=16 \
  --network=default

# Create the private services access connection required by Cloud SQL private IP.
gcloud services vpc-peerings connect \
  --service=servicenetworking.googleapis.com \
  --ranges=google-managed-services-default \
  --network=default

# Create a cost-efficient db-f1-micro PostgreSQL instance on a private IP
gcloud sql instances create marketplace-db \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=us-central1 \
  --no-assign-ip \
  --network=default

# Create the primary database
gcloud sql databases create marketplace \
  --instance=marketplace-db

# Create the application database user
gcloud sql users create marketplace \
  --instance=marketplace-db \
  --password="YOUR_SECURE_DATABASE_PASSWORD"
```
*(Note: To retrieve your database's private IP, run `gcloud sql instances describe marketplace-db --format="value(ipAddresses.ipAddress)"`)*

### Step 4: Save Database Password in Secret Manager
Store database secrets safely inside Secret Manager:

```bash
# Store the database password secret securely
echo -n "YOUR_SECURE_DATABASE_PASSWORD" | gcloud secrets create marketplace-db-password \
  --data-file=- \
  --replication-policy="automatic"
```

### Step 5: Provision a Serverless VPC Access Connector
Create a VPC connector to link serverless Cloud Run containers to your private VPC network where Cloud SQL resides:

```bash
# Create a VPC connector (allocates a /28 IP block)
gcloud compute networks vpc-access connectors create marketplace-vpc-connector \
  --region=us-central1 \
  --network=default \
  --range=10.8.0.0/28
```

### Step 6: Package and Push the Container Image
Compile the application and build the container image in the cloud using Google Cloud Build:

```bash
# Compile code, build image and push to your Artifact Registry
gcloud builds submit --tag us-central1-docker.pkg.dev/PROJECT_ID/marketplace-repo/marketplace-order-api:latest
```

### Step 7: Deploy the Container to Cloud Run
Deploy the image to Cloud Run, securing it behind Google IAM proxy authentication:

```bash
gcloud run deploy marketplace-order-api \
  --image us-central1-docker.pkg.dev/PROJECT_ID/marketplace-repo/marketplace-order-api:latest \
  --region us-central1 \
  --no-allow-unauthenticated \
  --vpc-connector=marketplace-vpc-connector \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://PRIVATE_IP:5432/marketplace,DB_USERNAME=marketplace \
  --set-secrets DB_PASSWORD=marketplace-db-password:latest
```

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
*Expected Response:* `{"status":"UP"}`

### 3. Verify Flyway Schema Creation
Stream the container startup logs to confirm Flyway migration scripts executed successfully against PostgreSQL:

```bash
gcloud run services logs tail marketplace-order-api
```
*Expected Output:*
```text
[INFO] o.f.c.i.d.DatabaseDriverFactory - Database: jdbc:postgresql://...
[INFO] o.f.core.internal.command.DbValidate - Successfully validated 1 migration
[INFO] o.f.c.i.c.DbMigrate - Current version of schema "public": << Empty Schema >>
[INFO] o.f.c.i.c.DbMigrate - Migrating schema "public" to version "1 - init schema"
[INFO] o.f.c.i.c.DbMigrate - Successfully applied 1 migration to schema "public"
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

## Upgrade Path: Managed Socket Factory Connection

If you want to migrate to the cleaner, managed connection pattern without using a VPC network adapter, you can transition to the **Google Cloud SQL Java Connector (Socket Factory)**.

### Step A: Add Dependency to pom.xml
Add the Google Cloud SQL Postgres socket factory dependency to `pom.xml`:

```xml
<dependency>
    <groupId>com.google.cloud.sql</groupId>
    <artifactId>postgres-socket-factory</artifactId>
    <version>1.28.3</version>
</dependency>
```

### Step B: Redeploy Using Instance Connections
Remove the VPC connector option and instead configure Cloud Run to attach the instance using native Unix domain sockets, modifying your JDBC connection string:

```bash
gcloud run deploy marketplace-order-api \
  --image us-central1-docker.pkg.dev/PROJECT_ID/marketplace-repo/marketplace-order-api:latest \
  --region us-central1 \
  --no-allow-unauthenticated \
  --add-cloudsql-instances PROJECT_ID:us-central1:marketplace-db \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_URL="jdbc:postgresql://google/marketplace?cloudSqlInstance=PROJECT_ID:us-central1:marketplace-db&socketFactory=com.google.cloud.sql.postgres.SocketFactory",DB_USERNAME=marketplace \
  --set-secrets DB_PASSWORD=marketplace-db-password:latest
```
