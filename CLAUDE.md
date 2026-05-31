# CLAUDE.md — privateappstore

> **Read [AGENTS.md](./AGENTS.md) first.** It covers the component model, the
> Actions-vs-GraphQL permission wall, the JCR content model, the build/test loop,
> and the SonarQube setup. This file lists only the highest-risk gotchas.

## Must-knows before editing

- **`_dsannotations` in `pom.xml` must scan both `…graphql.*` and `…actions.*`.**
  Narrowing it silently un-registers every `@Component(service=Action.class)`.
  After any pom change, verify the generated `OSGI-INF/*.xml` descriptors exist
  for the actions.
- **Any-user write features = Jahia Action, not GraphQL.** `/modules/graphql` is
  permission-gated (ordinary users denied). See `actions/SubmitReview.java`:
  system-session-as-user write (ACL-bypass + correct `jcr:createdBy`), atomic
  one-per-user via deterministic node name, recomputed aggregate rating.
- **Action `.do` POSTs need CSRF via XMLHttpRequest** (CSRFGuard patches XHR, not
  `fetch`). The `store-template` client already does this.
- **Java source level < 16**: no `instanceof` pattern matching, no records.
- **Don't break the `moduleList.json` contract** consumed by `store-template`.

## SonarQube

- Project key: `org.jahia.community:privateappstore`.
- Scan: `mvn -B clean install sonar:sonar` (Java 11; parent pins the scanner;
  `sonar` profile in `~/.m2/settings.xml` provides URL + token).
- Build site JCR paths from platform constants — `JahiaSitesService.SITES_JCR_PATH
  + FileSystem.SEPARATOR` (jackrabbit) — never hard-code `"/sites/"` or `"/"`
  (rule `S1075` flags both the URI and the path-delimiter).

## Standing repo rules

- Commit each change immediately (`git commit -s`), staging only the files you
  changed. Feature branch: `SECURITY-571-js-module-migration`.
- Never print or commit secrets (the SonarQube token, `tests/.env` credentials).
