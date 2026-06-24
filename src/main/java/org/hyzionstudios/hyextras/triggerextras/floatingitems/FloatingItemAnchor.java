package org.hyzionstudios.hyextras.triggerextras.floatingitems;

import java.util.Map;

public enum FloatingItemAnchor {
    TRIGGERING_ENTITY,
    VOLUME_CENTER,
    BLOCK_POSITION,
    EXPLICIT;

    public static final Map<FloatingItemAnchor, String> ALIASES = Map.of(
            TRIGGERING_ENTITY, "triggering_entity",
            VOLUME_CENTER, "volume_center",
            BLOCK_POSITION, "block_position",
            EXPLICIT, "explicit"
    );
}
