package org.hyzionstudios.hyextras.triggerextras;

import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import com.hypixel.hytale.protocol.InteractionType;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InteractableVolumeState {

    public static final String TAG_INTERACTABLE = "hextras:interactable";
    public static final String TAG_MESSAGE = "hextras:interaction_message";
    public static final String TAG_ACTION = "hextras:interaction_action";
    public static final String TAG_KEY = "hextras:interaction_key";
    public static final String TAG_NAME = "hextras:interaction_name";
    public static final String TAG_INTERACTION_TYPE = "hextras:interaction_type";
    /** When truthy, the interaction prompt is shown only after the effect chain's conditions pass. */
    public static final String TAG_PROMPT_CONDITIONAL = "hextras:interaction_prompt_conditional";
    public static final String DEFAULT_MESSAGE = "interactionHints.generic";
    public static final String DEFAULT_ACTION = "interact";
    public static final String DEFAULT_KEY = "Use";

    private final ConcurrentHashMap<String, Entry> runtimeEntries = new ConcurrentHashMap<>();

    public void set(
            String volumeId,
            @Nullable String message,
            @Nullable String action,
            @Nullable String key,
            @Nullable String name,
            @Nullable String interactionType) {
        runtimeEntries.put(volumeId, new Entry(
                true,
                blankToNull(message),
                blankToNull(action),
                blankToNull(key),
                blankToNull(name),
                normalize(blankToNull(interactionType))));
    }

    public void clear(String volumeId) {
        runtimeEntries.remove(volumeId);
    }

    @Nullable
    public Config resolve(VolumeEntry volume) {
        if (volume == null) {
            return null;
        }
        Entry runtime = runtimeEntries.get(volume.getId());
        if (runtime != null) {
            return runtime.enabled
                    ? new Config(
                            valueOrDefault(runtime.message, DEFAULT_MESSAGE),
                            valueOrDefault(runtime.action, DEFAULT_ACTION),
                            valueOrDefault(runtime.key, DEFAULT_KEY),
                            valueOrDefault(runtime.name, valueOrDefault(runtime.action, DEFAULT_ACTION)),
                            runtime.interactionType,
                            false)
                    : null;
        }

        Map<String, String> tags = volume.getRawTags();
        if (tags == null || !isTruthy(tags.get(TAG_INTERACTABLE))) {
            return null;
        }
        return new Config(
                valueOrDefault(blankToNull(tags.get(TAG_MESSAGE)), DEFAULT_MESSAGE),
                valueOrDefault(blankToNull(tags.get(TAG_ACTION)), DEFAULT_ACTION),
                valueOrDefault(blankToNull(tags.get(TAG_KEY)), DEFAULT_KEY),
                valueOrDefault(blankToNull(tags.get(TAG_NAME)), valueOrDefault(blankToNull(tags.get(TAG_ACTION)), DEFAULT_ACTION)),
                normalize(blankToNull(tags.get(TAG_INTERACTION_TYPE))),
                isTruthy(tags.get(TAG_PROMPT_CONDITIONAL)));
    }

    public boolean matchesInteraction(@Nullable Config config, InteractionType interactionType) {
        if (config == null || config.interactionType() == null) {
            return true;
        }
        return normalize(interactionType.name()).equals(config.interactionType());
    }

    public void clearAll() {
        runtimeEntries.clear();
    }

    private static boolean isTruthy(@Nullable String value) {
        return value != null
                && ("true".equalsIgnoreCase(value)
                || "1".equals(value)
                || "yes".equalsIgnoreCase(value)
                || "enabled".equalsIgnoreCase(value));
    }

    @Nullable
    private static String blankToNull(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String valueOrDefault(@Nullable String value, String fallback) {
        return value != null ? value : fallback;
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Entry(
            boolean enabled,
            @Nullable String message,
            @Nullable String action,
            @Nullable String key,
            @Nullable String name,
            @Nullable String interactionType) {}

    public record Config(
            String message,
            String action,
            String key,
            String name,
            @Nullable String interactionType,
            boolean conditionalPrompt) {}
}
