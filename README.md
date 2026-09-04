# Inventory Reservation & Order Fulfillment Platform

A multi-tenant inventory reservation and order fulfillment service built with
Java 17, Spring Boot 3.x, PostgreSQL 16, Spring Data JPA, Spring Security,
JWT, Flyway, and Docker Compose.

The system is designed for flash-sale workloads where many users may attempt
to reserve the same SKU concurrently. Inventory correctness is the primary
concern and the system must never oversell stock.

---

## 1. Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Data JPA / Hibernate
- PostgreSQL 16
- Flyway
- Spring Security
- JWT authentication
- Maven
- Docker / Docker Compose
- JUnit 5
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger

---

## 2. Features

### Inventory & Reservation

- Multi-tenant inventory management
- Product catalog identified by SKU
- Warehouse-level inventory
- Atomic inventory reservation
- PostgreSQL pessimistic row locking
- Concurrent reservation protection
- Reservation TTL
- Automatic stock release after expiry
- Idempotent reservation API
- Multi-warehouse allocation support

### Orders & Payments

- Order creation from valid reservations
- Asynchronous simulated payment processing
- Payment SUCCESS / FAILURE / TIMEOUT outcomes
- Reservation release on payment failure
- Safe handling of late payment after reservation expiry
- Prevention of double stock deduction

### Security & Tenancy

- JWT authentication
- USER and ADMIN roles
- Tenant isolation using `X-Tenant-Id`
- Cross-tenant access rejected with HTTP 403

### Events & Observability

- Persisted domain events
- Tenant-scoped domain event read API
- Spring Boot Actuator health endpoint
- Reservation concurrency integration tests
- OpenAPI / Swagger documentation

---

## 3. Architecture

### Main Components

- **Catalog** - product and SKU management
- **Inventory** - warehouse-level stock management
- **Reservation** - temporary inventory holds
- **Order** - conversion of reservations into orders
- **Payment** - asynchronous simulated payment processing
- **Tenant/Security** - JWT authentication and tenant isolation
- **Domain Events** - persisted business events
- **PostgreSQL** - authoritative source of truth

### High-Level Flow

```text
                     ┌─────────────────┐
                     │     Client      │
                     └────────┬────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │   REST APIs     │
                     └────────┬────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        Reservation         Order          Admin APIs
          Service          Service
              │               │
              └───────────────┼───────────────┘
                              ▼
                     ┌─────────────────┐
                     │   PostgreSQL    │
                     │                 │
                     │ Product         │
                     │ Inventory       │
                     │ Reservation     │
                     │ Order           │
                     │ Payment         │
                     │ Domain Events   │
                     └─────────────────┘
                              │
                              ▼
                     ┌─────────────────┐
                     │ Payment Adapter │
                     │  (simulated)    │
                     └─────────────────┘

---

## 5. Run with Docker

### Prerequisites

* Docker
* Docker Compose

Start the application and PostgreSQL:

```bash
docker compose up -d --build
```

Check running containers:

```bash
docker compose ps
```

Application:

```text
http://localhost:8080
```

Stop the application:

```bash
docker compose down
```

To reset the database completely:

```bash
docker compose down -v
docker compose up -d --build
```

---

## 6. API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

Health check:

```text
http://localhost:8080/actuator/health
```

---

## 7. Testing

The project includes integration tests covering:

* Concurrent reservation without overselling
* Reservation expiry and stock release
* Late payment after reservation expiry
* Reservation idempotency
* Tenant isolation
* Payment failure and stock release
* Domain event persistence and tenant isolation

Run tests with:

```bash
mvn test
```

The concurrency test verifies that when 20 parallel reservations compete for 5 units of stock, exactly 5 reservations succeed and 15 fail.
