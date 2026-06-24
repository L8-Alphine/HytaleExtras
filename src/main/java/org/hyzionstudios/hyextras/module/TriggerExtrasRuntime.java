package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

/** Shared guard for TriggerExtras actions, conditions, and bridge code. */
public final class TriggerExtrasRuntime {

    private TriggerExtrasRuntime() {}

    public static boolean isEnabled() {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        return plugin == null || plugin.isModuleEnabled(HyExtrasConfig.MODULE_TRIGGER_EXTRAS);
    }
}
