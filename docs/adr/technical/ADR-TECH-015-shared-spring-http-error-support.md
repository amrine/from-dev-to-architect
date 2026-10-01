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
- Keep a small Java-only `ApiError` response DTO in `tp-common`. It contains
  stable response data only; it does not use Spring's `ResponseEntity`,
  `ProblemDetail`, servlet types or annotations.
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
