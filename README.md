# from-dev-to-architect

> D'un développeur Java/Spring Boot & Angular vers un Architecte Cloud/DevOps —
> documenté semaine après semaine, décision après décision.

Ce dépôt est le journal de bord public d'un programme de montée en compétences
sur 28 mois (122 semaines), organisé autour d'un unique projet fil rouge :
**TeamPulse**, une application B2B de suivi du pulse hebdomadaire d'équipe.

Chaque semaine (`Wxxx`) part d'une problématique fonctionnelle et technique,
se découpe en tickets (`Txx`) portés par un ADR, et avance en trois phases :

1. **Foundations** — Spring Boot 4.1 · Angular 22 · Java 25 · Docker · Terraform · AWS · CI/CD
2. **Plateforme moderne** — Kubernetes/EKS · Kafka · Observabilité · Microservices
3. **Enterprise** — GitOps · progressive delivery · multi-account · sécurité, DR, FinOps

Le découpage complet, l'état d'avancement et les jalons sont publiés dans la
[roadmap publique](ROADMAP.md).

Rien n'est appris "dans le vide" : chaque techno sert directement TeamPulse.
Chaque ticket produit un commit, un artefact d'architecture (ADR, runbook,
diagramme) et une trace de décision — y compris les erreurs et incohérences
corrigées en cours de route.

📊 Le support de présentation (Slidev) régénère ses slides depuis les besoins,
ADRs et commits du repo.

