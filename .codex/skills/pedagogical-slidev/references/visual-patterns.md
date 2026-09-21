# TeamPulse Visual Patterns

Choose a pattern because it clarifies the current learning objective. Do not
use a visual merely to decorate a slide.

## Pattern catalogue

| Pattern | Best for | Required conclusion |
|---|---|---|
| Situation card | actors, trigger, consequence | why the learner should care |
| Before / after | observable change | what is different for the actor |
| Causal flow | problem to guarantee | which cause leads to which effect |
| Comparison matrix | alternatives and trade-offs | why the chosen option fits now |
| Layered architecture | responsibilities and dependencies | how the layers answer the problem |
| Decision card | ADR summary | what was chosen and what it costs |
| Worked example | implementation reasoning | how the decision becomes code |
| Proof card | tests, logs, database, HTTP | what evidence proves which guarantee |
| Quiz / prediction | retrieval and misconception | why the answer is correct |
| Exercise / completion | guided practice | what the learner can now attempt |
| Timeline | evolution and sequence | why this order matters |
| Checklist | Definition of Done | what remains observable |

## Selection rules

- One slide has one main teaching objective.
- Prefer one dominant visual relationship over several small decorations.
- Use a table for exact comparisons, a flow for causality, a timeline for
  sequence, and a diagram for ownership or dependencies.
- A diagram must have a reading direction, a legend when needed, and a short
  conclusion.
- Keep code excerpts short and show only the lines that prove the concept.
- Use `v-click` or `v-clicks` only for a meaningful causal or decision sequence.
- Do not rely on color alone to convey status, ownership, or success.

## Reveal design

Use this progression when it reduces cognitive load:

```text
context -> symptom -> need -> guarantee -> option -> decision -> proof
```

Do not reveal a complete target architecture before the learner has a reason
to inspect it. Do not hide information that is required to interpret the next
step.

## Pattern quality check

Before keeping a visual, ask:

1. Can the learner state what relationship the visual shows?
2. Is the reading order obvious without the presenter?
3. Is every element necessary for this objective?
4. Does the visual fit a desktop and small viewport?
5. Does the presenter note explain the insight rather than recite labels?
