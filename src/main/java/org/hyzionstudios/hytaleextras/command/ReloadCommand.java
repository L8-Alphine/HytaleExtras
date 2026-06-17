package org.hyzionstudios.hytaleextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /hextras reload} — reloads {@code hytaleextras.properties} at runtime.
 * Does not re-register effects or conditions; only refreshes config flags.
 */
public class ReloadCommand extends AbstractAsyncCommand {

    public ReloadCommand() {
        super("reload", "Reload the HytaleExtras configuration file");
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        HyextrasPlugin.get().reloadConfig();
        ctx.sendMessage(msg("HytaleExtras configuration reloaded."));
        return CompletableFuture.completedFuture(null);
    }

    private static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
