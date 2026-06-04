import {DocumentNode} from 'graphql';
import {createSite, deleteSite, setNodeProperty} from '@jahia/cypress';

/**
 * Prepackaged site (jahia-store-template JS module).
 *
 * Verifies the template set's settings/import.xml: creating a site with the
 * jahia-store-template template set — with NO manual seeding — yields a working store
 * entirely on the JS templates: a home page rendering the modules grid, a
 * "My modules" page, a site-administration page, and a modules-repository
 * folder. Then a published module dropped into that repository shows on home.
 *
 * Requires the JS build of jahia-store-template.
 */
describe('Prepackaged store site (JS module)', () => {
    const siteKey = 'prepkg';
    const repo = `/sites/${siteKey}/contents/modules-repository`;
    const homeRender = `/cms/render/default/en/sites/${siteKey}/home.html`;

    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

    const getNodeByPath: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/query/getNodeByPath.graphql');

    const islandBundle = '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js';

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('jahia-store-template JS module not deployed — skipping prepackaged spec');
                this.skip();
            }
        });
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        // No content seeding here — the template set's import.xml provides it.
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'prepkg.local',
            locale: 'en'
        });
    });

    after(() => {
        deleteSite(siteKey);
    });

    beforeEach(() => {
        cy.login();
    });

    it('provisions a working home page (chrome + modules list) from import.xml', () => {
        cy.visit(homeRender);
        cy.get('header').should('be.visible');
        cy.contains('footer', /all rights reserved/i).should('exist');
        // The seeded forgeModulesList rendered (empty until a module is published).
        cy.contains(/no published modules/i).should('be.visible');
    });

    it('provisions the My modules sub-page', () => {
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`);
        // Upload form present (now an XHR island; the createEntryFromJar action URL
        // is carried in its hydration props rather than a <form action>).
        cy.get('input[type="file"][name="file"]').should('exist');
        cy.get('body').should($b => expect($b.html()).to.contain('createEntryFromJar.do'));
        // Store administration is no longer an in-site page — it lives in the Jahia
        // site administration (jContent), covered by specs 08/09/10.
    });

    it('provisions the content folders the upload action needs (modules-repository + modules-required-versions)', () => {
        // The createEntryFromJar action reads <site>/contents/modules-required-versions
        // (auto-creating the per-Jahia-version child on upload) and writes modules under
        // modules-repository. Both must be seeded by import.xml or the upload throws
        // PathNotFoundException.
        for (const folder of ['modules-repository', 'modules-required-versions']) {
            cy.apollo({
                query: getNodeByPath,
                variables: {path: `/sites/${siteKey}/contents/${folder}`}
            })
                .its('data.jcr.nodeByPath.primaryNodeType.name')
                .should('equal', 'jnt:contentFolder');
        }
    });

    it('shows a published module dropped into the imported modules-repository', () => {
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'hello', title: 'Hello Module'}
        });
        setNodeProperty(`${repo}/hello`, 'published', 'true', 'en');

        cy.visit(homeRender);
        cy.contains('[data-forge-card]', 'Hello Module').should('be.visible');
    });
});
