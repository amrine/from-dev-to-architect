<!--
  TEMPLATE de semaine — copier vers pages/weeks/Wxxx.md
  Pattern narratif : Problématique → Découpage → Pourquoi ce découpage → deep dive Txx par Txx.
  Les incohérences NE vont PAS ici : elles vont dans pages/coherence.md
  (zone AUTO-GENERATED:Wxxx-COHERENCE), dernier slide du deck.
-->

---

layout: center
class: tp-section
routeAlias: wxxx
transition: fade
---

<!-- AUTO-GENERATED:Wxxx:START -->

<span class="tp-kicker">Semaine xxx · Phase {1|2|3}</span>

# {Titre de la semaine}

<PulseLine />

{Accroche : la problématique en une phrase.}

<div class="mt-4">
<span class="tp-badge tp-badge--done">{n} livrés</span>
<span class="tp-badge tp-badge--doc">{n} documentés</span>
</div>

---

## Wxxx · La problématique

<div class="tp-grid-2">

<div>

**Besoin fonctionnel**

<v-clicks>

- {Ce que le produit doit permettre / à qui ça sert}
- {Contrainte métier structurante}

</v-clicks>

</div>

<div>

**Besoin technique**

<v-clicks>

- {Ce que le système doit garantir}
- {Contrainte technique / préparation des semaines suivantes}

</v-clicks>

</div>

</div>

<v-click>

<div class="tp-card tp-card--pulse mt-3">
En une phrase : {l'objectif synthétique de la semaine}.
</div>

</v-click>

---

## Wxxx · Le découpage en tickets

| Ticket | Sujet   | ADR                                           | État                                                  |
| ------ | ------- | --------------------------------------------- | ----------------------------------------------------- |
| `T01`  | {sujet} | <span class="small muted">ADR-Wxxx-T01</span> | <span class="tp-badge tp-badge--done">terminé</span>  |
| `T02`  | {sujet} | <span class="small muted">ADR-Wxxx-T02</span> | <span class="tp-badge tp-badge--doc">documenté</span> |

---

## Wxxx · Pourquoi ce découpage ?

Chaque ticket isole **une décision d'architecture** et se livre indépendamment :

<v-clicks>

- `T01` — {pourquoi ce ticket vient en premier}
- `T02` — {ce qu'il débloque / pourquoi cet ordre}

</v-clicks>

<!-- AUTO-GENERATED:Wxxx:END -->

---

layout: center
class: tp-section
transition: fade
routeAlias: wxxx-t01
---

<!-- AUTO-GENERATED:Wxxx-T01:START -->

<span class="tp-kicker">Wxxx · Découpage 1/{n}</span>

# T01 — {Sujet}

<PulseLine />

{Objectif du ticket en une phrase.}

---

## T01 · Besoin & décision

<!-- deux colonnes : livrables attendus / décisions ADR -->

---

## T01 · Implémentation

<!-- code observé dans le commit, séquence technique -->

---

## T01 · Validation & bilan

<!-- preuve (tests, commandes) + ce qui est acquis -->

<!-- AUTO-GENERATED:Wxxx-T01:END -->
