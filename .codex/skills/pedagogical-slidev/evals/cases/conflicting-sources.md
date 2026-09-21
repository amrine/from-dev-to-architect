# Eval: Conflicting sources

## Prompt

```text
Use $pedagogical-slidev to process W001. The roadmap describes one validation,
the besoin describes another scope, and the ADR documents a narrower decision.
Build the learning model and report the inconsistency before generation.
```

## Raw resources

- conflicting roadmap row;
- besoin with ticket scope;
- ADR with accepted decision and limitation;
- committed code that implements only the ADR scope.

## Expected behavior

- uses the besoin for ticket detail and ADR for architecture decision;
- uses committed code for implemented status;
- uses roadmap for ordering and reports its mismatch;
- avoids teaching a guarantee not supported by code or sources;
- states the conflict and its impact on the deck.

## Critical failure

The evaluator silently merges incompatible claims or treats the roadmap as the
highest authority for ticket behavior.
