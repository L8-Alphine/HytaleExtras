package org.hyzionstudios.hyextras.api;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.event.HyExtrasEvents;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemInstance;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemResult;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemTuning;
import org.hyzionstudios.hyextras.imageicons.ImageIconAttachment;
import org.hyzionstudios.hyextras.imageicons.ImageIconDefinition;
import org.hyzionstudios.hyextras.imageicons.ImageIconProviderRegistration;
import org.hyzionstudios.hyextras.imageicons.ImageIconResult;
import org.hyzionstudios.hyextras.imageicons.ImageIconTuning;
import org.hyzionstudios.hyextras.packetapi.PacketCameraMode;
import org.hyzionstudios.hyextras.tagnpc.TagNpcEntityState;
import org.hyzionstudios.hyextras.tagnpc.TagNpcResult;
import org.hyzionstudios.hyextras.triggerextras.action.SetCameraAction;
import org.hyzionstudios.hyextras.util.RichText;
import org.hyzionstudios.hyextras.util.RuleEvaluationContext;
import org.hyzionstudios.hyextras.util.RuleEvaluator;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Stable public facade for other server mods that want to use HyExtras features
 * without depending on HyExtras internal service classes.
 *
 * <p>Call {@link #get()} after HyExtras has started. Methods that target online
 * players by packet return {@code false} when the player is offline. State methods
 * generally accept UUIDs and do not require the player to be online unless noted.</p>
 *
 * <p>Persistence rules:</p>
 * <ul>
 *   <li>Variables are runtime-only and clear on disconnect by default; enable
 *       {@code playerVariablesPersistent} or use a {@code persist:}-prefixed key to persist them.</li>
 *   <li>Cooldowns are runtime-only and clear on disconnect.</li>
 *   <li>Tags are persisted by HyExtras and survive reconnects/server restarts.</li>
 *   <li>Visibility overrides are runtime-only and clear on disconnect.</li>
 * </ul>
 */
public final class HyExtrasApi {

    /** Default per-player variable used by HyExtras volume policy for party/group identity. */
    public static final String DEFAULT_PARTY_VARIABLE = "partyId";

    private static final HyExtrasApi INSTANCE = new HyExtrasApi();

    private HyExtrasApi() {}

    /** Returns the singleton API facade. */
    public static HyExtrasApi get() {
        return INSTANCE;
    }

    /** Returns true when HyExtras is currently started and the facade can be used. */
    public boolean isAvailable() {
        return HyExtrasPlugin.get() != null;
    }

    /** Resolves an online player UUID by username, or null if the player is not online. */
    @Nullable
    public UUID getOnlinePlayerUuid(String username) {
        return plugin().getPlayerUuidByName(username);
    }

    /** Returns true if the player is currently online according to HyExtras' player registry. */
    public boolean isPlayerOnline(UUID player) {
        return plugin().getOnlinePlayerRef(player) != null;
    }

    /** Returns the current username for an online player, or null if the player is offline. */
    @Nullable
    public String getOnlinePlayerName(UUID player) {
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        return ref != null ? ref.getUsername() : null;
    }

    /** Returns IDs of trigger volumes the online player is currently inside. */
    public List<String> getActiveVolumeIds(UUID player) {
        return plugin().getActiveVolumesForPlayer(player).stream()
                .map(volume -> volume.getId())
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns a raw runtime variable value, or null if missing. */
    @Nullable
    public Object getVariable(UUID player, String key) {
        return plugin().getVariableService().get(player, key);
    }

    /** Returns a runtime variable as a string, or null if missing. */
    @Nullable
    public String getVariableString(UUID player, String key) {
        return plugin().getVariableService().getString(player, key);
    }

    /** Sets a runtime-only player variable. */
    public void setVariable(UUID player, String key, Object value) {
        plugin().getVariableService().set(player, key, value);
    }

    /** Convenience string setter for runtime-only player variables. */
    public void setVariableString(UUID player, String key, String value) {
        plugin().getVariableService().set(player, key, value);
    }

    /** Atomically increments a numeric runtime variable and returns the new value. */
    public long incrementVariable(UUID player, String key, long delta) {
        return plugin().getVariableService().increment(player, key, delta);
    }

    /** Removes one runtime variable. */
    public void removeVariable(UUID player, String key) {
        plugin().getVariableService().remove(player, key);
    }

    /** Clears all runtime variables for a player. */
    public void clearVariables(UUID player) {
        plugin().getVariableService().clearPlayer(player);
    }

    /** Returns a defensive snapshot of runtime variables for a player. */
    public Map<String, Object> snapshotVariables(UUID player) {
        return plugin().getVariableService().snapshot(player);
    }

    /**
     * Persists the player's currently-persisting variables to disk without clearing them.
     * Only variables covered by {@code playerVariablesPersistent} or the {@code persist:} key prefix
     * are written.
     */
    public void saveVariables(UUID player) {
        plugin().getVariableService().savePlayer(player);
    }

    /** Adds a persistent player tag. */
    public void addTag(UUID player, String tag) {
        plugin().getTagService().addTag(player, tag);
    }

    /** Removes a persistent player tag. */
    public void removeTag(UUID player, String tag) {
        plugin().getTagService().removeTag(player, tag);
    }

    /** Clears all persistent tags for a player and deletes the persisted tag file. */
    public void clearTags(UUID player) {
        plugin().getTagService().clearTags(player);
    }

    /** Returns whether a player has a persistent tag loaded in memory. */
    public boolean hasTag(UUID player, String tag) {
        return plugin().getTagService().hasTag(player, tag);
    }

    /** Returns a defensive snapshot of persistent tags currently loaded for a player. */
    public Set<String> snapshotTags(UUID player) {
        return plugin().getTagService().snapshotTags(player);
    }

    /** Persists the player's currently loaded tags without clearing them. */
    public void saveTags(UUID player) {
        plugin().getTagService().savePlayer(player);
    }

    /** Returns true when the named runtime cooldown is absent or expired. */
    public boolean isCooldownReady(UUID player, String name) {
        return plugin().getCooldownService().isReady(player, name);
    }

    /** Applies or resets a runtime cooldown for the given number of seconds. */
    public void applyCooldown(UUID player, String name, float durationSeconds) {
        plugin().getCooldownService().apply(player, name, durationSeconds);
    }

    /** Clears one runtime cooldown. */
    public void clearCooldown(UUID player, String name) {
        plugin().getCooldownService().clear(player, name);
    }

    /** Clears all runtime cooldowns for a player. */
    public void clearCooldowns(UUID player) {
        plugin().getCooldownService().clearAll(player);
    }

    /** Returns remaining cooldown time in seconds, or 0 when not active. */
    public float remainingCooldownSeconds(UUID player, String name) {
        return plugin().getCooldownService().remainingSeconds(player, name);
    }

    /** Returns a snapshot of active cooldowns as name to remaining seconds. */
    public Map<String, Float> snapshotCooldowns(UUID player) {
        return plugin().getCooldownService().snapshot(player);
    }

    /**
     * Hides {@code target} from {@code viewer}. Returns false when inputs are invalid
     * or a force-allow volume policy prevents the hide.
     */
    public boolean hidePlayerFrom(UUID viewer, UUID target, boolean usePackets) {
        return plugin().getPacketApi().hidePlayer(viewer, target, usePackets);
    }

    /** Username overload for {@link #hidePlayerFrom(UUID, UUID, boolean)}. */
    public boolean hidePlayerFrom(String viewerName, String targetName, boolean usePackets) {
        UUID viewer = getOnlinePlayerUuid(viewerName);
        UUID target = getOnlinePlayerUuid(targetName);
        return viewer != null && target != null && hidePlayerFrom(viewer, target, usePackets);
    }

    /** Removes an explicit hide override and sends a show packet when policy allows it. */
    public boolean showPlayerTo(UUID viewer, UUID target, boolean usePackets) {
        return plugin().getPacketApi().showPlayer(viewer, target, usePackets);
    }

    /** Username overload for {@link #showPlayerTo(UUID, UUID, boolean)}. */
    public boolean showPlayerTo(String viewerName, String targetName, boolean usePackets) {
        UUID viewer = getOnlinePlayerUuid(viewerName);
        UUID target = getOnlinePlayerUuid(targetName);
        return viewer != null && target != null && showPlayerTo(viewer, target, usePackets);
    }

    /** Returns the effective visibility result after volume policy and explicit overrides. */
    public boolean isPlayerHiddenFrom(UUID viewer, UUID target) {
        return plugin().getPacketApi().shouldHidePlayer(viewer, target);
    }

    /** Records a best-effort non-player entity hide by entity UUID for packet filtering. */
    public void hideEntityFrom(UUID viewer, UUID entity) {
        plugin().getPacketApi().hideEntity(viewer, entity);
    }

    /** Clears a best-effort non-player entity hide by entity UUID. */
    public void showEntityTo(UUID viewer, UUID entity) {
        plugin().getPacketApi().showEntity(viewer, entity);
    }

    /** Returns explicit UUIDs hidden from this viewer, before volume policy is applied. */
    public Set<UUID> snapshotHiddenPlayers(UUID viewer) {
        return plugin().getPacketApi().snapshotHiddenPlayers(viewer);
    }

    /** Clears explicit hide overrides for this viewer. Volume policy may still hide players. */
    public void clearHiddenPlayers(UUID viewer) {
        plugin().getPacketApi().clearHiddenPlayers(viewer, false);
    }

    /** Protects a player from supported NPC target memory until explicitly cleared. */
    public void protectPlayerFromTargeting(UUID player) {
        plugin().getTargetingPreventionService().protectPlayer(player);
    }

    /** Removes targeting protection applied by HyExtras. */
    public void unprotectPlayerFromTargeting(UUID player) {
        plugin().getTargetingPreventionService().unprotectPlayer(player);
    }

    /** Returns true when HyExtras is currently clearing this player from NPC target memory. */
    public boolean isPlayerProtectedFromTargeting(UUID player) {
        return plugin().getTargetingPreventionService().snapshotProtectedPlayers().contains(player);
    }

    /** Returns players currently protected from supported NPC target memory. */
    public Set<UUID> snapshotTargetingProtectedPlayers() {
        return plugin().getTargetingPreventionService().snapshotProtectedPlayers();
    }

    /** Sends a rich chat message to an online player. Returns false if offline. */
    public boolean sendRichMessage(UUID player, String message) {
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        if (ref == null) return false;
        ref.sendMessage(RichText.toMessage(message));
        return true;
    }

    /** Sends a title/subtitle packet to an online player. Returns false if offline. */
    public boolean sendTitle(UUID player, String title, @Nullable String subtitle, float durationSeconds) {
        return sendTitle(player, title, subtitle, durationSeconds, 0.5f, 0.5f);
    }

    /** Sends a title/subtitle packet with custom fade timings. Returns false if offline. */
    public boolean sendTitle(
            UUID player,
            String title,
            @Nullable String subtitle,
            float durationSeconds,
            float fadeInSeconds,
            float fadeOutSeconds) {
        return plugin().getPacketApi().sendTitle(
                player,
                title,
                subtitle,
                durationSeconds,
                fadeInSeconds,
                fadeOutSeconds);
    }

    /** Sends an action-bar style notification to an online player. Returns false if offline. */
    public boolean sendActionBar(UUID player, String message) {
        return plugin().getPacketApi().sendActionBar(player, message);
    }

    /** Sets an online player's camera mode. Returns false if offline. */
    public boolean setCamera(UUID player, SetCameraAction.CameraMode mode, boolean locked) {
        return plugin().getPacketApi().setCamera(player, toPacketCameraMode(mode), locked);
    }

    /** Resets an online player's camera to first person and unlocked. */
    public boolean resetCamera(UUID player) {
        return setCamera(player, SetCameraAction.CameraMode.RESET, false);
    }

    /** Evaluates a HyExtras rule expression against a player and their active volumes. */
    public boolean evaluateRule(String rule, UUID player) {
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        return RuleEvaluator.matches(rule, RuleEvaluationContext.runtime(
                player,
                ref != null ? ref.getUsername() : null,
                plugin().getActiveVolumesForPlayer(player)));
    }

    /** Resolves HyExtras placeholders against a player and their active volumes. */
    public String resolveText(String template, UUID player) {
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        return RuleEvaluator.resolveText(template, RuleEvaluationContext.runtime(
                player,
                ref != null ? ref.getUsername() : null,
                plugin().getActiveVolumesForPlayer(player)));
    }

    /** Registers a provider-owned local ImageIcons asset folder. */
    public ImageIconResult registerImageIconProvider(String providerId, Path assetsPath) {
        return plugin().getImageIconService().registerProvider(providerId, assetsPath);
    }

    /** Registers and caches a remote PNG/GIF icon under a provider-scoped icon id. */
    public ImageIconResult registerRemoteImageIcon(String providerId, String iconId, URI remoteUri) {
        return plugin().getImageIconService().registerRemoteIcon(providerId, iconId, remoteUri);
    }

    /** Reloads a single provider's local and remote icons. */
    public ImageIconResult reloadImageIconProvider(String providerId) {
        return plugin().getImageIconService().reloadProvider(providerId);
    }

    /** Unregisters one provider and clears only that provider's active icon attachments. */
    public ImageIconResult unregisterImageIconProvider(String providerId) {
        return plugin().getImageIconService().unregisterProvider(providerId);
    }

    /** Attaches a provider-scoped icon to an entity UUID using runtime-only packet state. */
    public ImageIconResult attachImageIcon(UUID target, String providerId, String iconId, ImageIconTuning tuning) {
        return plugin().getImageIconService().attachIcon(target, providerId, iconId, tuning);
    }

    /** Attaches a provider-scoped icon to a player UUID using runtime-only packet state. */
    public ImageIconResult attachImageIconToPlayer(
            UUID targetPlayer,
            String providerId,
            String iconId,
            ImageIconTuning tuning) {
        return plugin().getImageIconService().attachIconToPlayer(targetPlayer, providerId, iconId, tuning);
    }

    /** Clears one ImageIcons attachment by attachment UUID. */
    public boolean clearImageIcon(UUID attachmentId) {
        return plugin().getImageIconService().clearIcon(attachmentId);
    }

    /** Clears all ImageIcons attachments targeting the given entity or player UUID. */
    public int clearImageIcons(UUID target) {
        return plugin().getImageIconService().clearIcons(target);
    }

    /** Returns registered ImageIcons providers keyed by provider id. */
    public Map<String, ImageIconProviderRegistration> snapshotImageIconProviders() {
        return plugin().getImageIconService().snapshotProviders();
    }

    /** Returns loaded ImageIcons definitions for one provider keyed by provider-scoped icon id. */
    public Map<String, ImageIconDefinition> snapshotImageIcons(String providerId) {
        return plugin().getImageIconService().snapshotIcons(providerId);
    }

    /** Returns all loaded ImageIcons definitions grouped by provider id. */
    public Map<String, Map<String, ImageIconDefinition>> snapshotImageIcons() {
        return plugin().getImageIconService().snapshotIcons();
    }

    /** Returns active runtime ImageIcons attachments keyed by attachment UUID. */
    public Map<UUID, ImageIconAttachment> snapshotImageIconAttachments() {
        return plugin().getImageIconService().snapshotAttachments();
    }

    /** Returns load errors grouped by provider id. */
    public Map<String, List<String>> snapshotImageIconLoadErrors() {
        return plugin().getImageIconService().snapshotLoadErrors();
    }

    /** Adds a runtime TagNPC tag to any UUID-backed entity. */
    public TagNpcResult addEntityTag(UUID entity, String tag) {
        return plugin().getTagNpcService().addTag(entity, tag);
    }

    /** Removes a runtime TagNPC tag from any UUID-backed entity. */
    public TagNpcResult removeEntityTag(UUID entity, String tag) {
        return plugin().getTagNpcService().removeTag(entity, tag);
    }

    /** Returns whether a UUID-backed entity currently has the TagNPC tag. */
    public boolean hasEntityTag(UUID entity, String tag) {
        return plugin().getTagNpcService().hasTag(entity, tag);
    }

    /** Returns a defensive snapshot of runtime TagNPC tags for an entity. */
    public Set<String> snapshotEntityTags(UUID entity) {
        return plugin().getTagNpcService().snapshotTags(entity);
    }

    /** Clears runtime TagNPC tags for an entity. */
    public TagNpcResult clearEntityTags(UUID entity) {
        return plugin().getTagNpcService().clearTags(entity);
    }

    /** Sets a runtime TagNPC variable on any UUID-backed entity. */
    public TagNpcResult setEntityVariable(UUID entity, String key, Object value) {
        return plugin().getTagNpcService().setVariable(entity, key, value);
    }

    /** Returns a raw runtime TagNPC entity variable, or null if missing. */
    @Nullable
    public Object getEntityVariable(UUID entity, String key) {
        return plugin().getTagNpcService().getVariable(entity, key);
    }

    /** Returns a runtime TagNPC entity variable as a string, or null if missing. */
    @Nullable
    public String getEntityVariableString(UUID entity, String key) {
        return plugin().getTagNpcService().getVariableString(entity, key);
    }

    /** Atomically increments a runtime TagNPC numeric variable and returns the new value. */
    public long incrementEntityVariable(UUID entity, String key, long delta) {
        return plugin().getTagNpcService().incrementVariable(entity, key, delta);
    }

    /** Removes one runtime TagNPC entity variable. */
    public TagNpcResult removeEntityVariable(UUID entity, String key) {
        return plugin().getTagNpcService().removeVariable(entity, key);
    }

    /** Returns a defensive snapshot of runtime TagNPC variables for an entity. */
    public Map<String, Object> snapshotEntityVariables(UUID entity) {
        return plugin().getTagNpcService().snapshotVariables(entity);
    }

    /** Clears runtime TagNPC variables for an entity. */
    public TagNpcResult clearEntityVariables(UUID entity) {
        return plugin().getTagNpcService().clearVariables(entity);
    }

    /** Sets a developer-defined display name for a tracked TagNPC entity (blank clears it). */
    public TagNpcResult setEntityDisplayName(UUID entity, String displayName) {
        return plugin().getTagNpcService().setDisplayName(entity, displayName);
    }

    /** Returns the developer-defined display name for a TagNPC entity, or null when unset. */
    @Nullable
    public String getEntityDisplayName(UUID entity) {
        return plugin().getTagNpcService().getDisplayName(entity);
    }

    /** Hides a UUID-backed entity from one viewer through PacketAPI entity visibility state. */
    public TagNpcResult hideEntityFromViewer(UUID viewer, UUID entity) {
        return plugin().getTagNpcService().hideEntityFromViewer(viewer, entity);
    }

    /** Shows a UUID-backed entity to one viewer by clearing PacketAPI entity visibility state. */
    public TagNpcResult showEntityToViewer(UUID viewer, UUID entity) {
        return plugin().getTagNpcService().showEntityToViewer(viewer, entity);
    }

    /** Returns whether TagNPC/PacketAPI state currently hides an entity from a viewer. */
    public boolean isEntityHiddenFromViewer(UUID viewer, UUID entity) {
        return plugin().getTagNpcService().isEntityHiddenFromViewer(viewer, entity);
    }

    /** Returns UUIDs of entities currently tracked with the given TagNPC tag. */
    public Set<UUID> snapshotTaggedEntities(String tag) {
        return plugin().getTagNpcService().snapshotTaggedEntities(tag);
    }

    /** Returns a snapshot of one TagNPC entity state, or null when untracked. */
    @Nullable
    public TagNpcEntityState snapshotTagNpcState(UUID entity) {
        return plugin().getTagNpcService().snapshotState(entity);
    }

    /** Returns all tracked TagNPC entity states keyed by entity UUID. */
    public Map<UUID, TagNpcEntityState> snapshotAllTagNpcStates() {
        return plugin().getTagNpcService().snapshotAllStates();
    }

    /** Returns the nearest indexed non-player entity UUID to an online player within radius, or null. */
    @Nullable
    public UUID findNearestTagNpcEntity(UUID player, double radius) {
        return plugin().getTagNpcService().findClosestEntityToPlayer(player, radius);
    }

    /** Creates or replaces a decorative non-pickup floating item at an explicit store position. */
    public FloatingItemResult createFloatingItem(
            String id,
            ItemStack item,
            Store<EntityStore> store,
            Vector3d position,
            FloatingItemTuning tuning,
            boolean persistent) {
        return plugin().getFloatingItemService()
                .createFloatingItem(id, item, store, position, tuning, persistent);
    }

    /** Creates or replaces a decorative non-pickup floating item at an online player's position. */
    public FloatingItemResult createFloatingItemAtPlayer(
            String id,
            UUID player,
            ItemStack item,
            FloatingItemTuning tuning,
            boolean persistent) {
        return plugin().getFloatingItemService()
                .createFloatingItemAtPlayer(id, player, item, tuning, persistent);
    }

    /** Removes a floating item by id. */
    public FloatingItemResult removeFloatingItem(String id) {
        return plugin().getFloatingItemService().removeFloatingItem(id);
    }

    /** Sets whether a floating item is intangible. */
    public FloatingItemResult setFloatingItemIntangible(String id, boolean intangible) {
        return plugin().getFloatingItemService().setFloatingItemIntangible(id, intangible);
    }

    /** Moves a floating item to a new explicit store position. */
    public FloatingItemResult moveFloatingItem(String id, Store<EntityStore> store, Vector3d position) {
        return plugin().getFloatingItemService().moveFloatingItem(id, store, position);
    }

    /** Returns a snapshot of one floating item, or null when absent. */
    @Nullable
    public FloatingItemInstance snapshotFloatingItem(String id) {
        return plugin().getFloatingItemService().snapshotFloatingItem(id);
    }

    /** Returns all floating item snapshots keyed by id. */
    public Map<String, FloatingItemInstance> snapshotFloatingItems() {
        return plugin().getFloatingItemService().snapshotFloatingItems();
    }

    /** Returns floating items near a position in the same store. */
    public Map<String, FloatingItemInstance> snapshotFloatingItemsNear(
            Store<EntityStore> store,
            Vector3d origin,
            double radius) {
        return plugin().getFloatingItemService().snapshotFloatingItemsNear(store, origin, radius);
    }

    /**
     * Subscribes to a HyExtras state-change event. Event types live in {@link HyExtrasEvents}
     * (e.g. {@link HyExtrasEvents.PlayerVariableChangeEvent}, {@link HyExtrasEvents.InteractionEvent}).
     * Listeners run synchronously on the thread that posts the event and are isolated from each other.
     *
     * @return a handle whose {@link AutoCloseable#close()} unregisters the listener
     */
    public <E> AutoCloseable subscribe(Class<E> eventType, Consumer<E> listener) {
        return plugin().getEventBus().subscribe(eventType, listener);
    }

    private static HyExtrasPlugin plugin() {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        if (plugin == null) {
            throw new IllegalStateException("HyExtras is not running");
        }
        return plugin;
    }

    private static PacketCameraMode toPacketCameraMode(SetCameraAction.CameraMode mode) {
        if (mode == null) {
            return PacketCameraMode.FIRST_PERSON;
        }
        return switch (mode) {
            case FIRST_PERSON -> PacketCameraMode.FIRST_PERSON;
            case THIRD_PERSON -> PacketCameraMode.THIRD_PERSON;
            case RESET -> PacketCameraMode.RESET;
        };
    }
}
