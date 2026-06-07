# W001-T01 - Backend multi-module

## Objectif
Demarrer le backend TeamPulse avec une architecture modulaire claire, verifiable et evolutive.

## Besoin
Le backend doit etre organise en plusieurs modules correspondant aux responsabilites principales:
- un module bootstrap applicatif;
- un module partage;
- des modules metier par domaine.

L'application doit rester un monolithe deploye en une seule unite, tout en gardant des frontieres metier strictes.

## Modules attendus
- `tp-app`: point d'entree Spring Boot et assemblage global.
- `tp-common`: types transverses sans logique metier.
- `tp-organization`: domaine organization.
- `tp-identity`: domaine identity.
- `tp-team`: domaine team.

## Contraintes
- Un seul module doit demarrer le serveur Spring Boot.
- Les modules metier ne doivent pas dependre de `tp-app`.
- `tp-common` ne doit dependre d'aucun module metier.
- Les modules internes ne doivent pas communiquer en HTTP.
- Les appels synchrones inter-modules doivent passer par une API Java publique.
- Les notifications de faits metier doivent passer par des evenements.

## Livrables attendus
- Projet Maven multi-module.
- Module `tp-app` executable.
- Modules metier sous forme de JAR bibliotheques.
- Frontieres Spring Modulith declarees.
- Tests de verification d'architecture.

## Criteres d'acceptation
- `mvn clean verify` passe pour le socle d'architecture.
- Le JAR executable est produit par `tp-app`.
- Les modules detectes sont `common`, `organization`, `identity` et `team`.
- Les frontieres Modulith sont verifiees par test.

## Decision associee
Voir [ADR-W001-T01](../../adr/W001/ADR-W001-T01-backend-multi-module.md).
