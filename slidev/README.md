# TeamPulse — Deck Slidev

## Structure

```
slides.md              # headmatter global + cover animée + includes
pages/index.md         # sommaire navigable (routeAlias)
pages/weeks/W001.md    # semaine 001 (zones AUTO-GENERATED préservées)
components/PulseLine.vue   # signature visuelle : ligne ECG animée
styles/index.css       # design system (couleurs, typo, badges, cartes, tableaux)
setup/mermaid.ts       # thème Mermaid aligné sur l'identité du deck
```

## Lancer

```bash
yarn install
yarn dev          # présentation live
yarn export:pdf   # export PDF avec chaque étape d'animation
```

## Identité visuelle

- Fond encre navy `#0a0f1c` (ambiance "monitoring"), accent pouls teal `#2dd4bf`,
  bleu `#60a5fa` en secondaire, ambre `#fbbf24` réservé aux points de vigilance.
- Typographies : Space Grotesk (titres), Inter (corps), JetBrains Mono (code, labels tickets).
- Signature : le composant `<PulseLine />` (tracé ECG animé) sous les titres de section.

## Conventions

- Les zones `<!-- AUTO-GENERATED:...:START/END -->` sont préservées : le pipeline
  de régénération ticket par ticket reste fonctionnel.
- Navigation interne via `routeAlias` (`agenda`, `w001`, `w001-t01`) et `<Link to="...">`.
- Badges de statut : `.tp-badge--done` (livré), `.tp-badge--doc` (documenté),
  `.tp-badge--warn` (point de vigilance).

## Structure programme (v2)

```
pages/program/vision.md     # 01 · programme 28 mois, principes, 3 phases + année 3
pages/program/teampulse.md  # 02 · produit : personas, pulse, pourquoi ce fil rouge
pages/program/evolution.md  # 03 · monolithe → microservices → plateforme, LocalStack vs AWS, artefacts
pages/weeks/W001.md         # 04 · journal de bord semaine 001 (zones AUTO-GENERATED)
pages/weeks/_TEMPLATE.md    # gabarit pour générer W002, W003, ...
```

Pour ajouter une semaine : copier `_TEMPLATE.md` → `Wxxx.md`, remplacer les placeholders,
ajouter l'include dans `slides.md` et une carte dans `pages/index.md`.

## Pattern narratif d'une semaine (v4)

1. **Situation initiale** — une scène concrète et une question, sans annoncer la solution.
2. **Risques observables** — ce qui peut mal tourner dans le produit, le code ou l'environnement.
3. **Besoin fonctionnel** — la capacité attendue, formulée sans technologie.
4. **Garanties techniques** — qualités mesurables avant le choix des outils.
5. **Contraintes et non-objectifs** — limites explicites pour éviter le sur-engineering.
6. **Révélation Wxxx** — objectif de la semaine après compréhension du problème.
7. **Contrat pédagogique et preuves** — verbes observables, DoD et validations attendues.
8. **Découpage et ordre** — tickets, ADR, dépendances et incertitude réduite par chaque étape.
9. **Architecture cible** — état de fin de semaine, distinct de l'état déjà livré.
10. **Deep dive Txx par Txx** — besoin et décision, implémentation, validation et bilan.
11. **Incohérences ouvertes** — uniquement dans `pages/coherence.md`, zone
    `AUTO-GENERATED:Wxxx-COHERENCE`. Un écart résolu disparaît à la régénération suivante.
