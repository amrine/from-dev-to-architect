# ADR-W001-T04 - Multi-tenancy par référence d'organisation

## Statut

Accepted

## Ticket lié

W001-T04 - Multi-tenancy par référence d'organisation

## Besoin associé

[`docs/besoins/W001/W001-T04-multi-tenancy-organization-reference.md`](../../besoins/W001/W001-T04-multi-tenancy-organization-reference.md)

## ADR techniques adoptés

- [ADR-TECH-006 - Functional references over persistence identifiers](../technical/ADR-TECH-006-functional-references-over-persistence-identifiers.md)
- [ADR-TECH-007 - Monotonic Java reference generation](../technical/ADR-TECH-007-monotonic-java-reference-generation.md)
- [ADR-TECH-008 - Explicit tenant context and tenant-scoped persistence](../technical/ADR-TECH-008-explicit-tenant-context-and-tenant-scoped-persistence.md)
- [ADR-TECH-009 - Module-owned directory contracts and error translation](../technical/ADR-TECH-009-module-owned-directory-contracts-and-error-translation.md)
- [ADR-TECH-010 - JPA-managed technical state and optimistic locking](../technical/ADR-TECH-010-jpa-managed-technical-state-and-optimistic-locking.md)
- [ADR-TECH-011 - Spring application services and layer-owned validation](../technical/ADR-TECH-011-spring-application-services-and-layer-owned-validation.md)
- [ADR-TECH-012 - Module-owned error vocabularies and boundary translation](../technical/ADR-TECH-012-module-owned-error-vocabularies-and-boundary-translation.md)

Cet ADR décide leur adoption par TeamPulse et documente leurs bindings métier,
leurs packages, leurs erreurs et leurs contraintes PostgreSQL concrètes. Les
ADRs techniques restent propriétaires des mécanismes réutilisables.

## Fondations héritées

- [ADR-W001-T01](ADR-W001-T01-backend-multi-module.md) adopte le monolithe
  modulaire Maven/Spring Modulith décrit par ADR-TECH-001.
- [ADR-W001-T02](ADR-W001-T02-docker-compose-local.md) adopte PostgreSQL/Flyway
  par module et Testcontainers décrits par ADR-TECH-002 et ADR-TECH-003.
- [ADR-W001-T03](ADR-W001-T03-regles-architecture-archunit.md) adopte les
  frontières hexagonales et leur enforcement ArchUnit décrits par ADR-TECH-004
  et ADR-TECH-005.

T04 spécialise ces fondations sans en reprendre les décisions génériques.

## Contexte

TeamPulse est un monolithe modulaire composé notamment de `tp-organization`,
`tp-identity` et `tp-team`. La roadmap W001 proposait initialement de propager
l'identifiant numérique de l'organisation et de placer les champs JPA communs
dans une `BaseEntity`.

Cette solution exposerait la persistence interne de `tp-organization`,
affaiblirait l'isolation tenant et compliquerait une future extraction des
modules. TeamPulse doit également fournir des références publiques lisibles et
une frontière tenant explicite alors qu'aucune authentification JWT n'est encore
disponible.

T04 couvre le domaine, l'application, la persistence, les contrats inter-modules
et le provider tenant local. Les contrôleurs et contrats HTTP restent dans
W001-T05 ; l'authentification et les permissions restent dans W008.

## Décision

### 1. Adopter les références fonctionnelles TeamPulse

TeamPulse applique ADR-TECH-006 comme suit :

- `Organization`, `User` et `Team` possèdent une référence fonctionnelle
  textuelle, stable et opaque.
- `TeamMember` ne possède pas de référence fonctionnelle propre. Il reste une
  entité interne adressée par le tenant, l'équipe et l'utilisateur.
- Chaque table conserve un identifiant technique `BIGINT` représenté par un
  `Long`.
- Les identifiants de `Organization` et `User` restent exclusivement dans leurs
  entités JPA.
- `Team.id` et `TeamMember.id` restent visibles dans le domaine de `tp-team`, et
  `TeamMember.teamId` conserve la relation interne au même module. Aucun de ces
  identifiants ne franchit la frontière de `tp-team`.
- `Organization.reference` constitue l'identité fonctionnelle du tenant.
- Les modules consommateurs utilisent `organizationReference` en Java et
  `organization_reference` en PostgreSQL.
- Les colonnes de références utilisent `TEXT` et sont protégées par des
  contraintes `UNIQUE` lorsqu'elles identifient un agrégat.
- Aucun contrat inter-module n'expose un identifiant technique et aucune clé
  étrangère ne cible l'identifiant privé d'un autre module.

### 2. Adopter le générateur monotone de références

TeamPulse adopte sans déviation le format et l'algorithme d'ADR-TECH-007.

