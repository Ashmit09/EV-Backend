# VoltX — Viva Preparation (50 questions)

Answers are written to be said out loud. File references point into `backend/src/main/java/com/voltx/`.

## A. Basic Java OOP

**1. What are the four pillars of OOP?**
Encapsulation, inheritance, polymorphism and abstraction. In VoltX: the charger's status is
encapsulated in `ChargingUnit`; `ElectricCar/Bike/Scooter` inherit from `Vehicle`; pricing strategies
are polymorphic; and services depend on abstractions such as `PricingStrategy`.

**2. What is encapsulation? Show it in your project.**
Hiding data and exposing only safe operations. `ChargingUnit.status` is private with no setter. You
must call `reserve()`, `startCharging()`, `completeCharging()` etc., and each checks the legal
transition table in `ChargerStatus`.

**3. Why not just add `setStatus()`?**
Then any controller could jump a charger from AVAILABLE straight to CHARGING without a booking, or
free a charger that is still charging. Keeping the rule inside the entity means it is enforced
everywhere, once.

**4. What is inheritance?**
A class reusing the fields and methods of a parent. `ElectricScooter extends Vehicle` gets battery,
connectors and the compatibility algorithm, and only adds its own rule: no DC charging.

**5. What is an abstract class?**
A class that cannot be instantiated and may have abstract methods. `Vehicle` is abstract because a
"generic vehicle" does not exist — it must be a car, bike or scooter.

**6. What is polymorphism?**
One call, many behaviours. `vehicle.checkCompatibility(unit)` behaves differently for a scooter and
a car although the caller only holds a `Vehicle` reference.

**7. Compile-time vs runtime polymorphism?**
Compile-time = overloading, resolved by the compiler from parameter types (`EnergyCalculator.energyConsumed` has
three versions). Runtime = overriding, resolved by the JVM from the object's real class
(`PricingStrategy.calculatePrice`).

**8. Overloading vs overriding?**
Overloading: same name, different parameters, same class. Overriding: same signature in a subclass
replacing the parent's behaviour, marked with `@Override`.

**9. What is abstraction?**
Showing *what* and hiding *how*. The session service calls `simulationEngine.progressOf(session, now)`
and does not know about time multipliers or the DC tapering curve.

**10. Abstract class vs interface?**
An abstract class can hold state and constructors and is used for closely related classes
(`Vehicle`). An interface is a pure contract that unrelated classes can implement, and a class can
implement many (`PricingStrategy`). `AbstractPaymentProcessor` uses both: it implements the
`PaymentProcessor` interface and shares common code with its subclasses.

## B. Advanced OOP & Java

**11. What is the Template Method pattern and where is it?**
A base method defines the algorithm's skeleton and lets subclasses fill steps.
`Vehicle.checkCompatibility()` always does the connector check, then calls the subclass hook
`validateTypeSpecificConstraints()`. `AbstractPaymentProcessor.process()` is another example.

**12. What is the Strategy pattern?**
Interchangeable algorithms behind one interface. Pricing (`Standard/PeakHour/Promotional`),
simulators (AC/DC) and payment processors are strategies selected at runtime.

**13. How is the right strategy selected?**
Spring injects every `PricingStrategy` bean as a `List`; `AppConfig` builds a
`StrategyRegistry<PricingPlan, PricingStrategy>` (an `EnumMap`). The charger's `pricingPlan` is the key.

**14. Where did you use generics, and why?**
`StrategyRegistry<K extends Enum<K>, S>` is written once and reused for pricing and payments with
type safety. `PageResponse<T>` wraps any paginated DTO.

**15. Why are DTOs records?**
Records are immutable, concise and auto-generate `equals/hashCode/toString`. A response object
should never change after creation.

**16. Where did you use streams and lambdas?**
Analytics groups invoices by month with `Collectors.groupingBy`; station search composes filters with
`Predicate.and()`; `BookingPolicy` stores its rules as lambdas in a `List<BookingRule>`.

