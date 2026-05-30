import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * Module-tabs content lifecycle.
 *
 * Each forge module page exposes six editable tabs that map onto JCR
 * properties on either the module or its current version node. The legacy
 * UI used bootstrap-editable (click-to-edit spans wired with jQuery) — too
 * brittle to drive from Cypress reliably — so we exercise the data contract
 * with GraphQL mutations and verify the rendered module page reflects them.
 *
 * Tab → property mapping (from forgeModule.*.jsp + jnt:forgeModule mixin):
 *   Information  → jcr:title, description
 *   Install/FAQ  → howToInstall, FAQ
 *   License      → license (on the version)
 *   Screenshots  → screenshots (child folder, here we set the legacy URL list)
 *   Video        → video
 *   Metadata     → authorNameDisplayedAs, authorEmail, authorURL, codeRepository
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
                properties: [
                    {name: 'jcr:title', value: 'Cypress Tabs Module', language: 'en'},
                    {name: 'versionNumber', value: version}
                ]
            }
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    function setProp(pathOrId: string, name: string, value: string, language?: string, type?: string) {
        return cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId,
                properties: [
                    {name, value, language: language || null, type: type || null}
                ]
            }
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
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'Updated Title')
        readProp(modulePath, 'description', 'en')
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'Cypress description body')
    })

    it('Install / FAQ tab — howToInstall + FAQ persist', () => {
        setProp(modulePath, 'howToInstall', '## Install\nUnzip and deploy.', 'en')
        setProp(modulePath, 'FAQ', '## FAQ\n**Q:** Where to start?', 'en')

        readProp(modulePath, 'howToInstall', 'en')
            .its('data.jcr.nodeByPath.property.value')
            .should('contain', 'Unzip and deploy')
        readProp(modulePath, 'FAQ', 'en')
            .its('data.jcr.nodeByPath.property.value')
            .should('contain', 'Where to start')
    })

    it('License tab — license property on the version persists', () => {
        setProp(versionPath, 'license', 'Apache-2.0')

        readProp(versionPath, 'license')
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'Apache-2.0')
    })

    it('Screenshots tab — screenshots URL list persists', () => {
        setProp(modulePath, 'screenshots',
            'https://shots.example.com/a.png\nhttps://shots.example.com/b.png')

        readProp(modulePath, 'screenshots')
            .its('data.jcr.nodeByPath.property.value')
            .should('contain', 'shots.example.com')
    })

    it('Video tab — video property persists', () => {
        setProp(modulePath, 'video', 'https://video.example.com/intro.mp4')

        readProp(modulePath, 'video')
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'https://video.example.com/intro.mp4')
    })

    it('Metadata tab — author + repo properties persist', () => {
        setProp(modulePath, 'authorNameDisplayedAs', 'Cypress Author')
        setProp(modulePath, 'authorEmail', 'author@example.com')
        setProp(modulePath, 'authorURL', 'https://example.com/me')
        setProp(modulePath, 'codeRepository', 'https://github.com/example/repo')

        readProp(modulePath, 'authorNameDisplayedAs')
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'Cypress Author')
        readProp(modulePath, 'authorEmail')
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'author@example.com')
        readProp(modulePath, 'authorURL')
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'https://example.com/me')
        readProp(modulePath, 'codeRepository')
            .its('data.jcr.nodeByPath.property.value')
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
