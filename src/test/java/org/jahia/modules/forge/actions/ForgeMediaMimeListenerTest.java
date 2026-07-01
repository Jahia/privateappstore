package org.jahia.modules.forge.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the pure event-path matcher of {@link ForgeMediaMimeListener}. The JCR
 * read/reject/correct behaviour needs a running repository (covered by the Cypress E2E); this
 * verifies which events the listener selects and that it normalizes to the resource node path.
 */
class ForgeMediaMimeListenerTest {

    private static final String BASE = "/sites/store/contents/modules-repository/org/acme/mymodule";

    @Test
    @DisplayName("matches an icon resource node and its mime/data property events")
    void matchesIconEvents() {
        assertThat(ForgeMediaMimeListener.forgeMediaResourcePath(BASE + "/icon/logo.png/jcr:content"))
                .isEqualTo(BASE + "/icon/logo.png/jcr:content");
        assertThat(ForgeMediaMimeListener.forgeMediaResourcePath(BASE + "/icon/logo.png/jcr:content/jcr:mimeType"))
                .isEqualTo(BASE + "/icon/logo.png/jcr:content");
        assertThat(ForgeMediaMimeListener.forgeMediaResourcePath(BASE + "/icon/logo.png/jcr:content/jcr:data"))
                .isEqualTo(BASE + "/icon/logo.png/jcr:content");
    }

    @Test
    @DisplayName("matches a screenshots resource node")
    void matchesScreenshotEvents() {
        assertThat(ForgeMediaMimeListener.forgeMediaResourcePath(BASE + "/screenshots/shot1.png/jcr:content/jcr:mimeType"))
                .isEqualTo(BASE + "/screenshots/shot1.png/jcr:content");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            // Not under modules-repository:
            "/sites/store/home/page.html",
            // Under modules-repository but not icon/screenshots (e.g. the artifact file itself):
            "/sites/store/contents/modules-repository/org/acme/mymodule/mymodule-1.0/mymodule-1.0.jar/jcr:content/jcr:mimeType",
            // icon/screenshots outside the module repository:
            "/sites/store/files/icon/logo.png/jcr:content",
            // A container node, no jcr:content resource segment:
            "/sites/store/contents/modules-repository/org/acme/mymodule/icon"
    })
    @DisplayName("ignores events outside forge icon/screenshots resources")
    void ignoresUnrelated(String path) {
        assertThat(ForgeMediaMimeListener.forgeMediaResourcePath(path)).isNull();
    }
}
