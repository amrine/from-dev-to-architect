# ADR-W001-T03 - Règles d'architecture exécutables avec ArchUnit

## Statut

Accepted

## Ticket lié

W001-T03 - Règles d'architecture exécutables avec ArchUnit

## Besoin associé

`docs/besoins/W001/W001-T03-regles-architecture-archunit.md`

## ADR techniques adoptés

- [ADR-TECH-004 - Hexagonal boundaries inside business modules](../technical/ADR-TECH-004-hexagonal-business-module-boundaries.md)
- [ADR-TECH-005 - Executable architecture rules with ArchUnit](../technical/ADR-TECH-005-executable-architecture-rules-with-archunit.md)

Cet ADR décide leur adoption par TeamPulse et documente le contrat de packages,
le catalogue de règles et le paramétrage ArchUnit propres au projet.
ADR-TECH-004 reste propriétaire des frontières hexagonales réutilisables.
ADR-TECH-005 reste propriétaire de la stratégie générique d'enforcement.

## Contexte

Spring Modulith vérifie les frontières et les dépendances entre les modules
TeamPulse. Il ne décrit pas suffisamment la direction des dépendances à
l'intérieur d'un module métier ni le placement des composants techniques.

TeamPulse doit rendre ses conventions hexagonales exécutables sans dupliquer
les règles dans chaque module et sans créer de classes de production factices
pour peupler des packages encore inutilisés.

## Décision

### Responsabilité des contrôles

| Contrôle | Responsabilité TeamPulse |
| --- | --- |
| Maven | Dépendances physiques entre les modules Maven et scopes de dépendances. |
| Spring Modulith | Frontières, cycles, surfaces publiques et dépendances autorisées entre modules applicatifs. |
| ArchUnit | Placement et direction des dépendances à l'intérieur des modules métier. |

- Conserver `ApplicationModules.verify()` comme vérification de référence des
  frontières inter-modules.
- Ne pas reproduire avec ArchUnit les contrôles déjà possédés par Spring
  Modulith.
- Utiliser ArchUnit `1.4.2`, déclaré explicitement dans `tp-app` avec le scope
  Maven `test`.
- Exécuter les tests d'architecture dans `tp-app`, qui assemble les classes de
  production de tous les modules.
- Importer uniquement les classes de production sous le package racine dérivé
  de `TpAppApplication`, en excluant les classes de test.
- Exécuter ces tests sans démarrer Spring Boot, PostgreSQL ou Testcontainers.

### Catalogue des modules analysés

- Construire `ArchitectureModules` à partir du modèle Spring Modulith de
  `TpAppApplication`.
- Découvrir les modules métier dynamiquement au lieu de maintenir une seconde
  liste dans les tests.
- Exclure les modules déclarés comme partagés par Spring Modulith, notamment
  `common`, du catalogue des règles internes métier.
- Appliquer aujourd'hui le contrat commun aux modules découverts `identity`,
  `organization` et `team`.
- Paramétrer les règles avec l'identifiant et le package de base retournés par
  le modèle Spring Modulith.
- Centraliser les règles dans `BusinessModuleArchitectureRules` et vérifier
  avec R10 que tous les modules métier reçoivent le même catalogue.

L'exclusion supplémentaire de `io.teampulse.testsupport` du modèle applicatif
est une extension décidée par W001-T04. Elle est décrite plus bas sans être
réattribuée rétroactivement à T03.

### Adoption TeamPulse des contrats techniques

TeamPulse applique les conventions génériques de packages définies par
ADR-TECH-004 à ces racines de modules :

| Module | `<module-root>` |
| --- | --- |
| `identity` | `io.teampulse.identity` |
| `organization` | `io.teampulse.organization` |
| `team` | `io.teampulse.team` |

Les suffixes `domain`, `application.port.in`, `application.port.out`,
`application.service`, `infrastructure.persistence`, `infrastructure.web`,
`infrastructure.messaging`, `config`, `api` et `events` sont appliqués sans
déviation par rapport à ADR-TECH-004.

