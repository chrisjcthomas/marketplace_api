# How I built this marketplace order API

This guide is written for someone starting from almost zero. You do not need to already feel like a Java developer to follow it. The goal is to build a real backend project slowly enough that you understand what each piece is doing.

The project is a cloud-ready marketplace order API. It uses Java 25, Spring Boot 4, PostgreSQL, Flyway, Docker, Swagger/OpenAPI, JUnit, Mockito, and Cloud Run deployment notes. That sounds like a lot. It is a lot. But the trick is not to learn everything at once. The trick is to build one thin slice, verify it, then add the next slice.

The final app supports:

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

No frontend. No Kafka. No Redis. No Kubernetes. No real payment provider. Those are useful later, but they would distract from the main thing: understanding a working backend.

## What is an API?

API stands for Application Programming Interface. That sounds more complicated than it is.

An API is a way for one piece of software to talk to another piece of software using a set of agreed rules.

Think about a restaurant. You do not walk into the kitchen, open the fridge, and cook your own food. You use the menu. The menu tells you what you can ask for. The waiter takes your request to the kitchen. The kitchen does the work and sends something back.

An API is like that menu and ordering system for software.

In this project, the API lets another program ask the marketplace backend to do things:

```text
Create a user
Create a business
Create a product
List products
Place an order
Check an order
Update an order status
Simulate a payment
List notifications
```

The other program could be:

- a frontend website
- a mobile app
- Postman
- Swagger UI
- another backend service
- a scheduled job
- a testing script

The important part is that the other program does not need to know how the database works. It does not need to know that products live in a `products` table or that orders use an `order_items` table. It sends a request to the API and gets a response back.

For example:

```text
POST /api/products
```

means:

```text
"I want to create a product."
```

The request body might look like this:

```json
{
  "name": "Notebook",
  "price": 12.50,
  "stockQuantity": 10,
  "businessId": 1
}
```

The API receives that JSON, validates it, runs the business logic, saves a row in PostgreSQL, and returns a response.

That is the basic API loop:

```text
Client
   |
   | sends HTTP request
   v
API
   |
   | validates and runs business logic
   v
Database
   |
   | returns saved or requested data
   v
API
   |
   | sends HTTP response
   v
Client
```

You use an API when you want software systems to interact without sharing their internal code or database directly.

You would use an API when:

- a website needs to load product data from a backend
- a mobile app needs to create an order
- a payment provider needs to tell your system a payment succeeded
- two company systems need to exchange customer or inventory data
- an admin tool needs to update order status
- a reporting tool needs to read business data

You probably would not use a web API for every tiny thing. If all the code lives inside one program and never needs to be called from the outside, a normal method call may be enough. APIs matter when there is a boundary: different apps, different servers, different teams, or different systems.

In this project, Spring Boot helps us build a REST API. REST APIs usually use HTTP methods:

```text
GET     read data
POST    create data
PATCH   update part of something
DELETE  remove something
```

So when you see:

```text
GET /api/products
```

read it as:

```text
"Get me the list of products."
```

And when you see:

```text
POST /api/orders
```

read it as:

```text
"Create a new order."
```

That is why this project is called a marketplace order API. It is not the full marketplace website. It is the backend interface that other software can use to create users, products, orders, payments, and notifications.

## 1. What we are building

The app models a simple marketplace:

- A user can register.
- A user can own a business.
- A business can list products.
- A customer can place an order for products.
- The order calculates a total.
- Product stock goes down when the order is created.
- The app creates notifications for order and payment events.
- Payment is simulated with a boolean instead of a real payment provider.

The main workflow is order creation:

```text
HTTP request
   |
   v
OrderController
   |
   v
CreateOrderRequest DTO validation
   |
   v
OrderService transaction begins
   |
   +-- find customer
   +-- find products
   +-- check stock
   +-- reduce stock
   +-- calculate total
   +-- save order and order items
   +-- create notification
   |
   v
OrderResponse DTO
   |
   v
HTTP response
```

The important sentence to understand is this:

```text
Order creation is transactional because order persistence and inventory reduction must succeed or fail together.
```

That is not interview cosplay. That is the actual reason the service method uses `@Transactional`.

