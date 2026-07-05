---
name: generate-teampulse-slidev
description: Generate or update a professional French Slidev training deck for the TeamPulse project from docs/besoins/W00X Markdown files, docs/adr/W00X ADR files, and the TeamPulse Excel roadmap. Use when Codex is asked to create the initial Slidev project, update slides for a week such as W001, update slides for a ticket such as W001-T03, refresh Slidev navigation, preserve manual slide edits, or report missing TeamPulse ADRs and training content.
---

# Generate TeamPulse Slidev

Create or update the TeamPulse Slidev deck as a progressive training resource, not as a raw export of Excel or Markdown.

## Source Priority

Use sources in this order:

1. Les fichiers Markdown des besoins :
   `docs/besoins/W00X/`
2. Les fichiers Markdown des ADR :
   `docs/adr/W00X/`
3. Le code réellement commité dans la branche Git courante.
4. Le fichier Excel de roadmap :
   `/Users/maamrine/IdeaProjects/teampulse-parent/docs/teampulse-roadmap-v9-code-infra-detaille.xlsx`

Treat Markdown besoin files as the source of truth for ticket details. Treat ADR files as the source of truth for architecture decisions. Use the Excel roadmap for global ordering, objectives, validations, expected artifacts, and continuity between weeks.

If sources conflict, prefer besoin/ADR Markdown and report the inconsistency in the final summary. If ADRs are missing, keep generating useful slides but explicitly mark the ADR as missing.

## Before Editing

1. Identify the requested mode:
   - Full generation: create or initialize `slidev/`.
   - Week update: update one week such as `W001`.
   - Ticket update: update one ticket such as `W001-T03`.
2. Normalize week codes to `W001`, `W002`, `W010`; normalize ticket codes to `W001-T01`.
3. Run `scripts/inventory_sources.py` from this skill when useful to list available weeks, tickets, ADR matches, and missing ADRs.
4. Read the relevant besoin files and ADR files for the targeted week. Read the Excel roadmap only for complementary context.
5. Read `references/slidev-requirements.md` before every generation or update — it contains the full design system, slide type patterns, and component contracts that must be applied consistently.

## Design System

All slides must use the TeamPulse design system defined in `slidev/styles/index.css` and `slidev/components/`. Never introduce custom inline styles that override these tokens.

### CSS tokens (variables)

| Variable | Usage |
|---|---|
| `--tp-ink` | Main background (`#0a0f1c`) |
| `--tp-surface` | Card / block background |
| `--tp-surface-2` | Hover / header background |
| `--tp-line` | Thin borders |
| `--tp-pulse` | Teal accent — primary brand color (`#2dd4bf`) |
| `--tp-blue` | Secondary links / info (`#60a5fa`) |
| `--tp-amber` | Warnings / deviations (`#fbbf24`) |
| `--tp-text` | Body text (`#e6edf7`) |
| `--tp-muted` | Secondary text (`#8494b3`) |

### CSS utility classes

| Class | Usage |
|---|---|
| `tp-kicker` | Mono uppercase eyebrow above a title |
| `tp-pulse-text` | Teal highlight within a title (`<span>`) |
| `tp-subtitle` | Muted subtitle below a cover h1 |
| `tp-cover-meta` | Flex row of badges on the cover |
| `tp-section` | Layout class for week/ticket section covers (`layout: center`) |
| `tp-grid-2` | Two-column grid for side-by-side content |
| `tp-card` | Dark surface card with border |
| `tp-card--pulse` | Card with teal left border (key info) |
| `tp-card--warn` | Card with amber left border (alert/warning) |
| `tp-badge` | Pill badge (mono font) |
| `tp-badge--done` | Teal badge for completed items |
| `tp-badge--doc` | Blue badge for documented items |
| `tp-badge--warn` | Amber badge for deviations / missing |
| `tp-footref` | Absolute-positioned footer reference line |
| `small` | Reduced font size (`0.82rem`) |
| `muted` | Muted text color (`--tp-muted`) |

### Components

- `<PulseLine />` — animated teal ECG line; use after every section h1. Accepts optional `:width` prop (default 420).
- `<WeekNav />`, `<TaskNav />`, `<LearningCard />`, `<ArchitectureDecisionCard />`, `<ValidationCard />`, `<FurtherReading />` — create in `slidev/components/` following the same dark-surface style if not yet present.

### Frontmatter conventions per slide type

**Cover (`slides.md` root)**
```yaml
layout: cover
class: tp-cover
```

**Week / ticket section cover**
```yaml
layout: center
class: tp-section
transition: fade
```

**Standard content slide** — no layout or class needed (inherits `default`).

**Annexe / checklist slide**
```yaml
layout: default
```

### Mermaid

Mermaid is pre-configured in `slidev/setup/mermaid.ts` to match the dark palette. Use `{scale: 0.85}` annotation on complex diagrams. Do not override theme variables inline.

### Fonts

Always declared in `slides.md` frontmatter and must remain:
```yaml
fonts:
  sans: Inter
  serif: Space Grotesk
  mono: JetBrains Mono
```

## Slidev Structure

Create or maintain this structure:

```text
slidev/
  package.json
  slides.md
  pages/
    index.md
    weeks/
      W001.md
      W002.md
  components/
    WeekNav.vue
    TaskNav.vue
    LearningCard.vue
    ArchitectureDecisionCard.vue
    ValidationCard.vue
    FurtherReading.vue
  layouts/
  styles/
  public/
```

If `slidev/` already exists, update it incrementally. Do not destroy an existing deck or unrelated manual work.

## Incremental Editing Rules

Preserve manual content outside generated zones. Use stable generated markers:

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

Replace only the matching generated zone when it exists. Add a generated zone when it does not exist. Do not regenerate unrelated weeks or tickets unless navigation/index consistency requires it.

## Pedagogical Output

Write in French with a direct technical training tone. For each week, explain:

- why the need exists;
- how the week fits the roadmap;
- the ticket breakdown;
- architecture decisions and ADR links;
- validation and Definition of Done;
- what learners should remember.

For each ticket, prefer 2 to 5 focused slides:

- business/technical need;
- teaching explanation;
- ADR and decision;
- expected implementation;
- validation and Definition of Done.

Use Mermaid only when it clarifies architecture, flow, CI/CD, modules, or infrastructure in under 30 seconds.

## Navigation

Maintain two navigation paths:

- macro navigation between weeks: `W001 -> W002 -> W003`;
- detailed navigation inside a week: `W001-T01 -> W001-T02`.

Ensure week pages link to previous/next weeks, index, and tickets. Ensure ticket sections link back to the week, previous/next ticket, index, and next week when relevant.

## Quality Checks

After edits:

1. Verify `slidev/slides.md` exists.
2. Verify the index links to generated weeks.
3. Verify week and ticket navigation.
4. Verify every ticket mentions an ADR path or an explicit missing ADR warning.
5. Verify generated zones preserved manual sections.
6. Run `npm install` and `npm run build` from `slidev/` when dependencies and network access permit.
7. If starting the dev server is requested or useful, run the project and provide the local URL.

## Final Summary

Report:

- week and ticket processed;
- sources used;
- files created or modified;
- ADRs present and missing;
- inconsistencies between besoin, ADR, and Excel;
- limits or incomplete source material;
- verification commands run and their result.
