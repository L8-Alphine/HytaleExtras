package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.RichText;
import org.hyzionstudios.hyextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;

public class SendRewardMessageAction extends TriggerEffect {

    public static final BuilderCodec<SendRewardMessageAction> CODEC = BuilderCodec.builder(
                    SendRewardMessageAction.class, SendRewardMessageAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Message"),
                    SendRewardMessageAction::setMessage,
                    SendRewardMessageAction::getMessage).add()
            .append(CodecHelper.optString("SecondaryMessage"),
                    SendRewardMessageAction::setSecondaryMessage,
                    SendRewardMessageAction::getSecondaryMessage).add()
            .append(CodecHelper.optEnum("Display", Display.class, Display.ALIASES),
                    SendRewardMessageAction::setDisplay,
                    SendRewardMessageAction::getDisplay).add()
            .append(CodecHelper.optFloat("Duration"),
                    SendRewardMessageAction::setDuration,
                    SendRewardMessageAction::getDuration).add()
            .build();

    private String message;
    @Nullable private String secondaryMessage;
    @Nullable private Display display;
    @Nullable private Float duration;

    public SendRewardMessageAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (message == null || message.isBlank()) {
                warn("message is empty");
                return;
            }
            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) {
                warn("triggering entity is not a player");
                return;
            }
            String resolved = StringTemplate.resolve(message, ctx, HyExtrasPlugin.get().getVariableService());
            String resolvedSecondary = secondaryMessage != null && !secondaryMessage.isBlank()
                    ? StringTemplate.resolve(secondaryMessage, ctx, HyExtrasPlugin.get().getVariableService())
                    : null;
            switch (display != null ? display : Display.CHAT) {
                case CHAT -> pr.sendMessage(RichText.toMessage(resolved));
                case ACTION_BAR -> HyExtrasPlugin.get().getPacketApi().sendActionBar(pr.getUuid(), resolved);
                case TITLE -> HyExtrasPlugin.get().getPacketApi().sendTitle(
                        pr.getUuid(),
                        resolved,
                        resolvedSecondary,
                        duration != null ? duration : 3.0f,
                        0.5f,
                        0.5f);
            }
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[send_reward_message] failed");
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                .log("[send_reward_message] skipped: " + reason);
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    @Nullable public String getSecondaryMessage() { return secondaryMessage; }
    public void setSecondaryMessage(@Nullable String secondaryMessage) { this.secondaryMessage = secondaryMessage; }
    @Nullable public Display getDisplay() { return display; }
    public void setDisplay(@Nullable Display display) { this.display = display; }
    @Nullable public Float getDuration() { return duration; }
    public void setDuration(@Nullable Float duration) { this.duration = duration; }

    public enum Display {
        CHAT,
        ACTION_BAR,
        TITLE;

        public static final Map<Display, String> ALIASES = Map.of(
                CHAT, "chat",
                ACTION_BAR, "action_bar",
                TITLE, "title"
        );
    }
}
