package org.hyzionstudios.hyextras.imageicons;

import org.hyzionstudios.hyextras.config.HyExtrasConfig;

/**
 * Runtime-only placement and presentation tuning for an image icon attachment.
 */
public record ImageIconTuning(
        float offsetX,
        float offsetY,
        float offsetZ,
        float heightOffset,
        float scale,
        float maxDistance,
        BillboardMode billboardMode,
        int pixelOffsetX,
        int pixelOffsetY,
        int width,
        int height,
        int zOrder) {

    public enum BillboardMode {
        ALWAYS,
        VERTICAL,
        NONE
    }

    public static ImageIconTuning defaults(HyExtrasConfig config) {
        float maxDistance = config != null ? config.imageIconsDefaultVisibilityRadius : 48.0f;
        return new ImageIconTuning(
                0.0f,
                0.0f,
                0.0f,
                0.65f,
                1.0f,
                maxDistance,
                BillboardMode.ALWAYS,
                0,
                0,
                0,
                0,
                0);
    }

    public ImageIconTuning withFallbacks(HyExtrasConfig config, ImageIconDefinition definition) {
        ImageIconTuning defaults = defaults(config);
        return new ImageIconTuning(
                offsetX,
                offsetY,
                offsetZ,
                heightOffset == 0.0f ? defaults.heightOffset : heightOffset,
                scale == 0.0f ? defaults.scale : scale,
                maxDistance == 0.0f ? defaults.maxDistance : maxDistance,
                billboardMode == null ? defaults.billboardMode : billboardMode,
                pixelOffsetX,
                pixelOffsetY,
                width <= 0 && definition != null ? definition.width() : width,
                height <= 0 && definition != null ? definition.height() : height,
                zOrder);
    }
}
