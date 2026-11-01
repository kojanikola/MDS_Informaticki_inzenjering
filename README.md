# Order & Inventory: event-driven integration over RabbitMQ

Two independent Spring Boot services that communicate only through a message broker (RabbitMQ):

- **order-service** (port 8081): exposes `POST /orders`, publishes an `OrderCreatedEvent`, and tracks the order status from the inventory result.
- **inventory-service** (port 8082): consumes order events, reserves stock from an in-memory inventory (or rejects the order), and publishes an `OrderResultEvent` back.

The focus is the reliability of the integration between the services, not the business logic (task: *MDS Informatički inženjering – Tehnički zadatak*).

## Architecture

```
                        orders.exchange (topic)
                      ┌───────────────────────────┐
 Client               │ order.created             │
   │ POST /orders     ▼                           │
   ▼                inventory.order-created ──▶ inventory-service
order-service ──────▶ (queue)                       │ reserve / reject
   ▲  ▲                                             │
   │  │ GET /orders/{id}                            ▼
   │  └──────────── order.results ◀──── order.result.reserved | order.result.rejected
   └─ consumes results (queue)
       failures ──▶ orders.dlx ──▶ *.dlq (dead-letter queues)
```

| Module | Purpose |
|---|---|
| `events-contract` | Shared library: event records (`OrderCreatedEvent`, `OrderResultEvent`) and broker topology names. It is a plain jar, not a service. |
| `order-service` | Order API (MVC: controller → service → repository), RabbitMQ publisher/consumer. |
| `inventory-service` | Inventory processing (MVC: controller → service → repository), RabbitMQ consumer/publisher. |

Both services declare the same exchange, queues and bindings (names come from `events-contract`), so either can start first and orders are buffered while inventory is down.

## Run it

Requirements: Docker (or Podman) with Compose.

```bash
docker compose up --build -d
```

This starts RabbitMQ, `order-service` and `inventory-service`; the services wait for the broker to be healthy. Stop and clean up with:

```bash
docker compose down -v
```

| URL | What |
|---|---|
| http://localhost:8081/swagger-ui.html | Order API (Swagger UI) |
| http://localhost:8082/swagger-ui.html | Inventory API (Swagger UI) |
| http://localhost:15672 | RabbitMQ management UI (`guest` / `guest`) |
| http://localhost:8081/actuator/health, http://localhost:8082/actuator/health | Health (includes broker connectivity) |
| http://localhost:8081/actuator/metrics, http://localhost:8082/actuator/metrics | Metrics |

### Try it

```bash
# Place an order (item-1 has 10 in stock)
curl -i -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: demo-1" \
  -d '{"orderId":"123","itemId":"item-1","quantity":2}'

# Status: ACCEPTED, then RESERVED or REJECTED once inventory has answered
curl http://localhost:8081/orders/123

# Inventory side
curl http://localhost:8082/inventory/stock
curl http://localhost:8082/inventory/reservations/123
```

Seed stock (`inventory-service/src/main/resources/application.yml`): `item-1`: 10, `item-2`: 5, `item-3`: 0. Anything else is an unknown item and is rejected. Try `quantity` larger than the stock, or `item-3`, to see a rejection.

### API summary

| Service | Endpoint | Result |
|---|---|---|
| order | `POST /orders` | `202` accepted and confirmed by the broker; `400` invalid payload (RFC 7807 problem with field errors); `409` the `orderId` already exists; `503` broker did not accept the event (the id is released, so retrying the same request is safe) |
| order | `GET /orders/{orderId}` | `200` with status `ACCEPTED` / `RESERVED` / `REJECTED` (+ reason); `404` unknown |
| inventory | `GET /inventory/stock` | current available quantity per item |
| inventory | `GET /inventory/reservations/{orderId}` | `200` processing result; `404` not processed (yet) |

## Reliability mechanisms

