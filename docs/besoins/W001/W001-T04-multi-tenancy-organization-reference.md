# W001-T04 - Multi-tenancy par référence d'organisation

## Statut

Accepted

## Ticket lié

W001-T04 - Multi-tenancy par référence d'organisation

## Source roadmap

- Semaine : W001
- Phase : Phase 1
- Compétence appliquée : bootstrap et persistance multi-tenant
- Objectif TeamPulse : préparer un socle dans lequel les données d'une
  organisation ne peuvent jamais être lues ou modifiées depuis une autre
  organisation.
- Source :
  [`teampulse-roadmap-v9-code-infra-detaille.xlsx`](../../teampulse-roadmap-v9-code-infra-detaille.xlsx),
  feuille `TeamPulse - Exécution`, ligne W001.

Le présent besoin abandonne l'identifiant numérique historique de
l'organisation au profit d'une référence fonctionnelle appelée
`organizationReference` dans les contrats Java et `organization_reference` en
base de données.

## Contexte

TeamPulse est un monolithe modulaire qui héberge plusieurs organisations. Les
modules `organization`, `identity` et `team` possèdent chacun leurs données et
leurs identifiants techniques. Un module ne doit pas utiliser l'identifiant
interne d'une entité appartenant à un autre module comme contrat d'intégration.

L'organisation est la racine du tenant. Sa référence publique devient donc la
clé de rattachement commune aux utilisateurs, équipes et appartenances. Cette
référence remplace l'identifiant numérique de tenant envisagé initialement.

La sécurité JWT n'existe pas encore en W001. Le ticket prépare la frontière de
tenant dans les modèles, les ports et la persistance sans prétendre identifier
ou autoriser réellement l'appelant HTTP.

Le parcours fonctionnel dans lequel cette frontière sera utilisée est décrit
dans le
[`SCENARIO-METIER-TEAMPULSE.md`](../SCENARIO-METIER-TEAMPULSE.md). T04 livre les
verticales internes de domaine, d'application et de persistance nécessaires aux
organisations, utilisateurs, équipes et appartenances. Il ne livre pas par
anticipation leurs contrats HTTP, l'identité de l'acteur, les rôles, campagnes,
permissions JWT ou workflows ultérieurs.

## Problème à résoudre

Utiliser l'identifiant `BIGINT` interne d'une organisation dans tous les modules
créerait un couplage durable avec son modèle de persistance. À l'inverse, rendre
la référence d'organisation facultative permettrait à une opération métier
d'oublier son tenant et exposerait un risque de lecture ou d'écriture croisée.

TeamPulse doit donc disposer :

- d'une référence stable et opaque pour les échanges entre modules ;
- d'un contexte d'organisation obligatoire dans tous les cas d'usage tenantés ;
- de modèles minimaux cohérents pour `Organization`, `User`, `Team` et
  `TeamMember` ;
- de contraintes et de tests qui rendent les erreurs de tenant visibles dès
  W001.

## Objectif

À la fin du ticket :

- chaque organisation possède une référence unique indépendante de son
  identifiant de base de données ;
- tous les modules externes à `tp-organization` utilisent cette référence pour
  désigner l'organisation ;
- les ports applicatifs tenantés exigent explicitement un `TenantContext` ;
- les cas d'usage plateforme restent non tenantés et aucun `PlatformContext`
  vide n'est introduit avant qu'un besoin de sécurité réel ne le justifie ;
- les entités initiales et leurs contraintes de persistance permettent de
  démontrer l'isolation de deux organisations.

## Vocabulaire et conventions

- **Identifiant technique** : `BIGINT` généré par la base, local à la table et
  au module. Il n'est jamais exposé comme référence d'un autre module.
- **Référence fonctionnelle** : valeur textuelle stable, générée côté Java et
  utilisée pour les échanges entre modules.
- **Référence d'organisation** : référence fonctionnelle qui identifie le
  tenant ; elle remplace tout identifiant technique de tenant dans les nouveaux
  contrats.
- **Tenant** : périmètre logique et isolé d'un client dans une application
  multi-tenant. Les données et opérations d'un tenant ne doivent jamais être
  mélangées avec celles d'un autre. Dans TeamPulse W001, une organisation
  représente un tenant.
- **Contexte tenant** : `TenantContext` Java pur contenant obligatoirement une
  `tenantReference`. Ce nom reste générique et réutilisable ; en W001, sa valeur
  concrète est `Organization.reference`.

## Périmètre inclus

- Générateur transverse de références dans `tp-common`, en Java pur.
- `TenantContext(tenantReference)` transverse et Java pur pour les ports
  applicatifs tenantés.
- Références d'organisation, d'utilisateur et d'équipe.
- Modèles minimaux `Organization` et `User`, ainsi que la verticale interne
  complète de `Team` et `TeamMember` définie par T04 : cycles de vie, ports,
  orchestration applicative, persistance et tests.
- Codes d'erreur métier propres aux modules organisation, identité et équipe.
- Propagation obligatoire de `organizationReference` dans les ports tenantés.
- Service local fournissant la référence d'organisation courante pendant W001.
- API Java publique `UserDirectory` fournie par `tp-identity`, permettant
  aux modules organisation et équipe de vérifier un utilisateur sans accéder à
  sa persistance.
- API Java publique `OrganizationDirectory` fournie par
  `tp-organization`, permettant au module équipe de vérifier qu'une organisation
  est opérationnelle sans accéder à son domaine ou à sa persistance.
- Contrats d'erreur internes aux modules et exceptions techniques publiques
  limitées aux APIs `Directory`.
- Tables, clés, contraintes, index et migrations Flyway nécessaires au ticket.
- Champs de version et d'audit technique sur les entités persistées.
- Tests unitaires, applicatifs, d'intégration PostgreSQL et d'architecture
  nécessaires pour valider la frontière tenant.

## Périmètre exclu

- Contrat HTTP utilisateur (`POST /api/users`, `GET /api/users`), contrôleur,
  DTOs, traduction des erreurs HTTP et tests Web, y compris les parcours
  complets avec PostgreSQL : ces éléments seront réalisés dans W001-T05.
- Authentification, émission et validation des JWT.
- Extraction de la référence d'organisation depuis un utilisateur connecté.
- Autorisation par rôle, `hasPermission` et contrôle de l'appartenance de
  l'utilisateur à l'organisation ou à l'équipe.
- Contexte distinct de l'acteur d'une action et contrôle de sa disponibilité ou
  de ses permissions. `TenantContext` reste limité au tenant ; W001-T05
  introduira le contrat d'acteur et ces contrôles, puis W008 leur fournira une
  identité authentifiée issue du JWT.
- Contrôle transversal empêchant le passage d'un utilisateur vers `SUSPENDED`
  ou `DEACTIVATED` tant qu'il porte une responsabilité active. Cette
  orchestration appartient à W001-T05 et ne crée pas de dépendance inverse de
  `tp-identity` vers `tp-organization` ou `tp-team`.
- PostgreSQL Row-Level Security.
- Génération d'un client Java à partir d'un contrat OpenAPI.
- Coordination distribuée du générateur entre plusieurs nœuds Kubernetes.
- Adresses, téléphones, mobiles, fax, contacts et autres informations détaillées
  d'une organisation ; ces données seront portées plus tard par des tables
  annexes.
- Historique complet de toutes les modifications. W012 introduira les premiers
  événements d'audit persistés pour la création de `Team` et `User`, mais ne
  couvre pas à lui seul cet historique complet.
- Attribution réelle de `createdBy` et `modifiedBy` à l'utilisateur authentifié.
- `PlatformContext` vide : les cas d'usage plateforme sont simplement non
  tenantés jusqu'à l'introduction d'un contexte de sécurité justifié par W008.
- Surveillance continue du statut des responsables dans `tp-identity` et
  synchronisation automatique de `OrganizationStatus` ou `TeamStatus` après
  leur affectation.
- Événement `UserAvailabilityChanged`, listeners dans `tp-organization` ou
  `tp-team`, recherches par responsable et gestion des courses inter-modules
  associées.
- Cycle de vie des abonnements et calcul de l'accès effectif à partir de leur
  statut ; un abonnement ne modifie jamais `OrganizationStatus`.

## Règles métier / techniques

### Références fonctionnelles

- Le contrat Java transverse s'appelle `ReferenceFactory` et expose la méthode
  `String generate(String prefix)`.
- Son implémentation monotone s'appelle `MonotonicReferenceFactory`. Son
  caractère non bloquant est un détail interne et n'apparaît pas dans son nom.
- `ReferenceFormat.matches(reference, expectedPrefix)` centralise la validation
  syntaxique des références existantes. Ce contrat Java pur ne connaît aucun
  préfixe métier et retourne `false` lorsque la référence, son format ou le
  préfixe attendu ne correspondent pas.
- Le préfixe reçu est obligatoire et doit respecter exactement `[A-Z]{3}` :
  trois lettres ASCII majuscules, sans normalisation automatique. Une valeur
  nulle, trop courte, trop longue, en minuscules ou entourée d'espaces provoque
  une `IllegalArgumentException`.
- Les préfixes normatifs sont `ORG` pour une organisation, `USR` pour un
  utilisateur et `TEM` pour une équipe. `TeamMember` ne possède pas de
  référence fonctionnelle propre.
- Chaque module métier détient son préfixe dans une constante locale et appelle
  la factory avec cette chaîne. `tp-common` ne définit ni `ReferenceKind`, ni
  `ReferencePrefix` dépendant des métiers.
- Une référence suit le format
  `<TRIGRAMME>-<ANNÉE>-<JOUR_MOIS>-<SUFFIXE>`.
- Avec un trigramme et un suffixe de douze caractères, la référence complète
  contient exactement 26 caractères, séparateurs inclus.
- `JOUR_MOIS` utilise le format UTC `JJMM`, par exemple `0508` pour le 5 août.
- Le suffixe contient exactement douze caractères dans l'alphabet base 36
  majuscule `[0-9A-Z]`, selon le découpage fixe `MMMMMMNNNNCC`.
