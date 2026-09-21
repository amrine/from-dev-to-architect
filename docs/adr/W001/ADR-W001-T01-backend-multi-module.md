# ADR-W001-T01 - Adopter un monolithe modulaire pour TeamPulse

## Statut

Accepted

## Ticket lié

W001-T01 - Backend multi-module

## Besoin associé

[`docs/besoins/W001/W001-T01-backend-multi-module.md`](../../besoins/W001/W001-T01-backend-multi-module.md)

## Décision technique adoptée

TeamPulse adopte
[ADR-TECH-001 - Maven and Spring Modulith modular monolith](../technical/ADR-TECH-001-maven-spring-modulith-modular-monolith.md)
comme socle de son architecture modulaire.

Cet ADR projet décide que cette architecture est adaptée à TeamPulse et fixe
son paramétrage concret. ADR-TECH-001 reste propriétaire du mécanisme Maven et
Spring Modulith ainsi que de la discipline des modules partagés réutilisable par
d'autres projets.

## Contexte

TeamPulse démarre avec plusieurs responsabilités métier distinctes : identité,
organisation et équipes. Ces responsabilités doivent rester séparées et
testables sans introduire la complexité opérationnelle de services distribués.

Le produit doit être déployé comme une seule application Spring Boot, tout en
rendant explicites les surfaces publiques et les dépendances entre modules.

## Décision

### Socle technologique

- Utiliser Java 25, Maven, Spring Boot 4.1.x et Spring Modulith 2.0.x.
- `pom.xml` reste la source de vérité des versions mineures et correctives
  effectivement utilisées.
- Utiliser la stratégie de détection Spring Modulith
  `explicitly-annotated`.

### Modules TeamPulse

Le reactor Maven contient six modules, répartis selon leur responsabilité :

| Module | Responsabilité | Présence dans le modèle Spring Modulith |
| --- | --- | --- |
| `tp-app` | Bootstrap, assemblage et unique application exécutable | Racine d'assemblage |
| `tp-common` | Capacités techniques transverses partagées | Module partagé `common` |
| `tp-identity` | Identité et utilisateurs | Module métier `identity` |
| `tp-organization` | Organisation et racine du tenant | Module métier `organization` |
| `tp-team` | Équipes et appartenances | Module métier `team` |
| `tp-test-support` | Infrastructure de test réutilisable | Exclu du modèle applicatif |

- `tp-app` porte `@SpringBootApplication` et `@Modulithic` et produit l'unique
  JAR Spring Boot exécutable.
- Les modules métier et `tp-common` produisent des JAR de bibliothèque.
- `tp-test-support` est consommé uniquement avec le scope Maven `test` et ne
  constitue pas un module fonctionnel.
- Aucun module métier ni `tp-common` ne dépend de `tp-app`.
- `tp-common` ne dépend d'aucun module métier.
- Les capacités admises dans `tp-common` respectent la politique de partage
  d'ADR-TECH-001 et restent exposées par des interfaces nommées ciblées.

### Graphe des dépendances

```text
tp-app
+-- tp-common
+-- tp-identity
+-- tp-organization
+-- tp-team

tp-identity
+-- common::context
+-- common::mapping
+-- common::persistence
+-- common::reference

tp-organization
+-- common::mapping
+-- common::persistence
+-- common::reference
+-- identity::user

tp-team
+-- common::context
+-- common::mapping
+-- common::persistence
+-- common::reference
+-- identity::user
+-- organization::organization

tp-app, tp-identity, tp-organization, tp-team
+-- tp-test-support (scope test uniquement)
```

Les dépendances fonctionnelles spécifiques à la multi-tenancy et aux
directories sont justifiées par ADR-W001-T04. T01 reste propriétaire de la
forme globale du monolithe et de son assemblage.

### Surfaces publiques et communication

- Les capacités synchrones inter-modules sont exposées par des interfaces Java
  publiques déclarées avec `@NamedInterface`.
- Une interface nommée représente une capacité précise, par exemple
  `identity::user`, `organization::organization` ou `common::context`. Un
  package générique `api` n'est pas automatiquement exposé à tous les modules.
- Les notifications découplées utilisent des événements immuables représentant
  des faits passés et exposés par une interface nommée `events`.
- Aucun contrat public n'expose une entité JPA ou un type interne du domaine, de
  l'application ou de l'infrastructure.
