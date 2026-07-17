# TeamPulse Pedagogical Framework

## Contents

1. Learner contract
2. Core narrative
3. Global deck progression
4. Week progression
5. Ticket progression
6. Vocabulary and progressive disclosure
7. Scenarios, decisions, and proof
8. Slide writing rules
9. Presenter notes
10. Pedagogical review
11. Quality gates

## Learner Contract

Design the course for a developer who:

- can read and write Java, Spring, and Angular code;
- understands controllers, services, repositories, configuration, and tests;
- has not yet learned software architecture, infrastructure architecture, or architectural decision-making;
- needs to understand why a solution exists before learning how to implement it.

Never assume prior knowledge of modular monoliths, bounded contexts, schemas, migrations, containers, multi-tenancy, ADRs, CI/CD, cloud services, SLOs, or GitOps.

The course must help the learner move through three outcomes:

1. **Comprendre**: explain the problem, vocabulary, and constraints in their own words.
2. **Construire**: implement the chosen solution in TeamPulse.
3. **Prouver**: demonstrate the expected behavior with observable evidence.

## Core Narrative

Use this cycle at deck, week, and ticket level:

```text
Situation concrète
  -> problème observable
  -> besoin fonctionnel et technique
  -> garanties attendues
  -> options possibles
  -> décision et compromis
  -> implémentation TeamPulse
  -> preuve exécutable
  -> idée à retenir
```

Do not invert this cycle. In particular:

- do not start with a tool, framework, code sample, or architecture diagram;
- do not present a decision before the problem and alternatives are understood;
- do not present commands as proof before stating the behavior they must demonstrate;
- do not use the roadmap or source files as a ready-made course outline without rewriting their logic for the learner.

Use a consistent causal chain. Every technology must answer a previously stated need or guarantee. Every validation must prove a previously stated guarantee.

## Global Deck Progression

The introductory narration must bridge the learner's current coding experience to architecture.

1. Start from a familiar development situation: several features, developers, modules, environments, or deployments must coexist.
2. Show what becomes difficult as the product grows: unclear responsibilities, uncontrolled dependencies, shared data, inconsistent execution, and risky integration.
3. Define architecture in plain language as the organization of responsibilities, dependencies, data, and execution.
4. Explain that architecture is a set of explicit choices and compromises, not a diagram or a technology catalogue.
5. Introduce the TeamPulse roadmap as a sequence of product problems to solve and capabilities to prove.
6. Define an ADR before displaying an ADR path: a short record of the context, choice, alternatives, and consequences.
7. Define an environment before comparing local, integration, staging, or production environments.
8. Announce the recurring learning method: understand, build, prove.
9. Transition into the first week through its business situation, not through its technology stack.

Keep the existing manual introductory slides when updating only one week or ticket. Improve them only when the user requests global narration or the targeted change requires a local transition.

## Week Progression

Explain a complete `WXXX` before entering its `TXX` tickets. Use 8 to 12 slides when the content justifies them; merge adjacent stages when the week is simple. Do not add filler to meet a count.

### 1. Contextual cover

State the week's product mission, actors, time pressure, and expected change. The title must express an outcome or problem, not only a week code or stack.

### 2. Situation

Tell a realistic TeamPulse scenario with named roles and a moment in the project. A useful pattern is a Monday-to-Friday integration story. Make the learner able to imagine the scene before showing technical symptoms.

### 3. Symptoms and consequences

Show observable symptoms caused by the situation. Connect each symptom to a consequence for a developer, user, team, or operation. Avoid abstract lists such as "scalability, security, maintainability" without an event that makes the risk visible.

### 4. Functional need

Describe the product behavior the week must enable. Show the future user or team flow before the technical architecture.

### 5. Observable guarantees

Translate the need into statements that can later be proven. Write them without naming tools when possible.

Example:

- weak: `Utiliser PostgreSQL et Flyway`;
- stronger: `Chaque domaine démarre avec sa structure de données versionnée et reproductible`.

