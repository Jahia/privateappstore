import {createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress';

/**
 * SUPPORT-687 — regression coverage for the edge lockdown of store.jahia.com.
 *
 * Proves two things that an HAProxy-only test cannot:
 *
 *  1. jahia-store-template omits the header sign-in trigger when the request carries
 *     X-Jahia-Edge: public, and renders it otherwise.
 *  2. EdgeOriginCacheKeyPartGenerator keeps the two variants in SEPARATE fragment cache
 *     entries. This is the part that matters: templates/Page is cache.perUser, whose key
 *     for an anonymous visitor is "guest" either way, so without the extra key component
 *     the first render would be cached and handed to the other audience.
 *
 * The arms are interleaved deliberately (public, vpn, public). A run that only ever asks
 * in one order would pass even if the cache were cross-serving.
 *
 * cy.request is used rather than cy.visit because the decision is server-side: this reads
 * the SSR output directly and needs to set a request header, which cy.visit cannot.
 */
describe('SUPPORT-687 — edge origin drives the sign-in trigger, per cache entry', () => {
    const siteKey = 'edgeorigin';
    const sitePath = `/sites/${siteKey}`;
    const homeLive = `/cms/render/live/en${sitePath}/home.html`;

    /** Stable server-rendered marker for the Login island trigger (see Login.client.tsx). */
    const SIGN_IN_MARKER = 'aria-controls="login-panel"';

    /** Anonymous GET of the live home page, optionally claiming an edge origin. */
    const fetchHome = (edge?: string, url: string = homeLive) =>
        cy.request({
            url,
            headers: edge ? {'X-Jahia-Edge': edge} : {},
            failOnStatusCode: false
        });

    before(() => {
        cy.login();
        try {
            deleteSite(siteKey);
        } catch {
            // Ignore — first run.
        }

        createSite(siteKey, {
            languages: 'en',
            templateSet: 'jahia-store-template',
            serverName: 'edgeorigin.local',
            locale: 'en'
        });
        publishAndWaitJobEnding(sitePath, ['en']);
        // Every arm below is anonymous; a leftover root cookie would render the account
        // menu instead of the sign-in trigger and make all three arms agree for the wrong reason.
        cy.logout();
    });

    after(() => {
        cy.login();
        deleteSite(siteKey);
    });

    it('renders the sign-in trigger for a VPN visitor and hides it for a CDN visitor', () => {
        // Control first: the page must actually contain the trigger, or the negative
        // assertion below would pass on an empty/error page.
        fetchHome('vpn').its('body').should('include', SIGN_IN_MARKER);
        fetchHome('public').its('body').should('not.include', SIGN_IN_MARKER);
    });

    it('keeps the two variants in separate cache entries when interleaved', () => {
        // Arm 1 — populate the public entry.
        fetchHome('public').its('body').should('not.include', SIGN_IN_MARKER);
        // Arm 2 — same URL, VPN. If the key ignored the edge, this is where the cached
        // public fragment (no trigger) would be handed to an operator.
        fetchHome('vpn').its('body').should('include', SIGN_IN_MARKER);
        // Arm 3 — back to public. Symmetrically, this is where a public visitor would be
        // handed the operator's fragment.
        fetchHome('public').its('body').should('not.include', SIGN_IN_MARKER);
    });

    it('treats an absent header as privileged, so no-proxy environments are unchanged', () => {
        fetchHome().its('body').should('include', SIGN_IN_MARKER);
    });

    /**
     * Only meaningful with the edge in front of Jahia. CI and local dev run without it,
     * so probe first and skip rather than fail — the three arms above already cover the
     * module's own behaviour, which is what this suite owns.
     */
    it('lets HAProxy overwrite a client-supplied X-Jahia-Edge', function () {
        const proxied = `http://haproxy:8080${homeLive}`;
        cy.request({url: proxied, failOnStatusCode: false, timeout: 5000})
            .then(res => {
                if (res.status >= 500) {
                    this.skip();
                }
            })
            .then(() => {
                // Straight at Jahia the forged value is believed; nothing sanitises it there.
                fetchHome('public').its('body').should('not.include', SIGN_IN_MARKER);
                // Through HAProxy's VPN listener the same forged header is deleted and
                // re-set to "vpn", so the trigger comes back. This is what stops a visitor
                // poisoning the cache entry that operators then read.
                fetchHome('public', proxied).its('body').should('include', SIGN_IN_MARKER);
            });
    });
});