- `tp-common` contient les types Java purs `ReferenceFactory`,
  `MonotonicReferenceFactory`, `ReferenceFormat` et `GenerationState`.
- Le package `io.teampulse.common.reference` est exposé par l'interface nommée
  `common::reference`.
- `ReferenceFactory` expose `String generate(String prefix)`.
- `ReferenceFormat.matches(reference, expectedPrefix)` valide la grammaire
  commune sans connaître de concept ou d'erreur métier.
- Les préfixes TeamPulse sont :
  - `ORG` pour `Organization` ;
  - `USR` pour `User` ;
  - `TEM` pour `Team`.
- `TeamMember` n'a aucun préfixe puisqu'il ne possède pas de référence.
- `ReferenceConfiguration` fournit une unique instance applicative de la
  factory et son `Clock`.
- Les modules métier possèdent leurs constantes de préfixe et traduisent un
  format invalide ou une collision dans leur propre vocabulaire.
- Une collision PostgreSQL devient `REFERENCE_GENERATION_FAILED`, sans retry
  automatique de génération/persistence.
- La garantie reste mono-JVM. Une identité de nœud ou une coordination partagée
  devra être décidée avant un déploiement multi-instance Kubernetes.

### 3. Utiliser `Organization.reference` comme racine du tenant

TeamPulse adopte ADR-TECH-008 avec ce binding :

```text
<tenant-root>.reference = Organization.reference
TenantContext.tenantReference = Organization.reference
shared context capability = common::context
```

- `TenantContext` et `TenantContextProvider` sont des contrats Java purs dans
  `io.teampulse.common.context`, exposés par `common::context`.
- `TenantContext` porte uniquement `tenantReference` et la refuse lorsqu'elle
  est nulle ou blanche.
- Les cas d'usage tenantés de `tp-identity` et `tp-team` reçoivent explicitement
  un `TenantContext`.
- Le service extrait une seule fois `tenantReference`, la nomme
  `organizationReference` dans le vocabulaire TeamPulse et la transmet à chaque
  lecture, test d'existence et liste tenantés.
- Avant une mise à jour, le service vérifie que l'agrégat appartient au contexte
  courant. Le port d'écriture reçoit ensuite l'agrégat sans paramètre tenant
  redondant.
- Les ports de repository tenantés n'exposent aucune opération métier globale
  comme `findByReference(reference)` ou `findAll()`.
- Les méthodes génériques héritées de `JpaRepository` restent confinées à
  l'infrastructure et aux fixtures.
- Les opérations plateforme de `tp-organization`, dont la création de la racine
  tenant, restent explicitement non tenantées. Aucun `PlatformContext` vide
  n'est créé.
- `TenantContext` ne contient pas l'acteur. L'acteur, l'authentification et les
  permissions appartiennent à des contrats distincts introduits plus tard.
- PostgreSQL RLS n'est pas activé en W001. L'isolation repose sur les contrats,
  les requêtes tenantées et les contraintes relationnelles décrites plus bas.

Les signatures de repository suivent notamment ces formes :

```text
findByReference(organizationReference, aggregateReference)
findAll(organizationReference)
existsByEmail(organizationReference, email)
update(aggregate)
```

#### Provider tenant local

`tp-app` fournit une stratégie temporaire uniquement pour le profil `local` :

- `LocalTenantContextProvider` implémente `TenantContextProvider` et injecte le
  bean `ReferenceFactory` existant ;
- `LocalTenantConfiguration` enregistre le provider sous `@Profile("local")` ;
- `current()` initialise paresseusement un seul contexte avec
  `ReferenceFactory.generate("ORG")` puis retourne la même instance pendant le
  cycle de vie de l'application ;
- l'initialisation est thread-safe et un échec n'est pas mis en cache ;
- la référence reste en mémoire et change éventuellement après redémarrage ;
- aucun provider de secours n'est créé hors du profil local ;
- aucune organisation artificielle n'est insérée par Flyway.

Les futurs contrôleurs tenantés injecteront `TenantContextProvider`, appelleront
`current()` une seule fois et transmettront exactement le contexte obtenu. Ils
n'accepteront jamais la référence du tenant depuis une donnée libre fournie par
l'appelant.

### 4. Conserver la propriété des modèles métier

#### `tp-organization`

`Organization` est la racine du tenant et contient uniquement :

```text
reference
name
timezone
adminReference
managerReference
status
```

- `OrganizationStatus` contient `CREATING`, `ACTIVE`, `SUSPENDED` et
  `ARCHIVED`.
- Le nom est normalisé avec `strip()`, non blanc, limité à 200 caractères, et
  conserve sa casse et ses espaces internes.
- La timezone est un identifiant IANA validé avec `ZoneId`.
- Une organisation est créée en `CREATING` afin de permettre la création de ses
  futurs responsables avant activation.
