# ADR-W001-T05 — APIs métier et administration de plateforme

## Statut
Draft

## Ticket lié
W001-T05 — APIs métier et administration de plateforme

## Besoin associé
[`docs/besoins/W001/W001-T05-api-metiers-et-administration-plateforme.md`](../../besoins/W001/W001-T05-api-metiers-et-administration-plateforme.md)

## ADR techniques de référence (Draft)

Ces ADR décrivent des décisions techniques réutilisables. Leur statut Draft ne
vaut pas adoption par TeamPulse ; le présent ADR consigne la proposition
d'adoption locale et conserve lui-même son statut Draft.

- [ADR-TECH-013 — OpenAPI contracts and generated clients](../technical/ADR-TECH-013-openapi-contracts-and-generated-test-clients.md)
- [ADR-TECH-014 — HTTP boundary integration tests](../technical/ADR-TECH-014-http-boundary-integration-tests.md)
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
  clients générés sont compilés dans les sources de production du module afin
  d'être disponibles comme clients Java pour ses consommateurs. Le code de
  production du propriétaire ne les utilise pas pour appeler ses propres
  endpoints ; les tests qui doivent exercer le transport réseau peuvent les
  utiliser. La suite identité utilise `MockMvc` et n'exerce donc pas ce client.
  Le code serveur reste écrit selon les ports d'entrée existants et
  l'architecture hexagonale.

### 2. Orchestrer les commandes de plateforme par contrats Java synchrones

- `tp-administration` appelle les contrats Java publics des modules métier
  dans le flux de la commande lorsqu'il doit fournir immédiatement un résultat
  HTTP. Il n'utilise pas les événements pour commander une création ou obtenir
  son résultat.
- La création initiale d'une organisation initialise son statut à `CREATING`.
  Le premier utilisateur peut être créé ou invité, mais `INVITED` et `CREATING`
  restent `PENDING` selon `UserDirectory`.
- L'affectation d'un responsable respecte les invariants T04 : le responsable
  doit être `AVAILABLE`. T05 ne rend pas le premier compte disponible ; le
  bootstrap s'arrête donc en `CREATING` avant l'affectation finale. T05 n'expose
  pas l'activation/réactivation et ne doit pas permettre qu'une affectation de
  responsables la déclenche indirectement. W008 introduit l'activation du compte,
  l'affectation finale et la transition de l'organisation vers `ACTIVE`.
- `TenantContext` reste le tenant uniquement. T05 introduit un
  `ActorContext` distinct, réservé à l'audit/orchestration ; aucune
  authentification, affectation de rôle ou autorisation n'est décidée en T05.
  W008 fournit ces capacités.

### 3. Lier le profil local à une organisation persistée

Le profil `local` initialise une organisation de démonstration persistée et
en `CREATING`, puis fournit sa référence exacte au `TenantContextProvider`. Le
provider ne génère pas une référence en mémoire qui ne correspondrait pas à un
agrégat existant. Les opérations exigeant une organisation `ACTIVE` attendent
W008.
Les API T05 non authentifiées restent réservées à l'environnement local/de
développement jusqu'à W008 ; elles ne doivent pas être exposées à un réseau non
fiable.

### 4. Publier des faits sans inclure les notifications

- Le propriétaire publie `UserInvited` comme fait métier contenant des
  références uniquement. Il n'embarque pas les coordonnées ou autres PII.
  `OrganizationActivated` est réservé à W008, qui introduira la transition
  effective vers `ACTIVE`. L'adaptateur consommateur résout ultérieurement les
  coordonnées auprès d'un contrat de lecture du module identité.
- L'orchestration synchrone ne dépend pas de ces événements. Les événements
  traitent les effets secondaires ; aucun accusé de réception métier n'est
  ajouté à la réponse HTTP.
- T05 ne crée pas `tp-notification`, ne persiste pas de notifications et
  n'envoie aucun email. Le besoin actuellement envisagé sous W001-T10, encore
  Draft et non officialisé dans la roadmap canonique, pourra ajouter les
  consommateurs TeamPulse et le moteur réutilisable de persistance/livraison.
  Les invitations créées avant qu'un consommateur soit disponible ne
  déclenchent pas d'email rétroactif.

### 5. Standardiser les frontières HTTP et leur preuve

