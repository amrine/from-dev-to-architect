# ADR-TECH-013 — OpenAPI contracts and generated test clients

## Status
Draft

## Linked ticket
Initial adoption: W001-T05. Reusable technical decision.

## Context
HTTP contracts are easy to duplicate accidentally between server DTOs, test
clients and documentation. Handwritten test clients also drift as operations
change. A generated client alone, however, does not express readable business
scenarios, and generated server code can impose framework structure on a
hexagonal application.

## Decision
- Maintain one OpenAPI 3 contract for each HTTP owner. The initial owners are
  identity, organization, team and platform administration.
- Keep each contract with its owning module and treat it as the public
  transport contract for paths, parameters, request/response schemas and
  documented status/error responses.
- Implement server adapters explicitly against the owner's application ports;
  do not generate controllers or domain/application code from the specification.
- Use OpenAPI Generator's Maven plugin to generate Java HTTP clients and
  transport models for tests. Generated sources live under Maven's
  `target/generated-test-sources` and are not committed.
- Keep readable business DSLs handwritten and module-owned under each module's
  `src/test`. They wrap generated clients; they do not duplicate transport
  serialization. Generic HTTP setup and assertions belong in test-scoped
  `tp-test-support`. Do not create a separate `tp-test-clients` module.
- Pin the generator/plugin version centrally when implementing, after checking
  compatibility with the repository's Java 25 and Spring Boot 4.1 baseline.
  Generated clients are test tools and do not create a new production runtime
  dependency.
- Use the shared Maven product version for contract metadata and generated
  artifact metadata. Do not independently version each module or introduce a
  URL version segment without an explicit compatibility requirement.

## Alternatives considered
- Handwrite a separate client and documentation for every endpoint.
- Generate both server controllers and test clients from OpenAPI.
- Put generated clients and business DSLs in a shared Maven module.
- Use JGiven as the primary contract and client-generation mechanism.
- Assign independent SemVer versions to the four module APIs.

## Justification
The OpenAPI contract gives every owner one reviewable HTTP boundary and can
drive deterministic client generation. Keeping server adapters handwritten
preserves the existing ports-and-adapters design. Generated transport code
removes boilerplate, while module-local DSLs retain business vocabulary and
avoid cross-module test dependencies or a Maven cycle.

## Positive consequences
- API documentation, generated test clients and server behavior can be checked
  against the same schemas.
- Module ownership remains visible in both production code and tests.
- Generated sources are reproducible build artifacts rather than reviewed
  source-of-truth files.
- Test scenarios remain readable without adding a narrative-testing library.

## Negative consequences / trade-offs
- OpenAPI contracts and generated clients add build configuration and generator
  maintenance.
- A specification can still be inaccurate unless contract validation and
  black-box HTTP tests run in CI.
- Generated clients may be verbose; the DSL must add business value rather than
  become a second generated API layer.
- API compatibility policy remains tied to the shared product version until a
  genuine independent compatibility need appears.

## Technical impact
- One OpenAPI 3 document and generated test client per HTTP owner.
- Maven generation and validation executions, centrally versioned.
- Module-local test DSLs, with only generic HTTP facilities in
  `tp-test-support`.
- No generated production controller or server-interface requirement.

## Validation
- Fail the Maven build on an invalid OpenAPI document or failed client
  generation.
- Compile generated clients and module DSLs on the supported Java version.
- Exercise every documented request/response shape through real HTTP tests.
- Check that generated files are confined to `target` and that no production
  source imports a test client.
- Verify all contracts use the common product version and do not introduce
  independent module versions.

## Risks
- Generator upgrades can change generated source and serialization behavior;
  upgrades must be pinned and reviewed.
- A contract and generated client may agree with each other while both disagree
  with the server; only the real HTTP tests catch that divergence.

## References
- [OpenAPI Generator Maven plugin](https://openapi-generator.tech/docs/plugins/)
- [OpenAPI Generator Java client options](https://openapi-generator.tech/docs/generators/java/)
