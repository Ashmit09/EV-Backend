# VoltX – Smart EV Charging Station Management System

VoltX is a full-stack EV charging platform built as an academic project. Drivers find stations,
check connector compatibility, book charging slots, run a simulated charging session and pay an
itemised invoice. Administrators manage the network and see live analytics computed from the database.

> **Honest scope.** Charging hardware, electricity metering and payment gateways are **simulated**.
> Charging time is accelerated (1 real second ≈ 1 minute of charging) so a full session can be demonstrated live.

## Problem statement

EV drivers in Indian cities struggle to know whether a charger is free, whether it fits their
vehicle, and what a session will cost. Operators need to manage stations, prevent double bookings
and understand utilisation and revenue.

## Objectives

1. Demonstrate the four OOP pillars through real business logic (see [docs/OOP_EXPLANATION.md](docs/OOP_EXPLANATION.md)).
2. Deliver a working full-stack system: Spring Boot REST API, MySQL, React SPA.
3. Enforce business rules on the server: compatibility, no overlapping bookings, valid charger state transitions, ownership of data.

## Features

**Customers:** register / log in (JWT) · profile & password · manage cars, bikes and scooters ·
search stations by city, keyword, connector and availability · per-vehicle compatibility check ·
server-side cost estimate · book / cancel slots · start / stop simulated charging with live progress ·
history, invoices and simulated payment (UPI / card / wallet) · dashboard with monthly charts · in-app notifications.

**Administrators:** platform overview (revenue, energy, utilisation, charger status) · add / edit /
enable / disable stations · add / configure chargers and pricing strategy · out-of-service /
maintenance / restore (validated state machine) · all reservations · live sessions with force-stop ·
customer enable / disable · billing & revenue analytics · energy analytics · paginated audit log.

## Technology stack

| Layer | Technology |
| --- | --- |
| Backend | Java 17, Spring Boot 3.4 (Web, Data JPA, Security, Validation), Maven |
| Security | BCrypt, JWT (jjwt 0.12), role-based access |
| Database | MySQL 8, Flyway migrations (H2 in MySQL mode for tests) |
| Docs | springdoc-openapi / Swagger UI |
| Tests | JUnit 5, Mockito, Spring Boot Test, MockMvc |
| Frontend | React 19, Vite 7, TypeScript, Tailwind CSS 4, React Router 7, Axios, Recharts, Lucide icons |

## Architecture

Layered modular monolith — full detail with diagrams in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md),
UML (use case, class, two sequence diagrams, ERD, component) in [docs/UML.md](docs/UML.md).

```
React SPA ──HTTP/JSON──▶ JWT filter ─▶ Controllers ─▶ Service interfaces ─▶ Impl
                                                         │        ├─ Entities (state machines)
                                                         │        ├─ Strategies (pricing, payment, notification)
                                                         │        └─ Charging simulator
                                                         └─▶ Spring Data repositories ─▶ MySQL (Flyway)
```

## Folder structure

```
oops_proj/
├── backend/                     Spring Boot app
│   ├── src/main/java/com/voltx/ config, controller(+admin), dto, entity, enums, exception,
│   │                            mapper, repository, scheduler, security, service(+impl),
│   │                            simulator, strategy/{pricing,payment,notification}, util
│   ├── src/main/resources/      application.yml, db/migration/V1__schema.sql, V2__seed_stations.sql
│   └── src/test/java/com/voltx/ domain, strategy, service (Mockito), integration (MockMvc)
├── frontend/                    React app (src/api, components, context, layouts, pages, types, utils)
└── docs/                        ARCHITECTURE, UML, OOP_EXPLANATION, VIVA_PREPARATION
```

## Database schema

Tables: `users`, `vehicles`, `vehicle_connectors`, `charging_stations`, `charging_units`,
`reservations`, `charging_sessions`, `invoices`, `payments`, `audit_logs`.
See the ERD in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#3-database-design) and the SQL in
`backend/src/main/resources/db/migration/`.

## Installation

### Prerequisites
* JDK 17+ (`java -version`)
* Maven 3.9+
* MySQL 8 running locally
* Node.js 20+ and npm

### 1. Database
No manual SQL is needed: the JDBC URL uses `createDatabaseIfNotExist=true` and Flyway creates the
tables and seeds 10 stations / 29 chargers. Defaults are user `root` with an empty password; override with
environment variables:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/voltx?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | `root` |
| `SPRING_DATASOURCE_PASSWORD` | *(none)* |
| `VOLTX_JWT_SECRET` | random key per start-up (set ≥ 32 chars for stable tokens) |
| `PORT` | `8085` |
| `VOLTX_CORS_ORIGINS` | `http://localhost:5180,http://127.0.0.1:5180` |
| `VOLTX_SIM_MULTIPLIER` | `60` (simulated seconds per real second) |
| `VOLTX_SEED_DEMO_DATA` | `true` (demo users + 6 months of history on an empty DB) |

