package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;

public interface InternalModule {

    InternalModuleMetadata metadata();

    default void onRegister(HyExtrasPlugin plugin) {}

    default void onEnable(HyExtrasPlugin plugin) {}

    default void onDisable(HyExtrasPlugin plugin) {}

    default void onReload(HyExtrasPlugin plugin) {}
}
