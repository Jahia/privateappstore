import {gql} from '@apollo/client';

export const GET_CATEGORY_SETTINGS = gql`
    query ForgeCategorySettings($siteKey: String!) {
        forgeCategorySettings(siteKey: $siteKey) {
            siteKey
            rootCategoryUuid
            rootCategoryPath
            rootCategoryDisplayName
            siteLanguages
            categories {
                uuid
                name
                displayName
                titles {
                    language
                    title
                }
                usages
            }
        }
    }
`;

export const SET_ROOT_CATEGORY = gql`
    mutation SetRootCategory($siteKey: String!, $rootCategoryUuid: String!) {
        setRootCategory(siteKey: $siteKey, rootCategoryUuid: $rootCategoryUuid)
    }
`;

export const ADD_FORGE_CATEGORY = gql`
    mutation AddForgeCategory($siteKey: String!, $name: String!) {
        addForgeCategory(siteKey: $siteKey, name: $name)
    }
`;

export const UPDATE_FORGE_CATEGORY_TITLES = gql`
    mutation UpdateForgeCategoryTitles(
        $siteKey: String!
        $uuid: String!
        $titles: [InputForgeCategoryTitle!]!
    ) {
        updateForgeCategoryTitles(siteKey: $siteKey, uuid: $uuid, titles: $titles)
    }
`;

export const DELETE_FORGE_CATEGORY = gql`
    mutation DeleteForgeCategory($siteKey: String!, $uuid: String!) {
        deleteForgeCategory(siteKey: $siteKey, uuid: $uuid)
    }
`;