- Une organisation `ACTIVE` possède un administrateur et un manager validés
  `AVAILABLE` par `UserDirectory` au moment de l'opération.
- Une organisation `SUSPENDED` conserve obligatoirement son administrateur et
  peut ne plus avoir de manager.
- Retirer le manager d'une organisation `ACTIVE` réalise atomiquement
  `ACTIVE -> SUSPENDED`.
- L'administrateur ne peut jamais être retiré sans remplacement direct.
- Un remplacement de responsable ne change pas le statut et peut désigner la
  même personne pour les deux rôles.
- Affecter un manager en `SUSPENDED` ne réactive pas automatiquement
  l'organisation.
- `ARCHIVED` est terminal et conserve les références historiques présentes.
- Les transitions autorisées sont :

  ```text
  CREATING -> ACTIVE | ARCHIVED
  ACTIVE -> SUSPENDED | ARCHIVED
  SUSPENDED -> ACTIVE | ARCHIVED
  ```

- La disponibilité des responsables est une vérification ponctuelle. T04 ne
  surveille pas continuellement `tp-identity` et ne suspend pas automatiquement
  l'organisation après une évolution utilisateur.

#### `tp-identity`

`User` contient uniquement :

```text
reference
organizationReference
email
firstName
lastName
status
```

- `UserStatus` contient `INVITED`, `CREATING`, `ACTIVE`, `SUSPENDED` et
  `DEACTIVATED`.
- Une création directe commence en `CREATING` ; une invitation commence en
  `INVITED`.
- Les transitions autorisées sont :

  ```text
  INVITED -> CREATING | DEACTIVATED
  CREATING -> ACTIVE | DEACTIVATED
  ACTIVE -> SUSPENDED | DEACTIVATED
  SUSPENDED -> ACTIVE | DEACTIVATED
  ```

- `DEACTIVATED` est terminal.
- L'email est canonisé avec `strip()` puis `toLowerCase(Locale.ROOT)`. Il possède
  exactement un `@`, des parties locale et domaine non vides, aucun espace et
  au plus 254 caractères.
- `firstName` et `lastName` sont normalisés avec `strip()`, restent non blancs,
  sont limités à 100 caractères et conservent leur casse et leurs espaces
  internes.
- L'unicité de l'email canonique est limitée à `organizationReference`.

#### `tp-team` — `Team`

`Team` conserve `id` comme identifiant interne au module et contient :

```text
id
reference
organizationReference
name
status
adminReference
managerReference
```

- Le nom est normalisé avec `strip()`, non blanc, limité à 200 caractères et
  non unique.
- Une équipe possède toujours un administrateur et un manager ; les deux rôles
  peuvent désigner la même personne.
- Les responsables appartiennent au même tenant mais ne deviennent pas
  implicitement `TeamMember`.
- `TeamStatus` contient `ACTIVE`, `SUSPENDED` et `ARCHIVED`.
- La création en `ACTIVE` exige une organisation, un administrateur et un
  manager `AVAILABLE`.
- La réactivation revérifie ces trois disponibilités.
- Un responsable peut être remplacé en `ACTIVE` ou `SUSPENDED` par un
  utilisateur `AVAILABLE` du même tenant, sans changement automatique de
  statut.
- Une responsabilité ne peut jamais être laissée vide.
- `ARCHIVED` est terminal.
- Les transitions autorisées sont :

  ```text
  ACTIVE -> SUSPENDED | ARCHIVED
  SUSPENDED -> ACTIVE | ARCHIVED
  ```

#### `tp-team` — `TeamMember`

`TeamMember` contient :

```text
id
organizationReference
teamId
userReference
status
startedAt
endedAt
```

- `id` et `teamId` restent internes à `tp-team`.
- La table ne stocke ni `teamReference` ni référence fonctionnelle propre au
  membre.
- `TeamMemberEntity` garde `teamId` et `organizationReference` comme champs
  scalaires et ne déclare pas de `@ManyToOne TeamEntity`.
- `TeamMemberStatus` contient `INVITED`, `ACTIVE`, `SUSPENDED` et `REMOVED`.
- Les transitions autorisées sont :

  ```text
  INVITED -> ACTIVE | REMOVED
  ACTIVE -> SUSPENDED | REMOVED
  SUSPENDED -> ACTIVE | REMOVED
  ```

- `REMOVED` est terminal.
- `startedAt` est nul en `INVITED`, initialisé lors d'une activation ou d'une
  création directe en `ACTIVE`, puis conservé pendant les suspensions et
  réactivations.
- `endedAt` est renseigné uniquement lors du passage à `REMOVED`.
- Une invitation accepte un utilisateur `AVAILABLE` ou `PENDING`. Une création
  directe, activation ou réactivation exige `AVAILABLE`.
