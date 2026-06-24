package org.hyzionstudios.hyextras.config;

import org.hyzionstudios.hyextras.util.MissingPlaceholderBehavior;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime configuration for HyExtras.
 * Loaded from {@code hyextras.properties} in the plugin data directory.
 */
public final class HyExtrasConfig {

    public static final String MODULE_TRIGGER_EXTRAS = "trigger_extras";
    public static final String MODULE_PLACEHOLDER_API = "placeholder_api";
    public static final String MODULE_PACKET_API = "packet_api";
    public static final String MODULE_IMAGE_ICONS = "image_icons";
    public static final String MODULE_TAG_NPC = "tag_npc";
    public static final String MODULE_FLOATING_ITEMS = "floating_items";

    /** Gates Phase 4 packet-backed per-player override actions. On by default. */
    public boolean advancedPacketActions = true;

    /**
     * Enables experimental outbound EntityUpdates filtering for non-player entities.
     * Off by default because it runs on the packet path and can affect player join timing.
     */
    public boolean entityPacketFiltering = false;

    /** Logs startup/preflight diagnostics before HyExtras completes startup. */
    public boolean startupDiagnostics = true;

    /** Periodically syncs volume tag visibility policy to player-to-player packets. */
    public boolean playerVisibilityPolicySync = true;

    /** Logs extra detail about condition/action evaluation to the server console. */
    public boolean debugMode = false;

    public final Map<String, ModuleSettings> modules = new LinkedHashMap<>();

    public boolean stringTemplateNativePlaceholdersEnabled = true;
    public boolean stringTemplatePlaceholderApiEnabled = true;
    public MissingPlaceholderBehavior stringTemplatePlaceholderApiMissingBehavior =
            MissingPlaceholderBehavior.KEEP_ORIGINAL;

    public boolean imageIconsHotReload = true;
    public boolean imageIconsRemoteCacheEnabled = true;
    public long imageIconsRemoteCacheMaxBytes = 5_242_880L;
    public float imageIconsDefaultVisibilityRadius = 48.0f;
    public int imageIconsMaxIconsPerViewer = 64;
    public float tagNpcDefaultVisibilityRadius = 64.0f;
    public boolean tagNpcClearStateOnEntityUnload = true;
    public boolean floatingItemsDefaultPersistent = false;
    public boolean floatingItemsDefaultIntangible = true;
    public float floatingItemsDefaultVisibilityRadius = 48.0f;
    public float floatingItemsDefaultBobAmplitude = 0.15f;
    public float floatingItemsDefaultRotationDegreesPerSecond = 45.0f;
    public int floatingItemsMaxItems = 512;

    public HyExtrasConfig() {
        modules.put(MODULE_TRIGGER_EXTRAS, new ModuleSettings(true, true, true));
        modules.put(MODULE_PLACEHOLDER_API, new ModuleSettings(true, true, true));
        modules.put(MODULE_PACKET_API, new ModuleSettings(true, false, false));
        modules.put(MODULE_IMAGE_ICONS, new ModuleSettings(true, true, true));
        modules.put(MODULE_TAG_NPC, new ModuleSettings(true, true, true));
        modules.put(MODULE_FLOATING_ITEMS, new ModuleSettings(true, true, true));
    }

    public ModuleSettings module(String id) {
        return modules.getOrDefault(id, new ModuleSettings(false, false, false));
    }

    public static HyExtrasConfig defaults() {
        return new HyExtrasConfig();
    }

    public static final class ModuleSettings {
        public boolean enabled;
        public boolean allowInGameToggle;
        public boolean reloadable;

        public ModuleSettings(boolean enabled, boolean allowInGameToggle, boolean reloadable) {
            this.enabled = enabled;
            this.allowInGameToggle = allowInGameToggle;
            this.reloadable = reloadable;
        }
    }
}