### 6. Scope and constraints

State what the week prepares, what it delivers now, what remains outside the scope, and which existing constraints cannot be ignored. Make the difference between **préparer** and **livrer** explicit.

### 7. Technical mission

Summarize the checkpoint through three concrete outputs:

- **Code**: what will exist in the repository;
- **Données / exécution**: what behavior or infrastructure will exist;
- **Preuve**: what can be run or observed to accept the result.

### 8. Learning contract

State what the learner will understand, build, and prove during the week. Use concrete verbs and avoid vague outcomes such as "connaître Docker".

### 9. Acceptance scene

Return to the initial scenario at a precise moment, for example `Vendredi 17 h`. Describe what each actor can now do and what failures are prevented. This is the human-readable Definition of Done.

### 10. Ticket questions

Present tickets as questions the learner must answer, not as a backlog dump.

Example:

| Ticket | Question pédagogique |
|---|---|
| `T01` | Comment organiser le code sans mélanger les responsabilités ? |
| `T02` | Comment rendre les données locales reproductibles et isolées ? |

Explain merged, removed, or missing ticket numbers explicitly. Then explain why the ticket order reduces uncertainty or enables the next step.

### 11. Target architecture

Only now show the target architecture. Read it layer by layer in the same direction as the diagram. Define every new term, identify what changed during the week, and finish with one sentence that answers the initial problem.

## Ticket Progression

Use 2 to 5 focused slides for each `WXXX-TYY`. Preserve continuity with the week scenario.

1. **Reconnect**: restate the ticket question and the symptom it resolves.
2. **Understand**: define the concept in plain language and bridge from a familiar Java/Spring/Angular example.
3. **Choose**: compare realistic options, present the decision, and make benefits and disadvantages visible. Introduce the ADR here, not earlier.
4. **Build**: show the TeamPulse implementation, files, configuration, code, and commands needed for the decision.
5. **Prove**: state observable success first, then tests, logs, commands, or screenshots that prove it. End with one idea to retain.

Combine stages when a ticket is small, but never omit the problem or proof. Do not repeat the full week context on every ticket; use a short callback to maintain the thread.

## Vocabulary And Progressive Disclosure

Apply the first-use rule:

```text
term = plain-language definition + TeamPulse example + role in the current problem
```

Introduce only the vocabulary needed to understand the current decision. Define a term before using it in a title, diagram, comparison, or conclusion.

Terms that commonly require a definition include:

- module and modular monolith;
- responsibility and dependency;
- schema, migration, and Flyway history table;
- `DataSource` and database connection;
- container and Docker Compose;
- integration test and Testcontainers;
- tenant and multi-tenancy;
- environment and profile;
- ADR;
- CI/CD, SLO, GitOps, and infrastructure as code.

Do not define terms with other unexplained terms. Use a familiar bridge when possible:

```text
Ce que vous connaissez -> limite rencontrée -> nouveau concept -> exemple TeamPulse
```

## Scenarios, Decisions, And Proof

### Scenario quality

A scenario needs actors, a trigger, a goal, an obstacle, and a consequence. Reuse the same scenario across the week so the final proof closes the loop opened at the beginning.

### Decision quality

Present at least two credible options when an architectural choice matters. Explain:

- why the chosen option fits the current TeamPulse constraints;
- which benefit it provides now;
- which cost or limitation the team accepts;
- which future condition could justify revisiting it.

Avoid declaring a solution "best practice" without context.

### Proof quality

Distinguish three levels:

1. **Expected behavior**: what a person or system should observe.
2. **Evidence**: test result, log, database state, HTTP response, or screenshot.
3. **Mechanism**: command or tool used to obtain that evidence.

A Spring context that fails on an invalid database setup may be sufficient evidence; do not add redundant tests merely to inspect tables or schemas unless the source requirement demands them.

## Slide Writing Rules

