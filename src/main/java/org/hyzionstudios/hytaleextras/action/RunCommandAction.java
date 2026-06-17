package org.hyzionstudios.hytaleextras.action;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerContext;
import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEffect;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.hyzionstudios.hytaleextras.HytaleextrasPlugin;
import org.hyzionstudios.hytaleextras.codec.CodecHelper;
import org.hyzionstudios.hytaleextras.util.StringTemplate;

import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Runs a command string when a trigger volume fires.
 * The command runs as the triggering player (their permission level applies).
 *
 * <p>Supports {@link StringTemplate} placeholders: {@code {player}}, {@code {uuid}},
 * {@code {variable:key}}.
 *
 * <p>JSON config:
 * <pre>{@code
 * { "type": "run_command", "command": "say Hello {player}!" }
 * }</pre>
 */
public class RunCommandAction extends TriggerEffect {

    public static final BuilderCodec<RunCommandAction> CODEC = BuilderCodec.builder(
                    RunCommandAction.class, RunCommandAction::new, TriggerEffect.BASE_CODEC)
            .append(CodecHelper.string("Command"),       RunCommandAction::setCommand,   RunCommandAction::getCommand).add()
            .append(CodecHelper.optString("DebugName"),  RunCommandAction::setDebugName, RunCommandAction::getDebugName).add()
            .build();

    private String command;
    @Nullable private String debugName;

    public RunCommandAction() {}

    @Override
    public void execute(TriggerContext ctx) {
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
            String resolved = StringTemplate.resolve(command, ctx, HytaleextrasPlugin.get().getVariableService());
            CommandManager.get().handleCommand(pr, resolved)
                    .exceptionally(e -> {
                        HytaleextrasPlugin.get().getLogger()
                                .at(Level.WARNING).withCause(e)
                                .log("[run_command] " + label() + " failed during execution");
                        return null;
                    });
        } catch (Exception e) {
            HytaleextrasPlugin.get().getLogger()
                    .at(Level.WARNING).withCause(e)
                    .log("[run_command] " + label() + " failed");
        }
    }

    private String label() {
        return (debugName != null && !debugName.isBlank()) ? debugName : "run_command";
    }

    private void warn(String reason) {
        HytaleextrasPlugin.get().getLogger()
                .at(Level.WARNING)
                .log("[run_command] " + label() + " skipped: " + reason);
    }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    @Nullable public String getDebugName() { return debugName; }
    public void setDebugName(@Nullable String debugName) { this.debugName = debugName; }
}
