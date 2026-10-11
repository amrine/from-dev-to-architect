# ADR-TECH-014 — HTTP boundary integration tests

## Status
Draft

## Context
Tests that replace the application path or run only an HTTP framework slice do
not prove complete application wiring, context propagation, persistence
constraints or stored effects. A full-context `MockMvc` test exercises Spring
MVC dispatch without starting the embedded server. A random-port test exercises
the embedded server and an HTTP client as well. Fast unit and layer tests remain
useful, but they answer different questions from these integration tests.

## Decision
- Use a real embedded server on a random port when the acceptance boundary
  includes server startup, network transport, or a generated/real HTTP client.
- A project may use `MockMvc` with the full application context when the
  acceptance boundary is Spring MVC dispatch, request/response mapping,
  application wiring, and persisted effects. Its adoption contract must state
  that it does not prove embedded-server, network, or client behavior.
- Keep the application and adapter path owned by the test real. Use the real
  database or other backing service when its constraints or persisted effects
  are part of the behavior being proved.
- Replace collaborators only outside the subsystem under test, and only at
  their public contracts. Do not substitute the tested controller's
  application use case as a stand-in for an end-to-end HTTP test.
- Cover observable outcomes such as serialization, validation, status and
  error mapping, context propagation, tenant isolation where applicable, and
  persisted effects. Select the client boundary according to the behavior the
  project needs to prove.
- Keep web integration configuration separate from fast non-web test bases.
  Clean or isolate writes made by the server thread explicitly; a transaction
  on the test thread is not assumed to roll them back.
- A framework slice can still be useful for a focused adapter concern when the
  project adopts it. A slice does not replace the HTTP boundary evidence
  required by the owning project's contract.

## Alternatives considered
- Use only framework slices for controller coverage.
- Mock each controller's application port.
- Start the complete product application for every module-owned endpoint test.
- Convert every existing service/persistence test to start a web server.

## Justification
A real server and client prove the exchange a network caller observes.
Full-context `MockMvc` proves Spring MVC and application behavior without
starting that server. Both keep the tested subsystem real and can use real
persistence; the project adoption must not claim network evidence from
`MockMvc`. Substituting only external public collaborators preserves useful
module boundaries.

## Positive consequences
- MVC behavior is proved through the selected Spring MVC test client; network
  behavior is proved only by random-port tests.
- Serialization, error mapping, context isolation and persisted writes can be
  checked together.
- Fast tests for domain, application and persistence concerns remain focused.
- Boundary doubles do not conceal defects inside the subsystem under test.

## Negative consequences / trade-offs
- A full application context and backing services make these tests slower than
  unit tests; a random-port test also starts the embedded server.
- Test applications need explicit boundary fixtures and database cleanup.
- Persistence assertions may be test-only when the public API has no read
  operation for the effect being checked.
- `MockMvc` tests do not exercise embedded-server configuration, network
  transport, or generated HTTP clients.

## Technical impact
- Full-context MVC or real-server test setup for the HTTP owners selected by
  the project.
- Reuse of the project's database/container support where persistence is part
  of the contract.
- Generic transport helpers remain test-scoped; business DSLs remain with
  their owning tests.

## Validation
- Verify each test uses the client boundary named by the project adoption:
  full-context `MockMvc` for MVC behavior, or a real random-port server and
  client when embedded-server or network behavior is part of acceptance.
- Verify the tested application path and relevant backing services are real.
- Assert success and refusal cases, response bodies, context isolation and
  persisted outcomes that belong to the HTTP contract.
- Verify substitutes are limited to external public collaborators.
- Verify server-thread writes are cleaned or isolated between cases.

## Risks
- Shared database state can create order-dependent tests; cleanup and fixtures
  must be explicit.
- Neither full-context MVC nor black-box HTTP tests replace focused unit,
  transaction or database adapter tests for the layers that own those rules.

## Project adoption contract
Each adopting project records its local strategy in a project ADR, including:
- which HTTP owners and behaviors require boundary integration coverage;
- the selected boundary for each owner: full-context `MockMvc`, or a
  random-port server and HTTP client, including the behavior each choice proves
  and does not prove;
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

## References
- [Spring Boot — Testing Spring Boot applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
