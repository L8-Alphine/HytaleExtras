package org.hyzionstudios.hyextras.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.hyzionstudios.hyextras.service.CooldownService;
import org.hyzionstudios.hyextras.service.PlayerTagService;
import org.hyzionstudios.hyextras.service.PlayerVariableService;
import org.hyzionstudios.hyextras.module.InternalModuleManager;
import org.hyzionstudios.hyextras.state.RuntimeStateStore;
import org.hyzionstudios.hyextras.floatingitems.FloatingItemService;
import org.hyzionstudios.hyextras.tagnpc.TagNpcService;

/**
 * Root command: {@code /hextras}.
 * Sub-commands: var, cooldown, list, debug, reload.
 */
public class ExtrasRootCommand extends AbstractCommandCollection {

    public ExtrasRootCommand(
            PlayerVariableService vars,
            PlayerTagService tags,
            CooldownService cd,
            RuntimeStateStore state,
            InternalModuleManager modules,
            TagNpcService tagNpcService,
            FloatingItemService floatingItemService) {
        super("hextras", "HyExtras administrative commands");
        addSubCommand(new VarCommand(vars));
        addSubCommand(new TagCommand(tags));
        addSubCommand(new CooldownCommand(cd));
        addSubCommand(new ListCommand());
        addSubCommand(new DebugCommand(state));
        addSubCommand(new ModulesCommand(modules));
        addSubCommand(new ModuleCommand(modules));
        addSubCommand(new TagNpcCommand(tagNpcService));
        addSubCommand(new FloatingItemsCommand(floatingItemService));
        addSubCommand(new ReloadCommand());
    }
}
