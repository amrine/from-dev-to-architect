---
layout: default
routeAlias: evolution
---

## L'architecture évolue avec les compétences

```mermaid {scale: 0.75}
flowchart LR
  subgraph P1["Phase 1 · Monolithe modulaire"]
    M["Spring Boot 4 + Modulith<br/>PostgreSQL · Flyway<br/>Docker · GitLab CI<br/>AWS : VPC, EC2, ALB, RDS<br/>Terraform · CloudWatch"]
  end
  subgraph P2["Phase 2 · Microservices"]
    MS["identity · team · pulse<br/>analytics · notification · audit<br/>Kafka · outbox · saga · DLQ<br/>EKS · Helm · Prometheus/Grafana<br/>OpenTelemetry · SLO"]
  end
  subgraph P3["Phase 3 · Plateforme enterprise"]
    PF["GitOps ArgoCD · canary/blue-green<br/>multi-account · central logging<br/>DR (RTO/RPO) · SBOM<br/>policy-as-code · FinOps"]
  end
  M ==> MS ==> PF
```

<v-clicks>

- Le découpage en **modules Modulith** (Phase 1) préfigure les **bounded contexts** des futurs services : la migration est un déplacement de frontières déjà tracées, pas une réécriture.
- Les événements Spring in-process deviennent des **événements Kafka** — même sémantique métier, transport différent.

</v-clicks>

---

## Stratégie d'environnements : payer juste ce qu'il faut

| Environnement                                                | Quand                   | Quoi                                                  |
| ------------------------------------------------------------ | ----------------------- | ----------------------------------------------------- |
| <span class="tp-badge tp-badge--done">Local / Compose</span> | Développement quotidien | PostgreSQL, Kafka local, tests, services simples      |
| <span class="tp-badge tp-badge--doc">LocalStack</span>       | Intégration cloud-like  | S3, SQS/SNS, Lambda simple, secrets — sans facture    |
| <span class="tp-badge tp-badge--warn">AWS réel</span>        | Semaines clés           | VPC, IAM, RDS, ALB, EKS, KMS, coûts NAT/endpoints, DR |

<v-click>

<div class="tp-card tp-card--pulse mt-4">
Principe : ne pas payer inutilement, mais ne pas tout simuler — certains comportements AWS (IAM réel, coûts, réseau) ne s'apprennent <strong>que sur AWS</strong>. Budgets et alertes de coût dès la première facture.
</div>

</v-click>

---

## Politique de versions

Règle : **la dernière génération stable et supportée** de chaque techno, mise à jour à chaque nouvelle semaine `Wxxx`.

| Techno                                                   | Cible actuelle                   | Modèle de support                                                   |
| -------------------------------------------------------- | -------------------------------- | ------------------------------------------------------------------- |
| Java                                                     | **25** (LTS)                     | LTS tous les 2 ans — 21 reste supporté                              |
| Spring Boot                                              | **4.1.x**                        | Pas de LTS : 12 mois d'OSS par mineure, une mineure tous les 6 mois |
| Spring Modulith                                          | **2.x** aligné sur le BOM Boot 4 | Suit le train Spring                                                |
| Angular <span class="small muted">(front à venir)</span> | **22**                           | 6 mois actif + 12 mois LTS par majeure                              |
| PostgreSQL                                               | dernière majeure stable          | ~5 ans de support par majeure                                       |

<v-click>

<div class="tp-card tp-card--warn mt-3">
Attention au vocabulaire : <strong>Spring Boot n'a pas de LTS</strong> — rester sur une mineure supportée impose une montée de version régulière (3.5 est EOL depuis juin 2026). C'est un choix assumé : le programme vit 28 mois, les montées de version font partie de l'apprentissage DevOps.
</div>

</v-click>

---

## Les artefacts d'architecte, chaque semaine

<div class="tp-grid-2 mt-2" style="grid-template-columns: 1fr 1fr 1fr; gap: 0.7rem;">

<v-clicks>

<div class="tp-card">
<h3>Décider</h3>
<p class="small muted">ADR · diagrammes C4 · diagrammes réseau · threat models</p>
</div>

<div class="tp-card">
<h3>Exploiter</h3>
<p class="small muted">Runbooks · postmortems · SLO · checklists release & sécurité</p>
</div>

<div class="tp-card">
<h3>Prouver</h3>
<p class="small muted">Architecture reviews · portfolio · ce deck, régénéré semaine après semaine</p>
</div>

</v-clicks>

</div>

<v-click>

Rituel hebdomadaire : **1 commit** + **1 artefact** + **next steps** — le samedi matin est le cœur du progrès.

</v-click>
