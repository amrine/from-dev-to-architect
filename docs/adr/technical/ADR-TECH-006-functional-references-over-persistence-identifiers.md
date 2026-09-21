# ADR-TECH-006 - Functional references over persistence identifiers

## Status

Accepted

## Linked ticket

None. This is a reusable technical architecture decision.

## Context

Database-generated identifiers are efficient persistence keys, but exposing them
through module, transport, or integration contracts couples consumers to one
storage model. A future database migration or service extraction then turns an
internal implementation detail into a compatibility constraint.

Adding a functional reference to every object is not a better default. An entity
that never crosses its owning boundary may already have a sufficient internal
identity, and speculative references create formats and indexes that must be
maintained without a consumer need.

## Decision

- Keep persistence identifiers private to the persistence boundary that owns
  them.
- Address an aggregate or entity with an immutable functional reference when it
  must be identified durably outside that persistence boundary, including in a
  public contract, another module, a URL, an event, or an external integration.
- Do not give every aggregate or entity a functional reference mechanically.
  Internal entities may keep only an internal identifier when no independent
  external addressing requirement exists.
- Make a functional reference stable, opaque to consumers, and independent of
  the database primary-key strategy.
- Treat the reference as the public identity. Consumers must not infer database
  ordering, shard placement, or other implementation details from it.
- Let the owning business module define whether a reference exists and which
  business concept it identifies.
- Keep reference columns independent from the numeric primary-key type. A text
  representation is preferred when the reference has a versionable grammar.
- Protect persisted functional references with a uniqueness constraint.
- Do not create foreign keys from one module to another module's persistence
  identifier. Cross-module consistency is verified through the owner's public
  contracts.
- Internal relationships inside one owning module may still use technical
  identifiers when they do not escape that module.
- Choose the reference-generation mechanism separately. This ADR does not
  require UUID, ULID, a database sequence, or any custom generator.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the aggregates and entities that receive a functional reference;
- the entities that deliberately remain internally addressed;
- the public field and database-column names;
- the storage type and uniqueness constraints;
- any internal relationship that legitimately retains a technical identifier;
- the generation strategy adopted for each reference family;
- the module boundary that owns each reference;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project. Each project must explicitly
adopt it and list its concrete reference-bearing concepts.

## Alternatives considered

- Expose database primary keys through public contracts.
- Use a mutable business attribute as the external identity.
- Give every persisted entity a functional reference preemptively.
- Use cross-module foreign keys to another module's primary key.
- Couple the decision to one mandatory reference-generation algorithm.

## Justification

Separating public identity from persistence identity preserves module ownership
and keeps consumers independent of storage evolution. Applying the rule only to
objects that cross a boundary avoids unnecessary reference formats and indexes
for internal entities.

Keeping the generation mechanism separate allows a project to adopt the same
identity boundary with a UUID, ULID, database-backed sequence, or a custom
human-readable format.

## Positive consequences

- Persistence keys can evolve without changing public contracts.
- Module and service extraction no longer exposes another module's primary key.
- References remain usable in APIs, events, logs, and integrations.
- Internal entities avoid speculative public identities.
- Cross-module ownership becomes explicit.

## Negative consequences / trade-offs

- Referenced tables carry an additional unique column and index.
- Applications must map between technical identifiers and functional references.
- Text references may consume more index space than numeric keys.
- Reference formats become compatibility contracts once exposed.
- Cross-module referential integrity cannot rely on a foreign key to a private
  identifier.

## Technical impact

- Domain or application models that expose functional references.
- Persistence mappings and uniqueness constraints.
- Public contracts, events, and transport representations.
- Repository lookup methods based on functional references.
- Explicit internal-only use of technical identifiers.

## Validation

- Verify that no public or cross-module contract exposes another module's
  persistence identifier.
- Verify uniqueness of every persisted functional reference.
- Verify that externally addressed concepts can be loaded by their reference.
- Verify that internal-only entities have no speculative reference requirement.
- Verify that cross-module collaboration uses public contracts rather than
  direct primary-key relationships.

## Notes

This ADR decides which identity crosses an ownership boundary. Reference
grammar, generator implementation, prefixes, module names, and error codes
belong to separate technical or project-specific decisions.
