package org.hyzionstudios.hyextras.util;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class PlaceholderApiBridge {

    private static final Pattern PERCENT_PLACEHOLDER = Pattern.compile("%[^%\\s]+%");

    private PlaceholderApiBridge() {}

    public static TemplateRenderResult resolve(
            String text,
            @Nullable PlayerRef player,
            MissingPlaceholderBehavior missingBehavior) {
        if (text == null || text.isEmpty() || !PERCENT_PLACEHOLDER.matcher(text).find()) {
            return TemplateRenderResult.success(text);
        }
        if (!isAvailable()) {
            return handleMissing(text, missingBehavior, "PlaceholderAPI is not installed.");
        }
        try {
            Class<?> apiClass = Class.forName("at.helpch.placeholderapi.PlaceholderAPI");
            Method method = apiClass.getMethod("setPlaceholders", PlayerRef.class, String.class);
            String resolved = (String) method.invoke(null, player, text);
            if (PERCENT_PLACEHOLDER.matcher(resolved).find()) {
                return handleMissing(resolved, missingBehavior, "PlaceholderAPI left unresolved placeholders.");
            }
            return TemplateRenderResult.success(resolved);
        } catch (Exception e) {
            HyExtrasPlugin plugin = HyExtrasPlugin.get();
            if (plugin != null) {
                plugin.getLogger().at(Level.WARNING)
                        .log("[HyExtras] PlaceholderAPI rendering failed: " + e.getMessage());
            }
            return handleMissing(text, missingBehavior, "PlaceholderAPI rendering failed.");
        }
    }

    public static boolean isAvailable() {
        try {
            Class<?> pluginClass = Class.forName("at.helpch.placeholderapi.PlaceholderAPIPlugin");
            Method instance = pluginClass.getMethod("instance");
            return instance.invoke(null) != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static TemplateRenderResult handleMissing(
            String text,
            MissingPlaceholderBehavior behavior,
            String error) {
        return switch (behavior) {
            case KEEP_ORIGINAL -> TemplateRenderResult.success(text);
            case EMPTY -> TemplateRenderResult.success(PERCENT_PLACEHOLDER.matcher(text).replaceAll(""));
            case ERROR -> TemplateRenderResult.failure(text, error);
        };
    }
}
