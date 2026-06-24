package org.hyzionstudios.hyextras.config;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.util.MissingPlaceholderBehavior;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Loads {@link HyExtrasConfig} from {@code hyextras.properties} in the plugin data directory.
 * Writes a default file on first run if none exists and appends newly introduced
 * settings to existing config files without rewriting server-owner edits.
 */
public final class ConfigLoader {

    private static final String FILENAME = "hyextras.properties";
    private static final List<String> MODULE_IDS = List.of(
            HyExtrasConfig.MODULE_TRIGGER_EXTRAS,
            HyExtrasConfig.MODULE_PLACEHOLDER_API,
            HyExtrasConfig.MODULE_PACKET_API,
            HyExtrasConfig.MODULE_IMAGE_ICONS,
            HyExtrasConfig.MODULE_TAG_NPC,
            HyExtrasConfig.MODULE_FLOATING_ITEMS
    );

    private ConfigLoader() {}

    public static HyExtrasConfig load(Path dataDir) {
        HyExtrasConfig cfg = new HyExtrasConfig();
        Path file = dataDir.resolve(FILENAME);
        if (!Files.exists(file)) {
            writeDefaults(cfg, file);
            return cfg;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to read config — using defaults");
            return cfg;
        }
        appendMissingDefaults(props, cfg, file);
        cfg.advancedPacketActions = getBoolean(props, "advancedPacketActions", cfg.advancedPacketActions);
        cfg.entityPacketFiltering = getBoolean(props, "entityPacketFiltering", cfg.entityPacketFiltering);
        cfg.startupDiagnostics = getBoolean(props, "startupDiagnostics", cfg.startupDiagnostics);
        cfg.playerVisibilityPolicySync = getBoolean(
                props, "playerVisibilityPolicySync", cfg.playerVisibilityPolicySync);
        cfg.debugMode = getBoolean(props, "debugMode", cfg.debugMode);
        loadModuleConfig(props, cfg);
        cfg.stringTemplateNativePlaceholdersEnabled = getBoolean(
                props,
                "stringTemplate.nativePlaceholders.enabled",
                cfg.stringTemplateNativePlaceholdersEnabled);
        cfg.stringTemplatePlaceholderApiEnabled = getBoolean(
                props,
                "stringTemplate.placeholderApi.enabled",
                cfg.stringTemplatePlaceholderApiEnabled);
        cfg.stringTemplatePlaceholderApiMissingBehavior = getEnum(
                props,
                "stringTemplate.placeholderApi.missingBehavior",
                MissingPlaceholderBehavior.class,
                cfg.stringTemplatePlaceholderApiMissingBehavior);
        cfg.imageIconsHotReload = getBoolean(props, "imageIcons.hotReload", cfg.imageIconsHotReload);
        cfg.imageIconsRemoteCacheEnabled = getBoolean(
                props, "imageIcons.remoteCache.enabled", cfg.imageIconsRemoteCacheEnabled);
        cfg.imageIconsRemoteCacheMaxBytes = getLong(
                props, "imageIcons.remoteCache.maxBytes", cfg.imageIconsRemoteCacheMaxBytes);
        cfg.imageIconsDefaultVisibilityRadius = getFloat(
                props, "imageIcons.defaultVisibilityRadius", cfg.imageIconsDefaultVisibilityRadius);
        cfg.imageIconsMaxIconsPerViewer = getInt(
                props, "imageIcons.maxIconsPerViewer", cfg.imageIconsMaxIconsPerViewer);
        cfg.tagNpcDefaultVisibilityRadius = getFloat(
                props, "tagNpc.defaultVisibilityRadius", cfg.tagNpcDefaultVisibilityRadius);
        cfg.tagNpcClearStateOnEntityUnload = getBoolean(
                props, "tagNpc.clearStateOnEntityUnload", cfg.tagNpcClearStateOnEntityUnload);
        cfg.floatingItemsDefaultPersistent = getBoolean(
                props, "floatingItems.defaultPersistent", cfg.floatingItemsDefaultPersistent);
        cfg.floatingItemsDefaultIntangible = getBoolean(
                props, "floatingItems.defaultIntangible", cfg.floatingItemsDefaultIntangible);
        cfg.floatingItemsDefaultVisibilityRadius = getFloat(
                props, "floatingItems.defaultVisibilityRadius", cfg.floatingItemsDefaultVisibilityRadius);
        cfg.floatingItemsDefaultBobAmplitude = getFloat(
                props, "floatingItems.defaultBobAmplitude", cfg.floatingItemsDefaultBobAmplitude);
        cfg.floatingItemsDefaultRotationDegreesPerSecond = getFloat(
                props,
                "floatingItems.defaultRotationDegreesPerSecond",
                cfg.floatingItemsDefaultRotationDegreesPerSecond);
        cfg.floatingItemsMaxItems = getInt(props, "floatingItems.maxItems", cfg.floatingItemsMaxItems);
        return cfg;
    }

