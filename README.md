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

Rien n'est appris "dans le vide" : chaque techno sert directement TeamPulse.
Chaque ticket produit un commit, un artefact d'architecture (ADR, runbook,
diagramme) et une trace de décision — y compris les erreurs et incohérences
corrigées en cours de route.

📊 Le support de présentation (Slidev) régénère ses slides depuis les besoins,
ADRs et commits du repo.

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

| Paramètre | Valeur par défaut |
|---|---|
| URL JDBC | `jdbc:postgresql://localhost:5432/teampulse` |
| Base | `teampulse` |
| Utilisateur | `teampulse` |
| Mot de passe | `teampulse` |

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
