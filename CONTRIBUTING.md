# Contributing to From Dev to Architect

Thank you for your interest in contributing to **From Dev to Architect**.

This repository documents the evolution of TeamPulse from a modular application
to a cloud, platform, and enterprise architecture. Contributions should preserve
both the technical consistency of TeamPulse and the educational value of the
decisions documented along the way.

Both first-time and experienced contributors are welcome.

By participating, you agree to follow our
[Code of Conduct](CODE_OF_CONDUCT.md).

## Ways to contribute

Beginner-friendly contributions may include:

- documentation corrections and improvements;
- localized automated tests;
- Slidev content and presentation improvements;
- small validation or developer-experience tools;
- translations requested by an existing issue.

Experienced contributors may also work on:

- application features;
- infrastructure and delivery automation;
- security and observability;
- architectural improvements;
- selected roadmap tickets.

Changes that affect the roadmap or architecture require prior discussion with
the maintainer.

## Language

- Issues and discussions are welcome in English or French.
- Code, technical identifiers, branch names, and commit messages must be in
  English.
- New project-wide documentation should be written in English unless an issue
  explicitly asks for another language.
- Existing French documentation does not need to be translated unless that is
  the purpose of the contribution.

## Before starting

1. Search the existing issues and pull requests to avoid duplicate work.
2. Select an open issue, preferably one labelled `good first issue` or
   `help wanted`.
3. Comment on the issue to ask whether it is available.
4. Wait for the maintainer to assign it or confirm that you can start.

A pull request may be opened directly for a small typo or an obvious
documentation correction.

For a feature, roadmap item, or architectural change, open or join an issue
before writing code. Describe the problem and the proposed approach so that the
scope can be agreed upon first.

## Requirements and ADRs

Every roadmap ticket identified as `W00X-TYY` requires:

- a requirement document under `docs/besoins/W00X`;
- a dedicated Architecture Decision Record under `docs/adr/W00X`;
- consistency between the requirement, ADR, implementation, tests, and
  learning material.

An ADR is also required for a contribution that introduces a significant or
long-lasting architectural decision.

Documentation changes, localized bug fixes, small tests, slides, and routine
maintenance do not normally require an ADR. When unsure, ask in the issue
before implementation.

## Architecture rules

Read [AGENTS.md](AGENTS.md) before changing application code. In particular:

- dependencies must point inward from infrastructure to application to domain;
- business modules must not depend on `tp-app`;
- `tp-common` must contain only genuinely shared, non-business concerns;
- domain and application code must not depend on infrastructure;
- module boundaries enforced by ArchUnit and Spring Modulith must remain valid.

Avoid introducing a new framework, infrastructure component, or architectural
pattern without prior discussion.

## Development environment

Backend prerequisites:

- Java 25;
- Docker with Docker Compose.

Slidev prerequisites:

- Node.js 22;
- Corepack with Yarn 4.9.4.

See the main [README](README.md) for backend setup and
[slidev/README.md](slidev/README.md) for presentation-specific guidance.

## Create a branch

External contributors normally work from a fork. Repository collaborators may
create a branch directly in this repository. In both cases, changes must go
through a pull request; do not push directly to `main`.

Use a short, descriptive branch name:

```text
docs/12-slidev-setup
test/15-architecture-rule
fix/21-organization-migration
```

Roadmap branches keep their ticket identifier:

```text
W001-T04-multi-tenancy-org-id
```

Keep one issue or one cohesive change per branch.

## Commit messages

Write commit messages in English and use a simple, descriptive prefix:

```text
docs: explain the Slidev setup
test: cover organization module boundaries
fix: correct the local Flyway configuration
feat: add organization tenant resolution
chore: update the Maven wrapper
```

Prefer small commits that leave the repository in a coherent state.

## Validate your changes

For backend changes, start Docker and run:

```bash
./mvnw --batch-mode --no-transfer-progress test
```

For Slidev changes, run:

```bash
cd slidev
corepack enable
corepack prepare yarn@4.9.4 --activate
yarn install --immutable
yarn build
```

For documentation-only changes:

- review spelling and formatting;
- verify every link and referenced path;
- ensure commands and version numbers remain accurate.

Run every relevant validation when a contribution affects more than one area.
The same backend and Slidev checks run automatically in GitHub Actions.

## Slidev generated content

Do not manually edit content inside markers such as:

```text
<!-- AUTO-GENERATED:...:START -->
<!-- AUTO-GENERATED:...:END -->
```

Unless an issue explicitly instructs otherwise, update the source requirement,
ADR, code, or generation process instead. Preserve route aliases, navigation,
presenter notes, and the existing visual conventions.

## Open a pull request

Before opening a pull request:

- rebase or update your branch from the latest `main`;
- keep the change focused on one issue;
- remove unrelated formatting or generated-file changes;
- run the relevant validations;
- review your own diff.

In the pull request:

- explain the problem and the chosen solution;
- link the issue with `Closes #123` when appropriate;
- describe how the change was validated;
- identify affected modules or documentation;
- mention any architectural impact and link the ADR when required;
- include screenshots for visible Slidev changes.

Draft pull requests are welcome when early feedback would help. A pull request
is ready for merge only when its required checks pass and the documentation,
tests, and implementation are consistent.

## Review

Review feedback is part of the contribution process. Keep discussions focused
on the code, documentation, architecture, and learning goals of the project.

The maintainer may ask to narrow a change, add tests or documentation, update an
ADR, or split unrelated work before merging.
