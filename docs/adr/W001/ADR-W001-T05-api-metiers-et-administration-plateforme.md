# ADR-W001-T05 — APIs métier et administration de plateforme

## Statut
Draft

## Ticket lié
W001-T05 — APIs métier et administration de plateforme

## Besoin associé
[`docs/besoins/W001/W001-T05-api-metiers-et-administration-plateforme.md`](../../besoins/W001/W001-T05-api-metiers-et-administration-plateforme.md)

## ADR techniques adoptés
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

- Les détails sont délégués aux ADR-TECH-013 à 016 : contrats OpenAPI et
  clients de test générés, vrais tests HTTP/PostgreSQL par module, support
  Spring d'erreurs séparé du DTO Java pur et publications d'événements
  récupérables.
- Les erreurs métier restent codées et traduites par leur module propriétaire.
- Le test de contrôleur traverse un serveur réel sur port aléatoire et la vraie
  persistence PostgreSQL. Les tests `AbstractIntegrationTest` existants sans
  serveur restent inchangés ; une base Web dédiée réutilise leur support.
- Tous les modules du reactor conservent la version commune du parent Maven,
  actuellement `0.1.0-SNAPSHOT`. Aucune version de produit indépendante par
  module ou version d'API d'URL n'est ajoutée par défaut.

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
- L'implémentation de `W001-T05-http-test-support` prépare le support
  `RestTestClient`, le nettoyage des écritures PostgreSQL et les bases de test
  avec serveur réel dédiées à identité, organisation et équipe. Les bases
  `AbstractIntegrationTest` existantes en `NONE` restent inchangées. La
  compilation des sources de test passe ; le parcours HTTP réel
  sera prouvé par la première verticale API. Le statut de cet ADR reste `Draft`.
- L'implémentation de `W001-T05-shared-http-errors` ajoute `ApiError` avec les
  champs `code` et `message`, ainsi que l'advice Spring partagé auto-configuré
  dans `tp-web-support`. Les détails techniques et les données personnelles ne
  sont pas renvoyés dans les réponses ; les mappings métier restent locaux et
  prioritaires. `tp-common` et `tp-web-support` passent `verify`. La preuve
  HTTP réelle sera apportée par les verticales propriétaires ; le statut de cet
  ADR reste `Draft`.
- L'implémentation de `W001-T05-openapi-test-clients` fixe la version du
  générateur dans le POM parent et configure la génération de clients Java
  uniquement pour les sources de test, sous `target/generated-test-sources`.
  La verticale identité fournit maintenant le premier contrat OpenAPI et la
  compilation de son client généré ; les autres propriétaires ajoutent leurs
  contrats dans leurs verticales. Le statut de cet ADR reste `Draft`.
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
