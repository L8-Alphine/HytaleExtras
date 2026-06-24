package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.config.HyExtrasConfig;

public final class PlaceholderApiModule implements InternalModule {

    private static final InternalModuleMetadata METADATA = new InternalModuleMetadata(
            HyExtrasConfig.MODULE_PLACEHOLDER_API,
            "PlaceholderAPI",
            "Optional PlaceholderAPI bridge for StringTemplate percent placeholders.",
            true,
            true);

    @Override
    public InternalModuleMetadata metadata() {
        return METADATA;
    }
}
