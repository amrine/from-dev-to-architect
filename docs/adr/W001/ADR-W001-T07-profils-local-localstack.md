# ADR-W001-T07 - Profils local et localstack

## Statut
Draft

## Ticket lie
W001-T07 - Profils local et localstack

## Contexte
Le projet doit supporter un profil local simple et un profil localstack sans appel AWS reel.

## Decision
- Definir les profils local et localstack.
- local active la base locale et les config dev.
- localstack configure les endpoints mock sans dependance AWS reelle.

## Alternatives envisagees
- Un seul profil.
- Gestion uniquement par variables d'environnement.

## Justification
Des profils explicites evitent les erreurs de config et clarifient les usages.

## Consequences positives
- Separation claire des environnements.
- Pas d'appel AWS en local.

## Consequences negatives / compromis
- Multiplication des sections de config.

## Impact technique
- application.yml et fichiers de config par profil si besoin.

## Validation
- Lancement avec SPRING_PROFILES_ACTIVE=local puis localstack sans erreurs.