    public static boolean getBoolean(Properties props, String key, boolean fallback) {
        String raw = props.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(raw.trim())) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw.trim())) {
            return false;
        }
        return fallback;
    }

    public static String getString(Properties props, String key, String fallback) {
        String raw = props.getProperty(key);
        return raw != null ? raw : fallback;
    }

    public static int getInt(Properties props, String key, int fallback) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING)
                    .log("[HyExtras] Invalid config int " + key + "=" + raw + "; using " + fallback);
            return fallback;
        }
    }

    public static long getLong(Properties props, String key, long fallback) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING)
                    .log("[HyExtras] Invalid config long " + key + "=" + raw + "; using " + fallback);
            return fallback;
        }
    }

    public static float getFloat(Properties props, String key, float fallback) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING)
                    .log("[HyExtras] Invalid config float " + key + "=" + raw + "; using " + fallback);
            return fallback;
        }
    }

    public static <E extends Enum<E>> E getEnum(
            Properties props,
            String key,
            Class<E> enumType,
            E fallback) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING)
                    .log("[HyExtras] Invalid config enum " + key + "=" + raw + "; using " + fallback);
            return fallback;
        }
    }

    public static void loadModuleConfig(Properties props, HyExtrasConfig cfg) {
        for (String id : MODULE_IDS) {
            HyExtrasConfig.ModuleSettings settings = cfg.module(id);
            settings.enabled = getBoolean(props, moduleKey(id, "enabled"), settings.enabled);
            settings.allowInGameToggle = getBoolean(
                    props, moduleKey(id, "allowInGameToggle"), settings.allowInGameToggle);
            settings.reloadable = getBoolean(props, moduleKey(id, "reloadable"), settings.reloadable);
        }
    }

    public static boolean updateProperty(Path dataDir, String key, String value) {
        Path file = dataDir.resolve(FILENAME);
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                HyExtrasConfig cfg = new HyExtrasConfig();
                writeDefaults(cfg, file);
            }
            List<String> lines = Files.readAllLines(file);
            boolean replaced = false;
            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                String existingKey = trimmed.substring(0, trimmed.indexOf('=')).trim();
                if (existingKey.equals(key)) {
                    lines.set(i, key + "=" + value);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add("");
                lines.add("# Updated by HyExtras command");
                lines.add(key + "=" + value);
            }
            Files.write(file, lines);
            return true;
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to update config property " + key);
            return false;
        }
    }

    public static String moduleEnabledKey(String id) {
        return moduleKey(id, "enabled");
    }

    private static String moduleKey(String id, String field) {
        return "modules." + id + "." + field;
    }

    private static void appendMissingDefaults(Properties props, HyExtrasConfig cfg, Path file) {
        StringBuilder missing = new StringBuilder();
        appendMissing(missing, props, "advancedPacketActions", String.valueOf(cfg.advancedPacketActions), """

                # Enable per-player packet-backed features:
                # player_hide_entity/player_show_entity packets, titles, action bars, camera packets,
                # and high-level API packet helpers.
                """);
        appendMissing(missing, props, "entityPacketFiltering", String.valueOf(cfg.entityPacketFiltering), """

                # Experimental: filters outbound EntityUpdates for best-effort non-player entity hiding.
                # Leave false unless you are specifically testing non-player entity packet visibility.
                # If players time out while joining, set this back to false.
                """);
        appendMissing(missing, props, "startupDiagnostics", String.valueOf(cfg.startupDiagnostics), """

                # Print startup/preflight diagnostics for config, dependencies, packet filters,
                # duplicate HyExtras installs, and relevant Hytale plugin availability.
                """);
        appendMissing(missing, props, "playerVisibilityPolicySync", String.valueOf(cfg.playerVisibilityPolicySync), """

                # Periodically applies IsStoryArea/GroupArea volume visibility policy to player packets.
                # Recommended true so volume tags visually hide/show players without custom effects.
                """);
        appendMissing(missing, props, "debugMode", String.valueOf(cfg.debugMode), """

                # Print verbose runtime debug info, including player connect/disconnect state cleanup
                # and interaction bridge details.
                """);
        appendModuleDefaults(missing, props, cfg);
        appendMissing(missing, props,
                "stringTemplate.nativePlaceholders.enabled",
                String.valueOf(cfg.stringTemplateNativePlaceholdersEnabled), """

                # Resolve native HyExtras placeholders like {player}, {uuid}, and {variable:key}.
                """);
        appendMissing(missing, props,
                "stringTemplate.placeholderApi.enabled",
                String.valueOf(cfg.stringTemplatePlaceholderApiEnabled), """

                # Resolve PlaceholderAPI placeholders like %player_name% when PlaceholderAPI is installed.
                """);
        appendMissing(missing, props,
                "stringTemplate.placeholderApi.missingBehavior",
                cfg.stringTemplatePlaceholderApiMissingBehavior.name(), """

                # PlaceholderAPI missing behavior: KEEP_ORIGINAL, EMPTY, or ERROR.
                """);
        appendImageIconsDefaults(missing, props, cfg);

        if (missing.isEmpty()) {
            return;
        }

        try {
            String prefix = Files.size(file) > 0 ? "\n\n# Added by HyExtras config migration\n" : "";
            Files.writeString(file, prefix + missing, StandardOpenOption.APPEND);
            HyExtrasPlugin.get().getLogger()
                    .at(Level.INFO)
                    .log("[HyExtras] Updated hyextras.properties with missing default setting(s).");
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to append missing default config values");
        }
    }

    public static void appendModuleDefaults(StringBuilder missing, Properties props, HyExtrasConfig cfg) {
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_TRIGGER_EXTRAS, "enabled"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).enabled), """

                # Internal modules
                # TriggerExtras owns HyExtras trigger actions, conditions, and interaction bridge behavior.
                """);
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_TRIGGER_EXTRAS, "allowInGameToggle"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).allowInGameToggle), "");
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_TRIGGER_EXTRAS, "reloadable"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).reloadable), "");

        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_PLACEHOLDER_API, "enabled"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).enabled), """

                # PlaceholderAPI bridge for StringTemplate. Safe when PlaceholderAPI is not installed.
                """);
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_PLACEHOLDER_API, "allowInGameToggle"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).allowInGameToggle), "");
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_PLACEHOLDER_API, "reloadable"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).reloadable), "");

        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_PACKET_API, "enabled"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_PACKET_API).enabled), """

                # Packet API contains packet-backed visibility/camera/title services.
                """);
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_PACKET_API, "allowInGameToggle"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_PACKET_API).allowInGameToggle), "");
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_PACKET_API, "reloadable"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_PACKET_API).reloadable), "");

        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_IMAGE_ICONS, "enabled"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_IMAGE_ICONS).enabled), """

                # ImageIcons exposes provider-scoped local/remote image icon assets for developer mods.
                """);
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_IMAGE_ICONS, "allowInGameToggle"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_IMAGE_ICONS).allowInGameToggle), "");
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_IMAGE_ICONS, "reloadable"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_IMAGE_ICONS).reloadable), "");

        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_TAG_NPC, "enabled"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_TAG_NPC).enabled), """

                # TagNPC exposes runtime tags, variables, and visibility state for UUID-backed entities.
                """);
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_TAG_NPC, "allowInGameToggle"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_TAG_NPC).allowInGameToggle), "");
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_TAG_NPC, "reloadable"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_TAG_NPC).reloadable), "");

        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_FLOATING_ITEMS, "enabled"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).enabled), """

                # FloatingItems exposes decorative non-pickup item displays for API, commands, and TriggerExtras.
                """);
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_FLOATING_ITEMS, "allowInGameToggle"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).allowInGameToggle), "");
        appendMissing(missing, props,
                moduleKey(HyExtrasConfig.MODULE_FLOATING_ITEMS, "reloadable"),
                String.valueOf(cfg.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).reloadable), "");
    }

    private static void appendImageIconsDefaults(StringBuilder missing, Properties props, HyExtrasConfig cfg) {
        appendMissing(missing, props,
                "imageIcons.hotReload",
                String.valueOf(cfg.imageIconsHotReload), """

                # ImageIcons provider asset loading.
                # Watch registered provider folders recursively and reload changed PNG/GIF assets.
                """);
        appendMissing(missing, props,
                "imageIcons.remoteCache.enabled",
                String.valueOf(cfg.imageIconsRemoteCacheEnabled), """

                # Download remote PNG/GIF icons into the HyExtras cache before loading them.
                """);
        appendMissing(missing, props,
                "imageIcons.remoteCache.maxBytes",
                String.valueOf(cfg.imageIconsRemoteCacheMaxBytes), """

                # Maximum remote icon download size in bytes.
                """);
        appendMissing(missing, props,
                "imageIcons.defaultVisibilityRadius",
                String.valueOf(cfg.imageIconsDefaultVisibilityRadius), """

                # Default viewer radius for ImageIcons attachments when tuning does not override it.
                """);
        appendMissing(missing, props,
                "imageIcons.maxIconsPerViewer",
                String.valueOf(cfg.imageIconsMaxIconsPerViewer), """

                # Maximum ImageIcons attachments considered per viewer after priority sorting.
                """);
        appendMissing(missing, props,
                "tagNpc.defaultVisibilityRadius",
                String.valueOf(cfg.tagNpcDefaultVisibilityRadius), """

                # Default visibility radius reserved for TagNPC packet-backed features.
                """);
        appendMissing(missing, props,
                "tagNpc.clearStateOnEntityUnload",
                String.valueOf(cfg.tagNpcClearStateOnEntityUnload), """

                # Clear runtime TagNPC state when entity unload cleanup hooks are available.
                """);
        appendMissing(missing, props,
                "floatingItems.defaultPersistent",
                String.valueOf(cfg.floatingItemsDefaultPersistent), """

                # FloatingItems decorative item displays.
                # Runtime-only by default; set persistent per item through API, commands, or TriggerExtras.
                """);
        appendMissing(missing, props,
                "floatingItems.defaultIntangible",
                String.valueOf(cfg.floatingItemsDefaultIntangible), """

                # Default intangible state for new floating items.
                """);
        appendMissing(missing, props,
                "floatingItems.defaultVisibilityRadius",
                String.valueOf(cfg.floatingItemsDefaultVisibilityRadius), """

                # Default viewer radius for floating item displays.
                """);
        appendMissing(missing, props,
                "floatingItems.defaultBobAmplitude",
                String.valueOf(cfg.floatingItemsDefaultBobAmplitude), """

                # Default vertical bob amount for floating item displays.
                """);
        appendMissing(missing, props,
                "floatingItems.defaultRotationDegreesPerSecond",
                String.valueOf(cfg.floatingItemsDefaultRotationDegreesPerSecond), """

                # Default decorative rotation speed for floating item displays.
                """);
        appendMissing(missing, props,
                "floatingItems.maxItems",
                String.valueOf(cfg.floatingItemsMaxItems), """

                # Maximum runtime floating items tracked by HyExtras.
                """);
    }

    private static void appendMissing(
            StringBuilder out,
            Properties props,
            String key,
            String value,
            String comment) {
        if (props.containsKey(key)) {
            return;
        }
        out.append(comment).append(key).append('=').append(value).append('\n');
    }

    private static void writeDefaults(HyExtrasConfig cfg, Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                writer.write("""
                        # HyExtras Configuration
                        #
                        # Recommended defaults for new servers:
                        # - advancedPacketActions=true keeps supported per-player packet features enabled.
                        # - entityPacketFiltering=false keeps the experimental non-player EntityUpdates filter off.
                        # - startupDiagnostics=true prints preflight checks on startup so conflicts are visible.
                        # - playerVisibilityPolicySync=true makes volume visibility tags apply automatically.
                        #
                        # Enable per-player packet-backed features:
                        # player_hide_entity/player_show_entity packets, titles, action bars, camera packets,
                        # and high-level API packet helpers.
                        advancedPacketActions=%s

                        # Experimental: filters outbound EntityUpdates for best-effort non-player entity hiding.
                        # Leave false unless you are specifically testing non-player entity packet visibility.
                        # If players time out while joining, set this back to false.
                        entityPacketFiltering=%s

                        # Print startup/preflight diagnostics for config, dependencies, packet filters,
                        # duplicate HyExtras installs, and relevant Hytale plugin availability.
                        startupDiagnostics=%s

                        # Periodically applies IsStoryArea/GroupArea volume visibility policy to player packets.
                        # Recommended true so volume tags visually hide/show players without custom effects.
                        playerVisibilityPolicySync=%s

                        # Print verbose runtime debug info, including player connect/disconnect state cleanup
                        # and interaction bridge details.
                        debugMode=%s

                        # Internal modules
                        # TriggerExtras owns HyExtras trigger actions, conditions, and interaction bridge behavior.
                        modules.trigger_extras.enabled=%s
                        modules.trigger_extras.allowInGameToggle=%s
                        modules.trigger_extras.reloadable=%s

                        # PlaceholderAPI bridge for StringTemplate. Safe when PlaceholderAPI is not installed.
                        modules.placeholder_api.enabled=%s
                        modules.placeholder_api.allowInGameToggle=%s
                        modules.placeholder_api.reloadable=%s

                        # Packet API contains packet-backed visibility/camera/title services.
                        modules.packet_api.enabled=%s
                        modules.packet_api.allowInGameToggle=%s
                        modules.packet_api.reloadable=%s

                        # ImageIcons exposes provider-scoped local/remote image icon assets for developer mods.
                        modules.image_icons.enabled=%s
                        modules.image_icons.allowInGameToggle=%s
                        modules.image_icons.reloadable=%s

                        # TagNPC exposes runtime tags, variables, and visibility state for UUID-backed entities.
                        modules.tag_npc.enabled=%s
                        modules.tag_npc.allowInGameToggle=%s
                        modules.tag_npc.reloadable=%s

                        # FloatingItems exposes decorative non-pickup item displays for API, commands, and TriggerExtras.
                        modules.floating_items.enabled=%s
                        modules.floating_items.allowInGameToggle=%s
                        modules.floating_items.reloadable=%s

                        # StringTemplate
                        stringTemplate.nativePlaceholders.enabled=%s
                        stringTemplate.placeholderApi.enabled=%s
                        stringTemplate.placeholderApi.missingBehavior=%s

                        # ImageIcons provider asset loading.
                        # Watch registered provider folders recursively and reload changed PNG/GIF assets.
                        imageIcons.hotReload=%s

                        # Download remote PNG/GIF icons into the HyExtras cache before loading them.
                        imageIcons.remoteCache.enabled=%s

                        # Maximum remote icon download size in bytes.
                        imageIcons.remoteCache.maxBytes=%s

                        # Default viewer radius for ImageIcons attachments when tuning does not override it.
                        imageIcons.defaultVisibilityRadius=%s

                        # Maximum ImageIcons attachments considered per viewer after priority sorting.
                        imageIcons.maxIconsPerViewer=%s

                        # Default visibility radius reserved for TagNPC packet-backed features.
                        tagNpc.defaultVisibilityRadius=%s

                        # Clear runtime TagNPC state when entity unload cleanup hooks are available.
                        tagNpc.clearStateOnEntityUnload=%s

                        # FloatingItems decorative item displays.
                        # Runtime-only by default; set persistent per item through API, commands, or TriggerExtras.
                        floatingItems.defaultPersistent=%s

                        # Default intangible state for new floating items.
                        floatingItems.defaultIntangible=%s

                        # Default viewer radius for floating item displays.
                        floatingItems.defaultVisibilityRadius=%s

                        # Default vertical bob amount for floating item displays.
                        floatingItems.defaultBobAmplitude=%s

                        # Default decorative rotation speed for floating item displays.
                        floatingItems.defaultRotationDegreesPerSecond=%s

                        # Maximum runtime floating items tracked by HyExtras.
                        floatingItems.maxItems=%s
                        """.formatted(
                        cfg.advancedPacketActions,
                        cfg.entityPacketFiltering,
                        cfg.startupDiagnostics,
                        cfg.playerVisibilityPolicySync,
                        cfg.debugMode,
                        cfg.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).enabled,
                        cfg.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).allowInGameToggle,
                        cfg.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).reloadable,
                        cfg.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).enabled,
                        cfg.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).allowInGameToggle,
                        cfg.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).reloadable,
                        cfg.module(HyExtrasConfig.MODULE_PACKET_API).enabled,
                        cfg.module(HyExtrasConfig.MODULE_PACKET_API).allowInGameToggle,
                        cfg.module(HyExtrasConfig.MODULE_PACKET_API).reloadable,
                        cfg.module(HyExtrasConfig.MODULE_IMAGE_ICONS).enabled,
                        cfg.module(HyExtrasConfig.MODULE_IMAGE_ICONS).allowInGameToggle,
                        cfg.module(HyExtrasConfig.MODULE_IMAGE_ICONS).reloadable,
                        cfg.module(HyExtrasConfig.MODULE_TAG_NPC).enabled,
                        cfg.module(HyExtrasConfig.MODULE_TAG_NPC).allowInGameToggle,
                        cfg.module(HyExtrasConfig.MODULE_TAG_NPC).reloadable,
                        cfg.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).enabled,
                        cfg.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).allowInGameToggle,
                        cfg.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).reloadable,
                        cfg.stringTemplateNativePlaceholdersEnabled,
                        cfg.stringTemplatePlaceholderApiEnabled,
                        cfg.stringTemplatePlaceholderApiMissingBehavior.name(),
                        cfg.imageIconsHotReload,
                        cfg.imageIconsRemoteCacheEnabled,
                        cfg.imageIconsRemoteCacheMaxBytes,
                        cfg.imageIconsDefaultVisibilityRadius,
                        cfg.imageIconsMaxIconsPerViewer,
                        cfg.tagNpcDefaultVisibilityRadius,
                        cfg.tagNpcClearStateOnEntityUnload,
                        cfg.floatingItemsDefaultPersistent,
                        cfg.floatingItemsDefaultIntangible,
                        cfg.floatingItemsDefaultVisibilityRadius,
                        cfg.floatingItemsDefaultBobAmplitude,
                        cfg.floatingItemsDefaultRotationDegreesPerSecond,
                        cfg.floatingItemsMaxItems));
            }
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to write default config file");
        }
    }
}
