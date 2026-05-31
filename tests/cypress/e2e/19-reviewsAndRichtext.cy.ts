import {DocumentNode} from 'graphql'
import {
    createSite,
    deleteSite,
    createUser,
    deleteUser,
    grantRoles,
    publishAndWaitJobEnding,
    setNodeProperty
} from '@jahia/cypress'

/**
 * Deferred-feature coverage for the store-template JS module:
 *
 *   1. Rich-text editing of richtext metadata fields (description, …) via the
 *      RichTextEditor, with DOMPurify sanitization on save.
 *   2. In-site review SUBMISSION through the privateappstore `submitReview` action
 *      — the form (XHR, so Jahia's CSRF guard attaches the token), the cross-owner
 *      case on the live site (a logged-in user reviewing a published module they
 *      do not own / cannot jcr:write), the one-review-per-user guard, and the
 *      guest rejection.
 *
 * Reviewing is an action (not a GraphQL mutation) because the Jahia GraphQL
 * endpoint is permission-gated and not reachable by ordinary users.
 *
 * Review *display* and plain-field editing are covered by 17-authoring; this spec
 * covers the parts deferred at cutover. Requires the JS build of store-template.
 */
describe('Reviews + rich text (JS module)', () => {
    const siteKey = 'features'
    const repo = `/sites/${siteKey}/contents/modules-repository`
    const moduleEdit = `/cms/render/default/en${repo}/demo.html`
    const moduleLive = `/cms/render/live/en${repo}/demo.html`
    const liveAction = `/cms/render/live/en${repo}/demo.SubmitReview.do`
    const reviewer = 'reviewer'
    const reviewerPwd = 'reviewer1234'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')

    const islandBundle = '/modules/store-template/dist/client/components/forge/ReviewForm.client.tsx.js'

    before(function () {
        cy.request({url: islandBundle, failOnStatusCode: false}).then((res) => {
            if (res.status !== 200) {
                cy.log('store-template review island not deployed — skipping')
                this.skip()
            }
        })
        cy.login()
        try {
            deleteSite(siteKey)
        } catch {
            // ignore
        }
        try {
            deleteUser(reviewer)
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
        // A regular user who can read the published (live) module but has no write
        // on it — proves the action's privileged write is what lets them review.
        createUser(reviewer, reviewerPwd)
        grantRoles(`/sites/${siteKey}`, ['reader'], reviewer, 'USER')
    })

    after(() => {
        deleteSite(siteKey)
        deleteUser(reviewer)
    })

    // ---------------------------------------------------------------- rich text
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

    // --------------------------------------------------------- review form (UI)
    it('owner submits a review through the form (XHR + CSRF token) and the one-per-user guard kicks in', () => {
        cy.login()
        cy.visit(moduleEdit)
        cy.get('form[data-review-ready]', {timeout: 20000})

        cy.get('[data-star="5"]').click()
        cy.get('#review-title').type('Great tool')
        cy.get('#review-comment').type('Saved us a lot of time.')
        cy.contains('button', /post review/i).click()
        cy.get('[data-review-done]', {timeout: 20000}).should('be.visible')

        cy.reload()
        // Persisted + displayed, and the form is replaced by the already-reviewed note.
        cy.contains('Great tool', {timeout: 20000}).should('be.visible')
        cy.get('[data-review-ready]').should('contain.text', 'already reviewed')
    })

    // ----------------------------------------------- cross-owner elevation (live)
    it('a non-owner user reviews a published module on the live site (privileged, attributed)', () => {
        // Publish so the non-owner can read the module on the live site.
        publishAndWaitJobEnding(`${repo}/demo`, ['en'])

        cy.login(reviewer, reviewerPwd)
        cy.visit(moduleLive)
        cy.get('form[data-review-ready]', {timeout: 20000})
        // No write on the module → no editor, but the review form is available.
        cy.contains('button', /edit module/i).should('not.exist')

        cy.get('[data-star="4"]').click()
        cy.get('#review-title').type('Really useful')
        cy.get('#review-comment').type('Solved my problem in minutes.')
        cy.contains('button', /post review/i).click()
        cy.get('[data-review-done]', {timeout: 20000}).should('be.visible')

        // One-per-user on reload.
        cy.reload()
        cy.get('[data-review-ready]').should('contain.text', 'already reviewed')

        // The owner sees the cross-owner review on the live site, attributed to the reviewer.
        cy.login()
        cy.visit(moduleLive)
        cy.contains('Really useful', {timeout: 20000}).should('be.visible')
        cy.contains('Solved my problem in minutes.').should('be.visible')
        cy.contains(reviewer).should('be.visible')
    })

    it('rejects an anonymous (guest) submission', () => {
        // Guests bypass the CSRF guard, so this reaches the action, which requires
        // an authenticated user and refuses.
        cy.clearCookies()
        cy.request({
            method: 'POST',
            url: liveAction,
            form: true,
            failOnStatusCode: false,
            body: {rating: 5, language: 'en'}
        }).then((res) => {
            expect(res.status, 'guest is unauthorized').to.be.within(401, 403)
        })
    })
})
