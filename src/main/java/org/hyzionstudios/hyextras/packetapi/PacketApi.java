package org.hyzionstudios.hyextras.packetapi;

import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.interface_.Notification;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.ShowEventTitle;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.packetapi.service.PacketVisibilityService;
import org.hyzionstudios.hyextras.packetapi.service.PlayerVisibilitySyncService;
import org.hyzionstudios.hyextras.packetapi.service.VisibilityPolicyService;
import org.hyzionstudios.hyextras.service.PlayerVariableService;
import org.hyzionstudios.hyextras.state.PlayerOverrideService;
import org.hyzionstudios.hyextras.util.RichText;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class PacketApi {

    private final HyExtrasPlugin plugin;
    private final VisibilityPolicyService visibilityPolicyService;
    private final PacketVisibilityService packetVisibilityService;
    private final PlayerVisibilitySyncService playerVisibilitySyncService;

    public PacketApi(HyExtrasPlugin plugin, PlayerOverrideService overrides, PlayerVariableService variables) {
        this.plugin = plugin;
        this.visibilityPolicyService = new VisibilityPolicyService(plugin, overrides, variables);
        this.packetVisibilityService = new PacketVisibilityService(plugin, visibilityPolicyService);
        this.playerVisibilitySyncService = new PlayerVisibilitySyncService(plugin, visibilityPolicyService);
    }

    public void applyConfig() {
        applyPacketVisibilityConfig();
        applyPlayerVisibilitySyncConfig();
    }

    public void stopServices() {
        playerVisibilitySyncService.stop();
        packetVisibilityService.stop();
    }

    public void syncNow() {
        playerVisibilitySyncService.syncNow();
    }

    public void clearPlayer(UUID player) {
        visibilityPolicyService.clearPlayer(player);
    }

    public boolean hidePlayer(UUID viewer, UUID target, boolean usePackets) {
        return visibilityPolicyService.hidePlayer(viewer, target, usePackets);
    }

    public boolean showPlayer(UUID viewer, UUID target, boolean usePackets) {
        return visibilityPolicyService.showPlayer(viewer, target, usePackets);
    }

    public boolean shouldHidePlayer(UUID viewer, UUID target) {
        return visibilityPolicyService.shouldHidePlayer(viewer, target);
    }

    public void hideEntity(UUID viewer, UUID entity) {
        visibilityPolicyService.hideEntity(viewer, entity);
    }

    public void showEntity(UUID viewer, UUID entity) {
        visibilityPolicyService.showEntity(viewer, entity);
    }

    public Set<UUID> snapshotHiddenPlayers(UUID viewer) {
        return plugin.getPlayerOverrideService().snapshotHidden(viewer);
    }

    public void clearHiddenPlayers(UUID viewer, boolean usePackets) {
        Set<UUID> snapshot = plugin.getPlayerOverrideService().snapshotHidden(viewer);
        plugin.getPlayerOverrideService().clearAll(viewer);
        plugin.getTargetingPreventionService().unprotectPlayer(viewer);
        if (!usePackets || !packetsEnabled()) {
            return;
        }
        for (UUID target : snapshot) {
            if (!visibilityPolicyService.shouldHidePlayer(viewer, target)) {
                sendPlayerVisibility(viewer, target, false);
            }
        }
    }

    public boolean sendTitle(
            UUID player,
            String title,
            @Nullable String subtitle,
            float durationSeconds,
            float fadeInSeconds,
            float fadeOutSeconds) {
        PlayerRef ref = plugin.getOnlinePlayerRef(player);
        if (ref == null || !packetsEnabled()) return false;
        FormattedMessage primary = RichText.toFormattedMessage(title);
        FormattedMessage secondary = subtitle != null && !subtitle.isBlank()
                ? RichText.toFormattedMessage(subtitle)
                : null;
        ref.getPacketHandler().write(new ShowEventTitle(
                fadeInSeconds, fadeOutSeconds, durationSeconds, "", false, primary, secondary));
        return true;
    }

    public boolean sendActionBar(UUID player, String message) {
        PlayerRef ref = plugin.getOnlinePlayerRef(player);
        if (ref == null || !packetsEnabled()) return false;
        Notification notification = new Notification();
        notification.message = RichText.toFormattedMessage(message);
        notification.style = NotificationStyle.Default;
        ref.getPacketHandler().write(notification);
        return true;
    }

    public boolean setCamera(UUID player, PacketCameraMode mode, boolean locked) {
        PlayerRef ref = plugin.getOnlinePlayerRef(player);
        if (ref == null || !packetsEnabled()) return false;
        PacketCameraMode cameraMode = mode != null ? mode : PacketCameraMode.FIRST_PERSON;
        SetServerCamera packet = new SetServerCamera();
        packet.clientCameraView = cameraMode == PacketCameraMode.THIRD_PERSON
                ? ClientCameraView.ThirdPerson
                : ClientCameraView.FirstPerson;
        packet.isLocked = locked && cameraMode != PacketCameraMode.RESET;
        ref.getPacketHandler().write(packet);
        return true;
    }

    public VisibilityPolicyService visibilityPolicyService() {
        return visibilityPolicyService;
    }

    public PacketVisibilityService packetVisibilityService() {
        return packetVisibilityService;
    }

    private void applyPacketVisibilityConfig() {
        HyExtrasConfig config = plugin.getExtrasConfig();
        if (config != null
                && plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                && config.advancedPacketActions
                && config.entityPacketFiltering) {
            packetVisibilityService.start();
        } else {
            packetVisibilityService.stop();
            if (config != null) {
                plugin.getLogger().at(Level.INFO).log("[hextras packets] Entity packet filtering disabled "
                        + "(advancedPacketActions=" + config.advancedPacketActions
                        + ", entityPacketFiltering=" + config.entityPacketFiltering + ").");
            }
        }
    }

    private void applyPlayerVisibilitySyncConfig() {
        HyExtrasConfig config = plugin.getExtrasConfig();
        if (config != null
                && plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                && config.advancedPacketActions
                && config.playerVisibilityPolicySync) {
            playerVisibilitySyncService.start();
        } else {
            playerVisibilitySyncService.stop();
            if (config != null) {
                plugin.getLogger().at(Level.INFO).log("[hextras visibility] Player visibility policy sync disabled "
                        + "(advancedPacketActions=" + config.advancedPacketActions
                        + ", playerVisibilityPolicySync=" + config.playerVisibilityPolicySync + ").");
            }
        }
    }

    private void sendPlayerVisibility(UUID viewer, UUID target, boolean hide) {
        if (!packetsEnabled()) {
            return;
        }
        PlayerRef viewerRef = plugin.getOnlinePlayerRef(viewer);
        if (viewerRef == null || viewerRef.getHiddenPlayersManager() == null) {
            return;
        }
        if (hide) {
            viewerRef.getHiddenPlayersManager().hidePlayer(target);
        } else {
            viewerRef.getHiddenPlayersManager().showPlayer(target);
        }
    }

    private boolean packetsEnabled() {
        HyExtrasConfig config = plugin.getExtrasConfig();
        return config != null
                && plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                && config.advancedPacketActions;
    }
}
