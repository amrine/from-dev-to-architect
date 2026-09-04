# ADR-W001-T04 - Multi-tenancy par référence d'organisation

## Statut

Draft

## Ticket lié

W001-T04 - Multi-tenancy par référence d'organisation

## Besoin associé

[`docs/besoins/W001/W001-T04-multi-tenancy-organization-reference.md`](../../besoins/W001/W001-T04-multi-tenancy-organization-reference.md)

## Contexte

TeamPulse est actuellement un monolithe modulaire composé notamment de
`tp-organization`, `tp-identity` et `tp-team`. La roadmap W001 proposait
initialement de propager l'identifiant numérique de l'organisation et de placer
des champs JPA communs dans une `BaseEntity`.

Cette solution exposerait l'identifiant de persistance du module organisation à
tous les autres modules. Elle rendrait les contrats inter-modules dépendants du
schéma interne de `tp-organization` et compliquerait une future extraction en
services indépendants.

Le ticket doit également préparer l'isolation tenant alors qu'aucune sécurité
JWT n'est encore disponible. La frontière doit donc être explicite dans les
contrats et les requêtes sans laisser croire que W001 authentifie ou autorise
réellement un utilisateur.

Enfin, TeamPulse a besoin de références publiques lisibles. Leur génération doit
être effectuée côté Java, sans séquence métier en base, tout en supportant les
appels concurrents d'une instance. La coordination de plusieurs nœuds n'est pas
encore introduite et devra être revue avant Kubernetes.

## Décision

### 1. Séparer identifiant technique et référence fonctionnelle

- Chaque table conserve un identifiant technique `BIGINT`, représenté par un
  `Long` en Java.
- Cet identifiant est strictement interne à son module et à sa persistance.
- Chaque agrégat échangé entre modules possède une référence fonctionnelle
  textuelle, stable et opaque.
- `Organization.reference` devient l'identifiant du tenant dans les contrats.
- Les modules consommateurs utilisent le nom `organizationReference` en Java et
  la colonne `organization_ref` en PostgreSQL.
- Aucun nouveau contrat n'utilise un identifiant technique d'organisation pour
  représenter le tenant.
- Les colonnes de référence utilisent `TEXT`. Les règles de format et de
  longueur restent des invariants Java afin de pouvoir faire évoluer le format
  sans migration motivée uniquement par une taille de `VARCHAR`.

### 2. Générer les références dans `tp-common`

Le générateur est un composant Java pur et transverse placé dans un package
`reference` de `tp-common`. Il ne dépend ni de Spring, ni de JPA, ni d'un module
métier.

Le contrat public s'appelle `ReferenceFactory` et expose une seule opération :

```java
String generate(String prefix);
```

L'implémentation s'appelle `MonotonicReferenceFactory`. Ce vocabulaire est
propre à TeamPulse. Le nom ne porte ni le suffixe technique `Impl`, ni la
propriété interne « lock-free ».

L'API reçoit directement une `String` et n'introduit ni `ReferenceKind`, ni
`ReferencePrefix`. `MonotonicReferenceFactory` vérifie que le préfixe n'est pas
nul et respecte exactement l'expression `[A-Z]{3}`. Elle ne supprime pas les
espaces et ne convertit pas les minuscules : toute valeur non conforme provoque
une `IllegalArgumentException`.

Les préfixes normatifs sont :

- `ORG` pour `Organization` ;
- `USR` pour `User` ;
- `TEM` pour `Team`.

`TeamMember` ne possède pas de référence fonctionnelle propre. Chaque module
métier conserve son préfixe dans une constante locale et appelle
`ReferenceFactory.generate(...)`. Ainsi, `tp-common` reste générique et ne
dépend d'aucun catalogue de concepts métier.

Le format retenu est :

```text
<TRIGRAMME>-<ANNÉE>-<JOUR_MOIS>-<SUFFIXE>
```

Exemple illustratif :

```text
ORG-2026-0908-00000ZA7B90B
```

- `TRIGRAMME` identifie la famille de référence.
- `ANNÉE` utilise l'année UTC de l'instant logique accepté.
- `JOUR_MOIS` utilise le format UTC `JJMM`, par exemple `0508` pour le 5 août.
- `SUFFIXE` contient exactement douze caractères alphanumériques en majuscules et
  utilise l'alphabet base 36 `[0-9A-Z]`.
- Les séparateurs ne font pas partie des douze caractères du suffixe.
- La référence complète contient exactement 26 caractères, séparateurs inclus.

Le suffixe est découpé de manière fixe :

```text
MMMMMMNNNNCC
│     │   │
│     │   └── compteur atomique : 2 caractères base 36
│     └────── nonce JVM          : 4 caractères base 36
└──────────── millisecondes UTC  : 6 caractères base 36
```

- `MMMMMM` encode les millisecondes du temps logique écoulées depuis le début de
  sa journée UTC. `36^6 = 2 176 782 336` couvre les `86 400 000` millisecondes
  d'une journée sans époque TeamPulse fixe ni limite pluriannuelle.
- `NNNN` encode un nonce généré une seule fois au démarrage de la factory avec
  `SecureRandom`. `36^4` fournit 1 679 616 valeurs possibles.
- `CC` encode le compteur de la milliseconde logique, de `00` à `ZZ` ; `36^2`
  fournit exactement 1 296 valeurs.
- Les valeurs temporelles et le compteur sont complétés à gauche par `0` afin
  de conserver douze caractères. Le nonce reste stable pendant le cycle de vie
  de la factory.

Exemple simplifié avec un temps logique égal à 35 millisecondes après le début
du 9 août 2026 UTC, le nonce `A7B9` et le compteur décimal 11 :

```text
35 en base 36 -> Z  -> 00000Z
nonce JVM           -> A7B9
11 en base 36 -> B  -> 0B
suffixe             -> 00000ZA7B90B
référence           -> ORG-2026-0908-00000ZA7B90B
```

Le format exact des tokens est centralisé dans la factory ; aucun module métier
ne concatène lui-même les composants.

### 3. Utiliser un algorithme monotone et non bloquant dans une JVM

- Une `java.time.Clock` est injectée dans la factory.
- Un nonce base 36 de quatre caractères est tiré avec `SecureRandom` une seule
  fois par cycle de vie de la factory. Sa source est contrôlable dans les tests.
- Un état atomique conserve le dernier temps logique et le compteur associé.
- Une boucle compare-and-set produit l'état suivant sans verrou `synchronized`.
- Lorsque l'horloge avance, le nouveau temps devient le temps logique et le
  compteur repart de sa valeur initiale.
- Lorsque plusieurs références sont demandées dans la même milliseconde, le
  compteur est incrémenté de `00` à `ZZ`.
- Lorsque l'horloge système recule, le générateur conserve le dernier temps
  logique connu et continue d'incrémenter le compteur.
- L'année, `JOUR_MOIS` et la partie temporelle du suffixe sont calculés depuis
  ce même instant logique, et non séparément depuis l'horloge brute.
- Après `ZZ`, la génération suivante avance le temps logique d'une milliseconde
  et reprend le compteur à `00` sans attendre l'horloge réelle.

Une instance unique de la factory dans l'application garantit l'unicité des
valeurs qu'elle émet pendant son cycle de vie dans la JVM, y compris lors
d'appels concurrents et d'un recul de l'horloge. L'état conserve un instant
logique absolu ; l'année, `JOUR_MOIS` et les millisecondes dans la journée sont
toujours dérivés de ce même instant. La factory peut donc fonctionner au-delà
d'un changement de jour ou d'année sans épuisement du segment temporel.

Un redémarrage supprime l'état atomique et produit un nouveau nonce. Dans le
pire cas où une nouvelle factory réutilise la même date, la même milliseconde
logique et le même compteur, la probabilité de reprendre aussi le même nonce est
`1 / 36^4`, soit `1 / 1 679 616` ou environ `0,0000595 %`. La probabilité de ne
pas entrer en collision dans ce cas est donc d'environ `99,9999405 %`.

La contrainte `UNIQUE` de PostgreSQL reste l'ultime défense d'intégrité pour les
références persistées. Une collision est une anomalie exceptionnelle : elle ne
déclenche pas une boucle de régénération et de persistance. L'insertion échoue
clairement avec le code `REFERENCE_GENERATION_FAILED` du module concerné, sans
remplacer la donnée existante.

Cette combinaison ne constitue pas encore une preuve d'unicité absolue entre
plusieurs JVM. Une identité de nœud stable ou une coordination partagée sera
nécessaire avant un déploiement multi-instance. Ce point est volontairement un
TODO d'architecture et ne doit pas être masqué dans l'implémentation.

### 4. Modéliser explicitement le contexte tenant

Un tenant est le périmètre logique et isolé d'un client dans une application
multi-tenant. Les données et opérations d'un tenant ne doivent jamais être
mélangées avec celles d'un autre. Dans TeamPulse W001, chaque organisation
constitue un tenant et `Organization.reference` en est l'identité concrète.

