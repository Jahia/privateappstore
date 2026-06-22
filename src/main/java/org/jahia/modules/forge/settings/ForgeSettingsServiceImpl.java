package org.jahia.modules.forge.settings;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * {@link ForgeSettingsService} implemented as an OSGi {@link ManagedServiceFactory} keyed by site:
 * each site's settings are a <em>factory configuration</em> of PID {@value #FACTORY_PID}, carried by
 * a file {@code <KARAF_ETC>/org.jahia.modules.forge.forgeSettings-<siteKey>.cfg}.
 *
 * <p>Felix FileInstall turns every such {@code .cfg} into a configuration instance and the OSGi
 * configuration runtime delivers it to {@link #updated(String, Dictionary)}, keyed by its
 * {@code siteKey} property; {@link #get(String)} then serves the live in-memory map. Because the
 * configuration lives in a file, it survives restarts and redeploys unconditionally and can be
 * hand-edited in {@code karaf/etc} (FileInstall re-delivers the change). This replaces an earlier
 * design that created the per-site configuration programmatically through {@code ConfigurationAdmin}:
 * once a default config file shipped for the same factory PID, those API-created instances were
 * dropped on restart and the store configuration was lost.
 *
 * <p>The admin UI saves by writing that same file ({@link #save}/{@link #delete}), so there is one
 * configuration representation consumed one way. The password is stored base64-obfuscated (same
 * reversible obfuscation as before — an accepted risk; a follow-up should use a real secret store).
 */
@Component(
        service = {ForgeSettingsService.class, ManagedServiceFactory.class},
        property = Constants.SERVICE_PID + "=" + ForgeSettingsServiceImpl.FACTORY_PID,
        immediate = true)
public class ForgeSettingsServiceImpl implements ForgeSettingsService, ManagedServiceFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeSettingsServiceImpl.class);

    static final String FACTORY_PID = "org.jahia.modules.forge.forgeSettings";

    /** Karaf system properties locating {@code <JAHIA_HOME>/digital-factory-data/karaf/etc}. */
    private static final String KARAF_ETC = "karaf.etc";
    private static final String KARAF_HOME = "karaf.home";

    /**
     * A site key is interpolated into the config file name, so it must be a safe single path
     * segment — alphanumeric start, then alphanumerics, dot, underscore or hyphen. This rejects any
     * separator or {@code ..} traversal before it can reach the file system.
     */
    private static final Pattern SAFE_SITE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

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

    private static final String FILE_HEADER =
            "Jahia private app store - per-site settings. Managed by the store administration UI; "
            + "safe to hand-edit. 'siteKey' selects the site these settings apply to.";

    /** Live per-site settings, populated from configuration instances delivered to {@link #updated}. */
    private final ConcurrentHashMap<String, ForgeSettings> bySite = new ConcurrentHashMap<>();
    /** Configuration instance PID -> the site key it carries, so {@link #deleted} can evict by site. */
    private final ConcurrentHashMap<String, String> pidToSite = new ConcurrentHashMap<>();

    // --- ManagedServiceFactory: receive per-site factory configuration instances ---------------

    @Override
    public String getName() {
        return "Private App Store per-site settings";
    }

    @Override
    public void updated(String pid, Dictionary<String, ?> properties) throws ConfigurationException {
        final String siteKey = str(properties::get, P_SITE_KEY);
        if (StringUtils.isBlank(siteKey)) {
            // The shipped template ships with an empty siteKey; ignore it until an admin sets one.
            LOGGER.debug("Ignoring forge settings configuration {} with no siteKey", pid);
            return;
        }
        final String previousSite = pidToSite.put(pid, siteKey);
        if (previousSite != null && !previousSite.equals(siteKey)) {
            bySite.remove(previousSite);
        }
        bySite.put(siteKey, toSettings(key -> str(properties::get, key)));
        LOGGER.debug("Loaded forge settings for site '{}' (configuration {})", siteKey, pid);
    }

    @Override
    public void deleted(String pid) {
        final String siteKey = pidToSite.remove(pid);
        if (siteKey != null) {
            bySite.remove(siteKey);
            LOGGER.debug("Removed forge settings for site '{}' (configuration {})", siteKey, pid);
        }
    }

    // --- ForgeSettingsService ------------------------------------------------------------------

    @Override
    public ForgeSettings get(String siteKey) {
        final ForgeSettings settings = bySite.get(siteKey);
        return (settings != null) ? settings : ForgeSettings.empty();
    }

    @Override
    public void save(String siteKey, ForgeSettings settings) {
        final File file = configFile(siteKey);
        if (file == null) {
            throw new ForgeSettingsConfigException(
                    "Refusing to persist forge settings: unusable site key '" + siteKey + "' or no karaf/etc", null);
        }
        // A blank password leaves the previously stored (base64) value untouched, so carry it over.
        final String keptPassword = loadExisting(file).getProperty(P_PASSWORD);

        final Properties props = new Properties();
        props.setProperty(P_SITE_KEY, siteKey);
        putOrSkip(props, P_URL, settings.getUrl());
        putOrSkip(props, P_ID, settings.getId());
        putOrSkip(props, P_USER, settings.getUser());
        putOrSkip(props, P_LOGO_PATH, settings.getLogoPath());
        putOrSkip(props, P_COPYRIGHT, settings.getCopyright());
        putOrSkip(props, P_PRIVACY_URL, settings.getPrivacyUrl());
        putOrSkip(props, P_TERMS_URL, settings.getTermsUrl());
        putOrSkip(props, P_COOKIES_URL, settings.getCookiesUrl());
        putOrSkip(props, P_FACEBOOK_URL, settings.getFacebookUrl());
        putOrSkip(props, P_LINKEDIN_URL, settings.getLinkedinUrl());
        putOrSkip(props, P_TWITTER_URL, settings.getTwitterUrl());
        putOrSkip(props, P_YOUTUBE_URL, settings.getYoutubeUrl());
        putOrSkip(props, P_ROOT_CATEGORY, settings.getRootCategoryUuid());
        if (StringUtils.isNotBlank(settings.getPassword())) {
            props.setProperty(P_PASSWORD, Base64.getEncoder()
                    .encodeToString(settings.getPassword().getBytes(StandardCharsets.UTF_8)));
        } else if (StringUtils.isNotBlank(keptPassword)) {
            props.setProperty(P_PASSWORD, keptPassword);
        }

        try {
            writeAtomic(file, props);
        } catch (IOException e) {
            // Surface so the GraphQL mutation reports a structured error rather than failing silently.
            throw new ForgeSettingsConfigException("Unable to persist forge settings for site " + siteKey, e);
        }
        // Reflect the change immediately; FileInstall will re-deliver the same data via updated().
        bySite.put(siteKey, toSettings(props::getProperty));
    }

    @Override
    public void delete(String siteKey) {
        final File file = configFile(siteKey);
        bySite.remove(siteKey);
        if (file == null || !file.isFile()) {
            return;
        }
        try {
            // FileInstall observes the removal and calls deleted(pid); the eviction above is immediate.
            Files.delete(file.toPath());
        } catch (IOException e) {
            LOGGER.error("Unable to delete forge settings for site {}", siteKey, e);
        }
    }

    // --- file helpers --------------------------------------------------------------------------

    /**
     * The {@code .cfg} file for a site, or {@code null} when the site key is unsafe or no
     * {@code karaf/etc} can be located. Package-private for tests.
     */
    File configFile(String siteKey) {
        if (!isSafeSiteKey(siteKey)) {
            return null;
        }
        final File etc = etcDir();
        return (etc != null) ? new File(etc, configFileName(siteKey)) : null;
    }

    /** The config file name for a site key. Package-private and pure, for tests. */
    static String configFileName(String siteKey) {
        return FACTORY_PID + "-" + siteKey + ".cfg";
    }

    /** Whether a site key is a safe single file-name segment. Package-private and pure, for tests. */
    static boolean isSafeSiteKey(String siteKey) {
        return StringUtils.isNotBlank(siteKey) && SAFE_SITE_KEY.matcher(siteKey).matches();
    }

    private static File etcDir() {
        File dir = null;
        final String etc = System.getProperty(KARAF_ETC);
        if (StringUtils.isNotBlank(etc)) {
            dir = new File(etc);
        } else {
            final String home = System.getProperty(KARAF_HOME);
            if (StringUtils.isNotBlank(home)) {
                dir = new File(home, "etc");
            }
        }
        if (dir == null || !dir.isDirectory()) {
            LOGGER.error("Cannot locate karaf/etc (system properties '{}'/'{}'); forge settings cannot be saved",
                    KARAF_ETC, KARAF_HOME);
            return null;
        }
        return dir;
    }

    private static Properties loadExisting(File file) {
        final Properties props = new Properties();
        if (file.isFile()) {
            try {
                try (java.io.InputStream in = Files.newInputStream(file.toPath())) {
                    props.load(in);
                }
            } catch (IOException e) {
                LOGGER.warn("Unable to read existing forge settings file {}; treating as empty", file, e);
            }
        }
        return props;
    }

    /** Write properties to a temp file in the same directory, then move it into place atomically. */
    private static void writeAtomic(File file, Properties props) throws IOException {
        final Path target = file.toPath();
        final Path tmp = Files.createTempFile(target.getParent(), file.getName(), ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, FILE_HEADER);
            }
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicUnsupported) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // --- mapping -------------------------------------------------------------------------------

    /** Build settings from any string-keyed source (a config {@link Dictionary} or a {@link Properties}). */
    private static ForgeSettings toSettings(Function<String, String> get) {
        return ForgeSettings.builder()
                .url(blankToNull(get.apply(P_URL)))
                .id(blankToNull(get.apply(P_ID)))
                .user(blankToNull(get.apply(P_USER)))
                .password(decodePassword(get.apply(P_PASSWORD)))
                .logoPath(blankToNull(get.apply(P_LOGO_PATH)))
                .copyright(blankToNull(get.apply(P_COPYRIGHT)))
                .privacyUrl(blankToNull(get.apply(P_PRIVACY_URL)))
                .termsUrl(blankToNull(get.apply(P_TERMS_URL)))
                .cookiesUrl(blankToNull(get.apply(P_COOKIES_URL)))
                .facebookUrl(blankToNull(get.apply(P_FACEBOOK_URL)))
                .linkedinUrl(blankToNull(get.apply(P_LINKEDIN_URL)))
                .twitterUrl(blankToNull(get.apply(P_TWITTER_URL)))
                .youtubeUrl(blankToNull(get.apply(P_YOUTUBE_URL)))
                .rootCategoryUuid(blankToNull(get.apply(P_ROOT_CATEGORY)))
                .build();
    }

    /** Read a value from a config dictionary as a string (null when absent). */
    private static String str(Function<String, Object> get, String key) {
        final Object value = get.apply(key);
        return (value != null) ? value.toString() : null;
    }

    /** Normalise blank (e.g. a {@code url=} line in the shipped template) to null. */
    private static String blankToNull(String value) {
        return StringUtils.isNotBlank(value) ? value : null;
    }

    /**
     * Decode the stored password. {@link #save} (and the admin UI) store it base64-encoded, but a
     * value hand-typed into the {@code karaf/etc} .cfg may not be valid base64 — fall back to the
     * raw value instead of letting {@link Base64} throw, which would break every {@code get()} for
     * the site (the proxy, the upload deploy, the storefront chrome all call it).
     */
    static String decodePassword(String stored) {
        if (StringUtils.isBlank(stored)) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(stored), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return stored;
        }
    }

    private static void putOrSkip(Properties props, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            props.setProperty(key, value);
        }
    }

    /** Lets a config I/O failure surface through the JCR-exception-based mutation error path. */
    static final class ForgeSettingsConfigException extends RuntimeException {
        ForgeSettingsConfigException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