- La suspension et le retrait restent possibles lorsque l'organisation ou
  l'équipe est indisponible, car ils réduisent ou terminent un accès.
- Une équipe `SUSPENDED` autorise uniquement la suspension et le retrait ; une
  équipe `ARCHIVED` interdit toute mutation de membre.
- Une seule appartenance non terminée est autorisée pour le triplet
  `(organizationReference, teamId, userReference)`.
- Une réinvitation après `REMOVED` crée une nouvelle période et conserve la
  période terminée.
- Aucun changement de statut parent ne réécrit en cascade les statuts enfants.

### 5. Exposer des directories possédés par leurs modules

TeamPulse adopte ADR-TECH-009 avec deux capacités publiques.

#### `identity::user`

`tp-identity` expose `UserDirectory`, `UserAvailability` et
`UserDirectoryException` dans `io.teampulse.identity.api.user`, déclaré avec
`@NamedInterface("user")`.

```text
UserAvailability check(organizationReference, userReference)
```

- Les paramètres sont structurellement `@NotBlank`.
- `AVAILABLE` correspond à `ACTIVE`.
- `PENDING` correspond à `INVITED` ou `CREATING`.
- `UNAVAILABLE` correspond à `SUSPENDED` ou `DEACTIVATED`.
- `NOT_FOUND` couvre l'absence et l'appartenance à un autre tenant afin de ne pas
  révéler l'existence d'un utilisateur externe.
- `UserDirectoryException` signale uniquement une incapacité technique à
  répondre.

`tp-organization` et `tp-team` traduisent ces résultats dans leurs propres
erreurs. Une création, activation, réactivation ou responsabilité active exige
`AVAILABLE`; une invitation de membre accepte également `PENDING`.

#### `organization::organization`

`tp-organization` expose `OrganizationDirectory`, `OrganizationAvailability`
et `OrganizationDirectoryException` dans
`io.teampulse.organization.api.organization`, déclaré avec
`@NamedInterface("organization")`.

```text
OrganizationAvailability check(organizationReference)
```

- `AVAILABLE` correspond à `ACTIVE`.
- `UNAVAILABLE` correspond à `CREATING`, `SUSPENDED` ou `ARCHIVED`.
- `NOT_FOUND` correspond à une référence absente.
- Le directory mappe uniquement le statut persistant ; il ne rappelle pas
  `UserDirectory` pour recalculer les responsables.
- `OrganizationDirectoryException` signale uniquement une défaillance
  technique.

`tp-team` exige `AVAILABLE` pour créer ou réactiver une équipe et pour ajouter,
inviter, activer ou réactiver un membre. Les opérations de fermeture restent
autorisées lorsque l'organisation est `UNAVAILABLE`.

Les deux directories restent des appels Java synchrones internes au monolithe.
Une future extraction pourra fournir un adapter distant sans modifier les cas
d'usage consommateurs.

#### Préparation de W001-T05

`TenantContext` reste limité au tenant. W001-T05 introduira un contrat d'acteur
distinct et consultera les responsabilités actives avant de suspendre ou
désactiver un utilisateur. Cette orchestration d'administration ne sera pas
placée dans `tp-identity`, afin d'éviter un cycle avec ses consommateurs.

Une responsabilité d'organisation est active en `CREATING`, `ACTIVE` ou
`SUSPENDED`. Une responsabilité d'équipe est active en `ACTIVE` ou `SUSPENDED`.
Les références d'un agrégat `ARCHIVED` sont historiques et une simple
appartenance `TeamMember` ne constitue pas une responsabilité bloquante.

### 6. Conserver les erreurs dans leurs modules propriétaires

TeamPulse adopte ADR-TECH-012 avec un vocabulaire par domaine fonctionnel :

- chaque domaine possède une enum de codes stable et une exception non vérifiée
  unique exigeant un code non nul et acceptant une cause ;
- ces types restent indépendants de Spring, JPA et HTTP ;
- `tp-common` ne contient ni catalogue global d'erreurs métier, ni exception
  métier racine imposée aux modules.

`OrganizationErrorCode` contient :

```text
NOT_FOUND
INVALID_NAME
INVALID_TIMEZONE
INVALID_STATUS_TRANSITION
ACTIVATION_REQUIREMENTS_NOT_MET
ADMINISTRATOR_NOT_FOUND
ADMINISTRATOR_NOT_AVAILABLE
MANAGER_NOT_FOUND
MANAGER_NOT_AVAILABLE
USER_DIRECTORY_UNAVAILABLE
REFERENCE_GENERATION_FAILED
CONCURRENT_MODIFICATION
```

`UserErrorCode` contient :

