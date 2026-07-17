# ADR-W001-T02 - PostgreSQL local et schemas Flyway par module

## Statut
Accepted

## Ticket lie
W001-T02 - PostgreSQL local et schemas Flyway

## Besoin associe
`docs/besoins/W001/W001-T02-PostgreSQL-local-et-schemas-Flyway.md`

## Contexte
TeamPulse doit fournir un environnement PostgreSQL local simple a demarrer, mais aussi garantir que la structure SQL est reproductible et conforme aux frontieres du monolithe modulaire.

Une configuration Flyway globale donnerait un historique commun et obligerait les modules a coordonner leurs numeros de migration. A l'inverse, plusieurs bases de donnees alourdiraient inutilement le developpement local a ce stade.

Les tests d'integration doivent utiliser PostgreSQL sans dependre de l'etat du conteneur Docker Compose du developpeur.

## Decision
- Utiliser l'image officielle `postgres:18-bookworm`.
- Exposer PostgreSQL localement sur le port `5432`.
- Creer la base et l'utilisateur locaux `teampulse`.
- Utiliser un volume Docker nomme pour conserver les donnees.
- Ajouter un healthcheck base sur `pg_isready`.
- Activer la configuration locale explicitement avec `SPRING_PROFILES_ACTIVE=local`.
- Permettre de surcharger la connexion avec `TEAM_PULSE_DB_URL`, `TEAM_PULSE_DB_USERNAME` et `TEAM_PULSE_DB_PASSWORD`.
- Utiliser une seule `DataSource` Spring Boot portee par `tp-app`.
- Desactiver l'auto-configuration Flyway unique avec `spring.flyway.enabled=false`.
- Faire porter a chaque module metier sa configuration Flyway, son emplacement de migrations et son historique.
- Utiliser les schemas `tp_organization`, `tp_identity` et `tp_team`.
- Configurer chaque instance Flyway avec son schema comme `defaultSchema` et sa propre table `flyway_schema_history`.
- Utiliser `createSchemas(true)` pour creer le schema avant sa table d'historique.
- Ne pas ajouter de migration SQL vide ou redondante uniquement pour executer `CREATE SCHEMA`.
- Commencer la numerotation versionnee d'un module avec sa premiere modification SQL metier.
- Configurer Hibernate avec `spring.jpa.hibernate.ddl-auto=validate`.
- Ne pas configurer de `hibernate.default_schema` global.
- Faire declarer explicitement le schema aux futures entites JPA avec `@Table(schema = "...")`.
- Utiliser Testcontainers pour les tests d'integration.
- Conserver `TestcontainersConfiguration` dans `tp-app/src/test` et l'importer explicitement dans les tests qui ont besoin de PostgreSQL.
- Utiliser des tests de demarrage de contexte pour valider la connexion, l'execution de Flyway et la validation Hibernate.
- Ne pas dupliquer la validation de Flyway avec des assertions SQL sur la presence des schemas ou des tables d'historique tant qu'aucune migration metier n'existe.

## Configuration Docker Compose cible
```yaml
services:
  postgres:
    image: postgres:18-bookworm
    container_name: teampulse-postgres
    environment:
      POSTGRES_DB: teampulse
      POSTGRES_USER: teampulse
      POSTGRES_PASSWORD: teampulse
    ports:
      - "5432:5432"
    volumes:
      - teampulse-postgres-data:/var/lib/postgresql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U teampulse -d teampulse"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  teampulse-postgres-data:
```

## Repartition des responsabilites
```text
tp-app
  DataSource locale
  driver PostgreSQL
  TestcontainersConfiguration dans les sources de test

tp-identity
  IdentityFlywayConfiguration
  classpath:db/migration/identity
  tp_identity.flyway_schema_history

tp-organization
  OrganizationFlywayConfiguration
  classpath:db/migration/organization
  tp_organization.flyway_schema_history

tp-team
  TeamFlywayConfiguration
  classpath:db/migration/team
  tp_team.flyway_schema_history
```

Un test `@ApplicationModuleTest` charge la configuration Testcontainers comme infrastructure de test et uniquement la configuration Flyway du module cible. Cet import de test ne cree pas de dependance metier entre le module et `tp-app`.

Le demarrage reussi du contexte constitue le resultat attendu du test : une erreur de connexion, de migration Flyway ou de validation Hibernate fait echouer l'initialisation Spring. Des assertions SQL detaillees seront ajoutees avec les premieres migrations metier, lorsqu'elles permettront de verifier un contrat SQL reel plutot que de reproduire le fonctionnement interne de Flyway.

