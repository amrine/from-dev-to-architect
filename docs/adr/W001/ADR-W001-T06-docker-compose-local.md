# ADR-W001-T06 - Docker Compose local

## Statut
Draft

## Ticket lie
W001-T06 - Docker Compose local

## Contexte
Les developpeurs doivent lancer API et PostgreSQL en une commande.

## Decision
- docker-compose.yml avec:
  - postgres:16-alpine (healthcheck pg_isready, volume pgdata, network tp-net).
  - api construit depuis le Dockerfile, depend de postgres (service_healthy).
- Variables:
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/teampulse
  - SPRING_PROFILES_ACTIVE=local
  - port 8080:8080
- Ajouter .env.example sans secrets.
- Fournir un Makefile pour up, down, logs, psql, smoke.

## Alternatives envisagees
- Lancer Postgres en local sans Docker.
- Utiliser Testcontainers uniquement.
- Utiliser Compose sans healthcheck.

## Justification
Compose donne un demarrage simple et reproductible.

## Consequences positives
- Onboarding rapide.
- Environnement local proche de la prod.

## Consequences negatives / compromis
- Dependence a Docker.

## Impact technique
- docker-compose.yml, .env.example, Makefile.

## Validation
- docker compose up demarre sans erreur.
- curl -f localhost:8080/actuator/health retourne UP.

