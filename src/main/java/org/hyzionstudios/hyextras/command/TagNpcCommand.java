package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.tagnpc.TagNpcService;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TagNpcCommand extends AbstractCommandCollection {

    public TagNpcCommand(TagNpcService service) {
        super("tagnpc", "Manage runtime TagNPC entity state by UUID");
        addSubCommand(new TagGroup(service));
        addSubCommand(new VarGroup(service));
        addSubCommand(new HideCmd(service));
        addSubCommand(new ShowCmd(service));
        addSubCommand(new NearGroup(service));
    }

    private abstract static class EntityCommand extends AbstractAsyncCommand {
        final TagNpcService service;
        final RequiredArg<String> entityArg;

        EntityCommand(String name, String description, TagNpcService service) {
            super(name, description);
            this.service = service;
            this.entityArg = withRequiredArg("entityUuid", "Target entity UUID", ArgTypes.STRING);
        }

        UUID entity(CommandContext ctx) {
            return parseUuid(ctx.get(entityArg));
        }
    }

    private static final class TagGroup extends AbstractCommandCollection {
        TagGroup(TagNpcService service) {
            super("tag", "Manage TagNPC tags");
            addSubCommand(new TagAddCmd(service));
            addSubCommand(new TagRemoveCmd(service));
            addSubCommand(new TagListCmd(service));
        }
    }

    private static final class TagAddCmd extends EntityCommand {
        private final RequiredArg<String> tagArg;

        TagAddCmd(TagNpcService service) {
            super("add", "Add a runtime TagNPC tag", service);
            this.tagArg = withRequiredArg("tag", "Tag name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            String tag = ctx.get(tagArg);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(service.addTag(entity, tag).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class TagRemoveCmd extends EntityCommand {
        private final RequiredArg<String> tagArg;

        TagRemoveCmd(TagNpcService service) {
            super("remove", "Remove a runtime TagNPC tag", service);
            this.tagArg = withRequiredArg("tag", "Tag name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            String tag = ctx.get(tagArg);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(service.removeTag(entity, tag).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class TagListCmd extends EntityCommand {
        TagListCmd(TagNpcService service) {
            super("list", "List runtime TagNPC tags", service);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            Set<String> tags = service.snapshotTags(entity);
            ctx.sendMessage(TagCommand.msg(tags.isEmpty()
                    ? "Entity has no TagNPC tags."
                    : "TagNPC tags: " + String.join(", ", tags)));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class VarGroup extends AbstractCommandCollection {
        VarGroup(TagNpcService service) {
            super("var", "Manage TagNPC variables");
            addSubCommand(new VarSetCmd(service));
            addSubCommand(new VarGetCmd(service));
            addSubCommand(new VarAddCmd(service));
            addSubCommand(new VarListCmd(service));
        }
    }

    private static final class VarSetCmd extends EntityCommand {
        private final RequiredArg<String> keyArg;
        private final RequiredArg<String> valueArg;

        VarSetCmd(TagNpcService service) {
            super("set", "Set a runtime TagNPC variable", service);
            this.keyArg = withRequiredArg("key", "Variable key", ArgTypes.STRING);
            this.valueArg = withRequiredArg("value", "Variable value", ArgTypes.GREEDY_STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(service.setVariable(entity, ctx.get(keyArg), ctx.get(valueArg)).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class VarGetCmd extends EntityCommand {
        private final RequiredArg<String> keyArg;

        VarGetCmd(TagNpcService service) {
            super("get", "Get a runtime TagNPC variable", service);
            this.keyArg = withRequiredArg("key", "Variable key", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            String key = ctx.get(keyArg);
            ctx.sendMessage(TagCommand.msg(key + "=" + service.getVariableString(entity, key)));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class VarAddCmd extends EntityCommand {
        private final RequiredArg<String> keyArg;
        private final RequiredArg<Integer> amountArg;

        VarAddCmd(TagNpcService service) {
            super("add", "Add to a runtime TagNPC numeric variable", service);
            this.keyArg = withRequiredArg("key", "Variable key", ArgTypes.STRING);
            this.amountArg = withRequiredArg("amount", "Amount to add", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            long value = service.incrementVariable(entity, ctx.get(keyArg), ctx.get(amountArg));
            ctx.sendMessage(TagCommand.msg("New value: " + value));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class VarListCmd extends EntityCommand {
        VarListCmd(TagNpcService service) {
            super("list", "List runtime TagNPC variables", service);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            Map<String, Object> vars = service.snapshotVariables(entity);
            ctx.sendMessage(TagCommand.msg(vars.isEmpty()
                    ? "Entity has no TagNPC variables."
                    : "TagNPC variables: " + vars));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class HideCmd extends EntityCommand {
        private final RequiredArg<PlayerRef> viewerArg;

        HideCmd(TagNpcService service) {
            super("hide", "Hide an entity from a player viewer", service);
            this.viewerArg = withRequiredArg("viewer", "Viewer player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            PlayerRef viewer = ctx.get(viewerArg);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(service.hideEntityFromViewer(viewer.getUuid(), entity).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ShowCmd extends EntityCommand {
        private final RequiredArg<PlayerRef> viewerArg;

        ShowCmd(TagNpcService service) {
            super("show", "Show an entity to a player viewer", service);
            this.viewerArg = withRequiredArg("viewer", "Viewer player", ArgTypes.PLAYER_REF);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = entity(ctx);
            PlayerRef viewer = ctx.get(viewerArg);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("Invalid entity UUID."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(service.showEntityToViewer(viewer.getUuid(), entity).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NearGroup extends AbstractCommandCollection {
        NearGroup(TagNpcService service) {
            super("near", "Manage closest runtime TagNPC entity near a player");
            addSubCommand(new NearTagAddCmd(service));
            addSubCommand(new NearTagRemoveCmd(service));
            addSubCommand(new NearVarSetCmd(service));
            addSubCommand(new NearVarAddCmd(service));
            addSubCommand(new NearInfoCmd(service));
        }
    }

    private abstract static class NearCommand extends AbstractAsyncCommand {
        final TagNpcService service;
        final RequiredArg<PlayerRef> playerArg;
        final RequiredArg<Integer> radiusArg;

        NearCommand(String name, String description, TagNpcService service) {
            super(name, description);
            this.service = service;
            this.playerArg = withRequiredArg("player", "Player at the center of the search", ArgTypes.PLAYER_REF);
            this.radiusArg = withRequiredArg("radius", "Search radius in blocks", ArgTypes.INTEGER);
        }

        UUID nearest(CommandContext ctx) {
            PlayerRef player = ctx.get(playerArg);
            int radius = Math.max(1, ctx.get(radiusArg));
            return service.findClosestEntityToPlayer(player.getUuid(), radius);
        }
    }

    private static final class NearTagAddCmd extends NearCommand {
        private final RequiredArg<String> tagArg;

        NearTagAddCmd(TagNpcService service) {
            super("tag-add", "Add a tag to the closest UUID-backed non-player entity", service);
            this.tagArg = withRequiredArg("tag", "Tag name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = nearest(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("No UUID-backed NPC/mob found in range."));
                return CompletableFuture.completedFuture(null);
            }
            String tag = ctx.get(tagArg);
            ctx.sendMessage(TagCommand.msg(service.addTag(entity, tag).message() + " entity=" + entity));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NearTagRemoveCmd extends NearCommand {
        private final RequiredArg<String> tagArg;

        NearTagRemoveCmd(TagNpcService service) {
            super("tag-remove", "Remove a tag from the closest UUID-backed non-player entity", service);
            this.tagArg = withRequiredArg("tag", "Tag name", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = nearest(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("No UUID-backed NPC/mob found in range."));
                return CompletableFuture.completedFuture(null);
            }
            String tag = ctx.get(tagArg);
            ctx.sendMessage(TagCommand.msg(service.removeTag(entity, tag).message() + " entity=" + entity));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NearVarSetCmd extends NearCommand {
        private final RequiredArg<String> keyArg;
        private final RequiredArg<String> valueArg;

        NearVarSetCmd(TagNpcService service) {
            super("var-set", "Set a variable on the closest UUID-backed non-player entity", service);
            this.keyArg = withRequiredArg("key", "Variable key", ArgTypes.STRING);
            this.valueArg = withRequiredArg("value", "Variable value", ArgTypes.GREEDY_STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = nearest(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("No UUID-backed NPC/mob found in range."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(service.setVariable(entity, ctx.get(keyArg), ctx.get(valueArg)).message()
                    + " entity=" + entity));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NearVarAddCmd extends NearCommand {
        private final RequiredArg<String> keyArg;
        private final RequiredArg<Integer> amountArg;

        NearVarAddCmd(TagNpcService service) {
            super("var-add", "Add to a variable on the closest UUID-backed non-player entity", service);
            this.keyArg = withRequiredArg("key", "Variable key", ArgTypes.STRING);
            this.amountArg = withRequiredArg("amount", "Amount to add", ArgTypes.INTEGER);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = nearest(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("No UUID-backed NPC/mob found in range."));
                return CompletableFuture.completedFuture(null);
            }
            long value = service.incrementVariable(entity, ctx.get(keyArg), ctx.get(amountArg));
            ctx.sendMessage(TagCommand.msg("New value: " + value + " entity=" + entity));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class NearInfoCmd extends NearCommand {
        NearInfoCmd(TagNpcService service) {
            super("info", "Show the closest UUID-backed non-player entity in range", service);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            UUID entity = nearest(ctx);
            if (entity == null) {
                ctx.sendMessage(TagCommand.msg("No UUID-backed NPC/mob found in range."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg("Closest TagNPC entity=" + entity
                    + " tags=" + service.snapshotTags(entity)
                    + " vars=" + service.snapshotVariables(entity)));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static UUID parseUuid(String raw) {
        try {
            return raw == null ? null : UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
