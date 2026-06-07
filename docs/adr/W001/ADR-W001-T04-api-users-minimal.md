# ADR-W001-T04 - API utilisateurs minimale

## Statut
Draft

## Ticket lie
W001-T04 - API utilisateurs minimale

## Contexte
Il faut exposer un point d'entree minimal pour creer et lire des utilisateurs, plus la sante via Actuator.

## Decision
- Exposer POST /api/users et GET /api/users.
- Utiliser DTO en records Java 21 (UserDto, CreateUserRequest).
- Activer GET /actuator/health.
- La creation utilise orgId = 1L en dur pour W001.

## Alternatives envisagees
- Attendre JWT multi-tenant avant d'exposer l'API.
- Exposer un CRUD complet.
- Utiliser des DTO classes classiques.

## Justification
Une API minimale permet de valider le flux persistence et validation.

## Consequences positives
- Demarrage rapide.
- API facile a tester.

## Consequences negatives / compromis
- API non complete.
- orgId temporairement fixe.

## Impact technique
- Controller, DTOs, service de creation.
- Validation via @Email, @NotBlank.

## Validation
- curl -f localhost:8080/actuator/health.
- POST /api/users cree un user.
- GET /api/users liste les users.

