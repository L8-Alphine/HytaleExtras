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
 * Sets a tag on a named volume. Fires TAG_ADDED on that volume for the current entity,
 * enabling cross-volume communication via the native On Tag Added event.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "set_volume_tag", "TargetVolumeId": "door_lock", "Key": "locked", "Value": "1" }
 * { "type": "set_volume_tag", "TargetVolumeId": "scoreboard", "Key": "owner", "Value": "{player}" }
 * }</pre>
 */
public class SetVolumeTagAction extends TriggerEffect {

    public static final BuilderCodec<SetVolumeTagAction> CODEC = BuilderCodec.builder(
                    SetVolumeTagAction.class, SetVolumeTagAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("TargetVolumeId"),
                    SetVolumeTagAction::setTargetVolumeId,
                    SetVolumeTagAction::getTargetVolumeId).add()
            .append(CodecHelper.string("Key"),
                    SetVolumeTagAction::setKey,
                    SetVolumeTagAction::getKey).add()
            .append(CodecHelper.optString("Value"),
                    SetVolumeTagAction::setValue,
                    SetVolumeTagAction::getValue).add()
            .build();

    @Nullable private String targetVolumeId;
    private String key;
    @Nullable private String value; // default "1" if omitted

    public SetVolumeTagAction() {}

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

            String resolvedValue = StringTemplate.resolve(
                    value != null ? value : "1", ctx,
                    HyExtrasPlugin.get().getVariableService());

            String targetId = targetVolumeId != null && !targetVolumeId.isBlank()
                    ? targetVolumeId
                    : ctx.getVolume().getId();
            mgr.setTag(targetId, key, resolvedValue, ctx.getEntityRef(), uuid);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[set_volume_tag] failed: volume=" + targetVolumeId + " key=" + key);
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING).log("[set_volume_tag] skipped: " + reason);
    }

    @Nullable public String getTargetVolumeId() { return targetVolumeId; }
    public void setTargetVolumeId(@Nullable String v) { this.targetVolumeId = v; }
    public String getKey() { return key; }
    public void setKey(String k) { this.key = k; }
    @Nullable public String getValue() { return value; }
    public void setValue(@Nullable String v) { this.value = v; }
}
