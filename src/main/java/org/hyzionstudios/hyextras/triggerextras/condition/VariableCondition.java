package org.hyzionstudios.hyextras.triggerextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.service.PlayerVariableService;
import org.hyzionstudios.hyextras.util.ComparisonOperator;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Condition that tests a per-player variable against a value.
 *
 * <p>Supported operators (see {@link ComparisonOperator}):
 * <ul>
 *   <li>{@code exists} / {@code not_exists} — variable is / is not set</li>
 *   <li>{@code equals} / {@code not_equals} — string comparison against {@code Value}</li>
 *   <li>{@code greater_than} / {@code less_than} / {@code greater_or_equal} / {@code less_or_equal}
 *       — numeric comparison</li>
 *   <li>{@code divisible_by} — variable (numeric) is divisible by {@code Value} (non-zero)</li>
 *   <li>{@code contains} — variable (string) contains {@code Value}</li>
 *   <li>{@code regex} — variable matches the {@code Value} regular expression
 *       (requires {@code variable.regexEnabled})</li>
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
            .append(CodecHelper.enumField("Operator", ComparisonOperator.class, ComparisonOperator.ALIASES),
                    VariableCondition::setOperator, VariableCondition::getOperator).add()
            .append(CodecHelper.optString("Value"),    VariableCondition::setValue,    VariableCondition::getValue).add()
            .build();

    private String key;
    private ComparisonOperator operator;
    @Nullable private String value;

    public VariableCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return false;
        }
        try {
            if (key == null || key.isBlank() || operator == null) {
                warn("key or operator is empty");
                return false;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) return false;

            PlayerVariableService vars = HyExtrasPlugin.get().getVariableService();
            return operator.evaluate(vars.getString(uuid, key), value, regexEnabled());
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[variable_condition] test failed for key=" + key);
            return false;
        }
    }

    private static boolean regexEnabled() {
        HyExtrasConfig cfg = HyExtrasPlugin.get().getExtrasConfig();
        return cfg == null || cfg.variableRegexEnabled;
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[variable_condition] " + reason);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public ComparisonOperator getOperator() { return operator; }
    public void setOperator(ComparisonOperator operator) { this.operator = operator; }

    @Nullable public String getValue() { return value; }
    public void setValue(@Nullable String value) { this.value = value; }
}