## 2. Tools to install first

You need these:

- Java 25
- Maven
- Git
- Docker Desktop
- An editor such as IntelliJ IDEA or VS Code
- Postman, curl, or Swagger UI for testing requests

Check Java:

```powershell
java -version
```

You should see Java 25.

Check Maven:

```powershell
mvn -version
```

You should see Maven using Java 25.

Check Git:

```powershell
git --version
```

Check Docker:

```powershell
docker --version
```

If Docker says the daemon is not running, open Docker Desktop and wait until it finishes starting.

## 3. Create the project folder and start Git tracking

Start in a place where you keep projects. In this workspace, the project lives here:

```text
C:\Users\cobek\Desktop\test\marketplace-order-api
```

If you were creating it from scratch, you could do:

```powershell
cd C:\Users\cobek\Desktop\test
mkdir marketplace-order-api
cd marketplace-order-api
git init
```

Create a basic `.gitignore` right away. You do not want build outputs, editor files, local secrets, or database volumes tracked by Git.

```gitignore
target/
.idea/
.vscode/
*.iml
.env
*.log
```

Then make the first commit:

```powershell
git add .gitignore
git commit -m "Start marketplace order API project"
```

If Git asks who you are, configure your name and email:

```powershell
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

You can check what Git sees at any time:

```powershell
git status
```

Use Git as a save system for meaningful checkpoints. Do not wait until the entire app is finished.

Good commit points:

```text
Add Spring Boot project setup
Add marketplace domain entities
Add product and business endpoints
Add transactional order creation
Add simulated payment flow
Add Docker and deployment docs
Add beginner build tutorial
```

## 4. Create the Spring Boot project

The project uses Maven. The main build file is `pom.xml`.

The parent tells Maven this is a Spring Boot project:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.0.3</version>
    <relativePath/>
</parent>
```

The Java version is set to 25:

```xml
<java.version>25</java.version>
```

The important dependencies are:

- `spring-boot-starter-web` for REST controllers.
- `spring-boot-starter-data-jpa` for database persistence.
- `spring-boot-starter-validation` for request validation.
- `spring-boot-starter-actuator` for health checks.
- `flyway-core` and `flyway-database-postgresql` for schema migrations.
- `postgresql` for the database driver.
- `springdoc-openapi-starter-webmvc-ui` for Swagger UI.
- `spring-boot-starter-test` for tests.

In plain English, Maven downloads the libraries. Spring Boot wires them together. Your code defines the business behavior.

Create the main application class:

```java
package com.example.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MarketplaceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApiApplication.class, args);
    }
}
```

What this means:

- `public class MarketplaceApiApplication` is the Java class that starts the app.
- `main` is the entry point.
- `SpringApplication.run(...)` starts the Spring Boot server.
- `@SpringBootApplication` tells Spring to scan the package for controllers, services, repositories, and configuration.

Commit after the project compiles:

```powershell
mvn test
git add .
git commit -m "Add Spring Boot project setup"
```

## 5. Understand the project layers

This project uses a layered backend structure:

```text
controller
   |
   v
dto
   |
   v
service
   |
   v
repository
   |
   v
domain entity
   |
   v
database table
```

Here is what each layer does.

Controller:

Receives HTTP requests and returns HTTP responses. It should not contain serious business rules.

DTO:

Defines the shape of request and response JSON. DTO means Data Transfer Object. It is what crosses the API boundary.

Service:

Contains business logic. This is where order creation, payment simulation, stock reduction, and notification creation belong.

Repository:

Talks to the database through Spring Data JPA. You write small interfaces, and Spring creates the implementation at runtime.

Entity:

Represents data stored in the database. An entity usually maps to a table.

This separation matters because it keeps the app understandable. If a controller starts doing database logic, the app becomes hard to test. If an entity is returned directly from the API, internal database details leak into your public contract.

## 6. Add the domain model

The domain model is the set of Java classes that represent marketplace data.

This project uses:

```text
UserAccount
Business
Product
CustomerOrder
OrderItem
Payment
Notification
```

It also uses enums:

```text
OrderStatus
PaymentStatus
NotificationType
```

