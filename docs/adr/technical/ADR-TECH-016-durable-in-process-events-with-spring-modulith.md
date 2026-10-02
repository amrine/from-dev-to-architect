# ADR-TECH-016 — Durable in-process events with Spring Modulith

## Status
Draft

## Context
A command that must return its final business result cannot be safely
orchestrated through a fire-and-forget event. Events can carry facts to
secondary effects, but an untracked listener may lose work if the process stops
or the listener fails. Durable delivery also adds persistence and operational
responsibilities, so it should solve a real listener requirement.

## Decision
- Use synchronous calls to public module contracts for commands whose result
  is required by the initiating use case or transport response.
- Use application events for facts and secondary effects only. An event is not
  a command and its consumer does not return a business acknowledgement to the
  original caller.
- Keep payloads minimal and reference-based. Do not copy contact details,
  credentials or other personal data into an event; a consumer that needs such
  data calls the owning capability's public read contract.
- Do not add a durable publication registry, a placeholder listener or
  historical replay before a real listener exists and has a stated delivery
  requirement.
- When a real listener requires durable tracking, evaluate Spring Modulith's
  Event Publication Registry with the transaction integration selected by the
  project. Deliver secondary effects after commit, treat delivery as
  at-least-once and make listeners idempotent. Failed publications and recovery
  policy must be observable and documented.
- Keep operator retry controls, retention and scheduled recovery as explicit
  operational decisions. Do not add an external broker, event sourcing or
  acknowledgement protocol without a separate need.

## Alternatives considered
- Use events as commands and wait for listeners to return a business result.
- Add a durable registry before any listener needs it.
- Add placeholder consumers or replay prior facts to prepare a future
  notification capability.
- Use untracked asynchronous listeners when delivery must survive process
  failure.
- Add an external broker while in-process contracts meet the current need.
- Copy personal data into events to avoid a read through the owning contract.

## Justification
Synchronous module contracts keep command ownership and result handling clear.
Facts can be consumed independently. A persistent publication registry becomes
useful only when an actual listener needs durable delivery, avoiding schema and
recovery work before there is work to recover.

## Positive consequences
- A command response does not depend on a secondary consumer.
- Event payloads remain small and avoid unnecessary personal-data propagation.
- Durable in-process delivery can be added when a real listener requires it,
  without introducing broker operations prematurely.
- Listener retries have an explicit at-least-once and idempotency model.

## Negative consequences / trade-offs
- Without a listener and publication registry, events do not create pending
  work for a future consumer and are not replayed later.
- At-least-once delivery requires idempotency and duplicate handling.
- A registry adds schema, retention, monitoring and recovery responsibilities.
- Synchronous Java contracts are process-local and need a new adapter if a
  module is extracted into a separate service.

## Technical impact
- Public module contracts for synchronous commands and owner-defined event
  records for facts.
- Spring Modulith event publication APIs, with persistent registry integration
  only when selected for a real listener.
- Explicit transaction, listener and operational recovery policies at the
  adopting project.

## Validation
- Verify synchronous commands return the owning contract's result and do not
  wait for secondary consumers.
- Verify event payloads contain only the documented references and facts.
- If a registry is adopted, verify publication state follows the business
  transaction, successful listeners complete it and failures remain
  recoverable.
- Verify listener retries do not duplicate business effects.
- Verify no registry, placeholder listener or replay path exists solely to
  prepare a hypothetical future consumer.

## Risks
- A publication registry does not provide exactly-once external side effects.
- Retention and operator recovery need an explicit policy before production
  operation.
- Without a listener, an event is not a durable notification or deferred task.

## Project adoption contract
Each adopting project records its local decision in a project ADR, including:
- which commands remain synchronous and which facts may be published;
- event owner, payload fields and public read contract for any consumer data;
- whether a real listener currently exists and the delivery requirement that
  justifies a registry;
- transaction store/integration, after-commit behavior, idempotency and
  recovery/retention policy;
- tests for commit/rollback, delivery, failure and retry behavior;
- explicit confirmation that no future listener, notification or replay is
  implemented as a placeholder.

The technical ADR remains reusable. Its status does not automatically adopt a
publisher, listener or registry for another project.

## References
- [Spring Modulith — Working with Application Events](https://docs.spring.io/spring-modulith/reference/events.html)
