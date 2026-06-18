package org.hyzionstudios.hyextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.ExtraTriggerDispatcher;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import java.util.logging.Level;

/**
 * Dispatches another named trigger volume against the current entity.
 *
 * <p>The target volume is evaluated using the current event type and a target-volume
 * context, so target volume conditions and volume-aware effects behave as expected.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "trigger_named_volume", "VolumeId": "reward_fanfare_volume" }
 * }</pre>
 */
public class TriggerNamedVolumeAction extends TriggerEffect {

    public static final BuilderCodec<TriggerNamedVolumeAction> CODEC = BuilderCodec.builder(
                    TriggerNamedVolumeAction.class, TriggerNamedVolumeAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("VolumeId"), TriggerNamedVolumeAction::setVolumeId, TriggerNamedVolumeAction::getVolumeId).add()
            .build();

    private String volumeId;

    public TriggerNamedVolumeAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (volumeId == null || volumeId.isBlank()) {
                warn("volumeId is empty");
                return;
            }
            TriggerVolumeManager mgr = TriggerVolumeApiAdapter.getManagerForStore(ctx.getStore());
            if (mgr == null) {
                warn("could not resolve TriggerVolumeManager");
                return;
            }
            VolumeEntry target = mgr.getVolume(volumeId);
            if (target == null) {
                HyExtrasPlugin.get().getLogger()
                        .at(Level.WARNING)
                        .log("[trigger_named_volume] volume not found: " + volumeId);
                return;
            }
            ExtraTriggerDispatcher.dispatch(
                    target, ctx.getEntityRef(), ctx.getStore(), ctx.getEventType(),
                    ctx.getSpatialVolumes(), ctx.getTagKey(), ctx.getTagValue(),
                    ctx.getBlockPosition(), ctx.getBlockId());
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[trigger_named_volume] failed for volumeId=" + volumeId);
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[trigger_named_volume] skipped: " + reason);
    }

    public String getVolumeId() { return volumeId; }
    public void setVolumeId(String volumeId) { this.volumeId = volumeId; }
}