- Les ADR-TECH-013 à 016 fournissent les principes génériques. L'adoption
  proposée pour TeamPulse, ses modules, ses versions et ses choix de test est
  consignée dans le contrat local ci-dessous.
- Les erreurs métier restent codées et traduites par leur module propriétaire.
- Les tests de contrôleur gardent l'application et la vraie persistance
  PostgreSQL. Identité utilise le contexte complet avec `MockMvc` ; organisation
  et équipe utilisent un serveur réel sur port aléatoire. Le test HTTP
  d'administration, à ajouter avec son module, utilisera également un serveur
  réel conformément à l'acceptation T05. Les tests `AbstractIT` existants sans
  serveur restent inchangés ; les bases Web dédiées réutilisent leur support.
- Tous les modules du reactor conservent la version commune du parent Maven,
  actuellement `0.1.0-SNAPSHOT`. Aucune version de produit indépendante par
  module ou version d'API d'URL n'est ajoutée par défaut.

### Contrat d'adoption TeamPulse

Cette adoption est proposée par T05 ; elle ne change pas le statut Draft de cet
ADR ni celui des ADR techniques de référence.

- Propriétaires HTTP proposés : `tp-identity`, `tp-organization`, `tp-team` et
  `tp-administration`, chacun exposant la spec générée depuis ses contrôleurs,
  DTO et contraintes de validation avec SpringDoc `3.1.1`, version gérée dans le
  POM parent. Le build crée la spec sous
  `target/openapi.yaml` depuis l'application HTTP réelle avec Flyway et
  PostgreSQL Testcontainers, puis génère les clients sous
  `target/generated-sources/openapi`. Pour identité, la spec est servie par SpringDoc sur
  `/v3/api-docs.yaml`, sans classe de configuration OpenAPI dédiée.
  Les endpoints OpenAPI sont désactivés par défaut, activés uniquement dans les
  tests et le profil local.
- Les clients Java `restclient` et modèles sont générés par OpenAPI Generator
  Maven `7.25.0`, configuré dans le POM parent, à partir du YAML généré pendant
  `generate-test-sources`. Ils sont placés dans
  `target/generated-sources/openapi` et attachés aux sources de production. Le
  build recompile les sources de production pendant `process-test-sources`,
  après la génération, avant la compilation des tests. Les tests réutilisent
  donc les mêmes classes sans seconde copie. L'implémentation de chaque
  propriétaire ne dépend pas de son package `.generated` ; les appels
  synchrones inter-modules de T05 utilisent les contrats Java publics. Les
  consommateurs futurs peuvent utiliser le client généré selon leur propre
  adoption. Les options
  `useSpringBoot4=true`, `useJackson3=true` et
  `openApiNullable=false` ciblent le runtime Java 25 / Spring Boot 4.1.0. La
  version de l'artefact client vient du parent Maven ; le serveur utilise l'URL
  relative `/` pour ne pas figer hôte ou port.
- Les changements du générateur qui modifient ces classes doivent être relus
  comme des changements de l'API Java publiée par le module.
- Tests MVC retenus pour identité : `@SpringBootTest` avec
  `WebEnvironment.MOCK` et `@AutoConfigureMockMvc`, l'application complète,
  Flyway et PostgreSQL Testcontainers. Aucun cas d'usage interne n'est mocké et
  aucun `@WebMvcTest` n'est utilisé. Cette adoption vérifie le dispatch Spring
  MVC, le mapping, le chemin applicatif et les effets persistés ; elle ne prouve
  ni le démarrage du serveur embarqué, ni le transport réseau, ni le client
  généré. Organisation et équipe utilisent un serveur réel à port aléatoire ;
  l'API d'administration à venir suivra la même stratégie. Les bases
  `AbstractIT` non-Web restent en
  `WebEnvironment.NONE`. Les tests autonomes remplacent uniquement les contrats
  publics externes ; les tests d'administration assemblent les vrais modules
  concernés.
- Erreurs HTTP proposées : conserver `ApiError` comme DTO Java pur dans
  `tp-common` et placer le support Spring partagé dans `tp-web-support`, sans
  dépendance aux modules métier. Chaque propriétaire garderait ses codes et
  mappings d'erreurs métier ; les réponses HTTP ne divulgueraient ni PII ni
  détail technique.
