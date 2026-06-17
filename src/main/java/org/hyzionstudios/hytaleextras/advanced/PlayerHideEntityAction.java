package org.hyzionstudios.hytaleextras.advanced;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.state.PlayerOverrideService;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Hides {@code TargetPlayer} from the triggering player's view.
 *
 * <p>Server-side state is always updated. The client-side packet is only sent when
 * {@code UsePackets} is {@code true} (default) AND {@code advancedPacketActions} is enabled
 * in {@code hytaleextras.properties}.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "player_hide_entity", "TargetPlayer": "PlayerB" }
 * { "type": "player_hide_entity", "TargetPlayer": "PlayerB", "UsePackets": false }
 * }</pre>
 */
public class PlayerHideEntityAction extends TriggerEffect {

    public static final BuilderCodec<PlayerHideEntityAction> CODEC = BuilderCodec.builder(
                    PlayerHideEntityAction.class, PlayerHideEntityAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("TargetPlayer"),
                    PlayerHideEntityAction::setTargetPlayer,
                    PlayerHideEntityAction::getTargetPlayer).add()
            .append(CodecHelper.optBool("UsePackets"),
                    PlayerHideEntityAction::setUsePackets,
                    PlayerHideEntityAction::getUsePackets).add()
            .build();

    private String targetPlayer;
    @Nullable private Boolean usePackets;

    public PlayerHideEntityAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            if (targetPlayer == null || targetPlayer.isBlank()) {
                warn("targetPlayer is empty");
                return;
            }
            UUID viewerUuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (viewerUuid == null) { warn("triggering entity is not a player"); return; }

            UUID targetUuid = TriggerVolumeApiAdapter.getPlayerUuidByName(ctx.getStore(), targetPlayer);
            if (targetUuid == null) { warn("target player not found: " + targetPlayer); return; }

            PlayerOverrideService svc = HyextrasPlugin.get().getRuntimeState().playerOverrides();
            svc.hideEntity(viewerUuid, targetUuid);

            boolean doPackets = (usePackets == null || usePackets)
                    && HyextrasPlugin.get().getExtrasConfig().advancedPacketActions;
            if (doPackets) {
                HiddenPlayersManager hpm = TriggerVolumeApiAdapter.getHiddenPlayersManager(
                        ctx.getStore(), ctx.getEntityRef());
                if (hpm != null) hpm.hidePlayer(targetUuid);
            }
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[player_hide_entity] failed");
        }
    }

    private void warn(String reason) {
        HyextrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[player_hide_entity] skipped: " + reason);
    }

    public String getTargetPlayer() { return targetPlayer; }
    public void setTargetPlayer(String v) { this.targetPlayer = v; }

    @Nullable public Boolean getUsePackets() { return usePackets; }
    public void setUsePackets(@Nullable Boolean v) { this.usePackets = v; }
}
