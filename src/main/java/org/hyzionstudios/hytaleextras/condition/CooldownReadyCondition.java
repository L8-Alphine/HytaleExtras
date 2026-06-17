package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Condition that passes only if a named HytaleExtras cooldown is NOT currently active.
 * Intended to be paired with {@link org.hyzionstudios.hytaleextras.action.ApplyCooldownAction}.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "cooldown_ready", "Name": "reward_gate" }
 * }</pre>
 */
public class CooldownReadyCondition extends TriggerCondition {

    public static final BuilderCodec<CooldownReadyCondition> CODEC = BuilderCodec.builder(
                    CooldownReadyCondition.class, CooldownReadyCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("Name"), CooldownReadyCondition::setName, CooldownReadyCondition::getName).add()
            .build();

    private String name;

    public CooldownReadyCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            if (name == null || name.isBlank()) {
                HyextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[cooldown_ready] skipped: name is empty");
                return false;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return false;
            return HyextrasPlugin.get().getCooldownService().isReady(uuid, name);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[cooldown_ready] test failed for name=" + name);
            return false;
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
