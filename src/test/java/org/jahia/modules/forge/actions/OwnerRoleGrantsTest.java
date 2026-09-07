package org.jahia.modules.forge.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the subtree check that bounds {@link OwnerRoleGrants}' elevated writes.
 *
 * <p>This is the constraint that keeps the SEC-366 fix from trading one over-privilege for
 * another: the owner grant now runs in a <em>system</em> session, so the only thing standing
 * between it and the rest of the site is the requirement that the target live under the
 * repository node the Action already gated with {@code jahiaForgeUploadModule}. A sloppy
 * {@code startsWith} would let a sibling path escape that bound, so the boundary case is pinned
 * down explicitly.
 */
class OwnerRoleGrantsTest {

    private static final String REPO = "/sites/store/contents/modules-repository";

    @Test
    @DisplayName("the scope root itself is within scope")
    void isWithin_rootItself_isTrue() {
        assertThat(OwnerRoleGrants.isWithin(REPO, REPO)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "/sites/store/contents/modules-repository/org/jahia/modules/widget",
            "/sites/store/contents/modules-repository/packages/my-package",
            "/sites/store/contents/modules-repository/org/jahia/widget/widget-1.0"
    })
    @DisplayName("descendants of the upload repository are within scope")
    void isWithin_descendants_areTrue(String path) {
        assertThat(OwnerRoleGrants.isWithin(path, REPO)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            // A sibling whose name merely starts with the root's name - the case a bare
            // startsWith() would wrongly accept.
            "/sites/store/contents/modules-repository-archive/widget",
            "/sites/store/contents/modules-repositoryX",
            // Elsewhere in the same site, including the ACL-bearing site node itself.
            "/sites/store/contents/other-folder/widget",
            "/sites/store",
            // Another site, and the platform's own trees.
            "/sites/othersite/contents/modules-repository/widget",
            "/roles/store-developer",
            "/users/devuser"
    })
    @DisplayName("anything outside the upload repository is refused")
    void isWithin_outsideScope_isFalse(String path) {
        assertThat(OwnerRoleGrants.isWithin(path, REPO)).isFalse();
    }

    @Test
    @DisplayName("a null path or root is refused rather than throwing")
    void isWithin_nullArguments_areFalse() {
        assertThat(OwnerRoleGrants.isWithin(null, REPO)).isFalse();
        assertThat(OwnerRoleGrants.isWithin(REPO, null)).isFalse();
        assertThat(OwnerRoleGrants.isWithin(null, null)).isFalse();
    }
}
