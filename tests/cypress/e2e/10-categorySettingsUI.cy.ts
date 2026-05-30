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

        // Moonstone Field puts the id on the wrapper div — drill into the
        // real <input> child element. Assert each input value propagated to
        // React state before clicking the buttons (which are isDisabled-gated
        // on trimmed input length); without the assertion the click races
        // the React re-render and lands on a disabled button.
        cy.get('#root-category-uuid input', {timeout: 60000})
            .clear()
            .type(rootCategoryUuid)
            .should('have.value', rootCategoryUuid)
        cy.contains('button', /^Save$/i).should('not.be.disabled').click()

        cy.get('#new-category-name input', {timeout: 15000})
            .clear()
            .type('ui-portlets')
            .should('have.value', 'ui-portlets')
        cy.contains('button', /^Add$/i).should('not.be.disabled').click()

        cy.get('#title-en input', {timeout: 15000})
            .clear()
            .type('Portlets EN')
            .should('have.value', 'Portlets EN')
        cy.get('#title-fr input')
            .clear()
            .type('Portlets FR')
            .should('have.value', 'Portlets FR')
        cy.contains('button', /^Save$/i).should('not.be.disabled').click()

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
