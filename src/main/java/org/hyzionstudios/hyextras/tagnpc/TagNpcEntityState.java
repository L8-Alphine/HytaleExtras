package org.hyzionstudios.hyextras.tagnpc;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record TagNpcEntityState(
        UUID entity,
        Set<String> tags,
        Map<String, Object> variables,
        Instant lastSeen,
        @Nullable String displayName) {

    public TagNpcEntityState {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        variables = variables == null ? Map.of() : Map.copyOf(variables);
        lastSeen = lastSeen == null ? Instant.EPOCH : lastSeen;
    }
}
