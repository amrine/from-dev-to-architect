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

---

## Écarts constatés & arbitrages

<!-- AUTO-GENERATED:W001-COHERENCE:START -->

<div class="tp-grid-2 mt-2">

<v-clicks>

<div class="tp-card tp-card--warn">
<h3>W001 · Besoins T02 → T07 à rédiger</h3>
<p class="small muted">Les besoins existent mais n'ont pas encore été travaillés — compléter <code>docs/besoins/W001/README.md</code>.</p>
<span class="tp-badge tp-badge--warn">action · ce weekend</span>
</div>

<div class="tp-card tp-card--warn">
<h3>W001 · Aligner la roadmap sur la politique de versions</h3>
<p class="small muted">Roadmap Excel : Spring Boot <code>3.5+</code> (EOL) · code : <code>4.0.6</code>. Cible : dernière génération stable — <code>4.1.x</code>. Mettre à jour l'Excel et le parent Maven.</p>
<span class="tp-badge tp-badge--warn">action · roadmap Excel + pom</span>
</div>

<div class="tp-card tp-card--pulse">
<h3>W001 · Périmètre ADR T01 — décision actée</h3>
<p class="small muted"><code>T01</code> = squelette de l'appli + choix techniques uniquement. Mise en place et initialisation de la BDD → <code>T02</code> / <code>T03</code>. Déplacer le détail BDD de l'ADR T01 vers T03.</p>
<span class="tp-badge tp-badge--done">décidé · reste à éditer l'ADR</span>
</div>

<div class="tp-card tp-card--pulse">
<h3>Granularité Excel vs tickets — convention assumée</h3>
<p class="small muted">La roadmap Excel décrit la <strong>fonctionnalité macro</strong> par semaine ; le repo la découpe en <strong>tickets logiques</strong> <code>Txx</code>. Ce n'est plus un écart : documenté comme convention du projet.</p>
<span class="tp-badge tp-badge--done">résolu · convention</span>
</div>

</v-clicks>

</div>

<!-- AUTO-GENERATED:W001-COHERENCE:END -->

<div class="tp-footref">
Chaque écart corrigé disparaît à la prochaine régénération du deck
</div>
