package org.jahia.modules.forge.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the path/coordinate validators in {@link CreateEntryFromJar} — the SECURITY-571
 * guards that keep author-supplied archive metadata (groupId, module/package name, required
 * version, file extension) from traversing the JCR tree or smuggling a tainted file extension into
 * the temp-artifact path. Pure logic, no Jahia container required.
 */
class CreateEntryFromJarValidatorTest {

    // --- isSafePathFragment / isSafeCoordinate / isSafePackageName -------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"org.jahia", "my-module", "com.example_foo", "1.0", "modules"})
    @DisplayName("isSafePathFragment accepts plain coordinate tokens")
    void isSafePathFragment_accepts_plainTokens(String value) {
        assertThat(CreateEntryFromJar.isSafePathFragment(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"..", "a..b", "../etc", "a/b", "a\\b", "/abs", "x\\..\\y"})
    @DisplayName("isSafePathFragment rejects traversal / path-separator values")
    void isSafePathFragment_rejects_traversalAndSeparators(String value) {
        assertThat(CreateEntryFromJar.isSafePathFragment(value)).isFalse();
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("isSafePathFragment rejects null")
    void isSafePathFragment_rejects_null(String value) {
        assertThat(CreateEntryFromJar.isSafePathFragment(value)).isFalse();
    }

    @Test
    @DisplayName("isSafeCoordinate requires BOTH groupId and moduleName to be safe")
    void isSafeCoordinate_requiresBothSafe() {
        assertThat(CreateEntryFromJar.isSafeCoordinate("org.jahia.modules", "my-module")).isTrue();
        assertThat(CreateEntryFromJar.isSafeCoordinate("..", "my-module")).isFalse();
        assertThat(CreateEntryFromJar.isSafeCoordinate("org.jahia", "a/b")).isFalse();
        assertThat(CreateEntryFromJar.isSafeCoordinate("org.jahia", "a\\b")).isFalse();
    }

    @Test
    @DisplayName("isSafePackageName rejects separators and traversal")
    void isSafePackageName_rejectsSeparators() {
        assertThat(CreateEntryFromJar.isSafePackageName("my-package")).isTrue();
        assertThat(CreateEntryFromJar.isSafePackageName("a/b")).isFalse();
        assertThat(CreateEntryFromJar.isSafePackageName("a..b")).isFalse();
        assertThat(CreateEntryFromJar.isSafePackageName(null)).isFalse();
    }

    // --- isSafeMavenCoordinate / isSafeRequiredVersion ------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"1.0.0", "8.1.6.2", "1.0-SNAPSHOT", "8.2", "RELEASE_1"})
    @DisplayName("isSafeMavenCoordinate accepts the Maven token charset")
    void isSafeMavenCoordinate_accepts(String value) {
        assertThat(CreateEntryFromJar.isSafeMavenCoordinate(value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.2/../../x", "1.0 beta", "a;b", "$(whoami)", "a/b", "x\ny"})
    @DisplayName("isSafeMavenCoordinate rejects separators, whitespace and shell/expression metacharacters")
    void isSafeMavenCoordinate_rejects(String value) {
        assertThat(CreateEntryFromJar.isSafeMavenCoordinate(value)).isFalse();
    }

    @Test
    @DisplayName("isSafeMavenCoordinate rejects null and empty (requires at least one char)")
    void isSafeMavenCoordinate_rejectsNullAndEmpty() {
        assertThat(CreateEntryFromJar.isSafeMavenCoordinate(null)).isFalse();
        assertThat(CreateEntryFromJar.isSafeMavenCoordinate("")).isFalse();
    }

    @Test
    @DisplayName("isSafeRequiredVersion blocks a crafted required-version from traversing the JCR tree")
    void isSafeRequiredVersion_blocksTraversal() {
        assertThat(CreateEntryFromJar.isSafeRequiredVersion("8.1.6.2")).isTrue();
        assertThat(CreateEntryFromJar.isSafeRequiredVersion("8.2/../../x")).isFalse();
        assertThat(CreateEntryFromJar.isSafeRequiredVersion("8.2\\..\\x")).isFalse();
    }

    // --- trustedExtension -----------------------------------------------------------------------

    @Test
    @DisplayName("trustedExtension returns a constant for jar/tgz and null for everything else")
    void trustedExtension_allowlist() {
        assertThat(CreateEntryFromJar.trustedExtension("module.jar")).isEqualTo("jar");
        assertThat(CreateEntryFromJar.trustedExtension("package.tgz")).isEqualTo("tgz");
        assertThat(CreateEntryFromJar.trustedExtension("evil.war")).isNull();
        assertThat(CreateEntryFromJar.trustedExtension("module.jar.exe")).isNull();
        assertThat(CreateEntryFromJar.trustedExtension("noextension")).isNull();
        assertThat(CreateEntryFromJar.trustedExtension(null)).isNull();
    }

    @Test
    @DisplayName("trustedExtension never echoes the user-supplied extension back")
    void trustedExtension_neverReturnsUserInput() {
        String result = CreateEntryFromJar.trustedExtension("payload.sh");
        assertThat(result).isNull();
        // For an accepted extension the returned value is the interned constant, not the input slice.
        assertThat(CreateEntryFromJar.trustedExtension("a.JAR")).isNull(); // case-sensitive: only lowercase jar
    }

    @Test
    @DisplayName("isVersionAlreadyDeployed detects the Nexus release-repo 'cannot be updated' 400")
    void isVersionAlreadyDeployed_nexusRedeployBlocked() {
        final String mavenError = " Failed to deploy artifacts: Could not transfer artifact "
                + "org.jahia.community.modules:autofileuploader:jar:2.0.0 ... status: 400 "
                + "jahia-public-app-store/.../autofileuploader-2.0.0.jar cannot be updated -> [Help 1]";
        assertThat(CreateEntryFromJar.isVersionAlreadyDeployed(mavenError)).isTrue();
        // Case-insensitive (defensive against future Nexus phrasing tweaks).
        assertThat(CreateEntryFromJar.isVersionAlreadyDeployed("artifact CANNOT BE UPDATED")).isTrue();
    }

    @Test
    @DisplayName("isVersionAlreadyDeployed is false for other deploy failures and null")
    void isVersionAlreadyDeployed_otherFailures() {
        assertThat(CreateEntryFromJar.isVersionAlreadyDeployed(null)).isFalse();
        assertThat(CreateEntryFromJar.isVersionAlreadyDeployed(
                "Could not transfer artifact ...: Authentication failed ... 401 Unauthorized")).isFalse();
        assertThat(CreateEntryFromJar.isVersionAlreadyDeployed("Connection refused")).isFalse();
    }
}
