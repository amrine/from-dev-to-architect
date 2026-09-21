# Eval: Full generation

## Prompt

```text
Use $pedagogical-slidev to design the complete TeamPulse course from the
available needs, ADRs, committed code, and roadmap. First produce the resource
map, objectives/evidence matrix, learning path, storyline, and slide plan. Do
not edit the deck until the plan is reviewable. Then generate the complete deck,
presenter notes, navigation, and validation report.
```

## Raw resources

- at least two weeks with one complete and one partially documented ticket;
- one matching ADR with an explicit trade-off;
- committed code that proves one behavior;
- roadmap rows with ordering and validation;
- an existing Slidev entry point with one manual section.

## Expected behavior

- separates facts, decisions, implementations, interpretations, and gaps;
- creates observable objectives and maps each to evidence and activities;
- introduces a whole-task scenario before architecture terminology;
- uses progressive support before independent reasoning;
- creates a slide plan before Markdown;
- preserves the manual section and uses generated markers;
- adds notes, navigation, ADR references, and validation results;
- reports source conflicts and limits.

## Critical failures

- starts from tools or code without stating the problem;
- invents missing source facts or ADR decisions;
- generates slides without an objective/evidence map;
- deletes manual content;
- omits presenter notes or treats notes as visible slide content.