- `TenantContext` est un type Java pur dont l'unique propriété obligatoire
  s'appelle `tenantReference`.
- `TenantContext` est exposé aux modules métier par l'interface nommée
  `common::context`. Chaque module consommateur déclare explicitement cette
  dépendance sans ouvrir les autres packages internes de `tp-common`.
- Le nom `tenantReference` reste générique afin que le contexte ne dépende pas
  du modèle `Organization`. En W001, sa valeur est `Organization.reference`.
- Les cas d'usage tenantés reçoivent explicitement un `TenantContext` valide.
- Les services applicatifs extraient `tenantReference`, la nomment
  `organizationReference` dans le vocabulaire métier de TeamPulse et la
  transmettent explicitement aux ports sortants.
- Les repositories tenantés exigent `organizationReference` dans leurs méthodes
  de recherche, d'existence, de liste, de modification et de suppression.
- Aucune méthode métier globale telle que `findByReference(userReference)` ou
  `findAll()` n'est exposée par un repository tenanté.
- Les cas d'usage plateforme restent non tenantés. Aucun `PlatformContext` vide
  n'est introduit en W001, puisqu'il ne transporterait encore aucune identité
  authentifiée ni information fiable.

Exemples de formes attendues :

```text
findByReference(organizationReference, userReference)
findAll(organizationReference)
existsByEmail(organizationReference, email)
delete(organizationReference, userReference)
```

Cette décision exprime une frontière applicative. `TenantContext` ne prouve pas
encore que l'appel HTTP provient d'un utilisateur de l'organisation : il rend
seulement obligatoire la présence du périmètre tenant dans le contrat. L'identité
réelle, le claim JWT et les permissions seront traités avec la sécurité de W008.

### 5. Fournir un contexte d'organisation local sans donnée Flyway artificielle

Pour les flux HTTP locaux de W001, un service singleton fournit la référence
d'organisation courante :

- au premier appel, il demande une référence `ORG` à la factory ;
- il conserve cette référence en mémoire ;
- tous les appels suivants de la même exécution reçoivent la même valeur ;
- le flux local utilise cette valeur comme `tenantReference` du
  `TenantContext` ;
- après redémarrage, une nouvelle référence peut être générée ;
- le contrôleur ne recherche pas l'organisation directement dans le module
  `tp-organization` ;
- aucune ligne d'organisation temporaire n'est insérée par Flyway uniquement
  pour simuler le tenant courant.

Les tests d'intégration créent explicitement leurs propres organisations lorsque
leur scénario requiert une ligne persistée. Le service local n'est pas activé
comme mécanisme de tenant en production.

### 6. Garder la propriété des modèles dans leurs modules

#### `tp-organization`

`Organization` est la racine du tenant. Son modèle minimal contient :

```text
id
reference
name
status
timezone
adminReference
managerReference
version
createdAt
createdBy
modifiedAt
modifiedBy
```

- `OrganizationStatus` contient `CREATING`, `ACTIVE`, `SUSPENDED` et `ARCHIVED`.
- `CREATING` remplace `PROVISIONING`.
- `timezone` est validée avec `ZoneId` et stocke un identifiant IANA.
- Une organisation est d'abord persistée en `CREATING`. Ses références
  d'administrateur et de manager peuvent alors être absentes.
- La transition `CREATING -> ACTIVE` exige les deux responsables, un nom et une
  timezone valides. Les responsables doivent être retournés `AVAILABLE` par
  `UserDirectory` pour cette organisation.
- Une organisation sortie de `CREATING` possède exactement un administrateur et
  un manager.
- Les deux références peuvent désigner la même personne.
- Les transitions autorisées sont `CREATING -> ACTIVE`,
  `CREATING -> ARCHIVED`, `ACTIVE -> SUSPENDED`, `ACTIVE -> ARCHIVED`,
  `SUSPENDED -> ACTIVE` et `SUSPENDED -> ARCHIVED`.
- `ARCHIVED` est terminal.
- Les adresses et moyens de contact seront ajoutés dans des tables annexes et ne
  gonflent pas la table principale.

#### `tp-identity`

Le modèle de domaine `User` contient uniquement :

```text
reference
organizationReference
email
firstName
lastName
status
```

L'entité JPA `UserEntity` complète cet état métier avec :

```text
id
version
createdAt
createdBy
modifiedAt
modifiedBy
```

- `UserStatus` contient `INVITED`, `CREATING`, `ACTIVE`, `SUSPENDED` et
  `DEACTIVATED`.
- Une création directe commence en `CREATING` et une invitation en `INVITED`.
- Les transitions autorisées sont `INVITED -> CREATING`,
  `INVITED -> DEACTIVATED`, `CREATING -> ACTIVE`,
  `CREATING -> DEACTIVATED`, `ACTIVE -> SUSPENDED`,
  `ACTIVE -> DEACTIVATED`, `SUSPENDED -> ACTIVE` et
  `SUSPENDED -> DEACTIVATED`.
- `DEACTIVATED` est terminal.
- L'email est canonisé par suppression des espaces périphériques et passage en
  minuscules avec `Locale.ROOT`. Il contient exactement un `@`, une partie
  locale et un domaine non vides, aucun espace et au plus 254 caractères.
- `firstName` et `lastName` sont débarrassés de leurs espaces périphériques,
  restent non blancs et sont limités chacun à 100 caractères. Leur casse et
  leurs espaces internes sont préservés.
- L'unicité fonctionnelle de l'email est évaluée dans le périmètre de
  `organizationReference`, sur sa forme canonique.

#### `tp-team`

`Team` contient au minimum :

```text
id
reference
organizationReference
name
status
adminReference
managerReference
version
createdAt
createdBy
modifiedAt
modifiedBy
```

- Une équipe possède exactement un administrateur et un manager.
- Les deux références peuvent désigner le même utilisateur.
- L'administrateur et le manager appartiennent à la même organisation que
  l'équipe.
- Les responsabilités ne créent aucune appartenance implicite. Chaque
  responsable peut posséder ou non une ligne `TeamMember`, indépendamment de sa
  référence portée par `Team`.
- `TeamStatus` contient `ACTIVE`, `SUSPENDED` et `ARCHIVED`.
- Une équipe est créée en `ACTIVE` lorsque ses responsables sont retournés
  `AVAILABLE` par `UserDirectory`.
- Les transitions autorisées sont `ACTIVE -> SUSPENDED`,
  `ACTIVE -> ARCHIVED`, `SUSPENDED -> ACTIVE` et `SUSPENDED -> ARCHIVED`.
- `ARCHIVED` est terminal.

`TeamMember` stocke `teamId`, `userReference` et `organizationReference`.
`teamId` est l'identifiant technique de `Team` et reste interne à `tp-team`.
`userReference` franchit la frontière avec `tp-identity` sans exposer
l'identifiant technique de l'utilisateur. La duplication contrôlée de la
référence d'organisation permet de filtrer et de contraindre directement chaque
requête tenantée. Une contrainte interdit de rattacher deux fois simultanément le
même utilisateur à la même équipe dans une organisation.

Son modèle minimal contient :

```text
id
organizationReference
teamId
userReference
status
startedAt
endedAt
version
createdAt
createdBy
modifiedAt
modifiedBy
```

`TeamMember` ne possède pas de référence fonctionnelle propre. Il est une entité
interne de l'équipe et les contrats externes l'adressent avec `teamReference` et
`userReference` dans le contexte de l'organisation.

La clé étrangère composite `(team_id, organization_ref)` référence
`teams(id, organization_ref)` et empêche une appartenance de pointer vers une
équipe d'un autre tenant. Une requête entrante peut utiliser `teamReference` ;
l'adapter de persistance la résout avec `organizationReference` avant de créer le
`TeamMember`. La table `team_members` ne stocke pas `teamReference`.

- `TeamMemberStatus` contient `INVITED`, `ACTIVE`, `SUSPENDED` et `REMOVED`.
- Les transitions autorisées sont `INVITED -> ACTIVE`, `INVITED -> REMOVED`,
  `ACTIVE -> SUSPENDED`, `ACTIVE -> REMOVED`, `SUSPENDED -> ACTIVE` et
  `SUSPENDED -> REMOVED`.
- `REMOVED` est terminal.

`startedAt` représente le début effectif de l'appartenance. Il reste nul pendant
`INVITED`, est renseigné lors d'une création directe en `ACTIVE` ou de la
transition `INVITED -> ACTIVE`, puis reste inchangé pendant `SUSPENDED`.
`endedAt` reste nul pour `INVITED`, `ACTIVE` et `SUSPENDED`, puis est renseigné
au passage à `REMOVED`. Une transition directe `INVITED -> REMOVED` conserve
donc `startedAt = null`.

