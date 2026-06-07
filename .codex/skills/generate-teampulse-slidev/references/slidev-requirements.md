# TeamPulse Slidev Requirements

## Purpose

Build a professional Slidev deck that teaches TeamPulse progressively from `W001` onward. The deck must explain why each need exists, how it is decomposed, which architecture decisions are made, how to validate the work, and what learners should retain.

Do not make a simple Excel export. Do not copy Markdown verbatim. Transform the roadmap, besoin files, and ADR files into a training resource for developers, tech leads, architects, and DevOps learners.

## Execution Modes

### Full generation

Use when no Slidev project exists or the user asks for a full deck.

- Create `slidev/`.
- Create `slides.md`, `pages/index.md`, week pages, reusable components, styles, and public folders.
- Generate available weeks from the sources.
- Initialize a sober professional training theme.
- Verify the deck starts and builds when possible.

### Week update

Use when the user asks to update a week such as `W001`.

- Read `docs/besoins/W001/`.
- Read `docs/adr/W001/`.
- Use the Excel roadmap for complementary context.
- Update the `W001` page and all `W001` ticket sections.
- Update navigation and index if needed.
- Do not touch unrelated weeks except for index/navigation consistency.

### Ticket update

Use when the user asks to update a ticket such as `W001-T03`.

- Update the ticket section first.
- Update the week summary if the ticket changes the week-level understanding.
- Update ADR links and ticket navigation.
- Avoid regenerating unrelated tickets.

## Source Rules

Read these folders for a target week:

```text
docs/besoins/W00X/
docs/adr/W00X/
```

Use this roadmap file:

```text
docs/teampulse-roadmap-v9-code-infra-detaille.xlsx
```

```text
Le code réellement commité dans la branche Git courante.
```

Useful roadmap sheets may include:

- `Planning Détaillé`
- `TeamPulse - Exécution`
- `TeamPulse Features`
- `TeamPulse Evolution`
- `LocalStack vs AWS`
- `Catalogue Formations`
- `Dashboard`

Extract week/sprint, `W00X`, objectives, code to produce, infra/devops/cloud, validation, artifacts, Definition of Done, skills, and architecture links.

## Besoin Files

Besoin files may be named like:

```text
W001-T01-socle-backend.md
W001-T02-docker-compose-postgresql.md
```

They may contain context, objective, business need, technical need, impacted files, expected implementation, validations, Definition of Done, teaching notes, dependencies, risks, constraints, and training points.

Respect a clear existing structure. If the besoin file is incomplete, enrich slides from the roadmap and project context, then report that the besoin should be improved.

## ADR Rules

Every roadmap ticket `W00X-TYY` must have a dedicated ADR under:

```text
docs/adr/W00X/
```

Expected naming:

```text
ADR-W001-T01-short-name.md
ADR-W001-T02-short-name.md
```

Each ticket slide must display the associated ADR path. If absent, display an explicit warning and propose the expected ADR filename.

ADR content may include context, decision, alternatives, positive and negative consequences, architecture impact, files/modules impacted, validation, status, links to other ADRs, and known limits.

## Week Section Template

Create a section for each week with:

1. Cover: title, objective, roadmap phase, skill, expected result, stack, ticket links.
2. Why this need exists: problem, timing, avoided risks, junior developer pitfalls, architecture contribution.
3. Architecture view: simple Mermaid diagram when useful.
4. Ticket breakdown: coherent with besoin files, ADRs, and roadmap fields.

If tickets are not explicit, propose 3 to 7 logical tickets. A common split is:

- `T01` Understand the need and frame architecture.
- `T02` Implement application code.
- `T03` Implement infra/devops.
- `T04` Add tests and validations.
- `T05` Write ADR and artifacts.

## Ticket Section Template

For each ticket, create 2 to 5 slides:

### Need

Include ticket name, objective, problem solved, TeamPulse context, and expected result.

### Teaching Explanation

Explain the concept, why it is useful, how it applies to TeamPulse, common mistakes, and good practices.

### Architecture Decision

