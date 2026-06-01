import {DocumentNode} from 'graphql'
import {createSite, deleteSite, setNodeProperty, uploadFile} from '@jahia/cypress'

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
        // The store-template import.xml provides modules-repository + the
        // home/my-modules page (which hosts the JAR upload form).
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

    it('displays reviews and the average rating', () => {
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: `${repo}/widget/reviews`,
                name: 'r1',
                primaryNodeType: 'jnt:review',
                properties: [
                    {name: 'rating', value: '5'},
                    {name: 'jcr:title', value: 'Excellent module', language: 'en'},
                    {name: 'content', value: 'Works great for us.', language: 'en'}
                ]
            }
        })
        cy.visit(moduleRender)
        cy.contains('h2', /reviews/i).should('be.visible')
        cy.contains('Excellent module').should('be.visible')
        cy.contains('Works great for us.').should('be.visible')
    })

    it('renders the JAR upload form wired to the createEntryFromJar action', () => {
        cy.visit(`/cms/render/default/en/sites/${siteKey}/home/my-modules.html`)
        // The upload form is a client island that posts via XHR (so Jahia's CsrfGuard
        // attaches its token — a plain <form> POST is rejected). The createEntryFromJar
        // action URL now rides in the island's hydration props, not a <form action>.
        cy.get('input[type="file"][name="file"]').should('exist')
        cy.get('[data-upload-ready="true"]', {timeout: 20000}).should('exist')
        cy.get('body').should(($b) => expect($b.html()).to.contain('createEntryFromJar.do'))
    })

    it('owner can reorder and delete screenshots (jcr mutations)', () => {
        // Two screenshots in the module's (autocreated) screenshots node.
        uploadFile('../../assets/screenshot.png', `${repo}/widget/screenshots`, 'shot-a.png', 'image/png')
        uploadFile('../../assets/screenshot.png', `${repo}/widget/screenshots`, 'shot-b.png', 'image/png')

        cy.intercept('POST', '/modules/graphql').as('gql')
        cy.visit(moduleRender)
        cy.get('[data-screenshots-ready]', {timeout: 20000})
        cy.get('[data-screenshot-name]').then(($els) => {
            expect($els.eq(0).attr('data-screenshot-name')).to.eq('shot-a.png')
            expect($els.eq(1).attr('data-screenshot-name')).to.eq('shot-b.png')
        })

        // Move the first screenshot down -> order becomes b, a (persisted via reorderChildren).
        cy.get('[data-screenshot-name="shot-a.png"]').find('button[aria-label="Move down"]').click()
        cy.wait('@gql')
        cy.reload()
        cy.get('[data-screenshots-ready]', {timeout: 20000})
        cy.get('[data-screenshot-name]').first().should('have.attr', 'data-screenshot-name', 'shot-b.png')

        // Delete the (now first) screenshot -> only shot-a.png remains.
        cy.get('[data-screenshot-name="shot-b.png"]').find('button[aria-label="Delete screenshot"]').click()
        cy.wait('@gql')
        cy.reload()
        cy.get('[data-screenshots-ready]', {timeout: 20000})
        cy.get('[data-screenshot-name="shot-b.png"]').should('not.exist')
        cy.get('[data-screenshot-name="shot-a.png"]').should('exist')
    })
})
