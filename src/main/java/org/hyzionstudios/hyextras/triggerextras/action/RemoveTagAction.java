package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Removes a boolean tag from the triggering player's persistent tag set.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "remove_tag", "tag": "storyline_a_active" }
 * }</pre>
 */
public class RemoveTagAction extends TriggerEffect {

    public static final BuilderCodec<RemoveTagAction> CODEC = BuilderCodec.builder(
                    RemoveTagAction.class, RemoveTagAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Tag"), RemoveTagAction::setTag, RemoveTagAction::getTag).add()
            .build();

    private String tag;

    public RemoveTagAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (tag == null || tag.isBlank()) {
                HyExtrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[remove_tag] skipped: tag is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HyExtrasPlugin.get().getTagService().removeTag(uuid, tag);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[remove_tag] failed for tag=" + tag);
        }
    }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
