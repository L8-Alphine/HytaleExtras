package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;

import java.util.Set;
import java.util.UUID;

public final class TagNpcVisibleCondition extends TriggerCondition {

    public static final BuilderCodec<TagNpcVisibleCondition> CODEC = BuilderCodec.builder(
                    TagNpcVisibleCondition.class, TagNpcVisibleCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcVisibleCondition::setTarget, TagNpcVisibleCondition::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcVisibleCondition::setEntityUuid, TagNpcVisibleCondition::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcVisibleCondition::setTargetTag, TagNpcVisibleCondition::getTargetTag).add()
            .append(CodecHelper.optString("ViewerUuid"),
                    TagNpcVisibleCondition::setViewerUuid, TagNpcVisibleCondition::getViewerUuid).add()
            .append(CodecHelper.optBool("ExpectedVisible"),
                    TagNpcVisibleCondition::setExpectedVisible,
                    TagNpcVisibleCondition::getExpectedVisible).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String viewerUuid;
    private Boolean expectedVisible;

    @Override
    public boolean test(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return false;
        }
        UUID viewer = TagNpcTarget.resolveOne(ctx, viewerUuid);
        if (viewer == null) {
            return false;
        }
        Set<UUID> entities = TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag));
        if (entities.isEmpty()) {
            return false;
        }
        boolean wantVisible = expectedVisible == null || expectedVisible;
        for (UUID entity : entities) {
            boolean visible = !HyExtrasPlugin.get().getTagNpcService().isEntityHiddenFromViewer(viewer, entity);
            if (visible != wantVisible) {
                return false;
            }
        }
        return true;
    }

    public TagNpcTarget getTarget() { return target; }
    public void setTarget(TagNpcTarget target) { this.target = target; }
    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid; }
    public String getTargetTag() { return targetTag; }
    public void setTargetTag(String targetTag) { this.targetTag = targetTag; }
    public String getViewerUuid() { return viewerUuid; }
    public void setViewerUuid(String viewerUuid) { this.viewerUuid = viewerUuid; }
    public Boolean getExpectedVisible() { return expectedVisible; }
    public void setExpectedVisible(Boolean expectedVisible) { this.expectedVisible = expectedVisible; }
}
