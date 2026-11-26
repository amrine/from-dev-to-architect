# W001-T05 — APIs métier et administration de plateforme

## Statut
Draft

## Ticket lié
W001-T05 — APIs métier et administration de plateforme

## Source roadmap
- Semaine : W001
- Phase : fondations du backend TeamPulse
- Objectif public : exposer la première verticale HTTP TeamPulse.
- Raffinement accepté : couvrir les capacités et contrôles livrés en T04 dans
  `tp-identity`, `tp-organization` et `tp-team`, et distinguer
  l'administration de la plateforme.

## Contexte
W001-T04 a livré les modèles métier, cas d'usage, contrats inter-modules et
persistance des utilisateurs, organisations, équipes et appartenances. Ces
capacités restent la propriété de leurs modules. T05 ajoute les frontières
HTTP et les preuves d'intégration de bout en bout ; il ne recrée pas les
modèles, repositories ou migrations déjà livrés.

Le parcours d'administration initial traverse plusieurs propriétaires. Le
module `tp-administration` représente l'administration de la plateforme, tandis
que les API métier restent dans les modules qui possèdent les données.

## Problème à résoudre
Les capacités T04 ne sont pas encore utilisables via HTTP. Il faut exposer des
contrats cohérents, exécuter le bootstrap de plateforme sans déplacer la
propriété métier, garantir les contrôles tenantés et prouver les comportements
des contrôleurs avec de vrais échanges HTTP et PostgreSQL.

## Objectif
Exposer les API HTTP des trois modules métier et de l'administration de
plateforme, ajouter le minimum d'orchestration synchrone nécessaire au
provisioning initial et établir un contrat HTTP testable, réutilisable et
compatible avec l'architecture modulaire.

## Périmètre inclus
- Ajouter les adapters HTTP appartenant à `tp-identity`, `tp-organization` et
  `tp-team` pour les capacités T04 destinées à être exposées : création et
  invitation d'utilisateurs, lecture tenantée, cycle de vie des utilisateurs,
  cycle de vie des organisations hors transitions vers `ACTIVE` (activation et
  réactivation, reportées à W008), affectation/remplacement des responsables
  lorsque l'opération ne déclenche pas ces transitions, cycle de vie des équipes
  et gestion de leurs membres. L'affectation qui activerait l'organisation est
  reportée à W008. Chaque module conserve la validation de ses invariants et le
  mapping de ses erreurs métier.
- Créer `tp-administration`, propriétaire des seuls parcours d'administration
  de la plateforme. Il orchestre les contrats Java publics des modules métier
  de façon synchrone pour obtenir un résultat final dans la réponse HTTP ; il
  ne possède ni leurs données, ni leurs repositories, ni leurs règles métier.
- Permettre au bootstrap de créer une organisation en `CREATING` et de créer ou
  inviter son premier utilisateur. Le compte créé ou invité reste indisponible
  pour une responsabilité tant qu'il n'est pas `AVAILABLE` ; T05 ne fournit pas
  le parcours qui le rend disponible. Le bootstrap s'arrête donc avant
  l'affectation finale des responsables et l'organisation reste en `CREATING`.
  L'activation et la réactivation d'une organisation, y compris toute transition
  automatique vers `ACTIVE` déclenchée par l'affectation de responsables, sont
  reportées à W008 après l'activation du compte par le parcours d'authentification.
- Introduire un `ActorContext` distinct de `TenantContext`, réservé à l'audit et
  à l'orchestration. Les contrôleurs tenantés
  obtiennent le tenant depuis `TenantContextProvider.current()` une seule fois
  par requête et ne l'acceptent jamais depuis le body, le path, une query ou un
  header libre. `ActorContext` reste une information technique distincte et ne
  porte ni authentification, ni rôle, ni permission.
- Remplacer, pour le profil `local`, le tenant aléatoire en mémoire par une
  organisation de démonstration persistée en `CREATING`. Le provider local doit
  retourner la référence exacte de cette organisation, obtenue depuis une
  frontière publique ou une configuration liée à son initialisation ; il ne
  génère pas une référence tenant différente. Les opérations qui exigent une
  organisation `ACTIVE` restent indisponibles jusqu'à W008.