TeamPulse spécialise le catalogue générique R01 à R10 d'ADR-TECH-005 ainsi :

- R01 considère Spring, Jakarta Validation et JPA comme frameworks interdits au
  domaine, en plus des packages externes du module.
- R04 confine Jakarta Persistence, Spring Data, les entités JPA, repositories et
  mappers dans `infrastructure.persistence`.
- R05 confine Spring Web, Spring HTTP et Jakarta Servlet dans
  `infrastructure.web` et interdit au Web de dépendre directement de la
  persistence.
- R07 utilise `api` et `events` comme surface publique autonome des modules.
- R08 place les configurations Spring dans `config`, les repositories Spring
  dans `infrastructure.persistence` et réserve la racine du module à
  `package-info`.
- R02, R03, R06 et R09 sont appliquées sans spécialisation supplémentaire.
- R01 à R09 sont construites par `BusinessModuleArchitectureRules`.
- R10 est vérifiée séparément par `InternalArchitectureTests`, car elle contrôle
  le catalogue lui-même plutôt qu'une dépendance de production.

### Extensions possédées par W001-T04

W001-T04 complète le dispositif sans modifier la propriété des décisions T03 :

- R11 autorise dans `application.service` uniquement les types Spring
  déclaratifs retenus par T04, interdit JPA, Spring Data, `infrastructure` et
  `config`, puis impose `@Validated` aux classes annotées `@Service`.
- `TestSupportArchitectureTests` vérifie que `tp-test-support` reste hors du
  modèle Spring Modulith et qu'aucun code de production n'en dépend.
- `ArchitectureModules` exclut `io.teampulse.testsupport` du modèle analysé.

Le catalogue actuellement retourné par `BusinessModuleArchitectureRules`
contient donc R01 à R09 et R11. R10 vérifie cette liste pour chaque module
métier. La justification métier et technique de R11 et de `tp-test-support`
reste dans [ADR-W001-T04](ADR-W001-T04-multi-tenancy-organization-reference.md).

### Packages encore vides

- Autoriser temporairement `allowEmptyShould(true)` lorsqu'une règle ne possède
  encore aucune classe cible.
- Ne jamais présenter une règle vide comme une preuve que la frontière a été
  exercée.
- Ne créer aucune classe, interface ou abstraction de production uniquement
  pour activer ArchUnit.
- Appliquer automatiquement la règle dès qu'une capacité réelle introduit une
  classe dans le package concerné.
- Toute exception propre à un module doit être explicite, justifiée et associée
  à un ticket ou une condition de suppression lorsqu'elle est temporaire.

## Alternatives envisagées

- Conserver uniquement des conventions documentées et la revue de code.
- Utiliser Spring Modulith pour les couches internes.
- Dupliquer les règles ArchUnit dans chaque module métier.
- Maintenir manuellement une liste de modules distincte du modèle Modulith.
- Placer les ports dans `domain.port`.
- Utiliser directement les entités JPA comme modèles métier et contrats publics.
- Ajouter ArchUnit uniquement par dépendance transitive.
- Créer des classes factices pour peupler les packages vides.

Les alternatives structurelles et d'enforcement sont évaluées respectivement
dans ADR-TECH-004 et ADR-TECH-005. Elles ne sont pas redéveloppées ici.

## Justification

La découverte depuis Spring Modulith maintient une seule source de vérité : un
nouveau module métier est soumis automatiquement au contrat, tandis qu'un module
partagé ou technique ne reçoit pas accidentellement des règles métier.

La fabrique commune empêche la divergence des règles entre `identity`,
`organization` et `team`. Son placement dans `tp-app` donne accès au classpath
assemblé sans ajouter ArchUnit aux modules de production.

Le découpage hexagonal garde le domaine indépendant des frameworks, place les
ports avec les cas d'usage qui les possèdent et confine les décisions JPA ou HTTP
dans leurs adapters. Les règles structurelles protègent ces frontières ; elles
ne remplacent pas les tests de comportement.

