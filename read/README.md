# Dispatch Load Balancer

Spring Boot service that allocates delivery orders to a vehicle fleet. It respects vehicle capacity,
assigns high-priority orders first and minimizes travel distance (Haversine).

## Requirements
- JDK 17+
- Maven 3.8+

## Run
```bash
mvn clean test           # all unit + integration tests
mvn spring-boot:run      # http://localhost:8080
bash scripts/smoke-test.sh   # optional end-to-end curl walkthrough (app must be running)
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:dispatchdb`, user `sa`, empty password)
- Postman: import `postman/Dispatch-Load-Balancer.postman_collection.json` and run the *Happy path* folder in order.
  Sample payloads are in `samples/`.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/dispatch/orders` | Upsert delivery orders (by `orderId`) |
| POST | `/api/dispatch/vehicles` | Upsert vehicles (by `vehicleId`) |
| GET | `/api/dispatch/plan` | Compute the dispatch plan from the stored data |

### POST /api/dispatch/orders
```json
{"orders":[{"orderId":"ORD001","latitude":12.9716,"longitude":77.5946,
  "address":"MG Road, Bangalore, Karnataka, India","packageWeight":10,"priority":"HIGH"}]}
```
`200` -> `{"message":"Delivery orders accepted.","status":"success"}`

### POST /api/dispatch/vehicles
```json
{"vehicles":[{"vehicleId":"VEH001","capacity":100,"currentLatitude":12.9716,"currentLongitude":77.6413,
  "currentAddress":"Indiranagar, Bangalore, Karnataka, India"}]}
```
`200` -> `{"message":"Vehicle details accepted.","status":"success"}`

### GET /api/dispatch/plan
```json
{
  "summary": {"totalOrders": 10, "assignedOrders": 10, "unassignedOrders": 0, "vehiclesUsed": 3, "totalDistanceKm": 99.49},
  "vehicles": [{
    "vehicleId": "VEH001", "capacity": 100.0, "currentAddress": "Karol Bagh, Delhi, India",
    "currentLatitude": 28.7041, "currentLongitude": 77.1025,
    "totalLoad": 66.0, "remainingCapacity": 34.0, "totalDistanceKm": 40.15,
    "assignedOrders": [{"sequence": 1, "orderId": "ORD002", "address": "Connaught Place, Delhi, India",
      "latitude": 28.6139, "longitude": 77.209, "packageWeight": 10.0, "priority": "MEDIUM",
      "distanceFromPreviousKm": 14.44}]
  }],
  "unassignedOrders": [{"orderId": "EDGE001", "address": "Connaught Place, Delhi, India", "packageWeight": 500.0,
    "priority": "HIGH", "reason": "Package weight 500.00 exceeds the capacity of every vehicle (largest capacity: 120.00)"}]
}
```
Vehicles with no orders are still listed (empty `assignedOrders`). With no orders and/or no vehicles the
endpoint still returns `200` (empty plan, or every order listed as unassigned with reason "No vehicles available").

### Errors
All errors share one shape and return `400` for client mistakes, `500` for unexpected failures:
```json
{"status":"error","message":"Validation failed",
 "details":["orders[0].latitude: latitude must be between -90 and 90"],"timestamp":"2026-11-22T10:00:00Z"}
```
Covered: missing/blank fields, coordinates out of range, weight/capacity <= 0, unknown priority,
malformed JSON, empty lists, duplicate IDs within one request.

## Algorithm (`GreedyDispatchStrategy`)
1. **Assignment - O(N x V).** Sort orders by priority (HIGH, MEDIUM, LOW), then heaviest first, then `orderId`.
   Give each order to the vehicle that still has capacity and whose current route end is nearest
   (smallest marginal distance). Orders that fit nowhere are reported as unassigned with a reason.
2. **Sequencing.** For each vehicle, take the greedy sequence and a nearest-neighbour sequence, improve both with
   2-opt, and keep the shorter. Routes longer than `dispatch.optimizer.max-stops-for-local-search`
   (default 1500) keep the greedy order, and each route's 2-opt has a 1 s time budget, to bound latency.
3. Distances use the Haversine formula (R = 6371 km); legs are rounded to 2 decimals in the response.

### Assumptions (the spec is silent)
- "High-priority orders are assigned first" is applied as *assignment order*: HIGH orders get capacity before
  MEDIUM and LOW. Inside a vehicle, stops are then sequenced purely for shortest distance.
- A route is an open path starting at the vehicle's current location (no return leg).
- The plan is computed on demand from the stored data and is not persisted; orders are not marked "dispatched".
  The same data therefore always produces the same plan (all ties are broken by ID).
- Posting an existing ID updates it (upsert). Data is never deleted by the API.
- Weights and capacities share one unit and may be decimal.

## Design
- Layers: controller -> service -> repository; DTO records are separate from JPA entities.
- Strategy pattern (`DispatchStrategy`) so a different optimizer (e.g. savings algorithm) can be plugged in.
- `DistanceCalculator` interface with a Haversine implementation, so the optimizer is testable with a stub.
- Upsert: one `findAllById` plus a batched `saveAll`; `@Version` makes new rows `persist` (no per-row SELECT).
- `@RestControllerAdvice` gives one consistent error format.

## Tests
- `HaversineDistanceCalculatorTest`: reference distances, symmetry, antipodes, invalid input.
- `OrderServiceTest` / `VehicleServiceTest` (`@DataJpaTest`): insert, update, mixed batch, duplicates.
- `GreedyDispatchStrategyTest`: priority vs capacity, capacity limits, nearest vehicle, spill-over, sequencing,
  unassignable orders, no vehicles/orders, determinism, 5,000-order performance.
- `DispatchControllerTest`, `DispatchPlanControllerTest` (`@WebMvcTest`): contracts and validation.
- `DispatchApiIntegrationTest`, `DispatchPlanIntegrationTest` (`@SpringBootTest`): full stack against H2.

## Limitations / next steps
- Heuristic, not an exact solver: the problem is a capacitated VRP, which is NP-hard.
- Swap H2 for PostgreSQL by changing the datasource properties; for very large inputs chunk the `findAllById` call.
