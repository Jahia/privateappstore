import {DocumentNode} from 'graphql'
import {createSite, deleteSite, setNodeProperty} from '@jahia/cypress'

/**
 * Authoring views (store-template JS module) — Phase 3.
 *
 * Covers in-site editing of module metadata via the ModuleEditor island, which
 * saves through the generic jcr GraphQL mutations (session-authenticated, JCR
 * ACLs apply — no custom Java action).
 *
 * Requires the JS build of store-template.
 */
describe('Authoring views (JS module)', () => {
    const siteKey = 'authoring'
    const repo = `/sites/${siteKey}/contents/modules-repository`
    const moduleRender = `/cms/render/default/en${repo}/widget.html`

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql')

    const islandBundle = '/modules/store-template/dist/client/admin/AdminApp.client.tsx.js'

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then((res) => {
            if (res.status !== 200) {
                cy.log('store-template JS module not deployed — skipping authoring spec')
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
            serverName: 'authoring.local',
            locale: 'en'
        })
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {parentPath: `/sites/${siteKey}/contents`, name: 'modules-repository', primaryNodeType: 'jnt:contentFolder', properties: []}
        })
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'widget', title: 'Widget'}
        })
        setNodeProperty(`${repo}/widget`, 'description', '<p>Original description.</p>', 'en')
        setNodeProperty(`${repo}/widget`, 'published', 'true', 'en')
    })

    after(() => {
        deleteSite(siteKey)
    })

    beforeEach(() => {
        cy.login()
    })

    it('shows the in-site editor to a user with write permission', () => {
        cy.visit(moduleRender)
        cy.contains('button', /edit module/i, {timeout: 20000}).should('be.visible')
    })

    it('edits module metadata and persists via GraphQL', () => {
        cy.visit(moduleRender)
        // Wait for the editor island to hydrate before clicking (the SSR button
        // is visible before its handler is attached).
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()
        cy.get('#edit-jcr-title', {timeout: 10000}).should('have.value', 'Widget').clear().type('Widget Pro')
        cy.get('#edit-codeRepository').clear().type('https://github.com/acme/widget')
        cy.contains('button', /^Save$/).click()
        // Deterministic success state (no auto-reload race), then a
        // Cypress-controlled reload to confirm persistence.
        cy.contains(/saved/i, {timeout: 20000}).should('be.visible')
        cy.reload()
        cy.get('h1', {timeout: 20000}).should('contain.text', 'Widget Pro')
        cy.contains('a', 'https://github.com/acme/widget').should('exist')
    })
})