```text
NOT_FOUND
INVALID_EMAIL
INVALID_FIRST_NAME
INVALID_LAST_NAME
EMAIL_ALREADY_USED
INVALID_STATUS_TRANSITION
REFERENCE_GENERATION_FAILED
CONCURRENT_MODIFICATION
```

`TeamErrorCode` contient :

```text
NOT_FOUND
INVALID_NAME
ORGANIZATION_NOT_FOUND
ORGANIZATION_UNAVAILABLE
ORGANIZATION_DIRECTORY_UNAVAILABLE
ADMINISTRATOR_NOT_FOUND
ADMINISTRATOR_NOT_AVAILABLE
MANAGER_NOT_FOUND
MANAGER_NOT_AVAILABLE
USER_DIRECTORY_UNAVAILABLE
INVALID_STATUS_TRANSITION
TEAM_UNAVAILABLE
MEMBER_NOT_FOUND
MEMBER_ALREADY_EXISTS
MEMBER_USER_NOT_FOUND
MEMBER_USER_NOT_AVAILABLE
INVALID_MEMBER_STATUS_TRANSITION
REFERENCE_GENERATION_FAILED
CONCURRENT_MODIFICATION
```

`TeamMember` utilise `TeamErrorCode` puisqu'il appartient à `tp-team`.

Les valeurs attendues des directories sont traduites dans le vocabulaire du
consommateur. Une défaillance technique est enveloppée dans le code
`*_DIRECTORY_UNAVAILABLE` approprié et sa cause publique est conservée. Aucune
exception JPA, Spring ou interne au fournisseur ne franchit l'interface nommée.

Les futurs adapters Web traduiront les codes en contrats HTTP sans renvoyer les
messages internes.

### 7. Isoler les données tenantées dans PostgreSQL

TeamPulse combine ADR-TECH-002, ADR-TECH-006 et ADR-TECH-008 avec les contraintes
suivantes :

- `organizations.reference` est la racine du tenant ; la table ne possède pas
  de colonne `organization_reference` réflexive.
- Toute autre table tenantée porte `organization_reference TEXT NOT NULL`.
- `organizations.name` et `teams.name` utilisent `TEXT NOT NULL` avec
  `char_length(name) BETWEEN 1 AND 200`.
- Le nom d'équipe n'est pas unique.
- Les références fonctionnelles persistées sont uniques.
- Les index de lecture tenantée commencent par `organization_reference` lorsque
  les requêtes suivent ce filtre.
- Les contraintes composites incluent `organization_reference` lorsqu'elles
  empêchent une association entre tenants.
- Les responsables d'une organisation sont physiquement nullables, mais des
  `CHECK` imposent l'administrateur hors `CREATING`/`ARCHIVED` et le manager en
  `ACTIVE`.
- Les responsables d'une équipe sont obligatoires dès son insertion.
- `teams` possède une contrainte unique sur `(id, organization_reference)`.
- `team_members` possède la clé étrangère composite
  `(team_id, organization_reference) -> teams(id, organization_reference)`.
- `team_members` ne possède pas de `ON DELETE CASCADE`.
- Un index unique partiel sur
  `(organization_reference, team_id, user_reference)` limite à une seule
  appartenance en `INVITED`, `ACTIVE` ou `SUSPENDED`.
- Les contraintes temporelles imposent la cohérence de `started_at` et
  `ended_at`, notamment `ended_at >= started_at` lorsque les deux existent.
- Aucun schéma n'ajoute de clé étrangère vers l'identifiant technique d'un autre
  module.
- `UserAvailability` et `OrganizationAvailability` restent des contrôles
  applicatifs et ne sont pas reproduits en SQL.

### 8. Conserver l'état technique dans les entités JPA

TeamPulse adopte ADR-TECH-010 avec les bindings suivants :

- `OrganizationEntity`, `UserEntity`, `TeamEntity` et `TeamMemberEntity` portent
  localement `id`, `version`, `createdAt`, `createdBy`, `modifiedAt` et
  `modifiedBy` selon les besoins de leur domaine.
- `@Version` fournit le verrouillage optimiste ; aucun verrouillage pessimiste
  global n'est introduit.
- Les dates d'audit utilisent `Instant` et les acteurs valent `SYSTEM` en W001.
- Aucune `BaseEntity` ou `@MappedSuperclass` JPA n'entre dans `tp-common`.
- Pour `Organization` et `User`, id, version et audit restent exclusivement dans
  l'entité JPA.
- Pour `Team` et `TeamMember`, les identifiants nécessaires aux relations
  internes restent dans le domaine ; version et audit restent exclusivement
  dans les entités JPA.
- TeamPulse utilise MapStruct pour les mappings de persistence.
- `CommonMapperConfig` est exposé par l'interface nommée `common::mapping`.
- Les mises à jour utilisent `@MappingTarget` sur l'entité gérée et ignorent
  explicitement id, version et audit.