## Alternatives envisagees
- Lancer PostgreSQL localement sans Docker.
- Utiliser Testcontainers aussi pour le developpement interactif.
- Utiliser `postgres:latest` ou `postgres:alpine`.
- Utiliser un bind mount local pour les donnees PostgreSQL.
- Laisser Hibernate creer ou modifier les tables.
- Utiliser Liquibase.
- Utiliser un seul schema et un seul historique Flyway.
- Utiliser une base de donnees separee par module.
- Creer les schemas avec des migrations SQL redondantes.
- Verifier immediatement les schemas et les tables `flyway_schema_history` avec des assertions SQL dans chaque test de module.

## Justification
Docker Compose fournit un environnement local stable et reproductible. Le volume nomme preserve les donnees sans polluer le repository et le healthcheck prepare l'ajout futur de services dependants.

Un schema et un historique Flyway par module renforcent l'isolation logique tout en conservant une seule base et une seule `DataSource`. Chaque module peut faire evoluer ses migrations et reutiliser cette configuration lors d'une future extraction en microservice.

Testcontainers donne a chaque test une base PostgreSQL ephemere et propre. Les tests ne dependent donc ni des donnees locales ni du port `5432`.

Les tests de contexte restent volontairement simples. Flyway et Hibernate sont deja responsables de faire echouer le demarrage lorsque la base n'est pas initialisable ou incompatible. Ajouter des assertions SQL sur leurs mecanismes internes a ce stade augmenterait la maintenance sans couvrir de regle metier supplementaire.

## Consequences positives
- Environnement local simple a lancer.
- Version PostgreSQL explicite.
- Donnees locales persistantes.
- Frontieres SQL alignees avec les modules metier.
- Historique Flyway independant par module.
- Numerotation des migrations independante.
- Tests reproductibles sur une base vide.
- Test d'un module isole de la persistance des autres modules.
- Tests d'infrastructure courts, lisibles et centres sur le demarrage reel de l'application.
- Extraction future d'un module facilitee.

## Consequences negatives / compromis
- Docker est requis pour le developpement local et les tests d'integration.
- Plusieurs configurations Flyway doivent etre maintenues.
- Les entites JPA doivent declarer explicitement leur schema.
- Les requetes SQL directes entre schemas doivent etre evitees.
- La suite complete demarre plusieurs conteneurs PostgreSQL et prend plus de temps.
- Les tests actuels ne comparent pas explicitement la liste des schemas presents en base.
- Le volume local doit etre supprime explicitement pour repartir d'une base vide.

## Impact technique
- `docker/docker-compose.yaml`.
- `README.md` pour les commandes locales et les tests.
- `tp-app/src/main/resources/application.yaml` pour desactiver Flyway globalement et valider le schema JPA.
- `tp-app/src/main/resources/application-local.yaml` pour la `DataSource` locale.
- `tp-app/pom.xml` pour le driver PostgreSQL et les dependances Testcontainers de test.
- `tp-app/src/test/java/io/teampulse/TestcontainersConfiguration.java`.
- Configurations Flyway, dependances JPA/Flyway et migrations dans les modules `tp-identity`, `tp-organization` et `tp-team`.

## Validation
- `docker compose -f docker/docker-compose.yaml up -d` demarre PostgreSQL.
- Le conteneur devient `healthy`.
- `SPRING_PROFILES_ACTIVE=local ./mvnw -pl tp-app spring-boot:run` demarre l'application.
- Une base vide contient `tp_identity`, `tp_organization` et `tp_team`.
- Chaque schema contient sa table `flyway_schema_history`.
- `./mvnw clean -pl tp-app -am test` reussit avec PostgreSQL Testcontainers.
- Les logs de chaque `@ApplicationModuleTest` confirment le chargement de la seule configuration Flyway du module cible.
- Le test de contexte complet execute les trois configurations Flyway.
- Le demarrage avec `ddl-auto=validate` reussit.

## Risques
- Une entite sans schema explicite pourrait cibler `public`.
- Une migration placee dans le mauvais repertoire pourrait appartenir au mauvais module.
- Une configuration Flyway dupliquee sans convention commune pourrait diverger.
- Une regression chargeant une configuration Flyway supplementaire pourrait ne pas etre detectee sans assertion d'isolation dediee ; ce risque est accepte tant qu'aucune migration metier n'existe.
- Un test lance sans Docker disponible echouera au demarrage de Testcontainers.

## Notes
Si l'API est ajoutee au Compose plus tard, elle utilisera l'URL interne `jdbc:postgresql://postgres:5432/teampulse` et dependra du healthcheck PostgreSQL.
