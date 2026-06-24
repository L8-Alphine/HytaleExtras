package org.hyzionstudios.hyextras;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Tracks nearby UUID-backed non-player entities for TagNPC command targeting.
 */
public final class TagNpcEntityIndexSystem extends EntityTickingSystem<EntityStore> {

    private final HyExtrasPlugin plugin;
    private final Query<EntityStore> query = Archetype.of(
            UUIDComponent.getComponentType(),
            TransformComponent.getComponentType());

    public TagNpcEntityIndexSystem(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void tick(
            float delta,
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer) {
        if (plugin.getTagNpcService() == null) {
            return;
        }
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            return;
        }
        UUIDComponent uuid = chunk.getComponent(index, UUIDComponent.getComponentType());
        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (uuid == null || uuid.getUuid() == null || transform == null || transform.getPosition() == null) {
            return;
        }
        plugin.getTagNpcService().indexEntity(store, ref, uuid.getUuid(), transform.getPosition());
    }
}
