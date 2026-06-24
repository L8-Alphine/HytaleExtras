package org.hyzionstudios.hyextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Moves the triggering player a short distance away from the interacted block
 * or, when no block is present, away from the current volume center.
 */
public class PushBackPlayerAction extends TriggerEffect {

    public static final BuilderCodec<PushBackPlayerAction> CODEC = BuilderCodec.builder(
                    PushBackPlayerAction.class, PushBackPlayerAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.optFloat("Distance"), PushBackPlayerAction::setDistance, PushBackPlayerAction::getDistance).add()
            .append(CodecHelper.optFloat("YOffset"), PushBackPlayerAction::setYOffset, PushBackPlayerAction::getYOffset).add()
            .build();

    @Nullable private Float distance;
    @Nullable private Float yOffset;

    public PushBackPlayerAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        try {
            TransformComponent transform = ctx.getStore().getComponent(
                    ctx.getEntityRef(), TransformComponent.getComponentType());
            if (transform == null || transform.getPosition() == null) {
                return;
            }

            Vector3d position = new Vector3d(transform.getPosition());
            Vector3d source = ctx.getBlockPosition();
            if (source == null && ctx.getVolume() != null) {
                source = ctx.getVolume().getPosition();
            }
            if (source == null) {
                return;
            }

            Vector3d direction = new Vector3d(position.x - source.x, 0.0D, position.z - source.z);
            if (direction.lengthSquared() < 0.0001D) {
                direction.set(0.0D, 0.0D, 1.0D);
            } else {
                direction.normalize();
            }

            double pushDistance = Math.max(0.0D, distance != null ? distance : 1.25F);
            double verticalOffset = yOffset != null ? yOffset : 0.0F;
            Vector3d target = position.add(direction.mul(pushDistance)).add(0.0D, verticalOffset, 0.0D);
            transform.teleportPosition(target);
            transform.markChunkDirty(ctx.getStore());
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[push_back_player] failed");
        }
    }

    @Nullable public Float getDistance() { return distance; }
    public void setDistance(@Nullable Float distance) { this.distance = distance; }

    @Nullable public Float getYOffset() { return yOffset; }
    public void setYOffset(@Nullable Float yOffset) { this.yOffset = yOffset; }
}
