package org.hyzionstudios.hyextras;

import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.modules.i18n.parser.LangFileParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;

/**
 * Defensive loader for HyExtras editor translations.
 *
 * <p>Hytale's Trigger Volume editor receives labels from the server translation
 * packet. This merges this plugin's asset language file at startup while leaving
 * Hytale's native language pipeline untouched.
 */
public final class I18nDefaultsLoader {

    private static final String RESOURCE = "Server.Languages.en-US/server.lang";
    private static final String PREFIX = "server.";
    private static final String DEFAULT_LANGUAGE = "en-US";

    private I18nDefaultsLoader() {}

    @SuppressWarnings("unchecked")
    public static void load(HyExtrasPlugin plugin) {
        InputStream in = I18nDefaultsLoader.class.getClassLoader().getResourceAsStream(RESOURCE);
        if (in == null) {
            plugin.getLogger().at(Level.WARNING)
                    .log("[hextras i18n] Missing language resource: " + RESOURCE);
            return;
        }

        try (InputStream selectedIn = in) {
            Map<String, String> parsed = LangFileParser.parse(
                    new BufferedReader(new InputStreamReader(selectedIn, StandardCharsets.UTF_8)));

            I18nModule i18n = I18nModule.get();
            if (i18n == null) {
                plugin.getLogger().at(Level.WARNING)
                        .log("[hextras i18n] I18nModule unavailable; editor labels may show raw keys.");
                return;
            }

            Field bundledDefaultsField = I18nModule.class.getDeclaredField("bundledDefaults");
            bundledDefaultsField.setAccessible(true);
            Map<String, String> bundledDefaults = (Map<String, String>) bundledDefaultsField.get(i18n);

            Field languagesField = I18nModule.class.getDeclaredField("languages");
            languagesField.setAccessible(true);
            Map<String, Map<String, String>> languages =
                    (Map<String, Map<String, String>>) languagesField.get(i18n);
            Map<String, String> defaultLanguage = languages.computeIfAbsent(DEFAULT_LANGUAGE,
                    ignored -> new java.util.concurrent.ConcurrentHashMap<>());

            int added = 0;
            for (Map.Entry<String, String> entry : parsed.entrySet()) {
                String key = entry.getKey().startsWith(PREFIX)
                        ? entry.getKey()
                        : PREFIX + entry.getKey();
                bundledDefaults.put(key, entry.getValue());
                defaultLanguage.put(key, entry.getValue());
                added++;
            }

            Field cachedLanguagesField = I18nModule.class.getDeclaredField("cachedLanguages");
            cachedLanguagesField.setAccessible(true);
            Map<?, ?> cachedLanguages = (Map<?, ?>) cachedLanguagesField.get(i18n);
            cachedLanguages.clear();

            plugin.getLogger().at(Level.INFO)
                    .log("[hextras i18n] Loaded " + added + " editor translations from " + RESOURCE + ".");
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras i18n] Could not merge editor translation defaults; Hytale i18n internals may have changed.");
        }
    }
}
