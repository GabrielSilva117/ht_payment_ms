# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot microservice (Java 21, Spring Boot 4.0.2, Spring Cloud 2025.1.0) handling payments for the "hardware today" monolith. Registers with Eureka, exposed both via REST (called from the monolith's Feign client) and via RabbitMQ (async dispatch).

## Commands

- Build: `./mvnw clean package`
- Run: `./mvnw spring-boot:run`
- All tests: `./mvnw test`
- Single test: `./mvnw test -Dtest=HtPaymentMsApplicationTests`
- Single test method: `./mvnw test -Dtest=HtPaymentMsApplicationTests#contextLoads`
- Docker build: `docker build -t ht_payment_ms .`

Required env vars (RabbitMQ): `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`. Required env vars (Postgres): `SPRING_DATASOURCE_HOST`, `SPRING_DATASOURCE_PORT`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`. Required env vars (Stripe): `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`. Server port is random (`server.port: 0`) since the service registers itself in Eureka (`http://localhost:8761/eureka/`).

## Architecture

Two entry points converge on the same processing logic in `PaymentService`:

1. **REST path**: `PaymentController` (`POST /payment`) → `PaymentService.processPayment(PaymentDTO)` — used synchronously by the monolith's Feign client. Returns immediately with `PaymentStatus.PROCESSING` for CARD payments (gateway confirmation is async, see below).
2. **AMQP path**: `PaymentConsumers` (`@RabbitListener` on `payment_dispatch` queue) → `PaymentService.processPayment(String payload, Channel, Message)` — manual ack/nack, no auto-ack (see `application.yaml` `acknowledge-mode: manual`). Acks right after the row is durably persisted as PENDING, before the gateway call runs.

Both paths funnel into `PaymentFactory`, which picks the right `PaymentProcessor` implementation by `PaymentType` (currently only `CardProcessor` for `PaymentType.CARD`). Add new payment types by: adding to `PaymentType` enum, implementing `PaymentProcessor`, and registering the DTO subtype in `PaymentDTO`'s `@JsonSubTypes`.

`PaymentDTO` uses Jackson polymorphic deserialization (`@JsonTypeInfo` on `type` field) to resolve into concrete subtypes like `CardPaymentDTO`.

On success, `PaymentService`/`StripeWebhookController` publish a `NotificationDTO` via `NotificationPublisher` to the `notification.exchange` (routing key `email.create`) for the notification service to consume. `PaymentPublisher` produces payment lifecycle events onto `payment.exchange` (routing keys like `payment.card.created`), for other services to consume.

### Payment lifecycle / persistence

`Payment` entity (Postgres via Flyway, H2 in tests) tracks each payment through `PaymentEntityStatus`: `PENDING` (row created) → `PROCESSING` (CARD: Stripe `PaymentIntent` created, awaiting webhook) → `SUCCEEDED`/`FAILED`. `PaymentPersistenceService` owns all transitions (`createPending`, `markProcessing`, `markSucceeded`, `markFailed`). `cartId` is unique — a repeat request for the same cart returns the existing payment's status (`DuplicatePaymentException`) instead of double-processing.

### Stripe integration (CARD payments)

`CardPaymentDTO` carries only a `paymentMethodId` — the card is tokenized client-side (Stripe.js/Elements in the checkout frontend, `../hardware_today_front_angular`) before the monolith's Feign call ever reaches this service, so no raw PAN/CVV touches `ht_payment_ms`. `CardProcessor` calls `PaymentMethod.retrieve(paymentMethodId)` (to read `card.last4` for display/support) then `PaymentIntent.create` with `setPaymentMethod(paymentMethodId)` and `confirm(true)`, then always returns a `PENDING` outcome — the `StripeWebhookController` (`POST /payment/webhook/stripe`) is the sole source of truth for finalization, handling `payment_intent.succeeded`/`payment_intent.payment_failed`/`.canceled` and flipping the `Payment` row via `PaymentPersistenceService`. This avoids the processor and the webhook racing to decide the outcome. The webhook verifies `Stripe-Signature` via `Webhook.constructEvent(...)` (needs the raw body, not a parsed DTO) and is idempotent — a retried event for an already-terminal `Payment` is a no-op.

Local dev webhook delivery: `ht_services`' docker-compose runs a `stripe-cli` container (`stripe listen`) forwarding to the `gateway` service's Eureka discovery-locator route (`http://gateway:8087/ht-payment-ms/payment/webhook/stripe`), not directly to `payment-ms` (which has no published port). `stripe listen` prints a new ephemeral `STRIPE_WEBHOOK_SECRET` each run — it must be copied in manually, compose can't automate it.

### RabbitMQ topology (`MQConfig`)

- `payment.exchange` (topic) — bound to `payment_dispatch` queue via `payment.*.created` and `payment.card.*`.
- `notification.exchange` (direct) — bound to `email_dispatch` queue (binding currently commented out).
- AMQP error handling in `PaymentService.handleError`: currently always `basicNack` with no requeue (sends straight to DLQ) — the retry-count logic is scaffolded but disabled (commented out).

### Known oddity

`pom.xml` has both `org.springframework.boot:spring-boot-flyway` (non-standard artifact id, likely a mistake) and the real `org.flywaydb:flyway-core`/`flyway-database-postgresql` — flagged, not fixed.
