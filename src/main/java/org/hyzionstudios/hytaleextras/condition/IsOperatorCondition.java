package org.hyzionstudios.hytaleextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.HyextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;

import java.util.logging.Level;

/**
 * Passes if the triggering entity holds the operator permission.
 *
 * <p>Defaults to checking {@code "hytale.op"}. Override with {@code "Permission"} if the
 * server uses a different op-level permission string.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "is_operator" }
 * { "type": "is_operator", "Invert": true }
 * { "type": "is_operator", "Permission": "myserver.admin" }
 * }</pre>
 *
 * <p>TODO: Replace {@code hasPermission()} with a direct {@code isOperator()} call once
 * confirmed in the Hytale PlayerRef API.
 */
public class IsOperatorCondition extends TriggerCondition {

    private static final String DEFAULT_OP_PERMISSION = "hytale.op";

    public static final BuilderCodec<IsOperatorCondition> CODEC = BuilderCodec.builder(
                    IsOperatorCondition.class, IsOperatorCondition::new, TriggerCondition.BASE_CODEC)
            .append(CodecHelper.optString("Permission"),
                    IsOperatorCondition::setPermission,
                    IsOperatorCondition::getPermission).add()
            .append(CodecHelper.optBool("Invert"),
                    IsOperatorCondition::setInvert,
                    IsOperatorCondition::isInvert).add()
            .build();

    private String permission;
    private boolean invert = false;

    public IsOperatorCondition() {}

    @Override
    public boolean test(TriggerContext ctx) {
        try {
            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) return invert; // non-player entities never pass
            String perm = (permission != null && !permission.isBlank()) ? permission : DEFAULT_OP_PERMISSION;
            boolean hasOp = pr.hasPermission(perm);
            return invert ? !hasOp : hasOp;
        } catch (Exception e) {
            HyextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[is_operator] condition test failed");
            return false;
        }
    }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public boolean isInvert() { return invert; }
    public void setInvert(boolean invert) { this.invert = invert; }
}
