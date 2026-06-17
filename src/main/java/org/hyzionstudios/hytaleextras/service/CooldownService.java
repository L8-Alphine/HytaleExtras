package org.hyzionstudios.hytaleextras.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Named cooldown tracker separate from the native volume-level cooldown.
 * Expiry is stored as {@code System.currentTimeMillis() + durationMs}.
 * Thread-safe via {@link ConcurrentHashMap}.
 */
public final class CooldownService {

    private final Map<UUID, ConcurrentHashMap<String, Long>> store = new ConcurrentHashMap<>();

    /**
     * Returns {@code true} if the named cooldown is NOT active (i.e. ready to fire again).
     */
    public boolean isReady(UUID player, String name) {
        Map<String, Long> map = store.get(player);
        if (map == null) return true;
        Long expiry = map.get(name);
        return expiry == null || System.currentTimeMillis() >= expiry;
    }

    /**
     * Applies (or resets) a named cooldown for {@code durationSeconds} seconds.
     */
    public void apply(UUID player, String name, float durationSeconds) {
        long expiryMs = System.currentTimeMillis() + (long) (durationSeconds * 1000f);
        store.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(name, expiryMs);
    }

    /**
     * Returns remaining seconds until the cooldown expires, or {@code 0} if not active.
     */
    public float remainingSeconds(UUID player, String name) {
        Map<String, Long> map = store.get(player);
        if (map == null) return 0f;
        Long expiry = map.get(name);
        if (expiry == null) return 0f;
        long remaining = expiry - System.currentTimeMillis();
        return remaining <= 0 ? 0f : remaining / 1000f;
    }

    /** Removes a specific cooldown entry for a player. */
    public void clear(UUID player, String name) {
        Map<String, Long> map = store.get(player);
        if (map != null) map.remove(name);
    }

    /** Called on player disconnect — removes all cooldown state for this player. */
    public void clearPlayer(UUID player) {
        store.remove(player);
    }

    /**
     * Returns a snapshot of all active (non-expired) cooldowns for a player as
     * {@code name → remaining seconds}. Used by {@code /hextras debug player}.
     */
    public Map<String, Float> snapshot(UUID player) {
        Map<String, Long> map = store.get(player);
        if (map == null) return Map.of();
        long now = System.currentTimeMillis();
        Map<String, Float> result = new HashMap<>();
        map.forEach((name, expiry) -> {
            long remaining = expiry - now;
            if (remaining > 0) result.put(name, remaining / 1000f);
        });
        return result;
    }
}
