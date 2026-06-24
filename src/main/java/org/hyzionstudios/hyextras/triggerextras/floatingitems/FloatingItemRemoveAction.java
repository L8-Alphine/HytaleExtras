package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;

public final class FloatingItemRemoveAction extends TriggerEffect {

    public static final BuilderCodec<FloatingItemRemoveAction> CODEC = BuilderCodec.builder(
                    FloatingItemRemoveAction.class, FloatingItemRemoveAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Id"),
                    FloatingItemRemoveAction::setId, FloatingItemRemoveAction::getId).add()
            .build();

    private String id;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled()
                || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS)) {
            return;
        }
        HyExtrasPlugin.get().getFloatingItemService()
                .removeFloatingItem(StringTemplate.resolve(id, ctx, HyExtrasPlugin.get().getVariableService()));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
