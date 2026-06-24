package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;

import java.util.UUID;
import java.util.logging.Level;

/**
 * Marks the current interaction for cancellation when inside the HyExtras
 * interaction bridge (volumes tagged with {@code hextras:interact}).
 *
 * <p>Place this effect with event type {@code TAG_ADDED} in a volume that has
 * the {@code hextras:interact} static tag. When a player interacts and conditions
 * pass, the bridge executes effects — if {@code cancel_interaction} fires,
 * the native {@link com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent}
 * is cancelled so the door/chest/entity does not respond.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "cancel_interaction", "eventType": "TAG_ADDED" }
 * }</pre>
 */
public class CancelInteractionAction extends TriggerEffect {

    public static final BuilderCodec<CancelInteractionAction> CODEC = BuilderCodec.builder(
                    CancelInteractionAction.class, CancelInteractionAction::new, TriggerEffect.BASE_CODEC)
            .build();

    public CancelInteractionAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (ctx.getEventType() != TriggerEventType.TAG_ADDED
                    || !"hextras_interact".equals(ctx.getTagKey())) {
                return;
            }

            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return;
            HyExtrasPlugin.get().getInteractionTriggerService().markCancelPending(uuid);
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[cancel_interaction] failed");
        }
    }
}
