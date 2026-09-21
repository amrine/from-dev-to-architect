# ADR-TECH-012 - Module-owned error vocabularies and boundary translation

## Status

Accepted

## Linked ticket

None. This is a reusable technical error-modeling decision.

## Context

Business failures need stable semantics across domain, application, persistence,
module, and transport boundaries. Centralizing every code in one shared catalog
makes unrelated modules coordinate their evolution, while exposing framework
exceptions or internal messages couples consumers to implementation details.

Using one exception class per individual failure avoids an enum but creates a
large public type hierarchy. Conversely, reducing every outcome to one generic
technical exception prevents callers from distinguishing an expected refusal
from an unavailable dependency or an unknown defect.

## Decision

- Let the module or business capability that owns a failure own its error
  vocabulary.
- Represent stable, actionable failure semantics with an enum or equivalent
  finite code type. Codes describe what the owning capability refused or could
  not complete; they do not encode transport status, persistence technology, or
  implementation class names.
- Use one unchecked business exception for one cohesive error vocabulary unless
  different consumers or compatibility boundaries justify separate exception
  contracts.
- Require every business exception to carry a non-null code, an internal
  diagnostic message, and an optional cause.
- Keep codes and business exceptions independent of dependency-injection,
  persistence, messaging, and transport frameworks.
- Do not put module-specific business codes in a neutral shared module and do
  not impose one global business-exception root on every module.
- Represent expected query or availability outcomes with explicit result values
  when callers are expected to branch on them. Do not use technical exceptions
  as normal control flow.
- Translate known technical failures at the boundary that owns the technology.
  Map them to the owning capability's stable code and preserve the original
  cause.
- Preserve unknown technical failures as technical failures. Do not assign a
  convenient but misleading business code.
- Do not expose a provider's internal business exceptions through a public
  inter-module contract. A public capability may define a minimal unchecked
  technical exception when consumers must distinguish inability to answer from
  expected results.
- Let a consuming module translate public results or public technical failures
  into its own vocabulary. Preserve the public exception as the cause of a
  translated technical failure.
- Let incoming transport adapters map the owning module's codes to protocol
  responses. Do not make domain or application exceptions depend on HTTP,
  serialization, or presentation types.
- Never return internal diagnostic messages directly to an external caller.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- each error vocabulary and its owning module or business capability;
- the code and exception types and their package locations;
- the chosen granularity when one module owns several business capabilities;
- the complete stable code catalogs;
- the expected-result types that are deliberately not exceptions;
- the known technical-to-business translations at persistence, module, and
  transport boundaries;
- the public technical exceptions exposed by inter-module capabilities;
- the policy for diagnostic messages, causes, and external responses;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project.

## Alternatives considered

- Centralize all business error codes in a shared technical module.
- Require every module exception to inherit from one shared business base class.
- Create one exception class for every individual error code.
- Expose internal business exceptions through public module contracts.
- Let JPA, ORM, driver, or transport exceptions cross ownership boundaries.
- Return a boolean for every expected outcome.
- Convert every technical failure into a generic business error.
- Expose internal exception messages directly through transport responses.

## Justification

Module-owned codes preserve business ownership and allow unrelated capabilities
to evolve independently. One exception per cohesive vocabulary keeps failure
handling explicit without creating a type for every refusal.

Boundary translation prevents persistence and transport technologies from
becoming part of business contracts. Preserving causes retains diagnostic value,
while keeping unknown failures technical avoids presenting defects or outages as
legitimate business decisions.

## Positive consequences

- Error semantics evolve with the business capability that owns them.
- Consumers can branch on stable codes rather than diagnostic messages.
- Shared technical modules remain free of business catalogs.
- Framework and persistence exceptions do not leak across module boundaries.
- Expected outcomes, business refusals, and technical failures remain distinct.
- Transport contracts can evolve without changing domain exceptions.

## Negative consequences / trade-offs

- Every boundary requires explicit translation code.
- Stable codes become compatibility contracts that must be evolved deliberately.
- Causes can form deep chains across several module boundaries.
- One exception per vocabulary requires discipline to prevent codes from becoming
  an unrelated catch-all catalog.
- Transport adapters must maintain an explicit mapping from codes to protocol
  responses.

## Technical impact

- Module-owned code enums or equivalent finite types.
- Unchecked business exceptions carrying code, message, and optional cause.
- Capability-specific public result types and technical exceptions.
- Persistence, inter-module, and transport translation code.
- Unit, contract, and integration tests for known and unknown failures.

## Validation

- Verify that every business code belongs to one owning capability.
- Verify that shared technical modules contain no module-specific error catalog.
- Verify that business exceptions reject a null code and preserve their cause.
- Verify expected results without using exceptions as normal control flow.
- Verify known technical failures map to the intended owning-module code.
- Verify unknown failures are not mislabeled as business refusals.
- Verify public module contracts expose no internal exception or framework type.
- Verify transport responses do not expose internal diagnostic messages.

## Notes

Concrete module names, package names, code values, exception names, public
capabilities, constraint mappings, and transport statuses belong to the adopting
project's ADR.
