# ADR-TECH-014 — Black-box HTTP integration tests

## Status
Draft

## Linked ticket
Initial adoption: W001-T05. Reusable technical decision.

## Context
A controller test that mocks its use cases or runs only an MVC slice does not
prove the complete HTTP boundary, Spring wiring, tenant propagation, database
constraints or persistence effects. TeamPulse already has module-specific
`AbstractIntegrationTest` bases configured with
`WebEnvironment.NONE`; changing them globally would start unnecessary web
servers for existing service and persistence tests.

## Decision
- Test each module's controllers through the real embedded server on a random
  port with `@SpringBootTest(webEnvironment = RANDOM_PORT)` and the project's
  real HTTP test client.
- Use the real module application, controller, application services, Flyway
  migrations and PostgreSQL Testcontainers. Assert HTTP outcomes and persisted
  business effects; do not replace components owned by the module under test.
- Controller coverage is integration-only. Do not add controller unit tests,
  `@WebMvcTest`, `MockMvc`, or mocked use cases as substitutes for the
  required HTTP path. Unit tests remain appropriate for domain and application
  logic in their own layers.
- Leave existing `AbstractIntegrationTest` classes at
  `WebEnvironment.NONE`. Add a separate Web integration base per module test
  application and reuse the existing PostgreSQL/Testcontainers support and
  fixture conventions rather than forcing all tests to start a server.
- Stub only another module's public contract when running a module standalone:
  identity receives a deterministic tenant provider; organization substitutes
  `UserDirectory`; team substitutes `UserDirectory` and
  `OrganizationDirectory`. The tested module's own internal path stays real.
- Test `tp-administration` provisioning against the real business modules
  whose Java contracts it orchestrates.
- Use generated OpenAPI clients wrapped by module-local DSLs. Keep generic HTTP
  configuration, diagnostics and assertions in test-scoped
  `tp-test-support`.
- Because the HTTP client and server execute in separate threads and
  transactions, tests must clean or isolate database state explicitly; a
  transaction on the test thread is not assumed to roll back server writes.

## Alternatives considered
- MockMvc or a web slice for controller tests.
- Mocking each controller's application port.
- Changing every existing `AbstractIntegrationTest` to start a web server.
- Starting the entire product application for every module-owned endpoint test.

## Justification
The real server and database expose the observable contract that clients use
and prove the interaction of HTTP mapping, validation, tenant context,
application behavior and persistence. Module-owned test applications keep
ownership explicit, while stubs are restricted to dependencies outside the
module boundary under test.

## Positive consequences
- Controller behavior is proven through an actual HTTP exchange.
- Tenant isolation, serialization, HTTP errors and durable writes are validated
  together.
- Existing fast service/persistence tests retain their non-web environment.
- Test doubles do not conceal defects in the module being exercised.

## Negative consequences / trade-offs
- These tests start servers and containers and are slower than slices or unit
  tests.
- Per-module test applications need deliberate boundary stubs and database
  cleanup.
- Some internal persistence assertions may be needed where the public API
  intentionally has no read operation; these assertions stay test-only.

## Technical impact
- One module-owned real-HTTP test base and DSL per HTTP owner.
- Reuse of PostgreSQL Testcontainers support from `tp-test-support`.
- No production dependency on test support and no controller unit-test layer.

## Validation
- Verify the test context reports a real random-port web environment.
- Verify tests issue HTTP through the generated client/DSL and use PostgreSQL
  Testcontainers with module Flyway migrations.
- Assert success, validation failures, business refusals, error payloads,
  tenant isolation and persisted outcomes.
- Verify only external module contracts are stubbed and no controller test
  substitutes a mocked use case.
- Run the module tests and the full Maven `verify` lifecycle.

## Risks
- Tests that rely on shared database state can become order-dependent; fixtures
  and cleanup must be explicit.
- Full HTTP tests do not replace unit or PostgreSQL adapter tests for the layers
  that own domain and persistence rules.

## Implementation progress

The `W001-T05-http-test-support` branch adds a shared `RestTestClient` base and
a PostgreSQL cleaner that truncates application tables before and after each
HTTP test while preserving Flyway history. Identity, organization and team
each have a separate `RANDOM_PORT` base built on their existing test
application. Identity has a deterministic tenant provider; organization and
team retain only their existing public-contract stubs. The pre-existing
`AbstractIntegrationTest` classes remain `WebEnvironment.NONE`.

The identity vertical adds `UserControllerHttpIntegrationTest` and a module-
local DSL over the generated client. Against the real identity application,
Flyway and PostgreSQL Testcontainers, it verifies create, invite and tenant-
scoped list operations, persisted status, one tenant-provider call per
tenant-scoped request, and that a client-supplied organization reference cannot
select the tenant. It also checks local business-error mapping and shared
request-validation handling. `./mvnw --batch-mode --no-transfer-progress
-pl tp-identity -am verify` passes. This ADR remains `Draft` until the complete
T05 decision is validated.

## References
- [Spring Boot 4.1 testing applications](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html)
