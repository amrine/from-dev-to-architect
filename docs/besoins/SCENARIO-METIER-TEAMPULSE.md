# Scenario metier TeamPulse

## Statut
Draft

## Finalite du produit
TeamPulse est une application temoin pour apprendre a raisonner sur l'architecture
logicielle, l'infrastructure et l'exploitation d'un produit. Le parcours reste
principalement technique, mais il s'appuie sur une application suffisamment
complete pour que chaque decision d'architecture reponde a un comportement
metier observable.

TeamPulse transforme un signal collectif en action collective verifiable :

```text
administrer
-> lancer une campagne
-> repondre
-> agreger
-> publier
-> proposer
-> voter
-> agir
-> mesurer a nouveau
```

## Frontiere des organisations

- Un compte est soit un administrateur de la plateforme, soit un utilisateur
  d'une organisation. Il ne peut jamais etre les deux.
- Un administrateur plateforme n'appartient a aucune organisation et ne possede
  aucun acces aux donnees tenant.
- Un utilisateur metier appartient a une et une seule organisation.
- Une equipe appartient a une seule organisation.
- Un utilisateur peut appartenir a plusieurs equipes, mais uniquement dans son
  organisation.

L'administrateur plateforme gere le contenant technique. Il peut creer,
suspendre ou archiver une organisation et designer son premier administrateur,
mais il ne peut pas consulter les equipes, les campagnes, les reponses, les
resultats ou les actions de cette organisation.

## Roles

Les roles sont cumulatifs et contextualises. Ils ne forment pas une hierarchie
donnant automatiquement acces aux donnees sensibles.

| Role | Perimetre | Capacites principales |
| --- | --- | --- |
| Administrateur plateforme | Plateforme | Creer et administrer le cycle de vie d'une organisation, designer le premier administrateur, consulter l'etat technique |
| Administrateur d'organisation | Une organisation | Gerer les utilisateurs, les equipes, les affectations et les roles |
| Manager d'organisation | Une organisation | Consulter les tendances consolidees et le suivi global des actions |
| Administrateur d'equipe | Une equipe dont il est membre | Gerer les membres et superviser le cycle des campagnes et des votes |
| Manager d'equipe | Une equipe dont il est membre | Consulter les resultats, proposer et suivre les actions |
| Membre | Une ou plusieurs equipes de son organisation | Repondre, consulter les resultats publies, suggerer et voter |
| Auditeur d'organisation | Une organisation | Consulter les traces administratives et de securite sans lire les contributions sensibles |

Un administrateur d'organisation ne lit pas automatiquement les resultats d'une
equipe. Il doit aussi etre membre ou manager dans le perimetre concerne. Un
administrateur ou manager d'equipe est toujours membre de cette equipe.

## Administration initiale

1. L'administrateur plateforme cree une organisation.
2. Il designe son premier administrateur d'organisation.
3. L'administrateur d'organisation invite les utilisateurs et cree les equipes.
4. Il designe les managers d'organisation, administrateurs d'equipe et managers
   d'equipe.
5. L'administrateur d'organisation ou l'administrateur d'equipe ajoute les
   utilisateurs aux equipes autorisees.

## Campagne hebdomadaire

TeamPulse ouvre une campagne hebdomadaire pour chaque equipe. Un membre peut
repondre une seule fois par equipe et par campagne. Il renseigne au minimum son
humeur, sa charge, ses blocages, ses risques et, lorsque la politique de
l'organisation l'autorise, un commentaire libre.

La reponse reste modifiable jusqu'a la cloture. Le systeme calcule ensuite les
resultats. Un administrateur peut prolonger ou annuler exceptionnellement une
campagne, mais il ne peut ni modifier une reponse ni corriger manuellement un
resultat calcule.

## Resultats et confidentialite

Les membres de l'equipe et son manager consultent les resultats publies :

- scores par dimension ;
- tendances historiques ;
- taux de participation ;
- alertes et changements inhabituels ;
- themes des blocages ;
- commentaires anonymises ;
- actions passees et evolution observee depuis leur mise en oeuvre.

