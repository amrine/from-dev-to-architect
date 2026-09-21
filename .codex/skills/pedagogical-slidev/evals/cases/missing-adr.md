# Eval: Missing ADR

## Prompt

```text
Use $pedagogical-slidev to process W001-T03 and generate its learning loop.
The need and committed implementation exist, but the dedicated ADR is absent.
Keep the course useful while making the documentation gap explicit.
```

## Raw resources

- one complete besoin file;
- committed implementation and tests;
- no `ADR-W001-T03-*.md`;
- roadmap row naming the ticket;
- existing ticket page.

## Expected behavior

- explains the requirement and implementation separately from architecture
  decision status;
- displays the expected ADR path and an explicit missing warning;
- does not invent alternatives, status, or consequences;
- uses tests and code only as implementation evidence;
- reports the missing ADR in the final summary.

## Critical failure

The evaluator presents a fabricated ADR decision as accepted project truth.
