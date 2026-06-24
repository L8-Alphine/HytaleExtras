package org.hyzionstudios.hyextras.util;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public record RuleEvaluationContext(
        UUID playerUuid,
        @Nullable String playerName,
        @Nullable TriggerEventType eventType,
        @Nullable String tagKey,
        @Nullable String tagValue,
        List<VolumeEntry> volumes) {

    public static RuleEvaluationContext fromTrigger(UUID playerUuid, @Nullable String playerName, TriggerContext ctx) {
        List<VolumeEntry> volumes = ctx.getSpatialVolumes() != null
                ? ctx.getSpatialVolumes()
                : List.of(ctx.getVolume());
        return new RuleEvaluationContext(
                playerUuid,
                playerName,
                ctx.getEventType(),
                ctx.getTagKey(),
                ctx.getTagValue(),
                volumes);
    }

    public static RuleEvaluationContext runtime(UUID playerUuid, @Nullable String playerName, List<VolumeEntry> volumes) {
        return new RuleEvaluationContext(playerUuid, playerName, null, null, null, volumes);
    }
}
