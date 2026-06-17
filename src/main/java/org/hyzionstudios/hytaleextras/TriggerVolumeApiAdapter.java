package org.hyzionstudios.hytaleextras;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * All access to {@link TriggerVolumesPlugin} must go through this class.
 * Wrapping in try-catch ensures a broken effect/condition never crashes the whole mod.
 */
public final class TriggerVolumeApiAdapter {

    private TriggerVolumeApiAdapter() {}

    public static <T extends TriggerEffect> boolean registerEffect(
            String typeId, Class<T> clazz, BuilderCodec<T> codec) {
        try {
            TriggerVolumesPlugin.get().registerEffectType(typeId, clazz, codec);
            return true;
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.SEVERE).withCause(e)
                    .log("Failed to register effect type: " + typeId);
            return false;
        }
    }

    public static <T extends TriggerCondition> boolean registerCondition(
            String typeId, Class<T> clazz, BuilderCodec<T> codec) {
        try {
            TriggerVolumesPlugin.get().registerConditionType(typeId, clazz, codec);
            return true;
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.SEVERE).withCause(e)
                    .log("Failed to register condition type: " + typeId);
            return false;
        }
    }

    /**
     * Returns the UUID of the triggering entity by reading its {@link PlayerRef} component,
     * or {@code null} if the entity is not a player (e.g. NPC, projectile).
     */
    @Nullable
    public static UUID getEntityUuid(TriggerContext ctx) {
        PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
        return pr != null ? pr.getUuid() : null;
    }

    /**
     * Returns the {@link TriggerVolumeManager} for the world that owns the given store,
     * or {@code null} if it cannot be resolved (e.g. world is being unloaded).
     */
    @Nullable
    public static TriggerVolumeManager getManagerForStore(Store<EntityStore> store) {
        try {
            ResourceType<EntityStore, TriggerVolumeManager> rt =
                    TriggerVolumesPlugin.get().getManagerResourceType();
            return store.getResource(rt);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("Could not resolve TriggerVolumeManager from store.");
            return null;
        }
    }

    /**
     * Returns the current world hour (0–23) from {@link WorldTimeResource}, or {@code -1}
     * if the resource is unavailable (e.g. world not loaded).
     */
    public static int getWorldHour(Store<EntityStore> store) {
        try {
            WorldTimeResource wt = store.getResource(WorldTimeResource.getResourceType());
            return wt != null ? wt.getCurrentHour() : -1;
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("Could not read WorldTimeResource.");
            return -1;
        }
    }

    /**
     * Returns the {@link HiddenPlayersManager} for the entity identified by {@code ref},
     * or {@code null} if the entity is not a player or the component is missing.
     */
    @Nullable
    public static HiddenPlayersManager getHiddenPlayersManager(Store<EntityStore> store, Ref<EntityStore> ref) {
        try {
            PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
            return pr != null ? pr.getHiddenPlayersManager() : null;
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("Could not get HiddenPlayersManager.");
            return null;
        }
    }

    /**
     * Resolves an online player UUID by username using the plugin's player registry.
     * Returns {@code null} if no online player with that name is found.
     */
    @Nullable
    public static UUID getPlayerUuidByName(Store<EntityStore> store, String username) {
        return HyextrasPlugin.get().getPlayerUuidByName(username);
    }
}
