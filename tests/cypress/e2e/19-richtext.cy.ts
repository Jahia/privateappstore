import {DocumentNode} from 'graphql'
import {createSite, deleteSite, setNodeProperty} from '@jahia/cypress'

/**
 * Rich-text editing for the store-template JS module.
 *
 * The richtext metadata fields (description, how-to-install, FAQ, license) are
 * edited in-site with the RichTextEditor, and the HTML is sanitized with DOMPurify
 * on save before it is persisted and rendered (defense-in-depth against stored XSS).
 *
 * (This spec previously also covered the user-review feature, which has been
 * removed.) Requires the JS build of store-template.
 */
describe('Rich text editing (JS module)', () => {
    const siteKey = 'features'
    const repo = `/sites/${siteKey}/contents/modules-repository`
    const moduleEdit = `/cms/render/default/en${repo}/demo.html`

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')

    const islandBundle = '/modules/store-template/dist/client/components/forge/ModuleEditor.client.tsx.js'

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then((res) => {
            if (res.status !== 200) {
                cy.log('store-template editor island not deployed — skipping')
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
            serverName: 'features.local',
            locale: 'en'
        })
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'demo', title: 'Demo Module'}
        })
        setNodeProperty(`${repo}/demo`, 'description', '<p>Original description.</p>', 'en')
        setNodeProperty(`${repo}/demo`, 'published', 'true', 'en')
    })

    after(() => {
        deleteSite(siteKey)
    })

    beforeEach(() => {
        cy.login()
    })

    it('edits a richtext field with the rich-text editor and sanitizes the saved HTML', () => {
        cy.login()
        cy.visit(moduleEdit)
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()

        // The description field is now a contenteditable rich-text editor with a toolbar.
        cy.get('#edit-description', {timeout: 10000}).should('have.attr', 'contenteditable', 'true')
        cy.get('button[title="Bold"]').should('exist')
        cy.get('button[title="Link"]').should('exist')

        // Inject safe formatting plus a script and an onerror handler, then notify React.
        cy.get('#edit-description').then(($el) => {
            const el = $el[0] as HTMLElement
            el.innerHTML =
                '<h2>Injected heading</h2><p>Body text.</p>' +
                '<script>window.__xss = 1;</script>' +
                '<img src="x" onerror="window.__xss = 2;">'
            el.dispatchEvent(new InputEvent('input', {bubbles: true}))
        })

        cy.contains('button', /^Save$/).click()
        cy.contains(/saved/i, {timeout: 20000}).should('be.visible')
        cy.reload()

        // Safe formatting persisted and rendered.
        cy.contains('h2', 'Injected heading', {timeout: 20000}).should('be.visible')
        // Sanitized: the dangerous markup is gone and nothing executed.
        cy.contains('h2', 'Injected heading')
            .parents('section')
            .first()
            .then(($s) => {
                const html = $s.html().toLowerCase()
                expect(html, 'no inline event handler').not.to.contain('onerror')
                expect(html, 'no script element').not.to.contain('<script')
            })
        cy.get('img[onerror]').should('not.exist')
        cy.window().then((win) => {
            expect((win as unknown as {__xss?: number}).__xss).to.be.undefined
        })
    })
})
