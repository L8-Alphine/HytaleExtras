package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.ArithmeticExpression;
import org.hyzionstudios.hyextras.util.StringTemplate;

import java.util.UUID;
import java.util.logging.Level;

public class CalculateVariableAction extends TriggerEffect {

    public static final BuilderCodec<CalculateVariableAction> CODEC = BuilderCodec.builder(
                    CalculateVariableAction.class, CalculateVariableAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Key"), CalculateVariableAction::setKey, CalculateVariableAction::getKey).add()
            .append(CodecHelper.string("Formula"), CalculateVariableAction::setFormula, CalculateVariableAction::getFormula).add()
            .build();

    private String key;
    private String formula;

    public CalculateVariableAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (key == null || key.isBlank()) {
                warn("key is empty");
                return;
            }
            if (formula == null || formula.isBlank()) {
                warn("formula is empty");
                return;
            }
            UUID uuid = TriggerVolumeApiAdapter.getEntityUuid(ctx);
            if (uuid == null) {
                warn("entity is not a player");
                return;
            }
            String resolved = StringTemplate.resolve(formula, ctx, HyExtrasPlugin.get().getVariableService());
            double value = ArithmeticExpression.evaluate(resolved);
            HyExtrasPlugin.get().getVariableService().set(uuid, key, value == Math.rint(value) ? (long) value : value);
            HyExtrasPlugin.get().getLogger().at(Level.FINE)
                    .log("[calculate_variable] " + key + "=" + ArithmeticExpression.format(value));
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[calculate_variable] failed for key=" + key + " formula=" + formula);
        }
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                .log("[calculate_variable] skipped: " + reason);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
}
