# ADR-W001-T05 — APIs métier et administration de plateforme

## Statut
Draft

## Ticket lié
W001-T05 — APIs métier et administration de plateforme

## Besoin associé
[`docs/besoins/W001/W001-T05-api-metiers-et-administration-plateforme.md`](../../besoins/W001/W001-T05-api-metiers-et-administration-plateforme.md)

## ADR techniques de référence (Draft)

Les ADR techniques ci-dessous exposent des choix réutilisables. Leur statut
Draft ne vaut pas adoption par TeamPulse ; le présent ADR précise le contrat
d'adoption local proposé et conserve lui-même son statut Draft.

- [ADR-TECH-013 — OpenAPI contracts and generated test clients](../technical/ADR-TECH-013-openapi-contracts-and-generated-test-clients.md)
- [ADR-TECH-014 — Black-box HTTP integration tests](../technical/ADR-TECH-014-black-box-http-integration-tests.md)
- [ADR-TECH-015 — Shared Spring HTTP error support](../technical/ADR-TECH-015-shared-spring-http-error-support.md)
- [ADR-TECH-016 — Durable in-process events with Spring Modulith](../technical/ADR-TECH-016-durable-in-process-events-with-spring-modulith.md)

## Contexte
T04 a livré les capacités métier et la persistance des modules identité,
organisation et équipe. Les contrats HTTP doivent exposer ces capacités sans
déplacer leur propriété. Le provisioning de plateforme traverse plusieurs
modules et doit rendre un résultat synchrone. Les contrôles d'autorisation
authentifiés ne sont pas disponibles avant W008.

Le besoin T05 initial limité à une API utilisateur ne couvre ni les capacités
T04 déjà confirmées dans les trois modules métier, ni le rôle distinct de
l'administration de plateforme. Le besoin T05 actualisé exclut aussi le moteur
de notification : `tp-notification` et sa livraison relèvent de T10.

## Décision

### 1. Garder l'ownership HTTP dans les modules métier

- `tp-identity` possède ses endpoints et contrats utilisateur ;
  `tp-organization` possède les endpoints des opérations d'organisation
  exposées en T05, hors activation/réactivation reportées à W008 et hors
  affectation de responsables qui provoquerait une telle transition ;
  `tp-team` possède ceux des équipes et appartenances.
- Chaque module adapte ses propres cas d'usage. Les contrôleurs ne lisent pas
  directement la persistence et les DTO HTTP ne sont pas partagés comme modèles
  de domaine inter-modules.
- `tp-administration` est créé comme propriétaire des seuls parcours
  d'administration de la plateforme. Il ne devient pas l'API générique de
  gestion des organisations, utilisateurs ou équipes.
- Les API sont documentées dans un contrat OpenAPI par propriétaire HTTP. Les
  appels générés servent aux tests ; le code serveur reste écrit selon les
  ports d'entrée existants et l'architecture hexagonale.

### 2. Orchestrer les commandes de plateforme par contrats Java synchrones

- `tp-administration` appelle les contrats Java publics des modules métier
  dans le flux de la commande lorsqu'il doit fournir immédiatement un résultat
  HTTP. Il n'utilise pas les événements pour commander une création ou obtenir
  son résultat.
- La création initiale d'une organisation initialise son statut à `CREATING`.
  Le premier utilisateur peut être créé ou invité, mais `INVITED` et `CREATING`
  restent `PENDING` selon `UserDirectory`.
- La règle d'éligibilité d'un candidat responsable reste en arbitrage : le
  comportement T04 autorise `PENDING` dans certains parcours, tandis que le
  besoin T05 Draft propose `AVAILABLE` uniquement. En attendant la décision,
  T05 ne modifie pas le comportement accepté en T04. Le premier compte n'étant
  pas rendu disponible en T05, le bootstrap s'arrête en `CREATING` avant
  l'affectation finale. T05 n'expose
  pas l'activation/réactivation et ne doit pas permettre qu'une affectation de
  responsables la déclenche indirectement. W008 introduit l'activation du compte,
  l'affectation finale et la transition de l'organisation vers `ACTIVE`.
