# ADR-TECH-003 - PostgreSQL integration tests with Testcontainers

## Status

Accepted

## Linked ticket

None. This is a reusable technical testing decision.

## Context

Persistence behavior depends on the actual database engine. In-memory database
substitutes do not reliably reproduce PostgreSQL data types, constraints,
locking, transaction behavior, or SQL semantics. A developer-managed local
database also makes tests depend on mutable external state, fixed ports, and
manual setup.

This decision applies to applications whose production persistence behavior is
implemented on PostgreSQL and whose integration tests can run Docker-compatible
containers.

## Decision

- Run persistence integration tests against a real PostgreSQL instance provided
  by Testcontainers.
- Use the same PostgreSQL major version as the adopting project's supported
  runtime. Pin the image instead of using a floating tag such as `latest`.
- Let Testcontainers allocate the host port and connection details. Tests must
  not require the project's local Docker Compose environment to be running.
- Start from an ephemeral database whose lifecycle is controlled by the test
  suite or Spring test context.
- Supply the container connection through the framework's supported dynamic
  connection mechanism, such as Spring Boot service connections or dynamic
  properties.
- Keep Testcontainers and reusable test configuration outside production
  dependency scopes.
- A shared test-support module is allowed when several modules need the same
  container setup. It remains technical test tooling and must not become an
  application or business module.
- Load the smallest application context that proves the behavior under test.
  Module-isolated tests should load only the target module and the explicit
  substitutes required by its public dependencies.
- Use successful context startup as the proof for connection establishment,
  migration execution, and ORM schema validation.
- Test application-owned persistence behavior, constraints, transaction
  boundaries, and error translation with focused integration tests.
- Do not assert Flyway's internal history representation, parse startup logs, or
  duplicate checks already performed by Flyway and the ORM.
- Keep interaction-only assertions, retry sequencing, and failure simulation in
  focused unit tests when a real database adds no value.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the pinned PostgreSQL image and its relationship to the runtime version;
- the modules and test suites that use PostgreSQL integration tests;
- the mechanism used to publish the dynamic connection;
- the location and dependency scope of shared test support, if any;
- the test-context isolation strategy;
- the commands and Docker prerequisites used locally and in CI;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project. Each project must evaluate its
context and explicitly adopt it through its own ADR.

## Alternatives considered

- Use an in-memory database such as H2 for persistence integration tests.
- Connect tests to the PostgreSQL instance started by local Docker Compose.
- Connect tests to a long-lived shared PostgreSQL database.
- Mock repositories and skip database integration tests.
- Start PostgreSQL through a custom test script outside the test lifecycle.

## Justification

Testcontainers provides production-relevant PostgreSQL behavior while keeping
the database lifecycle reproducible and isolated from developer data. Dynamic
ports and framework-managed connection details remove the fixed environmental
assumptions of a local or shared database.

Separating reusable test infrastructure from production code prevents
Testcontainers from leaking into the runtime architecture. Context-startup tests
cover framework wiring, migrations, and schema validation; focused persistence
tests then prove only the database contracts owned by the application.

## Positive consequences

- Tests exercise PostgreSQL-specific behavior.
- Every test run can start from a known database state.
- Tests do not depend on local data or a fixed PostgreSQL port.
- Local and CI execution use the same database bootstrap mechanism.
- Shared test support can remove duplicate container wiring across modules.
- Migration and mapping failures are detected during test-context startup.

## Negative consequences / trade-offs

- Docker or another compatible container runtime is required.
- Integration tests are slower and consume more resources than pure unit tests.
- Container startup failures are environmental failures that must be diagnosed
  separately from application behavior.
- Test-context fragmentation can start several PostgreSQL containers during a
  full build.
- Image availability and container-runtime compatibility become build concerns.

## Technical impact

- Test-scoped Testcontainers dependencies.
- A pinned PostgreSQL container image.
- Framework integration for dynamic database connections.
- Optional shared test-support code outside production scopes.
- Context-startup and persistence integration tests.
- CI workers capable of running containers.

## Validation

- Run the persistence integration suite without starting the local Docker
  Compose environment.
- Verify that the test connection uses a dynamically allocated container port.
- Verify that a fresh database accepts all application migrations and ORM
  schema validation.
- Verify representative PostgreSQL constraints and transactional behavior
  through application-owned integration tests.
- Verify that production artifacts and production dependency scopes do not
  contain Testcontainers or shared test-support classes.
- Do not use direct assertions on Flyway history tables as evidence for this
  decision.

## Notes

This ADR defines a reusable PostgreSQL integration-test strategy. Container
class names, test-support module names, credentials, framework annotations, and
test commands belong to the adopting project's ADRs.