Une seule appartenance non terminée est autorisée pour le triplet
`(organizationReference, teamId, userReference)`. Les statuts non terminés sont
`INVITED`, `ACTIVE` et `SUSPENDED`. Une réinvitation après `REMOVED` crée une
nouvelle ligne avec un nouvel `id`, `startedAt = null` et `endedAt = null` ; la
ligne terminée reste inchangée pour conserver l'historique des périodes
d'appartenance.

Les modèles n'exposent pas de modification libre du statut. Ils fournissent des
opérations métier explicites et refusent toute transition absente des listes
précédentes. La suspension d'une organisation ou d'une équipe ne réécrit pas en
cascade les statuts de ses entités enfants. Les statuts terminaux conservent les
données et leur audit.

### 7. Exposer `UserDirectory` dans l'interface nommée `user` de `tp-identity`

`tp-identity` fournit une API Java publique contenant `UserDirectory`,
`UserAvailability` et `UserDirectoryException`. Ces types sont placés dans
`io.teampulse.identity.api.user`, déclaré avec `@NamedInterface("user")`. Le
package parent `io.teampulse.identity.api` sert uniquement de namespace et
n'expose aucune interface nommée générique. Les modules consommateurs déclarent
donc précisément `identity::user`, sans obtenir automatiquement l'accès aux
futures capacités publiques de `tp-identity`. Cette API est le seul contrat
utilisateur connu de `tp-organization` et `tp-team` ; elle n'expose ni entité de
domaine, ni repository, ni classe JPA.

Le contrat prend toujours la référence du tenant :

```text
UserAvailability check(organizationReference, userReference)
```

Les deux paramètres sont déclarés avec `@NotBlank` sur le contrat public.
Une valeur nulle, vide ou blanche constitue une violation du contrat du module
consommateur. La validation est exécutée avant l'appel au repository ; elle ne
produit donc ni `NOT_FOUND`, ni `UserDirectoryException`. Cette validation ne
normalise pas les références et ne vérifie pas leur format complet.

`UserAvailability` contient :

- `AVAILABLE` pour un utilisateur `ACTIVE` ;
- `PENDING` pour un utilisateur `INVITED` ou `CREATING` ;
- `UNAVAILABLE` pour un utilisateur `SUSPENDED` ou `DEACTIVATED` ;
- `NOT_FOUND` lorsque l'utilisateur n'existe pas ou appartient à une autre
  organisation.

Retourner `NOT_FOUND` aussi bien pour un utilisateur absent que pour un
utilisateur d'un autre tenant évite de révéler l'existence de ce dernier.

Ces quatre valeurs représentent des résultats métier prévisibles et ne
déclenchent pas d'exception inter-module. Lorsque `tp-identity` ne peut pas
répondre pour une raison technique, l'implémentation traduit l'erreur interne en
`UserDirectoryException`, exception non vérifiée appartenant au contrat public.
Elle ne laisse sortir ni exception JPA ou Spring, ni `UserException` interne.

`tp-organization` exige `AVAILABLE` pour l'administrateur et le manager avant la
transition de l'organisation vers `ACTIVE`. `tp-team` exige `AVAILABLE` pour les
responsables d'une équipe et pour une appartenance créée directement en
`ACTIVE`. Une appartenance `INVITED` accepte `PENDING`. `UNAVAILABLE` et
`NOT_FOUND` sont toujours refusés dans ces scénarios.

Aucun client Java OpenAPI n'est généré en W001. Lors d'une extraction future en
service, un adapter HTTP ou un client généré pourra implémenter ce contrat sans
modifier les règles des modules consommateurs.

### 8. Exposer `OrganizationDirectory` dans l'interface nommée `api` de `tp-organization`

`tp-organization` fournit une API Java publique contenant
`OrganizationDirectory`, `OrganizationAvailability` et
`OrganizationDirectoryException`. Ces types sont placés directement dans
`io.teampulse.organization.api`, package déjà déclaré avec
`@NamedInterface("api")`. Cette API est le seul contrat d'organisation connu de
`tp-team` ; elle n'expose ni modèle de domaine, ni enum `OrganizationStatus`, ni
repository, ni classe JPA.

Le contrat vérifie une référence précise et ne fournit aucune liste globale :

```text
OrganizationAvailability check(organizationReference)
```

`OrganizationAvailability` contient :

- `AVAILABLE` pour une organisation `ACTIVE` ;
- `UNAVAILABLE` pour une organisation `CREATING`, `SUSPENDED` ou `ARCHIVED` ;
- `NOT_FOUND` lorsque la référence n'existe pas.

Ces valeurs sont des résultats métier attendus. Une incapacité technique à
effectuer la vérification est exposée séparément par
`OrganizationDirectoryException`, sans révéler une exception de persistance ou
`OrganizationException` au module consommateur.

Le service de création d'équipe appelle d'abord `OrganizationDirectory` avec la
`tenantReference` portée par son `TenantContext`, utilisée comme
`organizationReference` dans ce contrat métier. Il poursuit uniquement avec
`AVAILABLE`, vérifie ensuite les deux responsables avec `UserDirectory`, puis
crée l'équipe directement en `ACTIVE`. `UNAVAILABLE` et `NOT_FOUND` refusent la
création.

En W001, cet échange reste un appel Java synchrone dans le monolithe modulaire.
Une future extraction de `tp-organization` pourra remplacer l'implémentation par
un adapter HTTP sans modifier le cas d'usage consommateur.

### 9. Définir les erreurs internes et leur traduction entre modules

Chaque module possède dans son package interne `domain.error` une enum de codes
stables et une seule exception métier non vérifiée :

```text
io.teampulse.organization.domain.error
├── OrganizationErrorCode
└── OrganizationException

io.teampulse.identity.domain.error
├── UserErrorCode
└── UserException

io.teampulse.team.domain.error
├── TeamErrorCode
└── TeamException
```

L'exception exige un code non nul, porte un message de diagnostic interne et
accepte une cause facultative. Le domaine l'utilise pour ses invariants et
transitions ; l'application du même module l'utilise pour les erreurs
d'orchestration. Le message interne n'est jamais renvoyé directement au client.
Les codes et exceptions ne dépendent ni de Spring, ni de JPA, ni de HTTP.

`tp-organization` possède `OrganizationErrorCode` :

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

`tp-identity` possède `UserErrorCode` :

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

`tp-team` possède `TeamErrorCode` :

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
MEMBER_NOT_FOUND
MEMBER_ALREADY_EXISTS
MEMBER_USER_NOT_FOUND
MEMBER_USER_NOT_AVAILABLE
INVALID_MEMBER_STATUS_TRANSITION
REFERENCE_GENERATION_FAILED
CONCURRENT_MODIFICATION
```

`TeamMember` utilise `TeamErrorCode` puisqu'il appartient au module `tp-team`.
Les codes `*_NOT_AVAILABLE` couvrent `PENDING` et `UNAVAILABLE` lorsqu'un
utilisateur `AVAILABLE` est obligatoire. Les codes `*_NOT_FOUND` restent
distincts. `EMAIL_ALREADY_USED` est toujours évalué dans le tenant courant.

Ces enums restent dans leurs modules propriétaires. `tp-common` ne contient
aucune enum globale d'erreurs métier ni exception métier racine imposée aux
modules. Les futurs adapters web propres à chaque module traduiront les codes en
messages et statuts HTTP sans modifier les règles métier.

Une valeur `UNAVAILABLE` ou `NOT_FOUND` retournée par un directory est traitée
comme un résultat métier puis traduite par le module consommateur dans son
propre code. Une défaillance technique suit une chaîne de traduction explicite :

```text
exception technique de persistance
  -> exception métier interne du module fournisseur
  -> DirectoryException publique du contrat appelé
  -> exception et code métier du module consommateur
  -> réponse HTTP produite par son adapter web
