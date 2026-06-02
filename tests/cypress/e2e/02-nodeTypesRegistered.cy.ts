import {DocumentNode} from 'graphql';

/**
 * Verify that the JCR node types declared in privateappstore's definitions.cnd
 * are actually registered after the module is started. If the CND failed to
 * load (e.g. malformed definition, missing dependency) the bundle may still
 * be ACTIVE but its types will be silently missing — which is why this is a
 * distinct test from the bundle-state smoke check.
 */
describe('Forge node types registered', () => {
    const getNodeTypeByName: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getNodeTypeByName.graphql');

    before(() => {
        cy.login();
    });

    const primaryTypes = [
        'jnt:forgeModule',
        'jnt:forgePackage',
        'jnt:forgeModuleVersion',
        'jnt:forgePackageVersion',
        'jnt:forgeModulesList',
        'jnt:forgeMyModulesList',
        'jnt:forgeScreenshotsList',
        'jnt:forgeSettings',
        'jnt:review',
        'jnt:reviews',
        'jnt:addReview'
    ];

    primaryTypes.forEach(typeName => {
        it(`registers primary type ${typeName}`, () => {
            cy.apollo({query: getNodeTypeByName, variables: {name: typeName}})
                .its('data.jcr.nodeTypeByName')
                .should((nt: { name: string; mixin: boolean }) => {
                    expect(nt, `node type ${typeName} should exist`).to.not.be.null;
                    expect(nt.name).to.equal(typeName);
                    expect(nt.mixin).to.equal(false);
                });
        });
    });

    const mixinTypes = ['jmix:forge', 'jmix:forgeElement', 'jmix:reviews', 'jmix:reportedReview'];

    mixinTypes.forEach(typeName => {
        it(`registers mixin type ${typeName}`, () => {
            cy.apollo({query: getNodeTypeByName, variables: {name: typeName}})
                .its('data.jcr.nodeTypeByName')
                .should((nt: { name: string; mixin: boolean }) => {
                    expect(nt, `mixin ${typeName} should exist`).to.not.be.null;
                    expect(nt.name).to.equal(typeName);
                    expect(nt.mixin).to.equal(true);
                });
        });
    });
});
