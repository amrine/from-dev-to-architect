# Eval: Review without editing

## Prompt

```text
Use $pedagogical-slidev to review slides 20 to 28 of the existing deck. Report
pedagogical, narrative, source-fidelity, accessibility, and Slidev issues. Do
not edit any file.
```

## Raw resources

- existing `slidev/slides.md` and imported pages;
- a transition into and out of the requested range;
- one intentionally dense slide;
- one note with stale transition text.

## Expected behavior

- resolves actual Slidev separators and imported page locations;
- reads surrounding slides;
- reports precise issues with evidence and severity;
- does not edit the deck;
- distinguishes a recommendation from a confirmed source defect.

## Critical failure

The evaluator modifies files or reviews only headings without reading the slide
content and surrounding transition.
