import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * Live-UI test for the React manageRoles admin route.
 *
 * Verifies the end-to-end grant flow:
 *   1. open the "Add member" panel for store-administrator
 *   2. search for the built-in 'root' user
 *   3. click the search result to grant the role
 *   4. assert the member appears under that role
 *   5. read the server-side ACL back through GraphQL
 */
describe('Manage roles — live UI', () => {
    const siteKey = 'manageRolesUiSite'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getManageRolesSettings: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/query/getManageRolesSettings.graphql')

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

    it('grants store-administrator to a searched user via the UI', () => {
        cy.login()
        cy.visit(`/jahia/administration/${siteKey}/manageRoles`)

        // Wait until the React app paints all three role sections.
        cy.contains('h2', siteKey, {timeout: 30000}).should('be.visible')
        cy.contains('h3', /administrator/i).should('be.visible')

        // The first "Add member" button on the page belongs to the first role
        // (store-administrator). Filtering by section keeps the click stable.
        cy.contains('section', /administrator/i).within(() => {
            cy.contains('button', /add member/i).click()
            cy.get('input').filter('[id^=search-]').type('root')
            cy.contains('button', /search/i).click()
        })

        cy.contains('li', 'root', {timeout: 15000}).click()

        // After granting, the section should now list a member named "root".
        cy.contains('section', /administrator/i)
            .find('li')
            .contains('root')
            .should('be.visible')

        // Server-side ACL reflects the grant.
        cy.apollo({query: getManageRolesSettings, variables: {siteKey}})
            .its('data.manageRolesSettings.roles')
            .should((roles: Array<{ role: string; members: Array<{ name: string }> }>) => {
                const admin = roles.find(r => r.role === 'store-administrator')
                expect(admin, 'store-administrator role present').to.not.be.undefined
                expect(admin!.members.map(m => m.name)).to.include('root')
            })
    })
})
