package org.hyzionstudios.hyextras.module;

public record ModuleToggleResult(
        boolean success,
        String moduleId,
        InternalModuleState state,
        String message) {
}
