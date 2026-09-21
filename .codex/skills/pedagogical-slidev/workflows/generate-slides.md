# Workflow: Generate Slides

## Objective

Convert an approved learning path and storyline into maintainable Slidev pages,
visual patterns, and presenter notes.

## Inputs

- approved knowledge and evidence maps;
- approved learning path;
- approved storyline;
- existing Slidev deck and generated-zone markers;
- [references/slidev-guidelines.md](../references/slidev-guidelines.md);
- [references/visual-patterns.md](../references/visual-patterns.md);
- [references/accessibility.md](../references/accessibility.md).

## Procedure

1. Create a slide plan before writing Markdown. Each slide has one objective,
   one dominant relationship, one chosen visual pattern, one source set, one
   interaction or reveal decision, and one transition.
2. Generate the week before its tickets and keep ticket sections connected to
   the week scenario.
3. Use visible titles that advance the learner's question; do not start with
   source code, configuration, or unexplained acronyms.
4. Choose a visual pattern based on the objective. Use tables for comparisons,
   flows for causality, diagrams for ownership, and proof cards for evidence.
5. Introduce code only after its purpose and decision are understood.
6. Add retrieval, prediction, quiz, exercise, or explanation moments when the
   learning path requires active participation.
7. Add the French presenter note as the final HTML comment of every presented
   slide. Put notes in imported page files, not `src:` stubs.
8. Add ADR paths or explicit missing-ADR warnings to ticket sections.
9. Update macro and detailed navigation without touching unrelated manual work.
10. Use stable generated markers and replace only the requested generated zones.

## Slide contract

Use [templates/slide-plan.md](../templates/slide-plan.md). A slide is ready only
when its record answers:

```text
What should the learner do or understand?
What evidence supports it?
What must be visible?
What may be revealed progressively?
What does the presenter say?
What is the transition?
```

## Outputs

- `slidev/slides.md`;
- `slidev/pages/index.md` and relevant week pages;
- generated components, layouts, styles, and assets only when needed;
- presenter notes;
- navigation links;
- source references and ADR indicators.
