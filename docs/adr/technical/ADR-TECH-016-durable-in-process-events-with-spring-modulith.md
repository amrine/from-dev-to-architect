# ADR-TECH-016 — Durable in-process events with Spring Modulith

## Status
Draft

## Linked ticket
Initial adoption: W001-T05; a notification consumer is proposed in the
unofficial Draft W001-T10. Reusable technical decision for a Spring Modulith
modular monolith.

## Context
Commands that must return a final business result cannot be safely orchestrated
through fire-and-forget events. Facts and secondary effects can be decoupled,
but a plain asynchronous listener can lose work if the process stops or the
listener fails. TeamPulse is currently a single Spring Boot modular monolith
with PostgreSQL and Spring Modulith 2.0.6; it is not operating an external
broker.

## Decision
- Use synchronous calls to public Java module contracts for commands whose
  result is required by the initiating use case or HTTP response.
- Use Spring application events only for facts/secondary effects, published
  after the owning business transition. Events are not commands and consumers
  do not return a business acknowledgement to the original request.
- When a durable listener is introduced, use Spring Modulith's persistent Event
  Publication Registry with the JPA integration and the shared PostgreSQL
  transaction. The registry records one publication per registered
  transactional listener and marks it complete only after that listener
  succeeds. Manage its schema with the repository's Flyway conventions.
- Run secondary consumers after commit. Treat delivery as at-least-once:
  listeners must be idempotent. Failed/incomplete publications remain
  observable and can be resubmitted; operator-facing retry controls and
  scheduled recovery policy are separate operational scope.
- Keep event payloads minimal and reference-based. Do not copy contact details,
  credentials or other PII into an event; a consumer that needs contact data
  calls the owning module's public read contract.
- In T05, define/publish the owner fact `UserInvited`, but do not create a
  notification listener or `tp-notification`. Organization activation and its
  `OrganizationActivated` fact are deferred to W008. T10 adds the notification
  consumers. Until such a listener exists, no notification delivery or
  historical replay is implied.
- Do not introduce Kafka, another external broker, event sourcing or business
  acknowledgement events in T05. A later service extraction may replace the
  delivery adapter without changing synchronous command ownership.

## Alternatives considered
- Use Spring events as commands and wait for listeners to produce the HTTP
  result.
- Use bare asynchronous listeners without persistent publication state.
- Add an external broker while all modules still run in one monolith.
- Publish full user contact data in events to avoid a directory lookup.
- Add a business `NotificationSent` acknowledgement to the provisioning API.

## Justification
Synchronous contracts preserve a clear command/result path inside one process.
Spring Modulith's publication registry adds transactional delivery tracking
when a real secondary listener exists, without requiring broker operations or
an acknowledgement protocol between business modules. Reference-only facts
reduce coupling and unnecessary propagation of personal data.

## Positive consequences
- A business command's response does not depend on a notification or other
  secondary effect.
- Registered listener publications can be persisted with the business
  transaction and recovered after failure.
- Event payloads remain small and avoid copying PII across module boundaries.
- The monolith avoids an external broker until distributed operation is
  justified.

## Negative consequences / trade-offs
- A publication registry is only useful once a listener is registered; events
  published before T10 do not create pending notification work for a future
  listener.
- At-least-once delivery requires idempotency and duplicate-handling in every
  listener.
- The publication log adds database schema, cleanup and operational recovery
  responsibilities.
- Synchronous Java calls remain process-local and need an adapter redesign if a
  module is extracted as a separate service.

## Technical impact
- Spring Modulith event APIs and the JPA-backed Event Publication Registry,
  version-managed by the existing Spring Modulith BOM.
- PostgreSQL/Flyway-managed publication schema in the application runtime.
- Owner-defined event records and public Java read contracts for consumers.
- T05 publishes `UserInvited`; W008 introduces organization activation and its
  `OrganizationActivated` fact. W001-T10 introduces notification listeners for
  the facts available at that point.

## Validation
- Verify that a failed owner transaction produces no committed business fact
  or listener publication.
- Verify publication records are created for registered listeners in the same
  transaction as the business transition.
- Verify successful listeners complete publications and failing listeners leave
  recoverable state.
- Verify duplicate/resubmitted delivery does not duplicate the consumer's
  business effect.
- Verify a synchronous command returns the module contract's final result and
  does not wait for a secondary consumer.
- Verify T05 cannot transition an organization to `ACTIVE` and does not publish
  `OrganizationActivated` before W008.
- Verify T05 has no broker, notification listener, or notification persistence.

## Risks
- The publication registry does not provide exactly-once side effects; external
  email providers can accept a message immediately before a process failure.
- Event retention and resubmission need an explicit operational policy before
  production use.
- T05-to-T10 is an intentional gap: previously created invitations are not
  notified retroactively.

## Implementation progress

The identity vertical publishes `UserInvited` after successful persistence.
The fact contains only the organization and user references; a focused service
test verifies the publish follows persistence and carries no user details.
The real HTTP test verifies an invitation is persisted in `INVITED` status.
There is no listener, Event Publication Registry, replay path or notification
delivery in T05. `./mvnw --batch-mode --no-transfer-progress -pl tp-identity
-am verify` passes. This ADR remains `Draft`.

The administration HTTP vertical continues to issue commands through the
public synchronous Java contracts of the owning modules. It adds no listener,
publication registry, replay path or notification side effect; no
`OrganizationActivated` event is published before W008. The full reactor
`verify` passes. This ADR remains `Draft`.

## References
- [Spring Modulith — Working with Application Events](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Modulith 2.0.x event publication API](https://docs.spring.io/spring-modulith/docs/2.0.x/api/org/springframework/modulith/events/core/package-summary.html)
