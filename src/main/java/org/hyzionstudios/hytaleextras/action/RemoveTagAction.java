package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

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
        try {
            if (tag == null || tag.isBlank()) {
                HytaleextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[remove_tag] skipped: tag is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HytaleextrasPlugin.get().getTagService().removeTag(uuid, tag);
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[remove_tag] failed for tag=" + tag);
        }
    }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
