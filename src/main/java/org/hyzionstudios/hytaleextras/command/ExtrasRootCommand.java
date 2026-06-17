package org.hyzionstudios.hytaleextras.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.hyzionstudios.hytaleextras.service.CooldownService;
import org.hyzionstudios.hytaleextras.service.PlayerTagService;
import org.hyzionstudios.hytaleextras.service.PlayerVariableService;
import org.hyzionstudios.hytaleextras.state.RuntimeStateStore;

/**
 * Root command: {@code /hextras}.
 * Sub-commands: var, cooldown, list, debug, reload.
 */
public class ExtrasRootCommand extends AbstractCommandCollection {

    public ExtrasRootCommand(PlayerVariableService vars, PlayerTagService tags, CooldownService cd, RuntimeStateStore state) {
        super("hextras", "HytaleExtras administrative commands");
        addSubCommand(new VarCommand(vars));
        addSubCommand(new TagCommand(tags));
        addSubCommand(new CooldownCommand(cd));
        addSubCommand(new ListCommand());
        addSubCommand(new DebugCommand(state));
        addSubCommand(new ReloadCommand());
    }
}