- `TenantContext` porte uniquement le périmètre des données. `ActorContext`
  identifie l'acteur technique enregistré pour l'audit, notamment `SYSTEM` en
  local ; il ne prouve pas une authentification et ne fournit aucun rôle ou
  droit. Le contexte de sécurité, lorsqu'il sera introduit, portera l'identité
  authentifiée et les décisions d'autorisation. T05 n'introduit pas ce dernier
  contexte ; ces capacités relèvent de W008.

### 3. Lier le profil local à une organisation persistée

Le `TenantContextProvider` du profil `local` initialise paresseusement une
organisation de démonstration persistée en `CREATING` à son premier appel,
puis mémorise sa référence pour la durée du processus. En cas d'échec, aucun
contexte n'est mémorisé et un appel ultérieur retente l'initialisation,
conformément au comportement accepté en T04. Il ne fabrique pas de référence
tenant en mémoire. Le comportement actuel crée donc une nouvelle organisation
de démonstration au premier appel de chaque processus ; la réutilisation d'une
organisation persistante entre redémarrages reste à arbitrer. Les opérations
exigeant une organisation `ACTIVE` attendent W008.
Les API T05 non authentifiées restent réservées à l'environnement local/de
développement jusqu'à W008 ; elles ne doivent pas être exposées à un réseau non
fiable.

### 4. Publier des faits sans inclure les notifications

- Le propriétaire publie `UserInvited` comme fait métier contenant des
  références uniquement. En T05, un port sortant d'identité délègue la
  publication à un adaptateur Spring ; le service applicatif n'appelle pas
  directement l'API d'événements Spring. Le fait n'embarque aucune PII et aucun
  consommateur n'est livré. `OrganizationActivated` est réservé à W008, qui
  introduira la transition effective vers `ACTIVE`.
- L'orchestration synchrone ne dépend pas de ces événements. Les événements
  traitent les effets secondaires ; aucun accusé de réception métier n'est
  ajouté à la réponse HTTP.
- T05 ne crée pas `tp-notification`, ne persiste pas de notifications et
  n'envoie aucun email. Le besoin actuellement envisagé sous W001-T10, encore
  Draft et non officialisé dans la roadmap canonique, pourra adopter un
  consommateur si un besoin réel le justifie. Un registre durable et la reprise
  ne seront évalués qu'avec un vrai listener qui en a besoin ; T05 ne livre ni
  registre, ni listener factice, ni rejeu historique. Les invitations créées
  avant qu'un consommateur soit disponible ne déclenchent pas d'email
  rétroactif.

### 5. Standardiser les frontières HTTP et leur preuve

- Les décisions génériques des ADR-TECH-013 à 016 sont adoptées pour TeamPulse
  selon le contrat local ci-dessous : contrats OpenAPI et clients de test
  générés, tests HTTP/PostgreSQL réels, support Spring d'erreurs séparé du DTO
  Java pur et publication in-process sans registre durable en l'absence de
  listener réel.
- Les erreurs métier restent codées et traduites par leur module propriétaire.
- La preuve des contrats contrôleur traverse un serveur réel sur port
  aléatoire, l'application et le chemin interne réels, Flyway et PostgreSQL
  Testcontainers. Aucun test de contrôleur en slice ou avec cas d'usage mocké
  n'est ajouté. Les tests `AbstractIntegrationTest` existants sans serveur
  restent inchangés ; une base Web dédiée réutilise leur support.
- Tous les modules du reactor conservent la version commune du parent Maven,
  actuellement `0.1.0-SNAPSHOT`. Aucune version de produit indépendante par
  module ou version d'API d'URL n'est ajoutée par défaut.

### Contrat d'adoption TeamPulse

Ce contrat décrit l'adoption locale proposée et l'implémentation observée ; il
ne change pas le statut Draft de cet ADR.

- Propriétaires HTTP : `tp-identity`, `tp-organization`, `tp-team` et
  `tp-administration`, chacun avec son contrat sous `src/main/openapi`.
- Génération : OpenAPI Generator Maven `7.25.0`, version centralisée dans le
  POM parent ; générateur Java `restclient`, modèles et clients générés sous
  `target/generated-test-sources/openapi`, attachés uniquement aux sources de
  test. `useSpringBoot4=true`, `useJackson3=true` et `openApiNullable=false`
  correspondent au runtime Java 25 / Spring Boot 4.1.0 et évitent le wrapper
  nullable non utilisé par ces clients. Les contrats et artefacts utilisent la
  version Maven commune `0.1.0-SNAPSHOT`, sans version d'URL. Le serveur
  annoncé est `http://localhost:8080` ; les clients DSL remplacent la base URL
  par le port aléatoire du serveur de test.
