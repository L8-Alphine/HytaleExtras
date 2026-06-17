package org.hyzionstudios.hytaleextras;

import com.hypixel.hytale.builtin.triggervolumes.EntityTargetType;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.hyzionstudios.hytaleextras.command.ExtrasRootCommand;
import org.hyzionstudios.hytaleextras.config.ConfigLoader;
import org.hyzionstudios.hytaleextras.config.HytaleExtrasConfig;
import org.hyzionstudios.hytaleextras.service.CooldownService;
import org.hyzionstudios.hytaleextras.service.InteractionTriggerService;
import org.hyzionstudios.hytaleextras.service.PlayerTagService;
import org.hyzionstudios.hytaleextras.service.PlayerVariableService;
import org.hyzionstudios.hytaleextras.state.PlayerOverrideService;
import org.hyzionstudios.hytaleextras.state.RuntimeStateStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class HyextrasPlugin extends JavaPlugin {

    private static HyextrasPlugin instance;

    private PlayerVariableService variableService;
    private CooldownService cooldownService;
    private PlayerOverrideService playerOverrideService;
    private InteractionTriggerService interactionTriggerService;
    private PlayerTagService tagService;
    private HytaleExtrasConfig extrasConfig;
    private RuntimeStateStore runtimeState;

    /** Volume IDs that have interaction-blocking active (runtime, cleared on restart). */
    private final Set<String> interactionBlockedVolumeIds = ConcurrentHashMap.newKeySet();
    /** Volume IDs that override a blocked parent volume (e.g. sub-volumes inside a locked area). */
    private final Set<String> interactionAllowedVolumeIds = ConcurrentHashMap.newKeySet();

    /** username → UUID for all currently connected players. */
    private final ConcurrentHashMap<String, UUID> playerNameToUuid = new ConcurrentHashMap<>();

    public HyextrasPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static HyextrasPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;
        ExtrasRegistry.register(this);
        getEntityStoreRegistry().registerSystem(new UseBlockInteractionSystem(this));
    }

    @Override
    protected void start() {
        instance = this;
        ExtrasRegistry.register(this);

        extrasConfig = ConfigLoader.load(getDataDirectory());
        variableService = new PlayerVariableService();
        cooldownService = new CooldownService();
        playerOverrideService = new PlayerOverrideService();
        interactionTriggerService = new InteractionTriggerService();
        tagService = new PlayerTagService(getDataDirectory());
        runtimeState = new RuntimeStateStore(variableService, cooldownService, playerOverrideService, extrasConfig);
        I18nDefaultsLoader.load(this);

        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            PlayerRef pr = event.getPlayerRef();
            playerNameToUuid.put(normalizePlayerName(pr.getUsername()), pr.getUuid());
            tagService.loadPlayer(pr.getUuid());
        });

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef pr = event.getPlayerRef();
            UUID uuid = pr.getUuid();
            playerNameToUuid.remove(normalizePlayerName(pr.getUsername()));
            tagService.saveAndClearPlayer(uuid);
            variableService.clearPlayer(uuid);
            cooldownService.clearPlayer(uuid);
            playerOverrideService.clearPlayer(uuid);
            interactionTriggerService.clearCancelPending(uuid);
        });

        getEventRegistry().registerGlobal(PlayerInteractEvent.class, event -> {
            if (event.getTargetBlock() != null) return;

            // getPlayerRef() is inherited from PlayerEvent — returns Ref<EntityStore> for the player entity
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

            if (dispatchInteractionVolumes(store, playerEntityRef, playerUuid, event.getActionType(), null)) {
                event.setCancelled(true);
            }
        });

        getCommandRegistry().registerCommand(new ExtrasRootCommand(variableService, tagService, cooldownService, runtimeState));

        getLogger().at(Level.INFO).log("HytaleExtras started — 22 effects and 8 conditions registered.");
    }

    boolean handleUseBlockPre(Store<EntityStore> store, UseBlockEvent.Pre event) {
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

    private boolean dispatchInteractionVolumes(
            Store<EntityStore> store,
            Ref<EntityStore> playerEntityRef,
            UUID playerUuid,
            InteractionType interactionType,
            @Nullable Vector3d blockPos) {
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

        // Check volume interaction blocking.
        // A player in a blocked volume cannot interact unless they are also in an allowed override volume.
        boolean blocked = false;
        boolean overrideFound = false;
        for (VolumeEntry vol : activeVolumes) {
            String vid = vol.getId();
            if (interactionBlockedVolumeIds.contains(vid)) blocked = true;
            if (interactionAllowedVolumeIds.contains(vid)) overrideFound = true;
        }
        if (blocked && !overrideFound) {
            interactionTriggerService.clearCancelPending(playerUuid);
            logInteractionDebug("Interaction cancelled by blocked volume override state");
            return true;
        }

        String typeName = interactionType.name();

        interactionTriggerService.beginInteraction(playerUuid);
        try {
            for (VolumeEntry volume : activeVolumes) {
                Map<String, String> rawTags = volume.getRawTags();
                if (rawTags == null || !rawTags.containsKey("hextras:interact")) continue;
                logInteractionDebug("Dispatching interaction to volume=" + volume.getId());
                ExtraTriggerDispatcher.dispatch(
                        volume, playerEntityRef, store, TriggerEventType.TAG_ADDED,
                        activeVolumes, "hextras_interact", typeName, blockPos, null);
            }

            boolean cancel = interactionTriggerService.isCancelPending(playerUuid);
            logInteractionDebug("Interaction dispatch complete cancelPending=" + cancel);
            return cancel;
        } finally {
            interactionTriggerService.endInteraction(playerUuid);
        }
    }

    private void logInteractionDebug(String message) {
        if (extrasConfig != null && extrasConfig.debugMode) {
            getLogger().at(Level.INFO).log("[HytaleExtras interaction] " + message);
        }
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

    @Override
    protected void shutdown() {
        instance = null;
        playerNameToUuid.clear();
        getLogger().at(Level.INFO).log("HytaleExtras shut down.");
    }

    /** Re-reads {@code hytaleextras.properties} and propagates the new config to the runtime state. */
    public void reloadConfig() {
        extrasConfig = ConfigLoader.load(getDataDirectory());
        runtimeState.updateConfig(extrasConfig);
        getLogger().at(Level.INFO).log("HytaleExtras config reloaded.");
    }

    /** Returns the UUID of a connected player by username, or {@code null} if not online. */
    @Nullable
    public UUID getPlayerUuidByName(String username) {
        if (username == null) return null;
        return playerNameToUuid.get(normalizePlayerName(username));
    }

    private static String normalizePlayerName(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    public PlayerVariableService getVariableService() { return variableService; }
    public CooldownService getCooldownService() { return cooldownService; }
    public PlayerOverrideService getPlayerOverrideService() { return playerOverrideService; }
    public InteractionTriggerService getInteractionTriggerService() { return interactionTriggerService; }
    public PlayerTagService getTagService() { return tagService; }
    public HytaleExtrasConfig getExtrasConfig() { return extrasConfig; }
    public RuntimeStateStore getRuntimeState() { return runtimeState; }
    public Set<String> getInteractionBlockedVolumeIds() { return interactionBlockedVolumeIds; }
    public Set<String> getInteractionAllowedVolumeIds() { return interactionAllowedVolumeIds; }
}
