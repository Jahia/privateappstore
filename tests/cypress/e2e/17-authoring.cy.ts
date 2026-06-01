import {DocumentNode} from 'graphql'
import {createSite, deleteSite, setNodeProperty, uploadFile} from '@jahia/cypress'

/**
 * Click the editor's Save and wait for the full-page reload it triggers (the
 * editor reloads so the SSR'd values refresh without a manual refresh). Stamping
 * `window` before the click and waiting for the stamp to vanish is reload-safe —
 * a window object doesn't "detach" like a DOM element would mid-navigation, so no
 * assertion ends up straddling the reload.
 */
const saveAndWaitReload = (saveLabel: RegExp = /^Save$/): void => {
    cy.window().then((w) => {
        ;(w as unknown as {__preSave?: boolean}).__preSave = true
    })
    cy.contains('button', saveLabel).click()
    cy.window({timeout: 20000}).should((w) => {
        expect((w as unknown as {__preSave?: boolean}).__preSave).to.be.undefined
    })
}

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

    const islandBundle = '/modules/store-template/dist/client/components/forge/ModuleEditor.client.tsx.js'

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
        // An unpublished version, so the per-version publish control has something
        // to flip (the module's own published flag starts true).
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: `${repo}/widget`,
                name: 'widget-1.0.0',
                primaryNodeType: 'jnt:forgeModuleVersion',
                properties: [
                    {name: 'versionNumber', value: '1.0.0'},
                    {name: 'published', value: 'false'}
                ]
            }
        })

        // A second module with TWO versions, dedicated to the delete-version test —
        // so widget keeps exactly one version (the publish-control test relies on a
        // single [data-forge-version] being unambiguous).
        cy.apollo({
            mutation: createForgeModule,
            variables: {parentPath: repo, name: 'multiver', title: 'Multi Version'}
        })
        setNodeProperty(`${repo}/multiver`, 'published', 'true', 'en')
        ;[
            {name: 'multiver-1.0.0', num: '1.0.0'},
            {name: 'multiver-2.0.0', num: '2.0.0'}
        ].forEach((v) => {
            cy.apollo({
                mutation: addNodeWithProps,
                variables: {
                    parentPath: `${repo}/multiver`,
                    name: v.name,
                    primaryNodeType: 'jnt:forgeModuleVersion',
                    properties: [
                        {name: 'versionNumber', value: v.num},
                        {name: 'published', value: 'true'}
                    ]
                }
            })
        })
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

    it('edits module metadata across tabs and persists via GraphQL', () => {
        cy.visit(moduleRender)
        // Wait for the editor island to hydrate before clicking (the SSR button
        // is visible before its handler is attached).
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()
        // General tab is active by default — the title lives here.
        cy.get('#edit-jcr-title', {timeout: 10000}).should('have.value', 'Widget').clear().type('Widget Pro')
        // Code repository is on the Author & links tab.
        cy.contains('[role="tab"]', /author/i).click()
        cy.get('#edit-codeRepository').clear().type('https://github.com/acme/widget')
        // Saving reloads the page so the SSR'd values refresh immediately (no manual
        // refresh); wait for that reload, then assert the persisted values.
        saveAndWaitReload()
        cy.get('h1', {timeout: 20000}).should('contain.text', 'Widget Pro')
        cy.contains('a', 'https://github.com/acme/widget').should('exist')
    })

    it('edits status + tags on the General tab and persists them', () => {
        cy.visit(moduleRender)
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()
        // Status (choicelist) + tags (j:tagList) are on the General tab. They use
        // the same non-i18n setValue / multi-value setValues paths as category.
        cy.get('#edit-status', {timeout: 10000}).select('legacy')
        cy.get('#edit-tags').type('analytics{enter}charts{enter}')
        // Wait for the save-triggered reload before asserting — so /legacy/ matches
        // the rendered status badge on the fresh page, not the (now-gone) open
        // <select>'s <option>.
        saveAndWaitReload()
        cy.contains(/legacy/i, {timeout: 20000}).should('be.visible')
        cy.get('[data-tag-list]').should('contain.text', 'analytics').and('contain.text', 'charts')
    })

    it('keyboard-navigates the editor tabs (roving tabindex)', () => {
        cy.visit(moduleRender)
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()
        // Scope to the editor's tablist ("Module fields") — the detail page now also
        // has a section tablist ("Module sections"), so a bare [role=tab] is ambiguous.
        cy.get('[aria-label="Module fields"]', {timeout: 10000}).as('editorTabs')
        // The active tab is the only one in the tab order; ArrowRight moves + selects.
        cy.get('@editorTabs').find('[role="tab"][aria-selected="true"]').should('contain.text', 'General')
        cy.get('@editorTabs').find('[role="tab"][aria-selected="true"]').type('{rightarrow}')
        cy.get('@editorTabs').find('[role="tab"][aria-selected="true"]').should('contain.text', 'Description')
        cy.get('#panel-description').should('have.attr', 'aria-labelledby', 'tab-description')
    })

    it('mounts CKEditor 5 (from richtext-ckeditor5) for richtext fields', () => {
        cy.visit(moduleRender)
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()
        cy.contains('[role="tab"]', /^Description$/).click()
        // The federated CKEditor build is loaded from the deployed richtext-ckeditor5
        // module and instantiated on the live page (allow time for the remote fetch).
        cy.get('[data-ckeditor-state="ready"]', {timeout: 30000}).should('exist')
        cy.get('.ck-editor__editable', {timeout: 10000}).should('be.visible')
        cy.get('.ck-toolbar button').its('length').should('be.greaterThan', 0)
        // The federated `.` entry ships no CSS, so the storefront injects the
        // matching CKEditor 5 stylesheet itself — without it the editor is unstyled.
        cy.get('head style[data-ckeditor5-styles]').should('exist')
    })

    it('uploads a module icon (GraphQL multipart) and stores real image bytes', () => {
        cy.visit(moduleRender)
        cy.get('[data-editor-ready]', {timeout: 20000})
        cy.contains('button', /edit module/i).click()
        // Icon upload lives on the (default) General tab.
        cy.get('[data-icon-input]', {timeout: 10000}).selectFile('assets/icon.png', {force: true})
        cy.contains('button', /upload icon/i).click()
        cy.get('[data-icon-status="uploaded"]', {timeout: 20000}).should('exist')
        // Persisted: the module header renders an icon <img> after reload (the
        // placeholder is a <span>, so its presence proves the file node was created).
        cy.reload()
        cy.get('header img', {timeout: 20000}).should('have.attr', 'src').and('match', /\S/)
        // Guard the multipart-upload bug: the stored binary must be the real PNG,
        // not a stringified servlet Part ("org.apache.catalina...ApplicationPart@…",
        // which was served as image/png but only ~49 bytes of ASCII).
        cy.get('header img').invoke('attr', 'src').then((src) => {
            cy.request({url: src as string, encoding: 'binary'}).then((resp) => {
                expect(resp.status).to.eq(200)
                // Real image bytes — the corrupt value was a ~49-byte ASCII string.
                expect(resp.body.length, 'icon byte size').to.be.greaterThan(100)
                expect(resp.body, 'not a stringified servlet Part')
                    .not.to.contain('ApplicationPart')
            })
        })
    })

    it('owner publishes / unpublishes the module via the publish control', () => {
        cy.visit(moduleRender)
        // widget was seeded published=true; the control reflects it once hydrated.
        cy.get('[data-publish-scope="module"][data-publish-ready="true"]', {timeout: 20000})
            .should('have.attr', 'data-published', 'true')
        cy.get('[data-publish-scope="module"] button').click()
        cy.get('[data-publish-scope="module"]').should('have.attr', 'data-published', 'false')
        // Persisted (setValue on the rendered workspace), survives reload.
        cy.reload()
        cy.get('[data-publish-scope="module"][data-publish-ready="true"]', {timeout: 20000})
            .should('have.attr', 'data-published', 'false')
    })

    it('owner publishes a specific version via the version publish control', () => {
        // Version controls live in the Versions tab.
        const openVersionsTab = () => {
            cy.get('[data-detail-tabs-ready]', {timeout: 20000})
            cy.contains('[role="tab"]', /versions/i).click()
        }
        cy.visit(moduleRender)
        openVersionsTab()
        // The seeded version starts unpublished.
        cy.get('[data-forge-version] [data-publish-scope="version"][data-publish-ready="true"]', {timeout: 20000})
            .should('have.attr', 'data-published', 'false')
        cy.get('[data-forge-version] [data-publish-scope="version"] button').click()
        cy.get('[data-forge-version] [data-publish-scope="version"]').should('have.attr', 'data-published', 'true')
        cy.reload()
        openVersionsTab()
        cy.get('[data-forge-version] [data-publish-scope="version"][data-publish-ready="true"]', {timeout: 20000})
            .should('have.attr', 'data-published', 'true')
    })

    it('owner removes a version via the version delete control (with confirm)', () => {
        const multiRender = `/cms/render/default/en${repo}/multiver.html`
        const openVersionsTab = () => {
            cy.get('[data-detail-tabs-ready]', {timeout: 20000})
            cy.contains('[role="tab"]', /versions/i).click()
        }
        cy.visit(multiRender)
        openVersionsTab()
        // Both seeded versions are listed.
        cy.contains('[data-forge-version]', '2.0.0').should('be.visible')
        cy.contains('[data-forge-version]', '1.0.0').should('be.visible')

        // In the 1.0.0 card: open the inline confirm, then confirm the deletion. A
        // successful delete reloads the page, so stamp window before the confirm
        // click and wait for the stamp to vanish (reload-safe, like saveAndWaitReload).
        const v1 = () => cy.contains('[data-forge-version]', '1.0.0')
        v1().find('[data-version-delete-scope="version"][data-version-delete-ready="true"]', {timeout: 20000})
            .should('exist')
        v1().contains('button', /remove version/i).click()
        cy.window().then((w) => {
            ;(w as unknown as {__preDelete?: boolean}).__preDelete = true
        })
        v1().contains('button', /^Delete$/).click()
        cy.window({timeout: 20000}).should((w) => {
            expect((w as unknown as {__preDelete?: boolean}).__preDelete).to.be.undefined
        })

        // After the reload: 2.0.0 remains, 1.0.0 is gone.
        openVersionsTab()
        cy.contains('[data-forge-version]', '2.0.0').should('be.visible')
        cy.get('[data-forge-version]').should('have.length', 1)
        cy.contains('[data-forge-version]', '1.0.0').should('not.exist')
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

    it('owner can add a version from the module page (upload form in the Versions tab)', () => {
        cy.visit(moduleRender)
        cy.get('[data-detail-tabs-ready]', {timeout: 20000})
        cy.contains('[role="tab"]', /versions/i).click()
        // The owner-only "Upload a new version" form lives in the Versions tab and
        // posts to the SAME createEntryFromJar action (which upserts: it appends the
        // version to the existing module). A real upload needs a Maven repo, so —
        // like the my-modules upload test — assert the form is present and wired.
        cy.get('[data-add-version]', {timeout: 20000}).should('be.visible')
        cy.get('[data-add-version] input[type="file"][name="file"]').should('exist')
        cy.get('[data-add-version] [data-upload-ready="true"]', {timeout: 20000}).should('exist')
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