- Write in direct, natural French suitable for oral teaching.
- Give each slide one main question or teaching objective.
- Prefer a specific title that advances the story over a generic title such as `Architecture` or `Contexte`.
- Keep visible text concise; move detailed teaching notes to presenter notes when the deck uses them.
- Use progressive clicks only when they reveal a causal sequence or support oral explanation.
- Keep code excerpts short and tied to a previously explained decision.
- Use diagrams only when their message can be understood in under 30 seconds.
- Add a reading direction and short conclusion to every non-trivial diagram.
- Do not use a cold section cover that contains only codes, stack labels, or generic objectives.
- Do not describe slide mechanics, keyboard shortcuts, visual styling, or how to read the presentation in visible course text.

## Presenter Notes

Treat presenter notes as the oral layer of the course. They must let the trainer either read a coherent explanation or scan the point that deserves emphasis.

Every presented slide needs one specific note with this structure:

1. **Message à faire passer**: the single conclusion the learner should retain.
2. **Déroulé oral**: a natural explanation that connects visible elements instead of reading them verbatim.
3. **Insister sur**: a misconception, distinction, accepted compromise, or teaching risk.
4. **Transition**: a causal bridge to the next slide or a clear closing sentence.

Use these writing rules:

- write in natural French suitable for speaking aloud;
- target roughly 70 to 140 words, adapting to the slide's complexity;
- preserve the same scenario and vocabulary progression as the visible course;
- explain how to read diagrams, code, tables, and proofs without narrating their visual styling;
- use `[click]` markers when the oral explanation must follow progressive reveals;
- do not duplicate every visible bullet or introduce a new architectural decision only in the notes;
- do not reference a fixed slide number in the note, because imports and future tickets can change numbering;
- update the transition when slides are inserted, removed, merged, or reordered;
- keep notes free of credentials, private URLs, tokens, or information that must not appear in a public build.

Use the final HTML comment block as required by Slidev. A generated-zone end marker must appear before the note when it closes the slide:

```markdown
<!-- AUTO-GENERATED:W001:END -->

<!--
**Message à faire passer**
...
-->
```

Do not add notes to `src:` import stubs. Add them to the slides inside the imported Markdown file.

When reviewing existing notes, verify that they still match the slide's objective, current source code, ADR decision, visible click order, and next-slide transition. A note that was once correct can become misleading after a content update.

## Pedagogical Review

When asked to review numbered slides:

1. Resolve the actual slide numbers from Slidev separators and imported pages; do not infer them from headings alone.
2. Read the slides before and after the requested range to assess transitions.
3. Identify the assumed learner knowledge on each slide.
4. Report where context, definition, causality, compromise, or proof is missing.
5. Prefer precise rewrites or sequencing changes over adding more text.
6. Do not edit files when the user asks only for an opinion or explanation.

## Quality Gates

Before finishing, answer all of these questions:

1. Can the learner explain the problem before seeing the chosen architecture?
2. Does the week begin with a concrete situation instead of a problem list or tool list?
3. Is the functional need explained before the technical response?
4. Is the difference between what the week prepares and delivers explicit?
5. Does every important technology answer a stated need or guarantee?
6. Is every new term defined before its first meaningful use?
7. Are tickets presented as learning questions, with their order justified?
8. Does every decision show at least one accepted disadvantage or limitation?
9. Does the final proof close the initial scenario with observable behavior?
10. Does each slide have only one main teaching objective?
11. Can every diagram be understood in under 30 seconds with its reading order?
12. Are acronyms and architecture terms sparse enough for the learner profile?
13. Do fully revealed click states fit at desktop and small viewports without overlap or clipping?
14. Did the update preserve unrelated manual content and generated zones?
15. Does every presented slide have a note that can be read aloud and scanned quickly?
16. Does each note add oral value instead of repeating the visible content?
17. Are `[click]` markers coherent with progressive reveals where they are used?
18. Do notes preserve the scenario, emphasize the important distinction, and prepare the next slide?
19. Are generated-zone markers excluded from parsed presenter notes?

If one answer is no, revise the affected sequence before considering the deck complete.
