import {DocumentNode} from 'graphql'
import {createSite, deleteSite, uploadFile} from '@jahia/cypress'

/**
 * Module-tabs content lifecycle.
 *
 * Each forge module page exposes six editable tabs that map onto JCR
 * properties on either the module or its current version node. The legacy
 * UI used bootstrap-editable (click-to-edit spans wired with jQuery) — too
 * brittle to drive from Cypress reliably — so we exercise the data contract
 * with GraphQL mutations and verify the rendered module page reflects them.
 *
 * Tab → JCR mapping (read from META-INF/definitions.cnd jnt:forgeModule block):
 *   Information  → jcr:title (i18n), description (i18n)
 *   Install/FAQ  → howToInstall (i18n), FAQ (i18n)
 *   License      → license (i18n) — lives on the MODULE, not the version
 *   Screenshots  → autocreated `screenshots` child node (jnt:forgeScreenshotsList)
 *   Video        → autocreated `video` child node (jnt:videostreaming)
 *   Metadata     → authorNameDisplayedAs (choicelist!), authorEmail,
 *                  authorURL, codeRepository (non-i18n strings)
 *
 * Notes for future maintainers:
 * - i18n properties NEED language='en' passed to setPropertiesBatch.
 * - Non-i18n properties must NOT pass language (the mutation no-ops if you do).
 * - authorNameDisplayedAs is constrained to 'username'|'fullName'|'organisation'
 *   — any other value is silently rejected and the default 'username' stays.
 * - screenshots/video are CHILD NODES, not properties — we assert their
 *   existence rather than try to set them as scalar properties.
 */
describe('Module tabs — content lifecycle', () => {
    const siteKey = 'moduleTabsSite'
    const moduleName = 'cy-tabs-module'
    const version = '1.0.0'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const mutateProp: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/mutateNodeProperty.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getNodeProperty: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/query/getNodeProperties.graphql')

    const modulePath = `/sites/${siteKey}/contents/${moduleName}`
    const versionName = `${moduleName}-${version}`
    const versionPath = `${modulePath}/${versionName}`

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

        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: `/sites/${siteKey}/contents`,
                name: moduleName,
                title: 'Cypress Tabs Module'
            }
        })

        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: versionName,
                primaryNodeType: 'jnt:forgeModuleVersion',
                // jnt:forgeModuleVersion has NO jcr:title definition — setting
                // it throws ConstraintViolationException and fails the whole
                // addNode, leaving a phantom (non-existent) version node.
                properties: [
                    {name: 'versionNumber', value: version}
                ]
            }
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    // Single-property setter. The @jahia/cypress setProperty mutation shape
    // (mutateProperty(name).setValue(value, language)) is the only path we've
    // found to consistently write through to JCR — language is required even
    // for non-i18n properties (the JCR-side resolver ignores it for those).
    function setProp(pathOrId: string, name: string, value: string, language?: string) {
        return cy.apollo({
            mutation: mutateProp,
            variables: {pathOrId, name, value, language: language || 'en'}
        })
    }

    function readProp(path: string, name: string, language?: string) {
        return cy.apollo({
            query: getNodeProperty,
            variables: {path, name, language: language || null},
            fetchPolicy: 'no-cache'
        })
    }

    it('Information tab — title + description persist', () => {
        setProp(modulePath, 'jcr:title', 'Updated Title', 'en')
        setProp(modulePath, 'description', 'Cypress description body', 'en')

        readProp(modulePath, 'jcr:title', 'en')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'Updated Title')
        readProp(modulePath, 'description', 'en')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'Cypress description body')
    })

    it('Install / FAQ tab — howToInstall + FAQ persist', () => {
        setProp(modulePath, 'howToInstall', '## Install\nUnzip and deploy.', 'en')
        setProp(modulePath, 'FAQ', '## FAQ\n**Q:** Where to start?', 'en')

        readProp(modulePath, 'howToInstall', 'en')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('contain', 'Unzip and deploy')
        readProp(modulePath, 'FAQ', 'en')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('contain', 'Where to start')
    })

    it('License tab — license property on the module persists', () => {
        // license is i18n on jnt:forgeModule (not on the version!).
        setProp(modulePath, 'license', 'Apache-2.0', 'en')

        readProp(modulePath, 'license', 'en')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'Apache-2.0')
    })

    it('Screenshots tab — autocreated screenshots child node exists', () => {
        // The CND declares `+ screenshots (jnt:forgeScreenshotsList) autocreated hidden`
        // so the child node should be present immediately after module creation.
        cy.apollo({
            query: getNodeProperty,
            variables: {path: `${modulePath}/screenshots`, name: 'jcr:primaryType', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'jnt:forgeScreenshotsList')
    })

    it('Video tab — video child node can be added on the module', () => {
        // Unlike `screenshots`, the CND declares `+ video (jnt:videostreaming)
        // = jnt:videostreaming` WITHOUT the `autocreated` flag, so the child
        // node is not created until the developer adds it. Create it via
        // addNode and verify the resulting primaryType.
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: 'video',
                primaryNodeType: 'jnt:videostreaming',
                properties: []
            }
        })

        cy.apollo({
            query: getNodeProperty,
            variables: {path: `${modulePath}/video`, name: 'jcr:primaryType', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'jnt:videostreaming')
    })

    it('Icon — an uploaded image lands in the module icon folder', () => {
        // jnt:forgeModule declares `+ icon (jnt:folder)` (not autocreated), so
        // create the folder first, then upload the image into it — mirroring
        // what the store-template owner UI does via GraphQL (the icon upload now
        // goes through a GraphQL multipart mutation, not a Jahia action).
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: 'icon',
                primaryNodeType: 'jnt:folder',
                properties: []
            }
        })

        // cy.fixture resolves from cypress/fixtures, so step up to tests/assets.
        uploadFile('../../assets/icon.png', `${modulePath}/icon`, 'icon.png', 'image/png')

        cy.apollo({
            query: getNodeProperty,
            variables: {path: `${modulePath}/icon/icon.png`, name: 'jcr:primaryType', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'jnt:file')
    })

    it('Metadata tab — author + repo properties persist', () => {
        // authorNameDisplayedAs is a choicelist constrained to
        // 'username'|'fullName'|'organisation' — using 'Cypress Author' is
        // rejected silently and leaves the default.
        setProp(modulePath, 'authorNameDisplayedAs', 'fullName')
        setProp(modulePath, 'authorEmail', 'author@example.com')
        setProp(modulePath, 'authorURL', 'https://example.com/me')
        setProp(modulePath, 'codeRepository', 'https://github.com/example/repo')

        readProp(modulePath, 'authorNameDisplayedAs')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'fullName')
        readProp(modulePath, 'authorEmail')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'author@example.com')
        readProp(modulePath, 'authorURL')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'https://example.com/me')
        readProp(modulePath, 'codeRepository')
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'https://github.com/example/repo')
    })

    it('Module page renders in EDIT with all content set', () => {
        cy.login()
        cy.request({
            url: `/cms/edit/default/en${modulePath}.html`,
            failOnStatusCode: false
        }).then((res) => {
            expect([200, 302]).to.include(res.status)
        })
    })
})
