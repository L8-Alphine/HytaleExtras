package org.hyzionstudios.hyextras.service;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player key→value variable store. Backed by in-memory maps; cleared on disconnect.
 * Thread-safe: outer and inner maps are both {@link ConcurrentHashMap}.
 */
public final class PlayerVariableService {

    private final Map<UUID, ConcurrentHashMap<String, Object>> store = new ConcurrentHashMap<>();

    @Nullable
    public Object get(UUID player, String key) {
        Map<String, Object> map = store.get(player);
        return map == null ? null : map.get(key);
    }

    @Nullable
    public String getString(UUID player, String key) {
        Object v = get(player, key);
        return v == null ? null : v.toString();
    }

    public long getLong(UUID player, String key) {
        Object v = get(player, key);
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) {
            try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    public void set(UUID player, String key, Object value) {
        store.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    public void remove(UUID player, String key) {
        Map<String, Object> map = store.get(player);
        if (map != null) map.remove(key);
    }

    /**
     * Atomically increments a numeric variable by {@code delta}.
     * If the variable does not exist it is treated as 0 before incrementing.
     *
     * @return the new value after increment
     */
    public long increment(UUID player, String key, long delta) {
        ConcurrentHashMap<String, Object> map =
                store.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
        map.merge(key, delta, (existing, d) -> {
            long cur = (existing instanceof Number n) ? n.longValue() : 0L;
            return cur + ((Number) d).longValue();
        });
        return getLong(player, key);
    }

    /** Called on player disconnect — removes all variables for this player. */
    public void clearPlayer(UUID player) {
        store.remove(player);
    }

    /** Returns an unmodifiable snapshot of all variables for a player (for debug commands). */
    public Map<String, Object> snapshot(UUID player) {
        Map<String, Object> m = store.get(player);
        return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(m);
    }

    /** Returns true if any variable is set for this player. */
    public boolean hasAny(UUID player) {
        Map<String, Object> m = store.get(player);
        return m != null && !m.isEmpty();
    }
}
