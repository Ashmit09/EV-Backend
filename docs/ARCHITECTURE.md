# VoltX – Architecture & Design

This document is the Phase 1–3 output: system architecture, folder structure,
database design, the UML class diagram and the mapping of the four OOP pillars
to real classes. All diagrams are Mermaid, so they render on GitHub and can be
regenerated (see `docs/UML.md` for the full UML set).

## 1. System architecture

VoltX is a **modular monolith**: one Spring Boot application exposing a REST API,
one React single-page application, one MySQL database.

```mermaid
flowchart LR
    subgraph Browser
        SPA["React SPA (Vite + Tailwind)<br/>Axios + JWT in localStorage"]
    end
    subgraph Backend["Spring Boot 3 (Java 17)"]
        SEC["Security filter chain<br/>JwtAuthenticationFilter"]
        CTRL["Controllers<br/>(REST, validation only)"]
        SVC["Services<br/>(business rules, @Transactional)"]
        DOM["Domain entities<br/>(encapsulated state machines)"]
        STRAT["Strategies<br/>PricingStrategy / ChargingSimulator<br/>PaymentProcessor / NotificationSender"]
        REPO["Spring Data JPA repositories"]
        SCHED["Schedulers (@Scheduled)<br/>reservation activation, auto-stop"]
    end
    DB[("MySQL 8<br/>Flyway migrations")]

    SPA -- "HTTPS / JSON" --> SEC --> CTRL --> SVC
    SVC --> DOM
    SVC --> STRAT
    SVC --> REPO --> DB
    SCHED --> SVC
```

Request flow: `Controller → Service (interface) → ServiceImpl → Entity methods + Strategies → Repository`.
Controllers never touch repositories; entities never touch Spring.

### Key design decisions

| Decision | Why |
| --- | --- |
| Modular monolith, not microservices | One deployable is enough for this domain and far easier to explain and run. |
| Rich domain entities (state changes via methods like `ChargingUnit.reserve()`) | Puts invariants next to the data (encapsulation); controllers cannot set a status directly. |
| Strategy interfaces for pricing, simulation, payment, notification | New pricing model = one new class, no edits to billing code (Open/Closed). |
| Charging progress computed **on the server** from elapsed time | The frontend only polls; it never decides energy or cost. |
| Pessimistic row lock on the charger while booking | Two users booking the same slot at the same instant cannot both succeed. |
| Flyway migrations | Schema is versioned, reproducible, and identical on every machine. |
| Stateless JWT | No server sessions; the SPA sends `Authorization: Bearer <token>`. |

### Simulation (honest scope)

No real hardware, metering or payment gateway is involved:

* **Charging** – a `ChargingSimulator` turns elapsed time into delivered energy. Time is
  accelerated (`voltx.simulation.time-multiplier`, default 60 ⇒ 1 real second = 1 simulated minute)
  so a session can be demonstrated in under a minute.
* **Payment** – `PaymentProcessor` implementations (UPI, card, wallet) generate a transaction
  reference and mark the invoice paid.
* **Notifications** – email / SMS / in-app senders log the message asynchronously.

## 2. Folder structure

```
oops_proj/
├── backend/                         Spring Boot application (Maven)
│   └── src/main/java/com/voltx/
│       ├── VoltXApplication.java
│       ├── config/                  OpenAPI, async, CORS, demo data seeder, properties
│       ├── controller/              REST controllers (customer + admin)
│       ├── dto/request|response/    API records — never expose entities
│       ├── entity/                  JPA entities (Vehicle hierarchy, ChargingUnit, ...)
│       ├── enums/                   ChargerStatus (with transition table), ConnectorType, ...
│       ├── exception/               VoltXException hierarchy + GlobalExceptionHandler
│       ├── mapper/                  Entity → DTO mapping
│       ├── repository/              Spring Data JPA interfaces
│       ├── security/                JWT service, filter, SecurityConfig, CurrentUser
│       ├── service/                 Service interfaces
│       │   └── impl/                Implementations (@Transactional business logic)
│       ├── strategy/
│       │   ├── pricing/             PricingStrategy + Standard / PeakHour / Promotional
│       │   ├── payment/             PaymentProcessor + AbstractPaymentProcessor + UPI/Card/Wallet
│       │   └── notification/        NotificationSender + Email / Sms / InApp
│       ├── simulator/               ChargingSimulator + Linear (AC) / TaperingCurve (DC)
│       ├── scheduler/               Reservation & session background jobs
│       └── util/                    EnergyCalculator (overloads), AppClock
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/            V1__schema.sql, V2__seed_stations.sql
├── frontend/                        React + Vite + TypeScript + Tailwind
│   └── src/
│       ├── api/                     Axios client + typed endpoint functions
│       ├── components/              UI kit (Button, Card, Modal, StatusBadge, ...)
│       ├── context/                 AuthContext, ToastContext
│       ├── layouts/                 Public / App shell with role-based sidebar
│       ├── pages/                   customer/*, admin/*, public pages
│       └── types/                   Shared TypeScript types
└── docs/                            ARCHITECTURE, UML, OOP_EXPLANATION, VIVA_PREPARATION
```