- Publier le fait métier `UserInvited` avec des références seulement, sans
  email ni autre PII. `OrganizationActivated` ne peut être publié qu'avec la
  transition effective introduite en W008. Ces faits ne sont pas des commandes
  et ne modifient pas la réponse synchrone des API. T05 ne livre ni listener,
  ni registry durable, ni rejeu historique. L'adoption d'une livraison durable
  Spring Modulith est à évaluer dans un ADR TeamPulse lorsqu'un vrai listener
  disposera d'un besoin de livraison formulé, conformément à ADR-TECH-016.
- Définir un contrat OpenAPI par propriétaire HTTP : identité, organisation,
  équipe et administration de plateforme. Générer les clients HTTP de test
  depuis ces contrats, puis conserver une DSL lisible propre à chaque module
  dans son `src/test`. Le support HTTP générique reste dans `tp-test-support` ;
  aucun module autonome `tp-test-clients` n'est créé.
- Ajouter `tp-web-support` pour le support Spring partagé des erreurs HTTP. Le
  DTO de réponse reste un type Java pur dans `tp-common` ; chaque module traduit
  ses propres erreurs métier et codes HTTP.
- Écrire les tests de contrôleurs exclusivement comme de vrais tests
  d'intégration par module : requête HTTP vers un serveur démarré sur port
  aléatoire, application Spring réelle, Flyway et PostgreSQL Testcontainers. Les
  tests traversent le contrôleur, les cas d'usage, la persistance et le mapping.
  Cette exigence est l'adoption locale TeamPulse ; la recommandation générique
  de slice de l'ADR-TECH-011 reste une option ciblée pour d'autres projets et
  ne remplace pas cette preuve HTTP.
- Réutiliser le support et les fixtures de `AbstractIntegrationTest` sans
  transformer ses tests existants `WebEnvironment.NONE` en tests avec serveur ;
  ajouter une base d'intégration Web dédiée par application de test.
- Remplacer uniquement les contrats d'autres modules aux frontières des tests :
  `UserDirectory` dans les tests autonomes de `tp-organization`,
  `UserDirectory` et `OrganizationDirectory` dans ceux de `tp-team`. Le chemin
  interne au module testé reste réel. Les tests d'administration assemblent les
  vrais modules dont l'orchestration est le sujet.
- Utiliser la version produit commune déjà portée par le Maven parent
  (`0.1.0-SNAPSHOT` pendant le développement) pour les métadonnées de contrat et
  la génération des clients ; ne pas créer de version Maven par module ni
  introduire un versionnement d'URL indépendant sans décision explicite.

## Périmètre exclu
- Créer `tp-notification`, persister des notifications, envoyer des emails ou
  des SMS, configurer un SMTP, fournir une inbox ou des outils de reprise : le
  module de notification et sa livraison sont séparés de T05. Le besoin de
  travail envisagé sous l'identifiant W001-T10 est encore Draft et non
  officialisé dans la roadmap canonique. Aucun email informatif n'est envoyé en
  T05.
- Rejouer rétroactivement les faits émis avant la présence d'un consommateur
  T10 ; les notifications ne commencent qu'une fois leur consommateur livré.
- Accepter une invitation avec un token, configurer des identifiants ou activer
  un compte utilisateur. Ces parcours relèvent de W008.
- Exposer ou exécuter en T05 l'activation/réactivation d'une organisation, y
  compris les chemins T04 où l'affectation des derniers responsables
  `AVAILABLE` provoque automatiquement une transition vers `ACTIVE`. Le
  bootstrap d'une nouvelle organisation reste en `CREATING` jusqu'à W008.
- Authentifier les requêtes, émettre ou valider des JWT, décider des rôles,
  permissions ou autorisations. W008 fournit l'identité authentifiée et les
  contrôles d'accès. Avant W008, les API T05 sont limitées à l'environnement
  local/de développement et ne doivent pas être exposées à un réseau non fiable.
