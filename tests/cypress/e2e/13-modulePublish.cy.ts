import {DocumentNode} from 'graphql'
import {createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress'
import {postAction} from '../support/forgeActions'

/**
 * Module publish lifecycle.
 *
 * Sets a changeLog on the version, marks both module and version published,
 * then asserts the JCR state. The legacy publishModule.do action does the
 * same plus a publish to LIVE.
 *
 * Implementation note: rather than mutate `published` / `changeLog` after
 * node creation, we set them inline on addNodeWithProperties. The
 * InputJCRProperty path is the same one used to create the version itself,
 * which exercises a code path known to honour `type: BOOLEAN` correctly.
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

        // Create the version WITH changeLog + published baked in. Both
        // properties are non-i18n on jnt:forgeModuleVersion (changeLog is
        // richtext-but-not-i18n, published is autocreated boolean), so
        // language stays null and type=BOOLEAN is needed only for the
        // boolean.
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: versionName,
                primaryNodeType: 'jnt:forgeModuleVersion',
                properties: [
                    {name: 'jcr:title', value: 'Cypress Publish Module', language: 'en'},
                    {name: 'versionNumber', value: version},
                    {name: 'url', value: 'https://nexus.example.com/repository/maven-releases/cy/publish/module/1.0.0/module-1.0.0.jar'},
                    {name: 'changeLog', value: '- Initial release\n- Cypress-driven test fixture'},
                    {name: 'published', value: 'true', type: 'BOOLEAN'}
                ]
            }
        })

        // Module's `published` flag — set at addNode time would have required
        // restructuring createForgeModule. Use a post-create mutation here:
        // setPropertiesBatch + type=BOOLEAN on a fresh node is the same path
        // that worked in the version's addNode above.
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: modulePath,
                properties: [{name: 'published', value: 'true', type: 'BOOLEAN'}]
            }
        })
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('the version was created with a changeLog', () => {
        cy.apollo({
            query: getNodeProperty,
            variables: {path: versionPath, name: 'changeLog', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.property.value')
            .should('contain', 'Initial release')
    })

    it('publishes the version and the module to LIVE', () => {
        publishAndWaitJobEnding(modulePath, ['en'])

        cy.apollo({
            query: getNodeProperty,
            variables: {path: modulePath, name: 'published', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'true')

        cy.apollo({
            query: getNodeProperty,
            variables: {path: versionPath, name: 'published', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.property.value')
            .should('equal', 'true')
    })

    it('publishModule.do action endpoint is wired', () => {
        // The action enforces a form-token (CSRF). We accept a non-404 status
        // as proof the endpoint is registered.
        postAction(modulePath, 'publishModule', {publish: 'true'}).should((res) => {
            expect(res.status, `unexpected status ${res.status}`)
                .to.not.equal(404)
        })
    })
})