### 2. Backend

```bash
cd backend
```

```bash
mvn spring-boot:run
```

API: http://localhost:8085/api · Swagger UI: http://localhost:8085/swagger-ui.html

### 3. Frontend

```bash
cd frontend
```

```bash
npm install
```

```bash
npm run dev
```

Open http://localhost:5180. To point at another backend, copy `.env.example` to `.env` and set `VITE_API_URL`.

### 4. Tests

```bash
cd backend && mvn test
```

54 tests: domain unit tests (state machine, vehicle polymorphism, energy, invoices), strategy tests
(pricing, simulators, payments), Mockito service tests (booking rules, auth) and MockMvc integration
tests covering registration, access control, the full book → charge → invoice → pay flow, cancellation,
incompatible connectors and admin management. Integration tests run the real Flyway migrations on H2.

## Demo credentials

| Role | Email | Password |
| --- | --- | --- |
| Administrator | admin@voltx.in | Admin@123 |
| Customer (Tata Nexon EV + Ather 450X) | aarav@voltx.in | Customer@123 |
| Customer (MG ZS EV) | priya@voltx.in | Customer@123 |
| Customer (Nissan Leaf + Ultraviolette F77) | rohan@voltx.in | Customer@123 |

These are local demo accounts created by `DemoDataSeeder`; change or disable them (`VOLTX_SEED_DEMO_DATA=false`) outside a demo.

## Suggested demo script

1. Sign in as **aarav@voltx.in** → dashboard shows real history and charts.
2. *Find stations* → Koramangala Hub → switch the vehicle between **Ather** (DC chargers marked incompatible) and **Nexon**.
3. Book **VX-BLR-01-B**, *Start now*, 1 h → note the server estimate.
4. *My bookings* → **Start charging** → watch live progress → **Stop charging** → invoice → pay by wallet.
5. Sign in as **admin@voltx.in** → overview, mark a charger *Maintenance*, try *Restore* twice (second fails: invalid transition), view *Audit logs*.

## API overview

| Area | Endpoints |
| --- | --- |
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Profile | `GET/PUT /api/users/me`, `PUT /api/users/me/password`, `GET /api/notifications` |
| Vehicles | `POST/GET /api/vehicles`, `GET/PUT/DELETE /api/vehicles/{id}` |
| Stations | `GET /api/stations` (`city`, `q`, `connector`, `availableOnly`), `/api/stations/search`, `/api/stations/cities`, `/api/stations/{id}?vehicleId=`, `/api/stations/{id}/chargers` |
| Chargers | `GET /api/chargers/{id}`, `GET /api/chargers/{id}/booked-slots?date=` |
| Reservations | `POST /api/reservations`, `POST /api/reservations/estimate`, `GET /api/reservations/my`, `GET /api/reservations/{id}`, `PATCH /api/reservations/{id}/cancel` |
| Sessions | `POST /api/sessions/start`, `GET /api/sessions/active`, `GET /api/sessions`, `GET /api/sessions/{id}`, `POST /api/sessions/{id}/stop` |
| Billing | `GET /api/invoices`, `GET /api/invoices/{id}`, `POST /api/invoices/{id}/pay` |
| Analytics | `GET /api/analytics/customer` |
| Admin | `/api/admin/stations[/{id}][/enable|/disable]`, `/api/admin/chargers[/{id}][/status]`, `/api/admin/users[/{id}/enable|disable]`, `/api/admin/reservations[/{id}/cancel]`, `/api/admin/sessions/active`, `/api/admin/sessions/{id}/stop`, `/api/admin/invoices`, `/api/admin/payments`, `/api/admin/analytics/{overview,revenue,energy}`, `/api/admin/audit-logs` |

Full request/response schemas are in Swagger UI.

## Screenshots

Add screenshots to `docs/screenshots/` (e.g. `landing.png`, `dashboard.png`, `booking.png`,
`live-session.png`, `invoice.png`, `admin-overview.png`) and reference them here:

```markdown
![Customer dashboard](docs/screenshots/dashboard.png)
```

## Documentation

* [Architecture & design](docs/ARCHITECTURE.md)
* [UML diagrams](docs/UML.md)
* [OOP explanation (class/method references)](docs/OOP_EXPLANATION.md)
* [Viva preparation – 54 Q&A](docs/VIVA_PREPARATION.md)
