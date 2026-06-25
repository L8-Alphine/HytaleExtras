package org.hyzionstudios.hyextras.service;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.event.HyExtrasEvents;
import org.hyzionstudios.hyextras.event.HyExtrasEvents.ChangeType;
import org.hyzionstudios.hyextras.util.AsyncSaver;
import org.hyzionstudios.hyextras.util.ValueCodec;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Per-player key&rarr;value variable store. Memory-only by default (cleared on disconnect), preserving
 * legacy behavior. Persistence is opt-in:
 * <ul>
 *   <li>set {@code playerVariablesPersistent=true} to persist every variable, or</li>
 *   <li>prefix individual keys with {@code persist:} to persist just those.</li>
 * </ul>
 * Persisted values are written to {@code {dataDir}/players/{uuid}.vars} (type-tagged via
 * {@link ValueCodec}), loaded on connect, and saved on disconnect. Disk I/O is offloaded and
 * coalesced by {@link AsyncSaver} when {@code persistence.async=true}.
 *
 * <p>Thread-safe: outer and inner maps are both {@link ConcurrentHashMap}.
 */
public final class PlayerVariableService {

    /** Keys with this prefix persist to disk even when global persistence is disabled. */
    public static final String PERSIST_PREFIX = "persist:";

    private final Map<UUID, ConcurrentHashMap<String, Object>> store = new ConcurrentHashMap<>();
    private final Path playersDir;
    private final AsyncSaver saver;

    public PlayerVariableService(Path dataDir) {
        this.playersDir = dataDir.resolve("players");
        HyExtrasConfig cfg = config();
        this.saver = new AsyncSaver("hextras-var-saver",
                cfg != null ? cfg.persistenceSaveDebounceMs : 1000L);
    }

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
        postChange(player, key, value, ChangeType.SET);
    }

    public void remove(UUID player, String key) {
        Map<String, Object> map = store.get(player);
        if (map != null) map.remove(key);
        postChange(player, key, null, ChangeType.REMOVE);
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
        long result = getLong(player, key);
        postChange(player, key, result, ChangeType.SET);
        return result;
    }

    /** Removes all variables for this player from memory and posts a CLEAR event. */
    public void clearPlayer(UUID player) {
        store.remove(player);
        postChange(player, null, null, ChangeType.CLEAR);
    }

    /** Returns an unmodifiable snapshot of all variables for a player (for debug commands). */
    public Map<String, Object> snapshot(UUID player) {
        Map<String, Object> m = store.get(player);
        return m == null ? Collections.emptyMap() : Collections.unmodifiableMap(Map.copyOf(m));
    }

    /** Returns true if any variable is set for this player. */
    public boolean hasAny(UUID player) {
        Map<String, Object> m = store.get(player);
        return m != null && !m.isEmpty();
    }

    // --- Persistence ---

    /** Loads persisted variables from disk when a player connects. No-op when no file exists. */
    public void loadPlayer(UUID player) {
        Path file = playersDir.resolve(player + ".vars");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Properties props = new Properties();
            props.load(r);
            ConcurrentHashMap<String, Object> map = store.computeIfAbsent(player, k -> new ConcurrentHashMap<>());
            for (String key : props.stringPropertyNames()) {
                map.put(key, ValueCodec.decode(props.getProperty(key)));
            }
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras] Failed to load variables for player " + player);
        }
    }

    /** Persists the player's currently-persisting variables without clearing them. */
    public void savePlayer(UUID player) {
        Properties snapshot = buildPersistSnapshot(player);
        Path file = playersDir.resolve(player + ".vars");
        Runnable task = snapshot == null ? () -> tryDelete(file) : () -> writeSnapshot(file, snapshot, player);
        if (persistenceAsync()) {
            saver.schedule(player.toString(), task);
        } else {
            task.run();
        }
    }

    /** Persists persisting variables and clears in-memory state when a player disconnects. */
    public void saveAndClearPlayer(UUID player) {
        Properties snapshot = buildPersistSnapshot(player);
        store.remove(player);
        Path file = playersDir.resolve(player + ".vars");
        // flush() runs synchronously to guarantee durability before the player is fully removed.
        saver.flush(player.toString(),
                snapshot == null ? () -> tryDelete(file) : () -> writeSnapshot(file, snapshot, player));
    }

    /** Flushes pending writes and stops the background saver. */
    public void stop() {
        saver.shutdown();
    }

    @Nullable
    private Properties buildPersistSnapshot(UUID player) {
        Map<String, Object> map = store.get(player);
        if (map == null || map.isEmpty()) {
            return null;
        }
        boolean globalPersist = config() != null && config().playerVariablesPersistent;
        Properties props = new Properties();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (globalPersist || entry.getKey().startsWith(PERSIST_PREFIX)) {
                props.setProperty(entry.getKey(), ValueCodec.encode(entry.getValue()));
            }
        }
        return props.isEmpty() ? null : props;
    }

    private void writeSnapshot(Path file, Properties props, UUID player) {
        try {
            AsyncSaver.writeProperties(file, props);
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras] Failed to save variables for player " + player);
        }
    }

    private void tryDelete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }

    private static boolean persistenceAsync() {
        HyExtrasConfig cfg = config();
        return cfg == null || cfg.persistenceAsync;
    }

    private void postChange(UUID player, @Nullable String key, @Nullable Object value, ChangeType type) {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        if (plugin != null && plugin.getEventBus() != null) {
            plugin.getEventBus().post(new HyExtrasEvents.PlayerVariableChangeEvent(player, key, value, type));
        }
    }

    @Nullable
    private static HyExtrasConfig config() {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        return plugin == null ? null : plugin.getExtrasConfig();
    }
}
