package org.hyzionstudios.hyextras.service;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.event.HyExtrasEvents;
import org.hyzionstudios.hyextras.event.HyExtrasEvents.ChangeType;
import org.hyzionstudios.hyextras.util.AsyncSaver;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Per-player boolean tag store. Tags are short string identifiers
 * (e.g. "storyline_a_active", "found_key", "door_unlocked").
 *
 * <p>Tags are distinct from variables: they are boolean flags (present / absent)
 * rather than key-value pairs. Each player's tags are saved to
 * {@code {dataDir}/players/{uuid}.tags} on disconnect and loaded on connect,
 * surviving server restarts. Writes are atomic (temp file + rename) and offloaded
 * via {@link AsyncSaver} when {@code persistence.async=true}; disconnect saves are
 * flushed synchronously for durability. Mutations post {@link HyExtrasEvents.PlayerTagChangeEvent}.
 */
public final class PlayerTagService {

    private final java.util.Map<UUID, Set<String>> tags = new ConcurrentHashMap<>();
    private final Path playersDir;
    private final AsyncSaver saver;

    public PlayerTagService(@Nonnull Path dataDir) {
        this.playersDir = dataDir.resolve("players");
        HyExtrasConfig cfg = config();
        this.saver = new AsyncSaver("hextras-tag-saver",
                cfg != null ? cfg.persistenceSaveDebounceMs : 1000L);
    }

    public void addTag(UUID player, String tag) {
        boolean changed = tags.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(tag);
        if (changed) {
            postChange(player, tag, ChangeType.ADD);
        }
    }

    public void removeTag(UUID player, String tag) {
        Set<String> set = tags.get(player);
        if (set != null && set.remove(tag)) {
            postChange(player, tag, ChangeType.REMOVE);
        }
    }

    public void clearTags(UUID player) {
        tags.remove(player);
        tryDeleteAsync(player);
        postChange(player, null, ChangeType.CLEAR);
    }

    public boolean hasTag(UUID player, String tag) {
        Set<String> set = tags.get(player);
        return set != null && set.contains(tag);
    }

    /** Returns an unmodifiable snapshot of all tags (for debug command). */
    public Set<String> snapshotTags(UUID player) {
        Set<String> set = tags.get(player);
        return set == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(set));
    }

    /** Persist the current in-memory tags without clearing them. */
    public void savePlayer(UUID player) {
        Set<String> set = tags.get(player);
        Path file = playersDir.resolve(player + ".tags");
        if (set == null || set.isEmpty()) {
            tryDeleteAsync(player);
            return;
        }
        Properties snapshot = buildSnapshot(set);
        Runnable task = () -> writeSnapshot(file, snapshot, player);
        if (persistenceAsync()) {
            saver.schedule(player.toString(), task);
        } else {
            task.run();
        }
    }

    /** Load persisted tags from disk when a player connects. */
    public void loadPlayer(UUID player) {
        Path file = playersDir.resolve(player + ".tags");
        if (!Files.exists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            Properties props = new Properties();
            props.load(r);
            Set<String> set = tags.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet());
            for (String key : props.stringPropertyNames()) {
                set.add(key);
            }
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras] Failed to load tags for player " + player);
        }
    }

    /** Persist tags to disk and clear in-memory state when a player disconnects. */
    public void saveAndClearPlayer(UUID player) {
        Set<String> set = tags.remove(player);
        Path file = playersDir.resolve(player + ".tags");
        if (set == null || set.isEmpty()) {
            saver.flush(player.toString(), () -> tryDelete(file));
            return;
        }
        Properties snapshot = buildSnapshot(set);
        saver.flush(player.toString(), () -> writeSnapshot(file, snapshot, player));
    }

    /** Flushes pending writes and stops the background saver. */
    public void stop() {
        saver.shutdown();
    }

    private static Properties buildSnapshot(Set<String> tagSet) {
        Properties props = new Properties();
        for (String tag : tagSet) {
            props.setProperty(tag, "1");
        }
        return props;
    }

    private void writeSnapshot(Path file, Properties props, UUID player) {
        try {
            AsyncSaver.writeProperties(file, props);
        } catch (IOException e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras] Failed to save tags for player " + player);
        }
    }

    private void tryDeleteAsync(UUID player) {
        Path file = playersDir.resolve(player + ".tags");
        if (persistenceAsync()) {
            saver.schedule(player.toString(), () -> tryDelete(file));
        } else {
            tryDelete(file);
        }
    }

    private void tryDelete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {}
    }

    private static boolean persistenceAsync() {
        HyExtrasConfig cfg = config();
        return cfg == null || cfg.persistenceAsync;
    }

    private void postChange(UUID player, String tag, ChangeType type) {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        if (plugin != null && plugin.getEventBus() != null) {
            plugin.getEventBus().post(new HyExtrasEvents.PlayerTagChangeEvent(player, tag, type));
        }
    }

    private static HyExtrasConfig config() {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        return plugin == null ? null : plugin.getExtrasConfig();
    }
}
