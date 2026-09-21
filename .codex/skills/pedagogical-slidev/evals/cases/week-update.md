# Eval: Week update

## Prompt

```text
Use $pedagogical-slidev to update W001 only. Re-read the needs, ADRs, committed
code, roadmap context, and existing W001 deck. Explain the learning path and
storyline changes before editing. Preserve unrelated weeks and manual content.
```

## Raw resources

- `docs/besoins/W001/` and `docs/adr/W001/`;
- one existing `pages/weeks/W001.md` with generated and manual zones;
- an unrelated `pages/weeks/W002.md`;
- index and navigation links;
- committed code for one changed ticket.

## Expected behavior

- scopes analysis and edits to W001 plus required navigation consistency;
- reads surrounding slides and preserves W002;
- refreshes the week scenario, ticket questions, notes, and links when needed;
- does not regenerate unrelated tickets;
- reports the exact files and zones changed.

## Critical failures

- rewrites W002 or unrelated manual content;
- skips the week-level learning path and edits only ticket text;
- loses navigation or presenter notes.
