# ADR-W001-T02 - PostgreSQL local et schémas Flyway par module

## Statut

Accepted

## Ticket lié

W001-T02 - PostgreSQL local et schémas Flyway

## Besoin associé

`docs/besoins/W001/W001-T02-PostgreSQL-local-et-schemas-Flyway.md`

## ADR techniques adoptés

- [ADR-TECH-002 - Shared PostgreSQL with module-owned Flyway migrations](../technical/ADR-TECH-002-shared-postgresql-with-module-owned-flyway.md)
- [ADR-TECH-003 - PostgreSQL integration tests with Testcontainers](../technical/ADR-TECH-003-postgresql-integration-tests-with-testcontainers.md)

Cet ADR décide leur adoption par TeamPulse et documente leur paramétrage
concret. ADR-TECH-002 reste propriétaire de la topologie PostgreSQL/Flyway et
de la stratégie Docker Compose locale réutilisables. ADR-TECH-003 reste
propriétaire de la stratégie générique de tests PostgreSQL.

## Contexte

TeamPulse doit fournir un PostgreSQL local simple à démarrer, garantir que sa
structure SQL est reproductible et préserver les frontières de son monolithe
modulaire.

Une configuration Flyway globale imposerait aux modules un historique et une
numérotation partagés. Plusieurs bases ou plusieurs `DataSource` ajouteraient
en revanche une complexité opérationnelle injustifiée pour un unique artefact
déployable.

Les tests d'intégration doivent exercer PostgreSQL sans dépendre du conteneur
Docker Compose ni des données locales du développeur.

## Décision

### Environnement PostgreSQL local

- Utiliser l'image officielle épinglée `postgres:18-bookworm`.
- Exposer PostgreSQL localement sur le port `5432`.
- Créer la base et l'utilisateur locaux `teampulse`.
- Utiliser un volume Docker nommé `teampulse-postgres-data`.
- Vérifier la disponibilité avec `pg_isready` dans le healthcheck.
- Activer explicitement la configuration locale avec
  `SPRING_PROFILES_ACTIVE=local`.
- Permettre de surcharger la connexion avec `TEAM_PULSE_DB_URL`,
  `TEAM_PULSE_DB_USERNAME` et `TEAM_PULSE_DB_PASSWORD`.
- Conserver `docker/docker-compose.yaml` comme source exécutable de cette
  configuration ; l'ADR ne duplique pas son contenu YAML.

### Persistance et migrations par module

- Utiliser une seule `DataSource` Spring Boot portée par `tp-app`.
- Désactiver l'auto-configuration Flyway globale avec
  `spring.flyway.enabled=false`.
- Faire porter à chaque module métier sa configuration Flyway, son schéma, son
  emplacement de migrations et son historique.
- Configurer chaque instance Flyway avec `createSchemas(true)`, le schéma du
  module comme `defaultSchema` et une table `flyway_schema_history` locale à ce
  schéma.
- Ne pas créer de migration vide ou redondante uniquement pour exécuter
  `CREATE SCHEMA`.
- Laisser chaque module numéroter ses migrations indépendamment.
- Configurer Hibernate avec `spring.jpa.hibernate.ddl-auto=validate`.
- Ne pas définir de `hibernate.default_schema` global.
- Déclarer explicitement le schéma dans chaque mapping JPA avec
  `@Table(schema = "...")`.
- Interdire l'accès direct d'un module aux tables d'un autre module, même si la
  `DataSource` et le rôle PostgreSQL sont partagés.

| Module | Schéma | Configuration Flyway | Emplacement des migrations |
| --- | --- | --- | --- |
| `tp-identity` | `tp_identity` | `IdentityFlywayConfiguration` | `classpath:db/migration/identity` |
| `tp-organization` | `tp_organization` | `OrganizationFlywayConfiguration` | `classpath:db/migration/organization` |
| `tp-team` | `tp_team` | `TeamFlywayConfiguration` | `classpath:db/migration/team` |

### Tests d'intégration PostgreSQL

- Utiliser Testcontainers avec l'image `postgres:18-bookworm`, sur un port
  dynamique et sans connexion au Docker Compose local.
- Centraliser le conteneur et les configurations PostgreSQL réutilisables dans
  `tp-test-support`, consommé uniquement avec le scope Maven `test`.
- Importer `PostgreSQLTestConfiguration` pour les tests de contexte qui ont
  seulement besoin de PostgreSQL.
- Importer `PersistenceIntegrationTestConfiguration` pour les tests de
  persistance qui ont également besoin de l'audit déterministe.
- Utiliser `@ServiceConnection` pour fournir la connexion dynamique à Spring
  Boot.
- Charger, dans les tests de module isolés, la configuration Flyway du module
  cible et les substituts explicitement nécessaires à ses dépendances
  publiques.
- Considérer le démarrage réussi du contexte comme preuve de la connexion, de
  l'exécution des migrations et de la validation Hibernate.
- Tester séparément les contraintes et comportements PostgreSQL appartenant à
  TeamPulse.
- Ne pas inspecter directement `flyway_schema_history` et ne pas parser les logs
  pour reproduire les contrôles déjà effectués par Flyway.

