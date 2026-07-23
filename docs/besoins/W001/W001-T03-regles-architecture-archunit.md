# W001-T03 - Regles d'architecture executables avec ArchUnit

## Statut
Accepted

## Objectif
Transformer les conventions d'architecture internes aux modules metier en regles executables, afin que le build detecte automatiquement une dependance interdite avant qu'elle ne s'installe dans le code.

## Besoin
Spring Modulith verifie deja les frontieres entre les modules `organization`, `identity`, `team` et `common`.
Cette verification ne decrit pas completement l'organisation interne d'un module metier.

Avant d'ajouter les premieres classes metier, les entites et les adapters de persistence, TeamPulse doit definir la direction autorisee des dependances entre le domaine, les cas d'usage et l'infrastructure.
Ces regles doivent etre documentees puis verifiees par des tests ArchUnit executes avec le build Maven.

Le ticket ne doit pas ajouter ArchUnit pour lui-meme. Il doit rendre les choix d'architecture hexagonale explicites, comprehensibles et automatiquement verifiables.

## Probleme a resoudre
Sans verification automatique, un module peut progressivement accumuler des dependances contraires a l'architecture cible, par exemple :
- une entite du domaine annotee avec JPA ;
- un service applicatif qui appelle directement un repository Spring Data ;
- un controleur REST qui utilise directement un adapter de persistence ;
- une API publique qui expose une entite JPA ou une classe interne ;
- une configuration technique appelee depuis le domaine ;
- un cycle entre les packages `domain`, `application` et `infrastructure`.

Ces erreurs compilent souvent correctement et peuvent rester invisibles tant qu'une evolution ou une extraction de module ne les revele pas.

## Responsabilite des outils
Les trois niveaux de verification doivent rester complementaires :

| Outil | Responsabilite |
| --- | --- |
| Maven | Controler les dependances physiques entre les modules Maven. |
| Spring Modulith | Controler les frontieres, cycles et dependances autorisees entre les modules metier. |
| ArchUnit | Controler la structure et la direction des dependances a l'interieur de chaque module. |

ArchUnit ne doit pas dupliquer les regles deja couvertes par `ApplicationModules.verify()`.
Le test Spring Modulith existant doit etre conserve.

## Structure cible d'un module metier
Chaque module `identity`, `organization` et `team` doit pouvoir adopter progressivement la structure suivante :

```text
io.teampulse.<module>
+-- api
+-- events
+-- domain
+-- application
|   +-- port
|   |   +-- in
|   |   +-- out
|   +-- service
+-- infrastructure
|   +-- web
|   +-- persistence
|   +-- messaging
+-- config
```

Tous ces packages ne doivent pas etre crees artificiellement pendant T03.
Ils apparaissent lorsqu'un besoin fonctionnel ou technique introduit les classes correspondantes.
Les regles ci-dessous constituent neanmoins le contrat a respecter des leur apparition.

## Role des packages

### `api`
- Porte les contrats Java synchrones exposes aux autres modules.
- Contient uniquement des interfaces, commandes, resultats et objets de transport stables.
- Ne contient aucune implementation de cas d'usage.
- N'expose aucune entite JPA, aucun repository et aucune classe interne du module.

### `events`
- Porte les evenements publics emis par le module.
- Les evenements representent des faits passes et sont immuables.
- Ils ne transportent ni entite JPA, ni repository, ni classe d'infrastructure.
- Ils ne dependent pas des packages internes `application` ou `infrastructure`.

### `domain`
- Porte les entites metier, value objects, agregats, regles et services de domaine.
- Reste independant de Spring, Spring Data, JPA, du Web et des adapters techniques.
- Peut utiliser la bibliotheque standard Java et des types metier neutres de `tp-common`.
- Ne depend jamais de `application`, `infrastructure`, `config`, `api` ou `events`.

### `application`
- Orchestre les cas d'usage du module.
- Depend du domaine et manipule ses modeles.
- Definit les ports entrants et sortants necessaires aux cas d'usage.
- Peut implementer un contrat public defini dans `api` et produire un type defini dans `events`.
- Ne depend jamais d'une implementation situee dans `infrastructure` ou `config`.
- Ne manipule directement ni controleur REST, ni repository Spring Data, ni entite JPA.

### `infrastructure`
- Porte les adapters techniques entrants et sortants.
- Peut dependre de `application`, de `domain`, de `api` et de `events` pour adapter le monde exterieur aux contrats internes.
- `infrastructure.web` appelle un port entrant ; il n'appelle pas directement la persistence.
- `infrastructure.persistence` implemente les ports sortants ; il n'est pas appele directement par le Web.
- Les adapters Web, persistence et messaging ne doivent pas se dependre directement entre eux.

### `config`
- Porte l'assemblage technique Spring propre au module.
- Peut creer et relier les beans du domaine, de l'application et de l'infrastructure.
- Peut porter une configuration technique telle que la configuration Flyway du module.
- Ne contient aucune regle metier.
- Aucun package `domain` ou `application` ne doit dependre de `config`.

