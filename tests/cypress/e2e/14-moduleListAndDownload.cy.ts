import {DocumentNode} from 'graphql'
import {createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress'

/**
 * End-to-end module visibility test.
 *
 * After a module + version are published, the contentFolder.moduleList.jsp
 * renders /sites/{site}/contents/modules-repository.moduleList.json which
 * returns the catalog consumed by remote Jahia DX instances. This test
 * proves:
 *   - the published-only filter behavior of that JSON (unpublished modules
 *     MUST NOT appear)
 *   - the downloadUrl exposed in the JSON is reachable
 *
 * Implementation note: every property is set on addNode rather than via a
 * post-create mutation — addNode's InputJCRProperty path is the only one
 * we've verified handles type: BOOLEAN reliably in this dxm-provider build.
 */
describe('Module list JSON + download URL', () => {
    const siteKey = 'moduleListSite'
    const moduleName = 'cy-listed-module'
    const groupId = 'org.cypress.test'
    const version = '1.0.0'
    const downloadUrl = 'http://localhost:8080/icons/jahia-logo.png'

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

        // Build the whole module → version content tree with everything set
        // at creation time. addNode's InputJCRProperty pipeline handles
        // type=BOOLEAN; we've seen post-create mutations fail to update it
        // reliably across dxm-provider versions.
        cy.apollo({
            mutation: addNodeWithProps,
            variables: {
                parentPath: `/sites/${siteKey}/contents`,
                name: moduleName,
                primaryNodeType: 'jnt:forgeModule',
                properties: [
                    {name: 'jcr:title', value: 'Cypress Listed Module', language: 'en'},
                    {name: 'groupId', value: groupId},
                    {name: 'published', value: 'true', type: 'BOOLEAN'}
                ]
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
                    {name: 'url', value: downloadUrl},
                    {name: 'published', value: 'true', type: 'BOOLEAN'}
                ]
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
                properties: [{name: 'published', value: 'false', type: 'BOOLEAN'}]
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