An enum is a fixed list of allowed values. For example:

```java
public enum OrderStatus {
    CREATED,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
```

This is safer than passing random strings around. If the status must be one of six values, the code should say that directly.

### Entity example: Product

`Product` has:

- `id`
- `name`
- `price`
- `stockQuantity`
- `business`
- `version`

The `business` field means many products can belong to one business:

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "business_id", nullable = false)
private Business business;
```

In database language, the `products` table has a `business_id` foreign key.

The `reduceStock` method is a small business rule:

```java
public void reduceStock(int quantity) {
    if (quantity > stockQuantity) {
        throw new IllegalArgumentException("Not enough stock for product " + id);
    }
    stockQuantity -= quantity;
}
```

That method changes the product object. Because the product is a JPA entity inside a transaction, JPA can write that changed stock value back to the database.

### Entity example: CustomerOrder

`CustomerOrder` has:

- a customer
- a status
- a total amount
- a creation time
- a list of order items

The relationship to order items is one order to many items:

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();
```

The important part is `cascade = CascadeType.ALL`. When the app saves an order, JPA also saves the order items attached to that order.

Commit after the entities exist:

```powershell
mvn test
git add src/main/java/com/example/marketplace/domain
git commit -m "Add marketplace domain entities"
```

## 7. Add database tables with Flyway

JPA knows about Java entities. PostgreSQL still needs real tables.

Flyway handles that through migration files:

```text
src/main/resources/db/migration/V1__init_schema.sql
```

The naming matters:

```text
V1__init_schema.sql
```

Flyway sees `V1` as migration version 1. It runs the file once and records that it ran.

The migration creates tables:

```sql
CREATE TABLE marketplace_users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);
```

That table stores users. `BIGSERIAL` creates auto-incrementing IDs. `UNIQUE` prevents two users from sharing the same email.

The products table links products to businesses:

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity >= 0),
    business_id BIGINT NOT NULL REFERENCES businesses(id),
    version BIGINT NOT NULL DEFAULT 0
);
```

The `business_id` column is a foreign key. It means every product must belong to a real business.

This is why SQL matters even in a Spring Boot project. The Java code and database schema have to agree.

## 8. Configure local PostgreSQL

Local development uses Docker Compose:

```yaml
services:
  postgres:
    image: postgres:18-alpine
    container_name: marketplace-postgres
    environment:
      POSTGRES_DB: marketplace
      POSTGRES_USER: marketplace
      POSTGRES_PASSWORD: marketplace
    ports:
      - "5432:5432"
```

Run it:

```powershell
docker compose up -d
```

The Spring Boot app reads database settings from `application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/marketplace}
    username: ${DB_USERNAME:marketplace}
    password: ${DB_PASSWORD:marketplace}
```

This means:

- If `DB_URL` exists as an environment variable, use it.
- If it does not exist, use the local default.

That pattern is important for cloud deployment. You do not want to hardcode real production secrets in a file.

## 9. Add repositories

Repositories are tiny interfaces:

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

`JpaRepository<Product, Long>` means:

- This repository manages `Product` entities.
- The ID type is `Long`.

Spring Data JPA gives you methods such as:

```text
findAll()
findById(id)
save(entity)
delete(entity)
```

You did not write those methods yourself. Spring creates them.

For payments, the app needs a custom lookup:

```java
Optional<Payment> findByOrderId(Long orderId);
```

Spring reads the method name and builds the query. That is convenient, but you should still understand what it means: find a payment row where the related order has this ID.

## 10. Add DTOs

DTOs define JSON input and output.

For users:

```java
public record CreateUserRequest(
        @NotBlank String fullName,
        @Email @NotBlank String email
) {
}
```

This is a Java record. A record is a compact way to create an immutable data carrier.

The validation annotations mean:

- `@NotBlank` rejects empty text.
- `@Email` checks email shape.

For responses:

```java
public record UserResponse(Long id, String fullName, String email) {
}
```

The response includes the ID because the database creates it.

Why not return the entity directly? Because entities are internal persistence objects. API responses are public contracts. Mixing them makes future changes harder.

## 11. Add services

Services are where the app earns its keep.

### UserService

The user service creates users:

```java
public UserResponse create(CreateUserRequest request) {
    UserAccount saved = userRepository.save(new UserAccount(request.fullName(), request.email()));
    return toResponse(saved);
}
```

Data flow:

```text
CreateUserRequest JSON
   |
   v
