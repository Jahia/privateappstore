import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * Live-UI test for the React categorySettings admin route.
 *
 * Drives the three top-level interactions: wire a root category, add a child,
 * fill in per-language titles. The root category itself is seeded directly via
 * GraphQL (jnt:category under the site root) — that part is infrastructure,
 * not what the admin would do, so it stays outside the UI flow.
 */
describe('Category settings — live UI', () => {
    const siteKey = 'categorySettingsUiSite'
    const rootCategoryName = 'cy-ui-root-categories'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNode: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeForTest.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getCategorySettings: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/query/getCategorySettings.graphql')

    let rootCategoryUuid: string

    before(() => {
        cy.login()
        try {
            deleteSite(siteKey)
        } catch {
            // ignore
        }
        createSite(siteKey, {
            languages: 'en,fr',
            templateSet: 'store-template',
            serverName: 'localhost',
            locale: 'en'
        })

        cy.apollo({
            mutation: addNode,
            variables: {
                parentPath: `/sites/${siteKey}`,
                name: rootCategoryName,
                primaryNodeType: 'jnt:category'
            }
        }).then((result) => {
            rootCategoryUuid = (result.data as { jcr: { addNode: { node: { uuid: string } } } })
                .jcr.addNode.node.uuid
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('configures a root, adds a category, and persists per-language titles', () => {
        cy.login()
        cy.visit(`/jahia/administration/${siteKey}/categorySettings`)

        // Wire the root category by pasting the UUID and hitting Save.
        cy.get('#root-category-uuid', {timeout: 30000})
            .clear()
            .type(rootCategoryUuid)
        cy.contains('button', /^Save$/i).click()

        // Once the root is set, the "Add" section appears.
        cy.get('#new-category-name', {timeout: 15000})
            .clear()
            .type('ui-portlets')
        cy.contains('button', /^Add$/i).click()

        // Auto-opened editor — fill EN + FR titles.
        cy.get('#title-en', {timeout: 15000}).clear().type('Portlets EN')
        cy.get('#title-fr').clear().type('Portlets FR')
        cy.contains('button', /^Save$/i).click()

        // Server-side state matches.
        cy.apollo({query: getCategorySettings, variables: {siteKey}})
            .its('data.forgeCategorySettings.categories')
            .should((cats: Array<{ titles: Array<{ language: string; title: string }> }>) => {
                expect(cats).to.have.length(1)
                const titles = Object.fromEntries(cats[0].titles.map(t => [t.language, t.title]))
                expect(titles.en).to.equal('Portlets EN')
                expect(titles.fr).to.equal('Portlets FR')
            })
    })
})
