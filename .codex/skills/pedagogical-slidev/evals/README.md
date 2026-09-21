# Pedagogical Slidev evals

## Purpose

These evals test whether the skill makes good decisions on realistic requests.
They validate the workflow, not just Markdown syntax or Slidev build success.

Use them after a substantial skill change, after a demonstrated regression, or
before trusting a new generation mode on the complete deck.

## Evaluation protocol

1. Create an isolated temporary workspace from the repository state or from the
   minimum fixture needed by the case.
2. Provide the evaluator with the case prompt, the skill folder, and only the
   raw resources listed by the case.
3. Do not provide the expected answer, suspected defect, or proposed fix.
4. Let the evaluator inspect sources and produce the intermediate artifacts and
   deck changes allowed by the case.
5. Collect the knowledge map, learning path, storyline, slide plan, generated
   files, review findings, and command output.
6. Score the result with the relevant rubrics in `rubrics/`.
7. Record failures as evidence. Change the skill only when the failure reveals
   a missing or incorrect instruction.

Keep eval work isolated so generated artifacts do not enter the working tree.
Do not use an eval to mutate production systems, send external messages, or
modify the real deck without explicit authorization.

## Pass criteria

- All critical checks pass.
- No source-fidelity blocker is present.
- No objective-to-evidence alignment blocker is present.
- No generated-zone or presenter-note blocker is present.
- Any missing ADR, source conflict, or unavailable command is reported.
- The evaluator distinguishes generated, reviewed, validated, and deferred work.

One failed critical case is enough to reject the skill revision until the cause
is understood.

## Cases

- `cases/full-generation.md`: build the complete learning model and deck plan.
- `cases/week-update.md`: update one week without touching unrelated weeks.
- `cases/missing-adr.md`: preserve uncertainty and expose the missing ADR.
- `cases/conflicting-sources.md`: apply source priority and report conflict.
- `cases/review-only.md`: review without editing.
- `cases/preserve-manual-content.md`: update a generated zone safely.

## Rubrics

- `rubrics/pedagogy.md`: objectives, progression, practice, transfer, and review.
- `rubrics/source-fidelity.md`: provenance, conflicts, scope, and ADR handling.
- `rubrics/slidev-quality.md`: Markdown, notes, navigation, zones, and build.
- `rubrics/accessibility.md`: readability, color, motion, diagrams, and mobile.
