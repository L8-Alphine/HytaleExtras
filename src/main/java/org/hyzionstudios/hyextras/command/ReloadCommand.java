package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /hextras reload} — reloads {@code hyextras.properties} at runtime.
 * Does not re-register effects or conditions; only refreshes config flags.
 */
public class ReloadCommand extends AbstractAsyncCommand {

    public ReloadCommand() {
        super("reload", "Reload the HyExtras configuration file");
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        HyExtrasPlugin.get().reloadConfig();
        ctx.sendMessage(msg("HyExtras configuration reloaded."));
        return CompletableFuture.completedFuture(null);
    }

    private static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
