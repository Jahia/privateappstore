import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * In-site Store Administration (store-template JS module).
 *
 * The store-template module renders the Private App Store admin (Forge
 * settings / Categories / Roles) as a React island on a `site-admin` page,
 * directly inside the website (replacing the legacy Spring Web Flow tabs).
 *
 * This spec drives a real browser through the engine's hydration:
 *   - creates a site on the store-template template set
 *   - adds a jnt:page using the `site-admin` template
 *   - opens the page and asserts the island hydrates past its SSR "Loading…"
 *     placeholder, that tabs switch, and that a Forge-settings save round-trips
 *     through /modules/graphql (authenticated via the browser session).
 *
 * Requires the JS build of store-template to be deployed on the target Jahia
 * (it provides the `site-admin` template + the admin island bundle).
 */
describe('In-site Store Administration (JS module)', () => {
    const siteKey = 'inSiteAdmin'
    const adminPath = `/sites/${siteKey}/admin`
    const renderUrl = `/cms/render/default/en${adminPath}.html`

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql')

    // The in-site admin only exists in the store-template JS build. When the
    // legacy JSP build is deployed (e.g. standard CI provisioning), its admin
    // island bundle is absent — skip rather than fail.
    const islandBundle = '/modules/store-template/dist/client/admin/AdminApp.client.tsx.js'

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then((res) => {
            if (res.status !== 200) {
                cy.log('store-template JS module not deployed — skipping in-site admin spec')
                this.skip()
            }
        })
        cy.login()
        try {
            deleteSite(siteKey)
        } catch {
            // ignore
        }
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'insiteadmin.local',
            locale: 'en'
        })

        // Add a page that uses the in-site admin template.
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: `/sites/${siteKey}`,
                name: 'admin',
                primaryNodeType: 'jnt:page',
                properties: [{name: 'j:templateName', value: 'site-admin'}]
            }
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    beforeEach(() => {
        cy.login()
    })

    it('renders three admin tabs and hydrates the Forge settings form', () => {
        cy.visit(renderUrl)
        cy.contains('h1', /store administration/i).should('be.visible')
        cy.get('[role="tab"]').should('have.length', 3)
        // Hydration proof: the SSR placeholder is "Loading…"; once the island
        // hydrates and the forgeSettings query resolves, the form renders.
        cy.get('#forge-url', {timeout: 30000}).should('be.visible')
        cy.get('#forge-id').should('exist')
        cy.get('#forge-user').should('exist')
    })

    it('switches to the Categories tab', () => {
        cy.visit(renderUrl)
        cy.get('#forge-url', {timeout: 30000}).should('be.visible')
        cy.contains('[role="tab"]', /categor/i).click()
        cy.contains(/root category/i, {timeout: 15000}).should('be.visible')
    })

    it('switches to the Roles tab', () => {
        cy.visit(renderUrl)
        cy.get('#forge-url', {timeout: 30000}).should('be.visible')
        cy.contains('[role="tab"]', /role/i).click()
        // Each role section exposes a "Grant role…" button when empty.
        cy.contains('button', /grant role/i, {timeout: 15000}).should('be.visible')
    })

    it('saves Forge settings through /modules/graphql', () => {
        cy.visit(renderUrl)
        cy.get('#forge-url', {timeout: 30000}).should('be.visible').clear().type('https://store.example.com')
        cy.contains('button', /^Save$/).click()
        cy.contains(/settings saved/i, {timeout: 20000}).should('be.visible')
    })
})
