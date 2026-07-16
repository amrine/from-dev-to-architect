---
layout: center
class: tp-section
routeAlias: programme
transition: fade
---

<span class="tp-kicker">Point de départ · développeur Java / Spring / Angular</span>

# Élargir son regard au-delà du code

<PulseLine />

Vous savez déjà construire un contrôleur, un service, un repository ou un composant Angular.

Le parcours apprend à décider **où placer ces éléments, qui peut dépendre de qui, comment les exécuter et comment prouver qu'ils forment un système cohérent**.

<div class="mt-4">
<span class="tp-badge tp-badge--done">Acquis · développement applicatif</span>
<span class="tp-badge tp-badge--doc">Objectif · raisonnement architectural</span>
</div>

<!--
**Message à faire passer**

L'architecture prolonge les compétences de développement ; elle ne les remplace pas.

**Déroulé oral**

Rappelez des objets familiers : contrôleur, service, repository, composant Angular. Puis posez quatre questions : où les placer, qui peut les utiliser, de quoi ont-ils besoin pour s'exécuter et comment prouver qu'ils fonctionnent ensemble ? C'est le passage du code local au système cohérent.

**Insister sur**

Un bon développeur sait écrire un composant. Le raisonnement architectural consiste à maîtriser les relations entre les composants.

**Transition**

Voyons la méthode utilisée dans tout le cours pour apprendre ce raisonnement.
-->

---

## La méthode : problème → décision → preuve

<p class="small muted">Un concept architectural n'est jamais présenté seul. Il arrive lorsqu'une situation du produit le rend nécessaire.</p>

```mermaid {scale: 0.68}
flowchart LR
  S["1 · Situation connue<br/>Java / Spring"] --> P["2 · Problème<br/>ou risque"]
  P --> D["3 · Décision<br/>et compromis"]
  D --> I["4 · Implémentation<br/>dans TeamPulse"]
  I --> V["5 · Preuve<br/>test ou exécution"]
```

<div class="tp-grid-2 mt-3">

<div class="tp-card tp-card--pulse">
<h3>ADR</h3>
<p class="small muted"><strong>Architecture Decision Record</strong> : une courte fiche qui conserve le problème, la décision, les alternatives et les conséquences.</p>
</div>

<div class="tp-card">
<h3>Preuve exécutable</h3>
<p class="small muted">Un build, un test ou un démarrage réussi montre que la décision est réellement respectée par le code.</p>
</div>

</div>

<!--
**Message à faire passer**

Une décision d'architecture doit toujours être reliée à un problème et à une preuve.

**Déroulé oral**

Lisez la chaîne de gauche à droite. La situation vient avant le vocabulaire technique. Le problème permet de comparer plusieurs options. La décision accepte un compromis, l'implémentation matérialise ce choix et la preuve vérifie qu'il est respecté.

**Insister sur**

Un ADR n'est pas une documentation générale : il conserve le contexte d'un choix, ses alternatives et ses conséquences. Une preuve exécutable évite qu'une décision reste une simple intention.

**Transition**

Cette méthode reste la même, mais les problèmes deviennent progressivement plus difficiles.
-->

---

## La difficulté augmente avec le produit

<p class="small muted">De <code>W01</code> à <code>W122</code>, chaque phase part d'un système maîtrisé avant d'ajouter une nouvelle difficulté.</p>

<div class="tp-grid-3 mt-3">

<v-clicks>

<div class="tp-card tp-card--pulse">
<span class="tp-kicker">Phase 1 · W01–W26</span>
<h3>Une application structurée</h3>
<p class="small muted">Une seule application Spring Boot, découpée en modules clairs, testée et déployable.</p>
<span class="tp-badge tp-badge--done">Monolithe modulaire</span>
<p class="small muted">Docker · CI · premières ressources AWS</p>
</div>

<div class="tp-card">
<span class="tp-kicker">Phase 2 · W27–W65</span>
<h3>Des services autonomes</h3>
<p class="small muted">Certains modules deviennent des applications déployables indépendamment lorsque le besoin le justifie.</p>
<span class="tp-badge tp-badge--doc">Microservices</span>
<p class="small muted">Communication · Kubernetes · observabilité</p>
</div>

<div class="tp-card">
<span class="tp-kicker">Phase 3 · W66–W122</span>
<h3>Une plateforme opérée</h3>
<p class="small muted">Les déploiements, la sécurité, la résilience et les coûts deviennent industrialisés.</p>
<span class="tp-badge tp-badge--doc">Plateforme cloud</span>
<p class="small muted">GitOps · reprise · gouvernance · FinOps</p>
</div>

</v-clicks>

</div>

<v-click>

<div class="tp-card tp-card--pulse mt-2 small">
<strong>Principe pédagogique :</strong> on ne commence pas par les microservices. On apprend d'abord à créer des frontières propres dans une application simple à exécuter. Une spécialisation optionnelle suit après <code>W122</code>.
</div>

</v-click>

<!--
**Message à faire passer**

La complexité est introduite seulement lorsque le produit sait déjà absorber l'étape précédente.

**Déroulé oral**

Présentez les trois phases comme une montée en autonomie. La première apprend à structurer une application unique. La deuxième extrait certains services lorsque leur autonomie apporte une valeur réelle. La troisième industrialise l'exploitation d'une plateforme distribuée.

[click] Ne détaillez pas toutes les technologies affichées ; elles servent de repères temporels.

**Insister sur**

Les microservices ne sont ni un point de départ ni une récompense. Ils ajoutent un coût que l'équipe doit être capable de justifier et d'opérer.

**Transition**

Pour rendre cette progression concrète, découvrons maintenant le produit TeamPulse.
-->
