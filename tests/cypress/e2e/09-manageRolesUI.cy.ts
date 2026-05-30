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
 *
 * Note: ManageRoles fetches ACL data on mount which is heavier than the
 * other admin routes, hence the larger initial timeout.
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
        // Route key is "storeRoles" — see ManageRoles/register.jsx. Using
        // "manageRoles" lands on Jahia's generic admin shell with a blank
        // right pane (200 status, but no route mount).
        cy.visit(`/jahia/administration/${siteKey}/storeRoles`)

        // The role-name h3 is the most reliable "ready" signal — it depends
        // on the ACL fetch completing.
        cy.contains('h3', /administrator/i, {timeout: 60000}).should('be.visible')

        // Keep the whole interaction scoped to the administrator section.
        // Cypress's default `cy.contains('li', 'root')` would otherwise match
        // the global nav <li> labelled "root" and navigate to /jahia/profile.
        cy.contains('section', /administrator/i).within(() => {
            // i18n key 'roles.addMember' renders as "Grant role to user or group" in EN.
            cy.contains('button', /grant role/i).click()
            cy.get('[id^=search-]').find('input').type('root')
            cy.contains('button', /^Search$/i).click()

            // Click the search result li inside this section — NOT the
            // sidebar nav li.
            cy.contains('li', 'root', {timeout: 15000}).click()
        })

        // After granting, the same section's member list contains "root".
        cy.contains('section', /administrator/i)
            .find('li')
            .contains('root')
            .should('be.visible')

        cy.apollo({query: getManageRolesSettings, variables: {siteKey}})
            .its('data.manageRolesSettings.roles')
            .should((roles: Array<{ role: string; members: Array<{ name: string }> }>) => {
                const admin = roles.find(r => r.role === 'store-administrator')
                expect(admin, 'store-administrator role present').to.not.be.undefined
                expect(admin!.members.map(m => m.name)).to.include('root')
            })
    })
})
