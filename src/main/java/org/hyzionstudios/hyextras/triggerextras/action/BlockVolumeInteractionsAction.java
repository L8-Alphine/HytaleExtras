package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.logging.Level;

/**
 * Enables, disables, or toggles interaction-blocking on a volume.
 * While a volume is blocked, players inside it cannot interact with anything
 * unless they are also inside an {@code allow_volume_interactions} override volume.
 *
 * <p>The block is tracked at server runtime in a plain Set; it clears on server restart.
 * Use {@code allow_volume_interactions} in smaller sub-volumes for area overrides.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "block_volume_interactions" }
 * { "type": "block_volume_interactions", "VolumeId": "dungeon_main", "Mode": "enable" }
 * { "type": "block_volume_interactions", "VolumeId": "door_zone", "Mode": "toggle" }
 * }</pre>
 */
public class BlockVolumeInteractionsAction extends TriggerEffect {

    public static final BuilderCodec<BlockVolumeInteractionsAction> CODEC = BuilderCodec.builder(
                    BlockVolumeInteractionsAction.class, BlockVolumeInteractionsAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("VolumeId"),
                    BlockVolumeInteractionsAction::setVolumeId,
                    BlockVolumeInteractionsAction::getVolumeId).add()
            .append(CodecHelper.optEnum("Mode", ToggleMode.class, ToggleMode.ALIASES),
                    BlockVolumeInteractionsAction::setMode,
                    BlockVolumeInteractionsAction::getMode).add()
            .build();

    @Nullable private String volumeId;
    @Nullable private ToggleMode mode; // ENABLE by default

    public BlockVolumeInteractionsAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            String vid = volumeId != null && !volumeId.isBlank()
                    ? volumeId
                    : ctx.getVolume().getId();
            Set<String> blockedSet = HyExtrasPlugin.get().getInteractionBlockedVolumeIds();
            applyMode(blockedSet, vid);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[block_volume_interactions] failed for volume=" + volumeId);
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
