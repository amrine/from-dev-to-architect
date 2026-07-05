---
layout: center
class: tp-section
routeAlias: produit
transition: fade
---

<span class="tp-kicker">Le fil rouge</span>

# TeamPulse, le produit

<PulseLine />

Application **B2B interne** : mesurer chaque semaine l'humeur, la charge et les risques d'une équipe.

---

## Personas & fonctionnalités

<div class="tp-grid-2">

<div>

**Quatre personas**

<v-clicks>

- **Admin** — organisations, équipes, utilisateurs, permissions
- **Manager** — pulse de son équipe, tendances, alertes
- **Member** — répond au pulse hebdomadaire
- **Auditor** — logs d'audit et exports

</v-clicks>

</div>

<div>

**Le pulse hebdomadaire**

<v-clicks>

- `mood` 1–5 · `workload` 1–5
- `blockers` texte court · `risk flag` oui/non
- **Une réponse** par user / team / semaine ISO
- Agrégats manager : moyennes, évolution, heatmap, **alertes** si le mood baisse ou la charge monte

</v-clicks>

</div>

</div>

<v-click>

<div class="tp-card mt-3">
<strong>Socle transverse</strong> — auth JWT + RBAC (<code>ADMIN</code> / <code>MANAGER</code> / <code>MEMBER</code>), audit des actions sensibles, puis multi-tenancy, anonymisation, webhooks HMAC, event-driven.
</div>

</v-click>

---

## Pourquoi ce produit porte l'apprentissage

<div class="tp-grid-2 mt-2">

<v-clicks>

<div class="tp-card">
<h3>Assez simple pour démarrer</h3>
<p class="small muted">CRUD users/teams, un formulaire de pulse, un dashboard : livrable en monolithe modulaire dès la Phase 1.</p>
</div>

<div class="tp-card">
<h3>Assez riche pour tout exercer</h3>
<p class="small muted">Événements métier (réponses, alertes) → Kafka, outbox, saga. Données sensibles → sécurité, audit, privacy. Multi-équipes → multi-tenancy.</p>
</div>

<div class="tp-card">
<h3>Réaliste pour un architecte</h3>
<p class="small muted">Arbitrages coût / sécurité / performance / time-to-market documentés en ADR à chaque étape.</p>
</div>

<div class="tp-card">
<h3>Un vrai portfolio</h3>
<p class="small muted">À la fin : code, IaC, pipelines, dashboards, runbooks, postmortems — la preuve, pas la promesse.</p>
</div>

</v-clicks>

</div>
