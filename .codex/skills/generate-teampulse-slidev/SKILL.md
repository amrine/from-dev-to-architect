---
name: generate-teampulse-slidev
description: Generate, review, or update a professional French Slidev course and its presenter notes for TeamPulse from docs/besoins/W00X Markdown files, docs/adr/W00X ADR files, committed code, and the TeamPulse Excel roadmap. Use a scenario-first, progressive pedagogy for Java, Spring, and Angular developers who do not yet know software architecture. Use when Codex is asked to create the Slidev project, improve the global narration, add or refresh speaker notes, update or review numbered slides, update a week such as W001, update a ticket such as W001-T03, refresh navigation, preserve manual slide edits, or report missing TeamPulse ADRs and training content.
---

# Generate TeamPulse Slidev

Create or update the TeamPulse Slidev deck as a progressive training resource, not as a raw export of Excel or Markdown. Make architecture understandable from situations the learner already knows before introducing architecture vocabulary or tools.

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
   - Presenter-notes update: add, refresh, or review the oral notes without changing visible slide content unless requested.
   - Pedagogical review: assess the requested numbered slides without editing unless the user asks for changes.
2. Normalize week codes to `W001`, `W002`, `W010`; normalize ticket codes to `W001-T01`.
3. Run `scripts/inventory_sources.py` from this skill when useful to list available weeks, tickets, ADR matches, and missing ADRs.
4. Read the relevant besoin files and ADR files for the targeted week. Read the Excel roadmap only for complementary context.
5. Read both references before every generation, review, or update:
   - `references/pedagogical-framework.md` defines the learner profile, narrative progression, vocabulary rules, and pedagogical quality gates.
   - `references/slidev-requirements.md` defines source handling, project structure, design system, slide patterns, and technical quality gates.

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

Write in French for a learner who knows Java, Spring, and Angular but has no prior architecture knowledge. Follow the complete rules in `references/pedagogical-framework.md`.

Use this narrative cycle throughout the deck:

```text
situation concrete -> probleme observable -> besoin -> decision et compromis -> implementation TeamPulse -> preuve executable
```

Apply these non-negotiable rules:

1. Define a term in plain language before using it to reason. Add a TeamPulse example on first use.
2. Explain the functional need and observable guarantees before naming technologies.
3. Introduce a `WXXX` through context, actors, stakes, and a realistic scenario before listing technical problems or tickets.
4. Explain the whole week before its `TXX` sections: what it prepares, what it delivers, its constraints, its acceptance scenario, its ticket questions, and why their order matters.
5. Treat each ticket as a focused learning loop connected to the week scenario. Prefer 2 to 5 slides: reconnect to the problem, explain the concept, present the decision and trade-offs, show the TeamPulse implementation, then prove the result.
6. Keep one main teaching objective per slide. Do not open a sequence with source code, a tool catalogue, or unexplained acronyms.
7. Present ADRs as the record of an architectural choice only after the learner understands the problem and the alternatives.
8. Frame validation and Definition of Done as observable behavior in a human scenario, then show the commands or tests that provide evidence.

Use Mermaid only when it clarifies architecture, flow, CI/CD, modules, or infrastructure in under 30 seconds. Give every diagram a reading direction, a legend when needed, and a short conclusion.

## Presenter Notes

Every slide that is actually presented must have a useful French presenter note. Follow the detailed contract in `references/pedagogical-framework.md` and `references/slidev-requirements.md`.

Use Slidev's final HTML comment syntax:

```markdown
<!--
**Message à faire passer**

La conclusion essentielle de la slide.

**Déroulé oral**

Une explication naturelle que le formateur peut lire ou reformuler.

**Insister sur**

Le piège, le compromis ou la distinction à ne pas manquer.

**Transition**

La phrase qui prépare la slide suivante.
-->
```

Apply these rules:

1. Write notes for the current slide's teaching objective; do not merely repeat visible text.
2. Keep enough detail to read the note aloud, usually 70 to 140 words, while making the key reminder easy to scan.
3. Use `[click]` markers when they help synchronize the note with `v-click` or `v-clicks` reveals.
4. Keep the note as the final comment block of the slide. When an `AUTO-GENERATED:*:END` marker closes a slide, place the presenter note after that marker so Slidev does not expose the marker as the note.
5. Put notes in imported page files, not on `src:` import stubs in `slides.md`.
6. Refresh a note whenever the slide's problem, decision, proof, or transition changes.
7. Do not put secrets or private operational data in notes. Public builds include notes unless built with Slidev's `--without-notes` option.

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
6. Run the pedagogical quality gates from `references/pedagogical-framework.md`, including first-use vocabulary, scenario continuity, need-before-tool ordering, and one objective per slide.
7. Run `node .codex/skills/generate-teampulse-slidev/scripts/validate_presenter_notes.mjs slidev/slides.md` from the repository root. Fix missing notes and `AUTO-GENERATED` markers detected as notes.
8. Inspect every modified slide at `1280x720` and at a small viewport such as `390x844`, including the fully revealed click state. Fix clipping, overlap, unreadable diagrams, and content density.
9. Run `npm install` and `npm run build` from `slidev/` when dependencies and network access permit.
10. If starting the dev server is requested or useful, run the project and provide the local URL.

## Final Summary

Report:

- week and ticket processed;
- sources used;
- files created or modified;
- presenter notes added, refreshed, and validated;
- ADRs present and missing;
- inconsistencies between besoin, ADR, and Excel;
- limits or incomplete source material;
- verification commands run and their result.
