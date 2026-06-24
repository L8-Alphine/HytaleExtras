package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;

import java.util.UUID;

public final class TagNpcHasTagCondition extends TriggerCondition {

    public static final BuilderCodec<TagNpcHasTagCondition> CODEC = BuilderCodec.builder(
                    TagNpcHasTagCondition.class, TagNpcHasTagCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcHasTagCondition::setTarget, TagNpcHasTagCondition::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcHasTagCondition::setEntityUuid, TagNpcHasTagCondition::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcHasTagCondition::setTargetTag, TagNpcHasTagCondition::getTargetTag).add()
            .append(CodecHelper.string("Tag"), TagNpcHasTagCondition::setTag, TagNpcHasTagCondition::getTag).add()
            .append(CodecHelper.optBool("Invert"),
                    TagNpcHasTagCondition::setInvert, TagNpcHasTagCondition::getInvert).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String tag;
    private Boolean invert;

    @Override
    public boolean test(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return false;
        }
        if (tag == null || tag.isBlank()) {
            return false;
        }
        String resolvedTag = StringTemplate.resolve(tag, ctx, HyExtrasPlugin.get().getVariableService());
        boolean matched = false;
        for (UUID entity : TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag))) {
            if (HyExtrasPlugin.get().getTagNpcService().hasTag(entity, resolvedTag)) {
                matched = true;
                break;
            }
        }
        return Boolean.TRUE.equals(invert) ? !matched : matched;
    }

    public TagNpcTarget getTarget() { return target; }
    public void setTarget(TagNpcTarget target) { this.target = target; }
    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid; }
    public String getTargetTag() { return targetTag; }
    public void setTargetTag(String targetTag) { this.targetTag = targetTag; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public Boolean getInvert() { return invert; }
    public void setInvert(Boolean invert) { this.invert = invert; }
}
