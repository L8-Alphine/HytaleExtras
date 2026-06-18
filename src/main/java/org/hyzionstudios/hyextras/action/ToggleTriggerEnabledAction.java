package org.hyzionstudios.hyextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Enables, disables, or toggles another named trigger volume.
 *
 * <p>The native system has separate {@code EnableVolume} and {@code DisableVolume} effects;
 * this action adds the missing {@code "toggle"} mode and unifies all three behind one type ID.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "toggle_trigger_enabled", "VolumeId": "boss_arena",  "Mode": "toggle"  }
 * { "type": "toggle_trigger_enabled", "VolumeId": "boss_arena",  "Mode": "enable"  }
 * { "type": "toggle_trigger_enabled", "VolumeId": "boss_arena",  "Mode": "disable" }
 * }</pre>
 *
 */
public class ToggleTriggerEnabledAction extends TriggerEffect {

    public static final BuilderCodec<ToggleTriggerEnabledAction> CODEC = BuilderCodec.builder(
                    ToggleTriggerEnabledAction.class, ToggleTriggerEnabledAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("VolumeId"),
                    ToggleTriggerEnabledAction::setVolumeId,
                    ToggleTriggerEnabledAction::getVolumeId).add()
            .append(CodecHelper.optEnum("Mode", ToggleMode.class, ToggleMode.ALIASES),
                    ToggleTriggerEnabledAction::setMode,
                    ToggleTriggerEnabledAction::getMode).add()
            .build();

    @Nullable private String volumeId;
    @Nullable private ToggleMode mode; // TOGGLE by default

    public ToggleTriggerEnabledAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            String targetId = volumeId != null && !volumeId.isBlank()
                    ? volumeId
                    : ctx.getVolume().getId();
            TriggerVolumeManager mgr = TriggerVolumeApiAdapter.getManagerForStore(ctx.getStore());
            if (mgr == null) {
                warn("TriggerVolumeManager not available");
                return;
            }
            VolumeEntry target = mgr.getVolume(targetId);
            if (target == null) {
                warn("volume not found: " + targetId);
                return;
            }
            boolean newState = resolveState(target.isEnabled());
            target.setEnabled(newState);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[toggle_trigger_enabled] failed for volume: " + volumeId);
        }
    }

    private boolean resolveState(boolean current) {
        return switch (mode != null ? mode : ToggleMode.TOGGLE) {
            case ENABLE -> true;
            case DISABLE -> false;
            case TOGGLE -> !current;
        };
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[toggle_trigger_enabled] skipped: " + reason);
    }

    @Nullable public String getVolumeId() { return volumeId; }
    public void setVolumeId(@Nullable String volumeId) { this.volumeId = volumeId; }

    @Nullable public ToggleMode getMode() { return mode; }
    public void setMode(@Nullable ToggleMode mode) { this.mode = mode; }
}
