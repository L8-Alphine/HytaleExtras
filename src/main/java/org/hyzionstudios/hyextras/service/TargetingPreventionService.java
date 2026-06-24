package org.hyzionstudios.hyextras.service;

import com.hypixel.hytale.builtin.npccombatactionevaluator.memory.TargetMemory;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tracks players protected by {@code PreventTargeting} and clears them from NPC target memory.
 */
public final class TargetingPreventionService {

    private final HyExtrasPlugin plugin;
    private final Set<UUID> protectedPlayers = ConcurrentHashMap.newKeySet();

    public TargetingPreventionService(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    public void protectPlayer(UUID player) {
        if (player == null) {
            return;
        }
        if (protectedPlayers.add(player)) {
            logDebug("protecting player from NPC targeting uuid=" + player);
        }
    }

    public void unprotectPlayer(UUID player) {
        if (player == null) {
            return;
        }
        if (protectedPlayers.remove(player)) {
            logDebug("released player targeting protection uuid=" + player);
        }
    }

    public void clear() {
        protectedPlayers.clear();
    }

    public Set<UUID> snapshotProtectedPlayers() {
        return Set.copyOf(protectedPlayers);
    }

    public boolean hasProtectedPlayers() {
        return !protectedPlayers.isEmpty();
    }

    public Set<Integer> protectedEntityIndexes(Store<EntityStore> store) {
        if (store == null || protectedPlayers.isEmpty()) {
            return Set.of();
        }

        Set<Integer> indexes = new HashSet<>();
        for (UUID uuid : protectedPlayers) {
            PlayerRef player = plugin.getOnlinePlayerRef(uuid);
            if (player == null || player.getReference() == null) {
                continue;
            }
            Ref<EntityStore> ref = player.getReference();
            if (ref.getStore() == store && ref.isValid()) {
                indexes.add(ref.getIndex());
            }
        }
        return indexes;
    }

    public int clearProtectedTargets(TargetMemory memory, Set<Integer> protectedIndexes) {
        if (memory == null || protectedIndexes == null || protectedIndexes.isEmpty()) {
            return 0;
        }

        int cleared = 0;
        List<Ref<EntityStore>> hostiles = memory.getKnownHostilesList();
        if (hostiles != null) {
            int before = hostiles.size();
            hostiles.removeIf(ref -> ref != null && protectedIndexes.contains(ref.getIndex()));
            cleared += before - hostiles.size();
        }

        for (Integer index : protectedIndexes) {
            if (memory.getKnownHostiles().remove(index.intValue()) != 0.0F) {
                cleared++;
            }
        }

        Ref<EntityStore> closest = memory.getClosestHostile();
        if (closest != null && protectedIndexes.contains(closest.getIndex())) {
            memory.setClosestHostile(null);
            cleared++;
        }

        return cleared;
    }

    private void logDebug(String message) {
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO).log("[HyExtras targeting] " + message);
        }
    }
}