- `MMMMMM` encode sur six caractères le nombre de millisecondes du temps
  logique écoulées depuis le début de sa journée UTC. `36^6 = 2 176 782 336`
  couvre largement les `86 400 000` millisecondes d'une journée.
- `NNNN` est un nonce de démarrage sur quatre caractères, généré une seule fois
  par cycle de vie de la factory avec `SecureRandom`. Il offre
  `36^4 = 1 679 616` valeurs possibles.
- `CC` est le compteur atomique de la milliseconde logique courante. Ses deux
  caractères couvrent `36^2 = 1 296` valeurs, de `00` à `ZZ`.
- Lorsque le compteur dépasse `ZZ`, le générateur avance le temps logique d'une
  milliseconde et reprend le compteur à `00`, sans attendre l'horloge réelle.
- L'année, `JOUR_MOIS` et la partie temporelle du suffixe sont tous dérivés du
  même instant logique accepté. Un recul de l'horloge ne peut donc pas rendre
  incohérents les différents segments de la référence.
- Le générateur est thread-safe, non bloquant et ne dépend ni de Spring, ni de
  JPA, ni d'un module métier.
- L'horloge est injectée afin de rendre les tests déterministes et de contrôler
  les reculs d'horloge. La source du nonce est également contrôlable dans les
  tests sans affaiblir l'utilisation de `SecureRandom` en production.
- Les références sont stockées dans des colonnes PostgreSQL `TEXT`.
  `ReferenceFormat` centralise leur format et leur longueur côté Java, tandis
  que chaque module choisit le préfixe attendu et traduit un refus dans son
  propre contrat d'erreur. Cette séparation permet une évolution future sans
  migration imposée uniquement par une taille de `VARCHAR`.
- Une unique instance de la factory est utilisée dans l'application. Elle
  garantit l'unicité des références qu'elle émet pendant son cycle de vie dans
  la JVM.
- Après un redémarrage, la garantie devient probabiliste : dans le pire cas où
  la date, la milliseconde logique et le compteur sont identiques, la probabilité
  de réutiliser le même nonce vaut `1 / 36^4`, soit environ `0,0000595 %`.
- La base conserve une contrainte `UNIQUE` sur chaque référence persistée comme
  ultime défense d'intégrité. Une collision n'est pas un chemin nominal et ne
  déclenche pas de boucle de régénération : l'insertion échoue clairement avec
  le code d'erreur métier `REFERENCE_GENERATION_FAILED` du module concerné.
- L'unicité entre plusieurs instances Java nécessitera une identité stable de
  nœud ou une coordination partagée, à décider avant Kubernetes.

### Stratégie de validation

- Chaque règle est garantie par la couche la plus profonde capable de l'évaluer
  correctement. Une validation anticipée dans une couche extérieure ne remplace
  jamais l'invariant porté par sa couche propriétaire.
- Les ports entrants et leurs commandes utilisent Jakarta Validation uniquement
  pour leur contrat structurel : présence des paramètres, valeurs obligatoires
  et validation en cascade. Les modèles du domaine ne portent aucune annotation
  Jakarta Validation.
- Les services applicatifs utilisent `@Validated` pour exécuter les contraintes
  des ports entrants et de leurs commandes au travers du bean Spring proxifié.
  `@Service` assure leur découverte et `@Transactional` leur frontière
  transactionnelle ; ces deux annotations ne définissent aucune règle de
  validation. Tout service applicatif déclaré avec `@Service` porte aussi
  `@Validated` afin que l'activation de la validation ne dépende pas du cas
  d'usage.
- `TenantContext` garantit à la construction une `tenantReference` non nulle et
  non blanche. Les ports tenantés exigent en plus un contexte non nul. Une
  commande comme `CreateUserCommand` porte ses contraintes structurelles et est
  validée en cascade depuis le port entrant.
- Les agrégats sont valides par construction, lors de leur restauration et après
  chaque opération métier. Ils possèdent la normalisation, les longueurs et
  formats métier, les invariants entre champs et les transitions de statut.
- Une vérification nécessitant un repository ou un `Directory` appartient à la
  couche application. La disponibilité d'un responsable est ainsi contrôlée
  ponctuellement par le cas d'usage ; l'agrégat conserve uniquement l'invariant
  structurel durable sur les références exigées par son statut.
- Les validations sont fail-fast par couche. Le service n'effectue aucune
  écriture après un refus connu et ne duplique pas un invariant du domaine pour
  choisir artificiellement l'ordre des erreurs.
- PostgreSQL protège en dernier recours les invariants persistants exprimables
  localement. Les contraintes connues sont traduites explicitement par l'adapter
  dans le code d'erreur du module ; une violation inconnue reste technique.
- Une validation n'entre dans `tp-common` que si elle est identique pour tous
  ses consommateurs, indépendante du métier, sans I/O, sans Spring, sans JPA et
  sans code d'erreur propre à un module. Elle reste placée avec le concept
  protégé, comme `ReferenceFormat` dans `common.reference`.
- Aucun `UserValidator`, `OrganizationValidator`, `CommonValidator` ou
  `ValidationUtils` générique n'est introduit. Une policy applicative dédiée
  n'est extraite que lorsqu'une règle avec I/O est complexe ou réellement
  réutilisée, et son nom décrit alors la décision métier contrôlée.
- Les erreurs de contrat, refus métier, résultats inter-modules et défaillances
  techniques restent distincts. Chaque module consommateur traduit les résultats
  externes dans son propre vocabulaire et conserve les causes techniques.

### Frontière des modules

- `tp-organization` est propriétaire de l'organisation et de son identifiant
  technique.
- `tp-identity` et `tp-team` conservent `organizationReference`, jamais
  l'identifiant technique de l'organisation.
- Une référence provenant d'un autre module est traitée comme une valeur externe
  opaque ; le module consommateur ne tente pas de reconstruire l'entité distante.
- Les ports et repositories tenantés n'exposent aucune opération métier globale.
  Les recherches, listes, contrôles d'unicité et suppressions exigent la
  référence d'organisation ou un `TenantContext` dont la `tenantReference`
  contient cette valeur en W001.
- Le package `io.teampulse.identity.api.user`, déclaré avec
  `@NamedInterface("user")`, expose `UserDirectory`, `UserAvailability` et
  `UserDirectoryException`. Le package parent `api` sert uniquement de namespace
  afin que chaque future capacité publique puisse être exposée séparément.
- `tp-organization` et `tp-team` dépendent uniquement de cette API publique,
  jamais du domaine, de JPA ou de l'infrastructure de `tp-identity`.
- `UserDirectory` exige `organizationReference` et `userReference` pour chaque
  vérification ; aucun accès global n'est exposé.
- Les deux références du contrat `UserDirectory` sont déclarées avec
  `@NotBlank`. Une valeur nulle, vide ou blanche est une violation du contrat du
  consommateur, évaluée avant l'appel au repository ; elle ne retourne pas
  `NOT_FOUND` et n'est pas traduite en `UserDirectoryException`.
- `UserAvailability` contient `AVAILABLE`, `PENDING`, `UNAVAILABLE` et
  `NOT_FOUND`.
- `ACTIVE` devient `AVAILABLE`, `INVITED` et `CREATING` deviennent `PENDING`,
  `SUSPENDED` et `DEACTIVATED` deviennent `UNAVAILABLE`.
- Un utilisateur absent ou appartenant à une autre organisation retourne
  `NOT_FOUND` afin de ne pas révéler son existence.
- Le package `io.teampulse.organization.api.organization`, déclaré avec
  `@NamedInterface("organization")`, expose `OrganizationDirectory`,
  `OrganizationAvailability` et `OrganizationDirectoryException`. Le package
  parent `api` sert uniquement de namespace.
- `OrganizationDirectory` expose
  `OrganizationAvailability check(String organizationReference)` ; aucune liste
  globale d'organisations n'est fournie.
- `OrganizationAvailability` contient `AVAILABLE`, `UNAVAILABLE` et `NOT_FOUND`.
- Une organisation `ACTIVE` devient `AVAILABLE` ; une organisation `CREATING`,
  `SUSPENDED` ou `ARCHIVED` devient `UNAVAILABLE` ; une référence inexistante
  devient `NOT_FOUND`.
- `tp-team` dépend uniquement de cette API publique et ne connaît ni
  `Organization`, ni `OrganizationStatus`, ni le repository ou l'entité JPA de
  `tp-organization`.
- Les résultats métier prévisibles (`AVAILABLE`, `PENDING`, `UNAVAILABLE` et
  `NOT_FOUND` selon le contrat) sont retournés par les enums d'availability ; ils
  ne sont pas représentés par une exception inter-module.
- Une incapacité technique du module fournisseur à répondre est traduite à la
  frontière publique en `UserDirectoryException` ou
  `OrganizationDirectoryException`. Ces exceptions non vérifiées ne révèlent
  ni exception JPA, ni code d'erreur métier interne.
- Le module consommateur peut traduire cette défaillance publique dans son
  propre code d'erreur en conservant l'exception d'origine comme cause.
- Aucun client OpenAPI n'est introduit en W001.

### Contexte local W001

- Le contrat transverse Java pur `TenantContextProvider` appartient au package
  `io.teampulse.common.context`, exposé aux modules métier par l'interface
  Spring Modulith nommée `common::context` :

  ```java
  public interface TenantContextProvider {
      TenantContext current();
  }
  ```

- Les futurs contrôleurs tenantés dépendent uniquement de ce contrat et
  transmettent le `TenantContext` retourné par `current()` au cas d'usage.
- `LocalTenantContextProvider`, placé dans `tp-app`, implémente la stratégie de
  bootstrap W001. Il reçoit le bean `ReferenceFactory` fourni par
  `ReferenceConfiguration` et n'instancie pas directement
  `MonotonicReferenceFactory`.
- `LocalTenantConfiguration`, également placée dans `tp-app`, enregistre ce
  provider uniquement sous le profil Spring `local`. Le bean est singleton par
  défaut dans le contexte Spring.