**17. What is a functional interface? Did you write one?**
An interface with exactly one abstract method, usable as a lambda. `BookingRule` with
`void check(BookingContext)` — each booking rule is one lambda.

**18. Where is `Optional` used?**
Repository lookups: `repo.findById(id).orElseThrow(() -> new StationNotFoundException(id))`, and
`Vehicle.findCompatibilityIssue()` returns `Optional<String>`.

**19. Where is multithreading used and why?**
Notifications are sent on an `@Async` thread pool so a slow email/SMS never delays a booking, and a
`@Scheduled` job expires no-shows and finishes sessions. The in-app inbox uses `ConcurrentHashMap`
because background threads write while request threads read.

**20. Why did you inject a `Clock` instead of calling `LocalDateTime.now()`?**
Testability. Tests freeze time (e.g. 10:00 or 19:30) to check booking windows and peak-hour pricing
deterministically.

## C. Project architecture

**21. Explain your architecture.**
Layered modular monolith: React → REST controllers → service interfaces → implementations → domain
entities and strategies → Spring Data repositories → MySQL. Controllers contain no business logic.

**22. Why a monolith and not microservices?**
One team, one domain, one database. Microservices would add network calls, distributed transactions
and deployment complexity with no benefit at this scale.

**23. Why separate service interfaces from implementations?**
Dependency inversion: controllers depend on `ReservationService`, not the class. It allows mocking
and swapping implementations.

**24. How is the charging simulator separated from business logic?**
`simulator/` contains `ChargingSimulator` implementations and an engine that maps real time to
simulated time. The session service only asks for a `ChargingProgress`; it never computes battery
physics.

**25. How is energy calculated?**
`energy = battery capacity × (final % − initial %) / 100` in `EnergyCalculator`. The simulator
decides the final % from effective power (min of charger and vehicle limit), efficiency and elapsed
time; DC charging tapers above 80%.

**26. Why does the frontend not calculate the cost?**
The browser can be manipulated and timers are unreliable. The server computes progress from the
stored start time each time the page polls, and computes the final bill when charging stops.

**27. What happens if the admin changes the price during a session?**
Nothing for that session — price and plan are copied into `ChargingSession` when it starts.

**28. What does the scheduler do?**
Every 15 s: reserves chargers whose slot starts within 15 minutes, expires bookings whose slot ended
without charging (and frees the charger), and auto-completes sessions that reached their target or
ran past their slot.

## D. Database & SQL

**29. Name your tables and key relationships.**
users, vehicles (+ vehicle_connectors), charging_stations, charging_units, reservations,
charging_sessions, invoices, payments, audit_logs. Station 1–N units, user 1–N vehicles,
reservation 1–0..1 session, session 1–1 invoice, invoice 1–N payments.

**30. How did you store the vehicle inheritance hierarchy?**
JPA SINGLE_TABLE: one `vehicles` table with a `vehicle_type` discriminator. Simple queries, no joins;
subclass-specific columns are not needed because subclasses differ in behaviour, not data.

**31. How do you prevent double booking?**
Inside one transaction: lock the charger row with `SELECT … FOR UPDATE` (`findByIdForUpdate`), check
for an overlapping CONFIRMED/ACTIVE reservation, then insert. A concurrent request waits for the lock
and then sees the first booking.

**32. What is the overlap condition?**
Two intervals overlap when `existing.start < new.end AND existing.end > new.start`. Touching slots
(10:00–11:00 and 11:00–12:00) do not overlap.

**33. Optimistic vs pessimistic locking — which do you use?**
Both. Pessimistic row lock for booking (correctness under contention). An `@Version` column on
`charging_units` adds optimistic locking so a stale admin update fails instead of overwriting.

**34. Why Flyway?**
The schema is versioned SQL (`V1__schema.sql`, `V2__seed_stations.sql`), applied the same way on every
machine. Hibernate's `ddl-auto` is `none`, so the database never changes silently.

**35. Which indexes did you add and why?**
`reservations(charging_unit_id, start_time, end_time)` for overlap checks, `charging_sessions(user_id,
status)` for "my active session", `charging_stations(city)` for search, `audit_logs(created_at)` for paging.

