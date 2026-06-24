package org.hyzionstudios.hyextras.packetapi.service;

import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.player.HiddenPlayersManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.service.PlayerVariableService;
import org.hyzionstudios.hyextras.state.PlayerOverrideService;
import org.hyzionstudios.hyextras.util.RuleEvaluationContext;
import org.hyzionstudios.hyextras.util.RuleEvaluator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Central visibility policy for player-to-player overrides and volume tag policy.
 */
public final class VisibilityPolicyService {

    public static final String DEFAULT_PARTY_VARIABLE = "partyId";

    private final HyExtrasPlugin plugin;
    private final PlayerOverrideService overrides;
    private final PlayerVariableService vars;
    private final Set<String> syncedHiddenPairs = ConcurrentHashMap.newKeySet();

    public VisibilityPolicyService(HyExtrasPlugin plugin, PlayerOverrideService overrides, PlayerVariableService vars) {
        this.plugin = plugin;
        this.overrides = overrides;
        this.vars = vars;
    }

    public boolean shouldHidePlayer(UUID viewer, UUID target) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return false;
        }
        List<VolumeEntry> viewerVolumes = plugin.getActiveVolumesForPlayer(viewer);
        List<VolumeEntry> targetVolumes = plugin.getActiveVolumesForPlayer(target);

        VolumeDecision volumeDecision = volumeDecision(viewer, target, viewerVolumes, targetVolumes);
        if (volumeDecision == VolumeDecision.FORCE_ALLOW) {
            return false;
        }
        if (volumeDecision == VolumeDecision.FORCE_HIDE) {
            return true;
        }
        return overrides.isEntityHidden(viewer, target);
    }

    public boolean shouldHideEntity(UUID viewer, Store<EntityStore> store, Ref<EntityStore> entityRef) {
        UUID entityUuid = getEntityUuid(store, entityRef);
        return entityUuid != null && overrides.isEntityHidden(viewer, entityUuid);
    }

    public boolean hidePlayer(UUID viewer, UUID target, boolean usePackets) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return false;
        }
        if (currentVolumeDecision(viewer, target) == VolumeDecision.FORCE_ALLOW) {
            return false;
        }
        overrides.hideEntity(viewer, target);
        if (usePackets) {
            sendPlayerVisibilityPacket(viewer, target, true);
        }
        return true;
    }

    public boolean showPlayer(UUID viewer, UUID target, boolean usePackets) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return false;
        }
        overrides.showEntity(viewer, target);
        if (usePackets && !shouldHidePlayer(viewer, target)) {
            sendPlayerVisibilityPacket(viewer, target, false);
        }
        return true;
    }

    public void hideEntity(UUID viewer, UUID entityUuid) {
        if (viewer != null && entityUuid != null && !viewer.equals(entityUuid)) {
            overrides.hideEntity(viewer, entityUuid);
        }
    }

    public void showEntity(UUID viewer, UUID entityUuid) {
        if (viewer != null && entityUuid != null) {
            overrides.showEntity(viewer, entityUuid);
        }
    }

    public void syncPlayerPacketPolicy(UUID viewer, UUID target, boolean hidden) {
        if (viewer == null || target == null || viewer.equals(target)) {
            return;
        }
        String key = pairKey(viewer, target);
        if (hidden) {
            if (syncedHiddenPairs.add(key)) {
                sendPlayerVisibilityPacket(viewer, target, true);
            }
        } else if (syncedHiddenPairs.remove(key)) {
            sendPlayerVisibilityPacket(viewer, target, false);
        }
    }

    public void clearPlayer(UUID player) {
        syncedHiddenPairs.removeIf(pair -> pair.startsWith(player + ">") || pair.endsWith(">" + player));
    }

    public boolean matchesRule(UUID player, @Nullable String username, @Nullable String rule, List<VolumeEntry> volumes) {
        return RuleEvaluator.matches(rule, RuleEvaluationContext.runtime(player, username, volumes));
    }

    private VolumeDecision currentVolumeDecision(UUID viewer, UUID target) {
        return volumeDecision(
                viewer,
                target,
                plugin.getActiveVolumesForPlayer(viewer),
                plugin.getActiveVolumesForPlayer(target));
    }

    private void sendPlayerVisibilityPacket(UUID viewer, UUID target, boolean hide) {
        if (plugin.getExtrasConfig() == null
                || !plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                || !plugin.getExtrasConfig().advancedPacketActions) {
            return;
        }
        PlayerRef viewerRef = plugin.getOnlinePlayerRef(viewer);
        if (viewerRef == null) {
            return;
        }
        try {
            HiddenPlayersManager hpm = viewerRef.getHiddenPlayersManager();
            if (hpm == null) {
                return;
            }
            if (hide) {
                hpm.hidePlayer(target);
            } else {
                hpm.showPlayer(target);
            }
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras visibility] failed to send player visibility packet");
        }
    }

    private VolumeDecision volumeDecision(
            UUID viewer,
            UUID target,
            List<VolumeEntry> viewerVolumes,
            List<VolumeEntry> targetVolumes) {
        List<VolumeEntry> shared = sharedVolumes(viewerVolumes, targetVolumes);

        if (hasTagValue(shared, "IsStoryArea", "false")) {
            return VolumeDecision.FORCE_ALLOW;
        }
        if (hasTagValue(shared, "IsStoryArea", "true")) {
            return VolumeDecision.FORCE_HIDE;
        }
        if (hasTagValue(shared, "GroupArea", "true")) {
            return groupDecision(viewer, target, shared);
        }
        return VolumeDecision.NEUTRAL;
    }

    private VolumeDecision groupDecision(UUID viewer, UUID target, List<VolumeEntry> shared) {
        String viewerParty = vars.getString(viewer, DEFAULT_PARTY_VARIABLE);
        String targetParty = vars.getString(target, DEFAULT_PARTY_VARIABLE);
        if (viewerParty == null || viewerParty.isBlank()
                || targetParty == null || targetParty.isBlank()
                || !viewerParty.equals(targetParty)) {
            return VolumeDecision.FORCE_HIDE;
        }

        int limit = partyLimit(shared);
        if (limit <= 0) {
            return VolumeDecision.FORCE_ALLOW;
        }

        List<UUID> members = new ArrayList<>();
        for (PlayerRef playerRef : plugin.getOnlinePlayers()) {
            UUID uuid = playerRef.getUuid();
            if (viewerParty.equals(vars.getString(uuid, DEFAULT_PARTY_VARIABLE))
                    && sharesAnyVolume(plugin.getActiveVolumesForPlayer(uuid), shared)) {
                members.add(uuid);
            }
        }
        members.sort(Comparator.comparing(UUID::toString));
        int viewerIndex = members.indexOf(viewer);
        int targetIndex = members.indexOf(target);
        if (viewerIndex >= 0 && targetIndex >= 0 && viewerIndex < limit && targetIndex < limit) {
            return VolumeDecision.FORCE_ALLOW;
        }
        return VolumeDecision.FORCE_HIDE;
    }

    private static boolean sharesAnyVolume(List<VolumeEntry> candidate, List<VolumeEntry> policyVolumes) {
        for (VolumeEntry a : candidate) {
            for (VolumeEntry b : policyVolumes) {
                if (a != null && b != null && a.getId().equals(b.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<VolumeEntry> sharedVolumes(List<VolumeEntry> a, List<VolumeEntry> b) {
        List<VolumeEntry> shared = new ArrayList<>();
        for (VolumeEntry left : a) {
            if (left == null) continue;
            for (VolumeEntry right : b) {
                if (right != null && left.getId().equals(right.getId())) {
                    shared.add(left);
                    break;
                }
            }
        }
        return shared;
    }

    private static boolean hasTagValue(List<VolumeEntry> volumes, String key, String expected) {
        for (VolumeEntry volume : volumes) {
            Map<String, String> tags = volume.getRawTags();
            if (tags != null && expected.equalsIgnoreCase(tags.get(key))) {
                return true;
            }
        }
        return false;
    }

    private static int partyLimit(List<VolumeEntry> volumes) {
        int limit = 0;
        for (VolumeEntry volume : volumes) {
            Map<String, String> tags = volume.getRawTags();
            if (tags == null) continue;
            String raw = tags.get("PartyAmount");
            if (raw == null || raw.isBlank()) continue;
            try {
                limit = Math.max(limit, Integer.parseInt(raw.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return limit;
    }

    private static String pairKey(UUID viewer, UUID target) {
        return viewer + ">" + target;
    }

    @Nullable
    public static UUID getEntityUuid(Store<EntityStore> store, Ref<EntityStore> ref) {
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef != null) {
            return playerRef.getUuid();
        }
        UUIDComponent component = store.getComponent(ref, UUIDComponent.getComponentType());
        return component != null ? component.getUuid() : null;
    }

    private enum VolumeDecision {
        FORCE_ALLOW,
        FORCE_HIDE,
        NEUTRAL
    }
}