- Les adapters utilisent une opération qui déclenche les violations connues
  dans leur frontière de traduction, notamment `saveAndFlush()` lorsque cette
  exécution immédiate est nécessaire.
- Les violations connues deviennent les codes du module, dont
  `CONCURRENT_MODIFICATION`, `EMAIL_ALREADY_USED` ou
  `REFERENCE_GENERATION_FAILED`. Une violation inconnue reste technique.
- Ces champs décrivent la création et le dernier état ; ils ne constituent pas
  un historique complet.

W012 introduira les premiers événements d'audit persistés pour `Team` et `User`.
Une historisation complète reste une décision distincte.

### 9. Mutualiser l'infrastructure des tests de persistance

T04 concrétise l'option de support partagé prévue par ADR-TECH-003 :

- le module Maven `tp-test-support` porte `TeamPulsePostgreSQLContainer`,
  `PostgreSQLTestConfiguration`, `JpaAuditingTestConfiguration`,
  `PersistenceIntegrationTestConfiguration` et
  `MutableAuditDateTimeProvider` ;
- `PostgreSQLTestConfiguration` sert aux tests de contexte nécessitant seulement
  PostgreSQL ;
- `PersistenceIntegrationTestConfiguration` compose PostgreSQL et l'audit pour
  les tests de persistence ;
- chaque module métier garde son application de test et son
  `AbstractIntegrationTest`, car son package et sa configuration Flyway lui
  appartiennent ;
- `tp-test-support` est consommé uniquement avec le scope Maven `test` ;
- il ne porte pas `@ApplicationModule`, reste exclu du modèle Spring Modulith et
  ne peut être référencé par le code de production ;
- aucune classe Testcontainers n'entre dans `tp-common`.

Cette décision remplace depuis T04 l'ancienne localisation de
`TestcontainersConfiguration` dans `tp-app/src/test` décrite historiquement par
T02. T02 reflète maintenant l'état courant et référence cette extension.

### 10. Appliquer la validation distribuée TeamPulse

TeamPulse adopte ADR-TECH-011 sans élargir son allowlist Spring :

- les classes de `application.service` utilisent uniquement `@Service`,
  `@Validated` et `@Transactional` ;
- toute classe annotée `@Service` porte aussi `@Validated` ;
- `@Transactional` définit la frontière du cas d'usage et `readOnly = true`
  reste limité aux lectures ;
- R11 interdit JPA, Spring Data, `infrastructure` et `config` dans
  `application.service`, limite les types Spring autorisés et impose
  `@Validated` ;
- R10 continue de vérifier le même catalogue R01 à R09 et R11 pour tous les
  modules métier.

La répartition des validations est :

- ports et commandes : contraintes structurelles Jakarta Validation ;
- value objects transverses : invariants à la construction ;
- domaines : normalisation, formats, invariants et transitions ;
- application : règles nécessitant repository, directory, horloge ou autre I/O ;
- PostgreSQL : `NOT NULL`, `CHECK`, `UNIQUE`, clés composites et verrouillage
  optimiste ;
- adapters : traduction des violations connues dans le vocabulaire du module.

Une validation n'entre dans `tp-common` que si elle est identique pour tous ses
consommateurs, indépendante du métier, sans I/O, sans Spring/JPA et sans code
d'erreur de module. `ReferenceFormat` respecte ce contrat. TeamPulse n'ajoute ni
`ValidationUtils`, ni validateur générique par entité.

Les tests suivent la couche propriétaire : domaine sans Spring, orchestration
avec ports substitués, validation/transactions avec bean proxifié, persistence
avec PostgreSQL réel et futur contrat HTTP avec un test Web ciblé. Une
annotation est testée par son effet observable, pas uniquement par réflexion.

### 11. Limiter explicitement le périmètre T04

- T04 ne contient aucun contrôleur, DTO, handler d'erreur HTTP, test Web ou
  parcours HTTP vers PostgreSQL. Ces éléments appartiennent à W001-T05.
- T04 n'authentifie aucun utilisateur et ne résout pas le tenant depuis JWT.
  Ces mécanismes appartiennent à W008.
- T04 ne garantit pas l'unicité absolue des références entre plusieurs JVM.
- T04 ne synchronise pas automatiquement les statuts après une évolution d'un
  responsable dans un autre module.
- T04 ne fournit ni transaction distribuée ni saga.
- T04 ne fournit pas d'historisation complète des modifications.
- La séquence Slidev T04 reste un artefact pédagogique distinct.

## Alternatives envisagées

Les alternatives techniques génériques sont évaluées dans ADR-TECH-006 à
ADR-TECH-012. T04 conserve uniquement les choix propres au domaine TeamPulse.

