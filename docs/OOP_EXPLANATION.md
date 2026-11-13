# OOP in VoltX — where each concept lives in the code

Paths are relative to `backend/src/main/java/com/voltx/`. Every concept below is used by a real
feature (booking, charging, billing), not by a demo class written just to show the concept.

---

## 1. Encapsulation

**Idea:** keep data private and allow changes only through methods that protect the rules.

| Where | What is protected | How |
| --- | --- | --- |
| `entity/ChargingUnit.java` | The charger `status` | No `setStatus()`. Only `reserve()`, `startCharging()`, `completeCharging()`, `releaseReservation()`, `markOutOfService()`, `markUnderMaintenance()`, `restoreService()`. All go through the private `transitionTo(ChargerStatus)`. |
| `enums/ChargerStatus.java` | Which transitions are legal | A static `EnumMap` table + `canTransitionTo()`. Example: `AVAILABLE → CHARGING` is rejected, you must reserve first. |
| `entity/Vehicle.java` | Battery level, specs | `updateBatteryLevel(int)` rejects values outside 0–100; `updateDetails(...)` validates capacity/power against the subclass limits. `getSupportedConnectors()` returns an **unmodifiable** set. |
| `entity/Reservation.java` | Booking lifecycle | `cancel()` only from CONFIRMED, `activate()` only from CONFIRMED, `complete()` only from ACTIVE. |
| `entity/ChargingSession.java` | Final figures | Price/plan are copied at start (snapshot). `complete(...)` can run once; a second call throws `InvalidSessionException`. |
| `entity/Invoice.java` | All money fields | Private constructor; the only way in is the factory `Invoice.issue(...)`, which computes tax and total itself. `markPaid()` refuses a second payment. |
| `entity/BaseEntity.java` | `id`, timestamps | No setters at all; set by the database and JPA callbacks. |
| `entity/AuditLog.java` | Audit history | No mutators — an audit row can never be changed after insert. |

Controller → service → entity: even an admin request goes through `ChargerServiceImpl.changeStatus()`,
which calls `unit.markOutOfService()`; an illegal move becomes HTTP 409 `INVALID_CHARGER_STATE`.

Test: `ChargingUnitStateMachineTest`.

---

## 2. Inheritance

| Hierarchy | Parent provides | Children add |
| --- | --- | --- |
| `Vehicle` → `ElectricCar`, `ElectricBike`, `ElectricScooter` | owner, make, model, registration, battery, connectors, the compatibility algorithm | their own capacity/power limits and charging rule |
| `BaseEntity` → every entity | `id`, `createdAt`, `updatedAt`, `equals/hashCode` | domain fields |
| `AbstractPaymentProcessor` → `Upi…`, `Card…`, `WalletPaymentProcessor` | validation + reference generation (template method `process`) | `referencePrefix()`, `authorize()`, optional `declineReason()` |
| `VoltXException` → `ResourceNotFoundException` → `StationNotFoundException`, `VehicleNotFoundException`, … and `BookingConflictException`, `IncompatibleConnectorException`, … | message, abstract `getStatus()` / `getErrorCode()` | the HTTP status and code |

Persistence: `Vehicle` uses JPA `SINGLE_TABLE` inheritance — one `vehicles` table with a
`vehicle_type` discriminator column (`CAR` / `BIKE` / `SCOOTER`). Hibernate creates the right
subclass when it reads a row.

Why not deeper hierarchies? Charging units differ only by data (connector, power), so they are
one class with an `isDcFastCharger()` query — inheritance there would add classes without behaviour.

---

## 3. Compile-time polymorphism (method overloading)

The compiler chooses the method from the argument types.

| Class | Overloads |
| --- | --- |
| `util/EnergyCalculator.java` | `energyConsumed(BigDecimal capacity, double initial%, double final%)`  ·  `energyConsumed(Vehicle, double, double)`  ·  `energyConsumed(BigDecimal powerKw, Duration)` |
| `service/NotificationService.java` | `notify(User, subject, message)` (default channels)  ·  `notify(User, subject, message, NotificationChannel...)` (varargs) |
| `service/AuditService.java` | `record(actor, action, entityType, entityId, details)`  ·  `record(actor, action, details)` |

