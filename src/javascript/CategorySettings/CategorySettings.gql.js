import {gql} from '@apollo/client';

export const GET_CATEGORY_SETTINGS = gql`
    query ForgeCategorySettings($siteKey: String!) {
        forge {
            categorySettings(siteKey: $siteKey) {
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
    }
`;

export const SET_ROOT_CATEGORY = gql`
    mutation SetRootCategory($siteKey: String!, $rootCategoryUuid: String!) {
        forge {
            setRootCategory(siteKey: $siteKey, rootCategoryUuid: $rootCategoryUuid)
        }
    }
`;

export const ADD_FORGE_CATEGORY = gql`
    mutation AddForgeCategory($siteKey: String!, $name: String!) {
        forge {
            addCategory(siteKey: $siteKey, name: $name)
        }
    }
`;

export const UPDATE_FORGE_CATEGORY_TITLES = gql`
    mutation UpdateForgeCategoryTitles(
        $siteKey: String!
        $uuid: String!
        $titles: [InputForgeCategoryTitle!]!
    ) {
        forge {
            updateCategoryTitles(siteKey: $siteKey, uuid: $uuid, titles: $titles)
        }
    }
`;

export const DELETE_FORGE_CATEGORY = gql`
    mutation DeleteForgeCategory($siteKey: String!, $uuid: String!) {
        forge {
            deleteCategory(siteKey: $siteKey, uuid: $uuid)
        }
    }
`;
