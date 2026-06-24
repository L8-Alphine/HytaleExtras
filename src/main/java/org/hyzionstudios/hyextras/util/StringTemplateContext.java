package org.hyzionstudios.hyextras.util;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.service.PlayerVariableService;

import javax.annotation.Nullable;
import java.util.UUID;

public record StringTemplateContext(
        TriggerContext triggerContext,
        PlayerVariableService variables,
        String playerName,
        @Nullable UUID playerUuid,
        @Nullable PlayerRef playerRef) {

    public static StringTemplateContext fromTrigger(TriggerContext ctx, PlayerVariableService vars) {
        PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
        String playerName = pr != null ? pr.getUsername() : "unknown";
        UUID uuid = pr != null ? pr.getUuid() : null;
        return new StringTemplateContext(ctx, vars, playerName, uuid, pr);
    }
}
