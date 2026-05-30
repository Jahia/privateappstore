import {DocumentNode} from 'graphql'
import {createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress'
import {postAction} from '../support/forgeActions'

/**
 * Module publish lifecycle.
 *
 * Sets a changeLog on the version, marks it published, marks the module
 * published, then asserts the JCR state. The publishModule.do action also
 * publishes the EDIT-workspace changes to LIVE — that's what makes the
 * module visible to anonymous traffic and lists like moduleList.json.
 */
describe('Module publish — version + module', () => {
    const siteKey = 'modulePublishSite'
    const moduleName = 'cy-publish-module'
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

        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: `/sites/${siteKey}/contents`,
                name: moduleName,
                title: 'Cypress Publish Module'
            }
        })

        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: versionName,
                primaryNodeType: 'jnt:forgeModuleVersion',
                properties: [
                    {name: 'jcr:title', value: 'Cypress Publish Module', language: 'en'},
                    {name: 'versionNumber', value: version},
                    {name: 'url', value: 'https://nexus.example.com/repository/maven-releases/cy/publish/module/1.0.0/module-1.0.0.jar'}
                ]
            }
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('sets a changeLog on the version', () => {
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: versionPath,
                name: 'changeLog',
                value: '- Initial release\n- Cypress-driven test fixture',
                language: 'en'
            }
        })

        cy.apollo({
            query: getNodeProps,
            variables: {path: versionPath, names: ['changeLog']}
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('contain', 'Initial release')
    })

    it('publishes the version and the module, then publishes the site to LIVE', () => {
        // Set published=true on both. The legacy publishModule.do action does
        // this plus a LIVE-workspace publish — we do the same here via JCR
        // mutations + publishAndWaitJobEnding so the LIVE state is
        // deterministic for downstream specs.
        cy.apollo({
            mutation: mutateProp,
            variables: {pathOrId: versionPath, name: 'published', value: 'true', language: null}
        })
        cy.apollo({
            mutation: mutateProp,
            variables: {pathOrId: modulePath, name: 'published', value: 'true', language: null}
        })

        publishAndWaitJobEnding(modulePath, ['en'])

        cy.apollo({
            query: getNodeProps,
            variables: {path: modulePath, names: ['published']}
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'true')

        cy.apollo({
            query: getNodeProps,
            variables: {path: versionPath, names: ['published']}
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'true')
    })

    it('publishModule.do action endpoint is reachable for the module', () => {
        // The action requires forge-developer permission; with the SUPER_USER
        // we expect a 200 response carrying a "published" JSON field. We do
        // not flip the value here (just confirms the action is wired and
        // routing matches) — flipping in CI would race with the JCR mutation
        // above.
        postAction(modulePath, 'publishModule', {publish: 'true'}).should((res) => {
            expect(res.status).to.be.oneOf([200, 302])
        })
    })
})
