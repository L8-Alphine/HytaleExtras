package org.hyzionstudios.hyextras.packetapi.service;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.EntityUpdate;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.entities.EntityUpdates;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Best-effort outbound entity update filtering for HyExtras visibility policy.
 */
public final class PacketVisibilityService {

    private final HyExtrasPlugin plugin;
    private final VisibilityPolicyService visibilityPolicy;
    private PacketFilter outboundFilter;

    public PacketVisibilityService(HyExtrasPlugin plugin, VisibilityPolicyService visibilityPolicy) {
        this.plugin = plugin;
        this.visibilityPolicy = visibilityPolicy;
    }

    public synchronized void start() {
        if (outboundFilter != null) {
            return;
        }
        outboundFilter = PacketAdapters.registerOutbound(this::filterOutbound);
        plugin.getLogger().at(Level.INFO)
                .log("[hextras packets] Experimental entity packet filtering registered.");
    }

    public synchronized void stop() {
        if (outboundFilter != null) {
            PacketAdapters.deregisterOutbound(outboundFilter);
            outboundFilter = null;
            plugin.getLogger().at(Level.INFO)
                    .log("[hextras packets] Experimental entity packet filtering deregistered.");
        }
    }

    public synchronized boolean isRunning() {
        return outboundFilter != null;
    }

    private boolean filterOutbound(PlayerRef viewer, Packet packet) {
        if (viewer == null || packet == null) {
            return true;
        }
        if (plugin.getExtrasConfig() == null
                || !plugin.getExtrasConfig().advancedPacketActions
                || !plugin.getExtrasConfig().entityPacketFiltering) {
            return true;
        }
        if (!(packet instanceof EntityUpdates entityUpdates)) {
            return true;
        }

        try {
            EntityUpdate[] updates = entityUpdates.updates;
            if (updates == null || updates.length == 0) {
                return true;
            }

            Ref<EntityStore> viewerRef = viewer.getReference();
            if (viewerRef == null || viewerRef.getStore() == null) {
                return true;
            }
            Store<EntityStore> store = viewerRef.getStore();
            UUID viewerUuid = viewer.getUuid();

            List<EntityUpdate> kept = new ArrayList<>(updates.length);
            for (EntityUpdate update : updates) {
                if (update == null || !shouldSuppress(viewerUuid, store, update.networkId)) {
                    kept.add(update);
                }
            }

            if (kept.size() == updates.length) {
                return true;
            }
            entityUpdates.updates = kept.toArray(EntityUpdate[]::new);
            return entityUpdates.updates.length > 0
                    || (entityUpdates.removed != null && entityUpdates.removed.length > 0);
        } catch (Exception e) {
            if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
                plugin.getLogger().at(Level.WARNING).withCause(e)
                        .log("[hextras packets] visibility filter failed; allowing packet");
            }
            return true;
        }
    }

    private boolean shouldSuppress(UUID viewerUuid, Store<EntityStore> store, int networkId) {
        EntityStore entityStore = store.getExternalData();
        if (entityStore == null) {
            return false;
        }
        Ref<EntityStore> targetRef = entityStore.getRefFromNetworkId(networkId);
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }

        PlayerRef targetPlayer = store.getComponent(targetRef, PlayerRef.getComponentType());
        if (targetPlayer != null) {
            boolean hidden = visibilityPolicy.shouldHidePlayer(viewerUuid, targetPlayer.getUuid());
            visibilityPolicy.syncPlayerPacketPolicy(viewerUuid, targetPlayer.getUuid(), hidden);
            return hidden;
        }
        return visibilityPolicy.shouldHideEntity(viewerUuid, store, targetRef);
    }
}
