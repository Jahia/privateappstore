import {DocumentNode} from 'graphql'
import {createSite, deleteSite, publishAndWaitJobEnding, setNodeProperty, uploadFile} from '@jahia/cypress'

/**
 * Storefront read views (store-template JS module): the module list grid and
 * the module detail page.
 *
 * Seeds a store-template site with a modules-repository, two published modules
 * (one with a version) and one unpublished draft, plus a home page hosting a
 * forgeModulesList, then asserts:
 *   - the grid lists published entries and hides the unpublished draft
 *   - a module detail page renders title, description, version + download
 *
 * Requires the JS build of store-template (provides the forge views/templates).
 */
describe('Storefront read views (JS module)', () => {
    const siteKey = 'storefront'
    const contents = `/sites/${siteKey}/contents`
    const repo = `${contents}/modules-repository`
    const homeRender = `/cms/render/default/en/sites/${siteKey}/home.html`
    const detailRender = `/cms/render/default/en${repo}/analytics.html`

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const updateForgeBranding: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeBranding.graphql')

    const islandBundle = '/modules/store-template/dist/client/components/forge/ModuleEditor.client.tsx.js'

    const addNode = (parentPath: string, name: string, primaryNodeType: string, properties: object[] = []) =>
        cy.apollo({mutation: addNodeWithProps, variables: {parentPath, name, primaryNodeType, properties}})

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then((res) => {
            if (res.status !== 200) {
                cy.log('store-template JS module not deployed — skipping storefront spec')
                this.skip()
            }
        })
        cy.login()
        try {
            deleteSite(siteKey)
        } catch {
            // ignore
        }
        // The store-template import.xml seeds the home page (+ modules list),
        // the My-modules sub-page and contents/modules-repository.
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'storefront.local',
            locale: 'en'
        })

        // Published module with a version (into the imported modules-repository)
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'analytics', title: 'Analytics Dashboard'}
        })
        setNodeProperty(`${repo}/analytics`, 'description', '<p>Real-time charts and KPI widgets.</p>', 'en')
        setNodeProperty(`${repo}/analytics`, 'status', 'supported', 'en')
        setNodeProperty(`${repo}/analytics`, 'published', 'true', 'en')
        setNodeProperty(`${repo}/analytics`, 'supportedByJahia', 'true', 'en')
        addNode(`${repo}/analytics`, 'v100', 'jnt:forgeModuleVersion', [
            {name: 'versionNumber', value: '1.0.0'},
            {name: 'published', value: 'true'},
            {name: 'url', value: 'https://store.example.com/analytics-1.0.0.jar'},
            {name: 'changeLog', value: '<ul><li>Initial release</li></ul>'}
        ])
        addNode(`${repo}/analytics`, 'video', 'jnt:videostreaming', [
            {name: 'provider', value: 'youtube'},
            {name: 'identifier', value: 'dQw4w9WgXcQ'}
        ])

        // A second published module with a different status (for filtering)
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'seo', title: 'SEO Toolkit'}
        })
        setNodeProperty(`${repo}/seo`, 'description', '<p>Meta tags and sitemaps.</p>', 'en')
        setNodeProperty(`${repo}/seo`, 'status', 'community', 'en')
        setNodeProperty(`${repo}/seo`, 'published', 'true', 'en')

        // Unpublished draft — must NOT appear in the grid
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'draft', title: 'Draft Module'}
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    beforeEach(() => {
        cy.login()
    })

    it('lists published modules and hides the unpublished draft', () => {
        cy.visit(homeRender)
        cy.contains('Analytics Dashboard').should('be.visible')
        cy.contains('SEO Toolkit').should('be.visible')
        cy.contains('Real-time charts').should('be.visible')
        cy.contains('Supported').should('be.visible')
        cy.contains('Draft Module').should('not.exist')
    })

    it('filters the grid by status (instant sidebar facet)', () => {
        cy.visit(homeRender)
        // Wait for the filter island to hydrate (it marks itself ready after its
        // first pass) before toggling a facet.
        cy.get('[data-filter-ready]', {timeout: 20000})
        // The Status facets are multi-select checkboxes in the sidebar.
        cy.contains('[data-forge-list] label', /supported/i).find('input[type=checkbox]').check()
        cy.contains('[data-forge-card]', 'Analytics Dashboard').should('be.visible')
        cy.contains('[data-forge-card]', 'SEO Toolkit').should('not.be.visible')
    })

    it('filters the grid by text', () => {
        cy.visit(homeRender)
        cy.get('[data-filter-ready]', {timeout: 20000})
        cy.get('[data-forge-list] input[type=search]').type('seo')
        cy.contains('[data-forge-card]', 'SEO Toolkit').should('be.visible')
        cy.contains('[data-forge-card]', 'Analytics Dashboard').should('not.be.visible')
    })

    it('opens a module detail page with version, video + download', () => {
        cy.visit(detailRender)
        cy.contains('h1', 'Analytics Dashboard').should('be.visible')
        cy.contains('Versions').should('be.visible')
        cy.contains('1.0.0').should('be.visible')
        cy.contains('a', 'Download')
            .should('have.attr', 'href')
            .and('contain', 'analytics-1.0.0.jar')
        cy.get('iframe[src*="youtube.com/embed/dQw4w9WgXcQ"]').should('exist')
    })

    it('the "My modules" list shows the user own modules, including drafts', () => {
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`)
        cy.contains('[data-forge-card]', 'Analytics Dashboard').should('be.visible')
        cy.contains('[data-forge-card]', 'SEO Toolkit').should('be.visible')
        // Owners see their own unpublished drafts here (unlike the public grid).
        cy.contains('[data-forge-card]', 'Draft Module').should('be.visible')
    })

    it('renders the configured footer (copyright + privacy link) from forge settings', () => {
        // Configure branding via the same mutation the Settings screen uses.
        cy.apollo({
            mutation: updateForgeBranding,
            variables: {
                siteKey,
                copyright: '© 2026 ACME Store',
                privacyUrl: 'https://acme.example.com/privacy'
            }
        })
        cy.visit(homeRender)
        cy.contains('footer', '© 2026 ACME Store').should('be.visible')
        cy.get('footer')
            .contains('a', /privacy/i)
            .should('have.attr', 'href', 'https://acme.example.com/privacy')
    })

    it('signs in via the header login form (posts to /cms/login)', () => {
        // Publish the site so the LIVE home exists (anonymous can only see LIVE),
        // then go anonymous — the header then shows the sign-in form.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en'])
        cy.logout()
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home.html`)
        // Open the login panel (the toggle button), then fill + submit the form.
        cy.contains('button', /log in/i).click()
        cy.get('#login-username').type('root')
        cy.get('#login-password').type(`${Cypress.env('SUPER_USER_PASSWORD')}{enter}`)
        // A successful form login redirects back to the page, now authenticated:
        // the header shows the account menu with a real Log out *button* (not a link).
        cy.get('header').contains('button', /log out/i, {timeout: 20000}).should('be.visible')
        cy.contains('root').should('be.visible')
    })

    it('gates the "My modules" nav entry by login + Store role', () => {
        cy.login()
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en'])
        // root holds every permission → the entry is shown.
        cy.visit(homeRender)
        cy.contains('nav a', /my modules/i).should('be.visible')
        // Anonymous visitor on the live site → the entry is hidden.
        cy.logout()
        cy.visit(`/cms/render/live/en/sites/${siteKey}/home.html`)
        cy.get('nav', {timeout: 20000}).should('exist')
        cy.contains('nav a', /my modules/i).should('not.exist')
    })

    it('renders the configured logo in the header (DAM reference)', () => {
        // Upload an image into the site media library, then point the logo at it.
        uploadFile('../../assets/icon.png', `/sites/${siteKey}/files`, 'store-logo.png', 'image/png')
        cy.apollo({
            mutation: updateForgeBranding,
            variables: {siteKey, logo: `/sites/${siteKey}/files/store-logo.png`}
        })
        cy.visit(homeRender)
        cy.get('header img', {timeout: 20000})
            .should('have.attr', 'src')
            .and('include', 'store-logo.png')
    })
})