- **Créer une organisation locale par Flyway** : rejeté, car une migration
  structurelle ne doit pas créer une donnée métier temporaire.
- **Exiger les responsables dès l'insertion de l'organisation** : rejeté, car
  les utilisateurs responsables ont eux-mêmes besoin de la référence de
  l'organisation. `CREATING` résout cette initialisation circulaire.
- **Recalculer l'organisation via `UserDirectory` à chaque consultation** :
  rejeté, car cela ajouterait un couplage synchrone sans garantie transactionnelle
  distribuée. `OrganizationDirectory` mappe son statut persistant.
- **Synchroniser immédiatement tous les changements utilisateur** : reporté.
  Un futur événement, son idempotence et sa gestion des courses forment une
  décision séparée.
- **Revérifier tous les responsables pour chaque opération de membre** : rejeté.
  Les validations sont ponctuelles aux opérations qui ouvrent ou rétablissent
  un accès.
- **Retirer un responsable d'équipe sans remplacement** : rejeté ; le
  remplacement est atomique afin de conserver l'agrégat valide.
- **Ne pas stocker `organizationReference` dans `TeamMember`** : rejeté, car la
  duplication contrôlée permet des filtres, index et contraintes composites
  tenantés.
- **Stocker `teamReference` dans `TeamMember`** : rejeté ; `teamId` reste une
  relation interne plus directe dans le même module.
- **Donner une référence fonctionnelle à `TeamMember`** : rejeté en l'absence de
  besoin d'adressage externe indépendant.
- **Mapper `TeamMemberEntity` avec `@ManyToOne TeamEntity`** : rejeté pour éviter
  les chargements et cascades implicites ; la clé étrangère composite suffit.
- **Ajouter `userReference` à `TenantContext`** : rejeté, car tenant et acteur
  sont deux responsabilités différentes.
- **Ajouter un `PlatformContext` vide** : rejeté tant qu'il ne porte aucune
  identité ou permission fiable.

## Justification

`Organization.reference` fournit une identité tenant uniforme sans exposer la
clé primaire de `tp-organization`. Le contexte explicite rend l'oubli du tenant
visible dans les signatures et les tests, tout en préparant une future
résolution JWT indépendante des cas d'usage.

Les directories conservent la propriété des informations dans leurs modules et
permettent à `tp-organization` et `tp-team` de vérifier ponctuellement les
préconditions sans dépendre des domaines ou entités JPA externes.

Les contraintes PostgreSQL renforcent les invariants tenantés exprimables
localement, tandis que les domaines et services possèdent les transitions et
les règles inter-modules. L'état JPA, l'audit et la concurrence restent hors des
modèles métier sauf lorsqu'un identifiant a un sens interne démontré.

## Conséquences positives

- Les identifiants techniques ne franchissent pas les frontières de modules.
- Le tenant est explicite dans les cas d'usage et les repositories.
- Les modèles métier restent indépendants de Spring et JPA.
- Les contrats `identity::user` et `organization::organization` sont étroits et
  transport-indépendants.
- Chaque module conserve son vocabulaire d'erreur.
- PostgreSQL protège l'isolation, les contraintes temporelles, l'unicité et les
  mises à jour concurrentes.
- Le support de test est mutualisé sans entrer dans le runtime.
- Les validations et leurs preuves ont un propriétaire explicite.

## Conséquences négatives / compromis

- Les références et `organizationReference` augmentent la taille des index et le
  nombre de paramètres propagés.
- La génération reste probabiliste après redémarrage ou entre plusieurs JVM.
- Les mappings d'id, version et audit sont volontairement répétés entre modules.
- Le contexte local change éventuellement après redémarrage et ne constitue pas
  une sécurité.
- Un responsable peut devenir indisponible après sa validation ponctuelle.
- L'absence de clé étrangère inter-module impose des contrôles applicatifs et
  des tests de contrat.
- Les appels synchrones aux directories ajoutent des chemins de défaillance.
- Les beans instanciés directement ne déclenchent ni validation de méthode ni
  transaction Spring.
- L'audit `SYSTEM` ne fournit pas encore l'auteur réel ni l'historique complet.

## Impact technique

### `tp-common`

- `TenantContext` et `TenantContextProvider` dans `common::context`.
- `ReferenceFactory`, `MonotonicReferenceFactory`, `ReferenceFormat` et
  `GenerationState` dans `common::reference`.
- `CommonMapperConfig` dans `common::mapping`.
- `ConstraintNameExtractor` dans `common::persistence`.

### `tp-test-support`

- Conteneur PostgreSQL 18, configurations de test PostgreSQL/audit et provider
  temporel mutable.
- Dépendances Spring Boot Test et Testcontainers confinées au support de test.

### `tp-organization`

