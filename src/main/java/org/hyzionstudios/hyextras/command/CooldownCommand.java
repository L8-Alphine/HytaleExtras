package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.service.CooldownService;

import java.util.concurrent.CompletableFuture;

/**
 * Sub-command group: {@code /hextras cooldown}.
 * Sub-commands: {@code check}, {@code clear}.
 */
public class CooldownCommand extends AbstractCommandCollection {

    public CooldownCommand(CooldownService cd) {
        super("cooldown", "Manage HyExtras named cooldowns");
        addSubCommand(new CheckCmd(cd));
        addSubCommand(new ClearCmd(cd));
    }

    // --- /hextras cooldown check <player> <name> ---
    private static final class CheckCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> nameArg;
        private final CooldownService cd;

        CheckCmd(CooldownService cd) {
            super("check", "Check remaining cooldown time");
            this.cd = cd;
            this.playerArg = withRequiredArg("player", "The target player",  ArgTypes.PLAYER_REF);
            this.nameArg   = withRequiredArg("name",   "Cooldown name",       ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String name = ctx.get(nameArg);
            float remaining = cd.remainingSeconds(target.getUuid(), name);
            String status = (remaining <= 0f)
                    ? "ready"
                    : String.format("%.1fs remaining", remaining);
            ctx.sendMessage(msg(target.getUsername() + " cooldown '" + name + "': " + status));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- /hextras cooldown clear <player> <name> ---
    private static final class ClearCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> nameArg;
        private final CooldownService cd;

        ClearCmd(CooldownService cd) {
            super("clear", "Clear a named cooldown for a player");
            this.cd = cd;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
            this.nameArg   = withRequiredArg("name",   "Cooldown name",      ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String name = ctx.get(nameArg);
            cd.clear(target.getUuid(), name);
            ctx.sendMessage(msg("Cleared cooldown '" + name + "' for " + target.getUsername()));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- shared utility ---

    static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
