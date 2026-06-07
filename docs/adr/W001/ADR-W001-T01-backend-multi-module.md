# ADR-W001-T01 - Backend multi-module

## Statut
Draft

## Ticket lie
W001-T01 - Backend multi-module

## Contexte
Nous devons demarrer un monolithe modulaire TeamPulse avec plusieurs domaines (organization, identity, team) et un module partage. Le bootstrap doit etre centralise dans un module unique.

## Decision
- Utiliser un projet Maven multi-module.
- Modules: tp-common, tp-organization, tp-identity, tp-team, tp-app.
- tp-app porte @SpringBootApplication et le wiring global.
- Parent pom: Spring Boot 4.0.x, Java 21.
- Starters: web, data-jpa, validation, actuator.
- Dependances DB: flyway-core, flyway-database-postgresql, postgresql.

## Alternatives envisagees
- Monolithe en un seul module.
- Micro-services separes par domaine.
- Build Gradle.

## Justification
Un monolithe modulaire garde des frontieres claires sans la complexite d'un systeme distribue.

## Consequences positives
- Separation claire des domaines.
- Demarrage simple via un seul module bootstrap.

## Consequences negatives / compromis
- Plus de configuration Maven.
- Gestion des dependances inter-modules a maintenir.

## Impact technique
- pom.xml parent et poms des modules.
- Modules tp-common, tp-organization, tp-identity, tp-team, tp-app.

## Validation
- mvn -pl tp-app -am package produit le jar.
- L'application demarre avec le module tp-app.

