import {DocumentNode} from 'graphql';
import {createSite, deleteSite} from '@jahia/cypress';

/**
 * Verifies the React + GraphQL replacement for categorySettings.flow:
 *   - read returns rootCategoryUuid=null + empty categories on fresh site
 *   - setRootCategory wires up a root jnt:category
 *   - addForgeCategory creates a child jnt:category and returns its UUID
 *   - updateForgeCategoryTitles writes per-language titles which the query reflects
 *   - deleteForgeCategory removes the node
 *   - the admin route /jahia/administration/{siteKey}/categorySettings responds
 */
describe('Category settings admin (React + GraphQL)', () => {
    const siteKey = 'categorySettingsSite';
    const rootCategoryName = 'cy-root-categories';

    const getCategorySettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getCategorySettings.graphql');

    const setRootCategory: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/setRootCategory.graphql');

    const addForgeCategory: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/addForgeCategory.graphql');

    const updateForgeCategoryTitles: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeCategoryTitles.graphql');

    const deleteForgeCategory: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/deleteForgeCategory.graphql');

    const addNode: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeForTest.graphql');

    const getNodeProperty: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getNodeProperties.graphql');

    // Used by the helper that creates a root category node we can use as the
    // site's rootCategory. Kept separate from the production GraphQL fixtures.
    // We inline the mutation here so test 06 stays self-contained.

    let rootCategoryUuid: string;

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        createSite(siteKey, {
            languages: 'en,fr',
            templateSet: 'jahia-store-template',
            serverName: 'localhost',
            locale: 'en'
        });

        // Create a jnt:category node we can wire as rootCategory. Jahia's
        // jnt:virtualsite accepts child content nodes, so dropping the category
        // directly under the site root is the simplest isolated fixture.
        cy.apollo({
            mutation: addNode,
            variables: {
                parentPath: `/sites/${siteKey}`,
                name: rootCategoryName,
                primaryNodeType: 'jnt:category'
            }
        }).then(result => {
            rootCategoryUuid = (result.data as { jcr: { addNode: { node: { uuid: string } } } })
                .jcr.addNode.node.uuid;
        });
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('returns empty settings before any root is configured', () => {
        cy.apollo({query: getCategorySettings, variables: {siteKey}})
            .its('data.forgeCategorySettings')
            .should((s: { rootCategoryUuid: string | null; siteLanguages: string[]; categories: unknown[] }) => {
                expect(s.rootCategoryUuid).to.be.null;
                expect(s.siteLanguages).to.include.members(['en', 'fr']);
                expect(s.categories).to.have.length(0);
            });
    });

    it('wires the root category, then exposes it in the query', () => {
        cy.apollo({
            mutation: setRootCategory,
            variables: {siteKey, rootCategoryUuid}
        });

        cy.apollo({query: getCategorySettings, variables: {siteKey}})
            .its('data.forgeCategorySettings')
            .should((s: { rootCategoryUuid: string; rootCategoryPath: string }) => {
                expect(s.rootCategoryUuid).to.equal(rootCategoryUuid);
                expect(s.rootCategoryPath).to.equal(`/sites/${siteKey}/${rootCategoryName}`);
            });
    });

    it('creates a category, sets per-language titles, then deletes it', () => {
        let createdUuid: string;

        cy.apollo({
            mutation: addForgeCategory,
            variables: {siteKey, name: 'portlets'}
        })
            .its('data.addForgeCategory')
            .should((uuid: string) => {
                expect(uuid).to.be.a('string').and.not.empty;
                createdUuid = uuid;
            });

        cy.then(() => {
            cy.apollo({
                mutation: updateForgeCategoryTitles,
                variables: {
                    siteKey,
                    uuid: createdUuid,
                    titles: [
                        {language: 'en', title: 'Portlets'},
                        {language: 'fr', title: 'Portlets (FR)'}
                    ]
                }
            });

            cy.apollo({query: getCategorySettings, variables: {siteKey}})
                .its('data.forgeCategorySettings.categories')
                .should((categories: Array<{ uuid: string; titles: Array<{ language: string; title: string }> }>) => {
                    const created = categories.find(c => c.uuid === createdUuid);
                    expect(created, 'created category present').to.not.be.undefined;
                    const titlesByLang = Object.fromEntries(created!.titles.map(t => [t.language, t.title]));
                    expect(titlesByLang.en).to.equal('Portlets');
                    expect(titlesByLang.fr).to.equal('Portlets (FR)');
                });

            cy.apollo({
                mutation: deleteForgeCategory,
                variables: {siteKey, uuid: createdUuid}
            });

            cy.apollo({query: getCategorySettings, variables: {siteKey}})
                .its('data.forgeCategorySettings.categories')
                .should('have.length', 0);
        });
    });

    it('refuses to delete a category outside the site root category (scope guard, SECURITY-571)', () => {
        // A real jnt:category, but a SIBLING of the configured root category — i.e.
        // outside the subtree this site administrator manages. The site-scoped
        // permission gate must NOT let it be deleted by UUID (the work runs in a
        // system session that bypasses ACLs).
        const outsiderPath = `/sites/${siteKey}/cy-outsider-category`;
        let outsiderUuid: string;
        cy.apollo({
            mutation: addNode,
            variables: {parentPath: `/sites/${siteKey}`, name: 'cy-outsider-category', primaryNodeType: 'jnt:category'}
        }).then(result => {
            outsiderUuid = (result.data as {jcr: {addNode: {node: {uuid: string}}}}).jcr.addNode.node.uuid;
        });

        cy.then(() => {
            // DeleteForgeCategory must be REJECTED. cy.apollo catches GraphQL errors and
            // yields the ApolloError (it does not throw), so a successful call would carry
            // data.deleteForgeCategory; a rejected one carries graphQLErrors and no data.
            cy.apollo({mutation: deleteForgeCategory, variables: {siteKey, uuid: outsiderUuid}}).then(r => {
                const res = r as {data?: {deleteForgeCategory?: boolean}; graphQLErrors?: unknown[]};
                expect(res.data?.deleteForgeCategory, 'no deletion performed').to.not.equal(true);
                expect(res.graphQLErrors, 'rejected with a GraphQL error').to.exist;
            });

            // The category must still exist (uuid unchanged) — the delete was a no-op.
            cy.apollo({
                query: getNodeProperty,
                variables: {path: outsiderPath, name: 'jcr:primaryType', language: null},
                fetchPolicy: 'no-cache'
            })
                .its('data.jcr.nodeByPath.uuid')
                .should('equal', outsiderUuid);
        });
    });

    it('serves the admin route', () => {
        cy.login();
        cy.request({
            url: `/jahia/administration/${siteKey}/categorySettings`,
            failOnStatusCode: false
        })
            .its('status')
            .should('be.oneOf', [200, 302]);
    });
});
