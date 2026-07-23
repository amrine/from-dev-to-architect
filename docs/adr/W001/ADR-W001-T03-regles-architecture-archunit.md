# ADR-W001-T03 - Regles d'architecture executables avec ArchUnit

## Statut
Accepted

## Ticket lie
W001-T03 - Regles d'architecture executables avec ArchUnit

## Besoin associe
`docs/besoins/W001/W001-T03-regles-architecture-archunit.md`

## Contexte
Spring Modulith verifie les frontieres et les dependances entre les modules metier
`identity`, `organization`, `team` et `common`. Il ne decrit pas suffisamment la
direction des dependances a l'interieur d'un module metier.

TeamPulse doit donc rendre executables les conventions d'architecture hexagonale
avant l'apparition des premieres classes metier, sans creer de classes de production
factices uniquement pour activer les regles.

## Decision

- Conserver `ApplicationModules.verify()` comme verification de reference des
  frontieres inter-modules.
- Utiliser ArchUnit pour verifier l'organisation interne de `identity`,
  `organization` et `team`.
- Declarer explicitement ArchUnit dans `tp-app` avec le scope Maven `test`.
- Decouvrir dynamiquement les modules metier a partir du modele Spring Modulith de
  `TpAppApplication`, sans maintenir une liste dans une configuration de test.
- Exclure de ce catalogue les modules declares comme partages par Spring Modulith,
  notamment `common`.
- Centraliser les regles communes dans une fabrique parametree par le nom et le
  package de base de chaque module decouvert.
- Appliquer le meme catalogue de regles a tous les modules metier decouverts.
- Placer les tests d'architecture dans `tp-app`, qui assemble les classes de
  production de tous les modules.
- Importer uniquement les classes de production sous le package racine derive de
  `TpAppApplication`, en excluant les classes de test.
- Placer les ports entrants et sortants dans `application.port.in` et
  `application.port.out`.
- Garder les entites metier dans `domain` et les entites JPA, repositories Spring
  Data et mappings dans `infrastructure.persistence`.
- Garder les controleurs et types HTTP dans `infrastructure.web`.
- Garder les configurations Spring dans `config`.
- Autoriser temporairement une regle sans classe cible lorsque le package concerne
  est encore vide.
- Ne creer aucune classe de production factice pour satisfaire ArchUnit.

ArchUnit controle la structure et la direction des dependances internes. Maven
controle les dependances physiques entre modules. Spring Modulith controle les
frontieres et dependances inter-modules. Ces responsabilites restent
complementaires.

## Alternatives envisagees

- Conserver uniquement des conventions documentees sans verification executable.
- Utiliser Spring Modulith pour verifier aussi les couches internes.
- Dupliquer les regles ArchUnit dans chaque module metier.
- Placer les ports dans `domain.port`.
- Utiliser directement les entites JPA comme modele metier.
- Ajouter ArchUnit uniquement par dependance transitive.
- Creer des classes de production factices pour peupler les packages vides.

## Justification

Une fabrique commune evite la divergence des regles entre les modules metier. La
decouverte a partir du modele Spring Modulith conserve une seule source de verite :
un nouveau module metier est automatiquement soumis aux regles, tandis qu'un module
partage reste hors du contrat interne R01 a R09. Le placement dans `tp-app` permet
une analyse unique du code assemble sans ajouter ArchUnit au runtime des modules de
production.

Les ports appartiennent a l'application car ils expriment les contrats des cas
d'usage. Le domaine reste independant de l'infrastructure et des frameworks.

La separation entre entites metier et entites JPA protege le domaine de la
persistence. Les decisions de performance JPA detaillees, telles que les projections,
les plans de chargement et la prevention des problemes N+1, seront documentees avec
les tickets qui introduiront les premiers cas d'usage et agregats.

## Consequences positives

- Les violations internes sont detectees pendant le build Maven.
- Tous les modules metier decouverts suivent le meme contrat executable.
- L'ajout d'un module metier ne necessite pas de modifier une liste propre aux tests
  ArchUnit.
- Le package racine n'est pas code en dur dans les tests, ce qui facilite la reprise
  du projet comme squelette.
- Les tests d'architecture peuvent s'executer sans Spring Boot, PostgreSQL ou
  Testcontainers.
- Les entites JPA restent confinees a la persistence.
- Les futures classes sont controlees automatiquement des leur apparition.
- Spring Modulith et ArchUnit gardent des responsabilites distinctes.

## Consequences negatives / compromis

- Certaines regles restent sans cible tant que les packages sont vides.
- Le mapping entre modele metier et modele de persistence ajoute du code et des
  allocations ; il devra etre mesure et limite aux frontieres utiles.
- Les regles doivent evoluer si de nouvelles zones techniques sont introduites.
- Une regle structurelle ne prouve ni le comportement fonctionnel ni la performance
  des requetes JPA.

## Impact technique

- `pom.xml` pour la version ArchUnit.
- `tp-app/pom.xml` pour la dependance ArchUnit de test.
- `tp-app/src/test/java/io/teampulse/architecture` pour la fabrique et les tests
  ArchUnit partages.
- `ArchitectureModules` derive le catalogue des modules du modele Spring Modulith et
  exclut les modules partages.
- `BusinessModuleArchitectureRules` construit les regles R01 a R09 pour chaque
  module decouvert.
- `InternalArchitectureTests` execute ces regles et verifie avec R10 que leur
  catalogue reste identique pour tous les modules metier.
- `ModulithArchitectureTests` reste la verification inter-modules.
- Aucun fichier de production ne doit etre ajoute uniquement pour ArchUnit.

## Validation

1. Executer les tests ArchUnit seuls et verifier qu'ils ne demarrent ni Spring Boot,
   ni PostgreSQL, ni Testcontainers.
2. Executer `mvn clean verify` et verifier que les tests Modulith et ArchUnit sont
   decouverts.
3. Introduire temporairement dans `domain` une dependance interdite vers `config` et
   constater l'echec de R01 avec un message explicite.
4. Supprimer la violation et constater le retour au vert.
5. Verifier que la dependance ArchUnit reste en scope Maven `test`.

## Risques

- Une regle appliquee a un package vide peut donner une fausse impression de
  couverture ; les packages cibles devront etre testes des l'apparition des classes.
- Une mauvaise strategie de chargement JPA peut annuler les gains apportes par la
  separation architecturale ; ce point sera valide avec les premiers cas concrets.
- Une exception propre a un seul module pourrait faire diverger le contrat commun.

## Notes

Les regles R01 a R09 sont centralisees dans une fabrique commune. R10 est verifiee
par un test qui s'assure que les memes identifiants de regles sont appliques a tous
les modules metier decouverts.

`AGENT.md` est aligne sur la decision `application.port.in` et
`application.port.out`.

Validation acceptee le 22 juillet 2026 apres l'echec controle de R01, la suppression
de la violation temporaire et le retour au vert des 29 tests d'architecture puis des
35 tests executes par `mvn clean verify`.
