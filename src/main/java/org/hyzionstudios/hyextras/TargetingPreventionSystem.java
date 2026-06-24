package org.hyzionstudios.hyextras;

import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.service.TargetingPreventionService;

import java.util.Set;
import java.util.logging.Level;

/**
 * Clears HyExtras protected players from NPC combat target memory.
 */
public final class TargetingPreventionSystem extends EntityTickingSystem<EntityStore> {

    private final HyExtrasPlugin plugin;
    private final Query<EntityStore> query = Archetype.of(TargetMemory.getComponentType());

    public TargetingPreventionSystem(HyExtrasPlugin plugin) {
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
        TargetingPreventionService service = plugin.getTargetingPreventionService();
        if (service == null || !service.hasProtectedPlayers()) {
            return;
        }

        Set<Integer> protectedIndexes = service.protectedEntityIndexes(store);
        if (protectedIndexes.isEmpty()) {
            return;
        }

        TargetMemory memory = chunk.getComponent(index, TargetMemory.getComponentType());
        int cleared = service.clearProtectedTargets(memory, protectedIndexes);
        if (cleared > 0
                && plugin.getExtrasConfig() != null
                && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO)
                    .log("[HyExtras targeting] cleared " + cleared
                            + " protected target reference(s) from entity " + chunk.getReferenceTo(index));
        }
    }
}
