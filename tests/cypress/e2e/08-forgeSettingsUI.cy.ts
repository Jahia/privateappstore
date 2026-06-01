import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * Live-UI test for the React forgeSettings admin route.
 *
 * Drives the actual page (cy.visit -> Moonstone Inputs -> Save button) so a
 * regression in the React layer (mount, mutation wiring, refetch, success
 * banner) is caught alongside the GraphQL contract that test 05 already
 * covers from the API side.
 *
 * NOTE: Moonstone's <Field> wrapper inherits the `id` we pass; the inner
 * <input> is reached via `#id input` rather than `#id` directly.
 */
describe('Forge settings — live UI', () => {
    const siteKey = 'forgeSettingsUiSite'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getForgeSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getForgeSettings.graphql')

    before(() => {
        cy.login()
        try {
            deleteSite(siteKey)
        } catch {
            // ignore
        }
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'localhost',
            locale: 'en'
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('renders, accepts input, saves, and the API reflects the change', () => {
        cy.login()
        cy.visit(`/jahia/administration/${siteKey}/forgeSettings`)

        cy.contains('h2', siteKey, {timeout: 60000}).should('be.visible')

        cy.get('#forge-url input').clear().type('https://store.example.com')
        cy.get('#forge-id input').clear().type('forge-ui-1')
        cy.get('#forge-user input').clear().type('forge-ui-user')
        cy.get('#forge-password input').clear().type('s3cret-ui')

        cy.contains('button', /^Save$/i).click()

        cy.contains(/success|updated|saved/i, {timeout: 15000}).should('be.visible')

        cy.apollo({query: getForgeSettings, variables: {siteKey}})
            .its('data.forgeSettings')
            .should((s: { url: string; id: string; user: string; passwordSet: boolean }) => {
                expect(s.url).to.equal('https://store.example.com')
                expect(s.id).to.equal('forge-ui-1')
                expect(s.user).to.equal('forge-ui-user')
                expect(s.passwordSet).to.equal(true)
            })
    })

    it('nests Settings/Categories/Roles under a "Store administration" group', () => {
        cy.login()
        // Landing on a child auto-opens its parent group in the admin tree, so all
        // three leaves and the group header are visible together.
        cy.visit(`/jahia/administration/${siteKey}/forgeSettings`)
        cy.contains('h2', siteKey, {timeout: 60000}).should('be.visible')

        cy.contains(/store administration/i, {timeout: 15000}).should('be.visible')
        cy.contains(/private app store/i).should('be.visible')
        cy.contains(/app store categories/i).should('be.visible')
        cy.contains(/app store roles/i).should('be.visible')
    })

    it('reloads the saved state after a page refresh', () => {
        cy.login()
        cy.visit(`/jahia/administration/${siteKey}/forgeSettings`)

        cy.get('#forge-url input', {timeout: 60000}).should('have.value', 'https://store.example.com')
        cy.get('#forge-id input').should('have.value', 'forge-ui-1')
        cy.get('#forge-user input').should('have.value', 'forge-ui-user')
        cy.get('#forge-password input').should('have.value', '')
    })
})