## 3. Database design

Nine core tables plus one collection table (`vehicle_connectors`, the normalised
list of connector types a vehicle supports).

```mermaid
erDiagram
    users ||--o{ vehicles : owns
    users ||--o{ reservations : makes
    users ||--o{ charging_sessions : runs
    users ||--o{ invoices : receives
    vehicles ||--o{ vehicle_connectors : supports
    vehicles ||--o{ reservations : "booked for"
    vehicles ||--o{ charging_sessions : charges
    charging_stations ||--o{ charging_units : contains
    charging_units ||--o{ reservations : "booked on"
    charging_units ||--o{ charging_sessions : hosts
    reservations ||--o| charging_sessions : "fulfilled by"
    charging_sessions ||--|| invoices : billed
    invoices ||--o{ payments : "paid by"

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar full_name
        varchar phone
        varchar role
        boolean enabled
    }
    vehicles {
        bigint id PK
        bigint owner_id FK
        varchar vehicle_type "CAR | BIKE | SCOOTER (discriminator)"
        varchar manufacturer
        varchar model
        varchar registration_number UK
        decimal battery_capacity_kwh
        decimal max_charging_power_kw
        int current_battery_percentage
    }
    vehicle_connectors {
        bigint vehicle_id FK
        varchar connector_type
    }
    charging_stations {
        bigint id PK
        varchar name
        varchar address
        varchar city
        decimal latitude
        decimal longitude
        time opening_time
        time closing_time
        boolean open_24_hours
        varchar status
    }
    charging_units {
        bigint id PK
        bigint station_id FK
        varchar charger_code UK
        varchar connector_type
        decimal max_power_kw
        decimal price_per_kwh
        varchar pricing_plan
        varchar status
        bigint version "optimistic lock"
    }
    reservations {
        bigint id PK
        bigint user_id FK
        bigint vehicle_id FK
        bigint charging_unit_id FK
        datetime start_time
        datetime end_time
        varchar status
    }
    charging_sessions {
        bigint id PK
        bigint reservation_id FK "UK"
        bigint user_id FK
        bigint vehicle_id FK
        bigint charging_unit_id FK
        datetime start_time
        datetime end_time
        decimal initial_battery_percentage
        decimal final_battery_percentage
        decimal energy_consumed_kwh
        int simulated_duration_minutes
        decimal cost
        varchar status
    }
    invoices {
        bigint id PK
        varchar invoice_number UK
        bigint session_id FK "UK"
        bigint user_id FK
        decimal energy_cost
        decimal adjustment_amount
        decimal tax_amount
        decimal total_amount
        varchar status
    }
    payments {
        bigint id PK
        bigint invoice_id FK
        decimal amount
        varchar method
        varchar status
        varchar transaction_ref UK
    }
    audit_logs {
        bigint id PK
        varchar actor_email
        varchar action
        varchar entity_type
        bigint entity_id
        varchar details
        datetime created_at
    }
```

Indexes: `reservations(charging_unit_id, start_time, end_time)` for overlap checks,
`charging_sessions(user_id, status)`, `charging_stations(city)`, `audit_logs(created_at)`.
Cascades: deleting a station cascades to its chargers; deleting a vehicle cascades to its
connector rows only (a vehicle with history cannot be deleted — the service blocks it).

## 4. UML class diagram (domain core)

