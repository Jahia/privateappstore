package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRCallback;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.JCRTemplate;
import org.jahia.services.sites.JahiaSitesService;

import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLName("ManageRolesMutations")
@GraphQLDescription("Private App Store role-management mutations")
public final class ManageRolesMutationExtension {

    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;
    private static final String GRANT = "GRANT";

    private ManageRolesMutationExtension() {
    }

    @GraphQLField
    @GraphQLName("grantSiteRole")
    @GraphQLDescription("Grant a role to a principal on the site node")
    public static Boolean grantSiteRole(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("role") @GraphQLNonNull final String role,
            @GraphQLName("principalName") @GraphQLNonNull final String principalName,
            @GraphQLName("principalType") @GraphQLNonNull final PrincipalType principalType) {
        // Only the store-managed roles may be granted through this gate. Without this an admin
        // with siteAdminForgeSettings could pass an arbitrary role name and create ACL entries
        // beyond the store's intended set (SECURITY-571). The allowlist is the same set the
        // admin UI renders (ManageRolesQueryExtension.FORGE_ROLES).
        if (!ManageRolesQueryExtension.FORGE_ROLES.contains(role)) {
            throw new ForgeSettingsException("Role not permitted: " + role, null);
        }
        return execute(siteKey, session -> {
            final JCRNodeWrapper site = session.getNode(SITES_PATH + siteKey);
            site.grantRoles(principalType.toPrincipalKey(principalName),
                    new HashSet<>(Collections.singletonList(role)));
            session.save();
            return Boolean.TRUE;
        });
    }

    @GraphQLField
    @GraphQLName("revokeSiteRole")
    @GraphQLDescription("Revoke a single role from a principal on the site node. Leaves other roles untouched.")
    public static Boolean revokeSiteRole(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("role") @GraphQLNonNull final String role,
            @GraphQLName("principalName") @GraphQLNonNull final String principalName,
            @GraphQLName("principalType") @GraphQLNonNull final PrincipalType principalType) {
        // Same allowlist as grantSiteRole: this gate manages only the store roles, so it must
        // never revoke a non-store role (e.g. strip site-administrator from a principal). Without
        // this a store admin could downgrade arbitrary site grants. (SECURITY-571 review NEW-1)
        if (!ManageRolesQueryExtension.FORGE_ROLES.contains(role)) {
            throw new ForgeSettingsException("Role not permitted: " + role, null);
        }
        return execute(siteKey, session -> {
            final JCRNodeWrapper site = session.getNode(SITES_PATH + siteKey);
            final String principalKey = principalType.toPrincipalKey(principalName);
            // JCRNodeWrapper has no per-role revoke. Strategy: pull the principal's
            // current role set on this node, drop the target role, then write the
            // remainder back. If nothing remains, drop the principal entirely.
            final List<String[]> grants = site.getAclEntries().get(principalKey);
            if (grants == null) {
                return Boolean.TRUE;
            }

            final Set<String> remainingRoles = directGrantsExcluding(grants, site.getPath(), role);
            site.revokeRolesForPrincipal(principalKey);
            if (!remainingRoles.isEmpty()) {
                site.grantRoles(principalKey, remainingRoles);
            }

            session.save();
            return Boolean.TRUE;
        });
    }

    /**
     * Collects the principal's currently-directly-granted roles on the given node,
     * minus the one being revoked. Inherited entries are intentionally excluded —
     * they can only be edited at their source node.
     */
    private static Set<String> directGrantsExcluding(List<String[]> grants, String sitePath, String roleToRevoke) {
        final Set<String> remaining = new HashSet<>();
        for (String[] grant : grants) {
            if (isDirectGrantForOtherRole(grant, sitePath, roleToRevoke)) {
                remaining.add(grant[2]);
            }
        }

        return remaining;
    }

    private static boolean isDirectGrantForOtherRole(String[] grant, String sitePath, String roleToRevoke) {
        // JCRNodeWrapperImpl.getAclEntries returns String[] of [path, aceType, roleName].
        return grant.length >= 3
                && GRANT.equals(grant[1])
                && sitePath.equals(grant[0])
                && !grant[2].equals(roleToRevoke);
    }

    private static <T> T execute(String siteKey, JCRCallback<T> work) {
        // Reject a path-traversal siteKey before it is concatenated into a JCR site path — the
        // sibling mutation/query extensions all validate first; this one must too (SECURITY-571).
        ForgeSettingsMutationExtension.validateSiteKey(siteKey);
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                final String sitePath = SITES_PATH + siteKey;
                if (!session.nodeExists(sitePath)) {
                    throw new RepositoryException("Site not found: " + siteKey);
                }

                final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
                if (!callerSession.nodeExists(sitePath)
                        || !callerSession.getNode(sitePath).hasPermission(PERMISSION)) {
                    throw new AccessDeniedException(PERMISSION);
                }

                return work.doInJCR(session);
            });
        } catch (RepositoryException e) {
            throw new ForgeSettingsException(
                    "Manage-roles mutation failed for site " + siteKey, e);
        }
    }
}
