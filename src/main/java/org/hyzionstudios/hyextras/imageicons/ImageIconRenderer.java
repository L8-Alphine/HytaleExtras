package org.hyzionstudios.hyextras.imageicons;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Isolated packet/nameplate renderer boundary for ImageIcons.
 *
 * <p>The current SDK packet surface gives HyExtras safe outbound packet hooks and
 * nameplate update types, but UUID-to-network-id rendering is still owned by the
 * packet layer. Keeping this boundary separate lets the real packet injector swap
 * in without changing the developer API or provider lifecycle.</p>
 */
final class ImageIconRenderer {

    private final HyExtrasPlugin plugin;
    private final Set<String> loggedUnavailable = ConcurrentHashMap.newKeySet();

    ImageIconRenderer(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    boolean packetBackendAvailable(String providerId, String action) {
        HyExtrasConfig config = plugin.getExtrasConfig();
        boolean available = config != null
                && plugin.isModuleEnabled(HyExtrasConfig.MODULE_PACKET_API)
                && config.advancedPacketActions;
        if (!available && loggedUnavailable.add(providerId + ":" + action)) {
            plugin.getLogger().at(Level.WARNING)
                    .log("[hextras image-icons] PacketAPI unavailable for provider " + providerId
                            + " while attempting " + action + ".");
        }
        return available;
    }

    void refresh(ImageIconAttachment attachment, ImageIconDefinition definition) {
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO)
                    .log("[hextras image-icons] refreshed runtime attachment "
                            + attachment.attachmentId() + " -> " + definition.qualifiedId());
        }
    }

    void clear(ImageIconAttachment attachment) {
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO)
                    .log("[hextras image-icons] cleared runtime attachment " + attachment.attachmentId());
        }
    }
}
