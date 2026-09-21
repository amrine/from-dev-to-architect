# Workflow: Review Deck

## Objective

Review the generated deck as a learning experience and as a functioning Slidev
artifact. Report issues precisely and fix only when the request authorizes it.

## Review passes

### 1. Source fidelity

- claims have source paths or are clearly marked as interpretation;
- besoin and ADR priorities are respected;
- committed code is not described as planned work;
- missing ADRs and source conflicts are visible;
- scope and non-scope are preserved.

### 2. Alignment

- every important objective has evidence and an activity;
- every slide maps to an objective or necessary transition;
- every validation proves a stated guarantee;
- no orphan content remains in the main path.

### 3. Pedagogy and cognition

- the learner sees the situation before the architecture;
- vocabulary is defined before meaningful use;
- the path includes appropriate scaffolding and active recall;
- examples progress toward independent reasoning;
- the learner is asked to discriminate options and explain trade-offs;
- the final proof closes the opening scenario;
- the deck includes a bounded transfer question.

### 4. Storyline

- actors, trigger, obstacle, decision, and consequence are coherent;
- ticket questions follow the week uncertainty-reduction order;
- callbacks do not become repetitive recitation;
- the chosen decision is not presented as universal best practice.

### 5. Visual and accessibility

- one main objective per slide;
- no clipping, overlap, unreadable diagrams, or excessive density;
- fully revealed click states work at `1280x720` and around `390x844`;
- color is not the only semantic signal;
- diagrams have reading direction and conclusion;
- notes provide an accessible oral explanation.

### 6. Technical integrity

- imports, route aliases, index, week, ticket, and next/previous links work;
- generated zones preserve manual content;
- every presented slide has a valid note;
- the note parser does not treat generated markers as notes;
- the Slidev build passes when dependencies are available.

## Outputs

Produce an issue list with:

```text
severity: blocker | major | minor
location: page/slide/section
principle violated
evidence
recommended correction
status: fixed | deferred | accepted limitation
```

Never hide a source conflict or a missing validation behind a visual rewrite.
