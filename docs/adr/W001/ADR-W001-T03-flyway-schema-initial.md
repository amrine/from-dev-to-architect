# ADR-W001-T03 - Flyway schema initial

## Statut
Draft

## Ticket lie
W001-T03 - Flyway schema initial

## Contexte
Nous devons versionner le schema DB et interdire la generation automatique.

## Decision
- Utiliser Flyway comme source de verite du schema.
- Ajouter V001__init.sql avec tables organizations, users, teams, team_members (toutes avec org_id NOT NULL).
- Configurer spring.jpa.hibernate.ddl-auto=validate.
- Utiliser PostgreSQL.

## Alternatives envisagees
- Hibernate auto DDL (update ou create).
- Liquibase.
- Schema cree a la main sans migration.

## Justification
Flyway garantit des migrations tracees et reproductibles.

## Consequences positives
- Schema versionne.
- Demarrage coherent entre environnements.

## Consequences negatives / compromis
- Chaque changement requiert une nouvelle migration.

## Impact technique
- tp-app/src/main/resources/db/migration/V001__init.sql.
- application.yml pour ddl-auto.

## Validation
- Base vide: Flyway cree le schema sans erreur.
- Demarrage avec ddl-auto=validate reussit.

