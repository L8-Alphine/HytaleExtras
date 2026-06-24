package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

public final class ImageIconsModule implements InternalModule {

    private static final InternalModuleMetadata METADATA = new InternalModuleMetadata(
            HyExtrasConfig.MODULE_IMAGE_ICONS,
            "ImageIcons",
            "Developer API for provider-scoped local/remote image icons and runtime attachments.",
            true,
            true);

    @Override
    public InternalModuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public void onEnable(HyExtrasPlugin plugin) {
        plugin.getImageIconService().start();
    }

    @Override
    public void onDisable(HyExtrasPlugin plugin) {
        plugin.getImageIconService().stop();
    }

    @Override
    public void onReload(HyExtrasPlugin plugin) {
        plugin.getImageIconService().reloadAllProviders();
    }
}
