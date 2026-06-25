package org.hyzionstudios.hyextras.triggerextras.advanced;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.packetapi.PacketApi;
import org.hyzionstudios.hyextras.util.RuleEvaluationContext;
import org.hyzionstudios.hyextras.util.RuleEvaluator;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Hides {@code TargetPlayer} from the triggering player's view.
 *
 * <p>Server-side state is always updated. The client-side packet is only sent when
 * {@code UsePackets} is {@code true} (default) AND {@code advancedPacketActions} is enabled
 * in {@code hyextras.properties}.
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
            .append(CodecHelper.optString("TargetPlayer"),
                    PlayerHideEntityAction::setTargetPlayer,
                    PlayerHideEntityAction::getTargetPlayer).add()
            .append(CodecHelper.optEnum("TargetSelector", TargetSelector.class, TargetSelector.ALIASES),
                    PlayerHideEntityAction::setTargetSelector,
                    PlayerHideEntityAction::getTargetSelector).add()
            .append(CodecHelper.optString("ViewerRule"),
                    PlayerHideEntityAction::setViewerRule,
                    PlayerHideEntityAction::getViewerRule).add()
            .append(CodecHelper.optString("TargetRule"),
                    PlayerHideEntityAction::setTargetRule,
                    PlayerHideEntityAction::getTargetRule).add()
            .append(CodecHelper.optBool("UsePackets"),
                    PlayerHideEntityAction::setUsePackets,
                    PlayerHideEntityAction::getUsePackets).add()
            .append(CodecHelper.optBool("PreventTargeting"),
                    PlayerHideEntityAction::setPreventTargeting,
                    PlayerHideEntityAction::getPreventTargeting).add()
            .build();

    @Nullable private String targetPlayer;
    @Nullable private TargetSelector targetSelector;
    @Nullable private String viewerRule;
    @Nullable private String targetRule;
    @Nullable private Boolean usePackets;
    @Nullable private Boolean preventTargeting;

    public PlayerHideEntityAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            PlayerRef viewer = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (viewer == null) { warn("triggering entity is not a player"); return; }
            UUID viewerUuid = viewer.getUuid();

            RuleEvaluationContext viewerCtx = RuleEvaluationContext.fromTrigger(viewerUuid, viewer.getUsername(), ctx);
            if (!RuleEvaluator.matches(viewerRule, viewerCtx)) {
                return;
            }

            PacketApi packetApi = HyExtrasPlugin.get().getPacketApi();
            boolean packets = (usePackets == null || usePackets);
            TargetSelector selector = targetSelector != null ? targetSelector : TargetSelector.PLAYER;
            maybeProtectFromTargeting(viewerUuid);

            if (selector == TargetSelector.ENTITIES) {
                // ENTITIES carries no explicit per-entity target here; the meaningful effect is the
                // targeting protection applied above. For per-entity packet hiding use tagnpc_hide_entity
                // (TargetTag / EntityUuid). Logged at debug only so legitimate use is not log spam.
                if (HyExtrasPlugin.get().getExtrasConfig() != null
                        && HyExtrasPlugin.get().getExtrasConfig().debugMode) {
                    HyExtrasPlugin.get().getLogger().at(Level.INFO).log(
                            "[player_hide_entity] TargetSelector=entities applied targeting protection only; "
                                    + "use tagnpc_hide_entity for per-entity packet hiding");
                }
                return;
            }

            if (targetPlayer != null && !targetPlayer.isBlank()) {
                UUID targetUuid = TriggerVolumeApiAdapter.getPlayerUuidByName(ctx.getStore(), targetPlayer);
                if (targetUuid == null) { warn("target player not found: " + targetPlayer); return; }
                PlayerRef targetRef = HyExtrasPlugin.get().getOnlinePlayerRef(targetUuid);
                if (targetRef != null && targetMatches(targetRef)) {
                    packetApi.hidePlayer(viewerUuid, targetUuid, packets);
                }
                return;
            }

            if (selector == TargetSelector.PLAYERS) {
                for (PlayerRef targetRef : HyExtrasPlugin.get().getOnlinePlayers()) {
                    if (targetRef.getUuid().equals(viewerUuid)) continue;
                    if (targetMatches(targetRef)) {
                        packetApi.hidePlayer(viewerUuid, targetRef.getUuid(), packets);
                    }
                }
                return;
            }

            warn("targetPlayer is empty");
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[player_hide_entity] failed");
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[player_hide_entity] skipped: " + reason);
    }

    private boolean targetMatches(PlayerRef targetRef) {
        List<VolumeEntry> volumes = HyExtrasPlugin.get().getActiveVolumesForPlayer(targetRef.getUuid());
        RuleEvaluationContext targetCtx = RuleEvaluationContext.runtime(
                targetRef.getUuid(), targetRef.getUsername(), volumes);
        return RuleEvaluator.matches(targetRule, targetCtx);
    }

    private void maybeProtectFromTargeting(UUID viewerUuid) {
        if (Boolean.TRUE.equals(preventTargeting)) {
            HyExtrasPlugin.get().getTargetingPreventionService().protectPlayer(viewerUuid);
            if (HyExtrasPlugin.get().getExtrasConfig() != null
                    && HyExtrasPlugin.get().getExtrasConfig().debugMode) {
                HyExtrasPlugin.get().getLogger()
                        .at(Level.INFO)
                        .log("[player_hide_entity] PreventTargeting active for viewer=" + viewerUuid);
            }
        } else if (HyExtrasPlugin.get().getExtrasConfig() != null
                && HyExtrasPlugin.get().getExtrasConfig().debugMode) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.INFO)
                    .log("[player_hide_entity] PreventTargeting=false; target memory unchanged");
        }
    }

    @Nullable public String getTargetPlayer() { return targetPlayer; }
    public void setTargetPlayer(@Nullable String v) { this.targetPlayer = v; }

    @Nullable public TargetSelector getTargetSelector() { return targetSelector; }
    public void setTargetSelector(@Nullable TargetSelector v) { this.targetSelector = v; }

    @Nullable public String getViewerRule() { return viewerRule; }
    public void setViewerRule(@Nullable String v) { this.viewerRule = v; }

    @Nullable public String getTargetRule() { return targetRule; }
    public void setTargetRule(@Nullable String v) { this.targetRule = v; }

    @Nullable public Boolean getUsePackets() { return usePackets; }
    public void setUsePackets(@Nullable Boolean v) { this.usePackets = v; }

    @Nullable public Boolean getPreventTargeting() { return preventTargeting; }
    public void setPreventTargeting(@Nullable Boolean v) { this.preventTargeting = v; }
}
