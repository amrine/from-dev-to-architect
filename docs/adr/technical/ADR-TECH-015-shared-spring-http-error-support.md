# ADR-TECH-015 — Shared Spring HTTP error support

## Status
Draft

## Linked ticket
Initial adoption: W001-T05. Reusable technical decision.

## Context
HTTP error serialization and framework-level error handling are repeated across
business modules, but each module owns its own business error codes and their
meaning. Putting Spring MVC types or one global business-error catalog in
`tp-common` would couple the shared Java contract to a framework or make
unrelated modules coordinate their error evolution.

## Decision
- Add a dedicated `tp-web-support` module for shared Spring Web error-adapter
  behavior. It depends on the neutral response contract but not on business
  modules.
- Keep a small Java-only `ApiError` response DTO in `tp-common`. Its stable
  response fields are `code` and `message`; it does not use Spring's
  `ResponseEntity`, `ProblemDetail`, servlet types or annotations.
- Use shared advice for transport/framework failures such as malformed input,
  Jakarta validation failures and unexpected technical failures. Do not place
  module-specific error codes, exception classes or business translations in
  that shared module.
- Keep each module's business-code-to-HTTP-status mapping in its own incoming
  Web adapter. The module maps its vocabulary to `ApiError` and the shared
  support renders the HTTP response consistently.
- Preserve causes and diagnostic context in server-side logs, but never return
  stack traces, internal exception messages, SQL details or personal data to
  callers.
- Preserve the existing technical decision that each business capability owns
  its error vocabulary and translates known failures at its boundary
  (ADR-TECH-012).

## Alternatives considered
- Duplicate a complete Spring exception handler in every business module.
- Put `@RestControllerAdvice`, HTTP statuses and Spring response types in
  `tp-common`.
- Centralize every module's error code and mapping in `tp-web-support`.
- Expose Spring `ProblemDetail` directly as the cross-module response DTO.

## Justification
The Spring-specific mechanism can be reused without leaking web framework
types into neutral response data. Module ownership remains explicit, so error
codes evolve with the capability that defines them while clients receive one
stable shape.

## Positive consequences
- Consistent HTTP error serialization across modules.
- No Spring MVC dependency is introduced by the `ApiError` type itself.
- Business codes and statuses remain owned by the module that understands them.
- Internal causes and PII are not exposed in public responses.

## Negative consequences / trade-offs
- Every module must maintain an explicit mapping from its stable codes to
  transport statuses.
- Shared advice and local business mappings need coordinated tests to ensure
  that precedence and serialization remain predictable.
- The response DTO becomes a public compatibility contract and must evolve
  deliberately.

## Technical impact
- New `tp-web-support` Maven module with Spring Web dependencies.
- Java-only `ApiError` DTO in `tp-common`.
- Module-owned Web error mappers and contract tests.
- No module-specific error code catalog in either shared module.

## Validation
- Verify `tp-web-support` depends on no business module.
- Verify `ApiError` imports no Spring, servlet or persistence types.
- Test common request-validation and technical-error responses through HTTP.
- Test every module's business-code mapping through its real HTTP integration
  tests.
- Verify responses contain stable public codes and no internal diagnostics or
  PII.

## Risks
- Overly broad shared handlers can capture exceptions before module-owned
  mappings; exception ownership and handler precedence must be tested.
- A response-contract change can affect every API client and therefore needs
  compatibility review.

## Implementation progress

The `W001-T05-shared-http-errors` branch adds the Java-only `ApiError` record
with `code` and `message`, plus `tp-web-support` with Spring Boot
auto-configuration for the shared advice. The advice returns stable generic
codes and safe messages for malformed requests, validation failures and
framework errors; unexpected failures are logged server-side and return a
generic 500 response. The shared advice has lowest precedence so module-owned
business mappings can take precedence.

`./mvnw --batch-mode --no-transfer-progress -pl tp-common,tp-web-support -am verify`
passes. The identity vertical adds a higher-precedence local advice for
`UserException` and verifies over real HTTP that duplicate email maps to its
module-owned `409 USER_EMAIL_ALREADY_USED` response while Jakarta request
validation maps to the shared sanitized `400 VALIDATION_FAILED` response. The
responses contain no submitted email or internal exception details, and
`./mvnw --batch-mode --no-transfer-progress -pl tp-identity -am verify` passes.
This ADR remains `Draft` until the complete T05 decision is validated.

The organization vertical adds a higher-precedence local mapper for
`OrganizationException`. It maps unavailable responsible users and deferred
organization transitions to module-owned `409` responses, missing users to
`404`, invalid domain values to `422`, and a failed `UserDirectory` check to a
sanitized `503`. Real HTTP tests verify the deferred assignment code and safe
message, the local timezone validation response, and the shared
`400 VALIDATION_FAILED` response. The organization `verify` command passes.
This ADR remains `Draft` until the complete T05 decision is validated.
