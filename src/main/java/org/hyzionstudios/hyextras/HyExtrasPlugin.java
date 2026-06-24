package org.hyzionstudios.hyextras;

import com.hypixel.hytale.builtin.triggervolumes.TriggerVolumesPlugin;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.command.ExtrasRootCommand;
import org.hyzionstudios.hyextras.config.ConfigLoader;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemService;
import org.hyzionstudios.hyextras.imageicons.ImageIconService;
import org.hyzionstudios.hyextras.module.FloatingItemsModule;
import org.hyzionstudios.hyextras.module.ImageIconsModule;
import org.hyzionstudios.hyextras.module.InternalModuleManager;
import org.hyzionstudios.hyextras.module.PacketApiModule;
import org.hyzionstudios.hyextras.module.PlaceholderApiModule;
import org.hyzionstudios.hyextras.module.TagNpcModule;
import org.hyzionstudios.hyextras.module.TriggerExtrasModule;
import org.hyzionstudios.hyextras.packetapi.PacketApi;
import org.hyzionstudios.hyextras.service.CooldownService;
import org.hyzionstudios.hyextras.service.PlayerTagService;
import org.hyzionstudios.hyextras.service.PlayerVariableService;
import org.hyzionstudios.hyextras.service.TargetingPreventionService;
import org.hyzionstudios.hyextras.state.PlayerOverrideService;
import org.hyzionstudios.hyextras.state.RuntimeStateStore;
import org.hyzionstudios.hyextras.tagnpc.TagNpcService;
import org.hyzionstudios.hyextras.triggerextras.TriggerActionRegistry;
import org.hyzionstudios.hyextras.triggerextras.TriggerConditionRegistry;
import org.hyzionstudios.hyextras.triggerextras.TriggerExtrasInteractionBridge;
import org.hyzionstudios.hyextras.triggerextras.UseBlockInteractionSystem;
import org.hyzionstudios.hyextras.triggerextras.service.InteractionTriggerService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class HyExtrasPlugin extends JavaPlugin {

    private static HyExtrasPlugin instance;

    private PlayerVariableService variableService;
    private CooldownService cooldownService;
    private PlayerOverrideService playerOverrideService;
    private PlayerTagService tagService;
    private TargetingPreventionService targetingPreventionService;
    private PacketApi packetApi;
    private HyExtrasConfig extrasConfig;
    private RuntimeStateStore runtimeState;
    private InternalModuleManager moduleManager;
    private TriggerExtrasInteractionBridge triggerExtrasInteractionBridge;
    private ImageIconService imageIconService;
    private TagNpcService tagNpcService;
    private FloatingItemService floatingItemService;

    /** username → UUID for all currently connected players. */
    private final ConcurrentHashMap<String, UUID> playerNameToUuid = new ConcurrentHashMap<>();
    /** UUID → PlayerRef for all currently connected players. */
    private final ConcurrentHashMap<UUID, PlayerRef> onlinePlayers = new ConcurrentHashMap<>();

    public HyExtrasPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static HyExtrasPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        instance = this;
        ensureModuleManagerRegistered();
        ensureTriggerExtrasInteractionBridge();
        getEntityStoreRegistry().registerSystem(new UseBlockInteractionSystem(this));
        getEntityStoreRegistry().registerSystem(new TargetingPreventionSystem(this));
        getEntityStoreRegistry().registerSystem(new TagNpcEntityIndexSystem(this));
    }

    @Override
    protected void start() {
        instance = this;
        ensureModuleManagerRegistered();
        ensureTriggerExtrasInteractionBridge();

        extrasConfig = ConfigLoader.load(getDataDirectory());
        variableService = new PlayerVariableService();
        cooldownService = new CooldownService();
        playerOverrideService = new PlayerOverrideService();
        tagService = new PlayerTagService(getDataDirectory());
        packetApi = new PacketApi(this, playerOverrideService, variableService);
        targetingPreventionService = new TargetingPreventionService(this);
        imageIconService = new ImageIconService(this);
        tagNpcService = new TagNpcService(this);
        floatingItemService = new FloatingItemService(this);
        runtimeState = new RuntimeStateStore(variableService, cooldownService, playerOverrideService, extrasConfig);
        moduleManager.initializeFromConfig(extrasConfig);
        runStartupDiagnostics("start-preflight");
        I18nDefaultsLoader.load(this);
        applyPacketFeatureConfig();

        getEventRegistry().register(PlayerConnectEvent.class, event -> {
            PlayerRef pr = event.getPlayerRef();
            playerNameToUuid.put(normalizePlayerName(pr.getUsername()), pr.getUuid());
            onlinePlayers.put(pr.getUuid(), pr);
            tagService.loadPlayer(pr.getUuid());
            logDebug("player connected name=" + pr.getUsername() + " uuid=" + pr.getUuid()
                    + " tags=" + tagService.snapshotTags(pr.getUuid()).size());
            packetApi.syncNow();
        });

        getEventRegistry().register(PlayerDisconnectEvent.class, event -> {
            PlayerRef pr = event.getPlayerRef();
            UUID uuid = pr.getUuid();
            playerNameToUuid.remove(normalizePlayerName(pr.getUsername()));
            onlinePlayers.remove(uuid);
            tagService.saveAndClearPlayer(uuid);
            variableService.clearPlayer(uuid);
            cooldownService.clearPlayer(uuid);
            playerOverrideService.clearPlayer(uuid);
            packetApi.clearPlayer(uuid);
            imageIconService.clearPlayer(uuid);
            tagNpcService.clearEntity(uuid);
            targetingPreventionService.unprotectPlayer(uuid);
            triggerExtrasInteractionBridge.clearPlayer(uuid);
            logDebug("player disconnected name=" + pr.getUsername() + " uuid=" + uuid + " state cleared");
            packetApi.syncNow();
        });

        getEventRegistry().registerGlobal(PlayerInteractEvent.class, triggerExtrasInteractionBridge::handlePlayerInteract);

        getCommandRegistry().registerCommand(new ExtrasRootCommand(
                variableService,
                tagService,
                cooldownService,
                runtimeState,
                moduleManager,
                tagNpcService,
                floatingItemService));

        getLogger().at(Level.INFO).log("HyExtras started — "
                + TriggerActionRegistry.TYPE_IDS.size() + " effects and "
                + TriggerConditionRegistry.TYPE_IDS.size() + " conditions registered.");
    }

    private void logDebug(String message) {
        if (extrasConfig != null && extrasConfig.debugMode) {
            getLogger().at(Level.INFO).log("[HyExtras debug] " + message);
        }
    }

    private void ensureModuleManagerRegistered() {
        if (moduleManager != null) {
            return;
        }
        moduleManager = new InternalModuleManager(this);
        registerBuiltInModules();
    }

    private void ensureTriggerExtrasInteractionBridge() {
        if (triggerExtrasInteractionBridge == null) {
            triggerExtrasInteractionBridge = new TriggerExtrasInteractionBridge(this);
        }
    }

    private void registerBuiltInModules() {
        moduleManager.register(new TriggerExtrasModule());
        moduleManager.register(new PlaceholderApiModule());
        moduleManager.register(new PacketApiModule());
        moduleManager.register(new ImageIconsModule());
        moduleManager.register(new TagNpcModule());
        moduleManager.register(new FloatingItemsModule());
    }

    private void runStartupDiagnostics(String phase) {
        if (extrasConfig == null || !extrasConfig.startupDiagnostics) {
            return;
        }

        getLogger().at(Level.INFO).log("[hextras preflight] phase=" + phase
                + " plugin=" + getIdentifier()
                + " version=" + getManifest().getVersion()
                + " serverRange=" + getManifest().getServerVersion());
        getLogger().at(Level.INFO).log("[hextras preflight] config advancedPacketActions="
                + extrasConfig.advancedPacketActions
                + ", entityPacketFiltering=" + extrasConfig.entityPacketFiltering
                + ", startupDiagnostics=" + extrasConfig.startupDiagnostics
                + ", playerVisibilityPolicySync=" + extrasConfig.playerVisibilityPolicySync
                + ", modules.trigger_extras=" + extrasConfig.module(HyExtrasConfig.MODULE_TRIGGER_EXTRAS).enabled
                + ", modules.placeholder_api=" + extrasConfig.module(HyExtrasConfig.MODULE_PLACEHOLDER_API).enabled
                + ", modules.packet_api=" + extrasConfig.module(HyExtrasConfig.MODULE_PACKET_API).enabled
                + ", modules.image_icons=" + extrasConfig.module(HyExtrasConfig.MODULE_IMAGE_ICONS).enabled
                + ", modules.tag_npc=" + extrasConfig.module(HyExtrasConfig.MODULE_TAG_NPC).enabled
                + ", modules.floating_items=" + extrasConfig.module(HyExtrasConfig.MODULE_FLOATING_ITEMS).enabled
                + ", imageIcons.hotReload=" + extrasConfig.imageIconsHotReload
                + ", imageIcons.remoteCache.enabled=" + extrasConfig.imageIconsRemoteCacheEnabled
                + ", imageIcons.maxIconsPerViewer=" + extrasConfig.imageIconsMaxIconsPerViewer
                + ", tagNpc.defaultVisibilityRadius=" + extrasConfig.tagNpcDefaultVisibilityRadius
                + ", tagNpc.clearStateOnEntityUnload=" + extrasConfig.tagNpcClearStateOnEntityUnload
                + ", floatingItems.maxItems=" + extrasConfig.floatingItemsMaxItems
                + ", preventTargetingMemorySync=true"
                + ", debugMode=" + extrasConfig.debugMode);
        getLogger().at(Level.INFO).log("[hextras preflight] recommendedPacketProfile="
                + (extrasConfig.advancedPacketActions
                && extrasConfig.playerVisibilityPolicySync
                && !extrasConfig.entityPacketFiltering)
                + " (supported player packets=" + extrasConfig.advancedPacketActions
                + ", player policy sync=" + extrasConfig.playerVisibilityPolicySync
                + ", experimental entity filter=" + extrasConfig.entityPacketFiltering + ")");
        getLogger().at(Level.INFO).log("[hextras preflight] registeredTypes effects="
                + TriggerActionRegistry.TYPE_IDS.size()
                + " conditions=" + TriggerConditionRegistry.TYPE_IDS.size());
        checkPluginConflicts();
        checkTriggerVolumesAvailability();
        checkPacketFeatureRisk();
    }

    private void checkPluginConflicts() {
        try {
            List<PluginBase> plugins = PluginManager.get().getPlugins();
            long sameIdentifierCount = plugins.stream()
                    .filter(plugin -> getIdentifier().equals(plugin.getIdentifier()))
                    .count();
            if (sameIdentifierCount > 1) {
                getLogger().at(Level.WARNING).log("[hextras preflight] Conflict: detected "
                        + sameIdentifierCount + " loaded plugins with identifier " + getIdentifier());
            }

            String relevantPlugins = plugins.stream()
                    .map(plugin -> String.valueOf(plugin.getIdentifier()))
                    .filter(id -> id.contains("TriggerVolumes")
                            || id.contains("NPCCombatActionEvaluator")
                            || id.contains("NPC")
                            || id.contains("Spawning")
                            || id.contains("InteractionModule"))
                    .collect(Collectors.joining(", "));
            getLogger().at(Level.INFO).log("[hextras preflight] loadedPlugins=" + plugins.size()
                    + (relevantPlugins.isBlank() ? "" : " relevant=[" + relevantPlugins + "]"));
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras preflight] Could not inspect loaded plugins.");
        }
    }

    private void checkTriggerVolumesAvailability() {
        try {
            TriggerVolumesPlugin triggerVolumes = TriggerVolumesPlugin.get();
            if (triggerVolumes == null) {
                getLogger().at(Level.WARNING)
                        .log("[hextras preflight] TriggerVolumesPlugin.get() returned null.");
            } else {
                getLogger().at(Level.INFO)
                        .log("[hextras preflight] TriggerVolumes plugin available.");
            }
        } catch (Exception e) {
            getLogger().at(Level.WARNING).withCause(e)
                    .log("[hextras preflight] TriggerVolumes plugin is not available yet; registration may retry.");
        }
    }

    private void checkPacketFeatureRisk() {
        if (extrasConfig.entityPacketFiltering) {
            getLogger().at(Level.WARNING).log("[hextras preflight] entityPacketFiltering=true is experimental. "
                    + "If players time out while joining, set entityPacketFiltering=false.");
        }
        if (!extrasConfig.advancedPacketActions) {
            getLogger().at(Level.INFO).log("[hextras preflight] advancedPacketActions=false; "
                    + "packet-backed visibility/camera/title helpers may be state-only or disabled.");
        }
    }

    public List<VolumeEntry> getActiveVolumesForPlayer(UUID playerUuid) {
        return triggerExtrasInteractionBridge.getActiveVolumesForPlayer(playerUuid);
    }

    public Collection<PlayerRef> getOnlinePlayers() {
        return List.copyOf(onlinePlayers.values());
    }

    @Nullable
    public PlayerRef getOnlinePlayerRef(UUID uuid) {
        return onlinePlayers.get(uuid);
    }

    @Override
    protected void shutdown() {
        stopPacketFeatureServices();
        if (imageIconService != null) {
            imageIconService.stop();
        }
        if (tagNpcService != null) {
            tagNpcService.stop();
        }
        if (floatingItemService != null) {
            floatingItemService.stop();
        }
        if (triggerExtrasInteractionBridge != null) {
            triggerExtrasInteractionBridge.getInteractableVolumeState().clearAll();
        }
        if (targetingPreventionService != null) {
            targetingPreventionService.clear();
        }
        instance = null;
        playerNameToUuid.clear();
        onlinePlayers.clear();
        getLogger().at(Level.INFO).log("HyExtras shut down.");
    }

    /** Re-reads {@code hyextras.properties} and propagates the new config to the runtime state. */
    public void reloadConfig() {
        extrasConfig = ConfigLoader.load(getDataDirectory());
        runtimeState.updateConfig(extrasConfig);
        if (moduleManager != null) {
            moduleManager.refreshFromConfig(extrasConfig);
        }
        applyPacketFeatureConfig();
        runStartupDiagnostics("reload");
        getLogger().at(Level.INFO).log("HyExtras config reloaded.");
    }

    public void applyPacketFeatureConfig() {
        if (packetApi != null) {
            packetApi.applyConfig();
        }
    }

    public void stopPacketFeatureServices() {
        if (packetApi != null) {
            packetApi.stopServices();
        }
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
    public InteractionTriggerService getInteractionTriggerService() {
        return triggerExtrasInteractionBridge.getInteractionTriggerService();
    }
    public PlayerTagService getTagService() { return tagService; }
    public PacketApi getPacketApi() { return packetApi; }
    public ImageIconService getImageIconService() { return imageIconService; }
    public TagNpcService getTagNpcService() { return tagNpcService; }
    public FloatingItemService getFloatingItemService() { return floatingItemService; }
    public TargetingPreventionService getTargetingPreventionService() { return targetingPreventionService; }
    public org.hyzionstudios.hyextras.triggerextras.InteractableVolumeState getInteractableVolumeState() {
        return triggerExtrasInteractionBridge.getInteractableVolumeState();
    }
    public HyExtrasConfig getExtrasConfig() { return extrasConfig; }
    public RuntimeStateStore getRuntimeState() { return runtimeState; }
    public InternalModuleManager getModuleManager() { return moduleManager; }
    public TriggerExtrasInteractionBridge getTriggerExtrasInteractionBridge() { return triggerExtrasInteractionBridge; }
    public boolean isModuleEnabled(String id) {
        return moduleManager == null || moduleManager.isEnabled(id);
    }
    public Set<String> getInteractionBlockedVolumeIds() {
        return triggerExtrasInteractionBridge.getInteractionBlockedVolumeIds();
    }
    public Set<String> getInteractionAllowedVolumeIds() {
        return triggerExtrasInteractionBridge.getInteractionAllowedVolumeIds();
    }
}
