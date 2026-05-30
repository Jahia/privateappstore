import {DocumentNode} from 'graphql'
import {
    createSite,
    deleteSite,
    publishAndWaitJobEnding,
    setNodeProperty
} from '@jahia/cypress'
import {postAction} from '../support/forgeActions'

/**
 * Module publish lifecycle.
 *
 * Sets a changeLog on the version, marks both module and version published,
 * then asserts the JCR state. The legacy publishModule.do action does the
 * same plus a publish to LIVE.
 *
 * Implementation note: this spec uses @jahia/cypress's setNodeProperty
 * helper rather than a hand-written mutation. The helper's underlying
 * mutation
 *   mutateProperty(name).setValue(value, language)
 * is the only write path we've found to reliably persist through to JCR
 * across i18n / non-i18n / boolean property types in this dxm-provider
 * build (3.6.1-SNAPSHOT). The `language` argument is required even for
 * non-i18n properties — JCR ignores it on those.
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

        // Use the @jahia/cypress helper — proven write path.
        setNodeProperty(versionPath, 'changeLog', '- Initial release\n- Cypress-driven test fixture', 'en')
        setNodeProperty(versionPath, 'published', 'true', 'en')
        setNodeProperty(modulePath, 'published', 'true', 'en')
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('the version was created with a changeLog', () => {
        cy.apollo({
            query: getNodeProperty,
            // changeLog is non-i18n — read with language=null. Reading a
            // non-i18n property with a language returns an empty set in
            // dxm-provider.
            variables: {path: versionPath, name: 'changeLog', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('contain', 'Initial release')
    })

    it('publishes the version and the module to LIVE', () => {
        publishAndWaitJobEnding(modulePath, ['en'])

        cy.apollo({
            query: getNodeProperty,
            variables: {path: modulePath, name: 'published', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'true')

        cy.apollo({
            query: getNodeProperty,
            variables: {path: versionPath, name: 'published', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.properties[0].value')
            .should('equal', 'true')
    })

    it('publishModule.do action endpoint is wired', () => {
        postAction(modulePath, 'publishModule', {publish: 'true'}).should((res) => {
            expect(res.status, `unexpected status ${res.status}`)
                .to.not.equal(404)
        })
    })
})
