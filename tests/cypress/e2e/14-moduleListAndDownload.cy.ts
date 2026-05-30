import {DocumentNode} from 'graphql'
import {
    createSite,
    deleteSite,
    publishAndWaitJobEnding,
    setNodeProperty
} from '@jahia/cypress'

/**
 * End-to-end module visibility test.
 *
 * After a module + version are published, the contentFolder.moduleList.jsp
 * renders /en/sites/{site}/contents/modules-repository.moduleList.json which
 * returns the catalog consumed by remote Jahia DX instances. This test
 * proves:
 *   - the module appears in the published catalog with its version + download URL
 *   - the published-only filter behavior (unpublished modules MUST NOT appear)
 *   - the downloadUrl exposed in the JSON is reachable
 *
 * CRITICAL: the JSON renderer walks jcr:getDescendantNodes(modules-repository,
 * 'jnt:forgeModule'), so the module MUST live UNDER modules-repository — which
 * is exactly where the real CreateEntryFromJar upload action puts it
 * (modules-repository/{groupId}/{name}). A module created elsewhere under
 * contents/ would never appear in this listing.
 *
 * Property writes use @jahia/cypress setNodeProperty (the proven write path);
 * the whole site is published so the /en/ LIVE URL resolves with the
 * published module in place.
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

    // The module lives under modules-repository — see class comment.
    const repositoryPath = `/sites/${siteKey}/contents/modules-repository`
    const modulePath = `${repositoryPath}/${moduleName}`
    const versionName = `${moduleName}-${version}`
    const versionPath = `${modulePath}/${versionName}`
    const jsonUrl = `/en/sites/${siteKey}/contents/modules-repository.moduleList.json`

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
                parentPath: repositoryPath,
                name: moduleName,
                title: 'Cypress Listed Module'
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

        setNodeProperty(modulePath, 'groupId', groupId, 'en')
        setNodeProperty(modulePath, 'published', 'true', 'en')
        setNodeProperty(versionPath, 'published', 'true', 'en')

        // Publish the whole site so the /en/ LIVE URL resolves with the
        // module + version present and flagged published in LIVE.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en'])
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
        setNodeProperty(modulePath, 'published', 'false', 'en')
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