Used for real: `ChargingSessionServiceImpl.finish()` calls the `Vehicle` overload;
`BillingServiceImpl.pay()` calls the varargs `notify` overload to add SMS.
Test: `EnergyCalculatorTest`.

---

## 4. Runtime polymorphism (method overriding + dynamic dispatch)

The JVM picks the implementation from the **object’s actual class**, not the variable’s type.

1. **Vehicle compatibility** – `BookingPolicy` holds a `Vehicle` reference and calls
   `vehicle.checkCompatibility(unit)`. Inside, `validateTypeSpecificConstraints(unit)` runs the
   override of the real subclass:
   * `ElectricScooter` – rejects every DC charger,
   * `ElectricBike` – rejects chargers above 60 kW,
   * `ElectricCar` – rejects DC if its own charger is below 25 kW.
   `ElectricCar` also overrides `defaultTargetPercentage()` → 80% on DC.
2. **Pricing** – `ChargingSessionServiceImpl` gets a `PricingStrategy` from the registry and calls
   `calculatePrice(...)`; Standard, PeakHour (+25% 18:00–22:00) or Promotional (−15%) runs.
3. **Simulation** – `ChargingSimulationEngine.simulatorFor(ctx)` returns a `ChargingSimulator`;
   `LinearChargingSimulator` (AC) or `TaperingCurveChargingSimulator` (DC) runs `simulate()`.
4. **Payments** – `BillingServiceImpl.pay()` calls `processor.process(invoice)` on a
   `PaymentProcessor` reference; the wallet declines above ₹2,000, UPI above ₹1,00,000.
5. **Notifications** – `AsyncNotificationDispatcher.dispatch()` loops over `List<NotificationSender>`
   and calls `sendNotification()`; e-mail, SMS and in-app each behave differently.
6. **Exceptions** – `GlobalExceptionHandler.handleDomain(VoltXException ex)` calls
   `ex.getStatus()`; each subclass answers with its own status (404, 409, 422…).

Tests: `VehicleCompatibilityTest.sameCallBehavesDifferentlyPerSubclass`, `PricingStrategyTest`,
`ChargingSimulatorTest`, `PaymentProcessorTest`.

---

## 5. Abstraction

**Idea:** expose *what* something does and hide *how*.

* **Interfaces:** `PricingStrategy`, `ChargingSimulator`, `PaymentProcessor`, `NotificationSender`,
  all service interfaces (`ReservationService`, `ChargingSessionService`, `BillingService`, …),
  repositories such as `StationRepository` (Spring Data writes the implementation at runtime),
  and the functional interface `BookingRule`.
* **Abstract classes:** `Vehicle`, `BaseEntity`, `AbstractPaymentProcessor`, `VoltXException`,
  `ResourceNotFoundException`.

Example: `ReservationServiceImpl` never knows whether time is accelerated or how a DC battery
tapers; it calls `simulationEngine.simulate(...)` and receives a `ChargingProgress` record.

---

## 6. Abstract class vs interface (with VoltX examples)

| | Abstract class | Interface |
| --- | --- | --- |
| State (fields) | Yes – `Vehicle` holds battery, connectors | No instance state |
| Constructors | Yes – `Vehicle(...)` validates specs | No |
| Multiple inheritance | A class extends one | A class implements many |
| Use when | Subclasses share **data and an algorithm** | Unrelated classes share a **capability/contract** |
| In VoltX | `Vehicle`, `AbstractPaymentProcessor` | `PricingStrategy`, `ChargingSimulator`, `PaymentProcessor` |

`AbstractPaymentProcessor` shows both together: the interface `PaymentProcessor` is the contract
the billing service depends on; the abstract class holds the common algorithm so the three
concrete processors only fill in two small methods (template method pattern).

