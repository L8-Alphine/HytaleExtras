package org.hyzionstudios.hyextras.api;

import com.hypixel.hytale.protocol.ClientCameraView;
import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.protocol.packets.interface_.Notification;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.protocol.packets.interface_.ShowEventTitle;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.action.SetCameraAction;
import org.hyzionstudios.hyextras.util.RichText;
import org.hyzionstudios.hyextras.util.RuleEvaluationContext;
import org.hyzionstudios.hyextras.util.RuleEvaluator;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
 *   <li>Variables are runtime-only and clear on disconnect.</li>
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
        return plugin().getVisibilityPolicyService().hidePlayer(viewer, target, usePackets);
    }

    /** Username overload for {@link #hidePlayerFrom(UUID, UUID, boolean)}. */
    public boolean hidePlayerFrom(String viewerName, String targetName, boolean usePackets) {
        UUID viewer = getOnlinePlayerUuid(viewerName);
        UUID target = getOnlinePlayerUuid(targetName);
        return viewer != null && target != null && hidePlayerFrom(viewer, target, usePackets);
    }

    /** Removes an explicit hide override and sends a show packet when policy allows it. */
    public boolean showPlayerTo(UUID viewer, UUID target, boolean usePackets) {
        return plugin().getVisibilityPolicyService().showPlayer(viewer, target, usePackets);
    }

    /** Username overload for {@link #showPlayerTo(UUID, UUID, boolean)}. */
    public boolean showPlayerTo(String viewerName, String targetName, boolean usePackets) {
        UUID viewer = getOnlinePlayerUuid(viewerName);
        UUID target = getOnlinePlayerUuid(targetName);
        return viewer != null && target != null && showPlayerTo(viewer, target, usePackets);
    }

    /** Returns the effective visibility result after volume policy and explicit overrides. */
    public boolean isPlayerHiddenFrom(UUID viewer, UUID target) {
        return plugin().getVisibilityPolicyService().shouldHidePlayer(viewer, target);
    }

    /** Records a best-effort non-player entity hide by entity UUID for packet filtering. */
    public void hideEntityFrom(UUID viewer, UUID entity) {
        plugin().getVisibilityPolicyService().hideEntity(viewer, entity);
    }

    /** Clears a best-effort non-player entity hide by entity UUID. */
    public void showEntityTo(UUID viewer, UUID entity) {
        plugin().getVisibilityPolicyService().showEntity(viewer, entity);
    }

    /** Returns explicit UUIDs hidden from this viewer, before volume policy is applied. */
    public Set<UUID> snapshotHiddenPlayers(UUID viewer) {
        return plugin().getPlayerOverrideService().snapshotHidden(viewer);
    }

    /** Clears explicit hide overrides for this viewer. Volume policy may still hide players. */
    public void clearHiddenPlayers(UUID viewer) {
        plugin().getPlayerOverrideService().clearAll(viewer);
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
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        if (ref == null) return false;
        FormattedMessage primary = RichText.toFormattedMessage(title);
        FormattedMessage secondary = subtitle != null && !subtitle.isBlank()
                ? RichText.toFormattedMessage(subtitle)
                : null;
        ref.getPacketHandler().write(new ShowEventTitle(
                fadeInSeconds, fadeOutSeconds, durationSeconds, "", false, primary, secondary));
        return true;
    }

    /** Sends an action-bar style notification to an online player. Returns false if offline. */
    public boolean sendActionBar(UUID player, String message) {
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        if (ref == null) return false;
        Notification notification = new Notification();
        notification.message = RichText.toFormattedMessage(message);
        notification.style = NotificationStyle.Default;
        ref.getPacketHandler().write(notification);
        return true;
    }

    /** Sets an online player's camera mode. Returns false if offline. */
    public boolean setCamera(UUID player, SetCameraAction.CameraMode mode, boolean locked) {
        PlayerRef ref = plugin().getOnlinePlayerRef(player);
        if (ref == null) return false;
        SetServerCamera packet = new SetServerCamera();
        packet.clientCameraView = mode == SetCameraAction.CameraMode.THIRD_PERSON
                ? ClientCameraView.ThirdPerson
                : ClientCameraView.FirstPerson;
        packet.isLocked = locked && mode != SetCameraAction.CameraMode.RESET;
        ref.getPacketHandler().write(packet);
        return true;
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

    private static HyExtrasPlugin plugin() {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        if (plugin == null) {
            throw new IllegalStateException("HyExtras is not running");
        }
        return plugin;
    }
}
