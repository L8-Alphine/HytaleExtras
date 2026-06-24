package org.hyzionstudios.hyextras.packetapi.service;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Periodically pushes volume tag visibility policy to player-to-player packets.
 *
 * <p>This intentionally avoids non-player EntityUpdates inspection. It only compares
 * online PlayerRefs and calls HiddenPlayersManager through VisibilityPolicyService.</p>
 */
public final class PlayerVisibilitySyncService {

    private final HyExtrasPlugin plugin;
    private final VisibilityPolicyService visibilityPolicy;
    private ScheduledExecutorService executor;

    public PlayerVisibilitySyncService(HyExtrasPlugin plugin, VisibilityPolicyService visibilityPolicy) {
        this.plugin = plugin;
        this.visibilityPolicy = visibilityPolicy;
    }

    public synchronized void start() {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "HyExtras-PlayerVisibilitySync");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleAtFixedRate(this::safeSync, 500L, 500L, TimeUnit.MILLISECONDS);
        plugin.getLogger().at(Level.INFO)
                .log("[hextras visibility] Player visibility policy sync started.");
    }

    public synchronized void stop() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        executor = null;
        plugin.getLogger().at(Level.INFO)
                .log("[hextras visibility] Player visibility policy sync stopped.");
    }

    public synchronized boolean isRunning() {
        return executor != null;
    }

    public void syncNow() {
        safeSync();
    }

    private void safeSync() {
        try {
            if (plugin.getExtrasConfig() == null
                    || !plugin.getExtrasConfig().advancedPacketActions
                    || !plugin.getExtrasConfig().playerVisibilityPolicySync) {
                return;
            }

            List<PlayerRef> players = new ArrayList<>(plugin.getOnlinePlayers());
            for (PlayerRef viewer : players) {
                UUID viewerUuid = viewer.getUuid();
                for (PlayerRef target : players) {
                    UUID targetUuid = target.getUuid();
                    if (viewerUuid.equals(targetUuid)) {
                        continue;
                    }
                    boolean hidden = visibilityPolicy.shouldHidePlayer(viewerUuid, targetUuid);
                    visibilityPolicy.syncPlayerPacketPolicy(viewerUuid, targetUuid, hidden);
                }
            }
        } catch (Exception e) {
            if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
                plugin.getLogger().at(Level.WARNING).withCause(e)
                        .log("[hextras visibility] Player visibility policy sync failed.");
            }
        }
    }
}
