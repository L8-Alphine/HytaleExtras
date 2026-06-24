package org.hyzionstudios.hyextras.imageicons;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ImageIconAttachment(
        UUID attachmentId,
        UUID target,
        String providerId,
        String iconId,
        Set<UUID> viewers,
        int priority,
        float radius,
        ImageIconTuning tuning,
        Instant createdAt) {

    public ImageIconAttachment {
        viewers = viewers == null ? Set.of() : Set.copyOf(viewers);
    }

    public boolean visibleToAll() {
        return viewers.isEmpty();
    }
}
