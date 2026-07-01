package org.jahia.modules.forge.actions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MagicByteImageValidator} — content-based (magic-byte) raster detection used
 * to re-validate spoofed uploaded module media server-side. Pure logic, no Jahia container.
 */
class MagicByteImageValidatorTest {

    private static byte[] withHeader(int... header) {
        byte[] bytes = new byte[Math.max(header.length, MagicByteImageValidator.SNIFF_LENGTH)];
        for (int i = 0; i < header.length; i++) {
            bytes[i] = (byte) header[i];
        }
        return bytes;
    }

    @Test
    @DisplayName("detects PNG from its 8-byte signature")
    void detectsPng() {
        assertThat(MagicByteImageValidator.detectRasterMime(
                withHeader(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)))
                .isEqualTo(MagicByteImageValidator.PNG);
    }

    @Test
    @DisplayName("detects JPEG from FF D8 FF")
    void detectsJpeg() {
        assertThat(MagicByteImageValidator.detectRasterMime(withHeader(0xFF, 0xD8, 0xFF, 0xE0)))
                .isEqualTo(MagicByteImageValidator.JPEG);
    }

    @Test
    @DisplayName("detects GIF from the GIF8 signature")
    void detectsGif() {
        assertThat(MagicByteImageValidator.detectRasterMime(withHeader(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)))
                .isEqualTo(MagicByteImageValidator.GIF);
    }

    @Test
    @DisplayName("detects WEBP from RIFF....WEBP")
    void detectsWebp() {
        assertThat(MagicByteImageValidator.detectRasterMime(withHeader(
                0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50)))
                .isEqualTo(MagicByteImageValidator.WEBP);
    }

    @Test
    @DisplayName("RIFF without the WEBP tag is NOT accepted (e.g. WAV/AVI containers)")
    void riffButNotWebp() {
        assertThat(MagicByteImageValidator.detectRasterMime(withHeader(
                0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45))) // "WAVE"
                .isNull();
    }

    @Test
    @DisplayName("an SVG (which can carry scripts) is rejected — not a raster signature")
    void rejectsSvg() {
        byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg'><script>x</script></svg>"
                .getBytes(StandardCharsets.UTF_8);
        assertThat(MagicByteImageValidator.detectRasterMime(svg)).isNull();
    }

    @Test
    @DisplayName("an HTML document is rejected")
    void rejectsHtml() {
        assertThat(MagicByteImageValidator.detectRasterMime(
                "<!doctype html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)))
                .isNull();
    }

    @Test
    @DisplayName("null / empty / too-short input is rejected, not misdetected")
    void rejectsNullEmptyShort() {
        assertThat(MagicByteImageValidator.detectRasterMime(null)).isNull();
        assertThat(MagicByteImageValidator.detectRasterMime(new byte[0])).isNull();
        // A PNG prefix that is truncated before the full signature must not match.
        assertThat(MagicByteImageValidator.detectRasterMime(new byte[]{(byte) 0x89, 0x50})).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg", "image/gif", "image/webp", "IMAGE/PNG", " image/webp "})
    @DisplayName("isAllowedRasterMime accepts the allow-list case-insensitively")
    void allowedMimes(String mime) {
        assertThat(MagicByteImageValidator.isAllowedRasterMime(mime)).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"image/svg+xml", "text/html", "application/octet-stream", "", "png"})
    @DisplayName("isAllowedRasterMime rejects SVG and everything off the allow-list")
    void disallowedMimes(String mime) {
        assertThat(MagicByteImageValidator.isAllowedRasterMime(mime)).isFalse();
    }
}
