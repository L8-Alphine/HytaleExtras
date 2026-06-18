package org.hyzionstudios.hyextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Removes a tag from a named volume. Fires TAG_REMOVED on that volume for the current entity.
 * If {@code Value} is set, the tag is only removed when the current tag value matches.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "remove_volume_tag", "TargetVolumeId": "door_lock", "Key": "locked" }
 * { "type": "remove_volume_tag", "TargetVolumeId": "door_lock", "Key": "locked", "Value": "1" }
 * }</pre>
 */
public class RemoveVolumeTagAction extends TriggerEffect {

    public static final BuilderCodec<RemoveVolumeTagAction> CODEC = BuilderCodec.builder(
                    RemoveVolumeTagAction.class, RemoveVolumeTagAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("TargetVolumeId"),
                    RemoveVolumeTagAction::setTargetVolumeId,
                    RemoveVolumeTagAction::getTargetVolumeId).add()
            .append(CodecHelper.string("Key"),
                    RemoveVolumeTagAction::setKey,
                    RemoveVolumeTagAction::getKey).add()
            .append(CodecHelper.optString("Value"),
                    RemoveVolumeTagAction::setValue,
                    RemoveVolumeTagAction::getValue).add()
            .build();

    @Nullable private String targetVolumeId;
    private String key;
    @Nullable private String value; // if set, only remove if the current tag value matches

    public RemoveVolumeTagAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (key == null || key.isBlank()) {
                warn("key is empty"); return;
            }
            var mgr = TriggerVolumeApiAdapter.getManagerForStore(ctx.getStore());
            if (mgr == null) { warn("TriggerVolumeManager unavailable"); return; }

            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) { warn("entity is not a player"); return; }

            String targetId = targetVolumeId != null && !targetVolumeId.isBlank()
                    ? targetVolumeId
                    : ctx.getVolume().getId();
            if (value != null) {
                String resolvedValue = StringTemplate.resolve(
                        value, ctx, HyExtrasPlugin.get().getVariableService());
                mgr.removeTag(targetId, key, resolvedValue, ctx.getEntityRef(), uuid);
            } else {
                mgr.removeTag(targetId, key, ctx.getEntityRef(), uuid);
            }
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[remove_volume_tag] failed: volume=" + targetVolumeId + " key=" + key);
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING).log("[remove_volume_tag] skipped: " + reason);
    }

    @Nullable public String getTargetVolumeId() { return targetVolumeId; }
    public void setTargetVolumeId(@Nullable String v) { this.targetVolumeId = v; }
    public String getKey() { return key; }
    public void setKey(String k) { this.key = k; }
    @Nullable public String getValue() { return value; }
    public void setValue(@Nullable String v) { this.value = v; }
}
