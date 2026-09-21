---
name: pedagogical-slidev
description: Design, generate, review, and update professional French TeamPulse Slidev courses from project resources through evidence-aligned pedagogy, scenario-based storytelling, accessible slide design, presenter notes, and executable proof.
---

# Pedagogical Slidev

Use this skill to transform TeamPulse resources into a course that helps a Java,
Spring, and Angular developer understand architecture through a realistic
problem, make a reasoned decision, implement it, and prove the result.

This is a learning-design workflow, not a Markdown exporter. The deck must make
the learner's reasoning visible before showing the technology or code.

## Operating contract

- Write the course in French for developers who know application development but
  do not yet know software architecture.
- Preserve the repository's actual terminology, source truth, visual system,
  navigation, manual slide content, and generated-zone boundaries.
- Separate facts, decisions, interpretations, assumptions, and missing sources.
- Do not invent implementation status, ADR content, tests, or guarantees.
- Treat the generated deck as `slides.md` plus imported pages, components,
  layouts, styles, assets, and presenter notes.
- A review-only request does not authorize edits.
- For a full generation or major update, present the intermediate learning model
  and slide plan before editing when the user expects checkpointed work. Continue
  only after validation of that plan.

## Source priority and traceability

Use sources in this order:

1. `docs/besoins/W00X/` for ticket requirements and expected behavior;
2. `docs/adr/W00X/` for architecture decisions and compromises;
3. committed code on the current Git branch for implemented behavior;
4. `docs/teampulse-roadmap-v9-code-infra-detaille.xlsx` for ordering, objectives,
   validations, artifacts, and continuity between weeks.

If sources conflict, prefer besoin and ADR Markdown for their respective scope,
use committed code to describe what is actually implemented, and report the
inconsistency. If an ADR is missing, keep the course useful but display an
explicit missing-ADR warning and do not manufacture a decision.

Every important claim in the learning model and every ticket section must retain
its source path or be marked as an interpretation, hypothesis, or gap.

## Pipeline

Run the pipeline in this order. The linked workflow is the detailed contract
for each stage.

```text
Resources
  -> Extraction des connaissances et des preuves
  -> Objectifs pédagogiques observables
  -> Critères de réussite et activités de vérification
  -> Progression et échafaudage pédagogique
  -> Storyline et scénario professionnel
  -> Découpage en slides et moments actifs
  -> Génération du deck Slidev
  -> Review pédagogique, cognitive, visuelle et accessibilité
  -> Validation technique et traçabilité
```

1. Read [workflows/analyze-resources.md](workflows/analyze-resources.md).
2. Read [workflows/build-learning-path.md](workflows/build-learning-path.md).
3. Read [workflows/build-storyline.md](workflows/build-storyline.md).
4. Read [workflows/generate-slides.md](workflows/generate-slides.md).
5. Read [workflows/review-deck.md](workflows/review-deck.md).

Read the references only when their stage requires them:

- [references/pedagogy.md](references/pedagogy.md) for learning design and
  quality gates;
- [references/storytelling.md](references/storytelling.md) for scenarios and
  continuity;
- [references/slidev-guidelines.md](references/slidev-guidelines.md) for the
  Slidev contract, source files, notes, and navigation;
- [references/visual-patterns.md](references/visual-patterns.md) for choosing
  a visual structure;
- [references/accessibility.md](references/accessibility.md) for inclusive and
  responsive presentation checks.

## Supported modes

Normalize week and ticket identifiers to `W001`, `W002`, and `W001-T01`.

- **Full generation**: initialize or reconcile the whole deck from available
  sources. Do not delete an existing deck or manual content.
- **Week update**: update one week and only the navigation/index dependencies
  required for consistency.
- **Ticket update**: update one ticket, its week summary when necessary, and
  its navigation and ADR status.
- **Presenter-notes update**: update oral notes without changing visible content
  unless requested.
- **Pedagogical review**: inspect and report without editing.
- **Deck review**: run the complete review workflow against an existing deck.

## Incremental editing

Preserve manual content outside stable generated zones:

```markdown
<!-- AUTO-GENERATED:W001:START -->
...
<!-- AUTO-GENERATED:W001:END -->
```

```markdown
<!-- AUTO-GENERATED:W001-T03:START -->
...
<!-- AUTO-GENERATED:W001-T03:END -->
```

Replace only the matching generated zone. Do not regenerate unrelated weeks or
tickets unless index or navigation consistency requires it.

## Validation and evals

Use the scripts for deterministic checks:

```bash
python3 .codex/skills/pedagogical-slidev/scripts/inventory_sources.py
node .codex/skills/pedagogical-slidev/scripts/validate_presenter_notes.mjs slidev/slides.md
```

Use [evals/README.md](evals/README.md) after modifying this skill or when a
regression is suspected. Evals test the skill's behavior with realistic raw
resources; they are not a substitute for the deck build or human pedagogical
judgment. Run the cases in an isolated workspace and evaluate them against the
rubrics in `evals/rubrics/`.

## Final report

Report the mode, week/ticket scope, sources used, knowledge gaps and conflicts,
learning objectives, files changed, notes added or refreshed, ADRs present or
missing, evals and deterministic checks run, build status, and any visual or
accessibility limitation. Distinguish generated, validated, reviewed, and
deferred work.
