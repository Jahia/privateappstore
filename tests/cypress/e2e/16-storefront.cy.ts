import {DocumentNode} from 'graphql'
import {createSite, deleteSite, setNodeProperty} from '@jahia/cypress'

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

    const islandBundle = '/modules/store-template/dist/client/admin/AdminApp.client.tsx.js'

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
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'storefront.local',
            locale: 'en'
        })

        addNode(contents, 'modules-repository', 'jnt:contentFolder')

        // Published module with a version
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

        // Unpublished draft — must NOT appear in the grid
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'draft', title: 'Draft Module'}
        })

        // Home page hosting a forge modules list
        addNode(`/sites/${siteKey}`, 'home', 'jnt:page', [{name: 'j:templateName', value: 'default'}])
        addNode(`/sites/${siteKey}/home`, 'main', 'jnt:contentList')
        addNode(`/sites/${siteKey}/home/main`, 'list', 'jnt:forgeModulesList')
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
        cy.contains('Real-time charts').should('be.visible')
        cy.contains('Supported').should('be.visible')
        cy.contains('Draft Module').should('not.exist')
    })

    it('opens a module detail page with version + download', () => {
        cy.visit(detailRender)
        cy.contains('h1', 'Analytics Dashboard').should('be.visible')
        cy.contains('Versions').should('be.visible')
        cy.contains('1.0.0').should('be.visible')
        cy.contains('a', 'Download')
            .should('have.attr', 'href')
            .and('contain', 'analytics-1.0.0.jar')
    })
})
