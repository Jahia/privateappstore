package org.jahia.modules.forge.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("ForgePrincipalType")
@GraphQLDescription("Whether a principal is a user or a group")
public enum PrincipalType {
    USER("u"),
    GROUP("g");

    private final String prefix;

    PrincipalType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String toPrincipalKey(String name) {
        return prefix + ":" + name;
    }

    public static PrincipalType fromPrincipalKey(String key) {
        if (key == null || key.length() < 2) {
            return null;
        }

        if (key.charAt(0) == 'u') {
            return USER;
        }

        if (key.charAt(0) == 'g') {
            return GROUP;
        }

        return null;
    }

    public static String stripPrefix(String key) {
        if (key == null) {
            return null;
        }

        final int idx = key.indexOf(':');
        return idx < 0 ? key : key.substring(idx + 1);
    }
}
