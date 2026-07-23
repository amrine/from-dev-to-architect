# AGENT.md

## But
Ce document decrit l'organisation du projet pour une architecture clean/hexagonale simple, sans DDD lourd. Il sert de reference pour tous les modules.

## Vue d'ensemble
- `tp-app` : module bootstrap (demarrage Spring, wiring, configuration globale).
- `tp-common` : module partage (types transverses, erreurs communes, utilitaires simples).
- `tp-identity`, `tp-organization`, `tp-team` : modules metier, chacun avec ses couches internes.

Regles de dependances (toujours vers l'interieur) :
- `infrastructure` -> `application` -> `domain`
- `tp-app` depend des modules metier
- Les modules metier ne dependent pas de `tp-app`
- `tp-common` est une dependance de tous les modules
- `tp-common` ne depend d'aucun module du projet

## Module partage (tp-common)
**But** : centraliser les types et outils transverses, sans logique metier.

**Exemples de contenu** :
- Identifiants (`EntityId`, `UuidFactory`)
- Erreurs communes (`ErrorCode`, `DomainException`)
- Temps/horloge (`Clock`)
- Petites fonctions utilitaires sans dependance Spring

**Structure type**
```
io.tpcommon
├─ error
├─ id
├─ time
├─ validation
└─ util
```

## Structure type d'un module metier
```
io.tpteam
├─ domain
│  ├─ model
│  └─ service
├─ application
│  ├─ port
│  │  ├─ in
│  │  └─ out
│  └─ service
└─ infrastructure
   ├─ web
   ├─ persistence
   ├─ messaging
   └─ config
```

## Couches, exemples et relations

### 1) domain/model (regles metier locales)
**Responsabilite** : modeles, invariants simples, valeur metier. Pas d'I/O.

**Exemple**
```java
package io.tpteam.domain.model;

public class Team {
    private final TeamId id;
    private final String name;

    public Team(TeamId id, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.id = id;
        this.name = name;
    }

    public TeamId id() { return id; }
    public String name() { return name; }
}
```

**Relations**
- Ne depend de rien d'autre.
- Utilise seulement Java standard.

### 2) application/port/in (cas d'usage exposes)
**Responsabilite** : interfaces d'entree pour l'application.

**Exemple**
```java
package io.tpteam.application.port.in;

import io.tpteam.domain.model.Team;

public interface CreateTeamUseCase {
    Team create(String name);
}
```

**Relations**
- Depend de `domain/model` uniquement.

### 3) application/port/out (sorties externes)
**Responsabilite** : contrats pour la persistence, messagerie, API externes.

**Exemple**
```java
package io.tpteam.application.port.out;

import io.tpteam.domain.model.Team;

public interface TeamRepository {
    Team save(Team team);
}
```

**Relations**
- Depend de `domain/model` uniquement.
- Pas d'implementation ici.

### 4) application/service (orchestration)
**Responsabilite** : implemente les ports `in`, orchestre la logique avec les ports `out`.

**Exemple**
```java
package io.tpteam.application.service;

import io.tpteam.application.port.in.CreateTeamUseCase;
import io.tpteam.application.port.out.TeamRepository;
import io.tpteam.domain.model.Team;

public class CreateTeamService implements CreateTeamUseCase {
    private final TeamRepository repository;

    public CreateTeamService(TeamRepository repository) {
        this.repository = repository;
    }

    @Override
    public Team create(String name) {
        Team team = new Team(null, name);
        return repository.save(team);
    }
}
```

**Relations**
- Depend de `application/port` et de `domain/model`.
- Ne depend pas de `infrastructure`.

### 5) infrastructure/web (adapters entrants)
**Responsabilite** : REST, DTO, mapping, validation d'entree.

**Exemple**
```java
package io.tpteam.infrastructure.web;

import io.tpteam.application.port.in.CreateTeamUseCase;
import io.tpteam.domain.model.Team;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/teams")
public class TeamController {
    private final CreateTeamUseCase createTeam;

    public TeamController(CreateTeamUseCase createTeam) {
        this.createTeam = createTeam;
    }

    @PostMapping
    public TeamResponse create(@RequestBody CreateTeamRequest request) {
        Team team = createTeam.create(request.name());
        return new TeamResponse(team.id(), team.name());
    }
}
```

**Relations**
- Depend de `application` via ports `in`.
- Depend de `domain/model` pour lecture.

### 6) infrastructure/persistence (adapters sortants)
**Responsabilite** : JPA, mapping DB, repos Spring Data.

**Exemple**
```java
package io.tpteam.infrastructure.persistence;

import io.tpteam.domain.model.Team;
import io.tpteam.application.port.out.TeamRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTeamRepository implements TeamRepository {
    private final SpringDataTeamRepository jpa;

    public JpaTeamRepository(SpringDataTeamRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Team save(Team team) {
        TeamEntity entity = TeamEntity.fromDomain(team);
        TeamEntity saved = jpa.save(entity);
        return saved.toDomain();
    }
}
```

**Relations**
- Depend de `application` via les ports `out` et de `domain/model`.
- Peut depend de Spring Data/JPA.

### 7) infrastructure/messaging (optionnel)
**Responsabilite** : publishers, listeners, integration events.

**Exemple**
```java
package io.tpteam.infrastructure.messaging;

import io.tpteam.application.port.out.TeamEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class KafkaTeamEventPublisher implements TeamEventPublisher {
    @Override
    public void publishTeamCreated(String teamId) {
        // push to Kafka
    }
}
```

### 8) bootstrap (tp-app)
**Responsabilite** : demarrage, wiring global, choix des adapters.

**Exemple**

```java
package io.teampulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TpAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(TpAppApplication.class, args);
    }
}
```

**Relations**
- Depend de tous les modules metier.
- Ne contient pas de logique metier.

## A faire / A ne pas faire

### A faire
- Garder la logique metier dans `domain` et `application`.
- Utiliser des ports (`in`/`out`) pour isoler l'infrastructure.
- Mapper les DTOs dans `infrastructure/web`.
- Garder `tp-app` comme point d'entree unique.
- Mettre dans `tp-common` seulement du partage transverse (pas de logique metier).

### A ne pas faire
- Ne pas appeler directement JPA dans `domain` ou `application`.
- Ne pas mettre de logique metier dans les controllers.
- Ne pas faire depend `domain` ou `application` de `infrastructure`.
- Ne pas dupliquer des regles metier dans plusieurs modules.
- Ne pas mettre d'adapters ou de logique metier specifique dans `tp-common`.
- Ne pas faire depend `tp-common` d'un autre module du projet.

## ADR obligatoire par ticket W00X

Pour chaque ticket de la roadmap identifie par un code de type `W00X-TYY`, un ADR dedie doit etre cree.

Un ticket ne doit pas etre considere comme termine tant que son ADR associe n'existe pas et n'est pas coherent avec l'implementation realisee.

### Objectif

Les ADR servent a tracer :

* le contexte du ticket ;
* la decision technique prise ;
* les alternatives envisagees ;
* les impacts sur l'architecture ;
* les consequences positives et negatives ;
* les fichiers ou modules impactes ;
* la maniere de valider la decision.

Meme lorsqu'un ticket semble simple, un ADR court doit etre produit afin de garder une tracabilite complete du projet.

### Emplacement des ADR

Les ADR doivent etre stockes par semaine/sprint :

```text
docs/adr/W001/
docs/adr/W002/
docs/adr/W003/
...
```

### Convention de nommage

Chaque ADR doit suivre ce format :

```text
ADR-W001-T01-nom-court-du-ticket.md
ADR-W001-T02-nom-court-du-ticket.md
ADR-W002-T01-nom-court-du-ticket.md
```

Exemples :

```text
docs/adr/W001/ADR-W001-T01-backend-multi-module.md
docs/adr/W001/ADR-W001-T02-docker-compose-local.md
docs/adr/W001/ADR-W001-T03-flyway-schema-initial.md
docs/adr/W001/ADR-W001-T04-multi-tenancy-org-id.md
```

### Regle d'execution agent

Avant de coder un ticket, l'agent doit :

1. identifier le code du ticket concerne ;
2. verifier si l'ADR du ticket existe deja ;
3. creer l'ADR s'il n'existe pas ;
4. lire les ADR precedents de la meme semaine si necessaire ;
5. implementer le ticket en respectant la decision documentee ;
6. mettre a jour l'ADR si l'implementation finale differe de la decision initiale.

### Statuts possibles

Chaque ADR doit contenir un statut :

```text
Draft
Accepted
Superseded
Rejected
```

* `Draft` : decision proposee avant ou pendant l'implementation.
* `Accepted` : decision validee et implementee.
* `Superseded` : decision remplacee par un ADR plus recent.
* `Rejected` : decision envisagee mais abandonnee.

### Template obligatoire d'un ADR

Chaque ADR doit suivre cette structure minimale :

```md
# ADR-W001-T01 - Titre court

## Statut
Draft | Accepted | Superseded | Rejected

## Ticket lie
W001-T01 - Nom du ticket

## Contexte
Decrire le probleme, le besoin ou la contrainte du ticket.

## Decision
Decrire clairement la decision prise.

## Alternatives envisagees
- Option A : ...
- Option B : ...
- Option C : ...

## Justification
Expliquer pourquoi cette decision est retenue.

## Consequences positives
- ...

## Consequences negatives / compromis
- ...

## Impact technique
Modules, packages, fichiers, tables, endpoints ou configurations impactes.

## Validation
Comment verifier que la decision est correctement appliquee.

## Notes
Informations complementaires si necessaire.
```

### Regle de coherence

Le code ne doit pas contredire les ADR.

Si une decision change pendant l'implementation :

* ne pas modifier silencieusement le code ;
* mettre a jour l'ADR concerne ;
* ou creer un nouvel ADR si la decision remplace une ancienne decision importante.

### Definition de termine

Un ticket W00X est termine uniquement si :

* le code est implemente ;
* les tests necessaires passent ;
* l'ADR du ticket existe ;
* l'ADR reflete reellement l'implementation ;
* la documentation de lancement ou d'usage est mise a jour si necessaire.

## Checklist rapide avant un commit
- Le code metier reste dans `domain`/`application`.
- Les adapters sont dans `infrastructure`.
- `tp-app` ne contient que bootstrap et configuration.
- `tp-common` ne depend de rien d'autre et reste framework-agnostic.
- L'ADR du ticket est present et coherent avec l'implementation.
