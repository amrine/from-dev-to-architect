---
layout: default
routeAlias: agenda
---

<span class="tp-kicker">Sommaire</span>

# Un produit, un programme, une preuve

<PulseLine :width="260" />

<div class="tp-grid-2 mt-5" style="grid-template-columns: 1fr 1fr; gap: 0.8rem;">

<Link to="programme">
<div class="tp-card tp-card--pulse">
<h3>01 · Le programme</h3>
<p class="small muted">28 mois, 3 phases + année de spécialisation : de développeur Java/Spring à architecte Cloud / DevOps.</p>
</div>
</Link>

<Link to="produit">
<div class="tp-card tp-card--pulse">
<h3>02 · Le produit TeamPulse</h3>
<p class="small muted">Pulse hebdomadaire d'équipe : personas, features, pourquoi il porte tout l'apprentissage.</p>
</div>
</Link>

<Link to="evolution">
<div class="tp-card tp-card--pulse">
<h3>03 · L'architecture qui évolue</h3>
<p class="small muted">Monolithe modulaire → microservices EKS → plateforme enterprise. Stratégie LocalStack vs AWS.</p>
</div>
</Link>

<Link to="w001">
<div class="tp-card tp-card--pulse">
<h3>04 · Journal de bord — W001</h3>
<p class="small muted">Socle backend : Maven multi-module + Spring Modulith.</p>
<span class="tp-badge tp-badge--done">T01 livré</span>
<span class="tp-badge tp-badge--doc">T02 → T07 documentés</span>
</div>
</Link>

</div>

<div class="tp-footref">
Sources : docs/besoins · docs/adr · branche Git courante · roadmap Excel v9
</div>
