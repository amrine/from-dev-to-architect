---
layout: default
routeAlias: versions
---

<span class="tp-kicker">Annexe technique · état du dépôt au 15 juillet 2026</span>

## Politique de versions

<p class="small muted">Les manifests du dépôt sont la source de vérité. Cette slide reflète les versions actuellement déclarées.</p>

| Composant         | Version du dépôt       | Source                         |
| ----------------- | ----------------------- | ------------------------------ |
| Java              | `25`                    | propriété du `pom.xml` parent  |
| Spring Boot       | `4.1.0`                 | parent Maven                   |
| Spring Modulith   | `2.0.6`                 | BOM Maven                      |
| Flyway PostgreSQL | `12.8.1`                | dependency management          |
| PostgreSQL local  | `postgres:18-bookworm`  | `docker/docker-compose.yaml`   |

<div class="tp-card tp-card--pulse mt-3 small">
<strong>Règle de maintenance :</strong> déclarer les versions dans les manifests · vérifier compatibilité, support et notes de version · prouver la montée par le build, les tests d'intégration et un ADR si l'impact est significatif.
</div>

<div class="tp-footref">
Sources : pom.xml · docker/docker-compose.yaml
</div>

<!--
**Message à faire passer**

Cette table est une photographie du dépôt, pas une recommandation éternelle ni une liste à mémoriser.

**Déroulé oral**

Montrez où retrouver la vérité : parent Maven, dependency management et Docker Compose. Expliquez qu'une version peut changer après la création du cours ; le support doit donc être régénéré ou vérifié avant une session.

**Insister sur**

Une montée de version professionnelle vérifie la compatibilité et le support, puis apporte une preuve par le build et les tests. Si elle modifie une décision structurante ou son compromis, l'ADR doit également évoluer.

**Transition**

Utilisez cette annexe pour répondre aux questions de reproduction, puis revenez aux concepts plutôt qu'aux numéros de version.
-->
