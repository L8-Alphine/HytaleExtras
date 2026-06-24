package org.hyzionstudios.hyextras.imageicons;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

public record ImageIconDefinition(
        String providerId,
        String iconId,
        SourceType sourceType,
        Path localPath,
        URI remoteUri,
        List<ImageIconFrame> frames,
        int width,
        int height,
        long lastModifiedMillis) {

    public enum SourceType {
        PNG,
        GIF,
        REMOTE_PNG,
        REMOTE_GIF
    }

    public ImageIconDefinition {
        frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public String qualifiedId() {
        return providerId + ":" + iconId;
    }

    public boolean animated() {
        return frames.size() > 1;
    }
}
