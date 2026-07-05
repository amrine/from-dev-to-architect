---
name: generate-w00x-besoins-adr
description: Generate TeamPulse project documentation from a W00X roadmap row. Use when the user provides a TeamPulse Excel roadmap row or week code and asks to generate, create, preview, update, or validate Markdown besoin files under docs/besoins/W00X and ADR files under docs/adr/W00X, with one BESOIN and one ADR per ticket T01, T02, etc.
---

# Génération des besoins et ADR par ticket W00X

## Objectif

Transformer une ligne de roadmap TeamPulse correspondant à une semaine `W00X` en documentation projet exploitable.

Générer deux familles de fichiers Markdown :

- `docs/besoins/W00X/BESOIN-W00X-TYY-nom-court.md`
- `docs/adr/W00X/ADR-W00X-TYY-nom-court.md`

Un ticket n'est pas prêt à développer tant que son fichier besoin et son ADR associé n'existent pas.

## Entrées acceptées

Exploiter une ligne issue de `TeamPulse - Exécution` avec ces colonnes si présentes :

```text
Semaine
Phase
Compétence appliquée
Objectif TeamPulse
Code à produire
Infra / DevOps / Cloud
Validation concrète
Artefact à conserver
Definition of Done
Mode / coût
Statut
Notes
```

Ou une ligne issue de `Planning Détaillé` avec ces colonnes si présentes :

```text
Semaine
Mois
Phase
Formation (Lun/Mar)
Heures Form.
Chemin Formation
Tâches TeamPulse — synthèse
Heures Projet
Artefact
Status
Notes
```

Si certaines colonnes sont absentes, travailler avec les informations disponibles. Ne pas inventer de périmètre hors sujet.

Si le contenu de la ligne W00X manque complètement, demander la ligne Excel ou le code de la semaine à traiter.

## Workflow

1. Identifier le numéro de semaine.
2. Normaliser la semaine au format `W001`, `W002`, etc.
3. Extraire les objectifs, validations, artefacts, DoD et contraintes.
4. Découper le travail en tickets atomiques `T01`, `T02`, `T03`, etc.
5. Créer exactement un besoin et un ADR par ticket.
6. Respecter strictement le périmètre de la semaine.
7. Créer les dossiers `docs/besoins/W00X/` et `docs/adr/W00X/` si nécessaire.
8. Écrire les fichiers Markdown dans le workspace quand un workspace est disponible, sauf si l'utilisateur demande seulement un aperçu.
9. Répondre avec le résumé de la semaine, les tickets, l'arborescence créée et les fichiers modifiés.

## Découpage en tickets

Créer des tickets atomiques correspondant chacun à une unité livrable claire.

Repères :

- Viser 3 à 8 tickets.
- Éviter les tickets trop gros.
- Éviter les tickets artificiels ou inutiles.
- Ne pas isoler une simple ligne de configuration si elle appartient naturellement à un ticket plus large.
- Numéroter dans l'ordre `T01`, `T02`, `T03`.

Exemples de tickets :

```text
W001-T01 — Initialiser le backend multi-module
W001-T02 — Configurer Docker Compose local
W001-T03 — Modéliser la multi-tenancy avec org_id
W001-T04 — Créer la migration Flyway initiale
W001-T05 — Exposer l'API minimale Users
W001-T06 — Ajouter les tests d'intégration backend
W001-T07 — Documenter le démarrage local
```

## Nommage

Créer ces dossiers par semaine :

```text
docs/besoins/W00X/
docs/adr/W00X/
```

Nommer les besoins :

```text
BESOIN-W00X-TYY-nom-court-du-ticket.md
```

Nommer les ADR :

```text
ADR-W00X-TYY-nom-court-du-ticket.md
```

Règles :

- Utiliser le même code semaine, le même code ticket et le même slug pour le besoin et l'ADR.
- Utiliser un slug court en kebab-case, sans accents, sans espaces et sans majuscules.
- Stocker l'ADR dans `docs/adr/W00X/`.

## Template besoin

Chaque fichier besoin doit suivre cette structure :

