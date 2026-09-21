# TeamPulse Pedagogy

## Learner contract

Design for a developer who can read and write Java, Spring, and Angular code,
but has not yet learned software architecture or architectural decision-making.
The course must help the learner:

1. **Comprendre** the problem, vocabulary, constraints, and alternatives;
2. **Construire** the chosen solution in TeamPulse;
3. **Prouver** the expected behavior with observable evidence;
4. **Transférer** the reasoning to one changed constraint.

Do not assume prior knowledge of modular monoliths, bounded contexts, schemas,
migrations, containers, multi-tenancy, ADRs, CI/CD, cloud services, SLOs, or
GitOps.

## Design model

Use a hybrid model rather than a single branded framework:

- **Backward design / constructive alignment**: outcome, acceptable evidence,
  activity, then material;
- **4C/ID-inspired whole-task learning**: complete professional task, support,
  worked example, guided completion, independent attempt, transfer;
- **Scenario-based learning**: authentic TeamPulse situation and decision;
- **Cognitive load and multimedia principles**: segment, pre-train, signal,
  remove decoration, and keep related words and visuals close;
- **Retrieval and spacing**: ask learners to recall, predict, explain, and revisit
  important ideas across the deck or weeks;
- **UDL/accessibility**: provide clear representation, readable interaction, and
  more than one way to understand or express the conclusion.

These are design constraints, not reasons to name theories on visible slides.

## Sources méthodologiques

Use these sources as guidance for the skill's design decisions, not as content
to copy into the deck:

