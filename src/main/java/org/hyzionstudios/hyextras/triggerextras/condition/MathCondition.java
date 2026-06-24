package org.hyzionstudios.hyextras.triggerextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.ArithmeticExpression;
import org.hyzionstudios.hyextras.util.StringTemplate;

import java.util.Map;
import java.util.logging.Level;

public class MathCondition extends TriggerCondition {

    private static final double EPSILON = 0.000001D;

    public static final BuilderCodec<MathCondition> CODEC = BuilderCodec.builder(
                    MathCondition.class, MathCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("Formula"), MathCondition::setFormula, MathCondition::getFormula).add()
            .append(CodecHelper.enumField("Operator", Operator.class, Operator.ALIASES),
                    MathCondition::setOperator, MathCondition::getOperator).add()
            .append(CodecHelper.string("Value"), MathCondition::setValue, MathCondition::getValue).add()
            .build();

    private String formula;
    private Operator operator;
    private String value;

    public MathCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return false;
        }
        try {
            if (formula == null || formula.isBlank() || operator == null || value == null || value.isBlank()) {
                warn("formula, operator, or value is empty");
                return false;
            }
            String resolvedFormula = StringTemplate.resolve(formula, ctx, HyExtrasPlugin.get().getVariableService());
            String resolvedValue = StringTemplate.resolve(value, ctx, HyExtrasPlugin.get().getVariableService());
            double left = ArithmeticExpression.evaluate(resolvedFormula);
            double right = ArithmeticExpression.evaluate(resolvedValue);
            return switch (operator) {
                case EQUALS -> Math.abs(left - right) <= EPSILON;
                case NOT_EQUALS -> Math.abs(left - right) > EPSILON;
                case GREATER_THAN -> left > right;
                case LESS_THAN -> left < right;
                case GREATER_OR_EQUAL -> left > right || Math.abs(left - right) <= EPSILON;
                case LESS_OR_EQUAL -> left < right || Math.abs(left - right) <= EPSILON;
            };
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[math_condition] test failed for formula=" + formula);
            return false;
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                .log("[math_condition] skipped: " + reason);
    }

    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public Operator getOperator() { return operator; }
    public void setOperator(Operator operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_OR_EQUAL,
        LESS_OR_EQUAL;

        public static final Map<Operator, String> ALIASES = Map.of(
                EQUALS, "equals",
                NOT_EQUALS, "not_equals",
                GREATER_THAN, "greater_than",
                LESS_THAN, "less_than",
                GREATER_OR_EQUAL, "greater_or_equal",
                LESS_OR_EQUAL, "less_or_equal"
        );
    }
}
