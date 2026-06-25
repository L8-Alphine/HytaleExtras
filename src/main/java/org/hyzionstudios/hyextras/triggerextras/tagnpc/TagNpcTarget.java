package org.hyzionstudios.hyextras.triggerextras.tagnpc;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.triggerextras.TriggerExtrasInteractionBridge;
import org.hyzionstudios.hyextras.util.StringTemplate;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public enum TagNpcTarget {
    TRIGGERING_ENTITY,
    ENTITY_UUID,
    TARGET_TAG,
    INTERACTED_ENTITY;

    public static final Map<TagNpcTarget, String> ALIASES = Map.of(
            TRIGGERING_ENTITY, "triggering_entity",
            ENTITY_UUID, "entity_uuid",
            TARGET_TAG, "target_tag",
            INTERACTED_ENTITY, "interacted_entity"
    );

    public static Set<UUID> resolveMany(
            TriggerContext ctx,
            TagNpcTarget target,
            String entityUuid,
            String targetTag) {
        if (targetTag != null && !targetTag.isBlank()) {
            return HyExtrasPlugin.get().getTagNpcService().snapshotTaggedEntities(resolveText(targetTag, ctx));
        }

        TagNpcTarget resolvedTarget = target != null ? target : TagNpcTarget.TRIGGERING_ENTITY;
        if (resolvedTarget == TARGET_TAG) {
            return Set.of();
        }
        if (entityUuid != null && !entityUuid.isBlank()) {
            UUID parsed = parseUuid(resolveText(entityUuid, ctx));
            return parsed == null ? Set.of() : Set.of(parsed);
        }
        if (resolvedTarget == ENTITY_UUID) {
            return Set.of();
        }
        if (resolvedTarget == INTERACTED_ENTITY) {
            UUID interacted = resolveInteractedEntity(ctx);
            return interacted == null ? Set.of() : Set.of(interacted);
        }
        UUID triggering = TriggerVolumeApiAdapter.getEntityUuid(ctx);
        return triggering == null ? Set.of() : Set.of(triggering);
    }

    /** Reads the triggering player's most-recently interacted entity (set by the interaction bridge). */
    private static UUID resolveInteractedEntity(TriggerContext ctx) {
        UUID player = TriggerVolumeApiAdapter.getEntityUuid(ctx);
        if (player == null) {
            return null;
        }
        Object raw = HyExtrasPlugin.get().getVariableService()
                .get(player, TriggerExtrasInteractionBridge.INTERACTED_ENTITY_VAR);
        return raw == null ? null : parseUuid(raw.toString());
    }

    public static UUID resolveOne(TriggerContext ctx, String entityUuid) {
        if (entityUuid != null && !entityUuid.isBlank()) {
            return parseUuid(resolveText(entityUuid, ctx));
        }
        return TriggerVolumeApiAdapter.getEntityUuid(ctx);
    }

    public static Set<UUID> ordered(Set<UUID> input) {
        return input == null || input.isEmpty() ? Set.of() : new LinkedHashSet<>(input);
    }

    private static String resolveText(String text, TriggerContext ctx) {
        return StringTemplate.resolve(text, ctx, HyExtrasPlugin.get().getVariableService());
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
