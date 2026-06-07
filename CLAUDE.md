# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend (Maven multi-module)

```bash
# Build and run all architecture tests (mandatory before every commit)
./mvnw clean verify

# Run a single test class
./mvnw -pl tp-app test -Dtest=ModulithArchitectureTests

# Run module bootstrap tests only
./mvnw -pl tp-app test -Dtest=OrganizationModuleTests,IdentityModuleTests,TeamModuleTests

# Run the application
./mvnw -pl tp-app spring-boot:run
```

### Slidev training deck

```bash
cd slidev
yarn dev       # start dev server with live reload
yarn build     # production build
yarn export    # static export
yarn export:pdf
```

## Architecture

### Backend — modular monolith with hexagonal internals

The backend is a **Maven multi-module project** (`packaging=pom` at root) where all version management lives in the root `pom.xml`. There is exactly one executable: `tp-app`. The other modules are libraries.

**Module map:**

| Module | Role |
|---|---|
| `tp-app` | Bootstrap only — `@SpringBootApplication`, `@Modulithic`, wiring. No business logic. `spring-boot-maven-plugin` lives here only. |
| `tp-common` | Transverse utilities (ids, errors, clock). Framework-agnostic. Must not depend on any other `tp-*` module. |
| `tp-organization` | Business module — organisations |
| `tp-identity` | Business module — identities / users |
| `tp-team` | Business module — teams |

**Spring Modulith boundary enforcement:**
- `detection-strategy: explicitly-annotated` — only packages with `@ApplicationModule` are modules.
- Each business module's root `package-info.java` carries `@ApplicationModule(allowedDependencies = { "common" })` — business modules can only depend on `common`, never on each other directly.
- Public surfaces are declared with `@NamedInterface`: `api` (Java contracts) and `events` (Spring events). Everything else is internal by default.
- `ModulithArchitectureTests.verifiesArchitecture()` runs `ApplicationModules.verify()` on every `mvn verify`.

**Hexagonal layout inside each business module** (see `AGENT.md` for full detail and examples):
```
io.teampulse.<module>
├── domain/model          # pure business rules, no I/O
├── domain/port/in        # use-case interfaces
├── domain/port/out       # repository / event publisher interfaces
├── application/usecase   # implements port/in, calls port/out
└── infrastructure/
    ├── web               # REST controllers, DTOs, mapping
    ├── persistence       # JPA adapters implementing port/out
    ├── messaging         # event publishers / listeners (optional)
    └── config            # module-local Spring config
```

Dependency direction: `infrastructure → application → domain`. Nothing inside `domain` or `application` may import Spring Data, JPA, or any adapter concern.

### ADR workflow

Every roadmap ticket `W00X-TYY` requires a dedicated ADR before the ticket is considered done.

- Location: `docs/adr/W00X/ADR-W00X-TYY-short-name.md`
- Besoin files: `docs/besoins/W00X/W00X-TYY-short-name.md`
- Statuses: `Draft` → `Accepted` | `Superseded` | `Rejected`
- ADR template and full rules are in `AGENT.md` under "ADR obligatoire par ticket W00X".

Before implementing any ticket: check for the ADR, create it if absent, implement, then update the ADR to `Accepted`.

### Slidev training deck

The deck lives in `slidev/` and is the progressive training resource for the project. It is generated and maintained via the `.codex/skills/generate-teampulse-slidev/` skill.

Key conventions:
- Entry point: `slidev/slides.md` (frontmatter, cover, `src:` imports of week pages).
- Week pages: `slidev/pages/weeks/W001.md`, etc.
- Generated zones are bounded by `<!-- AUTO-GENERATED:W001:START -->` / `<!-- AUTO-GENERATED:W001:END -->` markers. Manual content outside these markers must never be overwritten.
- The design system is fully defined in `slidev/styles/index.css` (tokens and utility classes) and `slidev/components/PulseLine.vue`. All slides must use only these tokens — no inline colors or new CSS classes.
- Full style reference and slide-type patterns: `.codex/skills/generate-teampulse-slidev/references/slidev-requirements.md`.

### Key constraints

- `tp-common` must remain framework-agnostic and must not depend on any `tp-*` module.
- `tp-app` must contain zero business logic.
- OpenAPI / REST is for external clients only — never for inter-module calls.
- Inter-module communication inside the process uses Java interfaces (`api` named interface) or Spring events (`events` named interface).