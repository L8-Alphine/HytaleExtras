package org.hyzionstudios.hytaleextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.state.RuntimeStateStore;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sub-command group: {@code /hextras debug}.
 * Sub-commands: {@code player}.
 */
public class DebugCommand extends AbstractCommandCollection {

    public DebugCommand(RuntimeStateStore state) {
        super("debug", "Debug player trigger state");
        addSubCommand(new DebugPlayerCmd(state));
    }

    // --- /hextras debug player <player> ---
    private static final class DebugPlayerCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final RuntimeStateStore state;

        DebugPlayerCmd(RuntimeStateStore state) {
            super("player", "Show variable, cooldown, and visibility state for a player");
            this.state = state;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            if (target == null) {
                ctx.sendMessage(msg("Target player not found."));
                return CompletableFuture.completedFuture(null);
            }
            UUID uuid = target.getUuid();

            Map<String, Object> vars = state.vars().snapshot(uuid);
            Map<String, Float> cds  = state.cooldowns().snapshot(uuid);
            Set<UUID> hidden         = state.playerOverrides().snapshotHidden(uuid);
            Set<String> tags         = HytaleextrasPlugin.get().getTagService().snapshotTags(uuid);

            StringBuilder sb = new StringBuilder("=== ").append(target.getUsername()).append(" ===");

            sb.append("\nVariables (").append(vars.size()).append("):");
            if (vars.isEmpty()) {
                sb.append(" none");
            } else {
                vars.forEach((k, v) -> sb.append("\n  ").append(k).append(" = ").append(v));
            }

            sb.append("\nCooldowns (").append(cds.size()).append("):");
            if (cds.isEmpty()) {
                sb.append(" none");
            } else {
                cds.forEach((k, v) ->
                        sb.append("\n  ").append(k)
                          .append(": ").append(String.format("%.1f", v)).append("s remaining"));
            }

            sb.append("\nTags (").append(tags.size()).append("):");
            if (tags.isEmpty()) {
                sb.append(" none");
            } else {
                tags.forEach(t -> sb.append("\n  ").append(t));
            }

            sb.append("\nHidden from view (").append(hidden.size()).append("):");
            if (hidden.isEmpty()) {
                sb.append(" none");
            } else {
                hidden.forEach(id -> sb.append("\n  ").append(id));
            }

            ctx.sendMessage(msg(sb.toString()));
            return CompletableFuture.completedFuture(null);
        }
    }

    static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
