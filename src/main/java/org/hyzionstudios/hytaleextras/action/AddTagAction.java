package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Adds a boolean tag to the triggering player's persistent tag set.
 * Tags survive server restarts; they are loaded on connect and saved on disconnect.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "add_tag", "tag": "storyline_a_active" }
 * { "type": "add_tag", "tag": "found_key_blue" }
 * }</pre>
 */
public class AddTagAction extends TriggerEffect {

    public static final BuilderCodec<AddTagAction> CODEC = BuilderCodec.builder(
                    AddTagAction.class, AddTagAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Tag"), AddTagAction::setTag, AddTagAction::getTag).add()
            .build();

    private String tag;

    public AddTagAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (tag == null || tag.isBlank()) {
                HyextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[add_tag] skipped: tag is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HyextrasPlugin.get().getTagService().addTag(uuid, tag);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[add_tag] failed for tag=" + tag);
        }
    }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
}