🌐 [Parcourir la présentation interactive TeamPulse](https://amrine.github.io/from-dev-to-architect/).

## Environnement local

### Prérequis

- Java 25
- Docker avec Docker Compose

### Démarrer PostgreSQL

Depuis la racine du projet :

```bash
docker compose -f docker/docker-compose.yaml up -d
```

Vérifier que PostgreSQL est prêt :

```bash
docker compose -f docker/docker-compose.yaml ps
```

Le service doit apparaître avec l'état `healthy`.

La configuration locale utilise les valeurs suivantes :

| Paramètre    | Valeur par défaut                            |
| ------------ | -------------------------------------------- |
| URL JDBC     | `jdbc:postgresql://localhost:5432/teampulse` |
| Base         | `teampulse`                                  |
| Utilisateur  | `teampulse`                                  |
| Mot de passe | `teampulse`                                  |

Ces valeurs peuvent être remplacées avec les variables
`TEAM_PULSE_DB_URL`, `TEAM_PULSE_DB_USERNAME` et
`TEAM_PULSE_DB_PASSWORD`.

### Démarrer l'application

Le profil Spring `local` doit être activé explicitement :

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw -pl tp-app spring-boot:run
```

Dans une configuration IntelliJ, ajouter la variable d'environnement :

```text
SPRING_PROFILES_ACTIVE=local
```

Au démarrage, chaque module initialise son propre schéma PostgreSQL et sa
propre table `flyway_schema_history`.

### Modules du backend

`tp-identity`, `tp-organization` et `tp-team` détiennent leurs API métier.
`tp-app` assemble les modules runtime et configure le démarrage. `tp-administration`
orchestre les contrats Java publics pour le provisioning initial et les contrôles
de responsabilités ; il ne possède pas les données des autres modules.
`tp-common` porte les types Java partagés, `tp-web-support` fournit l'advice
Spring des erreurs de transport, et `tp-test-support` reste réservé au scope
Maven `test`.

### Utiliser les API T05 en local

Les routes HTTP sont activées seulement avec les profils Spring `local` ou
`development`, sans authentification, sur `127.0.0.1:8080` par défaut. Ne les
exposez pas à un réseau non fiable. Les contrats sont publiés par leurs
propriétaires : [identité](tp-identity/src/main/openapi/openapi.yaml),
[organisation](tp-organization/src/main/openapi/openapi.yaml),
[équipe](tp-team/src/main/openapi/openapi.yaml) et
[administration](tp-administration/src/main/openapi/openapi.yaml).

| API | Parcours utilisables en local |
| --- | --- |
| Identité | `POST /api/users`, `POST /api/users/invitations` et `GET /api/users` utilisent le tenant local résolu par le serveur. Les comptes créés restent `CREATING` et les invitations `INVITED`. |
| Organisation | `POST /api/organizations` crée une organisation `CREATING`. Les routes d'affectation suivent T04 : un candidat `PENDING` peut être affecté tant que l'opération ne déclenche pas de transition. |
| Équipe | Les routes couvrent équipes, responsables et appartenances. Les commandes qui exigent une organisation `AVAILABLE`, dont la création d'équipe, restent refusées avec le tenant local `CREATING` jusqu'à W008. |
| Administration | `POST /api/platform/organizations` crée une organisation et son premier utilisateur ; `/api/platform/organizations/invitations` crée l'organisation et invite son premier utilisateur. Suspension et désactivation vérifient d'abord les responsabilités actives. |

Exemple de création initiale :

```bash
curl --fail-with-body -sS -X POST http://127.0.0.1:8080/api/platform/organizations \
  -H 'Content-Type: application/json' \
  -d '{"organizationName":"Demo API","timezone":"Europe/Paris","email":"admin@example.test","firstName":"Ada","lastName":"Lovelace"}'
```

Le provisioning initial est atomique : si la création ou l'invitation du premier
utilisateur échoue, l'organisation n'est pas persistée non plus. Le provider
`local` crée paresseusement une organisation de démonstration persistée en
`CREATING` au premier appel tenanté de chaque processus et retente après un
échec. Un redémarrage crée une nouvelle organisation ; les lignes des processus
précédents restent dans PostgreSQL. Le client ne choisit jamais le tenant dans
le body ou les headers.

Ces parcours restent bloqués jusqu'à W008 : acceptation des invitations et
activation des comptes, authentification/JWT et permissions, activation ou
réactivation des organisations, ainsi que toute affectation qui ferait passer
l'organisation à `ACTIVE`. Les équipes et appartenances qui exigent une
organisation `AVAILABLE` ne peuvent pas être créées dans le tenant local T05.

### Arrêter l'environnement

Les données sont conservées dans un volume Docker nommé :

```bash
docker compose -f docker/docker-compose.yaml down
```

Pour supprimer également les données locales et repartir d'une base vide :

```bash
docker compose -f docker/docker-compose.yaml down -v
```

La commande avec `-v` supprime définitivement le volume PostgreSQL local.

## Tests d'intégration

Les tests d'intégration utilisent Testcontainers. Ils ne se connectent pas au
PostgreSQL lancé par Docker Compose, mais démarrent une base PostgreSQL
éphémère sur un port aléatoire.

Le module `tp-test-support`, consommé uniquement avec le scope Maven `test`,
centralise le conteneur PostgreSQL et les configurations de test réutilisables.
Il ne fait pas partie du runtime ni du modèle applicatif Spring Modulith.

Docker doit être démarré, puis la suite complète peut être lancée avec :

```bash
./mvnw -pl tp-app -am test
```

Pour lancer le test d'intégration d'un seul module :

```bash
./mvnw -pl tp-app -am \
  -Dtest=IdentityModuleTests \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Remplacer `IdentityModuleTests` par `OrganizationModuleTests` ou
`TeamModuleTests` pour cibler un autre module. Chaque test charge uniquement
la configuration Flyway et le schéma du module concerné.

## Contributing

Contributions from both first-time and experienced contributors are welcome.
Issues and discussions may be written in English or French.

Read [CONTRIBUTING.md](CONTRIBUTING.md) before starting. Beginner-friendly
tasks will be labelled
[`good first issue`](https://github.com/amrine/from-dev-to-architect/labels/good%20first%20issue).

Product versioning and release tags are documented in
[VERSIONING.md](VERSIONING.md).

## Security

Do not report suspected vulnerabilities through a public issue or pull request.
Follow the private reporting instructions in [SECURITY.md](SECURITY.md).

## License

This project is licensed under the Apache License 2.0.
See the [LICENSE](LICENSE) file for details.