- Au premier appel à `current()`, le provider génère une référence avec le
  préfixe `ORG`, construit un unique `TenantContext`, puis retourne la même
  instance pendant toute l'exécution. Cette initialisation lazy est thread-safe
  afin que des appels concurrents ne déclenchent qu'une génération.
- Si la génération échoue ou produit une référence refusée par `TenantContext`,
  l'erreur est propagée, aucun contexte n'est mémorisé et un appel ultérieur peut
  retenter la résolution. Aucun tenant de secours n'est fabriqué.
- Cette valeur n'est pas conservée entre deux redémarrages et aucune organisation
  locale n'est insérée artificiellement par Flyway uniquement pour fournir un
  tenant de démonstration.
- La stratégie est limitée au développement local. Hors du profil `local`,
  aucun faux provider partagé n'est enregistré et une vraie stratégie devra
  être fournie avant de câbler des contrôleurs tenantés en production.
- `TenantContext` rend la présence du tenant explicite mais ne prouve ni
  l'identité de l'appelant, ni ses permissions. W008 remplacera
  `LocalTenantContextProvider` par une implémentation résolvant le contexte
  depuis un JWT validé, sans modifier les contrôleurs ni les cas d'usage.
- Un contrôleur tenanté n'accepte aucune `organizationReference` ou
  `tenantReference` libre dans le body, le path, la query ou un header, ne
  génère aucune référence et n'interroge pas directement `tp-organization` pour
  déterminer le tenant courant.
- Les cas d'usage plateforme ne reçoivent aucun `TenantContext`. Aucun
  `PlatformContext` vide n'est créé en W001, car il ne transporterait encore
  aucune identité ou information fiable.

### Modèle minimal de l'organisation

Le modèle de domaine `Organization` contient uniquement l'état métier :

- `reference` ;
- `name` ;
- `timezone` ;
- `adminReference` ;
- `managerReference` ;
- `status`.

L'identifiant technique n'appartient pas au modèle de domaine.
`OrganizationEntity` persiste cet état métier et porte en plus l'état technique
suivant :

- `id` ;
- `version` ;
- `createdAt` et `createdBy` ;
- `modifiedAt` et `modifiedBy`.

Règles associées :

- le nom est obligatoire, normalisé avec `strip()`, non vide après
  normalisation et limité à 200 caractères ;
- la casse et les espaces internes du nom sont conservés ;
- toute commande applicative qui porte le nom applique la même limite à sa
  valeur normalisée, sans introduire une règle divergente du domaine ;
- `CREATING` désigne une organisation en cours de constitution et remplace le
  terme `PROVISIONING` ;
- `ACTIVE` désigne une organisation opérationnelle ;
- `SUSPENDED` désigne une organisation temporairement bloquée ;
- `ARCHIVED` désigne une organisation fermée et conservée pour l'historique ;
- les transitions autorisées sont `CREATING -> ACTIVE`,
  `CREATING -> ARCHIVED`, `ACTIVE -> SUSPENDED`, `ACTIVE -> ARCHIVED`,
  `SUSPENDED -> ACTIVE` et `SUSPENDED -> ARCHIVED` ;
- `ARCHIVED` est un statut terminal ;
- la timezone utilise un identifiant IANA valide, compatible avec
  `java.time.ZoneId` ;
- les responsables respectent les contraintes suivantes :

  | Statut | Administrateur | Manager |
  | --- | --- | --- |
  | `CREATING` | optionnel | optionnel |
  | `ACTIVE` | obligatoire et validé `AVAILABLE` | obligatoire et validé `AVAILABLE` |
  | `SUSPENDED` | référence obligatoire | éventuellement absent |
  | `ARCHIVED` | éventuellement absent | éventuellement absent |

- le passage vers `ACTIVE`, la réactivation et le remplacement direct d'un
  responsable exigent que les utilisateurs concernés soient retournés
  `AVAILABLE` par `UserDirectory` au moment de l'exécution du cas d'usage ;
- après la transaction, `tp-organization` ne surveille pas continuellement
  `tp-identity` ; une organisation `ACTIVE` conserve seulement l'invariant
  structurel de deux références non nulles ;
- l'administrateur ne peut jamais être supprimé sans remplacement direct ;
- le manager peut être retiré sans remplacement ; son retrait depuis `ACTIVE`
  retire la référence et réalise atomiquement la transition vers `SUSPENDED` ;
- remplacer directement un responsable ne change pas le statut ; affecter ou
  remplacer un manager en `SUSPENDED` ne réactive pas implicitement
  l'organisation ;
- une même personne peut remplir les deux fonctions ;
- une organisation `CREATING` peut être archivée sans responsables ;
- l'archivage conserve toutes les références déjà présentes et aucune
  responsabilité ne peut être modifiée après le passage terminal à `ARCHIVED` ;
- la suspension ou la désactivation ultérieure d'un responsable dans
  `tp-identity` ne suspend pas automatiquement l'organisation dans T04 ;
- le statut d'un abonnement n'entraîne aucune transition de
  `OrganizationStatus` ;
- les adresses et moyens de contact ne sont pas ajoutés dans la table principale.

### Modèle minimal de l'utilisateur

Le modèle de domaine `User` contient uniquement les données utiles aux règles
métier :

- `reference` ;
- `organizationReference` ;
- `email` ;
- `firstName` et `lastName` ;
- `status`.

L'entité de persistance `UserEntity` porte en plus les données techniques :

- `id` ;
- `version` ;
- `createdAt` et `createdBy` ;
- `modifiedAt` et `modifiedBy`.

Ces données techniques ne participent ni aux invariants ni aux transitions de
`User`. L'adapter de persistance les conserve lors du mapping entre le domaine
et JPA.

Règles associées :

- `INVITED` désigne une invitation qui n'est pas encore acceptée ;
- `CREATING` désigne un compte en cours de finalisation ;
- `ACTIVE` désigne un utilisateur opérationnel ;
- `SUSPENDED` désigne un accès temporairement bloqué ;
- `DEACTIVATED` désigne un compte définitivement désactivé ;
- une création directe commence en `CREATING` ;
- une invitation commence en `INVITED` ;
- les transitions autorisées sont `INVITED -> CREATING`,
  `INVITED -> DEACTIVATED`, `CREATING -> ACTIVE`,
  `CREATING -> DEACTIVATED`, `ACTIVE -> SUSPENDED`,
  `ACTIVE -> DEACTIVATED`, `SUSPENDED -> ACTIVE` et
  `SUSPENDED -> DEACTIVATED` ;
- `DEACTIVATED` est un statut terminal ;
- l'email est obligatoire, débarrassé de ses espaces périphériques puis
  converti en minuscules avec `Locale.ROOT` ; sa forme canonique contient
  exactement un `@`, une partie locale et un domaine non vides, aucun espace et
  au plus 254 caractères ;
- l'unicité de l'email est vérifiée sur cette forme canonique dans le périmètre
  de `organizationReference` ;
- `firstName` et `lastName` sont obligatoires, débarrassés de leurs espaces
  périphériques et limités chacun à 100 caractères ; leur casse et leurs
  espaces internes sont conservés.

### Modèle minimal de l'équipe

`Team` contient au minimum :

- `id` ;
- `reference` ;
- `organizationReference` ;
- `name` ;
- `status` ;
- `adminReference` ;
- `managerReference` ;
- `version` ;
- `createdAt` et `createdBy` ;
- `modifiedAt` et `modifiedBy`.

Contrairement à `Organization` et `User`, le modèle de domaine `Team` conserve
son `id` technique, car `TeamMember` l'utilise comme relation interne au même
module. La version et les champs d'audit restent exclusivement portés par
`TeamEntity`. Le nom de l'équipe est obligatoire, normalisé avec `strip()`, non
blanc après normalisation et limité à 200 caractères. Sa casse et ses espaces
internes sont conservés. Deux équipes peuvent porter le même nom, y compris dans
une même organisation.

Une équipe possède exactement un administrateur et un manager. Une même personne
peut remplir les deux fonctions. Les deux utilisateurs doivent appartenir à la
même organisation que l'équipe. Ces responsabilités sont indépendantes de
l'appartenance : l'administrateur et le manager peuvent chacun posséder ou non
une ligne `TeamMember` dans l'équipe. Leur affectation ne crée aucune
appartenance automatiquement.

Une équipe est créée directement en `ACTIVE` uniquement lorsque son organisation
est retournée `AVAILABLE` par `OrganizationDirectory` et que ses deux
responsables sont retournés `AVAILABLE` par `UserDirectory`. Les valeurs
`UNAVAILABLE` et `NOT_FOUND` fournies par `OrganizationDirectory` interdisent la
création. `SUSPENDED` bloque temporairement son activité et `ARCHIVED` la
ferme tout en conservant son historique. Les transitions autorisées sont
`ACTIVE -> SUSPENDED`, `ACTIVE -> ARCHIVED`, `SUSPENDED -> ACTIVE` et
`SUSPENDED -> ARCHIVED`. `ARCHIVED` est terminal.

La réactivation d'une équipe exige ponctuellement que son organisation, son
administrateur et son manager soient tous retournés `AVAILABLE`. Elle est
refusée sinon et l'équipe reste `SUSPENDED`. L'administrateur et le manager
peuvent être remplacés lorsque l'équipe est `ACTIVE` ou `SUSPENDED`. Le
remplaçant doit être `AVAILABLE` dans la même organisation, le remplacement ne
change pas le statut et une même personne peut continuer à porter les deux
responsabilités. Aucun responsable ne peut être simplement retiré : le transfert
vers un remplaçant est atomique afin que les deux références restent toujours
présentes. Une équipe `ARCHIVED` est immuable.

Une évolution ultérieure du statut d'un responsable ne modifie pas
automatiquement `TeamStatus`. `tp-organization` n'envoie aucune notification de
suspension ou de réactivation à `tp-team` dans T04. La création, la réactivation
et le remplacement restent des contrôles synchrones et ponctuels au travers des
directories publics.

### Modèle minimal de l'appartenance