## Matrice des dependances internes
La matrice suivante exprime les dependances autorisees entre les zones d'un meme module.
La bibliotheque standard Java et les types neutres autorises de `tp-common` sont implicites.

| Zone source | Peut dependre de | Ne doit pas dependre de |
| --- | --- | --- |
| `api` | Types publics autonomes, `tp-common` | `domain`, `application`, `infrastructure`, `config` |
| `events` | Types publics autonomes, `tp-common` | `domain`, `application`, `infrastructure`, `config` |
| `domain` | `tp-common` | `api`, `events`, `application`, `infrastructure`, `config` |
| `application` | `domain`, `api`, `events`, `tp-common` | `infrastructure`, `config` |
| `infrastructure` | `application`, `domain`, `api`, `events`, `tp-common` | Autre adapter d'infrastructure sans passer par un port |
| `config` | `application`, `domain`, `infrastructure`, `api`, `events` | Regle metier ou cas d'usage |

La direction principale des dependances doit rester orientee vers le coeur :

```text
infrastructure --> application --> domain
       config --> assemblage des composants
```

Le domaine ne connait jamais les couches placees a sa gauche.

## Regles ArchUnit obligatoires

### R01 - Purete du domaine
- Les classes de `..domain..` ne dependent d'aucun type Spring.
- Elles ne dependent pas de `jakarta.persistence`.
- Elles ne dependent pas des packages Web, persistence, messaging ou configuration.
- Elles ne portent pas d'annotations `@Entity`, `@Embeddable`, `@Controller`, `@Service`, `@Repository` ou `@Configuration`.

### R02 - Direction application vers domaine
- Les classes de `..application..` peuvent dependre de `..domain..`.
- Les classes de `..domain..` ne dependent jamais de `..application..`.
- Les classes de `..application..` ne dependent pas de `..infrastructure..` ni de `..config..`.

### R03 - Ports possedes par l'application
- Les ports entrants et sortants appartiennent a `..application.port.in..` et `..application.port.out..`.
- Un adapter entrant depend d'un port entrant.
- Un adapter sortant implemente un port sortant.
- Un port ne depend jamais de l'adapter qui l'implemente.

### R04 - Isolation de la persistence
- Les types JPA et Spring Data restent dans `..infrastructure.persistence..`.
- Les interfaces et implementations Spring Data ne sont pas utilisees directement par `domain` ou `application`.
- Les objets de persistence ne traversent pas les frontieres publiques `api` et `events`.

### R05 - Isolation du Web
- Les controleurs et types propres au transport HTTP restent dans `..infrastructure.web..`.
- Un controleur depend d'un port entrant.
- Un controleur ne depend jamais directement d'un repository ou d'un adapter de persistence.
- Aucun type Web n'apparait dans `domain`, `application`, `api` ou `events`.

### R06 - Isolation des adapters
- `..infrastructure.web..`, `..infrastructure.persistence..` et `..infrastructure.messaging..` ne se dependent pas directement.
- Toute collaboration entre adapters passe par un port ou un cas d'usage de `application`.

### R07 - Surface publique du module
- Seuls `api` et `events` constituent des interfaces publiques pour les autres modules metier.
- Les contrats publics ne dependent d'aucun package interne du module.
- Les classes internes ne sont pas rendues publiques uniquement pour contourner une frontiere Modulith.
- Les dependances inter-modules continuent d'etre validees par Spring Modulith.

### R08 - Placement des composants techniques
- Les classes annotees comme controleurs sont placees dans `..infrastructure.web..`.
- Les composants JPA et Spring Data sont places dans `..infrastructure.persistence..`.
- Les classes de configuration Spring sont placees dans `..config..`.
- Une classe metier ne doit pas etre placee directement a la racine `io.teampulse.<module>`.

### R09 - Absence de cycles internes
- Aucun cycle n'est autorise entre `domain`, `application`, `infrastructure` et `config`.
- Aucun cycle n'est autorise entre les sous-packages d'adapters.

### R10 - Regles partagees entre les modules
- Les memes regles sont appliquees a `identity`, `organization` et `team`.
- Une exception ne peut pas etre ajoutee silencieusement pour un seul module.
- Toute exception temporaire doit etre documentee, justifiee et associee a une date ou un ticket de suppression.

## Gestion des packages encore vides
- T03 doit definir les regles avant l'apparition des premieres classes concernees.
- Aucune classe de production factice ne doit etre creee uniquement pour faire passer un test ArchUnit.
- Une regle sans classe cible peut rester inactive temporairement, mais elle ne doit pas etre presentee comme une preuve d'architecture respectee.
- Des qu'une classe apparait dans une zone cible avec T04 ou T05, la regle correspondante doit l'analyser automatiquement.
- La validation du ticket doit inclure au moins une violation introduite temporairement puis retiree, afin de demontrer que le build echoue reellement.

