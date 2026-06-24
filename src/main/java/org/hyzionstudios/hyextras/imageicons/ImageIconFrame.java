package org.hyzionstudios.hyextras.imageicons;

import java.nio.file.Path;

public record ImageIconFrame(
        int index,
        Path file,
        String glyph,
        int width,
        int height,
        int durationMillis) {
}
