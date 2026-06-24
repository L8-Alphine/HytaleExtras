package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.InternalModule;
import org.hyzionstudios.hyextras.module.InternalModuleManager;
import org.hyzionstudios.hyextras.module.ModuleToggleResult;

import java.util.concurrent.CompletableFuture;

/** Sub-command group: {@code /hextras module}. */
public final class ModuleCommand extends AbstractCommandCollection {

    public ModuleCommand(InternalModuleManager modules) {
        super("module", "Manage internal HyExtras modules");
        addSubCommand(new InfoCmd(modules));
        addSubCommand(new EnableCmd(modules));
        addSubCommand(new DisableCmd(modules));
        addSubCommand(new ReloadCmd(modules));
    }

    private abstract static class ModuleLeafCommand extends AbstractAsyncCommand {
        final InternalModuleManager modules;
        final RequiredArg<String> moduleArg;

        ModuleLeafCommand(String name, String description, String permission, InternalModuleManager modules) {
            super(name, description);
            this.modules = modules;
            this.moduleArg = withRequiredArg("module", "Internal module id", ArgTypes.STRING);
            requirePermission(permission);
        }
    }

    private static final class InfoCmd extends ModuleLeafCommand {
        InfoCmd(InternalModuleManager modules) {
            super("info", "Show internal module details", "hyextras.module.info", modules);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            String id = ctx.get(moduleArg);
            InternalModule module = modules.module(id).orElse(null);
            if (module == null) {
                ctx.sendMessage(msg("Unknown HyExtras module: " + id));
                return CompletableFuture.completedFuture(null);
            }
            HyExtrasConfig.ModuleSettings settings = HyExtrasPlugin.get()
                    .getExtrasConfig()
                    .module(module.metadata().id());
            ctx.sendMessage(msg(module.metadata().displayName()
                    + "\nID: " + module.metadata().id()
                    + "\nState: " + modules.state(module.metadata().id())
                    + "\nDescription: " + module.metadata().description()
                    + "\nConfig enabled: " + settings.enabled
                    + "\nAllow in-game toggle: " + settings.allowInGameToggle
                    + "\nReloadable: " + settings.reloadable
                    + "\nRuntime toggle safe: " + module.metadata().canToggleAtRuntime()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class EnableCmd extends ModuleLeafCommand {
        EnableCmd(InternalModuleManager modules) {
            super("enable", "Enable an internal module", "hyextras.module.enable", modules);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            ModuleToggleResult result = modules.enable(ctx.get(moduleArg), true);
            ctx.sendMessage(msg(result.message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class DisableCmd extends ModuleLeafCommand {
        DisableCmd(InternalModuleManager modules) {
            super("disable", "Disable an internal module", "hyextras.module.disable", modules);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            ModuleToggleResult result = modules.disable(ctx.get(moduleArg), true);
            ctx.sendMessage(msg(result.message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ReloadCmd extends ModuleLeafCommand {
        ReloadCmd(InternalModuleManager modules) {
            super("reload", "Reload an internal module", "hyextras.module.reload", modules);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            ModuleToggleResult result = modules.reload(ctx.get(moduleArg));
            ctx.sendMessage(msg(result.message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
