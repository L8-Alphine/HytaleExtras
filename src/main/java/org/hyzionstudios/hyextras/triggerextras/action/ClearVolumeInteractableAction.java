package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class ClearVolumeInteractableAction extends TriggerEffect {

    public static final BuilderCodec<ClearVolumeInteractableAction> CODEC = BuilderCodec.builder(
                    ClearVolumeInteractableAction.class, ClearVolumeInteractableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("VolumeId"),
                    ClearVolumeInteractableAction::setVolumeId,
                    ClearVolumeInteractableAction::getVolumeId).add()
            .build();

    @Nullable private String volumeId;

    public ClearVolumeInteractableAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            String targetId = volumeId != null && !volumeId.isBlank() ? volumeId : ctx.getVolume().getId();
            HyExtrasPlugin.get().getInteractableVolumeState().clear(targetId);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[clear_volume_interactable] failed for volume=" + volumeId);
        }
    }

    @Nullable public String getVolumeId() { return volumeId; }
    public void setVolumeId(@Nullable String volumeId) { this.volumeId = volumeId; }
}
