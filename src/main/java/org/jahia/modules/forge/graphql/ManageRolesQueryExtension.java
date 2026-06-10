package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.sites.JahiaSitesService;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLName("ManageRolesQueries")
@GraphQLDescription("Private App Store role-management queries")
public final class ManageRolesQueryExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageRolesQueryExtension.class);
    private static final String PERMISSION = "siteAdminForgeSettings";
    private static final String SITES_PATH = JahiaSitesService.SITES_JCR_PATH + FileSystem.SEPARATOR;
    private static final String GRANT = "GRANT";

    // Roles intentionally exposed by the Private App Store admin UI. Order is
    // preserved in the response so the screen renders them top-to-bottom in
    // the order admins expect to see them. Package-private so the grant mutation
    // can reuse it as its allowlist (one source of truth — the set of roles the
    // store admin may assign can never drift from the set the UI renders).
    static final List<String> FORGE_ROLES = Arrays.asList(
            "store-administrator",
            "store-developer",
            "reader");

    private ManageRolesQueryExtension() {
    }

    @GraphQLField
    @GraphQLName("manageRolesSettings")
    @GraphQLDescription("Read the role-management settings for a site")
    public static GqlManageRolesSettings getManageRolesSettings(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey) {
        ForgeSettingsMutationExtension.validateSiteKey(siteKey);
        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session ->
                    readManageRolesSettings(session, siteKey));
        } catch (RepositoryException e) {
            LOGGER.error("Error reading manage-roles settings for site {}", siteKey, e);
            return null;
        }
    }

    private static GqlManageRolesSettings readManageRolesSettings(JCRSessionWrapper session, String siteKey)
            throws RepositoryException {
        final String sitePath = SITES_PATH + siteKey;
        if (!session.nodeExists(sitePath)) {
            return null;
        }

        ensureCallerHasPermission(sitePath);

        final JCRNodeWrapper site = session.getNode(sitePath);
        final Map<String, List<GqlPrincipal>> membersByRole = emptyMembersByRole();
        for (Map.Entry<String, List<String[]>> entry : site.getAclEntries().entrySet()) {
            collectGrantsForPrincipal(session, siteKey, sitePath,
                    entry.getKey(), entry.getValue(), membersByRole);
        }

        final List<GqlRoleGrants> roles = new ArrayList<>(FORGE_ROLES.size());
        for (String role : FORGE_ROLES) {
            roles.add(new GqlRoleGrants(role, membersByRole.get(role)));
        }

        return new GqlManageRolesSettings(siteKey, roles);
    }

    private static Map<String, List<GqlPrincipal>> emptyMembersByRole() {
        final Map<String, List<GqlPrincipal>> membersByRole = new LinkedHashMap<>();
        for (String role : FORGE_ROLES) {
            membersByRole.put(role, new ArrayList<>());
        }

        return membersByRole;
    }

    /**
     * For one principal's ACL entries, add each direct GRANT of a forge-managed
     * role to the running membersByRole map. Inherited grants and DENY entries
     * are skipped — admins manage direct site grants here, inherited entries
     * are tracked at their origin node.
     */
    private static void collectGrantsForPrincipal(JCRSessionWrapper session, String siteKey, String sitePath,
                                                  String principalKey, List<String[]> grants,
                                                  Map<String, List<GqlPrincipal>> membersByRole) {
        for (String[] grant : grants) {
            if (!isDirectForgeGrant(grant, sitePath)) {
                continue;
            }

            final List<GqlPrincipal> bucket = membersByRole.get(grant[2]);
            if (bucket != null) {
                bucket.add(resolvePrincipal(session, siteKey, principalKey));
            }
        }
    }

    private static boolean isDirectForgeGrant(String[] grant, String sitePath) {
        // JCRNodeWrapperImpl.getAclEntries returns String[] of [path, aceType, roleName].
        // [0] = node path where the ACE lives (direct grants → site path; inherited → ancestor path)
        // [1] = "GRANT" | "DENY" | "EXTERNAL"
        // [2] = role name (or "role/externalPermissionName" for EXTERNAL)
        return grant.length >= 3
                && GRANT.equals(grant[1])
                && sitePath.equals(grant[0])
                && FORGE_ROLES.contains(grant[2]);
    }

    @GraphQLField
    @GraphQLName("searchForgePrincipals")
    @GraphQLDescription("Find users or groups whose name contains the given term (case-insensitive)")
    public static List<GqlPrincipal> searchForgePrincipals(
            @GraphQLName("siteKey") @GraphQLNonNull final String siteKey,
            @GraphQLName("searchTerm") @GraphQLNonNull final String searchTerm,
            @GraphQLName("type") @GraphQLNonNull final PrincipalType type) {
        ForgeSettingsMutationExtension.validateSiteKey(siteKey);
        if (StringUtils.isBlank(searchTerm)) {
            return Collections.emptyList();
        }

        try {
            return JCRTemplate.getInstance().doExecuteWithSystemSession(session -> {
                ensureCallerHasPermission(SITES_PATH + siteKey);
                return searchPrincipalNodes(session, siteKey, type, searchTerm.trim());
            });
        } catch (RepositoryException e) {
            LOGGER.error("Error searching principals for site {}", siteKey, e);
            return Collections.emptyList();
        }
    }

    private static void ensureCallerHasPermission(String sitePath) throws RepositoryException {
        final JCRSessionWrapper callerSession = JCRSessionFactory.getInstance().getCurrentUserSession();
        if (!callerSession.nodeExists(sitePath)
                || !callerSession.getNode(sitePath).hasPermission(PERMISSION)) {
            throw new AccessDeniedException(PERMISSION);
        }
    }

    private static List<GqlPrincipal> searchPrincipalNodes(JCRSessionWrapper session, String siteKey,
                                                          PrincipalType type, String term) throws RepositoryException {
        final NodeIterator it = runPrincipalQuery(session, siteKey, type,
                "localname(p) like '%" + sqlSafe(term) + "%'");
        final List<GqlPrincipal> results = new ArrayList<>();
        while (it.hasNext()) {
            final JCRNodeWrapper node = (JCRNodeWrapper) it.nextNode();
            results.add(new GqlPrincipal(node.getName(), type, node.getDisplayableName()));
        }

        return results;
    }

    private static GqlPrincipal resolvePrincipal(JCRSessionWrapper session, String siteKey, String principalKey) {
        final PrincipalType type = PrincipalType.fromPrincipalKey(principalKey);
        final String name = PrincipalType.stripPrefix(principalKey);
        if (type == null || name == null) {
            return new GqlPrincipal(principalKey, PrincipalType.USER, principalKey);
        }

        try {
            final NodeIterator it = runPrincipalQuery(session, siteKey, type,
                    "localname(p) = '" + sqlSafe(name) + "'");
            if (it.hasNext()) {
                final JCRNodeWrapper node = (JCRNodeWrapper) it.nextNode();
                return new GqlPrincipal(name, type, node.getDisplayableName());
            }
        } catch (RepositoryException e) {
            LOGGER.debug("Could not resolve display name for {}", principalKey, e);
        }

        return new GqlPrincipal(name, type, name);
    }

    private static NodeIterator runPrincipalQuery(JCRSessionWrapper session, String siteKey,
                                                  PrincipalType type, String localnamePredicate) throws RepositoryException {
        final String nodeType = type == PrincipalType.USER ? "jnt:user" : "jnt:group";
        final String statement = "select * from [" + nodeType + "] as p "
                + "where " + localnamePredicate + " "
                + "and (isdescendantnode(p, ['" + SITES_PATH + sqlSafe(siteKey) + "']) "
                + "or isdescendantnode(p, ['/users']) "
                + "or isdescendantnode(p, ['/groups']))";

        return session.getWorkspace().getQueryManager()
                .createQuery(statement, Query.JCR_SQL2)
                .execute().getNodes();
    }

    private static String sqlSafe(String value) {
        return JCRContentUtils.sqlEncode(value).replace("'", "''");
    }
}
