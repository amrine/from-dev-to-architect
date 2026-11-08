# ADR-TECH-014 — Spring Boot HTTP boundary integration tests

## Status

Draft

## Linked ticket

None. This is a reusable technical testing decision for modular Java
applications using Spring Boot.

## Context

HTTP tests can prove different boundaries. A web slice or a test that mocks the
application use case does not prove the complete application wiring or
persistence effects. Full-context `MockMvc` exercises Spring MVC dispatch and
application behavior without starting an embedded server. A random-port test
exercises the embedded server and an HTTP client through an actual HTTP
exchange.

Applications may also use non-web Spring test contexts for service and
persistence integration tests. Starting a web server for those tests adds
startup cost without proving behavior they depend on.

This ADR targets applications that use Spring Boot for HTTP, PostgreSQL for
persistence, and Flyway for schema migrations. It addresses HTTP acceptance
tests where server behavior, transport, or persisted effects are part of the
acceptance boundary.

## Decision

### HTTP boundary

- For acceptance tests that must prove server startup, HTTP transport, or
  behavior through an HTTP client, use
  `@SpringBootTest(webEnvironment = RANDOM_PORT)` and send requests through an
  HTTP client.
- Use full-context `MockMvc` only when the required boundary is Spring MVC
  dispatch, request/response mapping, application wiring, and any relevant
  persisted effects. Record that choice explicitly: `MockMvc` does not prove
  embedded-server configuration, network transport, or HTTP-client behavior.
- A web slice such as `@WebMvcTest`, or a test that mocks the controller's
  application use case, does not satisfy a required HTTP boundary test.
  Such tests may still cover a focused adapter concern.
- When an OpenAPI-generated client is part of the project's client strategy,
  exercise that client in at least one boundary test. Compiling the generated
  client alone does not prove that it works against the running server.

### Application and persistence path

- Keep the controller, application services, and other components owned by
  the subsystem under test real.
- When persistence behavior is part of the acceptance boundary, use PostgreSQL
  Testcontainers with the application's Flyway migrations. Supply the database
  connection through Spring Boot's supported Testcontainers integration or
  dynamic properties.
- For an isolated module test, replace only collaborators outside the
  subsystem under test, using their public contracts. Do not replace internal
  components to make the test context start.
- When acceptance depends on collaboration between multiple modules, include
  those modules in an integration test that exercises their real collaboration.
  Keep isolated module tests focused on the module they own.

### Test lifecycle and state

- Keep Web integration-test configuration separate from non-Web Spring test
  contexts, including contexts configured with `WebEnvironment.NONE`.
- Clean or isolate database writes performed by the server. A transaction on
  the test thread is not assumed to roll back writes performed on the server
  thread.
- Keep business scenario DSLs with the tests that own those scenarios. Shared
  test support may provide generic HTTP setup, diagnostics, or assertions, and
  remains test-scoped.

## Alternatives considered

- Use only `@WebMvcTest` or another web slice for HTTP acceptance coverage.
- Mock the tested controller's application use case.
- Start an embedded server for every service or persistence integration test.
- Start the full product application for every module-owned endpoint test.
- Use a developer-managed PostgreSQL instance for repeatable integration tests.

## Justification

A random-port test exercises the server and client boundary that an HTTP caller
observes. PostgreSQL Testcontainers and Flyway provide a reproducible way to
verify database behavior and migrations when persistence is part of acceptance.

A focused application context keeps module ownership visible. Replacing only
collaborators outside the tested subsystem preserves its internal wiring while
allowing isolated module tests. Separate non-Web contexts keep tests that do
not depend on HTTP from paying the cost of server startup.

## Positive consequences

- HTTP acceptance tests exercise the embedded server and a real HTTP exchange.
- Persistence outcomes can be checked against PostgreSQL and the application's
  actual migrations.
- Tests distinguish MVC-level evidence from network-level evidence.
- Tests for separate modules can remain focused without hiding defects inside
  the module under test.
- Existing non-Web integration tests keep their intended execution boundary.

## Negative consequences / trade-offs

- Random-port tests start an application server and PostgreSQL container, so
  they run more slowly than unit tests or web slices.
- Module-focused contexts require explicit fixtures for external contracts.
- Server-side database writes require deliberate cleanup or isolation.
- Generated clients and their DSLs add build and test maintenance when adopted.

## Technical impact

- Spring Boot test contexts configured for either MVC testing or random-port
  HTTP testing, according to the required acceptance boundary.
- PostgreSQL Testcontainers and Flyway migrations when persistence behavior is
  tested.
- An HTTP test client; an OpenAPI-generated client when that is part of the
  project's client strategy.
- Test-scoped shared transport support, where several test suites need it.
- Explicit fixtures for external public contracts and database state isolation.

## Validation

- Verify that each HTTP acceptance test uses the boundary it claims to prove:
  full-context `MockMvc` for MVC behavior, or a random-port server and HTTP
  client for embedded-server or network behavior.
- For random-port tests, verify that the test context starts a real embedded
  server and that the test sends HTTP requests to it.
- When PostgreSQL persistence behavior is in scope, verify that PostgreSQL
  Testcontainers starts, Flyway migrations run, and assertions inspect the
  resulting application state.
- When an OpenAPI-generated client is adopted, verify that at least one test
  executes it against the running server.
- Assert relevant success and refusal responses, validation and error payloads,
  and persisted effects.
- Verify that only collaborators outside the tested subsystem are replaced.
- Verify that server writes are cleaned or isolated between tests.
- Run the focused HTTP test suite and the project's full build lifecycle.

## Risks

- Shared database state can make tests order-dependent without explicit
  fixtures and cleanup.
- Full-context MVC and random-port tests do not replace domain, application,
  transaction, or persistence-adapter tests owned by other layers.
- A generated client and its specification can agree while the running server
  still violates the documented contract; runtime tests remain necessary.

## Project adoption contract

Each adopting project records, for every HTTP owner:

- the HTTP behaviors that require boundary integration coverage;
- the selected boundary: full-context `MockMvc` or a random-port server and
  HTTP client, with the evidence that choice provides;
- the Spring Boot test application or configuration used;
- whether an OpenAPI-generated client is used and how tests exercise it;
- whether PostgreSQL persistence and Flyway migrations are part of the test;
- which external public contracts may be replaced in isolated tests;
- how server-side database writes are cleaned or isolated;
- the focused and full-build validation commands, with the evidence currently
  available.

The project ADR records local classes, module names, fixtures, and commands.
Those local details do not change the technical boundary defined here.

## References

- [ADR-TECH-003 — PostgreSQL integration tests with Testcontainers](ADR-TECH-003-postgresql-integration-tests-with-testcontainers.md)
- [ADR-TECH-013 — OpenAPI contracts and generated clients](ADR-TECH-013-openapi-contracts-and-generated-test-clients.md)