- Erreurs : `ApiError` reste un DTO Java pur dans `tp-common`. L'advice Spring
  partagé est dans `tp-web-support`, sans dépendance métier ; chaque propriétaire
  conserve ses codes et mappings métier. Les frontières Java publiques
  traduisent les défaillances techniques en erreur stable `OPERATION_FAILED`,
  préservent la cause et sa chaîne pour le consommateur interne, et masquent les
  diagnostics dans la réponse HTTP.
- Tests HTTP : les tests d'acceptation des contrôleurs démarrent un serveur sur
  port aléatoire avec l'application réelle, Flyway et PostgreSQL Testcontainers.
  `tp-test-support` porte le support HTTP générique ; chaque module garde sa DSL
  métier dans `src/test`. Les bases non Web existantes conservent
  `WebEnvironment.NONE`. Les tests autonomes remplacent seulement les contrats
  publics externes : provider déterministe pour identité, `UserDirectory` pour
  organisation, `UserDirectory` et `OrganizationDirectory` pour équipe ; les
  tests d'administration assemblent les vrais modules concernés. Les writes
  du thread serveur sont nettoyées ou isolées explicitement. Cette spécialisation
  adopte la preuve HTTP réelle comme exigence TeamPulse ; elle ne transforme pas
  la stratégie générique de test en interdiction universelle des slices.
- Contexte : `TenantContextProvider.current()` est appelé une seule fois par
  requête tenantée et le résultat est transmis tel quel. `ActorContext` alimente
  l'audit via `AuditorAware` ; le provider par défaut fournit `SYSTEM` en W001.
  Ce contexte d'acteur n'authentifie ni n'autorise.
- Événements : identité publie `UserInvited` via un port et un adaptateur Spring,
  avec les références utilisateur et organisation seulement. T05 ne possède
  aucun listener, registre durable ou rejeu ; `OrganizationActivated` n'est pas
  publié.
- Sécurité locale : avant W008, les quatre APIs sont réservées aux profils
  locaux/de développement et écoutent sur loopback par défaut.
- Choix métier en attente : la disponibilité `PENDING` d'un candidat
  responsable, l'atomicité du provisioning organisation/utilisateur et la
  réutilisation du tenant local après redémarrage restent soumis à arbitrage.
  L'implémentation actuelle accepte certains candidats `PENDING`, effectue les
  deux créations sans transaction englobante et crée un tenant de démonstration
  par processus. Le lazy init avec retry reste aligné sur T04 Accepted.

Les validations d'implémentation sont consignées dans les notes de cet ADR.
Une réussite de build ou de CI ne vaut pas acceptation des décisions métier.

## Alternatives envisagées
- Exposer uniquement `POST/GET /api/users` : rejeté, car ce périmètre omet les
  capacités T04 de `tp-organization` et `tp-team` et l'administration plateforme.
- Faire porter tous les endpoints métier par `tp-administration` : rejeté, car
  cela déplacerait l'ownership HTTP et métier vers un module d'orchestration.
- Utiliser les événements comme commandes entre modules : rejeté, car le
  demandeur a besoin du résultat final synchrone.
- Créer `tp-notification` et envoyer les emails dès T05 : rejeté au profit de
  T10 afin de garder T05 centré sur les API et le provisioning métier.
- Ajouter rôles et permissions avant l'authentification : rejeté ; les
  responsabilités métier T04 restent distinctes des décisions d'accès W008.
- Permettre qu'une affectation finale active l'organisation en T05 : rejeté,
  car l'activation attend W008, quel que soit le statut de disponibilité du
  responsable.
- Exposer l'activation/réactivation de l'organisation en T05 : rejeté, car le
  premier compte ne peut devenir `AVAILABLE` qu'avec le parcours W008 et le
  bootstrap doit rester en `CREATING` jusque-là.