- Les modules internes ne communiquent pas entre eux via HTTP.
- T01 ne décide ni de l'usage futur d'OpenAPI, ni d'un protocole de communication
  pour une éventuelle extraction en services. Une telle évolution nécessitera
  une décision dédiée.

### Répartition des dépendances techniques

- `tp-app` porte le serveur Web, l'assemblage Spring Modulith et le driver
  PostgreSQL d'exécution.
- Chaque module métier porte les dépendances nécessaires à ses adapters,
  notamment Web, Jakarta Validation, JPA et Flyway.
- `tp-common` ne dépend ni du Web ni de JPA.
- `tp-test-support` confine Spring Boot Test, Testcontainers et les
  configurations partagées d'intégration dans les classpaths de test.
- PostgreSQL local, les schémas et Flyway sont détaillés par ADR-W001-T02.
- Les règles internes des couches sont détaillées et vérifiées par
  ADR-W001-T03.

## Alternatives envisagées

- Construire TeamPulse comme un monolithe sans frontières modulaires
  exécutables.
- Déployer immédiatement un service indépendant par domaine métier.
- Déployer plusieurs applications tout en conservant un repository et un build
  uniques.
- Utiliser des appels HTTP entre modules à l'intérieur de la même unité de
  déploiement.

Les alternatives Maven, Gradle, Spring Modulith ou conventions seules sont
évaluées dans ADR-TECH-001 et ne sont pas répétées ici.

## Justification

Le monolithe modulaire répond au besoin actuel de TeamPulse : une seule unité à
construire et à opérer, avec des frontières métier explicites. Les appels Java
en mémoire évitent un coût réseau et une cohérence distribuée sans valeur à ce
stade.

La séparation entre `identity`, `organization` et `team` conserve la propriété
des modèles et des données. Les interfaces nommées limitent les dépendances à la
capacité réellement consommée, tandis que Maven et Spring Modulith rendent le
graphe vérifiable.

## Conséquences positives

- Déploiement et exploitation d'une seule application.
- Responsabilités métier séparées et vérifiées pendant le build.
- Dépendances inter-modules limitées à des capacités publiques explicites.
- Tests de modules possibles sans démarrer toutes les implémentations métier.
- Aucun transport réseau artificiel entre modules colocalisés.
- Possibilité d'étudier une extraction future sans en faire une promesse de T01.

## Conséquences négatives / compromis

- Le reactor Maven et les métadonnées Modulith demandent plus de configuration.
- Chaque nouvelle dépendance inter-module doit être déclarée dans Maven et dans
  `allowedDependencies`.
- Les interfaces nommées constituent des contrats de compatibilité à maintenir.
- Les modules partagent encore le même processus, le même déploiement et les
  mêmes ressources d'exécution.
- Une extraction future nécessitera une décision sur les transports, les
  transactions, la résilience et la cohérence distribuée.

## Impact technique

- `pom.xml` parent et POM de chaque module.
- `tp-app/src/main/java/io/teampulse/TpAppApplication.java`.
- `package-info.java` racine de `common`, `identity`, `organization` et `team`.
- `package-info.java` de chaque interface nommée publique.
- Tests Spring Modulith et contrôles d'exclusion de `tp-test-support` dans
  `tp-app/src/test/java/io/teampulse/architecture`.

## Validation

- `./mvnw --batch-mode --no-transfer-progress verify` construit le reactor.
- `tp-app` produit l'unique JAR Spring Boot exécutable.
- `ArchitectureModules.modules().verify()` valide les frontières, les exclusions
  techniques et les dépendances autorisées à partir de `TpAppApplication`.
- Les modules détectés sont `common`, `identity`, `organization` et `team`.
- `common` est le seul module partagé.
- `tp-test-support` est absent du modèle Spring Modulith et des dépendances de
  production.
- Les tests `@ApplicationModuleTest` démarrent les modules métier avec les
  capacités externes explicitement fournies par leurs configurations de test.

## Notes

- T01 décide de l'architecture TeamPulse ; ADR-TECH-001 décrit son mécanisme
  technique réutilisable.
- Une réutilisation d'ADR-TECH-001 dans un autre projet exige un ADR local qui
  renseigne ses propres modules, versions, interfaces et déviations.
- Les décisions T02, T03 et T04 spécialisent cette architecture sans changer
  son principe de déploiement unique.