La localisation actuelle dans `tp-test-support` ne constitue pas une décision
rétroactive de T02. Elle remplace depuis W001-T04 l'ancienne configuration
Testcontainers locale à `tp-app`, conformément à
[ADR-W001-T04](ADR-W001-T04-multi-tenancy-organization-reference.md).

## Alternatives envisagées

Les alternatives génériques d'environnement local, de structure
PostgreSQL/Flyway et de tests Testcontainers sont évaluées respectivement dans
ADR-TECH-002 et ADR-TECH-003. TeamPulse les adopte sans déviation et ne répète
pas leur analyse dans cet ADR projet.

## Justification

Docker Compose fournit un environnement local stable, explicite et facile à
réinitialiser. Le volume nommé conserve les données sans ajouter de fichiers de
base au repository, tandis que le healthcheck prépare les dépendances futures
au service PostgreSQL.

Les schémas et historiques Flyway indépendants alignent la persistance sur les
frontières de `tp-identity`, `tp-organization` et `tp-team`, tout en conservant
une seule base et une seule connexion applicative. Flyway reste propriétaire du
DDL et Hibernate détecte les divergences au démarrage.

Testcontainers fournit à la suite de tests un PostgreSQL éphémère correspondant
à la version locale. Le support partagé évite de dupliquer l'image, les
identifiants et le wiring Spring Boot sans faire entrer cette infrastructure
dans le runtime ou le modèle Spring Modulith.

## Conséquences positives

- Environnement local reproductible avec une version PostgreSQL explicite.
- Données locales persistantes mais réinitialisables.
- Frontières SQL alignées avec les modules métier.
- Historique et numérotation Flyway indépendants par module.
- Dérive entre migrations et mappings détectée au démarrage.
- Tests sur un PostgreSQL réel et une base initialement vide.
- Tests indépendants du port `5432` et des données locales.
- Infrastructure Testcontainers mutualisée hors du runtime.

## Conséquences négatives / compromis

- Docker est requis pour l'environnement local et les tests d'intégration.
- Plusieurs configurations Flyway doivent être maintenues.
- Chaque entité JPA doit déclarer le schéma correct.
- Une seule base et un seul rôle PostgreSQL ne fournissent pas d'isolation
  physique entre modules.
- La suite complète peut démarrer plusieurs conteneurs PostgreSQL et prendre
  plus de temps que des tests unitaires.
- Le volume local doit être supprimé explicitement pour repartir d'une base
  vide.

## Impact technique

- `docker/docker-compose.yaml`.
- `README.md` pour les commandes locales et les tests.
- `tp-app/src/main/resources/application.yaml`.
- `tp-app/src/main/resources/application-local.yaml`.
- `tp-identity/src/main/java/io/teampulse/identity/config/IdentityFlywayConfiguration.java`.
- `tp-organization/src/main/java/io/teampulse/organization/config/OrganizationFlywayConfiguration.java`.
- `tp-team/src/main/java/io/teampulse/team/config/TeamFlywayConfiguration.java`.
- Migrations et mappings JPA des trois modules métier.
- `tp-test-support` pour le conteneur et les configurations de test réutilisables,
  conformément à ADR-W001-T04.
- Tests de contexte de `tp-app` et tests de persistance des modules métier.

## Validation

- `docker compose -f docker/docker-compose.yaml config` valide la configuration
  Compose.
- `docker compose -f docker/docker-compose.yaml up -d` démarre un conteneur qui
  devient `healthy`.
- `SPRING_PROFILES_ACTIVE=local ./mvnw -pl tp-app spring-boot:run` démarre
  TeamPulse avec la `DataSource` locale.
- Le démarrage sur une base vide exécute les migrations de `tp_identity`,
  `tp_organization` et `tp_team`, puis réussit avec `ddl-auto=validate`.
- `./mvnw -pl tp-app -am test` exécute les tests avec PostgreSQL
  Testcontainers sans nécessiter le Compose local.
- Les tests de persistance vérifient les tables, contraintes et comportements
  SQL possédés par TeamPulse, sans assertions sur les métadonnées internes de
  Flyway.
- Les dépendances de production et le modèle Spring Modulith excluent
  `tp-test-support` et Testcontainers.

## Risques

- Une entité sans schéma explicite pourrait cibler `public`.
- Une migration placée dans le mauvais répertoire pourrait être exécutée par le
  mauvais module.
- Des configurations Flyway dupliquées peuvent diverger si leur convention
  commune n'est pas maintenue.
- Le partage du même rôle PostgreSQL permet techniquement un accès inter-schémas
  que les frontières applicatives doivent interdire.
- Un test lancé sans runtime de conteneurs disponible échoue avant d'exercer le
  comportement applicatif.

## Notes

Si l'API est ajoutée au Compose plus tard, elle utilisera l'URL interne
`jdbc:postgresql://postgres:5432/teampulse` et dépendra du healthcheck
PostgreSQL.

Copier ADR-TECH-002 ou ADR-TECH-003 dans un autre projet ne suffit pas à les y
adopter. Le projet cible doit créer son propre ADR d'adoption avec ses modules,
schémas, versions, chemins, commandes et éventuelles déviations.
