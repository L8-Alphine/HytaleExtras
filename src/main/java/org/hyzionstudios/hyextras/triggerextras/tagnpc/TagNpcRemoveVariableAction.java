package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;

import java.util.UUID;

public final class TagNpcRemoveVariableAction extends TriggerEffect {

    public static final BuilderCodec<TagNpcRemoveVariableAction> CODEC = BuilderCodec.builder(
                    TagNpcRemoveVariableAction.class, TagNpcRemoveVariableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcRemoveVariableAction::setTarget, TagNpcRemoveVariableAction::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcRemoveVariableAction::setEntityUuid, TagNpcRemoveVariableAction::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcRemoveVariableAction::setTargetTag, TagNpcRemoveVariableAction::getTargetTag).add()
            .append(CodecHelper.string("Key"),
                    TagNpcRemoveVariableAction::setKey, TagNpcRemoveVariableAction::getKey).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String key;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return;
        }
        if (key == null || key.isBlank()) {
            return;
        }
        for (UUID entity : TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag))) {
            HyExtrasPlugin.get().getTagNpcService().removeVariable(entity, key);
        }
    }

    public TagNpcTarget getTarget() { return target; }
    public void setTarget(TagNpcTarget target) { this.target = target; }
    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid; }
    public String getTargetTag() { return targetTag; }
    public void setTargetTag(String targetTag) { this.targetTag = targetTag; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
