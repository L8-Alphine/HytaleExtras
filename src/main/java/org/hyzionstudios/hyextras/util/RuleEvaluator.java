package org.hyzionstudios.hyextras.util;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.builtin.triggervolumes.manager.VolumeEntry;
import org.hyzionstudios.hyextras.HyExtrasPlugin;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates simple HyExtras predicate placeholders such as
 * {@code {hasTag:story} and {@code {variable:partyId=alpha}.
 */
public final class RuleEvaluator {

    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]+)}");

    private RuleEvaluator() {}

    public static boolean matches(@Nullable String rule, RuleEvaluationContext ctx) {
        if (rule == null || rule.isBlank()) {
            return true;
        }
        Matcher matcher = TOKEN.matcher(rule);
        boolean sawToken = false;
        while (matcher.find()) {
            sawToken = true;
            if (!evaluateToken(matcher.group(1).trim(), ctx)) {
                return false;
            }
        }
        return sawToken;
    }

    public static String resolveText(@Nullable String template, RuleEvaluationContext ctx) {
        if (template == null || template.isEmpty()) {
            return template;
        }
        Matcher matcher = TOKEN.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String replacement = resolveToken(matcher.group(1).trim(), ctx);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static boolean evaluateToken(String token, RuleEvaluationContext ctx) {
        if (token.startsWith("!hasTag:")) {
            return !hasTag(ctx.playerUuid(), token.substring("!hasTag:".length()));
        }
        if (token.startsWith("hasTag:")) {
            return hasTag(ctx.playerUuid(), token.substring("hasTag:".length()));
        }
        if (token.startsWith("!variable:")) {
            return variableValue(ctx.playerUuid(), token.substring("!variable:".length())) == null;
        }
        if (token.startsWith("variable:")) {
            return evaluateVariable(ctx.playerUuid(), token.substring("variable:".length()));
        }
        if (token.startsWith("eventType:")) {
            TriggerEventType type = ctx.eventType();
            return type != null && type.name().equalsIgnoreCase(token.substring("eventType:".length()).trim());
        }
        if (token.startsWith("volumeTag:")) {
            return evaluateVolumeTag(ctx, token.substring("volumeTag:".length()));
        }
        warnUnknown(token);
        return false;
    }

    private static String resolveToken(String token, RuleEvaluationContext ctx) {
        if ("player".equals(token)) {
            return ctx.playerName() != null ? ctx.playerName() : "unknown";
        }
        if ("uuid".equals(token)) {
            return ctx.playerUuid() != null ? ctx.playerUuid().toString() : "unknown";
        }
        if ("eventType".equals(token)) {
            return ctx.eventType() != null ? ctx.eventType().name() : "";
        }
        if ("tagKey".equals(token)) {
            return ctx.tagKey() != null ? ctx.tagKey() : "";
        }
        if ("tagValue".equals(token)) {
            return ctx.tagValue() != null ? ctx.tagValue() : "";
        }
        if (token.startsWith("variable:")) {
            String raw = token.substring("variable:".length());
            String key = trimComparison(raw);
            String value = variableValue(ctx.playerUuid(), key);
            return value != null ? value : "";
        }
        if (token.startsWith("volumeTag:")) {
            String raw = token.substring("volumeTag:".length());
            if (raw.contains("=")) {
                return String.valueOf(evaluateToken(token, ctx));
            }
            String value = firstVolumeTag(ctx, raw.trim());
            return value != null ? value : "";
        }
        if (token.startsWith("hasTag:") || token.startsWith("!hasTag:") || token.startsWith("eventType:")) {
            return String.valueOf(evaluateToken(token, ctx));
        }
        return "{" + token + "}";
    }

    private static boolean hasTag(@Nullable UUID player, String tag) {
        if (player == null || tag == null || tag.isBlank()) {
            return false;
        }
        return HyExtrasPlugin.get().getTagService().hasTag(player, tag.trim());
    }

    @Nullable
    private static String variableValue(@Nullable UUID player, String key) {
        if (player == null || key == null || key.isBlank()) {
            return null;
        }
        return HyExtrasPlugin.get().getVariableService().getString(player, key.trim());
    }

    private static boolean evaluateVariable(@Nullable UUID player, String expr) {
        String op = expr.contains("!=") ? "!=" : (expr.contains("=") ? "=" : null);
        if (op == null) {
            return variableValue(player, expr) != null;
        }
        String[] parts = expr.split(Pattern.quote(op), 2);
        String actual = variableValue(player, parts[0]);
        String expected = parts.length > 1 ? parts[1].trim() : "";
        boolean equals = actual != null && actual.equals(expected);
        return "!=".equals(op) ? !equals : equals;
    }

    private static boolean evaluateVolumeTag(RuleEvaluationContext ctx, String expr) {
        String op = expr.contains("!=") ? "!=" : (expr.contains("=") ? "=" : null);
        if (op == null) {
            return firstVolumeTag(ctx, expr.trim()) != null;
        }
        String[] parts = expr.split(Pattern.quote(op), 2);
        String actual = firstVolumeTag(ctx, parts[0].trim());
        String expected = parts.length > 1 ? parts[1].trim() : "";
        boolean equals = actual != null && actual.equalsIgnoreCase(expected);
        return "!=".equals(op) ? !equals : equals;
    }

    @Nullable
    private static String firstVolumeTag(RuleEvaluationContext ctx, String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        for (VolumeEntry volume : ctx.volumes()) {
            if (volume == null) continue;
            Map<String, String> tags = volume.getRawTags();
            if (tags != null && tags.containsKey(key)) {
                return tags.get(key);
            }
        }
        return null;
    }

    private static String trimComparison(String raw) {
        int eq = raw.indexOf('=');
        if (eq >= 0) {
            return raw.substring(0, eq).replace("!", "").trim();
        }
        return raw.trim();
    }

    private static void warnUnknown(String token) {
        HyExtrasPlugin plugin = HyExtrasPlugin.get();
        if (plugin != null && plugin.getExtrasConfig() != null && plugin.getExtrasConfig().debugMode) {
            plugin.getLogger().at(Level.WARNING)
                    .log("[hextras rule] unknown predicate token: " + token.toLowerCase(Locale.ROOT));
        }
    }
}
