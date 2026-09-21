# TeamPulse Storytelling Guide

## Purpose

Storytelling gives the learner a reason to care about a decision. It must make
the architecture problem concrete without inventing business facts or turning
the course into fiction disconnected from the repository.

## Scenario model

Every week scenario contains:

```text
actors + trigger + goal + obstacle + consequence + decision point + outcome
```

Use the same causal thread across the week:

```text
Monday: a real product situation
  -> symptoms become visible
  -> the team states a functional need
  -> alternatives are compared
  -> a decision is recorded
  -> TeamPulse implements the decision
  -> Friday: an actor observes the accepted behavior
```

The scenario is anchored in source evidence. If a role, event, or consequence
is pedagogical reconstruction, label it as such internally and do not present it
as a project fact.

## Week arc

Build the week in this order:

1. **Situation**: who is working on what and at which moment;
2. **Friction**: what fails, slows down, or becomes unsafe;
3. **Need**: what the product or team must now be able to do;
4. **Guarantees**: what must be observable regardless of technology;
5. **Decision**: credible options, chosen trade-off, and rejected costs;
6. **Implementation**: where the choice lives in TeamPulse;
7. **Proof**: which human or system behavior demonstrates success;
8. **Transfer**: what changes if context, scale, or constraints change.

Do not reveal the target architecture before the friction and need are clear.

## Ticket arc

Each ticket is a short learning loop:

```text
callback to the scenario
  -> question to answer
  -> concept and prerequisite
  -> worked TeamPulse example
  -> guided decision or exercise
  -> implementation
  -> proof and retained idea
```

Use two to five slides when possible. Merge stages only when the learner can
still identify the problem, choice, accepted compromise, and proof.

## Decision scenes

An architectural decision scene must expose:

- the context that makes the decision necessary;
- at least two credible alternatives when the choice is consequential;
- the criterion that matters now;
- the benefit obtained immediately;
- the cost or limitation accepted;
- the future condition that could justify revisiting the decision;
- the ADR path that records the decision.

Never describe an option as universally best. Explain why it fits this
TeamPulse context.

## Continuity rules

- Reuse names and vocabulary consistently within a week.
- Use a short callback instead of repeating the whole context on every ticket.
- End the week by returning to the opening situation.
- Let the proof answer the same question raised at the beginning.
- If the source material cannot support a story detail, use a neutral scenario
  or state the limitation instead of filling the gap with invented facts.

## Transfer

After the local TeamPulse proof, ask one bounded transfer question:

- What changes if there are two organizations instead of one?
- What changes if the process restarts?
- What changes if the database is unavailable?
- Which assumption would invalidate this decision?

Transfer is a short bridge, not an invitation to teach an unrelated future
topic.
