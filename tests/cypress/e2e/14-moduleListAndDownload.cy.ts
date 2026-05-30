import {DocumentNode} from 'graphql'
import {createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress'

/**
 * End-to-end module visibility test.
 *
 * After a module + version are published, the contentFolder.moduleList.jsp
 * renders /sites/{site}/contents/modules-repository.moduleList.json which
 * returns the catalog consumed by remote Jahia DX instances. This test
 * proves the published-only filter behavior of that JSON (unpublished
 * modules MUST NOT appear) and follows the downloadUrl to verify the
 * artifact-location field round-trips back to a reachable URL.
 *
 * Note: the `published` flag must be set with `type: BOOLEAN` — otherwise
 * JSP `properties.published.boolean` reads false even with value "true".
 */
describe('Module list JSON + download URL', () => {
    const siteKey = 'moduleListSite'
    const moduleName = 'cy-listed-module'
    const groupId = 'org.cypress.test'
    const version = '1.0.0'
    const downloadUrl = 'http://localhost:8080/icons/jahia-logo.png'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const mutateProp: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/mutateNodeProperty.graphql')

    const modulePath = `/sites/${siteKey}/contents/${moduleName}`
    const versionName = `${moduleName}-${version}`
    const jsonUrl = `/sites/${siteKey}/contents/modules-repository.moduleList.json`

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
                title: 'Cypress Listed Module'
            }
        })
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: modulePath,
                properties: [{name: 'groupId', value: groupId, language: null, type: null}]
            }
        })
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: modulePath,
                properties: [{name: 'published', value: 'true', language: null, type: 'BOOLEAN'}]
            }
        })

        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: modulePath,
                name: versionName,
                primaryNodeType: 'jnt:forgeModuleVersion',
                properties: [
                    {name: 'jcr:title', value: 'Cypress Listed Module', language: 'en'},
                    {name: 'versionNumber', value: version},
                    {name: 'url', value: downloadUrl}
                ]
            }
        })
        // Boolean property must be set after node creation with explicit type
        // — addNodeWithProperties passes values through without coercion.
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: `${modulePath}/${versionName}`,
                properties: [{name: 'published', value: 'true', language: null, type: 'BOOLEAN'}]
            }
        })

        publishAndWaitJobEnding(`/sites/${siteKey}/contents/modules-repository`, ['en'])
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('moduleList.json lists the published module + version', () => {
        cy.request({
            url: jsonUrl,
            failOnStatusCode: false
        }).then((res) => {
            expect(res.status).to.equal(200)
            const payload = Array.isArray(res.body) ? res.body[0] : res.body
            expect(payload).to.have.property('modules')
            const found = payload.modules.find(
                (m: { name: string }) => m.name === moduleName
            )
            expect(found, `module ${moduleName} present in modules[]`).to.not.be.undefined
            expect(found.versions, 'versions array present').to.be.an('array')
            const v = found.versions.find((x: { version: string }) => x.version === version)
            expect(v, `version ${version} listed`).to.not.be.undefined
            expect(v.downloadUrl).to.equal(downloadUrl)
        })
    })

    it('the downloadUrl exposed in moduleList.json is reachable', () => {
        cy.request({
            url: jsonUrl,
            failOnStatusCode: false
        }).then((res) => {
            const payload = Array.isArray(res.body) ? res.body[0] : res.body
            const found = (payload.modules || []).find(
                (m: { name: string }) => m.name === moduleName
            )
            if (!found || !found.versions) {
                throw new Error('module/version not in listing — see preceding test failure')
            }

            const v = found.versions.find((x: { version: string }) => x.version === version)

            cy.request({
                url: v.downloadUrl,
                failOnStatusCode: false
            }).then((download) => {
                expect(download.status).to.be.lessThan(500)
            })
        })
    })

    it('omits the module when published=false', () => {
        cy.apollo({
            mutation: mutateProp,
            variables: {
                pathOrId: modulePath,
                properties: [{name: 'published', value: 'false', language: null, type: 'BOOLEAN'}]
            }
        })
        publishAndWaitJobEnding(modulePath, ['en'])

        cy.request({url: jsonUrl, failOnStatusCode: false}).then((res) => {
            const payload = Array.isArray(res.body) ? res.body[0] : res.body
            const found = (payload.modules || []).find(
                (m: { name: string }) => m.name === moduleName
            )
            expect(found, 'unpublished module hidden from listing').to.be.undefined
        })
    })
})
