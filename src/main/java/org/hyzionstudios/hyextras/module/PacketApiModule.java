package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

public final class PacketApiModule implements InternalModule {

    private static final InternalModuleMetadata METADATA = new InternalModuleMetadata(
            HyExtrasConfig.MODULE_PACKET_API,
            "Packet API",
            "Packet-backed visibility, camera, title, and packet filtering services.",
            false,
            false);

    @Override
    public InternalModuleMetadata metadata() {
        return METADATA;
    }

    @Override
    public void onEnable(HyExtrasPlugin plugin) {
        plugin.applyPacketFeatureConfig();
    }

    @Override
    public void onDisable(HyExtrasPlugin plugin) {
        plugin.stopPacketFeatureServices();
    }

    @Override
    public void onReload(HyExtrasPlugin plugin) {
        plugin.applyPacketFeatureConfig();
    }
}
