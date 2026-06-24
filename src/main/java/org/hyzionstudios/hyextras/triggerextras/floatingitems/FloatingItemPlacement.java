package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import org.joml.Vector3d;

final class FloatingItemPlacement {

    private FloatingItemPlacement() {}

    static Vector3d resolve(
            TriggerContext ctx,
            FloatingItemAnchor anchor,
            Float x,
            Float y,
            Float z,
            Float offsetX,
            Float offsetY,
            Float offsetZ) {
        FloatingItemAnchor resolved = anchor != null ? anchor : FloatingItemAnchor.TRIGGERING_ENTITY;
        Vector3d position = switch (resolved) {
            case EXPLICIT -> x == null || y == null || z == null
                    ? null
                    : new Vector3d(x, y, z);
            case VOLUME_CENTER -> ctx.getVolume() != null && ctx.getVolume().getPosition() != null
                    ? new Vector3d(ctx.getVolume().getPosition())
                    : null;
            case BLOCK_POSITION -> ctx.getBlockPosition() != null
                    ? new Vector3d(ctx.getBlockPosition())
                    : null;
            case TRIGGERING_ENTITY -> {
                TransformComponent transform = ctx.getStore().getComponent(
                        ctx.getEntityRef(), TransformComponent.getComponentType());
                yield transform != null && transform.getPosition() != null
                        ? new Vector3d(transform.getPosition())
                        : null;
            }
        };
        if (position == null) {
            return null;
        }
        position.add(
                offsetX == null ? 0.0D : offsetX,
                offsetY == null ? 0.0D : offsetY,
                offsetZ == null ? 0.0D : offsetZ);
        return position;
    }
}