UserAccount entity
   |
   v
marketplace_users row
   |
   v
UserResponse JSON
```

### OrderService

Order creation is the main business logic:

```java
@Transactional
public OrderResponse create(CreateOrderRequest request) {
    UserAccount customer = userService.requireUser(request.customerUserId());
    CustomerOrder order = new CustomerOrder(customer);

    request.items().forEach(item -> {
        Product product = productService.requireProduct(item.productId());
        if (product.getStockQuantity() < item.quantity()) {
            throw new BusinessRuleException("Insufficient stock for product " + product.getId());
        }
        product.reduceStock(item.quantity());
        order.addItem(product, item.quantity());
    });

    CustomerOrder saved = orderRepository.save(order);
    notificationService.create(
            customer,
            NotificationType.ORDER_CREATED,
            "Order " + saved.getId() + " was created."
    );
    return toResponse(saved);
}
```

Read that slowly. The method:

- finds the customer
- creates a new order object
- loops over requested items
- finds each product
- checks stock
- reduces stock
- adds an order item
- saves the order
- creates a notification
- returns a response

The transaction is the safety net. If saving the order fails, the stock reduction should not remain in the database. If reducing stock fails, the order should not be saved.

This is the part of the project you should be able to explain in an interview.

## 12. Add controllers

Controllers expose the API.

Example:

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }
}
```

What Spring does here:

- `@RestController` marks this class as an HTTP controller.
- `@RequestMapping("/api/products")` sets the base URL.
- `@PostMapping` handles `POST /api/products`.
- `@GetMapping` handles `GET /api/products`.
- `@RequestBody` tells Spring to read JSON from the request body.
- `@Valid` runs validation on the DTO.
- Constructor injection gives the controller a `ProductService`.

The controller does not calculate totals. It does not reduce stock. It does not call repositories directly. That work belongs in services.

## 13. Add global exception handling

Things go wrong:

- A user ID does not exist.
- A product ID does not exist.
- Stock is too low.
- A request body is invalid.
- A duplicate email violates a database constraint.

Instead of writing error handling in every controller, the app uses `GlobalExceptionHandler`.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
```

`@RestControllerAdvice` lets one class handle exceptions across controllers.

For missing data:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, List.of(ex.getMessage()));
}
```

That returns HTTP 404.

For business rule failures:

```java
@ExceptionHandler(BusinessRuleException.class)
public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
    return build(HttpStatus.BAD_REQUEST, List.of(ex.getMessage()));
}
```

That returns HTTP 400.

The point is not just to avoid crashes. The point is to make API errors predictable.

## 14. Add payment simulation

The payment endpoint is intentionally simple:

```text
POST /api/payments/simulate
```

Example request:

```json
{
  "orderId": 1,
  "approved": true
}
```

If `approved` is true:

- create a payment with status `COMPLETED`
- update the order to `PAID`
- create a payment completed notification

If `approved` is false:

- create a payment with status `FAILED`
- leave the order status alone
- create a payment failed notification

This avoids real payment provider complexity. Real payments need webhooks, idempotency, signatures, retries, PCI concerns, and provider-specific SDKs. That is not a 2 or 3 day beginner build.

## 15. Add notifications

Notifications are stored in PostgreSQL for now.

The endpoint:

```text
GET /api/notifications
```

This returns notifications newest first.

In a larger system, notifications might become event-driven:

```text
Order created
   |
   v
Kafka / Pub/Sub event
   |
   v
Notification service
   |
   v
Email / SMS / push notification
```

But this first version stores notifications directly because the goal is to understand the flow.

## 16. Add tests

The project includes a service test for order creation.

The happy path test checks:

- order total is calculated
- stock is reduced
- order items are saved
- notification is created

The failure test checks:

- order creation fails when requested quantity is higher than available stock

Run tests:

```powershell
mvn test
```

