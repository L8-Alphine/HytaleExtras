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

public final class FloatingItemIntangibleCondition extends TriggerCondition {

    public static final BuilderCodec<FloatingItemIntangibleCondition> CODEC = BuilderCodec.builder(
                    FloatingItemIntangibleCondition.class,
                    FloatingItemIntangibleCondition::new,
                    TriggerCondition.BASE_CODEC)
            .append(CodecHelper.string("Id"),
                    FloatingItemIntangibleCondition::setId,
                    FloatingItemIntangibleCondition::getId).add()
            .append(CodecHelper.optBool("Invert"),
                    FloatingItemIntangibleCondition::setInvert,
                    FloatingItemIntangibleCondition::getInvert).add()
            .build();

    private String id;
    @Nullable private Boolean invert;

    @Override
    public boolean test(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled()
                || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS)) {
            return false;
        }
        boolean intangible = HyExtrasPlugin.get().getFloatingItemService()
                .isIntangible(StringTemplate.resolve(id, ctx, HyExtrasPlugin.get().getVariableService()));
        return Boolean.TRUE.equals(invert) ? !intangible : intangible;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    @Nullable public Boolean getInvert() { return invert; }
    public void setInvert(@Nullable Boolean invert) { this.invert = invert; }
}
