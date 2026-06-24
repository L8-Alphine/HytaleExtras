package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.hyzionstudios.hyextras.triggerextras.TriggerActionRegistry;
import org.hyzionstudios.hyextras.triggerextras.TriggerConditionRegistry;

import java.util.concurrent.CompletableFuture;

/**
 * Sub-command group: {@code /hextras list}.
 * Sub-commands: {@code actions}, {@code conditions}.
 */
public class ListCommand extends AbstractCommandCollection {

    public ListCommand() {
        super("list", "List registered HyExtras effects and conditions");
        addSubCommand(new ActionsCmd());
        addSubCommand(new ConditionsCmd());
    }

    // --- /hextras list actions ---
    private static final class ActionsCmd extends AbstractAsyncCommand {

        ActionsCmd() { super("actions", "List all registered extra effect type IDs"); }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            String ids = String.join(", ", TriggerActionRegistry.TYPE_IDS);
            ctx.sendMessage(msg("Extra effects (" + TriggerActionRegistry.TYPE_IDS.size() + "): " + ids));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- /hextras list conditions ---
    private static final class ConditionsCmd extends AbstractAsyncCommand {

        ConditionsCmd() { super("conditions", "List all registered extra condition type IDs"); }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            String ids = String.join(", ", TriggerConditionRegistry.TYPE_IDS);
            ctx.sendMessage(msg("Extra conditions (" + TriggerConditionRegistry.TYPE_IDS.size() + "): " + ids));
            return CompletableFuture.completedFuture(null);
        }
    }

    static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