Expected result:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Tests are not decoration. They are how you prove the business logic still works after changes.

Commit after tests pass:

```powershell
git add .
git commit -m "Add transactional order creation tests"
```

## 17. Run the API locally

Start the database:

```powershell
docker compose up -d
```

Start the API:

```powershell
mvn spring-boot:run
```

Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Try this flow:

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

Create an order:

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

Simulate payment:

```json
{
  "orderId": 1,
  "approved": true
}
```

Then check:

```text
GET /api/orders/1
GET /api/products
GET /api/notifications
```

You should see the order, reduced product stock, and notifications.

## 18. Add Docker support

The `Dockerfile` builds the app in one container stage and runs it in another:

```dockerfile
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/marketplace-order-api-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Why two stages?

- The first stage has Maven and compiles the app.
- The second stage only needs Java to run the jar.

That keeps the runtime image smaller and cleaner.

Build the image:

```powershell
docker build -t marketplace-order-api:local .
```

Run it with environment variables:

```powershell
docker run --rm -p 8080:8080 `
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/marketplace `
  -e DB_USERNAME=marketplace `
  -e DB_PASSWORD=marketplace `
  marketplace-order-api:local
```

If this fails, check:

- Is Docker Desktop running?
- Is PostgreSQL running?
- Is port 8080 already in use?
- Is the database URL correct from inside the container?

## 19. Add Cloud Run deployment notes

The deployment path is:

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
Google Cloud Run
   |
   v
Cloud SQL PostgreSQL
   |
   v
Logs / metrics / health checks
```

Cloud Run is a good first cloud target because it runs containers. If the container listens on the expected port, Cloud Run can run it.

The app reads config from environment variables:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
PORT
SPRING_PROFILES_ACTIVE
```

That matters because secrets should not be hardcoded.

The deployment guide uses:

```bash
gcloud builds submit --tag gcr.io/PROJECT_ID/marketplace-order-api
```

Then:

```bash
gcloud run deploy marketplace-order-api \
  --image gcr.io/PROJECT_ID/marketplace-order-api \
  --region us-central1 \
  --no-allow-unauthenticated \
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,DB_URL=jdbc:postgresql://PRIVATE_IP:5432/marketplace,DB_USERNAME=marketplace \
  --set-secrets DB_PASSWORD=marketplace-db-password:latest \
  --vpc-connector SERVERLESS_VPC_CONNECTOR_NAME
```

Do not paste real passwords into documentation. Use Secret Manager or another secret system. Keep Cloud Run behind IAM until the app has real authentication and authorization.

## 20. Security basics for this project

This project does not include authentication yet. That is intentional for the first build. Still, you should understand the security decisions.

What is already reasonable:

- Database credentials are read from environment variables.
- The production profile does not hardcode a password.
- Flyway controls schema changes.
- Validation rejects bad request bodies before service logic runs.
- Global exception handling avoids random stack traces in normal API errors.
- The app uses DTOs instead of exposing JPA entities directly.

What is missing on purpose:

- Authentication.
- Authorization.
- Rate limiting.
- Real payment security.
- CORS rules for a real frontend.
- Audit logging.
- Idempotency keys for order/payment requests.

If this were going beyond a demo, the next security steps would be:

- Add authentication with JWT or session-based login.
- Restrict who can create businesses and products.
- Add idempotency to payment simulation and order creation.
- Add request logging without logging passwords or tokens.
- Review Docker image dependencies and keep base images updated.
- Add HTTPS-only deployment through the cloud provider.
- Use Cloud SQL private networking or a managed connector instead of public database access.

The honest interview version is:

```text
This first version focuses on the core order workflow. It avoids hardcoded secrets and keeps validation and transaction boundaries clear, but it does not yet implement authentication or authorization. If this moved toward production, I would add identity, role checks, idempotency, rate limiting, and stronger observability.
```

That sounds much better than pretending a beginner project is production-grade.

## 21. Git workflow while building

Use Git throughout the build.

Check status:

```powershell
git status
```

See changes:

```powershell
git diff
```

Stage changes:

```powershell
git add .
```

Commit:

```powershell
git commit -m "Add product endpoint"
```

View history:

```powershell
git log --oneline
```

A simple build history might look like this:

```text
f91a8a2 Add beginner build tutorial
c24bd17 Add Cloud Run deployment docs
aa78c03 Add Docker support
30b912e Add payment simulation
6e5b02a Add transactional order creation
41188dd Add product and business endpoints
18a7b63 Add marketplace domain entities
03d2fd1 Add Spring Boot project setup
```

If you use GitHub:

```powershell
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/marketplace-order-api.git
git push -u origin main
```

Make commits before risky changes. That way, if you break something, you can inspect what changed instead of guessing.

## 22. How to use AI while learning this project

Use AI as a tutor, not a vending machine.

For a new feature, ask:

```text
Use the spring-feature skill.

