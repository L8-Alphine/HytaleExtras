package org.hyzionstudios.hyextras.triggerextras;

import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.triggerextras.condition.CooldownReadyCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.HasTagCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.InteractionTypeCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.IsOperatorCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.MathCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.PlayerHiddenCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.VariableCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.VolumeHasTagCondition;
import org.hyzionstudios.hyextras.triggerextras.condition.WorldTimeBetweenCondition;
import org.hyzionstudios.hyextras.triggerextras.floatingitems.FloatingItemExistsCondition;
import org.hyzionstudios.hyextras.triggerextras.floatingitems.FloatingItemIntangibleCondition;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcHasTagCondition;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcVariableCondition;
import org.hyzionstudios.hyextras.triggerextras.tagnpc.TagNpcVisibleCondition;

import java.util.List;

public final class TriggerConditionRegistry {

    public static final List<String> TYPE_IDS = List.of(
            "variable_condition", "cooldown_ready", "is_operator", "world_time_between",
            "player_hidden", "volume_has_tag",
            "has_tag", "interaction_type", "math_condition",
            "tag_npc_has_tag", "tag_npc_variable_condition", "tag_npc_visible_condition",
            "floating_item_exists", "floating_item_intangible"
    );

    private TriggerConditionRegistry() {}

    public static boolean registerAll(HyExtrasPlugin plugin) {
        boolean ok = true;
        ok &= TriggerVolumeApiAdapter.registerCondition("variable_condition",  VariableCondition.class,           VariableCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("cooldown_ready",      CooldownReadyCondition.class,      CooldownReadyCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("is_operator",         IsOperatorCondition.class,         IsOperatorCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("world_time_between",  WorldTimeBetweenCondition.class,   WorldTimeBetweenCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("player_hidden",       PlayerHiddenCondition.class,       PlayerHiddenCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("volume_has_tag",      VolumeHasTagCondition.class,       VolumeHasTagCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("has_tag",             HasTagCondition.class,             HasTagCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("interaction_type",    InteractionTypeCondition.class,    InteractionTypeCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("math_condition",      MathCondition.class,               MathCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("tag_npc_has_tag",     TagNpcHasTagCondition.class,       TagNpcHasTagCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("tag_npc_variable_condition", TagNpcVariableCondition.class, TagNpcVariableCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("tag_npc_visible_condition",  TagNpcVisibleCondition.class,  TagNpcVisibleCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("floating_item_exists",       FloatingItemExistsCondition.class,     FloatingItemExistsCondition.CODEC);
        ok &= TriggerVolumeApiAdapter.registerCondition("floating_item_intangible",   FloatingItemIntangibleCondition.class, FloatingItemIntangibleCondition.CODEC);
        return ok;
    }
}
