package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;

import javax.annotation.Nullable;

public final class FloatingItemExistsCondition extends TriggerCondition {

    public static final BuilderCodec<FloatingItemExistsCondition> CODEC = BuilderCodec.builder(
                    FloatingItemExistsCondition.class, FloatingItemExistsCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("Id"),
                    FloatingItemExistsCondition::setId, FloatingItemExistsCondition::getId).add()
            .append(CodecHelper.optBool("Invert"),
                    FloatingItemExistsCondition::setInvert, FloatingItemExistsCondition::getInvert).add()
            .build();

    private String id;
    @Nullable private Boolean invert;

    @Override
    public boolean test(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled()
                || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS)) {
            return false;
        }
        boolean exists = HyExtrasPlugin.get().getFloatingItemService()
                .exists(StringTemplate.resolve(id, ctx, HyExtrasPlugin.get().getVariableService()));
        return Boolean.TRUE.equals(invert) ? !exists : exists;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    @Nullable public Boolean getInvert() { return invert; }
    public void setInvert(@Nullable Boolean invert) { this.invert = invert; }
}
