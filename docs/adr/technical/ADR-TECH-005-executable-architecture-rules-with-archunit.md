# ADR-TECH-005 - Executable architecture rules with ArchUnit

## Status

Accepted

## Linked ticket

None. This is a reusable technical testing decision.

## Context

Documented architecture conventions tend to drift as a codebase grows. Many
forbidden dependencies still compile, and code review alone does not guarantee
that every module applies the same internal rules.

Build modules and logical-module tools can verify physical and inter-module
boundaries, but they do not necessarily verify package placement, dependency
direction, framework isolation, or cycles inside each business module.

This decision applies to Java applications whose important structural rules can
be expressed from compiled classes and package ownership.

## Decision

- Use ArchUnit to make internal architecture constraints executable.
- Keep the responsibilities of the available controls distinct:
  - the build tool verifies physical module dependencies;
  - the logical-module model verifies module boundaries and permitted
    inter-module dependencies;
  - ArchUnit verifies package placement and dependency direction inside modules.
- Declare ArchUnit explicitly in test scope. Do not rely on a transitive
  dependency and do not expose it on production classpaths.
- Execute cross-module architecture tests from a test location whose classpath
  assembles all production classes under analysis.
- Import production classes only. Architecture tests must not accidentally prove
  rules against test fixtures or generated substitutes.
- Derive the analyzed root package from the application bootstrap or equivalent
  authoritative type instead of duplicating a project package literal.
- Derive the target module catalog from the existing logical architecture model
  when one exists. Do not maintain a second silent list of business modules.
- Exclude shared or technical modules according to explicit project policy.
- Centralize common rules in a parameterized rule factory based on a module's
  identifier and base package.
- Apply the same catalog to equivalent business modules. A module-specific
  exception must be explicit, justified, and associated with a removal condition
  when temporary.
- Give every rule a stable identifier, a precise description, and a reason that
  makes failures actionable.
- Allow a rule to have no target temporarily when the corresponding production
  capability does not exist yet. An empty target is not evidence that the
  boundary has been exercised.
- Never add fake production classes or empty abstractions only to activate an
  architecture rule.
- Run architecture tests without starting the application context, database,
  message broker, or containers.
- Validate critical rules with a controlled temporary violation before accepting
  the rule set, then remove the violation and verify the build returns to green.
- Keep behavioral, framework-integration, and performance assertions in their
  respective tests. ArchUnit proves structure, not runtime behavior.

### Generic rule catalog

The adopting project binds these rules to its concrete packages, frameworks,
and architecture-test implementation.

| Rule | Generic contract |
| --- | --- |
| R01 | Domain code is independent of configured frameworks and outward architectural zones. |
| R02 | Dependencies between application and domain point toward the domain; application code does not depend on infrastructure or technical configuration. |
| R03 | Incoming and outgoing ports belong to the application; incoming adapters use incoming ports and outgoing adapters implement outgoing ports. |
| R04 | Persistence frameworks, persistence models, and database repositories remain confined to the persistence adapter and do not cross into the core or public surface. |
| R05 | Transport-specific components remain confined to the transport adapter and do not access persistence adapters directly. |
| R06 | Transport, persistence, and messaging adapters do not depend directly on one another. |
| R07 | Public contracts and public events remain independent of internal domain, application, infrastructure, and configuration types. |
| R08 | Technical components reside in their designated zones and the module root contains only its module declaration. |
| R09 | Internal architectural zones and adapter categories are free of dependency cycles. |
| R10 | Every equivalent business module receives exactly the same applicable rule catalog. |

Additional rules may extend this catalog when a project introduces a deliberate
framework policy or technical module. Their identifiers, ownership, and scope
must be recorded by the adopting project.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the ArchUnit version and dependency scope;
- the test module or source set that assembles the analyzed production classes;
- the authoritative module catalog and root package source;
- the modules included and excluded from common rules;
- the rule identifiers and their concrete architectural meaning;
- the common-rule factory and architecture-test entry points;
- the policy for empty targets and exceptions;
- the focused and full-build validation commands;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project. Each project must evaluate its
context and explicitly adopt it through its own ADR.

## Alternatives considered

- Keep architecture conventions only in documentation and code review.
- Duplicate architecture tests in every business module.
- Maintain a manual module list separate from the logical architecture model.
- Use a logical-module tool for all internal layering rules.
- Depend transitively on ArchUnit through another test library.
- Populate empty packages with fake production classes.
- Start the application context to execute structural tests.

## Justification

ArchUnit turns high-value structural constraints into deterministic build
feedback. Hosting the tests on an assembled production classpath makes one rule
catalog cover every relevant module without adding architecture-test dependencies
to the modules themselves.

Deriving modules from the existing architecture model preserves one source of
truth. A parameterized rule factory prevents copy-and-paste divergence, while
stable identifiers and explicit reasons make failures understandable and allow
the catalog itself to be checked.

Allowing empty targets supports incremental architecture without forcing fake
code. The limitation remains visible: a rule becomes meaningful evidence only
when real production classes exercise its predicate.

## Positive consequences

- Forbidden dependencies fail during the standard build.
- Equivalent modules receive the same internal architecture contract.
- New business modules can be covered automatically when added to the logical
  model.
- Architecture tests remain fast and independent of runtime infrastructure.
- Rule failures identify both the violated constraint and its rationale.
- Production code is not polluted with test-only architecture scaffolding.

## Negative consequences / trade-offs

- Custom predicates and conditions require maintenance as the architecture
  evolves.
- Empty-target rules can create a false sense of coverage if not reported
  honestly.
- Reflection and indirect framework behavior may not be visible as ordinary
  bytecode dependencies.
- Broad package predicates can become brittle during legitimate package
  refactoring.
- Structural tests cannot prove runtime wiring, business behavior, database
  semantics, or performance.

## Technical impact

- Test-scoped ArchUnit dependency.
- One assembled architecture-test location.
- Production-class importer and authoritative module catalog.
- Parameterized rule factory and common rule identifiers.
- Standard-build execution of architecture tests.
- Documented exception and empty-target policies.

## Validation

- Execute the architecture suite without application or infrastructure startup.
- Verify that test classes are excluded from the imported class set.
- Verify that every governed business module receives the expected rule catalog.
- Introduce a controlled forbidden dependency and confirm that the intended rule
  fails with an actionable message.
- Remove the violation and confirm that the architecture suite returns to green.
- Verify that ArchUnit remains absent from production dependency scopes.
- Execute the normal project build and confirm that architecture tests are
  discovered automatically.

## Notes

This ADR defines a reusable enforcement approach. Package names, application
bootstrap classes, logical-module tools, exact rule catalogs, module exclusions,
and project-specific exceptions belong to the adopting project's ADRs.
