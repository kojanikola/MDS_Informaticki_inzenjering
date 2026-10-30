# MDS InformatiÄki inÅ¾enjering â€“ Java Backend task tracker

**Deadline / final commit:** Monday 2.11.2026 at 08:00
**Deliverables:** public git repo (grant MDS access), run/setup instructions, notes & assumptions, dockerized (Docker/Podman Compose)
**Contact:** Stefan D. JankoviÄ‡, stjankovic@mds.rs (questions by email; also confirm receipt with "Reply To All")
**Expected effort:** ~5â€“8 h. Priority: integration quality and engineering decisions, not business logic.

## Requirements (from the task PDF)
- Order API service: `POST /orders` with `{orderId, itemId, quantity}` publishes an event to the broker.
- Inventory Processing service: consumes the event, reserves stock if available, otherwise rejects. In-memory state is fine.
- Two independent services communicating via an open-source broker (no cloud-managed messaging).
- Java, no UI, dockerized, runnable locally with Compose.
- AI tools allowed, but every decision must be explainable in the interview.

## Decisions
| Topic | Decision |
|---|---|
| Broker | RabbitMQ |
| Build | Maven multi-module, Java 17, Spring Boot 3.x |
| Pattern | Standard Spring Boot MVC layering (controller / service / repository) |
| Reliability | Publisher confirms + persistent messages, idempotent consumer, retry with backoff + DLQ, result event back (reserved/rejected), Actuator health/metrics |
| Docs/tests | Swagger (springdoc), unit + integration tests |
| Language | English (code, comments, README, API docs) |

## Steps
- [ ] 0. Email: confirm receipt (Reply To All), create public repo, grant MDS access
- [x] 1. Project skeleton: Maven parent, `order-service`, `inventory-service`, shared events contract
- [x] 2. Order API: `POST /orders`, validation, error handling, Swagger
- [x] 3. RabbitMQ publishing: exchange/queue config, JSON events, publisher confirms
- [x] 4. Inventory consumer: in-memory stock, reserve/reject logic
- [ ] 5. Reliability: idempotency, retry/backoff, DLQ
- [ ] 6. Result event back + Actuator health/metrics, correlation IDs
- [ ] 7. Tests (unit + integration with Testcontainers)
- [ ] 8. Dockerfiles + docker-compose
- [ ] 9. README (run guide, architecture, assumptions, trade-offs)
- [ ] 10. Final check from a clean clone, final commit before the deadline
