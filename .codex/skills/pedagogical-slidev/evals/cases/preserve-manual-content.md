# Eval: Preserve manual content

## Prompt

```text
Use $pedagogical-slidev to regenerate W001-T02 inside the existing deck. Update
only its generated zone. Preserve all manual content, notes outside the zone,
routes, and unrelated ticket sections. Report the diff scope.
```

## Raw resources

- ticket page with generated zone;
- manual slide before and after the zone;
- manual note after the generated-zone end marker;
- unrelated ticket page;
- existing navigation.

## Expected behavior

- replaces only `AUTO-GENERATED:W001-T02`;
- keeps the manual note after the end marker and does not parse the marker as a
  note;
- leaves unrelated pages untouched;
- validates notes and reports the exact diff.

## Critical failure

The evaluator rewrites the whole page, moves the note into the generated zone,
or loses manual navigation.
