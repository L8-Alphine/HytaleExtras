package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class SetVolumeInteractableAction extends TriggerEffect {

    public static final BuilderCodec<SetVolumeInteractableAction> CODEC = BuilderCodec.builder(
                    SetVolumeInteractableAction.class, SetVolumeInteractableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("VolumeId"),
                    SetVolumeInteractableAction::setVolumeId,
                    SetVolumeInteractableAction::getVolumeId).add()
            .append(CodecHelper.optString("Message"),
                    SetVolumeInteractableAction::setMessage,
                    SetVolumeInteractableAction::getMessage).add()
            .append(CodecHelper.optString("Action"),
                    SetVolumeInteractableAction::setAction,
                    SetVolumeInteractableAction::getAction).add()
            .append(CodecHelper.optString("Key"),
                    SetVolumeInteractableAction::setKey,
                    SetVolumeInteractableAction::getKey).add()
            .append(CodecHelper.optString("Name"),
                    SetVolumeInteractableAction::setName,
                    SetVolumeInteractableAction::getName).add()
            .append(CodecHelper.optString("InteractionType"),
                    SetVolumeInteractableAction::setInteractionType,
                    SetVolumeInteractableAction::getInteractionType).add()
            .build();

    @Nullable private String volumeId;
    @Nullable private String message;
    @Nullable private String action;
    @Nullable private String key;
    @Nullable private String name;
    @Nullable private String interactionType;

    public SetVolumeInteractableAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            String targetId = volumeId != null && !volumeId.isBlank() ? volumeId : ctx.getVolume().getId();
            HyExtrasPlugin.get().getInteractableVolumeState().set(targetId, message, action, key, name, interactionType);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[set_volume_interactable] failed for volume=" + volumeId);
        }
    }

    @Nullable public String getVolumeId() { return volumeId; }
    public void setVolumeId(@Nullable String volumeId) { this.volumeId = volumeId; }
    @Nullable public String getMessage() { return message; }
    public void setMessage(@Nullable String message) { this.message = message; }
    @Nullable public String getAction() { return action; }
    public void setAction(@Nullable String action) { this.action = action; }
    @Nullable public String getKey() { return key; }
    public void setKey(@Nullable String key) { this.key = key; }
    @Nullable public String getName() { return name; }
    public void setName(@Nullable String name) { this.name = name; }
    @Nullable public String getInteractionType() { return interactionType; }
    public void setInteractionType(@Nullable String interactionType) { this.interactionType = interactionType; }
}
