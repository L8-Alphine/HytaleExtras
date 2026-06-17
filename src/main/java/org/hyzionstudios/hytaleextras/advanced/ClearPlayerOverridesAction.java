package org.hyzionstudios.hytaleextras.advanced;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.state.PlayerOverrideService;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Clears all per-player entity visibility overrides for the triggering player,
 * restoring the default view.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "clear_player_overrides" }
 * { "type": "clear_player_overrides", "UsePackets": false }
 * }</pre>
 */
public class ClearPlayerOverridesAction extends TriggerEffect {

    public static final BuilderCodec<ClearPlayerOverridesAction> CODEC = BuilderCodec.builder(
                    ClearPlayerOverridesAction.class, ClearPlayerOverridesAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optBool("UsePackets"),
                    ClearPlayerOverridesAction::setUsePackets,
                    ClearPlayerOverridesAction::getUsePackets).add()
            .build();

    @Nullable private Boolean usePackets;

    public ClearPlayerOverridesAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            UUID viewerUuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (viewerUuid == null) { warn("triggering entity is not a player"); return; }

            PlayerOverrideService svc = HytaleextrasPlugin.get().getRuntimeState().playerOverrides();
            Set<UUID> snapshot = svc.snapshotHidden(viewerUuid);
            svc.clearAll(viewerUuid);

            boolean doPackets = (usePackets == null || usePackets)
                    && HytaleextrasPlugin.get().getExtrasConfig().advancedPacketActions;
            if (doPackets && !snapshot.isEmpty()) {
                HiddenPlayersManager hpm = TriggerVolumeApiAdapter.getHiddenPlayersManager(
                        ctx.getStore(), ctx.getEntityRef());
                if (hpm != null) {
                    for (UUID targetUuid : snapshot) {
                        hpm.showPlayer(targetUuid);
                    }
                }
            }
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[clear_player_overrides] failed");
        }
    }

    private void warn(String reason) {
        HytaleextrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[clear_player_overrides] skipped: " + reason);
    }

    @Nullable public Boolean getUsePackets() { return usePackets; }
    public void setUsePackets(@Nullable Boolean v) { this.usePackets = v; }
}
