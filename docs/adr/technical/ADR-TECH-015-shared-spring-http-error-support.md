# ADR-TECH-015 — Shared Spring HTTP error support

## Status
Draft

## Context
HTTP error serialization and framework-level handling are repeated across
business capabilities, but each capability owns the meaning of its business
error codes. Putting framework response types in a neutral shared contract or
centralizing business errors would couple unrelated concerns.

## Decision
- Keep the shared error response data transfer object free of Spring, servlet
  and persistence types.
- Put reusable Spring Web transport advice in a dedicated support component
  that depends on the neutral error contract and not on business modules.
- Limit shared advice to framework and transport failures such as malformed
  input, request validation and unexpected technical failures. It does not
  define business error codes or business translations.
- Keep business-code-to-HTTP-status mapping in the incoming adapter owned by
  the capability that understands the error. That adapter maps to the neutral
  response data; shared support renders it consistently.
- Preserve causes and diagnostics for server-side logging, but never return
  stack traces, internal messages, SQL details or personal data to callers.
- Preserve the boundary rule that each business capability owns its error
  vocabulary and translates known failures (ADR-TECH-012).

## Alternatives considered
- Duplicate a complete Spring exception handler in every business module.
- Put Spring advice, HTTP statuses and framework response types in a neutral
  shared module.
- Centralize business error codes and mappings in shared web support.
- Expose a framework-specific response type as the cross-module error DTO.

## Justification
Spring-specific handling can be reused without coupling neutral response data
to the Web framework. Business codes and status choices remain with the
capability that owns their meaning while callers receive a stable shape.

## Positive consequences
- HTTP error serialization is consistent across owners.
- The neutral DTO does not gain a Web framework dependency.
- Business codes and statuses evolve with their owning capabilities.
- Internal causes and personal data stay out of public responses.

## Negative consequences / trade-offs
- Each owner maintains an explicit mapping from stable business codes to HTTP
  statuses.
- Shared and local handlers need coordinated tests for precedence and
  serialization.
- The shared response shape is a public compatibility contract and must evolve
  deliberately.

## Technical impact
- A neutral error response contract.
- A separate shared Spring Web transport adapter.
- Owner-specific HTTP error mappers and transport tests.
- No business error catalog in shared support.

## Validation
- Verify shared Web support depends on no business module.
- Verify the neutral response DTO imports no Spring, servlet or persistence
  types.
- Exercise common transport failures and every owner's business mapping through
  the test strategy recorded by that project.
- Verify responses contain stable public codes and no internal diagnostics or
  personal data.

## Risks
- Broad shared handlers can capture exceptions before owner-specific mappings;
  precedence must be explicit and tested.
- Changes to the response shape can affect every client and need compatibility
  review.

## Project adoption contract
Each adopting project records its local decision in a project ADR, including:
- the neutral error DTO and the support component that owns shared transport
  behavior;
- each owner's error vocabulary and HTTP mapper;
- exception/advice precedence and the failure categories handled at each
  boundary;
- how technical causes are retained internally and sanitized externally;
- HTTP tests that prove transport mappings and privacy requirements.

The technical ADR remains reusable. Its status does not accept the local error
model for another project.
