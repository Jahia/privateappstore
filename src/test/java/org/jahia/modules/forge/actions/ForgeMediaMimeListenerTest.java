package org.jahia.modules.forge.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure event-path pre-filter of {@link ForgeMediaMimeListener}. The precise
 * scoping (is the file under a jmix:forgeElement?), the media/artifact distinction and the
 * reject/correct behaviour are node-structure based and need a running repository (covered by the
 * Cypress E2E). This verifies the cheap path gate that decides which events are loaded at all.
 */
class ForgeMediaMimeListenerTest {

    private static final String BASE = "/sites/store/contents/modules-repository/org/acme/mymodule";

    @Test
    @DisplayName("matches an icon resource node and its mime/data property events")
    void matchesIconEvents() {
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(BASE + "/icon/logo.png/jcr:content"))
                .isEqualTo(BASE + "/icon/logo.png/jcr:content");
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(BASE + "/icon/logo.png/jcr:content/jcr:mimeType"))
                .isEqualTo(BASE + "/icon/logo.png/jcr:content");
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(BASE + "/icon/logo.png/jcr:content/jcr:data"))
                .isEqualTo(BASE + "/icon/logo.png/jcr:content");
    }

    @Test
    @DisplayName("matches a screenshots resource node")
    void matchesScreenshotEvents() {
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(BASE + "/screenshots/shot1.png/jcr:content/jcr:mimeType"))
                .isEqualTo(BASE + "/screenshots/shot1.png/jcr:content");
    }

    @Test
    @DisplayName("#61: matches a file planted OUTSIDE icon/screenshots (now covered; structural skip happens later)")
    void matchesFilePlantedElsewhere() {
        // Previously returned null (path scoping gap); now the pre-filter admits it and the
        // structural forge-element check + script-capable rule handle it in-session.
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(BASE + "/x.svg/jcr:content"))
                .isEqualTo(BASE + "/x.svg/jcr:content");
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(BASE + "/evil/x.svg/jcr:content/jcr:mimeType"))
                .isEqualTo(BASE + "/evil/x.svg/jcr:content");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            // Not under modules-repository:
            "/sites/store/home/page.html",
            "/sites/store/files/icon/logo.png/jcr:content",
            // Under modules-repository but no jcr:content resource segment (a container node):
            "/sites/store/contents/modules-repository/org/acme/mymodule/icon"
    })
    @DisplayName("ignores events outside the module repository or with no resource node")
    void ignoresUnrelated(String path) {
        assertThat(ForgeMediaMimeListener.forgeRepositoryResourcePath(path)).isNull();
    }
}
