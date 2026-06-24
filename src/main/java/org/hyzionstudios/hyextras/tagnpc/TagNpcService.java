package org.hyzionstudios.hyextras.tagnpc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public final class TagNpcService {

    private final HyExtrasPlugin plugin;
    private final ConcurrentHashMap<UUID, MutableEntityState> entities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, IndexedEntity> entityIndex = new ConcurrentHashMap<>();

    public TagNpcService(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO).log("[hextras tagnpc] runtime service started.");
        }
    }

    public void stop() {
        clearAll();
    }

    public void indexEntity(Store<EntityStore> store, Ref<EntityStore> ref, UUID entity, Vector3d position) {
        if (entity == null || position == null) {
            return;
        }
        entityIndex.put(entity, new IndexedEntity(store, ref, new Vector3d(position), Instant.now()));
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().tagNpcClearStateOnEntityUnload) {
            pruneStaleIndexedEntities();
        }
    }

    public TagNpcResult addTag(UUID entity, String tag) {
        if (!enabled()) {
            return TagNpcResult.failure("TagNPC module is disabled.");
        }
        String normalized = normalizeKey(tag);
        if (entity == null || normalized == null) {
            return TagNpcResult.failure("Entity UUID and tag are required.");
        }
        state(entity).tags.add(normalized);
        return TagNpcResult.success("Entity tag added.");
    }

    public TagNpcResult removeTag(UUID entity, String tag) {
        if (!enabled()) {
            return TagNpcResult.failure("TagNPC module is disabled.");
        }
        String normalized = normalizeKey(tag);
        if (entity == null || normalized == null) {
            return TagNpcResult.failure("Entity UUID and tag are required.");
        }
        MutableEntityState state = entities.get(entity);
        if (state != null) {
            state.tags.remove(normalized);
            state.touch();
        }
        return TagNpcResult.success("Entity tag removed.");
    }

    public boolean hasTag(UUID entity, String tag) {
        String normalized = normalizeKey(tag);
        MutableEntityState state = entity != null ? entities.get(entity) : null;
        return normalized != null && state != null && state.tags.contains(normalized);
    }

    public Set<String> snapshotTags(UUID entity) {
        MutableEntityState state = entity != null ? entities.get(entity) : null;
        return state == null ? Set.of() : Set.copyOf(state.tags);
    }

    public TagNpcResult clearTags(UUID entity) {
        if (entity == null) {
            return TagNpcResult.failure("Entity UUID is required.");
        }
        MutableEntityState state = entities.get(entity);
        if (state != null) {
            state.tags.clear();
            state.touch();
        }
        return TagNpcResult.success("Entity tags cleared.");
    }

    public TagNpcResult setVariable(UUID entity, String key, Object value) {
        if (!enabled()) {
            return TagNpcResult.failure("TagNPC module is disabled.");
        }
        String normalized = normalizeKey(key);
        if (entity == null || normalized == null) {
            return TagNpcResult.failure("Entity UUID and variable key are required.");
        }
        state(entity).variables.put(normalized, value == null ? "" : value);
        return TagNpcResult.success("Entity variable set.");
    }

    @Nullable
    public Object getVariable(UUID entity, String key) {
        String normalized = normalizeKey(key);
        MutableEntityState state = entity != null ? entities.get(entity) : null;
        return normalized != null && state != null ? state.variables.get(normalized) : null;
    }

    @Nullable
    public String getVariableString(UUID entity, String key) {
        Object value = getVariable(entity, key);
        return value != null ? value.toString() : null;
    }

    public long incrementVariable(UUID entity, String key, long delta) {
        String normalized = normalizeKey(key);
        if (!enabled() || entity == null || normalized == null) {
            return 0L;
        }
        MutableEntityState state = state(entity);
        AtomicLong result = new AtomicLong();
        state.variables.merge(normalized, delta, (existing, increment) -> {
            long current = toLong(existing);
            long next = current + ((Number) increment).longValue();
            result.set(next);
            return next;
        });
        Object current = state.variables.get(normalized);
        long value = current instanceof Number n ? n.longValue() : result.get();
        state.touch();
        return value;
    }

    public TagNpcResult removeVariable(UUID entity, String key) {
        String normalized = normalizeKey(key);
        if (entity == null || normalized == null) {
            return TagNpcResult.failure("Entity UUID and variable key are required.");
        }
        MutableEntityState state = entities.get(entity);
        if (state != null) {
            state.variables.remove(normalized);
            state.touch();
        }
        return TagNpcResult.success("Entity variable removed.");
    }

    public Map<String, Object> snapshotVariables(UUID entity) {
        MutableEntityState state = entity != null ? entities.get(entity) : null;
        return state == null ? Map.of() : Map.copyOf(state.variables);
    }

    public TagNpcResult clearVariables(UUID entity) {
        if (entity == null) {
            return TagNpcResult.failure("Entity UUID is required.");
        }
        MutableEntityState state = entities.get(entity);
        if (state != null) {
            state.variables.clear();
            state.touch();
        }
        return TagNpcResult.success("Entity variables cleared.");
    }

    public TagNpcResult hideEntityFromViewer(UUID viewer, UUID entity) {
        if (!enabled()) {
            return TagNpcResult.failure("TagNPC module is disabled.");
        }
        if (viewer == null || entity == null || viewer.equals(entity)) {
            return TagNpcResult.failure("Viewer and entity UUIDs are required and must differ.");
        }
        state(entity);
        plugin.getPacketApi().hideEntity(viewer, entity);
        if (!packetEntityFilteringAvailable()) {
            return TagNpcResult.failure("Visibility state recorded, but PacketAPI entity filtering is unavailable.");
        }
        return TagNpcResult.success("Entity hidden from viewer.");
    }

    public TagNpcResult showEntityToViewer(UUID viewer, UUID entity) {
        if (viewer == null || entity == null) {
            return TagNpcResult.failure("Viewer and entity UUIDs are required.");
        }
        plugin.getPacketApi().showEntity(viewer, entity);
        if (!packetEntityFilteringAvailable()) {
            return TagNpcResult.failure("Visibility state cleared, but PacketAPI entity filtering is unavailable.");
        }
        return TagNpcResult.success("Entity shown to viewer.");
    }

    public boolean isEntityHiddenFromViewer(UUID viewer, UUID entity) {
        return viewer != null
                && entity != null
                && plugin.getPlayerOverrideService().isEntityHidden(viewer, entity);
    }

    public Set<UUID> snapshotTaggedEntities(String tag) {
        String normalized = normalizeKey(tag);
        if (normalized == null) {
            return Set.of();
        }
        Set<UUID> matches = ConcurrentHashMap.newKeySet();
        entities.forEach((uuid, state) -> {
            if (state.tags.contains(normalized)) {
                matches.add(uuid);
            }
        });
        return Set.copyOf(matches);
    }

    public TagNpcEntityState snapshotState(UUID entity) {
        MutableEntityState state = entity != null ? entities.get(entity) : null;
        return state == null ? null : state.snapshot(entity);
    }

    public Map<UUID, TagNpcEntityState> snapshotAllStates() {
        Map<UUID, TagNpcEntityState> snapshot = new HashMap<>();
        entities.forEach((uuid, state) -> snapshot.put(uuid, state.snapshot(uuid)));
        return Map.copyOf(snapshot);
    }

    public UUID findClosestEntityToPlayer(UUID player, double radius) {
        PlayerRef playerRef = plugin.getOnlinePlayerRef(player);
        if (playerRef == null || playerRef.getReference() == null || !playerRef.getReference().isValid()) {
            return null;
        }
        Store<EntityStore> store = playerRef.getReference().getStore();
        TransformComponent transform = store != null
                ? store.getComponent(playerRef.getReference(), TransformComponent.getComponentType())
                : null;
        if (store == null || transform == null || transform.getPosition() == null) {
            return null;
        }
        return findClosestEntity(store, transform.getPosition(), radius);
    }

    public UUID findClosestEntity(Store<EntityStore> store, Vector3d origin, double radius) {
        if (store == null || origin == null || radius <= 0.0D) {
            return null;
        }
        pruneStaleIndexedEntities();
        double radiusSquared = radius * radius;
        UUID closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Map.Entry<UUID, IndexedEntity> entry : entityIndex.entrySet()) {
            IndexedEntity indexed = entry.getValue();
            if (indexed.store != store || indexed.ref == null || !indexed.ref.isValid()) {
                continue;
            }
            double distance = indexed.position.distanceSquared(origin);
            if (distance <= radiusSquared && distance < closestDistance) {
                closestDistance = distance;
                closest = entry.getKey();
            }
        }
        return closest;
    }

    public void clearEntity(UUID entity) {
        if (entity != null) {
            entities.remove(entity);
            entityIndex.remove(entity);
            plugin.getPlayerOverrideService().clearPlayer(entity);
        }
    }

    public void clearAll() {
        entities.keySet().forEach(entity -> plugin.getPlayerOverrideService().clearPlayer(entity));
        entities.clear();
        entityIndex.clear();
    }

    private MutableEntityState state(UUID entity) {
        MutableEntityState state = entities.computeIfAbsent(entity, ignored -> new MutableEntityState());
        state.touch();
        return state;
    }

    private boolean enabled() {
        return plugin.isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC);
    }

    private boolean packetEntityFilteringAvailable() {
        HyExtrasConfig config = plugin.getExtrasConfig();
        return config != null
                && plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                && config.advancedPacketActions
                && config.entityPacketFiltering;
    }

    private static long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0L;
    }

    private static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void pruneStaleIndexedEntities() {
        Instant cutoff = Instant.now().minusSeconds(10);
        entityIndex.entrySet().removeIf(entry -> {
            IndexedEntity indexed = entry.getValue();
            boolean stale = indexed.seenAt.isBefore(cutoff)
                    || indexed.ref == null
                    || !indexed.ref.isValid();
            if (stale && plugin.getExtrasConfig() != null && plugin.getExtrasConfig().tagNpcClearStateOnEntityUnload) {
                entities.remove(entry.getKey());
                plugin.getPlayerOverrideService().clearPlayer(entry.getKey());
            }
            return stale;
        });
    }

    private static final class MutableEntityState {
        private final Set<String> tags = ConcurrentHashMap.newKeySet();
        private final ConcurrentHashMap<String, Object> variables = new ConcurrentHashMap<>();
        private volatile Instant lastSeen = Instant.now();
        @Nullable private volatile String displayName;

        private void touch() {
            lastSeen = Instant.now();
        }

        private TagNpcEntityState snapshot(UUID entity) {
            return new TagNpcEntityState(entity, tags, variables, lastSeen, displayName);
        }
    }

    private record IndexedEntity(
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            Vector3d position,
            Instant seenAt) {
    }
}
