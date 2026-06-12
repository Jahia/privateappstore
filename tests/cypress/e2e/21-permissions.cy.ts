import {DocumentNode} from 'graphql';
import {createSite, deleteSite, setNodeProperty, publishAndWaitJobEnding, createUser, deleteUser, revokeRoles} from '@jahia/cypress';

/**
 * Negative permission coverage for the store — the two boundaries the blind review
 * flagged as untested:
 *
 *   1. A non-owner cannot edit a module. The in-site editor only renders when
 *      `node.hasPermission("jcr:write")` (ForgeEntryDetail `canEdit`), and the
 *      actual write goes through the generic jcr GraphQL mutation under the
 *      caller's session — so the real boundary is the JCR ACL. We run the editor's
 *      own mutation as a plain (non-owner) user and assert it does NOT apply, while
 *      the owner's identical write does.
 *   2. An anonymous caller is refused a download from a PRIVATE store. MavenProxy
 *      serves a site's artifacts only to a caller who can READ that site's
 *      modules-repository in their own LIVE session; a guest who cannot read it
 *      gets 403 (SECURITY-571 B1). We make the repository private by revoking the
 *      (inherited) guest "reader" grant on it.
 *
 * Identity note: GraphQL identity here is the apollo client's Basic-auth header
 * (cy.apolloClient), not the browser session — so a non-owner run uses a client
 * built with that user's credentials.
 */

const mutateNodeProperty: DocumentNode =
    require('graphql-tag/loader!../fixtures/graphql/mutation/mutateNodeProperty.graphql');

const createForgeModule: DocumentNode =
    require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

const getNodeProperty: DocumentNode =
    require('graphql-tag/loader!../fixtures/graphql/query/getNodeProperties.graphql');

const readDescription = (path: string) =>
    cy.apollo({query: getNodeProperty, variables: {path, name: 'description', language: 'en'}, fetchPolicy: 'no-cache'})
        .its('data.jcr.nodeByPath.properties[0].value');

describe('Non-owner cannot edit a module (JCR ACL enforcement)', () => {
    const siteKey = 'permEdit';
    const repo = `/sites/${siteKey}/contents/modules-repository`;
    const modulePath = `${repo}/widget`;
    const NON_OWNER = 'mallory';
    const NON_OWNER_PWD = 'Mallory#1234';

    const writeDescriptionAs = (value: string, creds?: {username: string; password: string}) => {
        const variables = {pathOrId: modulePath, name: 'description', value, language: 'en'};
        if (creds) {
            // A client bound to the non-owner's Basic-auth identity.
            return cy.apolloClient(creds, {log: true, setCurrentApolloClient: false})
                .apollo({mutation: mutateNodeProperty, variables});
        }

        // Default client → root (the module owner).
        return cy.apollo({mutation: mutateNodeProperty, variables});
    };

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // Ignore — first run.
        }

        createSite(siteKey, {languages: 'en', templateSet: 'jahia-store-template', serverName: 'permedit.local', locale: 'en'});
        // Created by root → root is the owner; the module is NOT owned by mallory.
        cy.apollo({mutation: createForgeModule, variables: {parentPath: repo, name: 'widget', title: 'Widget'}});
        setNodeProperty(modulePath, 'description', '<p>Original.</p>', 'en');
        createUser(NON_OWNER, NON_OWNER_PWD);
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
        deleteUser(NON_OWNER);
    });

    it('does not apply a non-owner write, but applies the owner write (ACL keys on the caller)', () => {
        // A non-owner runs the editor's own mutation: the JCR ACL must reject it, so the stored
        // value is unchanged (cy.apollo swallows the access-denied error — the outcome is the proof).
        writeDescriptionAs('<p>Tampered by a non-owner.</p>', {username: NON_OWNER, password: NON_OWNER_PWD});
        readDescription(modulePath).should('contain', 'Original').and('not.contain', 'Tampered');

        // Control: the same mutation as the owner (root) DOES apply — proving the rejection above
        // is the ACL discriminating by caller, not a broken request.
        writeDescriptionAs('<p>Edited by the owner.</p>');
        readDescription(modulePath).should('contain', 'Edited by the owner');
    });
});

describe('Anonymous is refused a download from a private store (MavenProxy authorization)', () => {
    const siteKey = 'permPrivate';
    const repo = `/sites/${siteKey}/contents/modules-repository`;
    // A syntactically valid Maven coordinate path (passes the proxy's SSRF/site-name guards),
    // so the request reaches the repository-read authorization gate.
    const proxyUrl = `/modules/mavenproxy/${siteKey}/org/example/widget/1.0.0/widget-1.0.0.jar`;

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // Ignore — first run.
        }

        createSite(siteKey, {languages: 'en', templateSet: 'jahia-store-template', serverName: 'permprivate.local', locale: 'en'});
        // Make the store PRIVATE: revoke the (inherited) guest "reader" grant on the module
        // repository, so a guest can no longer read the node the proxy gate checks.
        revokeRoles(repo, ['reader'], 'guest', 'USER');
        // The proxy reads the LIVE session, so the restricted ACL must be published.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
    });

    it('returns 403 to an anonymous caller', () => {
        cy.logout();
        cy.request({url: proxyUrl, failOnStatusCode: false}).then(res => {
            expect(res.status, 'anonymous download of a private store').to.equal(403);
        });
    });

    it('does not 403 an authenticated reader (the gate keys on the caller, not the path)', () => {
        cy.login();
        cy.request({url: proxyUrl, failOnStatusCode: false}).then(res => {
            // Root can read the repository → passes the authorization gate. This fixture has no
            // Maven URL configured, so it then fails downstream (400) — but crucially NOT 403.
            expect(res.status, 'authenticated reader is past the authz gate').to.not.equal(403);
        });
    });
});
