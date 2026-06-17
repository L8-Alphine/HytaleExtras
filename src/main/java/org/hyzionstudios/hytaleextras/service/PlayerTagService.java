package org.hyzionstudios.hytaleextras.service;

import org.hyzionstudios.hytaleextras.HyextrasPlugin;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
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
 * surviving server restarts.
 */
public final class PlayerTagService {

    private final Map<UUID, Set<String>> tags = new ConcurrentHashMap<>();
    private final Path playersDir;

    public PlayerTagService(@Nonnull Path dataDir) {
        this.playersDir = dataDir.resolve("players");
    }

    public void addTag(UUID player, String tag) {
        tags.computeIfAbsent(player, k -> ConcurrentHashMap.newKeySet()).add(tag);
    }

    public void removeTag(UUID player, String tag) {
        Set<String> set = tags.get(player);
        if (set != null) set.remove(tag);
    }

    public void clearTags(UUID player) {
        tags.remove(player);
        tryDeleteFile(player);
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
        if (set == null || set.isEmpty()) {
            tryDeleteFile(player);
            return;
        }
        saveToDisk(player, set);
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
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras] Failed to load tags for player " + player);
        }
    }

    /** Persist tags to disk and clear in-memory state when a player disconnects. */
    public void saveAndClearPlayer(UUID player) {
        Set<String> set = tags.remove(player);
        if (set == null || set.isEmpty()) {
            tryDeleteFile(player);
            return;
        }
        saveToDisk(player, set);
    }

    private void saveToDisk(UUID player, Set<String> tagSet) {
        try {
            Files.createDirectories(playersDir);
            Properties props = new Properties();
            for (String tag : tagSet) {
                props.setProperty(tag, "1");
            }
            try (Writer w = Files.newBufferedWriter(playersDir.resolve(player + ".tags"))) {
                props.store(w, null);
            }
        } catch (IOException e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras] Failed to save tags for player " + player);
        }
    }

    private void tryDeleteFile(UUID player) {
        try {
            Files.deleteIfExists(playersDir.resolve(player + ".tags"));
        } catch (IOException ignored) {}
    }
}
