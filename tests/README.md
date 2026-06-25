# Tests

End-to-end Cypress tests for the **Jahia Store** (`jahia-store`) module and the **Jahia Store Template** (`jahia-store-template`) module.

Two options are available to run the tests, you can either run everything in Docker or only run Jahia in Docker and run the tests using your local node.

## What gets installed

The Docker stack brings up three services on a shared network (`stack`):

| Service  | Image                        | Purpose                                                                                  |
|----------|------------------------------|------------------------------------------------------------------------------------------|
| `nexus`  | `sonatype/nexus3:3.70.1`     | Maven repository backing the private app store. Default creds: `admin` / `admin123`.     |
| `jahia`  | `${JAHIA_IMAGE}`             | Jahia instance with `jahia-store` + `jahia-store-template` provisioned. Waits for Nexus.   |
| `cypress`| `${TESTS_IMAGE}`             | Runs the Cypress specs against the Jahia instance.                                       |

The provisioning manifest installs Jahia plus:
- `jahia-store` — the backend module being tested (taken from `../target/*-SNAPSHOT.jar` when present, otherwise from Nexus).
- `jahia-store-template` — the front-end templates set, pulled from a local sibling checkout at `../../store-template/target/*-SNAPSHOT.tgz` when present, otherwise from Nexus (see `provisioning-manifest-snapshot.yml`). The on-disk folder keeps its `store-template/` name.

Nexus is reachable from inside the network at `http://nexus:8081` (Jahia / Cypress) and from the host at `http://localhost:8081`. A test configures the forge module's per-site settings (Nexus URL + credentials) through the store admin GraphQL (`forge { updateSettings }`) or the `ForgeSettingsService` — they are stored as a per-site OSGi factory config (`karaf/etc/org.jahia.modules.forge.forgeSettings-<siteKey>.cfg`), not JCR properties. Point the URL at the internal `http://nexus:8081`, using the `NEXUS_USERNAME` / `NEXUS_PASSWORD` credentials.

On first boot Nexus auto-creates two hosted Maven repositories (`maven-releases`, `maven-snapshots`) which the forge module can use without further setup. If you need additional repos for a specific test scenario, create them via the Nexus REST API in a `before()` hook.

To exercise both modules together locally, build them first:

```bash
# From the parent SECURITY-571 folder
mvn -f privateappstore/pom.xml clean package -DskipTests
mvn -f store-template/pom.xml clean package -DskipTests
```

Then run the tests as described below.

## Run all in Docker

Once you have a built test container, the entirety of the tests, from environment provisioning to report generation, can be executed using a single command.

```bash
# Build the test container
> bash ci.build.sh
# Execute the tests
> bash ci.startup.sh
```

This is this exact process that will be used by the CI platform to execute the tests. Although it's definitely the easiest way of going through one run, it's also the method you're the less likely to use on a day-to-day.

The primary reason for this method to be reserved to the CI platform is that it doesn't make it easy to develop new tests or debug one single test.

> IMPORTANT: If you are using this method locally, do not forget that you will need to **rebuild the test container** (`bash ci.build.sh`) every time a change is done in the `tests/` folder, otherwise your change will not make their way to the container.

## Run the tests on a local node

This is the method you will be using the most when developing or debugging tests, and the major point of attention here concerns the use of the `env.run.sh` script.

As a reminder, the purpose of the `env.run.sh` script is to provision the environment **AND** execute the tests; in most cases you'd want to provision the environment only once, but run the tests multiple times.

```bash
# Fetch the necessary javascript dependencies
> yarn
# Run the docker environment, but without the tests
> ./ci.startup.sh notests
# Provision the environment and run the tests in headless once
> ./env.run.sh
# For bash
> ./set-env.sh
> yarn run e2e:debug
```

Do *NOT* forget to load your environment variables using `source set-env.sh` prior to running Cypress, as well as **every time you open a new terminal**.