`TeamMember` appartient au même module que `Team`. Il relie donc l'équipe par son
`teamId` technique, utilisé comme clé étrangère interne, et l'utilisateur par sa
`userReference`, car celui-ci appartient au module identité. Il porte également
`organizationReference` et un statut afin que les requêtes et contraintes
d'isolation ne dépendent pas d'une jointure préalable. Une même association non
terminée équipe/utilisateur ne peut exister qu'une seule fois dans une
organisation.

`TeamMember` contient au minimum :

- `id` ;
- `organizationReference` ;
- `teamId` ;
- `userReference` ;
- `status` ;
- `startedAt` ;
- `endedAt` ;
- `version` ;
- `createdAt` et `createdBy` ;
- `modifiedAt` et `modifiedBy`.

Le modèle de domaine `TeamMember` conserve son `id` et son `teamId`, nécessaires
à son identité et à sa relation internes au module. Sa version et ses champs
d'audit restent exclusivement portés par `TeamMemberEntity`.

`TeamMember` n'a pas de référence fonctionnelle propre : il reste une entité
interne de l'équipe et n'est pas adressé indépendamment par les autres modules.

La clé étrangère composite `(teamId, organizationReference)` garantit que
l'équipe et l'appartenance relèvent du même tenant. Aucun `userId` technique du
module identité et aucune `teamReference` ne sont stockés dans `TeamMember`.

- `INVITED` désigne une invitation dans l'équipe ;
- `ACTIVE` désigne une appartenance opérationnelle ;
- `SUSPENDED` désigne une appartenance temporairement bloquée ;
- `REMOVED` désigne une appartenance terminée et conservée pour l'historique ;
- les transitions autorisées sont `INVITED -> ACTIVE`, `INVITED -> REMOVED`,
  `ACTIVE -> SUSPENDED`, `ACTIVE -> REMOVED`, `SUSPENDED -> ACTIVE` et
  `SUSPENDED -> REMOVED` ;
- `REMOVED` est un statut terminal.

Une invitation conserve `startedAt` à `null`, car l'utilisateur n'est pas
encore membre effectif. Une création directe en `ACTIVE` renseigne `startedAt`
immédiatement ; une transition `INVITED -> ACTIVE` le renseigne à la date
d'activation. Une suspension ne modifie pas cette date. `endedAt` reste à
`null` dans les statuts `INVITED`, `ACTIVE` et `SUSPENDED`, puis reçoit la date
de fin lors du passage à `REMOVED`. Une invitation retirée avant activation
conserve donc `startedAt = null` tout en renseignant `endedAt`. Lorsque
`startedAt` existe, `endedAt` ne peut pas lui être antérieur. Une réactivation
ne modifie jamais la date de début initiale.

Une appartenance créée directement en `ACTIVE` exige un utilisateur
`AVAILABLE`. Une appartenance `INVITED` peut cibler un utilisateur `AVAILABLE`
ou `PENDING`. L'activation d'une invitation et la réactivation d'une
appartenance suspendue revérifient ponctuellement que l'utilisateur est
`AVAILABLE`. La suspension et le retrait ne dépendent pas de sa disponibilité.
Les valeurs `UNAVAILABLE` et `NOT_FOUND` sont refusées lorsqu'une opération ouvre
ou rétablit l'accès.

Les opérations sur les appartenances dépendent également des statuts de leur
équipe et de leur organisation :

- une équipe `ACTIVE` accepte toutes les opérations autorisées par le statut du
  membre ;
- une équipe `SUSPENDED` accepte uniquement la suspension et le retrait d'un
  membre ;
- une équipe `ARCHIVED` n'accepte aucune mutation d'appartenance ;
- une organisation `UNAVAILABLE` interdit l'ajout, l'invitation, l'activation et
  la réactivation, mais n'empêche pas la suspension ou le retrait d'un membre.

Ces règles autorisent les opérations qui réduisent ou ferment un accès, même
lorsque le périmètre est suspendu. Elles n'entraînent aucune modification en
cascade des statuts existants.

Une seule appartenance `INVITED`, `ACTIVE` ou `SUSPENDED` peut exister pour le
même triplet `(organizationReference, teamId, userReference)`. Une réinvitation
après `REMOVED` crée une nouvelle ligne avec un nouvel `id` interne,
`startedAt = null` et `endedAt = null` ; l'ancienne ligne et sa période restent
conservées.

### Règles communes de cycle de vie

- Les statuts ne sont pas modifiés librement ; chaque transition passe par une
  opération métier explicite.
- Une transition absente des listes précédentes est refusée par le domaine.
- Suspendre une organisation ne modifie pas en masse les statuts de ses
  utilisateurs, équipes ou appartenances.
- Suspendre une équipe ne modifie pas les statuts de ses membres.
- Les statuts terminaux conservent les données et leur audit sans suppression
  physique automatique.

### Préparation du contrôle des responsabilités utilisateur

T04 conserve `TenantContext` limité à la seule `tenantReference` et
n'introduit pas de contexte d'acteur. W001-T05 portera l'orchestration qui
précède une transition de `User` vers `SUSPENDED` ou `DEACTIVATED` :

- rechercher les responsabilités d'administrateur ou de manager détenues dans
  `tp-organization` et `tp-team` au travers de contrats publics dédiés ;
- refuser la transition tant qu'une responsabilité active n'a pas été retirée
  ou transférée ;
- considérer comme actives les responsabilités d'une organisation `CREATING`,
  `ACTIVE` ou `SUSPENDED`, et celles d'une équipe `ACTIVE` ou `SUSPENDED` ;
- ignorer pour ce blocage les références conservées sur une organisation ou une
  équipe `ARCHIVED`, car elles sont uniquement historiques ;
- ne pas bloquer une transition pour une simple ligne `TeamMember` sans
  responsabilité d'administrateur ou de manager ;
- ne jamais faire dépendre `tp-identity` des domaines ou persistances de
  `tp-organization` et `tp-team`.

Le remplacement d'un responsable reste possible sur une organisation ou une
équipe `SUSPENDED` afin de permettre le départ d'un utilisateur. W001-T05
introduira un acteur explicite, distinct du tenant, et vérifiera sa disponibilité
ainsi que ses permissions applicatives. En W001, cet acteur proviendra d'une
source applicative contrôlée sans constituer une preuve d'authentification ;
W008 remplacera cette source par l'identité issue d'un JWT validé.

### Erreurs métier et défaillances inter-modules

Chaque module subdivise sa couche `domain` par domaine fonctionnel. Dans chaque
sous-domaine, les packages `model` et `error` sont placés au même niveau sous
`domain.<domaine>`. Le domaine propriétaire possède une enum de codes d'erreur
stable et une seule exception métier non vérifiée qui transporte obligatoirement
un code, un message de diagnostic interne et, si nécessaire, une cause. Les
enums ne portent ni message utilisateur, ni statut HTTP et ne sont pas placées
dans `tp-common`. L'adapter web du module décidera ultérieurement de leur
représentation externe sans exposer directement le message interne de
l'exception.

```text
io.teampulse.organization.domain.organization.error
├── OrganizationErrorCode
└── OrganizationException

io.teampulse.identity.domain.user.error
├── UserErrorCode
└── UserException

io.teampulse.team.domain.team.error
├── TeamErrorCode
└── TeamException
```

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

`ACTIVATION_REQUIREMENTS_NOT_MET` couvre l'absence des références nécessaires à
la transition vers `ACTIVE`. `INVALID_NAME` couvre un nom nul, blanc après
`strip()` ou supérieur à 200 caractères.

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

`EMAIL_ALREADY_USED` est évalué dans le périmètre de
`organizationReference`.

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

Les erreurs de `TeamMember` restent dans `TeamErrorCode`, car l'appartenance
appartient au module `tp-team`. Lorsqu'un utilisateur `AVAILABLE` est exigé,
`*_NOT_AVAILABLE` regroupe les résultats `PENDING` et `UNAVAILABLE`, tandis que
`*_NOT_FOUND` reste distinct.

`ORGANIZATION_UNAVAILABLE` signifie que l'organisation existe mais que son
statut interdit l'opération. `ORGANIZATION_DIRECTORY_UNAVAILABLE` et
`USER_DIRECTORY_UNAVAILABLE` signalent au contraire que le contrat public n'a
pas pu répondre pour une raison technique. Une exception JPA ou Spring est
traduite avant de franchir la frontière du module fournisseur ; elle ne devient
jamais une dépendance du module consommateur.

`TEAM_UNAVAILABLE` signifie que l'équipe existe dans le tenant mais que son
statut `SUSPENDED` ou `ARCHIVED` interdit l'opération demandée.
`INVALID_STATUS_TRANSITION` reste réservé à une transition invalide du cycle de
vie de l'équipe elle-même.

### Persistance, concurrence et audit

- Les identifiants techniques utilisent PostgreSQL `BIGINT` et Java `Long`.
- Toutes les références fonctionnelles sont uniques dans leur périmètre. Elles
  sont non nulles, sauf les références des responsables d'une organisation
  lorsque son statut autorise explicitement leur absence.
- Toute donnée tenantée porte `organization_reference TEXT NOT NULL`.
- `organizations.name` utilise `TEXT NOT NULL` et une contrainte
  `char_length(name) BETWEEN 1 AND 200`. La valeur persistée est le nom normalisé
  par le domaine ; sa casse et ses espaces internes sont conservés.
- `teams.name` utilise également `TEXT NOT NULL` et une contrainte
  `char_length(name) BETWEEN 1 AND 200`. Le nom normalisé n'est pas unique dans
  l'organisation.
- Les champs Java `adminReference`, `managerReference` et `userReference` sont
  persistés respectivement dans `admin_reference`, `manager_reference` et
  `user_reference`. La convention tenant reste `organizationReference` vers
  `organization_reference`. Si une référence d'administrateur plateforme est
  ajoutée ultérieurement, elle suivra la même règle : `platformAdminReference`
  en Java et `platform_admin_reference` en SQL ; T04 n'introduit pas ce champ.
