package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;

import java.util.UUID;

public final class TagNpcHideEntityAction extends TriggerEffect {

    public static final BuilderCodec<TagNpcHideEntityAction> CODEC = BuilderCodec.builder(
                    TagNpcHideEntityAction.class, TagNpcHideEntityAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optEnum("Target", TagNpcTarget.class, TagNpcTarget.ALIASES),
                    TagNpcHideEntityAction::setTarget, TagNpcHideEntityAction::getTarget).add()
            .append(CodecHelper.optString("EntityUuid"),
                    TagNpcHideEntityAction::setEntityUuid, TagNpcHideEntityAction::getEntityUuid).add()
            .append(CodecHelper.optString("TargetTag"),
                    TagNpcHideEntityAction::setTargetTag, TagNpcHideEntityAction::getTargetTag).add()
            .append(CodecHelper.optString("ViewerUuid"),
                    TagNpcHideEntityAction::setViewerUuid, TagNpcHideEntityAction::getViewerUuid).add()
            .build();

    private TagNpcTarget target;
    private String entityUuid;
    private String targetTag;
    private String viewerUuid;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled() || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_TAG_NPC)) {
            return;
        }
        UUID viewer = TagNpcTarget.resolveOne(ctx, viewerUuid);
        if (viewer == null) {
            return;
        }
        for (UUID entity : TagNpcTarget.ordered(TagNpcTarget.resolveMany(ctx, target, entityUuid, targetTag))) {
            HyExtrasPlugin.get().getTagNpcService().hideEntityFromViewer(viewer, entity);
        }
    }

    public TagNpcTarget getTarget() { return target; }
    public void setTarget(TagNpcTarget target) { this.target = target; }
    public String getEntityUuid() { return entityUuid; }
    public void setEntityUuid(String entityUuid) { this.entityUuid = entityUuid; }
    public String getTargetTag() { return targetTag; }
    public void setTargetTag(String targetTag) { this.targetTag = targetTag; }
    public String getViewerUuid() { return viewerUuid; }
    public void setViewerUuid(String viewerUuid) { this.viewerUuid = viewerUuid; }
}
