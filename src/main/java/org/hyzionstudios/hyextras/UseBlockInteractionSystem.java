package org.hyzionstudios.hyextras;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Handles block-use interactions before server interactions such as doors/containers run.
 */
public final class UseBlockInteractionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private final HyExtrasPlugin plugin;

    public UseBlockInteractionSystem(HyExtrasPlugin plugin) {
        super(UseBlockEvent.Pre.class);
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }

    @Override
    public void handle(
        int index,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer,
        UseBlockEvent.Pre event
    ) {
        if (!event.isCancelled() && plugin.handleUseBlockPre(store, event)) {
            event.setCancelled(true);
        }
    }
}