- Événements proposés : les commandes synchrones passeraient par les contrats
  Java publics. `UserInvited` serait un fait local contenant des références
  seulement. T05 n'ajouterait aucun listener de notification, registry durable
  ou rejeu historique ; `OrganizationActivated` attendrait W008.
- Versions locales : le parent définit Java 25, Spring Boot 4.1.0, Spring
  Modulith 2.0.6 et la version produit `0.1.0-SNAPSHOT`. La version concrète du
  générateur OpenAPI et les options de génération sont des choix TeamPulse ;
  elles ne sont pas des contraintes des ADR techniques réutilisables.
- Les validations et preuves de livraison propres à TeamPulse seront
  consignées dans cet ADR, notamment dans ses Notes. Les modules, références
  W001, versions concrètes et événements métier ne figurent pas dans les ADR
  techniques de référence ; ceux-ci ne portent pas l'historique
  d'implémentation du projet.

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
- Activer une organisation avec des responsables `PENDING` : rejeté, car cela
  contredirait les invariants déjà livrés et validés en T04.
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
- Les tests prouvent le comportement MVC et applicatif avec les modules et
  PostgreSQL réels ; les tests à port aléatoire prouvent aussi le démarrage du
  serveur et le transport réseau.
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
- Le client généré entre dans l'artefact de production et son contrat Java peut
  évoluer lors d'un changement de schéma ou de version du générateur. La
  génération après le démarrage de l'application impose aussi une compilation
  de production supplémentaire après OpenAPI Generator.
- Les APIs sans authentification constituent un risque si le profil local est
  utilisé ou exposé hors d'un environnement maîtrisé.

## Impact technique
- Modules : `tp-identity`, `tp-organization`, `tp-team`, nouveau
  `tp-administration`, `tp-app`, `tp-common`, nouveau `tp-web-support` et
  `tp-test-support`.
- Contrats : HTTP/OpenAPI par propriétaire ; contrats Java synchrones pour
  l'orchestration de plateforme ; fait `UserInvited` en T05 et
  `OrganizationActivated` à partir de W008, avec références seulement.
- Tests : PostgreSQL Testcontainers pour les propriétaires HTTP ; identité
  utilise `MockMvc` avec l'application complète, organisation et équipe
  utilisent un serveur réel sur port aléatoire. L'API d'administration à venir
  suivra le même contrat à port aléatoire. Les DSL métier éventuelles restent
  locales à chaque module.
- Sécurité : profil local/de développement uniquement jusqu'à W008 ; aucun
  rôle, permission ou JWT introduit par cette décision.
- Hors impact T05 : module de notification, persistance/livraison email, SMS et
  endpoints de lecture d'inbox.

## Validation
- Vérifier les contrats OpenAPI et le code généré par le build Maven.
- Exécuter les tests d'intégration/PostgreSQL de chaque propriétaire au niveau
  de transport adopté, les tests des contrôles T04, puis
  `./mvnw --batch-mode --no-transfer-progress verify`. La preuve de démarrage et
  de transport réseau est attendue uniquement des suites à port aléatoire.
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
- Les consommateurs d'événements sont au moins une fois et doivent être
  idempotents ; une défaillance de listener n'est pas un échec de la commande
  HTTP initiale.
- L'absence d'envoi rétroactif avant T10 est une limite fonctionnelle assumée.
- Un provider local lié à une référence non persistée créerait un tenant
  incohérent ; le démarrage local doit échouer clairement si l'organisation de
  démonstration ne peut être initialisée.

## Notes
- Les changements de décision intervenus pendant l'analyse sont conservés ici :
  notification/email déplacés de T05 vers T10 ; activation effective de
  l'organisation et publication de `OrganizationActivated` différées à W008.
- Les documents Draft T08-T14 de `ztmp` ne sont pas officialisés par cet ADR.
- L'implémentation de `W001-T05-http-test-support` centralise les bases JPA et
  MVC dans `tp-test-support`, avec `MockMvc`, Jackson et le nettoyage des
  écritures PostgreSQL. Organisation et équipe gardent leurs bases de test avec
  serveur réel sur port aléatoire ; identité utilise `MockMvc` en contexte
  Spring complet. Le module d'administration n'est pas encore présent dans
  cette branche ; sa future suite suivra le contrat à port aléatoire. Cette
  adoption ne prétend pas prouver l'écoute réseau pour identité. Les bases
  `AbstractIT` existantes en `NONE` restent inchangées.
  Le statut de cet ADR reste `Draft`.
