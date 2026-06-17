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
 * Applies a named HytaleExtras cooldown. Intended to be paired with
 * {@link org.hyzionstudios.hytaleextras.condition.CooldownReadyCondition}.
 *
 * <p>This is separate from the native volume-level cooldown.
 * Use it when you need fine-grained control over which effects are gated.</p>
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "apply_cooldown", "Name": "reward_gate", "Duration": 300.0 }
 * }</pre>
 */
public class ApplyCooldownAction extends TriggerEffect {

    public static final BuilderCodec<ApplyCooldownAction> CODEC = BuilderCodec.builder(
                    ApplyCooldownAction.class, ApplyCooldownAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Name"),         ApplyCooldownAction::setName,     ApplyCooldownAction::getName).add()
            .append(CodecHelper.floatField("Duration"), ApplyCooldownAction::setDuration, ApplyCooldownAction::getDuration).add()
            .build();

    private String name;
    private float duration;

    public ApplyCooldownAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (name == null || name.isBlank()) {
                HyextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[apply_cooldown] skipped: name is empty");
                return;
            }
            if (duration <= 0f) {
                HyextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[apply_cooldown] skipped: duration must be > 0");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HyextrasPlugin.get().getCooldownService().apply(uuid, name, duration);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[apply_cooldown] failed for name=" + name);
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public float getDuration() { return duration; }
    public void setDuration(float duration) { this.duration = duration; }
}
