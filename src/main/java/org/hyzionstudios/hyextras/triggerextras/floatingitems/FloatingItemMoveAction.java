package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;
import org.joml.Vector3d;

import javax.annotation.Nullable;

public final class FloatingItemMoveAction extends TriggerEffect {

    public static final BuilderCodec<FloatingItemMoveAction> CODEC = BuilderCodec.builder(
                    FloatingItemMoveAction.class, FloatingItemMoveAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Id"),
                    FloatingItemMoveAction::setId, FloatingItemMoveAction::getId).add()
            .append(CodecHelper.optEnum("Anchor", FloatingItemAnchor.class, FloatingItemAnchor.ALIASES),
                    FloatingItemMoveAction::setAnchor, FloatingItemMoveAction::getAnchor).add()
            .append(CodecHelper.optFloat("X"), FloatingItemMoveAction::setX, FloatingItemMoveAction::getX).add()
            .append(CodecHelper.optFloat("Y"), FloatingItemMoveAction::setY, FloatingItemMoveAction::getY).add()
            .append(CodecHelper.optFloat("Z"), FloatingItemMoveAction::setZ, FloatingItemMoveAction::getZ).add()
            .append(CodecHelper.optFloat("OffsetX"),
                    FloatingItemMoveAction::setOffsetX, FloatingItemMoveAction::getOffsetX).add()
            .append(CodecHelper.optFloat("OffsetY"),
                    FloatingItemMoveAction::setOffsetY, FloatingItemMoveAction::getOffsetY).add()
            .append(CodecHelper.optFloat("OffsetZ"),
                    FloatingItemMoveAction::setOffsetZ, FloatingItemMoveAction::getOffsetZ).add()
            .build();

    private String id;
    @Nullable private FloatingItemAnchor anchor;
    @Nullable private Float x;
    @Nullable private Float y;
    @Nullable private Float z;
    @Nullable private Float offsetX;
    @Nullable private Float offsetY;
    @Nullable private Float offsetZ;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled()
                || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS)) {
            return;
        }
        Vector3d position = FloatingItemPlacement.resolve(ctx, anchor, x, y, z, offsetX, offsetY, offsetZ);
        if (position == null) {
            return;
        }
        HyExtrasPlugin.get().getFloatingItemService().moveFloatingItem(
                StringTemplate.resolve(id, ctx, HyExtrasPlugin.get().getVariableService()),
                ctx.getStore(),
                position);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    @Nullable public FloatingItemAnchor getAnchor() { return anchor; }
    public void setAnchor(@Nullable FloatingItemAnchor anchor) { this.anchor = anchor; }
    @Nullable public Float getX() { return x; }
    public void setX(@Nullable Float x) { this.x = x; }
    @Nullable public Float getY() { return y; }
    public void setY(@Nullable Float y) { this.y = y; }
    @Nullable public Float getZ() { return z; }
    public void setZ(@Nullable Float z) { this.z = z; }
    @Nullable public Float getOffsetX() { return offsetX; }
    public void setOffsetX(@Nullable Float offsetX) { this.offsetX = offsetX; }
    @Nullable public Float getOffsetY() { return offsetY; }
    public void setOffsetY(@Nullable Float offsetY) { this.offsetY = offsetY; }
    @Nullable public Float getOffsetZ() { return offsetZ; }
    public void setOffsetZ(@Nullable Float offsetZ) { this.offsetZ = offsetZ; }
}
