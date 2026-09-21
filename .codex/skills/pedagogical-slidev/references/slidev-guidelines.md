# TeamPulse Slidev Guidelines

## Purpose

Implement the approved learning model as a professional, maintainable Slidev
deck. Do not copy Excel or Markdown verbatim and do not let Slidev structure
drive the learning design.

## Project structure

Maintain this structure when the corresponding content exists:

```text
slidev/
├── package.json
├── slides.md
├── pages/
│   ├── index.md
│   └── weeks/W001.md
├── components/
├── layouts/
├── styles/
├── setup/
└── public/
```

`slides.md` is the entry point, not necessarily the location of all slide
content. Imported pages, components, styles, and notes are part of the deck.

## Execution modes

- **Full generation**: create or reconcile the complete deck without deleting
  existing manual content.
- **Week update**: read the target need/ADR/code/roadmap and update only the
  target week plus required index/navigation links.
- **Ticket update**: update the target ticket and its week summary only when the
  ticket changes the week-level understanding.
- **Pedagogical review**: resolve actual Slidev separators and imported pages,
  read surrounding slides, report issues, and do not edit without permission.
- **Presenter-notes update**: update notes in imported page files while keeping
  visible content stable unless the user asks otherwise.

## Sources and ADRs

Use:

```text
docs/besoins/W00X/
docs/adr/W00X/
committed code on the current branch
docs/teampulse-roadmap-v9-code-infra-detaille.xlsx
```

Every ticket section displays its ADR path. If the ADR is absent, display an
explicit warning with the expected path; never invent the missing decision.

## Generated zones

Use stable markers for generated content:

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

Replace only the matching zone. Preserve manual content, manual slide edits,
notes, routes, and unrelated weeks. Review the diff before finishing.

## Navigation

Maintain both:

- macro navigation: `W001 -> W002 -> W003`;
- detailed navigation: `W001-T01 -> W001-T02`.

Ensure the index links to generated weeks, week pages link to index and adjacent
weeks, and ticket sections link to their week, adjacent tickets, index, and next
week when relevant.

## Frontmatter and design system

Use the existing TeamPulse design tokens and components. Never introduce custom
inline colors, fonts, or CSS classes that bypass `slidev/styles/index.css`.

Cover:

```yaml
layout: cover
class: tp-cover
```

Week or ticket section:

```yaml
layout: center
class: tp-section
transition: fade
```

Fonts remain:

```yaml
fonts:
  sans: Inter
  serif: Space Grotesk
  mono: JetBrains Mono
```

Use the existing `--tp-*` variables, utility classes, and components such as
`PulseLine`, `WeekNav`, `TaskNav`, `LearningCard`,
`ArchitectureDecisionCard`, `ValidationCard`, and `FurtherReading`.

Mermaid is allowed when it clarifies architecture, flow, modules, CI/CD, or
infrastructure quickly. Give complex diagrams `{scale: 0.85}`, a reading
direction, a legend when necessary, and a short conclusion.

## Slide and notes contract

For each slide, use [templates/slide-plan.md](../templates/slide-plan.md) and
choose a pattern from [visual-patterns.md](visual-patterns.md). Keep one main
objective, concise visible text, and code only after its purpose is explained.

Every presented slide ends with a non-empty French presenter note. The final
comment block must be the note. Never put notes on `src:` import stubs. A
generated-zone end marker must precede the note when it closes the slide:

```markdown
<!-- AUTO-GENERATED:W001:END -->

<!--
**Message à faire passer**
...
-->
```

Run:

```bash
node .codex/skills/pedagogical-slidev/scripts/validate_presenter_notes.mjs slidev/slides.md
```

## Package and commands

Follow the repository's package manager. Current repository guidance uses Yarn:

```bash
cd slidev
corepack enable
corepack prepare yarn@4.9.4 --activate
yarn install --immutable
yarn build
```

For local presentation:

```bash
yarn dev
```

Do not add dependencies when the existing Slidev/Vue stack solves the need.

## Technical and visual gates

Check:

1. `slidev/slides.md` exists and imports resolve;
2. index, week, ticket, previous, and next links work;
3. every ticket has an ADR path or explicit missing warning;
4. generated zones preserve unrelated manual content;
5. notes validate for every presented slide;
6. no generated marker is parsed as a note;
7. modified slides and fully revealed states fit `1280x720` and `390x844`;
8. diagrams are readable and have a conclusion;
9. the build passes when dependencies are available.

## Final report

State the scope, sources, files, generated zones, notes, ADR status, conflicts,
visual/accessibility limits, and each verification result. Distinguish what was
generated, reviewed, validated, deferred, or blocked.