- L'implémentation de `W001-T05-shared-http-errors` ajoute `ApiError` avec les
  champs `code` et `message`, ainsi que l'advice Spring partagé auto-configuré
  dans `tp-web-support`. Les détails techniques et les données personnelles ne
  sont pas renvoyés dans les réponses ; les mappings métier restent locaux et
  prioritaires. `tp-common` et `tp-web-support` passent `verify`. Les preuves
  des verticales propriétaires suivent leur niveau de transport adopté ; le
  statut de cet ADR reste `Draft`.
- L'implémentation de `W001-T05-openapi-test-clients` fixe la version du
  générateur dans le POM parent. La configuration actuelle attache les clients
  générés comme sources de production sous `target/generated-sources/openapi` ;
  un passage de compilation après la génération les inclut dans l'artefact avant
  la compilation des tests. La verticale identité génère d'abord le YAML sous
  `target/openapi.yaml` depuis l'application démarrée pour SpringDoc sur le port
  technique `18081`. `IdentityOpenApiConfiguration` remplace l'adresse de ce
  serveur temporaire par `/` et transmet la version Maven du parent dans
  `info.version` ; OpenAPI Generator utilise ces métadonnées, pas l'adresse du
  serveur de génération. Le client Java généré n'embarque donc pas `18081` ;
  OpenAPI Generator utilise `http://localhost` comme base par défaut pour une
  URL relative, et chaque consommateur configure `ApiClient.setBasePath()` pour
  son environnement. La règle d'architecture R12 empêche le code de production
  d'un propriétaire de dépendre de son propre package `.generated` ; une suite
  qui doit prouver le transport réseau peut utiliser le client généré. Les autres
  propriétaires configurent le même ordre dans leurs verticales. Le statut de
  cet ADR reste `Draft`.
- L'implémentation de `W001-T05-identity-api` expose `POST /api/users`,
  `POST /api/users/invitations` et `GET /api/users`. Chaque route tenantée
  résout une fois le `TenantContext` et les écritures utilisent le tenant du
  provider même si le corps contient une référence d'organisation inconnue du
  contrat. L'invitation persiste `INVITED` et publie `UserInvited` avec les
  références utilisateur/organisation seulement, via le port sortant
  `IdentityEventPublisher` et son adapter Spring dans
  `infrastructure.messaging`. La couverture du contrôleur
  utilise maintenant `UserControllerIT`, `MockMvc` et le contexte Spring
  complet avec PostgreSQL Testcontainers ; elle ne vérifie pas l'écoute réseau.
  Aucun build ni test n'a été exécuté sur le SHA relu ; les artefacts présents
  dans `target` ne prouvent pas sa validation. La commande ciblée à relancer est
  `./mvnw --batch-mode --no-transfer-progress -pl tp-identity -am
  -Dtest=UserControllerIT -Dsurefire.failIfNoSpecifiedTests=false test`.
- Le contrat Java public `identity::lifecycle` fournit à `tp-administration`
  les commandes synchrones de création, invitation, suspension et
  désactivation avec des types de contrat sans modèle domaine identité. Le
  contrat `UserLifecycle` constitue l'unique port entrant pour ces commandes et
  est implémenté directement par `UserLifecycleService`, utilisé par le
  contrôleur HTTP et l'orchestration d'administration. `UserProvisioningCommand`
  et `UserResponse` gardent l'API Java indépendante des commandes et
  modèles internes ; la lecture reste portée séparément par
  `ListUsersUseCase`. L'adaptateur `UserLifecycleContractService` et le port
  interne `UserLifecycleUseCase` sont supprimés pour éviter de répéter la même
  capacité et son mapping.

  L'administration reste responsable des vérifications de responsabilités T04
  avant les deux transitions de fermeture. Aucun endpoint HTTP identité ne
  contourne cette orchestration. Le mapping métier identité prévaut sur l'advice
  partagé ; les erreurs de transport restent génériques et masquées. Cet ADR
  reste `Draft`.
