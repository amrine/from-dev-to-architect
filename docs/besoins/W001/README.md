# W001 - Socle backend TeamPulse

## Objectif
Mettre en place le socle backend de TeamPulse sous forme de monolithe modulaire Spring Boot.

Ce dossier decrit l'expression de besoin de W001. Les ADR associees documentent les decisions techniques retenues, les alternatives et les justifications.

## Perimetre
- Initialiser l'architecture backend multi-module.
- Mettre en place les frontieres metier entre modules.
- Preparer la persistance PostgreSQL et les migrations Flyway.
- Exposer une premiere API minimale.
- Definir les roles contextualises et les appartenances temporelles.
- Preparer le packaging Docker et l'environnement local.
- Definir les profils de configuration locaux.

## Hors perimetre
- Authentification complete.
- Resolution dynamique du tenant depuis JWT.
- OpenAPI et generation de client front.
- Deploiement cloud.
- Extraction en micro-services.

## Tickets et decisions associees
| Ticket | Besoin | ADR |
| --- | --- | --- |
| W001-T01 | [Backend multi-module](./W001-T01-backend-multi-module.md) | [ADR-W001-T01](../../adr/W001/ADR-W001-T01-backend-multi-module.md) |
| W001-T02 | [PostgreSQL local et schemas Flyway](W001-T02-PostgreSQL-local-et-schemas-Flyway.md) | [ADR-W001-T02](../../adr/W001/ADR-W001-T02-docker-compose-local.md) |
| W001-T03 | [Regles d'architecture executables avec ArchUnit](./W001-T03-regles-architecture-archunit.md) | [ADR-W001-T03](../../adr/W001/ADR-W001-T03-regles-architecture-archunit.md) |
| W001-T04 | [Multi-tenancy et frontiere des comptes](./W001-T04-multi-tenancy-org-id.md) | [ADR-W001-T04](../../adr/W001/ADR-W001-T04-multi-tenancy-org-id.md) |
| W001-T05 | [Roles contextualises et API d'administration](./W001-T05-roles-contextualises-api-administration.md) | [ADR-W001-T05](../../adr/W001/ADR-W001-T05-roles-contextualises-api-administration.md) |

## Ordre d'implementation
```text
W001-T01 Backend multi-module
W001-T02 PostgreSQL local et schemas Flyway
W001-T03 Regles d'architecture executables avec ArchUnit
W001-T04 Multi-tenancy et frontiere des comptes
W001-T05 Roles contextualises et API d'administration
W001-T06 Dockerfile multi-stage
W001-T07 Profils local et localstack
```

`W001-T02` fournit PostgreSQL local et initialise les schemas Flyway par module.
`W001-T03` rend executables les regles d'architecture internes avant l'ajout des
premieres verticales metier.
`W001-T04` s'appuie sur ces schemas et migrations pour definir la racine du
tenant, les donnees portant `org_id` et l'exception du compte plateforme.
`W001-T05` valide ensuite une verticale API/persistence avec des roles
contextualises et une appartenance d'equipe qui se cloture sans effacer
l'historique.

Le scenario fonctionnel de reference est decrit dans
[`SCENARIO-METIER-TEAMPULSE.md`](../SCENARIO-METIER-TEAMPULSE.md).

## Regle documentaire
- Le besoin decrit le probleme a resoudre, les attentes et les criteres d'acceptation.
- L'ADR decrit la decision retenue, les alternatives, les justifications et les consequences.
- Un ticket W001 ne doit pas etre considere termine si son besoin et son ADR ne sont pas coherents avec l'implementation.