- `organizations.admin_reference` et `organizations.manager_reference` restent
  des colonnes nullables. Des contraintes dépendantes du statut imposent
  `admin_reference` dans `ACTIVE` et `SUSPENDED`, et `manager_reference` dans
  `ACTIVE`, tout en autorisant les absences définies pour `CREATING`,
  `SUSPENDED` et `ARCHIVED`.
- PostgreSQL ne tente pas de vérifier `AVAILABLE`, car cette information
  appartient à `tp-identity`. Cette précondition est contrôlée ponctuellement
  par les cas d'usage avec `UserDirectory`.
- Les contraintes temporelles de `team_members` imposent la cohérence de
  `started_at` et `ended_at` avec le statut. Lorsque les deux dates existent,
  `ended_at` ne peut pas précéder `started_at`.
- `version` est un `BIGINT` utilisé pour le verrouillage optimiste. Le choix
  ponctuel d'un verrou pessimiste reste une décision explicite d'un cas d'usage,
  pas le comportement par défaut.
- `createdAt`, `createdBy`, `modifiedAt` et `modifiedBy` sont présents dès W001.
- En l'absence d'authentification, `createdBy` et `modifiedBy` valent `SYSTEM`.
- Pour `Organization` et `User`, `id`, `version` et les quatre champs d'audit
  restent exclusivement dans leurs entités JPA. Les modèles de domaine sont
  restaurés à partir des seules données métier ; les adapters mettent à jour les
  entités JPA gérées afin de préserver l'identifiant, la version et l'audit.
- Le dispositif conserve uniquement la création et la dernière modification de
  la ligne. W012 introduira les premiers événements d'audit persistés pour la
  création de `Team` et `User`.
- L'historisation complète de toutes les modifications, avec l'entité, l'action,
  l'ancien et le nouvel état, l'auteur, la date et le tenant concernés, devra
  étendre W012 ou faire l'objet d'un ticket ultérieur dédié.
- Aucun `BaseEntity` JPA partagé n'est placé dans `tp-common`. Chaque module
  garde son mapping de persistance et le domaine reste indépendant de JPA.

## Critères d'acceptation

Les commandes suivantes ont été exécutées avec succès le 16 septembre 2026 :

- `./mvnw --batch-mode --no-transfer-progress -pl tp-identity verify` (102 tests),
- `./mvnw --batch-mode --no-transfer-progress -pl tp-organization verify` (217 tests),
- `./mvnw --batch-mode --no-transfer-progress -pl tp-team verify` (152 tests) et
- `./mvnw --batch-mode --no-transfer-progress verify` (576 tests, 0 échec, 0 erreur).


- [x] `ReferenceFactory` expose `String generate(String prefix)` et `MonotonicReferenceFactory` en fournit l'implémentation Java pure.
  - Preuve : `MonotonicReferenceFactory` et `MonotonicReferenceFactoryTest`.
- [x] `ReferenceFormat.matches(reference, expectedPrefix)` centralise le format technique sans connaître `ORG`, `USR` ou `TEM`, et les modèles métier conservent le choix du préfixe ainsi que leur politique d'erreur.
  - Preuve : `ReferenceFormat` et `ReferenceFormatTest`.
- [x] La factory accepte un préfixe conforme à `[A-Z]{3}` et rejette par `IllegalArgumentException` les valeurs nulles, mal dimensionnées, en minuscules ou contenant des espaces, sans les normaliser.
  - Preuve : `MonotonicReferenceFactoryTest.PrefixValidationTests`.
- [x] Le générateur produit le format `<TRIGRAMME>-<ANNÉE>-<JOUR_MOIS>-<SUFFIXE>` pour les préfixes `ORG`, `USR` et `TEM`, avec `JOUR_MOIS` au format UTC `JJMM` et un suffixe alphanumérique de douze caractères, soit 26 caractères au total.
  - Preuve : `MonotonicReferenceFactoryTest.FormattingTests` et `UserLifecycleServiceTest`.
- [x] Le suffixe suit exactement `MMMMMMNNNNCC` : six caractères base 36 pour les millisecondes logiques depuis le début de la journée UTC, quatre pour le nonce de démarrage de la JVM et deux pour le compteur de `00` à `ZZ`.
  - Preuve : `MonotonicReferenceFactoryTest.LogicalStateTests`.
- [x] Le nonce est généré une seule fois par factory avec `SecureRandom` et reste stable pendant tout son cycle de vie.
  - Preuve : `MonotonicReferenceFactoryTest.ConstructionTests`.
- [x] La 1 297e génération dans une même milliseconde logique avance le temps logique d'une milliseconde et produit un compteur `00` sans doublon.
  - Preuve : `MonotonicReferenceFactoryTest.LogicalStateTests`.
- [x] L'année, `JOUR_MOIS` et le suffixe temporel proviennent du même instant logique, y compris lorsque l'horloge recule.
  - Preuve : `MonotonicReferenceFactoryTest.LogicalStateTests`.
- [x] Des générations concurrentes dans une même JVM ne produisent aucun doublon.
  - Preuve : `MonotonicReferenceFactoryTest.ConcurrencyTests`.
- [x] Les tests contrôlent plusieurs références générées dans une même milliseconde ainsi qu'un recul de l'horloge.
  - Preuve : `MonotonicReferenceFactoryTest.LogicalStateTests`.
- [x] Chaque référence persistée possède une contrainte d'unicité en base.
  - Preuve : migrations `V0.1.0__create_users.sql`, `V0.1.0__create_organizations.sql`, `V0.1.0__create_teams.sql` et ITs `JpaUserRepositoryAdapterIT`, `JpaOrganizationRepositoryAdapterIT`, `JpaTeamRepositoryAdapterIT`.
- [x] Une collision détectée lors de la persistance ne remplace jamais une donnée existante, ne déclenche aucune boucle de retry et produit `REFERENCE_GENERATION_FAILED` dans le module concerné.
  - Preuve : `JpaUserRepositoryAdapterTest`, `JpaOrganizationRepositoryAdapterTest`, `JpaTeamRepositoryAdapterTest` et leurs ITs PostgreSQL.
- [x] Les modules identité et équipe ne stockent ni n'exposent l'identifiant technique de l'organisation.
  - Preuve : `ModulithArchitectureTests`, `JpaUserRepositoryQueryArchitectureTest` et `JpaTeamRepositoryQueryArchitectureTest`.
- [x] Une donnée tenantée ne peut pas être persistée sans `organization_reference`.
  - Preuve : migrations identité et équipe ; `JpaUserRepositoryAdapterIT.TenantIsolationTests`.
- [x] Une recherche effectuée avec la référence de l'organisation A ne retourne aucune donnée de l'organisation B.
  - Preuve : `JpaUserRepositoryAdapterIT.TenantIsolationTests`, `ListUsersServiceIT` et `TeamMembershipServiceIT.isolatesMembershipOperationsFromAnotherTenant()` sur PostgreSQL/Testcontainers.
- [x] Tout port applicatif tenanté exige un `TenantContext` non nul dont la `tenantReference` est non nulle, non vide et non blanche, sans normalisation implicite.
  - Preuve : `TenantContextTest`, `TenantedInputPortsArchitectureTest` identité/équipe et ITs de services tenantés.
- [x] En W001, la `tenantReference` reçue par un cas d'usage tenanté correspond à `Organization.reference`, puis est transmise aux ports sortants sous le nom métier `organizationReference`.
  - Preuve : `UserLifecycleServiceTest.passesTenantReferenceAsOrganizationReference()` et `ListUsersServiceTest.callsFindAllWithTenantReference()`.
- [x] `TenantContextProvider.current()` est un contrat Java pur de `common::context`, utilisable par tous les contrôleurs tenantés sans dépendance à HTTP, Spring Security ou `Organization`.
  - Preuve : `TenantContextProvider`, `common/context/package-info.java` et `ModulithArchitectureTests`.
- [x] Aucun `PlatformContext` vide n'est ajouté ; un cas d'usage plateforme reste un contrat non tenanté jusqu'à W008.
  - Preuve : `TenantContextProvider`, `LocalTenantConfigurationTest` et
    `rg -n 'PlatformContext' tp-*/src/main` sans résultat le 16 septembre 2026.
- [x] Sous le profil `local`, un unique `LocalTenantContextProvider` réutilise le bean `ReferenceFactory`, génère `ORG` au premier appel et retourne la même instance de `TenantContext` pendant toute l'exécution.
  - Preuve : `LocalTenantContextProviderTest` et `LocalTenantConfigurationTest`.
- [x] L'initialisation locale est lazy et thread-safe : des appels concurrents ne provoquent qu'une génération.
  - Preuve : `LocalTenantContextProviderTest`.
- [x] Un échec de génération ou une référence invalide n'est jamais mémorisé, ne produit aucun tenant de secours et permet une nouvelle tentative.
  - Preuve : `LocalTenantContextProviderTest`.
- [x] Hors du profil `local`, aucun `LocalTenantContextProvider` n'est enregistré.
  - Preuve : `LocalTenantConfigurationTest`.
- [x] Le nom d'une organisation est obligatoire, normalisé avec `strip()`, non vide et limité à 200 caractères après normalisation ; sa casse et ses espaces internes sont conservés.
  - Preuve : `OrganizationTest.ValidationTests` et `JpaOrganizationRepositoryAdapterIT.DatabaseConstraintTests`.
- [x] Le domaine reste l'unique source de la limite métier de 200 caractères. Les cas d'usage en observent le refus sans dupliquer cette règle avec Jakarta Validation, et PostgreSQL la protège en dernier recours.
  - Preuve : `OrganizationTest.ValidationTests`, `OrganizationLifecycleServiceIT` et migration `V0.1.0__create_organizations.sql`.
- [x] Une organisation `CREATING` peut être persistée sans responsables, mais ne peut devenir `ACTIVE` qu'après validation de son administrateur et de son manager dans la même organisation.
  - Preuve : `OrganizationLifecycleServiceTest`, `OrganizationResponsibleUsersServiceIT` et `JpaOrganizationRepositoryAdapterIT.DatabaseConstraintTests`.
