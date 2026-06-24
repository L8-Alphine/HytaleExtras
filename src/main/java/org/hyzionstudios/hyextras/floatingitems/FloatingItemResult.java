package org.hyzionstudios.hyextras.floatingitems;

import javax.annotation.Nullable;
import java.util.UUID;

public record FloatingItemResult(boolean success, String message, @Nullable UUID floatingItemUuid) {

    public static FloatingItemResult success(String message) {
        return new FloatingItemResult(true, message, null);
    }

    public static FloatingItemResult item(UUID floatingItemUuid, String message) {
        return new FloatingItemResult(true, message, floatingItemUuid);
    }

    public static FloatingItemResult failure(String message) {
        return new FloatingItemResult(false, message, null);
    }
}
