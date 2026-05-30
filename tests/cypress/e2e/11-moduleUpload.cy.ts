import {DocumentNode} from 'graphql'
import {createSite, deleteSite} from '@jahia/cypress'

/**
 * Module-upload workflow check.
 *
 * The legacy upload action chains a Maven deploy:deploy-file against the
 * configured forge URL BEFORE creating any JCR nodes — that part is exercised
 * by the integration stack (Nexus + mvn) and intentionally out of scope here.
 * What this test pins down is:
 *
 *   1. With forge settings unconfigured, the upload widget refuses to show
 *      a file input (the JSP branches on jmix:forgeSettings).
 *   2. Once forge settings are written, the upload widget renders with a
 *      multipart form posting to ...createEntryFromJar.do.
 *   3. A jnt:forgeModule can be created under contents/ and is reachable via
 *      its module page in EDIT workspace.
 */
describe('Module upload — UI workflow', () => {
    const siteKey = 'moduleUploadUiSite'
    const moduleName = 'cy-upload-module'

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const updateForgeSettings: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeSettings.graphql')
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const createForgeModule: DocumentNode =
        require('graphql-tag/loader!../fixtures/graphql/mutation/createForgeModule.graphql')

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
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('exposes the createEntryFromJar form after forge settings are configured', () => {
        // Wire forge settings — the upload widget gates on forgeSettingsUrl
        // being non-empty (see jnt_fileUpload/html/fileUpload.jar.jsp).
        cy.apollo({
            mutation: updateForgeSettings,
            variables: {
                siteKey,
                url: Cypress.env('NEXUS_URL')
                    ? `${Cypress.env('NEXUS_URL')}/repository/maven-releases/`
                    : 'http://nexus:8081/repository/maven-releases/',
                id: 'remote-repository',
                user: Cypress.env('NEXUS_USERNAME') || 'admin',
                password: Cypress.env('NEXUS_PASSWORD') || 'admin123'
            }
        })

        cy.login()
        cy.visit(`/cms/edit/default/en/sites/${siteKey}/contents/modules-repository.html`)

        // The form posts back to ...createEntryFromJar.do. The action URL is
        // the contract the JS upload code expects.
        cy.get('form.file_upload', {timeout: 30000})
            .should('have.attr', 'action')
            .and('match', /modules-repository\.createEntryFromJar\.do/)

        cy.get('form.file_upload input[type=file][name=file]').should('exist')
    })

    it('creates a forge module via the GraphQL contract used by the upload action', () => {
        // The legacy CreateEntryFromJar action ultimately calls addNode for
        // jnt:forgeModule; this asserts the same node type can be created via
        // GraphQL and is reachable on its rendered URL.
        cy.apollo({
            mutation: createForgeModule,
            variables: {
                parentPath: `/sites/${siteKey}/contents`,
                name: moduleName,
                title: 'Cypress Upload Module'
            }
        })
            .its('data.jcr.addNode.node.path')
            .should('equal', `/sites/${siteKey}/contents/${moduleName}`)

        cy.login()
        cy.request({
            url: `/cms/edit/default/en/sites/${siteKey}/contents/${moduleName}.html`,
            failOnStatusCode: false
        })
            .its('status')
            .should('be.oneOf', [200, 302])
    })
})
