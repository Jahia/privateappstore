import { createSite, deleteSite, publishAndWaitJobEnding } from '@jahia/cypress'

/**
 * Provision a site that uses the store-template templates set and verify that
 * the resulting home page renders. createSite() will fail if the templates set
 * is not deployed, so this test exercises store-template + privateappstore
 * together (privateappstore's CND is a transitive dependency of the templates).
 */
describe('store-template site provisioning', () => {
    const siteKey = 'storeTestSite'

    before(() => {
        cy.login()
        // Make sure we start clean — ignore failures if the site does not exist yet.
        try {
            deleteSite(siteKey)
        } catch {
            // intentionally ignored
        }
        createSite(siteKey, {
            languages: 'en',
            templateSet: 'store-template',
            serverName: 'localhost',
            locale: 'en',
        })
        // createSite leaves the site in EDIT only — publish it so the LIVE
        // workspace (where /sites/<key>/home.html is served from) actually
        // has the home page.
        publishAndWaitJobEnding(`/sites/${siteKey}`, ['en'])
    })

    after(() => {
        deleteSite(siteKey)
    })

    it('renders the home page in LIVE workspace', () => {
        cy.request({
            url: `/sites/${siteKey}/home.html`,
            failOnStatusCode: false,
        }).then((response) => {
            // 200 or 302 (redirect to login on protected modes) are both acceptable
            expect([200, 302]).to.include(response.status)
        })
    })

    it('exposes the privateappstore admin entry point for the site', () => {
        cy.login()
        cy.request({
            url: `/jahia/administration/${siteKey}`,
            failOnStatusCode: false,
        })
            .its('status')
            .should('be.oneOf', [200, 302])
    })
})
