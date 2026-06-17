package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Condition that passes when the triggering player has (or lacks) a persistent tag.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "has_tag", "tag": "storyline_a_active" }
 * { "type": "has_tag", "tag": "storyline_b_active", "invert": true }
 * }</pre>
 */
public class HasTagCondition extends TriggerCondition {

    public static final BuilderCodec<HasTagCondition> CODEC = BuilderCodec.builder(
                    HasTagCondition.class, HasTagCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("Tag"), HasTagCondition::setTag, HasTagCondition::getTag).add()
            .append(CodecHelper.optBool("Invert"), HasTagCondition::setInvert, HasTagCondition::getInvert).add()
            .build();

    private String tag;
    @Nullable private Boolean invert;

    public HasTagCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            if (tag == null || tag.isBlank()) {
                HytaleextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[has_tag] skipped: tag is empty");
                return false;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return false;
            boolean result = HytaleextrasPlugin.get().getTagService().hasTag(uuid, tag);
            return (invert != null && invert) != result;
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[has_tag] test failed for tag=" + tag);
            return false;
        }
    }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    @Nullable public Boolean getInvert() { return invert; }
    public void setInvert(@Nullable Boolean invert) { this.invert = invert; }
}
