# ADR-TECH-007 - Monotonic Java reference generation

## Status

Accepted

## Linked ticket

None. This is a reusable technical reference-generation decision.

## Context

An application may require readable, time-oriented functional references before
persistence, without depending on a database sequence. Generation must remain
testable, handle concurrent calls in one JVM, tolerate clock rollback, and state
its restart and multi-node limitations honestly.

Purely random identifiers provide strong entropy but do not satisfy a readable
prefix-and-date format. Database sequences centralize uniqueness but prevent
offline generation and couple the reference factory to persistence.

## Decision

- Provide a Java-pure `ReferenceFactory` contract with one operation:

  ```java
  String generate(String prefix);
  ```

- Validate the prefix strictly against `[A-Z]{3}`. Do not trim or normalize an
  invalid prefix silently.
- Keep validation of an existing reference in a separate stateless grammar
  contract. It receives the expected prefix and returns a neutral result rather
  than a project-specific error.
- Use this reference format:

  ```text
  <PREFIX>-<YEAR>-<DAY_MONTH>-<SUFFIX>
  ```

- Encode a twelve-character uppercase base-36 suffix as `MMMMMMNNNNCC`:
  - `MMMMMM` encodes logical milliseconds since the start of the logical UTC
    day;
  - `NNNN` is a four-character process-lifetime nonce;
  - `CC` is a two-character counter for one logical millisecond.
- Inject `java.time.Clock` and a controllable nonce source so time and entropy
  can be deterministic in tests.
- Generate the nonce once per factory lifecycle with a cryptographically strong
  random source in production.
- Keep the last logical instant and counter in one atomic state updated with a
  compare-and-set loop.
- When the physical clock advances, accept the new instant and reset the
  counter.
- When the physical clock stays equal or moves backward, retain the previous
  logical instant and increment the counter.
- After counter `ZZ`, advance the logical instant by one millisecond and reset
  the counter without blocking for the physical clock.
- Derive year, day-month, and milliseconds-in-day from the same accepted logical
  instant.
- Use one factory instance for one application runtime. Recreating factories
  fragments the monotonic state.
- Claim uniqueness only within one factory lifecycle in one JVM.
- Protect persisted references with a database uniqueness constraint. A
  collision after restart or across nodes is reported explicitly and does not
  trigger an unbounded generate-and-persist retry loop.
- Decide stable node identity or shared coordination before adopting this
  generator in a multi-instance deployment that requires deterministic global
  uniqueness.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the concrete factory and grammar types;
- the reference prefixes and their owning business concepts;
- the singleton or equivalent lifecycle wiring;
- the production clock and nonce wiring;
- the persistence uniqueness constraints;
- the error returned for an exceptional collision;
- the deployment topology and accepted uniqueness guarantee;
- the planned trigger for replacing or extending the algorithm;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project.

## Alternatives considered

- Expose a raw UUID or ULID.
- Generate references from a database sequence.
- Generate a fixed-length random suffix only.
- Use a synchronized counter.
- Reserve identifier blocks through a database or distributed coordinator.
- Encode a stable node identifier immediately.
- Allocate more suffix characters to the counter and fewer to restart entropy.
- Introduce a business-enum catalog in the shared generator.

## Justification

The six-character daily time segment covers every millisecond in one UTC day
without a fixed multi-year epoch. The four-character nonce provides
`36^4 = 1,679,616` process-lifetime values, while the two-character counter
provides `36^2 = 1,296` values for one logical millisecond.

Atomic state preserves monotonic behavior under concurrency and clock rollback
without a blocking lock. A database uniqueness constraint remains the final
integrity defense because a process restart or another JVM resets the local
state.

## Positive consequences

- References can be generated before persistence.
- Generation is independent of Spring and database access.
- The format remains readable and time-oriented.
- Tests control time, nonce, counter rollover, and clock rollback.
- Concurrent calls in one JVM share one monotonic state without a blocking lock.
- The multi-node limitation is explicit.

## Negative consequences / trade-offs

- The format is longer and more complex than a numeric identifier.
- Restart and multi-node uniqueness remain probabilistic.
- The exact suffix layout becomes a compatibility contract.
- A singleton lifecycle is required for the stated mono-JVM guarantee.
- A database uniqueness violation must still be translated by persistence code.
- Distributed deployment requires a later coordination decision.

## Technical impact

- Java-pure factory, grammar validator, and immutable generation state.
- Injected clock and nonce source.
- Atomic compare-and-set loop.
- Reference uniqueness constraints in persistence.
- Tests for formatting, concurrency, rollback, rollover, and collision handling.

## Validation

- Verify every accepted prefix and reject null, blank, lowercase, spaced, or
  incorrectly sized prefixes.
- Verify the complete grammar and fixed reference length.
- Verify deterministic output with a fixed clock and nonce.
- Verify unique results from concurrent calls in one JVM.
- Verify clock rollback, day/year transitions, and counter overflow.
- Verify that a persistence collision performs one write attempt and preserves
  the existing row.
- Verify that documentation never claims restart-safe or multi-node absolute
  uniqueness.

## Notes

The algorithm is project-agnostic, but business prefixes, type names, dependency
injection, error codes, and deployment replacement criteria belong to each
adopting project.
