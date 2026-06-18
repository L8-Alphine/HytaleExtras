package org.hyzionstudios.hyextras.state;

import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.service.CooldownService;
import org.hyzionstudios.hyextras.service.PlayerVariableService;

/**
 * Unified accessor for all runtime player state and plugin config.
 * Passed to commands and services that need cross-cutting access.
 * Config is volatile so hot-reloads propagate without synchronization overhead.
 */
public final class RuntimeStateStore {

    private final PlayerVariableService vars;
    private final CooldownService cooldowns;
    private final PlayerOverrideService playerOverrides;
    private volatile HyExtrasConfig config;

    public RuntimeStateStore(
            PlayerVariableService vars,
            CooldownService cooldowns,
            PlayerOverrideService playerOverrides,
            HyExtrasConfig config) {
        this.vars = vars;
        this.cooldowns = cooldowns;
        this.playerOverrides = playerOverrides;
        this.config = config;
    }

    public PlayerVariableService vars() { return vars; }
    public CooldownService cooldowns() { return cooldowns; }
    public PlayerOverrideService playerOverrides() { return playerOverrides; }
    public HyExtrasConfig config() { return config; }

    /** Called by {@code /hextras reload} — replaces the config reference atomically. */
    public void updateConfig(HyExtrasConfig config) {
        this.config = config;
    }
}
