# ADR-TECH-013 — OpenAPI contracts and generated test clients

## Status
Draft

## Context
HTTP contracts can drift between server adapters, client code and published
documentation. Handwritten clients repeat transport details, while generated
server code can impose a framework structure on an otherwise ports-and-adapters
application. Generated clients also need a project-owned layer for readable
business scenarios.

## Decision
- Give each HTTP owner one OpenAPI contract for paths, parameters, request and
  response schemas, and documented status/error responses.
- Keep the contract with its owner and implement server adapters explicitly
  against that owner's application ports. Do not generate domain, application
  or server-controller code from the specification.
- Generate clients and transport models for tests only. Keep generated sources
  in the build output and out of version control; do not add a production
  dependency solely for test clients.
- Keep business scenario DSLs handwritten and local to the owning module's
  test sources. Shared test support may provide generic transport setup, but
  should not become a second business client layer.
- Pin generator and plugin versions at the build level after checking
  compatibility with the project's supported runtime and serialization stack.
- Use the project's shared product version for contract metadata unless a
  demonstrated compatibility requirement justifies independent API versions.
  Do not add a URL version segment without that requirement.

## Alternatives considered
- Handwrite a separate client and documentation for every endpoint.
- Generate server controllers and test clients from OpenAPI.
- Put generated clients and business DSLs in a shared test module.
- Use a narrative-testing library as the contract and client-generation
  mechanism.
- Assign independent semantic versions to every module API.

## Justification
OpenAPI provides a reviewable transport contract and deterministic client
generation. Handwritten server adapters preserve the application's chosen
architecture, while owner-local DSLs retain business vocabulary without
duplicating serialization or creating cross-module test dependencies.

## Positive consequences
- Contract documentation, generated clients and server behavior can be checked
  against the same schemas.
- Ownership stays visible in production and test code.
- Generated sources are reproducible build artifacts rather than a second
  source of truth.
- Business scenarios remain readable without requiring generated client types
  throughout the tests.

## Negative consequences / trade-offs
- Contracts, generated clients and build configuration add maintenance work.
- A specification and generated client can agree while both disagree with the
  server; runtime contract tests are still required.
- Generated clients can be verbose, so local DSLs should add business meaning
  rather than wrap every generated method mechanically.
- Compatibility policy remains tied to the shared product version until an
  independent release need is demonstrated.

## Technical impact
- One OpenAPI document per HTTP owner.
- Centrally managed client generation in the build's test-source lifecycle.
- Owner-local business DSLs and project-defined generic HTTP test support.
- No generated production controller or server-interface requirement.

## Validation
- Fail the build on an invalid contract or failed client generation.
- Compile generated clients and local DSLs on the supported Java/runtime
  configuration.
- Exercise documented request and response shapes against the running server.
- Verify generated files remain in build output and production code does not
  depend on test clients.
- Verify version metadata follows the project's documented compatibility
  policy.

## Risks
- Generator upgrades can change source and serialization behavior; pin and
  review upgrades.
- Contract and generated-client agreement alone does not prove server behavior.

## Project adoption contract
Each adopting project records its local decision in a project ADR, including:
- HTTP owners and the source location of each contract;
- OpenAPI, generator and plugin versions, runtime compatibility, generation
  options, output path and test-only dependency scope;
- ownership and location of module-local business DSLs and generic test
  support;
- product/API version and server-base-URL policies;
- build/CI validation commands and the evidence currently available.

The technical ADR remains reusable. Its status does not approve a project's
local adoption or replace that project's own architectural decision.

## Implementation progress

The `W001-T05-openapi-test-clients` branch pins OpenAPI Generator Maven plugin
`7.25.0` in the parent and manages a test-only Java `restclient` configuration.
An owner activates the inherited `generate-test-sources` execution by declaring
the plugin in its POM and stores its contract at
`src/main/openapi/openapi.yaml`. Generated code is confined to
`target/generated-test-sources/openapi`, attached only to test compilation, and
uses the shared Maven product version. The generator targets Spring Boot 4 and
Jackson 3 to match the checked-in baseline.

No owner contracts exist yet on this branch, so generation and client
compilation are validated incrementally as the owning API verticals add their
specifications. The ADR remains `Draft` until those real contracts compile and
are exercised through HTTP.

## References
- [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [OpenAPI Generator Maven plugin](https://openapi-generator.tech/docs/plugins/)
- [OpenAPI Generator Java client options](https://openapi-generator.tech/docs/generators/java/)
- [OpenAPI Generator 7.25.0 release](https://github.com/OpenAPITools/openapi-generator/releases/tag/v7.25.0)
