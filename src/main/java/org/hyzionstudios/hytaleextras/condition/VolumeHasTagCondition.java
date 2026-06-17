package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;

/**
 * Condition that passes when a named volume has (or does not have) a specific tag.
 * Optionally checks that the tag equals an expected value.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "volume_has_tag", "TargetVolumeId": "door_lock", "Key": "locked" }
 * { "type": "volume_has_tag", "TargetVolumeId": "door_lock", "Key": "locked", "Value": "1" }
 * { "type": "volume_has_tag", "TargetVolumeId": "gate", "Key": "open", "Invert": true }
 * }</pre>
 */
public class VolumeHasTagCondition extends TriggerCondition {

    public static final BuilderCodec<VolumeHasTagCondition> CODEC = BuilderCodec.builder(
                    VolumeHasTagCondition.class, VolumeHasTagCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.optString("TargetVolumeId"),
                    VolumeHasTagCondition::setTargetVolumeId,
                    VolumeHasTagCondition::getTargetVolumeId).add()
            .append(CodecHelper.string("Key"),
                    VolumeHasTagCondition::setKey,
                    VolumeHasTagCondition::getKey).add()
            .append(CodecHelper.optString("Value"),
                    VolumeHasTagCondition::setValue,
                    VolumeHasTagCondition::getValue).add()
            .append(CodecHelper.optBool("Invert"),
                    VolumeHasTagCondition::setInvert,
                    VolumeHasTagCondition::getInvert).add()
            .build();

    @Nullable private String targetVolumeId;
    private String key;
    @Nullable private String value;
    @Nullable private Boolean invert;

    public VolumeHasTagCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            if (key == null || key.isBlank()) {
                warn("key is empty"); return false;
            }
            TriggerVolumeManager mgr = TriggerVolumeApiAdapter.getManagerForStore(ctx.getStore());
            if (mgr == null) { warn("TriggerVolumeManager unavailable"); return false; }
            String targetId = targetVolumeId != null && !targetVolumeId.isBlank()
                    ? targetVolumeId
                    : ctx.getVolume().getId();
            VolumeEntry target = mgr.getVolume(targetId);
            if (target == null) { warn("volume not found: " + targetId); return false; }

            Map<String, String> tags = target.getRawTags();
            boolean hasKey = tags != null && tags.containsKey(key);
            boolean result = value != null
                    ? hasKey && value.equals(tags.get(key))
                    : hasKey;

            return (invert != null && invert) != result;
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[volume_has_tag] test failed: volume=" + targetVolumeId + " key=" + key);
            return false;
        }
    }

    private void warn(String reason) {
        HyextrasPlugin.get().getLogger()
                .at(Level.WARNING).log("[volume_has_tag] skipped: " + reason);
    }

    @Nullable public String getTargetVolumeId() { return targetVolumeId; }
    public void setTargetVolumeId(@Nullable String v) { this.targetVolumeId = v; }
    public String getKey() { return key; }
    public void setKey(String k) { this.key = k; }
    @Nullable public String getValue() { return value; }
    public void setValue(@Nullable String v) { this.value = v; }
    @Nullable public Boolean getInvert() { return invert; }
    public void setInvert(@Nullable Boolean b) { this.invert = b; }
}
