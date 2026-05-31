import {DocumentNode} from 'graphql'
import {createSite, deleteSite, setNodeProperty} from '@jahia/cypress'

/**
 * Prepackaged site (store-template JS module).
 *
 * Verifies the template set's settings/import.xml: creating a site with the
 * store-template template set — with NO manual seeding — yields a working store
 * entirely on the JS templates: a home page rendering the modules grid, a
 * "My modules" page, a site-administration page, and a modules-repository
 * folder. Then a published module dropped into that repository shows on home.
 *
 * Requires the JS build of store-template.
 */
describe('Prepackaged store site (JS module)', () => {
    const siteKey = 'prepkg'
    const repo = `/sites/${siteKey}/contents/modules-repository`
    const homeRender = `/cms/render/default/en/sites/${siteKey}/home.html`

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')

    const islandBundle = '/modules/store-template/dist/client/admin/AdminApp.client.tsx.js'

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then((res) => {
            if (res.status !== 200) {
                cy.log('store-template JS module not deployed — skipping prepackaged spec')
                this.skip()
            }
        })
        cy.login()
        try {
            deleteSite(siteKey)
        } catch {
            // ignore
        }
        // No content seeding here — the template set's import.xml provides it.
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'prepkg.local',
            locale: 'en'
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    beforeEach(() => {
        cy.login()
    })

    it('provisions a working home page (chrome + modules list) from import.xml', () => {
        cy.visit(homeRender)
        cy.get('header').should('be.visible')
        cy.contains('footer', /all rights reserved/i).should('exist')
        // The seeded forgeModulesList rendered (empty until a module is published).
        cy.contains(/no published modules/i).should('be.visible')
    })

    it('provisions the My modules and Administration sub-pages', () => {
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`)
        cy.get('form[action$="createEntryFromJar.do"]').should('exist')

        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/administration.html`)
        cy.get('[role="tab"]', {timeout: 20000}).should('have.length', 3)
    })

    it('shows a published module dropped into the imported modules-repository', () => {
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'hello', title: 'Hello Module'}
        })
        setNodeProperty(`${repo}/hello`, 'published', 'true', 'en')

        cy.visit(homeRender)
        cy.contains('[data-forge-card]', 'Hello Module').should('be.visible')
    })
})