## Justification
Les modules propriétaires restent autonomes et leur API peut ultérieurement
être remplacée par un adaptateur distant sans transformer les appels métier en
HTTP internes. Le provisioning reste cohérent et synchrone ; les faits
secondaires peuvent être consommés indépendamment. Le report de l'activation
évite d'inventer une identité ou une disponibilité avant W008, tandis que le
report des notifications évite d'ajouter leur persistance et leur exploitation
à la responsabilité T05.

## Conséquences positives
- Les frontières HTTP reflètent les propriétaires de données et d'invariants.
- Le bootstrap garde un résultat synchrone sans couplage à un broker.
- Les responsabilités métier et l'autorisation sont explicitement séparées.
- Les tests prouvent les comportements réels du serveur, des modules et de
  PostgreSQL.
- `tp-notification` pourra évoluer dans le besoin envisagé en T10 sans faire
  connaître ses mécanismes aux modèles métier ni à `tp-administration`.

## Conséquences négatives / compromis
- Une nouvelle organisation, y compris celle du profil local, reste `CREATING`
  jusqu'à W008 ; les parcours exigeant `ACTIVE` ne sont pas utilisables dans
  cette phase et T05 ne livre pas seul un onboarding complet.
- Il n'y a aucun email d'invitation avant T10 et aucune reprise rétroactive
  prévue pour cette période.
- Les contrôleurs réels avec PostgreSQL sont plus lents que des tests de slice
  ou des tests utilisant des mocks.
- Les APIs sans authentification constituent un risque si le profil local est
  utilisé ou exposé hors d'un environnement maîtrisé.

## Impact technique
- Modules : `tp-identity`, `tp-organization`, `tp-team`, nouveau
  `tp-administration`, `tp-app`, `tp-common`, nouveau `tp-web-support` et
  `tp-test-support`.
- Contrats : HTTP/OpenAPI par propriétaire ; contrats Java synchrones pour
  l'orchestration de plateforme ; fait `UserInvited` en T05 et
  `OrganizationActivated` à partir de W008, avec références seulement.
- Tests : clients générés, DSL de test locale à chaque module, tests de serveur
  réel sur port aléatoire et PostgreSQL Testcontainers.
- Sécurité : profil local/de développement uniquement jusqu'à W008 ; aucun
  rôle, permission ou JWT introduit par cette décision.
- Hors impact T05 : module de notification, persistance/livraison email, SMS et
  endpoints de lecture d'inbox.

## Validation
- Vérifier les contrats OpenAPI et le code généré par le build Maven.
- Exécuter les tests HTTP/PostgreSQL des quatre propriétaires concernés, les
  tests des contrôles T04, puis `./mvnw --batch-mode --no-transfer-progress verify`.
- Vérifier qu'aucun parcours T05 ne fait passer l'organisation à `ACTIVE`, y
  compris par affectation de responsables, et qu'aucun contexte tenant n'est
  fourni par le client HTTP.
- Vérifier que les payloads d'événements ne contiennent aucune PII et qu'aucun
  `OrganizationActivated` n'est émis en T05 ; aucun listener ni module
  `tp-notification` n'est requis en T05.
- Vérifier les frontières Spring Modulith/ArchUnit, les réponses d'erreur et
  l'absence de détail technique divulgué.
- Vérifier le lien vers le besoin et la cohérence des références T04, W008 et
  T10.

## Risques
- Les endpoints sans autorisation ne doivent pas être exposés hors des profils
  locaux/de développement avant W008.
- Si un consommateur est ajouté ultérieurement pour un besoin réel, sa
  livraison peut être au moins une fois ; il devra être idempotent et ses
  échecs rester séparés du résultat de la commande HTTP initiale.
- L'absence d'envoi rétroactif avant T10 est une limite fonctionnelle assumée.
- Le provider local initialise à la première requête tenantée et retente au
  prochain appel si l'initialisation échoue ; une organisation distincte est
  actuellement créée par processus. La réutilisation après redémarrage reste à
  décider.
- Le provisioning peut laisser une organisation `CREATING` persistée si la
  création ou l'invitation de l'utilisateur échoue. Le résultat attendu
  (atomicité ou résultat partiel récupérable) reste à arbitrer.
- Le statut `PENDING` de certains candidats responsables est accepté par les
  règles T04 actuelles alors que le besoin T05 Draft propose `AVAILABLE` ; la
  règle cible reste à arbitrer et n'est pas traitée comme un défaut contre T04.