```mermaid
classDiagram
    direction LR
    class BaseEntity {
        <<abstract>>
        #Long id
        #LocalDateTime createdAt
        #LocalDateTime updatedAt
    }
    class Vehicle {
        <<abstract>>
        -String manufacturer
        -String registrationNumber
        -BigDecimal batteryCapacityKwh
        -BigDecimal maxChargingPowerKw
        -int currentBatteryPercentage
        -Set~ConnectorType~ supportedConnectors
        +checkCompatibility(ChargingUnit) void
        +effectiveChargingPower(BigDecimal) BigDecimal
        +updateBatteryLevel(int) void
        #validateTypeSpecificConstraints(ChargingUnit)* void
        +getVehicleType()* VehicleType
        +maxBatteryCapacityKwh()* BigDecimal
    }
    class ElectricCar
    class ElectricBike
    class ElectricScooter
    Vehicle <|-- ElectricCar
    Vehicle <|-- ElectricBike
    Vehicle <|-- ElectricScooter
    BaseEntity <|-- Vehicle

    class ChargingUnit {
        -String chargerCode
        -ConnectorType connectorType
        -BigDecimal maxPowerKw
        -BigDecimal pricePerKwh
        -PricingPlan pricingPlan
        -ChargerStatus status
        +reserve() void
        +startCharging() void
        +completeCharging() void
        +releaseReservation() void
        +markOutOfService() void
        +markUnderMaintenance() void
        +restoreService() void
        -transitionTo(ChargerStatus) void
    }
    class ChargingStation {
        -String name
        -String city
        -StationStatus status
        -List~ChargingUnit~ chargingUnits
        +isOpenDuring(LocalDateTime, LocalDateTime) boolean
        +enable() void
        +disable() void
        +countAvailableUnits() long
    }
    ChargingStation "1" *-- "many" ChargingUnit

    class PricingStrategy {
        <<interface>>
        +plan() PricingPlan
        +calculatePrice(BigDecimal kwh, BigDecimal rate, LocalDateTime start) PriceBreakdown
    }
    PricingStrategy <|.. StandardPricingStrategy
    PricingStrategy <|.. PeakHourPricingStrategy
    PricingStrategy <|.. PromotionalPricingStrategy

    class ChargingSimulator {
        <<interface>>
        +supports(ChargingUnit) boolean
        +simulate(ChargingContext, Duration) ChargingProgress
    }
    ChargingSimulator <|.. LinearChargingSimulator
    ChargingSimulator <|.. TaperingCurveChargingSimulator

    class PaymentProcessor {
        <<interface>>
        +method() PaymentMethod
        +process(Invoice) PaymentResult
    }
    class AbstractPaymentProcessor {
        <<abstract>>
        +process(Invoice) PaymentResult
        #authorize(Invoice)* boolean
        #referencePrefix()* String
    }
    PaymentProcessor <|.. AbstractPaymentProcessor
    AbstractPaymentProcessor <|-- UpiPaymentProcessor
    AbstractPaymentProcessor <|-- CardPaymentProcessor
    AbstractPaymentProcessor <|-- WalletPaymentProcessor

    class NotificationSender {
        <<interface>>
        +channel() NotificationChannel
        +sendNotification(Notification) void
    }
    NotificationSender <|.. EmailNotificationSender
    NotificationSender <|.. SmsNotificationSender
    NotificationSender <|.. InAppNotificationSender

    class Reservation {
        -LocalDateTime startTime
        -LocalDateTime endTime
        -ReservationStatus status
        +cancel(String) void
        +activate() void
        +complete() void
        +expire() void
        +isWithinStartWindow(LocalDateTime) boolean
    }
    class ChargingSession {
        -double initialBatteryPercentage
        -Double finalBatteryPercentage
        -BigDecimal energyConsumedKwh
        -BigDecimal cost
        -SessionStatus status
        +complete(...) void
    }
    Reservation --> Vehicle
    Reservation --> ChargingUnit
    ChargingSession --> Reservation
    ChargingSession --> Invoice
```

## 5. OOP pillar mapping (summary)

| Pillar | Where | What to show in the viva |
| --- | --- | --- |
| **Encapsulation** | `ChargingUnit`, `ChargerStatus`, `Reservation`, `ChargingSession`, `Vehicle.updateBatteryLevel` | All fields private, no public status setter; `reserve()/startCharging()/completeCharging()` go through `transitionTo()` which consults the transition table in `ChargerStatus`. |
| **Inheritance** | `Vehicle` → `ElectricCar/ElectricBike/ElectricScooter`; `BaseEntity`; `AbstractPaymentProcessor` → UPI/Card/Wallet; `VoltXException` → `ResourceNotFoundException` → `StationNotFoundException` … | Shared state/behaviour lives once in the parent. |
| **Polymorphism (runtime)** | `vehicle.checkCompatibility(unit)` calls the subclass's `validateTypeSpecificConstraints`; `PricingStrategy.calculatePrice`; `ChargingSimulator.simulate`; `NotificationSender.sendNotification`; `PaymentProcessor.process` | The service holds an interface/parent reference; the JVM picks the override at runtime. |
| **Polymorphism (compile-time)** | `EnergyCalculator.energyConsumed(...)` ×3 overloads, `NotificationService.notify(...)` ×2 overloads, `AuditService.record(...)` ×2 overloads | Same name, different parameter lists, chosen by the compiler. |
| **Abstraction** | Interfaces `PricingStrategy`, `ChargingSimulator`, `PaymentProcessor`, `NotificationSender`, all `*Service` interfaces, `StationRepository`; abstract classes `Vehicle`, `AbstractPaymentProcessor`, `BaseEntity`, `VoltXException` | Callers depend on *what* (interface), not *how* (implementation). |

The detailed, class-and-method level explanation is in `docs/OOP_EXPLANATION.md`.
