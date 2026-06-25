package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.ComparisonOperator;
import org.hyzionstudios.hyextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.UUID;

public final class TagNpcVariableCondition extends TriggerCondition {

    public static final BuilderCodec<TagNpcVariableCondition> CODEC = BuilderCodec.builder(
                    TagNpcVariableCondition.class, TagNpcVariableCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcVariableCondition::setTarget, TagNpcVariableCondition::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcVariableCondition::setEntityUuid, TagNpcVariableCondition::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcVariableCondition::setTargetTag, TagNpcVariableCondition::getTargetTag).add()
            .append(CodecHelper.string("Key"), TagNpcVariableCondition::setKey, TagNpcVariableCondition::getKey).add()
            .append(CodecHelper.enumField("Operator", ComparisonOperator.class, ComparisonOperator.ALIASES),
                    TagNpcVariableCondition::setOperator, TagNpcVariableCondition::getOperator).add()
            .append(CodecHelper.optString("Value"),
                    TagNpcVariableCondition::setValue, TagNpcVariableCondition::getValue).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String key;
    private ComparisonOperator operator;
    @Nullable private String value;

    @Override
    public boolean test(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return false;
        }
        if (key == null || key.isBlank() || operator == null) {
            return false;
        }
        String resolvedValue = value == null
                ? null
                : StringTemplate.resolve(value, ctx, HyExtrasPlugin.get().getVariableService());
        for (UUID entity : TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag))) {
            if (matches(entity, resolvedValue)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(UUID entity, @Nullable String expected) {
        Object raw = HyExtrasPlugin.get().getTagNpcService().getVariable(entity, key);
        String actual = raw == null ? null : raw.toString();
        return operator.evaluate(actual, expected, regexEnabled());
    }

    private static boolean regexEnabled() {
        HyExtrasConfig cfg = HyExtrasPlugin.get().getExtrasConfig();
        return cfg == null || cfg.variableRegexEnabled;
    }

    public TagNpcTarget getTarget() { return target; }
    public void setTarget(TagNpcTarget target) { this.target = target; }
    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid; }
    public String getTargetTag() { return targetTag; }
    public void setTargetTag(String targetTag) { this.targetTag = targetTag; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public ComparisonOperator getOperator() { return operator; }
    public void setOperator(ComparisonOperator operator) { this.operator = operator; }
    @Nullable public String getValue() { return value; }
    public void setValue(@Nullable String value) { this.value = value; }
}
