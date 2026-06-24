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

public final class TagNpcSetVariableAction extends TriggerEffect {

    public static final BuilderCodec<TagNpcSetVariableAction> CODEC = BuilderCodec.builder(
                    TagNpcSetVariableAction.class, TagNpcSetVariableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcSetVariableAction::setTarget, TagNpcSetVariableAction::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcSetVariableAction::setEntityUuid, TagNpcSetVariableAction::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcSetVariableAction::setTargetTag, TagNpcSetVariableAction::getTargetTag).add()
            .append(CodecHelper.string("Key"), TagNpcSetVariableAction::setKey, TagNpcSetVariableAction::getKey).add()
            .append(CodecHelper.string("Value"),
                    TagNpcSetVariableAction::setValue, TagNpcSetVariableAction::getValue).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String key;
    private String value;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return;
        }
        if (key == null || key.isBlank()) {
            return;
        }
        String resolvedValue = value == null
                ? ""
                : StringTemplate.resolve(value, ctx, HyExtrasPlugin.get().getVariableService());
        for (UUID entity : TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag))) {
            HyExtrasPlugin.get().getTagNpcService().setVariable(entity, key, resolvedValue);
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
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