```

Ainsi, `ORGANIZATION_UNAVAILABLE` signifie que l'organisation existe mais que
son statut interdit l'opération, tandis que
`ORGANIZATION_DIRECTORY_UNAVAILABLE` signifie que `tp-organization` n'a pas pu
répondre. `USER_DIRECTORY_UNAVAILABLE` porte la même distinction pour
`UserDirectory`. Le module consommateur conserve l'exception publique comme
cause lorsqu'il la traduit ; il ne capture jamais l'exception interne d'un
autre module.

### 10. Isoler les données dans les schémas et les requêtes

- `organizations` ne porte pas de colonne `organization_ref` réflexive ; sa
  propre colonne `reference` constitue la référence du tenant.
- `adminReference`, `managerReference` et `userReference` sont persistés dans
  `admin_reference`, `manager_reference` et `user_reference`.
  `organizationReference` conserve la convention SQL `organization_ref`. Une
  future référence d'administrateur plateforme suivra
  `platformAdminReference` en Java et `platform_admin_reference` en SQL, sans
  introduire ce champ dans T04.
- Les colonnes `admin_reference` et `manager_reference` de `organizations` sont
  nullables uniquement pendant `CREATING`. Une contrainte interdit tout autre
  statut sans les deux références.
- Les références d'administrateur et de manager d'une équipe sont obligatoires
  dès son insertion en `ACTIVE`.
- `teams` expose une contrainte unique sur `(id, organization_ref)` afin de servir
  de cible à la clé étrangère composite de `team_members`.
- `team_members` utilise la clé étrangère composite
  `(team_id, organization_ref) -> teams(id, organization_ref)`.
- `team_members` possède un index unique partiel sur
  `(organization_ref, team_id, user_reference)` limité aux statuts `INVITED`,
  `ACTIVE` et `SUSPENDED`.
- `team_members.started_at` est nul pendant `INVITED` et obligatoire dans les
  statuts `ACTIVE` et `SUSPENDED`. Dans `REMOVED`, il peut rester nul uniquement
  si l'invitation n'a jamais été activée. `ended_at` est nul hors du statut
  `REMOVED` et obligatoire dans ce statut.
- Toute table possédée par un tenant porte `organization_ref TEXT NOT NULL`.
- Les références persistées sont protégées par des contraintes `UNIQUE`.
- Les index de lecture tenantée commencent par `organization_ref` lorsque les
  requêtes sont filtrées par tenant.
- Les contraintes composites incluent `organization_ref` lorsque cela empêche
  une association entre deux tenants.
- Aucun module n'ajoute de clé étrangère vers l'identifiant technique d'un autre
  module.
- La cohérence inter-module est vérifiée par les ports applicatifs, notamment
  `UserDirectory` et `OrganizationDirectory`.

PostgreSQL RLS n'est pas activé dans ce ticket. L'isolation W001 repose sur les
contrats applicatifs, les requêtes tenantées et les contraintes relationnelles.

### 11. Préparer concurrence et audit sans modèle JPA partagé

- Chaque entité persistée porte un champ `version BIGINT` destiné au verrouillage
  optimiste.
- Le mapping JPA de chaque module applique `@Version` localement.
- Le verrouillage pessimiste n'est pas généralisé. Il pourra être choisi par un
  cas d'usage futur qui justifie le coût du verrou en base.
- Chaque entité porte `createdAt`, `createdBy`, `modifiedAt` et `modifiedBy`.
- En W001, `createdBy` et `modifiedBy` reçoivent la valeur `SYSTEM`.
- Les dates utilisent `Instant` en Java et un type PostgreSQL avec fuseau adapté.
- Les champs d'audit restent dupliqués dans les mappings de chaque module :
  aucune `@MappedSuperclass` JPA n'est ajoutée à `tp-common`.
- Pour `User`, l'identifiant technique, la version et l'audit appartiennent
  exclusivement à `UserEntity`. Ils ne sont ni transmis aux factories du
  domaine ni réintroduits dans `User` lors de sa restauration.
- Le mapping MapStruct de l'adapter copie uniquement l'état métier. Une mise à
  jour cible l'entité JPA gérée avec `@MappingTarget` et ignore explicitement
  `id`, `version` et les champs d'audit, afin de laisser JPA et l'infrastructure
  gérer leur cycle de vie.
- La configuration MapStruct transverse est exposée par l'interface nommée
  `common::mapping`. Les modules consommateurs déclarent explicitement cette
  dépendance sans ouvrir les autres packages internes de `tp-common`.
- Ces champs ne remplacent pas un historique. W012 introduira les premiers
  événements d'audit persistés pour la création de `Team` et `User`.
- W012 ne couvre pas automatiquement l'historisation complète. Celle-ci devra
  étendre W012 ou faire l'objet d'un ticket ultérieur dédié afin de conserver,
  pour chaque modification, l'entité, l'action, l'ancien et le nouvel état,
  l'auteur, la date et le tenant concernés.

### 12. Mutualiser l'infrastructure des tests de persistance

- Créer le module Maven `tp-test-support` pour porter l'infrastructure de test
  réutilisable par `tp-identity`, `tp-organization`, `tp-team` et `tp-app`.
- Placer dans ce module `TeamPulsePostgreSQLContainer`, la configuration
  `@ServiceConnection` PostgreSQL, l'auditeur `SYSTEM` et le provider temporel
  mutable utilisé par les assertions d'audit.
- Séparer `PostgreSQLTestConfiguration`, réutilisable par les tests de contexte
  de `tp-app`, de `JpaAuditingTestConfiguration`, nécessaire aux tests isolés de
  persistance des modules métier.
- Fournir `PersistenceIntegrationTestConfiguration` comme composition explicite
  des configurations PostgreSQL et audit.
- Conserver dans chaque module métier sa classe `@SpringBootApplication` de test
  et son `AbstractIntegrationTest`, car le package racine à scanner et la
  configuration Flyway restent propres au module.
- Déclarer `tp-test-support` uniquement avec le scope Maven `test` dans les
  modules consommateurs. Testcontainers ne doit apparaître dans aucun classpath
  de production.
- Ne pas annoter `tp-test-support` avec `@ApplicationModule` : il constitue un
  module Maven d'outillage, pas un module fonctionnel de TeamPulse.
- Exclure `io.teampulse.testsupport` du modèle analysé par Spring Modulith et
  interdire avec ArchUnit toute dépendance du code de production vers ce package.
- Ne conserver aucune classe Testcontainers dans `tp-common`.

Cette décision remplace, à partir de W001-T04, la localisation de
`TestcontainersConfiguration` dans `tp-app/src/test` décrite par ADR-W001-T02.
Elle étend les contrôles définis par ADR-W001-T03. Le catalogue interne des
modules métier est complété par R11 pour encadrer les dépendances et les
annotations Spring des services applicatifs ; R10 reste le contrôle de
cohérence du catalogue entre tous les modules métier.

### 13. Autoriser un usage limité de Spring dans les services applicatifs

- Les classes placées dans `application.service` peuvent utiliser `@Service`,
  `@Validated` et `@Transactional`.
- `@Service` déclare directement le service comme bean et évite un wiring
  répétitif dans une classe `@Configuration`.
- `@Validated` active la validation déclarative des contraintes Jakarta
  Validation portées par les paramètres des ports entrants et par leurs
  commandes.
- `@Transactional` définit la frontière transactionnelle au niveau du cas
  d'usage. Les opérations de lecture utilisent `@Transactional(readOnly = true)`
  lorsque leur comportement est strictement en lecture.
- Les annotations transactionnelles utilisées sont celles de Spring afin de
  disposer notamment de l'attribut `readOnly` et d'une sémantique homogène avec
  Spring Data.
- Cette autorisation est limitée à ces trois annotations déclaratives. Un
  service applicatif ne dépend ni de JPA, ni de Spring Data, ni d'une entité de
  persistance, ni d'un contrôleur ou d'un type HTTP.
- La logique d'orchestration reste exprimée avec les modèles du domaine et les
  ports applicatifs. Elle reste testable sans base de données ; les tests qui
  vérifient la validation et les transactions utilisent toutefois le bean
  Spring proxifié.
- Les services annotés ne sont pas déclarés une seconde fois avec `@Bean`.
- Une transaction englobant un contrôle d'existence et une insertion ne suffit
  pas à supprimer une course concurrente. Les contraintes PostgreSQL restent
  la garantie finale d'unicité et les adapters traduisent leurs violations dans
  le code d'erreur métier approprié.

Cette décision accepte un couplage Spring explicite et limité dans la couche
application. Le domaine, les ports métier et leurs modèles restent indépendants
de JPA, de Spring Data et des détails de transport.

### 14. Répartir les tests selon la responsabilité de chaque couche

La stratégie distingue les tests unitaires, qui vérifient le comportement
propre à une classe sans démarrer Spring, des tests d'intégration, qui vérifient
un contrat fourni par Spring, Jakarta Validation, JPA, Flyway ou PostgreSQL.
Chaque comportement est testé au niveau le plus bas qui permet de l'observer
sans simuler le framework responsable de ce comportement.

| Couche | Tests unitaires | Tests d'intégration |
| --- | --- | --- |
| Contrôleurs | Tester uniquement une transformation ou une décision propre au contrôleur lorsqu'elle existe. Les DTO ou mappers non triviaux peuvent être instanciés directement. Un contrôleur limité à l'adaptation HTTP n'a pas besoin d'un test unitaire qui reproduit Spring MVC. | Utiliser un test de slice Web avec le cas d'usage substitué pour vérifier désérialisation, Jakarta Validation, construction du `TenantContext`, mapping requête/commande, codes HTTP, corps de réponse et traduction des erreurs. Conserver quelques scénarios HTTP complets avec les vrais services et PostgreSQL pour les parcours critiques, sans reproduire tous les cas métier. |
| Services applicatifs | Instancier directement le service avec des ports sortants et une `ReferenceFactory` substitués. Vérifier l'orchestration, l'ordre des appels, la propagation du tenant, l'utilisation des valeurs canoniques du domaine, les erreurs métier et l'absence de persistence après un refus. | Utiliser le bean Spring proxifié pour vérifier `@Validated`, la validation en cascade des commandes, `@Transactional`, `readOnly`, le wiring et quelques interactions réelles service/repository. Ne pas attendre d'un test unitaire direct qu'il déclenche les proxies Spring. |
| Repositories et persistence | Tester séparément un mapper lorsqu'il porte une transformation significative, ainsi que la traduction d'une exception technique par l'adapter si elle peut être isolée utilement. Ne pas mocker `JpaRepository` pour tester une simple délégation ou le fonctionnement de Spring Data. | Utiliser PostgreSQL réel avec Flyway et JPA pour vérifier migration, mapping complet, requêtes tenantées, contraintes `NOT NULL` et `UNIQUE`, unicité canonique par organisation, audit, traduction des violations et verrouillage optimiste. H2 n'est pas utilisé comme substitut aux comportements PostgreSQL. |

La couverture suit donc les règles suivantes :

- les invariants et transitions du domaine sont couverts par des tests unitaires
  du domaine, sans Spring ;
- l'orchestration des cas d'usage est principalement couverte par des tests
  unitaires des services ;
- les annotations déclaratives et le wiring sont couverts par des tests
  d'intégration Spring ciblés ;
- les garanties de stockage sont couvertes une seule fois par les tests
  d'intégration PostgreSQL des repositories ;
- les tests HTTP ne répètent pas exhaustivement les tests du domaine, des
  services et de la persistence. Ils se concentrent sur le contrat HTTP et sur
  un nombre réduit de parcours verticaux critiques ;
- un test ne vérifie pas une annotation par réflexion lorsqu'il peut vérifier
  directement son effet observable à travers le framework concerné.

## Alternatives envisagées

### Propager l'identifiant `BIGINT` de l'organisation

Option rejetée. Elle est simple dans un monolithe, mais couple tous les modules à
la clé de persistance de `tp-organization` et prépare mal l'extraction future de
services.

### Utiliser un UUID brut comme référence publique

Option non retenue pour W001. Elle offre une excellente entropie côté Java, mais
ne respecte pas le format métier lisible avec trigramme, année et suffixe court.
Un UUID peut rester une option future si l'exigence de format change.

### Utiliser une séquence PostgreSQL pour la référence

Option rejetée. Elle donne une unicité centralisée, mais rend la génération
dépendante de la base et empêche de créer une référence avant l'appel de
persistance.

### Porter l'identifiant JPA, la version et l'audit dans `User`

Option rejetée. Ces champs sont nécessaires à la persistance, mais ne
participent à aucune règle métier de l'utilisateur. Les conserver dans
`UserEntity` évite de coupler le domaine au cycle de vie JPA et à la stratégie
d'audit.

### Utiliser uniquement dix caractères aléatoires

Option rejetée comme mécanisme unique. La probabilité de collision est faible,
mais le format n'est pas ordonnable, les tests sont moins déterministes et la
garantie reste probabiliste.

### Utiliser huit caractères de temps global et deux de compteur

Option initialement envisagée puis remplacée. Elle encode les millisecondes
depuis une époque TeamPulse fixe et fournit 1 296 valeurs par milliseconde, mais
elle impose une limite d'environ 89 ans au format. Le découpage par journée
évite cette date d'expiration et libère quatre caractères pour réduire le risque
de collision après redémarrage.

### Utiliser six caractères de temps, deux de nonce et deux de compteur

Option rejetée. Elle conserve 1 296 générations par milliseconde, mais seulement
1 296 nonces possibles. Dans le pire cas inter-redémarrage, la probabilité de
collision serait de `1 / 1 296`, soit environ `0,07716 %` : insuffisant pour la
garantie visée.

### Utiliser cinq caractères de temps, trois de nonce et deux de compteur

Option rejetée. `36^5` ne couvre pas les 86 400 000 millisecondes d'une journée.
Il faudrait introduire une unité logique de deux millisecondes, ce qui
complexifierait inutilement le format et les explications.

### Utiliser six caractères de temps, trois de nonce et trois de compteur

Option rejetée. Elle fournirait 46 656 compteurs par milliseconde, bien au-delà
du besoin, mais seulement 46 656 nonces. À longueur totale identique, le
découpage `6 + 4 + 2` affecte le caractère supplémentaire à la protection la
plus utile après un redémarrage.

### Réserver un lot de références en base

Option rejetée pour T04. Une allocation de slots ou une stratégie hi-lo
centraliserait l'unicité entre JVM, mais rendrait le générateur de `tp-common`
dépendant d'un port de persistance et de son intégration. La décision reste une
implémentation Java pure ; la coordination multi-nœud sera étudiée avant
Kubernetes.

### Utiliser une implémentation `synchronized`

Option non retenue. Elle simplifierait le compteur, mais sérialiserait tous les
appels. Un état atomique avec compare-and-set fournit la garantie mono-JVM sans
verrou bloquant.

### Introduire `ReferenceKind` ou `ReferencePrefix`

Option rejetée. Ces types obligeraient `tp-common` à connaître la liste des
concepts métier ou ajouteraient un emballage sans invariant supplémentaire. Le
contrat reste `generate(String prefix)` ; la factory valide strictement
`[A-Z]{3}` et chaque module reste propriétaire de sa constante locale.

### Introduire immédiatement un `nodeId`

Option reportée. Un identifiant de nœud n'est fiable que si son attribution est
unique, stable et contrôlée. Cette infrastructure n'existe pas encore dans le
déploiement local W001. Elle sera décidée avant Kubernetes.

### Passer directement `organizationReference` aux ports entrants

Option non retenue pour les cas d'usage tenantés. Une chaîne rendrait la
référence obligatoire mais n'exprimerait pas aussi clairement qu'elle constitue
le périmètre d'exécution complet du cas d'usage. `TenantContext` conserve cette
intention dans le contrat ; les ports sortants continuent toutefois à recevoir
explicitement `organizationReference`.

### Créer un `PlatformContext` vide

Option rejetée en W001. Sans identité authentifiée, rôle, permission ou autre
information fiable, ce type ne ferait que matérialiser l'absence de tenant sans
ajouter d'invariant utile. Les cas d'usage plateforme restent non tenantés ; un
contexte de sécurité dédié pourra être décidé avec W008 lorsqu'il portera une
information réelle.

### Insérer une organisation locale par Flyway

Option rejetée. Une migration structurelle ne doit pas créer une ligne métier
temporaire uniquement pour fournir le tenant d'un contrôleur pendant une version
de développement.

### Exiger les responsables dès l'insertion de l'organisation

Option rejetée. L'administrateur et le manager sont des utilisateurs qui ont
eux-mêmes besoin de la référence de l'organisation. L'état `CREATING` permet de
créer la racine du tenant, puis ses utilisateurs, avant d'activer l'organisation
avec ses deux responsables validés.

### Ne pas stocker `organizationReference` dans `TeamMember`

Option rejetée. Déduire systématiquement le tenant par une jointure avec `Team`
rend plus facile l'oubli du filtre, complique les index et limite les contraintes
composites d'isolation.

### Stocker `teamReference` dans `TeamMember`

Option rejetée. `Team` et `TeamMember` appartiennent au même module et au même
modèle de persistance. Une clé étrangère vers `teams.id` est plus directe et ne
franchit aucune frontière. La référence d'équipe reste réservée aux contrats
externes et est résolue dans le module avec la référence du tenant.

### Donner une référence fonctionnelle propre à `TeamMember`

Option rejetée. L'appartenance est une entité interne de l'équipe et aucun
contrat inter-module n'a besoin de l'adresser indépendamment. Le triplet tenant,
équipe et utilisateur identifie l'appartenance courante ; l'identifiant
technique, `startedAt` et `endedAt` distinguent les périodes historiques.

### Partager une `BaseEntity` JPA dans `tp-common`

Option rejetée. Elle réduirait quelques lignes de mapping, mais ferait dépendre
le module commun de JPA et contredirait les règles d'architecture du projet.

### Placer l'infrastructure Testcontainers dans `tp-common`

Option rejetée. Elle ajouterait Testcontainers aux dépendances de production de
`tp-common` et, par transitivité, aux modules métier. L'infrastructure partagée
reste dans un artefact explicitement consommé avec le scope Maven `test`.

### Déclarer `tp-test-support` comme module Spring Modulith

Option rejetée. `@ApplicationModule` l'ajouterait au graphe fonctionnel et à la
documentation de l'application, alors qu'il ne participe qu'à l'exécution des
tests. Son exclusion explicite conserve la distinction entre module Maven de
support et module applicatif.

### Générer immédiatement un client OpenAPI entre les modules

Option reportée. Dans le monolithe modulaire, un port Java et un adapter local
suffisent. OpenAPI sera étudié lorsque les modules communiqueront réellement par
HTTP.

### Placer les directories dans `application.directory`

Option rejetée pour TeamPulse. Le contrat resterait techniquement un port
applicatif valide, mais obligerait les modules consommateurs à dépendre de
l'organisation interne de la couche application. Les packages `api` exposés par
T01 matérialisent déjà la frontière publique et sont réutilisés.

### Exposer toute l'API publique de `tp-identity` avec une interface générique `api`

Option rejetée. Une interface nommée générique donnerait à chaque module
consommateur l'accès à toutes les capacités publiques actuelles et futures de
`tp-identity`. Le découpage par capacité avec `identity::user` applique le
principe du moindre couplage et permet d'ajouter ultérieurement d'autres
interfaces nommées sans élargir les dépendances existantes.

### Déclarer un `UserDirectory` différent dans chaque module consommateur

Option rejetée. Des ports presque identiques dans `tp-organization` et `tp-team`
dupliqueraient la définition de la disponibilité et ses règles de mapping.
L'API applicative publique de `tp-identity` fournit un contrat unique sans
exposer son domaine ou sa persistence.

### Placer `UserDirectory` dans `tp-common`

Option rejetée. La disponibilité d'un utilisateur appartient au métier du module
identité et ne constitue pas un type transverse neutre. `tp-common` reste donc
indépendant des contrats métier.

### Faire lire directement les organisations par `tp-team`

Option rejetée. Accéder au domaine, au repository ou à l'entité JPA de
`tp-organization` depuis `tp-team` violerait la propriété des données et
couplerait la création d'équipe à la persistance interne d'un autre module.

### Retourner un booléen depuis `OrganizationDirectory`

Option rejetée. Un booléen ne permettrait pas de distinguer une organisation
connue mais non opérationnelle d'une référence inexistante. L'enum
`OrganizationAvailability` rend ces résultats explicites sans exposer
`OrganizationStatus`.

### Centraliser toutes les erreurs métier dans `tp-common`

Option rejetée. Une enum globale couplerait les modules à des concepts qu'ils ne
possèdent pas et transformerait `tp-common` en catalogue métier. Chaque module
conserve donc sa propre enum.

### Exposer le package `domain.error` avec `@NamedInterface`

Option rejetée. Un module consommateur serait alors couplé aux erreurs internes
du fournisseur et pourrait dépendre de détails qui ne font pas partie du
contrat appelé. Seules les exceptions techniques minimales des directories sont
publiques dans l'interface nommée `api`.

### Laisser traverser directement les exceptions JPA ou Spring

Option rejetée. Elle ferait dépendre le module consommateur de l'infrastructure
du fournisseur. Toute exception technique est traduite avant de franchir la
frontière publique.

### Utiliser une classe d'exception pour chaque code métier

Option rejetée pour T04. Une exception non vérifiée par module, portant un code
obligatoire, conserve un contrat lisible sans multiplier les classes.

### Créer une enum d'erreurs séparée pour `TeamMember`

Option rejetée pour T04. `TeamMember` est possédé par `tp-team` et ses erreurs
sont utilisées par les mêmes cas d'usage d'équipe. Elles restent dans
`TeamErrorCode` afin de ne pas fragmenter prématurément le contrat d'erreur.

## Justification

Une référence fonctionnelle découple le contrat d'intégration de la persistence
interne tout en restant exploitable dans les logs, URLs et futurs événements. La
référence d'organisation devient une clé de tenant uniforme sans exposer la clé
primaire du module organisation.

Le générateur monotone répond au besoin de concurrence d'une instance Java et
reste testable grâce à `Clock`. La contrainte d'unicité PostgreSQL conserve une
dernière défense cohérente avec l'exigence d'intégrité. La limite multi-nœud est
rendue explicite plutôt que de revendiquer une garantie que W001 ne peut pas
encore démontrer.

Le `TenantContext` explicite et les signatures tenantées rendent l'oubli du
tenant visible à la compilation et dans les tests. Le champ générique
`tenantReference` exprime le périmètre isolé sans coupler `tp-common` à
`Organization`. Ce contrat prépare JWT sans introduire de contexte ambiant caché
avant que la sécurité ne fournisse une identité fiable.

Enfin, les ports inter-modules préservent l'architecture hexagonale : chaque
module reste propriétaire de ses modèles, enums et adapters de persistance.

La mutualisation dans `tp-test-support` évite la duplication de l'image
PostgreSQL, des identifiants de connexion, du wiring `@ServiceConnection` et de
l'audit déterministe. Le scope Maven `test`, l'exclusion Spring Modulith et la
règle ArchUnit forment trois protections complémentaires contre une fuite de
cette infrastructure dans le runtime.

## Conséquences positives

- Les identifiants techniques ne franchissent plus les frontières de modules.
- La référence de tenant est uniforme et utilisable dans les futurs transports
  HTTP et événementiels.
- Les opérations tenantées rendent leur contexte explicite avec
  `TenantContext(tenantReference)`.
- Les services applicatifs partagent une convention déclarative unique pour
  leur découverte, la validation de leurs entrées et leurs transactions.
- Les opérations plateforme ne dépendent pas d'un type vide sans invariant.
- Le générateur est indépendant de Spring et testable avec une horloge
  contrôlée.
- Les appels concurrents d'une même JVM ne nécessitent pas de verrou bloquant.
- Le suffixe `6 + 4 + 2` fournit 1 296 références par milliseconde logique,
  1 679 616 nonces de démarrage possibles et fonctionne sans date d'expiration
  liée à une époque fixe.
- Les contraintes et index PostgreSQL renforcent les invariants applicatifs.
- `tp-common` reste framework-agnostic.
- `UserDirectory` évite le couplage de `tp-organization` et `tp-team` avec le
  domaine ou la persistence de `tp-identity`.
- `OrganizationDirectory` permet à `tp-team` d'exiger une organisation active
  sans dépendre du domaine ou de la persistence de `tp-organization`.
- Les interfaces nommées `api` rendent les dépendances inter-modules visibles et
  vérifiables par Spring Modulith.
- Les codes d'erreur restent stables et indépendants des messages ou du
  protocole d'exposition.
- Chaque module reste propriétaire de son vocabulaire d'échec.
- Les résultats métier attendus sont distingués des défaillances techniques.
- Les exceptions de persistance et les erreurs internes ne fuient pas vers les
  modules consommateurs.
- Les modèles préparent le verrouillage optimiste et l'audit futur.
- Les tests de persistance réutilisent une configuration PostgreSQL et d'audit
  unique sans ajouter Testcontainers aux artefacts de production.
- `tp-test-support` reste absent du graphe fonctionnel Spring Modulith.

## Conséquences négatives / compromis

- Les références sont plus longues qu'un identifiant numérique.
- Le découpage du suffixe et l'interprétation « millisecondes dans la journée »
  font partie du format persistant et devront être conservés ou versionnés lors
  d'une évolution incompatible.
- `organizationReference` doit être propagée dans de nombreuses signatures et
  clés d'index.
- Les mappings d'audit et de version contiennent une duplication volontaire
  entre modules.
- Les références des responsables d'une organisation sont temporairement
  nullables pendant `CREATING` et exigent une contrainte dépendante du statut.
- La cohérence inter-module n'est pas garantie par des clés étrangères vers les
  identifiants techniques externes.
- Le contexte local change après redémarrage et n'est adapté qu'au développement.
- `TenantContext` ajoute volontairement un petit emballage autour d'une référence
  textuelle afin de rendre le périmètre tenant explicite dans les ports entrants.
- La couche application accepte une dépendance limitée à trois annotations
  Spring. Un appel direct construit avec `new` ne déclenche ni la validation de
  méthode, ni la transaction ; ces garanties nécessitent le bean proxifié.
- La stratégie W001 ne démontre pas encore une unicité absolue multi-instance.
- La garantie après redémarrage ou entre plusieurs JVM reste probabiliste ; une
  collision exceptionnelle fait échouer l'insertion au lieu d'être masquée par
  un retry automatique.
- Le verrouillage optimiste peut produire une erreur de concurrence que le cas
  d'usage devra traduire proprement.
- La création d'équipe ajoute un appel applicatif synchrone vers
  `tp-organization` avant les contrôles utilisateurs.
- Les enums devront rester limitées aux échecs stables et actionnables afin de
  ne pas devenir des catalogues fourre-tout.
- La traduction d'une défaillance à chaque frontière ajoute quelques classes et
  blocs de mapping explicites.
- Les exceptions publiques des directories deviennent des éléments stables de
  l'API Java du module.
- Le module de support doit rester explicitement exclu du modèle Spring Modulith
  tant que son package se trouve sous la racine `io.teampulse`.

## Impact technique

### `tp-common`

- Type `TenantContext(tenantReference)` en Java pur ; aucun `PlatformContext`
  vide.
- Contrat `ReferenceFactory.generate(String prefix)` et implémentation
  `MonotonicReferenceFactory`.
- Validation stricte du préfixe `[A-Z]{3}` sans normalisation automatique.
- Injection directe de `java.time.Clock` dans la factory.
- Validateurs transverses de référence, sans annotation JPA.
- `CommonMapperConfig` exposé uniquement via l'interface Spring Modulith nommée
  `mapping`.

### `tp-test-support`

- `TeamPulsePostgreSQLContainer` basé sur `postgres:18-bookworm`.
- Configurations de test PostgreSQL, audit JPA et persistance composée.
- `MutableAuditDateTimeProvider` pour les assertions temporelles déterministes.
- Dépendances Spring Boot Test et Testcontainers confinées dans cet artefact.

### `tp-organization`

- Domaine `Organization`, statut et timezone.
- Ports de création et de consultation par référence.
- Mapping de persistance avec version et audit.
- Service exposant ou validant la référence d'organisation.
- `OrganizationDirectory`, `OrganizationAvailability` et
  `OrganizationDirectoryException` dans `io.teampulse.organization.api`, déjà
  exposé par `@NamedInterface("api")`.
- `OrganizationErrorCode` et `OrganizationException` dans le package interne
  `io.teampulse.organization.domain.error`.
- Consommation de l'API publique `UserDirectory` pour valider les responsables.
- Dépendance Spring Modulith limitée à `identity::user` en plus de `common`.

### `tp-identity`

- Domaine `User`, statuts et transitions initiales.
- Séparation explicite entre l'état métier de `User` et les données techniques
  de `UserEntity` (`id`, version et audit).
- Canonisation de l'email, unicité tenantée de sa forme canonique et limites de
  254 caractères pour l'email et de 100 caractères pour chaque nom.
- `UserDirectory`, `UserAvailability` et `UserDirectoryException` dans
  `io.teampulse.identity.api.user`, exposé par `@NamedInterface("user")`.
- `UserErrorCode` et `UserException` dans le package interne
  `io.teampulse.identity.domain.error`.
- Repositories toujours filtrés par `organizationReference`.
- Services applicatifs déclarés avec `@Service`, validés avec `@Validated` et
  transactionnels avec `@Transactional`, sans dépendance vers JPA, Spring Data,
  `UserEntity` ou la couche HTTP.
- Implémentation de `UserDirectory` sans exposition du domaine ou de JPA.

### `tp-team`

- Domaines `Team` et `TeamMember`.
- `TeamErrorCode`, incluant les erreurs de `TeamMember`, et `TeamException` dans
  le package interne `io.teampulse.team.domain.error`.
- Consommation de l'API publique `OrganizationDirectory` de `tp-organization`.
- Consommation de l'API publique `UserDirectory` de `tp-identity`.
- Vérification de l'administrateur, du manager et des membres dans le tenant.
- Repositories et contraintes composites tenantés.
- Périodes d'appartenance explicites avec `startedAt` et `endedAt`.
- Dépendances Spring Modulith limitées à `organization::api`, `identity::user` et
  `common`.

### `tp-app`

- Wiring des factories, horloges, ports inter-modules et adapters.
- Service singleton de référence d'organisation courante pour le profil local.
- Aucun mécanisme JWT ou `hasPermission` dans W001.
- Dépendance `tp-test-support` en scope `test` et import explicite de
  `PostgreSQLTestConfiguration` dans les tests de contexte.
- Exclusion de `io.teampulse.testsupport` du modèle Spring Modulith et règle
  ArchUnit interdisant sa consommation par le code de production.

### PostgreSQL / Flyway

- Tables `organizations`, `users`, `teams` et `team_members` dans leurs schémas
  propriétaires.
- Identifiants `BIGINT`, références `TEXT`, versions `BIGINT` et champs d'audit.
- Contraintes d'unicité, de non-nullité et d'isolation tenant.
- Index commençant par `organization_ref` pour les recherches tenantées.
- Aucune donnée métier locale insérée par une migration structurelle.

### Documentation pédagogique

- Le Slidev explique la différence entre identifiant et référence.
- Il illustre l'année, le segment `JOUR_MOIS`, le temps logique interne, le
  découpage base 36 `6 + 4 + 2`, le nonce JVM, le compteur jusqu'à `ZZ`, son
  débordement et le comportement lors d'un recul d'horloge.
- Il explique que l'unicité est déterministe pendant le cycle de vie d'une
  factory singleton, probabiliste après redémarrage, et que la référence ne
  remplace pas `createdAt` comme source temporelle exacte.
- Il compare verrouillage optimiste et pessimiste.
- Il mentionne explicitement que l'identité de nœud deviendra importante avant
  Kubernetes, sans détailler prématurément son orchestration.

## Validation

### Stratégie de tests par couche

- Pour chaque contrôleur, couvrir avec un test Web les requêtes valides, les
  erreurs de validation, la résolution du tenant, le mapping du résultat et la
  traduction des principales erreurs métier en réponses HTTP.
- Pour chaque service applicatif, couvrir unitairement le succès, les refus
  métier, les interactions avec les ports et la propagation du tenant. Ajouter
  uniquement les tests Spring nécessaires pour les contraintes déclaratives,
  les transactions et le wiring.
- Pour chaque adapter de persistence, couvrir sur PostgreSQL les requêtes
  tenantées et les garanties réellement portées par le schéma ou JPA. Réserver
  les tests unitaires aux transformations ou traductions propres à l'adapter.
- Maintenir au moins un parcours vertical HTTP vers PostgreSQL pour chaque flux
  critique, notamment la création tenantée, sans dupliquer toute la matrice des
  couches inférieures.
- Vérifier qu'aucun test de contrôleur ne simule la logique métier du service et
  qu'aucun test de service ne simule le comportement interne de JPA ou de
  PostgreSQL.

### Générateur de références

- Vérifier que `ReferenceFactory` expose `generate(String prefix)` et que
  `MonotonicReferenceFactory` respecte ce contrat.
- Vérifier que `ORG`, `USR` et `TEM` sont acceptés.
- Vérifier qu'une valeur nulle, une longueur différente de trois, des
  minuscules, des espaces ou des caractères non ASCII majuscules provoquent une
  `IllegalArgumentException`, sans normalisation implicite.
- Vérifier le format, l'année UTC, le segment `JOUR_MOIS` au format `JJMM` et le
  suffixe alphanumérique de douze caractères.
- Vérifier le découpage fixe du suffixe `MMMMMMNNNNCC` : six caractères base 36
  pour les millisecondes logiques depuis le début de la journée UTC, quatre
  caractères pour le nonce de démarrage et deux pour le compteur.
- Injecter une source de nonce déterministe dans les tests et vérifier que le
  nonce est généré une seule fois puis reste stable pendant le cycle de vie de
  la factory.
- Vérifier les bornes temporelles de la journée, de `000000` jusqu'à
  l'encodage de `86 399 999`, ainsi que les passages de jour et d'année.
- Vérifier les valeurs limites `00` et `ZZ`, soit 1 296 générations dans une
  même milliseconde logique.
- Vérifier que la génération suivante avance le temps logique d'une milliseconde
  et reprend le compteur à `00` sans attendre l'horloge.
- Vérifier que l'année, `JOUR_MOIS` et le suffixe temporel restent issus du même
  instant logique lorsque l'horloge recule.
- Générer plusieurs valeurs avec une horloge figée et vérifier leur unicité.
- Exécuter des générations concurrentes et vérifier l'absence de doublon.
- Simuler une horloge qui recule et vérifier la monotonie du temps logique.
- Simuler l'épuisement du compteur d'une unité de temps.
- Vérifier qu'une collision signalée par une contrainte `UNIQUE` ne déclenche
  aucun retry, ne remplace pas la donnée existante et est traduite en
  `REFERENCE_GENERATION_FAILED` par le module concerné.

### Frontière tenant

- Vérifier qu'un cas d'usage tenanté exige un `TenantContext` non nul dont la
  `tenantReference` est non nulle, non vide et non blanche, sans normalisation
  implicite.
- Vérifier qu'en W001 `tenantReference` contient `Organization.reference`, puis
  que le service applicatif la transmet comme `organizationReference` aux ports
  sortants.
- Vérifier qu'aucun `PlatformContext` vide n'est introduit et que les cas
  d'usage plateforme restent non tenantés jusqu'à W008.
- Vérifier qu'aucun repository tenanté ne propose une opération globale.
- Créer deux organisations de test et prouver qu'une référence de A ne permet
  jamais de lire, modifier ou supprimer une donnée de B.

### Services applicatifs

- Vérifier que les services placés dans `application.service` utilisent
  uniquement `@Service`, `@Validated` et `@Transactional` parmi les annotations
  Spring autorisées dans cette couche.
- Vérifier sur le bean Spring proxifié que les contraintes des ports entrants
  rejettent un `TenantContext` ou une commande invalide avant l'exécution de
  l'orchestration.
- Vérifier que les créations s'exécutent dans une transaction et que les cas
  d'usage strictement en lecture déclarent `@Transactional(readOnly = true)`.
- Tester directement la logique d'orchestration avec des ports substitués, sans
  démarrer Spring ni PostgreSQL.
- Vérifier avec ArchUnit que les services applicatifs ne dépendent ni de JPA,
  ni de Spring Data, ni d'`infrastructure`, ni de `config`.

### Modèles et persistance

- Vérifier toutes les transitions autorisées et interdites de `Organization`,
  `User`, `Team` et `TeamMember`.
- Vérifier que `User` n'expose ni identifiant JPA, ni version, ni champ d'audit,
  et que le mapping conserve ces valeurs dans `UserEntity`.
- Vérifier la canonisation et la limite de 254 caractères de l'email, ainsi que
  la suppression des espaces périphériques et la limite de 100 caractères de
  `firstName` et `lastName`.
- Vérifier qu'une organisation `CREATING` peut être persistée sans responsables,
  puis que son activation échoue tant que les deux responsables valides ne sont
  pas affectés.
- Vérifier les invariants administrateur/manager, y compris le cas où les deux
  références sont identiques.
- Vérifier qu'une suspension d'organisation ou d'équipe ne modifie pas en
  cascade les statuts des entités enfants.
- Vérifier qu'aucun statut terminal ne peut être quitté.
- Vérifier la timezone avec des identifiants IANA valides et invalides.
- Vérifier les contraintes `NOT NULL`, `UNIQUE` et composites avec PostgreSQL.
- Vérifier une concurrence de modification déclenchant le verrouillage
  optimiste.
- Vérifier que les quatre champs d'audit sont renseignés avec `SYSTEM` en W001.
- Vérifier que `tp-organization` et `tp-team` utilisent uniquement l'API publique
  `UserDirectory`, sans dépendre d'une entité ou d'un repository de
  `tp-identity`.
- Vérifier que `UserDirectory`, `UserAvailability` et
  `UserDirectoryException` sont accessibles via `identity::user`, tandis que
  `io.teampulse.identity.domain.error` reste interne.
- Vérifier que `UserDirectory` rejette toute référence d'organisation ou
  d'utilisateur nulle, vide ou blanche comme violation du contrat, sans appeler
  le repository et sans produire `NOT_FOUND` ou `UserDirectoryException`.
- Vérifier le mapping de chaque `OrganizationStatus` vers
  `OrganizationAvailability`.
- Vérifier que `tp-team` utilise uniquement l'API publique
  `OrganizationDirectory`, sans dépendre d'une entité, d'un enum de domaine ou
  d'un repository de `tp-organization`.
- Vérifier que `OrganizationDirectory`, `OrganizationAvailability` et
  `OrganizationDirectoryException` sont accessibles via `organization::api`,
  tandis que `io.teampulse.organization.domain.error` reste interne.
- Vérifier qu'une équipe ne peut être créée que si `OrganizationDirectory`
  retourne `AVAILABLE`, et que `UNAVAILABLE` ou `NOT_FOUND` refusent la création.
- Vérifier que chaque validation, transition, indisponibilité et conflit couvert
  par T04 produit le code attendu dans l'enum propriétaire.
- Vérifier que chaque exception métier interne exige un code non nul et conserve
  sa cause lorsqu'elle traduit une erreur antérieure.
- Vérifier qu'un résultat `NOT_FOUND`, `PENDING` ou `UNAVAILABLE` est traité sans
  exception inter-module.
- Simuler une panne technique de chaque directory et vérifier sa traduction en
  exception publique, puis dans le code `*_DIRECTORY_UNAVAILABLE` du module
  consommateur, sans exposition d'une exception JPA ou Spring.
- Vérifier que les erreurs de `TeamMember` utilisent `TeamErrorCode`.
- Vérifier qu'aucune enum globale d'erreurs métier n'est ajoutée à `tp-common`.
- Vérifier que les enums ne dépendent ni d'un statut HTTP, ni d'un message de
  présentation.
- Vérifier le mapping de chaque `UserStatus` vers `UserAvailability`.
- Vérifier qu'un utilisateur d'un autre tenant retourne `NOT_FOUND`.
- Vérifier que seuls des responsables `AVAILABLE` permettent d'activer une
  organisation ou de créer une équipe active.
- Vérifier que l'affectation d'un administrateur ou d'un manager d'équipe ne
  crée pas automatiquement de `TeamMember`, et que chacun peut être membre ou
  non indépendamment de sa responsabilité.
- Vérifier que la clé étrangère composite de `TeamMember` accepte une équipe du
  même tenant et refuse le couple `teamId`/`organizationReference` incohérent.
- Vérifier que `team_members` ne stocke ni `teamReference`, ni l'identifiant
  technique d'un utilisateur du module identité.
- Vérifier que l'index unique partiel refuse une seconde appartenance non
  terminée pour le même tenant, la même équipe et le même utilisateur.
- Vérifier qu'une nouvelle ligne peut être créée après `REMOVED` et que
  l'ancienne ligne n'est ni réactivée ni remplacée.
- Vérifier que `startedAt` reste nul pendant `INVITED`, est fixé à l'entrée en
  `ACTIVE` et survit à une suspension, puis que `endedAt` est fixé uniquement au
  passage à `REMOVED`.

### Infrastructure de test partagée

- Exécuter les six scénarios PostgreSQL réels de `tp-identity` couvrant les
  contraintes d'unicité, l'isolation tenant, le mapping, l'audit et le
  verrouillage optimiste.
- Valider les migrations par le démarrage du contexte sur une base PostgreSQL
  vide avec Flyway puis `hibernate.ddl-auto=validate`. Ne pas ajouter
  d'assertion sur `flyway_schema_history` ni sur les métadonnées internes de
  Flyway : ces assertions testeraient la bibliothèque plutôt qu'un comportement
  TeamPulse.
- Vérifier que `tp-identity` charge sa propre application de test, sa
  configuration Flyway et `PersistenceIntegrationTestConfiguration` sans
  démarrer de serveur Web.
- Vérifier que `tp-app` et ses tests de modules importent la configuration
  PostgreSQL depuis `tp-test-support`.
- Vérifier avec l'arbre Maven `runtime` que `tp-test-support` et Testcontainers
  sont absents des dépendances de production des modules consommateurs.
- Vérifier que Spring Modulith ne découvre aucun module pour
  `io.teampulse.testsupport`.
- Vérifier que la règle ArchUnit échoue si une classe de production dépend de
  `io.teampulse.testsupport`.

### Architecture

- Exécuter les tests ArchUnit existants.
- Vérifier que `tp-common` ne dépend d'aucun module métier, de Spring ou de JPA.
- Vérifier que les domaines ne dépendent pas de l'infrastructure.
- Vérifier qu'aucune référence externe n'est remplacée par l'identifiant
  technique d'un autre module.
- Exécuter `ApplicationModules.verify()` et vérifier que les consommateurs
  dépendent uniquement de `identity::user` et `organization::api`, jamais des
  packages `domain.error` externes.
- Exécuter la suite assemblée `./mvnw -pl tp-app -am test` avec Docker actif.

## Risques

- Une méthode de repository ajoutée ultérieurement sans tenant pourrait
  contourner la convention.
- Une factory recréée plusieurs fois dans la même JVM fragmenterait l'état
  atomique ; son cycle de vie doit être unique dans l'application.
- Un redémarrage simultané ou plusieurs nœuds peuvent tirer le même nonce et
  réutiliser un état logique compatible. Le risque au pire est de `1 / 36^4`
  pour le nonce ; la contrainte en base détecte le conflit, mais la stratégie
  devra évoluer avant Kubernetes.
- Une référence trop longue peut augmenter la taille des index par rapport à un
  `BIGINT`.
- L'absence de clé étrangère inter-module impose des contrôles applicatifs
  fiables et des tests de contrat.
- Une enum d'erreurs enrichie sans discipline pourrait exposer des détails
  internes ou accumuler des codes sans comportement consommateur associé.
- Une traduction oubliée à la frontière d'un directory pourrait laisser fuir
  une exception technique ; les tests de contrat doivent couvrir ce chemin.
- L'ajout d'un type dans le package `api` l'intègre au contrat public du module
  et exige ensuite une évolution compatible avec ses consommateurs.
- Le singleton local pourrait être activé par erreur dans un environnement non
  local.
- Une organisation peut rester en `CREATING` après un provisioning incomplet ;
  un futur cas d'usage devra permettre sa reprise ou son archivage.
- `SYSTEM` ne permet pas encore d'identifier l'auteur réel d'une modification.
- Une modification accidentelle du scope Maven ou de l'exclusion Modulith
  pourrait faire apparaître l'outillage de test dans l'architecture de
  production ; les contrôles Maven et ArchUnit doivent rester actifs.

## Notes

- La conception d'un `nodeId`, son attribution et la gestion d'horloge en
  environnement distribué sont reportées à l'étape Kubernetes.
- Les permissions par `hasPermission`, l'organisation issue du JWT et le contrôle
  de l'appartenance de l'utilisateur seront traités plus tard avec W008.
- Les champs d'audit de W001 n'enregistrent que la création et le dernier état.
  W012 introduira les premiers événements persistés de création de `Team` et
  `User`. L'historisation complète devra être ajoutée par une extension de W012
  ou par un ticket ultérieur dédié.