| Concern | What is done |
|---|---|
| Message loss on publish | Publisher **confirms** + `mandatory` flag: `POST /orders` returns 202 only after RabbitMQ acknowledged the message and it was routed to a queue. Nack, unroutable message, confirm timeout or broker down → `503`, so an order is never reported as accepted when it was not. |
| Message loss in the broker | Durable exchanges and queues, **persistent** messages. |
| Message loss in the consumer | Ack-after-processing: a message is acknowledged only after processing and the result publish succeeded. |
| Duplicate delivery (at-least-once) | **Idempotent consumer** keyed on `orderId`: a redelivered event returns the original outcome and does not reserve stock twice, also under concurrent duplicates. The result event is re-published for duplicates, so a delivery that failed after processing is repaired by the redelivery. |
| Transient failures | **Retry with exponential backoff** (3 attempts, 1s → 2s → …, max 10s; configurable via `inventory.retry.*`). |
| Poison messages | Invalid events (bad quantity, blank ids) and unparseable payloads skip retries and go straight to the **dead-letter queue** (`inventory.order-created.dlq`). After the retries are exhausted, messages are dead-lettered too. The result queue has its own retry + DLQ (`order.results.dlq`). |
| Consumer down | Queues are declared by both services, so orders wait in `inventory.order-created` until inventory is back. |
| Loose coupling | Consumers convert to the type their listener declares and ignore the producer's `__TypeId__` header; events are JSON. |
| Observability | `X-Correlation-Id` (generated if missing) is returned to the caller, put in every log line (MDC) and carried in the AMQP `correlationId` across both services and both directions. Actuator health (includes RabbitMQ) and custom counters: `orders.events.published`, `orders.events.publish.failed`, `inventory.orders.processed{status}`, `inventory.orders.duplicates`. |

### Broker topology

| Name | Type / purpose |
|---|---|
| `orders.exchange` | topic exchange for all order events |
| `order.created` → `inventory.order-created` | order → inventory |
| `order.result.*` → `order.results` | inventory → order (`order.result.reserved`, `order.result.rejected`) |
| `orders.dlx` | direct dead-letter exchange |
| `inventory.order-created.dlq`, `order.results.dlq` | dead-letter queues |

## Develop and test

Requirements: Java 17, Maven 3.9+, and Docker for the integration tests.

```bash
mvn verify          # unit + integration tests (63)
```

- **Unit tests**: controllers (MockMvc), services, repositories (including a concurrency test that proves stock is never oversold), publishers (simulated ack / nack / unroutable / timeout / broker down), retry policy.
- **Integration tests** (Testcontainers, real RabbitMQ; skipped automatically if Docker is unavailable): REST call → persistent, correlated event; result event → order status; reserve / reject / duplicate / poison message / garbage payload; failure retried then dead-lettered; transient failure recovers.
- Docker image builds skip tests (`-DskipTests`) because the integration tests need Docker.

Running the services outside Docker (needs a RabbitMQ on `localhost:5672`, e.g. `docker run -p 5672:5672 -p 15672:15672 rabbitmq:3-management`):

```bash
mvn -q -DskipTests package
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar
```

Connection settings can be overridden with `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`.

## Decisions, assumptions and trade-offs

- **RabbitMQ over Kafka**: for a small command/result flow, per-message ack, DLX/DLQ, publisher confirms and routing are built in and need little setup. Kafka would fit replay or very high throughput better.
- **Shared `events-contract` module**: keeps both sides in sync and a contract change breaks the build instead of production. The cost is build-time coupling; the alternative is duplicating DTOs or using a schema registry.
- **Queue declared by both services** with identical arguments (RabbitMQ rejects a redeclaration with different arguments). This is what buffers orders while inventory is down. If you change queue arguments, delete the old queue on a long-lived broker first.
- **`orderId` is the idempotency key and is assumed unique per order.** The Order API answers `409 Conflict` for an id it has already accepted (the id is claimed atomically before publishing), so a rejected order is final and a new attempt needs a new id. As a second line of defence the inventory service also deduplicates by `orderId`: a repeated event (e.g. redelivery, or a client retry after an unconfirmed publish) returns the first outcome and never reserves stock twice.
- **No database** (allowed by the task): stock, reservations and order states live in memory and reset on restart. In production the reservation record and the stock update would share one transaction and a unique key on `orderId`; the order service would use a transactional **outbox** to publish events atomically with its own state.
- **`202 Accepted`, not `201`**: the order is accepted for asynchronous processing; its outcome is read via `GET /orders/{id}` (or the result event).
- **Client retries after a `503`** are safe: the claimed `orderId` is released when publishing fails. Edge case: if the confirm times out but the broker did receive the message, the retry publishes a second event, which inventory absorbs through `orderId`. A retry after a `202` gets `409`; the client reads the outcome with `GET /orders/{id}`.
- **Publish confirm logic is duplicated** in both services on purpose (they are independent deployables); a shared library would couple them further.
- **Security is out of scope**: default `guest/guest` broker credentials, no auth on the APIs, `show-details: always` on health. Not for production use.
- **Not done on purpose**: UI, a real stock database, message schema versioning, distributed tracing (a correlation id is used instead), TLS.

## Repository layout

```
.
├── pom.xml                  parent (Java 17, Spring Boot 3.3)
├── events-contract/         shared events + topology names
├── order-service/           Order API  (+ Dockerfile)
├── inventory-service/       Inventory processing (+ Dockerfile)
├── docker-compose.yml       RabbitMQ + both services
└── TRACKER.md               task checklist and decisions
```