## Notes
- Les changements de décision intervenus pendant l'analyse sont conservés ici :
  notification/email déplacés de T05 vers T10 ; activation effective de
  l'organisation et publication de `OrganizationActivated` différées à W008.
- Les documents Draft T08-T14 de `ztmp` ne sont pas officialisés par cet ADR.
- L'implémentation de `W001-T05-http-test-support` ajoute le support
  `RestTestClient`, le nettoyage des écritures PostgreSQL et les bases de test
  avec serveur réel dédiées aux propriétaires HTTP. Les bases
  `AbstractIntegrationTest` existantes en `NONE` restent inchangées. La
  compilation des sources de test et les verticales HTTP/PostgreSQL passent ;
  cet ADR reste `Draft`.
- L'implémentation de `W001-T05-shared-http-errors` ajoute `ApiError` avec les
  champs `code` et `message`, ainsi que l'advice Spring partagé auto-configuré
  dans `tp-web-support`. Les détails techniques et les données personnelles ne
  sont pas renvoyés dans les réponses ; les mappings métier restent locaux et
  prioritaires. `tp-common` et `tp-web-support` passent `verify`, puis les
  réponses ont été exercées dans les tests HTTP réels des propriétaires ; le
  statut de cet ADR reste `Draft`.
- L'implémentation de `W001-T05-openapi-test-clients` fixe OpenAPI Generator
  Maven `7.25.0` dans le POM parent et configure la génération de clients Java
  uniquement pour les sources de test, sous `target/generated-test-sources`.
  Les quatre contrats propriétaires sont présents et leurs clients générés
  compilent avec le reactor. Le statut de cet ADR reste `Draft`.
- L'implémentation de `W001-T05-identity-api` expose `POST /api/users`,
  `POST /api/users/invitations` et `GET /api/users`. Chaque route tenantée
  résout une fois le `TenantContext` et les écritures utilisent le tenant du
  provider même si le corps contient une référence d'organisation inconnue du
  contrat. L'invitation persiste `INVITED` et publie `UserInvited` avec les
  références utilisateur/organisation seulement.
- Le contrat Java public `identity::lifecycle` fournit à `tp-administration`
  les commandes synchrones de création, invitation, suspension et
  désactivation avec des types de contrat sans modèle domaine identité.
  L'administration reste responsable des vérifications de responsabilités T04
  avant les deux transitions de fermeture. Aucun endpoint HTTP identité ne
  contourne cette orchestration. Le mapping métier identité prévaut sur l'advice
  partagé ; les erreurs de transport restent génériques et masquées. Les tests
  HTTP/PostgreSQL et `./mvnw --batch-mode --no-transfer-progress -pl tp-identity
  -am verify` passent. Cet ADR reste `Draft`.
- La verticale organisation expose la création en `CREATING`, la suspension,
  l'archivage et la gestion T04 des responsables ; aucune route d'activation ou
  de réactivation n'est publiée. `OrganizationResponsibleUsersService` refuse
  avant mutation toute affectation disponible qui aurait activé ou réactivé
  l'organisation, y compris lorsque l'application l'appelle directement. Le
  code métier `LIFECYCLE_TRANSITION_DEFERRED` est traduit localement en
  `409 ORGANIZATION_LIFECYCLE_DEFERRED` et documenté dans OpenAPI. Les contrats
  Java `organization::organization` exposent le provisioning synchrone et une
  lecture des responsabilités/statut sans modèle domaine ni entité. Les tests
  vérifient les refus, l'absence d'écriture `ACTIVE`, les réponses métier et
  transport sur HTTP réel avec Flyway/PostgreSQL ;
  `./mvnw --batch-mode --no-transfer-progress -pl tp-organization -am verify`
  passe. Cet ADR reste `Draft`.
