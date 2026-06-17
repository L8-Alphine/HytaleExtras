package org.hyzionstudios.hytaleextras.util;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.service.PlayerVariableService;

import java.util.UUID;

/**
 * Resolves placeholder tokens inside trigger volume config strings.
 *
 * <ul>
 *   <li>{@code {player}} — the triggering player's username</li>
 *   <li>{@code {uuid}}   — the triggering entity's UUID</li>
 *   <li>{@code {variable:key}} — a per-player variable value (empty string if missing)</li>
 * </ul>
 */
public final class StringTemplate {

    private StringTemplate() {}

    public static String resolve(String template, TriggerContext ctx, PlayerVariableService vars) {
        PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
        String playerName = pr != null ? pr.getUsername() : "unknown";
        UUID uuid = pr != null ? pr.getUuid() : null;
        String uuidStr = uuid != null ? uuid.toString() : "unknown";

        String result = template
                .replace("{player}", playerName)
                .replace("{uuid}", uuidStr);

        if (uuid != null && result.contains("{variable:")) {
            result = resolveVariables(result, uuid, vars);
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
}
