---
layout: center
class: tp-section
routeAlias: produit
transition: fade
---

<span class="tp-kicker">Le produit fil rouge</span>

# TeamPulse, une boucle métier concrète

<PulseLine />

Chaque semaine, un membre partage son humeur, sa charge et ses blocages.

Le manager observe une tendance d'équipe et peut agir avant qu'un risque devienne critique.

<div class="mt-4">
<span class="tp-badge tp-badge--done">Application B2B interne</span>
<span class="tp-badge tp-badge--doc">4 rôles</span>
<span class="tp-badge tp-badge--doc">1 cycle par semaine</span>
</div>

<!--
**Message à faire passer**

TeamPulse est un produit métier suffisamment simple pour être compris rapidement, mais assez riche pour faire émerger de vrais problèmes d'architecture.

**Déroulé oral**

Décrivez la boucle hebdomadaire : un membre partage des signaux, l'équipe obtient une tendance et le manager peut agir. Le produit ne vaut pas par la collecte seule, mais par la capacité à détecter un risque assez tôt pour intervenir.

**Insister sur**

Le fil rouge reste le besoin métier. La technique sera toujours expliquée comme un moyen de préserver cette boucle.

**Transition**

Suivons une réponse depuis le membre jusqu'à l'action du manager.
-->

---

## Le scénario métier en quatre étapes

```mermaid {scale: 0.68}
flowchart LR
  M["1 · Member<br/>répond au pulse"] --> R["2 · TeamPulse<br/>enregistre la réponse"]
  R --> A["3 · Équipe<br/>les réponses sont agrégées"]
  A --> G["4 · Manager<br/>observe et agit"]
```

<div class="tp-grid-2 mt-3">

<div>

**Les quatre signaux**

<v-clicks>

- Humeur : comment je me sens
- Charge : quel effort je supporte
- Blocage : ce qui empêche d'avancer
- Risque : ce qui demande une attention rapide

</v-clicks>

</div>

<div>

**Les quatre rôles**

<v-clicks>

- **Member** répond
- **Manager** suit les tendances
- **Admin** organise équipes et accès
- **Auditor** contrôle les actions sensibles

</v-clicks>

</div>

</div>

<v-click>

<div class="tp-card tp-card--pulse mt-2 small">
Règle métier structurante : une seule réponse par personne, équipe et semaine. Cette règle simple traversera plus tard l'API, la base et les tests.
</div>

</v-click>

<!--
**Message à faire passer**

Une règle métier apparemment simple traverse plusieurs responsabilités du système.

**Déroulé oral**

Racontez un cas concret : le vendredi matin, un membre répond une seule fois pour son équipe et sa semaine. TeamPulse enregistre ses quatre signaux, agrège les réponses sans exposer inutilement les individus, puis le manager observe la tendance.

[click] Faites apparaître les signaux, puis les rôles. Montrez que chacun agit différemment sur la même boucle.

**Insister sur**

La contrainte « une réponse par personne, équipe et semaine » deviendra plus tard une règle d'API, de base de données et de test.

**Transition**

Cette boucle métier soulève maintenant quatre familles de questions techniques.
-->

---

## Pourquoi TeamPulse oblige à penser architecture

<div class="tp-grid-2 mt-2">

<v-clicks>

<div class="tp-card">
<h3>Où placer le code ?</h3>
<p class="small muted">Organisation, identité, équipe et pulse ont des responsabilités différentes qui doivent rester compréhensibles.</p>
</div>

<div class="tp-card">
<h3>Comment faire évoluer la donnée ?</h3>
<p class="small muted">Les tables changent avec le produit sans casser les autres développeurs ni les environnements existants.</p>
</div>

<div class="tp-card">
<h3>Comment isoler les organisations ?</h3>
<p class="small muted">Les données d'une entreprise ne doivent jamais être visibles par une autre entreprise.</p>
</div>

<div class="tp-card">
<h3>Comment savoir que le système tient ?</h3>
<p class="small muted">Tests, logs et procédures doivent détecter les erreurs avant les utilisateurs.</p>
</div>

</v-clicks>

</div>

<div class="tp-card tp-card--pulse mt-3 small">
Ces questions apparaissent progressivement. Le cours introduit une solution seulement lorsque TeamPulse rencontre le problème correspondant.
</div>

<!--
**Message à faire passer**

L'architecture apparaît quand plusieurs exigences du produit doivent rester vraies en même temps.

**Déroulé oral**

Parcourez les quatre questions sans encore donner de solution : organiser le code, faire évoluer la donnée, isoler les organisations et détecter les erreurs. Expliquez qu'elles correspondent aux responsabilités, aux données, à la sécurité et à l'exploitation.

[click] À chaque carte, demandez ce qui pourrait arriver si l'équipe ignore la question pendant plusieurs semaines.

**Insister sur**

Nous n'allons pas apprendre des outils hors contexte. Chaque solution arrivera au moment où TeamPulse rencontre le problème associé.

**Transition**

Avant W001, donnons une définition simple aux mots architecture, environnement et décision.
-->
