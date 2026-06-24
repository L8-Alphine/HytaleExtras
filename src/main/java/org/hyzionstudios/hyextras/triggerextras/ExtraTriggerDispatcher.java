package org.hyzionstudios.hyextras.triggerextras;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Shared synchronous dispatcher for HyExtras synthetic trigger events.
 *
 * <p>This is intentionally small and conservative. Native enter/exit/tick/block
 * events still belong to Hytale's trigger volume system; this helper covers
 * plugin-created events such as interaction dispatch and named volume chaining.
 */
public final class ExtraTriggerDispatcher {

    private ExtraTriggerDispatcher() {}

    public static boolean dispatch(
            VolumeEntry volume,
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            TriggerEventType eventType,
            List<VolumeEntry> spatialVolumes,
            @Nullable String tagKey,
            @Nullable String tagValue,
            @Nullable Vector3d blockPosition,
            @Nullable String blockId) {
        return dispatch(volume, entityRef, store, eventType, spatialVolumes,
                tagKey, tagValue, blockPosition, blockId, true);
    }

    public static boolean dispatch(
            VolumeEntry volume,
            Ref<EntityStore> entityRef,
            Store<EntityStore> store,
            TriggerEventType eventType,
            List<VolumeEntry> spatialVolumes,
            @Nullable String tagKey,
            @Nullable String tagValue,
            @Nullable Vector3d blockPosition,
            @Nullable String blockId,
            boolean respectCooldown) {
        try {
            if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
                return false;
            }
            if (volume == null || !volume.isEnabled()) return false;

            UUID entityUuid = TriggerVolumeApiAdapter.getEntityUuid(
                    new TriggerContext(entityRef, store, eventType, volume, spatialVolumes,
                            tagKey, tagValue, blockPosition, blockId));
            long now = System.nanoTime();
            if (respectCooldown && entityUuid != null && volume.isOnCooldown(entityUuid, now)) {
                return false;
            }

            TriggerContext ctx = new TriggerContext(
                    entityRef, store, eventType, volume, spatialVolumes,
                    tagKey, tagValue, blockPosition, blockId);

            for (TriggerCondition condition : volume.getConditions()) {
                if (condition.getEventType() != eventType) continue;
                boolean accepted;
                try {
                    accepted = condition.test(ctx);
                } catch (Exception e) {
                    HyExtrasPlugin.get().getLogger()
                            .at(Level.WARNING).withCause(e)
                            .log("[hextras dispatcher] condition error in volume=" + volume.getId());
                    accepted = false;
                }
                if (!accepted) {
                    fireEffects(volume.getRejectionEffects(), eventType, ctx, "rejection", volume.getId());
                    return false;
                }
                try {
                    condition.applyOnAccept(ctx);
                } catch (Exception e) {
                    HyExtrasPlugin.get().getLogger()
                            .at(Level.WARNING).withCause(e)
                            .log("[hextras dispatcher] condition accept hook error in volume=" + volume.getId());
                    fireEffects(volume.getRejectionEffects(), eventType, ctx, "rejection", volume.getId());
                    return false;
                }
            }

            fireEffects(volume.getEffects(), eventType, ctx, "effect", volume.getId());
            if (respectCooldown && entityUuid != null) {
                volume.recordActivation(entityUuid, now);
            }
            return true;
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[hextras dispatcher] dispatch failed for volume=" + (volume != null ? volume.getId() : "<null>"));
            return false;
        }
    }

    private static void fireEffects(
            List<TriggerEffect> effects,
            TriggerEventType eventType,
            TriggerContext ctx,
            String kind,
            String volumeId) {
        for (TriggerEffect effect : effects) {
            if (effect.getEventType() != eventType) continue;
            try {
                effect.execute(ctx);
            } catch (Exception e) {
                HyExtrasPlugin.get().getLogger()
                        .at(Level.WARNING).withCause(e)
                        .log("[hextras dispatcher] " + kind + " error in volume=" + volumeId);
            }
        }
    }
}
