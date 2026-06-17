package org.hyzionstudios.hytaleextras.service;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player interaction cancellation requests placed by
 * {@link org.hyzionstudios.hytaleextras.action.CancelInteractionAction}.
 *
 * <p>The HytaleExtras interaction bridge dispatches synchronously on the world event
 * thread. Cancellation is only accepted while a player is inside an active bridge
 * dispatch scope; this prevents a misplaced {@code cancel_interaction} on ENTER/EXIT
 * from cancelling a later unrelated interaction such as reopening the editor tool.
 */
public final class InteractionTriggerService {

    private final Set<UUID> activeInteractions = ConcurrentHashMap.newKeySet();
    private final Set<UUID> cancelPending = ConcurrentHashMap.newKeySet();

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
    }
}