## Contraintes d'implementation
- Declarer explicitement ArchUnit comme dependance de test ; ne pas s'appuyer sur sa presence transitive via Spring Modulith.
- Conserver ArchUnit hors du classpath de production.
- Placer les tests d'architecture dans `tp-app`, qui assemble les classes de tous les modules.
- Analyser uniquement le code de production sous le package racine de
  `TpAppApplication`, derive dynamiquement sans litteral propre a TeamPulse.
- Ne pas demarrer de contexte Spring, de base PostgreSQL ou de conteneur Docker pour ces tests.
- Executer les tests avec la suite Maven standard et avec `mvn verify`.
- Donner aux regles des noms et des messages d'erreur qui expliquent la contrainte violee.
- Centraliser les regles communes afin d'eviter trois copies divergentes pour les modules metier.

## Perimetre inclus
- Ajout de la dependance ArchUnit de test necessaire aux regles personnalisees.
- Conservation et clarification du test `ApplicationModules.verify()`.
- Tests des regles internes communes aux modules metier.
- Documentation de la structure cible et des dependances autorisees.
- Demonstration qu'une violation d'architecture fait echouer le build.

## Perimetre exclu
- Creation de classes metier, entites, repositories ou controleurs factices.
- Implementation du multi-tenancy et de `org_id`, traitee dans T04.
- Implementation de la premiere API fonctionnelle, traitee dans T05.
- Regles de formatage, Checkstyle, couverture de code ou analyse Sonar.
- Remplacement de Spring Modulith par ArchUnit.
- Test du comportement fonctionnel ou de la persistence.

## Livrables attendus
- Dependances de test ArchUnit explicites et maitrisees.
- Tests d'architecture executables depuis `tp-app`.
- Regles partagees pour `identity`, `organization` et `team`.
- Test Spring Modulith conserve pour les frontieres inter-modules.
- Documentation des responsabilites respectives de Maven, Spring Modulith et ArchUnit.

## Criteres d'acceptation
- [x] `mvn clean verify` execute les tests Spring Modulith et ArchUnit.
- [x] Les tests ArchUnit ne demarrent ni Spring Boot, ni PostgreSQL, ni Testcontainers.
- [x] Les regles R01 a R10 sont representees dans les tests ou explicitement marquees comme futures tant que leur package cible est vide.
- [x] Les regles communes sont appliquees aux trois modules metier sans duplication.
- [x] Une violation controlee de la direction des dependances fait echouer le build avec un message comprehensible.
- [x] La suppression de cette violation rend de nouveau le build vert.
- [x] Les classes de production existantes respectent les regles actives.
- [x] `ApplicationModules.verify()` reste la verification de reference pour les frontieres inter-modules.
- [x] Aucune dependance ArchUnit n'est ajoutee au runtime de production.
- [x] Aucune classe de production factice n'est ajoutee pour satisfaire les tests.

## Validation attendue
La validation doit etre realisee en trois temps :

1. Executer les tests ArchUnit seuls et constater qu'ils passent sans demarrer Spring
   Boot, PostgreSQL ou Testcontainers.
2. Introduire temporairement une dependance interdite representative, puis verifier
   que la regle ArchUnit correspondante fait echouer le build avec une explication
   exploitable.
3. Supprimer la violation, verifier le retour au vert des tests ArchUnit, puis
   executer `mvn clean verify` avec Docker disponible pour les autres tests du projet.

La classe volontairement non conforme ne doit pas etre conservee dans le code final.

Validation realisee le 22 juillet 2026 : une dependance temporaire de `domain` vers
`config` a fait echouer R01 en identifiant la classe et la dependance interdites.
Apres suppression de cette classe, les 29 tests d'architecture puis les 35 tests de
`mvn clean verify` sont revenus au vert.

## Notes pedagogiques
Ce ticket doit permettre de comprendre qu'une architecture n'est pas seulement un diagramme ou une convention d'equipe.
Une regle d'architecture utile exprime une intention, identifie les dependances interdites et fournit un retour rapide au developpeur qui la viole.

L'apprenant doit notamment savoir expliquer :
- pourquoi Spring Modulith et ArchUnit sont complementaires ;
- pourquoi la direction des dependances protege le domaine ;
- pourquoi les ports appartiennent au coeur applicatif et non aux adapters ;
- pourquoi un test d'architecture ne remplace pas un test fonctionnel ;
- pourquoi une suite verte sur des packages vides ne prouve pas encore que l'architecture est respectee.

## Liens avec les autres tickets
- Depend de : W001-T01 pour la structure multi-module et les frontieres Spring Modulith.
- Depend de : W001-T02 pour les premieres configurations techniques portees par les modules.
- Prepare : W001-T04, qui introduira la persistence multi-tenant avec `org_id`.
- Prepare : W001-T05, qui introduira la premiere verticale API, application, domaine et persistence.

## Decision associee
La decision est documentee dans
[`ADR-W001-T03-regles-architecture-archunit.md`](../../adr/W001/ADR-W001-T03-regles-architecture-archunit.md).
Elle est acceptee apres validation de tous les criteres du ticket.
