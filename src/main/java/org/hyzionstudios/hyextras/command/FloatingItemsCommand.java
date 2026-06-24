package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemInstance;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemResult;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemService;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemTuning;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class FloatingItemsCommand extends AbstractCommandCollection {

    public FloatingItemsCommand(FloatingItemService service) {
        super("floatingitems", "Manage decorative floating item displays");
        addSubCommand(new ListCmd(service));
        addSubCommand(new InfoCmd(service));
        addSubCommand(new CreateCmd(service));
        addSubCommand(new RemoveCmd(service));
        addSubCommand(new IntangibleCmd(service));
        addSubCommand(new ReloadCmd(service));
    }

    private abstract static class FloatingItemCommand extends AbstractAsyncCommand {
        final FloatingItemService service;

        FloatingItemCommand(String name, String description, String permission, FloatingItemService service) {
            super(name, description);
            this.service = service;
            requirePermission(permission);
        }
    }

    private static final class ListCmd extends FloatingItemCommand {
        ListCmd(FloatingItemService service) {
            super("list", "List floating items", "hyextras.floatingitems.list", service);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Map<String, FloatingItemInstance> items = service.snapshotFloatingItems();
            if (items.isEmpty()) {
                ctx.sendMessage(TagCommand.msg("No floating items are registered."));
            } else {
                ctx.sendMessage(TagCommand.msg("Floating items: " + String.join(", ", items.keySet())));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class InfoCmd extends FloatingItemCommand {
        private final RequiredArg<String> idArg;

        InfoCmd(FloatingItemService service) {
            super("info", "Show floating item details", "hyextras.floatingitems.info", service);
            this.idArg = withRequiredArg("id", "Floating item id", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            FloatingItemInstance item = service.snapshotFloatingItem(ctx.get(idArg));
            if (item == null) {
                ctx.sendMessage(TagCommand.msg("Floating item not found."));
                return CompletableFuture.completedFuture(null);
            }
            ctx.sendMessage(TagCommand.msg(item.id()
                    + " item=" + item.itemId()
                    + " quantity=" + item.quantity()
                    + " persistent=" + item.persistent()
                    + " intangible=" + item.intangible()
                    + " position=" + item.position().x + "," + item.position().y + "," + item.position().z));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class CreateCmd extends FloatingItemCommand {
        private final RequiredArg<String> idArg;
        private final RequiredArg<PlayerRef> playerArg;
        private final RequiredArg<String> itemArg;
        private final DefaultArg<Boolean> persistentArg;

        CreateCmd(FloatingItemService service) {
            super("create", "Create a floating item at a player's position", "hyextras.floatingitems.create", service);
            this.idArg = withRequiredArg("id", "Floating item id", ArgTypes.STRING);
            this.playerArg = withRequiredArg("player", "Anchor player", ArgTypes.PLAYER_REF);
            this.itemArg = withRequiredArg("itemId", "Item id", ArgTypes.STRING);
            boolean defaultPersistent = HyExtrasPlugin.get() != null
                    && HyExtrasPlugin.get().getExtrasConfig() != null
                    && HyExtrasPlugin.get().getExtrasConfig().floatingItemsDefaultPersistent;
            this.persistentArg = withDefaultArg(
                    "persistent",
                    "Persist across restarts",
                    ArgTypes.BOOLEAN,
                    defaultPersistent,
                    String.valueOf(defaultPersistent));
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            PlayerRef player = ctx.get(playerArg);
            HyExtrasConfig config = HyExtrasPlugin.get().getExtrasConfig();
            boolean persistent = ctx.get(persistentArg);
            FloatingItemResult result = service.createFloatingItemAtPlayer(
                    ctx.get(idArg),
                    player.getUuid(),
                    new ItemStack(ctx.get(itemArg), 1),
                    FloatingItemTuning.defaults(config),
                    persistent);
            ctx.sendMessage(TagCommand.msg(result.message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class RemoveCmd extends FloatingItemCommand {
        private final RequiredArg<String> idArg;

        RemoveCmd(FloatingItemService service) {
            super("remove", "Remove a floating item", "hyextras.floatingitems.remove", service);
            this.idArg = withRequiredArg("id", "Floating item id", ArgTypes.STRING);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            ctx.sendMessage(TagCommand.msg(service.removeFloatingItem(ctx.get(idArg)).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class IntangibleCmd extends FloatingItemCommand {
        private final RequiredArg<String> idArg;
        private final RequiredArg<Boolean> valueArg;

        IntangibleCmd(FloatingItemService service) {
            super("intangible", "Set floating item intangible state", "hyextras.floatingitems.modify", service);
            this.idArg = withRequiredArg("id", "Floating item id", ArgTypes.STRING);
            this.valueArg = withRequiredArg("value", "true or false", ArgTypes.BOOLEAN);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            ctx.sendMessage(TagCommand.msg(service.setFloatingItemIntangible(ctx.get(idArg), ctx.get(valueArg)).message()));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static final class ReloadCmd extends FloatingItemCommand {
        ReloadCmd(FloatingItemService service) {
            super("reload", "Reload floating item displays", "hyextras.floatingitems.reload", service);
        }

        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            ctx.sendMessage(TagCommand.msg(service.reload().message()));
            return CompletableFuture.completedFuture(null);
        }
    }
}
