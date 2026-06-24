package org.hyzionstudios.hyextras.imageicons;

import javax.annotation.Nullable;
import java.util.UUID;

public record ImageIconResult(boolean success, String message, @Nullable UUID attachmentId) {

    public static ImageIconResult success(String message) {
        return new ImageIconResult(true, message, null);
    }

    public static ImageIconResult attachment(UUID attachmentId) {
        return new ImageIconResult(true, "Attachment created.", attachmentId);
    }

    public static ImageIconResult failure(String message) {
        return new ImageIconResult(false, message, null);
    }
}
