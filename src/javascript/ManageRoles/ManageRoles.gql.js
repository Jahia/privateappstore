import {gql} from '@apollo/client';

export const GET_MANAGE_ROLES_SETTINGS = gql`
    query ManageRolesSettings($siteKey: String!) {
        manageRolesSettings(siteKey: $siteKey) {
            siteKey
            roles {
                role
                members {
                    name
                    type
                    displayName
                }
            }
        }
    }
`;

export const SEARCH_FORGE_PRINCIPALS = gql`
    query SearchForgePrincipals(
        $siteKey: String!
        $searchTerm: String!
        $type: ForgePrincipalType!
    ) {
        searchForgePrincipals(siteKey: $siteKey, searchTerm: $searchTerm, type: $type) {
            name
            type
            displayName
        }
    }
`;

export const GRANT_SITE_ROLE = gql`
    mutation GrantSiteRole(
        $siteKey: String!
        $role: String!
        $principalName: String!
        $principalType: ForgePrincipalType!
    ) {
        grantSiteRole(
            siteKey: $siteKey
            role: $role
            principalName: $principalName
            principalType: $principalType
        )
    }
`;

export const REVOKE_SITE_ROLE = gql`
    mutation RevokeSiteRole(
        $siteKey: String!
        $role: String!
        $principalName: String!
        $principalType: ForgePrincipalType!
    ) {
        revokeSiteRole(
            siteKey: $siteKey
            role: $role
            principalName: $principalName
            principalType: $principalType
        )
    }
`;
