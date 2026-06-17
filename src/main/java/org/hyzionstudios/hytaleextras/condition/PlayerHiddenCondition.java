package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.state.PlayerOverrideService;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Passes if the triggering player (viewer) currently has another player hidden via
 * {@code player_hide_entity}. Checks server-side state in {@link PlayerOverrideService}.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "player_hidden", "TargetPlayer": "PlayerB" }
 * { "type": "player_hidden", "TargetPlayer": "PlayerB", "Invert": true }
 * }</pre>
 */
public class PlayerHiddenCondition extends TriggerCondition {

    public static final BuilderCodec<PlayerHiddenCondition> CODEC = BuilderCodec.builder(
                    PlayerHiddenCondition.class, PlayerHiddenCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("TargetPlayer"),
                    PlayerHiddenCondition::setTargetPlayer,
                    PlayerHiddenCondition::getTargetPlayer).add()
            .append(CodecHelper.optBool("Invert"),
                    PlayerHiddenCondition::setInvert,
                    PlayerHiddenCondition::isInvert).add()
            .build();

    private String targetPlayer;
    @Nullable private Boolean invert;

    public PlayerHiddenCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            UUID viewerUuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (viewerUuid == null) return resolveInvert(false);

            UUID targetUuid = TriggerVolumeApiAdapter.getPlayerUuidByName(ctx.getStore(), targetPlayer);
            if (targetUuid == null) return resolveInvert(false);

            PlayerOverrideService svc = HyextrasPlugin.get().getRuntimeState().playerOverrides();
            boolean hidden = svc.isEntityHidden(viewerUuid, targetUuid);
            return resolveInvert(hidden);
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[player_hidden] condition test failed");
            return false;
        }
    }

    private boolean resolveInvert(boolean value) {
        return (invert != null && invert) ? !value : value;
    }

    public String getTargetPlayer() { return targetPlayer; }
    public void setTargetPlayer(String v) { this.targetPlayer = v; }

    @Nullable public Boolean isInvert() { return invert; }
    public void setInvert(@Nullable Boolean v) { this.invert = v; }
}
