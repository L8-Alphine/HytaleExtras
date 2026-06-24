package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

public final class FloatingItemsModule implements InternalModule {

    private static final InternalModuleMetadata METADATA = new InternalModuleMetadata(
            HyExtrasConfig.MODULE_FLOATING_ITEMS,
            "FloatingItems",
            "Decorative non-pickup floating item displays for API, commands, and TriggerExtras.",
            true,
            true);

    @Override
    public InternalModuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public void onEnable(HyExtrasPlugin plugin) {
        plugin.getFloatingItemService().start();
    }

    @Override
    public void onDisable(HyExtrasPlugin plugin) {
        plugin.getFloatingItemService().stop();
    }

    @Override
    public void onReload(HyExtrasPlugin plugin) {
        plugin.getFloatingItemService().reload();
    }
}
