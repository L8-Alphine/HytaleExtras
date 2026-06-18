package org.hyzionstudios.hyextras.config;

/**
 * Runtime configuration for HyExtras.
 * Loaded from {@code hyextras.properties} in the plugin data directory.
 */
public final class HyExtrasConfig {

    /** Gates Phase 4 packet-backed per-player override actions. On by default. */
    public boolean advancedPacketActions = true;

    /** Logs extra detail about condition/action evaluation to the server console. */
    public boolean debugMode = false;

    public static HyExtrasConfig defaults() {
        return new HyExtrasConfig();
    }
}
