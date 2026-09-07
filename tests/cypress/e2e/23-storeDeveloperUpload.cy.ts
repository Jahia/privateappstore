import {DocumentNode} from 'graphql';
import {createSite, createUser, deleteSite, deleteUser, grantRoles, publishAndWaitJobEnding} from '@jahia/cypress';

/**
 * A store developer uploads a module and owns what the upload created.
 *
 * The store-developer role is the tier an operator hands to everyone allowed to publish a module.
 * It carries live permissions only, so a store developer works in the live workspace and never in
 * the editing one, and the storefront posts the upload from the public site. This spec therefore
 * publishes the site and drives the upload as a real store developer, which spec 11 does not: spec
 * 11 uploads as root, and root holds every permission in both workspaces.
 *
 * That identity is what gives the spec its discrimination. The owner entry on each created node is
 * written by the module through a system session. Write it from the caller's session instead and
 * this upload fails, because a store developer holds no access-control rights.
 *
 * Requires the JS build of jahia-store-template, like specs 11, 17 and 22.
 */
describe('A store developer owns the module the upload creates', () => {
    const siteKey = 'devupload';
    const developer = 'storedev';
    const developerPassword = 'Storedev#1234';

    const repositoryPath = `/sites/${siteKey}/contents/modules-repository`;
    const modulePath = `${repositoryPath}/org/cypress/test/cy-js-module`;
    const versionPath = `${modulePath}/cy-js-module-1.0.0`;
    const islandBundle = '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js';

    const updateForgeSettings: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeSettings.graphql');

    const getAclEntries: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/query/getAclEntries.graphql');

    interface AclEntry {
        principal: {name: string};
        role: {name: string};
        aclEntryType: string;
        inherited: boolean;
    }

    /**
     * Assert that {@code path} carries a direct GRANT of {@code role} to {@code principal}, in the
     * named workspace. The reshaping lives inside should() so Cypress re-runs it, and so an added
     * __typename field cannot break a deep comparison.
     */
    const expectDirectGrant = (workspace: string, path: string, principal: string, role: string) =>
        cy.apollo({query: getAclEntries, variables: {workspace, path}, fetchPolicy: 'no-cache'})
            .its('data.jcr.nodeByPath.acl.aclEntries')
            .should((entries: AclEntry[]) => {
                const direct = entries
                    .filter(e => !e.inherited && e.aclEntryType === 'GRANT' && e.role.name === role)
                    .map(e => e.principal.name);
                expect(direct, `direct ${role} grants on ${path} in ${workspace}`).to.include(principal);
            });

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('jahia-store-template JS module not deployed — skipping the store-developer upload spec');
                this.skip();
            }
        });
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // Ignore — first run.
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'devupload.local',
            locale: 'en'
        });

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

        // One role, granted at the site the way an operator would. grantRoles writes the entry in the
        // editing workspace, so the site is published to carry it into live, where the role applies.
        createUser(developer, developerPassword);
        grantRoles(`/sites/${siteKey}`, ['store-developer'], developer, 'USER');
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
        deleteUser(developer);
    });

    it('uploads a JS module from the live storefront and owns the module and the version', () => {
        cy.login(developer, developerPassword);
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home/my-modules.html`);
        cy.get('[data-upload-ready="true"]', {timeout: 20000}).should('exist');

        cy.intercept('POST', /createEntryFromJar\.do/).as('upload');
        cy.get('input[type="file"][name="file"]').selectFile('assets/cy-js-module.tgz', {force: true});
        cy.get('[data-upload-ready] button[type="submit"]').click();
        cy.wait('@upload', {timeout: 60000});

        // The apollo client carries its own credentials and not the browser session, so the entries
        // are read back as root: an independent vantage from the identity that uploaded.
        expectDirectGrant('LIVE', modulePath, developer, 'owner');
        expectDirectGrant('LIVE', versionPath, developer, 'owner');
    });
});
