# ADR-TECH-004 - Hexagonal boundaries inside business modules

## Status

Accepted

## Linked ticket

None. This is a reusable technical architecture decision.

## Context

A business module can have an explicit boundary toward other modules while its
internal code still accumulates dependencies from domain logic to frameworks,
database mappings, transports, or technical configuration. Package names alone
do not define which layer owns a contract or which direction dependencies may
take.

This decision applies to business modules that need a lightweight hexagonal
structure. It does not require tactical Domain-Driven Design, one class per use
case, or a particular dependency-injection framework.

## Decision

### Generic business-module contract

`<module-root>` represents the base package of one business module in the
adopting project.

| Zone | Generic package convention | Responsibility |
| --- | --- | --- |
| Domain | `<module-root>.domain..` | Business models, invariants, value objects, domain errors, and domain services. |
| Incoming ports | `<module-root>.application.port.in..` | Capabilities exposed by the application to incoming adapters. |
| Outgoing ports | `<module-root>.application.port.out..` | External capabilities required by application use cases. |
| Application services | `<module-root>.application.service..` | Use-case orchestration through domain models and ports. |
| Persistence adapter | `<module-root>.infrastructure.persistence..` | ORM entities, database repositories, persistence mappings, and outgoing-adapter implementations. |
| Transport adapter | `<module-root>.infrastructure.web..` | Controllers, transport DTOs, protocol validation, and error mapping. |
| Messaging adapter | `<module-root>.infrastructure.messaging..` | Publishers, consumers, and broker-specific mappings. |
| Technical configuration | `<module-root>.config..` | Framework assembly and module-specific technical configuration. |
| Public contracts | `<module-root>.api..` | Stable contracts exposed to other modules or consumers. |
| Public events | `<module-root>.events..` | Immutable facts exposed without internal or infrastructure types. |

The primary dependency direction is:

```text
infrastructure --> application --> domain
       config --> component assembly
```

These packages are created only when a real capability requires them. The table
defines ownership and dependency boundaries, not a requirement to create an
empty package tree.

- Keep the domain independent of dependency-injection, persistence, validation,
  transport, and messaging frameworks.
- Put incoming and outgoing ports in the application layer. Ports express what
  the application provides or requires; the adapters depend on the ports, never
  the reverse.
- Keep persistence entities, ORM mappings, database repositories, and
  persistence mappers in an outgoing infrastructure adapter.
- Keep transport controllers, requests, responses, and protocol-specific error
  mappings in an incoming infrastructure adapter.
- Keep messaging publishers, consumers, and broker-specific types in messaging
  adapters.
- Prevent direct dependencies between adapters. Collaboration between adapters
  goes through an application port or use case.
- Keep technical configuration free of business rules. Domain and application
  code must not depend on the configuration package.
- Keep public inter-module contracts and events independent of internal domain,
  application, infrastructure, and configuration types.
- Do not expose persistence entities or transport types through public contracts
  or events.
- Prevent dependency cycles between the internal architectural zones and between
  adapter categories.
- Treat framework usage inside the application layer as an explicit adoption
  policy. It may be forbidden or restricted to a documented set of declarative
  annotations, but it must never grant access to infrastructure implementations.
- Add packages and abstractions only when a real capability requires them. Do
  not create empty production structures to illustrate the architecture.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the business modules governed by the decision;
- the concrete package names for every architectural zone;
- the location of incoming and outgoing ports;
- the public-contract and public-event packages, if present;
- the permitted dependency matrix;
- the policy for framework dependencies in the application layer;
- any shared types that the domain or public contracts may use;
- the mechanism used to verify the boundaries;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project. Each project must evaluate its
context and explicitly adopt it through its own ADR.

## Alternatives considered

- Organize code only by technical type, such as controllers, services, and
  repositories.
- Put ports in the domain layer.
- Let application services depend directly on technical repositories.
- Use persistence entities as domain models and public data contracts.
- Let adapters call one another directly.
- Put dependency injection and persistence annotations in domain models.
- Apply a full tactical DDD structure to every business module.

## Justification

Application-owned ports describe use-case needs without making the domain aware
of orchestration or I/O. Infrastructure can change behind those ports while the
business core remains testable with ordinary Java objects.

Separating domain models from persistence and transport representations prevents
technical lifecycle, serialization, and storage concerns from becoming business
invariants. Autonomous public contracts also prevent a module from exposing its
internal implementation merely to satisfy another module.

The structure stays deliberately lightweight: it establishes dependency and
ownership boundaries without requiring aggregates, repositories, factories, or
services that have no current business purpose.

## Positive consequences

- Business logic remains testable without infrastructure frameworks.
- Persistence and transport technologies can evolve behind explicit adapters.
- Public module contracts do not leak internal or persistence types.
- Dependency direction is understandable from package ownership.
- Adapter-to-adapter coupling is replaced by explicit application collaboration.
- A module can grow incrementally without creating speculative abstractions.

## Negative consequences / trade-offs

- Mapping between domain, persistence, and transport representations adds code.
- More package boundaries must be maintained consistently.
- Framework convenience cannot be used indiscriminately in the business core.
- Small capabilities may initially appear more verbose than a direct
  controller-to-repository implementation.
- Structural boundaries do not prove business behavior, transaction semantics,
  query performance, or runtime isolation.

## Technical impact

- Package structure inside each business module.
- Ownership of use cases and incoming and outgoing ports.
- Separate infrastructure adapters for persistence, transport, and messaging.
- Autonomous public contracts and events.
- Technical configuration isolated from domain and application code.
- Automated or review-based dependency-boundary checks.

## Validation

- Verify that domain code has no framework or outward-layer dependencies.
- Verify that application code depends on ports and domain types, not adapter
  implementations.
- Verify that incoming adapters use incoming ports and outgoing adapters
  implement outgoing ports.
- Verify that persistence and transport types remain in their adapters.
- Verify that public contracts and events expose no internal implementation
  types.
- Verify that internal zones and adapter categories are free of cycles.
- Use behavioral tests separately; architecture validation is not a substitute
  for domain, application, persistence, or transport tests.

## Notes

This ADR defines reusable ownership and dependency boundaries. Package roots,
module names, framework annotations, rule identifiers, architecture-test tools,
and project-specific exceptions belong to the adopting project's ADRs.