- [x] Une organisation `ACTIVE` possède deux références non nulles ; une organisation `SUSPENDED` conserve obligatoirement son administrateur mais peut ne plus avoir de manager.
  - Preuve : `OrganizationTest`, `OrganizationLifecycleServiceTest` et migration `V0.1.0__create_organizations.sql`.
- [x] L'administrateur ne peut être supprimé sans remplacement direct ; le manager peut être retiré et son retrait depuis `ACTIVE` produit atomiquement `ACTIVE -> SUSPENDED`.
  - Preuve : `OrganizationTest.AdministratorAssignmentTests`, `OrganizationTest.ManagerRemovalTests` et `OrganizationResponsibleUsersServiceTest`.
- [x] Le remplacement direct d'un responsable ne modifie pas le statut et exige que le nouvel utilisateur soit `AVAILABLE` au moment du cas d'usage.
  - Preuve : `OrganizationResponsibleUsersServiceTest`, `OrganizationResponsibleUsersValidatorTest` et leurs ITs.
- [x] Une organisation `CREATING` peut être archivée sans responsables ; tout archivage conserve les références présentes et `ARCHIVED` interdit toute transition ou modification ultérieure des responsabilités.
  - Preuve : `OrganizationTest.ArchivalTests`, `OrganizationTest.ArchivedTerminalStateTests` et `OrganizationLifecycleServiceTest`.
- [x] Les transitions autorisées et interdites de `Organization`, `User`, `Team` et `TeamMember` sont couvertes par des tests de domaine.
  - Preuve : `OrganizationTest`, `UserTest`, `TeamTest` et `TeamMemberTest`.
- [x] Suspendre une organisation ou une équipe ne modifie pas en cascade les statuts de leurs entités enfants.
  - Preuve : `OrganizationLifecycleServiceTest` et `TeamLifecycleServiceTest`.
- [x] L'API publique `UserDirectory` permet aux modules organisation et équipe de contrôler un utilisateur sans dépendre du domaine ou de la persistance du module identité.
  - Preuve : `UserDirectory`, `UserDirectoryServiceIT` et `ModulithArchitectureTests`.
- [x] `UserDirectory`, `UserAvailability` et `UserDirectoryException` sont exposés par `io.teampulse.identity.api.user` via l'interface nommée `identity::user`.
  - Preuve : `identity/api/user/package-info.java` et `ModulithArchitectureTests`.
- [x] `UserDirectory` refuse toute référence d'organisation ou d'utilisateur nulle, vide ou blanche avant l'appel au repository, sans retourner `NOT_FOUND` ni lever `UserDirectoryException`.
  - Preuve : `UserDirectoryServiceIT.rejectsInvalidReferencesAsContractViolations()`.
- [x] L'API publique `OrganizationDirectory` permet au module équipe de vérifier une organisation sans dépendre du domaine ou de la persistance du module organisation.
  - Preuve : `OrganizationDirectory`, `OrganizationDirectoryServiceIT` et `ModulithArchitectureTests`.
- [x] `OrganizationDirectory`, `OrganizationAvailability` et `OrganizationDirectoryException` sont exposés par `io.teampulse.organization.api.organization` via l'interface nommée `organization::organization`.
  - Preuve : `organization/api/organization/package-info.java` et `ModulithArchitectureTests`.
- [x] `OrganizationDirectory` retourne `AVAILABLE` pour `ACTIVE`, `UNAVAILABLE` pour `CREATING`, `SUSPENDED` et `ARCHIVED`, et `NOT_FOUND` pour une référence inexistante.
  - Preuve : `OrganizationDirectoryServiceTest` et `OrganizationDirectoryServiceIT`.
- [x] `OrganizationDirectory` déduit cette valeur du seul `OrganizationStatus` persisté et ne rappelle pas `UserDirectory`.
  - Preuve : `OrganizationDirectoryServiceTest`.
- [x] Le nom d'une équipe est normalisé avec `strip()`, non blanc et limité à 200 caractères, en conservant sa casse et ses espaces internes ; aucun invariant d'unicité n'est imposé sur ce nom.
  - Preuve : `TeamTest` et migration `V0.1.0__create_teams.sql`.
- [x] La création d'une équipe est refusée lorsque son organisation est `UNAVAILABLE` ou `NOT_FOUND`.
  - Preuve : `TeamLifecycleServiceTest` et `TeamLifecycleServiceIT`.
- [x] La création et la réactivation d'une équipe exigent une organisation, un administrateur et un manager `AVAILABLE` au moment du cas d'usage.
  - Preuve : `TeamLifecycleServiceTest`, `TeamLifecycleServiceIT` et `TeamResponsibleUsersValidatorTest`.
- [x] L'administrateur et le manager d'une équipe sont remplaçables en `ACTIVE` ou `SUSPENDED` par un utilisateur `AVAILABLE`, sans changement automatique de statut et sans possibilité de laisser une responsabilité vide.
  - Preuve : `TeamResponsibleUsersServiceTest`, `TeamResponsibleUsersServiceIT` et `TeamResponsibleUsersValidatorTest`.
- [x] Une équipe `ARCHIVED` interdit toute transition et toute modification de ses responsabilités ou appartenances.
  - Preuve : `TeamTest`, `TeamLifecycleServiceTest`, `TeamResponsibleUsersServiceTest` et `TeamMembershipServiceTest`.
- [x] `TEAM_UNAVAILABLE` distingue une équipe trouvée mais non opérationnelle de `NOT_FOUND` et d'une transition invalide de `TeamStatus`.
  - Preuve : `TeamLifecycleServiceTest` et `TeamMembershipServiceTest`.
- [x] `OrganizationErrorCode`, `UserErrorCode` et `TeamErrorCode` contiennent exactement les codes définis par ce besoin et restent dans leurs modules propriétaires.
  - Preuve : `OrganizationExceptionTest`, `UserLifecycleServiceTest`, `TeamExceptionTest` et `ModulithArchitectureTests`.
- [x] Les erreurs de `TeamMember` utilisent `TeamErrorCode` et aucune enum métier globale n'est ajoutée à `tp-common`.
  - Preuve : `JpaTeamMemberRepositoryAdapterTest`, `TeamExceptionTest` et `InternalArchitectureTests`.
- [x] Les opérations refusées exposent le code correspondant sans dépendre d'un statut HTTP ou d'un message utilisateur.
  - Preuve : tests de domaine `OrganizationTest`, `UserTest`, `TeamTest`, `TeamMemberTest` et tests de services associés.
- [x] Chaque module possède une seule exception métier interne portant un code obligatoire, un message de diagnostic et une cause facultative.
  - Preuve : `OrganizationExceptionTest`, `UserLifecycleServiceTest` et `TeamExceptionTest`.
- [x] Les résultats métier attendus des APIs `Directory` sont retournés par les enums d'availability ; une panne technique est exposée par l'exception publique du contrat puis traduite par le module consommateur.
  - Preuve : `UserDirectoryServiceTest`, `UserDirectoryServiceIT`, `OrganizationDirectoryServiceTest` et `OrganizationDirectoryServiceIT`.
- [x] Aucune exception JPA, Spring ou issue du package `domain.<domaine>.error` d'un autre module ne traverse une dépendance inter-module.
  - Preuve : `ModulithArchitectureTests` et tests des services `Directory`.
- [x] Un utilisateur d'une autre organisation est retourné `NOT_FOUND`.
  - Preuve : `UserDirectoryServiceTest` et `UserDirectoryServiceIT`.
- [x] Le passage ou retour d'une organisation vers `ACTIVE` et le remplacement direct de l'un de ses responsables exigent ponctuellement `AVAILABLE` ; la création d'une équipe exige également des responsables `AVAILABLE`.
  - Preuve : `OrganizationResponsibleUsersValidatorTest`, `OrganizationResponsibleUsersValidatorIT`, `TeamResponsibleUsersValidatorTest` et `TeamLifecycleServiceIT`.
- [x] La suspension ou désactivation ultérieure d'un responsable ne suspend pas automatiquement l'organisation dans T04.
  - Preuve : `OrganizationLifecycleServiceTest` et `OrganizationResponsibleUsersServiceTest`.
- [x] Le statut d'un abonnement ne modifie jamais `OrganizationStatus` ; une appartenance `INVITED` accepte un utilisateur `AVAILABLE` ou `PENDING`.
  - Preuve : `rg -n -i 'subscription' tp-organization/src/main` sans résultat le
    16 septembre 2026 ; `TeamMembershipServiceTest` et
    `TeamMemberUserAvailabilityValidatorTest` pour l'invitation.
- [x] Être administrateur ou manager d'une équipe n'implique aucune ligne `TeamMember` automatique ; chaque responsable peut être membre ou non.
  - Preuve : `TeamTest`, `TeamResponsibleUsersServiceTest` et `TeamMembershipServiceIT`.
- [x] Une équipe `SUSPENDED` autorise uniquement la suspension et le retrait d'un membre ; une organisation indisponible autorise également ces deux opérations de fermeture sans permettre d'ouvrir ou rétablir un accès.
  - Preuve : `TeamMembershipServiceTest` et `TeamMembershipServiceIT`.
- [x] L'ajout direct, l'activation et la réactivation d'un membre exigent un utilisateur `AVAILABLE`, tandis que la suspension et le retrait ne dépendent pas de sa disponibilité.
  - Preuve : `TeamMembershipServiceTest`, `TeamMembershipServiceIT` et `TeamMemberUserAvailabilityValidatorTest`.
- [x] `TeamMember` référence `Team` par `teamId` et une clé étrangère composite avec `organizationReference`, mais référence l'utilisateur uniquement par `userReference`.
  - Preuve : migration `V0.1.1__create_team_members.sql` et `JpaTeamMemberRepositoryAdapterIT` sur PostgreSQL/Testcontainers.
- [x] `TeamMember` ne possède pas de référence fonctionnelle propre et une seule appartenance non terminée existe pour un triplet `(organizationReference, teamId, userReference)`.
  - Preuve : `TeamMember`, migration `V0.1.1__create_team_members.sql` et `JpaTeamMemberRepositoryAdapterIT`.
