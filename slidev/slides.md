---
theme: default
title: TeamPulse Roadmap
info: |
  TeamPulse roadmap generated from besoins, ADRs, committed code and roadmap sources.
author: Moussab Amine AMRINE
highlighter: shiki
lineNumbers: false
colorSchema: dark
drawings:
  persist: false
transition: slide-left
mdc: true
fonts:
  sans: Inter
  serif: Space Grotesk
  mono: JetBrains Mono
layout: cover
class: tp-cover
---

<div class="tp-cover-inner">

<div v-motion :initial="{ opacity: 0, y: 24 }" :enter="{ opacity: 1, y: 0, transition: { duration: 600 } }">
  <span class="tp-kicker">Parcours guidé · 28 mois · Java / Spring / Angular</span>
</div>

<h1 v-motion :initial="{ opacity: 0, y: 32 }" :enter="{ opacity: 1, y: 0, transition: { duration: 700, delay: 150 } }">
  Team<span class="tp-pulse-text">Pulse</span>
</h1>

<PulseLine v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1, transition: { duration: 400, delay: 700 } }" />

<p class="tp-subtitle" v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1, transition: { duration: 600, delay: 900 } }">
  Apprendre à organiser, faire évoluer et exploiter une application, du premier module jusqu'au cloud.
</p>

<div class="tp-cover-meta" v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1, transition: { duration: 600, delay: 1100 } }">
  <span class="tp-badge tp-badge--done">Point de départ · développeur applicatif</span>
  <span class="tp-badge tp-badge--doc">Méthode · un produit fil rouge</span>
  <span class="tp-badge tp-badge--done">W001 · T01 + T02 livrés</span>
</div>

</div>

<!--
**Message à faire passer**

Ce parcours ne cherche pas à empiler des technologies. Il montre comment un développeur applicatif apprend progressivement à faire tenir un système dans la durée.

**Déroulé oral**

Partez de l'acquis du public : Java, Spring et Angular. Expliquez que TeamPulse servira de produit fil rouge pendant toute la roadmap. Les choix d'architecture apparaîtront uniquement lorsqu'un problème concret les rendra nécessaires.

**Insister sur**

Les 28 mois représentent une trajectoire pédagogique, pas un big bang technique à réaliser dès le départ.

**Transition**

Présentons d'abord les quatre étapes qui vont conduire du code fonctionnel vers un système cohérent.
-->

---
src: ./pages/index.md
---

---
src: ./pages/program/vision.md
---

---
src: ./pages/program/teampulse.md
---

---
src: ./pages/program/evolution.md
---

---
src: ./pages/weeks/W001.md
---

---
src: ./pages/coherence.md
---

---
src: ./pages/annexes/versions.md
---
