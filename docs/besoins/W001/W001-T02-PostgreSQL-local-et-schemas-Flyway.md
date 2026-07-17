# W001-T02 - PostgreSQL local et schemas Flyway

## Objectif
Fournir un environnement PostgreSQL local reproductible et initialiser un espace SQL autonome pour chaque module metier.

## Besoin
Les developpeurs doivent pouvoir lancer PostgreSQL sans installation manuelle et demarrer TeamPulse avec une configuration locale explicite.
La structure de la base doit etre reproductible entre environnements et chaque module metier doit pouvoir faire evoluer son schema independamment.

## Contraintes
- Utiliser une image PostgreSQL explicite, sans `latest`.
- Conserver les donnees locales entre les redemarrages.
- Fournir un healthcheck PostgreSQL.
- Eviter de stocker les fichiers internes PostgreSQL dans le repository.
- Utiliser une seule base et une seule `DataSource` pour le monolithe modulaire.
- Interdire la creation et la modification automatique des tables par Hibernate.
- Utiliser un schema PostgreSQL et un historique Flyway par module metier.
- Permettre a chaque module de numeroter ses migrations sans coordination globale.
- Ne pas charger Testcontainers dans l'application de production.

## Livrables attendus
- Fichier Docker Compose local avec PostgreSQL, volume persistant et healthcheck.
- Configuration Spring Boot activee par le profil `local`.
- Variables locales de connexion surchargeables.
- Configuration Flyway portee par chaque module metier.
- Schemas `tp_organization`, `tp_identity` et `tp_team`.
- Table `flyway_schema_history` dans chaque schema.
- Configuration JPA en mode `ddl-auto=validate`.
- Tests d'integration PostgreSQL avec Testcontainers.
- Documentation des commandes locales et des tests.

## Criteres d'acceptation
- PostgreSQL demarre avec Docker Compose et devient `healthy`.
- La base `teampulse` est accessible sur `localhost:5432`.
- L'application demarre avec `SPRING_PROFILES_ACTIVE=local`.
- Une base vide est initialisee avec les trois schemas metier.
- Chaque schema possede sa propre table `flyway_schema_history`.
- Le demarrage avec `ddl-auto=validate` reussit.
- Un test d'integration cible ne charge que le schema Flyway du module teste.
- La suite complete initialise les trois schemas sur PostgreSQL Testcontainers.
- Les tests ne dependent pas du conteneur PostgreSQL lance par Docker Compose.

## Decision associee
Voir [ADR-W001-T02](../../adr/W001/ADR-W001-T02-docker-compose-local.md).