- [x] Une réinvitation après `REMOVED` crée une nouvelle ligne sans réactiver ni remplacer l'ancienne appartenance.
  - Preuve : `TeamMemberTest` et `JpaTeamMemberRepositoryAdapterIT.persistsMembershipHistoryAndExcludesRemovedMembershipsFromCurrentLookups()`.
- [x] `startedAt` reste nul pendant `INVITED`, est renseigné à l'entrée en `ACTIVE` et n'est pas modifié par une suspension ; `endedAt` reste nul jusqu'au passage à `REMOVED` et ne précède jamais `startedAt` lorsque celle-ci existe.
  - Preuve : `TeamMemberTest` et migration `V0.1.1__create_team_members.sql`.
- [x] Les entités JPA persistées possèdent une version et les quatre champs d'audit ; ces données techniques ne sont pas exposées par le modèle de domaine `Organization` ou `User`.
  - Preuve : migrations Flyway, `JpaUserRepositoryAdapterIT.AuditTests`, `JpaOrganizationRepositoryAdapterIT.UpdateTests` et `JpaTeamMemberRepositoryAdapterIT`.
- [x] L'email de `User` est canonisé en minuscules sans espaces périphériques, limité à 254 caractères et unique par organisation sous cette forme.
  - Preuve : `UserTest.ValidationTests`, migration `V0.1.0__create_users.sql` et `JpaUserRepositoryAdapterIT.UniquenessTests`.
- [x] Les prénom et nom de `User` sont non blancs après suppression des espaces périphériques et limités chacun à 100 caractères.
  - Preuve : `UserTest.ValidationTests` et migration `V0.1.0__create_users.sql`.
- [x] Les longueurs et formats des références sont refusés côté Java lorsqu'ils sont invalides, même si les colonnes PostgreSQL utilisent `TEXT`.
  - Preuve : `ReferenceFormatTest`, `TenantContextTest`, `UserDirectoryServiceIT` et `OrganizationDirectoryServiceIT`.
- [x] Les contraintes Jakarta des ports entrants sont exécutées par les services `@Validated` au travers du bean Spring proxifié ; les tests unitaires par instanciation directe ne supposent pas cette interception.
  - Preuve : `UserLifecycleServiceIT`, `ListUsersServiceIT`, `OrganizationLifecycleServiceIT`, `TeamLifecycleServiceIT` et `TeamMembershipServiceIT`.
- [x] R11 impose `@Validated` à toute classe de `application.service` déclarée avec `@Service`.
  - Preuve : `BusinessModuleArchitectureRules` et `InternalArchitectureTests`.
- [x] Les modèles du domaine restent sans annotation Jakarta et refusent toute construction, restauration ou transition produisant un état invalide.
  - Preuve : `BusinessModuleArchitectureRules`, `InternalArchitectureTests`, `OrganizationTest`, `UserTest`, `TeamTest` et `TeamMemberTest`.
- [x] Les validations dépendant d'un `Directory` ou d'un repository restent dans la couche application et aucune logique métier propre à un module n'est déplacée dans `tp-common`.
  - Preuve : `BusinessModuleArchitectureRules`, `InternalArchitectureTests` et `ModulithArchitectureTests`.
- [x] Les violations PostgreSQL connues sont traduites explicitement par les adapters, tandis qu'une violation inconnue reste une erreur technique.
  - Preuve : `JpaUserRepositoryAdapterTest`, `JpaOrganizationRepositoryAdapterTest`, `JpaTeamRepositoryAdapterTest` et `JpaTeamMemberRepositoryAdapterTest`.
- [x] Chaque règle de validation est testée principalement dans sa couche propriétaire ; les couches supérieures couvrent uniquement leur intégration et leur traduction.
  - Preuve : suites domaine, application et persistence exécutées par les trois `verify` de module.
- [x] Les tests ArchUnit confirment que le domaine ne dépend ni de Spring, ni de Jakarta Validation, ni de JPA, et que `tp-common` reste framework-agnostic.
  - Preuve : `InternalArchitectureTests` et `BusinessModuleArchitectureRules` exécutés par le reactor complet.

## Validation attendue

- Tests unitaires du générateur avec horloge et nonce contrôlés, concurrence,
  recul du temps, changement de jour et d'année, encodage base 36, limites
  `00`/`ZZ` et débordement à la 1 297e génération d'une même milliseconde
  logique.
- Tests unitaires des invariants des modèles et transitions de statuts.
- Tests unitaires confirmant que les factories et restaurations du domaine ne
  peuvent produire un agrégat invalide, sans démarrer Spring.
- Tests unitaires des bornes du nom d'organisation : `null`, blanc après
  `strip()`, 200 caractères acceptés, 201 refusés, casse et espaces internes
  conservés.
- Tests applicatifs confirmant que le cas d'usage accepte 200 caractères
  normalisés et propage le refus du domaine pour 201, sans seconde limite métier
  portée par Jakarta Validation sur la commande.
- Tests unitaires des bornes du nom d'équipe : `null`, blanc après `strip()`,
  200 caractères acceptés, 201 refusés, casse et espaces internes conservés,
  sans refus de deux noms identiques.
- Tests du cycle des responsables couvrant les quatre statuts, le retrait du
  manager depuis `ACTIVE`, les remplacements sans changement de statut,
  l'archivage avec ou sans responsables et le caractère terminal de `ARCHIVED`.
- Tests du cycle temporel de `TeamMember`, couvrant l'invitation, l'activation,
  la suspension, le retrait et la réinvitation sur une nouvelle ligne, ainsi que
  le refus d'une date de fin antérieure à la date de début.
- Tests applicatifs du cycle de l'équipe couvrant la création, la réactivation
  après contrôle de l'organisation et des deux responsables, les remplacements
  sans changement de statut et l'immutabilité après archivage.
- Tests applicatifs des appartenances couvrant la matrice des opérations
  autorisées pour une équipe ou une organisation active, suspendue ou archivée,
  ainsi que les contrôles ponctuels de l'utilisateur lors de l'invitation, de
  l'ajout, de l'activation et de la réactivation.
- Tests unitaires et applicatifs vérifiant les codes d'erreur retournés pour les
  validations, transitions, indisponibilités et conflits définis par T04.
- Tests Spring ciblés utilisant les beans proxifiés pour vérifier `@Validated`,
  `@Valid` et les contraintes des commandes, sans reproduire les règles métier
  déjà couvertes par les tests du domaine.
- Tests unitaires de `TenantContext` et tests applicatifs prouvant que les ports
  tenantés exigent ce contexte, puis propagent sa `tenantReference` comme
  `organizationReference` vers les ports sortants.
- Tests unitaires de `LocalTenantContextProvider` vérifiant l'initialisation
  lazy, l'appel unique à `generate("ORG")`, la valeur de `tenantReference`, la
  conservation de la même instance, la génération unique en concurrence et la
  possibilité de retenter après une erreur ou une référence invalide.
- Test de câblage Spring ciblé avec le profil `local`, vérifiant qu'un unique
  `TenantContextProvider` utilise le vrai bean `ReferenceFactory`, retourne le
  même contexte et produit une référence préfixée par `ORG`. Vérifier également
  qu'aucun provider local n'est enregistré sans ce profil.
- En W001-T05, les tests Web des contrôleurs tenantés vérifieront qu'un seul
  appel à `TenantContextProvider.current()` est effectué par requête, que le
  contexte obtenu est transmis tel quel au cas d'usage et qu'aucune référence
  de tenant libre n'est acceptée, générée ou recherchée par le contrôleur.
- En W001-T05, les tests d'administration vérifieront qu'un utilisateur ne peut
  pas devenir `SUSPENDED` ou `DEACTIVATED` tant qu'il porte une responsabilité
  active, qu'une responsabilité archivée reste historique et qu'une simple
  appartenance `TeamMember` ne bloque pas la transition. Ils vérifieront aussi
  que l'acteur est fourni par une source applicative contrôlée distincte de
  `TenantContext`, sans présenter cette source W001 comme une authentification.
- Ces tests du provider et de son câblage ne démarrent ni PostgreSQL ni
  Testcontainers, car ce composant n'accède pas à la base. Les tests PostgreSQL
  restent requis séparément pour les contraintes et l'isolation de persistance
  de T04.
- Tests des cas d'usage plateforme confirmant qu'ils restent non tenantés et ne
  dépendent d'aucun `PlatformContext` vide.
- Tests vérifiant que les résultats attendus des directories sont retournés sans
  exception et qu'une panne technique est traduite en exception publique, puis
  dans le code d'erreur du module consommateur avec conservation de la cause.
- Tests applicatifs vérifiant les appels ponctuels à `UserDirectory` lors de
  l'activation, de la réactivation et du remplacement direct d'un responsable,
  sans surveillance après la transaction.
- Test de `OrganizationDirectory` confirmant que le mapping de `ACTIVE` vers
  `AVAILABLE` n'appelle jamais `UserDirectory`.
- Tests Spring Modulith vérifiant que les modules consommateurs accèdent
  uniquement aux interfaces nommées `identity::user` et
  `organization::organization`.
- Tests d'intégration PostgreSQL avec deux organisations, couvrant les
  contraintes `NOT NULL`, la limite de 200 caractères du nom, les nullabilités
  dépendantes du statut, les unicités et l'isolation des recherches.
- Le test PostgreSQL accepte un nom normalisé de 200 caractères et refuse une
  insertion directe de 201 caractères.
- Pour `Team`, vérifier également la limite de 200 caractères du nom, l'absence
  d'unicité sur ce nom, la clé étrangère composite, l'index unique partiel des
  appartenances courantes et la cohérence chronologique de `started_at` et
  `ended_at`.
- Pour `Organization`, vérifier la conservation de l'identifiant et de l'audit
  de création, l'évolution de la version et de `modifiedAt`, ainsi que la
  traduction d'un conflit optimiste réel en `CONCURRENT_MODIFICATION`.
- Pour `User`, vérifier aussi la conservation de l'identifiant et de l'audit de
  création, l'évolution de la version et de `modifiedAt`, ainsi que la
  traduction d'un conflit optimiste réel en `CONCURRENT_MODIFICATION`.
