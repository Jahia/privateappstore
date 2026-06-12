package org.jahia.modules.forge.settings;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * {@link ForgeSettingsService} backed by per-site OSGi factory configuration
 * (PID factory {@value #FACTORY_PID}). One configuration instance per site, identified by a
 * stored {@code siteKey} property. The password is stored base64-obfuscated (same reversible
 * obfuscation as the previous JCR storage — an accepted risk; a follow-up should move it to a
 * real secret store).
 */
@Component(service = ForgeSettingsService.class, immediate = true)
public class ForgeSettingsServiceImpl implements ForgeSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsServiceImpl.class);

    static final String FACTORY_PID = "org.jahia.modules.forge.forgeSettings";

    private static final String P_SITE_KEY = "siteKey";
    private static final String P_URL = "url";
    private static final String P_ID = "id";
    private static final String P_USER = "user";
    private static final String P_PASSWORD = "password";
    private static final String P_LOGO_PATH = "logoPath";
    private static final String P_COPYRIGHT = "copyright";
    private static final String P_PRIVACY_URL = "privacyUrl";
    private static final String P_TERMS_URL = "termsUrl";
    private static final String P_COOKIES_URL = "cookiesUrl";
    private static final String P_FACEBOOK_URL = "facebookUrl";
    private static final String P_LINKEDIN_URL = "linkedinUrl";
    private static final String P_TWITTER_URL = "twitterUrl";
    private static final String P_YOUTUBE_URL = "youtubeUrl";
    private static final String P_ROOT_CATEGORY = "rootCategoryUuid";

    private final ConfigurationAdmin configAdmin;

    @Activate
    public ForgeSettingsServiceImpl(@Reference ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    @Override
    public ForgeSettings get(String siteKey) {
        try {
            final Configuration cfg = findConfig(siteKey);
            if (cfg == null || cfg.getProperties() == null) {
                return ForgeSettings.empty();
            }
            return toSettings(cfg.getProperties());
        } catch (IOException e) {
            LOGGER.error("Unable to read forge settings for site {}", siteKey, e);
            return ForgeSettings.empty();
        }
    }

    @Override
    public void save(String siteKey, ForgeSettings settings) {
        try {
            Configuration cfg = findConfig(siteKey);
            if (cfg == null) {
                // bound to no specific bundle ("?") so it survives module reinstall
                cfg = configAdmin.createFactoryConfiguration(FACTORY_PID, "?");
            }
            Dictionary<String, Object> d = cfg.getProperties();
            if (d == null) {
                d = new Hashtable<>();
            }
            d.put(P_SITE_KEY, siteKey);
            putOrRemove(d, P_URL, settings.getUrl());
            putOrRemove(d, P_ID, settings.getId());
            putOrRemove(d, P_USER, settings.getUser());
            putOrRemove(d, P_LOGO_PATH, settings.getLogoPath());
            putOrRemove(d, P_COPYRIGHT, settings.getCopyright());
            putOrRemove(d, P_PRIVACY_URL, settings.getPrivacyUrl());
            putOrRemove(d, P_TERMS_URL, settings.getTermsUrl());
            putOrRemove(d, P_COOKIES_URL, settings.getCookiesUrl());
            putOrRemove(d, P_FACEBOOK_URL, settings.getFacebookUrl());
            putOrRemove(d, P_LINKEDIN_URL, settings.getLinkedinUrl());
            putOrRemove(d, P_TWITTER_URL, settings.getTwitterUrl());
            putOrRemove(d, P_YOUTUBE_URL, settings.getYoutubeUrl());
            putOrRemove(d, P_ROOT_CATEGORY, settings.getRootCategoryUuid());
            // Blank password leaves the previously stored (base64) value untouched.
            if (StringUtils.isNotBlank(settings.getPassword())) {
                d.put(P_PASSWORD, Base64.getEncoder()
                        .encodeToString(settings.getPassword().getBytes(StandardCharsets.UTF_8)));
            }
            cfg.update(d);
        } catch (IOException e) {
            // Surface as RepositoryException so the GraphQL mutation reports a structured error.
            throw new ForgeSettingsConfigException("Unable to persist forge settings for site " + siteKey, e);
        }
    }

    @Override
    public void delete(String siteKey) {
        try {
            final Configuration cfg = findConfig(siteKey);
            if (cfg != null) {
                cfg.delete();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to delete forge settings for site {}", siteKey, e);
        }
    }

    /** Find the single configuration instance for a site, or null when none exists yet. */
    private Configuration findConfig(String siteKey) throws IOException {
        try {
            final String filter = "(&(service.factoryPid=" + FACTORY_PID + ")(" + P_SITE_KEY + "="
                    + escape(siteKey) + "))";
            final Configuration[] cfgs = configAdmin.listConfigurations(filter);
            return (cfgs != null && cfgs.length > 0) ? cfgs[0] : null;
        } catch (org.osgi.framework.InvalidSyntaxException e) {
            LOGGER.error("Invalid configuration filter for site {}", siteKey, e);
            return null;
        }
    }

    private ForgeSettings toSettings(Dictionary<String, Object> d) {
        final String pw64 = str(d, P_PASSWORD);
        final String password = StringUtils.isNotBlank(pw64)
                ? new String(Base64.getDecoder().decode(pw64), StandardCharsets.UTF_8)
                : null;
        return ForgeSettings.builder()
                .url(str(d, P_URL))
                .id(str(d, P_ID))
                .user(str(d, P_USER))
                .password(password)
                .logoPath(str(d, P_LOGO_PATH))
                .copyright(str(d, P_COPYRIGHT))
                .privacyUrl(str(d, P_PRIVACY_URL))
                .termsUrl(str(d, P_TERMS_URL))
                .cookiesUrl(str(d, P_COOKIES_URL))
                .facebookUrl(str(d, P_FACEBOOK_URL))
                .linkedinUrl(str(d, P_LINKEDIN_URL))
                .twitterUrl(str(d, P_TWITTER_URL))
                .youtubeUrl(str(d, P_YOUTUBE_URL))
                .rootCategoryUuid(str(d, P_ROOT_CATEGORY))
                .build();
    }

    private static String str(Dictionary<String, Object> d, String key) {
        final Object v = d.get(key);
        return (v != null) ? v.toString() : null;
    }

    private static void putOrRemove(Dictionary<String, Object> d, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            d.put(key, value);
        } else {
            d.remove(key);
        }
    }

    /** Escape the value half of an LDAP/OSGi filter assertion. */
    static String escape(String value) {
        return value.replace("\\", "\\5c").replace("*", "\\2a")
                .replace("(", "\\28").replace(")", "\\29");
    }

    /** Lets a config I/O failure surface through the JCR-exception-based mutation error path. */
    static final class ForgeSettingsConfigException extends RuntimeException {
        ForgeSettingsConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
