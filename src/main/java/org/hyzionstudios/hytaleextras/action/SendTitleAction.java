package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.interface_.Notification;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.ShowEventTitle;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.util.RichText;
import org.hyzionstudios.hytaleextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Sends a title/subtitle/notification message to the triggering player.
 *
 * <ul>
 *   <li>{@code Title} / {@code Subtitle} — sent via {@link ShowEventTitle} packet</li>
 *   <li>{@code ActionBar} — sent via {@link Notification} packet with {@link NotificationStyle#Default}</li>
 * </ul>
 *
 * <p>Supports {@link StringTemplate} placeholders and {@link RichText} color/style codes.
 *
 * <p>JSON config:
 * <pre>{@code
 * {
 *   "type": "send_title",
 *   "Title": "Welcome, {player}!",
 *   "Subtitle": "Entering the ruins",
 *   "Duration": 3.0
 * }
 * }</pre>
 */
public class SendTitleAction extends TriggerEffect {

    public static final BuilderCodec<SendTitleAction> CODEC = BuilderCodec.builder(
                    SendTitleAction.class, SendTitleAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optString("Title"),     SendTitleAction::setTitle,    SendTitleAction::getTitle).add()
            .append(CodecHelper.optString("Subtitle"),  SendTitleAction::setSubtitle, SendTitleAction::getSubtitle).add()
            .append(CodecHelper.optString("ActionBar"), SendTitleAction::setActionBar, SendTitleAction::getActionBar).add()
            .append(CodecHelper.optFloat("Duration"),   SendTitleAction::setDuration, SendTitleAction::getDuration).add()
            .append(CodecHelper.optFloat("FadeIn"),     SendTitleAction::setFadeIn,   SendTitleAction::getFadeIn).add()
            .append(CodecHelper.optFloat("FadeOut"),    SendTitleAction::setFadeOut,  SendTitleAction::getFadeOut).add()
            .build();

    @Nullable private String title;
    @Nullable private String subtitle;
    @Nullable private String actionBar;
    @Nullable private Float duration;
    @Nullable private Float fadeIn;
    @Nullable private Float fadeOut;

    public SendTitleAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            boolean hasTitle = title != null && !title.isBlank();
            boolean hasBar   = actionBar != null && !actionBar.isBlank();
            if (!hasTitle && !hasBar) {
                HyextrasPlugin.get().getLogger()
                        .at(Level.WARNING).log("[send_title] skipped: both title and actionBar are empty");
                return;
            }

            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) return;

            var vars = HyextrasPlugin.get().getVariableService();

            if (hasTitle) {
                String resolvedTitle = StringTemplate.resolve(title, ctx, vars);
                FormattedMessage primaryFm = RichText.toFormattedMessage(resolvedTitle);
                FormattedMessage secondaryFm = null;
                if (subtitle != null && !subtitle.isBlank()) {
                    secondaryFm = RichText.toFormattedMessage(StringTemplate.resolve(subtitle, ctx, vars));
                }
                ShowEventTitle packet = new ShowEventTitle(
                        fadeIn   != null ? fadeIn   : 0.5f,
                        fadeOut  != null ? fadeOut  : 0.5f,
                        duration != null ? duration : 3.0f,
                        "", false, primaryFm, secondaryFm);
                pr.getPacketHandler().write(packet);
            }

            if (hasBar) {
                String resolvedBar = StringTemplate.resolve(actionBar, ctx, vars);
                Notification notification = new Notification();
                notification.message = RichText.toFormattedMessage(resolvedBar);
                notification.style = NotificationStyle.Default;
                pr.getPacketHandler().write(notification);
            }
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[send_title] failed");
        }
    }

    @Nullable public String getTitle() { return title; }
    public void setTitle(@Nullable String title) { this.title = title; }

    @Nullable public String getSubtitle() { return subtitle; }
    public void setSubtitle(@Nullable String subtitle) { this.subtitle = subtitle; }

    @Nullable public String getActionBar() { return actionBar; }
    public void setActionBar(@Nullable String actionBar) { this.actionBar = actionBar; }

    @Nullable public Float getDuration() { return duration; }
    public void setDuration(@Nullable Float duration) { this.duration = duration; }

    @Nullable public Float getFadeIn() { return fadeIn; }
    public void setFadeIn(@Nullable Float fadeIn) { this.fadeIn = fadeIn; }

    @Nullable public Float getFadeOut() { return fadeOut; }
    public void setFadeOut(@Nullable Float fadeOut) { this.fadeOut = fadeOut; }
}
