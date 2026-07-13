import {DocumentNode} from 'graphql';
import {createSite, deleteSite} from '@jahia/cypress';

/**
 * S34 / U1 — end-to-end proof that {@code ForgeMediaMimeListener} + {@code MagicByteImageValidator}
 * neutralize the stored-XSS surface: a file planted under a {@code jmix:forgeElement} with a spoofed
 * {@code jcr:mimeType} that is actually script-capable/markup is asynchronously REMOVED, while a
 * genuine raster survives. Files are seeded directly through the generic (non-CSRF-gated) JCR
 * GraphQL mutation — the same primitive a module author uses — with a spoofed mime.
 *
 * NOTE for Stage 6: authored offline (no local Cypress tooling). The binary seeding uses a
 * BINARY-typed {@code jcr:data} property (base64). If the live schema rejects inline binary seeding,
 * fall back to a browser {@code cy.visit + selectFile} upload; the assertions (offending node gone,
 * legit raster present) are unchanged. The wait is a deterministic poll on node-removal, not a fixed
 * timeout.
 */
describe('Media MIME guard removes spoofed/script-capable uploads (stored-XSS)', () => {
    const siteKey = 'mediaMimeGuard';
    const repo = `/sites/${siteKey}/contents/modules-repository`;
    const iconFolder = `${repo}/widget/icon`;
    const evilPath = `${iconFolder}/evil.png`;
    const legitPath = `${iconFolder}/logo.png`;

    const createForgeModule: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');
    const addNodeWithProperties: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql');
    const getNodeByPath: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getNodeByPath.graphql');

    // SVG carrying an inline <script>, base64-encoded, but declared as image/png.
    const evilSvg = Cypress.Buffer.from(
        '<svg xmlns="http://www.w3.org/2000/svg"><script>alert(document.domain)</script></svg>'
    ).toString('base64');

    const nodeExists = (path: string) =>
        cy.apollo({query: getNodeByPath, variables: {path}, fetchPolicy: 'no-cache', errorPolicy: 'all'})
            .then((res: {data?: {jcr?: {nodeByPath?: unknown}}}) => Boolean(res?.data?.jcr?.nodeByPath));

    const seedFile = (fileName: string, mimeType: string, base64Data: string) => {
        const filePath = `${iconFolder}/${fileName}`;
        cy.apollo({mutation: addNodeWithProperties, variables: {
            parentPath: iconFolder, name: fileName, primaryNodeType: 'jnt:file', properties: [], mixins: []
        }});
        cy.apollo({mutation: addNodeWithProperties, variables: {
            parentPath: filePath, name: 'jcr:content', primaryNodeType: 'jnt:resource',
            properties: [
                {name: 'jcr:mimeType', value: mimeType},
                {name: 'jcr:data', value: base64Data, type: 'BINARY'}
            ],
            mixins: []
        }});
    };

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // first run
        }

        createSite(siteKey, {languages: 'en', templateSet: 'jahia-store-template', serverName: 'mediamimeguard.local', locale: 'en'});
        cy.apollo({mutation: createForgeModule, variables: {parentPath: repo, name: 'widget', title: 'Widget'}});
        cy.apollo({mutation: addNodeWithProperties, variables: {
            parentPath: `${repo}/widget`, name: 'icon', primaryNodeType: 'jnt:contentFolder', properties: [], mixins: []
        }});

        // A genuine PNG (control) and the spoofed SVG-as-PNG (attack).
        cy.readFile('cypress/fixtures/icon.png', 'base64').then((pngB64: string) => {
            seedFile('logo.png', 'image/png', pngB64);
        });
        seedFile('evil.png', 'image/png', evilSvg);
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
    });

    it('removes the spoofed script-capable file and keeps the genuine raster', () => {
        // Poll until the async listener removes the offending node (both workspaces are checked
        // server-side). Deterministic wait, not a fixed sleep.
        cy.waitUntil(() => nodeExists(evilPath).then(exists => exists === false), {
            timeout: 30000,
            interval: 1000,
            errorMsg: 'spoofed file was not removed by ForgeMediaMimeListener'
        });

        // The legitimate raster must survive untouched.
        nodeExists(legitPath).should('equal', true);
    });
});
