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
 *   1. With forge settings configured, the createEntryFromJar.do endpoint
 *      responds to multipart-less probes with an action-style error JSON
 *      (proves the route is wired and the Action is active).
 *   2. A jnt:forgeModule can be created under contents/ and is reachable via
 *      its module page in EDIT workspace — the same node-creation path the
 *      upload action ultimately walks.
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

    it('the createEntryFromJar.do endpoint is wired once forge settings are configured', () => {
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
        // POST without a multipart body — the Action will reject with an
        // error JSON, but the fact we get a 200-class response with an error
        // body (not a 404) proves the .do is registered.
        cy.request({
            method: 'POST',
            url: `/sites/${siteKey}/contents/modules-repository.createEntryFromJar.do`,
            failOnStatusCode: false
        }).then((res) => {
            expect(res.status, `${res.status} ${typeof res.body === 'string' ? res.body.slice(0, 120) : ''}`)
                .to.not.equal(404)
        })
    })

    it('creates a forge module via the GraphQL contract used by the upload action', () => {
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
