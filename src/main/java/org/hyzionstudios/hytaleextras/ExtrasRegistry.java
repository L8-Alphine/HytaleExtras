package org.hyzionstudios.hytaleextras;

import org.hyzionstudios.hytaleextras.action.*;
import org.hyzionstudios.hytaleextras.advanced.ClearPlayerOverridesAction;
import org.hyzionstudios.hytaleextras.advanced.PlayerHideEntityAction;
import org.hyzionstudios.hytaleextras.advanced.PlayerShowEntityAction;
import org.hyzionstudios.hytaleextras.condition.CooldownReadyCondition;
import org.hyzionstudios.hytaleextras.condition.HasTagCondition;
import org.hyzionstudios.hytaleextras.condition.InteractionTypeCondition;
import org.hyzionstudios.hytaleextras.condition.IsOperatorCondition;
import org.hyzionstudios.hytaleextras.condition.PlayerHiddenCondition;
import org.hyzionstudios.hytaleextras.condition.VariableCondition;
import org.hyzionstudios.hytaleextras.condition.VolumeHasTagCondition;
import org.hyzionstudios.hytaleextras.condition.WorldTimeBetweenCondition;
import java.util.List;
import java.util.logging.Level;

/**
 * Registers all HytaleExtras effects and conditions with the native Trigger Volume system.
 * Called early from {@link HytaleextrasPlugin#setup()} so saved custom entries can decode,
 * then retried from {@link HytaleextrasPlugin#start()} if the native plugin was not ready yet.
 */
public final class ExtrasRegistry {

    public static final List<String> EFFECT_TYPE_IDS = List.of(
            "run_command", "set_variable", "add_variable", "increment_variable", "remove_variable",
            "apply_cooldown", "trigger_named_volume", "remove_item", "send_title", "send_rich_message",
            "toggle_trigger_enabled", "player_hide_entity", "player_show_entity",
            "clear_player_overrides",
            "set_volume_tag", "remove_volume_tag", "cancel_interaction",
            "add_tag", "remove_tag", "block_volume_interactions", "allow_volume_interactions",
            "set_camera"
    );

    public static final List<String> CONDITION_TYPE_IDS = List.of(
            "variable_condition", "cooldown_ready", "is_operator", "world_time_between",
            "player_hidden", "volume_has_tag",
            "has_tag", "interaction_type"
    );

    private ExtrasRegistry() {}

    private static volatile boolean registered;

    public static synchronized void register(HytaleextrasPlugin plugin) {
        if (registered) {
            return;
        }

        boolean ok = true;

        // Effects (actions)
        ok &= TriggerVolumeApiAdapter.registerEffect("run_command",            RunCommandAction.class,            RunCommandAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_variable",           SetVariableAction.class,           SetVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("add_variable",           IncrementVariableAction.class,     IncrementVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("increment_variable",     IncrementVariableAction.class,     IncrementVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_variable",        RemoveVariableAction.class,        RemoveVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("apply_cooldown",         ApplyCooldownAction.class,         ApplyCooldownAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("trigger_named_volume",   TriggerNamedVolumeAction.class,    TriggerNamedVolumeAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_item",            RemoveItemAction.class,            RemoveItemAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("send_title",             SendTitleAction.class,             SendTitleAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("send_rich_message",      SendRichMessageAction.class,       SendRichMessageAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("toggle_trigger_enabled", ToggleTriggerEnabledAction.class,  ToggleTriggerEnabledAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("player_hide_entity",     PlayerHideEntityAction.class,      PlayerHideEntityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("player_show_entity",     PlayerShowEntityAction.class,      PlayerShowEntityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("clear_player_overrides", ClearPlayerOverridesAction.class,  ClearPlayerOverridesAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_volume_tag",              SetVolumeTagAction.class,              SetVolumeTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_volume_tag",           RemoveVolumeTagAction.class,           RemoveVolumeTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("cancel_interaction",          CancelInteractionAction.class,         CancelInteractionAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("add_tag",                     AddTagAction.class,                    AddTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_tag",                  RemoveTagAction.class,                 RemoveTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("block_volume_interactions",   BlockVolumeInteractionsAction.class,   BlockVolumeInteractionsAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("allow_volume_interactions",   AllowVolumeInteractionsAction.class,   AllowVolumeInteractionsAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_camera",                  SetCameraAction.class,                 SetCameraAction.CODEC);

        // Conditions
        ok &= TriggerVolumeApiAdapter.registerCondition("variable_condition",  VariableCondition.class,           VariableCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("cooldown_ready",      CooldownReadyCondition.class,      CooldownReadyCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("is_operator",         IsOperatorCondition.class,         IsOperatorCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("world_time_between",  WorldTimeBetweenCondition.class,   WorldTimeBetweenCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("player_hidden",       PlayerHiddenCondition.class,       PlayerHiddenCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("volume_has_tag",      VolumeHasTagCondition.class,       VolumeHasTagCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("has_tag",             HasTagCondition.class,             HasTagCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("interaction_type",   InteractionTypeCondition.class,    InteractionTypeCondition.CODEC);

        if (!ok) {
            plugin.getLogger().at(Level.WARNING)
                    .log("HytaleExtras: trigger volume type registration did not fully complete; will retry later.");
            return;
        }

        registered = true;
        plugin.getLogger().at(Level.INFO)
                .log("HytaleExtras: registered 22 effects and 8 conditions.");
    }
}
