package org.hyzionstudios.hyextras.floatingitems;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record FloatingItemInstance(
        String id,
        UUID uuid,
        ItemStack item,
        @Nullable Store<EntityStore> store,
        Vector3d position,
        Set<String> tags,
        boolean persistent,
        boolean intangible,
        FloatingItemTuning tuning,
        @Nullable UUID createdBy,
        Instant createdAt) {

    public FloatingItemInstance {
        position = new Vector3d(position);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public String itemId() {
        return item != null ? item.getItemId() : "";
    }

    public int quantity() {
        return item != null ? item.getQuantity() : 0;
    }

    public FloatingItemInstance withPosition(Store<EntityStore> nextStore, Vector3d nextPosition) {
        return new FloatingItemInstance(
                id, uuid, item, nextStore, nextPosition, tags, persistent, intangible, tuning, createdBy, createdAt);
    }

    public FloatingItemInstance withIntangible(boolean nextIntangible) {
        return new FloatingItemInstance(
                id, uuid, item, store, position, tags, persistent, nextIntangible, tuning, createdBy, createdAt);
    }
}
