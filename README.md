# Dispatch Load Balancer

Spring Boot service that accepts delivery orders and a vehicle fleet and returns an
optimized dispatch plan: capacity-respecting, high-priority first, minimum travel distance
(Haversine).

## Requirements
- JDK 17+
- Maven 3.8+

## Run
```bash
mvn clean test          # run all tests
mvn spring-boot:run     # start on http://localhost:8080
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 console: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:mem:dispatchdb`, user `sa`, empty password)

## API

### POST /api/dispatch/orders
Upserts orders by `orderId`. Body: see `samples/orders.json`.
```bash
curl -X POST localhost:8080/api/dispatch/orders -H "Content-Type: application/json" -d @samples/orders.json
```
Response `200`: `{"message":"Delivery orders accepted.","status":"success"}`

### POST /api/dispatch/vehicles
Upserts vehicles by `vehicleId`. Body: see `samples/vehicles.json`.
```bash
curl -X POST localhost:8080/api/dispatch/vehicles -H "Content-Type: application/json" -d @samples/vehicles.json
```
Response `200`: `{"message":"Vehicle details accepted.","status":"success"}`

### Errors (400)
```json
{"status":"error","message":"Validation failed",
 "details":["orders[0].latitude: latitude must be between -90 and 90"],
 "timestamp":"2026-11-18T10:00:00Z"}
```
Covered: missing/blank fields, out-of-range coordinates, non-positive weight/capacity,
unknown priority, malformed JSON, empty lists, duplicate IDs within one request.

## Design notes
- Layers: controller -> service -> repository; DTO records separate from JPA entities.
- Upsert: one `findAllById` + batched `saveAll`; `@Version` makes new rows `persist` (no per-row SELECT).
- `DistanceCalculator` interface (Haversine impl) so the optimizer can be unit-tested with a stub.
- Global `@RestControllerAdvice` gives one consistent error shape.
