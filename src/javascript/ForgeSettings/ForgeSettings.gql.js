import {gql} from '@apollo/client';

export const GET_FORGE_SETTINGS = gql`
    query ForgeSettings($siteKey: String!) {
        forgeSettings(siteKey: $siteKey) {
            siteKey
            url
            id
            user
            passwordSet
        }
    }
`;

export const UPDATE_FORGE_SETTINGS = gql`
    mutation UpdateForgeSettings(
        $siteKey: String!
        $url: String
        $id: String
        $user: String
        $password: String
    ) {
        updateForgeSettings(
            siteKey: $siteKey
            url: $url
            id: $id
            user: $user
            password: $password
        ) {
            siteKey
            url
            id
            user
            passwordSet
        }
    }
`;
