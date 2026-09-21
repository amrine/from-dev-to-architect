# ADR-TECH-001 - Maven and Spring Modulith modular monolith

## Status

Accepted

## Linked ticket

None. This is a reusable technical architecture decision.

## Context

A Java and Spring Boot application may need explicit business boundaries while
remaining a single deployable unit. Package conventions alone do not make these
boundaries visible in the build and do not prevent accidental dependencies
between business capabilities.

This decision applies after a project has chosen a modular monolith. It does not
decide whether a specific product should use a modular monolith, a single
undivided application, or distributed services.

The adopting project must use Maven and Spring Boot and must be able to run its
business modules in the same JVM and deployment unit.

A shared module can reduce genuine duplication, but it can also become a
catch-all dependency that erases ownership. Its admission policy must therefore
be as explicit as the boundaries of the business modules.

## Decision

- Use a Maven multi-module build with one parent project of packaging type
  `pom`.
- Use exactly one executable Spring Boot bootstrap module.
- Package business modules and shared modules as library JARs.
- Make the bootstrap module responsible for assembling the application. A
  business module must never depend on the bootstrap module.
- Use Spring Modulith to model and verify logical application modules.
- Use the `explicitly-annotated` detection strategy so that only packages
  explicitly declared with `@ApplicationModule` become application modules.
- Declare shared modules explicitly from the bootstrap application.
- Admit a production capability to a shared module only when its semantics are
  identical for every consumer, it has no single business-module owner, and it
  does not require access to a business module or the bootstrap module.
- Keep business models, business policies, module-specific error vocabularies,
  persistence entities, transport adapters, and messaging adapters out of
  shared technical modules.
- Do not use a shared production module to host test infrastructure. Reusable
  test tooling belongs in a test-scoped technical module.
- Allow narrowly scoped shared framework configuration only when it expresses
  one uniform technical contract for all consumers. It must be exposed as its
  own capability and must not make unrelated shared packages transitively
  public.
- Expose inter-module capabilities with narrowly scoped `@NamedInterface`
  declarations named after the capability they provide.
- Keep domain, application, and infrastructure implementation packages internal
  unless a deliberate named interface exposes a type.
- Declare permitted logical dependencies with `allowedDependencies`.
- Keep the Maven dependency graph and the Spring Modulith dependency graph
  consistent.
- Use in-process Java contracts for synchronous communication inside the
  deployment unit. Do not introduce HTTP between modules of the same modular
  monolith.
- Expose immutable events representing past facts when asynchronous or decoupled
  notification is required. Events must not expose persistence entities or
  internal implementation types.
- Keep test-support Maven modules outside the Spring Modulith application model
  and outside production dependency scopes.
- Verify the assembled module model during the standard Maven build with
  `ApplicationModules.verify()`.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the executable bootstrap module;
- the business and shared modules;
- any technical modules excluded from the application model;
- the root package and module identifiers;
- the shared modules;
- the admission criteria and concrete capabilities of every shared module;
- the dependencies and framework types allowed in each shared capability;
- the named interfaces and permitted dependency graph;
- the selected Java, Spring Boot, Spring Modulith, and Maven versions;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project. Each project must evaluate its
context and explicitly adopt it through its own ADR.

## Alternatives considered

- Use one Maven module and rely only on package conventions.
- Use multiple Maven modules without a logical module model.
- Use Spring Modulith with its default package detection strategy.
- Use ArchUnit alone to model both internal layers and inter-module boundaries.
- Use Gradle multi-project builds instead of Maven.
- Communicate through HTTP between modules deployed in the same application.
- Put every reusable-looking type in one generic shared module.
- Put reusable test infrastructure in the shared production module.

## Justification

Maven modules provide physical build boundaries while Spring Modulith provides
an executable logical model of the application. Explicit module detection,
named interfaces, and allowed dependencies prevent accidental public surfaces
and make coupling visible during the build.

Keeping synchronous communication in-process preserves the operational
simplicity of a modular monolith. Events remain available when a fact must be
observed without introducing a direct command dependency.

## Positive consequences

- Business boundaries are visible in source code and verified during the build.
- Only one application artifact is deployed and operated.
- Public capabilities are narrower than entire modules or generic API packages.
- The Maven and logical module graphs provide complementary protections.
- Business modules can be tested in isolation.
- Shared technical code has explicit admission criteria and ownership.
- A project can later extract a module without first removing internal HTTP
  calls or leaked persistence types.

## Negative consequences / trade-offs

- The build contains more modules and dependency declarations.
- Every inter-module dependency must be maintained in both Maven and Spring
  Modulith metadata.
- Public named interfaces become compatibility boundaries.
- The admission policy may preserve small local duplication when a concept is
  not genuinely identical across consumers.
- Module tests need explicit substitutes for required capabilities whose
  implementations are intentionally not loaded.
- A modular monolith does not provide runtime or network isolation between its
  modules.
- Extraction into distributed services remains a separate architecture decision
  and is not guaranteed to be simple.

## Technical impact

- Parent Maven project and child module POM files.
- One Spring Boot bootstrap application annotated with `@Modulithic`.
- Root `package-info.java` files annotated with `@ApplicationModule`.
- Public capability packages annotated with `@NamedInterface`.
- Capability-oriented shared packages with explicit dependency policies.
- Explicit `allowedDependencies` for every application module.
- Architecture tests executed from a module that assembles the complete
  application classpath.

## Validation

- The Maven reactor builds every declared module.
- Only the bootstrap module produces an executable Spring Boot artifact.
- `ApplicationModules.verify()` succeeds on the assembled application.
- The detected modules and shared modules match the adopting project's ADR.
- An undeclared inter-module dependency causes the architecture test to fail.
- Business modules do not depend on the bootstrap module.
- Shared production modules do not depend on business or bootstrap modules and
  contain no module-specific business vocabulary.
- Test-support modules are absent from production dependency scopes and from the
  Spring Modulith application model.

## Notes

This ADR defines a reusable implementation pattern for a Maven and Spring
Modulith stack. Business module names, domain ownership, version selection,
database topology, HTTP APIs, and deployment-specific decisions belong to the
adopting project's ADRs.