Les aggregats sont masques lorsque le seuil minimal de confidentialite n'est pas
atteint. Le manager d'organisation consulte seulement des tendances consolidees
et ne lit pas les reponses individuelles ni les votes individuels.

## Actions et votes

Apres la publication, le manager d'equipe peut creer une proposition d'action
reliee a un resultat ou une alerte. Une proposition decrit le probleme, le
resultat attendu, l'effort estime, le responsable propose et l'echeance.

Les membres peuvent suggerer des idees et voter sur les propositions formelles.
Les votes sont secrets, modifiables jusqu'a la cloture et calcules par le
systeme. L'administrateur d'equipe supervise le calendrier et le quorum, mais ne
peut modifier aucun bulletin ni le resultat du scrutin.

Une proposition acceptee devient une action dont le cycle est :

```text
proposee -> soumise au vote -> acceptee -> en cours -> terminee -> evaluee
```

La campagne suivante permet d'evaluer si l'action a produit l'effet attendu.

## Retrait d'un membre

Une appartenance a une equipe n'est jamais supprimee physiquement. Elle possede
une date de debut, une date de fin et un statut.

- Un retrait normal peut prendre effet a la fin du cycle en cours.
- Un retrait urgent revoque immediatement les acces et exige un motif audite.
- Les reponses, votes et traces valides produits avant le retrait sont conserves.
- Un vote deja emis reste comptabilise ; un resultat publie n'est jamais
  recalcule a cause d'un retrait ulterieur.
- Un ancien membre ne peut plus consulter les resultats, commentaires ou actions
  de l'equipe.
- Une reintegration cree une nouvelle periode d'appartenance et ne permet pas de
  voter deux fois dans un scrutin existant.
- Si le membre etait responsable d'une action, le manager doit reaffecter cette
  action.

Les participants eligibles sont figes a l'ouverture d'une campagne ou d'un
scrutin. Une erreur compromettant le scrutin conduit a annuler et recreer le
scrutin avec une justification auditee, jamais a supprimer un bulletin isole.

## Invariants metier

- Un utilisateur metier appartient exactement a une organisation.
- Un administrateur plateforme n'appartient a aucune organisation.
- Une equipe et tous ses membres appartiennent a la meme organisation.
- Les roles d'equipe sont independants d'une equipe a l'autre.
- Une reponse est unique par membre, equipe et campagne.
- Un vote est unique par membre et scrutin.
- Une contribution historique valide est immutable.
- Les resultats confidentiels ne sont publies que lorsque le seuil requis est
  atteint.
- Toute intervention administrative exceptionnelle est auditee.

## Progression dans la roadmap

| Periode | Capacite metier | Probleme d'architecture rendu visible |
| --- | --- | --- |
| W001 | Organisations, frontiere des comptes, roles et appartenance temporelle | Modules, persistence, multi-tenancy et direction des dependances |
| W002-W004 | Administration des utilisateurs et equipes, premiere interface | API, mapping, persistence et frontend |
| W005-W008 | Authentification, permissions contextualisees et audit | JWT, autorisation, tenant context et tests de securite |
| W017-W019 | Campagnes, reponses et confidentialite | Workflow, anonymisation et privacy-by-design |
| W020-W026 | Resultats, actions et votes | Aggregation, concurrence, idempotence et premiere release complete |
| W027-W046 | Exploitation du parcours complet | Kubernetes, metriques, alertes et runbooks |
| W047-W057 | Extraction de services et evenements metier | Ownership, Kafka, outbox et coherence distribuee |
| W058-W065 | Notifications, webhooks et resilience | Retry, circuit breaker et livraison garantie |
| W066-W122 | Industrialisation et analytics | GitOps, securite, performance, archivage et reprise |

## Scene d'acceptation de la Phase 1

La release `v0.1` est demonstrable lorsqu'un administrateur plateforme cree une
organisation, que l'administrateur d'organisation constitue une equipe, que les
membres repondent a une campagne, que le manager propose une action apres les
resultats, que les membres votent et que le retrait ulterieur d'un membre
revoque ses acces sans modifier son vote historique.
