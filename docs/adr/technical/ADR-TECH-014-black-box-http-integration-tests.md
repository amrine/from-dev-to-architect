# ADR-TECH-014 — Black-box HTTP integration tests

## Status
Draft

## Context
Controller tests that replace the application path or run only an HTTP
framework slice do not prove the complete exchange, runtime wiring, context
propagation, persistence constraints or stored effects. Fast unit and layer
tests remain useful, but they answer different questions from a client-visible
HTTP test.

## Decision
- For behavior whose acceptance boundary is HTTP, exercise the real embedded
  server on a random port through an HTTP client.
- Keep the application and adapter path owned by the test real. Use the real
  database or other backing service when its constraints or persisted effects
  are part of the behavior being proved.
- Replace collaborators only outside the subsystem under test, and only at
  their public contracts. Do not substitute the tested controller's
  application use case as a stand-in for an end-to-end HTTP test.
- Cover observable outcomes such as serialization, validation, status and
  error mapping, context propagation, tenant isolation where applicable, and
  persisted effects.
- Keep web integration configuration separate from fast non-web test bases.
  Clean or isolate writes made by the server thread explicitly; a transaction
  on the test thread is not assumed to roll them back.
- A framework slice can still be useful for a focused adapter concern when the
  project adopts it. A slice does not replace the black-box HTTP evidence
  required by the owning project's contract.

## Alternatives considered
- Use only framework slices for controller coverage.
- Mock each controller's application port.
- Start the complete product application for every module-owned endpoint test.
- Convert every existing service/persistence test to start a web server.

## Justification
A real server and client expose the exchange a caller observes and can prove
the interaction of transport mapping, context handling, application behavior
and persistence. Keeping the tested subsystem real while substituting only
external public collaborators preserves useful module boundaries.

## Positive consequences
- HTTP behavior is proved through an actual request and response.
- Serialization, error mapping, context isolation and persisted writes can be
  checked together.
- Fast tests for domain, application and persistence concerns remain focused.
- Boundary doubles do not conceal defects inside the subsystem under test.

## Negative consequences / trade-offs
- Servers and backing services make these tests slower than unit or slice
  tests.
- Test applications need explicit boundary fixtures and database cleanup.
- Persistence assertions may be test-only when the public API has no read
  operation for the effect being checked.

## Technical impact
- A real-server test setup for the HTTP owners selected by the project.
- Reuse of the project's database/container support where persistence is part
  of the contract.
- Generic transport helpers remain test-scoped; business DSLs remain with
  their owning tests.

## Validation
- Verify tests start a real random-port server and issue HTTP requests through
  a client.
- Verify the tested application path and relevant backing services are real.
- Assert success and refusal cases, response bodies, context isolation and
  persisted outcomes that belong to the HTTP contract.
- Verify substitutes are limited to external public collaborators.
- Verify server-thread writes are cleaned or isolated between cases.

## Risks
- Shared database state can create order-dependent tests; cleanup and fixtures
  must be explicit.
- Black-box HTTP tests do not replace focused unit, transaction or database
  adapter tests for the layers that own those rules.

## Project adoption contract
Each adopting project records its local strategy in a project ADR, including:
- which HTTP owners and behaviors require black-box coverage;
- test applications, server/client setup and production wiring retained in the
  test path;
- backing services used to prove persistence and the boundary contracts that
  may be substituted;
- test-state isolation and cleanup conventions;
- the role, if any, of framework slices for focused adapter concerns, and why
  they do not replace required HTTP proof;
- local build/CI commands and the evidence currently available.

The technical ADR remains reusable. Its status does not automatically impose
the same test topology on another project.

## Implementation progress

The `W001-T05-http-test-support` branch adds a shared `RestTestClient` base and
a PostgreSQL cleaner that truncates application tables before and after each
HTTP test while preserving Flyway history. Identity, organization and team
each have a separate `RANDOM_PORT` base built on their existing test
application. Identity has a deterministic tenant provider; organization and
team retain only their existing public-contract stubs. The pre-existing
`AbstractIntegrationTest` classes remain `WebEnvironment.NONE`.

`./mvnw --batch-mode --no-transfer-progress -pl tp-test-support,tp-identity,tp-organization,tp-team -am test-compile`
compiles the support and module test sources. This foundation does not yet prove an HTTP request against a business
endpoint; that proof belongs to the first API vertical. The ADR remains `Draft` until the complete T05 decision is
validated.

## References
- [Spring Boot — Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
