package org.hyzionstudios.hyextras.state;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-viewer entity visibility overrides.
 *
 * <p>A UUID in a viewer's hidden set means that entity should not be visible to that viewer.
 * Server-side state is always tracked regardless of whether the packet was actually sent.
 */
public final class PlayerOverrideService {

    private final ConcurrentHashMap<UUID, Set<UUID>> hiddenFrom = new ConcurrentHashMap<>();

    public void hideEntity(UUID viewer, UUID target) {
        hiddenFrom.computeIfAbsent(viewer, k -> ConcurrentHashMap.newKeySet()).add(target);
    }

    public void showEntity(UUID viewer, UUID target) {
        Set<UUID> set = hiddenFrom.get(viewer);
        if (set != null) set.remove(target);
    }

    public boolean isEntityHidden(UUID viewer, UUID target) {
        Set<UUID> set = hiddenFrom.get(viewer);
        return set != null && set.contains(target);
    }

    public void clearAll(UUID viewer) {
        hiddenFrom.remove(viewer);
    }

    /** Called on disconnect — cleans up the player as both viewer and potential target. */
    public void clearPlayer(UUID player) {
        hiddenFrom.remove(player);
        hiddenFrom.values().forEach(set -> set.remove(player));
    }

    /** Returns a defensive snapshot of UUIDs hidden from this viewer (empty set if none). */
    public Set<UUID> snapshotHidden(UUID viewer) {
        Set<UUID> set = hiddenFrom.get(viewer);
        return set != null ? new HashSet<>(set) : Set.of();
    }
}
