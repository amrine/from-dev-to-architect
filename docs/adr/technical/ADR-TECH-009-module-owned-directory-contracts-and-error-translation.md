# ADR-TECH-009 - Module-owned directory contracts and error translation

## Status

Accepted

## Linked ticket

None. This is a reusable technical module-integration decision.

## Context

One module may need a narrow fact owned by another module without depending on
the provider's domain model, repository, persistence entity, or internal
exceptions. Duplicating a lookup port in every consumer fragments the semantics,
while exposing an entire generic API surface grants more coupling than the
consumer needs.

Expected business outcomes and technical failures also need different contracts.
Using exceptions for every unavailable or missing result obscures normal control
flow; leaking persistence exceptions couples consumers to the provider's
infrastructure.

## Decision

- Let the module that owns the information publish one narrow, capability-named
  directory contract.
- Place the directory, its result types, and its public technical exception in
  an autonomous public package or logical named interface.
- Do not expose the provider's domain models, internal error enums, repositories,
  persistence entities, or framework exceptions.
- Make each directory operation a focused lookup or availability check. Do not
  turn a directory into a generic repository or unrestricted listing API.
- Include tenant scope explicitly when the queried concept is tenant-owned.
- Represent expected outcomes with an explicit result type or enum. Distinguish
  states that consumers can act on, such as available, pending, unavailable, or
  not found, instead of collapsing them into a boolean.
- Use a public unchecked directory exception only when the provider cannot
  answer because of a technical failure.
- Validate structural preconditions at the public contract boundary before
  accessing persistence.
- Translate internal business and technical failures before they cross the
  public boundary. Never expose JPA, Spring, driver, or internal module
  exceptions.
- Let each consuming module translate directory results into its own business
  vocabulary and error codes.
- Preserve the public directory exception as the cause when a consumer wraps a
  technical failure.
- Keep expected results, business refusals, and technical failures as separate
  categories.
- Use an in-process Java implementation while modules share one deployment.
  Preserve the contract so a future HTTP or messaging adapter can replace the
  local implementation without changing consuming use cases.
- Prefer capability-specific public surfaces over one generic module-wide API.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- each directory capability and its owning module;
- its public package or logical-interface name;
- method signatures and tenant parameters;
- the complete expected-result semantics;
- the public technical exception;
- provider-side and consumer-side error translations;
- the consumers and allowed logical dependencies;
- the current transport and future extraction boundary;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project.

## Alternatives considered

- Let consumers access the provider's repository or persistence model directly.
- Expose provider domain entities and status enums.
- Declare a duplicate lookup port in every consuming module.
- Put business-specific directories in a common technical module.
- Return a boolean for all outcomes.
- Throw an exception for every expected unavailable or missing result.
- Let persistence and framework exceptions cross module boundaries.
- Expose one generic public interface containing all current and future module
  capabilities.
- Generate an HTTP client while modules still communicate in-process.

## Justification

A provider-owned directory keeps one definition of availability and one public
translation boundary. Explicit result values let consumers make business
decisions without learning the provider's internal lifecycle model.

Separating technical failure from expected outcomes gives consumers actionable
control flow and prevents infrastructure types from becoming part of the module
contract. A transport-independent Java interface also creates a stable seam for
future extraction.

## Positive consequences

- Consumers depend on a narrow stable capability.
- Provider domain and persistence models remain private.
- Expected outcomes are explicit and testable.
- Technical causes do not leak but remain available for diagnostics.
- Consumers retain ownership of their own error vocabulary.
- Future remote adapters can implement the same contract.

## Negative consequences / trade-offs

- Public result enums and exceptions become compatibility contracts.
- Every boundary needs explicit translation code.
- Synchronous directory calls add runtime coupling and possible failure paths.
- Point-in-time availability does not provide continuous distributed
  consistency.
- A future remote implementation must define timeout, retry, and observability
  policies separately.

## Technical impact

- Public directory interfaces, result types, and exceptions.
- Provider implementations and error translation.
- Consumer adapters or services translating results into local errors.
- Logical-module interfaces and permitted dependencies.
- Contract, architecture, and failure-path tests.

## Validation

- Verify that directory packages expose no internal domain or persistence type.
- Verify every expected result and the consumer decision it triggers.
- Verify technical failure translation on both provider and consumer sides.
- Verify that causes are preserved without exposing framework types in public
  signatures.
- Verify tenant isolation for tenant-owned lookup contracts.
- Verify logical-module dependencies target only the named capability.

## Notes

Directory names, result values, logical-interface identifiers, error codes, and
consumer policies belong to the adopting project's ADR.

This ADR owns the directory-specific result and translation boundary. The
broader policy for module-owned business error vocabularies is defined by
[ADR-TECH-012](ADR-TECH-012-module-owned-error-vocabularies-and-boundary-translation.md).