## Conséquences positives

- Les violations internes sont détectées pendant le build Maven.
- Tous les modules métier découverts suivent le même contrat exécutable.
- Un nouveau module métier ne nécessite pas la modification d'une liste ArchUnit
  parallèle.
- Les tests s'exécutent sans contexte Spring ni infrastructure externe.
- Les entités JPA et types HTTP restent confinés à leurs adapters.
- Les futures classes sont contrôlées dès leur apparition.
- Maven, Spring Modulith et ArchUnit gardent des responsabilités distinctes.

## Conséquences négatives / compromis

- Les prédicats et conditions ArchUnit personnalisés doivent évoluer avec
  l'architecture.
- Une règle sans cible peut donner une fausse impression de couverture si son
  statut n'est pas interprété correctement.
- La séparation des modèles métier, persistence et transport ajoute du mapping.
- Une règle structurelle ne prouve ni comportement fonctionnel, ni wiring
  Spring, ni transaction, ni performance SQL.
- Une évolution légitime des packages nécessite une mise à jour coordonnée des
  règles et de cet ADR.

## Impact technique

- `pom.xml` pour `archunit.version`.
- `tp-app/pom.xml` pour la dépendance ArchUnit en scope `test`.
- `tp-app/src/test/java/io/teampulse/architecture/ArchitectureModules.java`.
- `tp-app/src/test/java/io/teampulse/architecture/BusinessModuleArchitectureRules.java`.
- `tp-app/src/test/java/io/teampulse/architecture/InternalArchitectureTests.java`.
- `tp-app/src/test/java/io/teampulse/architecture/ModulithArchitectureTests.java`.
- Extension T04 :
  `tp-app/src/test/java/io/teampulse/architecture/TestSupportArchitectureTests.java`.
- Packages de production des modules `tp-identity`, `tp-organization` et
  `tp-team`.

## Validation

- Exécuter les contrôles ciblés :

  ```bash
  ./mvnw --batch-mode --no-transfer-progress \
    -pl tp-app -am \
    -Dtest=InternalArchitectureTests,ModulithArchitectureTests,TestSupportArchitectureTests \
    -Dsurefire.failIfNoSpecifiedTests=false \
    test
  ```

- Vérifier que cette exécution ne démarre ni Spring Boot, ni PostgreSQL, ni
  Testcontainers.
- Exécuter `./mvnw --batch-mode --no-transfer-progress clean verify` pour la
  validation complète du reactor.
- Introduire temporairement une dépendance de `domain` vers `config`, constater
  l'échec explicite de R01, supprimer la violation et constater le retour au
  vert.
- Vérifier que `archunit` et `tp-test-support` restent absents des scopes de
  production.
- Vérifier que `ApplicationModules.verify()` et les règles ArchUnit sont tous
  découverts par le build standard.

## Risques

- Une règle appliquée à un package vide ne prouve pas encore son efficacité sur
  une classe réelle.
- Une dépendance indirecte ou créée par réflexion peut ne pas être visible comme
  dépendance de bytecode ordinaire.
- Un prédicat trop large peut bloquer une évolution légitime ; l'exception ne
  doit toutefois jamais être ajoutée silencieusement pour un seul module.
- Une nouvelle catégorie d'adapter ou un nouvel usage de framework peut exiger
  une décision complémentaire et une extension du catalogue.

## Notes

`AGENTS.md` est aligné sur les ports dans `application.port.in` et
`application.port.out`.

Les nombres historiques de tests ne font pas partie de la décision : ils
évoluent avec le code. La preuve durable est constituée par les commandes de
validation, les règles exécutées et le résultat observé au moment du changement.

Copier ADR-TECH-004 ou ADR-TECH-005 dans un autre projet ne suffit pas à les y
adopter. Le projet cible doit créer son propre ADR avec ses modules, packages,
outils, règles, exceptions et commandes de validation.
