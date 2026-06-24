package org.hyzionstudios.hyextras.triggerextras;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.triggerextras.action.*;
import org.hyzionstudios.hyextras.triggerextras.advanced.ClearPlayerOverridesAction;
import org.hyzionstudios.hyextras.triggerextras.advanced.PlayerHideEntityAction;
import org.hyzionstudios.hyextras.triggerextras.advanced.PlayerShowEntityAction;
import org.hyzionstudios.hyextras.triggerextras.floatingitems.FloatingItemCreateAction;
import org.hyzionstudios.hyextras.triggerextras.floatingitems.FloatingItemMoveAction;
import org.hyzionstudios.hyextras.triggerextras.floatingitems.FloatingItemRemoveAction;
import org.hyzionstudios.hyextras.triggerextras.floatingitems.FloatingItemSetIntangibleAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcAddTagAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcAddVariableAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcHideEntityAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcRemoveTagAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcRemoveVariableAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcSetVariableAction;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcShowEntityAction;

import java.util.List;

public final class TriggerActionRegistry {

    public static final List<String> TYPE_IDS = List.of(
            "run_command", "set_variable", "add_variable", "increment_variable", "remove_variable",
            "calculate_variable", "apply_cooldown", "trigger_named_volume", "remove_item",
            "give_item_reward", "run_reward_command", "send_reward_message",
            "send_title", "send_rich_message",
            "toggle_trigger_enabled", "player_hide_entity", "player_show_entity",
            "clear_player_overrides",
            "set_volume_tag", "remove_volume_tag", "cancel_interaction",
            "set_volume_interactable", "clear_volume_interactable",
            "add_tag", "remove_tag", "block_volume_interactions", "allow_volume_interactions",
            "set_camera", "push_back_player", "set_voice_activity",
            "tag_npc_add_tag", "tag_npc_remove_tag", "tag_npc_set_variable",
            "tag_npc_add_variable", "tag_npc_remove_variable",
            "tag_npc_hide_entity", "tag_npc_show_entity",
            "floating_item_create", "floating_item_remove",
            "floating_item_set_intangible", "floating_item_move"
    );

    private TriggerActionRegistry() {}

    public static boolean registerAll(HyExtrasPlugin plugin) {
        boolean ok = true;
        ok &= TriggerVolumeApiAdapter.registerEffect("run_command",            RunCommandAction.class,            RunCommandAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_variable",           SetVariableAction.class,           SetVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("add_variable",           IncrementVariableAction.class,     IncrementVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("increment_variable",     IncrementVariableAction.class,     IncrementVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_variable",        RemoveVariableAction.class,        RemoveVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("calculate_variable",     CalculateVariableAction.class,     CalculateVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("apply_cooldown",         ApplyCooldownAction.class,         ApplyCooldownAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("trigger_named_volume",   TriggerNamedVolumeAction.class,    TriggerNamedVolumeAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_item",            RemoveItemAction.class,            RemoveItemAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("give_item_reward",       GiveItemRewardAction.class,        GiveItemRewardAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("run_reward_command",     RunRewardCommandAction.class,      RunRewardCommandAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("send_reward_message",    SendRewardMessageAction.class,     SendRewardMessageAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("send_title",             SendTitleAction.class,             SendTitleAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("send_rich_message",      SendRichMessageAction.class,       SendRichMessageAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("toggle_trigger_enabled", ToggleTriggerEnabledAction.class,  ToggleTriggerEnabledAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("player_hide_entity",     PlayerHideEntityAction.class,      PlayerHideEntityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("player_show_entity",     PlayerShowEntityAction.class,      PlayerShowEntityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("clear_player_overrides", ClearPlayerOverridesAction.class,  ClearPlayerOverridesAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_volume_tag",              SetVolumeTagAction.class,              SetVolumeTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_volume_tag",           RemoveVolumeTagAction.class,           RemoveVolumeTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("cancel_interaction",          CancelInteractionAction.class,         CancelInteractionAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_volume_interactable",     SetVolumeInteractableAction.class,     SetVolumeInteractableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("clear_volume_interactable",   ClearVolumeInteractableAction.class,   ClearVolumeInteractableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("add_tag",                     AddTagAction.class,                    AddTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("remove_tag",                  RemoveTagAction.class,                 RemoveTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("block_volume_interactions",   BlockVolumeInteractionsAction.class,   BlockVolumeInteractionsAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("allow_volume_interactions",   AllowVolumeInteractionsAction.class,   AllowVolumeInteractionsAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_camera",                  SetCameraAction.class,                 SetCameraAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("push_back_player",            PushBackPlayerAction.class,            PushBackPlayerAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("set_voice_activity",          SetVoiceActivityAction.class,          SetVoiceActivityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_add_tag",             TagNpcAddTagAction.class,              TagNpcAddTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_remove_tag",          TagNpcRemoveTagAction.class,           TagNpcRemoveTagAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_set_variable",        TagNpcSetVariableAction.class,         TagNpcSetVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_add_variable",        TagNpcAddVariableAction.class,         TagNpcAddVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_remove_variable",     TagNpcRemoveVariableAction.class,      TagNpcRemoveVariableAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_hide_entity",         TagNpcHideEntityAction.class,          TagNpcHideEntityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("tag_npc_show_entity",         TagNpcShowEntityAction.class,          TagNpcShowEntityAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("floating_item_create",         FloatingItemCreateAction.class,        FloatingItemCreateAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("floating_item_remove",         FloatingItemRemoveAction.class,        FloatingItemRemoveAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("floating_item_set_intangible", FloatingItemSetIntangibleAction.class, FloatingItemSetIntangibleAction.CODEC);
        ok &= TriggerVolumeApiAdapter.registerEffect("floating_item_move",           FloatingItemMoveAction.class,          FloatingItemMoveAction.CODEC);
        return ok;
    }
}
