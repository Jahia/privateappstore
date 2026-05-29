package org.jahia.modules.forge.graphql;

/**
 * Thrown when a Private App Store admin mutation fails to persist its
 * changes. Wraps the underlying {@code RepositoryException} (or any
 * other checked exception) without leaking JCR types to GraphQL callers.
 */
public class ForgeSettingsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ForgeSettingsException(String message, Throwable cause) {
        super(message, cause);
    }
}