**Why depend on an interface rather than a concrete class?**
`ChargingSessionServiceImpl` depends on `PricingStrategy`, not on `PeakHourPricingStrategy`.
Adding a "fleet discount" tariff needs one new `@Component` class — no change to billing or
session code (Open/Closed principle), and unit tests can pass any implementation.

---

## 7. Overloading vs overriding

| | Overloading | Overriding |
| --- | --- | --- |
| Same name, … | different parameters | same signature |
| Where | same class | subclass / implementing class |
| Resolved | compile time | runtime |
| VoltX | `EnergyCalculator.energyConsumed(...)` ×3 | `ElectricScooter.validateTypeSpecificConstraints(...)` |

---

## 8. SOLID principles

* **S – Single responsibility:** `BookingPolicy` only validates booking rules; `ChargingSimulationEngine`
  only converts time to energy; `BillingServiceImpl` only bills; controllers only translate HTTP.
* **O – Open/closed:** new pricing plans, simulators, payment methods or notification channels are
  new classes; `StrategyRegistry` picks them up automatically from Spring.
* **L – Liskov substitution:** any `Vehicle` subclass can be used where a `Vehicle` is expected; each
  only *adds* a type-specific rejection and never breaks the base contract (`checkCompatibility`
  always runs the connector check first).
* **I – Interface segregation:** small interfaces — `NotificationSender` has two methods,
  `BookingRule` has one. Customer and admin operations are separate methods so no client depends on
  methods it does not use.
* **D – Dependency inversion:** services depend on interfaces (`PricingStrategy`, `AuditService`,
  repositories); Spring injects concrete beans.

---

## 9. Dependency injection

Every Spring bean receives its collaborators through its **constructor**, e.g.

```java
public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       AuditService auditService, NotificationService notificationService)
```

Benefits: fields can be `final`, dependencies are visible, and tests can build the object by hand
with mocks (`ReservationServiceImplTest` does exactly this with Mockito and a fixed `Clock`).
Spring also injects collections: `List<PricingStrategy>` gives every strategy bean, which
`AppConfig` turns into a `StrategyRegistry<PricingPlan, PricingStrategy>`.

---

## 10. Other Java features (used where they solve a problem)

| Feature | Where |
| --- | --- |
| Enums with fields/behaviour | `ConnectorType` (label, isDirectCurrent), `ChargerStatus` (transition table), `PricingPlan` |
| Collections | `EnumMap`/`EnumSet` in `ChargerStatus`, `HashSet` of connectors, `ConcurrentHashMap` + `ConcurrentLinkedDeque` in `InAppNotificationStore` |
| Generics | `StrategyRegistry<K extends Enum<K>, S>`, `PageResponse<T>.from(Page<E>, Function)`, `AnalyticsServiceImpl.sum(List<T>, Function<T,BigDecimal>)` |
| Streams & lambdas | analytics grouping (`groupingBy`, `reducing`), station filters composed with `Predicate.and()` |
| Functional interfaces | `BookingRule` (custom), `Predicate`, `Function`, `IntSupplier` in the scheduler |
| Optional | repository lookups `.orElseThrow(() -> new StationNotFoundException(id))`, `Vehicle.findCompatibilityIssue()` |
| Records | all DTOs, `PriceBreakdown`, `ChargingProgress`, `ChargingContext`, `VoltXProperties` |
| Switch expressions | `VehicleFactory.create()`, `ChargerServiceImpl.changeStatus()` |
| Date/Time API | `LocalDateTime`, `LocalTime` (opening hours, peak window), `Duration`, `YearMonth` (monthly charts), injected `Clock` |
| Custom exceptions | `exception/` package, mapped centrally in `GlobalExceptionHandler` |
| Multithreading | `@Async` notification thread pool (`AppConfig.notificationExecutor`), `@Scheduled` lifecycle job, DB row locks for concurrent bookings |

---

## Why this architecture?

A layered modular monolith (controller → service → entity/strategy → repository) is the simplest
design that still separates concerns cleanly. Each layer can be explained and tested on its own,
there is one deployable and one database, and the OOP concepts sit in the domain and strategy
layers where real business decisions are made.
