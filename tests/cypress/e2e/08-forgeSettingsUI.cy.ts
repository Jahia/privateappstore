import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * Live-UI test for the React forgeSettings admin route.
 *
 * Drives the actual page (cy.visit -> Moonstone Inputs -> Save button) so a
 * regression in the React layer (mount, mutation wiring, refetch, success
 * banner) is caught alongside the GraphQL contract that test 05 already
 * covers from the API side.
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

        // React mount + first GraphQL response. The page header carries the
        // siteKey so we wait on it as the "ready" signal.
        cy.contains('h2', siteKey, {timeout: 30000}).should('be.visible')

        cy.get('#forge-url').clear().type('https://store.example.com')
        cy.get('#forge-id').clear().type('forge-ui-1')
        cy.get('#forge-user').clear().type('forge-ui-user')
        cy.get('#forge-password').clear().type('s3cret-ui')

        cy.contains('button', /^Save$/i).click()

        // Success banner from ForgeSettings.jsx.
        cy.contains(/success|updated|saved/i, {timeout: 15000}).should('be.visible')

        // Server-side state matches what the user typed.
        cy.apollo({query: getForgeSettings, variables: {siteKey}})
            .its('data.forgeSettings')
            .should((s: { url: string; id: string; user: string; passwordSet: boolean }) => {
                expect(s.url).to.equal('https://store.example.com')
                expect(s.id).to.equal('forge-ui-1')
                expect(s.user).to.equal('forge-ui-user')
                expect(s.passwordSet).to.equal(true)
            })
    })

    it('reloads the saved state after a page refresh', () => {
        cy.login()
        cy.visit(`/jahia/administration/${siteKey}/forgeSettings`)

        cy.get('#forge-url', {timeout: 30000}).should('have.value', 'https://store.example.com')
        cy.get('#forge-id').should('have.value', 'forge-ui-1')
        cy.get('#forge-user').should('have.value', 'forge-ui-user')
        // Password is intentionally never round-tripped to the client; the
        // input must come back blank with a placeholder hinting that it's set.
        cy.get('#forge-password').should('have.value', '')
    })
})
