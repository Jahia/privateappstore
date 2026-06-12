import {DocumentNode} from 'graphql';
import {createSite, deleteSite} from '@jahia/cypress';

/**
 * Module-upload workflow check.
 *
 * The legacy upload action chains a Maven deploy:deploy-file against the
 * configured forge URL BEFORE creating any JCR nodes — that part is exercised
 * by the integration stack (Nexus + mvn) and intentionally out of scope here.
 * What this test pins down is:
 *
 *   1. With forge settings configured, the createEntryFromJar.do endpoint
 *      responds to multipart-less probes with an action-style error JSON
 *      (proves the route is wired and the Action is active).
 *   2. A jnt:forgeModule can be created under contents/ and is reachable via
 *      its module page in EDIT workspace — the same node-creation path the
 *      upload action ultimately walks.
 */
describe('Module upload — UI workflow', () => {
    const siteKey = 'moduleUploadUiSite';
    const moduleName = 'cy-upload-module';

    const updateForgeSettings: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeSettings.graphql');

    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

    const getNodeProperty: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/query/getNodeProperties.graphql');

    const primaryType = (path: string) =>
        cy.apollo({query: getNodeProperty, variables: {path, name: 'jcr:primaryType', language: null}, fetchPolicy: 'no-cache'})
            .its('data.jcr.nodeByPath.properties[0].value');

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'localhost',
            locale: 'en'
        });
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('the createEntryFromJar.do endpoint is wired once forge settings are configured', () => {
        cy.apollo({
            mutation: updateForgeSettings,
            variables: {
                siteKey,
                url: Cypress.env('NEXUS_URL') ?
                    `${Cypress.env('NEXUS_URL')}/repository/maven-releases/` :
                    'http://nexus:8081/repository/maven-releases/',
                id: 'remote-repository',
                user: Cypress.env('NEXUS_USERNAME') || 'admin',
                password: Cypress.env('NEXUS_PASSWORD') || 'admin123'
            }
        });

        cy.login();
        // POST without a multipart body — the Action will reject with an
        // error JSON, but the fact we get a 200-class response with an error
        // body (not a 404) proves the .do is registered.
        cy.request({
            method: 'POST',
            url: `/sites/${siteKey}/contents/modules-repository.createEntryFromJar.do`,
            failOnStatusCode: false
        }).then(res => {
            expect(res.status, `${res.status} ${typeof res.body === 'string' ? res.body.slice(0, 120) : ''}`)
                .to.not.equal(404);
        });
    });

    it('creates a forge module via the GraphQL contract used by the upload action', () => {
        // The real CreateEntryFromJar action creates the module UNDER
        // modules-repository (resource.getNode() == modules-repository), so
        // exercise the same parent to faithfully mirror the upload contract.
        const repositoryPath = `/sites/${siteKey}/contents/modules-repository`;
        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: repositoryPath,
                name: moduleName,
                title: 'Cypress Upload Module'
            }
        })
            .its('data.jcr.addNode.node.path')
            .should('equal', `${repositoryPath}/${moduleName}`);

        cy.login();
        cy.request({
            url: `/cms/edit/default/en${repositoryPath}/${moduleName}.html`,
            failOnStatusCode: false
        })
            .its('status')
            .should('be.oneOf', [200, 302]);
    });

    it('renders the upload form as a hydrated XHR island that intercepts submit', function () {
        // Needs the JS build (island bundle). Skip on the legacy JSP build.
        cy.request({
            url: '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js',
            failOnStatusCode: false
        }).then(res => {
            if (res.status !== 200) {
                this.skip();
            }
        });
        cy.login();
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`);

        // Hydration marker proves the island mounted and the submit is JS-handled.
        // The old plain <form> had no marker and did a full-page navigation to the
        // .do (which is what broke CSRF). XHR posts let CsrfGuard inject the token.
        cy.get('[data-upload-ready="true"]', {timeout: 20000}).should('exist');
        cy.get('input[type="file"][name="file"]').should('exist');

        // Submitting with no file must be intercepted (no navigation to the .do) and
        // surface an inline message instead of landing the user on a blank page.
        cy.get('[data-upload-ready] button[type="submit"]').click();
        cy.contains('[role="alert"]', 'choose a module package').should('be.visible');
        cy.location('pathname').should('include', 'my-modules.html');
        cy.location('pathname').should('not.include', 'createEntryFromJar');
    });

    it('uploads a JS-module .tgz through the action and creates the forge node + version', function () {
        // End-to-end coverage of the createJavascriptModule path (the shared module-creation core,
        // minus the JAR-only Maven deploy which stays out of scope). The JS path attaches the
        // package to the version node — no Maven/Nexus round-trip — so it is fast and deterministic.
        cy.request({
            url: '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js',
            failOnStatusCode: false
        }).then(res => {
            if (res.status !== 200) {
                this.skip();
            }
        });
        const repositoryPath = `/sites/${siteKey}/contents/modules-repository`;
        cy.login();
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`);
        cy.get('[data-upload-ready="true"]', {timeout: 20000});

        cy.intercept('POST', /createEntryFromJar\.do/).as('upload');
        cy.get('input[type="file"][name="file"]').selectFile('assets/cy-js-module.tgz', {force: true});
        cy.get('[data-upload-ready] button[type="submit"]').click();
        // Wait for the action to run; its success path commits the node then redirects (which can
        // tear down response capture), so the authoritative check is the created node below.
        cy.wait('@upload', {timeout: 60000});

        // The createJavascriptModule path created the module under its groupId path
        // (groupId-as-folders + name) with a version child (name-version).
        const modulePath = `${repositoryPath}/org/cypress/test/cy-js-module`;
        primaryType(modulePath).should('equal', 'jnt:forgeModule');
        primaryType(`${modulePath}/cy-js-module-1.0.0`).should('equal', 'jnt:forgeModuleVersion');
    });
});
