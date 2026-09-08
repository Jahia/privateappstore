import {DocumentNode} from 'graphql';
import {createSite, deleteSite, createUser, deleteUser, grantRoles, publishAndWaitJobEnding} from '@jahia/cypress';

/**
 * Regression coverage for GHSA-g6wp-ghxm-mx76 / SEC-375.
 *
 * jahia-store-template rendered per-user data into fragments that declared no cache properties,
 * so Jahia cached them under a key that models what the viewer may DO (an ACL component, which
 * also separates anonymous from authenticated) but never who the viewer IS. Two users holding the
 * same role therefore shared the slot:
 *
 *   1. `Page/default` renders Layout -> Header, which writes the viewer's own username into the
 *      account widget. One logged-in user was served another's username while browsing normally.
 *   2. `ForgeMyModulesList` selects rows on `jcr:createdBy = <the viewer's username>` and
 *      deliberately shows unpublished drafts, so one store-developer was served another's module
 *      list - disclosing unreleased modules.
 *
 * Both are now fixed by a `cache.perUser` declaration, and this spec is what proves it.
 *
 * ## Three things are load-bearing in how this spec is written
 *
 * 1. **Both users hold exactly the same role.** That is the point, not an accident of the
 *    fixture. The cache key's ACL component already separates users of DIFFERENT permission, so a
 *    spec whose two users differ in role would pass against a completely unfixed build. Identical
 *    roles are what force the assertion onto the identity axis. Do not "tidy" one of the
 *    grantRoles calls away.
 *
 * 2. **Nothing flushes the cache and nothing varies the URL between the two users.** The
 *    advisory's own warning: a cache-busting query parameter enters the fragment key, the page
 *    then renders fresh, every user correctly sees their own name, and a still-broken build reads
 *    as fixed. The warm-then-check ORDER *is* the exploit - the first user populates the
 *    fragment, the second reads it - so each arm below visits a byte-identical URL, and the whole
 *    sequence stays inside one `it` so it cannot be reordered or run alone against a cold cache.
 *
 * 3. **Every arm answers HTTP 200**, exactly as in 23-storeDeveloperAcl. The status code is never
 *    the discriminator; only the rendered body is. And each arm asserts the positive case (this
 *    user's own name / own module IS present) as well as the negative, because
 *    `should('not.contain', ...)` passes trivially against a header that failed to render at all.
 *
 * Rendering is against `live` throughout: edit-mode rendering does not exercise the fragment
 * cache, so an EDIT-workspace version of this spec would prove nothing.
 *
 * Identity note: as in 21-permissions and 23-storeDeveloperAcl, GraphQL identity is the apollo
 * client's Basic-auth header (`cy.apolloClient`); browser identity is the session (`cy.login`).
 */

const createForgeModuleInWorkspace: DocumentNode =
    require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModuleInWorkspace.graphql');

