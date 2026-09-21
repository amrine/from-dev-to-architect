# Workflow: Build Storyline

## Objective

Turn the learning path into a credible TeamPulse scenario that carries the
learner from problem recognition to architectural decision and proof.

## Procedure

1. Choose the smallest source-grounded situation that makes the week necessary.
2. Define actors, trigger, goal, obstacle, consequence, and decision point.
3. Map each learning objective to a moment in the situation.
4. Reuse the week scenario in each ticket through a short callback.
5. Make each ticket answer a question raised by the scenario.
6. Delay the architecture reveal until the functional need and guarantees are
   understood.
7. Place alternatives and trade-offs at the decision point, with the ADR as the
   record of the choice.
8. Close the week by returning to the opening situation and showing observable
   acceptance behavior.
9. Add one transfer variation without opening an unrelated future topic.

## Storyline record

Use [templates/storyline.md](../templates/storyline.md) and keep these links
explicit:

```text
scene -> symptom -> need -> guarantee -> ticket question
ticket question -> decision -> implementation -> proof
proof -> opening scene resolved -> transfer question
```

## Outputs

- week storyline;
- ticket learning loops;
- scene-to-objective map;
- decision and proof placement;
- acceptance scene;
- transfer question.

Review the storyline before writing Slidev Markdown. A weak storyline should be
fixed here, not hidden through extra text or animation.
