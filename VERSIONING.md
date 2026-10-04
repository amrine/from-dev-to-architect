# TeamPulse versioning

This repository uses two complementary tag families. They answer different
questions and must not be mixed:

- ticket tags identify the exact roadmap increment that was delivered;
- release tags identify a coherent product increment.

## Version scheme

Product releases follow [Semantic Versioning 2.0.0](https://semver.org/):

```text
MAJOR.MINOR.PATCH
```

During development, Maven uses the next release followed by `-SNAPSHOT`.
The current development version is therefore:

```text
0.1.0-SNAPSHOT
```

All Maven modules in the reactor share the same product version. A module is
not released independently.

Before `1.0.0`:

- a `MINOR` release adds a significant, demonstrable product capability;
- a `PATCH` release fixes a released increment without adding a new capability;
- compatibility expectations remain pre-1.0 and any relevant breaking change
  must be documented.

After `1.0.0`, the normal SemVer compatibility rules apply:

- `MAJOR` may break the public contract;
- `MINOR` adds backward-compatible functionality;
- `PATCH` contains backward-compatible fixes.

## Git tags

### Ticket traceability tags

Each completed roadmap ticket may receive one descriptive tag, for example:

```text
W001-T04-multi-tenancy-organization-reference
```

These tags are delivery evidence. They do not represent a product release and
must not be used as Maven or container versions.

Existing `W001-*` tags remain unchanged.

### Product release tags

Product releases use an annotated tag with a `v` prefix:

```text
v0.1.0
v0.1.1
v0.2.0
v1.0.0
```

The `v0.1.0` tag is created only after all W001 tickets are complete, validated,
and merged into `main`. It must point to the merge or closure commit for the
whole W001 increment, not to the commit of an isolated ticket.

Release tags are immutable. A correction requires a new patch version rather
than moving an existing tag.

## Release workflow

1. Develop W001 tickets with the Maven version `0.1.0-SNAPSHOT`.
2. Add a descriptive `W001-TXX-*` tag when an individual ticket is delivered.
3. Merge the complete W001 increment into `main` after validation.
4. Create the annotated release tag `v0.1.0` on that closure commit.
5. Change the development version to `0.2.0-SNAPSHOT` for the next increment.

Release tags are created manually after review. Commit messages do not drive
automatic version calculation.

## Other version namespaces

Flyway migration versions are independent from the product version. For
example, `V001__create_users.sql` identifies a database migration within its
module and is not a release tag.
