package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

public final class TagNpcModule implements InternalModule {

    private static final InternalModuleMetadata METADATA = new InternalModuleMetadata(
            HyExtrasConfig.MODULE_TAG_NPC,
            "TagNPC",
            "Runtime tags, variables, and visibility state for UUID-backed NPCs, mobs, and entities.",
            true,
            true);

    @Override
    public InternalModuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public void onEnable(HyExtrasPlugin plugin) {
        plugin.getTagNpcService().start();
    }

    @Override
    public void onDisable(HyExtrasPlugin plugin) {
        plugin.getTagNpcService().stop();
    }

    @Override
    public void onReload(HyExtrasPlugin plugin) {
        plugin.getTagNpcService().start();
    }
}