- Domaine, cycle de vie et persistence de `Organization`.
- Ports et services de cycle de vie et de responsables.
- Consommation de `identity::user`.
- Publication de `organization::organization`.
- Schéma `tp_organization` et migration `V0.1.0__create_organizations.sql`.

### `tp-identity`

- Domaine, cycle de vie et persistence tenantée de `User`.
- Services applicatifs et repositories toujours filtrés par organisation.
- Publication de `identity::user`.
- Schéma `tp_identity` et migration `V0.1.0__create_users.sql`.

### `tp-team`

- Domaines, cycles de vie, persistence et tests de `Team` et `TeamMember`.
- Consommation de `identity::user` et `organization::organization`.
- Contraintes composites et périodes d'appartenance.
- Schéma `tp_team` et migrations `V0.1.0__create_teams.sql` et
  `V0.1.1__create_team_members.sql`.

### `tp-app`

- Wiring de la factory, de l'horloge et des contrats inter-modules.
- `LocalTenantContextProvider` et `LocalTenantConfiguration` sous profil local.
- Dépendance `tp-test-support` en scope `test`.
- Exclusion de `io.teampulse.testsupport` du modèle Spring Modulith.

## Validation

### Gate de clôture historique du 16 septembre 2026

Les commandes suivantes ont réussi depuis la racine du dépôt :

```bash
./mvnw --batch-mode --no-transfer-progress -pl tp-identity verify
./mvnw --batch-mode --no-transfer-progress -pl tp-organization verify
./mvnw --batch-mode --no-transfer-progress -pl tp-team verify
./mvnw --batch-mode --no-transfer-progress verify
```

Cette exécution historique a produit :

- `tp-identity` : 102 tests, zéro échec et zéro erreur ;
- `tp-organization` : 217 tests, zéro échec et zéro erreur ;
- `tp-team` : 152 tests, zéro échec et zéro erreur ;
- réacteur complet : 576 tests, zéro échec et zéro erreur ;
- PostgreSQL 18.4 via Testcontainers avec migrations Flyway ;
- `git diff --check 5fda261b^..HEAD` réussi.

Ces nombres constituent une preuve datée et ne sont pas un invariant durable de
la suite.

### Catégories de preuves

- **Références et tenant** : format, monotonie mono-JVM, concurrence,
  `TenantContext`, provider local et wiring Spring.
- **Architecture** : ports tenantés, interfaces nommées, Spring Modulith,
  règles R01 à R11 et exclusion de `tp-test-support`.
- **Persistence PostgreSQL** : migrations, mappings, isolation A/B, contraintes,
  audit `SYSTEM` et verrouillage optimiste dans les quatre adapters JPA.
- **Comportement inter-module** : disponibilités, erreurs
  `*_DIRECTORY_UNAVAILABLE` et causes conservées.
- **Collision sans retry** : une seule tentative d'écriture et conservation de
  la donnée existante par PostgreSQL.

Les contrôleurs, DTO, erreurs HTTP, tests Web et parcours HTTP vers PostgreSQL
ne font pas partie de cette validation.

## Risques

- Une méthode de repository ajoutée sans tenant pourrait contourner la
  convention.
- Plusieurs instances de la factory fragmenteraient son état monotone.
- Plusieurs JVM peuvent réutiliser un nonce compatible ; la stratégie doit
  évoluer avant Kubernetes.
- Une dépendance inter-module oubliée pourrait laisser fuiter un type interne ou
  une exception technique.
- Un module peut oublier de traduire une contrainte PostgreSQL connue.
- Une organisation peut rester en `CREATING` après un provisioning incomplet.
- Une disponibilité validée peut devenir obsolète après la transaction.
- Le provider local pourrait être activé par erreur hors développement si le
  profil est mal configuré.
- Une modification de scope Maven ou d'exclusion Modulith pourrait faire entrer
  `tp-test-support` dans l'architecture de production.

## Notes

- W001-T05 ajoute les parcours HTTP et le contrôle d'un acteur applicatif sans
  modifier `TenantContext`.
- W008 remplace le provider local par une résolution authentifiée du tenant et
  des permissions.
- La coordination multi-nœud et l'identité de nœud sont reportées avant le
  déploiement Kubernetes.
- Un futur événement de disponibilité pourra servir à la réconciliation ou à
  l'observabilité, mais ne modifiera pas automatiquement les agrégats sans une
  décision d'idempotence et de gestion des courses.
- W012 introduira les premiers événements d'audit persistés ; l'historisation
  complète reste hors périmètre.
- La séquence Slidev T04 est un artefact de clôture distinct.
- Copier un ADR technique dans un autre projet ne suffit pas à l'adopter : le
  projet cible doit créer son propre ADR avec ses agrégats, tenant root,
  préfixes, packages, contraintes, erreurs et déviations.
