---
layout: default
routeAlias: evolution
---

## L'architecture, concrètement

<div class="tp-card tp-card--pulse mt-2">
L'<strong>architecture logicielle</strong> est l'ensemble des décisions qui organisent les responsabilités, les dépendances, les données et l'exécution d'un système. Elle est visible dans le code et l'exploitation, pas seulement dans un diagramme.
</div>

<div class="tp-grid-2 mt-3">

<v-clicks>

<div class="tp-card">
<h3>Responsabilités</h3>
<p class="small muted">Un <strong>module</strong> regroupe le code qui répond à une même responsabilité métier.</p>
</div>

<div class="tp-card">
<h3>Dépendances</h3>
<p class="small muted">Une frontière indique ce qu'un module peut utiliser chez un autre et ce qui doit rester interne.</p>
</div>

<div class="tp-card">
<h3>Données</h3>
<p class="small muted">On décide qui possède une donnée, comment elle évolue et quelles règles protègent sa cohérence.</p>
</div>

<div class="tp-card">
<h3>Exécution</h3>
<p class="small muted">On définit comment l'application démarre, est testée, déployée, observée et réparée.</p>
</div>

</v-clicks>

</div>

<!--
**Message à faire passer**

L'architecture est observable dans quatre dimensions : responsabilités, dépendances, données et exécution.

**Déroulé oral**

Reliez chaque dimension à une application Spring connue. Les packages et services portent des responsabilités ; les imports et appels créent des dépendances ; les repositories manipulent des données ; les profils, builds et déploiements définissent l'exécution.

[click] Révélez les dimensions une à une et demandez au public un exemple dans un projet qu'il connaît.

**Insister sur**

Un diagramme peut représenter l'architecture, mais ce sont le code, la configuration et les procédures qui la font réellement respecter.

**Transition**

La dimension exécution oblige à préciser ce que nous appelons un environnement.
-->

---

## Un environnement, c'est quoi ?

<p><strong>Le même code</strong> + une <strong>configuration</strong> + des <strong>services externes</strong> comme PostgreSQL ou AWS.</p>

| Environnement                                                | Image simple                                 | Utilité pédagogique                                  |
| ------------------------------------------------------------ | -------------------------------------------- | ---------------------------------------------------- |
| <span class="tp-badge tp-badge--done">Local / Compose</span> | Application sur le poste, PostgreSQL en Docker | Développer vite avec le même service pour tous       |
| <span class="tp-badge tp-badge--doc">LocalStack</span>       | Imitation locale de quelques API AWS         | Apprendre un contrat cloud sans facture permanente   |
| <span class="tp-badge tp-badge--warn">AWS réel</span>        | Réseau, droits et services réellement hébergés | Apprendre les comportements, limites et coûts réels  |

<v-click>

<div class="tp-card tp-card--pulse mt-3 small">
On choisit l'environnement le moins coûteux qui conserve le comportement à apprendre. <strong>W001 commence en local</strong> avec PostgreSQL et Docker Compose.
</div>

</v-click>

<!--
**Message à faire passer**

Un environnement combine le même code avec une configuration et des services externes différents.

**Déroulé oral**

Comparez les trois lignes sans les présenter comme équivalentes. Docker Compose fournit rapidement PostgreSQL sur le poste. LocalStack imite certains contrats AWS, mais pas toute la réalité du cloud. AWS réel ajoute réseau, droits, limites et coûts.

**Insister sur**

On choisit l'environnement le moins coûteux qui permet d'apprendre ou de prouver le comportement recherché. Pour W001, PostgreSQL local suffit.

**Transition**

Une fois un environnement ou une architecture choisi, l'équipe doit conserver la raison de ce choix.
-->

---

## Une décision doit laisser une trace

| Question de l'équipe | Trace produite | Définition simple |
| -------------------- | -------------- | ----------------- |
| Pourquoi ce choix ? | **ADR** | Contexte, décision, alternatives et conséquences |
| À quoi ressemble le système ? | **Diagramme** | Responsabilités et flux importants |
| Comment démarrer ou réparer ? | **Runbook** | Procédure exécutable étape par étape |
| Comment savoir que cela fonctionne ? | **Test** | Vérification automatique d'un comportement attendu |

<v-click>

<div class="tp-card tp-card--pulse mt-4">
W001 appliquera exactement ce cycle : <strong>situation → problème → décision → code → preuve</strong>. La première question sera simple : comment trois développeurs peuvent-ils construire ensemble sans créer trois systèmes incompatibles ?
</div>

</v-click>

<!--
**Message à faire passer**

Chaque trace répond à une question différente et évite de dépendre de la mémoire de l'équipe.

**Déroulé oral**

L'ADR explique pourquoi un choix a été fait. Le diagramme montre les responsabilités et flux. Le runbook permet de démarrer ou réparer. Le test vérifie automatiquement un comportement. Aucun de ces documents ne remplace les autres.

**Insister sur**

Une documentation professionnelle relie la décision au code et à une preuve. Elle évolue avec le système au lieu de devenir une archive décorative.

**Transition**

Nous pouvons maintenant entrer dans W001 avec un scénario concret : trois développeurs doivent intégrer leur travail vendredi.
-->
