import {DocumentNode} from 'graphql';
import {createSite, deleteSite, publishAndWaitJobEnding, setNodeProperty, uploadFile} from '@jahia/cypress';

/**
 * Storefront read views (jahia-store-template JS module): the module list grid and
 * the module detail page.
 *
 * Seeds a jahia-store-template site with a modules-repository, two published modules
 * (one with a version) and one unpublished draft, plus a home page hosting a
 * forgeModulesList, then asserts:
 *   - the grid lists published entries and hides the unpublished draft
 *   - a module detail page renders title, description, version + download
 *
 * Requires the JS build of jahia-store-template (provides the forge views/templates).
 */
describe('Storefront read views (JS module)', () => {
    const siteKey = 'storefront';
    const contents = `/sites/${siteKey}/contents`;
    const repo = `${contents}/modules-repository`;
    const homeRender = `/cms/render/default/en/sites/${siteKey}/home.html`;
    const detailRender = `/cms/render/default/en${repo}/analytics.html`;

    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql');

    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql');

    const updateForgeBranding: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeBranding.graphql');

    const deleteNode: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/deleteNode.graphql');

    const islandBundle = '/modules/jahia-store-template/dist/client/components/forge/ModuleEditor.client.tsx.js';

    const addNode = (parentPath: string, name: string, primaryNodeType: string, properties: object[] = []) =>
        cy.apollo({mutation: addNodeWithProps, variables: {parentPath, name, primaryNodeType, properties}});

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then(res => {
            if (res.status !== 200) {
                cy.log('jahia-store-template JS module not deployed — skipping storefront spec');
                this.skip();
            }
        });
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        // The jahia-store-template import.xml seeds the home page (+ modules list),
        // the My-modules sub-page and contents/modules-repository.
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'storefront.local',
            locale: 'en'
        });

        // Published module with a version (into the imported modules-repository)
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'analytics', title: 'Analytics Dashboard'}
        });
        setNodeProperty(`${repo}/analytics`, 'description', '<p>Real-time charts and KPI widgets.</p>', 'en');
        setNodeProperty(`${repo}/analytics`, 'status', 'supported', 'en');
        setNodeProperty(`${repo}/analytics`, 'published', 'true', 'en');
        setNodeProperty(`${repo}/analytics`, 'supportedByJahia', 'true', 'en');
        // GroupId is intrinsic to a JAR module; the version download URL is GENERATED
        // from it (+ name/version/site), not stored on the version node.
        setNodeProperty(`${repo}/analytics`, 'groupId', 'org.cypress.test', 'en');
        addNode(`${repo}/analytics`, 'v100', 'jnt:forgeModuleVersion', [
            {name: 'versionNumber', value: '1.0.0'},
            {name: 'published', value: 'true'},
            {name: 'changeLog', value: '<ul><li>Initial release</li></ul>'}
        ]);
        addNode(`${repo}/analytics`, 'video', 'jnt:videostreaming', [
            {name: 'provider', value: 'youtube'},
            {name: 'identifier', value: 'dQw4w9WgXcQ'}
        ]);

        // A second published module with a different status (for filtering)
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'seo', title: 'SEO Toolkit'}
        });
        setNodeProperty(`${repo}/seo`, 'description', '<p>Meta tags and sitemaps.</p>', 'en');
        setNodeProperty(`${repo}/seo`, 'status', 'community', 'en');
        setNodeProperty(`${repo}/seo`, 'published', 'true', 'en');

        // Unpublished draft — must NOT appear in the grid
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'draft', title: 'Draft Module'}
        });
    });

    after(() => {
        deleteSite(siteKey);
    });

    beforeEach(() => {
        cy.login();
    });

    it('lists published modules and hides the unpublished draft', () => {
        cy.visit(homeRender);
        cy.contains('Analytics Dashboard').should('be.visible');
        cy.contains('SEO Toolkit').should('be.visible');
        cy.contains('Real-time charts').should('be.visible');
        cy.contains('Supported').should('be.visible');
        cy.contains('Draft Module').should('not.exist');
    });

    it('filters the grid by status (server-side facet)', () => {
        cy.visit(homeRender);
        // Server-side filtering: check a Status facet and submit the GET form; the page
        // reloads showing only matching modules (non-matching are absent, not just hidden).
        cy.get('[data-forge-filter] input[name="status"][value="supported"]', {timeout: 20000}).check();
        cy.get('[data-forge-filter] button[type="submit"]').click();
        cy.contains('[data-forge-card]', 'Analytics Dashboard').should('be.visible');
        cy.contains('[data-forge-card]', 'SEO Toolkit').should('not.exist');
    });

    it('filters the grid by text (server-side)', () => {
        cy.visit(homeRender);
        cy.get('[data-forge-filter] input[name="src_terms"]', {timeout: 20000}).clear().type('seo');
        cy.get('[data-forge-filter] button[type="submit"]').click();
        cy.contains('[data-forge-card]', 'SEO Toolkit').should('be.visible');
        cy.contains('[data-forge-card]', 'Analytics Dashboard').should('not.exist');
    });

    it('paginates the grid when modules exceed the page size', () => {
        const listNode = `/sites/${siteKey}/home/main/store-modules`;
        // Two published modules; shrink the page size to 1 so there are two pages.
        setNodeProperty(listNode, 'nbOfModulePerPage', '1', 'en');
        cy.visit(homeRender);
        cy.get('[data-forge-pagination]', {timeout: 20000}).should('exist');
        cy.get('[data-page-info]').should('contain.text', 'Page 1 of 2');
        cy.get('[data-forge-card]').should('have.length', 1);
        cy.get('[data-page-next]').click();
        cy.get('[data-page-info]').should('contain.text', 'Page 2 of 2');
        cy.get('[data-forge-card]').should('have.length', 1);
        // Restore the default so later tests see the full grid.
        setNodeProperty(listNode, 'nbOfModulePerPage', '9', 'en');
    });

    it('opens a module detail page with version, video + download', () => {
        cy.visit(detailRender);
        cy.contains('h1', 'Analytics Dashboard').should('be.visible');
        // Video is in the default Overview tab.
        cy.get('iframe[src*="youtube.com/embed/dQw4w9WgXcQ"]').should('exist');
        // Versions now open in a popup (from the header button), not a tab. Wait for the
        // island to hydrate (data-versions-ready) before clicking, else the handler is not yet wired.
        cy.get('[data-versions-open][data-versions-ready="true"]', {timeout: 20000}).click();
        cy.get('[data-versions-dialog][open]').within(() => {
            cy.contains('1.0.0').should('be.visible');
            cy.contains('a', 'Download')
                .should('have.attr', 'href')
                .and('contain', 'analytics-1.0.0.jar');
            // The per-version footer surfaces the release date (jcr:lastModified) — regression
            // for the dropped "Updated" / "Requires Jahia" version metadata.
            cy.get('[data-forge-version]').contains(/Updated/i).should('be.visible');
        });
    });

    it('shows the store.jahia.com-style Information rail + header download', () => {
        cy.visit(detailRender);
        // The latest-version download CTA is always in the title area.
        cy.get('[data-latest-download]')
            .should('have.attr', 'href')
            .and('contain', 'analytics-1.0.0.jar');
        // Module metadata lives in the always-visible Information rail (no longer behind a tab).
        cy.get('[data-detail-info]').within(() => {
            cy.contains('dt', 'Module ID').next('dd').should('contain.text', 'analytics');
            cy.contains('dt', /status/i).next('dd').should('contain.text', 'supported');
        });
    });

    it('the "My modules" list shows the user own modules, including drafts', () => {
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`);
        cy.contains('[data-forge-card]', 'Analytics Dashboard').should('be.visible');
        cy.contains('[data-forge-card]', 'SEO Toolkit').should('be.visible');
        // Owners see their own unpublished drafts here (unlike the public grid).
        cy.contains('[data-forge-card]', 'Draft Module').should('be.visible');
    });

    it('renders the configured footer (copyright + privacy link) from forge settings', () => {
        // Configure branding via the same mutation the Settings screen uses.
        cy.apollo({
            mutation: updateForgeBranding,
            variables: {
                siteKey,
                copyright: '© 2026 ACME Store',
                privacyUrl: 'https://acme.example.com/privacy'
            }
        });
        cy.visit(homeRender);
        cy.contains('footer', '© 2026 ACME Store').should('be.visible');
        cy.get('footer')
            .contains('a', /privacy/i)
            .should('have.attr', 'href', 'https://acme.example.com/privacy');
        // The RSS link points at the clean /feed alias (SEO rewrite), not the internal
        // .moduleList.rss render URL.
        cy.get('footer [data-rss-feed]')
            .should('have.attr', 'href')
            .and('match', /\/feed$/)
            .and('not.contain', 'moduleList.rss');
    });

    it('signs in via the header login form (posts to /cms/login)', () => {
        // Publish the site so the LIVE home exists (anonymous can only see LIVE),
        // then go anonymous — the header then shows the sign-in form.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
        cy.logout();
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home.html`);
        // Open the login panel (the toggle button), then fill + submit the form.
        cy.contains('button', /log in/i).click();
        cy.get('#login-username').type('root');
        cy.get('#login-password').type(`${Cypress.env('SUPER_USER_PASSWORD')}{enter}`);
        // A successful form login redirects back to the page, now authenticated:
        // the header shows the account menu with a real Log out *button* (not a link).
        cy.get('header').contains('button', /log out/i, {timeout: 20000}).should('be.visible');
        cy.contains('root').should('be.visible');
    });

    it('shows an inline error on bad credentials (no /cms/login dead-end)', () => {
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
        cy.logout();
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home.html`);
        cy.contains('button', /log in/i).click();
        cy.get('#login-username').type('root');
        cy.get('#login-password').type('definitely-not-the-password{enter}');
        // Jahia redirects back to the page with ?loginError=…; the login island surfaces an
        // inline message and reopens the form instead of stranding the user on /cms/login.
        cy.contains(/invalid username or password/i, {timeout: 20000}).should('be.visible');
        cy.location('pathname').should('not.contain', '/cms/login');
        // The error param is stripped from the URL so a refresh is clean.
        cy.location('search').should('not.contain', 'loginError');
    });

    it('gates the "My modules" nav entry by login + Store role', () => {
        cy.login();
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
        // Root holds every permission → the entry is shown.
        cy.visit(homeRender);
        cy.contains('nav a', /my modules/i).should('be.visible');
        // Anonymous visitor on the live site → the entry is hidden.
        cy.logout();
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home.html`);
        cy.get('nav', {timeout: 20000}).should('exist');
        cy.contains('nav a', /my modules/i).should('not.exist');
    });

    it('renders the configured logo in the header (DAM reference)', () => {
        // Upload an image into the site media library, then point the logo at it.
        uploadFile('../../assets/icon.png', `/sites/${siteKey}/files`, 'store-logo.png', 'image/png');
        cy.apollo({
            mutation: updateForgeBranding,
            variables: {siteKey, logo: `/sites/${siteKey}/files/store-logo.png`}
        });
        cy.visit(homeRender);
        cy.get('header img', {timeout: 20000})
            .should('have.attr', 'src')
            .and('include', 'store-logo.png');
    });

    // Destructive (removes the My-modules list node), so it runs LAST. The site is
    // torn down in after(), so nothing else depends on the deletion.
    it('keeps "My modules" hidden from anonymous even when the list content is invisible', () => {
        // Regression for the fail-open nav gate: the gate used to detect which page
        // to hide by querying for the jnt:forgeMyModulesList with the visitor's own
        // session. Delete that list node so the content-type query finds nothing —
        // the conventional-page-name safety net must still keep the entry hidden for
        // anonymous visitors (the page node itself is published and in the nav).
        cy.login();
        cy.apollo({mutation: deleteNode, variables: {path: `/sites/${siteKey}/home/my-modules/main/mine`}});
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en']);
        cy.logout();
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home.html`);
        cy.get('nav', {timeout: 20000}).should('exist');
        cy.contains('nav a', /my modules/i).should('not.exist');
    });
});
