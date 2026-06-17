package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.util.RichText;
import org.hyzionstudios.hytaleextras.util.StringTemplate;

import java.util.logging.Level;

/**
 * Sends a raw chat message to the triggering player.
 *
 * <p>Supports {@link StringTemplate} placeholders and {@link RichText} color/style codes.
 */
public class SendRichMessageAction extends TriggerEffect {

    public static final BuilderCodec<SendRichMessageAction> CODEC = BuilderCodec.builder(
                    SendRichMessageAction.class, SendRichMessageAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Message"), SendRichMessageAction::setMessage, SendRichMessageAction::getMessage).add()
            .build();

    private String message = "";

    public SendRichMessageAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (message == null || message.isBlank()) {
                HytaleextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[send_rich_message] skipped: message is empty");
                return;
            }

            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) {
                return;
            }

            String resolved = StringTemplate.resolve(
                    message,
                    ctx,
                    HytaleextrasPlugin.get().getVariableService());
            pr.sendMessage(RichText.toMessage(resolved));
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[send_rich_message] failed");
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
