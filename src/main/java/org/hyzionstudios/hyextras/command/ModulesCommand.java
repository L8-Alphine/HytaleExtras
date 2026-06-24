package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.hyzionstudios.hyextras.module.InternalModule;
import org.hyzionstudios.hyextras.module.InternalModuleManager;

import java.util.concurrent.CompletableFuture;

/** {@code /hextras modules} - lists internal HyExtras modules. */
public final class ModulesCommand extends AbstractAsyncCommand {

    private final InternalModuleManager modules;

    public ModulesCommand(InternalModuleManager modules) {
        super("modules", "List internal HyExtras modules");
        this.modules = modules;
        requirePermission("hyextras.module.list");
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        StringBuilder out = new StringBuilder("HyExtras modules:");
        for (InternalModule module : modules.modules()) {
            String id = module.metadata().id();
            out.append("\n")
                    .append(id)
                    .append(" - ")
                    .append(modules.state(id));
        }
        ctx.sendMessage(msg(out.toString()));
        return CompletableFuture.completedFuture(null);
    }

    static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
