# CLAUDE.md — jahia-store

> **Read [AGENTS.md](./AGENTS.md) first.** It covers the component model, the
> Actions-vs-GraphQL permission wall, the JCR content model, the build/test loop,
> and the SonarQube setup. This file lists only the highest-risk gotchas.

## Must-knows before editing

- **`_dsannotations` in `pom.xml` must scan both `…graphql.*` and `…actions.*`.**
  Narrowing it silently un-registers every `@Component(service=Action.class)`.
  After any pom change, verify the generated `OSGI-INF/*.xml` descriptors exist
  for the actions.
- **Write features with a Java side = Jahia Action, not GraphQL.** `/modules/graphql`
  is permission-gated (ordinary users denied). See `actions/CreateEntryFromJar.java`
  (module-JAR upload: Maven deploy + node creation, runs in the posting workspace)
  and `actions/PublishModule.java` (publish gate + auto-publish latest version).
- **Action `.do` POSTs need CSRF via XMLHttpRequest** (CSRFGuard patches XHR, not
  `fetch`, not plain `<form>` posts). The `jahia-store-template` client already does this.
  `/modules/graphql` is not CSRF-gated.
- **Java source level < 16**: no `instanceof` pattern matching, no records.
- **Don't break the `moduleList.json` contract** consumed by `jahia-store-template`.

## SonarQube

- Project key: `org.jahia.community:jahia-store`.
- Scan: `mvn -B clean install sonar:sonar` (Java 11; parent pins the scanner;
  `sonar` profile in `~/.m2/settings.xml` provides URL + token).
- Build site JCR paths from platform constants — `JahiaSitesService.SITES_JCR_PATH
  + FileSystem.SEPARATOR` (jackrabbit) — never hard-code `"/sites/"` or `"/"`
  (rule `S1075` flags both the URI and the path-delimiter).

## Standing repo rules

- Commit each change immediately (`git commit -s`), staging only the files you
  changed. Feature branch: `SECURITY-571-js-module-migration`.
- Never print or commit secrets (the SonarQube token, `tests/.env` credentials).
