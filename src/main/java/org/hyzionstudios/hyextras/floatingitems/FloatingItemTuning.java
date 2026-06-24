package org.hyzionstudios.hyextras.floatingitems;

import org.hyzionstudios.hyextras.config.HyExtrasConfig;

/** Decorative placement and presentation tuning for a floating item display. */
public record FloatingItemTuning(
        float scale,
        float visibilityRadius,
        float bobAmplitude,
        float rotationDegreesPerSecond,
        float offsetX,
        float offsetY,
        float offsetZ,
        int priority) {

    public static FloatingItemTuning defaults(HyExtrasConfig config) {
        return new FloatingItemTuning(
                1.0f,
                config != null ? config.floatingItemsDefaultVisibilityRadius : 48.0f,
                config != null ? config.floatingItemsDefaultBobAmplitude : 0.15f,
                config != null ? config.floatingItemsDefaultRotationDegreesPerSecond : 45.0f,
                0.0f,
                0.0f,
                0.0f,
                0);
    }

    public FloatingItemTuning withFallbacks(HyExtrasConfig config) {
        FloatingItemTuning defaults = defaults(config);
        return new FloatingItemTuning(
                scale <= 0.0f ? defaults.scale : scale,
                visibilityRadius <= 0.0f ? defaults.visibilityRadius : visibilityRadius,
                bobAmplitude < 0.0f ? defaults.bobAmplitude : bobAmplitude,
                rotationDegreesPerSecond < 0.0f
                        ? defaults.rotationDegreesPerSecond
                        : rotationDegreesPerSecond,
                offsetX,
                offsetY,
                offsetZ,
                priority);
    }
}
