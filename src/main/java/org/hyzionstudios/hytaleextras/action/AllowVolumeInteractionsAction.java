package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.logging.Level;

/**
 * Marks a volume as an interaction-override zone within a blocked area.
 * If a player is tracked in both a blocked volume AND an allowed override volume,
 * the override wins and interactions proceed normally.
 *
 * <p>Typical use: large "locked dungeon" volume blocked via {@code block_volume_interactions};
 * small "key hole" sub-volumes marked allowed so certain chests/doors remain interactive.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "allow_volume_interactions" }
 * { "type": "allow_volume_interactions", "VolumeId": "keyhole_a", "Mode": "toggle" }
 * }</pre>
 */
public class AllowVolumeInteractionsAction extends TriggerEffect {

    public static final BuilderCodec<AllowVolumeInteractionsAction> CODEC = BuilderCodec.builder(
                    AllowVolumeInteractionsAction.class, AllowVolumeInteractionsAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("VolumeId"),
                    AllowVolumeInteractionsAction::setVolumeId,
                    AllowVolumeInteractionsAction::getVolumeId).add()
            .append(CodecHelper.optEnum("Mode", ToggleMode.class, ToggleMode.ALIASES),
                    AllowVolumeInteractionsAction::setMode,
                    AllowVolumeInteractionsAction::getMode).add()
            .build();

    @Nullable private String volumeId;
    @Nullable private ToggleMode mode; // ENABLE by default

    public AllowVolumeInteractionsAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            String vid = volumeId != null && !volumeId.isBlank()
                    ? volumeId
                    : ctx.getVolume().getId();
            Set<String> allowedSet = HyextrasPlugin.get().getInteractionAllowedVolumeIds();
            applyMode(allowedSet, vid);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[allow_volume_interactions] failed for volume=" + volumeId);
        }
    }

    private void applyMode(Set<String> set, String vid) {
        switch (mode != null ? mode : ToggleMode.ENABLE) {
            case DISABLE -> set.remove(vid);
            case TOGGLE -> { if (!set.remove(vid)) set.add(vid); }
            case ENABLE -> set.add(vid);
        }
    }

    @Nullable public String getVolumeId() { return volumeId; }
    public void setVolumeId(@Nullable String v) { this.volumeId = v; }
    @Nullable public ToggleMode getMode() { return mode; }
    public void setMode(@Nullable ToggleMode m) { this.mode = m; }
}
