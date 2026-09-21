# ADR-TECH-011 - Spring application services and layer-owned validation

## Status

Accepted

## Linked ticket

None. This is a reusable technical application-boundary decision.

## Context

A hexagonal application needs validation, transactions, framework integration,
and database constraints without moving every rule into one validator or making
the domain depend on Spring. Completely framework-free application services can
also produce repetitive configuration and can leave method validation inactive
when a service is called through an unexpected path.

Rules have different owners: some describe the shape of an incoming contract,
some are permanent domain invariants, some require I/O, and others are final
persistence defenses. Treating them as one category creates duplication and
tests the wrong layer.

## Decision

### Spring application-service boundary

- Allow classes in the application-service layer to use only the declarative
  Spring annotations selected by this ADR: `@Service`, `@Validated`, and
  `@Transactional`.
- Use `@Service` for bean discovery instead of duplicating the same service with
  an additional `@Bean` declaration.
- Require every application service annotated with `@Service` to also carry
  `@Validated` when its incoming ports or commands declare Jakarta Validation
  constraints.
- Put transaction boundaries on application use cases. Use
  `@Transactional(readOnly = true)` only for strictly read-only operations.
- Keep application services independent of JPA, Spring Data, persistence
  entities, controllers, transport types, and infrastructure implementations.
- Keep domain models and application ports independent of Spring annotations.
- Enforce the limited Spring dependency with an executable architecture rule.

### Validation ownership

- Put structural input constraints on incoming ports and commands: required
  values, non-blank values, cascading validation, and technical contract limits.
- Guarantee value-object invariants at construction so values remain valid
  outside Spring.
- Keep domain models valid by construction, restoration, and every business
  operation. Domain code owns normalization, business formats, cross-field
  invariants, and lifecycle transitions.
- Normalize before validation only when normalization is part of the business
  contract. Do not silently repair canonical identifiers or references.
- Orchestrate rules requiring a repository, directory, clock, or external system
  in the application layer through ports.
- Validate known use-case preconditions before writing. Fail fast within each
  owner layer without promising a globally fixed error order across layers.
- Use database `NOT NULL`, `CHECK`, `UNIQUE`, relational constraints, and
  optimistic locking as the final defense for locally expressible persistent
  invariants.
- Do not ask the database to enforce domain transitions or remote-module
  availability.
- Translate known persistence violations into the owning module's error
  vocabulary. Preserve unknown technical failures as technical failures.
- Keep contract violations, business refusals, expected external-module results,
  and technical failures as distinct categories.
- Share a validation mechanism only when it is identical for every consumer,
  independent of business vocabulary, free of I/O, free of Spring and JPA, and
  free of module-specific error codes.
- Keep a shared rule with the concept it protects. Do not create generic entity
  validators or a catch-all validation utility.

### Verification ownership

| Owner | Unit-level proof | Integration-level proof |
| --- | --- | --- |
| Domain | Invariants, normalization, and transitions without Spring. | None unless a framework contract is intentionally part of the domain, which this ADR forbids. |
| Application service | Orchestration with substituted ports, tenant propagation, error mapping, and absence of writes after refusal. | Proxied bean behavior for method validation, transactions, read-only semantics, and selected real repository interactions. |
| Persistence adapter | Significant mapping and isolated technical-error translation when useful. | Real database behavior for mappings, queries, constraints, auditing, and optimistic locking. |
| Incoming adapter | Pure transformation only when it contains meaningful logic. | Framework slice tests for serialization, input validation, context resolution, status mapping, and error responses. |

- Do not mock a framework repository merely to prove simple delegation or the
  framework's own behavior.
- Do not test annotations by reflection when their observable framework effect
  can be tested through the proxied component.
- Use the lowest layer that can observe the behavior without reimplementing the
  responsible framework in a mock.
- Keep a small number of full vertical tests for critical paths instead of
  repeating every domain and application scenario through transport.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the allowed Spring annotations in application services;
- the architecture rule that enforces the allowlist and `@Validated` policy;
- the concrete package governed by the rule;
- the transaction and read-only conventions;
- the distribution of structural, domain, application, and persistence rules;
- the shared validation mechanisms and their neutrality justification;
- the error categories and module-owned error types;
- the unit, Spring-integration, database-integration, and transport-test strategy;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project.

## Alternatives considered

- Keep application services completely free of Spring and wire every service
  manually.
- Allow unrestricted Spring dependencies in the application layer.
- Put every validation in a generic validator per entity.
- Duplicate business invariants with Jakarta Validation.
- Trust incoming adapters as the only validation boundary.
- Delegate all integrity to the database.
- Put all business error codes in a shared technical module.
- Verify annotations only through reflection.
- Mock every framework dependency in unit tests.

## Justification

A small declarative Spring allowlist provides method validation and transaction
boundaries without granting the application layer access to persistence or
transport implementations. The domain remains ordinary Java and application
orchestration remains directly unit-testable.

Assigning each rule to the deepest layer that can own it prevents duplicated and
contradictory validation. Integration tests then prove framework and database
contracts where mocks cannot provide equivalent evidence.

## Positive consequences

- Validation and transaction behavior are consistent across application
  services.
- Domain invariants remain valid outside Spring.
- I/O-dependent rules stay explicit in application orchestration.
- Database constraints provide a final concurrent-write defense.
- Each rule has one reference test layer.
- Architecture tests prevent the Spring allowlist from expanding silently.

## Negative consequences / trade-offs

- Directly instantiated services do not execute method validation or
  transactions.
- Application services accept a deliberate limited dependency on Spring.
- Some invariants are defensively represented in both domain and database.
- Focused integration tests require more infrastructure than pure unit tests.
- Every known database violation needs explicit translation logic.
- The rule catalog and test matrix must evolve with new adapters and frameworks.

## Technical impact

- Spring application-service annotations and proxy behavior.
- Incoming-port and command constraints.
- Pure domain validation and application policies.
- Persistence constraints and error translation.
- Architecture rules enforcing the Spring allowlist.
- Unit, Spring integration, database integration, and incoming-adapter tests.

## Validation

- Verify that application services use only the allowed Spring types.
- Verify that every `@Service` in the governed package is also `@Validated` when
  required by the project contract.
- Verify structural constraints through a proxied bean.
- Verify domain invariants without Spring.
- Verify I/O-dependent policies with substituted ports and selected real
  integrations.
- Verify persistence guarantees against the real database engine.
- Verify transaction and read-only behavior through observable effects.
- Verify that shared validation code remains framework-free and
  business-neutral.

## Notes

Concrete package names, error enums, shared predicates, architecture-rule
identifiers, test-support types, and transport policies belong to the adopting
project's ADR.

This ADR owns validation placement and verification. The broader policy for
module-owned business error vocabularies and boundary translation is defined by
[ADR-TECH-012](ADR-TECH-012-module-owned-error-vocabularies-and-boundary-translation.md).