Do not give me the final code immediately.

First explain:
1. What problem this feature solves
2. What files are needed
3. What each file is responsible for
4. What data moves through the system
5. What can go wrong
6. Then give me the smallest working implementation
7. Then quiz me on the code
```

For understanding code, ask:

```text
Use the explain-like-im-learning skill.

Explain this file line by line like I am new to Java and Spring Boot.
Then explain how an experienced backend engineer would describe it in an interview.
```

For cloud work, ask:

```text
Use the cloud-deploy-check skill.

Review this project for Cloud Run and Cloud SQL readiness.
Explain what is configured correctly, what is missing, and what I should verify before deploying.
```

If AI gives you code you cannot explain, slow down. Ask for the flow. Ask what object becomes what database row. Ask what happens when the request is invalid. Ask why the method is transactional.

The point is to build the project and build your understanding at the same time.

## 23. What to say in an interview

Beginner version:

```text
I built a Spring Boot API for a small marketplace. Users can create businesses, businesses can list products, and customers can place orders. When an order is created, the service checks that the customer and products exist, checks stock, calculates the total, saves the order, reduces inventory, and creates a notification.
```

Backend version:

```text
I built a Java 25 and Spring Boot 4 marketplace order API using a layered architecture. Controllers handle HTTP requests, DTOs define API contracts, services contain business logic, repositories handle persistence, and entities map to PostgreSQL tables. The main workflow is transactional order creation, where order persistence and inventory reduction succeed or fail together. I used Flyway for schema migrations, Docker for local and cloud packaging, and documented a Cloud Run plus Cloud SQL deployment path.
```

Cloud version:

```text
I containerized the Spring Boot backend and prepared it for Cloud Run. The app reads database configuration from environment variables and connects to managed PostgreSQL through Cloud SQL. I chose Cloud Run because it let me deploy one Java service as a container without spending the whole project window on infrastructure.
```

Security-aware version:

```text
The project does not pretend to be production complete. It avoids hardcoded secrets, validates request bodies, uses DTOs, and keeps transaction boundaries clear. Before production, I would add authentication, authorization, idempotency for payment/order requests, rate limiting, and stronger logging and monitoring.
```

## 24. What to study next

Study these in order:

1. Java classes, fields, constructors, methods, records, and enums.
2. HTTP methods and status codes.
3. Spring controllers, services, repositories, and dependency injection.
4. DTOs versus entities.
5. PostgreSQL tables, primary keys, foreign keys, and transactions.
6. JPA relationships such as `@ManyToOne` and `@OneToMany`.
7. Flyway migrations.
8. Unit testing with JUnit and Mockito.
9. Docker images and containers.
10. Cloud Run, Cloud SQL, environment variables, and secrets.

Do not rush to microservices. A well-understood modular monolith beats a pile of services you cannot explain.

## 25. Final build checklist

Before calling the project ready, run:

```powershell
mvn test
mvn -DskipTests package
```

If Docker Desktop is running:

```powershell
docker compose up -d
docker build -t marketplace-order-api:local .
```

Then manually test the order flow in Swagger:

```text
POST /api/users
POST /api/businesses
POST /api/products
GET  /api/products
POST /api/orders
GET  /api/orders/{id}
POST /api/payments/simulate
GET  /api/notifications
```

A project like this is not about having the fanciest stack. It is about being able to point at each file and say what it does, why it exists, what can go wrong, and how you verified it.
