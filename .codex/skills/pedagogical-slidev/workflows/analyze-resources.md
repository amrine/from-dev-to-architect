# Workflow: Analyze Resources

## Objective

Turn raw TeamPulse resources into a traceable knowledge and evidence map before
deciding what to teach.

## Inputs

- targeted week or ticket, if any;
- `docs/besoins/W00X/`;
- `docs/adr/W00X/`;
- committed code on the current branch;
- TeamPulse roadmap workbook;
- existing Slidev pages when updating an existing deck.

## Procedure

1. Normalize the scope to a week or ticket code.
2. Run the source inventory script when useful:

   ```bash
   python3 .codex/skills/pedagogical-slidev/scripts/inventory_sources.py --target W001
   ```

3. Read besoin files and extract problem, need, constraints, expected behavior,
   dependencies, validation, Definition of Done, and teaching points.
4. Read matching ADRs and extract context, decision, alternatives, consequences,
   status, impacted modules, validation, and limits.
5. Inspect committed code only to describe implemented behavior and actual
   package/file locations.
6. Use Excel to fill ordering, objectives, artifacts, and continuity. Do not let
   Excel silently override a besoin or ADR.
7. Compare sources and record conflicts, missing ADRs, stale documentation,
   unverified claims, and scope boundaries.
8. Read the existing deck only to identify manual content, generated zones,
   existing navigation, and learner assumptions that must be preserved.

## Knowledge record

For each important item, record:

```text
id
statement
kind: fact | decision | implementation | interpretation | gap
source paths
scope: week | ticket | deck
confidence: confirmed | partial | unknown
teaching relevance
```

## Outputs

- resource inventory;
- knowledge map;
- evidence map;
- source conflict list;
- missing-ADR and incomplete-source list;
- explicit scope and non-scope list.

Do not generate slides at this stage. The output must be reviewable as a model
of the material, not as a prose summary.
