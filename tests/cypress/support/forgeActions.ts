/**
 * Small helpers that wrap the legacy Jahia "/.<action>.do" endpoints used by
 * the forge admin pages. Driving them from Cypress lets us exercise the
 * production action code without needing to render the jQuery-driven JSP UI.
 *
 * NOTE: form-encoded only — multipart uploads are best driven from a browser
 * context. Tests that need real file uploads should use cy.visit + selectFile
 * on the upload form, or seed the file node directly through GraphQL.
 */

interface ActionResponse {
    status: number
    body: unknown
}

const SUPER_USER = 'root'

function basicAuth(): string {
    const password = Cypress.env('SUPER_USER_PASSWORD') || 'root1234'
    return `Basic ${btoa(`${SUPER_USER}:${password}`)}`
}

export function postAction(
    path: string,
    action: string,
    body: Record<string, string> = {}
): Cypress.Chainable<ActionResponse> {
    const formBody = Object.entries(body)
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join('&')

    return cy
        .request({
            method: 'POST',
            url: `${path}.${action}.do`,
            headers: {
                Authorization: basicAuth(),
                'Content-Type': 'application/x-www-form-urlencoded',
                Accept: 'application/json'
            },
            body: formBody,
            failOnStatusCode: false
        })
        .then((res) => ({ status: res.status, body: res.body }))
}
