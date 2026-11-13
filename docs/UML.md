# VoltX – UML Diagrams

All diagrams are [Mermaid](https://mermaid.js.org) source. GitHub renders them directly; to export
images, paste a block into https://mermaid.live or run `npx @mermaid-js/mermaid-cli -i UML.md`.

## 1. Use-case diagram

```mermaid
flowchart LR
    C((EV Customer))
    A((Administrator))
    S((Scheduler<br/>system actor))

    subgraph VoltX
        UC1([Register / log in])
        UC2([Manage profile])
        UC3([Manage vehicles])
        UC4([Search stations])
        UC5([Check connector compatibility])
        UC6([Book charging slot])
        UC7([Cancel reservation])
        UC8([Start charging])
        UC9([Monitor charging progress])
        UC10([Stop charging])
        UC11([View history & invoices])
        UC12([Pay invoice])
        UC13([Manage stations])
        UC14([Manage charging units & pricing])
        UC15([Manage customers])
        UC16([Monitor / stop sessions])
        UC17([View analytics])
        UC18([View audit logs])
        UC19([Expire no-shows, auto-complete sessions])
    end

    C --- UC1 & UC2 & UC3 & UC4 & UC6 & UC7 & UC8 & UC9 & UC10 & UC11 & UC12
    UC6 -. includes .-> UC5
    UC10 -. includes .-> UC11
    A --- UC1 & UC13 & UC14 & UC15 & UC16 & UC17 & UC18
    S --- UC19
```

## 2. Class diagram (domain + strategies)

```mermaid
classDiagram
    direction TB
    class BaseEntity { <<abstract>> -Long id -LocalDateTime createdAt -LocalDateTime updatedAt }
    class User { -String email -String passwordHash -Role role -boolean enabled +updateProfile() +disable() }
    class Vehicle {
        <<abstract>>
        -BigDecimal batteryCapacityKwh
        -int currentBatteryPercentage
        -Set~ConnectorType~ supportedConnectors
        +checkCompatibility(ChargingUnit)
        +findCompatibilityIssue(ChargingUnit) Optional~String~
        +effectiveChargingPower(BigDecimal) BigDecimal
        +defaultTargetPercentage(ChargingUnit) int
        #validateTypeSpecificConstraints(ChargingUnit)*
        +getVehicleType()* VehicleType
    }
    class ElectricCar { +validateTypeSpecificConstraints() +defaultTargetPercentage() }
    class ElectricBike { +validateTypeSpecificConstraints() }
    class ElectricScooter { +validateTypeSpecificConstraints() }
    class ChargingStation { -String city -StationStatus status +isOpenDuring() +enable() +disable() }
    class ChargingUnit { -ChargerStatus status -PricingPlan pricingPlan +reserve() +startCharging() +completeCharging() +releaseReservation() +markOutOfService() +restoreService() }
    class ChargerStatus { <<enumeration>> AVAILABLE RESERVED CHARGING OUT_OF_SERVICE MAINTENANCE +canTransitionTo() }
    class Reservation { -LocalDateTime startTime -LocalDateTime endTime -ReservationStatus status +cancel() +activate() +complete() +expire() }
    class ChargingSession { -double initialBatteryPercentage -Double finalBatteryPercentage -BigDecimal energyConsumedKwh +complete() }
    class Invoice { -BigDecimal subtotal -BigDecimal taxAmount -BigDecimal totalAmount +issue()$ +markPaid() }
    class Payment { -PaymentMethod method -PaymentStatus status -String transactionRef }
    class AuditLog { -String actorEmail -AuditAction action }

    BaseEntity <|-- User
    BaseEntity <|-- Vehicle
    BaseEntity <|-- ChargingStation
    BaseEntity <|-- ChargingUnit
    BaseEntity <|-- Reservation
    BaseEntity <|-- ChargingSession
    BaseEntity <|-- Invoice
    BaseEntity <|-- Payment
    Vehicle <|-- ElectricCar
    Vehicle <|-- ElectricBike
    Vehicle <|-- ElectricScooter
    User "1" --> "*" Vehicle : owns
    ChargingStation "1" *-- "*" ChargingUnit
    ChargingUnit --> ChargerStatus
    Reservation --> User
    Reservation --> Vehicle
    Reservation --> ChargingUnit
    ChargingSession "0..1" --> "1" Reservation
    Invoice "1" --> "1" ChargingSession
    Payment "*" --> "1" Invoice

    class PricingStrategy { <<interface>> +plan() +calculatePrice(kwh, rate, start) PriceBreakdown }
    PricingStrategy <|.. StandardPricingStrategy
    PricingStrategy <|.. PeakHourPricingStrategy
    PricingStrategy <|.. PromotionalPricingStrategy

    class ChargingSimulator { <<interface>> +supports(ctx) +simulate(ctx, elapsed) ChargingProgress }
    ChargingSimulator <|.. LinearChargingSimulator
    ChargingSimulator <|.. TaperingCurveChargingSimulator
    ChargingSimulationEngine o-- ChargingSimulator

    class PaymentProcessor { <<interface>> +method() +process(Invoice) PaymentResult }
    class AbstractPaymentProcessor { <<abstract>> +process(Invoice) #authorize(amount)* #referencePrefix()* }
    PaymentProcessor <|.. AbstractPaymentProcessor
    AbstractPaymentProcessor <|-- UpiPaymentProcessor
    AbstractPaymentProcessor <|-- CardPaymentProcessor
    AbstractPaymentProcessor <|-- WalletPaymentProcessor

    class NotificationSender { <<interface>> +channel() +sendNotification(Notification) }
    NotificationSender <|.. EmailNotificationSender
    NotificationSender <|.. SmsNotificationSender
    NotificationSender <|.. InAppNotificationSender

    class StrategyRegistry~K,S~ { +get(K) S +keys() Set~K~ }
```

## 3. Sequence diagram – booking a charger

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer (React)
    participant RC as ReservationController
    participant RS as ReservationServiceImpl
    participant DB as MySQL
    participant BP as BookingPolicy
    participant V as Vehicle (subclass)
    participant U as ChargingUnit
    participant AU as AuditService
    participant N as NotificationService

    C->>RC: POST /api/reservations {vehicleId, chargingUnitId, startTime, duration}
    RC->>RC: @Valid request body
    RC->>RS: create(userId, request)
    activate RS
    Note over RS,DB: @Transactional begins
    RS->>DB: find vehicle by id AND owner
    RS->>DB: SELECT charger ... FOR UPDATE (pessimistic lock)
    RS->>BP: validate(context)
    BP->>BP: duration, past/future window, station active, charger status, opening hours
    BP->>V: checkCompatibility(unit)
    V->>V: connector check + validateTypeSpecificConstraints() (overridden)
    alt any rule fails
        BP-->>RC: InvalidBooking / ChargerUnavailable / IncompatibleConnector
        RC-->>C: 400 / 409 / 422 JSON error
    end
    RS->>DB: existsOverlappingOnUnit(start, end)?
    alt overlap
        RS-->>C: 409 BOOKING_CONFLICT
    end
    RS->>DB: INSERT reservation (CONFIRMED)
    opt slot starts within 15 minutes
        RS->>U: reserve()  (AVAILABLE → RESERVED)
    end
    RS->>AU: record(RESERVATION_CREATED)
    RS->>N: notify(user, "Booking confirmed")
    Note over RS,DB: commit (lock released)
    N-->>N: after commit → @Async dispatch on notification thread pool
    RS-->>RC: ReservationResponse
    deactivate RS
    RC-->>C: 201 Created
```

## 4. Sequence diagram – charging session

```mermaid
sequenceDiagram
    autonumber
    actor C as Customer (React)
    participant SC as ChargingSessionController
    participant SS as ChargingSessionServiceImpl
    participant U as ChargingUnit
    participant E as ChargingSimulationEngine
    participant SIM as ChargingSimulator
    participant P as PricingStrategy
    participant B as BillingServiceImpl
    participant DB as MySQL

    C->>SC: POST /api/sessions/start {reservationId}
    SC->>SS: start()
    SS->>DB: load reservation, check owner + CONFIRMED + start window
    SS->>DB: lock charger row
    SS->>U: startCharging()  (RESERVED → CHARGING)
    SS->>DB: INSERT session (price & plan snapshotted)
    SC-->>C: 201 session

    loop every 2 s while page is open
        C->>SC: GET /api/sessions/active
        SC->>SS: active()
        SS->>E: progressOf(session, now)
        E->>SIM: simulate(context, elapsed × multiplier)
        Note right of SIM: Linear (AC) or TaperingCurve (DC)<br/>chosen at runtime
        SIM-->>E: ChargingProgress(battery %, kWh, minutes)
        SS->>P: calculatePrice(kWh, rate, start)
        SC-->>C: live battery / energy / cost
    end

    C->>SC: POST /api/sessions/{id}/stop
    SC->>SS: stop()
    activate SS
    Note over SS,DB: one @Transactional unit
    SS->>E: final progress
    SS->>SS: energy = capacity × (final% − initial%) / 100
    SS->>P: calculatePrice() → PriceBreakdown
    SS->>SS: session.complete(...)
    SS->>U: completeCharging()  (CHARGING → AVAILABLE)
    SS->>SS: vehicle.updateBatteryLevel(), reservation.complete()
    SS->>B: generateInvoice(session, breakdown)  (+18% GST)
    B->>DB: INSERT invoice
    deactivate SS
    SC-->>C: completed session + invoiceId
```

## 5. Entity-relationship diagram

```mermaid
erDiagram
    users ||--o{ vehicles : owns
    users ||--o{ reservations : makes
    users ||--o{ charging_sessions : runs
    users ||--o{ invoices : billed
    vehicles ||--|{ vehicle_connectors : supports
    vehicles ||--o{ reservations : for
    vehicles ||--o{ charging_sessions : charges
    charging_stations ||--o{ charging_units : contains
    charging_units ||--o{ reservations : booked_on
    charging_units ||--o{ charging_sessions : hosts
    reservations ||--o| charging_sessions : fulfilled_by
    charging_sessions ||--|| invoices : produces
    invoices ||--o{ payments : settled_by

    users { bigint id PK
        varchar email UK
        varchar password_hash
        varchar role }
    vehicles { bigint id PK
        bigint owner_id FK
        varchar vehicle_type
        varchar registration_number UK }
    vehicle_connectors { bigint vehicle_id PK,FK
        varchar connector_type PK }
    charging_stations { bigint id PK
        varchar name
        varchar city
        varchar status }
    charging_units { bigint id PK
        bigint station_id FK
        varchar charger_code UK
        varchar status
        bigint version }
    reservations { bigint id PK
        bigint charging_unit_id FK
        datetime start_time
        datetime end_time
        varchar status }
    charging_sessions { bigint id PK
        bigint reservation_id FK,UK
        decimal energy_consumed_kwh
        decimal cost }
    invoices { bigint id PK
        bigint session_id FK,UK
        varchar invoice_number UK
        decimal total_amount }
    payments { bigint id PK
        bigint invoice_id FK
        varchar transaction_ref UK }
    audit_logs { bigint id PK
        varchar actor_email
        varchar action }
```

## 6. Component diagram

```mermaid
flowchart TB
    subgraph Frontend["React SPA (Vite, Tailwind, Recharts)"]
        Pages[Customer & admin pages]
        Ctx[AuthContext / ToastContext]
        ApiClient[Axios client + JWT interceptor]
        Pages --> Ctx --> ApiClient
        Pages --> ApiClient
    end

    subgraph Backend["Spring Boot application"]
        Sec[Security: JwtAuthenticationFilter, SecurityConfig]
        Ctl[REST controllers]
        Svc[Service interfaces + impl]
        Pol[BookingPolicy]
        Sim[Simulator: engine + simulators]
        Str[Strategies: pricing, payment, notification]
        Rep[Spring Data repositories]
        Sch[BookingLifecycleScheduler]
        Exc[GlobalExceptionHandler]
        Doc[springdoc OpenAPI / Swagger UI]
        Sec --> Ctl --> Svc
        Svc --> Pol
        Svc --> Sim
        Svc --> Str
        Svc --> Rep
        Sch --> Svc
        Ctl -.errors.-> Exc
    end

    DB[(MySQL 8 + Flyway)]
    ApiClient -- REST/JSON --> Sec
    Rep --> DB
```
