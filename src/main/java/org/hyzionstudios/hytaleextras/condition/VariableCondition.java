package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.service.PlayerVariableService;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Condition that tests a per-player variable against a value.
 *
 * <p>Supported operators:
 * <ul>
 *   <li>{@code exists} — variable is set (any value)</li>
 *   <li>{@code not_exists} — variable is not set</li>
 *   <li>{@code equals} — variable equals {@code Value} (string comparison)</li>
 *   <li>{@code not_equals} — variable does not equal {@code Value}</li>
 *   <li>{@code greater_than} — variable (numeric) > {@code Value} (numeric)</li>
 *   <li>{@code less_than} — variable (numeric) < {@code Value} (numeric)</li>
 * </ul>
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "variable_condition", "Key": "zone", "Operator": "equals", "Value": "spawn" }
 * }</pre>
 */
public class VariableCondition extends TriggerCondition {

    public static final BuilderCodec<VariableCondition> CODEC = BuilderCodec.builder(
                    VariableCondition.class, VariableCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("Key"),         VariableCondition::setKey,      VariableCondition::getKey).add()
            .append(CodecHelper.enumField("Operator", Operator.class, Operator.ALIASES),
                    VariableCondition::setOperator, VariableCondition::getOperator).add()
            .append(CodecHelper.optString("Value"),    VariableCondition::setValue,    VariableCondition::getValue).add()
            .build();

    private String key;
    private Operator operator;
    @Nullable private String value;

    public VariableCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            if (key == null || key.isBlank() || operator == null) {
                warn("key or operator is empty");
                return false;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return false;

            PlayerVariableService vars = HytaleextrasPlugin.get().getVariableService();
            return switch (operator) {
                case EXISTS -> vars.get(uuid, key) != null;
                case NOT_EXISTS -> vars.get(uuid, key) == null;
                case EQUALS -> Objects.equals(vars.getString(uuid, key), value);
                case NOT_EQUALS -> !Objects.equals(vars.getString(uuid, key), value);
                case GREATER_THAN -> vars.getLong(uuid, key) > parseLong(value);
                case LESS_THAN -> vars.getLong(uuid, key) < parseLong(value);
            };
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[variable_condition] test failed for key=" + key);
            return false;
        }
    }

    private long parseLong(@Nullable String s) {
        if (s == null || s.isBlank()) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }

    private void warn(String reason) {
        HytaleextrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[variable_condition] " + reason);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }

    @Nullable public String getValue() { return value; }
    public void setValue(@Nullable String value) { this.value = value; }

    public enum Operator {
        EXISTS,
        NOT_EXISTS,
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN;

        private static final Map<Operator, String> ALIASES = Map.of(
                EXISTS, "exists",
                NOT_EXISTS, "not_exists",
                EQUALS, "equals",
                NOT_EQUALS, "not_equals",
                GREATER_THAN, "greater_than",
                LESS_THAN, "less_than"
        );
    }
}
