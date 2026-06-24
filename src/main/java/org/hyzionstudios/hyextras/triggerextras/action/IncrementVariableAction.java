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
 * Atomically increments a per-player numeric variable.
 * If the variable does not exist it is treated as {@code 0} before incrementing.
 * Use a negative {@code Delta} to decrement.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "increment_variable", "Key": "lap_count", "Delta": 1 }
 * }</pre>
 */
public class IncrementVariableAction extends TriggerEffect {

    public static final BuilderCodec<IncrementVariableAction> CODEC = BuilderCodec.builder(
                    IncrementVariableAction.class, IncrementVariableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Key"),     IncrementVariableAction::setKey,   IncrementVariableAction::getKey).add()
            .append(CodecHelper.optLong("Delta"),  IncrementVariableAction::setDelta, IncrementVariableAction::getDelta).add()
            .build();

    private String key;
    private Long delta;

    public IncrementVariableAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (key == null || key.isBlank()) {
                warn("key is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) { warn("entity is not a player"); return; }
            long amount = (delta != null) ? delta : 1L;
            long newVal = HyExtrasPlugin.get().getVariableService().increment(uuid, key, amount);
            HyExtrasPlugin.get().getLogger()
                    .at(Level.FINE)
                    .log("[increment_variable] " + key + " → " + newVal + " (delta=" + amount + ")");
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[increment_variable] failed for key=" + key);
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[increment_variable] skipped: " + reason);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public Long getDelta() { return delta; }
    public void setDelta(Long delta) { this.delta = delta; }
}
