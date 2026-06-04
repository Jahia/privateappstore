import {gql} from '@apollo/client';

export const GET_FORGE_SETTINGS = gql`
    query ForgeSettings($siteKey: String!) {
        forgeSettings(siteKey: $siteKey) {
            siteKey
            url
            id
            user
            passwordSet
            logoPath
            copyright
            footerLinks {
                privacyUrl
                termsUrl
                cookiesUrl
                facebookUrl
                linkedinUrl
                twitterUrl
                youtubeUrl
            }
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
        $logo: String
        $copyright: String
        $privacyUrl: String
        $termsUrl: String
        $cookiesUrl: String
        $facebookUrl: String
        $linkedinUrl: String
        $twitterUrl: String
        $youtubeUrl: String
    ) {
        updateForgeSettings(
            siteKey: $siteKey
            settings: {
                url: $url
                id: $id
                user: $user
                password: $password
                logo: $logo
                copyright: $copyright
                footerLinks: {
                    privacyUrl: $privacyUrl
                    termsUrl: $termsUrl
                    cookiesUrl: $cookiesUrl
                    facebookUrl: $facebookUrl
                    linkedinUrl: $linkedinUrl
                    twitterUrl: $twitterUrl
                    youtubeUrl: $youtubeUrl
                }
            }
        ) {
            siteKey
            url
            id
            user
            passwordSet
            logoPath
            copyright
            footerLinks {
                privacyUrl
                termsUrl
                cookiesUrl
                facebookUrl
                linkedinUrl
                twitterUrl
                youtubeUrl
            }
        }
    }
`;

// Image files in the site's media library, for the logo picker.
export const GET_SITE_IMAGES = gql`
    query SiteImages($path: String!) {
        jcr {
            nodeByPath(path: $path) {
                descendants(typesFilter: {types: ["jmix:image"]}) {
                    nodes {
                        uuid
                        name
                        path
                    }
                }
            }
        }
    }
`;
