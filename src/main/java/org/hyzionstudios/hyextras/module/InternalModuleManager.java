package org.hyzionstudios.hyextras.module;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.ConfigLoader;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public final class InternalModuleManager {

    private final HyExtrasPlugin plugin;
    private final Map<String, ManagedModule> modules = new LinkedHashMap<>();

    public InternalModuleManager(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    public void register(InternalModule module) {
        String id = normalize(module.metadata().id());
        if (modules.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate internal module id: " + id);
        }
        modules.put(id, new ManagedModule(module, InternalModuleState.REGISTERED, false));
        module.onRegister(plugin);
    }

    public void initializeFromConfig(HyExtrasConfig config) {
        for (ManagedModule managed : modules.values()) {
            String id = managed.module.metadata().id();
            if (config.module(id).enabled) {
                enableInternal(managed, false);
            } else {
                managed.state = InternalModuleState.DISABLED;
                managed.active = false;
            }
        }
    }

    public void refreshFromConfig(HyExtrasConfig config) {
        for (ManagedModule managed : modules.values()) {
            String id = managed.module.metadata().id();
            boolean shouldEnable = config.module(id).enabled;
            boolean enabled = managed.state == InternalModuleState.ENABLED;
            if (shouldEnable == enabled) {
                continue;
            }
            if (!managed.module.metadata().canToggleAtRuntime()) {
                managed.state = InternalModuleState.RESTART_REQUIRED;
                continue;
            }
            if (shouldEnable) {
                enableInternal(managed, true);
            } else {
                disableInternal(managed, true);
            }
        }
    }

    public ModuleToggleResult enable(String id, boolean persist) {
        return setEnabled(id, true, persist);
    }

    public ModuleToggleResult disable(String id, boolean persist) {
        return setEnabled(id, false, persist);
    }

    public ModuleToggleResult reload(String id) {
        Optional<ManagedModule> found = find(id);
        if (found.isEmpty()) {
            return missing(id);
        }
        ManagedModule managed = found.get();
        HyExtrasConfig.ModuleSettings settings = plugin.getExtrasConfig().module(managed.module.metadata().id());
        if (!managed.module.metadata().reloadable() || !settings.reloadable) {
            managed.state = InternalModuleState.RESTART_REQUIRED;
            return new ModuleToggleResult(false, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " requires a server restart to reload.");
        }
        if (managed.state != InternalModuleState.ENABLED) {
            return new ModuleToggleResult(false, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " is not enabled.");
        }
        try {
            managed.state = InternalModuleState.RELOADING;
            managed.module.onReload(plugin);
            managed.state = InternalModuleState.ENABLED;
            return new ModuleToggleResult(true, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " reloaded.");
        } catch (Exception e) {
            managed.state = InternalModuleState.FAILED;
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[HyExtras] Failed to reload module " + managed.module.metadata().id());
            return new ModuleToggleResult(false, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " failed to reload.");
        }
    }

    public boolean isEnabled(String id) {
        Optional<ManagedModule> found = find(id);
        return found.isPresent() && found.get().active;
    }

    public InternalModuleState state(String id) {
        return find(id).map(managed -> managed.state).orElse(InternalModuleState.DISABLED);
    }

    public Optional<InternalModule> module(String id) {
        return find(id).map(managed -> managed.module);
    }

    public Collection<InternalModule> modules() {
        return modules.values().stream().map(managed -> managed.module).toList();
    }

    private ModuleToggleResult setEnabled(String id, boolean enabled, boolean persist) {
        Optional<ManagedModule> found = find(id);
        if (found.isEmpty()) {
            return missing(id);
        }
        ManagedModule managed = found.get();
        String moduleId = managed.module.metadata().id();
        HyExtrasConfig.ModuleSettings settings = plugin.getExtrasConfig().module(moduleId);
        if (persist && !settings.allowInGameToggle) {
            return new ModuleToggleResult(false, moduleId, managed.state,
                    managed.module.metadata().displayName() + " cannot be toggled in-game.");
        }
        if (persist) {
            ConfigLoader.updateProperty(plugin.getDataDirectory(),
                    ConfigLoader.moduleEnabledKey(moduleId),
                    String.valueOf(enabled));
            settings.enabled = enabled;
        }
        if (!managed.module.metadata().canToggleAtRuntime()) {
            managed.state = InternalModuleState.RESTART_REQUIRED;
            return new ModuleToggleResult(false, moduleId, managed.state,
                    managed.module.metadata().displayName() + " will change after a server restart.");
        }
        return enabled ? enableInternal(managed, true) : disableInternal(managed, true);
    }

    private ModuleToggleResult enableInternal(ManagedModule managed, boolean userVisible) {
        if (managed.state == InternalModuleState.ENABLED) {
            return new ModuleToggleResult(true, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " is already enabled.");
        }
        try {
            managed.active = true;
            managed.module.onEnable(plugin);
            managed.state = InternalModuleState.ENABLED;
            return new ModuleToggleResult(true, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " enabled.");
        } catch (Exception e) {
            managed.state = InternalModuleState.FAILED;
            managed.active = false;
            if (userVisible) {
                plugin.getLogger().at(Level.WARNING).withCause(e)
                        .log("[HyExtras] Failed to enable module " + managed.module.metadata().id());
            }
            return new ModuleToggleResult(false, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " failed to enable.");
        }
    }

    private ModuleToggleResult disableInternal(ManagedModule managed, boolean userVisible) {
        if (managed.state == InternalModuleState.DISABLED) {
            return new ModuleToggleResult(true, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " is already disabled.");
        }
        try {
            managed.active = false;
            managed.module.onDisable(plugin);
            managed.state = InternalModuleState.DISABLED;
            return new ModuleToggleResult(true, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " disabled.");
        } catch (Exception e) {
            managed.state = InternalModuleState.FAILED;
            managed.active = true;
            if (userVisible) {
                plugin.getLogger().at(Level.WARNING).withCause(e)
                        .log("[HyExtras] Failed to disable module " + managed.module.metadata().id());
            }
            return new ModuleToggleResult(false, managed.module.metadata().id(), managed.state,
                    managed.module.metadata().displayName() + " failed to disable.");
        }
    }

    private Optional<ManagedModule> find(String id) {
        return Optional.ofNullable(modules.get(normalize(id)));
    }

    private static ModuleToggleResult missing(String id) {
        return new ModuleToggleResult(false, normalize(id), InternalModuleState.DISABLED,
                "Unknown HyExtras module: " + id);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT);
    }

    private static final class ManagedModule {
        private final InternalModule module;
        private InternalModuleState state;
        private boolean active;

        private ManagedModule(InternalModule module, InternalModuleState state, boolean active) {
            this.module = module;
            this.state = state;
            this.active = active;
        }
    }
}