- La verticale équipe expose en HTTP les cycles de vie d'équipe, la gestion des
  responsables et les appartenances T04, avec un contrat OpenAPI et un client
  généré utilisé par une DSL de test locale. Chaque requête tenantée appelle
  `TenantContextProvider.current()` une seule fois ; aucune donnée de tenant
  client ne sélectionne le tenant. Le contrat Java
  `team::api` ajoute une lecture tenantée des responsabilités admin/manager,
  avec référence et état d'équipe uniquement ; les membres ordinaires ne sont
  pas des responsabilités. Les cas HTTP utilisent le vrai serveur, Flyway et
  PostgreSQL Testcontainers, avec stubs limités aux contrats publics
  `UserDirectory` et `OrganizationDirectory`. Les erreurs métier restent
  locales et les contrôles T04 de disponibilité sont conservés. Les dix tests
  HTTP, le test PostgreSQL de responsabilités et
  `./mvnw --batch-mode --no-transfer-progress -pl tp-team -am verify`
  passent. Cet ADR reste `Draft`.
- Le noyau `tp-administration` est ajouté au reactor et à `tp-app`. Il ne porte
  ni entité, ni repository, ni règle métier des modules propriétaires. Son
  provisioning appelle les contrats Java publics synchrones : il conserve
  l'organisation créée en `CREATING`, construit le `TenantContext` du premier
  utilisateur à partir de la référence réellement retournée par le contrat
  organisation, puis crée ou invite ce premier utilisateur sans affectation
  finale de responsables. Les contrôles avant suspension/désactivation lisent
  les contrats publics de responsabilités d'organisation et d'équipe ; ils
  refusent les responsabilités `CREATING`, `ACTIVE` ou `SUSPENDED` d'une
  organisation et `ACTIVE` ou `SUSPENDED` d'une équipe, mais laissent passer
  `ARCHIVED` et une simple appartenance `TeamMember`. Des tests d'intégration
  assemblent les vrais modules sur PostgreSQL Testcontainers et vérifient l'état
  persisté. `ActorContextProvider` alimente `AuditorAware`; son implémentation
  par défaut fournit `SYSTEM` en W001 et n'authentifie ni n'autorise. Cet ADR
  reste `Draft`. `./mvnw --batch-mode --no-transfer-progress
  -pl tp-administration -am verify` passe, avec dix tests d'intégration du
  noyau d'administration sur PostgreSQL Testcontainers.
- La verticale HTTP d'administration ajoute son propre contrat OpenAPI et des
  clients de test générés pour le provisioning et le cycle de vie utilisateur.
  Les routes créent une organisation toujours `CREATING` avec son premier
  utilisateur, invitent le premier utilisateur, puis exposent suspension et
  désactivation après résolution unique du tenant par
  `TenantContextProvider.current()`. Les erreurs de plateforme ont un mapping
  local et les détails techniques/PII restent masqués. Les quatre APIs sont
  limitées aux profils `local` et `development`, le serveur est lié à
  `127.0.0.1` par défaut ; les tests HTTP vérifient l'absence des routes sans
  profil autorisé, le binding loopback, l'état PostgreSQL/Flyway et le refus
  d'opérations d'équipe quand l'organisation locale est `CREATING`. Le profil
  local crée cette organisation via le contrat public synchrone
  `OrganizationProvisioning` et réutilise la référence persistée retournée.
  `./mvnw --batch-mode --no-transfer-progress -pl tp-administration,tp-app
  -am verify` et `./mvnw --batch-mode --no-transfer-progress verify` passent.
  Cet ADR reste `Draft`.
- La correction des frontières de contrats utilise un port sortant d'identité
  pour publier `UserInvited` via Spring et convertit les erreurs techniques des
  contrats Java publics en `OPERATION_FAILED`, avec cause conservée. La
  validation `./mvnw --batch-mode --no-transfer-progress -pl
  tp-identity,tp-organization -am verify` passe.
- Les quatre serveurs OpenAPI annoncent `http://localhost:8080`. Les parcours
  HTTP PostgreSQL couvrent le remplacement d'administrateur/manager et la
  suppression du manager avec `ACTIVE` vers `SUSPENDED`, le refus
  `409 USER_HAS_ACTIVE_RESPONSIBILITIES` sans mutation et le 404 inter-tenant
  sans mutation. `./mvnw --batch-mode --no-transfer-progress -pl
  tp-organization,tp-administration,tp-app -am verify` passe.
  Ces validations locales ne remplacent pas encore une preuve CI de `verify`
  sur tous les commits de la pile ; les PR restent non fusionnées et l'ADR
  reste `Draft`.
