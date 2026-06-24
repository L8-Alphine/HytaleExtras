package org.hyzionstudios.hyextras.util;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.manager.TriggerVolumeManager;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.TriggerVolumeApiAdapter;
import org.hyzionstudios.hyextras.config.HyExtrasConfig;
import org.hyzionstudios.hyextras.service.PlayerVariableService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resolves placeholder tokens inside trigger volume config strings.
 *
 * <ul>
 *   <li>{@code {player}} - the triggering player's username</li>
 *   <li>{@code {uuid}} - the triggering entity's UUID</li>
 *   <li>{@code {variable:key}} - a per-player variable value (empty string if missing)</li>
 *   <li>{@code {activeVolumeCount}} - number of active volumes for the triggering player</li>
 *   <li>{@code {currentVolumeTag:key}} - tag value on the current trigger volume</li>
 *   <li>{@code {volumeTag:volumeId:key}} - tag value on a named trigger volume</li>
 *   <li>{@code {volumeActive:volumeId}} - true when the named volume is active for the player</li>
 *   <li>{@code %placeholder%} - PlaceholderAPI placeholder when installed and enabled</li>
 * </ul>
 */
public final class StringTemplate {

    private StringTemplate() {}

    public static String resolve(String template, TriggerContext ctx, PlayerVariableService vars) {
        TemplateRenderResult result = render(template, StringTemplateContext.fromTrigger(ctx, vars));
        if (!result.success()) {
            throw new IllegalStateException(result.error());
        }
        return result.text();
    }

    public static TemplateRenderResult render(String template, StringTemplateContext context) {
        if (template == null || template.isEmpty()) {
            return TemplateRenderResult.success("");
        }

        String result = template;
        HyExtrasConfig config = HyExtrasPlugin.get() != null ? HyExtrasPlugin.get().getExtrasConfig() : null;
        if (config == null || config.stringTemplateNativePlaceholdersEnabled) {
            result = resolveNative(result, context);
        }

        result = PlaceholderRegistry.global().resolve(result, context);

        if (shouldResolvePlaceholderApi(config)) {
            MissingPlaceholderBehavior behavior = config != null
                    ? config.stringTemplatePlaceholderApiMissingBehavior
                    : MissingPlaceholderBehavior.KEEP_ORIGINAL;
            TemplateRenderResult placeholderApiResult = PlaceholderApiBridge.resolve(
                    result,
                    context.playerRef(),
                    behavior);
            if (!placeholderApiResult.success()) {
                return placeholderApiResult;
            }
            result = placeholderApiResult.text();
        }

        return TemplateRenderResult.success(result);
    }

    private static boolean shouldResolvePlaceholderApi(HyExtrasConfig config) {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        boolean moduleEnabled = plugin == null || plugin.isModuleEnabled(HyExtrasConfig.MODULE_PLACEHOLDER_API);
        return moduleEnabled && (config == null || config.stringTemplatePlaceholderApiEnabled);
    }

    private static String resolveNative(String template, StringTemplateContext context) {
        UUID uuid = context.playerUuid();
        String uuidStr = uuid != null ? uuid.toString() : "unknown";
        String result = template
                .replace("{player}", context.playerName())
                .replace("{uuid}", uuidStr);

        if (uuid != null && result.contains("{variable:")) {
            result = resolveVariables(result, uuid, context.variables());
        }

        result = resolveVolumePlaceholders(result, context);

        if (uuid != null) {
            result = RuleEvaluator.resolveText(result, RuleEvaluationContext.fromTrigger(
                    uuid,
                    context.playerName(),
                    context.triggerContext()));
        } else {
            result = RuleEvaluator.resolveText(result, RuleEvaluationContext.fromTrigger(
                    new UUID(0L, 0L),
                    context.playerName(),
                    context.triggerContext()));
        }

        return result;
    }

    private static String resolveVariables(String text, UUID uuid, PlayerVariableService vars) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf("{variable:", i);
            if (start == -1) {
                sb.append(text, i, text.length());
                break;
            }
            sb.append(text, i, start);
            int end = text.indexOf('}', start);
            if (end == -1) {
                sb.append(text, start, text.length());
                break;
            }
            String key = text.substring(start + 10, end);
            Object val = vars.get(uuid, key);
            sb.append(val != null ? val.toString() : "");
            i = end + 1;
        }
        return sb.toString();
    }

    private static String resolveVolumePlaceholders(String text, StringTemplateContext context) {
        if (!text.contains("{activeVolumeCount}")
                && !text.contains("{currentVolumeId}")
                && !text.contains("{currentVolumeTag:")
                && !text.contains("{volumeTag:")
                && !text.contains("{volumeActive:")) {
            return text;
        }

        String result = text;
        TriggerContext triggerContext = context.triggerContext();
        VolumeEntry current = triggerContext.getVolume();
        String currentVolumeId = current != null ? current.getId() : "";
        result = result.replace("{currentVolumeId}", currentVolumeId);

        int activeCount = 0;
        UUID uuid = context.playerUuid();
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        List<VolumeEntry> activeVolumes = List.of();
        if (plugin != null && uuid != null) {
            activeVolumes = plugin.getActiveVolumesForPlayer(uuid);
            activeCount = activeVolumes.size();
        }
        result = result.replace("{activeVolumeCount}", Integer.toString(activeCount));

        if (result.contains("{currentVolumeTag:")) {
            result = resolveTagTokens(result, "{currentVolumeTag:", token -> {
                Map<String, String> tags = current != null ? current.getRawTags() : null;
                String value = tags != null ? tags.get(token) : null;
                return value != null ? value : "";
            });
        }

        if (result.contains("{volumeTag:")) {
            TriggerVolumeManager manager = TriggerVolumeApiAdapter.getManagerForStore(triggerContext.getStore());
            result = resolveTagTokens(result, "{volumeTag:", token -> {
                int split = token.indexOf(':');
                if (split <= 0 || split >= token.length() - 1 || manager == null) {
                    return "";
                }
                VolumeEntry volume = manager.getVolume(token.substring(0, split));
                Map<String, String> tags = volume != null ? volume.getRawTags() : null;
                String value = tags != null ? tags.get(token.substring(split + 1)) : null;
                return value != null ? value : "";
            });
        }

        if (result.contains("{volumeActive:")) {
            List<VolumeEntry> active = activeVolumes;
            result = resolveTagTokens(result, "{volumeActive:", token -> {
                for (VolumeEntry volume : active) {
                    if (volume != null && token.equals(volume.getId())) {
                        return "true";
                    }
                }
                return "false";
            });
        }

        return result;
    }

    private static String resolveTagTokens(String text, String prefix, TokenResolver resolver) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int start = text.indexOf(prefix, i);
            if (start == -1) {
                sb.append(text, i, text.length());
                break;
            }
            sb.append(text, i, start);
            int end = text.indexOf('}', start);
            if (end == -1) {
                sb.append(text, start, text.length());
                break;
            }
            String token = text.substring(start + prefix.length(), end);
            sb.append(resolver.resolve(token));
            i = end + 1;
        }
        return sb.toString();
    }

    private interface TokenResolver {
        String resolve(String token);
    }
}
