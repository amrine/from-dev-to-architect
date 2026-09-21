# ADR-TECH-010 - JPA-managed technical state and optimistic locking

## Status

Accepted

## Linked ticket

None. This is a reusable technical persistence decision.

## Context

Persistence entities require technical identity, optimistic-lock versioning, and
audit metadata. Putting those fields in every domain model couples business
logic to JPA lifecycle and audit policy. Moving them into a shared JPA base class
reduces repetition but makes an otherwise neutral shared module depend on the
persistence framework.

Updates also need to preserve JPA-managed state. Replacing a managed entity from
a domain object can reset identity, version, or audit values and can bypass the
intended optimistic-lock behavior.

## Decision

- Keep JPA entities separate from domain models.
- Let the persistence entity own technical identity, optimistic-lock version,
  and current-state audit metadata unless one of those values has demonstrated
  business meaning in the domain.
- Use `@Version` locally on each persisted entity to detect concurrent updates.
- Prefer optimistic locking by default. Introduce pessimistic locking only for
  a use case whose contention and consistency requirements justify its database
  cost.
- Store `createdAt`, `createdBy`, `modifiedAt`, and `modifiedBy` on persisted
  entities when current-state audit is required.
- Use `Instant` for audit timestamps and a timezone-aware database type.
- Do not place a JPA `@MappedSuperclass` or persistence base entity in a neutral
  shared module merely to remove repeated fields.
- Duplicate the small technical mapping in each persistence adapter so module
  ownership and framework isolation remain explicit.
- Map only business state between the domain model and the persistence entity.
- On update, apply business changes to the already managed entity and preserve
  technical identity, version, and audit state.
- Keep the mapping mechanism replaceable. Manual mapping, generated mapping, or
  another mapper is acceptable when it preserves the same ownership rules.
- Give every database constraint whose violation maps to a stable application
  error an explicit and stable name.
- Trigger a pending database write before leaving the persistence-adapter
  boundary when deferred ORM execution would otherwise move a known failure
  outside that adapter's translation code. Use an explicit flush only where
  this immediate observation is required.
- Identify known integrity failures from structured exception data such as the
  violated constraint name. Do not parse localized or provider-generated error
  messages.
- Inspect wrapped causes inside the persistence adapter. A cause-chain helper
  may be shared only when the caller supplies the framework exception type and
  extraction function, keeping the helper independent of JPA, ORM, and driver
  APIs.
- Ensure known uniqueness, integrity, and optimistic-lock failures occur inside
  an adapter boundary that can translate them into the owning module's stable
  error vocabulary.
- Preserve unknown persistence failures as technical failures rather than
  assigning an arbitrary business code.
- Treat current-state audit fields as metadata, not as a complete historical
  audit log. Full history or event persistence requires a separate decision.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the persisted entities and their technical fields;
- any domain model that deliberately retains an internal technical identifier
  because it has meaning inside the owning module;
- the optimistic-lock strategy and translated concurrency error;
- the audit field names, actor source, and time source;
- the mapper technology and update-preservation policy;
- the location of shared mapper configuration, if any;
- the stable database-constraint names mapped to application errors;
- the operations that require an explicit flush before leaving the adapter;
- the framework-specific exception extractor and any neutral shared helper;
- the known database violations translated by each adapter;
- the boundary between current-state audit and historical audit;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project.

## Alternatives considered

- Put persistence identity, version, and audit fields in every domain model.
- Reuse the persistence entity as the domain model.
- Share a JPA base entity through a neutral common module.
- Recreate a new entity for every update.
- Use pessimistic locking for every write.
- Ignore concurrent updates and rely on last-write-wins behavior.
- Treat current audit columns as a complete change history.
- Let ORM flushing occur only at transaction commit and translate failures
  outside the persistence adapter.
- Parse database-vendor error messages to identify violated constraints.
- Translate every integrity failure to one generic business error.

## Justification

Separating technical state keeps the domain independent of JPA while retaining
the concurrency and observability mechanisms required by persistence. Local
duplication is cheaper than introducing a shared framework-bound inheritance
model that every module must adopt.

Updating the managed entity lets JPA retain identity and version state and makes
optimistic-lock failures observable at the persistence boundary. An explicit
mapper contract protects those fields regardless of the chosen mapping library.

## Positive consequences

- Domain models remain independent of JPA lifecycle.
- Concurrent updates are detected explicitly.
- Module persistence mappings remain independently evolvable.
- Updates preserve technical state and audit metadata.
- Known constraint violations are translated inside the adapter that owns the
  persistence technology.
- Stable constraint names provide deterministic translation without parsing
  provider messages.
- A neutral shared module stays free of JPA.
- Mapper technology can change without changing the domain contract.

## Negative consequences / trade-offs

- Technical fields are repeated across persistence entities.
- Domain-to-entity mapping requires explicit maintenance.
- Optimistic locking introduces a failure path that use cases must handle.
- Audit actor resolution requires environment-specific wiring.
- Current-state audit does not answer who changed every historical value.
- Explicit flushing can reduce write batching and must remain limited to
  boundaries that require immediate failure translation.
- Renaming a translated database constraint requires a coordinated adapter
  change.

## Technical impact

- JPA entity identity, version, and audit mappings.
- Domain-to-persistence mappers.
- Managed-entity update methods.
- Auditing configuration and deterministic time providers for tests.
- Persistence-exception translation.
- Stable constraint names, targeted flush policy, and cause-chain inspection.
- PostgreSQL constraints and integration tests.

## Validation

- Verify that domain models have no JPA annotations or audit-framework types.
- Verify creation and update mappings preserve the expected business state.
- Verify updates retain technical identity and increment the optimistic version.
- Verify audit creation fields remain stable and modification fields change.
- Verify a real concurrent update produces the module's concurrency error.
- Verify known constraint violations are translated and unknown failures remain
  technical.
- Verify translated constraint failures occur before the adapter returns and
  perform no automatic retry unless a separate decision requires it.
- Verify translation uses structured constraint identity rather than database
  error-message text.
- Verify the shared neutral module has no JPA dependency.

## Notes

Mapper libraries are implementation choices, not part of this reusable
decision. Concrete mapper types, shared mapper configuration, audit actors,
error codes, constraint names, flush methods, and domain exceptions belong to
the adopting project's ADR.

This ADR owns persistence-state and persistence-failure mechanics. The broader
policy for module-owned business error vocabularies is defined by
[ADR-TECH-012](ADR-TECH-012-module-owned-error-vocabularies-and-boundary-translation.md).