**36. What is a transaction and where do you need one?**
An all-or-nothing unit of work. Stopping a session updates the session, charger, vehicle,
reservation and creates an invoice — if invoice creation fails, everything rolls back.

## E. REST API & Spring Boot

**37. Give examples of your REST endpoints and status codes.**
`POST /api/reservations` → 201; overlap → 409; incompatible connector → 422; validation → 400;
not logged in → 401; other user's invoice → 403; unknown id → 404.

**38. What does `@Transactional(readOnly = true)` do?**
Marks a read-only transaction: Hibernate skips dirty checking, and lazy relations can be loaded
while building DTOs.

**39. What is constructor injection and why prefer it?**
Spring passes dependencies to the constructor. Fields are `final`, dependencies are explicit, and
objects can be created in unit tests without Spring.

**40. How is the API documented?**
springdoc-openapi generates Swagger UI at `/swagger-ui.html`, with a bearer-token "Authorize" button.

## F. Exception handling

**41. How are errors handled?**
Custom exceptions extend `VoltXException`, which declares `getStatus()` and `getErrorCode()`.
`GlobalExceptionHandler` (`@RestControllerAdvice`) converts any of them to the same JSON shape.

**42. Why one handler for the whole hierarchy?**
Polymorphism: the handler calls `ex.getStatus()` and each subclass answers. Adding a new exception
needs no handler change.

**43. What does the client see on an unexpected error?**
A generic 500 message. The stack trace is logged on the server only, so internals are never leaked.

## G. Security

**44. How are passwords stored?**
BCrypt hashes (salted, slow by design). The hash is never returned by any API.

**45. How does JWT authentication work here?**
Login returns a signed HS256 token (subject = email, role claim, 12 h expiry). The SPA sends
`Authorization: Bearer …`; `JwtAuthenticationFilter` verifies it and reloads the user, so a disabled
account loses access immediately.

**46. How is role-based access enforced?**
Twice for admin APIs: URL rules in `SecurityConfig` (`/api/admin/**` → ADMIN) and
`@PreAuthorize("hasRole('ADMIN')")` on admin controllers. Customer APIs require CUSTOMER.

**47. How do you stop a customer reading another customer's invoice?**
Ownership is checked in the service: `invoice.isOwnedBy(userId)` or else
`UnauthorizedOperationException` (403). The user id comes from the token, never from the request body.

**48. Where is the JWT secret stored?**
In the `VOLTX_JWT_SECRET` environment variable. If missing in development, a random key is generated
at start-up — no secret is committed.

## H. Scenario-based

**49. Two users click "Book" for the same charger and time at the same instant. What happens?**
Both transactions try to lock the charger row. The first books and commits; the second then runs the
overlap query, finds the booking and gets 409 BOOKING_CONFLICT.

**50. A scooter owner tries to book a 60 kW CCS2 charger, and the scooter's connector list includes CCS2. Allowed?**
No. The connector check passes, but `ElectricScooter.validateTypeSpecificConstraints()` rejects every DC
charger — 422 INCOMPATIBLE_CONNECTOR. That is runtime polymorphism deciding a real business rule.

**51. The admin marks a charger out of service while it has future bookings.**
`ChargingUnit.markOutOfService()` validates the transition (it is refused while RESERVED or CHARGING),
then `ReservationService.cancelUpcomingForCharger()` cancels upcoming bookings and notifies each customer,
all in one transaction, with audit entries.

**52. The customer books but never arrives.**
When the slot ends the scheduler marks the reservation EXPIRED, releases the charger
(RESERVED → AVAILABLE), writes an audit log and notifies the customer.

**53. The customer stops charging after 0 seconds.**
Energy is 0 kWh, so the invoice total is ₹0 and it is marked paid automatically — there is nothing to collect.

**54. How would you add a "night discount" tariff?**
Add `NIGHT` to `PricingPlan` and write `NightPricingStrategy implements PricingStrategy` as a
`@Component`. The registry picks it up; no service changes. That is the Open/Closed principle.
