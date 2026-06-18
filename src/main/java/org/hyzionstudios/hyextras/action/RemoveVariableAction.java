package org.hyzionstudios.hyextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Removes a per-player variable. Has no effect if the variable does not exist.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "remove_variable", "Key": "zone" }
 * }</pre>
 */
public class RemoveVariableAction extends TriggerEffect {

    public static final BuilderCodec<RemoveVariableAction> CODEC = BuilderCodec.builder(
                    RemoveVariableAction.class, RemoveVariableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Key"), RemoveVariableAction::setKey, RemoveVariableAction::getKey).add()
            .build();

    private String key;

    public RemoveVariableAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (key == null || key.isBlank()) {
                HyExtrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[remove_variable] skipped: key is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HyExtrasPlugin.get().getVariableService().remove(uuid, key);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[remove_variable] failed for key=" + key);
        }
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
