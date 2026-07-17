# TeamPulse Slidev Requirements

## Purpose

Build a professional Slidev deck that teaches TeamPulse progressively from `W001` onward to developers who know Java, Spring, and Angular but do not yet know software architecture. The deck must explain why each need exists, how it is decomposed, which architecture decisions are made, how to validate the work, and what learners should retain.

Do not make a simple Excel export. Do not copy Markdown verbatim. Transform the roadmap, besoin files, ADR files, and committed code into a scenario-first course that moves from familiar development situations to architecture decisions and executable proof.

`references/pedagogical-framework.md` is the authority for learner assumptions, narrative ordering, vocabulary introduction, week and ticket progression, and pedagogical quality gates. Apply it together with this technical and visual contract.

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

### Pedagogical review

Use when the user asks for an opinion, analysis, or review of numbered slides.

- Resolve actual Slidev numbers from slide separators and imported pages.
- Read the surrounding slides to evaluate the transition into and out of the requested range.
- Apply `references/pedagogical-framework.md` and report precise gaps in context, vocabulary, causality, compromise, or proof.
- Do not edit the deck unless the user explicitly asks for changes.

### Presenter-notes update

Use when the user asks to add, refresh, review, or improve speaker notes.

- Preserve visible slide content unless the user also requests content changes.
- Resolve imported pages and update the notes in their source Markdown files.
- Read surrounding slides so every note has an accurate transition.
- Validate every presented slide, not templates or `src:` import stubs.

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

Explain the whole `WXXX` before entering its tickets. Adapt the number of slides to the content, using this progression:

1. Contextual cover: product mission, actors, stakes, and expected change.
2. Realistic TeamPulse situation: a scenario that makes the problem visible.
3. Symptoms and consequences for users, developers, or operations.
4. Functional need and future product flow.
5. Observable guarantees, written before naming tools.
6. Scope and constraints: explicitly distinguish what the week prepares from what it delivers.
7. Technical mission: code, data/execution, and proof.
8. Learning contract: understand, build, and prove.
9. Human acceptance scene that makes the Definition of Done concrete.
10. Ticket breakdown expressed as learning questions, including merged or missing ticket numbers.
11. Explanation of why the ticket order matters.
12. Target architecture, introduced only after the need, with a reading direction and defined vocabulary.

Merge adjacent stages when the week is simple. Do not add filler. Keep the initial scenario alive through the section so the acceptance scene closes the story.

If tickets are not explicit, propose 3 to 7 logical tickets. A common split is:

- `T01` Understand the need and frame architecture.
- `T02` Implement application code.
- `T03` Implement infra/devops.
- `T04` Add tests and validations.
- `T05` Write ADR and artifacts.

## Ticket Section Template

For each ticket, create 2 to 5 focused slides that form one learning loop:

1. **Reconnect**: ticket question, week scenario, and symptom resolved.
2. **Understand**: plain-language concept definition and bridge from familiar Java/Spring/Angular knowledge.
3. **Choose**: credible alternatives, ADR decision, benefits, disadvantages, and conditions for revisiting the choice.
4. **Build**: TeamPulse files, modules, endpoints, configuration, scripts, tests, and useful commands. Include code only after its purpose is understood.
5. **Prove**: observable expected behavior, evidence, validation mechanism, and Definition of Done.

Combine stages when appropriate, but never omit the problem, accepted compromise, or proof. Do not begin a ticket sequence with source code or configuration.

## Pedagogical Blocks

Use concise recurring blocks:

- `À retenir`
- `Erreur fréquente`
- `Question d'entretien`
- `Exercice pratique`
- `Checklist de validation`
- `Lien avec l'architecture globale`
- `Pour aller plus loin`

## Presenter Notes Contract

Every presented slide must end with one non-empty HTML comment block that Slidev can parse as the presenter note.

```markdown
<!--
**Message à faire passer**

Conclusion essentielle.

**Déroulé oral**

Explication naturelle à lire ou reformuler.

**Insister sur**

Distinction, risque ou compromis important.

**Transition**

Lien causal vers la suite.
-->
```

Technical requirements:

- the note must be the last comment block of the slide;
- put the note after an `AUTO-GENERATED:*:END` marker when that marker closes the slide;
- never accept an `AUTO-GENERATED` marker as a parsed note;
- keep notes in imported Markdown pages rather than `src:` stubs;
- use Markdown inside notes and Slidev `[click]` markers when useful;
- keep notes synchronized with visible content, click order, ADRs, committed code, and transitions;
- do not include secrets because normal Slidev builds contain notes;
- use `slidev build --without-notes` for a public artifact that must exclude them.

Run from the repository root:

```bash
node .codex/skills/generate-teampulse-slidev/scripts/validate_presenter_notes.mjs slidev/slides.md
```

The command must report one valid presenter note for every presented slide.

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
9. The learner understands the problem and functional need before the architecture or tools appear.
10. New architecture terms and acronyms are defined before their first meaningful use.
11. Every technology maps to a stated need or guarantee, and every validation proves one.
12. The week states what it prepares, what it delivers, and why ticket order matters.
13. Every decision presents an accepted disadvantage or limitation.
14. Each slide has one main teaching objective.
15. Modified slides and fully revealed click states have no overlap, clipping, or unreadable content at `1280x720` and a small viewport such as `390x844`.
16. Every presented slide has a specific French presenter note.
17. No generated-zone marker is parsed as a presenter note.
18. Notes emphasize the teaching point, follow click order where relevant, and prepare the next slide.
19. The presenter-note validation script passes.
20. `npm run build` passes when dependencies are available.

## Final Report Shape

Include:

- semaine traitée;
- ticket traité;
- sources utilisées;
- slides mises à jour;
- notes présentateur ajoutées ou actualisées;
- résultat de validation des notes;
- ADR présents;
- ADR manquants;
- points d'attention;
- fichiers créés/modifiés;
- liens externes ajoutés;
- commandes de vérification.
