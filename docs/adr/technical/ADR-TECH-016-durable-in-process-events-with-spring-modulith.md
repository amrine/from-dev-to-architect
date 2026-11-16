# ADR-TECH-016 — In-process application events with Spring Modulith

## Status

Accepted

## Linked ticket

None. This is a reusable technical decision for modular Java applications using
Spring Modulith.

## Context

Commands and events have different responsibilities. A command that needs to
return a business result to its caller must obtain that result from the
capability that owns the operation. An application event represents a fact
that has already occurred and may trigger secondary behavior.

Spring application events provide in-process communication between application
modules. A listener that runs asynchronously after transaction commit can
fail or be skipped if the process stops before delivery. Spring Modulith's
Event Publication Registry records publications for transactional listeners
as part of the business transaction and tracks their completion. It adds
persistence, retention, monitoring, and recovery responsibilities.

## Decision

### Commands and events

- Use synchronous calls to public module contracts for commands whose result
  is needed by the initiating use case or its transport response.
- Use application events to communicate facts and trigger secondary effects.
  Do not use an event as a command or require its listener to return a business
  acknowledgement to the original caller.
- Publish events from the module that owns the fact. Keep event payloads
  minimal and limited to the references and facts consumers need.
- Do not include credentials, contact details, or other personal data in an
  event. A consumer that needs additional data reads it through the owning
  capability's public contract.
- Do not make a command's success depend on an event consumer. If a capability
  must complete before the command can succeed, call its public contract
  synchronously.

### Listener execution and durable delivery

- Use Spring Modulith's `@ApplicationModuleListener` for asynchronous,
  transaction-bound module listeners whose effects are secondary to the
  publishing command.
- Treat asynchronous in-process delivery without a publication registry as
  non-durable. It may be lost if the process stops before the listener runs.
- Add Spring Modulith's Event Publication Registry only when a real listener
  has a stated requirement for recoverable delivery across process failure.
  Configure the registry with the project's transaction integration so the
  publication record participates in the business transaction.
- Treat delivery with retries as at-least-once. Make listeners idempotent and
  define how duplicate effects are prevented.
- Make failed publications observable and define how operators inspect and
  resubmit them. Define retention and cleanup for completed publications.
- Do not add a registry, listener, replay mechanism, or notification path as a
  placeholder for a hypothetical future consumer.
- Do not add an external broker, event sourcing, or an acknowledgement protocol
  without a separate requirement and architecture decision.

## Alternatives considered

- Use an event as a command and wait for a listener to return the result.
- Call a public module contract synchronously when the caller needs its result.
- Run a secondary listener asynchronously without durable tracking when loss is
  unacceptable.
- Add the publication registry before a real listener requires recoverable
  delivery.
- Add an external broker while in-process module contracts meet the need.
- Copy personal data into events to avoid reading it through the owning
  capability.

## Justification

Synchronous public contracts keep command ownership and result handling
explicit. Events communicate facts to secondary consumers without making their
results part of the original response.

Spring Modulith provides an in-process listener model and an Event Publication
Registry for applications that need recoverable delivery. Making registry use
conditional on an actual delivery requirement avoids persistence and recovery
work when no listener needs it.

## Positive consequences

- Command results remain owned by the capability that performs the operation.
- Event consumers remain secondary to the publishing command.
- Event payloads avoid unnecessary propagation of personal data.
- Recoverable delivery can use Spring Modulith without requiring an external
  broker.
- Listener retries have an explicit at-least-once and idempotency model.

## Negative consequences / trade-offs

- Asynchronous in-process events without a registry are not durable.
- The registry adds persistence, retention, monitoring, and recovery
  responsibilities.
- At-least-once delivery requires idempotency and duplicate handling.
- Synchronous Java contracts are process-local and require another adapter if
  a module is extracted into a separate service.
- The registry does not provide exactly-once external side effects.

## Technical impact

- Owner-defined event types published through Spring's application event
  mechanism.
- Spring Modulith `@ApplicationModuleListener` for asynchronous,
  transaction-bound module listeners.
- Spring Modulith Event Publication Registry and its transaction integration
  when recoverable delivery is required.
- Listener idempotency, failure observability, retry, and retention policies.
- No external broker unless a separate requirement justifies one.

## Validation

- Verify commands that need another capability's result use its synchronous
  public contract and do not wait for an event acknowledgement.
- Verify event payloads contain only documented references and facts.
- Verify publishing a fact is consistent with the transaction that produces
  that fact.
- For asynchronous listeners, verify execution follows the configured
  transaction boundary and does not determine the original command result.
- If the registry is adopted, verify publication records follow business
  commit and rollback, successful listeners complete their records, and
  failures remain observable and recoverable.
- Verify listener retries do not duplicate business effects.
- Verify retention and cleanup for completed publication records.
- Verify no registry, listener, notification, or replay path exists only to
  prepare a hypothetical future consumer.

## Risks

- The registry does not make external side effects exactly-once.
- Retries can repeat effects unless listeners are idempotent.
- Retention and operator recovery need explicit policies before production use.
- Without a listener, an event is not a durable notification or deferred task.

## Project adoption contract

Each adopting project records in a project ADR:

- which operations use synchronous public contracts and which facts are
  published as events;
- the module that owns each event and the fields in its payload;
- each real listener and whether it is synchronous or asynchronous;
- whether a listener requires durable delivery and the requirement that
  justifies the Event Publication Registry;
- the transaction store and integration used by the registry;
- listener idempotency, failure observability, retry, retention, and recovery
  policies;
- tests for commit, rollback, listener delivery, failure, retry, and duplicate
  effects;
- confirmation that no placeholder listener, notification, registry, or
  replay path has been added.

The project ADR records local event names, module names, listener classes,
configuration, and validation commands. Those local details do not change the
technical decision defined here.

## References

- [Spring Modulith — Working with Application Events](https://docs.spring.io/spring-modulith/reference/events.html)
