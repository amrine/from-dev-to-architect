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
  <span class="tp-kicker">Programme Architecte Cloud / DevOps · 28 mois</span>
</div>

<h1 v-motion :initial="{ opacity: 0, y: 32 }" :enter="{ opacity: 1, y: 0, transition: { duration: 700, delay: 150 } }">
  Team<span class="tp-pulse-text">Pulse</span>
</h1>

<PulseLine v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1, transition: { duration: 400, delay: 700 } }" />

<p class="tp-subtitle" v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1, transition: { duration: 600, delay: 900 } }">
  D'une application Spring Boot jusqu'au cloud et au DevOps — un produit, un programme, une preuve.
</p>

<div class="tp-cover-meta" v-motion :initial="{ opacity: 0 }" :enter="{ opacity: 1, transition: { duration: 600, delay: 1100 } }">
  <span class="tp-badge tp-badge--done">W001-T01 · livré</span>
  <span class="tp-badge tp-badge--doc">T02 → T07 · documentés</span>
</div>

</div>

<!--
Deck Slidev TeamPulse.
Generated zones are bounded with AUTO-GENERATED markers and can be refreshed ticket by ticket.
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
