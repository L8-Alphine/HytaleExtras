package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.triggerextras.TriggerActionRegistry;
import org.hyzionstudios.hyextras.triggerextras.TriggerConditionRegistry;

import java.util.logging.Level;

public final class TriggerExtrasModule implements InternalModule {

    private static final InternalModuleMetadata METADATA = new InternalModuleMetadata(
            HyExtrasConfig.MODULE_TRIGGER_EXTRAS,
            "TriggerExtras",
            "HyExtras trigger actions, conditions, synthetic dispatch, and interaction bridge.",
            true,
            true);

    private static volatile boolean registeredTriggerTypes;

    @Override
    public InternalModuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public void onRegister(HyExtrasPlugin plugin) {
        registerTriggerTypes(plugin);
    }

    public static synchronized void registerTriggerTypes(HyExtrasPlugin plugin) {
        if (registeredTriggerTypes) {
            return;
        }

        boolean ok = true;
        ok &= TriggerActionRegistry.registerAll(plugin);
        ok &= TriggerConditionRegistry.registerAll(plugin);

        if (!ok) {
            plugin.getLogger().at(Level.WARNING)
                    .log("HyExtras: TriggerExtras type registration did not fully complete; will retry later.");
            return;
        }

        registeredTriggerTypes = true;
        plugin.getLogger().at(Level.INFO)
                .log("HyExtras: TriggerExtras registered " + TriggerActionRegistry.TYPE_IDS.size()
                        + " effects and " + TriggerConditionRegistry.TYPE_IDS.size() + " conditions.");
    }
}