- Vérifier la traduction d'une collision de référence sans retry dans un test
  unitaire de l'adapter ; le test PostgreSQL vérifie la collision réelle et la
  préservation de l'utilisateur existant.
- Exécution de tous les tests ArchUnit existants.
- Vérification manuelle qu'aucun contrat inter-module n'expose un identifiant
  technique externe.

## Artefacts attendus

- Générateur de références Java dans `tp-common` et ses tests.
- Contrat Java pur
  `tp-common/src/main/java/io/teampulse/common/context/TenantContextProvider.java`,
  exposé par `common::context`.
- Modèles et contrats applicatifs des modules `organization` et `identity`,
  ainsi que verticale interne complète `Team` / `TeamMember` définie par T04.
- `LocalTenantContextProvider` et `LocalTenantConfiguration` dans
  `tp-app/src/main/java/io/teampulse/context`, avec leurs tests unitaires et de
  câblage Spring.
- API publique `UserDirectory`, enum `UserAvailability`,
  `UserDirectoryException` et implémentation locale dans `tp-identity`.
- API publique `OrganizationDirectory`, enum `OrganizationAvailability`,
  `OrganizationDirectoryException` et implémentation locale dans
  `tp-organization`.
- Enums et exceptions métier internes `OrganizationErrorCode` /
  `OrganizationException`, `UserErrorCode` / `UserException` et
  `TeamErrorCode` / `TeamException` dans leurs modules propriétaires.
- Migrations Flyway des tables et contraintes initiales, notamment les
  garanties tenantées et temporelles de `teams` et `team_members`.
- Tests d'isolation multi-tenant.
- ADR-W001-T04 cohérent avec l'implémentation finale.
- Support Slidev expliquant la référence inter-module, `version`, les
  verrouillages optimiste et pessimiste, ainsi que la limite multi-nœud du
  générateur.

## Definition of Done

### Livré et prouvé

- [x] Le générateur de références, `TenantContext` et le provider local sont
  implémentés dans les modules prévus.
  - Preuve : `MonotonicReferenceFactoryTest`, `TenantContextTest`,
    `LocalTenantContextProviderTest` et `LocalTenantConfigurationTest`.
- [x] Les modèles `User`, `Organization`, `Team` et `TeamMember`, leurs ports,
  leurs erreurs et leurs APIs inter-modules sont livrés.
  - Preuve : tests de domaine et de service des trois modules ;
    `ModulithArchitectureTests`.
- [x] Les migrations Flyway et les contraintes PostgreSQL tenantées, d'audit,
  de version et de temporalité sont livrées.
  - Preuve : migrations `identity/V0.1.0`, `organization/V0.1.0`,
    `team/V0.1.0` et `team/V0.1.1` ; ITs JPA associées sur Testcontainers.
- [x] L'isolation A/B, les collisions sans retry, l'audit `SYSTEM` et les
  conflits optimistes sont démontrés contre PostgreSQL réel.
  - Preuve : `JpaUserRepositoryAdapterIT`, `JpaOrganizationRepositoryAdapterIT`,
    `JpaTeamRepositoryAdapterIT` et `JpaTeamMemberRepositoryAdapterIT`.
- [x] Le gate de clôture est vert sur les trois modules puis sur le reactor
  complet.
  - Preuve : quatre commandes `verify` exécutées le 16 septembre 2026 : 576
    tests, 0 échec et 0 erreur au total.

### À finaliser dans cette clôture

- [x] Le besoin et l'ADR-W001-T04 sont passés à `Accepted` et décrivent exclusivement la
  réalisation vérifiée.
- [x] La roadmap porte un suivi T04 cohérent avec le périmètre réellement
  livré, sans déclarer T05/HTTP terminé.
- [x] Le support Slidev T04 est finalisé, possède une note formateur par slide,
  et passe la validation des notes, le build et les revues desktop/mobile.
- [ ] Le diff indexé de clôture est relu, ne contient que les artefacts T04 et
  est commité avec le message validé.

### Explicitement hors périmètre de T04

- [x] Résolution du tenant depuis une identité authentifiée, émission et
  validation JWT : différé à W008.
- [x] Contrats HTTP, contrôleurs, DTOs, erreurs HTTP et tests Web : différés à
  W001-T05.
- [x] PostgreSQL Row-Level Security : non livré dans T04.
- [x] Coordination distribuée du générateur, identité de nœud et déploiement
  multi-nœud/Kubernetes : non livrés dans T04.

## Notes pédagogiques

Ce ticket montre que le multi-tenant ne se résume pas à ajouter une colonne. Il
faut distinguer identifiant interne, référence d'intégration, contexte
d'exécution, contraintes de persistance et future autorisation de sécurité.

Il introduit également le verrouillage optimiste par `version`. Le verrouillage
pessimiste sera présenté comme une alternative adaptée à certains traitements
fortement concurrents, mais il n'est pas généralisé au modèle.

### Synopsis Slidev à finaliser après l'implémentation

Le support T04 est destiné à un développeur qui connaît Java et Spring, mais qui
ne maîtrise pas encore l'architecture ou le multi-tenant. Il constitue un
mini-chapitre progressif et n'est pas limité à un nombre arbitraire de slides.
Chaque slide conserve un objectif pédagogique principal et la progression reste
guidée par la compréhension, pas par la quantité de contenu à faire tenir.

La trame pré-implémentation suit cet ordre :

1. partir d'un scénario concret dans lequel deux organisations utilisent
   TeamPulse ;
2. rendre visible le risque de mélange ou de fuite de données entre elles ;
3. définir simplement un tenant et le multi-tenant à partir de ce scénario ;
4. distinguer un identifiant technique d'une référence fonctionnelle ;
5. comparer l'identifiant `BIGINT`, l'UUID et une référence lisible comme
   contrats inter-modules ;
6. expliquer pourquoi TeamPulse choisit `organizationReference` pour désigner
   le tenant hors du module organisation ;
7. construire progressivement le format
   `<TRIGRAMME>-<ANNÉE>-<JOUR_MOIS>-<SUFFIXE>` ;
8. décomposer le suffixe en six caractères de millisecondes logiques dans la
   journée UTC, quatre caractères de nonce JVM et deux caractères de compteur,
   puis l'illustrer par un exemple ;
9. expliquer pourquoi `36^6` couvre une journée, les 1 679 616 nonces possibles,
   les 1 296 valeurs de `00` à `ZZ` et l'avancement du temps logique lorsque
   plusieurs références sont demandées dans la même milliseconde ;
10. montrer les effets d'un recul d'horloge et d'un redémarrage de la JVM ;
11. expliciter la garantie mono-JVM, la contrainte d'unicité PostgreSQL et la
    limite multi-nœud reportée avant Kubernetes ;
12. montrer pourquoi le tenant doit être propagé explicitement dans les cas
    d'usage et les repositories ;
13. expliquer pourquoi `TenantContext(tenantReference)` rend le périmètre
    tenant explicite sans constituer une autorisation de sécurité, et pourquoi
    TeamPulse n'introduit pas de `PlatformContext` vide en W001 ;
14. expliquer le service singleton local de W001 et son futur remplacement par
    un contexte issu du JWT en W008 ;
15. répartir les responsabilités et données entre `Organization`, `User`,
    `Team` et `TeamMember` ;
16. comparer une clé étrangère interne à une référence utilisée pour franchir
    la frontière d'un module ;
17. expliquer le rôle de `UserDirectory` et de `OrganizationDirectory`, leur
    exposition par les interfaces nommées dédiées `identity::user` et
    `organization::organization`, ainsi que l'absence de client OpenAPI dans le
    monolithe modulaire actuel ;
18. présenter les statuts et transitions comme des règles métier explicites,
    et non comme de simples valeurs modifiables en base ;
19. distinguer résultat métier attendu, exception interne, défaillance publique
    d'un directory, code d'erreur du module consommateur, puis message et statut
    HTTP ajoutés par les adapters ;
20. introduire `version`, comparer verrouillage optimiste et pessimiste, puis
    justifier le choix par défaut de TeamPulse ;
21. terminer par les champs d'audit, les contraintes, les tests d'isolation et
    les preuves observables attendues.

Pour chaque décision importante, le support devra présenter le problème, au
moins deux options crédibles, le choix TeamPulse, ses avantages, son compromis,
la condition qui justifierait de le revoir et la preuve attendue. Le texte
visible restera concis ; les notes du présentateur porteront l'explication
orale, les pièges et les transitions utiles au débutant.

Avant l'implémentation, cette trame fixe uniquement la progression et les
questions auxquelles le chapitre devra répondre. Après l'implémentation, le
support Slidev sera finalisé à partir du code réellement produit, des tests, des
preuves exécutables et de l'ADR éventuellement ajusté. Les mécanismes détaillés
de RLS, de JWT et de coordination Kubernetes resteront hors de ce chapitre.

## Liens avec les autres tickets

- Dépend de : W001-T01 pour le découpage Maven multi-module.
- Dépend de : W001-T02 pour PostgreSQL, les schémas et Flyway.
- Dépend de : W001-T03 pour les règles d'architecture et les tests ArchUnit.
- Prépare : W001-T05 pour l'identité de l'acteur, le contrôle transversal des
  responsabilités avant indisponibilité d'un utilisateur, les rôles et leurs
  affectations, ainsi que les contrats HTTP et leurs tests.
- Prépare : W002 pour enrichir les cas d'usage autour des équipes et membres
  sans réimplémenter leurs cycles internes livrés par T04.
- Prépare : W008 pour JWT, la résolution réelle du contexte et les permissions.
- Prépare : une future étape Kubernetes pour la coordination multi-nœud.
- Prépare : W012 pour les premiers événements d'audit persistés de création de
  `Team` et `User`.
- Prépare : une extension de W012 ou un ticket ultérieur dédié à l'historisation
  complète de toutes les modifications.

## Décision associée

Voir
[ADR-W001-T04](../../adr/W001/ADR-W001-T04-multi-tenancy-organization-reference.md).
