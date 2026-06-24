package org.hyzionstudios.hyextras.triggerextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hyextras.HyExtrasPlugin;
import org.hyzionstudios.hyextras.codec.CodecHelper;
import org.hyzionstudios.hyextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class RunRewardCommandAction extends TriggerEffect {

    public static final BuilderCodec<RunRewardCommandAction> CODEC = BuilderCodec.builder(
                    RunRewardCommandAction.class, RunRewardCommandAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Command"),
                    RunRewardCommandAction::setCommand,
                    RunRewardCommandAction::getCommand).add()
            .append(CodecHelper.optString("DebugName"),
                    RunRewardCommandAction::setDebugName,
                    RunRewardCommandAction::getDebugName).add()
            .build();

    private String command;
    @Nullable private String debugName;

    public RunRewardCommandAction() {}

    @Override
    public void execute(TriggerContext ctx) {
        if (!org.hyzionstudios.hyextras.module.TriggerExtrasRuntime.isEnabled()) {
            return;
        }
        try {
            if (command == null || command.isBlank()) {
                warn("command is empty");
                return;
            }
            PlayerRef pr = ctx.getStore().getComponent(ctx.getEntityRef(), PlayerRef.getComponentType());
            if (pr == null) {
                warn("triggering entity is not a player");
                return;
            }
            String resolved = StringTemplate.resolve(command, ctx, HyExtrasPlugin.get().getVariableService());
            CommandManager.get().handleCommand(pr, resolved)
                    .exceptionally(e -> {
                        HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                                .log("[run_reward_command] " + label() + " failed during execution");
                        return null;
                    });
        } catch (Exception e) {
            HyExtrasPlugin.get().getLogger().at(Level.WARNING).withCause(e)
                    .log("[run_reward_command] " + label() + " failed");
        }
    }

    private String label() {
        return debugName != null && !debugName.isBlank() ? debugName : "run_reward_command";
    }

    private void warn(String reason) {
        HyExtrasPlugin.get().getLogger().at(Level.WARNING)
                .log("[run_reward_command] " + label() + " skipped: " + reason);
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    @Nullable public String getDebugName() { return debugName; }
    public void setDebugName(@Nullable String debugName) { this.debugName = debugName; }
}
