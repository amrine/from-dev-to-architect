# ADR-TECH-002 - Shared PostgreSQL with module-owned Flyway migrations

## Status

Accepted

## Linked ticket

None. This is a reusable technical architecture decision.

## Context

A modular monolith may need database-level boundaries between business modules
without paying the operational cost of one database instance or one connection
pool per module.

A single schema and a single Flyway history make every module coordinate its
migration numbering and release order. Separate databases provide stronger
isolation, but complicate local development, transactions, deployment, and
operations before independent services are justified.

This decision applies after a project has selected PostgreSQL, Flyway, JPA, and
a modular monolith deployed as one application. It does not decide whether a
specific product should use a relational database or whether its modules should
be independently deployed.

Developers also need an interactive PostgreSQL environment that is reproducible
without installing and administering the database directly on the host. That
environment must remain distinct from the ephemeral database lifecycle owned by
automated integration tests.

## Decision

### Persistence topology

- Use one PostgreSQL database and one application `DataSource` for the deployed
  modular monolith.
- Give each business module its own PostgreSQL schema.
- Make each module the exclusive owner of the tables and migrations in its
  schema.
- Do not let a module query or map another module's tables directly. Cross-module
  collaboration must use the module's public application contracts.
- Give each module its own Flyway configuration, migration location, default
  schema, and schema-local history table.
- Disable the single global Flyway auto-configuration when it would compete
  with the module-owned Flyway instances.
- Configure Flyway to create the owned schema before creating its history table.
- Do not add an empty or redundant migration whose only purpose is to execute
  `CREATE SCHEMA`.
- Let every module version its migrations independently. Migration versions do
  not need to be globally coordinated across modules.
- Use Flyway as the only mechanism that creates or evolves application tables.
- Configure Hibernate to validate the mapped schema, never to create or update
  it.
- Map every persistence entity to its module schema explicitly instead of using
  one global Hibernate default schema.
- Treat extraction to a separate database as a future architecture decision.
  Schema ownership reduces coupling but does not make extraction automatic.

### Local development environment

- Provide the interactive local PostgreSQL runtime with Docker Compose.
- Use an official PostgreSQL image pinned to the supported major version and a
  deliberate distribution tag. Do not use a floating tag such as `latest`.
- Persist interactive developer data in a named Docker volume rather than a
  repository bind mount.
- Define a PostgreSQL-native healthcheck so dependent local services can wait
  for database readiness instead of relying only on container startup.
- Publish a host port and local credentials through project configuration, and
  allow the application connection to be overridden through external
  configuration.
- Activate local application wiring explicitly through a dedicated environment
  or profile. Do not make local database settings the implicit production
  default.
- Keep the Compose file as the executable source of truth. Documentation and
  ADRs describe the contract and commands but do not duplicate the complete
  Compose definition.
- Keep automated integration tests independent from the local Compose service,
  its fixed host port, its volume, and its mutable developer data.

## Project adoption contract

A project adopting this ADR must record a project-specific ADR containing:

- the PostgreSQL version and deployment topology;
- the pinned local image, published port, database, user, and named volume;
- the local profile or environment and connection-override variables;
- the Compose file location and lifecycle/reset commands;
- the modules that own database schemas;
- the schema name, Flyway configuration, migration location, and history table
  for every adopting module;
- the application module that owns the shared `DataSource` configuration;
- the Hibernate schema-validation configuration;
- the policy preventing direct cross-schema access;
- the local and automated validation commands;
- any deviation from this technical decision.

The technical ADR can be reused across projects, but its `Accepted` status does
not automatically accept it for another project. Each project must evaluate its
context and explicitly adopt it through its own ADR.

## Alternatives considered

- Use one shared schema and one global Flyway history.
- Use one shared schema with multiple migration locations.
- Use one database or one `DataSource` per module.
- Let Hibernate create or update the database schema.
- Use Liquibase instead of Flyway.
- Create module schemas through dedicated schema-only SQL migrations.
- Install and administer PostgreSQL directly on every developer workstation.
- Use a floating PostgreSQL image tag.
- Use the integration-test container as the interactive development database.
- Persist PostgreSQL files through a repository bind mount.

## Justification

Schema-level ownership gives each module an explicit persistence boundary while
preserving the operational simplicity of one PostgreSQL database and one
connection pool. Module-owned Flyway instances keep migration histories and
version sequences independent, which avoids release-time coordination between
unrelated modules.

Explicit JPA schema mappings make ownership visible in code and prevent an
entity from silently falling back to the PostgreSQL `public` schema. Flyway
remains the source of truth for DDL, while Hibernate validation detects drift
between mappings and the migrated database at application startup.

Docker Compose makes the interactive local database reproducible while a named
volume preserves useful developer data without adding database files to the
repository. Keeping that runtime separate from automated tests prevents local
state and fixed ports from influencing test results.

## Positive consequences

- Database ownership follows the modular architecture.
- Modules can evolve migration versions independently.
- One database and one connection pool remain simple to operate.
- A transaction can still span in-process module collaboration when required.
- Schema drift is detected during application startup.
- The persistence boundary needed for a future extraction is visible.
- Developers share one reproducible PostgreSQL startup contract.
- Local data can persist across restarts without becoming repository content.
- Automated tests remain isolated from the interactive local database.

## Negative consequences / trade-offs

- Several Flyway configurations must be maintained.
- Every entity mapping must declare the correct schema.
- PostgreSQL still provides no runtime isolation between modules sharing the
  same database role.
- Database-wide operations, backups, and resource contention remain shared.
- Cross-module foreign keys and SQL joins are intentionally constrained even
  though PostgreSQL can technically support them.
- Extracting a module still requires data migration and an explicit distributed
  consistency strategy.
- Docker and Docker Compose are required for the supported local workflow.
- A persistent local volume must be removed explicitly when a clean database is
  required.

## Technical impact

- Application `DataSource` and Flyway auto-configuration.
- One Flyway initializer and migration resource location per business module.
- PostgreSQL schemas and schema-local Flyway history tables.
- Explicit schema declarations in JPA entity mappings.
- Hibernate schema validation at application startup.
- Architecture rules or reviews preventing direct cross-schema persistence
  access.
- Docker Compose definition, named volume, healthcheck, local profile, and
  externalized connection settings.

## Validation

- Start the complete application against an empty PostgreSQL database.
- Verify that every module-owned migration executes successfully.
- Verify that Hibernate schema validation succeeds after the migrations.
- Run each module's persistence tests against PostgreSQL.
- Verify through architecture tests or dependency review that a module does not
  use another module's persistence implementation.
- Add SQL assertions only for application-owned database contracts. Do not test
  Flyway's internal metadata or reproduce its own migration checks.
- Validate the Compose configuration and verify that PostgreSQL becomes healthy.
- Start the application through the local profile against the Compose database.
- Run persistence integration tests with the Compose service stopped and verify
  that they use their own ephemeral PostgreSQL lifecycle.

## Notes

This ADR defines a reusable persistence topology and interactive local
PostgreSQL strategy for a modular monolith. Schema names, module names, concrete
database credentials, ports, volume names, environment-variable names, Docker
configuration, migration file names, and extraction plans belong to the
adopting project's ADRs.
