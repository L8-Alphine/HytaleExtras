package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;

public final class FloatingItemSetIntangibleAction extends TriggerEffect {

    public static final BuilderCodec<FloatingItemSetIntangibleAction> CODEC = BuilderCodec.builder(
                    FloatingItemSetIntangibleAction.class,
                    FloatingItemSetIntangibleAction::new,
                    TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Id"),
                    FloatingItemSetIntangibleAction::setId,
                    FloatingItemSetIntangibleAction::getId).add()
            .append(CodecHelper.bool("Intangible"),
                    FloatingItemSetIntangibleAction::setIntangible,
                    FloatingItemSetIntangibleAction::getIntangible).add()
            .build();

    private String id;
    private boolean intangible;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled()
                || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS)) {
            return;
        }
        HyExtrasPlugin.get().getFloatingItemService()
                .setFloatingItemIntangible(
                        StringTemplate.resolve(id, ctx, HyExtrasPlugin.get().getVariableService()),
                        intangible);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean getIntangible() { return intangible; }
    public void setIntangible(boolean intangible) { this.intangible = intangible; }
}
