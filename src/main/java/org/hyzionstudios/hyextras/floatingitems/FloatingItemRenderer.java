package org.hyzionstudios.hyextras.floatingitems;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Isolated display backend for decorative floating items.
 *
 * <p>This boundary intentionally does not create collectible item entities. A packet/display
 * implementation can replace the debug no-op without changing API or TriggerExtras state.</p>
 */
final class FloatingItemRenderer {

    private final HyExtrasPlugin plugin;
    private final Set<String> loggedUnavailable = ConcurrentHashMap.newKeySet();

    FloatingItemRenderer(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    boolean refresh(FloatingItemInstance item, String action) {
        if (!backendAvailable(item.id(), action)) {
            return false;
        }
        if (item.store() == null) {
            logUnavailable(item.id(), action, "no live entity store is attached");
            return false;
        }
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO)
                    .log("[hextras floating-items] refreshed decorative item "
                            + item.id() + " item=" + item.itemId());
        }
        return true;
    }

    void clear(FloatingItemInstance item) {
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO)
                    .log("[hextras floating-items] cleared decorative item " + item.id());
        }
    }

    private boolean backendAvailable(String id, String action) {
        HyExtrasConfig config = plugin.getExtrasConfig();
        boolean available = config != null
                && plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                && config.advancedPacketActions;
        if (!available) {
            logUnavailable(id, action, "PacketAPI display backend is unavailable");
        }
        return available;
    }

    private void logUnavailable(String id, String action, String reason) {
        if (loggedUnavailable.add(id + ":" + action + ":" + reason)) {
            plugin.getLogger().at(Level.WARNING)
                    .log("[hextras floating-items] " + reason + " for " + id + " while attempting " + action + ".");
        }
    }
}
