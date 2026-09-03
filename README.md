# Inventory Reservation & Order Fulfillment Platform

A multi-tenant inventory reservation and order fulfillment service built with
Java 17, Spring Boot 3.x, PostgreSQL, Spring Data JPA and Docker Compose.

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

---

## 2. Features

- Multi-tenant inventory management
- Product catalog identified by SKU
- Warehouse-level inventory
- Atomic inventory reservation
- Concurrent reservation protection
- Reservation TTL and automatic stock release
- Idempotent reservation API
- Order creation from valid reservations
- Asynchronous simulated payment
- Payment SUCCESS / FAILURE / TIMEOUT outcomes
- Safe handling of late payment after reservation expiry
- JWT authentication with USER and ADMIN roles
- Tenant isolation
- Persisted domain events
- Concurrency integration test
- Dockerized application and PostgreSQL database

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
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        Reservation       Order          Admin APIs
          Service        Service
              │              │
              └──────────────┼──────────────┘
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