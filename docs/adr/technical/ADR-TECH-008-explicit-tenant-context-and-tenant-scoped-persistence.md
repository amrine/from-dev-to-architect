# ADR-TECH-008 - Explicit tenant context and tenant-scoped persistence

## Status

Accepted

## Linked ticket

None. This is a reusable technical multi-tenancy decision.

## Context

Tenant isolation is easy to weaken when the current tenant is held in ambient
mutable state, accepted from untrusted input, or omitted from repository
methods. Passing only a raw string also hides that the value defines the entire
execution scope of a use case.

Authentication and tenant scoping are related but distinct. A project may need
an explicit tenant boundary before it has a final identity provider, actor
model, token format, or permission system.

## Decision

- Represent the execution scope with an immutable Java-pure
  `TenantContext(tenantReference)` value.
- Use the immutable functional reference of the tenant-root aggregate as
  `tenantReference`. In generic notation:

  ```text
  TenantContext.tenantReference = <tenant-root>.reference
  ```

- Keep the context independent of the concrete tenant-root domain type.
- Resolve the current context through a narrow `TenantContextProvider` contract
  at an incoming application boundary.
- Do not let incoming requests freely supply or override the tenant reference in
  a body, path, query parameter, or caller-controlled header.
- Pass `TenantContext` explicitly to every tenant-scoped incoming use case.
- Keep platform or bootstrap use cases explicitly non-tenant-scoped when they
  create or administer tenant roots.
- Do not introduce an empty platform context merely to make all signatures
  uniform.
- Keep tenant identity distinct from actor identity. Add an actor or security
  context only when it carries authenticated information and permissions.
- Extract `tenantReference` once in the application service and pass it to every
  tenant-scoped lookup, existence check, and listing operation.
- Require tenant-scoped application repositories to include the tenant
  reference in read methods. Do not expose global `findByReference` or `findAll`
  operations through tenant-scoped application ports.
- Before writing an existing aggregate, verify that its tenant reference matches
  the current context.
- Let a write port receive only the aggregate when that aggregate already owns
  the tenant reference. Do not pass a redundant tenant argument that can
  contradict the aggregate.
- Keep unrestricted framework-repository methods confined to infrastructure and
  controlled test fixtures.
- Add a non-null tenant-reference column to every tenant-owned table.
- Start tenant-filtered indexes with the tenant reference when it matches the
  query access pattern.
- Include the tenant reference in unique and relational constraints when doing
  so prevents cross-tenant collisions or associations.
- Do not create foreign keys to another module's private persistence identifier
  to enforce cross-module tenant coherence. Use public application contracts.
- Treat database row-level security as a separate defense-in-depth decision.
  This ADR establishes explicit application and relational boundaries without
  requiring RLS.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the tenant-root aggregate and its immutable functional reference;
- the concrete tenant-context and provider types;
- the shared capability or package exposing those contracts;
- the tenant-scoped and non-tenant-scoped use cases;
- the incoming-boundary resolution strategy for every environment;
- the repository signatures and tenant-owned tables;
- the tenant-aware indexes, unique constraints, and composite relationships;
- the actor/security boundary and deferred authentication work;
- whether database RLS is enabled;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project.

## Alternatives considered

- Pass a raw tenant-reference string to every use case.
- Store the tenant in a thread-local or other ambient context.
- Accept the tenant reference directly from caller-controlled request data.
- Add actor identity to the tenant context.
- Create an empty platform context for non-tenant operations.
- Expose global repository methods and rely on callers to filter correctly.
- Pass both the aggregate and a redundant tenant argument to every write port.
- Infer the tenant only through joins.
- Rely exclusively on PostgreSQL row-level security.

## Justification

An explicit context makes tenant scope visible in use-case contracts and tests.
Using the tenant root's functional reference avoids coupling consumers to its
database identifier while preserving one stable isolation key across modules.

Tenant-qualified repositories and relational constraints provide complementary
defenses. Application services preserve module ownership and business checks;
the database rejects locally expressible cross-tenant associations and
uniqueness violations.

Separating tenant and actor identity allows bootstrap, system, and platform
operations to exist without fabricating an authenticated user.

## Positive consequences

- Missing tenant scope is visible in incoming-port signatures.
- Repository reads cannot omit tenant scope silently.
- Aggregate writes verify context coherence before persistence.
- Database constraints can reject cross-tenant associations.
- Tenant-root persistence identifiers remain private.
- Authentication and actor modeling can evolve without changing use-case tenant
  contracts.

## Negative consequences / trade-offs

- Tenant context is propagated through many signatures.
- Every tenant-owned query and index must be reviewed for the tenant key.
- Shared database roles still permit technical cross-schema access unless
  further restricted.
- Explicit context does not authenticate a caller or authorize an action.
- A temporary local provider is not a production security mechanism.
- RLS, if needed, requires an additional decision and operational setup.

## Technical impact

- Tenant-context and provider contracts.
- Incoming use-case signatures.
- Application-service orchestration and aggregate-coherence checks.
- Tenant-scoped repository ports and adapters.
- Tenant columns, indexes, unique constraints, and composite keys.
- Incoming adapters that resolve rather than accept the tenant.

## Validation

- Verify that every tenant-scoped incoming port receives `TenantContext`.
- Verify that platform operations are deliberately non-tenant-scoped.
- Verify that tenant repositories expose no global read operations.
- Verify isolation with at least two tenants holding overlapping business data.
- Verify that cross-tenant updates and relationships are rejected.
- Verify that incoming adapters resolve the tenant exactly once and pass the
  resulting context unchanged.
- Verify that actor identity is not fabricated inside `TenantContext`.

## Notes

The concrete shared package, logical-module interface name, tenant-root type,
local provider, security provider, SQL column name, and database policy belong
to the adopting project's ADR.
