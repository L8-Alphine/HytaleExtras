package org.hyzionstudios.hyextras.util;

import javax.annotation.Nullable;

public record TemplateRenderResult(
        String text,
        boolean success,
        @Nullable String error) {

    public static TemplateRenderResult success(String text) {
        return new TemplateRenderResult(text, true, null);
    }

    public static TemplateRenderResult failure(String text, String error) {
        return new TemplateRenderResult(text, false, error);
    }
}
