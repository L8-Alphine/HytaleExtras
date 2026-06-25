package org.hyzionstudios.hyextras.event;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Container for HyExtras state-change event types posted on the {@link HyExtrasEventBus}.
 *
 * <p>These let external mods (and HyExtras subsystems) react to tag/variable/visibility/interaction
 * changes without polling. Events are immutable records; {@code tag}/{@code key} are {@code null}
 * for {@link ChangeType#CLEAR}.
 */
public final class HyExtrasEvents {

    private HyExtrasEvents() {}

    public enum ChangeType { ADD, REMOVE, SET, CLEAR }

    public record PlayerTagChangeEvent(UUID player, @Nullable String tag, ChangeType type) {}

    public record PlayerVariableChangeEvent(
            UUID player, @Nullable String key, @Nullable Object value, ChangeType type) {}

    public record EntityTagChangeEvent(UUID entity, @Nullable String tag, ChangeType type) {}

    public record EntityVariableChangeEvent(
            UUID entity, @Nullable String key, @Nullable Object value, ChangeType type) {}

    /** Posted when a viewer's view of a target is changed. {@code hidden} is the new state. */
    public record VisibilityChangeEvent(UUID viewer, UUID target, boolean hidden) {}

    /** Posted after an interaction is dispatched. {@code targetEntity} is null for block interactions. */
    public record InteractionEvent(
            UUID player,
            @Nullable UUID targetEntity,
            String interactionType,
            @Nullable String volumeId,
            boolean cancelled) {}
}
