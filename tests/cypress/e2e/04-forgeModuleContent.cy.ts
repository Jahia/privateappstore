import {DocumentNode} from 'graphql';
import {createSite, deleteSite} from '@jahia/cypress';

/**
 * End-to-end content lifecycle test against the combined module surface:
 *   1. Create a site with the store-template templates set.
 *   2. Create a jnt:forgeModule node under the site contents.
 *   3. Read it back, verifying the node type registered by privateappstore
 *      is usable for actual content creation (not just registered in the CND).
 *   4. Clean up.
 */
describe('Forge module content lifecycle', () => {
    const siteKey = 'forgeContentSite';
    const moduleName = 'cy-test-module';
    const modulePath = `/sites/${siteKey}/contents/${moduleName}`;

    const createForgeModule: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

    const getNodeByPath: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getNodeByPath.graphql');

    const deleteNode: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/deleteNode.graphql');

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
    });

    after(() => {
        cy.login();
        cy.apollo({mutation: deleteNode, variables: {path: modulePath}}).then(() => {
            // ignore — best effort cleanup before site deletion
        });
        deleteSite(siteKey);
    });

    it('creates and reads back a jnt:forgeModule node', () => {
        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: `/sites/${siteKey}/contents`,
                name: moduleName,
                title: 'Cypress Test Module'
            }
        })
            .its('data.jcr.addNode.node')
            .should((node: { path: string; uuid: string }) => {
                expect(node.path).to.equal(modulePath);
                expect(node.uuid).to.be.a('string').and.not.empty;
            });

        cy.apollo({query: getNodeByPath, variables: {path: modulePath}})
            .its('data.jcr.nodeByPath')
            .should((node: { name: string; primaryNodeType: { name: string } }) => {
                expect(node.name).to.equal(moduleName);
                expect(node.primaryNodeType.name).to.equal('jnt:forgeModule');
            });
    });
});
