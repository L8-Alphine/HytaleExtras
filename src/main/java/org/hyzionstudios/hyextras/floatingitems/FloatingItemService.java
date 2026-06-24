package org.hyzionstudios.hyextras.floatingitems;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class FloatingItemService {

    private static final String PERSISTENCE_FILE = "floating-items.properties";

    private final HyExtrasPlugin plugin;
    private final FloatingItemRenderer renderer;
    private final ConcurrentHashMap<String, FloatingItemInstance> items = new ConcurrentHashMap<>();

    public FloatingItemService(HyExtrasPlugin plugin) {
        this.plugin = plugin;
        this.renderer = new FloatingItemRenderer(plugin);
    }

    public void start() {
        loadPersistentDefinitions();
        refreshAll();
    }

    public void stop() {
        savePersistentDefinitions();
        items.values().forEach(renderer::clear);
        items.clear();
    }

    public FloatingItemResult reload() {
        savePersistentDefinitions();
        refreshAll();
        return FloatingItemResult.success("FloatingItems reloaded.");
    }

    public FloatingItemResult createFloatingItem(
            String id,
            ItemStack item,
            Store<EntityStore> store,
            Vector3d position,
            FloatingItemTuning tuning,
            boolean persistent) {
        return createFloatingItem(id, item, store, position, tuning, persistent, null);
    }

    public FloatingItemResult createFloatingItemAtPlayer(
            String id,
            UUID player,
            ItemStack item,
            FloatingItemTuning tuning,
            boolean persistent) {
        if (player == null) {
            return FloatingItemResult.failure("Player UUID is required.");
        }
        PlayerRef ref = plugin.getOnlinePlayerRef(player);
        if (ref == null || ref.getReference() == null || !ref.getReference().isValid()) {
            return FloatingItemResult.failure("Player is not online.");
        }
        Store<EntityStore> store = ref.getReference().getStore();
        TransformComponent transform = store != null
                ? store.getComponent(ref.getReference(), TransformComponent.getComponentType())
                : null;
        if (store == null || transform == null || transform.getPosition() == null) {
            return FloatingItemResult.failure("Player position is unavailable.");
        }
        return createFloatingItem(id, item, store, transform.getPosition(), tuning, persistent, player);
    }

    public FloatingItemResult createFloatingItem(
            String id,
            ItemStack item,
            @Nullable Store<EntityStore> store,
            Vector3d position,
            FloatingItemTuning tuning,
            boolean persistent,
            @Nullable UUID createdBy) {
        if (!enabled()) {
            return FloatingItemResult.failure("FloatingItems module is disabled.");
        }
        String normalizedId = normalizeId(id);
        if (normalizedId == null) {
            return FloatingItemResult.failure("Floating item id must use letters, numbers, '_', '-', or '.'.");
        }
        if (item == null || ItemStack.isEmpty(item)) {
            return FloatingItemResult.failure("Floating item requires a non-empty ItemStack.");
        }
        if (position == null) {
            return FloatingItemResult.failure("Floating item position is required.");
        }
        HyExtrasConfig config = plugin.getExtrasConfig();
        int maxItems = config != null ? config.floatingItemsMaxItems : 512;
        if (!items.containsKey(normalizedId) && items.size() >= Math.max(0, maxItems)) {
            return FloatingItemResult.failure("Floating item limit reached: " + maxItems);
        }
        FloatingItemTuning resolvedTuning = (tuning == null ? FloatingItemTuning.defaults(config) : tuning)
                .withFallbacks(config);
        boolean defaultIntangible = config == null || config.floatingItemsDefaultIntangible;
        FloatingItemInstance instance = new FloatingItemInstance(
                normalizedId,
                UUID.randomUUID(),
                item.cleanCopy(),
                store,
                position,
                Set.of(),
                persistent,
                defaultIntangible,
                resolvedTuning,
                createdBy,
                Instant.now());
        FloatingItemInstance previous = items.put(normalizedId, instance);
        if (previous != null) {
            renderer.clear(previous);
        }
        if (persistent || previous != null && previous.persistent()) {
            savePersistentDefinitions();
        }
        boolean rendered = renderer.refresh(instance, "create");
        return rendered
                ? FloatingItemResult.item(instance.uuid(), "Floating item created.")
                : new FloatingItemResult(
                        false,
                        "Floating item state recorded, but display backend is unavailable.",
                        instance.uuid());
    }

    public FloatingItemResult removeFloatingItem(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null) {
            return FloatingItemResult.failure("Floating item id is required.");
        }
        FloatingItemInstance removed = items.remove(normalizedId);
        if (removed == null) {
            return FloatingItemResult.failure("Floating item does not exist: " + normalizedId);
        }
        renderer.clear(removed);
        if (removed.persistent()) {
            savePersistentDefinitions();
        }
        return FloatingItemResult.success("Floating item removed.");
    }

    public FloatingItemResult setFloatingItemIntangible(String id, boolean intangible) {
        String normalizedId = normalizeId(id);
        FloatingItemInstance current = normalizedId != null ? items.get(normalizedId) : null;
        if (current == null) {
            return FloatingItemResult.failure("Floating item does not exist: " + id);
        }
        FloatingItemInstance next = current.withIntangible(intangible);
        items.put(normalizedId, next);
        if (next.persistent()) {
            savePersistentDefinitions();
        }
        boolean rendered = renderer.refresh(next, "intangible");
        return rendered
                ? FloatingItemResult.success("Floating item intangible=" + intangible)
                : FloatingItemResult.failure("Floating item state updated, but display backend is unavailable.");
    }

    public FloatingItemResult moveFloatingItem(String id, Store<EntityStore> store, Vector3d position) {
        String normalizedId = normalizeId(id);
        FloatingItemInstance current = normalizedId != null ? items.get(normalizedId) : null;
        if (current == null) {
            return FloatingItemResult.failure("Floating item does not exist: " + id);
        }
        if (store == null || position == null) {
            return FloatingItemResult.failure("Store and position are required.");
        }
        FloatingItemInstance next = current.withPosition(store, position);
        items.put(normalizedId, next);
        if (next.persistent()) {
            savePersistentDefinitions();
        }
        boolean rendered = renderer.refresh(next, "move");
        return rendered
                ? FloatingItemResult.success("Floating item moved.")
                : FloatingItemResult.failure("Floating item state moved, but display backend is unavailable.");
    }

    @Nullable
    public FloatingItemInstance snapshotFloatingItem(String id) {
        String normalizedId = normalizeId(id);
        return normalizedId == null ? null : items.get(normalizedId);
    }

    public Map<String, FloatingItemInstance> snapshotFloatingItems() {
        return Map.copyOf(items);
    }

    public Map<String, FloatingItemInstance> snapshotFloatingItemsNear(
            Store<EntityStore> store,
            Vector3d origin,
            double radius) {
        if (store == null || origin == null || radius < 0.0D) {
            return Map.of();
        }
        double radiusSquared = radius * radius;
        Map<String, FloatingItemInstance> matches = new HashMap<>();
        items.forEach((id, item) -> {
            if (item.store() == store && item.position().distanceSquared(origin) <= radiusSquared) {
                matches.put(id, item);
            }
        });
        return Map.copyOf(matches);
    }

    public boolean exists(String id) {
        String normalizedId = normalizeId(id);
        return normalizedId != null && items.containsKey(normalizedId);
    }

    public boolean isIntangible(String id) {
        FloatingItemInstance item = snapshotFloatingItem(id);
        return item != null && item.intangible();
    }

    private void refreshAll() {
        items.values().forEach(item -> renderer.refresh(item, "reload"));
    }

    private void loadPersistentDefinitions() {
        Path file = persistenceFile();
        if (!Files.exists(file)) {
            return;
        }
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras floating-items] Failed to load persistent definitions.");
            return;
        }
        String ids = props.getProperty("items", "");
        for (String rawId : ids.split(",")) {
            String id = normalizeId(rawId);
            if (id == null || items.containsKey(id)) {
                continue;
            }
            String prefix = "item." + id + ".";
            String itemId = props.getProperty(prefix + "itemId", "");
            int quantity = parseInt(props.getProperty(prefix + "quantity"), 1);
            Vector3d position = new Vector3d(
                    parseDouble(props.getProperty(prefix + "x"), 0.0D),
                    parseDouble(props.getProperty(prefix + "y"), 0.0D),
                    parseDouble(props.getProperty(prefix + "z"), 0.0D));
            FloatingItemTuning tuning = new FloatingItemTuning(
                    parseFloat(props.getProperty(prefix + "scale"), 1.0f),
                    parseFloat(props.getProperty(prefix + "visibilityRadius"), 48.0f),
                    parseFloat(props.getProperty(prefix + "bobAmplitude"), 0.15f),
                    parseFloat(props.getProperty(prefix + "rotationDegreesPerSecond"), 45.0f),
                    parseFloat(props.getProperty(prefix + "offsetX"), 0.0f),
                    parseFloat(props.getProperty(prefix + "offsetY"), 0.0f),
                    parseFloat(props.getProperty(prefix + "offsetZ"), 0.0f),
                    parseInt(props.getProperty(prefix + "priority"), 0));
            boolean intangible = Boolean.parseBoolean(props.getProperty(prefix + "intangible", "true"));
            FloatingItemInstance instance = new FloatingItemInstance(
                    id,
                    UUID.randomUUID(),
                    new ItemStack(itemId, Math.max(1, quantity)),
                    null,
                    position,
                    Set.of(),
                    true,
                    intangible,
                    tuning.withFallbacks(plugin.getExtrasConfig()),
                    null,
                    Instant.now());
            items.put(id, instance);
        }
    }

    private void savePersistentDefinitions() {
        Properties props = new Properties();
        String ids = String.join(",", items.values().stream()
                .filter(FloatingItemInstance::persistent)
                .map(FloatingItemInstance::id)
                .sorted()
                .toList());
        props.setProperty("items", ids);
        items.values().stream()
                .filter(FloatingItemInstance::persistent)
                .forEach(item -> {
                    String prefix = "item." + item.id() + ".";
                    props.setProperty(prefix + "itemId", item.itemId());
                    props.setProperty(prefix + "quantity", String.valueOf(Math.max(1, item.quantity())));
                    props.setProperty(prefix + "x", String.valueOf(item.position().x));
                    props.setProperty(prefix + "y", String.valueOf(item.position().y));
                    props.setProperty(prefix + "z", String.valueOf(item.position().z));
                    props.setProperty(prefix + "intangible", String.valueOf(item.intangible()));
                    props.setProperty(prefix + "scale", String.valueOf(item.tuning().scale()));
                    props.setProperty(prefix + "visibilityRadius", String.valueOf(item.tuning().visibilityRadius()));
                    props.setProperty(prefix + "bobAmplitude", String.valueOf(item.tuning().bobAmplitude()));
                    props.setProperty(
                            prefix + "rotationDegreesPerSecond",
                            String.valueOf(item.tuning().rotationDegreesPerSecond()));
                    props.setProperty(prefix + "offsetX", String.valueOf(item.tuning().offsetX()));
                    props.setProperty(prefix + "offsetY", String.valueOf(item.tuning().offsetY()));
                    props.setProperty(prefix + "offsetZ", String.valueOf(item.tuning().offsetZ()));
                    props.setProperty(prefix + "priority", String.valueOf(item.tuning().priority()));
                });
        Path file = persistenceFile();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                props.store(writer, "HyExtras FloatingItems persistent definitions");
            }
        } catch (IOException e) {
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras floating-items] Failed to save persistent definitions.");
        }
    }

    private Path persistenceFile() {
        return plugin.getDataDirectory().resolve(PERSISTENCE_FILE);
    }

    private boolean enabled() {
        return plugin.isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS);
    }

    private static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_.-]+") ? normalized : null;
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return raw == null ? fallback : Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(String raw, float fallback) {
        try {
            return raw == null ? fallback : Float.parseFloat(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static double parseDouble(String raw, double fallback) {
        try {
            return raw == null ? fallback : Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