```md
# BESOIN-W00X-TYY — Titre du besoin

## Statut
Draft

## Ticket lié
W00X-TYY — Titre du ticket

## Source roadmap
- Semaine : W00X
- Phase : ...
- Compétence appliquée : ...
- Objectif TeamPulse : ...

## Contexte
Décrire le contexte fonctionnel, technique ou pédagogique du besoin.

## Problème à résoudre
Décrire précisément le problème que ce ticket doit résoudre.

## Objectif
Décrire le résultat attendu à la fin du ticket.

## Périmètre inclus
- ...

## Périmètre exclu
- ...

## Règles métier / techniques
- ...

## Critères d'acceptation
- [ ] ...
- [ ] ...
- [ ] ...

## Validation attendue
Décrire comment vérifier concrètement que le besoin est satisfait.

## Artefacts attendus
- ...

## Notes pédagogiques
Expliquer ce que ce ticket permet d'apprendre ou de pratiquer.

## Liens avec les autres tickets
- Dépend de : ...
- Prépare : ...
```

## Template ADR

Chaque ADR doit suivre cette structure :

```md
# ADR-W00X-TYY — Titre court

## Statut
Draft

## Ticket lié
W00X-TYY — Nom du ticket

## Besoin associé
docs/besoins/W00X/BESOIN-W00X-TYY-nom-court.md

## Contexte
Décrire le problème, le besoin ou la contrainte du ticket.

## Décision
Décrire clairement la décision technique prise.

## Alternatives envisagées
- Option A : ...
- Option B : ...
- Option C : ...

## Justification
Expliquer pourquoi cette décision est retenue.

## Conséquences positives
- ...

## Conséquences négatives / compromis
- ...

## Impact technique
Décrire les modules, packages, fichiers, tables, endpoints ou configurations impactés.

## Validation
Décrire comment vérifier que la décision est correctement appliquée.

## Risques
- ...

## Notes
Informations complémentaires si nécessaire.
```

## Statuts

Utiliser uniquement :

- `Draft` : document proposé avant ou pendant l'implémentation.
- `Accepted` : document validé et cohérent avec l'implémentation.
- `Superseded` : document remplacé par une version ou décision plus récente.
- `Rejected` : besoin ou décision abandonné.

Par défaut, utiliser `Draft`.

## Cohérence besoin / ADR

Pour un même ticket :

- Le besoin répond à : pourquoi ce ticket existe et qu'est-ce qu'on veut obtenir ?
- L'ADR répond à : quelle décision technique prend-on pour satisfaire ce besoin ?
- Le champ `Besoin associé` de l'ADR doit pointer vers le fichier besoin exact.
- Les noms de fichiers, titres, tickets et slugs doivent rester cohérents.
- Si l'implémentation finale change la décision initiale, mettre à jour l'ADR et le besoin si le périmètre change.

## Règles anti-sur-scope

Ne jamais ajouter de technologies ou fonctionnalités absentes de la ligne roadmap, sauf nécessité stricte pour satisfaire le ticket.

Respecter :

- l'objectif de la semaine ;
- les tâches mentionnées ;
- la validation concrète ;
- la Definition of Done ;
- les artefacts attendus ;
- le mode / coût si présent.

Ne pas ajouter automatiquement :

- authentification ;
- sécurité avancée ;
- microservices ;
- cloud réel ;
- CI/CD ;
- Kafka ;
- Kubernetes ;
- observabilité avancée ;
- frontend ;
- tests complexes.

Ajouter ces éléments seulement s'ils sont explicitement mentionnés dans la ligne W00X.

## Sortie attendue

Quand des fichiers sont créés ou mis à jour, répondre avec :

1. un résumé de la semaine ;
2. la liste des tickets proposés ;
3. l'arborescence des fichiers ;
4. la liste des fichiers créés ou modifiés.

Si l'utilisateur demande un aperçu sans écriture, afficher aussi le contenu complet de chaque fichier dans cet ordre :

```text
docs/besoins/W00X/
docs/adr/W00X/
```

Pour chaque ticket, présenter d'abord le besoin puis l'ADR associé.

## Définition de terminé

La génération documentaire d'une semaine W00X est terminée uniquement si :

- chaque ticket possède un fichier besoin ;
- chaque ticket possède un ADR ;
- les noms de fichiers sont cohérents ;
- les liens besoin / ADR sont corrects ;
- les documents respectent le périmètre de la ligne Excel ;
- les critères d'acceptation sont testables ;
- les validations sont concrètes ;
- les impacts techniques sont explicites ;
- aucun élément hors-scope n'a été ajouté.
