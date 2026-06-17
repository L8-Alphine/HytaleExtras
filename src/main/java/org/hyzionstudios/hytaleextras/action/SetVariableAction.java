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
 * Sets a per-player variable to a fixed string value.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "set_variable", "Key": "zone", "Value": "spawn" }
 * }</pre>
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
        try {
            if (key == null || key.isBlank()) {
                warn("key is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HytaleextrasPlugin.get().getVariableService().set(uuid, key, value != null ? value : "");
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[set_variable] failed for key=" + key);
        }
    }

    private void warn(String reason) {
        HytaleextrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[set_variable] skipped: " + reason);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
