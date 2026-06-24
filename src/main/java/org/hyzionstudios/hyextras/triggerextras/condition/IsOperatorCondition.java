package org.hyzionstudios.hyextras.triggerextras.condition;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerCondition;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;

import java.util.logging.Level;

/**
 * Passes if the triggering player is a server operator.
 *
 * <p>By default this mirrors Hytale's {@code /op} command by checking membership in the
 * {@code "hytale:Admin"} permission group. Override with {@code "Permission"} to check a
 * specific permission string instead.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "is_operator" }
 * { "type": "is_operator", "Invert": true }
 * { "type": "is_operator", "Permission": "myserver.admin" }
 * }</pre>
 */
public class IsOperatorCondition extends TriggerCondition {

    private static final String OPERATOR_GROUP = "hytale:Admin";

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
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return false;
        }
        try {
            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) return invert; // non-player entities never pass
            boolean matches = hasCustomPermission(pr) || isOperator(pr);
            return invert ? !matches : matches;
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[is_operator] condition test failed");
            return false;
        }
    }

    private boolean hasCustomPermission(PlayerRef pr) {
        return permission != null && !permission.isBlank() && pr.hasPermission(permission);
    }

    private boolean isOperator(PlayerRef pr) {
        if (permission != null && !permission.isBlank()) {
            return false;
        }
        return PermissionsModule.get()
                .getGroupsForUser(pr.getUuid())
                .contains(OPERATOR_GROUP);
    }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public boolean isInvert() { return invert; }
    public void setInvert(boolean invert) { this.invert = invert; }
}
