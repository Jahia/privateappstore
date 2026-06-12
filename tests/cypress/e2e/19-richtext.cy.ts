import {DocumentNode} from 'graphql';
import {createSite, deleteSite, setNodeProperty} from '@jahia/cypress';

/** Click Save and wait for the full-page reload the editor triggers (reload-safe
 *  via a window stamp — see 17-authoring for the rationale). */
const saveAndWaitReload = (): void => {
    cy.window().then(w => {
        (w as unknown as {__preSave?: boolean}).__preSave = true;
    });
    // The editor Save is an icon-only button (tooltip + aria-label); target its
    // stable data hook rather than visible text.
    cy.get('[data-editor-save]').click();
    cy.window({timeout: 20000}).should(w => {
        expect((w as unknown as {__preSave?: boolean}).__preSave).to.be.undefined;
    });
};

/**
 * Rich-text editing for the jahia-store-template JS module.
 *
 * The richtext metadata fields (description, how-to-install, FAQ, license) are now
 * edited in-site with CKEditor 5, loaded at runtime from the deployed
 * richtext-ckeditor5 module's federated remote (the store does not bundle CKEditor).
 * CKEditor filters content at its model boundary, and the HTML is additionally
 * sanitized with DOMPurify on save (defense-in-depth) before it is persisted and
 * rendered with dangerouslySetInnerHTML.
 *
 * This spec verifies the edit -> save -> persist -> render round-trip and that the
 * rendered richtext is clean. Requires the JS build of jahia-store-template AND the
 * richtext-ckeditor5 module deployed.
 */
describe('Rich text editing (JS module)', () => {
    const siteKey = 'features';
    const repo = `/sites/${siteKey}/contents/modules-repository`;
    const moduleEdit = `/cms/render/default/en${repo}/demo.html`;

    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

    const islandBundle = '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js';
    const ckeditorRemote = '/modules/richtext-ckeditor5/javascript/apps/remoteEntry.js';

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('jahia-store-template editor island not deployed — skipping');
                this.skip();
            }
        });
        cy.request({url: ckeditorRemote, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('richtext-ckeditor5 remote not deployed — skipping');
                this.skip();
            }
        });
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'features.local',
            locale: 'en'
        });
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'demo', title: 'Demo Module'}
        });
        setNodeProperty(`${repo}/demo`, 'description', '<p>Original description.</p>', 'en');
        setNodeProperty(`${repo}/demo`, 'published', 'true', 'en');
    });

    after(() => {
        deleteSite(siteKey);
    });

    beforeEach(() => {
        cy.login();
    });

    it('edits the description with CKEditor 5 and persists clean HTML', () => {
        cy.visit(moduleEdit);
        cy.get('[data-editor-ready]', {timeout: 20000});
        cy.contains('button', /edit module/i).click();
        cy.contains('[role="tab"]', /^Description$/).click();

        // CKEditor is fetched from the federated remote and instantiated on the page.
        cy.get('[data-ckeditor-state="ready"]', {timeout: 30000}).should('exist');
        cy.get('.ck-editor__editable', {timeout: 10000}).as('ed').should('be.visible');

        // Replace the seeded content via the real editor.
        cy.get('@ed').click().type('{ctrl+a}{del}');
        cy.get('@ed').type('Edited via CKEditor');

        // Saving reloads the page; wait for that reload before asserting.
        saveAndWaitReload();

        // The rendered description reflects the edit…
        cy.contains('Edited via CKEditor', {timeout: 20000}).should('be.visible');
        // …as clean HTML (scoped to the richtext container, not the whole page).
        cy.contains('Edited via CKEditor').then($el => {
            const html = $el.parent().html().toLowerCase();
            expect(html, 'no script element from richtext').not.to.contain('<script');
            expect(html, 'no inline event handler').not.to.contain('onerror');
        });
    });
});