- [Backward design and constructive alignment](https://www.wgtn.ac.nz/teaching-support/lifecycle-of-teaching-a-course-archived/course-design/objectives)
  for the outcome -> evidence -> activity sequence;
- [Four-Component Instructional Design](https://www.4cid.org/about/)
  for complex professional tasks, support, procedural information, and
  part-task practice;
- [Mayer and Pilegard on segmenting and pre-training](https://www.cambridge.org/core/books/abs/cambridge-handbook-of-multimedia-learning/principles-for-managing-essential-processing-in-multimedia-learning/DD24C2F48B9B1277CE59F78276110258)
  for cognitive load and multimedia sequencing;
- [IES practice guide on organizing instruction](https://ies.ed.gov/ncee/wwc/PracticeGuide/1)
  for retrieval, spacing, worked examples, graphics, and explanatory questions;
- [CAST UDL Guidelines](https://www.cast.org/what-we-do/universal-design-for-learning/)
  for engagement, representation, action, expression, and accessibility.

Do not treat `5P` as the canonical backbone: several unrelated 5P models exist.
Use a named mnemonic only if it clarifies this workflow without replacing the
alignment, scaffolding, retrieval, and accessibility rules above.

## Learning contract

Every scope needs a learning contract before slide generation:

```text
Learner: who starts here?
Performance gap: what cannot they yet reason about or do?
Essential understanding: what must remain after the course?
Objective: what observable action will they perform?
Evidence: what would convince us they succeeded?
Activity: how will they practise or retrieve it?
Transfer: what changed condition will they handle?
```

Use observable verbs such as explain, distinguish, choose, map, implement,
diagnose, validate, justify, and adapt. Avoid `connaître`, `comprendre`, or
`voir` without a visible performance criterion.

## Alignment matrix

Maintain this chain for every important objective:

```text
objective -> evidence -> activity -> slide(s) -> validation
```

An objective is not complete because it has a definition slide. It is complete
when the learner can perform the intended action and the deck exposes evidence
of that action.

Distinguish:

1. **Behavior**: what the learner or system should do;
2. **Evidence**: what can be observed;
3. **Mechanism**: command, test, tool, or implementation used to obtain evidence.

## Cognitive progression

Use the smallest progression that fits the skill:

```text
familiar bridge
  -> plain-language concept
  -> pre-trained vocabulary
  -> worked TeamPulse example
  -> guided comparison or completion
  -> independent decision or implementation
  -> proof
  -> transfer variation
```

Reduce guidance as competence grows. Do not give a novice an unexplained
architecture diagram and call it practice. Do not force independent discovery
when a worked example is needed to reduce irrelevant cognitive load.

## Active learning

Use an active moment when the objective requires reasoning rather than recall:

- prediction before a reveal;
- retrieve a previous concept before introducing a dependent one;
- compare two options and justify a choice;
- complete a partial diagram, contract, or test;
- explain which evidence proves a guarantee;
- diagnose a failure and identify the missing boundary.

Provide the answer and reasoning after the attempt. A quiz without feedback is
not a complete learning loop.

## Narrative cycle

Use the cycle at deck, week, and ticket level:

```text
situation -> observable problem -> functional need -> guarantees
  -> alternatives -> decision and compromise -> implementation
  -> executable proof -> retained idea -> transfer
```

Do not start with a tool, framework, code sample, or architecture diagram. Every
technology must answer a previously stated need or guarantee.

## Week progression

Explain the whole week before its tickets:

1. contextual cover with mission, actors, stakes, and expected change;
2. concrete TeamPulse situation;
3. symptoms and consequences;
4. functional need;
5. observable guarantees without tool names;
6. scope, constraints, prepare versus deliver;
7. technical mission: code, execution/data, proof;
8. learning contract;
9. human acceptance scene;
10. ticket questions and order rationale;
11. target architecture after the need is understood.

Use the amount of slides justified by the content; never add filler to meet a
fixed count.

## Ticket progression

Use two to five focused slides when possible:

1. reconnect to the scenario and ticket question;
2. define the concept through a familiar bridge;
3. compare options and record the ADR decision;
4. show the TeamPulse implementation;
5. prove observable behavior and retain one idea.

Combine stages only when the learner can still identify the problem, choice,
accepted limitation, and proof.

## Vocabulary

First use has this shape:

```text
plain-language definition + TeamPulse example + role in current problem
```

Define a term before putting it in a title, diagram, comparison, or conclusion.
Do not define one unexplained term with another unexplained term.

## Decision quality

For a consequential architecture decision, show:

- why a decision is necessary now;
- at least two credible options;
- criteria relevant to TeamPulse;
- selected benefit;
- accepted cost or limitation;
- future condition that could justify revisiting it;
- ADR path and implementation evidence.

Never use universal claims such as `best practice` without context.

## Proof quality

State the expected human or system behavior first. Then show the evidence and
finally the mechanism that produces it. Keep implementation proof separate from
the guarantee it supports.

## Presenter notes

Every presented slide ends with one French HTML comment containing:

```markdown
<!--
**Message à faire passer**

Conclusion essentielle.

**Déroulé oral**

Explication naturelle qui relie les éléments visibles.

**Insister sur**

Piège, distinction ou compromis.

**Transition**

Lien causal vers la suite.
-->
```

Target roughly 70 to 140 words, adapt to complexity, and use `[click]` markers
when the oral sequence follows `v-click` or `v-clicks`. Notes must add oral
value, preserve the same vocabulary and scenario, and never contain secrets.

The note is the final comment block of the slide. If a generated zone closes the
slide, place the note after the `AUTO-GENERATED:*:END` marker. Put notes in the
imported page source, not on `src:` stubs.

## Pedagogical review rubric

Mark each item pass, issue, or not applicable:

1. The learner can state the problem before seeing the architecture.
2. The functional need precedes technical tools.
3. Objectives use observable verbs and have acceptable evidence.
4. Activities require the intended reasoning, not only recognition.
5. Guidance decreases toward an independent attempt.
6. Important ideas are retrieved or revisited where useful.
7. The scenario contains actors, trigger, obstacle, decision, and consequence.
8. The decision shows an accepted disadvantage or limitation.
9. The final proof closes the opening scenario.
10. Each slide has one main objective.
11. Every important term is defined before meaningful use.
12. The deck offers a bounded transfer question.
13. Notes are specific, coherent, and synchronized with reveals.
14. Accessibility and small viewport constraints are respected.
