package org.hyzionstudios.hyextras.triggerextras.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player interaction cancellation requests placed by
 * {@link org.hyzionstudios.hyextras.triggerextras.action.CancelInteractionAction}.
 *
 * <p>The HyExtras interaction bridge dispatches synchronously on the world event
 * thread. Cancellation is only accepted while a player is inside an active bridge
 * dispatch scope; this prevents a misplaced {@code cancel_interaction} on ENTER/EXIT
 * from cancelling a later unrelated interaction such as reopening the editor tool.
 */
public final class InteractionTriggerService {

    private final Set<UUID> activeInteractions = ConcurrentHashMap.newKeySet();
    private final Set<UUID> cancelPending = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<UUID, RecentInteraction> recentResults = new ConcurrentHashMap<>();

    public void beginInteraction(UUID uuid) {
        cancelPending.remove(uuid);
        activeInteractions.add(uuid);
    }

    public void endInteraction(UUID uuid) {
        activeInteractions.remove(uuid);
        cancelPending.remove(uuid);
    }

    public boolean markCancelPending(UUID uuid) {
        if (!activeInteractions.contains(uuid)) {
            return false;
        }
        return cancelPending.add(uuid);
    }

    public boolean isCancelPending(UUID uuid) {
        return cancelPending.contains(uuid);
    }

    public void clearCancelPending(UUID uuid) {
        activeInteractions.remove(uuid);
        cancelPending.remove(uuid);
        recentResults.remove(uuid);
    }

    public Boolean getRecentResult(UUID uuid, String key, long nowNanos, long maxAgeNanos) {
        RecentInteraction recent = recentResults.get(uuid);
        if (recent == null || !recent.key.equals(key)) {
            return null;
        }
        if (nowNanos - recent.timestampNanos > maxAgeNanos) {
            recentResults.remove(uuid, recent);
            return null;
        }
        return recent.cancel;
    }

    public void rememberResult(UUID uuid, String key, boolean cancel, long nowNanos) {
        recentResults.put(uuid, new RecentInteraction(key, cancel, nowNanos));
    }

    private static final class RecentInteraction {
        private final String key;
        private final boolean cancel;
        private final long timestampNanos;

        private RecentInteraction(String key, boolean cancel, long timestampNanos) {
            this.key = key;
            this.cancel = cancel;
            this.timestampNanos = timestampNanos;
        }
    }
}
