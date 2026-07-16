# ADR-W001-T01 - Backend multi-module

## Statut
Accepted

## Ticket lie
W001-T01 - Backend multi-module

## Contexte
Nous devons demarrer TeamPulse comme un monolithe modulaire Spring Boot.
L'application doit contenir plusieurs domaines metier (`organization`, `identity`, `team`) et un module partage (`common`), tout en gardant un point de demarrage unique.

Nous voulons eviter la complexite operationnelle des micro-services tout en conservant des frontieres metier explicites, testables et verifiables.

## Decision
- Utiliser un projet Maven multi-module.
- Modules Maven: `tp-common`, `tp-organization`, `tp-identity`, `tp-team`, `tp-app`.
- `tp-app` est le seul module executable et produit l'unique JAR Spring Boot.
- `tp-app` depend de `tp-common`, `tp-organization`, `tp-identity` et `tp-team`.
- Les modules metier produisent des JAR bibliotheques.
- Les modules metier dependent de `tp-common`.
- `tp-common` ne depend d'aucun module metier.
- Utiliser Java 25 et Spring Boot 4.1.x.
- Utiliser Spring Modulith pour definir et verifier les frontieres metier.
- `tp-app` porte `@SpringBootApplication` et `@Modulithic`.
- Les modules logiques Spring Modulith sont `common`, `organization`, `identity` et `team`.
- `common` est declare comme module partage.
- La detection Spring Modulith utilise la strategie `explicitly-annotated`.
- Chaque module logique est declare avec `@ApplicationModule`.
- Les packages publics d'un module sont exposes avec `@NamedInterface`, principalement `api` et `events`.
- Les packages `domain`, `application` et `infrastructure` restent internes au module.
- Les dependances inter-modules autorisees sont declarees avec `allowedDependencies`.

## Repartition des dependances techniques
- `tp-app` porte les dependances d'assemblage et d'execution globales: serveur web, Flyway et driver PostgreSQL.
- Les modules metier portent les dependances necessaires a leurs adapters: web, validation et data-jpa.
- `tp-common` reste sans dependance web ou JPA.
- Le detail de PostgreSQL local, des schemas et des migrations DB est traite dans `ADR-W001-T02`.
- Le detail Docker Compose local est traite dans `ADR-W001-T02`.

## Communication entre modules
- Les appels synchrones entre modules utilisent une API Java publique exposee dans le package `api`.
- Les notifications de faits metier utilisent des evenements Spring Modulith exposes dans le package `events`.
- Les modules internes ne communiquent pas entre eux via HTTP.
- OpenAPI sera utilise plus tard pour documenter et generer les clients HTTP externes, notamment vers le front.
- Les evenements doivent representer des faits passes et ne doivent pas exposer d'entites JPA ou de classes internes.

## Graphe de dependances
```text
tp-app
+-- tp-common
+-- tp-organization
+-- tp-identity
+-- tp-team

tp-organization --> tp-common
tp-identity     --> tp-common
tp-team         --> tp-common
tp-common       --> aucun module metier
```

Toute nouvelle dependance metier doit etre declaree a deux niveaux:
- dependance Maven dans le `pom.xml` concerne;
- dependance logique Spring Modulith dans `allowedDependencies`.

## Alternatives envisagees
- Monolithe en un seul module.
- Micro-services separes par domaine.
- Build Gradle.
- Frontieres documentees uniquement par convention, sans verification Spring Modulith.

## Justification
Un monolithe modulaire garde des frontieres claires sans la complexite d'un systeme distribue.
Spring Modulith rend ces frontieres executables: le build peut detecter les cycles, les acces interdits entre modules et les violations des dependances autorisees.

## Consequences positives
- Separation claire des domaines.
- Demarrage simple via un seul module bootstrap.
- Frontieres metier verifiees automatiquement pendant le build.
- Tests d'integration isoles par module.
- Communication interne sans cout reseau.
- Preparation a une extraction future eventuelle en micro-services.

## Consequences negatives / compromis
- Plus de configuration Maven.
- Gestion des dependances inter-modules a maintenir.
- Les noms logiques Spring Modulith different des `artifactId` Maven.
- Les API publiques et les evenements doivent rester stables.
- Toute nouvelle dependance inter-module doit respecter Maven et Spring Modulith.
- Les evenements peuvent introduire une coherence differee.

## Impact technique
- `pom.xml` parent et poms des modules.
- Modules `tp-common`, `tp-organization`, `tp-identity`, `tp-team`, `tp-app`.
- `tp-app/src/main/java/io/teampulse/TpAppApplication.java`.
- `package-info.java` racine de chaque module logique avec `@ApplicationModule`.
- `package-info.java` des packages `api` et `events` avec `@NamedInterface`.
- Tests d'architecture Spring Modulith dans `tp-app`.

## Validation
- `mvn clean verify` construit tous les modules et execute les tests.
- `ApplicationModules.of(TpAppApplication.class).verify()` valide les frontieres Spring Modulith.
- Les modules detectes sont `common`, `organization`, `identity` et `team`.
- Les modules `organization`, `identity` et `team` demarrent avec `@ApplicationModuleTest`.
- `common` est inclus comme module partage.
- `tp-app` produit l'unique JAR Spring Boot executable.
