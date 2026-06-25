package org.hyzionstudios.hyextras.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Shared entity resolution and classification so the packet, interaction, and TagNPC
 * subsystems agree on how to turn network ids / refs into entities and how to tell
 * players apart from non-player entities, instead of each doing ad-hoc
 * {@code getComponent(ref, PlayerRef...)} checks.
 *
 * <p>NPC-vs-mob discrimination is intentionally conservative until the Hytale 0.5.6
 * component surface for it is confirmed (tracked by the API spike); non-player entities
 * currently classify as {@link EntityKind#NON_PLAYER}.
 */
public final class EntityResolver {

    public enum EntityKind { PLAYER, NON_PLAYER, UNKNOWN }

    private EntityResolver() {}

    /** Resolves an entity ref from a network id within the store, or null when unresolvable/invalid. */
    @Nullable
    public static Ref<EntityStore> resolveByNetworkId(@Nullable Store<EntityStore> store, int networkId) {
        if (store == null) {
            return null;
        }
        EntityStore entityStore = store.getExternalData();
        if (entityStore == null) {
            return null;
        }
        Ref<EntityStore> ref = entityStore.getRefFromNetworkId(networkId);
        return (ref != null && ref.isValid()) ? ref : null;
    }

    /** Returns the {@link PlayerRef} component for the ref, or null when it is not a player. */
    @Nullable
    public static PlayerRef playerRef(@Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        return store.getComponent(ref, PlayerRef.getComponentType());
    }

    /** Returns true when the ref resolves to a player entity. */
    public static boolean isPlayer(@Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref) {
        return playerRef(store, ref) != null;
    }

    /** Resolves the stable UUID of an entity ref — player UUID for players, else its UUIDComponent. */
    @Nullable
    public static UUID uuid(@Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        PlayerRef playerRef = playerRef(store, ref);
        if (playerRef != null) {
            return playerRef.getUuid();
        }
        UUIDComponent component = store.getComponent(ref, UUIDComponent.getComponentType());
        return component != null ? component.getUuid() : null;
    }

    /**
     * Classifies an entity ref. Non-player entities are not yet split into NPC vs mob; that
     * distinction is deferred to the API spike and will be filled in here so all callers benefit.
     */
    public static EntityKind classify(@Nullable Store<EntityStore> store, @Nullable Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return EntityKind.UNKNOWN;
        }
        return isPlayer(store, ref) ? EntityKind.PLAYER : EntityKind.NON_PLAYER;
    }
}
