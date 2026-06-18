package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.service.PlayerVariableService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Sub-command group: {@code /hextras var}.
 * Sub-commands: {@code get}, {@code set}, {@code del}, {@code list}.
 */
public class VarCommand extends AbstractCommandCollection {

    public VarCommand(PlayerVariableService vars) {
        super("var", "Manage per-player variables");
        addSubCommand(new GetCmd(vars));
        addSubCommand(new SetCmd(vars));
        addSubCommand(new AddCmd("add", "Add to a numeric player variable", vars));
        addSubCommand(new AddCmd("increment", "Increment a numeric player variable", vars));
        addSubCommand(new DelCmd(vars));
        addSubCommand(new ListCmd(vars));
    }

    // --- /hextras var get <player> <key> ---
    private static final class GetCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> keyArg;
        private final PlayerVariableService vars;

        GetCmd(PlayerVariableService vars) {
            super("get", "Get a player variable value");
            this.vars = vars;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
            this.keyArg    = withRequiredArg("key",    "Variable key",      ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String key = ctx.get(keyArg);
            Object value = vars.get(target.getUuid(), key);
            String display = (value != null) ? value.toString() : "(not set)";
            ctx.sendMessage(msg(target.getUsername() + "." + key + " = " + display));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- /hextras var set <player> <key> <value> ---
    private static final class SetCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> keyArg;
        private final RequiredArg<String> valueArg;
        private final PlayerVariableService vars;

        SetCmd(PlayerVariableService vars) {
            super("set", "Set a player variable");
            this.vars = vars;
            this.playerArg = withRequiredArg("player", "The target player",  ArgTypes.PLAYER_REF);
            this.keyArg    = withRequiredArg("key",    "Variable key",        ArgTypes.STRING);
            this.valueArg  = withRequiredArg("value",  "Value to assign",     ArgTypes.GREEDY_STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String key   = ctx.get(keyArg);
            String value = ctx.get(valueArg);
            vars.set(target.getUuid(), key, value);
            ctx.sendMessage(msg("Set " + target.getUsername() + "." + key + " = " + value));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- /hextras var add <player> <key> <amount> ---
    private static final class AddCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> keyArg;
        private final RequiredArg<Integer> amountArg;
        private final PlayerVariableService vars;

        AddCmd(String name, String description, PlayerVariableService vars) {
            super(name, description);
            this.vars = vars;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
            this.keyArg = withRequiredArg("key", "Variable key", ArgTypes.STRING);
            this.amountArg = withRequiredArg("amount", "Amount to add", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String key = ctx.get(keyArg);
            int amount = ctx.get(amountArg);
            long value = vars.increment(target.getUuid(), key, amount);
            ctx.sendMessage(msg("Set " + target.getUsername() + "." + key + " = " + value));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- /hextras var del <player> <key> ---
    private static final class DelCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> keyArg;
        private final PlayerVariableService vars;

        DelCmd(PlayerVariableService vars) {
            super("del", "Delete a player variable");
            this.vars = vars;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
            this.keyArg    = withRequiredArg("key",    "Variable key",      ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String key = ctx.get(keyArg);
            vars.remove(target.getUuid(), key);
            ctx.sendMessage(msg("Deleted " + target.getUsername() + "." + key));
            return CompletableFuture.completedFuture(null);
        }
    }

    // --- /hextras var list <player> ---
    private static final class ListCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final PlayerVariableService vars;

        ListCmd(PlayerVariableService vars) {
            super("list", "List all variables for a player");
            this.vars = vars;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            Map<String, Object> snapshot = vars.snapshot(target.getUuid());
            if (snapshot.isEmpty()) {
                ctx.sendMessage(msg(target.getUsername() + " has no variables."));
            } else {
                ctx.sendMessage(msg("Variables for " + target.getUsername() + ":"));
                for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
                    ctx.sendMessage(msg("  " + entry.getKey() + " = " + entry.getValue()));
                }
            }
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
