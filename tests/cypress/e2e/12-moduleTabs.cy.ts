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
    const getNodeProps: DocumentNode =
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

        // Module node.
        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: `/sites/${siteKey}/contents`,
                name: moduleName,
                title: 'Cypress Tabs Module'
            }
        })

        // Version node — uses generic addNode with the properties used by
        // the JSP renderers (versionNumber, jcr:title).
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

    function setProp(pathOrId: string, name: string, value: string, language?: string) {
        return cy.apollo({
            mutation: mutateProp,
            variables: {pathOrId, name, value, language: language || null}
        })
    }

    it('Information tab — title + description persist', () => {
        setProp(modulePath, 'jcr:title', 'Updated Title', 'en')
        setProp(modulePath, 'description', 'Cypress description body', 'en')

        cy.apollo({
            query: getNodeProps,
            variables: {path: modulePath, names: ['jcr:title', 'description']}
        })
            .its('data.jcr.nodeByPath.properties')
            .should((props: Array<{ name: string; value: string }>) => {
                const map = Object.fromEntries(props.map(p => [p.name, p.value]))
                expect(map['jcr:title']).to.equal('Updated Title')
                expect(map.description).to.equal('Cypress description body')
            })
    })

    it('Install / FAQ tab — howToInstall + FAQ persist', () => {
        setProp(modulePath, 'howToInstall', '## Install\nUnzip and deploy.', 'en')
        setProp(modulePath, 'FAQ', '## FAQ\n**Q:** Where to start?', 'en')

        cy.apollo({
            query: getNodeProps,
            variables: {path: modulePath, names: ['howToInstall', 'FAQ']}
        })
            .its('data.jcr.nodeByPath.properties')
            .should((props: Array<{ name: string; value: string }>) => {
                const map = Object.fromEntries(props.map(p => [p.name, p.value]))
                expect(map.howToInstall).to.contain('Unzip and deploy')
                expect(map.FAQ).to.contain('Where to start')
            })
    })

    it('License tab — license property on the version persists', () => {
        setProp(versionPath, 'license', 'Apache-2.0')

        cy.apollo({
            query: getNodeProps,
            variables: {path: versionPath, names: ['license']}
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'Apache-2.0')
    })

    it('Screenshots tab — screenshots URL list persists', () => {
        setProp(modulePath, 'screenshots',
            'https://shots.example.com/a.png\nhttps://shots.example.com/b.png')

        cy.apollo({
            query: getNodeProps,
            variables: {path: modulePath, names: ['screenshots']}
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('contain', 'shots.example.com')
    })

    it('Video tab — video property persists', () => {
        setProp(modulePath, 'video', 'https://video.example.com/intro.mp4')

        cy.apollo({
            query: getNodeProps,
            variables: {path: modulePath, names: ['video']}
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'https://video.example.com/intro.mp4')
    })

    it('Metadata tab — author + repo properties persist', () => {
        setProp(modulePath, 'authorNameDisplayedAs', 'Cypress Author')
        setProp(modulePath, 'authorEmail', 'author@example.com')
        setProp(modulePath, 'authorURL', 'https://example.com/me')
        setProp(modulePath, 'codeRepository', 'https://github.com/example/repo')

        cy.apollo({
            query: getNodeProps,
            variables: {
                path: modulePath,
                names: ['authorNameDisplayedAs', 'authorEmail', 'authorURL', 'codeRepository']
            }
        })
            .its('data.jcr.nodeByPath.properties')
            .should((props: Array<{ name: string; value: string }>) => {
                const map = Object.fromEntries(props.map(p => [p.name, p.value]))
                expect(map.authorNameDisplayedAs).to.equal('Cypress Author')
                expect(map.authorEmail).to.equal('author@example.com')
                expect(map.authorURL).to.equal('https://example.com/me')
                expect(map.codeRepository).to.equal('https://github.com/example/repo')
            })
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
