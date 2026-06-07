# ADR-W001-T02 - Multi-tenancy par org_id

## Statut
Draft

## Ticket lie
W001-T02 - Multi-tenancy par org_id

## Contexte
Le systeme doit etre multi-tenant. Chaque entite metier doit etre rattachee a une organisation. Pour W001, l'orgId est fixe a 1L.

## Decision
- Toutes les tables incluent org_id NOT NULL.
- Introduire BaseEntity avec id, orgId, createdAt.
- Propager orgId dans toutes les entites (User, Team, TeamMember, Organization).
- En W001, utiliser orgId = 1L en dur, JWT et resolution du tenant en W008.

## Alternatives envisagees
- Base de donnees par tenant.
- Schema par tenant.
- Table de jointure tenant-entity.

## Justification
Le champ org_id permet une isolation simple par ligne et une evolution vers des filtres de requetes.

## Consequences positives
- Coherence du modele multi-tenant.
- Contraintes DB garantissent la presence du tenant.

## Consequences negatives / compromis
- Necessite de propager l'orgId dans chaque couche.
- Risque d'oubli lors de nouvelles entites.

## Impact technique
- BaseEntity dans tp-common ou module metier.
- Entites JPA et migrations Flyway.
- Services et repositories doivent renseigner ou filtrer orgId.

## Validation
- Test d'integration: creation d'un User avec orgId non nul.
- Contraintes DB refusent org_id NULL.

