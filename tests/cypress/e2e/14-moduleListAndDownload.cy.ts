import {DocumentNode} from 'graphql'
import {
    createSite,
    deleteSite,
    publishAndWaitJobEnding,
    setNodeProperty,
    uploadFile
} from '@jahia/cypress'

/**
 * End-to-end module catalog + download test.
 *
 * After a module + version are published, contentFolder.moduleList.jsp renders
 * /en/sites/{site}/contents/modules-repository.moduleList.json — the catalog
 * remote Jahia DX instances consume. This test proves:
 *   - the module appears in the published catalog with its version
 *   - the catalog's downloadUrl points at the real uploaded artifact and is
 *     actually downloadable (the "download test of the new module")
 *   - the published-only filter (unpublished modules MUST NOT appear)
 *
 * The version carries a real jnt:file (one of the module JARs under
 * tests/assets). When a version has a jnt:file child the JSP builds the
 * downloadUrl from that file's /files/ path rather than the `url` property —
 * so the catalog advertises a genuinely downloadable artifact.
 *
 * The module MUST live under modules-repository: the JSON renderer walks
 * jcr:getDescendantNodes(modules-repository, 'jnt:forgeModule'), exactly where
 * the real CreateEntryFromJar upload action places modules.
 */
describe('Module list JSON + download', () => {
    const siteKey = 'moduleListSite'
    const moduleName = 'cy-listed-module'
    const groupId = 'org.cypress.test'
    const version = '1.0.0'
    // A real module JAR (a copy of the privateappstore module under test),
    // shipped as a fixture under tests/assets with a version-agnostic name so
    // it does not drift with the build. cy.fixture resolves from
    // cypress/fixtures, so step up to tests/assets.
    const artifactFixture = '../../assets/sample-module.jar'
    const artifactName = 'sample-module.jar'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const addNodeWithProps: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/addNodeWithProperties.graphql')

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
                // No jcr:title — the type has no such definition.
                properties: [
                    {name: 'versionNumber', value: version}
                ]
            }
        })

        // Attach the real artifact as a jnt:file child of the version. The
        // moduleList JSP then derives downloadUrl from this file's path.
        uploadFile(artifactFixture, versionPath, artifactName, 'application/java-archive')

        setNodeProperty(modulePath, 'groupId', groupId, 'en')
        setNodeProperty(modulePath, 'published', 'true', 'en')
        setNodeProperty(versionPath, 'published', 'true', 'en')

        // Publish the whole site so the /en/ LIVE URL resolves and the file's
        // binary content is available from the LIVE files servlet.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en'])
    })

    after(() => {
        deleteSite(siteKey)
    })

    function findVersion(body: unknown) {
        const payload = Array.isArray(body) ? body[0] : body
        const modules = (payload as { modules?: Array<{ name: string; versions: Array<{ version: string; downloadUrl: string }> }> }).modules || []
        const module = modules.find(m => m.name === moduleName)
        return {payload, module, version: module?.versions?.find(v => v.version === version)}
    }

    it('moduleList.json lists the published module + version with a download URL', () => {
        cy.request({url: jsonUrl, failOnStatusCode: false}).then((res) => {
            expect(res.status).to.equal(200)
            const {payload, module, version: v} = findVersion(res.body)
            expect(payload).to.have.property('modules')
            expect(module, `module ${moduleName} present in modules[]`).to.not.be.undefined
            expect(v, `version ${version} listed`).to.not.be.undefined
            // downloadUrl points at the uploaded artifact via the files servlet.
            expect(v!.downloadUrl, 'downloadUrl present').to.be.a('string').and.not.be.empty
            expect(v!.downloadUrl).to.contain('/files/')
            expect(v!.downloadUrl).to.contain(artifactName)
        })
    })

    it('the catalog downloadUrl serves the artifact (download test)', () => {
        cy.request({url: jsonUrl, failOnStatusCode: false}).then((res) => {
            const {version: v} = findVersion(res.body)
            if (!v) {
                throw new Error('version not in listing — see preceding test failure')
            }

            // downloadUrl may be absolute (http://host/files/...). Strip the
            // scheme+host and fetch the path against the Cypress baseUrl so it
            // resolves to the Jahia under test regardless of the host the JSP
            // rendered (localhost vs the docker service name).
            const path = v.downloadUrl.replace(/^https?:\/\/[^/]+/, '')
            cy.request({url: path, failOnStatusCode: false, encoding: 'binary'}).then((download) => {
                expect(download.status, `download ${path}`).to.equal(200)
                expect(download.body.length, 'artifact body is non-empty').to.be.greaterThan(0)
            })
        })
    })

    it('omits the module when published=false', () => {
        setNodeProperty(modulePath, 'published', 'false', 'en')
        publishAndWaitJobEnding(modulePath, ['en'])

        cy.request({url: jsonUrl, failOnStatusCode: false}).then((res) => {
            const {module} = findVersion(res.body)
            expect(module, 'unpublished module hidden from listing').to.be.undefined
        })
    })
})
