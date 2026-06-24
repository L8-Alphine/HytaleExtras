package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.StringTemplate;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Sets a per-player variable to a fixed string value.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "set_variable", "Key": "zone", "Value": "spawn" }</pre>
 */
public class SetVariableAction extends TriggerEffect {

    public static final BuilderCodec<SetVariableAction> CODEC = BuilderCodec.builder(
                    SetVariableAction.class, SetVariableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Key"),   SetVariableAction::setKey,   SetVariableAction::getKey).add()
            .append(CodecHelper.string("Value"), SetVariableAction::setValue, SetVariableAction::getValue).add()
            .build();

    private String key;
    private String value;

    public SetVariableAction() {}

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
            if (uuid == null) return;
            String resolved = value != null
                    ? StringTemplate.resolve(value, ctx, HyExtrasPlugin.get().getVariableService())
                    : "";
            HyExtrasPlugin.get().getVariableService().set(uuid, key, resolved);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[set_variable] failed for key=" + key);
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[set_variable] skipped: " + reason);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
