package org.jahia.modules.forge.graphql;

/**
 * Thrown when a Private App Store review submission cannot be completed —
 * either because the request is invalid (rating out of range, not logged in,
 * already reviewed) or because the underlying JCR write failed.
 *
 * <p>It is a {@link RuntimeException} so the GraphQL runtime surfaces its
 * message to the caller without leaking JCR types, mirroring
 * {@code ForgeSettingsException}.
 */
public class ForgeReviewException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ForgeReviewException(String message) {
        super(message);
    }

    public ForgeReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
