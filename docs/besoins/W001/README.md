# W001 - Socle backend TeamPulse

## Objectif
Mettre en place le socle backend de TeamPulse sous forme de monolithe modulaire Spring Boot.

Ce dossier decrit l'expression de besoin de W001. Les ADR associees documentent les decisions techniques retenues, les alternatives et les justifications.

## Perimetre
- Initialiser l'architecture backend multi-module.
- Mettre en place les frontieres metier entre modules.
- Preparer la persistance PostgreSQL et les migrations Flyway.
- Exposer une premiere API minimale.
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

## Ordre d'implementation
```text
W001-T01 Backend multi-module
W001-T02 PostgreSQL local et schemas Flyway
W001-T04 Multi-tenancy par org_id
W001-T05 API utilisateurs minimale
W001-T06 Dockerfile multi-stage
W001-T07 Profils local et localstack
```

`W001-T02` fournit PostgreSQL local et initialise les schemas Flyway par module.
`W001-T04` s'appuie sur ces schemas et migrations pour porter `org_id`.
`W001-T05` valide ensuite une verticale API/persistence.

## Regle documentaire
- Le besoin decrit le probleme a resoudre, les attentes et les criteres d'acceptation.
- L'ADR decrit la decision retenue, les alternatives, les justifications et les consequences.
- Un ticket W001 ne doit pas etre considere termine si son besoin et son ADR ne sont pas coherents avec l'implementation.
