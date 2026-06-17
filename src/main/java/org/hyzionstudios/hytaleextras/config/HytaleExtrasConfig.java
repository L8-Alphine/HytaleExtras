package org.hyzionstudios.hytaleextras.config;

/**
 * Runtime configuration for HytaleExtras.
 * Loaded from {@code hytaleextras.properties} in the plugin data directory.
 */
public final class HytaleExtrasConfig {

    /** Gates Phase 4 packet-backed per-player override actions. On by default. */
    public boolean advancedPacketActions = true;

    /** Logs extra detail about condition/action evaluation to the server console. */
    public boolean debugMode = false;

    public static HytaleExtrasConfig defaults() {
        return new HytaleExtrasConfig();
    }
}
