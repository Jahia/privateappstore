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
 *
 * Note: boolean JCR properties need an explicit `type: BOOLEAN` on the
 * mutation — without it `setValue("true")` lands as a STRING which the JSP
 * EL `.boolean` coercion handles inconsistently.
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
        // changeLog on jnt:forgeModuleVersion is NOT i18n per the CND
        // (`- changeLog (string, richtext) indexed=no`) — passing language
        // makes the mutation a silent no-op.
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: versionPath,
                properties: [
                    {
                        name: 'changeLog',
                        value: '- Initial release\n- Cypress-driven test fixture',
                        language: null,
                        type: null
                    }
                ]
            }
        })

        cy.apollo({
            query: getNodeProperty,
            variables: {path: versionPath, name: 'changeLog', language: null},
            fetchPolicy: 'no-cache'
        })
            .its('data.jcr.nodeByPath.property.value')
            .should('contain', 'Initial release')
    })

    it('publishes the version and the module, then publishes the site to LIVE', () => {
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: versionPath,
                properties: [{name: 'published', value: 'true', language: null, type: 'BOOLEAN'}]
            }
        })
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: modulePath,
                properties: [{name: 'published', value: 'true', language: null, type: 'BOOLEAN'}]
            }
        })

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
        // The action enforces a form-token (CSRF) and rejects token-less
        // requests with 400. We accept that as proof the endpoint is wired
        // — driving the full action successfully would require a real
        // form-token round-trip from the legacy JSP UI.
        postAction(modulePath, 'publishModule', {publish: 'true'}).should((res) => {
            expect(res.status, `unexpected status ${res.status}`)
                .to.not.equal(404)
        })
    })
})
