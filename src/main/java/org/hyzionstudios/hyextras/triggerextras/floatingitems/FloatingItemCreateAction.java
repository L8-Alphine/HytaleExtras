package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemResult;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemTuning;
import org.hyzionstudios.hyextras.module.TriggerExtrasRuntime;
import org.hyzionstudios.hyextras.util.StringTemplate;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.logging.Level;

public final class FloatingItemCreateAction extends TriggerEffect {

    public static final BuilderCodec<FloatingItemCreateAction> CODEC = BuilderCodec.builder(
                    FloatingItemCreateAction.class, FloatingItemCreateAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Id"),
                    FloatingItemCreateAction::setId, FloatingItemCreateAction::getId).add()
            .append(new KeyedCodec<>("Item", ItemStack.CODEC),
                    FloatingItemCreateAction::setItem, FloatingItemCreateAction::getItem).add()
            .append(CodecHelper.optEnum("Anchor", FloatingItemAnchor.class, FloatingItemAnchor.ALIASES),
                    FloatingItemCreateAction::setAnchor, FloatingItemCreateAction::getAnchor).add()
            .append(CodecHelper.optFloat("X"), FloatingItemCreateAction::setX, FloatingItemCreateAction::getX).add()
            .append(CodecHelper.optFloat("Y"), FloatingItemCreateAction::setY, FloatingItemCreateAction::getY).add()
            .append(CodecHelper.optFloat("Z"), FloatingItemCreateAction::setZ, FloatingItemCreateAction::getZ).add()
            .append(CodecHelper.optFloat("OffsetX"),
                    FloatingItemCreateAction::setOffsetX, FloatingItemCreateAction::getOffsetX).add()
            .append(CodecHelper.optFloat("OffsetY"),
                    FloatingItemCreateAction::setOffsetY, FloatingItemCreateAction::getOffsetY).add()
            .append(CodecHelper.optFloat("OffsetZ"),
                    FloatingItemCreateAction::setOffsetZ, FloatingItemCreateAction::getOffsetZ).add()
            .append(CodecHelper.optBool("Persistent"),
                    FloatingItemCreateAction::setPersistent, FloatingItemCreateAction::getPersistent).add()
            .append(CodecHelper.optBool("Intangible"),
                    FloatingItemCreateAction::setIntangible, FloatingItemCreateAction::getIntangible).add()
            .append(CodecHelper.optFloat("Scale"),
                    FloatingItemCreateAction::setScale, FloatingItemCreateAction::getScale).add()
            .append(CodecHelper.optFloat("VisibilityRadius"),
                    FloatingItemCreateAction::setVisibilityRadius, FloatingItemCreateAction::getVisibilityRadius).add()
            .append(CodecHelper.optFloat("BobAmplitude"),
                    FloatingItemCreateAction::setBobAmplitude, FloatingItemCreateAction::getBobAmplitude).add()
            .append(CodecHelper.optFloat("RotationDegreesPerSecond"),
                    FloatingItemCreateAction::setRotationDegreesPerSecond,
                    FloatingItemCreateAction::getRotationDegreesPerSecond).add()
            .append(CodecHelper.optInteger("Priority"),
                    FloatingItemCreateAction::setPriority, FloatingItemCreateAction::getPriority).add()
            .build();

    private String id;
    private ItemStack item;
    @Nullable private FloatingItemAnchor anchor;
    @Nullable private Float x;
    @Nullable private Float y;
    @Nullable private Float z;
    @Nullable private Float offsetX;
    @Nullable private Float offsetY;
    @Nullable private Float offsetZ;
    @Nullable private Boolean persistent;
    @Nullable private Boolean intangible;
    @Nullable private Float scale;
    @Nullable private Float visibilityRadius;
    @Nullable private Float bobAmplitude;
    @Nullable private Float rotationDegreesPerSecond;
    @Nullable private Integer priority;

    @Override
    public void execute(TriggerContext ctx) {
        if (!TriggerExtrasRuntime.isEnabled()
                || !HyExtrasPlugin.get().isModuleEnabled(HyExtrasConfig.MODULE_FLOATING_ITEMS)) {
            return;
        }
        try {
            Vector3d position = FloatingItemPlacement.resolve(ctx, anchor, x, y, z, offsetX, offsetY, offsetZ);
            if (position == null) {
                return;
            }
            HyExtrasConfig config = HyExtrasPlugin.get().getExtrasConfig();
            boolean usePersistent = persistent != null
                    ? persistent
                    : config != null && config.floatingItemsDefaultPersistent;
            FloatingItemTuning defaults = FloatingItemTuning.defaults(config);
            FloatingItemTuning tuning = new FloatingItemTuning(
                    scale == null ? defaults.scale() : scale,
                    visibilityRadius == null ? defaults.visibilityRadius() : visibilityRadius,
                    bobAmplitude == null ? defaults.bobAmplitude() : bobAmplitude,
                    rotationDegreesPerSecond == null
                            ? defaults.rotationDegreesPerSecond()
                            : rotationDegreesPerSecond,
                    offsetX == null ? 0.0f : offsetX,
                    offsetY == null ? 0.0f : offsetY,
                    offsetZ == null ? 0.0f : offsetZ,
                    priority == null ? 0 : priority);
            String resolvedId = StringTemplate.resolve(id, ctx, HyExtrasPlugin.get().getVariableService());
            FloatingItemResult result = HyExtrasPlugin.get().getFloatingItemService()
                    .createFloatingItem(resolvedId, item, ctx.getStore(), position, tuning, usePersistent);
            if (intangible != null && HyExtrasPlugin.get().getFloatingItemService().exists(resolvedId)) {
                HyExtrasPlugin.get().getFloatingItemService().setFloatingItemIntangible(resolvedId, intangible);
            }
            if (!result.success()) {
                HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                        .log("[floating_item_create] " + result.message());
            }
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[floating_item_create] failed");
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public ItemStack getItem() { return item; }
    public void setItem(ItemStack item) { this.item = item; }
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
    @Nullable public Boolean getPersistent() { return persistent; }
    public void setPersistent(@Nullable Boolean persistent) { this.persistent = persistent; }
    @Nullable public Boolean getIntangible() { return intangible; }
    public void setIntangible(@Nullable Boolean intangible) { this.intangible = intangible; }
    @Nullable public Float getScale() { return scale; }
    public void setScale(@Nullable Float scale) { this.scale = scale; }
    @Nullable public Float getVisibilityRadius() { return visibilityRadius; }
    public void setVisibilityRadius(@Nullable Float visibilityRadius) { this.visibilityRadius = visibilityRadius; }
    @Nullable public Float getBobAmplitude() { return bobAmplitude; }
    public void setBobAmplitude(@Nullable Float bobAmplitude) { this.bobAmplitude = bobAmplitude; }
    @Nullable public Float getRotationDegreesPerSecond() { return rotationDegreesPerSecond; }
    public void setRotationDegreesPerSecond(@Nullable Float rotationDegreesPerSecond) {
        this.rotationDegreesPerSecond = rotationDegreesPerSecond;
    }
    @Nullable public Integer getPriority() { return priority; }
    public void setPriority(@Nullable Integer priority) { this.priority = priority; }
}
