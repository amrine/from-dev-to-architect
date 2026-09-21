---
layout: center
class: tp-section
routeAlias: coherence
transition: fade
---

<span class="tp-kicker">Dernier slide · Backlog qualité</span>

# Écarts à traiter

<PulseLine />

Écarts détectés entre besoins, ADRs, code commité et roadmap — chaque écart a un arbitrage et une action.

<!--
**Message à faire passer**

Un écart documenté n'est pas un échec caché : c'est un élément de qualité rendu visible, attribué et traitable.

**Déroulé oral**

Expliquez que le cours compare régulièrement quatre sources : besoins, ADR, code et roadmap. Lorsqu'elles divergent, le deck ne doit pas inventer une cohérence. Il expose l'écart, indique son impact et lui associe une action.

**Insister sur**

La transparence protège la confiance dans le support de cours. Une slide qui affirme plus que le dépôt réel enseigne une mauvaise pratique documentaire.

**Transition**

Voici l'écart actuellement identifié et l'arbitrage proposé.
-->

---

## Écarts constatés & arbitrages

<!-- AUTO-GENERATED:W001-COHERENCE:START -->

<div class="tp-grid-2 mt-2">

<v-clicks>

<div class="tp-card tp-card--pulse tp-card--compact">
<h3>W001 · T01 → T03 : socle prouvé</h3>
<p class="small muted">Besoins, ADR et code présents : multi-module, PostgreSQL/Flyway, ArchUnit/Modulith.</p>
<span class="tp-badge tp-badge--done">code + ADR acceptés</span>
</div>

<div class="tp-card tp-card--warn tp-card--compact">
<h3>W001 · T04 : décision implémentée, clôture à aligner</h3>
<p class="small muted">ADR Accepted + code livré : tenant, références, directories, isolation. Besoin Draft ; roadmap W001 « À faire ».</p>
<span class="tp-badge tp-badge--doc">action · réaligner les statuts</span>
</div>

<div class="tp-card tp-card--warn tp-card--compact">
<h3>W001 · T05 → T07 : références sans sources canoniques</h3>
<p class="small muted">README et roadmap les mentionnent ; aucun besoin/ADR canonique n'est présent. HTTP, Dockerfile et profils ne sont pas livrés.</p>
<span class="tp-badge tp-badge--warn">action · documenter avant génération</span>
</div>

<div class="tp-card tp-card--compact">
<h3>W001 · T08 → T14 : intention de trajectoire</h3>
<p class="small muted">Intention de roadmap non canonique : besoins, ADR acceptés et implémentations restent à établir.</p>
<span class="tp-badge tp-badge--warn">statut · non canonique</span>
</div>

</v-clicks>

</div>

<!-- AUTO-GENERATED:W001-COHERENCE:END -->

<div class="tp-footref">
Chaque écart corrigé disparaît à la prochaine régénération du deck
</div>

<!--
**Message à faire passer**

Le support distingue strictement le socle W001 prouvé, la clôture documentaire à aligner et la trajectoire seulement planifiée.

**Déroulé oral**

Présentez cet élément comme un backlog de qualité documentaire. T01 à T03 disposent de besoins, ADR acceptés et preuves de code. T04 a son ADR accepté et son vertical livré, mais son besoin reste Draft et l'agrégat W001 de la roadmap doit être réaligné. T05 à T07 puis T08 à T14 ne sont pas des contenus canoniques de ce parcours.

Soulignez que chaque carte disparaît quand la source concernée est réellement alignée, pas lorsqu'une intention est simplement formulée.

**Insister sur**

Ne transformez jamais un ticket planifié en contenu affirmatif tant que besoin, ADR et preuve de livraison ne sont pas établis.

**Transition**

Terminons par l'état des versions réellement déclaré dans le dépôt.
-->