Show ADR path, decision, alternatives, consequences, architecture impact, and missing ADR warning when applicable.

### Expected Implementation

List files to create or modify, impacted modules, endpoints, configuration, scripts, tests, and useful commands. Include code only when it is pedagogically useful.

### Validation

Show checks and Definition of Done. Include ADR existence and consistency as part of DoD.

## Pedagogical Blocks

Use concise recurring blocks:

- `À retenir`
- `Erreur fréquente`
- `Question d'entretien`
- `Exercice pratique`
- `Checklist de validation`
- `Lien avec l'architecture globale`
- `Pour aller plus loin`

## Design System

The TeamPulse design system is defined in `slidev/styles/index.css` and `slidev/components/`. All generated slides must use only these tokens, classes, and components. Never introduce custom inline colors, custom fonts, or new CSS classes outside `styles/index.css`.

### CSS design tokens

```css
--tp-ink: #0a0f1c        /* main background */
--tp-surface: #121b30    /* cards, code blocks */
--tp-surface-2: #182338  /* hover, headers */
--tp-line: #24304a       /* thin borders */
--tp-pulse: #2dd4bf      /* teal brand accent */
--tp-blue: #60a5fa       /* links, info */
--tp-amber: #fbbf24      /* warnings, deviations */
--tp-text: #e6edf7       /* body text */
--tp-muted: #8494b3      /* secondary text */
```

### Utility classes — reference table

| Class | Semantic | Where to use |
|---|---|---|
| `tp-kicker` | Mono uppercase eyebrow | Above every section h1 |
| `tp-pulse-text` | Teal `<span>` in titles | Brand word in cover h1 |
| `tp-subtitle` | Muted subtitle | Below cover h1 |
| `tp-cover-meta` | Flex badge row | Cover footer badges |
| `tp-section` | Section cover class | `class:` in week/ticket cover frontmatter |
| `tp-grid-2` | Two-column grid | Side-by-side content blocks |
| `tp-card` | Dark surface card | Any info/explain block |
| `tp-card--pulse` | Card + teal left border | Key rule, important decision |
| `tp-card--warn` | Card + amber left border | Risk, missing ADR, watchpoint |
| `tp-badge` | Mono pill badge | Status labels inline |
| `tp-badge--done` | Teal badge | Completed ticket/commit |
| `tp-badge--doc` | Blue badge | Documented, pending implementation |
| `tp-badge--warn` | Amber badge | Missing, deviation, alert |
| `tp-footref` | Absolute footer reference | Bottom-left source citation |
| `small` | `font-size: 0.82rem` | Secondary labels inside cards/tables |
| `muted` | `color: var(--tp-muted)` | De-emphasized text |

### Slide type patterns

**Cover slide** (`slides.md` frontmatter):
```yaml
layout: cover
class: tp-cover
```
```md
<div class="tp-cover-inner">
  <span class="tp-kicker">Roadmap technique · Semaine 001</span>
  <h1>Team<span class="tp-pulse-text">Pulse</span></h1>
  <PulseLine />
  <p class="tp-subtitle">…</p>
  <div class="tp-cover-meta">
    <span class="tp-badge tp-badge--done">…</span>
  </div>
</div>
```

**Week / ticket section cover** (first slide of each week or ticket section):
```yaml
layout: center
class: tp-section
transition: fade
routeAlias: w001
```
```md
<span class="tp-kicker">Semaine 001</span>
# Titre de la section
<PulseLine />
Tagline courte.
<div class="mt-4">
  <span class="tp-badge tp-badge--done">…</span>
</div>
```

**Standard content slide** (no frontmatter class needed):
```md
## Titre du slide

<div class="tp-grid-2">
  <div>
    <v-clicks>…</v-clicks>
  </div>
  <div>…</div>
</div>
```

**Warning / watchpoint card**:
```md
<div class="tp-card tp-card--warn">
  <h3>Titre alerte</h3>
  <p class="small muted">Description…</p>
</div>
```

**Key rule card**:
```md
<div class="tp-card tp-card--pulse mt-3">
Règle : texte…
</div>
```

