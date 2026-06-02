import {DocumentNode} from 'graphql';
import {
    createSite,
    deleteSite,
    publishAndWaitJobEnding,
    setNodeProperty,
    uploadFile
} from '@jahia/cypress';

/**
 * End-to-end module catalog + download test.
 *
 * After a module + version are published, contentFolder.moduleList.jsp renders
 * /en/sites/{site}/contents/modules-repository.moduleList.json — the catalog
 * remote Jahia DX instances consume. This test proves:
 *   - the module appears in the published catalog with its version
 *   - the catalog's downloadUrl points at the real uploaded artifact and is
 *     actually downloadable (the "download test of the new module")
 *   - the published-only filter (unpublished modules MUST NOT appear)
 *
 * The version carries a real jnt:file (one of the module JARs under
 * tests/assets). When a version has a jnt:file child the JSP builds the
 * downloadUrl from that file's /files/ path rather than the `url` property —
 * so the catalog advertises a genuinely downloadable artifact.
 *
 * The module MUST live under modules-repository: the JSON renderer walks
 * jcr:getDescendantNodes(modules-repository, 'jnt:forgeModule'), exactly where
 * the real CreateEntryFromJar upload action places modules.
 */
describe('Module list JSON + download', () => {
    const siteKey = 'moduleListSite';
    const moduleName = 'cy-listed-module';
    const groupId = 'org.cypress.test';
    const version = '1.0.0';
    // A real module JAR (a copy of the privateappstore module under test),
    // shipped as a fixture under tests/assets with a version-agnostic name so
    // it does not drift with the build. cy.fixture resolves from
    // cypress/fixtures, so step up to tests/assets.
    const artifactFixture = '../../assets/sample-module.jar';
    const artifactName = 'sample-module.jar';

    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql');

    const updateForgeSettings: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeSettings.graphql');

    const repositoryPath = `/sites/${siteKey}/contents/modules-repository`;
    const modulePath = `${repositoryPath}/${moduleName}`;
    const versionName = `${moduleName}-${version}`;
    const versionPath = `${modulePath}/${versionName}`;
    const jsonUrl = `/en/sites/${siteKey}/contents/modules-repository.moduleList.json`;

    // A second module whose version carries NO jnt:file and NO stored `url` — like a
    // JAR module deployed to the site's Maven repo. The moduleList JSP GENERATES the
    // downloadUrl from the module coordinates + the request server (the URL is no
    // longer stored on the node, so it adapts to scheme/host/port/site), pointing at
    // the MavenProxy servlet under /modules — never /cms (the render servlet → 404).
    const proxyModuleName = 'cy-proxy-module';
    const proxyGroupId = 'org.cypress.proxy';
    const proxyVersion = '2.0.0';
    const proxyModulePath = `${repositoryPath}/${proxyModuleName}`;
    const proxyVersionPath = `${proxyModulePath}/${proxyModuleName}-${proxyVersion}`;
    // The path the JSP must generate (the host comes from the request's server):
    // <server>/modules/mavenproxy/<site>/<groupPath>/<module>/<version>/<artifact>
    const proxyDownloadPath =
        `/modules/mavenproxy/${siteKey}/` +
        `${proxyGroupId.replace(/\./g, '/')}/${proxyModuleName}/${proxyVersion}/` +
        `${proxyModuleName}-${proxyVersion}.jar`;

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'localhost',
            locale: 'en'
        });

        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: repositoryPath,
                name: moduleName,
                title: 'Cypress Listed Module'
            }
        });

        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: versionName,
                primaryNodeType: 'jnt:forgeModuleVersion',
                // No jcr:title — the type has no such definition.
                properties: [
                    {name: 'versionNumber', value: version}
                ]
            }
        });

        // Attach the real artifact as a jnt:file child of the version. The
        // moduleList JSP then derives downloadUrl from this file's path.
        uploadFile(artifactFixture, versionPath, artifactName, 'application/java-archive');

        setNodeProperty(modulePath, 'groupId', groupId, 'en');
        setNodeProperty(modulePath, 'published', 'true', 'en');
        setNodeProperty(versionPath, 'published', 'true', 'en');

        // Second module: a Maven-deployed (jnt:file-less) version whose downloadUrl
        // is the stored `url` — pinned to the /modules/mavenproxy proxy path.
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repositoryPath, name: proxyModuleName, title: 'Cypress Proxy Module'}
        });
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: proxyModulePath,
                name: `${proxyModuleName}-${proxyVersion}`,
                primaryNodeType: 'jnt:forgeModuleVersion',
                // No `url` property — the catalog JSP generates the download URL.
                properties: [
                    {name: 'versionNumber', value: proxyVersion}
                ]
            }
        });
        setNodeProperty(proxyModulePath, 'groupId', proxyGroupId, 'en');
        setNodeProperty(proxyModulePath, 'published', 'true', 'en');
        setNodeProperty(proxyVersionPath, 'published', 'true', 'en');

        // Publish the whole site so the /en/ LIVE URL resolves and the file's
        // binary content is available from the LIVE files servlet.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
    });

    after(() => {
        deleteSite(siteKey);
    });

    function findVersion(body: unknown) {
        const payload = Array.isArray(body) ? body[0] : body;
        const modules = (payload as { modules?: Array<{ name: string; versions: Array<{ version: string; downloadUrl: string }> }> }).modules || [];
        const module = modules.find(m => m.name === moduleName);
        return {payload, module, version: module?.versions?.find(v => v.version === version)};
    }

    it('moduleList.json lists the published module + version with a download URL', () => {
        cy.request({url: jsonUrl, failOnStatusCode: false}).then(res => {
            expect(res.status).to.equal(200);
            const {payload, module, version: v} = findVersion(res.body);
            expect(payload).to.have.property('modules');
            expect(module, `module ${moduleName} present in modules[]`).to.not.be.undefined;
            expect(v, `version ${version} listed`).to.not.be.undefined;
            // DownloadUrl points at the uploaded artifact via the files servlet.
            expect(v!.downloadUrl, 'downloadUrl present').to.be.a('string').and.not.be.empty;
            expect(v!.downloadUrl).to.contain('/files/');
            expect(v!.downloadUrl).to.contain(artifactName);
        });
    });

    it('the catalog downloadUrl serves the artifact (download test)', () => {
        cy.request({url: jsonUrl, failOnStatusCode: false}).then(res => {
            const {version: v} = findVersion(res.body);
            if (!v) {
                throw new Error('version not in listing — see preceding test failure');
            }

            // DownloadUrl may be absolute (http://host/files/...). Strip the
            // scheme+host and fetch the path against the Cypress baseUrl so it
            // resolves to the Jahia under test regardless of the host the JSP
            // rendered (localhost vs the docker service name).
            const path = v.downloadUrl.replace(/^https?:\/\/[^/]+/, '');
            cy.request({url: path, failOnStatusCode: false, encoding: 'binary'}).then(download => {
                expect(download.status, `download ${path}`).to.equal(200);
                expect(download.body.length, 'artifact body is non-empty').to.be.greaterThan(0);
            });
        });
    });

    it('generates a jnt:file-less version downloadUrl via /modules/mavenproxy (not /cms, not stored)', () => {
        cy.request({url: jsonUrl, failOnStatusCode: false}).then(res => {
            expect(res.status).to.equal(200);
            const payload = Array.isArray(res.body) ? res.body[0] : res.body;
            const modules =
                (payload as {modules?: Array<{name: string; versions: Array<{version: string; downloadUrl: string}>}>})
                    .modules || [];
            const proxyModule = modules.find(m => m.name === proxyModuleName);
            expect(proxyModule, `module ${proxyModuleName} present in modules[]`).to.not.be.undefined;
            const v = proxyModule!.versions.find(ver => ver.version === proxyVersion);
            expect(v, `version ${proxyVersion} listed`).to.not.be.undefined;
            // No `url` was stored on the node: the JSP GENERATED an absolute URL from the
            // request server + module coordinates, ending in the proxy path...
            expect(v!.downloadUrl, 'generated mavenproxy download path').to.contain(proxyDownloadPath);
            expect(v!.downloadUrl).to.match(/^https?:\/\//);
            // ...targeting the MavenProxy servlet under /modules, never the render
            // servlet under /cms (a /cms/mavenproxy URL 404s — SECURITY-571).
            expect(v!.downloadUrl).to.contain('/modules/mavenproxy/');
            expect(v!.downloadUrl, 'must not use the render-servlet (/cms) path').to.not.contain('/cms/mavenproxy/');
        });
    });

    it('the /modules/mavenproxy download servlet is registered (reachable, not 404)', () => {
        // MavenProxy is an OSGi DS @Component served at /modules/mavenproxy. If its
        // package isn't scanned by bnd's _dsannotations the servlet is never registered
        // and EVERY module download 404s (SECURITY-571). Assert the endpoint reaches the
        // servlet — not a 404. (No forge settings on this site, so the authenticated
        // request then errors reading them; the point is the servlet RUNS, proving it
        // is registered. A 404 here means the registration regressed.)
        cy.login();
        cy.request({
            url: `/modules/mavenproxy/${siteKey}/org/jahia/modules/x/1.0.0/x-1.0.0.jar`,
            failOnStatusCode: false
        }).then(res => {
            expect(res.status, 'mavenproxy servlet must be registered (404 ⇒ unregistered)').to.not.equal(404);
        });
    });

    it('streams a Maven-deployed artifact through the /modules/mavenproxy servlet', () => {
        // The proxy fetches the artifact from the site's configured Maven repo, forwarding
        // the stored credentials. Point the site's forge repo at the stack's Nexus (the
        // proxy runs INSIDE the Jahia container, so it must use the container hostname),
        // drop a real artifact into Nexus, then download it through the proxy.
        const forgeRepo = 'http://nexus:8081/repository/maven-releases'; // Jahia (container) view
        const nexusUser = Cypress.env('NEXUS_USERNAME') || 'admin';
        const nexusPass = Cypress.env('NEXUS_PASSWORD') || 'admin123';
        // Where the Cypress runner reaches Nexus (host: localhost:8081; container: nexus:8081).
        const putRepo = Cypress.env('NEXUS_URL') ?
            `${Cypress.env('NEXUS_URL')}/repository/maven-releases` :
            forgeRepo;
        const relPath = 'org/jahia/modules/proxytest/1.0.0/proxytest-1.0.0.jar';

        cy.login();
        // UpdateForgeSettings base64-encodes the password; the proxy base64-decodes it.
        cy.apollo({
            mutation: updateForgeSettings,
            variables: {siteKey, url: forgeRepo, id: 'remote-repository', user: nexusUser, password: nexusPass}
        });

        // Deploy a real artifact into Nexus at the Maven-layout path the proxy will fetch.
        // failOnStatusCode:false: release repos are immutable, so a re-run's PUT is a no-op
        // (4xx) but the artifact is already present.
        cy.fixture(artifactFixture, 'binary').then(bin => {
            const bytes = Uint8Array.from(bin as unknown as string, c => c.charCodeAt(0));
            cy.request({
                method: 'PUT',
                url: `${putRepo}/${relPath}`,
                auth: {user: nexusUser, pass: nexusPass},
                headers: {'Content-Type': 'application/java-archive'},
                body: bytes.buffer,
                failOnStatusCode: false
            });
        });

        // Download through the proxy → it streams the artifact from Nexus.
        cy.login();
        cy.request({
            url: `/modules/mavenproxy/${siteKey}/${relPath}`,
            encoding: 'binary',
            failOnStatusCode: false
        }).then(res => {
            expect(res.status, 'proxy download status').to.equal(200);
            expect(res.body.length, 'streamed artifact is non-empty').to.be.greaterThan(100);
        });
    });

    it('omits the module when published=false', () => {
        setNodeProperty(modulePath, 'published', 'false', 'en');
        publishAndWaitJobEnding(modulePath, ['en']);

        cy.request({url: jsonUrl, failOnStatusCode: false}).then(res => {
            const {module} = findVersion(res.body);
            expect(module, 'unpublished module hidden from listing').to.be.undefined;
        });
    });
});
