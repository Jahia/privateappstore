import {DocumentNode} from 'graphql';
import {createSite, deleteSite} from '@jahia/cypress';

/**
 * Verifies the React + GraphQL replacement for the old forgeSettings.flow:
 *   1. forgeSettings query returns a payload for the site (initially empty).
 *   2. updateForgeSettings mutation persists url/id/user, sets passwordSet=true.
 *   3. forgeSettings query reflects the mutation.
 *   4. The admin route /jahia/administration/{siteKey}/forgeSettings responds.
 */
describe('Forge settings admin (React + GraphQL)', () => {
    const siteKey = 'forgeSettingsSite';

    const getForgeSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getForgeSettings.graphql');

    const updateForgeSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/updateForgeSettings.graphql');

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // ignore
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'localhost',
            locale: 'en'
        });
    });

    after(() => {
        deleteSite(siteKey);
    });

    it('returns an empty settings payload before any save', () => {
        cy.apollo({query: getForgeSettings, variables: {siteKey}})
            .its('data.forgeSettings')
            .should((settings: { siteKey: string; url: string | null; passwordSet: boolean }) => {
                expect(settings.siteKey).to.equal(siteKey);
                expect(settings.url).to.be.null;
                expect(settings.passwordSet).to.equal(false);
            });
    });

    it('persists settings via updateForgeSettings and reads them back', () => {
        cy.apollo({
            mutation: updateForgeSettings,
            variables: {
                siteKey,
                url: 'https://store.example.com',
                id: 'forge-1',
                user: 'forge-admin',
                password: 'p@ssw0rd'
            }
        })
            .its('data.updateForgeSettings')
            .should((settings: { url: string; passwordSet: boolean }) => {
                expect(settings.url).to.equal('https://store.example.com');
                expect(settings.passwordSet).to.equal(true);
            });

        cy.apollo({query: getForgeSettings, variables: {siteKey}})
            .its('data.forgeSettings')
            .should((settings: { url: string; id: string; user: string; passwordSet: boolean }) => {
                expect(settings.url).to.equal('https://store.example.com');
                expect(settings.id).to.equal('forge-1');
                expect(settings.user).to.equal('forge-admin');
                expect(settings.passwordSet).to.equal(true);
            });
    });

    it('keeps the existing password when mutation password is blank', () => {
        cy.apollo({
            mutation: updateForgeSettings,
            variables: {
                siteKey,
                url: 'https://store.example.com/v2',
                id: 'forge-1',
                user: 'forge-admin',
                password: null
            }
        })
            .its('data.updateForgeSettings.passwordSet')
            .should('equal', true);
    });

    it('serves the admin route', () => {
        cy.login();
        cy.request({
            url: `/jahia/administration/${siteKey}/forgeSettings`,
            failOnStatusCode: false
        })
            .its('status')
            .should('be.oneOf', [200, 302]);
    });
});
