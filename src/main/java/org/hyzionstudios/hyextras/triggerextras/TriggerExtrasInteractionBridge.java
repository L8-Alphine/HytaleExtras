package org.hyzionstudios.hyextras.triggerextras;

import com.hypixel.hytale.builtin.triggervolumes.EntityTargetType;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.triggerextras.service.InteractionTriggerService;
import org.hyzionstudios.hyextras.util.StringTemplate;
import org.joml.Vector3d;
import org.joml.Vector3i;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class TriggerExtrasInteractionBridge {

    private static final long INTERACTION_REPLAY_WINDOW_NANOS = 50_000_000L;

    private final HyExtrasPlugin plugin;
    private final InteractionTriggerService interactionTriggerService = new InteractionTriggerService();
    private final InteractableVolumeState interactableVolumeState = new InteractableVolumeState();
    private final Set<String> interactionBlockedVolumeIds = ConcurrentHashMap.newKeySet();
    private final Set<String> interactionAllowedVolumeIds = ConcurrentHashMap.newKeySet();

    public TriggerExtrasInteractionBridge(HyExtrasPlugin plugin) {
        this.plugin = plugin;
    }

    public void handlePlayerInteract(PlayerInteractEvent event) {
        Ref<EntityStore> playerEntityRef = event.getPlayerRef();
        Store<EntityStore> store = playerEntityRef.getStore();
        if (store == null) return;

        PlayerRef pr = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (pr == null) return;
        UUID playerUuid = pr.getUuid();

        if (event.isCancelled()) {
            interactionTriggerService.clearCancelPending(playerUuid);
            return;
        }

        Vector3i targetBlock = event.getTargetBlock();
        Vector3d blockPos = targetBlock != null
                ? new Vector3d(targetBlock.x + 0.5D, targetBlock.y + 0.5D, targetBlock.z + 0.5D)
                : null;
        if (dispatchInteractionVolumes(store, playerEntityRef, playerUuid, event.getActionType(), blockPos)) {
            event.setCancelled(true);
        }
    }

    public boolean handleUseBlockPre(Store<EntityStore> store, UseBlockEvent.Pre event) {
        if (!plugin.isModuleEnabled(HyExtrasConfig.MODULE_TRIGGER_EXTRAS)) {
            return false;
        }
        Ref<EntityStore> playerEntityRef = event.getContext().getEntity();
        if (playerEntityRef == null) {
            playerEntityRef = event.getContext().getOwningEntity();
        }
        if (playerEntityRef == null) {
            logInteractionDebug("UseBlockEvent.Pre skipped: no player entity ref");
            return false;
        }

        PlayerRef pr = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
        if (pr == null) {
            logInteractionDebug("UseBlockEvent.Pre skipped: entity is not a player");
            return false;
        }

        Vector3i targetBlock = event.getTargetBlock();
        Vector3d blockPos = targetBlock != null
                ? new Vector3d(targetBlock.x + 0.5D, targetBlock.y + 0.5D, targetBlock.z + 0.5D)
                : null;
        boolean cancel = dispatchInteractionVolumes(store, playerEntityRef, pr.getUuid(), event.getInteractionType(), blockPos);
        logInteractionDebug("UseBlockEvent.Pre type=" + event.getInteractionType().name()
                + " target=" + targetBlock + " cancel=" + cancel);
        return cancel;
    }

    public List<VolumeEntry> getActiveVolumesForPlayer(UUID playerUuid) {
        PlayerRef playerRef = plugin.getOnlinePlayerRef(playerUuid);
        if (playerRef == null || playerRef.getReference() == null) {
            return List.of();
        }
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        Store<EntityStore> store = playerEntityRef.getStore();
        if (store == null) {
            return List.of();
        }
        TriggerVolumeManager mgr = TriggerVolumeApiAdapter.getManagerForStore(store);
        if (mgr == null) {
            return List.of();
        }
        return getPlayerVolumes(mgr, store, playerEntityRef, playerUuid);
    }

    public void clearPlayer(UUID uuid) {
        interactionTriggerService.clearCancelPending(uuid);
    }

    public InteractionTriggerService getInteractionTriggerService() {
        return interactionTriggerService;
    }

    public InteractableVolumeState getInteractableVolumeState() {
        return interactableVolumeState;
    }

    public Set<String> getInteractionBlockedVolumeIds() {
        return interactionBlockedVolumeIds;
    }

    public Set<String> getInteractionAllowedVolumeIds() {
        return interactionAllowedVolumeIds;
    }

    private boolean dispatchInteractionVolumes(
            Store<EntityStore> store,
            Ref<EntityStore> playerEntityRef,
            UUID playerUuid,
            InteractionType interactionType,
            @Nullable Vector3d blockPos) {
        if (!plugin.isModuleEnabled(HyExtrasConfig.MODULE_TRIGGER_EXTRAS)) {
            return false;
        }
        long now = System.nanoTime();
        String interactionKey = interactionKey(interactionType, blockPos);
        Boolean replay = interactionTriggerService.getRecentResult(
                playerUuid, interactionKey, now, INTERACTION_REPLAY_WINDOW_NANOS);
        if (replay != null) {
            logInteractionDebug("Interaction replay type=" + interactionType.name()
                    + " key=" + interactionKey + " cancel=" + replay);
            return replay;
        }

        TriggerVolumeManager mgr = TriggerVolumeApiAdapter.getManagerForStore(store);
        if (mgr == null) {
            interactionTriggerService.clearCancelPending(playerUuid);
            logInteractionDebug("Interaction skipped: no TriggerVolumeManager");
            return false;
        }

        if (mgr.isViewing(playerUuid)) {
            interactionTriggerService.clearCancelPending(playerUuid);
            logInteractionDebug("Interaction skipped: player is viewing trigger volumes");
            return false;
        }

        List<VolumeEntry> activeVolumes = getPlayerVolumes(mgr, store, playerEntityRef, playerUuid);
        logInteractionDebug("Interaction type=" + interactionType.name()
                + " activeVolumes=" + activeVolumes.size());

        boolean blocked = false;
        boolean overrideFound = false;
        for (VolumeEntry vol : activeVolumes) {
            String vid = vol.getId();
            if (interactionBlockedVolumeIds.contains(vid)) blocked = true;
            if (interactionAllowedVolumeIds.contains(vid)) overrideFound = true;
        }
        if (blocked && !overrideFound) {
            interactionTriggerService.clearCancelPending(playerUuid);
            interactionTriggerService.rememberResult(playerUuid, interactionKey, true, now);
            logInteractionDebug("Interaction cancelled by blocked volume override state");
            return true;
        }

        String typeName = interactionType.name();

        interactionTriggerService.beginInteraction(playerUuid);
        try {
            for (VolumeEntry volume : activeVolumes) {
                Map<String, String> rawTags = volume.getRawTags();
                InteractableVolumeState.Config interactableConfig = interactableVolumeState.resolve(volume);
                boolean taggedForDispatch = rawTags != null && rawTags.containsKey("hextras:interact");
                boolean interactableForDispatch = interactableVolumeState.matchesInteraction(
                        interactableConfig,
                        interactionType);
                if (!taggedForDispatch && (interactableConfig == null || !interactableForDispatch)) continue;
                if (interactableConfig != null && interactableForDispatch) {
                    sendInteractionPrompt(playerUuid, volume, interactableConfig);
                }
                logInteractionDebug("Dispatching interaction to volume=" + volume.getId());
                ExtraTriggerDispatcher.dispatch(
                        volume, playerEntityRef, store, TriggerEventType.TAG_ADDED,
                        activeVolumes, "hextras_interact", typeName, blockPos, null, false);
            }

            boolean cancel = interactionTriggerService.isCancelPending(playerUuid);
            interactionTriggerService.rememberResult(playerUuid, interactionKey, cancel, now);
            logInteractionDebug("Interaction dispatch complete cancelPending=" + cancel);
            return cancel;
        } finally {
            interactionTriggerService.endInteraction(playerUuid);
        }
    }

    private void sendInteractionPrompt(UUID playerUuid, VolumeEntry volume, InteractableVolumeState.Config config) {
        String message = resolveInteractionTemplate(config.message())
                .replace("{action}", config.action())
                .replace("{key}", config.key())
                .replace("{name}", config.name());
        try {
            PlayerRef playerRef = plugin.getOnlinePlayerRef(playerUuid);
            if (playerRef == null || playerRef.getReference() == null) {
                return;
            }
            String resolved = StringTemplate.resolve(
                    message,
                    new TriggerContext(
                            playerRef.getReference(),
                            playerRef.getReference().getStore(),
                            TriggerEventType.TAG_ADDED,
                            volume,
                            List.of(),
                            "hextras_interact_prompt",
                            config.action(),
                            null,
                            null),
                    plugin.getVariableService());
            plugin.getPacketApi().sendActionBar(playerUuid, resolved);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[HyExtras interaction] failed to send interaction prompt for volume=" + volume.getId());
        }
    }

    private static String resolveInteractionTemplate(String configuredMessage) {
        String key = configuredMessage.startsWith("server.")
                ? configuredMessage.substring("server.".length())
                : configuredMessage;
        if (!key.startsWith("interactionHints.")) {
            return configuredMessage;
        }

        I18nModule i18n = I18nModule.get();
        if (i18n != null) {
            String localized = i18n.getMessage("en-US", "server." + key);
            if (localized == null) {
                localized = i18n.getMessage("en-US", key);
            }
            if (localized != null && !localized.isBlank()) {
                return localized;
            }
        }
        return fallbackInteractionTemplate(key);
    }

    private static String fallbackInteractionTemplate(String key) {
        return switch (key) {
            case "interactionHints.open" -> "Press [{key}] to open {name}";
            case "interactionHints.openDoor" -> "Press [{key}] to open";
            case "interactionHints.closeDoor" -> "Press [{key}] to close";
            case "interactionHints.gather" -> "Press [{key}] to gather {name}";
            case "interactionHints.pick" -> "Press [{key}] to pick {name}";
            case "interactionHints.edit" -> "Press [{key}] to edit";
            case "interactionHints.turnon" -> "Press [{key}] to turn on";
            case "interactionHints.turnoff" -> "Press [{key}] to turn off";
            case "interactionHints.activate" -> "Press [{key}] to activate";
            case "interactionHints.deactivate" -> "Press [{key}] to deactivate";
            case "interactionHints.sit" -> "Press [{key}] to sit";
            case "interactionHints.tame" -> "Press [{key}] to tame";
            case "interactionHints.feed" -> "Press [{key}] to feed";
            case "interactionHints.harvest" -> "Press [{key}] to harvest {name}";
            case "interactionHints.pet" -> "Press [{key}] to pet";
            case "interactionHints.trade" -> "Press [{key}] to trade";
            case "interactionHints.mount" -> "Press [{key}] to mount";
            case "interactionHints.burst" -> "Press [{key}] to burst {name}";
            case "interactionHints.pickup" -> "Press [{key}] to pick up {name}";
            case "interactionHints.inspect" -> "Press [{key}] to inspect {name}";
            default -> "Press [{key}] to interact";
        };
    }

    private static String interactionKey(InteractionType interactionType, @Nullable Vector3d blockPos) {
        if (blockPos == null) {
            return interactionType.name() + "|none";
        }
        int x = (int) Math.floor(blockPos.x);
        int y = (int) Math.floor(blockPos.y);
        int z = (int) Math.floor(blockPos.z);
        return interactionType.name() + "|block:" + x + "," + y + "," + z;
    }

    private static List<VolumeEntry> getPlayerVolumes(
            TriggerVolumeManager mgr,
            Store<EntityStore> store,
            Ref<EntityStore> playerEntityRef,
            UUID playerUuid) {
        LinkedHashSet<VolumeEntry> result = new LinkedHashSet<>();

        TransformComponent transform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (transform != null) {
            Vector3d position = transform.getPosition();
            if (position != null) {
                ArrayList<VolumeEntry> candidates = new ArrayList<>();
                mgr.getSpatialIndex().rebuildIfDirty(mgr.getVolumes());
                mgr.getSpatialIndex().collectCandidates(position, candidates);
                for (VolumeEntry volume : candidates) {
                    if (containsPlayer(volume, position)) {
                        result.add(volume);
                    }
                }
            }
        }

        for (VolumeEntry volume : mgr.getVolumes()) {
            if (volume.isEnabled() && volume.getTrackedEntities().containsKey(playerUuid)) {
                result.add(volume);
            }
        }

        return List.copyOf(result);
    }

    private static boolean containsPlayer(VolumeEntry volume, Vector3d position) {
        return volume != null
                && volume.isEnabled()
                && volume.getTargetTypes().contains(EntityTargetType.PLAYER)
                && volume.getShape() != null
                && volume.getShape().contains(volume.getPosition(), position);
    }

    private void logInteractionDebug(String message) {
        if (plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.INFO).log("[HyExtras interaction] " + message);
        }
    }
}
