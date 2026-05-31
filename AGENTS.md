# AGENTS.md — privateappstore

Guidance for AI agents (and humans) working in this repo. Read this before editing.

## What this is

`privateappstore` is the **Jahia 8.2 Java/OSGi module** that powers the Private
App Store ("Forge"): the JCR content model, the GraphQL admin API, the Jahia
Actions for authoring, background jobs, the Maven proxy, and the
`moduleList.json` feed. The **presentation/authoring UI** for it lives in the
sibling **`store-template`** JavaScript module — this repo is the backend and the
contract.

- Maven coordinates: `org.jahia.community:privateappstore` (parent
  `org.jahia.modules:jahia-modules`). Packaging: OSGi bundle (JAR).
- Java source level is **below 16** — no `instanceof` pattern matching, no
  records in this module. Use classic `instanceof` + cast.

## Component model (how things get registered)

Two registration mechanisms coexist; know which applies before adding a class:

1. **OSGi Declarative Services** (`@Component`) — used by GraphQL extensions and
   **Actions**. Which packages get DS descriptors generated is controlled by the
   bnd **`_dsannotations`** instruction in `pom.xml`.
   - ⚠️ **CRITICAL GOTCHA**: `_dsannotations` must include *both*
     `org.jahia.modules.forge.graphql.*` *and* `org.jahia.modules.forge.actions.*`.
     A past commit narrowed it to graphql-only, which silently **un-registered
     every `@Component(service=Action.class)`** (including the kept JAR-upload
     action). Tests that only assert "the form exists" do not catch this — verify
     the generated `OSGI-INF/*.xml` descriptors after touching the pom.
2. **Blueprint extender** — used by `filters`, `job`, and `proxy`. These are NOT
   in `_dsannotations` on purpose.

## Actions vs GraphQL (the permission wall)

The Jahia `/modules/graphql` endpoint is **permission-gated**: ordinary
authenticated users are denied (`GqlAccessDeniedException`). This shapes the API:

- **Admin/owner operations** (Forge settings, categories, roles, owner metadata
  edits) go through GraphQL extensions in `…/graphql/` (require a privileged role,
  or rely on JCR ACLs for generic mutations).
- **Any authenticated user operations** must be **Jahia Actions**
  (`org.jahia.bin.Action`, `@Component(service=Action.class)`), invoked through
  the render servlet at `…/<node>.<ActionName>.do`.
  - The flagship example is **`SubmitReview`** (`actions/SubmitReview.java`): any
    user can review any module they don't own. It writes under
    `JCRTemplate.doExecuteWithSystemSessionAsUser(user, …)` — a system session
    *impersonating the caller*, so it bypasses the module ACL while keeping
    `jcr:createdBy` correct. One review per user is enforced **atomically** via a
    deterministic node name (`review-<escaped user>` → `ItemExistsException`→409),
    and the aggregate rating is recomputed from the review children (self-healing).
  - **CSRF**: Action `.do` POSTs are protected by OWASP CSRFGuard, which patches
    **XMLHttpRequest only**. The `store-template` client posts reviews via XHR; a
    `fetch` POST would be rejected "Required Token is missing".

## JCR content model (essentials)

- `jnt:forgeModule` / `jnt:forgePackage` extend `jnt:post`, are `jmix:forgeElement`,
  and mix in `jmix:reviews` (autocreates a `reviews` child) and `jmix:rating`
  (`j:sumOfVotes` / `j:nbOfVotes`).
- `jnt:review` extends `jnt:post` with a mandatory `rating` (long); `jnt:post`
  carries `jcr:title` + `content`.
- No same-name siblings — `addNode` with an existing name throws
  `ItemExistsException` (used for atomic per-user dedup).

## Directory map

```
src/main/java/org/jahia/modules/forge/
  actions/     Jahia Actions (SubmitReview, CreateEntryFromJar, AddVideo, …)
  graphql/     GraphQL extensions (ForgeSettings, CategorySettings, ManageRoles)
  filters/     render filters (blueprint extender)
  job/         scheduled jobs (blueprint extender)
  proxy/       Maven proxy (blueprint extender)
  tags/        JSP tag/function helpers
src/main/resources/   CND, views, i18n, moduleList rendering
tests/                Cypress E2E (+ env tooling) — see below
```

## Build / test

- **Build**: `mvn clean install` (Java 11). Produces the OSGi bundle JAR.
- **E2E**: `tests/` uses `@jahia/cypress`. `env.run.sh` spins up Jahia + Nexus
  (docker-compose); `ci.build.sh` copies the locally-built `privateappstore` JAR
  *and* the sibling `store-template` `.tgz` into `tests/artifacts/`;
  `assets/provisioning.yml` installs the `javascript-modules-engine` + deps.
  Run: `cd tests && npx cypress run` (`baseUrl` localhost:8080). Suite = 20 specs,
  must stay green. Credentials come from `tests/.env` via `set-env.sh` (do not
  print or commit secrets).
  - **Accessibility gate** (`20-accessibility.cy.ts`): runs axe-core (cypress-axe)
    against the store-template pages (home grid, module detail, my-modules,
    in-site admin) at the full WCAG ladder up to **AAA** + landmark/region best
    practices. This is the enforced version of what used to be a manual EqualWeb
    audit — keep it green, and treat new AAA failures as real (the empty home
    page hides card/detail issues that only surface with seeded content).

## SonarQube

- Project key (derive as `<groupId>:<artifactId>`): **`org.jahia.community:privateappstore`**.
- Scan: `mvn -B clean install sonar:sonar` — the `jahia-modules` parent pins a
  Java-11-compatible scanner, so this runs on the host's default Java 11. The
  `sonar` profile (host `~/.m2/settings.xml`) supplies the URL + token.
- The gate counts **new-code** issues; keep new Java clean (`SubmitReview` is the
  reference — small focused methods, no generic `throws Exception`, constants over
  literals, no redundant casts).
- **Build site JCR paths from platform constants** (rule `S1075`): use
  `JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR + siteKey`
  (`org.apache.jackrabbit.core.fs.FileSystem`), never a hard-coded `"/sites/"` or
  a bare `"/"` — S1075 flags both the URI literal *and* the path-delimiter literal.
  `SITES_JCR_PATH` is `"/sites"` (no trailing slash).

## Conventions & contracts

- Layered: keep Actions/GraphQL thin; business logic in services/helpers.
- Domain errors as unchecked exceptions; log server-side, return safe messages.
- **Do not break the `moduleList.json` external contract** — it is consumed by
  `store-template` (and possibly external clients).
- Commit each change immediately (`git commit -s`), staging only changed files.
  Feature branch: `SECURITY-571-js-module-migration`.
