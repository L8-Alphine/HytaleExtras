package org.hyzionstudios.hyextras.module;

public record InternalModuleMetadata(
        String id,
        String displayName,
        String description,
        boolean reloadable,
        boolean canToggleAtRuntime) {
}