describe('A cached fragment never serves one user the identity of another (GHSA-g6wp-ghxm-mx76)', () => {
    const siteKey = 'cacheIdentity';
    const sitePath = `/sites/${siteKey}`;
    const repositoryPath = `${sitePath}/contents/modules-repository`;

    // Two ordinary store developers. The usernames must not be substrings of one another, or the
    // `not.contain` assertions below would false-fail.
    const ALICE = 'alicedev';
    const ALICE_PWD = 'Alicedev#1234';
    const BOB = 'bobdev';
    const BOB_PWD = 'Bobdev#1234';

    const ALICE_MODULE = 'alice-widget';
    const ALICE_TITLE = 'Alice Widget';
    const BOB_MODULE = 'bob-gadget';
    const BOB_TITLE = 'Bob Gadget';

    // One URL per page for every arm - no query string, ever. See note 2 above.
    const homeLive = `/cms/render/live/en${sitePath}/home.html`;
    const myModulesLive = `/cms/render/live/en${sitePath}/home/my-modules.html`;

    const islandBundle = '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js';

    /** Create a forge module in LIVE as `creds`, so the node's jcr:createdBy is that user. */
    const createModuleAs = (creds: { username: string; password: string }, name: string, title: string) =>
        cy
            .apolloClient(creds, {log: true, setCurrentApolloClient: false})
            .apollo({
                mutation: createForgeModuleInWorkspace,
                variables: {workspace: 'LIVE', parentPath: repositoryPath, name, title}
            });

    /** Switch the browser session to `user`, leaving no cookie from the previous arm. */
    const browseAs = (user: string, password: string) => {
        cy.logout();
        cy.login(user, password);
    };

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // Ignore — first run.
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'cacheidentity.local',
            locale: 'en'
        });

        createUser(ALICE, ALICE_PWD);
        createUser(BOB, BOB_PWD);
        // IDENTICAL roles — see note 1. store-developer also carries j:privilegedAccess, which is
        // what lets an ordinary uploader reach /modules/graphql at all (see 23-storeDeveloperAcl).
        grantRoles(sitePath, ['store-developer'], ALICE, 'USER');
        grantRoles(sitePath, ['store-developer'], BOB, 'USER');

        // The pages and the modules-repository have to exist in live before anything is created
        // there, and before the front-end can be rendered at all.
        publishAndWaitJobEnding(sitePath, ['en']);

        // Seed one module per user, in LIVE and under each user's own session, so that
        // ForgeMyModulesList's `jcr:createdBy` filter has something to discriminate on.
        createModuleAs({username: ALICE, password: ALICE_PWD}, ALICE_MODULE, ALICE_TITLE);
        createModuleAs({username: BOB, password: BOB_PWD}, BOB_MODULE, BOB_TITLE);
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
        deleteUser(ALICE);
        deleteUser(BOB);
    });

    it('serves each user their own username in the header, never the previous visitor\'s', function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('jahia-store-template JS module not deployed — skipping');
                this.skip();
            }
        });

        // Arm 1 — ALICE populates the fragment for this exact path.
        browseAs(ALICE, ALICE_PWD);
        cy.visit(homeLive);
        cy.get('[data-account-name]', {timeout: 20000}).should('contain', ALICE);

        // Arm 2 — BOB reads the SAME path. No flush, no query string: if the fragment is keyed
        // without identity, this is where he is handed ALICE's name. The positive assertion is
        // the control - without it an empty header would satisfy the negative one.
        browseAs(BOB, BOB_PWD);
        cy.visit(homeLive);
        cy.get('[data-account-name]', {timeout: 20000})
            .should('contain', BOB)
            .and('not.contain', ALICE);

        // Arm 3 — back to ALICE. Catches a "fix" that merely serves the most recent visitor to
        // everybody, which would satisfy arm 2 on its own.
        browseAs(ALICE, ALICE_PWD);
        cy.visit(homeLive);
        cy.get('[data-account-name]', {timeout: 20000})
            .should('contain', ALICE)
            .and('not.contain', BOB);
    });

    it('serves each user their own modules on my-modules, never the previous visitor\'s', function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('jahia-store-template JS module not deployed — skipping');
                this.skip();
            }
        });

        // Arm 1 — ALICE populates the my-modules fragment.
        browseAs(ALICE, ALICE_PWD);
        cy.visit(myModulesLive);
        cy.contains('[data-forge-card]', ALICE_TITLE, {timeout: 20000}).should('be.visible');
        cy.contains('[data-forge-card]', BOB_TITLE).should('not.exist');

        // Arm 2 — BOB reads the same path. On an unfixed build he is served ALICE's list, which
        // for a real store means the titles of her unreleased modules.
        browseAs(BOB, BOB_PWD);
        cy.visit(myModulesLive);
        cy.contains('[data-forge-card]', BOB_TITLE, {timeout: 20000}).should('be.visible');
        cy.contains('[data-forge-card]', ALICE_TITLE).should('not.exist');
    });
});