- Extraire les modules en microservices ou introduire un broker externe.
- Recréer les domaines, tables, repositories et migrations déjà livrés en T04,
  ou faire porter les règles métier par les contrôleurs ou `tp-administration`.
- Ajouter une UI, un CRUD générique sans cas d'usage, ou une DSL métier partagée
  entre tous les modules.

## Règles métier / techniques
- Les modules propriétaires gardent chacun leur contrat HTTP, leurs DTO de
  transport, leur mapping métier et leur vocabulaire d'erreur.
- Les appels qui doivent fournir immédiatement un résultat (provisioning,
  attribution d'un responsable, changement d'état) utilisent les contrats Java
  publics synchrones. Les événements ne remplacent pas ces commandes.
- `UserInvited` est publié après l'invitation et ne contient que des références.
  `OrganizationActivated` sera publié avec la transition vers `ACTIVE` introduite
  en W008. Les consommateurs éventuels sont idempotents ; aucun accusé de
  réception métier ne remonte au demandeur HTTP.
- Les routes tenantées transmettent exactement le `TenantContext` fourni par le
  provider ; aucune référence de tenant fournie librement par l'appelant n'est
  acceptée.
- Une simple appartenance `TeamMember` ne bloque pas la suspension ou la
  désactivation d'un utilisateur ; une responsabilité active d'administrateur
  ou de manager doit être retirée ou transférée conformément à T04.
- Les contrôles métier et les traductions d'erreurs sont vérifiés par le
  comportement HTTP observable, pas par des interactions Mockito avec les
  contrôleurs ou leurs dépendances internes.
- Les contrats OpenAPI et clients générés reflètent la version partagée du
  produit et ne deviennent pas un second catalogue de règles métier.

## Critères d'acceptation
- [ ] Les contrats HTTP sont séparés selon leurs propriétaires : identité,
  organisation, équipe et administration de plateforme.
- [ ] Les adapters exposent les capacités T04 retenues sans accès aux entités
  JPA ou repositories d'un autre module.
- [ ] `tp-administration` orchestre les contrats Java publics synchrones et ne
  possède ni données métier ni CRUD des modules.
- [ ] Le bootstrap crée une organisation `CREATING` et permet la création ou
  l'invitation du premier utilisateur. La règle d'éligibilité d'un candidat
  responsable reste à arbitrer entre le comportement T04 qui autorise
  `PENDING` dans certains parcours et la proposition T05 Draft `AVAILABLE`
  uniquement ; aucun parcours T05 ne fait passer
  une organisation à `ACTIVE`, y compris indirectement par affectation de
  responsables. L'activation du compte, l'affectation finale et les transitions
  vers `ACTIVE` attendent W008.
- [ ] Les contrôles T04 sur les responsabilités restent appliqués lors des
  opérations HTTP de suspension/désactivation d'un utilisateur.
- [ ] Les requêtes tenantées utilisent le contexte local fourni et ne peuvent
  pas choisir leur tenant via un champ ou header contrôlé par l'appelant.
- [ ] Le profil `local` utilise une organisation de démonstration persistée en
  `CREATING` et le provider retourne sa référence réellement persistée ; aucune
  opération exigeant `ACTIVE` ne réussit avant W008.
- [ ] `UserInvited` ne transporte aucune PII ; `OrganizationActivated` n'est
  pas émis en T05. T05 n'inclut ni consommateur de notification ni livraison
  email.
- [ ] Chaque contrôleur est couvert par des tests d'intégration HTTP réels, sans
  tests unitaires de contrôleur ni slice `@WebMvcTest`/`MockMvc`. Les tests
  utilisent PostgreSQL réel via Testcontainers et vérifient les réponses, les
  erreurs, l'isolation tenant et les effets persistés pertinents.
- [ ] Les stubs sont limités aux contrats inter-modules hors du propriétaire du
  test ; les composants internes du module testé ne sont pas remplacés.
