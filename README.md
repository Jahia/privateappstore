<a href="https://www.jahia.com/">
    <img src="https://www.jahia.com/modules/jahiacom-templates/images/jahia-3x.png" alt="Jahia logo" title="Jahia" align="right" height="60" />
</a>

Private App store
======================

The **Private App Store** ("Forge") backend — a Jahia 8.2 Java/OSGi module
providing the JCR content model, the GraphQL admin API, the Jahia Actions for
authoring (module upload, reviews, …), background jobs, a Maven proxy, and the
`moduleList.json` feed.

Its website and in-site administration UI are provided by the sibling
**[`store-template`](../store-template)** JavaScript module.

## Build & test

```bash
mvn clean install                 # build the OSGi bundle (Java 11)
cd tests && npx cypress run        # end-to-end suite (Jahia on localhost:8080)
```

The `tests/` harness (`@jahia/cypress`) provisions Jahia + Nexus, installs the
`javascript-modules-engine`, this module's JAR, and the `store-template` `.tgz`.

## Documentation

- **[AGENTS.md](./AGENTS.md)** — component model (OSGi DS vs blueprint), the
  Actions-vs-GraphQL permission wall, the JCR content model, build/test, and
  SonarQube setup. **Start here before modifying this module.**

## Open-Source

This is an Open-Source module, you can find more details about Open-Source @ Jahia [in this repository](https://github.com/Jahia/open-source).