**Source footer** (bottom of any slide with references):
```md
<div class="tp-footref">
Références : docs/besoins/W001 · docs/adr/W001
</div>
```

### Components

All components live in `slidev/components/` and must follow the same dark-surface visual language.

- `<PulseLine />` — animated teal ECG line with beat dot. Use after every section h1. Prop: `:width` (default 420).
- `<WeekNav />` — macro navigation: previous week, index, next week, ticket links.
- `<TaskNav />` — ticket navigation: previous ticket, week, next ticket, index, next week.
- `<LearningCard />` — pedagogical block with `title` and `type` (concept / warning / tip / interview / exercise).
- `<ArchitectureDecisionCard />` — ADR summary with `adr`, `decision`, `alternatives`, `impact`, `status`.
- `<ValidationCard />` — DoD checklist with `checks` and `dod`.
- `<FurtherReading />` — reference list with `items: [{ label, url, why }]`.

When creating a component not yet present, apply the dark-surface palette using CSS variables from `styles/index.css`. Never hardcode hex values inside components.

### Fonts

Declared in `slides.md` frontmatter — do not change:
```yaml
fonts:
  sans: Inter
  serif: Space Grotesk
  mono: JetBrains Mono
```
- Body: Inter
- Headings: Space Grotesk
- Kickers, badges, code, mono labels: JetBrains Mono

### Mermaid

`slidev/setup/mermaid.ts` already sets the dark theme matching `--tp-ink` / `--tp-pulse`. Do not override it inline. Use `{scale: 0.85}` on complex diagrams to avoid overflow.

### Global frontmatter (slides.md)

Do not remove or change these fields:
```yaml
theme: default
colorSchema: dark
highlighter: shiki
transition: slide-left
mdc: true
```

## Visual Components

Create simple reusable components:

- `WeekNav.vue`: previous week, index, next week, ticket links.
- `TaskNav.vue`: previous ticket, current week, next ticket, index, next week.
- `LearningCard.vue`: `title`, `type`; types may include `concept`, `warning`, `tip`, `interview`, `exercise`.
- `ArchitectureDecisionCard.vue`: `adr`, `decision`, `alternatives`, `impact`, `status`.
- `ValidationCard.vue`: `checks`, `dod`.
- `FurtherReading.vue`: `items` with `label`, `url`, `why`.

Keep the style professional, educational, clear, sober, modern, and readable in oral presentation. Avoid overloaded slides, raw copy/paste, gadget effects, and theory disconnected from TeamPulse.

## Internet Enrichment

Use the internet only when it materially improves the teaching value. Prefer official documentation:

- Slidev
- Spring Boot
- Docker
- PostgreSQL
- GitLab CI
- AWS
- Terraform
- Kubernetes
- OWASP
- CNCF
- Twelve-Factor App

Add links in `Pour aller plus loin` with one short sentence explaining why each link is useful. Do not copy long excerpts.

## Package

If `slidev/package.json` does not exist, create one close to:

```json
{
  "scripts": {
    "dev": "slidev --open",
    "build": "slidev build",
    "export": "slidev export",
    "format": "prettier --write ."
  },
  "dependencies": {
    "@slidev/cli": "latest",
    "@slidev/theme-default": "latest",
    "vue": "latest"
  },
  "devDependencies": {
    "prettier": "latest"
  }
}
```

Adapt to existing npm, pnpm, or yarn conventions.

## Quality Gates

Check:

1. `slidev/slides.md` exists.
2. Every generated week is linked from the index.
3. Every week links to previous/next weeks.
4. Every ticket is accessible from its week.
5. Every ticket mentions an ADR.
6. ADR paths are coherent.
7. Internal links are not broken.
8. Manual content outside `AUTO-GENERATED` zones is preserved.
9. `npm run build` passes when dependencies are available.

## Final Report Shape

Include:

- semaine traitée;
- ticket traité;
- sources utilisées;
- slides mises à jour;
- ADR présents;
- ADR manquants;
- points d'attention;
- fichiers créés/modifiés;
- liens externes ajoutés;
- commandes de vérification.
