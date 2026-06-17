package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.logging.Level;

/**
 * Passes when the current event is a player interaction dispatched by the HytaleExtras
 * interaction bridge, optionally filtered to a specific {@link com.hypixel.hytale.protocol.InteractionType}.
 *
 * <p>Must be used on effects with {@code eventType: TAG_ADDED} inside volumes that carry
 * the static tag {@code hextras:interact}. The bridge sets {@code tagKey = "hextras_interact"}
 * and {@code tagValue = InteractionType.name()} before firing effects.
 *
 * <p>Dropdown document keys include {@code use} (doors, chests, levers), {@code primary}
 * (left-click/attack), {@code secondary} (right-click), {@code ability1/2/3}, {@code pick},
 * and {@code pickup}.
 *
 * <p>JSON config:
 * <pre>{@code
 * // Passes for any interaction inside the volume
 * { "type": "interaction_type", "eventType": "TAG_ADDED" }
 * // Passes only for Use interactions (door / chest / lever)
 * { "type": "interaction_type", "eventType": "TAG_ADDED", "Interaction": "use" }
 * // Passes for anything OTHER than a primary attack
 * { "type": "interaction_type", "eventType": "TAG_ADDED", "Interaction": "primary", "Invert": true }
 * }</pre>
 */
public class InteractionTypeCondition extends TriggerCondition {

    public static final BuilderCodec<InteractionTypeCondition> CODEC = BuilderCodec.builder(
                    InteractionTypeCondition.class, InteractionTypeCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.optEnum("Interaction", InteractionType.class, InteractionAliases.ALIASES),
                    InteractionTypeCondition::setType,
                    InteractionTypeCondition::getType).add()
            .append(CodecHelper.optBool("Invert"),
                    InteractionTypeCondition::setInvert,
                    InteractionTypeCondition::isInvert).add()
            .build();

    @Nullable private InteractionType type;
    @Nullable private Boolean invert;

    public InteractionTypeCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            if (!"hextras_interact".equals(ctx.getTagKey())) return resolveInvert(false);

            if (type == null) {
                return resolveInvert(true);
            }

            boolean matches = type.name().equalsIgnoreCase(ctx.getTagValue());
            return resolveInvert(matches);
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[interaction_type] condition test failed");
            return false;
        }
    }

    private boolean resolveInvert(boolean value) {
        return (invert != null && invert) ? !value : value;
    }

    @Nullable public InteractionType getType() { return type; }
    public void setType(@Nullable InteractionType v) { this.type = v; }

    @Nullable public Boolean isInvert() { return invert; }
    public void setInvert(@Nullable Boolean v) { this.invert = v; }

    private static final class InteractionAliases {
        private static final Map<InteractionType, String> ALIASES = Map.ofEntries(
                Map.entry(InteractionType.Primary, "primary"),
                Map.entry(InteractionType.Secondary, "secondary"),
                Map.entry(InteractionType.Ability1, "ability1"),
                Map.entry(InteractionType.Ability2, "ability2"),
                Map.entry(InteractionType.Ability3, "ability3"),
                Map.entry(InteractionType.Use, "use"),
                Map.entry(InteractionType.Pick, "pick"),
                Map.entry(InteractionType.Pickup, "pickup"),
                Map.entry(InteractionType.CollisionEnter, "collision_enter"),
                Map.entry(InteractionType.CollisionLeave, "collision_leave"),
                Map.entry(InteractionType.Collision, "collision"),
                Map.entry(InteractionType.EntityStatEffect, "entity_stat_effect"),
                Map.entry(InteractionType.SwapTo, "swap_to"),
                Map.entry(InteractionType.SwapFrom, "swap_from"),
                Map.entry(InteractionType.Death, "death"),
                Map.entry(InteractionType.Wielding, "wielding"),
                Map.entry(InteractionType.ProjectileSpawn, "projectile_spawn"),
                Map.entry(InteractionType.ProjectileHit, "projectile_hit"),
                Map.entry(InteractionType.ProjectileMiss, "projectile_miss"),
                Map.entry(InteractionType.ProjectileBounce, "projectile_bounce"),
                Map.entry(InteractionType.Held, "held"),
                Map.entry(InteractionType.HeldOffhand, "held_offhand"),
                Map.entry(InteractionType.Equipped, "equipped"),
                Map.entry(InteractionType.Dodge, "dodge"),
                Map.entry(InteractionType.GameModeSwap, "game_mode_swap")
        );

        private InteractionAliases() {}
    }
}