- [ ] Les clients HTTP générés compilent à partir des contrats OpenAPI et sont
  enveloppés par une DSL métier locale à chaque `src/test`.
- [ ] Le support partagé d'erreur HTTP ne centralise aucun code métier et
  n'expose ni cause technique, ni message interne, ni donnée personnelle.
- [ ] Les règles ArchUnit et Spring Modulith continuent de vérifier les
  frontières et `tp-test-support` reste limité au scope test.

## Arbitrages restant ouverts

Ces points ne sont pas décidés par le statut Draft du besoin ou de l'ADR. Les
changements qui en dépendent attendent une décision explicite.

- **Candidats responsables `PENDING`** : T04 Accepted autorise ce statut dans
  certains parcours ; le besoin T05 Draft propose de limiter l'éligibilité à
  `AVAILABLE`. Jusqu'à l'arbitrage, T05 ne modifie pas la règle acceptée en T04.
- **Provisioning partiel** : le code crée actuellement l'organisation avant
  l'utilisateur sans transaction englobante ; un échec utilisateur peut donc
  laisser une organisation `CREATING` dont la référence n'est pas rendue au
  demandeur. Décider entre tout-ou-rien et un résultat partiel récupérable avec
  procédure et résultat observables.
- **Tenant local entre redémarrages** : l'initialisation paresseuse avec retry
  suit T04 Accepted. L'implémentation mémorise le contexte et crée une
  organisation de démonstration par processus ; décider si ce tenant reste par
  lancement ou s'il doit être retrouvé/réutilisé après redémarrage.

## Validation attendue
- Valider les contrats OpenAPI et exécuter leur génération de clients pendant
  le cycle Maven approprié.
- Exécuter les tests d'intégration Web et PostgreSQL de chacun des modules
  propriétaires, puis la suite complète :
  `./mvnw --batch-mode --no-transfer-progress verify`.
- Vérifier les contrats Modulith, les règles ArchUnit, l'absence de dépendance
  production vers `tp-test-support` et l'absence d'accès inter-module aux
  packages internes.
- Vérifier les réponses HTTP et l'état persistant après les commandes, y
  compris le refus d'affectation quand un responsable n'est pas `AVAILABLE` et
  l'absence de toute transition vers `ACTIVE` en T05.
- Revoir manuellement le diff documentaire, les liens besoin/ADR et les
  changements de périmètre T04/T05/W008/T10.

## Artefacts attendus
- Contrats OpenAPI des quatre propriétaires HTTP.
- Clients générés pour les tests et DSL métier dans chaque module concerné.
- Support générique HTTP dans `tp-test-support` et support Spring d'erreurs dans
  `tp-web-support`.
- Tests d'intégration Web/PostgreSQL par module et tests du bootstrap plateforme.
- Ce besoin, l'ADR TeamPulse W001-T05 et les ADR techniques référencés.

## Notes pédagogiques
Ce ticket distingue l'ownership HTTP et métier, l'orchestration synchrone d'une
commande des effets secondaires asynchrones, le contexte de tenant du contexte
d'acteur, et un vrai test HTTP/PostgreSQL d'une simulation de contrôleur.

## Liens avec les autres tickets
- Dépend de : W001-T01 à W001-T04 pour le monolithe modulaire, PostgreSQL,
  l'architecture et les capacités métier déjà livrées.
- Prépare : le besoin de notification envisagé en W001-T10 (Draft, non
  officialisé) pour `tp-notification` et la consommation des faits
  `UserInvited` ; la consommation de `OrganizationActivated` attend que W008
  introduise cette transition et son fait métier.
- Dépend fonctionnellement de : W008 pour l'acceptation des invitations,
  l'activation des comptes, l'authentification et l'activation des nouvelles
  organisations une fois leurs responsables disponibles.

## Décision associée
Voir [ADR-W001-T05 — APIs métier et administration de plateforme](../../adr/W001/ADR-W001-T05-api-metiers-et-administration-plateforme.md).
