package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.service.PlayerTagService;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Sub-command group: {@code /hextras tag}.
 * Sub-commands: {@code add}, {@code remove}, {@code set}, {@code has}, {@code list}, {@code clear}.
 */
public class TagCommand extends AbstractCommandCollection {

    public TagCommand(PlayerTagService tags) {
        super("tag", "Manage persistent per-player tags");
        addSubCommand(new AddCmd(tags));
        addSubCommand(new RemoveCmd(tags));
        addSubCommand(new SetCmd(tags));
        addSubCommand(new HasCmd(tags));
        addSubCommand(new ListCmd(tags));
        addSubCommand(new ClearCmd(tags));
    }

    private abstract static class TagMutationCommand extends AbstractAsyncCommand {
        final RequiredArg<PlayerRef> playerArg;
        final RequiredArg<String> tagArg;
        final PlayerTagService tags;

        TagMutationCommand(String name, String description, PlayerTagService tags) {
            super(name, description);
            this.tags = tags;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
            this.tagArg = withRequiredArg("tag", "Tag name", ArgTypes.STRING);
        }

        final void persist(PlayerRef target) {
            tags.savePlayer(target.getUuid());
        }
    }

    private static final class AddCmd extends TagMutationCommand {
        AddCmd(PlayerTagService tags) {
            super("add", "Add a persistent player tag", tags);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String tag = ctx.get(tagArg);
            tags.addTag(target.getUuid(), tag);
            persist(target);
            ctx.sendMessage(msg("Added tag '" + tag + "' to " + target.getUsername()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RemoveCmd extends TagMutationCommand {
        RemoveCmd(PlayerTagService tags) {
            super("remove", "Remove a persistent player tag", tags);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String tag = ctx.get(tagArg);
            tags.removeTag(target.getUuid(), tag);
            persist(target);
            ctx.sendMessage(msg("Removed tag '" + tag + "' from " + target.getUsername()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class SetCmd extends TagMutationCommand {
        private final RequiredArg<Boolean> valueArg;

        SetCmd(PlayerTagService tags) {
            super("set", "Set a persistent player tag to true or false", tags);
            this.valueArg = withRequiredArg("value", "true to add, false to remove", ArgTypes.BOOLEAN);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String tag = ctx.get(tagArg);
            boolean value = ctx.get(valueArg);
            if (value) {
                tags.addTag(target.getUuid(), tag);
            } else {
                tags.removeTag(target.getUuid(), tag);
            }
            persist(target);
            ctx.sendMessage(msg("Set tag '" + tag + "' for " + target.getUsername() + " = " + value));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class HasCmd extends TagMutationCommand {
        HasCmd(PlayerTagService tags) {
            super("has", "Check whether a player has a tag", tags);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            String tag = ctx.get(tagArg);
            ctx.sendMessage(msg(target.getUsername() + " tag '" + tag + "': "
                    + tags.hasTag(target.getUuid(), tag)));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ListCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final PlayerTagService tags;

        ListCmd(PlayerTagService tags) {
            super("list", "List persistent tags for a player");
            this.tags = tags;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            Set<String> snapshot = tags.snapshotTags(target.getUuid());
            if (snapshot.isEmpty()) {
                ctx.sendMessage(msg(target.getUsername() + " has no tags."));
            } else {
                ctx.sendMessage(msg("Tags for " + target.getUsername() + ": " + String.join(", ", snapshot)));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ClearCmd extends AbstractAsyncCommand {
        private final RequiredArg<PlayerRef> playerArg;
        private final PlayerTagService tags;

        ClearCmd(PlayerTagService tags) {
            super("clear", "Remove all persistent tags for a player");
            this.tags = tags;
            this.playerArg = withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef target = ctx.get(playerArg);
            tags.clearTags(target.getUuid());
            ctx.sendMessage(msg("Cleared all tags for " + target.getUsername()));
            return CompletableFuture.completedFuture(null);
        }
    }

    static Message msg(String text) {
        FormattedMessage fm = new FormattedMessage();
        fm.rawText = text;
        return new Message(fm);
    }
}
