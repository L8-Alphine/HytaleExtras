package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;

import java.util.UUID;

public final class TagNpcAddTagAction extends TriggerEffect {

    public static final BuilderCodec<TagNpcAddTagAction> CODEC = BuilderCodec.builder(
                    TagNpcAddTagAction.class, TagNpcAddTagAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcAddTagAction::setTarget, TagNpcAddTagAction::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcAddTagAction::setEntityUuid, TagNpcAddTagAction::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcAddTagAction::setTargetTag, TagNpcAddTagAction::getTargetTag).add()
            .append(CodecHelper.string("Tag"), TagNpcAddTagAction::setTag, TagNpcAddTagAction::getTag).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String tag;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return;
        }
        if (tag == null || tag.isBlank()) {
            return;
        }
        String resolvedTag = StringTemplate.resolve(tag, ctx, HyExtrasPlugin.get().getVariableService());
        for (UUID entity : TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag))) {
            HyExtrasPlugin.get().getTagNpcService().addTag(entity, resolvedTag);
        }
    }

    public TagNpcTarget getTarget() { return target; }
    public void setTarget(TagNpcTarget target) { this.target = target; }
    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid; }
    public String getTargetTag() { return targetTag; }
    public void setTargetTag(String targetTag) { this.targetTag = targetTag; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
