# ADR-W001-T05 - Dockerfile multi-stage

## Statut
Draft

## Ticket lie
W001-T05 - Dockerfile multi-stage

## Contexte
L'application doit etre construite et demarree en conteneur avec une image legere et securisee.

## Decision
- Dockerfile multi-stage:
  - build: maven:3.9-eclipse-temurin-21, mvn -pl tp-app -am package -DskipTests
  - runtime: eclipse-temurin:21-jre-alpine
- Executer avec un user non-root (uid 1000).
- Copier le jar depuis le build et lancer java -jar /app.jar.

## Alternatives envisagees
- Image unique avec Maven et JDK.
- Distroless Java.
- Lancer en root.

## Justification
Multi-stage reduit la taille et garde l'image runtime simple.

## Consequences positives
- Image plus legere.
- Meilleure posture de securite (non-root).

## Consequences negatives / compromis
- Build un peu plus long a cause de l'etape Maven.

## Impact technique
- Dockerfile a la racine.

## Validation
- docker build . puis docker run demarre l'API.

