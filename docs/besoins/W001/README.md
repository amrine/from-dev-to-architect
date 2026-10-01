# W001 - Socle backend TeamPulse

## Objectif
Mettre en place le socle backend de TeamPulse sous forme de monolithe modulaire Spring Boot.

Ce dossier decrit l'expression de besoin de W001. Les ADR associees documentent les decisions techniques retenues, les alternatives et les justifications.

## Perimetre
- Initialiser l'architecture backend multi-module.
- Mettre en place les frontieres metier entre modules.
- Preparer la persistance PostgreSQL et les migrations Flyway.
- Exposer les API metier de l'identite, des organisations et des equipes, ainsi
  que l'administration de la plateforme.
- Garantir les responsabilites metier T04 sans introduire l'authentification,
  les roles ou les permissions avant W008.
- Preparer le packaging Docker et l'environnement local.
- Definir les profils de configuration locaux.

## Hors perimetre
- Authentification complete.
- Resolution dynamique du tenant depuis JWT.
- Generation de client frontend.
- Notifications et livraison email hors du perimetre de T05.
- Deploiement cloud.
- Extraction en micro-services.

## Tickets et decisions associees
| Ticket | Besoin | ADR |
| --- | --- | --- |
| W001-T01 | [Backend multi-module](./W001-T01-backend-multi-module.md) | [ADR-W001-T01](../../adr/W001/ADR-W001-T01-backend-multi-module.md) |
| W001-T02 | [PostgreSQL local et schemas Flyway](W001-T02-PostgreSQL-local-et-schemas-Flyway.md) | [ADR-W001-T02](../../adr/W001/ADR-W001-T02-docker-compose-local.md) |
| W001-T03 | [Regles d'architecture executables avec ArchUnit](./W001-T03-regles-architecture-archunit.md) | [ADR-W001-T03](../../adr/W001/ADR-W001-T03-regles-architecture-archunit.md) |
| W001-T04 | [Multi-tenancy par reference d'organisation](./W001-T04-multi-tenancy-organization-reference.md) | [ADR-W001-T04](../../adr/W001/ADR-W001-T04-multi-tenancy-organization-reference.md) |
| W001-T05 | [APIs métier et administration de plateforme](./W001-T05-api-metiers-et-administration-plateforme.md) | [ADR-W001-T05](../../adr/W001/ADR-W001-T05-api-metiers-et-administration-plateforme.md) |

## Ordre d'implementation
```text
W001-T01 Backend multi-module
W001-T02 PostgreSQL local et schemas Flyway
W001-T03 Regles d'architecture executables avec ArchUnit
W001-T04 Multi-tenancy par reference d'organisation
W001-T05 APIs metier et administration de plateforme
W001-T06 Dockerfile multi-stage
W001-T07 Profils local et localstack
```

`W001-T02` fournit PostgreSQL local et initialise les schemas Flyway par module.
`W001-T03` rend executables les regles d'architecture internes avant l'ajout des
premieres verticales metier.
`W001-T04` s'appuie sur ces schemas et migrations pour definir la racine du
tenant, sa reference fonctionnelle et les verticales internes de domaine,
d'application et de persistence portant `organizationReference`, notamment
`Team` et `TeamMember`.
`W001-T05` expose les capacites des trois modules metier, ajoute
`tp-administration` pour les parcours de plateforme, et controle les
responsabilites avant l'indisponibilite d'un utilisateur. Un contexte d'acteur
distinct est reserve a l'audit et a l'orchestration, mais l'authentification,
les roles et les permissions sont reportes a W008. Le bootstrap et le profil
local creent une organisation persistée `CREATING` ; aucune activation ou
réactivation n'est exposée en T05. L'affectation finale des responsables et la
transition vers `ACTIVE` attendent W008. Les contrats OpenAPI et leurs clients de test sont
propres a chaque proprietaire HTTP ; les notifications/email sont hors du
perimetre T05 et font l'objet d'un besoin de travail distinct.

Le scenario fonctionnel de reference est decrit dans
[`SCENARIO-METIER-TEAMPULSE.md`](../SCENARIO-METIER-TEAMPULSE.md).

## Regle documentaire
- Le besoin decrit le probleme a resoudre, les attentes et les criteres d'acceptation.
- L'ADR decrit la decision retenue, les alternatives, les justifications et les consequences.
- Un ticket W001 ne doit pas etre considere termine si son besoin et son ADR ne sont pas coherents avec l'implementation.
